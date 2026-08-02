package com.example.birdgame3;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.List;

import static com.example.birdgame3.BirdGame3.MapType;
import static com.example.birdgame3.BirdGame3.MapVariant;
import static com.example.birdgame3.BirdGame3.StageChoice;
import static com.example.birdgame3.BirdGame3.StageRandomPool;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MapVariantTest {
    @Test
    void variantChoiceAlwaysUsesItsAuthoredBaseMap() {
        StageChoice choice = new StageChoice(MapType.FOREST, MapVariant.PARLIAMENT_ROOFTOPS);

        assertEquals(MapType.CITY, choice.map());
        assertEquals(MapVariant.PARLIAMENT_ROOFTOPS, choice.variant());
    }

    @Test
    void randomPoolsKeepMainAndVariantCatalogsSeparate() {
        BirdGame3 game = new BirdGame3();

        List<StageChoice> main = game.availableStageChoices(StageRandomPool.MAIN);
        List<StageChoice> variants = game.availableStageChoices(StageRandomPool.VARIANTS);
        List<StageChoice> all = game.availableStageChoices(StageRandomPool.ALL);

        assertTrue(main.stream().allMatch(choice -> choice.variant() == MapVariant.STANDARD));
        assertTrue(variants.stream().allMatch(choice -> choice.variant() != MapVariant.STANDARD));
        assertTrue(variants.stream().anyMatch(choice -> choice.variant() == MapVariant.CROWN_DUEL));
        assertTrue(all.containsAll(main));
        assertTrue(all.containsAll(variants));
    }

    @Test
    void everyVariantBuildsAPlayableArena() throws Exception {
        BirdGame3 game = new BirdGame3();
        game.currentMatchSeed = 42L;
        Method setupBase = BirdGame3.class.getDeclaredMethod("setupMatchArenaGeometry");
        Method applyVariant = BirdGame3.class.getDeclaredMethod("applySelectedMapVariantArena");
        setupBase.setAccessible(true);
        applyVariant.setAccessible(true);

        for (MapVariant variant : MapVariant.values()) {
            if (variant == MapVariant.STANDARD) continue;
            game.selectedMap = variant.baseMap;
            game.selectedMapVariant = variant;
            setupBase.invoke(game);
            applyVariant.invoke(game);
            assertFalse(game.platforms.isEmpty(), variant + " must leave solid platforms in the arena");
        }
    }

    @Test
    void commandBridgeUsesTheCompactStoryLayout() throws Exception {
        BirdGame3 game = new BirdGame3();
        game.currentMatchSeed = 7L;
        game.selectedMap = MapType.SKYCLIFFS;
        game.selectedMapVariant = MapVariant.CROWN_DUEL;
        Method setupBase = BirdGame3.class.getDeclaredMethod("setupMatchArenaGeometry");
        Method applyVariant = BirdGame3.class.getDeclaredMethod("applySelectedMapVariantArena");
        setupBase.setAccessible(true);
        applyVariant.setAccessible(true);

        setupBase.invoke(game);
        applyVariant.invoke(game);

        assertEquals(4, game.platforms.size());
        assertTrue(game.platforms.stream().anyMatch(platform -> platform.w == 2200.0));
    }

    @Test
    void ashfallRebirthLeavesTheTopBlastRouteOpen() throws Exception {
        BirdGame3 game = buildVariant(MapVariant.ASHFALL_REBIRTH);

        assertFalse(game.platforms.stream().anyMatch(platform -> platform.y <= 0.0),
                "Ashfall Rebirth must not recreate the full-width ceiling trap");
        assertFalse(game.platforms.stream().anyMatch(platform -> platform.y >= BirdGame3.GROUND_Y
                        && platform.w >= BirdGame3.WORLD_WIDTH),
                "the Rebirth Altar should remain a floating arena");
    }

    @Test
    void redesignedBossArenasDoNotReuseTheirMainStageSilhouettes() throws Exception {
        BirdGame3 titanDock = buildVariant(MapVariant.TITAN_DOCK);
        BirdGame3 parliament = buildVariant(MapVariant.PARLIAMENT_ROOFTOPS);
        BirdGame3 nullRoc = buildVariant(MapVariant.NULL_ROC_ASCENDING);
        BirdGame3 voidCrown = buildVariant(MapVariant.VOID_CROWN);

        assertFalse(titanDock.platforms.stream().anyMatch(platform -> platform.x == 720.0 && platform.w == 1820.0));
        assertFalse(parliament.platforms.stream().anyMatch(platform -> platform.w >= BirdGame3.WORLD_WIDTH));
        assertFalse(nullRoc.platforms.stream().anyMatch(platform -> platform.w == 2920.0));
        assertFalse(voidCrown.platforms.stream().anyMatch(platform -> platform.w == 2920.0));
        assertTrue(titanDock.usesIslandBoundsForCurrentArena());
        assertTrue(parliament.usesIslandBoundsForCurrentArena());
    }

    private BirdGame3 buildVariant(MapVariant variant) throws Exception {
        BirdGame3 game = new BirdGame3();
        game.currentMatchSeed = 91L;
        game.selectedMap = variant.baseMap;
        game.selectedMapVariant = variant;
        Method setupBase = BirdGame3.class.getDeclaredMethod("setupMatchArenaGeometry");
        Method applyVariant = BirdGame3.class.getDeclaredMethod("applySelectedMapVariantArena");
        setupBase.setAccessible(true);
        applyVariant.setAccessible(true);
        setupBase.invoke(game);
        applyVariant.invoke(game);
        return game;
    }
}
