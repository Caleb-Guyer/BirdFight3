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

    private enum OpiumView {
        IDLE(null, 0),
        NEUTRAL(Bird.VisualAuditOpiumAction.NEUTRAL, 45),
        SIDE_START(Bird.VisualAuditOpiumAction.SIDE, Bird.OPIUM_SIDE_FRAMES),
        SIDE_OPEN(Bird.VisualAuditOpiumAction.SIDE, Bird.OPIUM_SIDE_FRAMES - 4),
        SIDE_CLOSE(Bird.VisualAuditOpiumAction.SIDE, 1),
        UP(Bird.VisualAuditOpiumAction.UP, Bird.OPIUM_UP_FRAMES - 4),
        DOWN(Bird.VisualAuditOpiumAction.DOWN, 37),
        ULTIMATE(Bird.VisualAuditOpiumAction.ULTIMATE, Bird.OPIUM_ULTIMATE_FRAMES - 20);

        private final Bird.VisualAuditOpiumAction action;
        private final int remainingFrames;

        OpiumView(Bird.VisualAuditOpiumAction action, int remainingFrames) {
            this.action = action;
            this.remainingFrames = remainingFrames;
        }
    }

    private enum TitmouseView {
        IDLE(null, 0),
        NEUTRAL(Bird.VisualAuditTitmouseAction.NEUTRAL, 7),
        SIDE_START(Bird.VisualAuditTitmouseAction.SIDE, Bird.TITMOUSE_BARKSKIP_FRAMES),
        SIDE_OPEN(Bird.VisualAuditTitmouseAction.SIDE, Bird.TITMOUSE_BARKSKIP_FRAMES - 4),
        SIDE_CLOSE(Bird.VisualAuditTitmouseAction.SIDE, 1),
        UP(Bird.VisualAuditTitmouseAction.UP, Bird.TITMOUSE_VAULT_FRAMES - 4),
        DOWN(Bird.VisualAuditTitmouseAction.DOWN, 14),
        ULTIMATE(Bird.VisualAuditTitmouseAction.ULTIMATE, 6);

        private final Bird.VisualAuditTitmouseAction action;
        private final int remainingFrames;

        TitmouseView(Bird.VisualAuditTitmouseAction action, int remainingFrames) {
            this.action = action;
            this.remainingFrames = remainingFrames;
        }
    }

    private enum BatView {
        IDLE(null, 0),
        HANG(Bird.VisualAuditBatAction.HANG, 1),
        ECHO(Bird.VisualAuditBatAction.NEUTRAL, 10),
        WINGCUT_START(Bird.VisualAuditBatAction.SIDE, Bird.BAT_WINGCUT_FRAMES),
        WINGCUT_OPEN(Bird.VisualAuditBatAction.SIDE, Bird.BAT_WINGCUT_FRAMES - 4),
        WINGCUT_CLOSE(Bird.VisualAuditBatAction.SIDE, 1),
        MOONRISE(Bird.VisualAuditBatAction.UP, Bird.BAT_MOONRISE_FRAMES - 5),
        STALL(Bird.VisualAuditBatAction.DOWN_STALL, 5),
        DIVE(Bird.VisualAuditBatAction.DOWN_DIVE, 12),
        CATHEDRAL(Bird.VisualAuditBatAction.ULTIMATE, 80);

        private final Bird.VisualAuditBatAction action;
        private final int remainingFrames;

        BatView(Bird.VisualAuditBatAction action, int remainingFrames) {
            this.action = action;
            this.remainingFrames = remainingFrames;
        }
    }

    private enum PelicanView {
        IDLE(null, 0),
        POUCH_SNARE(Bird.VisualAuditPelicanAction.NEUTRAL, 8),
        BREAKWATER_START(Bird.VisualAuditPelicanAction.SIDE, Bird.PELICAN_SIDE_FRAMES),
        BREAKWATER_OPEN(Bird.VisualAuditPelicanAction.SIDE, Bird.PELICAN_SIDE_FRAMES - 8),
        BREAKWATER_CLOSE(Bird.VisualAuditPelicanAction.SIDE, 1),
        THERMAL_SAIL(Bird.VisualAuditPelicanAction.UP_ASCENT, Bird.PELICAN_UP_FRAMES - 7),
        KEEL_DIVE(Bird.VisualAuditPelicanAction.UP_DIVE, 10),
        BILGE_LOAD(Bird.VisualAuditPelicanAction.DOWN_LOAD, 16),
        BILGE_DUMP(Bird.VisualAuditPelicanAction.DOWN_BILGE, 9),
        MAELSTROM(Bird.VisualAuditPelicanAction.ULTIMATE, 100);

        private final Bird.VisualAuditPelicanAction action;
        private final int remainingFrames;

        PelicanView(Bird.VisualAuditPelicanAction action, int remainingFrames) {
            this.action = action;
            this.remainingFrames = remainingFrames;
        }
    }

    private enum HeisenView {
        IDLE(null, 0),
        CRYSTAL_CLOUD(Bird.VisualAuditHeisenAction.NEUTRAL, Bird.HEISEN_NEUTRAL_REUSE_FRAMES),
        BLUE_RUSH_START(Bird.VisualAuditHeisenAction.SIDE, Bird.HEISEN_SIDE_FRAMES),
        BLUE_RUSH_OPEN(Bird.VisualAuditHeisenAction.SIDE, Bird.HEISEN_SIDE_FRAMES - 7),
        BLUE_RUSH_CLOSE(Bird.VisualAuditHeisenAction.SIDE, 1),
        CRYSTAL_COLUMN(Bird.VisualAuditHeisenAction.UP, Bird.HEISEN_UP_FRAMES - 6),
        GLASS_NODE(Bird.VisualAuditHeisenAction.DOWN, Bird.HEISEN_DOWN_REUSE_FRAMES),
        GLASS_COOK(Bird.VisualAuditHeisenAction.ULTIMATE_ORBIT, 180),
        SHARD_VOLLEY(Bird.VisualAuditHeisenAction.ULTIMATE_VOLLEY, 100);

        private final Bird.VisualAuditHeisenAction action;
        private final int remainingFrames;

        HeisenView(Bird.VisualAuditHeisenAction action, int remainingFrames) {
            this.action = action;
            this.remainingFrames = remainingFrames;
        }
    }

    private enum RavenView {
        IDLE(null, 0),
        QUILL_START(Bird.VisualAuditRavenAction.NEUTRAL, 1),
        QUILL_FULL(Bird.VisualAuditRavenAction.NEUTRAL, Bird.RAVEN_QUILL_CHARGE_FAN_FRAMES),
        WARP_START(Bird.VisualAuditRavenAction.SIDE, Bird.RAVEN_SIDE_FRAMES),
        WARP_OPEN(Bird.VisualAuditRavenAction.SIDE, 7),
        WARP_CLOSE(Bird.VisualAuditRavenAction.SIDE, 1),
        MURDER_LIFT(Bird.VisualAuditRavenAction.UP, Bird.RAVEN_LIFT_FRAMES - 7),
        NEVERMORE_PLACE(Bird.VisualAuditRavenAction.DOWN_PLACE, 12),
        NEVERMORE_SWAP(Bird.VisualAuditRavenAction.DOWN_SWAP, 12),
        UNKINDNESS_WINDUP(Bird.VisualAuditRavenAction.ULTIMATE_WINDUP,
                Bird.RAVEN_ULTIMATE_WINDUP_FRAMES),
        UNKINDNESS_ROUTE(Bird.VisualAuditRavenAction.ULTIMATE_ROUTE,
                Bird.RAVEN_ULTIMATE_ROUTE_LIFE_FRAMES - 10);

        private final Bird.VisualAuditRavenAction action;
        private final int remainingFrames;

        RavenView(Bird.VisualAuditRavenAction action, int remainingFrames) {
            this.action = action;
            this.remainingFrames = remainingFrames;
        }
    }

    private enum GooseView {
        IDLE(null, 0),
        HONK_START(Bird.VisualAuditGooseAction.HONK_CHARGE, 1),
        HONK_FULL(Bird.VisualAuditGooseAction.HONK_CHARGE, Bird.GOOSE_HONK_MAX_HOLD_FRAMES),
        HONK_RELEASE(Bird.VisualAuditGooseAction.HONK_RELEASE, Bird.GOOSE_HONK_RECOVERY_FRAMES),
        BARGE_START(Bird.VisualAuditGooseAction.BARGE, Bird.GOOSE_BARGE_FRAMES),
        BARGE_OPEN(Bird.VisualAuditGooseAction.BARGE, Bird.GOOSE_BARGE_FRAMES / 2),
        BARGE_CLOSE(Bird.VisualAuditGooseAction.BARGE, 1),
        FORMATION_LIFT(Bird.VisualAuditGooseAction.LIFT, Bird.GOOSE_LIFT_FRAMES - 7),
        NEST_PLACE(Bird.VisualAuditGooseAction.NEST_PLACE, 12),
        NEST_GUARD(Bird.VisualAuditGooseAction.NEST_GUARD, Bird.GOOSE_NEST_GUARD_FRAMES - 8),
        NEST_COUNTER(Bird.VisualAuditGooseAction.NEST_COUNTER, Bird.GOOSE_COUNTER_BURST_FRAMES - 8),
        WHOLE_FLOCK(Bird.VisualAuditGooseAction.ULTIMATE, Bird.GOOSE_ULTIMATE_FRAMES - 30);

        private final Bird.VisualAuditGooseAction action;
        private final int remainingFrames;

        GooseView(Bird.VisualAuditGooseAction action, int remainingFrames) {
            this.action = action;
            this.remainingFrames = remainingFrames;
        }
    }

    private enum KiwiView {
        IDLE(null, 0),
        PROBE_START(Bird.VisualAuditKiwiAction.PROBE, Bird.KIWI_PROBE_FRAMES),
        PROBE_THRUST(Bird.VisualAuditKiwiAction.PROBE, Bird.KIWI_PROBE_FRAMES - 3),
        PROBE_RESET(Bird.VisualAuditKiwiAction.PROBE, Bird.KIWI_PROBE_FRAMES - 6),
        BURROW_DIG(Bird.VisualAuditKiwiAction.BURROW_DIG, Bird.KIWI_BURROW_FRAMES - 8),
        BURROW_ERUPT(Bird.VisualAuditKiwiAction.BURROW_ERUPT, 7),
        SPRING_KICK(Bird.VisualAuditKiwiAction.SPRING, Bird.KIWI_SPRING_FRAMES - 6),
        STOMP_WINDUP(Bird.VisualAuditKiwiAction.STOMP_WINDUP, Bird.KIWI_STOMP_FRAMES - 6),
        STOMP_AIR(Bird.VisualAuditKiwiAction.STOMP_AIR, Bird.KIWI_STOMP_FRAMES - 10),
        STOMP_IMPACT(Bird.VisualAuditKiwiAction.STOMP_IMPACT, Bird.KIWI_STOMP_IMPACT_FX_FRAMES),
        STAMPEDE_CHARGE(Bird.VisualAuditKiwiAction.ULTIMATE_CHARGE, 143),
        STAMPEDE_ERUPT(Bird.VisualAuditKiwiAction.ULTIMATE_ERUPTION, 24);

        private final Bird.VisualAuditKiwiAction action;
        private final int remainingFrames;

        KiwiView(Bird.VisualAuditKiwiAction action, int remainingFrames) {
            this.action = action;
            this.remainingFrames = remainingFrames;
        }
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

                if ((entry.bird() == BirdGame3.BirdType.VULTURE
                        && !"NULL_ROCK_VULTURE".equals(entry.key()))
                        || entry.bird() == BirdGame3.BirdType.OPIUMBIRD
                        || entry.bird() == BirdGame3.BirdType.TITMOUSE
                        || entry.bird() == BirdGame3.BirdType.BAT
                        || entry.bird() == BirdGame3.BirdType.PELICAN
                        || entry.bird() == BirdGame3.BirdType.HEISENBIRD
                        || entry.bird() == BirdGame3.BirdType.RAVEN
                        || entry.bird() == BirdGame3.BirdType.GOOSE
                        || entry.bird() == BirdGame3.BirdType.KIWI) {
                    checkPolishedFacingMirror(game, entry, failures);
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

        renderCount += renderOpiumSpecialSheet(game, entries, outputDir, failures);
        renderCount += renderTitmouseSpecialSheet(game, entries, outputDir, failures);
        renderCount += renderBatSpecialSheet(game, entries, outputDir, failures);
        renderCount += renderPelicanSpecialSheet(game, entries, outputDir, failures);
        renderCount += renderHeisenSpecialSheet(game, entries, outputDir, failures);
        renderCount += renderRavenSpecialSheet(game, entries, outputDir, failures);
        renderCount += renderGooseSpecialSheet(game, entries, outputDir, failures);
        renderCount += renderKiwiSpecialSheet(game, entries, outputDir, failures);

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

    private static int renderOpiumSpecialSheet(
            BirdGame3 game, List<BirdGame3.VisualAuditSkin> entries,
            Path outputDir, List<String> failures) throws IOException {
        List<BirdGame3.VisualAuditSkin> opiumEntries = entries.stream()
                .filter(entry -> entry.bird() == BirdGame3.BirdType.OPIUMBIRD)
                .filter(entry -> BirdSpriteLibrary.sheetFor(entry.bird(), entry.key()) == null)
                .toList();
        BufferedImage page = createPage(opiumEntries.size());
        Graphics2D graphics = page.createGraphics();
        configureGraphics(graphics);
        graphics.setColor(new Color(26, 36, 48));
        graphics.fillRect(0, 0, LABEL_WIDTH + OpiumView.values().length * CELL_WIDTH, HEADER_HEIGHT);
        graphics.setColor(new Color(240, 244, 248));
        graphics.setFont(new Font("Segoe UI", Font.BOLD, 17));
        graphics.drawString("OPIUM SPECIALS", 18, 25);
        graphics.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        graphics.setColor(new Color(148, 163, 184));
        graphics.drawString("Opening, active, and recovery silhouettes", 18, 45);
        graphics.setFont(new Font("Segoe UI", Font.BOLD, 12));
        for (OpiumView view : OpiumView.values()) {
            int columnX = LABEL_WIDTH + view.ordinal() * CELL_WIDTH;
            graphics.setColor(new Color(240, 244, 248));
            String label = view.name().replace('_', ' ');
            int textWidth = graphics.getFontMetrics().stringWidth(label);
            graphics.drawString(label, columnX + (CELL_WIDTH - textWidth) / 2, 34);
        }

        int renders = 0;
        for (int row = 0; row < opiumEntries.size(); row++) {
            BirdGame3.VisualAuditSkin entry = opiumEntries.get(row);
            int rowY = HEADER_HEIGHT + row * ROW_HEIGHT;
            drawRowLabel(graphics, entry, rowY);
            for (OpiumView view : OpiumView.values()) {
                Canvas canvas = new Canvas(ART_SIZE, ART_SIZE);
                if (view.action == null) {
                    game.drawVisualAuditCombatSilhouette(canvas, entry, Bird.VisualAuditPose.IDLE);
                } else {
                    game.drawVisualAuditOpiumActionPose(
                            canvas, entry, view.action, view.remainingFrames, true);
                }
                BufferedImage art = snapshot(canvas);
                PixelBounds bounds = measure(art);
                if (bounds.borderPixels > 0) {
                    failures.add(entry.name() + " / " + view.name()
                            + " touches the " + touchedEdges(bounds, art)
                            + " canvas edge in the Opium special audit.");
                }
                drawCell(graphics, art, view.ordinal(), rowY, bounds.borderPixels > 0);
                renders++;
            }
        }
        graphics.dispose();
        ImageIO.write(page, "png", outputDir.resolve("opium-special-audit.png").toFile());
        return renders;
    }

    private static int renderTitmouseSpecialSheet(
            BirdGame3 game, List<BirdGame3.VisualAuditSkin> entries,
            Path outputDir, List<String> failures) throws IOException {
        List<BirdGame3.VisualAuditSkin> titmouseEntries = entries.stream()
                .filter(entry -> entry.bird() == BirdGame3.BirdType.TITMOUSE)
                .filter(entry -> BirdSpriteLibrary.sheetFor(entry.bird(), entry.key()) == null)
                .toList();
        BufferedImage page = createPage(titmouseEntries.size());
        Graphics2D graphics = page.createGraphics();
        configureGraphics(graphics);
        graphics.setColor(new Color(26, 36, 48));
        graphics.fillRect(0, 0, LABEL_WIDTH + TitmouseView.values().length * CELL_WIDTH, HEADER_HEIGHT);
        graphics.setColor(new Color(240, 244, 248));
        graphics.setFont(new Font("Segoe UI", Font.BOLD, 17));
        graphics.drawString("TUFTED TITMOUSE SPECIALS", 18, 25);
        graphics.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        graphics.setColor(new Color(148, 163, 184));
        graphics.drawString("Opening, active, and recovery silhouettes", 18, 45);
        graphics.setFont(new Font("Segoe UI", Font.BOLD, 12));
        for (TitmouseView view : TitmouseView.values()) {
            int columnX = LABEL_WIDTH + view.ordinal() * CELL_WIDTH;
            graphics.setColor(new Color(240, 244, 248));
            String label = view.name().replace('_', ' ');
            int textWidth = graphics.getFontMetrics().stringWidth(label);
            graphics.drawString(label, columnX + (CELL_WIDTH - textWidth) / 2, 34);
        }

        int renders = 0;
        for (int row = 0; row < titmouseEntries.size(); row++) {
            BirdGame3.VisualAuditSkin entry = titmouseEntries.get(row);
            int rowY = HEADER_HEIGHT + row * ROW_HEIGHT;
            drawRowLabel(graphics, entry, rowY);
            for (TitmouseView view : TitmouseView.values()) {
                Canvas canvas = new Canvas(ART_SIZE, ART_SIZE);
                if (view.action == null) {
                    game.drawVisualAuditCombatSilhouette(canvas, entry, Bird.VisualAuditPose.IDLE);
                } else {
                    game.drawVisualAuditTitmouseActionPose(
                            canvas, entry, view.action, view.remainingFrames, true);
                }
                BufferedImage art = snapshot(canvas);
                PixelBounds bounds = measure(art);
                if (bounds.borderPixels > 0) {
                    failures.add(entry.name() + " / " + view.name()
                            + " touches the " + touchedEdges(bounds, art)
                            + " canvas edge in the Titmouse special audit.");
                }
                drawCell(graphics, art, view.ordinal(), rowY, bounds.borderPixels > 0);
                renders++;
            }
        }
        graphics.dispose();
        ImageIO.write(page, "png", outputDir.resolve("titmouse-special-audit.png").toFile());
        return renders;
    }

    private static int renderBatSpecialSheet(
            BirdGame3 game, List<BirdGame3.VisualAuditSkin> entries,
            Path outputDir, List<String> failures) throws IOException {
        List<BirdGame3.VisualAuditSkin> batEntries = entries.stream()
                .filter(entry -> entry.bird() == BirdGame3.BirdType.BAT)
                .filter(entry -> BirdSpriteLibrary.sheetFor(entry.bird(), entry.key()) == null)
                .toList();
        BufferedImage page = createPage(batEntries.size(), BatView.values().length);
        Graphics2D graphics = page.createGraphics();
        configureGraphics(graphics);
        graphics.setColor(new Color(26, 36, 48));
        graphics.fillRect(0, 0, page.getWidth(), HEADER_HEIGHT);
        graphics.setColor(new Color(240, 244, 248));
        graphics.setFont(new Font("Segoe UI", Font.BOLD, 17));
        graphics.drawString("BAT SPECIALS", 18, 25);
        graphics.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        graphics.setColor(new Color(148, 163, 184));
        graphics.drawString("Hanging contact and complete wing cycles", 18, 45);
        graphics.setFont(new Font("Segoe UI", Font.BOLD, 12));
        for (BatView view : BatView.values()) {
            int columnX = LABEL_WIDTH + view.ordinal() * CELL_WIDTH;
            graphics.setColor(new Color(240, 244, 248));
            String label = view.name().replace('_', ' ');
            int textWidth = graphics.getFontMetrics().stringWidth(label);
            graphics.drawString(label, columnX + (CELL_WIDTH - textWidth) / 2, 34);
        }

        int renders = 0;
        for (int row = 0; row < batEntries.size(); row++) {
            BirdGame3.VisualAuditSkin entry = batEntries.get(row);
            int rowY = HEADER_HEIGHT + row * ROW_HEIGHT;
            drawRowLabel(graphics, entry, rowY);
            for (BatView view : BatView.values()) {
                Canvas canvas = new Canvas(ART_SIZE, ART_SIZE);
                if (view.action == null) {
                    game.drawVisualAuditCombatSilhouette(canvas, entry, Bird.VisualAuditPose.IDLE);
                } else {
                    game.drawVisualAuditBatActionPose(
                            canvas, entry, view.action, view.remainingFrames, true);
                }
                BufferedImage art = snapshot(canvas);
                PixelBounds bounds = measure(art);
                if (bounds.borderPixels > 0) {
                    failures.add(entry.name() + " / " + view.name()
                            + " touches the " + touchedEdges(bounds, art)
                            + " canvas edge in the Bat special audit.");
                }
                drawCell(graphics, art, view.ordinal(), rowY, bounds.borderPixels > 0);
                renders++;
            }
        }
        graphics.dispose();
        ImageIO.write(page, "png", outputDir.resolve("bat-special-audit.png").toFile());
        return renders;
    }

    private static int renderPelicanSpecialSheet(
            BirdGame3 game, List<BirdGame3.VisualAuditSkin> entries,
            Path outputDir, List<String> failures) throws IOException {
        List<BirdGame3.VisualAuditSkin> pelicanEntries = entries.stream()
                .filter(entry -> entry.bird() == BirdGame3.BirdType.PELICAN)
                .filter(entry -> BirdSpriteLibrary.sheetFor(entry.bird(), entry.key()) == null)
                .toList();
        BufferedImage page = createPage(pelicanEntries.size(), PelicanView.values().length);
        Graphics2D graphics = page.createGraphics();
        configureGraphics(graphics);
        graphics.setColor(new Color(26, 36, 48));
        graphics.fillRect(0, 0, page.getWidth(), HEADER_HEIGHT);
        graphics.setColor(new Color(240, 244, 248));
        graphics.setFont(new Font("Segoe UI", Font.BOLD, 17));
        graphics.drawString("PELICAN SPECIALS", 18, 25);
        graphics.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        graphics.setColor(new Color(148, 163, 184));
        graphics.drawString("Attached pouch, bill aim, and complete wing cycles", 18, 45);
        graphics.setFont(new Font("Segoe UI", Font.BOLD, 12));
        for (PelicanView view : PelicanView.values()) {
            int columnX = LABEL_WIDTH + view.ordinal() * CELL_WIDTH;
            graphics.setColor(new Color(240, 244, 248));
            String label = view.name().replace('_', ' ');
            int textWidth = graphics.getFontMetrics().stringWidth(label);
            graphics.drawString(label, columnX + (CELL_WIDTH - textWidth) / 2, 34);
        }

        int renders = 0;
        for (int row = 0; row < pelicanEntries.size(); row++) {
            BirdGame3.VisualAuditSkin entry = pelicanEntries.get(row);
            int rowY = HEADER_HEIGHT + row * ROW_HEIGHT;
            drawRowLabel(graphics, entry, rowY);
            for (PelicanView view : PelicanView.values()) {
                Canvas canvas = new Canvas(ART_SIZE, ART_SIZE);
                if (view.action == null) {
                    game.drawVisualAuditCombatSilhouette(canvas, entry, Bird.VisualAuditPose.IDLE);
                } else {
                    game.drawVisualAuditPelicanActionPose(
                            canvas, entry, view.action, view.remainingFrames, true);
                }
                BufferedImage art = snapshot(canvas);
                PixelBounds bounds = measure(art);
                if (bounds.borderPixels > 0) {
                    failures.add(entry.name() + " / " + view.name()
                            + " touches the " + touchedEdges(bounds, art)
                            + " canvas edge in the Pelican special audit.");
                }
                drawCell(graphics, art, view.ordinal(), rowY, bounds.borderPixels > 0);
                renders++;
            }
        }
        graphics.dispose();
        ImageIO.write(page, "png", outputDir.resolve("pelican-special-audit.png").toFile());
        return renders;
    }

    private static int renderHeisenSpecialSheet(
            BirdGame3 game, List<BirdGame3.VisualAuditSkin> entries,
            Path outputDir, List<String> failures) throws IOException {
        List<BirdGame3.VisualAuditSkin> heisenEntries = entries.stream()
                .filter(entry -> entry.bird() == BirdGame3.BirdType.HEISENBIRD)
                .filter(entry -> BirdSpriteLibrary.sheetFor(entry.bird(), entry.key()) == null)
                .toList();
        BufferedImage page = createPage(heisenEntries.size(), HeisenView.values().length);
        Graphics2D graphics = page.createGraphics();
        configureGraphics(graphics);
        graphics.setColor(new Color(26, 36, 48));
        graphics.fillRect(0, 0, page.getWidth(), HEADER_HEIGHT);
        graphics.setColor(new Color(240, 244, 248));
        graphics.setFont(new Font("Segoe UI", Font.BOLD, 17));
        graphics.drawString("HEISENBIRD SPECIALS", 18, 25);
        graphics.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        graphics.setColor(new Color(148, 163, 184));
        graphics.drawString("Attached hat, facial marks, crystals, and complete wing cycles", 18, 45);
        graphics.setFont(new Font("Segoe UI", Font.BOLD, 12));
        for (HeisenView view : HeisenView.values()) {
            int columnX = LABEL_WIDTH + view.ordinal() * CELL_WIDTH;
            graphics.setColor(new Color(240, 244, 248));
            String label = view.name().replace('_', ' ');
            int textWidth = graphics.getFontMetrics().stringWidth(label);
            graphics.drawString(label, columnX + (CELL_WIDTH - textWidth) / 2, 34);
        }

        int renders = 0;
        for (int row = 0; row < heisenEntries.size(); row++) {
            BirdGame3.VisualAuditSkin entry = heisenEntries.get(row);
            int rowY = HEADER_HEIGHT + row * ROW_HEIGHT;
            drawRowLabel(graphics, entry, rowY);
            for (HeisenView view : HeisenView.values()) {
                Canvas canvas = new Canvas(ART_SIZE, ART_SIZE);
                if (view.action == null) {
                    game.drawVisualAuditCombatSilhouette(canvas, entry, Bird.VisualAuditPose.IDLE);
                } else {
                    game.drawVisualAuditHeisenActionPose(
                            canvas, entry, view.action, view.remainingFrames, true);
                }
                BufferedImage art = snapshot(canvas);
                PixelBounds bounds = measure(art);
                if (bounds.borderPixels > 0) {
                    failures.add(entry.name() + " / " + view.name()
                            + " touches the " + touchedEdges(bounds, art)
                            + " canvas edge in the Heisenbird special audit.");
                }
                drawCell(graphics, art, view.ordinal(), rowY, bounds.borderPixels > 0);
                renders++;
            }
        }
        graphics.dispose();
        ImageIO.write(page, "png", outputDir.resolve("heisen-special-audit.png").toFile());
        return renders;
    }

    private static int renderRavenSpecialSheet(
            BirdGame3 game, List<BirdGame3.VisualAuditSkin> entries,
            Path outputDir, List<String> failures) throws IOException {
        List<BirdGame3.VisualAuditSkin> ravenEntries = entries.stream()
                .filter(entry -> entry.bird() == BirdGame3.BirdType.RAVEN)
                .filter(entry -> BirdSpriteLibrary.sheetFor(entry.bird(), entry.key()) == null)
                .toList();
        BufferedImage page = createPage(ravenEntries.size(), RavenView.values().length);
        Graphics2D graphics = page.createGraphics();
        configureGraphics(graphics);
        graphics.setColor(new Color(20, 24, 34));
        graphics.fillRect(0, 0, page.getWidth(), HEADER_HEIGHT);
        graphics.setColor(new Color(240, 244, 248));
        graphics.setFont(new Font("Segoe UI", Font.BOLD, 17));
        graphics.drawString("RAVEN SPECIALS", 18, 25);
        graphics.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        graphics.setColor(new Color(148, 163, 184));
        graphics.drawString("Attached corvid anatomy, cracked Herald mask, and complete omen cycles", 18, 45);
        graphics.setFont(new Font("Segoe UI", Font.BOLD, 12));
        for (RavenView view : RavenView.values()) {
            int columnX = LABEL_WIDTH + view.ordinal() * CELL_WIDTH;
            graphics.setColor(new Color(240, 244, 248));
            String label = view.name().replace('_', ' ');
            int textWidth = graphics.getFontMetrics().stringWidth(label);
            graphics.drawString(label, columnX + (CELL_WIDTH - textWidth) / 2, 34);
        }

        int renders = 0;
        for (int row = 0; row < ravenEntries.size(); row++) {
            BirdGame3.VisualAuditSkin entry = ravenEntries.get(row);
            int rowY = HEADER_HEIGHT + row * ROW_HEIGHT;
            drawRowLabel(graphics, entry, rowY);
            for (RavenView view : RavenView.values()) {
                Canvas canvas = new Canvas(ART_SIZE, ART_SIZE);
                if (view.action == null) {
                    game.drawVisualAuditCombatSilhouette(canvas, entry, Bird.VisualAuditPose.IDLE);
                } else {
                    game.drawVisualAuditRavenActionPose(
                            canvas, entry, view.action, view.remainingFrames, true);
                }
                BufferedImage art = snapshot(canvas);
                PixelBounds bounds = measure(art);
                if (bounds.borderPixels > 0) {
                    failures.add(entry.name() + " / " + view.name()
                            + " touches the " + touchedEdges(bounds, art)
                            + " canvas edge in the Raven special audit.");
                }
                drawCell(graphics, art, view.ordinal(), rowY, bounds.borderPixels > 0);
                renders++;
            }
        }
        graphics.dispose();
        ImageIO.write(page, "png", outputDir.resolve("raven-special-audit.png").toFile());
        return renders;
    }

    private static int renderGooseSpecialSheet(
            BirdGame3 game, List<BirdGame3.VisualAuditSkin> entries,
            Path outputDir, List<String> failures) throws IOException {
        List<BirdGame3.VisualAuditSkin> gooseEntries = entries.stream()
                .filter(entry -> entry.bird() == BirdGame3.BirdType.GOOSE)
                .filter(entry -> BirdSpriteLibrary.sheetFor(entry.bird(), entry.key()) == null)
                .toList();
        BufferedImage page = createPage(gooseEntries.size(), GooseView.values().length);
        Graphics2D graphics = page.createGraphics();
        configureGraphics(graphics);
        graphics.setColor(new Color(23, 33, 27));
        graphics.fillRect(0, 0, page.getWidth(), HEADER_HEIGHT);
        graphics.setColor(new Color(240, 244, 238));
        graphics.setFont(new Font("Segoe UI", Font.BOLD, 17));
        graphics.drawString("GOOSE SPECIALS", 18, 25);
        graphics.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        graphics.setColor(new Color(157, 176, 158));
        graphics.drawString("Long-neck anatomy, webbed grounding, and complete territorial action cycles", 18, 45);
        graphics.setFont(new Font("Segoe UI", Font.BOLD, 12));
        for (GooseView view : GooseView.values()) {
            int columnX = LABEL_WIDTH + view.ordinal() * CELL_WIDTH;
            graphics.setColor(new Color(240, 244, 238));
            String label = view.name().replace('_', ' ');
            int textWidth = graphics.getFontMetrics().stringWidth(label);
            graphics.drawString(label, columnX + (CELL_WIDTH - textWidth) / 2, 34);
        }

        int renders = 0;
        for (int row = 0; row < gooseEntries.size(); row++) {
            BirdGame3.VisualAuditSkin entry = gooseEntries.get(row);
            int rowY = HEADER_HEIGHT + row * ROW_HEIGHT;
            drawRowLabel(graphics, entry, rowY);
            for (GooseView view : GooseView.values()) {
                Canvas canvas = new Canvas(ART_SIZE, ART_SIZE);
                if (view.action == null) {
                    game.drawVisualAuditCombatSilhouette(canvas, entry, Bird.VisualAuditPose.IDLE);
                } else {
                    game.drawVisualAuditGooseActionPose(
                            canvas, entry, view.action, view.remainingFrames, true);
                }
                BufferedImage art = snapshot(canvas);
                PixelBounds bounds = measure(art);
                if (bounds.borderPixels > 0) {
                    failures.add(entry.name() + " / " + view.name()
                            + " touches the " + touchedEdges(bounds, art)
                            + " canvas edge in the Goose special audit.");
                }
                drawCell(graphics, art, view.ordinal(), rowY, bounds.borderPixels > 0);
                renders++;
            }
        }
        graphics.dispose();
        ImageIO.write(page, "png", outputDir.resolve("goose-special-audit.png").toFile());
        return renders;
    }

    private static int renderKiwiSpecialSheet(
            BirdGame3 game, List<BirdGame3.VisualAuditSkin> entries,
            Path outputDir, List<String> failures) throws IOException {
        List<BirdGame3.VisualAuditSkin> kiwiEntries = entries.stream()
                .filter(entry -> entry.bird() == BirdGame3.BirdType.KIWI)
                .filter(entry -> BirdSpriteLibrary.sheetFor(entry.bird(), entry.key()) == null)
                .toList();
        BufferedImage page = createPage(kiwiEntries.size(), KiwiView.values().length);
        Graphics2D graphics = page.createGraphics();
        configureGraphics(graphics);
        graphics.setColor(new Color(34, 30, 25));
        graphics.fillRect(0, 0, page.getWidth(), HEADER_HEIGHT);
        graphics.setColor(new Color(244, 239, 226));
        graphics.setFont(new Font("Segoe UI", Font.BOLD, 17));
        graphics.drawString("KIWI BIRD SPECIALS", 18, 25);
        graphics.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        graphics.setColor(new Color(184, 169, 143));
        graphics.drawString("Wingless shag anatomy, articulated feet, and complete groundwork cycles", 18, 45);
        graphics.setFont(new Font("Segoe UI", Font.BOLD, 12));
        for (KiwiView view : KiwiView.values()) {
            int columnX = LABEL_WIDTH + view.ordinal() * CELL_WIDTH;
            graphics.setColor(new Color(244, 239, 226));
            String label = view.name().replace('_', ' ');
            int textWidth = graphics.getFontMetrics().stringWidth(label);
            graphics.drawString(label, columnX + (CELL_WIDTH - textWidth) / 2, 34);
        }

        int renders = 0;
        for (int row = 0; row < kiwiEntries.size(); row++) {
            BirdGame3.VisualAuditSkin entry = kiwiEntries.get(row);
            int rowY = HEADER_HEIGHT + row * ROW_HEIGHT;
            drawRowLabel(graphics, entry, rowY);
            for (KiwiView view : KiwiView.values()) {
                Canvas canvas = new Canvas(ART_SIZE, ART_SIZE);
                if (view.action == null) {
                    game.drawVisualAuditCombatSilhouette(canvas, entry, Bird.VisualAuditPose.IDLE);
                } else {
                    game.drawVisualAuditKiwiActionPose(
                            canvas, entry, view.action, view.remainingFrames, true);
                }
                BufferedImage art = snapshot(canvas);
                PixelBounds bounds = measure(art);
                if (bounds.borderPixels > 0) {
                    failures.add(entry.name() + " / " + view.name()
                            + " touches the " + touchedEdges(bounds, art)
                            + " canvas edge in the Kiwi special audit.");
                }
                drawCell(graphics, art, view.ordinal(), rowY, bounds.borderPixels > 0);
                renders++;
            }
        }
        graphics.dispose();
        ImageIO.write(page, "png", outputDir.resolve("kiwi-special-audit.png").toFile());
        return renders;
    }

    private static void checkPolishedFacingMirror(
            BirdGame3 game, BirdGame3.VisualAuditSkin entry, List<String> failures) {
        for (Bird.VisualAuditPose pose : Bird.VisualAuditPose.values()) {
            Canvas rightCanvas = new Canvas(ART_SIZE, ART_SIZE);
            Canvas leftCanvas = new Canvas(ART_SIZE, ART_SIZE);
            game.drawVisualAuditCombatSilhouette(rightCanvas, entry, pose, true);
            game.drawVisualAuditCombatSilhouette(leftCanvas, entry, pose, false);
            BufferedImage right = snapshot(rightCanvas);
            BufferedImage left = snapshot(leftCanvas);
            int compared = 0;
            int mismatched = 0;
            for (int y = 0; y < ART_SIZE; y++) {
                for (int x = 0; x < ART_SIZE; x++) {
                    int rightArgb = right.getRGB(x, y);
                    int leftArgb = left.getRGB(ART_SIZE - 1 - x, y);
                    if (((rightArgb >>> 24) | (leftArgb >>> 24)) <= 12) {
                        continue;
                    }
                    compared++;
                    if (maximumChannelDifference(rightArgb, leftArgb) > 22) {
                        mismatched++;
                    }
                }
            }
            double mismatchRatio = compared == 0 ? 1.0 : mismatched / (double) compared;
            if (mismatchRatio > 0.025) {
                failures.add(entry.name() + " / " + pose
                        + " is not horizontally even between facings ("
                        + String.format("%.1f%%", mismatchRatio * 100.0) + " mismatched pixels).");
            }
        }
    }

    private static int maximumChannelDifference(int first, int second) {
        int maximum = 0;
        for (int shift : new int[]{0, 8, 16, 24}) {
            maximum = Math.max(maximum,
                    Math.abs(((first >>> shift) & 0xFF) - ((second >>> shift) & 0xFF)));
        }
        return maximum;
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
                || entry.bird() == BirdGame3.BirdType.EAGLE
                || entry.bird() == BirdGame3.BirdType.FALCON
                || entry.bird() == BirdGame3.BirdType.PHOENIX
                || entry.bird() == BirdGame3.BirdType.HUMMINGBIRD
                || entry.bird() == BirdGame3.BirdType.TURKEY
                || entry.bird() == BirdGame3.BirdType.ROOSTER
                || entry.bird() == BirdGame3.BirdType.ROADRUNNER
                || entry.bird() == BirdGame3.BirdType.PENGUIN
                || entry.bird() == BirdGame3.BirdType.SHOEBILL
                || entry.bird() == BirdGame3.BirdType.MOCKINGBIRD
                || entry.bird() == BirdGame3.BirdType.RAZORBILL
                || entry.bird() == BirdGame3.BirdType.GRINCHHAWK
                || entry.bird() == BirdGame3.BirdType.VULTURE
                || entry.bird() == BirdGame3.BirdType.OPIUMBIRD
                || entry.bird() == BirdGame3.BirdType.TITMOUSE
                || entry.bird() == BirdGame3.BirdType.BAT
                || entry.bird() == BirdGame3.BirdType.PELICAN
                || entry.bird() == BirdGame3.BirdType.HEISENBIRD
                || entry.bird() == BirdGame3.BirdType.RAVEN
                || entry.bird() == BirdGame3.BirdType.GOOSE
                || entry.bird() == BirdGame3.BirdType.KIWI
                || "NULL_ROCK_VULTURE".equals(entry.key());
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
        return createPage(rows, View.values().length);
    }

    private static BufferedImage createPage(int rows, int columns) {
        int width = LABEL_WIDTH + columns * CELL_WIDTH;
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
                .append("for other entries; completed Pigeon, Eagle, Falcon, Phoenix, Hummingbird, Turkey, Rooster, Roadrunner, Penguin, Shoebill, Charles, Razorbill, Grinch-Hawk, Vulture, Opium Bird, Tufted Titmouse, Bat, Pelican, Heisenbird, Raven, Goose, Kiwi Bird, and Null Rock combat entries ")
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
                        || name.equals("opium-special-audit.png")
                        || name.equals("titmouse-special-audit.png")
                        || name.equals("bat-special-audit.png")
                        || name.equals("pelican-special-audit.png")
                        || name.equals("heisen-special-audit.png")
                        || name.equals("raven-special-audit.png")
                        || name.equals("goose-special-audit.png")
                        || name.equals("kiwi-special-audit.png")
                        || name.equals("visual-audit-report.md");
            }).toList()) {
                Files.deleteIfExists(path);
            }
        }
    }
}
