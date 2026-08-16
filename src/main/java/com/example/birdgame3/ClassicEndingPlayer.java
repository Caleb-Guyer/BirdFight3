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
        if (continuousPanorama) {
            double routeProgress = Math.clamp((beatIndex + progress) / cinematic.beats().size(), 0.0, 1.0);
            drawRoadrunnerPanorama(g, now / 1_000_000_000.0, routeProgress);
        } else if (subglacialMontage) {
            double routeProgress = Math.clamp((beatIndex + progress) / cinematic.beats().size(), 0.0, 1.0);
            drawPenguinSubglacialMontage(g, now / 1_000_000_000.0, routeProgress);
        } else {
            drawBackground(g, now / 1_000_000_000.0, progress);
            drawTableau(g, currentBeat().tableau(), progress, now / 1_000_000_000.0);
        }
        drawCinematicFrame(g, progress);
        drawNarration(g, currentBeat().narration());
        drawProgress(g);
        if (!continuousPanorama && !subglacialMontage) drawTransition(g, progress);
        g.restore();
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
                || ClassicEndingContent.isSubglacialMontage(cinematic)) && beatIndex > 0) return;
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
