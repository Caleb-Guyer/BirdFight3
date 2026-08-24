package com.example.birdgame3;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ControlSettingsPresentationTest {
    @Test
    void settingsOpenTheControlPageForTheDeviceInUse() {
        assertEquals(ControlSettingsPresentation.Page.KEYBOARD,
                ControlSettingsPresentation.pageFor(UiInputPrompts.Device.KEYBOARD_MOUSE));
        assertEquals(ControlSettingsPresentation.Page.CONTROLLER,
                ControlSettingsPresentation.pageFor(UiInputPrompts.Device.GAMEPAD));
        assertEquals(ControlSettingsPresentation.Page.WIIMOTE,
                ControlSettingsPresentation.pageFor(UiInputPrompts.Device.WIIMOTE_SIDEWAYS));
        assertEquals(ControlSettingsPresentation.Page.WIIMOTE,
                ControlSettingsPresentation.pageFor(UiInputPrompts.Device.WIIMOTE_NUNCHUK));
    }
}
