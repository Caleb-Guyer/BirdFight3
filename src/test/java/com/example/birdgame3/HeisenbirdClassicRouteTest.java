package com.example.birdgame3;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;

import static com.example.birdgame3.BirdGame3.BirdType;
import static com.example.birdgame3.BirdGame3.ClassicEncounter;
import static com.example.birdgame3.BirdGame3.ClassicEncounterStyle;
import static com.example.birdgame3.BirdGame3.MapType;
import static org.junit.jupiter.api.Assertions.*;

class HeisenbirdClassicRouteTest {
    @Test
    void perfectProductIsEightUniqueSelfContainedEncounters() throws Exception {
        List<ClassicEncounter> route = route(new BirdGame3());

        assertEquals(8, route.size());
        assertEquals(List.of("Raw Materials", "The Original Formula", "Stress Test",
                        "Market Pressure", "Counterfeit Product", "Hostile Takeover",
                        "Bonus: Final Calibration", "Product Launch"),
                route.stream().map(encounter -> encounter.name).toList());
        assertEquals(List.of(MapType.CARRION_EXCHANGE, MapType.ONEIRIC_OBSERVATORY,
                        MapType.ASHFALL_CATHEDRAL, MapType.CITY, MapType.SILENT_AMPHITHEATER,
                        MapType.BEACON_CROWN, MapType.STORMGLASS_REFINERY,
                        MapType.STORMGLASS_REFINERY),
                route.stream().map(encounter -> encounter.map).toList());
        assertEquals(ClassicEncounterStyle.HEISEN_CALIBRATION, route.get(6).style);
        assertEquals(ClassicEncounterStyle.HEISEN_BLUE_SKY_ENGINE_BOSS, route.get(7).style);
        assertTrue(route.getLast().bossFight);
        assertEquals("THE PERFECT PRODUCT", invoke(new BirdGame3(), "classicRouteTitle",
                new Class<?>[]{BirdType.class}, BirdType.HEISENBIRD));
    }

    @Test
    void allEnemiesAreUltlessAndBatchRoundsAdvance() throws Exception {
        for (int round = 0; round < 8; round++) {
            BirdGame3 game = prepared(round, 0x481000L + round, 0x482000L + round);
            for (int slot = 1; slot < game.activePlayers; slot++) {
                Bird enemy = game.players[slot];
                if (enemy != null && game.getEffectiveTeam(slot) == 2) assertFalse(enemy.hasUltimate());
            }
        }
        for (int round : List.of(2, 4, 5)) {
            BirdGame3 game = prepared(round, 0x483000L + round, 0x484000L + round);
            int waves = game.classicEncounter.waves.length;
            for (int wave = 0; wave < waves; wave++) {
                for (int slot = 1; slot < game.activePlayers; slot++) {
                    if (game.players[slot] != null && game.getEffectiveTeam(slot) == 2) {
                        game.players[slot].health = 0.0;
                        game.scores[slot] = 0;
                    }
                }
                assertEquals(wave + 1 < waves, game.holdClassicHeisenbirdEncounterOpen());
            }
        }
    }

    @Test
    void stressTestUsesThreeFairTrialsAndRepairsBetweenBatches() throws Exception {
        List<ClassicEncounter> route = route(new BirdGame3());
        ClassicEncounter stress = route.get(2);

        assertEquals(4, stress.cpuLevel);
        assertNotNull(stress.waves);
        assertEquals(3, stress.waves.length);
        assertArrayEquals(new double[]{76.0, 78.0, 82.0},
                new double[]{stress.waves[0][0].health(), stress.waves[1][0].health(),
                        stress.waves[2][0].health()}, 0.001);
        assertArrayEquals(new double[]{0.68, 0.68, 0.70},
                new double[]{stress.waves[0][0].powerMult(), stress.waves[1][0].powerMult(),
                        stress.waves[2][0].powerMult()}, 0.001);

        BirdGame3 game = prepared(2, 0x483200L, 0x483201L);
        Bird player = game.players[0];
        player.setStartingSmashDamagePercent(70.0);
        for (int slot = 1; slot < game.activePlayers; slot++) {
            if (game.players[slot] != null && game.getEffectiveTeam(slot) == 2) {
                game.players[slot].health = 0.0;
                game.scores[slot] = 0;
            }
        }
        assertTrue(game.holdClassicHeisenbirdEncounterOpen());
        assertEquals(48.0, player.smashDamagePercent(), 0.001,
                "Each completed stress batch must repair 22 damage before the next trial.");
    }

    @Test
    void counterfeitBalanceAppliesToEveryDynamicallySpawnedBatch() {
        BirdGame3 game = prepared(4, 0x483100L, 0x483101L);
        Bird firstBatch = firstEnemy(game);
        assertNotNull(firstBatch);
        double calibratedSize = firstBatch.baseSizeMultiplier;

        firstBatch.health = 0.0;
        game.scores[firstBatch.playerIndex] = 0;
        assertTrue(game.holdClassicHeisenbirdEncounterOpen());

        Bird secondBatch = firstEnemy(game);
        assertNotNull(secondBatch);
        assertEquals(calibratedSize, secondBatch.baseSizeMultiplier, 0.0001);
        assertFalse(secondBatch.hasUltimate());
    }

    @Test
    void ordinaryRoundsDoNotShowMeaninglessResetFooter() throws Exception {
        BirdGame3 game = prepared(0, 0x484001L, 0x484002L);
        @SuppressWarnings("unchecked")
        List<String> lines = (List<String>) invoke(game, "fightHudInfoLines", new Class<?>[0]);

        assertEquals(3, lines.size());
        assertTrue(lines.stream().noneMatch(line -> line.contains("RESET AFTER THIS ROUND")));
        assertTrue(lines.stream().noneMatch(line -> line.startsWith("PRODUCTION TRIAL")));
    }

    @Test
    void finalCalibrationIsOrderedReachableGuidedAndTimeoutSafe() throws Exception {
        BirdGame3 game = prepared(6, 0x485001L, 0x485002L);
        game.headlessHarnessMode = true;
        Bird player = game.players[0];
        @SuppressWarnings("unchecked")
        List<Object> regulators = (List<Object>) get(game, "classicHeisenRegulators");
        assertEquals(7, regulators.size());
        for (int index = 0; index < regulators.size(); index++) {
            Object regulator = regulators.get(index);
            double x = (double) get(regulator, "x");
            double y = (double) get(regulator, "y");
            assertTrue(x > 500.0 && x < BirdGame3.WORLD_WIDTH - 500.0);
            assertTrue(y > 150.0 && y < BirdGame3.GROUND_Y);
            player.x = x - player.bodyWidth() * 0.5;
            player.y = y - player.bodyHeight() * 0.5;
            game.applyHeisenbirdClassicRuntimeEffects();
            assertEquals(index + 1, get(game, "classicHeisenRegulatorIndex"));
            assertTrue((boolean) get(regulator, "broken"));
        }
        assertTrue(game.matchEnded);
        assertSame(player, game.harnessWinner);

        BirdGame3 timeout = prepared(6, 0x485003L, 0x485004L);
        timeout.headlessHarnessMode = true;
        timeout.finishClassicHeisenCalibrationFromTimeout();
        assertTrue(timeout.matchEnded);
        assertSame(timeout.players[0], timeout.harnessWinner);
    }

    @Test
    void blueSkyEngineFliesUsesStaminaAndOpensPunishWindows() throws Exception {
        BirdGame3 game = prepared(7, 0x486001L, 0x486002L);
        Bird boss = firstEnemy(game);
        assertNotNull(boss);
        assertTrue(game.isClassicStaminaBoss(boss));
        assertFalse(game.isAI[boss.playerIndex]);
        assertEquals(BirdGame3.BLUE_SKY_ENGINE_BASE_HEALTH * 0.72, boss.health, 0.001);
        assertEquals(BirdGame3.BLUE_SKY_ENGINE_CLOSED_DAMAGE_SCALE,
                game.classicStaminaBossIncomingDamageScale(boss), 0.0001);

        double startX = boss.bodyCenterX();
        double minX = startX;
        double maxX = startX;
        double largestIncomingScale = 0.0;
        for (int tick = 0; tick < 220 && game.harnessTick(); tick++) {
            minX = Math.min(minX, boss.bodyCenterX());
            maxX = Math.max(maxX, boss.bodyCenterX());
            largestIncomingScale = Math.max(largestIncomingScale,
                    game.classicStaminaBossIncomingDamageScale(boss));
        }
        assertTrue(maxX - minX > 180.0, "The Engine must leave a stationary spam position.");
        assertEquals(BirdGame3.BLUE_SKY_ENGINE_OPEN_DAMAGE_SCALE,
                largestIncomingScale, 0.0001);
        assertEquals("BLUE SKY ENGINE", invoke(game, "matchSummaryBirdLabel",
                new Class<?>[]{Bird.class}, boss));
    }

    @Test
    void refineryHazardsWarnBeforeLiveAndRouteUnlocksItsWholeCrownEnding() throws Exception {
        assertEquals(BirdGame3.StormglassArcState.DORMANT,
                BirdGame3.stormglassArcState(0, 0));
        assertEquals(BirdGame3.StormglassArcState.WARNING,
                BirdGame3.stormglassArcState(BirdGame3.STORMGLASS_ARC_START_DELAY_FRAMES, 0));
        assertEquals(BirdGame3.StormglassArcState.LIVE,
                BirdGame3.stormglassArcState(BirdGame3.STORMGLASS_ARC_START_DELAY_FRAMES
                        + BirdGame3.STORMGLASS_ARC_WARNING_FRAMES, 0));

        BirdGame3 game = new BirdGame3();
        assertFalse((boolean) invoke(game, "isMapUnlocked",
                new Class<?>[]{MapType.class}, MapType.STORMGLASS_REFINERY));
        game.setClassicCompleted(BirdType.HEISENBIRD);
        assertTrue((boolean) invoke(game, "isMapUnlocked",
                new Class<?>[]{MapType.class}, MapType.STORMGLASS_REFINERY));
        assertTrue((boolean) invoke(game, "isMapVariantUnlocked",
                new Class<?>[]{BirdGame3.MapVariant.class}, BirdGame3.MapVariant.EYE_OF_THE_SUPERCELL));

        ClassicEndingContent.Ending ending = ClassicEndingContent.endingFor(BirdType.HEISENBIRD);
        assertEquals(ClassicEndingContent.Alignment.DOMINATING, ending.alignment());
        assertTrue(ending.crownChoice().contains("intact Crown"));
        assertFalse(ending.crownChoice().toLowerCase().contains("split"));
        assertTrue(ClassicEndingContent.isHeisenBlueVault(ending.cinematic()));
        assertTrue(ending.cinematic().beats().stream()
                .anyMatch(beat -> beat.narration().contains("single Blue Vault")));
    }

    private static BirdGame3 prepared(int round, long routeSeed, long matchSeed) {
        BirdGame3 game = new BirdGame3();
        game.harnessPrepareClassicEncounter(BirdType.HEISENBIRD, round, 5.0, 6, routeSeed, matchSeed);
        return game;
    }

    private static Bird firstEnemy(BirdGame3 game) {
        for (int slot = 1; slot < game.activePlayers; slot++) {
            Bird bird = game.players[slot];
            if (bird != null && game.getEffectiveTeam(slot) == 2 && game.scores[slot] > 0) return bird;
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private static List<ClassicEncounter> route(BirdGame3 game) throws Exception {
        return (List<ClassicEncounter>) invoke(game, "buildHeisenbirdClassicRun", new Class<?>[0]);
    }

    private static Object invoke(Object target, String name, Class<?>[] types, Object... args) throws Exception {
        Method method = target.getClass().getDeclaredMethod(name, types);
        method.setAccessible(true);
        return method.invoke(target, args);
    }

    private static Object get(Object target, String name) throws Exception {
        Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        return field.get(target);
    }
}
