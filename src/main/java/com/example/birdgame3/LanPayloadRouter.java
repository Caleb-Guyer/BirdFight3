package com.example.birdgame3;

import com.example.birdgame3.BirdGame3.BirdType;
import com.example.birdgame3.BirdGame3.MapType;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;

final class LanPayloadRouter {
    static void handleClientPayload(BirdGame3 game, int slot, byte[] payload) throws IOException {
        DataInputStream msgIn = new DataInputStream(new ByteArrayInputStream(payload));
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
                boolean random = ord == LanProtocol.MAP_RANDOM;
                MapType map = (!random && ord >= 0 && ord < MapType.values().length)
                        ? MapType.values()[ord]
                        : null;
                game.onLanClientMapVote(slot, map, random);
            }
            case LanProtocol.MSG_READY -> {
                boolean ready = msgIn.readBoolean();
                game.onLanClientReady(slot, ready);
            }
            case LanProtocol.MSG_INPUT -> {
                int mask = msgIn.readInt();
                game.onLanClientInput(slot, mask);
            }
            default -> {
            }
        }
    }

    static void handleServerPayload(BirdGame3 game, byte[] payload) throws IOException {
        DataInputStream msgIn = new DataInputStream(new ByteArrayInputStream(payload));
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
            case LanProtocol.MSG_STATE -> game.onLanState(LanState.read(msgIn));
            case LanProtocol.MSG_END -> game.onLanMatchEnd(msgIn.readInt());
            case LanProtocol.MSG_COUNTDOWN -> game.onLanCountdown(msgIn.readInt());
            case LanProtocol.MSG_RESULTS_ACTION -> game.onLanResultsAction(msgIn.readInt(), msgIn.readInt());
            default -> {
            }
        }
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
