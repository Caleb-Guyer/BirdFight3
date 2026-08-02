package com.example.birdgame3;

import javafx.scene.input.KeyCode;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BirdGame3LockstepInputTest {
    @Test
    void hostLiveDirectionEdgesWaitForAuthoritativeBundleBeforeRegisteringDash() throws Exception {
        BirdGame3 game = lockstepHostGame();
        Bird hostBird = groundedHostBird(game);
        KeyCode right = game.rightKeyForPlayer(0);

        game.simTick = 1;
        invokeRegisterDashTapForKey(game, right);
        game.simTick = 2;
        invokeRegisterDashTapForKey(game, right);

        assertEquals(0, privateInt(hostBird, "dashTimer"),
                "Live host input must not mutate deterministic dash state before its bundle executes.");

        game.simTick = 9;
        invokeApplyLockstepBundle(game, LanProtocol.INPUT_RIGHT);
        game.simTick = 10;
        invokeApplyLockstepBundle(game, 0);
        game.simTick = 11;
        invokeApplyLockstepBundle(game, LanProtocol.INPUT_RIGHT);

        assertEquals(12, privateInt(hostBird, "dashTimer"),
                "The same direction edges must still register when applied from the shared bundle.");
    }

    @Test
    void offlineLiveDirectionEdgesStillRegisterDashNormally() throws Exception {
        BirdGame3 game = new BirdGame3();
        Bird bird = groundedHostBird(game);
        KeyCode right = game.rightKeyForPlayer(0);

        game.simTick = 1;
        invokeRegisterDashTapForKey(game, right);
        game.simTick = 2;
        invokeRegisterDashTapForKey(game, right);

        assertEquals(12, privateInt(bird, "dashTimer"));
    }

    private static BirdGame3 lockstepHostGame() throws Exception {
        BirdGame3 game = new BirdGame3();
        game.lanModeActive = true;
        game.lanIsHost = true;
        game.lanMatchActive = true;
        Field session = BirdGame3.class.getDeclaredField("lockstepSession");
        session.setAccessible(true);
        session.set(game, new LockstepSession(new boolean[]{true, true, false, false}));
        return game;
    }

    private static Bird groundedHostBird(BirdGame3 game) {
        game.activePlayers = 2;
        Bird bird = new Bird(500.0, BirdGame3.BirdType.PIGEON, 0, game);
        bird.y = BirdGame3.GROUND_Y - 80.0;
        game.players[0] = bird;
        return bird;
    }

    private static void invokeRegisterDashTapForKey(BirdGame3 game, KeyCode code) throws Exception {
        Method method = BirdGame3.class.getDeclaredMethod("registerDashTapForKey", KeyCode.class);
        method.setAccessible(true);
        method.invoke(game, code);
    }

    private static void invokeApplyLockstepBundle(BirdGame3 game, int hostMask) throws Exception {
        Method method = BirdGame3.class.getDeclaredMethod("applyLockstepBundle", int[].class);
        method.setAccessible(true);
        method.invoke(game, (Object) new int[]{hostMask, 0, 0, 0});
    }

    private static int privateInt(Object target, String name) throws Exception {
        Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        return field.getInt(target);
    }
}
