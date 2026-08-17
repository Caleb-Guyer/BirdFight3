package com.example.birdgame3;

import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.TextAlignment;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import static com.example.birdgame3.BirdGame3.MapType;
import static com.example.birdgame3.BirdGame3.MapVariant;
import static com.example.birdgame3.BirdGame3.StageChoice;

/** Draws the bundled HUD-free capture of a playable stage. It never consumes simulation randomness. */
final class StagePreviewRenderer {
    private static final String RESOURCE_ROOT = "/stage-previews/";
    private static final Map<String, Image> CAPTURE_CACHE = new HashMap<>();
    private static final Set<String> MISSING_CAPTURES = new HashSet<>();

    private StagePreviewRenderer() {
    }

    static void draw(Canvas canvas, StageChoice choice) {
        StageChoice stage = choice == null ? StageChoice.main(MapType.FOREST) : choice;
        GraphicsContext g = canvas.getGraphicsContext2D();
        double w = canvas.getWidth();
        double h = canvas.getHeight();
        g.clearRect(0, 0, w, h);
        g.save();
        g.beginPath();
        g.rect(0, 0, w, h);
        g.clip();

        if (!drawCapturedPreview(g, w, h, stage)) drawMissingCapture(g, w, h, stage);
        g.restore();
    }

    static void drawMainMap(Canvas canvas, MapType map) {
        draw(canvas, StageChoice.main(map == null ? MapType.FOREST : map));
    }

    static String resourceFileName(StageChoice choice) {
        StageChoice stage = choice == null ? StageChoice.main(MapType.FOREST) : choice;
        String prefix = stage.variant() == MapVariant.STANDARD ? "main-" : "variant-";
        String key = stage.variant() == MapVariant.STANDARD
                ? stage.map().name()
                : stage.variant().name();
        return prefix + key.toLowerCase(Locale.ROOT).replace('_', '-') + ".png";
    }

    static boolean capturedPreviewResourceExists(StageChoice choice) {
        return StagePreviewRenderer.class.getResource(RESOURCE_ROOT + resourceFileName(choice)) != null;
    }

    private static boolean drawCapturedPreview(GraphicsContext g, double w, double h, StageChoice stage) {
        String fileName = resourceFileName(stage);
        Image capture = CAPTURE_CACHE.get(fileName);
        if (capture == null && !MISSING_CAPTURES.contains(fileName)) {
            try (InputStream stream = StagePreviewRenderer.class.getResourceAsStream(RESOURCE_ROOT + fileName)) {
                if (stream == null) {
                    MISSING_CAPTURES.add(fileName);
                } else {
                    capture = new Image(stream);
                    if (capture.isError()) {
                        MISSING_CAPTURES.add(fileName);
                        capture = null;
                    } else {
                        CAPTURE_CACHE.put(fileName, capture);
                    }
                }
            } catch (IOException ignored) {
                MISSING_CAPTURES.add(fileName);
            }
        }
        if (capture == null) {
            return false;
        }
        // Preserve the capture's real proportions. Narrow cards crop the sides
        // instead of stretching buildings, birds, or platforms out of shape.
        SourceCrop crop = sourceCrop(capture.getWidth(), capture.getHeight(), w, h);
        g.drawImage(capture, crop.x(), crop.y(), crop.width(), crop.height(), 0, 0, w, h);
        g.setFill(new LinearGradient(0, 0, 0, 1, true, CycleMethod.NO_CYCLE,
                new Stop(0, Color.web("#000000", 0.03)),
                new Stop(0.72, Color.TRANSPARENT),
                new Stop(1, Color.web("#000000", 0.22))));
        g.fillRect(0, 0, w, h);
        return true;
    }

    static SourceCrop sourceCrop(double sourceWidth, double sourceHeight,
                                 double targetWidth, double targetHeight) {
        if (sourceWidth <= 0.0 || sourceHeight <= 0.0 || targetWidth <= 0.0 || targetHeight <= 0.0) {
            throw new IllegalArgumentException("Stage preview dimensions must be positive");
        }
        double sourceRatio = sourceWidth / sourceHeight;
        double targetRatio = targetWidth / targetHeight;
        if (targetRatio > sourceRatio) {
            double cropHeight = sourceWidth / targetRatio;
            return new SourceCrop(0.0, (sourceHeight - cropHeight) * 0.5, sourceWidth, cropHeight);
        }
        if (targetRatio < sourceRatio) {
            double cropWidth = sourceHeight * targetRatio;
            return new SourceCrop((sourceWidth - cropWidth) * 0.5, 0.0, cropWidth, sourceHeight);
        }
        return new SourceCrop(0.0, 0.0, sourceWidth, sourceHeight);
    }

    record SourceCrop(double x, double y, double width, double height) {
    }

    private static void drawMissingCapture(GraphicsContext g, double w, double h, StageChoice stage) {
        g.setFill(Color.web("#05070D"));
        g.fillRect(0, 0, w, h);
        g.setStroke(Color.web("#FF5252"));
        g.setLineWidth(Math.max(2.0, Math.min(w, h) * 0.025));
        g.strokeRect(4.0, 4.0, Math.max(0.0, w - 8.0), Math.max(0.0, h - 8.0));
        g.setTextAlign(TextAlignment.CENTER);
        g.setFill(Color.web("#FFCDD2"));
        g.setFont(Font.font("Consolas", FontWeight.BOLD, Math.max(10.0, Math.min(w, h) * 0.12)));
        g.fillText("MISSING STAGE CAPTURE", w * 0.5, h * 0.48);
        g.setFill(Color.web("#B0BEC5"));
        g.setFont(Font.font("Consolas", FontWeight.BOLD, Math.max(8.0, Math.min(w, h) * 0.075)));
        g.fillText(resourceFileName(stage).toUpperCase(Locale.ROOT), w * 0.5, h * 0.64);
    }

    static void drawRandom(Canvas canvas) {
        GraphicsContext g = canvas.getGraphicsContext2D();
        double w = canvas.getWidth();
        double h = canvas.getHeight();
        g.clearRect(0, 0, w, h);
        g.save();
        g.beginPath();
        g.rect(0, 0, w, h);
        g.clip();
        Color[] colors = {Color.web("#123C5A"), Color.web("#81243D"),
                Color.web("#255B3D"), Color.web("#452B70")};
        for (int quadrant = 0; quadrant < 4; quadrant++) {
            double x = quadrant % 2 * w * 0.5;
            double y = quadrant / 2 * h * 0.5;
            g.setFill(colors[quadrant]);
            g.fillRect(x, y, w * 0.5, h * 0.5);
            g.setStroke(Color.WHITE.deriveColor(0, 1, 1, 0.22));
            g.setLineWidth(Math.max(2.0, w * 0.008));
            g.strokeLine(x, y + h * 0.36, x + w * 0.5, y + h * 0.18);
            g.strokeLine(x + w * 0.08, y + h * 0.44, x + w * 0.40, y + h * 0.08);
        }
        g.setFill(Color.web("#05070D", 0.58));
        g.fillOval(w * 0.30, h * 0.12, w * 0.40, h * 0.76);
        g.setStroke(Color.web("#FFE66D"));
        g.setLineWidth(Math.max(4.0, w * 0.014));
        g.strokeOval(w * 0.30, h * 0.12, w * 0.40, h * 0.76);
        g.setTextAlign(TextAlignment.CENTER);
        g.setFont(Font.font("Impact", FontWeight.BOLD, Math.min(w, h) * 0.48));
        g.setFill(Color.WHITE);
        g.fillText("?", w * 0.5, h * 0.70);
        g.restore();
    }

    static Color accentFor(StageChoice stage) {
        if (stage != null && stage.variant() != MapVariant.STANDARD) {
            return switch (stage.variant().category) {
                case "Story Arenas" -> Color.web("#FFD54F");
                case "Boss Rush Arenas" -> Color.web("#CE93D8");
                case "Classic Routes" -> Color.web("#4DD0E1");
                default -> Color.web("#ECEFF1");
            };
        }
        MapType map = stage == null ? MapType.FOREST : stage.map();
        return switch (map) {
            case CITY, WORLDSEAM -> Color.web("#CE93D8");
            case SKYCLIFFS, DESERT, GLASSWIND_CAUSEWAY -> Color.web("#FFE082");
            case VIBRANT_JUNGLE, FOREST -> Color.web("#8BE28B");
            case CAVE, PRISON, SIGNAL_SPIRE -> Color.web("#90A4AE");
            case BATTLEFIELD, FROSTBITE_FJORD -> Color.web("#81D4FA");
            case BEACON_CROWN, SILENT_AMPHITHEATER -> Color.web("#B39DDB");
            case DOCK -> Color.web("#80DEEA");
            case ASHFALL_CATHEDRAL -> Color.web("#FF8A65");
            case RESONANCE_HALL -> Color.web("#FFD180");
        };
    }

}
