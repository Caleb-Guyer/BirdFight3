package com.example.birdgame3;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UiInputPromptsTest {
    private static final UiInputPrompts.Prompt[] TITLE_PROMPTS = {
            UiInputPrompts.prompt(UiInputPrompts.Command.START, "Start"),
            UiInputPrompts.prompt(UiInputPrompts.Command.BACK, "Exit"),
            UiInputPrompts.prompt(UiInputPrompts.Command.FULLSCREEN, "Fullscreen")
    };

    @Test
    void titlePromptShowsOnlyKeyboardBindingsForKeyboard() {
        String prompt = UiInputPrompts.render(UiInputPrompts.Device.KEYBOARD_MOUSE, TITLE_PROMPTS);
        assertEquals("ENTER  START   •   ESC  EXIT   •   F11  FULLSCREEN", prompt);
        assertFalse(prompt.contains(" / A"));
        assertFalse(prompt.contains("HOME"));
    }

    @Test
    void titlePromptShowsOnlyControllerBindingsForGamepad() {
        String prompt = UiInputPrompts.render(UiInputPrompts.Device.GAMEPAD, TITLE_PROMPTS);
        assertEquals("A  START   •   B  EXIT", prompt);
        assertFalse(prompt.contains("ENTER"));
        assertFalse(prompt.contains("F11"), "A controller must not advertise a keyboard-only fullscreen key");
    }

    @Test
    void titlePromptUsesTheConfiguredWiiRemoteLayout() {
        String sideways = UiInputPrompts.render(UiInputPrompts.Device.WIIMOTE_SIDEWAYS, TITLE_PROMPTS);
        String nunchuk = UiInputPrompts.render(UiInputPrompts.Device.WIIMOTE_NUNCHUK, TITLE_PROMPTS);

        assertTrue(sideways.contains("1 / 2  START"));
        assertTrue(sideways.contains("B / −  EXIT"));
        assertTrue(nunchuk.contains("A / C  START"));
        assertTrue(nunchuk.contains("B / Z  EXIT"));
        assertFalse(sideways.contains("ENTER"));
        assertFalse(nunchuk.contains("ENTER"));
    }

    @Test
    void gameplayReferenceUsesOneDeviceVocabularyAtATime() {
        assertEquals("B / RT", UiInputPrompts.gameplayBinding(
                UiInputPrompts.Device.GAMEPAD, UiInputPrompts.GameplayAction.SPECIAL));
        assertEquals("SHAKE", UiInputPrompts.gameplayBinding(
                UiInputPrompts.Device.WIIMOTE_SIDEWAYS, UiInputPrompts.GameplayAction.SPECIAL));
        assertEquals("B / TWIST", UiInputPrompts.gameplayBinding(
                UiInputPrompts.Device.WIIMOTE_NUNCHUK, UiInputPrompts.GameplayAction.SPECIAL));
        assertEquals("", UiInputPrompts.gameplayBinding(
                UiInputPrompts.Device.KEYBOARD_MOUSE, UiInputPrompts.GameplayAction.SPECIAL));
    }

    @Test
    void directionalSpecialPromptChangesAsAWholeWhenDeviceChanges() {
        String keyboard = directional(UiInputPrompts.Device.KEYBOARD_MOUSE);
        String controller = directional(UiInputPrompts.Device.GAMEPAD);
        String wiimote = directional(UiInputPrompts.Device.WIIMOTE_SIDEWAYS);

        assertEquals("A / D + SHIFT", keyboard);
        assertEquals("STICK LEFT / RIGHT + B / RT", controller);
        assertEquals("D-PAD LEFT / RIGHT + SHAKE", wiimote);
    }

    private static String directional(UiInputPrompts.Device device) {
        return UiInputPrompts.directionalSpecial(device, "SIDE", "SHIFT", "A", "D", "W", "S");
    }
}
