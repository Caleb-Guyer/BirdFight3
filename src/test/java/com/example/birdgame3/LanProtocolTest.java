package com.example.birdgame3;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
        state.dodgeTypeOrdinal = 2;
        state.dodgeTimer = 17;
        state.dodgeInvulnerabilityTimer = 9;
        state.dodgeCooldown = 6;
        state.dodgeDirection = -1;
        state.airDodgeAvailable = false;

        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        state.write(new DataOutputStream(bos));

        LanBirdState decoded = LanBirdState.read(new DataInputStream(new ByteArrayInputStream(bos.toByteArray())));

        assertEquals(2, decoded.dodgeTypeOrdinal);
        assertEquals(17, decoded.dodgeTimer);
        assertEquals(9, decoded.dodgeInvulnerabilityTimer);
        assertEquals(6, decoded.dodgeCooldown);
        assertEquals(-1, decoded.dodgeDirection);
        assertEquals(false, decoded.airDodgeAvailable);
    }
}
