package com.example.birdgame3;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;
import java.util.prefs.Preferences;

/**
 * Opt-in grid search for authored Adventure handicap thresholds.
 *
 * <p>Example:
 * {@code .\mvnw.cmd test -Dtest=AdventureBalanceSearchRun
 * -DadventureSearch=dead_air:NORMAL:0.55,0.60,0.65 -DadventureMatches=24}
 */
class AdventureBalanceSearchRun {

    @Test
    void searchRequestedMissionDifficultyProfiles() {
        String requested = System.getProperty("adventureSearch", "").trim();
        Assumptions.assumeFalse(requested.isEmpty(), "Set -DadventureSearch to run the tuning grid.");
        int matches = Math.max(1, Integer.getInteger("adventureMatches", 24));
        long baseSeed = Long.getLong("adventureSeed", 20260828L);
        BirdStats.reloadFromDisk();

        BirdGame3 catalog = freshGame();
        List<StoryCampaign.Mission> missions = catalog.harnessAdventureMissions();
        try {
            for (String request : requested.split(";")) {
                String[] fields = request.trim().split(":", 3);
                if (fields.length != 3) {
                    throw new IllegalArgumentException("Expected mission:DIFFICULTY:value,value; got " + request);
                }
                String missionId = fields[0].trim();
                StoryCampaign.Difficulty difficulty = StoryCampaign.Difficulty.valueOf(
                        fields[1].trim().toUpperCase(java.util.Locale.ROOT));
                int missionNumber = missionNumber(missions, missionId);
                StoryCampaign.Mission mission = missions.get(missionNumber - 1);
                List<BirdGame3.BirdType> allowedBirds = mission.playable().resolvedBirds();

                for (String rawCandidate : fields[2].split(",")) {
                    double candidate = Double.parseDouble(rawCandidate.trim());
                    AdventureMissionTuning.harnessOverrideAdvantage(missionId, difficulty, candidate);
                    BirdGame3 game = freshGame();
                    int wins = 0;
                    int cutoffs = 0;
                    for (int match = 0; match < matches; match++) {
                        BirdGame3.BirdType player = allowedBirds.get(match % allowedBirds.size());
                        long seed = AdventureBalanceLab.mixSeed(
                                baseSeed, missionNumber - 1, difficulty.ordinal(), match);
                        AdventureBalanceLab.MissionOutcome outcome = AdventureBalanceLab.playMission(
                                game, missionId, difficulty, player, 5, seed,
                                6L * 60L * 60L);
                        if (outcome.won()) wins++;
                        if (!outcome.decided()) cutoffs++;
                    }
                    System.out.printf(java.util.Locale.ROOT,
                            "[adventure-search] %s %s %.3f -> %.1f%% (%d/%d, %d cutoffs)%n",
                            missionId, difficulty, candidate, 100.0 * wins / matches,
                            wins, matches, cutoffs);
                }
                AdventureMissionTuning.harnessClearAdvantageOverrides();
            }
        } finally {
            AdventureMissionTuning.harnessClearAdvantageOverrides();
        }
    }

    private static int missionNumber(List<StoryCampaign.Mission> missions, String missionId) {
        for (int i = 0; i < missions.size(); i++) {
            if (missions.get(i).id().equals(missionId)) return i + 1;
        }
        throw new IllegalArgumentException("Unknown Adventure mission: " + missionId);
    }

    private static BirdGame3 freshGame() {
        return new BirdGame3(Preferences.userRoot().node(
                "/birdfight3-tests/adventure-balance-search/" + UUID.randomUUID()));
    }
}
