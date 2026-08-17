package com.example.birdgame3;

import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
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
        StagePreviewRenderer.drawMainMap(canvas, map);
    }

    static void drawMapPreview(Canvas canvas, BirdGame3.MapType map) {
        StagePreviewRenderer.drawMainMap(canvas, map);
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
