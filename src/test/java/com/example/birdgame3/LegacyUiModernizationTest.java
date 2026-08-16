package com.example.birdgame3;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LegacyUiModernizationTest {
    private static final Path GAME_SOURCE = Path.of(
            "src", "main", "java", "com", "example", "birdgame3", "BirdGame3.java");

    @Test
    void highTrafficLegacyScreensUseSharedModernChrome() throws IOException {
        String source = Files.readString(GAME_SOURCE);
        for (String method : new String[]{
                "showLanMenu", "showLanDirectMenu", "showInternetMenu",
                "showInternetHostSetup", "showInternetJoin", "showInternetHelp",
                "showLanJoin", "showLanLobby", "showClassicContinuePrompt",
                "showStoryDialogue", "showUnlockCard", "showAchievementRewardPreviewCard",
                "showTowerDefenseMapSelect", "showModernTournamentDecision"}) {
            String body = methodBody(source, method);
            assertTrue(body.contains("buildModernMenuPage()"), method + " lost the shared modern page shell");
            assertTrue(body.contains("buildMenuTopStrip("), method + " lost the shared top strip");
            assertFalse(body.contains("MenuLayout.buildMenuRoot"), method + " regressed to the legacy stacked layout");
        }
    }

    @Test
    void dynamicNetworkButtonsKeepTheSharedLayeredButtonTheme() throws IOException {
        String source = Files.readString(GAME_SOURCE);
        String lobbyRefresh = methodBody(source, "refreshLanLobbyUI");
        String feedRefresh = methodBody(source, "refreshLanCompanionFeedUI");

        assertTrue(lobbyRefresh.contains("MenuTheme.buttonStyle"));
        assertTrue(feedRefresh.contains("MenuTheme.buttonStyle"));
        assertFalse(lobbyRefresh.contains("-fx-background-color: #00C853"));
        assertFalse(feedRefresh.contains("-fx-background-color: #00897B"));
    }

    @Test
    void fixedStoryScreensRegisterTheirBackButtonsForEscape() throws IOException {
        String source = Files.readString(GAME_SOURCE);
        String installer = methodBody(source, "installFixedCampaignScene");
        assertTrue(installer.contains("bindEscape(scene, escapeButton);"),
                "Fixed Story screens must handle Escape at scene level");

        for (String method : new String[]{
                "showCampaignHub", "showCampaignActMissionSelect",
                "showCampaignMissionBriefing", "showCampaignHandoff",
                "showCampaignGallery", "showLegacyStories"}) {
            String body = methodBody(source, method);
            assertTrue(body.contains("installFixedCampaignScene(stage"),
                    method + " must use the fixed Story scene installer");
            assertTrue(body.contains(", back);"),
                    method + " must register its Back button for Escape");
        }
    }

    private static String methodBody(String source, String methodName) {
        int name = source.indexOf("private void " + methodName + "(");
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
