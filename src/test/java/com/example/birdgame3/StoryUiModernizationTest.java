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
