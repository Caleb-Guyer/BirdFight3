package com.example.birdgame3;

import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.PixelFormat;
import javafx.scene.image.WritableImage;
import javafx.stage.Stage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.prefs.Preferences;
import static org.junit.jupiter.api.Assertions.*;

/** Opt-in JavaFX menu/combat audit; never opens or loads the live profile. */
class FlockRunVisualAuditRun {
    @Test @Timeout(60)
    void renderPlayableRunFlowAndAllMenuPhases() throws Exception {
        System.setProperty("prism.order", "sw");
        Path output = Path.of("target/flock-run-audit").toAbsolutePath();
        Files.createDirectories(output);
        CountDownLatch ready = new CountDownLatch(1);
        Platform.startup(ready::countDown);
        assertTrue(ready.await(10, TimeUnit.SECONDS));
        Preferences prefs = Preferences.userRoot().node("/birdfight3-tests/flock-run-visual/" + UUID.randomUUID());
        BirdGame3[] games = new BirdGame3[1];
        Stage[] stages = new Stage[1];
        try {
            fx(() -> {
                BirdGame3 game = new BirdGame3(prefs);
                games[0] = game;
                set(game, "sfxEnabled", false);
                set(game, "musicEnabled", false);
                Method unlock = BirdGame3.class.getDeclaredMethod("unlockEverythingForDeveloperProfile");
                unlock.setAccessible(true);
                unlock.invoke(game);
                Stage stage = new Stage();
                stages[0] = stage;
                game.showFlockRun(stage);
                assertNotNull(stage.getScene().getRoot().lookup("#flockRunRoster"));
                assertEquals(BirdGame3.BirdType.values().length,
                        stage.getScene().getRoot().lookup("#flockRunRoster").lookupAll(".button").size());
                ((Button) stage.getScene().getRoot().lookup("#flockRunBird-EAGLE")).fire();
                snapshot(stage, output.resolve("01-selection.png"));
                Stage small = new Stage();
                small.setScene(new javafx.scene.Scene(new javafx.scene.layout.StackPane(), 1280, 720));
                try {
                    game.showFlockRun(small);
                    resizeOffscreenScene(small, 1280, 720);
                    assertEquals(1280, small.getScene().getWidth());
                    snapshot(small, output.resolve("01-selection-720p.png"));
                } finally { small.close(); }
                Button skin = stage.getScene().getRoot().lookupAll(".button").stream()
                        .filter(Button.class::isInstance).map(Button.class::cast)
                        .filter(b -> b.getText().startsWith("SKIN:")).findFirst().orElseThrow();
                assertFalse(skin.isDisabled());
                skin.fire();
                button(stage, "START").fire();
                FlockRunState selectedRun = ((FlockRunProgress) get(game, "flockRunProgress")).run;
                assertEquals(BirdGame3.BirdType.EAGLE, selectedRun.bird);
                assertNotNull(selectedRun.skinKey);
                snapshot(stage, output.resolve("02-route.png"));
                Button passage = stage.getScene().getRoot().lookupAll(".button").stream()
                        .filter(Button.class::isInstance).map(Button.class::cast)
                        .filter(b -> b.getAccessibleText() != null && b.getAccessibleText().startsWith("Sheltered passage"))
                        .findFirst().orElseThrow();
                passage.fire();
                game.timer.stop();
                assertTrue(game.flockRunMatchActive);
                assertEquals(112, game.players[0].health);
                assertFalse(game.isAI[0]);
                assertEquals(selectedRun.skinKey, game.players[0].appliedSkinKey);
                assertNull(get(game, "replayRecording"));
                game.timer.handle(System.nanoTime());
                snapshot(stage, output.resolve("03-fight.png"));
                game.players[0].health = 48;
                long seed = game.currentMatchSeed;
                Method exit = BirdGame3.class.getDeclaredMethod("exitPausedMatch", Stage.class);
                exit.setAccessible(true);
                exit.invoke(game, stage);
                assertFalse(game.flockRunMatchActive);
                Button resume = stage.getScene().getRoot().lookupAll(".button").stream()
                        .filter(Button.class::isInstance).map(Button.class::cast)
                        .filter(b -> b.getAccessibleText() != null && b.getAccessibleText().startsWith("RESUME ENCOUNTER"))
                        .findFirst().orElseThrow();
                resume.fire();
                game.timer.stop();
                assertEquals(112, game.players[0].health, "Resume restores the opening checkpoint");
                assertEquals(seed, game.currentMatchSeed);
                assertEquals(selectedRun.skinKey, game.players[0].appliedSkinKey);
                game.players[0].health = 48;
                game.captureFlockRunOutcome(game.players[0]);
                game.showMatchSummary(stage, game.players[0]);
                snapshot(stage, output.resolve("04-perks.png"));
                Stage smallRewards = new Stage();
                smallRewards.setScene(new javafx.scene.Scene(new javafx.scene.layout.StackPane(), 1280, 720));
                try {
                    game.showFlockRun(smallRewards);
                    resizeOffscreenScene(smallRewards, 1280, 720);
                    snapshot(smallRewards, output.resolve("04-perks-720p.png"));
                } finally { smallRewards.close(); }
                FlockRunProgress progress = (FlockRunProgress) get(game, "flockRunProgress");
                FlockRunStateTest.draft(progress.run);
                game.showFlockRun(stage);
                snapshot(stage, output.resolve("05-recovery-choice.png"));
                progress.run.chooseRoute(FlockRunState.Route.ELITE);
                game.showFlockRun(stage);
                snapshot(stage, output.resolve("06-resume.png"));
                progress.run = FlockRunStateTest.summit(18, FlockRunState.Route.ELITE);
                game.showFlockRun(stage);
                snapshot(stage, output.resolve("07-summit.png"));
                progress.run.chooseRoute(FlockRunState.Route.BOSS);
                progress.run.finishBattle(true, 58, 60 * 80);
                progress.recordCompletedRun();
                game.showFlockRun(stage);
                snapshot(stage, output.resolve("08-results.png"));
                button(stage, "NEW RUN").fire();
                assertNotNull(stage.getScene().getRoot().lookup("#flockRunRoster"));
                button(stage, "BACK").fire();
                assertNotNull(stage.getScene().getRoot().lookup("#flockRunPage"));
                assertEquals(0, prefs.keys().length, "A visual audit cannot bypass the profile-load save guard");
                Method more = BirdGame3.class.getDeclaredMethod("showClassicMoreMenu", Stage.class);
                more.setAccessible(true);
                more.invoke(game, stage);
                return null;
            });
            // Let the dashboard's deferred entrance reach its final visible state.
            Thread.sleep(750);
            fx(() -> {
                snapshot(stages[0], output.resolve("00-games-and-more.png"));
                return null;
            });
        } finally {
            fx(() -> {
                if (games[0] != null && games[0].timer != null) games[0].timer.stop();
                if (stages[0] != null) stages[0].close();
                return null;
            });
            prefs.removeNode();
            Platform.exit();
        }
    }

    private static Button button(Stage stage, String text) {
        return stage.getScene().getRoot().lookupAll(".button").stream().filter(Button.class::isInstance)
                .map(Button.class::cast).filter(b -> text.equals(b.getText())).findFirst().orElseThrow();
    }
    private static void resizeOffscreenScene(Stage stage, double width, double height) throws Exception {
        // A hidden Stage has no native resize events. Exercise the Scene's size
        // properties so the real layout listeners run without opening a window.
        Method setWidth = javafx.scene.Scene.class.getDeclaredMethod("setWidth", double.class);
        Method setHeight = javafx.scene.Scene.class.getDeclaredMethod("setHeight", double.class);
        setWidth.setAccessible(true); setHeight.setAccessible(true);
        setWidth.invoke(stage.getScene(), width); setHeight.invoke(stage.getScene(), height);
        ((javafx.scene.layout.Region) stage.getScene().getRoot()).resize(width, height);
    }
    private static void snapshot(Stage stage, Path output) throws Exception {
        Parent root = stage.getScene().getRoot();
        root.applyCss(); root.layout();
        Node page = root.lookup("#flockRunPage");
        if (page == null) page = root.lookup("#flockRunRoster");
        if (page != null) {
            for (Node node : page.lookupAll(".label")) {
                Label label = (Label) node;
                assertTrue(label.getHeight() + 1 >= label.prefHeight(label.getWidth()),
                        "Clipped label: " + label.getText() + " (" + label.getHeight() + " vs " + label.prefHeight(label.getWidth()) + ")");
            }
        }
        WritableImage image = stage.getScene().snapshot(null);
        int width = (int) image.getWidth(), height = (int) image.getHeight();
        int[] pixels = new int[width * height];
        image.getPixelReader().getPixels(0, 0, width, height, PixelFormat.getIntArgbInstance(), pixels, 0, width);
        BufferedImage buffered = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        buffered.setRGB(0, 0, width, height, pixels, 0, width);
        ImageIO.write(buffered, "png", output.toFile());
    }
    private static Object get(BirdGame3 game, String name) throws Exception {
        Field field = BirdGame3.class.getDeclaredField(name); field.setAccessible(true); return field.get(game);
    }
    private static void set(BirdGame3 game, String name, Object value) throws Exception {
        Field field = BirdGame3.class.getDeclaredField(name); field.setAccessible(true); field.set(game, value);
    }
    private static <T> T fx(Callable<T> action) throws Exception {
        FutureTask<T> task = new FutureTask<>(action); Platform.runLater(task); return task.get(40, TimeUnit.SECONDS);
    }
}
