package com.example.birdgame3;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class StoryMissionControllerTest {
    @Test
    void identicalTickInputsProduceIdenticalObjectiveHashes() {
        StoryCampaign.Mission mission = StoryCampaignContent.create().mission("dead_air");
        StoryMissionController first = new StoryMissionController(
                mission, StoryCampaign.Difficulty.NORMAL, 6000);
        StoryMissionController second = new StoryMissionController(
                mission, StoryCampaign.Difficulty.NORMAL, 6000);

        List<Long> firstHashes = runDeadAirObjective(first);
        List<Long> secondHashes = runDeadAirObjective(second);

        assertEquals(firstHashes, secondHashes);
        assertTrue(first.complete());
        assertTrue(second.complete());
    }

    @Test
    void eachTimedDifficultyModifierUsesTheAuthoredWindowScale() {
        StoryCampaign.Mission survive = missionWith(
                StoryCampaign.MissionPhase.timed(
                        StoryCampaign.ObjectiveType.SURVIVE, "Hold", 10, 1, true));
        StoryMissionController easy = new StoryMissionController(survive, StoryCampaign.Difficulty.EASY, 6000);
        StoryMissionController normal = new StoryMissionController(survive, StoryCampaign.Difficulty.NORMAL, 6000);
        StoryMissionController hard = new StoryMissionController(survive, StoryCampaign.Difficulty.HARD, 6000);
        List<StoryMissionController.Participant> player =
                List.of(new StoryMissionController.Participant(0, 1, 1000, 100, 100));

        for (int i = 0; i < 540; i++) {
            easy.tick(player);
            normal.tick(player);
            hard.tick(player);
        }
        assertTrue(hard.complete());
        assertFalse(normal.complete());
        assertFalse(easy.complete());
        for (int i = 540; i < 600; i++) normal.tick(player);
        for (int i = 540; i < 750; i++) easy.tick(player);
        assertTrue(normal.complete());
        assertTrue(easy.complete());
    }

    @Test
    void playerDefeatFailsAndRetainsLatestCheckpoint() {
        StoryCampaign.Mission mission = missionWith(
                StoryCampaign.MissionPhase.timed(
                        StoryCampaign.ObjectiveType.CAPTURE, "Capture", 5, 1, true),
                StoryCampaign.MissionPhase.timed(
                        StoryCampaign.ObjectiveType.REACH_EXIT, "Exit", 1, 1, true));
        StoryMissionController controller = new StoryMissionController(
                mission, StoryCampaign.Difficulty.NORMAL, 6000);

        List<StoryMissionController.Participant> capture =
                List.of(new StoryMissionController.Participant(0, 1, 3000, 100, 100));
        for (int i = 0; i < 120; i++) controller.tick(capture);
        assertEquals(1, controller.phaseIndex());
        assertEquals(1, controller.checkpointPhaseIndex());

        StoryMissionController.TickResult failed = controller.tick(
                List.of(new StoryMissionController.Participant(0, 1, 3000, 0, 100)));
        assertEquals(StoryMissionController.Outcome.FAILED, failed.outcome());
        assertEquals(1, failed.checkpointPhaseIndex());
    }

    @Test
    void gauntletRequestsDeterministicReinforcementWavesOnce() {
        StoryCampaign.Mission mission = missionWith(
                StoryCampaign.MissionPhase.timed(
                        StoryCampaign.ObjectiveType.GAUNTLET, "Two waves", 1, 2, true));
        StoryMissionController controller = new StoryMissionController(
                mission, StoryCampaign.Difficulty.NORMAL, 6000);
        StoryMissionController.Participant player =
                new StoryMissionController.Participant(0, 1, 1000, 100, 100);

        assertEquals(StoryMissionController.Outcome.RUNNING,
                controller.tick(List.of(player)).outcome());
        assertTrue(controller.takeReinforcementRequest());
        assertFalse(controller.takeReinforcementRequest());

        StoryMissionController.Participant enemy =
                new StoryMissionController.Participant(1, 2, 4500, 100, 100);
        controller.tick(List.of(player, enemy));
        StoryMissionController.Participant defeated =
                new StoryMissionController.Participant(1, 2, 4500, 0, 100);
        assertEquals(StoryMissionController.Outcome.COMPLETE,
                controller.tick(List.of(player, defeated)).outcome());
    }

    @Test
    void finalBossTransitionsAtHealthThresholdThenCompletesOnDefeat() {
        StoryCampaign.Mission mission = missionWith(
                StoryCampaign.MissionPhase.timed(
                        StoryCampaign.ObjectiveType.BOSS_PHASES, "Dark wings", 1, 3, true),
                StoryCampaign.MissionPhase.timed(
                        StoryCampaign.ObjectiveType.BOSS_PHASES, "Core", 1, 4, true));
        StoryMissionController controller = new StoryMissionController(
                mission, StoryCampaign.Difficulty.NORMAL, 6000);
        StoryMissionController.Participant player =
                new StoryMissionController.Participant(0, 1, 1000, 100, 100);

        controller.tick(List.of(player,
                new StoryMissionController.Participant(1, 2, 4500, 300, 300)));
        StoryMissionController.TickResult threshold = controller.tick(List.of(player,
                new StoryMissionController.Participant(1, 2, 4500, 95, 300)));
        assertEquals(StoryMissionController.Outcome.PHASE_ADVANCED, threshold.outcome());
        assertEquals(1, controller.phaseIndex());

        StoryMissionController.TickResult defeated = controller.tick(List.of(player,
                new StoryMissionController.Participant(1, 2, 4500, 0, 300)));
        assertEquals(StoryMissionController.Outcome.COMPLETE, defeated.outcome());
    }

    @Test
    void eliminationHoldAndProtectUseCombatStateWithoutWallClockReads() {
        StoryMissionController.Participant player =
                new StoryMissionController.Participant(0, 1, 3000, 100, 100);
        StoryMissionController.Participant ally =
                new StoryMissionController.Participant(1, 1, 1200, 100, 100);
        StoryMissionController.Participant enemy =
                new StoryMissionController.Participant(2, 2, 4500, 100, 100);

        StoryMissionController elimination = new StoryMissionController(
                missionWith(StoryCampaign.MissionPhase.elimination("Clear")),
                StoryCampaign.Difficulty.NORMAL, 6000);
        assertEquals(StoryMissionController.Outcome.RUNNING,
                elimination.tick(List.of(player, ally, enemy)).outcome());
        assertEquals(StoryMissionController.Outcome.COMPLETE,
                elimination.tick(List.of(player, ally,
                        new StoryMissionController.Participant(2, 2, 4500, 0, 100))).outcome());

        StoryMissionController hold = new StoryMissionController(
                missionWith(StoryCampaign.MissionPhase.timed(
                        StoryCampaign.ObjectiveType.HOLD_ZONE, "Hold", 5, 1, true)),
                StoryCampaign.Difficulty.NORMAL, 6000);
        for (int i = 0; i < 119; i++) {
            assertEquals(StoryMissionController.Outcome.RUNNING, hold.tick(List.of(player)).outcome());
        }
        assertEquals(StoryMissionController.Outcome.COMPLETE, hold.tick(List.of(player)).outcome());

        StoryMissionController protect = new StoryMissionController(
                missionWith(StoryCampaign.MissionPhase.timed(
                        StoryCampaign.ObjectiveType.PROTECT, "Protect", 1, 1, true)),
                StoryCampaign.Difficulty.NORMAL, 6000);
        for (int i = 0; i < 59; i++) protect.tick(List.of(player, ally, enemy));
        assertEquals(StoryMissionController.Outcome.COMPLETE,
                protect.tick(List.of(player, ally, enemy)).outcome());

        StoryMissionController failedProtect = new StoryMissionController(
                missionWith(StoryCampaign.MissionPhase.timed(
                        StoryCampaign.ObjectiveType.PROTECT, "Protect", 2, 1, true)),
                StoryCampaign.Difficulty.NORMAL, 6000);
        assertEquals(StoryMissionController.Outcome.FAILED,
                failedProtect.tick(List.of(player,
                        new StoryMissionController.Participant(1, 1, 1200, 0, 100),
                        enemy)).outcome());
    }

    @Test
    void protectionEndsImmediatelyWhenEverySpawnedEnemyIsDefeated() {
        StoryMissionController.Participant player =
                new StoryMissionController.Participant(0, 1, 3000, 100, 100);
        StoryMissionController.Participant protectedAlly =
                new StoryMissionController.Participant(1, 1, 1200, 100, 100);
        StoryMissionController.Participant defeatedEnemy =
                new StoryMissionController.Participant(2, 2, 4500, 0, 100);

        StoryMissionController protect = new StoryMissionController(
                missionWith(StoryCampaign.MissionPhase.timed(
                        StoryCampaign.ObjectiveType.PROTECT, "Protect", 90, 1, true)),
                StoryCampaign.Difficulty.NORMAL, 6000);
        assertEquals(StoryMissionController.Outcome.COMPLETE,
                protect.tick(List.of(player, protectedAlly, defeatedEnemy)).outcome());
        assertEquals(1, protect.phaseTicks(),
                "Clearing the attackers should skip the remaining protection timer.");
    }

    @Test
    void timedCombatPhaseDoesNotEarlyClearBeforeAnyEnemyHasSpawned() {
        StoryMissionController.Participant player =
                new StoryMissionController.Participant(0, 1, 3000, 100, 100);
        StoryMissionController.Participant protectedAlly =
                new StoryMissionController.Participant(1, 1, 1200, 100, 100);
        StoryMissionController protect = new StoryMissionController(
                missionWith(StoryCampaign.MissionPhase.timed(
                        StoryCampaign.ObjectiveType.PROTECT, "Protect", 90, 1, true)),
                StoryCampaign.Difficulty.NORMAL, 6000);

        assertEquals(StoryMissionController.Outcome.RUNNING,
                protect.tick(List.of(player, protectedAlly)).outcome(),
                "A spawn delay must not look like the player already defeated a wave.");
    }

    @Test
    void livingAllyPreventsTeamWipeAndCanFinishElimination() {
        StoryMissionController controller = new StoryMissionController(
                missionWith(StoryCampaign.MissionPhase.elimination("Clear")),
                StoryCampaign.Difficulty.NORMAL, 6000);
        StoryMissionController.Participant defeatedPlayer =
                new StoryMissionController.Participant(0, 1, 1000, 0, 100);
        StoryMissionController.Participant livingAlly =
                new StoryMissionController.Participant(1, 1, 1800, 70, 100);
        StoryMissionController.Participant livingEnemy =
                new StoryMissionController.Participant(2, 2, 4500, 60, 100);

        assertEquals(StoryMissionController.Outcome.RUNNING,
                controller.tick(List.of(defeatedPlayer, livingAlly, livingEnemy)).outcome());

        StoryMissionController.Participant defeatedEnemy =
                new StoryMissionController.Participant(2, 2, 4500, 0, 100);
        assertEquals(StoryMissionController.Outcome.COMPLETE,
                controller.tick(List.of(defeatedPlayer, livingAlly, defeatedEnemy)).outcome());
    }

    @Test
    void livingAllyCanFinishPositionObjectivesAfterPlayerFalls() {
        StoryMissionController capture = new StoryMissionController(
                missionWith(StoryCampaign.MissionPhase.timed(
                        StoryCampaign.ObjectiveType.CAPTURE, "Capture", 5, 1, true)),
                StoryCampaign.Difficulty.NORMAL, 6000);
        StoryMissionController.Participant defeatedPlayer =
                new StoryMissionController.Participant(0, 1, 1000, 0, 100);
        StoryMissionController.Participant allyInZone =
                new StoryMissionController.Participant(1, 1, 3000, 80, 100);
        for (int i = 0; i < 119; i++) {
            assertEquals(StoryMissionController.Outcome.RUNNING,
                    capture.tick(List.of(defeatedPlayer, allyInZone)).outcome());
        }
        assertEquals(StoryMissionController.Outcome.COMPLETE,
                capture.tick(List.of(defeatedPlayer, allyInZone)).outcome());

        StoryMissionController reachExit = new StoryMissionController(
                missionWith(StoryCampaign.MissionPhase.timed(
                        StoryCampaign.ObjectiveType.REACH_EXIT, "Exit", 1, 1, true)),
                StoryCampaign.Difficulty.NORMAL, 6000);
        StoryMissionController.Participant allyAtExit =
                new StoryMissionController.Participant(1, 1, 5900, 80, 100);
        assertEquals(StoryMissionController.Outcome.COMPLETE,
                reachExit.tick(List.of(defeatedPlayer, allyAtExit)).outcome());
    }

    @Test
    void captureRequiresVerticalAsWellAsHorizontalProximity() {
        StoryCampaign.Mission mission = missionWith(
                StoryCampaign.MissionPhase.timed(
                        StoryCampaign.ObjectiveType.CAPTURE, "Capture", 10, 1, true));
        StoryMissionController controller = new StoryMissionController(
                mission, StoryCampaign.Difficulty.NORMAL, 6000);
        StoryMissionController.Participant highAboveZone =
                new StoryMissionController.Participant(
                        0, 1, 3000, BirdGame3.GROUND_Y - 900, 100, 100);

        for (int tick = 0; tick < 120; tick++) {
            assertEquals(StoryMissionController.Outcome.RUNNING,
                    controller.tick(List.of(highAboveZone)).outcome());
        }
        assertEquals(0, controller.objectiveProgressTicks(),
                "Sharing only the territory's X coordinate must not capture it.");

        StoryMissionController.Participant closeToZone =
                new StoryMissionController.Participant(
                        0, 1, 3000, BirdGame3.GROUND_Y - 120, 100, 100);
        for (int tick = 0; tick < 119; tick++) {
            assertEquals(StoryMissionController.Outcome.RUNNING,
                    controller.tick(List.of(closeToZone)).outcome());
        }
        assertEquals(StoryMissionController.Outcome.COMPLETE,
                controller.tick(List.of(closeToZone)).outcome());
    }

    @Test
    void enemyOnlyContestsTerritoryWhenVerticallyClose() {
        StoryCampaign.Mission mission = missionWith(
                StoryCampaign.MissionPhase.timed(
                        StoryCampaign.ObjectiveType.HOLD_ZONE, "Hold", 10, 1, true));
        StoryMissionController contestedController = new StoryMissionController(
                mission, StoryCampaign.Difficulty.NORMAL, 6000);
        StoryMissionController.Participant allyInZone =
                new StoryMissionController.Participant(
                        0, 1, 3000, BirdGame3.GROUND_Y - 100, 100, 100);
        StoryMissionController.Participant enemyInZone =
                new StoryMissionController.Participant(
                        1, 2, 3000, BirdGame3.GROUND_Y - 100, 100, 100);
        for (int tick = 0; tick < 120; tick++) {
            assertEquals(StoryMissionController.Outcome.RUNNING,
                    contestedController.tick(List.of(allyInZone, enemyInZone)).outcome());
        }
        assertEquals(0, contestedController.objectiveProgressTicks());

        StoryMissionController controller = new StoryMissionController(
                mission, StoryCampaign.Difficulty.NORMAL, 6000);
        StoryMissionController.Participant enemyFarAbove =
                new StoryMissionController.Participant(
                        1, 2, 3000, BirdGame3.GROUND_Y - 900, 100, 100);

        for (int tick = 0; tick < 119; tick++) {
            assertEquals(StoryMissionController.Outcome.RUNNING,
                    controller.tick(List.of(allyInZone, enemyFarAbove)).outcome());
        }
        assertEquals(StoryMissionController.Outcome.COMPLETE,
                controller.tick(List.of(allyInZone, enemyFarAbove)).outcome(),
                "An enemy at the same X but high above the territory must not contest it.");
    }

    @Test
    void objectiveAssistTracksEachCaptureTargetAndThenTheExit() {
        StoryCampaign.Mission mission = missionWith(
                StoryCampaign.MissionPhase.timed(
                        StoryCampaign.ObjectiveType.CAPTURE, "Capture", 20, 3, true),
                StoryCampaign.MissionPhase.timed(
                        StoryCampaign.ObjectiveType.REACH_EXIT, "Exit", 20, 1, true));
        StoryMissionController controller = new StoryMissionController(
                mission, StoryCampaign.Difficulty.NORMAL, 6000);

        StoryMissionController.Participant player =
                new StoryMissionController.Participant(0, 1, 1000, 100, 100);
        StoryMissionController.Participant ally =
                new StoryMissionController.Participant(1, 1, 1000, 100, 100);
        double[] zones = {1440.0, 3000.0, 4560.0};
        for (double zone : zones) {
            assertEquals(zone, controller.objectiveAssistTargetX(), 0.0001);
            StoryMissionController.Participant allyInZone =
                    new StoryMissionController.Participant(1, 1, zone, 100, 100);
            for (int tick = 0; tick < 120; tick++) {
                controller.tick(List.of(player, allyInZone));
            }
        }

        assertEquals(5880.0, controller.objectiveAssistTargetX(), 0.0001);
        assertEquals(StoryMissionController.Outcome.COMPLETE,
                controller.tick(List.of(player,
                        new StoryMissionController.Participant(1, 1, 5880, 100, 100))).outcome());
        assertTrue(Double.isNaN(controller.objectiveAssistTargetX()));
    }

    @Test
    void battlefieldObjectivesStayOnTheMainIsland() {
        StoryCampaign.Mission mission = missionWith(
                StoryCampaign.MissionPhase.timed(
                        StoryCampaign.ObjectiveType.HOLD_ZONE, "Hold", 20, 3, true),
                StoryCampaign.MissionPhase.timed(
                        StoryCampaign.ObjectiveType.REACH_EXIT, "Exit", 20, 1, true));
        double islandLeft = 2400.0;
        double islandRight = 3600.0;
        double islandFloorY = BirdGame3.GROUND_Y - 80.0;
        StoryMissionController controller = new StoryMissionController(
                mission, StoryCampaign.Difficulty.NORMAL, 6000,
                islandFloorY, islandLeft, islandRight, 0);

        double[] zones = {2688.0, 3000.0, 3312.0};
        for (double zone : zones) {
            assertEquals(zone, controller.objectiveAssistTargetX(), 0.0001);
            assertTrue(zone >= islandLeft && zone <= islandRight);
            StoryMissionController.Participant playerInZone =
                    new StoryMissionController.Participant(
                            0, 1, zone, islandFloorY - 80.0, 100, 100);
            for (int tick = 0; tick < 120; tick++) {
                controller.tick(List.of(playerInZone));
            }
        }

        assertEquals(3480.0, controller.objectiveAssistTargetX(), 0.0001);
        assertEquals(3420.0, controller.reachExitMarkerX(), 0.0001);
        StoryMissionController.Participant playerAtExit =
                new StoryMissionController.Participant(
                        0, 1, controller.reachExitMarkerX(),
                        islandFloorY - 80.0, 100, 100);
        assertEquals(StoryMissionController.Outcome.COMPLETE,
                controller.tick(List.of(playerAtExit)).outcome());
    }

    private List<Long> runDeadAirObjective(StoryMissionController controller) {
        List<Long> hashes = new ArrayList<>();
        double[] zones = {1440, 3000, 4560};
        for (double zone : zones) {
            for (int tick = 0; tick < 120; tick++) {
                controller.tick(List.of(
                        new StoryMissionController.Participant(0, 1, zone, 100, 100),
                        new StoryMissionController.Participant(1, 1, 1000, 100, 100)
                ));
                hashes.add(controller.deterministicStateHash());
            }
        }
        controller.tick(List.of(
                new StoryMissionController.Participant(0, 1, 5900, 100, 100),
                new StoryMissionController.Participant(1, 1, 1000, 100, 100)
        ));
        hashes.add(controller.deterministicStateHash());
        return hashes;
    }

    private StoryCampaign.Mission missionWith(StoryCampaign.MissionPhase... phases) {
        return new StoryCampaign.Mission(
                "test_mission",
                "Test Mission",
                "Exercise deterministic objectives.",
                BirdGame3.MapType.BATTLEFIELD,
                StoryCampaign.ArenaVariant.STANDARD,
                StoryCampaign.PlayablePolicy.forced(BirdGame3.BirdType.PIGEON),
                List.of(),
                List.of(StoryCampaign.Fighter.enemy(BirdGame3.BirdType.RAVEN, "Test Enemy")),
                List.of(phases),
                "test_pre",
                "test_post",
                null,
                false
        );
    }
}
