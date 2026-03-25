package com.example.birdgame3;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.UUID;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BirdGame3SettingsTest {
    private Preferences prefs;

    @BeforeEach
    void setUp() {
        prefs = Preferences.userRoot().node("/birdfight3-tests/" + UUID.randomUUID());
    }

    @AfterEach
    void tearDown() throws BackingStoreException {
        prefs.removeNode();
        prefs.flush();
    }

    @Test
    void persistAchievementsStoresAudioChannelVolumes() throws Exception {
        BirdGame3 game = new BirdGame3();

        invokeVolumeSetter(game, "setMusicVolume", 0.42);
        invokeVolumeSetter(game, "setSfxVolume", 0.15);
        game.persistAchievements(prefs, false);

        assertEquals(0.42, prefs.getDouble("setting_music_volume", -1.0), 0.0001);
        assertEquals(0.15, prefs.getDouble("setting_sfx_volume", -1.0), 0.0001);
        assertTrue(prefs.getBoolean("setting_music", false));
        assertTrue(prefs.getBoolean("setting_sfx", false));

        invokeVolumeSetter(game, "setSfxVolume", 0.0);
        game.persistAchievements(prefs, false);

        assertEquals(0.0, prefs.getDouble("setting_sfx_volume", -1.0), 0.0001);
        assertFalse(prefs.getBoolean("setting_sfx", true));
    }

    private static void invokeVolumeSetter(BirdGame3 game, String methodName, double value) throws Exception {
        Method method = BirdGame3.class.getDeclaredMethod(methodName, double.class);
        method.setAccessible(true);
        method.invoke(game, value);
    }
}
