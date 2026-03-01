package net.protsenko.refrigeration.engineer.service;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import net.protsenko.refrigeration.engineer.domain.model.ChamberSpecList;
import net.protsenko.refrigeration.engineer.service.LLM.LLMAnalyzer;
import net.protsenko.refrigeration.engineer.service.processor.FileProcessorFactory;
import net.protsenko.refrigeration.engineer.service.processor.RawBundle;
import net.protsenko.refrigeration.engineer.service.processor.RawContent;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.pdf.PagePdfDocumentReader;
import org.springframework.ai.reader.tika.TikaDocumentReader;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.util.MimeTypeUtils;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class IntakeService {

    private final FileProcessorFactory factory;
    private final LLMAnalyzer llmAnalyzer;

    @SneakyThrows
    public ChamberSpecList upload(MultipartFile spec,
                                  MultipartFile layout,
                                  MultipartFile turnover) {

        RawContent specContent     = factory.getProcessor(spec).process(spec);
        RawContent layoutContent   = layout   != null ? factory.getProcessor(layout).process(layout)     : RawContent.empty();
        RawContent turnoverContent = turnover != null ? factory.getProcessor(turnover).process(turnover) : RawContent.empty();

        RawBundle bundle = new RawBundle(
                specContent.text(),
                specContent.images(),
                layoutContent.images(),
                turnoverContent.images(),
                turnoverContent.text()
        );

        return llmAnalyzer.analyze(bundle);
    }
}