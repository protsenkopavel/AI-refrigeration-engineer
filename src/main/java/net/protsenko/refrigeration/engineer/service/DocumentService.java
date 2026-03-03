package net.protsenko.refrigeration.engineer.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.protsenko.refrigeration.engineer.config.AppProperties;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.openxml4j.util.ZipSecureFile;
import org.apache.poi.xwpf.usermodel.*;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentService {
    private final AppProperties props;

    public record ParsedContent(String text, List<String> base64Images) {
    }

    public ParsedContent parseFiles(List<MultipartFile> files) {
        var textParts = new StringBuilder();
        var images = new ArrayList<String>();

        for (MultipartFile file : files) {
            String name = file.getOriginalFilename();
            if (name == null) name = "unknown";
            String lower = name.toLowerCase();

            try {
                if (lower.endsWith(".docx") || lower.endsWith(".doc")) {
                    textParts.append("\n\n=== Документ: ").append(name).append(" ===\n");
                    var docxContent = extractDocxContent(file.getInputStream());
                    textParts.append(docxContent.text());
                    images.addAll(docxContent.base64Images());

                } else if (lower.endsWith(".pdf")) {
                    textParts.append("\n\n=== PDF: ").append(name).append(" ===\n");
                    var pdfContent = extractPdfContent(file.getBytes());
                    textParts.append(pdfContent.text());
                    images.addAll(pdfContent.base64Images());

                } else if (lower.endsWith(".png") || lower.endsWith(".jpg") || lower.endsWith(".jpeg")) {
                    textParts.append("\n\n=== Изображение: ").append(name).append(" ===\n");
                    images.add(Base64.getEncoder().encodeToString(file.getBytes()));

                } else {
                    log.warn("Неподдерживаемый формат файла: {}", name);
                }
            } catch (Exception e) {
                log.error("Ошибка при обработке файла {}: {}", name, e.getMessage(), e);
                textParts.append("\n[Ошибка обработки файла ").append(name).append("]\n");
            }
        }

        return new ParsedContent(textParts.toString().trim(), images);
    }

    private static final List<String> IMAGE_EXTENSIONS = List.of("png", "jpg", "jpeg", "gif", "tiff", "bmp");

    private ParsedContent extractDocxContent(InputStream is) throws Exception {
        ZipSecureFile.setMinInflateRatio(0.001);
        var sb = new StringBuilder();
        var images = new ArrayList<String>();

        try (var doc = new XWPFDocument(is)) {
            for (XWPFParagraph p : doc.getParagraphs()) {
                String text = p.getText();
                if (text != null && !text.isBlank()) {
                    sb.append(text).append("\n");
                }
            }

            for (XWPFTable table : doc.getTables()) {
                sb.append("\n[Таблица]\n");
                for (XWPFTableRow row : table.getRows()) {
                    var cells = new ArrayList<String>();
                    for (XWPFTableCell cell : row.getTableCells()) {
                        cells.add(cell.getText().trim());
                    }
                    sb.append(String.join(" | ", cells)).append("\n");
                }
            }

            for (var picture : doc.getAllPictures()) {
                String ext = picture.suggestFileExtension();
                if (ext != null && IMAGE_EXTENSIONS.contains(ext.toLowerCase())) {
                    images.add(Base64.getEncoder().encodeToString(picture.getData()));
                    log.debug("Извлечено изображение из docx: {} ({} bytes)", ext, picture.getData().length);
                } else {
                    log.debug("Пропущено вложение: расширение={}", ext);
                }
            }
            log.info("Из docx извлечено {} изображений", images.size());
        }

        return new ParsedContent(sb.toString(), images);
    }

    private ParsedContent extractPdfContent(byte[] pdfBytes) throws Exception {
        var sb = new StringBuilder();
        var images = new ArrayList<String>();

        try (PDDocument doc = Loader.loadPDF(pdfBytes)) {
            var stripper = new PDFTextStripper();
            sb.append(stripper.getText(doc));

            var renderer = new PDFRenderer(doc);
            int maxPages = Math.min(doc.getNumberOfPages(), props.getMaxImagePages());
            for (int i = 0; i < maxPages; i++) {
                BufferedImage img = renderer.renderImageWithDPI(i, props.getImageDpi());
                try (var baos = new ByteArrayOutputStream()) {
                    ImageIO.write(img, "png", baos);
                    images.add(Base64.getEncoder().encodeToString(baos.toByteArray()));
                }
            }
        }

        return new ParsedContent(sb.toString(), images);
    }
}