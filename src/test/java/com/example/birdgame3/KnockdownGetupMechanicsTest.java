package com.example.birdgame3;

import javafx.scene.input.KeyCode;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class KnockdownGetupMechanicsTest {
    @Test
    void missedTechOffersRollJumpAndAutomaticNeutralGetup() throws Exception {
        BirdGame3 game = smashGame();

        Bird roller = groundedBird(game, 0, 260.0);
        enterMissedTech(roller);
        hold(game, game.rightKeyForPlayer(0), true);
        roller.update(1.0);
        assertEquals(0, getInt(roller, "knockdownTimer"));
        assertEquals("ROLL", getObject(roller, "activeGetupOption").toString());
        assertTrue(roller.vx > 0.0);
        assertEquals(0, getInt(roller, "dodgeStaleLevel"));

        Bird jumper = groundedBird(game, 1, 520.0);
        enterMissedTech(jumper);
        hold(game, game.jumpKeyForPlayer(1), true);
        jumper.update(1.0);
        assertEquals("JUMP", getObject(jumper, "activeGetupOption").toString());
        assertTrue(getInt(jumper, "jumpSquatTimer") > 0);
        assertEquals(0, getInt(jumper, "knockdownTimer"));

        Bird automatic = groundedBird(game, 2, 780.0);
        enterMissedTech(automatic);
        for (int frame = 0; frame < 40 && getInt(automatic, "knockdownTimer") > 0; frame++) {
            automatic.update(1.0);
        }
        assertEquals("NEUTRAL", getObject(automatic, "activeGetupOption").toString());
        assertEquals("SPOT", getObject(automatic, "dodgeType").toString());
        assertTrue(automatic.debugUniversalActionLabel().startsWith("GETUP NEUTRAL"));
    }

    @Test
    void onlyTwoWeakGroundHitsCanJabLockAMissedTech() throws Exception {
        BirdGame3 game = smashGame();
        Bird attacker = groundedBird(game, 0, 300.0);
        Bird target = groundedBird(game, 1, 360.0);
        enterMissedTech(target);

        invokeNeutralAttackImpact(attacker, target);
        assertEquals(1, getInt(target, "jabLockCount"));
        assertTrue(getBoolean(target, "missedTechKnockdownActive"));
        assertEquals(0.0, target.vy, 0.0001);

        invokeNeutralAttackImpact(attacker, target);
        assertEquals(2, getInt(target, "jabLockCount"));
        assertTrue(target.debugHitReactionTelemetryLabel().contains("LOCK 2/2"));

        invokeNeutralAttackImpact(attacker, target);
        assertFalse(getBoolean(target, "missedTechKnockdownActive"));
        assertEquals(0, getInt(target, "knockdownTimer"));
        assertTrue(Math.abs(target.vx) > 0.1 || Math.abs(target.vy) > 0.1,
                "The third weak hit must launch instead of creating an infinite jab lock.");
    }

    @Test
    void getupAndJabLockStateRoundTripsThroughLanSnapshot() throws Exception {
        BirdGame3 game = smashGame();
        Bird source = groundedBird(game, 0, 300.0);
        enterMissedTech(source);
        invokeJabLockOnly(source);

        LanBirdState encoded = roundTrip(source.toLanState());
        Bird restored = new Bird(300.0, BirdGame3.BirdType.EAGLE, 0, game);
        restored.applyLanState(encoded);

        assertEquals(source.debugHitReactionTelemetryLabel(), restored.debugHitReactionTelemetryLabel());
        assertEquals(source.deterministicHitReactionStateHash(), restored.deterministicHitReactionStateHash());
    }

    @Test
    void higherLevelCpuSelectsAGetupInsteadOfWaitingOnTheFloor() throws Exception {
        BirdGame3 game = smashGame();
        game.isAI[0] = true;
        Field levelsField = BirdGame3.class.getDeclaredField("cpuLevels");
        levelsField.setAccessible(true);
        ((int[]) levelsField.get(game))[0] = 9;
        Bird cpu = groundedBird(game, 0, 300.0);
        enterMissedTech(cpu);

        for (int frame = 0; frame < 16 && getObject(cpu, "activeGetupOption").toString().equals("NONE"); frame++) {
            cpu.update(1.0);
        }

        assertFalse(getObject(cpu, "activeGetupOption").toString().equals("NONE"));
        assertEquals(0, getInt(cpu, "knockdownTimer"));
    }

    private static BirdGame3 smashGame() throws Exception {
        BirdGame3 game = new BirdGame3();
        game.activePlayers = 4;
        Field field = BirdGame3.class.getDeclaredField("smashCombatRulesActive");
        field.setAccessible(true);
        field.setBoolean(game, true);
        return game;
    }

    private static Bird groundedBird(BirdGame3 game, int playerIndex, double x) {
        Bird bird = new Bird(x, BirdGame3.BirdType.EAGLE, playerIndex, game);
        bird.y = BirdGame3.GROUND_Y - bird.bodyHeight();
        game.players[playerIndex] = bird;
        return bird;
    }

    private static void enterMissedTech(Bird bird) throws Exception {
        Method method = Bird.class.getDeclaredMethod("enterMissedTechKnockdown");
        method.setAccessible(true);
        method.invoke(bird);
    }

    private static void invokeNeutralAttackImpact(Bird attacker, Bird target) throws Exception {
        Class<?> variantType = Class.forName("com.example.birdgame3.Bird$NormalAttackVariant");
        @SuppressWarnings({"rawtypes", "unchecked"})
        Object neutral = Enum.valueOf((Class<? extends Enum>) variantType, "NEUTRAL");
        Method method = Bird.class.getDeclaredMethod("processBirdAttack", Bird.class, int.class,
                double.class, double.class, double.class, double.class, String.class, variantType);
        method.setAccessible(true);
        method.invoke(attacker, target, 4, 1.0, 1.0, 1.0, 1.0, "Jab Lock Test", neutral);
    }

    private static void invokeJabLockOnly(Bird bird) throws Exception {
        Class<?> variantType = Class.forName("com.example.birdgame3.Bird$NormalAttackVariant");
        @SuppressWarnings({"rawtypes", "unchecked"})
        Object neutral = Enum.valueOf((Class<? extends Enum>) variantType, "NEUTRAL");
        Method method = Bird.class.getDeclaredMethod("tryApplyJabLock", variantType);
        method.setAccessible(true);
        assertTrue((boolean) method.invoke(bird, neutral));
    }

    private static LanBirdState roundTrip(LanBirdState state) throws Exception {
        java.io.ByteArrayOutputStream bytes = new java.io.ByteArrayOutputStream();
        try (java.io.DataOutputStream out = new java.io.DataOutputStream(bytes)) {
            state.write(out);
        }
        try (java.io.DataInputStream in = new java.io.DataInputStream(
                new java.io.ByteArrayInputStream(bytes.toByteArray()))) {
            return LanBirdState.read(in);
        }
    }

    private static void hold(BirdGame3 game, KeyCode key, boolean pressed) {
        game.setLocalActionsForKey(key, pressed);
    }

    private static int getInt(Object target, String name) throws Exception {
        Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        return field.getInt(target);
    }

    private static boolean getBoolean(Object target, String name) throws Exception {
        Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        return field.getBoolean(target);
    }

    private static Object getObject(Object target, String name) throws Exception {
        Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        return field.get(target);
    }
}
