package com.example.birdgame3;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;

class ShoebillMovesetIdentityTest {
    @Test
    void shoebillHasAnAuthoredCompleteStillHunterKit() throws Exception {
        BirdGame3 game = new BirdGame3();
        Bird shoebill = groundedBird(game, BirdGame3.BirdType.SHOEBILL, 0, 320.0);
        Bird penguin = groundedBird(game, BirdGame3.BirdType.PENGUIN, 1, 320.0);
        Bird charles = groundedBird(game, BirdGame3.BirdType.MOCKINGBIRD, 2, 320.0);

        int distinctFromPenguin = 0;
        int distinctFromSharedProfile = 0;
        for (Object variant : normalAttackVariantClass().getEnumConstants()) {
            Object profile = invoke(shoebill, "normalAttackProfile", variant);
            if (!profile.equals(invoke(penguin, "normalAttackProfile", variant))) distinctFromPenguin++;
            if (!profile.equals(invoke(charles, "normalAttackProfile", variant))) distinctFromSharedProfile++;
            String moveName = (String) invoke(shoebill, "normalAttackTelemetryName", variant);
            assertTrue(moveName.startsWith("Shoebill "));
            assertFalse(moveName.contains("Normal Attack"));
            assertFalse(moveName.contains("Tilt"));
        }

        assertEquals(15, distinctFromPenguin);
        assertEquals(15, distinctFromSharedProfile);
    }

    @Test
    void shoebillPummelIsAHeavyDeliberateBillClamp() throws Exception {
        BirdGame3 game = new BirdGame3();
        game.activePlayers = 2;
        Bird shoebill = groundedBird(game, BirdGame3.BirdType.SHOEBILL, 0, 320.0);
        Bird target = groundedBird(game, BirdGame3.BirdType.PIGEON, 1, 385.0);
        game.players[0] = shoebill;
        game.players[1] = target;
        linkGrab(shoebill, target);
        game.setAiControlKey(0, game.attackKeyForPlayer(0), true);

        double healthBefore = target.health;
        assertTrue((boolean) invoke(shoebill, "handleHoldingGrabState", false, false));

        double tunedDamage = 4.0 * shoebill.type.damageDealtMult * target.type.damageTakenMult;
        assertEquals(tunedDamage, healthBefore - target.health, 0.001);
        assertSame(target, getField(shoebill, "grabbedTarget"));
        assertSame(shoebill, getField(target, "grabbedBy"));
        assertEquals(13, getInt(shoebill, "grabThrowLockTimer"));
        assertEquals(49, getInt(shoebill, "grabHoldTimer"),
                "The normal hold tick plus Shoebill's ten-frame pummel cost should be deterministic.");
    }

    @Test
    void shoebillThrowsCoverFourPatientControlRoles() throws Exception {
        ThrowOutcome forward = performThrow("FORWARD");
        ThrowOutcome back = performThrow("BACK");
        ThrowOutcome up = performThrow("UP");
        ThrowOutcome down = performThrow("DOWN");

        assertTrue(forward.vx >= 20.0);
        assertTrue(back.vx <= -21.0);
        assertTrue(up.vy < back.vy);
        assertTrue(down.damage > up.damage);
        assertEquals("Shoebill Marsh Cast", forward.name);
        assertEquals("Shoebill Reed Reversal", back.name);
        assertEquals("Shoebill Canopy Lift", up.name);
        assertEquals("Shoebill Stillwater Press", down.name);
    }

    @Test
    void shoebillGroundedAttackPosesKeepLongLegsAboveTheFloor() throws Exception {
        BirdGame3 game = new BirdGame3();
        Bird shoebill = groundedBird(game, BirdGame3.BirdType.SHOEBILL, 0, 320.0);
        String[] groundedVariants = {
                "NEUTRAL", "SIDE_TILT", "UP_TILT", "DOWN_TILT",
                "SIDE_SMASH", "UP_SMASH", "DOWN_SMASH",
                "DASH_ATTACK", "LEDGE_ATTACK", "GETUP_ATTACK"
        };

        for (String variantName : groundedVariants) {
            Object variant = enumConstant("com.example.birdgame3.Bird$NormalAttackVariant", variantName);
            Object pose = invoke(shoebill, "currentShoebillNormalAttackPose", variant, 1.0);
            double translateY = (double) invoke(pose, "translateY");
            double rotation = (double) invoke(pose, "bodyRotationDegrees");
            assertTrue(translateY <= 0.0, variantName + " must not push Shoebill's feet below the floor");
            assertTrue(Math.abs(rotation) <= 11.0,
                    variantName + " must preserve Shoebill's grounded, statuesque silhouette");
        }
    }

    @Test
    void shoebillOwnsTheLongestNarrowGroundedPokeSoFar() throws Exception {
        BirdGame3 game = new BirdGame3();
        Bird shoebill = groundedBird(game, BirdGame3.BirdType.SHOEBILL, 0, 320.0);
        Bird penguin = groundedBird(game, BirdGame3.BirdType.PENGUIN, 1, 320.0);
        Object sideTilt = enumConstant("com.example.birdgame3.Bird$NormalAttackVariant", "SIDE_TILT");
        Object shoebillProfile = invoke(shoebill, "normalAttackProfile", sideTilt);
        Object penguinProfile = invoke(penguin, "normalAttackProfile", sideTilt);

        assertTrue((double) invoke(shoebillProfile, "horizontalReach")
                >= (double) invoke(penguinProfile, "horizontalReach") + 30.0);
        assertTrue((double) invoke(shoebillProfile, "verticalReach")
                < (double) invoke(penguinProfile, "verticalReach"));
        assertTrue((int) invoke(shoebillProfile, "cooldownFrames")
                > (int) invoke(penguinProfile, "cooldownFrames"),
                "Shoebill's superior spacing must retain a deliberate commitment cost.");
    }

    @Test
    void shoebillWingsOpenForCanopyAttacksAndTuckBehindBillThrusts() throws Exception {
        BirdGame3 game = new BirdGame3();
        Bird shoebill = groundedBird(game, BirdGame3.BirdType.SHOEBILL, 0, 320.0);
        Object upSmash = enumConstant("com.example.birdgame3.Bird$NormalAttackVariant", "UP_SMASH");
        Object sideSmash = enumConstant("com.example.birdgame3.Bird$NormalAttackVariant", "SIDE_SMASH");

        invoke(shoebill, "performAttack", 0, upSmash);
        Object attackState = invoke(shoebill, "currentBirdAnimationState");
        double canopyOpenness = (double) invoke(shoebill, "shoebillWingOpenness", attackState);

        invoke(shoebill, "performAttack", 0, sideSmash);
        attackState = invoke(shoebill, "currentBirdAnimationState");
        double thrustOpenness = (double) invoke(shoebill, "shoebillWingOpenness", attackState);

        assertTrue(canopyOpenness >= 0.78, "Canopy attacks must visibly spread Shoebill's wings");
        assertTrue(thrustOpenness <= 0.52, "Bill thrusts must keep Shoebill's wings tucked and readable");
    }

    @Test
    void shoebillAttackIdentityRoundTripsThroughLanState() throws Exception {
        BirdGame3 game = new BirdGame3();
        Bird source = groundedBird(game, BirdGame3.BirdType.SHOEBILL, 0, 320.0);
        Object forwardAir = enumConstant("com.example.birdgame3.Bird$NormalAttackVariant", "FORWARD_AIR");
        invoke(source, "performAttack", 0, forwardAir);

        Bird restored = groundedBird(game, BirdGame3.BirdType.SHOEBILL, 0, 320.0);
        restored.applyLanState(source.toLanState());

        assertEquals("FORWARD_AIR", ((Enum<?>) getField(restored, "activeAttackVariant")).name());
        assertEquals(source.attackAnimationTimer, restored.attackAnimationTimer);
    }

    private static ThrowOutcome performThrow(String directionName) throws Exception {
        BirdGame3 game = new BirdGame3();
        game.activePlayers = 2;
        Bird shoebill = groundedBird(game, BirdGame3.BirdType.SHOEBILL, 0, 320.0);
        Bird target = groundedBird(game, BirdGame3.BirdType.PIGEON, 1, 385.0);
        game.players[0] = shoebill;
        game.players[1] = target;
        linkGrab(shoebill, target);
        Object direction = enumConstant("com.example.birdgame3.Bird$GrabThrowDirection", directionName);
        String name = (String) invoke(shoebill, "throwTelemetryName", direction);
        double before = target.health;
        invoke(shoebill, "performThrow", direction);
        return new ThrowOutcome(before - target.health, target.vx, target.vy, name);
    }

    private static Bird groundedBird(BirdGame3 game, BirdGame3.BirdType type, int index, double x) {
        if (game.platforms.isEmpty()) {
            game.platforms.add(new Platform(0.0, BirdGame3.GROUND_Y, BirdGame3.WORLD_WIDTH, 80.0));
        }
        Bird bird = new Bird(x, type, index, game);
        bird.y = BirdGame3.GROUND_Y - bird.bodyHeight();
        return bird;
    }

    private static void linkGrab(Bird holder, Bird target) throws Exception {
        setField(holder, "grabbedTarget", target);
        setField(target, "grabbedBy", holder);
        setField(holder, "grabHoldTimer", 60);
    }

    private static Class<?> normalAttackVariantClass() throws ClassNotFoundException {
        return Class.forName("com.example.birdgame3.Bird$NormalAttackVariant");
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

    private static Object getField(Object target, String name) throws Exception {
        Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        return field.get(target);
    }

    private static int getInt(Object target, String name) throws Exception {
        return (int) getField(target, name);
    }

    private static void setField(Object target, String name, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        field.set(target, value);
    }

    private record ThrowOutcome(double damage, double vx, double vy, String name) {
    }
}
