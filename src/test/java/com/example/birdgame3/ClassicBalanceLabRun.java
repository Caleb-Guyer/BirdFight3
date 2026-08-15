package com.example.birdgame3;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

/**
 * Manual Classic-mode balance audit. This class name deliberately avoids the
 * normal Surefire test patterns, so CI does not run hundreds of AI matches.
 *
 * <p>Examples:
 * <pre>
 * .\mvnw.cmd test -Dtest=ClassicBalanceLabRun
 * .\mvnw.cmd test -Dtest=ClassicBalanceLabRun -DclassicBird=EAGLE -DclassicMatches=80
 * .\mvnw.cmd test -Dtest=ClassicBalanceLabRun -DclassicBird=ALL -DclassicMatches=24
 * </pre>
 */
class ClassicBalanceLabRun {

    @Test
    void runClassicBalanceLab() throws Exception {
        BirdGame3.BirdType[] birds = requestedBirds();
        int matches = Math.max(1, Integer.getInteger("classicMatches", 64));
        double difficulty = readDouble("classicDifficulty", BirdGame3.CLASSIC_STARTING_DIFFICULTY);
        int playerCpuLevel = Math.clamp(Integer.getInteger("classicCpu", 5), 1, 9);
        boolean includeObjectives = Boolean.getBoolean("classicObjectives");

        ClassicBalanceLab.Config config = new ClassicBalanceLab.Config(
                birds, matches, 4L * 60 * 60, 20260815L,
                difficulty, playerCpuLevel, includeObjectives);
        long start = System.nanoTime();
        ClassicBalanceLab.Report report = ClassicBalanceLab.run(config,
                line -> System.out.println("[classic-balance] " + line));
        double seconds = (System.nanoTime() - start) / 1e9;

        Path out = Path.of("audit", "classic-balance-report.md");
        Files.createDirectories(out.getParent());
        Files.writeString(out, report.markdown());
        System.out.printf(Locale.ROOT, "[classic-balance] %d matches in %.1f s -> %s%n",
                report.outcomes().size(), seconds, out.toAbsolutePath());
        System.out.println(report.markdown());
    }

    private static BirdGame3.BirdType[] requestedBirds() {
        String requested = System.getProperty("classicBird", "ROADRUNNER").trim();
        if ("ALL".equalsIgnoreCase(requested)) {
            return BirdGame3.BirdType.values();
        }
        return new BirdGame3.BirdType[]{BirdGame3.BirdType.valueOf(requested.toUpperCase(Locale.ROOT))};
    }

    private static double readDouble(String key, double fallback) {
        try {
            return Double.parseDouble(System.getProperty(key, Double.toString(fallback)));
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }
}
