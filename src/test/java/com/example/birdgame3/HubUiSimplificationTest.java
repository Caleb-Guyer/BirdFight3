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
        assertEquals(HubPresentationModel.Destination.values().length,
                occurrences(hub, "tagUltimateHubDestination("),
                "every hub destination should drive the center preview");
        assertTrue(hub.contains("setUltimateHubDrawerExpanded("),
                "the utility rail should open as a drawer instead of permanently consuming space");
        assertTrue(hub.contains("AnchorPane.setLeftAnchor(railShell, 1496.0)"));
        assertTrue(hub.contains("railShell.setTranslateX(0.0)"),
                "the utility drawer should start in its compact state");
        assertTrue(hub.contains("if (utilityDrawerExpanded[0])"),
                "Escape should close the open drawer before leaving the hub");
        assertTrue(source.contains("ROOST TIP"));
        assertTrue(source.contains("private String randomHubTip()"));
    }

    @Test
    void compactUtilityRailShowsIconsAndExpandedRowsPutTextAfterThem() throws IOException {
        String source = Files.readString(GAME_SOURCE).replace("\r\n", "\n");
        String railButton = methodBody(source,
                "private Button buildUltimateHubRailButton(String text, double height, Node icon, Runnable action)");
        String drawerState = methodBody(source,
                "private void setUltimateHubDrawerExpanded(StackPane drawer, Button toggle,");

        assertTrue(railButton.contains("new HBox(14, iconFrame, label, spacer)"),
                "expanded utility rows should read icon first, then label");
        assertTrue(drawerState.contains("double targetX = expanded ? -256.0 : 0.0"),
                "the drawer should expand leftward from its visible icon strip");
        assertTrue(drawerState.contains("expanded ? \"UTILITY MENU\" : \"\""),
                "collapsed drawer chrome should not expose clipped text");
    }

    @Test
    void centerMedallionProvidesAnIllustratedPreviewForEveryDestination() throws IOException {
        String source = Files.readString(GAME_SOURCE).replace("\r\n", "\n");
        String updater = methodBody(source,
                "private void updateUltimateHubCenterPreview(Node medallion,");
        String backdrop = methodBody(source,
                "private void drawUltimateHubPreviewBackdrop(Canvas canvas,");
        String layout = methodBody(source,
                "private void layoutUltimateHubPreviewPortraits(Canvas primary, Canvas secondary,");

        for (HubPresentationModel.Destination destination : HubPresentationModel.Destination.values()) {
            assertTrue(updater.contains("case " + destination.name()),
                    "missing portrait treatment for " + destination);
            assertTrue(backdrop.contains("case " + destination.name()),
                    "missing illustrated backdrop for " + destination);
            assertTrue(layout.contains("case " + destination.name()),
                    "missing a distinct portrait composition for " + destination);
        }
        String portrait = methodBody(source,
                "private void drawUltimateHubPreviewPortrait(Canvas canvas, BirdType type)");
        assertTrue(portrait.contains("drawRosterSprite(canvas, type, null, false)"),
                "the preview should feature the game's real bird artwork");
        assertTrue(updater.contains("FadeTransition"),
                "preview changes should be presented cleanly");
        assertTrue(backdrop.contains("StagePreviewRenderer.draw(canvas, stagePicture)"),
                "combat-oriented destinations should incorporate current stage captures");
        assertTrue(backdrop.contains("MapType.BATTLEFIELD"));
        assertTrue(backdrop.contains("MapType.BEACON_CROWN"));
        assertTrue(backdrop.contains("MapType.CITY"));
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
