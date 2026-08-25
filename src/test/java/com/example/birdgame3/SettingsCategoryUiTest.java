package com.example.birdgame3;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SettingsCategoryUiTest {
    private static final Path GAME_SOURCE = Path.of(
            "src", "main", "java", "com", "example", "birdgame3", "BirdGame3.java");

    @Test
    void optionsDashboardExposesSixFocusedBirdFightCategories() throws IOException {
        String source = Files.readString(GAME_SOURCE).replace("\r\n", "\n");
        String categories = enumBody(source, "SettingsSection");
        String dashboard = methodBody(source, "showMainSettings");

        assertTrue(categories.contains("GAME("));
        assertTrue(categories.contains("CONTROLS("));
        assertTrue(categories.contains("AUDIO("));
        assertTrue(categories.contains("DISPLAY("));
        assertTrue(categories.contains("NETWORK("));
        assertTrue(categories.contains("HELP("));
        assertTrue(dashboard.contains("SettingsSection.values()"));
        assertTrue(dashboard.contains("playFightMenuEntrance(frame, categoryCards)"));
        assertTrue(dashboard.contains("bindEscape(scene, back)"));
        assertTrue(dashboard.contains("bindFixedFrameScale(scene, frame, 0.0)"));
        assertTrue(source.contains("private void showSettingsLanguage(Stage stage)"));
        assertTrue(source.contains("Bird Fight 3 is currently authored in English"));
    }

    @Test
    void displayPageUsesAPersistedLiveBrightnessPreview() throws IOException {
        String source = Files.readString(GAME_SOURCE).replace("\r\n", "\n");
        String brightness = methodBody(source, "buildBrightnessSettingsRow");
        String apply = methodBody(source, "applySceneBrightness");

        assertTrue(brightness.contains("StagePreviewRenderer.draw(preview"));
        assertTrue(brightness.contains("new Slider(0, 100"));
        assertTrue(brightness.contains("setDisplayBrightness"));
        assertTrue(brightness.contains("saveAchievements()"));
        assertTrue(apply.contains("ColorAdjust"));
        assertTrue(apply.contains("birdFightDisplayBrightness"));
    }

    @Test
    void moveGuideCoversTheRosterAndUsesRealMoveDataAndLiveBindings() throws IOException {
        String source = Files.readString(GAME_SOURCE).replace("\r\n", "\n");
        String roster = methodBody(source, "showSettingsMoveGuideRoster");
        String guide = methodBody(source, "showSettingsMoveGuide");
        String input = methodBody(source, "buildSettingsMoveInputLabel");
        String art = methodBody(source, "buildSettingsMoveArt");

        assertTrue(roster.contains("BirdType.values()"));
        assertTrue(roster.contains("buildSettingsMoveGuideRosterCard"));
        assertTrue(roster.contains("bindEscape(scene, back)"));
        assertTrue(guide.contains("FighterMoveGuide.forBird(type)"));
        assertTrue(guide.contains("guide.moves()"));
        assertTrue(guide.contains("guide.ultimateName()"));
        assertTrue(guide.contains("buildSettingsMoveArt"));
        assertTrue(input.contains("playerDeviceProperty(playerIdx)"));
        assertTrue(art.contains("preview.draw(g)"));
        assertFalse(art.contains(".snapshot("),
                "move-guide art should not depend on the RTTexture snapshot path");
        assertFalse(guide.contains("Mario"));
        assertFalse(guide.contains("Final Smash"));
    }

    private static String enumBody(String source, String enumName) {
        int name = source.indexOf("private enum " + enumName);
        assertTrue(name >= 0, "Missing enum " + enumName);
        return balancedBody(source, name);
    }

    private static String methodBody(String source, String methodName) {
        int name = -1;
        for (String returnType : new String[]{"void", "Node", "HBox", "VBox", "Label", "Canvas", "Button"}) {
            name = source.indexOf("private " + returnType + " " + methodName + "(");
            if (name >= 0) break;
        }
        assertTrue(name >= 0, "Missing method " + methodName);
        return balancedBody(source, name);
    }

    private static String balancedBody(String source, int declaration) {
        int open = source.indexOf('{', declaration);
        assertTrue(open >= 0, "Missing body");
        int depth = 0;
        for (int i = open; i < source.length(); i++) {
            char c = source.charAt(i);
            if (c == '{') depth++;
            if (c == '}' && --depth == 0) return source.substring(open, i + 1);
        }
        throw new AssertionError("Unclosed body");
    }
}
