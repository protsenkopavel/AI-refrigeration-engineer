package net.protsenko.refrigeration.engineer.service;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import net.protsenko.refrigeration.engineer.domain.dto.ExtractionResponse;
import net.protsenko.refrigeration.engineer.domain.model.ChamberData;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.util.MimeTypeUtils;
import tools.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;

@Slf4j
@Service
public class OpenAiService {

    private final ChatClient chatClient;
    private final ObjectMapper mapper;

    private String systemPrompt;

    public OpenAiService(ChatClient.Builder chatClientBuilder, ObjectMapper mapper) {
        this.chatClient = chatClientBuilder.build();
        this.mapper = mapper;
    }

    @PostConstruct
    void init() {
        try {
            var resource = new ClassPathResource("extraction-prompt.txt");
            systemPrompt = new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException("Не удалось загрузить extraction-prompt.txt", e);
        }
    }

    public ExtractionResponse extract(String text, List<String> base64Images) {
        try {
            String userText = "Вот извлечённый текст из документов:\n\n"
                    + (text != null ? text : "(текст отсутствует)");

            log.info("Отправляем запрос в OpenAI (images={})", base64Images.size());

            String content;

            if (base64Images.isEmpty()) {
                content = chatClient.prompt()
                        .system(systemPrompt)
                        .user(userText)
                        .call()
                        .content();
            } else {
                content = chatClient.prompt()
                        .system(systemPrompt)
                        .user(u -> {
                            u.text(userText);
                            for (String img : base64Images) {
                                byte[] bytes = Base64.getDecoder().decode(img);
                                u.media(MimeTypeUtils.IMAGE_PNG, new ByteArrayResource(bytes));
                            }
                        })
                        .call()
                        .content();
            }

            return parseResponse(content);

        } catch (Exception e) {
            log.error("Ошибка при вызове OpenAI: {}", e.getMessage(), e);
            var errorResponse = new ExtractionResponse();
            errorResponse.setWarnings(List.of("Ошибка OpenAI: " + e.getMessage()));
            return errorResponse;
        }
    }

    private ExtractionResponse parseResponse(String content) throws Exception {
        content = content.trim();
        if (content.startsWith("```json")) content = content.substring(7);
        if (content.startsWith("```")) content = content.substring(3);
        if (content.endsWith("```")) content = content.substring(0, content.length() - 3);
        content = content.trim();

        ExtractionResponse result = mapper.readValue(content, ExtractionResponse.class);

        if (result.getChambers() != null) {
            result.getChambers().forEach(this::computeDimensions);
        }

        return result;
    }

    private void computeDimensions(ChamberData chamber) {
        if (chamber.getDimensions() == null) return;
        var d = chamber.getDimensions();

        Double length = d.getLengthA() != null ? d.getLengthA() : d.getLengthB();
        Double width = d.getWidthB() != null ? d.getWidthB() : d.getWidthG();

        if (length != null && width != null) {
            d.setArea(Math.round(length * width * 100.0) / 100.0);
            if (d.getHeight() != null) {
                d.setVolume(Math.round(length * width * d.getHeight() * 100.0) / 100.0);
            }
        }
    }
}