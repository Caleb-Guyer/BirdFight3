package com.example.birdgame3;
import com.example.birdgame3.Bird;

import javafx.animation.AnimationTimer;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.Point2D;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.Control;
import javafx.scene.control.ContentDisplay;
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
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.media.AudioClip;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;
import javafx.scene.shape.Circle;
import javafx.stage.Stage;

import java.util.*;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;
import java.util.function.BooleanSupplier;

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
    private final int[] cpuLevels = new int[]{5, 5, 5, 5};
    private final BirdType[] fightSelectedBirds = new BirdType[4];
    private final boolean[] fightRandomSelected = new boolean[4];
    private final String[] fightSelectedSkinKeys = new String[4];
    private int activePlayers = 2;
    private AnimationTimer timer;
    public List<Platform> platforms = new ArrayList<>();
    public List<PowerUp> powerUps = new ArrayList<>();
    public List<NectarNode> nectarNodes = new ArrayList<>();
    public List<SwingingVine> swingingVines = new ArrayList<>();
    private final List<double[]> cityStars = new ArrayList<>();
    private VBox[] playerSlots;
    private Button[] teamButtons = new Button[4];
    private final Button[] cpuButtons = new Button[4];
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
    private int birdCoins = 0;
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
    public enum MapType { FOREST, CITY, SKYCLIFFS, VIBRANT_JUNGLE, CAVE, BATTLEFIELD }

    public MapType selectedMap = MapType.FOREST; // default
    private double battlefieldIslandX = 0;
    private double battlefieldIslandW = 0;
    private double battlefieldIslandY = 0;

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
    private Runnable stageSelectReturn = null;
    private Runnable settingsReturn = null;

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

        boolean classicBossMusic = classicModeActive && classicEncounter != null && classicEncounter.bossFight;
        String file = (activeBossChallenge != BossChallengeType.NONE || classicBossMusic)
                ? "GW2ops.mp3"
                : switch (selectedMap) {
                    case FOREST -> "ultimate_battle.mp3";
                    case CITY -> "braniac_maniac.mp3";
                    case SKYCLIFFS -> "zombotany.mp3";
                    case VIBRANT_JUNGLE -> "loonboon.mp3";
                    case CAVE -> "dark-ages-ultimate-battle.mp3";
                    case BATTLEFIELD -> "skycity.mp3";
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
        scene.addEventFilter(MouseEvent.MOUSE_PRESSED, e -> {
            if (!(e.getTarget() instanceof Node target)) return;
            if (isInteractiveTarget(target)) return;
            Node focus = scene.getFocusOwner();
            if (focus != null) focus.requestFocus();
            e.consume();
        });
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

    private boolean isInteractiveTarget(Node target) {
        Node node = target;
        while (node != null) {
            if (node instanceof Control || node instanceof ScrollPane) return true;
            node = node.getParent();
        }
        return false;
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

    private void playErrorSound() {
        if (butterClip != null) {
            butterClip.setVolume(0.6);
            butterClip.play();
        } else if (bonkClip != null) {
            bonkClip.setVolume(0.6);
            bonkClip.play();
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
        ctrl.getProperties().put("highlightRestoreStyle", original);
        String marker = "-fx-border-color: white; -fx-border-width: 4; -fx-border-radius: 20; -fx-background-radius: 20; -fx-text-overrun: clip;";
        ctrl.getProperties().put("highlightMarker", marker);
        String style = original == null ? "" : original;
        if (!style.contains("-fx-border-color: white")) {
            ctrl.setStyle(style + (style.isBlank() ? "" : "; ") + marker);
        }
    }

    private void restoreControl(Control ctrl) {
        Object markerData = ctrl.getProperties().get("highlightMarker");
        if (!(markerData instanceof String marker) || marker.isBlank()) return;
        String style = ctrl.getStyle();
        if (style == null || style.isBlank()) return;
        String restored = style.replace(marker, "").replace(";;", ";").trim();
        if (restored.endsWith(";")) restored = restored.substring(0, restored.length() - 1).trim();
        ctrl.setStyle(restored);
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
        scene.setOnKeyReleased(this::handleGameplayKeyRelease);

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
        scene.setOnKeyReleased(this::handleGameplayKeyRelease);

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
    public boolean batUnlocked = false;
    public boolean falconUnlocked = false;
    public boolean phoenixUnlocked = false;
    public boolean titmouseUnlocked = false;

    private enum BossChallengeType { NONE, FALCON, PHOENIX, BAT, TITMOUSE }
    private BossChallengeType activeBossChallenge = BossChallengeType.NONE;
    private static final BirdType PHOENIX_CLASSIC_GATE = BirdType.RAZORBILL;

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
        SHOCK_DROPS("Shock Drops", "Overcharge and titan effects drop more often."),
        WIND_RALLY("Wind Rally", "Extra wind vents keep the fight airborne."),
        RAGE_RITUAL("Rage Ritual", "Rage and speed drops spark early momentum."),
        SHADOW_CACHE("Shadow Cache", "Shrink and neon drops keep spacing tight.");

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
        final boolean bossFight;
        int cpuLevel = 5;

        ClassicEncounter(String name, String announcer, String briefing, MapType map,
                         MatchMutator mutator, ClassicTwist twist, int timerFrames,
                         ClassicFighter[] allies, ClassicFighter[] enemies, boolean bossFight) {
            this.name = name;
            this.announcer = announcer;
            this.briefing = briefing;
            this.map = map;
            this.mutator = mutator;
            this.twist = twist;
            this.timerFrames = timerFrames;
            this.allies = allies;
            this.enemies = enemies;
            this.bossFight = bossFight;
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
    private static final String PELICAN_EPISODE_AWARD = "Sky King Eagle Skin + Bat Character";
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
                    "Final chapter. The Vulture boss runs the underground ring. Beat Vulture to unlock Bat and claim your reward.",
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
                    "The cave seals shut. Sudden death crows pour in early. End the Vulture warden before the swarm overwhelms you to unlock Bat.",
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

    // === ADVENTURE MODE ===
    private boolean adventureModeActive = false;
    private boolean adventureReplayMode = false;
    private int adventureChapterIndex = 0;
    private int adventureBattleIndex = 0;
    private int adventureChapterProgress = 0;
    private boolean adventureChapterCompleted = false;
    private final boolean[] adventureUnlocked = new boolean[BirdType.values().length];
    private BirdType adventureSelectedBird = BirdType.PIGEON;
    private String adventureSelectedSkinKey = null;
    private AdventureBattle currentAdventureBattle = null;
    private boolean adventureTeamMode = false;
    private final int[] adventureTeams = new int[]{1, 2, 2, 2};
    private int adventureMatchTimerOverride = -1;

    private enum DialogueSide { LEFT, RIGHT }

    static class AdventureDialogueLine {
        final BirdType leftBird;
        final BirdType rightBird;
        final DialogueSide speakerSide;
        final String speakerName;
        final String text;

        AdventureDialogueLine(BirdType leftBird, BirdType rightBird, DialogueSide speakerSide, String speakerName, String text) {
            this.leftBird = leftBird;
            this.rightBird = rightBird;
            this.speakerSide = speakerSide;
            this.speakerName = speakerName;
            this.text = text;
        }
    }

    static class AdventureBattle {
        final String title;
        final String briefing;
        final MapType map;
        final BirdType opponentType;
        final String opponentName;
        final double opponentHealth;
        final double opponentPowerMult;
        final double opponentSpeedMult;
        final BirdType requiredPlayerType;
        final EnumSet<BirdType> allowedBirds;
        final BirdType unlockReward;
        int cpuLevel = 5;
        final AdventureDialogueLine[] preDialogue;
        final AdventureDialogueLine[] winDialogue;
        final AdventureDialogueLine[] loseDialogue;

        AdventureBattle(String title, String briefing, MapType map, BirdType opponentType, String opponentName,
                        double opponentHealth, double opponentPowerMult, double opponentSpeedMult,
                        BirdType requiredPlayerType, EnumSet<BirdType> allowedBirds, BirdType unlockReward,
                        AdventureDialogueLine[] preDialogue, AdventureDialogueLine[] winDialogue, AdventureDialogueLine[] loseDialogue) {
            this.title = title;
            this.briefing = briefing;
            this.map = map;
            this.opponentType = opponentType;
            this.opponentName = opponentName;
            this.opponentHealth = opponentHealth;
            this.opponentPowerMult = opponentPowerMult;
            this.opponentSpeedMult = opponentSpeedMult;
            this.requiredPlayerType = requiredPlayerType;
            this.allowedBirds = allowedBirds;
            this.unlockReward = unlockReward;
            this.preDialogue = preDialogue;
            this.winDialogue = winDialogue;
            this.loseDialogue = loseDialogue;
        }
    }

    static class AdventureChapter {
        final String title;
        final String summary;
        final AdventureDialogueLine[] introDialogue;
        final AdventureBattle[] battles;
        final AdventureDialogueLine[] outroDialogue;

        AdventureChapter(String title, String summary, AdventureDialogueLine[] introDialogue,
                         AdventureBattle[] battles, AdventureDialogueLine[] outroDialogue) {
            this.title = title;
            this.summary = summary;
            this.introDialogue = introDialogue;
            this.battles = battles;
            this.outroDialogue = outroDialogue;
        }
    }

    private final AdventureChapter[] adventureChapters = new AdventureChapter[] {
            new AdventureChapter(
                    "Chapter 1: The Silent Beacon",
                    "The skyline Beacon has gone dark. Pigeon must rally allies before the Carrion Court tightens its grip.",
                    new AdventureDialogueLine[] {
                            new AdventureDialogueLine(
                                    BirdType.PIGEON,
                                    BirdType.MOCKINGBIRD,
                                    DialogueSide.LEFT,
                                    "Pigeon",
                                    "The Beacon went dark last night. The skyline feels wrong."
                            ),
                            new AdventureDialogueLine(
                                    BirdType.PIGEON,
                                    BirdType.MOCKINGBIRD,
                                    DialogueSide.RIGHT,
                                    "Old Sparrow",
                                    "When the Beacon dies, the Carrion Court stirs. Vulture scouts are already circling."
                            ),
                            new AdventureDialogueLine(
                                    BirdType.PIGEON,
                                    BirdType.MOCKINGBIRD,
                                    DialogueSide.LEFT,
                                    "Pigeon",
                                    "Then we gather wings. I will not let the rooftops fall silent."
                            ),
                            new AdventureDialogueLine(
                                    BirdType.PIGEON,
                                    BirdType.MOCKINGBIRD,
                                    DialogueSide.RIGHT,
                                    "Old Sparrow",
                                    "Start here. A neon messenger just landed. Earn her trust."
                            )
                    },
                    new AdventureBattle[] {
                            new AdventureBattle(
                                    "Battle 1: Rooftop Sprint",
                                    "A neon courier tests your speed and resolve.",
                                    MapType.CITY,
                                    BirdType.HUMMINGBIRD,
                                    "Rival: Neon Hummingbird",
                                    110,
                                    1.0,
                                    1.18,
                                    null,
                                    null,
                                    BirdType.HUMMINGBIRD,
                                    new AdventureDialogueLine[] {
                                            new AdventureDialogueLine(
                                                    BirdType.PIGEON,
                                                    BirdType.HUMMINGBIRD,
                                                    DialogueSide.RIGHT,
                                                    "Neon Hummingbird",
                                                    "I bring warnings, not hugs. The wind says you are slow."
                                            ),
                                            new AdventureDialogueLine(
                                                    BirdType.PIGEON,
                                                    BirdType.HUMMINGBIRD,
                                                    DialogueSide.LEFT,
                                                    "Pigeon",
                                                    "Then race me across the roofs. Winner earns the story."
                                            )
                                    },
                                    new AdventureDialogueLine[] {
                                            new AdventureDialogueLine(
                                                    BirdType.PIGEON,
                                                    BirdType.HUMMINGBIRD,
                                                    DialogueSide.RIGHT,
                                                    "Neon Hummingbird",
                                                    "Okay, stubborn one. You kept pace."
                                            ),
                                            new AdventureDialogueLine(
                                                    BirdType.PIGEON,
                                                    BirdType.HUMMINGBIRD,
                                                    DialogueSide.LEFT,
                                                    "Pigeon",
                                                    "Fly with me. The Beacon needs voices."
                                            ),
                                            new AdventureDialogueLine(
                                                    BirdType.PIGEON,
                                                    BirdType.HUMMINGBIRD,
                                                    DialogueSide.RIGHT,
                                                    "Neon Hummingbird",
                                                    "I am in. Next stop: the cliffs. The Eagle waits."
                                            )
                                    },
                                    new AdventureDialogueLine[] {
                                            new AdventureDialogueLine(
                                                    BirdType.PIGEON,
                                                    BirdType.HUMMINGBIRD,
                                                    DialogueSide.RIGHT,
                                                    "Neon Hummingbird",
                                                    "Come back when your wings stop shaking."
                                            ),
                                            new AdventureDialogueLine(
                                                    BirdType.PIGEON,
                                                    BirdType.HUMMINGBIRD,
                                                    DialogueSide.LEFT,
                                                    "Pigeon",
                                                    "Then I will."
                                            )
                                    }
                            ),
                            new AdventureBattle(
                                    "Battle 2: Old Friend Trial",
                                    "Eagle will only join if you prove you still fight for the flock.",
                                    MapType.SKYCLIFFS,
                                    BirdType.EAGLE,
                                    "Rival: Sky King Eagle",
                                    140,
                                    1.18,
                                    1.05,
                                    BirdType.PIGEON,
                                    null,
                                    BirdType.EAGLE,
                                    new AdventureDialogueLine[] {
                                            new AdventureDialogueLine(
                                                    BirdType.PIGEON,
                                                    BirdType.EAGLE,
                                                    DialogueSide.LEFT,
                                                    "Pigeon",
                                                    "Eagle! It is me. The Beacon is gone."
                                            ),
                                            new AdventureDialogueLine(
                                                    BirdType.PIGEON,
                                                    BirdType.EAGLE,
                                                    DialogueSide.RIGHT,
                                                    "Eagle",
                                                    "Years pass. Storms return. If you want my wings, prove your heart."
                                            )
                                    },
                                    new AdventureDialogueLine[] {
                                            new AdventureDialogueLine(
                                                    BirdType.PIGEON,
                                                    BirdType.EAGLE,
                                                    DialogueSide.RIGHT,
                                                    "Eagle",
                                                    "You still fight for the flock, not the crown. I am with you."
                                            ),
                                            new AdventureDialogueLine(
                                                    BirdType.PIGEON,
                                                    BirdType.EAGLE,
                                                    DialogueSide.LEFT,
                                                    "Pigeon",
                                                    "Then we move. The Carrion Court is next."
                                            ),
                                            new AdventureDialogueLine(
                                                    BirdType.PIGEON,
                                                    BirdType.EAGLE,
                                                    DialogueSide.RIGHT,
                                                    "Eagle",
                                                    "We will need more than friends."
                                            )
                                    },
                                    new AdventureDialogueLine[] {
                                            new AdventureDialogueLine(
                                                    BirdType.PIGEON,
                                                    BirdType.EAGLE,
                                                    DialogueSide.RIGHT,
                                                    "Eagle",
                                                    "Your wings are heavy. Come back lighter."
                                            )
                                    }
                            ),
                            new AdventureBattle(
                                    "Battle 3: Carrion Scout",
                                    "A Vulture scout blocks the path. Break the warning and keep your allies safe.",
                                    MapType.VIBRANT_JUNGLE,
                                    BirdType.VULTURE,
                                    "Enemy: Carrion Scout",
                                    160,
                                    1.15,
                                    0.95,
                                    null,
                                    EnumSet.of(BirdType.PIGEON, BirdType.HUMMINGBIRD, BirdType.EAGLE),
                                    null,
                                    new AdventureDialogueLine[] {
                                            new AdventureDialogueLine(
                                                    BirdType.PIGEON,
                                                    BirdType.VULTURE,
                                                    DialogueSide.RIGHT,
                                                    "Vulture Scout",
                                                    "So the Beacon's little light walks. How cute."
                                            ),
                                            new AdventureDialogueLine(
                                                    BirdType.PIGEON,
                                                    BirdType.VULTURE,
                                                    DialogueSide.LEFT,
                                                    "Pigeon",
                                                    "We are done hiding. Tell your Court we are coming."
                                            )
                                    },
                                    new AdventureDialogueLine[] {
                                            new AdventureDialogueLine(
                                                    BirdType.PIGEON,
                                                    BirdType.VULTURE,
                                                    DialogueSide.RIGHT,
                                                    "Vulture Scout",
                                                    "This is not over. The Court sees all."
                                            ),
                                            new AdventureDialogueLine(
                                                    BirdType.HUMMINGBIRD,
                                                    BirdType.PIGEON,
                                                    DialogueSide.LEFT,
                                                    "Neon Hummingbird",
                                                    "The Court runs deeper than rooftops."
                                            ),
                                            new AdventureDialogueLine(
                                                    BirdType.PIGEON,
                                                    BirdType.EAGLE,
                                                    DialogueSide.LEFT,
                                                    "Pigeon",
                                                    "Then we dive deeper."
                                            )
                                    },
                                    new AdventureDialogueLine[] {
                                            new AdventureDialogueLine(
                                                    BirdType.PIGEON,
                                                    BirdType.VULTURE,
                                                    DialogueSide.RIGHT,
                                                    "Vulture Scout",
                                                    "Run. The sky belongs to the Court."
                                            ),
                                            new AdventureDialogueLine(
                                                    BirdType.PIGEON,
                                                    BirdType.VULTURE,
                                                    DialogueSide.LEFT,
                                                    "Pigeon",
                                                    "Not yet."
                                            )
                                    }
                            )
                    },
                    new AdventureDialogueLine[] {
                            new AdventureDialogueLine(
                                    BirdType.PIGEON,
                                    BirdType.EAGLE,
                                    DialogueSide.LEFT,
                                    "Pigeon",
                                    "Two allies. One warning. The Beacon can be relit."
                            ),
                            new AdventureDialogueLine(
                                    BirdType.PIGEON,
                                    BirdType.EAGLE,
                                    DialogueSide.RIGHT,
                                    "Eagle",
                                    "We head for the caves next. That is where the shadows gather."
                            ),
                            new AdventureDialogueLine(
                                    BirdType.HUMMINGBIRD,
                                    BirdType.PIGEON,
                                    DialogueSide.LEFT,
                                    "Neon Hummingbird",
                                    "And where the bat listens."
                            )
                    }
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
        return switch (type) {
            case BAT -> batUnlocked;
            case FALCON -> falconUnlocked;
            case PHOENIX -> phoenixUnlocked;
            case TITMOUSE -> titmouseUnlocked;
            default -> true;
        };
    }

    private boolean isAdventureBirdUnlocked(BirdType type) {
        if (type == null) return false;
        return adventureUnlocked[type.ordinal()];
    }

    private List<BirdType> adventureUnlockedPool() {
        List<BirdType> pool = new ArrayList<>();
        for (BirdType bt : BirdType.values()) {
            if (isAdventureBirdUnlocked(bt)) pool.add(bt);
        }
        if (pool.isEmpty()) pool.add(BirdType.PIGEON);
        return pool;
    }

    private List<BirdType> adventureAllowedBirds(AdventureBattle battle) {
        List<BirdType> pool = new ArrayList<>();
        if (battle == null) return pool;
        if (battle.requiredPlayerType != null) {
            pool.add(battle.requiredPlayerType);
            return pool;
        }
        for (BirdType bt : BirdType.values()) {
            if (!isAdventureBirdUnlocked(bt)) continue;
            if (battle.allowedBirds != null && !battle.allowedBirds.contains(bt)) continue;
            pool.add(bt);
        }
        if (pool.isEmpty()) pool.add(BirdType.PIGEON);
        return pool;
    }

    private List<String> adventureSkinOptions(BirdType type) {
        List<String> options = new ArrayList<>();
        options.add(null);
        if (type == null) return options;
        if (type == BirdType.PIGEON) {
            if (cityPigeonUnlocked) options.add("CITY_PIGEON");
            if (noirPigeonUnlocked) options.add("NOIR_PIGEON");
        } else if (type == BirdType.EAGLE) {
            if (eagleSkinUnlocked) options.add("SKY_KING_EAGLE");
        } else {
            if (isClassicRewardUnlocked(type)) options.add(classicSkinDataKey(type));
        }
        return options;
    }

    private String normalizeAdventureSkinChoice(BirdType type, String skinKey) {
        if (type == null || skinKey == null) return null;
        if ("CITY_PIGEON".equals(skinKey) && type == BirdType.PIGEON && cityPigeonUnlocked) return skinKey;
        if ("NOIR_PIGEON".equals(skinKey) && type == BirdType.PIGEON && noirPigeonUnlocked) return skinKey;
        if ("SKY_KING_EAGLE".equals(skinKey) && type == BirdType.EAGLE && eagleSkinUnlocked) return skinKey;
        if (skinKey.startsWith("CLASSIC_SKIN_") && type != BirdType.PIGEON && type != BirdType.EAGLE) {
            String expected = classicSkinDataKey(type);
            if (skinKey.equals(expected) && isClassicRewardUnlocked(type)) return skinKey;
        }
        return null;
    }

    private String adventureSkinLabel(BirdType type, String skinKey) {
        if (skinKey == null) return "SKIN: BASE";
        if ("CITY_PIGEON".equals(skinKey)) return "SKIN: CITY PIGEON";
        if ("NOIR_PIGEON".equals(skinKey)) return "SKIN: NOIR PIGEON";
        if ("SKY_KING_EAGLE".equals(skinKey)) return "SKIN: SKY KING";
        if (skinKey.startsWith("CLASSIC_SKIN_")) return "SKIN: " + classicRewardFor(type);
        return "SKIN: BASE";
    }

    private void applySkinChoiceToBird(Bird bird, BirdType type, String skinKey) {
        if (bird == null || type == null) return;
        bird.isCitySkin = false;
        bird.isNoirSkin = false;
        bird.isClassicSkin = false;

        if (skinKey == null) return;
        if (type == BirdType.PIGEON) {
            if ("CITY_PIGEON".equals(skinKey)) bird.isCitySkin = true;
            else if ("NOIR_PIGEON".equals(skinKey)) bird.isNoirSkin = true;
            return;
        }
        if (type == BirdType.EAGLE) {
            if ("SKY_KING_EAGLE".equals(skinKey)) bird.isClassicSkin = true;
            return;
        }
        if (skinKey.equals(classicSkinDataKey(type)) && isClassicRewardUnlocked(type)) {
            bird.isClassicSkin = true;
        }
    }

    private String adventureRosterText() {
        List<String> names = new ArrayList<>();
        for (BirdType bt : BirdType.values()) {
            if (isAdventureBirdUnlocked(bt)) names.add(bt.name);
        }
        return names.isEmpty() ? "Pigeon" : String.join(", ", names);
    }

    private AdventureChapter activeAdventureChapter() {
        if (adventureChapters.length == 0) return null;
        adventureChapterIndex = Math.max(0, Math.min(adventureChapterIndex, adventureChapters.length - 1));
        return adventureChapters[adventureChapterIndex];
    }

    private AdventureBattle activeAdventureBattle() {
        AdventureChapter chapter = activeAdventureChapter();
        if (chapter == null || chapter.battles.length == 0) return null;
        adventureBattleIndex = Math.max(0, Math.min(adventureBattleIndex, chapter.battles.length - 1));
        return chapter.battles[adventureBattleIndex];
    }

    private boolean hasAnyLockedBirds() {
        for (BirdType bt : BirdType.values()) {
            if (!isBirdUnlocked(bt)) return true;
        }
        return false;
    }

    private boolean falconChallengeAvailable() {
        return classicCompleted[BirdType.EAGLE.ordinal()] || falconUnlocked;
    }

    private boolean phoenixChallengeAvailable() {
        return classicCompleted[PHOENIX_CLASSIC_GATE.ordinal()] || phoenixUnlocked;
    }

    private String classicCharacterReward(BirdType type) {
        if (type == BirdType.EAGLE) {
            return falconUnlocked ? "Falcon Character" : "Falcon Boss Challenge";
        }
        if (type == PHOENIX_CLASSIC_GATE) {
            return phoenixUnlocked ? "Phoenix Character" : "Phoenix Boss Challenge";
        }
        if (type == BirdType.HUMMINGBIRD) {
            return "Titmouse Character";
        }
        return "";
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
        FALCON("Falcon", 10, 18, 4.4, Color.rgb(176, 95, 55), 0.64, "Echo of Eagle: precision dive + sweetspot damage"),
        PHOENIX("Phoenix", 8, 20, 4.6, Color.ORANGERED, 0.66, "Rebirth Blaze (heal + fireburst)"),
        HUMMINGBIRD("Hummingbird", 6, 23, 5.0, Color.LIME, 0.85, "Hover/Fly + Nectar Frenzy (stings + lifesteal)"),
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

        if (competitionModeEnabled && !storyModeActive && !adventureModeActive && !isTrialMode && !classicModeActive && !matchEnded && matchTimer <= 0) {
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
        if (!(competitionModeEnabled && !storyModeActive && !adventureModeActive && !isTrialMode && !classicModeActive)) {
            shakeIntensity = suddenDeath.updateAndSpawn(
                    crowMinions,
                    random,
                    WORLD_WIDTH,
                    WORLD_HEIGHT,
                    shakeIntensity,
                    matchEnded
            );
        }

        if (!(competitionModeEnabled && !storyModeActive && !adventureModeActive && !isTrialMode && !classicModeActive)
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
                (teamModeEnabled && !isTrialMode && !storyModeActive && !adventureModeActive && !classicModeActive)
                        || (storyModeActive && storyTeamMode)
                        || (adventureModeActive && adventureTeamMode)
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
            case BATTLEFIELD -> {
                for (int i = 0; i < 520; i++) {
                    double ratio = i / 520.0;
                    Color c = Color.web("#7EC8FF").interpolate(Color.web("#DFF6F0"), ratio);
                    g.setFill(c);
                    g.fillRect(0, i * (WORLD_HEIGHT / 520.0), WORLD_WIDTH, WORLD_HEIGHT / 520.0 + 3);
                }
                g.setFill(Color.web("#1B5E20").darker());
                for (double tx : new double[]{700, 1700, 2700, 3700, 4700, 5600}) {
                    g.fillRect(tx - 260, GROUND_Y - 1400, 520, 1100);
                    g.setFill(Color.web("#2E7D32").darker());
                    g.fillOval(tx - 520, GROUND_Y - 1550, 1040, 650);
                    g.setFill(Color.web("#1B5E20").darker());
                }
                double mistY = battlefieldIslandY - 260;
                g.setFill(Color.web("#B2DFDB", 0.25));
                g.fillRect(0, mistY, WORLD_WIDTH, 520);

                double fadeStart = battlefieldIslandY + 120;
                for (int i = 0; i < 220; i++) {
                    double ratio = i / 220.0;
                    Color c = Color.web("#1B2B22").interpolate(Color.BLACK, ratio);
                    g.setFill(c);
                    g.fillRect(0, fadeStart + i * 8, WORLD_WIDTH, 8);
                }

                g.setStroke(Color.web("#3E2723"));
                g.setLineWidth(4);
                for (Platform p : platforms) {
                    g.setFill(Color.web("#5D4037"));
                    g.fillRoundRect(p.x, p.y, p.w, p.h, 34, 34);
                    g.setFill(Color.web("#2E7D32"));
                    g.fillRoundRect(p.x + 8, p.y - 12, p.w - 16, p.h + 16, 34, 34);
                    g.strokeRoundRect(p.x, p.y, p.w, p.h, 34, 34);
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
            drawPowerUpSprite(g, p, offset);
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
            if (b != null && b.health > 0) {
                b.draw(g);
                drawPlayerTag(g, b);
            }
        }

        g.restore();
    }

    private void drawPlayerTag(GraphicsContext g, Bird b) {
        String tag = "P" + (b.playerIndex + 1);
        Font font = Font.font("Arial Black", 20);
        Text text = new Text(tag);
        text.setFont(font);
        double textW = text.getLayoutBounds().getWidth();
        double textH = text.getLayoutBounds().getHeight();

        double drawSize = 80 * b.sizeMultiplier;
        double centerX = b.x + drawSize / 2.0;
        double boxW = textW + 14;
        double boxH = textH + 6;
        double boxX = centerX - boxW / 2.0;
        double boxY = b.y - boxH - 12;

        g.setFill(Color.BLACK.deriveColor(0, 1, 1, 0.6));
        g.fillRoundRect(boxX, boxY, boxW, boxH, 10, 10);
        g.setStroke(Color.WHITE.deriveColor(0, 1, 1, 0.85));
        g.setLineWidth(1.4);
        g.strokeRoundRect(boxX, boxY, boxW, boxH, 10, 10);
        g.setFill(Color.WHITE);
        g.setFont(font);
        g.fillText(tag, centerX - textW / 2.0, boxY + boxH - 4);
    }

    private void drawPowerUpSprite(GraphicsContext g, PowerUp p, double offset) {
        double cx = p.x;
        double cy = p.y + offset;

        g.setFill(p.type.color.deriveColor(0, 1, 1, 0.22));
        g.fillOval(cx - 44, cy - 44, 88, 88);
        g.setStroke(Color.WHITE.deriveColor(0, 1, 1, 0.8));
        g.setLineWidth(2.2);
        g.strokeOval(cx - 31, cy - 31, 62, 62);

        switch (p.type) {
            case HEALTH -> {
                g.setFill(Color.rgb(210, 20, 20));
                g.fillOval(cx - 22, cy - 22, 44, 44);
                g.setFill(Color.WHITE);
                g.fillRoundRect(cx - 6, cy - 16, 12, 32, 6, 6);
                g.fillRoundRect(cx - 16, cy - 6, 32, 12, 6, 6);
            }
            case SPEED -> {
                g.setFill(Color.CYAN.brighter());
                g.fillPolygon(new double[]{cx - 20, cx - 2, cx - 20}, new double[]{cy - 14, cy, cy + 14}, 3);
                g.fillPolygon(new double[]{cx - 2, cx + 16, cx - 2}, new double[]{cy - 14, cy, cy + 14}, 3);
                g.setStroke(Color.WHITE);
                g.setLineWidth(2);
                g.strokeLine(cx - 26, cy - 18, cx - 10, cy - 18);
                g.strokeLine(cx - 26, cy + 18, cx - 10, cy + 18);
            }
            case RAGE -> {
                g.setFill(Color.ORANGERED);
                g.fillPolygon(
                        new double[]{cx - 18, cx - 6, cx - 2, cx + 4, cx + 16, cx + 6, cx - 2},
                        new double[]{cy + 18, cy + 2, cy + 12, cy - 14, cy + 4, cy, cy + 18},
                        7
                );
                g.setStroke(Color.YELLOW);
                g.setLineWidth(2);
                g.strokeLine(cx - 14, cy + 18, cx + 12, cy - 8);
            }
            case SHRINK -> {
                g.setStroke(Color.MEDIUMPURPLE.brighter());
                g.setLineWidth(3);
                g.strokeOval(cx - 18, cy - 18, 36, 36);
                g.setFill(Color.MEDIUMPURPLE.brighter());
                g.fillPolygon(new double[]{cx - 24, cx - 12, cx - 12}, new double[]{cy, cy - 6, cy + 6}, 3);
                g.fillPolygon(new double[]{cx + 24, cx + 12, cx + 12}, new double[]{cy, cy - 6, cy + 6}, 3);
                g.fillPolygon(new double[]{cx, cx - 6, cx + 6}, new double[]{cy - 24, cy - 12, cy - 12}, 3);
                g.fillPolygon(new double[]{cx, cx - 6, cx + 6}, new double[]{cy + 24, cy + 12, cy + 12}, 3);
            }
            case NEON -> {
                g.setStroke(Color.MAGENTA.brighter());
                g.setLineWidth(3);
                for (int i = 0; i < 6; i++) {
                    double a = i * Math.PI / 3;
                    g.strokeLine(cx, cy, cx + Math.cos(a) * 22, cy + Math.sin(a) * 22);
                }
                g.setFill(Color.CYAN.brighter());
                g.fillOval(cx - 8, cy - 8, 16, 16);
            }
            case THERMAL -> {
                g.setFill(Color.GOLD);
                g.fillPolygon(new double[]{cx - 14, cx, cx + 14}, new double[]{cy + 6, cy - 16, cy + 6}, 3);
                g.fillPolygon(new double[]{cx - 10, cx, cx + 10}, new double[]{cy + 18, cy, cy + 18}, 3);
                g.setStroke(Color.WHITE);
                g.setLineWidth(2);
                g.strokeLine(cx, cy + 20, cx, cy - 22);
            }
            case VINE_GRAPPLE -> {
                g.setStroke(Color.LIMEGREEN.brighter());
                g.setLineWidth(4);
                g.strokeOval(cx - 15, cy - 15, 30, 30);
                g.strokeLine(cx + 10, cy + 10, cx + 22, cy + 22);
                g.setFill(Color.FORESTGREEN);
                g.fillOval(cx + 18, cy + 20, 10, 10);
            }
            case OVERCHARGE -> {
                g.setFill(Color.DEEPSKYBLUE.brighter());
                g.fillPolygon(
                        new double[]{cx - 10, cx + 2, cx - 2, cx + 12, cx, cx + 4},
                        new double[]{cy - 18, cy - 3, cy - 3, cy + 18, cy + 4, cy + 4},
                        6
                );
                g.setStroke(Color.WHITE);
                g.setLineWidth(2);
                g.strokeLine(cx - 20, cy - 14, cx - 10, cy - 14);
                g.strokeLine(cx + 12, cy + 14, cx + 22, cy + 14);
            }
            case TITAN -> {
                g.setFill(Color.DARKGOLDENROD);
                g.fillRoundRect(cx - 16, cy - 20, 32, 40, 10, 10);
                g.setStroke(Color.GOLD.brighter());
                g.setLineWidth(2.6);
                g.strokeRoundRect(cx - 16, cy - 20, 32, 40, 10, 10);
                g.setFill(Color.WHITE);
                g.fillPolygon(new double[]{cx - 3, cx + 3, cx + 3, cx + 8, cx, cx - 8, cx - 3},
                        new double[]{cy - 14, cy - 14, cy - 2, cy - 2, cy + 12, cy -2, cy -2}, 7);
            }
        }
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
        stage.show();
        stage.setFullScreen(true);
    }

    private void showMenu(Stage stage) {
        storyModeActive = false;
        storyReplayMode = false;
        adventureModeActive = false;
        adventureReplayMode = false;
        currentAdventureBattle = null;
        adventureTeamMode = false;
        Arrays.fill(adventureTeams, 1);
        classicModeActive = false;
        classicEncounter = null;
        classicRun.clear();
        classicRoundIndex = 0;
        classicTeamMode = false;
        Arrays.fill(classicTeams, 1);
        activeBossChallenge = BossChallengeType.NONE;
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
        showHub(stage);
    }

    private void drawSelectionSprite(Canvas canvas, BirdType type, String skinKey, boolean randomPick) {
        GraphicsContext g = canvas.getGraphicsContext2D();
        double w = canvas.getWidth();
        double h = canvas.getHeight();
        g.clearRect(0, 0, w, h);

        if (randomPick || type == null) {
            g.setFill(Color.web("#263238"));
            g.fillOval(w * 0.15, h * 0.15, w * 0.7, h * 0.7);
            g.setFill(Color.web("#FFD54F"));
            g.setFont(Font.font("Impact", Math.max(24, w * 0.35)));
            g.setTextAlign(TextAlignment.CENTER);
            g.fillText("?", w / 2.0, h * 0.62);
            g.setTextAlign(TextAlignment.LEFT);
            return;
        }

        Color base = type.color;
        Color accent = Color.BLACK;
        if ("CITY_PIGEON".equals(skinKey)) {
            base = Color.web("#FFD700");
            accent = Color.BLACK;
        } else if ("NOIR_PIGEON".equals(skinKey)) {
            base = Color.rgb(18, 18, 18);
            accent = Color.web("#F44336");
        } else if ("SKY_KING_EAGLE".equals(skinKey)) {
            base = Color.GOLD;
            accent = Color.ORANGE;
        } else if (skinKey != null && skinKey.startsWith("CLASSIC_SKIN_")) {
            base = classicSkinPrimaryColor(type);
            accent = classicSkinAccentColor(type);
        }

        double size = Math.min(w, h) * 0.68;
        double x = (w - size) / 2.0;
        double y = (h - size) / 2.0;

        g.setFill(base);
        g.fillOval(x, y, size, size);

        g.setFill(base.darker());
        g.fillOval(x - size * 0.18, y + size * 0.15, size * 0.52, size * 0.56);

        g.setFill(Color.WHITE);
        g.fillOval(x + size * 0.58, y + size * 0.25, size * 0.22, size * 0.22);
        g.setFill(accent);
        g.fillOval(x + size * 0.64, y + size * 0.31, size * 0.1, size * 0.1);

        g.setFill(Color.GOLDENROD);
        double beakX = x + size * 0.72;
        double beakY = y + size * 0.48;
        g.fillPolygon(
                new double[]{beakX, beakX + size * 0.28, beakX},
                new double[]{beakY, beakY + size * 0.08, beakY + size * 0.16},
                3
        );

        if (skinKey != null && skinKey.startsWith("CLASSIC_SKIN_")) {
            g.setStroke(accent);
            g.setLineWidth(3);
            g.strokeOval(x - size * 0.08, y - size * 0.08, size * 1.16, size * 1.16);
        }
    }

    private static final class BirdIconSpot {
        final BirdType type;
        final boolean random;
        final double cx;
        final double cy;

        BirdIconSpot(BirdType type, boolean random, double cx, double cy) {
            this.type = type;
            this.random = random;
            this.cx = cx;
            this.cy = cy;
        }
    }

    private void drawRosterSprite(Canvas canvas, BirdType type, String skinKey, boolean randomPick) {
        GraphicsContext g = canvas.getGraphicsContext2D();
        double w = canvas.getWidth();
        double h = canvas.getHeight();
        g.clearRect(0, 0, w, h);

        if (randomPick || type == null) {
            g.setFill(Color.web("#263238"));
            g.fillRoundRect(w * 0.1, h * 0.1, w * 0.8, h * 0.8, 20, 20);
            g.setFill(Color.web("#FFD54F"));
            g.setFont(Font.font("Impact", Math.max(28, w * 0.32)));
            g.setTextAlign(TextAlignment.CENTER);
            g.fillText("?", w / 2.0, h * 0.62);
            g.setTextAlign(TextAlignment.LEFT);
            return;
        }

        Bird preview = new Bird(0, type, 0, this);
        preview.suppressSelectEffects = true;
        applySkinChoiceToBird(preview, type, skinKey);
        preview.sizeMultiplier = 0.6;
        double drawSize = 80 * preview.sizeMultiplier;
        preview.x = (w - drawSize) / 2.0;
        preview.y = (h - drawSize) / 2.0 + 2;
        preview.facingRight = true;
        preview.draw(g);
    }

    private void showHub(Stage stage) {
        playMenuMusic();
        BorderPane root = new BorderPane();
        root.setPadding(new Insets(28, 40, 28, 40));
        root.setStyle("-fx-background-color: linear-gradient(to bottom, #0B1D2B, #1B2E3C, #1F3D4C);");

        Label title = new Label("BIRD FIGHT 3");
        title.setFont(Font.font("Impact", FontWeight.BOLD, 96));
        title.setTextFill(Color.web("#FFE082"));
        title.setEffect(new DropShadow(40, Color.BLACK));

        Label hubLabel = new Label("CENTRAL HUB");
        hubLabel.setFont(Font.font("Consolas", 28));
        hubLabel.setTextFill(Color.web("#80DEEA"));

        VBox titleBox = new VBox(6, title, hubLabel);
        titleBox.setAlignment(Pos.CENTER);

        Label coins = new Label("BIRD COINS: " + birdCoins);
        coins.setFont(Font.font("Consolas", 24));
        coins.setTextFill(Color.web("#FFD54F"));

        BorderPane top = new BorderPane();
        top.setCenter(titleBox);
        top.setRight(coins);
        BorderPane.setAlignment(coins, Pos.TOP_RIGHT);

        GridPane nav = new GridPane();
        nav.setHgap(26);
        nav.setVgap(26);
        nav.setAlignment(Pos.CENTER);

        Button fightBtn = uiFactory.action("FIGHT", 520, 130, 48, "#FF7043", 30, () -> showFightSetup(stage));
        Button adventureBtn = uiFactory.action("ADVENTURE", 520, 130, 48, "#26A69A", 30, () -> showAdventureHub(stage));
        Button classicBtn = uiFactory.action("CLASSIC & MORE", 520, 130, 40, "#1E88E5", 28, () -> showClassicMoreMenu(stage));
        Button shopBtn = uiFactory.action("SHOP", 520, 130, 48, "#FBC02D", 30, () -> showShop(stage));
        Button onlineBtn = uiFactory.action("ONLINE", 520, 130, 48, "#8D6E63", 30, () -> showOnlinePlaceholder(stage));

        nav.add(fightBtn, 0, 0);
        nav.add(adventureBtn, 1, 0);
        nav.add(classicBtn, 0, 1);
        nav.add(shopBtn, 1, 1);
        nav.add(onlineBtn, 2, 1);

        Button achievementsBtn = uiFactory.action("ACHIEVEMENTS", 360, 86, 34, "#455A64", 22, () -> showAchievements(stage));
        Button exitBtn = uiFactory.action("EXIT", 260, 86, 34, "#D32F2F", 22, () -> confirmExitGame(stage));
        HBox footer = new HBox(18, achievementsBtn, exitBtn);
        footer.setAlignment(Pos.CENTER);

        root.setTop(top);
        root.setCenter(nav);
        root.setBottom(footer);

        Scene scene = new Scene(root, WIDTH, HEIGHT);
        scene.addEventFilter(KeyEvent.KEY_PRESSED, e -> {
            if (e.getCode() == KeyCode.ESCAPE) {
                playButtonClick();
                confirmExitGame(stage);
                e.consume();
            }
        });
        setupKeyboardNavigation(scene);
        applyConsoleHighlight(scene);
        setScenePreservingFullscreen(stage, scene);
        fightBtn.requestFocus();
    }

    private void showFightSetup(Stage stage) {
        playMenuMusic();
        activePlayers = Math.max(2, Math.min(4, activePlayers));

        BorderPane root = new BorderPane();
        root.setPadding(new Insets(24, 34, 24, 34));
        root.setStyle("-fx-background-color: linear-gradient(to bottom, #091421, #152738, #1F3443);");

        Button back = uiFactory.action("BACK TO HUB", 320, 86, 32, "#D32F2F", 22, () -> showMenu(stage));
        Label title = new Label("FIGHT - LOCAL");
        title.setFont(Font.font("Impact", FontWeight.BOLD, 78));
        title.setTextFill(Color.web("#FFE082"));
        Label coins = new Label("BIRD COINS: " + birdCoins);
        coins.setFont(Font.font("Consolas", 22));
        coins.setTextFill(Color.web("#FFD54F"));

        Region spacer = new Region();
        HBox topBar = new HBox(18, back, title, spacer, coins);
        topBar.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);

        HBox countBox = new HBox(16);
        countBox.setAlignment(Pos.CENTER_LEFT);
        Button[] countButtons = new Button[3];
        final Runnable[] refreshLayoutRef = new Runnable[1];
        Runnable refreshCountButtons = () -> {
            for (int i = 0; i < countButtons.length; i++) {
                Button b = countButtons[i];
                if (b == null) continue;
                int value = i + 2;
                boolean active = activePlayers == value;
                b.setStyle("-fx-background-color: " + (active ? "#2E7D32" : "#4CAF50") + "; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 30;");
                b.setOpacity(active ? 1.0 : 0.75);
            }
        };
        for (int i = 2; i <= 4; i++) {
            int n = i;
            Button b = uiFactory.action(n + "P", 140, 76, 32, "#4CAF50", 24, () -> {
                activePlayers = n;
                refreshCountButtons.run();
                if (refreshLayoutRef[0] != null) refreshLayoutRef[0].run();
            });
            countButtons[i - 2] = b;
            countBox.getChildren().add(b);
        }
        refreshCountButtons.run();

        teamModeToggleButton = uiFactory.action("TEAM MODE: OFF", 360, 76, 28, "#546E7A", 22, () -> {
            teamModeEnabled = !teamModeEnabled;
            refreshTeamToggleButton();
        });
        refreshTeamToggleButton();

        Button settingsBtn = uiFactory.action("GAME SETTINGS", 300, 76, 28, "#455A64", 20, () -> {
            settingsReturn = () -> showFightSetup(stage);
            showGameSettings(stage);
        });

        HBox controls = new HBox(18, countBox, teamModeToggleButton, settingsBtn);
        controls.setAlignment(Pos.CENTER_LEFT);

        VBox top = new VBox(14, topBar, controls);
        root.setTop(top);

        List<BirdType> availableBirds = unlockedBirdPool();
        List<BirdType> gridBirds = new ArrayList<>(availableBirds);
        gridBirds.add(null); // random slot

        Pane selectionPane = new Pane();
        double paneW = 1500;
        double paneH = 520;
        selectionPane.setPrefSize(paneW, paneH);
        selectionPane.setStyle("-fx-background-color: rgba(0,0,0,0.35); -fx-border-color: #90A4AE; -fx-border-width: 3; -fx-background-radius: 20; -fx-border-radius: 20;");

        List<BirdIconSpot> spots = new ArrayList<>();
        Map<BirdType, BirdIconSpot> spotByType = new HashMap<>();
        BirdIconSpot randomSpot = null;

        int columns = 4;
        int rows = (int) Math.ceil(gridBirds.size() / (double) columns);
        double dockH = 120;
        double gridAreaH = paneH - dockH - 20;
        double cellW = paneW / columns;
        double cellH = gridAreaH / Math.max(1, rows);
        double iconSize = 80;

        for (int i = 0; i < gridBirds.size(); i++) {
            BirdType type = gridBirds.get(i);
            boolean isRandom = type == null;
            int col = i % columns;
            int row = i / columns;
            double centerX = col * cellW + cellW / 2.0;
            double centerY = 20 + row * cellH + cellH / 2.0;

            Canvas icon = new Canvas(iconSize, iconSize);
            drawRosterSprite(icon, type, null, isRandom);

            Label name = new Label(isRandom ? "RANDOM" : type.name.toUpperCase());
            name.setFont(Font.font("Consolas", 16));
            name.setTextFill(Color.web("#ECEFF1"));

            VBox card = new VBox(6, icon, name);
            card.setAlignment(Pos.CENTER);
            card.setLayoutX(centerX - iconSize / 2.0);
            card.setLayoutY(centerY - iconSize / 2.0);

            selectionPane.getChildren().add(card);
            BirdIconSpot spot = new BirdIconSpot(type, isRandom, centerX, centerY);
            spots.add(spot);
            if (type != null) {
                spotByType.put(type, spot);
            } else {
                randomSpot = spot;
            }
        }

        Region selectorDock = new Region();
        selectorDock.setPrefSize(560, dockH);
        selectorDock.setLayoutX(20);
        selectorDock.setLayoutY(paneH - dockH - 10);
        selectorDock.setStyle("-fx-background-color: rgba(10,10,10,0.6); -fx-border-color: #FFD54F; -fx-border-width: 2; -fx-background-radius: 18; -fx-border-radius: 18;");
        selectionPane.getChildren().add(selectorDock);

        Label dockLabel = new Label("PLAYER SELECTORS");
        dockLabel.setFont(Font.font("Consolas", 18));
        dockLabel.setTextFill(Color.web("#FFE082"));
        dockLabel.setLayoutX(40);
        dockLabel.setLayoutY(paneH - dockH - 2);
        selectionPane.getChildren().add(dockLabel);

        Point2D[] dockPositions = new Point2D[4];
        for (int i = 0; i < 4; i++) {
            double dx = 70 + i * 120;
            double dy = paneH - dockH / 2.0 - 10;
            dockPositions[i] = new Point2D(dx, dy);
        }

        VBox[] fightSlots = new VBox[4];
        Canvas[] portraits = new Canvas[4];
        Label[] nameLabels = new Label[4];
        Button[] skinButtons = new Button[4];

        Button readyBtn = uiFactory.action("FLY INTO BATTLE", 680, 110, 52, "#FF1744", 32, () -> {
            stageSelectReturn = () -> showFightSetup(stage);
            showStageSelect(stage);
        });
        readyBtn.setVisible(false);
        readyBtn.setManaged(false);

        Runnable updateReadyBanner = () -> {
            boolean ready = true;
            for (int i = 0; i < activePlayers; i++) {
                if (!fightRandomSelected[i] && fightSelectedBirds[i] == null) {
                    ready = false;
                    break;
                }
            }
            readyBtn.setVisible(ready);
            readyBtn.setManaged(ready);
        };

        Runnable[] updateSlot = new Runnable[4];

        for (int i = 0; i < 4; i++) {
            int idx = i;

            Label pLabel = new Label("P" + (idx + 1));
            pLabel.setFont(Font.font("Impact", 28));
            pLabel.setTextFill(Color.web("#FFCC80"));

            Button aiToggle = new Button(isAI[idx] ? "CPU" : "PLAYER");
            aiToggle.setPrefSize(140, 40);
            aiToggle.setFont(Font.font("Consolas", 18));
            aiToggle.setStyle("-fx-background-color: " + (isAI[idx] ? "#B71C1C" : "#1565C0") + "; -fx-text-fill: white;");
            aiToggle.setOnAction(e -> {
                playButtonClick();
                isAI[idx] = !isAI[idx];
                aiToggle.setText(isAI[idx] ? "CPU" : "PLAYER");
                aiToggle.setStyle("-fx-background-color: " + (isAI[idx] ? "#B71C1C" : "#1565C0") + "; -fx-text-fill: white;");
                refreshCpuButton(idx);
            });

            portraits[idx] = new Canvas(140, 140);
            nameLabels[idx] = new Label("SELECT");
            nameLabels[idx].setFont(Font.font("Consolas", 18));
            nameLabels[idx].setTextFill(Color.web("#ECEFF1"));

            skinButtons[idx] = new Button("SKIN: BASE");
            skinButtons[idx].setPrefSize(180, 36);
            skinButtons[idx].setFont(Font.font("Consolas", 16));
            skinButtons[idx].setStyle("-fx-background-color: #37474F; -fx-text-fill: white;");

            Button cpuBtn = new Button("CPU LV: " + cpuLevels[idx]);
            cpuBtn.setPrefSize(150, 36);
            cpuBtn.setFont(Font.font("Consolas", 16));
            cpuBtn.setOnAction(e -> {
                playButtonClick();
                if (!isAI[idx]) return;
                cpuLevels[idx] = cpuLevels[idx] % 9 + 1;
                refreshCpuButton(idx);
            });
            cpuButtons[idx] = cpuBtn;
            refreshCpuButton(idx);

            updateSlot[idx] = () -> {
                boolean randomPick = fightRandomSelected[idx];
                BirdType type = fightSelectedBirds[idx];
                String skinKey = fightSelectedSkinKeys[idx];
                if (randomPick) {
                    type = null;
                    skinKey = null;
                } else {
                    skinKey = normalizeAdventureSkinChoice(type, skinKey);
                    fightSelectedSkinKeys[idx] = skinKey;
                }
                drawRosterSprite(portraits[idx], type, skinKey, randomPick);
                nameLabels[idx].setText(randomPick ? "RANDOM" : (type != null ? type.name.toUpperCase() : "SELECT"));
                if (type == null || randomPick) {
                    skinButtons[idx].setDisable(true);
                    skinButtons[idx].setOpacity(0.6);
                    skinButtons[idx].setText("SKIN: BASE");
                } else {
                    List<String> options = adventureSkinOptions(type);
                    skinButtons[idx].setDisable(options.size() <= 1);
                    skinButtons[idx].setOpacity(options.size() <= 1 ? 0.6 : 1.0);
                    skinButtons[idx].setText(adventureSkinLabel(type, skinKey));
                }
                updateReadyBanner.run();
            };

            skinButtons[idx].setOnAction(e -> {
                playButtonClick();
                BirdType type = fightSelectedBirds[idx];
                if (type == null || fightRandomSelected[idx]) return;
                List<String> options = adventureSkinOptions(type);
                if (options.size() <= 1) return;
                String current = fightSelectedSkinKeys[idx];
                int pos = options.indexOf(current);
                if (pos < 0) pos = 0;
                fightSelectedSkinKeys[idx] = options.get((pos + 1) % options.size());
                updateSlot[idx].run();
            });

            VBox slot = new VBox(8, pLabel, aiToggle, portraits[idx], nameLabels[idx], skinButtons[idx], cpuBtn);
            slot.setAlignment(Pos.CENTER);
            slot.setPadding(new Insets(10));
            slot.setPrefWidth(260);
            slot.setStyle("-fx-background-color: rgba(0,0,0,0.45); -fx-background-radius: 16; -fx-border-color: #78909C; -fx-border-width: 2; -fx-border-radius: 16;");
            fightSlots[idx] = slot;
        }

        Circle[] selectors = new Circle[4];
        Text[] selectorLabels = new Text[4];
        boolean[] selectorLocked = new boolean[4];
        for (int i = 0; i < 4; i++) {
            int idx = i;
            Circle selector = new Circle(26);
            selector.setFill(Color.web("rgba(255,213,79,0.15)"));
            selector.setStroke(Color.web("#FFD54F"));
            selector.setStrokeWidth(3);
            selector.setVisible(i < activePlayers);
            selector.setManaged(false);
            selectors[i] = selector;
            selectionPane.getChildren().add(selector);

            Text label = new Text("P" + (idx + 1));
            label.setFont(Font.font("Impact", 16));
            label.setFill(Color.web("#FFD54F"));
            label.setManaged(false);
            selectorLabels[i] = label;
            selectionPane.getChildren().add(label);

            final double[] dragOffset = new double[2];
            selector.setOnMousePressed(e -> {
                if (selectorLocked[idx]) {
                    selectorLocked[idx] = false;
                    updateReadyBanner.run();
                    return;
                }
                Point2D local = selectionPane.sceneToLocal(e.getSceneX(), e.getSceneY());
                dragOffset[0] = selector.getCenterX() - local.getX();
                dragOffset[1] = selector.getCenterY() - local.getY();
            });
            selector.setOnMouseDragged(e -> {
                if (selectorLocked[idx]) return;
                Point2D local = selectionPane.sceneToLocal(e.getSceneX(), e.getSceneY());
                double nx = local.getX() + dragOffset[0];
                double ny = local.getY() + dragOffset[1];
                nx = Math.max(40, Math.min(nx, selectionPane.getPrefWidth() - 40));
                ny = Math.max(40, Math.min(ny, selectionPane.getPrefHeight() - 40));
                selector.setCenterX(nx);
                selector.setCenterY(ny);
                label.setX(nx - 10);
                label.setY(ny + 6);
            });
            selector.setOnMouseReleased(e -> {
                if (selectorLocked[idx]) return;
                BirdIconSpot best = null;
                double bestDist = Double.MAX_VALUE;
                for (BirdIconSpot spot : spots) {
                    double dx = selector.getCenterX() - spot.cx;
                    double dy = selector.getCenterY() - spot.cy;
                    double dist = dx * dx + dy * dy;
                    if (dist < bestDist) {
                        bestDist = dist;
                        best = spot;
                    }
                }
                if (best != null) {
                    selector.setCenterX(best.cx);
                    selector.setCenterY(best.cy);
                    label.setX(best.cx - 10);
                    label.setY(best.cy + 6);
                    if (best.random) {
                        fightRandomSelected[idx] = true;
                        fightSelectedBirds[idx] = null;
                    } else {
                        fightRandomSelected[idx] = false;
                        fightSelectedBirds[idx] = best.type;
                    }
                    selectorLocked[idx] = true;
                    updateSlot[idx].run();
                } else {
                    Point2D dock = dockPositions[idx];
                    selector.setCenterX(dock.getX());
                    selector.setCenterY(dock.getY());
                    label.setX(dock.getX() - 10);
                    label.setY(dock.getY() + 6);
                }
            });
        }

        final BirdIconSpot finalRandomSpot = randomSpot;
        Runnable refreshLayout = () -> {
            for (int i = 0; i < 4; i++) {
                boolean active = i < activePlayers;
                fightSlots[i].setVisible(active);
                fightSlots[i].setManaged(active);
                selectors[i].setVisible(active);
                selectorLabels[i].setVisible(active);
                selectorLocked[i] = selectorLocked[i] && active;
                if (!active) continue;
                BirdIconSpot spot = fightRandomSelected[i] ? finalRandomSpot : spotByType.get(fightSelectedBirds[i]);
                if (spot != null) {
                    selectors[i].setCenterX(spot.cx);
                    selectors[i].setCenterY(spot.cy);
                    selectorLabels[i].setX(spot.cx - 10);
                    selectorLabels[i].setY(spot.cy + 6);
                } else {
                    Point2D dock = dockPositions[i];
                    selectors[i].setCenterX(dock.getX());
                    selectors[i].setCenterY(dock.getY());
                    selectorLabels[i].setX(dock.getX() - 10);
                    selectorLabels[i].setY(dock.getY() + 6);
                }
                updateSlot[i].run();
            }
            updateReadyBanner.run();
        };
        refreshLayoutRef[0] = refreshLayout;

        refreshLayout.run();
        for (Node node : countBox.getChildren()) {
            if (node instanceof Button btn) {
                btn.addEventHandler(javafx.event.ActionEvent.ACTION, e -> refreshLayout.run());
            }
        }

        HBox playerBar = new HBox(18, fightSlots);
        playerBar.setAlignment(Pos.CENTER);
        playerBar.setPadding(new Insets(12, 0, 0, 0));
        BorderPane.setMargin(playerBar, new Insets(10, 0, 0, 0));

        StackPane center = new StackPane(selectionPane, readyBtn);
        StackPane.setAlignment(readyBtn, Pos.BOTTOM_CENTER);
        StackPane.setMargin(readyBtn, new Insets(0, 0, 16, 0));

        root.setCenter(center);
        root.setBottom(playerBar);

        Scene scene = new Scene(root, WIDTH, HEIGHT);
        scene.addEventFilter(KeyEvent.KEY_PRESSED, e -> {
            boolean handled = false;
            handled |= handleSelectorKey(e, 0, selectors[0], selectorLabels[0], selectorLocked, spots, dockPositions, selectionPane, updateSlot[0], updateReadyBanner);
            handled |= handleSelectorKey(e, 1, selectors[1], selectorLabels[1], selectorLocked, spots, dockPositions, selectionPane, updateSlot[1], updateReadyBanner);
            handled |= handleSelectorKey(e, 2, selectors[2], selectorLabels[2], selectorLocked, spots, dockPositions, selectionPane, updateSlot[2], updateReadyBanner);
            handled |= handleSelectorKey(e, 3, selectors[3], selectorLabels[3], selectorLocked, spots, dockPositions, selectionPane, updateSlot[3], updateReadyBanner);
            if (handled) {
                e.consume();
            }
        });
        setupKeyboardNavigation(scene);
        applyConsoleHighlight(scene);
        setScenePreservingFullscreen(stage, scene);
        back.requestFocus();
    }

    private boolean handleSelectorKey(KeyEvent e,
                                      int idx,
                                      Circle selector,
                                      Text label,
                                      boolean[] selectorLocked,
                                      List<BirdIconSpot> spots,
                                      Point2D[] dockPositions,
                                      Pane selectionPane,
                                      Runnable updateSlot,
                                      Runnable updateReady) {
        if (idx < 0 || idx >= activePlayers) return false;
        if (selector == null || label == null) return false;

        KeyCode code = e.getCode();
        KeyCode left = switch (idx) {
            case 0 -> KeyCode.A;
            case 1 -> KeyCode.LEFT;
            case 2 -> KeyCode.F;
            case 3 -> KeyCode.J;
            default -> KeyCode.A;
        };
        KeyCode right = switch (idx) {
            case 0 -> KeyCode.D;
            case 1 -> KeyCode.RIGHT;
            case 2 -> KeyCode.H;
            case 3 -> KeyCode.L;
            default -> KeyCode.D;
        };
        KeyCode up = switch (idx) {
            case 0 -> KeyCode.W;
            case 1 -> KeyCode.UP;
            case 2 -> KeyCode.T;
            case 3 -> KeyCode.I;
            default -> KeyCode.W;
        };
        KeyCode down = switch (idx) {
            case 0 -> KeyCode.S;
            case 1 -> KeyCode.DOWN;
            case 2 -> KeyCode.G;
            case 3 -> KeyCode.K;
            default -> KeyCode.S;
        };
        KeyCode select = switch (idx) {
            case 0 -> KeyCode.ENTER;
            case 1 -> KeyCode.SLASH;
            case 2 -> KeyCode.U;
            case 3 -> KeyCode.P;
            default -> KeyCode.ENTER;
        };
        KeyCode altSelect = switch (idx) {
            case 0 -> KeyCode.SPACE;
            case 1 -> null;
            case 2 -> KeyCode.Y;
            case 3 -> KeyCode.O;
            default -> KeyCode.SPACE;
        };

        double step = 26;
        boolean moved = false;
        if (code == select || (altSelect != null && code == altSelect)) {
            if (selectorLocked[idx]) {
                selectorLocked[idx] = false;
                updateReady.run();
                return true;
            }
            BirdIconSpot best = null;
            double bestDist = Double.MAX_VALUE;
            for (BirdIconSpot spot : spots) {
                double dx = selector.getCenterX() - spot.cx;
                double dy = selector.getCenterY() - spot.cy;
                double dist = dx * dx + dy * dy;
                if (dist < bestDist) {
                    bestDist = dist;
                    best = spot;
                }
            }
            if (best != null) {
                selector.setCenterX(best.cx);
                selector.setCenterY(best.cy);
                label.setX(best.cx - 10);
                label.setY(best.cy + 6);
                if (best.random) {
                    fightRandomSelected[idx] = true;
                    fightSelectedBirds[idx] = null;
                } else {
                    fightRandomSelected[idx] = false;
                    fightSelectedBirds[idx] = best.type;
                }
                selectorLocked[idx] = true;
                updateSlot.run();
            }
            return true;
        }

        if (selectorLocked[idx]) return false;

        double nx = selector.getCenterX();
        double ny = selector.getCenterY();
        if (code == left) {
            nx -= step;
            moved = true;
        } else if (code == right) {
            nx += step;
            moved = true;
        } else if (code == up) {
            ny -= step;
            moved = true;
        } else if (code == down) {
            ny += step;
            moved = true;
        }
        if (moved) {
            nx = Math.max(40, Math.min(nx, selectionPane.getPrefWidth() - 40));
            ny = Math.max(40, Math.min(ny, selectionPane.getPrefHeight() - 40));
            selector.setCenterX(nx);
            selector.setCenterY(ny);
            label.setX(nx - 10);
            label.setY(ny + 6);
            return true;
        }
        return false;
    }

    private void showClassicMoreMenu(Stage stage) {
        playMenuMusic();
        BorderPane root = new BorderPane();
        root.setPadding(new Insets(28, 40, 28, 40));
        root.setStyle("-fx-background-color: linear-gradient(to bottom, #0A1A2A, #17324A);");

        Label title = new Label("CLASSIC & MORE");
        title.setFont(Font.font("Impact", FontWeight.BOLD, 84));
        title.setTextFill(Color.web("#FFE082"));

        Button classicBtn = uiFactory.action("CLASSIC MODE", 700, 140, 44, "#1565C0", 30, () -> showClassicBirdSelect(stage));
        Button bossBtn = uiFactory.action("BOSS CHALLENGES", 700, 140, 36, "#6A1B9A", 30, () -> showBossChallenges(stage));
        VBox options = new VBox(26, classicBtn, bossBtn);
        options.setAlignment(Pos.CENTER);

        Button back = uiFactory.action("BACK TO HUB", 360, 90, 34, "#D32F2F", 22, () -> showMenu(stage));
        HBox bottom = new HBox(back);
        bottom.setAlignment(Pos.CENTER);

        root.setTop(title);
        BorderPane.setAlignment(title, Pos.CENTER);
        root.setCenter(options);
        root.setBottom(bottom);

        Scene scene = new Scene(root, WIDTH, HEIGHT);
        setupKeyboardNavigation(scene);
        applyConsoleHighlight(scene);
        setScenePreservingFullscreen(stage, scene);
        classicBtn.requestFocus();
    }

    private void showOnlinePlaceholder(Stage stage) {
        playMenuMusic();
        VBox root = new VBox(30);
        root.setAlignment(Pos.CENTER);
        root.setPadding(new Insets(60));
        root.setStyle("-fx-background-color: linear-gradient(to bottom, #0B1A24, #1C2F3C);");

        Label title = new Label("ONLINE");
        title.setFont(Font.font("Impact", FontWeight.BOLD, 92));
        title.setTextFill(Color.web("#FFE082"));

        Label message = new Label("Online mode is not available yet.\nCheck back in a future update.");
        message.setFont(Font.font("Consolas", 30));
        message.setTextFill(Color.web("#B3E5FC"));
        message.setTextAlignment(TextAlignment.CENTER);

        Button back = uiFactory.action("BACK TO HUB", 360, 90, 34, "#D32F2F", 22, () -> showMenu(stage));
        root.getChildren().addAll(title, message, back);

        Scene scene = new Scene(root, WIDTH, HEIGHT);
        setupKeyboardNavigation(scene);
        applyConsoleHighlight(scene);
        setScenePreservingFullscreen(stage, scene);
        back.requestFocus();
    }

    private static class ShopItem {
        final String name;
        final String description;
        final int cost;
        final Runnable purchase;
        final BooleanSupplier unlocked;
        ShopItem(String name, String description, int cost, Runnable purchase, BooleanSupplier unlocked) {
            this.name = name;
            this.description = description;
            this.cost = cost;
            this.purchase = purchase;
            this.unlocked = unlocked;
        }
    }

    private List<ShopItem> buildShopItems() {
        List<ShopItem> items = new ArrayList<>();
        items.add(new ShopItem("City Pigeon Skin", "Gold city plumage.", 600,
                () -> cityPigeonUnlocked = true, () -> cityPigeonUnlocked));
        items.add(new ShopItem("Noir Pigeon Skin", "Shadow noir styling.", 900,
                () -> noirPigeonUnlocked = true, () -> noirPigeonUnlocked));
        items.add(new ShopItem("Sky King Eagle Skin", "Royal sky-gold armor.", 1200,
                () -> eagleSkinUnlocked = true, () -> eagleSkinUnlocked));

        for (BirdType type : BirdType.values()) {
            if (type == BirdType.PIGEON || type == BirdType.EAGLE) continue;
            String label = classicRewardFor(type) + " (Classic Skin)";
            items.add(new ShopItem(label, "Classic palette for " + type.name + ".", 1000,
                    () -> classicSkinUnlocked[type.ordinal()] = true,
                    () -> classicSkinUnlocked[type.ordinal()]));
        }
        return items;
    }

    private void showShop(Stage stage) {
        playMenuMusic();
        BorderPane root = new BorderPane();
        root.setPadding(new Insets(26, 40, 26, 40));
        root.setStyle("-fx-background-color: linear-gradient(to bottom, #0B1D2B, #1A2E3F);");

        Label title = new Label("SHOP");
        title.setFont(Font.font("Impact", FontWeight.BOLD, 84));
        title.setTextFill(Color.web("#FFE082"));

        Label coins = new Label("BIRD COINS: " + birdCoins);
        coins.setFont(Font.font("Consolas", 26));
        coins.setTextFill(Color.web("#FFD54F"));

        BorderPane top = new BorderPane();
        top.setCenter(title);
        top.setRight(coins);

        VBox list = new VBox(18);
        list.setAlignment(Pos.TOP_CENTER);

        for (ShopItem item : buildShopItems()) {
            boolean owned = item.unlocked.getAsBoolean();
            VBox card = new VBox(8);
            card.setAlignment(Pos.CENTER_LEFT);
            card.setPadding(new Insets(18));
            card.setMaxWidth(1200);
            card.setStyle("-fx-background-color: rgba(0,0,0,0.55); -fx-border-color: #90A4AE; -fx-border-width: 2; -fx-border-radius: 16; -fx-background-radius: 16;");

            Label name = new Label(item.name);
            name.setFont(Font.font("Impact", 30));
            name.setTextFill(Color.web("#ECEFF1"));

            Label desc = new Label(item.description);
            desc.setFont(Font.font("Consolas", 20));
            desc.setTextFill(Color.web("#B0BEC5"));

            Label price = new Label(owned ? "OWNED" : ("COST: " + item.cost));
            price.setFont(Font.font("Consolas", 20));
            price.setTextFill(owned ? Color.LIMEGREEN : Color.web("#FFD54F"));

            Button buy = uiFactory.action(owned ? "OWNED" : "BUY", 220, 60, 28, owned ? "#455A64" : "#00C853", 20, () -> {
                if (item.unlocked.getAsBoolean()) return;
                if (birdCoins < item.cost) {
                    playErrorSound();
                    return;
                }
                birdCoins -= item.cost;
                item.purchase.run();
                saveAchievements();
                showShop(stage);
            });
            buy.setDisable(owned);

            HBox row = new HBox(20, buy, price);
            row.setAlignment(Pos.CENTER_LEFT);

            card.getChildren().addAll(name, desc, row);
            list.getChildren().add(card);
        }

        ScrollPane scroll = new ScrollPane(list);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background-color: transparent;");
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);

        Button back = uiFactory.action("BACK TO HUB", 360, 90, 34, "#D32F2F", 22, () -> showMenu(stage));
        HBox bottom = new HBox(back);
        bottom.setAlignment(Pos.CENTER);

        root.setTop(top);
        root.setCenter(scroll);
        root.setBottom(bottom);

        Scene scene = new Scene(root, WIDTH, HEIGHT);
        setupKeyboardNavigation(scene);
        applyConsoleHighlight(scene);
        setScenePreservingFullscreen(stage, scene);
        back.requestFocus();
    }

    private void showEpisodesHub(Stage stage) {
        storyModeActive = true;
        storyReplayMode = false;
        adventureModeActive = false;
        adventureReplayMode = false;
        currentAdventureBattle = null;
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
        VBox pelicanCard = buildEpisodeCard(stage, EpisodeType.PELICAN, "#FFD54F", "Command the Iron Beak in scripted chaos battles. Defeat Vulture to unlock Bat.");
        VBox cards = new VBox(18, pigeonCard, batCard, pelicanCard);
        cards.setAlignment(Pos.TOP_CENTER);

        ScrollPane scroll = new ScrollPane(cards);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background: transparent; -fx-background-color: transparent; -fx-control-inner-background: transparent;");
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

    private void showAdventureHub(Stage stage) {
        adventureModeActive = true;
        adventureReplayMode = false;
        adventureChapterIndex = 0;
        currentAdventureBattle = null;
        storyModeActive = false;
        storyReplayMode = false;
        classicModeActive = false;
        classicEncounter = null;
        classicRun.clear();
        classicRoundIndex = 0;
        classicTeamMode = false;
        Arrays.fill(classicTeams, 1);
        activeBossChallenge = BossChallengeType.NONE;
        competitionSeriesActive = false;
        competitionDraftComplete = false;
        competitionDraftSummary = "";
        competitionBannedBirds.clear();
        Arrays.fill(competitionRoundWins, 0);
        Arrays.fill(competitionTeamWins, 0);
        competitionRoundNumber = 1;
        playMenuMusic();

        AdventureChapter chapter = activeAdventureChapter();
        if (chapter == null) {
            showMenu(stage);
            return;
        }
        adventureChapterProgress = Math.max(0, Math.min(adventureChapterProgress, chapter.battles.length));
        if (adventureChapterCompleted) {
            adventureChapterProgress = chapter.battles.length;
        }

        VBox root = new VBox(26);
        root.setAlignment(Pos.CENTER);
        root.setPadding(new Insets(50));
        root.setStyle("-fx-background-color: linear-gradient(to bottom, #091426, #1A2B4B);");

        Label title = new Label("ADVENTURE MODE");
        title.setFont(Font.font("Arial Black", FontWeight.BOLD, 90));
        title.setTextFill(Color.web("#FFD54F"));

        VBox card = new VBox(16);
        card.setAlignment(Pos.CENTER_LEFT);
        card.setPadding(new Insets(26));
        card.setMaxWidth(1400);
        card.setStyle("-fx-background-color: rgba(0,0,0,0.6); -fx-background-radius: 24; -fx-border-color: #64B5F6; -fx-border-width: 3; -fx-border-radius: 24;");

        Label chapterTitle = new Label(chapter.title);
        chapterTitle.setFont(Font.font("Arial Black", 48));
        chapterTitle.setTextFill(Color.WHITE);

        String statusText = adventureChapterCompleted
                ? "Completed"
                : ("Battles cleared: " + adventureChapterProgress + "/" + chapter.battles.length);
        Label status = new Label(statusText);
        status.setFont(Font.font("Consolas", 30));
        status.setTextFill(adventureChapterCompleted ? Color.LIMEGREEN : Color.ORANGE);

        Label summary = new Label(chapter.summary);
        summary.setFont(Font.font("Consolas", 24));
        summary.setTextFill(Color.web("#B3E5FC"));
        summary.setWrapText(true);

        Label roster = new Label("Adventure Roster: " + adventureRosterText());
        roster.setFont(Font.font("Consolas", 24));
        roster.setTextFill(Color.web("#C5CAE9"));

        HBox actions = new HBox(18);
        actions.setAlignment(Pos.CENTER_LEFT);

        String continueText = adventureChapterProgress == 0 ? "START CHAPTER"
                : (adventureChapterCompleted ? "REPLAY CHAPTER" : "CONTINUE CHAPTER");
        Button continueBtn = uiFactory.action(continueText, 420, 96, 32, "#00C853", 22, () -> {
            playButtonClick();
            resetMatchStats();
            AdventureChapter active = activeAdventureChapter();
            if (active == null) {
                showMenu(stage);
                return;
            }
            adventureReplayMode = false;
            adventureBattleIndex = Math.max(0, Math.min(adventureChapterProgress, active.battles.length - 1));
            if (adventureChapterProgress == 0) {
                showAdventureChapterIntro(stage);
            } else {
                showAdventureBattleIntro(stage);
            }
        });

        Button selectBtn = uiFactory.action("BATTLE SELECT", 360, 96, 28, "#1976D2", 22, () -> {
            playButtonClick();
            showAdventureBattleSelect(stage);
        });

        actions.getChildren().addAll(continueBtn, selectBtn);
        card.getChildren().addAll(chapterTitle, status, summary, roster, actions);

        Button menuBtn = uiFactory.action("MAIN MENU", 420, 100, 34, "#FF1744", 24, () -> showMenu(stage));

        root.getChildren().addAll(title, card, menuBtn);

        Scene scene = new Scene(root, WIDTH, HEIGHT);
        setupKeyboardNavigation(scene);
        applyConsoleHighlight(scene);
        setScenePreservingFullscreen(stage, scene);
        continueBtn.requestFocus();
    }

    private void showAdventureBattleSelect(Stage stage) {
        playMenuMusic();
        AdventureChapter chapter = activeAdventureChapter();
        if (chapter == null) {
            showMenu(stage);
            return;
        }

        int unlocked = Math.max(1, Math.min(adventureChapterProgress + 1, chapter.battles.length));
        VBox root = new VBox(24);
        root.setAlignment(Pos.CENTER);
        root.setPadding(new Insets(50));
        root.setStyle("-fx-background-color: linear-gradient(to bottom, #0A1530, #1B2C52);");

        Label title = new Label(chapter.title + " - BATTLES");
        title.setFont(Font.font("Arial Black", FontWeight.BOLD, 84));
        title.setTextFill(Color.web("#4FC3F7"));

        VBox list = new VBox(16);
        list.setAlignment(Pos.CENTER);

        for (int i = 0; i < chapter.battles.length; i++) {
            AdventureBattle battle = chapter.battles[i];
            boolean isUnlocked = i < unlocked;
            String label = (i + 1) + ". " + battle.title + " - " + battle.opponentName;
            Button battleBtn = new Button(isUnlocked ? label : ((i + 1) + ". LOCKED"));
            battleBtn.setPrefSize(1500, 96);
            battleBtn.setFont(Font.font("Arial Black", 28));
            battleBtn.setStyle(isUnlocked
                    ? "-fx-background-color: #263238; -fx-text-fill: white; -fx-background-radius: 18;"
                    : "-fx-background-color: #444444; -fx-text-fill: #BBBBBB; -fx-background-radius: 18;");
            battleBtn.setDisable(!isUnlocked);

            if (isUnlocked) {
                int battleIndex = i;
                battleBtn.setOnAction(e -> {
                    playButtonClick();
                    resetMatchStats();
                    adventureReplayMode = battleIndex < adventureChapterProgress;
                    adventureBattleIndex = battleIndex;
                    showAdventureBattleIntro(stage);
                });
            }
            list.getChildren().add(battleBtn);
        }

        Button back = uiFactory.action("BACK TO ADVENTURE", 700, 105, 42, "#FF1744", 22, () -> showAdventureHub(stage));

        root.getChildren().addAll(title, list, back);
        Scene scene = new Scene(root, WIDTH, HEIGHT);
        setupKeyboardNavigation(scene);
        applyConsoleHighlight(scene);
        setScenePreservingFullscreen(stage, scene);
        back.requestFocus();
    }

    private void showAdventureChapterIntro(Stage stage) {
        AdventureChapter chapter = activeAdventureChapter();
        if (chapter == null) {
            showAdventureHub(stage);
            return;
        }
        showAdventureDialogue(stage, chapter.title, chapter.introDialogue, () -> showAdventureBattleIntro(stage));
    }

    private void showAdventureBattleIntro(Stage stage) {
        AdventureBattle battle = activeAdventureBattle();
        if (battle == null) {
            showAdventureHub(stage);
            return;
        }
        currentAdventureBattle = battle;
        showAdventureDialogue(stage, battle.title, battle.preDialogue, () -> showAdventureBirdSelect(stage, battle));
    }

    private void showAdventureBirdSelect(Stage stage, AdventureBattle battle) {
        playMenuMusic();
        if (battle == null) {
            showAdventureHub(stage);
            return;
        }

        List<BirdType> allowed = adventureAllowedBirds(battle);
        BirdType initial = allowed.get(0);
        if (allowed.contains(adventureSelectedBird)) {
            initial = adventureSelectedBird;
        }
        final BirdType[] selected = new BirdType[]{initial};
        if (battle.cpuLevel < 1 || battle.cpuLevel > 9) battle.cpuLevel = 5;
        final int[] cpuLevel = new int[]{battle.cpuLevel};
        String initialSkin = normalizeAdventureSkinChoice(initial, adventureSelectedSkinKey);
        final String[] selectedSkin = new String[]{initialSkin};

        BorderPane root = new BorderPane();
        root.setPadding(new Insets(30, 40, 30, 40));
        root.setStyle("-fx-background-color: linear-gradient(to bottom, #07111F, #16263F);");

        Label title = new Label("ADVENTURE - " + battle.title);
        title.setFont(Font.font("Arial Black", FontWeight.BOLD, 72));
        title.setTextFill(Color.web("#FFD54F"));
        BorderPane.setAlignment(title, Pos.CENTER);

        Label info = new Label(battle.briefing + "\nMap: " + battle.map.name() + "\nOpponent: " + battle.opponentName);
        info.setFont(Font.font("Consolas", 24));
        info.setTextFill(Color.web("#B3E5FC"));
        info.setWrapText(true);

        String allowedText;
        if (battle.requiredPlayerType != null) {
            allowedText = "Required bird: " + battle.requiredPlayerType.name;
        } else if (battle.allowedBirds != null) {
            allowedText = "Allowed birds: " + battle.allowedBirds.stream().map(b -> b.name).reduce((a, b) -> a + ", " + b).orElse("Pigeon");
        } else {
            allowedText = "Allowed birds: Any unlocked";
        }

        String rewardText = battle.unlockReward != null ? ("Unlock reward: " + battle.unlockReward.name) : "Reward: Story progress";
        Label rules = new Label(allowedText + "\n" + rewardText + "\nAdventure roster: " + adventureRosterText());
        rules.setFont(Font.font("Consolas", 22));
        rules.setTextFill(Color.web("#E0E0E0"));

        Button pickBtn = uiFactory.action("BIRD: " + selected[0].name, 820, 96, 34, "#263238", 24, () -> {});
        Button skinBtn = uiFactory.action(adventureSkinLabel(selected[0], selectedSkin[0]), 520, 90, 30, "#37474F", 22, () -> {});
        Button cpuBtn = uiFactory.action("CPU LEVEL: " + cpuLevel[0], 320, 90, 30, "#455A64", 22, () -> {});
        Runnable refreshSkin = () -> {
            List<String> options = adventureSkinOptions(selected[0]);
            if (!options.contains(selectedSkin[0])) {
                selectedSkin[0] = options.get(0);
            }
            skinBtn.setText(adventureSkinLabel(selected[0], selectedSkin[0]));
            boolean hasAlt = options.size() > 1;
            skinBtn.setDisable(!hasAlt);
            skinBtn.setOpacity(hasAlt ? 1.0 : 0.7);
        };
        pickBtn.setOnAction(e -> {
            playButtonClick();
            int idx = allowed.indexOf(selected[0]);
            if (idx < 0) idx = 0;
            selected[0] = allowed.get((idx + 1) % allowed.size());
            pickBtn.setText("BIRD: " + selected[0].name);
            refreshSkin.run();
        });
        if (battle.requiredPlayerType != null || allowed.size() <= 1) {
            pickBtn.setDisable(true);
            pickBtn.setOpacity(0.85);
        }

        skinBtn.setOnAction(e -> {
            playButtonClick();
            List<String> options = adventureSkinOptions(selected[0]);
            if (options.size() <= 1) return;
            int idx = options.indexOf(selectedSkin[0]);
            if (idx < 0) idx = 0;
            selectedSkin[0] = options.get((idx + 1) % options.size());
            skinBtn.setText(adventureSkinLabel(selected[0], selectedSkin[0]));
        });

        cpuBtn.setOnAction(e -> {
            playButtonClick();
            cpuLevel[0] = cpuLevel[0] % 9 + 1;
            battle.cpuLevel = cpuLevel[0];
            cpuBtn.setText("CPU LEVEL: " + cpuLevel[0]);
        });

        refreshSkin.run();
        HBox optionRow = new HBox(16, skinBtn, cpuBtn);
        optionRow.setAlignment(Pos.CENTER);

        VBox center = new VBox(18, info, rules, pickBtn, optionRow);
        center.setAlignment(Pos.CENTER);

        Button back = uiFactory.action("BACK", 320, 96, 34, "#546E7A", 22, () -> showAdventureBattleSelect(stage));
        Button start = uiFactory.action("START BATTLE", 420, 96, 34, "#00C853", 24, () -> {
            playButtonClick();
            adventureSelectedBird = selected[0];
            adventureSelectedSkinKey = selectedSkin[0];
            battle.cpuLevel = cpuLevel[0];
            startAdventureBattle(stage);
        });
        HBox bottom = new HBox(18, back, start);
        bottom.setAlignment(Pos.CENTER);

        root.setTop(title);
        root.setCenter(center);
        root.setBottom(bottom);

        Scene scene = new Scene(root, WIDTH, HEIGHT);
        setupKeyboardNavigation(scene);
        applyConsoleHighlight(scene);
        setScenePreservingFullscreen(stage, scene);
        start.requestFocus();
    }

    private void startAdventureBattle(Stage stage) {
        AdventureBattle battle = activeAdventureBattle();
        if (battle == null) {
            showAdventureHub(stage);
            return;
        }

        resetMatchStats();
        adventureModeActive = true;
        storyModeActive = false;
        storyReplayMode = false;
        classicModeActive = false;
        classicEncounter = null;
        classicRun.clear();
        classicRoundIndex = 0;
        classicTeamMode = false;
        Arrays.fill(classicTeams, 1);
        activeBossChallenge = BossChallengeType.NONE;
        competitionSeriesActive = false;
        competitionDraftComplete = false;
        competitionDraftSummary = "";
        competitionBannedBirds.clear();
        Arrays.fill(competitionRoundWins, 0);
        Arrays.fill(competitionTeamWins, 0);
        competitionRoundNumber = 1;
        teamModeEnabled = false;
        competitionModeEnabled = false;
        mutatorModeEnabled = false;
        isTrialMode = false;
        currentAdventureBattle = battle;
        selectedMap = battle.map;
        startMatch(stage);
    }

    private void setupAdventureBattleRoster(AdventureBattle battle) {
        if (battle == null) return;
        activePlayers = 2;
        BirdType playerType = adventureSelectedBird;
        if (battle.requiredPlayerType != null) {
            playerType = battle.requiredPlayerType;
        }
        if (!isAdventureBirdUnlocked(playerType)) {
            playerType = BirdType.PIGEON;
        }
        Bird player = createStoryBird(1200, playerType, 0, "You: " + playerType.name, 100, 1.0, 1.0, false);
        String skinKey = normalizeAdventureSkinChoice(playerType, adventureSelectedSkinKey);
        adventureSelectedSkinKey = skinKey;
        applySkinChoiceToBird(player, playerType, skinKey);
        createStoryBird(4200, battle.opponentType, 1, battle.opponentName,
                battle.opponentHealth, battle.opponentPowerMult, battle.opponentSpeedMult, true);
    }

    private void applyAdventureBattleArenaModifiers(AdventureBattle battle) {
        if (battle == null) return;
        if (adventureChapterIndex != 0) return;
        switch (adventureBattleIndex) {
            case 0 -> {
                addToKillFeed("ROOFTOP SPRINT: Neon signals boost speed.");
                powerUps.add(new PowerUp(1200, GROUND_Y - 760, PowerUpType.NEON));
                powerUps.add(new PowerUp(3200, GROUND_Y - 820, PowerUpType.SPEED));
            }
            case 1 -> {
                addToKillFeed("OLD FRIEND TRIAL: Thermal vents surge on the cliffs.");
                windVents.add(new WindVent(900, GROUND_Y - 340, 500));
                windVents.add(new WindVent(2800, GROUND_Y - 360, 520));
                powerUps.add(new PowerUp(2600, GROUND_Y - 980, PowerUpType.THERMAL));
            }
            case 2 -> {
                addToKillFeed("CARRION SCOUT: Crow eyes watch the fight.");
                for (int i = 0; i < 4; i++) {
                    double cx = 900 + random.nextDouble() * 4200;
                    double cy = 260 + random.nextDouble() * 420;
                    crowMinions.add(new CrowMinion(cx, cy, players[0]));
                }
                powerUps.add(new PowerUp(3000, GROUND_Y - 900, PowerUpType.RAGE));
                powerUps.add(new PowerUp(3600, GROUND_Y - 760, PowerUpType.HEALTH));
            }
        }
    }

    private void handleAdventureMatchEnd(Stage stage, Bird winner) {
        AdventureChapter chapter = activeAdventureChapter();
        AdventureBattle battle = activeAdventureBattle();
        if (chapter == null || battle == null) {
            showAdventureHub(stage);
            return;
        }

        boolean playerWon;
        if (adventureTeamMode) {
            int playerTeam = getEffectiveTeam(0);
            playerWon = winner != null && getEffectiveTeam(winner.playerIndex) == playerTeam;
        } else {
            playerWon = winner != null && winner.playerIndex == 0;
        }

        if (!playerWon) {
            showAdventureDialogue(stage,
                    "Defeat",
                    battle.loseDialogue,
                    () -> showAdventureBirdSelect(stage, battle));
            return;
        }

        if (battle.unlockReward != null && !isAdventureBirdUnlocked(battle.unlockReward)) {
            adventureUnlocked[battle.unlockReward.ordinal()] = true;
            saveAchievements();
        }

        if (!adventureReplayMode && adventureBattleIndex >= adventureChapterProgress) {
            adventureChapterProgress = Math.min(adventureBattleIndex + 1, chapter.battles.length);
            if (adventureChapterProgress >= chapter.battles.length) {
                adventureChapterCompleted = true;
            }
            saveAchievements();
        }

        Runnable afterWin = () -> {
            if (adventureReplayMode) {
                showAdventureBattleSelect(stage);
                return;
            }
            if (adventureChapterCompleted && adventureBattleIndex >= chapter.battles.length - 1) {
                showAdventureDialogue(stage, "Chapter Complete", chapter.outroDialogue, () -> showAdventureHub(stage));
                return;
            }
            adventureBattleIndex++;
            showAdventureBattleIntro(stage);
        };

        showAdventureDialogue(stage, "Victory", battle.winDialogue, afterWin);
    }

    private void showAdventureDialogue(Stage stage, String titleText, AdventureDialogueLine[] lines, Runnable onComplete) {
        playMenuMusic();
        if (lines == null || lines.length == 0) {
            if (onComplete != null) onComplete.run();
            return;
        }

        Canvas canvas = new Canvas(WIDTH, HEIGHT);
        GraphicsContext g = canvas.getGraphicsContext2D();
        final int[] idx = {0};

        Runnable render = () -> renderAdventureDialogueFrame(g, titleText, lines[idx[0]]);
        render.run();

        StackPane root = new StackPane(canvas);
        Scene scene = new Scene(root, WIDTH, HEIGHT);
        setupKeyboardNavigation(scene);
        applyConsoleHighlight(scene);
        setScenePreservingFullscreen(stage, scene);

        Runnable advance = () -> {
            idx[0]++;
            if (idx[0] >= lines.length) {
                if (onComplete != null) onComplete.run();
            } else {
                render.run();
            }
        };

        root.setOnMouseClicked(e -> {
            playButtonClick();
            advance.run();
        });
        scene.addEventFilter(KeyEvent.KEY_PRESSED, e -> {
            if (e.getCode() == KeyCode.SPACE || e.getCode() == KeyCode.ENTER) {
                playButtonClick();
                advance.run();
                e.consume();
            }
        });
    }

    private void renderAdventureDialogueFrame(GraphicsContext g, String titleText, AdventureDialogueLine line) {
        g.clearRect(0, 0, WIDTH, HEIGHT);
        Stop[] stops = new Stop[] {
                new Stop(0, Color.web("#071122")),
                new Stop(1, Color.web("#1D2C4C"))
        };
        LinearGradient bg = new LinearGradient(0, 0, 0, 1, true, CycleMethod.NO_CYCLE, stops);
        g.setFill(bg);
        g.fillRect(0, 0, WIDTH, HEIGHT);

        g.setFill(Color.web("#FFD54F"));
        g.setFont(Font.font("Arial Black", 64));
        g.fillText(titleText, 80, 90);

        double groundY = HEIGHT - 220;
        g.setFill(Color.rgb(14, 18, 28));
        g.fillRect(0, groundY, WIDTH, HEIGHT - groundY);
        g.setStroke(Color.rgb(70, 80, 100, 0.7));
        g.setLineWidth(4);
        g.strokeLine(0, groundY, WIDTH, groundY);

        Bird left = null;
        Bird right = null;
        double leftX = 260;
        double rightX = WIDTH - 360;
        if (line.leftBird != null) {
            left = createDialogueBird(line.leftBird, leftX, groundY, true);
            left.draw(g);
        }
        if (line.rightBird != null) {
            right = createDialogueBird(line.rightBird, rightX, groundY, false);
            right.draw(g);
        }

        Bird speaker = line.speakerSide == DialogueSide.LEFT ? left : right;
        if (speaker == null) speaker = left != null ? left : right;

        Font nameFont = Font.font("Arial Black", 28);
        Font textFont = Font.font("Consolas", 26);
        double padding = 26;
        double bubbleMaxWidth = 980;

        List<String> wrapped = wrapTextToLines(line.text, textFont, bubbleMaxWidth - padding * 2);
        double lineH = measureTextHeight("Ag", textFont) * 1.18;
        double nameH = measureTextHeight("Ag", nameFont);
        double bubbleW = Math.min(bubbleMaxWidth, Math.max(420, maxLineWidth(wrapped, textFont) + padding * 2));
        double bubbleH = padding * 2 + nameH + (wrapped.isEmpty() ? 0 : 10) + lineH * wrapped.size();
        double bubbleX = line.speakerSide == DialogueSide.LEFT ? 120 : WIDTH - bubbleW - 120;
        double bubbleY = 140;

        double headX = speaker != null ? speaker.x + 40 * speaker.sizeMultiplier : WIDTH / 2.0;
        double headY = speaker != null ? speaker.y + 20 * speaker.sizeMultiplier : groundY - 60;
        drawSpeechBubble(g, bubbleX, bubbleY, bubbleW, bubbleH, headX, headY);

        g.setFill(Color.web("#111111"));
        g.setFont(nameFont);
        g.fillText(line.speakerName, bubbleX + padding, bubbleY + padding + nameH);

        g.setFont(textFont);
        double textY = bubbleY + padding + nameH + 10 + lineH;
        for (String s : wrapped) {
            g.fillText(s, bubbleX + padding, textY);
            textY += lineH;
        }

        g.setFill(Color.web("#C5CAE9"));
        g.setFont(Font.font("Consolas", 22));
        g.fillText("Click to continue", WIDTH - 260, HEIGHT - 36);
    }

    private void drawSpeechBubble(GraphicsContext g, double x, double y, double w, double h, double tailX, double tailY) {
        g.setFill(Color.rgb(250, 250, 250, 0.95));
        g.setStroke(Color.rgb(10, 10, 10, 0.9));
        g.setLineWidth(3);
        g.fillRoundRect(x, y, w, h, 26, 26);
        g.strokeRoundRect(x, y, w, h, 26, 26);

        double tailBaseX = Math.min(Math.max(tailX, x + 40), x + w - 40);
        double tailBaseY = y + h;
        double[] xs = new double[]{tailBaseX - 18, tailBaseX + 18, tailX};
        double[] ys = new double[]{tailBaseY, tailBaseY, tailY};
        g.fillPolygon(xs, ys, 3);
        g.strokePolygon(xs, ys, 3);
    }

    private Bird createDialogueBird(BirdType type, double x, double groundY, boolean facingRight) {
        Bird b = new Bird(x, type, 0, this);
        double scale = 1.25;
        b.setBaseMultipliers(b.baseSizeMultiplier * scale, b.basePowerMultiplier, b.baseSpeedMultiplier);
        b.x = x;
        double drawSize = 80 * b.sizeMultiplier;
        b.y = groundY - drawSize;
        b.vx = 0;
        b.vy = 0;
        b.facingRight = facingRight;
        return b;
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
        Runnable backAction = () -> {
            Runnable target = settingsReturn;
            settingsReturn = null;
            if (target != null) {
                target.run();
            } else {
                showMenu(stage);
            }
        };
        Button back = uiFactory.action("BACK", 360, 100, 40, "#FF1744", 28, backAction);
        Button stageSelect = uiFactory.action("GO TO STAGE SELECT", 520, 100, 34, "#1565C0", 24, () -> {
            stageSelectReturn = () -> showGameSettings(stage);
            showStageSelect(stage);
        });
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
            case FALCON -> "Crimson Falcon";
            case PHOENIX -> "Solar Phoenix";
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
            case FALCON -> Color.web("#E64A19");
            case PHOENIX -> Color.web("#FF7043");
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
            case FALCON -> Color.web("#FFE082");
            case PHOENIX -> Color.web("#FFE082");
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

        int roundOneVariant = random.nextInt(3);
        if (roundOneVariant == 0) {
            BirdType r1 = pickClassicEnemy(playerType, usedBirds);
            run.add(new ClassicEncounter(
                    "Qualifier: First Impact",
                    "Skycaster",
                    "One rival enters. Win clean and establish tempo.",
                    pickClassicMap(usedMaps),
                    pickClassicMutator(MatchMutator.NONE, MatchMutator.LOW_GRAVITY),
                    pickClassicTwist(ClassicTwist.STORM_LIFTS, ClassicTwist.NECTAR_BLOOM, ClassicTwist.WIND_RALLY),
                    85 * 60,
                    new ClassicFighter[0],
                    new ClassicFighter[]{
                            classicFighter(r1, "Rival: " + r1.name, 120, 1.08, 1.03)
                    },
                    false
            ));
        } else if (roundOneVariant == 1) {
            BirdType r1a = pickClassicEnemy(playerType, usedBirds);
            BirdType r1b = pickClassicEnemy(playerType, usedBirds);
            run.add(new ClassicEncounter(
                    "Skirmish: Twin Talons",
                    "Skycaster",
                    "Two lighter foes push early pace. Win clean and stay composed.",
                    pickClassicMap(usedMaps),
                    pickClassicMutator(MatchMutator.NONE, MatchMutator.LOW_GRAVITY),
                    pickClassicTwist(ClassicTwist.NECTAR_BLOOM, ClassicTwist.SHADOW_CACHE),
                    80 * 60,
                    new ClassicFighter[0],
                    new ClassicFighter[]{
                            classicFighter(r1a, "Dart Wing: " + r1a.name, 84, 0.9, 1.06),
                            classicFighter(r1b, "Dart Wing: " + r1b.name, 86, 0.92, 1.05)
                    },
                    false
            ));
        } else {
            BirdType r1 = pickClassicEnemy(playerType, usedBirds);
            run.add(new ClassicEncounter(
                    "Trial: Hazard Pulse",
                    "Arena Warden",
                    "A tuned rival with hazards online early. Survive the opening storm.",
                    pickClassicMap(usedMaps),
                    pickClassicMutator(MatchMutator.POWERUP_STORM, MatchMutator.CROW_SURGE),
                    pickClassicTwist(ClassicTwist.SHOCK_DROPS, ClassicTwist.TITAN_CACHE),
                    88 * 60,
                    new ClassicFighter[0],
                    new ClassicFighter[]{
                            classicFighter(r1, "Charger: " + r1.name, 132, 1.12, 1.02)
                    },
                    false
            ));
        }

        int roundTwoVariant = random.nextInt(3);
        if (roundTwoVariant == 0) {
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
                    },
                    false
            ));
        } else if (roundTwoVariant == 1) {
            BirdType ally = pickClassicEnemy(playerType, usedBirds);
            BirdType enemy = pickClassicEnemy(playerType, usedBirds);
            run.add(new ClassicEncounter(
                    "Wingmate: Lockstep Clash",
                    "Command Relay",
                    "You gain an ally to break a stronger rival. Stay together and trade aggro.",
                    pickClassicMap(usedMaps),
                    pickClassicMutator(MatchMutator.TURBO_BRAWL, MatchMutator.POWERUP_STORM),
                    pickClassicTwist(ClassicTwist.RAGE_RITUAL, ClassicTwist.NECTAR_BLOOM),
                    92 * 60,
                    new ClassicFighter[]{
                            classicFighter(ally, "Ally: " + ally.name, 104, 1.0, 1.06)
                    },
                    new ClassicFighter[]{
                            classicFighter(enemy, "Vanguard: " + enemy.name, 160, 1.18, 1.06)
                    },
                    false
            ));
        } else {
            BirdType ally = pickClassicEnemy(playerType, usedBirds);
            BirdType e1 = pickClassicEnemy(playerType, usedBirds);
            BirdType e2 = pickClassicEnemy(playerType, usedBirds);
            run.add(new ClassicEncounter(
                    "Gridlock: Two-on-Two",
                    "Command Relay",
                    "Pair up and control space. Both sides bring balanced threats.",
                    pickClassicMap(usedMaps),
                    pickClassicMutator(MatchMutator.LOW_GRAVITY, MatchMutator.TURBO_BRAWL),
                    pickClassicTwist(ClassicTwist.WIND_RALLY, ClassicTwist.SHADOW_CACHE),
                    98 * 60,
                    new ClassicFighter[]{
                            classicFighter(ally, "Ally: " + ally.name, 110, 1.02, 1.05)
                    },
                    new ClassicFighter[]{
                            classicFighter(e1, "Twin: " + e1.name, 118, 1.05, 1.03),
                            classicFighter(e2, "Twin: " + e2.name, 118, 1.05, 1.03)
                    },
                    false
            ));
        }

        int roundThreeVariant = random.nextInt(3);
        if (roundThreeVariant == 0) {
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
                    },
                    false
            ));
        } else if (roundThreeVariant == 1) {
            BirdType r3a = pickClassicEnemy(playerType, usedBirds);
            BirdType r3b = pickClassicEnemy(playerType, usedBirds);
            BirdType r3c = pickClassicEnemy(playerType, usedBirds);
            run.add(new ClassicEncounter(
                    "Rush: Lone Pack",
                    "Ruin Oracle",
                    "Three lighter enemies hunt together. Control space before they surround you.",
                    pickClassicMap(usedMaps),
                    pickClassicMutator(MatchMutator.CROW_SURGE, MatchMutator.LOW_GRAVITY),
                    pickClassicTwist(ClassicTwist.CROW_CARNIVAL, ClassicTwist.SHADOW_CACHE),
                    88 * 60,
                    new ClassicFighter[0],
                    new ClassicFighter[]{
                            classicFighter(r3a, "Pack Wing: " + r3a.name, 86, 0.9, 1.05),
                            classicFighter(r3b, "Pack Wing: " + r3b.name, 84, 0.9, 1.05),
                            classicFighter(r3c, "Pack Wing: " + r3c.name, 82, 0.9, 1.06)
                    },
                    false
            ));
        } else {
            BirdType ally = pickClassicEnemy(playerType, usedBirds);
            BirdType r3a = pickClassicEnemy(playerType, usedBirds);
            BirdType r3b = pickClassicEnemy(playerType, usedBirds);
            run.add(new ClassicEncounter(
                    "Lockdown: Control Lanes",
                    "Arena Warden",
                    "Two elite enemies hold territory. Work with your ally to dislodge them.",
                    pickClassicMap(usedMaps),
                    pickClassicMutator(MatchMutator.POWERUP_STORM, MatchMutator.TURBO_BRAWL),
                    pickClassicTwist(ClassicTwist.RAGE_RITUAL, ClassicTwist.TITAN_CACHE),
                    96 * 60,
                    new ClassicFighter[]{
                            classicFighter(ally, "Ally: " + ally.name, 112, 1.04, 1.06)
                    },
                    new ClassicFighter[]{
                            classicFighter(r3a, "Warden: " + r3a.name, 142, 1.16, 1.05),
                            classicFighter(r3b, "Warden: " + r3b.name, 138, 1.14, 1.05)
                    },
                    false
            ));
        }

        int roundFourVariant = random.nextInt(3);
        if (roundFourVariant == 0) {
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
                    },
                    false
            ));
        } else if (roundFourVariant == 1) {
            BirdType ally = pickClassicEnemy(playerType, usedBirds);
            BirdType e1 = pickClassicEnemy(playerType, usedBirds);
            BirdType e2 = pickClassicEnemy(playerType, usedBirds);
            run.add(new ClassicEncounter(
                    "Crossfire: Double Clash",
                    "Command Relay",
                    "Both sides field duos. Focus targets together to avoid staggered defeats.",
                    pickClassicMap(usedMaps),
                    pickClassicMutator(MatchMutator.LOW_GRAVITY, MatchMutator.CROW_SURGE),
                    pickClassicTwist(ClassicTwist.WIND_RALLY, ClassicTwist.STORM_LIFTS),
                    95 * 60,
                    new ClassicFighter[]{
                            classicFighter(ally, "Ally: " + ally.name, 114, 1.04, 1.08)
                    },
                    new ClassicFighter[]{
                            classicFighter(e1, "Duelist: " + e1.name, 136, 1.12, 1.06),
                            classicFighter(e2, "Duelist: " + e2.name, 132, 1.1, 1.06)
                    },
                    false
            ));
        } else {
            BirdType miniBoss = pickClassicEnemy(playerType, usedBirds);
            BirdType support = pickClassicEnemy(playerType, usedBirds);
            run.add(new ClassicEncounter(
                    "Breaker: Boss Escort",
                    "Skycaster Prime",
                    "A mini-boss enters with support. Control space and split them apart.",
                    pickClassicMap(usedMaps),
                    pickClassicMutator(MatchMutator.TURBO_BRAWL, MatchMutator.POWERUP_STORM),
                    pickClassicTwist(ClassicTwist.TITAN_CACHE, ClassicTwist.RAGE_RITUAL),
                    100 * 60,
                    new ClassicFighter[0],
                    new ClassicFighter[]{
                            classicFighter(miniBoss, "Boss: " + miniBoss.name, 180, 1.22, 1.1),
                            classicFighter(support, "Escort: " + support.name, 120, 1.05, 1.1)
                    },
                    true
            ));
        }

        BirdType boss = pickClassicEnemy(playerType, usedBirds);
        BirdType lieutenant = pickClassicEnemy(playerType, usedBirds);
        boolean allyFinale = random.nextBoolean();
        if (allyFinale) {
            BirdType r5ally = pickClassicEnemy(playerType, usedBirds);
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
                    },
                    true
            ));
        } else {
            BirdType elite = pickClassicEnemy(playerType, usedBirds);
            run.add(new ClassicEncounter(
                    "Finale: Storm Crown",
                    "Skycaster Prime",
                    "Final arena. The boss leads an elite wing. Break them to clear Classic for this bird.",
                    pickClassicMap(usedMaps),
                    pickClassicMutator(MatchMutator.TURBO_BRAWL, MatchMutator.CROW_SURGE, MatchMutator.POWERUP_STORM),
                    pickClassicTwist(ClassicTwist.TITAN_CACHE, ClassicTwist.WIND_RALLY, ClassicTwist.RAGE_RITUAL),
                    110 * 60,
                    new ClassicFighter[0],
                    new ClassicFighter[]{
                            classicFighter(boss, "Boss: " + boss.name, 220, 1.34, 1.1),
                            classicFighter(lieutenant, "Elite: " + lieutenant.name, 150, 1.18, 1.12),
                            classicFighter(elite, "Elite: " + elite.name, 140, 1.16, 1.12)
                    },
                    true
            ));
        }

        return run;
    }

    private void showClassicBirdSelect(Stage stage) {
        storyModeActive = false;
        storyReplayMode = false;
        adventureModeActive = false;
        adventureReplayMode = false;
        currentAdventureBattle = null;
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

        BirdType classicPick = isBirdUnlocked(classicSelectedBird) ? classicSelectedBird : firstUnlockedBird();
        final BirdType[] selected = new BirdType[]{classicPick};
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

        Label info = new Label("Clear full runs to unlock each bird's rewards.");
        info.setFont(Font.font("Consolas", 22));
        info.setTextFill(Color.web("#FFE082"));

        Runnable refreshSelection = () -> {
            BirdType picked = selected[0];
            selectedLabel.setText("Selected: " + picked.name);
            String reward = classicRewardFor(picked);
            String charReward = classicCharacterReward(picked);
            boolean unlocked = isClassicRewardUnlocked(picked);
            boolean completed = isClassicCompleted(picked);
            rewardLabel.setText("Reward Skin: " + reward + (charReward.isBlank() ? "" : "\nReward Character: " + charReward));
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
        if (!isBirdUnlocked(birdType)) {
            showClassicBirdSelect(stage);
            return;
        }
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
        String charReward = classicCharacterReward(classicSelectedBird);
        boolean unlocked = isClassicRewardUnlocked(classicSelectedBird);
        String rewardText = "Reward Skin: " + reward + (charReward.isBlank() ? "" : "\nReward Character: " + charReward);
        Label rewardInfo = new Label(rewardText + "  |  " + (unlocked ? "UNLOCKED" : "LOCKED"));
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

    private void showBossChallenges(Stage stage) {
        storyModeActive = false;
        storyReplayMode = false;
        adventureModeActive = false;
        adventureReplayMode = false;
        currentAdventureBattle = null;
        classicModeActive = false;
        classicEncounter = null;
        classicRun.clear();
        classicRoundIndex = 0;
        classicTeamMode = false;
        Arrays.fill(classicTeams, 1);
        playMenuMusic();

        VBox root = new VBox(24);
        root.setAlignment(Pos.CENTER);
        root.setPadding(new Insets(36));
        root.setStyle("-fx-background-color: linear-gradient(to bottom, #120B25, #1E1B4B);");

        Label title = new Label("BOSS CHALLENGES");
        title.setFont(Font.font("Arial Black", FontWeight.BOLD, 82));
        title.setTextFill(Color.GOLD);

        Label subtitle = new Label("Unlock rare birds here.");
        subtitle.setFont(Font.font("Consolas", 24));
        subtitle.setTextFill(Color.web("#B3E5FC"));

        VBox cards = new VBox(20);
        cards.setAlignment(Pos.CENTER);
        boolean hasPending = false;

        if (!falconUnlocked) {
            hasPending = true;
            VBox falconCard = new VBox(14);
            falconCard.setAlignment(Pos.CENTER_LEFT);
            falconCard.setPadding(new Insets(24));
            falconCard.setMaxWidth(1500);
            falconCard.setStyle("-fx-background-color: rgba(0,0,0,0.56); -fx-border-color: #FFB74D; -fx-border-width: 3; -fx-border-radius: 20; -fx-background-radius: 20;");
            Label falconTitle = new Label("FALCON");
            falconTitle.setFont(Font.font("Arial Black", 42));
            falconTitle.setTextFill(Color.web("#FFE082"));
            boolean falconOpen = falconChallengeAvailable();
            Label falconStatus = new Label(!falconOpen
                    ? "Locked: clear Eagle Classic first."
                    : "Available: defeat Falcon boss to unlock the character.");
            falconStatus.setFont(Font.font("Consolas", 24));
            falconStatus.setTextFill(falconOpen ? Color.ORANGE : Color.GRAY);
            Button falconFight = uiFactory.action(
                    falconOpen ? "FIGHT FALCON" : "LOCKED",
                    450, 90, 30, falconOpen ? "#EF6C00" : "#555555", 22,
                    () -> startBossChallenge(stage, BossChallengeType.FALCON)
            );
            falconFight.setDisable(!falconOpen);
            falconFight.setOpacity(falconOpen ? 1.0 : 0.65);
            falconCard.getChildren().addAll(falconTitle, falconStatus, falconFight);
            cards.getChildren().add(falconCard);
        }

        if (!phoenixUnlocked) {
            hasPending = true;
            VBox phoenixCard = new VBox(14);
            phoenixCard.setAlignment(Pos.CENTER_LEFT);
            phoenixCard.setPadding(new Insets(24));
            phoenixCard.setMaxWidth(1500);
            phoenixCard.setStyle("-fx-background-color: rgba(0,0,0,0.56); -fx-border-color: #FF7043; -fx-border-width: 3; -fx-border-radius: 20; -fx-background-radius: 20;");
            Label phoenixTitle = new Label("PHOENIX");
            phoenixTitle.setFont(Font.font("Arial Black", 42));
            phoenixTitle.setTextFill(Color.web("#FFCC80"));
            boolean phoenixOpen = phoenixChallengeAvailable();
            Label phoenixStatus = new Label(!phoenixOpen
                    ? ("Locked: clear " + PHOENIX_CLASSIC_GATE.name + " Classic first.")
                    : "Available: defeat Phoenix boss to unlock the character.");
            phoenixStatus.setFont(Font.font("Consolas", 24));
            phoenixStatus.setTextFill(phoenixOpen ? Color.ORANGE : Color.GRAY);
            Button phoenixFight = uiFactory.action(
                    phoenixOpen ? "FIGHT PHOENIX" : "LOCKED",
                    450, 90, 30, phoenixOpen ? "#E64A19" : "#555555", 22,
                    () -> startBossChallenge(stage, BossChallengeType.PHOENIX)
            );
            phoenixFight.setDisable(!phoenixOpen);
            phoenixFight.setOpacity(phoenixOpen ? 1.0 : 0.65);
            phoenixCard.getChildren().addAll(phoenixTitle, phoenixStatus, phoenixFight);
            cards.getChildren().add(phoenixCard);
        }

        if (!batUnlocked) {
            hasPending = true;
            VBox batCard = new VBox(12);
            batCard.setAlignment(Pos.CENTER_LEFT);
            batCard.setPadding(new Insets(20));
            batCard.setMaxWidth(1500);
            batCard.setStyle("-fx-background-color: rgba(0,0,0,0.5); -fx-border-color: #7E57C2; -fx-border-width: 2; -fx-border-radius: 18; -fx-background-radius: 18;");
            Label batTitle = new Label("BAT");
            batTitle.setFont(Font.font("Arial Black", 34));
            batTitle.setTextFill(Color.web("#D1C4E9"));
            Label batStatus = new Label("Locked: defeat Vulture in Episode battles to unlock.");
            batStatus.setFont(Font.font("Consolas", 22));
            batStatus.setTextFill(Color.GRAY);
            batCard.getChildren().addAll(batTitle, batStatus);
            cards.getChildren().add(batCard);
        }

        if (!titmouseUnlocked) {
            hasPending = true;
            VBox titmouseCard = new VBox(12);
            titmouseCard.setAlignment(Pos.CENTER_LEFT);
            titmouseCard.setPadding(new Insets(20));
            titmouseCard.setMaxWidth(1500);
            titmouseCard.setStyle("-fx-background-color: rgba(0,0,0,0.5); -fx-border-color: #26A69A; -fx-border-width: 2; -fx-border-radius: 18; -fx-background-radius: 18;");
            Label titmouseTitle = new Label("TITMOUSE");
            titmouseTitle.setFont(Font.font("Arial Black", 34));
            titmouseTitle.setTextFill(Color.web("#B2DFDB"));
            Label titmouseStatus = new Label("Locked: clear Hummingbird Classic to unlock.");
            titmouseStatus.setFont(Font.font("Consolas", 22));
            titmouseStatus.setTextFill(Color.GRAY);
            titmouseCard.getChildren().addAll(titmouseTitle, titmouseStatus);
            cards.getChildren().add(titmouseCard);
        }

        if (!hasPending) {
            Label done = new Label("You currently have every obtainable bird.\nMore will come in future updates.");
            done.setFont(Font.font("Consolas", 30));
            done.setTextFill(Color.LIMEGREEN);
            done.setAlignment(Pos.CENTER);
            cards.getChildren().add(done);
        }

        Button back = uiFactory.action("MAIN MENU", 420, 100, 34, "#FF1744", 24, () -> showMenu(stage));

        root.getChildren().addAll(title, subtitle, cards, back);
        root.setMinHeight(HEIGHT);

        ScrollPane scroll = new ScrollPane(root);
        scroll.setFitToWidth(true);
        scroll.setFitToHeight(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scroll.setStyle("-fx-background: transparent; -fx-background-color: transparent;");

        Scene scene = new Scene(scroll, WIDTH, HEIGHT);
        setupKeyboardNavigation(scene);
        applyConsoleHighlight(scene);
        setScenePreservingFullscreen(stage, scene);
        back.requestFocus();
    }

    private void startBossChallenge(Stage stage, BossChallengeType type) {
        if (type == BossChallengeType.NONE) return;
        resetMatchStats();
        storyModeActive = false;
        storyReplayMode = false;
        adventureModeActive = false;
        adventureReplayMode = false;
        currentAdventureBattle = null;
        classicModeActive = false;
        classicEncounter = null;
        classicRun.clear();
        classicRoundIndex = 0;
        classicTeamMode = false;
        Arrays.fill(classicTeams, 1);
        activeBossChallenge = type;
        teamModeEnabled = false;
        competitionModeEnabled = false;
        mutatorModeEnabled = false;
        isTrialMode = false;
        activePlayers = 2;
        Arrays.fill(isAI, false);
        isAI[1] = true;

        BirdType playerType;
        if (type == BossChallengeType.FALCON) playerType = BirdType.EAGLE;
        else if (type == BossChallengeType.PHOENIX) playerType = PHOENIX_CLASSIC_GATE;
        else if (type == BossChallengeType.BAT) playerType = BirdType.PIGEON;
        else playerType = BirdType.HUMMINGBIRD;
        if (playerSlots != null && playerSlots.length >= 2) {
            setSlotBirdType(0, playerType);
            setSlotBirdType(1, BirdType.EAGLE);
        }
        selectedMap = switch (type) {
            case FALCON -> MapType.SKYCLIFFS;
            case PHOENIX -> MapType.CAVE;
            case BAT -> MapType.CAVE;
            case TITMOUSE -> MapType.VIBRANT_JUNGLE;
            default -> MapType.FOREST;
        };

        startMatch(stage);

        if (players[0] != null) {
            players[0].name = "You: " + playerType.name;
            players[0].health = 100;
        }

        BirdType bossType = switch (type) {
            case FALCON -> BirdType.FALCON;
            case PHOENIX -> BirdType.PHOENIX;
            case BAT -> BirdType.BAT;
            case TITMOUSE -> BirdType.TITMOUSE;
            default -> BirdType.EAGLE;
        };
        double bossX = (type == BossChallengeType.FALCON) ? 4200 : (type == BossChallengeType.TITMOUSE ? 4400 : 4300);
        players[1] = new Bird(bossX, bossType, 1, this);
        players[1].y = GROUND_Y - 600;
        players[1].name = "Boss: " + bossType.name;
        players[1].health = 100;
        players[1].setBaseMultipliers(
                1.15,
                (bossType == BirdType.FALCON || bossType == BirdType.PHOENIX) ? 1.3 : 1.2,
                bossType == BirdType.TITMOUSE ? 1.22 : 1.12
        );
        isAI[1] = true;

        addToKillFeed("BOSS CHALLENGE: " + players[1].name);
        if ((bossType == BirdType.FALCON && !falconUnlocked) || (bossType == BirdType.PHOENIX && !phoenixUnlocked)) {
            addToKillFeed("Win to unlock " + bossType.name + "!");
        } else {
            addToKillFeed("Rematch mode: " + bossType.name + " training duel.");
        }
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
        if (classicEncounter.cpuLevel < 1 || classicEncounter.cpuLevel > 9) classicEncounter.cpuLevel = 5;
        String text = classicEncounter.briefing
                + "\n\nMap: " + classicEncounter.map.name()
                + "\nMutator: " + classicEncounter.mutator.label
                + "\nTwist: " + classicEncounter.twist.label + " - " + classicEncounter.twist.description
                + "\nRound: " + (classicRoundIndex + 1) + "/" + classicRun.size();
        playMenuMusic();

        VBox root = new VBox(30);
        root.setAlignment(Pos.CENTER);
        root.setPadding(new Insets(60));
        root.setStyle("-fx-background-color: linear-gradient(to bottom, #081122, #13294B);");

        Label title = new Label("Classic " + (classicRoundIndex + 1) + ": " + classicEncounter.name);
        title.setFont(Font.font("Arial Black", FontWeight.BOLD, 92));
        title.setTextFill(Color.GOLD);

        VBox card = new VBox(20);
        card.setAlignment(Pos.CENTER_LEFT);
        card.setPadding(new Insets(35));
        card.setMaxWidth(1300);
        card.setStyle("-fx-background-color: rgba(0,0,0,0.6); -fx-background-radius: 24; -fx-border-color: #4FC3F7; -fx-border-width: 4; -fx-border-radius: 24;");

        Label speakerLabel = new Label(classicEncounter.announcer);
        speakerLabel.setFont(Font.font("Arial Black", 44));
        speakerLabel.setTextFill(Color.CYAN.brighter());

        Label textLabel = new Label(text);
        textLabel.setWrapText(true);
        textLabel.setFont(Font.font("Consolas", 34));
        textLabel.setTextFill(Color.WHITE);

        card.getChildren().addAll(speakerLabel, textLabel);

        Button cpuBtn = uiFactory.action("CPU LEVEL: " + classicEncounter.cpuLevel, 360, 95, 34, "#455A64", 24, () -> {});
        cpuBtn.setOnAction(e -> {
            playButtonClick();
            classicEncounter.cpuLevel = classicEncounter.cpuLevel % 9 + 1;
            cpuBtn.setText("CPU LEVEL: " + classicEncounter.cpuLevel);
        });
        HBox options = new HBox(20, cpuBtn);
        options.setAlignment(Pos.CENTER);

        HBox buttons = new HBox(30);
        buttons.setAlignment(Pos.CENTER);

        Button continueButton = uiFactory.action("START ENCOUNTER", 520, 120, 52, "#00C853", 32, () -> startClassicEncounter(stage));

        Button menuButton = uiFactory.action("MAIN MENU", 460, 120, 36, "#FF1744", 32, () -> showMenu(stage));
        menuButton.setWrapText(false);
        uiFactory.fitSingleLineOnLayout(menuButton, 36, 20);

        buttons.getChildren().addAll(continueButton, menuButton);
        root.getChildren().addAll(title, card, options, buttons);

        Scene scene = new Scene(root, WIDTH, HEIGHT);
        setupKeyboardNavigation(scene);
        applyConsoleHighlight(scene);
        setScenePreservingFullscreen(stage, scene);
        continueButton.requestFocus();
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
        boolean episodePlayable = ep != EpisodeType.BAT || batUnlocked;

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

        Label epStatus = new Label(
                !episodePlayable
                        ? "Locked - clear Vulture campaign (Pelican Episode)"
                        : (completed ? "Completed" : ("Unlocked Chapters: " + unlocked + "/" + chapters.length))
        );
        epStatus.setFont(Font.font("Consolas", 30));
        epStatus.setTextFill(!episodePlayable ? Color.GRAY : (completed ? Color.LIMEGREEN : Color.ORANGE));

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
        continueEpisode.setDisable(!episodePlayable);
        continueEpisode.setOpacity(episodePlayable ? 1.0 : 0.65);

        Button chapterSelect = uiFactory.action("CHAPTER SELECT", 380, 96, 30, "#1976D2", 22, () -> {
            selectedEpisode = ep;
            showEpisodeChapterSelect(stage);
        });
        chapterSelect.setDisable(!episodePlayable);
        chapterSelect.setOpacity(episodePlayable ? 1.0 : 0.65);

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
                powerUps.add(new PowerUp(2000, GROUND_Y - 820, PowerUpType.TITAN));
                powerUps.add(new PowerUp(3000, GROUND_Y - 980, PowerUpType.OVERCHARGE));
                powerUps.add(new PowerUp(4000, GROUND_Y - 820, PowerUpType.TITAN));
            }
            case WIND_RALLY -> {
                windVents.add(new WindVent(700, GROUND_Y - 360, 380));
                windVents.add(new WindVent(2500, GROUND_Y - 460, 640));
                windVents.add(new WindVent(4800, GROUND_Y - 360, 380));
                powerUps.add(new PowerUp(3000, GROUND_Y - 920, PowerUpType.THERMAL));
            }
            case RAGE_RITUAL -> {
                powerUps.add(new PowerUp(1500, GROUND_Y - 820, PowerUpType.RAGE));
                powerUps.add(new PowerUp(3000, GROUND_Y - 980, PowerUpType.SPEED));
                powerUps.add(new PowerUp(4500, GROUND_Y - 820, PowerUpType.RAGE));
            }
            case SHADOW_CACHE -> {
                powerUps.add(new PowerUp(1800, GROUND_Y - 840, PowerUpType.NEON));
                powerUps.add(new PowerUp(3000, GROUND_Y - 980, PowerUpType.SHRINK));
                powerUps.add(new PowerUp(4200, GROUND_Y - 840, PowerUpType.NEON));
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
            if (classicSelectedBird == BirdType.HUMMINGBIRD) {
                titmouseUnlocked = true;
            }
            saveAchievements();
            String reward = classicRewardFor(classicSelectedBird);
            String charReward = classicCharacterReward(classicSelectedBird);
            showStoryDialogue(
                    stage,
                    "Classic Cleared",
                    "Skycaster Prime",
                    "Run " + classicRunCodename + " completed with " + classicSelectedBird.name + ".\nReward unlocked: "
                            + reward + (charReward.isBlank() ? "" : "\nCharacter unlocked: " + charReward) + ".\nThis bird now has a completion badge.",
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

        StoryChapter clearedChapter = chapters[storyChapterIndex];
        boolean unlockedBatFromVulture = false;
        if (clearedChapter.opponentType == BirdType.VULTURE && !batUnlocked) {
            batUnlocked = true;
            unlockedBatFromVulture = true;
            saveAchievements();
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
                    batUnlocked = true;
                }
                saveAchievements();
                showStoryDialogue(stage,
                        "Episode Complete",
                        "Narrator",
                        "Episode clear. Award unlocked: " + activeEpisodeAward() + "."
                                + (unlockedBatFromVulture ? "\nBat character unlocked for defeating Vulture." : "")
                                + (selectedEpisode == EpisodeType.PIGEON ? "\nSelect Pigeon to cycle skins." : "")
                                + (selectedEpisode == EpisodeType.PELICAN ? "\nSelect Eagle to use the Sky King skin.\nBat character unlocked from Vulture campaign." : ""),
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
                "You won the duel."
                        + (unlockedBatFromVulture ? "\nBat character unlocked for defeating Vulture." : "")
                        + "\nNext chapter: " + next.title + " (" + next.opponentName + ").",
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
        playMenuMusic();
        BorderPane root = new BorderPane();
        root.setPadding(new Insets(28, 36, 30, 36));
        root.setStyle("-fx-background-color: linear-gradient(to bottom, #08101E, #1B2C44);");

        Runnable backAction = () -> {
            Runnable target = stageSelectReturn;
            stageSelectReturn = null;
            if (target != null) {
                target.run();
            } else {
                showMenu(stage);
            }
        };
        Button backArrow = uiFactory.action("<", 120, 90, 64, "#FF1744", 24, backAction);

        Label title = new Label("SELECT STAGE");
        title.setFont(Font.font("Impact", FontWeight.BOLD, 96));
        title.setTextFill(Color.web("#FFE082"));
        title.setEffect(new Glow(0.8));

        HBox topBar = new HBox(24, backArrow, title);
        topBar.setAlignment(Pos.CENTER_LEFT);

        class MapCard {
            final String name;
            final String desc;
            final String color;
            final MapType map;
            MapCard(String name, String desc, String color, MapType map) {
                this.name = name;
                this.desc = desc;
                this.color = color;
                this.map = map;
            }
        }

        List<MapCard> cards = List.of(
                new MapCard("BIG FOREST", "Dense trees, layered platforms, high vertical play.", "#2E7D32", MapType.FOREST),
                new MapCard("PIGEON'S ROOFTOPS", "Neon rooftops with city wind vents.", "#5E35B1", MapType.CITY),
                new MapCard("SKY CLIFFS", "Stepping cliffs and strong updrafts.", "#8D6E63", MapType.SKYCLIFFS),
                new MapCard("VIBRANT JUNGLE", "Vines, nectar nodes, wild vertical mix.", "#388E3C", MapType.VIBRANT_JUNGLE),
                new MapCard("ECHO CAVERN", "Tight cave corridors and hang ledges.", "#455A64", MapType.CAVE),
                new MapCard("BATTLEFIELD", "Small island, three platforms, deadly void.", "#1E88E5", MapType.BATTLEFIELD)
        );

        GridPane grid = new GridPane();
        grid.setHgap(26);
        grid.setVgap(22);
        grid.setAlignment(Pos.TOP_CENTER);

        for (int i = 0; i < cards.size(); i++) {
            MapCard card = cards.get(i);
            int col = i % 2;
            int row = i / 2;

            VBox cardBox = new VBox(8);
            cardBox.setPadding(new Insets(16));
            cardBox.setStyle("-fx-background-color: rgba(0,0,0,0.45); -fx-border-color: #90A4AE; -fx-border-width: 2; -fx-background-radius: 18; -fx-border-radius: 18;");

            Button btn = uiFactory.action(card.name, 680, 120, 34, card.color, 22, () -> beginFreshMatchOnMap(stage, card.map));
            Label desc = new Label(card.desc);
            desc.setFont(Font.font("Consolas", 20));
            desc.setTextFill(Color.web("#CFD8DC"));
            desc.setWrapText(true);
            desc.setMaxWidth(640);

            cardBox.getChildren().addAll(btn, desc);
            grid.add(cardBox, col, row);
        }

        Button randomBtn = uiFactory.action("RANDOM", 1400, 110, 38, "#8E24AA", 24, () -> {
            MapType[] maps = MapType.values();
            beginFreshMatchOnMap(stage, maps[random.nextInt(maps.length)]);
        });
        Label randomHint = new Label("Roll a random arena and adapt on the fly.");
        randomHint.setFont(Font.font("Consolas", 20));
        randomHint.setTextFill(Color.web("#B39DDB"));

        VBox randomBox = new VBox(8, randomBtn, randomHint);
        randomBox.setAlignment(Pos.CENTER);
        randomBox.setPadding(new Insets(12, 0, 0, 0));

        VBox center = new VBox(20, grid, randomBox);
        center.setAlignment(Pos.CENTER);

        ScrollPane scroll = new ScrollPane(center);
        scroll.setFitToWidth(true);
        scroll.setFitToHeight(true);
        scroll.setStyle("-fx-background-color: transparent;");
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);

        root.setTop(topBar);
        root.setCenter(scroll);

        Scene scene = new Scene(root, WIDTH, HEIGHT);
        scene.getRoot().setStyle(scene.getRoot().getStyle() + ";-fx-focus-color:transparent;-fx-faint-focus-color:transparent;");
        setupKeyboardNavigation(scene);
        applyConsoleHighlight(scene);
        setScenePreservingFullscreen(stage, scene);
        backArrow.requestFocus();
    }

    private void beginFreshMatchOnMap(Stage stage, MapType map) {
        stageSelectReturn = null;
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
        if (competitionModeEnabled && !storyModeActive && !adventureModeActive && !isTrialMode && !classicModeActive) {
            showCompetitionDraft(stage, () -> startMatch(stage));
            return;
        }
        startMatch(stage);
    }

    private void setSlotBirdType(int idx, BirdType type) {
        if (idx < 0 || idx >= fightSelectedBirds.length || type == null) return;
        fightSelectedBirds[idx] = type;
        fightRandomSelected[idx] = false;
        if (playerSlots == null || idx >= playerSlots.length || playerSlots[idx] == null) return;
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
            refreshCpuButton(idx);
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

        Button cpuButton = new Button("CPU LV: " + cpuLevels[idx]);
        cpuButton.setPrefSize(180, 60);
        cpuButton.setFont(Font.font("Arial Black", 22));
        cpuButton.setFocusTraversable(true);
        cpuButton.setOnAction(e -> {
            playButtonClick();
            if (!isAI[idx]) return;
            cpuLevels[idx] = cpuLevels[idx] % 9 + 1;
            refreshCpuButton(idx);
        });
        cpuButtons[idx] = cpuButton;
        refreshCpuButton(idx);

        HBox controls = new HBox(10, aiButton, teamButton, cpuButton);
        controls.setAlignment(Pos.CENTER);

        BirdType defaultType = BirdType.values()[idx % BirdType.values().length];
        if (defaultType == BirdType.FALCON) defaultType = BirdType.EAGLE;
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

    private void refreshCpuButton(int idx) {
        if (idx < 0 || idx >= cpuButtons.length) return;
        Button cpuBtn = cpuButtons[idx];
        if (cpuBtn == null) return;
        int level = cpuLevels[idx];
        cpuBtn.setText("CPU LV: " + level);
        boolean aiActive = isAI[idx];
        cpuBtn.setDisable(!aiActive);
        cpuBtn.setOpacity(aiActive ? 1.0 : 0.65);
        cpuBtn.setStyle("-fx-background-color: " + (aiActive ? "#5D4037" : "#455A64") + "; -fx-text-fill: white; -fx-font-weight: bold;");
    }

    private void refreshCpuButtons() {
        for (int i = 0; i < cpuButtons.length; i++) {
            refreshCpuButton(i);
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
            if (bt == BirdType.FALCON) continue; // Falcon is selected from Eagle's split button.
            if (bt != BirdType.EAGLE && !isBirdUnlocked(bt)) continue;
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

            if (bt == BirdType.EAGLE) {
                boolean falconSideVisible = classicCompleted[BirdType.EAGLE.ordinal()];
                String splitNeutral = falconSideVisible
                        ? "-fx-background-color: linear-gradient(to right, #8B0000 0%, #8B0000 50%, #B05F37 50%, #B05F37 100%); -fx-text-fill: white; -fx-font-weight: bold; -fx-text-overrun: clip;"
                        : "-fx-background-color: #8B0000; -fx-text-fill: white; -fx-font-weight: bold; -fx-text-overrun: clip;";
                String splitEagle = falconSideVisible
                        ? "-fx-background-color: linear-gradient(to right, #FFD54F 0%, #FFD54F 50%, #B05F37 50%, #B05F37 100%); -fx-text-fill: black; -fx-font-weight: bold; -fx-text-overrun: clip;"
                        : "-fx-background-color: #FFD54F; -fx-text-fill: black; -fx-font-weight: bold; -fx-text-overrun: clip;";
                String splitEagleSkin = falconSideVisible
                        ? "-fx-background-color: linear-gradient(to right, #FFEA00 0%, #FFEA00 50%, #B05F37 50%, #B05F37 100%); -fx-text-fill: black; -fx-font-weight: bold; -fx-text-overrun: clip;"
                        : "-fx-background-color: #FFEA00; -fx-text-fill: black; -fx-font-weight: bold; -fx-text-overrun: clip;";
                String splitFalcon = "-fx-background-color: linear-gradient(to right, #8B0000 0%, #8B0000 50%, #FFE082 50%, #FFE082 100%); -fx-text-fill: black; -fx-font-weight: bold; -fx-text-overrun: clip;";
                String splitFalconSkin = "-fx-background-color: linear-gradient(to right, #8B0000 0%, #8B0000 50%, #FFF59D 50%, #FFF59D 100%); -fx-text-fill: black; -fx-font-weight: bold; -fx-text-overrun: clip;";
                String falconClassicKey = classicSkinDataKey(BirdType.FALCON);
                boolean hasFalconClassic = isClassicRewardUnlocked(BirdType.FALCON);

                Label eagleSideLabel = new Label("EAGLE");
                eagleSideLabel.setFont(Font.font("Arial Black", 12));
                eagleSideLabel.setTextFill(Color.WHITE);
                eagleSideLabel.setPrefWidth(falconSideVisible ? 78 : 156);
                eagleSideLabel.setAlignment(Pos.CENTER);
                eagleSideLabel.setMouseTransparent(true);

                Label falconSideLabel = new Label(falconSideVisible ? "FALCON" : "");
                falconSideLabel.setFont(Font.font("Arial Black", 12));
                falconSideLabel.setTextFill(Color.WHITE);
                falconSideLabel.setPrefWidth(falconSideVisible ? 78 : 0);
                falconSideLabel.setAlignment(Pos.CENTER);
                falconSideLabel.setMouseTransparent(true);

                HBox splitLabel = new HBox(eagleSideLabel, falconSideLabel);
                splitLabel.setAlignment(Pos.CENTER);
                splitLabel.setMouseTransparent(true);

                b.setText("");
                b.setGraphic(splitLabel);
                b.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
                b.setStyle(splitNeutral);

                Runnable selectEagle = () -> {
                    selected.setText("Selected: Eagle");
                    box.setUserData(BirdType.EAGLE);
                    stats.setText("Power: " + BirdType.EAGLE.power + " | Speed: " + String.format("%.1f", BirdType.EAGLE.speed) + " | Jump: " + BirdType.EAGLE.jumpHeight + "\n\nSPECIAL: " + BirdType.EAGLE.ability);
                    b.setStyle(splitEagle);
                    eagleSideLabel.setTextFill(Color.BLACK);
                    falconSideLabel.setTextFill(Color.WHITE);
                };

                Runnable selectEagleSkin = () -> {
                    selected.setText("Selected: Sky King Eagle (Skin)");
                    box.setUserData("SKY_KING_EAGLE");
                    stats.setText("Power: " + BirdType.EAGLE.power + " | Speed: " + String.format("%.1f", BirdType.EAGLE.speed) + " | Jump: " + BirdType.EAGLE.jumpHeight + "\n\nSPECIAL: " + BirdType.EAGLE.ability);
                    b.setStyle(splitEagleSkin);
                    eagleSideLabel.setTextFill(Color.BLACK);
                    falconSideLabel.setTextFill(Color.WHITE);
                };

                Runnable selectFalcon = () -> {
                    if (!falconUnlocked) {
                        selected.setText("Falcon Locked (defeat Falcon boss)");
                        box.setUserData(BirdType.EAGLE);
                        stats.setText("Power: " + BirdType.EAGLE.power + " | Speed: " + String.format("%.1f", BirdType.EAGLE.speed) + " | Jump: " + BirdType.EAGLE.jumpHeight + "\n\nSPECIAL: " + BirdType.EAGLE.ability);
                        b.setStyle(splitNeutral);
                        eagleSideLabel.setTextFill(Color.WHITE);
                        falconSideLabel.setTextFill(Color.web("#FFCDD2"));
                        return;
                    }
                    selected.setText("Selected: Falcon");
                    box.setUserData(BirdType.FALCON);
                    stats.setText("Power: " + BirdType.FALCON.power + " | Speed: " + String.format("%.1f", BirdType.FALCON.speed) + " | Jump: " + BirdType.FALCON.jumpHeight + "\n\nSPECIAL: " + BirdType.FALCON.ability);
                    b.setStyle(splitFalcon);
                    eagleSideLabel.setTextFill(Color.WHITE);
                    falconSideLabel.setTextFill(Color.BLACK);
                };

                Runnable selectFalconSkin = () -> {
                    selected.setText("Selected: " + classicRewardFor(BirdType.FALCON) + " (Skin)");
                    box.setUserData(falconClassicKey);
                    stats.setText("Power: " + BirdType.FALCON.power + " | Speed: " + String.format("%.1f", BirdType.FALCON.speed) + " | Jump: " + BirdType.FALCON.jumpHeight + "\n\nSPECIAL: " + BirdType.FALCON.ability);
                    b.setStyle(splitFalconSkin);
                    eagleSideLabel.setTextFill(Color.WHITE);
                    falconSideLabel.setTextFill(Color.BLACK);
                };

                Runnable cycleEagleSide = () -> {
                    Object currentData = box.getUserData();
                    if ("SKY_KING_EAGLE".equals(currentData)) {
                        selectEagle.run();
                    } else if (currentData == BirdType.EAGLE && eagleSkinUnlocked) {
                        selectEagleSkin.run();
                    } else {
                        selectEagle.run();
                    }
                };

                Runnable cycleFalconSide = () -> {
                    if (!falconSideVisible) {
                        cycleEagleSide.run();
                        return;
                    }
                    if (!falconUnlocked) {
                        selectFalcon.run();
                        return;
                    }
                    if (!isBirdUnlocked(BirdType.FALCON)) {
                        selectEagle.run();
                        return;
                    }
                    Object currentData = box.getUserData();
                    if (falconClassicKey.equals(currentData)) {
                        selectFalcon.run();
                    } else if (currentData == BirdType.FALCON && hasFalconClassic) {
                        selectFalconSkin.run();
                    } else {
                        selectFalcon.run();
                    }
                };

                b.setOnMousePressed(e ->
                        b.getProperties().put("splitPickFalcon", falconSideVisible && e.getX() > b.getWidth() * 0.5));
                b.setOnKeyPressed(e -> {
                    if (e.getCode() == KeyCode.LEFT) {
                        b.getProperties().put("splitKeyboardSide", "eagle");
                        cycleEagleSide.run();
                        e.consume();
                    } else if (e.getCode() == KeyCode.RIGHT && falconSideVisible) {
                        b.getProperties().put("splitKeyboardSide", "falcon");
                        cycleFalconSide.run();
                        e.consume();
                    }
                });
                b.setOnAction(e -> {
                    playButtonClick();
                    Object sideData = b.getProperties().remove("splitKeyboardSide");
                    boolean keyboardFalcon = falconSideVisible && "falcon".equals(sideData);
                    boolean mouseFalcon = Boolean.TRUE.equals(b.getProperties().remove("splitPickFalcon")) && falconSideVisible;
                    boolean pickFalcon = mouseFalcon || keyboardFalcon;
                    if (pickFalcon) cycleFalconSide.run();
                    else cycleEagleSide.run();
                });

                grid.add(b, col++, row);
                if (col > 2) { col = 0; row++; }
                continue;
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

        if (hasAnyLockedBirds()) {
            Button lockedHint = new Button("?\nLOCKED");
            lockedHint.setPrefSize(160, 70);
            lockedHint.setFont(Font.font("Arial Black", 20));
            lockedHint.setWrapText(true);
            lockedHint.setDisable(true);
            lockedHint.setStyle("-fx-background-color: #3A3A3A; -fx-text-fill: #BDBDBD; -fx-font-weight: bold;");
            grid.add(lockedHint, col++, row);
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
        if (def == BirdType.FALCON) {
            selected.setText("Selected: Falcon");
            box.setUserData(BirdType.FALCON);
            stats.setText("Power: " + BirdType.FALCON.power + " | Speed: " + String.format("%.1f", BirdType.FALCON.speed) + " | Jump: " + BirdType.FALCON.jumpHeight + "\n\nSPECIAL: " + BirdType.FALCON.ability);
        }
        for (var node : grid.getChildren()) {
            boolean defaultMatches = false;
            if (node instanceof Button btn) {
                Object nodeData = btn.getUserData();
                defaultMatches = nodeData == def || (def == BirdType.FALCON && nodeData == BirdType.EAGLE);
            }
            if (node instanceof Button btn && defaultMatches) {
                selected.setText("Selected: " + def.name);
                box.setUserData(def);
                stats.setText("Power: " + def.power + " | Speed: " + String.format("%.1f", def.speed) + " | Jump: " + def.jumpHeight + "\n\nSPECIAL: " + def.ability);
                if (def == BirdType.EAGLE) {
                    if (classicCompleted[BirdType.EAGLE.ordinal()]) {
                        btn.setStyle("-fx-background-color: linear-gradient(to right, #FFD54F 0%, #FFD54F 50%, #B05F37 50%, #B05F37 100%); -fx-text-fill: black; -fx-font-weight: bold; -fx-text-overrun: clip;");
                    } else {
                        btn.setStyle("-fx-background-color: #FFD54F; -fx-text-fill: black; -fx-font-weight: bold; -fx-text-overrun: clip;");
                    }
                } else if (def == BirdType.FALCON) {
                    btn.setStyle("-fx-background-color: linear-gradient(to right, #8B0000 0%, #8B0000 50%, #FFE082 50%, #FFE082 100%); -fx-text-fill: black; -fx-font-weight: bold; -fx-text-overrun: clip;");
                } else {
                    btn.setStyle("-fx-background-color: " + toHex(def.color) + "; -fx-text-fill: white; -fx-font-weight: bold;");
                }
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

        if (storyModeActive || adventureModeActive || isTrialMode) return;

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
        boolean teamComp = teamModeEnabled && !storyModeActive && !adventureModeActive && !isTrialMode && !classicModeActive;
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
        boolean teamComp = teamModeEnabled && !storyModeActive && !adventureModeActive && !isTrialMode && !classicModeActive;
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
        if (!(competitionModeEnabled && !storyModeActive && !adventureModeActive && !isTrialMode && !classicModeActive)) return false;

        boolean teamComp = teamModeEnabled && !storyModeActive && !adventureModeActive && !isTrialMode && !classicModeActive;
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
        if (storyModeActive || adventureModeActive || isTrialMode) return;

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
        if (competitionModeEnabled && !storyModeActive && !adventureModeActive && !classicModeActive) return;
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
                        PowerUpType.SHRINK, PowerUpType.TITAN
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
                        PowerUpType.OVERCHARGE, PowerUpType.TITAN
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
        KeyCode code = e.getCode();
        boolean alreadyDown = pressedKeys.contains(code);
        if (!isBlockedInputForAI(code)) {
            pressedKeys.add(code);
        }
        if (code != null && !alreadyDown) {
            registerDashTapForKey(code);
        }
        if (e.getCode() == KeyCode.ESCAPE) {
            togglePause(stage);
        } else if (e.getCode() == KeyCode.F11) {
            stage.setFullScreen(!stage.isFullScreen());
        } else if (e.getCode() == KeyCode.F3) {
            debugHudEnabled = !debugHudEnabled;
            addToKillFeed(debugHudEnabled ? "DEBUG HUD: ON" : "DEBUG HUD: OFF");
        }
    }

    private void handleGameplayKeyRelease(KeyEvent e) {
        KeyCode code = e.getCode();
        if (!isBlockedInputForAI(code)) {
            pressedKeys.remove(code);
        }
    }

    private boolean isBlockedInputForAI(KeyCode code) {
        for (int i = 0; i < players.length; i++) {
            if (players[i] == null || !isAI[i]) continue;
            if (isControlKeyForPlayer(code, i)) return true;
        }
        return false;
    }

    private void registerDashTapForKey(KeyCode code) {
        for (int i = 0; i < activePlayers; i++) {
            if (players[i] == null || isAI[i]) continue;
            if (code == leftKeyForPlayer(i)) {
                players[i].registerDashTap(-1);
            } else if (code == rightKeyForPlayer(i)) {
                players[i].registerDashTap(1);
            }
        }
    }

    private KeyCode leftKeyForPlayer(int playerIdx) {
        return switch (playerIdx) {
            case 0 -> KeyCode.A;
            case 1 -> KeyCode.LEFT;
            case 2 -> KeyCode.F;
            case 3 -> KeyCode.J;
            default -> KeyCode.A;
        };
    }

    private KeyCode rightKeyForPlayer(int playerIdx) {
        return switch (playerIdx) {
            case 0 -> KeyCode.D;
            case 1 -> KeyCode.RIGHT;
            case 2 -> KeyCode.H;
            case 3 -> KeyCode.L;
            default -> KeyCode.D;
        };
    }

    private boolean isControlKeyForPlayer(KeyCode code, int playerIdx) {
        return switch (playerIdx) {
            case 0 -> code == KeyCode.A || code == KeyCode.D || code == KeyCode.W
                    || code == KeyCode.SPACE || code == KeyCode.SHIFT || code == KeyCode.S;
            case 1 -> code == KeyCode.LEFT || code == KeyCode.RIGHT || code == KeyCode.UP
                    || code == KeyCode.ENTER || code == KeyCode.SLASH || code == KeyCode.DOWN;
            case 2 -> code == KeyCode.F || code == KeyCode.H || code == KeyCode.T
                    || code == KeyCode.Y || code == KeyCode.U || code == KeyCode.G;
            case 3 -> code == KeyCode.J || code == KeyCode.L || code == KeyCode.I
                    || code == KeyCode.O || code == KeyCode.P || code == KeyCode.K;
            default -> false;
        };
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

    public int getCpuLevel(int playerIdx) {
        int level = 5;
        if (adventureModeActive && currentAdventureBattle != null) {
            level = currentAdventureBattle.cpuLevel;
        } else if (classicModeActive && classicEncounter != null) {
            level = classicEncounter.cpuLevel;
        } else if (playerIdx >= 0 && playerIdx < cpuLevels.length) {
            level = cpuLevels[playerIdx];
        }
        if (level < 1) level = 1;
        if (level > 9) level = 9;
        return level;
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
        if (adventureModeActive) {
            if (adventureTeamMode) {
                return adventureTeams[playerIdx] <= 0 ? 1 : adventureTeams[playerIdx];
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

    private void positionBattlefieldSpawns() {
        if (selectedMap != MapType.BATTLEFIELD || battlefieldIslandW <= 0) return;
        List<Bird> active = new ArrayList<>();
        for (Bird b : players) {
            if (b != null) active.add(b);
        }
        if (active.isEmpty()) return;

        double margin = Math.min(220, Math.max(120, battlefieldIslandW * 0.15));
        double usable = Math.max(0, battlefieldIslandW - margin * 2);
        double step = active.size() <= 1 ? 0 : usable / (active.size() - 1);

        for (int i = 0; i < active.size(); i++) {
            Bird b = active.get(i);
            double center = active.size() == 1
                    ? battlefieldIslandX + battlefieldIslandW / 2.0
                    : battlefieldIslandX + margin + step * i;
            double halfWidth = 40 * b.sizeMultiplier;
            b.x = center - halfWidth;
            b.y = battlefieldIslandY - 80 * b.sizeMultiplier;
            b.vx = 0;
            b.vy = 0;
        }
    }

    double battlefieldSpawnCenterX() {
        if (battlefieldIslandW > 0) {
            return battlefieldIslandX + battlefieldIslandW / 2.0;
        }
        return WORLD_WIDTH / 2.0;
    }

    double battlefieldSpawnY(double sizeMultiplier) {
        if (battlefieldIslandW > 0) {
            return battlefieldIslandY - 80 * sizeMultiplier;
        }
        return GROUND_Y - 300;
    }

    private void startMatch(Stage stage) { 
        isTrialMode = false;
        matchTimer = MATCH_DURATION_FRAMES;
        storyMatchTimerOverride = -1;
        storyTeamMode = false;
        Arrays.fill(storyTeams, 1);
        adventureMatchTimerOverride = -1;
        adventureTeamMode = false;
        Arrays.fill(adventureTeams, 1);
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
        boolean[] menuAI = Arrays.copyOf(isAI, isAI.length);
        Arrays.fill(isAI, false);
        StoryChapter storyChapter = null;
        AdventureBattle adventureBattle = null;
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
        } else if (adventureModeActive) {
            adventureBattle = currentAdventureBattle != null ? currentAdventureBattle : activeAdventureBattle();
            setupAdventureBattleRoster(adventureBattle);
        } else {
            List<BirdType> pool = unlockedBirdPool();
            for (int i = 0; i < activePlayers; i++) {
                BirdType type = fightSelectedBirds[i];
                boolean randomPick = fightRandomSelected[i];
                if (randomPick || type == null || !isBirdUnlocked(type)) {
                    type = pool.get(random.nextInt(pool.size()));
                }
                double startX = 300 + i * (WIDTH - 600) / Math.max(1, activePlayers - 1);
                isAI[i] = menuAI[i];
                players[i] = new Bird(startX, type, i, this);
                String skinKey = randomPick ? null : fightSelectedSkinKeys[i];
                skinKey = normalizeAdventureSkinChoice(type, skinKey);
                fightSelectedSkinKeys[i] = skinKey;
                applySkinChoiceToBird(players[i], type, skinKey);

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
        if (selectedMap == MapType.BATTLEFIELD) {
            double islandW = 1200;
            double islandH = 70;
            double islandX = (WORLD_WIDTH - islandW) / 2.0;
            double islandY = GROUND_Y - 120;
            platforms.add(new Platform(islandX, islandY, islandW, islandH));
            platforms.add(new Platform(islandX + 120, islandY - 320, 360, 45));
            platforms.add(new Platform(islandX + islandW - 480, islandY - 320, 360, 45));
            platforms.add(new Platform(islandX + (islandW - 420) / 2.0, islandY - 520, 420, 45));
            battlefieldIslandX = islandX;
            battlefieldIslandW = islandW;
            battlefieldIslandY = islandY;
        } else {
            platforms.add(new Platform(0, GROUND_Y, WORLD_WIDTH, 600)); // thick floor
            platforms.add(new Platform(-100, 0, 100, WORLD_HEIGHT)); // left wall
            platforms.add(new Platform(WORLD_WIDTH, 0, 100, WORLD_HEIGHT)); // right wall
            battlefieldIslandX = 0;
            battlefieldIslandW = 0;
            battlefieldIslandY = 0;
        }

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
        } else if (selectedMap == MapType.BATTLEFIELD) {
            mountainPeaks = null;
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
        if (adventureModeActive && adventureBattle != null) {
            applyAdventureBattleArenaModifiers(adventureBattle);
            if (adventureMatchTimerOverride > 0) {
                matchTimer = adventureMatchTimerOverride;
            }
        }
        if (classicModeActive && classicRound != null) {
            applyClassicEncounterArenaModifiers(classicRound);
            if (classicRound.timerFrames > 0) {
                matchTimer = classicRound.timerFrames;
            }
        }
        configureMatchModes();
        positionBattlefieldSpawns();

        Canvas canvas = new Canvas(WIDTH, HEIGHT);
        GraphicsContext g = canvas.getGraphicsContext2D();
        StackPane root = new StackPane(canvas);
        gameRoot = root;
        root.setStyle((selectedMap == MapType.FOREST || selectedMap == MapType.BATTLEFIELD)
                ? "-fx-background-color: linear-gradient(to bottom, #87CEEB 0%, #B3E5FC 50%, #E0F2F1 100%);"
                : "-fx-background-color: #000011;");
        Scene scene = new Scene(root, WIDTH, HEIGHT);
        makeSceneResponsive(scene);
        uiCanvas = new Canvas(WIDTH, HEIGHT);
        ui = uiCanvas.getGraphicsContext2D();
        root.getChildren().add(uiCanvas);

        pressedKeys.clear();
        scene.setOnKeyPressed(e -> handleGameplayKeyPress(stage, e));
        scene.setOnKeyReleased(this::handleGameplayKeyRelease);

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

                if (activeMutator != MatchMutator.NONE && !competitionModeEnabled && !storyModeActive && !adventureModeActive && !isTrialMode && !classicModeActive) {
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
                if (competitionModeEnabled && !storyModeActive && !adventureModeActive && !isTrialMode && !classicModeActive) {
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
        int shownHealth = (int) Math.round(Math.max(0, b.health));
        double maxHealth = Math.max(1.0, b.getMaxHealth());
        double fillRatio = Math.max(0, Math.min(1.0, b.health / maxHealth));
        g.setFill(Color.BLACK);
        g.fillRoundRect(x - 3, y - 3, 406, 46, 10, 10);
        g.setFill(Color.RED);
        g.fillRoundRect(x, y, 400, 40, 10, 10);
        boolean compStyle = competitionModeEnabled && !storyModeActive && !adventureModeActive && !isTrialMode && !classicModeActive;
        if (b.health > maxHealth) {
            g.setFill(Color.ORANGE.brighter());
        } else {
            g.setFill(compStyle ? Color.DODGERBLUE : Color.LIME);
        }
        g.fillRoundRect(x, y, 400 * fillRatio, 40, 10, 10);
        g.setFill(Color.WHITE);
        g.setFont(Font.font("Arial Black", 28));
        g.fillText(b.name + " " + shownHealth + "%", x + 20, y + 32);
    }

    private int awardBirdCoinsForMatch(Bird winner) {
        int coins = 30 + activePlayers * 10;
        if (winner != null) coins += 10;
        if (classicModeActive) coins += 60;
        else if (adventureModeActive) coins += 40;
        else if (storyModeActive) coins += 30;
        if (competitionModeEnabled) coins += 20;
        birdCoins += coins;
        saveAchievements();
        return coins;
    }

    private void showMatchSummary(Stage stage, Bird winner) {
        if (musicPlayer != null) musicPlayer.stop();
        if (menuMusicPlayer != null) menuMusicPlayer.stop();
        if (victoryMusicPlayer != null) {
            victoryMusicPlayer.stop();
            victoryMusicPlayer.play();
        }
        resolveBossChallengeOutcome(winner);
        VBox root = new VBox(40);
        root.setAlignment(Pos.CENTER);
        root.setPadding(new Insets(60));
        root.setStyle("-fx-background-color: linear-gradient(to bottom, #0f0c29, #302b63, #24243e);");

        Label title = new Label(winner != null ? winner.name.toUpperCase() + " WINS!" : "TIME'S UP!");
        title.setFont(Font.font("Arial Black", 110));
        title.setTextFill(winner != null ? Color.GOLD : Color.SILVER);
        title.setEffect(new DropShadow(50, Color.BLACK));

        int coinsEarned = awardBirdCoinsForMatch(winner);
        Label coinsLabel = new Label("BIRD COINS +" + coinsEarned + "   TOTAL: " + birdCoins);
        coinsLabel.setFont(Font.font("Consolas", 28));
        coinsLabel.setTextFill(Color.web("#FFD54F"));

        // Collect all active birds
        List<Bird> activeBirds = new ArrayList<>();
        for (int i = 0; i < activePlayers; i++) {
            if (players[i] != null) activeBirds.add(players[i]);
        }

        HBox buttons = buildSummaryButtons(stage, winner);
        applyWinnerMapProgress(winner);

        boolean teamSummaryMode =
                (teamModeEnabled && !isTrialMode && !storyModeActive && !adventureModeActive && !classicModeActive)
                        || (storyModeActive && storyTeamMode)
                        || (adventureModeActive && adventureTeamMode)
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
            root.getChildren().addAll(title, coinsLabel, podium, analyticsPanel, buttons);
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
        root.getChildren().addAll(title, coinsLabel, podium, analyticsPanel, buttons);
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
        if (activeBossChallenge != BossChallengeType.NONE) {
            Button rematch = button("REMATCH BOSS", "#FF7043");
            rematch.setOnAction(e -> {
                resetMatchStats();
                BossChallengeType type = activeBossChallenge;
                startBossChallenge(stage, type);
            });
            Button bossMenu = button("BOSS MENU", "#6A1B9A");
            bossMenu.setOnAction(e -> {
                resetMatchStats();
                activeBossChallenge = BossChallengeType.NONE;
                showBossChallenges(stage);
            });
            Button menu = button("MAIN MENU", "#9C27B0");
            menu.setOnAction(e -> {
                resetMatchStats();
                activeBossChallenge = BossChallengeType.NONE;
                showMenu(stage);
            });
            buttons.getChildren().addAll(rematch, bossMenu, menu);
        } else if (classicModeActive) {
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
        } else if (adventureModeActive) {
            Button continueAdventure = button("CONTINUE ADVENTURE", "#1565C0");
            continueAdventure.setOnAction(e -> {
                resetMatchStats();
                handleAdventureMatchEnd(stage, winner);
            });
            Button hub = button("ADVENTURE HUB", "#00897B");
            hub.setOnAction(e -> {
                resetMatchStats();
                showAdventureHub(stage);
            });
            Button menu = button("MAIN MENU", "#9C27B0");
            menu.setOnAction(e -> {
                resetMatchStats();
                showMenu(stage);
            });
            buttons.getChildren().addAll(continueAdventure, hub, menu);
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
                if (competitionModeEnabled && !storyModeActive && !adventureModeActive && !isTrialMode && !classicModeActive) {
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

    private void resolveBossChallengeOutcome(Bird winner) {
        if (activeBossChallenge == BossChallengeType.NONE) return;
        boolean playerWon = winner != null && winner.playerIndex == 0;
        if (!playerWon) return;

        if (activeBossChallenge == BossChallengeType.FALCON && !falconUnlocked) {
            falconUnlocked = true;
            addToKillFeed("FALCON UNLOCKED!");
            saveAchievements();
        } else if (activeBossChallenge == BossChallengeType.PHOENIX && !phoenixUnlocked) {
            phoenixUnlocked = true;
            addToKillFeed("PHOENIX UNLOCKED!");
            saveAchievements();
        }
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

        if (isTrialMode || storyModeActive || adventureModeActive) return;

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
        birdCoins = Math.max(0, prefs.getInt("bird_coins", 0));
        cityPigeonUnlocked = true;
        noirPigeonUnlocked = prefs.getBoolean("skin_noirpigeon", false);
        eagleSkinUnlocked = prefs.getBoolean("skin_eagle", true);
        batUnlocked = prefs.getBoolean("char_bat_unlocked", false);
        falconUnlocked = prefs.getBoolean("char_falcon_unlocked", false);
        phoenixUnlocked = prefs.getBoolean("char_phoenix_unlocked", false);
        titmouseUnlocked = prefs.getBoolean("char_titmouse_unlocked", false);
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
            batUnlocked = true;
        }
        for (BirdType type : BirdType.values()) {
            int idx = type.ordinal();
            adventureUnlocked[idx] = prefs.getBoolean("adv_bird_" + type.name(), false);
        }
        adventureUnlocked[BirdType.PIGEON.ordinal()] = true;
        int advBattles = adventureChapters.length > 0 ? adventureChapters[0].battles.length : 0;
        adventureChapterProgress = Math.max(0, Math.min(prefs.getInt("adv_ch1_progress", 0), advBattles));
        adventureChapterCompleted = prefs.getBoolean("adv_ch1_done", false);
        if (adventureChapterCompleted && advBattles > 0) {
            adventureChapterProgress = advBattles;
        }
        for (BirdType type : BirdType.values()) {
            int idx = type.ordinal();
            classicCompleted[idx] = prefs.getBoolean("classic_done_" + type.name(), false);
            classicSkinUnlocked[idx] = prefs.getBoolean("classic_skin_" + type.name(), false);
        }
        if (classicCompleted[BirdType.HUMMINGBIRD.ordinal()]) {
            titmouseUnlocked = true;
        }
        // Requested defaults
        eagleSkinUnlocked = true;
        classicSkinUnlocked[BirdType.EAGLE.ordinal()] = true;
        classicSkinUnlocked[BirdType.PIGEON.ordinal()] = noirPigeonUnlocked;
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
        prefs.putInt("bird_coins", birdCoins);
        prefs.putBoolean("skin_citypigeon", cityPigeonUnlocked);
        prefs.putBoolean("skin_noirpigeon", noirPigeonUnlocked);
        prefs.putBoolean("skin_eagle", eagleSkinUnlocked);
        prefs.putBoolean("char_bat_unlocked", batUnlocked);
        prefs.putBoolean("char_falcon_unlocked", falconUnlocked);
        prefs.putBoolean("char_phoenix_unlocked", phoenixUnlocked);
        prefs.putBoolean("char_titmouse_unlocked", titmouseUnlocked);
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
        prefs.putInt("adv_ch1_progress", adventureChapterProgress);
        prefs.putBoolean("adv_ch1_done", adventureChapterCompleted);
        for (BirdType type : BirdType.values()) {
            int idx = type.ordinal();
            prefs.putBoolean("adv_bird_" + type.name(), adventureUnlocked[idx]);
        }
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

        BorderPane root = new BorderPane();
        root.setPadding(new Insets(30, 40, 30, 40));
        root.setStyle("-fx-background-color: linear-gradient(to bottom, #0C1220, #1A2A44);");

        Label title = new Label("ACHIEVEMENTS");
        title.setFont(Font.font("Impact", FontWeight.BOLD, 96));
        title.setTextFill(Color.web("#FFE082"));
        title.setEffect(new DropShadow(30, Color.BLACK));

        VBox list = new VBox(18);
        list.setAlignment(Pos.TOP_CENTER);

        for (int i = 0; i < ACHIEVEMENT_COUNT; i++) {
            boolean unlocked = achievementsUnlocked[i];
            VBox card = new VBox(8);
            card.setPadding(new Insets(18, 22, 18, 22));
            card.setMaxWidth(1400);
            String bg = unlocked ? "rgba(46,125,50,0.85)" : "rgba(20,20,20,0.6)";
            String border = unlocked ? "#FFD54F" : "#37474F";
            card.setStyle("-fx-background-color: " + bg + "; -fx-border-color: " + border + "; -fx-border-width: 3; -fx-background-radius: 18; -fx-border-radius: 18;");

            Label nameLabel = new Label(ACHIEVEMENT_NAMES[i]);
            nameLabel.setFont(Font.font("Impact", 34));
            nameLabel.setTextFill(unlocked ? Color.web("#C8E6C9") : Color.web("#B0BEC5"));

            String status = unlocked ? "UNLOCKED" : "LOCKED";
            Label statusLabel = new Label(status);
            statusLabel.setFont(Font.font("Consolas", 20));
            statusLabel.setTextFill(unlocked ? Color.web("#FFF59D") : Color.web("#78909C"));

            String descText = sanitizeAchievementText(ACHIEVEMENT_DESCRIPTIONS[i]);
            Label descLabel = new Label(descText);
            descLabel.setFont(Font.font("Consolas", 22));
            descLabel.setTextFill(unlocked ? Color.web("#E8F5E9") : Color.web("#B0BEC5"));
            descLabel.setWrapText(true);
            descLabel.setMaxWidth(1200);

            HBox header = new HBox(16, nameLabel, statusLabel);
            header.setAlignment(Pos.CENTER_LEFT);

            card.getChildren().addAll(header, descLabel);
            list.getChildren().add(card);
        }

        ScrollPane scroll = new ScrollPane(list);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background: transparent; -fx-background-color: transparent;");
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);

        Button back = uiFactory.action("BACK", 360, 100, 40, "#FF1744", 28, () -> showMenu(stage));

        BorderPane.setAlignment(title, Pos.CENTER);
        root.setTop(title);
        root.setCenter(scroll);
        root.setBottom(back);
        BorderPane.setAlignment(back, Pos.CENTER);

        Scene scene = new Scene(root, WIDTH, HEIGHT);
        setupKeyboardNavigation(scene);
        applyConsoleHighlight(scene);

        // Hide default blue mouse focus ring
        root.setStyle(root.getStyle() + ";-fx-focus-color:transparent;-fx-faint-focus-color:transparent;");

        setScenePreservingFullscreen(stage, scene);
        back.requestFocus();
    }

    private String sanitizeAchievementText(String text) {
        if (text == null) return "";
        return text.replace("â€“", " - ")
                .replace("â€—", " - ")
                .replace("â”", "\"")
                .replace("â“", "\"")
                .replace("âœ“", "")
                .replace("â‐", "-")
                .replace("â”", "-")
                .replace("\uFFFD", "");
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










