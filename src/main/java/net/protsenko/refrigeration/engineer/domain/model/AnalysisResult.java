package net.protsenko.refrigeration.engineer.domain.model;

public record AnalysisResult(
        ChamberSpecList specList,
        ValidationResult validation,
        String rawFreeFormText
) { }
