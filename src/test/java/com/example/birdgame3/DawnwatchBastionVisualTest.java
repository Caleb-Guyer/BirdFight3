package com.example.birdgame3;

import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DawnwatchBastionVisualTest {
    private static final Path CAPTURE = Path.of(
            "src/main/resources/stage-previews/variant-dawnwatch-bastion.png");

    @Test
    void captureKeepsItsBellCitadelAndSupportedArchitecture() throws IOException {
        BufferedImage image = ImageIO.read(CAPTURE.toFile());
        assertNotNull(image, "Dawnwatch Bastion capture");
        assertEquals(640, image.getWidth());
        assertEquals(360, image.getHeight());

        int warmMetalPixels = 0;
        int darkLowerArchitecture = 0;
        int totalPixels = image.getWidth() * image.getHeight();
        int lowerPixels = image.getWidth() * (image.getHeight() - 210);
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                int rgb = image.getRGB(x, y);
                int red = (rgb >>> 16) & 0xFF;
                int green = (rgb >>> 8) & 0xFF;
                int blue = rgb & 0xFF;
                if (red >= 135 && green >= 90 && blue <= 100) warmMetalPixels++;
                if (y >= 210 && luminance(rgb) <= 70.0) darkLowerArchitecture++;
            }
        }

        assertTrue(warmMetalPixels / (double) totalPixels >= 0.055,
                "the dawn bell, trim, windows, and banners must remain readable at card size");
        assertTrue(darkLowerArchitecture / (double) lowerPixels >= 0.60,
                "grounded tower masonry must fill the lower stage instead of leaving floating ledges");

        double bell = luminance(image.getRGB(320, 104));
        double bellLeftChamber = luminance(image.getRGB(255, 113));
        double bellRightChamber = luminance(image.getRGB(385, 113));
        assertTrue(bell >= 150.0,
                "the great bell must remain the stage's bright central landmark");
        assertTrue(bell - Math.max(bellLeftChamber, bellRightChamber) >= 95.0,
                "the bell must keep strong contrast against its dark keep");

        double leftSun = luminance(image.getRGB(286, 31));
        double rightSun = luminance(image.getRGB(354, 31));
        assertTrue(leftSun >= 175.0 && rightSun >= 175.0,
                "the sunrise must remain visible behind both shoulders of the bell tower");
        assertTrue(Math.abs(leftSun - rightSun) <= 18.0,
                "the centered citadel silhouette must not drift away from the dawn focal point");

        assertTrue(luminance(image.getRGB(64, 280)) <= 90.0
                        && luminance(image.getRGB(576, 280)) <= 90.0,
                "both side towers must visibly continue beneath their playable battlements");
    }

    private static double luminance(int rgb) {
        int red = (rgb >>> 16) & 0xFF;
        int green = (rgb >>> 8) & 0xFF;
        int blue = rgb & 0xFF;
        return red * 0.2126 + green * 0.7152 + blue * 0.0722;
    }
}
