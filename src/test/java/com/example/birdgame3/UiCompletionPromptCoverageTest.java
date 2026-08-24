package com.example.birdgame3;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UiCompletionPromptCoverageTest {
    private static final Path GAME_SOURCE = Path.of(
            "src", "main", "java", "com", "example", "birdgame3", "BirdGame3.java");

    @Test
    void dialogueAndRewardCardsExplainTheirAcceptedDeviceInput() throws IOException {
        String source = Files.readString(GAME_SOURCE);
        String dialogue = methodBody(source, "private void showAdventureDialogue");
        String reward = methodBody(source, "private void showAchievementRewardPreviewCard");

        assertTrue(dialogue.contains("buildAdaptivePromptBar"));
        assertTrue(dialogue.contains("UiInputPrompts.Command.SELECT, \"CONTINUE\""));
        assertTrue(reward.contains("buildAdaptivePromptBar"));
        assertTrue(reward.contains("UiInputPrompts.Command.SELECT, \"CONTINUE\""));
        assertFalse(dialogue.contains("PRESS ENTER"));
        assertFalse(reward.contains("PRESS ENTER"));
    }

    @Test
    void allPrimaryGameScenesUseTheFullscreenSafeInstaller() throws IOException {
        String source = Files.readString(GAME_SOURCE);
        int directSetSceneCalls = occurrences(source, "stage.setScene(scene);");

        assertTrue(source.contains("private Scene setScenePreservingFullscreen"));
        assertTrue(source.contains("ensureSceneAutoScaled(scene)"));
        assertTrue(source.contains("seedSceneTargetSize(stage, scene)"));
        assertTrue(directSetSceneCalls == 1,
                "only the fullscreen-safe scene installer may call stage.setScene directly");
    }

    private static int occurrences(String text, String needle) {
        int count = 0;
        int from = 0;
        while ((from = text.indexOf(needle, from)) >= 0) {
            count++;
            from += needle.length();
        }
        return count;
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
