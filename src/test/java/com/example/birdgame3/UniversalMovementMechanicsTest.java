package com.example.birdgame3;

import javafx.scene.input.KeyCode;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UniversalMovementMechanicsTest {
    @Test
    void downJumpDropsThroughAuxiliaryPlatformAndLandsOnMainStage() throws Exception {
        BirdGame3 game = new BirdGame3();
        game.activePlayers = 1;
        game.platforms.clear();
        Platform mainStage = new Platform(100.0, BirdGame3.GROUND_Y - 70.0, 1_000.0, 36.0);
        Platform auxiliary = new Platform(300.0, BirdGame3.GROUND_Y - 310.0, 300.0, 28.0);
        game.platforms.add(mainStage);
        game.platforms.add(auxiliary);

        Bird bird = new Bird(380.0, BirdGame3.BirdType.EAGLE, 0, game);
        bird.y = auxiliary.y - bird.bodyHeight();
        game.players[0] = bird;
        double startingY = bird.y;

        KeyCode down = game.blockKeyForPlayer(0);
        KeyCode jump = game.jumpKeyForPlayer(0);
        game.setLocalActionsForKey(down, true);
        game.setLocalActionsForKey(jump, true);
        bird.update(1.0);

        assertTrue(getInt(bird, "platformDropTimer") > 0);
        assertTrue(bird.y > startingY, "A platform drop must immediately move below the support surface.");
        assertFalse(bird.isOnGround());
        assertEquals(0, getInt(bird, "jumpSquatTimer"));

        game.setLocalActionsForKey(down, false);
        game.setLocalActionsForKey(jump, false);
        for (int frame = 0; frame < 90 && !bird.isOnGround(); frame++) {
            bird.update(1.0);
        }

        assertTrue(bird.isOnGround(), "The fighter should land on the next valid support below.");
        assertEquals(mainStage.y, bird.bodyBottomY(), 0.01);
        assertEquals(0, getInt(bird, "platformDropTimer"));
    }

    @Test
    void downJumpCannotDropThroughMainStage() throws Exception {
        BirdGame3 game = new BirdGame3();
        game.activePlayers = 1;
        game.platforms.clear();
        Platform mainStage = new Platform(100.0, BirdGame3.GROUND_Y - 180.0, 1_000.0, 36.0);
        game.platforms.add(mainStage);

        Bird bird = new Bird(380.0, BirdGame3.BirdType.EAGLE, 0, game);
        bird.y = mainStage.y - bird.bodyHeight();
        game.players[0] = bird;
        double startingY = bird.y;

        game.setLocalActionsForKey(game.blockKeyForPlayer(0), true);
        game.setLocalActionsForKey(game.jumpKeyForPlayer(0), true);
        bird.update(1.0);

        assertEquals(0, getInt(bird, "platformDropTimer"));
        assertEquals(startingY, bird.y, 0.01, "The main stage must remain solid under down-jump.");
    }

    @Test
    void fastFallStartsAfterApexPersistsAndResetsOnLanding() throws Exception {
        BirdGame3 game = new BirdGame3();
        game.activePlayers = 1;
        Bird bird = new Bird(380.0, BirdGame3.BirdType.EAGLE, 0, game);
        bird.y = BirdGame3.GROUND_Y - 360.0;
        bird.vy = -5.0;
        game.players[0] = bird;

        game.setLocalActionsForKey(game.blockKeyForPlayer(0), true);
        setBoolean(bird, "blockHeldLastFrame", true);
        bird.update(1.0);
        assertFalse(getBoolean(bird, "fastFallActive"), "Holding down during ascent must not fast-fall early.");

        for (int frame = 0; frame < 30 && !getBoolean(bird, "fastFallActive"); frame++) {
            bird.update(1.0);
        }
        assertTrue(getBoolean(bird, "fastFallActive"), "Down should arm fast-fall once the apex is reached.");

        game.setLocalActionsForKey(game.blockKeyForPlayer(0), false);
        bird.update(1.0);
        assertTrue(getBoolean(bird, "fastFallActive"), "Fast-fall should persist after the input is released.");

        bird.y = BirdGame3.GROUND_Y - bird.bodyHeight() - 2.0;
        bird.vy = 8.0;
        bird.update(1.0);
        assertTrue(bird.isOnGround());
        assertFalse(getBoolean(bird, "fastFallActive"), "Landing must refresh fast-fall state.");
    }

    @Test
    void activeDashCanReverseImmediatelyWithoutWaitingForCooldown() throws Exception {
        BirdGame3 game = new BirdGame3();
        game.activePlayers = 1;
        Bird bird = new Bird(380.0, BirdGame3.BirdType.EAGLE, 0, game);
        bird.y = BirdGame3.GROUND_Y - bird.bodyHeight();
        game.players[0] = bird;

        game.simTick = 10L;
        bird.registerDashTap(1);
        game.simTick = 15L;
        bird.registerDashTap(1);
        assertTrue(getInt(bird, "dashTimer") > 0);
        assertTrue(getInt(bird, "dashCooldown") > 0);

        game.simTick = 16L;
        bird.registerDashTap(-1);

        assertEquals(-1, getInt(bird, "lastTapDir"));
        assertEquals(12, getInt(bird, "dashTimer"));
        assertEquals("DASH 12f", bird.debugUniversalActionLabel());
    }

    @Test
    void aerialAttackRetainsLimitedDirectionalDrift() throws Exception {
        BirdGame3 game = new BirdGame3();
        game.activePlayers = 1;
        Bird bird = new Bird(380.0, BirdGame3.BirdType.EAGLE, 0, game);
        bird.y = BirdGame3.GROUND_Y - 360.0;
        game.players[0] = bird;
        setBoolean(bird, "normalAttackTimelineActive", true);

        game.setLocalActionsForKey(game.rightKeyForPlayer(0), true);
        invokeHorizontalMovement(bird, false, true, false, false, 1.0);

        assertTrue(bird.vx > 0.0, "An aerial must still accept gentle directional drift during its timeline.");
        double firstFrameDrift = bird.vx;
        for (int frame = 0; frame < 120; frame++) {
            invokeHorizontalMovement(bird, false, true, false, false, 1.0);
        }

        assertTrue(bird.vx > firstFrameDrift);
        assertTrue(bird.vx < BirdGame3.BirdType.EAGLE.speed,
                "Attack drift must remain slower than unrestricted aerial movement.");

        game.setLocalActionsForKey(game.rightKeyForPlayer(0), false);
        game.setLocalActionsForKey(game.leftKeyForPlayer(0), true);
        double beforeReverse = bird.vx;
        invokeHorizontalMovement(bird, false, true, false, false, 1.0);
        assertTrue(bird.vx < beforeReverse, "Opposite input must influence an aerial without instantly reversing it.");
        assertTrue(bird.vx > 0.0, "One frame of drift must not erase aerial attack commitment.");
    }

    private static void invokeHorizontalMovement(Bird bird, boolean stunned, boolean airborne,
                                                 boolean jumpHeld, boolean jumpJustPressed,
                                                 double gameSpeed) throws Exception {
        Method method = Bird.class.getDeclaredMethod("handleHorizontalMovement",
                boolean.class, boolean.class, boolean.class, boolean.class, double.class);
        method.setAccessible(true);
        method.invoke(bird, stunned, airborne, jumpHeld, jumpJustPressed, gameSpeed);
    }

    private static int getInt(Object target, String fieldName) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        return field.getInt(target);
    }

    private static boolean getBoolean(Object target, String fieldName) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        return field.getBoolean(target);
    }

    private static void setBoolean(Object target, String fieldName, boolean value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.setBoolean(target, value);
    }
}
