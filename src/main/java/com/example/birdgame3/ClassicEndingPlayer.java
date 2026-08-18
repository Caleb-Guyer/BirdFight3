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
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;
import javafx.stage.Stage;

import java.util.ArrayList;
import java.util.List;

/**
 * Moving-picture Classic epilogues. Unlike Story Mode scenes, these are
 * automatic visual montages narrated only by the route bird.
 */
final class ClassicEndingPlayer {
    private static final double LOGICAL_WIDTH = 1920.0;
    private static final double LOGICAL_HEIGHT = 1080.0;
    private static final double BACKING_WIDTH = 1600.0;
    private static final double BACKING_HEIGHT = 900.0;
    private static final double ACTOR_SIZE = 2.55;
    private static final Font TITLE_FONT = Font.font("Arial Black", FontWeight.BOLD, 34);
    private static final Font NARRATION_FONT = Font.font("Arial", FontWeight.SEMI_BOLD, 34);
    private static final Font RECORD_FONT = Font.font("Consolas", FontWeight.BOLD, 20);

    private final BirdGame3 game;
    private final List<Bird> flock = new ArrayList<>();
    private ClassicEndingContent.Cinematic cinematic;
    private Bird narrator;
    private Bird boss;
    private Canvas canvas;
    private AnimationTimer timer;
    private Button pauseButton;
    private Runnable onFinished;
    private int beatIndex;
    private long beatStartNanos;
    private long pausedAtNanos;
    private long accumulatedPauseNanos;
    private boolean paused;
    private boolean finished;

    ClassicEndingPlayer(BirdGame3 game) {
        this.game = game;
    }

    void play(Stage stage, ClassicEndingContent.Cinematic cinematic,
              String narratorSkinKey, Runnable onFinished) {
        stopTimer();
        game.resetAfterCampaignCutscene();
        this.cinematic = cinematic;
        this.onFinished = onFinished;
        this.beatIndex = 0;
        this.beatStartNanos = 0L;
        this.pausedAtNanos = 0L;
        this.accumulatedPauseNanos = 0L;
        this.paused = false;
        this.finished = false;
        prepareActors(narratorSkinKey);

        if (canvas == null) {
            canvas = new Canvas(BACKING_WIDTH, BACKING_HEIGHT);
            canvas.setScaleX(LOGICAL_WIDTH / BACKING_WIDTH);
            canvas.setScaleY(LOGICAL_HEIGHT / BACKING_HEIGHT);
        } else {
            Parent parent = canvas.getParent();
            if (parent instanceof Pane pane) pane.getChildren().remove(canvas);
        }

        StackPane content = new StackPane(canvas);
        content.setMinSize(LOGICAL_WIDTH, LOGICAL_HEIGHT);
        content.setPrefSize(LOGICAL_WIDTH, LOGICAL_HEIGHT);
        content.setMaxSize(LOGICAL_WIDTH, LOGICAL_HEIGHT);
        content.setStyle("-fx-background-color: black;");

        Button previous = controlButton("PREVIOUS", () -> moveBeat(-1));
        pauseButton = controlButton("PAUSE", () -> {
            togglePause();
            refreshPauseButton();
        });
        Button next = controlButton("NEXT", () -> moveBeat(1));
        Button skip = controlButton("SKIP", this::finish);
        HBox controls = new HBox(8, previous, pauseButton, next, skip);
        controls.setAlignment(Pos.CENTER_RIGHT);
        controls.setPadding(new Insets(7, 12, 7, 12));
        controls.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
        controls.setStyle("-fx-background-color: rgba(3,7,12,0.68);"
                + "-fx-background-radius: 10; -fx-border-color: rgba(255,224,130,0.30);"
                + "-fx-border-radius: 10;");
        StackPane.setAlignment(controls, Pos.TOP_RIGHT);
        StackPane.setMargin(controls, new Insets(18, 22, 0, 0));
        content.getChildren().add(controls);

        StackPane root = new StackPane(content);
        root.getProperties().put("noAutoScale", true);
        root.setStyle("-fx-background-color: black;");
        Scene scene = new Scene(root, LOGICAL_WIDTH, LOGICAL_HEIGHT, Color.BLACK);
        game.prepareCampaignCutsceneScene(scene, root, content);
        scene.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            if (event.getCode() == KeyCode.SPACE || event.getCode() == KeyCode.ENTER
                    || event.getCode() == KeyCode.RIGHT) {
                moveBeat(1);
            } else if (event.getCode() == KeyCode.LEFT) {
                moveBeat(-1);
            } else if (event.getCode() == KeyCode.P) {
                togglePause();
                refreshPauseButton();
            } else if (event.getCode() == KeyCode.ESCAPE) {
                finish();
            } else {
                return;
            }
            event.consume();
        });
        game.setCampaignScene(stage, scene);
        game.startCampaignSequenceMusic(cinematic.musicCue(), true);
        presentBeat();

        timer = new AnimationTimer() {
            @Override
            public void handle(long now) {
                if (beatStartNanos == 0L) beatStartNanos = now;
                render(now);
                if (!paused && elapsedSeconds(now) >= currentBeat().durationSeconds()) {
                    moveBeat(1);
                }
            }
        };
        timer.start();
        canvas.requestFocus();
    }

    private void prepareActors(String narratorSkinKey) {
        narrator = game.createCampaignCutsceneBird(cinematic.narrator(), narratorSkinKey);
        boss = game.createCampaignCutsceneBird(cinematic.defeatedBoss(), cinematic.defeatedBossSkin());
        flock.clear();
        BirdGame3.BirdType[] types = {
                BirdGame3.BirdType.PIGEON,
                BirdGame3.BirdType.TITMOUSE,
                BirdGame3.BirdType.HUMMINGBIRD,
                BirdGame3.BirdType.TURKEY,
                BirdGame3.BirdType.GOOSE
        };
        for (BirdGame3.BirdType type : types) flock.add(game.createCampaignCutsceneBird(type, null));
    }

    private Button controlButton(String label, Runnable action) {
        Button button = new Button(label);
        button.setFont(Font.font("Arial Black", FontWeight.BOLD, 12));
        button.setTextFill(Color.WHITE);
        button.setStyle("-fx-background-color: rgba(10,17,25,0.82);"
                + "-fx-border-color: #FFE082; -fx-border-radius: 8; -fx-background-radius: 8;");
        button.setOnAction(event -> action.run());
        return button;
    }

    private void render(long now) {
        GraphicsContext g = canvas.getGraphicsContext2D();
        double elapsed = elapsedSeconds(now);
        double duration = currentBeat().durationSeconds();
        double progress = Math.clamp(elapsed / duration, 0.0, 1.0);
        g.save();
        g.scale(canvas.getWidth() / LOGICAL_WIDTH, canvas.getHeight() / LOGICAL_HEIGHT);
        boolean continuousPanorama = ClassicEndingContent.isContinuousPanorama(cinematic);
        boolean subglacialMontage = ClassicEndingContent.isSubglacialMontage(cinematic);
        boolean stillwaterRevelation = ClassicEndingContent.isStillwaterRevelation(cinematic);
        boolean charlesLivingScore = ClassicEndingContent.isCharlesLivingScore(cinematic);
        boolean razorbillFinalCut = ClassicEndingContent.isRazorbillFinalCut(cinematic);
        boolean grinchOpenSack = ClassicEndingContent.isGrinchHawkOpenSack(cinematic);
        boolean vultureFinalAccount = ClassicEndingContent.isVultureFinalAccount(cinematic);
        boolean opiumTwelfthFuture = ClassicEndingContent.isOpiumTwelfthFuture(cinematic);
        boolean heisenBlueVault = ClassicEndingContent.isHeisenBlueVault(cinematic);
        if (continuousPanorama) {
            double routeProgress = Math.clamp((beatIndex + progress) / cinematic.beats().size(), 0.0, 1.0);
            drawRoadrunnerPanorama(g, now / 1_000_000_000.0, routeProgress);
        } else if (subglacialMontage) {
            double routeProgress = Math.clamp((beatIndex + progress) / cinematic.beats().size(), 0.0, 1.0);
            drawPenguinSubglacialMontage(g, now / 1_000_000_000.0, routeProgress);
        } else if (stillwaterRevelation) {
            double routeProgress = Math.clamp((beatIndex + progress) / cinematic.beats().size(), 0.0, 1.0);
            drawShoebillStillwaterRevelation(g, now / 1_000_000_000.0, routeProgress);
        } else if (charlesLivingScore) {
            double routeProgress = Math.clamp((beatIndex + progress) / cinematic.beats().size(), 0.0, 1.0);
            drawCharlesLivingScore(g, now / 1_000_000_000.0, routeProgress);
        } else if (razorbillFinalCut) {
            double routeProgress = Math.clamp((beatIndex + progress) / cinematic.beats().size(), 0.0, 1.0);
            drawRazorbillFinalCut(g, now / 1_000_000_000.0, routeProgress);
        } else if (grinchOpenSack) {
            double routeProgress = Math.clamp((beatIndex + progress) / cinematic.beats().size(), 0.0, 1.0);
            drawGrinchOpenSack(g, now / 1_000_000_000.0, routeProgress);
        } else if (vultureFinalAccount) {
            double routeProgress = Math.clamp((beatIndex + progress) / cinematic.beats().size(), 0.0, 1.0);
            drawVultureFinalAccount(g, now / 1_000_000_000.0, routeProgress);
        } else if (opiumTwelfthFuture) {
            double routeProgress = Math.clamp((beatIndex + progress) / cinematic.beats().size(), 0.0, 1.0);
            drawOpiumTwelfthFuture(g, now / 1_000_000_000.0, routeProgress);
        } else if (heisenBlueVault) {
            double routeProgress = Math.clamp((beatIndex + progress) / cinematic.beats().size(), 0.0, 1.0);
            drawHeisenBlueVault(g, now / 1_000_000_000.0, routeProgress);
        } else {
            drawBackground(g, now / 1_000_000_000.0, progress);
            drawTableau(g, currentBeat().tableau(), progress, now / 1_000_000_000.0);
        }
        drawCinematicFrame(g, progress);
        drawNarration(g, currentBeat().narration());
        drawProgress(g);
        if (!continuousPanorama && !subglacialMontage && !stillwaterRevelation
                && !charlesLivingScore && !razorbillFinalCut && !grinchOpenSack
                && !vultureFinalAccount && !opiumTwelfthFuture && !heisenBlueVault) {
            drawTransition(g, progress);
        }
        g.restore();
    }

    private void drawHeisenBlueVault(GraphicsContext g, double time, double progress) {
        Color top = Color.web("#020815").interpolate(Color.web("#08253A"), progress * 0.72);
        Color bottom = Color.web("#101A2A").interpolate(Color.web("#153E50"), progress * 0.62);
        g.setFill(new LinearGradient(0, 0, 0, LOGICAL_HEIGHT, false, CycleMethod.NO_CYCLE,
                new Stop(0, top), new Stop(1, bottom)));
        g.fillRect(0, 0, LOGICAL_WIDTH, LOGICAL_HEIGHT);

        // A moving storm factory remains one continuous shot: the defeated
        // Engine is recalled, the intact Crown is vaulted, then weather is
        // visibly divided between customers and everyone else.
        double cloudDrift = time * 18.0;
        for (int cloud = 0; cloud < 12; cloud++) {
            double x = Math.floorMod((long) (cloud * 263.0 + cloudDrift), 2_240L) - 160.0;
            double y = 85.0 + (cloud % 4) * 74.0;
            g.setFill(Color.web("#A5D9E8", 0.07 + (cloud % 3) * 0.025));
            g.fillOval(x, y, 300 + cloud % 3 * 70, 82 + cloud % 2 * 24);
        }
        g.setFill(Color.web("#06131C"));
        g.fillRect(0, 760, LOGICAL_WIDTH, 320);
        for (int tower = 0; tower < 13; tower++) {
            double x = tower * 158.0 - 55.0;
            double height = 230 + (tower * 71 % 270);
            g.setFill(Color.web(tower < 7 ? "#0B2D3B" : "#151A24"));
            g.fillRect(x, 760 - height, 112, height);
            for (int row = 0; row < 6; row++) {
                boolean paid = tower < 7 && progress > 0.58;
                g.setFill(Color.web(paid ? "#69E6FF" : "#29343D", paid ? 0.72 : 0.40));
                g.fillRect(x + 22, 785 - height + row * 42, 17, 22);
                g.fillRect(x + 70, 785 - height + row * 42, 17, 22);
            }
        }

        double bossFade = Math.clamp(1.0 - progress * 5.0, 0.0, 1.0);
        if (bossFade > 0.0) {
            g.setStroke(Color.web("#69E6FF", bossFade * 0.76));
            g.setLineWidth(18.0);
            for (int ring = 0; ring < 4; ring++) {
                double radius = 100 + ring * 58 + time * 14 % 48;
                g.strokeOval(1_420 - radius, 405 - radius * 0.72, radius * 2, radius * 1.44);
            }
            drawBird(g, boss, 1_420, 560 + progress * 85.0, 1.02, false, bossFade);
            drawBossName(g, "THE BLUE SKY ENGINE", 1_420, 730, bossFade);
        }

        double crownRise = ease(Math.clamp((progress - 0.10) / 0.20, 0.0, 1.0));
        double vaultClose = ease(Math.clamp((progress - 0.34) / 0.20, 0.0, 1.0));
        if (crownRise > 0.0) {
            drawCrown(g, 960, 610 - crownRise * 255.0, 1.18, time, crownRise);
        }

        double vaultX = 960.0;
        double vaultY = 355.0;
        double vaultW = 520.0 * vaultClose;
        double vaultH = 420.0 * vaultClose;
        if (vaultClose > 0.0) {
            g.setFill(Color.web("#06141D", 0.72));
            g.fillRoundRect(vaultX - vaultW / 2.0, vaultY - vaultH / 2.0,
                    vaultW, vaultH, 70, 70);
            g.setStroke(Color.web("#69E6FF", 0.76 + Math.sin(time * 2.0) * 0.12));
            g.setLineWidth(18.0);
            g.strokeRoundRect(vaultX - vaultW / 2.0, vaultY - vaultH / 2.0,
                    vaultW, vaultH, 70, 70);
            g.setStroke(Color.web("#F7E46B", 0.82));
            g.setLineWidth(8.0);
            g.strokeOval(vaultX - 48, vaultY + 82, 96, 96);
            g.strokeLine(vaultX, vaultY + 178, vaultX, vaultY + 215);
        }

        double ownerReveal = ease(Math.clamp((progress - 0.30) / 0.27, 0.0, 1.0));
        if (ownerReveal > 0.0) {
            double ownerX = 470.0 - (1.0 - ownerReveal) * 290.0;
            drawBird(g, narrator, ownerX, 625, 1.35, true, ownerReveal);
            g.setFill(Color.web("#69E6FF", ownerReveal * 0.80));
            g.setFont(Font.font("Consolas", FontWeight.BOLD, 28));
            g.setTextAlign(TextAlignment.CENTER);
            g.fillText("SOLE PROPRIETOR", ownerX, 765);
        }

        double contractReveal = ease(Math.clamp((progress - 0.57) / 0.19, 0.0, 1.0));
        if (contractReveal > 0.0) {
            g.setFill(Color.web("#071018", 0.90 * contractReveal));
            g.fillRoundRect(1_260, 160, 520, 185, 26, 26);
            g.setStroke(Color.web("#F7E46B", contractReveal));
            g.setLineWidth(6.0);
            g.strokeRoundRect(1_260, 160, 520, 185, 26, 26);
            g.setFill(Color.WHITE.deriveColor(0, 1, 1, contractReveal));
            g.setFont(Font.font("Arial Black", 27));
            g.setTextAlign(TextAlignment.CENTER);
            g.fillText("BLUE SKY WEATHER LICENSE", 1_520, 218);
            g.setFill(Color.web("#69E6FF", contractReveal));
            g.setFont(Font.font("Consolas", FontWeight.BOLD, 21));
            g.fillText("RAIN  •  WIND  •  SUNLIGHT", 1_520, 270);
            g.fillText("PAYMENT REQUIRED", 1_520, 312);
        }

        if (progress > 0.70) {
            double storm = Math.clamp((progress - 0.70) / 0.20, 0.0, 1.0);
            g.setStroke(Color.web("#D7F6FF", 0.55 * storm));
            g.setLineWidth(4.0);
            for (int rain = 0; rain < 55; rain++) {
                double x = 1_050 + Math.floorMod(rain * 97, 850);
                double y = 370 + Math.floorMod((long) (rain * 61 + time * 260), 560L);
                g.strokeLine(x, y, x - 18, y + 42);
            }
            g.setFill(Color.web("#F7E46B", 0.86 * storm));
            g.setFont(Font.font("Arial Black", 36));
            g.setTextAlign(TextAlignment.CENTER);
            g.fillText("PAID", 500, 890);
            g.setFill(Color.web("#FF315E", 0.86 * storm));
            g.fillText("PAST DUE", 1_475, 890);
        }
        g.setTextAlign(TextAlignment.LEFT);
    }

    private void drawOpiumTwelfthFuture(GraphicsContext g, double time, double progress) {
        Color top = Color.web("#050515").interpolate(Color.web("#22113A"), progress);
        Color bottom = Color.web("#251C4A").interpolate(Color.web("#725182"), progress * 0.82);
        g.setFill(new LinearGradient(0, 0, 0, LOGICAL_HEIGHT, false, CycleMethod.NO_CYCLE,
                new Stop(0, top), new Stop(1, bottom)));
        g.fillRect(0, 0, LOGICAL_WIDTH, LOGICAL_HEIGHT);

        // The ending is a continuous orbit through the rebuilt observatory.
        // Twelve lenses remain visible throughout, so the blank final lens is
        // a visual decision rather than another paragraph of exposition.
        for (int star = 0; star < 110; star++) {
            double x = Math.floorMod(star * 337 + 91, 1_920);
            double y = 90 + Math.floorMod(star * 181 + 43, 690);
            double twinkle = 0.30 + 0.25 * Math.sin(time * 1.4 + star);
            g.setFill(Color.WHITE.deriveColor(0, 1, 1, twinkle));
            g.fillOval(x, y, 2 + star % 4, 2 + star % 4);
        }

        double drift = progress * 260.0;
        g.setFill(Color.web("#09091C", 0.96));
        for (int dome = 0; dome < 7; dome++) {
            double x = -160 + dome * 345.0 - drift * (0.12 + dome * 0.006);
            double y = 690 - (dome % 3) * 62.0;
            g.fillRect(x, y, 280, 390);
            g.fillOval(x - 40, y - 90, 360, 180);
            g.setStroke(Color.web("#9A84C7", 0.34));
            g.setLineWidth(7.0);
            g.strokeOval(x - 40, y - 90, 360, 180);
        }

        double bossFade = Math.clamp(1.0 - progress * 4.8, 0.0, 1.0);
        if (bossFade > 0.0) {
            // A locked sundial closes around the defeated Still King.
            g.setStroke(Color.web("#FFD166", bossFade * 0.86));
            g.setLineWidth(16.0);
            g.strokeOval(1_210, 235, 430, 430);
            for (int ray = 0; ray < 12; ray++) {
                double angle = ray * Math.PI / 6.0;
                g.strokeLine(1_425 + Math.cos(angle) * 220, 450 + Math.sin(angle) * 220,
                        1_425 + Math.cos(angle) * 265, 450 + Math.sin(angle) * 265);
            }
            g.strokeLine(1_425, 450, 1_545, 318);
            drawBird(g, boss, 1_425, 590 + progress * 75.0, 1.05, false, bossFade);
            drawBossName(g, "THE STILL KING", 1_425, 760, bossFade);
        }

        double crownRise = ease(Math.clamp((progress - 0.10) / 0.20, 0.0, 1.0));
        double crownBreak = ease(Math.clamp((progress - 0.42) / 0.20, 0.0, 1.0));
        if (crownRise > 0.0 && crownBreak < 1.0) {
            drawCrown(g, 1_060, 620 - crownRise * 270.0, 1.12, time,
                    crownRise * (1.0 - crownBreak));
        }

        double lensReveal = ease(Math.clamp((progress - 0.27) / 0.48, 0.0, 1.0));
        double orbitRotation = time * 0.035 + progress * 0.75;
        for (int lens = 0; lens < 12; lens++) {
            double reveal = Math.clamp((lensReveal * 12.0 - lens) / 2.0, 0.0, 1.0);
            if (reveal <= 0.0) continue;
            double angle = lens * Math.PI / 6.0 - Math.PI / 2.0 + orbitRotation;
            double radiusX = 450.0 + (lens % 2) * 95.0;
            double radiusY = 250.0 + (lens % 3) * 32.0;
            double x = 1_030 + Math.cos(angle) * radiusX;
            double y = 455 + Math.sin(angle) * radiusY;
            boolean blank = lens == 11;
            Color color = blank ? Color.web("#100B20") : Color.hsb(lens * 29.0 + 178.0, 0.44, 1.0);
            g.setFill(color.deriveColor(0, 1, 1, blank ? 0.92 * reveal : 0.24 * reveal));
            g.fillOval(x - 48, y - 48, 96, 96);
            g.setStroke(blank ? Color.web("#FFD166", reveal) : color.deriveColor(0, 0.9, 1, reveal));
            g.setLineWidth(blank ? 9.0 : 6.0);
            g.strokeOval(x - 48, y - 48, 96, 96);
            if (!blank && progress > 0.48) {
                g.setStroke(Color.WHITE.deriveColor(0, 1, 1, 0.45 * reveal));
                g.setLineWidth(3.0);
                g.strokeLine(x - 24, y + 15, x, y - 24);
                g.strokeLine(x, y - 24, x + 24, y + 15);
            } else if (blank) {
                g.setFill(Color.web("#FFD166", reveal));
                g.fillOval(x - 6, y - 6, 12, 12);
            }
        }

        // Eleven public telescope towers receive a lens. The twelfth pedestal
        // is intentionally open, preserving a future no prophecy can occupy.
        double publicReveal = ease(Math.clamp((progress - 0.56) / 0.28, 0.0, 1.0));
        for (int tower = 0; tower < 12; tower++) {
            double x = 95 + tower * 157.0;
            double y = 790 - (tower % 2) * 38.0;
            boolean blank = tower == 11;
            g.setStroke(Color.web(blank ? "#FFD166" : "#80DEEA", publicReveal * (blank ? 0.88 : 0.52)));
            g.setLineWidth(blank ? 7.0 : 4.0);
            g.strokeLine(x, y, x, 902);
            g.strokeLine(x - 34, 902, x + 34, 902);
            if (!blank) {
                g.strokeOval(x - 23, y - 23, 46, 46);
            } else {
                g.setLineDashes(10.0, 9.0);
                g.strokeOval(x - 28, y - 28, 56, 56);
                g.setLineDashes();
            }
        }

        double narratorX = 390 + ease(progress) * 560.0;
        double narratorY = 650 - Math.sin(progress * Math.PI) * 145.0
                - Math.sin(time * 1.3) * 7.0;
        drawBird(g, narrator, narratorX, narratorY, 1.12 + progress * 0.18, true, 1.0);
        if (progress > 0.84) {
            double finale = ease((progress - 0.84) / 0.16);
            g.setFill(Color.web("#E1BEE7", 0.09 * finale));
            g.fillOval(520, 80, 1_020, 840);
            drawFinalTitle(g, finale);
        }
    }

    private void drawGrinchOpenSack(GraphicsContext g, double time, double progress) {
        g.setFill(new LinearGradient(0, 0, 0, LOGICAL_HEIGHT, false, CycleMethod.NO_CYCLE,
                new Stop(0, Color.web("#050713")), new Stop(1, Color.web("#321238"))));
        g.fillRect(0, 0, LOGICAL_WIDTH, LOGICAL_HEIGHT);
        double moonGlow = 0.18 + progress * 0.22;
        g.setFill(Color.web("#FFF3C4", moonGlow));
        g.fillOval(690, 70, 540, 540);

        // A continuous dolly through the Bellkeeper's warehouse. The vaults
        // unlock outward as the Crown is remade, instead of cutting between
        // static dialogue poses.
        double drift = progress * 680.0;
        for (int vault = 0; vault < 6; vault++) {
            double x = 70 + vault * 370.0 - drift * 0.28;
            double y = 280 + (vault % 2) * 120.0;
            double open = Math.clamp((progress - 0.38 - vault * 0.035) * 5.0, 0.0, 1.0);
            g.setFill(Color.web("#171C28"));
            g.fillRoundRect(x, y, 280, 430, 28, 28);
            g.setStroke(Color.web("#A57B3F", 0.78));
            g.setLineWidth(12);
            g.strokeRoundRect(x, y, 280, 430, 28, 28);
            g.setFill(Color.web("#090A10"));
            g.fillRect(x + 140 - open * 145, y + 12, 140, 406);
            g.fillRect(x + 140, y + 12, 140 + open * 145, 406);
            if (open > 0.0) {
                g.setFill(Color.web("#FFE082", 0.18 + open * 0.36));
                g.fillRect(x + 22, y + 28, 236, 372);
            }
        }

        double defeated = Math.clamp(1.0 - progress * 4.0, 0.0, 1.0);
        if (defeated > 0.0) {
            drawBird(g, boss, 1_390 + progress * 120, 650 + progress * 90, 1.22, false, defeated * 0.55);
            drawBossName(g, "THE BELLKEEPER", 1_390, 810, defeated);
        }

        double crownAlpha = Math.clamp((progress - 0.14) * 4.5, 0.0, 1.0)
                * Math.clamp((0.68 - progress) * 5.0, 0.0, 1.0);
        if (crownAlpha > 0.0) {
            g.setFill(Color.web("#FFD54F", crownAlpha));
            g.fillPolygon(new double[]{840, 900, 960, 1_020, 1_080, 1_045, 875},
                    new double[]{470, 360, 455, 345, 470, 560, 560}, 7);
        }

        double sackReveal = Math.clamp((progress - 0.42) * 4.0, 0.0, 1.0);
        if (sackReveal > 0.0) {
            double bob = Math.sin(time * 1.8) * 8.0;
            g.setFill(Color.web("#5D173F", sackReveal));
            g.fillOval(770, 475 + bob, 380, 310);
            g.setStroke(Color.web("#FFD166", sackReveal));
            g.setLineWidth(16);
            g.strokeArc(780, 410 + bob, 360, 160, 200, 140, javafx.scene.shape.ArcType.OPEN);
            g.strokeLine(820, 515 + bob, 1_100, 515 + bob);
            for (int item = 0; item < 18; item++) {
                double release = Math.clamp((progress - 0.54 - item * 0.008) * 5.0, 0.0, 1.0);
                double angle = -2.7 + item * 0.31;
                double distance = release * (270 + Math.floorMod(item * 67, 420));
                double x = 960 + Math.cos(angle) * distance;
                double y = 525 + bob + Math.sin(angle) * distance - release * 120;
                Color itemColor = switch (item % 4) {
                    case 0 -> Color.web("#81C784");
                    case 1 -> Color.web("#90CAF9");
                    case 2 -> Color.web("#FFCC80");
                    default -> Color.web("#EF9A9A");
                };
                g.setFill(itemColor.deriveColor(0, 1, 1, sackReveal));
                g.fillRoundRect(x - 20, y - 15, 40, 30, 7, 7);
            }
        }

        drawBird(g, narrator, 520 + progress * 330, 680 - Math.sin(time * 1.5) * 10,
                1.18 + progress * 0.14, true, 1.0);
        if (progress > 0.82) drawFinalTitle(g, Math.clamp((progress - 0.82) * 5.5, 0.0, 1.0));
    }

    private void drawCharlesLivingScore(GraphicsContext g, double time, double progress) {
        Color top = Color.web("#05030A").interpolate(Color.web("#32152C"), progress * 0.72);
        Color bottom = Color.web("#180916").interpolate(Color.web("#77502B"), progress);
        g.setFill(new LinearGradient(0, 0, 0, LOGICAL_HEIGHT, false, CycleMethod.NO_CYCLE,
                new Stop(0, top), new Stop(1, bottom)));
        g.fillRect(0, 0, LOGICAL_WIDTH, LOGICAL_HEIGHT);

        // One continuous camera move through the rebuilt Resonance Hall.
        double drift = progress * 420.0;
        g.setFill(Color.web("#160A16", 0.96));
        g.fillRect(-drift * 0.12, 620, LOGICAL_WIDTH + 200, 460);
        g.setStroke(Color.web("#D8A854", 0.68));
        g.setLineWidth(10.0);
        for (int tier = 0; tier < 3; tier++) {
            double y = 170 + tier * 175.0;
            for (int side = 0; side < 2; side++) {
                double x = side == 0 ? 48 - drift * 0.18 : 1_520 + drift * 0.18;
                g.strokeRoundRect(x, y, 350, 130, 26, 26);
            }
        }

        // The original Maestro is a mask, tuning forks, batons, and score
        // ribbons—not a transformed bird. It breaks apart before the Crown rises.
        double maestroAlpha = Math.clamp(1.0 - progress * 4.2, 0.0, 1.0);
        if (maestroAlpha > 0.0) {
            g.save();
            g.setGlobalAlpha(maestroAlpha);
            drawHollowMaestroMask(g, 1_340 + progress * 180.0, 470 + progress * 95.0, 1.05, time);
            g.restore();
        }

        double crownRise = ease(Math.clamp((progress - 0.12) / 0.22, 0.0, 1.0));
        double crownBreak = ease(Math.clamp((progress - 0.50) / 0.20, 0.0, 1.0));
        if (crownRise > 0.0 && crownBreak < 1.0) {
            drawCrown(g, 1_070, 650 - crownRise * 310.0, 1.22, time,
                    Math.clamp(1.0 - crownBreak, 0.0, 1.0));
        }

        double scoreReveal = ease(Math.clamp((progress - 0.30) / 0.42, 0.0, 1.0));
        if (scoreReveal > 0.0) {
            g.setStroke(Color.web("#FFE082", 0.60 * scoreReveal));
            g.setLineWidth(5.0);
            for (int staff = 0; staff < 5; staff++) {
                double y = 260 + staff * 48.0 + Math.sin(time * 0.7 + staff) * 8.0;
                g.strokeLine(70, y, 1_850, y + Math.sin(staff) * 20.0);
            }
            for (int note = 0; note < 24; note++) {
                double angle = note * Math.PI * 2.0 / 24.0 + time * 0.06;
                double distance = 70 + crownBreak * (250 + (note % 5) * 54.0);
                double x = 1_070 + Math.cos(angle) * distance;
                double y = 405 + Math.sin(angle) * distance * 0.52;
                Color noteColor = Color.hsb(note * 15.0, 0.62, 1.0, 0.88 * scoreReveal);
                g.setFill(noteColor);
                g.fillOval(x - 12, y - 9, 24, 18);
                g.fillRect(x + 8, y - 52, 5, 48);
            }
        }

        double flockReveal = ease(Math.clamp((progress - 0.58) / 0.24, 0.0, 1.0));
        for (int i = 0; i < flock.size(); i++) {
            double x = 250 + i * 320.0;
            double y = 750 - (i % 2) * 80.0 - Math.sin(time + i) * 10.0;
            drawBird(g, flock.get(i), x, y, 0.58, i % 2 == 0, flockReveal);
            if (flockReveal > 0.0) {
                g.setStroke(Color.hsb(i * 63.0, 0.50, 1.0, 0.45 * flockReveal));
                g.setLineWidth(5.0);
                g.strokeArc(x - 65, y - 100, 170, 90, 18, 144, javafx.scene.shape.ArcType.OPEN);
            }
        }

        double charlesX = 390 + ease(progress) * 430.0;
        double charlesY = 650 - Math.sin(progress * Math.PI) * 74.0;
        drawBird(g, narrator, charlesX, charlesY, 1.16 + progress * 0.18, true, 1.0);

        if (progress > 0.84) {
            double finale = ease((progress - 0.84) / 0.16);
            g.setFill(Color.web("#FFF3D0", 0.10 * finale));
            g.fillOval(520, 120, 900, 780);
            drawFinalTitle(g, finale);
        }
    }

    private void drawVultureFinalAccount(GraphicsContext g, double time, double progress) {
        g.setFill(new LinearGradient(0, 0, 0, LOGICAL_HEIGHT, false, CycleMethod.NO_CYCLE,
                new Stop(0, Color.web("#070910")), new Stop(0.58, Color.web("#251B1C")),
                new Stop(1, Color.web("#08090D"))));
        g.fillRect(0, 0, LOGICAL_WIDTH, LOGICAL_HEIGHT);

        // One uninterrupted crane shot moves from the ruined Engine, through
        // the liberation, and finally into Vulture's private ledger room.
        double cameraDrift = progress * 860.0;
        g.save();
        g.translate(-cameraDrift * 0.18, 0.0);
        g.setFill(Color.web("#03050A", 0.96));
        for (int tower = 0; tower < 12; tower++) {
            double x = -120 + tower * 230.0;
            double h = 250 + Math.floorMod(tower * 191, 480);
            g.fillRect(x, 850 - h, 170 + (tower % 3) * 30, h + 260);
        }
        g.setStroke(Color.web("#735C47", 0.78));
        g.setLineWidth(18.0);
        g.strokeLine(20.0, 230.0, 2_350.0, 230.0);
        for (double x = 80.0; x < 2_300.0; x += 240.0) {
            g.strokeLine(x, 230.0, x + 110.0, 300.0);
            g.strokeLine(x + 110.0, 230.0, x, 300.0);
        }
        g.restore();

        // The defeated Debt Engine cools and physically breaks apart during
        // the first beat instead of being represented by an ordinary bird.
        double engineFade = Math.clamp(1.0 - progress * 4.2, 0.0, 1.0);
        if (engineFade > 0.0) {
            double cx = 1_390.0 + progress * 80.0;
            double cy = 580.0 + progress * 150.0;
            g.setFill(Color.web("#151820", engineFade));
            g.fillOval(cx - 205.0, cy - 170.0, 410.0, 340.0);
            g.setStroke(Color.web("#D6A84A", engineFade * 0.85));
            g.setLineWidth(18.0);
            g.strokeOval(cx - 205.0, cy - 170.0, 410.0, 340.0);
            for (int tooth = 0; tooth < 14; tooth++) {
                double a = tooth * Math.PI * 2.0 / 14.0 + progress * 2.0;
                double scatter = progress * 130.0;
                double x1 = cx + Math.cos(a) * (170.0 + scatter);
                double y1 = cy + Math.sin(a) * (140.0 + scatter * 0.6);
                g.strokeLine(x1, y1, x1 + Math.cos(a) * 48.0, y1 + Math.sin(a) * 48.0);
            }
            g.setFill(Color.web("#FF6B35", engineFade * 0.72));
            g.fillOval(cx - 58.0, cy - 58.0, 116.0, 116.0);
            drawBossName(g, "THE DEBT ENGINE", cx, cy + 250.0, engineFade);
        }

        double crownReveal = Math.clamp((progress - 0.12) * 5.0, 0.0, 1.0)
                * Math.clamp((0.55 - progress) * 5.0, 0.0, 1.0);
        if (crownReveal > 0.0) {
            double crownX = 960.0;
            double crownY = 430.0 - Math.sin(time * 1.7) * 8.0;
            g.setFill(Color.web("#FFD54F", crownReveal));
            g.fillPolygon(new double[]{crownX - 140, crownX - 82, crownX - 25, crownX + 35,
                            crownX + 95, crownX + 145, crownX + 112, crownX - 110},
                    new double[]{crownY + 100, crownY - 35, crownY + 75, crownY - 52,
                            crownY + 72, crownY - 25, crownY + 150, crownY + 150}, 8);
        }

        // The Crown fractures into literal keys which immediately travel to
        // cages and archives; the effect is readable without dialogue boxes.
        double keyPhase = Math.clamp((progress - 0.35) * 3.6, 0.0, 1.0);
        for (int key = 0; key < 18; key++) {
            double release = Math.clamp(keyPhase * 1.45 - key * 0.025, 0.0, 1.0);
            if (release <= 0.0) continue;
            double angle = -2.8 + key * 0.33;
            double distance = release * (210.0 + Math.floorMod(key * 91, 470));
            double x = 960.0 + Math.cos(angle) * distance;
            double y = 455.0 + Math.sin(angle) * distance - release * 120.0;
            g.setStroke(Color.web("#FFD166", release));
            g.setLineWidth(7.0);
            g.strokeOval(x - 18.0, y - 18.0, 36.0, 36.0);
            g.strokeLine(x + 18.0, y, x + 62.0, y);
            g.strokeLine(x + 46.0, y, x + 46.0, y + 18.0);
        }

        double liberation = Math.clamp((progress - 0.48) * 3.6, 0.0, 1.0);
        for (int cage = 0; cage < 5; cage++) {
            double x = 80.0 + cage * 360.0 - cameraDrift * 0.28;
            double y = 600.0 + (cage % 2) * 70.0;
            double open = Math.clamp(liberation * 1.7 - cage * 0.12, 0.0, 1.0);
            g.setStroke(Color.web("#68727D", 0.76));
            g.setLineWidth(9.0);
            g.strokeRoundRect(x, y, 250.0, 310.0, 18.0, 18.0);
            for (int bar = 1; bar < 6; bar++) {
                g.strokeLine(x + bar * 40.0, y + 10.0, x + bar * 40.0 + open * 150.0,
                        y + 300.0 - open * 100.0);
            }
            if (open > 0.2) {
                g.setFill(Color.web("#FF7043", 0.34 * open));
                for (int page = 0; page < 7; page++) {
                    double px = x + 30.0 + page * 28.0;
                    double py = y + 240.0 - open * (70.0 + page * 15.0);
                    g.fillRect(px, py, 24.0, 34.0);
                }
            }
        }

        // Deterministic crow silhouettes carry the keys across the same frame.
        for (int crow = 0; crow < 22; crow++) {
            double flight = Math.clamp((progress - 0.43 - crow * 0.008) * 2.7, 0.0, 1.0);
            double x = 820.0 + (crow % 5) * 65.0 + flight * (660.0 + (crow % 4) * 80.0);
            double y = 520.0 - (crow % 7) * 48.0 + Math.sin(time * 4.0 + crow) * 14.0;
            double wing = 12.0 + 8.0 * Math.sin(time * 7.0 + crow * 0.7);
            g.setFill(Color.web("#050609", 0.92));
            g.fillOval(x - 13.0, y - 8.0, 26.0, 16.0);
            g.fillPolygon(new double[]{x - 5, x - 46, x - 18}, new double[]{y, y - wing, y + 4}, 3);
            g.fillPolygon(new double[]{x + 5, x + 46, x + 18}, new double[]{y, y - wing, y + 4}, 3);
        }

        double finalReveal = Math.clamp((progress - 0.72) * 4.0, 0.0, 1.0);
        if (finalReveal > 0.0) {
            g.setFill(Color.web("#05070B", finalReveal * 0.74));
            g.fillRoundRect(1_045.0, 235.0, 760.0, 690.0, 44.0, 44.0);
            g.setStroke(Color.web("#D6A84A", finalReveal * 0.72));
            g.setLineWidth(8.0);
            g.strokeRoundRect(1_045.0, 235.0, 760.0, 690.0, 44.0, 44.0);
            drawBird(g, narrator, 1_450.0, 550.0, 1.42, true, finalReveal);
            // The retained black ledger is a deliberate final visual beat.
            g.setFill(Color.web("#090A0D", finalReveal));
            g.fillRoundRect(1_520.0, 610.0, 190.0, 245.0, 18.0, 18.0);
            g.setStroke(Color.web("#D6A84A", finalReveal));
            g.setLineWidth(8.0);
            g.strokeRoundRect(1_520.0, 610.0, 190.0, 245.0, 18.0, 18.0);
            g.setFill(Color.web("#D6A84A", finalReveal));
            g.setFont(Font.font("Consolas", FontWeight.BOLD, 26));
            g.setTextAlign(TextAlignment.CENTER);
            g.fillText("BLACK", 1_615.0, 690.0);
            g.fillText("LEDGER", 1_615.0, 726.0);
        }
    }

    private void drawRazorbillFinalCut(GraphicsContext g, double time, double progress) {
        Color skyTop = Color.web("#02030B").interpolate(Color.web("#17112B"), progress);
        Color skyBottom = Color.web("#160A25").interpolate(Color.web("#164052"), progress * 0.82);
        g.setFill(new LinearGradient(0, 0, 0, LOGICAL_HEIGHT, false, CycleMethod.NO_CYCLE,
                new Stop(0, skyTop), new Stop(1, skyBottom)));
        g.fillRect(0, 0, LOGICAL_WIDTH, LOGICAL_HEIGHT);

        double camera = ease(progress) * 520.0;
        g.setFill(Color.web("#0B0D1A"));
        for (int tower = -1; tower < 7; tower++) {
            double x = tower * 390.0 - camera * (0.16 + tower % 2 * 0.04);
            double top = 360.0 + Math.floorMod(tower * 137, 210);
            g.fillRect(x, top, 260.0, LOGICAL_HEIGHT - top);
            g.setFill(Color.web("#69F0E7", 0.13));
            for (double y = top + 55.0; y < LOGICAL_HEIGHT; y += 90.0) {
                g.fillRect(x + 44.0, y, 32.0, 18.0);
                g.fillRect(x + 170.0, y, 32.0, 18.0);
            }
            g.setFill(Color.web("#0B0D1A"));
        }

        double seamClose = ease(Math.clamp((progress - 0.46) / 0.28, 0.0, 1.0));
        double seamWidth = 270.0 * (1.0 - seamClose) + 16.0;
        g.setFill(Color.web("#69F0E7", 0.10 + (1.0 - seamClose) * 0.18));
        g.fillPolygon(new double[]{960 - seamWidth, 960 + seamWidth, 1_070 + seamWidth * 0.4,
                        890 - seamWidth * 0.3},
                new double[]{0, 0, LOGICAL_HEIGHT, LOGICAL_HEIGHT}, 4);
        g.setStroke(Color.web("#EA80FC", 0.72));
        g.setLineWidth(12.0 + (1.0 - seamClose) * 18.0);
        g.strokePolyline(new double[]{960, 1_010, 945, 1_020, 970},
                new double[]{0, 220, 430, 690, LOGICAL_HEIGHT}, 5);

        double bossFade = Math.clamp(1.0 - progress * 4.3, 0.0, 1.0);
        if (bossFade > 0.0) {
            g.save();
            g.setGlobalAlpha(bossFade);
            drawEndingSeamreaver(g, 1_360 + progress * 110.0, 470 + progress * 90.0,
                    1.15, time);
            g.restore();
        }

        double crownRise = ease(Math.clamp((progress - 0.10) / 0.22, 0.0, 1.0));
        double crownSplit = ease(Math.clamp((progress - 0.42) / 0.25, 0.0, 1.0));
        if (crownRise > 0.0 && crownSplit < 1.0) {
            drawCrown(g, 1_055, 650 - crownRise * 330.0, 1.15, time,
                    Math.clamp(1.0 - crownSplit, 0.0, 1.0));
        }
        for (int seal = 0; seal < 7; seal++) {
            double reveal = ease(Math.clamp((crownSplit - seal * 0.07), 0.0, 1.0));
            if (reveal <= 0.0) continue;
            double angle = -Math.PI * 0.86 + seal * Math.PI * 0.285;
            double distance = 80.0 + crownSplit * 340.0;
            double x = 1_055 + Math.cos(angle) * distance;
            double y = 390 + Math.sin(angle) * distance * 0.52;
            Color color = seal % 2 == 0 ? Color.web("#69F0E7") : Color.web("#EA80FC");
            g.setFill(color.deriveColor(0, 1, 1, 0.28 * reveal));
            g.fillOval(x - 42.0, y - 42.0, 84.0, 84.0);
            g.setStroke(color);
            g.setLineWidth(6.0);
            g.strokePolygon(new double[]{x, x + 28, x, x - 28},
                    new double[]{y - 36, y, y + 36, y}, 4);
        }

        double razorbillX = 300.0 + ease(progress) * 520.0;
        double razorbillY = 690.0 - Math.sin(progress * Math.PI) * 82.0;
        drawBird(g, narrator, razorbillX, razorbillY, 1.22 + progress * 0.14, true, 1.0);

        double gates = ease(Math.clamp((progress - 0.66) / 0.20, 0.0, 1.0));
        for (int side : new int[]{-1, 1}) {
            double x = side < 0 ? 250.0 : 1_670.0;
            g.setStroke(Color.web(side < 0 ? "#00E5FF" : "#EA80FC", gates * 0.82));
            g.setLineWidth(12.0);
            g.strokeOval(x - 90.0, 350.0, 180.0, 340.0);
        }
        if (progress > 0.86) drawFinalTitle(g, ease((progress - 0.86) / 0.14));
    }

    private void drawEndingSeamreaver(GraphicsContext g, double x, double y,
                                      double scale, double time) {
        g.save();
        g.translate(x, y);
        g.scale(scale, scale);
        g.rotate(Math.sin(time * 0.7) * 6.0);
        Color accent = Color.web("#EA80FC");
        g.setStroke(accent.deriveColor(0, 1, 1, 0.42));
        g.setLineWidth(11.0);
        for (int ring = 0; ring < 3; ring++) {
            double radius = 125.0 + ring * 38.0 + Math.sin(time * 1.3 + ring) * 7.0;
            g.strokeOval(-radius, -radius, radius * 2.0, radius * 2.0);
        }
        g.setFill(Color.web("#060711"));
        g.fillPolygon(new double[]{-155, -76, -28, 0, 28, 76, 155, 82, 28, 0, -28, -82},
                new double[]{0, -70, -34, -148, -34, -70, 0, 58, 34, 156, 34, 58}, 12);
        g.setStroke(Color.web("#00E5FF"));
        g.setLineWidth(10.0);
        g.strokePolygon(new double[]{-155, -76, -28, 0, 28, 76, 155, 82, 28, 0, -28, -82},
                new double[]{0, -70, -34, -148, -34, -70, 0, 58, 34, 156, 34, 58}, 12);
        g.setFill(Color.web("#E7F4F6"));
        g.fillPolygon(new double[]{-60, 0, 60, 34, 0, -34},
                new double[]{-35, -80, -35, 48, 86, 48}, 6);
        g.setFill(Color.web("#05050B"));
        g.fillOval(-30, -28, 22, 50);
        g.fillOval(8, -28, 22, 50);
        g.restore();
    }

    private void drawHollowMaestroMask(GraphicsContext g, double x, double y, double scale, double time) {
        g.save();
        g.translate(x, y);
        g.scale(scale, scale);
        g.setFill(Color.web("#0A0910"));
        g.fillOval(-150, -190, 300, 360);
        g.setStroke(Color.web("#E7D9BF"));
        g.setLineWidth(16.0);
        g.strokeOval(-150, -190, 300, 360);
        g.setFill(Color.web("#050508"));
        g.fillOval(-92, -62, 58, 94);
        g.fillOval(34, -62, 58, 94);
        g.setStroke(Color.web("#FFE082"));
        g.setLineWidth(9.0);
        g.strokeArc(-82, 40, 164, 82, 200, 140, javafx.scene.shape.ArcType.OPEN);
        g.setStroke(Color.web("#B0BEC5"));
        g.setLineWidth(12.0);
        for (int side : new int[]{-1, 1}) {
            double sway = Math.sin(time * 1.5 + side) * 25.0;
            g.strokeLine(side * 150, -50, side * (280 + sway), -150);
            g.strokeLine(side * 150, 25, side * (300 - sway), 135);
            g.strokeLine(side * 270, -205, side * 270, 190);
        }
        g.restore();
    }

    private void drawShoebillStillwaterRevelation(GraphicsContext g, double time, double progress) {
        Color top = Color.web("#02070D").interpolate(Color.web("#0B2830"), progress * 0.65);
        Color bottom = Color.web("#123D42").interpolate(Color.web("#07161D"), progress);
        g.setFill(new LinearGradient(0, 0, 0, LOGICAL_HEIGHT, false, CycleMethod.NO_CYCLE,
                new Stop(0, top), new Stop(1, bottom)));
        g.fillRect(0, 0, LOGICAL_WIDTH, LOGICAL_HEIGHT);
        drawStars(g, time * 0.35, 55.0);

        g.setFill(Color.web("#EAF5CF", 0.78));
        g.fillOval(1_380, 58, 260, 260);
        g.setFill(Color.web("#07171B", 0.92));
        for (int i = 0; i < 9; i++) {
            double x = -120 + i * 250.0;
            double height = 350 + (i % 3) * 72.0;
            g.fillRect(x + 82, 420 - height, 46, height + 180);
            g.fillOval(x, 270 - height, 220, 180);
        }

        double waterY = 610.0;
        g.setFill(Color.web("#02151D", 0.98));
        g.fillRect(0, waterY, LOGICAL_WIDTH, LOGICAL_HEIGHT - waterY);
        g.setStroke(Color.web("#69BFAE", 0.22));
        g.setLineWidth(3.0);
        for (int row = 0; row < 8; row++) {
            double shift = Math.sin(time * 0.55 + row) * 32.0;
            g.strokeLine(50 + shift, waterY + 32 + row * 44.0,
                    LOGICAL_WIDTH - 50 - shift, waterY + 36 + row * 44.0);
        }

        // The first third is a hall of false Oracle reflections. They peel
        // away one by one while a gold wake identifies the real silhouette.
        double illusionFade = Math.clamp(1.0 - progress * 2.8, 0.0, 1.0);
        if (illusionFade > 0.0) {
            drawBird(g, boss, 1_040, 485, 0.82, false, illusionFade * 0.46);
            drawBird(g, boss, 1_380, 505, 0.82, true, illusionFade * 0.46);
        }
        double bossFade = Math.clamp(1.0 - progress * 3.7, 0.0, 1.0);
        if (bossFade > 0.0) {
            drawBird(g, boss, 1_210, 500, 1.08, false, bossFade);
            g.setStroke(Color.web("#FFE082", 0.74 * bossFade));
            g.setLineWidth(9.0);
            for (int ring = 0; ring < 3; ring++) {
                double r = 88 + ring * 52 + Math.sin(time * 2.0) * 10.0;
                g.strokeOval(1_210 - r, 590 - r * 0.22, r * 2.0, r * 0.44);
            }
        }

        double crownRise = Math.clamp((progress - 0.16) / 0.26, 0.0, 1.0);
        double crownSink = Math.clamp((progress - 0.70) / 0.22, 0.0, 1.0);
        if (crownRise > 0.0 && crownSink < 1.0) {
            double crownY = 610 - crownRise * 285 + crownSink * 420;
            drawCrown(g, 960, crownY, 1.15 - crownSink * 0.28, time,
                    Math.clamp(1.0 - crownSink * 0.82, 0.0, 1.0));
        }

        // One silent revelation crosses the whole world, exposing cages and
        // false crowns without turning into a permanent surveillance field.
        double reveal = Math.clamp((progress - 0.42) / 0.17, 0.0, 1.0)
                * Math.clamp((0.73 - progress) / 0.14, 0.0, 1.0);
        if (reveal > 0.0) {
            double radius = 120 + reveal * 1_650.0;
            g.setStroke(Color.web("#FFF3B0", 0.72 * (1.0 - reveal * 0.35)));
            g.setLineWidth(18.0);
            g.strokeOval(960 - radius, 350 - radius * 0.55, radius * 2.0, radius * 1.1);
            g.setFill(Color.web("#FFE082", 0.16 * (1.0 - reveal)));
            g.fillOval(960 - radius, 350 - radius * 0.55, radius * 2.0, radius * 1.1);
            for (int i = 0; i < 7; i++) {
                double x = 180 + i * 250.0;
                g.setStroke(Color.web(i % 2 == 0 ? "#FF8A80" : "#E1F5FE", 0.65));
                g.setLineWidth(6.0);
                g.strokeRect(x, 395 - (i % 3) * 45.0, 78, 118);
            }
        }

        g.setFill(Color.web("#2B211B", 0.96));
        g.fillPolygon(new double[]{0, 510, 780, 960, 1_140, 1_410, LOGICAL_WIDTH},
                new double[]{645, 610, 700, 600, 700, 610, 645}, 7);
        g.setStroke(Color.web("#5F4936", 0.85));
        g.setLineWidth(26.0);
        g.strokeLine(320, 690, 960, 610);
        g.strokeLine(1_600, 690, 960, 610);

        double shoebillX = 520 + progress * 310.0;
        drawBird(g, narrator, shoebillX, 500, 1.16, true, 1.0);
        if (progress > 0.88) {
            g.setFill(Color.web("#EAF5CF", Math.clamp((progress - 0.88) / 0.12, 0.0, 1.0)));
            g.setFont(Font.font("Arial Black", FontWeight.BOLD, 38));
            g.setTextAlign(TextAlignment.CENTER);
            g.fillText("THE MARSH REMEMBERS. IT DOES NOT COMMAND.", 960, 785);
        }
    }

    private void drawPenguinSubglacialMontage(GraphicsContext g, double time, double progress) {
        Color skyTop = Color.web("#06112C").interpolate(Color.web("#183D65"), progress);
        Color skyBottom = Color.web("#4A8AA3").interpolate(Color.web("#79DCC8"), progress);
        g.setFill(new LinearGradient(0, 0, 0, LOGICAL_HEIGHT, false, CycleMethod.NO_CYCLE,
                new Stop(0, skyTop), new Stop(1, skyBottom)));
        g.fillRect(0, 0, LOGICAL_WIDTH, LOGICAL_HEIGHT);
        drawStars(g, time, progress * 280.0);

        // One unbroken cross-section follows the Crown from the battlefield,
        // beneath the ice, and back out into shelters around the world.
        double iceY = 430.0;
        g.setFill(Color.web("#DDFBFF", 0.94));
        g.fillPolygon(new double[]{0, 260, 520, 850, 1_160, 1_520, LOGICAL_WIDTH},
                new double[]{iceY + 20, iceY - 24, iceY + 12, iceY - 38,
                        iceY + 14, iceY - 18, iceY + 22}, 7);
        g.setFill(new LinearGradient(0, iceY, 0, LOGICAL_HEIGHT, false, CycleMethod.NO_CYCLE,
                new Stop(0, Color.web("#20708C", 0.88)), new Stop(1, Color.web("#031B39", 0.98))));
        g.fillRect(0, iceY + 22, LOGICAL_WIDTH, LOGICAL_HEIGHT - iceY - 22);
        g.setStroke(Color.web("#9DF7FF", 0.28));
        g.setLineWidth(4.0);
        for (int i = 0; i < 10; i++) {
            double wave = Math.sin(time * 0.85 + i) * 24.0;
            g.strokeLine(80 + wave, iceY + 78 + i * 55.0, LOGICAL_WIDTH - 80 - wave, iceY + 84 + i * 55.0);
        }

        double bossFade = Math.clamp(1.0 - progress * 5.2, 0.0, 1.0);
        if (bossFade > 0.0) {
            g.setFill(Color.web("#FFD54F", bossFade * 0.24));
            g.fillOval(1_170, 30, 520, 520);
            drawBird(g, boss, 1_300, 285 + progress * 80.0, 1.18, false, bossFade);
        }

        double dive = ease(Math.clamp((progress - 0.06) / 0.25, 0.0, 1.0));
        double penguinX = 420 + dive * 250.0;
        double penguinY = 310 + dive * 300.0 + Math.sin(time * 2.0) * 7.0;
        drawBird(g, narrator, penguinX, penguinY, 1.03, true, 1.0);

        double crownSink = ease(Math.clamp((progress - 0.05) / 0.30, 0.0, 1.0));
        double crownX = 930.0;
        double crownY = 270.0 + crownSink * 410.0;
        double crownAlpha = Math.clamp(1.0 - Math.max(0.0, progress - 0.38) * 4.5, 0.0, 1.0);
        if (crownAlpha > 0.0) {
            g.setStroke(Color.web("#FFE082", crownAlpha));
            g.setLineWidth(14.0);
            g.strokePolygon(new double[]{crownX - 115, crownX - 62, crownX, crownX + 62, crownX + 115,
                            crownX + 92, crownX - 92},
                    new double[]{crownY + 68, crownY - 45, crownY + 28, crownY - 45, crownY + 68,
                            crownY + 115, crownY + 115}, 7);
        }

        double shatter = ease(Math.clamp((progress - 0.34) / 0.24, 0.0, 1.0));
        if (shatter > 0.0) {
            for (int i = 0; i < 14; i++) {
                double angle = -Math.PI * 0.9 + i * Math.PI * 1.8 / 13.0;
                double distance = 60.0 + shatter * (340.0 + (i % 4) * 80.0);
                double hx = crownX + Math.cos(angle) * distance;
                double hy = 680.0 + Math.sin(angle) * distance * 0.46;
                g.setFill(Color.web(i % 3 == 0 ? "#FFF3B0" : "#80DEEA", 0.86));
                g.fillPolygon(new double[]{hx, hx + 22, hx + 7, hx - 16},
                        new double[]{hy - 19, hy + 3, hy + 25, hy + 5}, 4);
            }
        }

        double shelterReveal = ease(Math.clamp((progress - 0.54) / 0.30, 0.0, 1.0));
        if (shelterReveal > 0.0) {
            double[] shelterX = {230.0, 720.0, 1_210.0, 1_670.0};
            Color[] shelterLight = {Color.web("#80DEEA"), Color.web("#FFE082"),
                    Color.web("#A5D6A7"), Color.web("#FFAB91")};
            for (int i = 0; i < shelterX.length; i++) {
                double x = shelterX[i];
                double rise = (1.0 - shelterReveal) * 210.0;
                g.setFill(Color.web("#10263B", 0.92));
                g.fillRoundRect(x - 150, 690 + rise, 300, 210, 42, 42);
                g.fillPolygon(new double[]{x - 175, x, x + 175},
                        new double[]{710 + rise, 590 + rise, 710 + rise}, 3);
                g.setFill(shelterLight[i].deriveColor(0, 1, 1, 0.82));
                g.fillRoundRect(x - 44, 760 + rise, 88, 140, 42, 42);
                g.setStroke(Color.WHITE.deriveColor(0, 1, 1, 0.44));
                g.setLineWidth(5.0);
                g.strokeRoundRect(x - 150, 690 + rise, 300, 210, 42, 42);
                if (i < flock.size()) {
                    drawBird(g, flock.get(i), x - 36, 805 + rise, 0.48, i % 2 == 0, shelterReveal);
                }
            }
        }

        if (progress > 0.82) {
            double finale = ease((progress - 0.82) / 0.18);
            g.setFill(Color.web("#A7FFEB", 0.16 * finale));
            g.fillPolygon(new double[]{0, 380, 820, 1_210, 1_580, LOGICAL_WIDTH},
                    new double[]{80, 170, 50, 160, 42, 130}, 6);
            drawBird(g, narrator, 900, 525 - Math.sin(time * 1.4) * 12.0, 1.42, true, finale);
            drawFinalTitle(g, finale);
        }
    }

    private void drawRoadrunnerPanorama(GraphicsContext g, double time, double progress) {
        Color skyTop = Color.web("#31123F").interpolate(Color.web("#123A5A"), progress);
        Color skyBottom = Color.web("#F36B3D").interpolate(Color.web("#F8D47B"), progress);
        g.setFill(new LinearGradient(0, 0, 0, LOGICAL_HEIGHT, false, CycleMethod.NO_CYCLE,
                new Stop(0, skyTop), new Stop(1, skyBottom)));
        g.fillRect(0, 0, LOGICAL_WIDTH, LOGICAL_HEIGHT);
        drawStars(g, time, progress * 420.0);

        g.setFill(Color.web("#FFE7A2", 0.72));
        g.fillOval(1_490 - progress * 360, 120 + progress * 90, 360, 360);

        double travel = progress * 8_400.0;
        for (int segment = 0; segment < 7; segment++) {
            double x = 900 + segment * 1_420.0 - travel;
            drawRoadrunnerLandmark(g, segment, x, progress);
        }

        // The Crown is not carried: it melts into the unbroken road beneath
        // the tracking camera and fades after every traveler chooses a turn.
        g.setStroke(new LinearGradient(0, 0, 1, 0, true, CycleMethod.NO_CYCLE,
                new Stop(0, Color.web("#FFB300", 0.04)),
                new Stop(0.42, Color.web("#FFD54F", 0.92)),
                new Stop(1, Color.web("#FFF8D0", 0.62))));
        g.setLineWidth(34);
        g.strokePolyline(
                new double[]{-80, 250, 600, 940, 1_300, 1_650, 2_030},
                new double[]{760, 738, 770, 706, 748, 682, 710}, 7);
        g.setStroke(Color.web("#4B3430", 0.92));
        g.setLineWidth(118);
        g.strokePolyline(
                new double[]{-80, 250, 600, 940, 1_300, 1_650, 2_030},
                new double[]{800, 778, 810, 746, 788, 722, 750}, 7);
        g.setStroke(Color.web("#FFD54F", 0.88));
        g.setLineWidth(11);
        g.strokePolyline(
                new double[]{-80, 250, 600, 940, 1_300, 1_650, 2_030},
                new double[]{800, 778, 810, 746, 788, 722, 750}, 7);

        double bossAlpha = Math.clamp(1.0 - progress * 8.0, 0.0, 1.0);
        if (bossAlpha > 0.0) {
            drawBird(g, boss, 1_230 + progress * 320, 610, 1.18, false, bossAlpha);
            g.setStroke(Color.web("#FFE082", bossAlpha * 0.78));
            g.setLineWidth(12);
            g.strokeOval(1_030 + progress * 320, 315, 400, 400);
        }

        // Roadrunner remains framed in one tracking position while every
        // country moves past; there are no cuts or tableau resets.
        narrator.roadrunnerMomentum = Bird.ROADRUNNER_MOMENTUM_MAX;
        narrator.roadrunnerMomentumFxTimer = 18;
        drawBird(g, narrator, 680, 665 + Math.sin(time * 11.0) * 7.0, 1.0, true, 1.0);
        g.setStroke(Color.web("#FFF3C4", 0.54));
        g.setLineWidth(5);
        for (int i = 0; i < 8; i++) {
            double y = 520 + i * 30 + Math.sin(time * 2.0 + i) * 8;
            g.strokeLine(185 - i * 18, y, 520 - i * 12, y - 24);
        }

        if (progress > 0.82) {
            double reveal = ease((progress - 0.82) / 0.18);
            g.setStroke(Color.web("#D7DCE2", 0.75 * reveal));
            g.setLineWidth(14);
            g.strokeOval(1_470, 300, 250, 250);
            g.strokeLine(1_595, 425, 1_690, 330);
            g.setStroke(Color.web("#FF5252", reveal));
            g.setLineWidth(12);
            g.strokeLine(1_630, 385, 1_730, 485);
            g.strokeLine(1_730, 385, 1_630, 485);
            drawFinalTitle(g, reveal);
        }
    }

    private void drawRoadrunnerLandmark(GraphicsContext g, int segment, double x, double progress) {
        if (x < -900 || x > LOGICAL_WIDTH + 900) return;
        switch (segment) {
            case 0 -> {
                g.setFill(Color.web("#8C4439"));
                g.fillPolygon(new double[]{x - 360, x - 210, x + 220, x + 390},
                        new double[]{820, 330, 330, 820}, 4);
            }
            case 1 -> {
                g.setFill(Color.web("#12172B"));
                for (int i = 0; i < 6; i++) {
                    double h = 280 + (i % 3) * 110;
                    g.fillRect(x - 360 + i * 125, 820 - h, 92, h);
                    g.setFill(Color.web("#52E1E8", 0.55));
                    g.fillRect(x - 342 + i * 125, 580, 48, 8);
                    g.setFill(Color.web("#12172B"));
                }
            }
            case 2 -> {
                g.setFill(Color.web("#374D58"));
                g.fillPolygon(new double[]{x - 430, x - 140, x + 10, x + 250, x + 430},
                        new double[]{820, 360, 690, 290, 820}, 5);
            }
            case 3 -> {
                g.setFill(Color.web("#184531"));
                for (int i = 0; i < 7; i++) {
                    g.fillRect(x - 390 + i * 125, 390, 28, 430);
                    g.fillOval(x - 445 + i * 125, 300, 140, 150);
                }
            }
            case 4 -> {
                g.setStroke(Color.web("#8B97A5"));
                g.setLineWidth(16);
                for (int i = 0; i < 4; i++) {
                    double cageX = x - 300 + i * 190;
                    g.strokeRect(cageX, 420, 125, 280);
                    if (progress > 0.54) g.strokeLine(cageX + 125, 420, cageX + 215, 350);
                }
            }
            case 5 -> {
                g.setFill(Color.web("#40204F"));
                g.fillPolygon(new double[]{x - 420, x, x + 420}, new double[]{820, 220, 820}, 3);
                g.setFill(Color.web("#FFE082", 0.72));
                g.fillOval(x - 75, 260, 150, 150);
            }
            default -> {
                g.setStroke(Color.web("#FFF3C4", 0.38));
                g.setLineWidth(8);
                g.strokeLine(x - 500, 650, x + 600, 600);
            }
        }
    }

    private void drawBackground(GraphicsContext g, double time, double progress) {
        Color top;
        Color bottom;
        switch (cinematic.location()) {
            case CITY -> { top = Color.web("#071528"); bottom = Color.web("#44215D"); }
            case SKYCLIFFS -> { top = Color.web("#101F42"); bottom = Color.web("#668EC4"); }
            case FROSTBITE_FJORD -> { top = Color.web("#06142C"); bottom = Color.web("#6FAEC9"); }
            case VIBRANT_JUNGLE, FOREST -> { top = Color.web("#061F23"); bottom = Color.web("#356A40"); }
            case BEACON_CROWN -> { top = Color.web("#02040D"); bottom = Color.web("#32133F"); }
            default -> { top = Color.web("#101D2C"); bottom = Color.web("#485F70"); }
        }
        g.setFill(new LinearGradient(0, 0, 0, LOGICAL_HEIGHT, false, CycleMethod.NO_CYCLE,
                new Stop(0, top), new Stop(1, bottom)));
        g.fillRect(0, 0, LOGICAL_WIDTH, LOGICAL_HEIGHT);

        double drift = (beatIndex * 54.0 + progress * 70.0) % 240.0;
        drawStars(g, time, drift);
        switch (cinematic.location()) {
            case CITY -> drawCity(g, drift);
            case SKYCLIFFS -> drawCliffs(g, drift);
            case FROSTBITE_FJORD -> drawFjord(g, drift);
            case VIBRANT_JUNGLE, FOREST -> drawForest(g, drift);
            case BEACON_CROWN -> drawCrownRuins(g, drift);
            default -> drawCliffs(g, drift);
        }
    }

    private void drawStars(GraphicsContext g, double time, double drift) {
        g.setFill(Color.web("#DDEBFF", 0.56));
        for (int i = 0; i < 44; i++) {
            double x = Math.floorMod(i * 173, 1940) - drift * 0.12;
            double y = 62 + Math.floorMod(i * 97, 520);
            double r = 1.2 + (i % 3) * 0.7 + Math.sin(time * 1.4 + i) * 0.25;
            g.fillOval(x, y, r, r);
        }
    }

    private void drawCity(GraphicsContext g, double drift) {
        for (int i = -1; i < 11; i++) {
            double x = i * 220.0 - drift * 0.45;
            double h = 280 + Math.floorMod(i * 83, 300);
            g.setFill(Color.web(i % 2 == 0 ? "#070A17" : "#0B1021", 0.94));
            g.fillRect(x, 820 - h, 174, h + 180);
            g.setFill(Color.web("#D65AAE", 0.48));
            for (int row = 0; row < 6; row++) {
                g.fillRect(x + 30, 850 - h + row * 62, 18, 28);
                g.fillRect(x + 105, 850 - h + row * 62, 18, 28);
            }
        }
    }

    private void drawCliffs(GraphicsContext g, double drift) {
        g.setFill(Color.web("#172541", 0.88));
        for (int i = -1; i < 7; i++) {
            double x = i * 360.0 - drift * 0.3;
            g.fillPolygon(new double[]{x, x + 190, x + 360}, new double[]{850, 300 + (i % 3) * 90, 850}, 3);
        }
        g.setFill(Color.web("#0B1225"));
        g.fillRect(0, 820, LOGICAL_WIDTH, 260);
    }

    private void drawFjord(GraphicsContext g, double drift) {
        g.setFill(Color.web("#D8F4FF", 0.56));
        for (int i = -1; i < 9; i++) {
            double x = i * 260.0 - drift * 0.3;
            g.fillPolygon(new double[]{x, x + 125, x + 250}, new double[]{830, 390 + (i % 2) * 120, 830}, 3);
        }
        g.setFill(Color.web("#0B3150", 0.86));
        g.fillRect(0, 820, LOGICAL_WIDTH, 260);
        g.setStroke(Color.web("#B3E5FC", 0.5));
        g.setLineWidth(4);
        for (int i = 0; i < 10; i++) g.strokeLine(i * 230 - drift, 830, i * 230 + 120 - drift, 1080);
    }

    private void drawForest(GraphicsContext g, double drift) {
        for (int i = -1; i < 14; i++) {
            double x = i * 165.0 - drift * 0.4;
            g.setFill(Color.web("#092C25", 0.94));
            g.fillRect(x + 65, 430, 38, 480);
            g.setFill(Color.web(i % 2 == 0 ? "#0D4936" : "#15583D", 0.94));
            g.fillOval(x, 330 + (i % 3) * 28, 170, 180);
        }
        g.setFill(Color.web("#061910"));
        g.fillRect(0, 835, LOGICAL_WIDTH, 245);
    }

    private void drawCrownRuins(GraphicsContext g, double drift) {
        g.setStroke(Color.web("#B56DDA", 0.25));
        g.setLineWidth(5);
        for (int i = -1; i < 9; i++) {
            double x = i * 280.0 - drift * 0.35;
            g.strokeLine(x, 810, x + 150, 500 + (i % 3) * 90);
            g.strokeLine(x + 150, 500 + (i % 3) * 90, x + 270, 810);
        }
        g.setFill(Color.web("#090512"));
        g.fillRect(0, 820, LOGICAL_WIDTH, 260);
    }

    private void drawTableau(GraphicsContext g, ClassicEndingContent.Tableau tableau,
                             double progress, double time) {
        double eased = ease(progress);
        switch (tableau) {
            case BOSS_AFTERMATH -> {
                drawBird(g, boss, 1320 + eased * 120, 690 + eased * 105, 1.04, false, 1.0 - eased * 0.58);
                drawBird(g, narrator, 250 + eased * 320, 690 - Math.sin(progress * Math.PI) * 45,
                        1.0, true, eased);
                drawDebris(g, time, eased);
                drawBossName(g, cinematic.defeatedBossName(), 1320, 760, 1.0 - eased * 0.45);
            }
            case CROWN_DISCOVERY -> {
                drawBird(g, narrator, 450 + eased * 145, 690, 1.03, true, 1.0);
                drawCrown(g, 1170, 535 - eased * 120, 1.0 + eased * 0.30, time, 1.0);
                drawLightColumn(g, 1170, 525, eased);
            }
            case DECISION -> {
                double orbitX = Math.cos(progress * Math.PI * 1.2) * 110;
                drawCrown(g, 960, 470, 1.28, time, 0.96);
                drawBird(g, narrator, 960 + orbitX, 690 - Math.sin(progress * Math.PI) * 60,
                        1.30, orbitX < 0, 1.0);
                drawDecisionSplit(g, eased);
            }
            case CROWN_TRANSFORMATION -> {
                drawBird(g, narrator, 420 + eased * 250, 690 - Math.sin(progress * Math.PI) * 85,
                        1.12, true, 1.0);
                drawCrown(g, 1080, 500, 1.30 - eased * 0.45, time * 1.6, 1.0 - eased * 0.72);
                drawTransformation(g, eased, time);
            }
            case CHANGED_WORLD -> {
                drawOutcomeMotif(g, eased, time, false);
                for (int i = 0; i < flock.size(); i++) {
                    double x = 210 + i * 330 + Math.sin(time * 0.8 + i) * 16;
                    double y = 735 - (i % 2) * 95 - Math.sin(time * 1.2 + i) * 12;
                    drawBird(g, flock.get(i), x, y, 0.58, i % 2 == 0, eased);
                }
                drawBird(g, narrator, 960, 560 - Math.sin(time * 1.5) * 18, 1.18, true, eased);
            }
            case FINAL_PORTRAIT -> {
                drawOutcomeMotif(g, eased, time, true);
                drawBird(g, narrator, 960, 585 - Math.sin(time * 1.2) * 10, 1.58, true, eased);
                drawFinalTitle(g, eased);
            }
        }
    }

    private void drawTransformation(GraphicsContext g, double progress, double time) {
        g.save();
        g.setGlobalAlpha(progress);
        for (int i = 0; i < 18; i++) {
            double angle = i * Math.PI * 2 / 18.0 + time * 0.08;
            double distance = 35 + progress * (250 + (i % 4) * 45);
            double x = 1080 + Math.cos(angle) * distance;
            double y = 500 + Math.sin(angle) * distance * 0.58;
            g.setFill(routeAccent().deriveColor(i * 7, 1.0, 1.2, 0.82));
            g.fillPolygon(new double[]{x, x + 14, x - 7}, new double[]{y - 13, y + 11, y + 8}, 3);
        }
        g.restore();
        drawOutcomeMotif(g, progress, time, false);
    }

    private void drawOutcomeMotif(GraphicsContext g, double progress, double time, boolean finalFrame) {
        BirdGame3.BirdType bird = cinematic.narrator();
        switch (bird) {
            case PIGEON -> drawBeacons(g, progress, time);
            case EAGLE -> drawDominionStorm(g, progress, time);
            case FALCON -> drawTargets(g, progress, time);
            case PHOENIX -> drawSeasons(g, progress, time);
            case HUMMINGBIRD -> drawHeartbloom(g, progress, time);
            case TURKEY -> drawTableBells(g, progress, time);
            case ROOSTER -> drawRecallBells(g, progress, time);
            default -> {
            }
        }
        if (finalFrame) {
            g.setFill(new RadialGradient(0, 0, 960, 540, 420, false, CycleMethod.NO_CYCLE,
                    new Stop(0, routeAccent().deriveColor(0, 1, 1.2, 0.22)),
                    new Stop(1, Color.TRANSPARENT)));
            g.fillOval(540, 120, 840, 840);
        }
    }

    private void drawBeacons(GraphicsContext g, double p, double time) {
        g.setStroke(Color.web("#80DEEA", 0.50 * p));
        g.setLineWidth(4);
        for (int i = 0; i < 8; i++) {
            double x = 170 + i * 225;
            double y = 690 - (i % 3) * 110;
            if (i > 0) g.strokeLine(170 + (i - 1) * 225, 690 - ((i - 1) % 3) * 110, x, y);
            double pulse = 12 + Math.sin(time * 3 + i) * 4;
            g.setFill(Color.web("#B2EBF2", 0.84 * p));
            g.fillOval(x - pulse, y - pulse, pulse * 2, pulse * 2);
        }
    }

    private void drawDominionStorm(GraphicsContext g, double p, double time) {
        g.setStroke(Color.web("#FFF176", 0.82 * p));
        g.setLineWidth(7);
        for (int i = 0; i < 6; i++) {
            double x = 170 + i * 320 + Math.sin(time + i) * 30;
            g.strokePolyline(new double[]{x, x + 55, x + 20, x + 95},
                    new double[]{240, 390, 510, 680}, 4);
        }
        g.setStroke(Color.web("#FFE082", 0.65 * p));
        g.setLineWidth(11);
        g.strokeArc(665, 170, 590, 590, 20, 140, javafx.scene.shape.ArcType.OPEN);
    }

    private void drawTargets(GraphicsContext g, double p, double time) {
        g.setStroke(Color.web("#FF5252", 0.82 * p));
        for (int i = 0; i < 7; i++) {
            double angle = i * Math.PI * 2 / 7 + time * 0.08;
            double x = 960 + Math.cos(angle) * 600;
            double y = 510 + Math.sin(angle) * 280;
            double r = 28 + Math.sin(time * 2 + i) * 5;
            g.setLineWidth(4);
            g.strokeOval(x - r, y - r, r * 2, r * 2);
            g.strokeLine(x - r - 12, y, x + r + 12, y);
            g.strokeLine(x, y - r - 12, x, y + r + 12);
        }
    }

    private void drawSeasons(GraphicsContext g, double p, double time) {
        Color[] colors = {Color.web("#FF7043"), Color.web("#64B5F6"), Color.web("#E1F5FE"), Color.web("#81C784")};
        for (int i = 0; i < 4; i++) {
            double angle = i * Math.PI / 2 + time * 0.20;
            double x = 960 + Math.cos(angle) * 470;
            double y = 510 + Math.sin(angle) * 245;
            g.setFill(colors[i].deriveColor(0, 1, 1, 0.22 * p));
            g.fillOval(x - 95, y - 95, 190, 190);
            g.setStroke(colors[i].deriveColor(0, 1, 1.2, 0.86 * p));
            g.setLineWidth(8);
            g.strokeOval(x - 58, y - 58, 116, 116);
        }
    }

    private void drawHeartbloom(GraphicsContext g, double p, double time) {
        g.setStroke(Color.web("#69F0AE", 0.58 * p));
        g.setLineWidth(9);
        g.beginPath();
        g.moveTo(100, 760);
        for (int i = 0; i <= 18; i++) {
            double x = 100 + i * 100;
            double y = 690 - Math.sin(i * 0.8 + time * 0.35) * 150;
            g.lineTo(x, y);
        }
        g.stroke();
        for (int i = 0; i < 12; i++) {
            double x = 140 + i * 150;
            double y = 650 - Math.sin(i * 1.1 + time * 0.35) * 125;
            drawFlower(g, x, y, 22 + Math.sin(time * 2 + i) * 3, p);
        }
    }

    private void drawFlower(GraphicsContext g, double x, double y, double r, double p) {
        for (int petal = 0; petal < 6; petal++) {
            double a = petal * Math.PI / 3;
            g.setFill(Color.web(petal % 2 == 0 ? "#FF80AB" : "#B388FF", 0.82 * p));
            g.fillOval(x + Math.cos(a) * r - r * 0.55, y + Math.sin(a) * r - r * 0.55, r * 1.1, r * 1.1);
        }
        g.setFill(Color.web("#FFF176", 0.92 * p));
        g.fillOval(x - r * 0.42, y - r * 0.42, r * 0.84, r * 0.84);
    }

    private void drawTableBells(GraphicsContext g, double p, double time) {
        g.setFill(Color.web("#5D3318", 0.88 * p));
        g.fillRoundRect(180, 705, 1560, 100, 35, 35);
        for (int i = 0; i < 7; i++) {
            double x = 300 + i * 220;
            double y = 650 - Math.abs(Math.sin(time * 1.4 + i)) * 20;
            drawBell(g, x, y, 0.78, p);
        }
    }

    private void drawRecallBells(GraphicsContext g, double p, double time) {
        g.setStroke(Color.web("#FFE082", 0.42 * p));
        g.setLineWidth(4);
        for (int i = 0; i < 9; i++) {
            double x = 130 + i * 210;
            double y = 680 - Math.sin(i * 0.75) * 210;
            if (i > 0) {
                double px = 130 + (i - 1) * 210;
                double py = 680 - Math.sin((i - 1) * 0.75) * 210;
                g.strokeLine(px, py, x, y);
            }
            drawBell(g, x, y + Math.sin(time * 2 + i) * 8, 0.55, p);
        }
    }

    private void drawBell(GraphicsContext g, double x, double y, double scale, double alpha) {
        g.save();
        g.translate(x, y);
        g.scale(scale, scale);
        g.setFill(Color.web("#FFD54F", 0.90 * alpha));
        g.fillArc(-42, -52, 84, 94, 0, 180, javafx.scene.shape.ArcType.ROUND);
        g.fillRoundRect(-54, -4, 108, 18, 12, 12);
        g.setFill(Color.web("#FFF3B0", 0.96 * alpha));
        g.fillOval(-10, 8, 20, 20);
        g.restore();
    }

    private void drawCrown(GraphicsContext g, double x, double y, double scale, double time, double alpha) {
        g.save();
        g.translate(x, y);
        g.scale(scale, scale);
        g.setGlobalAlpha(alpha);
        double pulse = 1.0 + Math.sin(time * 3.2) * 0.045;
        g.scale(pulse, pulse);
        g.setFill(new RadialGradient(0, 0, 0, 0, 145, false, CycleMethod.NO_CYCLE,
                new Stop(0, Color.web("#FFF59D", 0.42)), new Stop(1, Color.TRANSPARENT)));
        g.fillOval(-145, -145, 290, 290);
        g.setFill(Color.web("#FFD54F"));
        g.fillPolygon(new double[]{-100, -75, -30, 0, 35, 78, 102, 90, -88},
                new double[]{25, -62, 2, -88, 2, -65, 25, 70, 70}, 9);
        g.setFill(Color.web("#4A214F"));
        g.fillOval(-24, 18, 48, 48);
        g.setFill(Color.web("#80DEEA"));
        g.fillOval(-11, 31, 22, 22);
        g.restore();
    }

    private void drawLightColumn(GraphicsContext g, double x, double y, double alpha) {
        g.setFill(new LinearGradient(x, 130, x, 850, false, CycleMethod.NO_CYCLE,
                new Stop(0, Color.TRANSPARENT),
                new Stop(0.45, Color.web("#FFF59D", 0.20 * alpha)),
                new Stop(1, Color.TRANSPARENT)));
        g.fillPolygon(new double[]{x - 180, x - 55, x + 55, x + 180},
                new double[]{850, y, y, 850}, 4);
    }

    private void drawDecisionSplit(GraphicsContext g, double alpha) {
        g.setStroke(routeAccent().deriveColor(0, 1, 1.2, 0.45 * alpha));
        g.setLineWidth(6);
        g.strokeLine(960, 155, 960, 815);
        g.setFill(Color.web("#FFFFFF", 0.035 * alpha));
        g.fillRect(0, 0, 960, 820);
        g.setFill(Color.web("#000000", 0.14 * alpha));
        g.fillRect(960, 0, 960, 820);
    }

    private void drawDebris(GraphicsContext g, double time, double alpha) {
        for (int i = 0; i < 22; i++) {
            double x = 980 + Math.floorMod(i * 91, 620) + Math.sin(time + i) * 10;
            double y = 530 + Math.floorMod(i * 53, 250) + alpha * (i % 4) * 16;
            g.setFill(Color.web(i % 2 == 0 ? "#6D4C78" : "#2B1A31", 0.75));
            g.fillPolygon(new double[]{x, x + 15, x - 9}, new double[]{y - 12, y + 10, y + 7}, 3);
        }
    }

    private void drawBossName(GraphicsContext g, String name, double x, double y, double alpha) {
        g.setFont(Font.font("Consolas", FontWeight.BOLD, 23));
        g.setTextAlign(TextAlignment.CENTER);
        g.setFill(Color.web("#B0BEC5", Math.clamp(alpha, 0.0, 1.0)));
        g.fillText(name.toUpperCase(), x, y);
    }

    private void drawBird(GraphicsContext g, Bird bird, double x, double y, double scale,
                          boolean facingRight, double alpha) {
        if (bird == null || alpha <= 0.0) return;
        g.save();
        g.setGlobalAlpha(Math.clamp(alpha, 0.0, 1.0));
        bird.sizeMultiplier = ACTOR_SIZE * scale * game.campaignCutsceneActorSkinScale(bird);
        double size = 80 * bird.sizeMultiplier;
        bird.x = x - size * 0.5;
        bird.y = y - 66 * scale - size * 0.5;
        bird.facingRight = facingRight;
        bird.suppressSelectEffects = true;
        bird.resetCutsceneVisualPose();
        bird.draw(g);
        g.restore();
    }

    private void drawFinalTitle(GraphicsContext g, double alpha) {
        g.setTextAlign(TextAlignment.CENTER);
        g.setFont(Font.font("Arial Black", FontWeight.BOLD, 48));
        g.setFill(Color.web("#FFE082", alpha));
        g.fillText(cinematic.title(), 960, 185);
        ClassicEndingContent.Ending ending = ClassicEndingContent.endingFor(cinematic.narrator());
        if (ending != null) {
            g.setFont(Font.font("Consolas", FontWeight.BOLD, 20));
            g.setFill(Color.web("#E1F5FE", 0.90 * alpha));
            g.fillText(ending.crownChoice().toUpperCase(), 960, 224);
        }
    }

    private void drawCinematicFrame(GraphicsContext g, double progress) {
        g.setFill(Color.BLACK);
        g.fillRect(0, 0, LOGICAL_WIDTH, 74);
        g.fillRect(0, 916, LOGICAL_WIDTH, 164);
        g.setTextAlign(TextAlignment.LEFT);
        g.setFont(TITLE_FONT);
        g.setFill(Color.web("#FFE082"));
        g.fillText(cinematic.title(), 66, 53);
        if (currentBeat().tableau() == ClassicEndingContent.Tableau.FINAL_PORTRAIT
                && cinematic.routeRecord() != null) {
            g.setTextAlign(TextAlignment.CENTER);
            g.setFont(RECORD_FONT);
            g.setFill(Color.web("#80DEEA"));
            g.fillText(ClassicEndingContent.routeRecordText(cinematic.routeRecord()), 960, 1040);
        }
    }

    private void drawNarration(GraphicsContext g, String narration) {
        List<String> lines = wrap(narration, NARRATION_FONT, 1580);
        g.setTextAlign(TextAlignment.CENTER);
        g.setFont(NARRATION_FONT);
        g.setFill(Color.web("#F5F7FA"));
        double startY = lines.size() > 1 ? 964 : 990;
        for (int i = 0; i < lines.size(); i++) g.fillText(lines.get(i), 960, startY + i * 43);
    }

    private void drawProgress(GraphicsContext g) {
        double totalWidth = cinematic.beats().size() * 28.0;
        double startX = 960 - totalWidth * 0.5;
        for (int i = 0; i < cinematic.beats().size(); i++) {
            g.setFill(i == beatIndex ? routeAccent() : Color.web("#546E7A", 0.72));
            double r = i == beatIndex ? 7 : 4;
            g.fillOval(startX + i * 28 - r, 91 - r, r * 2, r * 2);
        }
    }

    private void drawTransition(GraphicsContext g, double progress) {
        double alpha = 0.0;
        if (progress < 0.10) alpha = 1.0 - ease(progress / 0.10);
        else if (progress > 0.91) alpha = ease((progress - 0.91) / 0.09);
        if (alpha > 0.0) {
            g.setFill(Color.web("#000000", Math.clamp(alpha, 0.0, 1.0)));
            g.fillRect(0, 0, LOGICAL_WIDTH, LOGICAL_HEIGHT);
        }
    }

    private List<String> wrap(String text, Font font, double maxWidth) {
        List<String> lines = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        for (String word : text.split("\\s+")) {
            String candidate = current.isEmpty() ? word : current + " " + word;
            Text measure = new Text(candidate);
            measure.setFont(font);
            if (!current.isEmpty() && measure.getLayoutBounds().getWidth() > maxWidth) {
                lines.add(current.toString());
                current.setLength(0);
                current.append(word);
            } else {
                current.setLength(0);
                current.append(candidate);
            }
        }
        if (!current.isEmpty()) lines.add(current.toString());
        return lines;
    }

    private Color routeAccent() {
        ClassicEndingContent.Ending ending = ClassicEndingContent.endingFor(cinematic.narrator());
        if (ending == null) return Color.web("#80DEEA");
        return switch (ending.alignment()) {
            case HOPEFUL -> Color.web("#64FFDA");
            case AMBIGUOUS -> Color.web("#FFAB40");
            case DOMINATING -> Color.web("#FFD740");
        };
    }

    private ClassicEndingContent.Beat currentBeat() {
        return cinematic.beats().get(Math.clamp(beatIndex, 0, cinematic.beats().size() - 1));
    }

    private double elapsedSeconds(long now) {
        if (beatStartNanos == 0L) return 0.0;
        long effectiveNow = paused && pausedAtNanos > 0L ? pausedAtNanos : now;
        return Math.max(0.0, (effectiveNow - beatStartNanos - accumulatedPauseNanos) / 1_000_000_000.0);
    }

    private void moveBeat(int direction) {
        if (finished || cinematic == null || cinematic.beats().isEmpty()) return;
        int next = beatIndex + direction;
        if (next >= cinematic.beats().size()) {
            finish();
            return;
        }
        beatIndex = Math.max(0, next);
        beatStartNanos = 0L;
        pausedAtNanos = 0L;
        accumulatedPauseNanos = 0L;
        paused = false;
        refreshPauseButton();
        presentBeat();
    }

    private void presentBeat() {
        if (cinematic == null || cinematic.beats().isEmpty()) return;
        if ((ClassicEndingContent.isContinuousPanorama(cinematic)
                || ClassicEndingContent.isSubglacialMontage(cinematic)
                || ClassicEndingContent.isStillwaterRevelation(cinematic)
                || ClassicEndingContent.isOpiumTwelfthFuture(cinematic)
                || ClassicEndingContent.isHeisenBlueVault(cinematic)) && beatIndex > 0) return;
        game.playClassicEndingTableauCue(currentBeat().tableau());
    }

    private void togglePause() {
        if (finished) return;
        long now = System.nanoTime();
        if (!paused) {
            paused = true;
            pausedAtNanos = now;
        } else {
            paused = false;
            if (pausedAtNanos > 0L) accumulatedPauseNanos += Math.max(0L, now - pausedAtNanos);
            pausedAtNanos = 0L;
        }
    }

    private void refreshPauseButton() {
        if (pauseButton != null) pauseButton.setText(paused ? "RESUME" : "PAUSE");
    }

    private void finish() {
        if (finished) return;
        finished = true;
        stopTimer();
        game.finishCampaignSequenceMusic();
        game.resetAfterCampaignCutscene();
        Runnable callback = onFinished;
        onFinished = null;
        if (callback != null) callback.run();
    }

    private void stopTimer() {
        if (timer != null) {
            timer.stop();
            timer = null;
        }
    }

    private static double ease(double value) {
        double clamped = Math.clamp(value, 0.0, 1.0);
        return clamped * clamped * (3.0 - 2.0 * clamped);
    }
}
