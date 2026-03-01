package net.protsenko.refrigeration.engineer.service.LLM;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.protsenko.refrigeration.engineer.config.PromptRegistry;
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
    private final PromptRegistry prompts;

    public AnalysisResult analyze(RawBundle rawBundle) {
        ProcessedBundle bundle = imagePreprocessor.process(rawBundle);
        String freeFormText = extractAll(bundle);
        ChamberSpecList specList = structurize(freeFormText, bundle);
        ValidationResult validation = validate(freeFormText, bundle, specList);

        return new AnalysisResult(specList, validation, freeFormText);
    }

    String extractAll(ProcessedBundle bundle) {
        try {
            return chatClient.prompt()
                    .user(u -> {
                        u.text(prompts.extract())
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

    ChamberSpecList structurize(String freeFormText, ProcessedBundle bundle) {
        BeanOutputConverter<ChamberSpecList> converter = new BeanOutputConverter<>(ChamberSpecList.class);
        String jsonSchema = converter.getFormat();
        try {
            String response = chatClient.prompt()
                    .user(u -> {
                        u.text(prompts.structurize())
                                .param("jsonSchema", jsonSchema)
                                .param("report", freeFormText);

                        bundle.layoutTiles().forEach(img ->
                                u.media(MimeTypeUtils.IMAGE_PNG,
                                        new ByteArrayResource(img)));
                    })
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

    ValidationResult validate(String freeFormText, ProcessedBundle bundle, ChamberSpecList specList) {
        BeanOutputConverter<ValidationResult> converter = new BeanOutputConverter<>(ValidationResult.class);
        String jsonSchema = converter.getFormat();

        String structuredJson;
        try {
            structuredJson = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(specList);
        } catch (Exception e) {
            log.error("Не удалось сериализовать ChamberSpecList для валидации", e);
            return new ValidationResult(false, java.util.List.of(), java.util.List.of("Ошибка сериализации JSON"));
        }

        try {
            String response = chatClient.prompt()
                    .user(u -> {
                        u.text(prompts.validate())
                                .param("jsonSchema", jsonSchema)
                                .param("report", freeFormText)
                                .param("structured", structuredJson);

                        bundle.layoutTiles().forEach(img ->
                                u.media(MimeTypeUtils.IMAGE_PNG,
                                        new ByteArrayResource(img)));
                    })
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