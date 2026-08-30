package com.example.birdgame3;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GameplayFrameFailureGuardTest {

    @Test
    void repeatedFrameFailuresTripTheRecoveryCircuit() {
        BirdGame3.GameplayFrameFailureGuard guard = new BirdGame3.GameplayFrameFailureGuard(3);

        assertFalse(guard.recordFailure());
        assertFalse(guard.recordFailure());
        assertTrue(guard.recordFailure());
        assertEquals(3, guard.consecutiveFailures());
    }

    @Test
    void successfulFrameClearsTheFailureStreak() {
        BirdGame3.GameplayFrameFailureGuard guard = new BirdGame3.GameplayFrameFailureGuard(3);

        assertFalse(guard.recordFailure());
        assertFalse(guard.recordFailure());
        guard.recordSuccess();

        assertEquals(0, guard.consecutiveFailures());
        assertFalse(guard.recordFailure());
        assertFalse(guard.recordFailure());
        assertTrue(guard.recordFailure());
    }

    @Test
    void failureGuardRejectsAnImpossibleThreshold() {
        assertThrows(IllegalArgumentException.class,
                () -> new BirdGame3.GameplayFrameFailureGuard(0));
    }
}
