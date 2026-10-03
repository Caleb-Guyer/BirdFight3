package com.example.birdgame3;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.UUID;
import java.util.prefs.Preferences;
import static org.junit.jupiter.api.Assertions.*;

class FlockRunIntegrationTest {
    private final Preferences root = Preferences.userRoot().node("/birdfight3-tests/flock-run/" + UUID.randomUUID());
    @AfterEach void cleanup() throws Exception { root.removeNode(); root.flush(); }
    private BirdGame3 game() { return new BirdGame3(root); }

    @Test void perksAffectRealDamageUltimateAndCooldownsWithoutChangingGlobalTuning() throws Exception {
        FlockRunState run = FlockRunStateTest.summit(17, FlockRunState.Route.ELITE);
        run.chooseRoute(FlockRunState.Route.BOSS);
        BirdGame3 game = game();
        double[] tuning = {run.bird.damageDealtMult, run.bird.damageTakenMult, run.bird.cooldownRate, run.bird.ultimateRate};
        game.harnessPrepareFlockRunEncounter(run, 5);
        Bird player = game.players[0];
        assertEquals(run.health(), player.health);
        assertEquals(112, player.getMaxHealth());
        assertEquals(1.04 * run.speedMultiplier(), player.baseSpeedMultiplier, 1e-9);
        assertTrue(run.rank(FlockRunState.Perk.TALONS) > 0);
        assertTrue(run.rank(FlockRunState.Perk.GUARD) > 0);
        assertTrue(run.rank(FlockRunState.Perk.QUICKEN) > 0);
        assertTrue(run.rank(FlockRunState.Perk.SPIRIT) > 0);

        Bird target = Arrays.stream(game.players).filter(b -> b != null && game.getEffectiveTeam(b.playerIndex) == 2).findFirst().orElseThrow();
        double expected = 4 * player.type.damageDealtMult * run.damageMultiplier() * target.type.damageTakenMult;
        assertEquals(expected, invoke(target, "receiveScaledDamage", new Class<?>[]{double.class, Bird.class}, 4.0, player), 1e-8);
        player.flockDamageMultiplier = 1;
        double baselineMinionDamage = target.receiveOwnedMinionDamage(4, player);
        player.flockDamageMultiplier = run.damageMultiplier();
        assertEquals(baselineMinionDamage * run.damageMultiplier(), target.receiveOwnedMinionDamage(4, player), 1e-8);
        player.flockIncomingMultiplier = 1;
        double baselineIncoming = player.receiveExternalDamage(4);
        player.flockIncomingMultiplier = run.incomingMultiplier();
        assertEquals(baselineIncoming * run.incomingMultiplier(), player.receiveExternalDamage(4), 1e-8);
        player.setUltimateEnabled(false);
        player.setUltimateEnabled(true);
        invoke(player, "gainUltimate", new Class<?>[]{double.class}, 10.0);
        assertEquals(10 * player.type.ultimateRate * run.ultimateMultiplier() / 100, player.getUltimateRatio(), 1e-8);
        game.isAI[0] = false;
        player.specialCooldown = 100;
        for (int i = 0; i < 10; i++) player.update(1.0);
        assertTrue(player.specialCooldown < 100 - 10 * player.type.cooldownRate,
                "The perk must accelerate the real cooldown decrement");
        assertArrayEquals(tuning, new double[]{run.bird.damageDealtMult, run.bird.damageTakenMult, run.bird.cooldownRate, run.bird.ultimateRate});

        game.harnessPrepareClassicEncounter(BirdGame3.BirdType.PIGEON, 0, 5, 5, 11, 12);
        assertFalse(game.flockRunMatchActive);
        assertEquals(1, game.players[0].flockDamageMultiplier);
        assertEquals(1, game.players[0].flockIncomingMultiplier);
        assertEquals(1, game.players[0].flockCooldownMultiplier);
        assertEquals(1, game.players[0].flockUltimateMultiplier);
    }

    @Test void timeoutAndPlayerDeathEndRunEvenWithLivingAllies() {
        FlockRunState run = FlockRunStateTest.run(9);
        run.chooseRoute(FlockRunState.Route.SAFE);
        BirdGame3 game = game();
        game.harnessPrepareFlockRunEncounter(run, 5);
        game.matchTimer = 0;
        new MatchController(game).updateTimerAndSuddenDeath();
        assertTrue(game.matchEnded);
        assertEquals(FlockRunState.Phase.LOST, run.phase());

        for (long seed = 0; seed < 100; seed++) {
            run = FlockRunStateTest.summit(seed, FlockRunState.Route.ELITE);
            if (run.boss() == FlockRunState.Boss.PELICAN) break;
        }
        assertEquals(FlockRunState.Boss.PELICAN, run.boss());
        run.chooseRoute(FlockRunState.Route.BOSS);
        game.harnessPrepareFlockRunEncounter(run, 5);
        assertEquals(1, game.getEffectiveTeam(1));
        assertTrue(game.players[1].health > 0);
        game.players[0].health = 0;
        new MatchController(game).checkForMatchCompletion();
        assertTrue(game.matchEnded);
        assertEquals(FlockRunState.Phase.LOST, run.phase());
        assertEquals(0, run.health());
    }

    @Test void allThreeRealBossesCompleteWithoutHangsAndSeededCombatRepeats() {
        EnumSet<FlockRunState.Boss> tested = EnumSet.noneOf(FlockRunState.Boss.class);
        for (long seed = 0; seed < 100 && tested.size() < 3; seed++) {
            FlockRunState run = FlockRunStateTest.summit(seed, FlockRunState.Route.ELITE);
            if (!tested.add(run.boss())) continue;
            run.chooseRoute(FlockRunState.Route.BOSS);
            String checkpoint = run.encode();
            BirdGame3 game = game();
            BirdGame3.ClassicEncounter encounter = game.harnessPrepareFlockRunEncounter(run, 5);
            assertTrue(encounter.bossFight);
            assertFalse(game.usesSmashCombatRules());
            playToEnd(game);
            String result = run.encode();
            long hash = game.harnessStateHash();
            FlockRunState repeat = FlockRunState.decode(checkpoint);
            game.harnessPrepareFlockRunEncounter(repeat, 5);
            playToEnd(game);
            assertEquals(result, repeat.encode(), encounter.name);
            assertEquals(hash, game.harnessStateHash(), encounter.name);
        }
        assertEquals(3, tested.size());
    }

    @Test void victoryCapturesHealthOnceAndOnlyUpdatesFlockMedalsInTheOwningProfile() throws Exception {
        FlockRunState run = FlockRunStateTest.summit(3, FlockRunState.Route.ELITE);
        run.chooseRoute(FlockRunState.Route.BOSS);
        BirdGame3 game = game();
        game.harnessPrepareFlockRunEncounter(run, 5);
        game.players[0].health = 43;
        new MatchController(game).triggerMatchEnd(game.players[0]);
        assertEquals(FlockRunState.Phase.WON, run.phase());
        assertEquals(43, run.health());
        String finalState = run.encode();
        game.players[0].health = 1;
        game.captureFlockRunOutcome(null);
        assertEquals(finalState, run.encode());
        assertEquals(0, root.keys().length, "The harness must not write progression");
        FlockRunProgress progress = new FlockRunProgress();
        progress.run = run;
        progress.recordCompletedRun();
        progress.recordCompletedRun();
        progress.save(root.node("a"));
        assertEquals(run.score(), FlockRunProgress.load(root.node("a")).bestScore());
        assertEquals(FlockRunState.Medal.GOLD, FlockRunProgress.load(root.node("a")).medal(run.bird));
        assertNull(FlockRunProgress.load(root.node("b")).run);
        assertEquals(FlockRunState.Medal.NONE, FlockRunProgress.load(root.node("b")).medal(run.bird));
        Method load = BirdGame3.class.getDeclaredMethod("loadProfileProgress", Preferences.class);
        Method save = BirdGame3.class.getDeclaredMethod("saveProfileProgress", Preferences.class);
        load.setAccessible(true); save.setAccessible(true);
        load.invoke(game, root.node("a"));
        save.invoke(game, root.node("copy"));
        assertEquals(finalState, FlockRunProgress.load(root.node("copy")).run.encode());
        load.invoke(game, root.node("b"));
        save.invoke(game, root.node("copy-b"));
        assertNull(FlockRunProgress.load(root.node("copy-b")).run);
    }

    private static void playToEnd(BirdGame3 game) {
        for (int i = 0; i < 18000 && !game.matchEnded; i++) game.harnessTick();
        assertTrue(game.matchEnded, "The encounter must reach victory, defeat, or its real timer");
    }
    private static double invoke(Bird bird, String name, Class<?>[] types, Object... args) throws Exception {
        Method method = Bird.class.getDeclaredMethod(name, types);
        method.setAccessible(true);
        Object result = method.invoke(bird, args);
        return result instanceof Number number ? number.doubleValue() : 0;
    }
}
