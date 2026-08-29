package com.example.birdgame3;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * Manual all-level Adventure balance audit. The class name deliberately avoids
 * normal Surefire patterns so CI does not run thousands of full AI missions.
 *
 * <p>Examples:
 * <pre>
 * .\mvnw.cmd test -Dtest=AdventureBalanceLabRun
 * .\mvnw.cmd test -Dtest=AdventureBalanceLabRun -DadventureMission=dead_air -DadventureMatches=64
 * .\mvnw.cmd test -Dtest=AdventureBalanceLabRun -DadventureDifficulty=HARD -DadventureEnforceTargets=true
 * </pre>
 */
class AdventureBalanceLabRun {

    @Test
    void runAdventureBalanceLab() throws Exception {
        List<String> missionIds = requestedMissionIds();
        List<StoryCampaign.Difficulty> difficulties = requestedDifficulties();
        int matches = Math.max(1, Integer.getInteger("adventureMatches", 24));
        int playerCpuLevel = Math.clamp(Integer.getInteger("adventureCpu", 5), 1, 9);

        AdventureBalanceLab.Config config = new AdventureBalanceLab.Config(
                missionIds, difficulties, matches, 6L * 60L * 60L,
                20260828L, playerCpuLevel);
        long start = System.nanoTime();
        AdventureBalanceLab.Report report = AdventureBalanceLab.run(config,
                line -> System.out.println("[adventure-balance] " + line));
        double seconds = (System.nanoTime() - start) / 1e9;

        Path out = Path.of(System.getProperty(
                "adventureReportPath", "audit/adventure-balance-report.md"));
        Path parent = out.getParent();
        if (parent != null) Files.createDirectories(parent);
        Files.writeString(out, report.markdown());
        System.out.printf(Locale.ROOT,
                "[adventure-balance] %d matches in %.1f s; %d target misses; %d cutoffs -> %s%n",
                report.outcomes().size(), seconds, report.targetMisses().size(),
                report.cutoffCount(), out.toAbsolutePath());
        System.out.println(report.markdown());

        if (report.cutoffCount() > 0) {
            throw new AssertionError("Adventure lab found " + report.cutoffCount()
                    + " harness cutoffs; see " + out.toAbsolutePath());
        }
        if (Boolean.getBoolean("adventureEnforceTargets") && !report.targetMisses().isEmpty()) {
            throw new AssertionError("Adventure lab found " + report.targetMisses().size()
                    + " mission/difficulty results outside their target bands; see "
                    + out.toAbsolutePath());
        }
    }

    private static List<String> requestedMissionIds() {
        String requested = System.getProperty("adventureMission", "ALL").trim();
        if (requested.isEmpty() || "ALL".equalsIgnoreCase(requested)) return List.of();
        return Arrays.stream(requested.split(","))
                .map(String::trim)
                .filter(value -> !value.isEmpty())
                .toList();
    }

    private static List<StoryCampaign.Difficulty> requestedDifficulties() {
        String requested = System.getProperty("adventureDifficulty", "ALL").trim();
        if (requested.isEmpty() || "ALL".equalsIgnoreCase(requested)) {
            return List.of(StoryCampaign.Difficulty.values());
        }
        return Arrays.stream(requested.split(","))
                .map(String::trim)
                .filter(value -> !value.isEmpty())
                .map(value -> StoryCampaign.Difficulty.valueOf(value.toUpperCase(Locale.ROOT)))
                .toList();
    }
}
