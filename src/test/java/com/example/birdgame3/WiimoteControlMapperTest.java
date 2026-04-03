package com.example.birdgame3;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WiimoteControlMapperTest {

    @Test
    void sidewaysButtonsMapToPhysicalDirectionsAndFaceButtons() {
        WiimoteControlMapper mapper = new WiimoteControlMapper();
        mapper.map(raw(1_000_000_000L, false, false, false, false, false, false, false, false,
                false, false, false, false, false, false, false, false, false,
                0.0, 0.0, 1.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 1.0), WiimoteControlMode.SIDEWAYS);

        WiimoteMappedState mapped = mapper.map(raw(2_000_000_000L, false, false, false, false, true, false, true, true,
                false, false, true, true, true, true, false, false, false,
                0.0, 0.0, 1.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 1.0), WiimoteControlMode.SIDEWAYS);

        assertTrue(mapped.left());
        assertTrue(mapped.jump());
        assertTrue(mapped.attack());
        assertTrue(mapped.block());
        assertFalse(mapped.special());
        assertTrue(mapped.tauntCycle());
        assertTrue(mapped.menuUp());
        assertTrue(mapped.menuLeft());
        assertTrue(mapped.menuSelectHeld());
        assertTrue(mapped.menuSelect());
        assertTrue(mapped.menuBackHeld());
        assertTrue(mapped.menuBack());
    }

    @Test
    void sidewaysMenuHoldDoesNotRepeatImmediately() {
        WiimoteControlMapper mapper = new WiimoteControlMapper();
        mapper.map(raw(1_000_000_000L, false, false, false, false, false, false, false, false,
                false, false, false, false, false, false, false, false, false,
                0.0, 0.0, 1.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 1.0), WiimoteControlMode.SIDEWAYS);

        WiimoteMappedState first = mapper.map(raw(2_000_000_000L, false, false, false, false, true, false, false, false,
                false, false, false, false, false, false, false, false, false,
                0.0, 0.0, 1.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 1.0), WiimoteControlMode.SIDEWAYS);
        WiimoteMappedState heldSoon = mapper.map(raw(2_200_000_000L, false, false, false, false, true, false, false, false,
                false, false, false, false, false, false, false, false, false,
                0.0, 0.0, 1.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 1.0), WiimoteControlMode.SIDEWAYS);
        WiimoteMappedState heldLater = mapper.map(raw(2_500_000_000L, false, false, false, false, true, false, false, false,
                false, false, false, false, false, false, false, false, false,
                0.0, 0.0, 1.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 1.0), WiimoteControlMode.SIDEWAYS);

        assertTrue(first.menuLeft());
        assertFalse(heldSoon.menuLeft());
        assertTrue(heldLater.menuLeft());
    }

    @Test
    void sidewaysDirectionsIgnoreOneDroppedSampleThenRelease() {
        WiimoteControlMapper mapper = new WiimoteControlMapper();
        mapper.map(raw(1_000_000_000L, false, false, false, false, false, false, false, false,
                false, false, false, false, false, false, false, false, false,
                0.0, 0.0, 1.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 1.0), WiimoteControlMode.SIDEWAYS);

        WiimoteMappedState held = mapper.map(raw(2_000_000_000L, false, false, false, false, true, false, false, false,
                false, false, false, false, false, false, false, false, false,
                0.0, 0.0, 1.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 1.0), WiimoteControlMode.SIDEWAYS);
        WiimoteMappedState briefDrop = mapper.map(raw(2_050_000_000L, false, false, false, false, false, false, false, false,
                false, false, false, false, false, false, false, false, false,
                0.0, 0.0, 1.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 1.0), WiimoteControlMode.SIDEWAYS);
        WiimoteMappedState released = mapper.map(raw(2_120_000_000L, false, false, false, false, false, false, false, false,
                false, false, false, false, false, false, false, false, false,
                0.0, 0.0, 1.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 1.0), WiimoteControlMode.SIDEWAYS);

        assertTrue(held.left());
        assertTrue(briefDrop.left());
        assertFalse(released.left());
    }

    @Test
    void sidewaysButtonPressesPulseBrieflyAfterRelease() {
        WiimoteControlMapper mapper = new WiimoteControlMapper();
        mapper.map(raw(1_000_000_000L, false, false, false, false, false, false, false, false,
                false, false, false, false, false, false, false, false, false,
                0.0, 0.0, 1.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 1.0), WiimoteControlMode.SIDEWAYS);

        WiimoteMappedState pressed = mapper.map(raw(2_000_000_000L, false, false, false, false, false, false, false, false,
                false, false, true, false, false, false, false, false, false,
                0.0, 0.0, 1.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 1.0), WiimoteControlMode.SIDEWAYS);
        WiimoteMappedState shortlyAfterRelease = mapper.map(raw(2_050_000_000L, false, false, false, false, false, false, false, false,
                false, false, false, false, false, false, false, false, false,
                0.0, 0.0, 1.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 1.0), WiimoteControlMode.SIDEWAYS);
        WiimoteMappedState later = mapper.map(raw(2_150_000_000L, false, false, false, false, false, false, false, false,
                false, false, false, false, false, false, false, false, false,
                0.0, 0.0, 1.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 1.0), WiimoteControlMode.SIDEWAYS);
        WiimoteMappedState settled = mapper.map(raw(2_250_000_000L, false, false, false, false, false, false, false, false,
                false, false, false, false, false, false, false, false, false,
                0.0, 0.0, 1.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 1.0), WiimoteControlMode.SIDEWAYS);

        assertTrue(pressed.attack());
        assertTrue(pressed.menuSelectHeld());
        assertTrue(shortlyAfterRelease.attack());
        assertTrue(shortlyAfterRelease.menuSelectHeld());
        assertTrue(later.attack());
        assertFalse(settled.attack());
    }

    @Test
    void nunchukStickAndButtonsMapToActions() {
        WiimoteControlMapper mapper = new WiimoteControlMapper();
        for (int i = 0; i < 8; i++) {
            mapper.map(raw(1_000_000_000L + i * 100_000_000L, true, false, true, false, false, false, false, false,
                    false, false, false, false, false, false, false, false, false,
                    0.0, 0.0, 1.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 1.0), WiimoteControlMode.NUNCHUK);
        }

        WiimoteMappedState mapped = mapper.map(raw(2_000_000_000L, true, false, true, false, false, false, false, false,
                true, false, false, false, false, false, true, true, false,
                0.0, 0.0, 1.0, 0.0, 0.0, 0.0, 1.0, 1.0, 0.0, 0.0, 1.0), WiimoteControlMode.NUNCHUK);

        assertTrue(mapped.right());
        assertTrue(mapped.jump());
        assertTrue(mapped.block());
        assertTrue(mapped.attack());
        assertTrue(mapped.menuUp());
        assertFalse(mapped.menuDown());
        assertTrue(mapped.menuRight());
        assertTrue(mapped.menuSelect());
        assertTrue(mapped.menuBack());
    }

    @Test
    void nunchukNegativeMenuYAxisMapsToDown() {
        WiimoteControlMapper mapper = new WiimoteControlMapper();
        for (int i = 0; i < 8; i++) {
            mapper.map(raw(1_000_000_000L + i * 100_000_000L, true, false, true, false, false, false, false, false,
                    false, false, false, false, false, false, false, false, false,
                    0.0, 0.0, 1.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 1.0), WiimoteControlMode.NUNCHUK);
        }

        WiimoteMappedState mapped = mapper.map(raw(2_000_000_000L, true, false, true, false, false, false, false, false,
                false, false, false, false, false, false, false, false, false,
                0.0, 0.0, 1.0, 0.0, 0.0, 0.0, 0.0, -1.0, 0.0, 0.0, 1.0), WiimoteControlMode.NUNCHUK);

        assertFalse(mapped.menuUp());
        assertTrue(mapped.menuDown());
    }

    @Test
    void homeButtonMapsToMenuPauseInBothModes() {
        WiimoteControlMapper sidewaysMapper = new WiimoteControlMapper();
        sidewaysMapper.map(raw(1_000_000_000L, false, false, false, false, false, false, false, false,
                false, false, false, false, false, false, false, false, false,
                0.0, 0.0, 1.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 1.0), WiimoteControlMode.SIDEWAYS);
        WiimoteMappedState sideways = sidewaysMapper.map(raw(2_000_000_000L, false, false, false, false, false, false, false, false,
                false, false, false, false, false, false, false, false, true,
                0.0, 0.0, 1.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 1.0), WiimoteControlMode.SIDEWAYS);

        WiimoteControlMapper nunchukMapper = new WiimoteControlMapper();
        nunchukMapper.map(raw(1_000_000_000L, false, false, true, false, false, false, false, false,
                false, false, false, false, false, false, false, false, false,
                0.0, 0.0, 1.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 1.0), WiimoteControlMode.NUNCHUK);
        WiimoteMappedState nunchuk = nunchukMapper.map(raw(2_000_000_000L, false, false, true, false, false, false, false, false,
                false, false, false, false, false, false, false, false, true,
                0.0, 0.0, 1.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 1.0), WiimoteControlMode.NUNCHUK);

        assertTrue(sideways.menuPauseHeld());
        assertTrue(sideways.menuPause());
        assertTrue(nunchuk.menuPauseHeld());
        assertTrue(nunchuk.menuPause());
    }

    @Test
    void nunchukMotionAndSidewaysShakeTriggerExpectedActions() {
        WiimoteControlMapper jumpMapper = new WiimoteControlMapper();
        jumpMapper.map(raw(1_000_000_000L, false, false, true, false, false, false, false, false,
                false, false, false, false, false, false, false, false, false,
                0.0, 0.0, 1.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 1.0), WiimoteControlMode.NUNCHUK);
        WiimoteMappedState jumpMapped = jumpMapper.map(raw(2_000_000_000L, false, false, true, false, false, false, false, false,
                false, false, false, false, false, false, false, false, false,
                0.0, 0.0, 1.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 1.55), WiimoteControlMode.NUNCHUK);
        assertTrue(jumpMapped.jump());
        assertFalse(jumpMapped.attack());

        WiimoteControlMapper attackMapper = new WiimoteControlMapper();
        attackMapper.map(raw(1_000_000_000L, false, true, true, true, false, false, false, false,
                false, false, false, false, false, false, false, false, false,
                0.0, 0.0, 1.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 1.0), WiimoteControlMode.NUNCHUK);
        WiimoteMappedState attackMapped = attackMapper.map(raw(2_000_000_000L, false, true, true, true, false, false, false, false,
                false, false, false, false, false, false, false, false, false,
                0.0, 0.0, 1.0, 0.0, 0.0, 170.0, 0.0, 0.0, 0.0, 0.0, 1.0), WiimoteControlMode.NUNCHUK);
        assertTrue(attackMapped.attack());

        WiimoteControlMapper specialMapper = new WiimoteControlMapper();
        specialMapper.map(raw(1_000_000_000L, false, false, false, false, false, false, false, false,
                false, false, false, false, false, false, false, false, false,
                0.0, 0.0, 1.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 1.0), WiimoteControlMode.SIDEWAYS);
        WiimoteMappedState specialMapped = specialMapper.map(raw(2_000_000_000L, false, false, false, false, false, false, false, false,
                false, false, false, false, false, false, false, false, false,
                0.0, 0.0, 1.95, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 1.0), WiimoteControlMode.SIDEWAYS);
        assertTrue(specialMapped.special());
        assertFalse(specialMapped.attack());
    }

    private WiimoteRawInput raw(long sampleNanos,
                                boolean motionPlusAvailable,
                                boolean motionPlusActive,
                                boolean nunchukConnected,
                                boolean gyroAvailable,
                                boolean dpadUp,
                                boolean dpadDown,
                                boolean dpadLeft,
                                boolean dpadRight,
                                boolean buttonA,
                                boolean buttonB,
                                boolean buttonOne,
                                boolean buttonTwo,
                                boolean buttonPlus,
                                boolean buttonMinus,
                                boolean buttonC,
                                boolean buttonZ,
                                boolean buttonHome,
                                double remoteAccelX,
                                double remoteAccelY,
                                double remoteAccelZ,
                                double gyroYaw,
                                double gyroRoll,
                                double gyroPitch,
                                double nunchukStickX,
                                double nunchukStickY,
                                double nunchukAccelX,
                                double nunchukAccelY,
                                double nunchukAccelZ) {
        return new WiimoteRawInput(
                true,
                motionPlusAvailable,
                motionPlusActive,
                nunchukConnected,
                gyroAvailable,
                dpadUp,
                dpadDown,
                dpadLeft,
                dpadRight,
                buttonA,
                buttonB,
                buttonOne,
                buttonTwo,
                buttonPlus,
                buttonMinus,
                buttonHome,
                buttonC,
                buttonZ,
                remoteAccelX,
                remoteAccelY,
                remoteAccelZ,
                gyroYaw,
                gyroRoll,
                gyroPitch,
                nunchukStickX,
                nunchukStickY,
                nunchukAccelX,
                nunchukAccelY,
                nunchukAccelZ,
                sampleNanos,
                "connected"
        );
    }
}
