package com.example.birdgame3;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StoryUiModernizationTest {
    private static final Path GAME_SOURCE = Path.of(
            "src", "main", "java", "com", "example", "birdgame3", "BirdGame3.java");

    @Test
    void legacyEpisodeNavigationUsesSharedChromeAndActiveDevicePrompts() throws IOException {
        String source = Files.readString(GAME_SOURCE);
        for (String method : new String[]{"showLegacyStories", "showEpisodesHub", "showEpisodeChapterSelect"}) {
            String body = methodBody(source, method);
            assertTrue(body.contains("buildMenuTopStrip("), method + " must use the shared top strip");
            assertTrue(body.contains("buildAdaptivePromptBar("), method + " must describe only the active input device");
            assertTrue(body.contains("bindEscape(") || body.contains("installFixedCampaignScene(stage"),
                    method + " must expose consistent back navigation");
        }
        assertFalse(methodBody(source, "showEpisodesHub").contains("Legacy Episodes: self-contained"));
    }

    @Test
    void dialogueAndCampaignScreensAvoidRedundantSystemCopy() throws IOException {
        String source = Files.readString(GAME_SOURCE);
        String dialogue = methodBody(source, "showStoryDialogue");
        String hub = methodBody(source, "showCampaignHub");
        String briefing = methodBody(source, "showCampaignMissionBriefing");

        assertTrue(dialogue.contains("buildAdaptivePromptBar("));
        assertFalse(dialogue.contains("MISSION DISPATCH"));
        assertTrue(hub.contains("buildAdaptivePromptBar("));
        assertFalse(hub.contains("FIGHTER CHOICES CHANGE THE MISSION RESPONSE"));
        assertTrue(briefing.contains("buildAdaptivePromptBar("));
        assertFalse(briefing.contains("CPU \" + stillSkyProgress.difficulty.cpuLevel"));
    }

    @Test
    void campaignSelectionAndGalleryExposeDeviceAwareNavigation() throws IOException {
        String source = Files.readString(GAME_SOURCE);
        assertTrue(methodBody(source, "showCampaignActMissionSelect").contains("buildAdaptivePromptBar("));
        assertTrue(methodBody(source, "showCampaignHandoff").contains("buildAdaptivePromptBar("));

        String gallery = methodBody(source, "showCampaignGallery");
        assertTrue(gallery.contains("buildMenuTopStrip("));
        assertTrue(gallery.contains("buildAdaptivePromptBar("));
        assertFalse(gallery.contains("content.getChildren().addAll(title, count"));
    }

    @Test
    void stillSkyEntryUsesAnAnimatedDestinationDashboardBeforeTheRouteMap() throws IOException {
        String source = Files.readString(GAME_SOURCE);
        String hub = methodBody(source, "showCampaignHub");
        String routeMap = methodBody(source, "showCampaignRouteMap");

        assertTrue(hub.contains("updateAdventureHubHeroArt("));
        assertTrue(hub.contains("installHubSelectionPreview("));
        assertTrue(hub.contains("playFightMenuEntrance("));
        assertTrue(hub.contains("animateFightMenuExit("));
        assertTrue(hub.contains("buildAdaptivePromptBar("));
        assertFalse(hub.contains("drawStillSkyActRoute("));

        assertTrue(routeMap.contains("drawStillSkyActRoute("));
        assertTrue(routeMap.contains("() -> showCampaignHub(stage)"));
    }

    @Test
    void legacyAdventureFlowUsesFocusedFixedWidthPresentation() throws IOException {
        String source = Files.readString(GAME_SOURCE);
        String hub = methodBody(source, "showAdventureHub");
        String battles = methodBody(source, "showAdventureBattleSelect");
        String fighters = methodBody(source, "showAdventureBirdSelect");

        assertTrue(hub.contains("buildAdaptivePromptBar("));
        assertTrue(hub.contains("bindEscape(scene, menuBtn)"));
        assertFalse(hub.contains("Adventure Roster:"));
        assertFalse(hub.contains("Completion reward:"));
        assertFalse(hub.contains("selectedAdventureRoute.summary"));

        assertTrue(battles.contains("buildMenuTopStrip("));
        assertTrue(battles.contains("GridPane battles"));
        assertTrue(battles.contains("buildAdaptivePromptBar("));
        assertTrue(battles.contains("installFixedCampaignScene("));
        assertFalse(battles.contains("setPrefSize(1500, 96)"));

        assertTrue(fighters.contains("lockRegionSize(root, 1600, 950)"));
        assertTrue(fighters.contains("double paneW = 930"));
        assertTrue(fighters.contains("double rightCardWidth = 500"));
        assertTrue(fighters.contains("StagePreviewRenderer.draw(stagePreview"));
        assertTrue(fighters.contains("buildAdaptivePromptBar("));
        assertTrue(fighters.contains("installFixedCampaignScene("));
        assertFalse(fighters.contains("battle.briefing"));
        assertFalse(fighters.contains("Adventure roster:"));
        assertFalse(fighters.contains("double paneW = 1100"));
    }

    private static String methodBody(String source, String methodName) {
        int name = source.indexOf("private void " + methodName + "(");
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
