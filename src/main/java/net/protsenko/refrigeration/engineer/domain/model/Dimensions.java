package net.protsenko.refrigeration.engineer.domain.model;

public record Dimensions(
        SpecValue length,
        SpecValue width,
        SpecValue height,
        SpecValue area
) {}
