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

class PelicanClassicRouteTest {
    @Test
    void weightOfTheHarborUsesEightAuthoredLogisticsEncountersAndExistingStages() throws Exception {
        BirdGame3 game = new BirdGame3();
        List<ClassicEncounter> route = route(game);

        assertEquals(8, route.size());
        assertEquals(List.of("All Hands", "Rush Order", "Cold Storage", "Convoy Duty",
                        "The Pirate Hold", "Bonus: Rooftop Airlift", "Bottomless Appetite",
                        "The Empty Hold"),
                route.stream().map(encounter -> encounter.name).toList());
        assertEquals(List.of(MapType.DOCK, MapType.CITY, MapType.FROSTBITE_FJORD,
                        MapType.DESERT, MapType.DOCK, MapType.CITY,
                        MapType.CAVE, MapType.CARRION_EXCHANGE),
                route.stream().map(encounter -> encounter.map).toList());
        assertEquals(MapVariant.TITAN_DOCK, route.get(4).variant);
        assertEquals(MapVariant.ROOFTOP_RELAY, route.get(5).variant);
        assertEquals(ClassicEncounterStyle.HOARDMASTER_BOSS, route.getLast().style);
        assertTrue(route.getLast().bossFight);
        assertEquals("THE WEIGHT OF THE HARBOR", invoke(game, "classicRouteTitle",
                new Class<?>[]{BirdType.class}, BirdType.PELICAN));
        assertTrue(route.stream().noneMatch(encounter -> encounter.name.contains("TEMPORARY")));
    }

    @Test
    void routeUsesRealPelicanGameplayAndVariedBattleStructures() throws Exception {
        List<ClassicEncounter> route = route(new BirdGame3());

        assertEquals(ClassicEncounterStyle.MINIATURE_FLOCK, route.get(0).style);
        assertEquals(2, route.get(1).enemies.length);
        assertEquals(ClassicEncounterStyle.GIANT, route.get(2).style);
        assertEquals(1, route.get(3).allies.length);
        assertEquals(2, route.get(3).enemies.length);
        assertEquals(ClassicEncounterStyle.PELICAN_BOARDING_GAUNTLET, route.get(4).style);
        assertEquals(1, route.get(4).enemies.length);
        assertNotNull(route.get(4).waves);
        assertEquals(3, route.get(4).waves.length);
        assertEquals(ClassicEncounterStyle.BONUS_RELAY, route.get(5).style);
        assertEquals(BirdType.PELICAN, route.get(6).enemies[0].type());
        assertEquals(BirdType.VULTURE, route.get(7).enemies[0].type());
    }

    @Test
    void dockhandsAndAirliftMarkersAreUltlessAndAirliftCompletes() {
        for (int round : List.of(0, 5)) {
            BirdGame3 game = prepared(round, 0xCE1100L + round, 0xCE1200L + round);
            for (int slot = 1; slot < game.activePlayers; slot++) {
                assertNotNull(game.players[slot]);
                assertFalse(game.players[slot].hasUltimate());
            }
        }

        BirdGame3 game = prepared(5, 0xCE1300L, 0xCE1301L);
        assertEquals(MapVariant.ROOFTOP_RELAY, game.classicEncounter.variant);
        for (int slot = 1; slot < game.activePlayers; slot++) {
            Bird marker = game.players[slot];
            assertFalse(game.isAI[slot]);
            assertTrue(marker.bodyCenterX() > 400.0 && marker.bodyCenterX() < BirdGame3.WORLD_WIDTH - 300.0);
            assertTrue(marker.bodyCenterY() > 120.0 && marker.bodyCenterY() < BirdGame3.GROUND_Y);
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
    void hoardmasterIsAThreeStockActualBirdBossUsingReadableRealKit() {
        BirdGame3 game = prepared(7, 0xCE1400L, 0xCE1401L);
        Bird boss = game.players[1];

        assertNotNull(boss);
        assertEquals(BirdType.VULTURE, boss.type);
        assertEquals(3, game.scores[1]);
        assertEquals(3, game.scores[0]);
        assertEquals(1.38, boss.baseSizeMultiplier, 0.001);
        assertEquals(205.0, boss.health, 0.001);
        assertFalse(boss.hasUltimate());
        assertTrue(game.isAI[1]);

        double startX = boss.x;
        for (int tick = 0; tick < 180 && game.harnessTick(); tick++) {
            // Ordinary Vulture AI owns flight, crows, and recovery.
        }
        assertNotEquals(startX, boss.x, 1.0);
    }

    @Test
    void pirateHoldAdvancesThroughThreeUltlessBoardingWaves() {
        BirdGame3 game = prepared(4, 0xCE1350L, 0xCE1351L);
        assertEquals(2, game.scores[0]);
        assertEquals(BirdType.FALCON, game.players[1].type);
        assertFalse(game.players[1].hasUltimate());

        game.players[0].setTrailerSmashDamagePercent(80.0);
        eliminateEnemyTeam(game);
        assertTrue(game.holdClassicPelicanEncounterOpen());
        assertEquals(BirdType.RAVEN, game.players[1].type);
        assertTrue(game.players[0].smashDamagePercent() < 80.0);
        assertFalse(game.players[1].hasUltimate());

        eliminateEnemyTeam(game);
        assertTrue(game.holdClassicPelicanEncounterOpen());
        assertEquals(BirdType.HEISENBIRD, game.players[1].type);
        assertFalse(game.players[1].hasUltimate());

        eliminateEnemyTeam(game);
        assertFalse(game.holdClassicPelicanEncounterOpen());
    }

    @Test
    void endingKeepsTheCrownIntactAndOpensTheHarborWithoutOwningIt() {
        ClassicEndingContent.Ending ending = ClassicEndingContent.endingFor(BirdType.PELICAN);

        assertNotNull(ending);
        assertEquals("THE WEIGHT OF THE HARBOR", ending.routeTitle());
        assertEquals(ClassicEndingContent.Alignment.HOPEFUL, ending.alignment());
        assertEquals(BirdType.VULTURE, ending.defeatedBoss());
        assertTrue(ending.crownChoice().contains("intact Crown"));
        assertFalse(ending.crownChoice().toLowerCase().contains("split"));
        assertTrue(ClassicEndingContent.isPelicanOpenHarbor(ending.cinematic()));
        assertEquals(6, ending.cinematic().beats().size());
        assertTrue(ending.cinematic().beats().getLast().narration().endsWith("harbor stays hungry."));
    }

    private static BirdGame3 prepared(int round, long routeSeed, long matchSeed) {
        BirdGame3 game = new BirdGame3();
        game.harnessPrepareClassicEncounter(BirdType.PELICAN, round, 5.0, 6, routeSeed, matchSeed);
        return game;
    }

    private static void eliminateEnemyTeam(BirdGame3 game) {
        for (int slot = 1; slot < game.activePlayers; slot++) {
            Bird enemy = game.players[slot];
            if (enemy != null && game.getEffectiveTeam(slot) == 2) {
                enemy.health = 0.0;
                game.scores[slot] = 0;
            }
        }
    }

    @SuppressWarnings("unchecked")
    private static List<ClassicEncounter> route(BirdGame3 game) throws Exception {
        return (List<ClassicEncounter>) invoke(game, "buildPelicanClassicRun", new Class<?>[0]);
    }

    private static Object invoke(Object target, String name, Class<?>[] types, Object... args) throws Exception {
        Method method = target.getClass().getDeclaredMethod(name, types);
        method.setAccessible(true);
        return method.invoke(target, args);
    }
}
