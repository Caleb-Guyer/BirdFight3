package com.example.birdgame3;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;

class RoadrunnerMovesetIdentityTest {
    @Test
    void roadrunnerHasAnAuthoredCompleteGroundSpeedKit() throws Exception {
        BirdGame3 game = new BirdGame3();
        Bird roadrunner = groundedBird(game, BirdGame3.BirdType.ROADRUNNER, 0, 320.0);
        Bird rooster = groundedBird(game, BirdGame3.BirdType.ROOSTER, 1, 320.0);
        Bird opiumBird = groundedBird(game, BirdGame3.BirdType.OPIUMBIRD, 2, 320.0);

        int distinctFromRooster = 0;
        int distinctFromSharedProfile = 0;
        for (Object variant : normalAttackVariantClass().getEnumConstants()) {
            Object profile = invoke(roadrunner, "normalAttackProfile", variant);
            if (!profile.equals(invoke(rooster, "normalAttackProfile", variant))) distinctFromRooster++;
            if (!profile.equals(invoke(opiumBird, "normalAttackProfile", variant))) distinctFromSharedProfile++;
            String moveName = (String) invoke(roadrunner, "normalAttackTelemetryName", variant);
            assertTrue(moveName.startsWith("Roadrunner "));
            assertFalse(moveName.contains("Normal Attack"));
            assertFalse(moveName.contains("Tilt"));
        }

        assertEquals(15, distinctFromRooster);
        assertEquals(15, distinctFromSharedProfile);
    }

    @Test
    void roadrunnerPummelIsRapidKneePressure() throws Exception {
        BirdGame3 game = new BirdGame3();
        game.activePlayers = 2;
        Bird roadrunner = groundedBird(game, BirdGame3.BirdType.ROADRUNNER, 0, 320.0);
        Bird target = groundedBird(game, BirdGame3.BirdType.PIGEON, 1, 385.0);
        game.players[0] = roadrunner;
        game.players[1] = target;
        linkGrab(roadrunner, target);
        game.setAiControlKey(0, game.attackKeyForPlayer(0), true);

        double healthBefore = target.health;
        assertTrue((boolean) invoke(roadrunner, "handleHoldingGrabState", false, false));

        double tunedDamage = 2.0 * roadrunner.type.damageDealtMult * target.type.damageTakenMult;
        assertEquals(tunedDamage, healthBefore - target.health, 0.001);
        assertSame(target, getField(roadrunner, "grabbedTarget"));
        assertSame(roadrunner, getField(target, "grabbedBy"));
        assertEquals(7, getInt(roadrunner, "grabThrowLockTimer"));
        assertEquals(54, getInt(roadrunner, "grabHoldTimer"),
                "The normal hold tick plus Roadrunner's five-frame knee cost should be deterministic.");
    }

    @Test
    void roadrunnerThrowsHaveFourDistinctRacecraftRoles() throws Exception {
        ThrowOutcome forward = performThrow("FORWARD");
        ThrowOutcome back = performThrow("BACK");
        ThrowOutcome up = performThrow("UP");
        ThrowOutcome down = performThrow("DOWN");

        assertTrue(forward.vx >= 18.6);
        assertTrue(back.vx <= -19.8);
        assertTrue(up.vy < forward.vy);
        assertTrue(down.vy > up.vy);
        assertEquals("Roadrunner Finish-Line Fling", forward.name);
        assertEquals("Roadrunner Draft Pull", back.name);
        assertEquals("Roadrunner Top Gear Toss", up.name);
        assertEquals("Roadrunner Track Spike", down.name);
    }

    @Test
    void roadrunnerGroundedAttackPosesStayFloorSafeByConstruction() throws Exception {
        BirdGame3 game = new BirdGame3();
        Bird roadrunner = groundedBird(game, BirdGame3.BirdType.ROADRUNNER, 0, 320.0);
        String[] groundedVariants = {
                "NEUTRAL", "SIDE_TILT", "UP_TILT", "DOWN_TILT",
                "SIDE_SMASH", "UP_SMASH", "DOWN_SMASH",
                "DASH_ATTACK", "LEDGE_ATTACK", "GETUP_ATTACK"
        };

        for (String variantName : groundedVariants) {
            Object variant = enumConstant("com.example.birdgame3.Bird$NormalAttackVariant", variantName);
            Object pose = invoke(roadrunner, "currentRoadrunnerNormalAttackPose", variant, 1.0);
            double translateY = (double) invoke(pose, "translateY");
            double rotation = (double) invoke(pose, "bodyRotationDegrees");
            assertTrue(translateY <= 0.0, variantName + " must not push Roadrunner's feet below the floor");
            assertTrue(Math.abs(rotation) <= 14.0,
                    variantName + " must use restrained body rotation while grounded");
        }
    }

    @Test
    void roadrunnerDashAttackOwnsTheLongestFastGroundLane() throws Exception {
        BirdGame3 game = new BirdGame3();
        Bird roadrunner = groundedBird(game, BirdGame3.BirdType.ROADRUNNER, 0, 320.0);
        Bird opiumBird = groundedBird(game, BirdGame3.BirdType.OPIUMBIRD, 1, 320.0);
        Object dash = enumConstant("com.example.birdgame3.Bird$NormalAttackVariant", "DASH_ATTACK");
        Object roadrunnerProfile = invoke(roadrunner, "normalAttackProfile", dash);
        Object sharedProfile = invoke(opiumBird, "normalAttackProfile", dash);

        assertTrue((double) invoke(roadrunnerProfile, "horizontalReach")
                > (double) invoke(sharedProfile, "horizontalReach"));
        assertTrue((int) invoke(roadrunnerProfile, "cooldownFrames")
                < (int) invoke(sharedProfile, "cooldownFrames"));
        assertTrue((double) invoke(roadrunnerProfile, "horizontalLaunchScale") >= 1.45);
    }

    @Test
    void roadrunnerAttackIdentityRoundTripsThroughLanState() throws Exception {
        BirdGame3 game = new BirdGame3();
        Bird source = groundedBird(game, BirdGame3.BirdType.ROADRUNNER, 0, 320.0);
        Object dashAttack = enumConstant("com.example.birdgame3.Bird$NormalAttackVariant", "DASH_ATTACK");
        invoke(source, "performAttack", 0, dashAttack);

        Bird restored = groundedBird(game, BirdGame3.BirdType.ROADRUNNER, 0, 320.0);
        restored.applyLanState(source.toLanState());

        assertEquals("DASH_ATTACK", ((Enum<?>) getField(restored, "activeAttackVariant")).name());
        assertEquals(source.attackAnimationTimer, restored.attackAnimationTimer);
    }

    private static ThrowOutcome performThrow(String directionName) throws Exception {
        BirdGame3 game = new BirdGame3();
        game.activePlayers = 2;
        Bird roadrunner = groundedBird(game, BirdGame3.BirdType.ROADRUNNER, 0, 320.0);
        Bird target = groundedBird(game, BirdGame3.BirdType.PIGEON, 1, 385.0);
        game.players[0] = roadrunner;
        game.players[1] = target;
        linkGrab(roadrunner, target);
        Object direction = enumConstant("com.example.birdgame3.Bird$GrabThrowDirection", directionName);
        String name = (String) invoke(roadrunner, "throwTelemetryName", direction);
        double before = target.health;
        invoke(roadrunner, "performThrow", direction);
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
