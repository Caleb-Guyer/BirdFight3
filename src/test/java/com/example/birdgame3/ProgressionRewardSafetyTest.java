package com.example.birdgame3;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProgressionRewardSafetyTest {
    @Test
    void onlyAWinningSideWithAHumanCanEarnMatchRewards() {
        BirdGame3 game = new BirdGame3();
        game.activePlayers = 3;
        game.isAI[0] = false;
        game.isAI[1] = true;
        game.isAI[2] = true;

        assertTrue(game.profileHumanSideWonMatch(0));
        assertFalse(game.profileHumanSideWonMatch(1));
        assertFalse(game.profileHumanSideWonMatch(-1));

        game.teamModeEnabled = true;
        assertFalse(game.profileHumanSideWonMatch(1), "A CPU-only team must not pay the profile");

        game.isAI[2] = false;
        game.cycleLocalTeamForPlayer(2);
        assertTrue(game.profileHumanSideWonMatch(1), "A mixed team victory should reward its human teammate");
    }

    @Test
    void replayAndHeadlessResultsAreNeverRewardEligible() {
        BirdGame3 game = new BirdGame3();
        game.activePlayers = 1;
        game.isAI[0] = false;

        game.replayPlaybackActive = true;
        assertFalse(game.profileHumanSideWonMatch(0));
        game.replayPlaybackActive = false;
        game.headlessHarnessMode = true;
        assertFalse(game.profileHumanSideWonMatch(0));
    }

    @Test
    void cpuWinsDrawsAndReplayWinsPayZeroCoins() throws Exception {
        BirdGame3 game = new BirdGame3();
        game.activePlayers = 2;
        game.isAI[0] = false;
        game.isAI[1] = true;
        Bird cpu = new Bird(100.0, BirdGame3.BirdType.EAGLE, 1, game);
        Bird human = new Bird(200.0, BirdGame3.BirdType.PIGEON, 0, game);
        Method award = BirdGame3.class.getDeclaredMethod("awardBirdCoinsForMatch", Bird.class);
        award.setAccessible(true);

        assertEquals(0, award.invoke(game, cpu));
        assertEquals(0, award.invoke(game, new Object[]{null}));
        game.replayPlaybackActive = true;
        assertEquals(0, award.invoke(game, human));
    }
}
