package net.protsenko.refrigeration.engineer.service.processor;

import java.util.List;

public record RawBundle(
        String specText,
        List<byte[]> specImages,
        List<byte[]> layoutImages,
        List<byte[]> turnoverImages,
        String turnoverText
) {}
