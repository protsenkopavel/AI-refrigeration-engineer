package net.protsenko.refrigeration.engineer.service.processor;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;

@Component
public class PdfProcessor implements FileProcessor {

    @Override
    public RawContent process(MultipartFile file) throws Exception {
        try (PDDocument doc = Loader.loadPDF(file.getBytes())) {
            List<byte[]> pages = renderPages(doc);
            return new RawContent("", pages); // текст не извлекаем — всё через Vision
        }
    }

    private List<byte[]> renderPages(PDDocument doc) throws Exception {
        PDFRenderer renderer = new PDFRenderer(doc);
        List<byte[]> pages = new ArrayList<>();

        for (int i = 0; i < doc.getNumberOfPages(); i++) {
            BufferedImage image = renderer.renderImageWithDPI(i, 300);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(image, "PNG", baos);
            pages.add(baos.toByteArray());
        }
        return pages;
    }
}