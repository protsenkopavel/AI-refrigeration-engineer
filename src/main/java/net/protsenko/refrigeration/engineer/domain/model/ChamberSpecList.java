package net.protsenko.refrigeration.engineer.domain.model;

import java.util.List;

public record ChamberSpecList(
        List<ChamberSpec> chambers,
        List<String> globalRequirements,
        List<String> missingData
) {}
