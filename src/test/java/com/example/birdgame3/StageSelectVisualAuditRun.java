package com.example.birdgame3;

import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.SnapshotParameters;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
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
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.prefs.Preferences;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
            List<String> reviewStages = List.of("RANDOM", "BIG FOREST", "PARLIAMENT TOWERS", "OBSIDIAN FOUNDRY");
            for (int index = 0; index < reviewStages.size(); index++) {
                String stageName = reviewStages.get(index);
                Button tile = scene.getRoot().lookupAll(".button").stream()
                        .filter(Button.class::isInstance)
                        .map(Button.class::cast)
                        .filter(button -> (stageName + " stage").equalsIgnoreCase(button.getAccessibleText()))
                        .findFirst()
                        .orElseThrow(() -> new AssertionError("Missing stage-select tile: " + stageName));
                if (tile.getOnMouseEntered() != null) {
                    tile.getOnMouseEntered().handle(null);
                }
                scene.getRoot().applyCss();
                scene.getRoot().layout();
                long activeCursorTags = scene.getRoot().lookupAll(".label").stream()
                        .filter(Label.class::isInstance)
                        .map(Label.class::cast)
                        .filter(Label::isVisible)
                        .filter(label -> "P1".equals(label.getText()))
                        .count();
                assertEquals(1, activeCursorTags,
                        "Hover/focus must never leave two stage tiles looking selected.");
                WritableImage image = new WritableImage(BirdGame3.WIDTH, BirdGame3.HEIGHT);
                scene.getRoot().snapshot(new SnapshotParameters(), image);
                Path frameOutput = index == 0 ? output : auditFramePath(output, stageName);
                ImageIO.write(toBufferedImage(image), "png", frameOutput.toFile());
                if (tile.getOnMouseExited() != null) {
                    tile.getOnMouseExited().handle(null);
                }
            }
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

    private static Path auditFramePath(Path output, String stageName) {
        String fileName = output.getFileName().toString();
        int extension = fileName.lastIndexOf('.');
        String stem = extension >= 0 ? fileName.substring(0, extension) : fileName;
        String suffix = extension >= 0 ? fileName.substring(extension) : ".png";
        String safeName = stageName.toLowerCase().replaceAll("[^a-z0-9]+", "-");
        return output.resolveSibling(stem + "-" + safeName + suffix);
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
