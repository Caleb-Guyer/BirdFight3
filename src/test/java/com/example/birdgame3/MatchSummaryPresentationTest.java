package com.example.birdgame3;

import javafx.scene.Group;
import javafx.scene.effect.DropShadow;
import javafx.scene.shape.Rectangle;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import java.util.prefs.Preferences;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MatchSummaryPresentationTest {
    @Test
    void campaignFailureUsesDefeatMusicWhileSuccessUsesVictoryMusic() {
        assertEquals(BirdGame3.MatchSummaryMusicCue.DEFEAT,
                BirdGame3.matchSummaryMusicCue(true, false));
        assertEquals(BirdGame3.MatchSummaryMusicCue.VICTORY,
                BirdGame3.matchSummaryMusicCue(true, true));
        assertEquals(BirdGame3.MatchSummaryMusicCue.VICTORY,
                BirdGame3.matchSummaryMusicCue(false, false));
        assertNotNull(BirdGame3.class.getResource("/sounds/music-defeat.wav"));
    }

    @Test
    void cinematicSummaryScalesFromItsActualDesignResolution() {
        assertEquals(1.0,
                BirdGame3.fixedFrameScale(1920.0, 1080.0, 1920.0, 1080.0),
                0.0001);
        assertEquals(2048.0 / 1920.0,
                BirdGame3.fixedFrameScale(2048.0, 1280.0, 1920.0, 1080.0),
                0.0001);
        assertEquals(1280.0 / 1920.0,
                BirdGame3.fixedFrameScale(1280.0, 720.0, 1920.0, 1080.0),
                0.0001);
    }

    @Test
    void cinematicSummaryEffectCleanupIncludesNestedNodes() {
        Rectangle nested = new Rectangle(120.0, 40.0);
        nested.setEffect(new DropShadow());
        Group inner = new Group(nested);
        inner.setEffect(new DropShadow());
        Group root = new Group(inner);
        root.setEffect(new DropShadow());
        nested.setStyle("-fx-fill: red; -fx-effect: dropshadow(gaussian, black, 14, 0.2, 0, 5);");

        BirdGame3.clearNodeEffects(root);

        assertNull(root.getEffect());
        assertNull(inner.getEffect());
        assertNull(nested.getEffect());
        assertFalse(nested.getStyle().contains("-fx-effect"));
    }

    @Test
    void networkResultsReuseTheStandardCinematicSummaryScene() throws IOException {
        String source = Files.readString(Path.of(
                "src", "main", "java", "com", "example", "birdgame3", "BirdGame3.java"));
        int methodStart = source.indexOf("void showLanResults(Stage stage, int winnerIndex)");
        int nextMethod = source.indexOf("private void requestLanResultsAction", methodStart);

        assertTrue(methodStart >= 0 && nextMethod > methodStart);
        String networkResultsBody = source.substring(methodStart, nextMethod);
        assertTrue(networkResultsBody.contains("showMatchSummary(stage, winner);"));
        assertFalse(networkResultsBody.contains("new Scene("));
    }

    @Test
    void everyBirdAndSkinHasFiniteChampionAndRunnerUpFraming() {
        BirdGame3 game = new BirdGame3(Preferences.userRoot().node(
                "/birdfight3-tests/results-framing/" + UUID.randomUUID()));

        for (BirdGame3.VisualAuditSkin skin : game.visualAuditSkins()) {
            for (boolean champion : new boolean[]{true, false}) {
                BirdGame3.VictoryPortraitLayout layout =
                        game.victoryPortraitLayout(skin.bird(), skin.key(), champion);
                String label = skin.name() + (champion ? " champion" : " runner-up");
                assertNotNull(layout, label + " framing must exist");
                assertTrue(Double.isFinite(layout.extentFactor()) && layout.extentFactor() > 0.0,
                        label + " extent must be positive and finite");
                assertTrue(Double.isFinite(layout.minScale()) && layout.minScale() > 0.0,
                        label + " minimum scale must be positive and finite");
                assertTrue(Double.isFinite(layout.maxScale()) && layout.maxScale() >= layout.minScale(),
                        label + " scale range must be ordered and finite");
                assertTrue(Double.isFinite(layout.xBias()) && Double.isFinite(layout.yBias()),
                        label + " offsets must be finite");
            }
        }
    }

    @Test
    void oversizedVictorySkinsReduceTheirScaleInsteadOfUsingBaseBirdFraming() {
        BirdGame3 game = new BirdGame3(Preferences.userRoot().node(
                "/birdfight3-tests/results-skin-framing/" + UUID.randomUUID()));
        BirdGame3.VictoryPortraitLayout base =
                game.victoryPortraitLayout(BirdGame3.BirdType.VULTURE, null, true);
        BirdGame3.VictoryPortraitLayout nullRock = game.victoryPortraitLayout(
                BirdGame3.BirdType.VULTURE, "NULL_ROCK_VULTURE", true);

        assertTrue(nullRock.extentFactor() > base.extentFactor());
        assertTrue(nullRock.maxScale() < base.maxScale());
    }
}
