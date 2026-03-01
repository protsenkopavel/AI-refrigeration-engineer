package net.protsenko.refrigeration.engineer.domain.model;

public record EquipmentEntry(
        String name,
        Integer count,
        String powerKw
) {}