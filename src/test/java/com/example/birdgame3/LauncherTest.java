package com.example.birdgame3;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LauncherTest {
    @Test
    void recognizesDistributionVerificationDryRun() {
        assertTrue(Launcher.isDryRun(new String[]{"--dry-run"}));
        assertTrue(Launcher.isDryRun(new String[]{"ignored", "--dry-run"}));
    }

    @Test
    void ordinaryLaunchArgumentsDoNotEnableDryRun() {
        assertFalse(Launcher.isDryRun(null));
        assertFalse(Launcher.isDryRun(new String[0]));
        assertFalse(Launcher.isDryRun(new String[]{"--trailer"}));
    }
}
