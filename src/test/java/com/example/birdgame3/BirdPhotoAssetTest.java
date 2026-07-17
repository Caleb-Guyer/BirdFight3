package com.example.birdgame3;

import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.security.MessageDigest;
import java.util.HexFormat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BirdPhotoAssetTest {

    private static final String APPROVED_IDLE_EAGLE_SHA256 =
            "FB6D660E0BA4A760D9DFF53D08FD5989D8FD52F65726793E4B362120E1ECA6DB";

    @Test
    void idleEagleUsesApprovedPublicDomainCutoutWithSafeTransparentMargins() throws Exception {
        byte[] bytes;
        try (InputStream input = Bird.class.getResourceAsStream("/eagle.png")) {
            assertNotNull(input, "The public-domain idle Eagle cutout must be packaged with the game");
            bytes = input.readAllBytes();
        }

        String hash = HexFormat.of().withUpperCase().formatHex(
                MessageDigest.getInstance("SHA-256").digest(bytes));
        assertEquals(APPROVED_IDLE_EAGLE_SHA256, hash,
                "The approved, credited Eagle export must not be replaced by an unreviewed image");

        BufferedImage image = ImageIO.read(new ByteArrayInputStream(bytes));
        assertNotNull(image);
        assertTrue(image.getColorModel().hasAlpha(), "The Eagle must remain a transparent cutout");
        assertEquals(image.getWidth(), image.getHeight(), "The combat cutout canvas must remain square");

        int minX = image.getWidth();
        int minY = image.getHeight();
        int maxX = -1;
        int maxY = -1;
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                if ((image.getRGB(x, y) >>> 24) <= 12) {
                    continue;
                }
                minX = Math.min(minX, x);
                minY = Math.min(minY, y);
                maxX = Math.max(maxX, x);
                maxY = Math.max(maxY, y);
            }
        }

        assertTrue(maxX >= minX && maxY >= minY, "The Eagle cutout must contain visible pixels");
        int margin = 48;
        assertTrue(minX >= margin && minY >= margin
                        && maxX < image.getWidth() - margin
                        && maxY < image.getHeight() - margin,
                "The Eagle silhouette needs at least " + margin + " transparent pixels on every edge");
    }
}
