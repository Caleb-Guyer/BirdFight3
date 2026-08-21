package com.example.birdgame3;

import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ForestVisualTest {
    private static final Path CAPTURE = Path.of("src/main/resources/stage-previews/main-forest.png");

    @Test
    void captureKeepsItsOldGrowthTrunksRootedFloorAndSupportedBranches() throws IOException {
        BufferedImage image = ImageIO.read(CAPTURE.toFile());
        assertNotNull(image, "Big Forest capture");
        assertEquals(640, image.getWidth());
        assertEquals(360, image.getHeight());

        int canopyPixels = 0;
        int barkPixels = 0;
        int mossPixels = 0;
        int soilPixels = 0;
        int totalPixels = image.getWidth() * image.getHeight();
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                int rgb = image.getRGB(x, y);
                int red = red(rgb);
                int green = green(rgb);
                int blue = blue(rgb);
                if (green >= red * 1.25 && green >= blue * 1.12 && green >= 38) canopyPixels++;
                if (red >= green * 1.12 && green >= blue * 1.12 && red >= 42 && red <= 150) barkPixels++;
                if (green >= red * 1.22 && green >= blue * 1.16 && green >= 70) mossPixels++;
                if (y >= 290 && red >= green * 1.08 && green >= blue * 1.12 && luminance(rgb) <= 92.0) {
                    soilPixels++;
                }
            }
        }

        assertTrue(canopyPixels / (double) totalPixels >= 0.32,
                "layered woodland canopy must remain the dominant visual identity");
        assertTrue(barkPixels >= 21_000,
                "ancient trunks, roots, branch supports, and ledges must retain substantial bark structure");
        assertTrue(mossPixels >= 5_000,
                "collision surfaces must remain clearly readable as bright moss-covered branches");
        assertTrue(soilPixels >= 31_000,
                "the lower arena must remain a continuous, deep forest floor rather than an empty strip");

        int[] trunkCenters = {51, 210, 368, 534, 633};
        for (int center : trunkCenters) {
            int minX = Math.max(0, center - 17);
            int maxX = Math.min(image.getWidth() - 1, center + 17);
            assertTrue(countBark(image, minX, 45, maxX, 286) >= 3_000,
                    "each major old-growth column must visibly connect canopy to ground near x=" + center);
        }

        int connectedFloor = 0;
        for (int x = 0; x < image.getWidth(); x++) {
            int rgb = image.getRGB(x, 289);
            boolean livingTop = green(rgb) >= red(rgb) * 1.05 && green(rgb) >= blue(rgb) * 1.15;
            boolean rootedSoil = red(rgb) >= green(rgb) * 1.05 && green(rgb) >= blue(rgb) * 1.05;
            if (livingTop || rootedSoil) {
                connectedFloor++;
            }
        }
        assertTrue(connectedFloor >= 630,
                "the playable ground line must remain visually continuous across the entire arena");

        int dawnPixels = 0;
        for (int y = 45; y <= 115; y++) {
            for (int x = 270; x <= 370; x++) {
                int rgb = image.getRGB(x, y);
                if (luminance(rgb) >= 180.0 && red(rgb) >= blue(rgb)) dawnPixels++;
            }
        }
        assertTrue(dawnPixels >= 240,
                "the filtered dawn sun must remain visible as the central depth landmark");
        assertTrue(countBark(image, 145, 90, 495, 286) >= 14_000,
                "the central branch network must retain enough support structure to avoid floating ledges");
    }

    private static int countBark(BufferedImage image, int minX, int minY, int maxX, int maxY) {
        int count = 0;
        for (int y = minY; y <= maxY; y++) {
            for (int x = minX; x <= maxX; x++) {
                int rgb = image.getRGB(x, y);
                int red = red(rgb);
                int green = green(rgb);
                int blue = blue(rgb);
                if (red >= green * 1.12 && green >= blue * 1.12 && red >= 42 && red <= 150) count++;
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
