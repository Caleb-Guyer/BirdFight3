package com.example.birdgame3;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.fail;

class ParticleRngAuditTest {
    private static final List<String> SIM_RNG_TOKENS = List.of(
            "SimRng.next()",
            "random.next",
            "bird.random.next",
            "game.random.next"
    );

    @Test
    void particleEmissionPathsDoNotConsumeSimulationRandomness() throws IOException {
        Path sourceRoot = Path.of("src", "main", "java", "com", "example", "birdgame3");
        try (var files = Files.walk(sourceRoot)) {
            for (Path file : files.filter(path -> path.toString().endsWith(".java")).toList()) {
                auditFile(file, Files.readString(file));
            }
        }
    }

    private static void auditFile(Path file, String source) {
        String normalized = source.replace("\r\n", "\n").replace('\r', '\n');
        String[] lines = normalized.split("\n", -1);
        for (int line = 0; line < lines.length; line++) {
            if (lines[line].contains("scaledParticleCount(")
                    || lines[line].contains("scaledParticleBurstCount(")) {
                auditScaledParticleBlock(file, normalized, lines, line);
            }
            if (containsSimRng(lines[line]) && nearbyParticleEmission(lines, line)) {
                fail(file + ":" + (line + 1)
                        + " uses simulation RNG next to a cosmetic particle emission");
            }
        }
    }

    private static void auditScaledParticleBlock(Path file, String source, String[] lines, int line) {
        int offset = 0;
        for (int i = 0; i < line; i++) {
            offset += lines[i].length() + 1;
        }
        int open = source.indexOf('{', offset);
        if (open < 0) {
            return;
        }
        int close = matchingBrace(source, open);
        if (close < 0) {
            fail(file + ":" + (line + 1) + " has an unbalanced particle block");
        }
        String block = source.substring(open, close + 1);
        if (containsSimRng(block)) {
            fail(file + ":" + (line + 1)
                    + " consumes simulation RNG inside a scaled particle block");
        }
    }

    private static int matchingBrace(String source, int open) {
        int depth = 0;
        for (int i = open; i < source.length(); i++) {
            char c = source.charAt(i);
            if (c == '{') depth++;
            if (c == '}' && --depth == 0) return i;
        }
        return -1;
    }

    private static boolean nearbyParticleEmission(String[] lines, int center) {
        int start = Math.max(0, center - 8);
        int end = Math.min(lines.length - 1, center + 8);
        for (int i = start; i <= end; i++) {
            if (lines[i].contains("particles.add") || lines[i].contains("new Particle(")) {
                return true;
            }
        }
        return false;
    }

    private static boolean containsSimRng(String text) {
        return SIM_RNG_TOKENS.stream().anyMatch(text::contains);
    }
}
