package com.example.birdgame3;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.prefs.Preferences;

/**
 * Manual, deterministic one-fighter balance pass used between roster identity
 * milestones. It is deliberately named *Run so the normal Maven suite does not
 * execute hundreds of AI matches.
 *
 * Run with:
 *   .\mvnw.cmd test -Dtest=FocusedBalanceLabRun -DbalanceBird=EAGLE -DbalanceMatches=4
 */
class FocusedBalanceLabRun {
    private static final BirdGame3.MapType[] MAP_SAMPLE = {
            BirdGame3.MapType.BATTLEFIELD,
            BirdGame3.MapType.CITY,
            BirdGame3.MapType.FOREST
    };

    @Test
    void runFocusedRosterBalanceLab() throws Exception {
        BirdGame3.BirdType focus = BirdGame3.BirdType.valueOf(
                System.getProperty("balanceBird", "EAGLE").trim().toUpperCase(Locale.ROOT));
        int matchesPerSide = Math.max(1, Integer.getInteger("balanceMatches", 4));
        long seed = 20260821L + focus.ordinal() * 100_000L;
        long maxTicks = 4L * 60L * 60L;
        BirdStats.reloadFromDisk();

        BirdGame3 game = new BirdGame3(Preferences.userRoot().node(
                "/birdfight3-tests/focused-balance-lab/" + UUID.randomUUID()));
        Map<BirdGame3.BirdType, Result> results = new EnumMap<>(BirdGame3.BirdType.class);
        int totalWins = 0;
        int totalLosses = 0;
        int totalDraws = 0;
        long started = System.nanoTime();

        for (BirdGame3.MapType map : MAP_SAMPLE) {
            for (BirdGame3.BirdType opponent : BirdGame3.BirdType.values()) {
                if (opponent == focus) continue;
                Result result = results.computeIfAbsent(opponent, ignored -> new Result());
                for (int match = 0; match < matchesPerSide; match++) {
                    BalanceLab.MatchOutcome left = BalanceLab.playMatch(
                            game, focus, opponent, seed++, maxTicks, map);
                    BalanceLab.MatchOutcome right = BalanceLab.playMatch(
                            game, opponent, focus, seed++, maxTicks, map);
                    result.record(left.winner(), focus);
                    result.record(right.winner(), focus);
                }
            }
        }

        for (Result result : results.values()) {
            totalWins += result.wins;
            totalLosses += result.losses;
            totalDraws += result.draws;
        }
        int decided = totalWins + totalLosses;
        double winRate = decided == 0 ? 0.0 : (double) totalWins / decided;
        double seconds = (System.nanoTime() - started) / 1e9;

        StringBuilder report = new StringBuilder();
        report.append("# Focused Balance Report — ").append(focus.name).append("\n\n")
                .append("- Maps: Battlefield, Pigeon's Rooftops, Big Forest\n")
                .append("- Matches per side, map, and opponent: ").append(matchesPerSide).append('\n')
                .append("- Total matches: ").append(totalWins + totalLosses + totalDraws).append('\n')
                .append("- Draws/timeouts: ").append(totalDraws).append('\n')
                .append(String.format(Locale.ROOT, "- Overall decided win rate: %.1f%% (%d-%d)%n%n",
                        winRate * 100.0, totalWins, totalLosses))
                .append("| Opponent | Wins | Losses | Draws | Win rate |\n")
                .append("|---|---:|---:|---:|---:|\n");
        for (Map.Entry<BirdGame3.BirdType, Result> entry : results.entrySet()) {
            Result result = entry.getValue();
            int matchupDecided = result.wins + result.losses;
            double matchupRate = matchupDecided == 0 ? 0.0 : (double) result.wins / matchupDecided;
            report.append(String.format(Locale.ROOT, "| %s | %d | %d | %d | %.1f%% |%n",
                    entry.getKey().name, result.wins, result.losses, result.draws, matchupRate * 100.0));
        }
        report.append("\n## Focus fighter move telemetry\n\n")
                .append("| Move | Map | Uses | Hits | Damage | KOs | Self-KOs |\n")
                .append("|---|---|---:|---:|---:|---:|---:|\n");
        for (GameplayTelemetry.MoveSnapshot move : game.topTelemetryMovesForBird(focus, 16)) {
            report.append(String.format(Locale.ROOT, "| %s | %s | %d | %d | %d | %d | %d |%n",
                    move.moveName(), move.map(), move.uses(), move.hits(), move.damage(),
                    move.kos(), move.selfKos()));
        }

        Path output = Path.of("audit", "focused-balance-report.md");
        Files.createDirectories(output.getParent());
        Files.writeString(output, report);
        System.out.printf(Locale.ROOT,
                "[focused-balance] %s: %d matches, %.1f%% decided wins in %.1f s -> %s%n",
                focus.name, totalWins + totalLosses + totalDraws, winRate * 100.0,
                seconds, output.toAbsolutePath());
        System.out.println(report);
    }

    private static final class Result {
        int wins;
        int losses;
        int draws;

        void record(BirdGame3.BirdType winner, BirdGame3.BirdType focus) {
            if (winner == null) {
                draws++;
            } else if (winner == focus) {
                wins++;
            } else {
                losses++;
            }
        }
    }
}
