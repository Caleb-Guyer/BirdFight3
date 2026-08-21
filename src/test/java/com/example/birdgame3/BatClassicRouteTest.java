package com.example.birdgame3;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.List;

import static com.example.birdgame3.BirdGame3.BirdType;
import static com.example.birdgame3.BirdGame3.ClassicEncounter;
import static com.example.birdgame3.BirdGame3.ClassicEncounterStyle;
import static com.example.birdgame3.BirdGame3.MapType;
import static com.example.birdgame3.BirdGame3.MapVariant;
import static org.junit.jupiter.api.Assertions.*;

class BatClassicRouteTest {
    @Test
    void nightAnswersBackUsesEightDistinctAuthoredEncountersWithoutAddingAStage() throws Exception {
        BirdGame3 game = new BirdGame3();
        List<ClassicEncounter> route = route(game);

        assertEquals(8, route.size());
        assertEquals(List.of("First Echo", "What Hunts at Night", "The Weight of Silence",
                        "Counterpoint", "False Echoes", "Bonus: Moonlit Survey",
                        "The Voice in the Cave", "Thunder Without End"),
                route.stream().map(encounter -> encounter.name).toList());
        assertEquals(List.of(MapType.CAVE, MapType.ONEIRIC_OBSERVATORY,
                        MapType.SILENT_AMPHITHEATER, MapType.RESONANCE_HALL,
                        MapType.MIDNIGHT_WORKSHOP, MapType.SKYCLIFFS,
                        MapType.MIDNIGHT_WORKSHOP, MapType.SKYCLIFFS),
                route.stream().map(encounter -> encounter.map).toList());
        assertEquals(List.of(ClassicEncounterStyle.MINIATURE_FLOCK,
                        ClassicEncounterStyle.STANDARD, ClassicEncounterStyle.GIANT,
                        ClassicEncounterStyle.STANDARD, ClassicEncounterStyle.STANDARD,
                        ClassicEncounterStyle.BONUS_RELAY, ClassicEncounterStyle.STANDARD,
                        ClassicEncounterStyle.STORM_TYRANT_BOSS),
                route.stream().map(encounter -> encounter.style).toList());
        assertEquals("THE NIGHT ANSWERS BACK", invoke(game, "classicRouteTitle",
                new Class<?>[]{BirdType.class}, BirdType.BAT));
        assertTrue(route.stream().noneMatch(encounter -> encounter.name.contains("TEMPORARY")));
    }

    @Test
    void scoutsAndMoonlitTargetsCannotUseUltimates() {
        for (int round : List.of(0, 5)) {
            BirdGame3 game = prepared(round, 0xBA7000L + round, 0xBA7100L + round);
            assertTrue(game.activePlayers >= 4);
            for (int slot = 1; slot < game.activePlayers; slot++) {
                Bird enemy = game.players[slot];
                assertNotNull(enemy);
                assertFalse(enemy.hasUltimate(), "Route minions and objective markers must remain ultless.");
            }
        }
    }

    @Test
    void moonlitSurveyUsesReachableAuthoredPositionsAndCompletesSafely() {
        BirdGame3 game = prepared(5, 0xBA7200L, 0xBA7201L);

        assertEquals(MapType.SKYCLIFFS, game.classicEncounter.map);
        assertEquals(MapVariant.PEREGRINE_RUN, game.classicEncounter.variant);
        assertEquals(ClassicEncounterStyle.BONUS_RELAY, game.classicEncounter.style);
        for (int slot = 1; slot < game.activePlayers; slot++) {
            Bird target = game.players[slot];
            assertTrue(target.bodyCenterX() > 400.0 && target.bodyCenterX() < BirdGame3.WORLD_WIDTH - 400.0);
            assertTrue(target.bodyCenterY() > 120.0 && target.bodyCenterY() < BirdGame3.GROUND_Y);
            assertFalse(game.isAI[slot], "Bonus targets must not enter navigation loops.");
        }

        game.headlessHarnessMode = true;
        for (int slot = 1; slot < game.activePlayers; slot++) {
            game.players[slot].health = 0.0;
            game.scores[slot] = 0;
        }
        for (int tick = 0; tick < 8 && !game.matchEnded; tick++) game.harnessTick();
        assertTrue(game.matchEnded);
        assertSame(game.players[0], game.harnessWinner);
    }

    @Test
    void stormTyrantIsARealBirdBossWithTwoStocksAndActiveMovement() {
        BirdGame3 game = prepared(7, 0xBA7300L, 0xBA7301L);
        Bird boss = game.players[1];

        assertNotNull(boss);
        assertEquals(BirdType.EAGLE, boss.type);
        assertEquals(2, game.scores[1]);
        assertEquals(ClassicEncounterStyle.STORM_TYRANT_BOSS, game.classicEncounter.style);
        assertEquals(8, game.classicEncounter.cpuLevel);
        assertTrue(game.getCpuLevel(1) >= 4);

        double startX = boss.x;
        for (int tick = 0; tick < 180 && game.harnessTick(); tick++) {
            // Existing Storm Tyrant runtime owns boss movement and crosswinds.
        }
        assertNotEquals(startX, boss.x, 1.0,
                "The Storm Tyrant must not remain a stationary attack-spam target.");
    }

    @Test
    void endingKeepsTheCrownWholeAndOnlyListensForDanger() {
        ClassicEndingContent.Ending ending = ClassicEndingContent.endingFor(BirdType.BAT);

        assertNotNull(ending);
        assertEquals("THE NIGHT ANSWERS BACK", ending.routeTitle());
        assertEquals(ClassicEndingContent.Alignment.AMBIGUOUS, ending.alignment());
        assertEquals(BirdType.EAGLE, ending.defeatedBoss());
        assertTrue(ending.crownChoice().contains("intact Crown"));
        assertFalse(ending.crownChoice().toLowerCase().contains("split"));
        assertTrue(ClassicEndingContent.isBatListeningDark(ending.cinematic()));
        assertEquals(6, ending.cinematic().beats().size());
        assertTrue(ending.cinematic().beats().getLast().narration().endsWith("not unwatched."));
    }

    private static BirdGame3 prepared(int round, long routeSeed, long matchSeed) {
        BirdGame3 game = new BirdGame3();
        game.harnessPrepareClassicEncounter(BirdType.BAT, round, 5.0, 6, routeSeed, matchSeed);
        return game;
    }

    @SuppressWarnings("unchecked")
    private static List<ClassicEncounter> route(BirdGame3 game) throws Exception {
        return (List<ClassicEncounter>) invoke(game, "buildBatClassicRun", new Class<?>[0]);
    }

    private static Object invoke(Object target, String name, Class<?>[] types, Object... args) throws Exception {
        Method method = target.getClass().getDeclaredMethod(name, types);
        method.setAccessible(true);
        return method.invoke(target, args);
    }
}
