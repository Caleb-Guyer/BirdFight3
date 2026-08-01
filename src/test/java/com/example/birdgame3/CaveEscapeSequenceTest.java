package com.example.birdgame3;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CaveEscapeSequenceTest {
    @Test
    void pigeonDoesNotAutoRunWhenThePlayerProvidesNoInput() {
        CaveEscapeSequence escape = new CaveEscapeSequence(new BirdGame3());
        escape.resetForTest();
        double startX = escape.progressXForTest();

        for (int tick = 0; tick < 45; tick++) {
            escape.tickForTest();
        }

        assertEquals(startX, escape.progressXForTest(), 0.0001);
    }

    @Test
    void normalMovementAndHeldJumpControlTheEscapePigeon() {
        CaveEscapeSequence escape = new CaveEscapeSequence(new BirdGame3());
        escape.resetForTest();
        double startX = escape.progressXForTest();
        double startY = escape.verticalPositionForTest();

        escape.setControlsForTest(false, true, true, false, false, false);
        for (int tick = 0; tick < 12; tick++) {
            escape.tickForTest();
        }

        assertTrue(escape.progressXForTest() > startX + 30.0);
        assertTrue(escape.verticalPositionForTest() < startY - 40.0);
    }

    @Test
    void sideSpecialProvidesTheExpectedDirectionalBurst() {
        CaveEscapeSequence escape = new CaveEscapeSequence(new BirdGame3());
        escape.resetForTest();
        double startX = escape.progressXForTest();

        escape.setControlsForTest(false, true, false, false, false, true);
        escape.tickForTest();

        assertTrue(escape.progressXForTest() > startX + 15.0);
    }
}
