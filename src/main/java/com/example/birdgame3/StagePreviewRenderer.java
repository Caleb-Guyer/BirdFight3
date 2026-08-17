package com.example.birdgame3;

import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.effect.Glow;
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

/** Draws deterministic, stage-specific menu art. It never consumes simulation randomness. */
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

        if (drawCapturedPreview(g, w, h, stage)) {
            g.restore();
            return;
        }

        drawBackground(g, w, h, stage);
        if (stage.variant() == MapVariant.STANDARD) {
            drawMainGeometry(g, w, h, stage.map());
        } else {
            drawVariantGeometry(g, w, h, stage.variant());
        }
        drawAtmosphericFinish(g, w, h, accentFor(stage));
        g.restore();
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
        g.drawImage(capture, 0, 0, w, h);
        g.setFill(new LinearGradient(0, 0, 0, 1, true, CycleMethod.NO_CYCLE,
                new Stop(0, Color.web("#000000", 0.03)),
                new Stop(0.72, Color.TRANSPARENT),
                new Stop(1, Color.web("#000000", 0.22))));
        g.fillRect(0, 0, w, h);
        return true;
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

    private static void drawBackground(GraphicsContext g, double w, double h, StageChoice stage) {
        MapType map = stage.map();
        MapVariant variant = stage.variant();
        Color top;
        Color bottom;
        if (variant != MapVariant.STANDARD) {
            Color[] pair = variantPalette(variant);
            top = pair[0];
            bottom = pair[1];
        } else {
            Color[] pair = mainPalette(map);
            top = pair[0];
            bottom = pair[1];
        }
        g.setFill(new LinearGradient(0, 0, 0, 1, true, CycleMethod.NO_CYCLE,
                new Stop(0, top), new Stop(1, bottom)));
        g.fillRect(0, 0, w, h);

        for (int star = 0; star < 26; star++) {
            double x = Math.floorMod(star * 97, 521) / 521.0 * w;
            double y = Math.floorMod(star * 61, 211) / 211.0 * h * 0.64;
            double r = 1.1 + (star % 3) * 0.8;
            g.setFill(Color.WHITE.deriveColor(0, 1, 1, 0.15 + (star % 4) * 0.08));
            g.fillOval(x, y, r, r);
        }
        drawMapSilhouette(g, w, h, map, variant);
    }

    private static Color[] mainPalette(MapType map) {
        return switch (map) {
            case FOREST -> colors("#06180E", "#347044");
            case CITY -> colors("#060924", "#43265D");
            case SKYCLIFFS -> colors("#5C1834", "#E19462");
            case VIBRANT_JUNGLE -> colors("#052E2C", "#19945E");
            case DESERT -> colors("#75283C", "#F0A455");
            case CAVE -> colors("#050610", "#332047");
            case BATTLEFIELD -> colors("#0B173D", "#436FB0");
            case BEACON_CROWN -> colors("#050313", "#4B2365");
            case DOCK -> colors("#071C30", "#376B79");
            case FROSTBITE_FJORD -> colors("#031126", "#4F9EAD");
            case ASHFALL_CATHEDRAL -> colors("#11050A", "#A52C1C");
            case PRISON -> colors("#03070C", "#30434C");
            case RESONANCE_HALL -> colors("#100610", "#682B3D");
            case SIGNAL_SPIRE -> colors("#01050D", "#294A67");
            case SILENT_AMPHITHEATER -> colors("#020207", "#312A46");
            case GLASSWIND_CAUSEWAY -> colors("#020713", "#28738B");
            case WORLDSEAM -> colors("#020A13", "#42184C");
        };
    }

    private static Color[] variantPalette(MapVariant variant) {
        return switch (variant) {
            case CROWN_DUEL -> colors("#0C1124", "#7E5A34");
            case NULL_ROCK_DUEL -> colors("#020207", "#31143C");
            case SKYBREAK_SPIRES -> colors("#10162C", "#798CA8");
            case ASHFALL_REBIRTH -> colors("#10050A", "#C54720");
            case TITAN_DOCK -> colors("#06121E", "#425E66");
            case PARLIAMENT_ROOFTOPS -> colors("#020417", "#292D67");
            case CARRION_THRONE -> colors("#08140C", "#315122");
            case NULL_ROC_ASCENDING -> colors("#07030E", "#462352");
            case VOID_CROWN -> colors("#010104", "#21102D");
            case ROOFTOP_RELAY -> colors("#183B67", "#EB8858");
            case PEREGRINE_RUN -> colors("#102E57", "#9AD6EA");
            case TEMPEST_SUMMIT -> colors("#080C24", "#506A8C");
            case FROZEN_CALDERA -> colors("#06142A", "#75BDCC");
            case HEARTBLOOM_SANCTUARY -> colors("#071D1B", "#783662");
            case HARVEST_TRIBUNAL -> colors("#18110B", "#8A4D2B");
            case DAWNWATCH_BASTION -> colors("#17233E", "#E19A4A");
            case REDLINE_CANYON -> colors("#541E32", "#E56A3D");
            case LAST_ICE_SHELF -> colors("#031329", "#5F9EAE");
            case STILLWATER_MARSH -> colors("#061B1B", "#31594B");
            case OBSIDIAN_FOUNDRY -> colors("#08070B", "#6C2118");
            case STANDARD -> mainPalette(MapType.FOREST);
        };
    }

    private static Color[] colors(String top, String bottom) {
        return new Color[]{Color.web(top), Color.web(bottom)};
    }

    private static void drawMapSilhouette(GraphicsContext g, double w, double h,
                                          MapType map, MapVariant variant) {
        if (map == MapType.CITY) {
            g.setFill(Color.web("#050915", 0.82));
            for (int i = 0; i < 9; i++) {
                double bw = w / 8.0;
                double bh = h * (0.30 + (i * 37 % 31) / 100.0);
                g.fillRect(i * bw - 12, h - bh, bw * 0.84, bh);
                g.setFill(Color.web("#EC6CD7", 0.24));
                for (int row = 0; row < 4; row++) {
                    g.fillRect(i * bw + bw * 0.20, h - bh + 22 + row * 25, bw * 0.16, 7);
                }
                g.setFill(Color.web("#050915", 0.82));
            }
            g.setFill(Color.web("#E8EDFF", 0.34));
            g.fillOval(w * 0.68, h * 0.08, h * 0.24, h * 0.24);
        } else if (map == MapType.SKYCLIFFS || map == MapType.DESERT) {
            g.setFill(Color.web(map == MapType.DESERT ? "#5A261F" : "#26233A", 0.66));
            for (int i = -1; i < 7; i++) {
                double x = i * w * 0.19;
                g.fillPolygon(new double[]{x, x + w * 0.14, x + w * 0.30},
                        new double[]{h, h * (0.34 + (i & 1) * 0.13), h}, 3);
            }
        } else if (map == MapType.VIBRANT_JUNGLE || map == MapType.FOREST) {
            g.setFill(Color.web("#03120A", 0.68));
            for (int i = 0; i < 7; i++) {
                double x = w * (0.05 + i * 0.16);
                double top = h * (0.12 + (i % 3) * 0.09);
                g.fillRect(x, top, w * 0.065, h - top);
                g.fillOval(x - w * 0.10, top - h * 0.08, w * 0.27, h * 0.18);
            }
        } else if (map == MapType.CAVE) {
            g.setFill(Color.web("#080612", 0.90));
            for (int i = 0; i < 10; i++) {
                double x = i * w / 9.0;
                g.fillPolygon(new double[]{x - 30, x, x + 30}, new double[]{0, h * (0.20 + (i % 3) * 0.08), 0}, 3);
                g.fillPolygon(new double[]{x - 34, x, x + 34}, new double[]{h, h * (0.77 - (i % 2) * 0.09), h}, 3);
            }
        } else if (map == MapType.DOCK) {
            g.setFill(Color.web("#04101C", 0.76));
            g.fillRect(0, h * 0.68, w, h * 0.32);
            g.setStroke(Color.web("#91B5C5", 0.46));
            g.setLineWidth(4);
            for (double x : new double[]{w * 0.14, w * 0.48, w * 0.82}) {
                g.strokeLine(x, h * 0.18, x, h * 0.72);
                g.strokeLine(x, h * 0.18, x + w * 0.16, h * 0.42);
            }
        } else if (map == MapType.FROSTBITE_FJORD) {
            g.setFill(Color.web("#DDF8FF", 0.28));
            for (int i = 0; i < 6; i++) {
                double x = i * w * 0.20 - 20;
                g.fillPolygon(new double[]{x, x + w * 0.11, x + w * 0.22},
                        new double[]{h, h * (0.36 + (i % 2) * 0.13), h}, 3);
            }
            g.setStroke(Color.web("#70FFC8", 0.30));
            g.setLineWidth(13);
            g.strokeArc(w * 0.1, h * 0.02, w * 0.8, h * 0.48, 10, 160,
                    javafx.scene.shape.ArcType.OPEN);
        } else if (map == MapType.ASHFALL_CATHEDRAL) {
            g.setFill(Color.web("#09070B", 0.82));
            for (int i = 0; i < 5; i++) {
                double x = w * (0.12 + i * 0.19);
                g.fillRect(x, h * 0.24, w * 0.09, h * 0.62);
                g.fillPolygon(new double[]{x - w * 0.04, x + w * 0.045, x + w * 0.13},
                        new double[]{h * 0.25, h * 0.04, h * 0.25}, 3);
            }
            g.setFill(Color.web("#FF4B16", 0.42));
            g.fillRect(0, h * 0.82, w, h * 0.18);
        } else if (map == MapType.PRISON) {
            g.setFill(Color.web("#050A0E", 0.84));
            g.fillRect(w * 0.05, h * 0.22, w * 0.24, h * 0.70);
            g.fillRect(w * 0.71, h * 0.22, w * 0.24, h * 0.70);
            g.setStroke(Color.web("#90A4AE", 0.42));
            g.setLineWidth(4);
            for (int bar = 0; bar < 8; bar++) {
                double x = w * 0.08 + bar * w * 0.035;
                g.strokeLine(x, h * 0.30, x, h * 0.83);
                g.strokeLine(w - x, h * 0.30, w - x, h * 0.83);
            }
        } else if (map == MapType.GLASSWIND_CAUSEWAY) {
            g.setStroke(Color.web("#B7ECF5", 0.42));
            g.setLineWidth(6);
            for (double x : new double[]{w * 0.20, w * 0.80}) {
                g.strokeLine(x, h * 0.14, x, h * 0.82);
            }
            g.strokeArc(w * 0.18, h * 0.10, w * 0.64, h * 0.74, 0, 180,
                    javafx.scene.shape.ArcType.OPEN);
        } else if (map == MapType.WORLDSEAM) {
            g.setFill(Color.web("#050009", 0.88));
            g.fillPolygon(new double[]{w * 0.46, w * 0.54, w * 0.61, w * 0.53, w * 0.48, w * 0.39},
                    new double[]{0, 0, h, h, h * 0.53, h}, 6);
            g.setStroke(Color.web("#E4C8FF", 0.48));
            g.setLineWidth(7);
            g.strokePolyline(new double[]{w * 0.49, w * 0.54, w * 0.48, w * 0.53},
                    new double[]{0, h * 0.31, h * 0.63, h}, 4);
        } else if (map == MapType.RESONANCE_HALL) {
            g.setStroke(Color.web("#FFD180", 0.35));
            g.setLineWidth(6);
            for (int tier = 0; tier < 4; tier++) {
                g.strokeArc(w * (0.08 + tier * 0.05), h * (0.12 + tier * 0.06),
                        w * (0.84 - tier * 0.10), h * (0.74 - tier * 0.08), 7, 166,
                        javafx.scene.shape.ArcType.OPEN);
            }
        } else if (map == MapType.SIGNAL_SPIRE) {
            g.setStroke(Color.web("#B0BEC5", 0.46));
            g.setLineWidth(6);
            g.strokeLine(w * 0.5, 0, w * 0.5, h);
            for (int row = 0; row < 4; row++) {
                double y = h * (0.18 + row * 0.17);
                g.strokeLine(w * 0.24, y, w * 0.76, y);
                g.strokeLine(w * 0.24, y, w * 0.5, y + h * 0.15);
                g.strokeLine(w * 0.76, y, w * 0.5, y + h * 0.15);
            }
        } else if (map == MapType.SILENT_AMPHITHEATER) {
            g.setStroke(Color.web("#D1C4E9", 0.28));
            g.setLineWidth(7);
            for (int ring = 0; ring < 5; ring++) {
                double inset = ring * w * 0.06;
                g.strokeOval(inset, h * 0.08 + inset * 0.35, w - inset * 2, h * 0.92 - inset * 0.7);
            }
        } else if (map == MapType.BEACON_CROWN || map == MapType.BATTLEFIELD) {
            g.setFill(Color.web("#0A1022", 0.48));
            g.fillOval(w * 0.19, h * 0.31, w * 0.62, h * 0.58);
            if (map == MapType.BEACON_CROWN) {
                g.setStroke(Color.web("#FFE082", 0.34));
                g.setLineWidth(9);
                g.strokePolygon(new double[]{w * 0.24, w * 0.36, w * 0.5, w * 0.64, w * 0.76},
                        new double[]{h * 0.72, h * 0.23, h * 0.67, h * 0.23, h * 0.72}, 5);
            }
        }
    }

    private static void drawMainGeometry(GraphicsContext g, double w, double h, MapType map) {
        Color fill = Color.web("#E8EEF3", 0.82);
        Color edge = accentFor(StageChoice.main(map));
        switch (map) {
            case FOREST, VIBRANT_JUNGLE -> {
                platform(g, w, h, 0.06, 0.73, 0.88, 0.09, fill, edge);
                platform(g, w, h, 0.12, 0.49, 0.22, 0.06, fill, edge);
                platform(g, w, h, 0.39, 0.37, 0.22, 0.06, fill, edge);
                platform(g, w, h, 0.67, 0.51, 0.22, 0.06, fill, edge);
            }
            case CITY -> {
                buildingDeck(g, w, h, 0.04, 0.66, 0.24, 0.31, edge);
                buildingDeck(g, w, h, 0.36, 0.48, 0.26, 0.49, edge);
                buildingDeck(g, w, h, 0.70, 0.61, 0.26, 0.36, edge);
                platform(g, w, h, 0.20, 0.33, 0.18, 0.05, fill, edge);
                platform(g, w, h, 0.63, 0.30, 0.18, 0.05, fill, edge);
            }
            case SKYCLIFFS -> {
                cliff(g, w, h, 0.02, 0.69, 0.28, edge);
                cliff(g, w, h, 0.35, 0.48, 0.28, edge);
                cliff(g, w, h, 0.70, 0.65, 0.27, edge);
                platform(g, w, h, 0.41, 0.25, 0.18, 0.05, fill, edge);
            }
            case DESERT -> {
                platform(g, w, h, 0.02, 0.76, 0.96, 0.09, Color.web("#7B4427"), edge);
                platform(g, w, h, 0.06, 0.58, 0.30, 0.06, fill, edge);
                platform(g, w, h, 0.64, 0.48, 0.28, 0.06, fill, edge);
            }
            case CAVE -> {
                platform(g, w, h, 0.02, 0.77, 0.96, 0.08, Color.web("#2B2735"), edge);
                platform(g, w, h, 0.10, 0.56, 0.22, 0.05, fill, edge);
                platform(g, w, h, 0.39, 0.40, 0.22, 0.05, fill, edge);
                platform(g, w, h, 0.68, 0.55, 0.22, 0.05, fill, edge);
            }
            case BATTLEFIELD -> floatingIsland(g, w, h, 0.15, 0.66, 0.70, edge);
            case BEACON_CROWN -> {
                platform(g, w, h, 0.08, 0.70, 0.84, 0.09, Color.web("#21162F"), edge);
                platform(g, w, h, 0.20, 0.48, 0.20, 0.05, fill, edge);
                platform(g, w, h, 0.42, 0.34, 0.16, 0.05, fill, edge);
                platform(g, w, h, 0.62, 0.48, 0.20, 0.05, fill, edge);
            }
            case DOCK -> {
                platform(g, w, h, 0.03, 0.74, 0.40, 0.08, Color.web("#283E48"), edge);
                platform(g, w, h, 0.56, 0.65, 0.41, 0.08, Color.web("#283E48"), edge);
                platform(g, w, h, 0.30, 0.46, 0.35, 0.06, fill, edge);
                platform(g, w, h, 0.14, 0.29, 0.18, 0.05, fill, edge);
            }
            case FROSTBITE_FJORD -> glacier(g, w, h, edge);
            case ASHFALL_CATHEDRAL -> cathedral(g, w, h, edge, false);
            case PRISON -> {
                platform(g, w, h, 0.02, 0.78, 0.96, 0.08, Color.web("#263238"), edge);
                platform(g, w, h, 0.08, 0.55, 0.25, 0.05, fill, edge);
                platform(g, w, h, 0.38, 0.40, 0.24, 0.05, fill, edge);
                platform(g, w, h, 0.67, 0.55, 0.25, 0.05, fill, edge);
            }
            case RESONANCE_HALL -> symmetricHall(g, w, h, edge);
            case SIGNAL_SPIRE -> spirePlatforms(g, w, h, edge);
            case SILENT_AMPHITHEATER -> amphitheaterPlatforms(g, w, h, edge);
            case GLASSWIND_CAUSEWAY -> bridge(g, w, h, edge);
            case WORLDSEAM -> worldseam(g, w, h, edge);
        }
    }

    private static void drawVariantGeometry(GraphicsContext g, double w, double h, MapVariant variant) {
        Color edge = accentFor(new StageChoice(variant.baseMap, variant));
        Color pale = Color.web("#EEF5F7", 0.84);
        switch (variant) {
            case CROWN_DUEL -> {
                platform(g, w, h, 0.16, 0.68, 0.68, 0.08, Color.web("#242633"), edge);
                platform(g, w, h, 0.05, 0.80, 0.16, 0.05, pale, edge);
                platform(g, w, h, 0.79, 0.80, 0.16, 0.05, pale, edge);
            }
            case NULL_ROCK_DUEL -> altar(g, w, h, edge, false);
            case SKYBREAK_SPIRES -> {
                cliff(g, w, h, 0.04, 0.72, 0.23, edge);
                cliff(g, w, h, 0.38, 0.48, 0.24, edge);
                cliff(g, w, h, 0.73, 0.70, 0.23, edge);
                vent(g, w, h, 0.32, 0.76, edge);
                vent(g, w, h, 0.68, 0.76, edge);
            }
            case ASHFALL_REBIRTH -> cathedral(g, w, h, edge, true);
            case TITAN_DOCK -> dreadnought(g, w, h, edge);
            case PARLIAMENT_ROOFTOPS -> parliament(g, w, h, edge);
            case CARRION_THRONE -> jungleThrone(g, w, h, edge);
            case NULL_ROC_ASCENDING -> ascendingCrown(g, w, h, edge);
            case VOID_CROWN -> altar(g, w, h, edge, true);
            case ROOFTOP_RELAY -> rooftopRelay(g, w, h, edge);
            case PEREGRINE_RUN -> peregrineRun(g, w, h, edge);
            case TEMPEST_SUMMIT -> tempest(g, w, h, edge);
            case FROZEN_CALDERA -> frozenCaldera(g, w, h, edge);
            case HEARTBLOOM_SANCTUARY -> heartbloom(g, w, h, edge);
            case HARVEST_TRIBUNAL -> harvest(g, w, h, edge);
            case DAWNWATCH_BASTION -> bastion(g, w, h, edge);
            case REDLINE_CANYON -> redline(g, w, h, edge);
            case LAST_ICE_SHELF -> glacier(g, w, h, edge);
            case STILLWATER_MARSH -> stillwater(g, w, h, edge);
            case OBSIDIAN_FOUNDRY -> foundry(g, w, h, edge);
            case STANDARD -> drawMainGeometry(g, w, h, variant.baseMap);
        }
    }

    private static void platform(GraphicsContext g, double w, double h, double x, double y,
                                 double pw, double ph, Color fill, Color edge) {
        double px = x * w;
        double py = y * h;
        double width = pw * w;
        double height = ph * h;
        g.setFill(Color.web("#02040A", 0.34));
        g.fillRoundRect(px + 4, py + 5, width, height, 12, 12);
        g.setFill(fill);
        g.fillRoundRect(px, py, width, height, 12, 12);
        g.setStroke(edge.deriveColor(0, 1, 1, 0.92));
        g.setLineWidth(Math.max(2.0, h * 0.012));
        g.strokeRoundRect(px, py, width, height, 12, 12);
        g.setFill(edge.deriveColor(0, 1, 1, 0.35));
        g.fillRoundRect(px + 5, py + 4, Math.max(0, width - 10), Math.max(2, height * 0.18), 8, 8);
    }

    private static void buildingDeck(GraphicsContext g, double w, double h, double x, double y,
                                     double bw, double bh, Color edge) {
        g.setFill(Color.web("#111426", 0.96));
        g.fillRect(x * w, y * h, bw * w, bh * h);
        for (int row = 0; row < 4; row++) {
            g.setFill((row & 1) == 0 ? Color.web("#D452C7", 0.42) : Color.web("#4DD0E1", 0.28));
            g.fillRect((x + 0.05) * w, (y + 0.08 + row * 0.09) * h, bw * w * 0.18, h * 0.025);
        }
        platform(g, w, h, x - 0.01, y - 0.02, bw + 0.02, 0.055, Color.web("#283040"), edge);
    }

    private static void cliff(GraphicsContext g, double w, double h, double x, double y,
                              double cw, Color edge) {
        g.setFill(Color.web("#2A2732", 0.90));
        g.fillPolygon(new double[]{x * w, (x + cw) * w, (x + cw * 0.63) * w, (x + cw * 0.35) * w},
                new double[]{y * h, y * h, h, h}, 4);
        platform(g, w, h, x, y - 0.02, cw, 0.06, Color.web("#4A4650"), edge);
    }

    private static void floatingIsland(GraphicsContext g, double w, double h, double x, double y,
                                       double width, Color edge) {
        platform(g, w, h, x, y, width, 0.08, Color.web("#3A4652"), edge);
        g.setFill(Color.web("#252E38", 0.92));
        g.fillPolygon(new double[]{x * w, (x + width) * w, (x + width * 0.68) * w, (x + width * 0.34) * w},
                new double[]{(y + 0.08) * h, (y + 0.08) * h, h, h}, 4);
        platform(g, w, h, x + 0.10, y - 0.22, 0.20, 0.05, Color.web("#E8EEF3", 0.82), edge);
        platform(g, w, h, x + width - 0.30, y - 0.22, 0.20, 0.05, Color.web("#E8EEF3", 0.82), edge);
        platform(g, w, h, x + width * 0.5 - 0.10, y - 0.36, 0.20, 0.05, Color.web("#E8EEF3", 0.82), edge);
    }

    private static void glacier(GraphicsContext g, double w, double h, Color edge) {
        g.setFill(Color.web("#DDF8FF", 0.76));
        g.fillPolygon(new double[]{w * 0.04, w * 0.96, w * 0.82, w * 0.68, w * 0.48, w * 0.29, w * 0.16},
                new double[]{h * 0.73, h * 0.73, h, h * 0.91, h, h * 0.90, h}, 7);
        platform(g, w, h, 0.04, 0.69, 0.92, 0.08, Color.web("#DDF8FF", 0.86), edge);
        platform(g, w, h, 0.13, 0.49, 0.23, 0.05, Color.web("#EEF9FF", 0.84), edge);
        platform(g, w, h, 0.39, 0.34, 0.22, 0.05, Color.web("#EEF9FF", 0.84), edge);
        platform(g, w, h, 0.66, 0.49, 0.23, 0.05, Color.web("#EEF9FF", 0.84), edge);
    }

    private static void cathedral(GraphicsContext g, double w, double h, Color edge, boolean altarOnly) {
        if (!altarOnly) {
            platform(g, w, h, 0.04, 0.74, 0.92, 0.08, Color.web("#251B20"), edge);
        } else {
            platform(g, w, h, 0.13, 0.72, 0.74, 0.08, Color.web("#251B20"), edge);
        }
        platform(g, w, h, 0.15, 0.50, 0.24, 0.05, Color.web("#3A2A2D"), edge);
        platform(g, w, h, 0.61, 0.50, 0.24, 0.05, Color.web("#3A2A2D"), edge);
        platform(g, w, h, 0.40, 0.34, 0.20, 0.06, Color.web("#4A2D2C"), edge);
        g.setFill(Color.web("#FF6D00", 0.55));
        g.fillPolygon(new double[]{w * 0.45, w * 0.50, w * 0.55},
                new double[]{h * 0.72, h * 0.47, h * 0.72}, 3);
    }

    private static void symmetricHall(GraphicsContext g, double w, double h, Color edge) {
        platform(g, w, h, 0.08, 0.72, 0.84, 0.08, Color.web("#321B24"), edge);
        for (double x : new double[]{0.13, 0.64}) {
            platform(g, w, h, x, 0.48, 0.23, 0.05, Color.web("#F3ECE8", 0.82), edge);
            platform(g, w, h, x + 0.07, 0.28, 0.16, 0.045, Color.web("#F3ECE8", 0.82), edge);
        }
        platform(g, w, h, 0.40, 0.54, 0.20, 0.05, Color.web("#F3ECE8", 0.82), edge);
    }

    private static void spirePlatforms(GraphicsContext g, double w, double h, Color edge) {
        platform(g, w, h, 0.15, 0.74, 0.70, 0.07, Color.web("#253443"), edge);
        platform(g, w, h, 0.10, 0.55, 0.23, 0.05, Color.web("#DCE4E8", 0.82), edge);
        platform(g, w, h, 0.39, 0.39, 0.21, 0.05, Color.web("#DCE4E8", 0.82), edge);
        platform(g, w, h, 0.66, 0.23, 0.21, 0.05, Color.web("#DCE4E8", 0.82), edge);
    }

    private static void amphitheaterPlatforms(GraphicsContext g, double w, double h, Color edge) {
        platform(g, w, h, 0.08, 0.74, 0.84, 0.08, Color.web("#302C39"), edge);
        platform(g, w, h, 0.14, 0.56, 0.21, 0.05, Color.web("#E3DFE6", 0.76), edge);
        platform(g, w, h, 0.65, 0.56, 0.21, 0.05, Color.web("#E3DFE6", 0.76), edge);
        platform(g, w, h, 0.40, 0.39, 0.20, 0.05, Color.web("#E3DFE6", 0.76), edge);
    }

    private static void bridge(GraphicsContext g, double w, double h, Color edge) {
        platform(g, w, h, 0.06, 0.70, 0.88, 0.08, Color.web("#18313D"), edge);
        g.setStroke(Color.web("#8EBACA", 0.62));
        g.setLineWidth(5);
        for (double x = 0.10; x < 0.88; x += 0.09) {
            g.strokeLine(x * w, h * 0.78, (x + 0.07) * w, h * 0.94);
            g.strokeLine((x + 0.07) * w, h * 0.78, x * w, h * 0.94);
        }
        platform(g, w, h, 0.12, 0.48, 0.18, 0.05, Color.web("#DDEAF0", 0.82), edge);
        platform(g, w, h, 0.41, 0.34, 0.18, 0.05, Color.web("#DDEAF0", 0.82), edge);
        platform(g, w, h, 0.70, 0.48, 0.18, 0.05, Color.web("#DDEAF0", 0.82), edge);
    }

    private static void worldseam(GraphicsContext g, double w, double h, Color edge) {
        platform(g, w, h, 0.04, 0.70, 0.40, 0.08, Color.web("#10313A"), Color.web("#00E5FF"));
        platform(g, w, h, 0.56, 0.70, 0.40, 0.08, Color.web("#321837"), Color.web("#EA80FC"));
        platform(g, w, h, 0.12, 0.47, 0.22, 0.05, Color.web("#E4EEF0", 0.80), Color.web("#00E5FF"));
        platform(g, w, h, 0.66, 0.47, 0.22, 0.05, Color.web("#E4EEF0", 0.80), Color.web("#EA80FC"));
        g.setStroke(Color.web("#FFF59D", 0.72));
        g.setLineWidth(6);
        g.strokeOval(w * 0.21, h * 0.22, w * 0.12, h * 0.35);
        g.strokeOval(w * 0.67, h * 0.22, w * 0.12, h * 0.35);
    }

    private static void altar(GraphicsContext g, double w, double h, Color edge, boolean fragments) {
        platform(g, w, h, 0.34, 0.62, 0.32, 0.09, Color.web("#2E2138"), edge);
        platform(g, w, h, 0.42, 0.43, 0.16, 0.05, Color.web("#E8E2EA", 0.78), edge);
        if (fragments) {
            platform(g, w, h, 0.06, 0.73, 0.20, 0.06, Color.web("#2E2138"), edge);
            platform(g, w, h, 0.74, 0.73, 0.20, 0.06, Color.web("#2E2138"), edge);
            platform(g, w, h, 0.15, 0.35, 0.15, 0.05, Color.web("#2E2138"), edge);
            platform(g, w, h, 0.70, 0.35, 0.15, 0.05, Color.web("#2E2138"), edge);
        }
    }

    private static void vent(GraphicsContext g, double w, double h, double x, double y, Color edge) {
        g.setFill(edge.deriveColor(0, 1, 1, 0.20));
        g.fillOval((x - 0.055) * w, (y - 0.26) * h, w * 0.11, h * 0.28);
        g.setStroke(edge.deriveColor(0, 1, 1, 0.70));
        g.setLineWidth(3);
        g.strokeLine(x * w, y * h, x * w, (y - 0.22) * h);
    }

    private static void dreadnought(GraphicsContext g, double w, double h, Color edge) {
        g.setFill(Color.web("#202B32"));
        g.fillPolygon(new double[]{w * 0.10, w * 0.90, w * 0.79, w * 0.20},
                new double[]{h * 0.63, h * 0.63, h * 0.86, h * 0.86}, 4);
        platform(g, w, h, 0.09, 0.60, 0.82, 0.08, Color.web("#33434A"), edge);
        platform(g, w, h, 0.23, 0.43, 0.20, 0.05, Color.web("#DDE6E8", 0.78), edge);
        platform(g, w, h, 0.57, 0.43, 0.20, 0.05, Color.web("#DDE6E8", 0.78), edge);
        g.setStroke(edge.deriveColor(0, 1, 1, 0.62));
        g.setLineWidth(5);
        g.strokeLine(w * 0.5, h * 0.60, w * 0.5, h * 0.20);
    }

    private static void parliament(GraphicsContext g, double w, double h, Color edge) {
        buildingDeck(g, w, h, 0.05, 0.61, 0.23, 0.37, edge);
        buildingDeck(g, w, h, 0.38, 0.38, 0.24, 0.60, edge);
        buildingDeck(g, w, h, 0.72, 0.58, 0.23, 0.40, edge);
        platform(g, w, h, 0.23, 0.49, 0.19, 0.05, Color.web("#E9EDF0", 0.75), edge);
        platform(g, w, h, 0.58, 0.49, 0.19, 0.05, Color.web("#E9EDF0", 0.75), edge);
    }

    private static void jungleThrone(GraphicsContext g, double w, double h, Color edge) {
        platform(g, w, h, 0.09, 0.75, 0.82, 0.08, Color.web("#24412C"), edge);
        platform(g, w, h, 0.18, 0.56, 0.20, 0.05, Color.web("#B6D8B9", 0.72), edge);
        platform(g, w, h, 0.40, 0.40, 0.20, 0.05, Color.web("#B6D8B9", 0.72), edge);
        platform(g, w, h, 0.62, 0.56, 0.20, 0.05, Color.web("#B6D8B9", 0.72), edge);
        g.setStroke(Color.web("#A7D96D", 0.62));
        g.setLineWidth(5);
        g.strokeArc(w * 0.13, h * 0.10, w * 0.35, h * 0.70, 290, 165, javafx.scene.shape.ArcType.OPEN);
        g.strokeArc(w * 0.52, h * 0.10, w * 0.35, h * 0.70, 85, 165, javafx.scene.shape.ArcType.OPEN);
    }

    private static void ascendingCrown(GraphicsContext g, double w, double h, Color edge) {
        for (int step = 0; step < 6; step++) {
            double x = 0.07 + step * 0.145;
            double y = 0.75 - step * 0.105;
            platform(g, w, h, x, y, 0.17, 0.055, Color.web("#34233E"), edge);
        }
    }

    private static void rooftopRelay(GraphicsContext g, double w, double h, Color edge) {
        for (int roof = 0; roof < 5; roof++) {
            double x = 0.02 + roof * 0.20;
            double y = 0.72 - (roof % 3) * 0.12;
            buildingDeck(g, w, h, x, y, 0.17, 1.0 - y, edge);
        }
        g.setStroke(Color.web("#FFF59D", 0.72));
        g.setLineWidth(5);
        g.strokeLine(w * 0.08, h * 0.44, w * 0.92, h * 0.24);
    }

    private static void peregrineRun(GraphicsContext g, double w, double h, Color edge) {
        cliff(g, w, h, 0.04, 0.69, 0.25, edge);
        cliff(g, w, h, 0.38, 0.51, 0.24, edge);
        cliff(g, w, h, 0.72, 0.69, 0.24, edge);
        g.setStroke(Color.web("#F1FCFF", 0.38));
        g.setLineWidth(7);
        for (int lane = 0; lane < 3; lane++) {
            g.strokeLine(w * 0.12, h * (0.22 + lane * 0.10), w * 0.88, h * (0.40 + lane * 0.10));
        }
    }

    private static void tempest(GraphicsContext g, double w, double h, Color edge) {
        peregrineRun(g, w, h, edge);
        g.setStroke(Color.web("#EAF2FF", 0.82));
        g.setLineWidth(5);
        g.strokePolyline(new double[]{w * 0.18, w * 0.29, w * 0.24, w * 0.35},
                new double[]{h * 0.05, h * 0.24, h * 0.20, h * 0.43}, 4);
        g.strokePolyline(new double[]{w * 0.77, w * 0.68, w * 0.73, w * 0.62},
                new double[]{h * 0.02, h * 0.22, h * 0.18, h * 0.41}, 4);
    }

    private static void frozenCaldera(GraphicsContext g, double w, double h, Color edge) {
        cathedral(g, w, h, edge, true);
        g.setFill(Color.web("#E3FAFF", 0.52));
        g.fillPolygon(new double[]{w * 0.05, w * 0.22, w * 0.35}, new double[]{h * 0.82, h * 0.49, h * 0.82}, 3);
        g.fillPolygon(new double[]{w * 0.65, w * 0.80, w * 0.95}, new double[]{h * 0.82, h * 0.49, h * 0.82}, 3);
        vent(g, w, h, 0.24, 0.72, edge);
        vent(g, w, h, 0.76, 0.72, edge);
    }

    private static void heartbloom(GraphicsContext g, double w, double h, Color edge) {
        g.setStroke(Color.web("#80CBC4", 0.66));
        g.setLineWidth(12);
        for (double x : new double[]{0.18, 0.39, 0.61, 0.82}) {
            g.strokeLine(x * w, h * 0.78, x * w, h * (0.39 + Math.abs(0.5 - x) * 0.2));
            g.setFill(Color.web("#F48FB1", 0.78));
            g.fillOval((x - 0.10) * w, h * (0.31 + Math.abs(0.5 - x) * 0.2), w * 0.20, h * 0.14);
            platform(g, w, h, x - 0.09, 0.40 + Math.abs(0.5 - x) * 0.2,
                    0.18, 0.045, Color.web("#E8F5E9", 0.75), edge);
        }
    }

    private static void harvest(GraphicsContext g, double w, double h, Color edge) {
        platform(g, w, h, 0.09, 0.72, 0.82, 0.08, Color.web("#3E2B22"), edge);
        platform(g, w, h, 0.22, 0.48, 0.56, 0.08, Color.web("#6D4C41"), edge);
        for (double x : new double[]{0.16, 0.84}) {
            g.setFill(Color.web("#FFB74D", 0.78));
            g.fillOval((x - 0.035) * w, h * 0.48, w * 0.07, h * 0.16);
        }
    }

    private static void bastion(GraphicsContext g, double w, double h, Color edge) {
        platform(g, w, h, 0.07, 0.73, 0.86, 0.08, Color.web("#5A4935"), edge);
        for (double x : new double[]{0.13, 0.70}) {
            g.setFill(Color.web("#4A4D55", 0.92));
            g.fillRect(x * w, h * 0.36, w * 0.17, h * 0.37);
            platform(g, w, h, x - 0.02, 0.34, 0.21, 0.06, Color.web("#C9B279", 0.78), edge);
        }
        g.setStroke(Color.web("#FFE082", 0.82));
        g.setLineWidth(8);
        g.strokeOval(w * 0.42, h * 0.16, w * 0.16, h * 0.30);
    }

    private static void redline(GraphicsContext g, double w, double h, Color edge) {
        g.setStroke(Color.web("#E7C899", 0.76));
        g.setLineWidth(h * 0.10);
        g.strokePolyline(new double[]{w * 0.02, w * 0.29, w * 0.48, w * 0.72, w * 0.98},
                new double[]{h * 0.74, h * 0.56, h * 0.67, h * 0.43, h * 0.58}, 5);
        g.setStroke(Color.web("#3E3330"));
        g.setLineWidth(h * 0.065);
        g.strokePolyline(new double[]{w * 0.02, w * 0.29, w * 0.48, w * 0.72, w * 0.98},
                new double[]{h * 0.74, h * 0.56, h * 0.67, h * 0.43, h * 0.58}, 5);
        g.setStroke(edge);
        g.setLineWidth(4);
        g.strokeLine(w * 0.04, h * 0.71, w * 0.96, h * 0.55);
    }

    private static void stillwater(GraphicsContext g, double w, double h, Color edge) {
        g.setFill(Color.web("#0B2B28", 0.72));
        g.fillRect(0, h * 0.72, w, h * 0.28);
        g.setStroke(Color.web("#76543B", 0.86));
        g.setLineWidth(16);
        for (double x : new double[]{0.13, 0.37, 0.64, 0.88}) {
            g.strokeLine(x * w, h * 0.18, x * w, h * 0.90);
            g.strokeArc((x - 0.12) * w, h * 0.58, w * 0.24, h * 0.32, 5, 170,
                    javafx.scene.shape.ArcType.OPEN);
        }
        platform(g, w, h, 0.08, 0.68, 0.84, 0.06, Color.web("#3A513A"), edge);
        platform(g, w, h, 0.18, 0.46, 0.22, 0.05, Color.web("#BBC9AF", 0.72), edge);
        platform(g, w, h, 0.60, 0.46, 0.22, 0.05, Color.web("#BBC9AF", 0.72), edge);
    }

    private static void foundry(GraphicsContext g, double w, double h, Color edge) {
        platform(g, w, h, 0.06, 0.73, 0.88, 0.08, Color.web("#251D20"), edge);
        for (double x : new double[]{0.25, 0.50, 0.75}) {
            g.setFill(Color.web("#2A2529"));
            g.fillRect((x - 0.035) * w, h * 0.10, w * 0.07, h * 0.47);
            g.setFill(Color.web("#5D211D"));
            g.fillRect((x - 0.09) * w, h * 0.49, w * 0.18, h * 0.18);
            platform(g, w, h, x - 0.12, 0.35, 0.24, 0.05, Color.web("#3A3034"), edge);
        }
        g.setFill(Color.web("#FF3D00", 0.42));
        g.fillRect(0, h * 0.86, w, h * 0.14);
    }

    private static void drawAtmosphericFinish(GraphicsContext g, double w, double h, Color accent) {
        g.setFill(new LinearGradient(0, 0, 0, 1, true, CycleMethod.NO_CYCLE,
                new Stop(0, Color.TRANSPARENT), new Stop(0.72, Color.TRANSPARENT),
                new Stop(1, Color.web("#000000", 0.46))));
        g.fillRect(0, 0, w, h);
        g.setStroke(accent.deriveColor(0, 1, 1, 0.44));
        g.setLineWidth(Math.max(2.0, h * 0.012));
        g.strokeRoundRect(2, 2, w - 4, h - 4, 20, 20);
        g.setEffect(new Glow(0.25));
        g.setStroke(Color.WHITE.deriveColor(0, 1, 1, 0.20));
        g.setLineWidth(2.0);
        g.strokeLine(w * 0.03, h * 0.08, w * 0.42, h * 0.08);
        g.setEffect(null);
    }
}
