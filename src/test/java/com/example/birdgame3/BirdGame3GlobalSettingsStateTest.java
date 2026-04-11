package com.example.birdgame3;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BirdGame3GlobalSettingsStateTest {
    private Preferences prefs;

    @BeforeEach
    void setUp() {
        prefs = Preferences.userRoot().node("/birdfight3-settings-tests/" + UUID.randomUUID());
    }

    @AfterEach
    void tearDown() throws BackingStoreException {
        prefs.removeNode();
        prefs.flush();
    }

    @Test
    void loadAndSaveRoundTripsOnlineRelayHost() {
        prefs.put("online_relay_host", "relay.example.com");

        BirdGame3GlobalSettingsState state = BirdGame3GlobalSettingsState.load(prefs, new String[0], 0);
        assertEquals("relay.example.com", state.onlineRelayHost);

        state.onlineRelayHost = "relay2.example.com";
        state.saveTo(prefs, new String[0]);

        assertEquals("relay2.example.com", prefs.get("online_relay_host", ""));
    }
}
