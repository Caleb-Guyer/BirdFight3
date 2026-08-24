package com.example.birdgame3;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NetworkUiModernizationTest {
    private static final Path GAME_SOURCE = Path.of(
            "src", "main", "java", "com", "example", "birdgame3", "BirdGame3.java");

    @Test
    void everyNetworkDecisionScreenUsesActiveDevicePrompts() throws IOException {
        String source = Files.readString(GAME_SOURCE);
        for (String method : new String[]{
                "showLanMenu", "showLanDirectMenu", "showInternetMenu",
                "showInternetHostSetup", "showInternetJoin", "showInternetHelp",
                "showLanJoin", "showLanLobby", "showLanBirdSelect"}) {
            String body = methodBody(source, method);
            assertTrue(body.contains("buildAdaptivePromptBar("),
                    method + " must show only prompts for the active device");
            assertTrue(body.contains("bindEscape(scene"), method + " must provide a predictable Back path");
        }
    }

    @Test
    void networkFighterSelectUsesSharedChromeInsteadOfItsLegacyHandBuiltHeader() throws IOException {
        String source = Files.readString(GAME_SOURCE);
        String select = methodBody(source, "showLanBirdSelect");
        String refresh = methodBody(source, "updateLanBirdSelectButtons");

        assertTrue(select.contains("buildModernMenuPage()"));
        assertTrue(select.contains("buildMenuTopStrip("));
        assertTrue(select.contains("buildMenuTitleBanner(\"SELECT FIGHTER\""));
        assertTrue(select.contains("MenuTheme.panelStyle("));
        assertFalse(select.contains("Label title = new Label(\"NETWORK BATTLE\")"));
        assertFalse(select.contains("-fx-background-color: linear-gradient(to right, #8E0D16"));
        assertTrue(refresh.contains("MenuTheme.buttonStyle("));
        assertFalse(refresh.contains("String baseStyle = \"-fx-background-color"));
    }

    @Test
    void topLevelNetworkCardsKeepExplanationsBrief() throws IOException {
        String source = Files.readString(GAME_SOURCE);
        String menu = methodBody(source, "showLanMenu");
        assertFalse(menu.contains("All players run the same simulation"));
        assertFalse(menu.contains("Use the same game version and a wired connection"));
        assertFalse(menu.contains("public IP or DNS address. Best for trusted players"));
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
