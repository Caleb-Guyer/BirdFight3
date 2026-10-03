package com.example.birdgame3;

import javafx.application.Platform;
import javafx.scene.SnapshotParameters;
import javafx.scene.image.PixelFormat;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
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

/** Opt-in real JavaFX playback/seek/export audit; never loads a live profile. */
class ReplayStudioVisualAuditRun {
    @Test
    @Timeout(60)
    void renderStudioAndExportARealReplayClip() throws Exception {
        System.setProperty("prism.order", "sw");
        Path output = Path.of("target/replay-studio-audit").toAbsolutePath();
        Files.createDirectories(output);
        Path clip = output.resolve("studio-" + UUID.randomUUID() + ".avi");
        CountDownLatch startup = new CountDownLatch(1);
        Platform.startup(startup::countDown);
        assertTrue(startup.await(10, TimeUnit.SECONDS));
        Preferences preferences = Preferences.userRoot().node("/birdfight3-tests/replay-studio-visual/" + UUID.randomUUID());
        BirdGame3[] gameRef = new BirdGame3[1];
        Stage[] stageRef = new Stage[1];
        try {
            fx(() -> {
                BirdGame3 game = new BirdGame3(preferences);
                gameRef[0] = game;
                set(game, "sfxEnabled", false);
                set(game, "musicEnabled", false);
                Stage stage = new Stage();
                stageRef[0] = stage;
                game.startReplayPlayback(stage, replay(), true);
                game.timer.stop();
                assertEquals(BirdGame3.BirdType.BAT, game.players[1].type,
                        "Replay roster must include fighters even when the local profile has not unlocked them");
                ReplayStudioState state = (ReplayStudioState) get(game, "replayStudio");
                state.seek(360);
                Method tick = method("gameTick", long.class);
                for (int i = 0; i < 100 && state.seeking(); i++) tick.invoke(game, 0L);
                assertFalse(state.seeking());
                assertEquals(360, get(game, "replayFrameCursor"));
                assertEquals(360, game.simTick);
                game.timer.handle(System.nanoTime());
                StackPane root = (StackPane) get(game, "gameRoot");
                root.applyCss();
                root.layout();
                assertNotNull(root.lookup("#replayStudio"));
                saveSnapshot(root.snapshot(new SnapshotParameters(), null), output.resolve("studio.png"));

                // Start beyond the current cursor to exercise preparation without
                // encoding unwanted earlier frames, then re-seek backwards too.
                state.markIn(365);
                state.markOut(389);
                method("beginReplayClipExport", Stage.class, Path.class).invoke(game, stage, clip);
                game.timer.stop();
                return null;
            });
            long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(25);
            boolean done = false;
            while (!done && System.nanoTime() < deadline) {
                done = fx(() -> {
                    BirdGame3 game = gameRef[0];
                    game.timer.handle(System.nanoTime());
                    return get(game, "replayClipExport") == null;
                });
                if (!done) Thread.sleep(10);
            }
            assertTrue(done, "Real replay export should finish promptly");
            assertTrue(Files.isRegularFile(clip), fx(() -> String.valueOf(get(gameRef[0], "replayStudioStatus"))));
            byte[] bytes = Files.readAllBytes(clip);
            assertEquals("RIFF", new String(bytes, 0, 4, java.nio.charset.StandardCharsets.US_ASCII));
            int jpegStart = -1;
            for (int i = 0; i < bytes.length - 1; i++) {
                if ((bytes[i] & 255) == 255 && (bytes[i + 1] & 255) == 216) { jpegStart = i; break; }
            }
            assertTrue(jpegStart > 0);
            BufferedImage frame = ImageIO.read(new ByteArrayInputStream(bytes, jpegStart, bytes.length - jpegStart));
            assertEquals(1280, frame.getWidth());
            assertEquals(720, frame.getHeight());
            ImageIO.write(frame, "png", output.resolve("clip-first-frame.png").toFile());
            fx(() -> {
                BirdGame3 game = gameRef[0];
                ReplayStudioState state = (ReplayStudioState) get(game, "replayStudio");
                assertTrue(state.paused());
                assertEquals(389, get(game, "replayFrameCursor"));
                method("seekReplayFrame", Stage.class, int.class).invoke(game, stageRef[0], 200);
                game.timer.stop();
                for (int i = 0; i < 100 && state.seeking(); i++) method("gameTick", long.class).invoke(game, 0L);
                assertEquals(200, get(game, "replayFrameCursor"));
                assertEquals(200, game.simTick);
                return null;
            });
        } finally {
            fx(() -> {
                if (gameRef[0] != null) {
                    if (gameRef[0].timer != null) gameRef[0].timer.stop();
                    method("cancelReplayClipExport").invoke(gameRef[0]);
                }
                if (stageRef[0] != null) stageRef[0].close();
                return null;
            });
            preferences.removeNode();
            Platform.exit();
        }
    }

    private static MatchReplay replay() {
        MatchReplay replay = new MatchReplay(0xB17DF19L, 2);
        replay.mapName = "BATTLEFIELD";
        replay.mapVariantName = "STANDARD";
        replay.slotBirdTypes = new String[]{"PIGEON", "BAT"};
        replay.slotIsAi = new boolean[]{true, true};
        replay.slotTeams = new int[]{0, 0};
        replay.slotSkinKeys = new String[]{"DEFAULT", "DEFAULT"};
        replay.slotBaseSize = new double[]{1, 1};
        replay.slotBasePower = new double[]{1, 1};
        replay.slotBaseSpeed = new double[]{1, 1};
        replay.slotInitialStocks = new int[]{3, 3};
        replay.slotInitialHealth = new double[]{100, 100};
        for (int i = 0; i < 1800; i++) replay.frames.add(new int[2]);
        replay.knockouts.add(new MatchReplay.Knockout(1000, "Example KO bookmark"));
        return replay;
    }

    private static Method method(String name, Class<?>... types) throws Exception {
        Method method = BirdGame3.class.getDeclaredMethod(name, types);
        method.setAccessible(true);
        return method;
    }

    private static Object get(BirdGame3 game, String name) throws Exception {
        Field field = BirdGame3.class.getDeclaredField(name);
        field.setAccessible(true);
        return field.get(game);
    }

    private static void set(BirdGame3 game, String name, Object value) throws Exception {
        Field field = BirdGame3.class.getDeclaredField(name);
        field.setAccessible(true);
        field.set(game, value);
    }

    private static <T> T fx(Callable<T> action) throws Exception {
        FutureTask<T> task = new FutureTask<>(action);
        Platform.runLater(task);
        return task.get(15, TimeUnit.SECONDS);
    }

    private static void saveSnapshot(WritableImage image, Path output) throws Exception {
        int width = (int) image.getWidth();
        int height = (int) image.getHeight();
        int[] pixels = new int[width * height];
        image.getPixelReader().getPixels(0, 0, width, height, PixelFormat.getIntArgbInstance(), pixels, 0, width);
        BufferedImage buffered = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        buffered.setRGB(0, 0, width, height, pixels, 0, width);
        ImageIO.write(buffered, "png", output.toFile());
    }
}
