package com.example.birdgame3;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OfficialTrailerStoryboardTest {
    @Test
    void nullRocBeatUsesTheAuthoredCutsceneRenderer() {
        assertFalse(BirdGame3.isOfficialTrailerCutsceneScene(1));
        assertFalse(BirdGame3.isOfficialTrailerCutsceneScene(2));
        assertTrue(BirdGame3.isOfficialTrailerCutsceneScene(12));
        assertFalse(BirdGame3.isOfficialTrailerGameplayScene(12));
    }

    @Test
    void storyOpenPromiseAndFinalCardUsePurposeBuiltGraphicShots() {
        assertTrue(BirdGame3.isOfficialTrailerGraphicScene(0));
        assertTrue(BirdGame3.isOfficialTrailerGraphicScene(1));
        assertTrue(BirdGame3.isOfficialTrailerGraphicScene(2));
        assertTrue(BirdGame3.isOfficialTrailerGraphicScene(3));
        assertTrue(BirdGame3.isOfficialTrailerGraphicScene(5));
        assertTrue(BirdGame3.isOfficialTrailerGraphicScene(10));
        assertFalse(BirdGame3.isOfficialTrailerGraphicScene(14));
        assertTrue(BirdGame3.isOfficialTrailerGraphicScene(16));
        assertFalse(BirdGame3.isOfficialTrailerCutsceneScene(0));
        assertFalse(BirdGame3.isOfficialTrailerGameplayScene(0));
    }

    @Test
    void fightingCrescendoUsesRealGameplay() {
        assertTrue(BirdGame3.isOfficialTrailerGameplayScene(4));
        assertTrue(BirdGame3.isOfficialTrailerGameplayScene(6));
        assertTrue(BirdGame3.isOfficialTrailerGameplayScene(7));
        assertTrue(BirdGame3.isOfficialTrailerGameplayScene(8));
        assertTrue(BirdGame3.isOfficialTrailerGameplayScene(9));
        assertTrue(BirdGame3.isOfficialTrailerGameplayScene(11));
        assertTrue(BirdGame3.isOfficialTrailerGameplayScene(13));
        assertTrue(BirdGame3.isOfficialTrailerGameplayScene(14));
        assertTrue(BirdGame3.isOfficialTrailerGameplayScene(15));
        assertFalse(BirdGame3.isOfficialTrailerCutsceneScene(15));
    }

    @Test
    void everyReleaseTrailerSceneHasExactlyOneRenderer() {
        for (int scene = 0; scene <= 16; scene++) {
            int rendererCount = (BirdGame3.isOfficialTrailerGameplayScene(scene) ? 1 : 0)
                    + (BirdGame3.isOfficialTrailerCutsceneScene(scene) ? 1 : 0)
                    + (BirdGame3.isOfficialTrailerGraphicScene(scene) ? 1 : 0);
            assertEquals(1, rendererCount, "scene " + scene);
        }
    }

    @Test
    void modeReelUsesFullScreenGameplayInsteadOfTheOldGraphicGrid() {
        assertTrue(BirdGame3.isOfficialTrailerGameplayScene(7));
        assertFalse(BirdGame3.isOfficialTrailerGraphicScene(7));
    }

    @Test
    void customizationReelUsesTheCompletePlayerSkinCatalog() {
        BirdGame3 game = new BirdGame3();

        assertEquals(49, game.officialTrailerSkinCatalogCount());
        assertEquals(5, BirdGame3.officialTrailerSkinPageCount(
                game.officialTrailerSkinCatalogCount()));
    }

    @Test
    void massBattleAddsOneStoryFighterToEachCompleteRoster() {
        assertEquals(BirdGame3.BirdType.values().length + 1,
                BirdGame3.officialTrailerMassBattleTeamSize());
        assertEquals(23, BirdGame3.officialTrailerMassBattleTeamSize());
        assertEquals(46, BirdGame3.MAX_COMBATANTS);
        assertEquals(24, BirdGame3.STANDARD_MASS_COMBATANTS);
    }

    @Test
    void massBattleUsesTheRealMatchRosterTeamsAndCombatTick() {
        BirdGame3 game = new BirdGame3();
        game.harnessPrepareOfficialTrailerMassBattle(0x23_23_46L);

        assertEquals(46, game.activePlayers);
        int flock = 0;
        int corrupted = 0;
        for (int slot = 0; slot < game.activePlayers; slot++) {
            assertTrue(game.isAI[slot], "slot " + slot);
            assertTrue(game.players[slot] != null, "slot " + slot);
            if (game.getEffectiveTeam(slot) == 1) flock++;
            if (game.getEffectiveTeam(slot) == 2) corrupted++;
        }
        assertEquals(23, flock);
        assertEquals(23, corrupted);

        for (int tick = 0; tick < 720 && game.harnessTick(); tick++) {
            // Advance exactly through the normal Bird.update/world combat path.
        }
        int dealt = 0;
        for (int damage : game.damageDealt) dealt += damage;
        assertTrue(dealt > 0, "The real 23v23 match must reach live hit resolution.");
    }
}
