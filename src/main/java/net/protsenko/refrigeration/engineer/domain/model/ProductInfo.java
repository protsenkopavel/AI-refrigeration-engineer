package net.protsenko.refrigeration.engineer.domain.model;

public record ProductInfo(
        SpecValue productType,
        SpecValue loadingTemp,
        SpecValue storageTemp
) {}