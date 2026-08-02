package com.example.birdgame3;

import com.example.birdgame3.BirdGame3.BirdType;
import com.example.birdgame3.BirdGame3.MapType;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

class LanClient implements NetworkSessionClient {
    private static final int CONNECT_TIMEOUT_MS = 8_000;
    private static final int HANDSHAKE_TIMEOUT_MS = 10_000;
    private static final int MAX_QUEUED_MESSAGES = 512;

    private final BirdGame3 game;
    private final NetworkEndpoint endpoint;
    private Socket socket;
    private DataInputStream in;
    private DataOutputStream out;
    private final BlockingQueue<byte[]> outbound = new LinkedBlockingQueue<>(MAX_QUEUED_MESSAGES);
    private Thread readThread;
    private Thread writeThread;
    private volatile boolean running;
    private volatile boolean closed;
    private volatile boolean notifyOnDisconnect = true;
    private volatile String lastError = "";

    LanClient(BirdGame3 game, String host) {
        this(game, NetworkEndpoint.parse(host, LanProtocol.DEFAULT_PORT));
    }

    LanClient(BirdGame3 game, NetworkEndpoint endpoint) {
        this.game = game;
        this.endpoint = endpoint;
    }

    @Override
    public String getLastError() {
        return lastError;
    }

    @Override
    public boolean connect() {
        return connect(endpoint);
    }

    boolean connect(String host) {
        return connect(NetworkEndpoint.parse(host, LanProtocol.DEFAULT_PORT));
    }

    private boolean connect(NetworkEndpoint target) {
        if (running) return true;
        if (closed) {
            lastError = "Connection cancelled.";
            return false;
        }
        try {
            Socket newSocket = new Socket();
            socket = newSocket;
            newSocket.connect(new InetSocketAddress(target.host(), target.port()), CONNECT_TIMEOUT_MS);
            newSocket.setTcpNoDelay(true);
            newSocket.setKeepAlive(true);
            newSocket.setSoTimeout(HANDSHAKE_TIMEOUT_MS);
            DataInputStream newIn = new DataInputStream(newSocket.getInputStream());
            DataOutputStream newOut = new DataOutputStream(newSocket.getOutputStream());
            if (closed) {
                newSocket.close();
                lastError = "Connection cancelled.";
                return false;
            }
            byte[] hello = LanProtocol.buildMessage(LanProtocol.MSG_HELLO,
                    helloOut -> helloOut.writeInt(LanProtocol.VERSION));
            LanProtocol.writeFramed(newOut, hello);
            LanPayloadRouter.Welcome welcome = LanPayloadRouter.readServerWelcome(LanProtocol.readFramed(newIn));
            if (welcome.version() != LanProtocol.VERSION) {
                throw new IOException("Game versions do not match. Both players must run the same Bird Fight 3 version.");
            }
            newSocket.setSoTimeout(0);
            notifyOnDisconnect = true;
            in = newIn;
            out = newOut;
            running = true;
            readThread = new Thread(this::readLoop, "LanClient-Read");
            writeThread = new Thread(this::writeLoop, "LanClient-Write");
            readThread.setDaemon(true);
            writeThread.setDaemon(true);
            readThread.start();
            writeThread.start();
            game.onLanWelcome(welcome.slot());
            return true;
        } catch (IOException e) {
            lastError = friendlyError(e);
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
            lastError = "The network connection could not keep up.";
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

    @Override
    public void sendLockstepInput(long tick, int mask) {
        if (!running) return;
        try {
            byte[] msg = LanProtocol.buildMessage(LanProtocol.MSG_LOCKSTEP_INPUT, out -> {
                out.writeLong(tick);
                out.writeInt(mask);
            });
            enqueueOutbound(msg);
        } catch (IOException ignored) {
        }
    }

    private void readLoop() {
        try {
            while (running) {
                byte[] payload = LanProtocol.readFramed(in);
                LanPayloadRouter.handleServerPayload(game, payload);
            }
        } catch (IOException e) {
            if (!closed) {
                lastError = friendlyError(e);
            }
        } finally {
            boolean notify = notifyOnDisconnect;
            disconnectInternal();
            if (notify) {
                game.onLanDisconnected(lastError);
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

    private String friendlyError(IOException error) {
        String message = error == null ? null : error.getMessage();
        if (message == null || message.isBlank()) {
            return "The host closed the connection.";
        }
        if (message.toLowerCase().contains("timed out")) {
            return "Connection timed out. Check the address, firewall, and forwarded TCP port.";
        }
        if (message.toLowerCase().contains("refused")) {
            return "Connection refused. Check that the host is running and the TCP port is open.";
        }
        return message;
    }
}
