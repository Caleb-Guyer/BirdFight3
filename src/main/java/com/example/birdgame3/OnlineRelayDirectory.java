package com.example.birdgame3;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

final class OnlineRelayDirectory {
    static List<OnlineRoomInfo> fetchRooms(String relayHost) throws IOException {
        return fetchRooms(relayHost, OnlineRelayProtocol.DEFAULT_PORT);
    }

    static List<OnlineRoomInfo> fetchRooms(String relayHost, int relayPort) throws IOException {
        try (Socket socket = new Socket()) {
            socket.setTcpNoDelay(true);
            socket.connect(new InetSocketAddress(relayHost, relayPort), 5_000);
            DataInputStream in = new DataInputStream(socket.getInputStream());
            DataOutputStream out = new DataOutputStream(socket.getOutputStream());
            OnlineRelayProtocol.writeFramed(out, OnlineRelayProtocol.buildMessage(OnlineRelayProtocol.MSG_HELLO, relayOut -> {
                relayOut.writeInt(OnlineRelayProtocol.VERSION);
                relayOut.writeInt(LanProtocol.VERSION);
            }));
            OnlineRelayProtocol.writeFramed(out, OnlineRelayProtocol.buildMessage(OnlineRelayProtocol.MSG_LIST_ROOMS, null));

            byte[] payload = OnlineRelayProtocol.readFramed(in);
            DataInputStream msgIn = new DataInputStream(new ByteArrayInputStream(payload));
            byte type = msgIn.readByte();
            if (type == OnlineRelayProtocol.MSG_ERROR) {
                throw new IOException(msgIn.readUTF());
            }
            if (type != OnlineRelayProtocol.MSG_ROOM_LIST) {
                throw new IOException("Unexpected relay response.");
            }

            int count = Math.max(0, msgIn.readInt());
            List<OnlineRoomInfo> rooms = new ArrayList<>(count);
            for (int i = 0; i < count; i++) {
                rooms.add(new OnlineRoomInfo(
                        msgIn.readUTF(),
                        msgIn.readUTF(),
                        msgIn.readUTF(),
                        msgIn.readInt(),
                        msgIn.readInt()
                ));
            }
            return rooms;
        }
    }

    private OnlineRelayDirectory() {
    }
}
