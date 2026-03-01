package net.protsenko.refrigeration.engineer.domain.model;

public enum DataStatus {
    CONFIRMED,      // явно указано в ТЗ
    INFERRED,       // выведено по инженерной логике
    UNKNOWN,        // данных нет в принципе
    NOT_READABLE    // данные есть, но нечитаемы (растр, низкое разрешение)
}
