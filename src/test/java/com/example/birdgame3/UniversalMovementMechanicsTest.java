package com.example.birdgame3;

import javafx.scene.input.KeyCode;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;

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
