package com.example.birdgame3;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GameSaveRepositoryTest {
    private Preferences prefs;
    private Preferences importPrefs;

    @BeforeEach
    void setUp() {
        prefs = Preferences.userRoot().node("/birdfight3-tests/" + UUID.randomUUID());
        importPrefs = Preferences.userRoot().node("/birdfight3-tests/" + UUID.randomUUID());
    }

    @AfterEach
    void tearDown() throws BackingStoreException {
        prefs.removeNode();
        prefs.flush();
        importPrefs.removeNode();
        importPrefs.flush();
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
        assertEquals(2, repository.globalPrefs().getInt("save_schema_version", 0));
        assertFalse(repository.backups().isEmpty());
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
        assertEquals(null, clearedProfilePrefs.get("char_raven_unlocked", null));
        assertEquals(0, clearedProfilePrefs.getInt("classic_continues", 0));

        GameSaveRepository.SaveProfile fallback = repository.deleteProfile(secondProfile.id());
        assertEquals(firstProfile.id(), fallback.id());
        assertEquals(firstProfile.id(), repository.activeProfile().id());
        assertEquals(1, repository.profiles().size());
        assertThrows(IllegalStateException.class, () -> repository.deleteProfile(firstProfile.id()));
        assertTrue(repository.backups().size() >= 2);
    }

    @Test
    void restoresProfileDataFromAutomaticResetBackup() {
        GameSaveRepository repository = new GameSaveRepository(prefs);
        Preferences profilePrefs = repository.activeProfilePrefs();
        profilePrefs.putInt("bird_coins", 4030);
        profilePrefs.putBoolean("adv_ch9_done", true);

        repository.clearProfile(repository.activeProfile().id());
        assertEquals(0, repository.activeProfilePrefs().getInt("bird_coins", 0));
        assertFalse(repository.activeProfilePrefs().getBoolean("adv_ch9_done", false));

        GameSaveRepository.SaveBackup backup = repository.backups().getFirst();
        repository.restoreBackup(backup.id());

        assertEquals(4030, repository.activeProfilePrefs().getInt("bird_coins", -1));
        assertTrue(repository.activeProfilePrefs().getBoolean("adv_ch9_done", false));
        assertTrue(repository.backups().size() >= 2);
    }

    @Test
    void exportAndImportRoundTripsProfilesAndGlobals() throws Exception {
        GameSaveRepository source = new GameSaveRepository(prefs);
        source.globalPrefs().putBoolean("setting_music", false);
        source.globalPrefs().put("lan_last_host", "192.168.0.77");
        Preferences sourceProfile = source.activeProfilePrefs();
        sourceProfile.putInt("bird_coins", 4030);
        sourceProfile.putBoolean("char_bat_unlocked", true);

        GameSaveRepository.SaveProfile second = source.createProfile("Boss Rush Slot", true);
        Preferences secondProfile = source.activeProfilePrefs();
        secondProfile.putInt("classic_continues", 6);
        secondProfile.putBoolean("skin_tide_vulture", true);

        Path exportPath = Files.createTempFile("birdfight3-save-", ".birdsave");
        try {
            source.exportTo(exportPath);

            GameSaveRepository imported = new GameSaveRepository(importPrefs);
            imported.importFrom(exportPath);

            assertFalse(imported.globalPrefs().getBoolean("setting_music", true));
            assertEquals("192.168.0.77", imported.globalPrefs().get("lan_last_host", ""));
            assertEquals(2, imported.profiles().size());
            assertEquals("Boss Rush Slot", imported.activeProfile().name());
            assertEquals(6, imported.activeProfilePrefs().getInt("classic_continues", -1));
            assertTrue(imported.activeProfilePrefs().getBoolean("skin_tide_vulture", false));

            imported.setActiveProfile(imported.profiles().stream()
                    .filter(profile -> "Profile 1".equals(profile.name()))
                    .findFirst()
                    .orElseThrow()
                    .id());
            assertEquals(4030, imported.activeProfilePrefs().getInt("bird_coins", -1));
            assertTrue(imported.activeProfilePrefs().getBoolean("char_bat_unlocked", false));
            assertFalse(imported.backups().isEmpty());
        } finally {
            Files.deleteIfExists(exportPath);
        }
    }

    @Test
    void recoversLegacyRootSaveIntoBlankActiveProfileAndCreatesBackup() {
        String profileId = "profile_restore_target";
        Preferences profileNode = prefs.node("save_profiles").node(profileId);
        profileNode.put("name", "Profile 1");
        profileNode.putLong("created_at", 10L);
        profileNode.putLong("updated_at", 10L);
        profileNode.node("save").putBoolean("ach_30", true);
        prefs.put("save_active_profile_id", profileId);
        prefs.putBoolean("save_profiles_migrated", true);

        prefs.putInt("bird_coins", 4030);
        prefs.putInt("classic_continues", 4);
        prefs.putBoolean("ach_0", true);
        prefs.putBoolean("adv_ch9_done", true);

        GameSaveRepository repository = new GameSaveRepository(prefs);

        Preferences restored = repository.activeProfilePrefs();
        assertEquals(4030, restored.getInt("bird_coins", -1));
        assertEquals(4, restored.getInt("classic_continues", -1));
        assertTrue(restored.getBoolean("ach_0", false));
        assertTrue(restored.getBoolean("adv_ch9_done", false));
        assertFalse(restored.getBoolean("ach_30", false));
        assertTrue(prefs.getBoolean("save_legacy_recovered", false));
        assertFalse(repository.backups().isEmpty());
        assertTrue(repository.backups().getFirst().reason().contains("legacy recovery"));
    }
}
