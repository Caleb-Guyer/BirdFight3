package com.example.birdgame3;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Properties;

/**
 * Per-bird balance stats as editable data instead of compiled constants.
 *
 * <p>Overrides live in {@value #FILE_NAME} next to the game (the working
 * directory). Keys are {@code <bird>.<stat>} with the bird's enum name in
 * lower case, e.g. {@code pigeon.speed=3.9}. Missing keys fall back to the
 * compiled defaults; malformed values are ignored. F12 in Training mode
 * writes a template on first press and hot-reloads the file after edits —
 * stats apply instantly because gameplay code reads them from the
 * {@link BirdGame3.BirdType} enum at use time.
 */
final class BirdStats {
    static final String FILE_NAME = "bird-stats.properties";

    private BirdStats() {
    }

    static Path externalFile() {
        return Path.of(System.getProperty("user.dir"), FILE_NAME);
    }

    static void resetToDefaults() {
        for (BirdGame3.BirdType type : BirdGame3.BirdType.values()) {
            type.power = type.defaultPower;
            type.jumpHeight = type.defaultJumpHeight;
            type.speed = type.defaultSpeed;
            type.flyUpForce = type.defaultFlyUpForce;
        }
    }

    /** Applies overrides from the given properties; returns how many were applied. */
    static int apply(Properties props) {
        int applied = 0;
        for (BirdGame3.BirdType type : BirdGame3.BirdType.values()) {
            String prefix = key(type);
            Integer power = readInt(props, prefix + ".power");
            if (power != null) {
                type.power = power;
                applied++;
            }
            Integer jumpHeight = readInt(props, prefix + ".jumpHeight");
            if (jumpHeight != null) {
                type.jumpHeight = jumpHeight;
                applied++;
            }
            Double speed = readDouble(props, prefix + ".speed");
            if (speed != null) {
                type.speed = speed;
                applied++;
            }
            Double flyUpForce = readDouble(props, prefix + ".flyUpForce");
            if (flyUpForce != null) {
                type.flyUpForce = flyUpForce;
                applied++;
            }
        }
        return applied;
    }

    /**
     * Resets to defaults, then applies overrides from the external file if present.
     * Returns a user-facing summary, or null when no override file exists.
     */
    static String reloadFromDisk() {
        return reload(externalFile());
    }

    static String reload(Path file) {
        resetToDefaults();
        if (!Files.exists(file)) {
            return null;
        }
        Properties props = new Properties();
        try (InputStream in = Files.newInputStream(file)) {
            props.load(in);
        } catch (IOException e) {
            return "BIRD STATS: FAILED TO READ " + file.getFileName();
        }
        int applied = apply(props);
        return "BIRD STATS: " + applied + " OVERRIDES LOADED";
    }

    /** Writes a template listing every stat at its default value; false if it already exists or fails. */
    static boolean writeTemplateIfMissing() {
        Path file = externalFile();
        if (Files.exists(file)) {
            return false;
        }
        return writeTemplate(file);
    }

    static boolean writeTemplate(Path file) {
        StringBuilder sb = new StringBuilder();
        sb.append("# BirdFight3 balance overrides\n");
        sb.append("# Edit values, then press F12 in Training mode to hot-reload.\n");
        sb.append("# Delete a line to fall back to the compiled default.\n\n");
        for (BirdGame3.BirdType type : BirdGame3.BirdType.values()) {
            String prefix = key(type);
            sb.append("# ").append(type.name).append('\n');
            sb.append(prefix).append(".power=").append(type.defaultPower).append('\n');
            sb.append(prefix).append(".jumpHeight=").append(type.defaultJumpHeight).append('\n');
            sb.append(prefix).append(".speed=").append(type.defaultSpeed).append('\n');
            sb.append(prefix).append(".flyUpForce=").append(type.defaultFlyUpForce).append('\n');
            sb.append('\n');
        }
        try {
            Files.writeString(file, sb.toString());
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    static String key(BirdGame3.BirdType type) {
        return type.name().toLowerCase(Locale.ROOT);
    }

    private static Integer readInt(Properties props, String key) {
        String raw = props.getProperty(key);
        if (raw == null) return null;
        try {
            return Integer.parseInt(raw.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static Double readDouble(Properties props, String key) {
        String raw = props.getProperty(key);
        if (raw == null) return null;
        try {
            return Double.parseDouble(raw.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
