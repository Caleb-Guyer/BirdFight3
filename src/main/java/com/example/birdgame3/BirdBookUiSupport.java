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
        g.fillRoundRect(w * 0.1, h * 0.7, w * 0.3, h * 0.08, 10, 10);
        g.fillRoundRect(w * 0.6, h * 0.6, w * 0.28, h * 0.08, 10, 10);
        g.fillRoundRect(w * 0.32, h * 0.42, w * 0.36, h * 0.08, 10, 10);
    }

    static Color mapAccentColor(BirdGame3.MapType map) {
        return switch (map) {
            case CITY -> Color.web("#5E35B1");
            case SKYCLIFFS -> Color.web("#8D6E63");
            case VIBRANT_JUNGLE -> Color.web("#388E3C");
            case CAVE -> Color.web("#455A64");
            case BATTLEFIELD -> Color.web("#1E88E5");
            case BEACON_CROWN -> Color.web("#8E24AA");
            case DOCK -> Color.web("#26A69A");
            default -> Color.web("#2E7D32");
        };
    }

    static BirdGame3.MapType originMapForBird(BirdGame3.BirdType type) {
        return switch (type) {
            case PIGEON, MOCKINGBIRD, RAVEN -> BirdGame3.MapType.CITY;
            case EAGLE, FALCON, PENGUIN, RAZORBILL -> BirdGame3.MapType.SKYCLIFFS;
            case PHOENIX, BAT, VULTURE, OPIUMBIRD, HEISENBIRD -> BirdGame3.MapType.CAVE;
            case HUMMINGBIRD, TITMOUSE -> BirdGame3.MapType.VIBRANT_JUNGLE;
            case PELICAN -> BirdGame3.MapType.DOCK;
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
            case ROOSTER -> "Morning alarm with a battle plan. He calls three different chicks to swarm targets and keep the pressure on.";
            case PENGUIN -> "Slides more than it flies, but it still finds a way to win. Cool, calm, and stubborn as a glacier.";
            case SHOEBILL -> "Stares too long, strikes too fast. Marsh legends say it never blinks, only decides.";
            case MOCKINGBIRD -> "Old friend of Caleb Bossk and owner of the Charles Lounge. Passed the Bossk Test to become a Bosskhead, then turned every fight into his stage.";
            case RAZORBILL -> "Cut-clean wings and sharper intent. Prefers clean lines, clean hits, and no wasted motion.";
            case GRINCHHAWK -> "Holiday menace with a grudge. Brings chaos instead of gifts and calls it tradition.";
            case VULTURE -> "Patient and dangerous, Vulture circles until the moment is right. \"You are lucky to be on my side. My crows could end you in seconds,\" he warns.";
            case OPIUMBIRD -> "Drifts in a haze and leaves trouble behind. Calm, then suddenly cruel when the cloud rolls in.";
            case HEISENBIRD -> "Blue-hatted and bald, Heisenbird cooks sky-blue crystals in a hidden roost. The coop whispers \"say my name\" when he lands, and he is the one who pecks.";
            case TITMOUSE -> "Tiny rocket with a fearless heart. Loves speed, hates standing still, and dares you to keep up.";
            case BAT -> "Night specialist who hears everything and hides in the shadows. It knows the cave better than the cave knows itself.";
            case PELICAN -> "Iron beak, iron will. Hauls momentum like cargo and hits like a loaded ship.";
            case RAVEN -> "A shadow on the skyline with a talent for misdirection. It appears, it hits, and then it is already gone.";
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
