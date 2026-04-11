package com.example.birdgame3;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

public final class OnlineRelayServer {
    private static final String ROOM_CODE_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";

    private final int requestedPort;
    private final SecureRandom random = new SecureRandom();
    private final Map<String, Room> rooms = new ConcurrentHashMap<>();
    private ServerSocket serverSocket;
    private volatile boolean running;

    public OnlineRelayServer() {
        this(OnlineRelayProtocol.DEFAULT_PORT);
    }

    public OnlineRelayServer(int port) {
        this.requestedPort = Math.max(0, port);
    }

    public boolean start() {
        if (running) return true;
        try {
            serverSocket = new ServerSocket(requestedPort);
            serverSocket.setReuseAddress(true);
        } catch (IOException e) {
            return false;
        }
        running = true;
        Thread acceptThread = new Thread(this::acceptLoop, "OnlineRelay-Accept");
        acceptThread.setDaemon(true);
        acceptThread.start();
        return true;
    }

    public void stop() {
        running = false;
        try {
            if (serverSocket != null) {
                serverSocket.close();
            }
        } catch (IOException ignored) {
        }
        List<Room> snapshot = new ArrayList<>(rooms.values());
        for (Room room : snapshot) {
            room.closeRoom("Relay server stopped.");
        }
        rooms.clear();
    }

    public int getPort() {
        return serverSocket == null ? requestedPort : serverSocket.getLocalPort();
    }

    public static void main(String[] args) {
        int port = OnlineRelayProtocol.DEFAULT_PORT;
        if (args != null && args.length > 0) {
            try {
                port = Integer.parseInt(args[0]);
            } catch (NumberFormatException ignored) {
            }
        }
        OnlineRelayServer server = new OnlineRelayServer(port);
        if (!server.start()) {
            System.err.println("Failed to start Bird Fight 3 relay server on port " + port + '.');
            System.exit(1);
        }
        System.out.println("Bird Fight 3 relay server listening on port " + server.getPort() + '.');
        try {
            new java.util.concurrent.CountDownLatch(1).await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            server.stop();
        }
    }

    private void acceptLoop() {
        while (running) {
            try {
                Socket socket = serverSocket.accept();
                socket.setTcpNoDelay(true);
                ClientConnection connection = new ClientConnection(socket);
                connection.start();
            } catch (IOException ignored) {
                if (!running) {
                    return;
                }
            }
        }
    }

    private void handleMessage(ClientConnection connection, byte[] payload) throws IOException {
        DataInputStream msgIn = new DataInputStream(new ByteArrayInputStream(payload));
        byte type = msgIn.readByte();
        switch (type) {
            case OnlineRelayProtocol.MSG_HELLO -> handleHello(connection, msgIn);
            case OnlineRelayProtocol.MSG_LIST_ROOMS -> handleListRooms(connection);
            case OnlineRelayProtocol.MSG_CREATE_ROOM -> handleCreateRoom(connection, msgIn);
            case OnlineRelayProtocol.MSG_JOIN_ROOM -> handleJoinRoom(connection, msgIn);
            case OnlineRelayProtocol.MSG_HOST_BROADCAST -> handleHostBroadcast(connection, msgIn);
            case OnlineRelayProtocol.MSG_GUEST_TO_HOST -> handleGuestToHost(connection, msgIn);
            default -> sendError(connection, "Unknown relay command.");
        }
    }

    private void handleHello(ClientConnection connection, DataInputStream msgIn) throws IOException {
        int relayVersion = msgIn.readInt();
        int lanVersion = msgIn.readInt();
        if (relayVersion != OnlineRelayProtocol.VERSION) {
            sendError(connection, "Relay protocol mismatch.");
            return;
        }
        if (lanVersion != LanProtocol.VERSION) {
            sendError(connection, "Game version mismatch.");
            return;
        }
        connection.handshakeComplete = true;
    }

    private void handleListRooms(ClientConnection connection) throws IOException {
        if (!connection.handshakeComplete) {
            sendError(connection, "Handshake required before listing rooms.");
            return;
        }
        List<OnlineRoomInfo> roomInfos = rooms.values().stream()
                .filter(Room::listed)
                .map(Room::toInfo)
                .sorted(Comparator.comparing(OnlineRoomInfo::roomName, String.CASE_INSENSITIVE_ORDER)
                        .thenComparing(OnlineRoomInfo::code))
                .toList();
        connection.enqueue(OnlineRelayProtocol.buildMessage(OnlineRelayProtocol.MSG_ROOM_LIST, out -> {
            out.writeInt(roomInfos.size());
            for (OnlineRoomInfo info : roomInfos) {
                out.writeUTF(info.code());
                out.writeUTF(info.roomName());
                out.writeUTF(info.hostName());
                out.writeInt(info.playerCount());
                out.writeInt(info.maxPlayers());
            }
        }));
    }

    private void handleCreateRoom(ClientConnection connection, DataInputStream msgIn) throws IOException {
        if (!connection.handshakeComplete) {
            sendError(connection, "Handshake required before creating a room.");
            return;
        }
        if (connection.room != null) {
            sendError(connection, "Connection is already assigned to a room.");
            return;
        }
        if (rooms.size() >= OnlineRelayProtocol.MAX_ROOMS) {
            sendError(connection, "Relay room capacity reached.");
            return;
        }

        String roomName = OnlineRelayProtocol.sanitizeDisplayText(msgIn.readUTF(), "Open Room", 32);
        String hostName = OnlineRelayProtocol.sanitizeDisplayText(msgIn.readUTF(), "Host", 24);
        boolean listed = msgIn.readBoolean();
        String code = allocateRoomCode();
        Room room = new Room(code, roomName, hostName, listed, connection);
        connection.room = room;
        connection.host = true;
        connection.slot = 0;
        rooms.put(code, room);
        connection.enqueue(OnlineRelayProtocol.buildMessage(OnlineRelayProtocol.MSG_ROOM_CREATED, out -> out.writeUTF(code)));
    }

    private void handleJoinRoom(ClientConnection connection, DataInputStream msgIn) throws IOException {
        if (!connection.handshakeComplete) {
            sendError(connection, "Handshake required before joining a room.");
            return;
        }
        if (connection.room != null) {
            sendError(connection, "Connection is already assigned to a room.");
            return;
        }

        String code = OnlineRelayProtocol.sanitizeRoomCode(msgIn.readUTF());
        Room room = rooms.get(code);
        if (room == null || room.closed) {
            sendError(connection, "Room not found.");
            return;
        }

        int slot = room.addGuest(connection);
        if (slot < 0) {
            sendError(connection, "Room is full.");
            return;
        }

        connection.room = room;
        connection.host = false;
        connection.slot = slot;
        connection.enqueue(OnlineRelayProtocol.buildMessage(OnlineRelayProtocol.MSG_JOINED_ROOM, out -> {
            out.writeUTF(room.code);
            out.writeUTF(room.roomName);
            out.writeUTF(room.hostName);
        }));
        room.sendWelcome(connection, slot);
        room.notifyHostPlayerJoined(slot);
    }

    private void handleHostBroadcast(ClientConnection connection, DataInputStream msgIn) throws IOException {
        if (!connection.handshakeComplete || !connection.host || connection.room == null) {
            sendError(connection, "Only a room host may broadcast.");
            return;
        }
        connection.room.broadcastToGuests(OnlineRelayProtocol.readByteArray(msgIn));
    }

    private void handleGuestToHost(ClientConnection connection, DataInputStream msgIn) throws IOException {
        if (!connection.handshakeComplete || connection.host || connection.room == null || connection.slot <= 0) {
            sendError(connection, "Only a room guest may send host payloads.");
            return;
        }
        connection.room.forwardToHost(connection.slot, OnlineRelayProtocol.readByteArray(msgIn));
    }

    private void sendError(ClientConnection connection, String message) throws IOException {
        connection.sendAndClose(OnlineRelayProtocol.buildMessage(OnlineRelayProtocol.MSG_ERROR, out -> out.writeUTF(message)));
    }

    private void onConnectionClosed(ClientConnection connection) {
        Room room = connection.room;
        if (room == null) {
            return;
        }
        if (connection.host) {
            room.closeRoom("Host ended the room.");
            rooms.remove(room.code, room);
            return;
        }
        room.removeGuest(connection.slot, connection);
    }

    private String allocateRoomCode() {
        for (int attempt = 0; attempt < 10_000; attempt++) {
            StringBuilder code = new StringBuilder(OnlineRelayProtocol.ROOM_CODE_LENGTH);
            for (int i = 0; i < OnlineRelayProtocol.ROOM_CODE_LENGTH; i++) {
                code.append(ROOM_CODE_ALPHABET.charAt(random.nextInt(ROOM_CODE_ALPHABET.length())));
            }
            String candidate = code.toString();
            if (!rooms.containsKey(candidate)) {
                return candidate;
            }
        }
        throw new IllegalStateException("Could not allocate a unique room code.");
    }

    private final class Room {
        private final String code;
        private final String roomName;
        private final String hostName;
        private final boolean listed;
        private final ClientConnection hostConnection;
        private final ClientConnection[] guests = new ClientConnection[4];
        private volatile boolean closed;

        private Room(String code, String roomName, String hostName, boolean listed, ClientConnection hostConnection) {
            this.code = code;
            this.roomName = roomName;
            this.hostName = hostName;
            this.listed = listed;
            this.hostConnection = hostConnection;
            this.guests[0] = hostConnection;
        }

        private synchronized int addGuest(ClientConnection connection) {
            if (closed) return -1;
            for (int i = 1; i < guests.length; i++) {
                if (guests[i] == null) {
                    guests[i] = connection;
                    return i;
                }
            }
            return -1;
        }

        private synchronized void removeGuest(int slot, ClientConnection connection) {
            if (closed) return;
            if (slot > 0 && slot < guests.length && guests[slot] == connection) {
                guests[slot] = null;
                notifyHostPlayerLeft(slot);
            }
        }

        private synchronized void broadcastToGuests(byte[] lanPayload) throws IOException {
            if (closed) return;
            byte[] relayPayload = OnlineRelayProtocol.buildMessage(OnlineRelayProtocol.MSG_HOST_PAYLOAD,
                    out -> OnlineRelayProtocol.writeByteArray(out, lanPayload));
            for (int i = 1; i < guests.length; i++) {
                ClientConnection guest = guests[i];
                if (guest != null) {
                    guest.enqueue(relayPayload);
                }
            }
        }

        private synchronized void forwardToHost(int slot, byte[] lanPayload) throws IOException {
            if (closed) return;
            hostConnection.enqueue(OnlineRelayProtocol.buildMessage(OnlineRelayProtocol.MSG_GUEST_PAYLOAD, out -> {
                out.writeInt(slot);
                OnlineRelayProtocol.writeByteArray(out, lanPayload);
            }));
        }

        private void sendWelcome(ClientConnection guest, int slot) throws IOException {
            byte[] welcome = LanProtocol.buildMessage(LanProtocol.MSG_WELCOME, out -> {
                out.writeInt(slot);
                out.writeInt(LanProtocol.VERSION);
            });
            guest.enqueue(OnlineRelayProtocol.buildMessage(OnlineRelayProtocol.MSG_HOST_PAYLOAD,
                    out -> OnlineRelayProtocol.writeByteArray(out, welcome)));
        }

        private void notifyHostPlayerJoined(int slot) throws IOException {
            hostConnection.enqueue(OnlineRelayProtocol.buildMessage(OnlineRelayProtocol.MSG_PLAYER_JOINED, out -> out.writeInt(slot)));
        }

        private void notifyHostPlayerLeft(int slot) {
            try {
                hostConnection.enqueue(OnlineRelayProtocol.buildMessage(OnlineRelayProtocol.MSG_PLAYER_LEFT, out -> out.writeInt(slot)));
            } catch (IOException ignored) {
            }
        }

        private synchronized void closeRoom(String reason) {
            if (closed) return;
            closed = true;
            rooms.remove(code, this);
            for (int i = 1; i < guests.length; i++) {
                ClientConnection guest = guests[i];
                if (guest == null) continue;
                try {
                    guest.sendAndClose(OnlineRelayProtocol.buildMessage(OnlineRelayProtocol.MSG_ROOM_CLOSED, out -> out.writeUTF(reason)));
                } catch (IOException ignored) {
                }
                guests[i] = null;
            }
            hostConnection.close();
        }

        private synchronized OnlineRoomInfo toInfo() {
            int count = 0;
            for (ClientConnection guest : guests) {
                if (guest != null) {
                    count++;
                }
            }
            return new OnlineRoomInfo(code, roomName, hostName, count, guests.length);
        }

        private boolean listed() {
            return listed && !closed;
        }
    }

    private final class ClientConnection {
        private final Socket socket;
        private final DataInputStream in;
        private final DataOutputStream out;
        private final BlockingQueue<byte[]> outbound = new LinkedBlockingQueue<>();
        private final AtomicBoolean cleanedUp = new AtomicBoolean(false);
        private volatile boolean active = true;
        private volatile boolean handshakeComplete;
        private volatile boolean host;
        private volatile int slot = -1;
        private volatile Room room;
        private Thread readThread;
        private Thread writeThread;

        private ClientConnection(Socket socket) throws IOException {
            this.socket = socket;
            this.in = new DataInputStream(socket.getInputStream());
            this.out = new DataOutputStream(socket.getOutputStream());
        }

        private void start() {
            readThread = new Thread(this::readLoop, "OnlineRelay-Read-" + socket.getPort());
            writeThread = new Thread(this::writeLoop, "OnlineRelay-Write-" + socket.getPort());
            readThread.setDaemon(true);
            writeThread.setDaemon(true);
            readThread.start();
            writeThread.start();
        }

        private void enqueue(byte[] payload) {
            if (!active || payload == null) return;
            if (!outbound.offer(payload)) {
                close();
            }
        }

        private void close() {
            active = false;
            if (readThread != null) readThread.interrupt();
            if (writeThread != null) writeThread.interrupt();
            try {
                socket.close();
            } catch (IOException ignored) {
            }
        }

        private void sendAndClose(byte[] payload) throws IOException {
            if (payload != null) {
                synchronized (out) {
                    OnlineRelayProtocol.writeFramed(out, payload);
                }
            }
            close();
        }

        private void readLoop() {
            try {
                while (active) {
                    handleMessage(this, OnlineRelayProtocol.readFramed(in));
                }
            } catch (IOException ignored) {
            } finally {
                close();
                cleanup();
            }
        }

        private void writeLoop() {
            try {
                while (active) {
                    byte[] payload = outbound.take();
                    synchronized (out) {
                        OnlineRelayProtocol.writeFramed(out, payload);
                    }
                }
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
            } catch (IOException ignored) {
            } finally {
                close();
                cleanup();
            }
        }

        private void cleanup() {
            if (cleanedUp.compareAndSet(false, true)) {
                onConnectionClosed(this);
            }
        }
    }
}
