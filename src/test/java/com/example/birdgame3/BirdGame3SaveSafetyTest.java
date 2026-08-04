package com.example.birdgame3;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.UUID;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BirdGame3SaveSafetyTest {
    private Preferences testRoot;
    private String priorTrailerExportProperty;

    @AfterEach
    void tearDown() throws BackingStoreException {
        if (priorTrailerExportProperty == null) {
            System.clearProperty("birdfight3.officialTrailerExport");
        } else {
            System.setProperty("birdfight3.officialTrailerExport", priorTrailerExportProperty);
        }
        if (testRoot != null && testRoot.nodeExists("")) {
            Preferences parent = testRoot.parent();
            testRoot.removeNode();
            if (parent != null) {
                parent.flush();
            }
        }
    }

    @Test
    void trailerExportUsesAnIsolatedPreferencesTree() throws Exception {
        priorTrailerExportProperty = System.getProperty("birdfight3.officialTrailerExport");
        System.setProperty("birdfight3.officialTrailerExport", "target/trailer/test.mp4");
        Method defaultRoot = BirdGame3.class.getDeclaredMethod("defaultSavePreferencesRoot");
        defaultRoot.setAccessible(true);

        testRoot = (Preferences) defaultRoot.invoke(null);

        assertTrue(testRoot.absolutePath().startsWith("/birdfight3-tools/trailer/"));
    }

    @Test
    void saveBeforeProfileLoadCannotCreateOrOverwriteSaveData() throws Exception {
        testRoot = Preferences.userRoot().node("/birdfight3-tests/save-safety/" + UUID.randomUUID());
        BirdGame3 game = new BirdGame3(testRoot);

        game.requestProgressSave();

        assertEquals(0, testRoot.keys().length);
        assertEquals(0, testRoot.childrenNames().length);
    }
}
