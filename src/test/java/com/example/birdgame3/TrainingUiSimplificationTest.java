package com.example.birdgame3;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TrainingUiSimplificationTest {
    private static final Path GAME_SOURCE = Path.of(
            "src", "main", "java", "com", "example", "birdgame3", "BirdGame3.java");

    @Test
    void trainingUsesSharedChromeAndDeviceAwareHelp() throws IOException {
        String source = Files.readString(GAME_SOURCE);
        String training = methodBody(source, "showTrainingSetup");

        assertTrue(training.contains("buildModernMenuPage()"));
        assertTrue(training.contains("buildMenuTopStrip("));
        assertTrue(training.contains("buildMenuTitleBanner(\"TRAINING\""));
        assertTrue(training.contains("uiInputTracker.activeInputProperty()"));
        assertTrue(training.contains("buildAdaptivePromptBar("));
        assertFalse(training.contains("Label title = new Label(\"TRAINING MODE\")"));
        assertFalse(training.contains("Opponent has infinite health"));
    }

    @Test
    void academyFooterUsesActiveDevicePromptsAndOneEscapeBinding() throws IOException {
        String source = Files.readString(GAME_SOURCE);
        String academy = methodBody(source, "showGuidedTutorialHub");

        assertTrue(academy.contains("buildAdaptivePromptBar("));
        assertFalse(academy.contains("Character-specific move drills remain available"));
        assertEquals(1, occurrences(academy, "bindEscape(scene"));
    }

    private static int occurrences(String source, String needle) {
        int count = 0;
        for (int at = 0; (at = source.indexOf(needle, at)) >= 0; at += needle.length()) count++;
        return count;
    }

    private static String methodBody(String source, String methodName) {
        int name = source.indexOf("private void " + methodName + "(");
        assertTrue(name >= 0, "Missing method " + methodName);
        int open = source.indexOf('{', name);
        int depth = 0;
        for (int i = open; i < source.length(); i++) {
            char c = source.charAt(i);
            if (c == '{') depth++;
            if (c == '}' && --depth == 0) return source.substring(open, i + 1);
        }
        throw new AssertionError("Unclosed body for " + methodName);
    }
}
