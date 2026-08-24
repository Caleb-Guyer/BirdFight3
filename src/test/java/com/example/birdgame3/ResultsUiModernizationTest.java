package com.example.birdgame3;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ResultsUiModernizationTest {
    @Test
    void achievementToastCanMoveAboveTheResultActions() {
        assertEquals(26.0, BirdGame3.achievementToastAxisPosition(
                0.0, 1080.0, 204.0, 26.0, true), 0.001);
        assertEquals(850.0, BirdGame3.achievementToastAxisPosition(
                0.0, 1080.0, 204.0, 26.0, false), 0.001);
        assertEquals(0.0, BirdGame3.achievementToastAxisPosition(
                0.0, 120.0, 204.0, 26.0, true), 0.001);
    }

    @Test
    void resultsKeepOnlyPlacementEssentialsAndIncludeAllSupportedPlayers() throws IOException {
        String source = Files.readString(Path.of(
                "src", "main", "java", "com", "example", "birdgame3", "BirdGame3.java"));
        String rewards = methodBody(source, "private VBox buildCinematicRewardSummary");
        String table = methodBody(source, "private VBox buildCinematicResultsTable");
        String summary = methodBody(source, "void showMatchSummary");
        String achievement = methodBody(source, "public void unlockAchievement");

        assertTrue(rewards.contains("BIRD COINS"));
        assertTrue(rewards.contains("birdCoinBalanceText()"));
        assertFalse(rewards.contains("MATCH HIGHLIGHT"));
        assertFalse(rewards.contains("postMatchTelemetryMoveRows"));
        assertTrue(table.contains("Math.min(6"));
        assertTrue(table.contains("\"KOs\""));
        assertTrue(table.contains("\"FALLS\""));
        assertFalse(table.contains("\"DMG\""));
        assertFalse(table.contains("\"STOCK\""));
        assertFalse(table.contains("\"SCORE\""));
        assertTrue(summary.contains("ACHIEVEMENT_TOAST_TOP_ALIGNED_KEY"));
        assertFalse(achievement.contains("addToKillFeed"),
                "the dedicated achievement toast must not duplicate itself in the event feed");
    }

    private static String methodBody(String source, String signature) {
        int start = source.indexOf(signature);
        assertTrue(start >= 0, "missing " + signature);
        int open = source.indexOf('{', start);
        int depth = 0;
        for (int i = open; i < source.length(); i++) {
            char c = source.charAt(i);
            if (c == '{') depth++;
            if (c == '}' && --depth == 0) return source.substring(open, i + 1);
        }
        throw new AssertionError("unterminated " + signature);
    }
}
