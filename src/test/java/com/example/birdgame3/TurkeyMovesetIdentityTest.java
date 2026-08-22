package com.example.birdgame3;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;

class TurkeyMovesetIdentityTest {
    @Test
    void turkeyHasAnAuthoredCompleteHeavyweightNormalKit() throws Exception {
        BirdGame3 game = new BirdGame3();
        Bird turkey = groundedBird(game, BirdGame3.BirdType.TURKEY, 0, 320.0);
        Bird kiwi = groundedBird(game, BirdGame3.BirdType.KIWI, 1, 320.0);
        Bird shoebill = groundedBird(game, BirdGame3.BirdType.SHOEBILL, 2, 320.0);

        int distinctFromKiwi = 0;
        int distinctFromSharedProfile = 0;
        for (Object variant : normalAttackVariantClass().getEnumConstants()) {
            Object profile = invoke(turkey, "normalAttackProfile", variant);
            if (!profile.equals(invoke(kiwi, "normalAttackProfile", variant))) distinctFromKiwi++;
            if (!profile.equals(invoke(shoebill, "normalAttackProfile", variant))) distinctFromSharedProfile++;
            String moveName = (String) invoke(turkey, "normalAttackTelemetryName", variant);
            assertTrue(moveName.startsWith("Turkey "));
            assertFalse(moveName.contains("Normal Attack"));
            assertFalse(moveName.contains("Tilt"));
        }

        assertEquals(15, distinctFromKiwi);
        assertEquals(15, distinctFromSharedProfile);
    }

    @Test
    void turkeyPummelIsSlowHeavyDrumstickPressure() throws Exception {
        BirdGame3 game = new BirdGame3();
        game.activePlayers = 2;
        Bird turkey = groundedBird(game, BirdGame3.BirdType.TURKEY, 0, 320.0);
        Bird target = groundedBird(game, BirdGame3.BirdType.PIGEON, 1, 385.0);
        game.players[0] = turkey;
        game.players[1] = target;
        linkGrab(turkey, target);
        game.setAiControlKey(0, game.attackKeyForPlayer(0), true);

        double healthBefore = target.health;
        assertTrue((boolean) invoke(turkey, "handleHoldingGrabState", false, false));

        double tunedDamage = 4.0 * turkey.type.damageDealtMult * target.type.damageTakenMult;
        assertEquals(tunedDamage, healthBefore - target.health, 0.001);
        assertSame(target, getField(turkey, "grabbedTarget"));
        assertSame(turkey, getField(target, "grabbedBy"));
        assertEquals(13, getInt(turkey, "grabThrowLockTimer"));
    }

    @Test
    void turkeyThrowsHaveFourDistinctHeavyweightRoles() throws Exception {
        ThrowOutcome forward = performThrow("FORWARD");
        ThrowOutcome back = performThrow("BACK");
        ThrowOutcome up = performThrow("UP");
        ThrowOutcome down = performThrow("DOWN");

        assertTrue(forward.vx >= 20.0);
        assertTrue(back.vx <= -22.0);
        assertTrue(up.vy < forward.vy);
        assertTrue(down.damage > up.damage);
        assertEquals("Turkey Feast Fling", forward.name);
        assertEquals("Turkey Tail-Fan Reversal", back.name);
        assertEquals("Turkey Wishbone Launch", up.name);
        assertEquals("Turkey Platter Drop", down.name);
    }

    @Test
    void turkeyBackAirUsesItsTailFanSlamPose() throws Exception {
        BirdGame3 game = new BirdGame3();
        Bird turkey = airborneBird(game, BirdGame3.BirdType.TURKEY, 0, 320.0);
        Bird kiwi = airborneBird(game, BirdGame3.BirdType.KIWI, 1, 320.0);
        Bird shoebill = airborneBird(game, BirdGame3.BirdType.SHOEBILL, 2, 320.0);
        Object backAir = enumConstant("com.example.birdgame3.Bird$NormalAttackVariant", "BACK_AIR");

        invoke(turkey, "performAttack", 0, backAir);
        invoke(kiwi, "performAttack", 0, backAir);
        invoke(shoebill, "performAttack", 0, backAir);

        Object pose = invoke(turkey, "currentTargetAttackVisualPose");
        assertNotEquals(invoke(kiwi, "currentTargetAttackVisualPose"), pose);
        assertNotEquals(invoke(shoebill, "currentTargetAttackVisualPose"), pose);
    }

    @Test
    void turkeyAttackIdentityRoundTripsThroughLanState() throws Exception {
        BirdGame3 game = new BirdGame3();
        Bird source = groundedBird(game, BirdGame3.BirdType.TURKEY, 0, 320.0);
        Object downSmash = enumConstant("com.example.birdgame3.Bird$NormalAttackVariant", "DOWN_SMASH");
        invoke(source, "performAttack", 0, downSmash);

        Bird restored = groundedBird(game, BirdGame3.BirdType.TURKEY, 0, 320.0);
        restored.applyLanState(source.toLanState());

        assertEquals("DOWN_SMASH", ((Enum<?>) getField(restored, "activeAttackVariant")).name());
        assertEquals(source.attackAnimationTimer, restored.attackAnimationTimer);
    }

    private static ThrowOutcome performThrow(String directionName) throws Exception {
        BirdGame3 game = new BirdGame3();
        game.activePlayers = 2;
        Bird turkey = groundedBird(game, BirdGame3.BirdType.TURKEY, 0, 320.0);
        Bird target = groundedBird(game, BirdGame3.BirdType.PIGEON, 1, 385.0);
        game.players[0] = turkey;
        game.players[1] = target;
        linkGrab(turkey, target);
        Object direction = enumConstant("com.example.birdgame3.Bird$GrabThrowDirection", directionName);
        String name = (String) invoke(turkey, "throwTelemetryName", direction);
        double before = target.health;
        invoke(turkey, "performThrow", direction);
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
