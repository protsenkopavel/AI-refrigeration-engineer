package net.protsenko.refrigeration.engineer.service.processor;

import java.util.List;

public record ProcessedBundle(
        String specText,
        String turnoverText,
        List<byte[]> specImages,
        List<byte[]> layoutTiles,
        List<byte[]> turnoverImages,
        String layoutOcrText
) {
}
