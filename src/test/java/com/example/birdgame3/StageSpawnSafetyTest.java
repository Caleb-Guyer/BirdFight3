package com.example.birdgame3;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StageSpawnSafetyTest {

    @Test
    void everySelectableStagePlacesNormalMatchFightersOnSolidSurface() throws Exception {
        for (BirdGame3.MapType map : BirdGame3.MapType.values()) {
            assertStageSpawnsAreSupported(map, BirdGame3.MapVariant.STANDARD);
        }
        for (BirdGame3.MapVariant variant : BirdGame3.MapVariant.values()) {
            if (variant != BirdGame3.MapVariant.STANDARD) {
                assertStageSpawnsAreSupported(variant.baseMap, variant);
            }
        }
    }

    @Test
    void standaloneRooftopRelaySpawnsEveryFighterOnHomeRoof() throws Exception {
        BirdGame3 game = new BirdGame3();
        game.selectedMap = BirdGame3.MapType.CITY;
        game.selectedMapVariant = BirdGame3.MapVariant.ROOFTOP_RELAY;
        game.activePlayers = 4;
        for (int slot = 0; slot < game.activePlayers; slot++) {
            game.players[slot] = new Bird(600 + slot * 200,
                    slot == 0 ? BirdGame3.BirdType.PIGEON : BirdGame3.BirdType.FALCON,
                    slot, game);
        }

        invoke(game, "setupMatchArenaGeometry");
        invoke(game, "applySelectedMapVariantArena");
        assertFalse(game.usesIslandBoundsForCurrentArena(),
                "the long relay must retain its full-course blast bounds");

        Platform home = game.authoredAiMainStagePlatform();
        assertNotNull(home);
        assertEquals("HOME", home.signText);

        invoke(game, "positionBattlefieldSpawns");
        for (int slot = 0; slot < game.activePlayers; slot++) {
            Bird fighter = game.players[slot];
            assertEquals(home.y, fighter.bodyBottomY(), 0.001,
                    "fighter " + slot + " must stand on the HOME roof at match start");
            assertTrue(fighter.bodyCenterX() >= home.x && fighter.bodyCenterX() <= home.x + home.w,
                    "fighter " + slot + " must start inside the HOME roof");
        }
    }

    private static void invoke(BirdGame3 game, String methodName) throws Exception {
        Method method = BirdGame3.class.getDeclaredMethod(methodName);
        method.setAccessible(true);
        method.invoke(game);
    }

    private static void assertStageSpawnsAreSupported(BirdGame3.MapType map,
                                                       BirdGame3.MapVariant variant) throws Exception {
        BirdGame3 game = new BirdGame3();
        game.selectedMap = map;
        game.selectedMapVariant = variant;
        game.activePlayers = 4;
        for (int slot = 0; slot < game.activePlayers; slot++) {
            game.players[slot] = new Bird(400 + slot * 220,
                    BirdGame3.BirdType.values()[slot], slot, game);
        }

        invoke(game, "setupMatchArenaGeometry");
        invoke(game, "applySelectedMapVariantArena");
        invoke(game, "positionBattlefieldSpawns");

        for (int slot = 0; slot < game.activePlayers; slot++) {
            int fighterSlot = slot;
            Bird fighter = game.players[slot];
            boolean supported = game.platforms.stream().anyMatch(platform ->
                    fighter.bodyCenterX() >= platform.x
                            && fighter.bodyCenterX() <= platform.x + platform.w
                            && Math.abs(fighter.bodyBottomY() - platform.y) <= 0.001);
            assertTrue(supported, () -> map + " / " + variant
                    + " spawned fighter " + fighterSlot + " without a solid surface at ("
                    + fighter.bodyCenterX() + ", " + fighter.bodyBottomY() + ")");
        }
    }
}
