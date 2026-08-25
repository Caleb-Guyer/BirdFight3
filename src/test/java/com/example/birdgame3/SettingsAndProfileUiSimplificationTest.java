package com.example.birdgame3;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SettingsAndProfileUiSimplificationTest {
    private static final Path GAME_SOURCE = Path.of(
            "src", "main", "java", "com", "example", "birdgame3", "BirdGame3.java");

    @Test
    void settingsUseAFocusedCategoryDashboardAndDetailPages() throws IOException {
        String source = Files.readString(GAME_SOURCE);
        String dashboard = methodBody(source, "showMainSettings");
        String settings = methodBody(source, "showSettingsDetail");
        String volume = methodBody(source, "buildVolumeSettingsRow");

        assertTrue(dashboard.contains("SettingsSection.values()"));
        assertTrue(dashboard.contains("buildSettingsCategoryCard"));
        assertTrue(dashboard.contains("CHOOSE A CATEGORY"));
        assertFalse(dashboard.contains("VBox tabs = new VBox"));
        assertTrue(settings.contains("contentHolder.getChildren().setAll"));
        assertTrue(settings.contains("ControlSettingsPresentation.pageFor(activeInput.device())"));
        assertTrue(settings.contains("buildBrightnessSettingsRow()"));
        assertTrue(settings.contains("showSettingsMoveGuideRoster(stage)"));
        assertFalse(settings.contains("Camera jolts on big hits."));
        assertFalse(settings.contains("Update history and optional unlock codes"));
        assertFalse(settings.contains("Ready to edit controls."));
        assertFalse(volume.contains("descriptionLabel"));
        assertFalse(volume.contains("setShowTickLabels(true)"));
    }

    @Test
    void profilePickerKeepsOnlyMeaningfulStateAndHeaderActions() throws IOException {
        String source = Files.readString(GAME_SOURCE);
        String picker = methodBody(source, "showProfileManager");
        String card = methodBody(source, "buildProfileCard");

        assertTrue(picker.contains("HBox topActions = new HBox(12, create, saveTools)"));
        assertFalse(picker.contains("CHOOSE A SAVE"));
        assertFalse(picker.contains("profiles.size() +"));
        assertFalse(card.contains("READY"));
        assertFalse(card.contains("SELECTED"));
        assertTrue(card.contains("state.setVisible(active)"));
        assertTrue(card.contains("activate.setVisible(!active)"));
    }

    private static String methodBody(String source, String methodName) {
        int name = -1;
        for (String returnType : new String[]{"void", "VBox", "Node", "HBox", "Button"}) {
            name = source.indexOf("private " + returnType + " " + methodName + "(");
            if (name >= 0) break;
        }
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
