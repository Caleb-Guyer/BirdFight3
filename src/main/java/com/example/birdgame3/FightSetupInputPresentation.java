package com.example.birdgame3;

/**
 * Presentation-only copy for a fighter slot's current input source.
 * Keeping this decision outside the JavaFX scene makes the join states easy to
 * test and prevents menus from implying that every player uses a keyboard.
 */
final class FightSetupInputPresentation {
    record Display(String text, String accent, boolean disabled, double opacity) {
    }

    private FightSetupInputPresentation() {
    }

    static Display resolve(boolean active, boolean cpu, int controllerSlot,
                           boolean controllerConnected, boolean awaitingController,
                           UiInputPrompts.Device device) {
        UiInputPrompts.Device resolved = device == null
                ? UiInputPrompts.Device.KEYBOARD_MOUSE
                : device;
        if (!active) {
            return new Display("SLOT CLOSED", "#37474F", true, 0.45);
        }
        if (cpu) {
            return new Display("CPU CONTROL", "#6D4C41", true, 0.85);
        }
        if (controllerSlot >= 0) {
            String controller = "CONTROLLER " + (controllerSlot + 1);
            return controllerConnected
                    ? new Display(controller, UiInputPrompts.Device.GAMEPAD.accent(), false, 1.0)
                    : new Display(controller + " LOST", "#C62828", false, 1.0);
        }
        if (awaitingController) {
            String confirm = UiInputPrompts.inputFor(
                    UiInputPrompts.Device.GAMEPAD, UiInputPrompts.Command.SELECT);
            return new Display(confirm + "  CONNECT CONTROLLER", "#F9A825", false, 1.0);
        }
        return new Display(resolved.label(), resolved.accent(), false, 1.0);
    }
}
