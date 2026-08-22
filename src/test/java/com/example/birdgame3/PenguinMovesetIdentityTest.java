package com.example.birdgame3;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;

class PenguinMovesetIdentityTest {
    @Test
    void penguinHasAnAuthoredCompleteTractionCounterhitKit() throws Exception {
        BirdGame3 game = new BirdGame3();
        Bird penguin = groundedBird(game, BirdGame3.BirdType.PENGUIN, 0, 320.0);
        Bird roadrunner = groundedBird(game, BirdGame3.BirdType.ROADRUNNER, 1, 320.0);
        Bird vulture = groundedBird(game, BirdGame3.BirdType.VULTURE, 2, 320.0);

        int distinctFromRoadrunner = 0;
        int distinctFromSharedProfile = 0;
        for (Object variant : normalAttackVariantClass().getEnumConstants()) {
            Object profile = invoke(penguin, "normalAttackProfile", variant);
            if (!profile.equals(invoke(roadrunner, "normalAttackProfile", variant))) distinctFromRoadrunner++;
            if (!profile.equals(invoke(vulture, "normalAttackProfile", variant))) distinctFromSharedProfile++;
            String moveName = (String) invoke(penguin, "normalAttackTelemetryName", variant);
            assertTrue(moveName.startsWith("Penguin "));
            assertFalse(moveName.contains("Normal Attack"));
            assertFalse(moveName.contains("Tilt"));
        }

        assertEquals(15, distinctFromRoadrunner);
        assertEquals(15, distinctFromSharedProfile);
    }

    @Test
    void penguinPummelIsMeasuredColdShoulderPressure() throws Exception {
        BirdGame3 game = new BirdGame3();
        game.activePlayers = 2;
        Bird penguin = groundedBird(game, BirdGame3.BirdType.PENGUIN, 0, 320.0);
        Bird target = groundedBird(game, BirdGame3.BirdType.PIGEON, 1, 385.0);
        game.players[0] = penguin;
        game.players[1] = target;
        linkGrab(penguin, target);
        game.setAiControlKey(0, game.attackKeyForPlayer(0), true);

        double healthBefore = target.health;
        assertTrue((boolean) invoke(penguin, "handleHoldingGrabState", false, false));

        double tunedDamage = 3.0 * penguin.type.damageDealtMult * target.type.damageTakenMult;
        assertEquals(tunedDamage, healthBefore - target.health, 0.001);
        assertSame(target, getField(penguin, "grabbedTarget"));
        assertSame(penguin, getField(target, "grabbedBy"));
        assertEquals(10, getInt(penguin, "grabThrowLockTimer"));
        assertEquals(51, getInt(penguin, "grabHoldTimer"),
                "The normal hold tick plus Penguin's eight-frame pummel cost should be deterministic.");
    }

    @Test
    void penguinThrowsHaveFourDistinctRinkControlRoles() throws Exception {
        ThrowOutcome forward = performThrow("FORWARD");
        ThrowOutcome back = performThrow("BACK");
        ThrowOutcome up = performThrow("UP");
        ThrowOutcome down = performThrow("DOWN");

        assertTrue(forward.vx >= 18.0);
        assertTrue(back.vx <= -19.0);
        assertTrue(up.vy < forward.vy);
        assertTrue(down.damage > forward.damage);
        assertEquals("Penguin Belly Bump", forward.name);
        assertEquals("Penguin Flipper Sweep", back.name);
        assertEquals("Penguin Iceberg Toss", up.name);
        assertEquals("Penguin Ice Press", down.name);
    }

    @Test
    void penguinGroundedAttackPosesKeepWebbedFeetAboveTheFloor() throws Exception {
        BirdGame3 game = new BirdGame3();
        Bird penguin = groundedBird(game, BirdGame3.BirdType.PENGUIN, 0, 320.0);
        String[] groundedVariants = {
                "NEUTRAL", "SIDE_TILT", "UP_TILT", "DOWN_TILT",
                "SIDE_SMASH", "UP_SMASH", "DOWN_SMASH",
                "DASH_ATTACK", "LEDGE_ATTACK", "GETUP_ATTACK"
        };

        for (String variantName : groundedVariants) {
            Object variant = enumConstant("com.example.birdgame3.Bird$NormalAttackVariant", variantName);
            Object pose = invoke(penguin, "currentPenguinNormalAttackPose", variant, 1.0);
            double translateY = (double) invoke(pose, "translateY");
            double rotation = (double) invoke(pose, "bodyRotationDegrees");
            assertTrue(translateY <= 0.0, variantName + " must not push Penguin's feet below the floor");
            assertTrue(Math.abs(rotation) <= 12.0,
                    variantName + " must keep Penguin's grounded pear silhouette stable");
        }
    }

    @Test
    void penguinDashAttackOwnsALowWideBellySlideLane() throws Exception {
        BirdGame3 game = new BirdGame3();
        Bird penguin = groundedBird(game, BirdGame3.BirdType.PENGUIN, 0, 320.0);
        Bird vulture = groundedBird(game, BirdGame3.BirdType.VULTURE, 1, 320.0);
        Object dash = enumConstant("com.example.birdgame3.Bird$NormalAttackVariant", "DASH_ATTACK");
        Object penguinProfile = invoke(penguin, "normalAttackProfile", dash);
        Object sharedProfile = invoke(vulture, "normalAttackProfile", dash);

        assertTrue((double) invoke(penguinProfile, "horizontalReach")
                > (double) invoke(sharedProfile, "horizontalReach"));
        assertTrue((double) invoke(penguinProfile, "verticalReach")
                < (double) invoke(sharedProfile, "verticalReach"));
        assertTrue((double) invoke(penguinProfile, "horizontalLaunchScale") >= 1.30);
    }

    @Test
    void penguinFlippersOpenForClapsAndTuckForTheRinkSlide() throws Exception {
        BirdGame3 game = new BirdGame3();
        Bird penguin = groundedBird(game, BirdGame3.BirdType.PENGUIN, 0, 320.0);
        Object upSmash = enumConstant("com.example.birdgame3.Bird$NormalAttackVariant", "UP_SMASH");
        Object dashAttack = enumConstant("com.example.birdgame3.Bird$NormalAttackVariant", "DASH_ATTACK");

        invoke(penguin, "performAttack", 0, upSmash);
        Object attackState = invoke(penguin, "currentBirdAnimationState");
        double clapOpenness = (double) invoke(penguin, "penguinFlipperOpenness", attackState);

        invoke(penguin, "performAttack", 0, dashAttack);
        attackState = invoke(penguin, "currentBirdAnimationState");
        double slideOpenness = (double) invoke(penguin, "penguinFlipperOpenness", attackState);

        assertTrue(clapOpenness >= 0.82, "Penguin's upward clap must visibly spread both flippers");
        assertTrue(slideOpenness <= 0.30, "Penguin's rink slide must streamline both flippers");
    }

    @Test
    void penguinAttackIdentityRoundTripsThroughLanState() throws Exception {
        BirdGame3 game = new BirdGame3();
        Bird source = groundedBird(game, BirdGame3.BirdType.PENGUIN, 0, 320.0);
        Object dashAttack = enumConstant("com.example.birdgame3.Bird$NormalAttackVariant", "DASH_ATTACK");
        invoke(source, "performAttack", 0, dashAttack);

        Bird restored = groundedBird(game, BirdGame3.BirdType.PENGUIN, 0, 320.0);
        restored.applyLanState(source.toLanState());

        assertEquals("DASH_ATTACK", ((Enum<?>) getField(restored, "activeAttackVariant")).name());
        assertEquals(source.attackAnimationTimer, restored.attackAnimationTimer);
    }

    private static ThrowOutcome performThrow(String directionName) throws Exception {
        BirdGame3 game = new BirdGame3();
        game.activePlayers = 2;
        Bird penguin = groundedBird(game, BirdGame3.BirdType.PENGUIN, 0, 320.0);
        Bird target = groundedBird(game, BirdGame3.BirdType.PIGEON, 1, 385.0);
        game.players[0] = penguin;
        game.players[1] = target;
        linkGrab(penguin, target);
        Object direction = enumConstant("com.example.birdgame3.Bird$GrabThrowDirection", directionName);
        String name = (String) invoke(penguin, "throwTelemetryName", direction);
        double before = target.health;
        invoke(penguin, "performThrow", direction);
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
