package com.example.birdgame3;

import javafx.scene.canvas.Canvas;
import javafx.scene.image.Image;
import javafx.scene.image.PixelReader;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StagePreviewRendererTest {
    @Test
    void everyMainStageAndVariantHasCapturedPreviewArt() {
        Canvas tile = new Canvas(StageSelectLayout.TILE_IMAGE_WIDTH, StageSelectLayout.TILE_IMAGE_HEIGHT);
        Canvas hero = new Canvas(StageSelectLayout.PREVIEW_CANVAS_WIDTH, StageSelectLayout.PREVIEW_CANVAS_HEIGHT);

        for (BirdGame3.MapType map : BirdGame3.MapType.values()) {
            BirdGame3.StageChoice choice = BirdGame3.StageChoice.main(map);
            assertTrue(StagePreviewRenderer.capturedPreviewResourceExists(choice),
                    "Missing literal stage capture for " + map);
            StagePreviewRenderer.draw(tile, choice);
            StagePreviewRenderer.draw(hero, choice);
            assertNotNull(StagePreviewRenderer.accentFor(choice), map.name());
        }
        for (BirdGame3.MapVariant variant : BirdGame3.MapVariant.values()) {
            if (variant == BirdGame3.MapVariant.STANDARD) continue;
            BirdGame3.StageChoice choice = new BirdGame3.StageChoice(variant.baseMap, variant);
            assertTrue(StagePreviewRenderer.capturedPreviewResourceExists(choice),
                    "Missing literal stage capture for " + variant);
            StagePreviewRenderer.draw(tile, choice);
            StagePreviewRenderer.draw(hero, choice);
            assertNotNull(StagePreviewRenderer.accentFor(choice), variant.name());
        }
        StagePreviewRenderer.drawRandom(tile);
        StagePreviewRenderer.drawRandom(hero);
    }

    @Test
    void capturedStagePhotosAspectFillCardsWithoutDistortion() {
        StagePreviewRenderer.SourceCrop sameRatio =
                StagePreviewRenderer.sourceCrop(640, 360, 320, 180);
        assertEquals(new StagePreviewRenderer.SourceCrop(0, 0, 640, 360), sameRatio);

        StagePreviewRenderer.SourceCrop squareCard =
                StagePreviewRenderer.sourceCrop(640, 360, 200, 200);
        assertEquals(140.0, squareCard.x(), 0.0001);
        assertEquals(0.0, squareCard.y(), 0.0001);
        assertEquals(360.0, squareCard.width(), 0.0001);
        assertEquals(360.0, squareCard.height(), 0.0001);

        StagePreviewRenderer.SourceCrop wideCard =
                StagePreviewRenderer.sourceCrop(640, 360, 400, 160);
        assertEquals(0.0, wideCard.x(), 0.0001);
        assertEquals(52.0, wideCard.y(), 0.0001);
        assertEquals(640.0, wideCard.width(), 0.0001);
        assertEquals(256.0, wideCard.height(), 0.0001);
    }

    @Test
    void featherpediaAndRewardMapArtUsesTheCapturedPreviewRenderer() {
        for (BirdGame3.MapType map : BirdGame3.MapType.values()) {
            Canvas backdrop = new Canvas(360, 200);
            Canvas tile = new Canvas(130, 90);
            BirdBookUiSupport.drawMapBackdrop(backdrop, map);
            BirdBookUiSupport.drawMapPreview(tile, map);
            assertTrue(StagePreviewRenderer.capturedPreviewResourceExists(BirdGame3.StageChoice.main(map)),
                    "Featherpedia map art needs a real capture for " + map);
        }
    }

    @Test
    void previewSystemVisuallyDistinguishesMainAndVariantFamilies() {
        Set<String> mainAccents = new HashSet<>();
        for (BirdGame3.MapType map : BirdGame3.MapType.values()) {
            mainAccents.add(StagePreviewRenderer.accentFor(BirdGame3.StageChoice.main(map)).toString());
        }
        assertTrue(mainAccents.size() >= 8, "The stage grid should not collapse every main arena into one theme.");

        Set<String> variantAccents = new HashSet<>();
        for (String category : Set.of("Story Arenas", "Boss Rush Arenas", "Classic Routes")) {
            BirdGame3.MapVariant representative = null;
            for (BirdGame3.MapVariant variant : BirdGame3.MapVariant.values()) {
                if (category.equals(variant.category)) {
                    representative = variant;
                    break;
                }
            }
            assertNotNull(representative, category);
            variantAccents.add(StagePreviewRenderer.accentFor(
                    new BirdGame3.StageChoice(representative.baseMap, representative)).toString());
        }
        assertEquals(3, variantAccents.size(), "Story, Boss Rush, and Classic tiles need separate visual identities.");
    }

    @Test
    void rooftopRelayCaptureDoesNotRestoreTheLegacyGoldGapWalls() {
        try (InputStream stream = StagePreviewRendererTest.class.getResourceAsStream(
                "/stage-previews/variant-rooftop-relay.png")) {
            assertNotNull(stream, "Rooftop Relay's shared stage capture must be bundled");
            Image image = new Image(stream);
            PixelReader pixels = image.getPixelReader();
            assertNotNull(pixels, "Rooftop Relay's capture must be readable");

            int longestWarmRun = 0;
            for (int x = 0; x < (int) image.getWidth(); x++) {
                int warmRun = 0;
                for (int y = (int) image.getHeight() / 2; y < (int) image.getHeight(); y++) {
                    var color = pixels.getColor(x, y);
                    boolean legacyGold = color.getRed() > 0.74
                            && color.getGreen() > 0.47
                            && color.getBlue() < 0.51
                            && color.getRed() - color.getBlue() > 0.23;
                    warmRun = legacyGold ? warmRun + 1 : 0;
                    longestWarmRun = Math.max(longestWarmRun, warmRun);
                }
            }

            assertTrue(longestWarmRun < 12,
                    "Open skyline gaps must fade into cool cloud depth, not tall gold edge strips");
        } catch (java.io.IOException exception) {
            throw new AssertionError("Could not inspect Rooftop Relay's shared stage capture", exception);
        }
    }
}
