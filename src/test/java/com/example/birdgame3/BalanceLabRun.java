package com.example.birdgame3;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Manual entry point for the full balance lab — NOT part of the CI suite
 * (the class name deliberately avoids surefire's Test* patterns).
 *
 * Run it with:
 *   .\mvnw.cmd test -Dtest=BalanceLabRun
 *
 * Plays every roster pairing (both sides, seeded) and writes the win-rate
 * tier list and matchup matrix to audit/balance-report.md.
 */
class BalanceLabRun {

    @Test
    void runFullBalanceLab() throws Exception {
        long start = System.nanoTime();
        BalanceLab.Report report = BalanceLab.run(
                BalanceLab.Config.defaults(),
                line -> System.out.println("[balance-lab] " + line));
        double seconds = (System.nanoTime() - start) / 1e9;

        Path out = Path.of("audit", "balance-report.md");
        Files.createDirectories(out.getParent());
        Files.writeString(out, report.markdown());

        System.out.printf("[balance-lab] %d matches in %.1f s -> %s%n",
                report.outcomes().size(), seconds, out.toAbsolutePath());
        System.out.println(report.markdown());
    }
}
