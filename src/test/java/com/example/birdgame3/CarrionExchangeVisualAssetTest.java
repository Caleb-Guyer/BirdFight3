package com.example.birdgame3;

import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CarrionExchangeVisualAssetTest {
    private static final Path PREVIEW_ROOT = Path.of("src/main/resources/stage-previews");
    private static final List<String> CAPTURES = List.of(
            "main-carrion-exchange.png",
            "variant-sorting-floor.png",
            "variant-reclamation-core.png");

    @Test
    void exchangeCapturesRetainReadableHighlightsAndIndustrialDepth() throws IOException {
        for (String name : CAPTURES) {
            BufferedImage image = read(name);
            assertEquals(640, image.getWidth(), name);
            assertEquals(360, image.getHeight(), name);

            int brightPixels = 0;
            int darkPixels = 0;
            double luminanceTotal = 0.0;
            int pixelCount = image.getWidth() * image.getHeight();
            for (int y = 0; y < image.getHeight(); y++) {
                for (int x = 0; x < image.getWidth(); x++) {
                    int rgb = image.getRGB(x, y);
                    int red = (rgb >>> 16) & 0xFF;
                    int green = (rgb >>> 8) & 0xFF;
                    int blue = rgb & 0xFF;
                    double luminance = red * 0.2126 + green * 0.7152 + blue * 0.0722;
                    luminanceTotal += luminance;
                    if (luminance >= 105.0) brightPixels++;
                    if (luminance <= 24.0) darkPixels++;
                }
            }

            assertTrue(luminanceTotal / pixelCount >= 28.0,
                    name + " must not collapse into an unreadably dark silhouette");
            assertTrue(brightPixels / (double) pixelCount >= 0.035,
                    name + " needs enough lit structure to read at stage-card size");
            assertTrue(darkPixels / (double) pixelCount >= 0.25,
                    name + " should preserve the Exchange's deep suspended-city contrast");
        }
    }

    @Test
    void eachExchangeVariantHasASeparateVisualIdentity() throws IOException {
        for (int first = 0; first < CAPTURES.size(); first++) {
            for (int second = first + 1; second < CAPTURES.size(); second++) {
                BufferedImage a = read(CAPTURES.get(first));
                BufferedImage b = read(CAPTURES.get(second));
                double difference = averageColorDifference(a, b);
                assertTrue(difference >= 0.06,
                        CAPTURES.get(first) + " and " + CAPTURES.get(second)
                                + " look too similar: " + difference);
            }
        }
    }

    private static BufferedImage read(String name) throws IOException {
        BufferedImage image = ImageIO.read(PREVIEW_ROOT.resolve(name).toFile());
        assertNotNull(image, name);
        return image;
    }

    private static double averageColorDifference(BufferedImage a, BufferedImage b) {
        assertEquals(a.getWidth(), b.getWidth());
        assertEquals(a.getHeight(), b.getHeight());
        double total = 0.0;
        for (int y = 0; y < a.getHeight(); y += 2) {
            for (int x = 0; x < a.getWidth(); x += 2) {
                int first = a.getRGB(x, y);
                int second = b.getRGB(x, y);
                total += Math.abs(((first >>> 16) & 0xFF) - ((second >>> 16) & 0xFF));
                total += Math.abs(((first >>> 8) & 0xFF) - ((second >>> 8) & 0xFF));
                total += Math.abs((first & 0xFF) - (second & 0xFF));
            }
        }
        double sampledPixels = Math.ceil(a.getWidth() / 2.0) * Math.ceil(a.getHeight() / 2.0);
        return total / (sampledPixels * 3.0 * 255.0);
    }
}
