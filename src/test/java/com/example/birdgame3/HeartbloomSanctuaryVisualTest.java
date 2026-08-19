package com.example.birdgame3;

import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HeartbloomSanctuaryVisualTest {
    private static final Path CAPTURE = Path.of(
            "src/main/resources/stage-previews/variant-heartbloom-sanctuary.png");

    @Test
    void captureKeepsItsEclipseAndLayeredBotanicalIdentity() throws IOException {
        BufferedImage image = ImageIO.read(CAPTURE.toFile());
        assertNotNull(image, "Heartbloom Sanctuary capture");
        assertEquals(640, image.getWidth());
        assertEquals(360, image.getHeight());

        int brightPixels = 0;
        int botanicalPixels = 0;
        int supportedLowerPixels = 0;
        int totalPixels = image.getWidth() * image.getHeight();
        int lowerPixels = image.getWidth() * (image.getHeight() - 190);
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                int rgb = image.getRGB(x, y);
                int red = (rgb >>> 16) & 0xFF;
                int green = (rgb >>> 8) & 0xFF;
                int blue = rgb & 0xFF;
                double luminance = luminance(rgb);
                if (luminance >= 120.0) brightPixels++;

                boolean botanicalGreen = green >= 52
                        && green >= red * 1.14
                        && green >= blue * 0.80;
                if (botanicalGreen) botanicalPixels++;
                if (y >= 190 && botanicalGreen) supportedLowerPixels++;
            }
        }

        assertTrue(brightPixels / (double) totalPixels >= 0.015,
                "the eclipse, nectar cores, and updrafts must remain readable at card size");
        assertTrue(botanicalPixels / (double) totalPixels >= 0.09,
                "Heartbloom must read as a living sanctuary rather than a purple gradient");
        assertTrue(supportedLowerPixels / (double) lowerPixels >= 0.08,
                "rooted stems and leaves must visibly support the flower platforms");

        double eclipseCenter = luminance(image.getRGB(320, 56));
        double eclipseCorona = luminance(image.getRGB(361, 56));
        assertTrue(eclipseCenter <= 35.0,
                "the eclipse needs a dark, unmistakable center");
        assertTrue(eclipseCorona - eclipseCenter >= 120.0,
                "the eclipse corona must remain a strong sanctuary focal point");
    }

    private static double luminance(int rgb) {
        int red = (rgb >>> 16) & 0xFF;
        int green = (rgb >>> 8) & 0xFF;
        int blue = rgb & 0xFF;
        return red * 0.2126 + green * 0.7152 + blue * 0.0722;
    }
}
