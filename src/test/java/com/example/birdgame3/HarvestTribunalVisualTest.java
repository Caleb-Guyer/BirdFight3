package com.example.birdgame3;

import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HarvestTribunalVisualTest {
    private static final Path CAPTURE = Path.of(
            "src/main/resources/stage-previews/variant-harvest-tribunal.png");

    @Test
    void captureKeepsItsMoonlitCourtAndGroundedFeastTable() throws IOException {
        BufferedImage image = ImageIO.read(CAPTURE.toFile());
        assertNotNull(image, "Harvest Tribunal capture");
        assertEquals(640, image.getWidth());
        assertEquals(360, image.getHeight());

        int warmPixels = 0;
        int leftWarmPixels = 0;
        int rightWarmPixels = 0;
        int darkLowerPixels = 0;
        int totalPixels = image.getWidth() * image.getHeight();
        int lowerPixels = image.getWidth() * (image.getHeight() - 245);
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                int rgb = image.getRGB(x, y);
                int red = (rgb >>> 16) & 0xFF;
                int green = (rgb >>> 8) & 0xFF;
                int blue = rgb & 0xFF;
                boolean harvestWarmth = red >= 105 && red >= blue * 1.25 && green >= 45;
                if (harvestWarmth) {
                    warmPixels++;
                    if (x < image.getWidth() / 2) leftWarmPixels++;
                    else rightWarmPixels++;
                }
                if (y >= 245 && luminance(rgb) <= 70.0) darkLowerPixels++;
            }
        }

        assertTrue(warmPixels / (double) totalPixels >= 0.05,
                "braziers, banners, offerings, and gold trim must preserve the harvest identity");
        assertTrue(leftWarmPixels >= totalPixels * 0.018
                        && rightWarmPixels >= totalPixels * 0.018,
                "both wings of the tribunal must retain lit civic architecture");
        assertTrue(darkLowerPixels / (double) lowerPixels >= 0.78,
                "stone piers must visibly ground the feast table below every fighting surface");

        double moonCenter = luminance(image.getRGB(320, 52));
        double nightBesideMoon = luminance(image.getRGB(248, 52));
        assertTrue(moonCenter >= 205.0,
                "the harvest moon must remain the court's unmistakable focal point");
        assertTrue(moonCenter - nightBesideMoon >= 145.0,
                "the moon must keep strong separation from the night sky");

        int centerRunner = image.getRGB(320, 252);
        int runnerRed = (centerRunner >>> 16) & 0xFF;
        int runnerGreen = (centerRunner >>> 8) & 0xFF;
        assertTrue(runnerRed >= runnerGreen * 1.7,
                "the continuous crimson tribunal runner must remain readable across the central table");
        assertTrue(luminance(image.getRGB(75, 270)) <= 45.0
                        && luminance(image.getRGB(565, 270)) <= 45.0,
                "the two outer table sections must retain their grounded masonry silhouettes");
    }

    private static double luminance(int rgb) {
        int red = (rgb >>> 16) & 0xFF;
        int green = (rgb >>> 8) & 0xFF;
        int blue = rgb & 0xFF;
        return red * 0.2126 + green * 0.7152 + blue * 0.0722;
    }
}
