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

class RavenClassicRouteTest {
    @Test
    void futureHasOneAuthorUsesEightAuthoredEncountersAndNoNewStage() throws Exception {
        BirdGame3 game = new BirdGame3();
        List<ClassicEncounter> route = route(game);

        assertEquals(8, route.size());
        assertEquals(List.of("The First Omen", "A Murder of Two", "Three Bad Endings",
                        "The Oracle Blinks", "Bonus: Break the Futures", "History Is Written",
                        "The Unwritten Raven", "No Dawn Promised"),
                route.stream().map(encounter -> encounter.name).toList());
        assertEquals(List.of(MapType.FOREST, MapType.CARRION_EXCHANGE, MapType.CITY,
                        MapType.ONEIRIC_OBSERVATORY, MapType.SKYCLIFFS, MapType.PRISON,
                        MapType.CAVE, MapType.ASHFALL_CATHEDRAL),
                route.stream().map(encounter -> encounter.map).toList());
        assertEquals(MapVariant.PARLIAMENT_ROOFTOPS, route.get(2).variant);
        assertEquals(MapVariant.SKYBREAK_SPIRES, route.get(4).variant);
        assertEquals(MapVariant.ASHFALL_REBIRTH, route.getLast().variant);
        assertEquals("THE FUTURE HAS ONE AUTHOR", invoke(game, "classicRouteTitle",
                new Class<?>[]{BirdType.class}, BirdType.RAVEN));
        assertTrue(route.stream().noneMatch(encounter -> encounter.name.contains("TEMPORARY")));
    }

    @Test
    void routeVariesDuelAllyLaunchPuzzleGiantObjectiveMirrorAndBirdBoss() throws Exception {
        List<ClassicEncounter> route = route(new BirdGame3());

        assertEquals(1, route.get(0).enemies.length);
        assertEquals(1, route.get(1).allies.length);
        assertEquals(2, route.get(1).enemies.length);
        assertEquals(ClassicEncounterStyle.RAVEN_FORETOLD_FATES, route.get(2).style);
        assertEquals(3, route.get(2).enemies.length);
        assertEquals(ClassicEncounterStyle.GIANT, route.get(3).style);
        assertEquals(ClassicEncounterStyle.BONUS_RELAY, route.get(4).style);
        assertEquals(2, route.get(5).enemies.length);
        assertEquals(BirdType.RAVEN, route.get(6).enemies[0].type());
        assertEquals(BirdType.PHOENIX, route.get(7).enemies[0].type());
    }

    @Test
    void foretoldFatesStartLaunchableAndCannotLayerUltimates() {
        BirdGame3 game = prepared(2, 0xDA7100L, 0xDA7101L);

        assertEquals(1, game.scores[0]);
        for (int slot = 1; slot < game.activePlayers; slot++) {
            Bird enemy = game.players[slot];
            assertNotNull(enemy);
            assertTrue(enemy.smashDamagePercent() >= 62.0,
                    "Each foretold target must begin near launch percent");
            assertFalse(enemy.hasUltimate());
            assertTrue(enemy.bodyBottomY() <= BirdGame3.GROUND_Y + 0.001);
        }
    }

    @Test
    void falseFutureTargetsAreUltlessReachableAndCompleteSafely() {
        BirdGame3 game = prepared(4, 0xDA7200L, 0xDA7201L);

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
    void lastDawnIsAThreeStockActualPhoenixBossWithReadableRealKit() {
        BirdGame3 game = prepared(7, 0xDA7300L, 0xDA7301L);
        Bird boss = game.players[1];

        assertEquals(BirdType.PHOENIX, boss.type);
        assertEquals(3, game.scores[0]);
        assertEquals(3, game.scores[1]);
        assertEquals(1.46, boss.baseSizeMultiplier, 0.001);
        assertEquals(210.0, boss.health, 0.001);
        assertFalse(boss.hasUltimate());
        assertTrue(game.isAI[1]);
        double startX = boss.x;
        for (int tick = 0; tick < 180 && game.harnessTick(); tick++) {
            // The actual Phoenix CPU controls its standard movement and attacks.
        }
        assertNotEquals(startX, boss.x, 1.0);
    }

    @Test
    void endingKeepsTheCrownWholeAndMakesRavenTheOnlyAuthor() {
        ClassicEndingContent.Ending ending = ClassicEndingContent.endingFor(BirdType.RAVEN);

        assertNotNull(ending);
        assertEquals("THE FUTURE HAS ONE AUTHOR", ending.routeTitle());
        assertEquals(ClassicEndingContent.Alignment.DOMINATING, ending.alignment());
        assertEquals(BirdType.PHOENIX, ending.defeatedBoss());
        assertTrue(ending.crownChoice().contains("intact Crown"));
        assertFalse(ending.crownChoice().toLowerCase().contains("split"));
        assertTrue(ClassicEndingContent.isRavenBlackSun(ending.cinematic()));
        assertEquals(6, ending.cinematic().beats().size());
        assertEquals("Tomorrow is safe. Tomorrow is mine.",
                ending.cinematic().beats().getLast().narration());
    }

    private static BirdGame3 prepared(int round, long routeSeed, long matchSeed) {
        BirdGame3 game = new BirdGame3();
        game.harnessPrepareClassicEncounter(BirdType.RAVEN, round, 5.0, 6, routeSeed, matchSeed);
        return game;
    }

    @SuppressWarnings("unchecked")
    private static List<ClassicEncounter> route(BirdGame3 game) throws Exception {
        return (List<ClassicEncounter>) invoke(game, "buildRavenClassicRun", new Class<?>[0]);
    }

    private static Object invoke(Object target, String name, Class<?>[] types, Object... args) throws Exception {
        Method method = target.getClass().getDeclaredMethod(name, types);
        method.setAccessible(true);
        return method.invoke(target, args);
    }
}
