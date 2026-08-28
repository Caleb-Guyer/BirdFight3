package com.example.birdgame3;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FrontEndJourneyUiIntegrationTest {
    private static final Path GAME_SOURCE = Path.of(
            "src", "main", "java", "com", "example", "birdgame3", "BirdGame3.java");

    @Test
    void firstLaunchExplainsThePlayableAcademyAndUsesAdaptiveControls() throws IOException {
        String method = methodBody(source(), "private void showFirstLaunchTutorialPrompt(Stage stage)");

        for (String lesson : new String[]{
                "MOVEMENT", "NORMAL ATTACKS", "KNOCKOUTS", "RECOVERY",
                "DEFENSE + GRABS", "SPECIALS", "ULTIMATES", "PRACTICE MATCH"
        }) {
            assertTrue(method.contains(lesson), "first launch must advertise " + lesson);
        }
        assertTrue(method.contains("buildAdaptivePromptBar("));
        assertTrue(method.contains("UiInputPrompts.Command.BACK, \"NOT NOW\""));
        assertTrue(method.contains("animateFrontEndJourneyExit(frame, true"));
        assertTrue(method.contains("animateFrontEndJourneyExit(frame, false"));
        assertTrue(method.contains("playFrontEndJourneyEntrance(frame)"));
    }

    @Test
    void everyLocalMatchStepUsesTheSharedJourneyPromptsAndTransitions() throws IOException {
        String source = source();
        String rules = methodBody(source, "private void showVersusRulesets(Stage stage, boolean networkLobby)");
        String fighters = methodBody(source, "private void showFightSetup(Stage stage)");
        String stages = methodBody(source, "private void showStageSelect(Stage stage)");
        String loading = methodBody(source, "private void showVersusLoading(Stage stage, StageChoice choice, StageRandomPool randomPool)");
        String results = methodBody(source, "void showMatchSummary(Stage stage, Bird winner)");

        assertTrue(rules.contains("buildFrontEndJourneyPromptBar(FrontEndMatchFlow.Screen.RULES)"));
        assertTrue(fighters.contains("buildFrontEndJourneyPromptBar(FrontEndMatchFlow.Screen.FIGHTERS)"));
        assertTrue(stages.contains("buildFrontEndJourneyPromptBar(FrontEndMatchFlow.Screen.STAGES)"));
        assertTrue(loading.contains("buildFrontEndJourneyPromptBar(FrontEndMatchFlow.Screen.LOADING)"));
        assertTrue(results.contains("buildFrontEndJourneyPromptBar(FrontEndMatchFlow.Screen.RESULTS)"));

        for (String method : new String[]{rules, fighters, stages, loading}) {
            assertTrue(method.contains("playFrontEndJourneyEntrance("));
            assertTrue(method.contains("animateFrontEndJourneyExit("));
        }
        assertTrue(results.contains("animateFrontEndJourneyExit(frame, back"));
        assertFalse(loading.contains("REVIEW THE FIGHTERS, STAGE, AND RULES BEFORE STARTING"),
                "loading must show the active device's real confirm/back controls");
    }

    @Test
    void sharedButtonFittingHasAnEmergencyFloorAndNeverAssumesExtraWidth() throws IOException {
        String fit = methodBody(source(), "private void fitButtonText(Button b)");

        assertTrue(fit.contains("double emergencyFloor = Math.min(6, visualFloor)"));
        assertTrue(fit.contains("Math.max(1, width - 40)"));
        assertTrue(fit.contains("Math.max(1, height - 16)"));
        assertTrue(fit.contains("size -= size > visualFloor ? 2 : 0.5"));
        assertFalse(fit.contains("Math.max(110, width - 40)"));
    }

    private static String source() throws IOException {
        return Files.readString(GAME_SOURCE).replace("\r\n", "\n");
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
