package com.example.birdgame3;

import javafx.scene.image.Image;

import javax.imageio.ImageIO;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;

/**
 * Loads production bird sprite sheets bundled under {@code /sprites}, with the
 * working directory's {@code sprites/} folder taking priority as a hot-reload
 * override. A bird gets sprite rendering when both {@code <bird>.png} and
 * {@code <bird>.properties} exist (bird enum name in lower case, e.g.
 * {@code pigeon.png}); everything else keeps the built-in vector art, so the
 * roster can migrate to real art one bird at a time.
 *
 * <p>{@code template.png} / {@code template.properties} (written on demand)
 * demonstrate the layout: one animation per row, fixed-size frames. Rename
 * them to a bird's name to see the pipeline run with placeholder art.
 */
final class BirdSpriteLibrary {
    static final String DIR_NAME = "sprites";
    private static final int TEMPLATE_FRAME_SIZE = 80;
    private static final int TEMPLATE_FRAMES_PER_ROW = 4;

    private static final Map<BirdGame3.BirdType, BirdSpriteSheet> SHEETS =
            new EnumMap<>(BirdGame3.BirdType.class);
    private static final Map<BirdGame3.BirdType, List<SkinVariant>> SKIN_VARIANTS =
            new EnumMap<>(BirdGame3.BirdType.class);

    /** A skin-specific sheet: {@code <bird>-<suffix>.png}, matched against skin keys. */
    record SkinVariant(String normalizedSuffix, BirdSpriteSheet sheet) {
    }

    private BirdSpriteLibrary() {
    }

    static Path externalDir() {
        return Path.of(System.getProperty("user.dir"), DIR_NAME);
    }

    static BirdSpriteSheet sheetFor(BirdGame3.BirdType type) {
        return sheetFor(type, null);
    }

    /**
     * The sheet for a bird wearing a skin: a variant whose filename suffix
     * matches the skin key wins (longest match first, so {@code -noir_pigeon}
     * beats {@code -noir}); otherwise the bird's base sheet.
     */
    static BirdSpriteSheet sheetFor(BirdGame3.BirdType type, String skinKey) {
        if (type == null) {
            return null;
        }
        if (skinKey != null && !skinKey.isBlank()) {
            List<SkinVariant> variants = SKIN_VARIANTS.get(type);
            if (variants != null) {
                String normalizedKey = normalizeSkinToken(skinKey);
                SkinVariant best = null;
                for (SkinVariant variant : variants) {
                    if (skinSuffixMatches(variant.normalizedSuffix(), normalizedKey)
                            && (best == null || variant.normalizedSuffix().length() > best.normalizedSuffix().length())) {
                        best = variant;
                    }
                }
                if (best != null) {
                    return best.sheet();
                }
            }
        }
        return SHEETS.get(type);
    }

    /**
     * True when a filename suffix identifies a skin key. Both are reduced to
     * lower-case alphanumerics, so {@code pigeon-noir.png} matches the skin
     * key {@code NOIR_PIGEON_SKIN} and {@code eagle-sky_king.png} matches
     * {@code SKY_KING_EAGLE}.
     */
    static boolean skinSuffixMatches(String normalizedSuffix, String normalizedKey) {
        return !normalizedSuffix.isEmpty() && normalizedKey.contains(normalizedSuffix);
    }

    static String normalizeSkinToken(String value) {
        StringBuilder sb = new StringBuilder(value.length());
        for (int i = 0; i < value.length(); i++) {
            char c = Character.toLowerCase(value.charAt(i));
            if ((c >= 'a' && c <= 'z') || (c >= '0' && c <= '9')) {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    /** Rescans the sprites folder. Returns a user-facing summary, or null when the folder is absent/empty. */
    static String reload() {
        SHEETS.clear();
        SKIN_VARIANTS.clear();
        Path dir = externalDir();
        boolean externalDirectoryExists = Files.isDirectory(dir);
        int loaded = 0;
        int failed = 0;
        for (BirdGame3.BirdType type : BirdGame3.BirdType.values()) {
            String base = type.name().toLowerCase(Locale.ROOT);
            BirdSpriteSheet sheet = externalDirectoryExists
                    ? loadSheet(dir.resolve(base + ".png"), dir.resolve(base + ".properties"))
                    : null;
            if (sheet == LOAD_FAILED) {
                failed++;
                sheet = loadBundledSheet(base);
            } else if (sheet == null) {
                sheet = loadBundledSheet(base);
            }
            if (sheet == LOAD_FAILED) {
                failed++;
            } else if (sheet != null) {
                SHEETS.put(type, sheet);
                loaded++;
            }
            // Skin variants: <bird>-<suffix>.png alongside <bird>-<suffix>.properties.
            if (!externalDirectoryExists) {
                continue;
            }
            try (var files = Files.list(dir)) {
                for (Path png : files.filter(p -> {
                    String n = p.getFileName().toString().toLowerCase(Locale.ROOT);
                    return n.startsWith(base + "-") && n.endsWith(".png");
                }).toList()) {
                    String fileName = png.getFileName().toString();
                    String stem = fileName.substring(0, fileName.length() - 4);
                    String suffix = stem.substring(base.length() + 1);
                    BirdSpriteSheet variantSheet = loadSheet(png, dir.resolve(stem + ".properties"));
                    if (variantSheet == LOAD_FAILED) {
                        failed++;
                    } else if (variantSheet != null) {
                        SKIN_VARIANTS.computeIfAbsent(type, t -> new ArrayList<>())
                                .add(new SkinVariant(normalizeSkinToken(suffix), variantSheet));
                        loaded++;
                    }
                }
            } catch (IOException ignored) {
            }
        }
        if (loaded == 0 && failed == 0) {
            return null;
        }
        return "BIRD SPRITES: " + loaded + " LOADED" + (failed > 0 ? ", " + failed + " FAILED" : "");
    }

    private static BirdSpriteSheet loadBundledSheet(String base) {
        String imageResource = "/sprites/" + base + ".png";
        String propertiesResource = "/sprites/" + base + ".properties";
        try (InputStream imageIn = BirdSpriteLibrary.class.getResourceAsStream(imageResource);
             InputStream propsIn = BirdSpriteLibrary.class.getResourceAsStream(propertiesResource)) {
            if (imageIn == null || propsIn == null) {
                return null;
            }
            Properties props = new Properties();
            props.load(propsIn);
            Image image = new Image(imageIn);
            BirdSpriteSheet sheet = image.isError() ? null : BirdSpriteSheet.fromProperties(image, props);
            return sheet != null ? sheet : LOAD_FAILED;
        } catch (IOException | RuntimeException e) {
            return LOAD_FAILED;
        }
    }

    /** Sentinel distinguishing "files exist but are broken" from "no files". */
    private static final BirdSpriteSheet LOAD_FAILED =
            new BirdSpriteSheet(null, 1, 1, 1.0, Map.of());

    private static BirdSpriteSheet loadSheet(Path png, Path propsFile) {
        if (!Files.exists(png) || !Files.exists(propsFile)) {
            return null;
        }
        try (InputStream imageIn = Files.newInputStream(png);
             InputStream propsIn = Files.newInputStream(propsFile)) {
            Properties props = new Properties();
            props.load(propsIn);
            Image image = new Image(imageIn);
            BirdSpriteSheet sheet = image.isError() ? null : BirdSpriteSheet.fromProperties(image, props);
            return sheet != null ? sheet : LOAD_FAILED;
        } catch (IOException | RuntimeException e) {
            return LOAD_FAILED;
        }
    }

    /** Writes the demo template pair if absent; returns true when files were created. */
    static boolean writeTemplateIfMissing() {
        Path dir = externalDir();
        Path png = dir.resolve("template.png");
        Path props = dir.resolve("template.properties");
        if (Files.exists(png) && Files.exists(props)) {
            return false;
        }
        try {
            Files.createDirectories(dir);
            ImageIO.write(renderTemplateSheet(), "png", png.toFile());
            Files.writeString(props, templateProperties());
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    /**
     * Placeholder art: one row per animation state, each frame a simple bird
     * blob facing right, with a growing tick bar so playback is visible.
     */
    private static BufferedImage renderTemplateSheet() {
        int rows = BirdSpriteSheet.STATE_NAMES.size();
        int size = TEMPLATE_FRAME_SIZE;
        BufferedImage sheet = new BufferedImage(
                size * TEMPLATE_FRAMES_PER_ROW, size * rows, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = sheet.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        Color[] rowColors = {
                new Color(0x90CAF9), new Color(0x80CBC4), new Color(0xA5D6A7),
                new Color(0xFFF59D), new Color(0xEF9A9A), new Color(0xCE93D8),
                new Color(0xB0BEC5), new Color(0xFFCC80), new Color(0x757575)
        };
        for (int row = 0; row < rows; row++) {
            Color body = rowColors[row % rowColors.length];
            for (int frame = 0; frame < TEMPLATE_FRAMES_PER_ROW; frame++) {
                int ox = frame * size;
                int oy = row * size;
                double bob = Math.sin(frame * Math.PI / 2.0) * 4.0;
                g.setColor(body);
                g.fillOval(ox + 12, oy + 22 + (int) bob, 56, 44);
                g.setColor(body.darker());
                g.setStroke(new BasicStroke(2f));
                g.drawOval(ox + 12, oy + 22 + (int) bob, 56, 44);
                // wing flaps across the frames
                g.fillArc(ox + 20, oy + 28 + (int) bob, 30, 24, 30 + frame * 25, 160);
                // eye + beak, facing right
                g.setColor(Color.WHITE);
                g.fillOval(ox + 50, oy + 28 + (int) bob, 12, 12);
                g.setColor(Color.BLACK);
                g.fillOval(ox + 55, oy + 31 + (int) bob, 5, 5);
                g.setColor(new Color(0xFB8C00));
                g.fillPolygon(
                        new int[]{ox + 66, ox + 78, ox + 66},
                        new int[]{oy + 34 + (int) bob, oy + 39 + (int) bob, oy + 44 + (int) bob}, 3);
                // frame-progress bar so animation playback is obvious
                g.setColor(new Color(0, 0, 0, 90));
                g.fillRect(ox + 8, oy + size - 8, (size - 16) * (frame + 1) / TEMPLATE_FRAMES_PER_ROW, 4);
            }
            g.setColor(new Color(0, 0, 0, 120));
            g.drawString(BirdSpriteSheet.STATE_NAMES.get(row), 4, row * size + 14);
        }
        g.dispose();
        return sheet;
    }

    private static String templateProperties() {
        StringBuilder sb = new StringBuilder();
        sb.append("# BirdFight3 sprite sheet template\n");
        sb.append("# Rename template.png/.properties to a bird name (e.g. pigeon.png,\n");
        sb.append("# pigeon.properties) and press F12 in Training mode to see it in game.\n");
        sb.append("# Layout: one animation per row, frames left to right, art faces RIGHT.\n");
        sb.append("# Only 'idle' is required; missing states fall back automatically.\n");
        sb.append("#\n");
        sb.append("# Skin variants: pigeon-noir.png + pigeon-noir.properties is used when\n");
        sb.append("# the bird wears a skin whose key contains the suffix (case/underscore\n");
        sb.append("# insensitive). Birds without a matching variant use their base sheet.\n\n");
        sb.append("frameWidth=").append(TEMPLATE_FRAME_SIZE).append('\n');
        sb.append("frameHeight=").append(TEMPLATE_FRAME_SIZE).append('\n');
        sb.append("scale=1.0\n\n");
        for (int row = 0; row < BirdSpriteSheet.STATE_NAMES.size(); row++) {
            String state = BirdSpriteSheet.STATE_NAMES.get(row);
            sb.append(state).append(".row=").append(row).append('\n');
            sb.append(state).append(".frames=").append(TEMPLATE_FRAMES_PER_ROW).append('\n');
            sb.append(state).append(".ticksPerFrame=8\n");
            if (state.equals("attack") || state.equals("hitstun") || state.equals("ko")) {
                sb.append(state).append(".loop=false\n");
            }
            sb.append('\n');
        }
        return sb.toString();
    }
}
