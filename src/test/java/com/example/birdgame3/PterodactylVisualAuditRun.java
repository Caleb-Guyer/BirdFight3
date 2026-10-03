package com.example.birdgame3;

import javafx.application.Platform;
import javafx.scene.Parent;
import javafx.scene.SnapshotParameters;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.image.PixelFormat;
import javafx.scene.image.WritableImage;
import javafx.stage.Stage;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.TextAlignment;
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
import static org.junit.jupiter.api.Assertions.*;

/** Opt-in shared-selector and live-arena audit using an isolated profile. */
class PterodactylVisualAuditRun {
    @Test @Timeout(60) void selectBothSkinsAndEnterTheRealFlockRunArena() throws Exception {
        System.setProperty("prism.order", "sw");
        Path output = Path.of("target/pterodactyl-ui").toAbsolutePath(); Files.createDirectories(output);
        CountDownLatch ready = new CountDownLatch(1); Platform.startup(ready::countDown);
        assertTrue(ready.await(10, TimeUnit.SECONDS));
        Preferences prefs = Preferences.userRoot().node("/birdfight3-tests/pterodactyl-visual/" + UUID.randomUUID());
        FutureTask<Void> render = new FutureTask<>(() -> {
            BirdGame3 game = new BirdGame3(prefs);
            Field music = BirdGame3.class.getDeclaredField("musicEnabled"); music.setAccessible(true); music.set(game, false);
            Field sound = BirdGame3.class.getDeclaredField("sfxEnabled"); sound.setAccessible(true); sound.set(game, false);
            Method unlock = BirdGame3.class.getDeclaredMethod("unlockEverythingForDeveloperProfile"); unlock.setAccessible(true); unlock.invoke(game);
            Stage stage = new Stage();
            try {
                renderStyleComparison(new BirdGame3(prefs), output.resolve("05-style-comparison.png"));
                renderSelectionRoster(game, output.resolve("06-roster-selection.png"));
                renderSmashSelections(game, stage, output);
                game.showFlockRun(stage);
                Parent root = stage.getScene().getRoot(); root.applyCss(); root.layout();
                assertEquals(BirdGame3.BirdType.values().length, root.lookup("#flockRunRoster").lookupAll(".button").size());
                ((Button) root.lookup("#flockRunBird-PTERODACTYL")).fire();
                snapshot(stage, output.resolve("01-base-selection.png"));
                Button skin = stage.getScene().getRoot().lookupAll(".button").stream().filter(Button.class::isInstance)
                        .map(Button.class::cast).filter(button -> button.getText().startsWith("SKIN:")).findFirst().orElseThrow();
                skin.fire();
                assertTrue(skin.getText().contains("Fossilized"));
                snapshot(stage, output.resolve("02-fossil-selection.png"));
                button(stage, "START").fire();
                snapshot(stage, output.resolve("03-route.png"));
                Button safe = stage.getScene().getRoot().lookupAll(".button").stream().filter(Button.class::isInstance)
                        .map(Button.class::cast).filter(button -> button.getAccessibleText() != null
                                && button.getAccessibleText().startsWith("Sheltered passage")).findFirst().orElseThrow();
                safe.fire(); game.timer.stop();
                assertTrue(game.flockRunMatchActive);
                assertEquals(BirdGame3.BirdType.PTERODACTYL, game.players[0].type);
                assertTrue(game.players[0].isClassicSkin);
                game.timer.handle(System.nanoTime());
                snapshot(stage, output.resolve("04-fossil-arena.png"));
            } finally { if (game.timer != null) game.timer.stop(); stage.close(); }
            return null;
        });
        Platform.runLater(render);
        try { render.get(45, TimeUnit.SECONDS); }
        finally { prefs.removeNode(); Platform.exit(); }
    }
    private static Button button(Stage stage, String text) {
        return stage.getScene().getRoot().lookupAll(".button").stream().filter(Button.class::isInstance)
                .map(Button.class::cast).filter(button -> text.equals(button.getText())).findFirst().orElseThrow();
    }
    private static void snapshot(Stage stage, Path path) throws Exception {
        stage.getScene().getRoot().applyCss(); stage.getScene().getRoot().layout();
        WritableImage image = stage.getScene().snapshot(null);
        writeImage(image, path);
    }
    private static void renderSmashSelections(BirdGame3 game, Stage stage, Path output) throws Exception {
        Field picks = BirdGame3.class.getDeclaredField("fightSetupSelection"); picks.setAccessible(true);
        FightSetupSelectionState selection = (FightSetupSelectionState) picks.get(game);
        selection.selectBird(0, BirdGame3.BirdType.PTERODACTYL);
        selection.selectBird(1, BirdGame3.BirdType.PIGEON);
        game.activePlayers = 2;
        Method show = BirdGame3.class.getDeclaredMethod("showFightSetup", Stage.class); show.setAccessible(true);
        show.invoke(game, stage);
        snapshot(stage, output.resolve("07-smash-base.png"));
        selection.setSelectedSkinKey(0, "CLASSIC_SKIN_PTERODACTYL");
        show.invoke(game, stage);
        snapshot(stage, output.resolve("08-smash-fossil.png"));
    }
    private static void renderSelectionRoster(BirdGame3 game, Path path) throws Exception {
        Canvas sheet = new Canvas(1440, 850); GraphicsContext g = sheet.getGraphicsContext2D();
        g.setFill(Color.web("#151B22")); g.fillRect(0, 0, 1440, 850);
        g.setFill(Color.web("#FFE082")); g.setFont(Font.font("Arial", FontWeight.BOLD, 22));
        g.fillText("ROSTER COMPARISON · SELECTION PORTRAITS AND SMALL ICONS", 24, 34);
        Method draw = BirdGame3.class.getDeclaredMethod("drawRosterSprite", Canvas.class,
                BirdGame3.BirdType.class, String.class, boolean.class, boolean.class);
        draw.setAccessible(true);
        SnapshotParameters transparent = new SnapshotParameters();
        transparent.setFill(Color.TRANSPARENT);
        BirdGame3.BirdType[] types = BirdGame3.BirdType.values();
        for (int slot = 0; slot <= types.length; slot++) {
            boolean fossil = slot == types.length;
            BirdGame3.BirdType type = fossil ? BirdGame3.BirdType.PTERODACTYL : types[slot];
            double cx = slot % 6 * 240, cy = 58 + slot / 6 * 194;
            g.setFill(Color.web("#2C3642")); g.fillRoundRect(cx + 8, cy, 224, 180, 14, 14);
            Canvas portrait = new Canvas(128, 128), icon = new Canvas(64, 64);
            String skin = fossil ? "CLASSIC_SKIN_PTERODACTYL" : null;
            draw.invoke(game, portrait, type, skin, false, true);
            draw.invoke(game, icon, type, skin, false, true);
            g.drawImage(portrait.snapshot(transparent, null), cx + 14, cy + 8);
            g.drawImage(icon.snapshot(transparent, null), cx + 154, cy + 45);
            g.setFill(type == BirdGame3.BirdType.PTERODACTYL ? Color.web("#FFE082") : Color.web("#EAF0F5"));
            g.setFont(Font.font("Arial", FontWeight.BOLD, 13)); g.setTextAlign(TextAlignment.CENTER);
            g.fillText(fossil ? "FOSSILIZED PTERODACTYL" : type.name.toUpperCase(java.util.Locale.ROOT), cx + 120, cy + 160);
            g.setTextAlign(TextAlignment.LEFT);
        }
        writeImage(sheet.snapshot(null, null), path);
    }
    private static void renderStyleComparison(BirdGame3 game, Path path) throws Exception {
        Canvas canvas = new Canvas(1140, 570); GraphicsContext g = canvas.getGraphicsContext2D();
        g.setFill(Color.web("#151B22")); g.fillRect(0, 0, 1140, 570);
        g.setFill(Color.web("#FFE082")); g.setFont(Font.font("Arial", FontWeight.BOLD, 22));
        g.fillText("PTERODACTYL · ROSTER ART AND MOVE POSES", 24, 34);
        BirdGame3.BirdType[] types = {BirdGame3.BirdType.PIGEON, BirdGame3.BirdType.FALCON,
                BirdGame3.BirdType.BAT, BirdGame3.BirdType.KIWI, BirdGame3.BirdType.PTERODACTYL, BirdGame3.BirdType.PTERODACTYL};
        for (int slot = 0; slot < types.length; slot++) {
            Bird bird = new Bird(0, types[slot], slot, game); bird.sizeMultiplier = 1.35;
            bird.prepareVisualAuditPose(Bird.VisualAuditPose.IDLE);
            if (slot == 5) {
                Method skin = BirdGame3.class.getDeclaredMethod("applyPreviewSkinChoiceToBird", Bird.class, BirdGame3.BirdType.class, String.class);
                skin.setAccessible(true); skin.invoke(game, bird, types[slot], "CLASSIC_SKIN_PTERODACTYL");
            }
            drawStyleCell(g, bird, slot * 190, 58, slot == 5 ? "FOSSILIZED" : types[slot].name);
        }
        int[] phases = {PterodactylSpecials.GUST, PterodactylSpecials.LUNGE, PterodactylSpecials.UPDRAFT,
                PterodactylSpecials.WINDUP, PterodactylSpecials.CARRY, PterodactylSpecials.SLAM};
        String[] names = {"WING GUST", "BEAK LUNGE", "UPDRAFT", "SKY SNATCH", "CARRY", "EXTINCTION DIVE"};
        for (int slot = 0; slot < phases.length; slot++) {
            Bird bird = new Bird(0, BirdGame3.BirdType.PTERODACTYL, 0, game); bird.sizeMultiplier = 1.15;
            bird.prepareVisualAuditPose(Bird.VisualAuditPose.FLAP);
            bird.pterodactyl.phase = phases[slot]; bird.pterodactyl.elapsed = 8;
            bird.pterodactyl.ultimate = slot == 5;
            drawStyleCell(g, bird, slot * 190, 292, names[slot]);
        }
        writeImage(canvas.snapshot(null, null), path);
    }
    private static void drawStyleCell(GraphicsContext g, Bird bird, double cellX, double cellY, String label) {
        g.setFill(Color.web("#303B43")); g.fillRoundRect(cellX + 8, cellY, 174, 206, 16, 16);
        g.save(); g.translate(cellX + 95 - 40 * bird.sizeMultiplier, cellY + 59 - bird.y);
        bird.drawVisualAuditBody(g); g.restore();
        assertTrue(bird.visualFeatureGeometry().complete(), "Incomplete face for " + label);
        g.setFill(Color.web("#EAF0F5")); g.setFont(Font.font("Arial", FontWeight.BOLD, 13));
        g.setTextAlign(TextAlignment.CENTER); g.fillText(label, cellX + 95, cellY + 183); g.setTextAlign(TextAlignment.LEFT);
    }
    private static void writeImage(WritableImage image, Path path) throws Exception {
        int width = (int) image.getWidth(), height = (int) image.getHeight();
        int[] pixels = new int[width * height];
        image.getPixelReader().getPixels(0, 0, width, height, PixelFormat.getIntArgbInstance(), pixels, 0, width);
        BufferedImage buffered = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        buffered.setRGB(0, 0, width, height, pixels, 0, width); ImageIO.write(buffered, "png", path.toFile());
    }
}
