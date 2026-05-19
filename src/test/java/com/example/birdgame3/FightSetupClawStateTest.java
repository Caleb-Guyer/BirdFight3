package com.example.birdgame3;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FightSetupClawStateTest {
    @Test
    void initializesHomePositionFromDock() {
        FightSetupClawState state = new FightSetupClawState(2, 40.0, 54.0);

        state.moveHomeFromDock(0, 120.0, 180.0);
        state.moveHomeFromDock(1, 90.0, 60.0);

        assertEquals(120.0, state.x(0));
        assertEquals(126.0, state.y(0));
        assertEquals(90.0, state.x(1));
        assertEquals(40.0, state.y(1));
    }

    @Test
    void movementNormalizesDirectionAndClampsToBounds() {
        FightSetupClawState state = new FightSetupClawState(1, 40.0, 54.0);
        state.setPosition(0, 100.0, 100.0);

        assertTrue(state.moveByDirection(0, 3.0, 4.0, 0.5, 100.0, 300.0, 300.0));

        assertEquals(130.0, state.x(0));
        assertEquals(140.0, state.y(0));

        assertTrue(state.moveByDirection(0, -1.0, -1.0, 10.0, 100.0, 130.0, 150.0));

        assertEquals(40.0, state.x(0));
        assertEquals(40.0, state.y(0));
    }

    @Test
    void movementIgnoresZeroVector() {
        FightSetupClawState state = new FightSetupClawState(1, 40.0, 54.0);
        state.setPosition(0, 100.0, 100.0);

        assertFalse(state.moveByDirection(0, 0.0, 0.0, 1.0, 100.0, 300.0, 300.0));

        assertEquals(100.0, state.x(0));
        assertEquals(100.0, state.y(0));
    }

    @Test
    void grabStoresSelectorAndOffsetThenClears() {
        FightSetupClawState state = new FightSetupClawState(2, 40.0, 54.0);
        state.setPosition(0, 100.0, 120.0);

        assertTrue(state.beginGrab(0, 1, 130.0, 150.0));

        assertTrue(state.isGrabbing(0));
        assertEquals(1, state.grabbedSelector(0));
        assertEquals(130.0, state.grabbedSelectorX(0));
        assertEquals(150.0, state.grabbedSelectorY(0));
        assertTrue(state.selectorGrabbedByOtherClaw(1, 1));

        state.clearGrab(0);

        assertFalse(state.isGrabbing(0));
        assertEquals(-1, state.grabbedSelector(0));
        assertFalse(state.selectorGrabbedByOtherClaw(1, 1));
    }

    @Test
    void presentationReportsChangesOnlyWhenStateChanges() {
        FightSetupClawState state = new FightSetupClawState(1, 40.0, 54.0);

        FightSetupClawState.PresentationChange first = state.setPresentation(0, true, false);
        assertTrue(first.visibleChanged());
        assertFalse(first.closedChanged());
        assertTrue(first.needsImageRefresh());

        FightSetupClawState.PresentationChange second = state.setPresentation(0, true, false);
        assertFalse(second.visibleChanged());
        assertFalse(second.closedChanged());
        assertFalse(second.needsImageRefresh());

        FightSetupClawState.PresentationChange third = state.setPresentation(0, true, true);
        assertFalse(third.visibleChanged());
        assertTrue(third.closedChanged());
        assertTrue(third.needsImageRefresh());
    }

    @Test
    void negativeClawCountIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> new FightSetupClawState(-1, 40.0, 54.0));
    }
}
