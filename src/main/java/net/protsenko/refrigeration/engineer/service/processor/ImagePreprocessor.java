package net.protsenko.refrigeration.engineer.service.processor;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.ConvolveOp;
import java.awt.image.Kernel;
import java.awt.image.RescaleOp;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ImagePreprocessor {

    private static final int TILE_WIDTH = 2500;
    private static final int TILE_HEIGHT = 2000;
    private static final double TILE_OVERLAP = 0.20;

    private static final float CONTRAST_SCALE = 1.4f;
    private static final float CONTRAST_OFFSET = -20f;

    private final OcrService ocrService;

    public ProcessedBundle process(RawBundle raw) {
        log.info("Предобработка изображений: spec={}, layout={}, turnover={}",
                raw.specImages().size(), raw.layoutImages().size(), raw.turnoverImages().size());

        List<byte[]> enhancedSpec = raw.specImages().stream()
                .map(this::enhanceForReadability)
                .toList();

        List<byte[]> layoutTiles = new ArrayList<>();
        for (byte[] layoutImage : raw.layoutImages()) {
            byte[] enhanced = enhanceForReadability(layoutImage);
            layoutTiles.addAll(splitIntoTiles(enhanced));
        }
        log.info("Планировки: {} исходных → {} тайлов", raw.layoutImages().size(), layoutTiles.size());

        List<byte[]> enhancedTurnover = raw.turnoverImages().stream()
                .map(this::enhanceForReadability)
                .toList();

        String ocrText = extractOcrFromLayouts(raw.layoutImages());

        return new ProcessedBundle(
                raw.specText(),
                raw.turnoverText(),
                enhancedSpec,
                layoutTiles,
                enhancedTurnover,
                ocrText
        );
    }

    byte[] enhanceForReadability(byte[] imageBytes) {
        try {
            BufferedImage img = ImageIO.read(new ByteArrayInputStream(imageBytes));
            if (img == null) {
                log.warn("Не удалось прочитать изображение, возвращаю как есть");
                return imageBytes;
            }

            if (img.getType() != BufferedImage.TYPE_INT_RGB) {
                BufferedImage rgb = new BufferedImage(img.getWidth(), img.getHeight(), BufferedImage.TYPE_INT_RGB);
                Graphics2D g = rgb.createGraphics();
                g.drawImage(img, 0, 0, null);
                g.dispose();
                img = rgb;
            }

            RescaleOp contrastOp = new RescaleOp(CONTRAST_SCALE, CONTRAST_OFFSET, null);
            contrastOp.filter(img, img);

            float[] sharpenKernel = {
                    0, -0.5f, 0,
                    -0.5f, 3f, -0.5f,
                    0, -0.5f, 0
            };
            ConvolveOp sharpenOp = new ConvolveOp(
                    new Kernel(3, 3, sharpenKernel),
                    ConvolveOp.EDGE_NO_OP, null
            );
            img = sharpenOp.filter(img, null);

            return toPngBytes(img);
        } catch (IOException e) {
            log.warn("Ошибка при улучшении изображения: {}", e.getMessage());
            return imageBytes;
        }
    }

    List<byte[]> splitIntoTiles(byte[] imageBytes) {
        try {
            BufferedImage src = ImageIO.read(new ByteArrayInputStream(imageBytes));
            if (src == null) return List.of(imageBytes);

            if (src.getWidth() <= TILE_WIDTH * 1.3 && src.getHeight() <= TILE_HEIGHT * 1.3) {
                return List.of(imageBytes);
            }

            List<byte[]> tiles = new ArrayList<>();
            int stepX = (int) (TILE_WIDTH * (1 - TILE_OVERLAP));
            int stepY = (int) (TILE_HEIGHT * (1 - TILE_OVERLAP));

            for (int y = 0; y < src.getHeight(); y += stepY) {
                for (int x = 0; x < src.getWidth(); x += stepX) {
                    int w = Math.min(TILE_WIDTH, src.getWidth() - x);
                    int h = Math.min(TILE_HEIGHT, src.getHeight() - y);

                    if (w < TILE_WIDTH * 0.3 || h < TILE_HEIGHT * 0.3) continue;

                    BufferedImage tile = src.getSubimage(x, y, w, h);
                    tiles.add(toPngBytes(tile));
                }
            }

            log.debug("Изображение {}x{} → {} тайлов", src.getWidth(), src.getHeight(), tiles.size());
            return tiles.isEmpty() ? List.of(imageBytes) : tiles;
        } catch (IOException e) {
            log.warn("Ошибка при нарезке тайлов: {}", e.getMessage());
            return List.of(imageBytes);
        }
    }

    private String extractOcrFromLayouts(List<byte[]> layoutImages) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < layoutImages.size(); i++) {
            try {
                String text = ocrService.recognize(layoutImages.get(i));
                if (text != null && !text.isBlank()) {
                    sb.append("--- OCR планировки ").append(i + 1).append(" ---\n");
                    sb.append(text.trim()).append("\n\n");
                }
            } catch (Exception e) {
                log.warn("OCR не удался для планировки {}: {}", i + 1, e.getMessage());
            }
        }
        String result = sb.toString().trim();
        log.info("OCR планировок: извлечено {} символов", result.length());
        return result;
    }

    private byte[] toPngBytes(BufferedImage image) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(image, "png", baos);
        return baos.toByteArray();
    }
}