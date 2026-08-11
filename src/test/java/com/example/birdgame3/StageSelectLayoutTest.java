package com.example.birdgame3;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StageSelectLayoutTest {
    @Test
    void completeMainCatalogFitsWithoutScrolling() {
        assertEquals(3, StageSelectLayout.rowsFor(BirdGame3.MapType.values().length));
        assertTrue(StageSelectLayout.gridWidth()
                        <= BirdGame3.WIDTH - StageSelectLayout.ROOT_HORIZONTAL_PADDING * 2.0,
                "Five stage cards should fit inside the logical screen width.");
        assertTrue(StageSelectLayout.requiredScreenHeight(
                        StageSelectLayout.gridHeight(BirdGame3.MapType.values().length)) <= BirdGame3.HEIGHT,
                "Every main stage and both random choices should fit without a scroll pane.");
    }

    @Test
    void completeCategorizedVariantCatalogFitsWithoutScrolling() {
        int storyArenas = 0;
        int bossArenas = 0;
        int classicRoutes = 0;
        for (BirdGame3.MapVariant variant : BirdGame3.MapVariant.values()) {
            if (variant == BirdGame3.MapVariant.STANDARD) continue;
            if ("Story Arenas".equals(variant.category)) storyArenas++;
            if ("Boss Rush Arenas".equals(variant.category)) bossArenas++;
            if ("Classic Routes".equals(variant.category)) classicRoutes++;
        }

        assertEquals(1, StageSelectLayout.variantRowsFor(storyArenas));
        assertEquals(2, StageSelectLayout.variantRowsFor(bossArenas));
        assertEquals(1, StageSelectLayout.variantRowsFor(classicRoutes));
        assertTrue(StageSelectLayout.variantGridWidth()
                        <= BirdGame3.WIDTH - StageSelectLayout.ROOT_HORIZONTAL_PADDING * 2.0,
                "Six compact variant cards should fit inside the logical screen width.");
        assertTrue(StageSelectLayout.requiredScreenHeight(
                        StageSelectLayout.groupedCatalogHeight(storyArenas, bossArenas, classicRoutes)) <= BirdGame3.HEIGHT,
                "All labeled variant sections and random choices should fit without scrolling.");
    }
}
