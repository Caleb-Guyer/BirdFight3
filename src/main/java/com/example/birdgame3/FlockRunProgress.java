package com.example.birdgame3;

import java.util.EnumMap;
import java.util.prefs.Preferences;

/** Saved alongside the active profile, through the normal save-safety gates. */
final class FlockRunProgress {
    FlockRunState run;
    private final EnumMap<BirdGame3.BirdType, FlockRunState.Medal> medals = new EnumMap<>(BirdGame3.BirdType.class);
    private int bestScore;

    int bestScore() { return bestScore; }
    FlockRunState.Medal medal(BirdGame3.BirdType bird) { return medals.getOrDefault(bird, FlockRunState.Medal.NONE); }

    void recordCompletedRun() {
        if (run == null || run.phase() != FlockRunState.Phase.WON) return;
        bestScore = Math.max(bestScore, run.score());
        if (run.medal().ordinal() > medal(run.bird).ordinal()) medals.put(run.bird, run.medal());
    }

    static FlockRunProgress load(Preferences prefs) {
        FlockRunProgress progress = new FlockRunProgress();
        progress.run = FlockRunState.decode(prefs.get("flock_run_pending", ""));
        progress.bestScore = Math.max(0, Math.min(5000, prefs.getInt("flock_run_best_score", 0)));
        for (BirdGame3.BirdType bird : BirdGame3.BirdType.values()) {
            try { progress.medals.put(bird, FlockRunState.Medal.valueOf(prefs.get("flock_run_medal_" + bird.name(), "NONE"))); }
            catch (IllegalArgumentException ignored) { /* Ignore an unrecognized legacy/corrupt medal. */ }
        }
        progress.recordCompletedRun();
        return progress;
    }

    void save(Preferences prefs) {
        prefs.put("flock_run_pending", run == null ? "" : run.encode());
        prefs.putInt("flock_run_best_score", bestScore);
        for (BirdGame3.BirdType bird : BirdGame3.BirdType.values()) prefs.put("flock_run_medal_" + bird.name(), medal(bird).name());
    }
}
