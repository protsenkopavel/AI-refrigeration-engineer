package net.protsenko.refrigeration.engineer.domain.model;

public record DoorEntry(
        String type,
        String dimensions,
        String openingTime,
        String notes
) {}
