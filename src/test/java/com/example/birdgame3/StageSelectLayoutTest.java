package com.example.birdgame3;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StageSelectLayoutTest {
    @Test
    void completeUnifiedCatalogAndRandomFitWithoutScrolling() {
        int mainStages = BirdGame3.MapType.values().length;
        int variants = BirdGame3.MapVariant.values().length - 1;
        int totalTiles = mainStages + variants + 1;

        assertEquals(48, totalTiles, "Update the fixed-grid contract whenever a stage is added.");
        assertEquals(5, StageSelectLayout.rowsFor(totalTiles));
        assertTrue(totalTiles <= StageSelectLayout.capacity());
        assertTrue(StageSelectLayout.contentWidth()
                        <= BirdGame3.WIDTH - StageSelectLayout.ROOT_HORIZONTAL_PADDING * 2.0,
                "The enlarged preview and complete stage grid must fit inside the logical screen width.");
        assertTrue(StageSelectLayout.requiredScreenHeight(totalTiles) <= BirdGame3.HEIGHT,
                "Every stage, Random, the preview, and footer must fit without a scroll pane.");
    }

    @Test
    void developerCatalogContainsEveryMainStageAndVariantExactlyOnce() throws Exception {
        BirdGame3 game = new BirdGame3();
        Method developerUnlock = BirdGame3.class.getDeclaredMethod("unlockEverythingForDeveloperProfile");
        developerUnlock.setAccessible(true);
        developerUnlock.invoke(game);

        List<BirdGame3.StageChoice> catalog = game.unifiedStageSelectCatalog();
        assertEquals(BirdGame3.MapType.values().length + BirdGame3.MapVariant.values().length - 1,
                catalog.size());
        assertEquals(catalog.size(), new HashSet<>(catalog).size(), "The unified grid cannot contain duplicate tiles.");

        Set<BirdGame3.MapType> mainStages = new HashSet<>();
        Set<BirdGame3.MapVariant> variants = new HashSet<>();
        for (BirdGame3.StageChoice choice : catalog) {
            if (choice.variant() == BirdGame3.MapVariant.STANDARD) {
                mainStages.add(choice.map());
            } else {
                variants.add(choice.variant());
            }
        }
        assertEquals(Set.of(BirdGame3.MapType.values()), mainStages);
        Set<BirdGame3.MapVariant> expectedVariants = new HashSet<>(List.of(BirdGame3.MapVariant.values()));
        expectedVariants.remove(BirdGame3.MapVariant.STANDARD);
        assertEquals(expectedVariants, variants);
    }
}
