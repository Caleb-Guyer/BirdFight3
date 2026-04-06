package com.example.birdgame3;

import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.Structure;

import java.util.List;

final class XboxInputManager implements AutoCloseable {
    private static final int PLAYER_SLOTS = 4;
    private static final int ERROR_SUCCESS = 0;
    private static final int XINPUT_GAMEPAD_DPAD_UP = 0x0001;
    private static final int XINPUT_GAMEPAD_DPAD_DOWN = 0x0002;
    private static final int XINPUT_GAMEPAD_DPAD_LEFT = 0x0004;
    private static final int XINPUT_GAMEPAD_DPAD_RIGHT = 0x0008;
    private static final int XINPUT_GAMEPAD_START = 0x0010;
    private static final int XINPUT_GAMEPAD_BACK = 0x0020;
    private static final int XINPUT_GAMEPAD_LEFT_SHOULDER = 0x0100;
    private static final int XINPUT_GAMEPAD_RIGHT_SHOULDER = 0x0200;
    private static final int XINPUT_GAMEPAD_A = 0x1000;
    private static final int XINPUT_GAMEPAD_B = 0x2000;
    private static final int XINPUT_GAMEPAD_X = 0x4000;
    private static final int XINPUT_GAMEPAD_Y = 0x8000;
    private static final int XINPUT_GAMEPAD_TRIGGER_THRESHOLD = 30;
    private static final int XINPUT_GAMEPAD_LEFT_THUMB_DEADZONE = 7849;
    private static final int MENU_STICK_THRESHOLD = 16000;

    private final XInputLibrary xinput;
    private boolean available;
    private String unavailableReason;

    XboxInputManager() {
        XInputLibrary loaded = null;
        for (String dll : List.of("xinput1_4", "xinput9_1_0", "xinput1_3")) {
            try {
                loaded = Native.load(dll, XInputLibrary.class);
                break;
            } catch (Throwable ignored) {
            }
        }
        xinput = loaded;
        available = loaded != null;
        unavailableReason = available ? "" : "Xbox controller unavailable";
    }

    WiimoteMappedState stateForSlot(int playerIndex) {
        XInputState state = readState(playerIndex);
        if (state == null) {
            if (playerIndex < 0 || playerIndex >= PLAYER_SLOTS) {
                return WiimoteMappedState.off("Xbox slot unavailable");
            }
            return available ? WiimoteMappedState.waiting("Waiting for Xbox controller")
                    : WiimoteMappedState.waiting(unavailableReason);
        }
        return mapState(state, playerIndex);
    }

    boolean isConnected(int playerIndex) {
        return readState(playerIndex) != null;
    }

    boolean viewPressedForSlot(int playerIndex) {
        XInputState state = readState(playerIndex);
        if (state == null) {
            return false;
        }
        int buttons = Short.toUnsignedInt(state.gamepad.wButtons);
        return (buttons & XINPUT_GAMEPAD_BACK) != 0;
    }

    private XInputState readState(int playerIndex) {
        if (playerIndex < 0 || playerIndex >= PLAYER_SLOTS) {
            return null;
        }
        if (!available || xinput == null) {
            return null;
        }

        XInputState state;
        try {
            state = new XInputState();
        } catch (Throwable ignored) {
            disable("Xbox controller unavailable");
            return null;
        }
        int result;
        try {
            result = xinput.XInputGetState(playerIndex, state);
        } catch (Throwable ignored) {
            disable("Xbox controller unavailable");
            return null;
        }
        if (result != ERROR_SUCCESS) {
            return null;
        }
        return state;
    }

    WiimoteMappedState stateForPlayer(int playerIndex) {
        return stateForSlot(playerIndex);
    }

    WiimoteMappedState menuState() {
        for (int playerIndex = 0; playerIndex < PLAYER_SLOTS; playerIndex++) {
            WiimoteMappedState state = stateForSlot(playerIndex);
            if (state.connected()) {
                return state;
            }
        }
        return available ? WiimoteMappedState.waiting("Waiting for Xbox controller")
                : WiimoteMappedState.waiting(unavailableReason);
    }

    @Override
    public void close() {
    }

    private void disable(String reason) {
        available = false;
        unavailableReason = (reason == null || reason.isBlank()) ? "Xbox controller unavailable" : reason;
    }

    private WiimoteMappedState mapState(XInputState state, int playerIndex) {
        int buttons = Short.toUnsignedInt(state.gamepad.wButtons);
        int leftTrigger = Byte.toUnsignedInt(state.gamepad.bLeftTrigger);
        int rightTrigger = Byte.toUnsignedInt(state.gamepad.bRightTrigger);
        int leftStickX = state.gamepad.sThumbLX;
        int leftStickY = state.gamepad.sThumbLY;

        boolean left = (buttons & XINPUT_GAMEPAD_DPAD_LEFT) != 0 || leftStickX <= -XINPUT_GAMEPAD_LEFT_THUMB_DEADZONE;
        boolean right = (buttons & XINPUT_GAMEPAD_DPAD_RIGHT) != 0 || leftStickX >= XINPUT_GAMEPAD_LEFT_THUMB_DEADZONE;
        boolean jump = (buttons & XINPUT_GAMEPAD_A) != 0;
        boolean attack = (buttons & XINPUT_GAMEPAD_X) != 0;
        boolean special = (buttons & XINPUT_GAMEPAD_B) != 0 || rightTrigger > XINPUT_GAMEPAD_TRIGGER_THRESHOLD;
        boolean block = (buttons & XINPUT_GAMEPAD_LEFT_SHOULDER) != 0 || leftTrigger > XINPUT_GAMEPAD_TRIGGER_THRESHOLD;
        boolean tauntCycle = (buttons & XINPUT_GAMEPAD_Y) != 0;
        boolean tauntExecute = (buttons & XINPUT_GAMEPAD_RIGHT_SHOULDER) != 0;

        boolean menuUp = (buttons & XINPUT_GAMEPAD_DPAD_UP) != 0 || leftStickY >= MENU_STICK_THRESHOLD;
        boolean menuDown = (buttons & XINPUT_GAMEPAD_DPAD_DOWN) != 0 || leftStickY <= -MENU_STICK_THRESHOLD;
        boolean menuLeft = (buttons & XINPUT_GAMEPAD_DPAD_LEFT) != 0 || leftStickX <= -MENU_STICK_THRESHOLD;
        boolean menuRight = (buttons & XINPUT_GAMEPAD_DPAD_RIGHT) != 0 || leftStickX >= MENU_STICK_THRESHOLD;
        boolean menuSelect = (buttons & XINPUT_GAMEPAD_A) != 0;
        boolean menuBack = (buttons & XINPUT_GAMEPAD_B) != 0;
        boolean menuPause = (buttons & XINPUT_GAMEPAD_START) != 0;

        return new WiimoteMappedState(
                true,
                left,
                right,
                jump,
                attack,
                special,
                block,
                tauntCycle,
                tauntExecute,
                menuUp,
                menuDown,
                menuLeft,
                menuRight,
                menuUp,
                menuDown,
                menuLeft,
                menuRight,
                menuSelect,
                menuBack,
                menuPause,
                menuSelect,
                menuBack,
                menuPause,
                "Xbox controller P" + (playerIndex + 1)
        );
    }

    private interface XInputLibrary extends Library {
        int XInputGetState(int dwUserIndex, XInputState state);
    }

    public static final class XInputGamepad extends Structure {
        public short wButtons;
        public byte bLeftTrigger;
        public byte bRightTrigger;
        public short sThumbLX;
        public short sThumbLY;
        public short sThumbRX;
        public short sThumbRY;

        @Override
        protected List<String> getFieldOrder() {
            return List.of("wButtons", "bLeftTrigger", "bRightTrigger", "sThumbLX", "sThumbLY", "sThumbRX", "sThumbRY");
        }
    }

    public static final class XInputState extends Structure {
        public int dwPacketNumber;
        public XInputGamepad gamepad = new XInputGamepad();

        @Override
        protected List<String> getFieldOrder() {
            return List.of("dwPacketNumber", "gamepad");
        }
    }
}
