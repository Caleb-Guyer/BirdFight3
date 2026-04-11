package com.example.birdgame3;

import com.example.birdgame3.BirdGame3.BirdType;
import com.example.birdgame3.BirdGame3.MapType;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.LinkedBlockingQueue;

class LanHostServer implements NetworkSessionHost {
    private final BirdGame3 game;
    private final List<ClientHandler> clients = new CopyOnWriteArrayList<>();
    private final boolean[] slotTaken = new boolean[4];
    private ServerSocket serverSocket;
    private volatile boolean running;

    LanHostServer(BirdGame3 game) {
        this.game = game;
        slotTaken[0] = true;
    }

    @Override
    public boolean start() {
        if (running) return true;
        try {
            serverSocket = new ServerSocket(LanProtocol.DEFAULT_PORT);
            serverSocket.setReuseAddress(true);
        } catch (IOException e) {
            return false;
        }
        running = true;
        Thread acceptThread = new Thread(this::acceptLoop, "LanHost-Accept");
        acceptThread.setDaemon(true);
        acceptThread.start();
        return true;
    }

    @Override
    public void stop() {
        running = false;
        try {
            if (serverSocket != null) serverSocket.close();
        } catch (IOException ignored) {
        }
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
    public void broadcastLobby(MapType map, boolean mapRandom, boolean[] connected, BirdType[] birds, boolean[] randomBirds, String[] skinKeys, boolean[] ready) {
        try {
            byte[] msg = LanProtocol.buildMessage(LanProtocol.MSG_LOBBY, out -> {
                out.writeInt(mapRandom ? LanProtocol.MAP_RANDOM : map.ordinal());
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
    public void broadcastStart(MapType map, long seed, boolean[] connected, BirdType[] birds, String[] skinKeys) {
        try {
            byte[] msg = LanProtocol.buildMessage(LanProtocol.MSG_START, out -> {
                out.writeInt(map.ordinal());
                out.writeLong(seed);
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

    private void sendToAll(byte[] payload) {
        for (ClientHandler client : clients) {
            client.enqueue(payload);
        }
    }

    private void acceptLoop() {
        while (running) {
            try {
                Socket socket = serverSocket.accept();
                socket.setTcpNoDelay(true);
                int slot = allocateSlot();
                if (slot < 0) {
                    socket.close();
                    continue;
                }
                ClientHandler handler = new ClientHandler(socket, slot);
                clients.add(handler);
                handler.start();
                game.onLanClientConnected(slot);
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

    private synchronized void releaseSlot(int slot) {
        if (slot > 0 && slot < slotTaken.length) {
            slotTaken[slot] = false;
        }
    }

    private void handleDisconnect(ClientHandler handler) {
        clients.remove(handler);
        releaseSlot(handler.slot);
        game.onLanClientDisconnected(handler.slot);
    }

    private final class ClientHandler {
        private final Socket socket;
        private final DataInputStream in;
        private final DataOutputStream out;
        private final BlockingQueue<byte[]> outbound = new LinkedBlockingQueue<>();
        private final int slot;
        private volatile boolean active = true;
        private Thread readThread;
        private Thread writeThread;

        ClientHandler(Socket socket, int slot) throws IOException {
            this.socket = socket;
            this.slot = slot;
            this.in = new DataInputStream(socket.getInputStream());
            this.out = new DataOutputStream(socket.getOutputStream());
        }

        void start() {
            readThread = new Thread(this::readLoop, "LanHost-Read-" + slot);
            writeThread = new Thread(this::writeLoop, "LanHost-Write-" + slot);
            readThread.setDaemon(true);
            writeThread.setDaemon(true);
            readThread.start();
            writeThread.start();
            sendWelcome();
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
}
