package com.example.birdgame3;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;

class PhoenixMovesetIdentityTest {
    @Test
    void phoenixHasAnAuthoredCompleteNormalKit() throws Exception {
        BirdGame3 game = new BirdGame3();
        Bird phoenix = groundedBird(game, BirdGame3.BirdType.PHOENIX, 0, 320.0);
        Bird falcon = groundedBird(game, BirdGame3.BirdType.FALCON, 1, 320.0);
        Bird turkey = groundedBird(game, BirdGame3.BirdType.TURKEY, 2, 320.0);

        int distinctFromFalcon = 0;
        int distinctFromSharedProfile = 0;
        for (Object variant : normalAttackVariantClass().getEnumConstants()) {
            Object phoenixProfile = invoke(phoenix, "normalAttackProfile", variant);
            if (!phoenixProfile.equals(invoke(falcon, "normalAttackProfile", variant))) {
                distinctFromFalcon++;
            }
            if (!phoenixProfile.equals(invoke(turkey, "normalAttackProfile", variant))) {
                distinctFromSharedProfile++;
            }
            String moveName = (String) invoke(phoenix, "normalAttackTelemetryName", variant);
            assertTrue(moveName.startsWith("Phoenix "));
            assertFalse(moveName.contains("Normal Attack"));
            assertFalse(moveName.contains("Tilt"));
        }

        assertEquals(15, distinctFromFalcon);
        assertEquals(15, distinctFromSharedProfile,
                "Phoenix must not retain the shared ground and horizontal aerial profile.");
    }

    @Test
    void phoenixPummelBrandsTheTargetWithoutBreakingTheGrab() throws Exception {
        BirdGame3 game = new BirdGame3();
        game.activePlayers = 2;
        Bird phoenix = groundedBird(game, BirdGame3.BirdType.PHOENIX, 0, 320.0);
        Bird target = groundedBird(game, BirdGame3.BirdType.TURKEY, 1, 385.0);
        game.players[0] = phoenix;
        game.players[1] = target;
        linkGrab(phoenix, target);
        game.setAiControlKey(0, game.attackKeyForPlayer(0), true);

        double healthBefore = target.health;
        assertTrue((boolean) invoke(phoenix, "handleHoldingGrabState", false, false));

        assertTrue(target.health < healthBefore);
        assertEquals(0.0, target.vx, 0.0001);
        assertSame(target, getField(phoenix, "grabbedTarget"));
        assertSame(phoenix, getField(target, "grabbedBy"));
        assertEquals(10, getInt(phoenix, "grabThrowLockTimer"));
    }

    @Test
    void phoenixThrowsCoverFourFlameLaunchRoles() throws Exception {
        ThrowOutcome forward = performPhoenixThrow("FORWARD");
        ThrowOutcome back = performPhoenixThrow("BACK");
        ThrowOutcome up = performPhoenixThrow("UP");
        ThrowOutcome down = performPhoenixThrow("DOWN");

        assertTrue(forward.vx > 17.0);
        assertTrue(back.vx < -18.0);
        assertTrue(up.vy < forward.vy);
        assertTrue(down.damage > up.damage);
        assertEquals("Phoenix Cinder Sling", forward.name);
        assertEquals("Phoenix Ashen Reversal", back.name);
        assertEquals("Phoenix Flare Column", up.name);
        assertEquals("Phoenix Furnace Drop", down.name);
    }

    @Test
    void phoenixHorizontalAerialUsesItsOwnFlamePose() throws Exception {
        BirdGame3 game = new BirdGame3();
        Bird phoenix = airborneBird(game, BirdGame3.BirdType.PHOENIX, 0, 320.0);
        Bird falcon = airborneBird(game, BirdGame3.BirdType.FALCON, 1, 320.0);
        Bird turkey = airborneBird(game, BirdGame3.BirdType.TURKEY, 2, 320.0);
        Object backAir = enumConstant("com.example.birdgame3.Bird$NormalAttackVariant", "BACK_AIR");

        invoke(phoenix, "performAttack", 0, backAir);
        invoke(falcon, "performAttack", 0, backAir);
        invoke(turkey, "performAttack", 0, backAir);

        Object phoenixPose = invoke(phoenix, "currentTargetAttackVisualPose");
        assertNotEquals(invoke(falcon, "currentTargetAttackVisualPose"), phoenixPose);
        assertNotEquals(invoke(turkey, "currentTargetAttackVisualPose"), phoenixPose,
                "Phoenix's authored flame pose must cover more than its old vertical aerial pair.");
    }

    @Test
    void phoenixAttackIdentityRoundTripsThroughLanState() throws Exception {
        BirdGame3 game = new BirdGame3();
        Bird source = airborneBird(game, BirdGame3.BirdType.PHOENIX, 0, 320.0);
        Object forwardAir = enumConstant("com.example.birdgame3.Bird$NormalAttackVariant", "FORWARD_AIR");
        invoke(source, "performAttack", 0, forwardAir);

        Bird restored = airborneBird(game, BirdGame3.BirdType.PHOENIX, 0, 320.0);
        restored.applyLanState(source.toLanState());

        assertEquals("FORWARD_AIR", activeAttackVariantName(restored));
        assertEquals(source.attackAnimationTimer, restored.attackAnimationTimer);
    }

    private static ThrowOutcome performPhoenixThrow(String directionName) throws Exception {
        BirdGame3 game = new BirdGame3();
        game.activePlayers = 2;
        Bird phoenix = groundedBird(game, BirdGame3.BirdType.PHOENIX, 0, 320.0);
        Bird target = groundedBird(game, BirdGame3.BirdType.TURKEY, 1, 385.0);
        game.players[0] = phoenix;
        game.players[1] = target;
        linkGrab(phoenix, target);
        Object direction = enumConstant("com.example.birdgame3.Bird$GrabThrowDirection", directionName);
        String name = (String) invoke(phoenix, "throwTelemetryName", direction);
        double before = target.health;
        invoke(phoenix, "performThrow", direction);
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
