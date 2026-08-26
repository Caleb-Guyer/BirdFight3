package com.example.birdgame3;

import javafx.animation.Animation;
import javafx.application.Platform;
import javafx.event.Event;
import javafx.event.EventType;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.SnapshotParameters;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.image.WritableImage;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.prefs.Preferences;

import static com.example.birdgame3.BirdGame3.BirdType;
import static com.example.birdgame3.BirdGame3.MapType;
import static org.junit.jupiter.api.Assertions.*;

/** Optional offscreen render and real input/scene-transfer test. Never shows a window. */
class RewardUiVisualAuditRun {
    @Test
    @Timeout(value = 90, unit = TimeUnit.SECONDS)
    void renderRewardsAndExerciseQueuedInputWithoutOpeningTheGame() throws Exception {
        System.setProperty("prism.order", "sw");
        Path output = Path.of("target/reward-ui-audit");
        Files.createDirectories(output);
        Preferences prefs = Preferences.userRoot().node("/birdfight3-tests/reward-ui/" + UUID.randomUUID());
        CountDownLatch ready = new CountDownLatch(1);
        Platform.startup(ready::countDown);
        assertTrue(ready.await(15, TimeUnit.SECONDS));
        AtomicInteger completions = new AtomicInteger();
        CountDownLatch completed = new CountDownLatch(1);
        Stage[] hiddenStage = new Stage[1];
        try {
            onFx(() -> {
                BirdGame3 game = new BirdGame3(prefs);
                set(game, "musicEnabled", false);
                set(game, "sfxEnabled", false);
                set(game, "fullscreenEnabled", false);
                Stage stage = hiddenStage[0] = new Stage();
                assertAll("All reward portraits", game.visualAuditSkins().stream().map(skin -> () -> {
                    var reward = new RewardPresentation(skin.key() == null ? RewardPresentation.Kind.BIRD
                            : RewardPresentation.Kind.SKIN, skin.name(), "", skin.bird(), skin.key(), null);
                    SimRng.reseed(5197);
                    Node art = (Node) invoke(game, "buildRewardRevealArt",
                            new Class<?>[]{RewardPresentation.class, double.class, double.class}, reward, 416.0, 328.0);
                    assertPortraitFits(art, skin.name(), output.resolve("clipped-" + skin.key() + ".png"));
                    assertEquals(new Random(5197).nextLong(), SimRng.random().nextLong(),
                            skin.name() + " consumed simulation randomness");
                }));
                System.out.println("Reward artwork checked: " + game.visualAuditSkins().size() + " birds and skins");
                var rewards = List.of(
                        new RewardPresentation(RewardPresentation.Kind.BIRD, "Tufted Titmouse",
                                "Ready for your next fight.", BirdType.TITMOUSE, null, null),
                        new RewardPresentation(RewardPresentation.Kind.SKIN, "Ashen Sovereign Phoenix",
                                "A new look for Phoenix.", BirdType.PHOENIX,
                                game.visualAuditSkins().stream().filter(s -> s.name().equals("Ashen Sovereign Phoenix"))
                                        .findFirst().orElseThrow().key(), null),
                        new RewardPresentation(RewardPresentation.Kind.STAGE, "Glasswind Causeway",
                                "Ready to choose in stage selection.", null, null, MapType.GLASSWIND_CAUSEWAY),
                        new RewardPresentation(RewardPresentation.Kind.COINS, "Bird Coins +2,000",
                                "Annihilator! — Eliminate all other opponents in a match.", null, null, null),
                        new RewardPresentation(RewardPresentation.Kind.CONTINUE, "Classic Continue",
                                "Continue your Classic run.", null, null, null));
                for (var reward : rewards) {
                    SimRng.reseed(32914);
                    show(game, stage, reward, () -> fail("Rendering must never advance or grant a reward"));
                    assertEquals(new Random(32914).nextLong(), SimRng.random().nextLong(),
                            "Reward artwork must not consume simulation randomness");
                    // Transfer into different-sized scenes exactly as fullscreen navigation does,
                    // without ever calling Stage.show / setFullScreen.
                    for (int[] size : new int[][]{{1280, 720}, {2560, 1600}}) {
                        transfer(game, stage, size[0], size[1]);
                        Scene scene = stage.getScene();
                        prepare(scene);
                        assertLabelsFit(scene.getRoot());
                        Node frame = scene.lookup("#uiFrame");
                        var bounds = frame.localToScene(frame.getLayoutBounds());
                        assertTrue(bounds.getMinX() >= -.5 && bounds.getMinY() >= -.5);
                        assertTrue(bounds.getMaxX() <= size[0] + .5 && bounds.getMaxY() <= size[1] + .5);
                        write(scene.getRoot(), size[0], size[1],
                                output.resolve(reward.kind().name().toLowerCase() + "-" + size[0] + ".png"));
                    }
                }
                var pack = new ShopPackResult("Nebula Pack Opened", List.of(
                        new ShopPackResult.Reward("Bat Character", new ShopPreview(BirdType.BAT, "CHAR_BAT", "Bat")),
                        new ShopPackResult.Reward("Noir Pigeon Skin", new ShopPreview(BirdType.PIGEON, "NOIR_PIGEON", "Noir Pigeon")),
                        new ShopPackResult.Reward("Bird Coins +700", new ShopPreview(null, null, "Bird Coins +700", 700))));
                invoke(game, "showPackResult", new Class<?>[]{Stage.class, ShopPackResult.class}, stage, pack);
                prepare(stage.getScene());
                assertLabelsFit(stage.getScene().getRoot());
                write(stage.getScene().getRoot(), BirdGame3.WIDTH, BirdGame3.HEIGHT, output.resolve("pack.png"));
                assertEquals(0, ((BirdCoinLedger) get(game, "birdCoinLedger")).balance(),
                        "Showing a receipt must not grant its coin amount");
                assertFalse((Boolean) get(game, "batUnlocked"), "Showing artwork must not unlock its fighter");

                var pagedRewards = new java.util.ArrayList<>(pack.rewards());
                pagedRewards.add(new ShopPackResult.Reward("Sunscorch Flats Map",
                        new ShopPreview(null, "MAP_DESERT", "Sunscorch Flats Map")));
                invoke(game, "showPackResult", new Class<?>[]{Stage.class, ShopPackResult.class}, stage,
                        new ShopPackResult("Four rewards", pagedRewards));
                prepare(stage.getScene());
                Button nextPage = stage.getScene().getRoot().lookupAll(".button").stream()
                        .filter(Button.class::isInstance).map(Button.class::cast)
                        .filter(b -> "Next rewards".equals(b.getAccessibleText())).findFirst().orElseThrow();
                nextPage.fire();
                prepare(stage.getScene());
                assertEquals("Sunscorch Flats Map", ((Label) stage.getScene().lookup("#pack-reward-name")).getText());
                assertTrue(nextPage.isDisabled());
                assertLabelsFit(stage.getScene().getRoot());

                Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                        "Your campaign progress will be reset. Unlocked birds, skins, and coins will be kept.",
                        ButtonType.NO, ButtonType.YES);
                confirm.setHeaderText("Start a new adventure?");
                ModernDialogTheme.apply(confirm);
                var dialogPane = confirm.getDialogPane();
                // Alert creates its own hidden Scene. Detach before our offscreen layout.
                dialogPane.getScene().setRoot(new Group());
                Scene dialogScene = new Scene(dialogPane, 760, 300);
                prepare(dialogScene);
                for (ButtonType type : List.of(ButtonType.YES, ButtonType.NO)) {
                    Button button = (Button) dialogPane.lookupButton(type);
                    Text rendered = (Text) button.lookup(".text");
                    assertEquals(button.getText(), rendered.getText(), "Action caption was truncated");
                    assertTrue(rendered.getLayoutBounds().getWidth()
                            <= button.getWidth() - button.getInsets().getLeft() - button.getInsets().getRight());
                }
                assertTrue(((Button) dialogPane.lookupButton(ButtonType.NO)).isDefaultButton());
                write(dialogPane, 760, 300, output.resolve("confirmation.png"));
                confirm.close();

                // Exercise the actual unlock queue rather than just the visual helper.
                game.queueBirdUnlockCard(BirdType.BAT);
                game.queueBirdUnlockCard(BirdType.PHOENIX);
                invoke(game, "runAfterUnlockCards", new Class<?>[]{Stage.class, Runnable.class}, stage,
                        (Runnable) () -> { completions.incrementAndGet(); completed.countDown(); });
                transfer(game, stage, 2560, 1600);
                prepare(stage.getScene());
                key(stage.getScene(), KeyEvent.KEY_RELEASED, KeyCode.ENTER); // opening button's release
                assertEquals("Bat", ((Label) stage.getScene().lookup("#reward-name")).getText());
                assertFalse(Boolean.TRUE.equals(stage.getScene().lookup("#uiFrame").getProperties().get("fightMenuExitRunning")));
                for (int i = 0; i < 20; i++) key(stage.getScene(), KeyEvent.KEY_PRESSED, KeyCode.ENTER);
                assertFalse(Boolean.TRUE.equals(stage.getScene().lookup("#uiFrame").getProperties().get("fightMenuExitRunning")));
                key(stage.getScene(), KeyEvent.KEY_RELEASED, KeyCode.ENTER);
                Animation pendingExit = (Animation) stage.getScene().lookup("#uiFrame")
                        .getProperties().get("fightMenuTransition");
                invoke(game, "playFightMenuEntrance", new Class<?>[]{Node.class, List.class},
                        stage.getScene().lookup("#uiFrame"), List.of());
                assertSame(pendingExit, stage.getScene().lookup("#uiFrame").getProperties().get("fightMenuTransition"),
                        "A late entrance must not cancel a quick confirm's exit");
                finishExit(stage.getScene());
                assertEquals("Phoenix", ((Label) stage.getScene().lookup("#reward-name")).getText());
                key(stage.getScene(), KeyEvent.KEY_RELEASED, KeyCode.ENTER);
                assertEquals(0, completions.get(), "Release carried into new scene must not skip the second unlock");
                key(stage.getScene(), KeyEvent.KEY_PRESSED, KeyCode.SPACE);
                key(stage.getScene(), KeyEvent.KEY_RELEASED, KeyCode.SPACE);
                for (int i = 0; i < 5; i++) {
                    stage.getScene().getRoot().lookupAll(".button").stream()
                            .filter(Button.class::isInstance).map(Button.class::cast)
                            .filter(b -> b.getText().equals("CONTINUE")).forEach(Button::fire);
                }
                finishExit(stage.getScene());
                assertFalse(stage.isShowing());
                return null;
            });
            assertTrue(completed.await(5, TimeUnit.SECONDS));
            assertEquals(1, completions.get(), "Repeated input must only complete the queue once");
        } finally {
            onFx(() -> { if (hiddenStage[0] != null) hiddenStage[0].close(); return null; });
            Platform.exit();
            prefs.removeNode();
        }
    }

    private static void show(BirdGame3 game, Stage stage, RewardPresentation reward, Runnable complete) throws Exception {
        invoke(game, "showRewardReveal", new Class<?>[]{Stage.class, RewardPresentation.class, String.class, String.class, Runnable.class},
                stage, reward, "", "CONTINUE", complete);
    }

    private static void transfer(BirdGame3 game, Stage stage, int width, int height) throws Exception {
        Scene target = new Scene(new Group(), width, height);
        invoke(game, "swapFullscreenSceneRoot", new Class<?>[]{Scene.class, Scene.class}, target, stage.getScene());
        stage.setScene(target);
        invoke(game, "bindFixedFrameScale", new Class<?>[]{Scene.class, Node.class, double.class},
                target, target.lookup("#uiFrame"), 0.0);
    }

    private static void prepare(Scene scene) {
        // The offscreen Scene has no window pulses. Apply the same fixed-frame metric
        // the production resize listener schedules for the next FX pulse.
        Node frame = scene.lookup("#uiFrame");
        if (frame != null) {
            double scale = BirdGame3.fixedFrameScale(scene.getWidth(), scene.getHeight(), 1600, 950);
            frame.setScaleX(scale);
            frame.setScaleY(scale);
        }
        scene.getRoot().resize(scene.getWidth(), scene.getHeight());
        scene.getRoot().applyCss();
        scene.getRoot().layout();
    }

    private static void assertLabelsFit(Parent root) {
        for (Node node : root.lookupAll(".label")) {
            if (!(node instanceof Label label) || label.getText().isBlank()) continue;
            Text text = (Text) label.lookup(".text");
            assertNotNull(text);
            assertEquals(label.getText(), text.getText(), "Label lost text: " + label.getText());
            assertTrue(text.getLayoutBounds().getWidth() <= label.getWidth() + 1,
                    "Horizontal clipping: " + label.getText());
            assertTrue(text.getLayoutBounds().getHeight() <= label.getHeight() + 1,
                    "Vertical clipping: " + label.getText());
        }
    }

    private static void assertPortraitFits(Node art, String name, Path failureCapture) throws Exception {
        SnapshotParameters parameters = new SnapshotParameters();
        parameters.setFill(Color.TRANSPARENT);
        WritableImage image = art.snapshot(parameters, null);
        int width = (int) image.getWidth();
        int height = (int) image.getHeight();
        int pixels = 0;
        int minX = width, minY = height, maxX = -1, maxY = -1;
        for (int y = 0; y < height; y++) for (int x = 0; x < width; x++) {
            if ((image.getPixelReader().getArgb(x, y) >>> 24) < 20) continue;
            pixels++;
            minX = Math.min(minX, x); minY = Math.min(minY, y);
            maxX = Math.max(maxX, x); maxY = Math.max(maxY, y);
        }
        boolean fits = minX >= 2 && minY >= 2 && maxX < width - 2 && maxY < height - 2;
        if (!fits) write(art, width, height, failureCapture);
        assertTrue(fits, name + " touches the artwork boundary: " + minX + ", " + minY + " to " + maxX + ", " + maxY);
        assertTrue(pixels > width * height * .005, name + " has missing or tiny reward art");
    }

    private static void key(Scene scene, EventType<KeyEvent> type, KeyCode key) {
        Event.fireEvent(scene, new KeyEvent(type, "", "", key, false, false, false, false));
    }

    private static void finishExit(Scene scene) {
        Node frame = scene.lookup("#uiFrame");
        Animation exit = (Animation) frame.getProperties().get("fightMenuTransition");
        assertNotNull(exit);
        exit.stop();
        exit.getOnFinished().handle(null);
    }

    private static void write(Node root, int width, int height, Path output) throws Exception {
        WritableImage image = root.snapshot(new SnapshotParameters(), new WritableImage(width, height));
        BufferedImage buffered = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < height; y++) for (int x = 0; x < width; x++) {
            buffered.setRGB(x, y, image.getPixelReader().getArgb(x, y));
        }
        ImageIO.write(buffered, "png", output.toFile());
    }

    private static Object invoke(Object instance, String name, Class<?>[] signature, Object... args) throws Exception {
        Method method = BirdGame3.class.getDeclaredMethod(name, signature);
        method.setAccessible(true);
        return method.invoke(instance, args);
    }

    private static Object get(Object instance, String name) throws Exception {
        Field field = BirdGame3.class.getDeclaredField(name);
        field.setAccessible(true);
        return field.get(instance);
    }

    private static void set(Object instance, String name, Object value) throws Exception {
        Field field = BirdGame3.class.getDeclaredField(name);
        field.setAccessible(true);
        field.set(instance, value);
    }

    private static <T> T onFx(java.util.concurrent.Callable<T> task) throws Exception {
        FutureTask<T> future = new FutureTask<>(task);
        Platform.runLater(future);
        return future.get(65, TimeUnit.SECONDS);
    }
}
