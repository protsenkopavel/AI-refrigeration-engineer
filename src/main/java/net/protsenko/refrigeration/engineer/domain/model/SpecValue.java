package net.protsenko.refrigeration.engineer.domain.model;

public record SpecValue(
        String value,
        Confidence confidence,
        DataPresence presence
) {
    public enum Confidence {
        HIGH,      // явно указано в ТЗ/таблице
        MEDIUM,    // считано с чертежа / выведено из контекста
        LOW,       // нечёткое изображение / косвенные данные
        INFERRED,  // додумано по инженерной логике (например, высота по аналогии)
        UNKNOWN    // уровень уверенности не определён
    }

    public enum DataPresence {
        PRESENT,    // значение есть в источнике и извлечено
        ABSENT,     // в источнике явно стоит "-" / "не предусмотрено"
        UNKNOWN     // не найдено, непонятно есть ли
    }
}