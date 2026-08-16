package com.example.birdgame3;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static com.example.birdgame3.BirdGame3.BirdType;
import static com.example.birdgame3.BirdGame3.ClassicEncounter;
import static com.example.birdgame3.BirdGame3.ClassicEncounterStyle;
import static com.example.birdgame3.BirdGame3.MapType;
import static org.junit.jupiter.api.Assertions.*;

class CharlesClassicRouteTest {
    @Test
    void charlesHasTheApprovedNonlinearEightEncounterRoute() throws Exception {
        List<ClassicEncounter> route = route(new BirdGame3());

        assertEquals(8, route.size());
        assertEquals(List.of(
                        "Call and Answer", "Inherited Voice", "Manufactured Voice", "Stolen Voice",
                        "The Understudies", "Bonus: Perfect Pitch", "Dead Air", "The Hollow Maestro"),
                route.stream().map(encounter -> encounter.name).toList());
        assertEquals(List.of(
                        MapType.RESONANCE_HALL, MapType.SIGNAL_SPIRE, MapType.PRISON, MapType.CITY,
                        MapType.RESONANCE_HALL, MapType.RESONANCE_HALL, MapType.SIGNAL_SPIRE,
                        MapType.SILENT_AMPHITHEATER),
                route.stream().map(encounter -> encounter.map).toList());
        assertEquals(ClassicEncounterStyle.CHARLES_UNDERSTUDIES, route.get(4).style);
        assertEquals(ClassicEncounterStyle.PERFECT_PITCH, route.get(5).style);
        assertEquals(ClassicEncounterStyle.CHOIR_MASK_GAUNTLET, route.get(6).style);
        assertEquals(ClassicEncounterStyle.HOLLOW_MAESTRO_BOSS, route.get(7).style);
        assertTrue(route.getLast().bossFight);
        assertEquals("NO VOICE BUT HIS OWN", invoke(new BirdGame3(), "classicRouteTitle",
                new Class<?>[]{BirdType.class}, BirdType.MOCKINGBIRD));
    }

    @Test
    void allThreeAuditionsCanBeSelectedInAnyOrderWithoutRerollingTheirContent() throws Exception {
        BirdGame3 game = new BirdGame3();
        set(game, "classicSelectedBird", BirdType.MOCKINGBIRD);
        @SuppressWarnings("unchecked")
        List<ClassicEncounter> active = (List<ClassicEncounter>) get(game, "classicRun");
        active.addAll(route(game));

        Set<String> chosen = new HashSet<>();
        set(game, "classicRoundIndex", 1);
        assertTrue(game.selectCharlesAudition(3));
        chosen.add(active.get(1).name);
        set(game, "classicRoundIndex", 2);
        assertTrue(game.selectCharlesAudition(3));
        chosen.add(active.get(2).name);
        set(game, "classicRoundIndex", 3);
        assertTrue(game.selectCharlesAudition(3));
        chosen.add(active.get(3).name);

        assertEquals(Set.of("Inherited Voice", "Manufactured Voice", "Stolen Voice"), chosen);
        assertFalse(game.selectCharlesAudition(4));
    }

    @Test
    void perfectPitchRequiresTheShownOrderAndCompletesWithoutAHiddenOpponent() throws Exception {
        BirdGame3 game = prepared(5, 0xC4A215L, 0xC4A216L);
        game.headlessHarnessMode = true;
        set(game, "classicPitchRevealFrames", 0);

        assertFalse(game.classicUsesSmashRules());
        assertEquals(4, ((List<?>) get(game, "classicPitchBells")).size());
        assertArrayEquals(new int[]{0, 2, 1, 3}, (int[]) get(game, "classicPitchSequence"));
        assertTrue(game.holdClassicCharlesEncounterOpen());

        assertFalse(game.registerCharlesPitchStrike(1));
        assertEquals(0, get(game, "classicPitchSequenceIndex"));
        set(game, "classicPitchRevealFrames", 0);
        assertTrue(game.registerCharlesPitchStrike(0));
        assertTrue(game.registerCharlesPitchStrike(2));
        assertTrue(game.registerCharlesPitchStrike(1));
        assertTrue(game.registerCharlesPitchStrike(3));

        assertTrue((boolean) get(game, "classicPitchCompleted"));
        assertEquals(75, get(game, "classicBonusCoins"));
        assertSame(game.players[0], game.harnessWinner);
    }

    @Test
    void perfectPitchTimesOutAsAFailedObjectiveInsteadOfStartingSuddenDeath() {
        BirdGame3 game = prepared(5, 0xC4A231L, 0xC4A232L);
        game.headlessHarnessMode = true;

        assertTrue(game.isClassicPerfectPitchActive());
        game.finishClassicPerfectPitchFromTimeout();

        assertTrue(game.matchEnded);
        assertNull(game.harnessWinner);
        assertFalse(game.suddenDeath.isActive());
    }

    @Test
    void understudiesAndOriginalChoirMasksAdvanceThroughThreeUltlessWaves() {
        BirdGame3 understudies = prepared(4, 0xC4A217L, 0xC4A218L);
        assertThreeWaveEncounter(understudies, true);

        BirdGame3 masks = prepared(6, 0xC4A219L, 0xC4A220L);
        assertEquals(1, masks.scores[0]);
        assertFalse(masks.isAI[1], "Choir Masks use their own route controller, not a hidden bird AI.");
        assertThreeWaveEncounter(masks, false);
    }

    @Test
    void twoOnOneAuditionsUseRouteStocksWithoutChangingCharlesStats() {
        for (int round = 1; round <= 3; round++) {
            BirdGame3 game = prepared(round, 0xC4A260L + round, 0xC4A270L + round);
            assertEquals(2, game.scores[0]);
            double expectedSize = round == 1 ? 0.82 : (round == 2 ? 0.92 : 0.88);
            for (int slot = 1; slot < game.activePlayers; slot++) {
                Bird enemy = game.players[slot];
                if (enemy == null) continue;
                assertEquals(expectedSize, enemy.sizeMultiplier, 0.0001);
                assertFalse(enemy.hasUltimate());
            }
        }
    }

    @Test
    void hollowMaestroIsAnOriginalThreeMovementBossWithTelegraphedAttacks() throws Exception {
        BirdGame3 game = prepared(7, 0xC4A221L, 0xC4A222L);
        Bird boss = firstEnemy(game);

        assertNotNull(boss);
        assertEquals(3, game.scores[0]);
        assertEquals(3, game.scores[boss.playerIndex]);
        assertFalse(game.isAI[boss.playerIndex]);
        assertFalse(boss.hasUltimate());
        assertEquals(1.42, boss.sizeMultiplier, 0.0001);

        String source = Files.readString(Path.of("src/main/java/com/example/birdgame3/BirdGame3.java"));
        int start = source.indexOf("private void setupCharlesClassicRoute");
        int end = source.indexOf("private void setupHummingbirdNectarRoute", start);
        String routeCode = source.substring(start, end);
        assertTrue(routeCode.contains("drawHollowMaestroMask"));
        assertTrue(routeCode.contains("drawHollowMaestroTelegraph"));
        assertTrue(routeCode.contains("performHollowMaestroAttack"));
    }

    @Test
    void charlesRouteNeverChangesCharlesOrdinaryFighterKit() {
        BirdGame3 game = prepared(7, 0xC4A223L, 0xC4A224L);
        Bird player = game.players[0];
        double size = player.baseSizeMultiplier;
        double power = player.basePowerMultiplier;
        double speed = player.baseSpeedMultiplier;
        int attackCooldown = player.attackCooldown;
        int specialCooldown = player.specialCooldown;

        for (int tick = 0; tick < 240; tick++) {
            game.simTick++;
            game.applyCharlesClassicRuntimeEffects();
        }

        assertEquals(size, player.baseSizeMultiplier, 0.0);
        assertEquals(power, player.basePowerMultiplier, 0.0);
        assertEquals(speed, player.baseSpeedMultiplier, 0.0);
        assertEquals(attackCooldown, player.attackCooldown);
        assertEquals(specialCooldown, player.specialCooldown);
    }

    @Test
    void newMainMapsAreAppendOnlyConnectedAndUnlockedByCharlesBadge() throws Exception {
        assertEquals(11, MapType.PRISON.ordinal());
        assertEquals(12, MapType.RESONANCE_HALL.ordinal());
        assertEquals(13, MapType.SIGNAL_SPIRE.ordinal());
        assertEquals(14, MapType.SILENT_AMPHITHEATER.ordinal());

        assertMainPlatform(prepared(0, 0xC4A225L, 0xC4A226L), 720.0, 4_560.0);
        assertMainPlatform(prepared(1, 0xC4A227L, 0xC4A228L), 860.0, 4_280.0);
        assertMainPlatform(prepared(7, 0xC4A229L, 0xC4A230L), 920.0, 4_160.0);

        BirdGame3 progress = new BirdGame3();
        assertFalse((boolean) invoke(progress, "isMapUnlocked", new Class<?>[]{MapType.class},
                MapType.RESONANCE_HALL));
        progress.setClassicCompleted(BirdType.MOCKINGBIRD);
        for (MapType map : List.of(MapType.RESONANCE_HALL, MapType.SIGNAL_SPIRE,
                MapType.SILENT_AMPHITHEATER)) {
            assertTrue((boolean) invoke(progress, "isMapUnlocked", new Class<?>[]{MapType.class}, map));
        }
    }

    @Test
    void originalMusicAndLivingScoreEndingAreBundled() {
        for (String file : List.of("music-charles-route.wav", "music-charles-maestro.wav",
                "music-charles-ending.wav")) {
            Path path = Path.of("src/main/resources/sounds", file);
            assertTrue(Files.exists(path), file);
            assertTrue(path.toFile().length() > 900_000, file + " should be a full cue, not a stinger.");
        }
        ClassicEndingContent.Cinematic cinematic = ClassicEndingContent.endingFor(BirdType.MOCKINGBIRD).cinematic();
        assertTrue(ClassicEndingContent.isCharlesLivingScore(cinematic));
        assertEquals("music-charles-ending.wav", cinematic.musicCue());
        assertEquals(6, cinematic.beats().size());
    }

    private static void assertThreeWaveEncounter(BirdGame3 game, boolean normalAi) {
        assertNotNull(firstEnemy(game));
        assertEquals(normalAi, game.isAI[firstEnemy(game).playerIndex]);
        assertFalse(firstEnemy(game).hasUltimate());
        eliminateEnemyTeam(game);
        assertTrue(game.holdClassicCharlesEncounterOpen());
        assertEquals(normalAi, game.isAI[firstEnemy(game).playerIndex]);
        assertFalse(firstEnemy(game).hasUltimate());
        eliminateEnemyTeam(game);
        assertTrue(game.holdClassicCharlesEncounterOpen());
        assertEquals(normalAi, game.isAI[firstEnemy(game).playerIndex]);
        assertFalse(firstEnemy(game).hasUltimate());
        eliminateEnemyTeam(game);
        assertFalse(game.holdClassicCharlesEncounterOpen());
    }

    private static void assertMainPlatform(BirdGame3 game, double x, double width) {
        assertTrue(game.usesIslandBoundsForCurrentArena());
        assertTrue(game.platforms.stream().anyMatch(platform ->
                Math.abs(platform.x - x) < 0.01 && Math.abs(platform.w - width) < 0.01));
        assertTrue(game.platforms.stream().noneMatch(platform ->
                platform.x < -120.0 || platform.x + platform.w > BirdGame3.WORLD_WIDTH + 120.0));
    }

    private static BirdGame3 prepared(int round, long routeSeed, long matchSeed) {
        BirdGame3 game = new BirdGame3();
        game.harnessPrepareClassicEncounter(BirdType.MOCKINGBIRD, round, 5.0, 6, routeSeed, matchSeed);
        return game;
    }

    private static Bird firstEnemy(BirdGame3 game) {
        for (int slot = 1; slot < game.activePlayers; slot++) {
            Bird bird = game.players[slot];
            if (bird != null && game.getEffectiveTeam(slot) == 2 && game.scores[slot] > 0) return bird;
        }
        return null;
    }

    private static void eliminateEnemyTeam(BirdGame3 game) {
        for (int slot = 1; slot < game.activePlayers; slot++) {
            if (game.players[slot] != null) {
                game.players[slot].health = 0.0;
                game.scores[slot] = 0;
            }
        }
    }

    @SuppressWarnings("unchecked")
    private static List<ClassicEncounter> route(BirdGame3 game) throws Exception {
        return (List<ClassicEncounter>) invoke(game, "buildCharlesClassicRun", new Class<?>[0]);
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
