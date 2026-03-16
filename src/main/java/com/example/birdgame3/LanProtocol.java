package com.example.birdgame3;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

final class LanProtocol {
    static final int VERSION = 4;
    static final int DEFAULT_PORT = 28999;

    static final byte MSG_HELLO = 1;
    static final byte MSG_WELCOME = 2;
    static final byte MSG_LOBBY = 3;
    static final byte MSG_SELECT = 4;
    static final byte MSG_INPUT = 5;
    static final byte MSG_START = 6;
    static final byte MSG_STATE = 7;
    static final byte MSG_END = 8;
    static final byte MSG_MAP_VOTE = 9;
    static final byte MSG_READY = 10;
    static final byte MSG_COUNTDOWN = 11;
    static final byte MSG_RESULTS_ACTION = 12;

    static final int MAP_RANDOM = -1;
    static final int BIRD_RANDOM = -2;

    static final int INPUT_LEFT = 1 << 0;
    static final int INPUT_RIGHT = 1 << 1;
    static final int INPUT_JUMP = 1 << 2;
    static final int INPUT_ATTACK = 1 << 3;
    static final int INPUT_SPECIAL = 1 << 4;
    static final int INPUT_BLOCK = 1 << 5;
    static final int INPUT_TAUNT_CYCLE = 1 << 6;
    static final int INPUT_TAUNT_EXEC = 1 << 7;

    @FunctionalInterface
    interface MessageWriter {
        void write(DataOutputStream out) throws IOException;
    }

    static byte[] buildMessage(byte type, MessageWriter writer) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream(512);
        DataOutputStream out = new DataOutputStream(bos);
        out.writeByte(type);
        if (writer != null) {
            writer.write(out);
        }
        out.flush();
        return bos.toByteArray();
    }

    static void writeFramed(DataOutputStream out, byte[] payload) throws IOException {
        out.writeInt(payload.length);
        out.write(payload);
        out.flush();
    }

    static byte[] readFramed(DataInputStream in) throws IOException {
        int length = in.readInt();
        if (length <= 0 || length > 5_000_000) {
            throw new IOException("Invalid LAN message length: " + length);
        }
        byte[] payload = new byte[length];
        in.readFully(payload);
        return payload;
    }

    private LanProtocol() {
    }
}
