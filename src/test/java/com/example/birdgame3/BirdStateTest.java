package com.example.birdgame3;

import javafx.scene.input.KeyCode;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class BirdStateTest {
    @Test
    void temporarySizeChangesDamageAndKnockbackInBothDirections() throws Exception {
        BirdStats.resetToDefaults();

        SizeCombatOutcome normal = playSizeCombatExchange(1.0, 1.0);
        SizeCombatOutcome tinyAttacker = playSizeCombatExchange(0.6, 1.0);
        SizeCombatOutcome largeAttacker = playSizeCombatExchange(1.35, 1.0);
        SizeCombatOutcome tinyTarget = playSizeCombatExchange(1.0, 0.6);
        SizeCombatOutcome largeTarget = playSizeCombatExchange(1.0, 1.35);

        assertTrue(tinyAttacker.damage < normal.damage);
        assertTrue(tinyAttacker.launchSpeed < normal.launchSpeed);
        assertTrue(largeAttacker.damage > normal.damage);
        assertTrue(largeAttacker.launchSpeed > normal.launchSpeed);

        assertTrue(tinyTarget.damage > normal.damage);
        assertTrue(tinyTarget.launchSpeed > normal.launchSpeed);
        assertTrue(largeTarget.damage < normal.damage);
        assertTrue(largeTarget.launchSpeed < normal.launchSpeed);
    }

    @Test
    void naturalRosterSizeIsNeutralButAuthoredMiniAndGiantScalingIsNot() {
        BirdGame3 game = new BirdGame3();
        Bird pigeon = new Bird(100.0, BirdGame3.BirdType.PIGEON, 0, game);
        Bird pelican = new Bird(200.0, BirdGame3.BirdType.PELICAN, 1, game);
        Bird goose = new Bird(300.0, BirdGame3.BirdType.GOOSE, 2, game);

        assertEquals(1.0, pigeon.outgoingSizeDamageMultiplier(), 0.0001);
        assertEquals(1.0, pelican.outgoingSizeDamageMultiplier(), 0.0001);
        assertEquals(1.0, goose.outgoingSizeKnockbackMultiplier(), 0.0001);

        pigeon.setBaseMultipliers(0.68, 1.0, 1.0);
        pelican.setBaseMultipliers(1.20 * 1.58, 1.0, 1.0);
        assertTrue(pigeon.outgoingSizeDamageMultiplier() < 1.0);
        assertTrue(pelican.outgoingSizeKnockbackMultiplier() > 1.0);
    }

    @Test
    void kiwiWasAppendedWithoutChangingExistingReplayOrdinals() {
        assertEquals(20, BirdGame3.BirdType.GOOSE.ordinal());
        assertEquals(21, BirdGame3.BirdType.KIWI.ordinal());
    }

    @Test
    void kiwiDirectionalKitIsImmediateAndUsesIndependentReuseLocks() {
        BirdGame3 game = new BirdGame3();
        game.activePlayers = 1;
        Bird kiwi = new Bird(200.0, BirdGame3.BirdType.KIWI, 0, game);
        kiwi.y = BirdGame3.GROUND_Y - kiwi.bodyHeight();
        game.players[0] = kiwi;

        KiwiSpecials.use(kiwi, false);
        assertEquals(Bird.KIWI_PROBE_FRAMES, kiwi.kiwiProbeTimer);
        assertEquals(0, kiwi.specialCooldown);

        KiwiSpecials.reset(kiwi);
        game.setLocalActionsForKey(game.rightKeyForPlayer(0), true);
        KiwiSpecials.use(kiwi, false);
        assertEquals(Bird.KIWI_BURROW_FRAMES, kiwi.kiwiBurrowTimer);
        assertTrue(kiwi.vx > 10.0);

        KiwiSpecials.reset(kiwi);
        game.setLocalActionsForKey(game.rightKeyForPlayer(0), false);
        game.setLocalActionsForKey(game.jumpKeyForPlayer(0), true);
        KiwiSpecials.use(kiwi, false);
        assertEquals(Bird.KIWI_SPRING_FRAMES, kiwi.kiwiSpringTimer);
        assertTrue(kiwi.kiwiSpringUsed);
        assertTrue(kiwi.vy < -15.0);

        KiwiSpecials.reset(kiwi);
        game.setLocalActionsForKey(game.jumpKeyForPlayer(0), false);
        game.setLocalActionsForKey(game.blockKeyForPlayer(0), true);
        KiwiSpecials.use(kiwi, false);
        assertEquals(Bird.KIWI_STOMP_FRAMES, kiwi.kiwiStompTimer);
        assertFalse(kiwi.kiwiStompAirborne);
        assertEquals(0, kiwi.specialCooldown);
    }

    @Test
    void kiwiRapidProbeEndsWithAReadableLauncher() {
        BirdStats.resetToDefaults();
        BirdGame3 game = new BirdGame3();
        game.activePlayers = 2;
        Bird kiwi = new Bird(100.0, BirdGame3.BirdType.KIWI, 0, game);
        Bird target = new Bird(190.0, BirdGame3.BirdType.PIGEON, 1, game);
        kiwi.y = BirdGame3.GROUND_Y - kiwi.bodyHeight();
        target.y = BirdGame3.GROUND_Y - target.bodyHeight();
        game.players[0] = kiwi;
        game.players[1] = target;

        double healthBefore = target.health;
        KiwiSpecials.use(kiwi, false);
        for (int i = 0; i < Bird.KIWI_PROBE_FRAMES; i++) {
            KiwiSpecials.handleState(kiwi);
        }

        assertEquals(3, kiwi.kiwiProbeStrikeIndex);
        assertTrue(kiwi.kiwiProbeHit[target.playerIndex], "The finishing peck should connect.");
        assertEquals(13.0 * BirdGame3.BirdType.KIWI.defaultDamageDealtMult,
                healthBefore - target.health, 0.001,
                "All three forgiving pecks should deal their tuned total damage.");
        assertTrue(target.vx > 8.0, "The third probe should clearly launch forward.");
        assertTrue(target.vy < -3.0, "The third probe should pop the target upward.");
    }

    @Test
    void kiwiEarthStompControlsBothSidesWithoutASetupMeter() {
        BirdGame3 game = new BirdGame3();
        game.activePlayers = 3;
        Bird kiwi = new Bird(300.0, BirdGame3.BirdType.KIWI, 0, game);
        Bird left = new Bird(210.0, BirdGame3.BirdType.PIGEON, 1, game);
        Bird right = new Bird(390.0, BirdGame3.BirdType.EAGLE, 2, game);
        kiwi.y = BirdGame3.GROUND_Y - kiwi.bodyHeight();
        left.y = BirdGame3.GROUND_Y - left.bodyHeight();
        right.y = BirdGame3.GROUND_Y - right.bodyHeight();
        game.players[0] = kiwi;
        game.players[1] = left;
        game.players[2] = right;
        game.setLocalActionsForKey(game.blockKeyForPlayer(0), true);

        KiwiSpecials.use(kiwi, false);
        for (int i = 0; i < 10; i++) {
            KiwiSpecials.handleState(kiwi);
        }

        assertTrue(left.health < Bird.STARTING_HEALTH);
        assertTrue(right.health < Bird.STARTING_HEALTH);
        assertTrue(left.vx < 0.0);
        assertTrue(right.vx > 0.0);
        assertTrue(kiwi.kiwiStompImpactResolved, "The grounded version should visibly complete its pound.");
        assertTrue(kiwi.kiwiStompImpactFxTimer > 0, "The ground pound should leave a readable shockwave.");
        assertTrue(game.shakeIntensity >= 10.0, "The grounded impact should have physical weight.");
    }

    @Test
    void kiwiAirEarthStompPlungesAndStartsImpactFxWhenItLands() {
        BirdGame3 game = new BirdGame3();
        game.activePlayers = 1;
        Bird kiwi = new Bird(300.0, BirdGame3.BirdType.KIWI, 0, game);
        kiwi.y = BirdGame3.GROUND_Y - 280.0;
        game.players[0] = kiwi;
        game.setLocalActionsForKey(game.blockKeyForPlayer(0), true);

        KiwiSpecials.use(kiwi, false);
        assertTrue(kiwi.kiwiStompAirborne);
        KiwiSpecials.handleState(kiwi);
        assertTrue(kiwi.vy >= 13.8, "The airborne version should immediately commit to a fast plunge.");

        kiwi.y = BirdGame3.GROUND_Y - kiwi.bodyHeight();
        KiwiSpecials.handlePostMoveState(kiwi);

        assertTrue(kiwi.kiwiStompImpactResolved);
        assertEquals(Bird.KIWI_STOMP_IMPACT_FX_FRAMES, kiwi.kiwiStompImpactFxTimer,
                "A late landing should receive the full impact effect instead of an expired one.");
        assertTrue(game.shakeIntensity >= 15.0);
    }

    @Test
    void kiwiCanStartGroundEarthStompAfterShieldIsAlreadyRaised() {
        BirdGame3 game = new BirdGame3();
        game.activePlayers = 1;
        Bird kiwi = new Bird(300.0, BirdGame3.BirdType.KIWI, 0, game);
        kiwi.y = BirdGame3.GROUND_Y - kiwi.bodyHeight();
        game.players[0] = kiwi;

        game.setLocalActionsForKey(game.blockKeyForPlayer(0), true);
        kiwi.update(1.0);
        assertTrue(kiwi.isBlocking, "Setup should put grounded Kiwi into shield.");

        game.setLocalActionsForKey(game.specialKeyForPlayer(0), true);
        kiwi.update(1.0);

        assertFalse(kiwi.isBlocking, "Down special should drop Kiwi's existing shield.");
        assertTrue(kiwi.kiwiStompTimer > 0,
                "Pressing special while grounded and shielding should start Earth Stomp.");
        assertFalse(kiwi.kiwiStompAirborne);
    }

    @Test
    void kiwiUltimateIsOnePressMidnightStampede() {
        BirdGame3 game = new BirdGame3();
        game.activePlayers = 2;
        Bird kiwi = new Bird(300.0, BirdGame3.BirdType.KIWI, 0, game);
        Bird target = new Bird(430.0, BirdGame3.BirdType.PIGEON, 1, game);
        kiwi.y = BirdGame3.GROUND_Y - kiwi.bodyHeight();
        target.y = BirdGame3.GROUND_Y - target.bodyHeight();
        game.players[0] = kiwi;
        game.players[1] = target;
        kiwi.refillTrainingResources(true);

        BirdSpecialSystem.useSpecial(kiwi);
        assertEquals(Bird.KIWI_ULTIMATE_FRAMES, kiwi.kiwiUltimateTimer);
        assertFalse(kiwi.isUltimateReady());
        assertEquals(KiwiSpecials.MIDNIGHT_STAMPEDE_MOVE, game.lastTelemetryMoveName(0, ""));

        double healthBefore = target.health;
        for (int i = 0; i < 132; i++) {
            KiwiSpecials.handleState(kiwi);
        }
        assertTrue(target.health < healthBefore);
        assertTrue(Math.abs(target.vx) > 10.0 || target.vy < -8.0);
    }

    @Test
    void ultimateVisualReadyAllowsMockingbirdFallbackUlt() {
        BirdGame3 game = new BirdGame3();

        Bird pigeon = new Bird(100, BirdGame3.BirdType.PIGEON, 0, game);
        assertFalse(pigeon.isUltimateVisualReady());

        pigeon.refillTrainingResources(true);
        assertTrue(pigeon.isUltimateVisualReady());

        pigeon.health = 0;
        assertFalse(pigeon.isUltimateVisualReady());

        Bird mockingbird = new Bird(160, BirdGame3.BirdType.MOCKINGBIRD, 1, game);
        mockingbird.refillTrainingResources(true);
        assertTrue(mockingbird.isUltimateVisualReady(),
                "Mockingbird should glow without a captured neutral because Shadow Court falls back to the closest bird.");

        mockingbird.mockingbirdCapturedType = BirdGame3.BirdType.PIGEON;
        assertTrue(mockingbird.isUltimateVisualReady());
    }

    @Test
    void pigeonUltimateStartsSkywardSeedWaveInsteadOfBoostedSpecial() {
        BirdGame3 game = new BirdGame3();
        game.activePlayers = 1;

        Bird pigeon = new Bird(100, BirdGame3.BirdType.PIGEON, 0, game);
        game.players[0] = pigeon;
        pigeon.refillTrainingResources(true);

        BirdSpecialSystem.useSpecial(pigeon);

        assertTrue(pigeon.pigeonCoronationActive);
        assertEquals(Bird.PIGEON_SEED_WAVE_FRAMES, pigeon.pigeonCoronationTimer);
        assertTrue(pigeon.vy <= -17.5, "The ultimate should immediately launch Pigeon upward.");
        assertEquals(0, pigeon.pigeonFeatherBurstTimer);
        assertEquals(0, pigeon.pigeonRushTimer);
        assertEquals(0, pigeon.pigeonFlutterTimer);
        assertEquals(0, pigeon.pigeonScavengeTimer);
        assertFalse(pigeon.isUltimateReady());
    }

    @Test
    void fullUltimateMeterPreservesDirectionalSpecialsAndOnlyNeutralActivatesUltimate() {
        BirdGame3 pigeonGame = new BirdGame3();
        pigeonGame.activePlayers = 1;
        Bird pigeon = new Bird(100, BirdGame3.BirdType.PIGEON, 0, pigeonGame);
        pigeon.y = BirdGame3.GROUND_Y - pigeon.bodyHeight();
        pigeonGame.players[0] = pigeon;
        pigeon.refillTrainingResources(true);
        pigeonGame.setLocalActionsForKey(pigeonGame.rightKeyForPlayer(0), true);

        BirdSpecialSystem.useSpecial(pigeon);

        assertTrue(pigeon.isUltimateReady(), "Side special must preserve a full ultimate meter.");
        assertTrue(pigeon.pigeonRushTimer > 0, "Side input should still perform Street Rush.");
        assertFalse(pigeon.pigeonCoronationActive, "Side special must not activate Pigeon's ultimate.");

        BirdGame3 eagleGame = new BirdGame3();
        eagleGame.activePlayers = 1;
        Bird eagle = new Bird(100, BirdGame3.BirdType.EAGLE, 0, eagleGame);
        eagle.y = BirdGame3.GROUND_Y - eagle.bodyHeight();
        eagleGame.players[0] = eagle;
        eagle.refillTrainingResources(true);
        eagleGame.setLocalActionsForKey(eagleGame.jumpKeyForPlayer(0), true);

        BirdSpecialSystem.useSpecial(eagle);

        assertTrue(eagle.isUltimateReady(), "Up special must preserve a full ultimate meter.");
        assertTrue(eagle.raptorClimbTimer > 0, "Up input should still perform Eagle's normal recovery.");
        assertFalse(eagle.eagleSkySovereignActive, "Up special must not activate Eagle's ultimate.");

        BirdGame3 penguinGame = new BirdGame3();
        penguinGame.activePlayers = 1;
        Bird penguin = new Bird(100, BirdGame3.BirdType.PENGUIN, 0, penguinGame);
        penguin.y = BirdGame3.GROUND_Y - penguin.bodyHeight();
        penguinGame.players[0] = penguin;
        penguin.refillTrainingResources(true);
        penguinGame.setLocalActionsForKey(penguinGame.blockKeyForPlayer(0), true);

        BirdSpecialSystem.useSpecial(penguin);

        assertTrue(penguin.isUltimateReady(), "Down special must preserve a full ultimate meter.");
        assertNotNull(penguin.penguinSnowFort, "Down input should still build a normal Snow Fort.");
        assertFalse(penguin.penguinSnowFort.ultimate);
        assertEquals(0, penguin.penguinAbsoluteZeroTimer,
                "Down special must not activate Penguin's ultimate.");

        Bird.PenguinSnowFort existingFort = penguin.penguinSnowFort;
        BirdSpecialSystem.useSpecial(penguin);
        assertSame(existingFort, penguin.penguinSnowFort,
                "A full meter must not bypass the normal down-special reuse restriction.");
        assertTrue(penguin.isUltimateReady());
    }

    @Test
    void pigeonSkywardSeedWaveAscendsAndHitsDistantTargetsOnce() {
        BirdGame3 game = new BirdGame3();
        game.activePlayers = 3;

        Bird pigeon = new Bird(100, BirdGame3.BirdType.PIGEON, 0, game);
        Bird target = new Bird(960, BirdGame3.BirdType.EAGLE, 1, game);
        Bird fragileTarget = new Bird(300, BirdGame3.BirdType.FALCON, 2, game);
        pigeon.y = BirdGame3.GROUND_Y - 80.0;
        target.y = BirdGame3.GROUND_Y - 80.0;
        fragileTarget.y = BirdGame3.GROUND_Y - 80.0;
        fragileTarget.health = 5.0;
        game.players[0] = pigeon;
        game.players[1] = target;
        game.players[2] = fragileTarget;
        pigeon.refillTrainingResources(true);

        BirdSpecialSystem.useSpecial(pigeon);
        double startingHealth = target.health;
        double startingY = pigeon.y;

        for (int i = 0; i < 8; i++) {
            pigeon.update(1.0);
        }
        assertTrue(pigeon.y < startingY - 40.0, "Pigeon should ascend with the expanding seed wave.");
        assertEquals(startingHealth, target.health, 0.001,
                "A distant target should wait for the expanding front rather than being hit instantly.");

        for (int i = 8; i <= Bird.PIGEON_SEED_WAVE_FRAMES; i++) {
            pigeon.update(1.0);
        }

        assertFalse(pigeon.pigeonCoronationActive);
        assertEquals(0, pigeon.pigeonCoronationTimer);
        assertEquals(Bird.PIGEON_SEED_WAVE_DAMAGE, startingHealth - target.health, 0.001,
                "Each opponent should be damaged exactly once as the wave reaches them.");
        assertEquals(0.0, fragileTarget.health, 0.001,
                "The no-freeze impact path must also preserve lethal seed-wave hits.");
        assertTrue(target.vy < -0.1 || Math.abs(target.vx) > 0.1);
        assertEquals(0, game.hitstopFrames,
                "The expanding seed wave must never pause the whole match when it reaches a target.");
    }

    @Test
    void pigeonSeedWaveRenderAvoidsFullScreenPixelEffects() throws Exception {
        String source = Files.readString(Path.of(
                "src", "main", "java", "com", "example", "birdgame3", "Bird.java"));
        int waveStart = source.indexOf("private void drawPigeonSeedWave");
        int waveEnd = source.indexOf("private void drawEagleSkySovereignReticle", waveStart);

        assertTrue(waveStart >= 0 && waveEnd > waveStart);
        String waveRender = source.substring(waveStart, waveEnd);
        assertFalse(waveRender.contains("setEffect("),
                "A full-screen JavaFX pixel effect can freeze the render thread during Pigeon's ultimate.");
    }

    @Test
    void eagleUltimateStartsSkySovereignInsteadOfBoostedRaptorSpecial() {
        BirdGame3 game = new BirdGame3();
        game.activePlayers = 2;

        Bird eagle = new Bird(100, BirdGame3.BirdType.EAGLE, 0, game);
        Bird target = new Bird(190, BirdGame3.BirdType.PIGEON, 1, game);
        eagle.y = BirdGame3.GROUND_Y - 80.0;
        target.y = BirdGame3.GROUND_Y - 80.0;
        game.players[0] = eagle;
        game.players[1] = target;
        eagle.refillTrainingResources(true);

        BirdSpecialSystem.useSpecial(eagle);

        assertTrue(eagle.eagleSkySovereignActive);
        assertFalse(eagle.eagleSkySovereignDiving);
        assertEquals(Bird.EAGLE_SKY_SOVEREIGN_TARGET_FRAMES, eagle.eagleSkySovereignTimer);
        assertEquals(0, eagle.raptorCryTimer);
        assertEquals(0, eagle.raptorRushTimer);
        assertEquals(0, eagle.raptorClimbTimer);
        assertFalse(eagle.eagleDiveActive);
        assertFalse(eagle.isUltimateReady());
    }

    @Test
    void eagleSkySovereignImpactsTargetZoneAfterTargeting() {
        BirdGame3 game = new BirdGame3();
        game.activePlayers = 2;

        Bird eagle = new Bird(100, BirdGame3.BirdType.EAGLE, 0, game);
        Bird target = new Bird(190, BirdGame3.BirdType.PIGEON, 1, game);
        eagle.y = BirdGame3.GROUND_Y - 80.0;
        target.y = BirdGame3.GROUND_Y - 80.0;
        game.players[0] = eagle;
        game.players[1] = target;
        eagle.refillTrainingResources(true);

        BirdSpecialSystem.useSpecial(eagle);
        double startingHealth = target.health;

        for (int i = 0; i < Bird.EAGLE_SKY_SOVEREIGN_TARGET_FRAMES
                + Bird.EAGLE_SKY_SOVEREIGN_DIVE_FRAMES + 6; i++) {
            eagle.update(1.0);
        }

        assertFalse(eagle.eagleSkySovereignActive);
        assertTrue(target.health <= startingHealth - Bird.EAGLE_SKY_SOVEREIGN_DAMAGE);
        assertTrue(target.vy < -1.0 || Math.abs(target.vx) > 1.0);
        assertFalse(eagle.isUltimateReady());
    }

    @Test
    void falconUltimateStartsTerminalVelocityInsteadOfBoostedRaptorSpecial() {
        BirdGame3 game = new BirdGame3();
        game.activePlayers = 2;

        Bird falcon = new Bird(100, BirdGame3.BirdType.FALCON, 0, game);
        Bird target = new Bird(240, BirdGame3.BirdType.PIGEON, 1, game);
        falcon.y = BirdGame3.GROUND_Y - 80.0;
        target.y = BirdGame3.GROUND_Y - 80.0;
        falcon.facingRight = true;
        game.players[0] = falcon;
        game.players[1] = target;
        falcon.refillTrainingResources(true);

        BirdSpecialSystem.useSpecial(falcon);

        assertTrue(falcon.falconTerminalVelocityActive);
        assertFalse(falcon.falconTerminalVelocityStriking);
        assertEquals(Bird.FALCON_TERMINAL_VELOCITY_WARNING_FRAMES, falcon.falconTerminalVelocityTimer);
        assertEquals(0, falcon.raptorCryTimer);
        assertEquals(0, falcon.raptorRushTimer);
        assertEquals(0, falcon.raptorClimbTimer);
        assertFalse(falcon.eagleDiveActive);
        assertFalse(falcon.isUltimateReady());
    }

    @Test
    void falconTerminalVelocitySweetspotsMarkedTarget() {
        BirdGame3 game = new BirdGame3();
        game.activePlayers = 2;

        Bird falcon = new Bird(100, BirdGame3.BirdType.FALCON, 0, game);
        Bird target = new Bird(260, BirdGame3.BirdType.PIGEON, 1, game);
        falcon.y = BirdGame3.GROUND_Y - 80.0;
        target.y = BirdGame3.GROUND_Y - 80.0;
        falcon.facingRight = true;
        game.players[0] = falcon;
        game.players[1] = target;
        falcon.refillTrainingResources(true);

        BirdSpecialSystem.useSpecial(falcon);
        double startingHealth = target.health;

        for (int i = 0; i < Bird.FALCON_TERMINAL_VELOCITY_WARNING_FRAMES
                + Bird.FALCON_TERMINAL_VELOCITY_STRIKE_FRAMES + 4; i++) {
            falcon.update(1.0);
        }

        assertFalse(falcon.falconTerminalVelocityActive);
        assertTrue(target.health <= startingHealth - Bird.FALCON_TERMINAL_VELOCITY_SWEETSPOT_DAMAGE);
        assertTrue(target.vx > 10.0);
        assertTrue(target.vy < -8.0);
    }

    @Test
    void defeatedBirdCancelsLingeringFrenzyWithoutReviving() throws Exception {
        BirdGame3 game = new BirdGame3();
        game.activePlayers = 2;

        Bird hummingbird = new Bird(100, BirdGame3.BirdType.HUMMINGBIRD, 0, game);
        Bird target = new Bird(150, BirdGame3.BirdType.PIGEON, 1, game);
        game.players[0] = hummingbird;
        game.players[1] = target;

        setPrivateInt(hummingbird, "hummingFrenzyTimer", 90);
        hummingbird.health = 0;

        hummingbird.update(1.0);

        assertEquals(0.0, hummingbird.health, 0.0001);
        assertEquals(Bird.STARTING_HEALTH, target.health, 0.0001);
        assertEquals(0, getPrivateInt(hummingbird, "hummingFrenzyTimer"));
    }

    @Test
    void hummingbirdUltimateStartsNeedleheartOverdrive() throws Exception {
        BirdGame3 game = new BirdGame3();
        game.activePlayers = 2;

        Bird hummingbird = new Bird(180.0, BirdGame3.BirdType.HUMMINGBIRD, 0, game);
        Bird target = new Bird(300.0, BirdGame3.BirdType.PIGEON, 1, game);
        hummingbird.y = BirdGame3.GROUND_Y - 120.0;
        target.y = BirdGame3.GROUND_Y - 120.0;
        game.players[0] = hummingbird;
        game.players[1] = target;
        hummingbird.refillTrainingResources(true);

        BirdSpecialSystem.useSpecial(hummingbird);
        double startingHealth = target.health;

        assertEquals(Bird.HUMMING_NEEDLEHEART_TOTAL_FRAMES, getPrivateInt(hummingbird, "hummingFrenzyTimer"));
        assertEquals(0, getPrivateInt(hummingbird, "hummingNeedleHitTimer"));
        assertFalse(hummingbird.isUltimateReady());

        for (int i = 0; i < Bird.HUMMING_NEEDLEHEART_FINAL_FRAME + 12; i++) {
            hummingbird.update(1.0);
        }

        assertTrue(target.health < startingHealth);
        assertTrue(Math.abs(target.vx) > 8.0);
        assertTrue(target.vy < -8.0);
        assertTrue(getPrivateInt(target, "hummingNectarCoatedTimer") > 0);
    }

    @Test
    void hummingbirdReuseLockoutsStayInvisible() throws Exception {
        BirdGame3 game = new BirdGame3();
        game.activePlayers = 1;

        Bird hummingbird = new Bird(180.0, BirdGame3.BirdType.HUMMINGBIRD, 0, game);
        hummingbird.y = BirdGame3.GROUND_Y - 80.0;
        game.players[0] = hummingbird;

        KeyCode specialKey = game.specialKeyForPlayer(0);
        game.setLocalActionsForKey(specialKey, true);
        hummingbird.update(1.0);
        game.setLocalActionsForKey(specialKey, false);
        hummingbird.update(1.0);

        assertEquals(0, hummingbird.specialCooldown);
        assertTrue(getPrivateInt(hummingbird, "hummingNeedleReuseTimer") > 0);

        game.setLocalActionsForKey(specialKey, true);
        hummingbird.update(1.0);

        assertEquals(0, hummingbird.specialCooldown,
                "Hummingbird specials should use invisible per-move reuse gates.");
    }

    @Test
    void hummingbirdUpSpecialIsOnlyAnUpwardBurst() throws Exception {
        BirdGame3 game = new BirdGame3();
        game.activePlayers = 2;

        Bird hummingbird = new Bird(220.0, BirdGame3.BirdType.HUMMINGBIRD, 0, game);
        Bird target = new Bird(224.0, BirdGame3.BirdType.PIGEON, 1, game);
        hummingbird.y = BirdGame3.GROUND_Y - 80.0;
        target.y = BirdGame3.GROUND_Y - 80.0;
        game.players[0] = hummingbird;
        game.players[1] = target;

        double startingHealth = target.health;
        invokePrivateBooleanVoid(hummingbird, "specialHummingbirdHoverBurst", false);
        hummingbird.update(1.0);

        assertEquals(0, hummingbird.specialCooldown);
        assertTrue(hummingbird.vy < -20.0,
                "Hover Burst should be an extreme vertical launch.");
        assertEquals(startingHealth, target.health, 0.0001,
                "Hover Burst should not deal damage.");
    }

    @Test
    void hummingbirdNectarTrapCoatsTargetsAfterTheyLeaveTheFlower() throws Exception {
        BirdGame3 game = new BirdGame3();
        game.activePlayers = 2;

        Bird hummingbird = new Bird(300.0, BirdGame3.BirdType.HUMMINGBIRD, 0, game);
        Bird target = new Bird(242.0, BirdGame3.BirdType.PIGEON, 1, game);
        hummingbird.y = BirdGame3.GROUND_Y - 80.0;
        target.y = BirdGame3.GROUND_Y - 80.0;
        hummingbird.facingRight = true;
        game.players[0] = hummingbird;
        game.players[1] = target;

        invokePrivateBooleanVoid(hummingbird, "specialHummingbirdNectarTrap", false);
        for (int i = 0; i < 60; i++) {
            hummingbird.update(1.0);
        }

        assertEquals(0, hummingbird.specialCooldown);
        assertTrue(getPrivateInt(target, "hummingNectarCoatedTimer") > 0,
                "Stepping into the flower should coat the target in nectar.");
        assertTrue(getPrivateInt(target, "hummingNectarCoatedTimer") <= 100,
                "Nectar coating should use the nerfed shorter poison duration.");

        double healthAfterFlower = target.health;
        target.x += 260.0;
        target.vx = 9.0;
        for (int i = 0; i < 3; i++) {
            target.update(1.0);
        }

        assertTrue(getPrivateInt(target, "hummingNectarCoatedTimer") > 0,
                "Nectar should remain on the target briefly after leaving the flower.");
        assertTrue(target.health < healthAfterFlower,
                "The visible nectar coating should keep dealing damage after the target exits the trap.");
    }

    @Test
    void hummingbirdNectarCoatingOnlyAppliesPoisonDamage() throws Exception {
        BirdGame3 game = new BirdGame3();
        game.activePlayers = 2;

        Bird hummingbird = new Bird(300.0, BirdGame3.BirdType.HUMMINGBIRD, 0, game);
        Bird target = new Bird(360.0, BirdGame3.BirdType.PIGEON, 1, game);
        game.players[0] = hummingbird;
        game.players[1] = target;

        setPrivateInt(target, "hummingNectarCoatedTimer", 60);
        setPrivateInt(target, "hummingNectarCoatedDamageCooldown", 8);
        target.vx = 9.0;
        target.vy = 4.0;

        invokePrivateVoid(target, "handleHummingbirdNectarCoating");

        assertEquals(9.0, target.vx, 0.0001,
                "Hummingbird nectar should not slow horizontal movement.");
        assertEquals(4.0, target.vy, 0.0001,
                "Hummingbird nectar should not slow vertical movement.");
    }

    @Test
    void turkeyNeutralSpecialUsesInvisibleReuseTimer() throws Exception {
        BirdGame3 game = new BirdGame3();
        game.activePlayers = 1;

        Bird turkey = new Bird(180.0, BirdGame3.BirdType.TURKEY, 0, game);
        turkey.y = BirdGame3.GROUND_Y - 80.0;
        game.players[0] = turkey;

        KeyCode specialKey = game.specialKeyForPlayer(0);
        game.setLocalActionsForKey(specialKey, true);
        turkey.update(1.0);
        game.setLocalActionsForKey(specialKey, false);
        turkey.update(1.0);

        assertEquals(0, turkey.specialCooldown);
        assertTrue(getPrivateInt(turkey, "turkeyGobbleReuseTimer") > 0);

        game.setLocalActionsForKey(specialKey, true);
        turkey.update(1.0);

        assertEquals(0, turkey.specialCooldown,
                "Turkey's 4-special kit should use invisible per-move reuse timers.");
    }

    @Test
    void turkeyNeutralSpecialChargesBeforeAttackingOnRelease() throws Exception {
        BirdGame3 game = new BirdGame3();
        game.activePlayers = 2;

        Bird turkey = new Bird(180.0, BirdGame3.BirdType.TURKEY, 0, game);
        Bird target = new Bird(244.0, BirdGame3.BirdType.PIGEON, 1, game);
        turkey.y = BirdGame3.GROUND_Y - 80.0;
        target.y = BirdGame3.GROUND_Y - 80.0;
        game.players[0] = turkey;
        game.players[1] = target;

        KeyCode specialKey = game.specialKeyForPlayer(0);
        game.setLocalActionsForKey(specialKey, true);
        for (int i = 0; i < 48; i++) {
            turkey.update(1.0);
        }

        double targetHealthBeforeRelease = target.health;
        assertTrue(getPrivateBoolean(turkey, "turkeyGobbleCharging"),
                "Holding neutral special should charge Gobble Guard instead of attacking immediately.");
        assertEquals(0, getPrivateInt(turkey, "turkeyGobbleTimer"),
                "The Gobble Guard hitbox should not come out before release.");
        assertTrue(getPrivateInt(turkey, "turkeyGobbleHoldTimer") >= 40,
                "Gobble Guard should track charge duration.");
        assertEquals(targetHealthBeforeRelease, target.health, 0.0001,
                "Charging neutral special should not damage nearby targets.");

        game.setLocalActionsForKey(specialKey, false);
        turkey.update(1.0);

        assertFalse(getPrivateBoolean(turkey, "turkeyGobbleCharging"));
        assertTrue(getPrivateInt(turkey, "turkeyGobbleTimer") > 0,
                "Releasing neutral special should start the charged attack.");
        assertTrue(target.health < targetHealthBeforeRelease,
                "The charged neutral special should hit after release.");
        assertTrue(target.vx > 14.0,
                "The charged neutral special should launch with much stronger knockback.");
        assertTrue(getPrivateInt(turkey, "turkeyGobbleReuseTimer") <= 30,
                "Turkey neutral should use only a short immediate reuse lockout.");
    }

    @Test
    void turkeySideSpecialStaysActiveWhileHeldWithShortReuse() throws Exception {
        BirdGame3 game = new BirdGame3();
        game.activePlayers = 2;

        Bird turkey = new Bird(300.0, BirdGame3.BirdType.TURKEY, 0, game);
        Bird target = new Bird(382.0, BirdGame3.BirdType.PIGEON, 1, game);
        turkey.y = BirdGame3.GROUND_Y - 80.0;
        target.y = BirdGame3.GROUND_Y - 80.0;
        game.players[0] = turkey;
        game.players[1] = target;

        game.setLocalActionsForKey(game.rightKeyForPlayer(0), true);
        game.setLocalActionsForKey(game.specialKeyForPlayer(0), true);
        for (int i = 0; i < 36; i++) {
            turkey.update(1.0);
        }

        assertTrue(getPrivateInt(turkey, "turkeyStampedeTimer") > 0,
                "Side special should stay active while special is held.");
        assertTrue(getPrivateInt(turkey, "turkeyStampedeHoldFrames") >= 30,
                "Side special should track the held shove duration.");
        assertTrue(target.vx > 10.0,
                "Held side special should shove targets away with strong knockback.");
        assertTrue(getPrivateInt(turkey, "turkeyStampedeReuseTimer") <= 28,
                "Turkey side special should use only a short immediate reuse lockout.");

        game.setLocalActionsForKey(game.specialKeyForPlayer(0), false);
        turkey.update(1.0);

        assertEquals(0, getPrivateInt(turkey, "turkeyStampedeTimer"),
                "Releasing special should end the held side special.");
    }

    @Test
    void turkeyPanicFlapRecoversUpwardAndOnlyHitsBelow() throws Exception {
        BirdGame3 game = new BirdGame3();
        game.activePlayers = 3;

        Bird turkey = new Bird(260.0, BirdGame3.BirdType.TURKEY, 0, game);
        Bird belowTarget = new Bird(262.0, BirdGame3.BirdType.PIGEON, 1, game);
        Bird sideTarget = new Bird(430.0, BirdGame3.BirdType.PIGEON, 2, game);
        turkey.y = BirdGame3.GROUND_Y - 280.0;
        belowTarget.y = BirdGame3.GROUND_Y - 145.0;
        sideTarget.y = BirdGame3.GROUND_Y - 145.0;
        game.players[0] = turkey;
        game.players[1] = belowTarget;
        game.players[2] = sideTarget;

        double belowStart = belowTarget.health;
        double sideStart = sideTarget.health;
        invokePrivateBooleanVoid(turkey, "specialTurkeyPanicFlap", false);
        turkey.update(1.0);

        assertEquals(0, turkey.specialCooldown);
        assertEquals(0, getPrivateInt(turkey, "turkeyPanicFlapReuseTimer"),
                "Panic Flap should not use a time-based cooldown.");
        assertTrue(getPrivateBoolean(turkey, "turkeyPanicFlapUsed"),
                "Panic Flap should be locked only by the once-per-airtime flag.");
        assertTrue(turkey.vy < -10.0,
                "Panic Flap should launch Turkey upward as a recovery.");
        assertTrue(belowTarget.health < belowStart,
                "Panic Flap should lightly damage enemies directly below Turkey.");
        assertEquals(sideStart, sideTarget.health, 0.0001,
                "Panic Flap should not be a wide side hitbox.");
        assertTrue(belowTarget.vy > 0.0,
                "The wing blast should push caught enemies downward.");

        setPrivateInt(turkey, "turkeyPanicFlapTimer", 0);
        turkey.vy = 0.0;
        invokePrivateBooleanVoid(turkey, "specialTurkeyPanicFlap", false);
        assertEquals(0, getPrivateInt(turkey, "turkeyPanicFlapTimer"),
                "Panic Flap should not restart before Turkey lands.");

        turkey.y = BirdGame3.GROUND_Y - 10.0;
        turkey.update(1.0);
        assertFalse(getPrivateBoolean(turkey, "turkeyPanicFlapUsed"),
                "Landing should refresh Turkey's up special.");
    }

    @Test
    void turkeyFeastTrapSlowsWithoutDamageOrFullStun() throws Exception {
        BirdGame3 game = new BirdGame3();
        game.activePlayers = 2;

        Bird turkey = new Bird(300.0, BirdGame3.BirdType.TURKEY, 0, game);
        Bird target = new Bird(258.0, BirdGame3.BirdType.PIGEON, 1, game);
        turkey.y = BirdGame3.GROUND_Y - 80.0;
        target.y = BirdGame3.GROUND_Y - 80.0;
        turkey.facingRight = true;
        game.players[0] = turkey;
        game.players[1] = target;

        invokePrivateBooleanVoid(turkey, "specialTurkeyFeastTrap", false);
        double targetHealthBeforeTrap = target.health;
        turkey.update(1.0);

        assertEquals(0, turkey.specialCooldown);
        assertTrue(getPrivateInt(turkey, "turkeyFeastTrapReuseTimer") <= 42,
                "Turkey down special should use only a short immediate reuse lockout.");
        assertTrue(getPrivateInt(target, "turkeyStuffedTimer") > 0,
                "Stepping into Feast Trap should apply the stuffed debuff.");
        assertTrue(getPrivateInt(target, "turkeyStuffedTimer") <= 110,
                "Stuffing should use the nerfed shorter slow duration.");
        assertEquals(targetHealthBeforeTrap, target.health, 0.0001,
                "Turkey stuffing should not deal damage when applied.");

        target.vx = 12.0;
        target.vy = -6.0;
        invokePrivateVoid(target, "handleTurkeyStuffedEffect");
        assertTrue(target.vx > 8.0 && target.vx < 12.0,
                "Stuffed birds should be lightly slowed, not heavily crippled or frozen.");
        assertTrue(target.vy < 0.0 && target.vy > -6.0,
                "Stuffing should slow vertical movement without fully stopping it.");
        assertEquals(0.0, target.stunTime, 0.0001,
                "Stuffing should not stun the target.");

        target.x = 390.0;
        target.vx = 0.0;
        target.vy = 0.0;
        game.setLocalActionsForKey(game.rightKeyForPlayer(0), true);
        game.setLocalActionsForKey(game.specialKeyForPlayer(0), true);
        turkey.update(1.0);

        assertEquals(0, getPrivateInt(target, "turkeyStuffedTimer"),
                "Turkey's next hit should consume the stuffed debuff.");
        assertTrue(target.vx > 22.0,
                "Stuffed targets should take extra knockback from Turkey's next hit.");
    }

    @Test
    void turkeyUltimateSummonsHarvestTribunalAndVerdictSlash() throws Exception {
        BirdGame3 game = new BirdGame3();
        game.activePlayers = 2;

        Bird turkey = new Bird(280.0, BirdGame3.BirdType.TURKEY, 0, game);
        Bird target = new Bird(420.0, BirdGame3.BirdType.PIGEON, 1, game);
        turkey.y = BirdGame3.GROUND_Y - 80.0;
        target.y = BirdGame3.GROUND_Y - 80.0;
        game.players[0] = turkey;
        game.players[1] = target;

        turkey.refillTrainingResources(true);
        double targetHealthBefore = target.health;

        BirdSpecialSystem.useSpecial(turkey);

        assertEquals(Bird.TURKEY_HARVEST_TRIBUNAL_FRAMES,
                getPrivateInt(turkey, "turkeyHarvestTribunalTimer"),
                "Turkey ultimate should start the dedicated Harvest Tribunal state.");
        assertFalse(turkey.isUltimateReady(),
                "Starting Harvest Tribunal should consume the ultimate meter.");
        assertEquals(0, turkey.specialCooldown,
                "Harvest Tribunal should not leave a visible generic special cooldown.");
        assertEquals(0, getPrivateInt(turkey, "turkeyGobbleTimer"),
                "Turkey ultimate should not fall through into the boosted neutral special.");

        for (int i = 0; i < 54; i++) {
            turkey.update(1.0);
        }

        assertTrue(getPrivateInt(target, "turkeyStuffedTimer") > 0,
                "The tribunal pull should apply Turkey's stuffed debuff before the final hit.");
        assertEquals(targetHealthBefore, target.health, 0.0001,
                "The pull/setup phase should control space without dealing damage early.");

        for (int i = 54; i < Bird.TURKEY_HARVEST_TRIBUNAL_FINAL_FRAME + 2; i++) {
            turkey.update(1.0);
        }

        assertTrue(target.health < targetHealthBefore,
                "The verdict slash should damage a target caught at the tribunal table.");
        assertTrue(getPrivateBoolean(turkey, "turkeyHarvestTribunalFinalResolved"),
                "Harvest Tribunal should resolve exactly one final verdict.");
        assertEquals(0, getPrivateInt(target, "turkeyStuffedTimer"),
                "The verdict slash should consume the stuffed debuff.");
        assertTrue(target.vx > 12.0 || target.vy < -8.0,
                "The verdict slash should launch caught targets.");
    }

    @Test
    void opiumAndHeisenNeutralSpecialsUseInvisibleReuseTimers() throws Exception {
        BirdGame3 game = new BirdGame3();
        game.activePlayers = 2;

        Bird opium = new Bird(180.0, BirdGame3.BirdType.OPIUMBIRD, 0, game);
        Bird heisen = new Bird(320.0, BirdGame3.BirdType.HEISENBIRD, 1, game);
        opium.y = BirdGame3.GROUND_Y - 80.0;
        heisen.y = BirdGame3.GROUND_Y - 80.0;
        game.players[0] = opium;
        game.players[1] = heisen;

        invokePrivateBooleanVoid(opium, "specialOpiumNeutral", false);
        invokePrivateBooleanVoid(heisen, "specialHeisenNeutral", false);

        assertEquals(0, opium.specialCooldown);
        assertEquals(0, heisen.specialCooldown);
        assertTrue(getPrivateInt(opium, "opiumNeutralReuseTimer") > 0);
        assertTrue(getPrivateInt(heisen, "opiumNeutralReuseTimer") > 0);
        assertTrue(getPrivateDouble(opium, "opiumResourceMeter") < 100.0);
        assertTrue(getPrivateDouble(heisen, "opiumResourceMeter") < 100.0);

        game.setLocalActionsForKey(game.specialKeyForPlayer(0), true);
        opium.update(1.0);
    }

    @Test
    void heisenCrystalCloudMarksTargetsBrittleAndNextHitConsumesIt() throws Exception {
        BirdGame3 game = new BirdGame3();
        game.activePlayers = 2;

        Bird heisen = new Bird(260.0, BirdGame3.BirdType.HEISENBIRD, 0, game);
        Bird target = new Bird(330.0, BirdGame3.BirdType.PIGEON, 1, game);
        heisen.y = BirdGame3.GROUND_Y - 80.0;
        target.y = BirdGame3.GROUND_Y - 80.0;
        game.players[0] = heisen;
        game.players[1] = target;

        invokePrivateBooleanVoid(heisen, "specialHeisenNeutral", false);
        heisen.update(1.0);

        assertTrue(getPrivateInt(target, "heisenBrittleTimer") > 0,
                "Crystal Cloud should visibly mark nearby enemies as brittle.");

        int particlesBeforeShatter = game.particles.size();
        double healthBeforeHit = target.health;
        double dealt = applyPrivateDamage(heisen, target, 6.0);

        assertTrue(dealt > 6.0,
                "Heisenbird's next hit should gain bonus damage against a brittle target.");
        assertTrue(target.health < healthBeforeHit);
        assertEquals(0, getPrivateInt(target, "heisenBrittleTimer"),
                "A normal brittle mark should be consumed by the next Heisenbird hit.");
        assertTrue(game.hitstopFrames >= 5,
                "Shattering Brittle should have a distinct but brief crystal impact pause.");
        assertTrue(game.shakeIntensity >= 8);
        assertTrue(game.particles.size() > particlesBeforeShatter,
                "A Brittle shatter should release a readable burst of crystal shards.");
    }

    @Test
    void heisenFueledBlueRushGetsItsBrittleLaunchPayoff() {
        BirdGame3 game = new BirdGame3();
        game.activePlayers = 3;

        Bird heisen = new Bird(300.0, BirdGame3.BirdType.HEISENBIRD, 0, game);
        Bird brittleTarget = new Bird(390.0, BirdGame3.BirdType.PIGEON, 1, game);
        Bird unmarkedTarget = new Bird(430.0, BirdGame3.BirdType.EAGLE, 2, game);
        heisen.y = BirdGame3.GROUND_Y - heisen.bodyHeight();
        brittleTarget.y = BirdGame3.GROUND_Y - brittleTarget.bodyHeight();
        unmarkedTarget.y = BirdGame3.GROUND_Y - unmarkedTarget.bodyHeight();
        heisen.facingRight = true;
        game.players[0] = heisen;
        game.players[1] = brittleTarget;
        game.players[2] = unmarkedTarget;
        brittleTarget.applyHeisenBrittle(heisen, false);

        OpiumSpecials.side(heisen, true);
        double committedSpeed = Math.abs(heisen.vx);
        int committedFrames = heisen.opiumSideTimer;
        OpiumSpecials.applySideHits(heisen, true);

        assertTrue(heisen.opiumSideHit[brittleTarget.playerIndex]);
        assertTrue(heisen.opiumSideHit[unmarkedTarget.playerIndex]);
        assertTrue(brittleTarget.health < unmarkedTarget.health,
                "Brittle should add its promised damage to Blue Rush.");
        assertTrue(Math.abs(brittleTarget.vx) > Math.abs(unmarkedTarget.vx),
                "Fueled Blue Rush should cash Brittle out into its stronger launch route.");
        assertEquals(0, brittleTarget.heisenBrittleTimer);
        assertEquals(committedFrames, heisen.opiumSideTimer,
                "The shatter polish must not shorten Blue Rush recovery.");
        assertEquals(committedSpeed, Math.abs(heisen.vx), 0.0001,
                "The shatter polish must not alter Blue Rush movement.");
        assertTrue(game.hitstopFrames >= 5);
        assertTrue(game.shakeIntensity >= 8);
    }

    @Test
    void opiumAndHeisenMetersGateEffectsAndRefillFromDownSpecials() throws Exception {
        BirdGame3 game = new BirdGame3();
        game.activePlayers = 2;

        Bird opium = new Bird(220.0, BirdGame3.BirdType.OPIUMBIRD, 0, game);
        Bird heisen = new Bird(380.0, BirdGame3.BirdType.HEISENBIRD, 1, game);
        opium.y = heisen.y = BirdGame3.GROUND_Y - 80.0;
        opium.facingRight = true;
        heisen.facingRight = true;
        game.players[0] = opium;
        game.players[1] = heisen;

        setPrivateDouble(opium, "opiumResourceMeter", 0.0);
        invokePrivateBooleanVoid(opium, "specialOpiumSide", false);
        assertFalse(getPrivateBoolean(opium, "opiumSideFueled"),
                "Empty Opium side special should keep the dash but not create a lean trail.");
        invokePrivateBooleanVoid(opium, "specialOpiumUp", false);
        assertFalse(getPrivateBoolean(opium, "opiumUpFueled"),
                "Empty Opium up special should not create the lean plume.");

        setPrivateDouble(heisen, "opiumResourceMeter", 0.0);
        invokePrivateBooleanVoid(heisen, "specialHeisenNeutral", false);
        heisen.update(1.0);
        assertEquals(0, getPrivateInt(opium, "heisenBrittleTimer"),
                "Empty Heisen neutral should not apply useful brittle pressure.");

        setPrivateDouble(opium, "opiumResourceMeter", 0.0);
        setPrivateInt(opium, "opiumSideTimer", 0);
        setPrivateInt(opium, "opiumUpTimer", 0);
        opium.vx = 0.0;
        opium.vy = 0.0;
        invokePrivateBooleanVoid(opium, "specialOpiumDown", false);
        for (int i = 0; i < 4; i++) {
            opium.update(1.0);
        }
        assertTrue(getPrivateDouble(opium, "opiumResourceMeter") > 0.0,
                "Standing in Opium Bird's puddle should refill his opium meter.");
        assertEquals(0.48, getPrivateDouble(opium, "opiumResourceMeter"), 0.0001,
                "Opium Bird's puddle should refill slowly enough to prevent quick farming.");

        setPrivateDouble(heisen, "opiumResourceMeter", 0.0);
        invokePrivateBooleanVoid(heisen, "specialOpiumDown", true);
        heisen.update(1.0);
        assertTrue(getPrivateDouble(heisen, "opiumResourceMeter") > 0.0,
                "Standing in Heisenbird's crystal should refill his crystal meter.");
        assertEquals(0.10, getPrivateDouble(heisen, "opiumResourceMeter"), 0.0001,
                "Heisenbird's crystal should refill slowly enough to prevent quick farming.");
        List<?> crystals = (List<?>) getPrivateObject(heisen, "opiumTraps");
        assertFalse(crystals.isEmpty());
        assertTrue(getPrivateInt(crystals.getFirst(), "lifeFrames") > 500,
                "Heisenbird's refill crystal should stay long enough to be used intentionally.");
    }

    @Test
    void lotusPatchSlowsTargetsAndRefreshesLeanCloud() throws Exception {
        BirdGame3 game = new BirdGame3();
        game.activePlayers = 2;

        Bird opium = new Bird(300.0, BirdGame3.BirdType.OPIUMBIRD, 0, game);
        Bird target = new Bird(292.0, BirdGame3.BirdType.PIGEON, 1, game);
        opium.y = BirdGame3.GROUND_Y - 80.0;
        target.y = BirdGame3.GROUND_Y - 80.0;
        opium.facingRight = true;
        target.vx = 10.0;
        game.players[0] = opium;
        game.players[1] = target;

        setPrivateInt(opium, "leanTimer", 5);
        invokePrivateBooleanVoid(opium, "specialOpiumDown", false);
        opium.update(1.0);

        assertEquals(0, opium.specialCooldown);
        assertTrue(getPrivateInt(opium, "opiumDownReuseTimer") > 0);
        assertTrue(target.vx < 10.0,
                "Lotus Patch should slow enemies standing inside it.");
        assertTrue(opium.leanTimer >= 72,
                "Enemies standing in Lotus Patch should refresh the active Lean Cloud.");
    }

    @Test
    void opiumFueledHazeDriftRefundsOnceAndBrakesOnContact() {
        BirdGame3 game = new BirdGame3();
        game.activePlayers = 3;

        Bird opium = new Bird(300.0, BirdGame3.BirdType.OPIUMBIRD, 0, game);
        Bird firstTarget = new Bird(390.0, BirdGame3.BirdType.PIGEON, 1, game);
        Bird secondTarget = new Bird(430.0, BirdGame3.BirdType.EAGLE, 2, game);
        opium.y = BirdGame3.GROUND_Y - opium.bodyHeight();
        firstTarget.y = BirdGame3.GROUND_Y - firstTarget.bodyHeight();
        secondTarget.y = BirdGame3.GROUND_Y - secondTarget.bodyHeight();
        opium.facingRight = true;
        game.players[0] = opium;
        game.players[1] = firstTarget;
        game.players[2] = secondTarget;

        OpiumSpecials.side(opium, false);
        double committedSpeed = Math.abs(opium.vx);
        assertEquals(Bird.OPIUM_RESOURCE_MAX - Bird.OPIUM_SIDE_RESOURCE_COST,
                opium.opiumResourceMeter, 0.0001);

        OpiumSpecials.applySideHits(opium, false);

        assertTrue(opium.opiumSideHit[firstTarget.playerIndex]);
        assertTrue(opium.opiumSideHit[secondTarget.playerIndex]);
        assertEquals(Bird.OPIUM_RESOURCE_MAX - Bird.OPIUM_SIDE_RESOURCE_COST + Bird.OPIUM_SIDE_HIT_REFUND,
                opium.opiumResourceMeter, 0.0001,
                "A multi-target Haze Drift should refund meter only once per use.");
        assertEquals(Bird.OPIUM_SIDE_HIT_RECOVERY_FRAMES, opium.opiumSideTimer);
        assertTrue(Math.abs(opium.vx) < committedSpeed * 0.7,
                "Haze Drift should brake on its first confirmed hit instead of carrying Opium Bird past the route.");
        assertTrue(game.hitstopFrames >= 4,
                "A fueled Haze Drift confirm should have restrained but readable impact pause.");

        OpiumSpecials.applySideHits(opium, false);
        assertEquals(Bird.OPIUM_RESOURCE_MAX - Bird.OPIUM_SIDE_RESOURCE_COST + Bird.OPIUM_SIDE_HIT_REFUND,
                opium.opiumResourceMeter, 0.0001,
                "The same targets must not repeatedly refund meter during one Haze Drift.");
    }

    @Test
    void opiumFueledRisingVaporsRefundsMeterOnFirstHit() {
        BirdGame3 game = new BirdGame3();
        game.activePlayers = 2;

        Bird opium = new Bird(300.0, BirdGame3.BirdType.OPIUMBIRD, 0, game);
        Bird target = new Bird(320.0, BirdGame3.BirdType.PIGEON, 1, game);
        opium.y = BirdGame3.GROUND_Y - 260.0;
        target.y = BirdGame3.GROUND_Y - target.bodyHeight();
        game.players[0] = opium;
        game.players[1] = target;

        OpiumSpecials.up(opium, false);
        assertEquals(Bird.OPIUM_RESOURCE_MAX - Bird.OPIUM_UP_RESOURCE_COST,
                opium.opiumResourceMeter, 0.0001);

        OpiumSpecials.applyUpHits(opium, false);

        assertTrue(opium.opiumUpHit[target.playerIndex]);
        assertEquals(Bird.OPIUM_RESOURCE_MAX - Bird.OPIUM_UP_RESOURCE_COST + Bird.OPIUM_UP_HIT_REFUND,
                opium.opiumResourceMeter, 0.0001,
                "Landing fueled Rising Vapors should return a smaller amount of its resource cost.");
        assertTrue(game.hitstopFrames >= 4);
    }

    @Test
    void opiumUltimateAppliesDrowsyAndHeisenUltimateLaunchesHomingCrystalShards() throws Exception {
        BirdGame3 game = new BirdGame3();
        game.activePlayers = 4;

        Bird opium = new Bird(200.0, BirdGame3.BirdType.OPIUMBIRD, 0, game);
        Bird opiumTarget = new Bird(260.0, BirdGame3.BirdType.PIGEON, 1, game);
        Bird heisen = new Bird(560.0, BirdGame3.BirdType.HEISENBIRD, 2, game);
        Bird heisenTarget = new Bird(664.0, BirdGame3.BirdType.EAGLE, 3, game);
        opium.y = opiumTarget.y = heisen.y = heisenTarget.y = BirdGame3.GROUND_Y - 80.0;
        game.players[0] = opium;
        game.players[1] = opiumTarget;
        game.players[2] = heisen;
        game.players[3] = heisenTarget;

        setPrivateDouble(opium, "opiumResourceMeter", 0.0);
        setPrivateDouble(opium, "ultimateMeter", 100.0);
        invokePrivateVoid(opium, "special");
        opium.update(1.0);

        assertTrue(getPrivateInt(opium, "opiumUltimateTimer") > 0,
                "Opium Bird should spend ultimate meter on Purple Haze instead of a buffed normal special.");
        assertEquals(100.0, getPrivateDouble(opium, "opiumResourceMeter"), 0.0001,
                "Opium Bird's ultimate should refill his opium meter.");
        assertTrue(getPrivateInt(opiumTarget, "opiumDrowsyTimer") > 0,
                "Purple Haze should apply a readable drowsy debuff to enemies in range.");
        assertTrue(getPrivateDouble(opium, "opiumUltimateCloudX") > 0.0,
                "Purple Haze should spawn a persistent lean cloud in the arena.");

        setPrivateDouble(heisen, "opiumResourceMeter", 0.0);
        setPrivateDouble(heisen, "ultimateMeter", 100.0);
        invokePrivateVoid(heisen, "special");
        assertTrue(getPrivateInt(heisen, "heisenUltimateTimer") > 0);
        assertEquals(100.0, getPrivateDouble(heisen, "opiumResourceMeter"), 0.0001,
                "Heisenbird's ultimate should refill his crystal meter.");
        assertTrue(getPrivateInt(heisenTarget, "heisenBrittleTimer") > 0,
                "Say My Name should immediately mark nearby enemies brittle.");

        double healthBeforeOrbit = heisenTarget.health;
        for (int i = 0; i < 70; i++) {
            heisen.update(1.0);
        }
        assertTrue(heisenTarget.health < healthBeforeOrbit,
                "Orbiting crystals should damage enemies that stay in their path before they launch.");

        double healthBeforeVolley = heisenTarget.health;
        setPrivateInt(heisen, "heisenUltimateTimer", 1);
        heisen.update(1.0);
        assertTrue(getPrivateInt(heisen, "heisenUltimateVolleyTimer") > 0,
                "When Heisenbird's ultimate ends, the orbiting crystals should launch as a staggered visible volley.");
        for (int i = 0; i < 130 && heisenTarget.health >= healthBeforeVolley; i++) {
            heisen.update(1.0);
        }

        assertTrue(heisenTarget.health < healthBeforeVolley,
                "A launched crystal shard should hone toward and damage the nearest enemy.");
        boolean[] spentShards = (boolean[]) getPrivateObject(heisen, "heisenUltimateShardSpent");
        boolean anyShardSpent = false;
        for (boolean spent : spentShards) {
            anyShardSpent |= spent;
        }
        assertTrue(anyShardSpent,
                "A shard that hits or times out should leave the active volley instead of lingering forever.");
        assertEquals(0, getPrivateInt(heisenTarget, "heisenBrittleTimer"));
    }

    @Test
    void roosterSpawnsWithThreeFollowerChicks() {
        BirdGame3 game = new BirdGame3();
        game.activePlayers = 1;

        Bird rooster = new Bird(220.0, BirdGame3.BirdType.ROOSTER, 0, game);
        rooster.y = BirdGame3.GROUND_Y - 80.0;
        game.players[0] = rooster;

        rooster.update(1.0);

        List<ChickMinion> chicks = ownedChicks(game, rooster);
        boolean[] variants = new boolean[3];
        assertEquals(3, chicks.size(), "Rooster should spawn in with one chick of each type.");
        for (ChickMinion chick : chicks) {
            variants[chick.variant] = true;
            assertTrue(chick.followingOwner, "Starting chicks should follow Rooster in formation.");
            assertNull(chick.target, "Follower chicks should not be in fighting mode yet.");
        }
        assertArrayEquals(new boolean[]{true, true, true}, variants);
    }

    @Test
    void roosterNeutralAddsFollowerChicksUpToFiveWithoutVisibleCooldown() throws Exception {
        BirdGame3 game = new BirdGame3();
        game.activePlayers = 1;

        Bird rooster = new Bird(220.0, BirdGame3.BirdType.ROOSTER, 0, game);
        rooster.y = BirdGame3.GROUND_Y - 80.0;
        game.players[0] = rooster;
        rooster.update(1.0);

        invokePrivateVoid(rooster, "special");
        assertEquals(4, ownedChicks(game, rooster).size());
        assertEquals(0, rooster.specialCooldown);
        assertTrue(getPrivateInt(rooster, "roosterNeutralReuseTimer") > 0);

        setPrivateInt(rooster, "roosterNeutralReuseTimer", 0);
        invokePrivateVoid(rooster, "special");

        List<ChickMinion> chicks = ownedChicks(game, rooster);
        assertEquals(5, chicks.size(), "Neutral should fill Rooster's brood to the five-chick cap.");
        assertTrue(chicks.stream().allMatch(chick -> chick.followingOwner));
        assertEquals(0, rooster.specialCooldown,
                "Rooster's four-special kit should not show a cooldown bar.");
    }

    @Test
    void roosterSideThrowsNextFollowerIntoFightingMode() throws Exception {
        BirdGame3 game = new BirdGame3();
        game.activePlayers = 2;

        Bird rooster = new Bird(240.0, BirdGame3.BirdType.ROOSTER, 0, game);
        Bird target = new Bird(470.0, BirdGame3.BirdType.PIGEON, 1, game);
        rooster.y = BirdGame3.GROUND_Y - 80.0;
        target.y = BirdGame3.GROUND_Y - 80.0;
        game.players[0] = rooster;
        game.players[1] = target;
        rooster.update(1.0);

        game.setLocalActionsForKey(game.rightKeyForPlayer(0), true);
        invokePrivateVoid(rooster, "special");
        game.setLocalActionsForKey(game.rightKeyForPlayer(0), false);

        List<ChickMinion> chicks = ownedChicks(game, rooster);
        List<ChickMinion> fightingChicks = chicks.stream()
                .filter(chick -> !chick.followingOwner)
                .toList();
        assertEquals(1, fightingChicks.size(),
                "Side special should throw exactly the next follower into independent fighting mode.");
        ChickMinion thrown = fightingChicks.getFirst();
        assertSame(target, thrown.target);
        assertTrue(thrown.vx > 18.0, "Thrown chicks should launch fast enough to read as a toss.");
        assertTrue(thrown.thrownFrames > 0, "Thrown chicks should carry throw streak animation frames.");
        assertEquals(2, chicks.stream().filter(chick -> chick.followingOwner).count());
        assertEquals(0, rooster.specialCooldown);
        assertTrue(getPrivateInt(rooster, "roosterSideReuseTimer") > 0);
    }

    @Test
    void roosterUpBoostThrowsChicksUpAndOnlyUsesOncePerAirtime() throws Exception {
        BirdGame3 game = new BirdGame3();
        game.activePlayers = 1;

        Bird rooster = new Bird(260.0, BirdGame3.BirdType.ROOSTER, 0, game);
        rooster.y = BirdGame3.GROUND_Y - 220.0;
        game.players[0] = rooster;
        rooster.update(1.0);
        rooster.y = BirdGame3.GROUND_Y - 220.0;
        rooster.vy = 0.0;

        game.setLocalActionsForKey(game.jumpKeyForPlayer(0), true);
        invokePrivateVoid(rooster, "special");

        assertTrue(rooster.vy < -20.0, "Up special should be a strong chick-assisted vertical boost.");
        assertTrue(getPrivateBoolean(rooster, "roosterUpSpecialUsed"),
                "Rooster's up special should be locked by a once-per-airtime flag.");
        assertEquals(0, rooster.specialCooldown);
        assertTrue(ownedChicks(game, rooster).stream().allMatch(chick -> chick.followingOwner && chick.boostSparkFrames > 0),
                "Boosting should pull the brood into the launch animation.");

        rooster.vy = 0.0;
        invokePrivateVoid(rooster, "special");
        assertEquals(0.0, rooster.vy, 0.0001,
                "Rooster should not get another up special until he lands.");
        game.setLocalActionsForKey(game.jumpKeyForPlayer(0), false);
    }

    @Test
    void roosterDownRecallsFightingChicksBackToFormation() throws Exception {
        BirdGame3 game = new BirdGame3();
        game.activePlayers = 2;

        Bird rooster = new Bird(300.0, BirdGame3.BirdType.ROOSTER, 0, game);
        Bird target = new Bird(620.0, BirdGame3.BirdType.PIGEON, 1, game);
        rooster.y = BirdGame3.GROUND_Y - 80.0;
        target.y = BirdGame3.GROUND_Y - 80.0;
        game.players[0] = rooster;
        game.players[1] = target;
        rooster.update(1.0);

        for (int i = 0; i < game.chickMinions.size(); i++) {
            ChickMinion chick = game.chickMinions.get(i);
            chick.followingOwner = false;
            chick.target = target;
            chick.x = 740.0 + i * 45.0;
            chick.y = BirdGame3.GROUND_Y - 160.0;
        }

        game.setLocalActionsForKey(game.blockKeyForPlayer(0), true);
        invokePrivateVoid(rooster, "special");
        game.setLocalActionsForKey(game.blockKeyForPlayer(0), false);

        for (ChickMinion chick : ownedChicks(game, rooster)) {
            assertTrue(chick.followingOwner, "Down special should recall chicks from fighting mode.");
            assertNull(chick.target);
            assertTrue(Math.abs((chick.x + chick.width * 0.5) - (rooster.x + 40.0 * rooster.sizeMultiplier)) < 130.0,
                    "Recalled chicks should snap back near Rooster.");
            assertTrue(chick.commandFlashFrames > 0);
        }
        assertEquals(0, rooster.specialCooldown);
        assertTrue(getPrivateInt(rooster, "roosterDownReuseTimer") > 0);
    }

    @Test
    void roosterUltimateSummonsFlyingDawnStampede() throws Exception {
        BirdGame3 game = new BirdGame3();
        game.activePlayers = 2;

        Bird rooster = new Bird(300.0, BirdGame3.BirdType.ROOSTER, 0, game);
        Bird target = new Bird(620.0, BirdGame3.BirdType.PIGEON, 1, game);
        rooster.y = BirdGame3.GROUND_Y - 80.0;
        target.y = BirdGame3.GROUND_Y - 80.0;
        game.players[0] = rooster;
        game.players[1] = target;
        rooster.update(1.0);

        setPrivateDouble(rooster, "ultimateMeter", 100.0);
        invokePrivateVoid(rooster, "special");

        List<ChickMinion> swarm = ownedChicks(game, rooster).stream()
                .filter(chick -> chick.roosterSwarm)
                .toList();
        assertEquals(18, swarm.size(),
                "Rooster ultimate should create a large real swarm while the draw layer adds visual copies.");
        assertTrue(swarm.stream().allMatch(chick -> !chick.followingOwner),
                "Stampede chicks should immediately leave formation.");
        assertTrue(swarm.stream().allMatch(chick -> chick.target == target),
                "Stampede chicks should launch toward the nearest enemy.");
        assertTrue(swarm.stream().allMatch(chick -> chick.speed >= 14.0 && !chick.onGround),
                "Stampede chicks should be fast flying attackers.");
        assertTrue(swarm.stream().allMatch(chick -> chick.swarmHitsRemaining == 2 && chick.swarmVisualCopies >= 4),
                "Stampede chicks should have capped hits and extra render copies.");
        assertEquals(0.0, getPrivateDouble(rooster, "ultimateMeter"), 0.0001);
        assertEquals(0, rooster.specialCooldown);
        assertEquals(5, getPrivateInt(rooster, "roosterCommandFxKind"));
        assertEquals(RoosterSpecials.DAWN_STAMPEDE_MOVE, game.lastTelemetryMoveName(0, ""));
    }

    @Test
    void roosterAiRecallsAndReusesItsDeployedChicks() {
        BirdGame3 game = new BirdGame3();
        game.activePlayers = 2;

        Bird rooster = new Bird(220.0, BirdGame3.BirdType.ROOSTER, 0, game);
        Bird target = new Bird(540.0, BirdGame3.BirdType.PIGEON, 1, game);
        rooster.y = BirdGame3.GROUND_Y - 80.0;
        target.y = BirdGame3.GROUND_Y - 80.0;
        game.players[0] = rooster;
        game.players[1] = target;
        rooster.update(1.0);
        RoosterSpecials.neutral(rooster, false);
        RoosterSpecials.neutral(rooster, false);

        for (ChickMinion chick : ownedChicks(game, rooster)) {
            chick.followingOwner = false;
            chick.target = target;
        }
        double distance = Math.hypot(
                target.bodyCenterX() - rooster.bodyCenterX(),
                target.bodyCenterY() - rooster.bodyCenterY());

        assertFalse(rooster.shouldRoosterAIUseSpecial(target, distance, false, 0.0),
                "Rooster's CPU should leave deployed chicks alone while they are close enough to pressure the opponent.");
        assertEquals(Bird.DirectionalSpecialInput.NEUTRAL,
                rooster.chooseRoosterAISpecialInput(target, distance, true, false));

        for (ChickMinion chick : ownedChicks(game, rooster)) {
            chick.x = -700.0;
        }
        assertTrue(rooster.shouldRoosterAIUseSpecial(target, distance, false, 0.0),
                "Rooster's CPU must keep considering specials after every chick has been deployed.");
        assertEquals(Bird.DirectionalSpecialInput.DOWN,
                rooster.chooseRoosterAISpecialInput(target, distance, true, false),
                "A separated CPU Rooster should recall its fighting chicks instead of abandoning the command loop.");

        RoosterSpecials.down(rooster, false);

        assertTrue(ownedChicks(game, rooster).stream().allMatch(chick -> chick.followingOwner),
                "Recall should restore every deployed chick to formation.");
        assertEquals(Bird.DirectionalSpecialInput.SIDE,
                rooster.chooseRoosterAISpecialInput(target, distance, true, false),
                "After recalling, CPU Rooster should be able to redeploy the flock.");
    }

    @Test
    void titmouseAiHoldsDownSpecialToDetonateAFullSeedRoute() {
        BirdGame3 game = new BirdGame3();
        game.activePlayers = 2;

        Bird titmouse = new Bird(180.0, BirdGame3.BirdType.TITMOUSE, 0, game);
        Bird target = new Bird(900.0, BirdGame3.BirdType.PIGEON, 1, game);
        titmouse.y = BirdGame3.GROUND_Y - 80.0;
        target.y = BirdGame3.GROUND_Y - 80.0;
        game.players[0] = titmouse;
        game.players[1] = target;
        for (int i = 0; i < Bird.TITMOUSE_MAX_STASHES - 1; i++) {
            titmouse.titmouseSeedStashes.add(new Bird.TitmouseSeedStash(
                    target.bodyCenterX() + i * 8.0,
                    BirdGame3.GROUND_Y,
                    false));
        }
        titmouse.titmouseStashCharging = true;
        assertFalse(titmouse.maintainAIHeldSpecialInputs(),
                "A partial route should remain a quick placement tap instead of detonating early.");
        titmouse.titmouseStashCharging = false;
        titmouse.titmouseSeedStashes.clear();

        for (int i = 0; i < Bird.TITMOUSE_MAX_STASHES; i++) {
            titmouse.titmouseSeedStashes.add(new Bird.TitmouseSeedStash(
                    target.bodyCenterX() + i * 8.0,
                    BirdGame3.GROUND_Y,
                    false));
        }
        double distance = Math.hypot(
                target.bodyCenterX() - titmouse.bodyCenterX(),
                target.bodyCenterY() - titmouse.bodyCenterY());

        assertTrue(titmouse.shouldTitmouseAIUseSpecial(target, distance, true, false),
                "CPU Titmouse should consider a remote detonation even when the opponent is outside its normal attack range.");
        assertEquals(Bird.DirectionalSpecialInput.DOWN,
                titmouse.chooseTitmouseAISpecialInput(target, distance, true, false, false),
                "A target inside a full stash route should make CPU Titmouse choose the detonation input.");

        TitmouseSpecials.down(titmouse);
        double healthBefore = target.health;
        for (int frame = 0; frame < Bird.TITMOUSE_STASH_HOLD_FRAMES; frame++) {
            assertTrue(titmouse.maintainAIHeldSpecialInputs(),
                    "CPU Titmouse must keep holding Special and Down until the route detonates.");
            TitmouseSpecials.handleState(titmouse);
        }

        assertFalse(titmouse.titmouseStashCharging);
        assertTrue(titmouse.titmouseSeedStashes.isEmpty(),
                "A completed CPU hold should detonate and consume the armed route.");
        assertTrue(target.health < healthBefore,
                "The CPU-triggered route detonation should damage an opponent inside its blast radius.");
    }

    @Test
    void titmouseBarkskipBrakesAfterItsFirstConfirmedHit() {
        BirdGame3 game = new BirdGame3();
        game.activePlayers = 2;

        Bird titmouse = new Bird(300.0, BirdGame3.BirdType.TITMOUSE, 0, game);
        Bird target = new Bird(360.0, BirdGame3.BirdType.PIGEON, 1, game);
        titmouse.y = BirdGame3.GROUND_Y - titmouse.bodyHeight();
        target.y = BirdGame3.GROUND_Y - target.bodyHeight();
        titmouse.facingRight = true;
        game.players[0] = titmouse;
        game.players[1] = target;

        TitmouseSpecials.side(titmouse);
        double committedSpeed = Math.abs(titmouse.vx);
        TitmouseSpecials.handleBarkskip(titmouse);

        assertTrue(titmouse.titmouseBarkskipHit[target.playerIndex]);
        assertEquals(Bird.TITMOUSE_BARKSKIP_HIT_RECOVERY_FRAMES, titmouse.titmouseBarkskipTimer,
                "A confirmed Barkskip should move into its short recovery instead of crossing the whole screen.");
        assertEquals(Bird.TITMOUSE_BARKSKIP_HIT_SPEED, Math.abs(titmouse.vx), 0.0001);
        assertTrue(Math.abs(titmouse.vx) < committedSpeed * 0.5,
                "Barkskip should visibly brake on contact so Titmouse can continue the route nearby.");
        assertTrue(game.hitstopFrames >= 3);
    }

    @Test
    void titmouseMarkedTuftVaultGetsItsPromisedRoutePayoff() {
        BirdGame3 game = new BirdGame3();
        game.activePlayers = 3;

        Bird titmouse = new Bird(300.0, BirdGame3.BirdType.TITMOUSE, 0, game);
        Bird markedTarget = new Bird(320.0, BirdGame3.BirdType.PIGEON, 1, game);
        Bird unmarkedTarget = new Bird(355.0, BirdGame3.BirdType.PIGEON, 2, game);
        titmouse.y = BirdGame3.GROUND_Y - titmouse.bodyHeight();
        markedTarget.y = BirdGame3.GROUND_Y - markedTarget.bodyHeight();
        unmarkedTarget.y = markedTarget.y;
        game.players[0] = titmouse;
        game.players[1] = markedTarget;
        game.players[2] = unmarkedTarget;
        markedTarget.applyTitmouseMark(titmouse, false);

        TitmouseSpecials.up(titmouse);
        TitmouseSpecials.handleTuftVault(titmouse);

        assertTrue(titmouse.titmouseVaultHit[markedTarget.playerIndex]);
        assertTrue(titmouse.titmouseVaultHit[unmarkedTarget.playerIndex]);
        assertTrue(markedTarget.health < unmarkedTarget.health,
                "MARK must improve Tuft Vault instead of being ignored by the advertised route follow-up.");
        assertTrue(Math.abs(markedTarget.vx) > Math.abs(unmarkedTarget.vx));
        assertTrue(Math.abs(markedTarget.vy) > Math.abs(unmarkedTarget.vy));
        assertTrue(game.hitstopFrames >= 5,
                "The marked Vault payoff should have a readable impact pause.");
        assertTrue(game.shakeIntensity >= 8);
    }

    @Test
    void pelicanAiKeepsCommittedSpecialsOverSafeLandingLanes() {
        BirdGame3 game = new BirdGame3();
        game.harnessPrepareMatch(
                BirdGame3.BirdType.PELICAN,
                BirdGame3.BirdType.PIGEON,
                0x5AFE1ADEL,
                BirdGame3.MapType.BATTLEFIELD);
        Bird pelican = game.players[0];
        Bird target = game.players[1];
        Platform mainStage = null;
        for (Platform platform : game.platforms) {
            if (mainStage == null || platform.w > mainStage.w) {
                mainStage = platform;
            }
        }
        assertNotNull(mainStage);

        pelican.x = mainStage.x + mainStage.w * 0.5 - pelican.bodyWidth() * 0.5;
        pelican.y = mainStage.y - 360.0;
        pelican.vx = 0.0;
        target.x = pelican.x + 180.0;
        target.y = pelican.y - 170.0;
        double distance = pelican.combatDistanceTo(target);
        assertEquals(Bird.DirectionalSpecialInput.UP,
                pelican.choosePelicanAISpecialInput(target, distance, false, true, false, false),
                "Thermal Sail should remain available when its forced Keel Dive has solid ground below.");

        pelican.x = mainStage.x - 520.0;
        target.x = pelican.x + 180.0;
        distance = pelican.combatDistanceTo(target);
        assertNotEquals(Bird.DirectionalSpecialInput.UP,
                pelican.choosePelicanAISpecialInput(target, distance, false, true, false, false),
                "CPU Pelican must not start a forced Keel Dive over the lower blast zone.");

        pelican.pelicanCargoCount = 2;
        pelican.y = mainStage.y - pelican.bodyHeight();
        pelican.x = mainStage.x + mainStage.w * 0.5 - pelican.bodyWidth() * 0.5;
        target.x = pelican.x + 280.0;
        target.y = pelican.y;
        distance = pelican.combatDistanceTo(target);
        assertEquals(Bird.DirectionalSpecialInput.SIDE,
                pelican.choosePelicanAISpecialInput(target, distance, true, false, false, false),
                "Breakwater Run should remain available when its full route stays above the island.");

        pelican.x = mainStage.x + mainStage.w - pelican.bodyWidth() - 30.0;
        target.x = pelican.x + 280.0;
        distance = pelican.combatDistanceTo(target);
        assertNotEquals(Bird.DirectionalSpecialInput.SIDE,
                pelican.choosePelicanAISpecialInput(target, distance, true, false, false, false),
                "CPU Pelican must not spend cargo on a Breakwater Run that carries him beyond every landing lane.");
    }

    @Test
    void heisenbirdAiWaitsForCrystalMeterInsteadOfSpendingScraps() {
        BirdGame3 game = new BirdGame3();
        game.activePlayers = 2;
        Bird heisenbird = new Bird(240.0, BirdGame3.BirdType.HEISENBIRD, 0, game);
        Bird target = new Bird(440.0, BirdGame3.BirdType.PIGEON, 1, game);
        heisenbird.y = target.y = BirdGame3.GROUND_Y - 80.0;
        game.players[0] = heisenbird;
        game.players[1] = target;
        double distance = heisenbird.combatDistanceTo(target);

        assertEquals(Bird.DirectionalSpecialInput.SIDE,
                heisenbird.chooseHeisenbirdAISpecialInput(distance, true, false, false),
                "CPU Heisenbird should retain its established Blue Rush priority with a funded crystal meter.");

        heisenbird.opiumResourceMeter = Bird.HEISEN_SIDE_RESOURCE_COST - 1.0;
        heisenbird.opiumDownReuseTimer = 20;
        assertEquals(Bird.DirectionalSpecialInput.DOWN,
                heisenbird.chooseHeisenbirdAISpecialInput(distance, true, false, false),
                "A depleted CPU Heisenbird should wait for its refill node instead of powering Blue Rush with scraps.");

        heisenbird.opiumResourceMeter = Bird.HEISEN_SIDE_RESOURCE_COST + 1.0;
        assertEquals(Bird.DirectionalSpecialInput.SIDE,
                heisenbird.chooseHeisenbirdAISpecialInput(distance, true, false, false),
                "A funded Blue Rush should remain available while the refill node cools down.");

        heisenbird.opiumUpSpecialUsed = false;
        heisenbird.opiumResourceMeter = 0.0;
        assertEquals(Bird.DirectionalSpecialInput.UP,
                heisenbird.chooseHeisenbirdAISpecialInput(distance, false, true, false),
                "Crystal Column must remain available as an unfueled recovery when the meter is empty.");
    }

    @Test
    void ravenAiChargesBlackQuillOnlyAtSafeZoningRange() throws Exception {
        BirdGame3 game = new BirdGame3();
        game.activePlayers = 2;
        Bird raven = new Bird(240.0, BirdGame3.BirdType.RAVEN, 0, game);
        Bird distantTarget = new Bird(680.0, BirdGame3.BirdType.PIGEON, 1, game);
        raven.y = distantTarget.y = BirdGame3.GROUND_Y - 80.0;
        game.players[0] = raven;
        game.players[1] = distantTarget;
        game.isAI[0] = true;
        setPrivateInt(raven, "aiLockedTargetIndex", 1);

        raven.specialRavenBlackQuill(false);
        assertTrue(raven.shouldRavenAIChargeBlackQuill(),
                "A distant inactive target should give CPU Raven room to charge the fan.");
        for (int frame = 0; frame < Bird.RAVEN_QUILL_CHARGE_FAN_FRAMES; frame++) {
            raven.update(1.0);
        }

        assertTrue(raven.ravenQuillCharging,
                "CPU Raven should keep holding Black Quill through its authored fan threshold.");
        assertEquals(Bird.RAVEN_QUILL_CHARGE_FAN_FRAMES, raven.ravenQuillChargeFrames);

        raven.update(1.0);

        assertFalse(raven.ravenQuillCharging,
                "CPU Raven should release as soon as the three-quill fan is ready.");
        List<?> quills = (List<?>) getPrivateObject(raven, "ravenQuills");
        assertEquals(3, quills.size(),
                "The completed CPU charge should fire Raven's full three-projectile fan.");

        BirdGame3 pressureGame = new BirdGame3();
        pressureGame.activePlayers = 2;
        Bird pressuredRaven = new Bird(240.0, BirdGame3.BirdType.RAVEN, 0, pressureGame);
        Bird closeTarget = new Bird(350.0, BirdGame3.BirdType.PIGEON, 1, pressureGame);
        pressuredRaven.y = closeTarget.y = BirdGame3.GROUND_Y - 80.0;
        pressureGame.players[0] = pressuredRaven;
        pressureGame.players[1] = closeTarget;
        pressureGame.isAI[0] = true;
        setPrivateInt(pressuredRaven, "aiLockedTargetIndex", 1);

        pressuredRaven.specialRavenBlackQuill(false);
        assertFalse(pressuredRaven.shouldRavenAIChargeBlackQuill(),
                "CPU Raven should not stand still charging while an opponent is already in close range.");
        pressuredRaven.update(1.0);

        assertFalse(pressuredRaven.ravenQuillCharging);
        List<?> quickQuills = (List<?>) getPrivateObject(pressuredRaven, "ravenQuills");
        assertEquals(1, quickQuills.size(),
                "Under close pressure CPU Raven should release the fast single-quill shot.");
    }

    @Test
    void gooseAiChargesHonkOnlyAtSafeZoningRange() throws Exception {
        BirdGame3 game = new BirdGame3();
        game.activePlayers = 2;
        Bird goose = new Bird(240.0, BirdGame3.BirdType.GOOSE, 0, game);
        Bird distantTarget = new Bird(680.0, BirdGame3.BirdType.PIGEON, 1, game);
        goose.y = distantTarget.y = BirdGame3.GROUND_Y - 80.0;
        game.players[0] = goose;
        game.players[1] = distantTarget;
        game.isAI[0] = true;
        setPrivateInt(goose, "aiLockedTargetIndex", 1);

        GooseSpecials.neutral(goose, false);
        assertTrue(goose.shouldGooseAIChargeHonk(),
                "A distant inactive target should give CPU Goose room to charge Honk.");
        int fullChargeFrames = GooseSpecials.honkMaxHoldFrames(goose);
        for (int frame = 0; frame < fullChargeFrames; frame++) {
            boolean held = goose.maintainAIHeldSpecialInputs();
            assertTrue(held, "CPU Goose should preserve the charge while the target remains safely spaced.");
            GooseSpecials.handleState(goose, held);
        }

        assertTrue(goose.gooseHonkReleased,
                "CPU Goose should release Honk when the full charge is ready.");
        assertEquals(fullChargeFrames, goose.gooseHonkHoldFrames);

        BirdGame3 pressureGame = new BirdGame3();
        pressureGame.activePlayers = 2;
        Bird pressuredGoose = new Bird(240.0, BirdGame3.BirdType.GOOSE, 0, pressureGame);
        Bird closeTarget = new Bird(350.0, BirdGame3.BirdType.PIGEON, 1, pressureGame);
        pressuredGoose.y = closeTarget.y = BirdGame3.GROUND_Y - 80.0;
        pressureGame.players[0] = pressuredGoose;
        pressureGame.players[1] = closeTarget;
        pressureGame.isAI[0] = true;
        setPrivateInt(pressuredGoose, "aiLockedTargetIndex", 1);

        GooseSpecials.neutral(pressuredGoose, false);
        assertFalse(pressuredGoose.shouldGooseAIChargeHonk(),
                "CPU Goose should not commit to a charge while an opponent is already close.");
        for (int frame = 0; frame < Bird.GOOSE_HONK_MIN_HOLD_FRAMES; frame++) {
            boolean held = pressuredGoose.maintainAIHeldSpecialInputs();
            assertFalse(held, "A pressured CPU Goose should allow the fast Honk release.");
            GooseSpecials.handleState(pressuredGoose, held);
        }

        assertTrue(pressuredGoose.gooseHonkReleased);
        assertEquals(Bird.GOOSE_HONK_MIN_HOLD_FRAMES, pressuredGoose.gooseHonkHoldFrames,
                "Close pressure should keep CPU Goose's readable minimum tell without adding charge delay.");
    }

    @Test
    void kiwiAiKeepsCommittedSpecialsOverSafeLandingLanes() {
        BirdGame3 game = new BirdGame3();
        game.harnessPrepareMatch(
                BirdGame3.BirdType.KIWI,
                BirdGame3.BirdType.PIGEON,
                0x5AFECA11L,
                BirdGame3.MapType.BATTLEFIELD);
        Bird kiwi = game.players[0];
        Bird target = game.players[1];
        Platform mainStage = null;
        for (Platform platform : game.platforms) {
            if (mainStage == null || platform.w > mainStage.w) {
                mainStage = platform;
            }
        }
        assertNotNull(mainStage);

        kiwi.x = mainStage.x + mainStage.w * 0.5 - kiwi.bodyWidth() * 0.5;
        kiwi.y = mainStage.y - 260.0;
        kiwi.vx = 0.0;
        target.x = kiwi.x + 45.0;
        target.y = kiwi.y + 190.0;
        double distance = kiwi.combatDistanceTo(target);
        assertEquals(Bird.DirectionalSpecialInput.DOWN,
                kiwi.chooseKiwiAISpecialInput(target, distance, false, false, true, false),
                "Earth Stomp should remain available when solid stage lies below the plunge.");

        kiwi.x = mainStage.x - 480.0;
        target.x = kiwi.x + 45.0;
        distance = kiwi.combatDistanceTo(target);
        assertNotEquals(Bird.DirectionalSpecialInput.DOWN,
                kiwi.chooseKiwiAISpecialInput(target, distance, false, false, true, false),
                "CPU Kiwi must not lock into Earth Stomp over the lower blast zone.");

        kiwi.y = mainStage.y - kiwi.bodyHeight();
        kiwi.x = mainStage.x + mainStage.w * 0.5 - kiwi.bodyWidth() * 0.5;
        target.x = kiwi.x + 250.0;
        target.y = kiwi.y;
        distance = kiwi.combatDistanceTo(target);
        assertEquals(Bird.DirectionalSpecialInput.SIDE,
                kiwi.chooseKiwiAISpecialInput(target, distance, true, false, false, false),
                "Burrow Charge should remain available when its committed route ends over the island.");

        kiwi.x = mainStage.x + mainStage.w - kiwi.bodyWidth() - 30.0;
        target.x = kiwi.x + 250.0;
        distance = kiwi.combatDistanceTo(target);
        assertNotEquals(Bird.DirectionalSpecialInput.SIDE,
                kiwi.chooseKiwiAISpecialInput(target, distance, true, false, false, false),
                "CPU Kiwi must not Burrow Charge beyond every safe landing lane.");
    }

    @Test
    void mockingbirdAiUsesItsWholeKit() {
        assertEquals(20, Bird.aiSpecialDecisionCooldownFor(BirdGame3.BirdType.MOCKINGBIRD),
                "Charles needs the same setup cadence as the other technical fighters.");
        assertEquals(16, Bird.aiSpecialDecisionCooldownFor(BirdGame3.BirdType.PIGEON));
        assertEquals(26, Bird.aiSpecialDecisionCooldownFor(BirdGame3.BirdType.PHOENIX));

        BirdGame3 game = new BirdGame3();
        game.activePlayers = 2;

        Bird charles = new Bird(220.0, BirdGame3.BirdType.MOCKINGBIRD, 0, game);
        Bird target = new Bird(390.0, BirdGame3.BirdType.PIGEON, 1, game);
        charles.y = BirdGame3.GROUND_Y - 80.0;
        target.y = BirdGame3.GROUND_Y - 80.0;
        game.players[0] = charles;
        game.players[1] = target;
        double distance = Math.hypot(
                target.bodyCenterX() - charles.bodyCenterX(),
                target.bodyCenterY() - charles.bodyCenterY());

        assertEquals(Bird.DirectionalSpecialInput.DOWN,
                charles.chooseMockingbirdAISpecialInput(target, distance, true, false, false),
                "Charles should establish the Lounge before trying to fight through his setup kit.");

        charles.loungeActive = true;
        charles.loungeHealth = Bird.LOUNGE_MAX_HEALTH;
        charles.loungeX = 1000.0;
        charles.loungeY = charles.bodyCenterY();
        target.x = charles.x + 90.0;
        distance = Math.hypot(
                target.bodyCenterX() - charles.bodyCenterX(),
                target.bodyCenterY() - charles.bodyCenterY());
        assertEquals(Bird.DirectionalSpecialInput.DOWN,
                charles.chooseMockingbirdAISpecialInput(target, distance, true, false, false),
                "CPU Charles should move a distant Lounge onto a nearby opponent so he can capture their neutral.");

        charles.mockingbirdLoungeReuseTimer = 20;
        assertEquals(Bird.DirectionalSpecialInput.NEUTRAL,
                charles.chooseMockingbirdAISpecialInput(target, distance, true, false, false),
                "While Lounge relocation is committed, CPU Charles should protect himself with blowback.");
        charles.mockingbirdLoungeReuseTimer = 0;

        target.x = 390.0;
        distance = Math.hypot(
                target.bodyCenterX() - charles.bodyCenterX(),
                target.bodyCenterY() - charles.bodyCenterY());
        assertEquals(Bird.DirectionalSpecialInput.NEUTRAL,
                charles.chooseMockingbirdAISpecialInput(target, distance, true, false, false),
                "Without a captured neutral, CPU Charles should use his close-range blowback call.");

        charles.mockingbirdBlowbackTimer = 20;
        assertEquals(Bird.DirectionalSpecialInput.SIDE,
                charles.chooseMockingbirdAISpecialInput(target, distance, true, false, false),
                "While blowback is recharging, Charles should keep fighting with his microphone.");
        charles.mockingbirdBlowbackTimer = 0;

        charles.mockingbirdCapturedType = BirdGame3.BirdType.PIGEON;
        assertEquals(Bird.DirectionalSpecialInput.NEUTRAL,
                charles.chooseMockingbirdAISpecialInput(target, distance, true, false, false),
                "Once Charles earns a copied neutral, the CPU should actually use it before another Mimic Call.");

        assertEquals(Bird.DirectionalSpecialInput.DOWN,
                charles.chooseMockingbirdAISpecialInput(target, distance, true, true, false),
                "Low-health Charles should still move the Lounge to his current position for healing.");
    }

    @Test
    void mockingbirdEmptyNeutralBlowsEnemiesAwayWithoutDamage() throws Exception {
        BirdGame3 game = new BirdGame3();
        game.activePlayers = 3;

        Bird charles = new Bird(220.0, BirdGame3.BirdType.MOCKINGBIRD, 0, game);
        Bird target = new Bird(365.0, BirdGame3.BirdType.PIGEON, 1, game);
        Bird behind = new Bird(80.0, BirdGame3.BirdType.EAGLE, 2, game);
        charles.y = BirdGame3.GROUND_Y - charles.bodyHeight();
        target.y = BirdGame3.GROUND_Y - target.bodyHeight();
        behind.y = BirdGame3.GROUND_Y - behind.bodyHeight();
        charles.facingRight = true;
        game.players[0] = charles;
        game.players[1] = target;
        game.players[2] = behind;
        double targetHealth = target.health;
        double targetPercent = getPrivateDouble(target, "smashDamage");

        MockingbirdSpecials.neutral(charles, false);

        assertEquals(targetHealth, target.health, 0.0,
                "Blowback must never deal stamina damage.");
        assertEquals(targetPercent, getPrivateDouble(target, "smashDamage"), 0.0,
                "Blowback must never add Smash percent.");
        assertTrue(target.vx > 7.0);
        assertTrue(target.vy < 0.0);
        assertEquals(0.0, behind.vx, 0.0,
                "The forward call must not push fighters standing behind Charles.");
        assertEquals(Bird.MOCKINGBIRD_BLOWBACK_FRAMES, charles.mockingbirdBlowbackTimer);
        assertEquals(0, charles.specialMaxCooldown,
                "Charles's replacement neutral must not restore the obsolete cooldown bar.");
    }

    @Test
    void mockingbirdLoungeRelocationPreservesDamageAndHasCommitment() {
        BirdGame3 game = new BirdGame3();
        game.activePlayers = 1;
        Bird charles = new Bird(220.0, BirdGame3.BirdType.MOCKINGBIRD, 0, game);
        charles.y = BirdGame3.GROUND_Y - charles.bodyHeight();
        game.players[0] = charles;

        MockingbirdSpecials.down(charles, false);
        double originalLoungeX = charles.loungeX;
        double originalLoungeY = charles.loungeY;
        assertEquals(Bird.LOUNGE_MAX_HEALTH, charles.loungeHealth);
        assertEquals(Bird.MOCKINGBIRD_LOUNGE_REUSE_FRAMES, charles.mockingbirdLoungeReuseTimer);

        charles.loungeHealth = 37;
        charles.x += 240.0;
        MockingbirdSpecials.down(charles, false);
        assertEquals(originalLoungeX, charles.loungeX,
                "Repeated Down Special must not instantly drag Lounge across the arena.");
        assertEquals(originalLoungeY, charles.loungeY);
        assertEquals(37, charles.loungeHealth,
                "An unavailable relocation must not erase damage opponents dealt to Lounge.");

        charles.mockingbirdLoungeReuseTimer = 0;
        MockingbirdSpecials.down(charles, false);
        assertEquals(charles.x + 40.0, charles.loungeX,
                "Lounge should relocate once its short commitment window expires.");
        assertEquals(charles.y + 40.0, charles.loungeY);
        assertEquals(37, charles.loungeHealth,
                "Relocating a living Lounge must preserve its remaining health.");
        assertEquals(Bird.MOCKINGBIRD_LOUNGE_REUSE_FRAMES, charles.mockingbirdLoungeReuseTimer);
    }

    @Test
    void mockingbirdMicrophoneSideSpecialChargesReleasesAndHitsOnlyOnce() {
        BirdGame3 game = new BirdGame3();
        game.activePlayers = 2;
        Bird charles = new Bird(600.0, BirdGame3.BirdType.MOCKINGBIRD, 0, game);
        Bird target = new Bird(900.0, BirdGame3.BirdType.PIGEON, 1, game);
        charles.y = BirdGame3.GROUND_Y - charles.bodyHeight();
        target.y = BirdGame3.GROUND_Y - target.bodyHeight();
        charles.facingRight = true;
        game.players[0] = charles;
        game.players[1] = target;

        double startingHealth = target.health;
        MockingbirdSpecials.side(charles, false);
        for (int tick = 0; tick < 40; tick++) {
            MockingbirdSpecials.handleState(charles, true);
        }

        assertTrue(charles.mockingbirdMicCharging);
        assertEquals(startingHealth, target.health, 0.0, "Charging must not create an invisible hitbox.");
        assertTrue(MockingbirdSpecials.microphoneChargeRatio(charles) > 0.5);
        assertEquals(0, charles.specialMaxCooldown, "Charles should not show the obsolete cooldown bar.");

        MockingbirdSpecials.handleState(charles, false);
        assertFalse(charles.mockingbirdMicCharging);
        assertEquals(Bird.MOCKINGBIRD_MIC_SWING_FRAMES, charles.mockingbirdMicSwingTimer);

        double angle = MockingbirdSpecials.microphoneSwingAngle(charles);
        double reach = (78.0 + MockingbirdSpecials.microphoneChargeRatio(charles) * 54.0)
                * charles.sizeMultiplier;
        double micX = charles.bodyCenterX() + Math.cos(angle) * reach;
        double micY = charles.bodyCenterY() + Math.sin(angle) * reach * 0.72;
        target.x = micX - target.bodyWidth() * 0.5;
        target.y = micY - target.bodyHeight() * 0.5;

        MockingbirdSpecials.handleState(charles, false);
        assertTrue(target.health < startingHealth);
        double healthAfterFirstHit = target.health;
        MockingbirdSpecials.handleState(charles, false);
        assertEquals(healthAfterFirstHit, target.health, 0.0,
                "One microphone swing must never multi-hit the same fighter every frame.");
    }

    @Test
    void mockingbirdUltimateBypassesEveryDirectionalReuseLock() throws Exception {
        BirdGame3 game = new BirdGame3();
        game.activePlayers = 1;
        Bird charles = new Bird(220.0, BirdGame3.BirdType.MOCKINGBIRD, 0, game);
        charles.y = BirdGame3.GROUND_Y - charles.bodyHeight();
        charles.mockingbirdBlowbackTimer = 20;
        charles.mockingbirdSideReuseTimer = 20;
        charles.mockingbirdUpSpecialUsed = true;
        charles.mockingbirdLoungeReuseTimer = 20;
        game.players[0] = charles;
        setPrivateDouble(charles, "ultimateMeter", 100.0);

        assertTrue(charles.canStartMockingbirdSpecial(),
                "Shadow Court must bypass an active blowback call.");

        game.pressedKeys.add(game.leftKeyForPlayer(0));
        assertTrue(charles.canStartMockingbirdSpecial(),
                "Shadow Court must bypass Mimic Call's reuse lock.");
        game.pressedKeys.clear();

        game.pressedKeys.add(game.jumpKeyForPlayer(0));
        assertTrue(charles.canStartMockingbirdSpecial(),
                "Shadow Court must bypass a spent Forest Lift.");
        game.pressedKeys.clear();

        game.pressedKeys.add(game.blockKeyForPlayer(0));
        assertTrue(charles.canStartMockingbirdSpecial(),
                "Shadow Court must bypass Lounge's relocation commitment.");
    }

    @Test
    void mockingbirdLoungeCaptureUsesBodyOverlapForLargeBirds() throws Exception {
        BirdGame3 game = new BirdGame3();
        game.activePlayers = 2;

        Bird charles = new Bird(220.0, BirdGame3.BirdType.MOCKINGBIRD, 0, game);
        Bird pelican = new Bird(500.0, BirdGame3.BirdType.PELICAN, 1, game);
        charles.y = BirdGame3.GROUND_Y - 80.0;
        charles.loungeActive = true;
        charles.loungeHealth = Bird.LOUNGE_MAX_HEALTH;
        charles.loungeX = charles.bodyCenterX();
        charles.loungeY = charles.bodyCenterY();
        pelican.x = charles.loungeX + 92.0 - pelican.bodyWidth() * 0.5;
        pelican.y = charles.loungeY - pelican.bodyHeight() * 0.5;
        game.players[0] = charles;
        game.players[1] = pelican;

        invokePrivateVoid(charles, "captureMockingbirdLoungeAbility");

        assertEquals(BirdGame3.BirdType.PELICAN, charles.mockingbirdCapturedType,
                "Lounge capture should use the target's body, not only its center point.");
    }

    @Test
    void mockingbirdLoungeCapturesEnemyChickOwnersType() throws Exception {
        BirdGame3 game = new BirdGame3();
        game.activePlayers = 2;

        Bird charles = new Bird(240.0, BirdGame3.BirdType.MOCKINGBIRD, 0, game);
        Bird rooster = new Bird(900.0, BirdGame3.BirdType.ROOSTER, 1, game);
        charles.y = BirdGame3.GROUND_Y - 80.0;
        rooster.y = BirdGame3.GROUND_Y - 80.0;
        charles.loungeActive = true;
        charles.loungeHealth = Bird.LOUNGE_MAX_HEALTH;
        charles.loungeX = charles.bodyCenterX();
        charles.loungeY = charles.bodyCenterY();
        game.players[0] = charles;
        game.players[1] = rooster;

        ChickMinion chick = new ChickMinion(charles.loungeX, charles.loungeY, 0, false, rooster);
        chick.x = charles.loungeX - chick.width * 0.5;
        chick.y = charles.loungeY - chick.height * 0.5;
        game.chickMinions.add(chick);

        invokePrivateVoid(charles, "captureMockingbirdLoungeAbility");

        assertEquals(BirdGame3.BirdType.ROOSTER, charles.mockingbirdCapturedType,
                "Capturing an enemy chick should steal the owner's Rooster neutral.");
    }

    @Test
    void copiedRoosterNeutralLaunchesHuntingChicksForMockingbird() {
        BirdGame3 game = new BirdGame3();
        game.activePlayers = 2;

        Bird charles = new Bird(260.0, BirdGame3.BirdType.MOCKINGBIRD, 0, game);
        Bird target = new Bird(560.0, BirdGame3.BirdType.PIGEON, 1, game);
        charles.y = BirdGame3.GROUND_Y - 80.0;
        target.y = BirdGame3.GROUND_Y - 80.0;
        charles.facingRight = true;
        charles.mockingbirdCapturedType = BirdGame3.BirdType.ROOSTER;
        game.players[0] = charles;
        game.players[1] = target;

        MockingbirdSpecials.performCopiedNeutral(charles, BirdGame3.BirdType.ROOSTER, false);

        List<ChickMinion> chicks = ownedChicks(game, charles);
        assertEquals(2, chicks.size(), "Copied Rooster neutral should create an immediate attacking threat.");
        for (ChickMinion chick : chicks) {
            assertFalse(chick.followingOwner, "Charles cannot use Rooster commands, so copied chicks should not idle in formation.");
            assertSame(target, chick.target);
            assertTrue(chick.vx > 15.0);
            assertTrue(chick.thrownFrames > 0);
        }
        assertEquals(BirdGame3.BirdType.MOCKINGBIRD, charles.type);
        assertEquals(BirdGame3.BirdType.ROOSTER, charles.mockingbirdCopiedNeutralSource);
    }

    @Test
    void copiedRavenNeutralKeepsFlyingAndHitsAfterCharlesReturnsToMockingbird() {
        BirdGame3 game = new BirdGame3();
        game.activePlayers = 2;

        Bird charles = new Bird(260.0, BirdGame3.BirdType.MOCKINGBIRD, 0, game);
        Bird target = new Bird(430.0, BirdGame3.BirdType.PIGEON, 1, game);
        charles.y = BirdGame3.GROUND_Y - 80.0;
        target.y = BirdGame3.GROUND_Y - 80.0;
        charles.facingRight = true;
        charles.mockingbirdCapturedType = BirdGame3.BirdType.RAVEN;
        game.players[0] = charles;
        game.players[1] = target;

        double startingHealth = target.health;
        MockingbirdSpecials.performCopiedNeutral(charles, BirdGame3.BirdType.RAVEN, false);
        assertEquals(BirdGame3.BirdType.MOCKINGBIRD, charles.type,
                "Charles should return to his own type immediately after copying the neutral.");

        for (int tick = 0; tick < 16 && target.health == startingHealth; tick++) {
            charles.update(1.0);
        }

        assertTrue(target.health < startingHealth,
                "The copied Black Quill must keep simulating after Charles returns to his own type.");
        assertTrue(target.hasRavenPortentFrom(charles),
                "A copied Black Quill hit should retain Raven's mark effect and Charles's ownership.");
    }

    @Test
    void mockingbirdUltimateSpawnsCapturedShadowCourt() throws Exception {
        BirdGame3 game = new BirdGame3();
        game.activePlayers = 2;

        Bird charles = new Bird(260.0, BirdGame3.BirdType.MOCKINGBIRD, 0, game);
        Bird target = new Bird(620.0, BirdGame3.BirdType.PIGEON, 1, game);
        charles.y = BirdGame3.GROUND_Y - 80.0;
        target.y = BirdGame3.GROUND_Y - 80.0;
        charles.loungeActive = true;
        charles.loungeHealth = Bird.LOUNGE_MAX_HEALTH;
        charles.loungeX = 430.0;
        charles.loungeY = BirdGame3.GROUND_Y - 64.0;
        charles.mockingbirdCapturedType = BirdGame3.BirdType.PELICAN;
        game.players[0] = charles;
        game.players[1] = target;

        setPrivateDouble(charles, "ultimateMeter", 100.0);
        BirdSpecialSystem.useSpecial(charles);

        assertEquals(0.0, getPrivateDouble(charles, "ultimateMeter"), 0.0001);
        assertEquals(3, game.mockingbirdShadowMinions.size());
        assertTrue(charles.loungeRoyal);
        assertEquals(200.0, charles.loungeMaxHealth, 0.0001);
        assertEquals(MockingbirdSpecials.SHADOW_COURT_MOVE, game.lastTelemetryMoveName(0, ""));

        MockingbirdShadowMinion left = game.mockingbirdShadowMinions.get(0);
        MockingbirdShadowMinion right = game.mockingbirdShadowMinions.get(1);
        MockingbirdShadowMinion inside = game.mockingbirdShadowMinions.get(2);
        assertTrue(left.bodyCenterX() < charles.loungeX - 80.0);
        assertTrue(right.bodyCenterX() > charles.loungeX + 80.0);
        assertTrue(Math.abs(inside.bodyCenterX() - charles.loungeX) < 28.0);
        assertTrue(game.mockingbirdShadowMinions.stream()
                .allMatch(shadow -> shadow.owner == charles
                        && shadow.target == target
                        && shadow.copiedType == BirdGame3.BirdType.PELICAN
                        && shadow.health <= 14.0));
    }

    @Test
    void mockingbirdUltimateCreatesLoungeAndCopiesClosestBirdWhenEmpty() throws Exception {
        BirdGame3 game = new BirdGame3();
        game.activePlayers = 3;

        Bird charles = new Bird(240.0, BirdGame3.BirdType.MOCKINGBIRD, 0, game);
        Bird eagle = new Bird(420.0, BirdGame3.BirdType.EAGLE, 1, game);
        Bird pelican = new Bird(1050.0, BirdGame3.BirdType.PELICAN, 2, game);
        charles.y = BirdGame3.GROUND_Y - 80.0;
        eagle.y = BirdGame3.GROUND_Y - 80.0;
        pelican.y = BirdGame3.GROUND_Y - 80.0;
        game.players[0] = charles;
        game.players[1] = eagle;
        game.players[2] = pelican;

        setPrivateDouble(charles, "ultimateMeter", 100.0);
        BirdSpecialSystem.useSpecial(charles);

        assertTrue(charles.loungeActive);
        assertTrue(charles.loungeRoyal);
        assertEquals(3, game.mockingbirdShadowMinions.size());
        assertTrue(game.mockingbirdShadowMinions.stream()
                .allMatch(shadow -> shadow.copiedType == BirdGame3.BirdType.EAGLE));
    }

    @Test
    void mockingbirdShadowMinionDamagesNearestEnemy() throws Exception {
        BirdGame3 game = new BirdGame3();
        game.activePlayers = 2;

        Bird charles = new Bird(260.0, BirdGame3.BirdType.MOCKINGBIRD, 0, game);
        Bird target = new Bird(350.0, BirdGame3.BirdType.PIGEON, 1, game);
        charles.y = BirdGame3.GROUND_Y - 80.0;
        target.y = BirdGame3.GROUND_Y - 80.0;
        game.players[0] = charles;
        game.players[1] = target;

        MockingbirdShadowMinion shadow = new MockingbirdShadowMinion(
                target.bodyCenterX() - 12.0,
                target.bodyCenterY(),
                BirdGame3.BirdType.EAGLE,
                charles,
                0
        );
        shadow.attackCooldown = 0;
        shadow.target = target;
        game.mockingbirdShadowMinions.add(shadow);

        double healthBefore = target.health;
        invokePrivateVoid(game, "updateMockingbirdShadowMinions");

        assertTrue(target.health < healthBefore);
        assertTrue(game.damageDealt[0] > 0);
        assertTrue(shadow.attackCooldown > 0);
    }

    @Test
    void roadrunnerNeutralChargesThenReleasesMomentumBurst() throws Exception {
        BirdGame3 game = new BirdGame3();
        game.activePlayers = 2;

        Bird runner = new Bird(220.0, BirdGame3.BirdType.ROADRUNNER, 0, game);
        Bird target = new Bird(345.0, BirdGame3.BirdType.PIGEON, 1, game);
        runner.y = BirdGame3.GROUND_Y - 80.0;
        target.y = BirdGame3.GROUND_Y - 80.0;
        game.players[0] = runner;
        game.players[1] = target;

        KeyCode specialKey = game.specialKeyForPlayer(0);
        game.setLocalActionsForKey(specialKey, true);
        for (int i = 0; i < 42; i++) {
            runner.update(1.0);
        }

        double targetHealthBeforeRelease = target.health;
        assertTrue(getPrivateBoolean(runner, "roadrunnerBeepCharging"));
        assertTrue(getPrivateInt(runner, "roadrunnerBeepChargeFrames") >= 35);
        assertEquals(targetHealthBeforeRelease, target.health, 0.0001,
                "Beep-Beep Blitz should not hit until the held neutral is released.");

        game.setLocalActionsForKey(specialKey, false);
        runner.update(1.0);

        assertFalse(getPrivateBoolean(runner, "roadrunnerBeepCharging"));
        assertTrue(getPrivateInt(runner, "roadrunnerBeepBurstTimer") > 0);
        assertTrue(target.health < targetHealthBeforeRelease,
                "Releasing neutral should fire the charged burst hit.");
        assertTrue(target.vx > 10.0,
                "Charged Beep-Beep Blitz should launch forward with real knockback.");
        assertTrue(runner.vx > 25.0,
                "Charged neutral should release Roadrunner at high speed even without horizontal input held.");
        assertEquals(0, runner.specialCooldown);
        assertTrue(getPrivateInt(runner, "roadrunnerBeepReuseTimer") > 0);
    }

    @Test
    void roadrunnerQuickBeepBlitzHasARealLaunchFloor() {
        BirdGame3 game = new BirdGame3();
        game.activePlayers = 2;

        Bird runner = new Bird(220.0, BirdGame3.BirdType.ROADRUNNER, 0, game);
        Bird target = new Bird(300.0, BirdGame3.BirdType.PIGEON, 1, game);
        runner.y = BirdGame3.GROUND_Y - 80.0;
        target.y = BirdGame3.GROUND_Y - 80.0;
        runner.roadrunnerBeepBurstTimer = 1;
        runner.roadrunnerBeepDirection = 1;
        game.players[0] = runner;
        game.players[1] = target;

        RoadrunnerSpecials.applyBeepBlitzHit(runner, 0.0);

        assertEquals(1.15, RoadrunnerSpecials.CORE_SPECIAL_KNOCKBACK_MULTIPLIER, 0.0001);
        assertEquals(8.0 * RoadrunnerSpecials.CORE_SPECIAL_KNOCKBACK_MULTIPLIER, target.vx, 0.0001);
        assertEquals(-3.0 * RoadrunnerSpecials.CORE_SPECIAL_KNOCKBACK_MULTIPLIER, target.vy, 0.0001);
    }

    @Test
    void roadrunnerNeutralAutoReleasesAtFullChargeSpeed() throws Exception {
        BirdGame3 game = new BirdGame3();
        game.activePlayers = 1;

        Bird runner = new Bird(220.0, BirdGame3.BirdType.ROADRUNNER, 0, game);
        runner.y = BirdGame3.GROUND_Y - 80.0;
        runner.facingRight = true;
        game.players[0] = runner;

        game.setLocalActionsForKey(game.specialKeyForPlayer(0), true);
        for (int i = 0; i < 72; i++) {
            runner.update(1.0);
        }

        assertFalse(getPrivateBoolean(runner, "roadrunnerBeepCharging"),
                "Neutral should auto-release as soon as it reaches full charge.");
        assertTrue(getPrivateInt(runner, "roadrunnerBeepBurstTimer") > 0);
        assertTrue(runner.vx > 45.0,
                "A full-charge neutral should immediately propel Roadrunner at full speed.");
        game.setLocalActionsForKey(game.specialKeyForPlayer(0), false);
    }

    @Test
    void roadrunnerHighSpeedCoastsDownInsteadOfStoppingHard() {
        BirdGame3 game = new BirdGame3();
        game.activePlayers = 1;

        Bird runner = new Bird(260.0, BirdGame3.BirdType.ROADRUNNER, 0, game);
        runner.y = BirdGame3.GROUND_Y - 80.0;
        runner.vx = 30.0;
        game.players[0] = runner;

        runner.update(1.0);

        assertTrue(runner.vx > 24.0,
                "Roadrunner should bleed off high speed instead of instantly stopping when input drops.");
    }

    @Test
    void roadrunnerMomentumGracePreservesFlowBeforeSlowDecay() {
        BirdGame3 game = new BirdGame3();
        game.activePlayers = 1;

        Bird runner = new Bird(260.0, BirdGame3.BirdType.ROADRUNNER, 0, game);
        runner.y = BirdGame3.GROUND_Y - 80.0;
        runner.roadrunnerMomentum = 40.0;
        game.players[0] = runner;

        RoadrunnerSpecials.addMomentum(runner, 10.0);

        assertEquals(RoadrunnerSpecials.MOMENTUM_BUILD_GRACE_FRAMES,
                runner.roadrunnerMomentumGraceTimer);
        LanBirdState snapshot = runner.toLanState();
        Bird restored = new Bird(0.0, BirdGame3.BirdType.ROADRUNNER, 0, game);
        restored.applyLanState(snapshot);
        assertEquals(runner.roadrunnerMomentumGraceTimer, restored.roadrunnerMomentumGraceTimer,
                "LAN snapshots must preserve the deterministic grace timer.");
        for (int i = 0; i < RoadrunnerSpecials.MOMENTUM_BUILD_GRACE_FRAMES; i++) {
            RoadrunnerSpecials.handleMomentum(runner);
        }
        assertEquals(50.0, runner.roadrunnerMomentum, 0.0001,
                "Earned momentum should hold throughout the grace window.");

        RoadrunnerSpecials.handleMomentum(runner);
        assertEquals(50.0 - RoadrunnerSpecials.MOMENTUM_GROUND_DECAY_PER_FRAME,
                runner.roadrunnerMomentum, 0.0001,
                "Momentum should decay gradually once flow expires.");
    }

    @Test
    void roadrunnerCombatRetainsMomentumWithoutRemovingCounterplay() throws Exception {
        BirdGame3 game = new BirdGame3();
        game.activePlayers = 2;

        Bird runner = new Bird(220.0, BirdGame3.BirdType.ROADRUNNER, 0, game);
        Bird opponent = new Bird(330.0, BirdGame3.BirdType.PIGEON, 1, game);
        runner.y = BirdGame3.GROUND_Y - 80.0;
        opponent.y = BirdGame3.GROUND_Y - 80.0;
        runner.roadrunnerMomentum = 100.0;
        game.players[0] = runner;
        game.players[1] = opponent;

        double dealtToRunner = applyPrivateDamage(opponent, runner, 10.0);
        double expectedLoss = Math.clamp(4.0 + dealtToRunner * 0.45, 6.0, 18.0);
        assertEquals(100.0 - expectedLoss, runner.roadrunnerMomentum, 0.0001,
                "Taking damage should cost capped, damage-scaled momentum instead of a flat 38.");

        runner.roadrunnerMomentumGraceTimer = 0;
        applyPrivateDamage(runner, opponent, 8.0);
        assertEquals(RoadrunnerSpecials.MOMENTUM_HIT_GRACE_FRAMES,
                runner.roadrunnerMomentumGraceTimer,
                "Landing a hit should preserve Roadrunner's remaining flow.");
    }

    @Test
    void roadrunnerSpecialsPreserveEnoughMomentumToChainMovement() {
        BirdGame3 game = new BirdGame3();
        Bird runner = new Bird(220.0, BirdGame3.BirdType.ROADRUNNER, 0, game);
        runner.y = BirdGame3.GROUND_Y - 80.0;
        game.players[0] = runner;

        runner.roadrunnerMomentum = 100.0;
        RoadrunnerSpecials.side(runner, false);
        assertEquals(100.0 * (1.0 - RoadrunnerSpecials.RICOCHET_MOMENTUM_COST),
                runner.roadrunnerMomentum, 0.0001,
                "Ricochet should carry most of an established momentum chain into the next action.");

        runner.roadrunnerMomentum = 100.0;
        runner.roadrunnerDustDevilUsed = false;
        RoadrunnerSpecials.up(runner, false);
        assertEquals(100.0 * (1.0 - RoadrunnerSpecials.DUST_DEVIL_MOMENTUM_COST),
                runner.roadrunnerMomentum, 0.0001,
                "Recovery should not erase the momentum Roadrunner earned before going offstage.");
    }

    @Test
    void roadrunnerMomentumSoftensDamageWithoutProtectingAnIdleRunner() {
        double originalDamageTaken = BirdGame3.BirdType.ROADRUNNER.damageTakenMult;
        try {
            BirdGame3.BirdType.ROADRUNNER.damageTakenMult = 1.0;
            BirdGame3 game = new BirdGame3();
            game.activePlayers = 2;

            Bird movingRunner = new Bird(220.0, BirdGame3.BirdType.ROADRUNNER, 0, game);
            Bird idleRunner = new Bird(420.0, BirdGame3.BirdType.ROADRUNNER, 1, game);
            movingRunner.roadrunnerMomentum = Bird.ROADRUNNER_MOMENTUM_MAX;
            game.players[0] = movingRunner;
            game.players[1] = idleRunner;

            assertEquals(1.0 - RoadrunnerSpecials.MAX_MOMENTUM_DAMAGE_REDUCTION,
                    RoadrunnerSpecials.incomingDamageMultiplier(movingRunner), 0.0001);
            assertEquals(8.2, movingRunner.receiveExternalDamage(10.0), 0.0001,
                    "Maximum momentum should earn Roadrunner's full damage reduction.");
            assertTrue(movingRunner.roadrunnerMomentum < Bird.ROADRUNNER_MOMENTUM_MAX,
                    "Taking the softened hit should still cost momentum.");

            assertEquals(1.0, RoadrunnerSpecials.incomingDamageMultiplier(idleRunner), 0.0001);
            assertEquals(10.0, idleRunner.receiveExternalDamage(10.0), 0.0001,
                    "An idle Roadrunner should keep his full glass-cannon vulnerability.");
        } finally {
            BirdGame3.BirdType.ROADRUNNER.damageTakenMult = originalDamageTaken;
        }
    }

    @Test
    void roadrunnerMomentumPowersHitsWithoutBuffingAnIdleRunner() {
        double originalDamageDealt = BirdGame3.BirdType.ROADRUNNER.damageDealtMult;
        double originalDamageTaken = BirdGame3.BirdType.PIGEON.damageTakenMult;
        try {
            BirdGame3.BirdType.ROADRUNNER.damageDealtMult = 1.0;
            BirdGame3.BirdType.PIGEON.damageTakenMult = 1.0;
            BirdGame3 game = new BirdGame3();
            game.activePlayers = 4;

            Bird movingRunner = new Bird(220.0, BirdGame3.BirdType.ROADRUNNER, 0, game);
            Bird idleRunner = new Bird(420.0, BirdGame3.BirdType.ROADRUNNER, 1, game);
            Bird movingTarget = new Bird(620.0, BirdGame3.BirdType.PIGEON, 2, game);
            Bird idleTarget = new Bird(820.0, BirdGame3.BirdType.PIGEON, 3, game);
            movingRunner.roadrunnerMomentum = Bird.ROADRUNNER_MOMENTUM_MAX;
            game.players[0] = movingRunner;
            game.players[1] = idleRunner;
            game.players[2] = movingTarget;
            game.players[3] = idleTarget;

            assertEquals(1.0 + RoadrunnerSpecials.MAX_MOMENTUM_DAMAGE_BONUS,
                    RoadrunnerSpecials.outgoingDamageMultiplier(movingRunner), 0.0001);
            assertEquals(11.2, movingRunner.applyDamageTo(movingTarget, 10.0), 0.0001,
                    "Maximum momentum should earn Roadrunner's full outgoing damage bonus.");

            assertEquals(1.0, RoadrunnerSpecials.outgoingDamageMultiplier(idleRunner), 0.0001);
            assertEquals(10.0, idleRunner.applyDamageTo(idleTarget, 10.0), 0.0001,
                    "An idle Roadrunner should not receive free damage.");
        } finally {
            BirdGame3.BirdType.ROADRUNNER.damageDealtMult = originalDamageDealt;
            BirdGame3.BirdType.PIGEON.damageTakenMult = originalDamageTaken;
        }
    }

    @Test
    void roadrunnerSideRicochetUsesInvisibleReuseAndHitsFast() throws Exception {
        BirdGame3 game = new BirdGame3();
        game.activePlayers = 2;

        Bird runner = new Bird(240.0, BirdGame3.BirdType.ROADRUNNER, 0, game);
        Bird target = new Bird(328.0, BirdGame3.BirdType.PIGEON, 1, game);
        runner.y = BirdGame3.GROUND_Y - 80.0;
        target.y = BirdGame3.GROUND_Y - 80.0;
        game.players[0] = runner;
        game.players[1] = target;
        setPrivateDouble(runner, "roadrunnerMomentum", 80.0);

        game.setLocalActionsForKey(game.rightKeyForPlayer(0), true);
        invokePrivateVoid(runner, "special");
        double targetHealthBefore = target.health;
        runner.update(1.0);

        assertTrue(getPrivateInt(runner, "roadrunnerRicochetTimer") > 0);
        assertTrue(Math.abs(runner.vx) > 18.0,
                "Canyon Ricochet should be a high-speed dash.");
        assertTrue(target.health < targetHealthBefore,
                "Ricochet should damage birds in its lane.");
        assertEquals(0, runner.specialCooldown);
        assertTrue(getPrivateInt(runner, "roadrunnerRicochetReuseTimer") > 0);

        game.setLocalActionsForKey(game.rightKeyForPlayer(0), false);
    }

    @Test
    void roadrunnerUpSpecialIsOncePerAirtimeDustDevilRecovery() throws Exception {
        BirdGame3 game = new BirdGame3();
        game.activePlayers = 2;

        Bird runner = new Bird(260.0, BirdGame3.BirdType.ROADRUNNER, 0, game);
        Bird target = new Bird(300.0, BirdGame3.BirdType.PIGEON, 1, game);
        runner.y = BirdGame3.GROUND_Y - 260.0;
        target.y = BirdGame3.GROUND_Y - 260.0;
        runner.vy = 0.0;
        target.vy = 0.0;
        game.players[0] = runner;
        game.players[1] = target;
        setPrivateDouble(runner, "roadrunnerMomentum", 70.0);

        game.setLocalActionsForKey(game.jumpKeyForPlayer(0), true);
        invokePrivateVoid(runner, "special");
        runner.update(1.0);

        assertTrue(runner.vy < -20.0,
                "Dust Devil Lift should provide strong vertical recovery.");
        assertTrue(target.vy < -14.0,
                "Dust Devil Lift should knock nearby birds upward.");
        assertTrue(getPrivateBoolean(runner, "roadrunnerDustDevilUsed"));
        assertEquals(0, runner.specialCooldown);

        setPrivateInt(runner, "roadrunnerDustDevilTimer", 0);
        runner.vy = 0.0;
        invokePrivateVoid(runner, "special");
        assertEquals(0.0, runner.vy, 0.0001,
                "Roadrunner should not get another up special before landing.");
        game.setLocalActionsForKey(game.jumpKeyForPlayer(0), false);
    }

    @Test
    void roadrunnerPaintedRoadSlipsEnemiesAndBoostsOwnerMomentum() throws Exception {
        BirdGame3 game = new BirdGame3();
        game.activePlayers = 2;

        Bird runner = new Bird(300.0, BirdGame3.BirdType.ROADRUNNER, 0, game);
        Bird target = new Bird(365.0, BirdGame3.BirdType.PIGEON, 1, game);
        runner.y = BirdGame3.GROUND_Y - 80.0;
        target.y = BirdGame3.GROUND_Y - 80.0;
        runner.facingRight = true;
        game.players[0] = runner;
        game.players[1] = target;

        game.setLocalActionsForKey(game.blockKeyForPlayer(0), true);
        invokePrivateVoid(runner, "special");
        game.setLocalActionsForKey(game.blockKeyForPlayer(0), false);

        assertEquals(0, runner.specialCooldown);
        assertTrue(getPrivateInt(runner, "roadrunnerPaintedRoadReuseTimer") > 0);
        assertEquals(1, ((List<?>) getPrivateObject(runner, "roadrunnerPaintedRoads")).size());
        assertTrue(runner.vx > 8.0,
                "Down special should propel Roadrunner forward when no horizontal input is held.");

        runner.update(1.0);
        assertTrue(getPrivateInt(target, "roadrunnerSlipTimer") > 0,
                "Enemies standing on Painted Road should receive the slip debuff.");
        assertEquals(0, getPrivateInt(runner, "roadrunnerRoadBoostTimer"),
                "Roadrunner should not trigger his road boost before stepping off the road once.");

        target.vx = 0.0;
        target.update(1.0);
        assertTrue(target.vx < -8.0,
                "Stepping on the road should launch enemies opposite the road direction.");

        Object road = ((List<?>) getPrivateObject(runner, "roadrunnerPaintedRoads")).getFirst();
        double roadX = getPrivateDouble(road, "x");
        double roadY = getPrivateDouble(road, "y");
        target.x = 800.0;
        runner.x = roadX - 220.0;
        runner.y = roadY - 80.0;
        runner.vx = 0.0;
        runner.vy = 0.0;
        runner.update(1.0);

        runner.x = roadX - 40.0;
        runner.y = roadY - 80.0;
        runner.vx = 0.0;
        runner.vy = 0.0;
        runner.update(1.0);

        assertTrue(getPrivateInt(runner, "roadrunnerRoadBoostTimer") > 0,
                "Roadrunner should trigger the road boost after leaving and re-entering it.");
        assertTrue(runner.vx > 20.0,
                "Roadrunner's road should launch him hard in the road direction once armed.");
    }

    @Test
    void defeatedBirdRemovesOwnedSummons() {
        BirdGame3 game = new BirdGame3();
        Bird owner = new Bird(100, BirdGame3.BirdType.VULTURE, 0, game);
        game.players[0] = owner;

        CrowMinion crow = new CrowMinion(140, 140, null);
        crow.owner = owner;
        game.crowMinions.add(crow);

        ChickMinion chick = new ChickMinion(150, 150, 0, false, owner);
        game.chickMinions.add(chick);

        owner.health = 0;
        owner.update(1.0);

        assertTrue(game.crowMinions.isEmpty());
        assertTrue(game.chickMinions.isEmpty());
    }

    @Test
    void vultureNeutralConsumesHeldCrowTicksAndRecharges() throws Exception {
        BirdGame3 game = new BirdGame3();
        game.activePlayers = 2;

        Bird vulture = new Bird(100.0, BirdGame3.BirdType.VULTURE, 0, game);
        Bird target = new Bird(260.0, BirdGame3.BirdType.PIGEON, 1, game);
        vulture.y = BirdGame3.GROUND_Y - 80.0;
        target.y = BirdGame3.GROUND_Y - 80.0;
        game.players[0] = vulture;
        game.players[1] = target;

        game.setLocalActionsForKey(game.specialKeyForPlayer(0), true);
        vulture.update(1.0);

        assertEquals(1, game.crowMinions.size());
        assertEquals(Bird.VULTURE_CROW_TICK_MAX - 1, getPrivateInt(vulture, "vultureCrowTicks"));
        assertEquals(0, vulture.specialCooldown);

        for (int i = 0; i < 40 && game.crowMinions.size() < Bird.VULTURE_CROW_TICK_MAX; i++) {
            vulture.update(1.0);
        }

        assertEquals(Bird.VULTURE_CROW_TICK_MAX, game.crowMinions.size(),
                "Holding neutral should walk through all available Vulture crow ticks.");
        assertEquals(0, getPrivateInt(vulture, "vultureCrowTicks"));

        game.setLocalActionsForKey(game.specialKeyForPlayer(0), false);
        for (int i = 0; i < 165; i++) {
            vulture.update(1.0);
        }

        assertTrue(getPrivateInt(vulture, "vultureCrowTicks") >= 1,
                "Spent Vulture crow ticks should recharge over time.");
    }

    @Test
    void vultureBoneOfferingSpawnsDelayedAnchoredCrowSwarm() throws Exception {
        BirdGame3 game = new BirdGame3();
        game.activePlayers = 2;

        Bird vulture = new Bird(100.0, BirdGame3.BirdType.VULTURE, 0, game);
        Bird target = new Bird(190.0, BirdGame3.BirdType.PIGEON, 1, game);
        vulture.y = BirdGame3.GROUND_Y - 80.0;
        target.y = BirdGame3.GROUND_Y - 80.0;
        game.players[0] = vulture;
        game.players[1] = target;

        vulture.specialVultureBoneOffering();
        Object bait = getPrivateObject(vulture, "vultureBait");

        assertTrue(getPrivateInt(bait, "lifeFrames") >= 700,
                "Vulture's bone offering should last much longer than before.");
        for (int i = 0; i < 120; i++) {
            vulture.update(1.0);
        }
        assertTrue(game.crowMinions.isEmpty(),
                "Bone offering crows should not appear immediately.");

        for (int i = 0; i < 45; i++) {
            vulture.update(1.0);
        }
        int firstWave = game.crowMinions.size();
        assertTrue(firstWave > 0);
        assertTrue(game.crowMinions.stream().allMatch(CrowMinion::guardsAnchor));

        for (int i = 0; i < 160; i++) {
            vulture.update(1.0);
        }
        assertTrue(game.crowMinions.size() > firstWave,
                "Bone offering should build crow pressure gradually.");
        assertTrue(game.crowMinions.stream().allMatch(CrowMinion::guardsAnchor),
                "Bone offering crows should stay leashed to the bone.");
        assertEquals(8, game.crowMinions.size(),
                "Bone Offering should retain its full eight-crow flock.");
    }

    @Test
    void vultureUltimateStartsBlackSkyFeastInsteadOfBoostedCall() throws Exception {
        BirdGame3 game = new BirdGame3();
        game.activePlayers = 2;

        Bird vulture = new Bird(160.0, BirdGame3.BirdType.VULTURE, 0, game);
        Bird target = new Bird(360.0, BirdGame3.BirdType.PIGEON, 1, game);
        vulture.y = BirdGame3.GROUND_Y - 80.0;
        target.y = BirdGame3.GROUND_Y - 80.0;
        game.players[0] = vulture;
        game.players[1] = target;

        setPrivateDouble(vulture, "ultimateMeter", 100.0);
        invokePrivateVoid(vulture, "special");

        assertEquals(Bird.VULTURE_BLACK_SKY_FRAMES, getPrivateInt(vulture, "vultureBlackSkyTimer"));
        assertEquals(Bird.VULTURE_BLACK_SKY_INITIAL_CROWS, getPrivateInt(vulture, "vultureBlackSkyCrowsSpawned"));
        assertEquals(Bird.VULTURE_BLACK_SKY_INITIAL_CROWS, game.crowMinions.size());
        assertTrue(game.crowMinions.stream().allMatch(crow -> crow.owner == vulture && crow.hasCrown));
        assertEquals(0, getPrivateInt(vulture, "vultureCallTimer"),
                "Vulture ultimate should not fall through into boosted Summon Crows.");
        assertFalse(vulture.isUltimateReady());
        assertEquals(0, vulture.specialCooldown);
        assertEquals(VultureSpecials.BLACK_SKY_FEAST_MOVE, game.lastTelemetryMoveName(0, ""));
    }

    @Test
    void vultureBlackSkyFeastBuildsCrowStormAndFinalHit() throws Exception {
        BirdGame3 game = new BirdGame3();
        game.activePlayers = 2;

        Bird vulture = new Bird(160.0, BirdGame3.BirdType.VULTURE, 0, game);
        Bird target = new Bird(310.0, BirdGame3.BirdType.PIGEON, 1, game);
        vulture.y = BirdGame3.GROUND_Y - 80.0;
        target.y = BirdGame3.GROUND_Y - 80.0;
        game.players[0] = vulture;
        game.players[1] = target;

        setPrivateDouble(vulture, "ultimateMeter", 100.0);
        invokePrivateVoid(vulture, "special");
        int carrionTimerAfterStart = vulture.carrionSwarmTimer;
        vulture.update(1.0);
        assertEquals(carrionTimerAfterStart - 1, vulture.carrionSwarmTimer,
                "Carrion swarm visuals should tick in sim updates, not while rendering.");

        for (int i = 0; i < 70; i++) {
            vulture.update(1.0);
        }

        assertTrue(getPrivateInt(vulture, "vultureBlackSkyCrowsSpawned") >= Bird.VULTURE_BLACK_SKY_TARGET_CROWS,
                "Black Sky Feast should build into a large real crow swarm before the finisher.");
        double healthBeforeFinal = target.health;

        for (int i = 0; i < 65; i++) {
            vulture.update(1.0);
        }

        assertTrue(getPrivateBoolean(vulture, "vultureBlackSkyFinalHit"));
        assertTrue(target.health < healthBeforeFinal,
                "The feast finisher should damage targets caught near the storm center.");
        assertTrue(getPrivateInt(vulture, "vultureBlackSkyCrowsSpawned")
                        >= Bird.VULTURE_BLACK_SKY_TARGET_CROWS + Bird.VULTURE_BLACK_SKY_FINAL_CROWS,
                "The finisher should add a final crow burst.");
    }

    @Test
    void nullRockSideSpecialLocksNearestEnemyAndFiresGiganticLaser() {
        BirdGame3 game = new BirdGame3();
        game.activePlayers = 3;
        Bird boss = new Bird(500.0, BirdGame3.BirdType.VULTURE, 0, game);
        Bird nearest = new Bird(900.0, BirdGame3.BirdType.PIGEON, 1, game);
        Bird farther = new Bird(1500.0, BirdGame3.BirdType.EAGLE, 2, game);
        boss.isNullRockSkin = true;
        boss.y = BirdGame3.GROUND_Y - boss.bodyHeight();
        nearest.y = BirdGame3.GROUND_Y - nearest.bodyHeight();
        farther.y = BirdGame3.GROUND_Y - farther.bodyHeight();
        game.players[0] = boss;
        game.players[1] = nearest;
        game.players[2] = farther;

        double healthBefore = nearest.health;
        VultureSpecials.nullRockSide(boss, false);

        assertEquals(nearest.playerIndex, boss.nullRockLaserTargetIndex);
        assertEquals(Bird.NULL_ROCK_LASER_FRAMES, boss.nullRockLaserTimer);
        for (int frame = 0; frame <= Bird.NULL_ROCK_LASER_WINDUP_FRAMES; frame++) {
            VultureSpecials.handleState(boss, false);
        }

        assertTrue(boss.nullRockLaserFired);
        assertTrue(nearest.health < healthBefore);
        assertTrue(nearest.vx > 0.0, "The eye laser should launch along its aim line.");
        assertEquals(0, game.hitstopFrames,
                "Null Rock's eye laser must not freeze the entire match.");
    }

    @Test
    void playerNullRockTracksEachSpecialCooldownIndependently() {
        BirdGame3 game = new BirdGame3();
        game.activePlayers = 1;
        game.isAI[0] = false;
        Bird boss = new Bird(500.0, BirdGame3.BirdType.VULTURE, 0, game);
        boss.isNullRockSkin = true;
        game.players[0] = boss;

        VultureSpecials.nullRockSide(boss, false);

        assertEquals(0, boss.specialCooldown,
                "A player Null Rock should not use the boss AI's shared cooldown.");
        assertEquals(VultureSpecials.NULL_ROCK_SIDE_REUSE_FRAMES, boss.nullRockSideReuseTimer);
        assertEquals(0, boss.nullRockNeutralReuseTimer);
        assertEquals(0, boss.nullRockUpReuseTimer);
        assertEquals(0, boss.nullRockDownReuseTimer);

        VultureSpecials.reset(boss, false);
        game.setLocalActionsForKey(game.rightKeyForPlayer(0), true);
        assertFalse(VultureSpecials.canStart(boss, false, false),
                "The laser should remain unavailable during only its own cooldown.");
        game.setLocalActionsForKey(game.rightKeyForPlayer(0), false);
        assertTrue(VultureSpecials.canStart(boss, false, false),
                "Dark Flock should remain ready after the laser is used.");

        VultureSpecials.nullRockNeutral(boss, false);
        VultureSpecials.reset(boss, false);
        VultureSpecials.nullRockUp(boss, false);
        VultureSpecials.reset(boss, false);
        VultureSpecials.nullRockDown(boss, false);

        assertEquals(VultureSpecials.NULL_ROCK_NEUTRAL_REUSE_FRAMES, boss.nullRockNeutralReuseTimer);
        assertEquals(VultureSpecials.NULL_ROCK_SIDE_REUSE_FRAMES, boss.nullRockSideReuseTimer);
        assertEquals(VultureSpecials.NULL_ROCK_UP_REUSE_FRAMES, boss.nullRockUpReuseTimer);
        assertEquals(VultureSpecials.NULL_ROCK_DOWN_REUSE_FRAMES, boss.nullRockDownReuseTimer);
    }

    @Test
    void cpuNullRockKeepsSharedDifficultyScaledBossCooldown() {
        BirdGame3 game = new BirdGame3();
        game.activePlayers = 1;
        game.isAI[0] = true;
        Bird boss = new Bird(500.0, BirdGame3.BirdType.VULTURE, 0, game);
        boss.isNullRockSkin = true;
        game.players[0] = boss;

        VultureSpecials.nullRockSide(boss, false);
        VultureSpecials.reset(boss, false);

        assertTrue(boss.specialCooldown >= 480);
        assertEquals(0, boss.nullRockSideReuseTimer);
        assertFalse(VultureSpecials.canStart(boss, false, false),
                "The boss AI should retain its rare shared special cadence.");
    }

    @Test
    void nullRockEyeLaserRenderAvoidsFullMapPixelEffects() throws Exception {
        String source = Files.readString(Path.of(
                "src", "main", "java", "com", "example", "birdgame3", "Bird.java"));
        int laserStart = source.indexOf("if (nullRockLaserTimer <= 0) return;");
        int laserEnd = source.indexOf("private void drawNullRockBloodSpear", laserStart);

        assertTrue(laserStart >= 0 && laserEnd > laserStart);
        String laserRender = source.substring(laserStart, laserEnd);
        assertFalse(laserRender.contains("setEffect("),
                "Large JavaFX pixel effects on the full eye beam can freeze the render thread.");
        assertTrue(laserRender.contains("length + 900.0"),
                "The visible beam should stop shortly beyond its target instead of always drawing across the map.");
    }

    @Test
    void nullRockUpSpecialLiftsBossAndMaintainsThreeAiVultureHenchmen() throws Exception {
        BirdGame3 game = new BirdGame3();
        game.activePlayers = 2;
        Bird boss = new Bird(700.0, BirdGame3.BirdType.VULTURE, 0, game);
        Bird enemy = new Bird(1050.0, BirdGame3.BirdType.PIGEON, 1, game);
        boss.isNullRockSkin = true;
        game.players[0] = boss;
        game.players[1] = enemy;

        VultureSpecials.nullRockUp(boss, false);

        assertEquals(Bird.NULL_ROCK_LIFT_FRAMES, boss.nullRockLiftTimer);
        assertTrue(boss.vy < 0.0);
        assertEquals(3, VultureSpecials.ownedNullRockHenchmanCount(boss));
        assertTrue(game.crowMinions.stream().allMatch(crow -> crow.owner == boss
                && crow.effectiveVariant() == CrowMinion.VARIANT_VULTURE_HENCHMAN
                && crow.life >= 5));

        VultureSpecials.nullRockUp(boss, false);
        assertEquals(3, VultureSpecials.ownedNullRockHenchmanCount(boss),
                "Repeated lifts should replenish the trio, not flood the arena.");

        CrowMinion hunter = game.crowMinions.getFirst();
        hunter.x = enemy.bodyCenterX();
        hunter.y = enemy.bodyCenterY();
        hunter.vx = 2.0;
        hunter.vy = 0.0;
        double healthBefore = enemy.health;
        invokePrivateVoid(game, "updateWorldFixed");

        assertTrue(enemy.health < healthBefore);
        assertTrue(game.crowMinions.contains(hunter),
                "A Vulture henchman should survive contact and continue hunting like an AI companion.");
        assertTrue(hunter.contactCooldown > 0);
    }

    @Test
    void nullRockDownSpecialRainsTelegraphedSpearsAcrossTargetLane() {
        BirdGame3 game = new BirdGame3();
        game.activePlayers = 2;
        Bird boss = new Bird(500.0, BirdGame3.BirdType.VULTURE, 0, game);
        Bird target = new Bird(1120.0, BirdGame3.BirdType.PIGEON, 1, game);
        boss.isNullRockSkin = true;
        boss.y = BirdGame3.GROUND_Y - boss.bodyHeight();
        target.y = BirdGame3.GROUND_Y - target.bodyHeight();
        game.players[0] = boss;
        game.players[1] = target;

        double healthBefore = target.health;
        VultureSpecials.nullRockDown(boss, false);

        assertEquals(5, boss.nullRockSpearCount);
        assertTrue(boss.nullRockSpearDelay[2] > 0, "Spear rain should expose a dodge telegraph before impact.");
        for (int frame = 0; frame < Bird.NULL_ROCK_SPEAR_FRAMES; frame++) {
            VultureSpecials.handleState(boss, false);
        }

        assertTrue(target.health < healthBefore);
        assertTrue(target.vy > 0.0, "A falling blood spear should spike its victim downward.");
    }

    @Test
    void nullRockNeutralKeepsDarkFlockAndBossCooldownScalesWithDifficulty() throws Exception {
        BirdGame3 game = new BirdGame3();
        game.activePlayers = 2;
        Bird boss = new Bird(500.0, BirdGame3.BirdType.VULTURE, 0, game);
        Bird target = new Bird(900.0, BirdGame3.BirdType.PIGEON, 1, game);
        boss.isNullRockSkin = true;
        game.players[0] = boss;
        game.players[1] = target;

        VultureSpecials.nullRockNeutral(boss, false);
        assertEquals(7, game.crowMinions.size());
        assertTrue(game.crowMinions.stream().noneMatch(crow ->
                crow.effectiveVariant() == CrowMinion.VARIANT_VULTURE_HENCHMAN));

        game.isAI[0] = true;
        int[] levels = (int[]) getPrivateObject(game, "cpuLevels");
        levels[0] = 1;
        int easyCooldown = VultureSpecials.nullRockSpecialCooldown(boss, false);
        levels[0] = 9;
        int hardestCooldown = VultureSpecials.nullRockSpecialCooldown(boss, false);

        assertTrue(easyCooldown > hardestCooldown);
        assertTrue(easyCooldown >= 18 * 60, "Low difficulty should keep boss specials rare.");
        assertTrue(hardestCooldown <= 11 * 60, "High difficulty should use the full kit more often.");
    }

    @Test
    void crowContactLaunchesMoreSidewaysThanUpward() throws Exception {
        BirdGame3 game = new BirdGame3();
        game.activePlayers = 2;

        Bird target = new Bird(220.0, BirdGame3.BirdType.PIGEON, 0, game);
        Bird bystander = new Bird(980.0, BirdGame3.BirdType.EAGLE, 1, game);
        target.y = BirdGame3.GROUND_Y - 80.0;
        bystander.y = BirdGame3.GROUND_Y - 80.0;
        game.players[0] = target;
        game.players[1] = bystander;

        CrowMinion crow = new CrowMinion(target.x + 16.0, target.y + 40.0, target);
        crow.vx = 3.2;
        crow.vy = 0.0;
        game.crowMinions.add(crow);

        invokePrivateVoid(game, "updateWorldFixed");

        assertTrue(game.crowMinions.isEmpty());
        assertTrue(target.health < Bird.STARTING_HEALTH);
        assertTrue(target.vx > Math.abs(target.vy),
                "Crow contact should shove targets sideways more than it launches them upward.");
    }

    @Test
    void anchoredCrowContactLaunchesMoreSidewaysThanUpward() throws Exception {
        BirdGame3 game = new BirdGame3();
        game.activePlayers = 2;

        Bird target = new Bird(220.0, BirdGame3.BirdType.PIGEON, 0, game);
        Bird bystander = new Bird(980.0, BirdGame3.BirdType.EAGLE, 1, game);
        target.y = BirdGame3.GROUND_Y - 80.0;
        bystander.y = BirdGame3.GROUND_Y - 80.0;
        game.players[0] = target;
        game.players[1] = bystander;

        CrowMinion crow = new CrowMinion(target.x + 16.0, target.y + 40.0, target)
                .withAnchorGuard(target.x + 40.0, target.y + 40.0, 120.0, 20);
        crow.vx = 0.0;
        crow.vy = 0.0;
        game.crowMinions.add(crow);

        invokePrivateVoid(game, "updateWorldFixed");

        assertTrue(game.crowMinions.isEmpty());
        assertTrue(target.health < Bird.STARTING_HEALTH);
        assertTrue(target.vx > Math.abs(target.vy),
                "Bone-guarding crows should shove targets sideways more than they launch them upward.");
    }

    @Test
    void vultureDamageTuningAppliesToFreeAndAnchoredCrows() throws Exception {
        double originalVultureDamage = BirdGame3.BirdType.VULTURE.damageDealtMult;
        double originalPigeonDamageTaken = BirdGame3.BirdType.PIGEON.damageTakenMult;
        try {
            BirdGame3.BirdType.VULTURE.damageDealtMult = 0.5;
            BirdGame3.BirdType.PIGEON.damageTakenMult = 1.0;

            assertEquals(0.5, playOwnedVultureCrowHit(false), 0.0001,
                    "Free-flying crow contact should inherit Vulture's outgoing damage tuning.");
            assertEquals(0.5, playOwnedVultureCrowHit(true), 0.0001,
                    "Bone Offering crow contact should inherit Vulture's outgoing damage tuning.");
        } finally {
            BirdGame3.BirdType.VULTURE.damageDealtMult = originalVultureDamage;
            BirdGame3.BirdType.PIGEON.damageTakenMult = originalPigeonDamageTaken;
        }
    }

    @Test
    void ownedVultureCrowLaunchInheritsOwnerDamageTuningButNullRockDoesNot() throws Exception {
        double originalVultureDamage = BirdGame3.BirdType.VULTURE.damageDealtMult;
        double originalPigeonDamageTaken = BirdGame3.BirdType.PIGEON.damageTakenMult;
        try {
            BirdGame3.BirdType.VULTURE.damageDealtMult = 0.5;
            BirdGame3.BirdType.PIGEON.damageTakenMult = 1.0;
            BirdGame3 game = new BirdGame3();
            game.activePlayers = 2;
            Bird owner = new Bird(980.0, BirdGame3.BirdType.VULTURE, 0, game);
            Bird target = new Bird(220.0, BirdGame3.BirdType.PIGEON, 1, game);
            owner.y = BirdGame3.GROUND_Y - 80.0;
            target.y = BirdGame3.GROUND_Y - 80.0;
            game.players[0] = owner;
            game.players[1] = target;

            CrowMinion crow = new CrowMinion(target.x + 16.0, target.y + 40.0, target);
            crow.owner = owner;
            crow.vx = 0.0;
            crow.vy = 0.0;
            game.crowMinions.add(crow);
            invokePrivateVoid(game, "updateWorldFixed");

            assertEquals(0.5, crow.ownerLaunchMultiplier(), 0.0001);
            assertEquals(0.5, crow.contactLaunchMultiplier(), 0.0001,
                    "Free-flying crows should use only Vulture's outgoing tuning.");
            assertEquals((crow.vx * 0.95 + 2.8) * 0.5, target.vx, 0.0001,
                    "Player Vulture's crow shove should inherit his outgoing tuning.");
            assertEquals(-2.1, target.vy, 0.0001,
                    "Player Vulture's crow lift should inherit his outgoing tuning.");

            CrowMinion baitCrow = new CrowMinion(0.0, 0.0, null)
                    .withAnchorGuard(0.0, 0.0, 120.0, 20);
            baitCrow.owner = owner;
            assertEquals(0.5 * Bird.VULTURE_BAIT_CROW_LAUNCH_MULTIPLIER,
                    baitCrow.contactLaunchMultiplier(), 0.0001,
                    "Bone Offering crows should add their focused launch reduction.");

            owner.isNullRockSkin = true;
            CrowMinion bossCrow = new CrowMinion(0.0, 0.0, null);
            bossCrow.owner = owner;
            assertEquals(1.0, bossCrow.ownerLaunchMultiplier(), 0.0001,
                    "Null Rock's boss flock must remain independent of player balance tuning.");
            CrowMinion bossAnchorCrow = new CrowMinion(0.0, 0.0, null)
                    .withAnchorGuard(0.0, 0.0, 120.0, 20);
            bossAnchorCrow.owner = owner;
            assertEquals(1.0, bossAnchorCrow.contactLaunchMultiplier(), 0.0001,
                    "Null Rock's anchored boss summons must remain independent of player balance tuning.");
        } finally {
            BirdGame3.BirdType.VULTURE.damageDealtMult = originalVultureDamage;
            BirdGame3.BirdType.PIGEON.damageTakenMult = originalPigeonDamageTaken;
        }
    }

    @Test
    void ownedVultureCrowCreditsDamageAndHealthModeKoWithoutBuildingUltimate() throws Exception {
        BirdGame3 game = new BirdGame3();
        game.activePlayers = 2;
        game.headlessHarnessMode = true;

        Bird owner = new Bird(980.0, BirdGame3.BirdType.VULTURE, 0, game);
        Bird target = new Bird(220.0, BirdGame3.BirdType.PIGEON, 1, game);
        owner.y = BirdGame3.GROUND_Y - 80.0;
        target.y = BirdGame3.GROUND_Y - 80.0;
        target.health = 0.5;
        game.players[0] = owner;
        game.players[1] = target;

        CrowMinion crow = new CrowMinion(target.x + 16.0, target.y + 40.0, target);
        crow.owner = owner;
        crow.vx = 0.0;
        crow.vy = 0.0;
        game.crowMinions.add(crow);

        invokePrivateVoid(game, "updateWorldFixed");

        assertEquals(1, game.damageDealt[0], "Crow damage should appear in its owner's results statistics.");
        assertEquals(1, game.eliminations[0], "A lethal crow hit should credit its owner with the KO.");
        assertEquals(0.0, owner.getUltimateRatio(), 0.0001,
                "Passive crow damage must not feed Vulture's next ultimate summon cycle.");
        assertEquals(0.0, target.getUltimateRatio(), 0.0001,
                "Passive crow contact should not build the victim's ultimate meter either.");
    }

    @Test
    void ownedVultureCrowRegistersSmashLaunchAndKoCredit() throws Exception {
        BirdGame3 game = new BirdGame3();
        game.activePlayers = 2;
        game.headlessHarnessMode = true;
        setPrivateBoolean(game);
        game.scores[0] = 3;
        game.scores[1] = 3;

        Bird owner = new Bird(980.0, BirdGame3.BirdType.VULTURE, 0, game);
        Bird target = new Bird(220.0, BirdGame3.BirdType.PIGEON, 1, game);
        owner.y = BirdGame3.GROUND_Y - 80.0;
        target.y = BirdGame3.GROUND_Y - 80.0;
        setPrivateDouble(target, "smashDamage", 120.0);
        game.players[0] = owner;
        game.players[1] = target;

        CrowMinion crow = new CrowMinion(target.x + 16.0, target.y + 40.0, target);
        crow.owner = owner;
        crow.vx = 3.2;
        crow.vy = 0.0;
        game.crowMinions.add(crow);

        invokePrivateVoid(game, "updateWorldFixed");

        assertEquals(0, getPrivateInt(target, "recentSmashAttackerIndex"),
                "A crow launch should preserve its owner's blast-zone KO credit.");
        assertTrue(getPrivateDouble(target, "pendingSmashLaunchScale") > 1.0,
                "Crow knockback should scale with the victim's Smash damage like other attacks.");
        assertEquals(1, game.damageDealt[0]);
        assertEquals(0, game.eliminations[0], "Smash KOs are credited only after crossing a blast zone.");
    }

    private static double playOwnedVultureCrowHit(boolean anchored) throws Exception {
        BirdGame3 game = new BirdGame3();
        game.activePlayers = 2;

        Bird owner = new Bird(980.0, BirdGame3.BirdType.VULTURE, 0, game);
        Bird target = new Bird(220.0, BirdGame3.BirdType.PIGEON, 1, game);
        owner.y = BirdGame3.GROUND_Y - 80.0;
        target.y = BirdGame3.GROUND_Y - 80.0;
        game.players[0] = owner;
        game.players[1] = target;

        CrowMinion crow = new CrowMinion(target.x + 16.0, target.y + 40.0, target);
        crow.owner = owner;
        crow.vx = 0.0;
        crow.vy = 0.0;
        if (anchored) {
            crow.withAnchorGuard(target.x + 40.0, target.y + 40.0, 120.0, 20);
        }
        game.crowMinions.add(crow);

        double startingHealth = target.health;
        invokePrivateVoid(game, "updateWorldFixed");

        assertTrue(game.crowMinions.isEmpty());
        return startingHealth - target.health;
    }

    @Test
    void localAndAiInputsStaySeparated() {
        BirdGame3 game = new BirdGame3();
        game.activePlayers = 2;
        game.isAI[1] = true;

        game.setLocalActionsForKey(KeyCode.ENTER, true);
        assertFalse(game.isAttackPressed(1));

        game.setAiControlKey(1, game.attackKeyForPlayer(1), true);
        assertTrue(game.isAttackPressed(1));

        game.setLocalActionsForKey(KeyCode.ENTER, false);
        assertTrue(game.isAttackPressed(1));

        game.clearGameplayInputs();
        assertFalse(game.isAttackPressed(1));
    }

    @Test
    void sharedKeyboardAliasesMirrorTwoPlayerLocalInputs() {
        BirdGame3 game = new BirdGame3();
        game.activePlayers = 2;

        game.setLocalActionsForKey(KeyCode.Y, true);
        game.setLocalActionsForKey(KeyCode.O, true);

        assertTrue(game.isAttackPressed(0));
        assertTrue(game.isAttackPressed(1));

        game.setLocalActionsForKey(KeyCode.Y, false);
        game.setLocalActionsForKey(KeyCode.O, false);

        assertFalse(game.isAttackPressed(0));
        assertFalse(game.isAttackPressed(1));
    }

    @Test
    void giantCrowSurvivesHitWithKnockbackAndParticles() throws Exception {
        BirdGame3 game = new BirdGame3();
        game.activePlayers = 1;

        Bird attacker = new Bird(100.0, BirdGame3.BirdType.PIGEON, 0, game);
        attacker.facingRight = true;
        game.players[0] = attacker;

        CrowMinion giantCrow = new CrowMinion(attacker.x + 110.0, attacker.y + 40.0, null)
                .withVariant(CrowMinion.VARIANT_GIANT_CROW);
        giantCrow.vx = 0.0;
        giantCrow.vy = 0.0;
        game.crowMinions.add(giantCrow);

        invokePrivateVoid(attacker, "attack");

        assertEquals(1, game.crowMinions.size());
        CrowMinion survivor = game.crowMinions.getFirst();
        assertEquals(2, survivor.life);
        assertTrue(Math.abs(survivor.vx) > 0.1);
        assertTrue(survivor.vy < 0.0);
        assertTrue(survivor.hitFlashTimer > 0);
        assertTrue(game.particles.size() >= 6);
    }

    @Test
    void groundedSmashAttackBuildsMuchStrongerKnockbackThanSideTilt() {
        double tapKnockback = attackKnockbackAfterHoldingForFrames(1);
        double chargedKnockback = attackKnockbackAfterHoldingForFrames(36);

        assertTrue(chargedKnockback > tapKnockback * 2.2,
                () -> "Charged smash attacks should launch much harder than a quick side tilt"
                        + " (tap=" + tapKnockback + ", charged=" + chargedKnockback + ").");
    }

    @Test
    void fullyChargedSideSmashDoesNotTakeAZeroPercentBattlefieldStock() throws Exception {
        for (BirdGame3.BirdType attackerType : BirdGame3.BirdType.values()) {
            BirdGame3 game = battlefieldSmashTestGame(attackerType, BirdGame3.BirdType.EAGLE);
            Bird attacker = game.players[0];
            Bird target = game.players[1];

            performFullSideSmash(attacker);
            advanceLaunchedBird(target, 18);

            assertEquals(3, game.scores[1], () -> attackerType.name
                    + " must not force a zero-percent stock loss from Battlefield center"
                    + " (x=" + target.x + ", y=" + target.y + ", vx=" + target.vx + ", vy=" + target.vy
                    + ", feed=" + game.killFeed + ").");

            game.setLocalActionsForKey(game.leftKeyForPlayer(1), true);
            for (int frame = 0; frame < 45 && game.scores[1] == 3 && target.vx > 0.0; frame++) {
                target.update(1.0);
            }

            assertEquals(3, game.scores[1], () -> attackerType.name
                    + " must leave enough time to steer back after zero-percent launch hitstun.");
            assertTrue(target.vx <= 0.0, () -> attackerType.name
                    + " must let the defender reverse horizontal momentum after hitstun.");
        }
    }

    @Test
    void fullyChargedSideSmashStillFinishesAHighPercentBattlefieldStock() throws Exception {
        BirdGame3 game = battlefieldSmashTestGame(BirdGame3.BirdType.PIGEON, BirdGame3.BirdType.EAGLE);
        Bird attacker = game.players[0];
        Bird target = game.players[1];
        setPrivateDouble(target, "smashDamage", 120.0);

        performFullSideSmash(attacker);
        advanceLaunchedBird(target, 18);

        assertEquals(2, game.scores[1],
                "A full side smash should remain a reliable finisher once the defender has high damage.");
    }

    @Test
    void fullyChargedUpSmashDoesNotTakeAZeroPercentBattlefieldStock() throws Exception {
        for (BirdGame3.BirdType attackerType : BirdGame3.BirdType.values()) {
            BirdGame3 game = battlefieldSmashTestGame(attackerType, BirdGame3.BirdType.EAGLE);
            Bird attacker = game.players[0];
            Bird target = game.players[1];

            performFullSmash(attacker, "UP_SMASH");
            advanceLaunchedBird(target, 60);

            assertTrue(target.smashDamagePercent() > 0.0,
                    () -> attackerType.name + " up smash must connect in the safety test.");
            assertEquals(3, game.scores[1], () -> attackerType.name
                    + " up smash must not take a zero-percent Battlefield stock.");
        }
    }

    @Test
    void fullyChargedUpSmashStillFinishesAHighPercentBattlefieldStock() throws Exception {
        BirdGame3 game = battlefieldSmashTestGame(BirdGame3.BirdType.PIGEON, BirdGame3.BirdType.EAGLE);
        Bird target = game.players[1];
        setPrivateDouble(target, "smashDamage", 120.0);

        performFullSmash(game.players[0], "UP_SMASH");
        advanceLaunchedBird(target, 60);

        assertEquals(2, game.scores[1],
                "A full up smash should remain a reliable vertical finisher at high damage.");
    }

    @Test
    void fullyChargedDownSmashDoesNotTakeAZeroPercentBattlefieldStock() throws Exception {
        for (BirdGame3.BirdType attackerType : BirdGame3.BirdType.values()) {
            BirdGame3 game = battlefieldSmashTestGame(attackerType, BirdGame3.BirdType.EAGLE);
            Bird attacker = game.players[0];
            Bird target = game.players[1];

            performFullSmash(attacker, "DOWN_SMASH");
            advanceLaunchedBird(target, 18);

            assertTrue(target.smashDamagePercent() > 0.0,
                    () -> attackerType.name + " down smash must connect in the safety test.");
            assertEquals(3, game.scores[1], () -> attackerType.name
                    + " down smash must not force a zero-percent Battlefield stock during hitstun.");

            game.setLocalActionsForKey(game.leftKeyForPlayer(1), true);
            for (int frame = 0; frame < 45 && game.scores[1] == 3 && target.vx > 0.0; frame++) {
                target.update(1.0);
            }

            assertEquals(3, game.scores[1], () -> attackerType.name
                    + " down smash must leave enough time to steer back at zero percent.");
            assertTrue(target.vx <= 0.0, () -> attackerType.name
                    + " down smash must let the defender reverse momentum after hitstun.");
        }
    }

    @Test
    void fullyChargedDownSmashStillFinishesAHighPercentBattlefieldStock() throws Exception {
        BirdGame3 game = battlefieldSmashTestGame(BirdGame3.BirdType.PIGEON, BirdGame3.BirdType.EAGLE);
        Bird target = game.players[1];
        setPrivateDouble(target, "smashDamage", 120.0);

        performFullSmash(game.players[0], "DOWN_SMASH");
        advanceLaunchedBird(target, 24);

        assertEquals(2, game.scores[1],
                "A full down smash should remain a reliable horizontal finisher at high damage.");
    }

    @Test
    void groundedAttackBiasesKnockbackHorizontally() throws Exception {
        BirdGame3 game = new BirdGame3();
        game.activePlayers = 2;

        Bird attacker = new Bird(100.0, BirdGame3.BirdType.PIGEON, 0, game);
        Bird target = new Bird(190.0, BirdGame3.BirdType.EAGLE, 1, game);
        attacker.y = BirdGame3.GROUND_Y - 80.0;
        target.y = BirdGame3.GROUND_Y - 80.0;
        attacker.facingRight = true;
        game.players[0] = attacker;
        game.players[1] = target;

        invokePrivateVoid(attacker, "attack");

        assertTrue(target.vx > 0.0, "Attack should still push the target forward.");
        assertTrue(target.vy < 0.0, "Attack should still pop the target upward.");
        assertTrue(target.vx > Math.abs(target.vy) * 2.0,
                "Neutral ground attacks should favor horizontal knockback without suppressing vertical launch.");
    }

    @Test
    void groundedTiltInputsProduceDistinctSideAndUpTilts() throws Exception {
        BirdGame3 sideGame = new BirdGame3();
        sideGame.activePlayers = 2;

        Bird sideAttacker = new Bird(100.0, BirdGame3.BirdType.PIGEON, 0, sideGame);
        Bird sideTarget = new Bird(190.0, BirdGame3.BirdType.EAGLE, 1, sideGame);
        sideAttacker.y = BirdGame3.GROUND_Y - 80.0;
        sideTarget.y = BirdGame3.GROUND_Y - 80.0;
        sideAttacker.facingRight = true;
        sideGame.players[0] = sideAttacker;
        sideGame.players[1] = sideTarget;
        sideGame.setLocalActionsForKey(sideGame.rightKeyForPlayer(0), true);

        invokePrivateVoid(sideAttacker, "attack");

        BirdGame3 upGame = new BirdGame3();
        upGame.activePlayers = 2;

        Bird upAttacker = new Bird(100.0, BirdGame3.BirdType.PIGEON, 0, upGame);
        Bird upTarget = new Bird(190.0, BirdGame3.BirdType.EAGLE, 1, upGame);
        upAttacker.y = BirdGame3.GROUND_Y - 80.0;
        upTarget.y = BirdGame3.GROUND_Y - 80.0;
        upAttacker.facingRight = true;
        upGame.players[0] = upAttacker;
        upGame.players[1] = upTarget;
        upGame.setLocalActionsForKey(upGame.jumpKeyForPlayer(0), true);

        invokePrivateVoid(upAttacker, "attack");

        assertTrue(sideTarget.vx > 0.0);
        assertTrue(upTarget.vx > 0.0);
        assertTrue(sideTarget.vx > upTarget.vx * 1.4,
                "Side normals should launch much farther horizontally than up normals.");
        assertTrue(Math.abs(upTarget.vy) > Math.abs(sideTarget.vy) * 1.8,
                "Up normals should launch much higher than side normals.");
    }

    @Test
    void attackPlusBlockPerformsGroundDownTiltInsteadOfShielding() throws Exception {
        BirdGame3 game = new BirdGame3();
        game.activePlayers = 2;

        Bird attacker = new Bird(100.0, BirdGame3.BirdType.PIGEON, 0, game);
        Bird target = new Bird(190.0, BirdGame3.BirdType.EAGLE, 1, game);
        attacker.y = BirdGame3.GROUND_Y - 80.0;
        target.y = BirdGame3.GROUND_Y - 80.0;
        attacker.facingRight = true;
        game.players[0] = attacker;
        game.players[1] = target;

        double startingHealth = target.health;
        game.setLocalActionsForKey(game.attackKeyForPlayer(0), true);
        game.setLocalActionsForKey(game.blockKeyForPlayer(0), true);

        attacker.update(1.0);

        assertFalse(attacker.isBlocking, "Attack + block should reserve the input for a down normal, not raise shield.");
        assertEquals(0, getPrivateInt(attacker, "attackChargeFrames"), "Down tilts should not enter smash charge.");

        game.setLocalActionsForKey(game.attackKeyForPlayer(0), false);
        attacker.update(1.0);
        advanceAuthoredAttackToFirstActiveFrame(attacker);

        assertTrue(target.health < startingHealth, "Quickly releasing attack + block should perform the grounded down tilt.");
        assertTrue(target.vx > 0.0);
        assertTrue(target.vy > -3.0, "Grounded down tilt should launch flatter than the default launcher.");
    }

    @Test
    void attackPlusBlockHeldLongEnoughChargesAndReleasesGroundDownSmash() throws Exception {
        BirdGame3 game = new BirdGame3();
        game.activePlayers = 2;

        Bird attacker = new Bird(100.0, BirdGame3.BirdType.PIGEON, 0, game);
        Bird target = new Bird(190.0, BirdGame3.BirdType.EAGLE, 1, game);
        attacker.y = BirdGame3.GROUND_Y - 80.0;
        target.y = BirdGame3.GROUND_Y - 80.0;
        attacker.facingRight = true;
        game.players[0] = attacker;
        game.players[1] = target;

        double startingHealth = target.health;
        game.setLocalActionsForKey(game.attackKeyForPlayer(0), true);
        game.setLocalActionsForKey(game.blockKeyForPlayer(0), true);

        for (int i = 0; i < 8; i++) {
            attacker.update(1.0);
        }

        assertFalse(attacker.isBlocking, "Holding attack + block for a down smash should not raise shield.");
        assertTrue(getPrivateInt(attacker, "attackChargeFrames") > 0, "Holding the input should convert the grounded down attack into smash charge.");

        game.setLocalActionsForKey(game.attackKeyForPlayer(0), false);
        attacker.update(1.0);
        advanceAuthoredAttackToFirstActiveFrame(attacker);

        assertTrue(target.health < startingHealth, "Releasing after the hold should perform the down smash.");
        assertTrue(target.vx > 0.0);
        assertTrue(Math.abs(target.vx) > 9.0, "Down smash should launch harder than a down tilt.");
    }

    @Test
    void aerialBackAirLaunchesBehindTheAttacker() {
        BirdGame3 game = new BirdGame3();
        game.activePlayers = 2;

        Bird attacker = new Bird(200.0, BirdGame3.BirdType.PIGEON, 0, game);
        Bird target = new Bird(120.0, BirdGame3.BirdType.EAGLE, 1, game);
        attacker.y = BirdGame3.GROUND_Y - 280.0;
        target.y = BirdGame3.GROUND_Y - 280.0;
        attacker.facingRight = true;
        game.players[0] = attacker;
        game.players[1] = target;

        game.setLocalActionsForKey(game.leftKeyForPlayer(0), true);
        game.setLocalActionsForKey(game.attackKeyForPlayer(0), true);

        attacker.update(1.0);
        advanceAuthoredAttackToFirstActiveFrame(attacker);

        assertTrue(target.vx < 0.0, "Holding back in the air should create a back air that launches behind the bird.");
        assertTrue(target.health < Bird.STARTING_HEALTH);
    }

    @Test
    void damageScaledAttackKeepsAUsefulHorizontalLaunchAngle() throws Exception {
        BirdGame3 game = new BirdGame3();
        game.activePlayers = 2;
        setPrivateBoolean(game);

        Bird attacker = new Bird(100.0, BirdGame3.BirdType.PIGEON, 0, game);
        Bird target = new Bird(190.0, BirdGame3.BirdType.EAGLE, 1, game);
        attacker.y = BirdGame3.GROUND_Y - 80.0;
        target.y = BirdGame3.GROUND_Y - 80.0;
        attacker.facingRight = true;
        game.players[0] = attacker;
        game.players[1] = target;

        invokePrivateVoid(attacker, "attack");
        invokePrivateVoid(target, "applyPendingSmashLaunch");

        assertTrue(target.vx > 0.0, "Damage-scaled hit should still push the target forward.");
        assertTrue(target.vy < 0.0, "Damage-scaled hit should still launch the target upward.");
        assertTrue(target.vx > Math.abs(target.vy) * 2.0,
                "Damage scaling should preserve the move's horizontal launch identity.");
    }

    @Test
    void highPercentSideTiltCreatesRealLaunchAndHitstun() throws Exception {
        BirdGame3 game = new BirdGame3();
        game.activePlayers = 2;
        setPrivateBoolean(game);

        Bird attacker = new Bird(100.0, BirdGame3.BirdType.PIGEON, 0, game);
        Bird target = new Bird(190.0, BirdGame3.BirdType.EAGLE, 1, game);
        attacker.y = BirdGame3.GROUND_Y - 80.0;
        target.y = BirdGame3.GROUND_Y - 80.0;
        attacker.facingRight = true;
        game.players[0] = attacker;
        game.players[1] = target;
        setPrivateDouble(target, "smashDamage", 160.0);
        game.setLocalActionsForKey(game.rightKeyForPlayer(0), true);

        invokePrivateVoid(attacker, "attack");
        invokePrivateVoid(target, "applyPendingSmashLaunch");

        assertTrue(target.vx > 45.0,
                "A side tilt at high damage should create enough speed to threaten a side blast zone.");
        assertTrue(target.stunTime >= 12.0,
                "The defender should remain in launch hitstun instead of immediately steering out of the hit.");
    }

    @Test
    void launchPredictionFlagsAnUnavoidableSideBlastTrajectory() throws Exception {
        BirdGame3 game = new BirdGame3();
        game.activePlayers = 1;
        setPrivateBoolean(game);
        Bird bird = new Bird(BirdGame3.WORLD_WIDTH + 235.0,
                BirdGame3.BirdType.PIGEON, 0, game);
        bird.y = BirdGame3.GROUND_Y - 520.0;
        bird.vx = 31.0;
        bird.vy = -3.0;
        bird.stunTime = 28.0;
        game.players[0] = bird;
        setPrivateDouble(bird, "pendingSmashLaunchScale", 2.0);
        setPrivateDouble(bird, "pendingDamageScaledHitDamage", 18.0);

        invokePrivateVoid(bird, "applyPendingSmashLaunch");

        assertTrue(bird.debugProjectedLaunchKo());
        assertTrue(bird.debugProjectedKoTelemetryLabel().contains("RIGHT BLAST ZONE"));
        assertTrue(bird.debugProjectedKoFrames() <= 3);
    }

    @Test
    void launchPredictionDoesNotCallARecoverableCenterStageHitADeath() throws Exception {
        BirdGame3 game = new BirdGame3();
        game.activePlayers = 1;
        setPrivateBoolean(game);
        Bird bird = new Bird(BirdGame3.WORLD_WIDTH * 0.5,
                BirdGame3.BirdType.PIGEON, 0, game);
        bird.y = BirdGame3.GROUND_Y - 420.0;
        bird.vx = 7.0;
        bird.vy = -3.0;
        bird.stunTime = 18.0;
        game.players[0] = bird;
        setPrivateDouble(bird, "pendingSmashLaunchScale", 1.6);
        setPrivateDouble(bird, "pendingDamageScaledHitDamage", 10.0);

        invokePrivateVoid(bird, "applyPendingSmashLaunch");

        assertFalse(bird.debugProjectedLaunchKo());
        assertEquals("SAFE / RECOVERABLE", bird.debugProjectedKoTelemetryLabel());
    }

    @Test
    void launchPredictionStopsAtARealLandingSurface() throws Exception {
        BirdGame3 game = new BirdGame3();
        game.activePlayers = 1;
        setPrivateBoolean(game);
        Bird bird = new Bird(BirdGame3.WORLD_WIDTH * 0.5,
                BirdGame3.BirdType.PIGEON, 0, game);
        bird.y = BirdGame3.GROUND_Y - bird.bodyHeight() - 4.0;
        bird.vx = 28.0;
        bird.vy = 5.0;
        bird.stunTime = 30.0;
        game.players[0] = bird;
        setPrivateDouble(bird, "pendingSmashLaunchScale", 1.5);
        setPrivateDouble(bird, "pendingDamageScaledHitDamage", 14.0);

        invokePrivateVoid(bird, "applyPendingSmashLaunch");

        assertTrue(Math.hypot(bird.vx, bird.vy) > 19.0);
        assertFalse(bird.debugProjectedLaunchKo(),
                "A landing on the authored ground should terminate the fatal-trajectory estimate.");
    }

    @Test
    void launchTrailsAppearOnlyForFastTumbleAndExpireOnFixedTicks() throws Exception {
        BirdGame3 game = new BirdGame3();
        game.activePlayers = 1;
        setPrivateBoolean(game);
        Bird bird = new Bird(900.0, BirdGame3.BirdType.EAGLE, 0, game);
        bird.y = BirdGame3.GROUND_Y - 500.0;
        bird.vx = 26.0;
        bird.vy = -9.0;
        game.players[0] = bird;
        setPrivateInt(bird, "tumbleTimer", 30);

        invokePrivateVoid(game, "recordLaunchTrailSegmentsFixed");
        assertEquals(1, getPrivateCollectionSize(game, "launchTrailEffects"));

        for (int i = 0; i < 18; i++) {
            invokePrivateVoid(game, "updateLaunchTrailEffectsFixed");
        }
        assertEquals(0, getPrivateCollectionSize(game, "launchTrailEffects"));
    }

    @Test
    void highPercentUpTiltCanLaunchVertically() throws Exception {
        BirdGame3 game = new BirdGame3();
        game.activePlayers = 2;
        setPrivateBoolean(game);

        Bird attacker = new Bird(100.0, BirdGame3.BirdType.PIGEON, 0, game);
        Bird target = new Bird(190.0, BirdGame3.BirdType.EAGLE, 1, game);
        attacker.y = BirdGame3.GROUND_Y - 80.0;
        target.y = BirdGame3.GROUND_Y - 80.0;
        attacker.facingRight = true;
        game.players[0] = attacker;
        game.players[1] = target;
        setPrivateDouble(target, "smashDamage", 200.0);
        game.setLocalActionsForKey(game.jumpKeyForPlayer(0), true);

        invokePrivateVoid(attacker, "attack");
        invokePrivateVoid(target, "applyPendingSmashLaunch");

        assertTrue(target.vy < -23.0,
                "An up tilt at very high damage should be a credible vertical launcher.");
        assertTrue(target.stunTime >= 10.0);
    }

    @Test
    void damagingSpecialUsesTheSharedLaunchAndHitstunCurve() throws Exception {
        BirdGame3 game = new BirdGame3();
        game.activePlayers = 2;
        setPrivateBoolean(game);

        Bird attacker = new Bird(100.0, BirdGame3.BirdType.PIGEON, 0, game);
        Bird target = new Bird(190.0, BirdGame3.BirdType.EAGLE, 1, game);
        game.players[0] = attacker;
        game.players[1] = target;
        setPrivateDouble(target, "smashDamage", 180.0);

        attacker.applyTrackedSpecialDamage(target, 8);
        target.vx += 8.0;
        target.vy -= 5.0;
        invokePrivateVoid(target, "applyPendingSmashLaunch");

        assertTrue(target.vx > 20.0);
        assertTrue(target.vy < -10.0);
        assertTrue(target.stunTime >= 8.0,
                "Specials should use the same damage-based launch hitstun as normal attacks.");
    }

    @Test
    void damageScaledHitstunRetainsAirLaunchMomentum() throws Exception {
        BirdGame3 scaledGame = new BirdGame3();
        scaledGame.activePlayers = 1;
        setPrivateBoolean(scaledGame);
        Bird scaledTarget = new Bird(190.0, BirdGame3.BirdType.EAGLE, 0, scaledGame);
        scaledTarget.y = BirdGame3.GROUND_Y - 280.0;
        scaledTarget.vx = 20.0;
        scaledTarget.stunTime = 10.0;
        scaledGame.players[0] = scaledTarget;

        BirdGame3 legacyGame = new BirdGame3();
        legacyGame.activePlayers = 1;
        Bird legacyTarget = new Bird(190.0, BirdGame3.BirdType.EAGLE, 0, legacyGame);
        legacyTarget.y = BirdGame3.GROUND_Y - 280.0;
        legacyTarget.vx = 20.0;
        legacyTarget.stunTime = 10.0;
        legacyGame.players[0] = legacyTarget;

        scaledTarget.update(1.0);
        legacyTarget.update(1.0);

        assertTrue(scaledTarget.vx > 19.0,
                "Damage-scaled launch should retain most momentum during its first airborne frame.");
        assertTrue(scaledTarget.vx > legacyTarget.vx + 1.5,
                "The launch path should no longer apply both legacy stun drag and ordinary air friction.");
    }

    @Test
    void lightDamageWithoutKnockbackDoesNotCreateAStunlock() throws Exception {
        BirdGame3 game = new BirdGame3();
        game.activePlayers = 2;
        setPrivateBoolean(game);

        Bird attacker = new Bird(100.0, BirdGame3.BirdType.PIGEON, 0, game);
        Bird target = new Bird(190.0, BirdGame3.BirdType.EAGLE, 1, game);
        game.players[0] = attacker;
        game.players[1] = target;

        attacker.applyTrackedSpecialDamage(target, 2);
        invokePrivateVoid(target, "applyPendingSmashLaunch");

        assertEquals(0.0, target.stunTime, 0.0001,
                "Low-damage effects without a real launch must not repeatedly renew hitstun.");
    }

    @Test
    void highPercentSideTiltCanTakeAStockNearTheBlastZone() throws Exception {
        BirdGame3 game = new BirdGame3();
        game.activePlayers = 2;
        setPrivateBoolean(game);
        game.scores[0] = 3;
        game.scores[1] = 3;

        Bird attacker = new Bird(5600.0, BirdGame3.BirdType.PIGEON, 0, game);
        Bird target = new Bird(5690.0, BirdGame3.BirdType.EAGLE, 1, game);
        attacker.y = BirdGame3.GROUND_Y - 80.0;
        target.y = BirdGame3.GROUND_Y - 80.0;
        attacker.facingRight = true;
        game.players[0] = attacker;
        game.players[1] = target;
        setPrivateDouble(target, "smashDamage", 240.0);
        game.setLocalActionsForKey(game.rightKeyForPlayer(0), true);

        invokePrivateVoid(attacker, "attack");
        for (int frame = 0; frame < 20 && game.scores[1] == 3; frame++) {
            target.update(1.0);
        }

        assertEquals(2, game.scores[1],
                "A high-percent side tilt near the edge should launch through the side blast zone.");
        assertEquals(1, game.eliminations[0],
                "The ordinary attack should receive credit for the blast-zone KO.");
    }

    @Test
    void flatMapOuterWallsDoNotBounceLaunchedBirdsBackFromSideBlastZones() throws Exception {
        for (int direction : new int[]{-1, 1}) {
            BirdGame3 game = new BirdGame3();
            game.activePlayers = 1;
            game.selectedMap = BirdGame3.MapType.CITY;
            setPrivateBoolean(game);
            game.scores[0] = 3;
            game.platforms.add(new Platform(0.0, BirdGame3.GROUND_Y, BirdGame3.WORLD_WIDTH, 600.0));
            game.platforms.add(new Platform(-100.0, 0.0, 100.0, BirdGame3.WORLD_HEIGHT));
            game.platforms.add(new Platform(BirdGame3.WORLD_WIDTH, 0.0, 100.0, BirdGame3.WORLD_HEIGHT));

            double startX = direction < 0 ? 40.0 : BirdGame3.WORLD_WIDTH - 120.0;
            Bird launched = new Bird(startX, BirdGame3.BirdType.EAGLE, 0, game);
            launched.y = BirdGame3.GROUND_Y - launched.bodyHeight();
            launched.vx = direction * 90.0;
            launched.stunTime = 90.0;
            game.players[0] = launched;

            for (int frame = 0; frame < 12 && game.scores[0] == 3; frame++) {
                launched.update(1.0);
            }

            assertEquals(2, game.scores[0],
                    (direction < 0 ? "Left" : "Right")
                            + " world-limit wall must not rebound a launched fighter before the blast zone.");
        }
    }

    @Test
    void smashDirectionalInfluenceCanBendLaunchUpward() throws Exception {
        BirdGame3 baselineGame = new BirdGame3();
        baselineGame.activePlayers = 2;
        setPrivateBoolean(baselineGame);

        Bird baselineAttacker = new Bird(100.0, BirdGame3.BirdType.PIGEON, 0, baselineGame);
        Bird baselineTarget = new Bird(190.0, BirdGame3.BirdType.EAGLE, 1, baselineGame);
        baselineAttacker.y = BirdGame3.GROUND_Y - 80.0;
        baselineTarget.y = BirdGame3.GROUND_Y - 80.0;
        baselineAttacker.facingRight = true;
        baselineGame.players[0] = baselineAttacker;
        baselineGame.players[1] = baselineTarget;

        invokePrivateVoid(baselineAttacker, "attack");
        invokePrivateVoid(baselineTarget, "applyPendingSmashLaunch");

        BirdGame3 diGame = new BirdGame3();
        diGame.activePlayers = 2;
        setPrivateBoolean(diGame);

        Bird diAttacker = new Bird(100.0, BirdGame3.BirdType.PIGEON, 0, diGame);
        Bird diTarget = new Bird(190.0, BirdGame3.BirdType.EAGLE, 1, diGame);
        diAttacker.y = BirdGame3.GROUND_Y - 80.0;
        diTarget.y = BirdGame3.GROUND_Y - 80.0;
        diAttacker.facingRight = true;
        diGame.players[0] = diAttacker;
        diGame.players[1] = diTarget;

        diGame.setLocalActionsForKey(diGame.jumpKeyForPlayer(1), true);
        invokePrivateVoid(diAttacker, "attack");
        invokePrivateVoid(diTarget, "applyPendingSmashLaunch");

        assertTrue(diTarget.vx < baselineTarget.vx,
                "Holding up during launch should trade some forward speed for a steeper escape angle.");
        assertTrue(diTarget.vy < baselineTarget.vy,
                "Holding up during launch should angle the target farther upward.");
    }

    @Test
    void smashDirectionalInfluenceCanBendVerticalLaunchSideways() throws Exception {
        BirdGame3 game = new BirdGame3();
        game.activePlayers = 2;
        setPrivateBoolean(game);

        Bird target = new Bird(190.0, BirdGame3.BirdType.EAGLE, 1, game);
        game.players[1] = target;
        target.vx = 0.0;
        target.vy = -12.0;
        setPrivateDouble(target, "pendingSmashLaunchScale", 1.45);

        game.setLocalActionsForKey(game.rightKeyForPlayer(1), true);
        invokePrivateVoid(target, "applyPendingSmashLaunch");

        assertTrue(target.vx > 0.0, "Holding right during a vertical launch should bend the trajectory sideways.");
        assertTrue(target.vy < 0.0, "Directional influence should preserve upward launch on a vertical hit.");
    }

    @Test
    void launchCreatesPersistentTumbleAndTrainingTelemetry() throws Exception {
        BirdGame3 game = new BirdGame3();
        game.activePlayers = 2;
        setPrivateBoolean(game);

        Bird attacker = new Bird(100.0, BirdGame3.BirdType.PIGEON, 0, game);
        Bird target = new Bird(190.0, BirdGame3.BirdType.EAGLE, 1, game);
        game.players[0] = attacker;
        game.players[1] = target;
        target.vx = 18.0;
        target.vy = -9.0;
        setPrivateDouble(target, "pendingSmashLaunchScale", 1.8);
        setPrivateDouble(target, "pendingDamageScaledHitDamage", 12.0);

        invokePrivateVoid(target, "applyPendingSmashLaunch");

        assertTrue(target.debugInLaunchTumble());
        assertTrue(getPrivateInt(target, "tumbleTimer") > target.stunTime,
                "Launch tumble should outlast forced hitstun so aerial recovery has a readable window.");
        assertTrue(target.debugHitReactionTelemetryLabel().contains("TUMBLE"));
        assertTrue(target.debugLaunchTelemetryLabel().contains("ANGLE"));
    }

    @Test
    void smashDirectionalDisplacementUsesFreshInputEdgesOnly() throws Exception {
        BirdGame3 game = new BirdGame3();
        game.activePlayers = 1;
        setPrivateBoolean(game);
        Bird target = new Bird(300.0, BirdGame3.BirdType.EAGLE, 0, game);
        target.y = BirdGame3.GROUND_Y - 260.0;
        game.players[0] = target;
        game.setLocalActionsForKey(game.rightKeyForPlayer(0), true);

        target.vx = 12.0;
        target.vy = -6.0;
        setPrivateDouble(target, "pendingSmashLaunchScale", 1.4);
        setPrivateDouble(target, "pendingDamageScaledHitDamage", 8.0);
        double startX = target.x;
        invokePrivateVoid(target, "applyPendingSmashLaunch");
        double afterFreshEdge = target.x;

        assertEquals(startX + 7.0, afterFreshEdge, 0.0001);
        assertTrue(target.debugLaunchTelemetryLabel().contains("+7.0"));

        setPrivateObject(target, "rightHeldLastFrame", true);
        target.vx = 12.0;
        target.vy = -6.0;
        setPrivateDouble(target, "pendingSmashLaunchScale", 1.4);
        setPrivateDouble(target, "pendingDamageScaledHitDamage", 8.0);
        invokePrivateVoid(target, "applyPendingSmashLaunch");

        assertEquals(afterFreshEdge, target.x, 0.0001,
                "Holding a direction must not repeat SDI without a new deterministic input edge.");
    }

    @Test
    void actionableTumbleCanStillTechAHardLanding() throws Exception {
        BirdGame3 game = new BirdGame3();
        game.activePlayers = 1;
        setPrivateBoolean(game);
        Bird bird = new Bird(190.0, BirdGame3.BirdType.EAGLE, 0, game);
        bird.y = BirdGame3.GROUND_Y - 96.0;
        bird.vy = 18.0;
        bird.stunTime = 0.0;
        setPrivateInt(bird, "tumbleTimer", 30);
        game.players[0] = bird;
        game.setLocalActionsForKey(game.blockKeyForPlayer(0), true);

        for (int frame = 0; frame < 3 && !bird.isOnGround(); frame++) {
            bird.update(1.0);
        }

        assertTrue(bird.isOnGround());
        assertEquals(0, getPrivateInt(bird, "tumbleTimer"));
        assertEquals("SPOT", getPrivateObject(bird, "dodgeType").toString());
        assertTrue(bird.debugHitReactionTelemetryLabel().contains("GROUND"));
    }

    @Test
    void ceilingImpactsSupportTechsAndMeteorLaunchesAreReported() throws Exception {
        BirdGame3 game = new BirdGame3();
        game.activePlayers = 1;
        setPrivateBoolean(game);
        Platform ceiling = new Platform(100.0, 180.0, 420.0, 60.0);
        game.platforms.add(ceiling);
        Bird bird = new Bird(240.0, BirdGame3.BirdType.EAGLE, 0, game);
        game.players[0] = bird;

        bird.y = 225.0;
        bird.vx = 5.0;
        bird.vy = -15.0;
        setPrivateInt(bird, "tumbleTimer", 40);
        setPrivateInt(bird, "techBufferTimer", 6);
        invokePrivateVoid(bird, "handleCeilingTechCollision",
                new Class<?>[]{double.class, double.class}, 240.0, 255.0);

        assertEquals(ceiling.y + ceiling.h, bird.y, 0.0001);
        assertEquals(0.0, bird.vy, 0.0001);
        assertTrue(bird.debugHitReactionTelemetryLabel().contains("CEILING"));

        bird.vx = 3.0;
        bird.vy = 12.0;
        setPrivateDouble(bird, "pendingSmashLaunchScale", 1.5);
        setPrivateDouble(bird, "pendingDamageScaledHitDamage", 10.0);
        invokePrivateVoid(bird, "applyPendingSmashLaunch");

        assertTrue(bird.debugMeteorStateActive());
        assertTrue(bird.debugHitReactionTelemetryLabel().contains("METEOR"));
    }

    @Test
    void repeatedMoveUseStalesDamageAndLaunchDeterministically() throws Exception {
        BirdGame3 game = new BirdGame3();
        game.activePlayers = 2;
        setPrivateBoolean(game);
        Bird attacker = new Bird(100.0, BirdGame3.BirdType.PIGEON, 0, game);
        Bird target = new Bird(300.0, BirdGame3.BirdType.EAGLE, 1, game);
        game.players[0] = attacker;
        game.players[1] = target;

        int firstDamage = 0;
        int lastDamage = 0;
        double firstLaunch = 0.0;
        double lastLaunch = 0.0;
        for (int use = 0; use < Bird.STALE_MOVE_QUEUE_SIZE + 1; use++) {
            target.health = Bird.STARTING_HEALTH;
            setPrivateDouble(target, "smashDamage", 0.0);
            target.vx = 0.0;
            target.vy = 0.0;
            game.recordUltimateMoveUse(attacker, "Regression Repeater");
            int dealt = attacker.applyTrackedSpecialDamage(target, 20);
            target.vx += 20.0;
            target.vy -= 8.0;
            invokePrivateVoid(target, "applyPendingSmashLaunch");
            if (use == 0) {
                firstDamage = dealt;
                firstLaunch = Math.hypot(target.vx, target.vy);
            }
            if (use == Bird.STALE_MOVE_QUEUE_SIZE) {
                lastDamage = dealt;
                lastLaunch = Math.hypot(target.vx, target.vy);
            }
        }

        assertEquals(Bird.STALE_MOVE_QUEUE_SIZE, attacker.debugStaleMoveCount());
        assertTrue(lastDamage < firstDamage,
                "Nine repeated successful uses should meaningfully stale damage.");
        assertTrue(lastLaunch < firstLaunch,
                "Staling should also reduce launch so repeated finishers lose KO power.");
        assertEquals(0.55, attacker.debugCurrentStaleMoveMultiplier(), 0.0001);
        assertTrue(attacker.debugStaleMoveTelemetryLabel().contains("x0.55"));
    }

    @Test
    void oneMoveUseCommitsOnlyOnceAcrossMultipleTargetsAndShieldContact() throws Exception {
        BirdGame3 game = new BirdGame3();
        game.activePlayers = 3;
        setPrivateBoolean(game);
        Bird attacker = new Bird(100.0, BirdGame3.BirdType.PIGEON, 0, game);
        Bird first = new Bird(300.0, BirdGame3.BirdType.EAGLE, 1, game);
        Bird second = new Bird(500.0, BirdGame3.BirdType.FALCON, 2, game);
        game.players[0] = attacker;
        game.players[1] = first;
        game.players[2] = second;

        game.recordUltimateMoveUse(attacker, "Wide Regression Sweep");
        assertTrue(attacker.applyTrackedSpecialDamage(first, 10) > 0);
        assertTrue(attacker.applyTrackedSpecialDamage(second, 10) > 0);
        assertEquals(1, attacker.debugStaleMoveCount(),
                "A multi-target hit must occupy one stale slot, not one slot per victim.");

        second.health = Bird.STARTING_HEALTH;
        second.isBlocking = true;
        setPrivateDouble(second, "shieldHealth", 60.0);
        game.recordUltimateMoveUse(attacker, "Shield Regression Sweep");
        assertEquals(0, attacker.applyTrackedSpecialDamage(second, 10));
        assertEquals(2, attacker.debugStaleMoveCount(),
                "Connecting with a shield should commit the move to the stale queue.");
    }

    @Test
    void whiffsDoNotStaleAndVariedMovesRefreshOldEntries() throws Exception {
        BirdGame3 game = new BirdGame3();
        game.activePlayers = 2;
        setPrivateBoolean(game);
        Bird attacker = new Bird(100.0, BirdGame3.BirdType.PIGEON, 0, game);
        Bird target = new Bird(300.0, BirdGame3.BirdType.EAGLE, 1, game);
        game.players[0] = attacker;
        game.players[1] = target;

        game.recordUltimateMoveUse(attacker, "Whiffed Move");
        assertEquals(0, attacker.debugStaleMoveCount());

        game.recordUltimateMoveUse(attacker, "Primary Move");
        assertTrue(attacker.applyTrackedSpecialDamage(target, 10) > 0);
        for (int i = 0; i < Bird.STALE_MOVE_QUEUE_SIZE; i++) {
            target.health = Bird.STARTING_HEALTH;
            game.recordUltimateMoveUse(attacker, "Varied Move " + i);
            assertTrue(attacker.applyTrackedSpecialDamage(target, 10) > 0);
        }
        game.recordUltimateMoveUse(attacker, "Primary Move");

        assertEquals(1.0, attacker.debugCurrentStaleMoveMultiplier(), 0.0001,
                "Using the rest of the kit should naturally refresh an old move out of the queue.");
    }

    @Test
    void equalGroundedAttackBoxesClankWithoutDealingDamage() throws Exception {
        BirdGame3 game = new BirdGame3();
        game.activePlayers = 2;
        setPrivateBoolean(game);
        Bird first = new Bird(100.0, BirdGame3.BirdType.PIGEON, 0, game);
        Bird second = new Bird(1200.0, BirdGame3.BirdType.PIGEON, 1, game);
        first.y = BirdGame3.GROUND_Y - first.bodyHeight();
        second.y = BirdGame3.GROUND_Y - second.bodyHeight();
        first.facingRight = true;
        second.facingRight = false;
        game.players[0] = first;
        game.players[1] = second;
        invokePrivateVoid(first, "attack");
        invokePrivateVoid(second, "attack");

        first.x = 300.0;
        second.x = 375.0;
        double firstHealth = first.health;
        double secondHealth = second.health;
        invokePrivateVoid(first, "advanceNormalAttackTimeline");

        assertEquals(firstHealth, first.health, 0.0001);
        assertEquals(secondHealth, second.health, 0.0001);
        assertFalse(first.debugNormalAttackTimelineActive());
        assertFalse(second.debugNormalAttackTimelineActive());
        assertTrue(first.debugAttackInteractionTelemetryLabel().contains("CLANK"));
        assertTrue(second.debugAttackInteractionTelemetryLabel().contains("CLANK"));
    }

    @Test
    void strongerGroundedAttackWinsPriorityAndCancelsTheWeakerBox() throws Exception {
        BirdGame3 game = new BirdGame3();
        game.activePlayers = 2;
        setPrivateBoolean(game);
        Bird strong = new Bird(100.0, BirdGame3.BirdType.PELICAN, 0, game);
        Bird weak = new Bird(1200.0, BirdGame3.BirdType.HUMMINGBIRD, 1, game);
        strong.y = BirdGame3.GROUND_Y - strong.bodyHeight();
        weak.y = BirdGame3.GROUND_Y - weak.bodyHeight();
        strong.facingRight = true;
        weak.facingRight = false;
        game.players[0] = strong;
        game.players[1] = weak;
        invokePrivateVoid(strong, "attack");
        invokePrivateVoid(weak, "attack");

        strong.x = 300.0;
        weak.x = 375.0;
        double strongDamage = strong.smashDamagePercent();
        double weakDamage = weak.smashDamagePercent();
        invokePrivateVoid(strong, "advanceNormalAttackTimeline");

        assertEquals(strongDamage, strong.smashDamagePercent(), 0.0001);
        assertTrue(weak.smashDamagePercent() > weakDamage,
                strong.debugAttackInteractionTelemetryLabel() + " / "
                        + weak.debugAttackInteractionTelemetryLabel());
        assertFalse(weak.debugNormalAttackTimelineActive());
        assertTrue(strong.debugAttackInteractionTelemetryLabel().contains("PRIORITY_WIN"));
        assertTrue(weak.debugAttackInteractionTelemetryLabel().contains("PRIORITY_LOSS"));
    }

    @Test
    void overlappingAerialAttacksTradeAndDamageBothFighters() throws Exception {
        BirdGame3 game = new BirdGame3();
        game.activePlayers = 2;
        setPrivateBoolean(game);
        Bird first = new Bird(100.0, BirdGame3.BirdType.EAGLE, 0, game);
        Bird second = new Bird(1200.0, BirdGame3.BirdType.FALCON, 1, game);
        first.y = BirdGame3.GROUND_Y - 360.0;
        second.y = BirdGame3.GROUND_Y - 360.0;
        first.facingRight = true;
        second.facingRight = false;
        game.players[0] = first;
        game.players[1] = second;
        invokePrivateVoid(first, "attack");
        invokePrivateVoid(second, "attack");

        first.x = 300.0;
        second.x = 375.0;
        double firstDamage = first.smashDamagePercent();
        double secondDamage = second.smashDamagePercent();
        invokePrivateVoid(first, "advanceNormalAttackTimeline");

        assertTrue(first.smashDamagePercent() > firstDamage,
                "The defender's live aerial box should connect in a trade: "
                + first.debugAttackInteractionTelemetryLabel() + " / "
                        + second.debugAttackInteractionTelemetryLabel());
        assertTrue(second.smashDamagePercent() > secondDamage,
                "The initiating aerial should also connect in a trade.");
        assertTrue(first.debugAttackInteractionTelemetryLabel().contains("TRADE"));
        assertTrue(second.debugAttackInteractionTelemetryLabel().contains("TRADE"));
    }

    @Test
    void knockbackTuningBoostsNonSmashNormalsAndTonesDownSmashes() throws Exception {
        BirdGame3 game = new BirdGame3();
        Bird bird = new Bird(100.0, BirdGame3.BirdType.PIGEON, 0, game);

        Class<?> variantClass = Class.forName("com.example.birdgame3.Bird$NormalAttackVariant");
        Method multiplier = Bird.class.getDeclaredMethod("attackKnockbackBalanceMultiplier", variantClass);
        multiplier.setAccessible(true);

        Enum<?> sideTilt = enumConstant(variantClass, "SIDE_TILT");
        Enum<?> neutralAir = enumConstant(variantClass, "NEUTRAL_AIR");
        Enum<?> sideSmash = enumConstant(variantClass, "SIDE_SMASH");
        Enum<?> upSmash = enumConstant(variantClass, "UP_SMASH");

        assertTrue((double) multiplier.invoke(bird, sideTilt) > 1.0);
        assertTrue((double) multiplier.invoke(bird, neutralAir) > 1.0);
        assertTrue((double) multiplier.invoke(bird, sideSmash) < 1.0);
        assertTrue((double) multiplier.invoke(bird, upSmash) < 1.0);
    }

    @Test
    void roadrunnerAndRazorbillNormalsHaveDedicatedKnockbackBoosts() throws Exception {
        BirdGame3 game = new BirdGame3();
        Method multiplier = Bird.class.getDeclaredMethod("normalAttackBirdKnockbackMultiplier");
        multiplier.setAccessible(true);

        Bird pigeon = new Bird(100.0, BirdGame3.BirdType.PIGEON, 0, game);
        Bird roadrunner = new Bird(100.0, BirdGame3.BirdType.ROADRUNNER, 0, game);
        Bird razorbill = new Bird(100.0, BirdGame3.BirdType.RAZORBILL, 0, game);

        assertEquals(1.20, Bird.ROADRUNNER_NORMAL_KNOCKBACK_MULTIPLIER, 0.0001);
        assertEquals(1.15, Bird.RAZORBILL_NORMAL_KNOCKBACK_MULTIPLIER, 0.0001);
        assertEquals(1.0, (double) multiplier.invoke(pigeon), 0.0001);
        assertEquals(Bird.ROADRUNNER_NORMAL_KNOCKBACK_MULTIPLIER,
                (double) multiplier.invoke(roadrunner), 0.0001);
        assertEquals(Bird.RAZORBILL_NORMAL_KNOCKBACK_MULTIPLIER,
                (double) multiplier.invoke(razorbill), 0.0001);
    }

    @Test
    void phoenixNormalsUseItsFullPowerStatNowThatTheyHaveCooldowns() throws Exception {
        BirdGame3 game = new BirdGame3();
        Bird phoenix = new Bird(100.0, BirdGame3.BirdType.PHOENIX, 0, game);

        assertEquals(BirdGame3.BirdType.PHOENIX.power,
                invokeDoubleMethod(phoenix, "normalAttackPowerStat"), 0.0001,
                "Phoenix's normal damage and launch must not retain the obsolete no-cooldown penalty.");
    }

    @Test
    void smashRespawnNestGrantsTemporaryInvulnerability() throws Exception {
        BirdGame3 game = new BirdGame3();
        game.activePlayers = 2;
        setPrivateBoolean(game);

        Bird attacker = new Bird(100.0, BirdGame3.BirdType.PIGEON, 0, game);
        Bird respawned = new Bird(190.0, BirdGame3.BirdType.EAGLE, 1, game);
        game.players[0] = attacker;
        game.players[1] = respawned;

        respawned.resetForSmashRespawn(190.0, BirdGame3.GROUND_Y - 220.0, 0.0);

        Platform nest = (Platform) getPrivateObject(respawned, "respawnNestPlatform");
        assertNotNull(nest);
        assertTrue(respawned.isCombatInvulnerable());
        assertTrue(respawned.isOnGround());
        assertFalse(game.canDamage(attacker, respawned));

        setPrivateInt(respawned, "respawnInvulnerabilityTimer", 1);
        respawned.update(1.0);

        assertFalse(respawned.isCombatInvulnerable());
        assertNull(getPrivateObject(respawned, "respawnNestPlatform"));
        assertTrue(game.canDamage(attacker, respawned));
    }

    @Test
    void smashBlastZoneKoRespawnsBirdOnNestPlatform() throws Exception {
        BirdGame3 game = new BirdGame3();
        game.activePlayers = 1;
        setPrivateBoolean(game);
        game.scores[0] = 3;

        Bird bird = new Bird(BirdGame3.WORLD_WIDTH + 420.0, BirdGame3.BirdType.EAGLE, 0, game);
        bird.y = BirdGame3.GROUND_Y - 80.0;
        game.players[0] = bird;

        bird.update(1.0);

        assertEquals(2, game.scores[0]);
        assertTrue(bird.isCombatInvulnerable());
        assertTrue(bird.isOnGround());
        assertTrue(bird.y < BirdGame3.GROUND_Y - 180.0);
        assertNotNull(getPrivateObject(bird, "respawnNestPlatform"));
    }

    @Test
    void shieldAbsorbsBasicAttackIntoDurabilityInsteadOfHealth() throws Exception {
        BirdGame3 game = new BirdGame3();
        game.activePlayers = 2;

        Bird attacker = new Bird(100.0, BirdGame3.BirdType.PIGEON, 0, game);
        Bird defender = new Bird(190.0, BirdGame3.BirdType.EAGLE, 1, game);
        attacker.y = BirdGame3.GROUND_Y - 80.0;
        defender.y = BirdGame3.GROUND_Y - 80.0;
        attacker.facingRight = true;
        game.players[0] = attacker;
        game.players[1] = defender;

        game.setLocalActionsForKey(game.blockKeyForPlayer(1), true);
        for (int i = 0; i < 5; i++) {
            defender.update(1.0);
        }
        invokePrivateVoid(attacker, "attack");

        assertEquals(Bird.STARTING_HEALTH, defender.health, 0.0001);
        assertTrue(defender.isBlocking);
        assertTrue(getPrivateDouble(defender, "shieldHealth") < 60.0);
        assertTrue(getPrivateInt(defender, "shieldStunFrames") > 0);
    }

    @Test
    void shieldReleaseParryStunsAttackerWithoutConsumingShield() throws Exception {
        BirdGame3 game = new BirdGame3();
        game.activePlayers = 2;

        Bird attacker = new Bird(100.0, BirdGame3.BirdType.PIGEON, 0, game);
        Bird defender = new Bird(190.0, BirdGame3.BirdType.EAGLE, 1, game);
        attacker.y = BirdGame3.GROUND_Y - 80.0;
        defender.y = BirdGame3.GROUND_Y - 80.0;
        attacker.facingRight = true;
        game.players[0] = attacker;
        game.players[1] = defender;

        game.setLocalActionsForKey(game.blockKeyForPlayer(1), true);
        for (int i = 0; i < 5; i++) {
            defender.update(1.0);
        }
        double shieldBefore = getPrivateDouble(defender, "shieldHealth");
        game.setLocalActionsForKey(game.blockKeyForPlayer(1), false);
        defender.update(1.0);

        assertTrue(getPrivateInt(defender, "parryWindowFrames") > 0,
                "Releasing shield should open a brief perfect-shield window.");
        invokePrivateVoid(attacker, "attack");

        assertTrue(attacker.stunTime >= 20.0);
        assertEquals(Bird.STARTING_HEALTH, defender.health, 0.0001);
        assertEquals(shieldBefore, getPrivateDouble(defender, "shieldHealth"), 0.0001);
        assertFalse(defender.isBlocking);
    }

    @Test
    void grabBeatsShieldAndCapturesTarget() throws Exception {
        BirdGame3 game = new BirdGame3();
        game.activePlayers = 2;

        Bird attacker = new Bird(100.0, BirdGame3.BirdType.PIGEON, 0, game);
        Bird defender = new Bird(170.0, BirdGame3.BirdType.EAGLE, 1, game);
        attacker.y = BirdGame3.GROUND_Y - 80.0;
        defender.y = BirdGame3.GROUND_Y - 80.0;
        attacker.facingRight = true;
        game.players[0] = attacker;
        game.players[1] = defender;

        game.setLocalActionsForKey(game.blockKeyForPlayer(1), true);
        for (int i = 0; i < 3; i++) {
            defender.update(1.0);
        }

        game.setLocalActionsForKey(game.grabKeyForPlayer(0), true);
        attacker.update(1.0);
        assertNull(getPrivateObject(attacker, "grabbedTarget"),
                "A grab should have readable startup instead of capturing on the input frame.");
        assertTrue(attacker.debugUniversalActionLabel().startsWith("GRAB STARTUP"));
        game.setLocalActionsForKey(game.grabKeyForPlayer(0), false);
        for (int i = 0; i < 4; i++) attacker.update(1.0);

        assertSame(defender, getPrivateObject(attacker, "grabbedTarget"));
        assertSame(attacker, getPrivateObject(defender, "grabbedBy"));
        assertFalse(defender.isBlocking);
        assertEquals(Bird.STARTING_HEALTH, defender.health, 0.0001);
    }

    @Test
    void grabbedTargetCanBeThrownUpwardAfterHoldWindow() throws Exception {
        BirdGame3 game = new BirdGame3();
        game.activePlayers = 2;

        Bird attacker = new Bird(100.0, BirdGame3.BirdType.PIGEON, 0, game);
        Bird defender = new Bird(170.0, BirdGame3.BirdType.EAGLE, 1, game);
        attacker.y = BirdGame3.GROUND_Y - 80.0;
        defender.y = BirdGame3.GROUND_Y - 80.0;
        attacker.facingRight = true;
        game.players[0] = attacker;
        game.players[1] = defender;

        game.setLocalActionsForKey(game.grabKeyForPlayer(0), true);
        attacker.update(1.0);
        game.setLocalActionsForKey(game.grabKeyForPlayer(0), false);
        for (int i = 0; i < 4; i++) attacker.update(1.0);

        double startingHealth = defender.health;
        game.setLocalActionsForKey(game.jumpKeyForPlayer(0), true);
        for (int i = 0; i < 10; i++) {
            attacker.update(1.0);
            defender.update(1.0);
        }
        game.setLocalActionsForKey(game.jumpKeyForPlayer(0), false);

        assertNull(getPrivateObject(attacker, "grabbedTarget"));
        assertNull(getPrivateObject(defender, "grabbedBy"));
        assertTrue(defender.vy < 0.0);
        assertTrue(defender.health < startingHealth);
    }

    @Test
    void defeatedHolderReleasesGrabbedTarget() throws Exception {
        BirdGame3 game = new BirdGame3();
        game.activePlayers = 2;

        Bird attacker = new Bird(100.0, BirdGame3.BirdType.PIGEON, 0, game);
        Bird defender = new Bird(170.0, BirdGame3.BirdType.EAGLE, 1, game);
        attacker.y = BirdGame3.GROUND_Y - 80.0;
        defender.y = BirdGame3.GROUND_Y - 80.0;
        attacker.facingRight = true;
        game.players[0] = attacker;
        game.players[1] = defender;

        game.setLocalActionsForKey(game.grabKeyForPlayer(0), true);
        attacker.update(1.0);
        game.setLocalActionsForKey(game.grabKeyForPlayer(0), false);
        for (int i = 0; i < 4; i++) attacker.update(1.0);

        attacker.health = 0.0;
        attacker.update(1.0);

        assertNull(getPrivateObject(attacker, "grabbedTarget"));
        assertNull(getPrivateObject(defender, "grabbedBy"));
    }

    @Test
    void grabbedTargetCanMashOutInsteadOfBeingAutoThrown() throws Exception {
        BirdGame3 game = new BirdGame3();
        game.activePlayers = 2;
        Bird holder = new Bird(100.0, BirdGame3.BirdType.PIGEON, 0, game);
        Bird target = new Bird(170.0, BirdGame3.BirdType.EAGLE, 1, game);
        holder.y = target.y = BirdGame3.GROUND_Y - 80.0;
        game.players[0] = holder;
        game.players[1] = target;
        invokePrivateVoid(holder, "beginGrabOn", new Class<?>[]{Bird.class}, target);

        double startingHealth = target.health;
        for (int i = 0; i < 12 && getPrivateObject(target, "grabbedBy") != null; i++) {
            var previousKey = i % 2 == 0 ? game.jumpKeyForPlayer(1) : game.attackKeyForPlayer(1);
            var currentKey = i % 2 == 0 ? game.attackKeyForPlayer(1) : game.jumpKeyForPlayer(1);
            game.setLocalActionsForKey(previousKey, false);
            game.setLocalActionsForKey(currentKey, true);
            target.update(1.0);
        }

        assertNull(getPrivateObject(holder, "grabbedTarget"));
        assertNull(getPrivateObject(target, "grabbedBy"));
        assertEquals(startingHealth, target.health, 0.0001,
                "Escaping a grab should not secretly apply a throw or pummel.");
        assertTrue(Math.abs(target.vx) > 0.0, "An escape should create readable separation.");
    }

    @Test
    void highDamageTargetsHaveLongerGrabEscapeWindows() throws Exception {
        BirdGame3 game = new BirdGame3();
        setPrivateBoolean(game);
        game.activePlayers = 2;
        Bird holder = new Bird(100.0, BirdGame3.BirdType.PIGEON, 0, game);
        Bird target = new Bird(170.0, BirdGame3.BirdType.EAGLE, 1, game);
        holder.y = target.y = BirdGame3.GROUND_Y - 80.0;
        game.players[0] = holder;
        game.players[1] = target;
        setPrivateDouble(target, "smashDamage", 100.0);

        invokePrivateVoid(holder, "beginGrabOn", new Class<?>[]{Bird.class}, target);

        assertEquals(58, getPrivateInt(holder, "grabHoldTimer"));
        assertEquals("GRAB HOLD 58f | THROW LOCK 8f", holder.debugUniversalActionLabel());
        assertTrue(target.debugUniversalActionLabel().startsWith("GRABBED 58f | MASH"));
    }

    @Test
    void holdingAwayPerformsARealBackThrowWithoutTurningTheHolder() throws Exception {
        BirdGame3 game = new BirdGame3();
        game.activePlayers = 2;
        Bird holder = new Bird(100.0, BirdGame3.BirdType.PIGEON, 0, game);
        Bird target = new Bird(170.0, BirdGame3.BirdType.EAGLE, 1, game);
        holder.y = target.y = BirdGame3.GROUND_Y - 80.0;
        holder.facingRight = true;
        game.players[0] = holder;
        game.players[1] = target;
        invokePrivateVoid(holder, "beginGrabOn", new Class<?>[]{Bird.class}, target);
        setPrivateInt(holder, "grabThrowLockTimer", 0);
        game.setLocalActionsForKey(game.leftKeyForPlayer(0), true);

        Method hold = Bird.class.getDeclaredMethod("handleHoldingGrabState", boolean.class, boolean.class);
        hold.setAccessible(true);
        assertTrue((boolean) hold.invoke(holder, false, false));

        assertTrue(holder.facingRight, "Choosing back throw must not silently reverse the holder first.");
        assertTrue(target.vx < 0.0, "A right-facing bird's back throw should launch left.");
        assertNull(getPrivateObject(holder, "grabbedTarget"));
    }

    @Test
    void lanSnapshotsRestoreActiveGrabLinksAndTimers() throws Exception {
        BirdGame3 sourceGame = new BirdGame3();
        sourceGame.activePlayers = 2;
        Bird sourceHolder = new Bird(100.0, BirdGame3.BirdType.PIGEON, 0, sourceGame);
        Bird sourceTarget = new Bird(170.0, BirdGame3.BirdType.EAGLE, 1, sourceGame);
        sourceHolder.y = sourceTarget.y = BirdGame3.GROUND_Y - 80.0;
        sourceGame.players[0] = sourceHolder;
        sourceGame.players[1] = sourceTarget;
        invokePrivateVoid(sourceHolder, "beginGrabOn", new Class<?>[]{Bird.class}, sourceTarget);
        setPrivateInt(sourceHolder, "grabHoldTimer", 29);
        setPrivateInt(sourceHolder, "grabEscapeProgress", 8);
        LanBirdState holderState = sourceHolder.toLanState();
        LanBirdState targetState = sourceTarget.toLanState();

        BirdGame3 remoteGame = new BirdGame3();
        Bird remoteHolder = new Bird(0.0, BirdGame3.BirdType.PIGEON, 0, remoteGame);
        Bird remoteTarget = new Bird(0.0, BirdGame3.BirdType.EAGLE, 1, remoteGame);
        remoteGame.players[0] = remoteHolder;
        remoteGame.players[1] = remoteTarget;
        remoteHolder.applyLanState(holderState);
        remoteTarget.applyLanState(targetState);
        remoteHolder.resolveLanGrabLinks(holderState, remoteGame.players);
        remoteTarget.resolveLanGrabLinks(targetState, remoteGame.players);

        assertSame(remoteTarget, getPrivateObject(remoteHolder, "grabbedTarget"));
        assertSame(remoteHolder, getPrivateObject(remoteTarget, "grabbedBy"));
        assertEquals(29, getPrivateInt(remoteHolder, "grabHoldTimer"));
        assertEquals(8, getPrivateInt(remoteHolder, "grabEscapeProgress"));
        assertEquals(sourceHolder.deterministicGrabStateHash(), remoteHolder.deterministicGrabStateHash());
    }

    @Test
    void highLevelCpuPrioritizesGrabAgainstNearbyShield() throws Exception {
        BirdGame3 game = new BirdGame3();
        Bird cpu = new Bird(100.0, BirdGame3.BirdType.PIGEON, 0, game);
        Bird target = new Bird(170.0, BirdGame3.BirdType.EAGLE, 1, game);
        cpu.y = target.y = BirdGame3.GROUND_Y - 80.0;
        target.isBlocking = true;
        Method decision = Bird.class.getDeclaredMethod("shouldAIUseGrab",
                Bird.class, double.class, boolean.class, int.class, double.class);
        decision.setAccessible(true);

        assertTrue((boolean) decision.invoke(cpu, target, 70.0, true, 7, 0.7));
        assertFalse((boolean) decision.invoke(cpu, target, 70.0, true, 2, 0.05));
    }

    @Test
    void holdingShieldDrainsDurabilityAndShrinksItsVisual() throws Exception {
        BirdGame3 game = new BirdGame3();
        game.activePlayers = 1;

        Bird defender = new Bird(190.0, BirdGame3.BirdType.EAGLE, 0, game);
        defender.y = BirdGame3.GROUND_Y - 80.0;
        game.players[0] = defender;

        game.setLocalActionsForKey(game.blockKeyForPlayer(0), true);
        for (int i = 0; i < 180; i++) {
            defender.update(1.0);
        }

        assertTrue(defender.isBlocking);
        assertTrue(getPrivateDouble(defender, "shieldHealth") < 50.0,
                "A shield held for three seconds should visibly lose durability.");
        assertTrue(getPrivateDouble(defender, "shieldHoldVisual") > 0.9);
    }

    @Test
    void shieldWaitsOneSecondBeforeRegenerating() throws Exception {
        BirdGame3 game = new BirdGame3();
        game.activePlayers = 1;

        Bird defender = new Bird(190.0, BirdGame3.BirdType.EAGLE, 0, game);
        defender.y = BirdGame3.GROUND_Y - 80.0;
        game.players[0] = defender;

        game.setLocalActionsForKey(game.blockKeyForPlayer(0), true);
        for (int i = 0; i < 10; i++) {
            defender.update(1.0);
        }
        game.setLocalActionsForKey(game.blockKeyForPlayer(0), false);
        defender.update(1.0);
        double releasedHealth = getPrivateDouble(defender, "shieldHealth");

        for (int i = 0; i < 58; i++) {
            defender.update(1.0);
        }
        assertEquals(releasedHealth, getPrivateDouble(defender, "shieldHealth"), 0.0001,
                "Shield health must not refill during the regeneration lockout.");

        defender.update(1.0);
        assertTrue(getPrivateDouble(defender, "shieldHealth") > releasedHealth,
                "Shield regeneration should resume when the one-second lockout expires.");
    }

    @Test
    void criticallySmallShieldCanExposeDefenderToSideHit() throws Exception {
        BirdGame3 game = new BirdGame3();
        game.activePlayers = 2;

        Bird attacker = new Bird(100.0, BirdGame3.BirdType.PIGEON, 0, game);
        Bird defender = new Bird(190.0, BirdGame3.BirdType.EAGLE, 1, game);
        attacker.y = BirdGame3.GROUND_Y - 80.0;
        defender.y = BirdGame3.GROUND_Y - 80.0;
        attacker.facingRight = true;
        game.players[0] = attacker;
        game.players[1] = defender;
        setPrivateDouble(defender, "shieldHealth", 12.0);

        game.setLocalActionsForKey(game.blockKeyForPlayer(1), true);
        for (int i = 0; i < 4; i++) {
            defender.update(1.0);
        }
        invokePrivateVoid(attacker, "attack");

        assertTrue(defender.health < Bird.STARTING_HEALTH,
                "A critically shrunken shield should leave the bird's outer hurtbox exposed.");
    }

    @Test
    void spotDodgeAvoidsDamageWithoutConsumingShieldDurability() throws Exception {
        BirdGame3 game = new BirdGame3();
        game.activePlayers = 1;

        Bird defender = new Bird(190.0, BirdGame3.BirdType.EAGLE, 0, game);
        defender.y = BirdGame3.GROUND_Y - 80.0;
        game.players[0] = defender;

        game.setLocalActionsForKey(game.blockKeyForPlayer(0), true);
        for (int i = 0; i < 3; i++) {
            defender.update(1.0);
        }
        double shieldBefore = getPrivateDouble(defender, "shieldHealth");

        game.setLocalActionsForKey(game.grabKeyForPlayer(0), true);
        defender.update(1.0);
        game.setLocalActionsForKey(game.grabKeyForPlayer(0), false);

        double dealtDamage = defender.receiveExternalDamage(14.0);

        assertEquals(0.0, dealtDamage, 0.0001);
        assertEquals(Bird.STARTING_HEALTH, defender.health, 0.0001);
        assertEquals(shieldBefore, getPrivateDouble(defender, "shieldHealth"), 0.0001);
        assertFalse(defender.isBlocking);
        assertEquals("SPOT", getPrivateObject(defender, "dodgeType").toString());
        assertTrue(getPrivateInt(defender, "dodgeInvulnerabilityTimer") > 0);
    }

    @Test
    void shieldRollLaunchesBirdOutOfShield() throws Exception {
        BirdGame3 game = new BirdGame3();
        game.activePlayers = 1;

        Bird defender = new Bird(190.0, BirdGame3.BirdType.EAGLE, 0, game);
        defender.y = BirdGame3.GROUND_Y - 80.0;
        game.players[0] = defender;

        game.setLocalActionsForKey(game.blockKeyForPlayer(0), true);
        for (int i = 0; i < 3; i++) {
            defender.update(1.0);
        }

        double startX = defender.x;
        game.setLocalActionsForKey(game.rightKeyForPlayer(0), true);
        defender.update(1.0);

        assertFalse(defender.isBlocking);
        assertEquals("ROLL", getPrivateObject(defender, "dodgeType").toString());
        assertTrue(defender.x > startX + 4.0);
        assertTrue(getPrivateInt(defender, "dodgeDirection") > 0);
    }

    @Test
    void shieldRollUsesVisibleRollingPose() throws Exception {
        BirdGame3 game = new BirdGame3();
        game.activePlayers = 1;

        Bird defender = new Bird(190.0, BirdGame3.BirdType.EAGLE, 0, game);
        defender.y = BirdGame3.GROUND_Y - 80.0;
        game.players[0] = defender;

        game.setLocalActionsForKey(game.blockKeyForPlayer(0), true);
        for (int i = 0; i < 3; i++) {
            defender.update(1.0);
        }

        game.setLocalActionsForKey(game.rightKeyForPlayer(0), true);
        defender.update(1.0);

        Object pose = invokePrivateObjectMethod(defender, "currentAttackVisualPose");
        assertNotNull(pose);
        assertTrue(Math.abs(invokeDoubleMethod(pose, "bodyRotationDegrees")) > 15.0,
                "Shield rolls should visibly rotate the bird instead of reading as a pure slide.");
    }

    @Test
    void attackVisualPoseBlendsTowardNewTargetInsteadOfSnapping() throws Exception {
        BirdGame3 game = new BirdGame3();
        game.activePlayers = 1;

        Bird bird = new Bird(190.0, BirdGame3.BirdType.EAGLE, 0, game);
        bird.y = BirdGame3.GROUND_Y - 80.0;
        bird.facingRight = true;
        game.players[0] = bird;

        invokePrivateObjectMethod(bird, "currentAttackVisualPose");

        game.setLocalActionsForKey(game.rightKeyForPlayer(0), true);
        game.setLocalActionsForKey(game.attackKeyForPlayer(0), true);
        bird.update(1.0);

        Object displayPose = invokePrivateObjectMethod(bird, "currentAttackVisualPose");
        Object targetPose = invokePrivateObjectMethod(bird, "currentTargetAttackVisualPose");
        double displayTranslateX = invokeDoubleMethod(displayPose, "translateX");
        double targetTranslateX = invokeDoubleMethod(targetPose, "translateX");

        assertTrue(displayTranslateX > 0.0, "The blended pose should move away from idle once an attack starts.");
        assertTrue(displayTranslateX < targetTranslateX,
                "The displayed pose should ease toward the attack target instead of snapping to it in one frame.");
    }

    @Test
    void shieldingWhileAlreadyMovingStopsBirdInsteadOfRolling() throws Exception {
        BirdGame3 game = new BirdGame3();
        game.activePlayers = 1;

        Bird defender = new Bird(190.0, BirdGame3.BirdType.EAGLE, 0, game);
        defender.y = BirdGame3.GROUND_Y - 80.0;
        game.players[0] = defender;

        game.setLocalActionsForKey(game.rightKeyForPlayer(0), true);
        for (int i = 0; i < 3; i++) {
            defender.update(1.0);
        }

        double startX = defender.x;
        game.setLocalActionsForKey(game.blockKeyForPlayer(0), true);
        defender.update(1.0);

        assertTrue(defender.isBlocking);
        assertEquals("NONE", getPrivateObject(defender, "dodgeType").toString());
        assertEquals(startX, defender.x, 0.0001);
        assertEquals(0.0, defender.vx, 0.0001);
    }

    @Test
    void airDodgeConsumesChargeUntilLanding() throws Exception {
        BirdGame3 game = new BirdGame3();
        game.activePlayers = 1;

        Bird bird = new Bird(190.0, BirdGame3.BirdType.EAGLE, 0, game);
        bird.y = BirdGame3.GROUND_Y - 240.0;
        bird.vy = 3.0;
        game.players[0] = bird;

        game.setLocalActionsForKey(game.blockKeyForPlayer(0), true);
        bird.update(1.0);
        game.setLocalActionsForKey(game.blockKeyForPlayer(0), false);

        assertEquals("AIR", getPrivateObject(bird, "dodgeType").toString());
        assertFalse(getPrivateBoolean(bird, "airDodgeAvailable"));
        assertEquals(0.0, bird.receiveExternalDamage(12.0), 0.0001);

        bird.y = BirdGame3.GROUND_Y - 80.0;
        bird.vy = 6.0;
        bird.update(1.0);

        assertEquals("NONE", getPrivateObject(bird, "dodgeType").toString());
        assertTrue(getPrivateBoolean(bird, "airDodgeAvailable"));
    }

    @Test
    void directionalAirDodgeSupportsDiagonalBurst() throws Exception {
        BirdGame3 game = new BirdGame3();
        game.activePlayers = 1;

        Bird bird = new Bird(190.0, BirdGame3.BirdType.EAGLE, 0, game);
        bird.y = BirdGame3.GROUND_Y - 260.0;
        game.players[0] = bird;

        invokePrivateVoid(bird, "startAirDodge", new Class<?>[]{int.class, int.class}, 1, -1);

        assertEquals("AIR", getPrivateObject(bird, "dodgeType").toString());
        assertEquals(1, getPrivateInt(bird, "dodgeDirection"));
        assertEquals(-1, getPrivateInt(bird, "dodgeVerticalDirection"));
        assertTrue(bird.vx > 5.0);
        assertTrue(bird.vy < -5.0);
        assertTrue(bird.debugUniversalActionLabel().contains("[1,-1]"));
    }

    @Test
    void landingDuringAirDodgeAppliesRecoveryLag() throws Exception {
        BirdGame3 game = new BirdGame3();
        game.activePlayers = 1;

        Bird bird = new Bird(190.0, BirdGame3.BirdType.EAGLE, 0, game);
        bird.y = BirdGame3.GROUND_Y - 100.0;
        game.players[0] = bird;
        invokePrivateVoid(bird, "startAirDodge", new Class<?>[]{int.class, int.class}, 0, 1);

        for (int i = 0; i < 5 && !bird.isOnGround(); i++) {
            bird.update(1.0);
        }

        assertTrue(bird.isOnGround());
        assertTrue(getPrivateInt(bird, "landingLagTimer") > 0,
                "Landing out of an air dodge should create a punishable recovery window.");
    }

    @Test
    void groundedJumpWaitsForJumpSquatBeforeLiftoff() throws Exception {
        BirdGame3 game = new BirdGame3();
        game.activePlayers = 1;

        Bird bird = new Bird(190.0, BirdGame3.BirdType.EAGLE, 0, game);
        bird.y = BirdGame3.GROUND_Y - 80.0;
        game.players[0] = bird;

        double startY = bird.y;
        game.setLocalActionsForKey(game.jumpKeyForPlayer(0), true);

        bird.update(1.0);
        assertEquals(startY, bird.y, 0.0001);
        assertEquals(2, getPrivateInt(bird, "jumpSquatTimer"));
        assertTrue(bird.isOnGround());

        bird.update(1.0);
        assertEquals(startY, bird.y, 0.0001);
        assertEquals(1, getPrivateInt(bird, "jumpSquatTimer"));
        assertTrue(bird.isOnGround());

        bird.update(1.0);

        assertTrue(bird.y < startY);
        assertTrue(bird.vy < 0.0);
        assertEquals(0, getPrivateInt(bird, "jumpSquatTimer"));
    }

    @Test
    void tapJumpProducesShortHopWhileHeldJumpProducesFullHop() {
        double shortHopVy = launchVelocityAfterGroundJump(1);
        double fullHopVy = launchVelocityAfterGroundJump(3);

        assertTrue(shortHopVy < fullHopVy * 0.8,
                "Short hop should launch lower than a full hop.");
    }

    @Test
    void heldGroundJumpDoesNotConsumePigeonDoubleJump() {
        BirdGame3 game = new BirdGame3();
        game.activePlayers = 1;

        Bird pigeon = new Bird(190.0, BirdGame3.BirdType.PIGEON, 0, game);
        pigeon.y = BirdGame3.GROUND_Y - 80.0;
        game.players[0] = pigeon;

        game.setLocalActionsForKey(game.jumpKeyForPlayer(0), true);
        for (int i = 0; i < 4; i++) {
            pigeon.update(1.0);
        }

        assertFalse(pigeon.isOnGround());
        assertTrue(pigeon.canDoubleJump, "Holding jump through takeoff should not auto-spend the double jump.");
    }

    @Test
    void pigeonJumpInputAloneStillStartsNormalJump() throws Exception {
        BirdGame3 game = new BirdGame3();
        game.activePlayers = 1;

        Bird pigeon = new Bird(190.0, BirdGame3.BirdType.PIGEON, 0, game);
        pigeon.y = BirdGame3.GROUND_Y - 80.0;
        game.players[0] = pigeon;

        double startY = pigeon.y;
        game.setLocalActionsForKey(game.jumpKeyForPlayer(0), true);
        for (int i = 0; i < 4; i++) {
            pigeon.update(1.0);
        }

        assertTrue(pigeon.y < startY, "Pressing up alone should still make Pigeon jump.");
        assertEquals(0, getPrivateInt(pigeon, "pigeonFlutterTimer"),
                "Jump input by itself should not start Pigeon's recovery.");
        assertTrue(pigeon.canDoubleJump, "A normal jump should preserve Pigeon's extra jump.");
    }

    @Test
    void pigeonNeutralSpecialWaitsForReleaseAndQuickTapStillPecks() throws Exception {
        BirdGame3 game = new BirdGame3();
        game.activePlayers = 2;

        Bird pigeon = new Bird(100.0, BirdGame3.BirdType.PIGEON, 0, game);
        Bird target = new Bird(185.0, BirdGame3.BirdType.EAGLE, 1, game);
        pigeon.y = BirdGame3.GROUND_Y - 80.0;
        target.y = BirdGame3.GROUND_Y - 80.0;
        pigeon.facingRight = true;
        pigeon.health = 60.0;
        game.players[0] = pigeon;
        game.players[1] = target;

        double startingHealth = target.health;
        game.setLocalActionsForKey(game.specialKeyForPlayer(0), true);
        pigeon.update(1.0);

        assertTrue(pigeon.pigeonFeatherCharging,
                "Neutral special should wind up while special remains held.");
        assertEquals(startingHealth, target.health, 0.0001,
                "The peck should not hit before special is released.");

        game.setLocalActionsForKey(game.specialKeyForPlayer(0), false);
        pigeon.update(1.0);

        assertEquals(startingHealth - 4.0, target.health, 0.0001,
                "A quick tap should release the short, light version of Long Peck.");
        assertEquals(60.0, pigeon.health, 0.0001, "Neutral special should not heal Pigeon.");
        assertTrue(getPrivateInt(pigeon, "specialCooldown") > 0,
                "Neutral special should apply an anti-spam cooldown.");

        double afterFirstBurstHealth = target.health;
        target.x = 185.0;
        target.y = BirdGame3.GROUND_Y - 80.0;
        target.vx = 0.0;
        target.vy = 0.0;
        pigeon.update(1.0);
        game.setLocalActionsForKey(game.specialKeyForPlayer(0), true);
        pigeon.update(1.0);
        game.setLocalActionsForKey(game.specialKeyForPlayer(0), false);

        assertEquals(afterFirstBurstHealth, target.health, 0.0001,
                "Neutral special should have enough lockout to prevent immediate spam.");
    }

    @Test
    void pigeonHeldNeutralSpecialExtendsReachAndAutoReleasesAtFullCharge() {
        BirdGame3 game = new BirdGame3();
        game.activePlayers = 2;

        Bird pigeon = new Bird(100.0, BirdGame3.BirdType.PIGEON, 0, game);
        Bird distantTarget = new Bird(390.0, BirdGame3.BirdType.EAGLE, 1, game);
        pigeon.y = BirdGame3.GROUND_Y - 80.0;
        distantTarget.y = BirdGame3.GROUND_Y - 80.0;
        pigeon.facingRight = true;
        game.players[0] = pigeon;
        game.players[1] = distantTarget;

        assertEquals(Bird.PIGEON_NEUTRAL_MIN_REACH, PigeonSpecials.neutralReachForCharge(0), 0.0001);
        assertEquals(Bird.PIGEON_NEUTRAL_MAX_REACH,
                PigeonSpecials.neutralReachForCharge(Bird.PIGEON_NEUTRAL_MAX_CHARGE_FRAMES), 0.0001);
        assertTrue(PigeonSpecials.neutralReachForCharge(Bird.PIGEON_NEUTRAL_MAX_CHARGE_FRAMES / 2)
                        > PigeonSpecials.neutralReachForCharge(0),
                "Long Peck reach should grow continuously while it is held.");

        double startingHealth = distantTarget.health;
        game.setLocalActionsForKey(game.specialKeyForPlayer(0), true);
        pigeon.update(1.0);
        for (int i = 0; i < Bird.PIGEON_NEUTRAL_MAX_CHARGE_FRAMES; i++) {
            pigeon.update(1.0);
        }

        assertFalse(pigeon.pigeonFeatherCharging,
                "A fully charged Long Peck should release automatically instead of charging forever.");
        assertEquals(Bird.PIGEON_NEUTRAL_MAX_CHARGE_FRAMES, pigeon.pigeonFeatherBurstChargeFrames);
        assertTrue(pigeon.pigeonFeatherBurstTimer > 0);
        assertTrue(distantTarget.health < startingHealth,
                "The fully held Long Peck should reach a target that a quick tap cannot reach.");
        assertTrue(distantTarget.vx > 0.0, "The charged peck should launch forward.");

        game.setLocalActionsForKey(game.specialKeyForPlayer(0), false);
    }

    @Test
    void pigeonNeutralChargeIsCanceledWhenPigeonIsHit() {
        BirdGame3 game = new BirdGame3();
        game.activePlayers = 1;

        Bird pigeon = new Bird(190.0, BirdGame3.BirdType.PIGEON, 0, game);
        pigeon.y = BirdGame3.GROUND_Y - 80.0;
        game.players[0] = pigeon;

        game.setLocalActionsForKey(game.specialKeyForPlayer(0), true);
        pigeon.update(1.0);
        for (int i = 0; i < 12; i++) {
            pigeon.update(1.0);
        }
        assertTrue(pigeon.pigeonFeatherCharging);
        assertTrue(pigeon.pigeonFeatherChargeFrames >= 12);

        assertTrue(pigeon.receiveExternalDamage(5.0) > 0.0);
        assertFalse(pigeon.pigeonFeatherCharging,
                "Taking a hit should interrupt Long Peck's wind-up.");
        assertEquals(0, pigeon.pigeonFeatherChargeFrames);
        assertEquals(0, pigeon.pigeonFeatherBurstTimer,
                "An interrupted charge must not release a delayed peck.");

        game.setLocalActionsForKey(game.specialKeyForPlayer(0), false);
    }

    @Test
    void pigeonSideSpecialUsesDirectionalInputForDashStrike() {
        BirdGame3 game = new BirdGame3();
        game.activePlayers = 2;

        Bird pigeon = new Bird(100.0, BirdGame3.BirdType.PIGEON, 0, game);
        Bird target = new Bird(200.0, BirdGame3.BirdType.EAGLE, 1, game);
        pigeon.y = BirdGame3.GROUND_Y - 80.0;
        target.y = BirdGame3.GROUND_Y - 80.0;
        pigeon.facingRight = true;
        game.players[0] = pigeon;
        game.players[1] = target;

        double startingHealth = target.health;
        game.setLocalActionsForKey(game.rightKeyForPlayer(0), true);
        game.setLocalActionsForKey(game.specialKeyForPlayer(0), true);
        pigeon.update(1.0);
        game.setLocalActionsForKey(game.specialKeyForPlayer(0), false);
        game.setLocalActionsForKey(game.rightKeyForPlayer(0), false);

        assertTrue(pigeon.vx > 17.0, "Side special should commit Pigeon to a faster horizontal burst.");
        assertEquals(startingHealth - 3.0, target.health, 0.0001,
                "Side special should now deal lighter damage.");
        assertTrue(target.vy < -8.0, "Side special should launch targets much higher than before.");
    }

    @Test
    void pigeonSideSpecialTravelsMuchFartherForLessDamage() {
        BirdGame3 game = new BirdGame3();
        game.activePlayers = 2;

        Bird pigeon = new Bird(100.0, BirdGame3.BirdType.PIGEON, 0, game);
        Bird target = new Bird(430.0, BirdGame3.BirdType.EAGLE, 1, game);
        pigeon.y = BirdGame3.GROUND_Y - 80.0;
        target.y = BirdGame3.GROUND_Y - 80.0;
        pigeon.facingRight = true;
        game.players[0] = pigeon;
        game.players[1] = target;

        game.setLocalActionsForKey(game.rightKeyForPlayer(0), true);
        game.setLocalActionsForKey(game.specialKeyForPlayer(0), true);
        pigeon.update(1.0);
        game.setLocalActionsForKey(game.specialKeyForPlayer(0), false);
        game.setLocalActionsForKey(game.rightKeyForPlayer(0), false);

        for (int i = 0; i < 24; i++) {
            pigeon.update(1.0);
        }

        assertEquals(Bird.STARTING_HEALTH - 3.0, target.health, 0.0001,
                "Side special should trade damage for Fox-style travel distance.");
        assertTrue(target.vy < -8.0, "The long rush should still send targets much higher on hit.");
    }

    @Test
    void pigeonUpSpecialOverridesGroundJumpAndStartsFlutter() throws Exception {
        BirdGame3 game = new BirdGame3();
        game.activePlayers = 1;

        Bird pigeon = new Bird(190.0, BirdGame3.BirdType.PIGEON, 0, game);
        pigeon.y = BirdGame3.GROUND_Y - 80.0;
        game.players[0] = pigeon;

        game.setLocalActionsForKey(game.jumpKeyForPlayer(0), true);
        game.setLocalActionsForKey(game.specialKeyForPlayer(0), true);
        pigeon.update(1.0);
        game.setLocalActionsForKey(game.specialKeyForPlayer(0), false);
        game.setLocalActionsForKey(game.jumpKeyForPlayer(0), false);

        assertEquals(0, getPrivateInt(pigeon, "jumpSquatTimer"),
                "Up special should bypass jump squat instead of becoming a normal jump.");
        assertTrue(getPrivateInt(pigeon, "pigeonFlutterTimer") > 0);
        assertTrue(getPrivateBoolean(pigeon, "pigeonUpSpecialUsed"));
        assertFalse(pigeon.canDoubleJump, "Up special should spend Pigeon's remaining air recovery.");
        assertTrue(pigeon.vy < -8.0, "Up special should launch Pigeon upward immediately.");
    }

    @Test
    void pigeonGroundDownSpecialUsesBlockInputWithoutRaisingShieldAndDoesNotHealOnCompletion() {
        BirdGame3 game = new BirdGame3();
        game.activePlayers = 1;

        Bird pigeon = new Bird(190.0, BirdGame3.BirdType.PIGEON, 0, game);
        pigeon.y = BirdGame3.GROUND_Y - 80.0;
        pigeon.health = 48.0;
        game.players[0] = pigeon;

        game.setLocalActionsForKey(game.blockKeyForPlayer(0), true);
        game.setLocalActionsForKey(game.specialKeyForPlayer(0), true);
        pigeon.update(1.0);
        game.setLocalActionsForKey(game.blockKeyForPlayer(0), false);
        game.setLocalActionsForKey(game.specialKeyForPlayer(0), false);

        assertFalse(pigeon.isBlocking, "Down special should reserve block input instead of raising shield.");
        for (int i = 0; i < 120; i++) {
            pigeon.update(1.0);
        }

        assertEquals(48.0, pigeon.health, 0.0001,
                "Grounded scavenge should not restore health before it resolves.");
        for (int i = 0; i < 50; i++) {
            pigeon.update(1.0);
        }

        assertEquals(48.0, pigeon.health, 0.0001,
                "Completing the grounded scavenge should not heal Pigeon.");
    }

    @Test
    void pigeonHeldGroundDownSpecialSendsDamagingCracksInBothDirections() {
        BirdGame3 game = new BirdGame3();
        game.activePlayers = 3;

        Bird pigeon = new Bird(500.0, BirdGame3.BirdType.PIGEON, 0, game);
        Bird leftTarget = new Bird(360.0, BirdGame3.BirdType.EAGLE, 1, game);
        Bird rightTarget = new Bird(640.0, BirdGame3.BirdType.FALCON, 2, game);
        pigeon.y = BirdGame3.GROUND_Y - 80.0;
        leftTarget.y = BirdGame3.GROUND_Y - 80.0;
        rightTarget.y = BirdGame3.GROUND_Y - 80.0;
        game.players[0] = pigeon;
        game.players[1] = leftTarget;
        game.players[2] = rightTarget;

        game.setLocalActionsForKey(game.blockKeyForPlayer(0), true);
        game.setLocalActionsForKey(game.specialKeyForPlayer(0), true);
        pigeon.update(1.0);
        game.setLocalActionsForKey(game.blockKeyForPlayer(0), false);

        for (int i = 0; i < 35; i++) {
            pigeon.update(1.0);
        }

        assertTrue(pigeon.pigeonScavengeHoldFrames >= 35,
                "Holding special should sustain the grounded fault-line attack.");
        assertTrue(leftTarget.health < Bird.STARTING_HEALTH,
                "The left-moving crack should damage a grounded target.");
        assertTrue(rightTarget.health < Bird.STARTING_HEALTH,
                "The right-moving crack should damage a grounded target.");
        assertTrue(leftTarget.vx < 0.0, "The left crack should push away from Pigeon.");
        assertTrue(rightTarget.vx > 0.0, "The right crack should push away from Pigeon.");

        game.setLocalActionsForKey(game.specialKeyForPlayer(0), false);
        for (int i = 0; i < Bird.PIGEON_SCAVENGE_RELEASE_FRAMES + 1; i++) {
            pigeon.update(1.0);
        }
        assertEquals(0, pigeon.pigeonScavengeTimer,
                "Releasing special should end the fault line after its short recovery.");
    }

    @Test
    void pigeonShieldedSpecialConvertsIntoGroundDownSpecialWithoutHealing() throws Exception {
        BirdGame3 game = new BirdGame3();
        game.activePlayers = 1;

        Bird pigeon = new Bird(190.0, BirdGame3.BirdType.PIGEON, 0, game);
        pigeon.y = BirdGame3.GROUND_Y - 80.0;
        pigeon.health = 48.0;
        game.players[0] = pigeon;

        game.setLocalActionsForKey(game.blockKeyForPlayer(0), true);
        pigeon.update(1.0);
        assertTrue(pigeon.isBlocking, "Setup should place Pigeon into shield first.");

        game.setLocalActionsForKey(game.specialKeyForPlayer(0), true);
        pigeon.update(1.0);
        game.setLocalActionsForKey(game.specialKeyForPlayer(0), false);
        game.setLocalActionsForKey(game.blockKeyForPlayer(0), false);

        assertFalse(pigeon.isBlocking, "Pressing special out of shield should drop shield into down special.");
        assertTrue(getPrivateInt(pigeon, "pigeonScavengeTimer") > 0,
                "Special while shielding should activate grounded scavenge.");

        for (int i = 0; i < 170; i++) {
            pigeon.update(1.0);
        }

        assertEquals(48.0, pigeon.health, 0.0001,
                "Shield-canceled down special should not heal on completion.");
    }

    @Test
    void pigeonGroundDownSpecialDoesNotHealIfInterrupted() {
        BirdGame3 game = new BirdGame3();
        game.activePlayers = 1;

        Bird pigeon = new Bird(190.0, BirdGame3.BirdType.PIGEON, 0, game);
        pigeon.y = BirdGame3.GROUND_Y - 80.0;
        pigeon.health = 48.0;
        game.players[0] = pigeon;

        game.setLocalActionsForKey(game.blockKeyForPlayer(0), true);
        game.setLocalActionsForKey(game.specialKeyForPlayer(0), true);
        pigeon.update(1.0);
        game.setLocalActionsForKey(game.blockKeyForPlayer(0), false);
        game.setLocalActionsForKey(game.specialKeyForPlayer(0), false);

        for (int i = 0; i < 8; i++) {
            pigeon.update(1.0);
        }
        double damageTaken = pigeon.receiveExternalDamage(5.0);
        assertTrue(damageTaken > 0.0, "The interruption check needs Pigeon to actually take damage.");
        double healthAfterInterruption = pigeon.health;

        for (int i = 0; i < 170; i++) {
            pigeon.update(1.0);
        }

        assertEquals(healthAfterInterruption, pigeon.health, 0.0001,
                "Interrupted scavenge should not heal Pigeon afterward.");
    }

    @Test
    void pigeonAirDownSpecialCanBeHeldAsAMultiHitDrill() {
        BirdGame3 game = new BirdGame3();
        game.activePlayers = 2;

        Bird pigeon = new Bird(160.0, BirdGame3.BirdType.PIGEON, 0, game);
        Bird target = new Bird(170.0, BirdGame3.BirdType.EAGLE, 1, game);
        pigeon.y = BirdGame3.GROUND_Y - 280.0;
        target.y = BirdGame3.GROUND_Y - 170.0;
        game.players[0] = pigeon;
        game.players[1] = target;

        double startingHealth = target.health;
        game.setLocalActionsForKey(game.blockKeyForPlayer(0), true);
        game.setLocalActionsForKey(game.specialKeyForPlayer(0), true);
        pigeon.update(1.0);
        game.setLocalActionsForKey(game.blockKeyForPlayer(0), false);

        assertTrue(pigeon.vy >= 6.0, "Air down special should immediately commit Pigeon to a downward drill.");
        for (int i = 0; i < 12; i++) {
            pigeon.update(1.0);
        }

        assertTrue(pigeon.pigeonScavengeHoldFrames >= 12,
                "The aerial drill should remain active while special is held.");
        assertTrue(target.health < startingHealth, "The held drill should damage targets below Pigeon.");
        assertTrue(target.vy > 0.0, "The held drill should drag targets downward.");

        game.setLocalActionsForKey(game.specialKeyForPlayer(0), false);
    }

    @Test
    void phoenixFullMeterSpecialTriggersRebirthNova() throws Exception {
        BirdGame3 game = new BirdGame3();
        game.activePlayers = 2;

        Bird phoenix = new Bird(160.0, BirdGame3.BirdType.PHOENIX, 0, game);
        Bird target = new Bird(240.0, BirdGame3.BirdType.PIGEON, 1, game);
        phoenix.y = BirdGame3.GROUND_Y - 80.0;
        target.y = BirdGame3.GROUND_Y - 80.0;
        game.players[0] = phoenix;
        game.players[1] = target;

        setPrivateDouble(phoenix, "ultimateMeter", 100.0);
        double startingHealth = target.health;

        game.setLocalActionsForKey(game.specialKeyForPlayer(0), true);
        phoenix.update(1.0);

        assertFalse(getPrivateBoolean(phoenix, "phoenixCharging"));
        assertEquals(Bird.PHOENIX_REBIRTH_NOVA_TOTAL_FRAMES - 1,
                getPrivateInt(phoenix, "phoenixRebirthNovaTimer"));
        assertEquals(0.0, phoenix.getUltimateRatio(), 0.0001,
                "Starting Rebirth Nova should consume Phoenix's ultimate meter.");

        for (int i = 0; i < Bird.PHOENIX_REBIRTH_NOVA_WINDUP_FRAMES + 2; i++) {
            phoenix.update(1.0);
        }

        game.setLocalActionsForKey(game.specialKeyForPlayer(0), false);
        phoenix.update(1.0);

        assertTrue(getPrivateBoolean(phoenix, "phoenixRebirthNovaDetonated"));
        assertTrue(getPrivateInt(phoenix, "phoenixRebirthNovaBuffTimer") > 0,
                "Rebirth Nova should leave Phoenix in its reborn flame buff.");
        assertEquals(0, phoenix.specialCooldown,
                "Rebirth Nova should not leave a visible special cooldown.");
        assertEquals(0, getPrivateInt(phoenix, "phoenixAfterburnTimer"),
                "Rebirth Nova should not leave Phoenix with a lingering damaging afterburn.");
        assertTrue(target.health < startingHealth,
                "Rebirth Nova should damage nearby enemies when it detonates.");
    }

    @Test
    void phoenixNeutralChargeDamageAndKnockbackScaleWithHeldFramesOnlyOnBurst() throws Exception {
        BirdGame3 quickGame = new BirdGame3();
        quickGame.activePlayers = 2;
        Bird quickPhoenix = new Bird(160.0, BirdGame3.BirdType.PHOENIX, 0, quickGame);
        Bird quickTarget = new Bird(222.0, BirdGame3.BirdType.PIGEON, 1, quickGame);
        quickPhoenix.y = BirdGame3.GROUND_Y - 80.0;
        quickTarget.y = BirdGame3.GROUND_Y - 80.0;
        quickGame.players[0] = quickPhoenix;
        quickGame.players[1] = quickTarget;

        PhoenixSpecials.neutral(quickPhoenix, false);
        setPrivateInt(quickPhoenix, "phoenixChargeTimer", 5);
        PhoenixSpecials.releaseCharge(quickPhoenix);
        double quickDamage = Bird.STARTING_HEALTH - quickTarget.health;
        double quickKnockback = Math.hypot(quickTarget.vx, quickTarget.vy);

        BirdGame3 chargedGame = new BirdGame3();
        chargedGame.activePlayers = 2;
        Bird chargedPhoenix = new Bird(160.0, BirdGame3.BirdType.PHOENIX, 0, chargedGame);
        Bird chargedTarget = new Bird(222.0, BirdGame3.BirdType.PIGEON, 1, chargedGame);
        chargedPhoenix.y = BirdGame3.GROUND_Y - 80.0;
        chargedTarget.y = BirdGame3.GROUND_Y - 80.0;
        chargedGame.players[0] = chargedPhoenix;
        chargedGame.players[1] = chargedTarget;

        PhoenixSpecials.neutral(chargedPhoenix, false);
        setPrivateInt(chargedPhoenix, "phoenixChargeTimer", 70);
        PhoenixSpecials.releaseCharge(chargedPhoenix);
        double chargedDamage = Bird.STARTING_HEALTH - chargedTarget.health;
        double chargedKnockback = Math.hypot(chargedTarget.vx, chargedTarget.vy);

        assertTrue(chargedDamage > quickDamage,
                "A longer Phoenix neutral charge should deal more burst damage.");
        assertTrue(chargedKnockback > quickKnockback,
                "A longer Phoenix neutral charge should launch harder on the burst.");
        assertEquals(0, getPrivateInt(chargedPhoenix, "phoenixAfterburnTimer"),
                "The neutral special should not transition into a lingering afterburn hitbox.");
    }

    @Test
    void phoenixNeutralSpecialCannotBeImmediatelySpamRestarted() throws Exception {
        BirdGame3 game = new BirdGame3();
        game.activePlayers = 1;

        Bird phoenix = new Bird(160.0, BirdGame3.BirdType.PHOENIX, 0, game);
        phoenix.y = BirdGame3.GROUND_Y - 80.0;
        game.players[0] = phoenix;

        KeyCode specialKey = game.specialKeyForPlayer(0);
        game.setLocalActionsForKey(specialKey, true);
        phoenix.update(1.0);
        assertTrue(getPrivateBoolean(phoenix, "phoenixCharging"));

        game.setLocalActionsForKey(specialKey, false);
        phoenix.update(1.0);
        assertFalse(getPrivateBoolean(phoenix, "phoenixCharging"));
        assertTrue(getPrivateInt(phoenix, "phoenixNeutralReuseTimer") > 0,
                "The burst should start a short neutral-special reuse gate.");

        game.setLocalActionsForKey(specialKey, true);
        phoenix.update(1.0);

        assertFalse(getPrivateBoolean(phoenix, "phoenixCharging"),
                "Phoenix should not be able to immediately restart neutral special after bursting.");
    }

    @Test
    void phoenixUpSpecialUltimateDealsMoreDamageThanBaseVersion() throws Exception {
        BirdGame3 normalGame = new BirdGame3();
        normalGame.activePlayers = 2;
        Bird normalPhoenix = new Bird(160.0, BirdGame3.BirdType.PHOENIX, 0, normalGame);
        Bird normalTarget = new Bird(166.0, BirdGame3.BirdType.PIGEON, 1, normalGame);
        normalPhoenix.y = BirdGame3.GROUND_Y - 80.0;
        normalTarget.y = BirdGame3.GROUND_Y - 80.0;
        normalGame.players[0] = normalPhoenix;
        normalGame.players[1] = normalTarget;

        PhoenixSpecials.up(normalPhoenix, false);
        assertEquals(0, normalPhoenix.specialCooldown);
        normalPhoenix.update(1.0);
        double normalDamage = Bird.STARTING_HEALTH - normalTarget.health;

        BirdGame3 ultimateGame = new BirdGame3();
        ultimateGame.activePlayers = 2;
        Bird ultimatePhoenix = new Bird(160.0, BirdGame3.BirdType.PHOENIX, 0, ultimateGame);
        Bird ultimateTarget = new Bird(166.0, BirdGame3.BirdType.PIGEON, 1, ultimateGame);
        ultimatePhoenix.y = BirdGame3.GROUND_Y - 80.0;
        ultimateTarget.y = BirdGame3.GROUND_Y - 80.0;
        ultimateGame.players[0] = ultimatePhoenix;
        ultimateGame.players[1] = ultimateTarget;

        PhoenixSpecials.up(ultimatePhoenix, true);
        assertEquals(0, ultimatePhoenix.specialCooldown);
        ultimatePhoenix.update(1.0);
        double ultimateDamage = Bird.STARTING_HEALTH - ultimateTarget.health;

        assertTrue(ultimateDamage > normalDamage,
                "Helix Ascent should hit harder than the base Firespin.");
    }

    @Test
    void phoenixUpSpecialCarriesCaughtTargetsUpwardWithTickDamage() throws Exception {
        BirdGame3 game = new BirdGame3();
        game.activePlayers = 2;

        Bird phoenix = new Bird(160.0, BirdGame3.BirdType.PHOENIX, 0, game);
        Bird target = new Bird(164.0, BirdGame3.BirdType.PIGEON, 1, game);
        phoenix.y = BirdGame3.GROUND_Y - 80.0;
        target.y = BirdGame3.GROUND_Y - 80.0;
        game.players[0] = phoenix;
        game.players[1] = target;

        PhoenixSpecials.up(phoenix, false);
        double startingHealth = target.health;
        for (int i = 0; i < 8; i++) {
            phoenix.update(1.0);
        }

        assertTrue(target.health < startingHealth,
                "Firespin should repeatedly burn enemies caught in the rising flame column.");
        assertTrue(target.vy < 0.0,
                "Enemies caught by Firespin should be carried upward with the flames.");
    }

    @Test
    void phoenixGroundedUpSpecialContinuesAsRecoveryAfterLeavingGround() throws Exception {
        BirdGame3 game = new BirdGame3();
        game.activePlayers = 1;

        Bird phoenix = new Bird(160.0, BirdGame3.BirdType.PHOENIX, 0, game);
        phoenix.y = BirdGame3.GROUND_Y - 80.0;
        game.players[0] = phoenix;

        game.setLocalActionsForKey(game.jumpKeyForPlayer(0), true);
        game.setLocalActionsForKey(game.specialKeyForPlayer(0), true);
        phoenix.update(1.0);
        game.setLocalActionsForKey(game.jumpKeyForPlayer(0), false);
        game.setLocalActionsForKey(game.specialKeyForPlayer(0), false);

        assertTrue(getPrivateInt(phoenix, "phoenixSpiralTimer") > 0,
                "Grounded Phoenix up special should not cancel itself before the launch frame.");
        assertTrue(phoenix.vy < 0.0,
                "Grounded Phoenix up special should launch upward like the aerial version.");
    }

    @Test
    void phoenixUpSpecialStaysSpentWhenInterruptedInMidair() throws Exception {
        BirdGame3 game = new BirdGame3();
        game.activePlayers = 1;

        Bird phoenix = new Bird(190.0, BirdGame3.BirdType.PHOENIX, 0, game);
        phoenix.y = BirdGame3.GROUND_Y - 80.0;
        game.players[0] = phoenix;

        PhoenixSpecials.up(phoenix, false);
        assertTrue(getPrivateBoolean(phoenix, "phoenixSpiralUsed"));

        phoenix.y = BirdGame3.GROUND_Y - 260.0;
        phoenix.stunTime = 4.0;
        phoenix.update(1.0);

        assertTrue(getPrivateBoolean(phoenix, "phoenixSpiralUsed"),
                "Getting clipped out of Phoenix's recovery should not refresh the move for free.");

        while (phoenix.stunTime > 0.0) {
            phoenix.update(1.0);
        }

        game.setLocalActionsForKey(game.jumpKeyForPlayer(0), true);
        game.setLocalActionsForKey(game.specialKeyForPlayer(0), true);
        phoenix.update(1.0);
        game.setLocalActionsForKey(game.specialKeyForPlayer(0), false);
        game.setLocalActionsForKey(game.jumpKeyForPlayer(0), false);

        assertEquals(0, getPrivateInt(phoenix, "phoenixSpiralTimer"),
                "Phoenix should not restart its up special until it lands.");

        phoenix.y = BirdGame3.GROUND_Y - 80.0;
        phoenix.vx = 0.0;
        phoenix.vy = 0.0;
        phoenix.update(1.0);

        assertFalse(getPrivateBoolean(phoenix, "phoenixSpiralUsed"),
                "Landing should refresh Phoenix's spent up-special flag even if the move was interrupted.");
    }

    @Test
    void phoenixSideSpecialHasNoCooldownAndWaitsForHeadTiltBeforeShot() throws Exception {
        BirdGame3 game = new BirdGame3();
        game.activePlayers = 1;

        Bird phoenix = new Bird(220.0, BirdGame3.BirdType.PHOENIX, 0, game);
        phoenix.y = BirdGame3.GROUND_Y - 80.0;
        phoenix.facingRight = true;
        game.players[0] = phoenix;

        PhoenixSpecials.side(phoenix, false);

        assertEquals(0, phoenix.specialCooldown);
        assertTrue(getPrivateInt(phoenix, "phoenixCastLockTimer") > 0);

        double startX = phoenix.x;
        phoenix.vx = 4.5;
        phoenix.update(1.0);

        assertEquals(startX, phoenix.x, 0.0001,
                "Phoenix should stay planted while Snap Fire is in its cast lock.");
        assertTrue(getPrivateInt(phoenix, "phoenixCastLockTimer") > 0,
                "Snap Fire should spend its first frames tilting Phoenix's head up before the shot launches.");
        double windupX = getPrivateDouble(phoenix, "phoenixFireballX");

        while (getPrivateInt(phoenix, "phoenixCastLockTimer") > 0) {
            phoenix.update(1.0);
        }
        phoenix.update(1.0);

        assertTrue(getPrivateDouble(phoenix, "phoenixFireballX") > windupX + 12.0,
                "After the head-tilt windup, Snap Fire should launch forward as a projectile.");
    }

    @Test
    void phoenixSideSpecialCannotBeImmediatelySpamRestarted() throws Exception {
        BirdGame3 game = new BirdGame3();
        game.activePlayers = 1;

        Bird phoenix = new Bird(220.0, BirdGame3.BirdType.PHOENIX, 0, game);
        phoenix.y = BirdGame3.GROUND_Y - 80.0;
        phoenix.facingRight = true;
        game.players[0] = phoenix;

        KeyCode rightKey = game.rightKeyForPlayer(0);
        KeyCode specialKey = game.specialKeyForPlayer(0);
        game.setLocalActionsForKey(rightKey, true);
        game.setLocalActionsForKey(specialKey, true);
        phoenix.update(1.0);
        game.setLocalActionsForKey(specialKey, false);

        assertTrue(getPrivateInt(phoenix, "phoenixFireballReuseTimer") > 0);
        while (getPrivateInt(phoenix, "phoenixFireballTimer") > 0) {
            phoenix.update(1.0);
        }
        assertTrue(getPrivateInt(phoenix, "phoenixFireballReuseTimer") > 0,
                "The side-special reuse gate should outlast the projectile.");

        game.setLocalActionsForKey(specialKey, true);
        phoenix.update(1.0);

        assertEquals(0, getPrivateInt(phoenix, "phoenixFireballTimer"),
                "Phoenix should not be able to immediately restart side special after a shot.");
    }

    @Test
    void phoenixAirSideSpecialShootsDiagonallyDown() throws Exception {
        BirdGame3 game = new BirdGame3();
        game.activePlayers = 1;

        Bird phoenix = new Bird(220.0, BirdGame3.BirdType.PHOENIX, 0, game);
        phoenix.y = BirdGame3.GROUND_Y - 300.0;
        phoenix.facingRight = true;
        game.players[0] = phoenix;

        PhoenixSpecials.side(phoenix, false);

        assertTrue(getPrivateDouble(phoenix, "phoenixFireballVY") > 0.0,
                "Air Snap Fire should be aimed diagonally down.");

        while (getPrivateInt(phoenix, "phoenixCastLockTimer") > 0) {
            phoenix.update(1.0);
        }
        double launchX = getPrivateDouble(phoenix, "phoenixFireballX");
        double launchY = getPrivateDouble(phoenix, "phoenixFireballY");

        phoenix.update(1.0);

        assertTrue(getPrivateDouble(phoenix, "phoenixFireballX") > launchX,
                "Air Snap Fire should still travel forward.");
        assertTrue(getPrivateDouble(phoenix, "phoenixFireballY") > launchY,
                "Air Snap Fire should travel downward after the windup.");
    }

    @Test
    void phoenixSideSpecialTravelsFartherThenFizzlesHarmlessly() throws Exception {
        BirdGame3 game = new BirdGame3();
        game.activePlayers = 1;

        Bird phoenix = new Bird(220.0, BirdGame3.BirdType.PHOENIX, 0, game);
        phoenix.y = BirdGame3.GROUND_Y - 80.0;
        phoenix.facingRight = true;
        game.players[0] = phoenix;

        PhoenixSpecials.side(phoenix, false);
        while (getPrivateInt(phoenix, "phoenixCastLockTimer") > 0) {
            phoenix.update(1.0);
        }
        double launchX = getPrivateDouble(phoenix, "phoenixFireballX");

        int guard = 0;
        while (getPrivateInt(phoenix, "phoenixFireballTimer") > 0 && guard++ < 90) {
            phoenix.update(1.0);
        }

        assertEquals(0, getPrivateInt(phoenix, "phoenixFireballTimer"),
                "Snap Fire should leave its damaging state after max range.");
        assertTrue(getPrivateDouble(phoenix, "phoenixFireballX") > launchX + 380.0,
                "Snap Fire should travel meaningfully farther before it fizzles.");
        assertTrue(getPrivateInt(phoenix, "phoenixFireballFizzleTimer") > 0,
                "Snap Fire should enter a short visible fizzle instead of vanishing.");

        Bird target = new Bird(getPrivateDouble(phoenix, "phoenixFireballX") - 40.0,
                BirdGame3.BirdType.PIGEON, 1, game);
        target.y = getPrivateDouble(phoenix, "phoenixFireballY") - 40.0;
        game.activePlayers = 2;
        game.players[1] = target;
        double healthBefore = target.health;

        for (int i = 0; i < 6; i++) {
            phoenix.update(1.0);
        }

        assertEquals(healthBefore, target.health, 0.0001,
                "The fizzle tail should be visual only and must not keep a lingering hitbox.");
    }

    @Test
    void phoenixAirSideSpecialPrimesDiagonalPoseAndLandingFlare() throws Exception {
        BirdGame3 game = new BirdGame3();
        game.activePlayers = 1;

        Bird phoenix = new Bird(220.0, BirdGame3.BirdType.PHOENIX, 0, game);
        phoenix.y = BirdGame3.GROUND_Y - 300.0;
        phoenix.facingRight = true;
        game.players[0] = phoenix;

        PhoenixSpecials.side(phoenix, false);

        assertTrue(getPrivateInt(phoenix, "phoenixAirSideAimPoseTimer") > 0,
                "Air Snap Fire should hold a diagonal-down aim pose.");
        assertTrue(getPrivateInt(phoenix, "phoenixAirSideLandingPrimeTimer") > 0,
                "Air Snap Fire should prime a landing flare.");

        phoenix.y = BirdGame3.GROUND_Y - 80.0;
        phoenix.vy = 0.0;
        phoenix.update(1.0);

        assertEquals(0, getPrivateInt(phoenix, "phoenixAirSideLandingPrimeTimer"),
                "The landing flare should consume the primed landing cue.");
        assertTrue(getPrivateInt(phoenix, "phoenixAirSideLandingFxTimer") > 0,
                "Landing after Air Snap Fire should play a brief ember skid.");
    }

    @Test
    void phoenixGroundDownSpecialEruptsVerticallyInsteadOfSpreadingOutward() throws Exception {
        BirdGame3 game = new BirdGame3();
        game.activePlayers = 3;

        Bird phoenix = new Bird(260.0, BirdGame3.BirdType.PHOENIX, 0, game);
        Bird centerTarget = new Bird(262.0, BirdGame3.BirdType.PIGEON, 1, game);
        Bird sideTarget = new Bird(420.0, BirdGame3.BirdType.EAGLE, 2, game);
        phoenix.y = BirdGame3.GROUND_Y - 80.0;
        centerTarget.y = BirdGame3.GROUND_Y - 80.0;
        sideTarget.y = BirdGame3.GROUND_Y - 80.0;
        game.players[0] = phoenix;
        game.players[1] = centerTarget;
        game.players[2] = sideTarget;

        PhoenixSpecials.down(phoenix, false);

        assertEquals(0, phoenix.specialCooldown);
        assertFalse(getPrivateBoolean(phoenix, "phoenixLavaAirborne"));
        assertTrue(getPrivateInt(phoenix, "phoenixLavaReuseTimer") > 0,
                "Ground Faultfire should use an invisible reuse timer instead of the visible cooldown bar.");

        double centerStart = centerTarget.health;
        double sideStart = sideTarget.health;
        for (int i = 0; i < 20; i++) {
            phoenix.update(1.0);
        }

        assertTrue(centerTarget.health < centerStart,
                "Ground Faultfire should erupt under targets close to Phoenix.");
        assertEquals(sideStart, sideTarget.health, 0.0001,
                "Ground Faultfire should no longer spread outward across the floor.");
        assertTrue(centerTarget.vy < 0.0,
                "The eruption should launch caught targets upward.");
    }

    @Test
    void phoenixInvisibleReuseTimersDoNotTriggerCooldownFlash() {
        BirdGame3 game = new BirdGame3();
        game.activePlayers = 1;

        Bird phoenix = new Bird(260.0, BirdGame3.BirdType.PHOENIX, 0, game);
        phoenix.y = BirdGame3.GROUND_Y - 80.0;
        game.players[0] = phoenix;

        KeyCode blockKey = game.blockKeyForPlayer(0);
        KeyCode specialKey = game.specialKeyForPlayer(0);
        game.setLocalActionsForKey(blockKey, true);
        game.setLocalActionsForKey(specialKey, true);
        phoenix.update(1.0);

        game.setLocalActionsForKey(specialKey, false);
        phoenix.update(1.0);

        game.setLocalActionsForKey(specialKey, true);
        phoenix.update(1.0);

        assertEquals(0, phoenix.specialCooldown,
                "Phoenix down special should keep its cooldown invisible.");
    }

    @Test
    void phoenixAirDownSpecialBurnsTargetsDirectlyBelow() throws Exception {
        BirdGame3 game = new BirdGame3();
        game.activePlayers = 3;

        Bird phoenix = new Bird(220.0, BirdGame3.BirdType.PHOENIX, 0, game);
        Bird belowTarget = new Bird(222.0, BirdGame3.BirdType.PIGEON, 1, game);
        Bird sideTarget = new Bird(340.0, BirdGame3.BirdType.EAGLE, 2, game);
        phoenix.y = BirdGame3.GROUND_Y - 300.0;
        belowTarget.y = BirdGame3.GROUND_Y - 110.0;
        sideTarget.y = BirdGame3.GROUND_Y - 110.0;
        game.players[0] = phoenix;
        game.players[1] = belowTarget;
        game.players[2] = sideTarget;

        PhoenixSpecials.down(phoenix, false);

        assertTrue(getPrivateBoolean(phoenix, "phoenixLavaAirborne"));

        double belowStart = belowTarget.health;
        double sideStart = sideTarget.health;
        for (int i = 0; i < 12; i++) {
            phoenix.update(1.0);
        }

        assertTrue(belowTarget.health < belowStart,
                "Air Faultfire should damage targets directly below Phoenix.");
        assertEquals(sideStart, sideTarget.health, 0.0001,
                "Air Faultfire should stay in a narrow vertical lane instead of splashing sideways.");
        assertTrue(belowTarget.vy > 0.0,
                "The vertical flame stream should force targets downward.");
    }

    @Test
    void phoenixAirDownSpecialExtendsWhileSpecialIsHeld() throws Exception {
        BirdGame3 game = new BirdGame3();
        game.activePlayers = 1;

        Bird phoenix = new Bird(220.0, BirdGame3.BirdType.PHOENIX, 0, game);
        phoenix.y = BirdGame3.GROUND_Y - 300.0;
        game.players[0] = phoenix;

        PhoenixSpecials.down(phoenix, false);
        game.setLocalActionsForKey(game.specialKeyForPlayer(0), true);

        for (int i = 0; i < Bird.PHOENIX_LAVA_FRAMES + 16; i++) {
            phoenix.update(1.0);
        }

        assertTrue(getPrivateInt(phoenix, "phoenixLavaTimer") > 0,
                "Holding special in the air should sustain Faultfire past its old fixed duration.");
        assertTrue(getPrivateInt(phoenix, "phoenixLavaHoldFrames") > 0,
                "The held air stream should track held frames for deterministic sync.");

        game.setLocalActionsForKey(game.specialKeyForPlayer(0), false);
        for (int i = 0; i < Bird.PHOENIX_LAVA_FRAMES + 4; i++) {
            phoenix.update(1.0);
        }

        assertEquals(0, getPrivateInt(phoenix, "phoenixLavaTimer"),
                "Air Faultfire should expire after the player releases special.");
    }

    @Test
    void eagleAiUsesIndependentRaptorSpecialCadence() {
        assertEquals(20, Bird.aiSpecialDecisionCooldownFor(BirdGame3.BirdType.EAGLE),
                "Eagle's independent raptor reuse timers should use the technical-kit CPU decision cadence.");
        assertEquals(26, Bird.aiSpecialDecisionCooldownFor(BirdGame3.BirdType.FALCON),
                "Falcon's established decision cadence should remain unchanged by Eagle's pass.");
    }

    @Test
    void eagleNeutralSpecialTapFiresOneEggAndStartsInvisibleReuseTimer() throws Exception {
        BirdGame3 game = new BirdGame3();
        game.activePlayers = 2;

        Bird eagle = new Bird(100.0, BirdGame3.BirdType.EAGLE, 0, game);
        Bird target = new Bird(195.0, BirdGame3.BirdType.PIGEON, 1, game);
        eagle.y = BirdGame3.GROUND_Y - 80.0;
        target.y = BirdGame3.GROUND_Y - 80.0;
        eagle.facingRight = true;
        game.players[0] = eagle;
        game.players[1] = target;

        double startingHealth = target.health;
        RaptorSpecials.neutral(eagle, false);
        assertTrue(eagle.raptorEggCharging, "Neutral should begin a hold instead of hitting immediately.");
        RaptorSpecials.handleState(eagle, false);
        for (int frame = 0; frame < 10 && target.health == startingHealth; frame++) {
            RaptorSpecials.handleState(eagle, false);
        }

        assertEquals(startingHealth - 5.0, target.health, 0.0001,
                "A tapped Eagle egg should deal the base projectile damage.");
        assertTrue(getPrivateInt(eagle, "raptorCryTimer") > 0);
        assertTrue(getPrivateInt(eagle, "raptorCryReuseTimer") > 0);
        assertEquals(0, getPrivateInt(eagle, "specialCooldown"),
                "Egg Volley should not use the visible special cooldown bar.");
        assertTrue(target.vx > 0.0, "The egg should push targets forward.");
        assertTrue(target.vy < 0.0, "The egg should pop targets upward.");
    }

    @Test
    void eagleSideSpecialUsesDirectionalInputForTalonRush() throws Exception {
        BirdGame3 game = new BirdGame3();
        game.activePlayers = 2;

        Bird eagle = new Bird(100.0, BirdGame3.BirdType.EAGLE, 0, game);
        Bird target = new Bird(200.0, BirdGame3.BirdType.PIGEON, 1, game);
        eagle.y = BirdGame3.GROUND_Y - 80.0;
        target.y = BirdGame3.GROUND_Y - 80.0;
        eagle.facingRight = true;
        game.players[0] = eagle;
        game.players[1] = target;

        double startingHealth = target.health;
        RaptorSpecials.side(eagle, false);
        assertTrue(eagle.raptorRushCharging);
        for (int frame = 0; frame < Bird.RAPTOR_RUSH_MAX_CHARGE_FRAMES; frame++) {
            RaptorSpecials.handleState(eagle, true);
        }

        assertTrue(getPrivateInt(eagle, "raptorRushTimer") > 0);
        assertTrue(getPrivateInt(eagle, "raptorRushReuseTimer") > 0);
        assertEquals(1.0, eagle.raptorRushChargeRatio, 0.0001);
        assertTrue(eagle.raptorRushTimer > Bird.EAGLE_RUSH_GROUND_FRAMES,
                "A full hold should extend Talon Rush's active travel time.");
        assertEquals(0, getPrivateInt(eagle, "specialCooldown"),
                "Talon Rush should not trigger the visible cooldown bar.");
        assertTrue(eagle.vx > 20.0, "A full Talon Rush charge should be much faster than its tap version.");
        assertEquals(startingHealth - 16.0, target.health, 0.0001,
                "A full Talon Rush charge should scale its damage.");
        assertTrue(target.vy < -13.0, "A full Talon Rush charge should launch harder.");
    }

    @Test
    void falconSideSpecialAlsoBuildsSpeedDurationAndPowerWhileHeld() {
        BirdGame3 game = new BirdGame3();
        game.activePlayers = 2;
        Bird falcon = new Bird(100.0, BirdGame3.BirdType.FALCON, 0, game);
        Bird target = new Bird(190.0, BirdGame3.BirdType.PIGEON, 1, game);
        falcon.y = BirdGame3.GROUND_Y - 80.0;
        target.y = BirdGame3.GROUND_Y - 80.0;
        falcon.facingRight = true;
        game.players[0] = falcon;
        game.players[1] = target;

        RaptorSpecials.side(falcon, false);
        for (int frame = 0; frame < Bird.RAPTOR_RUSH_MAX_CHARGE_FRAMES; frame++) {
            RaptorSpecials.handleState(falcon, true);
        }

        assertEquals(1.0, falcon.raptorRushChargeRatio, 0.0001);
        assertTrue(falcon.raptorRushTimer > Bird.FALCON_RUSH_GROUND_FRAMES);
        assertTrue(falcon.vx > 25.0, "Falcon's full rush should retain the faster echo identity.");
        assertTrue(target.health <= Bird.STARTING_HEALTH - 11.0,
                "Falcon's full rush should gain meaningful damage from its charge.");
    }

    @Test
    void raptorChargeAndEggProjectileStateSurvivesLanSnapshots() {
        BirdGame3 game = new BirdGame3();
        Bird eagle = new Bird(100.0, BirdGame3.BirdType.EAGLE, 0, game);
        eagle.raptorEggCharging = true;
        eagle.raptorEggChargeFrames = 27;
        eagle.raptorEggDirection = -1;
        eagle.raptorEggActive[1] = true;
        eagle.raptorEggX[1] = 333.25;
        eagle.raptorEggY[1] = 444.5;
        eagle.raptorEggVX[1] = -14.75;
        eagle.raptorEggVY[1] = 1.5;
        eagle.raptorEggPower[1] = 0.625;
        eagle.raptorEggLife[1] = 48;
        eagle.raptorRushCharging = true;
        eagle.raptorRushChargeFrames = 19;
        eagle.raptorRushChargeRatio = 0.45;

        Bird restored = new Bird(0.0, BirdGame3.BirdType.EAGLE, 0, game);
        restored.applyLanState(eagle.toLanState());

        assertTrue(restored.raptorEggCharging);
        assertEquals(27, restored.raptorEggChargeFrames);
        assertEquals(-1, restored.raptorEggDirection);
        assertTrue(restored.raptorEggActive[1]);
        assertEquals(333.25, restored.raptorEggX[1]);
        assertEquals(444.5, restored.raptorEggY[1]);
        assertEquals(-14.75, restored.raptorEggVX[1]);
        assertEquals(1.5, restored.raptorEggVY[1]);
        assertEquals(0.625, restored.raptorEggPower[1]);
        assertEquals(48, restored.raptorEggLife[1]);
        assertTrue(restored.raptorRushCharging);
        assertEquals(19, restored.raptorRushChargeFrames);
        assertEquals(0.45, restored.raptorRushChargeRatio);
    }

    @Test
    void eagleNeutralReuseTimerOnlyBlocksRepeatingEggVolley() throws Exception {
        BirdGame3 game = new BirdGame3();
        game.activePlayers = 2;

        Bird eagle = new Bird(100.0, BirdGame3.BirdType.EAGLE, 0, game);
        Bird target = new Bird(220.0, BirdGame3.BirdType.PIGEON, 1, game);
        eagle.y = BirdGame3.GROUND_Y - 80.0;
        target.y = BirdGame3.GROUND_Y - 80.0;
        eagle.facingRight = true;
        game.players[0] = eagle;
        game.players[1] = target;

        game.setLocalActionsForKey(game.specialKeyForPlayer(0), true);
        eagle.update(1.0);
        game.setLocalActionsForKey(game.specialKeyForPlayer(0), false);
        eagle.update(1.0);

        while (getPrivateInt(eagle, "raptorCryTimer") > 0) {
            eagle.update(1.0);
        }

        assertTrue(getPrivateInt(eagle, "raptorCryReuseTimer") > 0);

        game.setLocalActionsForKey(game.specialKeyForPlayer(0), true);
        eagle.update(1.0);
        game.setLocalActionsForKey(game.specialKeyForPlayer(0), false);

        assertFalse(eagle.raptorEggCharging,
                "Egg Volley should stay locked until its hidden reuse timer expires.");

        eagle.update(1.0);

        game.setLocalActionsForKey(game.rightKeyForPlayer(0), true);
        game.setLocalActionsForKey(game.specialKeyForPlayer(0), true);
        eagle.update(1.0);
        game.setLocalActionsForKey(game.specialKeyForPlayer(0), false);
        game.setLocalActionsForKey(game.rightKeyForPlayer(0), false);
        eagle.update(1.0);

        assertTrue(getPrivateInt(eagle, "raptorRushTimer") > 0,
                "The hidden Egg Volley timer should not block Talon Rush.");
    }

    @Test
    void eagleUpSpecialOverridesGroundJumpAndStartsSkyrise() throws Exception {
        BirdGame3 game = new BirdGame3();
        game.activePlayers = 1;

        Bird eagle = new Bird(190.0, BirdGame3.BirdType.EAGLE, 0, game);
        eagle.y = BirdGame3.GROUND_Y - 80.0;
        game.players[0] = eagle;

        game.setLocalActionsForKey(game.jumpKeyForPlayer(0), true);
        game.setLocalActionsForKey(game.specialKeyForPlayer(0), true);
        eagle.update(1.0);
        game.setLocalActionsForKey(game.specialKeyForPlayer(0), false);
        game.setLocalActionsForKey(game.jumpKeyForPlayer(0), false);

        assertEquals(0, getPrivateInt(eagle, "jumpSquatTimer"),
                "Skyrise should bypass jump squat instead of becoming a normal jump.");
        assertTrue(getPrivateInt(eagle, "raptorClimbTimer") > 0);
        assertTrue(getPrivateBoolean(eagle, "raptorUpSpecialUsed"));
        assertEquals(0, getPrivateInt(eagle, "specialCooldown"),
                "Skyrise should not trigger the visible cooldown bar.");
        assertTrue(eagle.vy < -10.0, "Skyrise should launch Eagle sharply upward.");
    }

    @Test
    void eagleUpSpecialCannotBeUsedAgainUntilLanding() throws Exception {
        BirdGame3 game = new BirdGame3();
        game.activePlayers = 1;

        Bird eagle = new Bird(190.0, BirdGame3.BirdType.EAGLE, 0, game);
        eagle.y = BirdGame3.GROUND_Y - 80.0;
        game.players[0] = eagle;

        game.setLocalActionsForKey(game.jumpKeyForPlayer(0), true);
        game.setLocalActionsForKey(game.specialKeyForPlayer(0), true);
        eagle.update(1.0);
        game.setLocalActionsForKey(game.specialKeyForPlayer(0), false);
        game.setLocalActionsForKey(game.jumpKeyForPlayer(0), false);

        eagle.y = BirdGame3.GROUND_Y - 260.0;

        while (getPrivateInt(eagle, "raptorClimbTimer") > 0) {
            eagle.update(1.0);
        }

        assertTrue(getPrivateBoolean(eagle, "raptorUpSpecialUsed"));
        assertFalse(eagle.isOnGround(), "Skyrise should still be spent while Eagle is airborne.");

        game.setLocalActionsForKey(game.jumpKeyForPlayer(0), true);
        game.setLocalActionsForKey(game.specialKeyForPlayer(0), true);
        eagle.update(1.0);
        game.setLocalActionsForKey(game.specialKeyForPlayer(0), false);
        game.setLocalActionsForKey(game.jumpKeyForPlayer(0), false);

        assertEquals(0, getPrivateInt(eagle, "raptorClimbTimer"),
                "Skyrise should not restart again before Eagle lands.");

        eagle.y = BirdGame3.GROUND_Y - 80.0;
        eagle.vx = 0.0;
        eagle.vy = 0.0;
        eagle.update(1.0);

        assertFalse(getPrivateBoolean(eagle, "raptorUpSpecialUsed"),
                "Touching the ground should refresh Skyrise.");

        game.setLocalActionsForKey(game.jumpKeyForPlayer(0), true);
        game.setLocalActionsForKey(game.specialKeyForPlayer(0), true);
        eagle.update(1.0);
        game.setLocalActionsForKey(game.specialKeyForPlayer(0), false);
        game.setLocalActionsForKey(game.jumpKeyForPlayer(0), false);

        assertTrue(getPrivateInt(eagle, "raptorClimbTimer") > 0,
                "Skyrise should become available again after Eagle lands.");
    }

    @Test
    void eagleSkyriseRecoveryTravelIsStrongButBounded() {
        BirdGame3 game = new BirdGame3();
        game.activePlayers = 1;
        Bird eagle = new Bird(190.0, BirdGame3.BirdType.EAGLE, 0, game);
        eagle.y = 1700.0;
        game.players[0] = eagle;

        double startY = eagle.y;
        RaptorSpecials.up(eagle, false);
        for (int frame = 0; frame < Bird.EAGLE_CLIMB_FRAMES; frame++) {
            eagle.update(1.0);
        }

        double rise = startY - eagle.y;
        assertTrue(rise >= 145.0, "Skyrise still needs enough lift to rescue Eagle below a platform.");
        assertTrue(rise <= 230.0, "Skyrise should not erase a deep launch by itself.");
    }

    @Test
    void eagleAiCommitsSkyriseWhenRecoveringFromTheVoid() {
        BirdGame3 game = new BirdGame3();
        game.activePlayers = 2;
        game.selectedMap = BirdGame3.MapType.BATTLEFIELD;
        double islandX = 2400.0;
        double islandY = BirdGame3.GROUND_Y - 80.0;
        game.platforms.add(new Platform(islandX, islandY, 1200.0, 70.0));

        Bird eagle = new Bird(islandX - 310.0, BirdGame3.BirdType.EAGLE, 0, game);
        eagle.y = islandY + 130.0;
        eagle.vx = -3.0;
        Bird target = new Bird(islandX + 320.0, BirdGame3.BirdType.PIGEON, 1, game);
        target.y = islandY - target.bodyHeight();
        game.players[0] = eagle;
        game.players[1] = target;
        game.isAI[0] = true;

        eagle.update(1.0);

        assertTrue(game.isRightPressed(0), "Eagle should steer back toward the island.");
        assertTrue(eagle.raptorUpSpecialUsed, "Eagle should spend Skyrise once the recovery becomes urgent.");
        assertTrue(eagle.raptorClimbTimer > 0);
    }

    @Test
    void eagleDownSpecialUsesBlockInputWithoutRaisingShield() {
        BirdGame3 game = new BirdGame3();
        game.activePlayers = 1;

        Bird eagle = new Bird(190.0, BirdGame3.BirdType.EAGLE, 0, game);
        eagle.y = BirdGame3.GROUND_Y - 80.0;
        game.players[0] = eagle;

        game.setLocalActionsForKey(game.blockKeyForPlayer(0), true);
        game.setLocalActionsForKey(game.specialKeyForPlayer(0), true);
        eagle.update(1.0);
        game.setLocalActionsForKey(game.blockKeyForPlayer(0), false);
        game.setLocalActionsForKey(game.specialKeyForPlayer(0), false);

        assertFalse(eagle.isBlocking, "Heavenfall should reserve block input instead of raising shield.");
        assertTrue(eagle.eagleDiveActive, "Block + special should start Eagle's dive special.");
        assertTrue(eagle.diveTimer > 0);
    }

    @Test
    void eagleGroundDownSpecialLeapsBeforeDiveBegins() {
        BirdGame3 game = new BirdGame3();
        game.activePlayers = 2;

        Bird eagle = new Bird(190.0, BirdGame3.BirdType.EAGLE, 0, game);
        Bird target = new Bird(220.0, BirdGame3.BirdType.PIGEON, 1, game);
        eagle.y = BirdGame3.GROUND_Y - 80.0;
        target.y = BirdGame3.GROUND_Y - 80.0;
        game.players[0] = eagle;
        game.players[1] = target;

        double startingHealth = target.health;

        game.setLocalActionsForKey(game.blockKeyForPlayer(0), true);
        game.setLocalActionsForKey(game.specialKeyForPlayer(0), true);
        eagle.update(1.0);
        game.setLocalActionsForKey(game.blockKeyForPlayer(0), false);
        game.setLocalActionsForKey(game.specialKeyForPlayer(0), false);

        assertTrue(eagle.eagleDiveCountdown > 0, "Grounded Heavenfall should spend a few startup frames leaping first.");
        assertFalse(eagle.isOnGround(), "Grounded Heavenfall should launch Eagle off the floor before the slam starts.");
        assertEquals(startingHealth, target.health, 0.0001,
                "Grounded Heavenfall should not hit on the startup hop.");

        while (eagle.eagleDiveCountdown > 1) {
            eagle.update(1.0);
            assertFalse(eagle.isOnGround(), "Eagle should stay airborne through the leap startup.");
        }

        eagle.update(1.0);

        assertEquals(0, eagle.eagleDiveCountdown);
        assertTrue(eagle.vy >= 18.0, "After the leap, Heavenfall should transition into its fast downward slam.");
    }

    @Test
    void falconGroundDownSpecialLeapsBeforeDiagonalDiveBegins() {
        BirdGame3 game = new BirdGame3();
        game.activePlayers = 2;

        Bird falcon = new Bird(190.0, BirdGame3.BirdType.FALCON, 0, game);
        Bird target = new Bird(220.0, BirdGame3.BirdType.PIGEON, 1, game);
        falcon.y = BirdGame3.GROUND_Y - 80.0;
        target.y = BirdGame3.GROUND_Y - 80.0;
        falcon.facingRight = true;
        game.players[0] = falcon;
        game.players[1] = target;

        double startingHealth = target.health;

        game.setLocalActionsForKey(game.blockKeyForPlayer(0), true);
        game.setLocalActionsForKey(game.specialKeyForPlayer(0), true);
        falcon.update(1.0);
        game.setLocalActionsForKey(game.blockKeyForPlayer(0), false);
        game.setLocalActionsForKey(game.specialKeyForPlayer(0), false);

        assertTrue(falcon.eagleDiveCountdown > 0, "Grounded Falcon Dive should hop before it commits to the strike.");
        assertFalse(falcon.isOnGround(), "Grounded Falcon Dive should leave the ground during startup.");
        assertEquals(startingHealth, target.health, 0.0001,
                "Grounded Falcon Dive should not hit during the startup hop.");

        while (falcon.eagleDiveCountdown > 1) {
            falcon.update(1.0);
            assertFalse(falcon.isOnGround(), "Falcon should stay airborne through the startup hop.");
        }

        falcon.update(1.0);

        assertEquals(0, falcon.eagleDiveCountdown);
        assertTrue(falcon.vx > 0.0, "Falcon Dive should break forward once the hop finishes.");
        assertTrue(falcon.vy > 0.0, "Falcon Dive should angle down once the hop finishes.");
        assertEquals(falcon.vx, falcon.vy, 0.0001,
                "Falcon Dive should launch along a true diagonal after the leap.");
    }

    @Test
    void falconDownSpecialPoseFacesDiagonalDirection() throws Exception {
        BirdGame3 game = new BirdGame3();
        game.activePlayers = 1;

        Bird falcon = new Bird(190.0, BirdGame3.BirdType.FALCON, 0, game);
        falcon.y = BirdGame3.GROUND_Y - 220.0;
        falcon.facingRight = true;
        game.players[0] = falcon;

        game.setLocalActionsForKey(game.blockKeyForPlayer(0), true);
        game.setLocalActionsForKey(game.specialKeyForPlayer(0), true);
        falcon.update(1.0);
        game.setLocalActionsForKey(game.blockKeyForPlayer(0), false);
        game.setLocalActionsForKey(game.specialKeyForPlayer(0), false);

        Object pose = invokePrivateObjectMethod(falcon, "currentRaptorSpecialPose");
        assertNotNull(pose);
        assertEquals(Math.PI / 4.0, invokeDoubleMethod(pose, "aimAngleRadians"), 0.0001,
                "Falcon's dive pose should face diagonally to match the actual attack path.");
    }

    @Test
    void eagleDownSpecialCooldownDoesNotBlockEggVolley() throws Exception {
        BirdGame3 game = new BirdGame3();
        game.activePlayers = 2;

        Bird eagle = new Bird(100.0, BirdGame3.BirdType.EAGLE, 0, game);
        Bird target = new Bird(195.0, BirdGame3.BirdType.PIGEON, 1, game);
        eagle.y = BirdGame3.GROUND_Y - 80.0;
        target.y = BirdGame3.GROUND_Y - 80.0;
        eagle.facingRight = true;
        game.players[0] = eagle;
        game.players[1] = target;

        double startingHealth = target.health;
        game.setLocalActionsForKey(game.blockKeyForPlayer(0), true);
        game.setLocalActionsForKey(game.specialKeyForPlayer(0), true);
        eagle.update(1.0);
        game.setLocalActionsForKey(game.blockKeyForPlayer(0), false);
        game.setLocalActionsForKey(game.specialKeyForPlayer(0), false);

        assertTrue(getPrivateInt(eagle, "specialCooldown") > 0,
                "Heavenfall should still own its internal cooldown even though Eagle hides the bar.");

        eagle.eagleDiveActive = false;
        eagle.eagleAscentActive = false;
        eagle.eagleDiveCountdown = 0;
        eagle.diveTimer = 0;
        eagle.vx = 0.0;
        eagle.vy = 0.0;
        eagle.y = BirdGame3.GROUND_Y - 80.0;
        eagle.update(1.0);

        game.setLocalActionsForKey(game.specialKeyForPlayer(0), true);
        eagle.update(1.0);
        game.setLocalActionsForKey(game.specialKeyForPlayer(0), false);
        eagle.update(1.0);
        for (int frame = 0; frame < 10 && target.health == startingHealth; frame++) {
            eagle.update(1.0);
        }

        assertTrue(getPrivateInt(eagle, "raptorCryTimer") > 0,
                "Egg Volley should still be usable while Heavenfall cools down.");
        assertEquals(startingHealth - 5.0, target.health, 0.0001,
                "Egg Volley should still hit normally during Heavenfall's cooldown.");
        assertTrue(getPrivateInt(eagle, "specialCooldown") > 0,
                "Using Egg Volley should not erase Heavenfall's internal cooldown.");
    }

    @Test
    void falconNeutralHoldAddsEggsToTheVolley() {
        BirdGame3 tapGame = new BirdGame3();
        tapGame.activePlayers = 1;
        Bird tapFalcon = new Bird(100.0, BirdGame3.BirdType.FALCON, 0, tapGame);
        tapFalcon.y = BirdGame3.GROUND_Y - 80.0;
        tapGame.players[0] = tapFalcon;
        RaptorSpecials.neutral(tapFalcon, false);
        RaptorSpecials.handleState(tapFalcon, false);

        BirdGame3 heldGame = new BirdGame3();
        heldGame.activePlayers = 1;
        Bird heldFalcon = new Bird(100.0, BirdGame3.BirdType.FALCON, 0, heldGame);
        heldFalcon.y = BirdGame3.GROUND_Y - 80.0;
        heldGame.players[0] = heldFalcon;
        RaptorSpecials.neutral(heldFalcon, false);
        for (int frame = 0; frame < 36; frame++) {
            RaptorSpecials.handleState(heldFalcon, true);
        }
        RaptorSpecials.handleState(heldFalcon, false);

        int tappedEggs = 0;
        int heldEggs = 0;
        for (boolean active : tapFalcon.raptorEggActive) {
            if (active) tappedEggs++;
        }
        for (boolean active : heldFalcon.raptorEggActive) {
            if (active) heldEggs++;
        }
        assertEquals(1, tappedEggs);
        assertEquals(4, heldEggs,
                "Holding Falcon's neutral should build from one egg to a four-egg scatter.");
    }

    @Test
    void aerialAttackAutoCancelsOnEarlyLanding() throws Exception {
        BirdGame3 game = new BirdGame3();
        game.activePlayers = 1;

        Bird bird = new Bird(190.0, BirdGame3.BirdType.EAGLE, 0, game);
        bird.y = BirdGame3.GROUND_Y - 240.0;
        game.players[0] = bird;

        game.setLocalActionsForKey(game.attackKeyForPlayer(0), true);
        bird.update(1.0);
        game.setLocalActionsForKey(game.attackKeyForPlayer(0), false);

        bird.y = BirdGame3.GROUND_Y - 100.0;
        bird.vy = 25.0;
        bird.update(1.0);

        assertTrue(bird.isOnGround());
        assertEquals(0, getPrivateInt(bird, "landingLagTimer"));
        assertEquals(0, bird.attackAnimationTimer);

        game.setLocalActionsForKey(game.blockKeyForPlayer(0), true);
        bird.update(1.0);

        assertTrue(bird.isBlocking);
    }

    @Test
    void aerialAttackAutoCancelsOnLateLanding() throws Exception {
        BirdGame3 game = new BirdGame3();
        game.activePlayers = 1;

        Bird bird = new Bird(190.0, BirdGame3.BirdType.EAGLE, 0, game);
        bird.y = BirdGame3.GROUND_Y - 320.0;
        game.players[0] = bird;

        game.setLocalActionsForKey(game.attackKeyForPlayer(0), true);
        bird.update(1.0);
        game.setLocalActionsForKey(game.attackKeyForPlayer(0), false);

        while (bird.debugNormalAttackTimelineActive()
                && bird.debugNormalAttackFrame() <= bird.debugNormalAttackTotalFrames() - 2) {
            bird.y = BirdGame3.GROUND_Y - 320.0;
            bird.vy = 0.0;
            bird.update(1.0);
        }

        bird.y = BirdGame3.GROUND_Y - 100.0;
        bird.vy = 25.0;
        bird.update(1.0);

        assertTrue(bird.isOnGround());
        assertEquals(0, getPrivateInt(bird, "landingLagTimer"));
        assertEquals(0, bird.attackAnimationTimer);
    }

    @Test
    void aerialAttackLandingLagBlocksShieldUntilRecoveryEnds() throws Exception {
        BirdGame3 game = new BirdGame3();
        game.activePlayers = 1;

        Bird bird = new Bird(190.0, BirdGame3.BirdType.EAGLE, 0, game);
        bird.y = BirdGame3.GROUND_Y - 320.0;
        game.players[0] = bird;

        game.setLocalActionsForKey(game.attackKeyForPlayer(0), true);
        bird.update(1.0);
        game.setLocalActionsForKey(game.attackKeyForPlayer(0), false);

        for (int i = 0; i < 4; i++) {
            bird.y = BirdGame3.GROUND_Y - 320.0;
            bird.vy = 0.0;
            bird.update(1.0);
        }

        bird.y = BirdGame3.GROUND_Y - 100.0;
        bird.vy = 25.0;
        bird.update(1.0);

        assertTrue(bird.isOnGround());
        assertTrue(getPrivateInt(bird, "landingLagTimer") > 0);
        assertEquals(0, bird.attackAnimationTimer);

        game.setLocalActionsForKey(game.blockKeyForPlayer(0), true);
        bird.update(1.0);

        assertFalse(bird.isBlocking);

        for (int i = 0; i < 8; i++) {
            bird.update(1.0);
        }

        assertTrue(bird.isBlocking);
    }

    @Test
    void hitstunLandingWithShieldPressTriggersGroundTechRoll() throws Exception {
        BirdGame3 game = new BirdGame3();
        game.activePlayers = 1;
        setPrivateBoolean(game);

        Bird bird = new Bird(190.0, BirdGame3.BirdType.EAGLE, 0, game);
        bird.y = BirdGame3.GROUND_Y - 96.0;
        bird.vy = 18.0;
        bird.stunTime = 20.0;
        game.players[0] = bird;

        double startX = bird.x;
        game.setLocalActionsForKey(game.blockKeyForPlayer(0), true);
        game.setLocalActionsForKey(game.rightKeyForPlayer(0), true);

        bird.update(1.0);

        assertTrue(bird.isOnGround());
        assertEquals(0.0, bird.stunTime, 0.0001);
        assertEquals("ROLL", getPrivateObject(bird, "dodgeType").toString());
        assertEquals(0, getPrivateInt(bird, "knockdownTimer"));
        assertTrue(bird.vx > 0.0);

        bird.update(1.0);

        assertTrue(bird.x > startX + 4.0);
    }

    @Test
    void missedTechLandingEntersKnockdownAndBlocksShieldUntilRecoveryEnds() throws Exception {
        BirdGame3 game = new BirdGame3();
        game.activePlayers = 1;
        setPrivateBoolean(game);

        Bird bird = new Bird(190.0, BirdGame3.BirdType.EAGLE, 0, game);
        bird.y = BirdGame3.GROUND_Y - 96.0;
        bird.vy = 18.0;
        bird.stunTime = 20.0;
        game.players[0] = bird;

        bird.update(1.0);

        assertTrue(bird.isOnGround());
        assertEquals(0.0, bird.stunTime, 0.0001);
        assertTrue(getPrivateInt(bird, "knockdownTimer") > 0);
        assertEquals("NONE", getPrivateObject(bird, "dodgeType").toString());

        game.setLocalActionsForKey(game.blockKeyForPlayer(0), true);
        bird.update(1.0);

        assertFalse(bird.isBlocking);

        while (getPrivateInt(bird, "knockdownTimer") > 0) {
            bird.update(1.0);
        }
        bird.update(1.0);

        assertTrue(bird.isBlocking);
    }

    @Test
    void airborneShieldPressCanWallTechDuringHitstun() throws Exception {
        BirdGame3 game = new BirdGame3();
        game.activePlayers = 1;
        setPrivateBoolean(game);
        game.platforms.clear();
        Platform wall = new Platform(260.0, BirdGame3.GROUND_Y - 220.0, 32.0, 220.0);
        game.platforms.add(wall);

        Bird bird = new Bird(150.0, BirdGame3.BirdType.EAGLE, 0, game);
        bird.y = wall.y + 40.0;
        bird.vx = 36.0;
        bird.vy = 0.0;
        bird.stunTime = 18.0;
        game.players[0] = bird;

        game.setLocalActionsForKey(game.blockKeyForPlayer(0), true);
        bird.update(1.0);

        assertEquals(0.0, bird.stunTime, 0.0001);
        assertEquals(0, getPrivateInt(bird, "knockdownTimer"));
        assertTrue(getPrivateInt(bird, "dodgeInvulnerabilityTimer") > 0);
        assertEquals(wall.x - 80.0, bird.x, 0.0001);
        assertEquals(0.0, bird.vx, 0.0001);
    }

    @Test
    void battlefieldClampAdaptsToBirdRecoveryProfiles() throws Exception {
        BirdGame3 game = new BirdGame3();
        game.selectedMap = BirdGame3.MapType.BATTLEFIELD;
        double islandX = 2400.0;
        double islandY = BirdGame3.GROUND_Y - 80.0;
        game.platforms.add(new Platform(islandX, islandY, 1200, 70));

        Bird hummingbird = new Bird(2800, BirdGame3.BirdType.HUMMINGBIRD, 0, game);
        Bird pigeon = new Bird(2800, BirdGame3.BirdType.PIGEON, 1, game);
        Bird groundedVulture = new Bird(2800, BirdGame3.BirdType.VULTURE, 2, game);
        Bird risingVulture = new Bird(2800, BirdGame3.BirdType.VULTURE, 3, game);
        risingVulture.y = islandY - 280;
        risingVulture.vy = -5.0;

        Method clamp = Bird.class.getDeclaredMethod("clampGoalXAwayFromVoid", double.class);
        clamp.setAccessible(true);

        double offstageGoal = 1600.0;
        double hummingbirdGoal = (double) clamp.invoke(hummingbird, offstageGoal);
        double pigeonGoal = (double) clamp.invoke(pigeon, offstageGoal);
        double groundedVultureGoal = (double) clamp.invoke(groundedVulture, offstageGoal);
        double risingVultureGoal = (double) clamp.invoke(risingVulture, offstageGoal);

        assertTrue(hummingbirdGoal < pigeonGoal);
        assertTrue(pigeonGoal < groundedVultureGoal);
        assertTrue(risingVultureGoal < groundedVultureGoal);
    }

    @Test
    void penguinAiUsesIceJumpToRecoverOffstage() {
        BirdGame3 game = new BirdGame3();
        game.activePlayers = 2;
        game.selectedMap = BirdGame3.MapType.BATTLEFIELD;
        double islandX = 2400.0;
        double islandY = BirdGame3.GROUND_Y - 80.0;
        game.platforms.add(new Platform(islandX, islandY, 1200, 70));

        Bird penguin = new Bird(islandX - 160, BirdGame3.BirdType.PENGUIN, 0, game);
        penguin.y = islandY + 150;
        Bird target = new Bird(islandX + 280, BirdGame3.BirdType.EAGLE, 1, game);
        target.y = islandY - 80;

        game.players[0] = penguin;
        game.players[1] = target;
        game.isAI[0] = true;

        penguin.update(1.0);

        assertTrue(game.isRightPressed(0));
        assertTrue(game.isSpecialPressed(0));
    }

    @Test
    void penguinNeutralChargesThenReleasesBellySlideWithoutCooldownUi() throws Exception {
        BirdGame3 game = new BirdGame3();
        game.activePlayers = 2;

        Bird penguin = new Bird(120.0, BirdGame3.BirdType.PENGUIN, 0, game);
        Bird target = new Bird(210.0, BirdGame3.BirdType.EAGLE, 1, game);
        penguin.y = BirdGame3.GROUND_Y - 80.0;
        target.y = BirdGame3.GROUND_Y - 80.0;
        game.players[0] = penguin;
        game.players[1] = target;

        KeyCode specialKey = game.specialKeyForPlayer(0);
        game.setLocalActionsForKey(specialKey, true);
        for (int i = 0; i < 24; i++) {
            penguin.update(1.0);
        }

        assertTrue(getPrivateBoolean(penguin, "penguinBellyCharging"));
        assertEquals(0, penguin.specialCooldown);

        game.setLocalActionsForKey(specialKey, false);
        penguin.update(1.0);

        assertTrue(getPrivateInt(penguin, "penguinBellySlideTimer") > 0);
        assertTrue(target.health < Bird.STARTING_HEALTH || target.vx > 0.0);
        assertEquals(0, penguin.specialCooldown);
    }

    @Test
    void penguinUpSpecialUsesAirRecoveryRuleAndNoVisibleCooldown() throws Exception {
        BirdGame3 game = new BirdGame3();
        game.activePlayers = 1;

        Bird penguin = new Bird(220.0, BirdGame3.BirdType.PENGUIN, 0, game);
        penguin.y = BirdGame3.GROUND_Y - 260.0;
        game.players[0] = penguin;

        KeyCode jumpKey = game.jumpKeyForPlayer(0);
        KeyCode specialKey = game.specialKeyForPlayer(0);
        game.setLocalActionsForKey(jumpKey, true);
        game.setLocalActionsForKey(specialKey, true);
        penguin.update(1.0);

        assertTrue(getPrivateBoolean(penguin, "penguinUpSpecialUsed"));
        assertTrue(getPrivateInt(penguin, "penguinRocketTimer") > 0);
        assertTrue(penguin.vy < -12.0);
        assertEquals(0, penguin.specialCooldown);

        for (int i = 0; i < 28; i++) {
            penguin.update(1.0);
        }
        assertTrue(getPrivateInt(penguin, "penguinFlopTimer") > 0,
                "Holding up special should enter the slow falling blast only after the flap window.");
        assertTrue(penguin.vy > 0.0 && penguin.vy < 12.0);

        game.setLocalActionsForKey(specialKey, false);
        penguin.update(1.0);
        game.setLocalActionsForKey(specialKey, true);
        penguin.update(1.0);

        assertTrue(getPrivateBoolean(penguin, "penguinUpSpecialUsed"));
    }

    @Test
    void penguinUpSpecialCanBeSteeredWhileRising() {
        BirdGame3 game = new BirdGame3();
        game.activePlayers = 1;

        Bird penguin = new Bird(220.0, BirdGame3.BirdType.PENGUIN, 0, game);
        penguin.y = BirdGame3.GROUND_Y - 260.0;
        game.players[0] = penguin;

        game.setLocalActionsForKey(game.rightKeyForPlayer(0), true);
        game.setLocalActionsForKey(game.jumpKeyForPlayer(0), true);
        game.setLocalActionsForKey(game.specialKeyForPlayer(0), true);

        for (int i = 0; i < 8; i++) {
            penguin.update(1.0);
        }

        assertTrue(penguin.vx > 2.0, "Penguin should keep fluid horizontal control during the rocket rise.");
        assertTrue(penguin.vy < -4.0, "Penguin should still be rising while steering.");
        assertEquals(0, penguin.specialCooldown);
    }

    @Test
    void penguinUltimateStartsAbsoluteZeroFortress() throws Exception {
        BirdGame3 game = new BirdGame3();
        game.activePlayers = 2;

        Bird penguin = new Bird(300.0, BirdGame3.BirdType.PENGUIN, 0, game);
        Bird target = new Bird(620.0, BirdGame3.BirdType.PIGEON, 1, game);
        penguin.y = BirdGame3.GROUND_Y - 80.0;
        target.y = BirdGame3.GROUND_Y - 80.0;
        game.players[0] = penguin;
        game.players[1] = target;

        setPrivateDouble(penguin, "ultimateMeter", 100.0);
        invokePrivateVoid(penguin, "special");

        assertEquals(0.0, getPrivateDouble(penguin, "ultimateMeter"), 0.0001);
        assertTrue(getPrivateInt(penguin, "penguinAbsoluteZeroTimer") > 0);
        assertTrue(penguin.isCombatInvulnerable());
        assertEquals(0, penguin.specialCooldown);
        assertEquals(PenguinSpecials.ABSOLUTE_ZERO_FORTRESS_MOVE, game.lastTelemetryMoveName(0, ""));
    }

    @Test
    void penguinAbsoluteZeroFortressDropsSkyIcebergs() throws Exception {
        BirdGame3 game = new BirdGame3();
        game.activePlayers = 2;

        Bird penguin = new Bird(300.0, BirdGame3.BirdType.PENGUIN, 0, game);
        Bird target = new Bird(620.0, BirdGame3.BirdType.PIGEON, 1, game);
        penguin.y = BirdGame3.GROUND_Y - 80.0;
        target.y = BirdGame3.GROUND_Y - 80.0;
        game.players[0] = penguin;
        game.players[1] = target;

        setPrivateDouble(penguin, "ultimateMeter", 100.0);
        invokePrivateVoid(penguin, "special");
        double impactX = PenguinSpecials.absoluteZeroImpactX(penguin, 0);
        target.x = impactX - target.bodyWidth() * 0.5;
        target.y = BirdGame3.GROUND_Y - target.bodyHeight();
        double healthBefore = target.health;

        int firstImpactFrame = PenguinSpecials.absoluteZeroImpactFrame(0);
        for (int i = 0; i <= firstImpactFrame + 1; i++) {
            penguin.update(1.0);
        }

        assertTrue(target.health < healthBefore, "The first fortress iceberg should damage targets near impact.");
        assertTrue(getPrivateInt(penguin, "penguinAbsoluteZeroWaveIndex") >= 1);
        assertTrue(game.damageDealt[0] > 0);
    }

    @Test
    void grinchhawkUltimateStartsMidnightGiftstormInsteadOfBoostedSpecial() throws Exception {
        BirdGame3 game = new BirdGame3();
        game.activePlayers = 2;

        Bird grinch = new Bird(300.0, BirdGame3.BirdType.GRINCHHAWK, 0, game);
        Bird target = new Bird(430.0, BirdGame3.BirdType.PIGEON, 1, game);
        grinch.y = BirdGame3.GROUND_Y - 80.0;
        target.y = BirdGame3.GROUND_Y - 80.0;
        game.players[0] = grinch;
        game.players[1] = target;

        setPrivateDouble(grinch, "ultimateMeter", 100.0);
        invokePrivateVoid(grinch, "special");

        assertEquals(Bird.GRINCH_MIDNIGHT_GIFTSTORM_FRAMES, grinch.grinchGiftstormTimer);
        assertEquals(0, grinch.grinchHeartSnatchTimer,
                "Grinch-Hawk ultimate should not fall through into boosted Heart Snatch.");
        assertFalse(grinch.grinchSleighRiding);
        assertNull(grinch.grinchPresent);
        assertFalse(grinch.isUltimateReady());
        assertEquals(0, grinch.specialCooldown);
        assertEquals(GrinchhawkSpecials.MIDNIGHT_GIFTSTORM_MOVE, game.lastTelemetryMoveName(0, ""));
    }

    @Test
    void grinchhawkHeartSnatchTelegraphsBeforeItsActiveWindow() {
        BirdGame3 game = new BirdGame3();
        game.activePlayers = 2;

        Bird grinch = new Bird(300.0, BirdGame3.BirdType.GRINCHHAWK, 0, game);
        Bird target = new Bird(370.0, BirdGame3.BirdType.PIGEON, 1, game);
        grinch.y = BirdGame3.GROUND_Y - grinch.bodyHeight();
        target.y = BirdGame3.GROUND_Y - target.bodyHeight();
        game.players[0] = grinch;
        game.players[1] = target;

        double healthBefore = target.health;
        GrinchhawkSpecials.neutral(grinch, false);

        assertEquals(Bird.GRINCH_HEART_SNATCH_FRAMES, grinch.grinchHeartSnatchTimer);
        assertFalse(GrinchhawkSpecials.heartSnatchActive(grinch));
        assertEquals(healthBefore, target.health, 0.0001,
                "Heart Snatch must show its claw before the hitbox becomes active.");

        for (int frame = 1; frame < Bird.GRINCH_HEART_SNATCH_STARTUP_FRAMES; frame++) {
            GrinchhawkSpecials.handleState(grinch, false, 1.0, false, false);
            assertEquals(healthBefore, target.health, 0.0001,
                    "Heart Snatch must remain harmless throughout startup.");
        }

        GrinchhawkSpecials.handleState(grinch, false, 1.0, false, false);
        assertTrue(GrinchhawkSpecials.heartSnatchActive(grinch));
        assertTrue(target.health < healthBefore,
                "Heart Snatch should connect when its readable active window begins.");
    }

    @Test
    void grinchhawkHeartSnatchPullsTargetsIntoTheSleighRoute() {
        BirdGame3 game = new BirdGame3();
        game.activePlayers = 2;

        Bird grinch = new Bird(300.0, BirdGame3.BirdType.GRINCHHAWK, 0, game);
        Bird target = new Bird(390.0, BirdGame3.BirdType.PIGEON, 1, game);
        grinch.y = BirdGame3.GROUND_Y - grinch.bodyHeight();
        target.y = BirdGame3.GROUND_Y - target.bodyHeight();
        game.players[0] = grinch;
        game.players[1] = target;

        GrinchhawkSpecials.neutral(grinch, false);
        for (int frame = 0; frame < Bird.GRINCH_HEART_SNATCH_STARTUP_FRAMES; frame++) {
            GrinchhawkSpecials.handleState(grinch, false, 1.0, false, false);
        }

        assertTrue(grinch.grinchHeartSnatchHit[target.playerIndex]);
        assertTrue(target.vx < 0.0,
                "A target in front of right-facing Grinch-Hawk should be pulled inward, not launched away.");
        assertTrue(game.hitstopFrames >= 4,
                "The confirmed catch should have enough impact pause to read clearly.");
    }

    @Test
    void grinchhawkHeartSnatchActiveWindowDoesNotLinger() {
        BirdGame3 game = new BirdGame3();
        game.activePlayers = 2;

        Bird grinch = new Bird(300.0, BirdGame3.BirdType.GRINCHHAWK, 0, game);
        Bird target = new Bird(700.0, BirdGame3.BirdType.PIGEON, 1, game);
        grinch.y = BirdGame3.GROUND_Y - grinch.bodyHeight();
        target.y = BirdGame3.GROUND_Y - target.bodyHeight();
        game.players[0] = grinch;
        game.players[1] = target;

        GrinchhawkSpecials.neutral(grinch, false);
        for (int frame = 0; frame < Bird.GRINCH_HEART_SNATCH_STARTUP_FRAMES
                + Bird.GRINCH_HEART_SNATCH_ACTIVE_FRAMES; frame++) {
            GrinchhawkSpecials.handleState(grinch, false, 1.0, false, false);
        }

        assertFalse(GrinchhawkSpecials.heartSnatchActive(grinch));
        double healthBefore = target.health;
        target.x = 370.0;
        GrinchhawkSpecials.handleState(grinch, false, 1.0, false, false);

        assertEquals(healthBefore, target.health, 0.0001,
                "Heart Snatch must not become a lingering proximity hitbox after its catch window closes.");
    }

    @Test
    void grinchhawkForcedSleighEjectionFallsFromItsCurrentPosition() {
        BirdGame3 game = new BirdGame3();
        game.activePlayers = 1;
        Bird grinch = new Bird(420.0, BirdGame3.BirdType.GRINCHHAWK, 0, game);
        grinch.y = BirdGame3.GROUND_Y - 280.0;
        game.players[0] = grinch;
        grinch.grinchSleighActive = true;
        grinch.grinchSleighRiding = true;
        grinch.grinchSleighTimer = 60;
        grinch.grinchSleighDirection = 1;
        grinch.vx = Bird.GRINCH_SLEIGH_SPEED;
        grinch.vy = -0.6;
        grinch.stunTime = 6.0;
        double xBefore = grinch.x;
        double yBefore = grinch.y;

        GrinchhawkSpecials.handleState(grinch, false, 1.0, false, true);

        assertFalse(grinch.grinchSleighRiding, "Forced ejection must detach Grinch-Hawk from the sleigh.");
        assertEquals(xBefore, grinch.x, 0.0001, "Ejection must not teleport Grinch-Hawk horizontally.");
        assertEquals(yBefore, grinch.y, 0.0001, "Ejection must not teleport Grinch-Hawk vertically.");
        assertTrue(grinch.vy > 0.0, "A non-jump ejection must begin with downward velocity.");

        grinch.update(1.0);
        assertTrue(grinch.y > yBefore, "Gravity must carry the ejected Grinch-Hawk downward on the next tick.");
    }

    @Test
    void grinchhawkEjectedSleighFallsInsteadOfTeleportingToTheGround() {
        BirdGame3 game = new BirdGame3();
        game.activePlayers = 1;
        Bird grinch = new Bird(420.0, BirdGame3.BirdType.GRINCHHAWK, 0, game);
        grinch.y = BirdGame3.GROUND_Y - 300.0;
        game.players[0] = grinch;
        grinch.grinchSleighActive = true;
        grinch.grinchSleighRiding = true;
        grinch.grinchSleighTimer = Bird.GRINCH_SLEIGH_LIFE_FRAMES;
        grinch.grinchSleighDirection = 1;
        grinch.grinchSleighX = grinch.bodyCenterX();
        grinch.grinchSleighY = grinch.bodyBottomY() + 8.0 * grinch.sizeMultiplier;

        GrinchhawkSpecials.dismountSleigh(grinch, true);
        double ejectionY = grinch.grinchSleighY;
        double ejectionX = grinch.grinchSleighX;
        double groundY = GrinchhawkSpecials.sleighSurfaceY(grinch, grinch.grinchSleighX);
        GrinchhawkSpecials.handleSleigh(grinch, false, 1.0, false, false);

        assertTrue(grinch.grinchSleighActive,
                "The abandoned sleigh should remain visible while it begins falling.");
        assertTrue(grinch.grinchSleighY > ejectionY,
                "The abandoned sleigh should move downward after ejection.");
        assertTrue(grinch.grinchSleighY < groundY,
                "The abandoned sleigh must not teleport directly to the ground surface.");
        assertEquals(GrinchhawkSpecials.EJECTED_SLEIGH_FALL_SPEED,
                grinch.grinchSleighY - ejectionY, 0.0001,
                "The ejected sleigh should fall at its normal controlled speed.");
        assertTrue(grinch.grinchSleighX > ejectionX,
                "The normal fall must not discard the sleigh's forward momentum.");

        int remainingLifetime = GrinchhawkSpecials.EJECTED_SLEIGH_COAST_FRAMES
                + GrinchhawkSpecials.EJECTED_SLEIGH_HOLD_FRAMES
                + GrinchhawkSpecials.EJECTED_SLEIGH_REST_FRAMES;
        for (int frame = 0; frame <= remainingLifetime && grinch.grinchSleighActive; frame++) {
            GrinchhawkSpecials.handleSleigh(grinch, false, 1.0, false, false);
        }
        assertFalse(grinch.grinchSleighActive,
                "The abandoned sleigh should eventually expire after its fall and coast.");
    }

    @Test
    void grinchhawkEjectedSleighSlidesAcrossPlatformsAndCoastsToAStop() {
        BirdGame3 game = new BirdGame3();
        game.activePlayers = 1;
        Platform platform = new Platform(0.0, BirdGame3.GROUND_Y - 250.0,
                BirdGame3.WORLD_WIDTH, 40.0);
        game.platforms.add(platform);
        Bird grinch = new Bird(420.0, BirdGame3.BirdType.GRINCHHAWK, 0, game);
        grinch.y = platform.y - grinch.bodyHeight();
        game.players[0] = grinch;
        grinch.grinchSleighActive = true;
        grinch.grinchSleighRiding = true;
        grinch.grinchSleighTimer = Bird.GRINCH_SLEIGH_LIFE_FRAMES;
        grinch.grinchSleighDirection = 1;
        grinch.grinchSleighX = grinch.bodyCenterX();
        grinch.grinchSleighY = grinch.bodyBottomY() + 8.0 * grinch.sizeMultiplier;

        GrinchhawkSpecials.dismountSleigh(grinch, true);
        double platformSleighY = platform.y + 8.0 * grinch.sizeMultiplier;
        double startX = grinch.grinchSleighX;
        double firstStep = 0.0;
        double heldStep = 0.0;
        double earlyDecayStep = 0.0;
        double lateStep = 0.0;
        int earlyDecayProbeFrame = GrinchhawkSpecials.EJECTED_SLEIGH_HOLD_FRAMES + 30;
        int lateProbeFrame = GrinchhawkSpecials.EJECTED_SLEIGH_HOLD_FRAMES
                + GrinchhawkSpecials.EJECTED_SLEIGH_COAST_FRAMES * 3 / 4;
        int motionFrames = GrinchhawkSpecials.EJECTED_SLEIGH_HOLD_FRAMES
                + GrinchhawkSpecials.EJECTED_SLEIGH_COAST_FRAMES;
        for (int frame = 0; frame < motionFrames; frame++) {
            double previousX = grinch.grinchSleighX;
            GrinchhawkSpecials.handleSleigh(grinch, false, 1.0, false, false);
            double step = grinch.grinchSleighX - previousX;
            if (frame == 0) {
                firstStep = step;
            } else if (frame == GrinchhawkSpecials.EJECTED_SLEIGH_HOLD_FRAMES - 1) {
                heldStep = step;
            } else if (frame == earlyDecayProbeFrame) {
                earlyDecayStep = step;
            } else if (frame == lateProbeFrame) {
                lateStep = step;
            }
            assertTrue(grinch.grinchSleighActive,
                    "Landing on a platform must not break the ejected sleigh.");
            assertEquals(platformSleighY, grinch.grinchSleighY, 0.0001,
                    "The ejected sleigh should slide straight across its supporting platform.");
        }

        assertTrue(grinch.grinchSleighX > startX,
                "The ejected sleigh should retain some forward momentum on landing.");
        assertEquals(Bird.GRINCH_SLEIGH_SPEED, firstStep, 0.0001,
                "Ejection should preserve the sleigh's full riding speed.");
        assertEquals(firstStep, heldStep, 0.0001,
                "The sleigh should hold its inherited speed briefly before friction begins.");
        assertTrue(earlyDecayStep < firstStep && earlyDecayStep > firstStep * 0.9,
                "Early friction should make the sled only slightly slower, not abruptly brake it.");
        assertTrue(lateStep > 0.0 && lateStep < firstStep * 0.4,
                "After the hold, the ejected sleigh should lose forward speed gradually.");
        double stoppedX = grinch.grinchSleighX;
        for (int frame = 0; frame < 12; frame++) {
            GrinchhawkSpecials.handleSleigh(grinch, false, 1.0, false, false);
        }
        assertEquals(stoppedX, grinch.grinchSleighX, 0.0001,
                "Platform friction should reduce the sleigh's speed all the way to zero.");
        assertTrue(grinch.grinchSleighActive,
                "The stopped sleigh should remain briefly instead of breaking on contact.");
    }

    @Test
    void grinchhawkFallsWhenAnOccupiedSleighCrashes() {
        BirdGame3 game = new BirdGame3();
        Bird grinch = new Bird(420.0, BirdGame3.BirdType.GRINCHHAWK, 0, game);
        grinch.y = BirdGame3.GROUND_Y - 280.0;
        grinch.grinchSleighActive = true;
        grinch.grinchSleighRiding = true;
        grinch.grinchSleighTimer = 1;
        grinch.grinchSleighDirection = 1;
        grinch.grinchSleighX = grinch.bodyCenterX();
        grinch.grinchSleighY = grinch.bodyBottomY() + 8.0 * grinch.sizeMultiplier;
        grinch.vy = -0.6;
        double xBefore = grinch.x;
        double yBefore = grinch.y;

        GrinchhawkSpecials.crashSleigh(grinch);

        assertFalse(grinch.grinchSleighActive);
        assertFalse(grinch.grinchSleighRiding);
        assertEquals(xBefore, grinch.x, 0.0001);
        assertEquals(yBefore, grinch.y, 0.0001);
        assertTrue(grinch.vy > 0.0, "A crashing sleigh must drop its rider instead of suspending him.");
    }

    @Test
    void grinchhawkMidnightGiftstormDropsPresentsAndFinalSleigh() throws Exception {
        BirdGame3 game = new BirdGame3();
        game.activePlayers = 2;

        Bird grinch = new Bird(300.0, BirdGame3.BirdType.GRINCHHAWK, 0, game);
        Bird target = new Bird(380.0, BirdGame3.BirdType.PIGEON, 1, game);
        grinch.y = BirdGame3.GROUND_Y - 80.0;
        target.y = BirdGame3.GROUND_Y - 80.0;
        game.players[0] = grinch;
        game.players[1] = target;

        setPrivateDouble(grinch, "ultimateMeter", 100.0);
        double healthBefore = target.health;
        invokePrivateVoid(grinch, "special");

        for (int i = 0; i < Bird.GRINCH_MIDNIGHT_GIFTSTORM_DROP_START_FRAME + 3; i++) {
            grinch.update(1.0);
        }

        assertTrue(grinch.grinchGiftstormDropIndex >= 1,
                "Midnight Giftstorm should start raining presents on schedule.");
        assertTrue(target.health < healthBefore,
                "The first Giftstorm present should threaten the nearest target.");
        double healthAfterDrop = target.health;

        for (int i = 0; i < Bird.GRINCH_MIDNIGHT_GIFTSTORM_FINAL_FRAME + 8; i++) {
            grinch.update(1.0);
        }

        assertTrue(grinch.grinchGiftstormFinalResolved,
                "Midnight Giftstorm should resolve one final sleigh dive.");
        assertTrue(target.health < healthAfterDrop,
                "The final sleigh dive should damage caught targets.");
        assertTrue(Math.abs(target.vx) > 10.0 || target.vy < -8.0,
                "The final sleigh dive should launch caught targets.");
    }

    @Test
    void shoebillUltimateStartsFinalStillness() throws Exception {
        BirdGame3 game = new BirdGame3();
        game.activePlayers = 2;

        Bird shoebill = new Bird(300.0, BirdGame3.BirdType.SHOEBILL, 0, game);
        Bird target = new Bird(620.0, BirdGame3.BirdType.PIGEON, 1, game);
        shoebill.y = BirdGame3.GROUND_Y - 80.0;
        target.y = BirdGame3.GROUND_Y - 80.0;
        game.players[0] = shoebill;
        game.players[1] = target;

        setPrivateDouble(shoebill, "ultimateMeter", 100.0);
        invokePrivateVoid(shoebill, "special");

        assertEquals(0.0, getPrivateDouble(shoebill, "ultimateMeter"), 0.0001);
        assertTrue(getPrivateInt(shoebill, "shoebillFinalStillnessTimer") > 0);
        assertEquals(1, getPrivateInt(shoebill, "shoebillFinalStillnessTargetIndex"));
        assertTrue(shoebill.isCombatInvulnerable());
        assertEquals(0, shoebill.specialCooldown);
        assertEquals(ShoebillSpecials.FINAL_STILLNESS_MOVE, game.lastTelemetryMoveName(0, ""));
    }

    @Test
    void shoebillFinalStillnessBeamDamagesLockedTarget() throws Exception {
        BirdGame3 game = new BirdGame3();
        game.activePlayers = 2;

        Bird shoebill = new Bird(300.0, BirdGame3.BirdType.SHOEBILL, 0, game);
        Bird target = new Bird(620.0, BirdGame3.BirdType.PIGEON, 1, game);
        shoebill.y = BirdGame3.GROUND_Y - 80.0;
        target.y = BirdGame3.GROUND_Y - 80.0;
        game.players[0] = shoebill;
        game.players[1] = target;

        setPrivateDouble(shoebill, "ultimateMeter", 100.0);
        invokePrivateVoid(shoebill, "special");
        double healthBefore = target.health;

        int damageFrame = Bird.SHOEBILL_FINAL_STILLNESS_BEAM_START_FRAME + 8;
        for (int i = 0; i <= damageFrame; i++) {
            shoebill.update(1.0);
        }

        assertTrue(target.health < healthBefore, "Final Stillness should damage the locked target.");
        assertTrue(getPrivateBoolean(shoebill, "shoebillFinalStillnessBeamResolved"));
        assertTrue(game.damageDealt[0] > 0);
    }

    @Test
    void shoebillDeathStareTelegraphsAndCannotLoopStun() {
        BirdGame3 game = new BirdGame3();
        game.activePlayers = 3;
        Bird shoebill = new Bird(120.0, BirdGame3.BirdType.SHOEBILL, 0, game);
        Bird caught = new Bird(220.0, BirdGame3.BirdType.PIGEON, 1, game);
        Bird escaped = new Bird(300.0, BirdGame3.BirdType.EAGLE, 2, game);
        shoebill.y = BirdGame3.GROUND_Y - shoebill.bodyHeight();
        caught.y = BirdGame3.GROUND_Y - caught.bodyHeight();
        escaped.y = BirdGame3.GROUND_Y - escaped.bodyHeight();
        shoebill.facingRight = true;
        caught.facingRight = true;
        escaped.facingRight = true;
        game.players[0] = shoebill;
        game.players[1] = caught;
        game.players[2] = escaped;

        ShoebillSpecials.neutral(shoebill, false);

        assertEquals(0.0, caught.stunTime, 0.0001,
                "Death Stare should show its tell before applying the stun.");
        assertFalse(ShoebillSpecials.ready(shoebill, Bird.ShoebillSpecialVariant.NEUTRAL));

        int turnFrame = Bird.SHOEBILL_STARE_WINDUP_FRAMES / 2;
        for (int frame = 0; frame < Bird.SHOEBILL_STARE_WINDUP_FRAMES - 1; frame++) {
            if (frame == turnFrame) {
                escaped.facingRight = false;
            }
            shoebill.update(1.0);
        }
        assertEquals(0.0, caught.stunTime, 0.0001);
        assertEquals(0.0, escaped.stunTime, 0.0001);

        shoebill.update(1.0);

        assertEquals(Bird.SHOEBILL_STARE_STUN_FRAMES, caught.stunTime, 0.0001,
                "A target that leaves its back exposed through the tell should still be caught.");
        assertEquals(0.0, escaped.stunTime, 0.0001,
                "Turning to face Shoebill during the tell should avoid Death Stare.");
        assertTrue(shoebill.shoebillStareReuseTimer > Bird.SHOEBILL_STARE_STUN_FRAMES,
                "Death Stare's remaining reuse time should outlast its stun and prevent a loop.");
        assertFalse(ShoebillSpecials.ready(shoebill, Bird.ShoebillSpecialVariant.NEUTRAL));
    }

    @Test
    void razorbillUltimateStartsGuillotineWakeInsteadOfBoostedSpecial() throws Exception {
        BirdGame3 game = new BirdGame3();
        game.activePlayers = 2;

        Bird razorbill = new Bird(300.0, BirdGame3.BirdType.RAZORBILL, 0, game);
        Bird target = new Bird(620.0, BirdGame3.BirdType.PIGEON, 1, game);
        razorbill.y = BirdGame3.GROUND_Y - 80.0;
        target.y = BirdGame3.GROUND_Y - 80.0;
        game.players[0] = razorbill;
        game.players[1] = target;

        setPrivateDouble(razorbill, "ultimateMeter", 100.0);
        invokePrivateVoid(razorbill, "special");

        assertEquals(0.0, getPrivateDouble(razorbill, "ultimateMeter"), 0.0001);
        assertEquals(Bird.RAZORBILL_GUILLOTINE_TOTAL_FRAMES, razorbill.razorbillGuillotineTimer);
        assertEquals(0, razorbill.razorbillGuillotineSlashIndex);
        assertTrue(razorbill.isCombatInvulnerable());
        assertEquals(0, razorbill.razorbillStormTimer);
        assertEquals(0, razorbill.bladeStormFrames);
        assertEquals(0, razorbill.razorbillShearTimer);
        assertEquals(0, razorbill.razorbillCounterTimer);
        assertEquals(RazorbillSpecials.GUILLOTINE_WAKE_MOVE, game.lastTelemetryMoveName(0, ""));
    }

    @Test
    void razorbillSpecialHitsUseTheCorrectedLaunchScale() {
        BirdGame3 game = new BirdGame3();
        game.activePlayers = 2;

        Bird razorbill = new Bird(220.0, BirdGame3.BirdType.RAZORBILL, 0, game);
        Bird target = new Bird(300.0, BirdGame3.BirdType.PIGEON, 1, game);
        razorbill.y = BirdGame3.GROUND_Y - 80.0;
        target.y = BirdGame3.GROUND_Y - 80.0;
        game.players[0] = razorbill;
        game.players[1] = target;

        int dealt = RazorbillSpecials.applySpecialHit(razorbill, target, 6, 8.0, -4.0, false);

        assertTrue(dealt > 0);
        assertEquals(1.25, RazorbillSpecials.SPECIAL_KNOCKBACK_MULTIPLIER, 0.0001);
        assertEquals(8.0 * RazorbillSpecials.SPECIAL_KNOCKBACK_MULTIPLIER, target.vx, 0.0001);
        assertEquals(-4.0 * RazorbillSpecials.SPECIAL_KNOCKBACK_MULTIPLIER, target.vy, 0.0001);
    }

    @Test
    void razorbillSkimmingRazorTelegraphsBeforeTheDashBecomesActive() {
        BirdGame3 game = new BirdGame3();
        game.activePlayers = 2;

        Bird razorbill = new Bird(220.0, BirdGame3.BirdType.RAZORBILL, 0, game);
        Bird target = new Bird(270.0, BirdGame3.BirdType.PIGEON, 1, game);
        razorbill.y = BirdGame3.GROUND_Y - razorbill.bodyHeight();
        target.y = BirdGame3.GROUND_Y - target.bodyHeight();
        game.players[0] = razorbill;
        game.players[1] = target;

        double targetHealth = target.health;
        RazorbillSpecials.side(razorbill, false);

        assertEquals(Bird.RAZORBILL_DASH_FRAMES + Bird.RAZORBILL_DASH_STARTUP_FRAMES,
                razorbill.bladeStormFrames);
        assertTrue(Math.abs(razorbill.vx) < Math.abs(razorbill.razorbillDashVX) * 0.5,
                "Skimming Razor should flash its line before committing to full dash speed.");
        RazorbillSpecials.handleBladeStorm(razorbill);
        assertEquals(targetHealth, target.health, 0.0001,
                "The startup tell must not already contain the active hitbox.");

        razorbill.bladeStormFrames = Bird.RAZORBILL_DASH_FRAMES;
        RazorbillSpecials.handleBladeStorm(razorbill);
        assertTrue(target.health < targetHealth,
                "Skimming Razor should become active immediately after its startup tell.");
    }

    @Test
    void razorbillSkimmingRazorSlicesThroughMultipleEnemiesWithoutBraking() {
        BirdGame3 game = new BirdGame3();
        game.activePlayers = 3;

        Bird razorbill = new Bird(220.0, BirdGame3.BirdType.RAZORBILL, 0, game);
        Bird firstTarget = new Bird(270.0, BirdGame3.BirdType.PIGEON, 1, game);
        Bird secondTarget = new Bird(310.0, BirdGame3.BirdType.EAGLE, 2, game);
        razorbill.y = BirdGame3.GROUND_Y - razorbill.bodyHeight();
        firstTarget.y = BirdGame3.GROUND_Y - firstTarget.bodyHeight();
        secondTarget.y = BirdGame3.GROUND_Y - secondTarget.bodyHeight();
        game.players[0] = razorbill;
        game.players[1] = firstTarget;
        game.players[2] = secondTarget;

        RazorbillSpecials.side(razorbill, false);
        double committedVX = razorbill.razorbillDashVX;
        double committedVY = razorbill.razorbillDashVY;
        double firstHealth = firstTarget.health;
        double secondHealth = secondTarget.health;
        razorbill.bladeStormFrames = Bird.RAZORBILL_DASH_FRAMES;
        RazorbillSpecials.handleBladeStorm(razorbill);

        assertTrue(firstTarget.health < firstHealth);
        assertTrue(secondTarget.health < secondHealth);
        assertTrue(razorbill.razorbillDashHit[firstTarget.playerIndex]);
        assertTrue(razorbill.razorbillDashHit[secondTarget.playerIndex]);
        assertEquals(Bird.RAZORBILL_DASH_FRAMES, razorbill.bladeStormFrames,
                "Enemy contact must not shorten the piercing dash.");
        assertEquals(committedVX, razorbill.razorbillDashVX, 0.0001);
        assertEquals(committedVY, razorbill.razorbillDashVY, 0.0001);
        assertEquals(committedVX, razorbill.vx, 0.0001,
                "Razorbill should retain full horizontal speed while cutting through enemies.");
        assertEquals(committedVY, razorbill.vy, 0.0001,
                "Razorbill should retain the authored diagonal while cutting through enemies.");
    }

    @Test
    void razorbillSkimmingRazorIsFastShallowDiagonalAndHeadLeadsPath() throws Exception {
        BirdGame3 game = new BirdGame3();
        game.activePlayers = 1;

        Bird right = new Bird(220.0, BirdGame3.BirdType.RAZORBILL, 0, game);
        right.y = BirdGame3.GROUND_Y - right.bodyHeight();
        right.facingRight = true;
        game.players[0] = right;

        RazorbillSpecials.side(right, false);

        assertTrue(right.razorbillDashVX >= 30.0,
                "Skimming Razor should commit at its new high horizontal speed.");
        assertTrue(right.razorbillDashVY < 0.0);
        assertTrue(Math.abs(right.razorbillDashVX) > Math.abs(right.razorbillDashVY) * 3.5,
                "The diagonal should be much more horizontal than vertical.");
        Object rightPose = invokePrivateObjectMethod(right, "currentRazorbillSpecialPose");
        assertEquals(Math.atan2(right.razorbillDashVY, right.razorbillDashVX),
                invokeDoubleMethod(rightPose, "aimAngleRadians"), 0.0001,
                "Razorbill's head and bill should point along the dash vector.");

        Bird left = new Bird(420.0, BirdGame3.BirdType.RAZORBILL, 0, game);
        left.y = BirdGame3.GROUND_Y - left.bodyHeight();
        left.facingRight = false;
        game.players[0] = left;

        RazorbillSpecials.side(left, false);

        assertTrue(left.razorbillDashVX <= -30.0);
        Object leftPose = invokePrivateObjectMethod(left, "currentRazorbillSpecialPose");
        assertEquals(Math.atan2(left.razorbillDashVY, left.razorbillDashVX),
                invokeDoubleMethod(leftPose, "aimAngleRadians"), 0.0001);
        assertTrue(Math.cos(invokeDoubleMethod(leftPose, "aimAngleRadians")) < 0.0,
                "The head pose should reverse cleanly when the dash travels left.");
    }

    @Test
    void razorbillSkimmingRazorCanOnlyBeUsedOncePerAirtimeAndRefreshesOnLanding() {
        BirdGame3 game = new BirdGame3();
        game.activePlayers = 1;

        Bird razorbill = new Bird(220.0, BirdGame3.BirdType.RAZORBILL, 0, game);
        razorbill.y = BirdGame3.GROUND_Y - razorbill.bodyHeight();
        game.players[0] = razorbill;

        RazorbillSpecials.side(razorbill, false);

        assertTrue(razorbill.razorbillSideSpecialUsed);
        assertFalse(RazorbillSpecials.ready(razorbill, Bird.RazorbillSpecialVariant.SIDE));
        assertTrue(RazorbillSpecials.ready(razorbill, Bird.RazorbillSpecialVariant.UP),
                "Spending Skimming Razor must not also consume Razorbill's recovery move.");

        razorbill.update(1.0);
        assertTrue(razorbill.razorbillSideSpecialUsed,
                "Ground contact during the dash startup must not refresh Skimming Razor.");

        razorbill.y = BirdGame3.GROUND_Y - 260.0;
        razorbill.stunTime = 4.0;
        razorbill.update(1.0);
        razorbill.razorbillSideReuseTimer = 0;
        assertTrue(razorbill.razorbillSideSpecialUsed,
                "Being hit out of Skimming Razor must not restore another airborne use.");
        assertFalse(RazorbillSpecials.ready(razorbill, Bird.RazorbillSpecialVariant.SIDE));

        razorbill.stunTime = 0.0;
        razorbill.y = BirdGame3.GROUND_Y - razorbill.bodyHeight();
        razorbill.vx = 0.0;
        razorbill.vy = 0.0;
        razorbill.update(1.0);

        assertFalse(razorbill.razorbillSideSpecialUsed);
        assertTrue(RazorbillSpecials.ready(razorbill, Bird.RazorbillSpecialVariant.SIDE),
                "Landing should restore exactly one Skimming Razor use.");
    }

    @Test
    void razorbillGuillotineWakeDamagesAndLeavesLingeringRazorWake() throws Exception {
        BirdGame3 game = new BirdGame3();
        game.activePlayers = 2;

        Bird razorbill = new Bird(300.0, BirdGame3.BirdType.RAZORBILL, 0, game);
        Bird target = new Bird(620.0, BirdGame3.BirdType.PIGEON, 1, game);
        razorbill.y = BirdGame3.GROUND_Y - 80.0;
        target.y = BirdGame3.GROUND_Y - 80.0;
        game.players[0] = razorbill;
        game.players[1] = target;

        setPrivateDouble(razorbill, "ultimateMeter", 100.0);
        invokePrivateVoid(razorbill, "special");
        double healthBefore = target.health;

        for (int i = 0; i <= Bird.RAZORBILL_GUILLOTINE_FINAL_FRAME + 2; i++) {
            razorbill.update(1.0);
        }

        assertTrue(target.health < healthBefore, "Guillotine Wake should damage targets that stay inside the marked cuts.");
        assertTrue(razorbill.razorbillGuillotineWakeTimer > 0, "The final slash should leave a lingering razor wake.");
        assertTrue(game.damageDealt[0] > 0);

        for (int i = 0; i < Bird.RAZORBILL_GUILLOTINE_TOTAL_FRAMES; i++) {
            razorbill.update(1.0);
        }

        assertEquals(0, razorbill.razorbillGuillotineTimer);
        assertTrue(razorbill.razorbillGuillotineWakeTimer > 0,
                "The floor wake should outlast Razorbill's untargetable slash chain.");
        assertFalse(razorbill.isCombatInvulnerable());
    }

    @Test
    void penguinSnowFortGuardsAndTurnsIcebergIntoSnowball() throws Exception {
        BirdGame3 game = new BirdGame3();
        game.activePlayers = 2;

        Bird penguin = new Bird(120.0, BirdGame3.BirdType.PENGUIN, 0, game);
        Bird attacker = new Bird(285.0, BirdGame3.BirdType.EAGLE, 1, game);
        penguin.y = BirdGame3.GROUND_Y - 80.0;
        attacker.y = BirdGame3.GROUND_Y - 80.0;
        penguin.facingRight = true;
        game.players[0] = penguin;
        game.players[1] = attacker;

        KeyCode blockKey = game.blockKeyForPlayer(0);
        KeyCode specialKey = game.specialKeyForPlayer(0);
        game.setLocalActionsForKey(blockKey, true);
        game.setLocalActionsForKey(specialKey, true);
        penguin.update(1.0);

        assertNotNull(getPrivateObject(penguin, "penguinSnowFort"));
        double dealt = applyPrivateDamage(attacker, penguin, 20.0);
        assertTrue(dealt < 20.0);
        assertTrue(penguin.health > Bird.STARTING_HEALTH - 20.0);

        game.setLocalActionsForKey(blockKey, false);
        game.setLocalActionsForKey(specialKey, false);
        penguin.update(1.0);

        KeyCode rightKey = game.rightKeyForPlayer(0);
        game.setLocalActionsForKey(rightKey, true);
        game.setLocalActionsForKey(specialKey, true);
        penguin.update(1.0);

        Object iceObjects = getPrivateObject(penguin, "penguinIceObjects");
        assertTrue(iceObjects instanceof List<?> list && !list.isEmpty());
        Object firstObject = ((List<?>) iceObjects).getFirst();
        assertTrue(getPrivateBoolean(firstObject, "snowball"));
        assertEquals(0, penguin.specialCooldown);
    }

    @Test
    void penguinAirDownSpecialDropsIcebergStraightDown() throws Exception {
        BirdGame3 game = new BirdGame3();
        game.activePlayers = 1;

        Bird penguin = new Bird(320.0, BirdGame3.BirdType.PENGUIN, 0, game);
        penguin.y = BirdGame3.GROUND_Y - 260.0;
        game.players[0] = penguin;

        double startCenterX = penguin.bodyCenterX();
        game.setLocalActionsForKey(game.blockKeyForPlayer(0), true);
        game.setLocalActionsForKey(game.specialKeyForPlayer(0), true);
        penguin.update(1.0);
        game.setLocalActionsForKey(game.blockKeyForPlayer(0), false);
        game.setLocalActionsForKey(game.specialKeyForPlayer(0), false);

        Object iceObjects = getPrivateObject(penguin, "penguinIceObjects");
        assertTrue(iceObjects instanceof List<?> list && list.size() == 1);
        Object dropped = ((List<?>) iceObjects).getFirst();
        assertTrue(getPrivateBoolean(dropped, "verticalDrop"));
        assertFalse(getPrivateBoolean(dropped, "snowball"));
        assertEquals(0.0, getPrivateDouble(dropped, "vx"), 0.0001);
        assertTrue(getPrivateDouble(dropped, "vy") > 0.0);
        assertEquals(startCenterX, getPrivateDouble(dropped, "x"), 0.0001);
        assertNull(getPrivateObject(penguin, "penguinSnowFort"));
        assertTrue(getPrivateInt(penguin, "penguinSnowFortReuseTimer") > 0);
    }

    @Test
    void penguinAirDownSpecialIcebergDamagesTargetBelow() throws Exception {
        BirdGame3 game = new BirdGame3();
        game.activePlayers = 2;

        Bird penguin = new Bird(320.0, BirdGame3.BirdType.PENGUIN, 0, game);
        Bird target = new Bird(320.0, BirdGame3.BirdType.PIGEON, 1, game);
        penguin.y = BirdGame3.GROUND_Y - 280.0;
        target.x = penguin.bodyCenterX() - target.bodyWidth() * 0.5;
        target.y = BirdGame3.GROUND_Y - target.bodyHeight();
        game.players[0] = penguin;
        game.players[1] = target;

        game.setLocalActionsForKey(game.blockKeyForPlayer(0), true);
        game.setLocalActionsForKey(game.specialKeyForPlayer(0), true);
        penguin.update(1.0);
        game.setLocalActionsForKey(game.blockKeyForPlayer(0), false);
        game.setLocalActionsForKey(game.specialKeyForPlayer(0), false);
        double healthBefore = target.health;

        for (int i = 0; i < 50 && target.health >= healthBefore; i++) {
            penguin.update(1.0);
        }

        assertTrue(target.health < healthBefore, "Air down special's falling iceberg should threaten targets below Penguin.");
    }

    @Test
    void penguinIceObjectFallsWhenUnsupportedPastPlatformEdge() {
        BirdGame3 game = new BirdGame3();
        game.activePlayers = 1;
        game.selectedMap = BirdGame3.MapType.BATTLEFIELD;
        game.platforms.clear();
        Platform island = new Platform(500.0, BirdGame3.GROUND_Y - 280.0, 260.0, 60.0);
        game.platforms.add(island);

        Bird penguin = new Bird(island.x + 80.0, BirdGame3.BirdType.PENGUIN, 0, game);
        penguin.y = island.y - 80.0;
        game.players[0] = penguin;

        Bird.PenguinIceObject object = new Bird.PenguinIceObject(
                island.x + island.w + 80.0,
                island.y - 42.0,
                1.0,
                0.0,
                1,
                false,
                false);
        penguin.penguinIceObjects.add(object);

        for (int i = 0; i < 35; i++) {
            penguin.update(1.0);
        }

        assertFalse(object.shattered);
        assertTrue(object.y > island.y + 60.0,
                "Unsupported Penguin ice objects should fall instead of riding an invisible owner-height floor.");
    }

    @Test
    void penguinSnowFortBlocksMovementAndTakesAttackDamage() throws Exception {
        BirdGame3 game = new BirdGame3();
        game.activePlayers = 2;

        Bird penguin = new Bird(120.0, BirdGame3.BirdType.PENGUIN, 0, game);
        Bird attacker = new Bird(245.0, BirdGame3.BirdType.EAGLE, 1, game);
        penguin.y = BirdGame3.GROUND_Y - 80.0;
        attacker.y = BirdGame3.GROUND_Y - 80.0;
        penguin.facingRight = true;
        attacker.facingRight = false;
        attacker.vx = -8.0;
        game.players[0] = penguin;
        game.players[1] = attacker;

        game.setLocalActionsForKey(game.blockKeyForPlayer(0), true);
        game.setLocalActionsForKey(game.specialKeyForPlayer(0), true);
        penguin.update(1.0);

        Object fort = getPrivateObject(penguin, "penguinSnowFort");
        double fortX = getPrivateDouble(fort, "x");
        attacker.x = fortX - 40.0;
        penguin.update(1.0);

        assertTrue(Math.abs((attacker.x + 40.0) - fortX) > 62.0,
                "Snow Fort should push enemy bodies out instead of letting them walk through.");

        int healthBefore = getPrivateInt(fort, "health");
        invokePrivateIntVoid(attacker);
        advanceAuthoredAttackToFirstActiveFrame(attacker);
        int healthAfter = getPrivateInt(fort, "health");

        assertTrue(healthAfter < healthBefore, "Enemy attacks should damage the Snow Fort.");
    }

    @Test
    void penguinSnowFortDamageUsesBothFightersTuning() {
        double originalEagleDamage = BirdGame3.BirdType.EAGLE.damageDealtMult;
        double originalPenguinDamageTaken = BirdGame3.BirdType.PENGUIN.damageTakenMult;
        try {
            BirdGame3.BirdType.EAGLE.damageDealtMult = 0.5;
            BirdGame3.BirdType.PENGUIN.damageTakenMult = 0.8;
            BirdGame3 game = new BirdGame3();
            Bird penguin = new Bird(120.0, BirdGame3.BirdType.PENGUIN, 0, game);
            Bird eagle = new Bird(300.0, BirdGame3.BirdType.EAGLE, 1, game);

            assertEquals(8, PenguinSpecials.snowFortAttackDamage(penguin, eagle, 20.0),
                    "Deployables should inherit the same outgoing and incoming tuning as their fighters.");
        } finally {
            BirdGame3.BirdType.EAGLE.damageDealtMult = originalEagleDamage;
            BirdGame3.BirdType.PENGUIN.damageTakenMult = originalPenguinDamageTaken;
        }
    }

    @Test
    void penguinSnowFortGuardKeepsUltimateAdvantage() {
        BirdGame3 game = new BirdGame3();
        game.activePlayers = 2;
        Bird penguin = new Bird(120.0, BirdGame3.BirdType.PENGUIN, 0, game);
        Bird attacker = new Bird(300.0, BirdGame3.BirdType.EAGLE, 1, game);
        penguin.y = BirdGame3.GROUND_Y - penguin.bodyHeight();
        attacker.y = BirdGame3.GROUND_Y - attacker.bodyHeight();
        penguin.facingRight = true;
        game.players[0] = penguin;
        game.players[1] = attacker;

        PenguinSpecials.down(penguin, false);
        assertEquals(14.4, PenguinSpecials.adjustDamageForSnowFort(penguin, attacker, 20.0), 0.0001,
                "A correctly positioned normal fort should intercept 28% of incoming damage.");
        assertEquals(Bird.PENGUIN_SNOW_FORT_HEALTH - 8, penguin.penguinSnowFort.health);

        PenguinSpecials.down(penguin, true);
        assertEquals(13.6, PenguinSpecials.adjustDamageForSnowFort(penguin, attacker, 20.0), 0.0001,
                "The ultimate fort should retain its stronger 32% guard.");
        assertEquals(Bird.PENGUIN_SNOW_FORT_HEALTH + 34 - 9, penguin.penguinSnowFort.health);
    }

    @Test
    void penguinCannotRefreshDamagedSnowFortButKeepsAirDrop() {
        BirdGame3 game = new BirdGame3();
        game.activePlayers = 1;
        Bird penguin = new Bird(120.0, BirdGame3.BirdType.PENGUIN, 0, game);
        penguin.y = BirdGame3.GROUND_Y - penguin.bodyHeight();
        penguin.facingRight = true;
        game.players[0] = penguin;

        PenguinSpecials.down(penguin, false);
        Bird.PenguinSnowFort originalFort = penguin.penguinSnowFort;
        originalFort.health = 9;
        penguin.penguinSnowFortReuseTimer = 0;

        assertFalse(PenguinSpecials.ready(penguin, Bird.PenguinSpecialVariant.DOWN),
                "A living fort must keep its damage instead of becoming replaceable at full health.");
        PenguinSpecials.down(penguin, false);
        assertSame(originalFort, penguin.penguinSnowFort);
        assertEquals(9, originalFort.health);

        penguin.y -= 180.0;
        penguin.vy = 1.0;
        assertTrue(PenguinSpecials.ready(penguin, Bird.PenguinSpecialVariant.DOWN),
                "A living fort should not lock Penguin out of the aerial down-special.");
        PenguinSpecials.down(penguin, false);
        assertSame(originalFort, penguin.penguinSnowFort);
        assertEquals(1, penguin.penguinIceObjects.size());
        assertTrue(penguin.penguinIceObjects.getFirst().verticalDrop);
    }

    @Test
    void penguinSnowFortDoesNotExpireByTimerAndClearsOnDeath() throws Exception {
        BirdGame3 game = new BirdGame3();
        game.activePlayers = 1;

        Bird penguin = new Bird(120.0, BirdGame3.BirdType.PENGUIN, 0, game);
        penguin.y = BirdGame3.GROUND_Y - 80.0;
        penguin.facingRight = true;
        game.players[0] = penguin;

        invokePrivateBooleanVoid(penguin, "specialPenguinSnowFort", false);
        Object fort = getPrivateObject(penguin, "penguinSnowFort");
        assertNotNull(fort);
        setPrivateInt(fort, "lifeFrames", 1);

        for (int i = 0; i < 90; i++) {
            penguin.update(1.0);
        }

        assertSame(fort, getPrivateObject(penguin, "penguinSnowFort"),
                "Snow Fort should stay up after its old lifetime would have expired.");

        penguin.health = 0;
        penguin.update(1.0);

        assertNull(getPrivateObject(penguin, "penguinSnowFort"),
                "Snow Fort should disappear when Penguin dies.");
    }

    @Test
    void penguinSnowFortBlocksAlliedBirdsInTeamMode() throws Exception {
        BirdGame3 game = new BirdGame3();
        game.activePlayers = 2;
        game.teamModeEnabled = true;

        Bird penguin = new Bird(120.0, BirdGame3.BirdType.PENGUIN, 0, game);
        Bird ally = new Bird(245.0, BirdGame3.BirdType.EAGLE, 1, game);
        penguin.y = BirdGame3.GROUND_Y - 80.0;
        ally.y = BirdGame3.GROUND_Y - 80.0;
        penguin.facingRight = true;
        game.players[0] = penguin;
        game.players[1] = ally;

        invokePrivateBooleanVoid(penguin, "specialPenguinSnowFort", false);
        Object fort = getPrivateObject(penguin, "penguinSnowFort");
        double fortX = getPrivateDouble(fort, "x");
        ally.x = fortX - 40.0;

        penguin.update(1.0);

        assertTrue(Math.abs((ally.x + 40.0) - fortX) > 90.0,
                "Snow Fort should physically block allied birds too, not only enemies.");
    }

    @Test
    void pigeonAiRefreshesRecoveryBeforeItFallsTooLow() {
        BirdGame3 game = new BirdGame3();
        game.activePlayers = 2;
        game.selectedMap = BirdGame3.MapType.BATTLEFIELD;
        double islandX = 2400.0;
        double islandY = BirdGame3.GROUND_Y - 80.0;
        game.platforms.add(new Platform(islandX, islandY, 1200, 70));

        Bird pigeon = new Bird(islandX - 90, BirdGame3.BirdType.PIGEON, 0, game);
        pigeon.y = islandY + 70;
        pigeon.vx = -2.8;
        pigeon.canDoubleJump = false;
        Bird target = new Bird(islandX + 360, BirdGame3.BirdType.EAGLE, 1, game);
        target.y = islandY - 120;

        game.players[0] = pigeon;
        game.players[1] = target;
        game.isAI[0] = true;

        pigeon.update(1.0);

        assertTrue(game.isRightPressed(0));
        assertTrue(game.isSpecialPressed(0));
    }

    @Test
    void mockingbirdAiUsesForestLiftWhenTrappedBelowResonanceHallStage() throws Exception {
        BirdGame3 game = new BirdGame3();
        game.activePlayers = 2;
        game.selectedMap = BirdGame3.MapType.RESONANCE_HALL;
        double stageX = 760.0;
        double stageY = BirdGame3.GROUND_Y - 220.0;
        double stageW = 4_480.0;
        Platform mainStage = new Platform(stageX, stageY, stageW, 92.0);
        game.platforms.add(mainStage);
        setPrivateDouble(game, "battlefieldIslandX", stageX);
        setPrivateDouble(game, "battlefieldIslandY", stageY);
        setPrivateDouble(game, "battlefieldIslandW", stageW);

        Bird target = new Bird(3_780.0, BirdGame3.BirdType.PIGEON, 0, game);
        target.y = stageY - target.bodyHeight();
        Bird understudy = new Bird(2_120.0, BirdGame3.BirdType.MOCKINGBIRD, 1, game);
        understudy.y = stageY + 170.0;
        understudy.vy = 4.0;
        game.players[0] = target;
        game.players[1] = understudy;
        game.isAI[1] = true;

        understudy.update(1.0);

        assertTrue(game.isJumpPressed(1),
                "An offstage Charles must aim Forest Lift upward instead of looping below the platform.");
        assertTrue(game.isSpecialPressed(1),
                "Resonance Hall must use the same deterministic recovery behavior as every island arena.");
    }

    @Test
    void roadrunnerAiProtectsItsMomentumAtBattlefieldEdges() throws Exception {
        BirdGame3 game = new BirdGame3();
        game.activePlayers = 1;
        game.selectedMap = BirdGame3.MapType.BATTLEFIELD;
        double islandX = 2400.0;
        double islandY = BirdGame3.GROUND_Y - 80.0;
        Platform mainIsland = new Platform(islandX, islandY, 1200, 70);
        game.platforms.add(mainIsland);

        Bird runner = new Bird(islandX - 62.0, BirdGame3.BirdType.ROADRUNNER, 0, game);
        runner.y = islandY - 36.0;
        runner.vx = -9.0;
        game.players[0] = runner;

        Method caution = Bird.class.getDeclaredMethod("isAIVoidRecoveryCaution", boolean.class, Platform.class);
        caution.setAccessible(true);
        assertTrue((boolean) caution.invoke(runner, false, null),
                "Roadrunner should brake and recover as soon as its momentum carries it beyond the island lip.");

        runner.x = islandX + 24.0;
        Method jumpBeforeEdge = Bird.class.getDeclaredMethod("shouldAIJumpBeforeOffstage", double.class);
        jumpBeforeEdge.setAccessible(true);
        assertTrue((boolean) jumpBeforeEdge.invoke(runner, islandX - 220.0),
                "Roadrunner should jump before committing its high ground speed offstage.");
        assertFalse((boolean) jumpBeforeEdge.invoke(runner, islandX + 300.0),
                "Roadrunner should not jump merely for an onstage route.");
    }

    @Test
    void roadrunnerAiCountersteersBeforeOvershootingAClimbRoute() throws Exception {
        BirdGame3 game = new BirdGame3();
        Bird runner = new Bird(500.0, BirdGame3.BirdType.ROADRUNNER, 0, game);
        runner.vx = 24.0;
        game.players[0] = runner;

        Method adjust = Bird.class.getDeclaredMethod("adjustRoadrunnerAINavigationDirection",
                int.class, double.class, boolean.class);
        adjust.setAccessible(true);
        assertEquals(-1, adjust.invoke(runner, 1, 590.0, true),
                "Roadrunner should countersteer when momentum would carry it past a nearby climb route.");
        assertEquals(1, adjust.invoke(runner, 1, 900.0, true),
                "Roadrunner should keep accelerating while the climb route is still far away.");
        assertEquals(1, adjust.invoke(runner, 1, 590.0, false),
                "Ordinary combat movement should retain Roadrunner's player-like momentum.");
    }

    @Test
    void recoveryFromBattlefieldSidePlatformsStillTargetsTheMainIsland() throws Exception {
        BirdGame3 game = new BirdGame3();
        game.activePlayers = 1;
        game.selectedMap = BirdGame3.MapType.BATTLEFIELD;
        double islandX = 2400.0;
        double islandY = BirdGame3.GROUND_Y - 80.0;
        Platform mainIsland = new Platform(islandX, islandY, 1200, 70);
        Platform leftPlatform = new Platform(islandX + 120, islandY - 210, 420, 46);
        game.platforms.add(mainIsland);
        game.platforms.add(leftPlatform);

        Bird penguin = new Bird(leftPlatform.x + 120, BirdGame3.BirdType.PENGUIN, 0, game);
        penguin.x = leftPlatform.x + leftPlatform.w / 2.0 - 40.0;
        penguin.y = leftPlatform.y - 80.0;
        game.players[0] = penguin;
        game.isAI[0] = true;

        Method caution = Bird.class.getDeclaredMethod("isAIVoidRecoveryCaution", boolean.class, Platform.class);
        caution.setAccessible(true);
        Method recoveryGoal = Bird.class.getDeclaredMethod("aiRecoveryGoalX", Platform.class);
        recoveryGoal.setAccessible(true);

        boolean keepRecovering = (boolean) caution.invoke(penguin, true, leftPlatform);
        double goalX = (double) recoveryGoal.invoke(penguin, leftPlatform);

        assertTrue(keepRecovering);
        assertTrue(goalX > leftPlatform.x + leftPlatform.w - 40.0);
    }

    @Test
    void heisenbirdAiDropsTowardReachableTargetFromBattlefieldPlatform() throws Exception {
        BirdGame3 game = new BirdGame3();
        game.activePlayers = 2;
        game.selectedMap = BirdGame3.MapType.BATTLEFIELD;
        double islandX = 2400.0;
        double islandY = BirdGame3.GROUND_Y - 80.0;
        Platform mainIsland = new Platform(islandX, islandY, 1200, 70);
        Platform sidePlatform = new Platform(islandX + 160, islandY - 210, 420, 46);
        game.platforms.add(mainIsland);
        game.platforms.add(sidePlatform);

        Bird heisenbird = new Bird(sidePlatform.x + sidePlatform.w / 2.0 - 40.0, BirdGame3.BirdType.HEISENBIRD, 0, game);
        heisenbird.y = sidePlatform.y - 80.0;
        Bird pigeon = new Bird(heisenbird.x, BirdGame3.BirdType.PIGEON, 1, game);
        pigeon.y = mainIsland.y - 80.0;
        setPrivateDouble(heisenbird, "opiumResourceMeter", 100.0);
        game.players[0] = heisenbird;
        game.players[1] = pigeon;
        game.isAI[0] = true;

        heisenbird.update(1.0);

        assertTrue(getPrivateInt(heisenbird, "aiDropCommitFrames") > 0,
                "A reachable target below should keep the drop plan instead of falling into void recovery.");
        assertFalse(game.isJumpPressed(0),
                "AI should not jump when it is already above the target and needs to drop or attack.");
    }

    @Test
    void aiTargetLockKeepsCurrentTargetWhenScoresAreClose() throws Exception {
        BirdGame3 game = new BirdGame3();
        game.activePlayers = 3;

        Bird ai = new Bird(100.0, BirdGame3.BirdType.PENGUIN, 2, game);
        Bird lockedTarget = new Bird(260.0, BirdGame3.BirdType.EAGLE, 1, game);
        lockedTarget.health = 76.0;
        Bird rival = new Bird(210.0, BirdGame3.BirdType.PIGEON, 0, game);
        rival.health = 88.0;

        game.players[0] = rival;
        game.players[1] = lockedTarget;
        game.players[2] = ai;

        setPrivateInt(ai, "aiLockedTargetIndex", 1);
        setPrivateInt(ai, "aiTargetLockFrames", 24);

        Method pickTarget = Bird.class.getDeclaredMethod("pickAITarget");
        pickTarget.setAccessible(true);

        Bird chosen = (Bird) pickTarget.invoke(ai);

        assertEquals(lockedTarget, chosen);
    }

    @Test
    void aiTargetLockYieldsWhenAnotherTargetIsClearlyBetter() throws Exception {
        BirdGame3 game = new BirdGame3();
        game.activePlayers = 3;

        Bird ai = new Bird(100.0, BirdGame3.BirdType.PENGUIN, 2, game);
        Bird lockedTarget = new Bird(520.0, BirdGame3.BirdType.EAGLE, 1, game);
        lockedTarget.health = 100.0;
        Bird rival = new Bird(170.0, BirdGame3.BirdType.PIGEON, 0, game);
        rival.health = 30.0;

        game.players[0] = rival;
        game.players[1] = lockedTarget;
        game.players[2] = ai;

        setPrivateInt(ai, "aiLockedTargetIndex", 1);
        setPrivateInt(ai, "aiTargetLockFrames", 24);

        Method pickTarget = Bird.class.getDeclaredMethod("pickAITarget");
        pickTarget.setAccessible(true);

        Bird chosen = (Bird) pickTarget.invoke(ai);

        assertEquals(rival, chosen);
    }

    @Test
    void verticallyStackedAlliedCpusBreakApartAndFastFallTowardDistantTarget() throws Exception {
        BirdGame3 game = new BirdGame3();
        game.activePlayers = 3;
        game.selectedMap = BirdGame3.MapType.CAVE;
        game.teamModeEnabled = true;
        int[] teams = (int[]) getPrivateObject(game, "playerTeams");
        teams[0] = 1;
        teams[1] = 2;
        teams[2] = 2;

        Bird target = new Bird(1_200.0, BirdGame3.BirdType.PIGEON, 0, game);
        target.y = 1_650.0;
        Bird opiumBird = new Bird(1_200.0, BirdGame3.BirdType.OPIUMBIRD, 1, game);
        opiumBird.y = 320.0;
        Bird raven = new Bird(1_205.0, BirdGame3.BirdType.RAVEN, 2, game);
        raven.y = 355.0;

        game.players[0] = target;
        game.players[1] = opiumBird;
        game.players[2] = raven;
        game.isAI[1] = true;
        game.isAI[2] = true;
        int[] cpuLevels = (int[]) getPrivateObject(game, "cpuLevels");
        cpuLevels[1] = 5;
        setPrivateInt(opiumBird, "aiProgressTargetIndex", 0);
        setPrivateDouble(opiumBird, "aiBestTargetDistance", opiumBird.combatDistanceTo(target));
        setPrivateInt(opiumBird, "aiStackedFrames", 23);

        opiumBird.update(1.0);

        assertTrue(game.isRightPressed(1),
                "Odd-numbered stacked CPU slots should peel right when the target is directly below.");
        assertTrue(game.isBlockPressed(1),
                "The escape state should fast-fall instead of continuing the vertical bounce.");
        assertTrue(getPrivateInt(opiumBird, "aiNavigationEscapeFrames") > 0);
        assertFalse(game.isJumpPressed(1),
                "The anti-stuck descent must release Jump so flying birds cannot hover forever.");
    }

    @Test
    void titmouseAiFastFallsTowardSafeLandingInsteadOfHovering() throws Exception {
        BirdGame3 game = new BirdGame3();
        game.activePlayers = 2;
        game.selectedMap = BirdGame3.MapType.BATTLEFIELD;
        Platform mainIsland = new Platform(2_400.0, BirdGame3.GROUND_Y - 80.0, 1_200.0, 70.0);
        game.platforms.add(mainIsland);

        Bird titmouse = new Bird(2_820.0, BirdGame3.BirdType.TITMOUSE, 0, game);
        titmouse.y = mainIsland.y - 520.0;
        Bird target = new Bird(2_840.0, BirdGame3.BirdType.PIGEON, 1, game);
        target.y = mainIsland.y - target.bodyHeight();
        game.players[0] = titmouse;
        game.players[1] = target;
        game.isAI[0] = true;
        int[] cpuLevels = (int[]) getPrivateObject(game, "cpuLevels");
        cpuLevels[0] = 5;

        assertTrue(titmouse.shouldTitmouseAIReturnToGround(target));
        titmouse.update(1.0);

        assertTrue(game.isBlockPressed(0),
                "An airborne CPU Titmouse above a safe platform should commit to fast-falling.");
        assertFalse(game.isJumpPressed(0),
                "Titmouse must release sustained flight so gravity can bring it back to the stage.");
    }

    @Test
    void aiNavigationEscapeDoesNotInterruptNearbyCombat() throws Exception {
        BirdGame3 game = new BirdGame3();
        game.activePlayers = 2;

        Bird target = new Bird(1_230.0, BirdGame3.BirdType.PIGEON, 0, game);
        target.y = 520.0;
        Bird ai = new Bird(1_200.0, BirdGame3.BirdType.RAVEN, 1, game);
        ai.y = 420.0;
        game.players[0] = target;
        game.players[1] = ai;
        game.isAI[1] = true;
        setPrivateInt(ai, "aiNoProgressFrames", 104);
        setPrivateInt(ai, "aiProgressTargetIndex", 0);
        setPrivateDouble(ai, "aiBestTargetDistance", ai.combatDistanceTo(target));

        ai.update(1.0);

        assertEquals(0, getPrivateInt(ai, "aiNavigationEscapeFrames"),
                "Normal close-range exchanges should not activate navigation recovery.");
    }

    @Test
    void penguinAiUsesIceJumpToClimbTowardHigherTarget() {
        BirdGame3 game = new BirdGame3();
        game.activePlayers = 2;
        game.selectedMap = BirdGame3.MapType.FOREST;

        Platform upperPlatform = new Platform(980.0, BirdGame3.GROUND_Y - 480.0, 260.0, 46.0);
        game.platforms.add(upperPlatform);

        Bird penguin = new Bird(1040.0, BirdGame3.BirdType.PENGUIN, 0, game);
        penguin.y = BirdGame3.GROUND_Y - 80.0;
        Bird target = new Bird(1080.0, BirdGame3.BirdType.EAGLE, 1, game);
        target.y = upperPlatform.y - 80.0;

        game.players[0] = penguin;
        game.players[1] = target;
        game.isAI[0] = true;

        penguin.update(1.0);

        assertTrue(game.isSpecialPressed(0));
    }

    @Test
    void findClimbPlatformAvoidsUnreachableLedgeForPenguin() throws Exception {
        BirdGame3 game = new BirdGame3();
        Bird penguin = new Bird(960.0, BirdGame3.BirdType.PENGUIN, 0, game);

        Platform intermediate = new Platform(900.0, 1910.0, 200.0, 46.0);
        Platform unreachable = new Platform(1600.0, 1710.0, 200.0, 46.0);
        game.platforms.add(intermediate);
        game.platforms.add(unreachable);

        Method findClimb = Bird.class.getDeclaredMethod("findClimbPlatform", double.class, double.class);
        findClimb.setAccessible(true);

        Platform chosen = (Platform) findClimb.invoke(penguin, 1730.0, 600.0);

        assertEquals(intermediate, chosen);
    }

    @Test
    void hummingbirdCannotKeepClimbingAboveCameraReach() {
        BirdGame3 game = new BirdGame3();
        game.activePlayers = 1;
        Bird hummingbird = new Bird(600, BirdGame3.BirdType.HUMMINGBIRD, 0, game);
        game.players[0] = hummingbird;
        hummingbird.y = -60.0;
        hummingbird.vy = -3.5;
        game.setLocalActionsForKey(game.jumpKeyForPlayer(0), true);

        hummingbird.update(1.0);

        assertTrue(hummingbird.health < Bird.STARTING_HEALTH);
        assertTrue(hummingbird.vy > 0.0);
    }

    @Test
    void batCannotImmediatelyRehangAfterDroppingFromCeiling() throws Exception {
        BirdGame3 game = new BirdGame3();
        game.activePlayers = 1;
        Platform ceiling = new Platform(1000.0, 480.0, 320, 40);
        game.platforms.add(ceiling);

        Bird bat = new Bird(1120.0, BirdGame3.BirdType.BAT, 0, game);
        game.players[0] = bat;
        bat.x = 1120.0;
        bat.y = ceiling.y + ceiling.h + 2;

        Field batHangPlatformField = Bird.class.getDeclaredField("batHangPlatform");
        batHangPlatformField.setAccessible(true);
        batHangPlatformField.set(bat, ceiling);
        bat.batHanging = true;
        setPrivateInt(bat, "batHangLockTimer", 0);

        Method handleBatHanging = Bird.class.getDeclaredMethod("handleBatHanging", boolean.class);
        handleBatHanging.setAccessible(true);

        game.setLocalActionsForKey(game.jumpKeyForPlayer(0), true);

        boolean handledDropFrame = (boolean) handleBatHanging.invoke(bat, false);
        assertFalse(handledDropFrame);
        assertFalse(bat.batHanging);
        assertEquals(14, getPrivateInt(bat, "batRehangCooldownTimer"));

        bat.y = ceiling.y + ceiling.h + 10;
        bat.vy = -3.5;

        boolean handledImmediateRetry = (boolean) handleBatHanging.invoke(bat, false);
        assertFalse(handledImmediateRetry);
        assertFalse(bat.batHanging);

        setPrivateInt(bat, "batRehangCooldownTimer", 0);
        bat.vy = -3.5;

        boolean handledRetryAfterCooldown = (boolean) handleBatHanging.invoke(bat, false);
        assertTrue(handledRetryAfterCooldown);
        assertTrue(bat.batHanging);
    }

    @Test
    void batAiDropsFromCeilingWhenItsTargetLeavesTheAmbushLane() {
        BirdGame3 game = new BirdGame3();
        game.activePlayers = 2;

        Bird bat = new Bird(240.0, BirdGame3.BirdType.BAT, 0, game);
        Bird target = new Bird(360.0, BirdGame3.BirdType.PIGEON, 1, game);
        bat.y = 180.0;
        target.y = 360.0;
        bat.batHanging = true;
        game.players[0] = bat;
        game.players[1] = target;

        double nearbyDistance = Math.hypot(
                target.bodyCenterX() - bat.bodyCenterX(),
                target.bodyCenterY() - bat.bodyCenterY());
        assertFalse(bat.applyBatAIHangRelease(target, nearbyDistance),
                "CPU Bat should preserve a nearby downward ambush instead of dropping early.");
        assertFalse(game.isJumpPressed(0));

        target.y = bat.y;
        double nearbyWingcutDistance = Math.hypot(
                target.bodyCenterX() - bat.bodyCenterX(),
                target.bodyCenterY() - bat.bodyCenterY());
        assertFalse(bat.applyBatAIHangRelease(target, nearbyWingcutDistance),
                "CPU Bat should stay latched while a nearby target remains reachable by Wingcut or Echo Lance.");

        target.x = 920.0;
        double escapedDistance = Math.hypot(
                target.bodyCenterX() - bat.bodyCenterX(),
                target.bodyCenterY() - bat.bodyCenterY());
        assertTrue(bat.applyBatAIHangRelease(target, escapedDistance),
                "CPU Bat should release the ceiling when its opponent leaves every hanging attack route.");
        assertTrue(game.isJumpPressed(0),
                "The release decision should use Bat's normal deterministic jump input.");
    }

    @Test
    void batEchoLanceBouncesIntoTargetsOffPlatforms() throws Exception {
        BirdGame3 game = new BirdGame3();
        game.activePlayers = 2;

        Bird bat = new Bird(200.0, BirdGame3.BirdType.BAT, 0, game);
        Bird target = new Bird(300.0, BirdGame3.BirdType.PIGEON, 1, game);
        bat.y = 200.0;
        target.y = 230.0;
        bat.facingRight = true;
        bat.batHanging = true;
        game.players[0] = bat;
        game.players[1] = target;
        game.platforms.add(new Platform(170.0, 360.0, 260.0, 24.0));

        invokePrivateBooleanVoid(bat, "specialBatNeutral", false);

        assertTrue(getPrivateBoolean(bat, "batEchoFxBounced"),
                "Echo Lance should record a platform ricochet.");
        assertTrue(target.health <= Bird.STARTING_HEALTH - 13.0,
                "The rebound lane should land the stronger bounced Echo Lance hit.");
    }

    @Test
    void batWingcutCanSnapIntoCeilingHang() throws Exception {
        BirdGame3 game = new BirdGame3();
        game.activePlayers = 1;
        Platform ceiling = new Platform(160.0, 300.0, 420.0, 40.0);
        game.platforms.add(ceiling);

        Bird bat = new Bird(220.0, BirdGame3.BirdType.BAT, 0, game);
        bat.y = ceiling.y + ceiling.h + 28.0;
        bat.facingRight = true;
        game.players[0] = bat;

        invokePrivateBooleanVoid(bat, "specialBatWingcut", false);
        bat.update(1.0);

        assertTrue(bat.batHanging,
                "Wingcut should convert an underside touch into Ceiling Hang.");
        assertEquals(0, getPrivateInt(bat, "batWingcutTimer"));
    }

    @Test
    void batWingcutFromHangSkimsCeilingBeforeRelatching() throws Exception {
        BirdGame3 game = new BirdGame3();
        game.activePlayers = 1;
        Platform ceiling = new Platform(160.0, 300.0, 420.0, 40.0);
        game.platforms.add(ceiling);

        Bird bat = new Bird(220.0, BirdGame3.BirdType.BAT, 0, game);
        bat.y = ceiling.y + ceiling.h + 2.0;
        bat.facingRight = true;
        bat.batHanging = true;
        game.players[0] = bat;
        Field batHangPlatformField = Bird.class.getDeclaredField("batHangPlatform");
        batHangPlatformField.setAccessible(true);
        batHangPlatformField.set(bat, ceiling);

        invokePrivateBooleanVoid(bat, "specialBatWingcut", false);
        double startX = bat.x;
        for (int i = 0; i < 6; i++) {
            bat.update(1.0);
        }

        assertTrue(bat.x > startX + 80.0,
                "Wingcut from Ceiling Hang should travel sideways along the underside.");
        assertTrue(Math.abs(bat.y - (ceiling.y + ceiling.h + 2.0)) < 10.0,
                "Ceiling Wingcut should keep Bat near the platform underside instead of dropping immediately.");

        for (int i = 0; i < 20; i++) {
            bat.update(1.0);
        }

        assertTrue(bat.batHanging,
                "Ceiling Wingcut should relatch when the dash ends under a hangable platform.");
    }

    @Test
    void batMoonriseIsOncePerAirtimeRecovery() throws Exception {
        BirdGame3 game = new BirdGame3();
        game.activePlayers = 1;

        Bird bat = new Bird(240.0, BirdGame3.BirdType.BAT, 0, game);
        bat.y = BirdGame3.GROUND_Y - 260.0;
        game.players[0] = bat;

        game.setLocalActionsForKey(game.jumpKeyForPlayer(0), true);
        invokePrivateVoid(bat, "special");

        assertTrue(getPrivateBoolean(bat, "batMoonriseUsed"));
        assertTrue(bat.vy < -18.0,
                "Moonrise should give Bat a strong vertical recovery.");

        setPrivateInt(bat, "batMoonriseTimer", 0);
        bat.vy = 0.0;
        invokePrivateVoid(bat, "special");

        assertEquals(0.0, bat.vy, 0.0001,
                "Moonrise should not restart before Bat lands.");
        game.setLocalActionsForKey(game.jumpKeyForPlayer(0), false);

        bat.y = BirdGame3.GROUND_Y - 10.0;
        bat.update(1.0);

        assertFalse(getPrivateBoolean(bat, "batMoonriseUsed"),
                "Landing should refresh Bat's up special.");
    }

    @Test
    void batCeilingReleaseEmpowersNextAerial() throws Exception {
        double baselineDamage = batForwardAirDamageAfterCeilingRelease(false);
        double ambushDamage = batForwardAirDamageAfterCeilingRelease(true);

        assertTrue(ambushDamage > baselineDamage,
                "Dropping from Ceiling Hang should empower Bat's next aerial attack.");
    }

    @Test
    void batSilentDescentFromHangMeteorsTargetsBelow() throws Exception {
        BirdGame3 game = new BirdGame3();
        game.activePlayers = 2;

        Bird bat = new Bird(260.0, BirdGame3.BirdType.BAT, 0, game);
        Bird target = new Bird(262.0, BirdGame3.BirdType.PIGEON, 1, game);
        bat.y = 240.0;
        target.y = 338.0;
        bat.batHanging = true;
        game.players[0] = bat;
        game.players[1] = target;

        invokePrivateBooleanVoid(bat, "specialBatSilentDescent", false);
        for (int i = 0; i < 8; i++) {
            bat.update(1.0);
        }

        assertTrue(target.health < Bird.STARTING_HEALTH,
                "Silent Descent should hit targets underneath Bat.");
        assertTrue(target.vy > 0.0,
                "Starting Silent Descent from Ceiling Hang should meteor targets downward.");
        assertTrue(bat.vy < 0.0,
                "A confirmed hanging ambush should rebound Bat upward instead of carrying it through the target.");
        assertEquals(0, getPrivateInt(bat, "batSilentDiveTimer"));
        assertTrue(game.hitstopFrames >= 5,
                "The empowered Silent Descent confirm should have a readable but brief impact pause.");
    }

    @Test
    void batSilentDescentMissRemainsCommittedButAHitRebounds() throws Exception {
        BirdGame3 game = new BirdGame3();
        game.activePlayers = 2;

        Bird bat = new Bird(300.0, BirdGame3.BirdType.BAT, 0, game);
        Bird target = new Bird(640.0, BirdGame3.BirdType.PIGEON, 1, game);
        bat.y = 260.0;
        target.y = 350.0;
        game.players[0] = bat;
        game.players[1] = target;

        bat.specialBatSilentDescent(false);
        bat.batSilentStallTimer = 0;
        bat.batSilentDiveTimer = 12;
        bat.vy = 20.0;
        invokePrivateVoid(bat, "applyBatSilentDiveHits");

        assertEquals(12, bat.batSilentDiveTimer,
                "Missing Silent Descent must preserve its risky downward commitment.");
        assertTrue(bat.vy > 0.0);

        target.x = 310.0;
        invokePrivateVoid(bat, "applyBatSilentDiveHits");

        assertTrue(target.health < Bird.STARTING_HEALTH);
        assertEquals(0, bat.batSilentDiveTimer,
                "The first confirmed hit should end the dive and start the rebound immediately.");
        assertEquals(-Bird.BAT_SILENT_HIT_REBOUND_SPEED, bat.vy, 0.0001);
        assertTrue(game.hitstopFrames >= 4);
        assertTrue(game.shakeIntensity >= 7);
    }

    @Test
    void batSilentDescentFromGroundRisesBeforeDiving() throws Exception {
        BirdGame3 game = new BirdGame3();
        game.activePlayers = 1;

        Bird bat = new Bird(260.0, BirdGame3.BirdType.BAT, 0, game);
        bat.y = BirdGame3.GROUND_Y - 80.0;
        game.players[0] = bat;

        invokePrivateBooleanVoid(bat, "specialBatSilentDescent", false);
        double startY = bat.y;

        for (int i = 0; i < 4; i++) {
            bat.update(1.0);
        }

        assertTrue(bat.y < startY - 20.0,
                "Grounded Silent Descent should launch Bat upward before the stall.");
        assertTrue(getPrivateInt(bat, "batSilentStallTimer") > 0,
                "Grounded Silent Descent should still be in its startup sequence after the rise.");

        boolean sawDive = false;
        for (int i = 0; i < 16; i++) {
            bat.update(1.0);
            sawDive |= getPrivateInt(bat, "batSilentDiveTimer") > 0 || bat.vy > 0.0;
        }

        assertTrue(sawDive,
                "Grounded Silent Descent should transition into a downward dive.");
    }

    @Test
    void batCathedralEchoStartsLingeringUltimateState() throws Exception {
        BirdGame3 game = new BirdGame3();
        game.activePlayers = 2;

        Bird bat = new Bird(220.0, BirdGame3.BirdType.BAT, 0, game);
        Bird target = new Bird(312.0, BirdGame3.BirdType.PIGEON, 1, game);
        bat.y = target.y = BirdGame3.GROUND_Y - 80.0;
        game.players[0] = bat;
        game.players[1] = target;

        invokePrivateVoid(bat, "specialBatCathedralEcho");

        assertTrue(getPrivateInt(bat, "batCathedralTimer") > 0,
                "Cathedral Echo should persist after activation.");
        assertTrue(target.health < Bird.STARTING_HEALTH,
                "Cathedral Echo should open with an outward burst.");
        assertTrue(getPrivateInt(bat, "batCathedralWaveIndex") > 0,
                "Cathedral Echo should immediately begin its pulse sequence.");
    }

    private static double batForwardAirDamageAfterCeilingRelease(boolean releaseFromHang) throws Exception {
        BirdGame3 game = new BirdGame3();
        game.activePlayers = 2;

        Bird bat = new Bird(220.0, BirdGame3.BirdType.BAT, 0, game);
        Bird target = new Bird(332.0, BirdGame3.BirdType.PIGEON, 1, game);
        bat.y = target.y = BirdGame3.GROUND_Y - 260.0;
        bat.facingRight = true;
        game.players[0] = bat;
        game.players[1] = target;

        if (releaseFromHang) {
            bat.batHanging = true;
            invokePrivateVoid(bat, "releaseBatHang");
        }

        game.setLocalActionsForKey(game.rightKeyForPlayer(0), true);
        game.setLocalActionsForKey(game.attackKeyForPlayer(0), true);
        bat.update(1.0);
        advanceAuthoredAttackToFirstActiveFrame(bat);

        return Bird.STARTING_HEALTH - target.health;
    }

    @Test
    void birdsUniversallyGrabNearbyLedges() throws Exception {
        BirdGame3 game = new BirdGame3();
        game.activePlayers = 1;
        game.selectedMap = BirdGame3.MapType.BATTLEFIELD;
        Platform mainIsland = new Platform(1000.0, BirdGame3.GROUND_Y - 220.0, 900.0, 70.0);
        game.platforms.add(mainIsland);

        Bird pigeon = new Bird(mainIsland.x - 84.0, BirdGame3.BirdType.PIGEON, 0, game);
        pigeon.y = mainIsland.y - 12.0;
        pigeon.vx = 12.0;
        pigeon.vy = 4.0;
        pigeon.canDoubleJump = false;
        game.players[0] = pigeon;

        pigeon.update(1.0);

        assertTrue(getPrivateBoolean(pigeon, "ledgeHanging"));
        assertEquals(mainIsland, getPrivateObject(pigeon, "ledgePlatform"));
        assertTrue(pigeon.facingRight, "Bird should face back toward the stage while hanging.");
        assertFalse(pigeon.canDoubleJump,
                "Ledge grabs should not regenerate a midair jump that was already spent.");
        assertTrue(pigeon.y < mainIsland.y, "Bird should snap below the top lip instead of landing on the platform.");
    }

    @Test
    void rollingFromLedgeAppliesRegrabLockout() throws Exception {
        BirdGame3 game = new BirdGame3();
        game.activePlayers = 1;
        game.selectedMap = BirdGame3.MapType.BATTLEFIELD;
        Platform mainIsland = new Platform(1000.0, BirdGame3.GROUND_Y - 220.0, 900.0, 70.0);
        game.platforms.add(mainIsland);

        Bird pigeon = new Bird(mainIsland.x - 84.0, BirdGame3.BirdType.PIGEON, 0, game);
        game.players[0] = pigeon;

        pigeon.y = mainIsland.y - 12.0;
        pigeon.vx = 12.0;
        pigeon.vy = 4.0;
        pigeon.update(1.0);
        assertTrue(getPrivateBoolean(pigeon, "ledgeHanging"));

        setPrivateInt(pigeon, "ledgeLockTimer", 0);
        game.setLocalActionsForKey(game.blockKeyForPlayer(0), true);
        pigeon.update(1.0);

        assertFalse(getPrivateBoolean(pigeon, "ledgeHanging"));
        assertTrue(getPrivateInt(pigeon, "ledgeRegrabCooldownTimer") > 0,
                "Rolling from ledge should prevent immediate regrab stalling.");
        assertTrue(pigeon.x > mainIsland.x,
                "Shield from a left ledge should roll the bird safely onto the stage.");

        game.setLocalActionsForKey(game.blockKeyForPlayer(0), false);
        pigeon.x = mainIsland.x - 84.0;
        pigeon.y = mainIsland.y - 12.0;
        pigeon.vx = 12.0;
        pigeon.vy = 4.0;
        pigeon.update(1.0);

        assertFalse(getPrivateBoolean(pigeon, "ledgeHanging"),
                "Regrab cooldown should block an immediate second ledge catch.");

        setPrivateInt(pigeon, "ledgeRegrabCooldownTimer", 0);
        pigeon.x = mainIsland.x - 84.0;
        pigeon.y = mainIsland.y - 12.0;
        pigeon.vx = 12.0;
        pigeon.vy = 4.0;
        pigeon.update(1.0);

        assertTrue(getPrivateBoolean(pigeon, "ledgeHanging"));
    }

    @Test
    void repeatedLedgeGrabsProgressivelyReduceInvulnerabilityUntilLanding() throws Exception {
        BirdGame3 game = new BirdGame3();
        game.activePlayers = 1;
        Platform mainIsland = new Platform(1000.0, BirdGame3.GROUND_Y - 220.0, 900.0, 70.0);
        game.platforms.add(mainIsland);
        Bird pigeon = new Bird(900.0, BirdGame3.BirdType.PIGEON, 0, game);
        game.players[0] = pigeon;

        invokePrivateVoid(pigeon, "beginLedgeHang",
                new Class<?>[]{Platform.class, boolean.class}, mainIsland, false);
        assertTrue(getPrivateInt(pigeon, "ledgeInvulnerabilityTimer") > 0);
        invokePrivateVoid(pigeon, "dropFromLedge");
        setPrivateInt(pigeon, "ledgeRegrabCooldownTimer", 0);
        invokePrivateVoid(pigeon, "beginLedgeHang",
                new Class<?>[]{Platform.class, boolean.class}, mainIsland, false);

        assertEquals(2, getPrivateInt(pigeon, "ledgeGrabCountWithoutLanding"));
        assertEquals(14, getPrivateInt(pigeon, "ledgeInvulnerabilityTimer"),
                "The first regrab should receive only 80% of the initial intangibility.");
        assertTrue(pigeon.debugUniversalActionLabel().contains("GRAB 2"));

        invokePrivateVoid(pigeon, "dropFromLedge");
        setPrivateInt(pigeon, "ledgeRegrabCooldownTimer", 0);
        invokePrivateVoid(pigeon, "beginLedgeHang",
                new Class<?>[]{Platform.class, boolean.class}, mainIsland, false);
        assertEquals(9, getPrivateInt(pigeon, "ledgeInvulnerabilityTimer"));

        invokePrivateVoid(pigeon, "dropFromLedge");
        setPrivateInt(pigeon, "ledgeRegrabCooldownTimer", 0);
        invokePrivateVoid(pigeon, "beginLedgeHang",
                new Class<?>[]{Platform.class, boolean.class}, mainIsland, false);
        assertEquals(0, getPrivateInt(pigeon, "ledgeInvulnerabilityTimer"),
                "The third regrab and later should be fully punishable.");
    }

    @Test
    void holdingAwayDropsFromLedge() throws Exception {
        BirdGame3 game = new BirdGame3();
        game.activePlayers = 1;
        Platform mainIsland = new Platform(1000.0, BirdGame3.GROUND_Y - 220.0, 900.0, 70.0);
        game.platforms.add(mainIsland);
        Bird pigeon = new Bird(900.0, BirdGame3.BirdType.PIGEON, 0, game);
        game.players[0] = pigeon;
        invokePrivateVoid(pigeon, "beginLedgeHang",
                new Class<?>[]{Platform.class, boolean.class}, mainIsland, false);
        setPrivateInt(pigeon, "ledgeLockTimer", 0);
        game.setLocalActionsForKey(game.leftKeyForPlayer(0), true);

        pigeon.update(1.0);

        assertFalse(getPrivateBoolean(pigeon, "ledgeHanging"));
        assertTrue(pigeon.vy > 0.0);
        assertEquals(1, getPrivateInt(pigeon, "ledgeGrabCountWithoutLanding"),
                "Dropping should preserve the regrab penalty until the bird lands.");
    }

    @Test
    void occupiedLedgeIsTrumpedDeterministically() throws Exception {
        BirdGame3 game = new BirdGame3();
        game.activePlayers = 2;
        Platform mainIsland = new Platform(1000.0, BirdGame3.GROUND_Y - 220.0, 900.0, 70.0);
        game.platforms.add(mainIsland);
        Bird first = new Bird(900.0, BirdGame3.BirdType.PIGEON, 0, game);
        Bird second = new Bird(900.0, BirdGame3.BirdType.EAGLE, 1, game);
        game.players[0] = first;
        game.players[1] = second;
        invokePrivateVoid(first, "beginLedgeHang",
                new Class<?>[]{Platform.class, boolean.class}, mainIsland, false);

        invokePrivateVoid(second, "beginLedgeHang",
                new Class<?>[]{Platform.class, boolean.class}, mainIsland, false);

        assertFalse(getPrivateBoolean(first, "ledgeHanging"));
        assertTrue(getPrivateInt(first, "ledgeRegrabCooldownTimer") >= 36);
        assertEquals(0, getPrivateInt(first, "ledgeInvulnerabilityTimer"));
        assertTrue(Math.abs(first.vx) > 0.0);
        assertTrue(getPrivateBoolean(second, "ledgeHanging"));
    }

    @Test
    void lanSnapshotsRestoreActiveLedgePlatformAndTimers() throws Exception {
        BirdGame3 sourceGame = new BirdGame3();
        sourceGame.activePlayers = 1;
        Platform sourcePlatform = new Platform(1000.0, BirdGame3.GROUND_Y - 220.0, 900.0, 70.0);
        sourceGame.platforms.add(sourcePlatform);
        Bird source = new Bird(900.0, BirdGame3.BirdType.PIGEON, 0, sourceGame);
        sourceGame.players[0] = source;
        invokePrivateVoid(source, "beginLedgeHang",
                new Class<?>[]{Platform.class, boolean.class}, sourcePlatform, false);
        setPrivateInt(source, "ledgeHangFrames", 73);
        setPrivateInt(source, "ledgeGrabCountWithoutLanding", 2);
        LanBirdState snapshot = source.toLanState();

        BirdGame3 remoteGame = new BirdGame3();
        remoteGame.activePlayers = 1;
        Platform remotePlatform = new Platform(1000.0, BirdGame3.GROUND_Y - 220.0, 900.0, 70.0);
        remoteGame.platforms.add(remotePlatform);
        Bird restored = new Bird(0.0, BirdGame3.BirdType.PIGEON, 0, remoteGame);
        remoteGame.players[0] = restored;
        restored.applyLanState(snapshot);

        assertTrue(getPrivateBoolean(restored, "ledgeHanging"));
        assertSame(remotePlatform, getPrivateObject(restored, "ledgePlatform"));
        assertEquals(73, getPrivateInt(restored, "ledgeHangFrames"));
        assertEquals(2, getPrivateInt(restored, "ledgeGrabCountWithoutLanding"));
        assertEquals(source.deterministicLedgeStateHash(), restored.deterministicLedgeStateHash());

        restored.update(1.0);
        assertTrue(getPrivateBoolean(restored, "ledgeHanging"),
                "A restored ledge hang must survive the next simulation frame.");
    }

    @Test
    void trainingTelemetryReportsActionabilityDefenseMovementAndControlState() throws Exception {
        BirdGame3 game = new BirdGame3();
        game.activePlayers = 1;
        Bird pigeon = new Bird(500.0, BirdGame3.BirdType.PIGEON, 0, game);
        pigeon.y = BirdGame3.GROUND_Y - 80.0;
        game.players[0] = pigeon;

        assertEquals(0, pigeon.debugUniversalActionableFrames());
        assertEquals(1.0, pigeon.debugShieldDurabilityRatio(), 0.0001);
        assertTrue(pigeon.debugMovementTelemetryLabel().contains("GROUND"));
        assertTrue(pigeon.debugDefenseTelemetryLabel().contains("SHIELD 100%"));
        assertTrue(pigeon.debugGrabLedgeTelemetryLabel().contains("GRAB READY"));

        invokePrivateVoid(pigeon, "startSpotDodge");

        assertTrue(pigeon.debugUniversalActionableFrames() > 0);
        assertEquals(10, pigeon.debugCombatInvulnerabilityFrames());
        assertTrue(pigeon.debugDefenseTelemetryLabel().contains("INV 10f"));
        assertTrue(pigeon.debugUniversalActionLabel().startsWith("SPOT DODGE"));
    }

    @Test
    void excessiveLedgeHangForcesADrop() throws Exception {
        BirdGame3 game = new BirdGame3();
        game.activePlayers = 1;
        Platform mainIsland = new Platform(1000.0, BirdGame3.GROUND_Y - 220.0, 900.0, 70.0);
        game.platforms.add(mainIsland);
        Bird pigeon = new Bird(900.0, BirdGame3.BirdType.PIGEON, 0, game);
        game.players[0] = pigeon;
        invokePrivateVoid(pigeon, "beginLedgeHang",
                new Class<?>[]{Platform.class, boolean.class}, mainIsland, false);
        setPrivateInt(pigeon, "ledgeLockTimer", 0);
        setPrivateInt(pigeon, "ledgeHangFrames", 179);

        pigeon.update(1.0);

        assertFalse(getPrivateBoolean(pigeon, "ledgeHanging"));
        assertTrue(getPrivateInt(pigeon, "ledgeRegrabCooldownTimer") > 0);
        assertTrue(pigeon.vy > 0.0);
    }

    @Test
    void vineGrapplePickupNowGrantsOneUse() throws Exception {
        BirdGame3 game = new BirdGame3();
        Bird bird = new Bird(1000.0, BirdGame3.BirdType.PIGEON, 0, game);
        game.players[0] = bird;
        game.powerUps.add(new PowerUp(bird.x + 40.0, bird.y + 40.0, PowerUpType.VINE_GRAPPLE));

        invokePrivateVoid(bird, "handlePowerUpPickup");

        assertEquals(1, getPrivateInt(bird, "grappleUses"));
    }

    @Test
    void vineGrappleSpawnsTemporaryVineFromPlatformAbove() throws Exception {
        BirdGame3 game = new BirdGame3();
        game.activePlayers = 1;
        Platform above = new Platform(900.0, 560.0, 320, 36);
        game.platforms.add(above);

        Bird bird = new Bird(1000.0, BirdGame3.BirdType.PIGEON, 0, game);
        bird.x = 1000.0;
        bird.y = 940.0;
        game.players[0] = bird;

        setPrivateInt(bird, "grappleUses", 1);
        setPrivateInt(bird, "grappleTimer", 480);
        bird.specialCooldown = 0;
        game.setLocalActionsForKey(game.specialKeyForPlayer(0), true);

        invokePrivateVoid(bird, "handleVineGrapple");

        assertTrue(bird.onVine);
        assertNotNull(bird.attachedVine);
        assertTrue(bird.attachedVine.temporary);
        assertEquals(above.y + above.h, bird.attachedVine.baseY, 0.0001);
        assertEquals(0, getPrivateInt(bird, "grappleUses"));
        assertEquals(1, game.swingingVines.size());
    }

    @Test
    void vineAutoLaunchNowDependsOnSwingSpeedAndOutwardArc() throws Exception {
        BirdGame3 game = new BirdGame3();
        SwingingVine fastOutward = new SwingingVine(1200.0, 400.0, 420.0);
        fastOutward.angle = 0.55;
        fastOutward.angularVelocity = 0.045;

        SwingingVine slowVine = new SwingingVine(1200.0, 400.0, 420.0);
        slowVine.angle = 0.55;
        slowVine.angularVelocity = 0.03;

        SwingingVine inwardVine = new SwingingVine(1200.0, 400.0, 420.0);
        inwardVine.angle = 0.55;
        inwardVine.angularVelocity = -0.045;

        Method shouldAutoLaunch = BirdGame3.class.getDeclaredMethod("shouldAutoLaunchFromVine", SwingingVine.class);
        shouldAutoLaunch.setAccessible(true);

        assertTrue((boolean) shouldAutoLaunch.invoke(game, fastOutward));
        assertFalse((boolean) shouldAutoLaunch.invoke(game, slowVine));
        assertFalse((boolean) shouldAutoLaunch.invoke(game, inwardVine));
    }

    @Test
    void releasedTemporaryVineDetachesBeforeDisappearing() throws Exception {
        BirdGame3 game = new BirdGame3();
        SwingingVine vine = new SwingingVine(1200.0, 420.0, 320.0);
        vine.temporary = true;
        game.swingingVines.add(vine);

        Method updateSwingingVines = BirdGame3.class.getDeclaredMethod("updateSwingingVines");
        updateSwingingVines.setAccessible(true);
        updateSwingingVines.invoke(game);

        assertEquals(1, game.swingingVines.size());
        assertTrue(vine.detaching);
    }

    @Test
    void dockWaterLetsBirdSwimUpward() throws Exception {
        BirdGame3 game = new BirdGame3();
        game.activePlayers = 1;
        game.selectedMap = BirdGame3.MapType.DOCK;

        Method setupDockArena = BirdGame3.class.getDeclaredMethod("setupDockArena");
        setupDockArena.setAccessible(true);
        setupDockArena.invoke(game);

        Bird bird = new Bird(3900.0, BirdGame3.BirdType.PIGEON, 0, game);
        bird.x = 3900.0;
        bird.y = game.dockWaterSurfaceY() + 120.0;
        game.players[0] = bird;
        game.setLocalActionsForKey(game.jumpKeyForPlayer(0), true);

        double startY = bird.y;
        bird.update(1.0);

        assertTrue(bird.vy < 0.0);
        assertTrue(bird.y < startY);
    }

    @Test
    void dockUsesSandFloorOutsideWaterGap() throws Exception {
        BirdGame3 game = new BirdGame3();
        game.activePlayers = 1;
        game.selectedMap = BirdGame3.MapType.DOCK;

        Method setupDockArena = BirdGame3.class.getDeclaredMethod("setupDockArena");
        setupDockArena.setAccessible(true);
        setupDockArena.invoke(game);

        Bird sandBird = new Bird(1600.0, BirdGame3.BirdType.PIGEON, 0, game);
        sandBird.x = 1600.0;
        sandBird.y = BirdGame3.GROUND_Y + 24.0;
        sandBird.vy = 6.0;

        Bird waterBird = new Bird(3900.0, BirdGame3.BirdType.PIGEON, 0, game);
        waterBird.x = 3900.0;
        waterBird.y = BirdGame3.GROUND_Y + 24.0;
        waterBird.vy = 6.0;

        assertTrue(sandBird.isOnGround());
        assertFalse(waterBird.isOnGround());
        assertTrue(waterBird.y > BirdGame3.GROUND_Y - 20.0);
    }

    @Test
    void dockSkiffPlatformsSitAboveWaterline() throws Exception {
        BirdGame3 game = new BirdGame3();
        game.selectedMap = BirdGame3.MapType.DOCK;

        Method setupDockArena = BirdGame3.class.getDeclaredMethod("setupDockArena");
        setupDockArena.setAccessible(true);
        setupDockArena.invoke(game);

        double waterline = game.dockWaterSurfaceY();
        long submergedSkiffs = game.platforms.stream()
                .filter(p -> p.x >= 2800.0 && p.x <= 3400.0 && p.w <= 260.0 && p.h <= 24.0)
                .filter(p -> p.y >= waterline)
                .count();

        assertEquals(0, submergedSkiffs);
    }

    @Test
    void dockMatchSpawnsEveryBirdOnAStableSurface() throws Exception {
        BirdGame3 game = new BirdGame3();
        game.selectedMap = BirdGame3.MapType.DOCK;
        game.activePlayers = 4;
        game.players[0] = new Bird(0.0, BirdGame3.BirdType.PELICAN, 0, game);
        game.players[1] = new Bird(0.0, BirdGame3.BirdType.GOOSE, 1, game);
        game.players[2] = new Bird(0.0, BirdGame3.BirdType.RAVEN, 2, game);
        game.players[3] = new Bird(0.0, BirdGame3.BirdType.HEISENBIRD, 3, game);

        Method setupDockArena = BirdGame3.class.getDeclaredMethod("setupDockArena");
        setupDockArena.setAccessible(true);
        setupDockArena.invoke(game);
        Method positionBattlefieldSpawns = BirdGame3.class.getDeclaredMethod("positionBattlefieldSpawns");
        positionBattlefieldSpawns.setAccessible(true);
        positionBattlefieldSpawns.invoke(game);

        for (int i = 0; i < game.activePlayers; i++) {
            Bird bird = game.players[i];
            assertNotNull(bird);
            assertTrue(bird.isOnGround(),
                    bird.type.name + " should begin the Dock countdown standing on a platform.");
            assertEquals(bird.x, bird.prevX, 0.0001);
            assertEquals(bird.y, bird.prevY, 0.0001);
        }
    }

    @Test
    void lockedMatchCountdownUsesIdlePoseEvenForAirborneSpawns() throws Exception {
        BirdGame3 game = new BirdGame3();
        Bird bird = new Bird(300.0, BirdGame3.BirdType.PELICAN, 0, game);
        bird.y = BirdGame3.GROUND_Y - 500.0;
        game.players[0] = bird;

        Method animationState = Bird.class.getDeclaredMethod("currentBirdAnimationState");
        animationState.setAccessible(true);
        setPrivateInt(game, "matchIntroOverlayFrames", 120);

        assertEquals("IDLE", animationState.invoke(bird).toString(),
                "The frozen 3-2-1 countdown should never display a falling or attack pose.");

        setPrivateInt(game, "matchIntroOverlayFrames", 40);
        assertEquals("FALL", animationState.invoke(bird).toString(),
                "Once the fight is live, an airborne bird should resume its normal fall pose.");
    }

    @Test
    void dockLeverLaunchesPirateBomb() throws Exception {
        BirdGame3 game = new BirdGame3();
        game.selectedMap = BirdGame3.MapType.DOCK;
        game.activePlayers = 2;

        Method setupDockArena = BirdGame3.class.getDeclaredMethod("setupDockArena");
        setupDockArena.setAccessible(true);
        setupDockArena.invoke(game);

        Bird puller = new Bird(0.0, BirdGame3.BirdType.PELICAN, 0, game);
        Bird target = new Bird(3600.0, BirdGame3.BirdType.EAGLE, 1, game);
        game.players[0] = puller;
        game.players[1] = target;

        puller.x = getPrivateDouble(game, "dockLeverX") - 40.0;
        puller.y = getPrivateDouble(game, "dockLeverY") - 40.0;

        Method launchDockShipBomb = BirdGame3.class.getDeclaredMethod("launchDockShipBomb", Bird.class, Bird.class);
        launchDockShipBomb.setAccessible(true);
        launchDockShipBomb.invoke(game, puller, target);

        DockShipBomb bomb = (DockShipBomb) getPrivateObject(game, "dockShipBomb");
        assertNotNull(bomb);
        assertTrue(getPrivateInt(game, "dockLeverCooldown") > 0);
        assertFalse(bomb.fired);
        assertTrue(bomb.launchDelayFrames > 0);
    }

    @Test
    void dockStageUpdateDoesNotOverflowLeverState() throws Exception {
        BirdGame3 game = new BirdGame3();
        game.selectedMap = BirdGame3.MapType.DOCK;
        game.activePlayers = 2;

        Method setupDockArena = BirdGame3.class.getDeclaredMethod("setupDockArena");
        setupDockArena.setAccessible(true);
        setupDockArena.invoke(game);

        game.players[0] = new Bird(1040.0, BirdGame3.BirdType.PIGEON, 0, game);
        game.players[1] = new Bird(3660.0, BirdGame3.BirdType.EAGLE, 1, game);

        Method updateWorldFixed = BirdGame3.class.getDeclaredMethod("updateWorldFixed");
        updateWorldFixed.setAccessible(true);

        assertDoesNotThrow(() -> updateWorldFixed.invoke(game));
    }

    @Test
    void ashfallCathedralUsesIslandBoundsAndThermals() throws Exception {
        BirdGame3 game = new BirdGame3();
        game.selectedMap = BirdGame3.MapType.ASHFALL_CATHEDRAL;

        Method setupAshfallCathedralArena = BirdGame3.class.getDeclaredMethod("setupAshfallCathedralArena");
        setupAshfallCathedralArena.setAccessible(true);
        setupAshfallCathedralArena.invoke(game);

        Bird bird = new Bird(3000.0, BirdGame3.BirdType.PHOENIX, 0, game);
        game.players[0] = bird;

        assertTrue(bird.usesIslandBounds());
        assertFalse(bird.hasSolidGroundFloorUnderBody(),
                "Ashfall Cathedral should not have the normal invisible ground floor.");
        assertTrue(game.windVents.size() >= 3);
        assertTrue(game.platforms.stream().anyMatch(p -> p.y > BirdGame3.GROUND_Y),
                "Ashfall Cathedral should include low recovery fragments over the lava sea.");
    }

    @Test
    void ashfallGeyserWarningDoesNotDamage() throws Exception {
        BirdGame3 game = new BirdGame3();
        game.selectedMap = BirdGame3.MapType.ASHFALL_CATHEDRAL;
        game.activePlayers = 1;

        Method setupAshfallCathedralArena = BirdGame3.class.getDeclaredMethod("setupAshfallCathedralArena");
        setupAshfallCathedralArena.setAccessible(true);
        setupAshfallCathedralArena.invoke(game);

        Bird bird = new Bird(1540.0, BirdGame3.BirdType.EAGLE, 0, game);
        bird.x = 1540.0;
        bird.y = BirdGame3.GROUND_Y - 138.0 - 80.0;
        game.players[0] = bird;
        double startingHealth = bird.health;
        game.simTick = 40L;

        Method updateAshfallCathedralHazards = BirdGame3.class.getDeclaredMethod("updateAshfallCathedralHazards");
        updateAshfallCathedralHazards.setAccessible(true);
        updateAshfallCathedralHazards.invoke(game);

        assertEquals(startingHealth, bird.health, 0.0001);
        assertEquals(0.0, bird.vy, 0.0001);
    }

    @Test
    void ashfallGeyserImpactLaunchesAndDamages() throws Exception {
        BirdGame3 game = new BirdGame3();
        game.selectedMap = BirdGame3.MapType.ASHFALL_CATHEDRAL;
        game.activePlayers = 1;

        Method setupAshfallCathedralArena = BirdGame3.class.getDeclaredMethod("setupAshfallCathedralArena");
        setupAshfallCathedralArena.setAccessible(true);
        setupAshfallCathedralArena.invoke(game);

        Bird bird = new Bird(1540.0, BirdGame3.BirdType.EAGLE, 0, game);
        bird.x = 1540.0;
        bird.y = BirdGame3.GROUND_Y - 138.0 - 80.0;
        game.players[0] = bird;
        double startingHealth = bird.health;
        game.simTick = 92L;

        Method updateAshfallCathedralHazards = BirdGame3.class.getDeclaredMethod("updateAshfallCathedralHazards");
        updateAshfallCathedralHazards.setAccessible(true);
        updateAshfallCathedralHazards.invoke(game);

        assertTrue(bird.health < startingHealth);
        assertTrue(bird.vy < -10.0);
        assertTrue(bird.stunTime > 0.0);
        assertTrue(game.isAchievementUnlocked(BirdGame3Achievement.GEYSER_RIDER));
        assertEquals(1, game.achievementProgressValue(BirdGame3Achievement.GEYSER_RIDER));
    }

    @Test
    void dockBombLocksOnBeforeFiring() throws Exception {
        BirdGame3 game = new BirdGame3();
        game.selectedMap = BirdGame3.MapType.DOCK;
        game.activePlayers = 2;

        Method setupDockArena = BirdGame3.class.getDeclaredMethod("setupDockArena");
        setupDockArena.setAccessible(true);
        setupDockArena.invoke(game);

        Bird puller = new Bird(0.0, BirdGame3.BirdType.PELICAN, 0, game);
        Bird target = new Bird(3600.0, BirdGame3.BirdType.EAGLE, 1, game);
        game.players[0] = puller;
        game.players[1] = target;

        Method launchDockShipBomb = BirdGame3.class.getDeclaredMethod("launchDockShipBomb", Bird.class, Bird.class);
        launchDockShipBomb.setAccessible(true);
        launchDockShipBomb.invoke(game, puller, target);

        Method updateDockShipBomb = BirdGame3.class.getDeclaredMethod("updateDockShipBomb");
        updateDockShipBomb.setAccessible(true);
        DockShipBomb bomb = (DockShipBomb) getPrivateObject(game, "dockShipBomb");
        int delay = bomb.launchDelayFrames;
        for (int i = 0; i < delay; i++) {
            updateDockShipBomb.invoke(game);
        }

        bomb = (DockShipBomb) getPrivateObject(game, "dockShipBomb");
        assertNotNull(bomb);
        assertTrue(bomb.fired);
        assertTrue(bomb.cannonFlashFrames > 0);
    }

    @Test
    void dockMapCanBeUnlockedFromShopPreview() throws Exception {
        BirdGame3 game = new BirdGame3();
        ShopPreview preview = new ShopPreview(null, "MAP_DOCK", "Broken Harbor Map");

        Method isOwned = BirdGame3.class.getDeclaredMethod("isShopPreviewOwned", ShopPreview.class);
        isOwned.setAccessible(true);
        Method unlock = BirdGame3.class.getDeclaredMethod("unlockShopPreview", ShopPreview.class);
        unlock.setAccessible(true);

        assertFalse((boolean) isOwned.invoke(game, preview));

        unlock.invoke(game, preview);

        assertTrue((boolean) isOwned.invoke(game, preview));
        assertTrue(getPrivateBoolean(game, "dockMapUnlocked"));
    }

    @Test
    void premiumPacksIncludeRoadrunnerAndDesertRewardsAndUnlockThem() throws Exception {
        BirdGame3 game = new BirdGame3();

        Method buildShopItems = BirdGame3.class.getDeclaredMethod("buildShopItems");
        buildShopItems.setAccessible(true);
        @SuppressWarnings("unchecked")
        List<ShopItem> items = (List<ShopItem>) buildShopItems.invoke(game);

        for (String packName : List.of("Rooftop Pack", "Skyline Pack", "Nebula Pack", "Ascendant Pack")) {
            ShopItem pack = items.stream()
                    .filter(item -> packName.equals(item.name))
                    .findFirst()
                    .orElseThrow();
            assertTrue(pack.previews.stream().anyMatch(preview -> "CHAR_ROADRUNNER".equals(preview.skinKey())));
            assertTrue(pack.previews.stream().anyMatch(preview -> "MAP_DESERT".equals(preview.skinKey())));
        }

        Method isOwned = BirdGame3.class.getDeclaredMethod("isShopPreviewOwned", ShopPreview.class);
        isOwned.setAccessible(true);
        Method unlock = BirdGame3.class.getDeclaredMethod("unlockShopPreview", ShopPreview.class);
        unlock.setAccessible(true);

        ShopPreview roadrunner = new ShopPreview(BirdGame3.BirdType.ROADRUNNER, "CHAR_ROADRUNNER", "Roadrunner");
        ShopPreview desert = new ShopPreview(null, "MAP_DESERT", "Sunscorch Flats Map");

        assertFalse((boolean) isOwned.invoke(game, roadrunner));
        assertFalse((boolean) isOwned.invoke(game, desert));

        unlock.invoke(game, roadrunner);
        unlock.invoke(game, desert);

        assertTrue((boolean) isOwned.invoke(game, roadrunner));
        assertTrue((boolean) isOwned.invoke(game, desert));
        assertTrue(game.roadrunnerUnlocked);
        assertTrue(getPrivateBoolean(game, "desertMapUnlocked"));
    }

    @Test
    void nullRockCannotBeStunnedOrShrunkAndAscendsAtHalfHealth() throws Exception {
        BirdGame3 game = new BirdGame3();
        game.nullRockVultureUnlocked = true;
        Bird nullRock = new Bird(600.0, BirdGame3.BirdType.VULTURE, 0, game);

        Method applySkin = BirdGame3.class.getDeclaredMethod(
                "applySkinChoiceToBird",
                Bird.class,
                BirdGame3.BirdType.class,
                String.class
        );
        applySkin.setAccessible(true);
        applySkin.invoke(game, nullRock, BirdGame3.BirdType.VULTURE, "NULL_ROCK_VULTURE");

        double baseSize = nullRock.baseSizeMultiplier;
        double basePower = nullRock.basePowerMultiplier;
        double baseSpeed = nullRock.baseSpeedMultiplier;

        nullRock.applyStun(90);
        nullRock.applyShrinkEffect();

        assertEquals(0.0, nullRock.stunTime, 0.0001);
        assertEquals(0, nullRock.shrinkTimer);
        assertEquals(baseSize, nullRock.sizeMultiplier, 0.0001);

        nullRock.health = nullRock.getMaxHealth() * 0.50 + 20.0;
        double dealt = nullRock.receiveExternalDamage(40.0);

        assertTrue(dealt > 0.0);
        assertTrue(nullRock.isTrueNullRockForm());
        assertTrue(nullRock.baseSizeMultiplier > baseSize);
        assertTrue(nullRock.basePowerMultiplier > basePower);
        assertTrue(nullRock.baseSpeedMultiplier > baseSpeed);
        assertEquals("P1: True Null Rock", game.healthBarLabel(nullRock));
    }

    @Test
    void nullRockQuicklyFliesBackFromBattlefieldVoid() throws Exception {
        BirdGame3 game = new BirdGame3();
        game.selectedMap = BirdGame3.MapType.BATTLEFIELD;
        invokePrivateVoid(game, "setupBattlefieldArena");
        Bird nullRock = new Bird(game.battlefieldSpawnCenterX(), BirdGame3.BirdType.VULTURE, 0, game);
        nullRock.isNullRockSkin = true;
        nullRock.setBaseMultipliers(3.6, 1.0, 1.0);
        game.activePlayers = 1;
        game.players[0] = nullRock;
        double healthBefore = nullRock.health;
        nullRock.x += 500.0;
        nullRock.y = game.battlefieldVoidFloorY() + 400.0;
        nullRock.vy = 18.0;

        nullRock.update(1.0);

        assertTrue(nullRock.nullRockVoidRecoveryTimer > 0);
        assertTrue(nullRock.vy < 0.0, "The recovery should immediately launch Null Rock upward.");
        assertEquals(healthBefore, nullRock.health, 0.0001,
                "Falling into the void should not damage or defeat Null Rock.");

        for (int frame = 0; frame < Bird.NULL_ROCK_VOID_RECOVERY_FRAMES + 12; frame++) {
            nullRock.update(1.0);
        }

        assertEquals(0, nullRock.nullRockVoidRecoveryTimer);
        assertEquals(game.battlefieldSpawnCenterX(), nullRock.bodyCenterX(), 0.0001,
                "The recovery should return Null Rock to center stage.");
        assertTrue(nullRock.y < game.battlefieldSpawnY(nullRock.sizeMultiplier),
                "The recovery should finish safely above the stage.");
        assertEquals(healthBefore, nullRock.health, 0.0001);
    }

    @Test
    void nullRockPickupUsesExpandedBodyBounds() throws Exception {
        BirdGame3 game = new BirdGame3();
        Bird nullRock = new Bird(1000.0, BirdGame3.BirdType.VULTURE, 0, game);
        nullRock.isNullRockSkin = true;
        game.powerUps.add(new PowerUp(nullRock.x + 130.0, nullRock.y + 40.0, PowerUpType.SPEED));

        invokePrivateVoid(nullRock, "handlePowerUpPickup");

        assertTrue(game.powerUps.isEmpty());
        assertTrue(nullRock.speedTimer > 0);
    }

    @Test
    void attacksCanHitAcrossNullRockExpandedCombatBody() throws Exception {
        BirdGame3 game = new BirdGame3();
        game.activePlayers = 2;

        Bird attacker = new Bird(1000.0, BirdGame3.BirdType.PIGEON, 0, game);
        Bird nullRock = new Bird(1170.0, BirdGame3.BirdType.VULTURE, 1, game);
        nullRock.isNullRockSkin = true;

        game.players[0] = attacker;
        game.players[1] = nullRock;

        invokePrivateVoid(attacker, "attack");

        assertTrue(nullRock.health < Bird.STARTING_HEALTH);
    }

    @Test
    void nullRockRegularAttackStaysFocusedNearItsBeak() throws Exception {
        BirdGame3 game = new BirdGame3();
        game.nullRockVultureUnlocked = true;
        game.activePlayers = 3;

        Bird nullRock = new Bird(1000.0, BirdGame3.BirdType.VULTURE, 0, game);
        Bird targetNearBeak = new Bird(1365.0, BirdGame3.BirdType.EAGLE, 1, game);
        Bird targetBehind = new Bird(820.0, BirdGame3.BirdType.PIGEON, 2, game);
        nullRock.facingRight = true;

        Method applySkin = BirdGame3.class.getDeclaredMethod(
                "applySkinChoiceToBird",
                Bird.class,
                BirdGame3.BirdType.class,
                String.class
        );
        applySkin.setAccessible(true);
        applySkin.invoke(game, nullRock, BirdGame3.BirdType.VULTURE, "NULL_ROCK_VULTURE");

        game.players[0] = nullRock;
        game.players[1] = targetNearBeak;
        game.players[2] = targetBehind;

        invokePrivateVoid(nullRock, "attack");

        assertTrue(targetNearBeak.health < Bird.STARTING_HEALTH);
        assertEquals(Bird.STARTING_HEALTH, targetBehind.health, 0.0001);
    }

    @Test
    void localHealthBarUsesNullRockName() {
        BirdGame3 game = new BirdGame3();
        Bird nullRock = new Bird(600.0, BirdGame3.BirdType.VULTURE, 0, game);
        nullRock.isNullRockSkin = true;

        assertEquals("P1: The Null Rock", game.healthBarLabel(nullRock));
    }

    @Test
    void particleBurstsScaleDownDuringHeavyFightLoad() throws Exception {
        BirdGame3 game = new BirdGame3();
        game.activePlayers = 12;
        for (int i = 0; i < game.activePlayers; i++) {
            game.players[i] = new Bird(600.0 + i * 80.0, BirdGame3.BirdType.PIGEON, i, game);
        }
        for (int i = 0; i < 28; i++) {
            game.crowMinions.add(new CrowMinion(1200.0 + i * 10.0, 400.0, null));
        }
        for (int i = 0; i < 10; i++) {
            game.chickMinions.add(new ChickMinion(1000.0 + i * 20.0, 420.0, 0, false, null));
        }
        for (int i = 0; i < 1500; i++) {
            game.particles.add(new Particle(900.0, 400.0, 0.0, 0.0, javafx.scene.paint.Color.WHITE));
        }

        Method method = BirdGame3.class.getDeclaredMethod("scaledParticleBurstCount", int.class);
        method.setAccessible(true);
        int scaled = (int) method.invoke(game, 200);

        assertTrue(scaled < 200);
        assertTrue(scaled >= 24);
    }

    @Test
    void transientEffectOverflowTrimKeepsParticlesAndMinionsUnderCaps() throws Exception {
        BirdGame3 game = new BirdGame3();
        game.activePlayers = 12;
        for (int i = 0; i < game.activePlayers; i++) {
            game.players[i] = new Bird(500.0 + i * 90.0, BirdGame3.BirdType.EAGLE, i, game);
        }
        for (int i = 0; i < 3200; i++) {
            game.particles.add(new Particle(1000.0, 300.0, 0.0, 0.0, javafx.scene.paint.Color.GOLD));
        }
        for (int i = 0; i < 80; i++) {
            game.crowMinions.add(new CrowMinion(1200.0 + i * 14.0, 300.0, null));
        }
        for (int i = 0; i < 24; i++) {
            game.chickMinions.add(new ChickMinion(1300.0 + i * 18.0, 320.0, 0, false, null));
        }
        for (int i = 0; i < 18; i++) {
            game.piranhaHazards.add(new PiranhaHazard(2600.0 + i * 12.0, 2350.0, -4.5));
        }

        Method trim = BirdGame3.class.getDeclaredMethod("trimTransientEffectOverflow");
        trim.setAccessible(true);
        trim.invoke(game);

        Method particleCapMethod = BirdGame3.class.getDeclaredMethod("activeParticleSoftCap");
        particleCapMethod.setAccessible(true);
        Method crowCapMethod = BirdGame3.class.getDeclaredMethod("activeCrowMinionCap");
        crowCapMethod.setAccessible(true);
        Method chickCapMethod = BirdGame3.class.getDeclaredMethod("activeChickMinionCap");
        chickCapMethod.setAccessible(true);
        Method piranhaCapMethod = BirdGame3.class.getDeclaredMethod("activePiranhaHazardCap");
        piranhaCapMethod.setAccessible(true);

        int particleCap = (int) particleCapMethod.invoke(game);
        int crowCap = (int) crowCapMethod.invoke(game);
        int chickCap = (int) chickCapMethod.invoke(game);
        int piranhaCap = (int) piranhaCapMethod.invoke(game);

        assertTrue(game.particles.size() <= particleCap);
        assertTrue(game.crowMinions.size() <= crowCap);
        assertTrue(game.chickMinions.size() <= chickCap);
        assertTrue(game.piranhaHazards.size() <= piranhaCap);
    }

    @Test
    void trainingHitTrackingBuildsComboSessionDamageAndBlockWindow() throws Exception {
        BirdGame3 game = new BirdGame3();
        game.trainingModeActive = true;

        Bird player = new Bird(100.0, BirdGame3.BirdType.PIGEON, 0, game);
        Bird dummy = new Bird(220.0, BirdGame3.BirdType.EAGLE, 1, game);
        game.players[0] = player;
        game.players[1] = dummy;

        Method record = BirdGame3.class.getDeclaredMethod("recordTrainingHit", Bird.class, Bird.class, double.class);
        record.setAccessible(true);
        record.invoke(game, player, dummy, 18.5);

        assertEquals(1, getPrivateInt(game, "trainingComboHits"));
        assertEquals(18.5, getPrivateDouble(game, "trainingComboDamage"), 0.0001);
        assertEquals(18.5, getPrivateDouble(game, "trainingSessionDamage"), 0.0001);
        assertEquals(18.5, getPrivateDouble(game, "trainingLastHitDamage"), 0.0001);
        assertTrue(getPrivateInt(game, "trainingDummyBlockFrames") > 0);
    }

    @Test
    void trainingComboExpiresAfterWindowButKeepsSessionDamage() throws Exception {
        BirdGame3 game = new BirdGame3();
        game.trainingModeActive = true;

        Bird player = new Bird(100.0, BirdGame3.BirdType.PIGEON, 0, game);
        Bird dummy = new Bird(220.0, BirdGame3.BirdType.EAGLE, 1, game);
        game.players[0] = player;
        game.players[1] = dummy;

        Method record = BirdGame3.class.getDeclaredMethod("recordTrainingHit", Bird.class, Bird.class, double.class);
        record.setAccessible(true);
        record.invoke(game, player, dummy, 12.0);

        Method tickCombo = BirdGame3.class.getDeclaredMethod("updateTrainingComboTracker");
        tickCombo.setAccessible(true);
        for (int i = 0; i < 90; i++) {
            tickCombo.invoke(game);
        }

        assertEquals(0, getPrivateInt(game, "trainingComboHits"));
        assertEquals(0.0, getPrivateDouble(game, "trainingComboDamage"), 0.0001);
        assertEquals(12.0, getPrivateDouble(game, "trainingSessionDamage"), 0.0001);
        assertEquals(12.0, getPrivateDouble(game, "trainingLastHitDamage"), 0.0001);
    }

    @Test
    void academyProvidesDedicatedDrillForEveryBird() throws Exception {
        BirdGame3 game = new BirdGame3();
        Class<?> lessonClass = Class.forName("com.example.birdgame3.BirdGame3$GuidedTutorialLesson");
        Method drillForBird = BirdGame3.class.getDeclaredMethod(
                "trainingAcademyDrillLessonFor", BirdGame3.BirdType.class);
        Method birdForDrill = BirdGame3.class.getDeclaredMethod(
                "trainingAcademyDrillBirdFor", lessonClass);
        drillForBird.setAccessible(true);
        birdForDrill.setAccessible(true);

        for (BirdGame3.BirdType type : BirdGame3.BirdType.values()) {
            Object lesson = drillForBird.invoke(game, type);
            assertNotNull(lesson, type + " should have a dedicated Academy drill");
            assertEquals(type, birdForDrill.invoke(game, lesson),
                    type + " drill should map back to its roster bird");
        }
    }

    @Test
    void pigeonAcademyDrillTracksEveryRooftopRouteHit() throws Exception {
        BirdGame3 game = guidedAcademyGame("PIGEON_DRILL");
        Bird pigeon = new Bird(100.0, BirdGame3.BirdType.PIGEON, 0, game);
        Bird dummy = new Bird(220.0, BirdGame3.BirdType.EAGLE, 1, game);
        Method recordHit = BirdGame3.class.getDeclaredMethod(
                "recordTrainingCharacterDrillHit", Bird.class, Bird.class);
        recordHit.setAccessible(true);

        pigeon.pigeonFeatherBurstTimer = 1;
        recordHit.invoke(game, pigeon, dummy);
        pigeon.pigeonFeatherBurstTimer = 0;
        pigeon.pigeonRushTimer = 1;
        recordHit.invoke(game, pigeon, dummy);
        pigeon.pigeonRushTimer = 0;
        pigeon.pigeonFlutterTimer = 1;
        recordHit.invoke(game, pigeon, dummy);
        pigeon.pigeonFlutterTimer = 0;
        pigeon.pigeonScavengeTimer = 1;
        pigeon.pigeonScavengeAirborne = true;
        recordHit.invoke(game, pigeon, dummy);

        assertTrue(getPrivateBoolean(game, "trainingAcademyPigeonBurstHitSeen"));
        assertTrue(getPrivateBoolean(game, "trainingAcademyPigeonRushHitSeen"));
        assertTrue(getPrivateBoolean(game, "trainingAcademyPigeonFlutterHitSeen"));
        assertTrue(getPrivateBoolean(game, "trainingAcademyPigeonDropPeckHitSeen"));
        assertTrue(invokePrivateBoolean(game, "hasCompletedPigeonTrainingDrill"));
    }

    @Test
    void eagleAcademyDrillTracksEveryAirControlHit() throws Exception {
        BirdGame3 game = guidedAcademyGame("EAGLE_DRILL");
        Bird eagle = new Bird(100.0, BirdGame3.BirdType.EAGLE, 0, game);
        Bird dummy = new Bird(220.0, BirdGame3.BirdType.PIGEON, 1, game);
        Method recordHit = BirdGame3.class.getDeclaredMethod(
                "recordTrainingCharacterDrillHit", Bird.class, Bird.class);
        recordHit.setAccessible(true);

        eagle.raptorCryTimer = 1;
        recordHit.invoke(game, eagle, dummy);
        eagle.raptorCryTimer = 0;
        eagle.raptorRushTimer = 1;
        recordHit.invoke(game, eagle, dummy);
        eagle.raptorRushTimer = 0;
        eagle.raptorClimbTimer = 1;
        recordHit.invoke(game, eagle, dummy);
        eagle.raptorClimbTimer = 0;
        eagle.eagleDiveActive = true;
        recordHit.invoke(game, eagle, dummy);

        assertTrue(getPrivateBoolean(game, "trainingAcademyEagleCryHitSeen"));
        assertTrue(getPrivateBoolean(game, "trainingAcademyEagleRushHitSeen"));
        assertTrue(getPrivateBoolean(game, "trainingAcademyEagleClimbHitSeen"));
        assertTrue(getPrivateBoolean(game, "trainingAcademyEagleDiveHitSeen"));
        assertTrue(invokePrivateBoolean(game, "hasCompletedEagleTrainingDrill"));
    }

    @Test
    void gooseAcademyDrillTeachesNestTerritoryAndChargedHonk() throws Exception {
        BirdGame3 game = guidedAcademyGame("GOOSE_DRILL");
        Bird goose = new Bird(100.0, BirdGame3.BirdType.GOOSE, 0, game);
        Bird dummy = new Bird(220.0, BirdGame3.BirdType.PIGEON, 1, game);

        Method prepare = BirdGame3.class.getDeclaredMethod(
                "prepareGuidedTutorialLessonResources", Bird.class);
        prepare.setAccessible(true);
        prepare.invoke(game, goose);
        assertEquals(Bird.GOOSE_TERRITORY_MAX - 8.0, goose.gooseTerritoryMeter, 0.0001);

        goose.gooseNest = new GooseSpecials.GooseNest(goose.bodyCenterX(), goose.bodyBottomY(), false);
        goose.gooseTerritoryMeter = 18.0;
        Method update = BirdGame3.class.getDeclaredMethod("updateGooseTrainingDrill", Bird.class);
        update.setAccessible(true);
        update.invoke(game, goose);
        assertEquals(Bird.GOOSE_TERRITORY_MAX, goose.gooseTerritoryMeter, 0.0001,
                "Planting the lesson nest should finish the setup meter");

        goose.gooseHonkTimer = 1;
        goose.gooseHonkReleased = true;
        goose.gooseHonkEmpowered = true;
        Method recordHit = BirdGame3.class.getDeclaredMethod(
                "recordTrainingCharacterDrillHit", Bird.class, Bird.class);
        recordHit.setAccessible(true);
        goose.gooseHonkHoldFrames = Bird.GOOSE_HONK_MAX_HOLD_FRAMES - 1;
        recordHit.invoke(game, goose, dummy);
        assertFalse(getPrivateBoolean(game, "trainingAcademyGooseChargedHonkHitSeen"),
                "A partially charged Honk should not clear the final goal");

        goose.gooseHonkHoldFrames = Bird.GOOSE_HONK_MAX_HOLD_FRAMES;
        recordHit.invoke(game, goose, dummy);

        assertTrue(getPrivateBoolean(game, "trainingAcademyGooseNestPlacedSeen"));
        assertTrue(getPrivateBoolean(game, "trainingAcademyGooseTerritoryReadySeen"));
        assertTrue(getPrivateBoolean(game, "trainingAcademyGooseChargedHonkHitSeen"));
        assertTrue(invokePrivateBoolean(game, "hasCompletedGooseTrainingDrill"));
    }

    @Test
    void academyTrainingRosterUsesGuidedLessonBirds() throws Exception {
        BirdGame3 game = new BirdGame3();

        Class<?> academyModeClass = Class.forName("com.example.birdgame3.BirdGame3$TrainingAcademyMode");
        Class<?> lessonClass = Class.forName("com.example.birdgame3.BirdGame3$GuidedTutorialLesson");
        Object guidedMode = enumConstant(academyModeClass, "GUIDED_TUTORIAL");
        Object recoveryLesson = enumConstant(lessonClass, "RECOVERY_AND_LEDGE");

        Method setupRoster = BirdGame3.class.getDeclaredMethod("setupTrainingRoster");
        setupRoster.setAccessible(true);

        setPrivateObject(game, "trainingAcademyMode", guidedMode);
        setPrivateObject(game, "guidedTutorialLesson", recoveryLesson);
        setupRoster.invoke(game);

        assertEquals(BirdGame3.BirdType.PENGUIN, game.players[0].type);
        assertEquals(BirdGame3.BirdType.PIGEON, game.players[1].type);
    }

    @Test
    void resetTrainingPositionsRebuildsFreshRosterAtBattleSpawns() throws Exception {
        BirdGame3 game = new BirdGame3();
        game.trainingModeActive = true;

        Method setupRoster = BirdGame3.class.getDeclaredMethod("setupTrainingRoster");
        setupRoster.setAccessible(true);
        setupRoster.invoke(game);

        Method positionBattlefieldSpawns = BirdGame3.class.getDeclaredMethod("positionBattlefieldSpawns");
        positionBattlefieldSpawns.setAccessible(true);
        positionBattlefieldSpawns.invoke(game);

        Method captureSpawns = BirdGame3.class.getDeclaredMethod("captureTrainingSpawns");
        captureSpawns.setAccessible(true);
        captureSpawns.invoke(game);

        Bird originalPlayer = game.players[0];
        Bird originalDummy = game.players[1];
        double capturedPlayerX = originalPlayer.x;
        double capturedDummyX = originalDummy.x;
        originalPlayer.x = 999.0;
        originalPlayer.health = 17.0;
        originalDummy.x = 888.0;
        originalDummy.health = 6.0;

        Method resetPositions = BirdGame3.class.getDeclaredMethod("resetTrainingPositions");
        resetPositions.setAccessible(true);
        resetPositions.invoke(game);

        assertNotSame(game.players[0], originalPlayer);
        assertNotSame(game.players[1], originalDummy);
        assertEquals(capturedPlayerX, game.players[0].x, 0.0001);
        assertEquals(capturedDummyX, game.players[1].x, 0.0001);
        assertEquals(Bird.STARTING_HEALTH, game.players[0].health, 0.0001);
        assertEquals(Bird.STARTING_HEALTH, game.players[1].health, 0.0001);
    }

    @Test
    void trainingRefillRestoresHealthCooldownsMovementAndUltimate() {
        BirdGame3 game = new BirdGame3();
        Bird bird = new Bird(100.0, BirdGame3.BirdType.PIGEON, 0, game);
        bird.health = 14.0;
        bird.attackCooldown = 9;
        bird.specialCooldown = 45;
        bird.vx = 6.0;
        bird.vy = -5.0;

        bird.refillTrainingResources(true);

        assertEquals(Bird.STARTING_HEALTH, bird.health, 0.0001);
        assertEquals(0, bird.attackCooldown);
        assertEquals(0, bird.specialCooldown);
        assertEquals(0.0, bird.vx, 0.0001);
        assertEquals(0.0, bird.vy, 0.0001);
        assertTrue(bird.isUltimateReady());
    }

    @Test
    void roadrunnerUltimateCatchesIntoRedlineExecutionCutscene() throws Exception {
        BirdGame3 game = new BirdGame3();
        game.activePlayers = 2;

        Bird runner = new Bird(300.0, BirdGame3.BirdType.ROADRUNNER, 0, game);
        Bird target = new Bird(430.0, BirdGame3.BirdType.PIGEON, 1, game);
        runner.y = BirdGame3.GROUND_Y - runner.bodyHeight();
        target.y = runner.y;
        game.players[0] = runner;
        game.players[1] = target;

        setPrivateDouble(runner, "ultimateMeter", 100.0);
        double startingHealth = target.health;

        invokePrivateVoid(runner, "special");

        assertEquals(Bird.ROADRUNNER_REDLINE_DASH_FRAMES,
                getPrivateInt(runner, "roadrunnerRedlineTimer"));
        assertEquals(0, getPrivateInt(runner, "roadrunnerSandstormTimer"),
                "Roadrunner ultimate should no longer start the old sandstorm.");
        assertFalse(runner.isUltimateReady());

        for (int i = 0; i < 4; i++) {
            runner.update(1.0);
        }

        assertTrue(getPrivateBoolean(runner, "roadrunnerRedlineCinematic"),
                "A caught target should trigger the Redline cinematic.");
        assertTrue(runner.isCombatInvulnerable(),
                "Roadrunner should only gain ult invulnerability after the catch connects.");

        for (int i = 0; i < Bird.ROADRUNNER_REDLINE_FINAL_FRAME + 8; i++) {
            runner.update(1.0);
        }

        assertTrue(getPrivateBoolean(runner, "roadrunnerRedlineFinalResolved"),
                "Redline Execution should resolve a single final launch.");
        assertTrue(target.health <= startingHealth - 45.0,
                "The full caught ultimate should deal immense damage.");
        assertTrue(Math.abs(target.vx) > 20.0 && target.vy < -15.0,
                "The final hit should launch the caught target hard.");
    }

    @Test
    void roadrunnerUltimateWhiffSpendsMeterWithoutCutsceneDamage() throws Exception {
        BirdGame3 game = new BirdGame3();
        game.activePlayers = 2;

        Bird runner = new Bird(300.0, BirdGame3.BirdType.ROADRUNNER, 0, game);
        Bird target = new Bird(260.0, BirdGame3.BirdType.PIGEON, 1, game);
        runner.y = BirdGame3.GROUND_Y - runner.bodyHeight();
        target.y = runner.y - 260.0;
        game.players[0] = runner;
        game.players[1] = target;

        setPrivateDouble(runner, "ultimateMeter", 100.0);
        double startingHealth = target.health;

        invokePrivateVoid(runner, "special");
        assertFalse(runner.isUltimateReady());
        assertFalse(runner.isCombatInvulnerable(),
                "The initial lunge should still be punishable before a catch.");

        for (int i = 0; i < Bird.ROADRUNNER_REDLINE_DASH_FRAMES + 3; i++) {
            runner.update(1.0);
        }

        assertFalse(getPrivateBoolean(runner, "roadrunnerRedlineCinematic"));
        assertEquals(startingHealth, target.health, 0.0001);
        assertTrue(getPrivateInt(runner, "roadrunnerRedlineRecoveryTimer") > 0,
                "A whiff should exit into a short recovery instead of a cutscene.");
    }

    @Test
    void pelicanEmptyBilgeTapLoadsOneCargoAndHoldLoadsTwo() throws Exception {
        BirdGame3 game = new BirdGame3();
        game.activePlayers = 1;

        Bird pelican = new Bird(260.0, BirdGame3.BirdType.PELICAN, 0, game);
        pelican.y = BirdGame3.GROUND_Y - 96.0;
        game.players[0] = pelican;

        KeyCode specialKey = game.specialKeyForPlayer(0);
        KeyCode blockKey = game.blockKeyForPlayer(0);

        game.setLocalActionsForKey(blockKey, true);
        game.setLocalActionsForKey(specialKey, true);
        pelican.update(1.0);
        game.setLocalActionsForKey(specialKey, false);
        game.setLocalActionsForKey(blockKey, false);
        pelican.update(1.0);

        assertEquals(1, getPrivateInt(pelican, "pelicanCargoCount"));

        setPrivateInt(pelican, "pelicanCargoCount", 0);
        setPrivateInt(pelican, "pelicanDownReuseTimer", 0);
        pelican.update(1.0);

        game.setLocalActionsForKey(blockKey, true);
        game.setLocalActionsForKey(specialKey, true);
        pelican.update(1.0);
        for (int i = 0; i < 24; i++) {
            pelican.update(1.0);
        }

        assertEquals(2, getPrivateInt(pelican, "pelicanCargoCount"));
    }

    @Test
    void pelicanUltimateStartsMaelstromGulletInsteadOfBoostedSpecial() throws Exception {
        BirdGame3 game = new BirdGame3();
        game.activePlayers = 2;

        Bird pelican = new Bird(260.0, BirdGame3.BirdType.PELICAN, 0, game);
        Bird target = new Bird(380.0, BirdGame3.BirdType.PIGEON, 1, game);
        pelican.y = BirdGame3.GROUND_Y - pelican.bodyHeight();
        target.y = BirdGame3.GROUND_Y - target.bodyHeight();
        game.players[0] = pelican;
        game.players[1] = target;

        setPrivateInt(pelican, "pelicanCargoCount", 2);
        setPrivateDouble(pelican, "ultimateMeter", 100.0);
        invokePrivateVoid(pelican, "special");

        assertEquals(Bird.PELICAN_MAELSTROM_FRAMES, getPrivateInt(pelican, "pelicanMaelstromTimer"));
        assertEquals(2, getPrivateInt(pelican, "pelicanMaelstromCargoSpent"));
        assertEquals(0, getPrivateInt(pelican, "pelicanCargoCount"));
        assertEquals(0, getPrivateInt(pelican, "pelicanFullHoldTimer"),
                "Pelican ultimate should not open Full Hold anymore.");
        assertEquals(0, getPrivateInt(pelican, "pelicanNeutralTimer"),
                "Pelican ultimate should not fall through into boosted Pouch Snare.");
        assertEquals(0, getPrivateInt(pelican, "pelicanSideTimer"));
        assertEquals(0, getPrivateInt(pelican, "pelicanUpTimer"));
        assertFalse(getPrivateBoolean(pelican, "pelicanDownCharging"));
        assertFalse(pelican.isUltimateReady());
        assertEquals(PelicanSpecials.MAELSTROM_GULLET_MOVE, game.lastTelemetryMoveName(0, ""));
    }

    @Test
    void pelicanMaelstromGulletPullsDamagesAndLaunches() throws Exception {
        BirdGame3 game = new BirdGame3();
        game.activePlayers = 2;

        Bird pelican = new Bird(260.0, BirdGame3.BirdType.PELICAN, 0, game);
        Bird target = new Bird(320.0, BirdGame3.BirdType.PIGEON, 1, game);
        pelican.y = BirdGame3.GROUND_Y - pelican.bodyHeight();
        target.y = BirdGame3.GROUND_Y - target.bodyHeight();
        game.players[0] = pelican;
        game.players[1] = target;

        setPrivateInt(pelican, "pelicanCargoCount", 2);
        setPrivateDouble(pelican, "ultimateMeter", 100.0);
        double startingHealth = target.health;
        invokePrivateVoid(pelican, "special");

        for (int i = 0; i < Bird.PELICAN_MAELSTROM_PULL_START_FRAME + 4; i++) {
            pelican.update(1.0);
        }

        assertTrue(target.health < startingHealth,
                "The Maelstrom pull phase should tick damage targets in the pouch zone.");
        assertTrue(target.vx < 0.0, "The vortex should pull the target toward its center.");
        assertTrue(target.stunTime > 0.0, "Targets caught near the center should be briefly pouched.");
        double healthAfterPull = target.health;

        for (int i = 0; i < Bird.PELICAN_MAELSTROM_FINAL_FRAME; i++) {
            pelican.update(1.0);
        }

        assertTrue(getPrivateBoolean(pelican, "pelicanMaelstromFinalResolved"));
        assertTrue(target.health < healthAfterPull,
                "The Maelstrom final geyser should deal a separate heavy hit.");
        assertTrue(target.vy < -10.0, "The Maelstrom final should launch targets upward.");
    }

    @Test
    void pelicanBreakwaterRunSpendsCargoForAHeavyHit() throws Exception {
        BirdGame3 game = new BirdGame3();
        game.activePlayers = 2;

        Bird pelican = new Bird(260.0, BirdGame3.BirdType.PELICAN, 0, game);
        Bird target = new Bird(360.0, BirdGame3.BirdType.PIGEON, 1, game);
        pelican.y = BirdGame3.GROUND_Y - 96.0;
        target.y = BirdGame3.GROUND_Y - 80.0;
        pelican.facingRight = true;
        game.players[0] = pelican;
        game.players[1] = target;

        setPrivateInt(pelican, "pelicanCargoCount", 2);
        double startingHealth = target.health;

        invokePrivateBooleanVoid(pelican, "specialPelicanBreakwaterRun", false);
        double committedSpeed = Math.abs(pelican.vx);
        invokePrivateVoid(pelican, "handlePelicanBreakwaterRun");

        assertEquals(0, getPrivateInt(pelican, "pelicanCargoCount"));
        assertTrue(target.health < startingHealth);
        assertTrue(target.vx > 20.0);
        assertTrue(getPrivateInt(pelican, "pelicanSideTimer") > 5,
                "A confirmed hit must retain Breakwater's original committed action time.");
        assertTrue(Math.abs(pelican.vx) >= committedSpeed,
                "Impact feedback must not secretly change Breakwater's movement commitment.");
        assertTrue(game.hitstopFrames >= 7,
                "A full-cargo Breakwater hit should have a readable heavyweight impact pause.");
        assertTrue(game.shakeIntensity >= 10);
    }

    @Test
    void pelicanBreakwaterHitAddsFeedbackWithoutChangingItsCommitment() throws Exception {
        BirdGame3 game = new BirdGame3();
        game.activePlayers = 2;

        Bird pelican = new Bird(260.0, BirdGame3.BirdType.PELICAN, 0, game);
        Bird target = new Bird(760.0, BirdGame3.BirdType.PIGEON, 1, game);
        pelican.y = BirdGame3.GROUND_Y - pelican.bodyHeight();
        target.y = BirdGame3.GROUND_Y - target.bodyHeight();
        pelican.facingRight = true;
        game.players[0] = pelican;
        game.players[1] = target;

        invokePrivateBooleanVoid(pelican, "specialPelicanBreakwaterRun", false);
        double committedSpeed = Math.abs(pelican.vx);
        invokePrivateVoid(pelican, "handlePelicanBreakwaterRun");

        assertEquals(Bird.PELICAN_SIDE_FRAMES, getPrivateInt(pelican, "pelicanSideTimer"),
                "Missing Breakwater must preserve its full commitment.");
        assertTrue(Math.abs(pelican.vx) >= committedSpeed);
        assertEquals(0, game.hitstopFrames);

        target.x = pelican.x + 70.0;
        invokePrivateVoid(pelican, "handlePelicanBreakwaterRun");

        assertTrue(target.health < Bird.STARTING_HEALTH);
        assertEquals(Bird.PELICAN_SIDE_FRAMES, getPrivateInt(pelican, "pelicanSideTimer"),
                "A hit must not shorten Breakwater's original committed action time.");
        assertEquals(1, getPrivateInt(pelican, "pelicanSideDirection"));
        assertTrue(Math.abs(pelican.vx) >= committedSpeed,
                "A confirmed hit must preserve Breakwater's committed movement.");
        assertTrue(game.hitstopFrames >= 5);
        assertTrue(game.shakeIntensity >= 8);

        invokePrivateVoid(pelican, "handlePelicanBreakwaterRun");
        assertTrue(Math.abs(pelican.vx) >= committedSpeed,
                "Breakwater must remain committed after its impact feedback resolves.");
    }

    @Test
    void pelicanPouchSnareKnocksTargetsAwayAndLoadsCargo() throws Exception {
        BirdGame3 game = new BirdGame3();
        game.activePlayers = 2;

        Bird pelican = new Bird(260.0, BirdGame3.BirdType.PELICAN, 0, game);
        Bird target = new Bird(360.0, BirdGame3.BirdType.PIGEON, 1, game);
        pelican.y = BirdGame3.GROUND_Y - 96.0;
        target.y = BirdGame3.GROUND_Y - 80.0;
        pelican.facingRight = true;
        game.players[0] = pelican;
        game.players[1] = target;

        invokePrivateBooleanVoid(pelican, "specialPelicanPouchSnare", false);
        pelican.update(1.0);

        assertTrue(target.vx > 0.0, "Neutral special should knock targets away from Pelican.");
        assertTrue(target.vy < 0.0, "Neutral special should pop targets upward.");
        assertEquals(1, getPrivateInt(pelican, "pelicanCargoCount"));
    }

    @Test
    void pelicanThermalSailAutomaticallyTurnsIntoKeelDive() throws Exception {
        BirdGame3 game = new BirdGame3();
        game.activePlayers = 1;

        Bird pelican = new Bird(260.0, BirdGame3.BirdType.PELICAN, 0, game);
        pelican.y = BirdGame3.GROUND_Y - 360.0;
        game.players[0] = pelican;

        invokePrivateBooleanVoid(pelican, "specialPelicanThermalSail", false);
        for (int i = 0; i < 18; i++) {
            pelican.update(1.0);
        }

        assertTrue(getPrivateBoolean(pelican, "pelicanKeelDiveActive"),
                "Up special should force the dive after its ascent without extra input.");
        assertTrue(pelican.vy > 0.0, "Forced dive should drive Pelican downward.");

        for (int i = 0; i < 16; i++) {
            pelican.update(1.0);
        }

        assertEquals(0, getPrivateInt(pelican, "pelicanUpTimer"));
        assertTrue(getPrivateBoolean(pelican, "pelicanKeelDiveActive"),
                "The forced dive should keep running until Pelican lands.");
        assertTrue(pelican.vy > 0.0, "Pelican should still be slamming downward after the ascent timer expires.");
    }

    @Test
    void pelicanKeelDiveBouncesOffDockWaterInsteadOfDrowning() throws Exception {
        BirdGame3 game = new BirdGame3();
        game.activePlayers = 1;
        game.selectedMap = BirdGame3.MapType.DOCK;

        Method setupDockArena = BirdGame3.class.getDeclaredMethod("setupDockArena");
        setupDockArena.setAccessible(true);
        setupDockArena.invoke(game);

        Bird pelican = new Bird(3900.0, BirdGame3.BirdType.PELICAN, 0, game);
        pelican.x = 3900.0;
        pelican.y = game.dockWaterSurfaceY() + 18.0;
        pelican.vy = 17.0;
        pelican.pelicanUpTimer = 1;
        pelican.pelicanUpSpecialUsed = true;
        pelican.pelicanKeelDiveActive = true;
        game.players[0] = pelican;

        double startingHealth = pelican.health;
        pelican.update(1.0);

        assertEquals(startingHealth, pelican.health, 0.0001,
                "A keel dive entering water must not count as drowning.");
        assertFalse(pelican.pelicanKeelDiveActive,
                "Water contact should resolve the forced dive.");
        assertEquals(0, pelican.pelicanUpTimer);
        assertTrue(pelican.vy < 0.0,
                "Water impact should bounce Pelican back toward the surface.");
        assertTrue(pelican.y < game.dockWaterSurfaceY(),
                "The splash bounce should leave Pelican above the drowning region.");
    }

    @Test
    void pelicanKeelDiveDamageScalesWithCargo() throws Exception {
        double emptyCargoDamage = pelicanKeelDiveDamageAtCargo(0);
        double fullCargoDamage = pelicanKeelDiveDamageAtCargo(2);

        assertTrue(fullCargoDamage > emptyCargoDamage,
                "More cargo should make the forced landing hit harder.");
    }

    private static double pelicanKeelDiveDamageAtCargo(int cargo) throws Exception {
        BirdGame3 game = new BirdGame3();
        game.activePlayers = 2;

        Bird pelican = new Bird(260.0, BirdGame3.BirdType.PELICAN, 0, game);
        Bird target = new Bird(280.0, BirdGame3.BirdType.PIGEON, 1, game);
        pelican.y = BirdGame3.GROUND_Y - 96.0;
        target.y = BirdGame3.GROUND_Y - 80.0;
        game.players[0] = pelican;
        game.players[1] = target;

        setPrivateInt(pelican, "pelicanCargoCount", cargo);
        double startingHealth = target.health;
        invokePrivateVoid(pelican, "resolvePelicanKeelDiveLanding");
        return startingHealth - target.health;
    }

    @Test
    void pelicanFullHoldPreservesCargoUntilItExpires() throws Exception {
        BirdGame3 game = new BirdGame3();
        game.activePlayers = 1;

        Bird pelican = new Bird(260.0, BirdGame3.BirdType.PELICAN, 0, game);
        pelican.y = BirdGame3.GROUND_Y - 96.0;
        game.players[0] = pelican;

        invokePrivateVoid(pelican, "beginPelicanFullHold");
        assertEquals(2, getPrivateInt(pelican, "pelicanCargoCount"));

        invokePrivateBooleanVoid(pelican, "specialPelicanBreakwaterRun", false);
        assertEquals(2, getPrivateInt(pelican, "pelicanCargoCount"));

        setPrivateInt(pelican, "pelicanFullHoldTimer", 1);
        pelican.update(1.0);

        assertEquals(0, getPrivateInt(pelican, "pelicanCargoCount"));
    }

    @Test
    void ravenReuseLockoutsStayInvisible() throws Exception {
        BirdGame3 game = new BirdGame3();
        game.activePlayers = 1;

        Bird raven = new Bird(180.0, BirdGame3.BirdType.RAVEN, 0, game);
        raven.y = BirdGame3.GROUND_Y - 80.0;
        game.players[0] = raven;

        KeyCode specialKey = game.specialKeyForPlayer(0);
        game.setLocalActionsForKey(specialKey, true);
        raven.update(1.0);
        game.setLocalActionsForKey(specialKey, false);
        raven.update(1.0);

        assertEquals(0, raven.specialCooldown);
        assertTrue(getPrivateInt(raven, "ravenNeutralReuseTimer") > 0);

        game.setLocalActionsForKey(specialKey, true);
        raven.update(1.0);

        assertEquals(0, raven.specialCooldown,
                "Raven specials should use invisible per-move reuse gates.");
    }

    @Test
    void ravenChargedBlackQuillFansIntoThreeProjectiles() throws Exception {
        BirdGame3 game = new BirdGame3();
        game.activePlayers = 1;

        Bird raven = new Bird(180.0, BirdGame3.BirdType.RAVEN, 0, game);
        raven.y = BirdGame3.GROUND_Y - 80.0;
        game.players[0] = raven;

        KeyCode specialKey = game.specialKeyForPlayer(0);
        game.setLocalActionsForKey(specialKey, true);
        for (int i = 0; i < 20; i++) {
            raven.update(1.0);
        }
        game.setLocalActionsForKey(specialKey, false);
        raven.update(1.0);

        List<?> quills = (List<?>) getPrivateObject(raven, "ravenQuills");
        assertEquals(3, quills.size(),
                "Holding Black Quill long enough should release the charged fan.");
        assertFalse(getPrivateBoolean(raven, "ravenQuillCharging"));
    }

    @Test
    void ravenShadowWarpConsumesPortentAndEmpowersSlash() throws Exception {
        BirdGame3 game = new BirdGame3();
        game.activePlayers = 2;

        Bird raven = new Bird(180.0, BirdGame3.BirdType.RAVEN, 0, game);
        Bird target = new Bird(360.0, BirdGame3.BirdType.PIGEON, 1, game);
        raven.y = BirdGame3.GROUND_Y - 80.0;
        target.y = BirdGame3.GROUND_Y - 80.0;
        raven.facingRight = true;
        game.players[0] = raven;
        game.players[1] = target;

        invokePrivateBirdBooleanVoid(raven, target, false);
        assertEquals(Bird.RAVEN_ROUTE_TELL_FRAMES, raven.ravenSideReuseTimer,
                "A fresh Portent should telegraph the route before Shadow Warp is available.");

        game.setLocalActionsForKey(game.rightKeyForPlayer(0), true);
        game.setLocalActionsForKey(game.specialKeyForPlayer(0), true);
        raven.update(1.0);

        assertEquals(0, raven.ravenSideTimer,
                "Shadow Warp should not fire during the Portent reaction window.");
        assertTrue(getPrivateInt(target, "ravenPortentTimer") > 0,
                "A blocked early warp must not consume the Portent.");

        game.setLocalActionsForKey(game.rightKeyForPlayer(0), false);
        game.setLocalActionsForKey(game.specialKeyForPlayer(0), false);
        while (raven.ravenSideReuseTimer > 0) {
            raven.update(1.0);
        }

        double startingHealth = target.health;
        int startingParticles = game.particles.size();
        game.setLocalActionsForKey(game.rightKeyForPlayer(0), true);
        game.setLocalActionsForKey(game.specialKeyForPlayer(0), true);
        raven.update(1.0);

        assertEquals(0, getPrivateInt(target, "ravenPortentTimer"),
                "Shadow Warp should consume the selected Portent.");
        assertTrue(getPrivateBoolean(raven, "ravenSideEmpowered"),
                "Warping through a Portent should empower the slash.");
        assertTrue(raven.x > 240.0,
                "Shadow Warp should relocate Raven near the consumed Portent.");
        assertTrue(target.health < startingHealth,
                "The empowered Shadow Warp should connect after arriving at the Portent.");
        assertTrue(startingHealth - target.health <= 11.0,
                "The normal route payoff should not retain its old oversized damage.");
        assertEquals(20, target.stunTime,
                "The normal route payoff should leave a shorter punish window.");
        assertEquals(Bird.RAVEN_SIDE_REUSE_FRAMES, raven.ravenSideReuseTimer,
                "Shadow Warp should commit Raven to its full reuse window.");
        assertTrue(game.hitstopFrames >= 5,
                "The route payoff should have a brief, readable impact pause.");
        assertTrue(game.shakeIntensity >= 9,
                "The route payoff should feel heavier than an ordinary slash.");
        assertTrue(game.particles.size() > startingParticles,
                "The route payoff should create a concentrated impact burst.");
    }

    @Test
    void ravenPortentSnappedMurderLiftGetsRoutePayoffFeedback() throws Exception {
        BirdGame3 game = new BirdGame3();
        game.activePlayers = 2;

        Bird raven = new Bird(220.0, BirdGame3.BirdType.RAVEN, 0, game);
        Bird target = new Bird(300.0, BirdGame3.BirdType.PIGEON, 1, game);
        raven.y = BirdGame3.GROUND_Y - 80.0;
        target.y = BirdGame3.GROUND_Y - 80.0;
        game.players[0] = raven;
        game.players[1] = target;

        invokePrivateBirdBooleanVoid(raven, target, false);
        double startingHealth = target.health;
        int startingParticles = game.particles.size();

        invokePrivateBooleanVoid(raven, "specialRavenMurderLift", false);
        raven.update(1.0);

        assertTrue(getPrivateBoolean(raven, "ravenLiftSnapped"),
                "Murder Lift should snap through the nearby Portent.");
        assertEquals(0, getPrivateInt(target, "ravenPortentTimer"),
                "The snapped Murder Lift should consume its route point.");
        assertTrue(target.health < startingHealth,
                "The snapped Murder Lift should connect with its routed target.");
        assertTrue(game.hitstopFrames >= 5);
        assertTrue(game.shakeIntensity >= 9);
        assertTrue(game.particles.size() > startingParticles);
    }

    @Test
    void ravenMurderLiftRefreshesAfterLanding() throws Exception {
        BirdGame3 game = new BirdGame3();
        game.activePlayers = 1;

        Bird raven = new Bird(220.0, BirdGame3.BirdType.RAVEN, 0, game);
        raven.y = BirdGame3.GROUND_Y - 240.0;
        game.players[0] = raven;

        invokePrivateBooleanVoid(raven, "specialRavenMurderLift", false);

        assertTrue(getPrivateBoolean(raven, "ravenLiftUsed"));
        assertTrue(raven.vy < -20.0, "Murder Lift should launch upward decisively.");

        raven.y = BirdGame3.GROUND_Y - 80.0;
        raven.vy = 0.0;
        raven.update(1.0);

        assertFalse(getPrivateBoolean(raven, "ravenLiftUsed"),
                "Landing should refresh Raven's once-per-airtime lift.");
    }

    @Test
    void ravenMurderLiftRecoveryTravelIsStrongButBounded() {
        BirdGame3 game = new BirdGame3();
        game.activePlayers = 1;
        Bird raven = new Bird(220.0, BirdGame3.BirdType.RAVEN, 0, game);
        raven.y = 1700.0;
        game.players[0] = raven;

        double startY = raven.y;
        raven.specialRavenMurderLift(false);
        for (int frame = 0; frame < Bird.RAVEN_LIFT_FRAMES; frame++) {
            raven.update(1.0);
        }

        double rise = startY - raven.y;
        assertTrue(rise >= 245.0, "Murder Lift still needs a decisive vertical rescue.");
        assertTrue(rise <= 350.0, "Murder Lift should not erase a deep launch by itself.");
    }

    @Test
    void ravenSustainedFlightSlowsDescentWithoutHoveringForever() {
        BirdGame3 game = new BirdGame3();
        game.activePlayers = 1;
        Bird raven = new Bird(220.0, BirdGame3.BirdType.RAVEN, 0, game);
        raven.y = 1500.0;
        raven.vy = 0.0;
        game.players[0] = raven;
        game.setLocalActionsForKey(game.jumpKeyForPlayer(0), true);

        double startY = raven.y;
        for (int frame = 0; frame < 60; frame++) {
            raven.update(1.0);
        }

        assertTrue(raven.y > startY + 100.0,
                "Held wingbeats should slow Raven's descent without becoming a permanent hover.");
        assertTrue(raven.vy > 4.0, "Raven should eventually keep descending without Murder Lift.");
    }

    @Test
    void ravenAiCommitsMurderLiftWhenRecoveringFromTheVoid() {
        BirdGame3 game = new BirdGame3();
        game.activePlayers = 2;
        game.selectedMap = BirdGame3.MapType.BATTLEFIELD;
        double islandX = 2400.0;
        double islandY = BirdGame3.GROUND_Y - 80.0;
        game.platforms.add(new Platform(islandX, islandY, 1200.0, 70.0));

        Bird raven = new Bird(islandX - 310.0, BirdGame3.BirdType.RAVEN, 0, game);
        raven.y = islandY + 130.0;
        raven.vx = -3.0;
        Bird target = new Bird(islandX + 320.0, BirdGame3.BirdType.PIGEON, 1, game);
        target.y = islandY - target.bodyHeight();
        game.players[0] = raven;
        game.players[1] = target;
        game.isAI[0] = true;

        raven.update(1.0);

        assertTrue(game.isRightPressed(0), "Raven should steer back toward the island.");
        assertTrue(raven.ravenLiftUsed, "Raven should spend Murder Lift once the recovery becomes urgent.");
        assertTrue(raven.ravenLiftTimer > 0);
    }

    @Test
    void ravenNevermorePlacesAndSwapsWithDecoy() throws Exception {
        BirdGame3 game = new BirdGame3();
        game.activePlayers = 1;

        Bird raven = new Bird(220.0, BirdGame3.BirdType.RAVEN, 0, game);
        raven.y = BirdGame3.GROUND_Y - 80.0;
        game.players[0] = raven;

        double originalX = raven.x;
        invokePrivateBooleanVoid(raven, "specialRavenNevermore", false);
        assertNotNull(getPrivateObject(raven, "ravenDecoy"));
        assertEquals(Bird.RAVEN_DOWN_REUSE_FRAMES, raven.ravenDownReuseTimer);

        raven.x = 480.0;
        invokePrivateBooleanVoid(raven, "specialRavenNevermore", false);
        assertEquals(480.0, raven.x, 0.0001,
                "Nevermore must not swap again while its reuse timer is active.");

        game.setLocalActionsForKey(game.blockKeyForPlayer(0), true);
        assertFalse(raven.canStartRavenSpecial(),
                "An existing decoy must not bypass Nevermore's reuse gate.");
        game.setLocalActionsForKey(game.blockKeyForPlayer(0), false);
        while (raven.ravenDownReuseTimer > 0) {
            raven.update(1.0);
        }

        invokePrivateBooleanVoid(raven, "specialRavenNevermore", false);

        assertEquals(originalX, raven.x, 0.0001,
                "Recasting Nevermore should return Raven to the decoy.");
        assertEquals(Bird.RAVEN_DOWN_REUSE_FRAMES, raven.ravenDownReuseTimer,
                "Every Nevermore swap should restart its reuse timer.");
    }

    @Test
    void ravenDownTiltPlantsGroundPortent() throws Exception {
        BirdGame3 game = new BirdGame3();
        game.activePlayers = 1;

        Bird raven = new Bird(180.0, BirdGame3.BirdType.RAVEN, 0, game);
        raven.y = BirdGame3.GROUND_Y - 80.0;
        game.players[0] = raven;

        game.setLocalActionsForKey(game.attackKeyForPlayer(0), true);
        game.setLocalActionsForKey(game.blockKeyForPlayer(0), true);
        raven.update(1.0);
        game.setLocalActionsForKey(game.attackKeyForPlayer(0), false);
        raven.update(1.0);

        List<?> portents = (List<?>) getPrivateObject(raven, "ravenGroundPortents");
        assertEquals(1, portents.size(),
                "Raven's grounded down tilt should seed one Portent.");
    }

    @Test
    void ravenUltimateStagesPortalsRoutesThenVoidRavens() throws Exception {
        BirdGame3 game = new BirdGame3();
        game.activePlayers = 2;

        Bird raven = new Bird(180.0, BirdGame3.BirdType.RAVEN, 0, game);
        Bird target = new Bird(380.0, BirdGame3.BirdType.PIGEON, 1, game);
        raven.y = BirdGame3.GROUND_Y - 80.0;
        target.y = BirdGame3.GROUND_Y - 80.0;
        raven.facingRight = true;
        game.players[0] = raven;
        game.players[1] = target;

        invokePrivateBirdBooleanVoid(raven, target, true);
        double startingHealth = target.health;
        invokePrivateVoid(raven, "specialRavenUnkindness");

        assertTrue(getPrivateInt(raven, "ravenUltimateWindupTimer") > 0,
                "The Unkindness should begin with a portal windup.");
        assertTrue(((List<?>) getPrivateObject(raven, "ravenUltimatePortals")).size() >= 5,
                "The opener should create several visible portals.");
        assertEquals(0, ((List<?>) getPrivateObject(raven, "ravenUltimateRoutes")).size(),
                "The main route strike should wait until after the opener.");
        assertEquals(startingHealth, target.health, 0.0001,
                "The windup should not apply the main route damage immediately.");

        for (int i = 0; i < 32; i++) {
            raven.update(1.0);
        }

        assertFalse(((List<?>) getPrivateObject(raven, "ravenUltimateRoutes")).isEmpty(), "The delayed main strike should create route slashes.");
        assertTrue(target.health < startingHealth,
                "The delayed route strike should damage targets on the route.");

        for (int i = 0; i < 16; i++) {
            raven.update(1.0);
        }

        long ownedVoidRavens = game.crowMinions.stream()
                .filter(crow -> crow.owner == raven && crow.effectiveVariant() == CrowMinion.VARIANT_VOID_RAVEN)
                .count();
        assertTrue(ownedVoidRavens >= 5,
                "The finale should summon a flock of allied void ravens.");
    }

    private static void invokePrivateVoid(Object target, String methodName) throws Exception {
        Method method;
        Object[] args;
        try {
            method = target.getClass().getDeclaredMethod(methodName);
            args = new Object[0];
        } catch (NoSuchMethodException ex) {
            if ("attack".equals(methodName)) {
                method = target.getClass().getDeclaredMethod("performAttack", int.class);
                args = new Object[]{0};
            } else if ("handleVerticalCollision".equals(methodName)) {
                method = target.getClass().getDeclaredMethod(methodName, boolean.class);
                args = new Object[]{false};
            } else {
                throw ex;
            }
        }
        method.setAccessible(true);
        method.invoke(target, args);
        if ("attack".equals(methodName) && target instanceof Bird bird) {
            advanceAuthoredAttackToFirstActiveFrame(bird);
        }
    }

    private static void invokePrivateVoid(Object target, String methodName,
                                          Class<?>[] parameterTypes, Object... args) throws Exception {
        Method method = target.getClass().getDeclaredMethod(methodName, parameterTypes);
        method.setAccessible(true);
        method.invoke(target, args);
    }

    private static Object invokePrivateObjectMethod(Object target, String methodName) throws Exception {
        Method method = target.getClass().getDeclaredMethod(methodName);
        method.setAccessible(true);
        return method.invoke(target);
    }

    private static boolean invokePrivateBoolean(Object target, String methodName) throws Exception {
        Method method = target.getClass().getDeclaredMethod(methodName);
        method.setAccessible(true);
        return (boolean) method.invoke(target);
    }

    private static BirdGame3 guidedAcademyGame(String lessonName) throws Exception {
        BirdGame3 game = new BirdGame3();
        game.trainingModeActive = true;
        Class<?> academyModeClass = Class.forName("com.example.birdgame3.BirdGame3$TrainingAcademyMode");
        Class<?> lessonClass = Class.forName("com.example.birdgame3.BirdGame3$GuidedTutorialLesson");
        setPrivateObject(game, "trainingAcademyMode", enumConstant(academyModeClass, "GUIDED_TUTORIAL"));
        setPrivateObject(game, "guidedTutorialLesson", enumConstant(lessonClass, lessonName));
        return game;
    }

    private static void invokePrivateBooleanVoid(Object target, String methodName, boolean value) throws Exception {
        Method method = target.getClass().getDeclaredMethod(methodName, boolean.class);
        method.setAccessible(true);
        method.invoke(target, value);
    }

    private static void invokePrivateBirdBooleanVoid(Object target, Bird bird, boolean value) throws Exception {
        Method method = target.getClass().getDeclaredMethod("applyRavenPortent", Bird.class, boolean.class);
        method.setAccessible(true);
        method.invoke(target, bird, value);
    }

    private static void invokePrivateIntVoid(Object target) throws Exception {
        Method method = target.getClass().getDeclaredMethod("performAttack", int.class);
        method.setAccessible(true);
        method.invoke(target, 0);
    }

    private static double invokeDoubleMethod(Object target, String methodName) throws Exception {
        Method method = target.getClass().getDeclaredMethod(methodName);
        method.setAccessible(true);
        return ((Number) method.invoke(target)).doubleValue();
    }

    private static double applyPrivateDamage(Bird attacker, Bird target, double rawDamage) throws Exception {
        Method method = Bird.class.getDeclaredMethod("applyDamageTo", Bird.class, double.class);
        method.setAccessible(true);
        return ((Number) method.invoke(attacker, target, rawDamage)).doubleValue();
    }

    private static double attackKnockbackAfterHoldingForFrames(int holdFrames) {
        BirdGame3 game = new BirdGame3();
        game.activePlayers = 2;

        Bird attacker = new Bird(100.0, BirdGame3.BirdType.PIGEON, 0, game);
        Bird target = new Bird(190.0, BirdGame3.BirdType.EAGLE, 1, game);
        attacker.y = BirdGame3.GROUND_Y - 80.0;
        target.y = BirdGame3.GROUND_Y - 80.0;
        attacker.facingRight = true;
        game.players[0] = attacker;
        game.players[1] = target;

        KeyCode rightKey = game.rightKeyForPlayer(0);
        KeyCode attackKey = game.attackKeyForPlayer(0);
        game.setLocalActionsForKey(rightKey, true);
        game.setLocalActionsForKey(attackKey, true);
        for (int i = 0; i < holdFrames; i++) {
            attacker.update(1.0);
        }
        game.setLocalActionsForKey(attackKey, false);
        attacker.update(1.0);
        advanceAuthoredAttackToFirstActiveFrame(attacker);
        return target.vx;
    }

    private static BirdGame3 battlefieldSmashTestGame(
            BirdGame3.BirdType attackerType, BirdGame3.BirdType targetType) throws Exception {
        BirdGame3 game = new BirdGame3();
        game.harnessPrepareMatch(
                attackerType,
                targetType,
                20260808L,
                BirdGame3.MapType.BATTLEFIELD);
        game.isAI[0] = false;
        game.isAI[1] = false;

        Bird attacker = game.players[0];
        Bird target = game.players[1];
        double islandCenterX = getPrivateDouble(game, "battlefieldIslandX")
                + getPrivateDouble(game, "battlefieldIslandW") * 0.5;
        double islandTopY = getPrivateDouble(game, "battlefieldIslandY");
        attacker.x = islandCenterX - 90.0;
        target.x = islandCenterX;
        attacker.y = islandTopY - attacker.bodyHeight();
        target.y = islandTopY - target.bodyHeight();
        attacker.vx = 0.0;
        attacker.vy = 0.0;
        target.vx = 0.0;
        target.vy = 0.0;
        attacker.facingRight = true;
        return game;
    }

    private static void performFullSideSmash(Bird attacker) throws Exception {
        performFullSmash(attacker, "SIDE_SMASH");
    }

    private static void performFullSmash(Bird attacker, String variantName) throws Exception {
        Class<?> variantClass = Class.forName("com.example.birdgame3.Bird$NormalAttackVariant");
        Method performAttack = Bird.class.getDeclaredMethod("performAttack", int.class, variantClass);
        performAttack.setAccessible(true);
        performAttack.invoke(attacker, 60, enumConstant(variantClass, variantName));
        advanceAuthoredAttackToFirstActiveFrame(attacker);
    }

    private static void advanceAuthoredAttackToFirstActiveFrame(Bird attacker) {
        int safetyFrames = 30;
        while (attacker.debugNormalAttackTimelineActive()
                && !attacker.debugAttackBoxActive()
                && safetyFrames-- > 0) {
            attacker.update(1.0);
        }
    }

    private static void advanceLaunchedBird(Bird target, int frames) {
        for (int frame = 0; frame < frames; frame++) {
            target.update(1.0);
        }
    }

    private static double launchVelocityAfterGroundJump(int heldFrames) {
        BirdGame3 game = new BirdGame3();
        game.activePlayers = 1;

        Bird bird = new Bird(190.0, BirdGame3.BirdType.EAGLE, 0, game);
        bird.y = BirdGame3.GROUND_Y - 80.0;
        game.players[0] = bird;

        KeyCode jumpKey = game.jumpKeyForPlayer(0);
        game.setLocalActionsForKey(jumpKey, true);
        for (int i = 0; i < heldFrames; i++) {
            bird.update(1.0);
        }
        game.setLocalActionsForKey(jumpKey, false);
        for (int i = heldFrames; i < 3; i++) {
            bird.update(1.0);
        }
        return Math.abs(bird.vy);
    }

    private static List<ChickMinion> ownedChicks(BirdGame3 game, Bird owner) {
        return game.chickMinions.stream()
                .filter(chick -> chick.owner == owner && chick.life > 0)
                .toList();
    }

    @Test
    void jumpPressedJustBeforeLandingIsBuffered() throws Exception {
        BirdGame3 game = new BirdGame3();
        game.activePlayers = 1;

        Bird bird = new Bird(190.0, BirdGame3.BirdType.EAGLE, 0, game);
        game.players[0] = bird;

        bird.y = BirdGame3.GROUND_Y - 200.0;
        game.setLocalActionsForKey(game.jumpKeyForPlayer(0), true);
        bird.update(1.0);
        game.setLocalActionsForKey(game.jumpKeyForPlayer(0), false);

        assertTrue(getPrivateInt(bird, "jumpBufferFrames") > 0,
                "An airborne jump press should be buffered.");

        bird.y = BirdGame3.GROUND_Y - 80.0;
        bird.vy = 0.0;
        bird.update(1.0);

        assertTrue(getPrivateInt(bird, "jumpSquatTimer") > 0 || bird.vy < 0,
                "A buffered jump should fire on landing.");
        assertEquals(0, getPrivateInt(bird, "jumpBufferFrames"),
                "The jump buffer should be consumed by the buffered jump.");
    }

    @Test
    void coyoteTimeAllowsJumpJustAfterLeavingLedge() throws Exception {
        BirdGame3 game = new BirdGame3();
        game.activePlayers = 1;

        Bird bird = new Bird(190.0, BirdGame3.BirdType.EAGLE, 0, game);
        game.players[0] = bird;

        bird.y = BirdGame3.GROUND_Y - 80.0;
        bird.update(1.0);
        assertTrue(getPrivateInt(bird, "coyoteFrames") > 0,
                "Standing on the ground should refresh coyote time.");

        bird.y = BirdGame3.GROUND_Y - 200.0;
        game.setLocalActionsForKey(game.jumpKeyForPlayer(0), true);
        bird.update(1.0);
        game.setLocalActionsForKey(game.jumpKeyForPlayer(0), false);

        assertTrue(bird.vy < 0, "Jumping within the coyote window should launch a full jump.");
        assertEquals(0, getPrivateInt(bird, "coyoteFrames"),
                "Coyote time should be consumed by the jump.");
    }

    @Test
    void jumpPressedWithSpecialHeldIsNotBuffered() throws Exception {
        BirdGame3 game = new BirdGame3();
        game.activePlayers = 1;

        Bird bird = new Bird(190.0, BirdGame3.BirdType.EAGLE, 0, game);
        game.players[0] = bird;

        bird.y = BirdGame3.GROUND_Y - 200.0;
        game.setLocalActionsForKey(game.jumpKeyForPlayer(0), true);
        game.setLocalActionsForKey(game.specialKeyForPlayer(0), true);
        bird.update(1.0);
        game.setLocalActionsForKey(game.specialKeyForPlayer(0), false);
        game.setLocalActionsForKey(game.jumpKeyForPlayer(0), false);

        assertEquals(0, getPrivateInt(bird, "jumpBufferFrames"),
                "Jump pressed together with special is a special combo and must not buffer a jump.");
    }

    @Test
    void attackPressedDuringCooldownIsBufferedNearExpiry() throws Exception {
        BirdGame3 game = new BirdGame3();
        game.activePlayers = 1;

        Bird bird = new Bird(190.0, BirdGame3.BirdType.EAGLE, 0, game);
        game.players[0] = bird;
        bird.y = BirdGame3.GROUND_Y - 80.0;

        setPrivateInt(bird, "attackCooldown", 5);
        game.setLocalActionsForKey(game.attackKeyForPlayer(0), true);
        bird.update(1.0);
        game.setLocalActionsForKey(game.attackKeyForPlayer(0), false);

        assertTrue(getPrivateInt(bird, "attackBufferFrames") > 0,
                "An attack press during cooldown should be buffered.");

        for (int i = 0; i < 6 && getPrivateInt(bird, "attackCooldown") > 0; i++) {
            bird.update(1.0);
        }
        bird.update(1.0);

        assertTrue(getPrivateInt(bird, "attackCooldown") > 0,
                "The buffered attack should fire once the cooldown expires.");
        assertEquals(0, getPrivateInt(bird, "attackBufferFrames"),
                "The attack buffer should be consumed when the attack fires.");
    }

    private static void setPrivateInt(Object target, String fieldName, int value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.setInt(target, value);
    }

    private static int getPrivateCollectionSize(Object target, String fieldName) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        Object value = field.get(target);
        return value instanceof java.util.Collection<?> collection ? collection.size() : -1;
    }

    private static void setPrivateBoolean(Object target) throws Exception {
        Field field = target.getClass().getDeclaredField("smashCombatRulesActive");
        field.setAccessible(true);
        field.setBoolean(target, true);
    }

    private static void setPrivateDouble(Object target, String fieldName, double value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.setDouble(target, value);
    }

    private static void setPrivateObject(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }

    private static int getPrivateInt(Object target, String fieldName) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        return field.getInt(target);
    }

    private static double getPrivateDouble(Object target, String fieldName) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        return field.getDouble(target);
    }

    private static boolean getPrivateBoolean(Object target, String fieldName) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        return field.getBoolean(target);
    }

    @Test
    void gooseHonkTapKeepsAReadableMinimumTell() {
        BirdGame3 game = new BirdGame3();
        game.activePlayers = 2;

        Bird goose = new Bird(100.0, BirdGame3.BirdType.GOOSE, 0, game);
        Bird target = new Bird(220.0, BirdGame3.BirdType.PIGEON, 1, game);
        goose.y = BirdGame3.GROUND_Y - 80.0;
        target.y = BirdGame3.GROUND_Y - 80.0;
        goose.facingRight = true;
        game.players[0] = goose;
        game.players[1] = target;

        GooseSpecials.neutral(goose, false);
        double startingHealth = target.health;
        for (int frame = 1; frame < Bird.GOOSE_HONK_MIN_HOLD_FRAMES; frame++) {
            GooseSpecials.handleState(goose, false);
            assertFalse(goose.gooseHonkReleased,
                    "A released button should not skip the honk's minimum startup tell.");
            assertEquals(startingHealth, target.health, 0.0001);
        }

        GooseSpecials.handleState(goose, false);

        assertTrue(goose.gooseHonkReleased);
        assertTrue(target.health < startingHealth);
        assertTrue(goose.gooseHonkTimer <= Bird.GOOSE_HONK_RECOVERY_FRAMES,
                "Releasing early should transition into the fixed recovery instead of retaining unused charge time.");
    }

    @Test
    void chargedGooseHonkIsStrongerThanTappedHonk() {
        GooseHonkOutcome tapped = playGooseHonk(Bird.GOOSE_HONK_MIN_HOLD_FRAMES);
        GooseHonkOutcome charged = playGooseHonk(Bird.GOOSE_HONK_MAX_HOLD_FRAMES);

        assertTrue(charged.damage > tapped.damage,
                "Holding the honk should earn meaningfully more damage.");
        assertTrue(charged.horizontalLaunch > tapped.horizontalLaunch * 1.7,
                "The strongest honk launch should require a committed charge.");
        assertTrue(charged.stunFrames > tapped.stunFrames + 3.0,
                "The charged honk should retain payoff without giving the tapped version long stun.");
    }

    @Test
    void chargedGooseHonkEarnsItsKoLaunchFromDefenderDamage() throws Exception {
        GooseHonkOutcome freshTarget = playSmashGooseHonk(0.0);
        GooseHonkOutcome damagedTarget = playSmashGooseHonk(150.0);

        assertTrue(damagedTarget.horizontalLaunch > freshTarget.horizontalLaunch * 1.8,
                "Honk should become a real finisher through the shared damage-scaled launch curve.");
        assertTrue(damagedTarget.stunFrames > freshTarget.stunFrames,
                "High-percent honk launch should receive the shared launch hitstun needed to finish.");
    }

    @Test
    void fullyChargedGooseHonkOutlaunchesHisQuickNormal() throws Exception {
        GooseHonkOutcome normal = playSmashGooseNormalAttack(0.0);
        GooseHonkOutcome chargedHonk = playSmashGooseHonk(0.0);

        assertTrue(chargedHonk.horizontalLaunch > normal.horizontalLaunch * 1.05,
                () -> "A close fully charged honk should have visibly more launch than Goose's quick normal attack"
                        + " (honk=" + chargedHonk.horizontalLaunch
                        + ", normal=" + normal.horizontalLaunch + ").");
    }

    @Test
    void fullyChargedGooseHonkDoesNotTakeAZeroPercentBattlefieldStock() throws Exception {
        BirdGame3 game = battlefieldSmashTestGame(BirdGame3.BirdType.GOOSE, BirdGame3.BirdType.EAGLE);
        Bird goose = game.players[0];
        Bird target = game.players[1];

        GooseSpecials.neutral(goose, false);
        for (int frame = 0; frame < Bird.GOOSE_HONK_MAX_HOLD_FRAMES; frame++) {
            GooseSpecials.handleState(goose, frame + 1 < Bird.GOOSE_HONK_MAX_HOLD_FRAMES);
        }
        advanceLaunchedBird(target, 18);

        assertTrue(target.smashDamagePercent() > 0.0,
                "The charged honk must connect in the Battlefield safety test.");
        assertEquals(3, game.scores[1],
                "A charged honk must not take a zero-percent stock from Battlefield center.");

        game.setLocalActionsForKey(game.leftKeyForPlayer(1), true);
        for (int frame = 0; frame < 60 && game.scores[1] == 3 && target.vx > 0.0; frame++) {
            target.update(1.0);
        }

        assertEquals(3, game.scores[1],
                "A zero-percent defender must have time to steer back after charged honk hitstun.");
        assertTrue(target.vx <= 0.0,
                "A zero-percent defender must be able to reverse charged honk momentum.");
    }

    @Test
    void chargedGooseHonkReleaseBurstOutsizesTappedWhiff() {
        int tappedBurst = playWhiffedGooseHonkReleaseBurst(Bird.GOOSE_HONK_MIN_HOLD_FRAMES);
        int chargedBurst = playWhiffedGooseHonkReleaseBurst(Bird.GOOSE_HONK_MAX_HOLD_FRAMES);

        assertTrue(tappedBurst > 0,
                "Even a whiffed tap Honk should have a readable release shockwave.");
        assertTrue(chargedBurst > tappedBurst,
                "The release shockwave should visually communicate a fully committed charge.");
    }

    @Test
    void gooseHonkLaunchAndStunFallOffAcrossTheCone() {
        BirdGame3 game = new BirdGame3();
        game.activePlayers = 3;

        Bird goose = new Bird(100.0, BirdGame3.BirdType.GOOSE, 0, game);
        Bird nearTarget = new Bird(220.0, BirdGame3.BirdType.PIGEON, 1, game);
        Bird farTarget = new Bird(430.0, BirdGame3.BirdType.PIGEON, 2, game);
        goose.y = BirdGame3.GROUND_Y - 80.0;
        nearTarget.y = BirdGame3.GROUND_Y - 80.0;
        farTarget.y = BirdGame3.GROUND_Y - 80.0;
        goose.facingRight = true;
        game.players[0] = goose;
        game.players[1] = nearTarget;
        game.players[2] = farTarget;

        GooseSpecials.neutral(goose, false);
        for (int frame = 0; frame < Bird.GOOSE_HONK_MAX_HOLD_FRAMES; frame++) {
            GooseSpecials.handleState(goose, true);
        }

        assertTrue(nearTarget.health < Bird.STARTING_HEALTH);
        assertTrue(farTarget.health < Bird.STARTING_HEALTH);
        assertTrue(nearTarget.vx > farTarget.vx * 2.0,
                "Honk should preserve its close-range reward without carrying it to the cone edge.");
        assertTrue(farTarget.vx < 3.2,
                () -> "A normal charged honk at maximum range should reset spacing instead of acting as a safe KO launch"
                        + " (far launch=" + farTarget.vx + ").");
        assertTrue(nearTarget.stunTime > farTarget.stunTime,
                "Honk stun should decay with distance as well as launch.");
    }

    @Test
    void gooseHonkDoesNotStackOntoAlreadyLethalVelocity() {
        BirdGame3 game = new BirdGame3();
        game.activePlayers = 2;

        Bird goose = new Bird(100.0, BirdGame3.BirdType.GOOSE, 0, game);
        Bird target = new Bird(220.0, BirdGame3.BirdType.PIGEON, 1, game);
        goose.y = BirdGame3.GROUND_Y - 80.0;
        target.y = BirdGame3.GROUND_Y - 80.0;
        goose.facingRight = true;
        target.vx = 18.0;
        target.vy = -12.0;
        game.players[0] = goose;
        game.players[1] = target;

        GooseSpecials.neutral(goose, false);
        for (int frame = 0; frame < Bird.GOOSE_HONK_MAX_HOLD_FRAMES; frame++) {
            GooseSpecials.handleState(goose, true);
        }

        assertTrue(target.health < Bird.STARTING_HEALTH);
        assertEquals(18.0, target.vx, 0.0001,
                "Honk must not compound horizontal launch that is already beyond its velocity cap.");
        assertEquals(-12.0, target.vy, 0.0001,
                "Honk must not compound upward launch that is already beyond its velocity cap.");
        assertEquals(9.0, GooseSpecials.cappedHonkVelocity(0.0, 1, 9.0, 13.5), 0.0001,
                "The velocity cap must not weaken a charged close honk against a stationary victim.");
        assertEquals(13.5, GooseSpecials.cappedHonkVelocity(8.0, 1, 9.0, 13.5), 0.0001,
                "A moving victim should receive only the remaining launch budget.");
        assertEquals(-4.0, GooseSpecials.cappedHonkVelocity(5.0, -1, 9.0, 13.5), 0.0001,
                "Honk should still reverse a victim moving toward Goose when below the cap.");
    }

    @Test
    void cpuReactionAndOffenseCadenceImproveGraduallyByLevel() {
        for (int level = 1; level < 9; level++) {
            assertTrue(Bird.aiReactionFramesForLevel(level) > Bird.aiReactionFramesForLevel(level + 1),
                    "Each CPU level should react sooner without becoming frame-perfect.");
            assertTrue(Bird.aiOffenseDecisionIntervalForLevel(level)
                            >= Bird.aiOffenseDecisionIntervalForLevel(level + 1),
                    "Higher CPU levels should reconsider offense at least as quickly.");
        }
        assertTrue(Bird.aiReactionFramesForLevel(9) > 0,
                "Even the strongest CPU must retain a visible reaction delay.");
        assertTrue(Bird.aiOffenseDecisionIntervalForLevel(9) > 1,
                "Even the strongest CPU must commit for more than a single frame.");
    }

    @Test
    void cpuLaunchSurvivalScalesFromNoAssistToIntentionalDi() throws Exception {
        BirdGame3 lowGame = new BirdGame3();
        lowGame.activePlayers = 1;
        setPrivateBoolean(lowGame);
        Bird lowCpu = new Bird(BirdGame3.WORLD_WIDTH - 500.0,
                BirdGame3.BirdType.PIGEON, 0, lowGame);
        lowCpu.y = BirdGame3.GROUND_Y - 500.0;
        lowCpu.vx = 22.0;
        lowCpu.vy = 0.0;
        lowCpu.stunTime = 18.0;
        lowGame.players[0] = lowCpu;
        lowGame.isAI[0] = true;
        ((int[]) getPrivateObject(lowGame, "cpuLevels"))[0] = 2;

        invokePrivateVoid(lowCpu, "aiControl");

        assertFalse(lowGame.isLeftPressed(0));
        assertFalse(lowGame.isRightPressed(0));
        assertFalse(lowGame.isJumpPressed(0));
        assertFalse(lowGame.isBlockPressed(0),
                "Low-level CPUs should not receive hidden launch survival assistance.");

        BirdGame3 highGame = new BirdGame3();
        highGame.activePlayers = 1;
        setPrivateBoolean(highGame);
        Bird highCpu = new Bird(BirdGame3.WORLD_WIDTH - 500.0,
                BirdGame3.BirdType.PIGEON, 0, highGame);
        highCpu.y = BirdGame3.GROUND_Y - 500.0;
        highCpu.vx = 22.0;
        highCpu.vy = 0.0;
        highCpu.stunTime = 18.0;
        highGame.players[0] = highCpu;
        highGame.isAI[0] = true;
        ((int[]) getPrivateObject(highGame, "cpuLevels"))[0] = 9;

        invokePrivateVoid(highCpu, "aiControl");

        assertTrue(highGame.isBlockPressed(0),
                "A high-level CPU should angle a horizontal launch toward the stage surface.");
        assertFalse(highGame.isSpecialPressed(0));
        assertFalse(highGame.isAttackPressed(0));
    }

    @Test
    void highLevelCpuBuffersGroundTechAndLowLevelCpuMissesIt() throws Exception {
        BirdGame3 highGame = new BirdGame3();
        highGame.activePlayers = 1;
        setPrivateBoolean(highGame);
        Bird highCpu = new Bird(320.0, BirdGame3.BirdType.EAGLE, 0, highGame);
        highCpu.y = BirdGame3.GROUND_Y - highCpu.bodyHeight() - 8.0;
        highCpu.vy = 14.0;
        highCpu.stunTime = 12.0;
        setPrivateInt(highCpu, "tumbleTimer", 30);
        highGame.players[0] = highCpu;
        highGame.isAI[0] = true;
        ((int[]) getPrivateObject(highGame, "cpuLevels"))[0] = 9;

        highCpu.update(1.0);

        assertTrue(highCpu.debugHitReactionTelemetryLabel().contains("GROUND"),
                "A high-level CPU should intentionally buffer the imminent landing tech.");
        assertFalse(highCpu.debugHitReactionTelemetryLabel().contains("MISSED"));

        BirdGame3 lowGame = new BirdGame3();
        lowGame.activePlayers = 1;
        setPrivateBoolean(lowGame);
        Bird lowCpu = new Bird(320.0, BirdGame3.BirdType.EAGLE, 0, lowGame);
        lowCpu.y = BirdGame3.GROUND_Y - lowCpu.bodyHeight() - 8.0;
        lowCpu.vy = 14.0;
        lowCpu.stunTime = 12.0;
        setPrivateInt(lowCpu, "tumbleTimer", 30);
        lowGame.players[0] = lowCpu;
        lowGame.isAI[0] = true;
        ((int[]) getPrivateObject(lowGame, "cpuLevels"))[0] = 2;

        lowCpu.update(1.0);

        assertTrue(lowCpu.debugHitReactionTelemetryLabel().contains("MISSED_GROUND"),
                "Low-level CPUs should retain punishable missed-tech states.");
    }

    @Test
    void cpuReleasesHeldDownDiBeforeCreatingATechInputEdge() throws Exception {
        BirdGame3 game = new BirdGame3();
        game.activePlayers = 1;
        setPrivateBoolean(game);
        Bird cpu = new Bird(320.0, BirdGame3.BirdType.EAGLE, 0, game);
        cpu.y = BirdGame3.GROUND_Y - cpu.bodyHeight() - 30.0;
        cpu.vy = 10.0;
        cpu.stunTime = 12.0;
        setPrivateInt(cpu, "tumbleTimer", 30);
        setPrivateObject(cpu, "blockHeldLastFrame", true);
        game.players[0] = cpu;
        game.isAI[0] = true;
        ((int[]) getPrivateObject(game, "cpuLevels"))[0] = 9;

        invokePrivateVoid(cpu, "aiControl");
        assertFalse(game.isBlockPressed(0),
                "Held down-DI must be released before it can become a valid tech press.");

        setPrivateObject(cpu, "blockHeldLastFrame", false);
        invokePrivateVoid(cpu, "aiControl");
        assertTrue(game.isBlockPressed(0),
                "The next deterministic tick should create the fresh Block edge.");
    }

    @Test
    void trainingSurvivalDiPresetAppliesWithoutShieldOrMovementInputs() throws Exception {
        BirdGame3 game = new BirdGame3();
        game.trainingModeActive = true;
        game.activePlayers = 2;
        setPrivateBoolean(game);
        Bird player = new Bird(600.0, BirdGame3.BirdType.PIGEON, 0, game);
        Bird dummy = new Bird(BirdGame3.WORLD_WIDTH - 620.0,
                BirdGame3.BirdType.EAGLE, 1, game);
        dummy.y = BirdGame3.GROUND_Y - 460.0;
        dummy.vx = 18.0;
        dummy.vy = 0.0;
        dummy.stunTime = 16.0;
        dummy.configureTrainingLaunchDefense(true, false);
        game.players[0] = player;
        game.players[1] = dummy;
        setPrivateDouble(dummy, "pendingSmashLaunchScale", 1.5);
        setPrivateDouble(dummy, "pendingDamageScaledHitDamage", 12.0);
        double startX = dummy.x;

        invokePrivateVoid(dummy, "applyPendingSmashLaunch");

        assertTrue(Math.abs(getPrivateDouble(dummy, "lastDirectionalInfluenceDegrees")) > 1.0);
        assertTrue(dummy.x < startX,
                "The preset should SDI an outward hit back toward the playable stage.");
        assertFalse(game.isBlockPressed(1),
                "Training-only DI must not raise a shield and change whether the tested hit connects.");
        assertFalse(game.isLeftPressed(1),
                "Training-only DI must not walk the dummy before the tested hit.");
    }

    @Test
    void trainingAutoTechPresetProducesARealBufferedTech() throws Exception {
        BirdGame3 game = new BirdGame3();
        game.trainingModeActive = true;
        game.activePlayers = 2;
        setPrivateBoolean(game);
        Bird player = new Bird(600.0, BirdGame3.BirdType.PIGEON, 0, game);
        Bird dummy = new Bird(900.0, BirdGame3.BirdType.EAGLE, 1, game);
        dummy.y = BirdGame3.GROUND_Y - dummy.bodyHeight() - 8.0;
        dummy.vy = 14.0;
        dummy.stunTime = 12.0;
        setPrivateInt(dummy, "tumbleTimer", 30);
        dummy.configureTrainingLaunchDefense(false, true);
        game.players[0] = player;
        game.players[1] = dummy;

        dummy.update(1.0);

        assertTrue(dummy.debugHitReactionTelemetryLabel().contains("TECH GROUND"));
        assertFalse(dummy.debugHitReactionTelemetryLabel().contains("MISSED"));
    }

    @Test
    void campaignLaunchPercentUsesAuthoredStartingHealth() throws Exception {
        BirdGame3 game = new BirdGame3();
        game.campaignModeActive = true;
        Bird bird = new Bird(100.0, BirdGame3.BirdType.PIGEON, 1, game);
        double[] startingHealth = (double[]) getPrivateObject(game, "campaignStartingHealth");
        startingHealth[1] = 120.0;

        bird.health = 120.0;
        assertEquals(0.0, game.damageScaledLaunchPercent(bird), 0.0001);
        bird.health = 90.0;
        assertEquals(25.0, game.damageScaledLaunchPercent(bird), 0.0001);
        bird.health = 30.0;
        assertEquals(75.0, game.damageScaledLaunchPercent(bird), 0.0001);
    }

    @Test
    void campaignAllyWalksTowardObjectiveAfterEnemiesAreCleared() throws Exception {
        BirdGame3 game = new BirdGame3();
        game.campaignModeActive = true;
        game.campaignTeamMode = true;
        game.activePlayers = 3;
        StoryCampaign.Mission mission = StoryCampaignContent.create().mission("dead_air");
        StoryMissionController controller = new StoryMissionController(
                mission, StoryCampaign.Difficulty.NORMAL, BirdGame3.WORLD_WIDTH);
        Field controllerField = BirdGame3.class.getDeclaredField("campaignMissionController");
        controllerField.setAccessible(true);
        controllerField.set(game, controller);

        Bird player = new Bird(850.0, BirdGame3.BirdType.PIGEON, 0, game);
        Bird charles = new Bird(1000.0, BirdGame3.BirdType.MOCKINGBIRD, 1, game);
        Bird defeatedEnemy = new Bird(4200.0, BirdGame3.BirdType.RAVEN, 2, game);
        game.players[0] = player;
        game.players[1] = charles;
        game.players[2] = defeatedEnemy;
        game.isAI[1] = true;
        game.isAI[2] = true;
        game.campaignTeams[0] = 1;
        game.campaignTeams[1] = 1;
        game.campaignTeams[2] = 2;

        assertTrue(Double.isNaN(game.campaignObjectiveAssistTargetX(charles)),
                "Allies should keep fighting while an enemy remains.");
        defeatedEnemy.health = 0.0;
        assertEquals(1440.0, game.campaignObjectiveAssistTargetX(charles), 0.0001);

        invokePrivateVoid(charles, "aiControl");

        assertTrue(game.isRightPressed(1),
                "Charles should walk toward the first rooftop vent after combat ends.");
        assertFalse(game.isLeftPressed(1));
    }

    @Test
    void campaignControllerUsesBattlefieldPlayableBounds() throws Exception {
        BirdGame3 game = new BirdGame3();
        setPrivateDouble(game, "battlefieldIslandX", 2400.0);
        setPrivateDouble(game, "battlefieldIslandW", 1200.0);
        setPrivateDouble(game, "battlefieldIslandY", BirdGame3.GROUND_Y - 80.0);
        StoryCampaign.Mission mission = StoryCampaignContent.create().mission("dead_air");

        Method setup = BirdGame3.class.getDeclaredMethod(
                "setupCampaignMissionController", StoryCampaign.Mission.class);
        setup.setAccessible(true);
        setup.invoke(game, mission);

        StoryMissionController controller = (StoryMissionController) getPrivateObject(
                game, "campaignMissionController");
        assertEquals(2688.0, controller.objectiveAssistTargetX(), 0.0001,
                "Battlefield campaign targets must be derived from the main island, not world width.");
        assertEquals(BirdGame3.GROUND_Y - 80.0, controller.objectiveFloorY(), 0.0001);
    }

    @Test
    void harborLockSurvivalKeepsRunningAfterBothEnemiesAreDefeated() throws Exception {
        BirdGame3 game = new BirdGame3();
        game.headlessHarnessMode = true;
        game.campaignModeActive = true;
        game.campaignTeamMode = true;
        game.selectedMap = BirdGame3.MapType.DOCK;
        game.matchTimer = 10_000;

        StoryCampaign.Mission mission = StoryCampaignContent.create().mission("harbor_lock");
        setPrivateObject(game, "currentCampaignMission", mission);
        setPrivateObject(game, "campaignSelectedBird", BirdGame3.BirdType.GOOSE);
        StoryCampaignProgress progress =
                (StoryCampaignProgress) getPrivateObject(game, "stillSkyProgress");
        progress.difficulty = StoryCampaign.Difficulty.EASY;
        setPrivateInt(game, "campaignRetryPhaseIndex", 1);
        Method setupRoster = BirdGame3.class.getDeclaredMethod(
                "setupCampaignMissionRoster", StoryCampaign.Mission.class);
        setupRoster.setAccessible(true);
        setupRoster.invoke(game, mission);
        invokePrivateVoid(game, "setupMatchArenaGeometry");

        StoryMissionController controller =
                (StoryMissionController) getPrivateObject(game, "campaignMissionController");
        Bird goose = game.players[0];
        Bird heisenbird = game.players[1];
        Bird razorbill = game.players[2];
        Method launchBomb = BirdGame3.class.getDeclaredMethod(
                "launchDockShipBomb", Bird.class, Bird.class);
        launchBomb.setAccessible(true);
        launchBomb.invoke(game, goose, heisenbird);
        heisenbird.health = 0.0;
        razorbill.health = 0.0;

        for (int tick = 0; tick < 1_600; tick++) {
            assertTrue(game.harnessTick(), "Harbor Lock should still be running at tick " + tick);
        }

        assertEquals(1, controller.phaseIndex());
        assertFalse(controller.complete());
        assertFalse(controller.failed());
        assertTrue(controller.objectiveProgressRatio() > 0.75);

        javafx.scene.canvas.Canvas canvas =
                new javafx.scene.canvas.Canvas(BirdGame3.WIDTH, BirdGame3.HEIGHT);
        Method drawGame = BirdGame3.class.getDeclaredMethod(
                "drawGame", javafx.scene.canvas.GraphicsContext.class);
        drawGame.setAccessible(true);
        drawGame.invoke(game, canvas.getGraphicsContext2D());

        Method buildHud = BirdGame3.class.getDeclaredMethod("buildFightHudLayout");
        buildHud.setAccessible(true);
        Object hudLayout = buildHud.invoke(game);
        @SuppressWarnings("unchecked")
        java.util.Map<String, javafx.scene.image.WritableImage> portraitCache =
                (java.util.Map<String, javafx.scene.image.WritableImage>)
                        getPrivateObject(game, "fightHudPortraitCache");
        portraitCache.put("GOOSE|", new javafx.scene.image.WritableImage(1, 1));
        portraitCache.put("HEISENBIRD|", new javafx.scene.image.WritableImage(1, 1));
        portraitCache.put("RAZORBILL|", new javafx.scene.image.WritableImage(1, 1));
        Method drawHud = java.util.Arrays.stream(BirdGame3.class.getDeclaredMethods())
                .filter(method -> method.getName().equals("drawFightHud"))
                .findFirst()
                .orElseThrow();
        drawHud.setAccessible(true);
        drawHud.invoke(game, canvas.getGraphicsContext2D(), hudLayout);
    }

    @Test
    void fightHudGraysOnlyFullyEliminatedBirdsInMultifighterMatches() throws Exception {
        BirdGame3 game = new BirdGame3();
        game.activePlayers = 4;
        Bird eliminated = new Bird(100.0, BirdGame3.BirdType.PIGEON, 0, game);
        Bird active = new Bird(200.0, BirdGame3.BirdType.EAGLE, 1, game);
        eliminated.health = 0.0;

        assertTrue(game.shouldGrayOutFightHudPanel(eliminated));
        assertFalse(game.shouldGrayOutFightHudPanel(active));

        game.activePlayers = 2;
        assertFalse(game.shouldGrayOutFightHudPanel(eliminated),
                "The two-player HUD should retain its existing presentation.");

        game.activePlayers = 4;
        setPrivateBoolean(game);
        eliminated.health = eliminated.getMaxHealth();
        game.scores[0] = 0;
        assertTrue(game.shouldGrayOutFightHudPanel(eliminated),
                "A fighter with no stocks left should remain visibly eliminated.");

        eliminated.health = 0.0;
        game.scores[0] = 1;
        assertFalse(game.shouldGrayOutFightHudPanel(eliminated),
                "Losing a stock must not gray the card while the fighter can still respawn.");
    }

    @Test
    void carrionAudienceGuardsStayDownWhenReservedBossEnters() throws Exception {
        BirdGame3 game = new BirdGame3();
        game.headlessHarnessMode = true;
        game.campaignModeActive = true;
        game.campaignTeamMode = true;
        game.selectedMap = BirdGame3.MapType.CAVE;

        StoryCampaign.Mission mission = StoryCampaignContent.create().mission("carrion_audience");
        setPrivateObject(game, "currentCampaignMission", mission);
        setPrivateObject(game, "campaignSelectedBird", BirdGame3.BirdType.PIGEON);
        Method setupRoster = BirdGame3.class.getDeclaredMethod(
                "setupCampaignMissionRoster", StoryCampaign.Mission.class);
        setupRoster.setAccessible(true);
        setupRoster.invoke(game, mission);

        boolean[] bossSlots = (boolean[]) getPrivateObject(game, "campaignBossSlots");
        boolean[] reservedBossSlots =
                (boolean[]) getPrivateObject(game, "campaignReservedBossSlots");
        assertEquals(4, game.activePlayers);
        assertTrue(bossSlots[1]);
        assertTrue(reservedBossSlots[1]);
        assertNull(game.players[1], "Vulture must wait offstage during the guard fight.");

        game.players[2].health = 0.0;
        game.players[3].health = 0.0;
        game.checkCampaignMissionCompletion();

        StoryMissionController controller =
                (StoryMissionController) getPrivateObject(game, "campaignMissionController");
        assertEquals(1, controller.phaseIndex());
        assertNotNull(game.players[1]);
        assertEquals(BirdGame3.BirdType.VULTURE, game.players[1].type);
        assertTrue(game.players[1].health > 0.0);
        assertFalse(reservedBossSlots[1]);
        assertEquals(0.0, game.players[2].health, 0.0001,
                "Carrion Guard must not revive when Vulture enters.");
        assertEquals(0.0, game.players[3].health, 0.0001,
                "Cave Sentry must not revive when Vulture enters.");
    }

    @Test
    void campaignGauntletLeavesEveryDefeatedActorDown() throws Exception {
        BirdGame3 game = new BirdGame3();
        game.headlessHarnessMode = true;
        game.campaignModeActive = true;
        game.campaignTeamMode = true;
        game.selectedMap = BirdGame3.MapType.BATTLEFIELD;

        StoryCampaign.Mission mission = StoryCampaignContent.create().mission("crown_archive");
        setPrivateObject(game, "currentCampaignMission", mission);
        setPrivateObject(game, "campaignSelectedBird", BirdGame3.BirdType.PENGUIN);
        setPrivateInt(game, "campaignRetryPhaseIndex", 1);
        Method setupRoster = BirdGame3.class.getDeclaredMethod(
                "setupCampaignMissionRoster", StoryCampaign.Mission.class);
        setupRoster.setAccessible(true);
        setupRoster.invoke(game, mission);

        Bird defeatedFalcon = game.players[1];
        Bird defeatedGuard = game.players[2];
        assertEquals(BirdGame3.BirdType.FALCON, defeatedFalcon.type);
        assertEquals(BirdGame3.BirdType.EAGLE, defeatedGuard.type);
        defeatedFalcon.health = 0.0;
        defeatedGuard.health = 0.0;

        game.checkCampaignMissionCompletion();

        assertSame(defeatedFalcon, game.players[1]);
        assertEquals(0.0, game.players[1].health, 0.0001,
                "The defeated authored boss must remain defeated.");
        assertSame(defeatedGuard, game.players[2]);
        assertEquals(0.0, game.players[2].health, 0.0001,
                "The defeated guard must remain defeated too.");
        StoryMissionController controller =
                (StoryMissionController) getPrivateObject(game, "campaignMissionController");
        assertEquals(2, controller.phaseIndex(),
                "The gauntlet should advance instead of replacing defeated enemies.");
    }

    @Test
    void blackoutKeyLeavesVultureAndEveryOtherEnemyDefeatedPermanently()
            throws Exception {
        BirdGame3 game = new BirdGame3();
        game.headlessHarnessMode = true;
        game.campaignModeActive = true;
        game.campaignTeamMode = true;
        game.selectedMap = BirdGame3.MapType.PRISON;

        StoryCampaign.Mission mission = StoryCampaignContent.create().mission("blackout_key");
        setPrivateObject(game, "currentCampaignMission", mission);
        setPrivateObject(game, "campaignSelectedBird", BirdGame3.BirdType.RAVEN);
        setPrivateInt(game, "campaignRetryPhaseIndex", 1);
        Method setupRoster = BirdGame3.class.getDeclaredMethod(
                "setupCampaignMissionRoster", StoryCampaign.Mission.class);
        setupRoster.setAccessible(true);
        setupRoster.invoke(game, mission);

        Bird defeatedAutomaton = game.players[2];
        Bird defeatedVultureEcho = game.players[3];
        assertEquals(BirdGame3.BirdType.EAGLE, defeatedAutomaton.type);
        assertEquals(BirdGame3.BirdType.VULTURE, defeatedVultureEcho.type);
        defeatedAutomaton.receiveExternalDamage(defeatedAutomaton.health + 100.0);
        defeatedVultureEcho.receiveExternalDamage(defeatedVultureEcho.health + 100.0);

        boolean[] eliminated = (boolean[]) getPrivateObject(game, "campaignEnemyEliminated");
        assertTrue(eliminated[2], "A lethal hit must retire the automaton before the next phase tick.");
        assertTrue(eliminated[3], "A lethal hit must retire the echo before the next phase tick.");

        game.checkCampaignMissionCompletion();

        StoryMissionController controller =
                (StoryMissionController) getPrivateObject(game, "campaignMissionController");
        assertEquals(2, controller.phaseIndex());
        assertSame(defeatedAutomaton, game.players[2]);
        assertSame(defeatedVultureEcho, game.players[3]);
        assertEquals(0.0, game.players[2].health, 0.0001);
        assertEquals(0.0, game.players[3].health, 0.0001);

        // Simulate any late character mechanic or phase callback trying to
        // restore the defeated actors after the gauntlet has advanced.
        defeatedAutomaton.health = 112.0;
        defeatedVultureEcho.health = 112.0;
        game.isAI[2] = true;
        game.isAI[3] = true;
        game.checkCampaignMissionCompletion();

        assertEquals(0.0, defeatedAutomaton.health, 0.0001,
                "The Crown automaton cannot revive after its encounter defeat.");
        assertEquals(0.0, defeatedVultureEcho.health, 0.0001,
                "The Vulture echo cannot revive after its encounter defeat.");
        assertFalse(game.isAI[2]);
        assertFalse(game.isAI[3]);
    }

    @Test
    void nullRockFinaleFieldsEveryBirdOnceAsOneCoalition() throws Exception {
        BirdGame3 game = new BirdGame3();
        game.headlessHarnessMode = true;
        game.campaignModeActive = true;
        game.campaignTeamMode = true;
        game.selectedMap = BirdGame3.MapType.BEACON_CROWN;

        StoryCampaign.Mission mission = StoryCampaignContent.create().mission("the_null_rock");
        setPrivateObject(game, "currentCampaignMission", mission);
        setPrivateObject(game, "campaignSelectedBird", BirdGame3.BirdType.PIGEON);
        Method setupRoster = BirdGame3.class.getDeclaredMethod(
                "setupCampaignMissionRoster", StoryCampaign.Mission.class);
        setupRoster.setAccessible(true);
        setupRoster.invoke(game, mission);

        boolean[] present = new boolean[BirdGame3.BirdType.values().length];
        assertEquals(StoryCampaign.STILL_SKY_ROSTER.size(), game.activePlayers);
        for (int slot = 0; slot < game.activePlayers; slot++) {
            Bird bird = game.players[slot];
            assertNotNull(bird, "Every finale slot must contain a coalition bird.");
            assertFalse(present[bird.type.ordinal()], bird.type.name + " appeared twice.");
            present[bird.type.ordinal()] = true;
            assertEquals(1, game.campaignTeams[slot],
                    "The background commander must not consume a fighter slot.");
        }
        for (BirdGame3.BirdType type : StoryCampaign.STILL_SKY_ROSTER) {
            assertTrue(present[type.ordinal()], "The finale omitted " + type.name + '.');
        }
        assertFalse(present[BirdGame3.BirdType.KIWI.ordinal()],
                "Kiwi belongs to a later story and must not be inserted into The Still Sky.");
        assertEquals("music-null-rock.mp3", invokePrivateObjectMethod(game, "gameplayMusicFile"));
    }

    @Test
    void nullRockFinalCheckpointRestartsAsAProtectedOneOnOneBossFight() throws Exception {
        BirdGame3 game = new BirdGame3();
        game.headlessHarnessMode = true;
        game.campaignModeActive = true;
        game.campaignTeamMode = true;
        game.selectedMap = BirdGame3.MapType.BEACON_CROWN;

        StoryCampaign.Mission mission = StoryCampaignContent.create().mission("the_null_rock");
        setPrivateObject(game, "currentCampaignMission", mission);
        setPrivateObject(game, "campaignSelectedBird", BirdGame3.BirdType.PIGEON);
        setPrivateInt(game, "campaignRetryPhaseIndex", 3);
        Method setupRoster = BirdGame3.class.getDeclaredMethod(
                "setupCampaignMissionRoster", StoryCampaign.Mission.class);
        setupRoster.setAccessible(true);
        setupRoster.invoke(game, mission);
        invokePrivateVoid(game, "setupMatchArenaGeometry");
        Method applyArena = BirdGame3.class.getDeclaredMethod(
                "applyCampaignMissionArenaModifiers", StoryCampaign.Mission.class);
        applyArena.setAccessible(true);
        applyArena.invoke(game, mission);

        assertEquals(2, game.activePlayers);
        assertEquals(BirdGame3.BirdType.PIGEON, game.players[0].type);
        assertEquals(1, game.campaignTeams[0]);
        assertEquals(BirdGame3.BirdType.VULTURE, game.players[1].type);
        assertEquals(2, game.campaignTeams[1]);
        assertTrue(game.players[1].isNullRockForm());
        assertTrue(game.isCampaignNullRockDuelBoss(game.players[1]));
        assertEquals(4, game.platforms.size());
        StoryMissionController controller =
                (StoryMissionController) getPrivateObject(game, "campaignMissionController");
        assertEquals(StoryCampaign.ObjectiveType.BOSS_PHASES,
                controller.currentPhase().objective());
        assertEquals(5, controller.currentPhase().targetCount());

        Bird boss = game.players[1];
        double bossStartingHealth = boss.health;
        boss.health = bossStartingHealth * 0.74;
        game.checkCampaignMissionCompletion();
        invokePrivateVoid(game, "applyCampaignMissionRuntimeEffects");
        assertEquals(1, getPrivateInt(game, "campaignNullRockDuelStage"));
        assertTrue(boss.overchargeAttackTimer > 0);

        boss.health = bossStartingHealth * 0.35;
        game.checkCampaignMissionCompletion();
        invokePrivateVoid(game, "applyCampaignMissionRuntimeEffects");
        assertEquals(3, getPrivateInt(game, "campaignNullRockDuelStage"));
        assertTrue(boss.rageTimer > 0);
    }

    @Test
    void cutTheLockBuildsAStagedCommandBridgeOneOnOne() throws Exception {
        BirdGame3 game = new BirdGame3();
        game.headlessHarnessMode = true;
        game.campaignModeActive = true;
        game.campaignTeamMode = true;
        game.selectedMap = BirdGame3.MapType.SKYCLIFFS;

        StoryCampaign.Mission mission = StoryCampaignContent.create().mission("cut_the_lock");
        setPrivateObject(game, "currentCampaignMission", mission);
        setPrivateObject(game, "campaignSelectedBird", BirdGame3.BirdType.RAZORBILL);
        Method setupRoster = BirdGame3.class.getDeclaredMethod(
                "setupCampaignMissionRoster", StoryCampaign.Mission.class);
        setupRoster.setAccessible(true);
        setupRoster.invoke(game, mission);
        invokePrivateVoid(game, "setupMatchArenaGeometry");
        Method applyArena = BirdGame3.class.getDeclaredMethod(
                "applyCampaignMissionArenaModifiers", StoryCampaign.Mission.class);
        applyArena.setAccessible(true);
        applyArena.invoke(game, mission);

        assertEquals(2, game.activePlayers);
        assertEquals(BirdGame3.BirdType.RAZORBILL, game.players[0].type);
        assertEquals(BirdGame3.BirdType.EAGLE, game.players[1].type);
        assertTrue(game.isAI[1]);
        assertEquals(4, game.platforms.size());
        assertEquals(2, game.windVents.size());
        assertTrue(game.usesIslandBoundsForCurrentArena());
        assertEquals(2_200.0, getPrivateDouble(game, "battlefieldIslandW"), 0.0001);
        assertEquals("music-boss.mp3", invokePrivateObjectMethod(game, "gameplayMusicFile"));

        Bird eagle = game.players[1];
        double startingHealth = eagle.health;
        invokePrivateVoid(game, "applyCampaignMissionRuntimeEffects");
        assertEquals(1, getPrivateInt(game, "campaignCrownDuelStage"));

        eagle.health = startingHealth * 0.74;
        invokePrivateVoid(game, "applyCampaignMissionRuntimeEffects");
        assertEquals(2, getPrivateInt(game, "campaignCrownDuelStage"));
        assertTrue(eagle.overchargeAttackTimer >= 100);
        assertEquals(2, game.windVents.size());

        eagle.health = startingHealth * 0.49;
        invokePrivateVoid(game, "applyCampaignMissionRuntimeEffects");
        assertEquals(3, getPrivateInt(game, "campaignCrownDuelStage"));
        assertEquals(3, game.windVents.size());

        eagle.health = startingHealth * 0.24;
        invokePrivateVoid(game, "applyCampaignMissionRuntimeEffects");
        assertEquals(4, getPrivateInt(game, "campaignCrownDuelStage"));
        assertTrue(eagle.rageTimer >= 300);
        assertTrue(eagle.powerMultiplier >= eagle.basePowerMultiplier * 1.15);
        assertTrue(eagle.speedMultiplier >= eagle.baseSpeedMultiplier * 1.10);
    }

    private static GooseHonkOutcome playGooseHonk(int holdFrames) {
        BirdGame3 game = new BirdGame3();
        game.activePlayers = 2;

        Bird goose = new Bird(100.0, BirdGame3.BirdType.GOOSE, 0, game);
        Bird target = new Bird(220.0, BirdGame3.BirdType.PIGEON, 1, game);
        goose.y = BirdGame3.GROUND_Y - 80.0;
        target.y = BirdGame3.GROUND_Y - 80.0;
        goose.facingRight = true;
        game.players[0] = goose;
        game.players[1] = target;

        GooseSpecials.neutral(goose, false);
        for (int frame = 0; frame < holdFrames; frame++) {
            GooseSpecials.handleState(goose, frame + 1 < holdFrames);
        }
        return new GooseHonkOutcome(
                Bird.STARTING_HEALTH - target.health,
                target.vx,
                target.stunTime
        );
    }

    private static GooseHonkOutcome playSmashGooseHonk(double startingPercent) throws Exception {
        BirdGame3 game = new BirdGame3();
        game.activePlayers = 2;
        setPrivateBoolean(game);

        Bird goose = new Bird(100.0, BirdGame3.BirdType.GOOSE, 0, game);
        Bird target = new Bird(190.0, BirdGame3.BirdType.PIGEON, 1, game);
        goose.y = BirdGame3.GROUND_Y - 80.0;
        target.y = BirdGame3.GROUND_Y - 80.0;
        goose.facingRight = true;
        game.players[0] = goose;
        game.players[1] = target;
        setPrivateDouble(target, "smashDamage", startingPercent);

        GooseSpecials.neutral(goose, false);
        for (int frame = 0; frame < Bird.GOOSE_HONK_MAX_HOLD_FRAMES; frame++) {
            GooseSpecials.handleState(goose, frame + 1 < Bird.GOOSE_HONK_MAX_HOLD_FRAMES);
        }
        invokePrivateVoid(target, "applyPendingSmashLaunch");

        return new GooseHonkOutcome(
                target.smashDamagePercent() - startingPercent,
                target.vx,
                target.stunTime
        );
    }

    private static GooseHonkOutcome playSmashGooseNormalAttack(double startingPercent) throws Exception {
        BirdGame3 game = new BirdGame3();
        game.activePlayers = 2;
        setPrivateBoolean(game);

        Bird goose = new Bird(100.0, BirdGame3.BirdType.GOOSE, 0, game);
        Bird target = new Bird(190.0, BirdGame3.BirdType.PIGEON, 1, game);
        goose.y = BirdGame3.GROUND_Y - 80.0;
        target.y = BirdGame3.GROUND_Y - 80.0;
        goose.facingRight = true;
        game.players[0] = goose;
        game.players[1] = target;
        setPrivateDouble(target, "smashDamage", startingPercent);

        invokePrivateVoid(goose, "attack");
        invokePrivateVoid(target, "applyPendingSmashLaunch");

        return new GooseHonkOutcome(
                target.smashDamagePercent() - startingPercent,
                target.vx,
                target.stunTime
        );
    }

    private static int playWhiffedGooseHonkReleaseBurst(int holdFrames) {
        BirdGame3 game = new BirdGame3();
        game.activePlayers = 1;

        Bird goose = new Bird(100.0, BirdGame3.BirdType.GOOSE, 0, game);
        goose.y = BirdGame3.GROUND_Y - 80.0;
        goose.facingRight = true;
        game.players[0] = goose;

        GooseSpecials.neutral(goose, false);
        int particlesBeforeRelease = game.particles.size();
        for (int frame = 0; frame < holdFrames; frame++) {
            GooseSpecials.handleState(goose, frame + 1 < holdFrames);
        }
        assertTrue(goose.gooseHonkReleased);
        return game.particles.size() - particlesBeforeRelease;
    }

    private record GooseHonkOutcome(double damage, double horizontalLaunch, double stunFrames) {
    }

    private static SizeCombatOutcome playSizeCombatExchange(double attackerSizeRatio,
                                                             double targetSizeRatio) throws Exception {
        BirdGame3 game = new BirdGame3();
        game.activePlayers = 2;
        Bird attacker = new Bird(100.0, BirdGame3.BirdType.PIGEON, 0, game);
        Bird target = new Bird(220.0, BirdGame3.BirdType.PIGEON, 1, game);
        game.players[0] = attacker;
        game.players[1] = target;
        attacker.sizeMultiplier = attacker.baseSizeMultiplier * attackerSizeRatio;
        target.sizeMultiplier = target.baseSizeMultiplier * targetSizeRatio;

        double dealtDamage = attacker.applyDamageTo(target, 10.0);
        target.vx += 10.0;
        target.vy -= 8.0;
        invokePrivateVoid(target, "applyPendingSmashLaunch");
        return new SizeCombatOutcome(dealtDamage, Math.hypot(target.vx, target.vy));
    }

    private record SizeCombatOutcome(double damage, double launchSpeed) {
    }

    private static Object getPrivateObject(Object target, String fieldName) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        return field.get(target);
    }

    private static Enum<?> enumConstant(Class<?> enumClass, String name) {
        Object[] constants = enumClass.getEnumConstants();
        if (constants == null) {
            throw new IllegalArgumentException(enumClass.getName() + " is not an enum");
        }
        for (Object constant : constants) {
            if (constant instanceof Enum<?> enumConstant && enumConstant.name().equals(name)) {
                return enumConstant;
            }
        }
        throw new IllegalArgumentException("Missing enum constant " + enumClass.getName() + "." + name);
    }
}
