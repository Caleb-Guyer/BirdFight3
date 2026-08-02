package com.example.birdgame3;

import com.example.birdgame3.BirdGame3.BirdType;
import com.example.birdgame3.BirdGame3.MapType;

import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

interface NetworkSessionHost {
    int COMPANION_PHASE_LOBBY = 0;
    int COMPANION_PHASE_COUNTDOWN = 1;
    int COMPANION_PHASE_MATCH = 2;
    int COMPANION_PHASE_RESULTS = 3;

    boolean start();

    void stop();

    boolean hasClients();

    void setCompanionFeedEnabled(boolean enabled);

    boolean isCompanionFeedEnabled();

    boolean isCompanionFeedAvailable();

    int companionViewerCount();

    void broadcastLobby(MapType map, boolean mapRandom, boolean[] connected, BirdType[] birds,
                        boolean[] randomBirds, String[] skinKeys, boolean[] ready);

    void broadcastStart(MapType map, long seed, int inputDelayTicks, NetworkSimulationConfig simulationConfig,
                        boolean[] connected, BirdType[] birds, String[] skinKeys);

    void broadcastState(LanState state);

    void broadcastLockstepBundle(long tick, int[] masks);

    void broadcastLockstepHash(long tick, long hash);

    void broadcastMatchEnd(int winnerIndex);

    void broadcastCountdown(int seconds);

    void broadcastResultsAction(int action, int delayMs);

    void broadcastCompanionSnapshot(CompanionSnapshot snapshot);

    final class CompanionSnapshot {
        int phase = COMPANION_PHASE_LOBBY;
        String status = "";
        String mapName = "";
        int matchTimerFrames = 0;
        boolean matchEnded = false;
        boolean suddenDeathActive = false;
        boolean suddenDeathSmashStyle = false;
        boolean smashRules = true;
        int countdownSeconds = 0;
        int winnerIndex = -1;
        long generatedAtMillis = System.currentTimeMillis();
        final Player[] players = new Player[]{new Player(), new Player(), new Player(), new Player()};
        List<String> killFeed = new ArrayList<>();

        void write(DataOutputStream out) throws IOException {
            out.writeInt(LanProtocol.COMPANION_VERSION);
            out.writeLong(generatedAtMillis);
            out.writeInt(phase);
            out.writeUTF(nullToEmpty(status));
            out.writeUTF(nullToEmpty(mapName));
            out.writeInt(matchTimerFrames);
            out.writeBoolean(matchEnded);
            out.writeBoolean(suddenDeathActive);
            out.writeBoolean(suddenDeathSmashStyle);
            out.writeBoolean(smashRules);
            out.writeInt(countdownSeconds);
            out.writeInt(winnerIndex);
            out.writeInt(players.length);
            for (Player player : players) {
                Player p = player == null ? new Player() : player;
                out.writeBoolean(p.connected);
                out.writeBoolean(p.ready);
                out.writeUTF(nullToEmpty(p.birdName));
                out.writeUTF(nullToEmpty(p.colorHex));
                out.writeInt(p.score);
                out.writeDouble(p.health);
                out.writeDouble(p.damage);
                out.writeBoolean(p.alive);
            }
            int feedCount = killFeed == null ? 0 : Math.min(killFeed.size(), 6);
            out.writeInt(feedCount);
            for (int i = 0; i < feedCount; i++) {
                out.writeUTF(nullToEmpty(killFeed.get(i)));
            }
        }

        private static String nullToEmpty(String value) {
            return value == null ? "" : value;
        }

        static final class Player {
            boolean connected = false;
            boolean ready = false;
            String birdName = "";
            String colorHex = "#90A4AE";
            int score = 0;
            double health = 0.0;
            double damage = 0.0;
            boolean alive = false;
        }
    }
}
