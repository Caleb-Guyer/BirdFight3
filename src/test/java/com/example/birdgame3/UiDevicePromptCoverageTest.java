package com.example.birdgame3;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UiDevicePromptCoverageTest {
    private static final Path GAME_SOURCE = Path.of(
            "src", "main", "java", "com", "example", "birdgame3", "BirdGame3.java");

    @Test
    void pauseHeaderUsesTheActiveInputInsteadOfXboxOnlyGlyphs() throws IOException {
        String source = Files.readString(GAME_SOURCE);
        String pause = methodBody(source, "private VBox buildInMatchPauseMenu");

        assertTrue(pause.contains("buildAdaptivePromptBar"));
        assertTrue(pause.contains("UiInputPrompts.Command.PAUSE"));
        assertFalse(source.contains("controllerGlyphHint("));
    }

    @Test
    void replayAndTrailerPromptsFollowTheDeviceTheyActuallyAccept() throws IOException {
        String source = Files.readString(GAME_SOURCE);
        String replayOverlay = methodBody(source, "private void drawReplayOverlay");
        String replayInput = methodBody(source, "private void pollWiimoteGameplayInputs");
        String trailer = methodBody(source, "private void showUpdateTrailer");

        assertTrue(replayOverlay.contains("UiInputPrompts.inputFor"));
        assertFalse(replayOverlay.contains("ESC TO EXIT"));
        assertTrue(replayInput.contains("replayControls.menuBack() || replayControls.menuPause()"));
        assertTrue(trailer.contains("uiInputTracker.activeInputProperty()"));
        assertTrue(trailer.contains("UiInputPrompts.Command.SELECT, \"REPLAY\""));
        assertFalse(trailer.contains("ESC BACK   R / SPACE / ENTER REPLAY"));
    }

    @Test
    void keyboardBindingHelpDoesNotDuplicateAStaleEscapeInstruction() throws IOException {
        String source = Files.readString(GAME_SOURCE);

        assertTrue(source.contains("press a new keyboard key"));
        assertFalse(source.contains("ESC cancels a pending rebind"));
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
