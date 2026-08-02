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
}
