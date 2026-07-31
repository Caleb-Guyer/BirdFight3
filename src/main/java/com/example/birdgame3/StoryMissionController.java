package com.example.birdgame3;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Deterministic per-tick objective state for a Still Sky mission.
 *
 * <p>This controller knows nothing about wall-clock time or presentation. The
 * game supplies one immutable combat snapshot per simulation tick, so two
 * runs with the same seed and inputs produce the same objective state.
 */
final class StoryMissionController {
    enum Outcome { RUNNING, PHASE_ADVANCED, COMPLETE, FAILED }

    private static final double CAPTURE_ZONE_CENTER_Y_OFFSET = 110.0;
    private static final double CAPTURE_ZONE_VERTICAL_RADIUS = 210.0;

    record Participant(int playerIndex, int team, double x, double y,
                       double health, double maxHealth) {
        Participant(int playerIndex, int team, double x, double health, double maxHealth) {
            this(playerIndex, team, x, BirdGame3.GROUND_Y, health, maxHealth);
        }

        boolean alive() {
            return health > 0.0;
        }
    }

    record TickResult(Outcome outcome, int phaseIndex, int checkpointPhaseIndex,
                      String eventId, String message) {
        static TickResult running(int phaseIndex, int checkpointPhaseIndex) {
            return new TickResult(Outcome.RUNNING, phaseIndex, checkpointPhaseIndex, "", "");
        }
    }

    private final StoryCampaign.Mission mission;
    private final StoryCampaign.Difficulty difficulty;
    private final double arenaWidth;
    private final double arenaFloorY;
    private final Set<String> firedEvents = new HashSet<>();
    private int phaseIndex;
    private int checkpointPhaseIndex;
    private int phaseTicks;
    private int objectiveProgressTicks;
    private int capturedTargets;
    private int bossSegment;
    private int gauntletWavesDefeated;
    private boolean reinforcementRequested;
    private boolean complete;
    private boolean failed;

    StoryMissionController(StoryCampaign.Mission mission, StoryCampaign.Difficulty difficulty,
                           double arenaWidth) {
        this(mission, difficulty, arenaWidth, 0);
    }

    StoryMissionController(StoryCampaign.Mission mission, StoryCampaign.Difficulty difficulty,
                           double arenaWidth, int startPhaseIndex) {
        this(mission, difficulty, arenaWidth, BirdGame3.GROUND_Y, startPhaseIndex);
    }

    StoryMissionController(StoryCampaign.Mission mission, StoryCampaign.Difficulty difficulty,
                           double arenaWidth, double arenaFloorY, int startPhaseIndex) {
        this.mission = mission;
        this.difficulty = difficulty == null ? StoryCampaign.Difficulty.NORMAL : difficulty;
        this.arenaWidth = Math.max(640.0, arenaWidth);
        this.arenaFloorY = arenaFloorY;
        this.phaseIndex = Math.clamp(startPhaseIndex, 0, Math.max(0, mission.phases().size() - 1));
        this.checkpointPhaseIndex = this.phaseIndex;
    }

    TickResult tick(List<Participant> participants) {
        if (complete) {
            return new TickResult(Outcome.COMPLETE, phaseIndex, checkpointPhaseIndex,
                    mission.id() + ":complete", "Mission complete");
        }
        if (failed) {
            return new TickResult(Outcome.FAILED, phaseIndex, checkpointPhaseIndex,
                    mission.id() + ":failed", "Mission failed");
        }
        List<Participant> roster = participants == null ? List.of() : participants;
        if (noLivingTeam(roster, 1)) {
            return fail("team_down", "The allied flock was defeated");
        }

        StoryCampaign.MissionPhase phase = currentPhase();
        phaseTicks++;
        boolean success = switch (phase.objective()) {
            case ELIMINATION -> noLivingTeam(roster, 2);
            case GAUNTLET -> tickGauntlet(phase, roster);
            case SURVIVE -> phaseTicks >= scaledTargetTicks(phase);
            case PROTECT -> tickProtect(phase, roster);
            case CAPTURE, HOLD_ZONE -> tickCapture(phase, roster);
            case REACH_EXIT -> tickReachExit(phase, roster);
            case BOSS_PHASES -> tickBossPhase(phase, roster);
        };
        if (failed) {
            return new TickResult(Outcome.FAILED, phaseIndex, checkpointPhaseIndex,
                    mission.id() + ":phase:" + phaseIndex + ":failed", "Objective failed");
        }
        if (!success) {
            return TickResult.running(phaseIndex, checkpointPhaseIndex);
        }
        return advancePhase();
    }

    StoryCampaign.MissionPhase currentPhase() {
        return mission.phases().get(Math.clamp(phaseIndex, 0, mission.phases().size() - 1));
    }

    int phaseIndex() {
        return phaseIndex;
    }

    int checkpointPhaseIndex() {
        return checkpointPhaseIndex;
    }

    int phaseTicks() {
        return phaseTicks;
    }

    int objectiveProgressTicks() {
        return objectiveProgressTicks;
    }

    int capturedTargets() {
        return capturedTargets;
    }

    int bossSegment() {
        return bossSegment;
    }

    int gauntletWavesDefeated() {
        return gauntletWavesDefeated;
    }

    double objectiveAssistTargetX() {
        if (complete || failed) {
            return Double.NaN;
        }
        StoryCampaign.MissionPhase phase = currentPhase();
        return switch (phase.objective()) {
            case CAPTURE, HOLD_ZONE -> {
                int targetCount = Math.max(1, phase.targetCount());
                yield capturedTargets < targetCount
                        ? zoneCenterX(capturedTargets, targetCount)
                        : Double.NaN;
            }
            case REACH_EXIT -> arenaWidth - 120.0;
            default -> Double.NaN;
        };
    }

    boolean takeReinforcementRequest() {
        boolean requested = reinforcementRequested;
        reinforcementRequested = false;
        return requested;
    }

    boolean complete() {
        return complete;
    }

    boolean failed() {
        return failed;
    }

    double objectiveProgressRatio() {
        StoryCampaign.MissionPhase phase = currentPhase();
        return switch (phase.objective()) {
            case SURVIVE, PROTECT, REACH_EXIT ->
                    Math.clamp(phaseTicks / (double) Math.max(1, scaledTargetTicks(phase)), 0.0, 1.0);
            case CAPTURE, HOLD_ZONE -> {
                int targetCount = Math.max(1, phase.targetCount());
                double partial = objectiveProgressTicks / 120.0;
                yield Math.clamp((capturedTargets + partial) / targetCount, 0.0, 1.0);
            }
            case BOSS_PHASES -> Math.clamp(bossSegment / (double) Math.max(1, phase.targetCount()), 0.0, 1.0);
            default -> 0.0;
        };
    }

    long deterministicStateHash() {
        long hash = 0x9E3779B97F4A7C15L;
        hash = mix(hash, phaseIndex);
        hash = mix(hash, checkpointPhaseIndex);
        hash = mix(hash, phaseTicks);
        hash = mix(hash, objectiveProgressTicks);
        hash = mix(hash, capturedTargets);
        hash = mix(hash, bossSegment);
        hash = mix(hash, gauntletWavesDefeated);
        hash = mix(hash, reinforcementRequested ? 1 : 0);
        hash = mix(hash, complete ? 1 : 0);
        hash = mix(hash, failed ? 1 : 0);
        return hash;
    }

    private boolean tickProtect(StoryCampaign.MissionPhase phase, List<Participant> roster) {
        boolean hasProtectedAlly = roster.stream()
                .anyMatch(p -> p.team() == 1 && p.playerIndex() != 0);
        if (hasProtectedAlly && roster.stream()
                .noneMatch(p -> p.team() == 1 && p.playerIndex() != 0 && p.alive())) {
            failed = true;
            return false;
        }
        return phaseTicks >= scaledTargetTicks(phase);
    }

    private boolean tickCapture(StoryCampaign.MissionPhase phase, List<Participant> roster) {
        int targetCount = Math.max(1, phase.targetCount());
        if (capturedTargets >= targetCount) {
            return true;
        }
        double zoneX = zoneCenterX(capturedTargets, targetCount);
        boolean allyPresent = roster.stream()
                .anyMatch(p -> p.team() == 1 && isInsideCaptureZone(p, zoneX, 145.0));
        boolean contested = roster.stream()
                .anyMatch(p -> p.team() == 2 && isInsideCaptureZone(p, zoneX, 175.0));
        if (allyPresent && !contested) {
            objectiveProgressTicks++;
            if (objectiveProgressTicks >= 120) {
                capturedTargets++;
                objectiveProgressTicks = 0;
            }
        } else if (!allyPresent) {
            objectiveProgressTicks = Math.max(0, objectiveProgressTicks - 1);
        }
        if (phase.targetTicks() > 0 && phaseTicks > scaledTargetTicks(phase)) {
            failed = true;
        }
        return capturedTargets >= targetCount;
    }

    private boolean isInsideCaptureZone(Participant participant, double zoneX,
                                        double horizontalRadius) {
        if (participant == null || !participant.alive()) {
            return false;
        }
        double zoneY = arenaFloorY - CAPTURE_ZONE_CENTER_Y_OFFSET;
        return Math.abs(participant.x() - zoneX) <= horizontalRadius
                && Math.abs(participant.y() - zoneY) <= CAPTURE_ZONE_VERTICAL_RADIUS;
    }

    private boolean tickReachExit(StoryCampaign.MissionPhase phase, List<Participant> roster) {
        boolean reached = roster.stream()
                .anyMatch(p -> p.team() == 1 && p.alive()
                        && p.x() >= arenaWidth - 180.0);
        if (!reached && phase.targetTicks() > 0 && phaseTicks > scaledTargetTicks(phase)) {
            failed = true;
        }
        return reached;
    }

    private boolean tickBossPhase(StoryCampaign.MissionPhase phase, List<Participant> roster) {
        Participant boss = roster.stream()
                .filter(p -> p.team() == 2)
                .max((a, b) -> Double.compare(a.maxHealth(), b.maxHealth()))
                .orElse(null);
        if (boss == null || !boss.alive()) {
            bossSegment = Math.max(bossSegment, Math.max(1, phase.targetCount()));
            return true;
        }
        int segments = Math.max(1, phase.targetCount());
        double healthRatio = Math.clamp(boss.health() / Math.max(1.0, boss.maxHealth()), 0.0, 1.0);
        int reached = Math.min(segments - 1, (int) Math.floor((1.0 - healthRatio) * segments));
        bossSegment = Math.max(bossSegment, reached);
        boolean laterBossPhase = mission.phases().subList(
                        Math.min(phaseIndex + 1, mission.phases().size()),
                        mission.phases().size())
                .stream()
                .anyMatch(candidate -> candidate.objective() == StoryCampaign.ObjectiveType.BOSS_PHASES);
        return laterBossPhase && bossSegment >= Math.max(1, segments - 1);
    }

    private boolean tickGauntlet(StoryCampaign.MissionPhase phase, List<Participant> roster) {
        if (!noLivingTeam(roster, 2)) {
            return false;
        }
        gauntletWavesDefeated++;
        if (gauntletWavesDefeated >= Math.max(1, phase.targetCount())) {
            return true;
        }
        reinforcementRequested = true;
        return false;
    }

    private TickResult advancePhase() {
        StoryCampaign.MissionPhase completedPhase = currentPhase();
        String eventId = mission.id() + ":phase:" + phaseIndex + ":complete";
        if (!firedEvents.add(eventId)) {
            return TickResult.running(phaseIndex, checkpointPhaseIndex);
        }
        if (completedPhase.checkpoint()) {
            checkpointPhaseIndex = Math.min(phaseIndex + 1, mission.phases().size() - 1);
        }
        if (phaseIndex + 1 >= mission.phases().size()) {
            complete = true;
            return new TickResult(Outcome.COMPLETE, phaseIndex, checkpointPhaseIndex,
                    eventId, "Mission complete");
        }
        phaseIndex++;
        phaseTicks = 0;
        objectiveProgressTicks = 0;
        capturedTargets = 0;
        bossSegment = 0;
        gauntletWavesDefeated = 0;
        reinforcementRequested = false;
        return new TickResult(Outcome.PHASE_ADVANCED, phaseIndex, checkpointPhaseIndex,
                eventId, currentPhase().label());
    }

    private TickResult fail(String suffix, String message) {
        failed = true;
        return new TickResult(Outcome.FAILED, phaseIndex, checkpointPhaseIndex,
                mission.id() + ":" + suffix, message);
    }

    private int scaledTargetTicks(StoryCampaign.MissionPhase phase) {
        if (phase.targetTicks() <= 0) {
            return Integer.MAX_VALUE;
        }
        return Math.max(1, (int) Math.round(phase.targetTicks() * difficulty.objectiveWindowScale));
    }

    private boolean noLivingTeam(List<Participant> roster, int team) {
        return roster.stream().noneMatch(p -> p.team() == team && p.alive());
    }

    private double zoneCenterX(int index, int total) {
        if (total <= 1) {
            return arenaWidth * 0.5;
        }
        return arenaWidth * (0.24 + 0.52 * index / (double) (total - 1));
    }

    private static long mix(long hash, long value) {
        long mixed = hash ^ (value + 0x9E3779B97F4A7C15L + (hash << 6) + (hash >>> 2));
        return mixed ^ (mixed >>> 27);
    }
}
