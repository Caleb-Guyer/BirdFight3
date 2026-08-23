package com.example.birdgame3;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UniversalInputTransportTest {
    @Test
    void replayMasksPreserveMovementShieldAndGrabInputsTogether() throws Exception {
        BirdGame3 game = new BirdGame3();
        game.activePlayers = 1;
        Bird bird = new Bird(500.0, BirdGame3.BirdType.PIGEON, 0, game);
        game.players[0] = bird;

        game.setLocalActionsForKey(game.leftKeyForPlayer(0), true);
        game.setLocalActionsForKey(game.jumpKeyForPlayer(0), true);
        game.setLocalActionsForKey(game.grabKeyForPlayer(0), true);
        game.setLocalActionsForKey(game.blockKeyForPlayer(0), true);
        int replayMask = (int) invoke(game, "composeHumanInputMask",
                new Class<?>[]{int.class}, 0);

        int expectedReplayMask = (1 << 0) | (1 << 2) | (1 << 5) | (1 << 6);
        assertEquals(expectedReplayMask, replayMask);

        game.setLocalActionsForKey(game.leftKeyForPlayer(0), false);
        game.setLocalActionsForKey(game.jumpKeyForPlayer(0), false);
        game.setLocalActionsForKey(game.grabKeyForPlayer(0), false);
        game.setLocalActionsForKey(game.blockKeyForPlayer(0), false);
        MatchReplay replay = new MatchReplay(42L, 1);
        replay.frames.add(new int[]{replayMask});
        setField(game, "activeReplay", replay);
        invoke(game, "loadReplayFrame", new Class<?>[0]);

        assertTrue(bird.leftPressed());
        assertTrue(bird.jumpPressed());
        assertTrue(bird.blockPressed());
        assertTrue((boolean) invoke(bird, "grabPressed", new Class<?>[0]));
    }

    @Test
    void lockstepMasksPreserveMovementShieldAndGrabInputsTogether() throws Exception {
        BirdGame3 game = new BirdGame3();
        game.activePlayers = 1;
        Bird bird = new Bird(500.0, BirdGame3.BirdType.PIGEON, 0, game);
        game.players[0] = bird;

        game.setLocalActionsForKey(game.rightKeyForPlayer(0), true);
        game.setLocalActionsForKey(game.jumpKeyForPlayer(0), true);
        game.setLocalActionsForKey(game.grabKeyForPlayer(0), true);
        game.setLocalActionsForKey(game.blockKeyForPlayer(0), true);
        int wireMask = (int) invoke(game, "sampleLocalLockstepMask", new Class<?>[0]);
        int expectedWireMask = LanProtocol.INPUT_RIGHT | LanProtocol.INPUT_JUMP
                | LanProtocol.INPUT_GRAB | LanProtocol.INPUT_BLOCK;
        assertEquals(expectedWireMask, wireMask);

        invoke(game, "applyLockstepBundle", new Class<?>[]{int[].class},
                (Object) new int[]{wireMask, 0, 0, 0});

        assertTrue(bird.rightPressed());
        assertTrue(bird.jumpPressed());
        assertTrue(bird.blockPressed());
        assertTrue((boolean) invoke(bird, "grabPressed", new Class<?>[0]));
    }

    private static Object invoke(Object target, String name, Class<?>[] parameterTypes,
                                 Object... arguments) throws Exception {
        Method method = target.getClass().getDeclaredMethod(name, parameterTypes);
        method.setAccessible(true);
        return method.invoke(target, arguments);
    }

    private static void setField(Object target, String name, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        field.set(target, value);
    }
}
