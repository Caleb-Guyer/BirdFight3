package com.example.birdgame3;

import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.ArcType;

final class BirdBookUiSupport {
    private BirdBookUiSupport() {
    }

    static void drawLockedIcon(Canvas canvas, Color tint) {
        GraphicsContext g = canvas.getGraphicsContext2D();
        double w = canvas.getWidth();
        double h = canvas.getHeight();
        g.clearRect(0, 0, w, h);

        Color base = tint == null ? Color.web("#90A4AE") : tint;
        g.setFill(Color.web("#0F171F", 0.8));
        g.fillRoundRect(w * 0.08, h * 0.08, w * 0.84, h * 0.84, 18, 18);

        g.setStroke(base);
        g.setLineWidth(3);
        g.strokeRoundRect(w * 0.18, h * 0.18, w * 0.64, h * 0.64, 14, 14);

        g.setStroke(base.brighter());
        g.setLineWidth(5);
        g.strokeArc(w * 0.32, h * 0.22, w * 0.36, h * 0.3, 0, 180, ArcType.OPEN);
        g.setFill(base.deriveColor(0, 1, 1, 0.85));
        g.fillRoundRect(w * 0.32, h * 0.42, w * 0.36, h * 0.32, 10, 10);
        g.setFill(Color.web("#263238"));
        g.fillOval(w * 0.47, h * 0.53, w * 0.06, h * 0.1);
    }

    static void drawContinueIcon(Canvas canvas) {
        GraphicsContext g = canvas.getGraphicsContext2D();
        double w = canvas.getWidth();
        double h = canvas.getHeight();
        g.clearRect(0, 0, w, h);
        double size = Math.min(w, h) * 0.7;
        double x = (w - size) / 2.0;
        double y = (h - size) / 2.0;
        g.setFill(Color.web("#263238"));
        g.fillOval(x, y, size, size);
        g.setStroke(Color.web("#FFD54F"));
        g.setLineWidth(6);
        g.strokeOval(x, y, size, size);

        g.setStroke(Color.web("#FFE082"));
        g.setLineWidth(6);
        double pad = size * 0.18;
        g.strokeArc(x + pad, y + pad, size - pad * 2, size - pad * 2, 60, 260, ArcType.OPEN);

        double arrowX = x + size * 0.76;
        double arrowY = y + size * 0.3;
        g.setFill(Color.web("#FFE082"));
        g.fillPolygon(
                new double[]{arrowX, arrowX + size * 0.12, arrowX + size * 0.04},
                new double[]{arrowY, arrowY + size * 0.05, arrowY + size * 0.16},
                3
        );
    }

    static void drawCoinIcon(Canvas canvas) {
        GraphicsContext g = canvas.getGraphicsContext2D();
        double w = canvas.getWidth();
        double h = canvas.getHeight();
        g.clearRect(0, 0, w, h);

        double size = Math.min(w, h) * 0.62;
        double x = (w - size) / 2.0;
        double y = (h - size) / 2.0;

        double backSize = size * 0.9;
        double backX = x - size * 0.14;
        double backY = y + size * 0.12;
        g.setFill(Color.web("#F9A825"));
        g.fillOval(backX, backY, backSize, backSize);
        g.setStroke(Color.web("#F6C945"));
        g.setLineWidth(size * 0.06);
        g.strokeOval(backX, backY, backSize, backSize);

        g.setFill(Color.web("#FFD54F"));
        g.fillOval(x, y, size, size);
        g.setStroke(Color.web("#FFF59D"));
        g.setLineWidth(size * 0.08);
        g.strokeOval(x, y, size, size);

        g.setFill(Color.web("#F57F17"));
        double mark = size * 0.24;
        g.fillOval(x + size * 0.38, y + size * 0.38, mark, mark);
        g.setStroke(Color.web("#FFE082"));
        g.setLineWidth(size * 0.05);
        g.strokeOval(x + size * 0.38, y + size * 0.38, mark, mark);
    }

    static void drawMapBackdrop(Canvas canvas, BirdGame3.MapType map) {
        GraphicsContext g = canvas.getGraphicsContext2D();
        double w = canvas.getWidth();
        double h = canvas.getHeight();
        g.clearRect(0, 0, w, h);

        Color top;
        Color bottom;
        switch (map) {
            case CITY -> {
                top = Color.web("#0D2A52");
                bottom = Color.web("#311B92");
            }
            case SKYCLIFFS -> {
                top = Color.web("#5D4037");
                bottom = Color.web("#B3E5FC");
            }
            case VIBRANT_JUNGLE -> {
                top = Color.web("#0B3D24");
                bottom = Color.web("#2E7D32");
            }
            case DESERT -> {
                top = Color.web("#E08B3E");
                bottom = Color.web("#F6D089");
            }
            case CAVE -> {
                top = Color.web("#1A237E");
                bottom = Color.web("#263238");
            }
            case BATTLEFIELD -> {
                top = Color.web("#0D47A1");
                bottom = Color.web("#1E88E5");
            }
            case BEACON_CROWN -> {
                top = Color.web("#120C2B");
                bottom = Color.web("#3B1E54");
            }
            case DOCK -> {
                top = Color.web("#0F3047");
                bottom = Color.web("#2A6A83");
            }
            case FROSTBITE_FJORD -> {
                top = Color.web("#08142C");
                bottom = Color.web("#AEEBFF");
            }
            case ASHFALL_CATHEDRAL -> {
                top = Color.web("#09050A");
                bottom = Color.web("#F4511E");
            }
            case PRISON -> {
                top = Color.web("#071018");
                bottom = Color.web("#37474F");
            }
            case RESONANCE_HALL -> {
                top = Color.web("#090711");
                bottom = Color.web("#5A2342");
            }
            case SIGNAL_SPIRE -> {
                top = Color.web("#020612");
                bottom = Color.web("#3C577A");
            }
            case SILENT_AMPHITHEATER -> {
                top = Color.web("#020206");
                bottom = Color.web("#29243A");
            }
            default -> {
                top = Color.web("#1B5E20");
                bottom = Color.web("#4CAF50");
            }
        }

        LinearGradient gradient = new LinearGradient(0, 0, 0, 1, true, CycleMethod.NO_CYCLE,
                new Stop(0, top), new Stop(1, bottom));
        g.setFill(gradient);
        g.fillRect(0, 0, w, h);

        switch (map) {
            case CITY -> {
                g.setFill(Color.web("#101820", 0.75));
                double base = h * 0.78;
                double bw = w / 7.0;
                double[] heights = new double[]{0.32, 0.54, 0.4, 0.62, 0.36, 0.5, 0.42};
                for (int i = 0; i < heights.length; i++) {
                    double bh = h * heights[i];
                    g.fillRect(i * bw, base - bh, bw * 0.9, bh);
                }
                g.setFill(Color.web("#FFC107", 0.4));
                for (int i = 0; i < heights.length; i++) {
                    double bx = i * bw + bw * 0.2;
                    double by = base - h * heights[i] + h * 0.08;
                    for (int r = 0; r < 3; r++) {
                        g.fillRect(bx, by + r * h * 0.08, bw * 0.18, h * 0.03);
                    }
                }
            }
            case SKYCLIFFS -> {
                g.setFill(Color.web("#6D4C41", 0.75));
                g.fillPolygon(new double[]{0, w * 0.22, w * 0.44}, new double[]{h, h * 0.45, h}, 3);
                g.fillPolygon(new double[]{w * 0.35, w * 0.62, w * 0.88}, new double[]{h, h * 0.35, h}, 3);
                g.setFill(Color.web("#8D6E63", 0.65));
                g.fillPolygon(new double[]{w * 0.1, w * 0.35, w * 0.6}, new double[]{h, h * 0.55, h}, 3);
            }
            case VIBRANT_JUNGLE -> {
                g.setStroke(Color.web("#1B5E20", 0.8));
                g.setLineWidth(3);
                g.strokeLine(w * 0.15, 0, w * 0.25, h);
                g.strokeLine(w * 0.4, 0, w * 0.35, h);
                g.strokeLine(w * 0.7, 0, w * 0.78, h);
                g.setFill(Color.web("#2E7D32", 0.7));
                g.fillOval(w * 0.05, h * 0.7, w * 0.25, h * 0.25);
                g.fillOval(w * 0.7, h * 0.65, w * 0.25, h * 0.3);
            }
            case DESERT -> {
                g.setFill(Color.web("#F2B56B", 0.58));
                g.fillOval(-w * 0.08, h * 0.78, w * 0.4, h * 0.14);
                g.fillOval(w * 0.18, h * 0.76, w * 0.38, h * 0.12);
                g.fillOval(w * 0.48, h * 0.79, w * 0.24, h * 0.1);
                g.setFill(Color.web("#2AA4B7", 0.8));
                g.fillOval(w * 0.1, h * 0.8, w * 0.22, h * 0.07);
                g.setStroke(Color.web("#4E342E", 0.8));
                g.setLineWidth(3);
                g.strokeLine(w * 0.76, h * 0.88, w * 0.76, h * 0.5);
                g.strokeLine(w * 0.84, h * 0.88, w * 0.84, h * 0.42);
                g.setFill(Color.web("#9C5B2E", 0.76));
                g.fillRoundRect(w * 0.66, h * 0.6, w * 0.24, h * 0.16, 18, 18);
                g.setFill(Color.web("#7A4520", 0.35));
                g.fillRoundRect(w * 0.7, h * 0.54, w * 0.16, h * 0.06, 14, 14);
            }
            case CAVE -> {
                g.setFill(Color.web("#263238", 0.75));
                double spikeW = w / 6.0;
                for (int i = 0; i < 6; i++) {
                    double x = i * spikeW;
                    g.fillPolygon(new double[]{x, x + spikeW * 0.5, x + spikeW}, new double[]{0, h * 0.25, 0}, 3);
                }
                for (int i = 0; i < 5; i++) {
                    double x = i * spikeW + spikeW * 0.1;
                    g.fillPolygon(new double[]{x, x + spikeW * 0.5, x + spikeW}, new double[]{h, h * 0.75, h}, 3);
                }
            }
            case BATTLEFIELD -> {
                g.setFill(Color.web("#4E342E", 0.85));
                g.fillOval(w * 0.2, h * 0.65, w * 0.6, h * 0.3);
                g.setFill(Color.web("#2E7D32", 0.8));
                g.fillOval(w * 0.24, h * 0.62, w * 0.52, h * 0.22);
                g.setStroke(Color.web("#90CAF9", 0.5));
                g.setLineWidth(2.5);
                g.strokeLine(0, h * 0.62, w, h * 0.62);
            }
            case BEACON_CROWN -> {
                g.setFill(Color.web("#1A237E", 0.3));
                g.fillOval(w * 0.1, h * 0.08, w * 0.8, h * 0.55);
                g.setFill(Color.web("#4A148C", 0.82));
                g.fillPolygon(
                        new double[]{w * 0.18, w * 0.3, w * 0.42, w * 0.5, w * 0.58, w * 0.7, w * 0.82},
                        new double[]{h * 0.72, h * 0.34, h * 0.66, h * 0.22, h * 0.66, h * 0.34, h * 0.72},
                        7
                );
                g.setStroke(Color.web("#E1BEE7", 0.6));
                g.setLineWidth(3);
                g.strokeLine(w * 0.22, h * 0.72, w * 0.78, h * 0.72);
            }
            case DOCK -> {
                g.setFill(Color.web("#0A1F2C", 0.42));
                g.fillRect(0, h * 0.62, w, h * 0.38);
                g.setFill(Color.web("#12394D", 0.65));
                g.fillOval(w * 0.04, h * 0.68, w * 0.92, h * 0.22);

                g.setFill(Color.web("#3E2723", 0.92));
                g.fillRoundRect(w * 0.18, h * 0.58, w * 0.64, h * 0.12, 12, 12);
                g.setStroke(Color.web("#8D6E63", 0.9));
                g.setLineWidth(3);
                for (int i = 0; i < 6; i++) {
                    double px = w * (0.22 + i * 0.1);
                    g.strokeLine(px, h * 0.58, px, h * 0.7);
                }

                g.setStroke(Color.web("#5D4037", 0.9));
                g.setLineWidth(4);
                g.strokeLine(w * 0.32, h * 0.58, w * 0.32, h * 0.22);
                g.strokeLine(w * 0.68, h * 0.58, w * 0.68, h * 0.18);
                g.strokeLine(w * 0.24, h * 0.3, w * 0.44, h * 0.52);
                g.strokeLine(w * 0.76, h * 0.24, w * 0.54, h * 0.52);

                g.setFill(Color.web("#E6EE9C", 0.22));
                g.fillPolygon(
                        new double[]{w * 0.32, w * 0.48, w * 0.32},
                        new double[]{h * 0.24, h * 0.42, h * 0.56},
                        3
                );
            }
            case FROSTBITE_FJORD -> {
                g.setFill(Color.web("#64FFDA", 0.24));
                g.fillPolygon(
                        new double[]{0, w * 0.22, w * 0.46, w * 0.72, w},
                        new double[]{h * 0.28, h * 0.18, h * 0.34, h * 0.15, h * 0.3},
                        5
                );
                g.setFill(Color.web("#B388FF", 0.18));
                g.fillPolygon(
                        new double[]{w * 0.08, w * 0.34, w * 0.58, w * 0.86},
                        new double[]{h * 0.42, h * 0.24, h * 0.44, h * 0.25},
                        4
                );
                g.setFill(Color.web("#0A2942", 0.72));
                g.fillRect(0, h * 0.76, w, h * 0.24);
                g.setFill(Color.web("#E8FBFF", 0.92));
                g.fillRoundRect(w * 0.12, h * 0.62, w * 0.76, h * 0.12, 18, 18);
                g.setFill(Color.web("#80DEEA", 0.62));
                g.fillRoundRect(w * 0.18, h * 0.66, w * 0.64, h * 0.08, 14, 14);
                g.setFill(Color.web("#FFFFFF", 0.88));
                g.fillOval(w * 0.23, h * 0.55, w * 0.18, h * 0.16);
                g.fillOval(w * 0.58, h * 0.53, w * 0.22, h * 0.18);
            }
            case ASHFALL_CATHEDRAL -> {
                g.setFill(Color.web("#FF6D00", 0.22));
                g.fillOval(w * 0.22, h * 0.04, w * 0.56, h * 0.46);
                g.setFill(Color.web("#0D0810", 0.82));
                for (int i = 0; i < 5; i++) {
                    double x = w * (0.14 + i * 0.18);
                    g.fillRoundRect(x, h * 0.16, w * 0.08, h * 0.62, 8, 8);
                    g.fillPolygon(
                            new double[]{x - w * 0.04, x + w * 0.04, x + w * 0.12},
                            new double[]{h * 0.18, h * 0.02, h * 0.18},
                            3
                    );
                }
                g.setFill(Color.web("#FFB300", 0.74));
                g.fillPolygon(
                        new double[]{w * 0.5, w * 0.42, w * 0.48, w * 0.5, w * 0.52, w * 0.58},
                        new double[]{h * 0.22, h * 0.58, h * 0.52, h * 0.7, h * 0.52, h * 0.58},
                        6
                );
                g.setFill(Color.web("#FF3D00", 0.48));
                g.fillRoundRect(w * 0.14, h * 0.72, w * 0.72, h * 0.1, 12, 12);
                g.setFill(Color.web("#4DD0E1", 0.44));
                g.fillOval(w * 0.46, h * 0.38, w * 0.08, h * 0.1);
            }
            case PRISON -> {
                g.setFill(Color.web("#111C23", 0.92));
                g.fillRect(0, h * 0.36, w, h * 0.52);
                g.setFill(Color.web("#050A0E", 0.96));
                g.fillPolygon(
                        new double[]{0, 0, w * 0.35, w * 0.31, w * 0.25, 0},
                        new double[]{0, h * 0.34, h * 0.34, h * 0.2, h * 0.3, h * 0.42}, 6);
                g.fillPolygon(
                        new double[]{w, w, w * 0.65, w * 0.69, w * 0.75, w},
                        new double[]{0, h * 0.34, h * 0.34, h * 0.2, h * 0.3, h * 0.42}, 6);
                g.setStroke(Color.web("#607D8B", 0.9));
                g.setLineWidth(3);
                for (int side = 0; side < 2; side++) {
                    double startX = side == 0 ? w * 0.04 : w * 0.72;
                    for (int cell = 0; cell < 2; cell++) {
                        double x = startX + cell * w * 0.12;
                        g.setFill(Color.web("#020609"));
                        g.fillRoundRect(x, h * 0.34, w * 0.1, h * 0.32, 8, 8);
                        for (int bar = 0; bar < 4; bar++) {
                            double bx = x + w * (0.012 + bar * 0.025);
                            g.strokeLine(bx, h * 0.34, bx, h * 0.66);
                        }
                    }
                }
                g.setFill(Color.web("#050A0E"));
                g.fillRoundRect(w * 0.38, h * 0.28, w * 0.24, h * 0.44, 10, 10);
                g.setFill(Color.web("#00BCD4", 0.68));
                g.fillOval(w * 0.47, h * 0.38, w * 0.06, h * 0.09);
                g.setFill(Color.web("#B3E5FC", 0.1));
                g.fillPolygon(
                        new double[]{w * 0.2, w * 0.22, w * 0.58, w * 0.42},
                        new double[]{h * 0.16, h * 0.16, h * 0.78, h * 0.78}, 4);
                g.fillPolygon(
                        new double[]{w * 0.8, w * 0.78, w * 0.42, w * 0.58},
                        new double[]{h * 0.16, h * 0.16, h * 0.78, h * 0.78}, 4);
                g.setStroke(Color.web("#FFB300", 0.72));
                g.setLineWidth(4);
                g.strokeLine(0, h * 0.78, w, h * 0.78);
            }
            case RESONANCE_HALL -> {
                g.setFill(Color.web("#1A0C18", 0.94));
                g.fillRect(0, h * 0.60, w, h * 0.40);
                g.setStroke(Color.web("#D5A34B", 0.84));
                g.setLineWidth(4);
                g.strokeOval(w * 0.34, h * 0.08, w * 0.32, h * 0.42);
                for (int tier = 0; tier < 3; tier++) {
                    g.strokeRoundRect(w * 0.04, h * (0.20 + tier * 0.18), w * 0.20, h * 0.13, 10, 10);
                    g.strokeRoundRect(w * 0.76, h * (0.20 + tier * 0.18), w * 0.20, h * 0.13, 10, 10);
                }
            }
            case SIGNAL_SPIRE -> {
                g.setStroke(Color.web("#90A4AE", 0.92));
                g.setLineWidth(5);
                g.strokeLine(w * 0.5, 0, w * 0.5, h);
                for (int row = 0; row < 4; row++) {
                    double y = h * (0.16 + row * 0.18);
                    g.strokeLine(w * 0.28, y, w * 0.72, y);
                    g.strokeLine(w * 0.28, y, w * 0.5, y + h * 0.16);
                    g.strokeLine(w * 0.72, y, w * 0.5, y + h * 0.16);
                }
                g.setStroke(Color.web("#67E8F9", 0.65));
                g.strokeOval(w * 0.2, h * 0.20, w * 0.6, h * 0.24);
            }
            case SILENT_AMPHITHEATER -> {
                g.setStroke(Color.web("#FFE082", 0.74));
                g.setLineWidth(4);
                for (int tier = 0; tier < 4; tier++) {
                    double inset = w * (0.05 + tier * 0.08);
                    g.strokeArc(inset, h * (0.18 + tier * 0.08), w - inset * 2,
                            h * (0.70 - tier * 0.08), 8, 164, javafx.scene.shape.ArcType.OPEN);
                }
            }
            default -> {
                g.setFill(Color.web("#1B5E20", 0.75));
                g.fillPolygon(new double[]{0, w * 0.1, w * 0.2}, new double[]{h, h * 0.55, h}, 3);
                g.fillPolygon(new double[]{w * 0.15, w * 0.3, w * 0.45}, new double[]{h, h * 0.45, h}, 3);
                g.fillPolygon(new double[]{w * 0.5, w * 0.62, w * 0.74}, new double[]{h, h * 0.5, h}, 3);
                g.fillPolygon(new double[]{w * 0.7, w * 0.82, w}, new double[]{h, h * 0.58, h}, 3);
            }
        }

        g.setFill(Color.web("#000000", 0.12));
        g.fillRect(0, 0, w, h);
    }

    static void drawMapPreview(Canvas canvas, BirdGame3.MapType map) {
        GraphicsContext g = canvas.getGraphicsContext2D();
        double w = canvas.getWidth();
        double h = canvas.getHeight();
        g.clearRect(0, 0, w, h);
        drawMapBackdrop(canvas, map);

        g.setFill(Color.web("#ECEFF1", 0.8));
        if (map == BirdGame3.MapType.PRISON) {
            g.fillRoundRect(0, h * 0.74, w, h * 0.09, 8, 8);
            g.fillRoundRect(w * 0.10, h * 0.58, w * 0.25, h * 0.055, 7, 7);
            g.fillRoundRect(w * 0.65, h * 0.58, w * 0.25, h * 0.055, 7, 7);
            g.fillRoundRect(w * 0.33, h * 0.42, w * 0.34, h * 0.06, 7, 7);
            g.fillRoundRect(w * 0.19, h * 0.27, w * 0.13, h * 0.045, 6, 6);
            g.fillRoundRect(w * 0.68, h * 0.27, w * 0.13, h * 0.045, 6, 6);
            g.fillRoundRect(w * 0.43, h * 0.18, w * 0.14, h * 0.045, 6, 6);
        } else if (map == BirdGame3.MapType.RESONANCE_HALL) {
            g.fillRoundRect(w * 0.10, h * 0.70, w * 0.80, h * 0.08, 10, 10);
            g.fillRoundRect(w * 0.15, h * 0.50, w * 0.23, h * 0.055, 8, 8);
            g.fillRoundRect(w * 0.62, h * 0.50, w * 0.23, h * 0.055, 8, 8);
            g.fillRoundRect(w * 0.38, h * 0.53, w * 0.24, h * 0.055, 8, 8);
            g.fillRoundRect(w * 0.42, h * 0.25, w * 0.16, h * 0.045, 7, 7);
            g.setFill(Color.web("#FFE082", 0.92));
            for (double px : new double[]{0.24, 0.50, 0.76}) {
                g.fillRoundRect(w * px - 12, h * 0.68, 24, 5, 4, 4);
            }
        } else if (map == BirdGame3.MapType.SIGNAL_SPIRE) {
            g.fillRoundRect(w * 0.17, h * 0.72, w * 0.68, h * 0.065, 8, 8);
            g.fillRoundRect(w * 0.18, h * 0.55, w * 0.24, h * 0.045, 7, 7);
            g.fillRoundRect(w * 0.62, h * 0.47, w * 0.22, h * 0.045, 7, 7);
            g.fillRoundRect(w * 0.36, h * 0.34, w * 0.18, h * 0.042, 7, 7);
            g.fillRoundRect(w * 0.53, h * 0.20, w * 0.18, h * 0.042, 7, 7);
            g.setStroke(Color.web("#67E8F9", 0.82));
            g.setLineWidth(3.0);
            g.strokeLine(w * 0.08, h * 0.43, w * 0.92, h * 0.43);
        } else if (map == BirdGame3.MapType.SILENT_AMPHITHEATER) {
            g.fillRoundRect(w * 0.10, h * 0.72, w * 0.80, h * 0.075, 9, 9);
            g.fillRoundRect(w * 0.15, h * 0.58, w * 0.22, h * 0.05, 7, 7);
            g.fillRoundRect(w * 0.63, h * 0.58, w * 0.22, h * 0.05, 7, 7);
            g.fillRoundRect(w * 0.40, h * 0.52, w * 0.20, h * 0.055, 7, 7);
            g.fillRoundRect(w * 0.23, h * 0.38, w * 0.18, h * 0.045, 7, 7);
            g.fillRoundRect(w * 0.59, h * 0.38, w * 0.18, h * 0.045, 7, 7);
            g.setStroke(Color.web("#FFE082", 0.72));
            g.setLineWidth(3.0);
            g.strokeOval(w * 0.39, h * 0.38, w * 0.22, h * 0.28);
        } else {
            g.fillRoundRect(w * 0.1, h * 0.7, w * 0.3, h * 0.08, 10, 10);
            g.fillRoundRect(w * 0.6, h * 0.6, w * 0.28, h * 0.08, 10, 10);
            g.fillRoundRect(w * 0.32, h * 0.42, w * 0.36, h * 0.08, 10, 10);
        }
    }

    static Color mapAccentColor(BirdGame3.MapType map) {
        return switch (map) {
            case CITY -> Color.web("#5E35B1");
            case SKYCLIFFS -> Color.web("#8D6E63");
            case VIBRANT_JUNGLE -> Color.web("#388E3C");
            case DESERT -> Color.web("#D18841");
            case CAVE -> Color.web("#455A64");
            case BATTLEFIELD -> Color.web("#1E88E5");
            case BEACON_CROWN -> Color.web("#8E24AA");
            case DOCK -> Color.web("#26A69A");
            case FROSTBITE_FJORD -> Color.web("#4FC3F7");
            case ASHFALL_CATHEDRAL -> Color.web("#E64A19");
            case PRISON -> Color.web("#546E7A");
            case RESONANCE_HALL -> Color.web("#D5A34B");
            case SIGNAL_SPIRE -> Color.web("#67E8F9");
            case SILENT_AMPHITHEATER -> Color.web("#FFE082");
            default -> Color.web("#2E7D32");
        };
    }

    static BirdGame3.MapType originMapForBird(BirdGame3.BirdType type) {
        return switch (type) {
            case MOCKINGBIRD -> BirdGame3.MapType.RESONANCE_HALL;
            case PIGEON, RAVEN -> BirdGame3.MapType.CITY;
            case EAGLE, FALCON, RAZORBILL -> BirdGame3.MapType.SKYCLIFFS;
            case PENGUIN -> BirdGame3.MapType.FROSTBITE_FJORD;
            case PHOENIX -> BirdGame3.MapType.ASHFALL_CATHEDRAL;
            case BAT, VULTURE, OPIUMBIRD, HEISENBIRD -> BirdGame3.MapType.CAVE;
            case HUMMINGBIRD, TITMOUSE -> BirdGame3.MapType.VIBRANT_JUNGLE;
            case PELICAN, GOOSE -> BirdGame3.MapType.DOCK;
            case ROADRUNNER -> BirdGame3.MapType.DESERT;
            case KIWI -> BirdGame3.MapType.FOREST;
            default -> BirdGame3.MapType.FOREST;
        };
    }

    static String birdStatsLine(BirdGame3.BirdType type) {
        return "Power: " + type.power
                + " | Speed: " + String.format("%.1f", type.speed)
                + " | Jump: " + type.jumpHeight
                + " | Lift: " + String.format("%.2f", type.flyUpForce);
    }

    static String birdFunDescription(BirdGame3.BirdType type) {
        return switch (type) {
            case PIGEON -> "Rooftop regular who knows every shortcut and every rumor. Never looks lost, even when the sky is falling.";
            case EAGLE -> "Born to patrol the highest drafts and punish anyone below. Majestic until the dive starts, then it is all violence.";
            case FALCON -> "Precision hunter with a chip on its shoulder. It loves the cleanest hit and the loudest crowd reaction.";
            case PHOENIX -> "Flies like a blaze and lands like a firework. Somehow always returns, as if it is insulting the concept of defeat.";
            case HUMMINGBIRD -> "A blur with a sweet tooth and a short temper. Will duel you for a drop of nectar and win smiling.";
            case TURKEY -> "Big steps, bigger thumps. Treats the ground like an instrument and keeps the rhythm with shockwaves.";
            case ROOSTER -> "Morning alarm with a battle plan. He commands a rotating brood of chicks, throws them into fights, launches off them, and recalls the whole flock on demand.";
            case ROADRUNNER -> "A desert menace built around momentum. Moving fast powers up his hits and softens incoming damage; he also charges blitzes, ricochets through lanes, rides dust devils upward, and paints fake roads that turn enemy movement against them.";
            case PENGUIN -> "Charges belly slides, shoves icebergs, rockets upward, and builds snow forts. Cool, calm, and stubborn as a glacier.";
            case SHOEBILL -> "Stares too long, then decides. It dazes back-turned targets directly in front of its bill, winds up crushing bill thrusts, rides marsh reeds upward, and holds a stone-still statue counter.";
            case MOCKINGBIRD -> "Old friend of Caleb Bossk and owner of the Charles Lounge. Passed the Bossk Test to become a Bosskhead, then turned every fight into his stage.";
            case RAZORBILL -> "Cut-clean wings and sharper intent. Prefers clean lines, clean hits, and no wasted motion.";
            case GRINCHHAWK -> "Holiday menace with a grudge. Snatches hearts up close, rides a runaway sleigh, blasts upward with chimney flaps, and leaves fake presents where enemies least want them.";
            case VULTURE -> "Patient and dangerous, Vulture circles until the moment is right. \"You are lucky to be on my side. My crows could end you in seconds,\" he warns.";
            case OPIUMBIRD -> "Drifts in a haze and leaves trouble behind. Calm, then suddenly cruel when the cloud rolls in.";
            case HEISENBIRD -> "Blue-hatted and bald, Heisenbird cooks sky-blue crystals in a hidden roost. The coop whispers \"say my name\" when he lands, and he is the one who pecks.";
            case TITMOUSE -> "Tiny rocket with a fearless heart. Loves speed, hates standing still, and dares you to keep up.";
            case BAT -> "Night specialist who hears everything and hides in the shadows. It knows the cave better than the cave knows itself.";
            case PELICAN -> "Iron beak, iron will. Stores cargo in his pouch, trades mobility for weight, and hits like a loaded ship.";
            case RAVEN -> "A shadow on the skyline with a talent for misdirection. It appears, it hits, and then it is already gone.";
            case GOOSE -> "Territorial heavyweight with a long neck and no respect for personal space. It guards nests, shoves lanes, and turns one honk into a flock problem.";
            case KIWI -> "A grounded, stubborn brawler with a bill built for finding trouble. Kiwi probes fast, tunnels straight through a crowd, and plants both feet when the earth needs moving.";
        };
    }

    static String typeDisplayName(PowerUpType type) {
        return switch (type) {
            case HEALTH -> "Health";
            case SPEED -> "Speed Boost";
            case RAGE -> "Rage";
            case SHRINK -> "Shrink";
            case NEON -> "Neon Boost";
            case THERMAL -> "Thermal Rise";
            case VINE_GRAPPLE -> "Vine Grapple";
            case OVERCHARGE -> "Overcharge";
            case TITAN -> "Titan Form";
            case BROADSIDE -> "Broadside";
        };
    }

    static String powerUpDescription(PowerUpType type) {
        return switch (type) {
            case HEALTH -> "Instant +40 HP. Turns a losing duel into a second wind.";
            case SPEED -> "Big speed surge for a short time. Great for chases, escapes, and sudden flanks.";
            case RAGE -> "Double attack power for a short burst. Every hit feels like a hammer.";
            case SHRINK -> "Shrinks and weakens all enemies. Buy space, then punish hard.";
            case NEON -> "Hyper speed rush with extra power and mobility. The loudest pickup in the arena.";
            case THERMAL -> "Stronger lift and hang time. Float above the chaos and reset the fight.";
            case VINE_GRAPPLE -> "Summons one swing vine from the platform above you. Snap up, arc out, and launch from new angles.";
            case OVERCHARGE -> "Resets special cooldown and amps attacks. Perfect for turning a brawl.";
            case TITAN -> "Grow larger with boosted power and durability. You become the hazard.";
            case BROADSIDE -> "Legacy dockside cannon crate. Broken Harbor now uses a map lever that calls in pirate-ship bombs instead.";
        };
    }
}
