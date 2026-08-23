package com.example.birdgame3;

import org.junit.jupiter.api.Test;

import java.util.UUID;
import java.util.prefs.Preferences;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BalanceLabTest {

    @Test
    void everyMainStageHasADistinctAuditName() throws Exception {
        java.lang.reflect.Method mapName = BalanceLab.Report.class.getDeclaredMethod(
                "mapName", BirdGame3.MapType.class);
        mapName.setAccessible(true);
        java.util.Set<String> names = new java.util.HashSet<>();
        for (BirdGame3.MapType map : BirdGame3.MapType.values()) {
            String name = (String) mapName.invoke(null, map);
            assertTrue(names.add(name), () -> "Duplicate balance-audit stage label: " + name);
            assertTrue(!name.isBlank(), map.name());
        }
    }

    private static BirdGame3 freshGame() {
        return new BirdGame3(Preferences.userRoot().node("/birdfight3-tests/balance-lab/" + UUID.randomUUID()));
    }

    @Test
    void headlessMatchIsDeterministic() {
        long seed = 987654321L;
        long[] hashesA = runCollectingHashes(freshGame(), seed);
        long[] hashesB = runCollectingHashes(freshGame(), seed);
        for (int i = 0; i < hashesA.length; i++) {
            assertEquals(hashesA[i], hashesB[i],
                    "State hash diverged at checkpoint " + i + " — the headless sim is not deterministic.");
        }
    }

    private static long[] runCollectingHashes(BirdGame3 game, long seed) {
        game.harnessPrepareMatch(BirdGame3.BirdType.EAGLE, BirdGame3.BirdType.PENGUIN, seed);
        long[] hashes = new long[6];
        for (int checkpoint = 0; checkpoint < hashes.length; checkpoint++) {
            for (int t = 0; t < 150; t++) {
                if (!game.harnessTick()) break;
            }
            hashes[checkpoint] = game.harnessStateHash();
        }
        return hashes;
    }

    @Test
    void headlessMatchRunsToACleanOutcome() {
        BirdGame3 game = freshGame();
        BalanceLab.MatchOutcome outcome = BalanceLab.playMatch(
                game, BirdGame3.BirdType.TURKEY, BirdGame3.BirdType.HUMMINGBIRD, 42L, 4L * 60 * 60);

        assertTrue(outcome.ticks() > 60, "The match should run for a meaningful number of ticks.");
        // A winner, a timeout decision, or a genuine draw are all acceptable —
        // what matters is that the sim reached an outcome without exploding.
        if (outcome.winner() != null) {
            assertTrue(outcome.winner() == BirdGame3.BirdType.TURKEY
                            || outcome.winner() == BirdGame3.BirdType.HUMMINGBIRD,
                    "The winner must be one of the participants.");
        }
    }
}
