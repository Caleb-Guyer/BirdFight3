package com.example.birdgame3;

import java.util.ArrayList;
import java.util.List;

/**
 * A recorded match: the sim seed plus per-tick human input masks and dash-tap
 * events. Because the simulation is deterministic (single seeded SimRng stream,
 * tick-based timing), feeding these inputs back from the same seed reproduces
 * the match exactly.
 *
 * <p>Each frame holds one int mask per player: bit {@code ControlAction.ordinal()}
 * for held actions, plus the attack-up/attack-down bits defined in BirdGame3.
 * AI inputs are not recorded — AI re-derives its decisions deterministically
 * from game state and the sim RNG during playback.
 */
final class MatchReplay {
    /** Hard cap: 10 minutes at 60 ticks/s. Recording gives up beyond this. */
    static final int MAX_FRAMES = 60 * 60 * 10;
    /**
     * Bumped whenever a deterministic gameplay change makes older input streams
     * unsafe to play with the current simulation.
     */
    static final int CURRENT_SIMULATION_REVISION = 3;

    record DashTap(long tick, int playerIndex, int dir) {
    }

    final long seed;
    final int playerCount;
    final int simulationRevision;
    final List<int[]> frames = new ArrayList<>();
    final List<DashTap> dashTaps = new ArrayList<>();
    boolean overflowed = false;

    // Match configuration captured at record time, making playback self-contained:
    // a replay can be watched later regardless of current menu selections. The
    // slot* arrays hold the RESOLVED roster (random picks already decided) and the
    // adaptive-balance multipliers that were in force.
    String mapName;
    boolean teamModeEnabled;
    boolean mutatorModeEnabled;
    String[] slotBirdTypes;
    boolean[] slotIsAi;
    int[] slotTeams;
    String[] slotSkinKeys;
    double[] slotBaseSize;
    double[] slotBasePower;
    double[] slotBaseSpeed;
    long timestampMillis;
    String winnerLabel = "";

    MatchReplay(long seed, int playerCount) {
        this(seed, playerCount, CURRENT_SIMULATION_REVISION);
    }

    MatchReplay(long seed, int playerCount, int simulationRevision) {
        this.seed = seed;
        this.playerCount = playerCount;
        this.simulationRevision = simulationRevision;
    }

    boolean usable() {
        return !overflowed && !frames.isEmpty();
    }

    boolean selfContained() {
        return mapName != null && slotBirdTypes != null;
    }

    boolean compatibleWithCurrentSimulation() {
        return simulationRevision == CURRENT_SIMULATION_REVISION;
    }

    long durationTicks() {
        return frames.size();
    }
}
