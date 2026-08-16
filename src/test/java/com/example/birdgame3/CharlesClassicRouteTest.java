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
        assertEquals(2, prepared(0, 0xC4A25EL, 0xC4A25FL).scores[0],
                "The opening audition should teach the route with a second stock.");
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
    void hollowMaestroIsAnOriginalStaminaBossWithThreeTelegraphedMovements() throws Exception {
        BirdGame3 game = prepared(7, 0xC4A221L, 0xC4A222L);
        Bird boss = firstEnemy(game);

        assertNotNull(boss);
        assertTrue(game.usesSmashCombatRules(), "Charles should retain the route's normal Smash rules.");
        assertEquals(3, game.scores[0]);
        assertEquals(1, game.scores[boss.playerIndex],
                "The Maestro has one stamina life instead of three launchable stocks.");
        assertFalse(game.isAI[boss.playerIndex]);
        assertFalse(boss.hasUltimate());
        assertTrue(game.isClassicStaminaBoss(boss));
        assertEquals(BirdGame3.HOLLOW_MAESTRO_BASE_HEALTH, boss.health, 0.0001);
        assertEquals(boss.health, boss.getMaxHealth(), 0.0001);
        assertEquals(1.72, boss.sizeMultiplier, 0.0001);
        assertEquals("music-charles-maestro.mp3", invoke(game, "gameplayMusicFile", new Class<?>[0]));

        String source = Files.readString(Path.of("src/main/java/com/example/birdgame3/BirdGame3.java"));
        int start = source.indexOf("private void setupCharlesClassicRoute");
        int end = source.indexOf("private void setupHummingbirdNectarRoute", start);
        String routeCode = source.substring(start, end);
        assertTrue(routeCode.contains("drawHollowMaestroMask"));
        assertTrue(source.contains("drawClassicStaminaBossHud"));
        assertTrue(routeCode.contains("drawHollowMaestroTelegraph"));
        assertTrue(routeCode.contains("performHollowMaestroAttack"));
    }

    @Test
    void hollowMaestroTakesHealthDamageWithoutGivingUpCharlesSmashRules() throws Exception {
        BirdGame3 game = prepared(7, 0xC4A2A1L, 0xC4A2A2L);
        Bird charles = game.players[0];
        Bird boss = firstEnemy(game);
        double startingHealth = boss.health;

        double dealt = charles.applyUnshieldedDamageTo(boss, 40.0);

        assertTrue(dealt > 0.0);
        assertTrue(boss.health < startingHealth);
        assertEquals(dealt, startingHealth - boss.health, 0.0001);
        assertTrue(dealt > 40.0,
                "The Hollow Score stagger scale should keep the large stamina pool from becoming a damage sponge.");
        assertEquals(0.0, (double) get(boss, "smashDamage"), 0.0001,
                "Stamina damage must not secretly fill an ordinary launch percent meter.");
        assertEquals(1, game.scores[boss.playerIndex]);
        assertEquals(0, BirdGame3.hollowMaestroPhaseForHealth(startingHealth, startingHealth));
        assertEquals(1, BirdGame3.hollowMaestroPhaseForHealth(startingHealth * 0.60, startingHealth));
        assertEquals(2, BirdGame3.hollowMaestroPhaseForHealth(startingHealth * 0.20, startingHealth));

        boss.health = 1.0;
        charles.applyUnshieldedDamageTo(boss, 40.0);
        assertEquals(0.0, boss.health, 0.0);
        assertEquals(0, game.scores[boss.playerIndex],
                "Depleting the giant health bar must end the Maestro's only stamina life.");
    }

    @Test
    void hollowMaestroCannotBeDefeatedByWaitingOutItsStaminaClock() {
        BirdGame3 game = prepared(7, 0xC4A2A3L, 0xC4A2A4L);
        Bird boss = firstEnemy(game);
        game.headlessHarnessMode = true;
        game.matchTimer = 0;

        assertFalse(game.harnessTick());
        assertSame(boss, game.harnessWinner,
                "An intact stamina boss must win at time instead of losing to Charles's extra stocks.");
    }

    @Test
    void hollowMaestroCannotLoseItsStaminaLifeToABlastZoneShortcut() throws Exception {
        BirdGame3 game = prepared(7, 0xC4A2A5L, 0xC4A2A6L);
        Bird boss = firstEnemy(game);
        double startingHealth = boss.health;

        invoke(boss, "handleSmashBlastZoneKo",
                new Class<?>[]{boolean.class, boolean.class, double.class, double.class,
                        double.class, double.class, String.class, boolean.class, double.class, double.class},
                false, true, 840.0, 5_160.0, 3_000.0, BirdGame3.GROUND_Y - 400.0,
                "off the right side", false, 5_700.0, BirdGame3.GROUND_Y);

        assertEquals(startingHealth, boss.health, 0.0);
        assertEquals(1, game.scores[boss.playerIndex]);
        assertEquals(game.battlefieldSpawnCenterX() - boss.bodyWidth() * 0.5, boss.x, 0.0001);
        assertEquals(0.0, boss.vx, 0.0);
        assertEquals(0.0, boss.vy, 0.0);
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

        assertMainPlatform(prepared(0, 0xC4A225L, 0xC4A226L), 760.0, 4_480.0);
        assertMainPlatform(prepared(1, 0xC4A227L, 0xC4A228L), 980.0, 4_040.0);
        assertMainPlatform(prepared(7, 0xC4A229L, 0xC4A230L), 840.0, 4_320.0);

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
    void creditedCc0MusicAndLivingScoreEndingAreBundled() {
        for (String file : List.of("music-charles-hall.mp3", "music-charles-spire.mp3",
                "music-charles-maestro.mp3", "music-charles-ending.mp3")) {
            Path path = Path.of("src/main/resources/sounds", file);
            assertTrue(Files.exists(path), file);
            assertTrue(path.toFile().length() > 900_000, file + " should be a full cue, not a stinger.");
        }
        ClassicEndingContent.Cinematic cinematic = ClassicEndingContent.endingFor(BirdType.MOCKINGBIRD).cinematic();
        assertTrue(ClassicEndingContent.isCharlesLivingScore(cinematic));
        assertEquals("music-charles-ending.mp3", cinematic.musicCue());
        assertEquals(6, cinematic.beats().size());
    }

    @Test
    void resonanceHallPlatesRequireACommitmentThenLaunchAndRestoreRecovery() {
        BirdGame3 game = prepared(0, 0xC4A281L, 0xC4A282L);
        Bird charles = game.players[0];
        double surfaceY = BirdGame3.GROUND_Y - 220.0;
        charles.x = BirdGame3.RESONANCE_PLATE_X[1] - charles.bodyWidth() * 0.5;
        charles.y = surfaceY - charles.bodyHeight();
        charles.vy = 0.0;
        charles.canDoubleJump = false;

        for (int tick = 1; tick < BirdGame3.RESONANCE_PLATE_HOLD_FRAMES; tick++) {
            game.applyCharlesArenaRuntimeEffects();
            assertEquals(0.0, charles.vy, 0.0001, "The plate must telegraph before it launches.");
        }
        game.applyCharlesArenaRuntimeEffects();

        assertTrue(charles.vy <= -13.6);
        assertTrue(charles.canDoubleJump);
        assertEquals(BirdGame3.RESONANCE_PLATE_REUSE_FRAMES, game.resonancePlateReuseFrames[0]);
    }

    @Test
    void signalSpirePowerLineWarnsBeforeItCanShock() throws Exception {
        BirdGame3 game = prepared(1, 0xC4A283L, 0xC4A284L);
        Bird charles = game.players[0];
        game.simTick = BirdGame3.SIGNAL_POWER_LINE_START_DELAY_FRAMES;
        charles.x = 2_600.0;
        charles.y = BirdGame3.SIGNAL_LANE_Y[0] - charles.bodyHeight() * 0.5;
        charles.vx = 0.0;
        charles.vy = 0.0;
        double startingDamage = (double) get(charles, "smashDamage");

        game.applyCharlesArenaRuntimeEffects();
        assertEquals(BirdGame3.SignalPowerLineState.WARNING,
                BirdGame3.signalPowerLineState(game.simTick, 0));
        assertEquals(startingDamage, (double) get(charles, "smashDamage"), 0.0001,
                "The amber warning must never damage a fighter.");

        game.simTick += BirdGame3.SIGNAL_POWER_LINE_WARNING_FRAMES;
        game.applyCharlesArenaRuntimeEffects();

        assertEquals(BirdGame3.SignalPowerLineState.LIVE,
                BirdGame3.signalPowerLineState(game.simTick, 0));
        assertTrue((double) get(charles, "smashDamage") > startingDamage);
        assertTrue(charles.vx < 0.0, "The wire should launch away from the mast center.");
        assertTrue(charles.vy < 0.0);
        assertTrue(charles.stunTime >= 18);
        assertEquals(BirdGame3.SIGNAL_POWER_LINE_HIT_COOLDOWN_FRAMES,
                game.signalPowerLineHitCooldowns[0]);

        double damageAfterShock = (double) get(charles, "smashDamage");
        game.simTick++;
        game.applyCharlesArenaRuntimeEffects();
        assertEquals(damageAfterShock, (double) get(charles, "smashDamage"), 0.0001,
                "A live wire must not apply damage every simulation frame.");
    }

    @Test
    void signalSpireLinesArePermanentlyInstalledButActivateInStaggeredOrder() {
        assertEquals(BirdGame3.SignalPowerLineState.DORMANT,
                BirdGame3.signalPowerLineState(0, 0));
        assertEquals(BirdGame3.SignalPowerLineState.WARNING,
                BirdGame3.signalPowerLineState(BirdGame3.SIGNAL_POWER_LINE_START_DELAY_FRAMES, 0));
        assertEquals(BirdGame3.SignalPowerLineState.DORMANT,
                BirdGame3.signalPowerLineState(BirdGame3.SIGNAL_POWER_LINE_START_DELAY_FRAMES, 1));
        assertEquals(BirdGame3.SignalPowerLineState.WARNING,
                BirdGame3.signalPowerLineState(
                        BirdGame3.SIGNAL_POWER_LINE_START_DELAY_FRAMES
                                + BirdGame3.SIGNAL_POWER_LINE_STAGGER_FRAMES, 1));
    }

    @Test
    void silentAmphitheaterFieldSoftensHitstunWithoutFreezingTheFighter() {
        BirdGame3 game = prepared(7, 0xC4A285L, 0xC4A286L);
        Bird charles = game.players[0];
        charles.x = BirdGame3.SILENT_FIELD_CENTER_X - charles.bodyWidth() * 0.5;
        charles.y = BirdGame3.SILENT_FIELD_CENTER_Y - charles.bodyHeight() * 0.5;
        charles.vx = 12.0;
        charles.vy = -8.0;
        charles.stunTime = 30;

        game.applyCharlesArenaRuntimeEffects();

        assertEquals(11.844, charles.vx, 0.0001);
        assertEquals(-7.952, charles.vy, 0.0001);
        assertNotEquals(0.0, charles.vx, "The field is a refuge, not another screen-freeze effect.");
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
