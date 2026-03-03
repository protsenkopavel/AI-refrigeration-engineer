package net.protsenko.refrigeration.engineer.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.protsenko.refrigeration.engineer.domain.dto.ExtractionResponse;
import net.protsenko.refrigeration.engineer.service.DocumentService;
import net.protsenko.refrigeration.engineer.service.OpenAiService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ExtractionController {
    private final DocumentService documentService;
    private final OpenAiService openAiService;

    @PostMapping("/extract")
    public ResponseEntity<ExtractionResponse> extract(
            @RequestParam("files") List<MultipartFile> files) {

        log.info("Получено {} файлов для обработки", files.size());
        files.forEach(f -> log.info("  - {} ({} bytes)", f.getOriginalFilename(), f.getSize()));

        var parsed = documentService.parseFiles(files);
        log.info("Извлечено: {} символов текста, {} изображений",
                parsed.text().length(), parsed.base64Images().size());

        ExtractionResponse result = openAiService.extract(parsed.text(), parsed.base64Images());

        return ResponseEntity.ok(result);
    }
}