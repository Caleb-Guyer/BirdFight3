package com.example.birdgame3;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FightSetupInputPresentationTest {
    @Test
    void humanSlotsNameTheDeviceActuallyInUse() {
        FightSetupInputPresentation.Display keyboard = FightSetupInputPresentation.resolve(
                true, false, -1, false, false, UiInputPrompts.Device.KEYBOARD_MOUSE);
        FightSetupInputPresentation.Display wiimote = FightSetupInputPresentation.resolve(
                true, false, -1, false, false, UiInputPrompts.Device.WIIMOTE_NUNCHUK);

        assertEquals("KEYBOARD + MOUSE", keyboard.text());
        assertEquals("WII REMOTE + NUNCHUK", wiimote.text());
        assertFalse(wiimote.disabled());
    }

    @Test
    void controllerJoinAndDisconnectStatesAreExplicit() {
        FightSetupInputPresentation.Display awaiting = FightSetupInputPresentation.resolve(
                true, false, -1, false, true, UiInputPrompts.Device.KEYBOARD_MOUSE);
        FightSetupInputPresentation.Display connected = FightSetupInputPresentation.resolve(
                true, false, 1, true, false, UiInputPrompts.Device.GAMEPAD);
        FightSetupInputPresentation.Display lost = FightSetupInputPresentation.resolve(
                true, false, 1, false, false, UiInputPrompts.Device.GAMEPAD);

        assertEquals("A  CONNECT CONTROLLER", awaiting.text());
        assertEquals("CONTROLLER 2", connected.text());
        assertEquals("CONTROLLER 2 LOST", lost.text());
    }

    @Test
    void cpuAndClosedSlotsCannotRequestInput() {
        FightSetupInputPresentation.Display cpu = FightSetupInputPresentation.resolve(
                true, true, -1, false, false, UiInputPrompts.Device.GAMEPAD);
        FightSetupInputPresentation.Display closed = FightSetupInputPresentation.resolve(
                false, false, -1, false, false, UiInputPrompts.Device.GAMEPAD);

        assertEquals("CPU CONTROL", cpu.text());
        assertTrue(cpu.disabled());
        assertEquals("SLOT CLOSED", closed.text());
        assertTrue(closed.disabled());
        assertEquals(0.45, closed.opacity());
    }
}
