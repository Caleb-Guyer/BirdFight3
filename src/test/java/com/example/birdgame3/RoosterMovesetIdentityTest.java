package com.example.birdgame3;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;

class RoosterMovesetIdentityTest {
    @Test
    void roosterHasAnAuthoredCompleteCommanderRushdownKit() throws Exception {
        BirdGame3 game = new BirdGame3();
        Bird rooster = groundedBird(game, BirdGame3.BirdType.ROOSTER, 0, 320.0);
        Bird turkey = groundedBird(game, BirdGame3.BirdType.TURKEY, 1, 320.0);
        Bird penguin = groundedBird(game, BirdGame3.BirdType.PENGUIN, 2, 320.0);

        int distinctFromTurkey = 0;
        int distinctFromSharedProfile = 0;
        for (Object variant : normalAttackVariantClass().getEnumConstants()) {
            Object profile = invoke(rooster, "normalAttackProfile", variant);
            if (!profile.equals(invoke(turkey, "normalAttackProfile", variant))) distinctFromTurkey++;
            if (!profile.equals(invoke(penguin, "normalAttackProfile", variant))) distinctFromSharedProfile++;
            String moveName = (String) invoke(rooster, "normalAttackTelemetryName", variant);
            assertTrue(moveName.startsWith("Rooster "));
            assertFalse(moveName.contains("Normal Attack"));
            assertFalse(moveName.contains("Tilt"));
        }

        assertEquals(15, distinctFromTurkey);
        assertEquals(15, distinctFromSharedProfile);
    }

    @Test
    void roosterPummelIsFastSpurPressure() throws Exception {
        BirdGame3 game = new BirdGame3();
        game.activePlayers = 2;
        Bird rooster = groundedBird(game, BirdGame3.BirdType.ROOSTER, 0, 320.0);
        Bird target = groundedBird(game, BirdGame3.BirdType.PIGEON, 1, 385.0);
        game.players[0] = rooster;
        game.players[1] = target;
        linkGrab(rooster, target);
        game.setAiControlKey(0, game.attackKeyForPlayer(0), true);

        double healthBefore = target.health;
        assertTrue((boolean) invoke(rooster, "handleHoldingGrabState", false, false));

        double tunedDamage = 3.0 * rooster.type.damageDealtMult * target.type.damageTakenMult;
        assertEquals(tunedDamage, healthBefore - target.health, 0.001);
        assertSame(target, getField(rooster, "grabbedTarget"));
        assertSame(rooster, getField(target, "grabbedBy"));
        assertEquals(9, getInt(rooster, "grabThrowLockTimer"));
    }

    @Test
    void roosterThrowsHaveFourDistinctFormationRoles() throws Exception {
        ThrowOutcome forward = performThrow("FORWARD");
        ThrowOutcome back = performThrow("BACK");
        ThrowOutcome up = performThrow("UP");
        ThrowOutcome down = performThrow("DOWN");

        assertTrue(forward.vx >= 19.5);
        assertTrue(back.vx <= -21.0);
        assertTrue(up.vy < forward.vy);
        assertTrue(down.damage > up.damage);
        assertEquals("Rooster Marching Order", forward.name);
        assertEquals("Rooster Sickle-Tail Toss", back.name);
        assertEquals("Rooster Wake-Up Call", up.name);
        assertEquals("Rooster Coop Drop", down.name);
    }

    @Test
    void roosterSideTiltUsesItsSpurKickPose() throws Exception {
        BirdGame3 game = new BirdGame3();
        Bird rooster = groundedBird(game, BirdGame3.BirdType.ROOSTER, 0, 320.0);
        Bird turkey = groundedBird(game, BirdGame3.BirdType.TURKEY, 1, 320.0);
        Bird penguin = groundedBird(game, BirdGame3.BirdType.PENGUIN, 2, 320.0);
        Object sideTilt = enumConstant("com.example.birdgame3.Bird$NormalAttackVariant", "SIDE_TILT");

        invoke(rooster, "performAttack", 0, sideTilt);
        invoke(turkey, "performAttack", 0, sideTilt);
        invoke(penguin, "performAttack", 0, sideTilt);

        Object pose = invoke(rooster, "currentTargetAttackVisualPose");
        assertNotEquals(invoke(turkey, "currentTargetAttackVisualPose"), pose);
        assertNotEquals(invoke(penguin, "currentTargetAttackVisualPose"), pose);
    }

    @Test
    void roosterAttackIdentityRoundTripsThroughLanState() throws Exception {
        BirdGame3 game = new BirdGame3();
        Bird source = groundedBird(game, BirdGame3.BirdType.ROOSTER, 0, 320.0);
        Object dashAttack = enumConstant("com.example.birdgame3.Bird$NormalAttackVariant", "DASH_ATTACK");
        invoke(source, "performAttack", 0, dashAttack);

        Bird restored = groundedBird(game, BirdGame3.BirdType.ROOSTER, 0, 320.0);
        restored.applyLanState(source.toLanState());

        assertEquals("DASH_ATTACK", ((Enum<?>) getField(restored, "activeAttackVariant")).name());
        assertEquals(source.attackAnimationTimer, restored.attackAnimationTimer);
    }

    @Test
    void roosterAiPreservesClosePressureAndCommandsAtMidrange() throws Exception {
        BirdGame3 game = new BirdGame3();
        game.activePlayers = 2;
        Bird rooster = groundedBird(game, BirdGame3.BirdType.ROOSTER, 0, 320.0);
        Bird target = groundedBird(game, BirdGame3.BirdType.PIGEON, 1, 320.0);
        game.players[0] = rooster;
        game.players[1] = target;
        rooster.update(1.0);

        assertEquals(Bird.ROOSTER_STARTING_CHICKS, invoke(rooster, "ownedRoosterChickCount"));
        assertFalse(rooster.shouldRoosterAIUseSpecial(target, 110.0, false, 0.0),
                "Point-blank Rooster should use its authored normals instead of interrupting pressure with commands.");

        assertTrue(rooster.shouldRoosterAIUseSpecial(target, 230.0, false, 0.0));
        assertEquals(Bird.DirectionalSpecialInput.SIDE,
                rooster.chooseRoosterAISpecialInput(target, 230.0, true, false),
                "A ready follower should be deployed before Rooster spends time summoning reinforcements.");

        rooster.roosterSideReuseTimer = 20;
        assertFalse(rooster.shouldRoosterAIUseSpecial(target, 230.0, false, 0.0),
                "A cooling command must not consume an AI combat decision.");
        assertTrue(rooster.shouldRoosterAIUseSpecial(target, 330.0, false, 0.0),
                "At long range Rooster may safely summon a reinforcement while its throw command cools down.");
        assertEquals(Bird.DirectionalSpecialInput.NEUTRAL,
                rooster.chooseRoosterAISpecialInput(target, 330.0, true, false));
    }

    private static ThrowOutcome performThrow(String directionName) throws Exception {
        BirdGame3 game = new BirdGame3();
        game.activePlayers = 2;
        Bird rooster = groundedBird(game, BirdGame3.BirdType.ROOSTER, 0, 320.0);
        Bird target = groundedBird(game, BirdGame3.BirdType.PIGEON, 1, 385.0);
        game.players[0] = rooster;
        game.players[1] = target;
        linkGrab(rooster, target);
        Object direction = enumConstant("com.example.birdgame3.Bird$GrabThrowDirection", directionName);
        String name = (String) invoke(rooster, "throwTelemetryName", direction);
        double before = target.health;
        invoke(rooster, "performThrow", direction);
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
