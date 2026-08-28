package com.example.birdgame3;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VersusFrontEndIntegrationTest {
    private static final Path GAME_SOURCE = Path.of(
            "src", "main", "java", "com", "example", "birdgame3", "BirdGame3.java");

    @Test
    void startupAndHubUseTheExplicitTitleRulesFightersStageJourney() throws IOException {
        String source = source();

        assertTrue(source.contains("showTitleScreen(stage);"), "startup should enter through the title screen");
        assertTrue(source.contains("() -> showVersusRules(stage)"), "Fight should open rules before fighter select");
        assertTrue(source.contains("showVersusRulesets(stage, false);"),
                "local Smash should browse saved rulesets before entering the editor");
        assertTrue(source.contains("frontEndMatchFlow.confirmRules();"));
        assertTrue(source.contains("frontEndMatchFlow.confirmFighters(true);"));
        assertTrue(source.contains("showVersusLoading(stage, choice, standardFightRandomMapPool);"));
        assertTrue(source.contains("showRandomStagePoolEditor(stage, networkLobby);"),
                "the shared rules editor should preserve the local or network return path");
        assertTrue(source.contains("CONFIRM BATTLE"));
        assertTrue(source.contains("START BATTLE"));
    }

    @Test
    void localResultsExposeEveryExpectedContinuationAndControllerNavigation() throws IOException {
        String source = source();

        assertTrue(source.contains("setupKeyboardNavigation(scene);\n        applyConsoleHighlight(scene);\n        if (lanModeActive)"),
                "results navigation must not be limited to network matches");
        assertTrue(source.contains("button(\"REMATCH\""));
        assertTrue(source.contains("button(\"FIGHTERS\""));
        assertTrue(source.contains("button(\"STAGE\""));
        assertTrue(source.contains("button(\"RULES\""));
        assertTrue(source.contains("button(\"HUB\""));
        assertTrue(source.contains("summaryEscapeButton"), "Escape/B needs a deterministic local results destination");
    }

    @Test
    void everyFixedFrontEndScreenUsesTheSharedScaleAndBackContracts() throws IOException {
        String source = source();

        assertTrue(source.contains("bindFixedFrameScale(scene, frame, 0.0);"));
        assertTrue(source.contains("bindEscape(scene, () -> confirmExitGame(stage));"));
        assertTrue(source.contains("bindEscape(scene, backButton::fire);"));
        assertTrue(source.contains("bindEscape(scene, backArrow);"));
    }

    @Test
    void preMatchFlowUsesProgressiveRulesAndDeviceAwarePlayerPrompts() throws IOException {
        String source = source();

        assertTrue(source.contains("GridPane basicOptions = new GridPane();"));
        assertTrue(source.contains("GridPane advancedOptions = new GridPane();"));
        assertTrue(source.contains("CORE RULES · EVERYTHING NEEDED TO START"));
        assertTrue(source.contains("FightSetupInputPresentation.resolve("));
        assertTrue(source.contains("buildFrontEndJourneyPromptBar(FrontEndMatchFlow.Screen.FIGHTERS)"));
        String flowSource = Files.readString(Path.of(
                "src", "main", "java", "com", "example", "birdgame3", "FrontEndMatchFlow.java"));
        assertTrue(flowSource.contains("UiInputPrompts.Command.POINTER, \"MOVE CURSOR\""));
        assertTrue(flowSource.contains("UiInputPrompts.Command.POINTER_SELECT, \"PICK / RELEASE\""));
        assertTrue(flowSource.contains("UiInputPrompts.Command.READY, \"READY\""));
        assertFalse(source.contains("INPUT: KEYBOARD"),
                "fighter slots should name the active device instead of assuming keyboard");
        assertFalse(source.contains("SPECIALS REVEALED AT START"),
                "the roster header should not spend permanent space on four move-guide cards");
    }

    @Test
    void rulesetBrowserAndCreatorAreSeparateSmashStyleScreens() throws IOException {
        String source = source();
        String browser = methodBody(source, "private void showVersusRulesets(Stage stage, boolean networkLobby)");
        String editor = methodBody(source, "private void showVersusRulesEditor(Stage stage, boolean networkLobby)");
        String transition = methodBody(source,
                "private void continueFromVersusRulesSelection(Stage stage, boolean networkLobby)");

        assertTrue(browser.contains("FIGHT  ›  RULESETS"));
        assertTrue(browser.contains("CREATE RULESET"));
        assertTrue(browser.contains("CUSTOMIZE COPY"));
        assertTrue(browser.contains("buildVersusRulesetChoice"));
        assertTrue(browser.contains("buildRulesetPreviewRow"));
        assertTrue(browser.contains("VersusRulesUiModel.quickPresets()"),
                "stamina should be a built-in ruleset, not a hidden custom-only switch");
        assertTrue(browser.contains("VersusRulesUiModel.preview(preset, selected)"));
        assertTrue(browser.contains("VersusRulesUiModel.cycleCustomSlot"));
        assertTrue(browser.contains("setVersusRulesetChoiceTitle(customChoice"));
        assertTrue(browser.contains("continueFromVersusRulesSelection"));
        assertFalse(browser.contains("for (int i = 0; i < VersusRulesLibrary.SLOT_COUNT; i++)"),
                "the browser should expose only the active custom slot instead of three duplicate rows");
        assertFalse(browser.contains("buildRulesetPreviewRow(\"HAZARDS\""));
        assertFalse(browser.contains("buildRulesetPreviewRow(\"ULTIMATES\""));
        assertFalse(browser.contains("poolSummary"));

        String choice = methodBody(source,
                "private Button buildVersusRulesetChoice(String title, String accent, Runnable action)");
        assertFalse(choice.contains("summaryLabel"),
                "ruleset entries should show only their name; details belong in the side panel");
        assertFalse(choice.contains("eyebrowLabel"));

        assertTrue(editor.contains("RULESET CREATOR"));
        assertTrue(editor.contains("uiFactory.action(\"DONE\""));
        assertTrue(editor.contains("\"RULE TYPE\""));
        assertTrue(editor.contains("\"STOCKS / HP\""));
        assertFalse(editor.contains("currentSummary"),
                "the editor should not repeat every setting beneath the actual controls");
        assertFalse(editor.contains("SAVE & RETURN"),
                "changes save as they are made, so the only exit action should be Done");
        assertFalse(editor.contains("VersusRulesPreset.STANDARD"),
                "preset browsing belongs on the browser, not inside the creator");
        assertFalse(editor.contains("CHOOSE FIGHTERS"),
                "the creator should return to rulesets instead of advancing the match");

        assertTrue(transition.contains("frontEndMatchFlow.confirmRules()"));
        assertTrue(transition.contains("catch (RuntimeException | Error failure)"));
        assertTrue(transition.contains("frontEndMatchFlow.beginVersus()"));
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
