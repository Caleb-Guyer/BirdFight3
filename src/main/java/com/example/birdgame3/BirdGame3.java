package com.example.birdgame3;

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

    private static final int WIDTH = 1920;
    private static final int HEIGHT = 1080;
    private static final int GROUND_Y = 2400;
    private static final double GRAVITY = 0.75;

    private final Set<KeyCode> pressedKeys = new HashSet<>();
    private final Bird[] players = new Bird[4];
    private final boolean[] isAI = new boolean[4];
    private int activePlayers = 2;
    private AnimationTimer timer;
    private final List<Platform> platforms = new ArrayList<>();
    private final List<PowerUp> powerUps = new ArrayList<>();
    private final List<NectarNode> nectarNodes = new ArrayList<>();
    private final List<SwingingVine> swingingVines = new ArrayList<>();
    private final List<double[]> cityStars = new ArrayList<>();
    private VBox[] playerSlots;
    private final Random random = new Random();
    private long lastPowerUpSpawnTime = 0;
    private static final long POWERUP_SPAWN_INTERVAL = 1_000_000_000L * 8; // every 8 seconds
    private double shakeIntensity = 0;
    private double[] mountainPeaks = null; // will store fixed peak heights for Sky Cliffs
    private int hitstopFrames = 0;
    private final List<String> killFeed = new ArrayList<>();
    private final int MAX_FEED_LINES = 6;
    private static final double CEILING_Y = -100;
    private final int[] falls = new int[4];
    private final int[] scores = new int[4];  // live score: elims*50 + damage/2

    // === MATCH STATS TRACKING ===
    private final int[] damageDealt = new int[4];
    private final int[] eliminations = new int[4];
    private final int[] groundPounds = new int[4];     // Turkey special
    private final int[] loungeTime = new int[4];       // Mockingbird lounge active frames
    private final int[] leanTime = new int[4];         // Opium Bird lean cloud active frames
    private final int[] tauntsPerformed = new int[4];

    private final int[] vineGrapplePickups = new int[4];     // times picked up Vine Grapple
    private final int[] jungleWins = new int[4];             // wins on Vibrant Jungle map

    // City-specific stats tracking
    private final int[] rooftopJumps = new int[4];     // times a bird jumped off a high rooftop
    private final int[] neonPickups = new int[4];      // times a bird picked up the new "Neon" power-up
    private final int[] cityWins = new int[4];         // wins on city map (for achievement)

    // === SKYCLIFFS-specific stats tracking ===
    private final int[] thermalPickups = new int[4]; // times a bird picked up THERMAL power-up
    private final int[] highCliffJumps = new int[4]; // jumps from very high platforms (y < GROUND_Y - 1000)
    private final int[] cliffWins = new int[4];      // wins on Sky Cliffs map

    // === SUDDEN DEATH SYSTEM ===
    private int matchTimer = 90 * 60;           // 90 seconds at 60 FPS
    private boolean suddenDeathActive = false;

    private boolean matchEnded = false;  // prevents double-trigger

    private boolean isPaused = false;
    private boolean uiDirty = true;

    // === FIXED TIMESTEP PAUSE FIX ===
    private long lastUpdate = 0;
    private long accumulator = 0L;

    private StackPane gameRoot;
    private Stage currentStage;

    // === MAP + DYNAMIC CAMERA ===
    private static final double WORLD_WIDTH = 6000;
    private static final double WORLD_HEIGHT = 3000;

    // Camera state
    private double camX = 0, camY = 0;
    private double zoom = 1.0;           // 1.0 = 100%, 0.5 = zoomed out a lot
    private static final double MIN_ZOOM = 0.35;
    private static final double MAX_ZOOM = 1.5;
    private static final double ZOOM_SPEED = 0.008;

    // === MAPS ===
    private enum MapType { FOREST, CITY, SKYCLIFFS, VIBRANT_JUNGLE }

    private MapType selectedMap = MapType.FOREST; // default

    private final List<Particle> particles = new ArrayList<>();
    private final List<CrowMinion> crowMinions = new ArrayList<>();

    // === CITY WIND BURSTS ===
    private final List<WindVent> windVents = new ArrayList<>();
    private long lastWindBurstTime = 0;
    private static final long WIND_BURST_INTERVAL = 1_000_000_000L * 6; // every ~6 seconds
    private static final double WIND_FORCE = -28.0; // strong upward boost

    static class WindVent {
        double x, y, w;
        int cooldown = 0;

        WindVent(double x, double y, double w) {
            this.x = x;
            this.y = y;
            this.w = w;
        }
    }

    static class Platform {
        double x, y, w, h;
        String signText = null; // null = no sign
        Platform(double x, double y, double w, double h) {
            this.x = x;
            this.y = y;
            this.w = w;
            this.h = h;
        }
    }

    private class NectarNode {
        double x, y;
        boolean isSpeed; // true = speed boost, false = hover regen
        boolean active;

        NectarNode(double x, double y, boolean isSpeed) {
            this.x = x;
            this.y = y;
            this.isSpeed = isSpeed;
            this.active = true;
        }
    }

    private class SwingingVine {
        double baseX, baseY; // Anchor point at top
        double length; // Vine length
        double angle; // Current angle from vertical (radians)
        double platformX, platformY; // Platform at vine's end
        double platformW = 120, platformH = 40;
        double angularVelocity; // For swinging motion

        SwingingVine(double baseX, double baseY, double length) {
            this.baseX = baseX;
            this.baseY = baseY;
            this.length = length;
            this.angle = 0;
            this.angularVelocity = 0;
            updatePlatformPosition();
        }

        void updatePlatformPosition() {
            platformX = baseX + length * Math.sin(angle);
            platformY = baseY + length * Math.cos(angle);
        }
    }

    private void playHitSound(double intensity) {
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

        players[0] = new Bird(1200, BirdType.PIGEON, 0);
        players[0].y = GROUND_Y - 400;
        players[0].health = 100;
        players[0].name = "You: Pigeon";
        isAI[0] = false;

        for (int i = 1; i <= 2; i++) {
            players[i] = new Bird(1000 + i * 400, BirdType.PIGEON, i);
            players[i].y = GROUND_Y - 400;
            players[i].health = 100;
            players[i].name = "Ally: Pigeon";
            isAI[i] = true;
            players[i].isCitySkin = false;
        }

        players[3] = new Bird(4000, BirdType.PIGEON, 3);
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
        players[0] = new Bird(1200, BirdType.EAGLE, 0);
        players[0].y = GROUND_Y - 400;
        players[0].health = 100;
        players[0].name = "You: Eagle";
        isAI[0] = false;

        // 2 ally AI Eagles
        for (int i = 1; i <= 2; i++) {
            players[i] = new Bird(1000 + i * 400, BirdType.EAGLE, i);
            players[i].y = GROUND_Y - 400;
            players[i].health = 100;
            players[i].name = "Ally: Eagle";
            isAI[i] = true;
        }

        // BOSS: Sky Tyrant Eagle
        players[3] = new Bird(4000, BirdType.EAGLE, 3);
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

    private double flashAlpha = 0.0;     // white flash intensity
    private double redFlashAlpha = 0.0;  // red tint for normal big hits
    private int flashTimer = 0;          // frames remaining for flash

    private Canvas uiCanvas;
    private GraphicsContext ui;

    // === SOUND & MUSIC ===
    private AudioClip bonkClip, butterClip, jalapenoClip, swingClip, hugewaveClip, buttonClickClip, zombieFallingClip;
    private MediaPlayer musicPlayer;
    private MediaPlayer menuMusicPlayer;     // choose_your_seeds.mp3
    private MediaPlayer victoryMusicPlayer;  // finalfanfare.mp3

    private static final String[] ACHIEVEMENT_NAMES = {
            "First Blood", "Dominator", "Annihilator", "Turkey Slam Master", "Lean God", "Lounge Lizard",
            "Power-Up Hoarder", "Fall Guy", "Taunt Lord", "Clutch God",
            "Rooftop Runner", "Neon Addict", "Urban King",
            "Thermal Rider", "Cliff Diver", "Sky Emperor",
            "Vine Swinger", "Canopy King", "Pelican King"                                    // NEW JUNGLE ACHIEVEMENTS
    };

    private static final String[] ACHIEVEMENT_DESCRIPTIONS = {
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

    private static final int ACHIEVEMENT_COUNT = ACHIEVEMENT_NAMES.length;
    private final boolean[] achievementsUnlocked = new boolean[ACHIEVEMENT_COUNT];
    private final int[] achievementProgress = new int[ACHIEVEMENT_COUNT]; // for tracking partial progress

    // === SKIN UNLOCKS ===
    private boolean cityPigeonUnlocked = false;
    private boolean isTrialMode = false;
    private boolean eagleSkinUnlocked = false; // Sky King Eagle skin

    private static final int SKIN_COUNT = 1; // add more later if needed

    static class Particle {
        double x, y, vx, vy;
        Color color;
        int life = 60; // frames

        Particle(double x, double y, double vx, double vy, Color color) {
            this.x = x;
            this.y = y;
            this.vx = vx;
            this.vy = vy;
            this.color = color;
        }
    }

    static class CrowMinion {
        double x, y, vx, vy;
        int life = 1;
        Bird target;
        int age = 0;
        Bird owner = null;

        CrowMinion(double x, double y, Bird target) {
            this.x = x;
            this.y = y;
            this.target = target;
            this.vx = (Math.random() - 0.5) * 4;  // start with random drift
            this.vy = (Math.random() - 0.5) * 4;
            // they will pick a real target on the first update frame
        }
    }

    enum PowerUpType {
        HEALTH(Color.RED, "+40 HP"),
        SPEED(Color.CYAN, "SPEED!"),
        RAGE(Color.ORANGE, "RAGE!"),
        SHRINK(Color.PURPLE, "SHRINK!"),
        NEON(Color.MAGENTA.brighter(), "NEON BOOST!"),
        THERMAL(Color.GOLD.brighter(), "THERMAL RISE!"),
        VINE_GRAPPLE(Color.LIMEGREEN.brighter(), "VINE GRAPPLE!");  // NEW

        final Color color;
        final String text;
        PowerUpType(Color color, String text) {
            this.color = color;
            this.text = text;
        }
    }

    private void unlockAchievement(int index, String title) {
        achievementsUnlocked[index] = true;
        addToKillFeed("ACHIEVEMENT UNLOCKED: " + title);
        shakeIntensity = 30;
        hitstopFrames = 20;
        saveAchievements(); // save immediately!
    }

    static class PowerUp {
        double x, y;
        PowerUpType type;
        double floatOffset = 0;

        PowerUp(double x, double y, PowerUpType type) {
            this.x = x;
            this.y = y;
            this.type = type;
        }
    }

    enum BirdType {
        PIGEON("Pigeon", 8, 16, 3.8, Color.LIGHTGRAY, 0.0, "Double Jump + Heal Burst (+25 HP)"),
        EAGLE("Eagle", 9, 20, 4.4, Color.DARKRED, 0.65, "Soar (Hold Jump) + Devastating Dive Bomb"),
        HUMMINGBIRD("Hummingbird", 6, 24, 5.0, Color.LIME, 0.9, "Hover/Fly + Flutter Boost"),
        TURKEY("Turkey", 10, 10, 2.8, Color.SADDLEBROWN, 0.0, "Ground Pound AOE"),
        PENGUIN("Penguin", 7, 8, 3.5, Color.BLACK, 0.0, "Ice Slide Dash"),
        SHOEBILL("Shoebill", 11, 12, 3.8, Color.DARKSLATEBLUE, 0.35, "Stun (2.5s)"),
        MOCKINGBIRD("Charles", 5, 18, 4.0, Color.MEDIUMPURPLE, 0.4, "Spawn Lounge (Heal zone)"),
        RAZORBILL("Razorbill", 9, 12, 3.5, Color.INDIGO, 0.3, "Razor Dive + Blade Storm"),
        GRINCHHAWK("Grinch-Hawk", 12, 10, 2.6, Color.rgb(102, 153, 0), 0.0, "Steal ~20 HP from everyone"),
        VULTURE("Vulture", 8, 14, 3.3, Color.rgb(45, 25, 55), 0.25, "Summon Crows + Feast"),
        OPIUMBIRD("Opium Bird", 7, 20, 4.6, Color.rgb(138, 43, 226), 0.8, "Lean Cloud (DoT + Slow)"),
        TITMOUSE("Tufted Titmouse", 6, 22, 5.5, Color.SLATEGRAY, 0.95, "Zip Dash (Teleport + 22 dmg)"),
        PELICAN("Pelican", 13, 9, 2.9, Color.rgb(245, 220, 180), 0.0, "Pelican Plunge (Swallow + Launch)");

        final String name;
        final int power, jumpHeight;
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

    class Bird {
        double x, y, vx = 0, vy = 0;
        BirdType type;
        boolean facingRight = true;
        int playerIndex;
        double health = 100;
        String name;
        double stunTime = 0;
        int specialCooldown = 0;
        int specialMaxCooldown = 120;
        int attackCooldown = 0;
        int attackAnimationTimer = 0;
        boolean canDoubleJump = true;
        boolean loungeActive = false;
        boolean isCitySkin = false; // true = fedora + cigarette skin
        double loungeX, loungeY;
        int diveTimer = 0;
        // === TITMOUSE ZIP DASH ===
        boolean isZipping = false;
        double zipTargetX = 0;
        double zipTargetY = 0;
        int zipTimer = 0; // animation frames
        boolean isGroundPounding = false;
        int carrionSwarmTimer = 0;
        int crowSwarmCooldown = 0;     // each Vulture has his own cooldown
        boolean isFlying = false;// for Vulture special animation
        // OPIUM BIRD EXCLUSIVE
        int leanTimer = 0;            // how long the cloud is active
        int leanCooldown = 0;         // special cooldown (long
        boolean isHigh = false;       // is currently affected by own cloud
        int highTimer = 0;            // visual tripping effect
        int tauntCooldown = 0;
        int tauntTimer = 0;
        private int cooldownFlash = 0;
        int currentTaunt = 0;
        int eagleDiveCountdown = 0;   // frames until Eagle dive impact
        int bladeStormFrames = 0;     // frames remaining for Razorbill blade storm
        int plungeTimer = 0;

        boolean isBlocking = false;
        int blockCooldown = 0; // Prevent spam, 30 frames cooldown after release

        // Vine swinging
        SwingingVine attachedVine = null;
        boolean onVine = false;

        // Power-up buffs
        double speedMultiplier = 1.0;
        double powerMultiplier = 1.0;
        double sizeMultiplier = 1.0;
        int speedTimer = 0;
        int rageTimer = 0;
        int shrinkTimer = 0;
        int thermalTimer = 0;
        double thermalLift = 0.0;

        // === NECTAR BOOST FIELDS (for Vibrant Jungle map) ===
        double speedBoostTimer = 0;      // frames remaining for +20% speed
        double hoverRegenTimer = 0;      // frames remaining for faster hover regen
        double hoverRegenMultiplier = 1.0; // 1.5 when active

        // === CHARLES (MOCKINGBIRD) LOUNGE FIELDS ===
        int loungeHealth = 0;                    // 0 = inactive
        private static final int LOUNGE_MAX_HEALTH = 120;
        private static final double LOUNGE_HEAL_PER_SECOND = 12.0;  // heal rate
        private int loungeDamageFlash = 0;       // for hit flash effect

        private int grappleTimer = 0;        // frames remaining for grapple ability
        private int grappleUses = 0;         // uses left (max 3)
        private boolean isGrappling = false;
        private double grappleTargetX, grappleTargetY;

        private boolean enlargedByPlunge = false;

        Bird(double startX, BirdType type, int playerIndex) {
            this.x = startX;
            this.y = GROUND_Y - 200;
            this.type = type;
            this.playerIndex = playerIndex;
            this.name = (isAI[playerIndex] ? "AI" : "P") + (playerIndex + 1) + ": " + type.name;
            if (type == BirdType.PELICAN) {
                sizeMultiplier = 1.2;
            }
        }

        boolean isOnGround() {
            double bottom = y + 80 * sizeMultiplier;
            if (bottom >= GROUND_Y) return true;
            for (Platform p : platforms) {
                if (x + 40 * sizeMultiplier >= p.x && x + 40 * sizeMultiplier <= p.x + p.w && bottom >= p.y && bottom <= p.y + p.h)
                    return true;
            }
            return false;
        }

        private void loungeHeal() {
            if (type == BirdType.MOCKINGBIRD && loungeActive && loungeHealth > 0) {
                // current center of the bird
                double birdCenterX = x + 40;
                double birdCenterY = y + 40;

                double distToLounge = Math.hypot(birdCenterX - loungeX, birdCenterY - loungeY);

                if (distToLounge < 70) {
                    health += LOUNGE_HEAL_PER_SECOND / 60.0;
                    if (health > 100) health = 100;
                }
            }
        }

        private void handleVerticalCollision() {
            if (onVine) return;

            boolean hit = false;
            double newY = y;

            for (Platform p : platforms) {
                if (x + 40 * sizeMultiplier >= p.x && x + 40 * sizeMultiplier <= p.x + p.w &&
                        y + 80 * sizeMultiplier > p.y && y < p.y + p.h) {
                    newY = p.y - 80 * sizeMultiplier;
                    hit = true;
                    break;
                }
            }

            if (!hit && y + 80 * sizeMultiplier > GROUND_Y) {
                newY = GROUND_Y - 80 * sizeMultiplier;
                hit = true;
            }

            if (hit) {
                y = newY;
                if (vy > 0) vy = 0;
                canDoubleJump = true;

                // === TURKEY GROUND POUND DAMAGE + PARTICLES ===
                if (type == BirdType.TURKEY && isGroundPounding) {
                    isGroundPounding = false;

                    shakeIntensity = 22;
                    hitstopFrames = 15;
                    addToKillFeed(name.split(":")[0].trim() + " SLAMMED THE GROUND!");

                    for (Bird other : players) {
                        if (other == null || other == this || other.health <= 0) continue;

                        double dx = other.x - x;
                        if (Math.abs(dx) < 280 && Math.abs(other.y - y) < 180) {
                            int dmg = (int) (28 * powerMultiplier);
                            double oldHealth = other.health;
                            other.health = Math.max(0, other.health - dmg);
                            double dealtDamage = oldHealth - other.health;

                            damageDealt[playerIndex] += (int) dealtDamage;
                            boolean isKill = oldHealth > 0 && other.health <= 0;
                            if (isKill) {
                                eliminations[playerIndex]++;
                                groundPounds[playerIndex]++;
                                if (zombieFallingClip != null) zombieFallingClip.play();
                            }

                            if (dealtDamage >= 30) {
                                triggerFlash(Math.min(1.0, dealtDamage / 55.0), isKill);
                            } else if (dealtDamage >= 15) {
                                triggerFlash(Math.min(0.75, dealtDamage / 40.0), false);
                            }

                            if (dealtDamage >= 5) {
                                double particleCount = Math.min(50, 3 + dealtDamage * 2);
                                for (int i = 0; i < particleCount; i++) {
                                    double angle = (Math.random() * Math.PI * 2) - Math.PI / 4;
                                    double speed = 3 + Math.random() * (dealtDamage * 0.3);
                                    double vx = Math.cos(angle) * speed;
                                    double vy = Math.sin(angle) * speed - 3;
                                    Color c = Math.random() < 0.6 ? Color.WHITE : Color.rgb(220, 20, 20, 0.8);
                                    particles.add(new Particle(other.x + 40 + (Math.random() - 0.5) * 20,
                                            other.y + 40 + (Math.random() - 0.5) * 20, vx, vy, c));
                                }

                                String attacker = name.split(":")[0].trim();
                                String victim = other.name.split(":")[0].trim();
                                String verb = (type == BirdType.RAZORBILL && diveTimer > 0)
                                        ? "DIVEBOMBED" : (dealtDamage >= 35 ? "BRUTALIZED" : dealtDamage >= 25 ? "SMASHED" : "hit");
                                addToKillFeed(attacker + " " + verb + " " + victim + "! -" + (int) dealtDamage + " HP");

                                if (isKill) {
                                    addToKillFeed("ELIMINATED " + victim + "!");
                                }
                            }

                            if (dealtDamage >= 20) {
                                shakeIntensity = Math.min(20, dealtDamage / 2.0);
                                hitstopFrames = (int) Math.min(12, 4 + dealtDamage / 5);
                                playHitSound(dealtDamage);
                            }

                            other.vx += dx > 0 ? 20 : -20;
                            other.vy -= 12;
                        }
                    }

                    // Big dust cloud + shockwave particles
                    for (int i = 0; i < 80; i++) {
                        double angle = i / 80.0 * Math.PI * 2;
                        double speed = 4 + Math.random() * 10;
                        double vx = Math.cos(angle) * speed;
                        double vy = Math.sin(angle) * speed - 5;
                        Color c = Math.random() < 0.7 ? Color.SADDLEBROWN : Color.SANDYBROWN;
                        particles.add(new Particle(x + 40, y + 70, vx, vy, c));
                    }

                    for (int i = 0; i < 20; i++) {
                        double vx = (Math.random() - 0.5) * 20;
                        double vy = -8 - Math.random() * 10;
                        particles.add(new Particle(x + 40, y + 70, vx, vy, Color.GRAY));
                    }
                }
            }
        }

        private void attack() {
            if (health <= 0) return;
            double range = 120 * sizeMultiplier;
            int dmg = (int) (type.power * powerMultiplier);
            if (type == BirdType.RAZORBILL && diveTimer > 0) {
                range = 200;
                dmg = (int) (type.power * 4 * powerMultiplier);
            }

            for (Bird other : players) {
                if (other == null || other == this || other.health <= 0) continue;

                if (BirdGame3.this.isTrialMode) {
                    // Friendly team: 0,1,2
                    // Enemy: 3
                    boolean attackerIsFriendly = playerIndex <= 2;
                    boolean targetIsFriendly = other.playerIndex <= 2;

                    if (attackerIsFriendly == targetIsFriendly) {
                        continue; // Same team → no damage
                    }
                }
                // Normal mode: no restriction (existing behavior)

                double dist = Math.abs(x - other.x);
                if (dist < range && Math.abs(y - other.y) < 100 * sizeMultiplier) {
                    double kb = type.power * (facingRight ? 1 : -1) * 1.8;

                    // Strong blocking - always reduces damage
                    if (other.isBlocking) {
                        dmg = (int)(dmg * 0.45);           // 55% damage reduction (always active)
                        addToKillFeed(other.name.split(":")[0].trim() + " BLOCKED the attack! -" + dmg + " HP");
                        kb *= 0.35;                        // stronger knockback reduction

                        // Perfect facing parry (25% chance)
                        if (other.facingRight == (x < other.x) && random.nextDouble() < 0.25) {
                            stunTime = 35;
                            addToKillFeed(other.name.split(":")[0].trim() + " PARRIED! Attacker stunned!");
                        }
                    }

                    other.vx += kb;
                    other.vy -= 5;
                    double oldHealth = other.health;
                    other.health -= dmg;
                    if (other.health < 0) other.health = 0;
                    double dealtDamage = oldHealth - other.health;

                    // Track stats
                    damageDealt[playerIndex] += (int) dealtDamage;
                    if (other.health <= 0 && oldHealth > 0) {
                        eliminations[playerIndex]++;
                        checkAchievements(this);
                        if (zombieFallingClip != null) zombieFallingClip.play();
                    }

                    if (other.health <= 0 && oldHealth > 0) {
                        eliminations[playerIndex]++;
                        scores[playerIndex] += 50;  // bonus for kill
                    }
                    scores[playerIndex] += (int) dealtDamage / 2;  // half damage as score

                    // Particles + killfeed (same as before)
                    if (dealtDamage >= 5) {
                        double particleCount = Math.min(50, 3 + dealtDamage * 2);
                        for (int i = 0; i < particleCount; i++) {
                            double angle = (Math.random() * Math.PI * 2) - Math.PI / 4;
                            double speed = 3 + Math.random() * (dealtDamage * 0.3);
                            double vx = Math.cos(angle) * speed;
                            double vy = Math.sin(angle) * speed - 3;
                            Color c = Math.random() < 0.6 ? Color.WHITE : Color.rgb(220, 20, 20, 0.8);
                            particles.add(new Particle(other.x + 40 + (Math.random() - 0.5) * 20, other.y + 40 + (Math.random() - 0.5) * 20, vx, vy, c));
                        }
                        String attacker = name.split(":")[0].trim();
                        String victim = other.name.split(":")[0].trim();
                        String verb = (type == BirdType.RAZORBILL && diveTimer > 0) ? "DIVEBOMBED" :
                                (dealtDamage >= 35) ? "BRUTALIZED" : (dealtDamage >= 25) ? "SMASHED" : "hit";
                        addToKillFeed(attacker + " " + verb + " " + victim + "! -" + (int)dealtDamage + " HP");
                        if (other.health <= 0) {
                            addToKillFeed("ELIMINATED " + victim + "!");
                        }
                    }

                    if (dealtDamage >= 20) {
                        shakeIntensity = Math.min(20, dealtDamage / 2.0);
                        hitstopFrames = (int) Math.min(12, 4 + dealtDamage / 5);
                        playHitSound(dealtDamage);
                    }
                }
            }

            // === LOUNGE CAN BE HIT BY ANY ATTACK (even if Charles isn't hit) ===
            for (Bird target : players) {
                if (target == null || target.type != BirdType.MOCKINGBIRD || !target.loungeActive || target.loungeHealth <= 0)
                    continue;

                double distToLounge = Math.hypot(target.loungeX - (x + 40), target.loungeY - (y + 40));
                if (distToLounge < 130) {  // slightly bigger hitbox
                    int loungeDmg = (int) (type.power * 2.0 * powerMultiplier);
                    if (type == BirdType.RAZORBILL && diveTimer > 0) loungeDmg *= 2;

                    target.loungeHealth -= loungeDmg;
                    target.loungeDamageFlash = 15;

                    addToKillFeed(name.split(":")[0].trim() + " smashed the Lounge! -" + loungeDmg + " HP");

                    // Hit particles
                    for (int i = 0; i < 30; i++) {
                        double angle = Math.random() * Math.PI * 2;
                        particles.add(new Particle(
                                target.loungeX + Math.cos(angle) * 50,
                                target.loungeY + Math.sin(angle) * 40,
                                Math.cos(angle) * 10, Math.sin(angle) * 10 - 4,
                                Color.LIME));
                    }

                    if (target.loungeHealth <= 0) {
                        target.loungeActive = false;
                        target.loungeHealth = 0;
                        addToKillFeed("THE LOUNGE HAS BEEN OBLITERATED!");
                        shakeIntensity = 30;
                        hitstopFrames = 18;
                        // Big explosion
                        for (int i = 0; i < 120; i++) {
                            double angle = i / 120.0 * Math.PI * 2;
                            double speed = 8 + Math.random() * 14;
                            particles.add(new Particle(target.loungeX, target.loungeY,
                                    Math.cos(angle) * speed, Math.sin(angle) * speed - 5,
                                    Math.random() < 0.5 ? Color.LIME : Color.GREENYELLOW));
                        }
                    }
                    break; // only hit one lounge per attack
                }
            }
        }

        private void special() {
            if (health <= 0 || specialCooldown > 0) {
                if (specialCooldown > 0 && !isAI[playerIndex]) {
                    this.cooldownFlash = 15; // ← now uses per-bird field
                }
                return;
            }

            if (BirdGame3.this.jalapenoClip != null) BirdGame3.this.jalapenoClip.play();

            switch (type) {
                case PIGEON -> {
                    health = Math.min(100, health + 25);
                    canDoubleJump = true;
                    specialCooldown = 480;      // 8 seconds
                    specialMaxCooldown = 480;
                    addToKillFeed(name.split(":")[0].trim() + " HEAL BURST! +25 HP + DOUBLE JUMP");
                    shakeIntensity = 12;
                    hitstopFrames = 8;
                    for (int i = 0; i < 40; i++) {
                        double angle = Math.random() * Math.PI * 2;
                        double speed = 3 + Math.random() * 8;
                        particles.add(new Particle(x + 40, y + 40,
                                Math.cos(angle) * speed, Math.sin(angle) * speed - 4,
                                Color.LIME.deriveColor(0, 1, 1, 0.8)));
                    }
                }
                case EAGLE -> {
                    // === EPIC EAGLE DIVE BOMB - NOW MUCH DEADLIER ===
                    diveTimer = 120; // longer charge time (2 seconds) for bigger payoff
                    specialCooldown = 480; // slightly shorter cooldown (8 sec instead of 9)
                    specialMaxCooldown = 480;

                    // Massive visual + audio cue
                    shakeIntensity = 35;
                    hitstopFrames = 20;
                    addToKillFeed("SKREEEEEEEE!!! " + name.split(":")[0].trim() + " IS DIVING FROM THE HEAVENS!");

                    // Long red comet trail
                    for (int i = 0; i < 100; i++) {
                        double angle = Math.atan2(vy, vx) + Math.PI;
                        double dist = i * 10;
                        particles.add(new Particle(
                                x + 40 + Math.cos(angle) * dist,
                                y + 40 + Math.sin(angle) * dist,
                                0, 0,
                                Color.CRIMSON.deriveColor(0, 1, 1, 1.0 - i / 100.0)
                        ));
                    }

                    // Larger ground warning zone
                    double predictX = x + vx * 40;
                    for (int i = -15; i <= 15; i++) {
                        particles.add(new Particle(predictX + i * 60, GROUND_Y - 20, 0, -5 - Math.random() * 8, Color.ORANGERED.brighter()));
                    }

                    eagleDiveCountdown = 60; // longer countdown = more time to aim
                }
                case HUMMINGBIRD -> {
                    vy -= type.jumpHeight * 1.2;
                    vx += (facingRight ? 1 : -1) * 8;
                    specialCooldown = 300;      // 5 seconds
                    specialMaxCooldown = 300;
                    addToKillFeed(name.split(":")[0].trim() + " FLUTTER BOOST!");
                    for (int i = 0; i < 30; i++) {
                        particles.add(new Particle(x + 40, y + 40,
                                (Math.random() - 0.5) * 20, -10 - Math.random() * 10,
                                Color.CYAN.brighter()));
                    }
                }
                case TURKEY -> {
                    vy = -type.jumpHeight * 1.5;
                    isGroundPounding = true;
                    specialCooldown = 420;      // 7 seconds
                    specialMaxCooldown = 420;
                }
                case PENGUIN -> {
                    vx = (facingRight ? 1 : -1) * 22.0;
                    vy = 0;
                    specialCooldown = 540;      // 9 seconds
                    specialMaxCooldown = 540;
                    addToKillFeed(name.split(":")[0].trim() + " ICE SLIDE!");
                    // Ice trail particles
                    for (int i = 0; i < 50; i++) {
                        particles.add(new Particle(x + 40 + (Math.random() - 0.5) * 100,
                                y + 70, (Math.random() - 0.5) * 8, -2 - Math.random() * 4,
                                Color.CYAN.deriveColor(0, 1, 1, 0.7)));
                    }
                }
                case SHOEBILL -> {
                    for (Bird other : players) {
                        if (other == null || other == this || other.health <= 0) continue;
                        double dist = Math.hypot(other.x - x, other.y - y);
                        if (dist < 400) { // huge range
                            other.stunTime = 180; // 3 seconds
                        }
                    }
                    specialCooldown = 900;      // 12 seconds
                    specialMaxCooldown = 900;
                    addToKillFeed(name.split(":")[0].trim() + " DEATH STARE! Everyone stunned!");
                    shakeIntensity = 20;
                }
                case MOCKINGBIRD -> {
                    loungeActive = true;
                    loungeX = x + 40;
                    loungeY = y + 40;
                    loungeHealth = LOUNGE_MAX_HEALTH;
                    specialCooldown = 720;      // 12 seconds
                    specialMaxCooldown = 720;
                    addToKillFeed(name.split(":")[0].trim() + " opened the LOUNGE!");
                }
                case RAZORBILL -> {
                    // === RAZORBILL "BLADE STORM" — FINAL BALANCED VERSION ===
                    diveTimer = 180;                    // 3 seconds of blade mode (was 4)
                    specialCooldown = 720;              // 12 seconds cooldown (was 10)
                    specialMaxCooldown = 720;

                    shakeIntensity = 28;
                    hitstopFrames = 18;
                    addToKillFeed("BLADE STORM! " + name.split(":")[0].trim() + " IS UNSTOPPABLE!");

                    // Speed boost + bigger hitbox
                    vx *= 2.4;                          // fast but controllable
                    sizeMultiplier = 1.2;
                    powerMultiplier = 1.6;              // strong but not broken

                    // Epic visual startup
                    for (int i = 0; i < 100; i++) {
                        double angle = i / 100.0 * Math.PI * 2;
                        double speed = 8 + Math.random() * 10;
                        particles.add(new Particle(
                                x + 40 + Math.cos(angle) * 50,
                                y + 40 + Math.sin(angle) * 50,
                                Math.cos(angle) * speed,
                                Math.sin(angle) * speed,
                                Color.CYAN.brighter()
                        ));
                    }
                    bladeStormFrames = 180;  // start blade storm duration
                }
                case GRINCHHAWK -> {
                    int stolen = 0;
                    for (Bird other : players) {
                        if (other == null || other == this || other.health <= 0) continue;
                        int take = (int) Math.min(12 + (health > 80 ? 6 : 0), other.health);
                        other.health -= take;
                        stolen += take;
                        addToKillFeed(name.split(":")[0].trim() + " STOLE " + take + " HP from " + other.name.split(":")[0].trim() + "!");
                    }
                    health = Math.min(100, health + stolen);
                    specialCooldown = 900;      // 15 seconds — high risk, high reward
                    specialMaxCooldown = 900;
                    shakeIntensity = 20;
                }
                case VULTURE -> {
                    crowSwarmCooldown = 900;
                    specialCooldown = 900;
                    specialMaxCooldown = 900;
                    addToKillFeed(name.split(":")[0].trim() + " SUMMONS THE MURDER!");

                    int crowCount = 20 + random.nextInt(15); // 20-34 crows
                    for (int i = 0; i < crowCount; i++) {
                        // Spawn closer — within 1500 units of the vulture
                        double angle = Math.random() * Math.PI * 2;
                        double dist = 300 + Math.random() * 1200;
                        double spawnX = x + 40 + Math.cos(angle) * dist;
                        double spawnY = y + 40 + Math.sin(angle) * dist;

                        CrowMinion crow = new CrowMinion(spawnX, spawnY, null);
                        crow.owner = this;
                        crowMinions.add(crow);
                    }

                    shakeIntensity = 28;
                    hitstopFrames = 18;
                    carrionSwarmTimer = 180;

                    // Massive black feather storm
                    for (int i = 0; i < 200; i++) {
                        double angle = Math.random() * Math.PI * 2;
                        double speed = 8 + Math.random() * 16;
                        particles.add(new Particle(x + 40, y + 40,
                                Math.cos(angle) * speed,
                                Math.sin(angle) * speed - 6,
                                Color.rgb(10, 0, 20)));
                    }
                }
                case OPIUMBIRD -> {
                    leanTimer = 480;
                    leanCooldown = 900;
                    specialCooldown = 900;
                    specialMaxCooldown = 900;
                    addToKillFeed(name.split(":")[0].trim() + " DROPPED THE LEAN!");
                    shakeIntensity = 20;
                    hitstopFrames = 15;
                    for (int i = 0; i < 150; i++) {
                        double angle = Math.random() * Math.PI * 2;
                        particles.add(new Particle(x + 40, y + 40,
                                Math.cos(angle) * (2 + Math.random() * 10),
                                Math.sin(angle) * (2 + Math.random() * 10) - 4,
                                Color.PURPLE.deriveColor(0, 1, 1, 0.7)));
                    }
                }
                case TITMOUSE -> {
                    // Find closest living enemy
                    Bird target = null;
                    double bestDist = Double.MAX_VALUE;
                    for (Bird b : players) {
                        if (b == null || b == this || b.health <= 0) continue;
                        double d = Math.hypot(b.x - x, b.y - y);
                        if (d < bestDist) {
                            bestDist = d;
                            target = b;
                        }
                    }
                    if (target == null) {
                        addToKillFeed(name.split(":")[0].trim() + " tried to ZIP... but no target!");
                        specialCooldown = 300; // short cooldown if no target
                        specialMaxCooldown = 300;
                        return;
                    }

                    isZipping = true;
                    zipTargetX = target.x;
                    zipTargetY = target.y;
                    zipTimer = 30; // 0.5 sec dash animation

                    specialCooldown = 720;
                    specialMaxCooldown = 720;

                    addToKillFeed(name.split(":")[0].trim() + " ZIPPED to " + target.name.split(":")[0].trim() + "!");

                    // Trail particles (blue streak)
                    for (int i = 0; i < 50; i++) {
                        double offset = i * 8;
                        particles.add(new Particle(
                                x + 40 - vx * offset / 10,
                                y + 40 - vy * offset / 10,
                                (Math.random() - 0.5) * 8,
                                (Math.random() - 0.5) * 8 - 2,
                                Color.SKYBLUE.deriveColor(0, 1, 1, 0.8 - i / 60.0)
                        ));
                    }

                    // Instant move + damage on arrival (handled in update)
                }
                case PELICAN -> {
                    // Find closest enemy in big range
                    Bird target = null;
                    double bestDist = Double.MAX_VALUE;
                    for (Bird b : players) {
                        if (b == null || b == this || b.health <= 0) continue;
                        if (BirdGame3.this.isTrialMode && ((playerIndex <= 2) == (b.playerIndex <= 2))) continue;
                        double d = Math.hypot(b.x - x, b.y - y);
                        if (d < bestDist && d < 280) {
                            bestDist = d;
                            target = b;
                        }
                    }
                    if (target != null) {
                        plungeTimer = 45; // 0.75 sec animation
                        sizeMultiplier *= 1.18;
                        enlargedByPlunge = true;
                        specialCooldown = 660; // 11 seconds
                        specialMaxCooldown = 660;
                        addToKillFeed(name.split(":")[0].trim() + " PELICAN PLUNGE!!!");
                        shakeIntensity = 32;
                        hitstopFrames = 18;
                        // Swallow effect + huge launch
                        target.vx += (target.x > x ? 1 : -1) * 55;
                        target.vy = -38;
                        int dmg = (int)(32 * powerMultiplier);
                        double old = target.health;
                        target.health = Math.max(0, target.health - dmg);
                        damageDealt[playerIndex] += (int)(old - target.health);
                        if (target.health <= 0 && old > 0) eliminations[playerIndex]++;
                        // Big particles
                        for (int i = 0; i < 120; i++) {
                            double ang = Math.random() * Math.PI * 2;
                            particles.add(new Particle(target.x + 40, target.y + 40,
                                    Math.cos(ang) * (8 + Math.random() * 22),
                                    Math.sin(ang) * (8 + Math.random() * 22) - 12,
                                    Color.ORANGE.brighter()));
                        }
                        // === PELICAN ACHIEVEMENT PROGRESS ===
                        BirdGame3.this.achievementProgress[18]++;
                        if (BirdGame3.this.achievementProgress[18] >= 15 && !BirdGame3.this.achievementsUnlocked[18]) {
                            BirdGame3.this.unlockAchievement(18, "PELICAN KING!");
                        }
                    } else {
                        specialCooldown = 180; // short cooldown if no target
                    }
                }
            }
        }

        private KeyCode leftKey() {
            return switch (playerIndex) {
                case 0 -> KeyCode.A;
                case 1 -> KeyCode.LEFT;
                case 2 -> KeyCode.F;
                case 3 -> KeyCode.J;
                default -> KeyCode.A;
            };
        }

        private KeyCode rightKey() {
            return switch (playerIndex) {
                case 0 -> KeyCode.D;
                case 1 -> KeyCode.RIGHT;
                case 2 -> KeyCode.H;
                case 3 -> KeyCode.L;
                default -> KeyCode.D;
            };
        }

        private KeyCode jumpKey() {
            return switch (playerIndex) {
                case 0 -> KeyCode.W;
                case 1 -> KeyCode.UP;
                case 2 -> KeyCode.T;
                case 3 -> KeyCode.I;
                default -> KeyCode.W;
            };
        }

        private KeyCode attackKey() {
            return switch (playerIndex) {
                case 0 -> KeyCode.SPACE;
                case 1 -> KeyCode.ENTER;
                case 2 -> KeyCode.Y;
                case 3 -> KeyCode.O;
                default -> KeyCode.SPACE;
            };
        }

        private KeyCode specialKey() {
            return switch (playerIndex) {
                case 0 -> KeyCode.SHIFT;
                case 1 -> KeyCode.DOWN;
                case 2 -> KeyCode.U;
                case 3 -> KeyCode.P;
                default -> KeyCode.SHIFT;
            };
        }

        private KeyCode blockKey() {
            return switch (playerIndex) {
                case 0 -> KeyCode.S;
                case 1 -> KeyCode.BACK_SLASH;
                case 2 -> KeyCode.G;
                case 3 -> KeyCode.K;
                default -> KeyCode.S;
            };
        }

        private void aiControl() {
            // === FIND PRIMARY TARGET (closest living enemy) ===
            Bird target = null;
            double bestDist = Double.MAX_VALUE;
            for (Bird b : players) {
                if (b == null || b == this || b.health <= 0) continue;
                if (BirdGame3.this.isTrialMode) {
                    if (playerIndex <= 2) { if (b.playerIndex != 3) continue; }
                    else if (playerIndex == 3) { if (b.playerIndex > 2) continue; }
                }
                double d = Math.hypot(b.x - x, b.y - y);
                if (d < bestDist) { bestDist = d; target = b; }
            }

            // === FIND BEST POWER-UP (prioritize health when low, strong buffs otherwise) ===
            PowerUp bestPowerUp = null;
            double bestPowerUpScore = 0;
            for (PowerUp p : powerUps) {
                double dist = Math.hypot(p.x - (x + 40), p.y - (y + 40));
                double score = 0;
                if (p.type == PowerUpType.HEALTH) score = (100 - health) * 2 + 1000;
                else if (p.type == PowerUpType.RAGE || p.type == PowerUpType.NEON) score = 800;
                else if (p.type == PowerUpType.SPEED || p.type == PowerUpType.THERMAL) score = 500;
                score /= (1 + dist / 400.0); // closer = better
                if (score > bestPowerUpScore) {
                    bestPowerUpScore = score;
                    bestPowerUp = p;
                }
            }

            boolean hasTarget = target != null;
            boolean nearTarget = hasTarget && bestDist < 220;
            boolean mediumRange = hasTarget && bestDist < 500;
            boolean far = hasTarget && bestDist > 700;
            boolean onGround = isOnGround();
            boolean airborne = !onGround;

            // === DECIDE PRIMARY GOAL ===
            boolean chasePowerUp = bestPowerUp != null &&
                    (bestPowerUp.type == PowerUpType.HEALTH && health < 70 ||
                            bestPowerUpScore > 800);

            double goalX = hasTarget ? target.x : (chasePowerUp ? bestPowerUp.x : x);
            double goalY = hasTarget ? target.y : (chasePowerUp ? bestPowerUp.y : y);
            facingRight = goalX > x + 20;

            // Clear movement keys
            pressedKeys.remove(leftKey());
            pressedKeys.remove(rightKey());
            pressedKeys.remove(jumpKey());

            // === DODGING LOGIC - PRIORITY 1 ===
            boolean shouldDodge = false;
            double dodgeDir = 0; // -1 left, 1 right

            if (hasTarget) {
                // Dodge ground pound if enemy is preparing above us
                if (target.type == BirdType.TURKEY && target.isGroundPounding &&
                        Math.abs(target.x - x) < 300 && target.y < y - 100) {
                    shouldDodge = true;
                    dodgeDir = target.x > x ? -1 : 1;
                }
                // Dodge Eagle dive
                if (target.type == BirdType.EAGLE && target.diveTimer > 0 &&
                        Math.abs(target.x - x) < 400 && target.y < y) {
                    shouldDodge = true;
                    dodgeDir = target.x > x ? -1 : 1;
                }
                // Dodge ice slide
                if (target.type == BirdType.PENGUIN && Math.abs(target.vx) > 15 &&
                        Math.abs(target.x - x) < 400 && Math.abs(target.y - y) < 100) {
                    shouldDodge = true;
                    if (onGround) pressedKeys.add(jumpKey());
                }
            }

            if (shouldDodge) {
                if (dodgeDir < 0) pressedKeys.add(leftKey());
                else pressedKeys.add(rightKey());
                if (onGround) pressedKeys.add(jumpKey());
                return; // dodge takes full priority
            }

            // === VERTICAL POSITIONING - CRITICAL FIX ===
            boolean enemyDirectlyAbove = hasTarget && Math.abs(target.x - x) < 120 && target.y < y - 100 && target.isOnGround();
            boolean enemyDirectlyBelow = hasTarget && Math.abs(target.x - x) < 120 && target.y > y + 100 && isOnGround();

            // If enemy is spamming attack from above → GET DOWN or move away
            if (enemyDirectlyAbove) {
                // Move sideways first
                if (target.x > x) pressedKeys.add(leftKey());
                else pressedKeys.add(rightKey());
                // If on edge, fall off deliberately
                if (onGround) {
                    // Check if we're near edge
                    boolean leftEdge = true, rightEdge = true;
                    for (Platform p : platforms) {
                        if (x > p.x && x < p.x + p.w && y + 80 > p.y && y + 80 < p.y + p.h + 100) {
                            if (x - 100 > p.x) leftEdge = false;
                            if (x + 100 < p.x + p.w) rightEdge = false;
                        }
                    }
                    if ((target.x > x && rightEdge) || (target.x < x && leftEdge)) {
                        // Walk off the edge toward enemy to drop down
                        if (target.x > x) pressedKeys.add(rightKey());
                        else pressedKeys.add(leftKey());
                    }
                }
            }

            // If enemy is below and we're Turkey/Eagle/Razorbill → stay high or get higher
            if (enemyDirectlyBelow && (type == BirdType.TURKEY || type == BirdType.EAGLE || type == BirdType.RAZORBILL)) {
                if (airborne && type.flyUpForce > 0) pressedKeys.add(jumpKey());
            }

            // === HORIZONTAL MOVEMENT ===
            double horizThreshold = chasePowerUp ? 100 : 80;
            if (Math.abs(goalX - x) > horizThreshold) {
                if (goalX < x) pressedKeys.add(leftKey());
                else pressedKeys.add(rightKey());
            }

            // === JUMP / FALL / FLY LOGIC ===
            if (onGround) {
                boolean shouldJump = goalY < y - 120;

                // Jump to reach goal vertically
                // Jump over gaps or to dodge
                if (hasTarget && Math.abs(target.x - x) < 200 && target.y > y + 50) shouldJump = true;
                // Jump if enemy is preparing ground pound nearby
                if (hasTarget && target.type == BirdType.TURKEY && target.isGroundPounding && Math.abs(target.x - x) < 350) {
                    shouldJump = true;
                }

                if (shouldJump) pressedKeys.add(jumpKey());
            } else {
                // In air: flying birds maintain height
                if (type.flyUpForce > 0 && y > 200 && y < 1200) {
                    boolean maintainHeight = !hasTarget || !(target.y > y + 300) ||
                            (type != BirdType.EAGLE && type != BirdType.RAZORBILL && type != BirdType.TURKEY);
                    // Drop down if enemy is far below and we're offensive bird
                    if (maintainHeight) pressedKeys.add(jumpKey());
                }
            }

            // === ATTACK LOGIC - SMART ===

            // === BLOCK LOGIC FOR AI (block if enemy is attacking nearby and facing them) ===
            if (hasTarget && nearTarget && target.attackAnimationTimer > 0 &&
                    (facingRight == (target.x < x)) && random.nextDouble() < 0.4) { // 40% chance if conditions met
                pressedKeys.add(blockKey());
            } else {
                pressedKeys.remove(blockKey());
            }

            boolean canAttackSafely = !enemyDirectlyAbove && attackCooldown <= 0;
            if (nearTarget && canAttackSafely) {
                pressedKeys.add(attackKey());
            }

            // === SPECIAL ABILITY - VERY SMART USAGE ===
            if (specialCooldown <= 0) {
                switch (type) {
                    case PIGEON -> {
                        if (health < 40) pressedKeys.add(specialKey());
                    }
                    case EAGLE -> {
                        if (y < GROUND_Y - 900 && hasTarget && target.y > y + 200 && mediumRange) {
                            pressedKeys.add(specialKey());
                        }
                    }
                    case HUMMINGBIRD -> {
                        if (far || (hasTarget && target.y < y - 250)) {
                            pressedKeys.add(specialKey());
                        }
                    }
                    case TURKEY -> {
                        if (onGround && hasTarget && target.y > y + 80 && Math.abs(target.x - x) < 300) {
                            pressedKeys.add(specialKey());
                        }
                    }
                    case PENGUIN -> {
                        if (onGround && nearTarget) pressedKeys.add(specialKey());
                    }
                    case SHOEBILL -> {
                        if (mediumRange && random.nextDouble() < 0.07) pressedKeys.add(specialKey());
                    }
                    case MOCKINGBIRD -> {
                        if (health < 70 && onGround) pressedKeys.add(specialKey());
                    }
                    case RAZORBILL -> {
                        if (airborne && mediumRange && (hasTarget && target.y > y || nearTarget)) {
                            pressedKeys.add(specialKey());
                        }
                    }
                    case GRINCHHAWK -> {
                        if (health > 90 && nearTarget) pressedKeys.add(specialKey());
                    }
                    case VULTURE -> {
                        if (crowSwarmCooldown <= 0 && random.nextDouble() < 0.05) {
                            pressedKeys.add(specialKey());
                        }
                    }
                    case OPIUMBIRD -> {
                        if (nearTarget && onGround && random.nextDouble() < 0.7) {
                            pressedKeys.add(specialKey());
                        }
                    }
                    case TITMOUSE -> {
                        if (nearTarget) pressedKeys.add(specialKey());
                    }
                    case PELICAN -> {
                        if (nearTarget && plungeTimer <= 0) pressedKeys.add(specialKey());
                    }
                }
            }

            // === TAUNT WHEN DOMINATING ===
            if (tauntCooldown <= 0 && hasTarget && health > 85 && nearTarget && random.nextDouble() < 0.015) {
                currentTaunt = random.nextInt(3) + 1;
                tauntTimer = 60;
                tauntCooldown = 300;
                addToKillFeed(name.split(":")[0].trim() + " IS ABSOLUTELY COOKING!");
            }
        }

        void update(double gameSpeed) {   // ← CHANGE signature

            if (health > 0 && isAI[playerIndex]) aiControl();

            // Timers — respect slow-mo
            speedTimer = Math.max(0, (int)(speedTimer - gameSpeed));
            rageTimer = Math.max(0, (int)(rageTimer - gameSpeed));
            shrinkTimer = Math.max(0, (int)(shrinkTimer - gameSpeed));
            thermalTimer = Math.max(0, (int)(thermalTimer - gameSpeed));
            grappleTimer = Math.max(0, (int)(grappleTimer - gameSpeed));
            speedBoostTimer = Math.max(0, (int)(speedBoostTimer - gameSpeed));
            hoverRegenTimer = Math.max(0, (int)(hoverRegenTimer - gameSpeed));

            stunTime = Math.max(0, stunTime - gameSpeed);
            if (specialCooldown > 0) specialCooldown = (int)Math.max(0, specialCooldown - gameSpeed);
            if (crowSwarmCooldown > 0) crowSwarmCooldown = (int)Math.max(0, crowSwarmCooldown - gameSpeed);
            if (attackCooldown > 0) attackCooldown = (int)Math.max(0, attackCooldown - gameSpeed);
            diveTimer = Math.max(0, (int)(diveTimer - gameSpeed));
            if (attackAnimationTimer > 0) attackAnimationTimer = (int)Math.max(0, attackAnimationTimer - gameSpeed);
            leanCooldown = Math.max(0, (int)(leanCooldown - gameSpeed));
            leanTimer = Math.max(0, (int)(leanTimer - gameSpeed));
            highTimer = Math.max(0, (int)(highTimer - gameSpeed));
            tauntCooldown = Math.max(0, (int)(tauntCooldown - gameSpeed));
            tauntTimer = Math.max(0, (int)(tauntTimer - gameSpeed));
            eagleDiveCountdown = Math.max(0, (int)(eagleDiveCountdown - gameSpeed));
            bladeStormFrames = Math.max(0, (int)(bladeStormFrames - gameSpeed));
            plungeTimer = Math.max(0, (int)(plungeTimer - gameSpeed));
            blockCooldown = Math.max(0, (int)(blockCooldown - gameSpeed));

            // === VINE GRAPPLE SYSTEM ===
            if (grappleTimer > 0) {
                grappleTimer--;
                if (grappleTimer == 0) {
                    grappleUses = 0; // expire uses when timer ends
                }
            }

// Grapple activation on special press (while airborne and has uses)
            if (grappleUses > 0 && pressedKeys.contains(specialKey()) && !isOnGround() && !isGrappling && specialCooldown <= 0) {
                // Find nearest platform or swinging vine platform
                double bestDist = Double.MAX_VALUE;
                double targetX = x + 40;
                double targetY = y + 40;
                boolean found = false;

                // Check regular platforms
                for (Platform p : platforms) {
                    double centerX = p.x + p.w / 2;
                    double centerY = p.y; // attach to top of platform
                    double d = Math.hypot(centerX - (x + 40), centerY - (y + 40));
                    if (d < bestDist && d > 100 && d < 800) { // reasonable range
                        bestDist = d;
                        targetX = centerX;
                        targetY = centerY - 40;
                        found = true;
                    }
                }

                // Check swinging vine platforms
                if (selectedMap == MapType.VIBRANT_JUNGLE) {
                    for (SwingingVine v : swingingVines) {
                        double d = Math.hypot(v.platformX + v.platformW / 2 - (x + 40), v.platformY - (y + 40));
                        if (d < bestDist && d > 100 && d < 800) {
                            bestDist = d;
                            targetX = v.platformX + v.platformW / 2;
                            targetY = v.platformY - 40;
                            found = true;
                        }
                    }
                }

                if (found) {
                    isGrappling = true;
                    grappleTargetX = targetX;
                    grappleTargetY = targetY;
                    grappleUses--;
                    specialCooldown = 30; // short cooldown to prevent spam
                    addToKillFeed(name.split(":")[0].trim() + " GRAPPLED with vine!");
                    // Green vine trail
                    for (int i = 0; i < 30; i++) {
                        double progress = i / 30.0;
                        particles.add(new Particle(
                                x + 40 + (targetX - x - 40) * progress,
                                y + 40 + (targetY - y - 40) * progress,
                                0, 0, Color.FORESTGREEN.deriveColor(0,1,1,0.7)
                        ));
                    }
                }
            }

// Active grappling movement
            if (isGrappling) {
                double dx = grappleTargetX - (x + 40);
                double dy = grappleTargetY - (y + 40);
                double dist = Math.hypot(dx, dy);
                if (dist > 30) {
                    vx = dx / dist * 32;  // fast pull
                    vy = dy / dist * 32;
                } else {
                    // Arrived
                    isGrappling = false;
                    vx *= 0.6;
                    vy = 0;
                    canDoubleJump = true; // refresh jump
                }
            }

            // Reset expired power-up multipliers (after the main gameSpeed block above)
            if (speedBoostTimer <= 0) {
                speedBoostTimer = 0;
                speedMultiplier = 1.0;
            }
            if (hoverRegenTimer <= 0) {
                hoverRegenTimer = 0;
                hoverRegenMultiplier = 1.0;
            }

            if (speedTimer <= 0) {
                speedMultiplier = 1.0;
            }
            if (rageTimer <= 0) {
                powerMultiplier = 1.0;
            }
            if (shrinkTimer <= 0) {
                sizeMultiplier = (type == BirdType.PELICAN ? 1.2 : 1.0);
            }
            if (thermalTimer <= 0) {
                thermalLift = 0.0;
            }
            if (type == BirdType.PELICAN && plungeTimer <= 0 && enlargedByPlunge) {
                sizeMultiplier /= 1.18;
                enlargedByPlunge = false;
            }

            loungeHeal();
            if (type == BirdType.MOCKINGBIRD && loungeActive && loungeHealth > 0) {
                loungeTime[playerIndex]++;
            }

            // === OPIUM BIRD SPECIAL SYSTEM ===
            if (type == BirdType.OPIUMBIRD) {
                if (leanTimer > 0) {
                    leanTime[playerIndex]++;
                }

                // Lean cloud damage + slow effect
                if (leanTimer > 0) {
                    for (Bird other : players) {
                        if (other == null || other == this || other.health <= 0) continue;
                        double dx = other.x - x;
                        double dy = other.y - y;
                        if (Math.hypot(dx, dy) < 300) {
                            // Damage over time
                            if (Math.hypot(dx, dy) < 250) {  // smaller radius
                                if (random.nextInt(60) == 0) {
                                    other.health = Math.max(0, other.health - 1);
                                    addToKillFeed(name.split(":")[0].trim() + "'s lean is COOKING " + other.name.split(":")[0].trim() + " (-1 HP)");
                                }
                                other.vx *= 0.94;  // weaker slow
                                other.vy *= 0.98;
                            }

                            // Make them trip too if too close
                            if (Math.hypot(dx, dy) < 120 && random.nextInt(20) == 0) {
                                other.highTimer = 180;
                            }
                        }
                    }

                    // Self-high effect (visual only, he's immune to damage)
                    if (Math.random() < 0.1) highTimer = 120;
                }

                if (highTimer > 0) highTimer--;
            }

            boolean stunned = stunTime > 0;
            boolean airborne = !isOnGround();

// Moved blocking logic here (after stunned declaration)
            if (blockCooldown > 0) blockCooldown--;
            if (!stunned && pressedKeys.contains(blockKey()) && blockCooldown <= 0) {
                isBlocking = true;
                vx *= 0.6; // Slow movement while blocking
                vy *= 0.9; // Slight air resistance
            } else {
                if (isBlocking) blockCooldown = 30; // Cooldown after releasing block
                isBlocking = false;
            }

// === GRAVITY ONCE AND ONLY ONCE ===
            vy += GRAVITY * gameSpeed;

            // === EAGLE PASSIVE: SKY DOMINANCE ===
            if (type == BirdType.EAGLE) {
                if (y < GROUND_Y - 800) { // very high up
                    powerMultiplier = Math.max(powerMultiplier, 1.3);
                    speedMultiplier = Math.max(speedMultiplier, 1.2);
                    // Golden trail when soaring high
                    if (Math.random() < 0.3) {
                        particles.add(new Particle(x + 40 + (Math.random() - 0.5) * 60,
                                y + 80, (Math.random() - 0.5) * 6, 2 + Math.random() * 4,
                                Color.GOLD.deriveColor(0, 1, 1, 0.7)));
                    }
                } else if (y < GROUND_Y - 400) { // moderately high
                    powerMultiplier = Math.max(powerMultiplier, 1.1);
                }
            }

            if (type == BirdType.VULTURE && !stunned && pressedKeys.contains(jumpKey())) {
                isFlying = true;
                vy -= 0.65;  // slightly weaker lift
                if (vy < -6.0) vy = -6.0;  // lower max upward speed
            } else if (type == BirdType.VULTURE) {
                isFlying = false;
                // Add slight downward pull when not holding jump
                if (vy < 0) vy += 0.3;  // faster fall when not flying
            }

            // === OTHER BIRDS NORMAL FLY/GLIDE (Eagle, Hummingbird, etc.) ===
            if (!stunned && pressedKeys.contains(jumpKey()) && airborne) {
                vy -= (type.flyUpForce + thermalLift) * hoverRegenMultiplier; // Now boosted by nectar
            }

            // Slow falling when riding a thermal (feels like soaring)
            if (thermalTimer > 0 && vy > 0) {
                vy *= 0.85;
            }

            // === TITMOUSE ZIP DASH LOGIC ===
            if (type == BirdType.TITMOUSE && isZipping) {
                zipTimer--;
                if (zipTimer > 0) {
                    // Smooth lerp to target during dash
                    double progress = 1.0 - (zipTimer / 30.0);
                    x = x + (zipTargetX - x) * progress * 0.4;
                    y = y + (zipTargetY - y) * progress * 0.4;

                    // Blur trail
                    for (int i = 0; i < 5; i++) {
                        particles.add(new Particle(
                                x + 40 + (Math.random() - 0.5) * 30,
                                y + 40 + (Math.random() - 0.5) * 30,
                                (Math.random() - 0.5) * 20,
                                (Math.random() - 0.5) * 20,
                                Color.SKYBLUE.deriveColor(0, 1, 1, 0.6)
                        ));
                    }
                } else {
                    // Arrived! Deal damage
                    for (Bird other : players) {
                        if (other == null || other == this || other.health <= 0) continue;
                        double dist = Math.hypot(other.x - x, other.y - y);
                        if (dist < 120) {
                            int dmg = (int) (22 * powerMultiplier);
                            double oldHealth = other.health;
                            other.health = Math.max(0, other.health - dmg);
                            double dealt = oldHealth - other.health;
                            damageDealt[playerIndex] += (int) dealt;
                            if (other.health <= 0 && oldHealth > 0) eliminations[playerIndex]++;
                            if (zombieFallingClip != null) zombieFallingClip.play();

                            other.vx += (other.x > x ? 1 : -1) * 25;
                            other.vy -= 18;

                            addToKillFeed(name.split(":")[0].trim() + " ZAPPED " + other.name.split(":")[0].trim() + "! -" + dmg + " HP");

                            // Big impact flash + particles
                            hitstopFrames = 12;
                            shakeIntensity = 28;
                            triggerFlash(0.8, other.health <= 0);

                            for (int i = 0; i < 60; i++) {
                                double angle = Math.random() * Math.PI * 2;
                                double speed = 8 + Math.random() * 16;
                                particles.add(new Particle(
                                        other.x + 40,
                                        other.y + 40,
                                        Math.cos(angle) * speed,
                                        Math.sin(angle) * speed - 6,
                                        Color.SKYBLUE.brighter()
                                ));
                            }
                        }
                    }
                    isZipping = false;
                }
                // Skip normal movement during zip
                vx = vy = 0;
            }

            // === HORIZONTAL MOVEMENT ===
            if (!stunned) {
                double targetVx = 0;
                double airFric = airborne ? 0.90 : 0.75;
                double accel = airborne ? 0.20 : 0.45;

                if (pressedKeys.contains(leftKey())) {
                    targetVx = -type.speed * speedMultiplier;
                    if (type == BirdType.HUMMINGBIRD && pressedKeys.contains(jumpKey()) && airborne) {
                        targetVx *= 1.75;   // big speed boost while holding jump (flying)
                    }
                }
                else if (pressedKeys.contains(rightKey())) {
                    targetVx = type.speed * speedMultiplier;
                    if (type == BirdType.HUMMINGBIRD && pressedKeys.contains(jumpKey()) && airborne) {
                        targetVx *= 1.75;   // big speed boost while holding jump (flying)
                    }
                }

                vx = vx * airFric + targetVx * accel;
                if (Math.abs(vx) > 0.1) facingRight = vx > 0;

                // Normal jump (ground only)
                boolean canJump = isOnGround() || (type == BirdType.PIGEON && canDoubleJump);
                if (pressedKeys.contains(jumpKey()) && canJump) {
                    double mult = isOnGround() ? 1.0 : 0.75;
                    double jumpPower = type.jumpHeight * mult;
                    if (playerIndex == 3 && BirdGame3.this.isTrialMode) {
                        jumpPower *= 0.4; // boss has weak jumps
                    }
                    vy = -type.jumpHeight * mult;
                    if (!isOnGround() && type == BirdType.PIGEON) canDoubleJump = false;
                    if (BirdGame3.this.swingClip != null) BirdGame3.this.swingClip.play();
                }

                // City rooftop jump tracking
                if (selectedMap == MapType.CITY && pressedKeys.contains(jumpKey()) && canJump && y < GROUND_Y - 500) {
                    rooftopJumps[playerIndex]++;
                    // Sky Cliffs high cliff jump tracking
                    if (selectedMap == MapType.SKYCLIFFS && pressedKeys.contains(jumpKey()) && canJump && y < GROUND_Y - 1000) {
                        highCliffJumps[playerIndex]++;
                        achievementProgress[14]++;
                        if (achievementProgress[14] >= 15 && !achievementsUnlocked[14]) {
                            unlockAchievement(14, "CLIFF DIVER!");
                        }
                    }
                    BirdGame3.this.achievementProgress[10]++;
                    if (BirdGame3.this.achievementProgress[10] >= 20 && !BirdGame3.this.achievementsUnlocked[10]) {
                        BirdGame3.this.unlockAchievement(10, "ROOFTOP RUNNER!");
                    }
                }

                // Attack & Special
                if (pressedKeys.contains(attackKey()) && attackCooldown <= 0 && !isBlocking) {
                    attack();
                    if (BirdGame3.this.butterClip != null) BirdGame3.this.butterClip.play();
                    // Normal birds: fast attacks
                    // Boss: slow, heavy attacks
                    if (playerIndex == 3 && BirdGame3.this.isTrialMode) {
                        attackCooldown = 72;        // 1.2 seconds cooldown (was 0.5s)
                        attackAnimationTimer = 24;  // longer wind-up animation (feels heavier)
                    } else {
                        attackCooldown = 30;        // regular birds keep fast attacks
                        attackAnimationTimer = 12;
                    }
                }
                // Special ability — but block normal special if Vine Grapple is active
                if (pressedKeys.contains(specialKey())) {
                    if (grappleUses > 0 && !isOnGround() && !isGrappling && specialCooldown <= 0 && !isBlocking) {
                        // grapple code stays the same
                    } else if (grappleUses == 0 && specialCooldown <= 0 && !isBlocking) {
                        special();
                    } else if (!isAI[playerIndex] && specialCooldown > 0) {
                        cooldownFlash = 15;
                    }
                }
            } else {
                vx *= 0.92;
            }


            // Air/ground friction
            if (!pressedKeys.contains(leftKey()) && !pressedKeys.contains(rightKey())) {
                vx *= airborne ? 0.96 : 0.80;
            }

            // === EAGLE DIVE IMPACT ===
            if (type == BirdType.EAGLE && eagleDiveCountdown > 0) {
                eagleDiveCountdown--;
                if (eagleDiveCountdown == 0) {
                    shakeIntensity = 60;
                    hitstopFrames = 35;
                    addToKillFeed("KABOOOOOM!!! " + name.split(":")[0].trim() + " OBLITERATES THE GROUND!");

                    // Massive explosion visuals
                    for (int i = 0; i < 300; i++) {
                        double angle = i / 300.0 * Math.PI * 2;
                        double speed = 12 + Math.random() * 25;
                        double vx = Math.cos(angle) * speed;
                        double vy = Math.sin(angle) * speed - 10;
                        Color c = Math.random() < 0.5 ? Color.ORANGERED : Color.YELLOW.brighter();
                        particles.add(new Particle(x + 40, y + 70, vx, vy, c));
                    }

                    // Ground shatter effect
                    for (int i = 0; i < 20; i++) {
                        double offset = (Math.random() - 0.5) * 800;
                        for (int j = 0; j < 15; j++) {
                            particles.add(new Particle(x + 40 + offset + j * 10, GROUND_Y + j * 10,
                                    (Math.random() - 0.5) * 25, -6 - Math.random() * 15, Color.SADDLEBROWN.darker()));
                        }
                    }

                    // HUGE DAMAGE + KNOCKBACK
                    for (Bird other : players) {
                        if (other == null || other == this || other.health <= 0) continue;
                        if (BirdGame3.this.isTrialMode) {
                            boolean attackerIsFriendly = playerIndex <= 2;
                            boolean targetIsFriendly = other.playerIndex <= 2;
                            if (attackerIsFriendly == targetIsFriendly) continue;
                        }

                        double dx = other.x - x;
                        double dy = (other.y + 40) - (y + 70);
                        double dist = Math.hypot(dx, dy);

                        if (dist < 350) { // MUCH bigger radius
                            int dmg = (int) (65 * (1.0 - dist / 500.0));
                            if (dmg < 20) dmg = 20;

                            double oldHealth = other.health;
                            other.health = Math.max(0, other.health - dmg);
                            damageDealt[playerIndex] += (int) (oldHealth - other.health);
                            if (other.health <= 0 && oldHealth > 0) eliminations[playerIndex]++;

                            // Massive knockback
                            other.vx += dx / dist * 50;
                            other.vy -= 35;

                            String intensity = dmg >= 80 ? "ANNIHILATED" : dmg >= 60 ? "DEVASTATED" : dmg >= 40 ? "BLASTED" : "SMASHED";
                            addToKillFeed(name.split(":")[0].trim() + " " + intensity + " " + other.name.split(":")[0].trim() + "! -" + dmg + " HP");

                            // Extra particles on hit
                            if (dmg > 40) {
                                for (int k = 0; k < 60; k++) {
                                    double angle = Math.random() * Math.PI * 2;
                                    particles.add(new Particle(other.x + 40, other.y + 40,
                                            Math.cos(angle) * (10 + Math.random() * 20),
                                            Math.sin(angle) * (10 + Math.random() * 20) - 10,
                                            Color.CRIMSON.brighter()));
                                }
                            }
                        }
                    }
                }
            }

// === RAZORBILL BLADE STORM LOGIC ===
            if (type == BirdType.RAZORBILL && bladeStormFrames > 0) {
                bladeStormFrames--;
                int slashTimer = 180 - bladeStormFrames; // rough timer, increments each frame

                if ((180 - bladeStormFrames) % 12 == 0) {  // slash every 12 frames
                    for (Bird other : players) {
                        if (other == null || other == this || other.health <= 0) continue;
                        double dx = other.x - x;
                        double dy = other.y - y;
                        double dist = Math.hypot(dx, dy);
                        if (dist < 160) {
                            int dmg = 14 + random.nextInt(9);
                            double oldHealth = other.health;
                            other.health = Math.max(0, other.health - dmg);
                            damageDealt[playerIndex] += (int) (oldHealth - other.health);
                            if (other.health <= 0 && oldHealth > 0) eliminations[playerIndex]++;
                            other.vx += dx / dist * 32;
                            other.vy -= 20;
                            String verb = dmg >= 25 ? "GASHED" : "CUT";
                            addToKillFeed(name.split(":")[0].trim() + " " + verb + " " +
                                    other.name.split(":")[0].trim() + "! -" + dmg + " HP");

                            for (int k = 0; k < 25; k++) {
                                double angle = Math.atan2(dy, dx) + (Math.random() - 0.5) * 1.4;
                                particles.add(new Particle(other.x + 40, other.y + 40,
                                        Math.cos(angle) * (8 + Math.random() * 16),
                                        Math.sin(angle) * (8 + Math.random() * 16) - 8, Color.CYAN));
                            }
                            shakeIntensity = 18;
                            hitstopFrames = 6;
                        }
                    }

                    // Visual slash trails
                    for (int i = 0; i < 10; i++) {
                        double angle = facingRight ? 0 : Math.PI;
                        angle += (Math.random() - 0.5);
                        particles.add(new Particle(x + 40 + Math.cos(angle) * 70,
                                y + 40 + Math.sin(angle) * 70,
                                Math.cos(angle) * 25, Math.sin(angle) * 25,
                                Color.WHITE.deriveColor(0, 1, 1, 0.85)));
                    }
                }

                if (bladeStormFrames <= 0) {
                    sizeMultiplier = 1.0;
                    powerMultiplier = 1.0;
                }
            }

            // Apply velocity
            x += vx;
            y += vy;

            // SKYCLIFFS THERMAL UPDRAFTS - only if directly inside the gust
            if (selectedMap == MapType.SKYCLIFFS || selectedMap == MapType.VIBRANT_JUNGLE) {
                for (WindVent v : windVents) {
                    if (v.cooldown > 0) continue;  // ← skip inactive vents
                    double centerX = v.x + v.w / 2;
                    double centerY = v.y - 75;
                    double dx = (x + 40) - centerX;
                    double dy = (y + 40) - centerY;
                    if (Math.pow(dx / (v.w / 2 + 50), 2) + Math.pow(dy / 200, 2) <= 1.0) {
                        vy = Math.min(vy, WIND_FORCE);
                        if (Math.random() < 0.3) {
                            particles.add(new Particle(x + 40 + (Math.random() - 0.5) * 40, y + 80, (Math.random() - 0.5) * 8, -4 - Math.random() * 8, Color.CYAN.deriveColor(0, 1, 1, 0.7)));
                        }
                        break;
                    }
                }
            }

// === SIDE BOUNDARIES ===
            if (x < 50) x = 50;
            if (x > WORLD_WIDTH - 150 * sizeMultiplier) x = WORLD_WIDTH - 150 * sizeMultiplier;

            // === LEFT/RIGHT KILL PLANES (out of bounds death) ===
            if (x < -300 || x > WORLD_WIDTH + 300) {
                health = Math.max(0, health - 50); // lose 50 HP
                if (health <= 0) {
                    addToKillFeed(name.split(":")[0].trim() + " FLEW INTO THE VOID!");
                } else {
                    addToKillFeed(name.split(":")[0].trim() + " went out of bounds... -50 HP");
                }
                if (zombieFallingClip != null) zombieFallingClip.play();
                // Respawn safely in the middle
                x = 2000 + playerIndex * 600;
                y = GROUND_Y - 400;
                vx = 0;
                vy = 0;
                canDoubleJump = true;

                // Extra feedback for trial mode
                if (BirdGame3.this.isTrialMode) {
                    shakeIntensity = 20;
                    hitstopFrames = 15;
                }
            }

// === CEILING (NO FLYING INTO SPACE!) ===
            if (y < CEILING_Y) {
                y = CEILING_Y;
                vy = Math.max(vy, 0); // stop upward momentum
                if (type == BirdType.VULTURE) isFlying = false; // stop sustained flight
            }

// === GROUND & PLATFORM COLLISION (only when falling) ===
            handleVerticalCollision();

// === FALLING OFF THE BOTTOM OF THE WORLD ===
            if (y > WORLD_HEIGHT + 300) {
                falls[playerIndex]++;
                health = Math.max(0, health - 50);  // lose 50 HP instead of instant death
                if (health <= 0) {
                    addToKillFeed(name.split(":")[0].trim() + " FELL TO THEIR DOOM!");
                } else {
                    addToKillFeed(name.split(":")[0].trim() + " fell... but survived! -50 HP");
                }
                if (zombieFallingClip != null) zombieFallingClip.play();
                // Respawn at safe location
                x = 1000 + playerIndex * 800;
                y = GROUND_Y - 300;
                vx = vy = 0;
                BirdGame3.this.achievementProgress[7]++;
                if (BirdGame3.this.achievementProgress[7] >= 3 && !BirdGame3.this.achievementsUnlocked[7]) {
                    BirdGame3.this.unlockAchievement(7, "FALL GUY!");
                }
            }

            // Vulture passive feast
            if (type == BirdType.VULTURE && health > 0) {
                for (Bird b : players) {
                    if (b != null && b != this && b.health <= 0 && b.y > HEIGHT + 50 && b.y <= HEIGHT + 100) {
                        health = Math.min(100, health + 8);
                        addToKillFeed(name.split(":")[0].trim() + " FEASTS! +8 HP");
                        for (int i = 0; i < 15; i++) {
                            double angle = Math.random() * Math.PI * 2;
                            particles.add(new Particle(b.x + 40, b.y + 40,
                                    Math.cos(angle) * 4, Math.sin(angle) * 4 - 3, Color.DARKRED));
                        }
                    }
                }
            }

            // Power-up pickup
            for (Iterator<PowerUp> it = powerUps.iterator(); it.hasNext(); ) {
                PowerUp p = it.next();
                if (Math.abs(x + 40 - p.x) < 80 && Math.abs(y + 40 - p.y) < 80) {
                    switch (p.type) {
                        case HEALTH -> {
                            health = Math.min(100, health + 40);
                            addToKillFeed(name.split(":")[0].trim() + " grabbed HEALTH! +40 HP");
                            it.remove();
                        }
                        case SPEED -> {
                            speedMultiplier = 1.7;
                            speedTimer = 480;
                            addToKillFeed(name.split(":")[0].trim() + " got SPEED BOOST!");
                            it.remove();
                        }
                        case RAGE -> {
                            powerMultiplier = 2.0;
                            rageTimer = 420;
                            addToKillFeed(name.split(":")[0].trim() + " is ENRAGED!");
                            it.remove();
                        }
                        case SHRINK -> {
                            for (Bird b : players) {
                                if (b != null && b != this) {
                                    b.sizeMultiplier = 0.6;
                                    b.shrinkTimer = 360;
                                }
                            }
                            addToKillFeed(name.split(":")[0].trim() + " SHRANK everyone!");
                            it.remove();
                        }
                        case NEON -> {
                            // NEW CITY POWER-UP - now much more epic
                            speedMultiplier = 2.4;      // even faster than speed boost
                            speedTimer = 360;
                            canDoubleJump = true;       // refresh double jump
                            vy = -28;                   // huge upward launch
                            powerMultiplier = 1.3;      // slight damage boost while active
                            rageTimer = 360;

                            addToKillFeed(name.split(":")[0].trim() + " grabbed NEON BOOST! HYPERSPEED!");

                            // Big neon explosion + trail startup
                            for (int i = 0; i < 80; i++) {
                                double angle = Math.random() * Math.PI * 2;
                                double speed = 10 + Math.random() * 20;
                                Color c = Math.random() < 0.5 ? Color.MAGENTA.brighter() : Color.CYAN.brighter();
                                particles.add(new Particle(x + 40, y + 40, Math.cos(angle) * speed, Math.sin(angle) * speed - 8, c));
                            }

                            shakeIntensity = 20;
                            hitstopFrames = 12;

                            neonPickups[playerIndex]++;
                            scores[playerIndex] += 20;
                            BirdGame3.this.achievementProgress[11]++;
                            if (BirdGame3.this.achievementProgress[11] >= 8 && !BirdGame3.this.achievementsUnlocked[11]) {
                                BirdGame3.this.unlockAchievement(11, "NEON ADDICT!");
                            }
                            it.remove();
                        }
                        case THERMAL -> {
                            thermalTimer = 600;  // 10 seconds
                            thermalLift = 1.2;   // strong extra upward force
                            addToKillFeed(name.split(":")[0].trim() + " rides a THERMAL! SOARING!");
                            // Epic golden burst
                            for (int i = 0; i < 100; i++) {
                                double angle = Math.random() * Math.PI * 2;
                                double speed = 8 + Math.random() * 18;
                                particles.add(new Particle(x + 40, y + 40,
                                        Math.cos(angle) * speed,
                                        Math.sin(angle) * speed - 10,
                                        Color.GOLD.brighter()));
                            }
                            shakeIntensity = 15;
                            hitstopFrames = 10;

                            thermalPickups[playerIndex]++;
                            achievementProgress[13]++;

                            it.remove();
                        }
                        case VINE_GRAPPLE -> {
                            grappleTimer = 480; // 8 seconds
                            grappleUses = 3;
                            addToKillFeed(name.split(":")[0].trim() + " grabbed VINE GRAPPLE! Swing freely!");
                            // Epic green vine explosion
                            for (int i = 0; i < 80; i++) {
                                double angle = Math.random() * Math.PI * 2;
                                double speed = 8 + Math.random() * 16;
                                particles.add(new Particle(x + 40, y + 40,
                                        Math.cos(angle) * speed, Math.sin(angle) * speed - 6,
                                        Color.LIMEGREEN.brighter()));
                            }
                            shakeIntensity = 18;
                            hitstopFrames = 10;
                            vineGrapplePickups[playerIndex]++;
                            achievementProgress[16]++;
                            if (achievementProgress[16] >= 8 && !achievementsUnlocked[16]) {
                                unlockAchievement(16, "VINE SWINGER!");
                            }
                            it.remove();
                        }
                    }

// === POWER-UP HOARDER ACHIEVEMENT (runs for ANY power-up) ===
                    BirdGame3.this.achievementProgress[6]++;
                    if (BirdGame3.this.achievementProgress[6] >= 10 && !BirdGame3.this.achievementsUnlocked[6]) {
                        BirdGame3.this.unlockAchievement(6, "POWER-UP HOARDER!");
                    }
                    BirdGame3.this.checkAchievements(this);
                    // Particles on pickup
                    for (int i = 0; i < 30; i++) {
                        double angle = Math.random() * Math.PI * 2;
                        particles.add(new Particle(p.x, p.y, Math.cos(angle) * 8, Math.sin(angle) * 8 - 4, p.type.color.brighter()));
                    }
                    it.remove();
                }
            }

            // === TAUNTS (Q = cycle, E = perform) — NOW PER-BIRD AND PLAYER-ONLY! ===
            if (!isAI[playerIndex]) {
                if (tauntCooldown > 0) tauntCooldown--;

                KeyCode qKey = playerIndex == 0 ? KeyCode.Q :
                        playerIndex == 1 ? KeyCode.NUMPAD1 :
                                playerIndex == 2 ? KeyCode.NUMPAD2 : KeyCode.NUMPAD3;

                KeyCode eKey = playerIndex == 0 ? KeyCode.E :
                        playerIndex == 1 ? KeyCode.NUMPAD4 :
                                playerIndex == 2 ? KeyCode.NUMPAD5 : KeyCode.NUMPAD6;

                if (pressedKeys.contains(qKey) && tauntCooldown <= 0) {
                    currentTaunt = (currentTaunt % 3) + 1;
                    tauntCooldown = 30;
                }

                if (pressedKeys.contains(eKey) && tauntCooldown <= 0 && currentTaunt != 0) {
                    tauntTimer = 60;
                    tauntCooldown = 120;
                    tauntsPerformed[playerIndex]++;
                    checkAchievements(this);  // Check for Taunt Lord

                    String tauntName = switch (currentTaunt) {
                        case 1 -> "FLIPPED OFF";
                        case 2 -> "CHALLENGED";
                        case 3 -> "MOONED";
                        default -> "TAUNTED";
                    };
                    addToKillFeed(name.split(":")[0].trim() + " " + tauntName + " EVERYONE!");

                    for (int i = 0; i < 30; i++) {
                        Color c = currentTaunt == 1 ? Color.YELLOW : currentTaunt == 2 ? Color.RED : Color.PINK;
                        particles.add(new Particle(x + 40, y + 40, (Math.random() - 0.5) * 16, (Math.random() - 0.7) * 12, c));
                    }
                }
            }

            if (tauntTimer > 0) tauntTimer--;
        }

        void draw(GraphicsContext g) {
            double drawSize = 80 * sizeMultiplier;

            boolean airborne = !isOnGround();

            if (isBlocking) {
                double birdCenterX = x + 40 * sizeMultiplier;
                double birdCenterY = y + 40 * sizeMultiplier;
                double pulse = 0.6 + 0.4 * Math.sin(System.currentTimeMillis() / 200.0);
                g.setFill(Color.BLUE.deriveColor(0, 1, 1, pulse));
                g.fillOval(x - 20 * sizeMultiplier, y - 20 * sizeMultiplier, drawSize + 40 * sizeMultiplier, drawSize + 40 * sizeMultiplier);
                g.setStroke(Color.BLUE.brighter());
                g.setLineWidth(6 * sizeMultiplier);
                double shieldAngleStart = facingRight ? 0 : Math.PI;
                double shieldRadius = drawSize * 0.8;
                g.strokeArc(birdCenterX - shieldRadius, birdCenterY - shieldRadius, shieldRadius * 2, shieldRadius * 2, Math.toDegrees(shieldAngleStart) - 90, 180, javafx.scene.shape.ArcType.OPEN);
                if (Math.random() < 0.5) {
                    double particleAngle = shieldAngleStart + (Math.random() - 0.5) * Math.PI;
                    double px = birdCenterX + Math.cos(particleAngle) * shieldRadius;
                    double py = birdCenterY + Math.sin(particleAngle) * shieldRadius;
                    particles.add(new Particle(px, py, (Math.random() - 0.5) * 4, (Math.random() - 0.5) * 4 - 2, Color.BLUE.brighter()));
                }
            }

            // === UNHINGED TAUNTS (NOW PER-BIRD!) ===
            if (tauntTimer > 0) {
                double prog = tauntTimer / 60.0;
                switch (currentTaunt) {
                    case 1 -> { // MIDDLE FINGER
                        double wingX = facingRight ? x + 90 : x - 40;
                        double wingY = y + 20;
                        g.setFill(Color.BLACK);
                        g.fillRect(wingX - 10, wingY, 60, 15);
                        g.fillOval(wingX + 40, wingY - 20, 30, 50);
                        g.setFill(Color.WHITE);
                        g.setFont(Font.font("Arial Black", 24));
                        g.fillText("FRICK YOU!", wingX + 10, wingY - 30);
                    }
                    case 2 -> { // COME AT ME
                        g.setFill(Color.RED.deriveColor(0, 1, 1, 0.6));
                        g.fillOval(x - 40, y - 60, 160, 100);
                        g.setFill(Color.WHITE);
                        g.setFont(Font.font("Arial Black", 32));
                        g.fillText("COME AT ME", x - 10, y - 10);
                    }
                    case 3 -> { // MOONING
                        g.setFill(Color.PINK.brighter());
                        g.fillOval(x + 10, y + 50, 60, 70);
                        g.setFill(Color.WHITE);
                        g.fillOval(x + 25, y + 65, 15, 20);
                        g.fillOval(x + 45, y + 65, 15, 20);
                        g.setFill(Color.BLACK);
                        g.fillOval(x + 32, y + 75, 8, 8);
                        g.setFont(Font.font("Arial Black", 20));
                        g.fillText("KISS IT", x + 15, y + 120);
                    }
                }
                // pulse effect
                g.setFill(Color.WHITE.deriveColor(0, 1, 1, 0.3 + 0.4 * Math.sin(tauntTimer * 0.5)));
                g.fillOval(x - 30, y - 40, 140, 140);
            }

            if (this.cooldownFlash > 0) {
                g.setFill(Color.RED.deriveColor(0, 1, 1, 0.6));
                g.setFont(Font.font("Arial Black", 32));
                g.fillText("COOLDOWN!", x - 20, y - 60);
                cooldownFlash--;
            }

            if (rageTimer > 0) {
                g.setFill(Color.RED.deriveColor(0, 1, 1, 0.4));
                g.fillOval(x - 20, y - 20, drawSize + 40, drawSize + 40);
            }

            // === THERMAL RISE VISUALS ===
            if (thermalTimer > 0) {
                double pulse = 0.5 + 0.5 * Math.sin(System.currentTimeMillis() / 120.0);
                g.setFill(Color.GOLD.deriveColor(0, 1, 1, 0.4 + 0.3 * pulse));
                g.fillOval(x - 60, y - 60, drawSize + 120, drawSize + 120);
                // Rising sparkles
                if (Math.random() < 0.4) {
                    particles.add(new Particle(x + 40 + (Math.random() - 0.5) * 60,
                            y + 80,
                            (Math.random() - 0.5) * 4,
                            -6 - Math.random() * 8,
                            Color.YELLOW.deriveColor(0, 1, 1, 0.8)));
                }
            }

            // === NEON BOOST VISUALS ===
            if (rageTimer > 0 && speedMultiplier > 2.0) {  // detects Neon active (we reused rageTimer)
                double pulse = 0.5 + 0.5 * Math.sin(System.currentTimeMillis() / 100.0);
                // Pulsing neon glow
                g.setFill(Color.MAGENTA.deriveColor(0, 1, 1 + pulse, 0.6));
                g.fillOval(x - 50, y - 50, drawSize + 100, drawSize + 100);

                // Speed lines trailing behind
                g.setStroke(Color.CYAN.brighter());
                g.setLineWidth(4 + pulse * 4);
                for (int i = 1; i <= 8; i++) {
                    double offset = i * 15;
                    g.strokeLine(x + 40, y + 40,
                            x + 40 - vx * i * 2, y + 40 - vy * i * 2);
                }
            }

            // === OPIUM BIRD VISUALS (ONLY FOR OPIUM BIRD!) ===
            if (type == BirdType.OPIUMBIRD) {
                // Permanent subtle purple glow
                g.setFill(Color.rgb(138, 43, 226, 0.3));
                g.fillOval(x - 30, y - 40, drawSize + 60, drawSize + 80);

                // Dripping lean from beak
                g.setFill(Color.PURPLE.darker());
                for (int i = 0; i < 5; i++) {
                    double offset = Math.sin((System.currentTimeMillis() / 100.0) + i) * 4;
                    g.fillOval(x + 85 + offset, y + 50 + i * 12, 16, 24);
                }

                // Tripping effect when high
                if (highTimer > 0) {
                    double intensity = highTimer / 180.0;
                    g.setFill(Color.MAGENTA.deriveColor(0, 1, 1, 0.3 * intensity));
                    g.fillOval(x - 100, y - 100, drawSize + 200, drawSize + 200);
                    // Wavy body
                    g.setFill(Color.rgb(200, 0, 255));
                    g.fillOval(x + Math.sin(highTimer * 0.3) * 20, y + Math.cos(highTimer * 0.2) * 15, drawSize, drawSize);
                }

                // Active lean cloud (follows bird)
                if (leanTimer > 0) {
                    double cloudAlpha = 0.3 + 0.3 * Math.sin(System.currentTimeMillis() / 200.0);
                    g.setFill(Color.rgb(138, 43, 226, cloudAlpha));
                    g.fillOval(x - 120, y - 100, 300, 300);
                    g.setFill(Color.WHITE);
                    g.setFont(Font.font("Arial Black", 32));
                    g.fillText("LEAN", x + 10, y + 20);
                }

                // Cooldown bar
                if (leanCooldown > 0) {
                    g.setFill(Color.PURPLE.darker());
                    g.fillRoundRect(x - 10, y + 100, 100, 20, 15, 15);
                    g.setFill(Color.WHITE);
                    g.setFont(Font.font("Arial Black", 18));
                    g.fillText("LEAN", x + 15, y + 116);
                }
            }

            if (type == BirdType.TITMOUSE) {
                // Cute tufted crest
                g.setFill(Color.SILVER);
                g.fillOval(x + 20, y - 20, 40, 60); // crest puff

                // Big black eyes
                g.setFill(Color.BLACK);
                g.fillOval(x + 25, y + 15, 25, 25);
                g.fillOval(x + 45, y + 15, 25, 25);
                g.setFill(Color.WHITE);
                g.fillOval(x + 32, y + 20, 10, 10);
                g.fillOval(x + 52, y + 20, 10, 10);

                // Rusty orange flanks
                g.setFill(Color.PERU.brighter());
                g.fillOval(x + 10, y + 40, 60, 40);

                // Zip trail when zipping
                if (isZipping) {
                    g.setStroke(Color.SKYBLUE.brighter());
                    g.setLineWidth(8);
                    g.strokeLine(x + 40, y + 40, zipTargetX + 40, zipTargetY + 40);
                    g.setLineWidth(4);
                    g.setStroke(Color.WHITE);
                    g.strokeLine(x + 40, y + 40, zipTargetX + 40, zipTargetY + 40);
                }
            }

            // === EAGLE SOARING VISUALS (when not diving) ===
            if (type == BirdType.EAGLE && diveTimer == 0 && airborne && vy < 2) {
                // Gentle golden glow when gliding/soaring
                g.setFill(Color.GOLD.deriveColor(0, 1, 1, 0.2));
                g.fillOval(x - 50, y - 50, drawSize + 100, drawSize + 100);

                // Wing trail particles
                if (Math.random() < 0.2) {
                    particles.add(new Particle(x + (facingRight ? -20 : drawSize + 20), y + 40,
                            (facingRight ? 1 : -1) * (2 + Math.random() * 4),
                            (Math.random() - 0.5) * 4,
                            Color.GOLD.brighter()));
                }
            }

            // === EAGLE DIVE BOMB INSANE VISUALS ===
            if (type == BirdType.EAGLE && diveTimer > 0) {

                // Massive red pulsing aura
                g.setFill(Color.CRIMSON.deriveColor(0, 1, 1, 0.6 + 0.4 * Math.sin(diveTimer * 0.5)));
                g.fillOval(x - 80, y - 80, drawSize + 160, drawSize + 160);

                // Speed trail lines (like a comet)
                g.setStroke(Color.ORANGERED);
                g.setLineWidth(8);
                for (int i = 1; i <= 12; i++) {
                    double offset = i * 20;
                    g.strokeLine(
                            x + 40,
                            y + 40,
                            x + 40 - vx * i * 3,
                            y + 40 - vy * i * 3
                    );
                }
                g.setLineWidth(3);
                g.setStroke(Color.YELLOW);
                for (int i = 1; i <= 8; i++) {
                    double offset = i * 15;
                    g.strokeLine(
                            x + 40,
                            y + 40,
                            x + 40 - vx * i * 2.5,
                            y + 40 - vy * i * 2.5
                    );
                }

                // SKREEEEEEE text (only at the start)
                if (diveTimer > 70) {
                    g.setFill(Color.WHITE);
                    g.setFont(Font.font("Arial Black", FontWeight.BOLD, 64));
                    g.setEffect(new DropShadow(20, Color.BLACK));
                    g.fillText("SKREEEEEEEE!!!", x - 180, y - 60);
                    g.setEffect(null);
                }

                // Lightning crackle around the eagle
                g.setStroke(Color.YELLOW);
                g.setLineWidth(4);
                for (int i = 0; i < 6; i++) {
                    double angle = Math.random() * Math.PI * 2;
                    double len = 60 + Math.random() * 40;
                    g.strokeLine(
                            x + 40,
                            y + 40,
                            x + 40 + Math.cos(angle) * len,
                            y + 40 + Math.sin(angle) * len
                    );
                }
            }

            // === RAZORBILL BLADE STORM INSANE VISUALS ===
            if (type == BirdType.RAZORBILL && bladeStormFrames > 0) {
                double spin = System.currentTimeMillis() * 0.02;

                // Spinning blade aura
                g.setStroke(Color.CYAN.brighter());
                g.setLineWidth(6);
                for (int i = 0; i < 8; i++) {
                    double angle = spin + i * Math.PI / 4;
                    double len = 100 + Math.sin(spin * 3) * 30;
                    g.strokeLine(
                            x + 40,
                            y + 40,
                            x + 40 + Math.cos(angle) * len,
                            y + 40 + Math.sin(angle) * len
                    );
                }

                // Glowing core
                g.setFill(Color.WHITE.deriveColor(0, 1, 1, 0.6 + 0.4 * Math.sin(spin * 10)));
                g.fillOval(x - 40, y - 40, drawSize + 80, drawSize + 80);

                // "SLASHING" text flash
                if ((int) (spin * 10) % 20 < 5) {
                    g.setFill(Color.CYAN.brighter());
                    g.setFont(Font.font("Arial Black", FontWeight.BOLD, 48));
                    g.setEffect(new Glow(1.0));
                    g.fillText("SLASHING!", x - 80, y - 60);
                    g.setEffect(null);
                }
            }

            // === SKY KING EAGLE SKIN ===
// Show on player 0 if unlocked, AND always show on the boss (player 3) during the trial
            if (type == BirdType.EAGLE &&
                    ((eagleSkinUnlocked && playerIndex == 0) ||
                            (BirdGame3.this.isTrialMode && playerIndex == 3 && selectedMap == MapType.SKYCLIFFS))) {

                // Golden glowing aura
                g.setFill(Color.GOLD.deriveColor(0, 1, 1, 0.5));
                g.fillOval(x - 40, y - 40, drawSize + 80, drawSize + 80);

                // Crown-like golden crest
                g.setFill(Color.GOLD.brighter());
                g.fillOval(x + 15, y - 35, 50, 70);
                g.setFill(Color.ORANGE.brighter());
                g.fillOval(x + 25, y - 45, 30, 40);

                // Sparkle particles around the king
                if (Math.random() < 0.4) {  // slightly more sparkles for the boss
                    particles.add(new Particle(x + 40 + (Math.random() - 0.5) * 100, y + 40 + (Math.random() - 0.5) * 100,
                            (Math.random() - 0.5) * 5, (Math.random() - 0.5) * 5 - 3, Color.GOLD.brighter()));
                }
            }

            if (type == BirdType.GRINCHHAWK) {
                g.setFill(Color.YELLOW);
                g.fillOval(x + (facingRight ? 55 : 20) * sizeMultiplier, y + 22 * sizeMultiplier, 18 * sizeMultiplier, 18 * sizeMultiplier);
                g.setFill(Color.BLACK);
                g.fillOval(x + (facingRight ? 60 : 25) * sizeMultiplier, y + 25 * sizeMultiplier, 10 * sizeMultiplier, 10 * sizeMultiplier);
            }

            if (type == BirdType.VULTURE) {

                // Main body (dark purple-black)
                g.setFill(Color.rgb(35, 15, 45));
                g.fillOval(x, y, drawSize, drawSize);

                // Big creepy wings (spread when flying or moving fast)
                double wingSpread = isFlying || Math.abs(vx) > 2 ? 1.4 : 1.0;
                g.setFill(Color.rgb(20, 10, 30));
                g.fillOval(x - 30 * wingSpread, y + 10, 50 * wingSpread, 90);           // left wing
                g.fillOval(x + drawSize - 20 * wingSpread, y + 10, 50 * wingSpread, 90); // right wing

                // Bald red head
                g.setFill(Color.rgb(180, 30, 30));
                g.fillOval(x + 15, y + 10, 50, 55);

                // GLOWING RED EYES
                g.setFill(Color.CRIMSON.darker().darker());
                g.fillOval(x + 25, y + 25, 20, 20);
                g.fillOval(x + 45, y + 25, 20, 20);
                g.setFill(Color.RED.brighter());
                g.fillOval(x + 30, y + 30, 10, 10);
                g.fillOval(x + 50, y + 30, 10, 10);

                // Carrion swarm aura when ability active
                if (carrionSwarmTimer > 0) {
                    g.setFill(Color.BLACK.deriveColor(0, 1, 1, 0.6));
                    g.fillOval(x - 40, y - 30, drawSize + 80, drawSize + 100);
                    carrionSwarmTimer--;
                }


            }

            if (stunTime > 0) {
                g.setFill(Color.CYAN.deriveColor(0, 1, 1, 0.7));
                g.setFont(Font.font(28));
                g.fillText("FROZEN!", x + 5, y - 25);
            }

            // === EPIC SPECIAL COOLDOWN BAR ===
            if (specialCooldown > 0 && specialMaxCooldown > 0) {
                double ratio = (double) specialCooldown / specialMaxCooldown;

                // Background
                g.setFill(Color.BLACK.deriveColor(0, 1, 1, 0.8));
                g.fillRoundRect(x - 5, y + 92, 90, 14, 10, 10);

                // Filling bar (red → orange → cyan as it gets ready)
                Color fillColor = ratio > 0.66 ? Color.CRIMSON :
                        ratio > 0.33 ? Color.ORANGE : Color.CYAN.brighter();
                g.setFill(fillColor);
                g.fillRoundRect(x, y + 96, 80 * (1 - ratio), 6, 6, 6);

                // White border glow
                g.setStroke(Color.WHITE);
                g.setLineWidth(2);
                g.strokeRoundRect(x - 5, y + 92, 90, 14, 10, 10);

                // Seconds text
                // Special cooldown text — shows "CROWS" for Vulture while summoning is on cooldown
                g.setFill(Color.WHITE);
                g.setFont(Font.font("Arial Black", 16));

                // === SPECIAL COOLDOWN TEXT (fixed order!) ===
                String cooldownText;
                if (type == BirdType.VULTURE && crowSwarmCooldown > 0) {
                    cooldownText = "CROWS";
                } else if (type == BirdType.OPIUMBIRD && leanCooldown > 0) {
                    cooldownText = "LEAN";
                } else if (type == BirdType.MOCKINGBIRD && specialCooldown > 0) {
                    cooldownText = "LOUNGE";
                } else if (type == BirdType.PIGEON && specialCooldown > 0) {
                    cooldownText = "HEAL";                     // ← NEW: Pigeon shows "HEAL"
                } else if (specialCooldown > 0) {
                    cooldownText = (int) Math.ceil(specialCooldown / 60.0) + "s";
                } else {
                    cooldownText = "";
                }
                g.fillText(cooldownText, x + 20, y + 104);
            }

            if (type == BirdType.MOCKINGBIRD && loungeActive && loungeHealth > 0) {
                // Flash red when damaged
                Color loungeColor = loungeDamageFlash > 0 ? Color.RED : Color.LIME;
                g.setFill(loungeColor.deriveColor(0, 1, 1, 0.7));
                g.fillRoundRect(loungeX - 60, loungeY - 40, 120, 80, 30, 30);
                g.setStroke(loungeDamageFlash > 0 ? Color.ORANGERED : Color.DARKGREEN);
                g.setLineWidth(loungeDamageFlash > 0 ? 8 : 5);
                g.strokeRoundRect(loungeX - 60, loungeY - 40, 120, 80, 30, 30);

                // HP Bar above lounge
                g.setFill(Color.BLACK);
                g.fillRect(loungeX - 65, loungeY - 75, 130, 18);
                g.setFill(Color.RED.darker());
                g.fillRect(loungeX - 60, loungeY - 70, 120, 12);
                g.setFill(Color.LIME);
                g.fillRect(loungeX - 60, loungeY - 70, 120 * (loungeHealth / (double) LOUNGE_MAX_HEALTH), 12);

                g.setFill(Color.WHITE);
                g.setFont(Font.font("Arial Black", 20));
                g.fillText(loungeHealth + " HP", loungeX - 48, loungeY - 55);

                g.setFill(Color.BLACK);
                g.setFont(Font.font("Arial Black", 24));
                g.fillText("LOUNGE", loungeX - 52, loungeY + 8);

                if (loungeDamageFlash > 0) loungeDamageFlash--;
            }

            g.setFill(type.color);
            g.fillOval(x, y, drawSize, drawSize);
            g.setFill(type.color.brighter());
            g.fillOval(facingRight ? x + 50 * sizeMultiplier : x - 20 * sizeMultiplier, y + 20 * sizeMultiplier, 50 * sizeMultiplier, 40 * sizeMultiplier);
            g.setFill(Color.WHITE);
            g.fillOval(x + (facingRight ? 50 : 20) * sizeMultiplier, y + 20 * sizeMultiplier, 25 * sizeMultiplier, 25 * sizeMultiplier);
            g.setFill(Color.BLACK);
            g.fillOval(x + (facingRight ? 55 : 25) * sizeMultiplier, y + 25 * sizeMultiplier, 15 * sizeMultiplier, 15 * sizeMultiplier);

            // === CITY PIGEON ALTERNATE SKIN ===
            if (type == BirdType.PIGEON && isCitySkin) {
                // Fedora hat
                g.setFill(Color.DARKGRAY.darker());
                g.fillRoundRect(x + 20, y - 10, 40, 20, 10, 10);
                g.fillRect(x + 10, y - 5, 60, 8);

                // Cigarette
                g.setFill(Color.WHITE);
                g.fillRect(facingRight ? x + 85 : x - 15, y + 45, 20, 4);
                g.setFill(Color.ORANGE.brighter());
                g.fillRect(facingRight ? x + 105 : x - 35, y + 45, 8, 4);

                // City Pigeon cigarette smoke — more puffs, wispy
                if (Math.random() < 0.7) { // more frequent
                    double smokeX = facingRight ? x + 110 : x - 20;
                    double smokeY = y + 40 + Math.random() * 12;
                    particles.add(new Particle(
                            smokeX,
                            smokeY,
                            (Math.random() - 0.5) * 3,
                            -1.5 - Math.random() * 2,
                            Color.LIGHTGRAY.deriveColor(0, 1, 1, 0.3 + Math.random() * 0.4)
                    ));
                }
            }

            // === ANIMATED ATTACKING BEAK ===
            boolean isAttacking = attackAnimationTimer > 0;
            double openAmount = isAttacking ? (16 + Math.sin(attackAnimationTimer * 0.7) * 10) * sizeMultiplier : 3 * sizeMultiplier;
            double beakBaseY = y + 45 * sizeMultiplier;
            double beakLength = 28 * sizeMultiplier;

            g.setFill(isAttacking ? Color.ORANGERED : Color.ORANGE);

            if (facingRight) {
                double tipX = x + 80 * sizeMultiplier + beakLength;

                // Upper beak
                g.fillPolygon(
                        new double[]{x + 80 * sizeMultiplier, tipX, x + 80 * sizeMultiplier},
                        new double[]{beakBaseY - 8 * sizeMultiplier, beakBaseY - openAmount, beakBaseY + 8 * sizeMultiplier},
                        3
                );
                // Lower beak (opens downward)
                g.fillPolygon(
                        new double[]{x + 80 * sizeMultiplier, tipX, x + 80 * sizeMultiplier},
                        new double[]{beakBaseY - 8 * sizeMultiplier, beakBaseY + openAmount * 1.6, beakBaseY + 8 * sizeMultiplier},
                        3
                );

                // Optional: tongue flash when fully open
                if (isAttacking && attackAnimationTimer > 4) {
                    g.setFill(Color.DEEPPINK.darker());
                    g.fillOval(tipX - 12, beakBaseY - 4, 20, 14);
                }
            } else {
                double tipX = x - beakLength;

                // Upper beak (left-facing)
                g.fillPolygon(
                        new double[]{x, tipX, x},
                        new double[]{beakBaseY - 8 * sizeMultiplier, beakBaseY - openAmount, beakBaseY + 8 * sizeMultiplier},
                        3
                );
                // Lower beak
                g.fillPolygon(
                        new double[]{x, tipX, x},
                        new double[]{beakBaseY - 8 * sizeMultiplier, beakBaseY + openAmount * 1.6, beakBaseY + 8 * sizeMultiplier},
                        3
                );

                if (isAttacking && attackAnimationTimer > 4) {
                    g.setFill(Color.DEEPPINK.darker());
                    g.fillOval(tipX - 8, beakBaseY - 4, 20, 14);
                }
            }

            // Big white flash on the very first attack frame — much stronger for the boss!
            int flashFrame = (playerIndex == 3 && BirdGame3.this.isTrialMode) ? 24 : 12;
            if (attackAnimationTimer == flashFrame) {
                double flashOpacity = (playerIndex == 3 && BirdGame3.this.isTrialMode) ? 0.9 : 0.7;
                double flashSize = (playerIndex == 3 && BirdGame3.this.isTrialMode) ? 60 : 36;
                g.setFill(Color.WHITE.deriveColor(0, 1, 1, flashOpacity));
                g.fillOval(
                        facingRight ? x + 90 * sizeMultiplier : x - 40 * sizeMultiplier,
                        y + 30 * sizeMultiplier,
                        flashSize * sizeMultiplier,
                        flashSize * sizeMultiplier
                );
            }

            if (type == BirdType.PELICAN) {
                double headX = facingRight ? x + 50 * sizeMultiplier : x - 20 * sizeMultiplier;
                double pouchX = headX + 2 * sizeMultiplier;
                double pouchY = y + 42 * sizeMultiplier;
                double pouchW = (plungeTimer > 0 ? 62 : 46) * sizeMultiplier;
                double pouchH = (plungeTimer > 0 ? 38 : 28) * sizeMultiplier;
                g.setFill(Color.rgb(255, 180, 80));
                g.fillOval(pouchX, pouchY, pouchW, pouchH);
                g.setFill(Color.rgb(255, 200, 100));
                g.fillOval(pouchX + 5 * sizeMultiplier, pouchY + 4 * sizeMultiplier, pouchW - 12 * sizeMultiplier, pouchH - 12 * sizeMultiplier);
            }

            // === VINE GRAPPLE USES COUNTER ABOVE HEAD ===
            if (grappleUses > 0) {
                g.setFill(Color.LIMEGREEN.brighter());
                g.setFont(Font.font("Arial Black", FontWeight.BOLD, 36 * sizeMultiplier));
                g.setEffect(new Glow(0.8));
                g.setStroke(Color.BLACK);
                g.setLineWidth(4 * sizeMultiplier);
                String usesText = String.valueOf(grappleUses);
                double textWidth = g.getFont().getSize() * usesText.length() * 0.55; // rough width estimate
                // Shadow/stroke for readability
                g.strokeText(usesText, x + 40 - textWidth / 2, y - 60);
                // Main bright text
                g.setFill(Color.LIMEGREEN.brighter());
                g.fillText(usesText, x + 40 - textWidth / 2, y - 60);
                g.setEffect(null);

                // Optional: small vine icon below the number
                g.setFill(Color.FORESTGREEN.darker());
                g.fillOval(x + 25, y - 45, 30 * sizeMultiplier, 40 * sizeMultiplier);
                g.setFill(Color.LIMEGREEN);
                for (int i = 0; i < 3; i++) {
                    double leafAngle = i * Math.PI * 2 / 3;
                    g.fillOval(x + 40 + Math.cos(leafAngle) * 20 * sizeMultiplier,
                            y - 40 + Math.sin(leafAngle) * 20 * sizeMultiplier,
                            16 * sizeMultiplier, 24 * sizeMultiplier);
                }
            }
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
                    int damage = (c.owner != null) ? 2 : 5;
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
            if (c.age > 2400 || c.x < -1500 || c.x > WORLD_WIDTH + 1500 || c.y < -1500 || c.y > WORLD_HEIGHT + 1500) {
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

        // === ACHIEVEMENTS AND TRIALS BUTTONS SIDE-BY-SIDE ===
        HBox menuButtons = new HBox(60);
        menuButtons.setAlignment(Pos.CENTER);

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

        menuButtons.getChildren().addAll(achievementsBtn, trialsBtn);
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

                // === CITY PIGEON SKIN ===
                if (bt == BirdType.PIGEON && cityPigeonUnlocked) {
                    if ("CITY_PIGEON".equals(currentData)) {
                        // Switch back to normal
                        selected.setText("Selected: Pigeon");
                        box.setUserData(BirdType.PIGEON);
                        b.setStyle(baseStyle);
                    } else {
                        // Switch to City Pigeon skin
                        selected.setText("Selected: City Pigeon (Skin)");
                        box.setUserData("CITY_PIGEON");
                        b.setStyle("-fx-background-color: #FFD700; -fx-text-fill: black; -fx-font-weight: bold;");
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

    private void addToKillFeed(String message) {
        killFeed.add(0, message);  // newest on top
        if (killFeed.size() > MAX_FEED_LINES) {
            killFeed.remove(killFeed.size() - 1);
        }
    }

    private void triggerFlash(double whiteIntensity, boolean isKill) {
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
        Arrays.fill(players, null);
        for (int i = 0; i < activePlayers; i++) {
            VBox selectorBox = (VBox) playerSlots[i].getChildren().get(2);
            Object data = selectorBox.getUserData();

            BirdType type;
            boolean useCitySkin = false;
            boolean useSkyKingSkin = false;  // NEW

            if (data instanceof String) {
                if ("CITY_PIGEON".equals(data)) {
                    type = BirdType.PIGEON;
                    useCitySkin = true;
                } else if ("SKY_KING_EAGLE".equals(data)) {
                    type = BirdType.EAGLE;
                    useSkyKingSkin = true;  // We don't need a flag in Bird class — the draw() method already checks eagleSkinUnlocked
                } else {
                    // Fallback — shouldn't happen
                    type = BirdType.PIGEON;
                }
            } else {
                type = (BirdType) data;
            }

            double startX = 300 + i * (WIDTH - 600) / Math.max(1, activePlayers - 1);
            players[i] = new Bird(startX, type, i);

            if (useCitySkin) {
                players[i].isCitySkin = true;
            }
            // Sky King skin is already handled in draw() via eagleSkinUnlocked + playerIndex == 0
        }

        platforms.clear();
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

            // Add wind vents on main rooftops (for bursts)
            for (double bx : buildingX) {
                windVents.add(new WindVent(bx - 350 + 100, GROUND_Y - 320, 500)); // left side of rooftop
                windVents.add(new WindVent(bx - 350 + 500, GROUND_Y - 320, 100)); // right side
            }
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

                if (uiDirty || flashTimer > 0 || !killFeed.isEmpty()) {  // ← only clear if changed
                    ui.clearRect(0, 0, WIDTH, HEIGHT);
                    uiDirty = false;  // reset after draw
                }

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
                uiDirty = true;
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
        eagleSkinUnlocked = prefs.getBoolean("skin_eagle", false);
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
        try {
            prefs.flush(); // Write changes to disk
        } catch (BackingStoreException e) {
            e.printStackTrace(); // Safe to just log — game can still run without saving
        }
        prefs.putBoolean("skin_citypigeon", cityPigeonUnlocked);
        prefs.putBoolean("skin_eagle", eagleSkinUnlocked);
        for (int i = 0; i < 4; i++) {
            prefs.putInt("vine_pickups_" + i, vineGrapplePickups[i]);
            prefs.putInt("jungle_wins_" + i, jungleWins[i]);
        }
    }

    private void checkAchievements(Bird bird) {
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