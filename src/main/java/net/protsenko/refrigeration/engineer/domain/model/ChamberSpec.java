package net.protsenko.refrigeration.engineer.domain.model;

import lombok.Builder;

import java.util.List;

@Builder
public record ChamberSpec(
        String name,
        SpecValue chamberTemp,
        Dimensions dimensions,
        AdjacentSpaces adjacentSpaces,
        DailyVolume dailyVolume,
        ProductInfo productInfo,
        HeatSources heatSources,
        Ventilation ventilation,
        Insulation insulation,
        Doors doors,
        List<String> engineeringNotes,
        DataStatus dataStatus,
        List<ChamberSpec> subZones,
        DataStatus heatSourcesStatus
) {}