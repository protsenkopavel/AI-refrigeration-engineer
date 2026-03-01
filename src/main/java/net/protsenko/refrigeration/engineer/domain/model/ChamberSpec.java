package net.protsenko.refrigeration.engineer.domain.model;

import lombok.Builder;

@Builder
public record ChamberSpec(
        String name,
        SpecValue chamberTemp,
        Dimensions dimensions,
        AdjacentSpaces adjacentSpaces,
        DailyVolume dailyVolume,
        SpecValue productType,
        SpecValue loadingTemp,
        SpecValue storageTemp,
        HeatSources heatSources,
        Ventilation ventilation,
        Insulation insulation,
        Doors doors
) {}