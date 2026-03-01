package net.protsenko.refrigeration.engineer.config;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;

@Component
@RequiredArgsConstructor
public class PromptRegistry {

    // Spring сам загрузит файл из classpath
    @Value("classpath:prompts/extract.st")
    private Resource extractPrompt;

    @Value("classpath:prompts/structurize.st")
    private Resource structurizePrompt;

    @Value("classpath:prompts/validate.st")
    private Resource validatePrompt;

    public String load(Resource resource) {
        try {
            return resource.getContentAsString(StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException("Не удалось загрузить промпт", e);
        }
    }

    public String extract()     { return load(extractPrompt); }
    public String structurize() { return load(structurizePrompt); }
    public String validate()    { return load(validatePrompt); }
}
