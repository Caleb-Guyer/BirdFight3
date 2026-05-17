package com.example.birdgame3;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class XboxInputManagerTest {

    @Test
    void xinputStructuresExposeTheFullNativeLayout() {
        XboxInputManager.XInputGamepad gamepad =
                assertDoesNotThrow(XboxInputManager.XInputGamepad::new);
        XboxInputManager.XInputState state =
                assertDoesNotThrow(XboxInputManager.XInputState::new);

        assertEquals(12, gamepad.size());
        assertEquals(16, state.size());
    }

    @Test
    void dpadAndStickExposeVerticalAttackIntentWithoutChangingJumpOrBlock() throws Exception {
        XboxInputManager manager = new XboxInputManager();
        XboxInputManager.XInputState state = new XboxInputManager.XInputState();
        state.gamepad.wButtons = 0x0001;
        WiimoteMappedState dpadUp = map(manager, state);

        state = new XboxInputManager.XInputState();
        state.gamepad.sThumbLY = 12000;
        WiimoteMappedState stickUp = map(manager, state);

        state = new XboxInputManager.XInputState();
        state.gamepad.wButtons = 0x0002;
        WiimoteMappedState dpadDown = map(manager, state);

        state = new XboxInputManager.XInputState();
        state.gamepad.sThumbLY = -12000;
        WiimoteMappedState stickDown = map(manager, state);

        assertTrue(dpadUp.attackUp());
        assertTrue(stickUp.attackUp());
        assertTrue(dpadDown.attackDown());
        assertTrue(stickDown.attackDown());
        assertFalse(dpadUp.jump());
        assertFalse(dpadDown.block());
    }

    private WiimoteMappedState map(XboxInputManager manager, XboxInputManager.XInputState state) throws Exception {
        var method = XboxInputManager.class.getDeclaredMethod("mapState", XboxInputManager.XInputState.class, int.class);
        method.setAccessible(true);
        return (WiimoteMappedState) method.invoke(manager, state, 0);
    }
}
