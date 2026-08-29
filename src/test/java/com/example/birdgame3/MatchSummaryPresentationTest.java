package com.example.birdgame3;

import javafx.application.Platform;
import javafx.animation.PauseTransition;
import javafx.beans.property.SimpleStringProperty;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;
import javafx.util.Duration;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.prefs.Preferences;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
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
        Rectangle boundStyle = new Rectangle(80.0, 30.0);
        boundStyle.setEffect(new DropShadow());
        SimpleStringProperty liveStyle = new SimpleStringProperty("-fx-fill: blue;");
        boundStyle.styleProperty().bind(liveStyle);
        Group inner = new Group(nested, boundStyle);
        inner.setEffect(new DropShadow());
        Group root = new Group(inner);
        root.setEffect(new DropShadow());
        nested.setStyle("-fx-fill: red; -fx-effect: dropshadow(gaussian, black, 14, 0.2, 0, 5);");

        BirdGame3.clearNodeEffects(root);

        assertNull(root.getEffect());
        assertNull(inner.getEffect());
        assertNull(nested.getEffect());
        assertNull(boundStyle.getEffect());
        assertFalse(nested.getStyle().contains("-fx-effect"));
        assertTrue(boundStyle.styleProperty().isBound(),
                "device-aware UI bindings must survive result effect cleanup");
        assertEquals("-fx-fill: blue;", boundStyle.getStyle());
    }

    @Test
    void cinematicSummaryReusesOneBoundedBackgroundRenderTarget() {
        BirdGame3 game = new BirdGame3(Preferences.userRoot().node(
                "/birdfight3-tests/results-surface/" + UUID.randomUUID()));

        Canvas first = game.prepareMatchSummaryBackgroundCanvas();
        Canvas second = game.prepareMatchSummaryBackgroundCanvas();

        assertSame(first, second);
        assertTrue(first.getWidth() <= GameplayRenderSurface.MAX_BACKING_WIDTH);
        assertTrue(first.getHeight() <= GameplayRenderSurface.MAX_BACKING_HEIGHT);
    }

    @Test
    void cinematicBackgroundStartsOnlyAfterFullscreenSceneTransfer() throws IOException {
        String source = Files.readString(Path.of(
                "src", "main", "java", "com", "example", "birdgame3", "BirdGame3.java"));
        int methodStart = source.indexOf("void showMatchSummary(Stage stage, Bird winner)");
        int methodEnd = source.indexOf("Canvas prepareMatchSummaryBackgroundCanvas()", methodStart);
        assertTrue(methodStart >= 0 && methodEnd > methodStart);
        String body = source.substring(methodStart, methodEnd);

        int initialPaint = body.indexOf("drawCinematicResultsBackground(background.getGraphicsContext2D(), accent, 0.0)");
        int install = body.indexOf("Scene installedScene = setScenePreservingFullscreen(stage, scene)");
        int activeScene = body.indexOf("sceneRef[0] = installedScene");
        int timerOwnership = body.indexOf("matchSummaryBackgroundTimer = backgroundTimer");
        int timerStart = body.indexOf("backgroundTimer.start()");
        assertTrue(initialPaint >= 0);
        assertTrue(install > initialPaint);
        assertTrue(activeScene > install);
        assertTrue(timerOwnership > activeScene);
        assertTrue(timerStart > timerOwnership);
        assertTrue(body.contains("scene.getRoot() != frame"),
                "fullscreen keeps one Scene, so the results timer must follow its installed root");
        assertTrue(body.contains("matchSummaryBackgroundTimer != this"),
                "a superseded results renderer must stop even before its scene changes");
    }

    @Test
    void everySceneSwapStopsThePreviousResultsRenderer() throws IOException {
        String source = Files.readString(Path.of(
                "src", "main", "java", "com", "example", "birdgame3", "BirdGame3.java"));
        int methodStart = source.indexOf("private Scene setScenePreservingFullscreen(Stage stage, Scene scene)");
        int methodEnd = source.indexOf("private Scene installScenePreservingCurrentFullscreen", methodStart);
        assertTrue(methodStart >= 0 && methodEnd > methodStart);

        String body = source.substring(methodStart, methodEnd);
        assertTrue(body.contains("stopMatchSummaryBackgroundTimer();"));
    }

    @Test
    void cinematicSummaryUsesOnlyTheResponsiveSceneScale() throws IOException {
        String source = Files.readString(Path.of(
                "src", "main", "java", "com", "example", "birdgame3", "BirdGame3.java"));
        int methodStart = source.indexOf("void showMatchSummary(Stage stage, Bird winner)");
        int methodEnd = source.indexOf("Canvas prepareMatchSummaryBackgroundCanvas()", methodStart);
        assertTrue(methodStart >= 0 && methodEnd > methodStart);
        String body = source.substring(methodStart, methodEnd);

        assertTrue(body.contains("new Scene(frame, WIDTH, HEIGHT)"));
        assertTrue(body.contains("makeSceneResponsive(scene)"));
        assertFalse(body.contains("bindFixedFrameScale"));
    }

    @Test
    void cinematicWinnerPoseUsesOneCanvasInsteadOfThreeLargeRenderTargets() throws Exception {
        BirdGame3 game = new BirdGame3(Preferences.userRoot().node(
                "/birdfight3-tests/results-pose/" + UUID.randomUUID()));
        Method builder = BirdGame3.class.getDeclaredMethod(
                "buildCinematicVictoryPose", Bird.class, double.class);
        builder.setAccessible(true);
        Bird pigeon = new Bird(0, BirdGame3.BirdType.PIGEON, 0, game);

        StackPane pose = (StackPane) builder.invoke(game, pigeon, 760.0);

        assertEquals(1, countCanvases(pose));
    }

    @Test
    void firstStoryClearCanBuildTheCompleteResultsTree() throws Exception {
        CountDownLatch completed = new CountDownLatch(1);
        AtomicReference<Throwable> failure = new AtomicReference<>();
        Platform.startup(() -> {
            try {
                Platform.setImplicitExit(false);
                BirdGame3 game = new BirdGame3(Preferences.userRoot().node(
                        "/birdfight3-tests/first-story-results/" + UUID.randomUUID()));
                game.campaignModeActive = true;
                game.activePlayers = 3;
                setField(game, "campaignMissionWon", true);
                setField(game, "currentCampaignMission",
                        StoryCampaignContent.create().mission("dead_air"));

                Bird pigeon = new Bird(0, BirdGame3.BirdType.PIGEON, 0, game);
                Bird charles = new Bird(0, BirdGame3.BirdType.MOCKINGBIRD, 1, game);
                Bird raven = new Bird(0, BirdGame3.BirdType.RAVEN, 2, game);
                game.players[0] = pigeon;
                game.players[1] = charles;
                game.players[2] = raven;
                Stage stage = new Stage();
                game.showMatchSummary(stage, pigeon);

                PauseTransition waitForIntro = new PauseTransition(Duration.millis(2_200));
                waitForIntro.setOnFinished(event -> {
                    try {
                        Scene scene = stage.getScene();
                        assertNotNull(scene, "the results scene must be installed");
                        assertEquals("responsiveContainer", scene.getRoot().getId());
                        Label missionComplete = findNode(scene.getRoot(), Label.class, "MISSION COMPLETE");
                        Button continueStory = findNode(scene.getRoot(), Button.class, "CONTINUE STORY");
                        assertNotNull(missionComplete, "the first-clear headline must be in the live scene");
                        assertNotNull(continueStory, "the first-clear action must be in the live scene");
                        assertTrue(treeOpacity(missionComplete) > 0.95,
                                "the intro must reveal the mission headline");
                        assertTrue(treeOpacity(continueStory) > 0.95,
                                "the intro must reveal the story actions");
                    } catch (Throwable thrown) {
                        failure.set(thrown);
                    } finally {
                        completed.countDown();
                    }
                });
                waitForIntro.play();
            } catch (Throwable thrown) {
                failure.set(thrown);
                completed.countDown();
            }
        });

        assertTrue(completed.await(10, TimeUnit.SECONDS), "JavaFX results build timed out");
        if (failure.get() != null) {
            throw new AssertionError("First story results UI failed to build", failure.get());
        }
        Platform.exit();
    }

    private static <T extends Node> T findNode(Node node, Class<T> type, String text) {
        if (type.isInstance(node)) {
            String nodeText = node instanceof Button button ? button.getAccessibleText()
                    : node instanceof Label label ? label.getText() : "";
            if (text.equals(nodeText)) {
                return type.cast(node);
            }
        }
        if (node instanceof Parent parent) {
            for (Node child : parent.getChildrenUnmodifiable()) {
                T found = findNode(child, type, text);
                if (found != null) return found;
            }
        }
        return null;
    }

    private static double treeOpacity(Node node) {
        double opacity = 1.0;
        for (Node current = node; current != null; current = current.getParent()) {
            opacity *= current.getOpacity();
        }
        return opacity;
    }

    private static void setField(BirdGame3 game, String fieldName, Object value) throws Exception {
        Field field = BirdGame3.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(game, value);
    }

    private static int countCanvases(Node node) {
        int count = node instanceof Canvas ? 1 : 0;
        if (node instanceof Parent parent) {
            for (Node child : parent.getChildrenUnmodifiable()) {
                count += countCanvases(child);
            }
        }
        return count;
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
