package com.example.birdgame3;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LockstepSessionTest {

    private static LockstepSession hostWithOneClient() {
        return new LockstepSession(new boolean[]{true, true, false, false});
    }

    @Test
    void initialDelayTicksArePreSeededNeutral() {
        LockstepSession session = hostWithOneClient();
        for (long t = 1; t <= LockstepSession.INPUT_DELAY_TICKS; t++) {
            assertArrayEquals(new int[LockstepSession.MAX_SLOTS], session.bundleFor(t),
                    "Tick " + t + " should be pre-seeded neutral.");
        }
        assertNull(session.bundleFor(LockstepSession.INPUT_DELAY_TICKS + 1));
    }

    @Test
    void internetDelaySeedsItsEntireNegotiatedBuffer() {
        LockstepSession session = new LockstepSession(
                new boolean[]{true, true, false, false},
                LockstepSession.INTERNET_INPUT_DELAY_TICKS);

        assertEquals(LockstepSession.INTERNET_INPUT_DELAY_TICKS, session.inputDelayTicks());
        for (long tick = 1; tick <= LockstepSession.INTERNET_INPUT_DELAY_TICKS; tick++) {
            assertNotNull(session.bundleFor(tick));
        }
        assertNull(session.bundleFor(LockstepSession.INTERNET_INPUT_DELAY_TICKS + 1L));
    }

    @Test
    void samplesEachTargetTickExactlyOnce() {
        LockstepSession session = hostWithOneClient();
        long target = LockstepSession.INPUT_DELAY_TICKS + 1;
        assertTrue(session.shouldSample(target));
        assertFalse(session.shouldSample(target), "Retrying the same tick must not resample.");
        assertFalse(session.shouldSample(target - 1), "Older ticks must not resample.");
        assertTrue(session.shouldSample(target + 1));
    }

    @Test
    void bundleCompletesOnlyWhenAllRequiredSlotsArrive() {
        LockstepSession session = hostWithOneClient();
        long tick = 10;
        assertNull(session.acceptMask(0, tick, 0b101), "Host mask alone must not complete.");
        assertNull(session.bundleFor(tick));

        int[] completed = session.acceptMask(1, tick, 0b010);
        assertNotNull(completed, "Second required mask completes the bundle.");
        assertEquals(0b101, completed[0]);
        assertEquals(0b010, completed[1]);
        assertArrayEquals(completed, session.bundleFor(tick));

        assertNull(session.acceptMask(1, tick, 0b111), "Late duplicates are ignored.");
        assertEquals(0b010, session.bundleFor(tick)[1]);
    }

    @Test
    void disconnectCompletesPendingBundles() {
        LockstepSession session = hostWithOneClient();
        session.acceptMask(0, 7, 1);
        session.acceptMask(0, 8, 2);

        List<Long> completed = session.slotDisconnected(1);

        assertEquals(List.of(7L, 8L), completed);
        assertEquals(1, session.bundleFor(7)[0]);
        assertEquals(0, session.bundleFor(7)[1], "The missing slot completes neutral.");
        assertNotNull(session.acceptMask(0, 9, 3),
                "After disconnect the host's mask alone completes new ticks.");
    }

    @Test
    void clientAcceptsHostBundles() {
        LockstepSession session = new LockstepSession(new boolean[]{true, true, false, false});
        int[] masks = {1, 2, 0, 0};
        session.acceptBundle(20, masks);
        assertArrayEquals(masks, session.bundleFor(20));
        session.acceptBundle(20, new int[]{9, 9, 9, 9});
        assertArrayEquals(masks, session.bundleFor(20), "First bundle for a tick wins.");
    }

    @Test
    void pruneDropsOldBundlesButKeepsRecent() {
        LockstepSession session = hostWithOneClient();
        session.acceptBundle(100, new int[4]);
        session.acceptBundle(130, new int[4]);
        session.prune(130);
        assertNull(session.bundleFor(100));
        assertNotNull(session.bundleFor(130));
    }

    @Test
    void hashMismatchIsDetectedRegardlessOfArrivalOrder() {
        LockstepSession session = hostWithOneClient();
        assertFalse(session.recordHash(120, 111L, false), "Local hash alone is not a mismatch.");
        assertFalse(session.recordHash(240, 222L, true), "Remote hash alone is not a mismatch.");
        assertFalse(session.recordHash(120, 111L, true), "Matching hashes are fine.");
        assertTrue(session.recordHash(240, 999L, false), "Differing hashes for one tick = desync.");
    }
}
