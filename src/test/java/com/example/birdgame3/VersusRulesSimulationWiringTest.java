package com.example.birdgame3;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.*;

class VersusRulesSimulationWiringTest {
    @Test
    void activeCustomRulesDriveStocksTimerRatesAndHazards() throws Exception {
        BirdGame3 game = new BirdGame3();
        VersusRules rules = VersusRules.standard()
                .withStockCount(5)
                .withTimeLimitSeconds(330)
                .withDamageRatePercent(140)
                .withLaunchRatePercent(170)
                .withStageHazardsEnabled(false);
        setRules(game, rules);

        assertTrue(game.appliesVersusRules());
        assertEquals(5, game.smashStartingStocks());
        assertEquals(19_800, game.versusMatchTimerFrames());
        assertEquals(1.4, game.versusDamageRateMultiplier(), 0.0001);
        assertEquals(1.7, game.versusLaunchRateMultiplier(), 0.0001);
        assertFalse(game.versusStageHazardsEnabled());
    }

    @Test
    void friendlyFireOnlyOpensDamageBetweenVersusTeammatesWhenEnabled() throws Exception {
        BirdGame3 game = new BirdGame3();
        game.activePlayers = 2;
        game.teamModeEnabled = true;
        Field teamField = BirdGame3.class.getDeclaredField("playerTeams");
        teamField.setAccessible(true);
        int[] teams = (int[]) teamField.get(game);
        teams[0] = 1;
        teams[1] = 1;
        Bird attacker = new Bird(200, BirdGame3.BirdType.PIGEON, 0, game);
        Bird target = new Bird(300, BirdGame3.BirdType.EAGLE, 1, game);
        game.players[0] = attacker;
        game.players[1] = target;

        setRules(game, VersusRules.standard().withFriendlyFireEnabled(false));
        assertFalse(game.canDamage(attacker, target));

        setRules(game, VersusRules.standard().withFriendlyFireEnabled(true));
        assertTrue(game.canDamage(attacker, target));
    }

    private static void setRules(BirdGame3 game, VersusRules rules) throws Exception {
        Field field = BirdGame3.class.getDeclaredField("frontEndMatchFlow");
        field.setAccessible(true);
        ((FrontEndMatchFlow) field.get(game)).selectCustomRules(rules);
    }
}
