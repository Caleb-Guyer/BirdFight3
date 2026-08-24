package com.example.birdgame3;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ClassicFrontEndUiModernizationTest {
    private static final Path GAME_SOURCE = Path.of(
            "src", "main", "java", "com", "example", "birdgame3", "BirdGame3.java");

    @Test
    void classicBirdSelectUsesSharedChromeAndActiveDevicePrompts() throws IOException {
        String source = Files.readString(GAME_SOURCE);
        String select = methodBody(source, "showClassicBirdSelect");

        assertTrue(select.contains("buildMenuTopStrip("));
        assertTrue(select.contains("buildMenuTitleBanner("));
        assertTrue(select.contains("buildAdaptivePromptBar("));
        assertTrue(select.contains("MenuTheme.panelStyle("));
        assertTrue(select.contains("MenuTheme.buttonStyle("));
        assertFalse(select.contains("frontEndMatchFlow.rulesPreset().title"),
                "Versus rules are not useful information on Classic character select");
        assertFalse(select.contains("Label title = new Label(bossRush ? \"BOSS RUSH\" : \"CLASSIC MODE\")"));
        assertTrue(select.contains("HBox selectionHero"));
        assertTrue(select.contains("new VBox(12, rosterGrid, selectionHero)"));
        assertFalse(select.contains("Label playerOne = new Label(\"PLAYER 1\")"));
        assertFalse(select.contains("HBox lowerPanels"));
        assertFalse(select.contains("statusPanel"));
    }

    @Test
    void bossRushDoesNotAdvertiseClassicContinueCurrency() throws IOException {
        String select = methodBody(Files.readString(GAME_SOURCE), "showClassicBirdSelect");
        assertTrue(select.contains("coinContinue.setVisible(!bossRush)"));
        assertTrue(select.contains("coinContinue.setManaged(!bossRush)"));
        assertTrue(select.contains("bossRush ? \"BOSS GAUNTLET\" : \"COINS  \" + birdCoinBalanceText()"));
    }

    @Test
    void endingGalleryAndContinuePromptRemoveExplanatoryClutter() throws IOException {
        String source = Files.readString(GAME_SOURCE);
        String gallery = methodBody(source, "showClassicEndingGallery");
        String continuePrompt = methodBody(source, "showClassicContinuePrompt");

        assertTrue(gallery.contains("buildMenuTopStrip("));
        assertTrue(gallery.contains("buildAdaptivePromptBar("));
        assertFalse(gallery.contains("ending.crownChoice()"));
        assertFalse(gallery.contains("Every route badge unlocks its bird's alternate Crown epilogue"));
        assertTrue(gallery.contains("BADGE REQUIRED"));
        assertFalse(gallery.contains("ending.routeTitle()"));
        assertTrue(continuePrompt.contains("buildAdaptivePromptBar("));
        assertTrue(continuePrompt.contains("uiFactory.action(\"CONTINUE\""));
        assertFalse(continuePrompt.contains("uiFactory.action(\"CONTINUE  \" + CLASSIC_CONTINUE_BIRD_COIN_COST"));
    }

    @Test
    void classicResultsKeepOnlyRouteProgressAndNextAction() throws IOException {
        String summary = methodBody(Files.readString(GAME_SOURCE), "buildClassicSummaryPanel");

        assertTrue(summary.contains("NEXT  ·  "));
        assertTrue(summary.contains("DIFFICULTY "));
        assertFalse(summary.contains("RESULT: VICTORY"));
        assertFalse(summary.contains("Current: "));
        assertFalse(summary.contains("encounterMapDisplayName(current)"));
        assertFalse(summary.contains("current.mutator.label"));
        assertFalse(summary.contains("next.mutator.label"));
        assertFalse(summary.contains("Bird Coins "));
    }

    private static String methodBody(String source, String methodName) {
        int name = source.indexOf("private void " + methodName + "(");
        if (name < 0) name = source.indexOf("private VBox " + methodName + "(");
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
