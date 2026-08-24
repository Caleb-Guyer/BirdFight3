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

    @Test
    void staminaDamageSpendsAStockAndRespawnsAtFullConfiguredHp() throws Exception {
        BirdGame3 game = new BirdGame3();
        setRules(game, VersusRules.stamina().withStaminaHealth(180).withStockCount(2));
        setSmashCombatRules(game, true);
        game.activePlayers = 2;
        Bird attacker = new Bird(200, BirdGame3.BirdType.PIGEON, 0, game);
        Bird target = new Bird(300, BirdGame3.BirdType.EAGLE, 1, game);
        game.players[0] = attacker;
        game.players[1] = target;
        game.scores[0] = 2;
        game.scores[1] = 2;
        target.health = 180.0;

        double dealt = attacker.applyDamageTo(target, 1_000.0);

        assertEquals(180.0, dealt, 0.0001);
        assertEquals(0.0, target.health, 0.0001);
        assertEquals(1, game.scores[1]);
        assertEquals(1, game.falls[1]);
        assertEquals(0.0, target.smashDamagePercent(), 0.0001,
                "stamina damage must not secretly fill the launch-percent meter");

        target.update(1.0);

        assertEquals(180.0, target.health, 0.0001);
        assertEquals(1, game.scores[1]);
        assertTrue(target.y < BirdGame3.GROUND_Y,
                "the restored fighter should be moved onto the normal respawn return path");
    }

    private static void setRules(BirdGame3 game, VersusRules rules) throws Exception {
        Field field = BirdGame3.class.getDeclaredField("frontEndMatchFlow");
        field.setAccessible(true);
        ((FrontEndMatchFlow) field.get(game)).selectCustomRules(rules);
    }

    private static void setSmashCombatRules(BirdGame3 game, boolean active) throws Exception {
        Field field = BirdGame3.class.getDeclaredField("smashCombatRulesActive");
        field.setAccessible(true);
        field.setBoolean(game, active);
    }
}
