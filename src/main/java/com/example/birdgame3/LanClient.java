package com.example.birdgame3;

import com.example.birdgame3.BirdGame3.BirdType;
import com.example.birdgame3.BirdGame3.MapType;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

class LanClient implements NetworkSessionClient {
    private final BirdGame3 game;
    private final String host;
    private Socket socket;
    private DataInputStream in;
    private DataOutputStream out;
    private final BlockingQueue<byte[]> outbound = new LinkedBlockingQueue<>();
    private Thread readThread;
    private Thread writeThread;
    private volatile boolean running;
    private volatile boolean closed;
    private volatile boolean notifyOnDisconnect = true;
    private String lastError = "";

    LanClient(BirdGame3 game, String host) {
        this.game = game;
        this.host = host;
    }

    @Override
    public String getLastError() {
        return lastError;
    }

    @Override
    public boolean connect() {
        return connect(host);
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

    @Override
    public void disconnect() {
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

    @Override
    public void sendSelect(BirdType type, boolean random, String skinKey) {
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

    @Override
    public void sendMapVote(MapType map, boolean random) {
        if (!running) return;
        try {
            int ord = random ? LanProtocol.MAP_RANDOM : (map != null ? map.ordinal() : -1);
            byte[] msg = LanProtocol.buildMessage(LanProtocol.MSG_MAP_VOTE, out -> out.writeInt(ord));
            enqueueOutbound(msg);
        } catch (IOException ignored) {
        }
    }

    @Override
    public void sendReady(boolean ready) {
        if (!running) return;
        try {
            byte[] msg = LanProtocol.buildMessage(LanProtocol.MSG_READY, out -> out.writeBoolean(ready));
            enqueueOutbound(msg);
        } catch (IOException ignored) {
        }
    }

    @Override
    public void sendInputMask(int mask) {
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
                LanPayloadRouter.handleServerPayload(game, payload);
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
}
