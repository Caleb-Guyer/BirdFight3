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
        assertTrue(source.contains("UiInputPrompts.Command.SELECT, \"PICK / READY\""));
        assertFalse(source.contains("INPUT: KEYBOARD"),
                "fighter slots should name the active device instead of assuming keyboard");
        assertFalse(source.contains("SPECIALS REVEALED AT START"),
                "the roster header should not spend permanent space on four move-guide cards");
    }

    private static String source() throws IOException {
        return Files.readString(GAME_SOURCE).replace("\r\n", "\n");
    }
}
