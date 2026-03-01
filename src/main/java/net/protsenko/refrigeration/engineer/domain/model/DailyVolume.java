package net.protsenko.refrigeration.engineer.domain.model;

public record DailyVolume(
        String tons,       // тонн/сутки — если есть напрямую
        String pallets,    // паллет/сутки — если есть
        String palletWeight // средний вес паллеты, кг — для расчёта тоннажа
) {
}