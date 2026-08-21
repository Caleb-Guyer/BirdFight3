package com.example.birdgame3;

import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BeaconCrownVisualTest {
    private static final Path PREVIEWS = Path.of("src/main/resources/stage-previews");

    @Test
    void crownArenaFamilyKeepsFourDistinctBelievableStructures() throws IOException {
        Map<String, BufferedImage> captures = new LinkedHashMap<>();
        captures.put("Beacon Crown", read("main-beacon-crown.png"));
        captures.put("Final Duel", read("variant-null-rock-duel.png"));
        captures.put("Null Roc Ascending", read("variant-null-roc-ascending.png"));
        captures.put("Void Crown", read("variant-void-crown.png"));

        for (Map.Entry<String, BufferedImage> capture : captures.entrySet()) {
            assertEquals(640, capture.getValue().getWidth(), capture.getKey() + " width");
            assertEquals(360, capture.getValue().getHeight(), capture.getKey() + " height");
        }

        BufferedImage beacon = captures.get("Beacon Crown");
        assertTrue(countGold(beacon) >= 850,
                "Beacon Crown must retain its illuminated tower ribs, seal, and eclipse");
        assertTrue(countDark(beacon, 125, 190, 515, 359) >= 52_000,
                "Beacon Crown's playable platforms must remain part of a monumental central citadel");

        BufferedImage duel = captures.get("Final Duel");
        assertTrue(countCrimson(duel) >= 2_300,
                "the final altar must retain its fractured red Crown identity");
        assertTrue(countDark(duel, 145, 260, 495, 359) >= 23_000,
                "the final duel slab must remain attached to its broken altar foundation");

        BufferedImage ascent = captures.get("Null Roc Ascending");
        assertTrue(countCoolStone(ascent) >= 24_000,
                "the ascent must retain substantial Crown fragments under every landing surface");
        assertTrue(countGold(ascent) >= 800,
                "the diagonal ascent path must remain visually connected by broken Crown rails");

        BufferedImage voidCapture = captures.get("Void Crown");
        assertTrue(countPurple(voidCapture) >= 20_000,
                "Void Crown must retain its enormous orbital ring and fragment palette");
        assertTrue(countDark(voidCapture, 205, 40, 435, 330) >= 35_000,
                "Void Crown must retain its central black-hole altar composition");

        String[] names = captures.keySet().toArray(String[]::new);
        for (int first = 0; first < names.length; first++) {
            for (int second = first + 1; second < names.length; second++) {
                double ratio = changedPixelRatio(captures.get(names[first]), captures.get(names[second]));
                assertTrue(ratio >= 0.12,
                        names[first] + " and " + names[second]
                                + " must not regress into the same eclipse-and-bars scene: " + ratio);
            }
        }
    }

    private static BufferedImage read(String fileName) throws IOException {
        BufferedImage image = ImageIO.read(PREVIEWS.resolve(fileName).toFile());
        assertNotNull(image, fileName);
        return image;
    }

    private static int countGold(BufferedImage image) {
        return countMatching(image, (red, green, blue, luminance) ->
                red >= 120 && green >= 88 && blue <= 130 && red >= blue + 22);
    }

    private static int countCrimson(BufferedImage image) {
        return countMatching(image, (red, green, blue, luminance) ->
                red >= 80 && red >= green + 24 && red >= blue + 8);
    }

    private static int countCoolStone(BufferedImage image) {
        return countMatching(image, (red, green, blue, luminance) ->
                blue >= red + 10 && blue >= green + 4 && luminance >= 18.0 && luminance <= 100.0);
    }

    private static int countPurple(BufferedImage image) {
        return countMatching(image, (red, green, blue, luminance) ->
                blue >= green + 16 && red >= green + 5 && luminance >= 15.0);
    }

    private static int countMatching(BufferedImage image, PixelPredicate predicate) {
        int count = 0;
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                int rgb = image.getRGB(x, y);
                if (predicate.matches(red(rgb), green(rgb), blue(rgb), luminance(rgb))) count++;
            }
        }
        return count;
    }

    private static int countDark(BufferedImage image,
                                 int minX, int minY, int maxX, int maxY) {
        int count = 0;
        for (int y = minY; y <= maxY; y++) {
            for (int x = minX; x <= maxX; x++) {
                if (luminance(image.getRGB(x, y)) <= 54.0) count++;
            }
        }
        return count;
    }

    private static double changedPixelRatio(BufferedImage first, BufferedImage second) {
        int changed = 0;
        int total = first.getWidth() * first.getHeight();
        for (int y = 0; y < first.getHeight(); y++) {
            for (int x = 0; x < first.getWidth(); x++) {
                int a = first.getRGB(x, y);
                int b = second.getRGB(x, y);
                int distance = Math.abs(red(a) - red(b))
                        + Math.abs(green(a) - green(b))
                        + Math.abs(blue(a) - blue(b));
                if (distance >= 36) changed++;
            }
        }
        return changed / (double) total;
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

    @FunctionalInterface
    private interface PixelPredicate {
        boolean matches(int red, int green, int blue, double luminance);
    }
}
