package com.example.birdgame3;

import org.junit.jupiter.api.Test;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.UUID;
import java.util.prefs.Preferences;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Opt-in pilot diagnostics: measure real combat decisions instead of buffing global stats. */
class PterodactylPilotAuditRun {
    @Test void traceCommittedMovesAndVerticalSpacing() throws Exception {
        BirdStats.reloadFromDisk();
        Preferences prefs = Preferences.userRoot().node("/birdfight3-tests/pterodactyl-pilot/" + UUID.randomUUID());
        try {
            BirdGame3 game = new BirdGame3(prefs);
            StringBuilder report = new StringBuilder("stage,opponent,winner,ticks,damage,stunned,farVertical,gust,lunge,lift,swoop,carry,slam\n");
            for (BirdGame3.MapType map : new BirdGame3.MapType[]{BirdGame3.MapType.FOREST, BirdGame3.MapType.BATTLEFIELD}) {
                for (BirdGame3.BirdType opponent : new BirdGame3.BirdType[]{BirdGame3.BirdType.PIGEON,
                        BirdGame3.BirdType.FALCON, BirdGame3.BirdType.TURKEY, BirdGame3.BirdType.GOOSE,
                        BirdGame3.BirdType.EAGLE, BirdGame3.BirdType.BAT}) {
                    game.harnessPrepareMatch(BirdGame3.BirdType.PTERODACTYL, opponent, 91827 + opponent.ordinal(), map);
                    Bird bird = game.players[0], target = game.players[1];
                    int[] phases = new int[9]; int previous = 0, ticks = 0, stunned = 0, vertical = 0;
                    while (!game.matchEnded && ticks < 14400) {
                        game.harnessTick(); ticks++;
                        int phase = bird.pterodactyl.phase;
                        if (phase != previous && phase != 0) phases[phase]++;
                        previous = phase;
                        if (bird.stunTime > 0) stunned++;
                        if (Math.abs(bird.bodyCenterY() - target.bodyCenterY()) > 120) vertical++;
                    }
                    assertTrue(game.matchEnded, "Pilot cutoff on " + map + " vs " + opponent);
                    report.append(String.format(Locale.ROOT, "%s,%s,%s,%d,%d,%d,%d,%d,%d,%d,%d,%d,%d%n",
                            map, opponent, game.harnessWinner == bird ? "PTERODACTYL" : "OPPONENT", ticks,
                            game.damageDealt[0], stunned, vertical, phases[1], phases[2], phases[3], phases[4], phases[6], phases[7]));
                }
            }
            String name = System.getProperty("pterPilotReport", "target/pterodactyl-pilot.csv");
            Files.writeString(Path.of(name), report);
            System.out.print(report);
            StringBuilder moves = new StringBuilder("move,map,uses,hits,damage,kos,selfKos,recoveryFailures\n");
            for (GameplayTelemetry.MoveSnapshot move : game.topTelemetryMovesForBird(BirdGame3.BirdType.PTERODACTYL, 60)) {
                moves.append(String.format(Locale.ROOT, "%s,%s,%d,%d,%d,%d,%d,%d%n", move.moveName(), move.map(),
                        move.uses(), move.hits(), move.damage(), move.kos(), move.selfKos(), move.recoveryFailures()));
            }
            Files.writeString(Path.of(name.replace(".csv", "-moves.csv")), moves);
        } finally { prefs.removeNode(); }
    }
}
