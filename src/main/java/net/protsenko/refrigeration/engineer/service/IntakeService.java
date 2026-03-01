package net.protsenko.refrigeration.engineer.service;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import net.protsenko.refrigeration.engineer.domain.model.AnalysisResult;
import net.protsenko.refrigeration.engineer.service.LLM.LLMAnalyzer;
import net.protsenko.refrigeration.engineer.service.processor.FileProcessorFactory;
import net.protsenko.refrigeration.engineer.service.processor.RawBundle;
import net.protsenko.refrigeration.engineer.service.processor.RawContent;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class IntakeService {

    private final FileProcessorFactory factory;
    private final LLMAnalyzer llmAnalyzer;

    @SneakyThrows
    public AnalysisResult upload(MultipartFile spec, MultipartFile layout, MultipartFile turnover) {
        RawContent specContent = factory.getProcessor(spec).process(spec);
        RawContent layoutContent = layout != null ? factory.getProcessor(layout).process(layout) : RawContent.empty();
        RawContent turnoverContent = turnover != null ? factory.getProcessor(turnover).process(turnover) : RawContent.empty();

        RawBundle bundle = new RawBundle(
                specContent.text(),
                turnoverContent.text(),
                specContent.images(),
                layoutContent.images(),
                turnoverContent.images()
        );

        return llmAnalyzer.analyze(bundle);
    }
}