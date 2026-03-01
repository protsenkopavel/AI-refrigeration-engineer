package net.protsenko.refrigeration.engineer.service.processor;

import lombok.extern.slf4j.Slf4j;
import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;

@Slf4j
@Service
public class TesseractOcrService implements OcrService {

    private final Tesseract tesseract;

    public TesseractOcrService() {
        this.tesseract = new Tesseract();
        this.tesseract.setDatapath("/usr/share/tesseract-ocr/4.00/tessdata");
        this.tesseract.setLanguage("rus+eng");
        this.tesseract.setPageSegMode(6);
    }

    @Override
    public String recognize(byte[] imageBytes) {
        try {
            BufferedImage image = ImageIO.read(new ByteArrayInputStream(imageBytes));
            if (image == null) return "";
            return tesseract.doOCR(image);
        } catch (TesseractException e) {
            log.warn("Tesseract OCR ошибка: {}", e.getMessage());
            return "";
        } catch (Exception e) {
            log.warn("Ошибка чтения изображения для OCR: {}", e.getMessage());
            return "";
        }
    }
}