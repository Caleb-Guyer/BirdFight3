package com.example.birdgame3;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;

import static com.example.birdgame3.BirdGame3.BirdType;
import static com.example.birdgame3.BirdGame3.ClassicEncounter;
import static com.example.birdgame3.BirdGame3.ClassicEncounterStyle;
import static com.example.birdgame3.BirdGame3.MapVariant;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EagleClassicRouteTest {
    @Test
    void eagleHasTheFixedEightEncounterSkyKingRoute() throws Exception {
        List<ClassicEncounter> route = eagleRoute(new BirdGame3());

        assertEquals(8, route.size());
        assertEquals(List.of(
                        "Needle Squadron",
                        "Talon to Talon",
                        "Night Watch",
                        "The Migration Wall",
                        "Raptors Against Rebirth",
                        "Carrion Crown",
                        "Bonus: Storm Beacon Ascent",
                        "The Weight of the Crown"),
                route.stream().map(encounter -> encounter.name).toList());
        assertEquals(List.of(
                        MapVariant.STANDARD,
                        MapVariant.STANDARD,
                        MapVariant.PARLIAMENT_ROOFTOPS,
                        MapVariant.STANDARD,
                        MapVariant.ASHFALL_REBIRTH,
                        MapVariant.CARRION_THRONE,
                        MapVariant.SKYBREAK_SPIRES,
                        MapVariant.TEMPEST_SUMMIT),
                route.stream().map(encounter -> encounter.variant).toList());
        assertEquals(ClassicEncounterStyle.MINIATURE_FLOCK, route.get(0).style);
        assertEquals(ClassicEncounterStyle.GIANT, route.get(3).style);
        assertEquals(ClassicEncounterStyle.BONUS_RELAY, route.get(6).style);
        assertEquals(ClassicEncounterStyle.STORM_TYRANT_BOSS, route.get(7).style);
        assertTrue(route.getLast().bossFight);
    }

    @Test
    void authoredTeamsMatchTheApprovedRoute() throws Exception {
        List<ClassicEncounter> route = eagleRoute(new BirdGame3());

        assertEquals(3, route.get(0).enemies.length);
        assertTrue(List.of(route.get(0).enemies).stream()
                .allMatch(fighter -> fighter.type() == BirdType.HUMMINGBIRD));
        assertEquals(BirdType.FALCON, route.get(1).enemies[0].type());
        assertEquals(BirdType.PIGEON, route.get(2).allies[0].type());
        assertEquals(List.of(BirdType.RAVEN, BirdType.BAT),
                List.of(route.get(2).enemies[0].type(), route.get(2).enemies[1].type()));
        assertEquals(BirdType.GOOSE, route.get(3).enemies[0].type());
        assertEquals(BirdType.FALCON, route.get(4).allies[0].type());
        assertEquals(List.of(BirdType.PHOENIX, BirdType.HUMMINGBIRD),
                List.of(route.get(4).enemies[0].type(), route.get(4).enemies[1].type()));
        assertEquals(BirdType.VULTURE, route.get(5).enemies[0].type());
        assertEquals(BirdType.EAGLE, route.get(7).enemies[0].type());
        assertTrue(route.get(7).enemies[0].title().contains("Storm Tyrant"));
    }

    @Test
    void stormTyrantUsesTwoStockSmashRulesAndCannotUseAnUltimate() throws Exception {
        BirdGame3 game = preparedEagleGame();
        ClassicEncounter bossEncounter = eagleRoute(game).getLast();
        game.classicEncounter = bossEncounter;
        invoke(game, "setupClassicEncounterRoster", new Class<?>[]{ClassicEncounter.class}, bossEncounter);

        assertTrue(game.classicUsesSmashRules());
        assertEquals(1, game.smashStartingStocks());
        assertFalse(game.players[1].hasUltimate());
        assertTrue(game.players[1].sizeMultiplier >= 1.3);

        game.scores[0] = game.smashStartingStocks();
        game.scores[1] = game.smashStartingStocks();
        invoke(game, "applyClassicEncounterStockOverrides", new Class<?>[0]);
        assertEquals(3, game.scores[0]);
        assertEquals(2, game.scores[1]);
    }

    @Test
    void secondBossStockAddsCrosswindsAndFasterAttacksOnlyOnce() throws Exception {
        BirdGame3 game = preparedEagleGame();
        ClassicEncounter bossEncounter = eagleRoute(game).getLast();
        game.classicEncounter = bossEncounter;
        invoke(game, "setupClassicEncounterRoster", new Class<?>[]{ClassicEncounter.class}, bossEncounter);

        game.selectedMap = bossEncounter.map;
        game.selectedMapVariant = bossEncounter.variant;
        game.currentMatchSeed = 99L;
        invoke(game, "setupMatchArenaGeometry", new Class<?>[0]);
        invoke(game, "applySelectedMapVariantArena", new Class<?>[0]);
        int firstStockVentCount = game.windVents.size();
        Bird tyrant = game.players[1];
        tyrant.attackCooldown = 30;
        tyrant.raptorCryReuseTimer = 400;
        tyrant.raptorRushReuseTimer = 400;
        game.scores[1] = 1;

        invoke(game, "applyStormTyrantRuntimeEffects", new Class<?>[0]);

        assertEquals(firstStockVentCount + 2, game.windVents.size());
        assertEquals(12, tyrant.attackCooldown);
        assertEquals(150, tyrant.raptorCryReuseTimer);
        assertEquals(150, tyrant.raptorRushReuseTimer);
        assertTrue(tyrant.powerMultiplier >= tyrant.basePowerMultiplier * 1.06);
        assertTrue(tyrant.speedMultiplier >= tyrant.baseSpeedMultiplier * 1.06);

        invoke(game, "applyStormTyrantRuntimeEffects", new Class<?>[0]);
        assertEquals(firstStockVentCount + 2, game.windVents.size(),
                "the stock phase must not duplicate vents every simulation tick");
    }

    @Test
    void stormBeaconBonusUsesTheVerticalSpireTargets() throws Exception {
        BirdGame3 game = preparedEagleGame();
        ClassicEncounter bonus = eagleRoute(game).get(6);
        game.classicEncounter = bonus;
        invoke(game, "setupClassicEncounterRoster", new Class<?>[]{ClassicEncounter.class}, bonus);
        invoke(game, "positionClassicEncounterSpawns", new Class<?>[]{ClassicEncounter.class}, bonus);

        assertEquals(BirdGame3.GROUND_Y - 1120.0 - game.players[1].bodyHeight(), game.players[1].y, 0.001);
        assertEquals(BirdGame3.GROUND_Y - 700.0 - game.players[2].bodyHeight(), game.players[2].y, 0.001);
        assertEquals(BirdGame3.GROUND_Y - 1120.0 - game.players[3].bodyHeight(), game.players[3].y, 0.001);
        for (int i = 1; i < 4; i++) {
            assertTrue(game.players[i].classicBonusTarget);
            assertFalse(game.players[i].hasUltimate());
        }
    }

    @Test
    void eagleRouteTitleIsAuthoredInsteadOfPlaceholderText() throws Exception {
        Method title = BirdGame3.class.getDeclaredMethod("classicRouteTitle", BirdType.class);
        title.setAccessible(true);
        assertEquals("THE SKY HAS ONE KING", title.invoke(new BirdGame3(), BirdType.EAGLE));
    }

    private static BirdGame3 preparedEagleGame() throws Exception {
        BirdGame3 game = new BirdGame3();
        game.classicModeActive = true;
        setField(game, "classicSelectedBird", BirdType.EAGLE);
        setField(game, "classicDifficulty", 5.0);
        return game;
    }

    @SuppressWarnings("unchecked")
    private static List<ClassicEncounter> eagleRoute(BirdGame3 game) throws Exception {
        Method build = BirdGame3.class.getDeclaredMethod("buildClassicRun", BirdType.class);
        build.setAccessible(true);
        return (List<ClassicEncounter>) build.invoke(game, BirdType.EAGLE);
    }

    private static Object invoke(BirdGame3 game, String name, Class<?>[] parameterTypes, Object... arguments)
            throws Exception {
        Method method = BirdGame3.class.getDeclaredMethod(name, parameterTypes);
        method.setAccessible(true);
        return method.invoke(game, arguments);
    }

    private static void setField(BirdGame3 game, String name, Object value) throws Exception {
        Field field = BirdGame3.class.getDeclaredField(name);
        field.setAccessible(true);
        field.set(game, value);
    }
}
