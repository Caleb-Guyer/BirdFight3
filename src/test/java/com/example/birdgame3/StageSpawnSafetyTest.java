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
    void standaloneRooftopRelayDistributesFightersAcrossSeparateBuildings() throws Exception {
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
        String[] expectedRoofs = {"HOME", "MARKET", "TRANSIT", "RADIO"};
        for (int slot = 0; slot < game.activePlayers; slot++) {
            Bird fighter = game.players[slot];
            Platform support = game.platforms.stream()
                    .filter(platform -> fighter.bodyCenterX() >= platform.x
                            && fighter.bodyCenterX() <= platform.x + platform.w
                            && Math.abs(fighter.bodyBottomY() - platform.y) <= 0.001)
                    .findFirst()
                    .orElse(null);
            assertNotNull(support, "fighter " + slot + " must stand on a real rooftop");
            assertEquals(expectedRoofs[slot], support.signText,
                    "the opening formation must advance one building per fighter");
            assertEquals(fighter.x, fighter.prevX, 0.001);
            assertEquals(fighter.y, fighter.prevY, 0.001);
            if (slot > 0) {
                assertTrue(fighter.bodyCenterX() - game.players[slot - 1].bodyCenterX() >= 700.0,
                        "adjacent fighters must not overlap or begin in immediate attack range");
            }
        }
    }

    @Test
    void fourPlayerIslandStartUsesMostOfTheSafeFightingSurface() throws Exception {
        BirdGame3 game = new BirdGame3();
        game.selectedMap = BirdGame3.MapType.BATTLEFIELD;
        game.selectedMapVariant = BirdGame3.MapVariant.STANDARD;
        game.activePlayers = 4;
        for (int slot = 0; slot < game.activePlayers; slot++) {
            game.players[slot] = new Bird(0.0, BirdGame3.BirdType.values()[slot], slot, game);
        }

        invoke(game, "setupMatchArenaGeometry");
        invoke(game, "applySelectedMapVariantArena");
        Platform main = game.authoredAiMainStagePlatform();
        assertNotNull(main);

        invoke(game, "positionBattlefieldSpawns");
        double firstCenter = game.players[0].bodyCenterX();
        double lastCenter = game.players[3].bodyCenterX();
        assertTrue(lastCenter - firstCenter >= main.w * 0.55,
                "four fighters should use the arena width instead of clustering in its middle third");
        for (int slot = 1; slot < game.activePlayers; slot++) {
            Bird left = game.players[slot - 1];
            Bird right = game.players[slot];
            double requiredGap = (left.bodyWidth() + right.bodyWidth()) * 0.5 + 40.0;
            assertTrue(right.bodyCenterX() - left.bodyCenterX() >= requiredGap,
                    "opening collision boxes must have a visible safety gap");
        }
    }

    @Test
    void rooftopRelayTeamMatchSeparatesSidesWithoutStackingTeammates() throws Exception {
        BirdGame3 game = new BirdGame3();
        game.selectedMap = BirdGame3.MapType.CITY;
        game.selectedMapVariant = BirdGame3.MapVariant.ROOFTOP_RELAY;
        game.classicModeActive = true;
        game.classicTeamMode = true;
        game.activePlayers = 3;
        game.players[0] = new Bird(0.0, BirdGame3.BirdType.TITMOUSE, 0, game);
        game.players[1] = new Bird(0.0, BirdGame3.BirdType.ROADRUNNER, 1, game);
        game.players[2] = new Bird(0.0, BirdGame3.BirdType.GOOSE, 2, game);
        game.classicTeams[0] = 1;
        game.classicTeams[1] = 2;
        game.classicTeams[2] = 2;

        invoke(game, "setupMatchArenaGeometry");
        invoke(game, "applySelectedMapVariantArena");
        invoke(game, "positionBattlefieldSpawns");

        assertEquals("HOME", supportingPlatform(game, game.players[0]).signText);
        assertEquals("MARKET", supportingPlatform(game, game.players[1]).signText);
        assertEquals("MARKET", supportingPlatform(game, game.players[2]).signText);
        assertTrue(Math.abs(game.players[2].bodyCenterX() - game.players[1].bodyCenterX()) >= 500.0,
                "fighters sharing a team roof still need distinct starting spaces");
        assertTrue(game.players[0].bodyCenterX() < game.players[1].bodyCenterX(),
                "opposing teams should face each other from separate buildings");
    }

    private static void invoke(BirdGame3 game, String methodName) throws Exception {
        Method method = BirdGame3.class.getDeclaredMethod(methodName);
        method.setAccessible(true);
        method.invoke(game);
    }

    private static Platform supportingPlatform(BirdGame3 game, Bird fighter) {
        return game.platforms.stream()
                .filter(platform -> fighter.bodyCenterX() >= platform.x
                        && fighter.bodyCenterX() <= platform.x + platform.w
                        && Math.abs(fighter.bodyBottomY() - platform.y) <= 0.001)
                .findFirst()
                .orElseThrow();
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
