package net.protsenko.refrigeration.engineer.service.processor;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

@Component
@RequiredArgsConstructor
public class FileProcessorFactory {

    private final DocxProcessor docxProcessor;
    private final PdfProcessor pdfProcessor;
    private final XlsxProcessor xlsxProcessor;

    public FileProcessor getProcessor(MultipartFile file) {
        String filename = file.getOriginalFilename().toLowerCase();
        if (filename.endsWith(".docx") || filename.endsWith(".doc")) return docxProcessor;
        if (filename.endsWith(".pdf"))  return pdfProcessor;
        if (filename.endsWith(".xlsx") || filename.endsWith(".xls")) return xlsxProcessor;
        throw new IllegalArgumentException("Неподдерживаемый формат: " + filename);
    }
}
