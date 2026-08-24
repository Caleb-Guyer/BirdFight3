package com.example.birdgame3;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DeveloperUiModernizationTest {
    private static final Path GAME_SOURCE = Path.of(
            "src", "main", "java", "com", "example", "birdgame3", "BirdGame3.java");

    @Test
    void classicDeveloperLevelSelectIsEntitlementGatedAndInputAware() throws IOException {
        String source = Files.readString(GAME_SOURCE);
        String intro = methodBody(source, "showClassicEncounterIntro");
        String select = methodBody(source, "showClassicDeveloperLevelSelect");

        assertTrue(intro.contains("developerLevelSelect.setVisible(isClassicDeveloperLevelSelectContext())"));
        assertTrue(select.contains("if (!canUseClassicDeveloperLevelSelect())"));
        assertTrue(select.contains("buildModernMenuPage()"));
        assertTrue(select.contains("buildAdaptivePromptBar("));
        assertTrue(select.contains("bindEscape(scene, back)"));
        assertFalse(select.contains("Only FEATHERDEV-enabled profiles can use Classic Level Select."));
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
