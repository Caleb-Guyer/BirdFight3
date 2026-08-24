package com.example.birdgame3;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PauseUiSimplificationTest {
    private static final Path GAME_SOURCE = Path.of(
            "src", "main", "java", "com", "example", "birdgame3", "BirdGame3.java");

    @Test
    void fighterSelectorCyclesOnlyThroughPresentFighters() {
        List<Integer> present = List.of(0, 2, 3);

        assertEquals(2, BirdGame3.cycledPausePlayerIndex(present, 0, 1));
        assertEquals(0, BirdGame3.cycledPausePlayerIndex(present, 3, 1));
        assertEquals(3, BirdGame3.cycledPausePlayerIndex(present, 0, -1));
        assertEquals(0, BirdGame3.cycledPausePlayerIndex(present, 99, 0));
        assertEquals(-1, BirdGame3.cycledPausePlayerIndex(List.of(), 0, 1));
    }

    @Test
    void pauseUsesOneCompactSelectorAndOnlyContextualTabs() throws IOException {
        String source = Files.readString(GAME_SOURCE);
        String pause = methodBody(source, "private VBox buildInMatchPauseMenu");
        String selector = methodBody(source, "private Node buildPauseFighterSelector");
        String moves = methodBody(source, "private Node buildPauseMoveListPanel");
        String controls = methodBody(source, "private Node buildPauseControlsPanel");

        assertTrue(pause.contains("buildPauseFighterSelector(stage)"));
        assertFalse(pause.contains("buildPauseFighterStrip"));
        assertTrue(pause.contains("if (trainingModeActive)"));
        assertTrue(pause.contains("pauseTabButton(PausePanel.OPTIONS, stage)"));
        assertTrue(pause.contains("buildAdaptivePromptBar("));
        assertTrue(pause.contains("UiInputPrompts.Command.PAUSE"));
        assertTrue(selector.contains("cyclePausePlayer(stage, -1)"));
        assertTrue(selector.contains("cyclePausePlayer(stage, 1)"));
        assertFalse(moves.contains("CORE GAME PLAN"));
        assertFalse(controls.contains("DIRECTIONAL INPUTS"));
        assertFalse(controls.contains("Double-tap a direction to dash"));
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
