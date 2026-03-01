package net.protsenko.refrigeration.engineer.domain.model;

import java.util.List;

public record Doors(
        List<DoorEntry> entries
) {}
