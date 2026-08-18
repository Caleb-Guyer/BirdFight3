package com.example.birdgame3;

import javafx.application.Platform;
import javafx.geometry.Rectangle2D;
import javafx.scene.SnapshotParameters;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.PixelReader;
import javafx.scene.image.WritableImage;
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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Optional pixel render for reviewing the compact and resource-bearing fight HUD card. */
class FightHudVisualAuditRun {
    @Test
    @Timeout(value = 45, unit = TimeUnit.SECONDS)
    void renderHeisenbirdCardForReview() throws Exception {
        System.setProperty("prism.order", "sw");
        Path output = Path.of(System.getProperty(
                        "fightHudAudit.output", "audit/hud/fight-hud-heisenbird.png"))
                .toAbsolutePath().normalize();
        Files.createDirectories(output.getParent());

        CountDownLatch started = new CountDownLatch(1);
        Platform.startup(started::countDown);
        assertTrue(started.await(15, TimeUnit.SECONDS), "JavaFX did not start for the HUD visual audit");

        FutureTask<Void> renderTask = new FutureTask<>(() -> {
            renderCard(output, false);
            renderCard(output.resolveSibling("fight-hud-compact.png"), true);
            return null;
        });
        Platform.runLater(renderTask);
        try {
            renderTask.get(30, TimeUnit.SECONDS);
        } finally {
            Platform.exit();
        }
        assertTrue(Files.size(output) > 1_000L, "HUD audit image should contain a real card render");
        assertTrue(Files.size(output.resolveSibling("fight-hud-compact.png")) > 1_000L,
                "Compact HUD audit image should contain a real card render");
    }

    private static void renderCard(Path output, boolean compact) throws Exception {
        BirdGame3 game = new BirdGame3(Preferences.userRoot().node(
                "/birdfight3-tests/fight-hud-visual-audit/" + UUID.randomUUID()));
        game.harnessPrepareMatch(BirdGame3.BirdType.HEISENBIRD,
                BirdGame3.BirdType.PIGEON, 0x51A7B1DDL);
        game.players[0].name = "YOU";
        game.scores[0] = 2;
        setDouble(game.players[0], "ultimateMeter", 58.0);
        setDouble(game.players[0], "opiumResourceMeter", 74.0);
        if (compact) {
            BirdGame3.BirdType[] extras = {
                    BirdGame3.BirdType.FALCON,
                    BirdGame3.BirdType.PHOENIX,
                    BirdGame3.BirdType.TURKEY
            };
            for (int index = 0; index < extras.length; index++) {
                int slot = index + 2;
                game.players[slot] = new Bird(1_000.0 + slot * 100.0, extras[index], slot, game);
                game.scores[slot] = 2;
            }
            game.activePlayers = 5;
        }

        Method buildPanels = BirdGame3.class.getDeclaredMethod("buildFightHudPanels");
        buildPanels.setAccessible(true);
        @SuppressWarnings("unchecked")
        List<Object> panels = (List<Object>) buildPanels.invoke(game);
        assertFalse(panels.isEmpty());
        Object panel = panels.getFirst();
        Method panelRectAccessor = panel.getClass().getDeclaredMethod("panelRect");
        panelRectAccessor.setAccessible(true);
        Rectangle2D rect = (Rectangle2D) panelRectAccessor.invoke(panel);

        Canvas canvas = new Canvas(BirdGame3.WIDTH, BirdGame3.HEIGHT);
        GraphicsContext graphics = canvas.getGraphicsContext2D();
        Method drawPanel = BirdGame3.class.getDeclaredMethod(
                "drawFightHudPanel", GraphicsContext.class, panel.getClass());
        drawPanel.setAccessible(true);
        drawPanel.invoke(game, graphics, panel);

        BufferedImage full = snapshot(canvas);
        int pad = 12;
        int x = Math.max(0, (int) Math.floor(rect.getMinX()) - pad);
        int y = Math.max(0, (int) Math.floor(rect.getMinY()) - pad);
        int width = Math.min(full.getWidth() - x, (int) Math.ceil(rect.getWidth()) + pad * 2);
        int height = Math.min(full.getHeight() - y, (int) Math.ceil(rect.getHeight()) + pad * 2);
        ImageIO.write(full.getSubimage(x, y, width, height), "png", output.toFile());
    }

    private static void setDouble(Object target, String fieldName, double value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.setDouble(target, value);
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
