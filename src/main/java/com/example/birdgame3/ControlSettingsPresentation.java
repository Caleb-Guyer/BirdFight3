package com.example.birdgame3;

/** Presentation-only routing for the settings control reference. */
final class ControlSettingsPresentation {
    enum Page {
        KEYBOARD,
        CONTROLLER,
        WIIMOTE
    }

    private ControlSettingsPresentation() {
    }

    static Page pageFor(UiInputPrompts.Device device) {
        UiInputPrompts.Device resolved = device == null
                ? UiInputPrompts.Device.KEYBOARD_MOUSE
                : device;
        return switch (resolved) {
            case KEYBOARD_MOUSE -> Page.KEYBOARD;
            case GAMEPAD -> Page.CONTROLLER;
            case WIIMOTE_SIDEWAYS, WIIMOTE_NUNCHUK -> Page.WIIMOTE;
        };
    }
}
