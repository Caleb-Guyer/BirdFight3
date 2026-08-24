package com.example.birdgame3;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FighterSelectUiCrashRegressionTest {
    private static final Path GAME_SOURCE = Path.of(
            "src", "main", "java", "com", "example", "birdgame3", "BirdGame3.java");

    @Test
    void noEllipsisPresentationDoesNotOverwriteBoundButtonStyles() throws IOException {
        String source = Files.readString(GAME_SOURCE);
        String helper = methodBody(source, "private void applyNoEllipsis(Button b)");

        assertTrue(helper.contains("setTextOverrun(OverrunStyle.CLIP)"));
        assertTrue(helper.contains("setEllipsisString(\"\")"));
        assertFalse(helper.contains("setStyle("),
                "JavaFX throws when a shared presentation helper writes a bound style property");
    }

    @Test
    void fighterSelectCanBindLiveDeviceStylesAndRollsBackFailedTransitions() throws IOException {
        String source = Files.readString(GAME_SOURCE);
        String fightSetup = methodBody(source, "private void showFightSetup(Stage stage)");
        String sceneFitter = methodBody(source, "private void fitSceneButtons(Node node)");
        String buttonFitter = methodBody(source, "private void fitButtonText(Button b)");
        String rules = methodBody(source, "private void showVersusRulesEditor(Stage stage, boolean networkLobby)");

        assertTrue(fightSetup.contains("inputBtn.textProperty().bind"));
        assertTrue(fightSetup.contains("inputBtn.styleProperty().bind"));
        assertTrue(fightSetup.contains("setupKeyboardNavigation(scene)"));
        assertTrue(sceneFitter.contains("b.textProperty().isBound() || b.styleProperty().isBound()"));
        assertTrue(sceneFitter.contains("applyNoEllipsis(b)"));
        assertTrue(buttonFitter.contains("b.textProperty().isBound() || b.styleProperty().isBound()"),
                "the text fitter must never rewrite live-bound fighter input labels");
        assertTrue(rules.contains("catch (RuntimeException | Error failure)"));
        assertTrue(rules.contains("frontEndMatchFlow.beginVersus()"));
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
