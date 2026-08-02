package com.example.birdgame3;

import javafx.application.Platform;
import javafx.scene.SnapshotParameters;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.PixelReader;
import javafx.scene.image.WritableImage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.prefs.Preferences;

import static org.junit.jupiter.api.Assertions.assertTrue;

/** Optional full-stage render used when reviewing City arena presentation. */
class CityVisualAuditRun {
    private static final int OUTPUT_WIDTH = 1800;
    private static final int OUTPUT_HEIGHT = 900;

    @Test
    @Timeout(value = 45, unit = TimeUnit.SECONDS)
    void renderBothCityLayoutsForReview() throws Exception {
        System.setProperty("prism.order", "sw");
        Path outputDir = Path.of(System.getProperty("cityAudit.output", "audit/city"))
                .toAbsolutePath().normalize();
        Files.createDirectories(outputDir);

        CountDownLatch started = new CountDownLatch(1);
        Platform.startup(started::countDown);
        assertTrue(started.await(15, TimeUnit.SECONDS), "JavaFX did not start for the City visual audit");

        FutureTask<Void> renderTask = new FutureTask<>(() -> {
            renderArena(outputDir.resolve("pigeon-city.png"), BirdGame3.MapVariant.STANDARD);
            renderArena(outputDir.resolve("parliament-towers.png"), BirdGame3.MapVariant.PARLIAMENT_ROOFTOPS);
            return null;
        });
        Platform.runLater(renderTask);
        try {
            renderTask.get(30, TimeUnit.SECONDS);
        } finally {
            Platform.exit();
        }
    }

    private static void renderArena(Path output, BirdGame3.MapVariant variant) throws Exception {
        BirdGame3 game = new BirdGame3(Preferences.userRoot().node(
                "/birdfight3-tests/city-visual-audit/" + UUID.randomUUID()));
        game.currentMatchSeed = 73L;
        game.selectedMap = BirdGame3.MapType.CITY;
        game.selectedMapVariant = variant;

        Method setupArena = BirdGame3.class.getDeclaredMethod("setupMatchArenaGeometry");
        Method applyVariant = BirdGame3.class.getDeclaredMethod("applySelectedMapVariantArena");
        Method drawCity = BirdGame3.class.getDeclaredMethod(
                "drawCityArena", GraphicsContext.class, boolean.class);
        setupArena.setAccessible(true);
        applyVariant.setAccessible(true);
        drawCity.setAccessible(true);
        setupArena.invoke(game);
        applyVariant.invoke(game);

        Canvas canvas = new Canvas(OUTPUT_WIDTH, OUTPUT_HEIGHT);
        GraphicsContext graphics = canvas.getGraphicsContext2D();
        graphics.scale(OUTPUT_WIDTH / BirdGame3.WORLD_WIDTH, OUTPUT_HEIGHT / BirdGame3.WORLD_HEIGHT);
        drawCity.invoke(game, graphics, true);
        ImageIO.write(snapshot(canvas), "png", output.toFile());
    }

    private static BufferedImage snapshot(Canvas canvas) {
        int width = (int) canvas.getWidth();
        int height = (int) canvas.getHeight();
        SnapshotParameters parameters = new SnapshotParameters();
        parameters.setFill(javafx.scene.paint.Color.TRANSPARENT);
        WritableImage snapshot = new WritableImage(width, height);
        canvas.snapshot(parameters, snapshot);
        PixelReader pixels = snapshot.getPixelReader();
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                image.setRGB(x, y, pixels.getArgb(x, y));
            }
        }
        return image;
    }
}
