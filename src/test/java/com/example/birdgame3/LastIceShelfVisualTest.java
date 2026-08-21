package com.example.birdgame3;

import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LastIceShelfVisualTest {
    private static final Path CAPTURE = Path.of(
            "src/main/resources/stage-previews/variant-last-ice-shelf.png");

    @Test
    void captureKeepsItsConnectedIcebergOpenWaterAndMirroredMeltingTowers() throws IOException {
        BufferedImage image = ImageIO.read(CAPTURE.toFile());
        assertNotNull(image, "Last Ice Shelf capture");
        assertEquals(640, image.getWidth());
        assertEquals(360, image.getHeight());

        int icePixels = 0;
        int leftIcePixels = 0;
        int rightIcePixels = 0;
        int brightSnowPixels = 0;
        int deepLowerPixels = 0;
        double brightestMoonPixel = 0.0;
        int totalPixels = image.getWidth() * image.getHeight();
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                int rgb = image.getRGB(x, y);
                int red = red(rgb);
                int green = green(rgb);
                int blue = blue(rgb);
                boolean glacierIce = blue >= 70 && green >= red * 1.14 && blue >= red * 1.18;
                if (glacierIce) {
                    icePixels++;
                    if (x < image.getWidth() / 2) leftIcePixels++;
                    else rightIcePixels++;
                }
                if (red >= 170 && green >= 200 && blue >= 210) brightSnowPixels++;
                if (y >= 255 && luminance(rgb) <= 55.0) deepLowerPixels++;
                if (x >= 450 && y < 80) {
                    brightestMoonPixel = Math.max(brightestMoonPixel, luminance(rgb));
                }
            }
        }

        assertTrue(icePixels / (double) totalPixels >= 0.62,
                "the stage must remain dominated by its connected carved glacier and submerged mass");
        assertTrue(leftIcePixels >= totalPixels * 0.31
                        && rightIcePixels >= totalPixels * 0.30,
                "the two recovery wings and melting towers must remain visually balanced");
        assertTrue(brightSnowPixels / (double) totalPixels >= 0.012,
                "snowcaps and permanent platform rims must remain bright against the dark polar sky");
        assertTrue(deepLowerPixels / (double) (640 * 105) >= 0.44,
                "black water must remain visible around and beneath the submerged iceberg");
        assertTrue(brightestMoonPixel >= 215.0,
                "the moon must remain a clear focal point above the aurora-lit fortress");

        int connectedShelfPixels = 0;
        for (int x = 75; x < 565; x++) {
            int rgb = image.getRGB(x, 249);
            if (blue(rgb) >= 100 && green(rgb) >= red(rgb) * 1.14
                    && blue(rgb) >= red(rgb) * 1.18) {
                connectedShelfPixels++;
            }
        }
        assertTrue(connectedShelfPixels >= 475,
                "the main shelf and both recovery wings must remain one unbroken glacier silhouette");

        int westRecovery = image.getRGB(80, 266);
        int eastRecovery = image.getRGB(560, 266);
        assertTrue(luminance(westRecovery) >= 175.0 && luminance(eastRecovery) >= 175.0,
                "both low recovery shelves must remain readable above the open water");
        assertTrue(Math.abs(luminance(westRecovery) - luminance(eastRecovery)) <= 8.0,
                "the mirrored recovery shelves must retain comparable snow and ice values");

        int centralUnderwater = image.getRGB(320, 340);
        assertTrue(blue(centralUnderwater) >= red(centralUnderwater) * 3.0,
                "the lower center must still read as submerged blue ice rather than solid ground");
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
