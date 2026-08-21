package com.example.birdgame3;

import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StillwaterMarshVisualTest {
    private static final Path CAPTURE = Path.of(
            "src/main/resources/stage-previews/variant-stillwater-marsh.png");

    @Test
    void captureKeepsItsMoonlitWaterContinuousRootBridgeAndMirroredCypresses() throws IOException {
        BufferedImage image = ImageIO.read(CAPTURE.toFile());
        assertNotNull(image, "Stillwater Marsh capture");
        assertEquals(640, image.getWidth());
        assertEquals(360, image.getHeight());

        int tealPixels = 0;
        int marshGreenPixels = 0;
        int woodPixels = 0;
        int leftWoodPixels = 0;
        int rightWoodPixels = 0;
        int deepWaterPixels = 0;
        int fireflyPixels = 0;
        double brightestMoonPixel = 0.0;
        int totalPixels = image.getWidth() * image.getHeight();
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                int rgb = image.getRGB(x, y);
                int red = red(rgb);
                int green = green(rgb);
                int blue = blue(rgb);
                if (green >= red * 1.25 && blue >= red * 1.20 && green >= 35) tealPixels++;
                if (green >= red * 1.18 && green >= blue * 1.08 && green >= 50) marshGreenPixels++;
                boolean cypressWood = red >= 35 && red >= blue * 1.45 && green >= 22;
                if (cypressWood) {
                    woodPixels++;
                    if (x < image.getWidth() / 2) leftWoodPixels++;
                    else rightWoodPixels++;
                }
                if (y >= 260 && blue >= red * 1.35 && green >= red * 1.35) deepWaterPixels++;
                if (red >= 100 && green >= 110 && blue <= 100) fireflyPixels++;
                if (x >= 420 && y < 80) {
                    brightestMoonPixel = Math.max(brightestMoonPixel, luminance(rgb));
                }
            }
        }

        assertTrue(tealPixels / (double) totalPixels >= 0.34,
                "moonlit teal atmosphere and water must remain the stage's dominant identity");
        assertTrue(marshGreenPixels / (double) totalPixels >= 0.065,
                "moss, cypress crowns, reeds, and lily pads must keep the marsh alive");
        assertTrue(woodPixels / (double) totalPixels >= 0.065,
                "the playable layout must continue to read as a connected cypress root system");
        assertTrue(leftWoodPixels >= totalPixels * 0.038
                        && rightWoodPixels >= totalPixels * 0.038,
                "both wings must retain equally substantial cypress supports");
        assertTrue(deepWaterPixels / (double) (640 * 100) >= 0.48,
                "the bottom of the scene must remain visibly flooded rather than becoming solid ground");
        assertTrue(fireflyPixels >= 70,
                "fireflies must remain visible across the clearing instead of disappearing into the sky");
        assertTrue(brightestMoonPixel >= 205.0,
                "the full moon must remain the clear focal point above the dark wetland");

        int connectedBridgePixels = 0;
        for (int x = 65; x < 575; x++) {
            int rgb = image.getRGB(x, 252);
            if (red(rgb) >= 35 && red(rgb) >= blue(rgb) * 1.45 && green(rgb) >= 22) {
                connectedBridgePixels++;
            }
        }
        assertTrue(connectedBridgePixels >= 470,
                "the moss-lined main bridge must remain one uninterrupted root across the arena");

        int westRoot = image.getRGB(75, 280);
        int eastRoot = image.getRGB(565, 280);
        assertTrue(red(westRoot) >= blue(westRoot) * 1.25
                        && red(eastRoot) >= blue(eastRoot) * 1.25,
                "the two low recovery shelves must remain joined to visible roots below the bridge");
        assertTrue(Math.abs(luminance(westRoot) - luminance(eastRoot)) <= 2.0,
                "the mirrored submerged root foundations must remain visually balanced");
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
