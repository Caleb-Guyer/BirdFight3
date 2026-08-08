package com.example.birdgame3;

import org.junit.jupiter.api.Test;

import java.util.Locale;
import java.util.UUID;
import java.util.prefs.Preferences;

/**
 * Manual focused balance audit for Phoenix tuning changes.
 * Deliberately excluded from the default Surefire naming patterns.
 *
 * Run with:
 *   .\mvnw.cmd test -Dtest=PhoenixBalanceLabRun
 */
class PhoenixBalanceLabRun {

    @Test
    void runFocusedPhoenixBalanceLab() {
        BirdStats.reloadFromDisk();
        BirdGame3 game = new BirdGame3(Preferences.userRoot().node(
                "/birdfight3-tests/phoenix-balance-lab/" + UUID.randomUUID()));
        BirdGame3.BirdType focus = BirdGame3.BirdType.PHOENIX;
        long seed = 20260808L;
        int wins = 0;
        int decided = 0;
        int matches = 0;

        for (BirdGame3.MapType map : BirdGame3.MapType.values()) {
            for (BirdGame3.BirdType opponent : BirdGame3.BirdType.values()) {
                if (opponent == focus) continue;
                for (int sample = 0; sample < 2; sample++) {
                    BalanceLab.MatchOutcome left = BalanceLab.playMatch(
                            game, focus, opponent, seed++, 4L * 60 * 60, map);
                    BalanceLab.MatchOutcome right = BalanceLab.playMatch(
                            game, opponent, focus, seed++, 4L * 60 * 60, map);
                    matches += 2;
                    if (left.winner() != null) {
                        decided++;
                        if (left.winner() == focus) wins++;
                    }
                    if (right.winner() != null) {
                        decided++;
                        if (right.winner() == focus) wins++;
                    }
                }
            }
        }

        System.out.printf(Locale.ROOT,
                "[phoenix-balance] %s: %d/%d wins (%.1f%%), %d matches%n",
                focus.name, wins, decided,
                decided == 0 ? 0.0 : wins * 100.0 / decided, matches);
    }
}
