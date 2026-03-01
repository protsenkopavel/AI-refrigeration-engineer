package net.protsenko.refrigeration.engineer.service.processor;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.nio.file.Files;
import java.nio.file.Path;

@Slf4j
@Service
public class TesseractOcrService implements OcrService {

    private final boolean available;

    public TesseractOcrService() {
        boolean ok = false;
        try {
            Process p = new ProcessBuilder("tesseract", "--version")
                    .redirectErrorStream(true).start();
            p.waitFor();
            ok = p.exitValue() == 0;
            if (ok) log.info("Tesseract OCR (CLI) доступен");
        } catch (Exception e) {
            log.warn("Tesseract CLI недоступен: {}", e.getMessage());
        }
        this.available = ok;
    }

    @Override
    public String recognize(byte[] imageBytes) {
        if (!available) return "";
        try {
            Path tmp = Files.createTempFile("ocr_", ".png");
            Path preprocessed = Files.createTempFile("ocr_pre_", ".png");
            Path out = Files.createTempFile("ocr_out_", "");
            try {
                Files.write(tmp, imageBytes);

                new ProcessBuilder(
                        "convert", tmp.toString(),
                        "-density", "300",
                        "-units", "PixelsPerInch",
                        "-colorspace", "Gray",
                        "-normalize",
                        "-sharpen", "0x1",
                        preprocessed.toString()
                ).redirectErrorStream(true).start().waitFor();

                Path source = Files.exists(preprocessed) ? preprocessed : tmp;

                new ProcessBuilder(
                        "tesseract", source.toString(), out.toString(),
                        "-l", "rus+eng",
                        "--psm", "6",
                        "--oem", "3",
                        "-c", "preserve_interword_spaces=1"
                ).redirectErrorStream(true).start().waitFor();

                Path txtFile = out.resolveSibling(out.getFileName() + ".txt");
                return Files.exists(txtFile) ? Files.readString(txtFile) : "";
            } finally {
                Files.deleteIfExists(tmp);
                Files.deleteIfExists(preprocessed);
                Files.deleteIfExists(out.resolveSibling(out.getFileName() + ".txt"));
            }
        } catch (Exception e) {
            log.warn("OCR ошибка: {}", e.getMessage());
            return "";
        }
    }
}