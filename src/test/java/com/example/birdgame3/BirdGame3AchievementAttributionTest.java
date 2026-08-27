package com.example.birdgame3;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BirdGame3AchievementAttributionTest {

    @Test
    void cpuTurkeyCannotUnlockGroundPoundAchievementForProfile() {
        BirdGame3 game = new BirdGame3();
        Bird turkey = new Bird(100.0, BirdGame3.BirdType.TURKEY, 1, game);
        BirdGame3AchievementEvaluator evaluator = evaluator(game);
        game.players[1] = turkey;
        game.isAI[1] = true;
        game.groundPounds[1] = 3;

        evaluator.onCombatStatsUpdated(turkey);

        assertFalse(game.isAchievementUnlocked(BirdGame3Achievement.TURKEY_SLAM_MASTER));

        game.isAI[1] = false;
        evaluator.onCombatStatsUpdated(turkey);

        assertTrue(game.isAchievementUnlocked(BirdGame3Achievement.TURKEY_SLAM_MASTER));
    }

    @Test
    void cpuOpiumBirdCannotAdvanceLeanAchievementForProfile() {
        BirdGame3 game = new BirdGame3();
        Bird opiumBird = new Bird(100.0, BirdGame3.BirdType.OPIUMBIRD, 1, game);
        BirdGame3AchievementEvaluator evaluator = evaluator(game);
        game.players[1] = opiumBird;
        game.isAI[1] = true;

        for (int frame = 0; frame < 1_800; frame++) {
            evaluator.onLeanFrame(opiumBird);
        }

        assertEquals(0, game.achievementProgressValue(BirdGame3Achievement.LEAN_GOD));
        assertFalse(game.isAchievementUnlocked(BirdGame3Achievement.LEAN_GOD));

        game.isAI[1] = false;
        evaluator.onLeanFrame(opiumBird);

        assertEquals(1, game.achievementProgressValue(BirdGame3Achievement.LEAN_GOD));
    }

    @Test
    void cpuActorsCannotAdvanceOtherMatchAchievements() {
        BirdGame3 game = new BirdGame3();
        Bird cpu = new Bird(100.0, BirdGame3.BirdType.PELICAN, 1, game);
        BirdGame3AchievementEvaluator evaluator = evaluator(game);
        game.players[1] = cpu;
        game.isAI[1] = true;
        cpu.health = 100.0;

        for (int count = 0; count < 20; count++) {
            evaluator.onTaunt(cpu);
            evaluator.onPowerUpPickup(cpu);
            evaluator.onPelicanPlunge(cpu);
            evaluator.onHighRooftopJump(cpu.playerIndex);
            evaluator.onHighCliffJump(cpu.playerIndex);
            evaluator.onStageFall(cpu.playerIndex, false);
            evaluator.onNeonPickup(cpu.playerIndex);
            evaluator.onThermalPickup(cpu.playerIndex);
            evaluator.onVineGrapplePickup(cpu.playerIndex);
        }
        evaluator.onAshfallGeyserSurvival(cpu, false);
        evaluator.onMatchWinner(cpu, BirdGame3.MapType.CITY, false);

        assertEquals(0, game.achievementProgressValue(BirdGame3Achievement.TAUNT_LORD));
        assertEquals(0, game.achievementProgressValue(BirdGame3Achievement.PELICAN_KING));
        assertEquals(0, game.achievementProgressValue(BirdGame3Achievement.ROOFTOP_RUNNER));
        assertEquals(0, game.achievementProgressValue(BirdGame3Achievement.CLIFF_DIVER));
        assertEquals(0, game.achievementProgressValue(BirdGame3Achievement.FALL_GUY));
        assertEquals(0, game.achievementProgressValue(BirdGame3Achievement.NEON_ADDICT));
        assertEquals(0, game.achievementProgressValue(BirdGame3Achievement.THERMAL_RIDER));
        assertEquals(0, game.achievementProgressValue(BirdGame3Achievement.VINE_SWINGER));
        assertEquals(0, game.achievementProgressValue(BirdGame3Achievement.URBAN_KING));
        assertEquals(0, game.cityWins[cpu.playerIndex]);
        assertEquals(0, game.rooftopJumps[cpu.playerIndex]);
        assertEquals(0, game.highCliffJumps[cpu.playerIndex]);
        assertEquals(0, game.neonPickups[cpu.playerIndex]);
        assertEquals(0, game.thermalPickups[cpu.playerIndex]);
        assertEquals(0, game.vineGrapplePickups[cpu.playerIndex]);
        assertFalse(game.isAchievementUnlocked(BirdGame3Achievement.POWER_UP_HOARDER));
        assertFalse(game.isAchievementUnlocked(BirdGame3Achievement.GEYSER_RIDER));
    }

    private static BirdGame3AchievementEvaluator evaluator(BirdGame3 game) {
        return new BirdGame3AchievementEvaluator(game, new BirdGame3ProgressionService());
    }
}
