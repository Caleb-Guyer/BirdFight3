package com.example.birdgame3;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HubUiSimplificationTest {
    private static final Path GAME_SOURCE = Path.of(
            "src", "main", "java", "com", "example", "birdgame3", "BirdGame3.java");

    @Test
    void primaryHubKeepsModesLargeAndRetainsItsCompactTopTipStrip() throws IOException {
        String source = Files.readString(GAME_SOURCE).replace("\r\n", "\n");
        String hub = methodBody(source, "private void showHub(Stage stage)");

        assertEquals(5, occurrences(hub, "buildUltimateHubMainTileButton("),
                "the hub should keep a small set of large primary destinations");
        assertEquals(4, occurrences(hub, "buildUltimateHubRailButton("),
                "utilities should remain on the compact side rail");
        assertTrue(hub.contains("HubPresentationModel.IDLE_TITLE"));
        assertTrue(hub.contains("HubPresentationModel.Destination.FIGHT.description()"));
        assertTrue(hub.contains("buildAdaptivePromptBar("));
        assertTrue(hub.contains("bindFixedFrameScale(scene, frame, 0.0);"));
        assertTrue(hub.contains("buildUltimateHubTipPanel(randomHubTip())"));
        assertTrue(source.contains("ROOST TIP"));
        assertTrue(source.contains("private String randomHubTip()"));
    }

    @Test
    void gamesDashboardUsesTheSameConciseContextContract() throws IOException {
        String source = Files.readString(GAME_SOURCE).replace("\r\n", "\n");
        String games = methodBody(source, "private void showClassicMoreMenu(Stage stage)");

        assertEquals(7, occurrences(games, "registerHubInteractiveNode("));
        assertTrue(games.contains("HubPresentationModel.ExtraMode.CLASSIC.description()"));
        assertTrue(games.contains("HubPresentationModel.ExtraMode.TRAINING.description()"));
        assertFalse(games.contains("Pick a route"));
        assertFalse(games.contains("route ladder with branching encounters"));
    }

    private static int occurrences(String value, String token) {
        int count = 0;
        int offset = 0;
        while ((offset = value.indexOf(token, offset)) >= 0) {
            count++;
            offset += token.length();
        }
        return count;
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
