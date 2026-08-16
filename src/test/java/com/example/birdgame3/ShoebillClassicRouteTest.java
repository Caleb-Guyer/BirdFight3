package com.example.birdgame3;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static com.example.birdgame3.BirdGame3.BirdType;
import static com.example.birdgame3.BirdGame3.ClassicEncounter;
import static com.example.birdgame3.BirdGame3.ClassicEncounterStyle;
import static com.example.birdgame3.BirdGame3.MapVariant;
import static org.junit.jupiter.api.Assertions.*;

class ShoebillClassicRouteTest {
    @Test
    void shoebillHasTheApprovedEightEncounterLongWatchRoute() throws Exception {
        List<ClassicEncounter> route = route(new BirdGame3());

        assertEquals(8, route.size());
        assertEquals(List.of(
                        "Needles in the Reeds", "The Larger Beak", "Eyes in the Dark", "Swift Trail",
                        "Statue Court", "Bonus: Ripple Hunt", "The Marsh in Panic", "The Mire Oracle"),
                route.stream().map(encounter -> encounter.name).toList());
        assertEquals(List.of(
                        MapVariant.STANDARD, MapVariant.TITAN_DOCK, MapVariant.STANDARD,
                        MapVariant.REDLINE_CANYON, MapVariant.CROWN_DUEL, MapVariant.STILLWATER_MARSH,
                        MapVariant.STILLWATER_MARSH, MapVariant.STILLWATER_MARSH),
                route.stream().map(encounter -> encounter.variant).toList());
        assertEquals(ClassicEncounterStyle.MINIATURE_FLOCK, route.get(0).style);
        assertEquals(ClassicEncounterStyle.GIANT, route.get(1).style);
        assertEquals(ClassicEncounterStyle.SHOEBILL_TRAIL, route.get(3).style);
        assertEquals(ClassicEncounterStyle.RIPPLE_HUNT, route.get(5).style);
        assertEquals(ClassicEncounterStyle.MARSH_GAUNTLET, route.get(6).style);
        assertEquals(ClassicEncounterStyle.MIRE_ORACLE_BOSS, route.get(7).style);
        assertTrue(route.getLast().bossFight);
        assertEquals("THE LONG WATCH", invoke(new BirdGame3(), "classicRouteTitle",
                new Class<?>[]{BirdType.class}, BirdType.SHOEBILL));
    }

    @Test
    void routeUsesApprovedFightsAndBothRoundFourTrails() throws Exception {
        BirdGame3 game = new BirdGame3();
        List<ClassicEncounter> route = route(game);
        set(game, "classicSelectedBird", BirdType.SHOEBILL);
        @SuppressWarnings("unchecked")
        List<ClassicEncounter> activeRoute = (List<ClassicEncounter>) get(game, "classicRun");
        activeRoute.addAll(route);

        assertEquals(List.of(BirdType.HUMMINGBIRD, BirdType.HUMMINGBIRD, BirdType.HUMMINGBIRD),
                List.of(route.get(0).enemies[0].type(), route.get(0).enemies[1].type(), route.get(0).enemies[2].type()));
        assertEquals(BirdType.PELICAN, route.get(1).enemies[0].type());
        assertEquals(BirdType.BAT, route.get(2).allies[0].type());
        assertEquals(List.of(BirdType.RAVEN, BirdType.HEISENBIRD),
                List.of(route.get(2).enemies[0].type(), route.get(2).enemies[1].type()));

        assertTrue(game.selectShoebillClassicTrail(false));
        ClassicEncounter heavy = activeRoute.get(3);
        assertEquals("Heavy Trail", heavy.name);
        assertEquals(MapVariant.LAST_ICE_SHELF, heavy.variant);
        assertEquals(List.of(BirdType.GOOSE, BirdType.PENGUIN),
                List.of(heavy.enemies[0].type(), heavy.enemies[1].type()));

        assertTrue(game.selectShoebillClassicTrail(true));
        ClassicEncounter swift = activeRoute.get(3);
        assertEquals(MapVariant.REDLINE_CANYON, swift.variant);
        assertEquals(List.of(BirdType.ROADRUNNER, BirdType.FALCON),
                List.of(swift.enemies[0].type(), swift.enemies[1].type()));
    }

    @Test
    void rippleHuntUsesRealTargetArtAndAnyNormalHitCanClearIt() throws Exception {
        BirdGame3 game = prepared(5, 0x51A11L, 0x7100L);

        assertFalse(game.classicUsesSmashRules());
        for (int slot = 1; slot < game.activePlayers; slot++) {
            Bird target = game.players[slot];
            assertNotNull(target);
            assertTrue(target.classicBonusTarget);
            assertFalse(target.hasUltimate());
            assertFalse(game.isAI[slot]);
            invoke(target, "receiveScaledDamage", new Class<?>[]{double.class, Bird.class},
                    40.0, game.players[0]);
            target.update(1.0);
            assertTrue(target.health <= 0.0);
        }
    }

    @Test
    void marshGauntletSpawnsAllThreeWavesWithoutMinionUlts() {
        BirdGame3 game = prepared(6, 0x51A12L, 0x7101L);

        assertFalse(firstEnemy(game).hasUltimate());
        eliminateEnemyTeam(game);
        assertTrue(game.holdClassicShoebillEncounterOpen());
        assertEquals(2, enemyCount(game));
        assertAllEnemiesHaveNoUltimate(game);
        eliminateEnemyTeam(game);
        assertTrue(game.holdClassicShoebillEncounterOpen());
        assertEquals(2, enemyCount(game));
        assertAllEnemiesHaveNoUltimate(game);
        eliminateEnemyTeam(game);
        assertFalse(game.holdClassicShoebillEncounterOpen());
    }

    @Test
    void mireOracleTellIsPermanentAndEchoesDoNotReceiveBossStocks() throws Exception {
        BirdGame3 game = prepared(7, 0x51A13L, 0x7102L);

        Bird oracle = game.players[1];
        assertTrue(oracle.name.startsWith("Boss: The Mire Oracle"));
        assertEquals(3, game.scores[0]);
        assertEquals(1, game.scores[1]);
        assertEquals(1, game.scores[2]);
        assertEquals(1, game.scores[3]);
        assertAllEnemiesHaveNoUltimate(game);

        String source = Files.readString(Path.of("src/main/java/com/example/birdgame3/BirdGame3.java"));
        int start = source.indexOf("private void drawClassicShoebillRouteFeatures");
        int end = source.indexOf("private void setupHummingbirdNectarRoute", start);
        String routeArt = source.substring(start, end);
        assertTrue(routeArt.contains("TRUE WAKE"));
        assertTrue(routeArt.contains("#FFE082"));
        assertFalse(routeArt.contains("ShoebillSpecials"));
    }

    @Test
    void classicRouteNeverChangesShoebillsNormalFighterKit() {
        BirdGame3 game = prepared(7, 0x51A14L, 0x7103L);
        Bird player = game.players[0];
        double baseSize = player.baseSizeMultiplier;
        double basePower = player.basePowerMultiplier;
        double baseSpeed = player.baseSpeedMultiplier;
        int attackCooldown = player.attackCooldown;
        int specialCooldown = player.specialCooldown;

        for (int i = 0; i < 240; i++) game.applyShoebillClassicRuntimeEffects();

        assertEquals(baseSize, player.baseSizeMultiplier, 0.0);
        assertEquals(basePower, player.basePowerMultiplier, 0.0);
        assertEquals(baseSpeed, player.baseSpeedMultiplier, 0.0);
        assertEquals(attackCooldown, player.attackCooldown);
        assertEquals(specialCooldown, player.specialCooldown);
    }

    @Test
    void stillwaterMarshIsConnectedAndShoebillBossSimulationIsDeterministic() throws Exception {
        BirdGame3 first = prepared(7, 0x51A15L, 0x7104L);

        assertEquals(MapVariant.STILLWATER_MARSH, get(first, "activeArenaGeometryVariant"));
        assertTrue(first.platforms.stream().anyMatch(platform ->
                Math.abs(platform.x - 900.0) < 0.01 && platform.w >= 4_200.0));
        assertTrue(first.platforms.stream().allMatch(platform ->
                platform.x + platform.w * 0.5 >= 470.0 && platform.x + platform.w * 0.5 <= 5_530.0));

        long[] firstHashes = new long[240];
        for (int tick = 0; tick < 240; tick++) {
            first.harnessTick();
            firstHashes[tick] = first.harnessStateHash();
        }
        BirdGame3 second = prepared(7, 0x51A15L, 0x7104L);
        for (int tick = 0; tick < 240; tick++) {
            second.harnessTick();
            assertEquals(firstHashes[tick], second.harnessStateHash(), "desync at tick " + tick);
        }
    }

    private static BirdGame3 prepared(int round, long routeSeed, long matchSeed) {
        BirdGame3 game = new BirdGame3();
        game.harnessPrepareClassicEncounter(BirdType.SHOEBILL, round, 5.0, 6, routeSeed, matchSeed);
        return game;
    }

    @SuppressWarnings("unchecked")
    private static List<ClassicEncounter> route(BirdGame3 game) throws Exception {
        return (List<ClassicEncounter>) invoke(game, "buildShoebillClassicRun", new Class<?>[0]);
    }

    private static Bird firstEnemy(BirdGame3 game) {
        for (int i = 1; i < game.activePlayers; i++) if (game.players[i] != null) return game.players[i];
        return null;
    }

    private static int enemyCount(BirdGame3 game) {
        int count = 0;
        for (int i = 1; i < game.activePlayers; i++) if (game.players[i] != null && game.scores[i] > 0) count++;
        return count;
    }

    private static void eliminateEnemyTeam(BirdGame3 game) {
        for (int i = 1; i < game.activePlayers; i++) {
            if (game.players[i] != null) {
                game.players[i].health = 0.0;
                game.scores[i] = 0;
            }
        }
    }

    private static void assertAllEnemiesHaveNoUltimate(BirdGame3 game) {
        for (int i = 1; i < game.activePlayers; i++) {
            if (game.players[i] != null && game.scores[i] > 0) assertFalse(game.players[i].hasUltimate());
        }
    }

    private static Object invoke(Object target, String methodName, Class<?>[] types, Object... args) throws Exception {
        Method method = target.getClass().getDeclaredMethod(methodName, types);
        method.setAccessible(true);
        return method.invoke(target, args);
    }

    private static Object get(Object target, String fieldName) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        return field.get(target);
    }

    private static void set(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }
}
