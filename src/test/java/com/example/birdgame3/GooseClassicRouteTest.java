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

class GooseClassicRouteTest {
    @Test
    void skyHasNoBorderUsesEightAuthoredEncountersAndExistingStages() throws Exception {
        BirdGame3 game = new BirdGame3();
        List<ClassicEncounter> route = route(game);

        assertEquals(8, route.size());
        assertEquals(List.of("Unauthorized Landing", "Winter Formation", "Restricted Airspace",
                        "Open the Crossing", "The Whole Flock", "Bonus: Migration Beacons",
                        "The Flock of One", "No Sky Closed"),
                route.stream().map(encounter -> encounter.name).toList());
        assertEquals(List.of(MapType.CITY, MapType.FROSTBITE_FJORD, MapType.SKYCLIFFS,
                        MapType.PRISON, MapType.DESERT, MapType.SKYCLIFFS,
                        MapType.DOCK, MapType.BEACON_CROWN),
                route.stream().map(encounter -> encounter.map).toList());
        assertEquals(MapVariant.LAST_ICE_SHELF, route.get(1).variant);
        assertEquals(MapVariant.REDLINE_CANYON, route.get(4).variant);
        assertEquals(MapVariant.DAWNWATCH_BASTION, route.getLast().variant);
        assertEquals("THE SKY HAS NO BORDER", invoke(game, "classicRouteTitle",
                new Class<?>[]{BirdType.class}, BirdType.GOOSE));
        assertTrue(route.stream().noneMatch(encounter -> encounter.name.contains("TEMPORARY")));
    }

    @Test
    void routeBuildsItsIdentityFromRealFormationBattlesNotTemporaryPowers() throws Exception {
        List<ClassicEncounter> route = route(new BirdGame3());

        assertEquals(1, route.get(1).allies.length);
        assertEquals(2, route.get(1).enemies.length);
        assertEquals(ClassicEncounterStyle.GIANT, route.get(2).style);
        assertEquals(1, route.get(3).allies.length);
        assertEquals(2, route.get(3).enemies.length);
        assertEquals(2, route.get(4).allies.length);
        assertEquals(1, route.get(4).enemies.length);
        assertEquals(ClassicEncounterStyle.BONUS_RELAY, route.get(5).style);
        assertEquals(BirdType.GOOSE, route.get(6).enemies[0].type());
        assertEquals(BirdType.EAGLE, route.get(7).enemies[0].type());
    }

    @Test
    void formationRoundsUseCorrectTeamsAndSafeSeparatedSpawns() {
        for (int round : List.of(1, 3, 4)) {
            BirdGame3 game = prepared(round, 0xF17000L + round, 0xF17100L + round);
            int allies = 0;
            int enemies = 0;
            for (int slot = 1; slot < game.activePlayers; slot++) {
                Bird bird = game.players[slot];
                assertNotNull(bird);
                assertTrue(bird.bodyBottomY() <= BirdGame3.GROUND_Y + 0.001);
                if (game.getEffectiveTeam(slot) == 1) allies++;
                if (game.getEffectiveTeam(slot) == 2) enemies++;
                assertTrue(Math.abs(bird.bodyCenterX() - game.players[0].bodyCenterX()) > 80.0,
                        "Formation fighters must not spawn piled onto Goose");
            }
            assertEquals(game.classicEncounter.allies.length, allies);
            assertEquals(game.classicEncounter.enemies.length, enemies);
        }
    }

    @Test
    void migrationBeaconsAreUltlessReachableAndCompleteSafely() {
        BirdGame3 game = prepared(5, 0xF17200L, 0xF17201L);
        for (int slot = 1; slot < game.activePlayers; slot++) {
            assertFalse(game.players[slot].hasUltimate());
            assertTrue(game.players[slot].bodyCenterY() > 100.0);
            assertTrue(game.players[slot].bodyCenterY() < BirdGame3.GROUND_Y);
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
    void borderKingIsAThreeStockActualEagleBoss() {
        BirdGame3 game = prepared(7, 0xF17300L, 0xF17301L);
        Bird boss = game.players[1];

        assertEquals(BirdType.EAGLE, boss.type);
        assertEquals(3, game.scores[0]);
        assertEquals(3, game.scores[1]);
        assertEquals(1.08, boss.baseSizeMultiplier, 0.001);
        assertEquals(175.0, boss.health, 0.001);
        assertFalse(boss.hasUltimate());
        assertTrue(game.isAI[1]);
        double startX = boss.x;
        for (int tick = 0; tick < 180 && game.harnessTick(); tick++) {
            // The actual Eagle CPU retains its standard egg and charge kit.
        }
        assertNotEquals(startX, boss.x, 1.0);
    }

    @Test
    void endingKeepsTheCrownIntactAsAnOwnerlessFlywayCompass() {
        ClassicEndingContent.Ending ending = ClassicEndingContent.endingFor(BirdType.GOOSE);

        assertNotNull(ending);
        assertEquals("THE SKY HAS NO BORDER", ending.routeTitle());
        assertEquals(ClassicEndingContent.Alignment.HOPEFUL, ending.alignment());
        assertEquals(BirdType.EAGLE, ending.defeatedBoss());
        assertTrue(ending.crownChoice().contains("intact Crown"));
        assertFalse(ending.crownChoice().toLowerCase().contains("split"));
        assertTrue(ClassicEndingContent.isGooseOpenFlyway(ending.cinematic()));
        assertEquals(6, ending.cinematic().beats().size());
        assertTrue(ending.cinematic().beats().getLast().narration().endsWith("the crossing."));
    }

    private static BirdGame3 prepared(int round, long routeSeed, long matchSeed) {
        BirdGame3 game = new BirdGame3();
        game.harnessPrepareClassicEncounter(BirdType.GOOSE, round, 5.0, 6, routeSeed, matchSeed);
        return game;
    }

    @SuppressWarnings("unchecked")
    private static List<ClassicEncounter> route(BirdGame3 game) throws Exception {
        return (List<ClassicEncounter>) invoke(game, "buildGooseClassicRun", new Class<?>[0]);
    }

    private static Object invoke(Object target, String name, Class<?>[] types, Object... args) throws Exception {
        Method method = target.getClass().getDeclaredMethod(name, types);
        method.setAccessible(true);
        return method.invoke(target, args);
    }
}
