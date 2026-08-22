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

class TitmouseClassicRouteTest {
    @Test
    void alarmInTheTreesUsesEightAuthoredEncountersAndNoNewStage() throws Exception {
        List<ClassicEncounter> route = route(new BirdGame3());

        assertEquals(8, route.size());
        assertEquals(List.of("First Chirp", "Cache Thieves", "Raise the Alarm",
                        "Every Voice Different", "No Place to Hide", "All Wings Answer",
                        "Bonus: Where I Left Everything", "The Last Hush"),
                route.stream().map(encounter -> encounter.name).toList());
        assertEquals(List.of(MapType.FOREST, MapType.CITY, MapType.VIBRANT_JUNGLE,
                        MapType.FOREST, MapType.CAVE, MapType.BEACON_CROWN,
                        MapType.FROSTBITE_FJORD, MapType.SILENT_AMPHITHEATER),
                route.stream().map(encounter -> encounter.map).toList());
        assertEquals(ClassicEncounterStyle.TITMOUSE_MEMORY_CACHE, route.get(6).style);
        assertEquals(ClassicEncounterStyle.TITMOUSE_OLD_OWL_BOSS, route.get(7).style);
        assertEquals(3, route.get(3).waves.length);
        assertTrue(route.get(3).waves[0][0].powerMult() >= 0.95);
        assertTrue(route.get(3).waves[1][0].powerMult() >= 1.00);
        assertTrue(route.get(3).waves[2][0].powerMult() >= 1.10,
                "The voice gauntlet must escalate despite its safety-oriented style scale.");
        assertTrue(route.getLast().bossFight);
        assertEquals("THE ALARM IN THE TREES", invoke(new BirdGame3(), "classicRouteTitle",
                new Class<?>[]{BirdType.class}, BirdType.TITMOUSE));
    }

    @Test
    void everyEnemyIsUltlessAndBothAlarmGauntletsAdvance() throws Exception {
        for (int round = 0; round < 8; round++) {
            BirdGame3 game = prepared(round, 0x521000L + round, 0x522000L + round);
            for (int slot = 1; slot < game.activePlayers; slot++) {
                Bird enemy = game.players[slot];
                if (enemy != null && game.getEffectiveTeam(slot) == 2) assertFalse(enemy.hasUltimate());
            }
        }

        for (int round : List.of(3, 5)) {
            BirdGame3 game = prepared(round, 0x523000L + round, 0x524000L + round);
            int waves = game.classicEncounter.waves.length;
            for (int wave = 0; wave < waves; wave++) {
                clearEnemies(game);
                assertEquals(wave + 1 < waves, game.holdClassicTitmouseEncounterOpen());
            }
        }
    }

    @Test
    void memoryCourseIsReachableGuidedCompletableAndTimeoutSafe() throws Exception {
        BirdGame3 game = prepared(6, 0x525001L, 0x525002L);
        game.headlessHarnessMode = true;
        Bird player = game.players[0];
        @SuppressWarnings("unchecked")
        List<Object> caches = (List<Object>) get(game, "classicTitmouseMemoryCaches");
        assertEquals(7, caches.size());
        assertTrue(game.holdClassicTitmouseEncounterOpen());

        for (int index = 0; index < caches.size(); index++) {
            Object cache = caches.get(index);
            double x = (double) get(cache, "x");
            double y = (double) get(cache, "y");
            assertTrue(x >= 700.0 && x <= BirdGame3.WORLD_WIDTH - 700.0);
            assertTrue(y > 150.0 && y < BirdGame3.GROUND_Y);
            player.x = x - player.bodyWidth() * 0.5;
            player.y = y - player.bodyHeight() * 0.5;
            game.applyTitmouseClassicRuntimeEffects();
            assertTrue((boolean) get(cache, "broken"));
        }
        assertTrue(game.matchEnded);
        assertSame(player, game.harnessWinner);

        BirdGame3 timeout = prepared(6, 0x525003L, 0x525004L);
        timeout.headlessHarnessMode = true;
        timeout.finishClassicTitmouseMemoryFromTimeout();
        assertTrue(timeout.matchEnded);
        assertSame(timeout.players[0], timeout.harnessWinner);
    }

    @Test
    void oldOwlFliesUsesStaminaPunishWindowsAndBreaksCornerSpam() throws Exception {
        BirdGame3 game = prepared(7, 0x526001L, 0x526002L);
        Bird boss = firstEnemy(game);
        Bird player = game.players[0];
        assertNotNull(boss);
        assertTrue(game.isClassicStaminaBoss(boss));
        assertFalse(game.isAI[boss.playerIndex]);
        assertEquals(BirdGame3.OLD_OWL_BASE_HEALTH * 0.82, boss.health, 0.001);
        assertEquals(BirdGame3.OLD_OWL_GUARDED_DAMAGE_SCALE,
                game.classicStaminaBossIncomingDamageScale(boss), 0.0001);

        double minX = boss.bodyCenterX();
        double maxX = minX;
        double maxScale = 0.0;
        for (int tick = 0; tick < 220 && game.harnessTick(); tick++) {
            minX = Math.min(minX, boss.bodyCenterX());
            maxX = Math.max(maxX, boss.bodyCenterX());
            maxScale = Math.max(maxScale, game.classicStaminaBossIncomingDamageScale(boss));
        }
        assertTrue(maxX - minX > 180.0, "The Old Owl must leave a stationary spam position.");
        assertEquals(BirdGame3.OLD_OWL_OPEN_DAMAGE_SCALE, maxScale, 0.0001);

        player.x = boss.x;
        player.y = boss.y;
        game.onClassicStaminaBossDamaged(boss, player, 40.0);
        assertTrue((int) get(game, "classicOldOwlReversalTimer") > 0,
                "Sustained close damage must arm the defensive wing burst.");
        assertEquals("THE OLD OWL", invoke(game, "matchSummaryBirdLabel",
                new Class<?>[]{Bird.class}, boss));
    }

    @Test
    void endingKeepsTheCrownWholeAndTurnsItIntoAWarning() {
        ClassicEndingContent.Ending ending = ClassicEndingContent.endingFor(BirdType.TITMOUSE);

        assertNotNull(ending);
        assertEquals(ClassicEndingContent.Alignment.HOPEFUL, ending.alignment());
        assertTrue(ending.crownChoice().contains("intact Crown"));
        assertFalse(ending.crownChoice().toLowerCase().contains("split"));
        assertTrue(ClassicEndingContent.isTitmouseWarningBeacon(ending.cinematic()));
        assertTrue(ending.cinematic().beats().getLast().narration().endsWith("warning tells everyone to fly."));
    }

    private static BirdGame3 prepared(int round, long routeSeed, long matchSeed) {
        BirdGame3 game = new BirdGame3();
        game.harnessPrepareClassicEncounter(BirdType.TITMOUSE, round, 5.0, 6, routeSeed, matchSeed);
        return game;
    }

    private static Bird firstEnemy(BirdGame3 game) {
        for (int slot = 1; slot < game.activePlayers; slot++) {
            Bird bird = game.players[slot];
            if (bird != null && game.getEffectiveTeam(slot) == 2 && game.scores[slot] > 0) return bird;
        }
        return null;
    }

    private static void clearEnemies(BirdGame3 game) {
        for (int slot = 1; slot < game.activePlayers; slot++) {
            if (game.players[slot] != null && game.getEffectiveTeam(slot) == 2) {
                game.players[slot].health = 0.0;
                game.scores[slot] = 0;
            }
        }
    }

    @SuppressWarnings("unchecked")
    private static List<ClassicEncounter> route(BirdGame3 game) throws Exception {
        return (List<ClassicEncounter>) invoke(game, "buildTitmouseClassicRun", new Class<?>[0]);
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
