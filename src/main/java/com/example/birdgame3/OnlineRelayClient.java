package com.example.birdgame3;

import com.example.birdgame3.BirdGame3.BirdType;
import com.example.birdgame3.BirdGame3.MapType;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

final class OnlineRelayClient implements NetworkSessionClient {
    private final BirdGame3 game;
    private final String relayHost;
    private final int relayPort;
    private final String roomCode;
    private final BlockingQueue<byte[]> outbound = new LinkedBlockingQueue<>();
    private Socket socket;
    private DataInputStream in;
    private DataOutputStream out;
    private Thread readThread;
    private Thread writeThread;
    private volatile boolean running;
    private volatile boolean closed;
    private volatile boolean notifyOnDisconnect = true;
    private String lastError = "";

    OnlineRelayClient(BirdGame3 game, String relayHost, int relayPort, String roomCode) {
        this.game = game;
        this.relayHost = relayHost;
        this.relayPort = relayPort;
        this.roomCode = OnlineRelayProtocol.sanitizeRoomCode(roomCode);
    }

    @Override
    public String getLastError() {
        return lastError;
    }

    @Override
    public boolean connect() {
        if (running) return true;
        if (closed) {
            lastError = "Connection cancelled.";
            return false;
        }
        try {
            Socket newSocket = new Socket();
            newSocket.setTcpNoDelay(true);
            newSocket.connect(new InetSocketAddress(relayHost, relayPort), 5_000);
            DataInputStream newIn = new DataInputStream(newSocket.getInputStream());
            DataOutputStream newOut = new DataOutputStream(newSocket.getOutputStream());

            OnlineRelayProtocol.writeFramed(newOut, OnlineRelayProtocol.buildMessage(OnlineRelayProtocol.MSG_HELLO, relayOut -> {
                relayOut.writeInt(OnlineRelayProtocol.VERSION);
                relayOut.writeInt(LanProtocol.VERSION);
            }));
            OnlineRelayProtocol.writeFramed(newOut, OnlineRelayProtocol.buildMessage(OnlineRelayProtocol.MSG_JOIN_ROOM, relayOut -> relayOut.writeUTF(roomCode)));

            byte[] payload = OnlineRelayProtocol.readFramed(newIn);
            DataInputStream msgIn = new DataInputStream(new ByteArrayInputStream(payload));
            byte type = msgIn.readByte();
            if (type == OnlineRelayProtocol.MSG_ERROR) {
                lastError = msgIn.readUTF();
                newSocket.close();
                return false;
            }
            if (type != OnlineRelayProtocol.MSG_JOINED_ROOM) {
                lastError = "Unexpected relay response.";
                newSocket.close();
                return false;
            }

            String joinedCode = msgIn.readUTF();
            String joinedRoomName = msgIn.readUTF();
            String joinedHostName = msgIn.readUTF();

            socket = newSocket;
            in = newIn;
            out = newOut;
            notifyOnDisconnect = true;
            running = true;
            readThread = new Thread(this::readLoop, "OnlineRelayClient-Read");
            writeThread = new Thread(this::writeLoop, "OnlineRelayClient-Write");
            readThread.setDaemon(true);
            writeThread.setDaemon(true);
            readThread.start();
            writeThread.start();
            game.onOnlineRoomJoined(joinedCode, joinedRoomName, joinedHostName);
            sendHello();
            return true;
        } catch (IOException e) {
            lastError = e.getMessage() != null ? e.getMessage() : "Connection failed.";
            disconnect();
            return false;
        }
    }

    @Override
    public void disconnect() {
        closed = true;
        notifyOnDisconnect = false;
        disconnectInternal();
    }

    @Override
    public void sendSelect(BirdType type, boolean random, String skinKey) {
        try {
            int ord = random ? LanProtocol.BIRD_RANDOM : (type != null ? type.ordinal() : -1);
            enqueueLanPayload(LanProtocol.buildMessage(LanProtocol.MSG_SELECT, lanOut -> {
                lanOut.writeInt(ord);
                lanOut.writeUTF(skinKey == null ? "" : skinKey);
            }));
        } catch (IOException ignored) {
        }
    }

    @Override
    public void sendMapVote(MapType map, boolean random) {
        try {
            int ord = random ? LanProtocol.MAP_RANDOM : (map != null ? map.ordinal() : -1);
            enqueueLanPayload(LanProtocol.buildMessage(LanProtocol.MSG_MAP_VOTE, lanOut -> lanOut.writeInt(ord)));
        } catch (IOException ignored) {
        }
    }

    @Override
    public void sendReady(boolean ready) {
        try {
            enqueueLanPayload(LanProtocol.buildMessage(LanProtocol.MSG_READY, lanOut -> lanOut.writeBoolean(ready)));
        } catch (IOException ignored) {
        }
    }

    @Override
    public void sendInputMask(int mask) {
        try {
            enqueueLanPayload(LanProtocol.buildMessage(LanProtocol.MSG_INPUT, lanOut -> lanOut.writeInt(mask)));
        } catch (IOException ignored) {
        }
    }

    private void sendHello() throws IOException {
        enqueueLanPayload(LanProtocol.buildMessage(LanProtocol.MSG_HELLO, lanOut -> lanOut.writeInt(LanProtocol.VERSION)));
    }

    private void enqueueLanPayload(byte[] lanPayload) throws IOException {
        enqueueOutbound(OnlineRelayProtocol.encodeLanPayload(OnlineRelayProtocol.MSG_GUEST_TO_HOST, lanPayload));
    }

    private void enqueueOutbound(byte[] payload) {
        if (!running || payload == null) return;
        if (!outbound.offer(payload)) {
            lastError = "Failed to queue relay message.";
            disconnectInternal();
        }
    }

    private void disconnectInternal() {
        running = false;
        if (readThread != null) readThread.interrupt();
        if (writeThread != null) writeThread.interrupt();
        try {
            if (socket != null) socket.close();
        } catch (IOException ignored) {
        }
    }

    private void readLoop() {
        try {
            while (running) {
                byte[] payload = OnlineRelayProtocol.readFramed(in);
                DataInputStream msgIn = new DataInputStream(new ByteArrayInputStream(payload));
                byte type = msgIn.readByte();
                switch (type) {
                    case OnlineRelayProtocol.MSG_HOST_PAYLOAD -> LanPayloadRouter.handleServerPayload(game, OnlineRelayProtocol.readByteArray(msgIn));
                    case OnlineRelayProtocol.MSG_ROOM_CLOSED, OnlineRelayProtocol.MSG_ERROR -> {
                        lastError = msgIn.readUTF();
                        running = false;
                    }
                    default -> {
                    }
                }
            }
        } catch (IOException ignored) {
            if (lastError == null || lastError.isBlank()) {
                lastError = "Relay connection closed.";
            }
        } finally {
            boolean notify = notifyOnDisconnect;
            disconnectInternal();
            if (notify) {
                game.onOnlineRelayClosed(lastError == null || lastError.isBlank() ? "Relay connection closed." : lastError);
            }
        }
    }

    private void writeLoop() {
        try {
            while (running) {
                OnlineRelayProtocol.writeFramed(out, outbound.take());
            }
        } catch (InterruptedException ignored) {
            Thread.currentThread().interrupt();
        } catch (IOException ignored) {
            if (lastError == null || lastError.isBlank()) {
                lastError = "Relay connection closed.";
            }
        } finally {
            disconnectInternal();
        }
    }
}
