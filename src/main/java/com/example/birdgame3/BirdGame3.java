package com.example.birdgame3;
import com.example.birdgame3.Bird;

import javafx.animation.AnimationTimer;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.Control;
import javafx.scene.control.Label;
import javafx.scene.control.OverrunStyle;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.effect.DropShadow;
import javafx.scene.effect.Glow;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.media.AudioClip;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;
import javafx.stage.Stage;

import java.util.*;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

public class BirdGame3 extends Application {

    public static final int WIDTH = 1920;
    public static final int HEIGHT = 1080;
    public static final int GROUND_Y = 2400;
    public static final double GRAVITY = 0.75;
    private static final int MATCH_DURATION_FRAMES = 90 * 60;
    private static final int TRIAL_DURATION_FRAMES = 180 * 60;
    private static final int COMPETITION_DURATION_FRAMES = 120 * 60;

    final Set<KeyCode> pressedKeys = new HashSet<>();
    public Bird[] players = new Bird[4];
    public boolean[] isAI = new boolean[4];
    private int activePlayers = 2;
    private AnimationTimer timer;
    public List<Platform> platforms = new ArrayList<>();
    public List<PowerUp> powerUps = new ArrayList<>();
    public List<NectarNode> nectarNodes = new ArrayList<>();
    public List<SwingingVine> swingingVines = new ArrayList<>();
    private final List<double[]> cityStars = new ArrayList<>();
    private VBox[] playerSlots;
    private Button[] teamButtons = new Button[4];
    private Button teamModeToggleButton;
    private boolean teamModeEnabled = false;
    private final int[] playerTeams = new int[]{1, 2, 1, 2};
    private final Random random = new Random();
    private long lastPowerUpSpawnTime = 0;
    public static final long POWERUP_SPAWN_INTERVAL = 1_000_000_000L * 8; // every 8 seconds
    public double shakeIntensity = 0;
    private double[] mountainPeaks = null; // will store fixed peak heights for Sky Cliffs
    public int hitstopFrames = 0;
    public List<String> killFeed = new ArrayList<>();
    private final int MAX_FEED_LINES = 6;
    public static final double CEILING_Y = -100;
    public int[] falls = new int[4];
    public int[] scores = new int[4];  // live score: elims*50 + damage/2
    public int[] specialsUsed = new int[4];
    public int[] specialHits = new int[4];
    public int[] specialDamageDealt = new int[4];

    // === MATCH STATS TRACKING ===
    public int[] damageDealt = new int[4];
    public int[] eliminations = new int[4];
    public int[] groundPounds = new int[4];     // Turkey special
    public int[] loungeTime = new int[4];       // Mockingbird lounge active frames
    public int[] leanTime = new int[4];         // Opium Bird lean cloud active frames
    public int[] tauntsPerformed = new int[4];

    public int[] vineGrapplePickups = new int[4];     // times picked up Vine Grapple
    public int[] jungleWins = new int[4];             // wins on Vibrant Jungle map

    // City-specific stats tracking
    public int[] rooftopJumps = new int[4];     // times a bird jumped off a high rooftop
    public int[] neonPickups = new int[4];      // times a bird picked up the new "Neon" power-up
    public int[] cityWins = new int[4];         // wins on city map (for achievement)

    // === SKYCLIFFS-specific stats tracking ===
    public int[] thermalPickups = new int[4]; // times a bird picked up THERMAL power-up
    public int[] highCliffJumps = new int[4]; // jumps from very high platforms (y < GROUND_Y - 1000)
    public int[] cliffWins = new int[4];      // wins on Sky Cliffs map

    // === SUDDEN DEATH SYSTEM ===
    private int matchTimer = MATCH_DURATION_FRAMES;
    private final SuddenDeathController suddenDeath = new SuddenDeathController();

    private boolean matchEnded = false;  // prevents double-trigger

    private boolean isPaused = false;
    private boolean debugHudEnabled = false;
    private long matchStartNano = 0L;
    private boolean balanceOutcomeRecorded = false;

    // === LONG-TERM BALANCE TELEMETRY ===
    private static final int BALANCE_MIN_SAMPLE = 6;
    private static final double BALANCE_HIGH_WINRATE = 0.60;
    private static final double BALANCE_LOW_WINRATE = 0.40;
    private final int[] typePicks = new int[BirdType.values().length];
    private final int[] typeWins = new int[BirdType.values().length];
    private final int[] typeDamage = new int[BirdType.values().length];
    private final int[] typeElims = new int[BirdType.values().length];

    // === FIXED TIMESTEP PAUSE FIX ===
    private long lastUpdate = 0;
    private long accumulator = 0L;

    private StackPane gameRoot;
    private Stage currentStage;

    // === MAP + DYNAMIC CAMERA ===
    public static final double WORLD_WIDTH = 6000;
    public static final double WORLD_HEIGHT = 3000;

    // Camera state
    private double camX = 0, camY = 0;
    private double zoom = 1.0;           // 1.0 = 100%, 0.5 = zoomed out a lot
    public static final double MIN_ZOOM = 0.35;
    public static final double MAX_ZOOM = 1.5;
    public static final double ZOOM_SPEED = 0.008;

    // === MAPS ===
    public enum MapType { FOREST, CITY, SKYCLIFFS, VIBRANT_JUNGLE, CAVE }

    public MapType selectedMap = MapType.FOREST; // default

    public List<Particle> particles = new ArrayList<>();
    public List<CrowMinion> crowMinions = new ArrayList<>();

    // === CITY WIND BURSTS ===
    public List<WindVent> windVents = new ArrayList<>();
    private long lastWindBurstTime = 0;
    public static final long WIND_BURST_INTERVAL = 1_000_000_000L * 6; // every ~6 seconds
    public static final double WIND_FORCE = -28.0; // strong upward boost
    private boolean mutatorModeEnabled = false;
    private boolean competitionModeEnabled = false;
    private MatchMutator activeMutator = MatchMutator.NONE;
    private long activePowerUpSpawnInterval = POWERUP_SPAWN_INTERVAL;
    private long lastMutatorHazardTime = 0L;
    private static final int COMPETITION_ROUND_TARGET = 3;
    private final int[] competitionRoundWins = new int[4];
    private final int[] competitionTeamWins = new int[3];
    private int competitionRoundNumber = 1;
    private boolean competitionSeriesActive = false;
    private boolean competitionDraftComplete = false;
    private String competitionDraftSummary = "";
    private final Set<BirdType> competitionBannedBirds = new HashSet<>();

    public Canvas uiCanvas;
    public GraphicsContext ui;

    private double flashAlpha = 0.0;     // white flash intensity
    private double redFlashAlpha = 0.0;  // red tint for normal big hits
    private int flashTimer = 0;          // frames remaining for flash
    private final UIFactory uiFactory = new UIFactory();

    public void playHitSound(double intensity) {
        if (bonkClip != null) {
            bonkClip.setVolume(Math.min(1.0, intensity / 40.0));
            bonkClip.play();
        }
    }

    private void loadSounds() {
        try {
            String p = "/sounds/";
            bonkClip = new AudioClip(getClass().getResource(p + "bonk.mp3").toExternalForm());
            butterClip = new AudioClip(getClass().getResource(p + "butter.mp3").toExternalForm());
            jalapenoClip = new AudioClip(getClass().getResource(p + "jalapeno.mp3").toExternalForm());
            swingClip = new AudioClip(getClass().getResource(p + "swing.mp3").toExternalForm());
            hugewaveClip = new AudioClip(getClass().getResource(p + "hugewave.mp3").toExternalForm());

            // === NEW MENU & VICTORY MUSIC ===
            menuMusicPlayer = new MediaPlayer(new Media(getClass().getResource(p + "choose_your_seeds.mp3").toExternalForm()));
            menuMusicPlayer.setCycleCount(MediaPlayer.INDEFINITE);
            menuMusicPlayer.setVolume(0.55);

            victoryMusicPlayer = new MediaPlayer(new Media(getClass().getResource(p + "finalfanfare.mp3").toExternalForm()));
            victoryMusicPlayer.setVolume(0.75);

            // === WIIMOTE BUTTON CLICK ===
            buttonClickClip = new AudioClip(getClass().getResource(p + "buttonclick.mp3").toExternalForm());
            buttonClickClip.setVolume(0.9);

            zombieFallingClip = new AudioClip(getClass().getResource(p + "zombie-falling.mp3").toExternalForm());

        } catch (Exception e) {
            System.out.println("Sounds not found - put mp3 files in 'sounds' folder inside src/main/resources");
        }
    }

    private void startMusic() {
        // STOP menu + victory BEFORE starting map music
        if (menuMusicPlayer != null) menuMusicPlayer.stop();
        if (victoryMusicPlayer != null) victoryMusicPlayer.stop();
        if (musicPlayer != null) musicPlayer.stop();

        String file = switch (selectedMap) {
            case FOREST         -> "ultimate_battle.mp3";
            case CITY           -> "braniac_maniac.mp3";
            case SKYCLIFFS      -> "zombotany.mp3";
            case VIBRANT_JUNGLE -> "loonboon.mp3";
            case CAVE           -> "dark-ages-ultimate-battle.mp3";
        };

        try {
            Media media = new Media(getClass().getResource("/sounds/" + file).toExternalForm());
            musicPlayer = new MediaPlayer(media);
            musicPlayer.setCycleCount(MediaPlayer.INDEFINITE);
            musicPlayer.setVolume(0.45);
            musicPlayer.play();
        } catch (Exception e) {
            System.out.println("Music not found: " + file);
        }
    }

    private void playMenuMusic() {
        if (musicPlayer != null) musicPlayer.stop();
        if (victoryMusicPlayer != null) victoryMusicPlayer.stop();
        if (menuMusicPlayer != null) {
            menuMusicPlayer.stop();
            menuMusicPlayer.play();
        }
    }

    // === WIIMOTE-FRIENDLY MENU NAVIGATION (WASD + Space) ===
    private void setupKeyboardNavigation(Scene scene) {
        fitSceneButtons(scene.getRoot());
        javafx.application.Platform.runLater(() -> fitSceneButtons(scene.getRoot()));
        scene.widthProperty().addListener((obs, oldV, newV) -> fitSceneButtons(scene.getRoot()));
        scene.heightProperty().addListener((obs, oldV, newV) -> fitSceneButtons(scene.getRoot()));
        scene.setOnKeyPressed(e -> {
            KeyCode code = e.getCode();

            if (code == KeyCode.F11 && scene.getWindow() instanceof Stage s) {
                s.setFullScreen(!s.isFullScreen());
                e.consume();
                return;
            }

            // WASD â†’ arrows for focus
            KeyCode arrow = switch (code) {
                case W -> KeyCode.UP;
                case S -> KeyCode.DOWN;
                case A -> KeyCode.LEFT;
                case D -> KeyCode.RIGHT;
                default -> null;
            };
            if (arrow != null) {
                e.consume();
                Node focus = scene.getFocusOwner();
                if (focus != null) {
                    focus.fireEvent(new KeyEvent(KeyEvent.KEY_PRESSED, "", "", arrow, false, false, false, false));
                }
            }

            // SPACE/ENTER = SELECT + CLICK SOUND ON EVERY BUTTON
            if (code == KeyCode.SPACE || code == KeyCode.ENTER) {
                playButtonClick();   // guaranteed sound on every button
                Node focus = scene.getFocusOwner();
                if (focus instanceof Button btn) {
                    btn.fire();
                }
                e.consume();
            }
        });
    }

    private void makeSceneResponsive(Scene scene) {
        if (!sceneContainsCanvas(scene.getRoot())) return;
        if (scene.getRoot() instanceof StackPane sp && "responsiveContainer".equals(sp.getId())) return;

        Node content = scene.getRoot();
        StackPane container = new StackPane(content);
        container.setId("responsiveContainer");
        scene.setRoot(container);

        Runnable applyScale = () -> {
            double sx = scene.getWidth() / WIDTH;
            double sy = scene.getHeight() / HEIGHT;
            // Fill the full window on any aspect ratio (no letterboxing borders).
            content.setScaleX(Math.max(0.01, sx));
            content.setScaleY(Math.max(0.01, sy));
        };
        scene.widthProperty().addListener((obs, oldV, newV) -> applyScale.run());
        scene.heightProperty().addListener((obs, oldV, newV) -> applyScale.run());
        applyScale.run();
    }

    private boolean sceneContainsCanvas(Node node) {
        if (node instanceof Canvas) return true;
        if (node instanceof Parent p) {
            for (Node child : p.getChildrenUnmodifiable()) {
                if (sceneContainsCanvas(child)) return true;
            }
        }
        return false;
    }

    private void fitSceneButtons(Node node) {
        if (node instanceof Button b) {
            fitButtonText(b);
        }
        if (node instanceof Parent p) {
            for (Node child : p.getChildrenUnmodifiable()) {
                fitSceneButtons(child);
            }
        }
    }

    private void fitButtonText(Button b) {
        String original = (String) b.getProperties().computeIfAbsent("origText", k -> b.getText());
        if (original == null || original.isBlank()) return;
        if (b.getGraphic() != null) return;

        b.setWrapText(true);
        applyNoEllipsis(b);
        b.setTextAlignment(TextAlignment.CENTER);
        b.setAlignment(Pos.CENTER);

        Font current = b.getFont() != null ? b.getFont() : Font.font("Arial Black", 30);
        double size = current.getSize();
        double minSize = Math.min(12, size);
        double width = b.getPrefWidth() > 0 ? b.getPrefWidth() : 420;
        double height = b.getPrefHeight() > 0 ? b.getPrefHeight() : 96;
        double availableW = Math.max(110, width - 40);
        double availableH = Math.max(40, height - 16);

        List<String> bestLines = List.of(original);
        double chosen = minSize;
        while (size > minSize) {
            Font trial = Font.font(current.getFamily(), size);
            List<String> lines = wrapTextToLines(original, trial, availableW);
            double lineH = measureTextHeight("Ag", trial) * 1.12;
            double blockH = lines.size() * lineH;
            double blockW = maxLineWidth(lines, trial);
            if (blockH <= availableH && blockW <= availableW) {
                bestLines = lines;
                chosen = size;
                break;
            }
            size -= 2;
        }

        if (size <= minSize) {
            Font trial = Font.font(current.getFamily(), minSize);
            bestLines = wrapTextToLines(original, trial, availableW);
            chosen = minSize;
        }

        b.setFont(Font.font(current.getFamily(), chosen));
        b.setText(String.join("\n", bestLines));
    }

    private List<String> wrapTextToLines(String text, Font font, double maxWidth) {
        List<String> lines = new ArrayList<>();
        if (text == null || text.isBlank()) return lines;

        String[] words = text.trim().split("\\s+");
        String current = "";
        for (String word : words) {
            if (measureTextWidth(word, font) > maxWidth) {
                if (!current.isEmpty()) {
                    lines.add(current);
                    current = "";
                }
                lines.addAll(splitWordToFit(word, font, maxWidth));
                continue;
            }
            String candidate = current.isEmpty() ? word : current + " " + word;
            if (measureTextWidth(candidate, font) <= maxWidth || current.isEmpty()) {
                current = candidate;
            } else {
                lines.add(current);
                current = word;
            }
        }
        if (!current.isEmpty()) lines.add(current);
        return lines;
    }

    private List<String> splitWordToFit(String word, Font font, double maxWidth) {
        List<String> chunks = new ArrayList<>();
        if (word == null || word.isEmpty()) return chunks;

        StringBuilder current = new StringBuilder();
        for (int i = 0; i < word.length(); i++) {
            current.append(word.charAt(i));
            if (measureTextWidth(current.toString(), font) > maxWidth) {
                if (current.length() == 1) {
                    chunks.add(current.toString());
                    current.setLength(0);
                } else {
                    char last = current.charAt(current.length() - 1);
                    current.deleteCharAt(current.length() - 1);
                    chunks.add(current.toString());
                    current.setLength(0);
                    current.append(last);
                }
            }
        }
        if (!current.isEmpty()) {
            chunks.add(current.toString());
        }
        return chunks;
    }

    private double maxLineWidth(List<String> lines, Font font) {
        double max = 0;
        for (String line : lines) {
            max = Math.max(max, measureTextWidth(line, font));
        }
        return max;
    }

    private double measureTextWidth(String text, Font font) {
        Text t = new Text(text);
        t.setFont(font);
        return t.getLayoutBounds().getWidth();
    }

    private double measureTextHeight(String text, Font font) {
        Text t = new Text(text);
        t.setFont(font);
        return t.getLayoutBounds().getHeight();
    }

    private void fitMainMenuButtonSingleLine(Button b, double maxSize, double minSize) {
        if (b == null) return;
        String text = b.getText();
        if (text == null || text.isBlank()) return;

        b.setWrapText(false);
        applyNoEllipsis(b);

        double w = b.getWidth() > 0 ? b.getWidth() : (b.getPrefWidth() > 0 ? b.getPrefWidth() : 420);
        double h = b.getHeight() > 0 ? b.getHeight() : (b.getPrefHeight() > 0 ? b.getPrefHeight() : 100);
        double availableW = Math.max(120, w - 44);
        double availableH = Math.max(40, h - 18);

        double size = maxSize;
        while (size > minSize) {
            Font f = Font.font("Arial Black", size);
            if (measureTextWidth(text, f) <= availableW && measureTextHeight("Ag", f) <= availableH) {
                b.setFont(f);
                return;
            }
            size -= 1.0;
        }
        b.setFont(Font.font("Arial Black", minSize));
    }

    private void applyNoEllipsis(Button b) {
        b.setTextOverrun(OverrunStyle.CLIP);
        b.setEllipsisString("");
        String style = b.getStyle();
        if (style == null) style = "";
        if (!style.contains("-fx-text-overrun: clip")) {
            b.setStyle(style + (style.isBlank() ? "" : "; ") + "-fx-text-overrun: clip;");
        }
    }

    private final class UIFactory {
        Button action(String text, double width, double height, double fontSize, String bgColor, double radius, Runnable onAction) {
            Button b = new Button(text);
            b.setPrefSize(width, height);
            b.setFont(Font.font("Arial Black", fontSize));
            b.setWrapText(text != null && text.contains("\n"));
            b.setTextAlignment(TextAlignment.CENTER);
            b.setAlignment(Pos.CENTER);
            b.setStyle("-fx-background-color: " + bgColor + "; -fx-text-fill: white; -fx-background-radius: " + radius + ";");
            applyNoEllipsis(b);
            if (onAction != null) {
                b.setOnAction(e -> {
                    playButtonClick();
                    onAction.run();
                });
            }
            return b;
        }

        void fitSingleLineOnLayout(Button b, double maxSize, double minSize) {
            javafx.application.Platform.runLater(() -> fitMainMenuButtonSingleLine(b, maxSize, minSize));
        }
    }

    private void confirmExitGame(Stage stage) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION,
                "Exit Bird Fight 3?",
                ButtonType.YES,
                ButtonType.NO);
        alert.setTitle("Exit Game");
        alert.setHeaderText("Are you sure you want to quit?");
        alert.initOwner(stage);
        alert.showAndWait().ifPresent(choice -> {
            if (choice == ButtonType.YES) {
                stage.close();
            }
        });
    }

    private void setScenePreservingFullscreen(Stage stage, Scene scene) {
        boolean wasFullscreen = stage.isFullScreen();
        stage.setScene(scene);
        if (wasFullscreen && !stage.isFullScreen()) {
            stage.setFullScreen(true);
        }
    }

    private void playButtonClick() {
        if (buttonClickClip != null) {
            buttonClickClip.stop();
            buttonClickClip.play();
        }
    }

    // === WIIMOTE HIGHLIGHT ===
    private void applyConsoleHighlight(Scene scene) {
        scene.focusOwnerProperty().addListener((obs, oldFocus, newFocus) -> {
            if (oldFocus instanceof Control oldCtrl) restoreControl(oldCtrl);
            if (newFocus instanceof Control newCtrl) highlightControlSimple(newCtrl);
        });
    }

    private void highlightControlSimple(Control ctrl) {
        String original = ctrl.getStyle();
        ctrl.setUserData(original);

        ctrl.setStyle("-fx-background-color: #FFFF00; "
                + "-fx-text-fill: black; "
                + "-fx-border-color: white; "
                + "-fx-border-width: 4; "
                + "-fx-border-radius: 20; "
                + "-fx-background-radius: 20; "
                + "-fx-text-overrun: clip;");
    }

    private void restoreControl(Control ctrl) {
        Object data = ctrl.getUserData();
        if (data instanceof String style) ctrl.setStyle(style);
    }

    private void startPigeonTrial(Stage stage) {
        selectedMap = MapType.CITY;
        isTrialMode = true;
        matchStartNano = System.nanoTime();
        balanceOutcomeRecorded = false;
        this.currentStage = stage;
        activePlayers = 4;

        players[0] = new Bird(1200, BirdType.PIGEON, 0, this);
        players[0].y = GROUND_Y - 400;
        players[0].health = 100;
        players[0].name = "You: Pigeon";
        isAI[0] = false;

        for (int i = 1; i <= 2; i++) {
            players[i] = new Bird(1000 + i * 400, BirdType.PIGEON, i, this);
            players[i].y = GROUND_Y - 400;
            players[i].health = 100;
            players[i].name = "Ally: Pigeon";
            isAI[i] = true;
            players[i].isCitySkin = false;
        }

        players[3] = new Bird(4000, BirdType.PIGEON, 3, this);
        players[3].y = GROUND_Y - 500;
        players[3].health = 300;
        players[3].setBaseMultipliers(2.5, 1.8, 0.4);
        players[3].isCitySkin = true;
        players[3].name = "BOSS: City Pigeon";
        isAI[3] = true;

        matchTimer = TRIAL_DURATION_FRAMES;
        matchEnded = false;
        resetSuddenDeathState();

        addToKillFeed("CITY PIGEON TRIAL: Defeat the boss with your allies!");
        addToKillFeed("You have 2 friendly pigeons helping you!");

        platforms.clear();
        particles.clear();
        powerUps.clear();

        if (selectedMap == MapType.CITY && cityStars.isEmpty()) {
            for (int i = 0; i < 250; i++) {
                cityStars.add(new double[]{ random.nextDouble() * WORLD_WIDTH, random.nextDouble() * (GROUND_Y - 200) });
            }
        }

        crowMinions.clear();
        windVents.clear();

        platforms.add(new Platform(0, GROUND_Y, WORLD_WIDTH, 600));
        platforms.add(new Platform(-500, 0, 500, WORLD_HEIGHT + 1000));
        platforms.add(new Platform(WORLD_WIDTH, 0, 500, WORLD_HEIGHT + 1000));

        double[] buildingX = {400, 1400, 2400, 3400, 4400, 5400};
        for (double bx : buildingX) {
            platforms.add(new Platform(bx - 350, GROUND_Y - 320, 700, 50));
            platforms.add(new Platform(bx - 200, GROUND_Y - 120, 400, 40));
            Platform high1 = new Platform(bx - 150, GROUND_Y - 720, 300, 40);
            Platform high2 = new Platform(bx + 50, GROUND_Y - 1020, 200, 40);
            String[] possibleSigns = {"CLUB", "BAR", "HOTEL", "EAT", "LIVE", "24/7", "OPEN", "PIZZA", "DIVE", "LOUNGE"};
            high1.signText = possibleSigns[random.nextInt(possibleSigns.length)];
            high2.signText = possibleSigns[random.nextInt(possibleSigns.length)];
            platforms.add(high1);
            platforms.add(high2);
            windVents.add(new WindVent(bx - 350 + 100, GROUND_Y - 320, 500));
            windVents.add(new WindVent(bx - 350 + 500, GROUND_Y - 320, 100));
        }

        platforms.add(new Platform(800, GROUND_Y - 450, 400, 40));
        platforms.add(new Platform(1800, GROUND_Y - 600, 400, 40));
        platforms.add(new Platform(2800, GROUND_Y - 400, 400, 40));
        platforms.add(new Platform(3800, GROUND_Y - 550, 400, 40));
        platforms.add(new Platform(4800, GROUND_Y - 480, 400, 40));
        platforms.add(new Platform(1000, GROUND_Y - 100, 600, 40));
        platforms.add(new Platform(3000, GROUND_Y - 150, 500, 40));
        platforms.add(new Platform(4500, GROUND_Y - 80, 400, 40));

        // Generate fixed mountain peaks for consistent background
        mountainPeaks = new double[7];
        double[] mountainX = {0, 800, 1800, 2800, 3800, 4800, WORLD_WIDTH};
        for (int i = 0; i < mountainX.length - 1; i++) {
            mountainPeaks[i] = GROUND_Y - 400 - random.nextDouble() * 600;
        }

        Canvas canvas = new Canvas(WIDTH, HEIGHT);
        GraphicsContext g = canvas.getGraphicsContext2D();
        StackPane root = new StackPane(canvas);
        gameRoot = root;
        root.setStyle("-fx-background-color: #000011;");
        Scene scene = new Scene(root, WIDTH, HEIGHT);
        makeSceneResponsive(scene);
        uiCanvas = new Canvas(WIDTH, HEIGHT);
        ui = uiCanvas.getGraphicsContext2D();
        root.getChildren().add(uiCanvas);

        pressedKeys.clear();
        scene.setOnKeyPressed(e -> handleGameplayKeyPress(stage, e));
        scene.setOnKeyReleased(e -> pressedKeys.remove(e.getCode()));

        if (timer != null) timer.stop();
        lastUpdate = 0;
        accumulator = 0;

        timer = new AnimationTimer() {
            @Override
            public void handle(long now) {
                gameTick(1.0);
                drawGame(g);   // â† ONE LINE instead of 150 lines

                ui.clearRect(0, 0, WIDTH, HEIGHT);

                // Minimap: bottom-right, 200x150, scaled world view
                double mapW = 200, mapH = 150;
                ui.setFill(Color.BLACK.deriveColor(0,1,1,0.6));
                ui.fillRect(WIDTH - mapW - 20, HEIGHT - mapH - 20, mapW, mapH);
                ui.setStroke(Color.WHITE);
                ui.strokeRect(WIDTH - mapW - 20, HEIGHT - mapH - 20, mapW, mapH);
// Draw birds as dots
                double scaleX = mapW / WORLD_WIDTH;
                double scaleY = mapH / WORLD_HEIGHT;
                for (Bird b : players) {
                    if (b != null && b.health > 0) {
                        ui.setFill(b.type.color);
                        double mx = b.x * scaleX;
                        double my = b.y * scaleY;
                        ui.fillOval(WIDTH - mapW - 20 + mx - 4, HEIGHT - mapH - 20 + my - 4, 8, 8);
                    }
                }
// Camera view rect
                double camViewW = (WIDTH / zoom) * scaleX;
                double camViewH = (HEIGHT / zoom) * scaleY;
                ui.setStroke(Color.YELLOW);
                ui.strokeRect(WIDTH - mapW - 20 + camX * scaleX, HEIGHT - mapH - 20 + camY * scaleY, camViewW, camViewH);

                // === CENTERED HEALTH BARS (works for 2/3/4 players) ===
                double barWidth = 400;
                double gap = 20;
                double totalWidth = activePlayers * (barWidth + gap) - gap;
                double startX = (WIDTH - totalWidth) / 2.0;

                for (int i = 0; i < 3; i++) {
                    if (players[i] != null && players[i].health > 0) {
                        drawHealthBar(ui, players[i], startX + i * (barWidth + gap), 80);
                    }
                }

                if (players[3] != null && players[3].health > 0) {
                    ui.setFill(Color.BLACK);
                    ui.fillRoundRect(WIDTH / 2 - 403, 20, 806, 66, 20, 20);
                    ui.setFill(Color.DARKRED.darker());
                    ui.fillRoundRect(WIDTH / 2 - 400, 23, 800, 60, 20, 20);
                    ui.setFill(Color.RED.brighter());
                    ui.fillRoundRect(WIDTH / 2 - 400, 23, 800 * (players[3].health / 300.0), 60, 20, 20);
                    ui.setFill(Color.WHITE);
                    ui.setFont(Font.font("Arial Black", FontWeight.BOLD, 48));
                    ui.fillText("BOSS: CITY PIGEON", WIDTH / 2 - 280, 70);
                    ui.setFont(Font.font("Arial Black", 36));
                    ui.fillText((int)players[3].health + " / 300 HP", WIDTH / 2 - 120, 110);
                }
                ui.setFill(Color.WHITE);
                ui.setFont(Font.font("Arial Black", 48));
                String timeText = "TRIAL: " + (matchTimer / 60) + "s";
                ui.fillText(timeText, WIDTH / 2 - 150, 180);
                ui.setFont(Font.font("Arial Black", 26));
                for (int i = 0; i < killFeed.size(); i++) {
                    String msg = killFeed.get(i);
                    double alpha = Math.max(0.3, 1.0 - (i / (double) MAX_FEED_LINES));
                    ui.setFill(new Color(1, 1, 1, alpha));
                    ui.fillText(msg, WIDTH - 700, 80 + i * 50);
                    ui.setFill(new Color(0, 0, 0, alpha * 0.6));
                    ui.fillText(msg, WIDTH - 698, 78 + i * 50);
                }
                drawDebugBalanceHud(ui);
                if (flashTimer > 0) {
                    flashTimer--;
                    if (flashAlpha > 0) {
                        ui.setFill(Color.WHITE.deriveColor(0, 1, 1, flashAlpha));
                        ui.fillRect(0, 0, WIDTH, HEIGHT);
                        flashAlpha *= 0.89;
                    }
                    if (redFlashAlpha > 0) {
                        ui.setFill(Color.RED.deriveColor(0, 1, 1, redFlashAlpha));
                        ui.fillRect(0, 0, WIDTH, HEIGHT);
                        redFlashAlpha *= 0.86;
                    }
                }
                if (matchEnded) {
                    if (hitstopFrames <= 0) {
                        timer.stop();
                        new AnimationTimer() {
                            int delay = 180;
                            @Override public void handle(long n) {
                                if (--delay <= 0) { this.stop(); showMenu(stage); }
                            }
                        }.start();
                    }
                }
            }
        };

        timer.start();
        setScenePreservingFullscreen(stage, scene);
        startMusic();
        canvas.requestFocus();

        zoom = 0.35;
        camX = WORLD_WIDTH / 2 - WIDTH / (2 * zoom);
        camY = (GROUND_Y - 1200) - HEIGHT / (2 * zoom);

    }

    private void startEagleTrial(Stage stage) {
        selectedMap = MapType.SKYCLIFFS;
        isTrialMode = true;
        matchStartNano = System.nanoTime();
        balanceOutcomeRecorded = false;
        this.currentStage = stage;
        activePlayers = 4;

        // Player
        players[0] = new Bird(1200, BirdType.EAGLE, 0, this);
        players[0].y = GROUND_Y - 400;
        players[0].health = 100;
        players[0].name = "You: Eagle";
        isAI[0] = false;

        // 2 ally AI Eagles
        for (int i = 1; i <= 2; i++) {
            players[i] = new Bird(1000 + i * 400, BirdType.EAGLE, i, this);
            players[i].y = GROUND_Y - 400;
            players[i].health = 100;
            players[i].name = "Ally: Eagle";
            isAI[i] = true;
        }

        // BOSS: Sky Tyrant Eagle
        players[3] = new Bird(4000, BirdType.EAGLE, 3, this);
        players[3].y = GROUND_Y - 800;
        players[3].health = 350;
        players[3].setBaseMultipliers(3.0, 2.2, 0.7);
        players[3].name = "BOSS: SKY TYRANT";
        isAI[3] = true;

        matchTimer = TRIAL_DURATION_FRAMES;
        matchEnded = false;
        resetSuddenDeathState();

        addToKillFeed("SKY TYRANT TRIAL: Defeat the boss in the cliffs!");
        addToKillFeed("Your 2 eagle allies will help you soar and dive!");

        platforms.clear();
        particles.clear();
        powerUps.clear();
        crowMinions.clear();
        windVents.clear();

        // === FULL SKYCLIFFS PLATFORMS AND WIND VENTS (same as in startMatch) ===
        double[] cliffX = {600, 1600, 2600, 3600, 4600, 5400};
        for (double cx : cliffX) {
            platforms.add(new Platform(cx - 400, GROUND_Y - 600, 800, 60));
            platforms.add(new Platform(cx - 200, GROUND_Y - 1000, 400, 50));
            platforms.add(new Platform(cx - 100, GROUND_Y - 1400, 200, 40));
        }
        // Stepping stone chains
        platforms.add(new Platform(800, GROUND_Y - 200, 400, 50));
        platforms.add(new Platform(700, GROUND_Y - 450, 350, 50));
        platforms.add(new Platform(900, GROUND_Y - 700, 400, 50));
        platforms.add(new Platform(750, GROUND_Y - 950, 300, 50));
        platforms.add(new Platform(2200, GROUND_Y - 180, 500, 50));
        platforms.add(new Platform(2100, GROUND_Y - 420, 450, 50));
        platforms.add(new Platform(2300, GROUND_Y - 660, 500, 50));
        platforms.add(new Platform(2200, GROUND_Y - 900, 400, 50));
        platforms.add(new Platform(2150, GROUND_Y - 1150, 350, 50));
        platforms.add(new Platform(4200, GROUND_Y - 220, 450, 50));
        platforms.add(new Platform(4100, GROUND_Y - 480, 400, 50));
        platforms.add(new Platform(4300, GROUND_Y - 740, 450, 50));
        platforms.add(new Platform(4200, GROUND_Y - 1000, 350, 50));
        platforms.add(new Platform(1000, GROUND_Y - 800, 500, 50));
        platforms.add(new Platform(3000, GROUND_Y - 900, 600, 50));
        platforms.add(new Platform(5000, GROUND_Y - 700, 500, 50));

        // Wind vents (thermals)
        for (double cx : cliffX) {
            windVents.add(new WindVent(cx - 300, GROUND_Y - 600, 600));
            windVents.add(new WindVent(cx - 150, GROUND_Y - 1000, 300));
        }
        windVents.add(new WindVent(700, GROUND_Y - 250, 400));
        windVents.add(new WindVent(2100, GROUND_Y - 230, 500));
        windVents.add(new WindVent(4100, GROUND_Y - 270, 450));

        // Boundaries
        platforms.add(new Platform(0, GROUND_Y, WORLD_WIDTH, 600));
        platforms.add(new Platform(-500, 0, 500, WORLD_HEIGHT + 1000));
        platforms.add(new Platform(WORLD_WIDTH, 0, 500, WORLD_HEIGHT + 1000));

        mountainPeaks = new double[7];
        double[] mountainX = {0, 800, 1800, 2800, 3800, 4800, WORLD_WIDTH};
        for (int i = 0; i < mountainX.length - 1; i++) {
            mountainPeaks[i] = GROUND_Y - 400 - random.nextDouble() * 600;
        }

        Canvas canvas = new Canvas(WIDTH, HEIGHT);
        GraphicsContext g = canvas.getGraphicsContext2D();
        StackPane root = new StackPane(canvas);
        gameRoot = root;
        root.setStyle("-fx-background-color: #001133;");
        Scene scene = new Scene(root, WIDTH, HEIGHT);
        makeSceneResponsive(scene);
        uiCanvas = new Canvas(WIDTH, HEIGHT);
        ui = uiCanvas.getGraphicsContext2D();
        root.getChildren().add(uiCanvas);

        pressedKeys.clear();
        scene.setOnKeyPressed(e -> handleGameplayKeyPress(stage, e));
        scene.setOnKeyReleased(e -> pressedKeys.remove(e.getCode()));

        if (timer != null) timer.stop();
        lastUpdate = 0;
        accumulator = 0;

        timer = new AnimationTimer() {
            @Override
            public void handle(long now) {
                gameTick(1.0);
                drawGame(g);   // â† ONE LINE

                ui.clearRect(0, 0, WIDTH, HEIGHT);

                // Minimap: bottom-right, 200x150, scaled world view
                double mapW = 200, mapH = 150;
                ui.setFill(Color.BLACK.deriveColor(0,1,1,0.6));
                ui.fillRect(WIDTH - mapW - 20, HEIGHT - mapH - 20, mapW, mapH);
                ui.setStroke(Color.WHITE);
                ui.strokeRect(WIDTH - mapW - 20, HEIGHT - mapH - 20, mapW, mapH);
// Draw birds as dots
                double scaleX = mapW / WORLD_WIDTH;
                double scaleY = mapH / WORLD_HEIGHT;
                for (Bird b : players) {
                    if (b != null && b.health > 0) {
                        ui.setFill(b.type.color);
                        double mx = b.x * scaleX;
                        double my = b.y * scaleY;
                        ui.fillOval(WIDTH - mapW - 20 + mx - 4, HEIGHT - mapH - 20 + my - 4, 8, 8);
                    }
                }
// Camera view rect
                double camViewW = (WIDTH / zoom) * scaleX;
                double camViewH = (HEIGHT / zoom) * scaleY;
                ui.setStroke(Color.YELLOW);
                ui.strokeRect(WIDTH - mapW - 20 + camX * scaleX, HEIGHT - mapH - 20 + camY * scaleY, camViewW, camViewH);

                // === CENTERED HEALTH BARS (works for 2/3/4 players) ===
                double barWidth = 400;
                double gap = 20;
                double totalWidth = activePlayers * (barWidth + gap) - gap;
                double startX = (WIDTH - totalWidth) / 2.0;

                for (int i = 0; i < 3; i++) {
                    if (players[i] != null && players[i].health > 0) {
                        drawHealthBar(ui, players[i], startX + i * (barWidth + gap), 80);
                    }
                }

                if (players[3] != null && players[3].health > 0) {
                    ui.setFill(Color.BLACK);
                    ui.fillRoundRect(WIDTH / 2 - 403, 20, 806, 66, 20, 20);
                    ui.setFill(Color.DARKRED.darker());
                    ui.fillRoundRect(WIDTH / 2 - 400, 23, 800, 60, 20, 20);
                    ui.setFill(Color.ORANGE.brighter());
                    ui.fillRoundRect(WIDTH / 2 - 400, 23, 800 * (players[3].health / 350.0), 60, 20, 20);
                    ui.setFill(Color.WHITE);
                    ui.setFont(Font.font("Arial Black", FontWeight.BOLD, 48));
                    ui.fillText("BOSS: SKY TYRANT", WIDTH / 2 - 280, 70);
                    ui.setFont(Font.font("Arial Black", 36));
                    ui.fillText((int)players[3].health + " / 350 HP", WIDTH / 2 - 120, 110);
                }
                ui.setFill(Color.WHITE);
                ui.setFont(Font.font("Arial Black", 48));
                String timeText = "TRIAL: " + (matchTimer / 60) + "s";
                ui.fillText(timeText, WIDTH / 2 - 150, 180);
                ui.setFont(Font.font("Arial Black", 26));
                for (int i = 0; i < killFeed.size(); i++) {
                    String msg = killFeed.get(i);
                    double alpha = Math.max(0.3, 1.0 - (i / (double) MAX_FEED_LINES));
                    ui.setFill(new Color(1, 1, 1, alpha));
                    ui.fillText(msg, WIDTH - 700, 80 + i * 50);
                    ui.setFill(new Color(0, 0, 0, alpha * 0.6));
                    ui.fillText(msg, WIDTH - 698, 78 + i * 50);
                }
                drawDebugBalanceHud(ui);
                if (flashTimer > 0) {
                    flashTimer--;
                    if (flashAlpha > 0) {
                        ui.setFill(Color.WHITE.deriveColor(0, 1, 1, flashAlpha));
                        ui.fillRect(0, 0, WIDTH, HEIGHT);
                        flashAlpha *= 0.89;
                    }
                    if (redFlashAlpha > 0) {
                        ui.setFill(Color.RED.deriveColor(0, 1, 1, redFlashAlpha));
                        ui.fillRect(0, 0, WIDTH, HEIGHT);
                        redFlashAlpha *= 0.86;
                    }
                }
                if (matchEnded) {
                    if (hitstopFrames <= 0) {
                        timer.stop();
                        new AnimationTimer() {
                            int delay = 180;
                            @Override public void handle(long n) {
                                if (--delay <= 0) { this.stop(); showMenu(stage); }
                            }
                        }.start();
                    }
                }
            }
        };

        timer.start();
        setScenePreservingFullscreen(stage, scene);
        startMusic();
        canvas.requestFocus();

        // Fixed zoomed-out camera for full cliffs view
        zoom = 0.4;
        camX = WORLD_WIDTH / 2 - WIDTH / (2 * zoom);
        camY = (GROUND_Y - 600) - HEIGHT / (2 * zoom);
    }

    // === SOUND & MUSIC ===
    public AudioClip bonkClip, butterClip, jalapenoClip, swingClip, hugewaveClip, buttonClickClip, zombieFallingClip;
    public MediaPlayer musicPlayer, menuMusicPlayer, victoryMusicPlayer;

    public static final String[] ACHIEVEMENT_NAMES = {
            "First Blood", "Dominator", "Annihilator", "Turkey Slam Master", "Lean God", "Lounge Lizard",
            "Power-Up Hoarder", "Fall Guy", "Taunt Lord", "Clutch God",
            "Rooftop Runner", "Neon Addict", "Urban King",
            "Thermal Rider", "Cliff Diver", "Sky Emperor",
            "Vine Swinger", "Canopy King", "Pelican King"                                    // NEW JUNGLE ACHIEVEMENTS
    };

    public static final String[] ACHIEVEMENT_DESCRIPTIONS = {
            "Get your first elimination in a match",
            "Eliminate 3 opponents in one match (max possible in 4-player)",
            "Eliminate all other opponents in one match",
            "Perform 3 ground pounds in one match (Turkey only)",
            "Spend a total of 5 minutes inside your lean cloud (Opium Bird only)",
            "Heal a total of 100 HP inside the lounge (Mockingbird only)",
            "Pick up 10 power-ups in one match",
            "Fall off the bottom of the map 3 times",
            "Perform 10 taunts in total",
            "Win a match with less than 20 HP remaining",
            "Perform 20 high jumps on rooftops in the City map",
            "Pick up 8 Neon Boost power-ups across matches",
            "Win 5 matches on Pigeon's Rooftops",
            "Pick up 10 Thermal Rise power-ups across matches",
            "Perform 15 jumps from the highest cliffs (Sky Cliffs map)",
            "Win 5 matches on Sky Cliffs",
            "Pick up 8 Vine Grapple power-ups across matches",
            "Win 5 matches on Vibrant Jungle",
            "Pelican Plunge 15 enemies across matches"
    };

    public static final int ACHIEVEMENT_COUNT = ACHIEVEMENT_NAMES.length;
    public boolean[] achievementsUnlocked = new boolean[ACHIEVEMENT_COUNT];
    public int[] achievementProgress = new int[ACHIEVEMENT_COUNT]; // for tracking partial progress

    // === SKIN UNLOCKS ===
    public boolean cityPigeonUnlocked = true;
    public boolean noirPigeonUnlocked = false;
    public boolean isTrialMode = false;
    public boolean eagleSkinUnlocked = true; // Sky Tyrant Eagle skin
    public boolean batUnlocked = true;

    public static final int SKIN_COUNT = 2; // City Pigeon + Noir Pigeon

    // === CLASSIC MODE ===
    private boolean classicModeActive = false;
    private BirdType classicSelectedBird = BirdType.PIGEON;
    private final boolean[] classicCompleted = new boolean[BirdType.values().length];
    private final boolean[] classicSkinUnlocked = new boolean[BirdType.values().length];
    private final List<ClassicEncounter> classicRun = new ArrayList<>();
    private int classicRoundIndex = 0;
    private ClassicEncounter classicEncounter = null;
    private String classicRunCodename = "";
    private boolean classicTeamMode = false;
    private final int[] classicTeams = new int[]{1, 2, 2, 2};

    private enum ClassicTwist {
        STORM_LIFTS("Storm Lifts", "Thermal vents surge and reward vertical control."),
        CROW_CARNIVAL("Crow Carnival", "Neutral crows patrol lanes from the opening bell."),
        TITAN_CACHE("Titan Cache", "The arena spawns high-impact powerups immediately."),
        NECTAR_BLOOM("Nectar Bloom", "Nectar nodes pulse in map hotspots for burst tempo."),
        SHOCK_DROPS("Shock Drops", "Overcharge and frost effects drop more often.");

        final String label;
        final String description;

        ClassicTwist(String label, String description) {
            this.label = label;
            this.description = description;
        }
    }

    static class ClassicFighter {
        final BirdType type;
        final String title;
        final double health;
        final double powerMult;
        final double speedMult;

        ClassicFighter(BirdType type, String title, double health, double powerMult, double speedMult) {
            this.type = type;
            this.title = title;
            this.health = health;
            this.powerMult = powerMult;
            this.speedMult = speedMult;
        }
    }

    static class ClassicEncounter {
        final String name;
        final String announcer;
        final String briefing;
        final MapType map;
        final MatchMutator mutator;
        final ClassicTwist twist;
        final int timerFrames;
        final ClassicFighter[] allies;
        final ClassicFighter[] enemies;

        ClassicEncounter(String name, String announcer, String briefing, MapType map,
                         MatchMutator mutator, ClassicTwist twist, int timerFrames,
                         ClassicFighter[] allies, ClassicFighter[] enemies) {
            this.name = name;
            this.announcer = announcer;
            this.briefing = briefing;
            this.map = map;
            this.mutator = mutator;
            this.twist = twist;
            this.timerFrames = timerFrames;
            this.allies = allies;
            this.enemies = enemies;
        }
    }

    // === EPISODES ===
    private boolean storyModeActive = false;
    private boolean storyReplayMode = false;
    private int storyChapterIndex = 0;
    private EpisodeType selectedEpisode = EpisodeType.PIGEON;
    private int pigeonEpisodeUnlockedChapters = 1;
    private boolean pigeonEpisodeCompleted = false;
    private int batEpisodeUnlockedChapters = 1;
    private boolean batEpisodeCompleted = false;
    private int pelicanEpisodeUnlockedChapters = 1;
    private boolean pelicanEpisodeCompleted = false;
    private static final String PIGEON_EPISODE_TITLE = "Episode 1: Rise Of The Rooftop Pigeon";
    private static final String PIGEON_EPISODE_AWARD = "Noir Pigeon Skin";
    private static final String BAT_EPISODE_TITLE = "Episode 2: Nocturne Of The Echo Bat";
    private static final String BAT_EPISODE_AWARD = "Bat Character Unlock";
    private static final String PELICAN_EPISODE_TITLE = "Episode 3: Tempest Of The Iron Beak";
    private static final String PELICAN_EPISODE_AWARD = "Sky King Eagle Skin";
    private boolean storyTeamMode = false;
    private final int[] storyTeams = new int[]{1, 2, 2, 2};
    private int storyMatchTimerOverride = -1;

    private enum EpisodeType { PIGEON, BAT, PELICAN }
    private enum MatchMutator {
        NONE("None", "Standard rules."),
        LOW_GRAVITY("Low Gravity", "Everyone falls slower and stays airborne longer."),
        POWERUP_STORM("Power-Up Storm", "Power-ups rain in much faster."),
        CROW_SURGE("Crow Surge", "Neutral crows periodically invade mid-match."),
        TURBO_BRAWL("Turbo Brawl", "Faster movement and harder hits for all birds.");

        final String label;
        final String description;

        MatchMutator(String label, String description) {
            this.label = label;
            this.description = description;
        }
    }

    static class StoryChapter {
        final String title;
        final String speaker;
        final String dialogue;
        final BirdType opponentType;
        final String opponentName;
        final MapType map;
        final double opponentHealth;
        final double opponentPowerMult;
        final double opponentSpeedMult;

        StoryChapter(String title, String speaker, String dialogue, BirdType opponentType, String opponentName,
                     MapType map, double opponentHealth, double opponentPowerMult, double opponentSpeedMult) {
            this.title = title;
            this.speaker = speaker;
            this.dialogue = dialogue;
            this.opponentType = opponentType;
            this.opponentName = opponentName;
            this.map = map;
            this.opponentHealth = opponentHealth;
            this.opponentPowerMult = opponentPowerMult;
            this.opponentSpeedMult = opponentSpeedMult;
        }
    }

    private final StoryChapter[] storyChapters = new StoryChapter[] {
            new StoryChapter(
                    "Chapter 1: Rooftop Rumors",
                    "Old Sparrow",
                    "A challenger landed on your block. Win the opening duel and defend your rooftop name.",
                    BirdType.HUMMINGBIRD,
                    "Rival: Neon Hummingbird",
                    MapType.CITY,
                    115,
                    1.0,
                    1.15
            ),
            new StoryChapter(
                    "Chapter 2: The Forest Test",
                    "Forest Crow",
                    "Word spread beyond the city. In the forest, power beats style. Survive the Turkey's slam game.",
                    BirdType.TURKEY,
                    "Rival: Iron Turkey",
                    MapType.FOREST,
                    150,
                    1.2,
                    0.9
            ),
            new StoryChapter(
                    "Chapter 3: Edge Of The Cliffs",
                    "Sky Scout",
                    "A Razorbill enforcer controls the cliffs. Beat the speed and survive the blade storm.",
                    BirdType.RAZORBILL,
                    "Rival: Cliff Razorbill",
                    MapType.SKYCLIFFS,
                    180,
                    1.3,
                    1.1
            ),
            new StoryChapter(
                    "Chapter 4: Canopy Kingpin",
                    "Jungle Oracle",
                    "Final chapter. The Vulture boss runs the underground ring. End it and claim your reward.",
                    BirdType.VULTURE,
                    "Boss: Canopy Vulture",
                    MapType.VIBRANT_JUNGLE,
                    220,
                    1.35,
                    1.05
            )
    };

    private final StoryChapter[] batStoryChapters = new StoryChapter[] {
            new StoryChapter(
                    "Chapter 1: Cave Whispers",
                    "Blind Sage",
                    "The cavern hums with danger. Prove your sonar and survive the opening hunt.",
                    BirdType.SHOEBILL,
                    "Rival: Stone Shoebill",
                    MapType.CAVE,
                    130,
                    1.1,
                    0.95
            ),
            new StoryChapter(
                    "Chapter 2: Echo Ambush",
                    "Tunnel Scout",
                    "A zip predator stalks the dark lanes. Outfly it before it locks on.",
                    BirdType.TITMOUSE,
                    "Rival: Tunnel Titmouse",
                    MapType.CAVE,
                    150,
                    1.2,
                    1.15
            ),
            new StoryChapter(
                    "Chapter 3: Throne Of Night",
                    "Cavern Oracle",
                    "Final duel. The cave warlord commands the abyss. End the reign.",
                    BirdType.VULTURE,
                    "Boss: Abyss Vulture",
                    MapType.CAVE,
                    230,
                    1.35,
                    1.05
            )
    };

    private final StoryChapter[] pelicanStoryChapters = new StoryChapter[] {
            new StoryChapter(
                    "Chapter 1: Harbor Blitz",
                    "Dockmaster Gull",
                    "Cargo alarms blare across the rooftops. Outrun the courier ace before the storm front closes in.",
                    BirdType.TITMOUSE,
                    "Rival: Flash Courier",
                    MapType.CITY,
                    145,
                    1.15,
                    1.35
            ),
            new StoryChapter(
                    "Chapter 2: Jungle Tag Team",
                    "Canopy Ranger",
                    "Your bat ally dives in. Coordinate and crush the bruisers before they isolate either of you.",
                    BirdType.TURKEY,
                    "Duo: Iron Turkey + Stone Shoebill",
                    MapType.VIBRANT_JUNGLE,
                    170,
                    1.25,
                    1.0
            ),
            new StoryChapter(
                    "Chapter 3: Gale Gauntlet",
                    "Sky Marshal",
                    "Three enforcers hold the cliffs. Survive the pressure lanes and break their formation.",
                    BirdType.RAZORBILL,
                    "Squad: Razor, Talon, Neon",
                    MapType.SKYCLIFFS,
                    180,
                    1.25,
                    1.2
            ),
            new StoryChapter(
                    "Chapter 4: Abyss Lockdown",
                    "Echo Warden",
                    "The cave seals shut. Sudden death crows pour in early. End the warden before the swarm overwhelms you.",
                    BirdType.VULTURE,
                    "Boss: Swarm Warden",
                    MapType.CAVE,
                    250,
                    1.4,
                    1.05
            ),
            new StoryChapter(
                    "Chapter 5: Crown Of Thunder",
                    "Storm Oracle",
                    "Finale. A two-boss parliament controls the skyline. Lead your strike partner and finish the ring forever.",
                    BirdType.VULTURE,
                    "Final Bosses: Storm Vulture + Opium Seer",
                    MapType.CITY,
                    260,
                    1.45,
                    1.1
            )
    };

    private StoryChapter[] activeStoryChapters() {
        return switch (selectedEpisode) {
            case BAT -> batStoryChapters;
            case PELICAN -> pelicanStoryChapters;
            default -> storyChapters;
        };
    }

    private BirdType activeEpisodePlayerType() {
        return switch (selectedEpisode) {
            case BAT -> BirdType.BAT;
            case PELICAN -> BirdType.PELICAN;
            default -> BirdType.PIGEON;
        };
    }

    private String activeEpisodeTitle() {
        return switch (selectedEpisode) {
            case BAT -> BAT_EPISODE_TITLE;
            case PELICAN -> PELICAN_EPISODE_TITLE;
            default -> PIGEON_EPISODE_TITLE;
        };
    }

    private String activeEpisodeAward() {
        return switch (selectedEpisode) {
            case BAT -> BAT_EPISODE_AWARD;
            case PELICAN -> PELICAN_EPISODE_AWARD;
            default -> PIGEON_EPISODE_AWARD;
        };
    }

    private int getEpisodeUnlocked(EpisodeType ep) {
        return switch (ep) {
            case BAT -> batEpisodeUnlockedChapters;
            case PELICAN -> pelicanEpisodeUnlockedChapters;
            default -> pigeonEpisodeUnlockedChapters;
        };
    }

    private boolean isEpisodeCompleted(EpisodeType ep) {
        return switch (ep) {
            case BAT -> batEpisodeCompleted;
            case PELICAN -> pelicanEpisodeCompleted;
            default -> pigeonEpisodeCompleted;
        };
    }

    private void setEpisodeUnlocked(EpisodeType ep, int value) {
        if (ep == EpisodeType.BAT) batEpisodeUnlockedChapters = value;
        else if (ep == EpisodeType.PELICAN) pelicanEpisodeUnlockedChapters = value;
        else pigeonEpisodeUnlockedChapters = value;
    }

    private void setEpisodeCompleted(EpisodeType ep, boolean completed) {
        if (ep == EpisodeType.BAT) batEpisodeCompleted = completed;
        else if (ep == EpisodeType.PELICAN) pelicanEpisodeCompleted = completed;
        else pigeonEpisodeCompleted = completed;
    }

    private boolean isEpisodeRewardUnlocked(EpisodeType ep) {
        return switch (ep) {
            case BAT -> batUnlocked;
            case PELICAN -> eagleSkinUnlocked;
            default -> noirPigeonUnlocked;
        };
    }

    private String episodeBriefing(EpisodeType ep) {
        return switch (ep) {
            case BAT -> "You are the Bat. Use cave ceilings, sonar, and air control to dominate each chapter.";
            case PELICAN ->
                    "You are the Pelican. Expect scripted chaos: tag-team rounds, survival swarms, and multi-boss fights.";
            default -> "You are the Pigeon. Keep pushing through each chapter. Win to unlock the next fight.";
        };
    }

    private StoryChapter[] chaptersForEpisode(EpisodeType ep) {
        return switch (ep) {
            case BAT -> batStoryChapters;
            case PELICAN -> pelicanStoryChapters;
            default -> storyChapters;
        };
    }

    private boolean isBirdUnlocked(BirdType type) {
        return type != BirdType.BAT || batUnlocked;
    }

    private BirdType firstUnlockedBird() {
        for (BirdType bt : BirdType.values()) {
            if (isBirdUnlocked(bt)) return bt;
        }
        return BirdType.PIGEON;
    }

    public void unlockAchievement(int index, String title) {
        achievementsUnlocked[index] = true;
        addToKillFeed("ACHIEVEMENT UNLOCKED: " + title);
        shakeIntensity = 30;
        hitstopFrames = 20;
        saveAchievements(); // save immediately!
    }

    public enum BirdType {
        PIGEON("Pigeon", 7, 16, 3.9, Color.LIGHTGRAY, 0.80, "Double Jump + Heal Burst + Light Glide"),
        EAGLE("Eagle", 9, 19, 4.2, Color.DARKRED, 0.6, "Soar (Hold Jump) + Dive Bomb"),
        HUMMINGBIRD("Hummingbird", 6, 23, 5.0, Color.LIME, 0.85, "Hover/Fly + Flutter Boost"),
        TURKEY("Turkey", 10, 10, 3.0, Color.SADDLEBROWN, 0.82, "Ground Pound AOE + Heavy Flap"),
        PENGUIN("Penguin", 8, 9, 3.6, Color.BLACK, 0.0, "Ice Jump Dash"),
        SHOEBILL("Shoebill", 10, 12, 3.7, Color.DARKSLATEBLUE, 0.3, "AoE Stun"),
        MOCKINGBIRD("Charles", 5, 18, 4.0, Color.MEDIUMPURPLE, 0.4, "Spawn Lounge (Heal zone)"),
        RAZORBILL("Razorbill", 8, 12, 3.6, Color.INDIGO, 0.25, "Razor Dive + Blade Storm"),
        GRINCHHAWK("Grinch-Hawk", 10, 10, 2.8, Color.rgb(102, 153, 0), 0.80, "Steal HP from everyone + Heavy Flap"),
        VULTURE("Vulture", 7, 14, 3.1, Color.rgb(45, 25, 55), 0.2, "Summon Crows + Feast"),
        OPIUMBIRD("Opium Bird", 7, 19, 4.4, Color.rgb(138, 43, 226), 0.7, "Lean Cloud (DoT + Slow)"),
        TITMOUSE("Tufted Titmouse", 6, 21, 5.4, Color.SLATEGRAY, 0.9, "Zip Dash"),
        BAT("Bat", 7, 14, 3.7, Color.rgb(55, 35, 85), 0.65, "Sonar Screech + Ceiling Hang"),
        PELICAN("Pelican", 11, 9, 2.9, Color.rgb(245, 220, 180), 0.84, "Pelican Plunge + Glide");

        final String name;
        int power, jumpHeight;
        final double speed, flyUpForce;
        final Color color;
        final String ability;

        BirdType(String name, int power, int jumpHeight, double speed, Color color, double flyUpForce, String ability) {
            this.name = name;
            this.power = power;
            this.jumpHeight = jumpHeight;
            this.speed = speed;
            this.color = color;
            this.flyUpForce = flyUpForce;
            this.ability = ability;
        }
    }

    private void gameTick(double speed) {
        if (lastUpdate == 0) {
            lastUpdate = System.nanoTime();
            return;
        }

        if (hitstopFrames > 0) {
            hitstopFrames--;
            return;
        }

        final long FRAME_TIME = 16_666_666L;   // exactly 60 FPS
        long elapsed = System.nanoTime() - lastUpdate;
        lastUpdate = System.nanoTime();
        accumulator += elapsed;

        final long MAX_UPDATES = 6;
        int updates = 0;

        while (accumulator >= FRAME_TIME && updates < MAX_UPDATES) {
            // === CORE GAME LOGIC (runs at fixed 60 FPS) ===
            for (int i = 0; i < activePlayers; i++) {
                if (players[i] != null) {
                    players[i].update(speed);
                }
            }

            long now = System.nanoTime();
            applyMatchModeRuntimeEffects(now);
            spawnPowerUp(now);
            if (!matchEnded) matchTimer--;

            accumulator -= FRAME_TIME;
            updates++;
        }
        for (Bird b : players) {
            if (b == null || b.health <= 0) continue;
            for (NectarNode node : nectarNodes) {
                if (node.active && Math.hypot(node.x - (b.x + 40), node.y - (b.y + 40)) < 80) {
                    node.active = false;
                    if (node.isSpeed) {
                        b.speedBoostTimer = 300;
                        b.speedMultiplier = 1.2;
                        addToKillFeed(b.name.split(":")[0].trim() + " sipped SPEED NECTAR! +20% speed");
                    } else {
                        b.hoverRegenTimer = 300;
                        b.hoverRegenMultiplier = 1.5;
                        addToKillFeed(b.name.split(":")[0].trim() + " sipped HOVER NECTAR! Better hover regen");
                    }
                    for (int i = 0; i < 30; i++) {
                        double angle = Math.random() * Math.PI * 2;
                        particles.add(new Particle(node.x, node.y, Math.cos(angle) * 8, Math.sin(angle) * 8 - 4, node.isSpeed ? Color.YELLOW : Color.AQUA));
                    }
                }
            }
        }

        if (selectedMap == MapType.VIBRANT_JUNGLE) {
            for (SwingingVine vine : swingingVines) {
                vine.angularVelocity += (Math.random() - 0.5) * 0.0015;
                if (Math.random() < 0.008) vine.angularVelocity += (Math.random() - 0.5) * 0.04;
                double gravity = 0.009;
                double angularAccel = -gravity * Math.sin(vine.angle);
                vine.angularVelocity += angularAccel;
                vine.angularVelocity *= 0.995;
                vine.angle += vine.angularVelocity;
                vine.angle = Math.max(-Math.PI / 2.2, Math.min(Math.PI / 2.2, vine.angle));
                vine.updatePlatformPosition();
                for (Bird b : players) {
                    if (b == null || b.health <= 0) continue;
                    double birdLeft = b.x + 20;
                    double birdRight = b.x + 60 * b.sizeMultiplier;
                    double birdBottom = b.y + 80 * b.sizeMultiplier;
                    double birdTop = b.y;
                    double platLeft = vine.platformX;
                    double platRight = vine.platformX + vine.platformW;
                    double platTop = vine.platformY;
                    boolean horizontallyOver = birdLeft < platRight && birdRight > platLeft;
                    boolean verticallyOnTop = birdBottom >= platTop && birdBottom <= platTop + 30;
                    if (!b.onVine && horizontallyOver && verticallyOnTop && b.vy >= 0 && birdTop < platTop) {
                        b.onVine = true;
                        b.attachedVine = vine;
                        b.y = platTop - 80 * b.sizeMultiplier;
                        b.vy = 0;
                        addToKillFeed(b.name.split(":")[0].trim() + " grabbed a vine!");
                    }
                    if (b.onVine && b.attachedVine == vine) {
                        b.x = vine.platformX + vine.platformW / 2 - 40 * b.sizeMultiplier;
                        b.y = vine.platformY - 80 * b.sizeMultiplier;
                        if (!isAI[b.playerIndex]) {
                            if (pressedKeys.contains(b.leftKey())) vine.angularVelocity -= 0.004;
                            if (pressedKeys.contains(b.rightKey())) vine.angularVelocity += 0.004;
                        }
                        if (isAI[b.playerIndex]) {
                            Bird target = null;
                            double best = Double.MAX_VALUE;
                            for (Bird other : players) {
                                if (other != null && other.health > 0 && other != b) {
                                    double d = Math.abs(other.x - b.x);
                                    if (d < best) {
                                        best = d;
                                        target = other;
                                    }
                                }
                            }
                            if (target != null) {
                                if (target.x < b.x) vine.angularVelocity -= 0.006;
                                if (target.x > b.x) vine.angularVelocity += 0.006;
                            }
                        }
                        if (pressedKeys.contains(b.jumpKey())) {
                            b.onVine = false;
                            b.attachedVine = null;
                            b.vy = -b.type.jumpHeight * 0.9;
                            b.canDoubleJump = true;
                            addToKillFeed(b.name.split(":")[0].trim() + " swung off the vine!");
                        }
                        b.vy = 0;
                    }
                }
            }
            for (Bird b : players) {
                if (b != null && b.onVine && b.attachedVine != null) {
                    double platY = b.attachedVine.platformY;
                    if (b.y + 80 * b.sizeMultiplier > platY + 50) {
                        b.onVine = false;
                        b.attachedVine = null;
                    }
                }
            }
        }

        for (Iterator<CrowMinion> it = crowMinions.iterator(); it.hasNext(); ) {
            CrowMinion c = it.next();
            c.age++;
            Bird closest = null;
            double best = Double.MAX_VALUE;
            for (Bird b : players) {
                if (b == null || b.health <= 0) continue;
                if (c.owner != null && !canDamage(c.owner, b)) continue;
                double d = Math.hypot(b.x + 40 - c.x, b.y + 40 - c.y);
                if (d < best) {
                    best = d;
                    closest = b;
                }
            }
            if (closest != null) {
                double dx = closest.x + 40 - c.x;
                double dy = closest.y + 40 - c.y;
                double dist = Math.hypot(dx, dy);
                if (dist > 0) {
                    double spd = 3.2;
                    c.vx += dx / dist * 0.22;
                    c.vy += dy / dist * 0.22;
                    double speedNow = Math.hypot(c.vx, c.vy);
                    if (speedNow > spd) {
                        c.vx = c.vx / speedNow * spd;
                        c.vy = c.vy / speedNow * spd;
                    }
                }
                if (dist < 48) {
                    int damage = (c.owner != null) ? 1 : 4;
                    closest.health -= damage;
                    if (closest.health < 0) closest.health = 0;
                    String source = (c.owner != null) ? "CROW" : "MURDER CROW";
                    addToKillFeed(source + " devours " + closest.name.split(":")[0].trim() + "! -" + damage + " HP");
                    closest.vx += c.vx * 1.5;
                    closest.vy -= 12;
                    int particleCount = (c.owner != null) ? 18 : 35;
                    Color particleColor = (c.owner != null) ? Color.DARKRED.deriveColor(0, 1, 1, 0.9) : Color.BLACK;
                    for (int i = 0; i < particleCount; i++) {
                        double angle = Math.random() * Math.PI * 2;
                        double spd = 6 + Math.random() * 20;
                        particles.add(new Particle(c.x, c.y, Math.cos(angle) * spd, Math.sin(angle) * spd - 8, particleColor));
                    }
                    if (c.owner == null) {
                        triggerFlash(0.6, false);
                        shakeIntensity = Math.max(shakeIntensity, 20);
                    }
                    it.remove();
                    continue;
                }
            }
            c.x += c.vx;
            c.y += c.vy;
            if (c.age > 1800 || c.x < -1500 || c.x > WORLD_WIDTH + 1500 || c.y < -1500 || c.y > WORLD_HEIGHT + 1500) {
                it.remove();
            }
        }

        if (isTrialMode && !matchEnded) {
            boolean bossAlive = players[3] != null && players[3].health > 0;
            int allyAlive = 0;
            Bird allyWinner = null;
            for (int i = 0; i < 3; i++) {
                if (players[i] != null && players[i].health > 0) {
                    allyAlive++;
                    if (allyWinner == null) allyWinner = players[i];
                }
            }

            if (!bossAlive) {
                matchEnded = true;
                if (selectedMap == MapType.CITY) cityPigeonUnlocked = true;
                if (selectedMap == MapType.SKYCLIFFS) eagleSkinUnlocked = true;
                saveAchievements();
                addToKillFeed("TRIAL COMPLETE! Boss defeated!");
                recordBalanceOutcome(allyWinner);
            } else if (allyAlive == 0 || matchTimer <= 0) {
                matchEnded = true;
                addToKillFeed("TRIAL FAILED! The boss survives.");
                recordBalanceOutcome(players[3]);
            }
        }

        if (competitionModeEnabled && !storyModeActive && !isTrialMode && !classicModeActive && !matchEnded && matchTimer <= 0) {
            Bird timeoutWinner = findTimeoutWinner();
            addToKillFeed("TIME! Tournament decision.");
            triggerMatchEnd(timeoutWinner);
        } else if (!isTrialMode && !matchEnded && matchTimer <= 0 && !suddenDeath.isActive()) {
            suddenDeath.start();
            if (hugewaveClip != null) hugewaveClip.play();
            addToKillFeed("SUDDEN DEATH! A MURDER OF CROWS DESCENDS!");
            shakeIntensity = 40;
            hitstopFrames = 30;
        }
        if (!(competitionModeEnabled && !storyModeActive && !isTrialMode && !classicModeActive)) {
            shakeIntensity = suddenDeath.updateAndSpawn(
                    crowMinions,
                    random,
                    WORLD_WIDTH,
                    WORLD_HEIGHT,
                    shakeIntensity,
                    matchEnded
            );
        }

        if (!(competitionModeEnabled && !storyModeActive && !isTrialMode && !classicModeActive)
                && selectedMap == MapType.CITY
                && System.nanoTime() - lastWindBurstTime > WIND_BURST_INTERVAL) {
            lastWindBurstTime = System.nanoTime();
            Collections.shuffle(windVents);
            int bursts = 2 + random.nextInt(2);
            for (int i = 0; i < Math.min(bursts, windVents.size()); i++) {
                WindVent vent = windVents.get(i);
                vent.cooldown = 120;
                for (Bird b : players) {
                    if (b != null && b.health > 0) {
                        double dx = b.x + 40 - (vent.x + vent.w / 2);
                        if (Math.abs(dx) < vent.w / 2 + 100 && b.y > vent.y - 300) {
                            b.vy = Math.min(b.vy, WIND_FORCE);
                            addToKillFeed("WIND GUST lifts " + b.name.split(":")[0].trim() + "!");
                            for (int j = 0; j < 40; j++) {
                                double angle = -Math.PI / 2 + (Math.random() - 0.5) * 0.8;
                                double spd = 8 + Math.random() * 12;
                                particles.add(new Particle(vent.x + vent.w / 2 + (Math.random() - 0.5) * vent.w, vent.y - 20, Math.cos(angle) * spd, Math.sin(angle) * spd, Color.CYAN.deriveColor(0, 1, 1, 0.7)));
                            }
                        }
                    }
                }
            }
        }
        for (WindVent v : windVents) if (v.cooldown > 0) v.cooldown--;

        double minX = Double.MAX_VALUE, maxX = Double.MIN_VALUE;
        double minY = Double.MAX_VALUE, maxY = Double.MIN_VALUE;
        int aliveCount = 0;
        for (Bird b : players) {
            if (b != null && b.health > 0) {
                aliveCount++;
                minX = Math.min(minX, b.x);
                maxX = Math.max(maxX, b.x + 80);
                minY = Math.min(minY, b.y);
                maxY = Math.max(maxY, b.y + 80);
            }
        }
        if (aliveCount >= 2) {
            double birdsWidth = maxX - minX;
            double birdsHeight = maxY - minY;
            double targetZoomW = WIDTH / (birdsWidth + 800);
            double targetZoomH = HEIGHT / (birdsHeight + 800);
            double targetZoom = Math.min(targetZoomW, targetZoomH);
            targetZoom = Math.max(MIN_ZOOM, Math.min(MAX_ZOOM, targetZoom));
            zoom += (targetZoom - zoom) * ZOOM_SPEED;
        } else if (aliveCount == 1) {
            zoom += (0.9 - zoom) * 0.01;
        }
        double centerX = (minX + maxX) / 2.0;
        double centerY = (minY + maxY) / 2.0;
        camX = centerX - WIDTH / (2 * zoom);
        camY = centerY - HEIGHT / (2 * zoom);
        camX = Math.max(0, Math.min(camX, WORLD_WIDTH - WIDTH / zoom));
        camY = Math.max(0, Math.min(camY, WORLD_HEIGHT - HEIGHT / zoom));

        for (Iterator<Particle> it = particles.iterator(); it.hasNext(); ) {
            Particle p = it.next();
            p.x += p.vx;
            p.y += p.vy;
            p.vy += 0.4;
            p.life--;
            if (p.life <= 0 || p.y > WORLD_HEIGHT + 200) it.remove();
        }

        int alive = 0;
        Bird winner = null;
        Set<Integer> aliveTeams = new HashSet<>();
        for (Bird b : players) {
            if (b != null && b.health > 0) {
                alive++;
                winner = b;
                aliveTeams.add(getEffectiveTeam(b.playerIndex));
            }
        }
        boolean teamModeMatch =
                (teamModeEnabled && !isTrialMode && !storyModeActive && !classicModeActive)
                        || (storyModeActive && storyTeamMode)
                        || (classicModeActive && classicTeamMode);
        boolean isMatchOver = teamModeMatch ? aliveTeams.size() <= 1 : alive <= 1;
        if (isMatchOver && !matchEnded) {
            triggerMatchEnd(winner);
        }
    }

    private void drawGame(GraphicsContext g) {
        g.clearRect(0, 0, WIDTH, HEIGHT);
        g.save();
        g.scale(zoom, zoom);
        g.translate(-camX, -camY);

        if (shakeIntensity > 0) {
            double shakeX = (Math.random() - 0.5) * shakeIntensity * 2;
            double shakeY = (Math.random() - 0.5) * shakeIntensity * 2;
            g.translate(shakeX, shakeY);
            shakeIntensity *= 0.9;
            if (shakeIntensity < 0.5) shakeIntensity = 0;
        }

        // === BACKGROUND (different per map) ===
        switch (selectedMap) {
            case FOREST -> {
                g.setFill(Color.DARKGREEN.darker());
                for (double tx : new double[]{800, 2100, 3400, 4800, 5600}) {
                    g.fillRect(tx - 300, GROUND_Y - 1200, 600, 1500);
                    g.setFill(Color.FORESTGREEN.darker());
                    g.fillOval(tx - 500, GROUND_Y - 1400, 1000, 800);
                    g.setFill(Color.DARKGREEN.darker());
                }
                g.setFill(Color.SADDLEBROWN.darker());
                g.fillRect(0, GROUND_Y, WORLD_WIDTH, WORLD_HEIGHT - GROUND_Y);
                g.setFill(Color.FORESTGREEN.darker().darker());
                g.fillRect(0, GROUND_Y, WORLD_WIDTH, 100);
                g.setFill(Color.FORESTGREEN.brighter());
                for (Platform p : platforms) {
                    if (p.y < GROUND_Y - 50) g.fillRoundRect(p.x - 20, p.y - 30, p.w + 40, 80, 60, 60);
                }
                g.setFill(Color.SADDLEBROWN.darker());
                g.setStroke(Color.SIENNA);
                g.setLineWidth(3);
                for (Platform p : platforms) {
                    g.fillRect(p.x, p.y, p.w, p.h);
                    g.strokeRect(p.x, p.y, p.w, p.h);
                }
            }
            case SKYCLIFFS -> {
                for (int i = 0; i < 600; i++) {
                    double ratio = i / 600.0;
                    Color c = Color.ORANGERED.interpolate(Color.DEEPSKYBLUE.darker(), ratio);
                    g.setFill(c.deriveColor(0, 1, 1, 0.75));
                    g.fillRect(0, i * (WORLD_HEIGHT / 600.0), WORLD_WIDTH, WORLD_HEIGHT / 600.0 + 3);
                }
                g.setFill(Color.PURPLE.darker().darker().darker());
                double[] mountainX = {0, 800, 1800, 2800, 3800, 4800, WORLD_WIDTH};
                for (int m = 0; m < mountainX.length - 1; m++) {
                    double baseY = GROUND_Y + 300;
                    double peakY = mountainPeaks[m];
                    double midX = mountainX[m] + 400 + (m % 2 == 0 ? 100 : -100);
                    g.fillPolygon(new double[]{mountainX[m], midX, mountainX[m+1]}, new double[]{baseY, peakY, baseY}, 3);
                }
                double cloudTime = System.currentTimeMillis() / 40.0;
                g.setFill(Color.WHITE.deriveColor(0, 1, 1, 0.3));
                Random cloudRandom = new Random(42);
                for (int layer = 0; layer < 4; layer++) {
                    double speed = 0.15 + layer * 0.15;
                    double offset = cloudTime * speed;
                    for (int i = 0; i < 25; i++) {
                        cloudRandom.setSeed(i + layer * 100);
                        double baseX = i * 320;
                        double cx = (baseX + offset * (layer + 1)) % (WORLD_WIDTH + 1000) - 500;
                        double cy = 50 + layer * 350 + cloudRandom.nextDouble() * 300;
                        double size = 150 + layer * 100;
                        g.fillOval(cx - size/2, cy - size/4, size, size/2);
                        g.fillOval(cx - size/3, cy - size/5, size * 0.85, size/2 * 0.85);
                        g.fillOval(cx + size/4, cy - size/6, size * 0.75, size/2 * 0.75);
                    }
                }
                g.setFill(Color.LIGHTBLUE.deriveColor(0, 1, 1, 0.25));
                g.fillRect(0, GROUND_Y - 300, WORLD_WIDTH, 500);
                g.setFill(Color.SIENNA.darker().darker());
                g.fillRect(0, GROUND_Y, WORLD_WIDTH, WORLD_HEIGHT - GROUND_Y);
                g.setFill(Color.SIENNA.darker().darker().darker());
                g.setStroke(Color.SADDLEBROWN.darker().darker());
                g.setLineWidth(10);
                for (Platform p : platforms) {
                    g.fillRoundRect(p.x + 10, p.y + 10, p.w - 20, p.h - 10, 50, 50);
                    g.strokeRoundRect(p.x + 10, p.y + 10, p.w - 20, p.h - 10, 50, 50);
                    g.setFill(Color.DARKGREEN.brighter().brighter());
                    g.fillRoundRect(p.x + 40, p.y + 5, p.w - 80, 40, 40, 40);
                    Random crackRand = new Random((long) (p.x * 1000));
                    g.setStroke(Color.BLACK);
                    g.setLineWidth(4);
                    for (int c = 0; c < 10; c++) {
                        double crackStartX = p.x + crackRand.nextDouble() * p.w;
                        double crackEndX = crackStartX + (crackRand.nextDouble() - 0.5) * 120;
                        g.strokeLine(crackStartX, p.y + 20, crackEndX, p.y + p.h - 20);
                    }
                    g.setFill(Color.DARKGREEN.darker());
                    for (int v = 0; v < 8; v++) {
                        double vineX = p.x + crackRand.nextDouble() * p.w;
                        g.fillRect(vineX, p.y + p.h, 12, 60 + crackRand.nextDouble() * 40);
                    }
                }
                g.setStroke(Color.GOLD.brighter().brighter().brighter());
                g.setLineWidth(16);
                for (Platform p : platforms) {
                    if (p.y < GROUND_Y - 1200) g.strokeRoundRect(p.x - 10, p.y - 10, p.w + 20, p.h + 20, 60, 60);
                }
                for (WindVent v : windVents) {
                    double pulse = 0.5 + 0.5 * Math.sin(System.currentTimeMillis() / 300.0 + v.x);
                    g.setFill(Color.CYAN.deriveColor(0, 1, 1 + pulse, 0.5));
                    g.fillOval(v.x + v.w/2 - 100, v.y - 150, 200, 300);
                }
            }
            case VIBRANT_JUNGLE -> {
                for (int i = 0; i < 600; i++) {
                    double ratio = i / 600.0;
                    Color c = Color.TEAL.interpolate(Color.LIMEGREEN.darker(), ratio);
                    g.setFill(c.deriveColor(0, 1, 1, 0.85));
                    g.fillRect(0, i * (WORLD_HEIGHT / 600.0), WORLD_WIDTH, WORLD_HEIGHT / 600.0 + 3);
                }
                g.setFill(Color.SADDLEBROWN.darker().darker());
                g.setStroke(Color.SIENNA.darker());
                g.setLineWidth(20);
                double[] treeX = {600, 1600, 2600, 3600, 4600, 5400};
                for (double tx : treeX) {
                    g.fillRect(tx - 100, GROUND_Y - 2100, 200, 2200);
                    g.strokeRect(tx - 100, GROUND_Y - 2100, 200, 2200);
                    g.setFill(Color.SADDLEBROWN.darker());
                    g.fillOval(tx - 180, GROUND_Y - 100, 360, 200);
                    g.setStroke(Color.SIENNA.darker().darker());
                    g.setLineWidth(6);
                    for (int i = 0; i < 15; i++) {
                        double offset = (i % 2 == 0 ? 30 : -30);
                        g.strokeLine(tx - 80 + offset, GROUND_Y - 2000, tx - 80 + offset, GROUND_Y - 100);
                    }
                    g.setFill(Color.DARKGREEN.darker().darker());
                    g.fillOval(tx - 300, GROUND_Y - 2150, 600, 300);
                }
                g.setFill(Color.DARKGREEN.darker().darker());
                double[] mountainX = {0, 800, 1800, 2800, 3800, 4800, WORLD_WIDTH};
                for (int m = 0; m < mountainX.length - 1; m++) {
                    double baseY = GROUND_Y + 500;
                    double peakY = mountainPeaks[m];
                    double midX = mountainX[m] + 400;
                    g.fillPolygon(new double[]{mountainX[m], midX, mountainX[m+1]}, new double[]{baseY, peakY, baseY}, 3);
                }
                g.setFill(Color.WHITE.deriveColor(0, 1, 1, 0.25));
                g.fillRect(800, GROUND_Y - 1300, 1000, 1500);
                g.fillRect(3200, GROUND_Y - 1400, 900, 1600);
                g.fillRect(5000, GROUND_Y - 1200, 1100, 1400);
                g.setFill(Color.DARKOLIVEGREEN.darker());
                g.fillRect(0, GROUND_Y, WORLD_WIDTH, WORLD_HEIGHT - GROUND_Y);
                g.setFill(Color.SEAGREEN.darker());
                for (Platform p : platforms) {
                    g.fillRoundRect(p.x, p.y, p.w, p.h, 40, 40);
                    g.setFill(Color.CYAN.deriveColor(0, 1, 1, 0.5));
                    g.fillOval(p.x + p.w/2 - 30, p.y - 30, 60, 60);
                }
                g.setStroke(Color.SADDLEBROWN.darker().darker());
                g.setLineWidth(16);
                for (SwingingVine vine : swingingVines) {
                    double ropeEndX = vine.platformX + vine.platformW / 2;
                    double ropeEndY = vine.platformY + 20;
                    g.strokeLine(vine.baseX, vine.baseY, ropeEndX, ropeEndY);
                    int flowerCount = 5;
                    for (int i = 1; i <= flowerCount; i++) {
                        double t = i / (double)(flowerCount + 1);
                        double fx = vine.baseX + t * (ropeEndX - vine.baseX);
                        double fy = vine.baseY + t * (ropeEndY - vine.baseY);
                        g.setFill(Color.MAGENTA.brighter());
                        g.setEffect(new Glow(0.6));
                        for (int p = 0; p < 5; p++) {
                            double petalAngle = p * Math.PI * 2 / 5;
                            double px = fx + Math.cos(petalAngle) * 18;
                            double py = fy + Math.sin(petalAngle) * 18;
                            g.fillOval(px - 12, py - 12, 24, 24);
                        }
                        g.setEffect(null);
                        g.setFill(Color.GOLD);
                        g.fillOval(fx - 8, fy - 8, 16, 16);
                        g.setFill(Color.WHITE);
                        g.fillOval(fx - 4, fy - 6, 6, 6);
                    }
                    g.setFill(Color.SEAGREEN.darker());
                    g.fillRoundRect(vine.platformX, vine.platformY, vine.platformW, vine.platformH, 30, 30);
                    g.setStroke(Color.DARKGREEN);
                    g.setLineWidth(4);
                    g.strokeRoundRect(vine.platformX, vine.platformY, vine.platformW, vine.platformH, 30, 30);
                }
                for (NectarNode node : nectarNodes) {
                    if (node.active) {
                        Color orb = node.isSpeed ? Color.GOLD.brighter() : Color.AQUA.brighter();
                        g.setFill(orb);
                        g.fillOval(node.x - 20, node.y - 20, 40, 40);
                        g.setFill(Color.WHITE.deriveColor(0, 1, 1, 0.6));
                        g.fillOval(node.x - 10, node.y - 10, 20, 20);
                    }
                }
                for (WindVent v : windVents) {
                    g.setFill(Color.CYAN.deriveColor(0, 1, 1, 0.25));
                    g.fillOval(v.x + v.w/2 - 120, v.y - 200, 240, 400);
                }
            }
            case CAVE -> {
                // Deep cave gradient
                for (int i = 0; i < 700; i++) {
                    double ratio = i / 700.0;
                    Color c = Color.rgb(10, 12, 24).interpolate(Color.rgb(32, 22, 50), ratio);
                    g.setFill(c);
                    g.fillRect(0, i * (WORLD_HEIGHT / 700.0), WORLD_WIDTH, WORLD_HEIGHT / 700.0 + 3);
                }

                // Cave silhouette layers
                g.setFill(Color.rgb(18, 14, 30));
                for (int i = 0; i < 20; i++) {
                    double bx = i * 320;
                    g.fillPolygon(
                            new double[]{bx, bx + 160, bx + 320},
                            new double[]{0, 160 + (i % 3) * 40, 0},
                            3
                    );
                    g.fillPolygon(
                            new double[]{bx, bx + 160, bx + 320},
                            new double[]{GROUND_Y + 250, GROUND_Y + 60 + (i % 4) * 50, GROUND_Y + 250},
                            3
                    );
                }

                // Back-layer stalactites/stalagmites
                g.setFill(Color.rgb(24, 18, 38));
                for (int i = 0; i < 28; i++) {
                    double bx = i * 230 + (i % 2) * 70;
                    double topLen = 120 + (i % 5) * 35;
                    g.fillPolygon(new double[]{bx, bx + 44, bx + 88}, new double[]{0, topLen, 0}, 3);
                    double bottomLen = 90 + (i % 4) * 42;
                    g.fillPolygon(new double[]{bx + 30, bx + 74, bx + 118},
                            new double[]{GROUND_Y + 240, GROUND_Y + 240 - bottomLen, GROUND_Y + 240}, 3);
                }

                // Distant bat silhouettes for atmosphere (animated, non-linear scatter)
                Random batRand = new Random(9042);
                double batTime = System.currentTimeMillis() / 700.0;
                for (int i = 0; i < 32; i++) {
                    double bx = 120 + batRand.nextDouble() * (WORLD_WIDTH - 240);
                    double byBase = 120 + batRand.nextDouble() * (GROUND_Y - 520);
                    double speed = 0.5 + batRand.nextDouble() * 1.4;
                    double phase = batRand.nextDouble() * Math.PI * 2;
                    double ampX = 35 + batRand.nextDouble() * 90;
                    double ampY = 10 + batRand.nextDouble() * 35;
                    double bxAnim = bx + Math.sin(batTime * speed + phase) * ampX;
                    double by = byBase + Math.cos(batTime * speed * 1.3 + phase) * ampY;
                    double flap = 0.7 + 0.3 * Math.sin(batTime * 5.0 * speed + phase);
                    g.setFill(Color.rgb(8, 8, 16, 0.42 + 0.16 * flap));
                    g.fillOval(bxAnim - 15, by, 30, 15);
                    g.fillOval(bxAnim - (44 * flap), by - 5, 32 * flap + 6, 18);
                    g.fillOval(bxAnim + 12, by - 5, 32 * flap + 6, 18);
                }

                // Glowing crystals/mushrooms for unique cave identity (scattered, non-linear)
                Random crystalRand = new Random(6117);
                for (int i = 0; i < 46; i++) {
                    double cx = 60 + crystalRand.nextDouble() * (WORLD_WIDTH - 120);
                    double cy = GROUND_Y - 70 - crystalRand.nextDouble() * 260;
                    Color glow = crystalRand.nextBoolean() ? Color.CYAN : Color.MEDIUMPURPLE;
                    g.setFill(glow.deriveColor(0, 1, 1, 0.25));
                    g.fillOval(cx - 50, cy - 50, 100, 100);
                    g.setFill(glow.brighter());
                    g.fillPolygon(
                            new double[]{cx - 14, cx, cx + 14},
                            new double[]{cy + 20, cy - 22, cy + 20},
                            3
                    );
                    if (i % 3 == 0) {
                        g.setFill(Color.web("#66ffe0", 0.55));
                        g.fillOval(cx - 9, cy - 34, 18, 12);
                    }
                }

                // Solid rock floor
                g.setFill(Color.rgb(35, 30, 46));
                g.fillRect(0, GROUND_Y, WORLD_WIDTH, WORLD_HEIGHT - GROUND_Y);

                // Platforms as rocky shelves
                g.setFill(Color.rgb(82, 76, 96));
                g.setStroke(Color.rgb(130, 118, 156));
                g.setLineWidth(4);
                for (Platform p : platforms) {
                    g.fillRoundRect(p.x, p.y, p.w, p.h, 20, 20);
                    g.strokeRoundRect(p.x, p.y, p.w, p.h, 20, 20);
                    if (p.y < GROUND_Y - 120) {
                        g.setFill(Color.CYAN.deriveColor(0, 1, 1, 0.2));
                        g.fillOval(p.x + p.w * 0.5 - 18, p.y - 18, 36, 24);
                        // Hanging crystals from undersides
                        g.setFill(Color.MEDIUMPURPLE.deriveColor(0, 1, 1, 0.4));
                        double c1 = p.x + p.w * 0.28;
                        double c2 = p.x + p.w * 0.72;
                        g.fillPolygon(new double[]{c1 - 7, c1, c1 + 7}, new double[]{p.y + p.h, p.y + p.h + 24, p.y + p.h}, 3);
                        g.fillPolygon(new double[]{c2 - 7, c2, c2 + 7}, new double[]{p.y + p.h, p.y + p.h + 24, p.y + p.h}, 3);
                        g.setFill(Color.rgb(82, 76, 96));
                    }
                }

                // Echo draft vents
                for (WindVent v : windVents) {
                    double pulse = 0.5 + 0.5 * Math.sin(System.currentTimeMillis() / 260.0 + v.x * 0.02);
                    g.setFill(Color.CYAN.deriveColor(0, 1, 1, 0.22 + 0.18 * pulse));
                    g.fillOval(v.x + v.w / 2 - 120, v.y - 210, 240, 420);
                }

                // Low cave fog
                g.setFill(Color.rgb(120, 100, 180, 0.12));
                g.fillRect(0, GROUND_Y - 140, WORLD_WIDTH, 220);
            }
            case CITY -> {
                g.setFill(Color.MIDNIGHTBLUE.darker());
                g.fillRect(0, 0, WORLD_WIDTH, WORLD_HEIGHT);
                g.setFill(Color.WHITE);
                for (double[] star : cityStars) {
                    g.fillOval(star[0] - 1, star[1] - 1, 2, 2);
                }
                g.setFill(Color.rgb(15, 15, 30));
                double[] farX = {100, 900, 1700, 2700, 3700, 4700, 5500};
                for (double fx : farX) {
                    g.fillRect(fx - 200, GROUND_Y - 1400, 400, 1400);
                    g.fillRect(fx - 150, GROUND_Y - 1600, 300, 200);
                }
                double pulse = 0.5 + 0.5 * Math.sin(System.currentTimeMillis() / 300.0);
                g.setFill(Color.rgb(255, 100, 200, 0.3 + 0.4 * pulse));
                for (double fx : farX) {
                    for (int row = 0; row < 6; row++) {
                        for (int col = 0; col < 2; col++) {
                            double wx = fx - 150 + col * 200;
                            double wy = GROUND_Y - 1200 + row * 180;
                            g.fillRect(wx, wy, 60, 80);
                        }
                    }
                }
                g.setFill(Color.rgb(30, 30, 50));
                g.fillRect(0, GROUND_Y, WORLD_WIDTH, WORLD_HEIGHT - GROUND_Y);
                g.setFill(Color.rgb(65, 65, 85));
                g.setStroke(Color.CYAN.brighter());
                g.setLineWidth(4);
                for (Platform p : platforms) {
                    g.fillRect(p.x, p.y, p.w, p.h);
                    g.strokeRect(p.x, p.y, p.w, p.h);
                }
                g.setStroke(Color.MAGENTA.brighter());
                g.setLineWidth(2);
                for (Platform p : platforms) {
                    if (p.y < GROUND_Y - 400) g.strokeRect(p.x - 8, p.y - 8, p.w + 16, p.h + 16);
                }
                g.setFont(Font.font("Arial Black", FontWeight.BOLD, 32));
                g.setEffect(new Glow(1.0));
                double time = System.currentTimeMillis();
                for (Platform p : platforms) {
                    if (p.signText != null) {
                        double neonPulse = 0.7 + 0.3 * Math.sin(time / 200.0 + p.x * 0.01);
                        Color signColor = (p.x % 800 < 400) ? Color.MAGENTA.brighter() : Color.CYAN.brighter();
                        g.setFill(signColor.deriveColor(0, 1, 1, neonPulse));
                        double textWidth = p.signText.length() * 18;
                        g.fillText(p.signText, p.x + p.w / 2 - textWidth / 2, p.y - 20);
                    }
                }
                g.setEffect(null);
                g.setFill(Color.rgb(90, 90, 110));
                for (Platform p : platforms) {
                    if (p.w > 300 && p.y < GROUND_Y - 200) {
                        g.fillRect(p.x + p.w * 0.25, p.y - 45, 100, 45);
                        g.fillRect(p.x + p.w * 0.65, p.y - 45, 100, 45);
                        g.setFill(Color.rgb(50, 50, 70));
                        g.fillOval(p.x + p.w * 0.25 + 30, p.y - 35, 40, 25);
                        g.fillOval(p.x + p.w * 0.65 + 30, p.y - 35, 40, 25);
                    }
                }
            }
        }

        for (Iterator<Particle> it = particles.iterator(); it.hasNext(); ) {
            Particle p = it.next();
            if (p.life <= 0 || p.y > WORLD_HEIGHT + 200) {
                it.remove();
                continue;
            }
            g.setFill(p.color.deriveColor(0, 1, 1, p.life / 60.0));
            g.fillOval(p.x - 4, p.y - 4, 8, 8);
        }
        if (particles.size() > 3000) {
            particles.subList(0, particles.size() - 2500).clear();  // â† changed from 2000 to keep more but cap at ~500 active
        }

        for (PowerUp p : powerUps) {
            p.floatOffset += 0.1;
            double offset = Math.sin(p.floatOffset) * 10;
            g.setFill(p.type.color);
            g.fillOval(p.x - 30, p.y - 30 + offset, 60, 60);
            g.setFill(Color.WHITE);
            g.setFont(Font.font("Arial Black", 18));
            g.fillText(p.type.text, p.x - 28, p.y + 8 + offset);
        }

        for (CrowMinion c : crowMinions) {
            boolean isMurderCrow = (c.owner == null);
            double pulseVal = 1.0 + 0.3 * Math.sin(c.age * 0.4);
            double scale = isMurderCrow ? pulseVal * 1.4 : pulseVal;
            g.setFill(isMurderCrow ? Color.rgb(20, 0, 0) : Color.BLACK);
            g.fillOval(c.x - 12 * scale, c.y - 10 * scale, 24 * scale, 20 * scale);
            g.setFill(isMurderCrow ? Color.rgb(40, 0, 0) : Color.rgb(30, 30, 40));
            g.fillOval(c.x - 25 * scale, c.y - 8 * scale, 20 * scale, 25 * scale);
            g.fillOval(c.x + 5 * scale, c.y - 8 * scale, 20 * scale, 25 * scale);
            g.setFill(isMurderCrow ? Color.RED : Color.RED.brighter());
            g.fillOval(c.x - 4 * scale, c.y - 5 * scale, 8 * scale, 8 * scale);
            g.fillOval(c.x + 10 * scale, c.y - 5 * scale, 8 * scale, 8 * scale);
        }

        for (Bird b : players) {
            if (b != null && b.health > 0) b.draw(g);
        }

        g.restore();
    }

    @Override
    public void start(Stage stage) {
        stage.setTitle("Bird Fight 3 - Power-Up Chaos!");
        stage.setResizable(true);
        stage.centerOnScreen();
        stage.setMaximized(true);
        stage.setFullScreenExitKeyCombination(KeyCombination.NO_MATCH);
        stage.setFullScreenExitHint("");
        loadAchievements();
        loadSounds();
        showMenu(stage);
        stage.setFullScreen(true);
    }

    private void showMenu(Stage stage) {
        storyModeActive = false;
        storyReplayMode = false;
        classicModeActive = false;
        classicEncounter = null;
        classicRun.clear();
        classicRoundIndex = 0;
        classicTeamMode = false;
        Arrays.fill(classicTeams, 1);
        competitionSeriesActive = false;
        competitionDraftComplete = false;
        competitionDraftSummary = "";
        competitionBannedBirds.clear();
        Arrays.fill(competitionRoundWins, 0);
        Arrays.fill(competitionTeamWins, 0);
        competitionRoundNumber = 1;
        if (musicPlayer != null) musicPlayer.stop();
        if (victoryMusicPlayer != null) victoryMusicPlayer.stop();
        playMenuMusic();
        VBox root = new VBox(30);
        root.setAlignment(Pos.CENTER);
        root.setPadding(new Insets(40));
        root.setStyle("-fx-background-color: linear-gradient(to bottom, #87CEEB, #98D8E9);");

        Label title = new Label("BIRD FIGHT 3");
        title.setFont(Font.font("Arial Black", 100));
        title.setTextFill(Color.WHITE);

        HBox countBox = new HBox(26);
        countBox.setAlignment(Pos.CENTER);

        for (int i = 1; i <= 4; i++) {
            int n = i;
            Button b = uiFactory.action(n + "P", 170, 90, 40, "#4CAF50", 30, () -> {
                activePlayers = n;
                for (int j = 0; j < 4; j++) {
                    boolean show = j < n;
                    playerSlots[j].setVisible(show);
                    playerSlots[j].setManaged(show);
                }
                refreshTeamButtons();
            });
            countBox.getChildren().add(b);
        }

        teamModeToggleButton = uiFactory.action("TEAM MODE: OFF", 420, 90, 34, "#546E7A", 30, () -> {
            teamModeEnabled = !teamModeEnabled;
            refreshTeamToggleButton();
            refreshTeamButtons();
        });

        HBox setupBox = new HBox(24, countBox, teamModeToggleButton);
        setupBox.setAlignment(Pos.CENTER);

        // === CLASSIC / ACHIEVEMENTS ===
        HBox menuButtons = new HBox(40);
        menuButtons.setAlignment(Pos.CENTER);

        Button classicBtn = uiFactory.action("CLASSIC MODE", 520, 100, 44, "#1565C0", 50, () -> showClassicBirdSelect(stage));
        Button achievementsBtn = uiFactory.action("ACHIEVEMENTS", 520, 100, 42, "#9C27B0", 50, () -> showAchievements(stage));
        classicBtn.setWrapText(false);
        achievementsBtn.setWrapText(false);

        menuButtons.getChildren().addAll(classicBtn, achievementsBtn);
        root.getChildren().add(menuButtons);

        playerSlots = new VBox[4];
        for (int i = 0; i < 4; i++) {
            playerSlots[i] = createPlayerSlot(i);
            boolean show = i < 2;
            playerSlots[i].setVisible(show);
            playerSlots[i].setManaged(show);
        }

        GridPane grid = new GridPane();
        grid.setHgap(80);
        grid.setVgap(50);
        grid.setAlignment(Pos.CENTER);
        grid.add(playerSlots[0], 0, 0);
        grid.add(playerSlots[1], 1, 0);
        grid.add(playerSlots[2], 0, 1);
        grid.add(playerSlots[3], 1, 1);

        Button fight = uiFactory.action("FLY INTO\nBATTLE!", 1000, 180, 68, "#FF1744", 100, () -> showStageSelect(stage));
        Button settingsBtn = uiFactory.action("GAME SETTINGS", 740, 110, 40, "#455A64", 36, () -> showGameSettings(stage));
        settingsBtn.setWrapText(false);
        uiFactory.fitSingleLineOnLayout(settingsBtn, 40, 24);

        root.getChildren().addAll(title, setupBox, grid, fight, settingsBtn);

        Runnable relayoutMenu = () -> {
            boolean twoOrLess = activePlayers <= 2;
            grid.setVgap(twoOrLess ? 0 : 50);
            root.setSpacing(twoOrLess ? 22 : 30);
        };
        relayoutMenu.run();
        for (Node node : countBox.getChildren()) {
            if (node instanceof Button btn) {
                btn.addEventHandler(javafx.event.ActionEvent.ACTION, e -> relayoutMenu.run());
            }
        }

        ScrollPane scroll = new ScrollPane(root);
        scroll.setFitToWidth(true);
        scroll.setFitToHeight(true);
        scroll.setStyle("-fx-background-color: transparent;");

        Scene scene = new Scene(scroll, WIDTH, HEIGHT);
        Runnable resizeTopButtons = () -> {
            double totalGap = 1 * 40.0;
            double available = Math.max(1080, scene.getWidth() - 180);
            double each = Math.max(420, Math.min(650, (available - totalGap) / 2.0));
            classicBtn.setPrefWidth(each);
            achievementsBtn.setPrefWidth(each);
            fitMainMenuButtonSingleLine(classicBtn, 44, 24);
            fitMainMenuButtonSingleLine(achievementsBtn, 44, 20);
        };
        scene.widthProperty().addListener((obs, oldV, newV) -> resizeTopButtons.run());
        javafx.application.Platform.runLater(resizeTopButtons);
        scene.addEventFilter(KeyEvent.KEY_PRESSED, e -> {
            if (e.getCode() == KeyCode.ESCAPE) {
                playButtonClick();
                confirmExitGame(stage);
                e.consume();
            }
        });
        scene.getRoot().setStyle(scene.getRoot().getStyle() + ";-fx-focus-color:transparent;-fx-faint-focus-color:transparent;");
        setupKeyboardNavigation(scene);
        applyConsoleHighlight(scene);
        setScenePreservingFullscreen(stage, scene);
        refreshTeamToggleButton();
        refreshTeamButtons();
        countBox.getChildren().get(1).requestFocus(); // 2 PLAYERS

        stage.show();
    }

    private void showEpisodesHub(Stage stage) {
        storyModeActive = true;
        storyReplayMode = false;
        playMenuMusic();

        BorderPane root = new BorderPane();
        root.setPadding(new Insets(30, 40, 30, 40));
        root.setStyle("-fx-background-color: linear-gradient(to bottom, #081122, #1F355E);");

        Label title = new Label("EPISODES");
        title.setFont(Font.font("Arial Black", FontWeight.BOLD, 96));
        title.setTextFill(Color.GOLD);
        BorderPane.setAlignment(title, Pos.CENTER);

        VBox pigeonCard = buildEpisodeCard(stage, EpisodeType.PIGEON, "#4FC3F7", "Select Pigeon to cycle skins when unlocked.");
        VBox batCard = buildEpisodeCard(stage, EpisodeType.BAT, "#B388FF", "Master aerial cave combat with Bat.");
        VBox pelicanCard = buildEpisodeCard(stage, EpisodeType.PELICAN, "#FFD54F", "Command the Iron Beak in scripted chaos battles.");
        VBox cards = new VBox(18, pigeonCard, batCard, pelicanCard);
        cards.setAlignment(Pos.TOP_CENTER);

        ScrollPane scroll = new ScrollPane(cards);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background: transparent; -fx-background-color: transparent;");
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);

        Button menuButton = uiFactory.action("MAIN MENU", 520, 95, 34, "#FF1744", 24, () -> showMenu(stage));
        menuButton.setWrapText(false);
        uiFactory.fitSingleLineOnLayout(menuButton, 34, 20);
        HBox bottom = new HBox(menuButton);
        bottom.setAlignment(Pos.CENTER);

        root.setTop(title);
        root.setCenter(scroll);
        root.setBottom(bottom);

        Scene scene = new Scene(root, WIDTH, HEIGHT);
        setupKeyboardNavigation(scene);
        applyConsoleHighlight(scene);
        setScenePreservingFullscreen(stage, scene);
        if (!pigeonCard.getChildren().isEmpty() && pigeonCard.getChildren().get(3) instanceof HBox h && !h.getChildren().isEmpty() && h.getChildren().get(0) instanceof Button b) {
            b.requestFocus();
        }
    }

    private void refreshSettingsToggleButton(Button btn, String prefix, boolean enabled) {
        btn.setText(prefix + ": " + (enabled ? "ON" : "OFF"));
        String color = enabled ? "#2E7D32" : "#546E7A";
        btn.setStyle("-fx-background-color: " + color + "; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 24;");
    }

    private void showGameSettings(Stage stage) {
        playMenuMusic();

        VBox root = new VBox(24);
        root.setAlignment(Pos.CENTER);
        root.setPadding(new Insets(50));
        root.setStyle("-fx-background-color: linear-gradient(to bottom, #0D1B2A, #1B263B);");

        Label title = new Label("GAME SETTINGS");
        title.setFont(Font.font("Arial Black", FontWeight.BOLD, 86));
        title.setTextFill(Color.GOLD);

        VBox card = new VBox(18);
        card.setAlignment(Pos.CENTER_LEFT);
        card.setPadding(new Insets(26));
        card.setMaxWidth(1320);
        card.setStyle("-fx-background-color: rgba(0,0,0,0.58); -fx-background-radius: 22; -fx-border-color: #64B5F6; -fx-border-width: 3; -fx-border-radius: 22;");

        Button mutatorToggle = uiFactory.action("", 620, 96, 34, "#546E7A", 24, () -> {});
        Button compToggle = uiFactory.action("", 620, 96, 34, "#546E7A", 24, () -> {});

        mutatorToggle.setOnAction(e -> {
            playButtonClick();
            mutatorModeEnabled = !mutatorModeEnabled;
            if (mutatorModeEnabled && competitionModeEnabled) {
                competitionModeEnabled = false;
                competitionSeriesActive = false;
                competitionDraftComplete = false;
                competitionDraftSummary = "";
                competitionBannedBirds.clear();
                Arrays.fill(competitionRoundWins, 0);
                Arrays.fill(competitionTeamWins, 0);
                competitionRoundNumber = 1;
            }
            refreshSettingsToggleButton(mutatorToggle, "MUTATOR MODE", mutatorModeEnabled);
            refreshSettingsToggleButton(compToggle, "COMPETITION MODE", competitionModeEnabled);
        });
        compToggle.setOnAction(e -> {
            playButtonClick();
            competitionModeEnabled = !competitionModeEnabled;
            if (competitionModeEnabled && mutatorModeEnabled) {
                mutatorModeEnabled = false;
            } else if (!competitionModeEnabled) {
                competitionSeriesActive = false;
                competitionDraftComplete = false;
                competitionDraftSummary = "";
                competitionBannedBirds.clear();
                Arrays.fill(competitionRoundWins, 0);
                Arrays.fill(competitionTeamWins, 0);
                competitionRoundNumber = 1;
            }
            refreshSettingsToggleButton(mutatorToggle, "MUTATOR MODE", mutatorModeEnabled);
            refreshSettingsToggleButton(compToggle, "COMPETITION MODE", competitionModeEnabled);
        });

        refreshSettingsToggleButton(mutatorToggle, "MUTATOR MODE", mutatorModeEnabled);
        refreshSettingsToggleButton(compToggle, "COMPETITION MODE", competitionModeEnabled);

        Label mutatorInfo = new Label(
                "Mutator Mode:\n" +
                "- Randomly activates 1 mutator each match.\n" +
                "- Variety includes: Low Gravity, Power-Up Storm, Crow Surge, Turbo Brawl."
        );
        mutatorInfo.setFont(Font.font("Consolas", 24));
        mutatorInfo.setTextFill(Color.web("#B3E5FC"));

        Label compInfo = new Label(
                "Competition Mode (Tournament Rules):\n" +
                "- No power-up spawns.\n" +
                "- Fixed 120 second round timer.\n" +
                "- No sudden-death crows or random city wind bursts.\n" +
                "- On timeout, highest-health bird wins (damage dealt as tie-breaker)."
        );
        compInfo.setFont(Font.font("Consolas", 24));
        compInfo.setTextFill(Color.web("#FFECB3"));

        card.getChildren().addAll(mutatorToggle, mutatorInfo, compToggle, compInfo);

        HBox buttons = new HBox(20);
        buttons.setAlignment(Pos.CENTER);
        Button back = uiFactory.action("BACK", 360, 100, 40, "#FF1744", 28, () -> showMenu(stage));
        Button stageSelect = uiFactory.action("GO TO STAGE SELECT", 520, 100, 34, "#1565C0", 24, () -> showStageSelect(stage));
        buttons.getChildren().addAll(back, stageSelect);

        root.getChildren().addAll(title, card, buttons);
        Scene scene = new Scene(root, WIDTH, HEIGHT);
        setupKeyboardNavigation(scene);
        applyConsoleHighlight(scene);
        setScenePreservingFullscreen(stage, scene);
        mutatorToggle.requestFocus();
    }

    private boolean isClassicCompleted(BirdType type) {
        if (type == null) return false;
        return classicCompleted[type.ordinal()];
    }

    private String classicRewardFor(BirdType type) {
        return switch (type) {
            case PIGEON -> "Pigeon Noir";
            case EAGLE -> "Sky Tyrant Eagle";
            case HUMMINGBIRD -> "Plasma Hummingbird";
            case TURKEY -> "Warpaint Turkey";
            case PENGUIN -> "Glacier Penguin";
            case SHOEBILL -> "Obsidian Shoebill";
            case MOCKINGBIRD -> "Velvet Charles";
            case RAZORBILL -> "Storm Razorbill";
            case GRINCHHAWK -> "Krampus Hawk";
            case VULTURE -> "Ashen Vulture";
            case OPIUMBIRD -> "Nightshade Opium Bird";
            case TITMOUSE -> "Volt Titmouse";
            case BAT -> "Moonlit Bat";
            case PELICAN -> "Titan Pelican";
        };
    }

    public boolean isClassicRewardUnlocked(BirdType type) {
        if (type == null) return false;
        if (type == BirdType.PIGEON) return noirPigeonUnlocked;
        if (type == BirdType.EAGLE) return eagleSkinUnlocked;
        return classicSkinUnlocked[type.ordinal()];
    }

    private void unlockClassicReward(BirdType type) {
        if (type == null) return;
        if (type == BirdType.PIGEON) {
            noirPigeonUnlocked = true;
            classicSkinUnlocked[type.ordinal()] = true;
        } else if (type == BirdType.EAGLE) {
            eagleSkinUnlocked = true;
            classicSkinUnlocked[type.ordinal()] = true;
        } else {
            classicSkinUnlocked[type.ordinal()] = true;
        }
    }

    private String classicSkinDataKey(BirdType type) {
        return "CLASSIC_SKIN_" + type.name();
    }

    public Color classicSkinPrimaryColor(BirdType type) {
        return switch (type) {
            case HUMMINGBIRD -> Color.web("#00E5FF");
            case TURKEY -> Color.web("#7B1FA2");
            case PENGUIN -> Color.web("#80DEEA");
            case SHOEBILL -> Color.web("#1E88E5");
            case MOCKINGBIRD -> Color.web("#EC407A");
            case RAZORBILL -> Color.web("#26C6DA");
            case GRINCHHAWK -> Color.web("#8BC34A");
            case VULTURE -> Color.web("#D32F2F");
            case OPIUMBIRD -> Color.web("#AB47BC");
            case TITMOUSE -> Color.web("#FFCA28");
            case BAT -> Color.web("#5E35B1");
            case PELICAN -> Color.web("#FFB74D");
            case EAGLE -> Color.GOLD;
            case PIGEON -> Color.rgb(18, 18, 18);
        };
    }

    public Color classicSkinAccentColor(BirdType type) {
        return switch (type) {
            case HUMMINGBIRD -> Color.web("#B2FF59");
            case TURKEY -> Color.web("#F06292");
            case PENGUIN -> Color.web("#E1F5FE");
            case SHOEBILL -> Color.web("#90CAF9");
            case MOCKINGBIRD -> Color.web("#F8BBD0");
            case RAZORBILL -> Color.web("#80DEEA");
            case GRINCHHAWK -> Color.web("#DCEDC8");
            case VULTURE -> Color.web("#FFCDD2");
            case OPIUMBIRD -> Color.web("#E1BEE7");
            case TITMOUSE -> Color.web("#FFF59D");
            case BAT -> Color.web("#D1C4E9");
            case PELICAN -> Color.web("#FFE0B2");
            case EAGLE -> Color.web("#FFF176");
            case PIGEON -> Color.web("#F44336");
        };
    }

    private BirdType pickClassicEnemy(BirdType playerType, Set<BirdType> used) {
        List<BirdType> pool = new ArrayList<>();
        for (BirdType bt : BirdType.values()) {
            if (bt == playerType) continue;
            if (!isBirdUnlocked(bt)) continue;
            if (!used.contains(bt)) pool.add(bt);
        }
        if (pool.isEmpty()) {
            for (BirdType bt : BirdType.values()) {
                if (bt != playerType && isBirdUnlocked(bt)) {
                    pool.add(bt);
                }
            }
        }
        if (pool.isEmpty()) return BirdType.PIGEON;
        BirdType pick = pool.get(random.nextInt(pool.size()));
        used.add(pick);
        return pick;
    }

    private MapType pickClassicMap(Set<MapType> used) {
        List<MapType> maps = new ArrayList<>(Arrays.asList(MapType.values()));
        maps.removeIf(used::contains);
        if (maps.isEmpty()) maps.addAll(Arrays.asList(MapType.values()));
        MapType map = maps.get(random.nextInt(maps.size()));
        used.add(map);
        return map;
    }

    private MatchMutator pickClassicMutator(MatchMutator... options) {
        if (options == null || options.length == 0) return MatchMutator.NONE;
        return options[random.nextInt(options.length)];
    }

    private ClassicTwist pickClassicTwist(ClassicTwist... options) {
        if (options == null || options.length == 0) return ClassicTwist.STORM_LIFTS;
        return options[random.nextInt(options.length)];
    }

    private ClassicFighter classicFighter(BirdType type, String title, double health, double powerMult, double speedMult) {
        return new ClassicFighter(type, title, health, powerMult, speedMult);
    }

    private String buildClassicRunCodename() {
        String[] a = {"Tempest", "Neon", "Iron", "Night", "Solar", "Ghost", "Abyss", "Prism"};
        String[] b = {"Circuit", "Dynasty", "Gauntlet", "Arc", "Protocol", "Crown", "Storm", "Orbit"};
        return a[random.nextInt(a.length)] + " " + b[random.nextInt(b.length)];
    }

    private List<ClassicEncounter> buildClassicRun(BirdType playerType) {
        List<ClassicEncounter> run = new ArrayList<>();
        Set<MapType> usedMaps = new HashSet<>();
        Set<BirdType> usedBirds = new HashSet<>();

        BirdType r1 = pickClassicEnemy(playerType, usedBirds);
        run.add(new ClassicEncounter(
                "Qualifier: First Impact",
                "Skycaster",
                "One rival enters. Win clean and establish tempo.",
                pickClassicMap(usedMaps),
                pickClassicMutator(MatchMutator.NONE, MatchMutator.LOW_GRAVITY),
                pickClassicTwist(ClassicTwist.STORM_LIFTS, ClassicTwist.NECTAR_BLOOM),
                85 * 60,
                new ClassicFighter[0],
                new ClassicFighter[]{
                        classicFighter(r1, "Rival: " + r1.name, 120, 1.08, 1.03)
                }
        ));

        BirdType r2a = pickClassicEnemy(playerType, usedBirds);
        BirdType r2b = pickClassicEnemy(playerType, usedBirds);
        run.add(new ClassicEncounter(
                "Ambush: Split-Lane Pressure",
                "Arena Warden",
                "Two enemies coordinate from opposite angles. One misstep snowballs.",
                pickClassicMap(usedMaps),
                pickClassicMutator(MatchMutator.POWERUP_STORM, MatchMutator.LOW_GRAVITY, MatchMutator.NONE),
                pickClassicTwist(ClassicTwist.TITAN_CACHE, ClassicTwist.SHOCK_DROPS),
                95 * 60,
                new ClassicFighter[0],
                new ClassicFighter[]{
                        classicFighter(r2a, "Pressure Wing: " + r2a.name, 96, 0.93, 0.98),
                        classicFighter(r2b, "Pressure Wing: " + r2b.name, 94, 0.92, 1.0)
                }
        ));

        BirdType ally = pickClassicEnemy(playerType, usedBirds);
        BirdType r3a = pickClassicEnemy(playerType, usedBirds);
        BirdType r3b = pickClassicEnemy(playerType, usedBirds);
        run.add(new ClassicEncounter(
                "Relay: Duo Protocol",
                "Command Relay",
                "You gain one ally. Keep formation and rotate aggro before they isolate either of you.",
                pickClassicMap(usedMaps),
                pickClassicMutator(MatchMutator.TURBO_BRAWL, MatchMutator.POWERUP_STORM),
                pickClassicTwist(ClassicTwist.NECTAR_BLOOM, ClassicTwist.STORM_LIFTS),
                95 * 60,
                new ClassicFighter[]{
                        classicFighter(ally, "Ally: " + ally.name, 108, 1.0, 1.08)
                },
                new ClassicFighter[]{
                        classicFighter(r3a, "Pair Captain: " + r3a.name, 130, 1.12, 1.04),
                        classicFighter(r3b, "Pair Captain: " + r3b.name, 124, 1.1, 1.06)
                }
        ));

        BirdType r4a = pickClassicEnemy(playerType, usedBirds);
        BirdType r4b = pickClassicEnemy(playerType, usedBirds);
        BirdType r4c = pickClassicEnemy(playerType, usedBirds);
        run.add(new ClassicEncounter(
                "Gauntlet: Three-Flock Crush",
                "Ruin Oracle",
                "Three coordinated hunters attack in waves. They are individually weaker, but relentless together.",
                pickClassicMap(usedMaps),
                pickClassicMutator(MatchMutator.CROW_SURGE, MatchMutator.TURBO_BRAWL),
                pickClassicTwist(ClassicTwist.CROW_CARNIVAL, ClassicTwist.SHOCK_DROPS),
                90 * 60,
                new ClassicFighter[0],
                new ClassicFighter[]{
                        classicFighter(r4a, "Gauntlet Wing: " + r4a.name, 92, 0.88, 0.96),
                        classicFighter(r4b, "Gauntlet Wing: " + r4b.name, 90, 0.86, 0.98),
                        classicFighter(r4c, "Gauntlet Wing: " + r4c.name, 88, 0.85, 1.0)
                }
        ));

        BirdType r5ally = pickClassicEnemy(playerType, usedBirds);
        BirdType boss = pickClassicEnemy(playerType, usedBirds);
        BirdType lieutenant = pickClassicEnemy(playerType, usedBirds);
        run.add(new ClassicEncounter(
                "Finale: Crown Circuit",
                "Skycaster Prime",
                "Final arena. A boss duo controls the map core. Break them to clear Classic for this bird.",
                pickClassicMap(usedMaps),
                pickClassicMutator(MatchMutator.TURBO_BRAWL, MatchMutator.CROW_SURGE, MatchMutator.POWERUP_STORM),
                pickClassicTwist(ClassicTwist.TITAN_CACHE, ClassicTwist.STORM_LIFTS, ClassicTwist.CROW_CARNIVAL),
                110 * 60,
                new ClassicFighter[]{
                        classicFighter(r5ally, "Ally: " + r5ally.name, 112, 1.04, 1.08)
                },
                new ClassicFighter[]{
                        classicFighter(boss, "Boss: " + boss.name, 210, 1.32, 1.08),
                        classicFighter(lieutenant, "Boss: " + lieutenant.name, 175, 1.24, 1.12)
                }
        ));

        return run;
    }

    private void showClassicBirdSelect(Stage stage) {
        storyModeActive = false;
        storyReplayMode = false;
        classicModeActive = false;
        classicEncounter = null;
        classicRun.clear();
        classicRoundIndex = 0;
        classicTeamMode = false;
        Arrays.fill(classicTeams, 1);
        playMenuMusic();

        BorderPane root = new BorderPane();
        root.setPadding(new Insets(26, 36, 26, 36));
        root.setStyle("-fx-background-color: linear-gradient(to bottom, #091B2E, #142F45);");

        Label title = new Label("CLASSIC MODE");
        title.setFont(Font.font("Arial Black", FontWeight.BOLD, 88));
        title.setTextFill(Color.GOLD);

        Label subtitle = new Label("Single-player gauntlet. Pick one bird, clear five randomized encounters, unlock its signature skin.");
        subtitle.setFont(Font.font("Consolas", 24));
        subtitle.setTextFill(Color.web("#B3E5FC"));

        VBox top = new VBox(8, title, subtitle);
        top.setAlignment(Pos.CENTER);

        GridPane grid = new GridPane();
        grid.setHgap(18);
        grid.setVgap(18);
        grid.setAlignment(Pos.CENTER);

        final BirdType[] selected = new BirdType[]{classicSelectedBird};
        List<Button> birdButtons = new ArrayList<>();

        Label selectedLabel = new Label();
        selectedLabel.setFont(Font.font("Arial Black", 44));
        selectedLabel.setTextFill(Color.WHITE);

        Label rewardLabel = new Label();
        rewardLabel.setFont(Font.font("Consolas", 30));
        rewardLabel.setTextFill(Color.GOLD);

        Label unlockLabel = new Label();
        unlockLabel.setFont(Font.font("Consolas", 26));

        Label badgeLabel = new Label();
        badgeLabel.setFont(Font.font("Consolas", 24));

        Label info = new Label("Defaults: City Pigeon and Sky Tyrant Eagle skins are already unlocked.");
        info.setFont(Font.font("Consolas", 22));
        info.setTextFill(Color.web("#FFE082"));

        Runnable refreshSelection = () -> {
            BirdType picked = selected[0];
            selectedLabel.setText("Selected: " + picked.name);
            String reward = classicRewardFor(picked);
            boolean unlocked = isClassicRewardUnlocked(picked);
            boolean completed = isClassicCompleted(picked);
            rewardLabel.setText("Reward Skin: " + reward);
            unlockLabel.setText(unlocked ? "Reward Status: UNLOCKED" : "Reward Status: LOCKED");
            unlockLabel.setTextFill(unlocked ? Color.LIMEGREEN : Color.ORANGE);
            badgeLabel.setText(completed ? "Classic Badge: EARNED" : "Classic Badge: NOT EARNED");
            badgeLabel.setTextFill(completed ? Color.web("#69F0AE") : Color.web("#FFAB91"));

            for (Button btn : birdButtons) {
                BirdType bt = (BirdType) btn.getUserData();
                String base = "-fx-background-color: " + toHex(bt.color) + "; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 16;";
                if (bt == picked) {
                    base += "-fx-border-color: #FFD54F; -fx-border-width: 4; -fx-border-radius: 16;";
                } else if (isClassicCompleted(bt)) {
                    base += "-fx-border-color: #69F0AE; -fx-border-width: 3; -fx-border-radius: 16;";
                }
                btn.setStyle(base);
            }
        };

        int col = 0;
        int row = 0;
        for (BirdType bt : BirdType.values()) {
            if (!isBirdUnlocked(bt)) continue;
            String text = bt.name + (isClassicCompleted(bt) ? "\n* CLEARED" : "");
            Button btn = new Button(text);
            btn.setPrefSize(240, 92);
            btn.setWrapText(true);
            btn.setFont(Font.font("Arial Black", 22));
            btn.setUserData(bt);
            btn.setOnAction(e -> {
                playButtonClick();
                selected[0] = bt;
                refreshSelection.run();
            });
            birdButtons.add(btn);
            grid.add(btn, col++, row);
            if (col > 3) {
                col = 0;
                row++;
            }
        }

        VBox rightCard = new VBox(14, selectedLabel, rewardLabel, unlockLabel, badgeLabel, info);
        rightCard.setAlignment(Pos.TOP_LEFT);
        rightCard.setPadding(new Insets(26));
        rightCard.setMaxWidth(760);
        rightCard.setStyle("-fx-background-color: rgba(0,0,0,0.56); -fx-border-color: #64B5F6; -fx-border-width: 3; -fx-border-radius: 22; -fx-background-radius: 22;");

        HBox center = new HBox(22, grid, rightCard);
        center.setAlignment(Pos.CENTER);

        Button start = uiFactory.action("GENERATE RUN", 480, 110, 38, "#00C853", 24, () -> showClassicRunBriefing(stage, selected[0]));
        Button back = uiFactory.action("MAIN MENU", 420, 110, 38, "#FF1744", 24, () -> showMenu(stage));
        HBox bottom = new HBox(20, start, back);
        bottom.setAlignment(Pos.CENTER);

        root.setTop(top);
        root.setCenter(center);
        root.setBottom(bottom);

        Scene scene = new Scene(root, WIDTH, HEIGHT);
        setupKeyboardNavigation(scene);
        applyConsoleHighlight(scene);
        setScenePreservingFullscreen(stage, scene);

        refreshSelection.run();
        if (!birdButtons.isEmpty()) {
            birdButtons.get(0).requestFocus();
        } else {
            back.requestFocus();
        }
    }

    private void showClassicRunBriefing(Stage stage, BirdType birdType) {
        if (birdType == null) birdType = BirdType.PIGEON;
        classicSelectedBird = birdType;
        classicRunCodename = buildClassicRunCodename();
        classicRun.clear();
        classicRun.addAll(buildClassicRun(classicSelectedBird));
        classicRoundIndex = 0;
        classicEncounter = classicRun.isEmpty() ? null : classicRun.get(0);
        playMenuMusic();

        BorderPane root = new BorderPane();
        root.setPadding(new Insets(30, 36, 30, 36));
        root.setStyle("-fx-background-color: linear-gradient(to bottom, #081122, #1D3557);");

        Label title = new Label("CLASSIC BRIEFING");
        title.setFont(Font.font("Arial Black", FontWeight.BOLD, 84));
        title.setTextFill(Color.GOLD);

        Label runInfo = new Label("Run Codename: " + classicRunCodename + "\nPilot: " + classicSelectedBird.name);
        runInfo.setFont(Font.font("Consolas", 30));
        runInfo.setTextFill(Color.WHITE);

        String reward = classicRewardFor(classicSelectedBird);
        boolean unlocked = isClassicRewardUnlocked(classicSelectedBird);
        Label rewardInfo = new Label("Reward Skin: " + reward + "  |  " + (unlocked ? "UNLOCKED" : "LOCKED"));
        rewardInfo.setFont(Font.font("Consolas", 28));
        rewardInfo.setTextFill(unlocked ? Color.LIMEGREEN : Color.ORANGE);

        VBox route = new VBox(10);
        route.setAlignment(Pos.TOP_LEFT);
        route.setPadding(new Insets(18));
        route.setStyle("-fx-background-color: rgba(0,0,0,0.58); -fx-border-color: #4FC3F7; -fx-border-width: 3; -fx-border-radius: 18; -fx-background-radius: 18;");
        for (int i = 0; i < classicRun.size(); i++) {
            ClassicEncounter enc = classicRun.get(i);
            String line = String.format(
                    "R%d - %s | %s | Mutator: %s | Twist: %s",
                    i + 1, enc.name, enc.map.name(), enc.mutator.label, enc.twist.label
            );
            Label l = new Label(line);
            l.setFont(Font.font("Consolas", 24));
            l.setTextFill(Color.web("#E3F2FD"));
            route.getChildren().add(l);
        }

        Button start = uiFactory.action("START CLASSIC RUN", 560, 110, 38, "#00C853", 24, () -> {
            playButtonClick();
            resetMatchStats();
            classicModeActive = true;
            showClassicEncounterIntro(stage);
        });
        Button back = uiFactory.action("BACK TO BIRDS", 460, 110, 38, "#FF1744", 24, () -> showClassicBirdSelect(stage));
        HBox buttons = new HBox(20, start, back);
        buttons.setAlignment(Pos.CENTER);

        VBox center = new VBox(18, runInfo, rewardInfo, route);
        center.setAlignment(Pos.TOP_CENTER);
        root.setTop(title);
        BorderPane.setAlignment(title, Pos.CENTER);
        root.setCenter(center);
        root.setBottom(buttons);

        Scene scene = new Scene(root, WIDTH, HEIGHT);
        setupKeyboardNavigation(scene);
        applyConsoleHighlight(scene);
        setScenePreservingFullscreen(stage, scene);
        start.requestFocus();
    }

    private void showClassicEncounterIntro(Stage stage) {
        if (!classicModeActive || classicRun.isEmpty()) {
            showClassicBirdSelect(stage);
            return;
        }
        if (classicRoundIndex < 0 || classicRoundIndex >= classicRun.size()) {
            classicRoundIndex = 0;
        }
        classicEncounter = classicRun.get(classicRoundIndex);
        String text = classicEncounter.briefing
                + "\n\nMap: " + classicEncounter.map.name()
                + "\nMutator: " + classicEncounter.mutator.label
                + "\nTwist: " + classicEncounter.twist.label + " - " + classicEncounter.twist.description
                + "\nRound: " + (classicRoundIndex + 1) + "/" + classicRun.size();
        showStoryDialogue(stage,
                "Classic " + (classicRoundIndex + 1) + ": " + classicEncounter.name,
                classicEncounter.announcer,
                text,
                () -> startClassicEncounter(stage));
    }

    private void startClassicEncounter(Stage stage) {
        if (!classicModeActive || classicEncounter == null) {
            showClassicBirdSelect(stage);
            return;
        }
        selectedMap = classicEncounter.map;
        startMatch(stage);
    }

    private VBox buildEpisodeCard(Stage stage, EpisodeType ep, String borderColor, String flavorText) {
        StoryChapter[] chapters = chaptersForEpisode(ep);
        int unlocked = Math.max(1, Math.min(getEpisodeUnlocked(ep), chapters.length));
        boolean completed = isEpisodeCompleted(ep);

        VBox card = new VBox(14);
        card.setAlignment(Pos.CENTER_LEFT);
        card.setPadding(new Insets(28));
        card.setMaxWidth(1480);
        card.setStyle("-fx-background-color: rgba(0,0,0,0.55); -fx-background-radius: 24; -fx-border-color: " + borderColor + "; -fx-border-width: 4; -fx-border-radius: 24;");

        Label epTitle = new Label(switch (ep) {
            case BAT -> BAT_EPISODE_TITLE;
            case PELICAN -> PELICAN_EPISODE_TITLE;
            default -> PIGEON_EPISODE_TITLE;
        });
        epTitle.setFont(Font.font("Arial Black", 46));
        epTitle.setTextFill(Color.WHITE);

        Label epStatus = new Label(completed ? "Completed" : ("Unlocked Chapters: " + unlocked + "/" + chapters.length));
        epStatus.setFont(Font.font("Consolas", 30));
        epStatus.setTextFill(completed ? Color.LIMEGREEN : Color.ORANGE);

        boolean rewardUnlocked = isEpisodeRewardUnlocked(ep);
        Label epReward = new Label("Episode Award: " + (switch (ep) {
            case BAT -> BAT_EPISODE_AWARD;
            case PELICAN -> PELICAN_EPISODE_AWARD;
            default -> PIGEON_EPISODE_AWARD;
        })
                + (rewardUnlocked ? " (Unlocked)" : " (Locked)"));
        epReward.setFont(Font.font("Consolas", 26));
        epReward.setTextFill(rewardUnlocked ? Color.GOLD : Color.LIGHTGRAY);

        Label flavor = new Label(flavorText);
        flavor.setFont(Font.font("Consolas", 22));
        flavor.setTextFill(Color.LIGHTBLUE);

        HBox actions = new HBox(18);
        actions.setAlignment(Pos.CENTER_LEFT);

        Button continueEpisode = uiFactory.action(
                completed ? "REPLAY EPISODE" : "CONTINUE EPISODE",
                430, 96, 32, "#00C853", 22, () -> {
                    resetMatchStats();
                    selectedEpisode = ep;
                    storyReplayMode = false;
                    StoryChapter[] active = activeStoryChapters();
                    storyChapterIndex = Math.max(0, Math.min(getEpisodeUnlocked(ep) - 1, active.length - 1));
                    String briefing = episodeBriefing(ep);
                    showStoryDialogue(stage, "Episode Briefing", "Narrator", briefing, () -> showCurrentStoryChapterIntro(stage));
                });

        Button chapterSelect = uiFactory.action("CHAPTER SELECT", 380, 96, 30, "#1976D2", 22, () -> {
            selectedEpisode = ep;
            showEpisodeChapterSelect(stage);
        });

        actions.getChildren().addAll(continueEpisode, chapterSelect);
        card.getChildren().addAll(epTitle, epStatus, epReward, actions, flavor);
        return card;
    }

    private void showEpisodeChapterSelect(Stage stage) {
        playMenuMusic();
        StoryChapter[] chapters = activeStoryChapters();
        int unlocked = Math.max(1, Math.min(getEpisodeUnlocked(selectedEpisode), chapters.length));
        VBox root = new VBox(30);
        root.setAlignment(Pos.CENTER);
        root.setPadding(new Insets(50));
        root.setStyle("-fx-background-color: linear-gradient(to bottom, #0A1530, #1B2C52);");

        Label title = new Label(activeEpisodeTitle() + " - CHAPTERS");
        title.setFont(Font.font("Arial Black", FontWeight.BOLD, 84));
        title.setTextFill(Color.CYAN.brighter());

        VBox chapterList = new VBox(18);
        chapterList.setAlignment(Pos.CENTER);

        for (int i = 0; i < chapters.length; i++) {
            StoryChapter chapter = chapters[i];
            boolean isUnlocked = i < unlocked;
            String label = (i + 1) + ". " + chapter.title + " - " + chapter.opponentName;
            Button chapterBtn = new Button(isUnlocked ? label : ((i + 1) + ". LOCKED"));
            chapterBtn.setPrefSize(1500, 100);
            chapterBtn.setFont(Font.font("Arial Black", 30));
            chapterBtn.setStyle(isUnlocked
                    ? "-fx-background-color: #263238; -fx-text-fill: white; -fx-background-radius: 18;"
                    : "-fx-background-color: #444444; -fx-text-fill: #BBBBBB; -fx-background-radius: 18;");
            chapterBtn.setDisable(!isUnlocked);

            if (isUnlocked) {
                int chapterIndex = i;
                chapterBtn.setOnAction(e -> {
                    playButtonClick();
                    resetMatchStats();
                    storyReplayMode = true;
                    storyChapterIndex = chapterIndex;
                    showCurrentStoryChapterIntro(stage);
                });
            }
            chapterList.getChildren().add(chapterBtn);
        }

        Button back = uiFactory.action("BACK TO EPISODES", 700, 105, 42, "#FF1744", 22, () -> showEpisodesHub(stage));

        root.getChildren().addAll(title, chapterList, back);
        Scene scene = new Scene(root, WIDTH, HEIGHT);
        setupKeyboardNavigation(scene);
        applyConsoleHighlight(scene);
        setScenePreservingFullscreen(stage, scene);
        back.requestFocus();
    }

    private void showCurrentStoryChapterIntro(Stage stage) {
        if (!storyModeActive) return;
        StoryChapter[] chapters = activeStoryChapters();
        if (storyChapterIndex < 0 || storyChapterIndex >= chapters.length) {
            showEpisodesHub(stage);
            return;
        }
        StoryChapter chapter = chapters[storyChapterIndex];
        showStoryDialogue(stage, chapter.title, chapter.speaker, chapter.dialogue, () -> startStoryBattle(stage));
    }

    private void startStoryBattle(Stage stage) {
        if (!storyModeActive) return;
        StoryChapter[] chapters = activeStoryChapters();
        if (storyChapterIndex < 0 || storyChapterIndex >= chapters.length) {
            showEpisodesHub(stage);
            return;
        }
        StoryChapter chapter = chapters[storyChapterIndex];
        selectedMap = chapter.map;
        activePlayers = 2;
        startMatch(stage);
    }

    private Bird createStoryBird(double x, BirdType type, int playerIdx, String name,
                                 double health, double powerMult, double speedMult, boolean ai) {
        Bird b = new Bird(x, type, playerIdx, this);
        b.name = name;
        b.health = health;
        b.setBaseMultipliers(b.baseSizeMultiplier, powerMult, speedMult);
        players[playerIdx] = b;
        isAI[playerIdx] = ai;
        return b;
    }

    private void setupDefaultStoryDuel(StoryChapter chapter) {
        activePlayers = 2;
        BirdType playerType = activeEpisodePlayerType();
        createStoryBird(1200, playerType, 0, "You: " + playerType.name, 100, 1.0, 1.0, false);
        createStoryBird(4200, chapter.opponentType, 1, chapter.opponentName,
                chapter.opponentHealth, chapter.opponentPowerMult, chapter.opponentSpeedMult, true);
    }

    private void assignStoryTeams(int... teams) {
        storyTeamMode = true;
        for (int i = 0; i < teams.length && i < storyTeams.length; i++) {
            storyTeams[i] = teams[i];
        }
    }

    private void setupPelicanStoryChapter(StoryChapter chapter) {
        switch (storyChapterIndex) {
            case 1 -> {
                activePlayers = 4;
                createStoryBird(900, BirdType.PELICAN, 0, "You: Iron Beak", 110, 1.05, 1.0, false);
                createStoryBird(1700, BirdType.BAT, 1, "Ally: Echo Bat", 105, 1.0, 1.2, true);
                createStoryBird(3800, BirdType.TURKEY, 2, "Enemy: Iron Turkey", 170, 1.3, 0.95, true);
                createStoryBird(4700, BirdType.SHOEBILL, 3, "Enemy: Stone Shoebill", 175, 1.25, 1.0, true);
                assignStoryTeams(1, 1, 2, 2);
                storyMatchTimerOverride = 75 * 60;
            }
            case 2 -> {
                activePlayers = 4;
                createStoryBird(900, BirdType.PELICAN, 0, "You: Iron Beak", 115, 1.1, 1.0, false);
                // 1v3 chapter balance: each enemy is individually weaker than the player.
                createStoryBird(3700, BirdType.RAZORBILL, 1, "Enemy: Razor Captain", 92, 0.86, 0.9, true);
                createStoryBird(4500, BirdType.EAGLE, 2, "Enemy: Talon Eagle", 90, 0.84, 0.9, true);
                createStoryBird(5300, BirdType.HUMMINGBIRD, 3, "Enemy: Neon Hummingbird", 86, 0.82, 0.92, true);
                assignStoryTeams(1, 2, 2, 2);
                storyMatchTimerOverride = 80 * 60;
            }
            case 3 -> {
                activePlayers = 2;
                createStoryBird(1000, BirdType.PELICAN, 0, "You: Iron Beak", 120, 1.12, 1.0, false);
                createStoryBird(4400, BirdType.VULTURE, 1, chapter.opponentName, 250, 1.45, 1.08, true);
                storyMatchTimerOverride = 70 * 60;
            }
            case 4 -> {
                activePlayers = 4;
                createStoryBird(800, BirdType.PELICAN, 0, "You: Iron Beak", 120, 1.12, 1.0, false);
                createStoryBird(1500, BirdType.EAGLE, 1, "Ally: Sky King", 120, 1.2, 1.12, true);
                createStoryBird(4100, BirdType.VULTURE, 2, "Boss: Storm Vulture", 255, 1.45, 1.08, true);
                createStoryBird(5000, BirdType.OPIUMBIRD, 3, "Boss: Opium Seer", 230, 1.4, 1.2, true);
                assignStoryTeams(1, 1, 2, 2);
                storyMatchTimerOverride = 95 * 60;
            }
            default -> {
                setupDefaultStoryDuel(chapter);
                storyMatchTimerOverride = 85 * 60;
            }
        }
    }

    private void setupStoryChapterRoster(StoryChapter chapter) {
        if (selectedEpisode == EpisodeType.PELICAN) {
            setupPelicanStoryChapter(chapter);
            return;
        }
        setupDefaultStoryDuel(chapter);
    }

    private void applyStoryChapterArenaModifiers(StoryChapter chapter) {
        if (selectedEpisode != EpisodeType.PELICAN) return;
        switch (storyChapterIndex) {
            case 0 -> {
                addToKillFeed("HARBOR BLITZ: Rooftop vents surge with stormwind.");
                windVents.add(new WindVent(1200, GROUND_Y - 520, 420));
                windVents.add(new WindVent(3000, GROUND_Y - 560, 420));
                windVents.add(new WindVent(4700, GROUND_Y - 520, 420));
                powerUps.add(new PowerUp(1600, GROUND_Y - 740, PowerUpType.NEON));
                powerUps.add(new PowerUp(4300, GROUND_Y - 760, PowerUpType.NEON));
            }
            case 1 -> {
                addToKillFeed("JUNGLE TAG TEAM: Shared team lives. Control nectar lanes.");
                nectarNodes.add(new NectarNode(1300, GROUND_Y - 900, true));
                nectarNodes.add(new NectarNode(2950, GROUND_Y - 1200, false));
                nectarNodes.add(new NectarNode(4700, GROUND_Y - 860, true));
                powerUps.add(new PowerUp(3000, GROUND_Y - 1450, PowerUpType.VINE_GRAPPLE));
            }
            case 2 -> {
                addToKillFeed("GALE GAUNTLET: 1v3 survival. Thermal pockets now hyperactive.");
                windVents.add(new WindVent(950, GROUND_Y - 320, 500));
                windVents.add(new WindVent(2500, GROUND_Y - 350, 550));
                windVents.add(new WindVent(4300, GROUND_Y - 320, 500));
                powerUps.add(new PowerUp(2600, GROUND_Y - 1220, PowerUpType.THERMAL));
                powerUps.add(new PowerUp(3300, GROUND_Y - 920, PowerUpType.HEALTH));
            }
            case 3 -> {
                addToKillFeed("ABYSS LOCKDOWN: Crow swarm is active from frame one.");
                for (int i = 0; i < 7; i++) {
                    double cx = 900 + random.nextDouble() * 4200;
                    double cy = 220 + random.nextDouble() * 500;
                    crowMinions.add(new CrowMinion(cx, cy, players[0]));
                }
                powerUps.add(new PowerUp(3000, GROUND_Y - 900, PowerUpType.RAGE));
                powerUps.add(new PowerUp(3600, GROUND_Y - 650, PowerUpType.HEALTH));
            }
            case 4 -> {
                addToKillFeed("CROWN OF THUNDER: Two-boss parliament. Team up and break the ring.");
                windVents.add(new WindVent(850, GROUND_Y - 380, 450));
                windVents.add(new WindVent(2600, GROUND_Y - 460, 620));
                windVents.add(new WindVent(5000, GROUND_Y - 380, 450));
                powerUps.add(new PowerUp(2200, GROUND_Y - 860, PowerUpType.SPEED));
                powerUps.add(new PowerUp(3000, GROUND_Y - 980, PowerUpType.RAGE));
                powerUps.add(new PowerUp(3800, GROUND_Y - 860, PowerUpType.HEALTH));
            }
        }
    }

    private void setupClassicEncounterRoster(ClassicEncounter encounter) {
        classicTeamMode = true;
        Arrays.fill(classicTeams, 2);
        classicTeams[0] = 1;

        int slot = 0;
        createStoryBird(900, classicSelectedBird, slot, "You: " + classicSelectedBird.name, 112, 1.05, 1.04, false);
        if (classicSelectedBird == BirdType.PIGEON && noirPigeonUnlocked) {
            players[slot].isNoirSkin = true;
        } else if (classicSelectedBird == BirdType.EAGLE && eagleSkinUnlocked) {
            players[slot].isClassicSkin = true;
        } else if (isClassicRewardUnlocked(classicSelectedBird)) {
            players[slot].isClassicSkin = true;
        }
        slot++;

        if (encounter.allies != null) {
            for (int i = 0; i < encounter.allies.length && slot < 4; i++) {
                ClassicFighter ally = encounter.allies[i];
                createStoryBird(
                        1300 + i * 350,
                        ally.type,
                        slot,
                        ally.title,
                        ally.health,
                        ally.powerMult,
                        ally.speedMult,
                        true
                );
                classicTeams[slot] = 1;
                slot++;
            }
        }

        if (encounter.enemies != null) {
            for (int i = 0; i < encounter.enemies.length && slot < 4; i++) {
                ClassicFighter enemy = encounter.enemies[i];
                createStoryBird(
                        3800 + i * 520,
                        enemy.type,
                        slot,
                        enemy.title,
                        enemy.health,
                        enemy.powerMult,
                        enemy.speedMult,
                        true
                );
                classicTeams[slot] = 2;
                slot++;
            }
        }

        activePlayers = Math.max(2, slot);
    }

    private void applyClassicEncounterArenaModifiers(ClassicEncounter encounter) {
        if (encounter == null) return;
        addToKillFeed("CLASSIC TWIST: " + encounter.twist.label);
        switch (encounter.twist) {
            case STORM_LIFTS -> {
                windVents.add(new WindVent(900, GROUND_Y - 380, 420));
                windVents.add(new WindVent(2800, GROUND_Y - 420, 520));
                windVents.add(new WindVent(4700, GROUND_Y - 380, 420));
                powerUps.add(new PowerUp(3000, GROUND_Y - 980, PowerUpType.THERMAL));
            }
            case CROW_CARNIVAL -> {
                for (int i = 0; i < 6; i++) {
                    double cx = 700 + random.nextDouble() * 4600;
                    double cy = 240 + random.nextDouble() * 520;
                    crowMinions.add(new CrowMinion(cx, cy, null));
                }
                powerUps.add(new PowerUp(3000, GROUND_Y - 900, PowerUpType.RAGE));
            }
            case TITAN_CACHE -> {
                powerUps.add(new PowerUp(1200, GROUND_Y - 760, PowerUpType.TITAN));
                powerUps.add(new PowerUp(3000, GROUND_Y - 960, PowerUpType.OVERCHARGE));
                powerUps.add(new PowerUp(4700, GROUND_Y - 760, PowerUpType.HEALTH));
            }
            case NECTAR_BLOOM -> {
                nectarNodes.add(new NectarNode(1200, GROUND_Y - 900, true));
                nectarNodes.add(new NectarNode(3000, GROUND_Y - 1200, false));
                nectarNodes.add(new NectarNode(4700, GROUND_Y - 900, true));
                powerUps.add(new PowerUp(3000, GROUND_Y - 760, PowerUpType.SPEED));
            }
            case SHOCK_DROPS -> {
                powerUps.add(new PowerUp(2000, GROUND_Y - 820, PowerUpType.FROST_NOVA));
                powerUps.add(new PowerUp(3000, GROUND_Y - 980, PowerUpType.OVERCHARGE));
                powerUps.add(new PowerUp(4000, GROUND_Y - 820, PowerUpType.FROST_NOVA));
            }
        }
    }

    private boolean didPlayerWinClassic(Bird winner) {
        if (winner == null || players[0] == null) return false;
        return getEffectiveTeam(winner.playerIndex) == getEffectiveTeam(0);
    }

    private void handleClassicMatchEnd(Stage stage, Bird winner) {
        if (!classicModeActive || classicEncounter == null) {
            showClassicBirdSelect(stage);
            return;
        }

        if (!didPlayerWinClassic(winner)) {
            showStoryDialogue(
                    stage,
                    "Run Failed",
                    "Skycaster",
                    "You were eliminated in " + classicEncounter.name + ".\nContinue to retry this encounter.",
                    () -> startClassicEncounter(stage)
            );
            return;
        }

        if (classicRoundIndex >= classicRun.size() - 1) {
            classicCompleted[classicSelectedBird.ordinal()] = true;
            unlockClassicReward(classicSelectedBird);
            saveAchievements();
            String reward = classicRewardFor(classicSelectedBird);
            showStoryDialogue(
                    stage,
                    "Classic Cleared",
                    "Skycaster Prime",
                    "Run " + classicRunCodename + " completed with " + classicSelectedBird.name + ".\nReward unlocked: "
                            + reward + ".\nThis bird now has a completion badge.",
                    () -> showClassicBirdSelect(stage)
            );
            return;
        }

        classicRoundIndex++;
        classicEncounter = classicRun.get(classicRoundIndex);
        saveAchievements();
        showStoryDialogue(
                stage,
                "Encounter Cleared",
                "Skycaster",
                "Advance to Round " + (classicRoundIndex + 1) + ": " + classicEncounter.name + ".",
                () -> showClassicEncounterIntro(stage)
        );
    }

    private void handleStoryMatchEnd(Stage stage, Bird winner) {
        StoryChapter[] chapters = activeStoryChapters();
        if (storyChapterIndex < 0 || storyChapterIndex >= chapters.length) {
            showEpisodesHub(stage);
            return;
        }

        boolean playerWon;
        if (storyTeamMode) {
            int playerTeam = getEffectiveTeam(0);
            playerWon = winner != null && getEffectiveTeam(winner.playerIndex) == playerTeam;
        } else {
            playerWon = winner != null && winner.playerIndex == 0;
        }
        if (!playerWon) {
            showStoryDialogue(stage,
                    "Defeat",
                    "Narrator",
                    "You were knocked out in " + chapters[storyChapterIndex].title + ".",
                    () -> startStoryBattle(stage));
            return;
        }

        int unlocked = getEpisodeUnlocked(selectedEpisode);
        if (!storyReplayMode && storyChapterIndex == unlocked - 1 && unlocked < chapters.length) {
            setEpisodeUnlocked(selectedEpisode, unlocked + 1);
            saveAchievements();
        }

        if (storyChapterIndex >= chapters.length - 1) {
            if (!isEpisodeCompleted(selectedEpisode)) {
                setEpisodeCompleted(selectedEpisode, true);
                if (selectedEpisode == EpisodeType.PIGEON) {
                    noirPigeonUnlocked = true;
                } else if (selectedEpisode == EpisodeType.BAT) {
                    batUnlocked = true;
                } else if (selectedEpisode == EpisodeType.PELICAN) {
                    eagleSkinUnlocked = true;
                }
                saveAchievements();
                showStoryDialogue(stage,
                        "Episode Complete",
                        "Narrator",
                        "Episode clear. Award unlocked: " + activeEpisodeAward() + "."
                                + (selectedEpisode == EpisodeType.PIGEON ? "\nSelect Pigeon to cycle skins." : "")
                                + (selectedEpisode == EpisodeType.PELICAN ? "\nSelect Eagle to use the Sky King skin." : ""),
                        () -> showEpisodesHub(stage));
            } else {
                showStoryDialogue(stage,
                        "Replay Complete",
                        "Narrator",
                        "Chapter clear. Episode is already complete, so this was a replay victory.",
                        () -> showEpisodesHub(stage));
            }
            return;
        }

        if (storyReplayMode) {
            showStoryDialogue(stage,
                    "Replay Complete",
                    "Narrator",
                    "Chapter clear. Choose another unlocked chapter or return to episodes.",
                    () -> showEpisodeChapterSelect(stage));
            return;
        }

        storyChapterIndex++;
        StoryChapter next = chapters[storyChapterIndex];
        showStoryDialogue(stage,
                "Victory",
                "Narrator",
                "You won the duel.\nNext chapter: " + next.title + " (" + next.opponentName + ").",
                () -> showCurrentStoryChapterIntro(stage));
    }

    private void showStoryDialogue(Stage stage, String titleText, String speaker, String dialogue, Runnable onContinue) {
        playMenuMusic();

        VBox root = new VBox(30);
        root.setAlignment(Pos.CENTER);
        root.setPadding(new Insets(60));
        root.setStyle("-fx-background-color: linear-gradient(to bottom, #081122, #13294B);");

        Label title = new Label(titleText);
        title.setFont(Font.font("Arial Black", FontWeight.BOLD, 92));
        title.setTextFill(Color.GOLD);

        VBox card = new VBox(20);
        card.setAlignment(Pos.CENTER_LEFT);
        card.setPadding(new Insets(35));
        card.setMaxWidth(1300);
        card.setStyle("-fx-background-color: rgba(0,0,0,0.6); -fx-background-radius: 24; -fx-border-color: #4FC3F7; -fx-border-width: 4; -fx-border-radius: 24;");

        Label speakerLabel = new Label(speaker);
        speakerLabel.setFont(Font.font("Arial Black", 44));
        speakerLabel.setTextFill(Color.CYAN.brighter());

        Label text = new Label(dialogue);
        text.setWrapText(true);
        text.setFont(Font.font("Consolas", 34));
        text.setTextFill(Color.WHITE);

        card.getChildren().addAll(speakerLabel, text);

        HBox buttons = new HBox(30);
        buttons.setAlignment(Pos.CENTER);

        Button continueButton = uiFactory.action("CONTINUE", 460, 120, 52, "#00C853", 32, onContinue);

        Button menuButton = uiFactory.action("MAIN MENU", 460, 120, 36, "#FF1744", 32, () -> showMenu(stage));
        menuButton.setWrapText(false);
        uiFactory.fitSingleLineOnLayout(menuButton, 36, 20);

        buttons.getChildren().addAll(continueButton, menuButton);
        root.getChildren().addAll(title, card, buttons);

        Scene scene = new Scene(root, WIDTH, HEIGHT);
        setupKeyboardNavigation(scene);
        applyConsoleHighlight(scene);
        setScenePreservingFullscreen(stage, scene);
        continueButton.requestFocus();
    }

    private void showStageSelect(Stage stage) {
        BorderPane root = new BorderPane();
        root.setPadding(new Insets(26, 36, 30, 36));
        root.setStyle("-fx-background-color: linear-gradient(to bottom, #000033, #000066);");

        Label title = new Label("SELECT STAGE");
        title.setFont(Font.font("Arial Black", 100));
        title.setTextFill(Color.CYAN);
        title.setEffect(new Glow(0.8));

        Button backArrow = uiFactory.action("<", 120, 90, 64, "#FF1744", 24, () -> showMenu(stage));
        HBox topBar = new HBox(24, backArrow, title);
        topBar.setAlignment(Pos.CENTER_LEFT);

        VBox options = new VBox(25);
        options.setAlignment(Pos.CENTER);
        options.setMaxWidth(1200);

        Button forestBtn = uiFactory.action("BIG FOREST", 1400, 140, 65, "#228B22", 25, () -> {
            beginFreshMatchOnMap(stage, MapType.FOREST);
        });

        Button cityBtn = uiFactory.action("PIGEON'S ROOFTOPS", 1400, 140, 65, "#4B0082", 25, () -> {
            beginFreshMatchOnMap(stage, MapType.CITY);
        });

        Button cliffsBtn = uiFactory.action("SKY CLIFFS", 1400, 140, 65, "#8B4513", 25, () -> {
            beginFreshMatchOnMap(stage, MapType.SKYCLIFFS);
        });

        Button jungleBtn = uiFactory.action("VIBRANT JUNGLE", 1400, 140, 65, "#228B22", 25, () -> {
            beginFreshMatchOnMap(stage, MapType.VIBRANT_JUNGLE);
        });

        Button caveBtn = uiFactory.action("ECHO CAVERN", 1400, 140, 65, "#37474F", 25, () -> {
            beginFreshMatchOnMap(stage, MapType.CAVE);
        });

        Button randomBtn = uiFactory.action("R\nA\nN\nD\nO\nM", 190, 860, 65, "#9C27B0", 24, () -> {
            MapType[] maps = MapType.values();
            beginFreshMatchOnMap(stage, maps[random.nextInt(maps.length)]);
        });
        randomBtn.setWrapText(true);

        options.getChildren().addAll(forestBtn, cityBtn, cliffsBtn, jungleBtn, caveBtn);

        ScrollPane scroll = new ScrollPane(options);
        scroll.setFitToWidth(true);
        scroll.setFitToHeight(true);
        scroll.setStyle("-fx-background-color: transparent;");
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);

        VBox randomColumn = new VBox(randomBtn);
        randomColumn.setAlignment(Pos.CENTER);
        randomColumn.setPadding(new Insets(6, 0, 6, 0));

        HBox mapSelectLayout = new HBox(22, scroll, randomColumn);
        mapSelectLayout.setAlignment(Pos.CENTER);
        HBox.setHgrow(scroll, javafx.scene.layout.Priority.ALWAYS);

        root.setTop(topBar);
        root.setCenter(mapSelectLayout);

        Scene scene = new Scene(root, WIDTH, HEIGHT);
        scene.getRoot().setStyle(scene.getRoot().getStyle() + ";-fx-focus-color:transparent;-fx-faint-focus-color:transparent;");
        setupKeyboardNavigation(scene);
        applyConsoleHighlight(scene);
        setScenePreservingFullscreen(stage, scene);
        forestBtn.requestFocus();
    }

    private void beginFreshMatchOnMap(Stage stage, MapType map) {
        selectedMap = map;
        classicModeActive = false;
        classicEncounter = null;
        classicRun.clear();
        classicRoundIndex = 0;
        classicTeamMode = false;
        Arrays.fill(classicTeams, 1);
        competitionSeriesActive = false;
        Arrays.fill(competitionRoundWins, 0);
        Arrays.fill(competitionTeamWins, 0);
        competitionRoundNumber = 1;
        competitionDraftComplete = false;
        competitionDraftSummary = "";
        competitionBannedBirds.clear();
        if (competitionModeEnabled && !storyModeActive && !isTrialMode && !classicModeActive) {
            showCompetitionDraft(stage, () -> startMatch(stage));
            return;
        }
        startMatch(stage);
    }

    private void setSlotBirdType(int idx, BirdType type) {
        if (playerSlots == null || idx < 0 || idx >= playerSlots.length || playerSlots[idx] == null || type == null) return;
        VBox selectorBox = (VBox) playerSlots[idx].getChildren().get(2);
        selectorBox.setUserData(type);
        if (!selectorBox.getChildren().isEmpty() && selectorBox.getChildren().get(0) instanceof Label selected) {
            selected.setText("Selected: " + type.name);
        }
    }

    private List<BirdType> unlockedBirdPool() {
        List<BirdType> pool = new ArrayList<>();
        for (BirdType bt : BirdType.values()) {
            if (isBirdUnlocked(bt)) pool.add(bt);
        }
        if (pool.isEmpty()) pool.add(firstUnlockedBird());
        return pool;
    }

    private void prepareCompetitionDraft(BirdType banA, BirdType banB) {
        competitionBannedBirds.clear();
        if (banA != null) competitionBannedBirds.add(banA);
        if (banB != null) competitionBannedBirds.add(banB);
        competitionDraftComplete = false;
        competitionDraftSummary = "Draft bans: Side A banned " + (banA != null ? banA.name : "-")
                + ", Side B banned " + (banB != null ? banB.name : "-") + ".";
    }

    private void showCompetitionBirdSelect(Stage stage, Runnable onReady) {
        playMenuMusic();

        List<BirdType> filteredPool = new ArrayList<>(unlockedBirdPool());
        filteredPool.removeIf(competitionBannedBirds::contains);
        final List<BirdType> pool = filteredPool.isEmpty() ? new ArrayList<>(unlockedBirdPool()) : filteredPool;

        BorderPane root = new BorderPane();
        root.setPadding(new Insets(30, 34, 30, 34));
        root.setStyle("-fx-background-color: linear-gradient(to bottom, #0A1F2E, #1B263B);");

        Label title = new Label("COMPETITION BIRD SELECT");
        title.setFont(Font.font("Arial Black", FontWeight.BOLD, 72));
        title.setTextFill(Color.GOLD);

        Label info = new Label(competitionDraftSummary + "\nPick birds for this series. Player count and teams are kept from menu.");
        info.setFont(Font.font("Consolas", 24));
        info.setTextFill(Color.web("#B3E5FC"));

        VBox top = new VBox(8, title, info);
        top.setAlignment(Pos.CENTER);

        VBox rows = new VBox(14);
        rows.setAlignment(Pos.TOP_CENTER);

        BirdType[] picks = new BirdType[activePlayers];
        for (int i = 0; i < activePlayers; i++) {
            picks[i] = pool.get(i % pool.size());
        }

        for (int i = 0; i < activePlayers; i++) {
            int idx = i;
            Label who = new Label("PLAYER " + (i + 1) + (teamModeEnabled ? "  (" + (playerTeams[i] == 2 ? "TEAM B" : "TEAM A") + ")" : ""));
            who.setFont(Font.font("Arial Black", 28));
            who.setTextFill(Color.WHITE);

            Button pickBtn = uiFactory.action("BIRD: " + picks[i].name, 920, 84, 30, "#263238", 24, () -> {});
            pickBtn.setOnAction(e -> {
                playButtonClick();
                int current = pool.indexOf(picks[idx]);
                if (current < 0) current = 0;
                picks[idx] = pool.get((current + 1) % pool.size());
                pickBtn.setText("BIRD: " + picks[idx].name);
            });

            HBox row = new HBox(18, who, pickBtn);
            row.setAlignment(Pos.CENTER);
            rows.getChildren().add(row);
        }

        ScrollPane scroll = new ScrollPane(rows);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background: transparent; -fx-background-color: transparent;");
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);

        Button backToBans = uiFactory.action("BACK TO BANS", 360, 94, 30, "#546E7A", 22, () -> showCompetitionDraft(stage, onReady));
        Button startSeries = uiFactory.action("START SERIES", 420, 94, 34, "#00C853", 24, () -> {
            for (int i = 0; i < activePlayers; i++) {
                setSlotBirdType(i, picks[i]);
            }
            competitionDraftComplete = true;
            if (onReady != null) onReady.run();
        });
        HBox bottom = new HBox(20, backToBans, startSeries);
        bottom.setAlignment(Pos.CENTER);

        root.setTop(top);
        root.setCenter(scroll);
        root.setBottom(bottom);

        Scene scene = new Scene(root, WIDTH, HEIGHT);
        setupKeyboardNavigation(scene);
        applyConsoleHighlight(scene);
        setScenePreservingFullscreen(stage, scene);
        if (!rows.getChildren().isEmpty() && rows.getChildren().get(0) instanceof HBox h && h.getChildren().size() > 1 && h.getChildren().get(1) instanceof Button b) {
            b.requestFocus();
        }
    }

    private void showCompetitionDraft(Stage stage, Runnable onReady) {
        playMenuMusic();

        BorderPane root = new BorderPane();
        root.setPadding(new Insets(32, 36, 32, 36));
        root.setStyle("-fx-background-color: linear-gradient(to bottom, #102027, #1A237E);");

        Label title = new Label("COMPETITION DRAFT-BAN");
        title.setFont(Font.font("Arial Black", FontWeight.BOLD, 72));
        title.setTextFill(Color.GOLD);

        Label status = new Label("Side A: choose 1 ban");
        status.setFont(Font.font("Consolas", 30));
        status.setTextFill(Color.web("#B3E5FC"));

        VBox top = new VBox(10, title, status);
        top.setAlignment(Pos.CENTER);

        VBox list = new VBox(12);
        list.setAlignment(Pos.TOP_CENTER);
        List<BirdType> pool = unlockedBirdPool();
        BirdType[] bans = new BirdType[2];
        int[] turn = new int[]{0};

        for (BirdType bt : pool) {
            Button b = uiFactory.action(bt.name, 1250, 88, 34, "#263238", 24, () -> {});
            b.setOnAction(e -> {
                playButtonClick();
                if (turn[0] > 1) return;
                if (turn[0] == 0) {
                    bans[0] = bt;
                    turn[0] = 1;
                    status.setText("Side B: choose 1 ban");
                    b.setDisable(true);
                    b.setStyle("-fx-background-color: #C62828; -fx-text-fill: white; -fx-font-weight: bold;");
                    return;
                }
                if (bt == bans[0]) return;
                bans[1] = bt;
                turn[0] = 2;
                b.setDisable(true);
                b.setStyle("-fx-background-color: #1565C0; -fx-text-fill: white; -fx-font-weight: bold;");
                status.setText("Bans locked. Move to bird select.");
                prepareCompetitionDraft(bans[0], bans[1]);
                showCompetitionBirdSelect(stage, onReady);
            });
            list.getChildren().add(b);
        }

        ScrollPane scroll = new ScrollPane(list);
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scroll.setStyle("-fx-background: transparent; -fx-background-color: transparent;");

        Button back = uiFactory.action("BACK", 320, 96, 34, "#FF1744", 24, () -> showMenu(stage));
        HBox bottom = new HBox(back);
        bottom.setAlignment(Pos.CENTER);

        root.setTop(top);
        root.setCenter(scroll);
        root.setBottom(bottom);

        Scene scene = new Scene(root, WIDTH, HEIGHT);
        setupKeyboardNavigation(scene);
        applyConsoleHighlight(scene);
        setScenePreservingFullscreen(stage, scene);
        if (!list.getChildren().isEmpty() && list.getChildren().get(0) instanceof Button first) first.requestFocus();
    }
    private VBox createMapBox(String buttonText, String subText, String bgColor, Color subColor) {
        VBox box = new VBox(10);
        box.setAlignment(Pos.CENTER);

        Button btn = new Button(buttonText);
        btn.setPrefSize(1400, 120); // Wide horizontal rectangle
        btn.setFont(Font.font("Arial Black", 60)); // Larger for readability
        btn.setStyle("-fx-background-color: " + bgColor + "; -fx-text-fill: white; -fx-background-radius: 20; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.5), 10, 0, 0, 4);"); // Clean with shadow

        Label sub = new Label(subText);
        sub.setFont(Font.font("Arial Black", 40));
        sub.setTextFill(subColor);

        box.getChildren().addAll(btn, sub);
        return box;
    }

    private VBox createPlayerSlot(int idx) {
        VBox box = new VBox(15);
        box.setAlignment(Pos.CENTER);
        box.setPadding(new Insets(20));
        box.setStyle("-fx-background-color: rgba(0,0,0,0.5); -fx-background-radius: 25; -fx-border-color: gold; -fx-border-width: 3;");

        Label title = new Label("Player " + (idx + 1));
        title.setFont(Font.font("Arial Black", 36));
        title.setTextFill(Color.YELLOW);

        // === HUMAN / AI SWITCHER (normal Button so focus never sticks) ===
        Button aiButton = new Button("HUMAN");
        aiButton.setPrefSize(180, 60);
        aiButton.setFont(Font.font("Arial Black", 26));
        aiButton.setStyle("-fx-background-color: #2196F3; -fx-text-fill: white;");
        aiButton.setFocusTraversable(true);
        aiButton.setOnAction(e -> {
            playButtonClick();   // sound always plays
            isAI[idx] = !isAI[idx];
            aiButton.setText(isAI[idx] ? "AI" : "HUMAN");
            aiButton.setStyle("-fx-background-color: " + (isAI[idx] ? "#F44336" : "#2196F3") + "; -fx-text-fill: white;");
        });

        Button teamButton = new Button();
        teamButton.setPrefSize(180, 60);
        teamButton.setFont(Font.font("Arial Black", 24));
        teamButton.setFocusTraversable(true);
        teamButton.setOnAction(e -> {
            playButtonClick();
            if (!teamModeEnabled) return;
            playerTeams[idx] = (playerTeams[idx] == 1) ? 2 : 1;
            refreshTeamButtons();
        });
        teamButtons[idx] = teamButton;
        applyTeamButtonStyle(teamButton, idx);

        HBox controls = new HBox(10, aiButton, teamButton);
        controls.setAlignment(Pos.CENTER);

        BirdType defaultType = BirdType.values()[idx % BirdType.values().length];
        if (!isBirdUnlocked(defaultType)) defaultType = firstUnlockedBird();
        VBox selector = createBirdSelector(defaultType);
        selector.setUserData(defaultType);

        box.getChildren().addAll(title, controls, selector);
        return box;
    }

    private void refreshTeamToggleButton() {
        if (teamModeToggleButton == null) return;
        teamModeToggleButton.setText(teamModeEnabled ? "TEAM MODE: ON" : "TEAM MODE: OFF");
        String color = teamModeEnabled ? "#2E7D32" : "#546E7A";
        teamModeToggleButton.setStyle("-fx-background-color: " + color + "; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 30;");
    }

    private void refreshTeamButtons() {
        for (int i = 0; i < teamButtons.length; i++) {
            Button teamBtn = teamButtons[i];
            if (teamBtn == null) continue;
            boolean showForPlayer = teamModeEnabled && i < activePlayers;
            teamBtn.setVisible(showForPlayer);
            teamBtn.setManaged(showForPlayer);
            teamBtn.setDisable(!teamModeEnabled);
            applyTeamButtonStyle(teamBtn, i);
        }
    }

    private void applyTeamButtonStyle(Button teamButton, int playerIdx) {
        int team = playerTeams[playerIdx] == 2 ? 2 : 1;
        String teamText = team == 1 ? "TEAM A" : "TEAM B";
        String color = team == 1 ? "#1565C0" : "#C62828";
        String opacity = teamModeEnabled ? "1.0" : "0.5";
        teamButton.setText(teamText);
        teamButton.setStyle("-fx-background-color: " + color + "; -fx-text-fill: white; -fx-font-weight: bold; -fx-opacity: " + opacity + ";");
    }

    private VBox createBirdSelector(BirdType def) {
        VBox box = new VBox(15);
        box.setAlignment(Pos.CENTER);
        box.setPadding(new Insets(20));
        box.setStyle("-fx-background-color: rgba(0,0,0,0.4); -fx-background-radius: 20;");

        Label selected = new Label("Selected: " + def.name);
        selected.setFont(Font.font(26));
        selected.setTextFill(Color.YELLOW);

        ScrollPane statsPane = new ScrollPane();
        statsPane.setPrefSize(560, 220);
        statsPane.setMinSize(560, 220);
        statsPane.setMaxSize(560, 220);
        statsPane.setFitToWidth(true);
        statsPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        statsPane.setStyle("-fx-background: black; -fx-background-color: rgba(0,0,0,0.7); -fx-border-color: #FFD54F; -fx-border-width: 2;");
        Label stats = new Label("Power: " + def.power + " | Speed: " + String.format("%.1f", def.speed) + " | Jump: " + def.jumpHeight + "\n\nSPECIAL: " + def.ability);
        stats.setFont(Font.font("Consolas", 24));
        stats.setTextFill(Color.WHITE);
        stats.setWrapText(true);
        stats.setMaxWidth(530);
        statsPane.setContent(stats);

        GridPane grid = new GridPane();
        grid.setHgap(15);
        grid.setVgap(15);
        int col = 0, row = 0;

        for (BirdType bt : BirdType.values()) {
            Button b = new Button(bt.name);
            b.setPrefSize(160, 70);
            double autoSize = Math.max(11, 18 - Math.max(0, bt.name.length() - 8) * 0.9);
            b.setFont(Font.font("Arial Black", autoSize));
            b.setWrapText(true);
            b.setTextOverrun(OverrunStyle.CLIP);
            b.setTextAlignment(TextAlignment.CENTER);
            String baseStyle = "-fx-background-color: " + toHex(bt.color) + "; -fx-text-fill: white; -fx-font-weight: bold;";
            b.setStyle(baseStyle + " -fx-text-overrun: clip;");
            b.setUserData(bt);
            if (!isBirdUnlocked(bt)) {
                b.setText(bt.name + "\nLOCKED");
                b.setDisable(true);
                b.setOpacity(0.55);
                b.setStyle("-fx-background-color: #4A4A4A; -fx-text-fill: #BBBBBB; -fx-font-weight: bold;");
            }

            b.setOnAction(e -> {
                playButtonClick();
                Object currentData = box.getUserData();

                // === PIGEON SKIN CYCLER ===
                if (bt == BirdType.PIGEON && (cityPigeonUnlocked || noirPigeonUnlocked)) {
                    if (cityPigeonUnlocked && noirPigeonUnlocked) {
                        if ("CITY_PIGEON".equals(currentData)) {
                            selected.setText("Selected: Noir Pigeon (Skin)");
                            box.setUserData("NOIR_PIGEON");
                            b.setStyle("-fx-background-color: #111111; -fx-text-fill: #F44336; -fx-font-weight: bold;");
                        } else if ("NOIR_PIGEON".equals(currentData)) {
                            selected.setText("Selected: Pigeon");
                            box.setUserData(BirdType.PIGEON);
                            b.setStyle(baseStyle);
                        } else {
                            selected.setText("Selected: City Pigeon (Skin)");
                            box.setUserData("CITY_PIGEON");
                            b.setStyle("-fx-background-color: #FFD700; -fx-text-fill: black; -fx-font-weight: bold;");
                        }
                    } else if (cityPigeonUnlocked) {
                        if ("CITY_PIGEON".equals(currentData)) {
                            selected.setText("Selected: Pigeon");
                            box.setUserData(BirdType.PIGEON);
                            b.setStyle(baseStyle);
                        } else {
                            selected.setText("Selected: City Pigeon (Skin)");
                            box.setUserData("CITY_PIGEON");
                            b.setStyle("-fx-background-color: #FFD700; -fx-text-fill: black; -fx-font-weight: bold;");
                        }
                    } else {
                        if ("NOIR_PIGEON".equals(currentData)) {
                            selected.setText("Selected: Pigeon");
                            box.setUserData(BirdType.PIGEON);
                            b.setStyle(baseStyle);
                        } else {
                            selected.setText("Selected: Noir Pigeon (Skin)");
                            box.setUserData("NOIR_PIGEON");
                            b.setStyle("-fx-background-color: #111111; -fx-text-fill: #F44336; -fx-font-weight: bold;");
                        }
                    }
                }
                // === SKY KING EAGLE SKIN ===
                else if (bt == BirdType.EAGLE && eagleSkinUnlocked) {
                    if ("SKY_KING_EAGLE".equals(currentData)) {
                        selected.setText("Selected: Eagle");
                        box.setUserData(BirdType.EAGLE);
                        b.setStyle(baseStyle);
                    } else if (currentData == BirdType.EAGLE) {
                        selected.setText("Selected: Sky King Eagle (Skin)");
                        box.setUserData("SKY_KING_EAGLE");
                        b.setStyle("-fx-background-color: #FFD700; -fx-text-fill: black; -fx-font-weight: bold;");
                    } else {
                        selected.setText("Selected: Eagle");
                        box.setUserData(BirdType.EAGLE);
                        b.setStyle(baseStyle);
                    }
                }
                // === NORMAL BIRDS ===
                else {
                    String classicKey = classicSkinDataKey(bt);
                    boolean hasClassicSkin = bt != BirdType.PIGEON && bt != BirdType.EAGLE && isClassicRewardUnlocked(bt);
                    if (hasClassicSkin && classicKey.equals(currentData)) {
                        selected.setText("Selected: " + bt.name);
                        box.setUserData(bt);
                        b.setStyle(baseStyle);
                    } else if (hasClassicSkin && currentData == bt) {
                        selected.setText("Selected: " + classicRewardFor(bt) + " (Skin)");
                        box.setUserData(classicKey);
                        b.setStyle("-fx-background-color: #212121; -fx-text-fill: #FFD54F; -fx-font-weight: bold;");
                    } else {
                        selected.setText("Selected: " + bt.name);
                        box.setUserData(bt);
                        b.setStyle(baseStyle);
                    }
                }

                stats.setText("Power: " + bt.power + " | Speed: " + String.format("%.1f", bt.speed) + " | Jump: " + bt.jumpHeight + "\n\nSPECIAL: " + bt.ability);
            });

            grid.add(b, col++, row);
            if (col > 2) { col = 0; row++; }
        }

        Button random = new Button("RANDOM");
        random.setPrefSize(160, 70);
        random.setStyle("-fx-background-color: #9C27B0; -fx-text-fill: white;");
        random.setOnAction(e -> {
            playButtonClick();
            List<BirdType> unlocked = new ArrayList<>();
            for (BirdType bt : BirdType.values()) {
                if (isBirdUnlocked(bt)) unlocked.add(bt);
            }
            BirdType r = unlocked.isEmpty() ? BirdType.PIGEON : unlocked.get((int) (Math.random() * unlocked.size()));
            box.setUserData(r);
            selected.setText("Selected: " + r.name + " (Random!)");
            stats.setText("Power: " + r.power + " | Speed: " + String.format("%.1f", r.speed) + " | Jump: " + r.jumpHeight + "\n\nSPECIAL: " + r.ability);
        });
        grid.add(random, col, row);

        // Default selection - force normal version
        for (var node : grid.getChildren()) {
            if (node instanceof Button btn && btn.getUserData() == def) {
                selected.setText("Selected: " + def.name);
                box.setUserData(def);
                stats.setText("Power: " + def.power + " | Speed: " + String.format("%.1f", def.speed) + " | Jump: " + def.jumpHeight + "\n\nSPECIAL: " + def.ability);
                btn.setStyle("-fx-background-color: " + toHex(def.color) + "; -fx-text-fill: white; -fx-font-weight: bold;");
                break;
            }
        }

        box.getChildren().addAll(selected, grid, statsPane);
        return box;
    }

    private String toHex(Color c) {
        return String.format("#%02X%02X%02X", (int) (c.getRed() * 255), (int) (c.getGreen() * 255), (int) (c.getBlue() * 255));
    }

    private void configureMatchModes() {
        activeMutator = MatchMutator.NONE;
        activePowerUpSpawnInterval = POWERUP_SPAWN_INTERVAL;
        lastMutatorHazardTime = System.nanoTime();

        if (classicModeActive) {
            if (classicEncounter == null) return;
            activeMutator = classicEncounter.mutator;
            if (activeMutator != MatchMutator.NONE) {
                addToKillFeed("CLASSIC MUTATOR: " + activeMutator.label);
            }
            if (activeMutator == MatchMutator.POWERUP_STORM) {
                activePowerUpSpawnInterval = 1_000_000_000L * 3;
            } else if (activeMutator == MatchMutator.TURBO_BRAWL) {
                for (int i = 0; i < activePlayers; i++) {
                    Bird b = players[i];
                    if (b == null) continue;
                    b.setBaseMultipliers(
                            b.baseSizeMultiplier,
                            b.basePowerMultiplier * 1.10,
                            b.baseSpeedMultiplier * 1.12
                    );
                }
            }
            return;
        }

        if (storyModeActive || isTrialMode) return;

        if (competitionModeEnabled) {
            if (!competitionSeriesActive) {
                Arrays.fill(competitionRoundWins, 0);
                Arrays.fill(competitionTeamWins, 0);
                competitionRoundNumber = 1;
                competitionSeriesActive = true;
            }
            matchTimer = COMPETITION_DURATION_FRAMES;
            activePowerUpSpawnInterval = Long.MAX_VALUE;
            powerUps.clear();
            addToKillFeed("COMPETITION MODE: Round " + competitionRoundNumber + " (first to " + COMPETITION_ROUND_TARGET + ")");
            if (!competitionDraftSummary.isBlank()) {
                addToKillFeed(competitionDraftSummary);
                competitionDraftSummary = "";
            }
            return;
        }

        if (!mutatorModeEnabled) return;

        MatchMutator[] pool = {
                MatchMutator.LOW_GRAVITY,
                MatchMutator.POWERUP_STORM,
                MatchMutator.CROW_SURGE,
                MatchMutator.TURBO_BRAWL
        };
        activeMutator = pool[random.nextInt(pool.length)];
        addToKillFeed("MUTATOR ACTIVE: " + activeMutator.label);

        if (activeMutator == MatchMutator.POWERUP_STORM) {
            activePowerUpSpawnInterval = 1_000_000_000L * 3;
            addToKillFeed("Power-Up Storm: faster drops all match.");
        } else if (activeMutator == MatchMutator.TURBO_BRAWL) {
            for (int i = 0; i < activePlayers; i++) {
                Bird b = players[i];
                if (b == null) continue;
                b.setBaseMultipliers(
                        b.baseSizeMultiplier,
                        b.basePowerMultiplier * 1.12,
                        b.baseSpeedMultiplier * 1.14
                );
            }
            addToKillFeed("Turbo Brawl: +speed and +power for everyone.");
        }
    }

    private Bird findTimeoutWinner() {
        boolean teamComp = teamModeEnabled && !storyModeActive && !isTrialMode && !classicModeActive;
        if (teamComp) {
            int[] teamHealth = new int[3];
            int[] teamDamage = new int[3];
            for (int i = 0; i < activePlayers; i++) {
                Bird b = players[i];
                if (b == null || b.health <= 0) continue;
                int team = getEffectiveTeam(b.playerIndex);
                if (team < 1 || team > 2) continue;
                teamHealth[team] += (int) Math.round(b.health);
                teamDamage[team] += Math.max(0, damageDealt[b.playerIndex]);
            }
            int winnerTeam = -1;
            if (teamHealth[1] > teamHealth[2]) winnerTeam = 1;
            else if (teamHealth[2] > teamHealth[1]) winnerTeam = 2;
            else if (teamDamage[1] > teamDamage[2]) winnerTeam = 1;
            else if (teamDamage[2] > teamDamage[1]) winnerTeam = 2;
            if (winnerTeam == -1) return null;
            for (int i = 0; i < activePlayers; i++) {
                Bird b = players[i];
                if (b != null && b.health > 0 && getEffectiveTeam(b.playerIndex) == winnerTeam) {
                    return b;
                }
            }
            for (int i = 0; i < activePlayers; i++) {
                Bird b = players[i];
                if (b != null && getEffectiveTeam(b.playerIndex) == winnerTeam) {
                    return b;
                }
            }
            return null;
        }

        Bird winner = null;
        double bestHealth = -1;
        int bestDamage = -1;
        for (int i = 0; i < activePlayers; i++) {
            Bird b = players[i];
            if (b == null || b.health <= 0) continue;
            int dmg = damageDealt[i];
            if (b.health > bestHealth || (Math.abs(b.health - bestHealth) < 0.001 && dmg > bestDamage)) {
                bestHealth = b.health;
                bestDamage = dmg;
                winner = b;
            }
        }
        return winner;
    }

    private String competitionScoreLine() {
        boolean teamComp = teamModeEnabled && !storyModeActive && !isTrialMode && !classicModeActive;
        if (teamComp) {
            return "ROUND " + competitionRoundNumber + " | TEAM A " + competitionTeamWins[1] + " - TEAM B " + competitionTeamWins[2];
        }
        StringBuilder sb = new StringBuilder("ROUND ").append(competitionRoundNumber).append(" | ");
        boolean first = true;
        for (int i = 0; i < activePlayers; i++) {
            if (players[i] == null) continue;
            if (!first) sb.append("   ");
            sb.append("P").append(i + 1).append(":").append(competitionRoundWins[i]);
            first = false;
        }
        return sb.toString();
    }

    private boolean handleCompetitionRoundEnd(Bird winner) {
        if (!(competitionModeEnabled && !storyModeActive && !isTrialMode && !classicModeActive)) return false;

        boolean teamComp = teamModeEnabled && !storyModeActive && !isTrialMode && !classicModeActive;
        boolean seriesWon = false;
        String roundWinnerText = "DRAW";

        if (winner != null) {
            if (teamComp) {
                int team = getEffectiveTeam(winner.playerIndex);
                if (team == 1 || team == 2) {
                    competitionTeamWins[team]++;
                    roundWinnerText = team == 1 ? "TEAM A" : "TEAM B";
                    seriesWon = competitionTeamWins[team] >= COMPETITION_ROUND_TARGET;
                }
            } else {
                int idx = winner.playerIndex;
                if (idx >= 0 && idx < competitionRoundWins.length) {
                    competitionRoundWins[idx]++;
                    roundWinnerText = winner.name.split(":")[0].trim();
                    seriesWon = competitionRoundWins[idx] >= COMPETITION_ROUND_TARGET;
                }
            }
        }

        addToKillFeed("ROUND " + competitionRoundNumber + " WINNER: " + roundWinnerText);

        if (seriesWon) {
            competitionSeriesActive = false;
            return false;
        }

        competitionRoundNumber++;
        javafx.application.Platform.runLater(() -> {
            if (currentStage == null) return;
            resetMatchStats();
            startMatch(currentStage);
        });
        return true;
    }

    private void applyMatchModeRuntimeEffects(long now) {
        if (storyModeActive || isTrialMode) return;

        if (activeMutator == MatchMutator.LOW_GRAVITY) {
            for (int i = 0; i < activePlayers; i++) {
                Bird b = players[i];
                if (b == null || b.health <= 0) continue;
                if (b.vy > 0) b.vy *= 0.87;
            }
        } else if (activeMutator == MatchMutator.CROW_SURGE) {
            if (now - lastMutatorHazardTime > 1_000_000_000L * 6) {
                lastMutatorHazardTime = now;
                int waves = 2 + random.nextInt(2);
                for (int i = 0; i < waves; i++) {
                    double y = 220 + random.nextDouble() * (WORLD_HEIGHT - 900);
                    CrowMinion left = new CrowMinion(-120, y, null);
                    left.vx = 4.5 + random.nextDouble() * 2.0;
                    left.vy = (random.nextDouble() - 0.5) * 3.5;
                    crowMinions.add(left);

                    CrowMinion right = new CrowMinion(WORLD_WIDTH + 120, y, null);
                    right.vx = -4.5 - random.nextDouble() * 2.0;
                    right.vy = (random.nextDouble() - 0.5) * 3.5;
                    crowMinions.add(right);
                }
                addToKillFeed("MUTATOR: Crow surge wave!");
                shakeIntensity = Math.max(shakeIntensity, 12);
            }
        }
    }

    private void triggerMatchEnd(Bird winner) {
        if (matchEnded) return;

        if (handleCompetitionRoundEnd(winner)) {
            matchEnded = true;
            timer.stop();
            return;
        }

        matchEnded = true;
        final Bird finalWinner = winner;
        final Stage finalStage = currentStage;
        new AnimationTimer() {
            private int framesLeft = 100;
            @Override public void handle(long now) {
                if (framesLeft > 0) {
                    framesLeft--;
                    if (finalWinner != null && framesLeft % 3 == 0) {
                        for (int i = 0; i < 15; i++) {
                            double angle = Math.random() * Math.PI * 2;
                            double spd = 6 + Math.random() * 16;
                            particles.add(new Particle(finalWinner.x + 40, finalWinner.y + 40, Math.cos(angle) * spd, Math.sin(angle) * spd - 7, Color.GOLD.deriveColor(0, 1, 1, 0.95)));
                        }
                    }
                    return;
                }
                this.stop();
                timer.stop();
                recordBalanceOutcome(finalWinner);
                showMatchSummary(finalStage, finalWinner);
                if (finalWinner != null && finalWinner.health < 20 && finalWinner.health > 0 && !achievementsUnlocked[9]) {
                    unlockAchievement(9, "CLUTCH GOD!");
                }
            }
        }.start();
    }

    private void spawnPowerUp(long now) {
        if (competitionModeEnabled && !storyModeActive && !classicModeActive) return;
        if (now - lastPowerUpSpawnTime < activePowerUpSpawnInterval) return;
        double spawnChance = activeMutator == MatchMutator.POWERUP_STORM ? 0.98 : 0.8;
        if (random.nextDouble() < spawnChance) {
            double[] spawn = pickPowerUpSpawnPoint();
            double x = spawn[0];
            double y = spawn[1];
            PowerUpType type;
            if (activeMutator == MatchMutator.POWERUP_STORM && random.nextDouble() < 0.22) {
                PowerUpType[] storm = {
                        PowerUpType.NEON, PowerUpType.THERMAL, PowerUpType.RAGE,
                        PowerUpType.SPEED, PowerUpType.HEALTH, PowerUpType.OVERCHARGE,
                        PowerUpType.FROST_NOVA, PowerUpType.TITAN
                };
                type = storm[random.nextInt(storm.length)];
            } else if (selectedMap == MapType.SKYCLIFFS && random.nextDouble() < 0.35) {
                type = PowerUpType.THERMAL;
            } else if (selectedMap == MapType.CITY && random.nextDouble() < 0.4) {
                type = PowerUpType.NEON;
            } else if (selectedMap == MapType.VIBRANT_JUNGLE && random.nextDouble() < 0.4) {
                type = PowerUpType.VINE_GRAPPLE;                                    // NEW
            } else {
                PowerUpType[] common = {
                        PowerUpType.HEALTH, PowerUpType.SPEED, PowerUpType.RAGE, PowerUpType.SHRINK,
                        PowerUpType.OVERCHARGE, PowerUpType.FROST_NOVA, PowerUpType.TITAN
                };
                type = common[random.nextInt(common.length)];
            }
            powerUps.add(new PowerUp(x, y, type));
            lastPowerUpSpawnTime = now;
        } else {
            lastPowerUpSpawnTime = now;
        }
    }

    private double[] pickPowerUpSpawnPoint() {
        // Prefer platform tops so pickups are reachable and visible.
        if (!platforms.isEmpty() && random.nextDouble() < 0.72) {
            List<Platform> candidates = new ArrayList<>();
            for (Platform p : platforms) {
                if (p.w >= 140 && p.y <= GROUND_Y - 30 && p.y > 120) {
                    candidates.add(p);
                }
            }
            if (!candidates.isEmpty()) {
                Platform p = candidates.get(random.nextInt(candidates.size()));
                double x = p.x + 40 + random.nextDouble() * Math.max(20, p.w - 80);
                double y = Math.max(120, p.y - 45);
                return new double[]{x, y};
            }
        }

        double x = 200 + random.nextDouble() * (WORLD_WIDTH - 400);
        // Clamp to above ground so power-ups never spawn beneath the floor.
        double y = 260 + random.nextDouble() * (GROUND_Y - 520);
        y = Math.min(y, GROUND_Y - 120);
        return new double[]{x, y};
    }

    private void handleGameplayKeyPress(Stage stage, KeyEvent e) {
        pressedKeys.add(e.getCode());
        if (e.getCode() == KeyCode.ESCAPE) {
            togglePause(stage);
        } else if (e.getCode() == KeyCode.F11) {
            stage.setFullScreen(!stage.isFullScreen());
        } else if (e.getCode() == KeyCode.F3) {
            debugHudEnabled = !debugHudEnabled;
            addToKillFeed(debugHudEnabled ? "DEBUG HUD: ON" : "DEBUG HUD: OFF");
        }
    }

    private void drawDebugBalanceHud(GraphicsContext g) {
        if (!debugHudEnabled) return;

        double panelX = 20;
        double panelY = 20;
        double panelW = 760;
        double panelH = 170 + activePlayers * 26;

        g.setFill(Color.BLACK.deriveColor(0, 1, 1, 0.78));
        g.fillRoundRect(panelX, panelY, panelW, panelH, 18, 18);
        g.setStroke(Color.CYAN.brighter());
        g.setLineWidth(2.5);
        g.strokeRoundRect(panelX, panelY, panelW, panelH, 18, 18);

        double elapsedSec = Math.max(1.0, matchStartNano > 0
                ? (System.nanoTime() - matchStartNano) / 1_000_000_000.0
                : 1.0);

        g.setFill(Color.CYAN.brighter());
        g.setFont(Font.font("Consolas", FontWeight.BOLD, 24));
        g.fillText("DEBUG BALANCE HUD (F3)", panelX + 18, panelY + 32);

        g.setFill(Color.LIGHTGRAY);
        g.setFont(Font.font("Consolas", 17));
        g.fillText("Match " + (int) elapsedSec + "s | Map: " + selectedMap + " | Trial: " + (isTrialMode ? "Yes" : "No"),
                panelX + 18, panelY + 58);

        g.setFill(Color.WHITE);
        g.setFont(Font.font("Consolas", 16));
        g.fillText("Player                        DPS   Specials  Elims  Falls  WRÎ”", panelX + 18, panelY + 84);

        for (int i = 0; i < activePlayers; i++) {
            Bird b = players[i];
            if (b == null) continue;

            double dps = damageDealt[i] / elapsedSec;
            int picks = typePicks[b.type.ordinal()];
            int wins = typeWins[b.type.ordinal()];
            double wr = picks > 0 ? wins / (double) picks : 0.5;
            int wrDelta = (int) Math.round((wr - 0.5) * 100.0);
            String wrText = (wrDelta >= 0 ? "+" : "") + wrDelta + "%";

            String row = String.format(
                    "%-28s %5.1f %8d %7d %6d %6s",
                    b.name.length() > 28 ? b.name.substring(0, 28) : b.name,
                    dps,
                    specialsUsed[i],
                    eliminations[i],
                    falls[i],
                    wrText
            );
            g.setFill(b.type.color.brighter());
            g.fillText(row, panelX + 18, panelY + 112 + i * 26);
        }

        g.setFill(Color.GRAY.brighter());
        g.setFont(Font.font("Consolas", 14));
        g.fillText("WRÎ” = bird type lifetime win-rate delta from 50%", panelX + 18, panelY + panelH - 10);
    }

    public void recordSpecialImpact(int playerIdx, int damage, boolean didHit) {
        if (playerIdx < 0 || playerIdx >= specialHits.length) return;
        if (didHit) specialHits[playerIdx]++;
        if (damage > 0) specialDamageDealt[playerIdx] += damage;
    }

    public int getEffectiveTeam(int playerIdx) {
        if (playerIdx < 0 || playerIdx >= players.length) return -1;
        if (isTrialMode) return playerIdx <= 2 ? 1 : 2;
        if (classicModeActive) {
            if (!classicTeamMode) return playerIdx;
            return classicTeams[playerIdx] <= 0 ? 1 : classicTeams[playerIdx];
        }
        if (storyModeActive) {
            if (storyTeamMode) {
                return storyTeams[playerIdx] <= 0 ? 1 : storyTeams[playerIdx];
            }
            return playerIdx;
        }
        if (!teamModeEnabled) return playerIdx;
        return playerTeams[playerIdx] == 2 ? 2 : 1;
    }

    public boolean areAllies(int playerA, int playerB) {
        if (playerA < 0 || playerA >= players.length || playerB < 0 || playerB >= players.length) return false;
        return getEffectiveTeam(playerA) == getEffectiveTeam(playerB);
    }

    public boolean canDamage(Bird attacker, Bird target) {
        if (attacker == null || target == null || attacker == target) return false;
        if (target.health <= 0) return false;
        return !areAllies(attacker.playerIndex, target.playerIndex);
    }

    public void addToKillFeed(String message) {
        killFeed.add(0, message);  // newest on top
        if (killFeed.size() > MAX_FEED_LINES) {
            killFeed.remove(killFeed.size() - 1);
        }
    }

    public void triggerFlash(double whiteIntensity, boolean isKill) {
        flashAlpha = whiteIntensity;
        redFlashAlpha = isKill ? 0.0 : whiteIntensity * 0.55;
        flashTimer = isKill ? 22 : 14;

        if (isKill) {
            hitstopFrames = Math.max(hitstopFrames, 20);
            shakeIntensity = Math.max(shakeIntensity, 30);
        }
// Global cap to prevent freeze
        hitstopFrames = Math.min(hitstopFrames, 25);
    }

    private void startMatch(Stage stage) { 
        isTrialMode = false;
        matchTimer = MATCH_DURATION_FRAMES;
        storyMatchTimerOverride = -1;
        storyTeamMode = false;
        Arrays.fill(storyTeams, 1);
        if (!classicModeActive) {
            classicEncounter = null;
            classicTeamMode = false;
            Arrays.fill(classicTeams, 1);
        }
        activeMutator = MatchMutator.NONE;
        activePowerUpSpawnInterval = POWERUP_SPAWN_INTERVAL;
        lastMutatorHazardTime = System.nanoTime();
        resetSuddenDeathState();
        matchStartNano = System.nanoTime();
        balanceOutcomeRecorded = false;
        this.currentStage = stage;
        Arrays.fill(scores, 0);
        Arrays.fill(players, null);
        Arrays.fill(isAI, false);
        StoryChapter storyChapter = null;
        ClassicEncounter classicRound = null;
        if (classicModeActive) {
            if (classicRun.isEmpty()) {
                classicRun.addAll(buildClassicRun(classicSelectedBird));
                classicRoundIndex = 0;
            }
            if (classicRoundIndex < 0 || classicRoundIndex >= classicRun.size()) {
                classicRoundIndex = 0;
            }
            classicRound = classicRun.get(classicRoundIndex);
            classicEncounter = classicRound;
            setupClassicEncounterRoster(classicRound);
        } else if (storyModeActive) {
            StoryChapter[] chapters = activeStoryChapters();
            if (storyChapterIndex < 0 || storyChapterIndex >= chapters.length) {
                storyChapterIndex = 0;
            }
            storyChapter = chapters[storyChapterIndex];
            setupStoryChapterRoster(storyChapter);
        } else {
            for (int i = 0; i < activePlayers; i++) {
                VBox selectorBox = (VBox) playerSlots[i].getChildren().get(2);
                Object data = selectorBox.getUserData();

                BirdType type;
                boolean useCitySkin = false;
                boolean useNoirSkin = false;
                boolean useClassicSkin = false;

                if (data instanceof String) {
                    if ("CITY_PIGEON".equals(data)) {
                        type = BirdType.PIGEON;
                        useCitySkin = true;
                    } else if ("NOIR_PIGEON".equals(data)) {
                        type = BirdType.PIGEON;
                        useNoirSkin = true;
                    } else if ("SKY_KING_EAGLE".equals(data)) {
                        type = BirdType.EAGLE;
                        useClassicSkin = true;
                    } else if (((String) data).startsWith("CLASSIC_SKIN_")) {
                        String token = ((String) data).substring("CLASSIC_SKIN_".length());
                        BirdType parsed;
                        try {
                            parsed = BirdType.valueOf(token);
                        } catch (IllegalArgumentException ex) {
                            parsed = BirdType.PIGEON;
                        }
                        type = parsed;
                        useClassicSkin = true;
                    } else {
                        type = BirdType.PIGEON;
                    }
                } else {
                    type = (BirdType) data;
                }
                if (!isBirdUnlocked(type)) {
                    type = firstUnlockedBird();
                }

                double startX = 300 + i * (WIDTH - 600) / Math.max(1, activePlayers - 1);
                players[i] = new Bird(startX, type, i, this);

                if (useCitySkin) {
                    players[i].isCitySkin = true;
                }
                if (useNoirSkin) {
                    players[i].isNoirSkin = true;
                }
                if (useClassicSkin && type != BirdType.PIGEON) {
                    players[i].isClassicSkin = true;
                }

                applyAdaptiveBalance(players[i]);
            }
        }

        platforms.clear();
        windVents.clear();
        nectarNodes.clear();
        swingingVines.clear();
        crowMinions.clear();
        particles.clear();
        powerUps.clear();

        if (selectedMap != MapType.CITY) cityStars.clear();

        if (selectedMap == MapType.CITY && cityStars.isEmpty()) {
            for (int i = 0; i < 250; i++) {
                cityStars.add(new double[]{
                        random.nextDouble() * WORLD_WIDTH,
                        random.nextDouble() * (GROUND_Y - 200)
                });
            }
        }

// Common elements for both maps
        platforms.add(new Platform(0, GROUND_Y, WORLD_WIDTH, 600)); // thick floor
        platforms.add(new Platform(-100, 0, 100, WORLD_HEIGHT)); // left wall
        platforms.add(new Platform(WORLD_WIDTH, 0, 100, WORLD_HEIGHT)); // right wall

        if (selectedMap == MapType.FOREST) {
            // === ORIGINAL FOREST PLATFORMS (unchanged) ===
            double[] treeX = {800, 2100, 3400, 4800, 5600};
            for (double tx : treeX) {
                platforms.add(new Platform(tx - 200, GROUND_Y - 800, 400, 30));
                platforms.add(new Platform(tx - 150, GROUND_Y - 500, 300, 30));
                platforms.add(new Platform(tx - 100, GROUND_Y - 200, 200, 30));
            }
            Random rand = new Random();
            for (int i = 0; i < 25; i++) {
                double px = 300 + rand.nextDouble() * (WORLD_WIDTH - 600);
                double py = 800 + rand.nextDouble() * (GROUND_Y - 1000);
                double pw = 200 + rand.nextDouble() * 400;
                platforms.add(new Platform(px, py, pw, 30));
            }
        } else if (selectedMap == MapType.SKYCLIFFS) {
            // === SKY CLIFFS PLATFORMS - More accessible with stepping stones ===
            double[] cliffX = {600, 1600, 2600, 3600, 4600, 5400};

            // Main large ledges (keep for epic fights)
            for (double cx : cliffX) {
                platforms.add(new Platform(cx - 400, GROUND_Y - 600, 800, 60));  // main wide
                platforms.add(new Platform(cx - 200, GROUND_Y - 1000, 400, 50)); // higher
                platforms.add(new Platform(cx - 100, GROUND_Y - 1400, 200, 40)); // small high
            }

            // NEW: Stepping stone chains from ground to highs (spaced for short jumps)
            // Left side chain
            platforms.add(new Platform(800, GROUND_Y - 200, 400, 50));
            platforms.add(new Platform(700, GROUND_Y - 450, 350, 50));
            platforms.add(new Platform(900, GROUND_Y - 700, 400, 50));
            platforms.add(new Platform(750, GROUND_Y - 950, 300, 50));

            // Central chain
            platforms.add(new Platform(2200, GROUND_Y - 180, 500, 50));
            platforms.add(new Platform(2100, GROUND_Y - 420, 450, 50));
            platforms.add(new Platform(2300, GROUND_Y - 660, 500, 50));
            platforms.add(new Platform(2200, GROUND_Y - 900, 400, 50));
            platforms.add(new Platform(2150, GROUND_Y - 1150, 350, 50));

            // Right side chain
            platforms.add(new Platform(4200, GROUND_Y - 220, 450, 50));
            platforms.add(new Platform(4100, GROUND_Y - 480, 400, 50));
            platforms.add(new Platform(4300, GROUND_Y - 740, 450, 50));
            platforms.add(new Platform(4200, GROUND_Y - 1000, 350, 50));

            // Extra mid-level connectors
            platforms.add(new Platform(1000, GROUND_Y - 800, 500, 50));
            platforms.add(new Platform(3000, GROUND_Y - 900, 600, 50));
            platforms.add(new Platform(5000, GROUND_Y - 700, 500, 50));

            // Wind vents (thermal updrafts) - keep + add a few low ones for early boosts
            for (double cx : cliffX) {
                windVents.add(new WindVent(cx - 300, GROUND_Y - 600, 600));
                windVents.add(new WindVent(cx - 150, GROUND_Y - 1000, 300));
            }
            // Low vents for short jumpers
            windVents.add(new WindVent(700, GROUND_Y - 250, 400));
            windVents.add(new WindVent(2100, GROUND_Y - 230, 500));
            windVents.add(new WindVent(4100, GROUND_Y - 270, 450));

            // Fixed mountain peaks
            mountainPeaks = new double[7];
            double[] mountainX = {0, 800, 1800, 2800, 3800, 4800, WORLD_WIDTH};
            for (int i = 0; i < mountainX.length - 1; i++) {
                mountainPeaks[i] = GROUND_Y - 400 - random.nextDouble() * 600;
            }
        } else if (selectedMap == MapType.VIBRANT_JUNGLE) {
            // Clear new collections
            nectarNodes.clear();
            swingingVines.clear();

            // Kapok tree platforms: tall, layered branches
            double[] treeX = {600, 1600, 2600, 3600, 4600, 5400};
            for (double tx : treeX) {
                platforms.add(new Platform(tx - 350, GROUND_Y - 2000, 700, 60)); // High canopy
                platforms.add(new Platform(tx - 250, GROUND_Y - 1500, 500, 50)); // Mid-high
                platforms.add(new Platform(tx - 200, GROUND_Y - 1000, 400, 50)); // Mid
                platforms.add(new Platform(tx - 300, GROUND_Y - 500, 600, 60)); // Low branches
            }

            // Floating orchid platforms (small, scattered)
            Random rand = new Random();
            for (int i = 0; i < 40; i++) {
                double px = 200 + rand.nextDouble() * (WORLD_WIDTH - 400);
                double py = 400 + rand.nextDouble() * (GROUND_Y - 600);
                double pw = 100 + rand.nextDouble() * 200;
                platforms.add(new Platform(px, py, pw, 40));
                // 30% chance for nectar node on platform
                if (rand.nextDouble() < 0.3) {
                    nectarNodes.add(new NectarNode(px + pw/2, py - 40, rand.nextBoolean()));
                }
            }

            // Waterfall ledges (wide, stable)
            platforms.add(new Platform(800, GROUND_Y - 1300, 1000, 70));
            platforms.add(new Platform(3200, GROUND_Y - 1400, 900, 60));
            platforms.add(new Platform(5000, GROUND_Y - 1200, 1100, 70));

            // Swinging vines (attached to high platforms)
            SwingingVine vine1 = new SwingingVine(1200, GROUND_Y - 2000, 400);
            vine1.angle = -0.3;
            vine1.angularVelocity = 0.02;
            swingingVines.add(vine1);

            SwingingVine vine2 = new SwingingVine(2000, GROUND_Y - 1500, 350);
            vine2.angle = 0.4;
            swingingVines.add(vine2);

            SwingingVine vine3 = new SwingingVine(3000, GROUND_Y - 2000, 450);
            vine3.angle = -0.2;
            vine3.angularVelocity = -0.01;
            swingingVines.add(vine3);

            SwingingVine vine4 = new SwingingVine(4000, GROUND_Y - 1500, 300);
            vine4.angle = 0.35;
            swingingVines.add(vine4);

            SwingingVine vine5 = new SwingingVine(5200, GROUND_Y - 2000, 400);
            vine5.angle = -0.25;
            vine5.angularVelocity = 0.015;
            swingingVines.add(vine5);

            // Mist breezes (gentle upward vents)
            double[] breezeX = {1000, 2000, 3000, 4000, 5000};
            for (double bx : breezeX) {
                windVents.add(new WindVent(bx - 200, GROUND_Y - 1000, 400)); // Mid-level
                windVents.add(new WindVent(bx - 150, GROUND_Y - 1600, 300)); // High
                windVents.add(new WindVent(bx - 100, GROUND_Y - 400, 300)); // Low
            }

            // Canopy peaks for background
            mountainPeaks = new double[7];
            double[] canopyX = {0, 800, 1800, 2800, 3800, 4800, WORLD_WIDTH};
            for (int i = 0; i < mountainPeaks.length; i++) {
                mountainPeaks[i] = GROUND_Y - 800 - rand.nextDouble() * 1000;
            }
        } else if (selectedMap == MapType.CAVE) {
            // Hard cave ceiling to support bat hanging and vertical combat lanes.
            platforms.add(new Platform(0, 0, WORLD_WIDTH, 70));

            // Main cavern shelves
            platforms.add(new Platform(500, GROUND_Y - 420, 900, 55));
            platforms.add(new Platform(1850, GROUND_Y - 620, 760, 55));
            platforms.add(new Platform(3000, GROUND_Y - 500, 920, 55));
            platforms.add(new Platform(4350, GROUND_Y - 700, 760, 55));
            platforms.add(new Platform(5200, GROUND_Y - 430, 620, 55));

            // Stalactite hangs (underside-friendly narrow ledges)
            double[] stalactiteX = {650, 1300, 2050, 2800, 3550, 4300, 5050, 5650};
            for (double sx : stalactiteX) {
                platforms.add(new Platform(sx - 140, 240 + random.nextDouble() * 300, 280, 34));
            }

            // Midair crystal bridges
            for (int i = 0; i < 12; i++) {
                double px = 260 + i * 470 + random.nextDouble() * 120;
                double py = 500 + random.nextDouble() * (GROUND_Y - 900);
                platforms.add(new Platform(px, py, 220 + random.nextDouble() * 180, 30));
            }

            // Vertical shafts with updraft pockets
            double[] ventX = {950, 2400, 3900, 5400};
            for (double vx : ventX) {
                windVents.add(new WindVent(vx - 180, GROUND_Y - 820, 360));
                windVents.add(new WindVent(vx - 130, GROUND_Y - 1450, 260));
            }

            mountainPeaks = new double[7];
            for (int i = 0; i < mountainPeaks.length; i++) {
                mountainPeaks[i] = GROUND_Y - 1000 - random.nextDouble() * 800;
            }
        } else { // CITY - Clean, structured nighttime rooftops
            // Main building rooftops (wide, aligned platforms)
            double[] buildingX = {400, 1400, 2400, 3400, 4400, 5400};
            for (double bx : buildingX) {
                platforms.add(new Platform(bx - 350, GROUND_Y - 320, 700, 50));
                platforms.add(new Platform(bx - 200, GROUND_Y - 120, 400, 40));

                Platform high1 = new Platform(bx - 150, GROUND_Y - 720, 300, 40);
                Platform high2 = new Platform(bx + 50, GROUND_Y - 1020, 200, 40);

                String[] possibleSigns = {"CLUB", "BAR", "HOTEL", "EAT", "LIVE", "24/7", "OPEN", "PIZZA", "DIVE", "LOUNGE"};
                high1.signText = possibleSigns[random.nextInt(possibleSigns.length)];
                high2.signText = possibleSigns[random.nextInt(possibleSigns.length)];

                platforms.add(high1);
                platforms.add(high2);

                // Wind vents (unchanged)
                windVents.add(new WindVent(bx - 350 + 100, GROUND_Y - 320, 500));
                windVents.add(new WindVent(bx - 350 + 500, GROUND_Y - 320, 100));
            }

            // Smaller connecting platforms between buildings
            platforms.add(new Platform(800, GROUND_Y - 450, 400, 40));
            platforms.add(new Platform(1800, GROUND_Y - 600, 400, 40));
            platforms.add(new Platform(2800, GROUND_Y - 400, 400, 40));
            platforms.add(new Platform(3800, GROUND_Y - 550, 400, 40));
            platforms.add(new Platform(4800, GROUND_Y - 480, 400, 40));

            // A few lower street-level platforms
            platforms.add(new Platform(1000, GROUND_Y - 100, 600, 40));
            platforms.add(new Platform(3000, GROUND_Y - 150, 500, 40));
            platforms.add(new Platform(4500, GROUND_Y - 80, 400, 40));

            // NO random floating pillars/antennas â€” removed completely

            // Rooftop vents already added in the building loop above.
        }

        if (storyModeActive && storyChapter != null) {
            applyStoryChapterArenaModifiers(storyChapter);
            if (storyMatchTimerOverride > 0) {
                matchTimer = storyMatchTimerOverride;
            }
        }
        if (classicModeActive && classicRound != null) {
            applyClassicEncounterArenaModifiers(classicRound);
            if (classicRound.timerFrames > 0) {
                matchTimer = classicRound.timerFrames;
            }
        }
        configureMatchModes();

        Canvas canvas = new Canvas(WIDTH, HEIGHT);
        GraphicsContext g = canvas.getGraphicsContext2D();
        StackPane root = new StackPane(canvas);
        gameRoot = root;
        root.setStyle(selectedMap == MapType.FOREST
                ? "-fx-background-color: linear-gradient(to bottom, #87CEEB 0%, #B3E5FC 50%, #E0F2F1 100%);"
                : "-fx-background-color: #000011;");
        Scene scene = new Scene(root, WIDTH, HEIGHT);
        makeSceneResponsive(scene);
        uiCanvas = new Canvas(WIDTH, HEIGHT);
        ui = uiCanvas.getGraphicsContext2D();
        root.getChildren().add(uiCanvas);

        pressedKeys.clear();
        scene.setOnKeyPressed(e -> handleGameplayKeyPress(stage, e));
        scene.setOnKeyReleased(e -> pressedKeys.remove(e.getCode()));

        if (timer != null) timer.stop();
        lastUpdate = 0;
        accumulator = 0;

        timer = new AnimationTimer() {
            @Override
            public void handle(long now) {
                gameTick(1.0);
                drawGame(g);   // â† ONE LINE

                ui.clearRect(0, 0, WIDTH, HEIGHT);

                // Draw scores
                ui.setFill(Color.WHITE);
                ui.setFont(Font.font("Arial Black", 32));
                ui.fillText("SCORES:", 50, 150);
                for (int i = 0; i < activePlayers; i++) {
                    if (players[i] != null) {
                        ui.fillText(players[i].name.split(":")[0] + ": " + scores[i], 50, 190 + i * 40);
                    }
                }
// Minimap: bottom-right, 200x150, scaled world view
                double mapW = 200, mapH = 150;
                ui.setFill(Color.BLACK.deriveColor(0,1,1,0.6));
                ui.fillRect(WIDTH - mapW - 20, HEIGHT - mapH - 20, mapW, mapH);
                ui.setStroke(Color.WHITE);
                ui.strokeRect(WIDTH - mapW - 20, HEIGHT - mapH - 20, mapW, mapH);
// Draw birds as dots
                double scaleX = mapW / WORLD_WIDTH;
                double scaleY = mapH / WORLD_HEIGHT;
                for (Bird b : players) {
                    if (b != null && b.health > 0) {
                        ui.setFill(b.type.color);
                        double mx = b.x * scaleX;
                        double my = b.y * scaleY;
                        ui.fillOval(WIDTH - mapW - 20 + mx - 4, HEIGHT - mapH - 20 + my - 4, 8, 8);
                    }
                }
// Camera view rect
                double camViewW = (WIDTH / zoom) * scaleX;
                double camViewH = (HEIGHT / zoom) * scaleY;
                ui.setStroke(Color.YELLOW);
                ui.strokeRect(WIDTH - mapW - 20 + camX * scaleX, HEIGHT - mapH - 20 + camY * scaleY, camViewW, camViewH);

                // === CENTERED HEALTH BARS (works for 2/3/4 players) ===
                double barWidth = 400;
                double gap = 20;
                double totalWidth = activePlayers * (barWidth + gap) - gap;
                double startX = (WIDTH - totalWidth) / 2.0;
                double healthBarY = 80;

                for (int i = 0; i < activePlayers; i++) {
                    if (players[i] != null && players[i].health > 0) {
                        drawHealthBar(ui, players[i], startX + i * (barWidth + gap), healthBarY);
                    }
                }

                if (activeMutator != MatchMutator.NONE && !competitionModeEnabled && !storyModeActive && !isTrialMode && !classicModeActive) {
                    ui.setFill(Color.web("#80DEEA"));
                    ui.setFont(Font.font("Arial Black", 24));
                    ui.fillText("MUTATOR: " + activeMutator.label.toUpperCase(), WIDTH / 2 - 220, healthBarY + 78);
                }
                if (classicModeActive && classicEncounter != null) {
                    ui.setFill(Color.web("#FFE082"));
                    ui.setFont(Font.font("Arial Black", 22));
                    ui.fillText(
                            "CLASSIC " + (classicRoundIndex + 1) + "/" + classicRun.size() + "  " + classicEncounter.name.toUpperCase(),
                            WIDTH / 2 - 360,
                            healthBarY + 104
                    );
                    ui.setFill(Color.web("#B3E5FC"));
                    ui.setFont(Font.font("Consolas", 20));
                    ui.fillText("TWIST: " + classicEncounter.twist.label, WIDTH / 2 - 160, healthBarY + 130);
                    ui.setFill(Color.web("#FFF59D"));
                    ui.fillText("MUTATOR: " + classicEncounter.mutator.label, WIDTH / 2 - 170, healthBarY + 154);
                }
                if (competitionModeEnabled && !storyModeActive && !isTrialMode && !classicModeActive) {
                    ui.setFill(Color.web("#FFD54F"));
                    ui.setFont(Font.font("Arial Black", 22));
                    ui.fillText("COMPETITION MODE", WIDTH / 2 - 160, healthBarY + 78);
                    ui.setFill(Color.web("#FFF59D"));
                    ui.setFont(Font.font("Consolas", 21));
                    ui.fillText(competitionScoreLine(), WIDTH / 2 - 245, healthBarY + 104);
                }

                ui.setFill(Color.WHITE);
                ui.setFont(Font.font("Arial Black", 48));
                String timeText = matchTimer > 0 ? String.format("TIME: %d", matchTimer / 60) : "SUDDEN DEATH!";
                if (matchTimer <= 600 && matchTimer > 0) {
                    ui.setFill(Color.RED.brighter());
                    ui.setEffect(new Glow(0.8));
                } else if (matchTimer <= 0) {
                    ui.setFill(Color.CRIMSON.darker());
                    ui.setEffect(new Glow(1.2));
                }
                ui.fillText(timeText, WIDTH / 2 - 120, 80);
                ui.setEffect(null);
                ui.setFont(Font.font("Arial Black", 26));
                for (int i = 0; i < killFeed.size(); i++) {
                    String msg = killFeed.get(i);
                    double alpha = Math.max(0.3, 1.0 - (i / (double) MAX_FEED_LINES));
                    ui.setFill(new Color(1, 1, 1, alpha));
                    ui.fillText(msg, WIDTH - 700, 80 + i * 50);
                    ui.setFill(new Color(0, 0, 0, alpha * 0.6));
                    ui.fillText(msg, WIDTH - 698, 78 + i * 50);
                }
                ui.setFill(new Color(0, 0, 0, 0.75));
                ui.fillRoundRect(20, HEIGHT - 70, WIDTH - 40, 60, 20, 20);
                ui.setFill(Color.WHITE);
                ui.setFont(Font.font(22));
                ui.fillText("Controls: WASD+Space+Shift | Q/E=Taunt | P2=Arrows+Enter+Slash | P3=TFGH+Y+U | P4=IJKL+O+P | S/Down/G/K=Block", 50, HEIGHT - 35);
                drawDebugBalanceHud(ui);
                if (flashTimer > 0) {
                    flashTimer--;
                    if (flashAlpha > 0) {
                        ui.setFill(Color.WHITE.deriveColor(0, 1, 1, flashAlpha));
                        ui.fillRect(0, 0, WIDTH, HEIGHT);
                        flashAlpha *= 0.89;
                    }
                    if (redFlashAlpha > 0) {
                        ui.setFill(Color.RED.deriveColor(0, 1, 1, redFlashAlpha));
                        ui.fillRect(0, 0, WIDTH, HEIGHT);
                        redFlashAlpha *= 0.86;
                    }
                }
            }
        };

        timer.start();
        setScenePreservingFullscreen(stage, scene);
        startMusic();
        canvas.requestFocus();

        zoom = 0.6;
        camX = 1000;
        camY = 800;
    }

    private void drawHealthBar(GraphicsContext g, Bird b, double x, double y) {
        g.setFill(Color.BLACK);
        g.fillRoundRect(x - 3, y - 3, 406, 46, 10, 10);
        g.setFill(Color.RED);
        g.fillRoundRect(x, y, 400, 40, 10, 10);
        boolean compStyle = competitionModeEnabled && !storyModeActive && !isTrialMode && !classicModeActive;
        g.setFill(compStyle ? Color.DODGERBLUE : Color.LIME);
        g.fillRoundRect(x, y, b.health * 4, 40, 10, 10);
        g.setFill(Color.WHITE);
        g.setFont(Font.font("Arial Black", 28));
        g.fillText(b.name + " " + b.health + "%", x + 20, y + 32);
    }

    private void showMatchSummary(Stage stage, Bird winner) {
        if (musicPlayer != null) musicPlayer.stop();
        if (menuMusicPlayer != null) menuMusicPlayer.stop();
        if (victoryMusicPlayer != null) {
            victoryMusicPlayer.stop();
            victoryMusicPlayer.play();
        }
        VBox root = new VBox(40);
        root.setAlignment(Pos.CENTER);
        root.setPadding(new Insets(60));
        root.setStyle("-fx-background-color: linear-gradient(to bottom, #0f0c29, #302b63, #24243e);");

        Label title = new Label(winner != null ? winner.name.toUpperCase() + " WINS!" : "TIME'S UP!");
        title.setFont(Font.font("Arial Black", 110));
        title.setTextFill(winner != null ? Color.GOLD : Color.SILVER);
        title.setEffect(new DropShadow(50, Color.BLACK));

        // Collect all active birds
        List<Bird> activeBirds = new ArrayList<>();
        for (int i = 0; i < activePlayers; i++) {
            if (players[i] != null) activeBirds.add(players[i]);
        }

        HBox buttons = buildSummaryButtons(stage, winner);
        applyWinnerMapProgress(winner);

        boolean teamSummaryMode =
                (teamModeEnabled && !isTrialMode && !storyModeActive && !classicModeActive)
                        || (storyModeActive && storyTeamMode)
                        || (classicModeActive && classicTeamMode);
        if (teamSummaryMode) {
            int winningTeam = winner != null ? getEffectiveTeam(winner.playerIndex) : -1;
            title.setText(winningTeam > 0 ? teamLabel(winningTeam) + " WINS!" : "TIME'S UP!");
            title.setTextFill(winningTeam > 0 ? teamColor(winningTeam).brighter() : Color.SILVER);

            Map<Integer, List<Bird>> teams = new HashMap<>();
            for (Bird bird : activeBirds) {
                int teamId = getEffectiveTeam(bird.playerIndex);
                teams.computeIfAbsent(teamId, t -> new ArrayList<>()).add(bird);
            }

            List<Integer> ranking = new ArrayList<>(teams.keySet());
            ranking.sort((a, b) -> {
                int scoreA = 0;
                int scoreB = 0;
                for (Bird bird : teams.get(a)) {
                    scoreA += eliminations[bird.playerIndex] * 100 + damageDealt[bird.playerIndex] + (int) bird.health;
                }
                for (Bird bird : teams.get(b)) {
                    scoreB += eliminations[bird.playerIndex] * 100 + damageDealt[bird.playerIndex] + (int) bird.health;
                }
                return Integer.compare(scoreB, scoreA);
            });

            HBox podium = new HBox(70);
            podium.setAlignment(Pos.CENTER);

            for (int idx = 0; idx < ranking.size(); idx++) {
                int teamId = ranking.get(idx);
                List<Bird> members = teams.get(teamId);

                int kills = 0;
                int damage = 0;
                int taunts = 0;
                int fallsCount = 0;
                for (Bird bird : members) {
                    kills += eliminations[bird.playerIndex];
                    damage += damageDealt[bird.playerIndex];
                    taunts += tauntsPerformed[bird.playerIndex];
                    fallsCount += falls[bird.playerIndex];
                }

                String award;
                if (idx == 0) award = "TEAM CHAMPIONS";
                else if (damage >= 350) award = "BRUTE SQUAD";
                else if (kills >= 5) award = "FINISHER CREW";
                else if (taunts >= 4) award = "TRASH TALKERS";
                else if (fallsCount >= 3) award = "RISK TAKERS";
                else award = "WILD CARD";

                String membersText = members.stream()
                        .map(b -> b.name.split(":")[0].trim())
                        .reduce((a, b) -> a + " + " + b)
                        .orElse("-");

                VBox slot = new VBox(12);
                slot.setAlignment(Pos.CENTER);
                slot.setPadding(new Insets(25));
                slot.setStyle("-fx-background-color: rgba(0,0,0,0.5); -fx-background-radius: 25;");

                String placeText = (idx == 0) ? "1ST" : (idx == 1 ? "2ND" : "3RD");
                Label placeLabel = new Label(placeText);
                placeLabel.setFont(Font.font("Arial Black", 64));
                placeLabel.setTextFill(idx == 0 ? Color.GOLD : idx == 1 ? Color.SILVER : Color.web("#CD7F32"));

                Label teamName = new Label(teamLabel(teamId));
                teamName.setFont(Font.font("Arial Black", 50));
                teamName.setTextFill(teamColor(teamId));

                Label memberLabel = new Label(membersText);
                memberLabel.setFont(Font.font("Arial Black", 28));
                memberLabel.setTextFill(Color.WHITE);

                Label statsLabel = new Label("DMG " + damage + "  KILLS " + kills);
                statsLabel.setFont(Font.font("Consolas", 24));
                statsLabel.setTextFill(Color.LIGHTGRAY);

                Label awardLabel = new Label(award);
                awardLabel.setFont(Font.font("Arial Black", 30));
                awardLabel.setTextFill(Color.CYAN.brighter());
                awardLabel.setEffect(new Glow(0.8));

                slot.getChildren().addAll(placeLabel, teamName, memberLabel, statsLabel, awardLabel);
                podium.getChildren().add(slot);
            }

            VBox analyticsPanel = buildPostMatchAnalyticsPanel(activeBirds);
            root.getChildren().addAll(title, podium, analyticsPanel, buttons);
            setScenePreservingFullscreen(stage, new Scene(root, WIDTH, HEIGHT));
            return;
        }

        // Sort by performance (same as before)
        activeBirds.sort((a, b) -> {
            if (a == winner) return -1;
            if (b == winner) return 1;
            int scoreA = eliminations[a.playerIndex] * 100 + damageDealt[a.playerIndex] + (int) a.health;
            int scoreB = eliminations[b.playerIndex] * 100 + damageDealt[b.playerIndex] + (int) b.health;
            return Integer.compare(scoreB, scoreA);
        });

        HBox podium = new HBox(100);
        podium.setAlignment(Pos.CENTER);

        // === UNIQUE AWARD SYSTEM ===
        Set<String> usedAwards = new HashSet<>();
        Random rand = new Random();

        // Special awards that can only go to ONE player
        Bird killLeader = null, mostDamage = null, mostGroundPounds = null,
                mostLean = null, mostTaunts = null, fallGuy = null;

        int maxKills = -1, maxDamage = -1, maxPounds = -1, maxLean = -1, maxTaunts = -1;

        int maxFalls = -1;

        for (Bird b : activeBirds) {
            if (eliminations[b.playerIndex] > maxKills) {
                maxKills = eliminations[b.playerIndex];
                killLeader = b;
            }
            if (damageDealt[b.playerIndex] > maxDamage) {
                maxDamage = damageDealt[b.playerIndex];
                mostDamage = b;
            }
            if (groundPounds[b.playerIndex] > maxPounds) {
                maxPounds = groundPounds[b.playerIndex];
                mostGroundPounds = b;
            }
            if (leanTime[b.playerIndex] > maxLean) {
                maxLean = leanTime[b.playerIndex];
                mostLean = b;
            }
            if (tauntsPerformed[b.playerIndex] > maxTaunts) {
                maxTaunts = tauntsPerformed[b.playerIndex];
                mostTaunts = b;
            }

            if (falls[b.playerIndex] > maxFalls) {
                maxFalls = falls[b.playerIndex];
                fallGuy = b;
            }

            if (b.health <= 0) {
                if (fallGuy == null || b.y > fallGuy.y) {
                    fallGuy = b;
                }
            }
        }

        String[] funnyAwards = {
                "MOST BRUTAL", "AIR CAMPER", "HEAL SLUT", "POWER-UP HOARDER",
                "CROW FARMER", "STUN MASTER", "DIVE BOMBER", "ICE KING",
                "HEART THIEF", "PEACEFUL BIRD", "MOST STYLISH", "PARTY ANIMAL",
                "SELF-KO KING", "JUST VIBING", "CLUTCH GOD", "SKY TERROR",
                "GROUND ZERO", "FEATHER DUSTER", "BEAK BREAKER", "WING CLIPPER"
        };

        for (Bird bird : activeBirds) {
            int place = activeBirds.indexOf(bird) + 1;
            VBox slot = new VBox(15);
            slot.setAlignment(Pos.CENTER);
            slot.setPadding(new Insets(25));
            slot.setStyle("-fx-background-color: rgba(0,0,0,0.5); -fx-background-radius: 25;");

            Label placeLabel = new Label(place == 1 ? "1ST" : place == 2 ? "2ND" : place == 3 ? "3RD" : place + "TH");
            placeLabel.setFont(Font.font("Arial Black", 70));
            placeLabel.setTextFill(place == 1 ? Color.GOLD : place == 2 ? Color.SILVER : place == 3 ? Color.web("#CD7F32") : Color.GRAY);
            placeLabel.setEffect(new DropShadow(20, Color.BLACK));

            Canvas icon = new Canvas(140, 140);
            GraphicsContext gc = icon.getGraphicsContext2D();
            gc.setFill(bird.type.color);
            gc.fillOval(20, 20, 100, 100);
            gc.setFill(bird.type.color.brighter());
            gc.fillOval(bird.facingRight ? 70 : 20, 40, 60, 50);
            gc.setFill(Color.WHITE);
            gc.fillOval(bird.facingRight ? 80 : 40, 50, 30, 30);
            gc.setFill(Color.BLACK);
            gc.fillOval(bird.facingRight ? 90 : 50, 60, 18, 18);
            gc.setFill(Color.ORANGE);
            double[] xPoints = bird.facingRight ? new double[]{120, 145, 120} : new double[]{20, -5, 20};
            double[] yPoints = {75, 85, 95};
            gc.fillPolygon(xPoints, yPoints, 3);

            String name = bird.name
                    .replace("P1:", "Player 1:")
                    .replace("P2:", "Player 2:")
                    .replace("P3:", "Player 3:")
                    .replace("P4:", "Player 4:")
                    .replace("AI", "AI Player");
            Label nameLabel = new Label(name);
            nameLabel.setFont(Font.font("Arial Black", 34));
            nameLabel.setTextFill(Color.WHITE);

            // === UNIQUE AWARD LOGIC ===
            String award;

            if (place == 1) {
                award = (bird.health < 20 && bird.health > 0) ? "CLUTCH GOD" : "CHAMPION";
            } else if (bird == killLeader && maxKills >= 2) {
                award = "KILL LEADER";
            } else if (bird == mostDamage && maxDamage > 100) {
                award = "MOST BRUTAL";
            } else if (bird == mostGroundPounds && maxPounds > 0) {
                award = "GROUND POUNDER";
            } else if (bird == mostLean && maxLean > 120) {
                award = "LEAN GOD";
            } else if (bird == mostTaunts && maxTaunts >= 3) {
                award = "TAUNT LORD";
            } else if (bird == fallGuy && maxFalls >= 1) {
                award = "FALL GUY";
            } else {
                // Give unique funny award (existing code)
                String candidate;
                do {
                    candidate = funnyAwards[rand.nextInt(funnyAwards.length)];
                } while (usedAwards.contains(candidate));
                award = candidate;
            }
            usedAwards.add(award);

            Label awardLabel = new Label(award);
            awardLabel.setFont(Font.font("Arial Black", 32));
            awardLabel.setTextFill(Color.CYAN.brighter());
            awardLabel.setEffect(new Glow(0.8));

            slot.getChildren().addAll(placeLabel, icon, nameLabel, awardLabel);
            podium.getChildren().add(slot);
        }

        VBox analyticsPanel = buildPostMatchAnalyticsPanel(activeBirds);
        root.getChildren().addAll(title, podium, analyticsPanel, buttons);
        setScenePreservingFullscreen(stage, new Scene(root, WIDTH, HEIGHT));
    }

    private Label labelFor(Bird b, String stat) {
        return new Label(b != null ? b.name + " (" + stat + ")" : "-");
    }

    private Button button(String text, String color) {
        Button b = uiFactory.action(text, 460, 130, 44, color, 40, null);
        b.setWrapText(false);
        uiFactory.fitSingleLineOnLayout(b, 44, 20);
        return b;
    }

    private HBox buildSummaryButtons(Stage stage, Bird winner) {
        HBox buttons = new HBox(100);
        buttons.setAlignment(Pos.CENTER);
        if (classicModeActive) {
            boolean wonRound = didPlayerWinClassic(winner);
            boolean finalRound = classicRoundIndex >= classicRun.size() - 1;
            String advanceText = wonRound
                    ? (finalRound ? "CLAIM REWARD" : "NEXT ENCOUNTER")
                    : "RETRY ENCOUNTER";

            Button continueClassic = button(advanceText, "#1565C0");
            continueClassic.setOnAction(e -> {
                resetMatchStats();
                handleClassicMatchEnd(stage, winner);
            });

            Button restartRun = button("RESTART RUN", "#00897B");
            restartRun.setOnAction(e -> {
                resetMatchStats();
                showClassicRunBriefing(stage, classicSelectedBird);
            });

            Button menu = button("MAIN MENU", "#9C27B0");
            menu.setOnAction(e -> {
                resetMatchStats();
                showMenu(stage);
            });
            buttons.getChildren().addAll(continueClassic, restartRun, menu);
        } else if (storyModeActive) {
            Button continueStory = button("CONTINUE EPISODE", "#1565C0");
            continueStory.setOnAction(e -> {
                resetMatchStats();
                handleStoryMatchEnd(stage, winner);
            });
            Button menu = button("MAIN MENU", "#9C27B0");
            menu.setOnAction(e -> {
                resetMatchStats();
                showMenu(stage);
            });
            buttons.getChildren().addAll(continueStory, menu);
        } else {
            Button rematch = button("REMATCH", "#FF1744");
            rematch.setOnAction(e -> {
                resetMatchStats();
                competitionSeriesActive = false;
                competitionDraftComplete = false;
                competitionDraftSummary = "";
                competitionBannedBirds.clear();
                Arrays.fill(competitionRoundWins, 0);
                Arrays.fill(competitionTeamWins, 0);
                competitionRoundNumber = 1;
                if (competitionModeEnabled && !storyModeActive && !isTrialMode && !classicModeActive) {
                    showCompetitionDraft(stage, () -> startMatch(stage));
                } else {
                    startMatch(stage);
                }
            });
            Button menu = button("MAIN MENU", "#9C27B0");
            menu.setOnAction(e -> {
                resetMatchStats();
                showMenu(stage);
            });
            buttons.getChildren().addAll(rematch, menu);
        }
        return buttons;
    }

    private void applyWinnerMapProgress(Bird winner) {
        if (winner == null) return;
        if (selectedMap == MapType.CITY) {
            cityWins[winner.playerIndex]++;
            if (cityWins[winner.playerIndex] >= 5 && !achievementsUnlocked[12]) {
                unlockAchievement(12, "URBAN KING!");
            }
        }

        if (selectedMap == MapType.SKYCLIFFS) {
            cliffWins[winner.playerIndex]++;
            if (cliffWins[winner.playerIndex] >= 5 && !achievementsUnlocked[15]) {
                unlockAchievement(15, "SKY EMPEROR!");
            }
        }

        if (selectedMap == MapType.VIBRANT_JUNGLE) {
            jungleWins[winner.playerIndex]++;
            if (jungleWins[winner.playerIndex] >= 5 && !achievementsUnlocked[17]) {
                unlockAchievement(17, "CANOPY KING!");
            }
        }
    }

    private String teamLabel(int teamId) {
        return teamId == 2 ? "TEAM B" : "TEAM A";
    }

    private Color teamColor(int teamId) {
        return teamId == 2 ? Color.web("#FF6B6B") : Color.web("#64B5F6");
    }

    private void applyAdaptiveBalance(Bird bird) {
        if (bird == null) return;

        int idx = bird.type.ordinal();
        int picks = typePicks[idx];
        if (picks < BALANCE_MIN_SAMPLE) return;

        double winRate = typeWins[idx] / (double) picks;
        double powerScale = 1.0;
        double speedScale = 1.0;

        if (winRate >= BALANCE_HIGH_WINRATE) {
            powerScale = winRate >= 0.70 ? 0.90 : 0.94;
            speedScale = winRate >= 0.70 ? 0.95 : 0.97;
        } else if (winRate <= BALANCE_LOW_WINRATE) {
            powerScale = winRate <= 0.30 ? 1.10 : 1.06;
            speedScale = winRate <= 0.30 ? 1.05 : 1.03;
        }

        bird.setBaseMultipliers(
                bird.baseSizeMultiplier,
                bird.basePowerMultiplier * powerScale,
                bird.baseSpeedMultiplier * speedScale
        );
    }

    private void recordBalanceOutcome(Bird winner) {
        if (balanceOutcomeRecorded) return;
        balanceOutcomeRecorded = true;

        if (isTrialMode || storyModeActive) return;

        for (int i = 0; i < activePlayers; i++) {
            Bird b = players[i];
            if (b == null) continue;
            int typeIdx = b.type.ordinal();
            typePicks[typeIdx]++;
            typeDamage[typeIdx] += Math.max(0, damageDealt[i]);
            typeElims[typeIdx] += Math.max(0, eliminations[i]);
        }
        if (winner != null) {
            typeWins[winner.type.ordinal()]++;
        }
    }

    private VBox buildPostMatchAnalyticsPanel(List<Bird> activeBirds) {
        record MatchTypeStats(BirdType type, int picks, int damage, int kills, int specials, int specialHits) {}

        Map<BirdType, int[]> byType = new HashMap<>();
        int totalDamage = 0;
        for (Bird b : activeBirds) {
            int idx = b.playerIndex;
            int[] s = byType.computeIfAbsent(b.type, t -> new int[6]);
            s[0] += 1;
            s[1] += Math.max(0, damageDealt[idx]);
            s[2] += Math.max(0, eliminations[idx]);
            s[3] += Math.max(0, specialsUsed[idx]);
            s[4] += Math.max(0, specialHits[idx]);
            s[5] += Math.max(0, specialDamageDealt[idx]);
            totalDamage += Math.max(0, damageDealt[idx]);
        }

        List<MatchTypeStats> rows = new ArrayList<>();
        for (var e : byType.entrySet()) {
            int[] s = e.getValue();
            rows.add(new MatchTypeStats(e.getKey(), s[0], s[1], s[2], s[3], s[4]));
        }
        rows.sort((a, b) -> Integer.compare(b.damage(), a.damage()));

        VBox box = new VBox(8);
        box.setAlignment(Pos.CENTER_LEFT);
        box.setPadding(new Insets(14, 24, 14, 24));
        box.setStyle("-fx-background-color: rgba(0,0,0,0.45); -fx-background-radius: 20; -fx-border-color: #7C4DFF; -fx-border-radius: 20;");

        Label header = new Label("POST-MATCH ANALYTICS");
        header.setFont(Font.font("Arial Black", 34));
        header.setTextFill(Color.web("#B388FF"));
        box.getChildren().add(header);

        Label columns = new Label("Type                     Ability Hit%   Damage Share   Kill Conv");
        columns.setFont(Font.font("Consolas", 22));
        columns.setTextFill(Color.WHITE);
        box.getChildren().add(columns);

        int safeTotalDamage = Math.max(1, totalDamage);
        for (MatchTypeStats r : rows) {
            double hitRate = r.specials() > 0 ? (r.specialHits() * 100.0 / r.specials()) : 0.0;
            double damageShare = r.damage() * 100.0 / safeTotalDamage;
            double killConv = r.damage() > 0 ? (r.kills() * 100.0 / r.damage()) : 0.0; // kills per 100 dmg

            String line = String.format(
                    "%-24s %10.1f%% %12.1f%% %10.2f",
                    r.type().name,
                    hitRate,
                    damageShare,
                    killConv
            );
            Label row = new Label(line);
            row.setFont(Font.font("Consolas", 21));
            row.setTextFill(r.type().color.brighter());
            box.getChildren().add(row);
        }

        Label note = new Label("Kill Conv = eliminations per 100 damage. Ability Hit% = special hits / special uses.");
        note.setFont(Font.font("Consolas", 16));
        note.setTextFill(Color.LIGHTGRAY);
        box.getChildren().add(note);
        return box;
    }

    private void resetMatchStats() {
        Arrays.fill(falls, 0);
        Arrays.fill(damageDealt, 0);
        Arrays.fill(eliminations, 0);
        Arrays.fill(groundPounds, 0);
        Arrays.fill(loungeTime, 0);
        Arrays.fill(leanTime, 0);
        Arrays.fill(tauntsPerformed, 0);
        Arrays.fill(specialsUsed, 0);
        Arrays.fill(specialHits, 0);
        Arrays.fill(specialDamageDealt, 0);
        killFeed.clear();
        matchEnded = false;
        balanceOutcomeRecorded = false;
        matchTimer = MATCH_DURATION_FRAMES;
        resetSuddenDeathState();
        Arrays.fill(thermalPickups, 0);
        Arrays.fill(highCliffJumps, 0);
        Arrays.fill(cliffWins, 0);
        Arrays.fill(vineGrapplePickups, 0);
        Arrays.fill(jungleWins, 0);
    }

    private void resetSuddenDeathState() {
        suddenDeath.reset();
    }

    private void loadAchievements() {
        Preferences prefs = Preferences.userNodeForPackage(BirdGame3.class);
        for (int i = 0; i < ACHIEVEMENT_COUNT; i++) {
            achievementsUnlocked[i] = prefs.getBoolean("ach_" + i, false);
            achievementProgress[i] = prefs.getInt("prog_" + i, 0);
        }
        cityPigeonUnlocked = true;
        noirPigeonUnlocked = prefs.getBoolean("skin_noirpigeon", false);
        eagleSkinUnlocked = prefs.getBoolean("skin_eagle", true);
        batUnlocked = true;
        pigeonEpisodeUnlockedChapters = Math.max(1, Math.min(prefs.getInt("ep_pigeon_unlocked", 1), storyChapters.length));
        pigeonEpisodeCompleted = prefs.getBoolean("ep_pigeon_completed", false);
        batEpisodeUnlockedChapters = Math.max(1, Math.min(prefs.getInt("ep_bat_unlocked", 1), batStoryChapters.length));
        batEpisodeCompleted = prefs.getBoolean("ep_bat_completed", false);
        pelicanEpisodeUnlockedChapters = Math.max(1, Math.min(prefs.getInt("ep_pelican_unlocked", 1), pelicanStoryChapters.length));
        pelicanEpisodeCompleted = prefs.getBoolean("ep_pelican_completed", false);
        if (pigeonEpisodeCompleted) {
            pigeonEpisodeUnlockedChapters = storyChapters.length;
            noirPigeonUnlocked = true;
        }
        if (batEpisodeCompleted) {
            batEpisodeUnlockedChapters = batStoryChapters.length;
            batUnlocked = true;
        }
        if (pelicanEpisodeCompleted) {
            pelicanEpisodeUnlockedChapters = pelicanStoryChapters.length;
            eagleSkinUnlocked = true;
        }
        for (BirdType type : BirdType.values()) {
            int idx = type.ordinal();
            classicCompleted[idx] = prefs.getBoolean("classic_done_" + type.name(), false);
            classicSkinUnlocked[idx] = prefs.getBoolean("classic_skin_" + type.name(), false);
        }
        // Requested defaults
        eagleSkinUnlocked = true;
        classicSkinUnlocked[BirdType.EAGLE.ordinal()] = true;
        classicSkinUnlocked[BirdType.PIGEON.ordinal()] = noirPigeonUnlocked;
        batUnlocked = true;
        for (int i = 0; i < 4; i++) {
            vineGrapplePickups[i] = prefs.getInt("vine_pickups_" + i, 0);
            jungleWins[i] = prefs.getInt("jungle_wins_" + i, 0);
        }
        for (BirdType type : BirdType.values()) {
            int idx = type.ordinal();
            typePicks[idx] = prefs.getInt("balance_picks_" + type.name(), 0);
            typeWins[idx] = prefs.getInt("balance_wins_" + type.name(), 0);
            typeDamage[idx] = prefs.getInt("balance_damage_" + type.name(), 0);
            typeElims[idx] = prefs.getInt("balance_elims_" + type.name(), 0);
        }
    }
    private void saveAchievements() {
        Preferences prefs = Preferences.userNodeForPackage(BirdGame3.class);
        for (int i = 0; i < ACHIEVEMENT_COUNT; i++) {
            prefs.putBoolean("ach_" + i, achievementsUnlocked[i]);
            prefs.putInt("prog_" + i, achievementProgress[i]);
        }
        prefs.putBoolean("skin_citypigeon", cityPigeonUnlocked);
        prefs.putBoolean("skin_noirpigeon", noirPigeonUnlocked);
        prefs.putBoolean("skin_eagle", eagleSkinUnlocked);
        prefs.putBoolean("char_bat_unlocked", batUnlocked);
        for (BirdType type : BirdType.values()) {
            int idx = type.ordinal();
            prefs.putBoolean("classic_done_" + type.name(), classicCompleted[idx]);
            prefs.putBoolean("classic_skin_" + type.name(), classicSkinUnlocked[idx]);
        }
        prefs.putInt("ep_pigeon_unlocked", pigeonEpisodeUnlockedChapters);
        prefs.putBoolean("ep_pigeon_completed", pigeonEpisodeCompleted);
        prefs.putInt("ep_bat_unlocked", batEpisodeUnlockedChapters);
        prefs.putBoolean("ep_bat_completed", batEpisodeCompleted);
        prefs.putInt("ep_pelican_unlocked", pelicanEpisodeUnlockedChapters);
        prefs.putBoolean("ep_pelican_completed", pelicanEpisodeCompleted);
        for (int i = 0; i < 4; i++) {
            prefs.putInt("vine_pickups_" + i, vineGrapplePickups[i]);
            prefs.putInt("jungle_wins_" + i, jungleWins[i]);
        }
        for (BirdType type : BirdType.values()) {
            int idx = type.ordinal();
            prefs.putInt("balance_picks_" + type.name(), typePicks[idx]);
            prefs.putInt("balance_wins_" + type.name(), typeWins[idx]);
            prefs.putInt("balance_damage_" + type.name(), typeDamage[idx]);
            prefs.putInt("balance_elims_" + type.name(), typeElims[idx]);
        }
        try {
            prefs.flush();
        } catch (BackingStoreException e) {
            e.printStackTrace();
        }
    }
    public void checkAchievements(Bird bird) {
        int idx = bird.playerIndex;

        // First Blood
        if (eliminations[idx] >= 1 && !achievementsUnlocked[0]) {
            unlockAchievement(0, "FIRST BLOOD!");
        }

        // Dominator - 3 eliminations in one match
        if (eliminations[idx] >= 3 && !achievementsUnlocked[1]) {
            unlockAchievement(1, "DOMINATOR!");
        }

        // Annihilator - eliminate all other players (3 kills in 4-player)
        if (eliminations[idx] >= 3 && activePlayers == 4 && !achievementsUnlocked[2]) {
            unlockAchievement(2, "ANNIHILATOR!");
        }
        // If only 3 or 2 players, make it possible with 2 or 1 kill
        else if (eliminations[idx] >= 2 && activePlayers == 3 && !achievementsUnlocked[2]) {
            unlockAchievement(2, "ANNIHILATOR!");
        } else if (eliminations[idx] >= 1 && activePlayers == 2 && !achievementsUnlocked[2]) {
            unlockAchievement(2, "ANNIHILATOR!");
        }

        // Turkey Slam Master
        if (groundPounds[idx] >= 3 && !achievementsUnlocked[3]) {
            unlockAchievement(3, "TURKEY SLAM MASTER!");
        }

        // Lean God â€“ 5 minutes total in lean cloud
        if (bird.type == BirdType.OPIUMBIRD) {
            achievementProgress[4] += leanTime[idx];
            if (achievementProgress[4] >= 18000 && !achievementsUnlocked[4]) { // 300 seconds = 18000 frames @ 60fps
                unlockAchievement(4, "LEAN GOD!");
            }
        }

        // Lounge Lizard â€“ heal 100 HP total
        if (bird.type == BirdType.MOCKINGBIRD) {
            achievementProgress[5] += (int) (loungeTime[idx] * (12.0 / 60.0));
            if (achievementProgress[5] >= 100 && !achievementsUnlocked[5]) {
                unlockAchievement(5, "LOUNGE LIZARD!");
            }
        }

        // Power-Up Hoarder (already checked on pickup)
        // Fall Guy (already checked on fall)

        // Taunt Lord
        if (tauntsPerformed[idx] >= 10 && !achievementsUnlocked[8]) {
            unlockAchievement(8, "TAUNT LORD!");
        }

        // Pelican King
        if (bird.type == BirdType.PELICAN && BirdGame3.this.achievementProgress[18] >= 15 && !BirdGame3.this.achievementsUnlocked[18]) {
            BirdGame3.this.unlockAchievement(18, "PELICAN KING!");
        }

        // Clutch God (checked at match end in victory animation)
    }

    private void showAchievements(Stage stage) {
        playMenuMusic();
        if (musicPlayer != null) musicPlayer.stop();

        VBox root = new VBox(20);
        root.setAlignment(Pos.CENTER);
        root.setPadding(new Insets(50));
        root.setStyle("-fx-background-color: linear-gradient(to bottom, #1a0033, #2d1b69);");

        Label title = new Label("ACHIEVEMENTS");
        title.setFont(Font.font("Arial Black", FontWeight.BOLD, 90));
        title.setTextFill(Color.GOLD);
        title.setEffect(new DropShadow(30, Color.PURPLE));

        GridPane grid = new GridPane();
        grid.setHgap(40);
        grid.setVgap(25);
        grid.setAlignment(Pos.CENTER);

        for (int i = 0; i < ACHIEVEMENT_COUNT; i++) {
            Label nameLabel = new Label(ACHIEVEMENT_NAMES[i]);
            nameLabel.setFont(Font.font("Arial Black", 36));
            nameLabel.setTextFill(achievementsUnlocked[i] ? Color.LIMEGREEN : Color.GRAY);

            String status = achievementsUnlocked[i] ? " âœ“ UNLOCKED" : " (Locked)";
            Label descLabel = new Label(ACHIEVEMENT_DESCRIPTIONS[i] + status);
            descLabel.setFont(Font.font("Consolas", 24));
            descLabel.setTextFill(achievementsUnlocked[i] ? Color.LIMEGREEN : Color.DARKGRAY);
            descLabel.setWrapText(true);
            descLabel.setMaxWidth(600);

            grid.add(nameLabel, 0, i);
            grid.add(descLabel, 1, i);
        }

        Button back = new Button("BACK");
        back.setPrefSize(400, 100);
        back.setFont(Font.font("Arial Black", 60));
        back.setStyle("-fx-background-color: #FF1744; -fx-text-fill: white; -fx-background-radius: 50;");
        back.setOnAction(e -> showMenu(stage));

        ScrollPane scroll = new ScrollPane(grid);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background: transparent;");

        root.getChildren().addAll(title, scroll, back);

        Scene scene = new Scene(root, WIDTH, HEIGHT);
        setupKeyboardNavigation(scene);
        applyConsoleHighlight(scene);

        // Hide default blue mouse focus ring
        root.setStyle(root.getStyle() + ";-fx-focus-color:transparent;-fx-faint-focus-color:transparent;");

        setScenePreservingFullscreen(stage, scene);
        back.requestFocus();
    }

    private void showTrials(Stage stage) {
        playMenuMusic();
        if (musicPlayer != null) musicPlayer.stop();
        VBox root = new VBox(40);
        root.setAlignment(Pos.CENTER);
        root.setPadding(new Insets(60));
        root.setStyle("-fx-background-color: linear-gradient(to bottom, #000033, #001166);");

        Label title = new Label("TRIALS");
        title.setFont(Font.font("Arial Black", FontWeight.BOLD, 100));
        title.setTextFill(Color.CYAN.brighter());
        title.setEffect(new Glow(0.9));

        VBox trialBox = new VBox(20);
        trialBox.setAlignment(Pos.CENTER);
        trialBox.setPadding(new Insets(40));
        trialBox.setStyle("-fx-background-color: rgba(0,0,0,0.6); -fx-background-radius: 30; -fx-border-color: cyan; -fx-border-width: 4; -fx-border-radius: 30;");

        Label trialName = new Label("CITY PIGEON TRIAL");
        trialName.setFont(Font.font("Arial Black", 50));
        trialName.setTextFill(Color.WHITE);

        String statusText = cityPigeonUnlocked ? "âœ“ COMPLETED (Replayable)" : "Defeat the City Pigeon boss with your 2 allies";
        Label status = new Label(statusText);
        status.setFont(Font.font("Consolas", 32));
        status.setTextFill(cityPigeonUnlocked ? Color.LIMEGREEN : Color.ORANGE);

        Label reward = new Label("Reward: City Pigeon Alternate Skin\n(Fedora + Cigarette)");
        reward.setFont(Font.font("Consolas", 28));
        reward.setTextFill(Color.GOLD);
        reward.setAlignment(Pos.CENTER);

        Button playBtn = uiFactory.action(
                cityPigeonUnlocked ? "REPLAY TRIAL" : "PLAY TRIAL",
                500, 120, 50, "#FF1744", 40,
                () -> startPigeonTrial(stage)
        );

        trialBox.getChildren().addAll(trialName, status, reward, playBtn);

        Button back = uiFactory.action("BACK", 400, 100, 60, "#444444", 50, () -> showMenu(stage));

        root.getChildren().addAll(title, trialBox, back);

        VBox eagleTrialBox = new VBox(20);
        eagleTrialBox.setAlignment(Pos.CENTER);
        eagleTrialBox.setPadding(new Insets(40));
        eagleTrialBox.setStyle("-fx-background-color: rgba(0,0,0,0.6); -fx-background-radius: 30; -fx-border-color: orange; -fx-border-width: 4; -fx-border-radius: 30;");

        Label eagleName = new Label("SKY TYRANT TRIAL");
        eagleName.setFont(Font.font("Arial Black", 50));
        eagleName.setTextFill(Color.WHITE);

        String eagleStatus = eagleSkinUnlocked ? "âœ“ COMPLETED (Replayable)" : "Defeat the Sky Tyrant with your allies";
        Label eagleStat = new Label(eagleStatus);
        eagleStat.setFont(Font.font("Consolas", 32));
        eagleStat.setTextFill(eagleSkinUnlocked ? Color.LIMEGREEN : Color.ORANGE);

        Label eagleReward = new Label("Reward: Sky King Eagle Skin\n(Golden feathers + crown glow)");
        eagleReward.setFont(Font.font("Consolas", 28));
        eagleReward.setTextFill(Color.GOLD);
        eagleReward.setAlignment(Pos.CENTER);

        Button eagleBtn = uiFactory.action(
                eagleSkinUnlocked ? "REPLAY TRIAL" : "PLAY TRIAL",
                500, 120, 50, "#FF5722", 40,
                () -> startEagleTrial(stage)
        );

        eagleTrialBox.getChildren().addAll(eagleName, eagleStat, eagleReward, eagleBtn);

        root.getChildren().add(eagleTrialBox);

        Scene scene = new Scene(root, WIDTH, HEIGHT);
        // Disable default blue mouse focus ring â€” only our yellow highlight shows
        scene.getRoot().setStyle(scene.getRoot().getStyle() + ";-fx-focus-color:transparent;-fx-faint-focus-color:transparent;");
        setupKeyboardNavigation(scene);
        applyConsoleHighlight(scene);
        setScenePreservingFullscreen(stage, scene);
        back.requestFocus();
    }

    private void togglePause(Stage stage) {
        Scene scene = stage.getScene();
        if (isPaused) {
            // === RESUME ===
            scene.setOnKeyPressed(e -> handleGameplayKeyPress(stage, e));
            gameRoot.getChildren().removeIf(node -> node instanceof VBox && "pauseMenu".equals(node.getId()));

            lastUpdate = 0;      // â† CRITICAL: reset so no speed-up
            accumulator = 0;

            timer.start();
            isPaused = false;
        } else {
            // === PAUSE ===
            scene.setOnKeyPressed(e -> {
                KeyCode code = e.getCode();
                KeyCode arrow = switch (code) {
                    case W -> KeyCode.UP;
                    case S -> KeyCode.DOWN;
                    case A -> KeyCode.LEFT;
                    case D -> KeyCode.RIGHT;
                    default -> null;
                };
                if (arrow != null) {
                    e.consume();
                    Node focus = scene.getFocusOwner();
                    if (focus != null) focus.fireEvent(new KeyEvent(KeyEvent.KEY_PRESSED, "", "", arrow, false, false, false, false));
                }
                if (code == KeyCode.SPACE || code == KeyCode.ENTER) {
                    playButtonClick();
                    Node focus = scene.getFocusOwner();
                    if (focus instanceof Button btn) btn.fire();
                    e.consume();
                }
                if (code == KeyCode.ESCAPE) {
                    togglePause(stage);
                    e.consume();
                }
            });

            VBox pauseMenu = new VBox(20);
            pauseMenu.setId("pauseMenu");
            pauseMenu.setAlignment(Pos.CENTER);
            pauseMenu.setPrefSize(WIDTH, HEIGHT);
            pauseMenu.setStyle("-fx-background-color: rgba(0,0,0,0.7);");

            Label pauseLabel = new Label("PAUSED");
            pauseLabel.setFont(Font.font("Arial Black", FontWeight.BOLD, 80));
            pauseLabel.setTextFill(Color.WHITE);

            Button resumeButton = new Button("Resume");
            resumeButton.setPrefSize(300, 60);
            resumeButton.setFont(Font.font("Arial Black", 30));
            resumeButton.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white; -fx-background-radius: 20;");
            resumeButton.setFocusTraversable(true);
            resumeButton.setOnAction(ev -> { playButtonClick(); togglePause(stage); });

            Button restartButton = new Button("Restart");
            restartButton.setPrefSize(300, 60);
            restartButton.setFont(Font.font("Arial Black", 30));
            restartButton.setStyle("-fx-background-color: #FFC107; -fx-text-fill: white; -fx-background-radius: 20;");
            restartButton.setFocusTraversable(true);
            restartButton.setOnAction(ev -> {
                playButtonClick();
                togglePause(stage);
                resetMatchStats();
                if (isTrialMode) {
                    if (selectedMap == MapType.CITY) startPigeonTrial(stage);
                    else if (selectedMap == MapType.SKYCLIFFS) startEagleTrial(stage);
                } else startMatch(stage);
            });

            Button exitButton = new Button("Exit to Menu");
            exitButton.setPrefSize(300, 60);
            exitButton.setFont(Font.font("Arial Black", 30));
            exitButton.setStyle("-fx-background-color: #F44336; -fx-text-fill: white; -fx-background-radius: 20;");
            exitButton.setFocusTraversable(true);
            exitButton.setOnAction(ev -> { playButtonClick(); showMenu(stage); });

            pauseMenu.getChildren().addAll(pauseLabel, resumeButton, restartButton, exitButton);
            gameRoot.getChildren().add(pauseMenu);
            resumeButton.requestFocus();
            applyConsoleHighlight(scene);

            timer.stop();
            isPaused = true;
        }
    }

    @Override
    public void stop() throws Exception {
        saveAchievements();
        super.stop();
    }

    static void main(String[] args) {
        launch(args);
    }
}





