package net.protsenko.refrigeration.engineer.service.LLM;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.protsenko.refrigeration.engineer.domain.model.*;
import net.protsenko.refrigeration.engineer.service.processor.RawBundle;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.stereotype.Service;
import org.springframework.util.MimeTypeUtils;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

@Slf4j
@Service
@RequiredArgsConstructor
public class LLMAnalyzer {

    private final ChatClient chatClient;
    private final Executor executor = Executors.newVirtualThreadPerTaskExecutor();

    public ChamberSpecList analyze(RawBundle bundle) {
        List<String> chamberNames = detectChambers(bundle);
        log.info("Обнаружены камеры: {}", chamberNames);

        Map<String, String> temperatures = detectTemperatures(bundle, chamberNames);
        log.info("Температуры: {}", temperatures);

        CompletableFuture<Map<String, DailyVolume>> volumesFuture = async(() -> detectDailyVolumes(bundle, chamberNames));
        CompletableFuture<Map<String, Dimensions>> dimensionsFuture = async(() -> detectDimensions(bundle, chamberNames));
        CompletableFuture<Map<String, AdjacentSpaces>> adjacentFuture = async(() -> detectAdjacentSpaces(bundle, chamberNames, temperatures));
        CompletableFuture<Map<String, ProductInfo>> productFuture = async(() -> detectProductInfo(bundle, chamberNames));
        CompletableFuture<Map<String, HeatSources>> heatFuture = async(() -> detectHeatSources(bundle, chamberNames));
        CompletableFuture<Map<String, Ventilation>> ventFuture = async(() -> detectVentilation(bundle, chamberNames));
        CompletableFuture<Map<String, Insulation>> insulFuture = async(() -> detectInsulation(bundle, chamberNames));
        CompletableFuture<Map<String, Doors>> doorsFuture = async(() -> detectDoors(bundle, chamberNames));

        CompletableFuture.allOf(
                volumesFuture, dimensionsFuture, adjacentFuture, productFuture,
                heatFuture, ventFuture, insulFuture, doorsFuture
        ).join();

        Map<String, DailyVolume> volumes = volumesFuture.join();
        Map<String, Dimensions> dimensions = dimensionsFuture.join();
        Map<String, AdjacentSpaces> adjacent = adjacentFuture.join();
        Map<String, ProductInfo> product = productFuture.join();
        Map<String, HeatSources> heat = heatFuture.join();
        Map<String, Ventilation> vent = ventFuture.join();
        Map<String, Insulation> insul = insulFuture.join();
        Map<String, Doors> doors = doorsFuture.join();

        List<ChamberSpec> chambers = chamberNames.stream()
                .map(name -> ChamberSpec.builder()
                        .name(name)
                        .chamberTemp(new SpecValue(temperatures.get(name)))
                        .dimensions(dimensions.getOrDefault(name, null))
                        .adjacentSpaces(adjacent.getOrDefault(name, null))
                        .dailyVolume(volumes.getOrDefault(name, null))
                        .productType(product.containsKey(name) ? product.get(name).productType() : null)
                        .loadingTemp(product.containsKey(name) ? product.get(name).loadingTemp() : null)
                        .storageTemp(product.containsKey(name) ? product.get(name).storageTemp() : null)
                        .heatSources(heat.getOrDefault(name, null))
                        .ventilation(vent.getOrDefault(name, null))
                        .insulation(insul.getOrDefault(name, null))
                        .doors(doors.getOrDefault(name, null))
                        .build())
                .toList();

        return new ChamberSpecList(chambers);
    }

    private List<String> detectChambers(RawBundle bundle) {
        String prompt = """
                Ты — инженер-холодильщик. Проанализируй текст технического задания
                и планировочное решение (если приложено).
                
                Найди ВСЕ помещения, которые подлежат холодоснабжению или охлаждению.
                Включай камеры хранения, морозильники, тамбуры, экспедиции с охлаждением.
                Не включай сухие склады, офисы, технические помещения без охлаждения.
                Не включай общие названия типа "холодильный склад" — только конкретные помещения.
                Если на планировке видны несколько одинаковых зон (например две экспедиции) — перечисли каждую отдельно.
                
                Верни ТОЛЬКО валидный JSON без markdown-блоков.
                Формат: {jsonFormat}
                
                Текст ТЗ: {specText}
                """;

        ChamberNameList result = call(
                bundle, prompt,
                Map.of("jsonFormat", "{\"names\":[\"Камера 1\",\"Камера 2\"]}"),
                true, true, false,
                ChamberNameList.class
        );
        return result != null ? result.names() : List.of();
    }

    private Map<String, String> detectTemperatures(RawBundle bundle, List<String> chamberNames) {
        String prompt = """
                Ты — инженер-холодильщик.
                
                Для каждого помещения из списка найди расчётный температурный режим хранения.
                Ищи в тексте ТЗ и на изображениях.
                Если не найдено — верни null. Не придумывай значения.
                
                Список помещений: {chamberNames}
                
                Верни ТОЛЬКО валидный JSON без markdown-блоков.
                Формат: {jsonFormat}
                
                Текст ТЗ: {specText}
                """;

        ChamberValueMap result = call(
                bundle, prompt,
                Map.of(
                        "chamberNames", String.join(", ", chamberNames),
                        "jsonFormat", "{\"values\":{\"Камера 1\":\"значение\",\"Камера 2\":null}}"
                ),
                true, true, false,
                ChamberValueMap.class
        );
        return result != null ? result.values() : Map.of();
    }

    private Map<String, DailyVolume> detectDailyVolumes(RawBundle bundle, List<String> chamberNames) {
        String jsonFormat = """
                {"values":{"Камера 1":{"tons":"98.18","pallets":null,"palletWeight":null},"Камера 2":{"tons":null,"pallets":"163","palletWeight":"600"}}}
                """;

        String prompt = """
                Ты — инженер-холодильщик.
                
                Для каждого помещения из списка найди суточный грузооборот.
                Грузооборот может быть указан в тоннах, в паллетах, или в обоих значениях.
                Также найди средний вес одного паллета в кг если указан.
                
                Ищи в тексте ТЗ, тексте таблицы грузооборота и на изображениях.
                Если параметр не найден — верни null. Не придумывай значения.
                
                Список помещений: {chamberNames}
                
                Верни ТОЛЬКО валидный JSON без markdown-блоков.
                Формат: {jsonFormat}
                
                Текст ТЗ: {specText}
                
                Таблица грузооборота: {turnoverText}
                """;

        ChamberVolumeMap result = call(
                bundle, prompt,
                Map.of(
                        "chamberNames", String.join(", ", chamberNames),
                        "jsonFormat", jsonFormat,
                        "turnoverText", bundle.turnoverText().isBlank() ? "не предоставлена" : bundle.turnoverText()
                ),
                true, false, true,
                ChamberVolumeMap.class
        );
        return result != null ? result.values() : Map.of();
    }

    private Map<String, Dimensions> detectDimensions(RawBundle bundle, List<String> chamberNames) {
        String jsonFormat = """
                {"values":{"Камера 1":{"length":{"value":"24"},"width":{"value":"12"},"height":{"value":"10.5"},"area":{"value":null}},"Камера 2":{"length":{"value":null},"width":{"value":null},"height":{"value":"10.5"},"area":{"value":"340.3"}}}}
                """;

        String prompt = """
                Ты — инженер-холодильщик. Перед тобой технологическая планировка склада.
                
                МАСШТАБ: если на чертеже указан масштаб (например М1:100) — используй его для расчёта размеров.
                СЕТКА ОСЕЙ: по периметру чертежа видны строительные оси с размерными цепочками.
                Стандартные пролёты: 6м, 12м или 24м. Размер камеры = сумма пролётов между осями.
                
                Для каждого помещения найди:
                ДЛИНА и ШИРИНА — по осям на планировке.
                ВЫСОТА — приоритет 1: текст ТЗ, приоритет 2: аннотация "h=10.5м" на чертеже.
                ПЛОЩАДЬ — из экспликации, только если длина/ширина не определены.
                
                Если параметр не найден — верни null. Не придумывай значения.
                
                Список помещений: {chamberNames}
                
                Верни ТОЛЬКО валидный JSON без markdown-блоков.
                Формат: {jsonFormat}
                
                Текст ТЗ: {specText}
                """;

        ChamberDimensionsMap result = call(
                bundle, prompt,
                Map.of(
                        "chamberNames", String.join(", ", chamberNames),
                        "jsonFormat", jsonFormat
                ),
                true, true, false,
                ChamberDimensionsMap.class
        );
        return result != null ? result.values() : Map.of();
    }

    private Map<String, AdjacentSpaces> detectAdjacentSpaces(RawBundle bundle,
                                                             List<String> chamberNames,
                                                             Map<String, String> temperatures) {
        String jsonFormat = """
                {"values":{"Камера 1":{"north":{"value":"+18"},"south":{"value":"улица"},"east":{"value":null},"west":{"value":"+6"},"ceiling":{"value":"улица"},"floor":{"value":"грунт"}}}}
                """;

        String prompt = """
                Ты — инженер-холодильщик.
                
                Для каждого помещения определи смежные помещения и их температуры.
                
                1. Смотри на планировку — какие помещения граничат с каждой камерой.
                2. Для каждой стороны (север, юг, восток, запад) верни температуру числом в °C.
                3. Если смежное помещение — улица, верни "улица".
                4. Потолок: кровля = "улица", другой этаж = его температура.
                5. Пол: грунт = "грунт", подвал = его температура.
                6. Если не удалось определить — верни null.
                
                Список помещений: {chamberNames}
                Температуры камер для справки: {temperatures}
                
                Верни ТОЛЬКО валидный JSON без markdown-блоков.
                Формат: {jsonFormat}
                
                Текст ТЗ: {specText}
                """;

        ChamberAdjacentMap result = call(
                bundle, prompt,
                Map.of(
                        "chamberNames", String.join(", ", chamberNames),
                        "temperatures", temperatures.toString(),
                        "jsonFormat", jsonFormat
                ),
                true, true, false,
                ChamberAdjacentMap.class
        );
        return result != null ? result.values() : Map.of();
    }

    private Map<String, ProductInfo> detectProductInfo(RawBundle bundle, List<String> chamberNames) {
        String jsonFormat = """
                {"values":{"Камера 1":{"productType":{"value":"мясо птицы"},"loadingTemp":{"value":"+4"},"storageTemp":{"value":"-18"}}}}
                """;

        String prompt = """
                Ты — инженер-холодильщик.
                
                Для каждого помещения найди:
                - productType: тип хранимой продукции
                - loadingTemp: температура продукции при загрузке (°C)
                - storageTemp: требуемая температура хранения (°C)
                
                Ищи в тексте ТЗ и на изображениях.
                Если не найдено — верни null. Не придумывай.
                
                Список помещений: {chamberNames}
                
                Верни ТОЛЬКО валидный JSON без markdown-блоков.
                Формат: {jsonFormat}
                
                Текст ТЗ: {specText}
                """;

        ChamberProductMap result = call(
                bundle, prompt,
                Map.of(
                        "chamberNames", String.join(", ", chamberNames),
                        "jsonFormat", jsonFormat
                ),
                true, false, true,
                ChamberProductMap.class
        );
        return result != null ? result.values() : Map.of();
    }

    private Map<String, HeatSources> detectHeatSources(RawBundle bundle, List<String> chamberNames) {
        String jsonFormat = """
                {"values":{"Камера 1":{"staffCount":{"value":"2"},"equipment":{"value":"1 погрузчик 5кВт"}}}}
                """;

        String prompt = """
                Ты — инженер-холодильщик.
                
                Для каждого помещения найди источники тепловыделения:
                - staffCount: количество персонала одновременно в камере
                - equipment: тепловыделяющее оборудование с мощностью в кВт если есть
                
                Ищи в тексте ТЗ и на изображениях (таблица характеристик помещений).
                Если не найдено — верни null. Не придумывай.
                
                Список помещений: {chamberNames}
                
                Верни ТОЛЬКО валидный JSON без markdown-блоков.
                Формат: {jsonFormat}
                
                Текст ТЗ: {specText}
                """;

        ChamberHeatSourcesMap result = call(
                bundle, prompt,
                Map.of(
                        "chamberNames", String.join(", ", chamberNames),
                        "jsonFormat", jsonFormat
                ),
                true, false, true,
                ChamberHeatSourcesMap.class
        );
        return result != null ? result.values() : Map.of();
    }

    private Map<String, Ventilation> detectVentilation(RawBundle bundle, List<String> chamberNames) {
        String jsonFormat = """
                {"values":{"Камера 1":{"airVolume":{"value":"0.5 крат"},"supplyTemp":{"value":null}}}}
                """;

        String prompt = """
                Ты — инженер-холодильщик.
                
                Для каждого помещения найди параметры вентиляции:
                - airVolume: объём приточного воздуха (м³/час или кратность воздухообмена)
                - supplyTemp: температура приточного воздуха (°C)
                
                Важно: если указано "по расчёту исходя из количества работников X м³/час на человека" —
                верни точную формулировку, например "60 м³/час на человека". Не подставляй итоговое число.
                
                Ищи в тексте ТЗ и таблице параметров микроклимата.
                Если не найдено — верни null. Не придумывай.
                
                Список помещений: {chamberNames}
                
                Верни ТОЛЬКО валидный JSON без markdown-блоков.
                Формат: {jsonFormat}
                
                Текст ТЗ: {specText}
                """;

        ChamberVentilationMap result = call(
                bundle, prompt,
                Map.of(
                        "chamberNames", String.join(", ", chamberNames),
                        "jsonFormat", jsonFormat
                ),
                true, false, false,
                ChamberVentilationMap.class
        );
        return result != null ? result.values() : Map.of();
    }

    private Map<String, Insulation> detectInsulation(RawBundle bundle, List<String> chamberNames) {
        String jsonFormat = """
                {"values":{"Камера 1":{"material":{"value":"сэндвич-панель пенополиуретан"},"thickness":{"value":"150мм"}}}}
                """;

        String prompt = """
                Ты — инженер-холодильщик.
                
                Для каждого помещения найди параметры ограждающих конструкций:
                - material: материал теплоизоляции
                - thickness: толщина теплоизоляции в мм
                
                Ищи в тексте ТЗ. Толщина обычно указывается отдельно для средне- и низкотемпературных камер.
                Если для всех камер одинаковый материал — примени ко всем.
                Если не найдено — верни null. Не придумывай.
                
                Список помещений: {chamberNames}
                
                Верни ТОЛЬКО валидный JSON без markdown-блоков.
                Формат: {jsonFormat}
                
                Текст ТЗ: {specText}
                """;

        ChamberInsulationMap result = call(
                bundle, prompt,
                Map.of(
                        "chamberNames", String.join(", ", chamberNames),
                        "jsonFormat", jsonFormat
                ),
                true, false, false,
                ChamberInsulationMap.class
        );
        return result != null ? result.values() : Map.of();
    }

    private Map<String, Doors> detectDoors(RawBundle bundle, List<String> chamberNames) {
        String jsonFormat = """
                {"values":{"Камера 1":{"count":{"value":"2"},"type":{"value":"распашная"},"openingTime":{"value":"по расчёту"}}}}
                """;

        String prompt = """
                Ты — инженер-холодильщик.
                
                Для каждого помещения найди информацию о дверях и воротах/доках:
                - count: количество дверей и ворот
                - type: тип (распашная, откатная, секционные ворота, докшелтер и т.д.)
                - openingTime: время открытия в минутах за смену или процент времени.
                  Если указано "по расчёту" — верни "по расчёту".
                
                Ищи в тексте ТЗ и на планировке.
                Экспедиции имеют доки/ворота для грузовых машин.
                Камеры хранения имеют двери.
                Если не найдено — верни null. Не придумывай.
                
                Список помещений: {chamberNames}
                
                Верни ТОЛЬКО валидный JSON без markdown-блоков.
                Формат: {jsonFormat}
                
                Текст ТЗ: {specText}
                """;

        ChamberDoorsMap result = call(
                bundle, prompt,
                Map.of(
                        "chamberNames", String.join(", ", chamberNames),
                        "jsonFormat", jsonFormat
                ),
                true, true, false,
                ChamberDoorsMap.class
        );
        return result != null ? result.values() : Map.of();
    }

    private <T> T call(RawBundle bundle,
                       String promptTemplate,
                       Map<String, String> params,
                       boolean withSpecImages,
                       boolean withLayoutImages,
                       boolean withTurnoverImages,
                       Class<T> responseType) {
        try {
            BeanOutputConverter<T> converter = new BeanOutputConverter<>(responseType);

            var userSpec = chatClient.prompt()
                    .user(u -> {
                        u.text(promptTemplate).param("specText", bundle.specText());
                        params.forEach(u::param);

                        if (withSpecImages) {
                            bundle.specImages().forEach(img ->
                                    u.media(MimeTypeUtils.IMAGE_PNG, new ByteArrayResource(img)));
                        }
                        if (withLayoutImages) {
                            bundle.layoutImages().forEach(img ->
                                    u.media(MimeTypeUtils.IMAGE_PNG, new ByteArrayResource(img)));
                        }
                        if (withTurnoverImages) {
                            bundle.turnoverImages().forEach(img ->
                                    u.media(MimeTypeUtils.IMAGE_PNG, new ByteArrayResource(img)));
                        }
                    });

            String response = userSpec.call().content();
            return converter.convert(response);
        } catch (Exception e) {
            log.error("Ошибка при вызове LLM для {}: {}", responseType.getSimpleName(), e.getMessage());
            return null;
        }
    }

    private <T> CompletableFuture<T> async(java.util.concurrent.Callable<T> task) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return task.call();
            } catch (Exception e) {
                log.error("Ошибка в async задаче: {}", e.getMessage());
                return null;
            }
        }, executor);
    }

    private record ChamberNameList(List<String> names) {
    }

    private record ChamberValueMap(Map<String, String> values) {
    }

    private record ChamberVolumeMap(Map<String, DailyVolume> values) {
    }

    private record ChamberDimensionsMap(Map<String, Dimensions> values) {
    }

    private record ChamberAdjacentMap(Map<String, AdjacentSpaces> values) {
    }

    private record ChamberProductMap(Map<String, ProductInfo> values) {
    }

    private record ChamberHeatSourcesMap(Map<String, HeatSources> values) {
    }

    private record ChamberVentilationMap(Map<String, Ventilation> values) {
    }

    private record ChamberInsulationMap(Map<String, Insulation> values) {
    }

    private record ChamberDoorsMap(Map<String, Doors> values) {
    }
}
