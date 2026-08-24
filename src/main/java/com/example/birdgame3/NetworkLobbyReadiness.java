package com.example.birdgame3;

import java.util.Arrays;

/** Pure readiness rules shared by LAN and direct-internet lobbies. */
final class NetworkLobbyReadiness {
    static boolean allConnectedPlayersReady(boolean[] connected, boolean[] ready, int minimumPlayers) {
        if (connected == null || ready == null) return false;
        int count = 0;
        int slots = Math.min(connected.length, ready.length);
        for (int i = 0; i < slots; i++) {
            if (!connected[i]) continue;
            count++;
            if (!ready[i]) return false;
        }
        return count >= Math.max(1, minimumPlayers);
    }

    static void invalidate(boolean[] ready) {
        if (ready != null) Arrays.fill(ready, false);
    }

    private NetworkLobbyReadiness() {
    }
}
