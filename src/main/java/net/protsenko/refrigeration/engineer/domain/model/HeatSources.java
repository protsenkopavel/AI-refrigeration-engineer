package net.protsenko.refrigeration.engineer.domain.model;

import java.util.List;

public record HeatSources(
        List<StaffEntry> staff,
        List<EquipmentEntry> equipment
) {}