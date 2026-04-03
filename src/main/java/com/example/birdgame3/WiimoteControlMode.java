package com.example.birdgame3;

enum WiimoteControlMode {
    OFF("OFF", "Keyboard only"),
    SIDEWAYS("SIDEWAYS", "Sideways Wiimote"),
    NUNCHUK("NUNCHUK", "Wiimote + Nunchuk");

    final String label;
    final String description;

    WiimoteControlMode(String label, String description) {
        this.label = label;
        this.description = description;
    }

    WiimoteControlMode next() {
        return switch (this) {
            case OFF -> SIDEWAYS;
            case SIDEWAYS -> NUNCHUK;
            case NUNCHUK -> OFF;
        };
    }

    static WiimoteControlMode fromPreference(String raw) {
        if (raw == null || raw.isBlank()) {
            return OFF;
        }
        try {
            return WiimoteControlMode.valueOf(raw.trim().toUpperCase());
        } catch (IllegalArgumentException ignored) {
            return OFF;
        }
    }
}
