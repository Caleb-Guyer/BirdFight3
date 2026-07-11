package com.example.birdgame3;

import com.example.birdgame3.BirdGame3.BirdType;
import com.example.birdgame3.BirdGame3.MapType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.prefs.Preferences;

/**
 * Headless AI-vs-AI balance lab.
 *
 * <p>Plays every roster pairing as full deterministic smash-rules matches on
 * every stage with no rendering, no JavaFX toolkit, and no progression side
 * effects. Each pairing runs with the sides swapped equally to cancel any
 * spawn-position advantage, and every match gets its own seed, so a rerun with
 * the same config reproduces the same results exactly.
 *
 * <p>Tuning loop: run the lab, adjust {@code bird-stats.properties} (which is
 * loaded from the working directory at startup and applies to the lab too),
 * re-run, and watch the outliers move toward 50%.
 */
final class BalanceLab {

    record Config(int matchesPerPairPerSide, long maxTicksPerMatch, long baseSeed, MapType[] maps) {
        Config {
            if (maps == null || maps.length == 0) {
                maps = new MapType[]{MapType.BATTLEFIELD};
            } else {
                maps = maps.clone();
            }
        }

        static Config defaults() {
            // 2 per side = 4 per pairing per map; cap at ~4 sim minutes per match
            // (90s timer + generous sudden-death allowance).
            return new Config(2, 4L * 60 * 60, 20260708L, MapType.values());
        }

        @Override
        public MapType[] maps() {
            return maps.clone();
        }
    }

    record MatchOutcome(BirdType left, BirdType right, BirdType winner, MapType map, long ticks) {
    }

    private record Aggregate(double[][] winRateMatrix, double[] overallWinRate, int[] decidedMatches) {
    }

    record MapSummary(MapType map, int matches, long draws, double[][] winRateMatrix,
                      double[] overallWinRate, int[] decidedMatches) {
    }

    record Report(List<MatchOutcome> outcomes, BirdType[] roster, MapType[] maps,
                  double[][] winRateMatrix, double[] overallWinRate, int[] decidedMatches,
                  List<MapSummary> mapSummaries) {

        String markdown() {
            StringBuilder sb = new StringBuilder();
            sb.append("# Balance Lab Report\n\n");
            sb.append("- Matches played: ").append(outcomes.size()).append('\n');
            long draws = outcomes.stream().filter(o -> o.winner() == null).count();
            sb.append("- Draws/timeouts: ").append(draws).append('\n');
            sb.append("- Maps: ").append(mapList()).append('\n');
            sb.append("- Rules: smash, AI vs AI\n\n");

            appendTierList(sb, "Tier list (all maps)", overallWinRate, decidedMatches);

            sb.append("\n## Map summary\n\n");
            sb.append("| Map | Matches | Draws | Leader | Leader win rate | Lowest | Lowest win rate |\n");
            sb.append("|---|---:|---:|---|---:|---|---:|\n");
            for (MapSummary summary : mapSummaries) {
                int leader = bestIndex(summary.overallWinRate());
                int trailer = worstIndex(summary.overallWinRate());
                sb.append(String.format(Locale.ROOT, "| %s | %d | %d | %s | %.1f%% | %s | %.1f%% |%n",
                        mapName(summary.map()),
                        summary.matches(),
                        summary.draws(),
                        roster[leader].name,
                        summary.overallWinRate()[leader] * 100.0,
                        roster[trailer].name,
                        summary.overallWinRate()[trailer] * 100.0));
            }

            sb.append("\n## Tier list by map\n");
            for (MapSummary summary : mapSummaries) {
                appendTierList(sb, mapName(summary.map()), summary.overallWinRate(), summary.decidedMatches());
            }

            appendMatchupMatrix(sb, "\n## Matchup matrix (all maps)", winRateMatrix);
            return sb.toString();
        }

        private String mapList() {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < maps.length; i++) {
                if (i > 0) sb.append(", ");
                sb.append(mapName(maps[i]));
            }
            return sb.toString();
        }

        private void appendTierList(StringBuilder sb, String title, double[] rates, int[] totals) {
            sb.append("## ").append(title).append("\n\n");
            sb.append("| Bird | Win rate | Decided matches |\n|---|---:|---:|\n");
            Integer[] order = orderedByWinRate(rates);
            for (int i : order) {
                sb.append(String.format(Locale.ROOT, "| %s | %.1f%% | %d |%n",
                        roster[i].name, rates[i] * 100.0, totals[i]));
            }
            sb.append('\n');
        }

        private void appendMatchupMatrix(StringBuilder sb, String title, double[][] matrix) {
            sb.append(title).append("\n\n| |");
            for (BirdType t : roster) sb.append(' ').append(shortName(t)).append(" |");
            sb.append('\n').append("|---|");
            sb.append("---|".repeat(roster.length)).append('\n');
            for (int r = 0; r < roster.length; r++) {
                sb.append("| ").append(shortName(roster[r])).append(" |");
                for (int c = 0; c < roster.length; c++) {
                    if (r == c) {
                        sb.append(" - |");
                    } else if (Double.isNaN(matrix[r][c])) {
                        sb.append(" ? |");
                    } else {
                        sb.append(String.format(Locale.ROOT, " %.0f%% |", matrix[r][c] * 100.0));
                    }
                }
                sb.append('\n');
            }
        }

        private Integer[] orderedByWinRate(double[] rates) {
            Integer[] order = new Integer[roster.length];
            for (int i = 0; i < order.length; i++) order[i] = i;
            Arrays.sort(order, Comparator.comparingDouble((Integer i) -> rates[i]).reversed());
            return order;
        }

        private int bestIndex(double[] rates) {
            return orderedByWinRate(rates)[0];
        }

        private int worstIndex(double[] rates) {
            Integer[] order = orderedByWinRate(rates);
            return order[order.length - 1];
        }

        private static String mapName(MapType map) {
            return switch (map) {
                case CITY -> "Pigeon's Rooftops";
                case SKYCLIFFS -> "Sky Cliffs";
                case VIBRANT_JUNGLE -> "Vibrant Jungle";
                case DESERT -> "Sunscorch Flats";
                case CAVE -> "Echo Cavern";
                case BATTLEFIELD -> "Battlefield";
                case BEACON_CROWN -> "Beacon Crown";
                case DOCK -> "Broken Harbor";
                case FROSTBITE_FJORD -> "Frostbite Fjord";
                case ASHFALL_CATHEDRAL -> "Ashfall Cathedral";
                default -> "Big Forest";
            };
        }

        private static String shortName(BirdType t) {
            String n = t.name();
            return n.length() <= 4 ? n : n.substring(0, 4);
        }
    }

    private BalanceLab() {
    }

    static Report run(Config config, Consumer<String> progress) {
        // The lab plays under the same tuning overrides as the real game, so a
        // bird-stats.properties edit + rerun measures exactly what would ship.
        String tuning = BirdStats.reloadFromDisk();
        if (progress != null) {
            progress.accept(tuning != null ? tuning : "BIRD STATS: COMPILED DEFAULTS");
        }
        BirdType[] roster = BirdType.values();
        MapType[] maps = config.maps();
        BirdGame3 game = new BirdGame3(
                Preferences.userRoot().node("/birdfight3-tests/balance-lab/" + UUID.randomUUID()));
        List<MatchOutcome> outcomes = new ArrayList<>();
        long seed = config.baseSeed();
        int totalPairs = roster.length * (roster.length - 1) / 2;
        int totalMapPairs = totalPairs * maps.length;
        int mapPairIndex = 0;

        for (MapType map : maps) {
            int pairIndex = 0;
            for (int a = 0; a < roster.length; a++) {
                for (int b = a + 1; b < roster.length; b++) {
                    pairIndex++;
                    mapPairIndex++;
                    for (int n = 0; n < config.matchesPerPairPerSide(); n++) {
                        outcomes.add(playMatch(game, roster[a], roster[b], seed++, config.maxTicksPerMatch(), map));
                        outcomes.add(playMatch(game, roster[b], roster[a], seed++, config.maxTicksPerMatch(), map));
                    }
                    if (progress != null) {
                        progress.accept(String.format(Locale.ROOT, "map %s pair %d/%d  overall %d/%d  %s vs %s done",
                                Report.mapName(map), pairIndex, totalPairs, mapPairIndex, totalMapPairs,
                                roster[a].name, roster[b].name));
                    }
                }
            }
        }
        return aggregate(outcomes, roster, maps);
    }

    static MatchOutcome playMatch(BirdGame3 game, BirdType left, BirdType right, long seed, long maxTicks) {
        return playMatch(game, left, right, seed, maxTicks, MapType.BATTLEFIELD);
    }

    static MatchOutcome playMatch(BirdGame3 game, BirdType left, BirdType right, long seed, long maxTicks, MapType map) {
        game.harnessPrepareMatch(left, right, seed, map);
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
        return new MatchOutcome(left, right, winner, map == null ? MapType.BATTLEFIELD : map, ticks);
    }

    private static Report aggregate(List<MatchOutcome> outcomes, BirdType[] roster, MapType[] maps) {
        Aggregate allMaps = aggregate(outcomes, roster, ignored -> true);
        List<MapSummary> mapSummaries = new ArrayList<>();
        for (MapType map : maps) {
            List<MatchOutcome> mapOutcomes = outcomes.stream()
                    .filter(outcome -> outcome.map() == map)
                    .toList();
            Aggregate mapAggregate = aggregate(mapOutcomes, roster, ignored -> true);
            long draws = mapOutcomes.stream().filter(o -> o.winner() == null).count();
            mapSummaries.add(new MapSummary(map, mapOutcomes.size(), draws,
                    mapAggregate.winRateMatrix(), mapAggregate.overallWinRate(), mapAggregate.decidedMatches()));
        }
        return new Report(outcomes, roster, maps.clone(),
                allMaps.winRateMatrix(), allMaps.overallWinRate(), allMaps.decidedMatches(), mapSummaries);
    }

    private static Aggregate aggregate(List<MatchOutcome> outcomes, BirdType[] roster,
                                       Predicate<MatchOutcome> include) {
        int n = roster.length;
        int[][] wins = new int[n][n];
        int[][] decided = new int[n][n];
        for (MatchOutcome outcome : outcomes) {
            if (!include.test(outcome) || outcome.winner() == null) continue;
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
        return new Aggregate(matrix, overall, totals);
    }
}
