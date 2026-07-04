package com.example.birdgame3;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;
import java.util.prefs.Preferences;

import static org.junit.jupiter.api.Assertions.assertFalse;

class DesktopShortcutSupportTest {

    private final Preferences prefs =
            Preferences.userRoot().node("/birdfight3-tests/shortcut/" + UUID.randomUUID());

    @AfterEach
    void cleanup() throws Exception {
        prefs.removeNode();
        System.clearProperty("jpackage.app-version");
    }

    @Test
    void devRunsWithoutJpackagePropertyDoNothing() {
        System.clearProperty("jpackage.app-version");

        DesktopShortcutSupport.ensureDesktopShortcut(prefs);

        assertFalse(prefs.getBoolean(DesktopShortcutSupport.PREF_KEY, false),
                "A dev run must not consume the one-time shortcut offer.");
    }

    @Test
    void packagedFlagAloneIsNotEnoughWhenProcessIsABareJvm() {
        // Tests run under java.exe; even with the jpackage marker present the
        // guard must refuse to point a shortcut at the JVM itself.
        System.setProperty("jpackage.app-version", "9.9.9");

        DesktopShortcutSupport.ensureDesktopShortcut(prefs);

        assertFalse(prefs.getBoolean(DesktopShortcutSupport.PREF_KEY, false),
                "A bare-JVM process must not consume the shortcut offer or create a link.");
    }
}
