package com.example.birdgame3;

import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CaveVisualTest {
    private static final Path CAPTURE = Path.of("src/main/resources/stage-previews/main-cave.png");

    @Test
    void captureKeepsItsEchoGeodeGroundedPillarsAndReadableDraftShafts() throws IOException {
        BufferedImage image = ImageIO.read(CAPTURE.toFile());
        assertNotNull(image, "Echo Cavern capture");
        assertEquals(640, image.getWidth());
        assertEquals(360, image.getHeight());

        int limestonePixels = 0;
        int cyanEchoPixels = 0;
        int violetMineralPixels = 0;
        int deepShadowPixels = 0;
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                int rgb = image.getRGB(x, y);
                int red = red(rgb);
                int green = green(rgb);
                int blue = blue(rgb);
                double luminance = luminance(rgb);
                if (blue >= red * 1.05 && blue >= green * 1.02
                        && luminance >= 24.0 && luminance <= 125.0) limestonePixels++;
                if (blue >= red * 1.35 && green >= red * 1.35 && green >= 55) cyanEchoPixels++;
                if (blue >= green * 1.12 && red >= green * 0.90 && blue >= 60) violetMineralPixels++;
                if (luminance <= 35.0) deepShadowPixels++;
            }
        }

        int totalPixels = image.getWidth() * image.getHeight();
        assertTrue(limestonePixels / (double) totalPixels >= 0.60,
                "layered limestone chambers, pillars, braces, and shelves must dominate Echo Cavern");
        assertTrue(deepShadowPixels / (double) totalPixels >= 0.60,
                "deep negative space must preserve the cavern's scale and fighter contrast");
        assertTrue(cyanEchoPixels >= 2_700,
                "the geode and draft fissures must retain a readable cyan echo identity");
        assertTrue(violetMineralPixels >= 17_000,
                "violet strata and crystal deposits must keep the lower chamber visually grounded");

        assertTrue(countDarkRow(image, 8, 45.0) >= 635,
                "the real ceiling collider must remain a continuous visible cavern roof");
        assertTrue(countDarkRow(image, 290, 85.0) >= 635,
                "the real ground collider must remain a continuous visible stone foundation");

        int[] pillarCenters = {22, 158, 291, 426, 567};
        for (int center : pillarCenters) {
            assertTrue(countRock(image, Math.max(0, center - 13), 35,
                            Math.min(639, center + 13), 286) >= 5_700,
                    "each major chamber pillar must visibly span roof to foundation near x=" + center);
        }

        assertTrue(countCyan(image, 260, 75, 380, 175) >= 580,
                "the central echo geode must remain the stage's clear focal landmark");
        int[] draftCenters = {74, 248, 428, 608};
        for (int center : draftCenters) {
            assertTrue(countCyan(image, Math.max(0, center - 16), 70,
                            Math.min(639, center + 16), 245) >= (center == 608 ? 380 : 720),
                    "each authored wind lane must remain visible as a fissure-backed draft near x=" + center);
        }
    }

    private static int countDarkRow(BufferedImage image, int y, double maximumLuminance) {
        int count = 0;
        for (int x = 0; x < image.getWidth(); x++) {
            if (luminance(image.getRGB(x, y)) <= maximumLuminance) count++;
        }
        return count;
    }

    private static int countRock(BufferedImage image, int minX, int minY, int maxX, int maxY) {
        int count = 0;
        for (int y = minY; y <= maxY; y++) {
            for (int x = minX; x <= maxX; x++) {
                int rgb = image.getRGB(x, y);
                double luminance = luminance(rgb);
                if (blue(rgb) >= red(rgb) * 1.04 && luminance >= 22.0 && luminance <= 95.0) count++;
            }
        }
        return count;
    }

    private static int countCyan(BufferedImage image, int minX, int minY, int maxX, int maxY) {
        int count = 0;
        for (int y = minY; y <= maxY; y++) {
            for (int x = minX; x <= maxX; x++) {
                int rgb = image.getRGB(x, y);
                if (blue(rgb) >= red(rgb) * 1.35
                        && green(rgb) >= red(rgb) * 1.35
                        && green(rgb) >= 45) count++;
            }
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
