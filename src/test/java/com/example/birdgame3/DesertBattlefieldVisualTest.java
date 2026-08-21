package com.example.birdgame3;

import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DesertBattlefieldVisualTest {
    private static final Path PREVIEWS = Path.of("src/main/resources/stage-previews");

    @Test
    void desertCaptureKeepsItsCaravanCityOasisGroundedWatchtowersAndCliffAscent() throws IOException {
        BufferedImage image = load("main-desert.png");

        assertTrue(countPixels(image, 0, 0, 639, 220,
                        (r, g, b) -> r >= 100 && r >= b + 24 && b >= 55) >= 80_000,
                "the new dusk sky must dominate the Desert atmosphere");
        assertTrue(countPixels(image, 35, 250, 135, 330,
                        (r, g, b) -> b >= r + 8 && g >= r + 20) >= 1_200,
                "the oasis must remain clearly readable as water");
        assertTrue(countPixels(image, 115, 205, 455, 310,
                        (r, g, b) -> r >= 90 && r >= b + 24 && g >= b + 8) >= 12_000,
                "the caravan city and grounded waystations must fill the old empty centre");
        assertTrue(countPixels(image, 510, 120, 639, 350,
                        (r, g, b) -> r >= 90 && r >= b + 28 && g >= b + 10) >= 18_000,
                "the stepped cliff ascent must remain a substantial landform");
    }

    @Test
    void redlineCaptureKeepsOneContinuousViaductAndGroundedMesaPiers() throws IOException {
        BufferedImage image = load("variant-redline-canyon.png");

        assertTrue(countPixels(image, 0, 255, 639, 285,
                        (r, g, b) -> r >= 45 && r <= 135 && g <= 95 && b <= 95) >= 11_000,
                "the Redline road and truss must read continuously across the route");
        assertTrue(countPixels(image, 0, 145, 639, 350,
                        (r, g, b) -> r >= 65 && r >= g + 18 && r >= b + 8) >= 60_000,
                "large mesa piers must replace the old disconnected poles");
        assertTrue(longestMatchingColumn(image, 45, 595, 165, 342,
                        (r, g, b) -> r >= 65 && r >= g + 18 && r >= b + 8) >= 80,
                "at least one upper ledge must visibly continue into its canyon foundation");
    }

    @Test
    void battlefieldCaptureReadsAsOneAncientSkyCitadel() throws IOException {
        BufferedImage image = load("main-battlefield.png");

        assertTrue(countPixels(image, 195, 135, 445, 340,
                        (r, g, b) -> b >= r + 6 && g >= r + 4 && luminance(r, g, b) <= 100.0)
                        >= 18_000,
                "the island, towers, and summit arch must form one substantial citadel");
        assertTrue(countPixels(image, 220, 160, 420, 320,
                        (r, g, b) -> r >= 95 && g >= 80 && b <= 125) >= 80,
                "golden ruin trim and the central standard must remain visible");
        assertTrue(countPixels(image, 255, 210, 385, 330,
                        (r, g, b) -> luminance(r, g, b) <= 65.0) >= 2_500,
                "the open summit arch and lower doorways must preserve readable negative space");
    }

    private static BufferedImage load(String name) throws IOException {
        BufferedImage image = ImageIO.read(PREVIEWS.resolve(name).toFile());
        assertNotNull(image, name);
        assertEquals(640, image.getWidth(), name + " width");
        assertEquals(360, image.getHeight(), name + " height");
        return image;
    }

    private static int countPixels(BufferedImage image,
                                   int minX, int minY, int maxX, int maxY,
                                   PixelPredicate predicate) {
        int count = 0;
        for (int y = minY; y <= maxY; y++) {
            for (int x = minX; x <= maxX; x++) {
                int rgb = image.getRGB(x, y);
                if (predicate.test(red(rgb), green(rgb), blue(rgb))) count++;
            }
        }
        return count;
    }

    private static int longestMatchingColumn(BufferedImage image,
                                             int minX, int maxX, int minY, int maxY,
                                             PixelPredicate predicate) {
        int longest = 0;
        for (int x = minX; x <= maxX; x++) {
            int run = 0;
            for (int y = minY; y <= maxY; y++) {
                int rgb = image.getRGB(x, y);
                run = predicate.test(red(rgb), green(rgb), blue(rgb)) ? run + 1 : 0;
                longest = Math.max(longest, run);
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

    private static double luminance(int r, int g, int b) {
        return r * 0.2126 + g * 0.7152 + b * 0.0722;
    }

    @FunctionalInterface
    private interface PixelPredicate {
        boolean test(int red, int green, int blue);
    }
}
