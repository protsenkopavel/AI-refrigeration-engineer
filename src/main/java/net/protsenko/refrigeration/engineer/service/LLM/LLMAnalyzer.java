package net.protsenko.refrigeration.engineer.service.LLM;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.protsenko.refrigeration.engineer.domain.model.AnalysisResult;
import net.protsenko.refrigeration.engineer.domain.model.ChamberSpecList;
import net.protsenko.refrigeration.engineer.domain.model.ValidationResult;
import net.protsenko.refrigeration.engineer.service.processor.ImagePreprocessor;
import net.protsenko.refrigeration.engineer.service.processor.ProcessedBundle;
import net.protsenko.refrigeration.engineer.service.processor.RawBundle;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.stereotype.Service;
import org.springframework.util.MimeTypeUtils;

@Slf4j
@Service
@RequiredArgsConstructor
public class LLMAnalyzer {

    private final ChatClient chatClient;
    private final ImagePreprocessor imagePreprocessor;
    private final ObjectMapper objectMapper;

    public AnalysisResult analyze(RawBundle rawBundle) {
        ProcessedBundle bundle = imagePreprocessor.process(rawBundle);
        String freeFormText = extractAll(bundle);
        ChamberSpecList specList = structurize(freeFormText);
        ValidationResult validation = validate(freeFormText, specList);

        return new AnalysisResult(specList, validation, freeFormText);
    }

    String extractAll(ProcessedBundle bundle) {
        String prompt = """
                Ты — опытный инженер-холодильщик. Перед тобой техническое задание на проектирование
                холодильного склада, планировочное решение и таблица грузооборота (если приложены).
                Также приложен OCR-текст планировок (может содержать ошибки распознавания — сверяй с изображениями).
                
                Проанализируй ВСЕ предоставленные материалы и извлеки максимум информации.
                
                ## 1. ПОМЕЩЕНИЯ
                Перечисли все помещения, подлежащие холодоснабжению: камеры хранения, морозильники,
                тамбуры, экспедиции с охлаждением. Не включай сухие склады, офисы, технические помещения.
                Не включай общие названия ("холодильный склад") — только конкретные помещения.
                Если видны несколько одинаковых зон — перечисли каждую отдельно.
                
                ## 2. ТЕМПЕРАТУРНЫЙ РЕЖИМ
                Для каждого помещения: расчётная температура хранения, допуск, влажность если указана.
                
                ## 3. ГАБАРИТЫ
                Для каждого помещения: длина, ширина, высота, площадь.
                Используй масштаб чертежа и сетку строительных осей с размерными цепочками.
                Стандартные пролёты: 6м, 12м, 24м.
                ВАЖНО: для каждого значения укажи источник (текст ТЗ / экспликация / чертёж / аннотация)
                и уровень уверенности (высокая / средняя / низкая).
                
                ## 4. СМЕЖНЫЕ ПОМЕЩЕНИЯ
                Для каждого помещения определи, что граничит с каждой стороны
                (север, юг, восток, запад, потолок, пол) и температуру там.
                Улица = "улица", грунт = "грунт". Если не определено — так и напиши.
                
                ## 5. СУТОЧНЫЙ ГРУЗООБОРОТ
                Для каждого помещения: тонны/сутки, паллеты/сутки, средний вес паллета в кг.
                
                ## 6. ПРОДУКЦИЯ
                Для каждого помещения: тип продукции, температура при загрузке, температура хранения.
                
                ## 7. ИСТОЧНИКИ ТЕПЛОВЫДЕЛЕНИЯ
                Для каждого помещения перечисли ВЕСЬ персонал по ролям с количеством:
                (комплектовщики, водители погрузчиков, ритрачисты, перевозчики паллет и т.д.)
                и ВСЁ оборудование с количеством и мощностью в кВт если указана:
                (погрузчики, ритраки, паллетообмотчики, поломоечные машины и т.д.)
                
                ## 8. ВЕНТИЛЯЦИЯ
                Для каждого помещения: объём приточного воздуха (м³/час или кратность),
                температура притока. Если "по расчёту исходя из X м³/час на человека" — сохрани формулировку.
                
                ## 9. ТЕПЛОИЗОЛЯЦИЯ
                Для каждого помещения: материал и толщина ограждающих конструкций.
                
                ## 10. ДВЕРИ И ВОРОТА
                Для каждого помещения перечисли ВСЕ двери/ворота: тип каждой
                (распашная, откатная, секционные ворота, докшелтер),
                размеры если указаны, время открытия, примечания (ПВХ-завеса, запираемая и т.д.)
                
                ## 11. ИНЖЕНЕРНЫЕ ЗАМЕТКИ
                Любые требования, которые не вписываются в пункты выше, но важны для проектирования:
                тип оттайки воздухоохладителей, обогрев грунта, система "Человек в камере", тип хладоносителя
                тип теплоносителя и т.д.
                
                ## 12. НЕДОСТАЮЩИЕ ДАННЫЕ
                Перечисли, каких данных не хватает в исходных материалах для полноценного расчёта
                холодильной нагрузки.
                
                ПРАВИЛА:
                - Если параметр не найден — явно напиши "не найден" и почему.
                - НЕ ПРИДУМЫВАЙ значения.
                - Для каждого числового значения указывай источник и уверенность.
                - Отвечай подробно и структурированно.
                
                Текст ТЗ:
                {specText}
                
                Таблица грузооборота:
                {turnoverText}
                
                OCR-текст планировок (может содержать ошибки):
                {ocrText}
                """;

        try {
            return chatClient.prompt()
                    .user(u -> {
                        u.text(prompt)
                                .param("specText", bundle.specText())
                                .param("turnoverText",
                                        bundle.turnoverText().isBlank() ? "не предоставлена" : bundle.turnoverText())
                                .param("ocrText",
                                        bundle.layoutOcrText().isBlank() ? "OCR не выполнен" : bundle.layoutOcrText());

                        bundle.specImages().forEach(img ->
                                u.media(MimeTypeUtils.IMAGE_PNG, new ByteArrayResource(img)));
                        bundle.layoutTiles().forEach(img ->
                                u.media(MimeTypeUtils.IMAGE_PNG, new ByteArrayResource(img)));
                        bundle.turnoverImages().forEach(img ->
                                u.media(MimeTypeUtils.IMAGE_PNG, new ByteArrayResource(img)));
                    })
                    .call()
                    .content();
        } catch (Exception e) {
            log.error("Этап A (extractAll) ошибка: {}", e.getMessage(), e);
            throw new LLMAnalysisException("Не удалось извлечь данные из ТЗ", e);
        }
    }

    ChamberSpecList structurize(String freeFormText) {
        BeanOutputConverter<ChamberSpecList> converter = new BeanOutputConverter<>(ChamberSpecList.class);
        String jsonSchema = converter.getFormat();

        String prompt = """
                Ты — инженер-холодильщик. Ниже — отчёт об анализе технического задания на холодоснабжение.
                Преобразуй его в структурированный JSON.
                
                ПРАВИЛА:
                1. Если в отчёте сказано "не найден" / "не определён" — ставь null.
                2. НЕ ПРИДУМЫВАЙ значения, которых нет в отчёте.
                3. Для SpecValue.confidence используй:
                   - HIGH   — значение явно указано в тексте ТЗ или таблице
                   - MEDIUM — считано с чертежа или выведено из контекста
                   - LOW    — нечёткое изображение, косвенные данные, отчёт выражает сомнение
                   - INFERRED — додумано по инженерной логике
                4. В heatSources.staff — перечисли ВСЕ роли из отчёта как отдельные StaffEntry.
                5. В heatSources.equipment — перечисли ВСЁ оборудование как отдельные EquipmentEntry.
                6. В doors.entries — каждая дверь/ворота отдельным DoorEntry.
                7. В engineeringNotes камеры — заметки, специфичные для этого помещения.
                8. В globalRequirements — общие требования ко всему объекту.
                9. В missingData — все пункты из раздела "Недостающие данные".
                
                ПРАВИЛА ДЛЯ ЭКСПЕДИЦИЙ (ВАЖНО — предотвращает двойной счёт нагрузки):
                10. Если в отчёте одна суммарная экспедиция С известной площадью, а подзоны (приёмка/отгрузка)
                    видны на плане но без отдельных площадей — создай ОДНУ запись в chambers[] с суммарной
                    площадью, а подзоны помести в subZones[] с dataStatus=INFERRED.
                    НЕ создавай три отдельных chambers[] для одной физической зоны.
                11. Если подзоны имеют СОБСТВЕННЫЕ площади, подтверждённые в экспликации — тогда создай
                    их как отдельные chambers[] с dataStatus=CONFIRMED и убери суммарную запись.
                
                ПРАВИЛА ДЛЯ heatSources (ВАЖНО — различай ноль и неизвестность):
                12. Если в отчёте явно сказано "0" или "не используется" — ставь count=0 (это подтверждённый ноль).
                13. Если в отчёте сказано "не читается", "нет данных", "не заполнено" — ставь
                    heatSourcesStatus=NOT_READABLE и оставь staff=[] и equipment=[], добавив в
                    engineeringNotes: "heatSources не читаются, не интерпретировать как 0".
                14. Никогда не оставляй heatSourcesStatus=null при пустых массивах.
                
                ПРАВИЛА ДЛЯ ВЕНТИЛЯЦИИ:
                15. ventilation.airVolume.value должно содержать ОДНО числовое значение или формулу.
                    Если режимов несколько (фрикулинг / общеобменная) — запиши как два отдельных поля
                    airVolumeFreecooling и airVolumeGeneral, либо помести оба значения через " / "
                    и добавь пояснение в engineeringNotes.
                
                Правило 16 (ориентация плана):
                Если на чертеже отсутствует явная стрелка «север», применяй конвенциональную ориентацию:
                - СЕВЕР = верхний край листа
                - ЮГ = нижний край листа
                - ВОСТОК = правый край листа
                - ЗАПАД = левый край листа
                Используй эту ориентацию для заполнения adjacentSpaces.north/south/east/west.
                НЕ пиши в engineeringNotes "север не определён" — он определён конвенционально.
                Если есть явные признаки, что план повёрнут (надпись "С" со стрелкой, роза ветров) — использовать их вместо конвенции.
                
                Правило 17 (симметрия смежностей):
                Когда заполняешь adjacentSpaces — после завершения всех камер пройди по списку ещё раз:
                для каждой заполненной смежности A→B проверь, заполнена ли обратная B→A.
                Если нет — заполни её с confidence=INFERRED.
                Не трогай уже заполненные поля.
                
                Верни ТОЛЬКО валидный JSON без markdown-блоков.
                
                JSON-схема:
                {jsonSchema}
                
                Отчёт:
                {report}
                """;

        try {
            String response = chatClient.prompt()
                    .user(u -> u.text(prompt)
                            .param("jsonSchema", jsonSchema)
                            .param("report", freeFormText))
                    .call()
                    .content();

            return converter.convert(response);
        } catch (LLMAnalysisException e) {
            throw e;
        } catch (Exception e) {
            log.error("Этап B (structurize) ошибка: {}", e.getMessage(), e);
            throw new LLMAnalysisException("Не удалось структурировать данные", e);
        }
    }

    ValidationResult validate(String freeFormText, ChamberSpecList specList) {
        BeanOutputConverter<ValidationResult> converter = new BeanOutputConverter<>(ValidationResult.class);
        String jsonSchema = converter.getFormat();

        String structuredJson;
        try {
            structuredJson = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(specList);
        } catch (Exception e) {
            log.error("Не удалось сериализовать ChamberSpecList для валидации", e);
            return new ValidationResult(false, java.util.List.of(), java.util.List.of("Ошибка сериализации JSON"));
        }

        String prompt = """
                Ты — инженер-холодильщик и QA-специалист.
                
                Ниже два представления одних и тех же данных:
                1. Свободный текст отчёта (источник истины)
                2. Структурированный JSON (может содержать ошибки преобразования)
                
                Найди ВСЕ расхождения:
                
                - Потерянные данные: в тексте есть, в JSON — null или отсутствует
                - Искажённые значения: числа/текст отличаются от исходного отчёта
                - Перепутанные помещения: значения присвоены не тому помещению
                - Подозрительные паттерны: одинаковое значение у всех камер без явного обоснования
                - Потерянные инженерные заметки
                - Потерянные позиции персонала/оборудования
                
                ДОПОЛНИТЕЛЬНЫЕ ПРОВЕРКИ:
                - Двойной счёт экспедиций: если одна и та же физическая зона присутствует
                  одновременно как суммарный chambers[] И как отдельные subZones/chambers[] —
                  это расхождение типа DUPLICATE_ZONE, пометить как critical.
                - heatSources без статуса: если heatSources.staff=[] и heatSources.equipment=[]
                  при heatSourcesStatus=null — пометить как предупреждение NOT_READABLE_HEAT_SOURCES,
                  т.к. пустые массивы неотличимы от нуля.
                - ventilation.airVolume с несколькими режимами в одной строке — пометить как
                  предупреждение MULTI_VALUE_IN_SINGLE_FIELD.
                
                valid = true только если расхождений типа DUPLICATE_ZONE нет и critical-ошибок нет.
                
                Для каждого расхождения укажи: камеру (или "global"), поле, значение в тексте,
                значение в JSON, severity (critical / warning / info) и рекомендацию по исправлению.
                
                - ASYMMETRIC_ADJACENCY (warning): камера A указана соседом B, но B не указывает A как соседа с противоположной стороны.
                
                Верни ТОЛЬКО валидный JSON без markdown-блоков.
                
                JSON-схема ответа:
                {jsonSchema}
                
                === СВОБОДНЫЙ ТЕКСТ ОТЧЁТА ===
                {report}
                
                === СТРУКТУРИРОВАННЫЙ JSON ===
                {structured}
                """;

        try {
            String response = chatClient.prompt()
                    .user(u -> u.text(prompt)
                            .param("jsonSchema", jsonSchema)
                            .param("report", freeFormText)
                            .param("structured", structuredJson))
                    .call()
                    .content();

            ValidationResult result = converter.convert(response);
            return result != null
                    ? result
                    : new ValidationResult(false, java.util.List.of(),
                    java.util.List.of("Не удалось распарсить результат валидации"));
        } catch (Exception e) {
            log.error("Этап C (validate) ошибка: {}", e.getMessage(), e);
            return new ValidationResult(false, java.util.List.of(),
                    java.util.List.of("Ошибка валидации: " + e.getMessage()));
        }
    }


    public static class LLMAnalysisException extends RuntimeException {
        public LLMAnalysisException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}