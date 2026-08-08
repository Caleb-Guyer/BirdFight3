package com.example.birdgame3;

import javafx.scene.input.KeyCode;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MatchControllerTest {
    @Test
    void standardBattleDurationIsTwoMinutesThirtySeconds() {
        assertEquals(150 * 60, BirdGame3.MATCH_DURATION_FRAMES);
    }

    @Test
    void prepareMatchStartClearsTransientMatchState() {
        BirdGame3 game = new BirdGame3();
        MatchController controller = new MatchController(game);

        game.players[0] = new Bird(100, BirdGame3.BirdType.PIGEON, 0, game);
        game.players[1] = new Bird(250, BirdGame3.BirdType.EAGLE, 1, game);
        game.scores[0] = 120;
        game.scores[1] = 80;
        game.falls[0] = 1;
        game.damageDealt[0] = 50;
        game.eliminations[0] = 2;
        game.killFeed.add("OLD EVENT");
        game.matchEnded = true;
        game.matchHistoryRecorded = true;
        game.balanceOutcomeRecorded = true;
        game.hitstopFrames = 12;
        game.matchTimer = 1;

        controller.prepareMatchStart(null);

        assertEquals(BirdGame3.MATCH_DURATION_FRAMES, game.matchTimer);
        assertFalse(game.matchEnded);
        assertFalse(game.matchHistoryRecorded);
        assertFalse(game.balanceOutcomeRecorded);
        assertEquals(0, game.hitstopFrames);
        assertTrue(game.killFeed.isEmpty());
        assertEquals(0, game.scores[0]);
        assertEquals(0, game.scores[1]);
        assertEquals(0, game.falls[0]);
        assertEquals(0, game.damageDealt[0]);
        assertEquals(0, game.eliminations[0]);
        assertNull(game.players[0]);
        assertNull(game.players[1]);
    }

    @Test
    void prepareMatchStartClearsLatchedGameplayInputs() {
        BirdGame3 game = new BirdGame3();
        MatchController controller = new MatchController(game);
        game.activePlayers = 2;

        KeyCode right = game.rightKeyForPlayer(0);
        game.setLocalActionsForKey(right, true);
        assertTrue(game.isRightPressed(0));

        controller.prepareMatchStart(null);

        assertFalse(game.isRightPressed(0));
    }

    @Test
    void findTimeoutWinnerPrefersHigherStocksInSmashMode() throws Exception {
        BirdGame3 game = new BirdGame3();
        MatchController controller = new MatchController(game);
        setSmashCombatRules(game, true);
        game.activePlayers = 2;
        game.players[0] = new Bird(100, BirdGame3.BirdType.PIGEON, 0, game);
        game.players[1] = new Bird(250, BirdGame3.BirdType.EAGLE, 1, game);
        game.scores[0] = 2;
        game.scores[1] = 1;
        game.players[0].receiveExternalDamage(220);
        game.players[1].receiveExternalDamage(20);

        Bird winner = controller.findTimeoutWinner();

        assertEquals(game.players[0], winner);
    }

    @Test
    void updateTimerStartsSmashSuddenDeathWhenStocksTie() throws Exception {
        BirdGame3 game = new BirdGame3();
        MatchController controller = new MatchController(game);
        setSmashCombatRules(game, true);
        game.activePlayers = 2;
        game.players[0] = new Bird(100, BirdGame3.BirdType.PIGEON, 0, game);
        game.players[1] = new Bird(250, BirdGame3.BirdType.EAGLE, 1, game);
        game.scores[0] = 2;
        game.scores[1] = 2;
        game.matchTimer = 0;

        controller.updateTimerAndSuddenDeath();

        assertTrue(game.suddenDeath.isActive());
        assertTrue(game.suddenDeath.isSmashStyle());
        assertEquals(1, game.scores[0]);
        assertEquals(1, game.scores[1]);
        assertEquals(BirdGame3.SMASH_SUDDEN_DEATH_PERCENT, game.players[0].smashDamagePercent(), 0.0001);
        assertEquals(BirdGame3.SMASH_SUDDEN_DEATH_PERCENT, game.players[1].smashDamagePercent(), 0.0001);
        assertTrue(game.killFeed.stream().anyMatch(line -> line.contains("SUDDEN DEATH")));
    }

    @Test
    void smashSuddenDeathContinuesSpawningCrowsAfterItStarts() throws Exception {
        BirdGame3 game = new BirdGame3();
        MatchController controller = new MatchController(game);
        setSmashCombatRules(game, true);
        game.activePlayers = 2;
        game.players[0] = new Bird(100, BirdGame3.BirdType.PIGEON, 0, game);
        game.players[1] = new Bird(250, BirdGame3.BirdType.EAGLE, 1, game);
        game.scores[0] = 2;
        game.scores[1] = 2;
        game.matchTimer = 0;

        controller.updateTimerAndSuddenDeath();
        for (int i = 0; i < 140; i++) {
            controller.updateTimerAndSuddenDeath();
        }

        assertTrue(game.suddenDeath.isActive());
        assertFalse(game.crowMinions.isEmpty());
    }

    @Test
    void classicSmashRoundsUseTeamStockCompletion() throws Exception {
        BirdGame3 game = new BirdGame3();
        MatchController controller = new MatchController(game);
        setSmashCombatRules(game, true);
        game.classicModeActive = true;
        game.classicTeamMode = true;

        Method teamMatch = MatchController.class.getDeclaredMethod("isStandardTeamMatch");
        teamMatch.setAccessible(true);

        assertTrue((boolean) teamMatch.invoke(controller));
    }

    private static void setSmashCombatRules(BirdGame3 game, boolean active) throws Exception {
        Field field = BirdGame3.class.getDeclaredField("smashCombatRulesActive");
        field.setAccessible(true);
        field.setBoolean(game, active);
    }
}
