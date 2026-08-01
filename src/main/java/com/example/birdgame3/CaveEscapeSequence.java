package com.example.birdgame3;

import javafx.animation.AnimationTimer;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.TextAlignment;
import javafx.stage.Stage;

import java.util.List;

/** Fixed-tick, presentation-isolated escape that closes The Still Sky. */
final class CaveEscapeSequence {
    private static final double WIDTH = 1920.0;
    private static final double HEIGHT = 1080.0;
    private static final double WORLD_LENGTH = 22_400.0;
    private static final double FLOOR_Y = 805.0;
    private static final long STEP_NS = 16_666_667L;
    private static final int LIMIT_TICKS = 65 * 60;
    private static final List<Gap> GAPS = List.of(
            new Gap(2460, 280), new Gap(4680, 390), new Gap(7270, 300),
            new Gap(10120, 470), new Gap(13240, 350), new Gap(16280, 430),
            new Gap(19380, 520));
    private static final List<Rock> ROCKS = List.of(
            new Rock(1540, 100, 120), new Rock(3270, 145, 175), new Rock(3980, 90, 110),
            new Rock(5820, 170, 210), new Rock(6680, 110, 135), new Rock(8460, 190, 235),
            new Rock(9310, 120, 150), new Rock(11180, 200, 245), new Rock(12240, 110, 135),
            new Rock(14580, 175, 210), new Rock(15500, 105, 130), new Rock(17620, 220, 255),
            new Rock(18610, 120, 145), new Rock(20780, 185, 225));

    private final BirdGame3 game;
    private AnimationTimer timer;
    private Canvas canvas;
    private Bird pigeon;
    private Runnable onEscaped;
    private Runnable onAbandoned;
    private long lastNow;
    private long accumulator;
    private int ticks;
    private double x;
    private double y;
    private double vy;
    private double cameraX;
    private boolean onGround;
    private boolean rightHeld;
    private boolean leftHeld;
    private boolean jumpQueued;
    private boolean failed;
    private boolean finished;
    private String failureReason = "";
    private HBox failControls;

    CaveEscapeSequence(BirdGame3 game) {
        this.game = game;
    }

    void play(Stage stage, Runnable onEscaped, Runnable onAbandoned) {
        stop();
        this.onEscaped = onEscaped;
        this.onAbandoned = onAbandoned;
        resetRun();

        canvas = new Canvas(1600, 900);
        canvas.setScaleX(WIDTH / canvas.getWidth());
        canvas.setScaleY(HEIGHT / canvas.getHeight());
        StackPane content = new StackPane(canvas);
        content.setMinSize(WIDTH, HEIGHT);
        content.setPrefSize(WIDTH, HEIGHT);
        content.setMaxSize(WIDTH, HEIGHT);
        content.setStyle("-fx-background-color: #03040A;");

        Button retry = button("RETRY ESCAPE", this::resetRun);
        Button leave = button("RETURN TO STORY", () -> abandon(stage));
        failControls = new HBox(18, retry, leave);
        failControls.setAlignment(Pos.CENTER);
        failControls.setPadding(new Insets(15));
        failControls.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
        failControls.setStyle("-fx-background-color: rgba(5,7,14,0.94);"
                + "-fx-border-color: #FF8A80; -fx-border-width: 2; -fx-background-radius: 8; -fx-border-radius: 8;");
        failControls.setVisible(false);
        StackPane.setAlignment(failControls, Pos.CENTER);
        content.getChildren().add(failControls);

        StackPane root = new StackPane(content);
        root.getProperties().put("noAutoScale", true);
        Scene scene = new Scene(root, WIDTH, HEIGHT, Color.web("#03040A"));
        game.prepareCampaignCutsceneScene(scene, root, content);
        scene.addEventFilter(KeyEvent.KEY_PRESSED, event -> handlePressed(event, stage));
        scene.addEventFilter(KeyEvent.KEY_RELEASED, this::handleReleased);
        game.setCampaignScene(stage, scene);
        game.startCampaignSequenceMusic("music-escape.mp3", true);
        game.playCampaignCaveCollapseCue();

        timer = new AnimationTimer() {
            @Override public void handle(long now) {
                if (lastNow == 0L) lastNow = now;
                accumulator += Math.min(100_000_000L, now - lastNow);
                lastNow = now;
                int steps = 0;
                while (!failed && !finished && accumulator >= STEP_NS && steps++ < 6) {
                    tick();
                    accumulator -= STEP_NS;
                }
                render(now);
            }
        };
        timer.start();
        canvas.requestFocus();
    }

    private Button button(String text, Runnable action) {
        Button button = new Button(text);
        button.setFont(Font.font("Arial Black", FontWeight.BOLD, 18));
        button.setTextFill(Color.WHITE);
        button.setStyle("-fx-background-color: #263238; -fx-border-color: #80DEEA;"
                + "-fx-border-width: 2; -fx-background-radius: 7; -fx-border-radius: 7;");
        button.setOnAction(event -> action.run());
        return button;
    }

    private void resetRun() {
        x = 520.0;
        y = FLOOR_Y - 104.0;
        vy = 0.0;
        cameraX = 0.0;
        ticks = 0;
        accumulator = 0L;
        lastNow = 0L;
        onGround = true;
        rightHeld = false;
        leftHeld = false;
        jumpQueued = false;
        failed = false;
        finished = false;
        failureReason = "";
        if (failControls != null) failControls.setVisible(false);
        pigeon = game.createCampaignCutsceneBird(BirdGame3.BirdType.PIGEON, null);
        pigeon.setBaseMultipliers(1.18, 1.0, 1.0);
        pigeon.facingRight = true;
    }

    private void tick() {
        ticks++;
        double speed = rightHeld ? 10.8 : leftHeld ? 5.2 : 8.1;
        x += speed;
        if (jumpQueued && onGround) {
            vy = -15.8;
            onGround = false;
        }
        jumpQueued = false;
        vy += 0.82;
        y += vy;

        double floor = floorAt(x);
        if (!Double.isNaN(floor) && y + 104.0 >= floor && vy >= 0.0) {
            y = floor - 104.0;
            vy = 0.0;
            onGround = true;
        } else {
            onGround = false;
        }

        for (Rock rock : ROCKS) {
            double top = FLOOR_Y - rock.height;
            if (x + 38 > rock.x && x - 38 < rock.x + rock.width
                    && y + 96 > top && y + 14 < FLOOR_Y) {
                fail("Pigeon was caught by the collapse.");
                return;
            }
        }

        double blastFront = Math.max(-450.0, ticks * 6.15 - 900.0);
        if (x < blastFront + 155.0) {
            fail("The charge overtook the tunnel.");
        } else if (y > HEIGHT + 180.0) {
            fail("Pigeon fell beneath the escape route.");
        } else if (ticks >= LIMIT_TICKS) {
            fail("The tunnel sealed before Pigeon escaped.");
        } else if (x >= WORLD_LENGTH - 360.0) {
            finishEscape();
        }
        cameraX = Math.clamp(x - 520.0, 0.0, WORLD_LENGTH - WIDTH);
    }

    private double floorAt(double worldX) {
        for (Gap gap : GAPS) {
            if (worldX > gap.x && worldX < gap.x + gap.width) return Double.NaN;
        }
        if (worldX > 8800 && worldX < 9800) return FLOOR_Y - (worldX - 8800) * 0.12;
        if (worldX >= 9800 && worldX < 10700) return FLOOR_Y - 120.0 + (worldX - 9800) * 0.133;
        return FLOOR_Y;
    }

    private void fail(String reason) {
        if (failed || finished) return;
        failed = true;
        failureReason = reason;
        if (failControls != null) failControls.setVisible(true);
        game.playCampaignCaveCollapseCue();
    }

    private void finishEscape() {
        if (finished) return;
        finished = true;
        stop();
        game.finishCampaignSequenceMusic();
        game.resetAfterCampaignCutscene();
        Runnable callback = onEscaped;
        onEscaped = null;
        if (callback != null) callback.run();
    }

    private void abandon(Stage stage) {
        stop();
        game.finishCampaignSequenceMusic();
        Runnable callback = onAbandoned;
        onAbandoned = null;
        if (callback != null) callback.run();
    }

    private void handlePressed(KeyEvent event, Stage stage) {
        if (event.getCode() == KeyCode.RIGHT || event.getCode() == KeyCode.D) rightHeld = true;
        else if (event.getCode() == KeyCode.LEFT || event.getCode() == KeyCode.A) leftHeld = true;
        else if (event.getCode() == KeyCode.UP || event.getCode() == KeyCode.W
                || event.getCode() == KeyCode.SPACE) {
            if (failed) resetRun(); else jumpQueued = true;
        } else if (event.getCode() == KeyCode.ENTER && failed) resetRun();
        else if (event.getCode() == KeyCode.ESCAPE) abandon(stage);
        else return;
        event.consume();
    }

    private void handleReleased(KeyEvent event) {
        if (event.getCode() == KeyCode.RIGHT || event.getCode() == KeyCode.D) rightHeld = false;
        else if (event.getCode() == KeyCode.LEFT || event.getCode() == KeyCode.A) leftHeld = false;
        else return;
        event.consume();
    }

    private void render(long now) {
        if (canvas == null) return;
        GraphicsContext g = canvas.getGraphicsContext2D();
        g.save();
        g.scale(canvas.getWidth() / WIDTH, canvas.getHeight() / HEIGHT);
        g.setFill(new LinearGradient(0, 0, 0, 1, true, CycleMethod.NO_CYCLE,
                new Stop(0, Color.web("#050816")), new Stop(1, Color.web("#241126"))));
        g.fillRect(0, 0, WIDTH, HEIGHT);
        drawCave(g, now);
        drawPigeon(g);
        drawHud(g);
        if (failed) drawFailure(g);
        g.restore();
    }

    private void drawCave(GraphicsContext g, long now) {
        double blastFront = Math.max(-450.0, ticks * 6.15 - 900.0) - cameraX;
        double pulse = 0.5 + 0.5 * Math.sin(now / 120_000_000.0);
        g.setFill(Color.web("#381014", 0.62 + pulse * 0.12));
        g.fillRect(0, 0, Math.max(0.0, blastFront + 180.0), HEIGHT);
        g.setFill(Color.web("#FF6F00", 0.36 + pulse * 0.20));
        g.fillRect(blastFront, 0, 210, HEIGHT);

        g.setFill(Color.web("#171421"));
        for (int i = 0; i < 45; i++) {
            double wx = i * 560.0 - cameraX;
            double h = 170 + (i % 5) * 55;
            g.fillPolygon(new double[]{wx - 90, wx + 120, wx + 260},
                    new double[]{0, h, 0}, 3);
        }
        g.setFill(Color.web("#2A2433"));
        for (double sx = Math.floor(cameraX / 360.0) * 360.0; sx < cameraX + WIDTH + 500; sx += 360.0) {
            if (!Double.isNaN(floorAt(sx + 180))) {
                g.fillRect(sx - cameraX, FLOOR_Y, 365, HEIGHT - FLOOR_Y);
                g.setFill(Color.web("#4A4052"));
                g.fillRect(sx - cameraX, FLOOR_Y, 365, 18);
                g.setFill(Color.web("#2A2433"));
            }
        }
        for (Gap gap : GAPS) {
            double gx = gap.x - cameraX;
            g.setFill(Color.web("#020207"));
            g.fillRect(gx, FLOOR_Y - 4, gap.width, HEIGHT - FLOOR_Y + 10);
            g.setFill(Color.web("#FF3D00", 0.22 + pulse * 0.18));
            g.fillOval(gx - 40, FLOOR_Y + 120, gap.width + 80, 210);
        }
        for (Rock rock : ROCKS) {
            double rx = rock.x - cameraX;
            double top = FLOOR_Y - rock.height;
            g.setFill(Color.web("#3B3442"));
            g.fillPolygon(new double[]{rx, rx + rock.width * 0.42, rx + rock.width, rx + rock.width * 0.84},
                    new double[]{FLOOR_Y, top, top + rock.height * 0.18, FLOOR_Y}, 4);
            g.setStroke(Color.web("#75677D"));
            g.setLineWidth(5);
            g.strokeLine(rx + rock.width * 0.42, top, rx + rock.width * 0.84, FLOOR_Y);
        }
        g.setFill(Color.web("#B0BEC5", 0.34));
        for (int i = 0; i < 52; i++) {
            double wx = (i * 941.0 + (now / 13_000_000.0) * (8 + i % 5)) % (WORLD_LENGTH + 900.0);
            double sx = wx - cameraX;
            if (sx < -80 || sx > WIDTH + 80) continue;
            double sy = (i * 137.0 + now / 7_000_000.0) % 760.0;
            g.fillOval(sx, sy, 8 + i % 4 * 4, 14 + i % 3 * 5);
        }
    }

    private void drawPigeon(GraphicsContext g) {
        if (pigeon == null) return;
        pigeon.x = x - cameraX - pigeon.bodyWidth() * 0.5;
        pigeon.y = y;
        pigeon.prevX = pigeon.x;
        pigeon.prevY = pigeon.y;
        pigeon.vx = rightHeld ? 10.8 : 8.1;
        pigeon.vy = vy;
        pigeon.facingRight = true;
        pigeon.draw(g);
    }

    private void drawHud(GraphicsContext g) {
        g.setFill(Color.web("#02050B", 0.86));
        g.fillRoundRect(54, 45, 620, 118, 18, 18);
        g.setStroke(Color.web("#80DEEA"));
        g.setLineWidth(3);
        g.strokeRoundRect(54, 45, 620, 118, 18, 18);
        g.setFill(Color.web("#80DEEA"));
        g.setFont(Font.font("Consolas", FontWeight.BOLD, 23));
        g.fillText("THE LAST FLIGHT  •  ESCAPE THE COLLAPSE", 82, 85);
        g.setFill(Color.WHITE);
        g.setFont(Font.font("Arial Black", FontWeight.BOLD, 31));
        int seconds = Math.max(0, (LIMIT_TICKS - ticks + 59) / 60);
        g.fillText("TIME " + seconds + "    DISTANCE " + (int) (100.0 * x / WORLD_LENGTH) + "%", 82, 132);
        g.setTextAlign(TextAlignment.RIGHT);
        g.setFont(Font.font("Consolas", FontWeight.BOLD, 20));
        g.fillText("RUN: A/D OR ←/→   JUMP: W/↑/SPACE", WIDTH - 66, 78);
        g.setTextAlign(TextAlignment.LEFT);
    }

    private void drawFailure(GraphicsContext g) {
        g.setFill(Color.web("#02030A", 0.74));
        g.fillRect(0, 0, WIDTH, HEIGHT);
        g.setTextAlign(TextAlignment.CENTER);
        g.setFill(Color.web("#FF8A80"));
        g.setFont(Font.font("Arial Black", FontWeight.BOLD, 64));
        g.fillText("THE CAVE CLOSED", WIDTH * 0.5, 420);
        g.setFill(Color.WHITE);
        g.setFont(Font.font("Arial", FontWeight.NORMAL, 28));
        g.fillText(failureReason, WIDTH * 0.5, 474);
        g.setTextAlign(TextAlignment.LEFT);
    }

    private void stop() {
        if (timer != null) {
            timer.stop();
            timer = null;
        }
        if (canvas != null) {
            Parent parent = canvas.getParent();
            if (parent instanceof Pane pane) pane.getChildren().remove(canvas);
        }
    }

    private record Gap(double x, double width) {}
    private record Rock(double x, double width, double height) {}
}
