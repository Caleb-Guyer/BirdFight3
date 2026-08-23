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

class KiwiClassicRouteTest {
    @Test
    void groundRemembersUsesEightAuthoredEncountersAndOnlyExistingStages() throws Exception {
        BirdGame3 game = new BirdGame3();
        List<ClassicEncounter> route = route(game);

        assertEquals(8, route.size());
        assertEquals(List.of("The Smallest Footprint", "Roots Know the Way", "The Weight Above",
                        "No Wings Required", "The Foundation Holds", "Bonus: Buried Markers",
                        "The Last Burrow", "The Ground Beneath the Crown"),
                route.stream().map(encounter -> encounter.name).toList());
        assertEquals(List.of(MapType.VIBRANT_JUNGLE, MapType.FOREST, MapType.FOREST,
                        MapType.DESERT, MapType.DOCK, MapType.FOREST,
                        MapType.CAVE, MapType.BEACON_CROWN),
                route.stream().map(encounter -> encounter.map).toList());
        assertEquals(MapVariant.HEARTBLOOM_SANCTUARY, route.getFirst().variant);
        assertEquals(MapVariant.STILLWATER_MARSH, route.get(1).variant);
        assertEquals(MapVariant.HARVEST_TRIBUNAL, route.get(2).variant);
        assertEquals(MapVariant.REDLINE_CANYON, route.get(3).variant);
        assertEquals(MapVariant.TITAN_DOCK, route.get(4).variant);
        assertEquals(MapVariant.DAWNWATCH_BASTION, route.getLast().variant);
        assertEquals("THE GROUND REMEMBERS", invoke(game, "classicRouteTitle",
                new Class<?>[]{BirdType.class}, BirdType.KIWI));
        assertTrue(route.stream().noneMatch(encounter -> encounter.name.contains("TEMPORARY")));
    }

    @Test
    void routeVarietyNeverInventsAClassicOnlyKiwiAbility() throws Exception {
        List<ClassicEncounter> route = route(new BirdGame3());

        assertEquals(ClassicEncounterStyle.MINIATURE_FLOCK, route.get(0).style);
        assertEquals(1, route.get(1).allies.length);
        assertEquals(2, route.get(1).enemies.length);
        assertEquals(ClassicEncounterStyle.GIANT, route.get(2).style);
        assertEquals(2, route.get(3).enemies.length);
        assertEquals(1, route.get(4).allies.length);
        assertEquals(2, route.get(4).enemies.length);
        assertEquals(ClassicEncounterStyle.BONUS_RELAY, route.get(5).style);
        assertEquals(BirdType.KIWI, route.get(6).enemies[0].type());
        assertTrue(route.stream().map(encounter -> encounter.briefing)
                .noneMatch(text -> text.contains("route power") || text.contains("borrowed ability")));
        assertTrue(route.get(1).allies[0].powerMult() <= 0.70,
                "Stillwater's ally must not trivialize the team round");
        assertTrue(route.get(1).enemies[0].powerMult() >= 0.66);
        assertTrue(route.get(2).enemies[0].health() <= 92.0,
                "The giant claim must remain a survivable mid-route skill check");
        assertTrue(route.get(2).enemies[0].powerMult() <= 0.48);
    }

    @Test
    void teamRoundsUseCorrectTeamsAndSafeSeparatedSpawns() {
        for (int round : List.of(1, 4)) {
            BirdGame3 game = prepared(round, 0xF18000L + round, 0xF18100L + round);
            int allies = 0;
            int enemies = 0;
            for (int slot = 1; slot < game.activePlayers; slot++) {
                Bird bird = game.players[slot];
                assertNotNull(bird);
                assertTrue(bird.bodyBottomY() <= BirdGame3.GROUND_Y + 0.001);
                if (game.getEffectiveTeam(slot) == 1) allies++;
                if (game.getEffectiveTeam(slot) == 2) enemies++;
                assertTrue(Math.abs(bird.bodyCenterX() - game.players[0].bodyCenterX()) > 80.0,
                        "Team fighters must not spawn piled onto Kiwi");
            }
            assertEquals(game.classicEncounter.allies.length, allies);
            assertEquals(game.classicEncounter.enemies.length, enemies);
        }
    }

    @Test
    void titanDockBoardersKeepLateRouteFormationPressure() {
        BirdGame3 game = prepared(4, 0xF18180L, 0xF18181L);

        for (int slot = 2; slot < game.activePlayers; slot++) {
            Bird boarder = game.players[slot];
            assertNotNull(boarder);
            assertEquals(1.02, boarder.baseSizeMultiplier, 0.001);
            assertTrue(boarder.basePowerMultiplier >= 0.65);
        }
    }

    @Test
    void buriedMarkersStayOnTheGroundAndCompleteSafely() {
        BirdGame3 game = prepared(5, 0xF18200L, 0xF18201L);
        assertEquals(BirdGame3.GROUND_Y, game.players[0].bodyBottomY(), 0.001);
        double previousX = game.players[0].bodyCenterX();
        for (int slot = 1; slot < game.activePlayers; slot++) {
            Bird target = game.players[slot];
            assertFalse(target.hasUltimate());
            assertEquals(BirdGame3.GROUND_Y, target.bodyBottomY(), 0.001);
            assertTrue(target.bodyCenterX() - previousX >= 1_000.0,
                    "Buried markers must form a readable ground course");
            previousX = target.bodyCenterX();
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
    void zenithIsAThreeStockActualEagleBoss() {
        BirdGame3 game = prepared(7, 0xF18300L, 0xF18301L);
        Bird boss = game.players[1];

        assertEquals(BirdType.EAGLE, boss.type);
        assertEquals(3, game.scores[0]);
        assertEquals(3, game.scores[1]);
        assertEquals(1.16, boss.baseSizeMultiplier, 0.001);
        assertEquals(0.75, boss.basePowerMultiplier, 0.001);
        assertEquals(170.0, boss.health, 0.001);
        assertFalse(boss.hasUltimate());
        assertTrue(game.isAI[1]);
        double startX = boss.x;
        for (int tick = 0; tick < 180 && game.harnessTick(); tick++) {
            // The Zenith is an actual Eagle CPU using its normal combat kit.
        }
        assertNotEquals(startX, boss.x, 1.0);
    }

    @Test
    void endingBuriesTheIntactCrownInsteadOfSplittingIt() {
        ClassicEndingContent.Ending ending = ClassicEndingContent.endingFor(BirdType.KIWI);

        assertNotNull(ending);
        assertEquals("THE GROUND REMEMBERS", ending.routeTitle());
        assertEquals(ClassicEndingContent.Alignment.HOPEFUL, ending.alignment());
        assertEquals(BirdType.EAGLE, ending.defeatedBoss());
        assertTrue(ending.crownChoice().contains("intact Crown"));
        assertFalse(ending.crownChoice().toLowerCase().contains("split"));
        assertTrue(ClassicEndingContent.isKiwiDeepRoot(ending.cinematic()));
        assertEquals(6, ending.cinematic().beats().size());
        assertTrue(ending.cinematic().beats().getLast().narration().endsWith("does."));
    }

    private static BirdGame3 prepared(int round, long routeSeed, long matchSeed) {
        BirdGame3 game = new BirdGame3();
        game.harnessPrepareClassicEncounter(BirdType.KIWI, round, 5.0, 6, routeSeed, matchSeed);
        return game;
    }

    @SuppressWarnings("unchecked")
    private static List<ClassicEncounter> route(BirdGame3 game) throws Exception {
        return (List<ClassicEncounter>) invoke(game, "buildKiwiClassicRun", new Class<?>[0]);
    }

    private static Object invoke(Object target, String name, Class<?>[] types, Object... args) throws Exception {
        Method method = target.getClass().getDeclaredMethod(name, types);
        method.setAccessible(true);
        return method.invoke(target, args);
    }
}
