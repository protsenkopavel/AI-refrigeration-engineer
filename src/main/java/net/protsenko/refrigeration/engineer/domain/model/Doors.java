package net.protsenko.refrigeration.engineer.domain.model;

public record Doors(
        SpecValue count,
        SpecValue type,
        SpecValue openingTime
) {}
