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
import javafx.scene.paint.RadialGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.ArcType;
import javafx.scene.shape.StrokeLineCap;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.TextAlignment;
import javafx.stage.Stage;

import java.util.Arrays;
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
    private double cameraX;
    private boolean rightHeld;
    private boolean leftHeld;
    private boolean jumpHeld;
    private boolean downHeld;
    private boolean attackHeld;
    private boolean specialHeld;
    private boolean grabHeld;
    private boolean controllerLeftHeld;
    private boolean controllerRightHeld;
    private boolean controllerJumpHeld;
    private boolean controllerDownHeld;
    private boolean controllerAttackHeld;
    private boolean controllerSpecialHeld;
    private boolean controllerGrabHeld;
    private boolean controllerAttackUpHeld;
    private boolean controllerAttackDownHeld;
    private boolean lastMergedLeft;
    private boolean lastMergedRight;
    private final boolean[] destroyedRocks = new boolean[ROCKS.size()];
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
        game.prepareCampaignPlayableSequenceScene(scene, root, content);
        game.addCampaignSceneEventFilter(scene, KeyEvent.KEY_PRESSED,
                event -> handlePressed(event, stage));
        game.addCampaignSceneEventFilter(scene, KeyEvent.KEY_RELEASED, this::handleReleased);
        game.setCampaignScene(stage, scene);
        // The farewell cue belongs to Pigeon and Eagle's last conversation.
        // Control begins with a separate urgent escape cue.
        game.startCampaignSequenceMusic("music-escape.mp3", true);
        game.playCampaignCaveCollapseCue();

        timer = new AnimationTimer() {
            @Override public void handle(long now) {
                if (lastNow == 0L) lastNow = now;
                pollControllerInput();
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
        cameraX = 0.0;
        ticks = 0;
        accumulator = 0L;
        lastNow = 0L;
        rightHeld = false;
        leftHeld = false;
        jumpHeld = false;
        downHeld = false;
        attackHeld = false;
        specialHeld = false;
        grabHeld = false;
        controllerLeftHeld = false;
        controllerRightHeld = false;
        controllerJumpHeld = false;
        controllerDownHeld = false;
        controllerAttackHeld = false;
        controllerSpecialHeld = false;
        controllerGrabHeld = false;
        controllerAttackUpHeld = false;
        controllerAttackDownHeld = false;
        lastMergedLeft = false;
        lastMergedRight = false;
        Arrays.fill(destroyedRocks, false);
        failed = false;
        finished = false;
        failureReason = "";
        if (failControls != null) failControls.setVisible(false);
        pigeon = game.createCampaignEscapeBird(null);
        pigeon.x = 520.0;
        pigeon.y = collisionFloorAt(pigeon.x + pigeon.bodyWidth() * 0.5) - pigeon.bodyHeight();
        pigeon.prevX = pigeon.x;
        pigeon.prevY = pigeon.y;
        pigeon.vx = 0.0;
        pigeon.vy = 0.0;
        pigeon.facingRight = true;
        game.beginCampaignCaveEscapePhysics(pigeon, this::collisionFloorAt,
                WORLD_LENGTH, HEIGHT + 180.0);
    }

    private void tick() {
        ticks++;
        boolean moveLeft = leftHeld || controllerLeftHeld;
        boolean moveRight = rightHeld || controllerRightHeld;
        boolean jump = jumpHeld || controllerJumpHeld;
        boolean attack = attackHeld || controllerAttackHeld;
        boolean special = specialHeld || controllerSpecialHeld;
        boolean grab = grabHeld || controllerGrabHeld;
        boolean block = downHeld || controllerDownHeld;
        boolean attackUp = controllerAttackUpHeld || (attack && jump && !block);
        boolean attackDown = controllerAttackDownHeld || (attack && block);
        game.setCampaignSequenceActions(moveLeft, moveRight, jump, attack, special,
                grab, block, attackUp, attackDown);
        if (moveLeft && !lastMergedLeft) pigeon.registerDashTap(-1);
        if (moveRight && !lastMergedRight) pigeon.registerDashTap(1);
        lastMergedLeft = moveLeft;
        lastMergedRight = moveRight;

        double previousX = pigeon.x;
        double previousY = pigeon.y;
        pigeon.update(1.0);
        game.updateCampaignSequenceParticles();
        resolveRockCollisions(previousX, previousY);

        double blastFront = collapseFrontWorldX();
        double pigeonCenterX = pigeon.bodyCenterX();
        if (pigeonCenterX < blastFront + 155.0) {
            fail("The charge overtook the tunnel.");
        } else if (pigeon.y > HEIGHT + 180.0) {
            fail("Pigeon fell beneath the escape route.");
        } else if (ticks >= LIMIT_TICKS) {
            fail("The tunnel sealed before Pigeon escaped.");
        } else if (pigeonCenterX >= WORLD_LENGTH - 360.0) {
            finishEscape();
        }
        cameraX = Math.clamp(pigeonCenterX - 520.0, 0.0, WORLD_LENGTH - WIDTH);
    }

    private void resolveRockCollisions(double previousX, double previousY) {
        double birdLeft = pigeon.x;
        double birdRight = pigeon.x + pigeon.bodyWidth();
        double birdTop = pigeon.y;
        double birdBottom = pigeon.bodyBottomY();
        boolean specialActive = pigeon.pigeonFeatherCharging
                || pigeon.pigeonFeatherBurstTimer > 0
                || pigeon.pigeonRushTimer > 0
                || pigeon.pigeonFlutterTimer > 0
                || pigeon.pigeonScavengeTimer > 0;
        boolean attackActive = pigeon.attackAnimationTimer > 0;
        for (int i = 0; i < ROCKS.size(); i++) {
            if (destroyedRocks[i]) continue;
            Rock rock = ROCKS.get(i);
            double rockTop = FLOOR_Y - rock.height;
            if (birdRight <= rock.x || birdLeft >= rock.x + rock.width
                    || birdBottom <= rockTop || birdTop >= FLOOR_Y) {
                continue;
            }

            boolean approachingFront = pigeon.facingRight
                    ? pigeon.bodyCenterX() <= rock.x + rock.width * 0.58
                    : pigeon.bodyCenterX() >= rock.x + rock.width * 0.42;
            if (specialActive || (attackActive && rock.width <= 145.0 && approachingFront)) {
                destroyedRocks[i] = true;
                pigeon.vx *= 0.72;
                game.playCampaignCaveRockBreakCue();
                continue;
            }

            double previousRight = previousX + pigeon.bodyWidth();
            double previousLeft = previousX;
            if (previousRight <= rock.x + 10.0 && pigeon.vx >= 0.0) {
                pigeon.x = rock.x - pigeon.bodyWidth();
                pigeon.vx = Math.min(0.0, pigeon.vx);
            } else if (previousLeft >= rock.x + rock.width - 10.0 && pigeon.vx <= 0.0) {
                pigeon.x = rock.x + rock.width;
                pigeon.vx = Math.max(0.0, pigeon.vx);
            } else if (previousY + pigeon.bodyHeight() > rockTop + 8.0) {
                boolean pushLeft = pigeon.bodyCenterX() < rock.x + rock.width * 0.5;
                pigeon.x = pushLeft ? rock.x - pigeon.bodyWidth() : rock.x + rock.width;
                pigeon.vx = pushLeft ? Math.min(0.0, pigeon.vx) : Math.max(0.0, pigeon.vx);
            }
            birdLeft = pigeon.x;
            birdRight = pigeon.x + pigeon.bodyWidth();
            birdTop = pigeon.y;
            birdBottom = pigeon.bodyBottomY();
        }
    }

    private double collapseFrontWorldX() {
        return Math.max(-450.0, ticks * 6.15 - 900.0);
    }

    private double collapseDistance() {
        return pigeon == null ? 0.0 : pigeon.bodyCenterX() - (collapseFrontWorldX() + 155.0);
    }

    private double terrainFloorAt(double worldX) {
        for (Gap gap : GAPS) {
            if (worldX > gap.x && worldX < gap.x + gap.width) return Double.NaN;
        }
        if (worldX > 8800 && worldX < 9800) return FLOOR_Y - (worldX - 8800) * 0.12;
        if (worldX >= 9800 && worldX < 10700) return FLOOR_Y - 120.0 + (worldX - 9800) * 0.133;
        return FLOOR_Y;
    }

    private double collisionFloorAt(double worldX) {
        double floor = terrainFloorAt(worldX);
        for (int i = 0; i < ROCKS.size(); i++) {
            if (destroyedRocks[i]) continue;
            Rock rock = ROCKS.get(i);
            if (worldX >= rock.x && worldX <= rock.x + rock.width) {
                double rockTop = FLOOR_Y - rock.height;
                floor = Double.isFinite(floor) ? Math.min(floor, rockTop) : rockTop;
            }
        }
        return floor;
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
        // The epilogue requests the same emotional track and resumes it in place.
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
        KeyCode code = event.getCode();
        if (code == game.rightKeyForPlayer(0) || code == KeyCode.RIGHT) rightHeld = true;
        else if (code == game.leftKeyForPlayer(0) || code == KeyCode.LEFT) leftHeld = true;
        else if (code == game.jumpKeyForPlayer(0) || code == KeyCode.UP) {
            if (failed) resetRun();
            jumpHeld = true;
        } else if (code == game.attackKeyForPlayer(0)) {
            if (failed) resetRun();
            attackHeld = true;
        } else if (code == game.specialKeyForPlayer(0)) {
            if (failed) resetRun();
            specialHeld = true;
        } else if (code == game.grabKeyForPlayer(0)) {
            if (failed) resetRun();
            grabHeld = true;
        } else if (code == game.blockKeyForPlayer(0) || code == KeyCode.DOWN) {
            downHeld = true;
        } else if (code == KeyCode.ENTER && failed) resetRun();
        else if (code == KeyCode.ESCAPE) abandon(stage);
        else return;
        event.consume();
    }

    private void handleReleased(KeyEvent event) {
        KeyCode code = event.getCode();
        if (code == game.rightKeyForPlayer(0) || code == KeyCode.RIGHT) rightHeld = false;
        else if (code == game.leftKeyForPlayer(0) || code == KeyCode.LEFT) leftHeld = false;
        else if (code == game.jumpKeyForPlayer(0) || code == KeyCode.UP) jumpHeld = false;
        else if (code == game.attackKeyForPlayer(0)) attackHeld = false;
        else if (code == game.specialKeyForPlayer(0)) specialHeld = false;
        else if (code == game.grabKeyForPlayer(0)) grabHeld = false;
        else if (code == game.blockKeyForPlayer(0) || code == KeyCode.DOWN) downHeld = false;
        else return;
        event.consume();
    }

    private void pollControllerInput() {
        WiimoteMappedState state = game.campaignSequenceControllerState();
        boolean connected = state != null && state.connected();
        controllerLeftHeld = connected && state.left();
        controllerRightHeld = connected && state.right();
        controllerJumpHeld = connected && state.jump();
        controllerDownHeld = connected && state.block();
        controllerAttackHeld = connected && state.attack();
        controllerSpecialHeld = connected && state.special();
        controllerGrabHeld = connected && state.grab();
        controllerAttackUpHeld = connected && state.attackUp();
        controllerAttackDownHeld = connected && state.attackDown();
        if (failed && connected && (controllerJumpHeld || controllerAttackHeld
                || controllerSpecialHeld || controllerGrabHeld)) {
            resetRun();
        }
    }

    private void render(long now) {
        if (canvas == null) return;
        GraphicsContext g = canvas.getGraphicsContext2D();
        g.save();
        g.scale(canvas.getWidth() / WIDTH, canvas.getHeight() / HEIGHT);
        double danger = Math.clamp(1.0 - collapseDistance() / 780.0, 0.0, 1.0);
        double shake = danger * danger * 7.0;
        g.save();
        g.translate(Math.sin(now / 13_000_000.0) * shake,
                Math.cos(now / 17_000_000.0) * shake * 0.55);
        drawCave(g, now);
        drawPigeon(g);
        g.restore();
        drawHud(g);
        if (failed) drawFailure(g);
        g.restore();
    }

    private void drawCave(GraphicsContext g, long now) {
        double time = now / 1_000_000_000.0;
        double blastFront = collapseFrontWorldX() - cameraX;
        double pulse = 0.5 + 0.5 * Math.sin(time * 8.0);
        g.setFill(new LinearGradient(0, 0, 0, 1, true, CycleMethod.NO_CYCLE,
                new Stop(0, Color.web("#050714")),
                new Stop(0.58, Color.web("#171324")),
                new Stop(1, Color.web("#2A182A"))));
        g.fillRect(-24, -24, WIDTH + 48, HEIGHT + 48);

        drawExitLight(g, time);
        drawCaveDepth(g, time);
        drawTunnelRemains(g);
        drawFloor(g);

        for (Gap gap : GAPS) {
            double gx = gap.x - cameraX;
            g.setFill(Color.web("#020207"));
            g.fillRect(gx, FLOOR_Y - 4, gap.width, HEIGHT - FLOOR_Y + 10);
            g.setFill(new RadialGradient(0, 0, gx + gap.width * 0.5, FLOOR_Y + 160,
                    gap.width * 0.7, false, CycleMethod.NO_CYCLE,
                    new Stop(0, Color.web("#FF6D00", 0.35 + pulse * 0.16)),
                    new Stop(0.55, Color.web("#D50000", 0.16)),
                    new Stop(1, Color.TRANSPARENT)));
            g.fillOval(gx - 55, FLOOR_Y + 55, gap.width + 110, 300);
            g.setStroke(Color.web("#FF8A65", 0.28));
            g.setLineWidth(4);
            g.strokeLine(gx - 8, FLOOR_Y + 4, gx + 35, FLOOR_Y + 42);
            g.strokeLine(gx + gap.width + 8, FLOOR_Y + 4,
                    gx + gap.width - 32, FLOOR_Y + 48);
        }
        for (int i = 0; i < ROCKS.size(); i++) {
            if (destroyedRocks[i]) continue;
            Rock rock = ROCKS.get(i);
            double rx = rock.x - cameraX;
            double top = FLOOR_Y - rock.height;
            g.setFill(new LinearGradient(rx, top, rx + rock.width, FLOOR_Y, false,
                    CycleMethod.NO_CYCLE,
                    new Stop(0, Color.web("#5E5266")),
                    new Stop(0.48, Color.web("#3B3442")),
                    new Stop(1, Color.web("#211D29"))));
            g.fillPolygon(new double[]{rx, rx + rock.width * 0.42, rx + rock.width, rx + rock.width * 0.84},
                    new double[]{FLOOR_Y, top, top + rock.height * 0.18, FLOOR_Y}, 4);
            g.setStroke(Color.web("#9A899F", 0.62));
            g.setLineWidth(4);
            g.strokeLine(rx + rock.width * 0.42, top, rx + rock.width * 0.84, FLOOR_Y);
            g.setStroke(Color.web("#17131D", 0.72));
            g.setLineWidth(3);
            g.strokeLine(rx + rock.width * 0.42, top,
                    rx + rock.width * 0.55, top + rock.height * 0.42);
            g.strokeLine(rx + rock.width * 0.55, top + rock.height * 0.42,
                    rx + rock.width * 0.35, top + rock.height * 0.67);
        }

        drawCollapseFront(g, blastFront, time, pulse);

        for (int i = 0; i < 42; i++) {
            double wx = (i * 941.0 + time * (65 + i % 5 * 17)) % (WORLD_LENGTH + 900.0);
            double sx = wx - cameraX;
            if (sx < -80 || sx > WIDTH + 80) continue;
            double sy = Math.floorMod((long) (i * 137.0 + time * (95 + i % 4 * 24)), 760L);
            double size = 5 + i % 4 * 3;
            g.setFill(Color.web(i % 5 == 0 ? "#FFCC80" : "#B0BEC5",
                    i % 5 == 0 ? 0.34 : 0.22));
            g.fillOval(sx, sy, size, size * 1.45);
        }

        drawForegroundVignette(g);
    }

    private void drawExitLight(GraphicsContext g, double time) {
        double exitX = WORLD_LENGTH - 250.0 - cameraX;
        if (exitX > WIDTH + 1500.0 || exitX < -900.0) return;
        double pulse = 0.92 + Math.sin(time * 1.7) * 0.06;
        g.setFill(new RadialGradient(0, 0, exitX, 430, 760 * pulse, false,
                CycleMethod.NO_CYCLE,
                new Stop(0, Color.web("#FFFDE7", 0.96)),
                new Stop(0.18, Color.web("#FFE082", 0.66)),
                new Stop(0.52, Color.web("#80DEEA", 0.19)),
                new Stop(1, Color.TRANSPARENT)));
        g.fillOval(exitX - 760, -250, 1520, 1420);
        g.setFill(Color.web("#FFF8E1", 0.82));
        g.fillPolygon(new double[]{exitX - 105, exitX + 115, exitX + 420, exitX - 390},
                new double[]{85, 85, FLOOR_Y, FLOOR_Y}, 4);
    }

    private void drawCaveDepth(GraphicsContext g, double time) {
        g.setFill(Color.web("#0B0D1B"));
        for (int i = 0; i < 20; i++) {
            double sx = i * 1240.0 - cameraX * 0.22 - 520.0;
            double width = 520 + i % 3 * 130;
            g.fillOval(sx, 190 + i % 4 * 72, width, 590 - i % 3 * 45);
        }

        g.setStroke(Color.web("#57485F", 0.23));
        g.setLineWidth(18);
        for (int i = 0; i < 14; i++) {
            double sx = i * 1760.0 - cameraX * 0.42;
            g.strokeArc(sx - 280, 170, 560, 710, 12, 156, ArcType.OPEN);
        }

        g.setFill(Color.web("#211C2B"));
        for (int i = 0; i < 46; i++) {
            double sx = i * 520.0 - cameraX * 0.72;
            double depth = 135 + (i * 47 % 240);
            double width = 165 + i % 4 * 48;
            g.fillPolygon(new double[]{sx - width * 0.5, sx, sx + width * 0.5},
                    new double[]{-10, depth, -10}, 3);
        }

        g.setStroke(Color.web("#7E6A85", 0.18));
        g.setLineWidth(3);
        for (int i = 0; i < 32; i++) {
            double sx = i * 730.0 - cameraX * 0.55;
            double drift = Math.sin(time * 0.35 + i) * 7.0;
            g.strokeLine(sx, 210 + i % 5 * 85 + drift,
                    sx + 170 + i % 3 * 70, 285 + i % 5 * 85 + drift);
        }
    }

    private void drawTunnelRemains(GraphicsContext g) {
        for (int i = 0; i < 18; i++) {
            double worldX = 980.0 + i * 1280.0;
            double sx = worldX - cameraX;
            if (sx < -260 || sx > WIDTH + 260) continue;
            g.setStroke(Color.web("#46515B", 0.54));
            g.setLineWidth(15);
            g.strokeLine(sx - 150, FLOOR_Y, sx - 105, 330 + i % 3 * 55);
            g.strokeLine(sx + 150, FLOOR_Y, sx + 92, 345 + i % 4 * 48);
            g.setStroke(Color.web("#75838D", 0.35));
            g.setLineWidth(5);
            g.strokeArc(sx - 145, 270 + i % 3 * 35, 290, 235, 4, 172, ArcType.OPEN);
            g.setFill(Color.web("#D6A72D", 0.38));
            g.fillRect(sx - 126, FLOOR_Y - 18, 32, 6);
            g.fillRect(sx + 92, FLOOR_Y - 18, 32, 6);
        }

        String[] sectionNames = {"CROWN CORE", "FAULT GALLERY", "OLD MIGRATION WAY", "SURFACE BREACH"};
        double[] sectionX = {1500, 7200, 13200, 19000};
        for (int i = 0; i < sectionX.length; i++) {
            double sx = sectionX[i] - cameraX;
            if (sx < -300 || sx > WIDTH + 300) continue;
            g.setFill(Color.web("#090D15", 0.88));
            g.fillRoundRect(sx - 135, 510, 270, 52, 8, 8);
            g.setStroke(Color.web(i == 3 ? "#FFE082" : "#607D8B", 0.72));
            g.setLineWidth(2);
            g.strokeRoundRect(sx - 135, 510, 270, 52, 8, 8);
            g.setFill(Color.web(i == 3 ? "#FFE082" : "#B0BEC5", 0.84));
            g.setFont(Font.font("Consolas", FontWeight.BOLD, 17));
            g.setTextAlign(TextAlignment.CENTER);
            g.fillText(sectionNames[i], sx, 543);
            g.setTextAlign(TextAlignment.LEFT);
        }
    }

    private void drawFloor(GraphicsContext g) {
        double startWorld = Math.floor(cameraX / 52.0) * 52.0 - 52.0;
        for (double worldX = startWorld; worldX < cameraX + WIDTH + 104.0; worldX += 52.0) {
            double floor = terrainFloorAt(worldX + 26.0);
            if (Double.isNaN(floor)) continue;
            double nextFloor = terrainFloorAt(worldX + 78.0);
            if (Double.isNaN(nextFloor)) nextFloor = floor;
            double sx = worldX - cameraX;
            g.setFill(worldX % 208.0 == 0.0 ? Color.web("#302938") : Color.web("#292330"));
            g.fillPolygon(new double[]{sx, sx + 54, sx + 54, sx},
                    new double[]{floor, nextFloor, HEIGHT + 30, HEIGHT + 30}, 4);
            g.setStroke(Color.web("#71627A", 0.76));
            g.setLineWidth(4);
            g.strokeLine(sx, floor, sx + 54, nextFloor);
            if (((long) worldX / 52L) % 4 == 1) {
                g.setStroke(Color.web("#121018", 0.84));
                g.setLineWidth(2);
                g.strokeLine(sx + 17, floor + 7, sx + 31, floor + 25);
                g.strokeLine(sx + 31, floor + 25, sx + 24, floor + 43);
            }
        }
        for (double worldX = Math.floor(cameraX / 840.0) * 840.0;
             worldX < cameraX + WIDTH + 840.0; worldX += 840.0) {
            double floor = terrainFloorAt(worldX + 90.0);
            if (Double.isNaN(floor)) continue;
            double sx = worldX + 90.0 - cameraX;
            g.setFill(Color.web("#80DEEA", 0.34));
            g.fillPolygon(new double[]{sx - 34, sx + 4, sx - 34, sx - 16},
                    new double[]{floor - 34, floor - 18, floor - 2, floor - 18}, 4);
        }
    }

    private void drawCollapseFront(GraphicsContext g, double blastFront, double time, double pulse) {
        g.setFill(new LinearGradient(blastFront - 360, 0, blastFront + 250, 0, false,
                CycleMethod.NO_CYCLE,
                new Stop(0, Color.web("#27080B", 0.88)),
                new Stop(0.58, Color.web("#B71C1C", 0.52)),
                new Stop(0.82, Color.web("#FF6D00", 0.62 + pulse * 0.18)),
                new Stop(1, Color.TRANSPARENT)));
        g.fillRect(-40, -20, Math.max(0.0, blastFront + 310), HEIGHT + 40);
        g.setStroke(Color.web("#FFF3E0", 0.46 + pulse * 0.28));
        g.setLineWidth(8 + pulse * 8);
        g.strokeLine(blastFront + 18, 0, blastFront + 18, HEIGHT);
        g.setLineCap(StrokeLineCap.ROUND);
        for (int i = 0; i < 14; i++) {
            double sy = Math.floorMod((long) (i * 107 + time * (180 + i * 13)), 1040L);
            double reach = 55 + i % 5 * 25;
            g.setStroke(Color.web(i % 3 == 0 ? "#FFF8E1" : "#FF8A65", 0.58));
            g.setLineWidth(3 + i % 3);
            g.strokeLine(blastFront + 22, sy, blastFront + 22 + reach, sy - 22 - i % 4 * 9);
        }
        g.setLineCap(StrokeLineCap.BUTT);
    }

    private void drawForegroundVignette(GraphicsContext g) {
        g.setFill(new LinearGradient(0, 0, 0, 1, true, CycleMethod.NO_CYCLE,
                new Stop(0, Color.web("#010208", 0.82)),
                new Stop(0.13, Color.TRANSPARENT),
                new Stop(0.78, Color.TRANSPARENT),
                new Stop(1, Color.web("#010208", 0.74))));
        g.fillRect(-20, -20, WIDTH + 40, HEIGHT + 40);
    }

    private void drawPigeon(GraphicsContext g) {
        if (pigeon == null) return;
        double screenX = pigeon.bodyCenterX() - cameraX;
        double screenY = pigeon.bodyCenterY();
        double speed = Math.abs(pigeon.vx);
        g.setFill(new RadialGradient(0, 0, screenX, screenY, 118, false,
                CycleMethod.NO_CYCLE,
                new Stop(0, Color.web("#FFF8E1", 0.16)),
                new Stop(0.48, Color.web("#80DEEA", 0.08)),
                new Stop(1, Color.TRANSPARENT)));
        g.fillOval(screenX - 118, screenY - 118, 236, 236);
        if (speed > 3.5) {
            double direction = pigeon.vx >= 0.0 ? -1.0 : 1.0;
            g.setLineCap(StrokeLineCap.ROUND);
            for (int i = 0; i < 5; i++) {
                double lineY = screenY - 35 + i * 18.0;
                double length = (28 + i * 11) * Math.min(1.0, speed / 10.8);
                g.setStroke(Color.web(i % 2 == 0 ? "#E1F5FE" : "#80DEEA", 0.24));
                g.setLineWidth(3 + i * 0.35);
                g.strokeLine(screenX + direction * 48, lineY,
                        screenX + direction * (48 + length), lineY);
            }
            g.setLineCap(StrokeLineCap.BUTT);
        }
        if (pigeon.isOnGround() && speed > 1.5) {
            for (int i = 0; i < 4; i++) {
                double drift = Math.floorMod(ticks * (5L + i) + i * 19L, 46L);
                g.setFill(Color.web("#B0BEC5", 0.16 + i * 0.025));
                g.fillOval(screenX - Math.signum(pigeon.vx) * (36 + drift),
                        pigeon.bodyBottomY() - 5 - i * 5,
                        9 + i * 2, 5 + i);
            }
        }

        game.drawCampaignSequenceParticles(g, cameraX);
        g.save();
        g.translate(-cameraX, 0.0);
        pigeon.draw(g);
        g.restore();
    }

    private void drawHud(GraphicsContext g) {
        g.setFill(Color.web("#02050B", 0.86));
        g.fillRoundRect(54, 45, 720, 158, 18, 18);
        g.setStroke(Color.web("#80DEEA"));
        g.setLineWidth(3);
        g.strokeRoundRect(54, 45, 720, 158, 18, 18);
        g.setFill(Color.web("#80DEEA"));
        g.setFont(Font.font("Consolas", FontWeight.BOLD, 23));
        g.fillText("THE LAST FLIGHT  -  ESCAPE THE COLLAPSE", 82, 85);
        g.setFill(Color.WHITE);
        g.setFont(Font.font("Arial Black", FontWeight.BOLD, 29));
        int seconds = Math.max(0, (LIMIT_TICKS - ticks + 59) / 60);
        double progress = pigeon == null ? 0.0
                : Math.clamp(pigeon.bodyCenterX() / WORLD_LENGTH, 0.0, 1.0);
        g.fillText("TIME " + seconds + "    ROUTE " + (int) (progress * 100.0) + "%", 82, 126);
        g.setFill(Color.web("#17242D"));
        g.fillRoundRect(82, 151, 658, 22, 11, 11);
        g.setFill(new LinearGradient(82, 0, 740, 0, false, CycleMethod.NO_CYCLE,
                new Stop(0, Color.web("#26C6DA")), new Stop(1, Color.web("#FFE082"))));
        g.fillRoundRect(82, 151, 658 * progress, 22, 11, 11);
        g.setStroke(Color.web("#CFD8DC", 0.58));
        g.setLineWidth(2);
        g.strokeRoundRect(82, 151, 658, 22, 11, 11);

        double threat = Math.clamp(1.0 - collapseDistance() / 1100.0, 0.0, 1.0);
        g.setFill(Color.web("#02050B", 0.86));
        g.fillRoundRect(WIDTH - 775, 45, 721, 132, 18, 18);
        g.setStroke(threat > 0.62 ? Color.web("#FF7043") : Color.web("#78909C"));
        g.strokeRoundRect(WIDTH - 775, 45, 721, 132, 18, 18);
        g.setTextAlign(TextAlignment.RIGHT);
        g.setFont(Font.font("Consolas", FontWeight.BOLD, 18));
        g.setFill(Color.web("#ECEFF1"));
        g.fillText("MOVE " + keyName(game.leftKeyForPlayer(0)) + "/" + keyName(game.rightKeyForPlayer(0))
                        + "   FLY " + keyName(game.jumpKeyForPlayer(0))
                        + "   ATTACK " + keyName(game.attackKeyForPlayer(0))
                        + "   SPECIAL " + keyName(game.specialKeyForPlayer(0))
                        + "   GRAB " + keyName(game.grabKeyForPlayer(0)),
                WIDTH - 82, 82);
        g.setFill(threat > 0.62 ? Color.web("#FF8A65") : Color.web("#B0BEC5"));
        g.fillText(threat > 0.78 ? "THE BLAST IS CLOSING" : "FULL PIGEON MOVESET ACTIVE",
                WIDTH - 82, 116);
        g.setFill(Color.web("#151B23"));
        g.fillRoundRect(WIDTH - 740, 137, 658, 15, 8, 8);
        g.setFill(Color.web("#FF5722", 0.82));
        g.fillRoundRect(WIDTH - 740, 137, 658 * threat, 15, 8, 8);
        g.setTextAlign(TextAlignment.LEFT);
    }

    private String keyName(KeyCode key) {
        if (key == null || key == KeyCode.UNDEFINED) return "?";
        return key.getName().toUpperCase();
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

    void resetForTest() {
        resetRun();
    }

    void setControlsForTest(boolean left, boolean right, boolean jump, boolean down,
                            boolean attack, boolean special) {
        leftHeld = left;
        rightHeld = right;
        jumpHeld = jump;
        downHeld = down;
        attackHeld = attack;
        specialHeld = special;
    }

    void setControllerControlsForTest(boolean left, boolean right, boolean jump, boolean down,
                                      boolean attack, boolean special) {
        controllerLeftHeld = left;
        controllerRightHeld = right;
        controllerJumpHeld = jump;
        controllerDownHeld = down;
        controllerAttackHeld = attack;
        controllerSpecialHeld = special;
    }

    void tickForTest() {
        tick();
    }

    double progressXForTest() {
        return pigeon == null ? 0.0 : pigeon.bodyCenterX();
    }

    double verticalPositionForTest() {
        return pigeon == null ? 0.0 : pigeon.y;
    }

    Bird pigeonForTest() {
        return pigeon;
    }

    private void stop() {
        if (timer != null) {
            timer.stop();
            timer = null;
        }
        if (canvas != null) {
            Parent parent = canvas.getParent();
            if (parent instanceof Pane pane) pane.getChildren().remove(canvas);
            canvas = null;
        }
        game.endCampaignCaveEscapePhysics();
    }

    private record Gap(double x, double width) {}
    private record Rock(double x, double width, double height) {}
}
