package net.protsenko.refrigeration.engineer.service.processor;

import org.apache.poi.openxml4j.util.ZipSecureFile;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFPictureData;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class DocxProcessor implements FileProcessor {

    @Override
    public RawContent process(MultipartFile file) throws Exception {
        ZipSecureFile.setMinInflateRatio(0);
        try (XWPFDocument doc = new XWPFDocument(file.getInputStream())) {
            String text = extractText(doc);
            List<byte[]> images = extractImages(doc);
            return new RawContent(cleanText(text), images);
        }
    }

    private String cleanText(String text) {
        return text
                .replaceAll("\\t", " ")           // табы в пробел
                .replaceAll(" {2,}", " ")          // множественные пробелы в один
                .replaceAll("(?m)^\\s+$", "")      // строки только из пробелов
                .replaceAll("\n{3,}", "\n\n")       // больше двух переносов в два
                .strip();
    }

    private String extractText(XWPFDocument doc) {
        StringBuilder sb = new StringBuilder();

        // параграфы
        doc.getParagraphs().forEach(p -> {
            if (!p.getText().isBlank()) sb.append(p.getText()).append("\n");
        });

        // таблицы
        doc.getTables().forEach(table -> {
            table.getRows().forEach(row -> {
                String rowText = row.getTableCells().stream()
                        .map(c -> c.getText().strip())
                        .filter(t -> !t.isBlank())
                        .collect(Collectors.joining(" | "));
                if (!rowText.isBlank()) sb.append(rowText).append("\n");
            });
        });

        return sb.toString();
    }

    private List<byte[]> extractImages(XWPFDocument doc) {
        return doc.getAllPictures().stream()
                .map(XWPFPictureData::getData)
                .toList();
    }
}
