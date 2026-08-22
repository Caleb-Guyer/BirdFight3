package com.example.birdgame3;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;

class HummingbirdMovesetIdentityTest {
    @Test
    void hummingbirdHasAnAuthoredCompleteNormalKit() throws Exception {
        BirdGame3 game = new BirdGame3();
        Bird hummingbird = groundedBird(game, BirdGame3.BirdType.HUMMINGBIRD, 0, 320.0);
        Bird falcon = groundedBird(game, BirdGame3.BirdType.FALCON, 1, 320.0);
        Bird turkey = groundedBird(game, BirdGame3.BirdType.TURKEY, 2, 320.0);

        int distinctFromFalcon = 0;
        int distinctFromSharedProfile = 0;
        for (Object variant : normalAttackVariantClass().getEnumConstants()) {
            Object profile = invoke(hummingbird, "normalAttackProfile", variant);
            if (!profile.equals(invoke(falcon, "normalAttackProfile", variant))) distinctFromFalcon++;
            if (!profile.equals(invoke(turkey, "normalAttackProfile", variant))) distinctFromSharedProfile++;
            String moveName = (String) invoke(hummingbird, "normalAttackTelemetryName", variant);
            assertTrue(moveName.startsWith("Hummingbird "));
            assertFalse(moveName.contains("Normal Attack"));
            assertFalse(moveName.contains("Tilt"));
        }

        assertEquals(15, distinctFromFalcon);
        assertEquals(15, distinctFromSharedProfile);
    }

    @Test
    void hummingbirdPummelIsAFastPrecisionPrick() throws Exception {
        BirdGame3 game = new BirdGame3();
        game.activePlayers = 2;
        Bird hummingbird = groundedBird(game, BirdGame3.BirdType.HUMMINGBIRD, 0, 320.0);
        Bird target = groundedBird(game, BirdGame3.BirdType.TURKEY, 1, 385.0);
        game.players[0] = hummingbird;
        game.players[1] = target;
        linkGrab(hummingbird, target);
        game.setAiControlKey(0, game.attackKeyForPlayer(0), true);

        double healthBefore = target.health;
        assertTrue((boolean) invoke(hummingbird, "handleHoldingGrabState", false, false));

        assertTrue(target.health < healthBefore);
        assertSame(target, getField(hummingbird, "grabbedTarget"));
        assertSame(hummingbird, getField(target, "grabbedBy"));
        assertEquals(7, getInt(hummingbird, "grabThrowLockTimer"));
    }

    @Test
    void hummingbirdThrowsTradeDamageForFourPositioningRoles() throws Exception {
        ThrowOutcome forward = performThrow("FORWARD");
        ThrowOutcome back = performThrow("BACK");
        ThrowOutcome up = performThrow("UP");
        ThrowOutcome down = performThrow("DOWN");

        assertTrue(forward.vx > 17.0);
        assertTrue(back.vx < -18.0);
        assertTrue(up.vy < forward.vy);
        assertTrue(down.damage > up.damage);
        assertEquals("Hummingbird Nectar Sling", forward.name);
        assertEquals("Hummingbird Bloom Reversal", back.name);
        assertEquals("Hummingbird Petal Lift", up.name);
        assertEquals("Hummingbird Stamen Drop", down.name);
    }

    @Test
    void hummingbirdForwardAirUsesItsNeedleFlightPose() throws Exception {
        BirdGame3 game = new BirdGame3();
        Bird hummingbird = airborneBird(game, BirdGame3.BirdType.HUMMINGBIRD, 0, 320.0);
        Bird falcon = airborneBird(game, BirdGame3.BirdType.FALCON, 1, 320.0);
        Bird turkey = airborneBird(game, BirdGame3.BirdType.TURKEY, 2, 320.0);
        Object forwardAir = enumConstant("com.example.birdgame3.Bird$NormalAttackVariant", "FORWARD_AIR");

        invoke(hummingbird, "performAttack", 0, forwardAir);
        invoke(falcon, "performAttack", 0, forwardAir);
        invoke(turkey, "performAttack", 0, forwardAir);

        Object pose = invoke(hummingbird, "currentTargetAttackVisualPose");
        assertNotEquals(invoke(falcon, "currentTargetAttackVisualPose"), pose);
        assertNotEquals(invoke(turkey, "currentTargetAttackVisualPose"), pose);
    }

    @Test
    void hummingbirdAttackIdentityRoundTripsThroughLanState() throws Exception {
        BirdGame3 game = new BirdGame3();
        Bird source = airborneBird(game, BirdGame3.BirdType.HUMMINGBIRD, 0, 320.0);
        Object downAir = enumConstant("com.example.birdgame3.Bird$NormalAttackVariant", "DOWN_AIR");
        invoke(source, "performAttack", 0, downAir);

        Bird restored = airborneBird(game, BirdGame3.BirdType.HUMMINGBIRD, 0, 320.0);
        restored.applyLanState(source.toLanState());

        assertEquals("DOWN_AIR", ((Enum<?>) getField(restored, "activeAttackVariant")).name());
        assertEquals(source.attackAnimationTimer, restored.attackAnimationTimer);
    }

    private static ThrowOutcome performThrow(String directionName) throws Exception {
        BirdGame3 game = new BirdGame3();
        game.activePlayers = 2;
        Bird hummingbird = groundedBird(game, BirdGame3.BirdType.HUMMINGBIRD, 0, 320.0);
        Bird target = groundedBird(game, BirdGame3.BirdType.TURKEY, 1, 385.0);
        game.players[0] = hummingbird;
        game.players[1] = target;
        linkGrab(hummingbird, target);
        Object direction = enumConstant("com.example.birdgame3.Bird$GrabThrowDirection", directionName);
        String name = (String) invoke(hummingbird, "throwTelemetryName", direction);
        double before = target.health;
        invoke(hummingbird, "performThrow", direction);
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

    private static Bird airborneBird(BirdGame3 game, BirdGame3.BirdType type, int index, double x) {
        Bird bird = groundedBird(game, type, index, x);
        bird.y -= 220.0;
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
