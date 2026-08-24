package com.example.birdgame3;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InputAwareUiIntegrationTest {
    private static final Path GAME_SOURCE = Path.of(
            "src", "main", "java", "com", "example", "birdgame3", "BirdGame3.java");

    @Test
    void highTrafficFrontEndScreensUseAdaptivePrompts() throws IOException {
        String source = Files.readString(GAME_SOURCE);
        assertTrue(methodBody(source, "showTitleScreen").contains("buildAdaptivePromptBar("));
        assertTrue(methodBody(source, "showHub").contains("buildAdaptivePromptBar("));
        assertTrue(methodBody(source, "showClassicMoreMenu").contains("buildAdaptivePromptBar("));
        assertTrue(methodBody(source, "showStageSelect").contains("buildAdaptivePromptBar("));

        assertFalse(source.contains("ENTER / A  START"));
        assertFalse(source.contains("PRESS ENTER / A"));
        assertFalse(source.contains("ENTER / A SELECT"));
    }

    @Test
    void pauseReferenceShowsOnlyTheSelectedPlayersActiveDevice() throws IOException {
        String source = Files.readString(GAME_SOURCE);
        String moveCard = methodBody(source, "buildPauseMoveCard");
        String controls = methodBody(source, "buildPauseControlsPanel");

        assertTrue(moveCard.contains("playerDeviceProperty(playerIdx)"));
        assertTrue(controls.contains("ACTIVE INPUT"));
        assertTrue(controls.contains("playerDeviceProperty(playerIdx)"));
        assertFalse(moveCard.contains("KEYBOARD  "));
        assertFalse(moveCard.contains("CONTROLLER  "));
        assertFalse(controls.contains("YOUR KEY"));
    }

    @Test
    void backNavigationRemainsInstalledOnTheModernizedScreens() throws IOException {
        String source = Files.readString(GAME_SOURCE);
        assertTrue(methodBody(source, "showTitleScreen").contains("bindEscape(scene"));
        assertTrue(methodBody(source, "showHub").contains("KeyCode.ESCAPE"));
        assertTrue(methodBody(source, "showClassicMoreMenu").contains("bindEscape(scene, back)"));
        assertTrue(methodBody(source, "showStageSelect").contains("bindEscape(scene, backArrow)"));
    }

    private static String methodBody(String source, String methodName) {
        int name = -1;
        for (String returnType : new String[]{"void", "Node", "HBox", "Label"}) {
            name = source.indexOf("private " + returnType + " " + methodName + "(");
            if (name >= 0) break;
        }
        assertTrue(name >= 0, "Missing method " + methodName);
        int open = source.indexOf('{', name);
        assertTrue(open >= 0, "Missing body for " + methodName);
        int depth = 0;
        for (int i = open; i < source.length(); i++) {
            char c = source.charAt(i);
            if (c == '{') depth++;
            if (c == '}' && --depth == 0) return source.substring(open, i + 1);
        }
        throw new AssertionError("Unclosed body for " + methodName);
    }
}
