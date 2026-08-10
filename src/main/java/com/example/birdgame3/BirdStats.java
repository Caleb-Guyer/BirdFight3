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
            type.damageDealtMult = type.defaultDamageDealtMult;
            type.damageTakenMult = type.defaultDamageTakenMult;
            type.cooldownRate = type.defaultCooldownRate;
            type.ultimateRate = type.defaultUltimateRate;
        }
        BirdGame3.GRAVITY = BirdGame3.DEFAULT_GRAVITY;
        Bird.STARTING_HEALTH = Bird.DEFAULT_STARTING_HEALTH;
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
            Double damageDealtMult = readDouble(props, prefix + ".damageDealtMult");
            if (damageDealtMult != null) {
                type.damageDealtMult = clampMultiplier(damageDealtMult);
                applied++;
            }
            Double damageTakenMult = readDouble(props, prefix + ".damageTakenMult");
            if (damageTakenMult != null) {
                type.damageTakenMult = clampMultiplier(damageTakenMult);
                applied++;
            }
            Double cooldownRate = readDouble(props, prefix + ".cooldownRate");
            if (cooldownRate != null) {
                type.cooldownRate = clampMultiplier(cooldownRate);
                applied++;
            }
            Double ultimateRate = readDouble(props, prefix + ".ultimateRate");
            if (ultimateRate != null) {
                type.ultimateRate = clampMultiplier(ultimateRate);
                applied++;
            }
        }
        Double gravity = readDouble(props, "global.gravity");
        if (gravity != null) {
            BirdGame3.GRAVITY = Math.clamp(gravity, 0.05, 5.0);
            applied++;
        }
        Double startingHealth = readDouble(props, "global.startingHealth");
        if (startingHealth != null) {
            Bird.STARTING_HEALTH = Math.clamp(startingHealth, 10.0, 5000.0);
            applied++;
        }
        return applied;
    }

    /** Keeps combat multipliers inside a sane band so a typo can't zero out the sim. */
    private static double clampMultiplier(double value) {
        return Math.clamp(value, 0.1, 10.0);
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
        migrateLegacyShippedTuning(props);
        int applied = apply(props);
        return "BIRD STATS: " + applied + " OVERRIDES LOADED";
    }

    /**
     * The updater intentionally preserves this file. Upgrade only complete,
     * unmodified presets that shipped in an older build; changing even one of
     * these four values marks the bird's preset as player-owned.
     */
    private static void migrateLegacyShippedTuning(Properties props) {
        migrateLegacyPreset(props, "goose",
                new double[]{0.52, 1.55, 0.54, 0.46},
                new double[]{0.68, 1.35, 0.70, 0.62});
        migrateLegacyPreset(props, "bat",
                new double[]{0.82, 1.20, 0.82, 0.90},
                new double[]{0.95, 1.08, 0.95, 1.00});
        migrateLegacyPreset(props, "vulture",
                new double[]{0.70, 1.42, 0.70, 0.62},
                new double[]{0.84, 1.26, 0.84, 0.78});
        migrateLegacyPreset(props, "titmouse",
                new double[]{1.45, 0.68, 1.40, 1.25},
                new double[]{1.10, 0.92, 1.40, 1.25});
    }

    private static void migrateLegacyPreset(Properties props, String bird,
                                             double[] legacyValues, double[] replacementValues) {
        String[] stats = {"damageDealtMult", "damageTakenMult", "cooldownRate", "ultimateRate"};
        for (int i = 0; i < stats.length; i++) {
            Double value = readDouble(props, bird + "." + stats[i]);
            if (value == null || Double.compare(value, legacyValues[i]) != 0) {
                return;
            }
        }
        for (int i = 0; i < stats.length; i++) {
            props.setProperty(bird + "." + stats[i], Double.toString(replacementValues[i]));
        }
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
        sb.append("# Delete a line to fall back to the compiled default.\n");
        sb.append("#\n");
        sb.append("# The *Mult/*Rate keys are multipliers (1.0 = normal, clamped 0.1-10):\n");
        sb.append("#   damageDealtMult  - scales ALL damage the bird deals (attacks + specials)\n");
        sb.append("#   damageTakenMult  - scales damage received (0.8 = tankier, 1.2 = squishier)\n");
        sb.append("#   cooldownRate     - attack/special cooldown recovery speed (1.25 = 25% faster)\n");
        sb.append("#   ultimateRate     - ultimate meter charge speed\n");
        sb.append('\n');
        sb.append("# Global knobs\n");
        sb.append("global.gravity=").append(BirdGame3.DEFAULT_GRAVITY).append('\n');
        sb.append("global.startingHealth=").append(Bird.DEFAULT_STARTING_HEALTH).append('\n');
        sb.append('\n');
        for (BirdGame3.BirdType type : BirdGame3.BirdType.values()) {
            String prefix = key(type);
            sb.append("# ").append(type.name).append('\n');
            sb.append(prefix).append(".power=").append(type.defaultPower).append('\n');
            sb.append(prefix).append(".jumpHeight=").append(type.defaultJumpHeight).append('\n');
            sb.append(prefix).append(".speed=").append(type.defaultSpeed).append('\n');
            sb.append(prefix).append(".flyUpForce=").append(type.defaultFlyUpForce).append('\n');
            sb.append(prefix).append(".damageDealtMult=").append(type.defaultDamageDealtMult).append('\n');
            sb.append(prefix).append(".damageTakenMult=").append(type.defaultDamageTakenMult).append('\n');
            sb.append(prefix).append(".cooldownRate=").append(type.defaultCooldownRate).append('\n');
            sb.append(prefix).append(".ultimateRate=").append(type.defaultUltimateRate).append('\n');
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
