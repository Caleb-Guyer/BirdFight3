package com.example.birdgame3;

import com.example.birdgame3.BirdGame3.BirdType;
import com.example.birdgame3.BirdGame3.MapVariant;
import com.example.birdgame3.BirdGame3.MapType;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.LinkedBlockingQueue;

class LanHostServer implements NetworkSessionHost {
    private static final int HANDSHAKE_TIMEOUT_MS = 10_000;
    private static final int MAX_QUEUED_MESSAGES = 512;

    private final BirdGame3 game;
    private final int port;
    private final List<ClientHandler> clients = new CopyOnWriteArrayList<>();
    private final List<CompanionHandler> companionClients = new CopyOnWriteArrayList<>();
    private final boolean[] slotTaken = new boolean[4];
    private ServerSocket serverSocket;
    private ServerSocket companionServerSocket;
    private volatile boolean companionFeedEnabled;
    private volatile boolean companionFeedAvailable;
    private volatile byte[] lastCompanionSnapshot;
    private volatile boolean running;

    LanHostServer(BirdGame3 game) {
        this(game, LanProtocol.DEFAULT_PORT, true);
    }

    LanHostServer(BirdGame3 game, int port, boolean companionFeedEnabled) {
        this.game = game;
        this.port = NetworkEndpoint.parsePort(Integer.toString(port));
        this.companionFeedEnabled = companionFeedEnabled;
        slotTaken[0] = true;
    }

    @Override
    public boolean start() {
        if (running) return true;
        try {
            ServerSocket socket = new ServerSocket();
            socket.setReuseAddress(true);
            socket.bind(new InetSocketAddress(port));
            serverSocket = socket;
        } catch (IOException e) {
            return false;
        }
        running = true;
        Thread acceptThread = new Thread(this::acceptLoop, "LanHost-Accept");
        acceptThread.setDaemon(true);
        acceptThread.start();
        startCompanionServer();
        return true;
    }

    @Override
    public void stop() {
        running = false;
        try {
            if (serverSocket != null) serverSocket.close();
        } catch (IOException ignored) {
        }
        stopCompanionServer();
        for (ClientHandler client : clients) {
            client.close();
        }
        clients.clear();
        for (int i = 1; i < slotTaken.length; i++) {
            slotTaken[i] = false;
        }
    }

    @Override
    public boolean hasClients() {
        return !clients.isEmpty();
    }

    @Override
    public void setCompanionFeedEnabled(boolean enabled) {
        companionFeedEnabled = enabled;
        if (enabled) {
            startCompanionServer();
        } else {
            stopCompanionServer();
        }
    }

    @Override
    public boolean isCompanionFeedEnabled() {
        return companionFeedEnabled;
    }

    @Override
    public boolean isCompanionFeedAvailable() {
        return companionFeedEnabled && companionFeedAvailable;
    }

    @Override
    public int companionViewerCount() {
        return companionClients.size();
    }

    @Override
    public void broadcastLobby(MapType map, MapVariant variant, boolean mapRandom, boolean[] connected, BirdType[] birds, boolean[] randomBirds, String[] skinKeys, boolean[] ready) {
        try {
            byte[] msg = LanProtocol.buildMessage(LanProtocol.MSG_LOBBY, out -> {
                out.writeInt(mapRandom ? LanProtocol.MAP_RANDOM : map.ordinal());
                out.writeInt((variant == null ? MapVariant.STANDARD : variant).ordinal());
                for (int i = 0; i < 4; i++) {
                    out.writeBoolean(connected[i]);
                    out.writeBoolean(ready != null && ready[i]);
                    if (randomBirds != null && randomBirds[i]) {
                        out.writeInt(LanProtocol.BIRD_RANDOM);
                    } else {
                        out.writeInt(birds[i] != null ? birds[i].ordinal() : -1);
                    }
                    String skinKey = skinKeys != null ? skinKeys[i] : null;
                    out.writeUTF(skinKey == null ? "" : skinKey);
                }
            });
            sendToAll(msg);
        } catch (IOException ignored) {
        }
    }

    @Override
    public void broadcastStart(MapType map, MapVariant variant, long seed, int inputDelayTicks, NetworkSimulationConfig simulationConfig,
                               boolean[] connected, BirdType[] birds, String[] skinKeys) {
        try {
            byte[] msg = LanProtocol.buildMessage(LanProtocol.MSG_START, out -> {
                out.writeInt(map.ordinal());
                out.writeInt((variant == null ? MapVariant.STANDARD : variant).ordinal());
                out.writeLong(seed);
                out.writeInt(LockstepSession.sanitizeInputDelay(inputDelayTicks));
                (simulationConfig == null ? NetworkSimulationConfig.capture() : simulationConfig).write(out);
                for (int i = 0; i < 4; i++) {
                    out.writeBoolean(connected[i]);
                    out.writeInt(birds[i] != null ? birds[i].ordinal() : -1);
                    String skinKey = skinKeys != null ? skinKeys[i] : null;
                    out.writeUTF(skinKey == null ? "" : skinKey);
                }
            });
            sendToAll(msg);
        } catch (IOException ignored) {
        }
    }

    @Override
    public void broadcastState(LanState state) {
        if (!running) return;
        try {
            byte[] msg = LanProtocol.buildMessage(LanProtocol.MSG_STATE, state::write);
            sendToAll(msg);
        } catch (IOException ignored) {
        }
    }

    @Override
    public void broadcastLockstepBundle(long tick, int[] masks) {
        if (!running) return;
        try {
            byte[] msg = LanProtocol.buildMessage(LanProtocol.MSG_LOCKSTEP_BUNDLE, out -> {
                out.writeLong(tick);
                for (int i = 0; i < LockstepSession.MAX_SLOTS; i++) {
                    out.writeInt(i < masks.length ? masks[i] : 0);
                }
            });
            sendToAll(msg);
        } catch (IOException ignored) {
        }
    }

    @Override
    public void broadcastLockstepHash(long tick, long hash) {
        if (!running) return;
        try {
            byte[] msg = LanProtocol.buildMessage(LanProtocol.MSG_LOCKSTEP_HASH, out -> {
                out.writeLong(tick);
                out.writeLong(hash);
            });
            sendToAll(msg);
        } catch (IOException ignored) {
        }
    }

    @Override
    public void broadcastMatchEnd(int winnerIndex) {
        try {
            byte[] msg = LanProtocol.buildMessage(LanProtocol.MSG_END, out -> out.writeInt(winnerIndex));
            sendToAll(msg);
        } catch (IOException ignored) {
        }
    }

    @Override
    public void broadcastCountdown(int seconds) {
        try {
            byte[] msg = LanProtocol.buildMessage(LanProtocol.MSG_COUNTDOWN, out -> out.writeInt(seconds));
            sendToAll(msg);
        } catch (IOException ignored) {
        }
    }

    @Override
    public void broadcastResultsAction(int action, int delayMs) {
        try {
            byte[] msg = LanProtocol.buildMessage(LanProtocol.MSG_RESULTS_ACTION, out -> {
                out.writeInt(action);
                out.writeInt(delayMs);
            });
            sendToAll(msg);
        } catch (IOException ignored) {
        }
    }

    @Override
    public void broadcastCompanionSnapshot(CompanionSnapshot snapshot) {
        if (!running || !companionFeedEnabled || snapshot == null) return;
        try {
            byte[] msg = LanProtocol.buildMessage(LanProtocol.MSG_COMPANION_SNAPSHOT, snapshot::write);
            lastCompanionSnapshot = msg;
            sendToCompanions(msg);
        } catch (IOException ignored) {
        }
    }

    private void sendToAll(byte[] payload) {
        for (ClientHandler client : clients) {
            client.enqueue(payload);
        }
    }

    private void sendToCompanions(byte[] payload) {
        for (CompanionHandler client : companionClients) {
            client.enqueue(payload);
        }
    }

    private synchronized void startCompanionServer() {
        if (!running || !companionFeedEnabled) return;
        if (companionServerSocket != null && !companionServerSocket.isClosed()) {
            companionFeedAvailable = true;
            return;
        }
        try {
            ServerSocket socket = new ServerSocket();
            socket.setReuseAddress(true);
            socket.bind(new InetSocketAddress(LanProtocol.COMPANION_PORT));
            companionServerSocket = socket;
            companionFeedAvailable = true;
            Thread acceptThread = new Thread(this::companionAcceptLoop, "LanCompanion-Accept");
            acceptThread.setDaemon(true);
            acceptThread.start();
        } catch (IOException ignored) {
            companionFeedAvailable = false;
            try {
                if (companionServerSocket != null) companionServerSocket.close();
            } catch (IOException ignoredClose) {
            }
            companionServerSocket = null;
        }
    }

    private synchronized void stopCompanionServer() {
        companionFeedAvailable = false;
        try {
            if (companionServerSocket != null) companionServerSocket.close();
        } catch (IOException ignored) {
        }
        companionServerSocket = null;
        for (CompanionHandler client : companionClients) {
            client.close();
        }
        companionClients.clear();
    }

    private void acceptLoop() {
        while (running) {
            try {
                Socket socket = serverSocket.accept();
                socket.setTcpNoDelay(true);
                socket.setKeepAlive(true);
                int slot = allocateSlot();
                if (slot < 0) {
                    rejectAndClose(socket, "This lobby is full.");
                    continue;
                }
                ClientHandler handler = new ClientHandler(socket, slot);
                clients.add(handler);
                handler.start();
            } catch (IOException e) {
                if (running) {
                    game.onLanServerError(e);
                }
            }
        }
    }

    private synchronized int allocateSlot() {
        for (int i = 1; i < slotTaken.length; i++) {
            if (!slotTaken[i]) {
                slotTaken[i] = true;
                return i;
            }
        }
        return -1;
    }

    private void rejectAndClose(Socket socket, String reason) {
        try (socket; DataOutputStream rejectOut = new DataOutputStream(socket.getOutputStream())) {
            LanProtocol.writeFramed(rejectOut, LanPayloadRouter.buildReject(reason));
        } catch (IOException ignored) {
        }
    }

    private void companionAcceptLoop() {
        while (running && companionFeedEnabled) {
            try {
                ServerSocket socket = companionServerSocket;
                if (socket == null || socket.isClosed()) return;
                Socket companionSocket = socket.accept();
                companionSocket.setTcpNoDelay(true);
                companionSocket.setKeepAlive(true);
                CompanionHandler handler = new CompanionHandler(companionSocket);
                companionClients.add(handler);
                handler.start();
                game.onLanCompanionViewerChanged();
            } catch (IOException ignored) {
                if (running && companionFeedEnabled) {
                    companionFeedAvailable = false;
                }
                return;
            }
        }
    }

    private synchronized void releaseSlot(int slot) {
        if (slot > 0 && slot < slotTaken.length) {
            slotTaken[slot] = false;
        }
    }

    private void handleDisconnect(ClientHandler handler) {
        clients.remove(handler);
        releaseSlot(handler.slot);
        if (handler.handshakeComplete) {
            game.onLanClientDisconnected(handler.slot);
        }
    }

    private void handleCompanionDisconnect(CompanionHandler handler) {
        companionClients.remove(handler);
        game.onLanCompanionViewerChanged();
    }

    private final class ClientHandler {
        private final Socket socket;
        private final DataInputStream in;
        private final DataOutputStream out;
        private final BlockingQueue<byte[]> outbound = new LinkedBlockingQueue<>(MAX_QUEUED_MESSAGES);
        private final int slot;
        private volatile boolean active = true;
        private volatile boolean handshakeComplete;
        private Thread readThread;
        private Thread writeThread;

        ClientHandler(Socket socket, int slot) throws IOException {
            this.socket = socket;
            this.slot = slot;
            this.in = new DataInputStream(socket.getInputStream());
            this.out = new DataOutputStream(socket.getOutputStream());
            socket.setSoTimeout(HANDSHAKE_TIMEOUT_MS);
        }

        void start() {
            readThread = new Thread(this::readLoop, "LanHost-Read-" + slot);
            writeThread = new Thread(this::writeLoop, "LanHost-Write-" + slot);
            readThread.setDaemon(true);
            writeThread.setDaemon(true);
            readThread.start();
            writeThread.start();
        }

        void enqueue(byte[] payload) {
            if (!active || payload == null) return;
            if (!outbound.offer(payload)) {
                close();
            }
        }

        void close() {
            active = false;
            if (readThread != null) readThread.interrupt();
            if (writeThread != null) writeThread.interrupt();
            try {
                socket.close();
            } catch (IOException ignored) {
            }
        }

        private void sendWelcome() {
            try {
                byte[] msg = LanProtocol.buildMessage(LanProtocol.MSG_WELCOME, out -> {
                    out.writeInt(slot);
                    out.writeInt(LanProtocol.VERSION);
                });
                enqueue(msg);
            } catch (IOException ignored) {
            }
        }

        private void readLoop() {
            try {
                int clientVersion = LanPayloadRouter.readClientHelloVersion(LanProtocol.readFramed(in));
                if (clientVersion != LanProtocol.VERSION) {
                    reject("Game versions do not match. Both players must run the same Bird Fight 3 version.");
                    return;
                }
                socket.setSoTimeout(0);
                sendWelcome();
                handshakeComplete = true;
                game.onLanClientConnected(slot);
                while (active) {
                    byte[] payload = LanProtocol.readFramed(in);
                    LanPayloadRouter.handleClientPayload(game, slot, payload);
                }
            } catch (IOException ignored) {
            } finally {
                close();
                handleDisconnect(this);
            }
        }

        private void reject(String reason) {
            try {
                LanProtocol.writeFramed(out, LanPayloadRouter.buildReject(reason));
            } catch (IOException ignored) {
            }
        }

        private void writeLoop() {
            try {
                while (active) {
                    byte[] payload = outbound.take();
                    LanProtocol.writeFramed(out, payload);
                }
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
            } catch (IOException ignored) {
            } finally {
                close();
            }
        }
    }

    private final class CompanionHandler {
        private final Socket socket;
        private final DataOutputStream out;
        private final BlockingQueue<byte[]> outbound = new LinkedBlockingQueue<>(3);
        private volatile boolean active = true;
        private Thread writeThread;

        CompanionHandler(Socket socket) throws IOException {
            this.socket = socket;
            this.out = new DataOutputStream(socket.getOutputStream());
        }

        void start() {
            writeThread = new Thread(this::writeLoop, "LanCompanion-Write");
            writeThread.setDaemon(true);
            writeThread.start();
            byte[] latest = lastCompanionSnapshot;
            if (latest != null) {
                enqueue(latest);
            }
        }

        void enqueue(byte[] payload) {
            if (!active || payload == null) return;
            if (!outbound.offer(payload)) {
                outbound.poll();
                if (!outbound.offer(payload)) {
                    close();
                }
            }
        }

        void close() {
            active = false;
            if (writeThread != null) writeThread.interrupt();
            try {
                socket.close();
            } catch (IOException ignored) {
            }
        }

        private void writeLoop() {
            try {
                while (active) {
                    byte[] payload = outbound.take();
                    LanProtocol.writeFramed(out, payload);
                }
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
            } catch (IOException ignored) {
            } finally {
                close();
                handleCompanionDisconnect(this);
            }
        }
    }
}
