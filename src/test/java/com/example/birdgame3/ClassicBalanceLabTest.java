package com.example.birdgame3;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;
import java.util.prefs.Preferences;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ClassicBalanceLabTest {

    private static BirdGame3 freshGame() {
        return new BirdGame3(Preferences.userRoot().node(
                "/birdfight3-tests/classic-balance-lab-test/" + UUID.randomUUID()));
    }

    @Test
    void classicEncounterHarnessUsesTheRealRoadrunnerOpeningRound() {
        BirdGame3 game = freshGame();
        long routeSeed = 1234L;
        BirdGame3.ClassicEncounter encounter = game.harnessPrepareClassicEncounter(
                BirdGame3.BirdType.ROADRUNNER, 0, 5.0, 5, routeSeed, 5678L);

        assertEquals("Off the Line", encounter.name);
        assertEquals(BirdGame3.ClassicEncounterStyle.MINIATURE_FLOCK, encounter.style);
        assertEquals(BirdGame3.MapType.CITY, game.selectedMap);
        assertTrue(game.classicModeActive);
        assertTrue(game.usesSmashCombatRules());
        assertEquals(2, game.activePlayers,
                "Round 1 is a three-wave miniature gauntlet, starting as a 1-vs-1.");
        assertTrue(game.isAI[0], "The lab must let AI control the Classic player slot.");
        assertEquals(5, game.getCpuLevel(0));
        assertEquals(2, game.scores[0],
                "Roadrunner gets a second stock for the three-wave opening gauntlet.");
        assertFalse(game.players[1].hasUltimate());
    }

    @Test
    void classicEncounterOutcomeIsDeterministic() {
        ClassicBalanceLab.EncounterOutcome first = ClassicBalanceLab.playEncounter(
                freshGame(), BirdGame3.BirdType.ROADRUNNER, 0,
                5.0, 5, 77L, 991L, 4L * 60 * 60);
        ClassicBalanceLab.EncounterOutcome second = ClassicBalanceLab.playEncounter(
                freshGame(), BirdGame3.BirdType.ROADRUNNER, 0,
                5.0, 5, 77L, 991L, 4L * 60 * 60);

        assertEquals(first, second);
        assertTrue(first.ticks() > 60);
    }

    @Test
    void reportSeparatesObjectiveRoundsFromCombatWinRates() {
        ClassicBalanceLab.Config config = new ClassicBalanceLab.Config(
                new BirdGame3.BirdType[]{BirdGame3.BirdType.ROADRUNNER},
                1, 4L * 60 * 60, 88L, 5.0, 5, false);
        ClassicBalanceLab.Report report = ClassicBalanceLab.run(config, null);

        assertTrue(report.summaries().stream()
                .anyMatch(summary -> "Off the Line".equals(summary.encounterName())
                        && summary.matches() == 1 && !summary.objectiveRound()));
        assertTrue(report.summaries().stream()
                .anyMatch(summary -> summary.style() == BirdGame3.ClassicEncounterStyle.REDLINE_RUN
                        && summary.matches() == 0 && summary.objectiveRound()));
        assertTrue(report.markdown().contains("Objective/bonus rounds: listed but not simulated"));
    }

    @Test
    void laterRoadrunnerEncountersReceivePreviouslyEarnedBolts() {
        BirdGame3 game = freshGame();

        game.harnessPrepareClassicEncounter(BirdGame3.BirdType.ROADRUNNER, 7,
                5.0, 5, 77L, 992L);

        assertEquals(7, game.harnessClassicRoadrunnerBoltCount());
    }

    @Test
    void clearRateCountsEveryAttemptInsteadOfDiscardingCutoffDraws() {
        assertEquals(0.30, ClassicBalanceLab.attemptClearRate(3, 10), 0.0001);
        assertEquals(0.0, ClassicBalanceLab.attemptClearRate(0, 0), 0.0);
    }

    @Test
    void sixtyFourMatchReportsTreatFortyFiveWinsAsStatisticalTargetEdge() {
        ClassicBalanceLab.Config config = new ClassicBalanceLab.Config(
                new BirdGame3.BirdType[]{BirdGame3.BirdType.ROADRUNNER},
                64, 4L * 60 * 60, 91L, 5.0, 5, false);
        ClassicBalanceLab.EncounterSummary summary = new ClassicBalanceLab.EncounterSummary(
                BirdGame3.BirdType.ROADRUNNER, 3, "The Roadblock",
                BirdGame3.ClassicEncounterStyle.GIANT, "Titan Dock", false,
                64, 45, 19, 0, 45.0 / 64.0, 60.0 * 80.0, 120.0);
        ClassicBalanceLab.Report report = new ClassicBalanceLab.Report(config, List.of(), List.of(summary));

        assertTrue(report.markdown().contains("| 70.3% |"));
        assertTrue(report.markdown().contains("| Target band |"));
        assertTrue(ClassicBalanceLab.TARGET_CLEAR_RATE_MAX < 46.0 / 64.0);
    }

    @Test
    void authoredBossTimerOutlivesAShortGeneralAuditCapAndStillDecidesTheMatch() {
        ClassicBalanceLab.EncounterOutcome outcome = ClassicBalanceLab.playEncounter(
                freshGame(), BirdGame3.BirdType.VULTURE, 7,
                5.0, 5, 0x7A11A0L, 0x7A11A1L, 60L);

        assertTrue(outcome.ticks() > 60L);
        assertTrue(outcome.decided(),
                "The audit must reach the Debt Engine's real timeout instead of reporting an artificial draw.");
    }
}
