package com.example.birdgame3;

import javafx.scene.image.Image;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;

/**
 * One bird's sprite sheet: an image laid out as a grid of fixed-size frames,
 * one animation per row, plus timing metadata parsed from a properties file.
 *
 * <p>Animation names match the bird animation states in lower case
 * ({@code idle}, {@code run}, {@code flap}, {@code fall}, {@code attack},
 * {@code hitstun}, {@code shield}, {@code dodge}, {@code ko}). Only
 * {@code idle} is required — missing states fall back along sensible chains
 * (e.g. {@code dodge} → {@code flap} → {@code idle}). Frame clocks advance on
 * sim ticks, so animations freeze during hitstop and replay identically.
 */
final class BirdSpriteSheet {

    record Animation(int row, int frames, int ticksPerFrame, boolean loop) {
        int frameIndexAt(long ticksInState) {
            if (frames <= 1) return 0;
            long step = Math.max(1, ticksPerFrame);
            long index = Math.max(0, ticksInState) / step;
            return loop ? (int) (index % frames) : (int) Math.min(index, frames - 1L);
        }
    }

    static final List<String> STATE_NAMES =
            List.of("idle", "run", "flap", "fall", "attack", "hitstun", "shield", "dodge", "ko");

    private static final Map<String, List<String>> FALLBACKS = Map.of(
            "run", List.of("idle"),
            "flap", List.of("fall", "idle"),
            "fall", List.of("flap", "idle"),
            "attack", List.of("idle"),
            "hitstun", List.of("idle"),
            "shield", List.of("idle"),
            "dodge", List.of("flap", "fall", "idle"),
            "ko", List.of("hitstun", "idle")
    );

    final Image image;
    final int frameWidth;
    final int frameHeight;
    final double scale;
    private final Map<String, Animation> animations;

    BirdSpriteSheet(Image image, int frameWidth, int frameHeight, double scale, Map<String, Animation> animations) {
        this.image = image;
        this.frameWidth = frameWidth;
        this.frameHeight = frameHeight;
        this.scale = scale;
        this.animations = animations;
    }

    /** Resolves an animation for a state, following the fallback chain. Null only if no idle exists. */
    Animation animationFor(String state) {
        Animation direct = animations.get(state);
        if (direct != null) return direct;
        for (String fallback : FALLBACKS.getOrDefault(state, List.of("idle"))) {
            Animation candidate = animations.get(fallback);
            if (candidate != null) return candidate;
        }
        return animations.get("idle");
    }

    /**
     * Parses sheet metadata. Returns null when required keys are missing or
     * malformed ({@code frameWidth}, {@code frameHeight}, and an {@code idle} row).
     */
    static BirdSpriteSheet fromProperties(Image image, Properties props) {
        int frameWidth = readInt(props, "frameWidth", -1);
        int frameHeight = readInt(props, "frameHeight", -1);
        double scale = readDouble(props, "scale", 1.0);
        if (frameWidth <= 0 || frameHeight <= 0 || scale <= 0) {
            return null;
        }
        Map<String, Animation> animations = new HashMap<>();
        for (String state : STATE_NAMES) {
            int row = readInt(props, state + ".row", -1);
            if (row < 0) continue;
            int frames = Math.max(1, readInt(props, state + ".frames", 1));
            int ticksPerFrame = Math.max(1, readInt(props, state + ".ticksPerFrame", 8));
            boolean loop = Boolean.parseBoolean(props.getProperty(state + ".loop", "true").trim().toLowerCase(Locale.ROOT));
            animations.put(state, new Animation(row, frames, ticksPerFrame, loop));
        }
        if (!animations.containsKey("idle")) {
            return null;
        }
        return new BirdSpriteSheet(image, frameWidth, frameHeight, scale, animations);
    }

    private static int readInt(Properties props, String key, int fallback) {
        String raw = props.getProperty(key);
        if (raw == null) return fallback;
        try {
            return Integer.parseInt(raw.trim());
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    private static double readDouble(Properties props, String key, double fallback) {
        String raw = props.getProperty(key);
        if (raw == null) return fallback;
        try {
            return Double.parseDouble(raw.trim());
        } catch (NumberFormatException e) {
            return fallback;
        }
    }
}
