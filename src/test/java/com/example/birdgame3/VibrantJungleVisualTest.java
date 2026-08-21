package com.example.birdgame3;

import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VibrantJungleVisualTest {
    private static final Path MAIN_CAPTURE = Path.of(
            "src/main/resources/stage-previews/main-vibrant-jungle.png");
    private static final Path THRONE_CAPTURE = Path.of(
            "src/main/resources/stage-previews/variant-carrion-throne.png");

    @Test
    void mainCaptureKeepsSixRootedCanopyTowersAndIntegratedBranches() throws IOException {
        BufferedImage image = readCapture(MAIN_CAPTURE, "Vibrant Jungle capture");
        int canopyPixels = 0;
        int barkPixels = 0;
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                int rgb = image.getRGB(x, y);
                if (isLivingCanopy(rgb)) canopyPixels++;
                if (isBark(rgb)) barkPixels++;
            }
        }

        int totalPixels = image.getWidth() * image.getHeight();
        assertTrue(canopyPixels / (double) totalPixels >= 0.58,
                "layered tropical canopy and mist must remain the arena's dominant identity");
        assertTrue(barkPixels >= 42_000,
                "tree trunks, branch braces, roots, and ledges must retain substantial connected woodwork");
        assertTrue(countDarkRow(image, 289, 100.0) >= 635,
                "the moss lip must sit on a continuous visible root-and-soil floor");

        int[] trunkCenters = {32, 152, 272, 392, 512, 608};
        for (int center : trunkCenters) {
            assertTrue(countBark(image, Math.max(0, center - 12), 45,
                            Math.min(639, center + 12), 286) >= 4_100,
                    "each authored climbing tower must remain visibly rooted near x=" + center);
        }
    }

    @Test
    void carrionThroneCaptureIsADeadwoodBossArenaWithAReadableBoneSeat() throws IOException {
        BufferedImage image = readCapture(THRONE_CAPTURE, "Carrion Throne capture");
        int darkPixels = 0;
        int deadwoodPixels = 0;
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                int rgb = image.getRGB(x, y);
                if (luminance(rgb) <= 48.0) darkPixels++;
                if (isDeadwood(rgb)) deadwoodPixels++;
            }
        }

        int totalPixels = image.getWidth() * image.getHeight();
        assertTrue(darkPixels / (double) totalPixels >= 0.67,
                "Carrion Throne must retain its severe dead-canopy silhouette");
        assertTrue(deadwoodPixels / (double) totalPixels >= 0.47,
                "dead trees, roots, and branch supports must ground the boss arena");
        assertTrue(countDarkRow(image, 285, 100.0) >= 635,
                "the throne's root floor must remain continuous across the stage");
        assertTrue(countBone(image, 225, 40, 415, 230) >= 1_000,
                "the central ribbed bone throne must remain the arena's readable landmark");
        assertTrue(countDeadwood(image, 225, 40, 415, 285) >= 15_000,
                "the bone seat must remain attached to the massive central dead tree");
    }

    private static BufferedImage readCapture(Path path, String label) throws IOException {
        BufferedImage image = ImageIO.read(path.toFile());
        assertNotNull(image, label);
        assertEquals(640, image.getWidth());
        assertEquals(360, image.getHeight());
        return image;
    }

    private static int countBark(BufferedImage image,
                                 int minX, int minY, int maxX, int maxY) {
        int count = 0;
        for (int y = minY; y <= maxY; y++) {
            for (int x = minX; x <= maxX; x++) {
                if (isBark(image.getRGB(x, y))) count++;
            }
        }
        return count;
    }

    private static int countDeadwood(BufferedImage image,
                                     int minX, int minY, int maxX, int maxY) {
        int count = 0;
        for (int y = minY; y <= maxY; y++) {
            for (int x = minX; x <= maxX; x++) {
                if (isDeadwood(image.getRGB(x, y))) count++;
            }
        }
        return count;
    }

    private static int countBone(BufferedImage image,
                                 int minX, int minY, int maxX, int maxY) {
        int count = 0;
        for (int y = minY; y <= maxY; y++) {
            for (int x = minX; x <= maxX; x++) {
                int rgb = image.getRGB(x, y);
                if (red(rgb) >= 165 && green(rgb) >= 150 && blue(rgb) <= 155) count++;
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

    private static boolean isLivingCanopy(int rgb) {
        int red = red(rgb);
        int green = green(rgb);
        int blue = blue(rgb);
        return green >= red * 1.22 && green >= blue * 1.08 && green >= 45;
    }

    private static boolean isBark(int rgb) {
        int red = red(rgb);
        int green = green(rgb);
        int blue = blue(rgb);
        return red >= green * 1.15 && green >= blue * 1.05
                && red >= 45 && red <= 165;
    }

    private static boolean isDeadwood(int rgb) {
        int red = red(rgb);
        int green = green(rgb);
        int blue = blue(rgb);
        return red >= green * 1.08 && green >= blue * 0.95
                && red >= 40 && red <= 150;
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
