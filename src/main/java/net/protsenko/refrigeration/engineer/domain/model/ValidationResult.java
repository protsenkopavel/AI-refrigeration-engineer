package net.protsenko.refrigeration.engineer.domain.model;

import java.util.List;

public record ValidationResult(
        boolean valid,
        List<Discrepancy> discrepancies,
        List<String> warnings
) {
    public record Discrepancy(
            String chamber,
            String field,
            String freeFormValue,
            String structuredValue,
            String suggestion
    ) {
    }
}
