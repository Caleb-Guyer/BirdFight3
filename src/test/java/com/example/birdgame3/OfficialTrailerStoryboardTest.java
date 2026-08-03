package com.example.birdgame3;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OfficialTrailerStoryboardTest {
    @Test
    void narrativeBeatsUseTheAuthoredCutsceneRenderer() {
        assertTrue(BirdGame3.isOfficialTrailerCutsceneScene(0));
        assertTrue(BirdGame3.isOfficialTrailerCutsceneScene(1));
        assertTrue(BirdGame3.isOfficialTrailerCutsceneScene(6));
        assertFalse(BirdGame3.isOfficialTrailerGameplayScene(0));
        assertFalse(BirdGame3.isOfficialTrailerGameplayScene(1));
        assertFalse(BirdGame3.isOfficialTrailerGameplayScene(6));
    }

    @Test
    void nullRockFinaleUsesRealGameplay() {
        assertTrue(BirdGame3.isOfficialTrailerGameplayScene(11));
        assertFalse(BirdGame3.isOfficialTrailerCutsceneScene(11));
    }
}
