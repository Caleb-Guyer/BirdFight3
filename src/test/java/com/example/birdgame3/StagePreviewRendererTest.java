package com.example.birdgame3;

import javafx.scene.canvas.Canvas;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StagePreviewRendererTest {
    @Test
    void everyMainStageAndVariantHasDeterministicPreviewArt() {
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
}
