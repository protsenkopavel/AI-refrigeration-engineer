package net.protsenko.refrigeration.engineer.domain.model;

public record AdjacentSpaces(
        SpecValue north,
        SpecValue south,
        SpecValue east,
        SpecValue west,
        SpecValue ceiling,
        SpecValue floor
) {}