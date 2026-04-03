package com.example.birdgame3;

record WiimoteMappedState(
        boolean connected,
        boolean left,
        boolean right,
        boolean jump,
        boolean attack,
        boolean special,
        boolean block,
        boolean tauntCycle,
        boolean tauntExecute,
        boolean menuUp,
        boolean menuDown,
        boolean menuLeft,
        boolean menuRight,
        boolean menuUpHeld,
        boolean menuDownHeld,
        boolean menuLeftHeld,
        boolean menuRightHeld,
        boolean menuSelectHeld,
        boolean menuBackHeld,
        boolean menuPauseHeld,
        boolean menuSelect,
        boolean menuBack,
        boolean menuPause,
        String status
) {
    static WiimoteMappedState off(String status) {
        return new WiimoteMappedState(false, false, false, false, false, false, false, false, false,
                false, false, false, false, false, false, false, false, false, false, false, false, false,
                false, status);
    }

    static WiimoteMappedState waiting(String status) {
        return off(status);
    }
}
