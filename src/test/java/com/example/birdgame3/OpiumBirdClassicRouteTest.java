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

class OpiumBirdClassicRouteTest {
    @Test
    void manufacturedFutureUsesCompetentButFairOpposition() throws Exception {
        ClassicEncounter manufactured = route(new BirdGame3()).get(1);

        assertEquals(4, manufactured.cpuLevel);
        assertEquals(2, manufactured.enemies.length);
        assertEquals(74.0, manufactured.enemies[0].health(), 0.0001);
        assertEquals(70.0, manufactured.enemies[1].health(), 0.0001);
    }
    @Test
    void twelfthFutureIsAnEightEncounterSelfContainedRoute() throws Exception {
        List<ClassicEncounter> route = route(new BirdGame3());

        assertEquals(8, route.size());
        assertEquals(List.of("One Step Ahead", "Manufactured Future", "Sleep Comes in Waves",
                        "The Future Burns", "The Future Obeys", "Eleven Dead Ends",
                        "Bonus: Wake Before the Bell", "The Still Future"),
                route.stream().map(encounter -> encounter.name).toList());
        assertEquals(List.of(MapType.GLASSWIND_CAUSEWAY, MapType.MIDNIGHT_WORKSHOP,
                        MapType.ONEIRIC_OBSERVATORY, MapType.ASHFALL_CATHEDRAL, MapType.CITY,
                        MapType.WORLDSEAM, MapType.ONEIRIC_OBSERVATORY, MapType.DESERT),
                route.stream().map(encounter -> encounter.map).toList());
        assertEquals(ClassicEncounterStyle.OPIUM_FORECAST_GAUNTLET, route.get(2).style);
        assertEquals(ClassicEncounterStyle.OPIUM_FORECAST_GAUNTLET, route.get(5).style);
        assertEquals(5, route.get(0).cpuLevel,
                "The opening speed team must demand real spacing from the completed normal kit.");
        assertEquals(5, route.get(5).cpuLevel,
                "The dead-end mirrors should test adaptation without a reaction-speed advantage.");
        assertEquals(ClassicEncounterStyle.OPIUM_LUCID_DASH, route.get(6).style);
        assertEquals(ClassicEncounterStyle.OPIUM_STILL_KING_BOSS, route.get(7).style);
        assertTrue(route.getLast().bossFight);
        assertEquals("THE TWELFTH FUTURE", invoke(new BirdGame3(), "classicRouteTitle",
                new Class<?>[]{BirdType.class}, BirdType.OPIUMBIRD));
    }

    @Test
    void everyEnemyIsUltlessAndForecastGauntletsAdvanceAsSeparateWaves() throws Exception {
        assertEquals(2, prepared(0, 0x0F0101L, 0x0F0102L).scores[0]);
        assertEquals(2, prepared(1, 0x0F0103L, 0x0F0104L).scores[0]);
        assertEquals(2, prepared(2, 0x0F0105L, 0x0F0106L).scores[0]);
        assertEquals(2, prepared(3, 0x0F0107L, 0x0F0108L).scores[0]);
        assertEquals(1, prepared(5, 0x0F0109L, 0x0F0110L).scores[0]);
        assertEquals(3, prepared(7, 0x0F0111L, 0x0F0112L).scores[0]);

        for (int round = 0; round < 8; round++) {
            BirdGame3 game = prepared(round, 0x0F1000L + round, 0x0F2000L + round);
            for (int slot = 1; slot < game.activePlayers; slot++) {
                Bird enemy = game.players[slot];
                if (enemy != null && game.getEffectiveTeam(slot) == 2) assertFalse(enemy.hasUltimate());
            }
        }

        for (int round : List.of(2, 5)) {
            BirdGame3 game = prepared(round, 0x0F3000L + round, 0x0F4000L + round);
            for (int wave = 0; wave < 3; wave++) {
                Bird enemy = firstEnemy(game);
                assertNotNull(enemy);
                assertFalse(enemy.hasUltimate());
                if (round == 5) set(game.players[0], "smashDamage", 150.0);
                enemy.health = 0.0;
                game.scores[enemy.playerIndex] = 0;
                assertEquals(wave < 2, game.holdClassicOpiumBirdEncounterOpen());
                if (round == 5 && wave < 2) {
                    assertEquals(0.0, (double) get(game.players[0], "smashDamage"), 0.001);
                    assertEquals(2, game.scores[0],
                            "Clearing the first dead end should award one foresight reserve");
                }
            }
        }
    }

    @Test
    void deadEndWavesUseTheRouteSpecificReadableScale() throws Exception {
        ClassicEncounter deadEnds = route(new BirdGame3()).get(5);
        assertEquals(96.0, deadEnds.waves[0][0].health(), 0.0001);
        assertEquals(102.0, deadEnds.waves[1][0].health(), 0.0001);
        assertEquals(108.0, deadEnds.waves[2][0].health(), 0.0001);
        assertEquals(0.76, BirdGame3.OPIUM_DEAD_END_POWER_SCALE, 0.0001);

        BirdGame3 game = prepared(5, 0x0F0113L, 0x0F0114L);
        Bird first = firstEnemy(game);
        assertNotNull(first);
        assertTrue(first.sizeMultiplier < 0.86,
                "The first dead end must be smaller than the ordinary forecast-gauntlet scale.");

        first.health = 0.0;
        game.scores[first.playerIndex] = 0;
        assertTrue(game.holdClassicOpiumBirdEncounterOpen());
        Bird second = firstEnemy(game);
        assertNotNull(second);
        assertTrue(second.sizeMultiplier <= BirdGame3.OPIUM_DEAD_END_SIZE_SCALE,
                "Subsequent dead ends must preserve the same readable route-specific ceiling.");
    }

    @Test
    void wakingChamberHasTwelveReachableFragmentsAGuideAndSafeTimeout() throws Exception {
        BirdGame3 game = prepared(6, 0x0F5001L, 0x0F5002L);
        game.headlessHarnessMode = true;
        Bird player = game.players[0];
        @SuppressWarnings("unchecked")
        List<Object> fragments = (List<Object>) get(game, "classicOpiumLucidFragments");
        assertEquals(12, fragments.size());
        double floorX = (double) get(game, "battlefieldIslandX");
        double floorW = (double) get(game, "battlefieldIslandW");
        for (Object fragment : fragments) {
            double x = (double) get(fragment, "x");
            double y = (double) get(fragment, "y");
            assertTrue(x >= floorX && x <= floorX + floorW);
            assertTrue(y > 120.0 && y < BirdGame3.GROUND_Y);
            player.x = x - player.bodyWidth() * 0.5;
            player.y = y - player.bodyHeight() * 0.5;
            game.applyOpiumBirdClassicRuntimeEffects();
            assertTrue((boolean) get(fragment, "broken"));
        }
        player.x = BirdGame3.OPIUM_WAKING_BELL_X;
        player.y = (double) get(game, "battlefieldIslandY") - player.bodyHeight();
        game.applyOpiumBirdClassicRuntimeEffects();
        assertTrue(game.matchEnded);
        assertSame(player, game.harnessWinner);

        BirdGame3 timeout = prepared(6, 0x0F5003L, 0x0F5004L);
        timeout.headlessHarnessMode = true;
        timeout.finishClassicOpiumLucidDashFromTimeout();
        assertTrue(timeout.matchEnded);
        assertSame(timeout.players[0], timeout.harnessWinner);
    }

    @Test
    void stillKingUsesThreeBreakableCertaintiesAndARealStaminaBar() throws Exception {
        BirdGame3 game = prepared(7, 0x0F6001L, 0x0F6002L);
        Bird player = game.players[0];
        Bird boss = firstEnemy(game);
        assertNotNull(boss);
        assertTrue(game.isClassicStaminaBoss(boss));
        assertFalse(game.isAI[boss.playerIndex]);
        assertFalse(boss.hasUltimate());
        assertEquals(3, game.scores[0]);
        assertEquals(BirdGame3.OPIUM_STILL_KING_BASE_HEALTH, boss.health, 0.001);
        assertEquals(0.10, game.classicStaminaBossIncomingDamageScale(boss), 0.0001);

        @SuppressWarnings("unchecked")
        List<Object> seals = (List<Object>) get(game, "classicOpiumCertaintySeals");
        assertEquals(3, seals.size());
        for (Object seal : seals) {
            player.x = (double) get(seal, "x") - player.bodyWidth() * 0.5;
            player.y = (double) get(seal, "y") - player.bodyHeight() * 0.5;
            setAttack(game, true);
            game.applyOpiumBirdClassicRuntimeEffects();
            setAttack(game, false);
            for (int tick = 0; tick < 12; tick++) game.applyOpiumBirdClassicRuntimeEffects();
            assertTrue((boolean) get(seal, "broken"));
        }
        assertEquals(1.18, game.classicStaminaBossIncomingDamageScale(boss), 0.0001);

        double startX = boss.bodyCenterX();
        double minX = startX;
        double maxX = startX;
        for (int tick = 0; tick < 220 && game.harnessTick(); tick++) {
            minX = Math.min(minX, boss.bodyCenterX());
            maxX = Math.max(maxX, boss.bodyCenterX());
        }
        assertTrue(maxX - minX > 220.0, "The Still King must leave a stationary spam position.");
        assertEquals("STILL KING", invoke(game, "matchSummaryBirdLabel",
                new Class<?>[]{Bird.class}, boss));
    }

    @Test
    void oneiricStageBadgeAndAnimatedEndingShipTogether() throws Exception {
        BirdGame3 game = new BirdGame3();
        assertFalse((boolean) invoke(game, "isMapUnlocked",
                new Class<?>[]{MapType.class}, MapType.ONEIRIC_OBSERVATORY));
        game.setClassicCompleted(BirdType.OPIUMBIRD);
        assertTrue((boolean) invoke(game, "isMapUnlocked",
                new Class<?>[]{MapType.class}, MapType.ONEIRIC_OBSERVATORY));
        assertTrue((boolean) invoke(game, "isMapVariantUnlocked",
                new Class<?>[]{BirdGame3.MapVariant.class}, BirdGame3.MapVariant.WAKING_CHAMBER));

        ClassicEndingContent.Cinematic ending = ClassicEndingContent.endingFor(BirdType.OPIUMBIRD).cinematic();
        assertTrue(ClassicEndingContent.isOpiumTwelfthFuture(ending));
        assertEquals("THE DOOR LEFT OPEN", ending.title());
        assertEquals(MapType.ONEIRIC_OBSERVATORY, ending.location());
        assertEquals(6, ending.beats().size());
        assertTrue(ending.beats().getLast().narration().contains("allowed to surprise us"));

        assertTrue(StagePreviewRenderer.capturedPreviewResourceExists(
                BirdGame3.StageChoice.main(MapType.ONEIRIC_OBSERVATORY)));
        assertTrue(StagePreviewRenderer.capturedPreviewResourceExists(
                new BirdGame3.StageChoice(MapType.ONEIRIC_OBSERVATORY,
                        BirdGame3.MapVariant.WAKING_CHAMBER)));
    }

    private static BirdGame3 prepared(int round, long routeSeed, long matchSeed) {
        BirdGame3 game = new BirdGame3();
        game.harnessPrepareClassicEncounter(BirdType.OPIUMBIRD, round, 5.0, 6, routeSeed, matchSeed);
        return game;
    }

    private static Bird firstEnemy(BirdGame3 game) {
        for (int slot = 1; slot < game.activePlayers; slot++) {
            Bird bird = game.players[slot];
            if (bird != null && game.getEffectiveTeam(slot) == 2 && game.scores[slot] > 0) return bird;
        }
        return null;
    }

    private static void setAttack(BirdGame3 game, boolean down) throws Exception {
        boolean[][] input = (boolean[][]) get(game, "localActionPressed");
        input[0][3] = down;
    }

    @SuppressWarnings("unchecked")
    private static List<ClassicEncounter> route(BirdGame3 game) throws Exception {
        return (List<ClassicEncounter>) invoke(game, "buildOpiumBirdClassicRun", new Class<?>[0]);
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

    private static void set(Object target, String name, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        field.set(target, value);
    }
}
