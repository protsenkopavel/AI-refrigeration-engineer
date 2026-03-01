package net.protsenko.refrigeration.engineer.service.processor;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@Component
public class XlsxProcessor implements FileProcessor {

    @Override
    public RawContent process(MultipartFile file) throws Exception {
        try (Workbook workbook = new XSSFWorkbook(file.getInputStream())) {
            String text = extractText(workbook);
            return new RawContent(text, List.of());
        }
    }

    private String extractText(Workbook workbook) {
        StringBuilder sb = new StringBuilder();
        for (Sheet sheet : workbook) {
            sb.append("Лист: ").append(sheet.getSheetName()).append("\n");
            for (Row row : sheet) {
                String rowText = StreamSupport.stream(row.spliterator(), false)
                        .map(this::cellValue)
                        .filter(t -> !t.isBlank())
                        .collect(Collectors.joining(" | "));
                if (!rowText.isBlank()) sb.append(rowText).append("\n");
            }
        }
        return sb.toString();
    }

    private String cellValue(Cell cell) {
        return switch (cell.getCellType()) {
            case STRING  -> cell.getStringCellValue();
            case NUMERIC -> String.valueOf(cell.getNumericCellValue());
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            default      -> "";
        };
    }
}
