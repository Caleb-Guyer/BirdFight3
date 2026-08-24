package com.example.birdgame3;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class NetworkLobbyReadinessTest {
    @Test
    void everyConnectedPlayerMustAcceptTheVisibleConfiguration() {
        boolean[] connected = {true, true, false, false};
        boolean[] ready = {true, false, true, true};

        assertFalse(NetworkLobbyReadiness.allConnectedPlayersReady(connected, ready, 2));

        ready[1] = true;
        assertTrue(NetworkLobbyReadiness.allConnectedPlayersReady(connected, ready, 2));
    }

    @Test
    void oneReadyHostCannotStartAlone() {
        assertFalse(NetworkLobbyReadiness.allConnectedPlayersReady(
                new boolean[]{true, false, false, false},
                new boolean[]{true, false, false, false}, 2));
    }

    @Test
    void changingHostRulesRevokesEveryReadyState() {
        boolean[] ready = {true, true, false, true};

        NetworkLobbyReadiness.invalidate(ready);

        assertArrayEquals(new boolean[]{false, false, false, false}, ready);
    }
}
