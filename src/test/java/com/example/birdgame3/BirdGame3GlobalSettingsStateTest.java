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
    void loadAndSaveRoundTripsLanLastHost() {
        prefs.put("lan_last_host", "192.168.1.40");

        BirdGame3GlobalSettingsState state = BirdGame3GlobalSettingsState.load(prefs, new String[0], 0);
        assertEquals("192.168.1.40", state.lanLastHost);

        state.lanLastHost = "192.168.1.77";
        state.saveTo(prefs, new String[0]);

        assertEquals("192.168.1.77", prefs.get("lan_last_host", ""));
    }
}
