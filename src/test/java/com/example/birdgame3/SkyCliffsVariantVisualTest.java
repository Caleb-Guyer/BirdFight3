package com.example.birdgame3;

import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Path;
import java.util.EnumMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Protects the four authored Sky Cliffs variants from returning to recolored floating bars. */
class SkyCliffsVariantVisualTest {
    private static final Path PREVIEW_DIRECTORY = Path.of("src/main/resources/stage-previews");

    @Test
    void everyVariantRetainsItsOwnReadableArchitecturalIdentity() throws IOException {
        Map<BirdGame3.MapVariant, BufferedImage> captures = new EnumMap<>(BirdGame3.MapVariant.class);
        captures.put(BirdGame3.MapVariant.CROWN_DUEL, read("variant-crown-duel.png"));
        captures.put(BirdGame3.MapVariant.SKYBREAK_SPIRES, read("variant-skybreak-spires.png"));
        captures.put(BirdGame3.MapVariant.PEREGRINE_RUN, read("variant-peregrine-run.png"));
        captures.put(BirdGame3.MapVariant.TEMPEST_SUMMIT, read("variant-tempest-summit.png"));

        for (Map.Entry<BirdGame3.MapVariant, BufferedImage> entry : captures.entrySet()) {
            assertEquals(640, entry.getValue().getWidth(), entry.getKey() + " preview width");
            assertEquals(360, entry.getValue().getHeight(), entry.getKey() + " preview height");
        }

        BufferedImage command = captures.get(BirdGame3.MapVariant.CROWN_DUEL);
        assertTrue(countCoolStructuralPixels(command) >= 12_000,
                "Command Bridge must retain substantial steel pylons, trusswork, and decking");
        assertTrue(countGoldPixels(command) >= 35,
                "Command Bridge must retain its central Crown command seal");
        assertTrue(longestDarkRun(command, 330) < 500,
                "Command Bridge is suspended above open sky and must not gain a fake floor");

        BufferedImage skybreak = captures.get(BirdGame3.MapVariant.SKYBREAK_SPIRES);
        assertTrue(countCoolStructuralPixels(skybreak) >= 44_000,
                "Skybreak must remain a dense family of grounded mountain spires");
        assertTrue(longestDarkRun(skybreak, 315) >= 630,
                "Skybreak's real full-width ground collider must remain visibly grounded");

        BufferedImage peregrine = captures.get(BirdGame3.MapVariant.PEREGRINE_RUN);
        assertTrue(countWarmSkyPixels(peregrine) >= 70_000,
                "Peregrine Run must retain its distinct high-speed dawn atmosphere");
        assertTrue(countGoldPixels(peregrine) >= 450,
                "Peregrine Run must keep the readable illuminated dive route");
        assertTrue(longestDarkRun(peregrine, 345) < 500,
                "Peregrine Run's open gaps must not be painted as solid ground");

        BufferedImage tempest = captures.get(BirdGame3.MapVariant.TEMPEST_SUMMIT);
        assertTrue(countDarkPixels(tempest) >= 145_000,
                "Tempest Summit must remain a severe high-contrast storm arena");
        assertTrue(countGoldPixels(tempest) >= 120,
                "Tempest Summit must retain its amber summit rims and lightning rods");

        BirdGame3.MapVariant[] variants = captures.keySet().toArray(BirdGame3.MapVariant[]::new);
        for (int first = 0; first < variants.length; first++) {
            for (int second = first + 1; second < variants.length; second++) {
                double changed = changedPixelRatio(captures.get(variants[first]), captures.get(variants[second]));
                assertTrue(changed >= 0.48,
                        variants[first] + " and " + variants[second]
                                + " must remain visibly different, changed pixels=" + changed);
            }
        }
    }

    private static BufferedImage read(String fileName) throws IOException {
        BufferedImage image = ImageIO.read(PREVIEW_DIRECTORY.resolve(fileName).toFile());
        assertNotNull(image, fileName);
        return image;
    }

    private static int countCoolStructuralPixels(BufferedImage image) {
        int count = 0;
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                int rgb = image.getRGB(x, y);
                int red = red(rgb);
                int green = green(rgb);
                int blue = blue(rgb);
                if (blue >= red + 7 && green >= red + 4 && luminance(rgb) >= 18.0
                        && luminance(rgb) <= 115.0) count++;
            }
        }
        return count;
    }

    private static int countWarmSkyPixels(BufferedImage image) {
        int count = 0;
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                int rgb = image.getRGB(x, y);
                if (red(rgb) >= blue(rgb) + 28 && red(rgb) >= green(rgb) + 5
                        && luminance(rgb) >= 60.0) count++;
            }
        }
        return count;
    }

    private static int countGoldPixels(BufferedImage image) {
        int count = 0;
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                int rgb = image.getRGB(x, y);
                if (red(rgb) >= 150 && green(rgb) >= 110 && blue(rgb) <= 150
                        && red(rgb) >= blue(rgb) + 30) count++;
            }
        }
        return count;
    }

    private static int countDarkPixels(BufferedImage image) {
        int count = 0;
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                if (luminance(image.getRGB(x, y)) <= 54.0) count++;
            }
        }
        return count;
    }

    private static int longestDarkRun(BufferedImage image, int y) {
        int longest = 0;
        int current = 0;
        for (int x = 0; x < image.getWidth(); x++) {
            if (luminance(image.getRGB(x, y)) <= 85.0) {
                current++;
                longest = Math.max(longest, current);
            } else {
                current = 0;
            }
        }
        return longest;
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
                if (distance >= 42) changed++;
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
}
