package com.example.birdgame3;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.Socket;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OnlineRelayServerTest {
    private OnlineRelayServer server;

    @BeforeEach
    void setUp() {
        server = new OnlineRelayServer(0);
        assertTrue(server.start());
    }

    @AfterEach
    void tearDown() {
        if (server != null) {
            server.stop();
        }
    }

    @Test
    void createsListsAndRelaysRoomTraffic() throws Exception {
        try (Socket hostSocket = socket();
             Socket guestSocket = socket()) {
            DataInputStream hostIn = new DataInputStream(hostSocket.getInputStream());
            DataOutputStream hostOut = new DataOutputStream(hostSocket.getOutputStream());
            DataInputStream guestIn = new DataInputStream(guestSocket.getInputStream());
            DataOutputStream guestOut = new DataOutputStream(guestSocket.getOutputStream());

            sendHello(hostOut);
            OnlineRelayProtocol.writeFramed(hostOut, OnlineRelayProtocol.buildMessage(OnlineRelayProtocol.MSG_CREATE_ROOM, out -> {
                out.writeUTF("Night Owls");
                out.writeUTF("HostBird");
                out.writeBoolean(true);
            }));

            DataInputStream created = messageIn(hostIn);
            assertEquals(OnlineRelayProtocol.MSG_ROOM_CREATED, created.readByte());
            String roomCode = created.readUTF();
            assertFalse(roomCode.isBlank());

            List<OnlineRoomInfo> rooms = OnlineRelayDirectory.fetchRooms("127.0.0.1", server.getPort());
            assertEquals(1, rooms.size());
            assertEquals(roomCode, rooms.getFirst().code());
            assertEquals("Night Owls", rooms.getFirst().roomName());
            assertEquals("HostBird", rooms.getFirst().hostName());
            assertEquals(1, rooms.getFirst().playerCount());

            sendHello(guestOut);
            OnlineRelayProtocol.writeFramed(guestOut, OnlineRelayProtocol.buildMessage(OnlineRelayProtocol.MSG_JOIN_ROOM, out -> out.writeUTF(roomCode)));

            DataInputStream joined = messageIn(guestIn);
            assertEquals(OnlineRelayProtocol.MSG_JOINED_ROOM, joined.readByte());
            assertEquals(roomCode, joined.readUTF());
            assertEquals("Night Owls", joined.readUTF());
            assertEquals("HostBird", joined.readUTF());

            DataInputStream welcomeRelay = messageIn(guestIn);
            assertEquals(OnlineRelayProtocol.MSG_HOST_PAYLOAD, welcomeRelay.readByte());
            byte[] welcomePayload = OnlineRelayProtocol.readByteArray(welcomeRelay);
            DataInputStream welcomeIn = new DataInputStream(new ByteArrayInputStream(welcomePayload));
            assertEquals(LanProtocol.MSG_WELCOME, welcomeIn.readByte());
            assertEquals(1, welcomeIn.readInt());
            assertEquals(LanProtocol.VERSION, welcomeIn.readInt());

            DataInputStream joinedNotice = messageIn(hostIn);
            assertEquals(OnlineRelayProtocol.MSG_PLAYER_JOINED, joinedNotice.readByte());
            assertEquals(1, joinedNotice.readInt());

            byte[] readyPayload = LanProtocol.buildMessage(LanProtocol.MSG_READY, out -> out.writeBoolean(true));
            OnlineRelayProtocol.writeFramed(guestOut, OnlineRelayProtocol.buildMessage(OnlineRelayProtocol.MSG_GUEST_TO_HOST,
                    out -> OnlineRelayProtocol.writeByteArray(out, readyPayload)));

            DataInputStream guestPayloadMsg = messageIn(hostIn);
            assertEquals(OnlineRelayProtocol.MSG_GUEST_PAYLOAD, guestPayloadMsg.readByte());
            assertEquals(1, guestPayloadMsg.readInt());
            byte[] forwardedReady = OnlineRelayProtocol.readByteArray(guestPayloadMsg);
            assertEquals(readyPayload.length, forwardedReady.length);
            assertEquals(LanProtocol.MSG_READY, forwardedReady[0]);

            byte[] countdownPayload = LanProtocol.buildMessage(LanProtocol.MSG_COUNTDOWN, out -> out.writeInt(4));
            OnlineRelayProtocol.writeFramed(hostOut, OnlineRelayProtocol.buildMessage(OnlineRelayProtocol.MSG_HOST_BROADCAST,
                    out -> OnlineRelayProtocol.writeByteArray(out, countdownPayload)));

            DataInputStream hostPayloadMsg = messageIn(guestIn);
            assertEquals(OnlineRelayProtocol.MSG_HOST_PAYLOAD, hostPayloadMsg.readByte());
            byte[] forwardedCountdown = OnlineRelayProtocol.readByteArray(hostPayloadMsg);
            DataInputStream countdownIn = new DataInputStream(new ByteArrayInputStream(forwardedCountdown));
            assertEquals(LanProtocol.MSG_COUNTDOWN, countdownIn.readByte());
            assertEquals(4, countdownIn.readInt());
        }
    }

    private Socket socket() throws Exception {
        Socket socket = new Socket("127.0.0.1", server.getPort());
        socket.setSoTimeout(4_000);
        socket.setTcpNoDelay(true);
        return socket;
    }

    private void sendHello(DataOutputStream out) throws Exception {
        OnlineRelayProtocol.writeFramed(out, OnlineRelayProtocol.buildMessage(OnlineRelayProtocol.MSG_HELLO, relayOut -> {
            relayOut.writeInt(OnlineRelayProtocol.VERSION);
            relayOut.writeInt(LanProtocol.VERSION);
        }));
    }

    private DataInputStream messageIn(DataInputStream in) throws Exception {
        byte[] payload = OnlineRelayProtocol.readFramed(in);
        assertNotNull(payload);
        return new DataInputStream(new ByteArrayInputStream(payload));
    }
}
