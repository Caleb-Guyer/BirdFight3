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

final class OnlineRelayHost implements NetworkSessionHost {
    private final BirdGame3 game;
    private final String relayHost;
    private final int relayPort;
    private final String roomName;
    private final String hostName;
    private final BlockingQueue<byte[]> outbound = new LinkedBlockingQueue<>();
    private Socket socket;
    private DataInputStream in;
    private DataOutputStream out;
    private Thread readThread;
    private Thread writeThread;
    private volatile boolean running;
    private volatile boolean notifyOnDisconnect = true;
    private volatile int clientCount;
    private String roomCode = "";
    private String lastError = "";

    OnlineRelayHost(BirdGame3 game, String relayHost, int relayPort, String roomName, String hostName) {
        this.game = game;
        this.relayHost = relayHost;
        this.relayPort = relayPort;
        this.roomName = OnlineRelayProtocol.sanitizeDisplayText(roomName, "Open Room", 32);
        this.hostName = OnlineRelayProtocol.sanitizeDisplayText(hostName, "Host", 24);
    }

    @Override
    public boolean start() {
        if (running) return true;
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
            OnlineRelayProtocol.writeFramed(newOut, OnlineRelayProtocol.buildMessage(OnlineRelayProtocol.MSG_CREATE_ROOM, relayOut -> {
                relayOut.writeUTF(this.roomName);
                relayOut.writeUTF(this.hostName);
                relayOut.writeBoolean(true);
            }));

            byte[] payload = OnlineRelayProtocol.readFramed(newIn);
            DataInputStream msgIn = new DataInputStream(new ByteArrayInputStream(payload));
            byte type = msgIn.readByte();
            if (type == OnlineRelayProtocol.MSG_ERROR) {
                lastError = msgIn.readUTF();
                newSocket.close();
                return false;
            }
            if (type != OnlineRelayProtocol.MSG_ROOM_CREATED) {
                lastError = "Unexpected relay response.";
                newSocket.close();
                return false;
            }

            roomCode = msgIn.readUTF();
            socket = newSocket;
            in = newIn;
            out = newOut;
            clientCount = 0;
            notifyOnDisconnect = true;
            running = true;
            readThread = new Thread(this::readLoop, "OnlineRelayHost-Read");
            writeThread = new Thread(this::writeLoop, "OnlineRelayHost-Write");
            readThread.setDaemon(true);
            writeThread.setDaemon(true);
            readThread.start();
            writeThread.start();
            game.onOnlineRoomCreated(roomCode, roomName);
            return true;
        } catch (IOException e) {
            lastError = e.getMessage() != null ? e.getMessage() : "Could not reach relay server.";
            stop();
            return false;
        }
    }

    @Override
    public void stop() {
        notifyOnDisconnect = false;
        disconnectInternal();
    }

    @Override
    public boolean hasClients() {
        return clientCount > 0;
    }

    @Override
    public void broadcastLobby(MapType map, boolean mapRandom, boolean[] connected, BirdType[] birds,
                               boolean[] randomBirds, String[] skinKeys, boolean[] ready) {
        try {
            byte[] lanPayload = LanProtocol.buildMessage(LanProtocol.MSG_LOBBY, lanOut -> {
                lanOut.writeInt(mapRandom ? LanProtocol.MAP_RANDOM : map.ordinal());
                for (int i = 0; i < 4; i++) {
                    lanOut.writeBoolean(connected[i]);
                    lanOut.writeBoolean(ready != null && ready[i]);
                    if (randomBirds != null && randomBirds[i]) {
                        lanOut.writeInt(LanProtocol.BIRD_RANDOM);
                    } else {
                        lanOut.writeInt(birds[i] != null ? birds[i].ordinal() : -1);
                    }
                    String skinKey = skinKeys != null ? skinKeys[i] : null;
                    lanOut.writeUTF(skinKey == null ? "" : skinKey);
                }
            });
            enqueueOutbound(OnlineRelayProtocol.encodeLanPayload(OnlineRelayProtocol.MSG_HOST_BROADCAST, lanPayload));
        } catch (IOException ignored) {
        }
    }

    @Override
    public void broadcastStart(MapType map, long seed, boolean[] connected, BirdType[] birds, String[] skinKeys) {
        try {
            byte[] lanPayload = LanProtocol.buildMessage(LanProtocol.MSG_START, lanOut -> {
                lanOut.writeInt(map.ordinal());
                lanOut.writeLong(seed);
                for (int i = 0; i < 4; i++) {
                    lanOut.writeBoolean(connected[i]);
                    lanOut.writeInt(birds[i] != null ? birds[i].ordinal() : -1);
                    String skinKey = skinKeys != null ? skinKeys[i] : null;
                    lanOut.writeUTF(skinKey == null ? "" : skinKey);
                }
            });
            enqueueOutbound(OnlineRelayProtocol.encodeLanPayload(OnlineRelayProtocol.MSG_HOST_BROADCAST, lanPayload));
        } catch (IOException ignored) {
        }
    }

    @Override
    public void broadcastState(LanState state) {
        try {
            enqueueOutbound(OnlineRelayProtocol.encodeLanPayload(
                    OnlineRelayProtocol.MSG_HOST_BROADCAST,
                    LanProtocol.buildMessage(LanProtocol.MSG_STATE, state::write)
            ));
        } catch (IOException ignored) {
        }
    }

    @Override
    public void broadcastMatchEnd(int winnerIndex) {
        try {
            enqueueOutbound(OnlineRelayProtocol.encodeLanPayload(
                    OnlineRelayProtocol.MSG_HOST_BROADCAST,
                    LanProtocol.buildMessage(LanProtocol.MSG_END, lanOut -> lanOut.writeInt(winnerIndex))
            ));
        } catch (IOException ignored) {
        }
    }

    @Override
    public void broadcastCountdown(int seconds) {
        try {
            enqueueOutbound(OnlineRelayProtocol.encodeLanPayload(
                    OnlineRelayProtocol.MSG_HOST_BROADCAST,
                    LanProtocol.buildMessage(LanProtocol.MSG_COUNTDOWN, lanOut -> lanOut.writeInt(seconds))
            ));
        } catch (IOException ignored) {
        }
    }

    @Override
    public void broadcastResultsAction(int action, int delayMs) {
        try {
            enqueueOutbound(OnlineRelayProtocol.encodeLanPayload(
                    OnlineRelayProtocol.MSG_HOST_BROADCAST,
                    LanProtocol.buildMessage(LanProtocol.MSG_RESULTS_ACTION, lanOut -> {
                        lanOut.writeInt(action);
                        lanOut.writeInt(delayMs);
                    })
            ));
        } catch (IOException ignored) {
        }
    }

    String getLastError() {
        return lastError;
    }

    String getRoomCode() {
        return roomCode;
    }

    private void enqueueOutbound(byte[] payload) {
        if (!running || payload == null) return;
        if (!outbound.offer(payload)) {
            lastError = "Could not queue relay payload.";
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
                    case OnlineRelayProtocol.MSG_PLAYER_JOINED -> {
                        int slot = msgIn.readInt();
                        clientCount++;
                        game.onLanClientConnected(slot);
                    }
                    case OnlineRelayProtocol.MSG_PLAYER_LEFT -> {
                        int slot = msgIn.readInt();
                        clientCount = Math.max(0, clientCount - 1);
                        game.onLanClientDisconnected(slot);
                    }
                    case OnlineRelayProtocol.MSG_GUEST_PAYLOAD -> {
                        int slot = msgIn.readInt();
                        byte[] lanPayload = OnlineRelayProtocol.readByteArray(msgIn);
                        try {
                            LanPayloadRouter.handleClientPayload(game, slot, lanPayload);
                        } catch (IOException ignored) {
                        }
                    }
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
