package com.example.birdgame3;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProfileUiModernizationTest {
    private static final Path GAME_SOURCE = Path.of(
            "src", "main", "java", "com", "example", "birdgame3", "BirdGame3.java");

    @Test
    void profilePickerKeepsAdvancedAndDestructiveToolsBehindIntentionalScreens() throws IOException {
        String source = Files.readString(GAME_SOURCE);
        String picker = methodBody(source, "showProfileManager");
        String card = methodBody(source, "buildProfileCard");

        assertTrue(picker.contains("buildModernMenuPage()"));
        assertTrue(picker.contains("showProfileSaveTools(stage)"));
        assertTrue(picker.contains("buildAdaptivePromptBar("));
        assertFalse(picker.contains("BACK UP NOW"));
        assertFalse(picker.contains("EXPORT SAVE"));
        assertFalse(picker.contains("IMPORT SAVE"));
        assertFalse(card.contains("RESET"));
        assertFalse(card.contains("DELETE"));
        assertTrue(card.contains("MANAGE"));
        assertTrue(card.contains("showProfileDetails(stage, profile.id())"));
    }

    @Test
    void profileDetailsSaveToolsAndBackupsUseSharedChromeAndAdaptivePrompts() throws IOException {
        String source = Files.readString(GAME_SOURCE);
        for (String method : new String[]{"showProfileDetails", "showProfileSaveTools", "showBackupManager"}) {
            String body = methodBody(source, method);
            assertTrue(body.contains("buildModernMenuPage()"), method + " must use shared page chrome");
            assertTrue(body.contains("buildMenuTopStrip("), method + " must use the shared top strip");
            assertTrue(body.contains("buildAdaptivePromptBar("), method + " must expose active-device prompts");
            assertTrue(body.contains("bindEscape(scene, back)"), method + " must support Escape/Back");
        }
        assertTrue(methodBody(source, "showProfileDetails").contains("RESET PROGRESS"));
        assertTrue(methodBody(source, "showProfileSaveTools").contains("IMPORT SAVE"));
        assertFalse(methodBody(source, "buildBackupCard").contains("Source profile id"));
    }

    private static String methodBody(String source, String methodName) {
        int name = -1;
        for (String returnType : new String[]{"void", "VBox"}) {
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
