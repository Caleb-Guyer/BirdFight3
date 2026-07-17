package com.example.birdgame3;

import javafx.application.Platform;
import javafx.scene.SnapshotParameters;
import javafx.scene.canvas.Canvas;
import javafx.scene.image.PixelReader;
import javafx.scene.image.WritableImage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.prefs.Preferences;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Optional focused visual audit. Run with scripts/visual-audit.cmd; it is not
 * included in the normal unit-test naming pattern and never starts a match.
 */
class BirdVisualAuditRun {
    private static final int ART_SIZE = 160;
    private static final int HUD_SIZE = 72;
    private static final int LABEL_WIDTH = 228;
    private static final int CELL_WIDTH = 168;
    private static final int ROW_HEIGHT = 190;
    private static final int HEADER_HEIGHT = 58;
    private static final int ROWS_PER_PAGE = 10;

    private enum View {
        PORTRAIT,
        HUD,
        IDLE,
        RUN,
        FLAP,
        ATTACK,
        HIT,
        KO
    }

    private record PixelBounds(int minX, int minY, int maxX, int maxY, int opaquePixels,
                               int borderPixels, long signature) {
        int width() {
            return maxX < minX ? 0 : maxX - minX + 1;
        }

        int height() {
            return maxY < minY ? 0 : maxY - minY + 1;
        }

        double centerX() {
            return (minX + maxX) * 0.5;
        }

        double centerY() {
            return (minY + maxY) * 0.5;
        }
    }

    private record RenderedView(BufferedImage image, PixelBounds bounds) {
    }

    private record AuditResult(Path outputDir, int pages, int renderCount,
                               List<String> failures, List<String> warnings) {
    }

    @Test
    @Timeout(value = 90, unit = TimeUnit.SECONDS)
    void renderEveryBirdAndSkinToReviewableContactSheets() throws Exception {
        System.setProperty("prism.order", "sw");
        Path outputDir = Path.of("audit", "visual").toAbsolutePath().normalize();
        Files.createDirectories(outputDir);
        deletePreviousOutputs(outputDir);

        CountDownLatch started = new CountDownLatch(1);
        Platform.startup(started::countDown);
        assertTrue(started.await(15, TimeUnit.SECONDS), "JavaFX did not start for the visual audit");

        FutureTask<AuditResult> renderTask = new FutureTask<>(() -> renderAudit(outputDir));
        Platform.runLater(renderTask);
        AuditResult result;
        try {
            result = renderTask.get(75, TimeUnit.SECONDS);
        } finally {
            Platform.exit();
        }

        System.out.println("Visual audit: " + result.renderCount + " renders across " + result.pages
                + " pages in " + result.outputDir);
        List<String> blockingFindings = new ArrayList<>(result.failures);
        if (Boolean.getBoolean("visualAudit.failOnFindings")) {
            blockingFindings.addAll(result.warnings);
        }
        assertTrue(blockingFindings.isEmpty(), () -> "Visual audit failed; see "
                + result.outputDir.resolve("visual-audit-report.md") + System.lineSeparator()
                + String.join(System.lineSeparator(), blockingFindings.stream().limit(12).toList()));
    }

    private static AuditResult renderAudit(Path outputDir) throws Exception {
        BirdSpriteLibrary.reload();
        BirdGame3 game = new BirdGame3(Preferences.userRoot().node(
                "/birdfight3-tests/visual-audit-run/" + UUID.randomUUID()));
        List<BirdGame3.VisualAuditSkin> entries = game.visualAuditSkins();
        List<String> failures = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        Map<String, Long> baseIdleSignatures = new HashMap<>();
        int renderCount = 0;

        int pages = (entries.size() + ROWS_PER_PAGE - 1) / ROWS_PER_PAGE;
        for (int pageIndex = 0; pageIndex < pages; pageIndex++) {
            int from = pageIndex * ROWS_PER_PAGE;
            int to = Math.min(entries.size(), from + ROWS_PER_PAGE);
            List<BirdGame3.VisualAuditSkin> pageEntries = entries.subList(from, to);
            BufferedImage page = createPage(pageEntries.size());
            Graphics2D graphics = page.createGraphics();
            configureGraphics(graphics);
            drawHeaders(graphics, pageIndex + 1, pages);

            for (int row = 0; row < pageEntries.size(); row++) {
                BirdGame3.VisualAuditSkin entry = pageEntries.get(row);
                int rowY = HEADER_HEIGHT + row * ROW_HEIGHT;
                drawRowLabel(graphics, entry, rowY);
                EnumMap<View, RenderedView> renders = new EnumMap<>(View.class);
                for (View view : View.values()) {
                    RenderedView rendered = renderView(game, entry, view);
                    renders.put(view, rendered);
                    renderCount++;
                    checkBounds(entry, view, rendered, failures, warnings);
                    drawCell(graphics, rendered.image, view.ordinal(), rowY,
                            rendered.bounds.borderPixels > 0);
                }

                long idleSignature = renders.get(View.IDLE).bounds.signature;
                String birdKey = entry.bird().name();
                if (entry.key() == null) {
                    baseIdleSignatures.put(birdKey, idleSignature);
                } else {
                    Long baseSignature = baseIdleSignatures.get(birdKey);
                    if (baseSignature != null && baseSignature == idleSignature) {
                        failures.add(entry.name() + " is pixel-identical to " + entry.bird().name
                                + " base art in IDLE; the skin likely fell back to the default design.");
                    }
                }
            }
            graphics.dispose();
            Path pagePath = outputDir.resolve(String.format("bird-skin-audit-%02d.png", pageIndex + 1));
            ImageIO.write(page, "png", pagePath.toFile());
        }

        writeReport(outputDir, entries.size(), pages, renderCount, failures, warnings);
        return new AuditResult(outputDir, pages, renderCount, List.copyOf(failures), List.copyOf(warnings));
    }

    private static RenderedView renderView(BirdGame3 game, BirdGame3.VisualAuditSkin entry, View view) {
        int size = view == View.HUD ? HUD_SIZE : ART_SIZE;
        Canvas canvas = new Canvas(size, size);
        if (view == View.PORTRAIT || view == View.HUD) {
            game.drawVisualAuditRosterSprite(canvas, entry);
        } else {
            game.drawVisualAuditCombatPose(canvas, entry, Bird.VisualAuditPose.valueOf(view.name()));
        }
        BufferedImage image = snapshot(canvas);
        if (view == View.PORTRAIT || view == View.HUD) {
            return new RenderedView(image, measure(image));
        }

        Canvas silhouetteCanvas = new Canvas(size, size);
        game.drawVisualAuditCombatSilhouette(
                silhouetteCanvas, entry, Bird.VisualAuditPose.valueOf(view.name()));
        return new RenderedView(image, measure(snapshot(silhouetteCanvas)));
    }

    private static BufferedImage snapshot(Canvas canvas) {
        int width = (int) canvas.getWidth();
        int height = (int) canvas.getHeight();
        SnapshotParameters parameters = new SnapshotParameters();
        parameters.setFill(javafx.scene.paint.Color.TRANSPARENT);
        WritableImage snapshot = new WritableImage(width, height);
        canvas.snapshot(parameters, snapshot);
        PixelReader pixels = snapshot.getPixelReader();
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                image.setRGB(x, y, pixels.getArgb(x, y));
            }
        }
        return image;
    }

    private static PixelBounds measure(BufferedImage image) {
        int minX = image.getWidth();
        int minY = image.getHeight();
        int maxX = -1;
        int maxY = -1;
        int opaque = 0;
        int border = 0;
        long signature = 0xcbf29ce484222325L;
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                int argb = image.getRGB(x, y);
                int alpha = argb >>> 24;
                signature ^= Integer.toUnsignedLong(argb);
                signature *= 0x100000001b3L;
                if (alpha <= 12) {
                    continue;
                }
                opaque++;
                minX = Math.min(minX, x);
                minY = Math.min(minY, y);
                maxX = Math.max(maxX, x);
                maxY = Math.max(maxY, y);
                if (x == 0 || y == 0 || x == image.getWidth() - 1 || y == image.getHeight() - 1) {
                    border++;
                }
            }
        }
        return new PixelBounds(minX, minY, maxX, maxY, opaque, border, signature);
    }

    private static void checkBounds(BirdGame3.VisualAuditSkin entry, View view, RenderedView rendered,
                                    List<String> failures, List<String> warnings) {
        PixelBounds bounds = rendered.bounds;
        BufferedImage image = rendered.image;
        String label = entry.name() + " / " + view;
        if (bounds.opaquePixels < 80 || bounds.width() == 0 || bounds.height() == 0) {
            failures.add(label + " rendered no meaningful visible art.");
            return;
        }
        if (bounds.borderPixels > 0) {
            String finding = label + " touches the " + touchedEdges(bounds, image)
                    + " canvas edge and may be clipped (" + bounds.borderPixels + " border pixels).";
            if (view == View.PORTRAIT || view == View.HUD || requiresCleanCombatFraming(entry)) {
                failures.add(finding);
            } else {
                warnings.add(finding);
            }
        }
        if (bounds.width() < image.getWidth() * 0.12 || bounds.height() < image.getHeight() * 0.12) {
            failures.add(label + " occupies less than 12% of its frame and is too small.");
        }

        double offsetX = Math.abs(bounds.centerX() - (image.getWidth() - 1) * 0.5) / image.getWidth();
        double offsetY = Math.abs(bounds.centerY() - (image.getHeight() - 1) * 0.5) / image.getHeight();
        if ((view == View.PORTRAIT || view == View.HUD) && (offsetX > 0.30 || offsetY > 0.30)) {
            failures.add(label + " is severely off-center (x=" + percent(offsetX) + ", y=" + percent(offsetY) + ").");
        } else if ((view == View.PORTRAIT || view == View.HUD) && (offsetX > 0.18 || offsetY > 0.18)) {
            warnings.add(label + " may need centering review (x=" + percent(offsetX) + ", y=" + percent(offsetY) + ").");
        }
        if (bounds.width() > image.getWidth() * 0.94 || bounds.height() > image.getHeight() * 0.94) {
            warnings.add(label + " uses over 94% of the frame and has little safety padding.");
        }
    }

    private static boolean requiresCleanCombatFraming(BirdGame3.VisualAuditSkin entry) {
        return entry.bird() == BirdGame3.BirdType.PIGEON
                || "NULL_ROCK_VULTURE".equals(entry.key())
                || "SKY_KING_EAGLE".equals(entry.key())
                || "STOCK_PHOTO_EAGLE".equals(entry.key())
                || "STOCK_PHOTO_TURKEY".equals(entry.key());
    }

    private static String percent(double ratio) {
        return Math.round(ratio * 100.0) + "%";
    }

    private static String touchedEdges(PixelBounds bounds, BufferedImage image) {
        List<String> edges = new ArrayList<>(4);
        if (bounds.minX == 0) edges.add("left");
        if (bounds.maxX == image.getWidth() - 1) edges.add("right");
        if (bounds.minY == 0) edges.add("top");
        if (bounds.maxY == image.getHeight() - 1) edges.add("bottom");
        return String.join("/", edges);
    }

    private static BufferedImage createPage(int rows) {
        int width = LABEL_WIDTH + View.values().length * CELL_WIDTH;
        int height = HEADER_HEIGHT + rows * ROW_HEIGHT;
        BufferedImage page = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = page.createGraphics();
        configureGraphics(graphics);
        graphics.setColor(new Color(12, 17, 24));
        graphics.fillRect(0, 0, width, height);
        graphics.dispose();
        return page;
    }

    private static void configureGraphics(Graphics2D graphics) {
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
    }

    private static void drawHeaders(Graphics2D graphics, int page, int pages) {
        graphics.setColor(new Color(26, 36, 48));
        graphics.fillRect(0, 0, LABEL_WIDTH + View.values().length * CELL_WIDTH, HEADER_HEIGHT);
        graphics.setColor(new Color(240, 244, 248));
        graphics.setFont(new Font("Segoe UI", Font.BOLD, 17));
        graphics.drawString("BIRD / SKIN", 18, 25);
        graphics.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        graphics.setColor(new Color(148, 163, 184));
        graphics.drawString("Visual audit " + page + " / " + pages, 18, 45);
        graphics.setFont(new Font("Segoe UI", Font.BOLD, 13));
        for (View view : View.values()) {
            int x = LABEL_WIDTH + view.ordinal() * CELL_WIDTH;
            graphics.setColor(new Color(240, 244, 248));
            int textWidth = graphics.getFontMetrics().stringWidth(view.name());
            graphics.drawString(view.name(), x + (CELL_WIDTH - textWidth) / 2, 34);
        }
    }

    private static void drawRowLabel(Graphics2D graphics, BirdGame3.VisualAuditSkin entry, int rowY) {
        graphics.setColor(new Color(17, 24, 33));
        graphics.fillRect(0, rowY, LABEL_WIDTH, ROW_HEIGHT);
        graphics.setColor(new Color(62, 78, 96));
        graphics.drawLine(0, rowY, LABEL_WIDTH + View.values().length * CELL_WIDTH, rowY);
        graphics.setColor(new Color(242, 245, 249));
        graphics.setFont(new Font("Segoe UI", Font.BOLD, 15));
        drawClamped(graphics, entry.name(), 18, rowY + 70, LABEL_WIDTH - 34);
        graphics.setColor(new Color(139, 154, 174));
        graphics.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        graphics.drawString(entry.bird().name, 18, rowY + 93);
        graphics.setFont(new Font("Consolas", Font.PLAIN, 10));
        drawClamped(graphics, entry.key() == null ? "BASE" : entry.key(), 18, rowY + 114, LABEL_WIDTH - 34);
    }

    private static void drawCell(Graphics2D graphics, BufferedImage art, int column, int rowY, boolean clipped) {
        int x = LABEL_WIDTH + column * CELL_WIDTH;
        graphics.setColor(new Color(20, 28, 38));
        graphics.fillRect(x, rowY, CELL_WIDTH, ROW_HEIGHT);
        graphics.setColor(new Color(31, 43, 56));
        for (int cy = rowY + 10; cy < rowY + ROW_HEIGHT - 10; cy += 16) {
            for (int cx = x + 4; cx < x + CELL_WIDTH - 4; cx += 16) {
                if ((((cx - x) / 16) + ((cy - rowY) / 16)) % 2 == 0) {
                    graphics.fillRect(cx, cy, 16, 16);
                }
            }
        }
        int drawX = x + (CELL_WIDTH - art.getWidth()) / 2;
        int drawY = rowY + (ROW_HEIGHT - art.getHeight()) / 2;
        graphics.drawImage(art, drawX, drawY, null);
        graphics.setColor(clipped ? new Color(239, 83, 80) : new Color(57, 74, 92));
        graphics.drawRect(x, rowY, CELL_WIDTH - 1, ROW_HEIGHT - 1);
    }

    private static void drawClamped(Graphics2D graphics, String text, int x, int baseline, int maxWidth) {
        String value = text;
        while (value.length() > 3 && graphics.getFontMetrics().stringWidth(value) > maxWidth) {
            value = value.substring(0, value.length() - 2);
        }
        if (!value.equals(text)) {
            value = value.stripTrailing() + "…";
        }
        graphics.drawString(value, x, baseline);
    }

    private static void writeReport(Path outputDir, int entries, int pages, int renders,
                                    List<String> failures, List<String> warnings) throws IOException {
        StringBuilder report = new StringBuilder();
        report.append("# Bird visual audit\n\n")
                .append("Generated: ").append(OffsetDateTime.now()).append("\n\n")
                .append("- Catalog entries: ").append(entries).append("\n")
                .append("- Rendered views: ").append(renders).append("\n")
                .append("- Contact-sheet pages: ").append(pages).append("\n")
                .append("- Result: **").append(!failures.isEmpty() ? "FAIL"
                        : warnings.isEmpty() ? "PASS" : "PASS WITH REVIEW FINDINGS").append("**\n\n")
                .append("Checks: visible pixels, authored-body clipping (excluding transient combat FX), ")
                .append("severe portrait/HUD centering, minimum scale, ")
                .append("and exact idle-image fallback to base art. Edge contact and tight padding remain review findings ")
                .append("for other entries; completed Pigeon, Null Rock, Sky King, and stock-photo bird combat entries ")
                .append("treat edge contact as a failure; ")
                .append("run with `-DvisualAudit.failOnFindings=true` to make them blocking.\n\n");
        appendFindings(report, "Failures", failures);
        appendFindings(report, "Warnings", warnings);
        Files.writeString(outputDir.resolve("visual-audit-report.md"), report.toString());
    }

    private static void appendFindings(StringBuilder report, String heading, List<String> findings) {
        report.append("## ").append(heading).append("\n\n");
        if (findings.isEmpty()) {
            report.append("None.\n\n");
            return;
        }
        for (String finding : findings) {
            report.append("- ").append(finding).append('\n');
        }
        report.append('\n');
    }

    private static void deletePreviousOutputs(Path outputDir) throws IOException {
        try (Stream<Path> files = Files.list(outputDir)) {
            for (Path path : files.filter(file -> {
                String name = file.getFileName().toString();
                return name.startsWith("bird-skin-audit-") && name.endsWith(".png")
                        || name.equals("visual-audit-report.md");
            }).toList()) {
                Files.deleteIfExists(path);
            }
        }
    }
}
