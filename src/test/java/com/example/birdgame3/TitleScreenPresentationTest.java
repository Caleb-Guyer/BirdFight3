package com.example.birdgame3;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TitleScreenPresentationTest {
    private static final Path GAME_SOURCE = Path.of(
            "src", "main", "java", "com", "example", "birdgame3", "BirdGame3.java");

    @Test
    void mainTitleUsesAQuietWordmarkAndSingleFullWidthPrompt() throws IOException {
        String source = Files.readString(GAME_SOURCE).replace("\r\n", "\n");
        String title = methodBody(source, "showTitleScreen");

        assertTrue(title.contains("drawBirdFightTitleBackdrop(backdrop.getGraphicsContext2D(), false)"));
        assertTrue(title.contains("buildBirdFightTitleWordmark(false)"));
        assertTrue(title.contains("PRESS ANY BUTTON"));
        assertTrue(title.contains("bindTitleCardAdvance(scene, startButton)"));
        assertTrue(title.contains("bindFixedFrameScale(scene, frame, 0.0)"));
        assertFalse(title.contains("PRESS START"));
        assertFalse(title.contains("buildAdaptivePromptBar("));
        assertFalse(title.contains("redSlash"));
        assertFalse(title.contains("blueSlash"));
    }

    @Test
    void stillSkyGetsItsOwnMinimalAdventureTitleBeforeTheCampaignHub() throws IOException {
        String source = Files.readString(GAME_SOURCE).replace("\r\n", "\n");
        String title = methodBody(source, "showCampaignTitleScreen");
        String hub = methodBody(source, "showHub");

        assertTrue(title.contains("drawBirdFightTitleBackdrop(backdrop.getGraphicsContext2D(), true)"));
        assertTrue(title.contains("buildBirdFightTitleWordmark(true)"));
        assertTrue(title.contains("THE STILL SKY"));
        assertTrue(title.contains("A D V E N T U R E"));
        assertTrue(title.contains("PRESS ANY BUTTON"));
        assertTrue(title.contains("() -> showCampaignHub(stage)"));
        assertTrue(title.contains("bindEscape(scene, () -> showMenu(stage))"));
        assertTrue(hub.contains("() -> showCampaignTitleScreen(stage)"));
    }

    @Test
    void titleAdvanceIsInputNeutralButPreservesBackAndFullscreen() throws IOException {
        String source = Files.readString(GAME_SOURCE).replace("\r\n", "\n");
        String advance = methodBody(source, "bindTitleCardAdvance");

        assertTrue(advance.contains("KeyEvent.KEY_PRESSED"));
        assertTrue(advance.contains("MouseEvent.MOUSE_PRESSED"));
        assertTrue(advance.contains("code == KeyCode.ESCAPE"));
        assertTrue(advance.contains("code == KeyCode.F11"));
        assertTrue(advance.contains("titleCardAdvancing"));
        assertTrue(advance.contains("advanceButton.fire()"));
    }

    private static String methodBody(String source, String methodName) {
        int name = -1;
        for (String returnType : new String[]{"void", "Pane", "String"}) {
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
