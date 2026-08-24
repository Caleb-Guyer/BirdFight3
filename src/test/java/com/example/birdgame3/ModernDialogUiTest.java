package com.example.birdgame3;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ModernDialogUiTest {
    private static final Path MAIN_SOURCE = Path.of("src", "main", "java", "com", "example", "birdgame3");

    @Test
    void modalThemeCoversTypographyFocusInputsAndActions() throws IOException {
        String css = Files.readString(Path.of(
                "src", "main", "resources", "com", "example", "birdgame3", "modern-dialog.css"));

        assertTrue(css.contains(".dialog-pane > .header-panel"));
        assertTrue(css.contains(".dialog-pane .button:focused"));
        assertTrue(css.contains(".dialog-pane .button:default"));
        assertTrue(css.contains(".dialog-pane .text-field"));
    }

    @Test
    void everyGameOwnedJavaFxDialogReceivesTheModernTheme() throws IOException {
        for (String filename : List.of(
                "BirdGame3.java",
                "BirdGame3SaveProfilesUi.java",
                "BirdGame3TournamentSetupUi.java",
                "GameUpdater.java")) {
            String source = Files.readString(MAIN_SOURCE.resolve(filename));
            assertDialogCreationsAreThemed(filename, source, "new Alert(");
            assertDialogCreationsAreThemed(filename, source, "new TextInputDialog(");
        }
    }

    @Test
    void controllerAndWiiRemoteCanOperateModalDialogs() throws IOException {
        String source = Files.readString(MAIN_SOURCE.resolve("BirdGame3.java"));
        String bridge = methodBody(source, "private AnimationTimer createDialogInputBridge");

        assertTrue(bridge.contains("controllerMenuState()"));
        assertTrue(bridge.contains("KeyCode.TAB"));
        assertTrue(bridge.contains("KeyCode.ENTER"));
        assertTrue(bridge.contains("KeyCode.ESCAPE"));
        assertFalse(bridge.contains("SimRng"));
    }

    private static void assertDialogCreationsAreThemed(String filename, String source, String creation) {
        int from = 0;
        while ((from = source.indexOf(creation, from)) >= 0) {
            int show = source.indexOf("showAndWait()", from);
            assertTrue(show >= 0, filename + " has a dialog that is never shown");
            String setup = source.substring(from, show);
            assertTrue(setup.contains("modernizeDialog(") || setup.contains("ModernDialogTheme.apply("),
                    filename + " has an unthemed dialog near source offset " + from);
            from += creation.length();
        }
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
