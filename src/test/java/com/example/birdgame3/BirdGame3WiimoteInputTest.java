package com.example.birdgame3;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BirdGame3WiimoteInputTest {

    @Test
    void stableWiimoteDirectionBridgesBriefDrops() {
        OpposingDirectionStabilizer stabilizer = new OpposingDirectionStabilizer(2, 2);

        boolean initialHold = stabilizer.stabilize(0, true, false);
        boolean briefDrop = stabilizer.stabilize(0, false, false);
        boolean released = stabilizer.stabilize(0, false, false);

        assertTrue(initialHold);
        assertTrue(briefDrop);
        assertFalse(released);
    }

    @Test
    void stableWiimoteDirectionStopsImmediatelyWhenOppositeDirectionAppears() {
        OpposingDirectionStabilizer stabilizer = new OpposingDirectionStabilizer(2, 2);
        stabilizer.stabilize(0, true, false);
        boolean canceledByOpposite = stabilizer.stabilize(0, false, true);

        assertFalse(canceledByOpposite);
    }

    @Test
    void stableWiimoteActionBridgesBriefDrops() {
        DigitalHoldStabilizer stabilizer = new DigitalHoldStabilizer(8, 2);

        boolean initialHold = stabilizer.stabilize(1, true);
        boolean briefDrop = stabilizer.stabilize(1, false);
        boolean released = stabilizer.stabilize(1, false);

        assertTrue(initialHold);
        assertTrue(briefDrop);
        assertFalse(released);
    }

    @Test
    void pauseMenuDirectionRepeatUsesHeldInput() {
        HeldDirectionRepeater repeater = new HeldDirectionRepeater(4, 260_000_000L, 135_000_000L);

        long start = 4_000_000_000L;
        boolean initialHold = repeater.shouldTrigger(1, true, start);
        boolean beforeRepeat = repeater.shouldTrigger(1, true, start + 120_000_000L);
        boolean repeatedHold = repeater.shouldTrigger(1, true, start + 300_000_000L);
        boolean released = repeater.shouldTrigger(1, false, start + 320_000_000L);
        boolean repress = repeater.shouldTrigger(1, true, start + 340_000_000L);

        assertTrue(initialHold);
        assertFalse(beforeRepeat);
        assertTrue(repeatedHold);
        assertFalse(released);
        assertTrue(repress);
    }
}
