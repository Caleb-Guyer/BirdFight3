package com.example.birdgame3;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

class LanProtocolTest {
    @Test
    void framedPayloadRoundTrips() throws IOException {
        byte[] payload = LanProtocol.buildMessage(LanProtocol.MSG_READY, out -> out.writeBoolean(true));

        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        LanProtocol.writeFramed(new DataOutputStream(bos), payload);

        byte[] read = LanProtocol.readFramed(new DataInputStream(new ByteArrayInputStream(bos.toByteArray())));
        assertArrayEquals(payload, read);

        DataInputStream in = new DataInputStream(new ByteArrayInputStream(read));
        assertEquals(LanProtocol.MSG_READY, in.readByte());
        assertTrue(in.readBoolean());
    }

    @Test
    void invalidFrameLengthIsRejected() throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        DataOutputStream out = new DataOutputStream(bos);
        out.writeInt(5_000_001);
        out.flush();

        DataInputStream in = new DataInputStream(new ByteArrayInputStream(bos.toByteArray()));
        assertThrows(IOException.class, () -> LanProtocol.readFramed(in));
    }

    @Test
    void lanBirdStateRoundTripsDodgeFields() throws IOException {
        LanBirdState state = new LanBirdState();
        state.attackChargeFrames = 14;
        state.pendingGroundAttackFrames = 5;
        state.pendingGroundAttackVariantOrdinal = 3;
        state.chargingAttackVariantOrdinal = 6;
        state.activeAttackVariantOrdinal = 10;
        state.dodgeTypeOrdinal = 2;
        state.dodgeTimer = 17;
        state.dodgeInvulnerabilityTimer = 9;
        state.dodgeCooldown = 6;
        state.dodgeDirection = -1;
        state.airDodgeAvailable = false;
        state.activeAerialLandingLagFrames = 12;
        state.phoenixLavaReuseTimer = 31;
        state.roadrunnerMomentum = 74.5;
        state.roadrunnerBeepCharging = true;
        state.roadrunnerBeepChargeFrames = 28;
        state.roadrunnerBeepMaxChargeHoldFrames = 6;
        state.roadrunnerRicochetTimer = 9;
        state.roadrunnerRicochetSpeed = 27.5;
        state.roadrunnerDustDevilUsed = true;
        state.roadrunnerRoadBoostTimer = 18;
        state.roadrunnerSlipTimer = 44;
        state.roadrunnerSlipDirection = -1;
        state.penguinBellyCharging = true;
        state.penguinBellyChargeFrames = 41;
        state.penguinBellySlideTimer = 13;
        state.penguinBellyDirection = -1;
        state.penguinRocketTimer = 21;
        state.penguinUpSpecialUsed = true;
        state.penguinSnowFortActive = true;
        state.penguinSnowFortX = 1234.5;
        state.penguinSnowFortY = 2100.25;
        state.penguinSnowFortHealth = 33;

        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        state.write(new DataOutputStream(bos));

        LanBirdState decoded = LanBirdState.read(new DataInputStream(new ByteArrayInputStream(bos.toByteArray())));

        assertEquals(14, decoded.attackChargeFrames);
        assertEquals(5, decoded.pendingGroundAttackFrames);
        assertEquals(3, decoded.pendingGroundAttackVariantOrdinal);
        assertEquals(6, decoded.chargingAttackVariantOrdinal);
        assertEquals(10, decoded.activeAttackVariantOrdinal);
        assertEquals(2, decoded.dodgeTypeOrdinal);
        assertEquals(17, decoded.dodgeTimer);
        assertEquals(9, decoded.dodgeInvulnerabilityTimer);
        assertEquals(6, decoded.dodgeCooldown);
        assertEquals(-1, decoded.dodgeDirection);
        assertFalse(decoded.airDodgeAvailable);
        assertEquals(12, decoded.activeAerialLandingLagFrames);
        assertEquals(31, decoded.phoenixLavaReuseTimer);
        assertEquals(74.5, decoded.roadrunnerMomentum);
        assertTrue(decoded.roadrunnerBeepCharging);
        assertEquals(28, decoded.roadrunnerBeepChargeFrames);
        assertEquals(6, decoded.roadrunnerBeepMaxChargeHoldFrames);
        assertEquals(9, decoded.roadrunnerRicochetTimer);
        assertEquals(27.5, decoded.roadrunnerRicochetSpeed);
        assertTrue(decoded.roadrunnerDustDevilUsed);
        assertEquals(18, decoded.roadrunnerRoadBoostTimer);
        assertEquals(44, decoded.roadrunnerSlipTimer);
        assertEquals(-1, decoded.roadrunnerSlipDirection);
        assertTrue(decoded.penguinBellyCharging);
        assertEquals(41, decoded.penguinBellyChargeFrames);
        assertEquals(13, decoded.penguinBellySlideTimer);
        assertEquals(-1, decoded.penguinBellyDirection);
        assertEquals(21, decoded.penguinRocketTimer);
        assertTrue(decoded.penguinUpSpecialUsed);
        assertTrue(decoded.penguinSnowFortActive);
        assertEquals(1234.5, decoded.penguinSnowFortX);
        assertEquals(2100.25, decoded.penguinSnowFortY);
        assertEquals(33, decoded.penguinSnowFortHealth);
    }
}
