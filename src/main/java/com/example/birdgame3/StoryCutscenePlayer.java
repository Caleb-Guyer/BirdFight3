package com.example.birdgame3;

import javafx.animation.AnimationTimer;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.effect.ColorAdjust;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.RadialGradient;
import javafx.scene.paint.Stop;
import javafx.scene.paint.CycleMethod;
import javafx.scene.shape.ArcType;
import javafx.scene.shape.StrokeLineCap;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.TextAlignment;
import javafx.scene.text.Text;
import javafx.stage.Stage;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Presentation-only Canvas dialogue cinematics for The Still Sky.
 *
 * <p>The player uses {@link AnimationTimer} and closed-form animation. It never
 * reads or advances {@link SimRng}; combat is not running while a story scene
 * owns the stage.
 */
final class StoryCutscenePlayer {
    private static final double LOGICAL_WIDTH = 1920.0;
    private static final double LOGICAL_HEIGHT = 1080.0;
    private static final double BACKING_WIDTH = 1600.0;
    private static final double BACKING_HEIGHT = 900.0;
    private static final double PICTURE_BOTTOM = 820.0;
    private static final double ACTOR_SCREEN_SIZE = 2.2;
    private static final double MIN_ACTOR_SEPARATION = 390.0;
    private static final double ACTOR_STAGE_LEFT = 270.0;
    private static final double ACTOR_STAGE_RIGHT = 1650.0;
    private static final Font SPEAKER_FONT = Font.font("Arial Black", FontWeight.BOLD, 28);
    private static final Font DIALOGUE_FONT = Font.font("Arial", FontWeight.SEMI_BOLD, 30);
    private static final Font LOCATION_FONT = Font.font("Consolas", FontWeight.BOLD, 18);
    private static final Font SCENE_TITLE_FONT = Font.font("Arial Black", FontWeight.BOLD, 26);
    private static final ColorAdjust RECORDED_VOICE_SILHOUETTE_EFFECT =
            new ColorAdjust(0.0, -1.0, -0.88, 0.18);

    private record MotionOffset(double x, double y) {
        private static final MotionOffset NONE = new MotionOffset(0.0, 0.0);
    }

    private final BirdGame3 game;
    private final Map<BirdGame3.BirdType, Bird> actors = new EnumMap<>(BirdGame3.BirdType.class);
    private final Map<String, Boolean> actorOnLeft = new HashMap<>();
    private Bird oldSparrowActor;
    private Bird recordedVoiceActor;
    private StoryCampaign.Cutscene scene;
    private List<StoryCampaign.DialogueLine> lines = List.of();
    private BirdGame3.BirdType selectedBird;
    private String selectedSkinKey;
    private int lineIndex;
    private boolean paused;
    private boolean manual;
    private boolean finished;
    private long lineStartNanos;
    private long pausedAtNanos;
    private long accumulatedPauseNanos;
    private AnimationTimer timer;
    private Canvas canvas;
    private Button pauseButton;
    private Button textModeButton;
    private Runnable onFinished;
    private boolean resumeGameplayOnFinish;

    StoryCutscenePlayer(BirdGame3 game) {
        this.game = game;
    }

    void play(Stage stage, StoryCampaign.Cutscene scene, BirdGame3.BirdType selectedBird,
              String selectedSkinKey, boolean resumeGameplayOnFinish, Runnable onFinished) {
        if (timer != null) {
            timer.stop();
            timer = null;
        }
        game.beginCampaignCutsceneInputCapture();
        game.resetAfterCampaignCutscene();
        this.scene = scene;
        this.selectedBird = selectedBird;
        this.selectedSkinKey = selectedSkinKey;
        this.lines = scene.linesFor(selectedBird);
        this.lineIndex = 0;
        this.paused = false;
        this.manual = false;
        this.finished = false;
        this.lineStartNanos = 0L;
        this.pausedAtNanos = 0L;
        this.accumulatedPauseNanos = 0L;
        this.onFinished = onFinished;
        this.resumeGameplayOnFinish = resumeGameplayOnFinish;
        prepareActors(selectedSkinKey);

        // A missing/invalid dialogue script must never strand the player on an
        // empty scene. Campaign content validation normally prevents this,
        // but continuing is safer than turning a recoverable content problem
        // into a permanent black screen.
        if (lines.isEmpty()) {
            System.err.println("Story cutscene has no dialogue: " + scene.id()
                    + "; continuing campaign.");
            finish();
            return;
        }

        if (canvas == null) {
            canvas = new Canvas(BACKING_WIDTH, BACKING_HEIGHT);
            canvas.setScaleX(LOGICAL_WIDTH / BACKING_WIDTH);
            canvas.setScaleY(LOGICAL_HEIGHT / BACKING_HEIGHT);
        } else {
            Parent parent = canvas.getParent();
            if (parent instanceof Pane pane) {
                pane.getChildren().remove(canvas);
            }
        }
        StackPane content = new StackPane(canvas);
        content.setMinSize(LOGICAL_WIDTH, LOGICAL_HEIGHT);
        content.setPrefSize(LOGICAL_WIDTH, LOGICAL_HEIGHT);
        content.setMaxSize(LOGICAL_WIDTH, LOGICAL_HEIGHT);
        content.setStyle("-fx-background-color: black;");

        Button previous = controlButton("BACK", () -> moveLine(-1));
        pauseButton = controlButton("PAUSE", this::togglePause);
        textModeButton = controlButton("TEXT: AUTO", () -> {
            manual = !manual;
            refreshTextModeButton();
        });
        Button next = controlButton("NEXT", () -> moveLine(1));
        Button skip = controlButton("SKIP SCENE", this::finish);
        HBox controls = new HBox(9, previous, pauseButton, textModeButton, next, skip);
        controls.setAlignment(Pos.CENTER_RIGHT);
        controls.setPadding(new Insets(7, 16, 7, 16));
        controls.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
        controls.setStyle("-fx-background-color: rgba(2,8,14,0.82);"
                + "-fx-background-radius: 12; -fx-border-color: rgba(128,222,234,0.28);"
                + "-fx-border-radius: 12; -fx-border-width: 1;");
        StackPane.setMargin(controls, new Insets(7, 20, 0, 0));
        StackPane.setAlignment(controls, Pos.TOP_RIGHT);
        content.getChildren().add(controls);

        StackPane root = new StackPane(content);
        root.getProperties().put("noAutoScale", true);
        root.setStyle("-fx-background-color: black;");
        Scene fxScene = new Scene(root, LOGICAL_WIDTH, LOGICAL_HEIGHT, Color.BLACK);
        game.prepareCampaignCutsceneScene(fxScene, root, content);
        game.addCampaignSceneEventFilter(fxScene, KeyEvent.KEY_PRESSED, event -> {
            game.noteCampaignCutsceneKeyState(event.getCode(), true);
            if (event.getCode() == KeyCode.SPACE || event.getCode() == KeyCode.ENTER) {
                moveLine(1);
            } else if (event.getCode() == KeyCode.P) {
                togglePause();
            } else if (event.getCode() == KeyCode.M) {
                manual = !manual;
                refreshTextModeButton();
            } else if (event.getCode() == KeyCode.ESCAPE) {
                finish();
            } else {
                return;
            }
            event.consume();
        });
        game.addCampaignSceneEventFilter(fxScene, KeyEvent.KEY_RELEASED,
                event -> game.noteCampaignCutsceneKeyState(event.getCode(), false));
        canvas.setOnMouseClicked(event -> moveLine(1));

        // Paint before handing the scene to the Stage. AnimationTimer does not
        // promise an immediate pulse, so relying on its first callback exposes
        // a fully black frame during the results -> story transition. It also
        // meant a render exception could leave that black frame installed
        // forever with no visible way forward.
        long firstFrameNanos = System.nanoTime();
        lineStartNanos = firstFrameNanos;
        if (!renderSafely(firstFrameNanos)) {
            return;
        }
        game.setCampaignScene(stage, fxScene);
        game.startCampaignCutscenePresentation(scene);
        game.presentCampaignCutsceneLine(scene, lines.getFirst());

        timer = new AnimationTimer() {
            @Override
            public void handle(long now) {
                if (lineStartNanos == 0L) {
                    lineStartNanos = now;
                }
                if (!renderSafely(now)) {
                    return;
                }
                if (!paused && !manual
                        && elapsedSeconds(now) >= automaticDuration(lines.get(lineIndex).text())) {
                    moveLine(1);
                }
            }
        };
        timer.start();
        canvas.requestFocus();
    }

    /**
     * Draws an authored story frame into another Canvas without taking over a
     * Stage. The official trailer uses this so its narrative beats are the
     * game's real cutscenes, including the same clean, reset character poses.
     */
    void renderTrailerFrame(GraphicsContext g, StoryCampaign.Cutscene trailerScene,
                            BirdGame3.BirdType trailerSelectedBird, String trailerSkinKey,
                            int requestedLineIndex, double lineElapsedSeconds) {
        if (g == null || trailerScene == null) {
            return;
        }
        boolean sceneChanged = scene == null
                || !scene.id().equals(trailerScene.id())
                || selectedBird != trailerSelectedBird
                || !Objects.equals(selectedSkinKey, trailerSkinKey);
        if (sceneChanged) {
            scene = trailerScene;
            selectedBird = trailerSelectedBird;
            selectedSkinKey = trailerSkinKey;
            lines = scene.linesFor(selectedBird);
            prepareActors(trailerSkinKey);
        }
        if (lines.isEmpty()) {
            return;
        }
        lineIndex = Math.clamp(requestedLineIndex, 0, lines.size() - 1);
        paused = false;
        lineStartNanos = 0L;
        pausedAtNanos = 0L;
        accumulatedPauseNanos = 0L;
        long now = (long) (Math.max(0.0, lineElapsedSeconds) * 1_000_000_000.0);

        g.save();
        drawBackground(g, now);
        StoryCampaign.DialogueLine line = lines.get(lineIndex);
        drawShot(g, line, now);
        drawCinematicOverlay(g, line, now);
        drawSubtitle(g, line);
        drawLetterbox(g);
        g.restore();
    }

    private void prepareActors(String selectedSkinKey) {
        actors.clear();
        actorOnLeft.clear();
        oldSparrowActor = null;
        recordedVoiceActor = null;
        boolean nextActorOnLeft = true;
        for (StoryCampaign.DialogueLine line : lines) {
            if (!actorOnLeft.containsKey(line.speaker())) {
                actorOnLeft.put(line.speaker(), nextActorOnLeft);
                nextActorOnLeft = !nextActorOnLeft;
            }
            if ("Old Sparrow".equals(line.speaker()) && oldSparrowActor == null) {
                oldSparrowActor = game.createCampaignCutsceneBird(
                        BirdGame3.BirdType.TITMOUSE, BirdGame3.OLD_SPARROW_SKIN);
            }
            if (usesRecordedVultureSilhouette(line) && recordedVoiceActor == null) {
                recordedVoiceActor = game.createCampaignCutsceneBird(BirdGame3.BirdType.VULTURE, null);
            }
            if (line.bird() != null && !actors.containsKey(line.bird())) {
                String skin = line.bird() == selectedBird ? selectedSkinKey : null;
                actors.put(line.bird(), game.createCampaignCutsceneBird(line.bird(), skin));
            }
        }
    }

    private Button controlButton(String text, Runnable action) {
        Button button = new Button(text);
        button.setFont(Font.font("Arial Black", FontWeight.BOLD, 13));
        button.setTextFill(Color.WHITE);
        button.setStyle("-fx-background-color: rgba(8,21,34,0.86);"
                + "-fx-border-color: #80DEEA; -fx-border-width: 1.5;"
                + "-fx-background-radius: 9; -fx-border-radius: 9;");
        button.setOnAction(event -> action.run());
        return button;
    }

    private void refreshTextModeButton() {
        if (textModeButton != null) {
            textModeButton.setText(manual ? "TEXT: MANUAL" : "TEXT: AUTO");
        }
    }

    private void render(long now) {
        GraphicsContext g = canvas.getGraphicsContext2D();
        double width = canvas.getWidth();
        double height = canvas.getHeight();
        g.save();
        try {
            g.scale(width / LOGICAL_WIDTH, height / LOGICAL_HEIGHT);
            drawBackground(g, now);
            if (!lines.isEmpty()) {
                StoryCampaign.DialogueLine line = lines.get(lineIndex);
                drawShot(g, line, now);
                drawCinematicOverlay(g, line, now);
                drawSubtitle(g, line);
            }
            drawLetterbox(g);
        } finally {
            g.restore();
        }
    }

    /**
     * Keeps a presentation-only drawing failure from trapping the campaign on
     * a blank Canvas. The exception remains visible in the console for repair,
     * while the player's completed mission continues through its normal
     * callback exactly once.
     */
    private boolean renderSafely(long now) {
        try {
            render(now);
            return true;
        } catch (RuntimeException failure) {
            String sceneId = scene == null ? "<unknown>" : scene.id();
            System.err.println("Story cutscene rendering failed for " + sceneId
                    + "; continuing campaign.");
            failure.printStackTrace(System.err);
            finish();
            return false;
        }
    }

    private void drawBackground(GraphicsContext g, long now) {
        Color top;
        Color bottom;
        switch (scene.location()) {
            case CITY -> { top = Color.web("#08172A"); bottom = Color.web("#3B2458"); }
            case DOCK -> { top = Color.web("#061D2B"); bottom = Color.web("#1D6077"); }
            case ASHFALL_CATHEDRAL -> { top = Color.web("#120610"); bottom = Color.web("#8B1D24"); }
            case FROSTBITE_FJORD -> { top = Color.web("#07162E"); bottom = Color.web("#76B8D4"); }
            case VIBRANT_JUNGLE, FOREST -> { top = Color.web("#092B2D"); bottom = Color.web("#2D6C43"); }
            case DESERT -> { top = Color.web("#4E2430"); bottom = Color.web("#C87845"); }
            case CAVE -> { top = Color.web("#050812"); bottom = Color.web("#243044"); }
            case SKYCLIFFS -> { top = Color.web("#172B52"); bottom = Color.web("#7799C8"); }
            case BEACON_CROWN -> { top = Color.web("#03050E"); bottom = Color.web("#311443"); }
            case BATTLEFIELD -> { top = Color.web("#142235"); bottom = Color.web("#576A72"); }
            default -> { top = Color.web("#13242D"); bottom = Color.web("#49666B"); }
        }
        g.setFill(new LinearGradient(0, 0, 0, LOGICAL_HEIGHT, false, CycleMethod.NO_CYCLE,
                new Stop(0, top), new Stop(1, bottom)));
        g.fillRect(0, 0, LOGICAL_WIDTH, LOGICAL_HEIGHT);

        double time = now / 1_000_000_000.0;
        drawEnvironment(g, time);
        drawAtmosphere(g, time);
    }

    private void drawEnvironment(GraphicsContext g, double time) {
        double horizon = 770;
        switch (scene.location()) {
            case CITY -> {
                g.setFill(Color.web("#03070D", 0.82));
                for (int i = 0; i < 13; i++) {
                    double x = i * 165.0 - 35;
                    double buildingHeight = 180 + Math.floorMod(scene.id().hashCode() + i * 83, 260);
                    g.fillRect(x, horizon - buildingHeight, 132, buildingHeight + 80);
                    g.setFill(Color.web(i % 3 == 0 ? "#80DEEA" : "#B388FF", 0.28));
                    for (int row = 0; row < 5; row++) {
                        double lit = Math.floorMod(scene.id().hashCode() + i * 37 + row * 11, 4);
                        if (lit != 0) g.fillRect(x + 22 + (row % 2) * 52, horizon - buildingHeight + 28 + row * 34, 18, 7);
                    }
                    g.setFill(Color.web("#03070D", 0.82));
                }
                g.setStroke(Color.web("#80DEEA", 0.20));
                g.setLineWidth(3);
                for (int i = 0; i < 8; i++) {
                    double x = 80 + i * 270;
                    g.strokeLine(x, horizon, x + 90, 390 + (i % 3) * 70);
                }
            }
            case DOCK -> {
                g.setFill(Color.web("#020A10", 0.78));
                g.fillRect(0, horizon - 28, LOGICAL_WIDTH, 130);
                g.setStroke(Color.web("#7AD7E8", 0.28));
                g.setLineWidth(3);
                for (int i = 0; i < 7; i++) {
                    double pierX = 75 + i * 310;
                    g.strokeLine(pierX, horizon, pierX, 470 + (i % 2) * 55);
                    g.strokeLine(pierX, 505, pierX + 135, 420);
                }
                for (int i = 0; i < 12; i++) {
                    double waveX = Math.floorMod(i * 191L + scene.id().hashCode(), 2000L) - 40;
                    g.strokeArc(waveX, horizon + 18 + (i % 3) * 22, 130, 24, 5, 165, ArcType.OPEN);
                }
            }
            case ASHFALL_CATHEDRAL -> {
                g.setFill(Color.web("#080309", 0.82));
                for (int i = 0; i < 7; i++) {
                    double x = i * 330.0 - 90;
                    g.fillPolygon(new double[]{x, x + 140, x + 280},
                            new double[]{horizon, 300 + (i % 2) * 100, horizon}, 3);
                }
                g.setFill(new RadialGradient(0, 0, 960, 745, 560, false, CycleMethod.NO_CYCLE,
                        new Stop(0, Color.web("#FF6D00", 0.24)),
                        new Stop(1, Color.TRANSPARENT)));
                g.fillRect(300, 350, 1320, 470);
            }
            case FROSTBITE_FJORD -> {
                g.setFill(Color.web("#DDF8FF", 0.20));
                for (int i = 0; i < 9; i++) {
                    double x = i * 255.0 - 130;
                    double peak = 360 + Math.floorMod(scene.id().hashCode() + i * 47, 210);
                    g.fillPolygon(new double[]{x, x + 165, x + 330},
                            new double[]{horizon, peak, horizon}, 3);
                }
                g.setFill(Color.web("#07111E", 0.68));
                g.fillRect(0, horizon, LOGICAL_WIDTH, 80);
            }
            case VIBRANT_JUNGLE, FOREST -> {
                g.setFill(Color.web("#031511", 0.72));
                for (int i = 0; i < 14; i++) {
                    double x = i * 155.0 - 45;
                    double trunkWidth = 42 + i % 3 * 18;
                    g.fillRect(x, 240 + i % 4 * 45, trunkWidth, horizon);
                    g.fillOval(x - 85, 190 + i % 4 * 45, 220, 150);
                }
                g.setStroke(Color.web("#81C784", 0.28));
                g.setLineWidth(5);
                for (int i = 0; i < 8; i++) {
                    double x = 130 + i * 260;
                    g.strokeArc(x, 120, 170, 520, 160, 95, ArcType.OPEN);
                }
            }
            case DESERT -> {
                g.setFill(Color.web("#2B1520", 0.35));
                g.fillOval(-230, 610, 1100, 360);
                g.fillOval(590, 570, 1320, 420);
                g.setStroke(Color.web("#FFD180", 0.22));
                g.setLineWidth(4);
                for (int i = 0; i < 6; i++) {
                    g.strokeArc(i * 370.0 - 180, 650 + i % 2 * 34, 610, 180, 15, 145, ArcType.OPEN);
                }
            }
            case CAVE -> {
                g.setFill(Color.web("#010207", 0.82));
                for (int i = 0; i < 12; i++) {
                    double x = i * 185.0 - 55;
                    double length = 90 + Math.floorMod(scene.id().hashCode() + i * 73, 250);
                    g.fillPolygon(new double[]{x, x + 85, x + 155},
                            new double[]{66, 66 + length, 66}, 3);
                }
                g.fillRect(0, horizon, LOGICAL_WIDTH, 120);
            }
            case SKYCLIFFS -> {
                g.setFill(Color.web("#E3F2FD", 0.16));
                for (int i = 0; i < 8; i++) {
                    double cloudX = Math.floorMod(scene.id().hashCode() + i * 311L + (long) (time * 8), 2300L) - 190;
                    double cloudY = 170 + i % 4 * 115;
                    g.fillOval(cloudX, cloudY, 220, 70);
                    g.fillOval(cloudX + 85, cloudY - 25, 180, 90);
                }
                g.setFill(Color.web("#09111E", 0.72));
                g.fillPolygon(new double[]{0, 0, 530, 760, 1030},
                        new double[]{horizon, 480, 620, horizon, horizon}, 5);
                g.fillPolygon(new double[]{1210, 1480, 1780, 1920, 1920},
                        new double[]{horizon, 610, 450, 430, horizon}, 5);
            }
            case BEACON_CROWN -> {
                g.setStroke(Color.web("#CE93D8", 0.30));
                g.setLineWidth(7);
                for (int i = 0; i < 6; i++) {
                    double radius = 120 + i * 105;
                    g.strokeArc(960 - radius, 510 - radius * 0.48, radius * 2, radius * 0.96,
                            6 + time * (i % 2 == 0 ? 3 : -3), 168, ArcType.OPEN);
                }
                g.setFill(Color.web("#02030A", 0.82));
                for (int i = 0; i < 5; i++) {
                    double x = 175 + i * 390.0;
                    g.fillPolygon(new double[]{x, x + 85, x + 170},
                            new double[]{horizon, 260 + (i % 2) * 90, horizon}, 3);
                }
            }
            case BATTLEFIELD -> {
                g.setFill(Color.web("#060A10", 0.68));
                g.fillRect(0, horizon, LOGICAL_WIDTH, 100);
                g.setStroke(Color.web("#CFD8DC", 0.20));
                g.setLineWidth(5);
                for (int i = 0; i < 7; i++) {
                    double x = 90 + i * 300;
                    g.strokeLine(x, horizon, x + 150, 510 + i % 2 * 80);
                    g.strokeLine(x + 150, 510 + i % 2 * 80, x + 260, horizon);
                }
            }
            default -> {
                g.setFill(Color.web("#05080C", 0.72));
                g.fillRect(0, horizon, LOGICAL_WIDTH, 110);
            }
        }
        g.setFill(Color.web("#02050A", 0.56));
        g.fillRect(0, horizon, LOGICAL_WIDTH, PICTURE_BOTTOM - horizon);
    }

    private void drawAtmosphere(GraphicsContext g, double time) {
        Color particleColor = switch (scene.location()) {
            case ASHFALL_CATHEDRAL -> Color.web("#FFB74D");
            case FROSTBITE_FJORD -> Color.web("#F2FBFF");
            case DESERT -> Color.web("#FFD180");
            case VIBRANT_JUNGLE, FOREST -> Color.web("#C5E1A5");
            case BEACON_CROWN -> Color.web("#CE93D8");
            case CITY, DOCK -> Color.web("#80DEEA");
            default -> Color.web("#D6F5FF");
        };
        boolean falling = scene.location() == BirdGame3.MapType.FROSTBITE_FJORD
                || scene.location() == BirdGame3.MapType.CITY
                || scene.location() == BirdGame3.MapType.DOCK;
        boolean rising = scene.location() == BirdGame3.MapType.ASHFALL_CATHEDRAL
                || scene.location() == BirdGame3.MapType.BEACON_CROWN;
        g.setFill(particleColor);
        for (int i = 0; i < 58; i++) {
            long seed = scene.id().hashCode() * 31L + i * 173L;
            double x = Math.floorMod(seed, 2100L) - 90.0;
            double baseY = 85 + Math.floorMod(seed * 13L + i * 97L, 690L);
            double driftX = time * (8 + i % 7);
            double travelY = time * (18 + i % 9);
            x = Math.floorMod((long) (x + driftX), 2100L) - 90.0;
            double y;
            if (falling) {
                y = 85 + Math.floorMod((long) (baseY + travelY), 700L);
            } else if (rising) {
                y = 785 - Math.floorMod((long) (baseY + travelY), 700L);
            } else {
                y = baseY + Math.sin(time * 0.7 + i) * 12;
            }
            double size = 2 + i % 5;
            g.setGlobalAlpha(0.12 + (i % 5) * 0.035);
            if (falling && scene.location() != BirdGame3.MapType.FROSTBITE_FJORD) {
                g.setStroke(particleColor);
                g.setLineWidth(Math.max(1.0, size * 0.55));
                g.strokeLine(x, y, x - 16, y + 54);
            } else {
                g.fillOval(x, y, size, size);
            }
        }
        g.setGlobalAlpha(1.0);
    }

    private void drawShot(GraphicsContext g, StoryCampaign.DialogueLine line, long now) {
        double elapsed = elapsedSeconds(now);
        double easedOpening = smoothStep(Math.min(1.0, elapsed / 0.75));
        double cameraX = switch (line.shot()) {
            case CLOSE -> (actorOnLeft.getOrDefault(line.speaker(), true) ? 145 : -145) * easedOpening;
            case PAN -> -170 + easedOpening * 340;
            case ACTION -> Math.sin(Math.min(1.0, elapsed / 0.55) * Math.PI) * 72;
            case REVEAL -> (1.0 - easedOpening) * -105;
            default -> 0;
        };
        double cameraY = line.shot() == StoryCampaign.ShotStyle.REVEAL
                ? (1.0 - easedOpening) * 45
                : 0.0;
        if (line.shot() == StoryCampaign.ShotStyle.ACTION && elapsed < 0.45) {
            double shake = (1.0 - elapsed / 0.45) * 7.0;
            cameraX += Math.sin(elapsed * 92.0) * shake;
            cameraY += Math.cos(elapsed * 77.0) * shake * 0.55;
        }
        double zoom = switch (line.shot()) {
            case CLOSE -> 1.46;
            case TWO_SHOT -> 1.12;
            case ACTION -> 1.20;
            case REVEAL -> 0.88 + Math.min(1.0, elapsed) * 0.22;
            case CROWD, ESTABLISHING, WIDE -> 0.86;
            default -> 1.0;
        };
        g.save();
        g.translate(LOGICAL_WIDTH / 2.0, 560 + cameraY);
        g.scale(zoom, zoom);
        g.translate(-LOGICAL_WIDTH / 2.0 + cameraX, -560);

        StoryCampaign.DialogueLine prior = previousDistinctLine(lineIndex, line);
        double actorScale = screenConstantActorScale(zoom);
        MotionOffset currentMotion = motionOffset(line.motion(), elapsed, true);
        if (prior != null) {
            double currentX = actorAnchorX(line);
            double priorX = actorAnchorX(prior);
            if (priorX == currentX) {
                priorX = oppositeActorAnchor(currentX);
            }
            double[] centers = resolveActorCenters(
                    priorX,
                    currentX + currentMotion.x(),
                    MIN_ACTOR_SEPARATION * actorScale,
                    ACTOR_STAGE_LEFT,
                    ACTOR_STAGE_RIGHT);
            double priorY = 700;
            double currentY = 680 + currentMotion.y();
            drawActorLighting(g, centers[0], priorY, actorScale, false, elapsed);
            drawActorLighting(g, centers[1], currentY, actorScale, true, elapsed);
            drawMotionEffects(g, line, centers[1], currentY, currentMotion, elapsed, actorScale, true);
            drawActor(g, prior, centers[0], priorY, facesRightAtAnchor(priorX),
                    false, elapsed, actorScale);
            drawActor(g, line, centers[1], currentY, facesRightAtAnchor(currentX),
                    true, elapsed, actorScale);
            drawMotionEffects(g, line, centers[1], currentY, currentMotion, elapsed, actorScale, false);
        } else {
            boolean facingRight = actorOnLeft.getOrDefault(line.speaker(), true);
            double actorX = Math.clamp(960 + currentMotion.x(), ACTOR_STAGE_LEFT, ACTOR_STAGE_RIGHT);
            double actorY = 680 + currentMotion.y();
            drawActorLighting(g, actorX, actorY, actorScale, true, elapsed);
            drawMotionEffects(g, line, actorX, actorY, currentMotion, elapsed, actorScale, true);
            drawActor(g, line, actorX, actorY, facingRight, true, elapsed, actorScale);
            drawMotionEffects(g, line, actorX, actorY, currentMotion, elapsed, actorScale, false);
        }
        g.restore();

        if (line.shot() == StoryCampaign.ShotStyle.BLACK) {
            g.setFill(Color.BLACK);
            g.fillRect(0, 0, LOGICAL_WIDTH, PICTURE_BOTTOM);
        } else if (line.shot() == StoryCampaign.ShotStyle.REVEAL) {
            g.setFill(Color.web("#D8F7FF", Math.max(0.0, 0.38 - elapsed * 0.22)));
            g.fillRect(0, 0, LOGICAL_WIDTH, PICTURE_BOTTOM);
        }
    }

    private MotionOffset motionOffset(StoryCampaign.ActorMotion motion, double elapsed, boolean speaking) {
        double progress;
        return switch (motion) {
            case ENTER_LEFT -> {
                progress = smoothStep(Math.min(1.0, elapsed / 1.05));
                yield new MotionOffset(-390 * (1.0 - progress), -18 * Math.sin(progress * Math.PI));
            }
            case ENTER_RIGHT -> {
                progress = smoothStep(Math.min(1.0, elapsed / 1.05));
                yield new MotionOffset(390 * (1.0 - progress), -18 * Math.sin(progress * Math.PI));
            }
            case FLY_BY -> {
                progress = smoothStep(Math.min(1.0, elapsed / 1.20));
                yield new MotionOffset(-420 * (1.0 - progress), -120 * Math.sin(progress * Math.PI));
            }
            case ATTACK -> {
                progress = Math.min(1.0, elapsed / 0.72);
                yield new MotionOffset(Math.sin(progress * Math.PI) * 92, -14 * Math.sin(progress * Math.PI));
            }
            case RECOIL -> {
                progress = Math.min(1.0, elapsed / 0.85);
                yield new MotionOffset(-Math.sin(progress * Math.PI) * 82, Math.sin(progress * Math.PI) * 18);
            }
            case TURN_AWAY -> new MotionOffset(speaking ? 34 : -34, 0);
            case EXIT_LEFT -> {
                progress = smoothStep(Math.min(1.0, elapsed / 1.15));
                yield new MotionOffset(-480 * progress, -30 * progress);
            }
            case EXIT_RIGHT -> {
                progress = smoothStep(Math.min(1.0, elapsed / 1.15));
                yield new MotionOffset(480 * progress, -30 * progress);
            }
            case FALL -> {
                progress = Math.min(1.0, elapsed / 1.35);
                yield new MotionOffset(-28 * progress, progress * progress * 260);
            }
            case RISE -> {
                progress = smoothStep(Math.min(1.0, elapsed / 1.2));
                yield new MotionOffset(0, 165 * (1.0 - progress));
            }
            default -> new MotionOffset(0, Math.sin(elapsed * 2.0) * 5);
        };
    }

    private void drawActor(GraphicsContext g, StoryCampaign.DialogueLine line, double x, double y,
                           boolean facingRight, boolean speaking, double elapsed, double scale) {
        if (line.motion() == StoryCampaign.ActorMotion.TURN_AWAY) {
            facingRight = !facingRight;
        }

        if (usesRecordedVultureSilhouette(line)) {
            drawRecordedVultureSilhouette(g, x, y, facingRight, scale);
            if (speaking) {
                drawSpeakerPointer(g, x, y, scale, elapsed);
            }
            return;
        }

        if (line.bird() == null) {
            if ("Old Sparrow".equals(line.speaker())) {
                drawBirdActor(g, oldSparrowActor, x, y, facingRight, scale);
            } else if ("Crown System".equals(line.speaker())) {
                drawCrownSystem(g, x, y, scale, elapsed);
            } else {
                drawCivilian(g, x, y, scale);
            }
            if (speaking) {
                drawSpeakerPointer(g, x, y, scale, elapsed);
            }
            return;
        }
        Bird actor = actors.get(line.bird());
        drawBirdActor(g, actor, x, y, facingRight, scale);
        if (speaking) {
            drawSpeakerPointer(g, x, y, scale, elapsed);
        }
    }

    static boolean usesRecordedVultureSilhouette(StoryCampaign.DialogueLine line) {
        if (line == null || line.text() == null) return false;
        String text = line.text().stripLeading();
        String prefix = "Recorded voice:";
        return text.length() >= prefix.length()
                && text.regionMatches(true, 0, prefix, 0, prefix.length());
    }

    private void drawRecordedVultureSilhouette(GraphicsContext g, double x, double y,
                                                boolean facingRight, double scale) {
        if (recordedVoiceActor == null) return;
        g.save();
        g.setGlobalAlpha(0.78);
        g.setEffect(RECORDED_VOICE_SILHOUETTE_EFFECT);
        drawBirdActor(g, recordedVoiceActor, x, y, facingRight, scale);
        g.restore();
    }

    private void drawBirdActor(GraphicsContext g, Bird actor, double x, double y,
                               boolean facingRight, double scale) {
        if (actor == null) return;
        actor.sizeMultiplier = ACTOR_SCREEN_SIZE * scale * game.campaignCutsceneActorSkinScale(actor);
        double drawSize = 80 * actor.sizeMultiplier;
        actor.x = x - drawSize / 2.0;
        actor.y = y - 66 * scale - drawSize / 2.0;
        actor.facingRight = facingRight;
        actor.suppressSelectEffects = true;
        actor.resetCutsceneVisualPose();
        actor.draw(g);
    }

    private void drawCrownSystem(GraphicsContext g, double x, double y,
                                 double scale, double elapsed) {
        g.save();
        g.translate(x, y - 105 * scale);
        g.scale(scale, scale);
        double pulse = 1.0 + Math.sin(elapsed * 3.4) * 0.055;
        g.scale(pulse, pulse);
        g.setFill(new RadialGradient(0, 0, 0, 0, 125, false, CycleMethod.NO_CYCLE,
                new Stop(0, Color.web("#E1F5FE", 0.34)),
                new Stop(0.48, Color.web("#80DEEA", 0.15)),
                new Stop(1, Color.TRANSPARENT)));
        g.fillOval(-125, -125, 250, 250);
        g.setStroke(Color.web("#80DEEA", 0.82));
        g.setLineWidth(5);
        g.strokeOval(-72, -72, 144, 144);
        g.setStroke(Color.web("#E1F5FE", 0.64));
        g.setLineWidth(3);
        g.strokeArc(-102, -48, 204, 96, elapsed * 28, 228, ArcType.OPEN);
        g.strokeArc(-54, -106, 108, 212, -elapsed * 36, 196, ArcType.OPEN);
        g.setFill(Color.web("#E1F5FE", 0.92));
        g.fillPolygon(new double[]{0, 26, 0, -26},
                new double[]{-46, 0, 46, 0}, 4);
        g.setFill(Color.web("#80DEEA", 0.48));
        g.fillOval(-9, -9, 18, 18);
        g.restore();
    }

    private void drawSpeakerPointer(GraphicsContext g, double x, double y, double scale, double elapsed) {
        double pulse = Math.sin(elapsed * 5.5) * 4.0 * scale;
        double top = y - 252 * scale + pulse;
        double tip = y - 216 * scale + pulse;
        double halfWidth = 18 * scale;
        g.setFill(Color.web("#02080E", 0.78));
        g.fillPolygon(
                new double[]{x - halfWidth - 4 * scale, x + halfWidth + 4 * scale, x},
                new double[]{top - 4 * scale, top - 4 * scale, tip + 5 * scale},
                3);
        g.setFill(Color.web("#80DEEA"));
        g.fillPolygon(
                new double[]{x - halfWidth, x + halfWidth, x},
                new double[]{top, top, tip},
                3);
        g.setStroke(Color.WHITE.deriveColor(0, 1, 1, 0.72));
        g.setLineWidth(1.5 * scale);
        g.strokePolygon(
                new double[]{x - halfWidth, x + halfWidth, x},
                new double[]{top, top, tip},
                3);
    }

    private void drawActorLighting(GraphicsContext g, double x, double y, double scale,
                                   boolean speaking, double elapsed) {
        double pulse = speaking ? 0.05 + Math.sin(elapsed * 2.4) * 0.018 : 0.0;
        double radius = (speaking ? 250 : 185) * scale;
        g.setFill(new RadialGradient(0, 0, x, y - 70 * scale, radius, false,
                CycleMethod.NO_CYCLE,
                new Stop(0, Color.web(speaking ? "#B2EBF2" : "#90A4AE", speaking ? 0.16 + pulse : 0.07)),
                new Stop(0.55, Color.web(speaking ? "#80DEEA" : "#607D8B", speaking ? 0.055 : 0.025)),
                new Stop(1, Color.TRANSPARENT)));
        g.fillOval(x - radius, y - 70 * scale - radius, radius * 2, radius * 2);

        g.setFill(Color.web("#000000", speaking ? 0.34 : 0.26));
        g.fillOval(x - 92 * scale, y - 22 * scale, 184 * scale, 34 * scale);
    }

    private void drawMotionEffects(GraphicsContext g, StoryCampaign.DialogueLine line,
                                   double x, double y, MotionOffset offset, double elapsed,
                                   double scale, boolean behindActor) {
        StoryCampaign.ActorMotion motion = line.motion();
        if (behindActor) {
            if (motion == StoryCampaign.ActorMotion.FLY_BY
                    || motion == StoryCampaign.ActorMotion.ENTER_LEFT
                    || motion == StoryCampaign.ActorMotion.ENTER_RIGHT
                    || motion == StoryCampaign.ActorMotion.EXIT_LEFT
                    || motion == StoryCampaign.ActorMotion.EXIT_RIGHT) {
                double direction = offset.x() >= 0 ? -1.0 : 1.0;
                double strength = Math.min(1.0, Math.abs(offset.x()) / 260.0);
                if (strength < 0.02) return;
                g.save();
                g.setLineCap(StrokeLineCap.ROUND);
                for (int i = 0; i < 6; i++) {
                    double trailY = y - (125 - i * 35) * scale;
                    double length = (80 + i * 18) * scale * strength;
                    g.setStroke(Color.web(i % 2 == 0 ? "#B2EBF2" : "#80DEEA",
                            Math.max(0.06, 0.25 - i * 0.025) * strength));
                    g.setLineWidth((7 - i * 0.65) * scale);
                    g.strokeLine(x + direction * 70 * scale, trailY,
                            x + direction * (70 * scale + length), trailY + i % 2 * 7);
                }
                g.restore();
            } else if (motion == StoryCampaign.ActorMotion.RISE) {
                g.setStroke(Color.web("#80DEEA", Math.max(0.0, 0.38 - elapsed * 0.18)));
                g.setLineWidth(5 * scale);
                for (int i = -2; i <= 2; i++) {
                    g.strokeLine(x + i * 30 * scale, y + 28 * scale,
                            x + i * 44 * scale, y + 105 * scale);
                }
            }
            return;
        }

        if (motion == StoryCampaign.ActorMotion.ATTACK) {
            double phase = Math.min(1.0, elapsed / 0.72);
            double impactAlpha = Math.max(0.0, 1.0 - Math.abs(phase - 0.5) * 3.2);
            double impactX = x + (line.bird() == null || facesRightAtAnchor(actorAnchorX(line)) ? 125 : -125) * scale;
            g.save();
            g.translate(impactX, y - 92 * scale);
            g.setStroke(Color.web("#FFF59D", impactAlpha * 0.92));
            g.setLineWidth(6 * scale);
            for (int i = 0; i < 10; i++) {
                double angle = i * Math.PI * 2.0 / 10.0;
                double inner = 30 * scale;
                double outer = (70 + i % 3 * 15) * scale;
                g.strokeLine(Math.cos(angle) * inner, Math.sin(angle) * inner,
                        Math.cos(angle) * outer, Math.sin(angle) * outer);
            }
            g.setFill(Color.web("#FFFFFF", impactAlpha * 0.66));
            g.fillOval(-25 * scale, -25 * scale, 50 * scale, 50 * scale);
            g.restore();
        } else if (motion == StoryCampaign.ActorMotion.RECOIL) {
            double alpha = Math.max(0.0, 0.7 - elapsed * 0.55);
            g.setFill(Color.web("#CFD8DC", alpha));
            for (int i = 0; i < 7; i++) {
                double angle = -2.6 + i * 0.36;
                double distance = (40 + elapsed * (90 + i * 12)) * scale;
                g.fillOval(x + Math.cos(angle) * distance,
                        y - 90 * scale + Math.sin(angle) * distance,
                        7 * scale, 7 * scale);
            }
        } else if (motion == StoryCampaign.ActorMotion.FALL) {
            g.setStroke(Color.web("#B0BEC5", Math.max(0.0, 0.35 - elapsed * 0.12)));
            g.setLineWidth(4 * scale);
            for (int i = -1; i <= 1; i++) {
                g.strokeLine(x + i * 28 * scale, y - 215 * scale,
                        x + i * 36 * scale, y - 295 * scale);
            }
        }
    }

    private double actorAnchorX(StoryCampaign.DialogueLine line) {
        return actorOnLeft.getOrDefault(line.speaker(), true) ? 600 : 1250;
    }

    private static double oppositeActorAnchor(double anchorX) {
        return anchorX <= LOGICAL_WIDTH / 2.0 ? 1250 : 600;
    }

    static boolean facesRightAtAnchor(double anchorX) {
        return anchorX <= LOGICAL_WIDTH / 2.0;
    }

    static double screenConstantActorScale(double cameraZoom) {
        return 1.0 / cameraZoom;
    }

    /**
     * Keeps two actors from occupying the same silhouette while preserving
     * which side each actor was directed to use.
     */
    static double[] resolveActorCenters(double first, double second, double minimumSeparation,
                                        double stageLeft, double stageRight) {
        double leftBound = Math.min(stageLeft, stageRight);
        double rightBound = Math.max(stageLeft, stageRight);
        double available = Math.max(0.0, rightBound - leftBound);
        double separation = Math.clamp(Math.abs(minimumSeparation), 0.0, available);
        double resolvedFirst = Math.clamp(first, leftBound, rightBound);
        double resolvedSecond = Math.clamp(second, leftBound, rightBound);
        if (Math.abs(resolvedSecond - resolvedFirst) >= separation) {
            return new double[]{resolvedFirst, resolvedSecond};
        }

        double direction = resolvedSecond >= resolvedFirst ? 1.0 : -1.0;
        double midpoint = (resolvedFirst + resolvedSecond) * 0.5;
        resolvedFirst = midpoint - direction * separation * 0.5;
        resolvedSecond = midpoint + direction * separation * 0.5;

        double minimum = Math.min(resolvedFirst, resolvedSecond);
        if (minimum < leftBound) {
            double shift = leftBound - minimum;
            resolvedFirst += shift;
            resolvedSecond += shift;
        }
        double maximum = Math.max(resolvedFirst, resolvedSecond);
        if (maximum > rightBound) {
            double shift = maximum - rightBound;
            resolvedFirst -= shift;
            resolvedSecond -= shift;
        }
        return new double[]{resolvedFirst, resolvedSecond};
    }

    private static double smoothStep(double value) {
        double t = Math.clamp(value, 0.0, 1.0);
        return t * t * (3.0 - 2.0 * t);
    }

    private StoryCampaign.DialogueLine previousDistinctLine(int from, StoryCampaign.DialogueLine current) {
        for (int i = from - 1; i >= 0; i--) {
            StoryCampaign.DialogueLine candidate = lines.get(i);
            if (!candidate.speaker().equals(current.speaker())) {
                return candidate;
            }
        }
        return null;
    }

    private void drawCivilian(GraphicsContext g, double x, double y, double scale) {
        g.save();
        g.translate(x, y);
        g.scale(scale, scale);
        g.setFill(Color.web("#90A4AE"));
        g.fillOval(-70, -105, 140, 115);
        g.fillOval(-42, -145, 84, 76);
        g.setFill(Color.web("#CFD8DC"));
        g.fillOval(-18, -122, 9, 9);
        g.restore();
    }

    private void drawCinematicOverlay(GraphicsContext g, StoryCampaign.DialogueLine line, long now) {
        double elapsed = elapsedSeconds(now);

        if (line.shot() == StoryCampaign.ShotStyle.ACTION) {
            double alpha = Math.max(0.0, 0.34 - elapsed * 0.18);
            g.save();
            g.setLineCap(StrokeLineCap.ROUND);
            for (int i = 0; i < 13; i++) {
                double y = 120 + i * 48.0;
                double x = Math.floorMod(
                        scene.id().hashCode() * 17L + lineIndex * 89L + i * 151L, 1850L);
                double length = 95 + i % 4 * 45;
                g.setStroke(Color.web(i % 3 == 0 ? "#FFF59D" : "#E1F5FE", alpha));
                g.setLineWidth(3 + i % 3);
                g.strokeLine(x, y, x - length, y + 24);
            }
            g.restore();
        } else if (line.shot() == StoryCampaign.ShotStyle.REVEAL) {
            double ringAlpha = Math.max(0.0, 0.42 - elapsed * 0.17);
            double radius = 150 + elapsed * 135;
            g.setStroke(Color.web("#B2EBF2", ringAlpha));
            g.setLineWidth(5);
            g.strokeOval(LOGICAL_WIDTH / 2.0 - radius, 430 - radius * 0.55,
                    radius * 2, radius * 1.1);
        }

        if (scene.deathScene()) {
            g.setFill(new LinearGradient(0, 0, 0, PICTURE_BOTTOM, false, CycleMethod.NO_CYCLE,
                    new Stop(0, Color.web("#000000", 0.06)),
                    new Stop(0.62, Color.web("#32050A", 0.08)),
                    new Stop(1, Color.web("#5C0A12", 0.20))));
            g.fillRect(0, 0, LOGICAL_WIDTH, PICTURE_BOTTOM);
        }

        g.setFill(new RadialGradient(0, 0, LOGICAL_WIDTH / 2.0, 420, 1030, false,
                CycleMethod.NO_CYCLE,
                new Stop(0, Color.TRANSPARENT),
                new Stop(0.64, Color.web("#000000", 0.02)),
                new Stop(1, Color.web("#000000", 0.62))));
        g.fillRect(0, 0, LOGICAL_WIDTH, PICTURE_BOTTOM);

        double cutFade = Math.max(0.0, 1.0 - elapsed / 0.20);
        if (cutFade > 0.0) {
            g.setFill(Color.web("#000000", cutFade));
            g.fillRect(0, 0, LOGICAL_WIDTH, PICTURE_BOTTOM);
        }

        if (lineIndex == 0 && elapsed < 3.2) {
            double titleAlpha = elapsed < 0.45
                    ? smoothStep(elapsed / 0.45)
                    : Math.clamp((3.2 - elapsed) / 0.75, 0.0, 1.0);
            g.setGlobalAlpha(titleAlpha);
            g.setFill(Color.web("#02070D", 0.82));
            g.fillRoundRect(78, 94, 570, 92, 16, 16);
            g.setStroke(Color.web("#80DEEA", 0.58));
            g.setLineWidth(2);
            g.strokeRoundRect(78, 94, 570, 92, 16, 16);
            g.setTextAlign(TextAlignment.LEFT);
            g.setFont(LOCATION_FONT);
            g.setFill(Color.web("#80DEEA"));
            g.fillText(scene.location().name().replace('_', ' '), 108, 124);
            g.setFont(SCENE_TITLE_FONT);
            g.setFill(Color.WHITE);
            g.fillText(scene.title().toUpperCase(), 108, 164);
            g.setGlobalAlpha(1.0);
        }
    }

    private void drawSubtitle(GraphicsContext g, StoryCampaign.DialogueLine line) {
        g.setFill(Color.web("#02060B", 0.94));
        g.fillRect(0, 820, LOGICAL_WIDTH, 260);
        g.setStroke(Color.web("#80DEEA", 0.75));
        g.setLineWidth(3);
        g.strokeLine(0, 820, LOGICAL_WIDTH, 820);

        g.setTextAlign(TextAlignment.LEFT);
        g.setFill(Color.web("#80DEEA"));
        g.setFont(SPEAKER_FONT);
        g.fillText(line.speaker().toUpperCase(), 180, 880);
        g.setFill(Color.WHITE);
        g.setFont(DIALOGUE_FONT);
        drawWrappedText(g, line.text(), 180, 930, 1560, 42);

        g.setTextAlign(TextAlignment.RIGHT);
        g.setFill(Color.web("#B0BEC5"));
        g.setFont(LOCATION_FONT);
        g.fillText(scene.title() + "  •  " + (lineIndex + 1) + "/" + lines.size(),
                1740, 866);
        g.setTextAlign(TextAlignment.LEFT);
    }

    private void drawLetterbox(GraphicsContext g) {
        g.setFill(Color.BLACK);
        g.fillRect(0, 0, LOGICAL_WIDTH, 66);
        g.fillRect(0, 1044, LOGICAL_WIDTH, 36);
    }

    private void drawWrappedText(GraphicsContext g, String text, double x, double y,
                                 double maxWidth, double lineHeight) {
        StringBuilder line = new StringBuilder();
        double cursorY = y;
        for (String word : text.split("\\s+")) {
            String candidate = line.isEmpty() ? word : line + " " + word;
            Text measurement = new Text(candidate);
            measurement.setFont(g.getFont());
            if (measurement.getLayoutBounds().getWidth() > maxWidth && !line.isEmpty()) {
                g.fillText(line.toString(), x, cursorY);
                cursorY += lineHeight;
                line.setLength(0);
                line.append(word);
            } else {
                if (!line.isEmpty()) line.append(' ');
                line.append(word);
            }
        }
        if (!line.isEmpty()) {
            g.fillText(line.toString(), x, cursorY);
        }
    }

    private void moveLine(int direction) {
        if (finished || lines.isEmpty()) {
            finish();
            return;
        }
        int next = lineIndex + direction;
        if (next >= lines.size()) {
            finish();
            return;
        }
        lineIndex = Math.max(0, next);
        lineStartNanos = 0L;
        accumulatedPauseNanos = 0L;
        pausedAtNanos = paused ? System.nanoTime() : 0L;
        game.presentCampaignCutsceneLine(scene, lines.get(lineIndex));
    }

    private void togglePause() {
        paused = !paused;
        if (pauseButton != null) {
            pauseButton.setText(paused ? "RESUME" : "PAUSE");
        }
        long now = System.nanoTime();
        if (paused) {
            pausedAtNanos = now;
        } else if (pausedAtNanos > 0L) {
            accumulatedPauseNanos += now - pausedAtNanos;
            pausedAtNanos = 0L;
        }
    }

    private double elapsedSeconds(long now) {
        long effectiveNow = paused && pausedAtNanos > 0L ? pausedAtNanos : now;
        return Math.max(0.0, (effectiveNow - lineStartNanos - accumulatedPauseNanos) / 1_000_000_000.0);
    }

    private double automaticDuration(String text) {
        return Math.clamp(2.0 + (text == null ? 0 : text.length()) * 0.038, 2.8, 7.2);
    }

    private void finish() {
        if (finished) return;
        finished = true;
        if (timer != null) {
            timer.stop();
            timer = null;
        }
        game.finishCampaignCutsceneInputCapture(resumeGameplayOnFinish);
        game.resetAfterCampaignCutscene();
        Runnable callback = onFinished;
        onFinished = null;
        if (callback != null) {
            callback.run();
        }
    }
}
