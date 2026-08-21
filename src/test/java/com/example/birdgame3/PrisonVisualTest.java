package com.example.birdgame3;

import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PrisonVisualTest {
    private static final Path CAPTURE = Path.of("src/main/resources/stage-previews/main-prison.png");

    @Test
    void captureKeepsItsFramedCellblocksTransferCoreAndGroundedCatwalks() throws IOException {
        BufferedImage image = ImageIO.read(CAPTURE.toFile());
        assertNotNull(image, "Crownlock Prison capture");
        assertEquals(640, image.getWidth());
        assertEquals(360, image.getHeight());

        int steelPixels = 0;
        int cyanControlPixels = 0;
        int alarmPixels = 0;
        int amberInteractionPixels = 0;
        int deepFoundationPixels = 0;
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                int rgb = image.getRGB(x, y);
                int red = red(rgb);
                int green = green(rgb);
                int blue = blue(rgb);
                if (blue >= red * 1.14 && green >= red * 1.12 && blue >= 38) steelPixels++;
                if (green >= red * 1.25 && blue >= red * 1.35 && green >= 80) cyanControlPixels++;
                if (red >= 130 && red >= green * 1.65) alarmPixels++;
                if (red >= 130 && green >= 85 && blue <= 65) amberInteractionPixels++;
                if (y >= 286 && luminance(rgb) <= 30.0) deepFoundationPixels++;
            }
        }

        int totalPixels = image.getWidth() * image.getHeight();
        assertTrue(steelPixels / (double) totalPixels >= 0.37,
                "framed steel cellblocks and trussed catwalks must dominate Crownlock's architecture");
        assertTrue(cyanControlPixels >= 6_000,
                "the transfer core and searchlights must retain a clear cold-blue control identity");
        assertTrue(alarmPixels >= 80,
                "lockdown lamps must remain readable along the breached roofline");
        assertTrue(amberInteractionPixels >= 500,
                "lever stations, cell lamps, and floor markings must retain their amber interaction language");
        assertTrue(deepFoundationPixels / (double) (640 * 74) >= 0.74,
                "the transfer floor must descend into a substantial dark mechanical foundation");

        int connectedFloorPixels = 0;
        for (int x = 0; x < image.getWidth(); x++) {
            int rgb = image.getRGB(x, 270);
            if (blue(rgb) >= red(rgb) * 1.12 && green(rgb) >= red(rgb) * 1.08
                    && luminance(rgb) >= 35.0) {
                connectedFloorPixels++;
            }
        }
        assertTrue(connectedFloorPixels >= 630,
                "the main fighting floor must remain a continuous transfer deck across both blast lines");

        int westCells = countSteel(image, 0, 78, 155, 198);
        int eastCells = countSteel(image, 485, 78, 639, 198);
        assertTrue(westCells >= 8_000 && eastCells >= 8_000,
                "both two-story cell wings must remain visibly framed and barred");
        assertTrue(Math.abs(westCells - eastCells) <= 1_100,
                "the two cell wings must retain comparable structural weight");

        assertTrue(countSteel(image, 75, 198, 190, 270) >= 5_000,
                "western catwalks must remain attached to substantial trusses and columns");
        assertTrue(countSteel(image, 450, 198, 565, 270) >= 5_000,
                "eastern catwalks must remain attached to substantial trusses and columns");
        assertTrue(countSteel(image, 215, 85, 425, 268) >= 14_000,
                "the central catwalk route must remain integrated into the transfer core machinery");

        int openRoofSky = image.getRGB(300, 24);
        assertTrue(blue(openRoofSky) >= red(openRoofSky) * 2.2 && luminance(openRoofSky) <= 28.0,
                "the upper center must remain an open night-sky blast route, not a hidden solid ceiling");
    }

    private static int countSteel(BufferedImage image, int minX, int minY, int maxX, int maxY) {
        int count = 0;
        for (int y = minY; y <= maxY; y++) {
            for (int x = minX; x <= maxX; x++) {
                int rgb = image.getRGB(x, y);
                if (blue(rgb) >= red(rgb) * 1.14 && green(rgb) >= red(rgb) * 1.10
                        && blue(rgb) >= 35) {
                    count++;
                }
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
