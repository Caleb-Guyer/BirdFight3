package com.example.birdgame3;

import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SkyCliffsVisualTest {
    private static final Path CAPTURE = Path.of(
            "src/main/resources/stage-previews/main-skycliffs.png");

    @Test
    void captureKeepsItsSixGroundedSpiresCloudSeaAndHighAltitudeLandmark() throws IOException {
        BufferedImage image = ImageIO.read(CAPTURE.toFile());
        assertNotNull(image, "Sky Cliffs capture");
        assertEquals(640, image.getWidth());
        assertEquals(360, image.getHeight());

        int skyPixels = 0;
        int stonePixels = 0;
        int paleAtmospherePixels = 0;
        int cloudSeaPixels = 0;
        int darkFoundationPixels = 0;
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                int rgb = image.getRGB(x, y);
                int red = red(rgb);
                int green = green(rgb);
                int blue = blue(rgb);
                double luminance = luminance(rgb);
                if (blue >= red + 30 && green >= red + 20
                        && luminance >= 50.0 && luminance <= 150.0) skyPixels++;
                if (isCliffStone(rgb)) stonePixels++;
                if (red >= 160 && green >= 175 && blue >= 170) {
                    paleAtmospherePixels++;
                    if (y >= 190 && y <= 285) cloudSeaPixels++;
                }
                if (y >= 290 && luminance <= 85.0) darkFoundationPixels++;
            }
        }

        int totalPixels = image.getWidth() * image.getHeight();
        assertTrue(skyPixels / (double) totalPixels >= 0.36,
                "the arena must retain a broad, readable high-altitude blue atmosphere");
        assertTrue(stonePixels / (double) totalPixels >= 0.50,
                "carved cliff architecture must dominate instead of floating neon shelves");
        assertTrue(paleAtmospherePixels >= 4_500,
                "snow, cloud, sun, and wind highlights must preserve atmospheric depth");
        assertTrue(cloudSeaPixels >= 4_000,
                "the cloud sea must remain visible behind the playable stonework");
        assertTrue(darkFoundationPixels >= 38_000,
                "the lower arena must remain a substantial continuous stone plateau");
        assertTrue(countDarkRow(image, 295, 85.0) >= 635,
                "the real ground collider must remain visible across the complete stage width");

        int[] spireCenters = {32, 152, 272, 392, 512, 608};
        for (int center : spireCenters) {
            assertTrue(countCliffStone(image, Math.max(0, center - 14), 65,
                            Math.min(639, center + 14), 286) >= 4_400,
                    "each authored climbing tower must remain a grounded stone spire near x=" + center);
        }

        assertTrue(countPale(image, 260, 20, 380, 95) >= 600,
                "the summit sun must remain the stage's clear central landmark");
    }

    private static int countCliffStone(BufferedImage image,
                                       int minX, int minY, int maxX, int maxY) {
        int count = 0;
        for (int y = minY; y <= maxY; y++) {
            for (int x = minX; x <= maxX; x++) {
                if (isCliffStone(image.getRGB(x, y))) count++;
            }
        }
        return count;
    }

    private static boolean isCliffStone(int rgb) {
        int red = red(rgb);
        int green = green(rgb);
        int blue = blue(rgb);
        double luminance = luminance(rgb);
        return blue >= green && green >= red
                && blue - green <= 25 && green - red <= 35
                && luminance >= 25.0 && luminance <= 115.0;
    }

    private static int countPale(BufferedImage image,
                                 int minX, int minY, int maxX, int maxY) {
        int count = 0;
        for (int y = minY; y <= maxY; y++) {
            for (int x = minX; x <= maxX; x++) {
                int rgb = image.getRGB(x, y);
                if (red(rgb) >= 160 && green(rgb) >= 175 && blue(rgb) >= 170) count++;
            }
        }
        return count;
    }

    private static int countDarkRow(BufferedImage image, int y, double maximumLuminance) {
        int count = 0;
        for (int x = 0; x < image.getWidth(); x++) {
            if (luminance(image.getRGB(x, y)) <= maximumLuminance) count++;
        }
        return count;
    }

    private static int red(int rgb) {
        return (rgb >>> 16) & 0xFF;
    }

    private static int green(int rgb) {
        return (rgb >>> 8) & 0xFF;
    }

    private static int blue(int rgb) {
        return rgb & 0xFF;
    }

    private static double luminance(int rgb) {
        return red(rgb) * 0.2126 + green(rgb) * 0.7152 + blue(rgb) * 0.0722;
    }
}
