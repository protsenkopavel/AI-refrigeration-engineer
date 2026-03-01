package net.protsenko.refrigeration.engineer.domain.model;

public record SpecValue(
        String value,
        Confidence confidence
) {
    public enum Confidence {
        HIGH,      // явно указано в ТЗ/таблице
        MEDIUM,    // считано с чертежа / выведено из контекста
        LOW,       // нечёткое изображение / косвенные данные
        INFERRED,  // додумано по инженерной логике (например, высота по аналогии)
        UNKNOWN    // уровень уверенности не определён
    }
}