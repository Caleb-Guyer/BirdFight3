package com.example.birdgame3;

import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.SnapshotParameters;
import javafx.scene.image.PixelReader;
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
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.prefs.Preferences;

import static org.junit.jupiter.api.Assertions.assertTrue;

/** Optional full-screen render used to review the no-scroll stage selector without showing a window. */
class StageSelectVisualAuditRun {
    @Test
    @Timeout(value = 45, unit = TimeUnit.SECONDS)
    void renderUnifiedSelectorForReview() throws Exception {
        System.setProperty("prism.order", "sw");
        Path output = Path.of(System.getProperty("stageSelectAudit.output",
                "target/stage-select-audit.png")).toAbsolutePath().normalize();
        Files.createDirectories(output.getParent());

        CountDownLatch started = new CountDownLatch(1);
        Platform.startup(started::countDown);
        assertTrue(started.await(15, TimeUnit.SECONDS), "JavaFX did not start for the stage-select visual audit");

        FutureTask<Void> renderTask = new FutureTask<>(() -> {
            BirdGame3 game = new BirdGame3(Preferences.userRoot().node(
                    "/birdfight3-tests/stage-select-audit/" + UUID.randomUUID()));
            Field musicEnabled = BirdGame3.class.getDeclaredField("musicEnabled");
            musicEnabled.setAccessible(true);
            musicEnabled.setBoolean(game, false);
            Method unlock = BirdGame3.class.getDeclaredMethod("unlockEverythingForDeveloperProfile");
            Method show = BirdGame3.class.getDeclaredMethod("showStageSelect", Stage.class);
            unlock.setAccessible(true);
            show.setAccessible(true);
            unlock.invoke(game);

            Stage stage = new Stage();
            show.invoke(game, stage);
            Scene scene = stage.getScene();
            scene.getRoot().applyCss();
            scene.getRoot().layout();
            WritableImage image = new WritableImage(BirdGame3.WIDTH, BirdGame3.HEIGHT);
            scene.getRoot().snapshot(new SnapshotParameters(), image);
            ImageIO.write(toBufferedImage(image), "png", output.toFile());
            stage.close();
            return null;
        });
        Platform.runLater(renderTask);
        try {
            renderTask.get(30, TimeUnit.SECONDS);
        } finally {
            Platform.exit();
        }
    }

    private static BufferedImage toBufferedImage(WritableImage image) {
        int width = (int) image.getWidth();
        int height = (int) image.getHeight();
        PixelReader pixels = image.getPixelReader();
        BufferedImage buffered = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                buffered.setRGB(x, y, pixels.getArgb(x, y));
            }
        }
        return buffered;
    }
}
