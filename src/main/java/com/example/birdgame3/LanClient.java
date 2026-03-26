package com.example.birdgame3;

import com.example.birdgame3.BirdGame3.BirdType;
import com.example.birdgame3.BirdGame3.MapType;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

class LanClient {
    private final BirdGame3 game;
    private Socket socket;
    private DataInputStream in;
    private DataOutputStream out;
    private final BlockingQueue<byte[]> outbound = new LinkedBlockingQueue<>();
    private Thread readThread;
    private Thread writeThread;
    private volatile boolean running;
    private volatile boolean closed;
    private volatile boolean notifyOnDisconnect = true;
    private volatile int playerIndex = -1;
    private String lastError = "";

    LanClient(BirdGame3 game) {
        this.game = game;
    }

    String getLastError() {
        return lastError;
    }

    boolean connect(String host) {
        if (running) return true;
        if (closed) {
            lastError = "Connection cancelled.";
            return false;
        }
        try {
            Socket newSocket = new Socket(host, LanProtocol.DEFAULT_PORT);
            newSocket.setTcpNoDelay(true);
            DataInputStream newIn = new DataInputStream(newSocket.getInputStream());
            DataOutputStream newOut = new DataOutputStream(newSocket.getOutputStream());
            if (closed) {
                newSocket.close();
                lastError = "Connection cancelled.";
                return false;
            }
            socket = newSocket;
            in = newIn;
            out = newOut;
            notifyOnDisconnect = true;
            running = true;
            readThread = new Thread(this::readLoop, "LanClient-Read");
            writeThread = new Thread(this::writeLoop, "LanClient-Write");
            readThread.setDaemon(true);
            writeThread.setDaemon(true);
            readThread.start();
            writeThread.start();
            sendHello();
            return true;
        } catch (IOException e) {
            lastError = e.getMessage() != null ? e.getMessage() : "Connection failed.";
            disconnect();
            return false;
        }
    }

    void disconnect() {
        closed = true;
        notifyOnDisconnect = false;
        disconnectInternal();
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

    private void enqueueOutbound(byte[] payload) {
        if (!running || payload == null) return;
        if (!outbound.offer(payload)) {
            lastError = "Failed to queue LAN message.";
            disconnectInternal();
        }
    }

    void sendSelect(BirdType type, boolean random, String skinKey) {
        if (!running) return;
        try {
            int ord = random ? LanProtocol.BIRD_RANDOM : (type != null ? type.ordinal() : -1);
            byte[] msg = LanProtocol.buildMessage(LanProtocol.MSG_SELECT, out -> {
                out.writeInt(ord);
                out.writeUTF(skinKey == null ? "" : skinKey);
            });
            enqueueOutbound(msg);
        } catch (IOException ignored) {
        }
    }

    void sendMapVote(MapType map, boolean random) {
        if (!running) return;
        try {
            int ord = random ? LanProtocol.MAP_RANDOM : (map != null ? map.ordinal() : -1);
            byte[] msg = LanProtocol.buildMessage(LanProtocol.MSG_MAP_VOTE, out -> out.writeInt(ord));
            enqueueOutbound(msg);
        } catch (IOException ignored) {
        }
    }

    void sendReady(boolean ready) {
        if (!running) return;
        try {
            byte[] msg = LanProtocol.buildMessage(LanProtocol.MSG_READY, out -> out.writeBoolean(ready));
            enqueueOutbound(msg);
        } catch (IOException ignored) {
        }
    }

    void sendInputMask(int mask) {
        if (!running) return;
        try {
            byte[] msg = LanProtocol.buildMessage(LanProtocol.MSG_INPUT, out -> out.writeInt(mask));
            enqueueOutbound(msg);
        } catch (IOException ignored) {
        }
    }

    private void sendHello() throws IOException {
        byte[] msg = LanProtocol.buildMessage(LanProtocol.MSG_HELLO, out -> out.writeInt(LanProtocol.VERSION));
        enqueueOutbound(msg);
    }

    private void readLoop() {
        try {
            while (running) {
                byte[] payload = LanProtocol.readFramed(in);
                DataInputStream msgIn = new DataInputStream(new ByteArrayInputStream(payload));
                byte type = msgIn.readByte();
                switch (type) {
                    case LanProtocol.MSG_WELCOME -> {
                        int idx = msgIn.readInt();
                        int version = msgIn.readInt();
                        if (version != LanProtocol.VERSION) {
                            running = false;
                            break;
                        }
                        playerIndex = idx;
                        game.onLanWelcome(idx);
                    }
                    case LanProtocol.MSG_LOBBY -> {
                        int mapOrd = msgIn.readInt();
                        boolean mapRandom = mapOrd == LanProtocol.MAP_RANDOM;
                        MapType map = readMapByOrdinal(mapOrd);
                        boolean[] connected = new boolean[4];
                        boolean[] ready = new boolean[4];
                        BirdType[] birds = new BirdType[4];
                        boolean[] randomBirds = new boolean[4];
                        String[] skinKeys = new String[4];
                        for (int i = 0; i < 4; i++) {
                            connected[i] = msgIn.readBoolean();
                            ready[i] = msgIn.readBoolean();
                            int birdOrd = msgIn.readInt();
                            if (birdOrd == LanProtocol.BIRD_RANDOM) {
                                randomBirds[i] = true;
                            } else {
                                birds[i] = readBirdByOrdinal(birdOrd);
                            }
                            String skinKey = msgIn.readUTF();
                            skinKeys[i] = skinKey.isBlank() ? null : skinKey;
                        }
                        game.onLanLobbyUpdate(map, mapRandom, connected, birds, randomBirds, skinKeys, ready);
                    }
                    case LanProtocol.MSG_START -> {
                        int mapOrd = msgIn.readInt();
                        MapType map = readMapByOrdinal(mapOrd);
                        long seed = msgIn.readLong();
                        boolean[] connected = new boolean[4];
                        BirdType[] birds = new BirdType[4];
                        String[] skinKeys = new String[4];
                        for (int i = 0; i < 4; i++) {
                            connected[i] = msgIn.readBoolean();
                            int birdOrd = msgIn.readInt();
                            birds[i] = readBirdByOrdinal(birdOrd);
                            String skinKey = msgIn.readUTF();
                            skinKeys[i] = skinKey.isBlank() ? null : skinKey;
                        }
                        game.onLanStartMatch(map, seed, connected, birds, skinKeys);
                    }
                    case LanProtocol.MSG_STATE -> {
                        LanState state = LanState.read(msgIn);
                        game.onLanState(state);
                    }
                    case LanProtocol.MSG_END -> {
                        int winnerIndex = msgIn.readInt();
                        game.onLanMatchEnd(winnerIndex);
                    }
                    case LanProtocol.MSG_COUNTDOWN -> {
                        int seconds = msgIn.readInt();
                        game.onLanCountdown(seconds);
                    }
                    case LanProtocol.MSG_RESULTS_ACTION -> {
                        int action = msgIn.readInt();
                        int delayMs = msgIn.readInt();
                        game.onLanResultsAction(action, delayMs);
                    }
                    default -> {
                    }
                }
            }
        } catch (IOException ignored) {
        } finally {
            boolean notify = notifyOnDisconnect;
            disconnectInternal();
            if (notify) {
                game.onLanDisconnected();
            }
        }
    }

    private void writeLoop() {
        try {
            while (running) {
                byte[] payload = outbound.take();
                LanProtocol.writeFramed(out, payload);
            }
        } catch (InterruptedException ignored) {
            Thread.currentThread().interrupt();
        } catch (IOException ignored) {
        } finally {
            disconnectInternal();
        }
    }

    private MapType readMapByOrdinal(int ord) {
        MapType[] values = MapType.values();
        if (ord < 0 || ord >= values.length) {
            return MapType.FOREST;
        }
        return values[ord];
    }

    private BirdType readBirdByOrdinal(int ord) {
        BirdType[] values = BirdType.values();
        if (ord < 0 || ord >= values.length) return null;
        return values[ord];
    }

    public int getPlayerIndex() {
        return playerIndex;
    }

    public void setPlayerIndex(int playerIndex) {
        this.playerIndex = playerIndex;
    }
}
