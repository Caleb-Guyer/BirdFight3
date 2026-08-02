package com.example.birdgame3;

import com.example.birdgame3.BirdGame3.BirdType;
import com.example.birdgame3.BirdGame3.MapVariant;
import com.example.birdgame3.BirdGame3.MapType;
import com.example.birdgame3.BirdGame3.StageRandomPool;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;

final class LanPayloadRouter {
    record Welcome(int slot, int version) {
    }

    static int readClientHelloVersion(byte[] payload) throws IOException {
        DataInputStream msgIn = payloadInput(payload);
        if (msgIn.readByte() != LanProtocol.MSG_HELLO) {
            throw new IOException("The first client message was not a handshake.");
        }
        return msgIn.readInt();
    }

    static Welcome readServerWelcome(byte[] payload) throws IOException {
        DataInputStream msgIn = payloadInput(payload);
        byte type = msgIn.readByte();
        if (type == LanProtocol.MSG_REJECT) {
            String reason = msgIn.readUTF();
            throw new IOException(reason.isBlank() ? "The host rejected the connection." : reason);
        }
        if (type != LanProtocol.MSG_WELCOME) {
            throw new IOException("The host returned an invalid handshake.");
        }
        return new Welcome(msgIn.readInt(), msgIn.readInt());
    }

    static byte[] buildReject(String reason) throws IOException {
        String safeReason = reason == null || reason.isBlank() ? "Connection rejected." : reason;
        return LanProtocol.buildMessage(LanProtocol.MSG_REJECT, out -> out.writeUTF(safeReason));
    }

    static void handleClientPayload(BirdGame3 game, int slot, byte[] payload) throws IOException {
        DataInputStream msgIn = payloadInput(payload);
        byte type = msgIn.readByte();
        switch (type) {
            case LanProtocol.MSG_HELLO -> {
                int version = msgIn.readInt();
                if (version != LanProtocol.VERSION) {
                    throw new IOException("Client version mismatch.");
                }
            }
            case LanProtocol.MSG_SELECT -> {
                int ord = msgIn.readInt();
                String skinKey = msgIn.readUTF();
                boolean random = ord == LanProtocol.BIRD_RANDOM;
                BirdType typeBird = (!random && ord >= 0 && ord < BirdType.values().length)
                        ? BirdType.values()[ord]
                        : null;
                game.onLanClientSelected(slot, typeBird, random, skinKey.isBlank() ? null : skinKey);
            }
            case LanProtocol.MSG_MAP_VOTE -> {
                int ord = msgIn.readInt();
                int variantOrd = msgIn.readInt();
                int randomPoolOrd = msgIn.readInt();
                MapType map = (ord >= 0 && ord < MapType.values().length)
                        ? MapType.values()[ord]
                        : null;
                game.onLanClientMapVote(slot, map, readMapVariantByOrdinal(variantOrd),
                        readRandomPoolByOrdinal(randomPoolOrd));
            }
            case LanProtocol.MSG_READY -> {
                boolean ready = msgIn.readBoolean();
                game.onLanClientReady(slot, ready);
            }
            case LanProtocol.MSG_INPUT -> {
                int mask = msgIn.readInt() & LanProtocol.INPUT_MASK_ALL;
                game.onLanClientInput(slot, mask);
            }
            case LanProtocol.MSG_LOCKSTEP_INPUT -> {
                long tick = msgIn.readLong();
                int mask = msgIn.readInt() & LanProtocol.INPUT_MASK_ALL;
                game.onLanLockstepInput(slot, tick, mask);
            }
            default -> {
            }
        }
    }

    static void handleServerPayload(BirdGame3 game, byte[] payload) throws IOException {
        DataInputStream msgIn = payloadInput(payload);
        byte type = msgIn.readByte();
        switch (type) {
            case LanProtocol.MSG_WELCOME -> {
                int idx = msgIn.readInt();
                int version = msgIn.readInt();
                if (version != LanProtocol.VERSION) {
                    throw new IOException("Server version mismatch.");
                }
                game.onLanWelcome(idx);
            }
            case LanProtocol.MSG_LOBBY -> {
                int mapOrd = msgIn.readInt();
                boolean mapRandom = mapOrd == LanProtocol.MAP_RANDOM;
                MapType map = readMapByOrdinal(mapOrd);
                MapVariant variant = readMapVariantByOrdinal(msgIn.readInt());
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
                game.onLanLobbyUpdate(map, variant, mapRandom, connected, birds, randomBirds, skinKeys, ready);
            }
            case LanProtocol.MSG_START -> {
                int mapOrd = msgIn.readInt();
                MapType map = readMapByOrdinal(mapOrd);
                MapVariant variant = readMapVariantByOrdinal(msgIn.readInt());
                long seed = msgIn.readLong();
                int inputDelayTicks = LockstepSession.sanitizeInputDelay(msgIn.readInt());
                NetworkSimulationConfig simulationConfig = NetworkSimulationConfig.read(msgIn);
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
                game.onLanStartMatch(map, variant, seed, inputDelayTicks, simulationConfig, connected, birds, skinKeys);
            }
            case LanProtocol.MSG_STATE -> game.onLanState(LanState.read(msgIn));
            case LanProtocol.MSG_LOCKSTEP_BUNDLE -> {
                long tick = msgIn.readLong();
                int[] masks = new int[LockstepSession.MAX_SLOTS];
                for (int i = 0; i < masks.length; i++) {
                    masks[i] = msgIn.readInt();
                }
                game.onLanLockstepBundle(tick, masks);
            }
            case LanProtocol.MSG_LOCKSTEP_HASH -> game.onLanLockstepHash(msgIn.readLong(), msgIn.readLong());
            case LanProtocol.MSG_END -> game.onLanMatchEnd(msgIn.readInt());
            case LanProtocol.MSG_COUNTDOWN -> game.onLanCountdown(msgIn.readInt());
            case LanProtocol.MSG_RESULTS_ACTION -> game.onLanResultsAction(msgIn.readInt(), msgIn.readInt());
            default -> {
            }
        }
    }

    private static DataInputStream payloadInput(byte[] payload) throws IOException {
        if (payload == null || payload.length == 0) {
            throw new IOException("Empty network message.");
        }
        return new DataInputStream(new ByteArrayInputStream(payload));
    }

    private static MapVariant readMapVariantByOrdinal(int ordinal) {
        return ordinal >= 0 && ordinal < MapVariant.values().length
                ? MapVariant.values()[ordinal]
                : MapVariant.STANDARD;
    }

    private static StageRandomPool readRandomPoolByOrdinal(int ordinal) {
        return ordinal >= 0 && ordinal < StageRandomPool.values().length
                ? StageRandomPool.values()[ordinal]
                : StageRandomPool.NONE;
    }

    private static MapType readMapByOrdinal(int ord) {
        MapType[] values = MapType.values();
        if (ord < 0 || ord >= values.length) {
            return MapType.FOREST;
        }
        return values[ord];
    }

    private static BirdType readBirdByOrdinal(int ord) {
        BirdType[] values = BirdType.values();
        if (ord < 0 || ord >= values.length) return null;
        return values[ord];
    }

    private LanPayloadRouter() {
    }
}
