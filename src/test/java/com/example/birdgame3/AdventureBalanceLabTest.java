package com.example.birdgame3;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.prefs.Preferences;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AdventureBalanceLabTest {

    private static BirdGame3 freshGame() {
        return new BirdGame3(Preferences.userRoot().node(
                "/birdfight3-tests/adventure-balance-lab-test/" + UUID.randomUUID()));
    }

    @Test
    void harnessExposesEveryAuthoredMissionInCampaignOrder() {
        List<StoryCampaign.Mission> missions = freshGame().harnessAdventureMissions();

        assertEquals(40, missions.size());
        assertEquals("dead_air", missions.getFirst().id());
        assertEquals("the_null_rock", missions.getLast().id());
        assertEquals(40, missions.stream().map(StoryCampaign.Mission::id)
                .collect(java.util.stream.Collectors.toSet()).size());
    }

    @Test
    void everyMissionCanBePreparedOnEasyNormalAndHardWithComparablePlayerAi() {
        BirdGame3 game = freshGame();
        Set<String> prepared = new HashSet<>();

        for (StoryCampaign.Mission mission : game.harnessAdventureMissions()) {
            BirdGame3.BirdType player = mission.playable().resolvedBirds().getFirst();
            int previousEnemyCpu = 0;
            for (StoryCampaign.Difficulty difficulty : StoryCampaign.Difficulty.values()) {
                StoryCampaign.Mission preparedMission = game.harnessPrepareAdventureMission(
                        mission.id(), difficulty, player, 5,
                        0xAD7E_0000L + mission.id().hashCode() * 31L + difficulty.ordinal());

                prepared.add(preparedMission.id() + ":" + difficulty.name());
                assertTrue(game.campaignModeActive);
                assertEquals(mission.map(), game.selectedMap);
                assertNotNull(game.players[0]);
                assertTrue(game.isAI[0]);
                assertEquals(5, game.getCpuLevel(0),
                        "The player pilot must stay fixed so difficulty comparisons are meaningful.");
                assertEquals(0, game.harnessCampaignPhaseIndex());

                int enemySlot = firstLivingEnemySlot(game);
                if (enemySlot > 0) {
                    int enemyCpu = game.getCpuLevel(enemySlot);
                    assertTrue(enemyCpu > previousEnemyCpu,
                            "Enemy AI must rise from Easy to Normal to Hard even when boss reductions apply.");
                    assertTrue(enemyCpu <= difficulty.cpuLevel);
                    previousEnemyCpu = enemyCpu;
                }
            }
        }
        assertEquals(40 * 3, prepared.size());
    }

    @Test
    void difficultyTargetsStepDownFromEasyToHard() {
        AdventureBalanceLab.TargetBand easy = AdventureBalanceLab.targetBand(
                StoryCampaign.Difficulty.EASY);
        AdventureBalanceLab.TargetBand normal = AdventureBalanceLab.targetBand(
                StoryCampaign.Difficulty.NORMAL);
        AdventureBalanceLab.TargetBand hard = AdventureBalanceLab.targetBand(
                StoryCampaign.Difficulty.HARD);

        assertEquals(0.50, easy.target(), 0.0001);
        assertEquals(0.35, normal.target(), 0.0001);
        assertEquals(0.15, hard.target(), 0.0001);
        assertTrue(easy.target() > normal.target());
        assertTrue(normal.target() > hard.target());
        assertTrue(easy.contains(0.50));
        assertFalse(hard.contains(0.50));
    }

    @Test
    void missionTargetsBecomeHarderAcrossTheCampaignAndForBosses() {
        AdventureBalanceLab.TargetBand early = AdventureBalanceLab.targetBand(
                StoryCampaign.Difficulty.NORMAL, 1, false, false);
        AdventureBalanceLab.TargetBand middle = AdventureBalanceLab.targetBand(
                StoryCampaign.Difficulty.NORMAL, 20, false, false);
        AdventureBalanceLab.TargetBand late = AdventureBalanceLab.targetBand(
                StoryCampaign.Difficulty.NORMAL, 40, false, false);
        AdventureBalanceLab.TargetBand lateBoss = AdventureBalanceLab.targetBand(
                StoryCampaign.Difficulty.NORMAL, 40, true, false);
        AdventureBalanceLab.TargetBand finalBoss = AdventureBalanceLab.targetBand(
                StoryCampaign.Difficulty.NORMAL, 40, true, true);

        assertEquals(0.45, early.target(), 0.0001);
        assertTrue(early.target() > middle.target());
        assertTrue(middle.target() > late.target());
        assertTrue(late.target() > lateBoss.target());
        assertTrue(lateBoss.target() > finalBoss.target());
    }

    @Test
    void campaignMissionOutcomeIsDeterministic() {
        AdventureBalanceLab.MissionOutcome first = AdventureBalanceLab.playMission(
                freshGame(), "dead_air", StoryCampaign.Difficulty.NORMAL,
                BirdGame3.BirdType.PIGEON, 5, 991L, 6L * 60L * 60L);
        AdventureBalanceLab.MissionOutcome second = AdventureBalanceLab.playMission(
                freshGame(), "dead_air", StoryCampaign.Difficulty.NORMAL,
                BirdGame3.BirdType.PIGEON, 5, 991L, 6L * 60L * 60L);

        assertEquals(first, second);
        assertTrue(first.decided());
        assertTrue(first.ticks() > 60L);
    }

    @Test
    void reportScoresEveryDifficultyAndCountsCutoffsAsFailedClears() {
        AdventureBalanceLab.Config config = new AdventureBalanceLab.Config(
                List.of("dead_air"), List.of(StoryCampaign.Difficulty.values()),
                1, 6L * 60L * 60L, 88L, 5);
        AdventureBalanceLab.Report report = AdventureBalanceLab.runWithoutTuningReload(config, null);

        assertEquals(3, report.summaries().size());
        assertEquals(Set.of(StoryCampaign.Difficulty.values()),
                report.summaries().stream().map(AdventureBalanceLab.MissionSummary::difficulty)
                        .collect(java.util.stream.Collectors.toSet()));
        assertTrue(report.markdown().contains("Easy | 3 | 50% | 40%–60%"));
        assertTrue(report.markdown().contains("Normal | 5 | 35% | 25%–45%"));
        assertTrue(report.markdown().contains("Hard | 7 | 15% | 5%–25%"));
        assertTrue(report.markdown().contains("Earlier missions rise by up to 10 points"));
        assertEquals(0.30, AdventureBalanceLab.attemptClearRate(3, 10), 0.0001);
    }

    @Test
    void perfectWeatherCrowPassivesCannotMutateTheActiveCrowTraversal() {
        long seed = AdventureBalanceLab.mixSeed(20260828L, 30,
                StoryCampaign.Difficulty.NORMAL.ordinal(), 0);

        AdventureBalanceLab.MissionOutcome outcome = AdventureBalanceLab.playMission(
                freshGame(), "perfect_weather", StoryCampaign.Difficulty.NORMAL,
                BirdGame3.BirdType.EAGLE, 5, seed, 6L * 60L * 60L);

        assertTrue(outcome.decided(),
                "The mission must finish without a crow-passive concurrent modification crash.");
    }

    @Test
    void harborEngineTrapCallbacksCannotMutateTheActiveTrapTraversal() {
        AdventureBalanceLab.Config config = new AdventureBalanceLab.Config(
                List.of("harbor_engine"), List.of(StoryCampaign.Difficulty.EASY),
                24, 6L * 60L * 60L, 20260828L, 5);

        AdventureBalanceLab.Report report = AdventureBalanceLab.runWithoutTuningReload(config, null);

        assertEquals(1, report.summaries().size());
        assertEquals(24, report.summaries().getFirst().matches());
        assertEquals(0, report.summaries().getFirst().cutoffs(),
                "Harbor Engine must complete without trap traversal crashes or harness hangs.");
    }

    @Test
    void crowCountryDamageCallbacksCannotMutateTheActiveQuillTraversal() {
        try {
            AdventureMissionTuning.harnessOverrideAdvantage(
                    "crow_country", StoryCampaign.Difficulty.NORMAL, 2.75);
            AdventureBalanceLab.Config config = new AdventureBalanceLab.Config(
                    List.of("crow_country"), List.of(StoryCampaign.Difficulty.NORMAL),
                    24, 6L * 60L * 60L, 20260828L, 5);

            AdventureBalanceLab.Report report = AdventureBalanceLab.runWithoutTuningReload(config, null);

            assertEquals(1, report.summaries().size());
            assertEquals(24, report.summaries().getFirst().matches());
            assertEquals(0, report.summaries().getFirst().cutoffs(),
                    "Crow Country must complete without Raven quill traversal crashes.");
        } finally {
            AdventureMissionTuning.harnessClearAdvantageOverrides();
        }
    }

    @Test
    void objectiveAiCanAdvanceNeedleRouteCapture() {
        BirdGame3 game = freshGame();
        game.harnessPrepareAdventureMission(
                "needle_route", StoryCampaign.Difficulty.EASY,
                BirdGame3.BirdType.HUMMINGBIRD, 5, 0x0B1EC71EL);
        for (int i = 1; i < game.activePlayers; i++) {
            if (game.players[i] != null && game.getEffectiveTeam(i) == 2) {
                game.players[i].health = 0.0;
            }
        }

        long ticks = 0L;
        while (ticks < 4_200L && game.harnessCampaignPhaseIndex() == 0 && game.harnessTick()) {
            ticks++;
        }

        Bird player = game.players[0];
        BirdGame3.CampaignObjectiveTarget target = game.campaignObjectiveAssistTarget(player);
        assertTrue(game.harnessCampaignPhaseIndex() > 0,
                "Objective AI never advanced the capture phase after " + ticks
                        + " ticks; player=" + player.bodyCenterX() + "," + player.bodyCenterY()
                        + " health=" + player.health + " target=" + target);
    }

    @Test
    void objectiveAiCanReachBellkeeperRelay() {
        BirdGame3 game = freshGame();
        game.harnessPrepareAdventureMission(
                "master_key", StoryCampaign.Difficulty.EASY,
                BirdGame3.BirdType.MOCKINGBIRD, 5, 0xBE11C0DEL);

        long ticks = 0L;
        while (ticks < 3_000L && game.harnessCampaignPhaseIndex() == 0 && game.harnessTick()) {
            ticks++;
        }

        Bird player = game.players[0];
        BirdGame3.CampaignObjectiveTarget target = game.campaignObjectiveAssistTarget(player);
        assertTrue(game.harnessCampaignPhaseIndex() > 0,
                "Objective AI never reached the Bellkeeper relay after " + ticks
                        + " ticks; player=" + player.bodyCenterX() + "," + player.bodyCenterY()
                        + " health=" + player.health + " target=" + target);
    }

    private static int firstLivingEnemySlot(BirdGame3 game) {
        for (int i = 1; i < game.activePlayers; i++) {
            if (game.players[i] != null && game.getEffectiveTeam(i) == 2) return i;
        }
        return -1;
    }
}
