package com.example.birdgame3;

import org.junit.jupiter.api.Test;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LanTransportTest {
    @Test
    void serverAcceptsMatchingHandshakeBeforeClaimingSlot() throws Exception {
        int port = freePort();
        CapturingGame game = new CapturingGame();
        LanHostServer server = new LanHostServer(game, port, false);
        assertTrue(server.start());
        try (Socket socket = new Socket("127.0.0.1", port)) {
            DataOutputStream out = new DataOutputStream(socket.getOutputStream());
            DataInputStream in = new DataInputStream(socket.getInputStream());
            LanProtocol.writeFramed(out, LanProtocol.buildMessage(LanProtocol.MSG_HELLO,
                    hello -> hello.writeInt(LanProtocol.VERSION)));

            LanPayloadRouter.Welcome welcome = LanPayloadRouter.readServerWelcome(LanProtocol.readFramed(in));
            assertEquals(LanProtocol.VERSION, welcome.version());
            assertEquals(1, welcome.slot());
            assertTrue(game.connected.await(2, TimeUnit.SECONDS));
            assertEquals(1, game.connectedSlot);
        } finally {
            server.stop();
        }
    }

    @Test
    void serverRejectsMismatchedVersionWithoutClaimingLobbySlot() throws Exception {
        int port = freePort();
        CapturingGame game = new CapturingGame();
        LanHostServer server = new LanHostServer(game, port, false);
        assertTrue(server.start());
        try (Socket socket = new Socket("127.0.0.1", port)) {
            DataOutputStream out = new DataOutputStream(socket.getOutputStream());
            DataInputStream in = new DataInputStream(socket.getInputStream());
            LanProtocol.writeFramed(out, LanProtocol.buildMessage(LanProtocol.MSG_HELLO,
                    hello -> hello.writeInt(LanProtocol.VERSION - 1)));

            java.io.IOException rejection = assertThrows(java.io.IOException.class,
                    () -> LanPayloadRouter.readServerWelcome(LanProtocol.readFramed(in)));
            assertTrue(rejection.getMessage().contains("versions do not match"));
            assertFalse(game.connected.await(200, TimeUnit.MILLISECONDS));
        } finally {
            server.stop();
        }
    }

    @Test
    void clientAndServerCompleteDirectConnectHandshake() throws Exception {
        int port = freePort();
        CapturingGame hostGame = new CapturingGame();
        CapturingGame clientGame = new CapturingGame();
        LanHostServer server = new LanHostServer(hostGame, port, false);
        assertTrue(server.start());
        LanClient client = new LanClient(clientGame, new NetworkEndpoint("127.0.0.1", port));
        try {
            assertTrue(client.connect(), client.getLastError());
            assertTrue(hostGame.connected.await(2, TimeUnit.SECONDS));
            assertTrue(clientGame.welcomed.await(2, TimeUnit.SECONDS));
            assertEquals(1, clientGame.welcomeSlot);
        } finally {
            client.disconnect();
            server.stop();
        }
    }

    private static int freePort() throws Exception {
        try (ServerSocket socket = new ServerSocket(0)) {
            return socket.getLocalPort();
        }
    }

    private static final class CapturingGame extends BirdGame3 {
        private final CountDownLatch connected = new CountDownLatch(1);
        private final CountDownLatch welcomed = new CountDownLatch(1);
        private volatile int connectedSlot = -1;
        private volatile int welcomeSlot = -1;

        @Override
        void onLanClientConnected(int slot) {
            connectedSlot = slot;
            connected.countDown();
        }

        @Override
        void onLanClientDisconnected(int slot) {
        }

        @Override
        void onLanWelcome(int slot) {
            welcomeSlot = slot;
            welcomed.countDown();
        }

        @Override
        void onLanDisconnected(String reason) {
        }

        @Override
        void onLanServerError(java.io.IOException error) {
        }
    }
}
