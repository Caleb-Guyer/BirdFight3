package com.example.birdgame3;
import com.example.birdgame3.Bird;

import javafx.animation.AnimationTimer;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.Control;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.effect.DropShadow;
import javafx.scene.effect.Glow;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.media.AudioClip;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;

import java.util.*;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

public class BirdGame3 extends Application {

    public static final int WIDTH = 1920;
    public static final int HEIGHT = 1080;
    public static final int GROUND_Y = 2400;
    public static final double GRAVITY = 0.75;

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
    private int matchTimer = 90 * 60;           // 90 seconds at 60 FPS
    private boolean suddenDeathActive = false;

    private boolean matchEnded = false;  // prevents double-trigger

    private boolean isPaused = false;

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
    public enum MapType { FOREST, CITY, SKYCLIFFS, VIBRANT_JUNGLE }

    public MapType selectedMap = MapType.FOREST; // default

    public List<Particle> particles = new ArrayList<>();
    public List<CrowMinion> crowMinions = new ArrayList<>();

    // === CITY WIND BURSTS ===
    public List<WindVent> windVents = new ArrayList<>();
    private long lastWindBurstTime = 0;
    public static final long WIND_BURST_INTERVAL = 1_000_000_000L * 6; // every ~6 seconds
    public static final double WIND_FORCE = -28.0; // strong upward boost

    public Canvas uiCanvas;
    public GraphicsContext ui;

    private double flashAlpha = 0.0;     // white flash intensity
    private double redFlashAlpha = 0.0;  // red tint for normal big hits
    private int flashTimer = 0;          // frames remaining for flash

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
            System.out.println("⚠️ Sounds not found - put mp3 files in 'sounds' folder inside src/main/resources");
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
        scene.setOnKeyPressed(e -> {
            KeyCode code = e.getCode();

            // WASD → arrows for focus
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
                + "-fx-border-width: 10; "
                + "-fx-border-radius: 20;");
    }

    private void restoreControl(Control ctrl) {
        Object data = ctrl.getUserData();
        if (data instanceof String style) ctrl.setStyle(style);
    }

    private void startPigeonTrial(Stage stage) {
        selectedMap = MapType.CITY;
        isTrialMode = true;
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
        players[3].sizeMultiplier = 2.5;
        players[3].powerMultiplier = 1.8;
        players[3].speedMultiplier = 0.4;
        players[3].isCitySkin = true;
        players[3].name = "BOSS: City Pigeon";
        isAI[3] = true;

        matchTimer = 60 * 180;
        matchEnded = false;
        suddenDeathActive = false;

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
        uiCanvas = new Canvas(WIDTH, HEIGHT);
        ui = uiCanvas.getGraphicsContext2D();
        root.getChildren().add(uiCanvas);

        pressedKeys.clear();
        scene.setOnKeyPressed(e -> {
            pressedKeys.add(e.getCode());
            if (e.getCode() == KeyCode.ESCAPE) {
                togglePause(stage);
            }
        });
        scene.setOnKeyReleased(e -> pressedKeys.remove(e.getCode()));

        if (timer != null) timer.stop();
        lastUpdate = 0;
        accumulator = 0;

        timer = new AnimationTimer() {
            @Override
            public void handle(long now) {
                gameTick(1.0);
                drawGame(g);   // ← ONE LINE instead of 150 lines

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
        stage.setScene(scene);
        startMusic();
        canvas.requestFocus();

        zoom = 0.35;
        camX = WORLD_WIDTH / 2 - WIDTH / (2 * zoom);
        camY = (GROUND_Y - 1200) - HEIGHT / (2 * zoom);

    }

    private void startEagleTrial(Stage stage) {
        selectedMap = MapType.SKYCLIFFS;
        isTrialMode = true;
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
        players[3].sizeMultiplier = 3.0;
        players[3].powerMultiplier = 2.2;
        players[3].speedMultiplier = 0.7;
        players[3].name = "BOSS: SKY TYRANT";
        isAI[3] = true;

        matchTimer = 60 * 180; // 3 minutes
        matchEnded = false;
        suddenDeathActive = false;

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
        uiCanvas = new Canvas(WIDTH, HEIGHT);
        ui = uiCanvas.getGraphicsContext2D();
        root.getChildren().add(uiCanvas);

        pressedKeys.clear();
        scene.setOnKeyPressed(e -> {
            pressedKeys.add(e.getCode());
            if (e.getCode() == KeyCode.ESCAPE) {
                togglePause(stage);
            }
        });
        scene.setOnKeyReleased(e -> pressedKeys.remove(e.getCode()));

        if (timer != null) timer.stop();
        lastUpdate = 0;
        accumulator = 0;

        timer = new AnimationTimer() {
            @Override
            public void handle(long now) {
                gameTick(1.0);
                drawGame(g);   // ← ONE LINE

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
        stage.setScene(scene);
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
    public boolean cityPigeonUnlocked = false;
    public boolean noirPigeonUnlocked = false;
    public boolean isTrialMode = false;
    public boolean eagleSkinUnlocked = false; // Sky King Eagle skin

    public static final int SKIN_COUNT = 2; // City Pigeon + Noir Pigeon

    // === EPISODES ===
    private boolean storyModeActive = false;
    private boolean storyReplayMode = false;
    private int storyChapterIndex = 0;
    private int pigeonEpisodeUnlockedChapters = 1;
    private boolean pigeonEpisodeCompleted = false;
    private static final String PIGEON_EPISODE_TITLE = "Episode 1: Rise Of The Rooftop Pigeon";
    private static final String PIGEON_EPISODE_AWARD = "Noir Pigeon Skin";

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

    public void unlockAchievement(int index, String title) {
        achievementsUnlocked[index] = true;
        addToKillFeed("ACHIEVEMENT UNLOCKED: " + title);
        shakeIntensity = 30;
        hitstopFrames = 20;
        saveAchievements(); // save immediately!
    }

    public enum BirdType {
        PIGEON("Pigeon", 7, 16, 3.9, Color.LIGHTGRAY, 0.0, "Double Jump + Heal Burst"),
        EAGLE("Eagle", 9, 19, 4.2, Color.DARKRED, 0.6, "Soar (Hold Jump) + Dive Bomb"),
        HUMMINGBIRD("Hummingbird", 6, 23, 5.0, Color.LIME, 0.85, "Hover/Fly + Flutter Boost"),
        TURKEY("Turkey", 10, 10, 3.0, Color.SADDLEBROWN, 0.0, "Ground Pound AOE"),
        PENGUIN("Penguin", 8, 9, 3.6, Color.BLACK, 0.0, "Ice Slide Dash"),
        SHOEBILL("Shoebill", 10, 12, 3.7, Color.DARKSLATEBLUE, 0.3, "AoE Stun"),
        MOCKINGBIRD("Charles", 5, 18, 4.0, Color.MEDIUMPURPLE, 0.4, "Spawn Lounge (Heal zone)"),
        RAZORBILL("Razorbill", 8, 12, 3.6, Color.INDIGO, 0.25, "Razor Dive + Blade Storm"),
        GRINCHHAWK("Grinch-Hawk", 10, 10, 2.8, Color.rgb(102, 153, 0), 0.0, "Steal HP from everyone"),
        VULTURE("Vulture", 7, 14, 3.1, Color.rgb(45, 25, 55), 0.2, "Summon Crows + Feast"),
        OPIUMBIRD("Opium Bird", 7, 19, 4.4, Color.rgb(138, 43, 226), 0.7, "Lean Cloud (DoT + Slow)"),
        TITMOUSE("Tufted Titmouse", 6, 21, 5.4, Color.SLATEGRAY, 0.9, "Zip Dash"),
        PELICAN("Pelican", 11, 9, 2.9, Color.rgb(245, 220, 180), 0.0, "Pelican Plunge");

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

            spawnPowerUp(System.nanoTime());
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
                if (b == null || b.health <= 0 || (c.owner != null && c.owner == b)) continue;
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

        if (!matchEnded && matchTimer <= 0 && !suddenDeathActive) {
            suddenDeathActive = true;
            if (hugewaveClip != null) hugewaveClip.play();
            addToKillFeed("SUDDEN DEATH! A MURDER OF CROWS DESCENDS!");
            shakeIntensity = 40;
            hitstopFrames = 30;
        }
        if (suddenDeathActive && !matchEnded) {
            int sdFrames = 3600 - matchTimer;
            double sdSeconds = sdFrames / 60.0;
            double baseInterval = 60;
            double interval = Math.max(12, baseInterval - sdSeconds * 2.5);
            int crowsPerWave = 2 + (int) (sdSeconds / 10);
            if (sdFrames % (int) interval == 0) {
                for (int i = 0; i < crowsPerWave; i++) {
                    double y = 200 + random.nextDouble() * (WORLD_HEIGHT - 800);
                    double spd = 4.0 + sdSeconds * 0.2;
                    CrowMinion left = new CrowMinion(-100, y, null);
                    left.vx = spd + random.nextDouble() * 2;
                    left.vy = (random.nextDouble() - 0.5) * 4;
                    crowMinions.add(left);
                    CrowMinion right = new CrowMinion(WORLD_WIDTH + 100, y, null);
                    right.vx = -spd - random.nextDouble() * 2;
                    right.vy = (random.nextDouble() - 0.5) * 4;
                    crowMinions.add(right);
                }
                shakeIntensity = Math.max(shakeIntensity, 15);
            }
        }

        if (selectedMap == MapType.CITY && System.nanoTime() - lastWindBurstTime > WIND_BURST_INTERVAL) {
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
        for (Bird b : players) {
            if (b != null && b.health > 0) {
                alive++;
                winner = b;
            }
        }
        if (alive <= 1 && !matchEnded) {
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
                    showMatchSummary(finalStage, finalWinner);
                    if (finalWinner != null && finalWinner.health < 20 && finalWinner.health > 0 && !achievementsUnlocked[9]) {
                        unlockAchievement(9, "CLUTCH GOD!");
                    }
                }
            }.start();
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
            particles.subList(0, particles.size() - 2500).clear();  // ← changed from 2000 to keep more but cap at ~500 active
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
        stage.setTitle("Bird Game 3 - Power-Up Chaos!");
        stage.setResizable(false);
        stage.centerOnScreen();
        loadAchievements();
        loadSounds();
        showMenu(stage);
    }

    private void showMenu(Stage stage) {
        storyModeActive = false;
        storyReplayMode = false;
        if (musicPlayer != null) musicPlayer.stop();
        if (victoryMusicPlayer != null) victoryMusicPlayer.stop();
        playMenuMusic();
        VBox root = new VBox(30);
        root.setAlignment(Pos.CENTER);
        root.setPadding(new Insets(40));
        root.setStyle("-fx-background-color: linear-gradient(to bottom, #87CEEB, #98D8E9);");

        Label title = new Label("BIRD GAME 3");
        title.setFont(Font.font("Arial Black", 100));
        title.setTextFill(Color.WHITE);

        HBox countBox = new HBox(40);
        countBox.setAlignment(Pos.CENTER);

        for (int i = 1; i <= 4; i++) {
            int n = i;
            Button b = new Button(n + " PLAYER" + (n > 1 ? "S" : ""));
            b.setPrefSize(220, 90);
            b.setFont(Font.font("Arial Black", 40));
            b.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white; -fx-background-radius: 30;");
            b.setOnAction(e -> {
                playButtonClick();
                activePlayers = n;
                for (int j = 0; j < 4; j++) playerSlots[j].setVisible(j < n);
            });
            countBox.getChildren().add(b);
        }

        // === STORY / ACHIEVEMENTS / TRIALS ===
        HBox menuButtons = new HBox(60);
        menuButtons.setAlignment(Pos.CENTER);

        Button storyBtn = new Button("EPISODES");
        storyBtn.setPrefSize(500, 100);
        storyBtn.setFont(Font.font("Arial Black", 56));
        storyBtn.setStyle("-fx-background-color: #1565C0; -fx-text-fill: white; -fx-background-radius: 50;");
        storyBtn.setOnAction(e -> {
            playButtonClick();
            showEpisodesHub(stage);
        });

        Button achievementsBtn = new Button("ACHIEVEMENTS");
        achievementsBtn.setPrefSize(500, 100);
        achievementsBtn.setFont(Font.font("Arial Black", 60));
        achievementsBtn.setStyle("-fx-background-color: #9C27B0; -fx-text-fill: white; -fx-background-radius: 50;");
        achievementsBtn.setOnAction(e -> {
            playButtonClick();
            showAchievements(stage);
        });

        Button trialsBtn = new Button("TRIALS");
        trialsBtn.setPrefSize(500, 100);
        trialsBtn.setFont(Font.font("Arial Black", 60));
        trialsBtn.setStyle("-fx-background-color: #FF5722; -fx-text-fill: white; -fx-background-radius: 50;");
        trialsBtn.setOnAction(e -> {
            playButtonClick();
            showTrials(stage);
        });

        menuButtons.getChildren().addAll(storyBtn, achievementsBtn, trialsBtn);
        root.getChildren().add(menuButtons);

        playerSlots = new VBox[4];
        for (int i = 0; i < 4; i++) {
            playerSlots[i] = createPlayerSlot(i);
            playerSlots[i].setVisible(i < 2);
        }

        GridPane grid = new GridPane();
        grid.setHgap(80);
        grid.setVgap(50);
        grid.setAlignment(Pos.CENTER);
        grid.add(playerSlots[0], 0, 0);
        grid.add(playerSlots[1], 1, 0);
        grid.add(playerSlots[2], 0, 1);
        grid.add(playerSlots[3], 1, 1);

        Button fight = new Button("FLY INTO BATTLE!");
        fight.setPrefSize(1000, 180);
        fight.setFont(Font.font("Arial Black", 90));
        fight.setStyle("-fx-background-color: #FF1744; -fx-text-fill: white; -fx-background-radius: 100;");
        fight.setOnAction(e -> {
            playButtonClick();
            showStageSelect(stage);
        });

        root.getChildren().addAll(title, countBox, grid, fight);

        ScrollPane scroll = new ScrollPane(root);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background-color: transparent;");

        Scene scene = new Scene(scroll, WIDTH, HEIGHT);
        scene.getRoot().setStyle(scene.getRoot().getStyle() + ";-fx-focus-color:transparent;-fx-faint-focus-color:transparent;");
        setupKeyboardNavigation(scene);
        applyConsoleHighlight(scene);
        stage.setScene(scene);
        countBox.getChildren().get(1).requestFocus(); // 2 PLAYERS

        stage.show();
    }

    private void showEpisodesHub(Stage stage) {
        storyModeActive = true;
        storyReplayMode = false;
        playMenuMusic();

        VBox root = new VBox(30);
        root.setAlignment(Pos.CENTER);
        root.setPadding(new Insets(50));
        root.setStyle("-fx-background-color: linear-gradient(to bottom, #081122, #1F355E);");

        Label title = new Label("EPISODES");
        title.setFont(Font.font("Arial Black", FontWeight.BOLD, 96));
        title.setTextFill(Color.GOLD);

        VBox episodeCard = new VBox(18);
        episodeCard.setAlignment(Pos.CENTER_LEFT);
        episodeCard.setPadding(new Insets(35));
        episodeCard.setMaxWidth(1400);
        episodeCard.setStyle("-fx-background-color: rgba(0,0,0,0.55); -fx-background-radius: 24; -fx-border-color: #4FC3F7; -fx-border-width: 4; -fx-border-radius: 24;");

        Label epTitle = new Label(PIGEON_EPISODE_TITLE);
        epTitle.setFont(Font.font("Arial Black", 52));
        epTitle.setTextFill(Color.WHITE);

        int clampedUnlocked = Math.max(1, Math.min(pigeonEpisodeUnlockedChapters, storyChapters.length));
        String status = pigeonEpisodeCompleted
                ? "Completed"
                : ("Unlocked Chapters: " + clampedUnlocked + "/" + storyChapters.length);
        Label epStatus = new Label(status);
        epStatus.setFont(Font.font("Consolas", 34));
        epStatus.setTextFill(pigeonEpisodeCompleted ? Color.LIMEGREEN : Color.ORANGE);

        Label epReward = new Label("Episode Award: " + PIGEON_EPISODE_AWARD + (noirPigeonUnlocked ? " (Unlocked)" : " (Locked)"));
        epReward.setFont(Font.font("Consolas", 30));
        epReward.setTextFill(noirPigeonUnlocked ? Color.GOLD : Color.LIGHTGRAY);

        HBox actions = new HBox(24);
        actions.setAlignment(Pos.CENTER_LEFT);

        Button continueEpisode = new Button(pigeonEpisodeCompleted ? "REPLAY EPISODE" : "CONTINUE EPISODE");
        continueEpisode.setPrefSize(460, 110);
        continueEpisode.setFont(Font.font("Arial Black", 40));
        continueEpisode.setStyle("-fx-background-color: #00C853; -fx-text-fill: white; -fx-background-radius: 28;");
        continueEpisode.setOnAction(e -> {
            playButtonClick();
            resetMatchStats();
            storyReplayMode = false;
            storyChapterIndex = Math.max(0, Math.min(pigeonEpisodeUnlockedChapters - 1, storyChapters.length - 1));
            showStoryDialogue(stage,
                    "Episode Briefing",
                    "Narrator",
                    "You are the Pigeon. Keep pushing through each chapter. Win to unlock the next fight.",
                    () -> showCurrentStoryChapterIntro(stage));
        });

        Button chapterSelect = new Button("CHAPTER SELECT");
        chapterSelect.setPrefSize(460, 110);
        chapterSelect.setFont(Font.font("Arial Black", 40));
        chapterSelect.setStyle("-fx-background-color: #1976D2; -fx-text-fill: white; -fx-background-radius: 28;");
        chapterSelect.setOnAction(e -> {
            playButtonClick();
            showEpisodeChapterSelect(stage);
        });

        Button menuButton = new Button("MAIN MENU");
        menuButton.setPrefSize(460, 110);
        menuButton.setFont(Font.font("Arial Black", 40));
        menuButton.setStyle("-fx-background-color: #FF1744; -fx-text-fill: white; -fx-background-radius: 28;");
        menuButton.setOnAction(e -> {
            playButtonClick();
            showMenu(stage);
        });

        actions.getChildren().addAll(continueEpisode, chapterSelect, menuButton);
        episodeCard.getChildren().addAll(epTitle, epStatus, epReward, actions);
        root.getChildren().addAll(title, episodeCard);

        Scene scene = new Scene(root, WIDTH, HEIGHT);
        setupKeyboardNavigation(scene);
        applyConsoleHighlight(scene);
        stage.setScene(scene);
        continueEpisode.requestFocus();
    }

    private void showEpisodeChapterSelect(Stage stage) {
        playMenuMusic();
        VBox root = new VBox(30);
        root.setAlignment(Pos.CENTER);
        root.setPadding(new Insets(50));
        root.setStyle("-fx-background-color: linear-gradient(to bottom, #0A1530, #1B2C52);");

        Label title = new Label("EPISODE CHAPTERS");
        title.setFont(Font.font("Arial Black", FontWeight.BOLD, 84));
        title.setTextFill(Color.CYAN.brighter());

        VBox chapterList = new VBox(18);
        chapterList.setAlignment(Pos.CENTER);
        int unlocked = Math.max(1, Math.min(pigeonEpisodeUnlockedChapters, storyChapters.length));

        for (int i = 0; i < storyChapters.length; i++) {
            StoryChapter chapter = storyChapters[i];
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

        Button back = new Button("BACK TO EPISODES");
        back.setPrefSize(700, 105);
        back.setFont(Font.font("Arial Black", 42));
        back.setStyle("-fx-background-color: #FF1744; -fx-text-fill: white; -fx-background-radius: 22;");
        back.setOnAction(e -> {
            playButtonClick();
            showEpisodesHub(stage);
        });

        root.getChildren().addAll(title, chapterList, back);
        Scene scene = new Scene(root, WIDTH, HEIGHT);
        setupKeyboardNavigation(scene);
        applyConsoleHighlight(scene);
        stage.setScene(scene);
        back.requestFocus();
    }

    private void showCurrentStoryChapterIntro(Stage stage) {
        if (!storyModeActive) return;
        if (storyChapterIndex < 0 || storyChapterIndex >= storyChapters.length) {
            showEpisodesHub(stage);
            return;
        }
        StoryChapter chapter = storyChapters[storyChapterIndex];
        showStoryDialogue(stage, chapter.title, chapter.speaker, chapter.dialogue, () -> startStoryBattle(stage));
    }

    private void startStoryBattle(Stage stage) {
        if (!storyModeActive) return;
        StoryChapter chapter = storyChapters[storyChapterIndex];
        selectedMap = chapter.map;
        activePlayers = 2;
        startMatch(stage);
    }

    private void handleStoryMatchEnd(Stage stage, Bird winner) {
        boolean playerWon = winner != null && winner.playerIndex == 0;
        if (!playerWon) {
            showStoryDialogue(stage,
                    "Defeat",
                    "Narrator",
                    "You were knocked out in " + storyChapters[storyChapterIndex].title + ".",
                    () -> startStoryBattle(stage));
            return;
        }

        if (!storyReplayMode && storyChapterIndex == pigeonEpisodeUnlockedChapters - 1 && pigeonEpisodeUnlockedChapters < storyChapters.length) {
            pigeonEpisodeUnlockedChapters++;
            saveAchievements();
        }

        if (storyChapterIndex >= storyChapters.length - 1) {
            if (!pigeonEpisodeCompleted) {
                pigeonEpisodeCompleted = true;
                noirPigeonUnlocked = true;
                saveAchievements();
                showStoryDialogue(stage,
                        "Episode Complete",
                        "Narrator",
                        "Episode clear. Award unlocked: " + PIGEON_EPISODE_AWARD + ".\nSelect Pigeon to cycle skins.",
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
        StoryChapter next = storyChapters[storyChapterIndex];
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

        Button continueButton = new Button("CONTINUE");
        continueButton.setPrefSize(460, 120);
        continueButton.setFont(Font.font("Arial Black", 52));
        continueButton.setStyle("-fx-background-color: #00C853; -fx-text-fill: white; -fx-background-radius: 32;");
        continueButton.setOnAction(e -> {
            playButtonClick();
            onContinue.run();
        });

        Button menuButton = new Button("MAIN MENU");
        menuButton.setPrefSize(460, 120);
        menuButton.setFont(Font.font("Arial Black", 52));
        menuButton.setStyle("-fx-background-color: #FF1744; -fx-text-fill: white; -fx-background-radius: 32;");
        menuButton.setOnAction(e -> {
            playButtonClick();
            showMenu(stage);
        });

        buttons.getChildren().addAll(continueButton, menuButton);
        root.getChildren().addAll(title, card, buttons);

        Scene scene = new Scene(root, WIDTH, HEIGHT);
        setupKeyboardNavigation(scene);
        applyConsoleHighlight(scene);
        stage.setScene(scene);
        continueButton.requestFocus();
    }

    private void showStageSelect(Stage stage) {
        VBox root = new VBox(40);
        root.setAlignment(Pos.CENTER);
        root.setPadding(new Insets(60));
        root.setStyle("-fx-background-color: linear-gradient(to bottom, #000033, #000066);");

        Label title = new Label("SELECT STAGE");
        title.setFont(Font.font("Arial Black", 100));
        title.setTextFill(Color.CYAN);
        title.setEffect(new Glow(0.8));

        VBox options = new VBox(25);           // bigger gap now that no subtext
        options.setAlignment(Pos.CENTER);
        options.setMaxWidth(1400);

        // Clean big buttons only — no subtext
        Button forestBtn = new Button("BIG FOREST");
        forestBtn.setPrefSize(1400, 140);
        forestBtn.setFont(Font.font("Arial Black", 65));
        forestBtn.setStyle("-fx-background-color: #228B22; -fx-text-fill: white; -fx-background-radius: 25;");
        forestBtn.setOnAction(e -> {playButtonClick(); selectedMap = MapType.FOREST; startMatch(stage); });

        Button randomBtn = new Button("RANDOM");
        randomBtn.setPrefSize(1400, 140);
        randomBtn.setFont(Font.font("Arial Black", 65));
        randomBtn.setStyle("-fx-background-color: #9C27B0; -fx-text-fill: white; -fx-background-radius: 25;");
        randomBtn.setOnAction(e -> {
            playButtonClick();
            MapType[] maps = MapType.values();
            selectedMap = maps[random.nextInt(maps.length)];
            startMatch(stage);
        });

        Button cityBtn = new Button("PIGEON'S ROOFTOPS");
        cityBtn.setPrefSize(1400, 140);
        cityBtn.setFont(Font.font("Arial Black", 65));
        cityBtn.setStyle("-fx-background-color: #4B0082; -fx-text-fill: white; -fx-background-radius: 25;");
        cityBtn.setOnAction(e -> {playButtonClick(); selectedMap = MapType.CITY; startMatch(stage); });

        Button cliffsBtn = new Button("SKY CLIFFS");
        cliffsBtn.setPrefSize(1400, 140);
        cliffsBtn.setFont(Font.font("Arial Black", 65));
        cliffsBtn.setStyle("-fx-background-color: #8B4513; -fx-text-fill: white; -fx-background-radius: 25;");
        cliffsBtn.setOnAction(e -> {playButtonClick(); selectedMap = MapType.SKYCLIFFS; startMatch(stage); });

        Button jungleBtn = new Button("VIBRANT JUNGLE");
        jungleBtn.setPrefSize(1400, 140);
        jungleBtn.setFont(Font.font("Arial Black", 65));
        jungleBtn.setStyle("-fx-background-color: #228B22; -fx-text-fill: white; -fx-background-radius: 25;");
        jungleBtn.setOnAction(e -> {playButtonClick(); selectedMap = MapType.VIBRANT_JUNGLE; startMatch(stage); });

        Button back = new Button("BACK");
        back.setPrefSize(1400, 140);
        back.setFont(Font.font("Arial Black", 65));
        back.setStyle("-fx-background-color: #FF1744; -fx-text-fill: white; -fx-background-radius: 25;");
        back.setOnAction(e ->{
            playButtonClick();
            showMenu(stage);
        });

        options.getChildren().addAll(forestBtn, randomBtn, cityBtn, cliffsBtn, jungleBtn, back);

        root.getChildren().addAll(title, options);

        Scene scene = new Scene(root, WIDTH, HEIGHT);
        scene.getRoot().setStyle(scene.getRoot().getStyle() + ";-fx-focus-color:transparent;-fx-faint-focus-color:transparent;");
        setupKeyboardNavigation(scene);
        applyConsoleHighlight(scene);
        stage.setScene(scene);
        forestBtn.requestFocus();
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

        VBox selector = createBirdSelector(BirdType.values()[idx % BirdType.values().length]);
        selector.setUserData(BirdType.values()[idx % BirdType.values().length]);

        box.getChildren().addAll(title, aiButton, selector);
        return box;
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
        statsPane.setPrefSize(380, 160);
        statsPane.setStyle("-fx-background: black;");
        Label stats = new Label("Power: " + def.power + " | Speed: " + String.format("%.1f", def.speed) + " | Jump: " + def.jumpHeight + "\n\nSPECIAL: " + def.ability);
        stats.setFont(Font.font("Consolas", 18));
        stats.setTextFill(Color.WHITE);
        stats.setWrapText(true);
        statsPane.setContent(stats);

        GridPane grid = new GridPane();
        grid.setHgap(15);
        grid.setVgap(15);
        int col = 0, row = 0;

        for (BirdType bt : BirdType.values()) {
            Button b = new Button(bt.name);
            b.setPrefSize(160, 70);
            b.setFont(Font.font(16));
            String baseStyle = "-fx-background-color: " + toHex(bt.color) + "; -fx-text-fill: white; -fx-font-weight: bold;";
            b.setStyle(baseStyle);
            b.setUserData(bt);

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
                        // Switch back to normal Eagle
                        selected.setText("Selected: Eagle");
                        box.setUserData(BirdType.EAGLE);
                        b.setStyle(baseStyle);
                    } else {
                        // Switch to Sky King skin
                        selected.setText("Selected: Sky King Eagle (Skin)");
                        box.setUserData("SKY_KING_EAGLE");
                        b.setStyle("-fx-background-color: #FFD700; -fx-text-fill: black; -fx-font-weight: bold;");
                    }
                }
                // === NORMAL BIRDS ===
                else {
                    selected.setText("Selected: " + bt.name);
                    box.setUserData(bt);
                    b.setStyle(baseStyle);
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
            BirdType r = BirdType.values()[(int) (Math.random() * BirdType.values().length)];
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

    private void spawnPowerUp(long now) {
        if (now - lastPowerUpSpawnTime < POWERUP_SPAWN_INTERVAL) return;
        if (random.nextDouble() < 0.8) {
            double x = 200 + random.nextDouble() * (WORLD_WIDTH - 400);
            double y = 300 + random.nextDouble() * (WORLD_HEIGHT - 800);
            PowerUpType type;
            if (selectedMap == MapType.SKYCLIFFS && random.nextDouble() < 0.35) {
                type = PowerUpType.THERMAL;
            } else if (selectedMap == MapType.CITY && random.nextDouble() < 0.4) {
                type = PowerUpType.NEON;
            } else if (selectedMap == MapType.VIBRANT_JUNGLE && random.nextDouble() < 0.4) {
                type = PowerUpType.VINE_GRAPPLE;                                    // NEW
            } else {
                PowerUpType[] common = {PowerUpType.HEALTH, PowerUpType.SPEED, PowerUpType.RAGE, PowerUpType.SHRINK};
                type = common[random.nextInt(common.length)];
            }
            powerUps.add(new PowerUp(x, y, type));
            lastPowerUpSpawnTime = now;
        } else {
            lastPowerUpSpawnTime = now;
        }
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
        matchTimer = 90 * 60;
        this.currentStage = stage;
        Arrays.fill(scores, 0);
        Arrays.fill(players, null);
        if (storyModeActive) {
            StoryChapter chapter = storyChapters[storyChapterIndex];
            activePlayers = 2;

            players[0] = new Bird(1200, BirdType.PIGEON, 0, this);
            players[0].name = "You: Pigeon";
            players[0].health = 100;
            isAI[0] = false;

            players[1] = new Bird(4200, chapter.opponentType, 1, this);
            players[1].name = chapter.opponentName;
            players[1].health = chapter.opponentHealth;
            players[1].powerMultiplier = chapter.opponentPowerMult;
            players[1].speedMultiplier = chapter.opponentSpeedMult;
            isAI[1] = true;
        } else {
            for (int i = 0; i < activePlayers; i++) {
                VBox selectorBox = (VBox) playerSlots[i].getChildren().get(2);
                Object data = selectorBox.getUserData();

                BirdType type;
                boolean useCitySkin = false;
                boolean useNoirSkin = false;

                if (data instanceof String) {
                    if ("CITY_PIGEON".equals(data)) {
                        type = BirdType.PIGEON;
                        useCitySkin = true;
                    } else if ("NOIR_PIGEON".equals(data)) {
                        type = BirdType.PIGEON;
                        useNoirSkin = true;
                    } else if ("SKY_KING_EAGLE".equals(data)) {
                        type = BirdType.EAGLE;
                    } else {
                        type = BirdType.PIGEON;
                    }
                } else {
                    type = (BirdType) data;
                }

                double startX = 300 + i * (WIDTH - 600) / Math.max(1, activePlayers - 1);
                players[i] = new Bird(startX, type, i, this);

                if (useCitySkin) {
                    players[i].isCitySkin = true;
                }
                if (useNoirSkin) {
                    players[i].isNoirSkin = true;
                }
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

            // NO random floating pillars/antennas — removed completely

            // Rooftop vents already added in the building loop above.
        }

        Canvas canvas = new Canvas(WIDTH, HEIGHT);
        GraphicsContext g = canvas.getGraphicsContext2D();
        StackPane root = new StackPane(canvas);
        gameRoot = root;
        root.setStyle(selectedMap == MapType.FOREST
                ? "-fx-background-color: linear-gradient(to bottom, #87CEEB 0%, #B3E5FC 50%, #E0F2F1 100%);"
                : "-fx-background-color: #000011;");
        Scene scene = new Scene(root, WIDTH, HEIGHT);
        uiCanvas = new Canvas(WIDTH, HEIGHT);
        ui = uiCanvas.getGraphicsContext2D();
        root.getChildren().add(uiCanvas);

        pressedKeys.clear();
        scene.setOnKeyPressed(e -> {
            pressedKeys.add(e.getCode());
            if (e.getCode() == KeyCode.ESCAPE) {
                togglePause(stage);
            }
        });
        scene.setOnKeyReleased(e -> pressedKeys.remove(e.getCode()));

        if (timer != null) timer.stop();
        lastUpdate = 0;
        accumulator = 0;

        timer = new AnimationTimer() {
            @Override
            public void handle(long now) {
                gameTick(1.0);
                drawGame(g);   // ← ONE LINE

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

                for (int i = 0; i < activePlayers; i++) {
                    if (players[i] != null && players[i].health > 0) {
                        drawHealthBar(ui, players[i], startX + i * (barWidth + gap), 80);
                    }
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
                ui.fillText("Controls: WASD+Space+Shift | Q/E=Taunt | P2=Arrows+Enter+Down | P3=TFGH+Y+U | P4=IJKL+O+P | S/Backslash/G/K=Block", 50, HEIGHT - 35);
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
        stage.setScene(scene);
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
        g.setFill(Color.LIME);
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

        HBox buttons = new HBox(100);
        buttons.setAlignment(Pos.CENTER);
        if (storyModeActive) {
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
                startMatch(stage);
            });
            Button menu = button("MAIN MENU", "#9C27B0");
            menu.setOnAction(e -> {
                resetMatchStats();
                showMenu(stage);
            });
            buttons.getChildren().addAll(rematch, menu);
        }

        if (winner != null && selectedMap == MapType.CITY) {
            cityWins[winner.playerIndex]++;
            if (cityWins[winner.playerIndex] >= 5 && !achievementsUnlocked[12]) {
                unlockAchievement(12, "URBAN KING!");
            }
        }

        if (winner != null && selectedMap == MapType.SKYCLIFFS) {
            cliffWins[winner.playerIndex]++;
            if (cliffWins[winner.playerIndex] >= 5 && !achievementsUnlocked[15]) {
                unlockAchievement(15, "SKY EMPEROR!");
            }
        }

        if (winner != null && selectedMap == MapType.VIBRANT_JUNGLE) {
            jungleWins[winner.playerIndex]++;
            if (jungleWins[winner.playerIndex] >= 5 && !achievementsUnlocked[17]) {
                unlockAchievement(17, "CANOPY KING!");
            }
        }

        root.getChildren().addAll(title, podium, buttons);
        stage.setScene(new Scene(root, WIDTH, HEIGHT));
    }

    private Label labelFor(Bird b, String stat) {
        return new Label(b != null ? b.name + " (" + stat + ")" : "-");
    }

    private Button button(String text, String color) {
        Button b = new Button(text);
        b.setPrefSize(460, 130);
        b.setFont(Font.font("Arial Black", 64));
        b.setStyle("-fx-background-color: " + color + "; -fx-text-fill: white; -fx-background-radius: 40;");
        return b;
    }

    private void resetMatchStats() {
        Arrays.fill(falls, 0);
        Arrays.fill(damageDealt, 0);
        Arrays.fill(eliminations, 0);
        Arrays.fill(groundPounds, 0);
        Arrays.fill(loungeTime, 0);
        Arrays.fill(leanTime, 0);
        Arrays.fill(tauntsPerformed, 0);
        killFeed.clear();
        matchEnded = false;
        matchTimer = 90 * 60;
        suddenDeathActive = false;
        Arrays.fill(thermalPickups, 0);
        Arrays.fill(highCliffJumps, 0);
        Arrays.fill(cliffWins, 0);
        Arrays.fill(vineGrapplePickups, 0);
        Arrays.fill(jungleWins, 0);
    }

    private void loadAchievements() {
        Preferences prefs = Preferences.userNodeForPackage(BirdGame3.class);
        for (int i = 0; i < ACHIEVEMENT_COUNT; i++) {
            achievementsUnlocked[i] = prefs.getBoolean("ach_" + i, false);
            achievementProgress[i] = prefs.getInt("prog_" + i, 0);
        }
        cityPigeonUnlocked = prefs.getBoolean("skin_citypigeon", false);
        noirPigeonUnlocked = prefs.getBoolean("skin_noirpigeon", false);
        eagleSkinUnlocked = prefs.getBoolean("skin_eagle", false);
        pigeonEpisodeUnlockedChapters = Math.max(1, prefs.getInt("ep_pigeon_unlocked", 1));
        pigeonEpisodeCompleted = prefs.getBoolean("ep_pigeon_completed", false);
        if (pigeonEpisodeCompleted) {
            pigeonEpisodeUnlockedChapters = storyChapters.length;
            noirPigeonUnlocked = true;
        }
        for (int i = 0; i < 4; i++) {
            vineGrapplePickups[i] = prefs.getInt("vine_pickups_" + i, 0);
            jungleWins[i] = prefs.getInt("jungle_wins_" + i, 0);
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
        prefs.putInt("ep_pigeon_unlocked", pigeonEpisodeUnlockedChapters);
        prefs.putBoolean("ep_pigeon_completed", pigeonEpisodeCompleted);
        for (int i = 0; i < 4; i++) {
            prefs.putInt("vine_pickups_" + i, vineGrapplePickups[i]);
            prefs.putInt("jungle_wins_" + i, jungleWins[i]);
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

        // Lean God – 5 minutes total in lean cloud
        if (bird.type == BirdType.OPIUMBIRD) {
            achievementProgress[4] += leanTime[idx];
            if (achievementProgress[4] >= 18000 && !achievementsUnlocked[4]) { // 300 seconds = 18000 frames @ 60fps
                unlockAchievement(4, "LEAN GOD!");
            }
        }

        // Lounge Lizard – heal 100 HP total
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

            String status = achievementsUnlocked[i] ? " ✓ UNLOCKED" : " (Locked)";
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

        stage.setScene(scene);
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

        String statusText = cityPigeonUnlocked ? "✓ COMPLETED (Replayable)" : "Defeat the City Pigeon boss with your 2 allies";
        Label status = new Label(statusText);
        status.setFont(Font.font("Consolas", 32));
        status.setTextFill(cityPigeonUnlocked ? Color.LIMEGREEN : Color.ORANGE);

        Label reward = new Label("Reward: City Pigeon Alternate Skin\n(Fedora + Cigarette)");
        reward.setFont(Font.font("Consolas", 28));
        reward.setTextFill(Color.GOLD);
        reward.setAlignment(Pos.CENTER);

        Button playBtn = new Button(cityPigeonUnlocked ? "REPLAY TRIAL" : "PLAY TRIAL");
        playBtn.setPrefSize(500, 120);
        playBtn.setFont(Font.font("Arial Black", 50));
        playBtn.setStyle("-fx-background-color: #FF1744; -fx-text-fill: white; -fx-background-radius: 40;");
        playBtn.setOnAction(e -> startPigeonTrial(stage));

        trialBox.getChildren().addAll(trialName, status, reward, playBtn);

        Button back = new Button("BACK");
        back.setPrefSize(400, 100);
        back.setFont(Font.font("Arial Black", 60));
        back.setStyle("-fx-background-color: #444444; -fx-text-fill: white; -fx-background-radius: 50;");
        back.setOnAction(e -> showMenu(stage));

        root.getChildren().addAll(title, trialBox, back);

        stage.setScene(new Scene(root, WIDTH, HEIGHT));

        VBox eagleTrialBox = new VBox(20);
        eagleTrialBox.setAlignment(Pos.CENTER);
        eagleTrialBox.setPadding(new Insets(40));
        eagleTrialBox.setStyle("-fx-background-color: rgba(0,0,0,0.6); -fx-background-radius: 30; -fx-border-color: orange; -fx-border-width: 4; -fx-border-radius: 30;");

        Label eagleName = new Label("SKY TYRANT TRIAL");
        eagleName.setFont(Font.font("Arial Black", 50));
        eagleName.setTextFill(Color.WHITE);

        String eagleStatus = eagleSkinUnlocked ? "✓ COMPLETED (Replayable)" : "Defeat the Sky Tyrant with your allies";
        Label eagleStat = new Label(eagleStatus);
        eagleStat.setFont(Font.font("Consolas", 32));
        eagleStat.setTextFill(eagleSkinUnlocked ? Color.LIMEGREEN : Color.ORANGE);

        Label eagleReward = new Label("Reward: Sky King Eagle Skin\n(Golden feathers + crown glow)");
        eagleReward.setFont(Font.font("Consolas", 28));
        eagleReward.setTextFill(Color.GOLD);
        eagleReward.setAlignment(Pos.CENTER);

        Button eagleBtn = new Button(eagleSkinUnlocked ? "REPLAY TRIAL" : "PLAY TRIAL");
        eagleBtn.setPrefSize(500, 120);
        eagleBtn.setFont(Font.font("Arial Black", 50));
        eagleBtn.setStyle("-fx-background-color: #FF5722; -fx-text-fill: white; -fx-background-radius: 40;");
        eagleBtn.setOnAction(e -> startEagleTrial(stage));

        eagleTrialBox.getChildren().addAll(eagleName, eagleStat, eagleReward, eagleBtn);

        root.getChildren().add(eagleTrialBox);

        Scene scene = new Scene(root, WIDTH, HEIGHT);
        // Disable default blue mouse focus ring — only our yellow highlight shows
        scene.getRoot().setStyle(scene.getRoot().getStyle() + ";-fx-focus-color:transparent;-fx-faint-focus-color:transparent;");
        setupKeyboardNavigation(scene);
        applyConsoleHighlight(scene);
        stage.setScene(scene);
        back.requestFocus();
    }

    private void togglePause(Stage stage) {
        Scene scene = stage.getScene();
        if (isPaused) {
            // === RESUME ===
            scene.setOnKeyPressed(e -> {
                pressedKeys.add(e.getCode());
                if (e.getCode() == KeyCode.ESCAPE) togglePause(stage);
            });
            gameRoot.getChildren().removeIf(node -> node instanceof VBox && "pauseMenu".equals(node.getId()));

            lastUpdate = 0;      // ← CRITICAL: reset so no speed-up
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



