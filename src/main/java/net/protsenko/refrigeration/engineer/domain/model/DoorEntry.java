package net.protsenko.refrigeration.engineer.domain.model;

/**
 * Отдельная дверь/ворота камеры.
 * Камера может иметь несколько дверей разного типа.
 */
public record DoorEntry(
        String type,
        String dimensions,
        String openingTime,
        String notes
) {}
