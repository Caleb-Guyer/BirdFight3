package com.example.birdgame3;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.UUID;
import java.util.prefs.Preferences;

/** Opt-in, identical-seed before/after audit; never loads a live profile. */
class GameplayPolishAuditRun {
    @Test
    void auditRecoverySensitiveFighters() throws Exception {
        BirdStats.reloadFromDisk();
        Preferences prefs = Preferences.userRoot().node("/birdfight3-tests/gameplay-polish/" + UUID.randomUUID());
        try {
            BirdGame3 game = new BirdGame3(prefs);
            BirdGame3.BirdType[] focus = {BirdGame3.BirdType.TITMOUSE, BirdGame3.BirdType.PELICAN, BirdGame3.BirdType.RAVEN};
            BirdGame3.MapType[] maps = {BirdGame3.MapType.BATTLEFIELD, BirdGame3.MapType.FOREST,
                    BirdGame3.MapType.WORLDSEAM, BirdGame3.MapType.CAVE};
            StringBuilder report = new StringBuilder("# Gameplay polish audit\n\n")
                    .append("CPU matches are regression and playtest leads, not human balance verdicts.\n\n")
                    .append("Each fighter faces all 21 opponents twice per side on each stage.\n")
                    .append("Seeds start at 20260908; matches use a 14,400-tick cap.\n\n")
                    .append("| Fighter | Stage | Wins | Losses | Draws | Win rate |\n")
                    .append("|---|---|---:|---:|---:|---:|\n");
            long seed = 20260908L;
            for (BirdGame3.BirdType bird : focus) {
                for (BirdGame3.MapType map : maps) {
                    int wins = 0, losses = 0, draws = 0;
                    for (BirdGame3.BirdType opponent : BirdGame3.BirdType.values()) {
                        // Preserve the original 22-fighter cohort and seed order
                        // used by the recorded before/after landing comparison.
                        if (bird == opponent || opponent.ordinal() > BirdGame3.BirdType.KIWI.ordinal()) continue;
                        for (int sample = 0; sample < 2; sample++) {
                            for (int side = 0; side < 2; side++) {
                                BalanceLab.MatchOutcome result = BalanceLab.playMatch(game,
                                        side == 0 ? bird : opponent, side == 0 ? opponent : bird,
                                        seed++, 14_400L, map);
                                if (result.winner() == bird) wins++;
                                else if (result.winner() == null) draws++;
                                else losses++;
                            }
                        }
                    }
                    String row = String.format(Locale.ROOT, "| %s | %s | %d | %d | %d | %.1f%% |%n",
                            bird.name, map, wins, losses, draws, 100.0 * wins / Math.max(1, wins + losses));
                    report.append(row);
                    System.out.print(row);
                }
            }
            Path output = Path.of(System.getProperty("polishReport", "target/gameplay-polish-after.md"));
            Files.createDirectories(output.toAbsolutePath().getParent());
            Files.writeString(output, report);
        } finally { prefs.removeNode(); }
    }
}
