package com.example.birdgame3;

import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TitanDockVisualTest {
    private static final Path CAPTURE = Path.of(
            "src/main/resources/stage-previews/variant-titan-dock.png");

    @Test
    void captureKeepsItsDreadnoughtDrydockAndVisibleWaterHazard() throws IOException {
        BufferedImage image = ImageIO.read(CAPTURE.toFile());
        assertNotNull(image, "Titan Dock capture");
        assertEquals(640, image.getWidth());
        assertEquals(360, image.getHeight());

        assertTrue(countIndustrialPixels(image) >= 52_000,
                "the dreadnought, drydock gantries, barges, and masts must dominate the arena");
        assertTrue(countWaterPixels(image, 275, 359) >= 25_000,
                "open water must remain clearly visible beneath the playable machinery");
        assertTrue(countGoldPixels(image) >= 300,
                "hazard trim, command mast, and warning lamps must remain readable");
        assertTrue(countIndustrialPixels(image, 155, 205, 485, 330) >= 25_000,
                "the central platform must remain inseparable from the armored ship hull");

        assertTrue(longestIndustrialColumn(image, 0, 125, 40, 300) >= 175,
                "the west drydock crane must visibly continue toward the water");
        assertTrue(longestIndustrialColumn(image, 515, 639, 40, 300) >= 175,
                "the east drydock crane must visibly continue toward the water");
    }

    private static int countIndustrialPixels(BufferedImage image) {
        return countIndustrialPixels(image, 0, 0, image.getWidth() - 1, image.getHeight() - 1);
    }

    private static int countIndustrialPixels(BufferedImage image,
                                             int minX, int minY, int maxX, int maxY) {
        int count = 0;
        for (int y = minY; y <= maxY; y++) {
            for (int x = minX; x <= maxX; x++) {
                int rgb = image.getRGB(x, y);
                int red = red(rgb);
                int green = green(rgb);
                int blue = blue(rgb);
                double luminance = luminance(rgb);
                if (blue >= red + 5 && green >= red + 3
                        && luminance >= 14.0 && luminance <= 102.0) count++;
            }
        }
        return count;
    }

    private static int countWaterPixels(BufferedImage image, int minY, int maxY) {
        int count = 0;
        for (int y = minY; y <= maxY; y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                int rgb = image.getRGB(x, y);
                if (blue(rgb) >= red(rgb) + 18 && green(rgb) >= red(rgb) + 10
                        && luminance(rgb) <= 80.0) count++;
            }
        }
        return count;
    }

    private static int countGoldPixels(BufferedImage image) {
        int count = 0;
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                int rgb = image.getRGB(x, y);
                if (red(rgb) >= 125 && green(rgb) >= 85 && blue(rgb) <= 105
                        && red(rgb) >= blue(rgb) + 34) count++;
            }
        }
        return count;
    }

    private static int longestIndustrialColumn(BufferedImage image,
                                               int minX, int maxX, int minY, int maxY) {
        int longest = 0;
        for (int x = minX; x <= maxX; x++) {
            int current = 0;
            for (int y = minY; y <= maxY; y++) {
                int rgb = image.getRGB(x, y);
                boolean industrial = blue(rgb) >= red(rgb) + 4
                        && luminance(rgb) <= 92.0;
                current = industrial ? current + 1 : 0;
                longest = Math.max(longest, current);
            }
        }
        return longest;
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
