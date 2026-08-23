package com.example.birdgame3;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;

class NormalAttackTimelineTest {
    @Test
    void pigeonNeutralRespectsStartupActiveAndRecoveryFrames() throws Exception {
        BirdGame3 game = twoBirdGame(BirdGame3.BirdType.PIGEON, BirdGame3.BirdType.EAGLE,
                320.0, 382.0);
        Bird pigeon = game.players[0];
        Bird target = game.players[1];
        double startingHealth = target.health;

        performAttack(pigeon, "NEUTRAL");

        assertTrue(pigeon.debugNormalAttackTimelineActive());
        assertEquals(0, pigeon.debugNormalAttackFrame());
        assertEquals(7, pigeon.debugNormalAttackTotalFrames());
        assertEquals("STARTUP", pigeon.debugNormalAttackPhaseLabel());
        assertEquals(startingHealth, target.health, 0.0001,
                "Starting a Pigeon attack must not resolve damage on the input tick.");

        advanceTimer(pigeon, 2);
        assertEquals(startingHealth, target.health, 0.0001);
        assertEquals("STARTUP", pigeon.debugNormalAttackPhaseLabel());
        assertFalse(pigeon.debugAttackBoxActive());

        advanceTimer(pigeon, 1);
        assertTrue(target.health < startingHealth);
        assertEquals("ACTIVE", pigeon.debugNormalAttackPhaseLabel());
        assertTrue(pigeon.debugAttackBoxActive());
        assertTrue(pigeon.debugNormalAttackConnected());

        double healthAfterFirstActiveFrame = target.health;
        advanceTimer(pigeon, 1);
        assertEquals(healthAfterFirstActiveFrame, target.health, 0.0001,
                "A lingering single-hit normal must not damage the same target twice.");

        advanceTimer(pigeon, 3);
        assertFalse(pigeon.debugNormalAttackTimelineActive());
        assertEquals(0, pigeon.attackAnimationTimer);
        assertTrue(pigeon.attackCooldown > 0,
                "The existing repeat cadence remains explicit after movement recovery ends.");
    }

    @Test
    void whiffStillCommitsPigeonToTheFullRecoveryTimeline() throws Exception {
        BirdGame3 game = twoBirdGame(BirdGame3.BirdType.PIGEON, BirdGame3.BirdType.EAGLE,
                320.0, 1_400.0);
        Bird pigeon = game.players[0];

        performAttack(pigeon, "SIDE_TILT");
        advanceTimer(pigeon, 7);

        assertEquals("RECOVERY", pigeon.debugNormalAttackPhaseLabel());
        assertEquals(3, pigeon.debugNormalAttackRemainingFrames());
        assertTrue(pigeon.attackAnimationTimer > 0);
        assertFalse(pigeon.debugNormalAttackConnected());

        advanceTimer(pigeon, 3);
        assertFalse(pigeon.debugNormalAttackTimelineActive());
        assertEquals(0, pigeon.attackAnimationTimer);
        assertTrue(pigeon.attackCooldown > 0);
    }

    @Test
    void pigeonSideSmashHasDeterministicSweetAndSourZones() throws Exception {
        AttackOutcome sour = sideSmashOutcome(390.0);
        AttackOutcome sweet = sideSmashOutcome(495.0);

        assertTrue(sweet.damage >= sour.damage,
                "The authored beak tip must not deal less damage than the inner hitbox.");
        assertTrue(Math.abs(sweet.horizontalVelocity) > Math.abs(sour.horizontalVelocity),
                "The authored beak tip should launch harder than the sour spot.");
    }

    @Test
    void hitstunInterruptsStartupBeforeItsHitboxCanAppear() throws Exception {
        BirdGame3 game = twoBirdGame(BirdGame3.BirdType.PIGEON, BirdGame3.BirdType.EAGLE,
                320.0, 382.0);
        Bird pigeon = game.players[0];
        Bird target = game.players[1];
        double startingHealth = target.health;

        performAttack(pigeon, "SIDE_SMASH");
        advanceTimer(pigeon, 3);
        pigeon.applyStun(8.0);
        advanceTimer(pigeon, 10);

        assertFalse(pigeon.debugNormalAttackTimelineActive());
        assertEquals(0, pigeon.attackAnimationTimer);
        assertEquals(startingHealth, target.health, 0.0001);
    }

    @Test
    void aerialsAutoCancelBeforeActiveFramesButLandLagDuringTheStrike() throws Exception {
        BirdGame3 earlyGame = airborneTwoBirdGame();
        Bird early = earlyGame.players[0];
        performAttack(early, "NEUTRAL_AIR");
        advanceTimer(early, 2);
        invoke(early, "resolveAerialLandingRecovery");
        assertEquals(0, intField(early, "landingLagTimer"));
        assertFalse(early.debugNormalAttackTimelineActive());

        BirdGame3 activeGame = airborneTwoBirdGame();
        Bird active = activeGame.players[0];
        performAttack(active, "NEUTRAL_AIR");
        advanceTimer(active, 4);
        assertEquals("ACTIVE", active.debugNormalAttackPhaseLabel());
        invoke(active, "resolveAerialLandingRecovery");
        assertEquals(6, intField(active, "landingLagTimer"));
        assertFalse(active.debugNormalAttackTimelineActive());
    }

    @Test
    void nonMigratedBirdsKeepLegacyImmediateResolutionUntilAuthored() throws Exception {
        BirdGame3 game = twoBirdGame(BirdGame3.BirdType.EAGLE, BirdGame3.BirdType.PIGEON,
                320.0, 382.0);
        Bird eagle = game.players[0];
        Bird target = game.players[1];
        double startingHealth = target.health;

        performAttack(eagle, "NEUTRAL");

        assertTrue(target.health < startingHealth);
        assertFalse(eagle.debugNormalAttackTimelineActive());
    }

    @Test
    void activeTimelineAndPerTargetHitHistoryRoundTripThroughLanWireState() throws Exception {
        BirdGame3 game = twoBirdGame(BirdGame3.BirdType.PIGEON, BirdGame3.BirdType.EAGLE,
                320.0, 382.0);
        Bird source = game.players[0];
        performAttack(source, "NEUTRAL");
        advanceTimer(source, 3);

        LanBirdState snapshot = source.toLanState();
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        snapshot.write(new DataOutputStream(bytes));
        LanBirdState decoded = LanBirdState.read(new DataInputStream(
                new ByteArrayInputStream(bytes.toByteArray())));

        Bird restored = groundedBird(game, BirdGame3.BirdType.PIGEON, 0, 320.0);
        restored.applyLanState(decoded);

        assertTrue(restored.debugNormalAttackTimelineActive());
        assertEquals(3, restored.debugNormalAttackFrame());
        assertEquals("ACTIVE", restored.debugNormalAttackPhaseLabel());
        assertTrue(restored.debugNormalAttackConnected());
        assertEquals(snapshot.normalAttackLastHitFrame[1], decoded.normalAttackLastHitFrame[1]);
    }

    @Test
    void hitstopFreezesTheAuthoredFrameAndShieldAdvantageUsesRemainingRecovery() throws Exception {
        BirdGame3 game = twoBirdGame(BirdGame3.BirdType.PIGEON, BirdGame3.BirdType.EAGLE,
                320.0, 382.0);
        game.trainingModeActive = true;
        Bird pigeon = game.players[0];
        Bird defender = game.players[1];
        game.setLocalActionsForKey(game.blockKeyForPlayer(1), true);
        for (int frame = 0; frame < 5; frame++) {
            defender.update(1.0);
        }

        performAttack(pigeon, "NEUTRAL");
        advanceTimer(pigeon, 3);

        assertEquals(Bird.STARTING_HEALTH, defender.health, 0.0001);
        assertTrue(booleanField(game, "trainingShieldAdvantageAvailable"));
        assertEquals(defender.shieldStunFrames - pigeon.debugNormalAttackRemainingFrames(),
                intField(game, "trainingLastShieldAdvantageFrames"));
        assertTrue(game.hitstopFrames > 0);

        int activeFrame = pigeon.debugNormalAttackFrame();
        game.harnessTick();
        assertEquals(activeFrame, pigeon.debugNormalAttackFrame(),
                "Hitstop must consume a fixed game tick without advancing authored attack frames.");
    }

    @Test
    void lateAttackPressBuffersUntilRepeatCooldownEnds() throws Exception {
        BirdGame3 game = twoBirdGame(BirdGame3.BirdType.PIGEON, BirdGame3.BirdType.EAGLE,
                320.0, 1_400.0);
        Bird pigeon = game.players[0];
        performAttack(pigeon, "SIDE_TILT");
        advanceTimer(pigeon, 10);
        assertFalse(pigeon.debugNormalAttackTimelineActive());
        while (pigeon.debugNormalAttackCooldownFrames() > 8) {
            pigeon.update(1.0);
        }
        assertEquals(8, pigeon.debugNormalAttackCooldownFrames());

        game.setLocalActionsForKey(game.attackKeyForPlayer(0), true);
        pigeon.update(1.0);
        assertFalse(pigeon.debugNormalAttackTimelineActive(),
                "A buffered press must not bypass the current move's repeat cooldown.");
        game.setLocalActionsForKey(game.attackKeyForPlayer(0), false);

        for (int frame = 0; frame < 10 && !pigeon.debugNormalAttackTimelineActive(); frame++) {
            pigeon.update(1.0);
        }
        assertTrue(pigeon.debugNormalAttackTimelineActive(),
                "The buffered normal should begin on the first actionable fixed tick.");
        assertEquals(0, pigeon.debugNormalAttackFrame());
        assertEquals("Pigeon Rooftop Peck", pigeon.debugNormalAttackMoveName());
    }

    private static AttackOutcome sideSmashOutcome(double targetX) throws Exception {
        BirdGame3 game = twoBirdGame(BirdGame3.BirdType.PIGEON, BirdGame3.BirdType.EAGLE,
                320.0, targetX);
        Bird pigeon = game.players[0];
        Bird target = game.players[1];
        double startingHealth = target.health;
        performAttack(pigeon, "SIDE_SMASH");
        advanceTimer(pigeon, 9);
        return new AttackOutcome(startingHealth - target.health, target.vx);
    }

    private static BirdGame3 airborneTwoBirdGame() {
        BirdGame3 game = twoBirdGame(BirdGame3.BirdType.PIGEON, BirdGame3.BirdType.EAGLE,
                320.0, 382.0);
        game.players[0].y -= 240.0;
        game.players[1].y -= 240.0;
        return game;
    }

    private static BirdGame3 twoBirdGame(BirdGame3.BirdType attackerType, BirdGame3.BirdType targetType,
                                         double attackerX, double targetX) {
        BirdGame3 game = new BirdGame3();
        game.activePlayers = 2;
        game.players[0] = groundedBird(game, attackerType, 0, attackerX);
        game.players[1] = groundedBird(game, targetType, 1, targetX);
        return game;
    }

    private static Bird groundedBird(BirdGame3 game, BirdGame3.BirdType type, int index, double x) {
        if (game.platforms.isEmpty()) {
            game.platforms.add(new Platform(0.0, BirdGame3.GROUND_Y,
                    BirdGame3.WORLD_WIDTH, 80.0));
        }
        Bird bird = new Bird(x, type, index, game);
        bird.y = BirdGame3.GROUND_Y - bird.bodyHeight();
        return bird;
    }

    private static void performAttack(Bird bird, String variantName) throws Exception {
        Object variant = enumConstant("com.example.birdgame3.Bird$NormalAttackVariant", variantName);
        invoke(bird, "performAttack", 0, variant);
    }

    private static void advanceTimer(Bird bird, int frames) throws Exception {
        for (int frame = 0; frame < frames; frame++) {
            invoke(bird, "updateTimers", 1.0);
        }
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static Object enumConstant(String className, String name) throws Exception {
        Class enumClass = Class.forName(className);
        return Enum.valueOf(enumClass, name);
    }

    private static Object invoke(Object target, String name, Object... args) throws Exception {
        Method match = null;
        for (Method method : target.getClass().getDeclaredMethods()) {
            if (method.getName().equals(name) && method.getParameterCount() == args.length) {
                match = method;
                break;
            }
        }
        assertNotNull(match, "Missing method " + name);
        match.setAccessible(true);
        return match.invoke(target, args);
    }

    private static int intField(Object target, String name) throws Exception {
        var field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        return field.getInt(target);
    }

    private static boolean booleanField(Object target, String name) throws Exception {
        var field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        return field.getBoolean(target);
    }

    private record AttackOutcome(double damage, double horizontalVelocity) {
    }
}
