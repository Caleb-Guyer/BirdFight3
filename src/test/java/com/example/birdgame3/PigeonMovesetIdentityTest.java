package com.example.birdgame3;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;

class PigeonMovesetIdentityTest {
    @Test
    void universalAttackVariantsAreAppendedForReplayOrdinalSafety() throws Exception {
        Object[] variants = normalAttackVariantClass().getEnumConstants();

        assertEquals("DOWN_AIR", ((Enum<?>) variants[11]).name());
        assertEquals("DASH_ATTACK", ((Enum<?>) variants[12]).name());
        assertEquals("LEDGE_ATTACK", ((Enum<?>) variants[13]).name());
        assertEquals("GETUP_ATTACK", ((Enum<?>) variants[14]).name());
    }

    @Test
    void pigeonHasACompleteNormalKitInsteadOfTheSharedDefaultProfile() throws Exception {
        BirdGame3 game = new BirdGame3();
        Bird pigeon = groundedBird(game, BirdGame3.BirdType.PIGEON, 0, 320.0);
        Bird eagle = groundedBird(game, BirdGame3.BirdType.EAGLE, 1, 320.0);

        int distinctProfiles = 0;
        for (Object variant : normalAttackVariantClass().getEnumConstants()) {
            Object pigeonProfile = invoke(pigeon, "normalAttackProfile", variant);
            Object eagleProfile = invoke(eagle, "normalAttackProfile", variant);
            if (!pigeonProfile.equals(eagleProfile)) {
                distinctProfiles++;
            }
            String moveName = (String) invoke(pigeon, "normalAttackTelemetryName", variant);
            assertTrue(moveName.startsWith("Pigeon "));
            assertFalse(moveName.contains("Normal Attack"));
        }

        assertEquals(15, distinctProfiles,
                "Every Pigeon normal should have authored frame, reach, or launch data.");
    }

    @Test
    void dashAttackConsumesDashAndCarriesForwardMomentum() throws Exception {
        BirdGame3 game = new BirdGame3();
        game.activePlayers = 1;
        Bird pigeon = groundedBird(game, BirdGame3.BirdType.PIGEON, 0, 320.0);
        game.players[0] = pigeon;
        setInt(pigeon, "dashTimer", 6);
        game.setAiControlKey(0, game.rightKeyForPlayer(0), true);
        assertTrue(game.isRightPressed(0));

        Object selected = invoke(pigeon, "selectNormalAttackVariant", true);
        assertEquals("DASH_ATTACK", ((Enum<?>) selected).name());
        invoke(pigeon, "performAttack", 0, selected);

        assertEquals(0, getInt(pigeon, "dashTimer"));
        assertTrue(pigeon.vx > 8.0);
        assertEquals("DASH_ATTACK", activeAttackVariantName(pigeon));
    }

    @Test
    void appendedAttackVariantsRoundTripThroughLanState() throws Exception {
        BirdGame3 game = new BirdGame3();
        Bird source = groundedBird(game, BirdGame3.BirdType.PIGEON, 0, 320.0);
        Object dashAttack = enumConstant(
                "com.example.birdgame3.Bird$NormalAttackVariant", "DASH_ATTACK");
        invoke(source, "performAttack", 0, dashAttack);

        LanBirdState snapshot = source.toLanState();
        Bird restored = groundedBird(game, BirdGame3.BirdType.PIGEON, 0, 320.0);
        restored.applyLanState(snapshot);

        assertEquals("DASH_ATTACK", activeAttackVariantName(restored));
    }

    @Test
    void pummelDealsDamageWithoutReleasingOrLaunchingTheGrabbedBird() throws Exception {
        BirdGame3 game = new BirdGame3();
        game.activePlayers = 2;
        Bird pigeon = groundedBird(game, BirdGame3.BirdType.PIGEON, 0, 320.0);
        Bird target = groundedBird(game, BirdGame3.BirdType.EAGLE, 1, 385.0);
        game.players[0] = pigeon;
        game.players[1] = target;
        linkGrab(pigeon, target);
        setInt(pigeon, "grabThrowLockTimer", 0);
        game.setAiControlKey(0, game.attackKeyForPlayer(0), true);
        assertTrue(pigeon.isOnGround());
        assertTrue(game.isAttackPressed(0));

        double healthBefore = target.health;
        assertTrue((boolean) invoke(pigeon, "handleHoldingGrabState", false, false));

        assertTrue(target.health < healthBefore);
        assertEquals(0.0, target.vx, 0.0001);
        assertSame(target, getField(pigeon, "grabbedTarget"));
        assertSame(pigeon, getField(target, "grabbedBy"));
        assertEquals(10, getInt(pigeon, "grabThrowLockTimer"));
    }

    @Test
    void ledgeAttackAndGetupAttackHaveDedicatedStates() throws Exception {
        BirdGame3 ledgeGame = new BirdGame3();
        ledgeGame.activePlayers = 1;
        ledgeGame.isAI[0] = false;
        Bird ledgePigeon = groundedBird(ledgeGame, BirdGame3.BirdType.PIGEON, 0, 320.0);
        ledgeGame.players[0] = ledgePigeon;
        Platform ledge = new Platform(260.0, BirdGame3.GROUND_Y, 520.0, 40.0);
        ledgeGame.platforms.add(ledge);
        setField(ledgePigeon, "ledgePlatform", ledge);
        setBoolean(ledgePigeon, "ledgeHanging", true);
        setBoolean(ledgePigeon, "ledgeGrabOnRightSide", false);
        setInt(ledgePigeon, "ledgeLockTimer", 0);
        ledgeGame.setAiControlKey(0, ledgeGame.attackKeyForPlayer(0), true);
        assertTrue(ledgeGame.isAttackPressed(0));

        assertTrue((boolean) invoke(ledgePigeon, "handleLedgeHanging", false));
        assertTrue(ledgePigeon.debugUniversalActionLabel().contains("ATTACK STARTUP"));
        for (int frame = 0; frame < 7; frame++) {
            assertTrue((boolean) invoke(ledgePigeon, "handleLedgeHanging", false));
        }
        assertFalse((boolean) invoke(ledgePigeon, "handleLedgeHanging", false));
        assertEquals("LEDGE_ATTACK", activeAttackVariantName(ledgePigeon));

        BirdGame3 getupGame = new BirdGame3();
        getupGame.activePlayers = 1;
        Bird getupPigeon = groundedBird(getupGame, BirdGame3.BirdType.PIGEON, 0, 320.0);
        getupGame.players[0] = getupPigeon;
        getupPigeon.knockdownTimer = 30;
        getupGame.setAiControlKey(0, getupGame.attackKeyForPlayer(0), true);

        assertTrue((boolean) invoke(getupPigeon, "handleGroundedGetupAttack", false, false));
        assertEquals(0, getupPigeon.knockdownTimer);
        assertEquals("GETUP_ATTACK", activeAttackVariantName(getupPigeon));
        assertTrue(getInt(getupPigeon, "dodgeInvulnerabilityTimer") >= 8);
    }

    @Test
    void pigeonThrowsHaveDistinctLaunchJobsAndNames() throws Exception {
        ThrowOutcome forward = performPigeonThrow("FORWARD");
        ThrowOutcome up = performPigeonThrow("UP");
        ThrowOutcome down = performPigeonThrow("DOWN");

        assertTrue(forward.vx > 15.0);
        assertTrue(up.vy < forward.vy);
        assertTrue(down.damage > up.damage);
        assertEquals("Pigeon Parcel Toss", forward.name);
        assertEquals("Pigeon Chimney Launch", up.name);
        assertEquals("Pigeon Curb Bounce", down.name);
    }

    private static ThrowOutcome performPigeonThrow(String directionName) throws Exception {
        BirdGame3 game = new BirdGame3();
        game.activePlayers = 2;
        Bird pigeon = groundedBird(game, BirdGame3.BirdType.PIGEON, 0, 320.0);
        Bird target = groundedBird(game, BirdGame3.BirdType.EAGLE, 1, 385.0);
        game.players[0] = pigeon;
        game.players[1] = target;
        linkGrab(pigeon, target);
        Object direction = enumConstant("com.example.birdgame3.Bird$GrabThrowDirection", directionName);
        String name = (String) invoke(pigeon, "throwTelemetryName", direction);
        double before = target.health;
        invoke(pigeon, "performThrow", direction);
        return new ThrowOutcome(before - target.health, target.vx, target.vy, name);
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

    private static void linkGrab(Bird holder, Bird target) throws Exception {
        setField(holder, "grabbedTarget", target);
        setField(target, "grabbedBy", holder);
        setInt(holder, "grabHoldTimer", 60);
    }

    private static Class<?> normalAttackVariantClass() throws ClassNotFoundException {
        return Class.forName("com.example.birdgame3.Bird$NormalAttackVariant");
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static Object enumConstant(String className, String name) throws Exception {
        Class enumClass = Class.forName(className);
        return Enum.valueOf(enumClass, name);
    }

    private static String activeAttackVariantName(Bird bird) throws Exception {
        return ((Enum<?>) getField(bird, "activeAttackVariant")).name();
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

    private static Object getField(Object target, String name) throws Exception {
        Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        return field.get(target);
    }

    private static int getInt(Object target, String name) throws Exception {
        return (int) getField(target, name);
    }

    private static void setInt(Object target, String name, int value) throws Exception {
        setField(target, name, value);
    }

    private static void setBoolean(Object target, String name, boolean value) throws Exception {
        setField(target, name, value);
    }

    private static void setField(Object target, String name, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        field.set(target, value);
    }

    private record ThrowOutcome(double damage, double vx, double vy, String name) {
    }
}
