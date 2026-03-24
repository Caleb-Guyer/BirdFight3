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
}
