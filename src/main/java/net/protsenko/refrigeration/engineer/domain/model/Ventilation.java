package net.protsenko.refrigeration.engineer.domain.model;

public record Ventilation(
        SpecValue airVolume,
        SpecValue supplyTemp
) {}
