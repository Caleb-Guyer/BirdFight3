package com.example.birdgame3;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GameSaveRepositoryTest {
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
    void migratesLegacyProfileDataIntoDefaultProfile() {
        prefs.putBoolean("setting_music", false);
        prefs.put("lan_last_host", "192.168.1.20");
        prefs.putInt("classic_continues", 4);
        prefs.putBoolean("char_bat_unlocked", true);
        prefs.put("daily_challenge_best_key", "2026-03-27");

        GameSaveRepository repository = new GameSaveRepository(prefs);

        assertEquals(1, repository.profiles().size());
        assertEquals("Profile 1", repository.activeProfile().name());
        assertFalse(repository.globalPrefs().getBoolean("setting_music", true));
        assertEquals("192.168.1.20", repository.globalPrefs().get("lan_last_host", ""));

        Preferences activeProfilePrefs = repository.activeProfilePrefs();
        assertEquals(4, activeProfilePrefs.getInt("classic_continues", -1));
        assertTrue(activeProfilePrefs.getBoolean("char_bat_unlocked", false));
        assertEquals("2026-03-27", activeProfilePrefs.get("daily_challenge_best_key", ""));
    }

    @Test
    void createClearAndDeleteProfilesKeepsActiveProfileValid() {
        GameSaveRepository repository = new GameSaveRepository(prefs);
        GameSaveRepository.SaveProfile firstProfile = repository.activeProfile();
        assertNotNull(firstProfile);

        GameSaveRepository.SaveProfile secondProfile = repository.createProfile("Challenge Slot", false);
        GameSaveRepository.SaveProfile renamedProfile = repository.renameProfile(secondProfile.id(), "Boss Rush");
        assertEquals("Boss Rush", renamedProfile.name());

        repository.setActiveProfile(secondProfile.id());
        assertEquals(secondProfile.id(), repository.activeProfile().id());

        Preferences secondProfilePrefs = repository.profilePrefs(secondProfile.id());
        secondProfilePrefs.putBoolean("char_raven_unlocked", true);
        secondProfilePrefs.putInt("classic_continues", 7);

        repository.clearProfile(secondProfile.id());
        Preferences clearedProfilePrefs = repository.profilePrefs(secondProfile.id());
        assertNull(clearedProfilePrefs.get("char_raven_unlocked", null));
        assertEquals(0, clearedProfilePrefs.getInt("classic_continues", 0));

        GameSaveRepository.SaveProfile fallback = repository.deleteProfile(secondProfile.id());
        assertEquals(firstProfile.id(), fallback.id());
        assertEquals(firstProfile.id(), repository.activeProfile().id());
        assertEquals(1, repository.profiles().size());
        assertThrows(IllegalStateException.class, () -> repository.deleteProfile(firstProfile.id()));
    }
}
