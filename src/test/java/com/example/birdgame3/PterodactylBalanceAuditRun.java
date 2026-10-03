package com.example.birdgame3;

import org.junit.jupiter.api.Test;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.UUID;
import java.util.prefs.Preferences;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Opt-in regression/balance lead audit, with no profile loading or writes. */
class PterodactylBalanceAuditRun {
    @Test void auditEveryOpponentOnRepresentativeStages() throws Exception {
        BirdStats.reloadFromDisk();
        Preferences prefs = Preferences.userRoot().node("/birdfight3-tests/pterodactyl-balance/" + UUID.randomUUID());
        try {
            BirdGame3 game = new BirdGame3(prefs);
            StringBuilder report = new StringBuilder("# Pterodactyl balance audit\n\n")
                    .append("CPU results are human-playtest leads, not a verdict on competitive balance.\n")
                    .append("Two seeds per side against every other fighter; 14,400-tick cap.\n\n")
                    .append("| Stage | Wins | Losses | Draws | Win rate |\n|---|---:|---:|---:|---:|\n");
            long seed = 20261002L;
            int drawsTotal = 0;
            for (BirdGame3.MapType map : new BirdGame3.MapType[]{BirdGame3.MapType.BATTLEFIELD,
                    BirdGame3.MapType.FOREST, BirdGame3.MapType.WORLDSEAM, BirdGame3.MapType.CAVE, BirdGame3.MapType.SKYCLIFFS}) {
                int wins = 0, losses = 0, draws = 0;
                for (BirdGame3.BirdType opponent : BirdGame3.BirdType.values()) {
                    if (opponent == BirdGame3.BirdType.PTERODACTYL) continue;
                    for (int sample = 0; sample < 2; sample++) for (int side = 0; side < 2; side++) {
                        BalanceLab.MatchOutcome outcome = BalanceLab.playMatch(game,
                                side == 0 ? BirdGame3.BirdType.PTERODACTYL : opponent,
                                side == 0 ? opponent : BirdGame3.BirdType.PTERODACTYL, seed++, 14_400, map);
                        if (outcome.winner() == BirdGame3.BirdType.PTERODACTYL) wins++;
                        else if (outcome.winner() == null) draws++;
                        else losses++;
                    }
                }
                drawsTotal += draws;
                String row = String.format(Locale.ROOT, "| %s | %d | %d | %d | %.1f%% |%n", map, wins, losses, draws,
                        100.0 * wins / Math.max(1, wins + losses));
                report.append(row); System.out.print(row);
            }
            Path output = Path.of("target/pterodactyl-balance.md");
            Files.writeString(output, report);
            assertTrue(drawsTotal <= 5, "Unexpected timeout cluster; inspect " + output);
        } finally { prefs.removeNode(); }
    }
}
