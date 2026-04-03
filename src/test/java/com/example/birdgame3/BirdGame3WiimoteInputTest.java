package com.example.birdgame3;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BirdGame3WiimoteInputTest {

    @Test
    void stableWiimoteDirectionBridgesBriefDrops() throws Exception {
        BirdGame3 game = new BirdGame3();
        Method stableDirection = BirdGame3.class.getDeclaredMethod(
                "stableWiimoteDirection",
                int.class,
                boolean.class,
                boolean.class,
                long.class,
                boolean.class
        );
        stableDirection.setAccessible(true);

        long start = 1_000_000_000L;
        boolean initialHold = (boolean) stableDirection.invoke(game, 0, true, false, start, true);
        boolean briefDrop = (boolean) stableDirection.invoke(game, 0, false, false, start + 100_000_000L, true);
        boolean released = (boolean) stableDirection.invoke(game, 0, false, false, start + 340_000_000L, true);

        assertTrue(initialHold);
        assertTrue(briefDrop);
        assertFalse(released);
    }

    @Test
    void stableWiimoteDirectionStopsImmediatelyWhenOppositeDirectionAppears() throws Exception {
        BirdGame3 game = new BirdGame3();
        Method stableDirection = BirdGame3.class.getDeclaredMethod(
                "stableWiimoteDirection",
                int.class,
                boolean.class,
                boolean.class,
                long.class,
                boolean.class
        );
        stableDirection.setAccessible(true);

        long start = 2_000_000_000L;
        stableDirection.invoke(game, 0, true, false, start, true);
        boolean canceledByOpposite = (boolean) stableDirection.invoke(game, 0, false, true, start + 40_000_000L, true);

        assertFalse(canceledByOpposite);
    }

    @Test
    void stableWiimoteActionBridgesBriefDrops() throws Exception {
        BirdGame3 game = new BirdGame3();
        Class<?> controlActionClass = Class.forName("com.example.birdgame3.BirdGame3$ControlAction");
        Method stableAction = BirdGame3.class.getDeclaredMethod(
                "stableWiimoteAction",
                int.class,
                controlActionClass,
                boolean.class,
                long.class
        );
        stableAction.setAccessible(true);
        @SuppressWarnings("unchecked")
        Object attackAction = Enum.valueOf((Class<Enum>) controlActionClass.asSubclass(Enum.class), "ATTACK");

        long start = 3_000_000_000L;
        boolean initialHold = (boolean) stableAction.invoke(game, 0, attackAction, true, start);
        boolean briefDrop = (boolean) stableAction.invoke(game, 0, attackAction, false, start + 40_000_000L);
        boolean released = (boolean) stableAction.invoke(game, 0, attackAction, false, start + 100_000_000L);

        assertTrue(initialHold);
        assertTrue(briefDrop);
        assertFalse(released);
    }

    @Test
    void pauseMenuDirectionRepeatUsesHeldInput() throws Exception {
        BirdGame3 game = new BirdGame3();
        Method triggerPauseDirection = BirdGame3.class.getDeclaredMethod(
                "shouldTriggerPauseMenuDirection",
                int.class,
                boolean.class,
                long.class
        );
        triggerPauseDirection.setAccessible(true);

        long start = 4_000_000_000L;
        boolean initialHold = (boolean) triggerPauseDirection.invoke(game, 1, true, start);
        boolean beforeRepeat = (boolean) triggerPauseDirection.invoke(game, 1, true, start + 120_000_000L);
        boolean repeatedHold = (boolean) triggerPauseDirection.invoke(game, 1, true, start + 300_000_000L);
        boolean released = (boolean) triggerPauseDirection.invoke(game, 1, false, start + 320_000_000L);
        boolean repress = (boolean) triggerPauseDirection.invoke(game, 1, true, start + 340_000_000L);

        assertTrue(initialHold);
        assertFalse(beforeRepeat);
        assertTrue(repeatedHold);
        assertFalse(released);
        assertTrue(repress);
    }
}
