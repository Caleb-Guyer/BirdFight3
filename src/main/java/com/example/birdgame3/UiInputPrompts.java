package com.example.birdgame3;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * Presentation-only vocabulary for concise, device-specific UI prompts.
 * Nothing in this class is read by the simulation or consumes randomness.
 */
final class UiInputPrompts {
    enum Device {
        KEYBOARD_MOUSE("KEYBOARD + MOUSE", "#B0BEC5"),
        GAMEPAD("CONTROLLER", "#64B5F6"),
        WIIMOTE_SIDEWAYS("WII REMOTE", "#80DEEA"),
        WIIMOTE_NUNCHUK("WII REMOTE + NUNCHUK", "#80DEEA");

        private final String label;
        private final String accent;

        Device(String label, String accent) {
            this.label = label;
            this.accent = accent;
        }

        String label() {
            return label;
        }

        String accent() {
            return accent;
        }
    }

    enum Command {
        MOVE,
        PREVIEW,
        SELECT,
        START,
        BACK,
        PAUSE,
        FULLSCREEN
    }

    enum GameplayAction {
        LEFT,
        RIGHT,
        JUMP,
        ATTACK,
        SPECIAL,
        GRAB,
        BLOCK,
        TAUNT_CYCLE,
        TAUNT_EXECUTE
    }

    record Prompt(Command command, String verb) {
        Prompt {
            Objects.requireNonNull(command, "command");
            verb = verb == null ? "" : verb.trim().toUpperCase(Locale.ROOT);
        }
    }

    private UiInputPrompts() {
    }

    static Prompt prompt(Command command, String verb) {
        return new Prompt(command, verb);
    }

    static String render(Device device, Prompt... prompts) {
        Device resolved = device == null ? Device.KEYBOARD_MOUSE : device;
        List<String> rendered = new ArrayList<>();
        if (prompts != null) {
            for (Prompt prompt : prompts) {
                if (prompt == null) continue;
                String input = inputFor(resolved, prompt.command());
                if (input == null || input.isBlank()) continue;
                rendered.add(input + (prompt.verb().isBlank() ? "" : "  " + prompt.verb()));
            }
        }
        return String.join("   •   ", rendered);
    }

    static String inputFor(Device device, Command command) {
        Device resolved = device == null ? Device.KEYBOARD_MOUSE : device;
        if (command == null) return "";
        return switch (resolved) {
            case KEYBOARD_MOUSE -> switch (command) {
                case MOVE -> "WASD / ARROWS";
                case PREVIEW -> "MOUSE / ARROWS";
                case SELECT, START -> "ENTER";
                case BACK, PAUSE -> "ESC";
                case FULLSCREEN -> "F11";
            };
            case GAMEPAD -> switch (command) {
                case MOVE, PREVIEW -> "LEFT STICK / D-PAD";
                case SELECT, START -> "A";
                case BACK -> "B";
                case PAUSE -> "MENU";
                case FULLSCREEN -> "";
            };
            case WIIMOTE_SIDEWAYS -> switch (command) {
                case MOVE, PREVIEW -> "D-PAD";
                case SELECT, START -> "1 / 2";
                case BACK -> "B / −";
                case PAUSE -> "HOME";
                case FULLSCREEN -> "";
            };
            case WIIMOTE_NUNCHUK -> switch (command) {
                case MOVE, PREVIEW -> "STICK / D-PAD";
                case SELECT, START -> "A / C";
                case BACK -> "B / Z";
                case PAUSE -> "HOME";
                case FULLSCREEN -> "";
            };
        };
    }

    static String gameplayBinding(Device device, GameplayAction action) {
        Device resolved = device == null ? Device.KEYBOARD_MOUSE : device;
        if (action == null) return "";
        return switch (resolved) {
            case KEYBOARD_MOUSE -> ""; // Player-specific remappable bindings are supplied by BirdGame3.
            case GAMEPAD -> switch (action) {
                case LEFT, RIGHT -> "LEFT STICK / D-PAD";
                case JUMP -> "A";
                case ATTACK -> "X";
                case SPECIAL -> "B / RT";
                case GRAB -> "RB";
                case BLOCK -> "LB / LT";
                case TAUNT_CYCLE -> "Y";
                case TAUNT_EXECUTE -> "VIEW";
            };
            case WIIMOTE_SIDEWAYS -> switch (action) {
                case LEFT, RIGHT -> "D-PAD";
                case JUMP -> "2 / D-PAD UP";
                case ATTACK -> "1";
                case SPECIAL -> "SHAKE";
                case GRAB -> "+";
                case BLOCK -> "D-PAD DOWN";
                case TAUNT_CYCLE -> "−";
                case TAUNT_EXECUTE -> "+ + −";
            };
            case WIIMOTE_NUNCHUK -> switch (action) {
                case LEFT, RIGHT -> "STICK / D-PAD";
                case JUMP -> "C / D-PAD UP";
                case ATTACK -> "A / SWING";
                case SPECIAL -> "B / TWIST";
                case GRAB -> "+";
                case BLOCK -> "Z / D-PAD DOWN";
                case TAUNT_CYCLE -> "−";
                case TAUNT_EXECUTE -> "+ + −";
            };
        };
    }

    static String directionalSpecial(Device device, String direction, String keyboardSpecial,
                                     String keyboardLeft, String keyboardRight,
                                     String keyboardJump, String keyboardBlock) {
        Device resolved = device == null ? Device.KEYBOARD_MOUSE : device;
        String normalized = direction == null ? "NEUTRAL" : direction.trim().toUpperCase(Locale.ROOT);
        if (resolved == Device.KEYBOARD_MOUSE) {
            return switch (normalized) {
                case "SIDE" -> keyboardLeft + " / " + keyboardRight + " + " + keyboardSpecial;
                case "UP" -> keyboardJump + " + " + keyboardSpecial;
                case "DOWN" -> keyboardBlock + " + " + keyboardSpecial;
                default -> keyboardSpecial + " (NO DIRECTION)";
            };
        }
        String special = gameplayBinding(resolved, GameplayAction.SPECIAL);
        return switch (resolved) {
            case GAMEPAD -> switch (normalized) {
                case "SIDE" -> "STICK LEFT / RIGHT + " + special;
                case "UP" -> "STICK UP / A + " + special;
                case "DOWN" -> "STICK DOWN / LB + " + special;
                default -> special + " (NO DIRECTION)";
            };
            case WIIMOTE_SIDEWAYS -> switch (normalized) {
                case "SIDE" -> "D-PAD LEFT / RIGHT + SHAKE";
                case "UP" -> "D-PAD UP / 2 + SHAKE";
                case "DOWN" -> "D-PAD DOWN + SHAKE";
                default -> "SHAKE (NO DIRECTION)";
            };
            case WIIMOTE_NUNCHUK -> switch (normalized) {
                case "SIDE" -> "STICK LEFT / RIGHT + B / TWIST";
                case "UP" -> "STICK UP / C + B / TWIST";
                case "DOWN" -> "STICK DOWN / Z + B / TWIST";
                default -> "B / TWIST (NO DIRECTION)";
            };
            case KEYBOARD_MOUSE -> throw new IllegalStateException("Keyboard handled above");
        };
    }
}
