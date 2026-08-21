package com.example.birdgame3;

import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FrozenCalderaVisualTest {
    private static final Path CAPTURE = Path.of(
            "src/main/resources/stage-previews/variant-frozen-caldera.png");

    @Test
    void captureKeepsItsAuroraGlacierFoundationsAndBuriedThermal() throws IOException {
        BufferedImage image = ImageIO.read(CAPTURE.toFile());
        assertNotNull(image, "Frozen Caldera capture");
        assertEquals(640, image.getWidth());
        assertEquals(360, image.getHeight());

        int icyPixels = 0;
        int leftIcePixels = 0;
        int rightIcePixels = 0;
        int auroraPixels = 0;
        int thermalPixels = 0;
        int darkLowerPixels = 0;
        int totalPixels = image.getWidth() * image.getHeight();
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                int rgb = image.getRGB(x, y);
                int red = (rgb >>> 16) & 0xFF;
                int green = (rgb >>> 8) & 0xFF;
                int blue = rgb & 0xFF;
                boolean glacierIce = blue >= 60 && green >= red * 1.12 && blue >= red * 1.25;
                if (glacierIce) {
                    icyPixels++;
                    if (x < image.getWidth() / 2) leftIcePixels++;
                    else rightIcePixels++;
                }
                if (y < 115 && green >= red * 1.10 && blue >= red * 1.18) auroraPixels++;
                if (red >= 65 && red >= blue * 1.18 && green >= 28) thermalPixels++;
                if (y >= 260 && luminance(rgb) <= 50.0) darkLowerPixels++;
            }
        }

        assertTrue(icyPixels / (double) totalPixels >= 0.20,
                "carved ice and glacier faces must dominate the Frozen Caldera silhouette");
        assertTrue(leftIcePixels >= totalPixels * 0.105
                        && rightIcePixels >= totalPixels * 0.105,
                "both recovery wings must retain equally grounded glacier architecture");
        assertTrue(auroraPixels / (double) (640 * 115) >= 0.22,
                "the upper sky must preserve the caldera's broad cyan aurora");
        assertTrue(thermalPixels / (double) totalPixels >= 0.0015,
                "the buried last thermal must remain visible through the frozen center");
        assertTrue(darkLowerPixels / (double) (640 * 100) >= 0.70,
                "the glacier foundations must descend into a readable volcanic chasm");

        int centerVein = image.getRGB(320, 270);
        int veinRed = (centerVein >>> 16) & 0xFF;
        int veinBlue = centerVein & 0xFF;
        assertTrue(veinRed >= veinBlue * 1.45,
                "the central thermal fissure must stay warm against the blue glacier");

        int westFoundation = image.getRGB(75, 275);
        int eastFoundation = image.getRGB(565, 275);
        assertTrue(blue(westFoundation) >= red(westFoundation) * 2.0
                        && blue(eastFoundation) >= red(eastFoundation) * 2.0,
                "the two outer fighting islands must keep visible ice foundations below them");
        assertTrue(Math.abs(luminance(westFoundation) - luminance(eastFoundation)) <= 2.0,
                "the mirrored recovery foundations must remain visually balanced");
    }

    private static int red(int rgb) {
        return (rgb >>> 16) & 0xFF;
    }

    private static int blue(int rgb) {
        return rgb & 0xFF;
    }

    private static double luminance(int rgb) {
        int red = (rgb >>> 16) & 0xFF;
        int green = (rgb >>> 8) & 0xFF;
        int blue = rgb & 0xFF;
        return red * 0.2126 + green * 0.7152 + blue * 0.0722;
    }
}
