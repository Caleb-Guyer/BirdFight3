package com.example.birdgame3;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReplayStudioStateTest {
    @Test
    void newSessionStartsPlayingAtNormalSpeedWithFifteenSecondClip() {
        ReplayStudioState state = new ReplayStudioState();
        state.reset(6_000);

        assertEquals(6_000, state.totalFrames());
        assertFalse(state.paused());
        assertEquals(1.0, state.speed());
        assertEquals("1x", state.speedLabel());
        assertFalse(state.seeking());
        assertFalse(state.consumeStepRequest());
        assertEquals(0, state.clipStart());
        assertEquals(900, state.clipEnd());
        assertTrue(state.validClip());
    }

    @Test
    void resetDiscardsControlsAndMarkersFromPreviousReplay() {
        ReplayStudioState state = new ReplayStudioState();
        state.reset(6_000);
        state.slower();
        state.markIn(500);
        state.seek(3_000);
        state.requestStep();
        state.reset(120);

        assertFalse(state.paused());
        assertEquals(1.0, state.speed());
        assertFalse(state.consumeStepRequest());
        assertFalse(state.seeking());
        assertEquals(-1, state.seekTarget());
        assertEquals(0, state.clipStart());
        assertEquals(120, state.clipEnd());
        assertTrue(state.validClip());
    }

    @Test
    void speedControlsUseOnlySupportedSpeedsAndStopAtBothBounds() {
        ReplayStudioState state = new ReplayStudioState();
        state.reset(120);
        state.slower();
        assertEquals(0.5, state.speed());
        assertEquals("0.5x", state.speedLabel());
        state.slower();
        state.slower();
        assertEquals(0.25, state.speed());
        assertEquals("0.25x", state.speedLabel());
        state.faster();
        state.faster();
        state.faster();
        state.faster();
        assertEquals(2.0, state.speed());
        assertEquals("2x", state.speedLabel());
        assertFalse(state.paused());
    }

    @Test
    void stepPausesAndConsumesEachPressExactlyOnce() {
        ReplayStudioState state = new ReplayStudioState();
        state.reset(120);
        state.requestStep();
        state.requestStep();

        assertTrue(state.paused());
        assertTrue(state.consumeStepRequest());
        assertTrue(state.consumeStepRequest());
        assertFalse(state.consumeStepRequest());
        assertTrue(state.paused());
    }

    @Test
    void resumingClearsQueuedStepsSoTheyCannotRunAfterAnotherPause() {
        ReplayStudioState state = new ReplayStudioState();
        state.reset(120);
        state.requestStep();
        state.togglePaused();
        assertFalse(state.paused());
        state.togglePaused();
        assertTrue(state.paused());
        assertFalse(state.consumeStepRequest());
        state.requestStep();
        state.setPaused(false);
        assertFalse(state.consumeStepRequest());
    }

    @Test
    void seekClampsBothEndsAndCompletesOnlyAtItsTarget() {
        ReplayStudioState state = new ReplayStudioState();
        state.reset(120);
        state.seek(Integer.MAX_VALUE);

        assertTrue(state.seeking());
        assertTrue(state.paused());
        assertEquals(120, state.seekTarget());
        assertFalse(state.reachedSeek(119));
        assertTrue(state.reachedSeek(120));
        assertTrue(state.paused());
        assertFalse(state.seeking());
        assertFalse(state.reachedSeek(120));

        state.seek(Integer.MIN_VALUE);
        assertEquals(0, state.seekTarget());
        assertTrue(state.reachedSeek(0));
    }

    @Test
    void seekingAndSteppingReplaceEachOtherWithoutLeavingStaleRequests() {
        ReplayStudioState state = new ReplayStudioState();
        state.reset(120);
        state.requestStep();
        state.seek(50);
        assertFalse(state.consumeStepRequest());
        state.seek(60);
        assertEquals(60, state.seekTarget());
        state.requestStep();
        assertFalse(state.seeking());
        assertTrue(state.consumeStepRequest());
        state.seek(30);
        state.cancelSeek();
        assertFalse(state.reachedSeek(30));
        assertTrue(state.paused());
    }

    @Test
    void clipAllowsSingleFrameAndExactMaximumButRejectsOutOfBoundsEditsAtomically() {
        ReplayStudioState state = new ReplayStudioState();
        state.reset(10_000);
        assertTrue(state.markOut(ReplayStudioState.MAX_CLIP_FRAMES));
        assertEquals(ReplayStudioState.MAX_CLIP_FRAMES, state.clipEnd());
        assertEquals(0, state.clipStart());
        assertTrue(state.markIn(9_999));
        assertTrue(state.validClip());

        assertFalse(state.markIn(10_000));
        assertFalse(state.markOut(0));
        assertFalse(state.markIn(-1));
        assertFalse(state.markOut(10_001));
        assertFalse(state.markOut(Integer.MAX_VALUE));
        assertFalse(state.markIn(Integer.MIN_VALUE));
        assertEquals(9_999, state.clipStart());
        assertEquals(10_000, state.clipEnd());
        assertTrue(state.validClip());
    }

    @Test
    void markingInCanJumpAnywhereAndPreservesDurationUntilTheReplayEnd() {
        ReplayStudioState state = new ReplayStudioState();
        state.reset(10_000);
        assertTrue(state.markIn(7_000));
        assertEquals(7_000, state.clipStart());
        assertEquals(7_900, state.clipEnd());
        assertTrue(state.markIn(9_900));
        assertEquals(9_900, state.clipStart());
        assertEquals(10_000, state.clipEnd());
        assertTrue(state.markIn(100));
        assertEquals(100, state.clipStart());
        assertEquals(200, state.clipEnd());
        assertTrue(state.validClip());
    }

    @Test
    void markingOutBeforeInMovesInBackwardAndLimitsItAtReplayStart() {
        ReplayStudioState state = new ReplayStudioState();
        state.reset(10_000);
        state.markIn(7_000);
        assertTrue(state.markOut(5_000));
        assertEquals(4_100, state.clipStart());
        assertEquals(5_000, state.clipEnd());
        assertTrue(state.markOut(410));
        assertEquals(0, state.clipStart());
        assertEquals(410, state.clipEnd());
        assertTrue(state.validClip());
    }

    @Test
    void extendingOutMovesInOnlyWhenTheMaximumDurationWouldBeExceeded() {
        ReplayStudioState state = new ReplayStudioState();
        state.reset(10_000);
        state.markIn(1_000);
        assertTrue(state.markOut(2_000));
        assertEquals(1_000, state.clipStart());
        assertEquals(2_000, state.clipEnd());
        assertTrue(state.markOut(9_000));
        assertEquals(5_400, state.clipStart());
        assertEquals(9_000, state.clipEnd());
        assertTrue(state.validClip());
    }

    @Test
    void markingInNearTheLargestFrameCountCannotOverflowTheClipEnd() {
        ReplayStudioState state = new ReplayStudioState();
        state.reset(Integer.MAX_VALUE);
        assertTrue(state.markIn(Integer.MAX_VALUE - 1));
        assertEquals(Integer.MAX_VALUE - 1, state.clipStart());
        assertEquals(Integer.MAX_VALUE, state.clipEnd());
        assertTrue(state.validClip());
    }

    @Test
    void clipCanReachTheReplayEndWithoutExtendingPastIt() {
        ReplayStudioState state = new ReplayStudioState();
        state.reset(120);
        assertTrue(state.markIn(119));
        assertTrue(state.markOut(120));
        assertFalse(state.markOut(121));
        assertTrue(state.validClip());
        assertEquals(119, state.clipStart());
        assertEquals(120, state.clipEnd());
    }

    @Test
    void emptyReplayCannotProduceAPositiveClipAndStillSupportsSeekToStart() {
        ReplayStudioState state = new ReplayStudioState();
        state.reset(-10);
        assertEquals(0, state.totalFrames());
        assertEquals(0, state.clipStart());
        assertEquals(0, state.clipEnd());
        assertFalse(state.validClip());
        assertFalse(state.markIn(0));
        assertFalse(state.markOut(1));
        state.seek(100);
        assertEquals(0, state.seekTarget());
        assertTrue(state.reachedSeek(0));
    }

    @Test
    void timestampsAreDerivedFromSixtyHertzFramesWithoutOverflow() {
        assertEquals("0:00.000", ReplayStudioState.formatTimestamp(-1));
        assertEquals("0:00.000", ReplayStudioState.formatTimestamp(0));
        assertEquals("0:00.016", ReplayStudioState.formatTimestamp(1));
        assertEquals("0:00.983", ReplayStudioState.formatTimestamp(59));
        assertEquals("0:01.000", ReplayStudioState.formatTimestamp(60));
        assertEquals("1:00.000", ReplayStudioState.formatTimestamp(3_600));
        assertEquals("10:00.000", ReplayStudioState.formatTimestamp(36_000));
        assertEquals("596523:14.116", ReplayStudioState.formatTimestamp(Integer.MAX_VALUE));
    }
}
