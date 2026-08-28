package com.example.birdgame3;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SelectionFlowUiSimplificationTest {
    private static final Path GAME_SOURCE = Path.of(
            "src", "main", "java", "com", "example", "birdgame3", "BirdGame3.java");

    @Test
    void fighterSelectUsesOneRosterOnePlayerStripAndContextualControls() throws IOException {
        String source = Files.readString(GAME_SOURCE);
        String method = methodBody(source, "private void showFightSetup(Stage stage)");

        assertTrue(method.contains("SMASH  ›  FIGHTERS"));
        assertFalse(method.contains("SELECT FIGHTERS"), "the breadcrumb is the only screen heading");
        assertFalse(method.contains("SELECT YOUR BIRD"), "the roster must not repeat the screen heading");
        assertFalse(method.contains("actionCardBody"), "ready and player count belong in one bottom action bar");
        assertTrue(method.contains("new HBox(18, playerCountControls, actionSpacer, rosterPrompt, readyBanner)"));
        assertTrue(method.contains("skinButtons[idx].setVisible(skinChoiceAvailable)"));
        assertTrue(method.contains("skinButtons[idx].setManaged(skinChoiceAvailable)"));
        assertTrue(method.contains("inputBtn.setVisible(active && !cpu)"));
        assertTrue(method.contains("inputBtn.setManaged(active && !cpu)"));
        assertTrue(method.contains("buildFrontEndJourneyPromptBar(FrontEndMatchFlow.Screen.FIGHTERS)"));
        assertTrue(method.contains("readyBanner.requestFocus()"),
                "initial confirm input must target the real ready action instead of Back");
        assertTrue(method.contains("readyBanner.getProperties().put(\"uiAction\""));
        assertTrue(method.contains("bindFixedFrameScale(scene, content)"));
    }

    @Test
    void stageSelectKeepsRealImagesAndMovesAllContextToOnePreview() throws IOException {
        String source = Files.readString(GAME_SOURCE);
        String method = methodBody(source, "private void showStageSelect(Stage stage)");
        String tile = methodBody(source,
                "private Button buildStageSelectTile(StageChoice choice, boolean random,");

        assertTrue(method.contains("StagePreviewRenderer.draw(largePreview, choice)"));
        assertTrue(tile.contains("StagePreviewRenderer.draw(image, choice)"));
        assertFalse(method.contains("countChip"));
        assertFalse(method.contains("rulesChip"));
        assertFalse(method.contains("stageLegend"));
        assertFalse(method.contains("previewBase"));
        assertFalse(method.contains("previewDescription"));
        assertFalse(method.contains("NOW SELECTING"));
        assertFalse(tile.contains("badgeText"), "category markers and their legend duplicated the preview");
        assertTrue(method.contains("buildAdaptivePromptBar("));
        assertTrue(method.contains("bindEscape(scene, backArrow)"));
        assertTrue(method.contains("setupKeyboardNavigation(scene)"));
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
