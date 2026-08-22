package com.example.birdgame3;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;

class EagleMovesetIdentityTest {
    @Test
    void eagleHasAnAuthoredCompleteNormalKit() throws Exception {
        BirdGame3 game = new BirdGame3();
        Bird eagle = groundedBird(game, BirdGame3.BirdType.EAGLE, 0, 320.0);
        Bird pigeon = groundedBird(game, BirdGame3.BirdType.PIGEON, 1, 320.0);
        Bird falcon = groundedBird(game, BirdGame3.BirdType.FALCON, 2, 320.0);

        int distinctFromPigeon = 0;
        int distinctFromSharedRaptor = 0;
        for (Object variant : normalAttackVariantClass().getEnumConstants()) {
            Object eagleProfile = invoke(eagle, "normalAttackProfile", variant);
            if (!eagleProfile.equals(invoke(pigeon, "normalAttackProfile", variant))) {
                distinctFromPigeon++;
            }
            if (!eagleProfile.equals(invoke(falcon, "normalAttackProfile", variant))) {
                distinctFromSharedRaptor++;
            }
            String moveName = (String) invoke(eagle, "normalAttackTelemetryName", variant);
            assertTrue(moveName.startsWith("Eagle "));
            assertFalse(moveName.contains("Normal Attack"));
            assertFalse(moveName.contains("Tilt"));
        }

        assertEquals(15, distinctFromPigeon);
        assertEquals(15, distinctFromSharedRaptor,
                "Eagle must not silently fall back to Falcon's shared normal data.");
    }

    @Test
    void eaglePummelIsACommittedTalonPressThatKeepsTheGrab() throws Exception {
        BirdGame3 game = new BirdGame3();
        game.activePlayers = 2;
        Bird eagle = groundedBird(game, BirdGame3.BirdType.EAGLE, 0, 320.0);
        Bird target = groundedBird(game, BirdGame3.BirdType.FALCON, 1, 385.0);
        game.players[0] = eagle;
        game.players[1] = target;
        linkGrab(eagle, target);
        game.setAiControlKey(0, game.attackKeyForPlayer(0), true);

        double healthBefore = target.health;
        assertTrue((boolean) invoke(eagle, "handleHoldingGrabState", false, false));

        assertTrue(target.health < healthBefore);
        assertEquals(0.0, target.vx, 0.0001);
        assertSame(target, getField(eagle, "grabbedTarget"));
        assertSame(eagle, getField(target, "grabbedBy"));
        assertEquals(12, getInt(eagle, "grabThrowLockTimer"));
    }

    @Test
    void eagleThrowsCoverFourDifferentLaunchRoles() throws Exception {
        ThrowOutcome forward = performEagleThrow("FORWARD");
        ThrowOutcome back = performEagleThrow("BACK");
        ThrowOutcome up = performEagleThrow("UP");
        ThrowOutcome down = performEagleThrow("DOWN");

        assertTrue(forward.vx > 20.0);
        assertTrue(back.vx < -22.0);
        assertTrue(up.vy < forward.vy);
        assertTrue(down.damage > up.damage);
        assertEquals("Eagle Royal Release", forward.name);
        assertEquals("Eagle Wingcast", back.name);
        assertEquals("Eagle Talon Hoist", up.name);
        assertEquals("Eagle Summit Drop", down.name);
    }

    @Test
    void eagleAerialUsesItsOwnAttackPoseInsteadOfFalconsGenericPose() throws Exception {
        BirdGame3 game = new BirdGame3();
        Bird eagle = airborneBird(game, BirdGame3.BirdType.EAGLE, 0, 320.0);
        Bird falcon = airborneBird(game, BirdGame3.BirdType.FALCON, 1, 320.0);
        Object upAir = enumConstant("com.example.birdgame3.Bird$NormalAttackVariant", "UP_AIR");

        invoke(eagle, "performAttack", 0, upAir);
        invoke(falcon, "performAttack", 0, upAir);

        Object eaglePose = invoke(eagle, "currentTargetAttackVisualPose");
        Object falconPose = invoke(falcon, "currentTargetAttackVisualPose");
        assertNotEquals(falconPose, eaglePose,
                "Eagle's authored wing-and-talon pose must not collapse to Falcon's shared pose.");
    }

    @Test
    void eagleAttackIdentityRoundTripsThroughLanState() throws Exception {
        BirdGame3 game = new BirdGame3();
        Bird source = airborneBird(game, BirdGame3.BirdType.EAGLE, 0, 320.0);
        Object backAir = enumConstant("com.example.birdgame3.Bird$NormalAttackVariant", "BACK_AIR");
        invoke(source, "performAttack", 0, backAir);

        Bird restored = airborneBird(game, BirdGame3.BirdType.EAGLE, 0, 320.0);
        restored.applyLanState(source.toLanState());

        assertEquals("BACK_AIR", activeAttackVariantName(restored));
        assertEquals(source.attackAnimationTimer, restored.attackAnimationTimer);
    }

    private static ThrowOutcome performEagleThrow(String directionName) throws Exception {
        BirdGame3 game = new BirdGame3();
        game.activePlayers = 2;
        Bird eagle = groundedBird(game, BirdGame3.BirdType.EAGLE, 0, 320.0);
        Bird target = groundedBird(game, BirdGame3.BirdType.FALCON, 1, 385.0);
        game.players[0] = eagle;
        game.players[1] = target;
        linkGrab(eagle, target);
        Object direction = enumConstant("com.example.birdgame3.Bird$GrabThrowDirection", directionName);
        String name = (String) invoke(eagle, "throwTelemetryName", direction);
        double before = target.health;
        invoke(eagle, "performThrow", direction);
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

    private static Bird airborneBird(BirdGame3 game, BirdGame3.BirdType type, int index, double x) {
        Bird bird = groundedBird(game, type, index, x);
        bird.y -= 220.0;
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

    private static void setField(Object target, String name, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        field.set(target, value);
    }

    private record ThrowOutcome(double damage, double vx, double vy, String name) {
    }
}
