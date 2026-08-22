package com.example.birdgame3;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;

class FalconMovesetIdentityTest {
    @Test
    void falconHasAnAuthoredCompleteNormalKit() throws Exception {
        BirdGame3 game = new BirdGame3();
        Bird falcon = groundedBird(game, BirdGame3.BirdType.FALCON, 0, 320.0);
        Bird eagle = groundedBird(game, BirdGame3.BirdType.EAGLE, 1, 320.0);
        Bird phoenix = groundedBird(game, BirdGame3.BirdType.PHOENIX, 2, 320.0);

        int distinctFromEagle = 0;
        int distinctFromSharedRaptor = 0;
        for (Object variant : normalAttackVariantClass().getEnumConstants()) {
            Object falconProfile = invoke(falcon, "normalAttackProfile", variant);
            if (!falconProfile.equals(invoke(eagle, "normalAttackProfile", variant))) {
                distinctFromEagle++;
            }
            if (!falconProfile.equals(invoke(phoenix, "normalAttackProfile", variant))) {
                distinctFromSharedRaptor++;
            }
            String moveName = (String) invoke(falcon, "normalAttackTelemetryName", variant);
            assertTrue(moveName.startsWith("Falcon "));
            assertFalse(moveName.contains("Normal Attack"));
            assertFalse(moveName.contains("Tilt"));
        }

        assertEquals(15, distinctFromEagle);
        assertEquals(15, distinctFromSharedRaptor,
                "Falcon must not silently fall back to the shared normal profile.");
    }

    @Test
    void falconPummelIsAQuickPinionJabThatKeepsTheGrab() throws Exception {
        BirdGame3 game = new BirdGame3();
        game.activePlayers = 2;
        Bird falcon = groundedBird(game, BirdGame3.BirdType.FALCON, 0, 320.0);
        Bird target = groundedBird(game, BirdGame3.BirdType.PHOENIX, 1, 385.0);
        game.players[0] = falcon;
        game.players[1] = target;
        linkGrab(falcon, target);
        game.setAiControlKey(0, game.attackKeyForPlayer(0), true);

        double healthBefore = target.health;
        assertTrue((boolean) invoke(falcon, "handleHoldingGrabState", false, false));

        assertTrue(target.health < healthBefore);
        assertEquals(0.0, target.vx, 0.0001);
        assertSame(target, getField(falcon, "grabbedTarget"));
        assertSame(falcon, getField(target, "grabbedBy"));
        assertEquals(8, getInt(falcon, "grabThrowLockTimer"));
    }

    @Test
    void falconThrowsCoverFourFastPositioningRoles() throws Exception {
        ThrowOutcome forward = performFalconThrow("FORWARD");
        ThrowOutcome back = performFalconThrow("BACK");
        ThrowOutcome up = performFalconThrow("UP");
        ThrowOutcome down = performFalconThrow("DOWN");

        assertTrue(forward.vx > 18.0);
        assertTrue(back.vx < -20.0);
        assertTrue(up.vy < forward.vy);
        assertTrue(down.damage > up.damage);
        assertEquals("Falcon Slipstream Toss", forward.name);
        assertEquals("Falcon Wake Reversal", back.name);
        assertEquals("Falcon Thermal Launch", up.name);
        assertEquals("Falcon Runway Bounce", down.name);
    }

    @Test
    void falconAerialUsesItsOwnStreamlinedAttackPose() throws Exception {
        BirdGame3 game = new BirdGame3();
        Bird falcon = airborneBird(game, BirdGame3.BirdType.FALCON, 0, 320.0);
        Bird eagle = airborneBird(game, BirdGame3.BirdType.EAGLE, 1, 320.0);
        Bird phoenix = airborneBird(game, BirdGame3.BirdType.PHOENIX, 2, 320.0);
        Object forwardAir = enumConstant("com.example.birdgame3.Bird$NormalAttackVariant", "FORWARD_AIR");

        invoke(falcon, "performAttack", 0, forwardAir);
        invoke(eagle, "performAttack", 0, forwardAir);
        invoke(phoenix, "performAttack", 0, forwardAir);

        Object falconPose = invoke(falcon, "currentTargetAttackVisualPose");
        assertNotEquals(invoke(eagle, "currentTargetAttackVisualPose"), falconPose);
        assertNotEquals(invoke(phoenix, "currentTargetAttackVisualPose"), falconPose,
                "Falcon's speed-line pose must not collapse to the shared raptor pose.");
    }

    @Test
    void falconAttackIdentityRoundTripsThroughLanState() throws Exception {
        BirdGame3 game = new BirdGame3();
        Bird source = airborneBird(game, BirdGame3.BirdType.FALCON, 0, 320.0);
        Object downAir = enumConstant("com.example.birdgame3.Bird$NormalAttackVariant", "DOWN_AIR");
        invoke(source, "performAttack", 0, downAir);

        Bird restored = airborneBird(game, BirdGame3.BirdType.FALCON, 0, 320.0);
        restored.applyLanState(source.toLanState());

        assertEquals("DOWN_AIR", activeAttackVariantName(restored));
        assertEquals(source.attackAnimationTimer, restored.attackAnimationTimer);
    }

    private static ThrowOutcome performFalconThrow(String directionName) throws Exception {
        BirdGame3 game = new BirdGame3();
        game.activePlayers = 2;
        Bird falcon = groundedBird(game, BirdGame3.BirdType.FALCON, 0, 320.0);
        Bird target = groundedBird(game, BirdGame3.BirdType.PHOENIX, 1, 385.0);
        game.players[0] = falcon;
        game.players[1] = target;
        linkGrab(falcon, target);
        Object direction = enumConstant("com.example.birdgame3.Bird$GrabThrowDirection", directionName);
        String name = (String) invoke(falcon, "throwTelemetryName", direction);
        double before = target.health;
        invoke(falcon, "performThrow", direction);
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
