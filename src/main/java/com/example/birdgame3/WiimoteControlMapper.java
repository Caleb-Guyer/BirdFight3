package com.example.birdgame3;

final class WiimoteControlMapper {
    private static final long INPUT_PULSE_NS = 120_000_000L;
    private static final long TAUNT_PULSE_NS = 100_000_000L;
    private static final long JUMP_COOLDOWN_NS = 220_000_000L;
    private static final long BLOCK_COOLDOWN_NS = 220_000_000L;
    private static final long ATTACK_COOLDOWN_NS = 180_000_000L;
    private static final long SPECIAL_COOLDOWN_NS = 240_000_000L;
    private static final long MENU_INITIAL_REPEAT_NS = 420_000_000L;
    private static final long MENU_REPEAT_NS = 210_000_000L;
    private static final int DIRECTION_RELEASE_DEBOUNCE_SAMPLES = 2;
    private static final int DIGITAL_RELEASE_DEBOUNCE_SAMPLES = 2;
    private static final double MENU_AXIS_THRESHOLD = 0.45;
    private static final double MOVE_AXIS_THRESHOLD = 0.33;
    private static final double VERTICAL_FLICK_THRESHOLD = 0.34;
    private static final double ATTACK_SWING_THRESHOLD = 130.0;
    private static final double SPECIAL_TWIST_THRESHOLD = 155.0;
    private static final double ATTACK_JERK_THRESHOLD = 0.62;
    private static final double SPECIAL_SHAKE_THRESHOLD = 0.88;
    private static final double STILL_GYRO_THRESHOLD = 42.0;
    private static final double STILL_DELTA_THRESHOLD = 0.08;

    private Vector3 neutralRemote = new Vector3(0.0, 0.0, 1.0);
    private Vector3 neutralNunchuk = new Vector3(0.0, 0.0, 1.0);
    private int neutralRemoteSamples = 0;
    private int neutralNunchukSamples = 0;
    private double stickCenterX = 0.0;
    private double stickCenterY = 0.0;
    private int stickCenterSamples = 0;
    private WiimoteRawInput previous = WiimoteRawInput.disconnected("Waiting for Wiimote");
    private long jumpPulseUntilNs = 0L;
    private long attackPulseUntilNs = 0L;
    private long specialPulseUntilNs = 0L;
    private long blockPulseUntilNs = 0L;
    private long tauntCyclePulseUntilNs = 0L;
    private long tauntExecutePulseUntilNs = 0L;
    private long menuSelectPulseUntilNs = 0L;
    private long menuBackPulseUntilNs = 0L;
    private long menuPausePulseUntilNs = 0L;
    private long lastJumpTriggerNs = 0L;
    private long lastAttackTriggerNs = 0L;
    private long lastSpecialTriggerNs = 0L;
    private long lastBlockTriggerNs = 0L;
    private boolean leftHeldStable = false;
    private boolean rightHeldStable = false;
    private int leftReleaseSamples = 0;
    private int rightReleaseSamples = 0;
    private final long[] nextMenuRepeatNs = new long[4];
    private final boolean[] digitalHeldStable = new boolean[13];
    private final int[] digitalReleaseSamples = new int[13];

    void reset() {
        neutralRemote = new Vector3(0.0, 0.0, 1.0);
        neutralNunchuk = new Vector3(0.0, 0.0, 1.0);
        neutralRemoteSamples = 0;
        neutralNunchukSamples = 0;
        stickCenterX = 0.0;
        stickCenterY = 0.0;
        stickCenterSamples = 0;
        previous = WiimoteRawInput.disconnected("Waiting for Wiimote");
        jumpPulseUntilNs = 0L;
        attackPulseUntilNs = 0L;
        specialPulseUntilNs = 0L;
        blockPulseUntilNs = 0L;
        tauntCyclePulseUntilNs = 0L;
        tauntExecutePulseUntilNs = 0L;
        menuSelectPulseUntilNs = 0L;
        menuBackPulseUntilNs = 0L;
        menuPausePulseUntilNs = 0L;
        lastJumpTriggerNs = 0L;
        lastAttackTriggerNs = 0L;
        lastSpecialTriggerNs = 0L;
        lastBlockTriggerNs = 0L;
        leftHeldStable = false;
        rightHeldStable = false;
        leftReleaseSamples = 0;
        rightReleaseSamples = 0;
        for (int i = 0; i < nextMenuRepeatNs.length; i++) {
            nextMenuRepeatNs[i] = 0L;
        }
        for (int i = 0; i < digitalHeldStable.length; i++) {
            digitalHeldStable[i] = false;
            digitalReleaseSamples[i] = 0;
        }
    }

    WiimoteMappedState map(WiimoteRawInput raw, WiimoteControlMode mode) {
        if (mode == WiimoteControlMode.OFF) {
            reset();
            return WiimoteMappedState.off("Wiimote mode off");
        }
        if (raw == null || !raw.connected()) {
            previous = raw == null ? WiimoteRawInput.disconnected("Waiting for Wiimote") : raw;
            return WiimoteMappedState.waiting(raw == null ? "Waiting for Wiimote" : raw.status());
        }

        long now = raw.sampleNanos() > 0 ? raw.sampleNanos() : System.nanoTime();
        updateNeutralSamples(raw);

        boolean jumpGesture = false;
        boolean attackGesture = false;
        boolean specialGesture = false;
        boolean blockGesture = false;

        if (previous.connected()) {
            Vector3 currentRemote = new Vector3(raw.remoteAccelX(), raw.remoteAccelY(), raw.remoteAccelZ());
            Vector3 previousRemote = new Vector3(previous.remoteAccelX(), previous.remoteAccelY(), previous.remoteAccelZ());
            Vector3 remoteDelta = currentRemote.subtract(previousRemote);
            Vector3 verticalReference = neutralRemote.normalizeOr(new Vector3(0.0, 0.0, 1.0));
            double verticalImpulse = remoteDelta.dot(verticalReference);
            double totalJerk = remoteDelta.length();
            double attackSwing = Math.max(Math.abs(raw.gyroPitch()), Math.abs(raw.gyroRoll()));
            double specialTwist = Math.abs(raw.gyroYaw());

            if (mode == WiimoteControlMode.NUNCHUK && raw.nunchukConnected() && previous.nunchukConnected()) {
                Vector3 currentNunchuk = new Vector3(raw.nunchukAccelX(), raw.nunchukAccelY(), raw.nunchukAccelZ());
                Vector3 previousNunchuk = new Vector3(previous.nunchukAccelX(), previous.nunchukAccelY(), previous.nunchukAccelZ());
                Vector3 nunchukDelta = currentNunchuk.subtract(previousNunchuk);
                Vector3 nunchukReference = neutralNunchuk.normalizeOr(new Vector3(0.0, 0.0, 1.0));
                verticalImpulse = nunchukDelta.dot(nunchukReference);
            }

            if (mode == WiimoteControlMode.NUNCHUK) {
                if (now - lastJumpTriggerNs >= JUMP_COOLDOWN_NS && verticalImpulse >= VERTICAL_FLICK_THRESHOLD) {
                    jumpPulseUntilNs = now + INPUT_PULSE_NS;
                    lastJumpTriggerNs = now;
                    jumpGesture = true;
                }
                if (now - lastBlockTriggerNs >= BLOCK_COOLDOWN_NS && verticalImpulse <= -VERTICAL_FLICK_THRESHOLD) {
                    blockPulseUntilNs = now + INPUT_PULSE_NS;
                    lastBlockTriggerNs = now;
                    blockGesture = true;
                }
                if (!jumpGesture && !blockGesture) {
                    if (now - lastSpecialTriggerNs >= SPECIAL_COOLDOWN_NS
                            && (specialTwist >= SPECIAL_TWIST_THRESHOLD || totalJerk >= SPECIAL_SHAKE_THRESHOLD)) {
                        specialPulseUntilNs = now + INPUT_PULSE_NS;
                        lastSpecialTriggerNs = now;
                        specialGesture = true;
                    } else if (now - lastAttackTriggerNs >= ATTACK_COOLDOWN_NS
                            && (attackSwing >= ATTACK_SWING_THRESHOLD || totalJerk >= ATTACK_JERK_THRESHOLD)) {
                        attackPulseUntilNs = now + INPUT_PULSE_NS;
                        lastAttackTriggerNs = now;
                        attackGesture = true;
                    }
                }
            } else if (now - lastSpecialTriggerNs >= SPECIAL_COOLDOWN_NS && totalJerk >= SPECIAL_SHAKE_THRESHOLD) {
                specialPulseUntilNs = now + INPUT_PULSE_NS;
                lastSpecialTriggerNs = now;
                specialGesture = true;
            }
        }

        boolean left;
        boolean right;
        boolean rawJumpHeld;
        boolean rawBlockHeld;
        boolean rawAttackHeld;
        boolean rawSpecialHeld;
        boolean rawTauntCycleHeld = raw.buttonPlus() || now < tauntCyclePulseUntilNs;
        boolean rawTauntExecuteHeld = raw.buttonMinus() || now < tauntExecutePulseUntilNs;

        if (edgePressed(previous.buttonPlus(), raw.buttonPlus())) {
            tauntCyclePulseUntilNs = now + TAUNT_PULSE_NS;
            rawTauntCycleHeld = true;
        }
        if (edgePressed(previous.buttonMinus(), raw.buttonMinus())) {
            tauntExecutePulseUntilNs = now + TAUNT_PULSE_NS;
            rawTauntExecuteHeld = true;
        }

        if (mode == WiimoteControlMode.NUNCHUK) {
            if (edgePressed(previous.buttonC() || previous.dpadUp(), raw.buttonC() || raw.dpadUp())) {
                jumpPulseUntilNs = now + INPUT_PULSE_NS;
            }
            if (edgePressed(previous.buttonZ() || previous.dpadDown(), raw.buttonZ() || raw.dpadDown())) {
                blockPulseUntilNs = now + INPUT_PULSE_NS;
            }
            if (edgePressed(previous.buttonA(), raw.buttonA())) {
                attackPulseUntilNs = now + INPUT_PULSE_NS;
            }
            if (edgePressed(previous.buttonB(), raw.buttonB())) {
                specialPulseUntilNs = now + INPUT_PULSE_NS;
            }
            double stickX = centeredStickX(raw);
            boolean rawLeft = raw.dpadLeft() || stickX <= -MOVE_AXIS_THRESHOLD;
            boolean rawRight = raw.dpadRight() || stickX >= MOVE_AXIS_THRESHOLD;
            left = debouncedDirection(rawLeft, rawRight, true);
            right = debouncedDirection(rawRight, rawLeft, false);
            rawJumpHeld = raw.buttonC() || raw.dpadUp() || now < jumpPulseUntilNs || jumpGesture;
            rawBlockHeld = raw.buttonZ() || raw.dpadDown() || now < blockPulseUntilNs || blockGesture;
            rawAttackHeld = raw.buttonA() || now < attackPulseUntilNs || attackGesture;
            rawSpecialHeld = raw.buttonB() || now < specialPulseUntilNs || specialGesture;
        } else {
            if (edgePressed(previous.buttonTwo() || sidewaysUpHeld(previous), raw.buttonTwo() || sidewaysUpHeld(raw))) {
                jumpPulseUntilNs = now + INPUT_PULSE_NS;
            }
            if (edgePressed(sidewaysDownHeld(previous), sidewaysDownHeld(raw))) {
                blockPulseUntilNs = now + INPUT_PULSE_NS;
            }
            if (edgePressed(previous.buttonOne(), raw.buttonOne())) {
                attackPulseUntilNs = now + INPUT_PULSE_NS;
            }
            boolean rawLeft = sidewaysLeftHeld(raw);
            boolean rawRight = sidewaysRightHeld(raw);
            left = debouncedDirection(rawLeft, rawRight, true);
            right = debouncedDirection(rawRight, rawLeft, false);
            rawJumpHeld = raw.buttonTwo() || sidewaysUpHeld(raw) || now < jumpPulseUntilNs;
            rawBlockHeld = sidewaysDownHeld(raw) || now < blockPulseUntilNs;
            rawAttackHeld = raw.buttonOne() || now < attackPulseUntilNs;
            rawSpecialHeld = now < specialPulseUntilNs || specialGesture;
        }

        boolean rawMenuUp = stabilizedDigital(8, menuUpHeld(raw, mode));
        boolean rawMenuDown = stabilizedDigital(9, menuDownHeld(raw, mode));
        boolean rawMenuLeft = stabilizedDigital(10, menuLeftHeld(raw, mode));
        boolean rawMenuRight = stabilizedDigital(11, menuRightHeld(raw, mode));
        boolean rawMenuSelectBase = mode == WiimoteControlMode.NUNCHUK
                ? raw.buttonA() || raw.buttonC()
                : raw.buttonOne() || raw.buttonTwo();
        boolean previousMenuSelect = mode == WiimoteControlMode.NUNCHUK
                ? previous.buttonA() || previous.buttonC()
                : previous.buttonOne() || previous.buttonTwo();
        boolean rawMenuBackBase = mode == WiimoteControlMode.NUNCHUK
                ? raw.buttonB() || raw.buttonZ()
                : raw.buttonB() || raw.buttonMinus();
        boolean previousMenuBack = mode == WiimoteControlMode.NUNCHUK
                ? previous.buttonB() || previous.buttonZ()
                : previous.buttonB() || previous.buttonMinus();
        boolean rawMenuPauseBase = raw.buttonHome();
        boolean previousMenuPause = previous.buttonHome();
        if (edgePressed(previousMenuSelect, rawMenuSelectBase)) {
            menuSelectPulseUntilNs = now + INPUT_PULSE_NS;
        }
        if (edgePressed(previousMenuBack, rawMenuBackBase)) {
            menuBackPulseUntilNs = now + INPUT_PULSE_NS;
        }
        if (edgePressed(previousMenuPause, rawMenuPauseBase)) {
            menuPausePulseUntilNs = now + INPUT_PULSE_NS;
        }
        boolean rawMenuSelect = stabilizedDigital(5, rawMenuSelectBase);
        boolean rawMenuBack = stabilizedDigital(6, rawMenuBackBase);
        boolean rawMenuPause = stabilizedDigital(7, rawMenuPauseBase);
        boolean menuUp = menuHold(menuUpHeld(previous, mode), rawMenuUp, now, 0);
        boolean menuDown = menuHold(menuDownHeld(previous, mode), rawMenuDown, now, 1);
        boolean menuLeft = menuHold(menuLeftHeld(previous, mode), rawMenuLeft, now, 2);
        boolean menuRight = menuHold(menuRightHeld(previous, mode), rawMenuRight, now, 3);
        boolean menuSelect = rawMenuSelect || now < menuSelectPulseUntilNs;
        boolean menuBack = rawMenuBack || now < menuBackPulseUntilNs;
        boolean menuPause = rawMenuPause || now < menuPausePulseUntilNs;
        boolean jump = stabilizedDigital(0, rawJumpHeld);
        boolean attack = stabilizedDigital(1, rawAttackHeld);
        boolean special = stabilizedDigital(2, rawSpecialHeld);
        boolean block = stabilizedDigital(3, rawBlockHeld);
        boolean tauntCycle = stabilizedDigital(4, rawTauntCycleHeld);
        boolean tauntExecute = stabilizedDigital(5 + 7, rawTauntExecuteHeld);

        previous = raw;
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
                rawMenuUp,
                rawMenuDown,
                rawMenuLeft,
                rawMenuRight,
                rawMenuSelect,
                rawMenuBack,
                rawMenuPause,
                menuSelect,
                menuBack,
                menuPause,
                raw.status()
        );
    }

    private boolean edgePressed(boolean previousDown, boolean currentDown) {
        return currentDown && !previousDown;
    }

    private boolean stabilizedDigital(int index, boolean currentlyHeld) {
        if (index < 0 || index >= digitalHeldStable.length) {
            return currentlyHeld;
        }
        if (currentlyHeld) {
            digitalHeldStable[index] = true;
            digitalReleaseSamples[index] = 0;
            return true;
        }
        if (!digitalHeldStable[index]) {
            return false;
        }
        digitalReleaseSamples[index]++;
        if (digitalReleaseSamples[index] >= DIGITAL_RELEASE_DEBOUNCE_SAMPLES) {
            digitalHeldStable[index] = false;
            digitalReleaseSamples[index] = 0;
            return false;
        }
        return true;
    }

    private boolean debouncedDirection(boolean directionHeld, boolean oppositeHeld, boolean leftDirection) {
        boolean stableHeld = leftDirection ? leftHeldStable : rightHeldStable;
        if (directionHeld) {
            if (leftDirection) {
                leftHeldStable = true;
                leftReleaseSamples = 0;
            } else {
                rightHeldStable = true;
                rightReleaseSamples = 0;
            }
            return true;
        }
        if (oppositeHeld) {
            if (leftDirection) {
                leftHeldStable = false;
                leftReleaseSamples = 0;
            } else {
                rightHeldStable = false;
                rightReleaseSamples = 0;
            }
            return false;
        }
        if (!stableHeld) {
            return false;
        }
        if (leftDirection) {
            leftReleaseSamples++;
            if (leftReleaseSamples >= DIRECTION_RELEASE_DEBOUNCE_SAMPLES) {
                leftHeldStable = false;
                leftReleaseSamples = 0;
                return false;
            }
        } else {
            rightReleaseSamples++;
            if (rightReleaseSamples >= DIRECTION_RELEASE_DEBOUNCE_SAMPLES) {
                rightHeldStable = false;
                rightReleaseSamples = 0;
                return false;
            }
        }
        return true;
    }

    private void updateNeutralSamples(WiimoteRawInput raw) {
        Vector3 remote = new Vector3(raw.remoteAccelX(), raw.remoteAccelY(), raw.remoteAccelZ());
        if (neutralRemoteSamples < 28) {
            neutralRemote = neutralRemote.average(remote, neutralRemoteSamples);
            neutralRemoteSamples++;
        } else if (isStill(remote, new Vector3(previous.remoteAccelX(), previous.remoteAccelY(), previous.remoteAccelZ()),
                raw.gyroYaw(), raw.gyroRoll(), raw.gyroPitch())) {
            neutralRemote = neutralRemote.blend(remote, 0.015);
        }

        if (raw.nunchukConnected()) {
            Vector3 nunchuk = new Vector3(raw.nunchukAccelX(), raw.nunchukAccelY(), raw.nunchukAccelZ());
            if (neutralNunchukSamples < 28) {
                neutralNunchuk = neutralNunchuk.average(nunchuk, neutralNunchukSamples);
                neutralNunchukSamples++;
            } else {
                neutralNunchuk = neutralNunchuk.blend(nunchuk, 0.015);
            }

            if (stickCenterSamples < 30) {
                stickCenterX = rollingAverage(stickCenterX, raw.nunchukStickX(), stickCenterSamples);
                stickCenterY = rollingAverage(stickCenterY, raw.nunchukStickY(), stickCenterSamples);
                stickCenterSamples++;
            } else if (Math.abs(raw.nunchukStickX() - stickCenterX) < 0.16 && Math.abs(raw.nunchukStickY() - stickCenterY) < 0.16) {
                stickCenterX = blend(stickCenterX, raw.nunchukStickX(), 0.015);
                stickCenterY = blend(stickCenterY, raw.nunchukStickY(), 0.015);
            }
        }
    }

    private boolean isStill(Vector3 current, Vector3 previousVector, double yaw, double roll, double pitch) {
        return current.subtract(previousVector).length() <= STILL_DELTA_THRESHOLD
                && Math.abs(yaw) <= STILL_GYRO_THRESHOLD
                && Math.abs(roll) <= STILL_GYRO_THRESHOLD
                && Math.abs(pitch) <= STILL_GYRO_THRESHOLD;
    }

    private boolean sidewaysLeftHeld(WiimoteRawInput raw) {
        return raw != null && raw.dpadUp();
    }

    private boolean sidewaysRightHeld(WiimoteRawInput raw) {
        return raw != null && raw.dpadDown();
    }

    private boolean sidewaysUpHeld(WiimoteRawInput raw) {
        return raw != null && raw.dpadRight();
    }

    private boolean sidewaysDownHeld(WiimoteRawInput raw) {
        return raw != null && raw.dpadLeft();
    }

    private boolean menuUpHeld(WiimoteRawInput raw, WiimoteControlMode mode) {
        if (raw == null) {
            return false;
        }
        return switch (mode) {
            case SIDEWAYS -> sidewaysUpHeld(raw);
            case NUNCHUK -> raw.dpadUp() || centeredStickY(raw) >= MENU_AXIS_THRESHOLD;
            case OFF -> false;
        };
    }

    private boolean menuDownHeld(WiimoteRawInput raw, WiimoteControlMode mode) {
        if (raw == null) {
            return false;
        }
        return switch (mode) {
            case SIDEWAYS -> sidewaysDownHeld(raw);
            case NUNCHUK -> raw.dpadDown() || centeredStickY(raw) <= -MENU_AXIS_THRESHOLD;
            case OFF -> false;
        };
    }

    private boolean menuLeftHeld(WiimoteRawInput raw, WiimoteControlMode mode) {
        if (raw == null) {
            return false;
        }
        return switch (mode) {
            case SIDEWAYS -> sidewaysLeftHeld(raw);
            case NUNCHUK -> raw.dpadLeft() || centeredStickX(raw) <= -MENU_AXIS_THRESHOLD;
            case OFF -> false;
        };
    }

    private boolean menuRightHeld(WiimoteRawInput raw, WiimoteControlMode mode) {
        if (raw == null) {
            return false;
        }
        return switch (mode) {
            case SIDEWAYS -> sidewaysRightHeld(raw);
            case NUNCHUK -> raw.dpadRight() || centeredStickX(raw) >= MENU_AXIS_THRESHOLD;
            case OFF -> false;
        };
    }

    private boolean menuHold(boolean previousHeld, boolean currentHeld, long now, int index) {
        if (!currentHeld) {
            nextMenuRepeatNs[index] = 0L;
            return false;
        }
        if (!previousHeld) {
            nextMenuRepeatNs[index] = now + MENU_INITIAL_REPEAT_NS;
            return true;
        }
        if (now >= nextMenuRepeatNs[index]) {
            nextMenuRepeatNs[index] = now + MENU_REPEAT_NS;
            return true;
        }
        return false;
    }

    private double centeredStickX(WiimoteRawInput raw) {
        if (raw == null || !raw.nunchukConnected()) {
            return 0.0;
        }
        return Math.clamp(raw.nunchukStickX() - stickCenterX, -1.0, 1.0);
    }

    private double centeredStickY(WiimoteRawInput raw) {
        if (raw == null || !raw.nunchukConnected()) {
            return 0.0;
        }
        return Math.clamp(raw.nunchukStickY() - stickCenterY, -1.0, 1.0);
    }

    private double rollingAverage(double currentAverage, double sample, int samples) {
        return (currentAverage * samples + sample) / Math.max(1, samples + 1);
    }

    private double blend(double base, double sample, double factor) {
        return base + (sample - base) * factor;
    }

    private record Vector3(double x, double y, double z) {
        Vector3 subtract(Vector3 other) {
            return new Vector3(x - other.x, y - other.y, z - other.z);
        }

        double dot(Vector3 other) {
            return x * other.x + y * other.y + z * other.z;
        }

        double length() {
            return Math.sqrt(x * x + y * y + z * z);
        }

        Vector3 normalizeOr(Vector3 fallback) {
            double length = length();
            if (length <= 0.0001) {
                return fallback;
            }
            return new Vector3(x / length, y / length, z / length);
        }

        Vector3 average(Vector3 sample, int samples) {
            double total = Math.max(1, samples + 1);
            return new Vector3((x * samples + sample.x) / total,
                    (y * samples + sample.y) / total,
                    (z * samples + sample.z) / total);
        }

        Vector3 blend(Vector3 sample, double factor) {
            return new Vector3(
                    x + (sample.x - x) * factor,
                    y + (sample.y - y) * factor,
                    z + (sample.z - z) * factor
            );
        }
    }
}
