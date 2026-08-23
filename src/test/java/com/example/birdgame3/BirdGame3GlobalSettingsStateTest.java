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
        prefs.put("internet_last_endpoint", "games.example.com:30100");
        prefs.putInt("internet_host_port", 30100);

        BirdGame3GlobalSettingsState state = BirdGame3GlobalSettingsState.load(prefs, new String[0], 0);
        assertEquals("192.168.1.40", state.lanLastHost);
        assertEquals("games.example.com:30100", state.internetLastEndpoint);
        assertEquals(30100, state.internetHostPort);

        state.lanLastHost = "192.168.1.77";
        state.internetLastEndpoint = "203.0.113.9:28999";
        state.internetHostPort = 28999;
        state.saveTo(prefs, new String[0]);

        assertEquals("192.168.1.77", prefs.get("lan_last_host", ""));
        assertEquals("203.0.113.9:28999", prefs.get("internet_last_endpoint", ""));
        assertEquals(28999, prefs.getInt("internet_host_port", 0));
    }

    @Test
    void loadAndSaveRoundTripsLastSeenUpdateSplash() {
        prefs.put("last_seen_update_splash", "POWER_UP_CHAOS_LAUNCH");

        BirdGame3GlobalSettingsState state = BirdGame3GlobalSettingsState.load(prefs, new String[0], 0);
        assertEquals("POWER_UP_CHAOS_LAUNCH", state.lastSeenUpdateSplashKey);

        state.lastSeenUpdateSplashKey = "REBIRTH_UPDATE";
        state.saveTo(prefs, new String[0]);

        assertEquals("REBIRTH_UPDATE", prefs.get("last_seen_update_splash", ""));
    }

    @Test
    void loadAndSaveRoundTripsVersusRulesPreset() {
        prefs.put("versus_rules_preset", "competitive");

        BirdGame3GlobalSettingsState state = BirdGame3GlobalSettingsState.load(prefs, new String[0], 0);
        assertEquals(VersusRulesPreset.COMPETITIVE.name(), state.versusRulesPresetName);

        state.versusRulesPresetName = VersusRulesPreset.CHAOS.name();
        state.saveTo(prefs, new String[0]);
        assertEquals(VersusRulesPreset.CHAOS.name(), prefs.get("versus_rules_preset", ""));

        prefs.put("versus_rules_preset", "deleted-preset");
        state = BirdGame3GlobalSettingsState.load(prefs, new String[0], 0);
        assertEquals(VersusRulesPreset.STANDARD.name(), state.versusRulesPresetName);
    }
}
