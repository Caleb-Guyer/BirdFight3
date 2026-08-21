package com.example.birdgame3;

import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FrostbiteFjordVisualTest {
    private static final Path CAPTURE = Path.of(
            "src/main/resources/stage-previews/main-frostbite-fjord.png");

    @Test
    void captureKeepsItsFjordFoundationsIcefallsAndOpenBlackWater() throws IOException {
        BufferedImage image = ImageIO.read(CAPTURE.toFile());
        assertNotNull(image, "Frostbite Fjord capture");
        assertEquals(640, image.getWidth());
        assertEquals(360, image.getHeight());

        int brightIcePixels = 0;
        int auroraPixels = 0;
        int deepWaterPixels = 0;
        double brightestMoonPixel = 0.0;
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                int rgb = image.getRGB(x, y);
                int red = red(rgb);
                int green = green(rgb);
                int blue = blue(rgb);
                if (red >= 175 && green >= 205 && blue >= 215) brightIcePixels++;
                if (y < 100 && green >= red * 1.08 && blue >= red * 1.12) auroraPixels++;
                if (y >= 290 && luminance(rgb) <= 48.0) deepWaterPixels++;
                if (x >= 470 && y < 80) brightestMoonPixel = Math.max(brightestMoonPixel, luminance(rgb));
            }
        }

        assertTrue(brightIcePixels >= 2_700,
                "snowcaps, ledge rims, and icefalls must stay crisp against the polar night");
        assertTrue(auroraPixels / (double) (640 * 100) >= 0.82,
                "the broad layered aurora must remain visible across the upper fjord");
        assertTrue(deepWaterPixels / (double) (640 * 70) >= 0.58,
                "black seawater must remain visible beneath and between the glacier shelves");
        assertTrue(brightestMoonPixel >= 220.0,
                "the moon must remain a clear focal point above the eastern wall");

        int connectedMainShelf = 0;
        for (int x = 126; x <= 514; x++) {
            int rgb = image.getRGB(x, 274);
            if (blue(rgb) >= red(rgb) * 1.18 && green(rgb) >= red(rgb) * 1.12
                    && luminance(rgb) >= 70.0) {
                connectedMainShelf++;
            }
        }
        assertTrue(connectedMainShelf >= 375,
                "the main fighting shelf must stay one continuous carved glacier silhouette");

        int westRecovery = image.getRGB(62, 294);
        int eastRecovery = image.getRGB(578, 293);
        assertTrue(blue(westRecovery) >= red(westRecovery) * 1.75
                        && blue(eastRecovery) >= red(eastRecovery) * 1.75,
                "both low recovery ledges must retain visible underwater ice foundations");
        assertTrue(Math.abs(luminance(westRecovery) - luminance(eastRecovery)) <= 22.0,
                "the two recovery wings must remain visually balanced");

        assertIceSupport(image, 195, 225, "western upper ledges");
        assertIceSupport(image, 320, 224, "central sea stack");
        assertIceSupport(image, 425, 225, "eastern upper ledges");

        int openChannel = image.getRGB(320, 335);
        assertTrue(luminance(openChannel) <= 30.0 && blue(openChannel) >= red(openChannel) * 6.0,
                "the lower center must remain an open deep-water channel rather than another solid iceberg");
    }

    private static void assertIceSupport(BufferedImage image, int x, int y, String label) {
        int icyPixels = 0;
        for (int sampleY = y - 16; sampleY <= y + 16; sampleY++) {
            int rgb = image.getRGB(x, sampleY);
            if (blue(rgb) >= red(rgb) * 1.30 && green(rgb) >= red(rgb) * 1.18
                    && luminance(rgb) >= 55.0) {
                icyPixels++;
            }
        }
        assertTrue(icyPixels >= 18, label + " must remain attached to a visible icefall or cliff support");
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
