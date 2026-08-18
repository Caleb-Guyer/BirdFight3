package com.example.birdgame3;

import javafx.application.Platform;
import javafx.scene.SnapshotParameters;
import javafx.scene.canvas.Canvas;
import javafx.scene.image.PixelReader;
import javafx.scene.image.WritableImage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Optional asset generator for literal, HUD-free screenshots of every playable arena. */
class StagePreviewCaptureRun {
    private static final int CAPTURE_WIDTH = 640;
    private static final int CAPTURE_HEIGHT = 360;

    @Test
    @Timeout(value = 90, unit = TimeUnit.SECONDS)
    void captureEveryPlayableStage() throws Exception {
        System.setProperty("prism.order", "sw");
        Path output = Path.of(System.getProperty("stageCapture.output",
                "target/generated-stage-previews")).toAbsolutePath().normalize();
        Files.createDirectories(output);

        CountDownLatch started = new CountDownLatch(1);
        Platform.startup(started::countDown);
        assertTrue(started.await(15, TimeUnit.SECONDS), "JavaFX did not start for stage capture");

        FutureTask<Integer> captureTask = new FutureTask<>(() -> {
            BirdGame3 game = new BirdGame3();
            int written = 0;
            for (BirdGame3.StageChoice choice : allStageChoices()) {
                Canvas canvas = game.captureActualStagePreview(choice, CAPTURE_WIDTH, CAPTURE_HEIGHT);
                WritableImage image = new WritableImage(CAPTURE_WIDTH, CAPTURE_HEIGHT);
                canvas.snapshot(new SnapshotParameters(), image);
                Path destination = output.resolve(StagePreviewRenderer.resourceFileName(choice));
                ImageIO.write(toBufferedImage(image), "png", destination.toFile());
                written++;
            }
            return written;
        });
        Platform.runLater(captureTask);
        try {
            assertEquals(47, captureTask.get(75, TimeUnit.SECONDS));
        } finally {
            Platform.exit();
        }
    }

    private static List<BirdGame3.StageChoice> allStageChoices() {
        List<BirdGame3.StageChoice> choices = new ArrayList<>();
        for (BirdGame3.MapType map : BirdGame3.MapType.values()) {
            choices.add(BirdGame3.StageChoice.main(map));
        }
        for (BirdGame3.MapVariant variant : BirdGame3.MapVariant.values()) {
            if (variant != BirdGame3.MapVariant.STANDARD) {
                choices.add(new BirdGame3.StageChoice(variant.baseMap, variant));
            }
        }
        return choices;
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
