package com.example.birdgame3;

import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AshfallCathedralVisualTest {
    private static final Path CAPTURE = Path.of(
            "src/main/resources/stage-previews/main-ashfall-cathedral.png");

    @Test
    void captureKeepsItsStainedGlassConnectedBasilicaAndLavaRootedChapels() throws IOException {
        BufferedImage image = ImageIO.read(CAPTURE.toFile());
        assertNotNull(image, "Ashfall Cathedral capture");
        assertEquals(640, image.getWidth());
        assertEquals(360, image.getHeight());

        int warmPixels = 0;
        int cyanGlassPixels = 0;
        int brightGlassPixels = 0;
        int visibleLavaPixels = 0;
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                int rgb = image.getRGB(x, y);
                int red = red(rgb);
                int green = green(rgb);
                int blue = blue(rgb);
                if (red >= 90 && red >= blue * 1.45 && green >= 20) warmPixels++;
                if (blue >= red * 1.18 && green >= red * 1.18 && blue >= 45) cyanGlassPixels++;
                if (red >= 170 && green >= 75) brightGlassPixels++;
                if (y >= 292 && red >= 75 && red >= blue * 2.2 && green >= 18) visibleLavaPixels++;
            }
        }

        assertTrue(warmPixels >= 10_000,
                "volcanic masonry, stained glass, and lava must retain the cathedral's warm identity");
        assertTrue(cyanGlassPixels >= 700,
                "cool stained-glass panes must remain visible against the ember-red basilica");
        assertTrue(brightGlassPixels >= 1_300,
                "the rose window and molten fissures must retain their bright focal highlights");
        assertTrue(visibleLavaPixels / (double) (640 * 68) >= 0.07,
                "lava must remain visibly exposed around the cathedral foundations");

        int roseCenter = image.getRGB(320, 110);
        assertTrue(red(roseCenter) >= 220 && green(roseCenter) >= 150,
                "the phoenix rose window must remain the scene's clear central focal point");

        int connectedNavePixels = 0;
        for (int x = 85; x <= 555; x++) {
            int rgb = image.getRGB(x, 273);
            if (red(rgb) >= 28 && red(rgb) >= blue(rgb) * 1.35 && luminance(rgb) >= 18.0) {
                connectedNavePixels++;
            }
        }
        assertTrue(connectedNavePixels >= 460,
                "the main fighting floor must remain one uninterrupted cathedral nave");

        assertMasonrySupport(image, 125, 235, 155, 276, "western choir balcony");
        assertMasonrySupport(image, 485, 235, 515, 276, "eastern choir balcony");
        assertMasonrySupport(image, 285, 155, 355, 245, "central bell and altar tower");

        int westChapel = image.getRGB(61, 305);
        int eastChapel = image.getRGB(579, 305);
        assertTrue(red(westChapel) >= blue(westChapel) * 1.55
                        && red(eastChapel) >= blue(eastChapel) * 1.55,
                "both low recovery ledges must descend into masonry chapels rather than float over lava");
        assertTrue(Math.abs(luminance(westChapel) - luminance(eastChapel)) <= 2.0,
                "the mirrored recovery chapels must remain visually balanced");

        int lowerCenter = image.getRGB(320, 320);
        assertTrue(luminance(lowerCenter) <= 22.0,
                "the nave foundation must keep its deep obsidian mass beneath the playable floor");
    }

    private static void assertMasonrySupport(BufferedImage image, int minX, int minY,
                                              int maxX, int maxY, String label) {
        int masonryPixels = 0;
        int totalPixels = 0;
        for (int y = minY; y <= maxY; y++) {
            for (int x = minX; x <= maxX; x++) {
                totalPixels++;
                int rgb = image.getRGB(x, y);
                if (red(rgb) >= 20 && red(rgb) >= blue(rgb) * 1.15
                        && red(rgb) >= green(rgb) * 1.22) {
                    masonryPixels++;
                }
            }
        }
        assertTrue(masonryPixels >= totalPixels * 0.92,
                label + " must remain attached to substantial cathedral masonry");
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
