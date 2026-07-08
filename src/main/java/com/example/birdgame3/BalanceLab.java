package com.example.birdgame3;

import com.example.birdgame3.BirdGame3.BirdType;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.prefs.Preferences;

/**
 * Headless AI-vs-AI balance lab.
 *
 * <p>Plays every roster pairing as full deterministic smash-rules matches on
 * Battlefield — no rendering, no JavaFX toolkit, no progression side effects —
 * and aggregates win rates into a matchup matrix. Each pairing runs with the
 * sides swapped equally to cancel any spawn-position advantage, and every
 * match gets its own seed, so a rerun with the same config reproduces the same
 * results exactly.
 *
 * <p>Tuning loop: run the lab, adjust {@code bird-stats.properties} (which is
 * loaded from the working directory at startup and applies to the lab too),
 * re-run, and watch the outliers move toward 50%.
 */
final class BalanceLab {

    record Config(int matchesPerPairPerSide, long maxTicksPerMatch, long baseSeed) {
        static Config defaults() {
            // 2 per side = 4 per pairing; cap at ~4 sim minutes per match
            // (90s timer + generous sudden-death allowance).
            return new Config(2, 4L * 60 * 60, 20260708L);
        }
    }

    record MatchOutcome(BirdType left, BirdType right, BirdType winner, long ticks) {
    }

    record Report(List<MatchOutcome> outcomes, BirdType[] roster,
                  double[][] winRateMatrix, double[] overallWinRate, int[] decidedMatches) {

        String markdown() {
            StringBuilder sb = new StringBuilder();
            sb.append("# Balance Lab Report\n\n");
            sb.append("- Matches played: ").append(outcomes.size()).append('\n');
            long draws = outcomes.stream().filter(o -> o.winner() == null).count();
            sb.append("- Draws/timeouts: ").append(draws).append('\n');
            sb.append("- Map: Battlefield, smash rules, AI vs AI\n\n");

            sb.append("## Tier list (overall win rate)\n\n");
            sb.append("| Bird | Win rate | Decided matches |\n|---|---:|---:|\n");
            Integer[] order = new Integer[roster.length];
            for (int i = 0; i < order.length; i++) order[i] = i;
            java.util.Arrays.sort(order, Comparator.comparingDouble((Integer i) -> overallWinRate[i]).reversed());
            for (int i : order) {
                sb.append(String.format(Locale.ROOT, "| %s | %.1f%% | %d |%n",
                        roster[i].name, overallWinRate[i] * 100.0, decidedMatches[i]));
            }

            sb.append("\n## Matchup matrix (row's win rate vs column)\n\n| |");
            for (BirdType t : roster) sb.append(' ').append(shortName(t)).append(" |");
            sb.append('\n').append("|---|");
            sb.append("---|".repeat(roster.length)).append('\n');
            for (int r = 0; r < roster.length; r++) {
                sb.append("| ").append(shortName(roster[r])).append(" |");
                for (int c = 0; c < roster.length; c++) {
                    if (r == c) {
                        sb.append(" - |");
                    } else if (Double.isNaN(winRateMatrix[r][c])) {
                        sb.append(" ? |");
                    } else {
                        sb.append(String.format(Locale.ROOT, " %.0f%% |", winRateMatrix[r][c] * 100.0));
                    }
                }
                sb.append('\n');
            }
            return sb.toString();
        }

        private static String shortName(BirdType t) {
            String n = t.name();
            return n.length() <= 4 ? n : n.substring(0, 4);
        }
    }

    private BalanceLab() {
    }

    static Report run(Config config, Consumer<String> progress) {
        BirdType[] roster = BirdType.values();
        BirdGame3 game = new BirdGame3(
                Preferences.userRoot().node("/birdfight3-tests/balance-lab/" + UUID.randomUUID()));
        List<MatchOutcome> outcomes = new ArrayList<>();
        long seed = config.baseSeed();
        int totalPairs = roster.length * (roster.length - 1) / 2;
        int pairIndex = 0;

        for (int a = 0; a < roster.length; a++) {
            for (int b = a + 1; b < roster.length; b++) {
                pairIndex++;
                for (int n = 0; n < config.matchesPerPairPerSide(); n++) {
                    outcomes.add(playMatch(game, roster[a], roster[b], seed++, config.maxTicksPerMatch()));
                    outcomes.add(playMatch(game, roster[b], roster[a], seed++, config.maxTicksPerMatch()));
                }
                if (progress != null) {
                    progress.accept(String.format(Locale.ROOT, "pair %d/%d  %s vs %s done",
                            pairIndex, totalPairs, roster[a].name, roster[b].name));
                }
            }
        }
        return aggregate(outcomes, roster);
    }

    static MatchOutcome playMatch(BirdGame3 game, BirdType left, BirdType right, long seed, long maxTicks) {
        game.harnessPrepareMatch(left, right, seed);
        long ticks = 0;
        while (game.harnessTick() && ticks < maxTicks) {
            ticks++;
        }
        BirdType winner = null;
        if (game.harnessWinner != null) {
            winner = game.harnessWinner.playerIndex == 0 ? left : right;
        } else if (!game.matchEnded) {
            // Hit the tick cap: decide on stocks, then health, else draw.
            Bird p0 = game.players[0];
            Bird p1 = game.players[1];
            if (p0 != null && p1 != null) {
                if (game.scores[0] != game.scores[1]) {
                    winner = game.scores[0] > game.scores[1] ? left : right;
                } else if (p0.health != p1.health) {
                    winner = p0.health > p1.health ? left : right;
                }
            }
        }
        return new MatchOutcome(left, right, winner, ticks);
    }

    private static Report aggregate(List<MatchOutcome> outcomes, BirdType[] roster) {
        int n = roster.length;
        int[][] wins = new int[n][n];
        int[][] decided = new int[n][n];
        for (MatchOutcome outcome : outcomes) {
            if (outcome.winner() == null) continue;
            int w = outcome.winner().ordinal();
            int l = (outcome.winner() == outcome.left() ? outcome.right() : outcome.left()).ordinal();
            wins[w][l]++;
            decided[w][l]++;
            decided[l][w]++;
        }
        double[][] matrix = new double[n][n];
        double[] overall = new double[n];
        int[] totals = new int[n];
        for (int r = 0; r < n; r++) {
            int totalWins = 0;
            int totalDecided = 0;
            for (int c = 0; c < n; c++) {
                if (r == c) continue;
                matrix[r][c] = decided[r][c] == 0 ? Double.NaN : (double) wins[r][c] / decided[r][c];
                totalWins += wins[r][c];
                totalDecided += decided[r][c];
            }
            overall[r] = totalDecided == 0 ? 0.0 : (double) totalWins / totalDecided;
            totals[r] = totalDecided;
        }
        return new Report(outcomes, roster, matrix, overall, totals);
    }
}
