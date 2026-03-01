package net.protsenko.refrigeration.engineer.service.processor;

import java.util.List;

public record RawBundle(
        String specText,
        String turnoverText,
        List<byte[]> specImages,
        List<byte[]> layoutImages,
        List<byte[]> turnoverImages
) {
    public String turnoverText() {
        return turnoverText != null ? turnoverText : "";
    }
}
