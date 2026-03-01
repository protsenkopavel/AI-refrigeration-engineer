package net.protsenko.refrigeration.engineer.service.processor;

import java.util.List;

public record RawContent(
        String text,
        List<byte[]> images
) {
    public static RawContent empty() {
        return new RawContent("", List.of());
    }
}
