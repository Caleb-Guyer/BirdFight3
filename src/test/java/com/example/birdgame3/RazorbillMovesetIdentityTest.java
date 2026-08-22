package com.example.birdgame3;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;

class RazorbillMovesetIdentityTest {
    @Test
    void razorbillHasAnAuthoredCompletePrecisionBladeKit() throws Exception {
        BirdGame3 game = new BirdGame3();
        Bird razorbill = groundedBird(game, BirdGame3.BirdType.RAZORBILL, 0, 320.0);
        Bird charles = groundedBird(game, BirdGame3.BirdType.MOCKINGBIRD, 1, 320.0);
        Bird opiumBird = groundedBird(game, BirdGame3.BirdType.TITMOUSE, 2, 320.0);

        int distinctFromCharles = 0;
        int distinctFromSharedProfile = 0;
        for (Object variant : normalAttackVariantClass().getEnumConstants()) {
            Object profile = invoke(razorbill, "normalAttackProfile", variant);
            if (!profile.equals(invoke(charles, "normalAttackProfile", variant))) distinctFromCharles++;
            if (!profile.equals(invoke(opiumBird, "normalAttackProfile", variant))) distinctFromSharedProfile++;
            String moveName = (String) invoke(razorbill, "normalAttackTelemetryName", variant);
            assertTrue(moveName.startsWith("Razorbill "));
            assertFalse(moveName.contains("Normal Attack"));
            assertFalse(moveName.contains("Tilt"));
        }

        assertEquals(15, distinctFromCharles);
        assertEquals(15, distinctFromSharedProfile);
    }

    @Test
    void razorbillPummelUsesAQuickEdgePress() throws Exception {
        BirdGame3 game = new BirdGame3();
        game.activePlayers = 2;
        Bird razorbill = groundedBird(game, BirdGame3.BirdType.RAZORBILL, 0, 320.0);
        Bird target = groundedBird(game, BirdGame3.BirdType.PIGEON, 1, 385.0);
        game.players[0] = razorbill;
        game.players[1] = target;
        linkGrab(razorbill, target);
        game.setAiControlKey(0, game.attackKeyForPlayer(0), true);

        double healthBefore = target.health;
        assertTrue((boolean) invoke(razorbill, "handleHoldingGrabState", false, false));

        double tunedDamage = 3.0 * razorbill.type.damageDealtMult * target.type.damageTakenMult;
        assertEquals(tunedDamage, healthBefore - target.health, 0.001);
        assertSame(target, getField(razorbill, "grabbedTarget"));
        assertSame(razorbill, getField(target, "grabbedBy"));
        assertEquals(8, getInt(razorbill, "grabThrowLockTimer"));
        assertEquals(53, getInt(razorbill, "grabHoldTimer"),
                "The normal hold tick plus Razorbill's six-frame pummel cost must stay deterministic.");
    }

    @Test
    void razorbillThrowsCutAcrossFourDistinctLaunchLanes() throws Exception {
        ThrowOutcome forward = performThrow("FORWARD");
        ThrowOutcome back = performThrow("BACK");
        ThrowOutcome up = performThrow("UP");
        ThrowOutcome down = performThrow("DOWN");

        assertTrue(forward.vx >= 20.0);
        assertTrue(back.vx <= -21.0);
        assertTrue(up.vy < back.vy);
        assertTrue(down.damage > up.damage);
        assertEquals("Razorbill Line Cut", forward.name);
        assertEquals("Razorbill Crosscut", back.name);
        assertEquals("Razorbill Vertical Incision", up.name);
        assertEquals("Razorbill Keel Drop", down.name);
    }

    @Test
    void razorbillGroundedAttackPosesKeepHisFeetAboveTheStage() throws Exception {
        BirdGame3 game = new BirdGame3();
        Bird razorbill = groundedBird(game, BirdGame3.BirdType.RAZORBILL, 0, 320.0);
        String[] groundedVariants = {
                "NEUTRAL", "SIDE_TILT", "UP_TILT", "DOWN_TILT",
                "SIDE_SMASH", "UP_SMASH", "DOWN_SMASH",
                "DASH_ATTACK", "LEDGE_ATTACK", "GETUP_ATTACK"
        };

        for (String variantName : groundedVariants) {
            Object variant = enumConstant("com.example.birdgame3.Bird$NormalAttackVariant", variantName);
            Object pose = invoke(razorbill, "currentRazorbillNormalAttackPose", variant, 1.0);
            double translateY = (double) invoke(pose, "translateY");
            double rotation = (double) invoke(pose, "bodyRotationDegrees");
            assertTrue(translateY <= 0.0, variantName + " must not sink Razorbill below the stage");
            assertTrue(Math.abs(rotation) <= 12.0,
                    variantName + " must keep Razorbill's grounded cuts visually planted");
        }
    }

    @Test
    void razorbillNormalsFavorLongNarrowCutsWithoutReplacingBladeStorm() throws Exception {
        BirdGame3 game = new BirdGame3();
        Bird razorbill = groundedBird(game, BirdGame3.BirdType.RAZORBILL, 0, 320.0);
        Bird opiumBird = groundedBird(game, BirdGame3.BirdType.TITMOUSE, 1, 320.0);
        Object sideTilt = enumConstant("com.example.birdgame3.Bird$NormalAttackVariant", "SIDE_TILT");
        Object forwardAir = enumConstant("com.example.birdgame3.Bird$NormalAttackVariant", "FORWARD_AIR");
        Object sideSmash = enumConstant("com.example.birdgame3.Bird$NormalAttackVariant", "SIDE_SMASH");
        Object dashAttack = enumConstant("com.example.birdgame3.Bird$NormalAttackVariant", "DASH_ATTACK");
        Object razorbillTilt = invoke(razorbill, "normalAttackProfile", sideTilt);
        Object sharedTilt = invoke(opiumBird, "normalAttackProfile", sideTilt);
        Object razorbillFair = invoke(razorbill, "normalAttackProfile", forwardAir);
        Object razorbillSmash = invoke(razorbill, "normalAttackProfile", sideSmash);
        Object razorbillDash = invoke(razorbill, "normalAttackProfile", dashAttack);

        assertTrue((double) invoke(razorbillTilt, "horizontalReach")
                > (double) invoke(sharedTilt, "horizontalReach"));
        assertTrue((double) invoke(razorbillTilt, "verticalReach")
                < (double) invoke(sharedTilt, "verticalReach"));
        assertTrue((double) invoke(razorbillFair, "horizontalReach") >= 180.0);
        assertTrue((double) invoke(razorbillSmash, "horizontalLaunchScale") > 1.50);
        assertTrue((int) invoke(razorbillDash, "cooldownFrames") <= 25);
        assertTrue((double) invoke(razorbillDash, "horizontalLaunchScale") >= 1.40,
                "Slipstream Cleave should reward a committed ground approach without becoming reusable flight.");
    }

    @Test
    void razorbillWingsOpenForVerticalCutsAndTuckForSlipstreamCleave() throws Exception {
        BirdGame3 game = new BirdGame3();
        Object upSmash = enumConstant("com.example.birdgame3.Bird$NormalAttackVariant", "UP_SMASH");
        Object dashAttack = enumConstant("com.example.birdgame3.Bird$NormalAttackVariant", "DASH_ATTACK");
        Bird verticalCutter = groundedBird(game, BirdGame3.BirdType.RAZORBILL, 0, 320.0);
        Bird dasher = groundedBird(game, BirdGame3.BirdType.RAZORBILL, 1, 320.0);

        invoke(verticalCutter, "performAttack", 0, upSmash);
        Object attackState = invoke(verticalCutter, "currentBirdAnimationState");
        double verticalOpenness = (double) invoke(verticalCutter, "razorbillWingOpenness", attackState);

        invoke(dasher, "performAttack", 0, dashAttack);
        attackState = invoke(dasher, "currentBirdAnimationState");
        double dashOpenness = (double) invoke(dasher, "razorbillWingOpenness", attackState);

        assertTrue(verticalOpenness >= 0.82, "Split Horizon must visibly spread Razorbill's blade wings");
        assertTrue(dashOpenness <= 0.30, "Slipstream Cleave must tuck Razorbill into a narrow silhouette");
    }

    @Test
    void razorbillAttackIdentityRoundTripsThroughLanState() throws Exception {
        BirdGame3 game = new BirdGame3();
        Bird source = groundedBird(game, BirdGame3.BirdType.RAZORBILL, 0, 320.0);
        Object forwardAir = enumConstant("com.example.birdgame3.Bird$NormalAttackVariant", "FORWARD_AIR");
        invoke(source, "performAttack", 0, forwardAir);

        Bird restored = groundedBird(game, BirdGame3.BirdType.RAZORBILL, 0, 320.0);
        restored.applyLanState(source.toLanState());

        assertEquals("FORWARD_AIR", ((Enum<?>) getField(restored, "activeAttackVariant")).name());
        assertEquals(source.attackAnimationTimer, restored.attackAnimationTimer);
    }

    private static ThrowOutcome performThrow(String directionName) throws Exception {
        BirdGame3 game = new BirdGame3();
        game.activePlayers = 2;
        Bird razorbill = groundedBird(game, BirdGame3.BirdType.RAZORBILL, 0, 320.0);
        Bird target = groundedBird(game, BirdGame3.BirdType.PIGEON, 1, 385.0);
        game.players[0] = razorbill;
        game.players[1] = target;
        linkGrab(razorbill, target);
        Object direction = enumConstant("com.example.birdgame3.Bird$GrabThrowDirection", directionName);
        String name = (String) invoke(razorbill, "throwTelemetryName", direction);
        double before = target.health;
        invoke(razorbill, "performThrow", direction);
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
