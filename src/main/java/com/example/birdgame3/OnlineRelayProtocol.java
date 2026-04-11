package com.example.birdgame3;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

final class OnlineRelayProtocol {
    static final int VERSION = 1;
    static final String DEFAULT_HOST = "127.0.0.1";
    static final int DEFAULT_PORT = 29001;
    static final int MAX_ROOMS = 512;
    static final int ROOM_CODE_LENGTH = 6;

    static final byte MSG_HELLO = 1;
    static final byte MSG_CREATE_ROOM = 2;
    static final byte MSG_LIST_ROOMS = 3;
    static final byte MSG_JOIN_ROOM = 4;
    static final byte MSG_HOST_BROADCAST = 5;
    static final byte MSG_GUEST_TO_HOST = 6;

    static final byte MSG_ROOM_CREATED = 21;
    static final byte MSG_ROOM_LIST = 22;
    static final byte MSG_JOINED_ROOM = 23;
    static final byte MSG_HOST_PAYLOAD = 24;
    static final byte MSG_GUEST_PAYLOAD = 25;
    static final byte MSG_PLAYER_JOINED = 26;
    static final byte MSG_PLAYER_LEFT = 27;
    static final byte MSG_ERROR = 28;
    static final byte MSG_ROOM_CLOSED = 29;

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
            throw new IOException("Invalid relay message length: " + length);
        }
        byte[] payload = new byte[length];
        in.readFully(payload);
        return payload;
    }

    static byte[] encodeLanPayload(byte relayType, byte[] lanPayload) throws IOException {
        return buildMessage(relayType, out -> writeByteArray(out, lanPayload));
    }

    static void writeByteArray(DataOutputStream out, byte[] value) throws IOException {
        if (value == null) {
            out.writeInt(0);
            return;
        }
        out.writeInt(value.length);
        out.write(value);
    }

    static byte[] readByteArray(DataInputStream in) throws IOException {
        int length = in.readInt();
        if (length < 0 || length > 5_000_000) {
            throw new IOException("Invalid relay payload length: " + length);
        }
        byte[] value = new byte[length];
        in.readFully(value);
        return value;
    }

    static String sanitizeRoomCode(String roomCode) {
        if (roomCode == null) {
            return "";
        }
        return roomCode.trim().toUpperCase();
    }

    static String sanitizeDisplayText(String value, String fallback, int maxLength) {
        String trimmed = value == null ? "" : value.trim();
        if (trimmed.isEmpty()) {
            return fallback;
        }
        if (trimmed.length() > maxLength) {
            return trimmed.substring(0, maxLength).trim();
        }
        return trimmed;
    }

    private OnlineRelayProtocol() {
    }
}
