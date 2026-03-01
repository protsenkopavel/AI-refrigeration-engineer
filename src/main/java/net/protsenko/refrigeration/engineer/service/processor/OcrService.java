package net.protsenko.refrigeration.engineer.service.processor;

public interface OcrService {
    String recognize(byte[] imageBytes);
}
