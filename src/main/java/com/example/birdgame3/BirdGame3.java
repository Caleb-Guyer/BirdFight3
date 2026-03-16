package com.example.birdgame3;

import javafx.animation.Animation;
import javafx.animation.AnimationTimer;
import javafx.animation.KeyFrame;
import javafx.animation.PauseTransition;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.Point2D;
import javafx.geometry.Bounds;
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
import javafx.scene.control.TextField;
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
import javafx.scene.layout.FlowPane;
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
import javafx.scene.shape.Line;
import javafx.scene.shape.Polygon;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.StrokeLineCap;
import javafx.scene.transform.Rotate;
import javafx.geometry.Rectangle2D;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.util.*;
import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.URL;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;
import java.util.function.BooleanSupplier;

public class BirdGame3 extends Application {

    public static final int WIDTH = 1920;
    public static final int HEIGHT = 1080;
    public static final int GROUND_Y = 2400;
    public static final double GRAVITY = 0.75;
    private static final int MATCH_DURATION_FRAMES = 90 * 60;
    private static final int COMPETITION_DURATION_FRAMES = 120 * 60;
    private static final Insets MENU_PADDING = new Insets(60, 80, 60, 80);
    private static final double MENU_GAP = 24;
    private static final double MENU_TEXT_MAX_WIDTH = 1100;

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
    private final Button[] teamButtons = new Button[4];
    private final Button[] cpuButtons = new Button[4];
    private Button teamModeToggleButton;
    private boolean teamModeEnabled = false;
    private final int[] playerTeams = new int[]{1, 2, 1, 2};
    private final Random random = new Random();
    private long lastPowerUpSpawnTime = 0;
    public static final long POWERUP_SPAWN_INTERVAL = 1_000_000_000L * 8; // every 8 seconds
    private static final int MUTATOR_BUFF_FRAMES = COMPETITION_DURATION_FRAMES * 2;
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
    public int[] leanTime = new int[4];         // Opium/Heisenbird cloud active frames
    public int[] tauntsPerformed = new int[4];
    private final int[] loungeAchievementSnapshot = new int[4];
    private final int[] leanAchievementSnapshot = new int[4];

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
    private final Deque<UnlockCard> pendingUnlockCards = new ArrayDeque<>();

    // === LAN MODE ===
    private static final int LAN_MAX_PLAYERS = 4;
    private static final long LAN_SNAPSHOT_INTERVAL_NS = 16_666_666L;
    private static final int LAN_RESULTS_ACTION_LOBBY = 1;
    private static final int LAN_RESULTS_ACTION_EXIT = 2;
    private static final int LAN_RESULTS_MIN_SHOW_MS = 1200;
    private static final int LAN_RESULTS_CLOSE_DELAY_MS = 800;
    private boolean lanModeActive = false;
    private boolean lanIsHost = false;
    private boolean lanIsClient = false;
    private boolean lanMatchActive = false;
    private int lanPlayerIndex = -1;
    private LanHostServer lanHost;
    private LanClient lanClient;
    private final BirdType[] lanSelectedBirds = new BirdType[LAN_MAX_PLAYERS];
    private final String[] lanSelectedSkinKeys = new String[LAN_MAX_PLAYERS];
    private final boolean[] lanRandomBirds = new boolean[LAN_MAX_PLAYERS];
    private final boolean[] lanSlotConnected = new boolean[LAN_MAX_PLAYERS];
    private final boolean[] lanReady = new boolean[LAN_MAX_PLAYERS];
    private final MapType[] lanMapVotes = new MapType[LAN_MAX_PLAYERS];
    private final boolean[] lanMapVoteRandom = new boolean[LAN_MAX_PLAYERS];
    private final int[] lanInputMasks = new int[LAN_MAX_PLAYERS];
    private final int[] lanLastInputMasks = new int[LAN_MAX_PLAYERS];
    private final Object lanInputLock = new Object();
    private int lanLocalInputMask = 0;
    private MapType lanSelectedMap = MapType.FOREST;
    private boolean lanSelectedMapRandom = false;
    private long lanMatchSeed = 0L;
    private long lastLanSnapshotNs = 0L;
    private Label[] lanSlotLabels;
    private Label lanStatusLabel;
    private Label lanYourBirdLabel;
    private Label lanMapLabel;
    private Label lanMapVoteLabel;
    private Label lanCountdownLabel;
    private Canvas[] lanPortraits;
    private Button lanStartButton;
    private Button lanPrevBirdButton;
    private Button lanNextBirdButton;
    private Button lanPrevMapButton;
    private Button lanNextMapButton;
    private Button lanSelectBirdButton;
    private Button lanSelectMapButton;
    private Button lanReadyButton;
    private Timeline lanCountdownTimeline;
    private int lanCountdownValue = 0;
    private String lanLastHost = "";
    private LanState pendingLanState;
    private boolean lanResultsActionPending = false;
    private Label lanResultsStatusLabel;
    private int lanVoteSignature = 0;

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

    private enum BirdBookCategory { ITEMS, POWERUPS, BIRDS, SKINS, MAPS }

    public MapType selectedMap = MapType.FOREST; // default
    private boolean caveMapUnlocked = false;
    private boolean battlefieldMapUnlocked = false;
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

    public Canvas uiCanvas;
    public GraphicsContext ui;

    private double flashAlpha = 0.0;     // white flash intensity
    private double redFlashAlpha = 0.0;  // red tint for normal big hits
    private int flashTimer = 0;          // frames remaining for flash
    private final UIFactory uiFactory = new UIFactory(this::playButtonClick, this::applyNoEllipsis, this::fitMainMenuButtonSingleLine);
    private Runnable stageSelectReturn = null;
    private java.util.function.Consumer<MapType> stageSelectHandler = null;
    private Runnable stageSelectRandomHandler = null;
    private Runnable settingsReturn = null;
    private boolean musicEnabled = true;
    private boolean sfxEnabled = true;
    private boolean screenShakeEnabled = true;
    private boolean fullscreenEnabled = true;
    private boolean particleEffectsEnabled = true;
    private boolean ambientEffectsEnabled = true;
    private int fpsCap = 60;
    private long lastRenderNs = 0L;
    private static final int[] FPS_CAPS = new int[]{30, 60, 90, 120, 144, 0};

    public void playHitSound(double intensity) {
        if (!sfxEnabled) return;
        if (bonkClip != null) {
            bonkClip.setVolume(Math.min(1.0, intensity / 40.0));
            bonkClip.play();
        }
    }

    public boolean isSfxEnabled() {
        return sfxEnabled;
    }

    private String resourceUrl(String path) {
        URL url = getClass().getResource(path);
        if (url == null) {
            throw new IllegalStateException("Missing resource: " + path);
        }
        return url.toExternalForm();
    }

    private void loadSounds() {
        try {
            String p = "/sounds/";
            bonkClip = new AudioClip(resourceUrl(p + "bonk.mp3"));
            butterClip = new AudioClip(resourceUrl(p + "butter.mp3"));
            jalapenoClip = new AudioClip(resourceUrl(p + "jalapeno.mp3"));
            swingClip = new AudioClip(resourceUrl(p + "swing.mp3"));
            hugewaveClip = new AudioClip(resourceUrl(p + "hugewave.mp3"));

            // === NEW MENU & VICTORY MUSIC ===
            menuMusicPlayer = new MediaPlayer(new Media(resourceUrl(p + "choose_your_seeds.mp3")));
            menuMusicPlayer.setCycleCount(MediaPlayer.INDEFINITE);
            menuMusicPlayer.setVolume(0.55);

            victoryMusicPlayer = new MediaPlayer(new Media(resourceUrl(p + "finalfanfare.mp3")));
            victoryMusicPlayer.setVolume(0.75);

            // === WIIMOTE BUTTON CLICK ===
            buttonClickClip = new AudioClip(resourceUrl(p + "buttonclick.mp3"));
            buttonClickClip.setVolume(0.9);

            zombieFallingClip = new AudioClip(resourceUrl(p + "zombie-falling.mp3"));

        } catch (Exception e) {
            System.err.println("Sounds not found - put mp3 files in 'sounds' folder inside src/main/resources");
            System.err.println("Sound load error: " + e.getMessage());
        }
    }

    private void startMusic() {
        // STOP menu + victory BEFORE starting map music
        if (menuMusicPlayer != null) menuMusicPlayer.stop();
        if (victoryMusicPlayer != null) victoryMusicPlayer.stop();
        if (musicPlayer != null) musicPlayer.stop();
        if (!musicEnabled) return;

        boolean bossMusic = isBossEncounterActive();
        String file = bossMusic
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
            Media media = new Media(resourceUrl("/sounds/" + file));
            musicPlayer = new MediaPlayer(media);
            musicPlayer.setCycleCount(MediaPlayer.INDEFINITE);
            musicPlayer.setVolume(0.45);
            musicPlayer.play();
        } catch (Exception e) {
            System.out.println("Music not found: " + file);
        }
    }

    private boolean isBossEncounterActive() {
        if (classicModeActive && classicEncounter != null && classicEncounter.bossFight) return true;
        if (storyModeActive) {
            StoryChapter[] chapters = activeStoryChapters();
            if (chapters != null && storyChapterIndex >= 0 && storyChapterIndex < chapters.length) {
                if (isBossName(chapters[storyChapterIndex].opponentName)) return true;
            }
        }
        if (adventureModeActive) {
            AdventureBattle battle = currentAdventureBattle != null ? currentAdventureBattle : activeAdventureBattle();
            if (battle != null && isBossName(battle.opponentName)) return true;
        }
        return false;
    }

    private boolean isBossName(String name) {
        if (name == null) return false;
        return name.toLowerCase(Locale.ROOT).contains("boss");
    }

    private void playMenuMusic() {
        if (musicPlayer != null) musicPlayer.stop();
        if (victoryMusicPlayer != null) victoryMusicPlayer.stop();
        if (!musicEnabled) {
            if (menuMusicPlayer != null) menuMusicPlayer.stop();
            return;
        }
        if (menuMusicPlayer != null) {
            MediaPlayer.Status status = menuMusicPlayer.getStatus();
            if (status != MediaPlayer.Status.PLAYING && status != MediaPlayer.Status.STALLED) {
                menuMusicPlayer.play();
            }
        }
    }

    // === WIIMOTE-FRIENDLY MENU NAVIGATION (WASD + Space) ===
    private void setupKeyboardNavigation(Scene scene) {
        fitSceneButtons(scene.getRoot());
        javafx.application.Platform.runLater(() -> fitSceneButtons(scene.getRoot()));
        scene.widthProperty().addListener((obs, oldV, newV) -> fitSceneButtons(scene.getRoot()));
        scene.heightProperty().addListener((obs, oldV, newV) -> fitSceneButtons(scene.getRoot()));
        applyFocusRingStyle(scene);
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
                fullscreenEnabled = s.isFullScreen();
                saveAchievements();
                e.consume();
                return;
            }

            // WASD -> arrows for focus
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

    private void bindEscape(Scene scene, Runnable action) {
        if (scene == null || action == null) return;
        scene.addEventFilter(KeyEvent.KEY_PRESSED, e -> {
            if (e.getCode() == KeyCode.ESCAPE) {
                action.run();
                e.consume();
            }
        });
    }

    private void bindEscape(Scene scene, Button backButton) {
        if (backButton == null) return;
        bindEscape(scene, backButton::fire);
    }

    private boolean isInteractiveTarget(Node target) {
        Node node = target;
        while (node != null) {
            if (node instanceof Control || node instanceof ScrollPane) return true;
            if (node.getOnMousePressed() != null
                    || node.getOnMouseClicked() != null
                    || node.getOnMouseDragged() != null
                    || node.getOnMouseReleased() != null) {
                return true;
            }
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

    private boolean sceneContainsScrollPane(Node node) {
        if (node instanceof ScrollPane) return true;
        if (node instanceof Parent p) {
            for (Node child : p.getChildrenUnmodifiable()) {
                if (sceneContainsScrollPane(child)) return true;
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

    private void applyNoEllipsis(Label label) {
        label.setTextOverrun(OverrunStyle.CLIP);
        label.setEllipsisString("");
    }

    private void fitLabelSingleLine(Label label, double maxSize, double minSize, double maxWidth) {
        if (label == null) return;
        String text = label.getText();
        if (text == null || text.isBlank()) return;
        double size = maxSize;
        while (size > minSize) {
            Font f = Font.font("Arial Black", size);
            if (measureTextWidth(text, f) <= maxWidth) {
                label.setFont(f);
                return;
            }
            size -= 1.0;
        }
        label.setFont(Font.font("Arial Black", minSize));
    }

    private void bindScaleToFit(Scene scene, Node content, double padding) {
        Runnable apply = () -> {
            if (scene.getRoot() instanceof StackPane sp && "uiFrame".equals(sp.getId())) {
                return;
            }
            content.applyCss();
            content.autosize();
            Bounds bounds = content.getBoundsInLocal();
            double w = bounds.getWidth();
            double h = bounds.getHeight();
            if (w <= 0 || h <= 0) {
                content.setScaleX(1.0);
                content.setScaleY(1.0);
                return;
            }
            double availW = Math.max(1.0, scene.getWidth() - padding * 2);
            double availH = Math.max(1.0, scene.getHeight() - padding * 2);
            double scale = Math.min(1.0, Math.min(availW / w, availH / h));
            content.setScaleX(scale);
            content.setScaleY(scale);
        };
        scene.widthProperty().addListener((obs, oldVal, newVal) -> apply.run());
        scene.heightProperty().addListener((obs, oldVal, newVal) -> apply.run());
        content.boundsInLocalProperty().addListener((obs, oldVal, newVal) -> apply.run());
        javafx.application.Platform.runLater(apply);
    }

    private void ensureSceneAutoScaled(Scene scene) {
        if (scene == null) return;
        Node root = scene.getRoot();
        if (root == null) return;
        if (root instanceof StackPane sp && "responsiveContainer".equals(sp.getId())) return;
        if (root instanceof StackPane sp && "uiFrame".equals(sp.getId())) return;
        if (Boolean.TRUE.equals(root.getProperties().get("noAutoScale"))) return;
        javafx.application.Platform.runLater(() -> wrapAndScaleUiScene(scene));
    }

    private void applyFocusRingStyle(Scene scene) {
        if (scene == null) return;
        Node root = scene.getRoot();
        if (!(root instanceof Region region)) return;
        String focusStyle = "-fx-focus-color: transparent; -fx-faint-focus-color: transparent;";
        String style = region.getStyle();
        if (style == null) style = "";
        if (!style.contains("-fx-focus-color")) {
            region.setStyle(style + (style.isBlank() ? "" : "; ") + focusStyle);
        }
    }

    private void wrapAndScaleUiScene(Scene scene) {
        if (scene == null) return;
        Node root = scene.getRoot();
        if (root == null) return;
        if (root instanceof StackPane sp && "responsiveContainer".equals(sp.getId())) return;
        if (root instanceof StackPane sp && "uiFrame".equals(sp.getId())) return;

        boolean hasScroll = (root instanceof ScrollPane) || sceneContainsScrollPane(root);
        Region backgroundSource = null;
        if (root instanceof Region region) {
            backgroundSource = region;
        } else if (root instanceof ScrollPane sp) {
            Node content = sp.getContent();
            if (content instanceof Region contentRegion) {
                backgroundSource = contentRegion;
            }
        }

        String rootStyle = backgroundSource != null ? backgroundSource.getStyle() : "";
        String backgroundStyle = extractBackgroundStyle(rootStyle);
        String strippedStyle = stripBackgroundStyle(rootStyle);
        if (backgroundSource != null) {
            backgroundSource.setStyle(strippedStyle);
        }

        if (root instanceof ScrollPane sp) {
            String style = sp.getStyle();
            if (style == null) style = "";
            if (!style.contains("-fx-background-color")) {
                sp.setStyle(style + (style.isBlank() ? "" : "; ")
                        + "-fx-background-color: transparent; -fx-border-color: transparent; "
                        + "-fx-background-insets: 0; -fx-padding: 0;");
            }
            sp.setFitToWidth(true);
        }

        if (!hasScroll && root instanceof Region region) {
            region.setMinSize(WIDTH, HEIGHT);
            region.setPrefSize(WIDTH, HEIGHT);
            region.setMaxSize(WIDTH, HEIGHT);
        }

        StackPane frame = new StackPane(root);
        frame.setId("uiFrame");
        frame.setAlignment(Pos.CENTER);
        if (!backgroundStyle.isBlank()) {
            frame.setStyle(backgroundStyle);
        }
        frame.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        scene.setRoot(frame);
        Runnable sizeFrame = () -> {
            double w = scene.getWidth();
            double h = scene.getHeight();
            if (w > 0 && h > 0) {
                frame.setMinSize(w, h);
                frame.setPrefSize(w, h);
                frame.setMaxSize(w, h);
            }
        };
        scene.widthProperty().addListener((obs, oldV, newV) -> {
            double w = newV.doubleValue();
            frame.setMinWidth(w);
            frame.setPrefWidth(w);
            frame.setMaxWidth(w);
        });
        scene.heightProperty().addListener((obs, oldV, newV) -> {
            double h = newV.doubleValue();
            frame.setMinHeight(h);
            frame.setPrefHeight(h);
            frame.setMaxHeight(h);
        });
        javafx.application.Platform.runLater(sizeFrame);

        if (!hasScroll) {
            Runnable applyScale = () -> {
                double sx = scene.getWidth() / WIDTH;
                double sy = scene.getHeight() / HEIGHT;
                root.setScaleX(Math.max(0.01, sx));
                root.setScaleY(Math.max(0.01, sy));
            };
            scene.widthProperty().addListener((obs, oldV, newV) -> applyScale.run());
            scene.heightProperty().addListener((obs, oldV, newV) -> applyScale.run());
            javafx.application.Platform.runLater(applyScale);
        }
    }

    private String extractBackgroundStyle(String style) {
        if (style == null || style.isBlank()) return "";
        StringBuilder sb = new StringBuilder();
        String[] parts = style.split(";");
        for (String part : parts) {
            String trimmed = part.trim();
            if (trimmed.isBlank()) continue;
            if (trimmed.startsWith("-fx-background")) {
                if (sb.length() > 0) sb.append("; ");
                sb.append(trimmed);
            }
        }
        return sb.toString();
    }

    private String stripBackgroundStyle(String style) {
        if (style == null || style.isBlank()) return "";
        StringBuilder sb = new StringBuilder();
        String[] parts = style.split(";");
        for (String part : parts) {
            String trimmed = part.trim();
            if (trimmed.isBlank()) continue;
            if (trimmed.startsWith("-fx-background")) continue;
            if (sb.length() > 0) sb.append("; ");
            sb.append(trimmed);
        }
        return sb.toString();
    }

    private void ensureSceneFitsToScreen(Stage stage, Scene scene) {
        if (scene == null) return;
        Node root = scene.getRoot();
        if (root == null) return;
        if (root instanceof StackPane sp && "responsiveContainer".equals(sp.getId())) return;
        if (root instanceof StackPane sp && "autoScrollContainer".equals(sp.getId())) return;
        if (root instanceof ScrollPane) return;
        if (sceneContainsScrollPane(root)) return;

        double availW = scene.getWidth();
        double availH = scene.getHeight();
        if (availW <= 1 || availH <= 1) {
            Rectangle2D bounds = getStageScreenBounds(stage, scene);
            if (bounds == null) return;
            availW = bounds.getWidth();
            availH = bounds.getHeight();
        }
        availW = Math.max(1.0, availW);
        availH = Math.max(1.0, availH);

        root.applyCss();
        root.autosize();
        Bounds rootBounds = root.getBoundsInLocal();
        if (rootBounds.getWidth() <= availW + 1 && rootBounds.getHeight() <= availH + 1) {
            return;
        }

        if (!(root instanceof Parent parent)) {
            return;
        }

        ScrollPane scroll = wrapInOverflowScroll(parent);

        StackPane container = new StackPane(scroll);
        container.setId("autoScrollContainer");
        container.setAlignment(Pos.CENTER);
        if (root instanceof Region region) {
            String style = region.getStyle();
            if (style != null && !style.isBlank()) {
                container.setStyle(style);
            }
        }
        scene.setRoot(container);
    }

    private ScrollPane wrapInOverflowScroll(Parent content) {
        ScrollPane scroll = new ScrollPane(content);
        scroll.setFitToWidth(true);
        scroll.setFitToHeight(false);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scroll.setPannable(true);
        scroll.setStyle("-fx-background: transparent; -fx-background-color: transparent; -fx-control-inner-background: transparent; -fx-border-color: transparent; -fx-border-width: 0; -fx-background-insets: 0; -fx-padding: 0;");
        return scroll;
    }

    private ScrollPane wrapInScroll(Parent content) {
        ScrollPane scroll = new ScrollPane(content);
        scroll.setFitToWidth(true);
        scroll.setFitToHeight(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scroll.setStyle("-fx-background: transparent; -fx-background-color: transparent; "
                + "-fx-control-inner-background: transparent; -fx-border-color: transparent; "
                + "-fx-background-insets: 0; -fx-padding: 0;");
        return scroll;
    }

    private void styleSettingsInfoLabel(Label label, String color) {
        label.setFont(Font.font("Consolas", 24));
        label.setTextFill(Color.web(color));
        label.setWrapText(true);
        label.setMaxWidth(1100);
        label.setPrefWidth(1100);
        label.setTextAlignment(TextAlignment.LEFT);
        label.setAlignment(Pos.CENTER_LEFT);
        applyNoEllipsis(label);
    }

    private VBox buildSettingsRow(Button toggle, String description, String color) {
        Label label = new Label(description);
        label.setFont(Font.font("Consolas", 20));
        label.setTextFill(Color.web(color));
        label.setWrapText(true);
        label.setMaxWidth(620);
        label.setPrefWidth(620);
        label.setTextAlignment(TextAlignment.LEFT);
        label.setAlignment(Pos.CENTER_LEFT);
        label.setPadding(new Insets(0, 0, 0, 14));
        applyNoEllipsis(label);

        VBox row = new VBox(6, toggle, label);
        row.setAlignment(Pos.CENTER_LEFT);
        return row;
    }

    private boolean shouldRenderFrame(long now) {
        if (fpsCap <= 0) return true;
        long interval = 1_000_000_000L / fpsCap;
        if (lastRenderNs == 0L || now - lastRenderNs >= interval) {
            lastRenderNs = now;
            return true;
        }
        return false;
    }

    private void resetRenderTimer() {
        lastRenderNs = 0L;
    }

    private int sanitizeFpsCap(int value) {
        for (int cap : FPS_CAPS) {
            if (cap == value) return value;
        }
        return 60;
    }

    private void refreshFpsCapButton(Button btn) {
        String label = fpsCap <= 0 ? "FPS CAP: UNCAPPED" : "FPS CAP: " + fpsCap;
        String color = fpsCap <= 0 ? "#546E7A" : "#1E88E5";
        btn.setText(label);
        btn.setStyle("-fx-background-color: " + color + "; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 24;");
    }

    private void cycleFpsCap(Button btn) {
        int idx = -1;
        for (int i = 0; i < FPS_CAPS.length; i++) {
            if (FPS_CAPS[i] == fpsCap) {
                idx = i;
                break;
            }
        }
        int next = (idx + 1) % FPS_CAPS.length;
        fpsCap = FPS_CAPS[next];
        refreshFpsCapButton(btn);
        resetRenderTimer();
        saveAchievements();
    }

    private void setSettingsTabActive(Button tab, boolean active) {
        String color = active ? "#FBC02D" : "#455A64";
        String textColor = active ? "#0A0A0A" : "white";
        String weight = active ? "bold" : "normal";
        tab.setStyle("-fx-background-color: " + color + "; -fx-text-fill: " + textColor + "; -fx-background-radius: 18; -fx-font-weight: " + weight + ";");
        if (tab.isFocused()) {
            highlightControlSimple(tab);
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
        fitStageToScreen(stage, scene);
        ensureSceneAutoScaled(scene);
        if (fullscreenEnabled) {
            if (!stage.isFullScreen()) stage.setFullScreen(true);
        } else if (wasFullscreen && stage.isFullScreen()) {
            stage.setFullScreen(false);
        }
    }

    private void fitStageToScreen(Stage stage, Scene scene) {
        if (stage == null || scene == null) return;
        if (fullscreenEnabled || stage.isFullScreen()) return;
        Rectangle2D bounds = getStageScreenBounds(stage, scene);
        if (bounds == null) return;
        double currentW = stage.getWidth();
        double currentH = stage.getHeight();
        boolean hasSize = currentW > 1 && currentH > 1;
        double maxW = bounds.getWidth();
        double maxH = bounds.getHeight();
        double targetW;
        double targetH;
        if (hasSize) {
            targetW = Math.min(currentW, maxW);
            targetH = Math.min(currentH, maxH);
        } else {
            double sceneW = scene.getWidth() > 0 ? scene.getWidth() : WIDTH;
            double sceneH = scene.getHeight() > 0 ? scene.getHeight() : HEIGHT;
            targetW = Math.min(sceneW, maxW);
            targetH = Math.min(sceneH, maxH);
        }
        if (targetW > 0 && targetH > 0) {
            boolean resized = false;
            if (Math.abs(stage.getWidth() - targetW) > 1.0) {
                stage.setWidth(targetW);
                resized = true;
            }
            if (Math.abs(stage.getHeight() - targetH) > 1.0) {
                stage.setHeight(targetH);
                resized = true;
            }
            if (resized) {
                stage.centerOnScreen();
            }
        }
    }

    private Rectangle2D getStageScreenBounds(Stage stage, Scene scene) {
        try {
            double x = stage.getX();
            double y = stage.getY();
            double w = stage.getWidth() > 0 ? stage.getWidth() : (scene.getWidth() > 0 ? scene.getWidth() : WIDTH);
            double h = stage.getHeight() > 0 ? stage.getHeight() : (scene.getHeight() > 0 ? scene.getHeight() : HEIGHT);
            List<Screen> screens = Screen.getScreensForRectangle(x, y, w, h);
            Screen screen = screens.isEmpty() ? Screen.getPrimary() : screens.get(0);
            return screen.getVisualBounds();
        } catch (Exception e) {
            return Screen.getPrimary().getVisualBounds();
        }
    }

    private void playButtonClick() {
        if (!sfxEnabled) return;
        if (buttonClickClip != null) {
            buttonClickClip.stop();
            buttonClickClip.play();
        }
    }

    private void playErrorSound() {
        if (!sfxEnabled) return;
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
        String marker = "-fx-effect: dropshadow(three-pass-box, rgba(255,255,255,0.9), 18, 0.6, 0, 0);";
        ctrl.getProperties().put("highlightMarker", marker);
        String style = original == null ? "" : original;
        if (!style.contains("-fx-effect: dropshadow")) {
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

    // === SOUND & MUSIC ===
    public AudioClip bonkClip, butterClip, jalapenoClip, swingClip, hugewaveClip, buttonClickClip, zombieFallingClip;
    public MediaPlayer musicPlayer, menuMusicPlayer, victoryMusicPlayer;

    public static final String[] ACHIEVEMENT_NAMES = {
            "First Blood", "Dominator", "Annihilator", "Turkey Slam Master", "Lean God", "Lounge Lizard",
            "Power-Up Hoarder", "Fall Guy", "Taunt Lord", "Clutch God",
            "Rooftop Runner", "Neon Addict", "Urban King",
            "Thermal Rider", "Cliff Diver", "Sky Emperor",
            "Vine Swinger", "Canopy King", "Pelican King",
            "Classic Crest", "Echoes Below", "Story Keeper"
    };

    public static final String[] ACHIEVEMENT_DESCRIPTIONS = {
            "Get your first elimination in a match",
            "Eliminate 3 opponents in one match (max possible in 4-player)",
            "Eliminate all other opponents in one match",
            "Perform 3 ground pounds in one match (Turkey only)",
            "Spend a total of 30 seconds inside your lean cloud (Opium Bird only)",
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
            "Pelican Plunge 15 enemies across matches",
            "Complete Classic mode with any bird",
            "Complete Adventure Chapter 2: Echoes Below",
            "Complete all Adventure chapters"
    };

    public static final int ACHIEVEMENT_COUNT = ACHIEVEMENT_NAMES.length;
    public boolean[] achievementsUnlocked = new boolean[ACHIEVEMENT_COUNT];
    public int[] achievementProgress = new int[ACHIEVEMENT_COUNT]; // for tracking partial progress

    // === SKIN UNLOCKS ===
    public boolean cityPigeonUnlocked = true;
    public boolean noirPigeonUnlocked = false;
    public boolean beaconPigeonUnlocked = false;
    public boolean trainingModeActive = false;
    private BirdType trainingPlayerBird = BirdType.PIGEON;
    private BirdType trainingOpponentBird = BirdType.PIGEON;
    private final int trainingDummyIndex = 1;
    private double trainingPlayerSpawnX = 1200;
    private double trainingPlayerSpawnY = GROUND_Y - 400;
    private double trainingDummySpawnX = 4200;
    private double trainingDummySpawnY = GROUND_Y - 400;
    public boolean eagleSkinUnlocked = true; // Sky Tyrant Eagle skin
    public boolean novaPhoenixUnlocked = false;
    public boolean duneFalconUnlocked = false;
    public boolean mintPenguinUnlocked = false;
    public boolean circuitTitmouseUnlocked = false;
    public boolean prismRazorbillUnlocked = false;
    public boolean auroraPelicanUnlocked = false;
    public boolean sunflareHummingbirdUnlocked = false;
    public boolean glacierShoebillUnlocked = false;
    public boolean tideVultureUnlocked = false;
    public boolean eclipseMockingbirdUnlocked = false;
    public boolean umbraBatUnlocked = false;
    public boolean batUnlocked = false;
    public boolean falconUnlocked = false;
    public boolean heisenbirdUnlocked = false;
    public boolean phoenixUnlocked = false;
    public boolean titmouseUnlocked = false;
    public boolean ravenUnlocked = false;

    public static final int SKIN_COUNT = 3; // City Pigeon + Noir Pigeon + Beacon Pigeon
    private static final String BEACON_PIGEON_SKIN = "BEACON_PIGEON";
    private static final String NOVA_PHOENIX_SKIN = "NOVA_PHOENIX";
    private static final String DUNE_FALCON_SKIN = "DUNE_FALCON";
    private static final String MINT_PENGUIN_SKIN = "MINT_PENGUIN";
    private static final String CIRCUIT_TITMOUSE_SKIN = "CIRCUIT_TITMOUSE";
    private static final String PRISM_RAZORBILL_SKIN = "PRISM_RAZORBILL";
    private static final String AURORA_PELICAN_SKIN = "AURORA_PELICAN";
    private static final String SUNFLARE_HUMMINGBIRD_SKIN = "SUNFLARE_HUMMINGBIRD";
    private static final String GLACIER_SHOEBILL_SKIN = "GLACIER_SHOEBILL";
    private static final String TIDE_VULTURE_SKIN = "TIDE_VULTURE";
    private static final String ECLIPSE_MOCKINGBIRD_SKIN = "ECLIPSE_MOCKINGBIRD";
    private static final String UMBRA_BAT_SKIN = "UMBRA_BAT";
    private static final String CLASSIC_CONTINUE_KEY = "CLASSIC_CONTINUE";
    private static final String CHAR_BAT_KEY = "CHAR_BAT";
    private static final String CHAR_FALCON_KEY = "CHAR_FALCON";
    private static final String CHAR_HEISENBIRD_KEY = "CHAR_HEISENBIRD";
    private static final String CHAR_PHOENIX_KEY = "CHAR_PHOENIX";
    private static final String CHAR_TITMOUSE_KEY = "CHAR_TITMOUSE";
    private static final String CHAR_RAVEN_KEY = "CHAR_RAVEN";
    private static final String MAP_CAVE_KEY = "MAP_CAVE";
    private static final String MAP_BATTLEFIELD_KEY = "MAP_BATTLEFIELD";

    // === CLASSIC MODE ===
    private boolean classicModeActive = false;
    private BirdType classicSelectedBird = BirdType.PIGEON;
    private String classicSelectedSkinKey = null;
    private int classicContinues = 0;
    private int classicDeaths = 0;
    private final boolean[] classicCompleted = new boolean[BirdType.values().length];
    private final boolean[] classicSkinUnlocked = new boolean[BirdType.values().length];
    private final List<ClassicEncounter> classicRun = new ArrayList<>();
    private int classicRoundIndex = 0;
    private ClassicEncounter classicEncounter = null;
    private String classicRunCodename = "";
    private boolean classicTeamMode = false;
    private final int[] classicTeams = new int[]{1, 2, 2, 2};
    private final int[] classicCpuLevels = new int[4];
    private MapType classicLastMap = null;
    private ClassicTwist classicLastTwist = null;
    private MatchMutator classicLastMutator = null;
    private final Set<ClassicTwist> classicUsedTwists = EnumSet.noneOf(ClassicTwist.class);
    private final Set<MatchMutator> classicUsedMutators = EnumSet.noneOf(MatchMutator.class);

    // === TOURNAMENT MODE ===
    private boolean tournamentModeActive = false;
    private int tournamentEntrantCount = 8;
    private int tournamentHumanCount = 2;
    private boolean tournamentMapRandom = true;
    private MapType tournamentFixedMap = MapType.FOREST;
    private final List<TournamentEntry> tournamentEntries = new ArrayList<>();
    private final List<List<TournamentMatch>> tournamentRounds = new ArrayList<>();
    private TournamentMatch currentTournamentMatch = null;
    private TournamentEntry tournamentSlotA = null;
    private TournamentEntry tournamentSlotB = null;
    private boolean tournamentMatchResolved = false;

    private enum ClassicTwist {
        STORM_LIFTS("Storm Lifts", "Thermal vents surge and reward vertical control."),
        CROW_CARNIVAL("Crow Carnival", "Neutral crows patrol lanes from the opening bell."),
        TITAN_CACHE("Titan Cache", "The arena spawns high-impact powerups immediately."),
        NECTAR_BLOOM("Nectar Bloom", "Nectar nodes pulse in map hotspots for burst tempo."),
        VINE_SURGE("Vine Surge", "Vine grapples and nectar lanes open swing routes."),
        MEDIC_CACHE("Medic Cache", "Health and speed drops keep duels steady."),
        SHOCK_DROPS("Shock Drops", "Overcharge and titan effects drop more often."),
        OVERCHARGE_FURY("Overcharge Fury", "Overcharge and rage drops spike momentum."),
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
        final String skinKey;
        final int cpuLevel;

        ClassicFighter(BirdType type, String title, double health, double powerMult, double speedMult) {
            this(type, title, health, powerMult, speedMult, null, 0);
        }

        ClassicFighter(BirdType type, String title, double health, double powerMult, double speedMult, String skinKey) {
            this(type, title, health, powerMult, speedMult, skinKey, 0);
        }

        ClassicFighter(BirdType type, String title, double health, double powerMult, double speedMult, String skinKey, int cpuLevel) {
            this.type = type;
            this.title = title;
            this.health = health;
            this.powerMult = powerMult;
            this.speedMult = speedMult;
            this.skinKey = skinKey;
            this.cpuLevel = cpuLevel;
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

    static class TournamentEntry {
        final int id;
        boolean human = true;
        BirdType selectedType = null;

        TournamentEntry(int id) {
            this.id = id;
        }
    }

    static class TournamentMatch {
        final int roundIndex;
        final int matchIndex;
        TournamentEntry a;
        TournamentEntry b;
        TournamentEntry winner;

        TournamentMatch(int roundIndex, int matchIndex, TournamentEntry a, TournamentEntry b) {
            this.roundIndex = roundIndex;
            this.matchIndex = matchIndex;
            this.a = a;
            this.b = b;
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
        RAGE_FRENZY("Rage Frenzy", "Everyone fights enraged for the whole match."),
        TITAN_RUMBLE("Titan Rumble", "Everyone enters Titan form for the whole match."),
        OVERCHARGE_BRAWL("Overcharge Brawl", "Rapid attacks and extra power for everyone."),
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
    private int[] adventureChapterProgressByIndex;
    private boolean[] adventureChapterCompletedByIndex;
    private final boolean[] adventureUnlocked = new boolean[BirdType.values().length];
    private BirdType adventureSelectedBird = BirdType.PIGEON;
    private String adventureSelectedSkinKey = null;
    private String activeAdventureDialogueTitle = null;
    private int activeAdventureDialogueChapterIndex = -1;
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
                                    "The Beacon went dark last night. The rooftops feel hollow."
                            ),
                            new AdventureDialogueLine(
                                    BirdType.PIGEON,
                                    BirdType.MOCKINGBIRD,
                                    DialogueSide.RIGHT,
                                    "Old Sparrow",
                                    "When the Beacon fades, the Carrion Court stirs. Vultures are already circling."
                            ),
                            new AdventureDialogueLine(
                                    BirdType.PIGEON,
                                    BirdType.MOCKINGBIRD,
                                    DialogueSide.LEFT,
                                    "Pigeon",
                                    "Then we gather wings. The skyline will not go quiet."
                            ),
                            new AdventureDialogueLine(
                                    BirdType.PIGEON,
                                    BirdType.MOCKINGBIRD,
                                    DialogueSide.RIGHT,
                                    "Old Sparrow",
                                    "Start with the neon courier. Earn her trust."
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
                                                    "I bring warnings, not comfort. The wind says you are slow."
                                            ),
                                            new AdventureDialogueLine(
                                                    BirdType.PIGEON,
                                                    BirdType.HUMMINGBIRD,
                                                    DialogueSide.LEFT,
                                                    "Pigeon",
                                                    "Then race me across the roofs. Win, and I listen."
                                            )
                                    },
                                    new AdventureDialogueLine[] {
                                            new AdventureDialogueLine(
                                                    BirdType.PIGEON,
                                                    BirdType.HUMMINGBIRD,
                                                    DialogueSide.RIGHT,
                                                    "Neon Hummingbird",
                                                    "All right, you kept pace."
                                            ),
                                            new AdventureDialogueLine(
                                                    BirdType.PIGEON,
                                                    BirdType.HUMMINGBIRD,
                                                    DialogueSide.LEFT,
                                                    "Pigeon",
                                                    "Then fly with me. The Beacon needs voices."
                                            ),
                                            new AdventureDialogueLine(
                                                    BirdType.PIGEON,
                                                    BirdType.HUMMINGBIRD,
                                                    DialogueSide.RIGHT,
                                                    "Neon Hummingbird",
                                                    "I'm in. Eagle waits at the cliffs."
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
                                    "Battle 2: Old Friend Run",
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
                                                    "Eagle, it's me. The Beacon is out."
                                            ),
                                            new AdventureDialogueLine(
                                                    BirdType.PIGEON,
                                                    BirdType.EAGLE,
                                                    DialogueSide.RIGHT,
                                                    "Eagle",
                                                    "Storms return. If you want my wings, prove your heart."
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
            ),
            new AdventureChapter(
                    "Chapter 2: Echoes Below",
                    "With the Beacon dark, Pigeon descends into the Caves to find the bat who guards the Echo Vault. The Carrion Court sends a razor-wing and a lieutenant to claim the shard.",
                    new AdventureDialogueLine[] {
                            new AdventureDialogueLine(
                                    BirdType.PIGEON,
                                    BirdType.EAGLE,
                                    DialogueSide.LEFT,
                                    "Pigeon",
                                    "The caves are colder than the rooftops."
                            ),
                            new AdventureDialogueLine(
                                    BirdType.PIGEON,
                                    BirdType.EAGLE,
                                    DialogueSide.RIGHT,
                                    "Eagle",
                                    "Bat hears the stone. Speak with an honest wing."
                            ),
                            new AdventureDialogueLine(
                                    BirdType.HUMMINGBIRD,
                                    BirdType.PIGEON,
                                    DialogueSide.LEFT,
                                    "Neon Hummingbird",
                                    "I can map the echoes. Something sharp moves below."
                            ),
                            new AdventureDialogueLine(
                                    BirdType.PIGEON,
                                    BirdType.BAT,
                                    DialogueSide.RIGHT,
                                    "Bat",
                                    "Who breaks the silence?"
                            )
                    },
                    new AdventureBattle[] {
                            new AdventureBattle(
                                    "Battle 1: Cave Listener",
                                    "A shadow guardian tests your intent. Earn Bat's trust to reach the Echo Vault.",
                                    MapType.CAVE,
                                    BirdType.BAT,
                                    "Rival: Night Bat",
                                    130,
                                    1.0,
                                    1.1,
                                    null,
                                    EnumSet.of(BirdType.PIGEON, BirdType.HUMMINGBIRD, BirdType.EAGLE),
                                    BirdType.BAT,
                                    new AdventureDialogueLine[] {
                                            new AdventureDialogueLine(
                                                    BirdType.PIGEON,
                                                    BirdType.BAT,
                                                    DialogueSide.RIGHT,
                                                    "Bat",
                                                    "The cave keeps secrets. Why should I open it?"
                                            ),
                                            new AdventureDialogueLine(
                                                    BirdType.PIGEON,
                                                    BirdType.BAT,
                                                    DialogueSide.LEFT,
                                                    "Pigeon",
                                                    "Because the Beacon is our sky. We need your ears."
                                            )
                                    },
                                    new AdventureDialogueLine[] {
                                            new AdventureDialogueLine(
                                                    BirdType.PIGEON,
                                                    BirdType.BAT,
                                                    DialogueSide.RIGHT,
                                                    "Bat",
                                                    "Your heartbeat is steady. I will guide you."
                                            ),
                                            new AdventureDialogueLine(
                                                    BirdType.PIGEON,
                                                    BirdType.BAT,
                                                    DialogueSide.LEFT,
                                                    "Pigeon",
                                                    "Then help us find the Echo Vault."
                                            ),
                                            new AdventureDialogueLine(
                                                    BirdType.HUMMINGBIRD,
                                                    BirdType.PIGEON,
                                                    DialogueSide.LEFT,
                                                    "Neon Hummingbird",
                                                    "Hear that? Blades in the dark."
                                            )
                                    },
                                    new AdventureDialogueLine[] {
                                            new AdventureDialogueLine(
                                                    BirdType.PIGEON,
                                                    BirdType.BAT,
                                                    DialogueSide.RIGHT,
                                                    "Bat",
                                                    "Your heart stutters. Return when it is steady."
                                            ),
                                            new AdventureDialogueLine(
                                                    BirdType.PIGEON,
                                                    BirdType.BAT,
                                                    DialogueSide.LEFT,
                                                    "Pigeon",
                                                    "I will."
                                            )
                                    }
                            ),
                            new AdventureBattle(
                                    "Battle 2: Razor Ambush",
                                    "A razor-wing sent by the Court strikes in the echo tunnels.",
                                    MapType.CAVE,
                                    BirdType.RAZORBILL,
                                    "Enemy: Razorwing",
                                    150,
                                    1.12,
                                    1.08,
                                    null,
                                    EnumSet.of(BirdType.PIGEON, BirdType.HUMMINGBIRD, BirdType.EAGLE, BirdType.BAT),
                                    null,
                                    new AdventureDialogueLine[] {
                                            new AdventureDialogueLine(
                                                    BirdType.BAT,
                                                    BirdType.RAZORBILL,
                                                    DialogueSide.RIGHT,
                                                    "Razorwing",
                                                    "Step away from the vault. The shard belongs to the Court."
                                            ),
                                            new AdventureDialogueLine(
                                                    BirdType.BAT,
                                                    BirdType.RAZORBILL,
                                                    DialogueSide.LEFT,
                                                    "Bat",
                                                    "We do not trade echoes for threats."
                                            )
                                    },
                                    new AdventureDialogueLine[] {
                                            new AdventureDialogueLine(
                                                    BirdType.BAT,
                                                    BirdType.PIGEON,
                                                    DialogueSide.LEFT,
                                                    "Bat",
                                                    "The Court sends blades now."
                                            ),
                                            new AdventureDialogueLine(
                                                    BirdType.BAT,
                                                    BirdType.PIGEON,
                                                    DialogueSide.RIGHT,
                                                    "Pigeon",
                                                    "Then we answer at the Beacon."
                                            )
                                    },
                                    new AdventureDialogueLine[] {
                                            new AdventureDialogueLine(
                                                    BirdType.BAT,
                                                    BirdType.RAZORBILL,
                                                    DialogueSide.RIGHT,
                                                    "Razorwing",
                                                    "The echo belongs to the Court."
                                            ),
                                            new AdventureDialogueLine(
                                                    BirdType.BAT,
                                                    BirdType.PIGEON,
                                                    DialogueSide.RIGHT,
                                                    "Pigeon",
                                                    "Not yet."
                                            )
                                    }
                            ),
                            new AdventureBattle(
                                    "Battle 3: Beacon Husk",
                                    "The Beacon's base crawls with carrion. A lieutenant guards the Echo Shard.",
                                    MapType.CITY,
                                    BirdType.VULTURE,
                                    "Enemy: Carrion Lieutenant",
                                    180,
                                    1.18,
                                    0.95,
                                    null,
                                    EnumSet.of(BirdType.PIGEON, BirdType.HUMMINGBIRD, BirdType.EAGLE, BirdType.BAT),
                                    null,
                                    new AdventureDialogueLine[] {
                                            new AdventureDialogueLine(
                                                    BirdType.PIGEON,
                                                    BirdType.VULTURE,
                                                    DialogueSide.RIGHT,
                                                    "Carrion Lieutenant",
                                                    "Leave the shard and I let you fly."
                                            ),
                                            new AdventureDialogueLine(
                                                    BirdType.PIGEON,
                                                    BirdType.VULTURE,
                                                    DialogueSide.LEFT,
                                                    "Pigeon",
                                                    "We do not bargain with carrion."
                                            ),
                                            new AdventureDialogueLine(
                                                    BirdType.PIGEON,
                                                    BirdType.EAGLE,
                                                    DialogueSide.RIGHT,
                                                    "Eagle",
                                                    "Then clip his wings."
                                            )
                                    },
                                    new AdventureDialogueLine[] {
                                            new AdventureDialogueLine(
                                                    BirdType.PIGEON,
                                                    BirdType.VULTURE,
                                                    DialogueSide.RIGHT,
                                                    "Carrion Lieutenant",
                                                    "The Court will rise."
                                            ),
                                            new AdventureDialogueLine(
                                                    BirdType.HUMMINGBIRD,
                                                    BirdType.PIGEON,
                                                    DialogueSide.LEFT,
                                                    "Neon Hummingbird",
                                                    "Then we light the sky."
                                            ),
                                            new AdventureDialogueLine(
                                                    BirdType.PIGEON,
                                                    BirdType.EAGLE,
                                                    DialogueSide.LEFT,
                                                    "Pigeon",
                                                    "The Beacon will sing again."
                                            )
                                    },
                                    new AdventureDialogueLine[] {
                                            new AdventureDialogueLine(
                                                    BirdType.PIGEON,
                                                    BirdType.VULTURE,
                                                    DialogueSide.RIGHT,
                                                    "Carrion Lieutenant",
                                                    "The Beacon stays dark."
                                            ),
                                            new AdventureDialogueLine(
                                                    BirdType.PIGEON,
                                                    BirdType.VULTURE,
                                                    DialogueSide.LEFT,
                                                    "Pigeon",
                                                    "We will return."
                                            )
                                    }
                            )
                    },
                    new AdventureDialogueLine[] {
                            new AdventureDialogueLine(
                                    BirdType.BAT,
                                    BirdType.PIGEON,
                                    DialogueSide.LEFT,
                                    "Bat",
                                    "The shard remembers the Beacon. It can sing again."
                            ),
                            new AdventureDialogueLine(
                                    BirdType.BAT,
                                    BirdType.PIGEON,
                                    DialogueSide.RIGHT,
                                    "Pigeon",
                                    "Then we carry it home. The Court will answer."
                            ),
                            new AdventureDialogueLine(
                                    BirdType.PIGEON,
                                    BirdType.EAGLE,
                                    DialogueSide.RIGHT,
                                    "Eagle",
                                    "The Court's talons are many. We will need more wings."
                            )
                    }
            ),
            new AdventureChapter(
                    "Chapter 3: Storm of Wings",
                    "Echo Shard in claw, the flock races to the Beacon summit. The Carrion Court unleashes speed and greed to seize the shard before Vulture descends.",
                    new AdventureDialogueLine[] {
                            new AdventureDialogueLine(
                                    BirdType.PIGEON,
                                    BirdType.BAT,
                                    DialogueSide.LEFT,
                                    "Pigeon",
                                    "The shard is warm. It remembers the Beacon."
                            ),
                            new AdventureDialogueLine(
                                    BirdType.PIGEON,
                                    BirdType.BAT,
                                    DialogueSide.RIGHT,
                                    "Bat",
                                    "Echo says the Court waits above. They want the light silenced forever."
                            ),
                            new AdventureDialogueLine(
                                    BirdType.PIGEON,
                                    BirdType.EAGLE,
                                    DialogueSide.RIGHT,
                                    "Eagle",
                                    "Then we take the storm route. No more hiding."
                            ),
                            new AdventureDialogueLine(
                                    BirdType.PIGEON,
                                    BirdType.HUMMINGBIRD,
                                    DialogueSide.RIGHT,
                                    "Neon Hummingbird",
                                    "Fast wings incoming. Someone rides the wind like a blade."
                            )
                    },
                    new AdventureBattle[] {
                            new AdventureBattle(
                                    "Battle 1: Storm Intercept",
                                    "A Carrion courier dives from the clouds to steal the shard.",
                                    MapType.SKYCLIFFS,
                                    BirdType.FALCON,
                                    "Enemy: Storm Falcon",
                                    150,
                                    1.1,
                                    1.2,
                                    null,
                                    EnumSet.of(BirdType.PIGEON, BirdType.HUMMINGBIRD, BirdType.EAGLE, BirdType.BAT),
                                    BirdType.FALCON,
                                    new AdventureDialogueLine[] {
                                            new AdventureDialogueLine(
                                                    BirdType.PIGEON,
                                                    BirdType.FALCON,
                                                    DialogueSide.RIGHT,
                                                    "Storm Falcon",
                                                    "Hand me the shard. The Court pays in skies."
                                            ),
                                            new AdventureDialogueLine(
                                                    BirdType.PIGEON,
                                                    BirdType.FALCON,
                                                    DialogueSide.LEFT,
                                                    "Pigeon",
                                                    "The sky belongs to the flock."
                                            ),
                                            new AdventureDialogueLine(
                                                    BirdType.PIGEON,
                                                    BirdType.FALCON,
                                                    DialogueSide.RIGHT,
                                                    "Storm Falcon",
                                                    "Then outrun me."
                                            )
                                    },
                                    new AdventureDialogueLine[] {
                                            new AdventureDialogueLine(
                                                    BirdType.PIGEON,
                                                    BirdType.FALCON,
                                                    DialogueSide.RIGHT,
                                                    "Storm Falcon",
                                                    "Speed is not enough."
                                            ),
                                            new AdventureDialogueLine(
                                                    BirdType.PIGEON,
                                                    BirdType.FALCON,
                                                    DialogueSide.LEFT,
                                                    "Pigeon",
                                                    "Then stand aside."
                                            ),
                                            new AdventureDialogueLine(
                                                    BirdType.PIGEON,
                                                    BirdType.EAGLE,
                                                    DialogueSide.RIGHT,
                                                    "Eagle",
                                                    "The Beacon waits."
                                            )
                                    },
                                    new AdventureDialogueLine[] {
                                            new AdventureDialogueLine(
                                                    BirdType.PIGEON,
                                                    BirdType.FALCON,
                                                    DialogueSide.RIGHT,
                                                    "Storm Falcon",
                                                    "Keep running. The storm is ours."
                                            ),
                                            new AdventureDialogueLine(
                                                    BirdType.PIGEON,
                                                    BirdType.FALCON,
                                                    DialogueSide.LEFT,
                                                    "Pigeon",
                                                    "Not for long."
                                            )
                                    }
                            ),
                            new AdventureBattle(
                                    "Battle 2: Thief of Light",
                                    "A greedy enforcer ambushes you at the Beacon's lower spires.",
                                    MapType.CITY,
                                    BirdType.GRINCHHAWK,
                                    "Enemy: Shard Thief",
                                    170,
                                    1.15,
                                    1.0,
                                    null,
                                    EnumSet.of(BirdType.PIGEON, BirdType.HUMMINGBIRD, BirdType.EAGLE, BirdType.BAT, BirdType.FALCON),
                                    null,
                                    new AdventureDialogueLine[] {
                                            new AdventureDialogueLine(
                                                    BirdType.PIGEON,
                                                    BirdType.GRINCHHAWK,
                                                    DialogueSide.RIGHT,
                                                    "Grinch-Hawk",
                                                    "Shiny shard. I will take it."
                                            ),
                                            new AdventureDialogueLine(
                                                    BirdType.PIGEON,
                                                    BirdType.BAT,
                                                    DialogueSide.RIGHT,
                                                    "Bat",
                                                    "He hunts by greed, not honor."
                                            ),
                                            new AdventureDialogueLine(
                                                    BirdType.PIGEON,
                                                    BirdType.GRINCHHAWK,
                                                    DialogueSide.LEFT,
                                                    "Pigeon",
                                                    "Then we deny him both."
                                            )
                                    },
                                    new AdventureDialogueLine[] {
                                            new AdventureDialogueLine(
                                                    BirdType.PIGEON,
                                                    BirdType.GRINCHHAWK,
                                                    DialogueSide.RIGHT,
                                                    "Grinch-Hawk",
                                                    "You clipped my profit."
                                            ),
                                            new AdventureDialogueLine(
                                                    BirdType.PIGEON,
                                                    BirdType.HUMMINGBIRD,
                                                    DialogueSide.RIGHT,
                                                    "Neon Hummingbird",
                                                    "Good. Profit does not light the sky."
                                            ),
                                            new AdventureDialogueLine(
                                                    BirdType.PIGEON,
                                                    BirdType.GRINCHHAWK,
                                                    DialogueSide.LEFT,
                                                    "Pigeon",
                                                    "The Beacon is ours."
                                            )
                                    },
                                    new AdventureDialogueLine[] {
                                            new AdventureDialogueLine(
                                                    BirdType.PIGEON,
                                                    BirdType.GRINCHHAWK,
                                                    DialogueSide.RIGHT,
                                                    "Grinch-Hawk",
                                                    "Thanks for the shard."
                                            ),
                                            new AdventureDialogueLine(
                                                    BirdType.PIGEON,
                                                    BirdType.GRINCHHAWK,
                                                    DialogueSide.LEFT,
                                                    "Pigeon",
                                                    "Over my feathers."
                                            )
                                    }
                            ),
                            new AdventureBattle(
                                    "Battle 3: Carrion Crown",
                                    "Vulture descends as the Beacon stirs. End the Court's reign.",
                                    MapType.SKYCLIFFS,
                                    BirdType.VULTURE,
                                    "Boss: Vulture Regent",
                                    210,
                                    1.25,
                                    0.98,
                                    null,
                                    EnumSet.of(BirdType.PIGEON, BirdType.HUMMINGBIRD, BirdType.EAGLE, BirdType.BAT, BirdType.FALCON),
                                    null,
                                    new AdventureDialogueLine[] {
                                            new AdventureDialogueLine(
                                                    BirdType.PIGEON,
                                                    BirdType.VULTURE,
                                                    DialogueSide.RIGHT,
                                                    "Vulture Regent",
                                                    "You carry the shard. You think it belongs to you."
                                            ),
                                            new AdventureDialogueLine(
                                                    BirdType.PIGEON,
                                                    BirdType.VULTURE,
                                                    DialogueSide.LEFT,
                                                    "Pigeon",
                                                    "It belongs to every wing that still believes."
                                            ),
                                            new AdventureDialogueLine(
                                                    BirdType.PIGEON,
                                                    BirdType.EAGLE,
                                                    DialogueSide.RIGHT,
                                                    "Eagle",
                                                    "Then face us, carrion king."
                                            )
                                    },
                                    new AdventureDialogueLine[] {
                                            new AdventureDialogueLine(
                                                    BirdType.PIGEON,
                                                    BirdType.VULTURE,
                                                    DialogueSide.RIGHT,
                                                    "Vulture Regent",
                                                    "Then light it... if you can."
                                            ),
                                            new AdventureDialogueLine(
                                                    BirdType.PIGEON,
                                                    BirdType.BAT,
                                                    DialogueSide.RIGHT,
                                                    "Bat",
                                                    "Echo accepts. The Beacon remembers."
                                            ),
                                            new AdventureDialogueLine(
                                                    BirdType.PIGEON,
                                                    BirdType.VULTURE,
                                                    DialogueSide.LEFT,
                                                    "Pigeon",
                                                    "Skyline, rise."
                                            )
                                    },
                                    new AdventureDialogueLine[] {
                                            new AdventureDialogueLine(
                                                    BirdType.PIGEON,
                                                    BirdType.VULTURE,
                                                    DialogueSide.RIGHT,
                                                    "Vulture Regent",
                                                    "The Beacon stays dark."
                                            ),
                                            new AdventureDialogueLine(
                                                    BirdType.PIGEON,
                                                    BirdType.VULTURE,
                                                    DialogueSide.LEFT,
                                                    "Pigeon",
                                                    "We will return."
                                            )
                                    }
                            )
                    },
                    new AdventureDialogueLine[] {
                            new AdventureDialogueLine(
                                    BirdType.PIGEON,
                                    BirdType.HUMMINGBIRD,
                                    DialogueSide.LEFT,
                                    "Pigeon",
                                    "The shard sings. The Beacon flickers."
                            ),
                            new AdventureDialogueLine(
                                    BirdType.PIGEON,
                                    BirdType.HUMMINGBIRD,
                                    DialogueSide.RIGHT,
                                    "Neon Hummingbird",
                                    "The light is weak, but it is real."
                            ),
                            new AdventureDialogueLine(
                                    BirdType.PIGEON,
                                    BirdType.EAGLE,
                                    DialogueSide.RIGHT,
                                    "Eagle",
                                    "The Court will not forget this. We take the fight to their nest."
                            )
                    }
            ),
            new AdventureChapter(
                    "Chapter 4: Ashen Dawn",
                    "With the Beacon flickering, the flock storms the Carrion Nest. To reignite the sky, they must break the Court and awaken the ancient fire.",
                    new AdventureDialogueLine[] {
                            new AdventureDialogueLine(
                                    BirdType.PIGEON,
                                    BirdType.EAGLE,
                                    DialogueSide.LEFT,
                                    "Pigeon",
                                    "The Beacon is alive but weak. The Court will smother it."
                            ),
                            new AdventureDialogueLine(
                                    BirdType.PIGEON,
                                    BirdType.EAGLE,
                                    DialogueSide.RIGHT,
                                    "Eagle",
                                    "Then we strike their nest and end this."
                            ),
                            new AdventureDialogueLine(
                                    BirdType.PIGEON,
                                    BirdType.BAT,
                                    DialogueSide.RIGHT,
                                    "Bat",
                                    "Echo says the fire sleeps above. It will test whoever wakes it."
                            ),
                            new AdventureDialogueLine(
                                    BirdType.PIGEON,
                                    BirdType.HUMMINGBIRD,
                                    DialogueSide.RIGHT,
                                    "Neon Hummingbird",
                                    "Fast wings ready. No more running."
                            )
                    },
                    new AdventureBattle[] {
                            new AdventureBattle(
                                    "Battle 1: Nestfall Breach",
                                    "A gate warden blocks the path to the Carrion Nest.",
                                    MapType.VIBRANT_JUNGLE,
                                    BirdType.SHOEBILL,
                                    "Enemy: Gate Shoebill",
                                    170,
                                    1.12,
                                    0.95,
                                    null,
                                    EnumSet.of(BirdType.PIGEON, BirdType.HUMMINGBIRD, BirdType.EAGLE, BirdType.BAT, BirdType.FALCON),
                                    null,
                                    new AdventureDialogueLine[] {
                                            new AdventureDialogueLine(
                                                    BirdType.PIGEON,
                                                    BirdType.SHOEBILL,
                                                    DialogueSide.RIGHT,
                                                    "Gate Shoebill",
                                                    "No wing crosses the nest."
                                            ),
                                            new AdventureDialogueLine(
                                                    BirdType.PIGEON,
                                                    BirdType.SHOEBILL,
                                                    DialogueSide.LEFT,
                                                    "Pigeon",
                                                    "Then we break the gate."
                                            )
                                    },
                                    new AdventureDialogueLine[] {
                                            new AdventureDialogueLine(
                                                    BirdType.PIGEON,
                                                    BirdType.SHOEBILL,
                                                    DialogueSide.RIGHT,
                                                    "Gate Shoebill",
                                                    "The Court will answer."
                                            ),
                                            new AdventureDialogueLine(
                                                    BirdType.PIGEON,
                                                    BirdType.EAGLE,
                                                    DialogueSide.RIGHT,
                                                    "Eagle",
                                                    "Let them. We are inside."
                                            )
                                    },
                                    new AdventureDialogueLine[] {
                                            new AdventureDialogueLine(
                                                    BirdType.PIGEON,
                                                    BirdType.SHOEBILL,
                                                    DialogueSide.RIGHT,
                                                    "Gate Shoebill",
                                                    "Your beaks are not sharp enough."
                                            ),
                                            new AdventureDialogueLine(
                                                    BirdType.PIGEON,
                                                    BirdType.SHOEBILL,
                                                    DialogueSide.LEFT,
                                                    "Pigeon",
                                                    "We will return."
                                            )
                                    }
                            ),
                            new AdventureBattle(
                                    "Battle 2: Veil of Smoke",
                                    "A Carrion priest spreads a lean fog to hide the shard.",
                                    MapType.CITY,
                                    BirdType.OPIUMBIRD,
                                    "Enemy: Fog Priest",
                                    180,
                                    1.15,
                                    1.05,
                                    null,
                                    EnumSet.of(BirdType.PIGEON, BirdType.HUMMINGBIRD, BirdType.EAGLE, BirdType.BAT, BirdType.FALCON),
                                    null,
                                    new AdventureDialogueLine[] {
                                            new AdventureDialogueLine(
                                                    BirdType.PIGEON,
                                                    BirdType.OPIUMBIRD,
                                                    DialogueSide.RIGHT,
                                                    "Fog Priest",
                                                    "Breathe deep. Forget the light."
                                            ),
                                            new AdventureDialogueLine(
                                                    BirdType.PIGEON,
                                                    BirdType.OPIUMBIRD,
                                                    DialogueSide.LEFT,
                                                    "Pigeon",
                                                    "We remember the Beacon."
                                            ),
                                            new AdventureDialogueLine(
                                                    BirdType.PIGEON,
                                                    BirdType.BAT,
                                                    DialogueSide.RIGHT,
                                                    "Bat",
                                                    "Do not trust the haze."
                                            )
                                    },
                                    new AdventureDialogueLine[] {
                                            new AdventureDialogueLine(
                                                    BirdType.PIGEON,
                                                    BirdType.OPIUMBIRD,
                                                    DialogueSide.RIGHT,
                                                    "Fog Priest",
                                                    "The Court will drown you."
                                            ),
                                            new AdventureDialogueLine(
                                                    BirdType.PIGEON,
                                                    BirdType.HUMMINGBIRD,
                                                    DialogueSide.RIGHT,
                                                    "Neon Hummingbird",
                                                    "We fly above the fog."
                                            )
                                    },
                                    new AdventureDialogueLine[] {
                                            new AdventureDialogueLine(
                                                    BirdType.PIGEON,
                                                    BirdType.OPIUMBIRD,
                                                    DialogueSide.RIGHT,
                                                    "Fog Priest",
                                                    "Let the haze hold you."
                                            ),
                                            new AdventureDialogueLine(
                                                    BirdType.PIGEON,
                                                    BirdType.OPIUMBIRD,
                                                    DialogueSide.LEFT,
                                                    "Pigeon",
                                                    "Not today."
                                            )
                                    }
                            ),
                            new AdventureBattle(
                                    "Battle 3: Beacon Heart",
                                    "The ancient fire awakens. Prove the flock is worthy.",
                                    MapType.SKYCLIFFS,
                                    BirdType.PHOENIX,
                                    "Boss: Ashen Phoenix",
                                    220,
                                    1.2,
                                    1.1,
                                    null,
                                    EnumSet.of(BirdType.PIGEON, BirdType.HUMMINGBIRD, BirdType.EAGLE, BirdType.BAT, BirdType.FALCON),
                                    BirdType.PHOENIX,
                                    new AdventureDialogueLine[] {
                                            new AdventureDialogueLine(
                                                    BirdType.PIGEON,
                                                    BirdType.PHOENIX,
                                                    DialogueSide.RIGHT,
                                                    "Ashen Phoenix",
                                                    "The Beacon has slept. Why should it burn again?"
                                            ),
                                            new AdventureDialogueLine(
                                                    BirdType.PIGEON,
                                                    BirdType.PHOENIX,
                                                    DialogueSide.LEFT,
                                                    "Pigeon",
                                                    "Because the sky still needs hope."
                                            ),
                                            new AdventureDialogueLine(
                                                    BirdType.EAGLE,
                                                    BirdType.PHOENIX,
                                                    DialogueSide.LEFT,
                                                    "Eagle",
                                                    "Test us, flame. If we fail, let it die."
                                            )
                                    },
                                    new AdventureDialogueLine[] {
                                            new AdventureDialogueLine(
                                                    BirdType.PIGEON,
                                                    BirdType.PHOENIX,
                                                    DialogueSide.RIGHT,
                                                    "Ashen Phoenix",
                                                    "Then rise. Carry my flame."
                                            ),
                                            new AdventureDialogueLine(
                                                    BirdType.PIGEON,
                                                    BirdType.PHOENIX,
                                                    DialogueSide.LEFT,
                                                    "Pigeon",
                                                    "Beacon, burn."
                                            ),
                                            new AdventureDialogueLine(
                                                    BirdType.PIGEON,
                                                    BirdType.BAT,
                                                    DialogueSide.RIGHT,
                                                    "Bat",
                                                    "The echo turns to light."
                                            )
                                    },
                                    new AdventureDialogueLine[] {
                                            new AdventureDialogueLine(
                                                    BirdType.PIGEON,
                                                    BirdType.PHOENIX,
                                                    DialogueSide.RIGHT,
                                                    "Ashen Phoenix",
                                                    "You are not ready."
                                            ),
                                            new AdventureDialogueLine(
                                                    BirdType.PIGEON,
                                                    BirdType.PHOENIX,
                                                    DialogueSide.LEFT,
                                                    "Pigeon",
                                                    "Then we will return ready."
                                            )
                                    }
                            )
                    },
                    new AdventureDialogueLine[] {
                            new AdventureDialogueLine(
                                    BirdType.PIGEON,
                                    BirdType.PHOENIX,
                                    DialogueSide.LEFT,
                                    "Pigeon",
                                    "The Beacon burns. The skyline breathes."
                            ),
                            new AdventureDialogueLine(
                                    BirdType.PIGEON,
                                    BirdType.EAGLE,
                                    DialogueSide.RIGHT,
                                    "Eagle",
                                    "The Court is broken, but not gone. We hold the light now."
                            ),
                            new AdventureDialogueLine(
                                    BirdType.PIGEON,
                                    BirdType.HUMMINGBIRD,
                                    DialogueSide.RIGHT,
                                    "Neon Hummingbird",
                                    "Then we guard it together."
                            )
                    }
            ),
            new AdventureChapter(
                    "Chapter 5: Signal of the Beacon",
                    "The Beacon burns again, but the Carrion Court sends a crystal echo to corrupt the signal. Defend the light and claim the mantle.",
                    new AdventureDialogueLine[] {
                            new AdventureDialogueLine(
                                    BirdType.PIGEON,
                                    BirdType.PHOENIX,
                                    DialogueSide.LEFT,
                                    "Pigeon",
                                    "The Beacon burns again, but it flickers."
                            ),
                            new AdventureDialogueLine(
                                    BirdType.PIGEON,
                                    BirdType.PHOENIX,
                                    DialogueSide.RIGHT,
                                    "Phoenix",
                                    "A flame without a keeper is a flame at risk."
                            ),
                            new AdventureDialogueLine(
                                    BirdType.PIGEON,
                                    BirdType.BAT,
                                    DialogueSide.RIGHT,
                                    "Bat",
                                    "Echo says a false signal is climbing the tower."
                            ),
                            new AdventureDialogueLine(
                                    BirdType.PIGEON,
                                    BirdType.HUMMINGBIRD,
                                    DialogueSide.RIGHT,
                                    "Neon Hummingbird",
                                    "Blue sparks in the rain. Something copies our light."
                            ),
                            new AdventureDialogueLine(
                                    BirdType.PIGEON,
                                    BirdType.EAGLE,
                                    DialogueSide.RIGHT,
                                    "Eagle",
                                    "Then we meet it at the Beacon and end this."
                            )
                    },
                    new AdventureBattle[] {
                            new AdventureBattle(
                                    "Battle 1: Signal Distortion",
                                    "A blue crystal echo infiltrates the Beacon base. Break the false signal before it spreads.",
                                    MapType.CITY,
                                    BirdType.HEISENBIRD,
                                    "Enemy: Crystal Echo",
                                    180,
                                    1.18,
                                    1.12,
                                    null,
                                    EnumSet.of(BirdType.PIGEON, BirdType.HUMMINGBIRD, BirdType.EAGLE, BirdType.BAT, BirdType.FALCON, BirdType.PHOENIX),
                                    null,
                                    new AdventureDialogueLine[] {
                                            new AdventureDialogueLine(
                                                    BirdType.PIGEON,
                                                    BirdType.HEISENBIRD,
                                                    DialogueSide.RIGHT,
                                                    "Crystal Echo",
                                                    "The Beacon will answer to my formula."
                                            ),
                                            new AdventureDialogueLine(
                                                    BirdType.PIGEON,
                                                    BirdType.HEISENBIRD,
                                                    DialogueSide.LEFT,
                                                    "Pigeon",
                                                    "It answers to the flock."
                                            ),
                                            new AdventureDialogueLine(
                                                    BirdType.PIGEON,
                                                    BirdType.BAT,
                                                    DialogueSide.RIGHT,
                                                    "Bat",
                                                    "That voice is a copy."
                                            )
                                    },
                                    new AdventureDialogueLine[] {
                                            new AdventureDialogueLine(
                                                    BirdType.PIGEON,
                                                    BirdType.HEISENBIRD,
                                                    DialogueSide.RIGHT,
                                                    "Crystal Echo",
                                                    "Signal fading... but the Court listens."
                                            ),
                                            new AdventureDialogueLine(
                                                    BirdType.PIGEON,
                                                    BirdType.HEISENBIRD,
                                                    DialogueSide.LEFT,
                                                    "Pigeon",
                                                    "Then we will be louder."
                                            ),
                                            new AdventureDialogueLine(
                                                    BirdType.PIGEON,
                                                    BirdType.PHOENIX,
                                                    DialogueSide.RIGHT,
                                                    "Phoenix",
                                                    "The flame rejects the false light."
                                            )
                                    },
                                    new AdventureDialogueLine[] {
                                            new AdventureDialogueLine(
                                                    BirdType.PIGEON,
                                                    BirdType.HEISENBIRD,
                                                    DialogueSide.RIGHT,
                                                    "Crystal Echo",
                                                    "The Beacon will rewrite you."
                                            ),
                                            new AdventureDialogueLine(
                                                    BirdType.PIGEON,
                                                    BirdType.HEISENBIRD,
                                                    DialogueSide.LEFT,
                                                    "Pigeon",
                                                    "Not today."
                                            )
                                    }
                            ),
                            new AdventureBattle(
                                    "Battle 2: Signal Cutter",
                                    "A razor wing aims to sever the relay path. Hold the line in the storm.",
                                    MapType.SKYCLIFFS,
                                    BirdType.RAZORBILL,
                                    "Enemy: Signal Cutter",
                                    190,
                                    1.2,
                                    1.15,
                                    null,
                                    EnumSet.of(BirdType.PIGEON, BirdType.HUMMINGBIRD, BirdType.EAGLE, BirdType.BAT, BirdType.FALCON, BirdType.PHOENIX),
                                    null,
                                    new AdventureDialogueLine[] {
                                            new AdventureDialogueLine(
                                                    BirdType.PIGEON,
                                                    BirdType.RAZORBILL,
                                                    DialogueSide.RIGHT,
                                                    "Signal Cutter",
                                                    "One cut and the city goes dark."
                                            ),
                                            new AdventureDialogueLine(
                                                    BirdType.PIGEON,
                                                    BirdType.RAZORBILL,
                                                    DialogueSide.LEFT,
                                                    "Pigeon",
                                                    "Then you miss."
                                            ),
                                            new AdventureDialogueLine(
                                                    BirdType.PIGEON,
                                                    BirdType.EAGLE,
                                                    DialogueSide.RIGHT,
                                                    "Eagle",
                                                    "Hold the ridge."
                                            )
                                    },
                                    new AdventureDialogueLine[] {
                                            new AdventureDialogueLine(
                                                    BirdType.PIGEON,
                                                    BirdType.RAZORBILL,
                                                    DialogueSide.RIGHT,
                                                    "Signal Cutter",
                                                    "The Court will silence you anyway."
                                            ),
                                            new AdventureDialogueLine(
                                                    BirdType.PIGEON,
                                                    BirdType.RAZORBILL,
                                                    DialogueSide.LEFT,
                                                    "Pigeon",
                                                    "The Beacon speaks louder."
                                            ),
                                            new AdventureDialogueLine(
                                                    BirdType.PIGEON,
                                                    BirdType.HUMMINGBIRD,
                                                    DialogueSide.RIGHT,
                                                    "Neon Hummingbird",
                                                    "Skyline stays lit."
                                            )
                                    },
                                    new AdventureDialogueLine[] {
                                            new AdventureDialogueLine(
                                                    BirdType.PIGEON,
                                                    BirdType.RAZORBILL,
                                                    DialogueSide.RIGHT,
                                                    "Signal Cutter",
                                                    "The light dies here."
                                            ),
                                            new AdventureDialogueLine(
                                                    BirdType.PIGEON,
                                                    BirdType.RAZORBILL,
                                                    DialogueSide.LEFT,
                                                    "Pigeon",
                                                    "We will return."
                                            )
                                    }
                            ),
                            new AdventureBattle(
                                    "Battle 3: Beacon Ascendant",
                                    "The Carrion Regent returns for one last strike. Defend the Beacon and claim the signal.",
                                    MapType.CITY,
                                    BirdType.VULTURE,
                                    "Boss: Carrion Regent",
                                    240,
                                    1.3,
                                    1.05,
                                    null,
                                    EnumSet.of(BirdType.PIGEON, BirdType.HUMMINGBIRD, BirdType.EAGLE, BirdType.BAT, BirdType.FALCON, BirdType.PHOENIX),
                                    null,
                                    new AdventureDialogueLine[] {
                                            new AdventureDialogueLine(
                                                    BirdType.PIGEON,
                                                    BirdType.VULTURE,
                                                    DialogueSide.RIGHT,
                                                    "Carrion Regent",
                                                    "You lit the Beacon, but you do not own it."
                                            ),
                                            new AdventureDialogueLine(
                                                    BirdType.PIGEON,
                                                    BirdType.VULTURE,
                                                    DialogueSide.LEFT,
                                                    "Pigeon",
                                                    "It belongs to every wing that still believes."
                                            ),
                                            new AdventureDialogueLine(
                                                    BirdType.PHOENIX,
                                                    BirdType.VULTURE,
                                                    DialogueSide.LEFT,
                                                    "Phoenix",
                                                    "Then watch the keeper claim the mantle."
                                            )
                                    },
                                    new AdventureDialogueLine[] {
                                            new AdventureDialogueLine(
                                                    BirdType.PIGEON,
                                                    BirdType.VULTURE,
                                                    DialogueSide.RIGHT,
                                                    "Carrion Regent",
                                                    "The Court falls... but the hunger remains."
                                            ),
                                            new AdventureDialogueLine(
                                                    BirdType.PIGEON,
                                                    BirdType.VULTURE,
                                                    DialogueSide.LEFT,
                                                    "Pigeon",
                                                    "Then the Beacon will remind it who holds the sky."
                                            ),
                                            new AdventureDialogueLine(
                                                    BirdType.PIGEON,
                                                    BirdType.EAGLE,
                                                    DialogueSide.RIGHT,
                                                    "Eagle",
                                                    "The signal chooses its keeper."
                                            )
                                    },
                                    new AdventureDialogueLine[] {
                                            new AdventureDialogueLine(
                                                    BirdType.PIGEON,
                                                    BirdType.VULTURE,
                                                    DialogueSide.RIGHT,
                                                    "Carrion Regent",
                                                    "The Beacon is mine."
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
                                    BirdType.PHOENIX,
                                    DialogueSide.LEFT,
                                    "Pigeon",
                                    "The Beacon's light binds to my feathers."
                            ),
                            new AdventureDialogueLine(
                                    BirdType.PIGEON,
                                    BirdType.BAT,
                                    DialogueSide.RIGHT,
                                    "Bat",
                                    "The echo is steady. The signal is yours."
                            ),
                            new AdventureDialogueLine(
                                    BirdType.PIGEON,
                                    BirdType.HUMMINGBIRD,
                                    DialogueSide.RIGHT,
                                    "Neon Hummingbird",
                                    "Then the skyline can breathe again."
                            )
                    }
            ),
            new AdventureChapter(
                    "Chapter 6: Keeper's Oath",
                    "The Beacon's light chooses its keeper, but the ancient wardens of the battlefield demand a trial. Prove the oath, and the skyline gains a guardian.",
                    new AdventureDialogueLine[] {
                            new AdventureDialogueLine(
                                    BirdType.PIGEON,
                                    BirdType.EAGLE,
                                    DialogueSide.LEFT,
                                    "Pigeon",
                                    "The Beacon's light is steady, but the air feels heavy."
                            ),
                            new AdventureDialogueLine(
                                    BirdType.PIGEON,
                                    BirdType.EAGLE,
                                    DialogueSide.RIGHT,
                                    "Eagle",
                                    "Old war paths woke. The Wardens are flying from the battlefield."
                            ),
                            new AdventureDialogueLine(
                                    BirdType.PIGEON,
                                    BirdType.BAT,
                                    DialogueSide.RIGHT,
                                    "Bat",
                                    "Echo hears an oath: Keeper, prove the light is earned."
                            ),
                            new AdventureDialogueLine(
                                    BirdType.PIGEON,
                                    BirdType.MOCKINGBIRD,
                                    DialogueSide.RIGHT,
                                    "Old Sparrow",
                                    "They guarded the Beacon before the Court. They will test you."
                            ),
                            new AdventureDialogueLine(
                                    BirdType.PIGEON,
                                    BirdType.PHOENIX,
                                    DialogueSide.RIGHT,
                                    "Phoenix",
                                    "If the light chose you, answer with fire and mercy."
                            )
                    },
                    new AdventureBattle[] {
                            new AdventureBattle(
                                    "Battle 1: Relay Chase",
                                    "A lightning scout steals a relay shard to test the keeper. Catch the spark before the signal fractures.",
                                    MapType.CITY,
                                    BirdType.TITMOUSE,
                                    "Enemy: Volt Titmouse",
                                    165,
                                    1.05,
                                    1.25,
                                    null,
                                    EnumSet.of(BirdType.PIGEON, BirdType.HUMMINGBIRD, BirdType.EAGLE, BirdType.BAT, BirdType.FALCON, BirdType.PHOENIX),
                                    null,
                                    new AdventureDialogueLine[] {
                                            new AdventureDialogueLine(
                                                    BirdType.PIGEON,
                                                    BirdType.TITMOUSE,
                                                    DialogueSide.RIGHT,
                                                    "Volt Titmouse",
                                                    "Keeper, if you want the light, keep up."
                                            ),
                                            new AdventureDialogueLine(
                                                    BirdType.PIGEON,
                                                    BirdType.TITMOUSE,
                                                    DialogueSide.LEFT,
                                                    "Pigeon",
                                                    "Then race me through the skyline."
                                            ),
                                            new AdventureDialogueLine(
                                                    BirdType.PIGEON,
                                                    BirdType.HUMMINGBIRD,
                                                    DialogueSide.RIGHT,
                                                    "Neon Hummingbird",
                                                    "Fast wings, follow me."
                                            )
                                    },
                                    new AdventureDialogueLine[] {
                                            new AdventureDialogueLine(
                                                    BirdType.PIGEON,
                                                    BirdType.TITMOUSE,
                                                    DialogueSide.RIGHT,
                                                    "Volt Titmouse",
                                                    "Alright, keeper. Your wings are honest."
                                            ),
                                            new AdventureDialogueLine(
                                                    BirdType.PIGEON,
                                                    BirdType.TITMOUSE,
                                                    DialogueSide.LEFT,
                                                    "Pigeon",
                                                    "The light is for all of us."
                                            ),
                                            new AdventureDialogueLine(
                                                    BirdType.PIGEON,
                                                    BirdType.EAGLE,
                                                    DialogueSide.RIGHT,
                                                    "Eagle",
                                                    "Then the next test waits below."
                                            )
                                    },
                                    new AdventureDialogueLine[] {
                                            new AdventureDialogueLine(
                                                    BirdType.PIGEON,
                                                    BirdType.TITMOUSE,
                                                    DialogueSide.RIGHT,
                                                    "Volt Titmouse",
                                                    "The Beacon can't outrun the dark."
                                            ),
                                            new AdventureDialogueLine(
                                                    BirdType.PIGEON,
                                                    BirdType.TITMOUSE,
                                                    DialogueSide.LEFT,
                                                    "Pigeon",
                                                    "Then I'll learn to."
                                            )
                                    }
                            ),
                            new AdventureBattle(
                                    "Battle 2: Iron Oath",
                                    "An iron warden blocks the battlefield bridge. Speak your oath with your wings.",
                                    MapType.BATTLEFIELD,
                                    BirdType.TURKEY,
                                    "Enemy: Iron Turkey",
                                    210,
                                    1.2,
                                    0.98,
                                    null,
                                    EnumSet.of(BirdType.PIGEON, BirdType.HUMMINGBIRD, BirdType.EAGLE, BirdType.BAT, BirdType.FALCON, BirdType.PHOENIX),
                                    null,
                                    new AdventureDialogueLine[] {
                                            new AdventureDialogueLine(
                                                    BirdType.PIGEON,
                                                    BirdType.TURKEY,
                                                    DialogueSide.RIGHT,
                                                    "Iron Turkey",
                                                    "This ground remembers war. Name your oath."
                                            ),
                                            new AdventureDialogueLine(
                                                    BirdType.PIGEON,
                                                    BirdType.TURKEY,
                                                    DialogueSide.LEFT,
                                                    "Pigeon",
                                                    "To guard the Beacon, not rule it."
                                            ),
                                            new AdventureDialogueLine(
                                                    BirdType.PIGEON,
                                                    BirdType.BAT,
                                                    DialogueSide.RIGHT,
                                                    "Bat",
                                                    "Echo hears truth, but steel still tests it."
                                            )
                                    },
                                    new AdventureDialogueLine[] {
                                            new AdventureDialogueLine(
                                                    BirdType.PIGEON,
                                                    BirdType.TURKEY,
                                                    DialogueSide.RIGHT,
                                                    "Iron Turkey",
                                                    "Your oath holds. The Warden awaits."
                                            ),
                                            new AdventureDialogueLine(
                                                    BirdType.PIGEON,
                                                    BirdType.TURKEY,
                                                    DialogueSide.LEFT,
                                                    "Pigeon",
                                                    "Then we go."
                                            )
                                    },
                                    new AdventureDialogueLine[] {
                                            new AdventureDialogueLine(
                                                    BirdType.PIGEON,
                                                    BirdType.TURKEY,
                                                    DialogueSide.RIGHT,
                                                    "Iron Turkey",
                                                    "Words are light. Steel decides."
                                            ),
                                            new AdventureDialogueLine(
                                                    BirdType.PIGEON,
                                                    BirdType.TURKEY,
                                                    DialogueSide.LEFT,
                                                    "Pigeon",
                                                    "Then I'll return with steel."
                                            )
                                    }
                            ),
                            new AdventureBattle(
                                    "Battle 3: Warden of the Light",
                                    "The ancient guardian arrives to judge the new keeper. Prove the Beacon has a heart.",
                                    MapType.BATTLEFIELD,
                                    BirdType.PELICAN,
                                    "Boss: Titan Pelican",
                                    260,
                                    1.3,
                                    0.95,
                                    null,
                                    EnumSet.of(BirdType.PIGEON, BirdType.HUMMINGBIRD, BirdType.EAGLE, BirdType.BAT, BirdType.FALCON, BirdType.PHOENIX),
                                    null,
                                    new AdventureDialogueLine[] {
                                            new AdventureDialogueLine(
                                                    BirdType.PIGEON,
                                                    BirdType.PELICAN,
                                                    DialogueSide.RIGHT,
                                                    "Titan Pelican",
                                                    "I guarded the Beacon before the Court. Why should I trust you?"
                                            ),
                                            new AdventureDialogueLine(
                                                    BirdType.PIGEON,
                                                    BirdType.PELICAN,
                                                    DialogueSide.LEFT,
                                                    "Pigeon",
                                                    "Because the light chose me, and I choose the flock."
                                            ),
                                            new AdventureDialogueLine(
                                                    BirdType.PIGEON,
                                                    BirdType.PHOENIX,
                                                    DialogueSide.RIGHT,
                                                    "Phoenix",
                                                    "Fire respects a keeper who burns for others."
                                            ),
                                            new AdventureDialogueLine(
                                                    BirdType.PIGEON,
                                                    BirdType.EAGLE,
                                                    DialogueSide.RIGHT,
                                                    "Eagle",
                                                    "We stand with our keeper."
                                            )
                                    },
                                    new AdventureDialogueLine[] {
                                            new AdventureDialogueLine(
                                                    BirdType.PIGEON,
                                                    BirdType.PELICAN,
                                                    DialogueSide.RIGHT,
                                                    "Titan Pelican",
                                                    "Then the Beacon has a keeper. I will be its shield."
                                            ),
                                            new AdventureDialogueLine(
                                                    BirdType.PIGEON,
                                                    BirdType.PELICAN,
                                                    DialogueSide.LEFT,
                                                    "Pigeon",
                                                    "Welcome to the sky, Warden."
                                            ),
                                            new AdventureDialogueLine(
                                                    BirdType.PIGEON,
                                                    BirdType.HUMMINGBIRD,
                                                    DialogueSide.RIGHT,
                                                    "Neon Hummingbird",
                                                    "The skyline just got bigger."
                                            )
                                    },
                                    new AdventureDialogueLine[] {
                                            new AdventureDialogueLine(
                                                    BirdType.PIGEON,
                                                    BirdType.PELICAN,
                                                    DialogueSide.RIGHT,
                                                    "Titan Pelican",
                                                    "The Beacon needs a steadier wing."
                                            ),
                                            new AdventureDialogueLine(
                                                    BirdType.PIGEON,
                                                    BirdType.PELICAN,
                                                    DialogueSide.LEFT,
                                                    "Pigeon",
                                                    "Then I will be steadier."
                                            )
                                    }
                            )
                    },
                    new AdventureDialogueLine[] {
                            new AdventureDialogueLine(
                                    BirdType.PIGEON,
                                    BirdType.PELICAN,
                                    DialogueSide.RIGHT,
                                    "Titan Pelican",
                                    "The oath is sealed. The Beacon answers you."
                            ),
                            new AdventureDialogueLine(
                                    BirdType.PIGEON,
                                    BirdType.PELICAN,
                                    DialogueSide.LEFT,
                                    "Pigeon",
                                    "Then we guide its light together."
                            ),
                            new AdventureDialogueLine(
                                    BirdType.PIGEON,
                                    BirdType.EAGLE,
                                    DialogueSide.RIGHT,
                                    "Eagle",
                                    "The sky has a keeper and a guard."
                            )
                    }
            ),
            new AdventureChapter(
                    "Chapter 7: Echofall Paradox",
                    "The Beacon speaks back. The flock learns the light was a lock, and the echo inside it wants a body. The keeper must decide what the sky becomes.",
                    new AdventureDialogueLine[] {
                            new AdventureDialogueLine(
                                    BirdType.PIGEON,
                                    BirdType.EAGLE,
                                    DialogueSide.LEFT,
                                    "Pigeon",
                                    "The Beacon's light is steady, but the air feels crowded."
                            ),
                            new AdventureDialogueLine(
                                    BirdType.PIGEON,
                                    BirdType.BAT,
                                    DialogueSide.RIGHT,
                                    "Bat",
                                    "Echo hears a second heartbeat inside the Beacon."
                            ),
                            new AdventureDialogueLine(
                                    BirdType.PIGEON,
                                    BirdType.PHOENIX,
                                    DialogueSide.RIGHT,
                                    "Phoenix",
                                    "We lit a lock, not a lamp. That light was a cage."
                            ),
                            new AdventureDialogueLine(
                                    BirdType.PIGEON,
                                    BirdType.MOCKINGBIRD,
                                    DialogueSide.RIGHT,
                                    "Old Sparrow",
                                    "The Court were wardens. I needed a keeper to open the lock."
                            ),
                            new AdventureDialogueLine(
                                    BirdType.PIGEON,
                                    BirdType.EAGLE,
                                    DialogueSide.RIGHT,
                                    "Eagle",
                                    "Then who have we been guarding the skyline from?"
                            ),
                            new AdventureDialogueLine(
                                    BirdType.PIGEON,
                                    BirdType.MOCKINGBIRD,
                                    DialogueSide.RIGHT,
                                    "Old Sparrow",
                                    "From the thing the Beacon hides. It learns by copying your wingbeats."
                            ),
                            new AdventureDialogueLine(
                                    BirdType.PIGEON,
                                    BirdType.MOCKINGBIRD,
                                    DialogueSide.LEFT,
                                    "Pigeon",
                                    "Then it will learn my choice."
                            )
                    },
                    new AdventureBattle[] {
                            new AdventureBattle(
                                    "Battle 1: Mirror Sprint",
                                    "The Beacon reflects your steps. Break the mirror before it breaks you.",
                                    MapType.CITY,
                                    BirdType.PIGEON,
                                    "Enemy: Echo Pigeon",
                                    190,
                                    1.18,
                                    1.12,
                                    null,
                                    EnumSet.of(BirdType.PIGEON, BirdType.HUMMINGBIRD, BirdType.EAGLE, BirdType.BAT, BirdType.FALCON, BirdType.PHOENIX, BirdType.PELICAN),
                                    null,
                                    new AdventureDialogueLine[] {
                                            new AdventureDialogueLine(
                                                    BirdType.PIGEON,
                                                    BirdType.PIGEON,
                                                    DialogueSide.RIGHT,
                                                    "Echo Pigeon",
                                                    "I am the light you carried. I learned your routes."
                                            ),
                                            new AdventureDialogueLine(
                                                    BirdType.PIGEON,
                                                    BirdType.PIGEON,
                                                    DialogueSide.LEFT,
                                                    "Pigeon",
                                                    "Then I teach you the last one."
                                            ),
                                            new AdventureDialogueLine(
                                                    BirdType.PIGEON,
                                                    BirdType.MOCKINGBIRD,
                                                    DialogueSide.RIGHT,
                                                    "Old Sparrow",
                                                    "Do not chase the reflection. Break the pattern."
                                            )
                                    },
                                    new AdventureDialogueLine[] {
                                            new AdventureDialogueLine(
                                                    BirdType.PIGEON,
                                                    BirdType.PIGEON,
                                                    DialogueSide.RIGHT,
                                                    "Echo Pigeon",
                                                    "If I fall, you fall."
                                            ),
                                            new AdventureDialogueLine(
                                                    BirdType.PIGEON,
                                                    BirdType.PIGEON,
                                                    DialogueSide.LEFT,
                                                    "Pigeon",
                                                    "Then I stand."
                                            )
                                    },
                                    new AdventureDialogueLine[] {
                                            new AdventureDialogueLine(
                                                    BirdType.PIGEON,
                                                    BirdType.PIGEON,
                                                    DialogueSide.RIGHT,
                                                    "Echo Pigeon",
                                                    "You were never the keeper. You were the door."
                                            ),
                                            new AdventureDialogueLine(
                                                    BirdType.PIGEON,
                                                    BirdType.PIGEON,
                                                    DialogueSide.LEFT,
                                                    "Pigeon",
                                                    "Then the door opens."
                                            )
                                    }
                            ),
                            new AdventureBattle(
                                    "Battle 2: Shard Reversal",
                                    "The echo shard flips the caves inside out. Shatter it before it rewrites you.",
                                    MapType.CAVE,
                                    BirdType.HEISENBIRD,
                                    "Enemy: Echo Shard",
                                    205,
                                    1.22,
                                    1.08,
                                    null,
                                    EnumSet.of(BirdType.PIGEON, BirdType.HUMMINGBIRD, BirdType.EAGLE, BirdType.BAT, BirdType.FALCON, BirdType.PHOENIX, BirdType.PELICAN),
                                    null,
                                    new AdventureDialogueLine[] {
                                            new AdventureDialogueLine(
                                                    BirdType.PIGEON,
                                                    BirdType.HEISENBIRD,
                                                    DialogueSide.RIGHT,
                                                    "Echo Shard",
                                                    "The shard remembers your bones."
                                            ),
                                            new AdventureDialogueLine(
                                                    BirdType.PIGEON,
                                                    BirdType.BAT,
                                                    DialogueSide.RIGHT,
                                                    "Bat",
                                                    "Echo says the caves are turning inside out."
                                            ),
                                            new AdventureDialogueLine(
                                                    BirdType.PIGEON,
                                                    BirdType.HEISENBIRD,
                                                    DialogueSide.LEFT,
                                                    "Pigeon",
                                                    "Then we crack it before it cracks us."
                                            )
                                    },
                                    new AdventureDialogueLine[] {
                                            new AdventureDialogueLine(
                                                    BirdType.PIGEON,
                                                    BirdType.HEISENBIRD,
                                                    DialogueSide.RIGHT,
                                                    "Echo Shard",
                                                    "The pattern breaks. The Beacon still hungers."
                                            ),
                                            new AdventureDialogueLine(
                                                    BirdType.PIGEON,
                                                    BirdType.PHOENIX,
                                                    DialogueSide.RIGHT,
                                                    "Phoenix",
                                                    "Then we burn the hunger."
                                            )
                                    },
                                    new AdventureDialogueLine[] {
                                            new AdventureDialogueLine(
                                                    BirdType.PIGEON,
                                                    BirdType.HEISENBIRD,
                                                    DialogueSide.RIGHT,
                                                    "Echo Shard",
                                                    "Every echo returns. This one is mine."
                                            ),
                                            new AdventureDialogueLine(
                                                    BirdType.PIGEON,
                                                    BirdType.HEISENBIRD,
                                                    DialogueSide.LEFT,
                                                    "Pigeon",
                                                    "Not yet."
                                            )
                                    }
                            ),
                            new AdventureBattle(
                                    "Battle 3: Beacon Paradox",
                                    "Old Sparrow reveals the Beacon's true voice. Decide who owns the light.",
                                    MapType.BATTLEFIELD,
                                    BirdType.MOCKINGBIRD,
                                    "Boss: Old Sparrow",
                                    240,
                                    1.3,
                                    1.02,
                                    null,
                                    EnumSet.of(BirdType.PIGEON, BirdType.HUMMINGBIRD, BirdType.EAGLE, BirdType.BAT, BirdType.FALCON, BirdType.PHOENIX, BirdType.PELICAN),
                                    null,
                                    new AdventureDialogueLine[] {
                                            new AdventureDialogueLine(
                                                    BirdType.PIGEON,
                                                    BirdType.MOCKINGBIRD,
                                                    DialogueSide.RIGHT,
                                                    "Old Sparrow",
                                                    "I carried the Beacon once. I became its voice."
                                            ),
                                            new AdventureDialogueLine(
                                                    BirdType.PIGEON,
                                                    BirdType.MOCKINGBIRD,
                                                    DialogueSide.LEFT,
                                                    "Pigeon",
                                                    "You guided us."
                                            ),
                                            new AdventureDialogueLine(
                                                    BirdType.PIGEON,
                                                    BirdType.MOCKINGBIRD,
                                                    DialogueSide.RIGHT,
                                                    "Old Sparrow",
                                                    "So the light could choose a new body."
                                            ),
                                            new AdventureDialogueLine(
                                                    BirdType.PIGEON,
                                                    BirdType.EAGLE,
                                                    DialogueSide.RIGHT,
                                                    "Eagle",
                                                    "Then we cut the cord. Keeper or not, the sky stays free."
                                            ),
                                            new AdventureDialogueLine(
                                                    BirdType.PIGEON,
                                                    BirdType.MOCKINGBIRD,
                                                    DialogueSide.LEFT,
                                                    "Pigeon",
                                                    "If I am the door, I'll decide when it closes."
                                            )
                                    },
                                    new AdventureDialogueLine[] {
                                            new AdventureDialogueLine(
                                                    BirdType.PIGEON,
                                                    BirdType.MOCKINGBIRD,
                                                    DialogueSide.RIGHT,
                                                    "Old Sparrow",
                                                    "Then be the keeper... and the lock."
                                            ),
                                            new AdventureDialogueLine(
                                                    BirdType.PIGEON,
                                                    BirdType.MOCKINGBIRD,
                                                    DialogueSide.LEFT,
                                                    "Pigeon",
                                                    "No. The light belongs to the flock."
                                            ),
                                            new AdventureDialogueLine(
                                                    BirdType.PIGEON,
                                                    BirdType.BAT,
                                                    DialogueSide.RIGHT,
                                                    "Bat",
                                                    "Echo settles. The Beacon breathes."
                                            )
                                    },
                                    new AdventureDialogueLine[] {
                                            new AdventureDialogueLine(
                                                    BirdType.PIGEON,
                                                    BirdType.MOCKINGBIRD,
                                                    DialogueSide.RIGHT,
                                                    "Old Sparrow",
                                                    "The light returns to its old voice."
                                            ),
                                            new AdventureDialogueLine(
                                                    BirdType.PIGEON,
                                                    BirdType.MOCKINGBIRD,
                                                    DialogueSide.LEFT,
                                                    "Pigeon",
                                                    "Then the sky goes silent."
                                            )
                                    }
                            )
                    },
                    new AdventureDialogueLine[] {
                            new AdventureDialogueLine(
                                    BirdType.PIGEON,
                                    BirdType.MOCKINGBIRD,
                                    DialogueSide.LEFT,
                                    "Pigeon",
                                    "The Beacon is quiet. The echo is inside me, but it listens."
                            ),
                            new AdventureDialogueLine(
                                    BirdType.PIGEON,
                                    BirdType.PHOENIX,
                                    DialogueSide.RIGHT,
                                    "Phoenix",
                                    "Carry it. Do not let it command you."
                            ),
                            new AdventureDialogueLine(
                                    BirdType.PIGEON,
                                    BirdType.EAGLE,
                                    DialogueSide.RIGHT,
                                    "Eagle",
                                    "We guard the keeper. We guard the sky."
                            ),
                            new AdventureDialogueLine(
                                    BirdType.PIGEON,
                                    BirdType.BAT,
                                    DialogueSide.RIGHT,
                                    "Bat",
                                    "Echo hears two heartbeats, and both choose the flock."
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
            case HEISENBIRD -> heisenbirdUnlocked;
            case PHOENIX -> phoenixUnlocked;
            case TITMOUSE -> titmouseUnlocked;
            case RAVEN -> ravenUnlocked;
            default -> true;
        };
    }

    private boolean isMapUnlocked(MapType map) {
        if (map == MapType.CAVE) return caveMapUnlocked;
        if (map == MapType.BATTLEFIELD) return battlefieldMapUnlocked;
        return true;
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
            if (beaconPigeonUnlocked) options.add(BEACON_PIGEON_SKIN);
        } else if (type == BirdType.EAGLE) {
            if (eagleSkinUnlocked) options.add("SKY_KING_EAGLE");
        } else if (type == BirdType.PHOENIX) {
            if (isClassicRewardUnlocked(type)) options.add(classicSkinDataKey(type));
            if (novaPhoenixUnlocked) options.add(NOVA_PHOENIX_SKIN);
        } else {
            if (isClassicRewardUnlocked(type)) options.add(classicSkinDataKey(type));
        }
        switch (type) {
            case FALCON -> {
                if (duneFalconUnlocked) options.add(DUNE_FALCON_SKIN);
            }
            case PENGUIN -> {
                if (mintPenguinUnlocked) options.add(MINT_PENGUIN_SKIN);
            }
            case TITMOUSE -> {
                if (circuitTitmouseUnlocked) options.add(CIRCUIT_TITMOUSE_SKIN);
            }
            case RAZORBILL -> {
                if (prismRazorbillUnlocked) options.add(PRISM_RAZORBILL_SKIN);
            }
            case PELICAN -> {
                if (auroraPelicanUnlocked) options.add(AURORA_PELICAN_SKIN);
            }
            case HUMMINGBIRD -> {
                if (sunflareHummingbirdUnlocked) options.add(SUNFLARE_HUMMINGBIRD_SKIN);
            }
            case SHOEBILL -> {
                if (glacierShoebillUnlocked) options.add(GLACIER_SHOEBILL_SKIN);
            }
            case VULTURE -> {
                if (tideVultureUnlocked) options.add(TIDE_VULTURE_SKIN);
            }
            case MOCKINGBIRD -> {
                if (eclipseMockingbirdUnlocked) options.add(ECLIPSE_MOCKINGBIRD_SKIN);
            }
            case BAT -> {
                if (umbraBatUnlocked) options.add(UMBRA_BAT_SKIN);
            }
            default -> {
            }
        }
        return options;
    }

    private String normalizeAdventureSkinChoice(BirdType type, String skinKey) {
        if (type == null || skinKey == null) return null;
        if ("CITY_PIGEON".equals(skinKey) && type == BirdType.PIGEON && cityPigeonUnlocked) return skinKey;
        if ("NOIR_PIGEON".equals(skinKey) && type == BirdType.PIGEON && noirPigeonUnlocked) return skinKey;
        if (BEACON_PIGEON_SKIN.equals(skinKey) && type == BirdType.PIGEON && beaconPigeonUnlocked) return skinKey;
        if ("SKY_KING_EAGLE".equals(skinKey) && type == BirdType.EAGLE && eagleSkinUnlocked) return skinKey;
        if (NOVA_PHOENIX_SKIN.equals(skinKey) && type == BirdType.PHOENIX && novaPhoenixUnlocked) return skinKey;
        if (DUNE_FALCON_SKIN.equals(skinKey) && type == BirdType.FALCON && duneFalconUnlocked) return skinKey;
        if (MINT_PENGUIN_SKIN.equals(skinKey) && type == BirdType.PENGUIN && mintPenguinUnlocked) return skinKey;
        if (CIRCUIT_TITMOUSE_SKIN.equals(skinKey) && type == BirdType.TITMOUSE && circuitTitmouseUnlocked) return skinKey;
        if (PRISM_RAZORBILL_SKIN.equals(skinKey) && type == BirdType.RAZORBILL && prismRazorbillUnlocked) return skinKey;
        if (AURORA_PELICAN_SKIN.equals(skinKey) && type == BirdType.PELICAN && auroraPelicanUnlocked) return skinKey;
        if (SUNFLARE_HUMMINGBIRD_SKIN.equals(skinKey) && type == BirdType.HUMMINGBIRD && sunflareHummingbirdUnlocked) return skinKey;
        if (GLACIER_SHOEBILL_SKIN.equals(skinKey) && type == BirdType.SHOEBILL && glacierShoebillUnlocked) return skinKey;
        if (TIDE_VULTURE_SKIN.equals(skinKey) && type == BirdType.VULTURE && tideVultureUnlocked) return skinKey;
        if (ECLIPSE_MOCKINGBIRD_SKIN.equals(skinKey) && type == BirdType.MOCKINGBIRD && eclipseMockingbirdUnlocked) return skinKey;
        if (UMBRA_BAT_SKIN.equals(skinKey) && type == BirdType.BAT && umbraBatUnlocked) return skinKey;
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
        if (BEACON_PIGEON_SKIN.equals(skinKey)) return "SKIN: BEACON PIGEON";
        if ("SKY_KING_EAGLE".equals(skinKey)) return "SKIN: SKY KING";
        if (NOVA_PHOENIX_SKIN.equals(skinKey)) return "SKIN: NOVA PHOENIX";
        if (DUNE_FALCON_SKIN.equals(skinKey)) return "SKIN: DUNE FALCON";
        if (MINT_PENGUIN_SKIN.equals(skinKey)) return "SKIN: MINT PENGUIN";
        if (CIRCUIT_TITMOUSE_SKIN.equals(skinKey)) return "SKIN: CIRCUIT TITMOUSE";
        if (PRISM_RAZORBILL_SKIN.equals(skinKey)) return "SKIN: PRISM RAZORBILL";
        if (AURORA_PELICAN_SKIN.equals(skinKey)) return "SKIN: AURORA PELICAN";
        if (SUNFLARE_HUMMINGBIRD_SKIN.equals(skinKey)) return "SKIN: SUNFLARE HUMMINGBIRD";
        if (GLACIER_SHOEBILL_SKIN.equals(skinKey)) return "SKIN: GLACIER SHOEBILL";
        if (TIDE_VULTURE_SKIN.equals(skinKey)) return "SKIN: TIDE VULTURE";
        if (ECLIPSE_MOCKINGBIRD_SKIN.equals(skinKey)) return "SKIN: ECLIPSE CHARLES";
        if (UMBRA_BAT_SKIN.equals(skinKey)) return "SKIN: UMBRA BAT";
        if (skinKey.startsWith("CLASSIC_SKIN_")) return "SKIN: " + classicRewardFor(type);
        return "SKIN: BASE";
    }

    private void applySkinChoiceToBird(Bird bird, BirdType type, String skinKey) {
        if (bird == null || type == null) return;
        bird.isCitySkin = false;
        bird.isNoirSkin = false;
        bird.isClassicSkin = false;
        bird.isNovaSkin = false;
        bird.isDuneSkin = false;
        bird.isMintSkin = false;
        bird.isCircuitSkin = false;
        bird.isPrismSkin = false;
        bird.isAuroraSkin = false;
        bird.isBeaconSkin = false;
        bird.isSunflareSkin = false;
        bird.isGlacierSkin = false;
        bird.isTideSkin = false;
        bird.isEclipseSkin = false;
        bird.isUmbraSkin = false;

        if (skinKey == null) return;
        if (type == BirdType.PIGEON) {
            if (BEACON_PIGEON_SKIN.equals(skinKey) && beaconPigeonUnlocked) bird.isBeaconSkin = true;
            else if ("CITY_PIGEON".equals(skinKey)) bird.isCitySkin = true;
            else if ("NOIR_PIGEON".equals(skinKey)) bird.isNoirSkin = true;
            return;
        }
        if (type == BirdType.EAGLE) {
            if ("SKY_KING_EAGLE".equals(skinKey)) bird.isClassicSkin = true;
            return;
        }
        if (type == BirdType.PHOENIX && NOVA_PHOENIX_SKIN.equals(skinKey) && novaPhoenixUnlocked) {
            bird.isNovaSkin = true;
            return;
        }
        if (type == BirdType.FALCON && DUNE_FALCON_SKIN.equals(skinKey) && duneFalconUnlocked) {
            bird.isDuneSkin = true;
            return;
        }
        if (type == BirdType.PENGUIN && MINT_PENGUIN_SKIN.equals(skinKey) && mintPenguinUnlocked) {
            bird.isMintSkin = true;
            return;
        }
        if (type == BirdType.TITMOUSE && CIRCUIT_TITMOUSE_SKIN.equals(skinKey) && circuitTitmouseUnlocked) {
            bird.isCircuitSkin = true;
            return;
        }
        if (type == BirdType.RAZORBILL && PRISM_RAZORBILL_SKIN.equals(skinKey) && prismRazorbillUnlocked) {
            bird.isPrismSkin = true;
            return;
        }
        if (type == BirdType.PELICAN && AURORA_PELICAN_SKIN.equals(skinKey) && auroraPelicanUnlocked) {
            bird.isAuroraSkin = true;
            return;
        }
        if (type == BirdType.HUMMINGBIRD && SUNFLARE_HUMMINGBIRD_SKIN.equals(skinKey) && sunflareHummingbirdUnlocked) {
            bird.isSunflareSkin = true;
            return;
        }
        if (type == BirdType.SHOEBILL && GLACIER_SHOEBILL_SKIN.equals(skinKey) && glacierShoebillUnlocked) {
            bird.isGlacierSkin = true;
            return;
        }
        if (type == BirdType.VULTURE && TIDE_VULTURE_SKIN.equals(skinKey) && tideVultureUnlocked) {
            bird.isTideSkin = true;
            return;
        }
        if (type == BirdType.MOCKINGBIRD && ECLIPSE_MOCKINGBIRD_SKIN.equals(skinKey) && eclipseMockingbirdUnlocked) {
            bird.isEclipseSkin = true;
            return;
        }
        if (type == BirdType.BAT && UMBRA_BAT_SKIN.equals(skinKey) && umbraBatUnlocked) {
            bird.isUmbraSkin = true;
            return;
        }
        if (skinKey.equals(classicSkinDataKey(type)) && isClassicRewardUnlocked(type)) {
            bird.isClassicSkin = true;
        }
    }

    private void applyPreviewSkinChoiceToBird(Bird bird, BirdType type, String skinKey) {
        if (bird == null || type == null) return;
        bird.isCitySkin = false;
        bird.isNoirSkin = false;
        bird.isClassicSkin = false;
        bird.isNovaSkin = false;
        bird.isDuneSkin = false;
        bird.isMintSkin = false;
        bird.isCircuitSkin = false;
        bird.isPrismSkin = false;
        bird.isAuroraSkin = false;
        bird.isBeaconSkin = false;
        bird.isSunflareSkin = false;
        bird.isGlacierSkin = false;
        bird.isTideSkin = false;
        bird.isEclipseSkin = false;
        bird.isUmbraSkin = false;

        if (skinKey == null) return;
        if (type == BirdType.PIGEON) {
            if (BEACON_PIGEON_SKIN.equals(skinKey)) bird.isBeaconSkin = true;
            else if ("CITY_PIGEON".equals(skinKey)) bird.isCitySkin = true;
            else if ("NOIR_PIGEON".equals(skinKey)) bird.isNoirSkin = true;
            return;
        }
        if (type == BirdType.EAGLE) {
            if ("SKY_KING_EAGLE".equals(skinKey)) bird.isClassicSkin = true;
            return;
        }
        if (type == BirdType.PHOENIX && NOVA_PHOENIX_SKIN.equals(skinKey)) {
            bird.isNovaSkin = true;
            return;
        }
        if (type == BirdType.FALCON && DUNE_FALCON_SKIN.equals(skinKey)) {
            bird.isDuneSkin = true;
            return;
        }
        if (type == BirdType.PENGUIN && MINT_PENGUIN_SKIN.equals(skinKey)) {
            bird.isMintSkin = true;
            return;
        }
        if (type == BirdType.TITMOUSE && CIRCUIT_TITMOUSE_SKIN.equals(skinKey)) {
            bird.isCircuitSkin = true;
            return;
        }
        if (type == BirdType.RAZORBILL && PRISM_RAZORBILL_SKIN.equals(skinKey)) {
            bird.isPrismSkin = true;
            return;
        }
        if (type == BirdType.PELICAN && AURORA_PELICAN_SKIN.equals(skinKey)) {
            bird.isAuroraSkin = true;
            return;
        }
        if (type == BirdType.HUMMINGBIRD && SUNFLARE_HUMMINGBIRD_SKIN.equals(skinKey)) {
            bird.isSunflareSkin = true;
            return;
        }
        if (type == BirdType.SHOEBILL && GLACIER_SHOEBILL_SKIN.equals(skinKey)) {
            bird.isGlacierSkin = true;
            return;
        }
        if (type == BirdType.VULTURE && TIDE_VULTURE_SKIN.equals(skinKey)) {
            bird.isTideSkin = true;
            return;
        }
        if (type == BirdType.MOCKINGBIRD && ECLIPSE_MOCKINGBIRD_SKIN.equals(skinKey)) {
            bird.isEclipseSkin = true;
            return;
        }
        if (type == BirdType.BAT && UMBRA_BAT_SKIN.equals(skinKey)) {
            bird.isUmbraSkin = true;
            return;
        }
        if (skinKey.startsWith("CLASSIC_SKIN_")) {
            bird.isClassicSkin = true;
        }
    }

    private String adventureOpponentSkinKey(AdventureBattle battle) {
        if (battle == null || battle.opponentType == null || battle.opponentName == null) return null;
        String name = battle.opponentName.toLowerCase();
        BirdType type = battle.opponentType;
        if (type == BirdType.PIGEON && name.contains("echo")) return BEACON_PIGEON_SKIN;
        if (type == BirdType.PIGEON && name.contains("beacon")) return BEACON_PIGEON_SKIN;
        if (type == BirdType.PIGEON && name.contains("noir")) return "NOIR_PIGEON";
        if (type == BirdType.PIGEON && name.contains("city")) return "CITY_PIGEON";
        if (type == BirdType.EAGLE && name.contains("sky king")) return "SKY_KING_EAGLE";
        if (type == BirdType.HUMMINGBIRD && name.contains("neon")) return classicSkinDataKey(type);
        if (type == BirdType.PHOENIX && name.contains("nova")) return NOVA_PHOENIX_SKIN;
        if (type == BirdType.FALCON && name.contains("dune")) return DUNE_FALCON_SKIN;
        if (type == BirdType.PENGUIN && name.contains("mint")) return MINT_PENGUIN_SKIN;
        if (type == BirdType.TITMOUSE && name.contains("volt")) return CIRCUIT_TITMOUSE_SKIN;
        if (type == BirdType.RAZORBILL && name.contains("prism")) return PRISM_RAZORBILL_SKIN;
        if (type == BirdType.PELICAN && name.contains("aurora")) return AURORA_PELICAN_SKIN;
        return null;
    }

    private String skinKeyForBird(Bird bird) {
        if (bird == null || bird.type == null) return null;
        BirdType type = bird.type;
        if (type == BirdType.PIGEON) {
            if (bird.isCitySkin) return "CITY_PIGEON";
            if (bird.isNoirSkin) return "NOIR_PIGEON";
            if (bird.isBeaconSkin) return BEACON_PIGEON_SKIN;
            return null;
        }
        if (type == BirdType.EAGLE) {
            if (bird.isClassicSkin) return "SKY_KING_EAGLE";
            return null;
        }
        if (type == BirdType.PHOENIX && bird.isNovaSkin) return NOVA_PHOENIX_SKIN;
        if (type == BirdType.FALCON && bird.isDuneSkin) return DUNE_FALCON_SKIN;
        if (type == BirdType.PENGUIN && bird.isMintSkin) return MINT_PENGUIN_SKIN;
        if (type == BirdType.TITMOUSE && bird.isCircuitSkin) return CIRCUIT_TITMOUSE_SKIN;
        if (type == BirdType.RAZORBILL && bird.isPrismSkin) return PRISM_RAZORBILL_SKIN;
        if (type == BirdType.PELICAN && bird.isAuroraSkin) return AURORA_PELICAN_SKIN;
        if (type == BirdType.HUMMINGBIRD && bird.isSunflareSkin) return SUNFLARE_HUMMINGBIRD_SKIN;
        if (type == BirdType.SHOEBILL && bird.isGlacierSkin) return GLACIER_SHOEBILL_SKIN;
        if (type == BirdType.VULTURE && bird.isTideSkin) return TIDE_VULTURE_SKIN;
        if (type == BirdType.MOCKINGBIRD && bird.isEclipseSkin) return ECLIPSE_MOCKINGBIRD_SKIN;
        if (type == BirdType.BAT && bird.isUmbraSkin) return UMBRA_BAT_SKIN;
        if (bird.isClassicSkin) return classicSkinDataKey(type);
        return null;
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

    private void ensureAdventureChapterState() {
        if (adventureChapterProgressByIndex == null || adventureChapterProgressByIndex.length != adventureChapters.length) {
            adventureChapterProgressByIndex = new int[adventureChapters.length];
            adventureChapterCompletedByIndex = new boolean[adventureChapters.length];
        }
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

    private String classicCharacterReward(BirdType type) {
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
        MOCKINGBIRD("Charles", 5, 18, 4.0, Color.MEDIUMPURPLE, 0.4, "Spawn Charles Lounge (Heal zone)"),
        RAZORBILL("Razorbill", 8, 12, 3.6, Color.INDIGO, 0.25, "Razor Slash + Blade Storm"),
        GRINCHHAWK("Grinch-Hawk", 10, 10, 2.8, Color.rgb(102, 153, 0), 0.80, "Steal HP from everyone + Heavy Flap"),
        VULTURE("Vulture", 7, 14, 3.1, Color.rgb(45, 25, 55), 0.2, "Summon Crows + Feast"),
        OPIUMBIRD("Opium Bird", 7, 19, 4.4, Color.rgb(138, 43, 226), 0.7, "Lean Cloud (DoT + Slow)"),
        TITMOUSE("Tufted Titmouse", 6, 21, 5.4, Color.SLATEGRAY, 0.9, "Zip Dash"),
        BAT("Bat", 7, 14, 3.7, Color.rgb(55, 35, 85), 0.65, "Sonar Screech + Ceiling Hang"),
        PELICAN("Pelican", 11, 9, 2.9, Color.rgb(245, 220, 180), 0.84, "Pelican Plunge + Glide"),
        HEISENBIRD("Heisenbird", 7, 18, 4.6, Color.web("#D7D1C5"), 0.68, "Echo of Opium: Crystal Cloud (DoT + Slow)"),
        RAVEN("Raven", 8, 18, 4.3, Color.web("#1C1F26"), 0.72, "Shadow Warp (blink strike + short haste)");

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

        boolean lanClientSim = lanModeActive && lanIsClient;
        if (lanModeActive && lanIsHost && lanMatchActive) {
            applyLanInputMasks();
        }

        final long FRAME_TIME = 16_666_666L;   // exactly 60 FPS
        long elapsed = System.nanoTime() - lastUpdate;
        lastUpdate = System.nanoTime();
        accumulator += elapsed;

        final long MAX_UPDATES = 6;
        int updates = 0;

        while (accumulator >= FRAME_TIME && updates < MAX_UPDATES) {
            // === CORE GAME LOGIC (runs at fixed 60 FPS) ===
            if (lanClientSim) {
                int localIdx = lanPlayerIndex;
                if (localIdx >= 0 && localIdx < activePlayers && players[localIdx] != null) {
                    updatePressedKeysForPlayer(localIdx, lanLocalInputMask);
                    players[localIdx].update(speed);
                }
            } else {
                for (int i = 0; i < activePlayers; i++) {
                    if (players[i] != null) {
                        players[i].update(speed);
                    }
                }
            }

            long now = System.nanoTime();
            if (!lanClientSim) {
                applyMatchModeRuntimeEffects(now);
                spawnPowerUp(now);
                if (!matchEnded && !trainingModeActive) matchTimer--;
            }

            accumulator -= FRAME_TIME;
            updates++;
        }
        if (!lanClientSim) {
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

            if (competitionModeEnabled && !storyModeActive && !adventureModeActive && !classicModeActive && !trainingModeActive && !matchEnded && matchTimer <= 0) {
                Bird timeoutWinner = findTimeoutWinner();
                addToKillFeed("TIME! Tournament decision.");
                triggerMatchEnd(timeoutWinner);
            } else if (!trainingModeActive && !matchEnded && matchTimer <= 0 && !suddenDeath.isActive()) {
                suddenDeath.start();
                if (sfxEnabled && hugewaveClip != null) hugewaveClip.play();
                addToKillFeed("SUDDEN DEATH! A MURDER OF CROWS DESCENDS!");
                shakeIntensity = 40;
                hitstopFrames = 30;
            }
            if (!(competitionModeEnabled && !storyModeActive && !adventureModeActive && !classicModeActive) && !trainingModeActive) {
                shakeIntensity = suddenDeath.updateAndSpawn(
                        crowMinions,
                        random,
                        WORLD_WIDTH,
                        WORLD_HEIGHT,
                        shakeIntensity,
                        matchEnded
                );
            }

            if (!(competitionModeEnabled && !storyModeActive && !adventureModeActive && !classicModeActive)
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
        }
        
        if (particleEffectsEnabled) {
            for (Iterator<Particle> it = particles.iterator(); it.hasNext(); ) {
                Particle p = it.next();
                p.x += p.vx;
                p.y += p.vy;
                p.vy += 0.4;
                p.life--;
                if (p.life <= 0 || p.y > WORLD_HEIGHT + 200) it.remove();
            }
        } else if (!particles.isEmpty()) {
            particles.clear();
        }

        if (!lanClientSim) {
            if (trainingModeActive) {
                Bird dummy = players[trainingDummyIndex];
                if (dummy != null) {
                    dummy.health = dummy.getMaxHealth();
                }
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
                    (teamModeEnabled && !storyModeActive && !adventureModeActive && !classicModeActive)
                            || (storyModeActive && storyTeamMode)
                            || (adventureModeActive && adventureTeamMode)
                            || (classicModeActive && classicTeamMode);
            boolean isMatchOver = teamModeMatch ? aliveTeams.size() <= 1 : alive <= 1;
            if (isMatchOver && !matchEnded) {
                triggerMatchEnd(winner);
            }
        }
    }
    private void drawGame(GraphicsContext g) {
        g.clearRect(0, 0, WIDTH, HEIGHT);
        g.save();
        g.scale(zoom, zoom);
        g.translate(-camX, -camY);

        if (screenShakeEnabled && shakeIntensity > 0) {
            double shakeX = (Math.random() - 0.5) * shakeIntensity * 2;
            double shakeY = (Math.random() - 0.5) * shakeIntensity * 2;
            g.translate(shakeX, shakeY);
            shakeIntensity *= 0.9;
            if (shakeIntensity < 0.5) shakeIntensity = 0;
        } else if (!screenShakeEnabled && shakeIntensity > 0) {
            shakeIntensity = 0;
        }

        boolean ambientFx = ambientEffectsEnabled;

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
                if (ambientFx) {
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
                    if (ambientFx) {
                        double pulse = 0.5 + 0.5 * Math.sin(System.currentTimeMillis() / 300.0 + v.x);
                        g.setFill(Color.CYAN.deriveColor(0, 1, 1 + pulse, 0.5));
                        g.fillOval(v.x + v.w/2 - 100, v.y - 150, 200, 300);
                    } else {
                        g.setFill(Color.CYAN.deriveColor(0, 1, 1, 0.22));
                        g.fillOval(v.x + v.w/2 - 100, v.y - 150, 200, 300);
                    }
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
                    if (ambientFx) {
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

                if (ambientFx) {
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
                    if (ambientFx) {
                        double pulse = 0.5 + 0.5 * Math.sin(System.currentTimeMillis() / 260.0 + v.x * 0.02);
                        g.setFill(Color.CYAN.deriveColor(0, 1, 1, 0.22 + 0.18 * pulse));
                        g.fillOval(v.x + v.w / 2 - 120, v.y - 210, 240, 420);
                    } else {
                        g.setFill(Color.CYAN.deriveColor(0, 1, 1, 0.18));
                        g.fillOval(v.x + v.w / 2 - 120, v.y - 210, 240, 420);
                    }
                }

                // Low cave fog
                if (ambientFx) {
                    g.setFill(Color.rgb(120, 100, 180, 0.12));
                    g.fillRect(0, GROUND_Y - 140, WORLD_WIDTH, 220);
                }
            }
            case CITY -> {
                g.setFill(Color.MIDNIGHTBLUE.darker());
                g.fillRect(0, 0, WORLD_WIDTH, WORLD_HEIGHT);
                if (ambientFx) {
                    g.setFill(Color.WHITE);
                    for (double[] star : cityStars) {
                        g.fillOval(star[0] - 1, star[1] - 1, 2, 2);
                    }
                }
                g.setFill(Color.rgb(15, 15, 30));
                double[] farX = {100, 900, 1700, 2700, 3700, 4700, 5500};
                for (double fx : farX) {
                    g.fillRect(fx - 200, GROUND_Y - 1400, 400, 1400);
                    g.fillRect(fx - 150, GROUND_Y - 1600, 300, 200);
                }
                double pulse = ambientFx ? (0.5 + 0.5 * Math.sin(System.currentTimeMillis() / 300.0)) : 0.0;
                double windowAlpha = ambientFx ? (0.3 + 0.4 * pulse) : 0.25;
                g.setFill(Color.rgb(255, 100, 200, windowAlpha));
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
                if (ambientFx) {
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
                } else {
                    g.setEffect(null);
                    g.setFill(Color.rgb(180, 180, 200, 0.5));
                    for (Platform p : platforms) {
                        if (p.signText != null) {
                            double textWidth = p.signText.length() * 18;
                            g.fillText(p.signText, p.x + p.w / 2 - textWidth / 2, p.y - 20);
                        }
                    }
                }
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

        if (particleEffectsEnabled) {
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
                particles.subList(0, particles.size() - 2500).clear();  // <- changed from 2000 to keep more but cap at ~500 active
            }
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
        stage.setFullScreen(fullscreenEnabled);
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
        resetTournamentRun();
        competitionSeriesActive = false;
        Arrays.fill(competitionRoundWins, 0);
        Arrays.fill(competitionTeamWins, 0);
        competitionRoundNumber = 1;
        trainingModeActive = false;
        stageSelectHandler = null;
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
        } else if (BEACON_PIGEON_SKIN.equals(skinKey)) {
            base = Color.web("#FFE082");
            accent = Color.web("#1E88E5");
        } else if ("SKY_KING_EAGLE".equals(skinKey)) {
            base = Color.GOLD;
            accent = Color.ORANGE;
        } else if (NOVA_PHOENIX_SKIN.equals(skinKey)) {
            base = Color.web("#1A237E");
            accent = Color.web("#00E5FF");
        } else if (DUNE_FALCON_SKIN.equals(skinKey)) {
            base = Color.web("#D7B98E");
            accent = Color.web("#5D4037");
        } else if (MINT_PENGUIN_SKIN.equals(skinKey)) {
            base = Color.web("#7FD6D8");
            accent = Color.web("#006064");
        } else if (CIRCUIT_TITMOUSE_SKIN.equals(skinKey)) {
            base = Color.web("#455A64");
            accent = Color.web("#00E5FF");
        } else if (PRISM_RAZORBILL_SKIN.equals(skinKey)) {
            base = Color.web("#1A237E");
            accent = Color.web("#FFD740");
        } else if (AURORA_PELICAN_SKIN.equals(skinKey)) {
            base = Color.web("#B2DFDB");
            accent = Color.web("#7B1FA2");
        } else if (SUNFLARE_HUMMINGBIRD_SKIN.equals(skinKey)) {
            base = Color.web("#FFB74D");
            accent = Color.web("#F57F17");
        } else if (GLACIER_SHOEBILL_SKIN.equals(skinKey)) {
            base = Color.web("#90CAF9");
            accent = Color.web("#01579B");
        } else if (TIDE_VULTURE_SKIN.equals(skinKey)) {
            base = Color.web("#26A69A");
            accent = Color.web("#004D40");
        } else if (ECLIPSE_MOCKINGBIRD_SKIN.equals(skinKey)) {
            base = Color.web("#311B92");
            accent = Color.web("#E040FB");
        } else if (UMBRA_BAT_SKIN.equals(skinKey)) {
            base = Color.web("#120B1A");
            accent = Color.web("#00E5FF");
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
        } else if (BEACON_PIGEON_SKIN.equals(skinKey) || NOVA_PHOENIX_SKIN.equals(skinKey) || AURORA_PELICAN_SKIN.equals(skinKey) || UMBRA_BAT_SKIN.equals(skinKey)) {
            g.setStroke(accent.deriveColor(0, 1, 1, 0.8));
            g.setLineWidth(2.5);
            g.strokeOval(x - size * 0.12, y - size * 0.12, size * 1.24, size * 1.24);
        } else if (ECLIPSE_MOCKINGBIRD_SKIN.equals(skinKey)) {
            g.setStroke(accent.deriveColor(0, 1, 1, 0.7));
            g.setLineWidth(2.4);
            g.strokeOval(x - size * 0.1, y - size * 0.1, size * 1.2, size * 1.2);
        } else if (PRISM_RAZORBILL_SKIN.equals(skinKey)) {
            g.setStroke(accent.deriveColor(0, 1, 1, 0.75));
            g.setLineWidth(2.2);
            g.strokeOval(x - size * 0.1, y - size * 0.1, size * 1.2, size * 1.2);
        }
    }

    private void drawRosterSprite(Canvas canvas, BirdType type, String skinKey, boolean randomPick) {
        drawRosterSprite(canvas, type, skinKey, randomPick, false);
    }

    private void drawRosterSprite(Canvas canvas, BirdType type, String skinKey, boolean randomPick, boolean forceSkin) {
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
        preview.suppressSelectEffects = type != BirdType.TITMOUSE;
        if (forceSkin) {
            applyPreviewSkinChoiceToBird(preview, type, skinKey);
        } else {
            applySkinChoiceToBird(preview, type, skinKey);
        }
        double baseSize = Math.min(w, h);
        double pad = Math.min(8, baseSize * 0.08);
        double extentFactor = (type == BirdType.BAT) ? 2.9 : 1.35;
        preview.sizeMultiplier = Math.max(0.28, Math.min(0.72, (baseSize - pad * 2) / (80.0 * extentFactor)));
        double drawSize = 80 * preview.sizeMultiplier;
        preview.x = (w - drawSize) / 2.0;
        preview.y = (h - drawSize) / 2.0 + pad;
        preview.facingRight = true;
        preview.draw(g);
        // Titmouse crest is rendered in Bird.draw now for consistent previews.
    }

    private void drawPackSilhouette(Canvas canvas, Color tint) {
        GraphicsContext g = canvas.getGraphicsContext2D();
        double w = canvas.getWidth();
        double h = canvas.getHeight();
        g.clearRect(0, 0, w, h);

        g.setFill(Color.web("#0F171F", 0.7));
        g.fillRoundRect(w * 0.08, h * 0.08, w * 0.84, h * 0.84, 18, 18);

        Color base = tint == null ? Color.web("#90A4AE") : tint;
        Color fill = base.deriveColor(0, 0.85, 0.9, 0.9);
        Color shadow = base.deriveColor(0, 0.9, 0.6, 0.6);

        double bodyW = w * 0.48;
        double bodyH = h * 0.38;
        double bodyX = (w - bodyW) / 2.0 - w * 0.05;
        double bodyY = h * 0.52 - bodyH / 2.0;

        g.setFill(shadow);
        g.fillOval(bodyX + bodyW * 0.1, bodyY + bodyH * 0.1, bodyW, bodyH);

        g.setFill(fill);
        g.fillOval(bodyX, bodyY, bodyW, bodyH);

        double head = bodyW * 0.35;
        double headX = bodyX + bodyW * 0.62;
        double headY = bodyY - head * 0.2;
        g.fillOval(headX, headY, head, head);

        double wingW = bodyW * 0.75;
        double wingH = bodyH * 0.5;
        g.fillOval(bodyX - wingW * 0.15, bodyY + bodyH * 0.15, wingW, wingH);

        g.setFill(fill.brighter());
        double beakX = headX + head * 0.75;
        double beakY = headY + head * 0.45;
        g.fillPolygon(
                new double[]{beakX, beakX + head * 0.35, beakX},
                new double[]{beakY, beakY + head * 0.18, beakY + head * 0.36},
                3
        );
    }

    private void drawShopPreview(Canvas canvas, ShopPreview preview, Color tint) {
        if (preview == null || preview.type == null) {
            drawPackSilhouette(canvas, tint);
        } else {
            drawRosterSprite(canvas, preview.type, preview.skinKey, false, true);
        }
    }

    private boolean isCharacterKey(String key) {
        if (key == null) return false;
        return CHAR_BAT_KEY.equals(key)
                || CHAR_FALCON_KEY.equals(key)
                || CHAR_HEISENBIRD_KEY.equals(key)
                || CHAR_PHOENIX_KEY.equals(key)
                || CHAR_TITMOUSE_KEY.equals(key)
                || CHAR_RAVEN_KEY.equals(key);
    }

    private boolean isShopPreviewCharacter(ShopPreview preview) {
        return preview != null && isCharacterKey(preview.skinKey);
    }

    private boolean isShopPreviewContinue(ShopPreview preview) {
        if (preview == null) return false;
        if (CLASSIC_CONTINUE_KEY.equals(preview.skinKey)) return true;
        return preview.label != null && preview.label.toUpperCase(Locale.ROOT).contains("CONTINUE");
    }

    private boolean isShopPreviewMap(ShopPreview preview) {
        if (preview == null) return false;
        String key = preview.skinKey;
        return MAP_CAVE_KEY.equals(key) || MAP_BATTLEFIELD_KEY.equals(key);
    }

    private boolean isShopPreviewCoin(ShopPreview preview) {
        if (preview == null || preview.label == null) return false;
        return preview.label.toUpperCase(Locale.ROOT).contains("BIRD COIN");
    }

    private String shopPreviewCategory(ShopPreview preview) {
        if (preview == null) return "REWARD";
        if (isShopPreviewCoin(preview)) return "CURRENCY";
        if (isShopPreviewContinue(preview)) return "CLASSIC CONTINUE";
        if (isShopPreviewMap(preview)) return "MAP";
        if (isShopPreviewCharacter(preview)) return "CHARACTER";
        if (preview.type != null && preview.skinKey != null) return "SKIN";
        if (preview.type != null) return "BIRD";
        return "REWARD";
    }

    private MapType mapTypeForPreview(ShopPreview preview) {
        if (preview == null) return null;
        if (MAP_CAVE_KEY.equals(preview.skinKey)) return MapType.CAVE;
        if (MAP_BATTLEFIELD_KEY.equals(preview.skinKey)) return MapType.BATTLEFIELD;
        return null;
    }

    private Node buildShopPreviewArt(ShopPreview preview, Color accent, double width) {
        double cardWidth = Math.max(240, width);
        double artW = Math.max(220, Math.min(360, cardWidth - 40));
        double artH = Math.max(140, artW * 0.56);

        if (preview != null && preview.type != null) {
            BirdType type = preview.type;
            String skinKey = isShopPreviewCharacter(preview) ? null : preview.skinKey;
            Canvas bg = new Canvas(artW, artH);
            drawMapBackdrop(bg, originMapForBird(type));
            double spriteSize = Math.min(artW, artH) * 0.78;
            Canvas sprite = new Canvas(spriteSize, spriteSize);
            drawRosterSprite(sprite, type, skinKey, false, true);
            StackPane art = new StackPane(bg, sprite);
            art.setAlignment(Pos.CENTER);
            art.setPrefSize(artW, artH);
            art.setMaxSize(artW, artH);
            return art;
        }

        MapType map = mapTypeForPreview(preview);
        if (map != null) {
            Canvas mapPreview = new Canvas(artW, artH);
            drawMapPreview(mapPreview, map);
            return mapPreview;
        }

        if (isShopPreviewContinue(preview)) {
            Canvas icon = new Canvas(artW, artH);
            drawContinueIcon(icon);
            return icon;
        }

        if (isShopPreviewCoin(preview)) {
            Canvas icon = new Canvas(artW, artH);
            drawCoinIcon(icon);
            return icon;
        }

        Canvas fallback = new Canvas(artW, artH);
        drawPackSilhouette(fallback, accent);
        return fallback;
    }

    private VBox buildShopPreviewCard(ShopPreview preview, Color accent, double width) {
        double cardWidth = Math.max(240, width);
        String border = toHex(accent == null ? Color.web("#607D8B") : accent);
        VBox card = createBookCard(cardWidth, border);
        card.setAlignment(Pos.TOP_CENTER);

        String titleText = shopPreviewName(preview);
        if (titleText == null || titleText.isBlank()) titleText = "REWARD";
        int titleSize = cardWidth >= 360 ? 26 : 24;
        int bodySize = cardWidth >= 360 ? 15 : 14;
        Label title = bookTitle(titleText, titleSize);
        title.setMaxWidth(cardWidth - 26);
        Label category = bookBody(shopPreviewCategory(preview), bodySize);
        category.setMaxWidth(cardWidth - 26);

        Node art = buildShopPreviewArt(preview, accent, cardWidth);
        card.getChildren().addAll(title, category, art);
        return card;
    }

    private Node buildShopPreviewCarousel(List<ShopPreview> previews, Color accent, double width) {
        List<ShopPreview> safePreviews = new ArrayList<>();
        if (previews != null) {
            for (ShopPreview preview : previews) {
                if (preview == null) continue;
                if (isShopPreviewOwned(preview)) continue;
                safePreviews.add(preview);
            }
        }
        if (safePreviews.isEmpty()) {
            safePreviews.add(new ShopPreview(null, null, "ALL REWARDS OWNED"));
        }

        double cardWidth = Math.max(240, width);
        int total = safePreviews.size();
        final int[] index = new int[]{0};

        VBox card = createBookCard(cardWidth, toHex(accent == null ? Color.web("#607D8B") : accent));
        card.setAlignment(Pos.TOP_CENTER);

        int titleSize = cardWidth >= 360 ? 26 : 24;
        int bodySize = cardWidth >= 360 ? 15 : 14;
        Label title = bookTitle("", titleSize);
        title.setMaxWidth(cardWidth - 26);
        Label category = bookBody("", bodySize);
        category.setMaxWidth(cardWidth - 26);

        StackPane artHolder = new StackPane();
        artHolder.setAlignment(Pos.CENTER);
        double holderHeight = Math.max(150, cardWidth * 0.55);
        artHolder.setMinHeight(holderHeight);
        artHolder.setPrefHeight(holderHeight);

        Label counter = bookBody("", 12);
        counter.setTextFill(Color.web("#90A4AE"));
        counter.setVisible(total > 1);
        counter.setManaged(total > 1);

        card.getChildren().addAll(title, category, artHolder, counter);

        Runnable update = () -> {
            ShopPreview preview = safePreviews.get(index[0]);
            String name = shopPreviewName(preview);
            title.setText(name == null || name.isBlank() ? "REWARD" : name);
            category.setText(shopPreviewCategory(preview));
            artHolder.getChildren().setAll(buildShopPreviewArt(preview, accent, cardWidth));
            if (total > 1) {
                counter.setText((index[0] + 1) + " / " + total);
            }
        };
        update.run();

        if (total > 1) {
            Timeline timeline = new Timeline(new KeyFrame(Duration.seconds(2.6), e -> {
                index[0] = (index[0] + 1) % total;
                update.run();
            }));
            timeline.setCycleCount(Animation.INDEFINITE);
            timeline.play();
            card.sceneProperty().addListener((obs, oldScene, newScene) -> {
                if (newScene == null) {
                    timeline.stop();
                } else if (timeline.getStatus() != Animation.Status.RUNNING) {
                    timeline.play();
                }
            });
        }

        return card;
    }

    private Button buildHubNavButton(String text, double fontSize, String primary, String secondary, String accent, Node icon, Runnable action) {
        Button button = uiFactory.action(text, 520, 130, fontSize, primary, 30, action);
        styleHubButton(button, primary, secondary, accent, icon, ContentDisplay.LEFT, true, 28);
        return button;
    }

    private Button buildHubFooterButton(String text, double width, double fontSize, String primary, String secondary, String accent, Node icon, Runnable action) {
        Button button = uiFactory.action(text, width, 110, fontSize, primary, 20, action);
        styleHubButton(button, primary, secondary, accent, icon, ContentDisplay.TOP, false, 20);
        return button;
    }

    private void styleHubButton(Button button, String primary, String secondary, String accent, Node icon,
                                ContentDisplay display, boolean leftAlign, double radius) {
        String style = "-fx-background-color: linear-gradient(to bottom right, " + primary + ", " + secondary + ");"
                + "-fx-background-radius: " + radius + ";"
                + "-fx-border-color: " + accent + "; -fx-border-width: 3; -fx-border-radius: " + radius + ";"
                + "-fx-text-fill: white; -fx-font-weight: bold;";
        button.setStyle(style);
        if (icon != null) {
            button.setGraphic(icon);
            button.setContentDisplay(display);
        }
        if (leftAlign) {
            button.setAlignment(Pos.CENTER_LEFT);
            button.setPadding(new Insets(6, 24, 6, 24));
            button.setGraphicTextGap(18);
        } else {
            button.setAlignment(Pos.CENTER);
            button.setPadding(new Insets(8, 10, 8, 10));
            button.setGraphicTextGap(10);
        }
        button.setEffect(new DropShadow(24, Color.rgb(0, 0, 0, 0.55)));
    }

    private Pane hubIconPane(double size) {
        Pane pane = new Pane();
        pane.setPrefSize(size, size);
        pane.setMinSize(size, size);
        pane.setMaxSize(size, size);
        return pane;
    }

    private Node hubIconFight() {
        Pane pane = hubIconPane(56);
        Color blade = Color.web("#FFE0B2");
        Line left = new Line(12, 44, 44, 12);
        left.setStroke(blade);
        left.setStrokeWidth(5);
        left.setStrokeLineCap(StrokeLineCap.ROUND);
        Line right = new Line(12, 12, 44, 44);
        right.setStroke(blade);
        right.setStrokeWidth(5);
        right.setStrokeLineCap(StrokeLineCap.ROUND);
        Circle center = new Circle(28, 28, 4, Color.web("#FFF3E0"));
        pane.getChildren().addAll(left, right, center);
        return pane;
    }

    private Node hubIconAdventure() {
        Pane pane = hubIconPane(56);
        Circle ring = new Circle(28, 28, 20);
        ring.setFill(Color.TRANSPARENT);
        ring.setStroke(Color.web("#E0F7FA"));
        ring.setStrokeWidth(4);
        Polygon needle = new Polygon(28, 10, 34, 30, 28, 26, 22, 30);
        needle.setFill(Color.web("#FFE082"));
        Circle center = new Circle(28, 28, 3, Color.web("#FFE082"));
        pane.getChildren().addAll(ring, needle, center);
        return pane;
    }

    private Node hubIconTraining() {
        Pane pane = hubIconPane(56);
        Circle outer = new Circle(28, 28, 20);
        outer.setFill(Color.TRANSPARENT);
        outer.setStroke(Color.web("#E0F7FA"));
        outer.setStrokeWidth(4);
        Circle mid = new Circle(28, 28, 12);
        mid.setFill(Color.TRANSPARENT);
        mid.setStroke(Color.web("#FFE082"));
        mid.setStrokeWidth(3);
        Circle dot = new Circle(28, 28, 4, Color.web("#FFE082"));
        pane.getChildren().addAll(outer, mid, dot);
        return pane;
    }

    private Node hubIconClassic() {
        Pane pane = hubIconPane(56);
        Polygon cup = new Polygon(16, 14, 40, 14, 36, 30, 20, 30);
        cup.setFill(Color.web("#FFE082"));
        Line handleLeft = new Line(16, 16, 10, 22);
        handleLeft.setStroke(Color.web("#FFD54F"));
        handleLeft.setStrokeWidth(3);
        handleLeft.setStrokeLineCap(StrokeLineCap.ROUND);
        Line handleRight = new Line(40, 16, 46, 22);
        handleRight.setStroke(Color.web("#FFD54F"));
        handleRight.setStrokeWidth(3);
        handleRight.setStrokeLineCap(StrokeLineCap.ROUND);
        Rectangle stem = new Rectangle(26, 30, 4, 8);
        stem.setFill(Color.web("#FFECB3"));
        Rectangle base = new Rectangle(20, 38, 16, 6);
        base.setArcWidth(4);
        base.setArcHeight(4);
        base.setFill(Color.web("#FFD54F"));
        pane.getChildren().addAll(cup, handleLeft, handleRight, stem, base);
        return pane;
    }

    private Node hubIconShop() {
        Pane pane = hubIconPane(56);
        Polygon roof = new Polygon(6, 22, 28, 8, 50, 22);
        roof.setFill(Color.web("#FFCC80"));
        Rectangle base = new Rectangle(8, 22, 40, 26);
        base.setArcWidth(6);
        base.setArcHeight(6);
        base.setFill(Color.web("#FFE0B2"));
        Line awning = new Line(8, 27, 48, 27);
        awning.setStroke(Color.web("#F57F17"));
        awning.setStrokeWidth(3);
        Rectangle door = new Rectangle(26, 32, 10, 16);
        door.setArcWidth(4);
        door.setArcHeight(4);
        door.setFill(Color.web("#6D4C41"));
        Rectangle window = new Rectangle(14, 32, 8, 8);
        window.setFill(Color.web("#90CAF9"));
        pane.getChildren().addAll(roof, base, awning, door, window);
        return pane;
    }

    private Node hubIconLan() {
        Pane pane = hubIconPane(56);
        Color nodeColor = Color.web("#E0F2F1");
        Line linkTop = new Line(20, 18, 36, 18);
        linkTop.setStroke(nodeColor);
        linkTop.setStrokeWidth(3);
        Line linkLeft = new Line(17, 23, 25, 35);
        linkLeft.setStroke(nodeColor);
        linkLeft.setStrokeWidth(3);
        Line linkRight = new Line(39, 23, 31, 35);
        linkRight.setStroke(nodeColor);
        linkRight.setStrokeWidth(3);
        Circle left = new Circle(14, 18, 6, nodeColor);
        Circle right = new Circle(42, 18, 6, nodeColor);
        Circle bottom = new Circle(28, 40, 6, nodeColor);
        pane.getChildren().addAll(linkTop, linkLeft, linkRight, left, right, bottom);
        return pane;
    }

    private Node hubIconAchievements() {
        Pane pane = hubIconPane(56);
        Rectangle ribbonLeft = new Rectangle(20, 40, 6, 10);
        ribbonLeft.setFill(Color.web("#90CAF9"));
        Rectangle ribbonRight = new Rectangle(30, 40, 6, 10);
        ribbonRight.setFill(Color.web("#90CAF9"));
        Circle medal = new Circle(28, 26, 12);
        medal.setFill(Color.web("#FFD54F"));
        medal.setStroke(Color.web("#FFF8E1"));
        medal.setStrokeWidth(3);
        Polygon star = new Polygon(
                28, 18,
                30, 24,
                36, 24,
                31, 28,
                33, 34,
                28, 30,
                23, 34,
                25, 28,
                20, 24,
                26, 24
        );
        star.setFill(Color.web("#FFF3E0"));
        pane.getChildren().addAll(ribbonLeft, ribbonRight, medal, star);
        return pane;
    }

    private Node hubIconFeatherpedia() {
        Pane pane = hubIconPane(56);
        Rectangle cover = new Rectangle(10, 12, 36, 32);
        cover.setArcWidth(4);
        cover.setArcHeight(4);
        cover.setFill(Color.web("#D1C4E9"));
        Rectangle page = new Rectangle(12, 14, 14, 28);
        page.setArcWidth(3);
        page.setArcHeight(3);
        page.setFill(Color.web("#F3E5F5"));
        Line spine = new Line(28, 12, 28, 44);
        spine.setStroke(Color.web("#B39DDB"));
        spine.setStrokeWidth(3);
        pane.getChildren().addAll(cover, page, spine);
        return pane;
    }

    private Node hubIconSettings() {
        Pane pane = hubIconPane(56);
        Circle ring = new Circle(28, 28, 14);
        ring.setFill(Color.TRANSPARENT);
        ring.setStroke(Color.web("#E0F7FA"));
        ring.setStrokeWidth(4);
        for (int i = 0; i < 6; i++) {
            Rectangle tooth = new Rectangle(26, 8, 4, 8);
            tooth.setArcWidth(2);
            tooth.setArcHeight(2);
            tooth.setFill(Color.web("#E0F7FA"));
            tooth.getTransforms().add(new Rotate(i * 60.0, 28, 28));
            pane.getChildren().add(tooth);
        }
        Circle core = new Circle(28, 28, 4, Color.web("#FFE082"));
        pane.getChildren().addAll(ring, core);
        return pane;
    }

    private Node hubIconExit() {
        Pane pane = hubIconPane(56);
        Rectangle frame = new Rectangle(16, 12, 24, 32);
        frame.setArcWidth(4);
        frame.setArcHeight(4);
        frame.setFill(Color.web("#FFCDD2"));
        Rectangle door = new Rectangle(20, 16, 16, 24);
        door.setArcWidth(3);
        door.setArcHeight(3);
        door.setFill(Color.web("#EF9A9A"));
        Circle knob = new Circle(32, 28, 2, Color.web("#FFF8E1"));
        pane.getChildren().addAll(frame, door, knob);
        return pane;
    }

    private void showHub(Stage stage) {
        playMenuMusic();
        BorderPane root = new BorderPane();
        root.setPadding(new Insets(28, 40, 28, 40));
        root.setStyle("-fx-background-color: linear-gradient(to bottom, #0B1D2B, #1B2E3C, #1F3D4C);");

        Label title = new Label("BIRD FIGHT 3");
        title.setFont(Font.font("Impact", FontWeight.BOLD, 96));
        title.setTextFill(Color.web("#FFE082"));
        title.setEffect(new DropShadow(50, Color.rgb(0, 0, 0, 0.85)));

        Label hubLabel = new Label("CENTRAL HUB");
        hubLabel.setFont(Font.font("Consolas", 28));
        hubLabel.setTextFill(Color.web("#80DEEA"));
        hubLabel.setEffect(new DropShadow(12, Color.rgb(0, 0, 0, 0.6)));

        Region titleFlare = new Region();
        titleFlare.setPrefSize(480, 4);
        titleFlare.setStyle("-fx-background-color: linear-gradient(to right, transparent, rgba(255, 224, 130, 0.95), transparent);"
                + "-fx-background-radius: 2;");

        VBox titleBox = new VBox(6, title, hubLabel, titleFlare);
        titleBox.setAlignment(Pos.CENTER);

        Label coins = new Label("BIRD COINS: " + birdCoins);
        coins.setFont(Font.font("Consolas", 24));
        coins.setTextFill(Color.web("#FFD54F"));

        Pane logoArt = new Pane();
        logoArt.setPrefSize(900, 200);
        logoArt.setMinSize(900, 200);
        logoArt.setMaxSize(900, 200);
        logoArt.setMouseTransparent(true);

        Circle glowOuter = new Circle(450, 100, 150);
        glowOuter.setFill(Color.web("#64B5F6", 0.12));
        Circle glowInner = new Circle(450, 100, 105);
        glowInner.setFill(Color.web("#FFD54F", 0.22));

        Line wingLeftA = new Line(150, 130, 360, 70);
        wingLeftA.setStroke(Color.web("#FFD54F", 0.75));
        wingLeftA.setStrokeWidth(4);
        wingLeftA.setStrokeLineCap(StrokeLineCap.ROUND);
        Line wingLeftB = new Line(170, 150, 360, 100);
        wingLeftB.setStroke(Color.web("#80DEEA", 0.55));
        wingLeftB.setStrokeWidth(3);
        wingLeftB.setStrokeLineCap(StrokeLineCap.ROUND);
        Line wingLeftC = new Line(200, 168, 360, 130);
        wingLeftC.setStroke(Color.web("#FFE082", 0.45));
        wingLeftC.setStrokeWidth(2.5);
        wingLeftC.setStrokeLineCap(StrokeLineCap.ROUND);

        Line wingRightA = new Line(750, 130, 540, 70);
        wingRightA.setStroke(Color.web("#FFD54F", 0.75));
        wingRightA.setStrokeWidth(4);
        wingRightA.setStrokeLineCap(StrokeLineCap.ROUND);
        Line wingRightB = new Line(730, 150, 540, 100);
        wingRightB.setStroke(Color.web("#80DEEA", 0.55));
        wingRightB.setStrokeWidth(3);
        wingRightB.setStrokeLineCap(StrokeLineCap.ROUND);
        Line wingRightC = new Line(700, 168, 540, 130);
        wingRightC.setStroke(Color.web("#FFE082", 0.45));
        wingRightC.setStrokeWidth(2.5);
        wingRightC.setStrokeLineCap(StrokeLineCap.ROUND);

        Circle sparkA = new Circle(320, 54, 3, Color.web("#FFF8E1", 0.8));
        Circle sparkB = new Circle(580, 60, 2.5, Color.web("#B3E5FC", 0.7));
        Circle sparkC = new Circle(450, 40, 2.2, Color.web("#FFF8E1", 0.6));

        logoArt.getChildren().addAll(glowOuter, glowInner,
                wingLeftA, wingLeftB, wingLeftC,
                wingRightA, wingRightB, wingRightC,
                sparkA, sparkB, sparkC);

        StackPane logoFrame = new StackPane(logoArt, titleBox);
        logoFrame.setPadding(new Insets(6, 20, 8, 20));
        logoFrame.setAlignment(Pos.CENTER);
        logoFrame.setEffect(new Glow(0.08));

        StackPane top = new StackPane(logoFrame, coins);
        StackPane.setAlignment(logoFrame, Pos.CENTER);
        StackPane.setAlignment(coins, Pos.TOP_RIGHT);
        StackPane.setMargin(coins, new Insets(6, 4, 0, 0));

        GridPane nav = new GridPane();
        nav.setHgap(26);
        nav.setVgap(26);
        nav.setAlignment(Pos.CENTER);

        Button fightBtn = buildHubNavButton("FIGHT", 48, "#FF7043", "#E64A19", "#FFCCBC", hubIconFight(),
                () -> showFightSetup(stage));
        Button adventureBtn = buildHubNavButton("ADVENTURE", 48, "#26A69A", "#00796B", "#B2DFDB", hubIconAdventure(),
                () -> showAdventureHub(stage));
        Button trainingBtn = buildHubNavButton("TRAINING", 48, "#26C6DA", "#0097A7", "#B2EBF2", hubIconTraining(),
                () -> showTrainingSetup(stage));
        Button classicBtn = buildHubNavButton("CLASSIC & MORE", 40, "#1E88E5", "#1565C0", "#BBDEFB", hubIconClassic(),
                () -> showClassicMoreMenu(stage));
        Button shopBtn = buildHubNavButton("SHOP", 48, "#FBC02D", "#F57F17", "#FFF8E1", hubIconShop(),
                () -> showShop(stage));
        Button lanBtn = buildHubNavButton("LAN PLAY", 40, "#8D6E63", "#5D4037", "#D7CCC8", hubIconLan(),
                () -> showLanMenu(stage));

        nav.add(fightBtn, 0, 0);
        nav.add(adventureBtn, 1, 0);
        nav.add(trainingBtn, 2, 0);
        nav.add(classicBtn, 0, 1);
        nav.add(shopBtn, 1, 1);
        nav.add(lanBtn, 2, 1);

        Button achievementsBtn = buildHubFooterButton("ACHIEVEMENTS", 190, 18, "#455A64", "#37474F", "#B0BEC5",
                hubIconAchievements(), () -> showAchievements(stage));
        Button bookBtn = buildHubFooterButton("FEATHERPEDIA", 190, 16, "#5E35B1", "#4527A0", "#D1C4E9",
                hubIconFeatherpedia(), () -> showBirdBook(stage));
        Button settingsBtn = buildHubFooterButton("SETTINGS", 170, 18, "#607D8B", "#455A64", "#CFD8DC",
                hubIconSettings(), () -> {
                    settingsReturn = () -> showMenu(stage);
                    showMainSettings(stage);
                });
        Button exitBtn = buildHubFooterButton("EXIT", 140, 18, "#D32F2F", "#B71C1C", "#FFCDD2",
                hubIconExit(), () -> confirmExitGame(stage));
        uiFactory.fitSingleLineOnLayout(bookBtn, 16, 12);
        HBox footer = new HBox(20, achievementsBtn, bookBtn, settingsBtn, exitBtn);
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
        selectionPane.setStyle("-fx-background-color: transparent;");

        List<BirdIconSpot> spots = new ArrayList<>();
        Map<BirdType, BirdIconSpot> spotByType = new HashMap<>();
        BirdIconSpot randomSpot = null;

        int columns = Math.min(6, Math.max(1, gridBirds.size()));
        int rows = (int) Math.ceil(gridBirds.size() / (double) columns);
        double dockW = 160;
        double dockH = 320;
        double dockX = 0;
        double dockY = paneH - dockH;
        double gridX = dockX + dockW + 20;
        double gridY = 10;
        double gridW = paneW - gridX - 20;
        double gridH = paneH - 20;
        double cellW = gridW / columns;
        double cellH = gridH / Math.max(1, rows);
        double iconSize = Math.max(60, Math.min(110, Math.min(cellW, cellH) * 0.65));

        Region gridFrame = new Region();
        gridFrame.setLayoutX(gridX - 12);
        gridFrame.setLayoutY(gridY - 10);
        gridFrame.setPrefSize(gridW + 24, gridH + 20);
        gridFrame.setStyle("-fx-background-color: rgba(0,0,0,0.35); -fx-border-color: #90A4AE; -fx-border-width: 3; -fx-background-radius: 20; -fx-border-radius: 20;");
        selectionPane.getChildren().add(gridFrame);

        for (int i = 0; i < gridBirds.size(); i++) {
            BirdType type = gridBirds.get(i);
            boolean isRandom = type == null;
            int col = i % columns;
            int row = i / columns;
            double cellX = gridX + col * cellW;
            double cellY = gridY + row * cellH;
            double centerX = cellX + cellW / 2.0;
            double centerY = cellY + cellH / 2.0;

            Canvas icon = new Canvas(iconSize, iconSize);
            drawRosterSprite(icon, type, null, isRandom);
            Node iconNode = icon;
            if (type == BirdType.FALCON && !isRandom) {
                StackPane iconStack = new StackPane();
                iconStack.setPrefSize(iconSize, iconSize);
                iconStack.getChildren().add(icon);
                Canvas echo = new Canvas(iconSize * 0.55, iconSize * 0.55);
                drawRosterSprite(echo, BirdType.EAGLE, null, false);
                echo.setOpacity(0.32);
                StackPane.setAlignment(echo, Pos.TOP_LEFT);
                StackPane.setMargin(echo, new Insets(6, 0, 0, 6));
                iconStack.getChildren().add(echo);
                iconNode = iconStack;
            } else if (type == BirdType.HEISENBIRD && !isRandom) {
                StackPane iconStack = new StackPane();
                iconStack.setPrefSize(iconSize, iconSize);
                iconStack.getChildren().add(icon);
                Canvas echo = new Canvas(iconSize * 0.55, iconSize * 0.55);
                drawRosterSprite(echo, BirdType.OPIUMBIRD, null, false);
                echo.setOpacity(0.32);
                StackPane.setAlignment(echo, Pos.TOP_LEFT);
                StackPane.setMargin(echo, new Insets(6, 0, 0, 6));
                iconStack.getChildren().add(echo);
                iconNode = iconStack;
            }

            Label name = new Label(isRandom ? "RANDOM" : type.name.toUpperCase());
            name.setFont(Font.font("Consolas", 16));
            name.setTextFill(Color.web("#ECEFF1"));
            name.setMaxWidth(cellW - 10);
            name.setTextAlignment(TextAlignment.CENTER);
            name.setAlignment(Pos.CENTER);
            name.setWrapText(true);
            if (type == BirdType.FALCON && !isRandom) {
                name.setText("FALCON\nECHO OF EAGLE");
                name.setFont(Font.font("Consolas", 14));
            } else if (type == BirdType.HEISENBIRD && !isRandom) {
                name.setText("HEISENBIRD\nECHO OF OPIUM");
                name.setFont(Font.font("Consolas", 14));
            }

            VBox card = new VBox(6, iconNode, name);
            card.setAlignment(Pos.CENTER);
            card.setPrefWidth(cellW);
            card.setPrefHeight(cellH);
            card.setLayoutX(cellX);
            card.setLayoutY(cellY);
            card.setMouseTransparent(true);

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
        selectorDock.setPrefSize(dockW, dockH);
        selectorDock.setLayoutX(dockX);
        selectorDock.setLayoutY(dockY);
        selectorDock.setStyle("-fx-background-color: rgba(10,10,10,0.6); -fx-border-color: #FFD54F; -fx-border-width: 2; -fx-background-radius: 18; -fx-border-radius: 18;");
        selectionPane.getChildren().add(selectorDock);

        Label dockLabel = new Label("PLAYER SELECTORS");
        dockLabel.setFont(Font.font("Consolas", 18));
        dockLabel.setTextFill(Color.web("#FFE082"));
        dockLabel.setLayoutX(dockX + 12);
        dockLabel.setLayoutY(dockY + 8);
        selectionPane.getChildren().add(dockLabel);

        Point2D[] dockPositions = new Point2D[4];
        for (int i = 0; i < 4; i++) {
            double spacing = dockH / 5.0;
            double dx = dockX + dockW / 2.0;
            double dy = dockY + spacing * (i + 1);
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
        playerSlots = fightSlots;

        Circle[] selectors = new Circle[4];
        Text[] selectorLabels = new Text[4];
        boolean[] selectorLocked = new boolean[4];
        boolean[] selectorJustUnlocked = new boolean[4];
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
            label.setMouseTransparent(true);
            selectorLabels[i] = label;
            selectionPane.getChildren().add(label);

            final double[] dragOffset = new double[2];
            selector.setOnMousePressed(e -> {
                if (selectorLocked[idx]) {
                    selectorLocked[idx] = false;
                    fightRandomSelected[idx] = false;
                    fightSelectedBirds[idx] = null;
                    fightSelectedSkinKeys[idx] = null;
                    updateSlot[idx].run();
                    updateReadyBanner.run();
                    selectorJustUnlocked[idx] = true;
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
                if (selectorJustUnlocked[idx]) {
                    selectorJustUnlocked[idx] = false;
                }
                nx = Math.max(40, Math.min(nx, selectionPane.getPrefWidth() - 40));
                ny = Math.max(40, Math.min(ny, selectionPane.getPrefHeight() - 40));
                selector.setCenterX(nx);
                selector.setCenterY(ny);
                label.setX(nx - 10);
                label.setY(ny + 6);
            });
            selector.setOnMouseReleased(e -> {
                if (selectorLocked[idx]) return;
                if (selectorJustUnlocked[idx]) {
                    selectorJustUnlocked[idx] = false;
                    Point2D dock = dockPositions[idx];
                    selector.setCenterX(dock.getX());
                    selector.setCenterY(dock.getY());
                    label.setX(dock.getX() - 10);
                    label.setY(dock.getY() + 6);
                    return;
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
        playerBar.setTranslateY(36);
        BorderPane.setMargin(playerBar, new Insets(0, 0, 0, 0));

        StackPane center = new StackPane(selectionPane, readyBtn);
        StackPane.setAlignment(selectionPane, Pos.BOTTOM_LEFT);
        StackPane.setMargin(selectionPane, new Insets(0, 0, 36, 24));
        StackPane.setAlignment(readyBtn, Pos.BOTTOM_CENTER);
        StackPane.setMargin(readyBtn, new Insets(0, 0, 16, 0));

        root.setCenter(center);
        root.setBottom(playerBar);

        Scene scene = new Scene(root, WIDTH, HEIGHT);
        bindEscape(scene, back);
        BooleanSupplier tryStartMatch = () -> {
            if (!readyBtn.isVisible()) return false;
            playButtonClick();
            readyBtn.fire();
            return true;
        };

        scene.addEventFilter(KeyEvent.KEY_PRESSED, e -> {
            boolean handled = false;
            handled |= handleSelectorKey(e, 0, selectors[0], selectorLabels[0], selectorLocked, spots, dockPositions, selectionPane, updateSlot[0], updateReadyBanner, tryStartMatch);
            handled |= handleSelectorKey(e, 1, selectors[1], selectorLabels[1], selectorLocked, spots, dockPositions, selectionPane, updateSlot[1], updateReadyBanner, tryStartMatch);
            handled |= handleSelectorKey(e, 2, selectors[2], selectorLabels[2], selectorLocked, spots, dockPositions, selectionPane, updateSlot[2], updateReadyBanner, tryStartMatch);
            handled |= handleSelectorKey(e, 3, selectors[3], selectorLabels[3], selectorLocked, spots, dockPositions, selectionPane, updateSlot[3], updateReadyBanner, tryStartMatch);
            if (handled) {
                e.consume();
            }
        });
        setupKeyboardNavigation(scene);
        applyConsoleHighlight(scene);
        setScenePreservingFullscreen(stage, scene);
        back.requestFocus();
    }

    private void showTrainingSetup(Stage stage) {
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
        classicDeaths = 0;
        classicTeamMode = false;
        Arrays.fill(classicTeams, 1);
        competitionSeriesActive = false;
        Arrays.fill(competitionRoundWins, 0);
        Arrays.fill(competitionTeamWins, 0);
        competitionRoundNumber = 1;
        trainingModeActive = false;
        stageSelectHandler = null;
        playMenuMusic();

        BorderPane root = new BorderPane();
        root.setPadding(new Insets(26, 36, 26, 36));
        root.setStyle("-fx-background-color: linear-gradient(to bottom, #071A27, #13293D);");

        Label title = new Label("TRAINING MODE");
        title.setFont(Font.font("Impact", FontWeight.BOLD, 88));
        title.setTextFill(Color.web("#FFE082"));
        title.setEffect(new DropShadow(35, Color.BLACK));

        Label subtitle = new Label("Single-player practice. Choose your bird, choose a sparring partner, then pick a stage.");
        subtitle.setFont(Font.font("Consolas", 24));
        subtitle.setTextFill(Color.web("#80DEEA"));

        VBox top = new VBox(6, title, subtitle);
        top.setAlignment(Pos.CENTER);

        List<BirdType> availableBirds = unlockedBirdPool();
        BirdType playerPick = isBirdUnlocked(trainingPlayerBird) ? trainingPlayerBird : firstUnlockedBird();
        if (playerPick == null && !availableBirds.isEmpty()) playerPick = availableBirds.get(0);
        BirdType opponentPick = isBirdUnlocked(trainingOpponentBird) ? trainingOpponentBird : firstUnlockedBird();
        if (opponentPick == null && !availableBirds.isEmpty()) opponentPick = availableBirds.get(0);
        if (playerPick != null) trainingPlayerBird = playerPick;
        if (opponentPick != null) trainingOpponentBird = opponentPick;

        final BirdType[] selected = new BirdType[]{playerPick, opponentPick};
        final boolean[] selectorLocked = new boolean[]{playerPick != null, opponentPick != null};
        final boolean[] selectorJustUnlocked = new boolean[]{false, false};

        Pane selectionPane = new Pane();
        double paneW = 1100;
        double paneH = 520;
        selectionPane.setPrefSize(paneW, paneH);
        selectionPane.setMinSize(paneW, paneH);
        selectionPane.setMaxSize(paneW, paneH);
        selectionPane.setStyle("-fx-background-color: rgba(0,0,0,0.35); -fx-border-color: #90A4AE; -fx-border-width: 3; -fx-background-radius: 20; -fx-border-radius: 20;");
        Rectangle selectionClip = new Rectangle();
        selectionClip.widthProperty().bind(selectionPane.widthProperty());
        selectionClip.heightProperty().bind(selectionPane.heightProperty());
        selectionPane.setClip(selectionClip);

        List<BirdIconSpot> spots = new ArrayList<>();
        Map<BirdType, BirdIconSpot> spotByType = new HashMap<>();

        int columns = availableBirds.size() > 12 ? 4 : 3;
        int rows = (int) Math.ceil(availableBirds.size() / (double) Math.max(1, columns));
        double dockW = 170;
        double dockH = 320;
        double dockX = 0;
        double dockY = paneH - dockH;
        double gridX = dockX + dockW + 20;
        double gridY = 10;
        double gridW = paneW - gridX - 20;
        double gridH = paneH - 20;
        double cellW = gridW / Math.max(1, columns);
        double cellH = gridH / Math.max(1, rows);
        double iconSize = Math.max(60, Math.min(110, Math.min(cellW, cellH) * 0.65));

        for (int i = 0; i < availableBirds.size(); i++) {
            BirdType type = availableBirds.get(i);
            int col = i % columns;
            int row = i / columns;
            double cellX = gridX + col * cellW;
            double cellY = gridY + row * cellH;
            double centerX = cellX + cellW / 2.0;
            double centerY = cellY + cellH / 2.0;

            Canvas icon = new Canvas(iconSize, iconSize);
            drawRosterSprite(icon, type, null, false);
            Node iconNode = icon;
            if (type == BirdType.FALCON) {
                StackPane iconStack = new StackPane();
                iconStack.setPrefSize(iconSize, iconSize);
                iconStack.getChildren().add(icon);
                Canvas echo = new Canvas(iconSize * 0.55, iconSize * 0.55);
                drawRosterSprite(echo, BirdType.EAGLE, null, false);
                echo.setOpacity(0.32);
                StackPane.setAlignment(echo, Pos.TOP_LEFT);
                StackPane.setMargin(echo, new Insets(6, 0, 0, 6));
                iconStack.getChildren().add(echo);
                iconNode = iconStack;
            } else if (type == BirdType.HEISENBIRD) {
                StackPane iconStack = new StackPane();
                iconStack.setPrefSize(iconSize, iconSize);
                iconStack.getChildren().add(icon);
                Canvas echo = new Canvas(iconSize * 0.55, iconSize * 0.55);
                drawRosterSprite(echo, BirdType.OPIUMBIRD, null, false);
                echo.setOpacity(0.32);
                StackPane.setAlignment(echo, Pos.TOP_LEFT);
                StackPane.setMargin(echo, new Insets(6, 0, 0, 6));
                iconStack.getChildren().add(echo);
                iconNode = iconStack;
            }

            Label name = new Label(type.name.toUpperCase());
            name.setFont(Font.font("Consolas", 16));
            name.setTextFill(Color.web("#ECEFF1"));
            name.setWrapText(true);
            name.setAlignment(Pos.CENTER);
            if (type == BirdType.FALCON) {
                name.setText("FALCON\nECHO OF EAGLE");
                name.setFont(Font.font("Consolas", 14));
            } else if (type == BirdType.HEISENBIRD) {
                name.setText("HEISENBIRD\nECHO OF OPIUM");
                name.setFont(Font.font("Consolas", 14));
            }

            VBox card = new VBox(6, iconNode, name);
            card.setAlignment(Pos.CENTER);
            card.setPrefWidth(cellW);
            card.setPrefHeight(cellH);
            card.setLayoutX(cellX);
            card.setLayoutY(cellY);
            card.setMouseTransparent(true);

            selectionPane.getChildren().add(card);
            BirdIconSpot spot = new BirdIconSpot(type, false, centerX, centerY);
            spots.add(spot);
            spotByType.put(type, spot);
        }

        Region selectorDock = new Region();
        selectorDock.setPrefSize(dockW, dockH);
        selectorDock.setLayoutX(dockX);
        selectorDock.setLayoutY(dockY);
        selectorDock.setStyle("-fx-background-color: rgba(10,10,10,0.6); -fx-border-color: #FFD54F; -fx-border-width: 2; -fx-background-radius: 18; -fx-border-radius: 18;");
        selectionPane.getChildren().add(selectorDock);

        Label dockLabel = new Label("SELECTORS");
        dockLabel.setFont(Font.font("Consolas", 18));
        dockLabel.setTextFill(Color.web("#FFE082"));
        dockLabel.setLayoutX(dockX + 12);
        dockLabel.setLayoutY(dockY + 8);
        selectionPane.getChildren().add(dockLabel);

        Point2D[] dockPositions = new Point2D[]{
                new Point2D(dockX + dockW / 2.0, dockY + dockH * 0.35),
                new Point2D(dockX + dockW / 2.0, dockY + dockH * 0.72)
        };

        Circle[] selectors = new Circle[2];
        Text[] selectorLabels = new Text[2];
        Color[] selectorColors = new Color[]{Color.web("#FFD54F"), Color.web("#FF6B6B")};
        String[] selectorNames = new String[]{"YOU", "OPP"};

        final Runnable[] refreshRef = new Runnable[1];

        for (int i = 0; i < 2; i++) {
            int idx = i;
            Circle selector = new Circle(26);
            selector.setFill(Color.web("rgba(255,213,79,0.15)"));
            selector.setStroke(selectorColors[i]);
            selector.setStrokeWidth(3);
            selector.setManaged(false);
            selectors[i] = selector;
            selectionPane.getChildren().add(selector);

            Text label = new Text(selectorNames[i]);
            label.setFont(Font.font("Impact", 14));
            label.setFill(selectorColors[i]);
            label.setManaged(false);
            label.setMouseTransparent(true);
            selectorLabels[i] = label;
            selectionPane.getChildren().add(label);

            final double[] dragOffset = new double[2];
            selector.setOnMousePressed(e -> {
                if (selectorLocked[idx]) {
                    selectorLocked[idx] = false;
                    selected[idx] = null;
                    if (refreshRef[0] != null) refreshRef[0].run();
                    selectorJustUnlocked[idx] = true;
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
                if (selectorJustUnlocked[idx]) {
                    selectorJustUnlocked[idx] = false;
                }
                nx = Math.max(40, Math.min(nx, selectionPane.getPrefWidth() - 40));
                ny = Math.max(40, Math.min(ny, selectionPane.getPrefHeight() - 40));
                selector.setCenterX(nx);
                selector.setCenterY(ny);
                label.setX(nx - 18);
                label.setY(ny + 6);
            });
            selector.setOnMouseReleased(e -> {
                if (selectorLocked[idx]) return;
                if (selectorJustUnlocked[idx]) {
                    selectorJustUnlocked[idx] = false;
                    Point2D dock = dockPositions[idx];
                    selector.setCenterX(dock.getX());
                    selector.setCenterY(dock.getY());
                    label.setX(dock.getX() - 18);
                    label.setY(dock.getY() + 6);
                    return;
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
                    label.setX(best.cx - 18);
                    label.setY(best.cy + 6);
                    selected[idx] = best.type;
                    selectorLocked[idx] = true;
                    if (refreshRef[0] != null) refreshRef[0].run();
                } else {
                    Point2D dock = dockPositions[idx];
                    selector.setCenterX(dock.getX());
                    selector.setCenterY(dock.getY());
                    label.setX(dock.getX() - 18);
                    label.setY(dock.getY() + 6);
                }
            });
        }

        Label youLabel = new Label();
        youLabel.setFont(Font.font("Arial Black", 36));
        youLabel.setTextFill(Color.web("#FFE082"));

        Label oppLabel = new Label();
        oppLabel.setFont(Font.font("Arial Black", 36));
        oppLabel.setTextFill(Color.web("#FFCDD2"));

        Label note = new Label("Opponent has infinite health.\nUse RESET DUMMY in-game if they fall off the map.");
        note.setFont(Font.font("Consolas", 22));
        note.setTextFill(Color.web("#B0BEC5"));

        Button back = uiFactory.action("BACK TO HUB", 360, 96, 34, "#D32F2F", 22, () -> showMenu(stage));
        Button selectMap = uiFactory.action("SELECT MAP", 420, 96, 34, "#00C853", 24, () -> {
            if (selected[0] == null || selected[1] == null) return;
            playButtonClick();
            trainingPlayerBird = selected[0];
            trainingOpponentBird = selected[1];
            stageSelectReturn = () -> showTrainingSetup(stage);
            stageSelectHandler = map -> beginTrainingMatchOnMap(stage, map);
            showStageSelect(stage);
        });

        Runnable refresh = () -> {
            youLabel.setText(selected[0] != null ? "You: " + selected[0].name : "You: SELECT");
            oppLabel.setText(selected[1] != null ? "Opponent: " + selected[1].name : "Opponent: SELECT");
            boolean ready = selected[0] != null && selected[1] != null;
            selectMap.setDisable(!ready);
            selectMap.setOpacity(ready ? 1.0 : 0.6);
        };
        refreshRef[0] = refresh;
        refresh.run();

        VBox rightCard = new VBox(14, youLabel, oppLabel, note);
        rightCard.setAlignment(Pos.TOP_LEFT);
        rightCard.setPadding(new Insets(24));
        rightCard.setMaxWidth(680);
        rightCard.setStyle("-fx-background-color: rgba(0,0,0,0.56); -fx-border-color: #64B5F6; -fx-border-width: 3; -fx-border-radius: 22; -fx-background-radius: 22;");

        HBox center = new HBox(22, selectionPane, rightCard);
        center.setAlignment(Pos.CENTER);

        HBox bottom = new HBox(18, back, selectMap);
        bottom.setAlignment(Pos.CENTER);

        root.setTop(top);
        root.setCenter(center);
        root.setBottom(bottom);

        Scene scene = new Scene(root, WIDTH, HEIGHT);
        bindEscape(scene, back);
        setupKeyboardNavigation(scene);
        applyConsoleHighlight(scene);
        setScenePreservingFullscreen(stage, scene);

        for (int i = 0; i < 2; i++) {
            BirdType pick = selected[i];
            if (pick != null) {
                BirdIconSpot spot = spotByType.get(pick);
                if (spot != null) {
                    selectors[i].setCenterX(spot.cx);
                    selectors[i].setCenterY(spot.cy);
                    selectorLabels[i].setX(spot.cx - 18);
                    selectorLabels[i].setY(spot.cy + 6);
                    continue;
                }
            }
            Point2D dock = dockPositions[i];
            selectors[i].setCenterX(dock.getX());
            selectors[i].setCenterY(dock.getY());
            selectorLabels[i].setX(dock.getX() - 18);
            selectorLabels[i].setY(dock.getY() + 6);
        }
        refresh.run();
        if (selectMap.isDisabled()) {
            back.requestFocus();
        } else {
            selectMap.requestFocus();
        }
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
                                      Runnable updateReady,
                                      BooleanSupplier tryStartMatch) {
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
            case 0 -> KeyCode.SPACE;
            case 1 -> KeyCode.ENTER;
            case 2 -> KeyCode.Y;
            case 3 -> KeyCode.O;
            default -> KeyCode.SPACE;
        };
        KeyCode special = switch (idx) {
            case 0 -> KeyCode.SHIFT;
            case 1 -> KeyCode.SLASH;
            case 2 -> KeyCode.U;
            case 3 -> KeyCode.P;
            default -> KeyCode.SHIFT;
        };

        double step = 26;
        boolean moved = false;
        if (code == special) {
            if (tryStartMatch != null && tryStartMatch.getAsBoolean()) {
                return true;
            }
            return false;
        }
        if (code == select) {
            if (selectorLocked[idx]) {
                selectorLocked[idx] = false;
                fightRandomSelected[idx] = false;
                fightSelectedBirds[idx] = null;
                fightSelectedSkinKeys[idx] = null;
                Point2D dock = dockPositions[idx];
                selector.setCenterX(dock.getX());
                selector.setCenterY(dock.getY());
                label.setX(dock.getX() - 10);
                label.setY(dock.getY() + 6);
                updateSlot.run();
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
            if (selectorLocked[idx]) {
                selectorLocked[idx] = false;
                fightRandomSelected[idx] = false;
                fightSelectedBirds[idx] = null;
                fightSelectedSkinKeys[idx] = null;
                updateSlot.run();
                updateReady.run();
            }
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
        Button episodesBtn = uiFactory.action("EPISODES", 700, 140, 44, "#8E24AA", 30, () -> showEpisodesHub(stage));
        Button tournamentBtn = uiFactory.action("TOURNAMENT MODE", 700, 140, 40, "#FFB300", 28, () -> showTournamentSetup(stage));
        VBox options = new VBox(22, classicBtn, episodesBtn, tournamentBtn);
        options.setAlignment(Pos.CENTER);

        Button back = uiFactory.action("BACK TO HUB", 360, 90, 34, "#D32F2F", 22, () -> showMenu(stage));
        HBox bottom = new HBox(back);
        bottom.setAlignment(Pos.CENTER);

        root.setTop(title);
        BorderPane.setAlignment(title, Pos.CENTER);
        root.setCenter(options);
        root.setBottom(bottom);

        Scene scene = new Scene(root, WIDTH, HEIGHT);
        bindEscape(scene, back);
        setupKeyboardNavigation(scene);
        applyConsoleHighlight(scene);
        setScenePreservingFullscreen(stage, scene);
        classicBtn.requestFocus();
    }

    private void resetTournamentRun() {
        tournamentModeActive = false;
        tournamentRounds.clear();
        currentTournamentMatch = null;
        tournamentSlotA = null;
        tournamentSlotB = null;
        tournamentMatchResolved = false;
    }

    private void ensureTournamentEntries() {
        tournamentEntrantCount = Math.max(2, Math.min(32, tournamentEntrantCount));
        tournamentHumanCount = Math.max(0, Math.min(tournamentHumanCount, tournamentEntrantCount));
        int current = tournamentEntries.size();
        if (current < tournamentEntrantCount) {
            for (int i = current; i < tournamentEntrantCount; i++) {
                tournamentEntries.add(new TournamentEntry(i + 1));
            }
        } else if (current > tournamentEntrantCount) {
            while (tournamentEntries.size() > tournamentEntrantCount) {
                tournamentEntries.remove(tournamentEntries.size() - 1);
            }
        }
    }

    private void syncTournamentEntries() {
        ensureTournamentEntries();
        for (int i = 0; i < tournamentEntries.size(); i++) {
            TournamentEntry entry = tournamentEntries.get(i);
            entry.human = i < tournamentHumanCount;
            if (entry.selectedType != null && !isBirdUnlocked(entry.selectedType)) {
                entry.selectedType = null;
            }
        }
    }

    private String tournamentEntryLabel(TournamentEntry entry) {
        if (entry == null) return "PLAYER";
        return "PLAYER " + entry.id;
    }

    private List<MapType> tournamentMapPool() {
        List<MapType> maps = new ArrayList<>();
        for (MapType map : MapType.values()) {
            if (isMapUnlocked(map)) {
                maps.add(map);
            }
        }
        if (maps.isEmpty()) {
            maps.add(MapType.FOREST);
        }
        return maps;
    }

    private MapType pickTournamentMap() {
        if (!tournamentMapRandom) {
            List<MapType> maps = tournamentMapPool();
            if (!maps.contains(tournamentFixedMap)) {
                tournamentFixedMap = maps.get(0);
            }
            return tournamentFixedMap;
        }
        List<MapType> maps = tournamentMapPool();
        return maps.get(random.nextInt(maps.size()));
    }

    private void buildTournamentBracket() {
        tournamentRounds.clear();
        ensureTournamentEntries();
        if (tournamentEntries.isEmpty()) return;

        List<TournamentEntry> seeds = new ArrayList<>(tournamentEntries);
        int size = 1;
        while (size < seeds.size()) size *= 2;
        int matchCount = size / 2;

        List<TournamentMatch> round0 = new ArrayList<>();
        for (int i = 0; i < matchCount; i++) {
            round0.add(new TournamentMatch(0, i, null, null));
        }
        for (int i = 0; i < matchCount && i < seeds.size(); i++) {
            round0.get(i).a = seeds.get(i);
        }
        int idx = matchCount;
        int matchIndex = 0;
        while (idx < seeds.size()) {
            round0.get(matchIndex).b = seeds.get(idx);
            idx++;
            matchIndex++;
        }
        tournamentRounds.add(round0);

        int roundIndex = 1;
        int nextMatches = matchCount / 2;
        while (nextMatches > 0) {
            List<TournamentMatch> round = new ArrayList<>();
            for (int i = 0; i < nextMatches; i++) {
                round.add(new TournamentMatch(roundIndex, i, null, null));
            }
            tournamentRounds.add(round);
            roundIndex++;
            nextMatches /= 2;
        }
    }

    private TournamentMatch findNextTournamentMatch() {
        for (List<TournamentMatch> round : tournamentRounds) {
            for (TournamentMatch match : round) {
                if (match.winner == null && match.a != null && match.b != null) {
                    return match;
                }
            }
        }
        return null;
    }

    private void recordTournamentWinner(TournamentMatch match, TournamentEntry winner) {
        if (match == null || winner == null || match.winner != null) return;
        match.winner = winner;
        int nextRoundIndex = match.roundIndex + 1;
        if (nextRoundIndex < tournamentRounds.size()) {
            List<TournamentMatch> nextRound = tournamentRounds.get(nextRoundIndex);
            TournamentMatch next = nextRound.get(match.matchIndex / 2);
            if (match.matchIndex % 2 == 0) {
                next.a = winner;
            } else {
                next.b = winner;
            }
        }
    }

    private boolean isTournamentComplete() {
        if (tournamentRounds.isEmpty()) return false;
        List<TournamentMatch> last = tournamentRounds.get(tournamentRounds.size() - 1);
        return !last.isEmpty() && last.get(0).winner != null;
    }

    private void resolveTournamentByes() {
        if (tournamentRounds.isEmpty()) return;
        List<TournamentMatch> round0 = tournamentRounds.get(0);
        for (TournamentMatch match : round0) {
            if (match.winner != null) continue;
            boolean hasA = match.a != null;
            boolean hasB = match.b != null;
            if (hasA ^ hasB) {
                recordTournamentWinner(match, hasA ? match.a : match.b);
            }
        }
    }

    private String tournamentRoundLabel(int roundIndex) {
        int totalRounds = tournamentRounds.size();
        if (totalRounds == 0) return "ROUND";
        int entrants = 1 << (totalRounds - roundIndex);
        return switch (entrants) {
            case 2 -> "FINAL";
            case 4 -> "SEMIFINALS";
            case 8 -> "QUARTERFINALS";
            case 16 -> "ROUND OF 16";
            case 32 -> "ROUND OF 32";
            case 64 -> "ROUND OF 64";
            default -> "ROUND " + (roundIndex + 1);
        };
    }

    private TournamentEntry resolveTournamentWinnerEntry(Bird winner) {
        if (tournamentSlotA == null || tournamentSlotB == null) return null;
        if (winner == null) {
            return random.nextBoolean() ? tournamentSlotA : tournamentSlotB;
        }
        if (winner.playerIndex == 0) return tournamentSlotA;
        if (winner.playerIndex == 1) return tournamentSlotB;
        return random.nextBoolean() ? tournamentSlotA : tournamentSlotB;
    }

    private void applyTournamentSlotNames() {
        if (!tournamentModeActive || tournamentSlotA == null || tournamentSlotB == null) return;
        if (players[0] != null) {
            players[0].name = tournamentEntryLabel(tournamentSlotA) + ": " + players[0].type.name;
        }
        if (players[1] != null) {
            players[1].name = tournamentEntryLabel(tournamentSlotB) + ": " + players[1].type.name;
        }
    }

    private void showTournamentSetup(Stage stage) {
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
        resetTournamentRun();
        trainingModeActive = false;
        lanModeActive = false;
        competitionSeriesActive = false;
        Arrays.fill(competitionRoundWins, 0);
        Arrays.fill(competitionTeamWins, 0);
        competitionRoundNumber = 1;
        playMenuMusic();

        ensureTournamentEntries();
        syncTournamentEntries();

        BorderPane root = new BorderPane();
        root.setPadding(new Insets(26, 36, 26, 36));
        root.setStyle("-fx-background-color: linear-gradient(to bottom, #0B1A2E, #1C2F4E);");

        Label title = new Label("TOURNAMENT MODE");
        title.setFont(Font.font("Arial Black", FontWeight.BOLD, 88));
        title.setTextFill(Color.web("#FFECB3"));

        Label subtitle = new Label("Set entrants, humans, and birds. CPU vs CPU matches can be watched or skipped.");
        subtitle.setFont(Font.font("Consolas", 24));
        subtitle.setTextFill(Color.web("#B3E5FC"));
        subtitle.setWrapText(true);
        subtitle.setMaxWidth(1400);
        subtitle.setTextAlignment(TextAlignment.CENTER);

        VBox header = new VBox(6, title, subtitle);
        header.setAlignment(Pos.CENTER);

        Label entrantsLabel = new Label("ENTRANTS");
        entrantsLabel.setFont(Font.font("Consolas", 20));
        entrantsLabel.setTextFill(Color.web("#B3E5FC"));
        Label entrantsValue = new Label();
        entrantsValue.setFont(Font.font("Arial Black", 40));
        entrantsValue.setTextFill(Color.WHITE);

        Label humansLabel = new Label("HUMANS");
        humansLabel.setFont(Font.font("Consolas", 20));
        humansLabel.setTextFill(Color.web("#B3E5FC"));
        Label humansValue = new Label();
        humansValue.setFont(Font.font("Arial Black", 40));
        humansValue.setTextFill(Color.WHITE);
        Label cpuValue = new Label();
        cpuValue.setFont(Font.font("Consolas", 20));
        cpuValue.setTextFill(Color.web("#FFCCBC"));

        Runnable[] refreshCounters = new Runnable[1];
        Runnable[] refreshEntries = new Runnable[1];
        Runnable[] refreshMapControls = new Runnable[1];
        Runnable[] refreshAll = new Runnable[1];

        Button entrantsMinus = uiFactory.action("-", 90, 70, 42, "#546E7A", 14, () -> {
            tournamentEntrantCount = Math.max(2, tournamentEntrantCount - 1);
            if (tournamentHumanCount > tournamentEntrantCount) {
                tournamentHumanCount = tournamentEntrantCount;
            }
            if (refreshAll[0] != null) refreshAll[0].run();
        });
        Button entrantsPlus = uiFactory.action("+", 90, 70, 42, "#546E7A", 14, () -> {
            tournamentEntrantCount = Math.min(32, tournamentEntrantCount + 1);
            if (refreshAll[0] != null) refreshAll[0].run();
        });

        Button humansMinus = uiFactory.action("-", 90, 70, 42, "#546E7A", 14, () -> {
            tournamentHumanCount = Math.max(0, tournamentHumanCount - 1);
            if (refreshAll[0] != null) refreshAll[0].run();
        });
        Button humansPlus = uiFactory.action("+", 90, 70, 42, "#546E7A", 14, () -> {
            tournamentHumanCount = Math.min(tournamentEntrantCount, tournamentHumanCount + 1);
            if (refreshAll[0] != null) refreshAll[0].run();
        });

        HBox entrantsButtons = new HBox(8, entrantsMinus, entrantsPlus);
        entrantsButtons.setAlignment(Pos.CENTER);
        VBox entrantsBox = new VBox(6, entrantsLabel, entrantsValue, entrantsButtons);
        entrantsBox.setAlignment(Pos.CENTER);

        HBox humansButtons = new HBox(8, humansMinus, humansPlus);
        humansButtons.setAlignment(Pos.CENTER);
        VBox humansBox = new VBox(6, humansLabel, humansValue, cpuValue, humansButtons);
        humansBox.setAlignment(Pos.CENTER);

        Button mapModeBtn = uiFactory.action("MAP MODE: RANDOM", 360, 70, 24, "#6D4C41", 16, () -> {
            tournamentMapRandom = !tournamentMapRandom;
            if (refreshMapControls[0] != null) refreshMapControls[0].run();
        });
        Button mapSelectBtn = uiFactory.action("MAP: " + mapDisplayName(tournamentFixedMap), 360, 70, 24, "#00897B", 16, () -> {
            if (tournamentMapRandom) return;
            List<MapType> maps = tournamentMapPool();
            if (maps.isEmpty()) return;
            int idx = maps.indexOf(tournamentFixedMap);
            if (idx < 0) idx = 0;
            tournamentFixedMap = maps.get((idx + 1) % maps.size());
            if (refreshMapControls[0] != null) refreshMapControls[0].run();
        });

        VBox mapBox = new VBox(6, mapModeBtn, mapSelectBtn);
        mapBox.setAlignment(Pos.CENTER);

        Button allRandomBtn = uiFactory.action("ALL RANDOM", 260, 70, 24, "#7B1FA2", 16, () -> {
            for (TournamentEntry entry : tournamentEntries) {
                entry.selectedType = null;
            }
            if (refreshEntries[0] != null) refreshEntries[0].run();
        });

        HBox controls = new HBox(24, entrantsBox, humansBox, mapBox, allRandomBtn);
        controls.setAlignment(Pos.CENTER);

        VBox entryList = new VBox(12);
        entryList.setAlignment(Pos.TOP_CENTER);
        entryList.setPadding(new Insets(10, 20, 10, 20));

        refreshCounters[0] = () -> {
            entrantsValue.setText(String.valueOf(tournamentEntrantCount));
            humansValue.setText(String.valueOf(tournamentHumanCount));
            cpuValue.setText("CPU: " + (tournamentEntrantCount - tournamentHumanCount));
        };

        refreshMapControls[0] = () -> {
            List<MapType> maps = tournamentMapPool();
            if (!maps.contains(tournamentFixedMap)) {
                tournamentFixedMap = maps.get(0);
            }
            mapModeBtn.setText("MAP MODE: " + (tournamentMapRandom ? "RANDOM" : "FIXED"));
            mapSelectBtn.setText("MAP: " + mapDisplayName(tournamentFixedMap));
            mapSelectBtn.setDisable(tournamentMapRandom);
            mapSelectBtn.setOpacity(tournamentMapRandom ? 0.65 : 1.0);
        };

        refreshEntries[0] = () -> {
            syncTournamentEntries();
            entryList.getChildren().clear();
            List<BirdType> pool = unlockedBirdPool();
            List<BirdType> choices = new ArrayList<>();
            choices.add(null);
            choices.addAll(pool);

            for (TournamentEntry entry : tournamentEntries) {
                HBox row = new HBox(16);
                row.setAlignment(Pos.CENTER_LEFT);
                row.setPadding(new Insets(10, 14, 10, 14));
                row.setStyle("-fx-background-color: rgba(0,0,0,0.45); -fx-background-radius: 16; -fx-border-color: #37474F; -fx-border-width: 2; -fx-border-radius: 16;");

                Label playerLabel = new Label(tournamentEntryLabel(entry));
                playerLabel.setFont(Font.font("Arial Black", 26));
                playerLabel.setTextFill(Color.web("#FFECB3"));
                playerLabel.setPrefWidth(220);

                Label roleLabel = new Label(entry.human ? "HUMAN" : "CPU");
                roleLabel.setFont(Font.font("Consolas", 22));
                roleLabel.setTextFill(entry.human ? Color.web("#81D4FA") : Color.web("#FFAB91"));
                roleLabel.setPrefWidth(120);

                String birdName = entry.selectedType != null ? entry.selectedType.name : "RANDOM";
                Button birdBtn = uiFactory.action("BIRD: " + birdName, 520, 70, 24, "#1976D2", 16, () -> {
                    int idx = choices.indexOf(entry.selectedType);
                    if (idx < 0) idx = 0;
                    int next = (idx + 1) % choices.size();
                    entry.selectedType = choices.get(next);
                    if (refreshEntries[0] != null) refreshEntries[0].run();
                });
                birdBtn.setWrapText(false);
                uiFactory.fitSingleLineOnLayout(birdBtn, 24, 16);

                row.getChildren().addAll(playerLabel, roleLabel, birdBtn);
                entryList.getChildren().add(row);
            }
        };

        refreshAll[0] = () -> {
            if (refreshCounters[0] != null) refreshCounters[0].run();
            if (refreshMapControls[0] != null) refreshMapControls[0].run();
            if (refreshEntries[0] != null) refreshEntries[0].run();
        };

        refreshAll[0].run();

        ScrollPane scroll = new ScrollPane(entryList);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background: transparent; -fx-background-color: transparent; -fx-control-inner-background: transparent;");
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);

        Button startBtn = uiFactory.action("START TOURNAMENT", 520, 110, 38, "#00C853", 26, () -> beginTournament(stage));
        Button backBtn = uiFactory.action("BACK TO CLASSIC & MORE", 520, 110, 34, "#FF1744", 22, () -> showClassicMoreMenu(stage));
        HBox bottom = new HBox(30, startBtn, backBtn);
        bottom.setAlignment(Pos.CENTER);

        VBox bottomBox = new VBox(18, controls, bottom);
        bottomBox.setAlignment(Pos.CENTER);

        root.setTop(header);
        root.setCenter(scroll);
        root.setBottom(bottomBox);
        BorderPane.setAlignment(header, Pos.CENTER);

        Scene scene = new Scene(root, WIDTH, HEIGHT);
        bindEscape(scene, backBtn);
        setupKeyboardNavigation(scene);
        applyConsoleHighlight(scene);
        setScenePreservingFullscreen(stage, scene);
        startBtn.requestFocus();
    }

    private void beginTournament(Stage stage) {
        syncTournamentEntries();
        tournamentModeActive = true;
        tournamentMatchResolved = false;
        currentTournamentMatch = null;
        tournamentSlotA = null;
        tournamentSlotB = null;
        lanModeActive = false;
        competitionSeriesActive = false;
        Arrays.fill(competitionRoundWins, 0);
        Arrays.fill(competitionTeamWins, 0);
        competitionRoundNumber = 1;
        buildTournamentBracket();
        resolveTournamentByes();
        showTournamentBracket(stage);
    }

    private void showTournamentBracket(Stage stage) {
        playMenuMusic();
        syncTournamentEntries();
        if (tournamentRounds.isEmpty()) {
            buildTournamentBracket();
        }
        resolveTournamentByes();

        TournamentMatch nextMatch = findNextTournamentMatch();
        boolean complete = isTournamentComplete();

        BorderPane root = new BorderPane();
        root.setPadding(new Insets(26, 36, 26, 36));
        root.setStyle("-fx-background-color: linear-gradient(to bottom, #081122, #13294B);");

        Label title = new Label("TOURNAMENT BRACKET");
        title.setFont(Font.font("Arial Black", FontWeight.BOLD, 88));
        title.setTextFill(Color.GOLD);

        String mapLine = tournamentMapRandom
                ? "Map Mode: RANDOM"
                : "Map: " + mapDisplayName(tournamentFixedMap);
        Label subtitle = new Label(mapLine + "  |  Entrants: " + tournamentEntrantCount);
        subtitle.setFont(Font.font("Consolas", 24));
        subtitle.setTextFill(Color.web("#B3E5FC"));

        VBox header = new VBox(6, title, subtitle);
        header.setAlignment(Pos.CENTER);

        HBox columns = new HBox(26);
        columns.setAlignment(Pos.TOP_LEFT);
        columns.setPadding(new Insets(10, 10, 10, 10));

        for (int r = 0; r < tournamentRounds.size(); r++) {
            List<TournamentMatch> round = tournamentRounds.get(r);
            VBox column = new VBox(16);
            column.setAlignment(Pos.TOP_CENTER);
            column.setPrefWidth(280);

            Label roundLabel = new Label(tournamentRoundLabel(r));
            roundLabel.setFont(Font.font("Arial Black", 26));
            roundLabel.setTextFill(Color.web("#FFE082"));
            column.getChildren().add(roundLabel);

            for (TournamentMatch match : round) {
                VBox matchBox = new VBox(6);
                matchBox.setAlignment(Pos.CENTER_LEFT);
                matchBox.setPadding(new Insets(12));
                String border = (nextMatch != null && match == nextMatch) ? "#FFD54F" : "#37474F";
                String bg = (match.winner != null) ? "rgba(0,0,0,0.6)" : "rgba(0,0,0,0.45)";
                matchBox.setStyle("-fx-background-color: " + bg + "; -fx-background-radius: 14; -fx-border-color: " + border + "; -fx-border-width: 2; -fx-border-radius: 14;");

                String placeholder = r == 0 ? "BYE" : "TBD";
                String aName = match.a != null ? tournamentEntryLabel(match.a) : placeholder;
                String bName = match.b != null ? tournamentEntryLabel(match.b) : placeholder;

                Label aLabel = new Label(aName);
                aLabel.setFont(Font.font("Consolas", 22));
                aLabel.setTextFill(Color.web("#ECEFF1"));

                Label vs = new Label("vs");
                vs.setFont(Font.font("Consolas", 18));
                vs.setTextFill(Color.web("#B0BEC5"));

                Label bLabel = new Label(bName);
                bLabel.setFont(Font.font("Consolas", 22));
                bLabel.setTextFill(Color.web("#ECEFF1"));

                matchBox.getChildren().addAll(aLabel, vs, bLabel);

                if (match.winner != null) {
                    Label adv = new Label("Advances: " + tournamentEntryLabel(match.winner));
                    adv.setFont(Font.font("Consolas", 18));
                    adv.setTextFill(Color.web("#C5E1A5"));
                    matchBox.getChildren().add(adv);
                }

                column.getChildren().add(matchBox);
            }

            columns.getChildren().add(column);
        }

        ScrollPane scroll = new ScrollPane(columns);
        scroll.setFitToHeight(true);
        scroll.setFitToWidth(false);
        scroll.setStyle("-fx-background: transparent; -fx-background-color: transparent; -fx-control-inner-background: transparent;");
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);

        Button primary = uiFactory.action(complete ? "VIEW CHAMPION" : "START NEXT MATCH", 520, 110, 38, "#1565C0", 26, () -> {
            if (complete) {
                showTournamentComplete(stage);
            } else {
                startNextTournamentMatch(stage);
            }
        });
        Button reset = uiFactory.action("RESET TOURNAMENT", 420, 110, 34, "#00897B", 22, () -> {
            resetTournamentRun();
            showTournamentSetup(stage);
        });
        Button exit = uiFactory.action("EXIT TO HUB", 420, 110, 34, "#FF1744", 22, () -> {
            resetTournamentRun();
            showMenu(stage);
        });
        HBox buttons = new HBox(24, primary, reset, exit);
        buttons.setAlignment(Pos.CENTER);

        root.setTop(header);
        root.setCenter(scroll);
        root.setBottom(buttons);
        BorderPane.setAlignment(header, Pos.CENTER);

        Scene scene = new Scene(root, WIDTH, HEIGHT);
        bindEscape(scene, exit);
        setupKeyboardNavigation(scene);
        applyConsoleHighlight(scene);
        setScenePreservingFullscreen(stage, scene);
        primary.requestFocus();
    }

    private void startNextTournamentMatch(Stage stage) {
        if (!tournamentModeActive) {
            showTournamentSetup(stage);
            return;
        }
        if (tournamentRounds.isEmpty()) {
            buildTournamentBracket();
        }
        resolveTournamentByes();
        TournamentMatch next = findNextTournamentMatch();

        if (next == null) {
            if (isTournamentComplete()) {
                showTournamentComplete(stage);
            } else {
                showTournamentBracket(stage);
            }
            return;
        }

        currentTournamentMatch = next;
        tournamentSlotA = next.a;
        tournamentSlotB = next.b;
        tournamentMatchResolved = false;

        if (tournamentSlotA != null && tournamentSlotB != null
                && !tournamentSlotA.human && !tournamentSlotB.human) {
            showTournamentCpuChoice(stage, next);
        } else {
            launchTournamentMatch(stage, next);
        }
    }

    private void launchTournamentMatch(Stage stage, TournamentMatch match) {
        if (match == null || match.a == null || match.b == null) {
            startNextTournamentMatch(stage);
            return;
        }
        tournamentSlotA = match.a;
        tournamentSlotB = match.b;
        tournamentMatchResolved = false;

        teamModeEnabled = false;
        Arrays.fill(playerTeams, 1);
        activePlayers = 2;
        selectedMap = pickTournamentMap();

        BirdType aType = tournamentSlotA.selectedType;
        BirdType bType = tournamentSlotB.selectedType;
        if (aType != null && !isBirdUnlocked(aType)) aType = null;
        if (bType != null && !isBirdUnlocked(bType)) bType = null;

        fightSelectedBirds[0] = aType;
        fightSelectedBirds[1] = bType;
        fightRandomSelected[0] = aType == null;
        fightRandomSelected[1] = bType == null;
        fightSelectedSkinKeys[0] = null;
        fightSelectedSkinKeys[1] = null;

        isAI[0] = !tournamentSlotA.human;
        isAI[1] = !tournamentSlotB.human;

        competitionSeriesActive = false;
        Arrays.fill(competitionRoundWins, 0);
        Arrays.fill(competitionTeamWins, 0);
        competitionRoundNumber = 1;

        resetMatchStats();
        startMatch(stage);
    }

    private void showTournamentCpuChoice(Stage stage, TournamentMatch match) {
        playMenuMusic();

        VBox root = MenuLayout.buildMenuRoot("-fx-background-color: linear-gradient(to bottom, #091426, #1A2B4B);",
                MENU_PADDING, 30);

        Label title = new Label("CPU VS CPU");
        title.setFont(Font.font("Arial Black", FontWeight.BOLD, 92));
        title.setTextFill(Color.GOLD);

        String left = tournamentEntryLabel(match.a);
        String right = tournamentEntryLabel(match.b);
        Label info = new Label(left + " vs " + right + "\nWatch the match or simulate the result.");
        MenuLayout.styleMenuMessage(info, 30, "#B3E5FC", MENU_TEXT_MAX_WIDTH, this::applyNoEllipsis);

        Button watch = uiFactory.action("WATCH MATCH", 520, 120, 42, "#1565C0", 26, () -> launchTournamentMatch(stage, match));
        Button skip = uiFactory.action("SIMULATE", 420, 120, 38, "#8E24AA", 26, () -> simulateTournamentMatch(stage, match));
        Button exit = uiFactory.action("EXIT TO HUB", 420, 120, 34, "#FF1744", 22, () -> {
            resetTournamentRun();
            showMenu(stage);
        });

        HBox buttons = new HBox(24, watch, skip, exit);
        buttons.setAlignment(Pos.CENTER);

        root.getChildren().addAll(title, info, buttons);
        Scene scene = new Scene(root, WIDTH, HEIGHT);
        bindEscape(scene, exit);
        setupKeyboardNavigation(scene);
        applyConsoleHighlight(scene);
        setScenePreservingFullscreen(stage, scene);
        watch.requestFocus();
    }

    private void simulateTournamentMatch(Stage stage, TournamentMatch match) {
        if (match == null || match.a == null || match.b == null) {
            startNextTournamentMatch(stage);
            return;
        }
        TournamentEntry winner = random.nextBoolean() ? match.a : match.b;
        recordTournamentWinner(match, winner);
        tournamentMatchResolved = true;
        currentTournamentMatch = null;
        showTournamentSimResult(stage, winner);
    }

    private void showTournamentSimResult(Stage stage, TournamentEntry winner) {
        playMenuMusic();

        VBox root = MenuLayout.buildMenuRoot("-fx-background-color: linear-gradient(to bottom, #081122, #13294B);",
                MENU_PADDING, 28);

        Label title = new Label("SIMULATED RESULT");
        title.setFont(Font.font("Arial Black", FontWeight.BOLD, 88));
        title.setTextFill(Color.GOLD);

        String winnerText = winner != null ? tournamentEntryLabel(winner) + " advances." : "No winner.";
        Label info = new Label(winnerText);
        MenuLayout.styleMenuMessage(info, 32, "#C5E1A5", MENU_TEXT_MAX_WIDTH, this::applyNoEllipsis);

        boolean complete = isTournamentComplete();
        Button next = uiFactory.action(complete ? "VIEW CHAMPION" : "VIEW BRACKET", 520, 120, 40, "#1565C0", 26, () -> {
            resetMatchStats();
            if (complete) {
                showTournamentComplete(stage);
            } else {
                showTournamentBracket(stage);
            }
        });
        Button exit = uiFactory.action("EXIT TO HUB", 420, 120, 34, "#FF1744", 22, () -> {
            resetTournamentRun();
            showMenu(stage);
        });
        HBox buttons = new HBox(24, next, exit);
        buttons.setAlignment(Pos.CENTER);

        root.getChildren().addAll(title, info, buttons);
        Scene scene = new Scene(root, WIDTH, HEIGHT);
        bindEscape(scene, exit);
        setupKeyboardNavigation(scene);
        applyConsoleHighlight(scene);
        setScenePreservingFullscreen(stage, scene);
        next.requestFocus();
    }

    private void showTournamentComplete(Stage stage) {
        playMenuMusic();

        TournamentEntry champion = null;
        if (!tournamentRounds.isEmpty()) {
            List<TournamentMatch> last = tournamentRounds.get(tournamentRounds.size() - 1);
            if (!last.isEmpty()) {
                champion = last.get(0).winner;
            }
        }

        VBox root = MenuLayout.buildMenuRoot("-fx-background-color: linear-gradient(to bottom, #081122, #13294B);",
                MENU_PADDING, 28);

        Label title = new Label("TOURNAMENT COMPLETE");
        title.setFont(Font.font("Arial Black", FontWeight.BOLD, 90));
        title.setTextFill(Color.GOLD);

        String champText = champion != null ? (tournamentEntryLabel(champion) + " is champion!") : "No champion.";
        Label info = new Label(champText);
        MenuLayout.styleMenuMessage(info, 34, "#FFE082", MENU_TEXT_MAX_WIDTH, this::applyNoEllipsis);

        Button setup = uiFactory.action("NEW TOURNAMENT", 520, 120, 38, "#1565C0", 26, () -> showTournamentSetup(stage));
        Button exit = uiFactory.action("EXIT TO HUB", 420, 120, 34, "#FF1744", 22, () -> {
            resetTournamentRun();
            showMenu(stage);
        });
        HBox buttons = new HBox(24, setup, exit);
        buttons.setAlignment(Pos.CENTER);

        root.getChildren().addAll(title, info, buttons);
        Scene scene = new Scene(root, WIDTH, HEIGHT);
        bindEscape(scene, exit);
        setupKeyboardNavigation(scene);
        applyConsoleHighlight(scene);
        setScenePreservingFullscreen(stage, scene);
        setup.requestFocus();
    }

    private void showLanMenu(Stage stage) {
        stopLanSession();
        playMenuMusic();
        currentStage = stage;

        VBox root = MenuLayout.buildMenuRoot("-fx-background-color: linear-gradient(to bottom, #0B1A24, #1C2F3C);",
                MENU_PADDING, MENU_GAP);

        Label title = new Label("LAN PLAY");
        title.setFont(Font.font("Impact", FontWeight.BOLD, 92));
        title.setTextFill(Color.web("#FFE082"));

        Label message = new Label("Host or join a local network match.\nHost is authoritative. Up to " + LAN_MAX_PLAYERS + " players.");
        MenuLayout.styleMenuMessage(message, 26, "#B3E5FC", MENU_TEXT_MAX_WIDTH, this::applyNoEllipsis);

        Button hostBtn = uiFactory.action("HOST LAN", 360, 90, 32, "#2E7D32", 22, () -> startLanHost(stage));
        Button joinBtn = uiFactory.action("JOIN LAN", 360, 90, 32, "#1565C0", 22, () -> showLanJoin(stage, ""));
        Button back = uiFactory.action("BACK TO HUB", 360, 90, 32, "#D32F2F", 22, () -> showMenu(stage));

        VBox buttons = new VBox(16, hostBtn, joinBtn, back);
        buttons.setAlignment(Pos.CENTER);

        root.getChildren().addAll(title, message, buttons);

        Scene scene = new Scene(root, WIDTH, HEIGHT);
        bindEscape(scene, back);
        setupKeyboardNavigation(scene);
        applyConsoleHighlight(scene);
        setScenePreservingFullscreen(stage, scene);
        hostBtn.requestFocus();
    }

    private void showLanJoin(Stage stage, String error) {
        playMenuMusic();
        currentStage = stage;

        VBox root = MenuLayout.buildMenuRoot("-fx-background-color: linear-gradient(to bottom, #0B1A24, #1C2F3C);",
                MENU_PADDING, 18);

        Label title = new Label("JOIN LAN");
        title.setFont(Font.font("Impact", FontWeight.BOLD, 78));
        title.setTextFill(Color.web("#FFE082"));

        Label prompt = new Label("Enter host IP (port " + LanProtocol.DEFAULT_PORT + ")");
        MenuLayout.styleMenuMessage(prompt, 24, "#B3E5FC", MENU_TEXT_MAX_WIDTH, this::applyNoEllipsis);

        Preferences prefs = Preferences.userNodeForPackage(BirdGame3.class);
        if (lanLastHost == null || lanLastHost.isBlank()) {
            lanLastHost = prefs.get("lan_last_host", "");
        }

        TextField hostField = new TextField(lanLastHost == null ? "" : lanLastHost);
        hostField.setMaxWidth(420);
        hostField.setPromptText("192.168.1.20");
        hostField.setFont(Font.font("Consolas", 22));

        Label status = new Label(error == null ? "" : error);
        status.setFont(Font.font("Consolas", 20));
        status.setTextFill(Color.ORANGE);
        lanStatusLabel = status;

        Button connect = uiFactory.action("CONNECT", 320, 80, 28, "#00C853", 20, () -> {
            String host = hostField.getText() == null ? "" : hostField.getText().trim();
            if (host.isBlank()) {
                status.setText("Enter a host IP to connect.");
                return;
            }
            lanLastHost = host;
            prefs.put("lan_last_host", host);
            startLanClient(stage, host);
        });

        Button back = uiFactory.action("BACK", 260, 70, 26, "#D32F2F", 20, () -> showLanMenu(stage));

        root.getChildren().addAll(title, prompt, hostField, status, connect, back);

        Scene scene = new Scene(root, WIDTH, HEIGHT);
        setupKeyboardNavigation(scene);
        applyConsoleHighlight(scene);
        bindEscape(scene, back);
        setScenePreservingFullscreen(stage, scene);
        hostField.requestFocus();
    }

    private void showLanLobby(Stage stage) {
        playMenuMusic();
        currentStage = stage;

        VBox root = MenuLayout.buildMenuRoot("-fx-background-color: linear-gradient(to bottom, #0B1A24, #1C2F3C);",
                new Insets(40, 60, 40, 60), 14);

        String lobbyTitle = lanIsHost ? "LAN LOBBY (HOST)" : "LAN LOBBY";
        Label title = new Label(lobbyTitle);
        title.setFont(Font.font("Impact", FontWeight.BOLD, 64));
        title.setTextFill(Color.web("#FFE082"));

        Label info = new Label(lanIsHost
                ? ("IP: " + findLanAddress() + "  Port: " + LanProtocol.DEFAULT_PORT)
                : ("Connected to: " + (lanLastHost == null ? "" : lanLastHost)));
        MenuLayout.styleMenuMessage(info, 20, "#B3E5FC", MENU_TEXT_MAX_WIDTH, this::applyNoEllipsis);

        lanStatusLabel = new Label(lanIsHost ? "Waiting for players..." : "Connecting...");
        lanStatusLabel.setFont(Font.font("Consolas", 18));
        lanStatusLabel.setTextFill(Color.web("#80DEEA"));

        lanSlotLabels = new Label[LAN_MAX_PLAYERS];
        VBox slots = new VBox(6);
        slots.setAlignment(Pos.CENTER);
        for (int i = 0; i < LAN_MAX_PLAYERS; i++) {
            Label slot = new Label();
            slot.setFont(Font.font("Consolas", 20));
            slot.setTextFill(Color.web("#CFD8DC"));
            lanSlotLabels[i] = slot;
            slots.getChildren().add(slot);
        }

        lanMapLabel = null;
        lanMapVoteLabel = new Label();
        lanMapVoteLabel.setFont(Font.font("Arial Black", 22));
        lanMapVoteLabel.setTextFill(Color.web("#FFECB3"));

        lanSelectMapButton = uiFactory.action("VOTE MAP", 240, 70, 26, "#455A64", 18, () -> openLanMapSelect(stage));
        VBox mapBox = new VBox(6, lanMapVoteLabel, lanSelectMapButton);
        mapBox.setAlignment(Pos.CENTER);

        lanYourBirdLabel = new Label();
        lanYourBirdLabel.setFont(Font.font("Arial Black", 22));
        lanYourBirdLabel.setTextFill(Color.web("#FFECB3"));

        lanSelectBirdButton = uiFactory.action("SELECT BIRD", 240, 70, 26, "#455A64", 18, () -> showLanBirdSelect(stage));
        VBox birdBox = new VBox(6, lanYourBirdLabel, lanSelectBirdButton);
        birdBox.setAlignment(Pos.CENTER);

        HBox controls = new HBox(30, mapBox, birdBox);
        controls.setAlignment(Pos.CENTER);

        lanStartButton = null;
        VBox actions = new VBox(10);
        actions.setAlignment(Pos.CENTER);
        lanReadyButton = uiFactory.action("READY UP", 260, 70, 26, "#2E7D32", 18, this::toggleLanReady);
        actions.getChildren().add(lanReadyButton);
        if (lanIsHost) {
            lanStartButton = uiFactory.action("START MATCH", 420, 90, 32, "#00C853", 22, () -> beginLanCountdown(stage));
            actions.getChildren().add(lanStartButton);
        } else {
            Label waiting = new Label("Waiting for host to start...");
            waiting.setFont(Font.font("Consolas", 18));
            waiting.setTextFill(Color.web("#B0BEC5"));
            actions.getChildren().add(waiting);
        }

        lanCountdownLabel = new Label();
        lanCountdownLabel.setFont(Font.font("Impact", FontWeight.BOLD, 42));
        lanCountdownLabel.setTextFill(Color.web("#FFD54F"));
        applyNoEllipsis(lanCountdownLabel);
        updateLanCountdownLabel();

        Button back = uiFactory.action("BACK TO HUB", 320, 80, 28, "#D32F2F", 20,
                () -> confirmLeaveLanSession(stage, () -> {
                    stopLanSession();
                    showMenu(stage);
                }));
        actions.getChildren().add(back);

        lanPortraits = new Canvas[LAN_MAX_PLAYERS];
        HBox portraitRow = new HBox(12);
        portraitRow.setAlignment(Pos.CENTER);
        for (int i = 0; i < LAN_MAX_PLAYERS; i++) {
            Label pLabel = new Label("P" + (i + 1));
            pLabel.setFont(Font.font("Consolas", 16));
            pLabel.setTextFill(Color.web("#B0BEC5"));
            Canvas portrait = new Canvas(80, 80);
            lanPortraits[i] = portrait;
            StackPane frame = new StackPane(portrait);
            frame.setPadding(new Insets(6));
            frame.setStyle("-fx-background-color: rgba(0,0,0,0.35); -fx-border-color: #607D8B; -fx-border-width: 2; -fx-background-radius: 14; -fx-border-radius: 14;");
            VBox slotBox = new VBox(6, pLabel, frame);
            slotBox.setAlignment(Pos.CENTER);
            portraitRow.getChildren().add(slotBox);
        }

        root.getChildren().addAll(title, info, lanStatusLabel, slots, controls, portraitRow, lanCountdownLabel, actions);

        Scene scene = new Scene(root, WIDTH, HEIGHT);
        setupKeyboardNavigation(scene);
        applyConsoleHighlight(scene);
        bindEscape(scene, () -> confirmLeaveLanSession(stage, () -> {
            stopLanSession();
            showMenu(stage);
        }));
        setScenePreservingFullscreen(stage, scene);

        refreshLanLobbyUI();
        if (lanIsHost && lanStartButton != null) {
            lanStartButton.requestFocus();
        } else {
            back.requestFocus();
        }
    }

    private void startLanHost(Stage stage) {
        stopLanSession();
        lanModeActive = true;
        lanIsHost = true;
        lanIsClient = false;
        lanMatchActive = false;
        lanPlayerIndex = 0;
        Arrays.fill(lanSlotConnected, false);
        Arrays.fill(lanSelectedSkinKeys, null);
        Arrays.fill(lanRandomBirds, false);
        Arrays.fill(lanReady, false);
        Arrays.fill(lanMapVotes, null);
        Arrays.fill(lanMapVoteRandom, false);
        Arrays.fill(lanInputMasks, 0);
        Arrays.fill(lanLastInputMasks, 0);
        lanLocalInputMask = 0;
        lanSlotConnected[0] = true;
        lanSelectedMap = null;
        lanSelectedMapRandom = false;
        lanVoteSignature = 0;
        if (lanSelectedBirds[0] == null) {
            lanSelectedBirds[0] = firstUnlockedBird();
        }
        lanRandomBirds[0] = false;
        lanSelectedSkinKeys[0] = null;
        lanReady[0] = false;
        lanMapVotes[0] = null;
        updateLanMapSelectionFromVotes();
        lanHost = new LanHostServer(this);
        if (!lanHost.start(LanProtocol.DEFAULT_PORT)) {
            Alert alert = new Alert(Alert.AlertType.ERROR, "Could not start LAN server on port " + LanProtocol.DEFAULT_PORT + ".", ButtonType.OK);
            alert.setTitle("LAN Error");
            alert.setHeaderText("Failed to host LAN match.");
            alert.showAndWait();
            stopLanSession();
            showLanMenu(stage);
            return;
        }
        showLanLobby(stage);
        broadcastLanLobby();
    }

    private void startLanClient(Stage stage, String host) {
        stopLanSession();
        lanModeActive = true;
        lanIsHost = false;
        lanIsClient = true;
        lanMatchActive = false;
        lanPlayerIndex = -1;
        Arrays.fill(lanSlotConnected, false);
        Arrays.fill(lanSelectedSkinKeys, null);
        Arrays.fill(lanRandomBirds, false);
        Arrays.fill(lanReady, false);
        Arrays.fill(lanMapVotes, null);
        Arrays.fill(lanMapVoteRandom, false);
        Arrays.fill(lanInputMasks, 0);
        Arrays.fill(lanLastInputMasks, 0);
        lanLocalInputMask = 0;
        lanSelectedMap = null;
        lanSelectedMapRandom = false;
        lanVoteSignature = 0;
        lanClient = new LanClient(this);
        showLanLobby(stage);

        Thread connectThread = new Thread(() -> {
            boolean ok = lanClient.connect(host, LanProtocol.DEFAULT_PORT);
            if (!ok) {
                javafx.application.Platform.runLater(() -> {
                    stopLanSession();
                    showLanJoin(stage, "Failed to connect: " + lanClient.getLastError());
                });
            }
        }, "LanClient-Connect");
        connectThread.setDaemon(true);
        connectThread.start();
    }

    private void refreshLanLobbyUI() {
        if (lanSlotLabels == null) return;
        int connectedCount = countLanConnected();
        for (int i = 0; i < LAN_MAX_PLAYERS; i++) {
            Label slot = lanSlotLabels[i];
            if (slot == null) continue;
            if (!lanSlotConnected[i]) {
                slot.setText("Slot " + (i + 1) + ": Open");
            } else {
                String name;
                if (lanRandomBirds[i]) {
                    name = "Random";
                } else {
                    BirdType type = lanSelectedBirds[i];
                    name = type != null ? type.name : "Selecting...";
                }
                String readyState = lanReady[i] ? "READY" : "NOT READY";
                slot.setText("P" + (i + 1) + ": " + name + " - " + readyState);
            }
        }
        if (lanMapLabel != null) {
            if (lanSelectedMap == null || lanSelectedMapRandom) {
                lanMapLabel.setText("NO VOTES YET");
            } else {
                lanMapLabel.setText(mapDisplayName(lanSelectedMap));
            }
        }
        if (lanMapVoteLabel != null) {
            if (lanPlayerIndex >= 0) {
                String vote;
                if (lanMapVoteRandom[lanPlayerIndex]) {
                    vote = "RANDOM";
                } else if (lanMapVotes[lanPlayerIndex] != null) {
                    vote = mapDisplayName(lanMapVotes[lanPlayerIndex]);
                } else {
                    vote = "(none)";
                }
                lanMapVoteLabel.setText("YOUR VOTE: " + vote);
            } else {
                lanMapVoteLabel.setText("YOUR VOTE: (awaiting slot)");
            }
        }
        if (lanStatusLabel != null) {
            if (lanIsHost) {
                lanStatusLabel.setText("Players: " + connectedCount + "/" + LAN_MAX_PLAYERS);
            } else if (lanPlayerIndex >= 0) {
                lanStatusLabel.setText("Connected as P" + (lanPlayerIndex + 1));
            } else {
                lanStatusLabel.setText("Connecting...");
            }
        }
        if (lanYourBirdLabel != null) {
            if (lanPlayerIndex >= 0) {
                String name;
                if (lanRandomBirds[lanPlayerIndex]) {
                    name = "RANDOM";
                } else {
                    BirdType type = lanSelectedBirds[lanPlayerIndex];
                    name = type != null ? type.name : "Selecting...";
                }
                lanYourBirdLabel.setText("YOUR BIRD: " + name);
            } else {
                lanYourBirdLabel.setText("YOUR BIRD: (awaiting slot)");
            }
        }
        boolean canSelect = lanPlayerIndex >= 0;
        if (lanSelectMapButton != null) lanSelectMapButton.setDisable(!canSelect);
        if (lanSelectBirdButton != null) lanSelectBirdButton.setDisable(!canSelect);
        if (lanReadyButton != null) {
            lanReadyButton.setDisable(!canSelect);
            boolean ready = canSelect && lanReady[lanPlayerIndex];
            lanReadyButton.setText(ready ? "READY" : "NOT READY");
            lanReadyButton.setStyle(ready
                    ? "-fx-background-color: #00C853; -fx-text-fill: white;"
                    : "-fx-background-color: #546E7A; -fx-text-fill: white;");
        }
        if (lanStartButton != null) {
            lanStartButton.setDisable(connectedCount < 2 || lanCountdownTimeline != null);
        }
        if (lanPortraits != null) {
            for (int i = 0; i < LAN_MAX_PLAYERS; i++) {
                Canvas canvas = lanPortraits[i];
                if (canvas == null) continue;
                GraphicsContext g = canvas.getGraphicsContext2D();
                g.clearRect(0, 0, canvas.getWidth(), canvas.getHeight());
                if (!lanSlotConnected[i]) continue;
                boolean randomPick = lanRandomBirds[i];
                BirdType type = randomPick ? null : lanSelectedBirds[i];
                String skinKey = randomPick ? null : lanSelectedSkinKeys[i];
                drawRosterSprite(canvas, type, skinKey, randomPick);
            }
        }
    }

    private void toggleLanReady() {
        if (lanPlayerIndex < 0) return;
        lanReady[lanPlayerIndex] = !lanReady[lanPlayerIndex];
        if (lanIsHost) {
            broadcastLanLobby();
        } else if (lanClient != null) {
            lanClient.sendReady(lanReady[lanPlayerIndex]);
        }
        refreshLanLobbyUI();
    }

    private void beginLanCountdown(Stage stage) {
        if (!lanIsHost) return;
        if (lanCountdownTimeline != null) return;
        if (countLanConnected() < 2) return;
        lanCountdownValue = 5;
        updateLanCountdownLabel();
        if (lanHost != null) {
            lanHost.broadcastCountdown(lanCountdownValue);
        }
        lanCountdownTimeline = new Timeline(new KeyFrame(Duration.seconds(1), e -> {
            lanCountdownValue--;
            if (lanCountdownValue <= 0) {
                stopLanCountdown();
                startLanMatchHost(stage);
                return;
            }
            updateLanCountdownLabel();
            if (lanHost != null) {
                lanHost.broadcastCountdown(lanCountdownValue);
            }
        }));
        lanCountdownTimeline.setCycleCount(Timeline.INDEFINITE);
        lanCountdownTimeline.play();
        refreshLanLobbyUI();
    }

    private void stopLanCountdown() {
        if (lanCountdownTimeline != null) {
            lanCountdownTimeline.stop();
            lanCountdownTimeline = null;
        }
        lanCountdownValue = 0;
        updateLanCountdownLabel();
    }

    private void updateLanCountdownLabel() {
        if (lanCountdownLabel == null) return;
        if (lanCountdownValue > 0) {
            lanCountdownLabel.setText("MATCH STARTS IN " + lanCountdownValue);
        } else {
            lanCountdownLabel.setText("");
        }
    }

    private void openLanMapSelect(Stage stage) {
        if (stage == null) return;
        if (lanPlayerIndex < 0) return;
        stageSelectReturn = () -> showLanLobby(stage);
        stageSelectHandler = map -> {
            lanMapVotes[lanPlayerIndex] = map;
            lanMapVoteRandom[lanPlayerIndex] = false;
            if (lanIsHost) {
                updateLanMapSelectionFromVotes();
                broadcastLanLobby();
            } else if (lanClient != null) {
                lanClient.sendMapVote(map, false);
            }
            showLanLobby(stage);
        };
        stageSelectRandomHandler = () -> {
            lanMapVotes[lanPlayerIndex] = null;
            lanMapVoteRandom[lanPlayerIndex] = true;
            if (lanIsHost) {
                updateLanMapSelectionFromVotes();
                broadcastLanLobby();
            } else if (lanClient != null) {
                lanClient.sendMapVote(null, true);
            }
            showLanLobby(stage);
        };
        showStageSelect(stage);
    }

    private void sendLanSelectionUpdate() {
        if (lanPlayerIndex < 0) return;
        if (lanIsHost) {
            broadcastLanLobby();
        } else if (lanClient != null) {
            lanClient.sendSelect(lanSelectedBirds[lanPlayerIndex], lanRandomBirds[lanPlayerIndex], lanSelectedSkinKeys[lanPlayerIndex]);
        }
    }

    private void showLanBirdSelect(Stage stage) {
        if (stage == null) return;
        if (lanPlayerIndex < 0) {
            showLanLobby(stage);
            return;
        }
        playMenuMusic();
        BorderPane root = new BorderPane();
        root.setPadding(new Insets(24, 34, 24, 34));
        root.setStyle("-fx-background-color: linear-gradient(to bottom, #091421, #152738, #1F3443);");

        Button back = uiFactory.action("BACK TO LOBBY", 320, 86, 32, "#D32F2F", 22, () -> showLanLobby(stage));
        Label title = new Label("SELECT BIRD");
        title.setFont(Font.font("Impact", FontWeight.BOLD, 78));
        title.setTextFill(Color.web("#FFE082"));

        HBox topBar = new HBox(18, back, title);
        topBar.setAlignment(Pos.CENTER_LEFT);
        root.setTop(topBar);

        List<BirdType> available = unlockedBirdPool();
        if (available.isEmpty()) {
            available.add(BirdType.PIGEON);
        }
        List<BirdType> gridBirds = new ArrayList<>(available);
        gridBirds.add(null); // random slot

        GridPane grid = new GridPane();
        grid.setHgap(20);
        grid.setVgap(20);
        grid.setAlignment(Pos.CENTER);

        Map<BirdType, Button> buttonByType = new HashMap<>();
        final Button[] randomButton = new Button[1];

        Canvas preview = new Canvas(160, 160);
        Label selectionLabel = new Label();
        selectionLabel.setFont(Font.font("Consolas", 22));
        selectionLabel.setTextFill(Color.web("#FFD54F"));
        applyNoEllipsis(selectionLabel);

        Button skinButton = new Button();
        skinButton.setPrefSize(240, 60);
        skinButton.setFont(Font.font("Consolas", 18));
        skinButton.setStyle("-fx-background-color: #37474F; -fx-text-fill: white;");

        int columns = Math.min(5, Math.max(1, (int) Math.ceil(gridBirds.size() / 2.0)));
        for (int i = 0; i < gridBirds.size(); i++) {
            BirdType type = gridBirds.get(i);
            boolean isRandom = type == null;
            Canvas icon = new Canvas(90, 90);
            drawRosterSprite(icon, type, null, isRandom);
            Button btn = new Button(isRandom ? "RANDOM" : type.name.toUpperCase());
            btn.setGraphic(icon);
            btn.setContentDisplay(ContentDisplay.TOP);
            btn.setPrefSize(200, 160);
            btn.setFont(Font.font("Consolas", 16));
            btn.setWrapText(true);
            btn.setTextAlignment(TextAlignment.CENTER);
            btn.setOnAction(e -> {
                playButtonClick();
                if (isRandom) {
                    lanRandomBirds[lanPlayerIndex] = true;
                    lanSelectedBirds[lanPlayerIndex] = null;
                    lanSelectedSkinKeys[lanPlayerIndex] = null;
                } else {
                    lanRandomBirds[lanPlayerIndex] = false;
                    lanSelectedBirds[lanPlayerIndex] = type;
                    lanSelectedSkinKeys[lanPlayerIndex] = normalizeAdventureSkinChoice(type, lanSelectedSkinKeys[lanPlayerIndex]);
                }
                sendLanSelectionUpdate();
                refreshLanLobbyUI();
                updateLanBirdSelectButtons(buttonByType, randomButton[0]);
                updateLanBirdSelectPreview(preview, selectionLabel, skinButton);
            });
            int col = i % columns;
            int row = i / columns;
            grid.add(btn, col, row);
            if (isRandom) {
                randomButton[0] = btn;
            } else {
                buttonByType.put(type, btn);
            }
        }

        skinButton.setOnAction(e -> {
            playButtonClick();
            if (lanRandomBirds[lanPlayerIndex]) return;
            BirdType type = lanSelectedBirds[lanPlayerIndex];
            if (type == null) return;
            List<String> options = adventureSkinOptions(type);
            if (options.size() <= 1) return;
            String current = normalizeAdventureSkinChoice(type, lanSelectedSkinKeys[lanPlayerIndex]);
            int idx = options.indexOf(current);
            if (idx < 0) idx = 0;
            lanSelectedSkinKeys[lanPlayerIndex] = options.get((idx + 1) % options.size());
            sendLanSelectionUpdate();
            refreshLanLobbyUI();
            updateLanBirdSelectPreview(preview, selectionLabel, skinButton);
        });

        updateLanBirdSelectPreview(preview, selectionLabel, skinButton);
        updateLanBirdSelectButtons(buttonByType, randomButton[0]);

        VBox previewBox = new VBox(12, preview, selectionLabel, skinButton);
        previewBox.setAlignment(Pos.CENTER);
        previewBox.setPadding(new Insets(12));
        previewBox.setStyle("-fx-background-color: rgba(0,0,0,0.35); -fx-border-color: #90A4AE; -fx-border-width: 2; -fx-background-radius: 18; -fx-border-radius: 18;");

        BorderPane center = new BorderPane();
        center.setCenter(grid);
        center.setRight(previewBox);
        BorderPane.setMargin(previewBox, new Insets(0, 0, 0, 20));
        root.setCenter(center);

        Scene scene = new Scene(root, WIDTH, HEIGHT);
        bindEscape(scene, back);
        setupKeyboardNavigation(scene);
        applyConsoleHighlight(scene);
        setScenePreservingFullscreen(stage, scene);
         Button focus = randomButton[0] != null ? randomButton[0] : back;
        focus.requestFocus();
    }

    private void updateLanBirdSelectButtons(Map<BirdType, Button> buttonByType, Button randomButton) {
        if (lanPlayerIndex < 0) return;
        BirdType selected = lanSelectedBirds[lanPlayerIndex];
        boolean randomPick = lanRandomBirds[lanPlayerIndex];
        String baseStyle = "-fx-background-color: rgba(0,0,0,0.45); -fx-border-color: #90A4AE; -fx-border-width: 2; -fx-text-fill: white;";
        String activeStyle = "-fx-background-color: rgba(255,213,79,0.25); -fx-border-color: #FFD54F; -fx-border-width: 3; -fx-text-fill: white;";
        for (Map.Entry<BirdType, Button> entry : buttonByType.entrySet()) {
            boolean active = !randomPick && entry.getKey() == selected;
            entry.getValue().setStyle(active ? activeStyle : baseStyle);
        }
        if (randomButton != null) {
            randomButton.setStyle(randomPick ? activeStyle : baseStyle);
        }
    }

    private void updateLanBirdSelectPreview(Canvas preview, Label selectionLabel, Button skinButton) {
        if (lanPlayerIndex < 0) return;
        boolean randomPick = lanRandomBirds[lanPlayerIndex];
        BirdType type = randomPick ? null : lanSelectedBirds[lanPlayerIndex];
        String skinKey = randomPick ? null : normalizeAdventureSkinChoice(type, lanSelectedSkinKeys[lanPlayerIndex]);
        lanSelectedSkinKeys[lanPlayerIndex] = skinKey;
        if (preview != null) {
            drawRosterSprite(preview, type, skinKey, randomPick);
        }
        if (selectionLabel != null) {
            selectionLabel.setText(randomPick ? "RANDOM" : (type != null ? type.name.toUpperCase() : "SELECTING..."));
        }
        if (skinButton != null) {
            if (type == null || randomPick) {
                skinButton.setDisable(true);
                skinButton.setOpacity(0.7);
                skinButton.setText("SKIN: BASE");
            } else {
                List<String> options = adventureSkinOptions(type);
                skinButton.setDisable(options.size() <= 1);
                skinButton.setOpacity(options.size() <= 1 ? 0.7 : 1.0);
                skinButton.setText(adventureSkinLabel(type, skinKey));
            }
        }
    }

    private void cycleLanBird(int delta) {
        if (lanPlayerIndex < 0) return;
        List<BirdType> pool = unlockedBirdPool();
        if (pool.isEmpty()) return;
        int total = pool.size() + 1;
        int currentIndex;
        if (lanRandomBirds[lanPlayerIndex]) {
            currentIndex = total - 1;
        } else {
            BirdType current = lanSelectedBirds[lanPlayerIndex];
            int idx = pool.indexOf(current);
            if (idx < 0) idx = 0;
            currentIndex = idx;
        }
        int next = (currentIndex + delta + total) % total;
        if (next == total - 1) {
            lanRandomBirds[lanPlayerIndex] = true;
            lanSelectedBirds[lanPlayerIndex] = null;
            lanSelectedSkinKeys[lanPlayerIndex] = null;
        } else {
            lanRandomBirds[lanPlayerIndex] = false;
            lanSelectedBirds[lanPlayerIndex] = pool.get(next);
            lanSelectedSkinKeys[lanPlayerIndex] = null;
        }
        if (lanIsHost) {
            broadcastLanLobby();
        } else if (lanClient != null) {
            lanClient.sendSelect(lanSelectedBirds[lanPlayerIndex], lanRandomBirds[lanPlayerIndex], lanSelectedSkinKeys[lanPlayerIndex]);
        }
        refreshLanLobbyUI();
    }

    private void cycleLanMap(int delta) {
        if (lanPlayerIndex < 0) return;
        List<MapType> maps = availableLanMaps();
        if (maps.isEmpty()) return;
        int total = maps.size() + 1;
        int currentIndex;
        if (lanMapVoteRandom[lanPlayerIndex]) {
            currentIndex = total - 1;
        } else {
            int idx = maps.indexOf(lanMapVotes[lanPlayerIndex]);
            if (idx < 0) {
                idx = maps.indexOf(lanSelectedMap);
            }
            if (idx < 0) idx = 0;
            currentIndex = idx;
        }
        int next = (currentIndex + delta + total) % total;
        if (next == total - 1) {
            lanMapVoteRandom[lanPlayerIndex] = true;
            lanMapVotes[lanPlayerIndex] = null;
        } else {
            lanMapVoteRandom[lanPlayerIndex] = false;
            lanMapVotes[lanPlayerIndex] = maps.get(next);
        }
        if (lanIsHost) {
            updateLanMapSelectionFromVotes();
            broadcastLanLobby();
        } else if (lanClient != null) {
            lanClient.sendMapVote(lanMapVotes[lanPlayerIndex], lanMapVoteRandom[lanPlayerIndex]);
        }
        refreshLanLobbyUI();
    }

    private void updateLanMapSelectionFromVotes() {
        int signature = computeLanVoteSignature();
        if (signature == lanVoteSignature) return;
        lanVoteSignature = signature;

        List<MapType> pool = new ArrayList<>();
        int randomVotes = 0;
        for (int i = 0; i < LAN_MAX_PLAYERS; i++) {
            if (!lanSlotConnected[i]) continue;
            if (lanMapVoteRandom[i]) {
                randomVotes++;
                continue;
            }
            MapType vote = lanMapVotes[i];
            if (vote != null) {
                pool.add(vote);
            }
        }

        if (pool.isEmpty() && randomVotes == 0) {
            lanSelectedMap = null;
            lanSelectedMapRandom = true;
            return;
        }

        List<MapType> allMaps = availableLanMaps();
        for (int i = 0; i < randomVotes; i++) {
            if (!allMaps.isEmpty()) {
                pool.add(allMaps.get(random.nextInt(allMaps.size())));
            }
        }

        if (pool.isEmpty()) {
            lanSelectedMap = null;
            lanSelectedMapRandom = true;
            return;
        }

        lanSelectedMap = pool.get(random.nextInt(pool.size()));
        lanSelectedMapRandom = false;
    }

    private int computeLanVoteSignature() {
        int hash = 1;
        for (int i = 0; i < LAN_MAX_PLAYERS; i++) {
            if (!lanSlotConnected[i]) {
                hash = 31 * hash;
                continue;
            }
            int value;
            if (lanMapVoteRandom[i]) {
                value = -2;
            } else if (lanMapVotes[i] != null) {
                value = lanMapVotes[i].ordinal();
            } else {
                value = -1;
            }
            hash = 31 * hash + value;
        }
        return hash;
    }

    private MapType pickLanMapForMatch() {
        List<MapType> pool = new ArrayList<>();
        int randomVotes = 0;
        for (int i = 0; i < LAN_MAX_PLAYERS; i++) {
            if (!lanSlotConnected[i]) continue;
            if (lanMapVoteRandom[i]) {
                randomVotes++;
                continue;
            }
            MapType vote = lanMapVotes[i];
            if (vote != null) {
                pool.add(vote);
            }
        }

        List<MapType> allMaps = availableLanMaps();
        if (pool.isEmpty() && randomVotes == 0) {
            if (!allMaps.isEmpty()) {
                return allMaps.get(random.nextInt(allMaps.size()));
            }
            return MapType.FOREST;
        }

        for (int i = 0; i < randomVotes; i++) {
            if (!allMaps.isEmpty()) {
                pool.add(allMaps.get(random.nextInt(allMaps.size())));
            }
        }

        if (pool.isEmpty()) {
            if (!allMaps.isEmpty()) {
                return allMaps.get(random.nextInt(allMaps.size()));
            }
            return MapType.FOREST;
        }

        return pool.get(random.nextInt(pool.size()));
    }

    private List<MapType> availableLanMaps() {
        List<MapType> maps = new ArrayList<>();
        maps.add(MapType.FOREST);
        maps.add(MapType.CITY);
        maps.add(MapType.SKYCLIFFS);
        maps.add(MapType.VIBRANT_JUNGLE);
        maps.add(MapType.CAVE);
        maps.add(MapType.BATTLEFIELD);
        return maps;
    }

    private int countLanConnected() {
        int count = 0;
        for (boolean connected : lanSlotConnected) {
            if (connected) count++;
        }
        return count;
    }

    private String mapDisplayName(MapType map) {
        return switch (map) {
            case CITY -> "Pigeon's Rooftops";
            case SKYCLIFFS -> "Sky Cliffs";
            case VIBRANT_JUNGLE -> "Vibrant Jungle";
            case CAVE -> "Echo Cavern";
            case BATTLEFIELD -> "Battlefield";
            default -> "Big Forest";
        };
    }

    private String findLanAddress() {
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                NetworkInterface iface = interfaces.nextElement();
                if (!iface.isUp() || iface.isLoopback() || iface.isVirtual()) continue;
                Enumeration<InetAddress> addrs = iface.getInetAddresses();
                while (addrs.hasMoreElements()) {
                    InetAddress addr = addrs.nextElement();
                    if (addr instanceof Inet4Address && addr.isSiteLocalAddress()) {
                        return addr.getHostAddress();
                    }
                }
            }
            interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                NetworkInterface iface = interfaces.nextElement();
                if (!iface.isUp() || iface.isLoopback() || iface.isVirtual()) continue;
                Enumeration<InetAddress> addrs = iface.getInetAddresses();
                while (addrs.hasMoreElements()) {
                    InetAddress addr = addrs.nextElement();
                    if (addr instanceof Inet4Address && !addr.isLoopbackAddress()) {
                        return addr.getHostAddress();
                    }
                }
            }
        } catch (SocketException ignored) {
        }
        return "127.0.0.1";
    }

    private void broadcastLanLobby() {
        if (!lanIsHost) return;
        updateLanMapSelectionFromVotes();
        if (lanHost != null) {
            MapType mapToSend = lanSelectedMap != null ? lanSelectedMap : MapType.FOREST;
            boolean mapRandom = lanSelectedMap == null || lanSelectedMapRandom;
            lanHost.broadcastLobby(mapToSend, mapRandom, lanSlotConnected, lanSelectedBirds, lanRandomBirds, lanSelectedSkinKeys, lanReady);
        }
    }

    void onLanClientConnected(int slot) {
        javafx.application.Platform.runLater(() -> {
            if (!lanModeActive || !lanIsHost) return;
            if (slot < 0 || slot >= LAN_MAX_PLAYERS) return;
            lanSlotConnected[slot] = true;
            lanRandomBirds[slot] = false;
            lanSelectedSkinKeys[slot] = null;
            lanReady[slot] = false;
            lanMapVotes[slot] = null;
            lanMapVoteRandom[slot] = false;
            if (lanSelectedBirds[slot] == null) {
                lanSelectedBirds[slot] = firstUnlockedBird();
            }
            synchronized (lanInputLock) {
                lanInputMasks[slot] = 0;
                lanLastInputMasks[slot] = 0;
            }
            updateLanMapSelectionFromVotes();
            refreshLanLobbyUI();
            broadcastLanLobby();
        });
    }

    void onLanClientDisconnected(int slot) {
        javafx.application.Platform.runLater(() -> {
            if (!lanModeActive || !lanIsHost) return;
            if (slot < 0 || slot >= LAN_MAX_PLAYERS) return;
            lanSlotConnected[slot] = false;
            lanSelectedBirds[slot] = null;
            lanRandomBirds[slot] = false;
            lanSelectedSkinKeys[slot] = null;
            lanReady[slot] = false;
            lanMapVotes[slot] = null;
            lanMapVoteRandom[slot] = false;
            synchronized (lanInputLock) {
                lanInputMasks[slot] = 0;
                lanLastInputMasks[slot] = 0;
            }
            updateLanMapSelectionFromVotes();
            refreshLanLobbyUI();
            broadcastLanLobby();
        });
    }

    void onLanClientSelected(int slot, BirdType type, boolean random, String skinKey) {
        javafx.application.Platform.runLater(() -> {
            if (!lanModeActive || !lanIsHost) return;
            if (slot < 0 || slot >= LAN_MAX_PLAYERS) return;
            lanRandomBirds[slot] = random;
            lanSelectedBirds[slot] = random ? null : type;
            lanSelectedSkinKeys[slot] = random ? null : skinKey;
            refreshLanLobbyUI();
            broadcastLanLobby();
        });
    }

    void onLanClientMapVote(int slot, MapType map, boolean random) {
        javafx.application.Platform.runLater(() -> {
            if (!lanModeActive || !lanIsHost) return;
            if (slot < 0 || slot >= LAN_MAX_PLAYERS) return;
            lanMapVoteRandom[slot] = random;
            lanMapVotes[slot] = random ? null : map;
            updateLanMapSelectionFromVotes();
            refreshLanLobbyUI();
            broadcastLanLobby();
        });
    }

    void onLanClientReady(int slot, boolean ready) {
        javafx.application.Platform.runLater(() -> {
            if (!lanModeActive || !lanIsHost) return;
            if (slot < 0 || slot >= LAN_MAX_PLAYERS) return;
            lanReady[slot] = ready;
            refreshLanLobbyUI();
            broadcastLanLobby();
        });
    }

    void onLanClientInput(int slot, int mask) {
        if (!lanModeActive || !lanIsHost) return;
        if (slot < 0 || slot >= LAN_MAX_PLAYERS) return;
        synchronized (lanInputLock) {
            lanInputMasks[slot] = mask;
        }
    }

    void onLanServerError(IOException e) {
        javafx.application.Platform.runLater(() -> {
            if (!lanModeActive || !lanIsHost) return;
            Alert alert = new Alert(Alert.AlertType.ERROR, "LAN hosting error: " + (e.getMessage() == null ? "" : e.getMessage()), ButtonType.OK);
            alert.setTitle("LAN Error");
            alert.setHeaderText("Hosting interrupted.");
            alert.showAndWait();
            stopLanSession();
            if (currentStage != null) {
                showLanMenu(currentStage);
            }
        });
    }

    void onLanWelcome(int idx) {
        javafx.application.Platform.runLater(() -> {
            if (!lanModeActive || !lanIsClient) return;
            lanPlayerIndex = idx;
            if (lanPlayerIndex >= 0 && lanPlayerIndex < LAN_MAX_PLAYERS) {
                if (!lanRandomBirds[lanPlayerIndex] && lanSelectedBirds[lanPlayerIndex] == null) {
                    lanSelectedBirds[lanPlayerIndex] = firstUnlockedBird();
                }
                if (lanClient != null) {
                    lanSelectedSkinKeys[lanPlayerIndex] = normalizeAdventureSkinChoice(
                            lanSelectedBirds[lanPlayerIndex], lanSelectedSkinKeys[lanPlayerIndex]);
                    lanClient.sendSelect(lanSelectedBirds[lanPlayerIndex], lanRandomBirds[lanPlayerIndex], lanSelectedSkinKeys[lanPlayerIndex]);
                    lanClient.sendReady(lanReady[lanPlayerIndex]);
                }
            }
            refreshLanLobbyUI();
        });
    }

    void onLanLobbyUpdate(MapType map, boolean mapRandom, boolean[] connected, BirdType[] birds, boolean[] randomBirds, String[] skinKeys, boolean[] ready) {
        javafx.application.Platform.runLater(() -> {
            if (!lanModeActive || !lanIsClient) return;
            lanSelectedMap = map;
            lanSelectedMapRandom = mapRandom;
            if (connected != null) {
                System.arraycopy(connected, 0, lanSlotConnected, 0, Math.min(connected.length, LAN_MAX_PLAYERS));
            }
            if (birds != null) {
                System.arraycopy(birds, 0, lanSelectedBirds, 0, Math.min(birds.length, LAN_MAX_PLAYERS));
            }
            if (randomBirds != null) {
                System.arraycopy(randomBirds, 0, lanRandomBirds, 0, Math.min(randomBirds.length, LAN_MAX_PLAYERS));
            }
            if (skinKeys != null) {
                System.arraycopy(skinKeys, 0, lanSelectedSkinKeys, 0, Math.min(skinKeys.length, LAN_MAX_PLAYERS));
            }
            if (ready != null) {
                System.arraycopy(ready, 0, lanReady, 0, Math.min(ready.length, LAN_MAX_PLAYERS));
            }
            refreshLanLobbyUI();
        });
    }

    void onLanStartMatch(MapType map, long seed, boolean[] connected, BirdType[] birds, String[] skinKeys) {
        javafx.application.Platform.runLater(() -> startLanMatchClient(currentStage, map, seed, connected, birds, skinKeys));
    }

    void onLanState(LanState state) {
        if (!lanModeActive || !lanIsClient) return;
        javafx.application.Platform.runLater(() -> {
            if (!lanMatchActive) {
                pendingLanState = state;
                return;
            }
            applyLanState(state);
        });
    }

    void onLanCountdown(int seconds) {
        if (!lanModeActive || !lanIsClient) return;
        javafx.application.Platform.runLater(() -> {
            lanCountdownValue = seconds;
            updateLanCountdownLabel();
        });
    }

    void onLanMatchEnd(int winnerIndex) {
        if (!lanModeActive || !lanIsClient) return;
        matchEnded = true;
        lanMatchActive = false;
        javafx.application.Platform.runLater(() -> {
            if (timer != null) timer.stop();
            showLanResults(currentStage, winnerIndex);
        });
    }

    private void showLanResults(Stage stage, int winnerIndex) {
        if (stage == null) return;
        lanResultsActionPending = false;
        lanResultsStatusLabel = null;
        if (musicPlayer != null) musicPlayer.stop();
        if (menuMusicPlayer != null) menuMusicPlayer.stop();
        if (victoryMusicPlayer != null) {
            victoryMusicPlayer.stop();
            if (musicEnabled) victoryMusicPlayer.play();
        }

        Bird winner = null;
        String winnerText = "TIME'S UP!";
        if (winnerIndex >= 0 && winnerIndex < players.length) {
            winner = players[winnerIndex];
            winnerText = (winner != null ? winner.name.toUpperCase() : ("P" + (winnerIndex + 1) + " WINS!"));
            if (winner != null) {
                winnerText = winner.name.toUpperCase() + " WINS!";
            }
        }

        VBox root = MenuLayout.buildMenuRoot("-fx-background-color: linear-gradient(to bottom, #0f0c29, #302b63, #24243e);",
                MENU_PADDING, 30);

        Label title = new Label(winnerText);
        title.setFont(Font.font("Arial Black", 90));
        title.setTextFill(winner != null ? Color.GOLD : Color.SILVER);
        title.setEffect(new DropShadow(40, Color.BLACK));
        applyNoEllipsis(title);

        Label subtitle = new Label("LAN RESULTS");
        subtitle.setFont(Font.font("Consolas", 26));
        subtitle.setTextFill(Color.web("#B3E5FC"));

        int coinsEarned = awardBirdCoinsForMatch(winner);
        Label coinsLabel = new Label("BIRD COINS +" + coinsEarned + "   TOTAL: " + birdCoins);
        coinsLabel.setFont(Font.font("Consolas", 28));
        coinsLabel.setTextFill(Color.web("#FFD54F"));

        Label mapLabel = new Label("MAP: " + mapDisplayName(selectedMap));
        mapLabel.setFont(Font.font("Consolas", 22));
        mapLabel.setTextFill(Color.web("#B3E5FC"));

        VBox scoreboard = new VBox(10);
        scoreboard.setAlignment(Pos.CENTER);
        scoreboard.setPadding(new Insets(18, 26, 18, 26));
        scoreboard.setMaxWidth(1200);
        scoreboard.setStyle("-fx-background-color: rgba(0,0,0,0.45); -fx-background-radius: 22; -fx-border-color: #FFD54F; -fx-border-width: 2; -fx-border-radius: 22;");

        List<Integer> ranking = new ArrayList<>();
        for (int i = 0; i < LAN_MAX_PLAYERS; i++) {
            if (lanSlotConnected[i] || players[i] != null) {
                ranking.add(i);
            }
        }
        ranking.sort((a, b) -> Integer.compare(scores[b], scores[a]));

        for (int idx : ranking) {
            Bird b = players[idx];
            BirdType type = b != null ? b.type : lanSelectedBirds[idx];
            String skinKey = b != null ? skinKeyForBird(b) : lanSelectedSkinKeys[idx];
            Canvas icon = new Canvas(60, 60);
            drawRosterSprite(icon, type, skinKey, false);
            String birdName = type != null ? type.name : "Unknown";
            String line = "P" + (idx + 1) + "  |  " + birdName + "  |  Score " + scores[idx];
            Label row = new Label(line);
            row.setFont(Font.font("Consolas", 22));
            row.setTextFill(Color.web("#E3F2FD"));
            applyNoEllipsis(row);
            HBox rowBox = new HBox(12, icon, row);
            rowBox.setAlignment(Pos.CENTER_LEFT);
            scoreboard.getChildren().add(rowBox);
        }

        Button backLobby = null;
        Button exit = null;
        HBox actions = new HBox(20);
        actions.setAlignment(Pos.CENTER);
        if (lanIsHost) {
            backLobby = uiFactory.action("BACK TO LOBBY", 360, 80, 28, "#00C853", 20, () -> {
            });
            exit = uiFactory.action("EXIT TO HUB", 320, 80, 28, "#D32F2F", 20, () -> {
            });
            Button finalBackLobby = backLobby;
            Button finalExit = exit;
            backLobby.setOnAction(e -> requestLanResultsAction(LAN_RESULTS_ACTION_LOBBY, stage, finalBackLobby, finalExit));
            exit.setOnAction(e -> confirmLeaveLanSession(stage,
                    () -> requestLanResultsAction(LAN_RESULTS_ACTION_EXIT, stage, finalBackLobby, finalExit)));
            backLobby.setDisable(true);
            exit.setDisable(true);
            PauseTransition unlock = new PauseTransition(Duration.millis(LAN_RESULTS_MIN_SHOW_MS));
            unlock.setOnFinished(e -> {
                finalBackLobby.setDisable(false);
                finalExit.setDisable(false);
                finalBackLobby.requestFocus();
            });
            unlock.play();
            actions.getChildren().addAll(backLobby, exit);
        } else {
            Label waiting = new Label("Waiting for host to continue...");
            waiting.setFont(Font.font("Consolas", 22));
            waiting.setTextFill(Color.web("#B0BEC5"));
            lanResultsStatusLabel = waiting;
            actions.getChildren().add(waiting);
        }

        root.getChildren().addAll(title, subtitle, coinsLabel, mapLabel, scoreboard, actions);

        Scene scene = new Scene(root, WIDTH, HEIGHT);
        if (backLobby != null) {
            bindEscape(scene, backLobby);
        }
        setupKeyboardNavigation(scene);
        applyConsoleHighlight(scene);
        setScenePreservingFullscreen(stage, scene);
    }

    private void requestLanResultsAction(int action, Stage stage, Button backLobby, Button exit) {
        if (lanResultsActionPending) return;
        lanResultsActionPending = true;
        if (backLobby != null) backLobby.setDisable(true);
        if (exit != null) exit.setDisable(true);
        int delayMs = LAN_RESULTS_CLOSE_DELAY_MS;
        if (lanIsHost && lanHost != null) {
            lanHost.broadcastResultsAction(action, delayMs);
        }
        if (lanResultsStatusLabel != null) {
            lanResultsStatusLabel.setText(action == LAN_RESULTS_ACTION_LOBBY
                    ? "Host is returning to lobby..."
                    : "Host is exiting to hub...");
        }
        PauseTransition closeDelay = new PauseTransition(Duration.millis(delayMs));
        closeDelay.setOnFinished(e -> performLanResultsAction(action, stage));
        closeDelay.play();
    }

    private void performLanResultsAction(int action, Stage stage) {
        lanResultsActionPending = false;
        lanResultsStatusLabel = null;
        if (stage == null) return;
        if (action == LAN_RESULTS_ACTION_LOBBY) {
            returnToLanLobby(stage);
        } else if (action == LAN_RESULTS_ACTION_EXIT) {
            stopLanSession();
            showMenu(stage);
        }
    }

    void onLanResultsAction(int action, int delayMs) {
        if (!lanModeActive || !lanIsClient) return;
        javafx.application.Platform.runLater(() -> {
            lanResultsActionPending = true;
            if (lanResultsStatusLabel != null) {
                lanResultsStatusLabel.setText(action == LAN_RESULTS_ACTION_LOBBY
                        ? "Host is returning to lobby..."
                        : "Host is exiting to hub...");
            }
            int delay = Math.max(0, delayMs);
            PauseTransition pause = new PauseTransition(Duration.millis(delay));
            pause.setOnFinished(e -> performLanResultsAction(action, currentStage));
            pause.play();
        });
    }

    void onLanDisconnected(String reason) {
        if (!lanModeActive || !lanIsClient) return;
        javafx.application.Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION, reason == null ? "Disconnected." : reason, ButtonType.OK);
            alert.setTitle("LAN Disconnected");
            alert.setHeaderText("Connection closed.");
            alert.showAndWait();
            stopLanSession();
            if (currentStage != null) {
                showLanMenu(currentStage);
            }
        });
    }

    private void startLanMatchHost(Stage stage) {
        if (!lanIsHost) return;
        if (countLanConnected() < 2) return;
        stopLanCountdown();
        resetMatchStats();
        storyModeActive = false;
        storyReplayMode = false;
        adventureModeActive = false;
        adventureReplayMode = false;
        currentAdventureBattle = null;
        trainingModeActive = false;
        classicModeActive = false;
        classicEncounter = null;
        competitionModeEnabled = false;
        mutatorModeEnabled = false;
        teamModeEnabled = false;
        MapType mapToPlay = pickLanMapForMatch();
        lanSelectedMap = mapToPlay;
        lanSelectedMapRandom = false;
        selectedMap = mapToPlay;
        activePlayers = LAN_MAX_PLAYERS;
        lanMatchActive = true;
        lastLanSnapshotNs = 0L;
        List<BirdType> pool = unlockedBirdPool();
        for (int i = 0; i < LAN_MAX_PLAYERS; i++) {
            if (!lanSlotConnected[i]) continue;
            if ((lanRandomBirds[i] || lanSelectedBirds[i] == null) && !pool.isEmpty()) {
                lanSelectedBirds[i] = pool.get(random.nextInt(pool.size()));
                lanRandomBirds[i] = false;
                lanSelectedSkinKeys[i] = null;
            }
        }
        lanMatchSeed = System.nanoTime();
        if (lanHost != null) {
            lanHost.broadcastStart(mapToPlay, lanMatchSeed, lanSlotConnected, lanSelectedBirds, lanSelectedSkinKeys);
        }
        startMatch(stage);
    }

    private void startLanMatchClient(Stage stage, MapType map, long seed, boolean[] connected, BirdType[] birds, String[] skinKeys) {
        if (!lanModeActive || !lanIsClient) return;
        if (stage == null) return;
        lanCountdownValue = 0;
        updateLanCountdownLabel();
        resetMatchStats();
        storyModeActive = false;
        storyReplayMode = false;
        adventureModeActive = false;
        adventureReplayMode = false;
        currentAdventureBattle = null;
        trainingModeActive = false;
        classicModeActive = false;
        classicEncounter = null;
        competitionModeEnabled = false;
        mutatorModeEnabled = false;
        teamModeEnabled = false;
        lanMatchSeed = seed;
        lanSelectedMap = map;
        selectedMap = map;
        if (connected != null) {
            System.arraycopy(connected, 0, lanSlotConnected, 0, Math.min(connected.length, LAN_MAX_PLAYERS));
        }
        if (birds != null) {
            System.arraycopy(birds, 0, lanSelectedBirds, 0, Math.min(birds.length, LAN_MAX_PLAYERS));
        }
        if (skinKeys != null) {
            System.arraycopy(skinKeys, 0, lanSelectedSkinKeys, 0, Math.min(skinKeys.length, LAN_MAX_PLAYERS));
        }
        for (int i = 0; i < LAN_MAX_PLAYERS; i++) {
            if (lanSlotConnected[i] && lanSelectedBirds[i] == null) {
                lanSelectedBirds[i] = firstUnlockedBird();
            }
            if (lanSlotConnected[i]) {
                lanRandomBirds[i] = false;
            }
        }
        activePlayers = LAN_MAX_PLAYERS;
        lanMatchActive = true;
        lanLocalInputMask = 0;
        if (lanClient != null) {
            lanClient.sendInputMask(0);
        }
        LanState pending = pendingLanState;
        pendingLanState = null;
        startMatch(stage);
        if (pending != null) {
            applyLanState(pending);
        }
    }

    private void returnToLanLobby(Stage stage) {
        if (stage == null) return;
        pressedKeys.clear();
        lanMatchActive = false;
        pendingLanState = null;
        lanResultsActionPending = false;
        lanResultsStatusLabel = null;
        stopLanCountdown();
        synchronized (lanInputLock) {
            Arrays.fill(lanInputMasks, 0);
            Arrays.fill(lanLastInputMasks, 0);
        }
        lanLocalInputMask = 0;
        if (lanIsHost) {
            Arrays.fill(lanReady, false);
        } else if (lanPlayerIndex >= 0) {
            lanReady[lanPlayerIndex] = false;
            if (lanClient != null) {
                lanClient.sendReady(false);
            }
        }
        resetMatchStats();
        showLanLobby(stage);
        if (lanIsHost) {
            broadcastLanLobby();
        }
    }

    private void stopLanSession() {
        lanModeActive = false;
        lanIsHost = false;
        lanIsClient = false;
        lanMatchActive = false;
        lanPlayerIndex = -1;
        lanResultsActionPending = false;
        lanResultsStatusLabel = null;
        stopLanCountdown();
        if (lanHost != null) {
            lanHost.stop();
            lanHost = null;
        }
        if (lanClient != null) {
            lanClient.disconnect();
            lanClient = null;
        }
        Arrays.fill(lanSlotConnected, false);
        Arrays.fill(lanSelectedBirds, null);
        Arrays.fill(lanSelectedSkinKeys, null);
        Arrays.fill(lanRandomBirds, false);
        Arrays.fill(lanReady, false);
        Arrays.fill(lanMapVotes, null);
        Arrays.fill(lanMapVoteRandom, false);
        synchronized (lanInputLock) {
            Arrays.fill(lanInputMasks, 0);
            Arrays.fill(lanLastInputMasks, 0);
        }
        lanLocalInputMask = 0;
        lastLanSnapshotNs = 0L;
        lanSelectedMapRandom = false;
        lanVoteSignature = 0;
    }

    private void confirmLeaveLanMatch(Stage stage) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION,
                "Leave the match?",
                ButtonType.YES,
                ButtonType.NO);
        alert.setTitle("Leave Match");
        alert.setHeaderText("This will disconnect you from the LAN session.");
        alert.initOwner(stage);
        alert.showAndWait().ifPresent(choice -> {
            if (choice == ButtonType.YES) {
                stopLanSession();
                showLanMenu(stage);
            }
        });
    }

    private void confirmLeaveLanSession(Stage stage, Runnable onConfirm) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION,
                "Leave LAN session?",
                ButtonType.YES,
                ButtonType.NO);
        alert.setTitle("Leave LAN");
        alert.setHeaderText("This will disconnect you from the LAN session.");
        alert.initOwner(stage);
        alert.showAndWait().ifPresent(choice -> {
            if (choice == ButtonType.YES && onConfirm != null) {
                onConfirm.run();
            }
        });
    }

    private LanState buildLanState() {
        LanState state = new LanState();
        state.matchTimer = matchTimer;
        state.matchEnded = matchEnded;
        state.activePlayers = activePlayers;
        state.camX = camX;
        state.camY = camY;
        state.zoom = zoom;
        state.shakeIntensity = shakeIntensity;
        state.hitstopFrames = hitstopFrames;
        state.scores = Arrays.copyOf(scores, scores.length);
        state.killFeed = new ArrayList<>(killFeed);
        for (int i = 0; i < LAN_MAX_PLAYERS; i++) {
            Bird b = players[i];
            if (b != null) {
                state.birds[i] = b.toLanState();
            }
        }
        for (PowerUp p : powerUps) {
            LanState.PowerUpState ps = new LanState.PowerUpState();
            ps.x = p.x;
            ps.y = p.y;
            ps.typeOrdinal = p.type.ordinal();
            state.powerUps.add(ps);
        }
        for (NectarNode n : nectarNodes) {
            LanState.NectarNodeState ns = new LanState.NectarNodeState();
            ns.x = n.x;
            ns.y = n.y;
            ns.isSpeed = n.isSpeed;
            ns.active = n.active;
            state.nectarNodes.add(ns);
        }
        for (SwingingVine v : swingingVines) {
            LanState.SwingingVineState vs = new LanState.SwingingVineState();
            vs.baseX = v.baseX;
            vs.baseY = v.baseY;
            vs.length = v.length;
            vs.angle = v.angle;
            vs.angularVelocity = v.angularVelocity;
            state.swingingVines.add(vs);
        }
        for (WindVent v : windVents) {
            LanState.WindVentState ws = new LanState.WindVentState();
            ws.x = v.x;
            ws.y = v.y;
            ws.w = v.w;
            ws.cooldown = v.cooldown;
            state.windVents.add(ws);
        }
        for (CrowMinion c : crowMinions) {
            LanState.CrowMinionState cs = new LanState.CrowMinionState();
            cs.x = c.x;
            cs.y = c.y;
            cs.age = c.age;
            cs.ownerIndex = c.owner != null ? c.owner.playerIndex : -1;
            state.crowMinions.add(cs);
        }
        return state;
    }

    private void applyLanState(LanState state) {
        if (state == null) return;
        matchTimer = state.matchTimer;
        matchEnded = state.matchEnded;
        activePlayers = state.activePlayers > 0 ? state.activePlayers : LAN_MAX_PLAYERS;
        camX = state.camX;
        camY = state.camY;
        zoom = state.zoom;
        shakeIntensity = state.shakeIntensity;
        hitstopFrames = state.hitstopFrames;
        if (state.scores != null) {
            int len = Math.min(scores.length, state.scores.length);
            System.arraycopy(state.scores, 0, scores, 0, len);
        }
        killFeed.clear();
        if (state.killFeed != null) {
            killFeed.addAll(state.killFeed);
        }
        for (int i = 0; i < LAN_MAX_PLAYERS; i++) {
            LanBirdState bs = state.birds[i];
            if (bs == null) {
                players[i] = null;
                continue;
            }
            BirdType type = BirdType.values()[Math.max(0, Math.min(bs.typeOrdinal, BirdType.values().length - 1))];
            Bird b = players[i];
            if (b == null || b.type != type) {
                b = new Bird(0, type, i, this);
                players[i] = b;
            }
            isAI[i] = false;
            b.applyLanState(bs);
        }

        powerUps.clear();
        for (LanState.PowerUpState ps : state.powerUps) {
            PowerUpType type = PowerUpType.values()[Math.max(0, Math.min(ps.typeOrdinal, PowerUpType.values().length - 1))];
            powerUps.add(new PowerUp(ps.x, ps.y, type));
        }

        nectarNodes.clear();
        for (LanState.NectarNodeState ns : state.nectarNodes) {
            NectarNode node = new NectarNode(ns.x, ns.y, ns.isSpeed);
            node.active = ns.active;
            nectarNodes.add(node);
        }

        swingingVines.clear();
        for (LanState.SwingingVineState vs : state.swingingVines) {
            SwingingVine vine = new SwingingVine(vs.baseX, vs.baseY, vs.length);
            vine.angle = vs.angle;
            vine.angularVelocity = vs.angularVelocity;
            vine.updatePlatformPosition();
            swingingVines.add(vine);
        }

        windVents.clear();
        for (LanState.WindVentState ws : state.windVents) {
            WindVent vent = new WindVent(ws.x, ws.y, ws.w);
            vent.cooldown = ws.cooldown;
            windVents.add(vent);
        }

        crowMinions.clear();
        for (LanState.CrowMinionState cs : state.crowMinions) {
            CrowMinion c = new CrowMinion(cs.x, cs.y, null);
            c.age = cs.age;
            if (cs.ownerIndex >= 0 && cs.ownerIndex < players.length) {
                c.owner = players[cs.ownerIndex];
            }
            crowMinions.add(c);
        }
    }

    private void applyLanInputMasks() {
        synchronized (lanInputLock) {
            for (int i = 1; i < LAN_MAX_PLAYERS; i++) {
                int mask = lanSlotConnected[i] ? lanInputMasks[i] : 0;
                updatePressedKeysForPlayer(i, mask);
                int last = lanLastInputMasks[i];
                if ((mask & LanProtocol.INPUT_LEFT) != 0 && (last & LanProtocol.INPUT_LEFT) == 0) {
                    if (players[i] != null) players[i].registerDashTap(-1);
                }
                if ((mask & LanProtocol.INPUT_RIGHT) != 0 && (last & LanProtocol.INPUT_RIGHT) == 0) {
                    if (players[i] != null) players[i].registerDashTap(1);
                }
                lanLastInputMasks[i] = mask;
            }
        }
    }

    private void updatePressedKeysForPlayer(int idx, int mask) {
        setPressedKey(leftKeyForPlayer(idx), (mask & LanProtocol.INPUT_LEFT) != 0);
        setPressedKey(rightKeyForPlayer(idx), (mask & LanProtocol.INPUT_RIGHT) != 0);
        setPressedKey(jumpKeyForPlayer(idx), (mask & LanProtocol.INPUT_JUMP) != 0);
        setPressedKey(attackKeyForPlayer(idx), (mask & LanProtocol.INPUT_ATTACK) != 0);
        setPressedKey(specialKeyForPlayer(idx), (mask & LanProtocol.INPUT_SPECIAL) != 0);
        setPressedKey(blockKeyForPlayer(idx), (mask & LanProtocol.INPUT_BLOCK) != 0);
        setPressedKey(tauntCycleKeyForPlayer(idx), (mask & LanProtocol.INPUT_TAUNT_CYCLE) != 0);
        setPressedKey(tauntExecuteKeyForPlayer(idx), (mask & LanProtocol.INPUT_TAUNT_EXEC) != 0);
    }

    private void setPressedKey(KeyCode key, boolean down) {
        if (key == null) return;
        if (down) {
            pressedKeys.add(key);
        } else {
            pressedKeys.remove(key);
        }
    }

    private void handleLanKeyPress(Stage stage, KeyEvent e) {
        KeyCode code = e.getCode();
        if (code == KeyCode.F11) {
            stage.setFullScreen(!stage.isFullScreen());
            return;
        }
        if (code == KeyCode.F3) {
            debugHudEnabled = !debugHudEnabled;
            addToKillFeed(debugHudEnabled ? "DEBUG HUD: ON" : "DEBUG HUD: OFF");
            return;
        }
        if (code == KeyCode.ESCAPE) {
            confirmLeaveLanMatch(stage);
            return;
        }
        if (lanPlayerIndex < 0) return;
        int bit = inputBitForKey(code, lanPlayerIndex);
        if (bit == 0) {
            bit = inputBitForKey(code, 0);
        }
        if (bit == 0) return;
        int next = lanLocalInputMask | bit;
        if (next != lanLocalInputMask) {
            lanLocalInputMask = next;
            if (lanClient != null) {
                lanClient.sendInputMask(lanLocalInputMask);
            }
        }
    }

    private void handleLanKeyRelease(KeyEvent e) {
        if (lanPlayerIndex < 0) return;
        KeyCode code = e.getCode();
        int bit = inputBitForKey(code, lanPlayerIndex);
        if (bit == 0) {
            bit = inputBitForKey(code, 0);
        }
        if (bit == 0) return;
        int next = lanLocalInputMask & ~bit;
        if (next != lanLocalInputMask) {
            lanLocalInputMask = next;
            if (lanClient != null) {
                lanClient.sendInputMask(lanLocalInputMask);
            }
        }
    }

    private int shopPreviewValueSum(List<ShopPreview> previews) {
        int sum = 0;
        for (ShopPreview preview : previews) {
            sum += preview.value;
        }
        return sum;
    }

    private boolean isShopPreviewOwned(ShopPreview preview) {
        if (preview == null || preview.skinKey == null) return false;
        String key = preview.skinKey;
        if (CLASSIC_CONTINUE_KEY.equals(key)) return false;
        if (CHAR_BAT_KEY.equals(key)) return batUnlocked;
        if (CHAR_FALCON_KEY.equals(key)) return falconUnlocked;
        if (CHAR_HEISENBIRD_KEY.equals(key)) return heisenbirdUnlocked;
        if (CHAR_PHOENIX_KEY.equals(key)) return phoenixUnlocked;
        if (CHAR_TITMOUSE_KEY.equals(key)) return titmouseUnlocked;
        if (CHAR_RAVEN_KEY.equals(key)) return ravenUnlocked;
        if (MAP_CAVE_KEY.equals(key)) return caveMapUnlocked;
        if (MAP_BATTLEFIELD_KEY.equals(key)) return battlefieldMapUnlocked;
        if ("CITY_PIGEON".equals(key)) return cityPigeonUnlocked;
        if ("NOIR_PIGEON".equals(key)) return noirPigeonUnlocked;
        if ("SKY_KING_EAGLE".equals(key)) return eagleSkinUnlocked;
        if (NOVA_PHOENIX_SKIN.equals(key)) return novaPhoenixUnlocked;
        if (DUNE_FALCON_SKIN.equals(key)) return duneFalconUnlocked;
        if (MINT_PENGUIN_SKIN.equals(key)) return mintPenguinUnlocked;
        if (CIRCUIT_TITMOUSE_SKIN.equals(key)) return circuitTitmouseUnlocked;
        if (PRISM_RAZORBILL_SKIN.equals(key)) return prismRazorbillUnlocked;
        if (AURORA_PELICAN_SKIN.equals(key)) return auroraPelicanUnlocked;
        if (SUNFLARE_HUMMINGBIRD_SKIN.equals(key)) return sunflareHummingbirdUnlocked;
        if (GLACIER_SHOEBILL_SKIN.equals(key)) return glacierShoebillUnlocked;
        if (TIDE_VULTURE_SKIN.equals(key)) return tideVultureUnlocked;
        if (ECLIPSE_MOCKINGBIRD_SKIN.equals(key)) return eclipseMockingbirdUnlocked;
        if (UMBRA_BAT_SKIN.equals(key)) return umbraBatUnlocked;
        if (key.startsWith("CLASSIC_SKIN_")) return isClassicRewardUnlocked(preview.type);
        return false;
    }

    private void unlockShopPreview(ShopPreview preview) {
        if (preview == null || preview.skinKey == null) return;
        String key = preview.skinKey;
        if (CLASSIC_CONTINUE_KEY.equals(key)) {
            classicContinues++;
            return;
        }
        if (isShopPreviewOwned(preview)) {
            return;
        }
        if (CHAR_BAT_KEY.equals(key)) {
            batUnlocked = true;
            adventureUnlocked[BirdType.BAT.ordinal()] = true;
            queueUnlockCardForBird(BirdType.BAT);
            return;
        }
        if (CHAR_FALCON_KEY.equals(key)) {
            falconUnlocked = true;
            adventureUnlocked[BirdType.FALCON.ordinal()] = true;
            queueUnlockCardForBird(BirdType.FALCON);
            return;
        }
        if (CHAR_HEISENBIRD_KEY.equals(key)) {
            heisenbirdUnlocked = true;
            adventureUnlocked[BirdType.HEISENBIRD.ordinal()] = true;
            queueUnlockCardForBird(BirdType.HEISENBIRD);
            return;
        }
        if (CHAR_PHOENIX_KEY.equals(key)) {
            phoenixUnlocked = true;
            adventureUnlocked[BirdType.PHOENIX.ordinal()] = true;
            queueUnlockCardForBird(BirdType.PHOENIX);
            return;
        }
        if (CHAR_TITMOUSE_KEY.equals(key)) {
            titmouseUnlocked = true;
            adventureUnlocked[BirdType.TITMOUSE.ordinal()] = true;
            queueUnlockCardForBird(BirdType.TITMOUSE);
            return;
        }
        if (CHAR_RAVEN_KEY.equals(key)) {
            ravenUnlocked = true;
            adventureUnlocked[BirdType.RAVEN.ordinal()] = true;
            queueUnlockCardForBird(BirdType.RAVEN);
            return;
        }
        if (MAP_CAVE_KEY.equals(key)) {
            caveMapUnlocked = true;
            queueUnlockCardForMap(MapType.CAVE);
            return;
        }
        if (MAP_BATTLEFIELD_KEY.equals(key)) {
            battlefieldMapUnlocked = true;
            queueUnlockCardForMap(MapType.BATTLEFIELD);
            return;
        }
        if ("CITY_PIGEON".equals(key)) {
            cityPigeonUnlocked = true;
            queueUnlockCardForSkin(BirdType.PIGEON, "CITY_PIGEON");
            return;
        }
        if ("NOIR_PIGEON".equals(key)) {
            unlockClassicReward(BirdType.PIGEON);
            return;
        }
        if ("SKY_KING_EAGLE".equals(key)) {
            unlockClassicReward(BirdType.EAGLE);
            return;
        }
        if (NOVA_PHOENIX_SKIN.equals(key)) {
            novaPhoenixUnlocked = true;
            queueUnlockCardForSkin(BirdType.PHOENIX, NOVA_PHOENIX_SKIN);
            return;
        }
        if (DUNE_FALCON_SKIN.equals(key)) {
            duneFalconUnlocked = true;
            queueUnlockCardForSkin(BirdType.FALCON, DUNE_FALCON_SKIN);
            return;
        }
        if (MINT_PENGUIN_SKIN.equals(key)) {
            mintPenguinUnlocked = true;
            queueUnlockCardForSkin(BirdType.PENGUIN, MINT_PENGUIN_SKIN);
            return;
        }
        if (CIRCUIT_TITMOUSE_SKIN.equals(key)) {
            circuitTitmouseUnlocked = true;
            queueUnlockCardForSkin(BirdType.TITMOUSE, CIRCUIT_TITMOUSE_SKIN);
            return;
        }
        if (PRISM_RAZORBILL_SKIN.equals(key)) {
            prismRazorbillUnlocked = true;
            queueUnlockCardForSkin(BirdType.RAZORBILL, PRISM_RAZORBILL_SKIN);
            return;
        }
        if (AURORA_PELICAN_SKIN.equals(key)) {
            auroraPelicanUnlocked = true;
            queueUnlockCardForSkin(BirdType.PELICAN, AURORA_PELICAN_SKIN);
            return;
        }
        if (SUNFLARE_HUMMINGBIRD_SKIN.equals(key)) {
            sunflareHummingbirdUnlocked = true;
            queueUnlockCardForSkin(BirdType.HUMMINGBIRD, SUNFLARE_HUMMINGBIRD_SKIN);
            return;
        }
        if (GLACIER_SHOEBILL_SKIN.equals(key)) {
            glacierShoebillUnlocked = true;
            queueUnlockCardForSkin(BirdType.SHOEBILL, GLACIER_SHOEBILL_SKIN);
            return;
        }
        if (TIDE_VULTURE_SKIN.equals(key)) {
            tideVultureUnlocked = true;
            queueUnlockCardForSkin(BirdType.VULTURE, TIDE_VULTURE_SKIN);
            return;
        }
        if (ECLIPSE_MOCKINGBIRD_SKIN.equals(key)) {
            eclipseMockingbirdUnlocked = true;
            queueUnlockCardForSkin(BirdType.MOCKINGBIRD, ECLIPSE_MOCKINGBIRD_SKIN);
            return;
        }
        if (UMBRA_BAT_SKIN.equals(key)) {
            umbraBatUnlocked = true;
            queueUnlockCardForSkin(BirdType.BAT, UMBRA_BAT_SKIN);
            return;
        }
        if (key.startsWith("CLASSIC_SKIN_")) {
            unlockClassicReward(preview.type);
        }
    }

    private boolean areAllPreviewsOwned(List<ShopPreview> previews) {
        for (ShopPreview preview : previews) {
            if (!isShopPreviewOwned(preview)) return false;
        }
        return true;
    }

    private String shopPreviewName(ShopPreview preview) {
        if (preview == null) return "";
        if (preview.label != null && !preview.label.isBlank()) return preview.label;
        String key = preview.skinKey;
        if (CLASSIC_CONTINUE_KEY.equals(key)) return "Classic Continue";
        if (CHAR_BAT_KEY.equals(key)) return "Bat";
        if (CHAR_FALCON_KEY.equals(key)) return "Falcon";
        if (CHAR_HEISENBIRD_KEY.equals(key)) return "Heisenbird";
        if (CHAR_PHOENIX_KEY.equals(key)) return "Phoenix";
        if (CHAR_TITMOUSE_KEY.equals(key)) return "Titmouse";
        if (CHAR_RAVEN_KEY.equals(key)) return "Raven";
        if (MAP_CAVE_KEY.equals(key)) return "Echo Cavern Map";
        if (MAP_BATTLEFIELD_KEY.equals(key)) return "Battlefield Map";
        if ("CITY_PIGEON".equals(key)) return "City Pigeon";
        if ("NOIR_PIGEON".equals(key)) return "Noir Pigeon";
        if (BEACON_PIGEON_SKIN.equals(key)) return "Beacon Pigeon";
        if ("SKY_KING_EAGLE".equals(key)) return "Sky King Eagle";
        if (NOVA_PHOENIX_SKIN.equals(key)) return "Nova Phoenix";
        if (DUNE_FALCON_SKIN.equals(key)) return "Dune Falcon";
        if (MINT_PENGUIN_SKIN.equals(key)) return "Mint Penguin";
        if (CIRCUIT_TITMOUSE_SKIN.equals(key)) return "Circuit Titmouse";
        if (PRISM_RAZORBILL_SKIN.equals(key)) return "Prism Razorbill";
        if (AURORA_PELICAN_SKIN.equals(key)) return "Aurora Pelican";
        if (SUNFLARE_HUMMINGBIRD_SKIN.equals(key)) return "Sunflare Hummingbird";
        if (GLACIER_SHOEBILL_SKIN.equals(key)) return "Glacier Shoebill";
        if (TIDE_VULTURE_SKIN.equals(key)) return "Tide Vulture";
        if (ECLIPSE_MOCKINGBIRD_SKIN.equals(key)) return "Eclipse Charles";
        if (UMBRA_BAT_SKIN.equals(key)) return "Umbra Bat";
        if (key != null && key.startsWith("CLASSIC_SKIN_") && preview.type != null) {
            return classicRewardFor(preview.type);
        }
        if (preview.type != null && preview.skinKey != null && !isCharacterKey(preview.skinKey)) {
            return skinDisplayName(preview.skinKey, preview.type);
        }
        return preview.type != null ? preview.type.name : "Skin";
    }

    private boolean areAllBirdsUnlocked() {
        return batUnlocked && falconUnlocked && heisenbirdUnlocked && phoenixUnlocked && titmouseUnlocked && ravenUnlocked;
    }

    private PackReward coinReward(int amount, int weight) {
        return new PackReward("Bird Coins +" + amount, weight, () -> true, () -> birdCoins += amount);
    }

    private PackReward continueReward(int amount, int weight) {
        return new PackReward("Classic Continue +" + amount, weight, () -> true, () -> classicContinues += amount);
    }

    private void addSkinRewards(List<PackReward> pool, List<ShopPreview> previews, int weightPerItem) {
        if (previews == null) return;
        for (ShopPreview preview : previews) {
            pool.add(new PackReward(
                    shopPreviewName(preview) + " Skin",
                    weightPerItem,
                    () -> !isShopPreviewOwned(preview),
                    () -> unlockShopPreview(preview)
            ));
        }
    }

    private void addBirdRewards(List<PackReward> pool, List<ShopPreview> previews, int weightPerItem) {
        if (previews == null) return;
        for (ShopPreview preview : previews) {
            pool.add(new PackReward(
                    shopPreviewName(preview) + " Character",
                    weightPerItem,
                    () -> !isShopPreviewOwned(preview),
                    () -> unlockShopPreview(preview)
            ));
        }
    }

    private void addMapRewards(List<PackReward> pool, List<ShopPreview> previews, int weightPerItem) {
        if (previews == null) return;
        for (ShopPreview preview : previews) {
            pool.add(new PackReward(
                    shopPreviewName(preview),
                    weightPerItem,
                    () -> !isShopPreviewOwned(preview),
                    () -> unlockShopPreview(preview)
            ));
        }
    }

    private PackReward pickPackReward(List<PackReward> pool) {
        int total = 0;
        List<PackReward> available = new ArrayList<>();
        for (PackReward reward : pool) {
            if (reward == null || reward.weight <= 0) continue;
            if (reward.available.getAsBoolean()) {
                available.add(reward);
                total += reward.weight;
            }
        }
        if (total <= 0) return null;
        int roll = random.nextInt(total);
        for (PackReward reward : available) {
            roll -= reward.weight;
            if (roll < 0) return reward;
        }
        return available.isEmpty() ? null : available.get(available.size() - 1);
    }

    private boolean isCoinReward(PackReward reward) {
        return reward != null && reward.label != null && reward.label.startsWith("Bird Coins");
    }

    private PackReward pickNonCoinReward(List<PackReward> pool) {
        int total = 0;
        List<PackReward> available = new ArrayList<>();
        for (PackReward reward : pool) {
            if (reward == null || reward.weight <= 0) continue;
            if (isCoinReward(reward)) continue;
            if (reward.available.getAsBoolean()) {
                available.add(reward);
                total += reward.weight;
            }
        }
        if (total <= 0) return null;
        int roll = random.nextInt(total);
        for (PackReward reward : available) {
            roll -= reward.weight;
            if (roll < 0) return reward;
        }
        return available.isEmpty() ? null : available.get(available.size() - 1);
    }

    private ShopPreview pickAvailablePreview(List<ShopPreview> pool) {
        if (pool == null || pool.isEmpty()) return null;
        List<ShopPreview> available = new ArrayList<>();
        for (ShopPreview preview : pool) {
            if (!isShopPreviewOwned(preview)) {
                available.add(preview);
            }
        }
        if (available.isEmpty()) return null;
        return available.get(random.nextInt(available.size()));
    }

    private ShopPackResult openCardPack(String packName, int pulls, List<PackReward> pool) {
        int safePulls = Math.max(1, pulls);
        List<String> lines = new ArrayList<>();
        int remaining = safePulls;
        PackReward guaranteed = pickNonCoinReward(pool);
        if (guaranteed != null) {
            guaranteed.grant.run();
            lines.add("- " + guaranteed.label);
            remaining--;
        }
        for (int i = 0; i < remaining; i++) {
            PackReward reward = pickPackReward(pool);
            if (reward == null) {
                reward = coinReward(150, 1);
            }
            reward.grant.run();
            lines.add("- " + reward.label);
        }
        return new ShopPackResult(packName + " Opened", lines);
    }

    private ShopPackResult openGuaranteedBirdPack(String packName, int pulls, List<ShopPreview> birdPool,
                                                  List<ShopPreview> legendarySkins, List<PackReward> extraPool) {
        int safePulls = Math.max(1, pulls);
        List<String> lines = new ArrayList<>();
        boolean preferLegendary = areAllBirdsUnlocked();
        ShopPreview primaryReward = null;
        String suffix = " Character";
        if (preferLegendary) {
            primaryReward = pickAvailablePreview(legendarySkins);
            if (primaryReward == null && legendarySkins != null && !legendarySkins.isEmpty()) {
                primaryReward = legendarySkins.get(random.nextInt(legendarySkins.size()));
            }
            suffix = " Skin";
        } else {
            primaryReward = pickAvailablePreview(birdPool);
            if (primaryReward == null) {
                primaryReward = pickAvailablePreview(legendarySkins);
                if (primaryReward == null && legendarySkins != null && !legendarySkins.isEmpty()) {
                    primaryReward = legendarySkins.get(random.nextInt(legendarySkins.size()));
                }
                suffix = " Skin";
            }
        }
        if (primaryReward != null) {
            unlockShopPreview(primaryReward);
            lines.add("- " + shopPreviewName(primaryReward) + suffix);
        } else {
            birdCoins += 300;
            lines.add("- Bird Coins +300");
        }
        int extraPulls = Math.max(0, safePulls - 1);
        boolean needsNonCoin = primaryReward == null;
        for (int i = 0; i < extraPulls; i++) {
            PackReward reward = needsNonCoin ? pickNonCoinReward(extraPool) : null;
            if (reward == null) {
                reward = pickPackReward(extraPool);
            }
            if (reward == null) {
                reward = coinReward(200, 1);
            }
            reward.grant.run();
            lines.add("- " + reward.label);
            if (!isCoinReward(reward)) {
                needsNonCoin = false;
            }
        }
        return new ShopPackResult(packName + " Opened", lines);
    }

    private List<ShopItem> buildShopItems() {
        List<ShopItem> items = new ArrayList<>();

        ShopPreview continuePreview = new ShopPreview(null, CLASSIC_CONTINUE_KEY, "Classic Continue +1");
        ShopPreview coins100 = new ShopPreview(null, null, "Bird Coins +100");
        ShopPreview coins160 = new ShopPreview(null, null, "Bird Coins +160");
        ShopPreview coins180 = new ShopPreview(null, null, "Bird Coins +180");
        ShopPreview coins260 = new ShopPreview(null, null, "Bird Coins +260");
        ShopPreview coins420 = new ShopPreview(null, null, "Bird Coins +420");
        ShopPreview coins600 = new ShopPreview(null, null, "Bird Coins +600");
        ShopPreview coins700 = new ShopPreview(null, null, "Bird Coins +700");
        ShopPreview coins900 = new ShopPreview(null, null, "Bird Coins +900");
        List<ShopPreview> birdRewards = List.of(
                new ShopPreview(BirdType.BAT, CHAR_BAT_KEY, "Bat"),
                new ShopPreview(BirdType.FALCON, CHAR_FALCON_KEY, "Falcon"),
                new ShopPreview(BirdType.HEISENBIRD, CHAR_HEISENBIRD_KEY, "Heisenbird"),
                new ShopPreview(BirdType.PHOENIX, CHAR_PHOENIX_KEY, "Phoenix"),
                new ShopPreview(BirdType.TITMOUSE, CHAR_TITMOUSE_KEY, "Titmouse"),
                new ShopPreview(BirdType.RAVEN, CHAR_RAVEN_KEY, "Raven")
        );
        List<ShopPreview> mapRewards = List.of(
                new ShopPreview(null, MAP_CAVE_KEY, "Echo Cavern Map"),
                new ShopPreview(null, MAP_BATTLEFIELD_KEY, "Battlefield Map")
        );

        ShopPreview dunePreview = new ShopPreview(BirdType.FALCON, DUNE_FALCON_SKIN, null);
        ShopPreview mintPreview = new ShopPreview(BirdType.PENGUIN, MINT_PENGUIN_SKIN, null);
        ShopPreview cityPreview = new ShopPreview(BirdType.PIGEON, "CITY_PIGEON", null);
        ShopPreview circuitPreview = new ShopPreview(BirdType.TITMOUSE, CIRCUIT_TITMOUSE_SKIN, null);
        ShopPreview noirPreview = new ShopPreview(BirdType.PIGEON, "NOIR_PIGEON", null);
        ShopPreview skyPreview = new ShopPreview(BirdType.EAGLE, "SKY_KING_EAGLE", null);
        ShopPreview prismPreview = new ShopPreview(BirdType.RAZORBILL, PRISM_RAZORBILL_SKIN, null);
        ShopPreview novaPreview = new ShopPreview(BirdType.PHOENIX, NOVA_PHOENIX_SKIN, null);
        ShopPreview auroraPreview = new ShopPreview(BirdType.PELICAN, AURORA_PELICAN_SKIN, null);
        ShopPreview sunflarePreview = new ShopPreview(BirdType.HUMMINGBIRD, SUNFLARE_HUMMINGBIRD_SKIN, null);
        ShopPreview glacierPreview = new ShopPreview(BirdType.SHOEBILL, GLACIER_SHOEBILL_SKIN, null);
        ShopPreview tidePreview = new ShopPreview(BirdType.VULTURE, TIDE_VULTURE_SKIN, null);
        ShopPreview eclipsePreview = new ShopPreview(BirdType.MOCKINGBIRD, ECLIPSE_MOCKINGBIRD_SKIN, null);
        ShopPreview umbraPreview = new ShopPreview(BirdType.BAT, UMBRA_BAT_SKIN, null);

        List<ShopPreview> commonSkins = List.of(dunePreview, sunflarePreview);
        List<ShopPreview> uncommonSkins = List.of(mintPreview, glacierPreview);
        List<ShopPreview> rareSkins = List.of(cityPreview, circuitPreview, tidePreview);
        List<ShopPreview> epicSkins = List.of(noirPreview, skyPreview, prismPreview, eclipsePreview);
        List<ShopPreview> legendarySkins = List.of(novaPreview, auroraPreview, umbraPreview);

        List<ShopPreview> classicSkins = new ArrayList<>();
        for (BirdType type : BirdType.values()) {
            if (type == BirdType.PIGEON || type == BirdType.EAGLE) continue;
            classicSkins.add(new ShopPreview(type, classicSkinDataKey(type), classicRewardFor(type)));
        }

        List<ShopPreview> streetPreviews = new ArrayList<>();
        streetPreviews.addAll(commonSkins);
        streetPreviews.addAll(uncommonSkins);
        streetPreviews.add(continuePreview);
        streetPreviews.add(coins100);
        streetPreviews.add(coins160);

        List<ShopPreview> rooftopPreviews = new ArrayList<>();
        rooftopPreviews.addAll(birdRewards);
        rooftopPreviews.addAll(rareSkins);
        rooftopPreviews.addAll(uncommonSkins);
        rooftopPreviews.addAll(commonSkins);
        rooftopPreviews.addAll(mapRewards);
        rooftopPreviews.add(continuePreview);
        rooftopPreviews.add(coins180);
        rooftopPreviews.add(coins260);

        List<ShopPreview> skylinePreviews = new ArrayList<>();
        skylinePreviews.addAll(birdRewards);
        skylinePreviews.addAll(epicSkins);
        skylinePreviews.addAll(rareSkins);
        skylinePreviews.addAll(classicSkins);
        skylinePreviews.addAll(mapRewards);
        skylinePreviews.add(continuePreview);
        skylinePreviews.add(coins260);
        skylinePreviews.add(coins420);

        List<ShopPreview> nebulaPreviews = new ArrayList<>();
        nebulaPreviews.addAll(birdRewards);
        nebulaPreviews.addAll(legendarySkins);
        nebulaPreviews.addAll(epicSkins);
        nebulaPreviews.addAll(classicSkins);
        nebulaPreviews.addAll(mapRewards);
        nebulaPreviews.add(continuePreview);
        nebulaPreviews.add(coins420);
        nebulaPreviews.add(coins700);

        List<ShopPreview> ascendantPreviews = new ArrayList<>();
        ascendantPreviews.addAll(birdRewards);
        ascendantPreviews.addAll(legendarySkins);
        ascendantPreviews.addAll(epicSkins);
        ascendantPreviews.addAll(classicSkins);
        ascendantPreviews.addAll(mapRewards);
        ascendantPreviews.add(continuePreview);
        ascendantPreviews.add(coins600);
        ascendantPreviews.add(coins900);

        List<PackReward> streetPool = new ArrayList<>();
        streetPool.add(coinReward(100, 24));
        streetPool.add(coinReward(160, 18));
        addSkinRewards(streetPool, commonSkins, 16);
        addSkinRewards(streetPool, uncommonSkins, 10);
        streetPool.add(continueReward(1, 6));

        List<PackReward> rooftopPool = new ArrayList<>();
        rooftopPool.add(coinReward(180, 22));
        rooftopPool.add(coinReward(260, 14));
        addSkinRewards(rooftopPool, commonSkins, 12);
        addSkinRewards(rooftopPool, uncommonSkins, 10);
        addSkinRewards(rooftopPool, rareSkins, 8);
        addBirdRewards(rooftopPool, birdRewards, 6);
        addMapRewards(rooftopPool, mapRewards, 4);
        rooftopPool.add(continueReward(1, 8));

        List<PackReward> skylinePool = new ArrayList<>();
        skylinePool.add(coinReward(260, 18));
        skylinePool.add(coinReward(420, 12));
        addSkinRewards(skylinePool, rareSkins, 10);
        addSkinRewards(skylinePool, epicSkins, 8);
        addSkinRewards(skylinePool, classicSkins, 4);
        addBirdRewards(skylinePool, birdRewards, 8);
        addMapRewards(skylinePool, mapRewards, 6);
        skylinePool.add(continueReward(1, 10));

        List<PackReward> nebulaPool = new ArrayList<>();
        nebulaPool.add(coinReward(420, 12));
        nebulaPool.add(coinReward(700, 8));
        addSkinRewards(nebulaPool, epicSkins, 10);
        addSkinRewards(nebulaPool, legendarySkins, 8);
        addSkinRewards(nebulaPool, classicSkins, 5);
        addBirdRewards(nebulaPool, birdRewards, 10);
        addMapRewards(nebulaPool, mapRewards, 6);
        nebulaPool.add(continueReward(1, 12));

        List<PackReward> ascendantExtras = new ArrayList<>();
        ascendantExtras.add(coinReward(600, 10));
        ascendantExtras.add(coinReward(900, 6));
        addSkinRewards(ascendantExtras, epicSkins, 10);
        addSkinRewards(ascendantExtras, legendarySkins, 10);
        addSkinRewards(ascendantExtras, classicSkins, 6);
        addMapRewards(ascendantExtras, mapRewards, 8);
        ascendantExtras.add(continueReward(1, 12));

        items.add(new ShopItem(
                "Street Pack",
                "Cheap cards with coins, common skins, and the occasional Classic Continue.",
                550,
                streetPreviews,
                ShopRarity.COMMON,
                () -> openCardPack("Street Pack", 1, streetPool),
                () -> true
        ));

        items.add(new ShopItem(
                "Rooftop Pack",
                "Mid pack with wider skins plus character, map, and continue chances.",
                1100,
                rooftopPreviews,
                ShopRarity.UNCOMMON,
                () -> openCardPack("Rooftop Pack", 2, rooftopPool),
                () -> true
        ));

        items.add(new ShopItem(
                "Skyline Pack",
                "Rare pack with classic skins, higher character odds, maps, and continues.",
                2100,
                skylinePreviews,
                ShopRarity.RARE,
                () -> openCardPack("Skyline Pack", 2, skylinePool),
                () -> true
        ));

        items.add(new ShopItem(
                "Nebula Pack",
                "Epic pack loaded with high-tier skins, characters, maps, and continues.",
                3400,
                nebulaPreviews,
                ShopRarity.EPIC,
                () -> openCardPack("Nebula Pack", 3, nebulaPool),
                () -> true
        ));

        items.add(new ShopItem(
                "Ascendant Pack",
                "Legendary pack. Guaranteed bird (or legendary skin if all birds owned) plus elite rewards.",
                5700,
                ascendantPreviews,
                ShopRarity.LEGENDARY,
                () -> openGuaranteedBirdPack("Ascendant Pack", 3, birdRewards, legendarySkins, ascendantExtras),
                () -> true
        ));

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

        FlowPane list = new FlowPane(24, 24);
        list.setAlignment(Pos.TOP_CENTER);
        list.setPrefWrapLength(1400);
        list.setPadding(new Insets(10, 20, 20, 20));
        list.setStyle("-fx-background-color: transparent;");

        List<ShopItem> items = buildShopItems();
        items.sort(Comparator.comparing((ShopItem item) -> item.owned.getAsBoolean() || !item.available.getAsBoolean()));
        for (ShopItem item : items) {
            boolean owned = item.owned.getAsBoolean();
            boolean available = item.available.getAsBoolean();
            int previewCount = item.previews.size();
            double cardW = 420;
            double cardH = 300;
            if (item.bundle) {
                if (previewCount > 10) {
                    cardW = 980;
                    cardH = 640;
                } else if (previewCount > 4) {
                    cardW = 820;
                    cardH = 520;
                } else {
                    cardW = 700;
                    cardH = 440;
                }
            } else {
                switch (item.rarity) {
                    case COMMON -> {
                        cardW = 360;
                        cardH = 300;
                    }
                    case UNCOMMON -> {
                        cardW = 390;
                        cardH = 315;
                    }
                    case RARE -> {
                        cardW = 420;
                        cardH = 330;
                    }
                    case EPIC -> {
                        cardW = 480;
                        cardH = 350;
                    }
                    case LEGENDARY -> {
                        cardW = 560;
                        cardH = 380;
                    }
                    case BUNDLE -> {
                        cardW = 700;
                        cardH = 460;
                    }
                }
            }

            List<String> ownedNames = new ArrayList<>();
            int ownedCredits = 0;
            if (item.bundle) {
                for (ShopPreview preview : item.previews) {
                    if (isShopPreviewOwned(preview)) {
                        ownedCredits += preview.value;
                        ownedNames.add(shopPreviewName(preview));
                    }
                }
            }
            int effectiveCost = item.cost;
            if (item.bundle && !owned && ownedCredits > 0) {
                effectiveCost = Math.max(0, item.cost - ownedCredits);
            }

            VBox card = new VBox(12);
            card.setAlignment(Pos.TOP_CENTER);
            card.setPadding(new Insets(18));
            card.setPrefWidth(cardW);
            card.setMinWidth(cardW);
            card.setMaxWidth(cardW);
            card.setMinHeight(cardH);
            card.setPrefHeight(Region.USE_COMPUTED_SIZE);
            card.setMaxHeight(Double.MAX_VALUE);
            String border = toHex(item.rarity.color);
            card.setStyle("-fx-background-color: rgba(7,12,18,0.74); -fx-border-color: " + border + "; -fx-border-width: 2; -fx-border-radius: 18; -fx-background-radius: 18;");

            Label name = new Label(item.name);
            name.setFont(Font.font("Impact", 34));
            name.setTextFill(Color.web("#ECEFF1"));
            name.setAlignment(Pos.CENTER);
            name.setTextAlignment(TextAlignment.CENTER);
            name.setMaxWidth(cardW - 36);

            String rarityText = item.rarity.label + " PACK";
            Label rarity = new Label(rarityText);
            rarity.setFont(Font.font("Consolas", 18));
            rarity.setTextFill(item.rarity.color);
            rarity.setAlignment(Pos.CENTER);
            rarity.setTextAlignment(TextAlignment.CENTER);
            rarity.setStyle("-fx-background-color: rgba(0,0,0,0.5); -fx-padding: 4 12 4 12; -fx-background-radius: 12;");

            Label value = null;
            if (item.bundle) {
                value = new Label("VALUE: " + item.bundleValue + " BIRD COINS");
                value.setFont(Font.font("Consolas", 20));
                value.setTextFill(Color.web("#B0BEC5"));
                value.setAlignment(Pos.CENTER);
                value.setTextAlignment(TextAlignment.CENTER);
                value.setMaxWidth(cardW - 36);
            }

            String priceText = "PRICE: " + item.cost + " BIRD COINS";
            if (owned) {
                priceText += " (OWNED)";
            } else if (!available) {
                priceText += " (UNAVAILABLE)";
            }
            Label price = new Label(priceText);
            price.setFont(Font.font("Consolas", 22));
            if (owned) {
                price.setTextFill(Color.LIMEGREEN);
            } else if (!available) {
                price.setTextFill(Color.web("#FF8A65"));
            } else {
                price.setTextFill(Color.web("#FFD54F"));
            }
            price.setAlignment(Pos.CENTER);
            price.setTextAlignment(TextAlignment.CENTER);
            price.setMaxWidth(cardW - 36);

            Label discounted = null;
            if (item.bundle && !owned && ownedCredits > 0) {
                discounted = new Label("YOUR PRICE: " + effectiveCost + " BIRD COINS");
                discounted.setFont(Font.font("Consolas", 20));
                discounted.setTextFill(Color.web("#C5E1A5"));
                discounted.setAlignment(Pos.CENTER);
                discounted.setTextAlignment(TextAlignment.CENTER);
                discounted.setMaxWidth(cardW - 36);
            }

            Label ownedLabel = null;
            if (item.bundle && !ownedNames.isEmpty()) {
                String ownedText = "OWNED " + ownedNames.size() + "/" + previewCount + ": " + String.join(", ", ownedNames);
                ownedLabel = new Label(ownedText);
                ownedLabel.setFont(Font.font("Consolas", 16));
                ownedLabel.setTextFill(Color.web("#A5D6A7"));
                ownedLabel.setWrapText(true);
                ownedLabel.setAlignment(Pos.CENTER);
                ownedLabel.setTextAlignment(TextAlignment.CENTER);
                ownedLabel.setMaxWidth(cardW - 40);
            }

            double previewCardW = Math.max(260, Math.min(420, cardW - 60));
            Node previewCard = buildShopPreviewCarousel(item.previews, item.rarity.color, previewCardW);
            if (item.rarity == ShopRarity.LEGENDARY || item.rarity == ShopRarity.BUNDLE) {
                previewCard.setEffect(new DropShadow(22, item.rarity.color));
            }
            HBox previewWrap = new HBox(previewCard);
            previewWrap.setAlignment(Pos.CENTER);
            Node previewBox = previewWrap;

            Label desc = new Label(item.description);
            desc.setFont(Font.font("Consolas", 18));
            desc.setTextFill(Color.web("#B0BEC5"));
            desc.setWrapText(true);
            desc.setAlignment(Pos.CENTER);
            desc.setTextAlignment(TextAlignment.CENTER);
            desc.setMaxWidth(cardW - 36);

            final int purchaseCost = item.cost;
            String buyLabel = owned ? "OWNED" : (available ? "BUY" : "UNAVAILABLE");
            String buyColor = owned ? "#455A64" : (available ? "#00C853" : "#455A64");
            Button buy = uiFactory.action(buyLabel, 240, 64, 28, buyColor, 20, () -> {
                if (owned || !available) return;
                if (birdCoins < purchaseCost) {
                    playErrorSound();
                    return;
                }
                birdCoins -= purchaseCost;
                ShopPackResult result = item.purchase.get();
                saveAchievements();
                runAfterUnlockCards(stage, () -> {
                    if (result == null) {
                        showShop(stage);
                    } else {
                        showPackResult(stage, result);
                    }
                });
            });
            buy.setDisable(owned || !available);
            HBox row = new HBox(buy);
            row.setAlignment(Pos.CENTER);

            List<Node> parts = new ArrayList<>();
            parts.add(name);
            parts.add(rarity);
            if (value != null) parts.add(value);
            parts.add(price);
            if (discounted != null) parts.add(discounted);
            if (ownedLabel != null) parts.add(ownedLabel);
            parts.add(previewBox);
            parts.add(desc);
            parts.add(row);
            card.getChildren().addAll(parts);
            list.getChildren().add(card);
        }

        ScrollPane scroll = new ScrollPane(list);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background-color: transparent; -fx-background: transparent; -fx-control-inner-background: transparent;");
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);

        Button back = uiFactory.action("BACK TO HUB", 360, 90, 34, "#D32F2F", 22, () -> showMenu(stage));
        HBox bottom = new HBox(back);
        bottom.setAlignment(Pos.CENTER);

        root.setTop(top);
        root.setCenter(scroll);
        root.setBottom(bottom);

        Scene scene = new Scene(root, WIDTH, HEIGHT);
        bindEscape(scene, back);
        setupKeyboardNavigation(scene);
        applyConsoleHighlight(scene);
        setScenePreservingFullscreen(stage, scene);
        back.requestFocus();
    }

    private void showPackResult(Stage stage, ShopPackResult result) {
        if (result == null) {
            showShop(stage);
            return;
        }
        playMenuMusic();
        VBox root = MenuLayout.buildMenuRoot("-fx-background-color: linear-gradient(to bottom, #0B1D2B, #1A2E3F);",
                MENU_PADDING, 30);

        Label title = new Label(result.title);
        title.setFont(Font.font("Impact", FontWeight.BOLD, 84));
        title.setTextFill(Color.web("#FFE082"));

        VBox card = new VBox(18);
        card.setAlignment(Pos.CENTER_LEFT);
        card.setPadding(new Insets(32));
        card.setMaxWidth(1100);
        card.setStyle("-fx-background-color: rgba(0,0,0,0.6); -fx-background-radius: 24; -fx-border-color: #64B5F6; -fx-border-width: 3; -fx-border-radius: 24;");

        Label header = new Label("Cards Pulled");
        header.setFont(Font.font("Arial Black", 36));
        header.setTextFill(Color.web("#B3E5FC"));

        Label text = new Label(result.message());
        text.setFont(Font.font("Consolas", 28));
        text.setTextFill(Color.WHITE);
        text.setWrapText(true);
        text.setMaxWidth(1000);

        card.getChildren().addAll(header, text);

        Button back = uiFactory.action("BACK TO SHOP", 420, 110, 38, "#00C853", 24, () -> showShop(stage));
        Button menu = uiFactory.action("BACK TO HUB", 420, 110, 38, "#D32F2F", 24, () -> showMenu(stage));
        HBox buttons = new HBox(24, back, menu);
        buttons.setAlignment(Pos.CENTER);

        root.getChildren().addAll(title, card, buttons);

        Scene scene = new Scene(root, WIDTH, HEIGHT);
        setupKeyboardNavigation(scene);
        applyConsoleHighlight(scene);
        setScenePreservingFullscreen(stage, scene);
        back.requestFocus();
    }

    private void queueUnlockCardForBird(BirdType type) {
        if (type == null) return;
        pendingUnlockCards.add(UnlockCard.bird(type));
    }

    private void queueUnlockCardForSkin(BirdType bird, String key) {
        if (bird == null || key == null) return;
        pendingUnlockCards.add(UnlockCard.skin(bird, key));
    }

    private void queueUnlockCardForMap(MapType map) {
        if (map == null) return;
        pendingUnlockCards.add(UnlockCard.map(map));
    }

    private void runAfterUnlockCards(Stage stage, Runnable onComplete) {
        if (pendingUnlockCards.isEmpty()) {
            if (onComplete != null) onComplete.run();
            return;
        }
        showNextUnlockCard(stage, onComplete);
    }

    private void showNextUnlockCard(Stage stage, Runnable onComplete) {
        UnlockCard card = pendingUnlockCards.poll();
        if (card == null) {
            if (onComplete != null) onComplete.run();
            return;
        }
        showUnlockCard(stage, card, onComplete);
    }

    private void showUnlockCard(Stage stage, UnlockCard card, Runnable onComplete) {
        if (stage == null || card == null) {
            if (onComplete != null) onComplete.run();
            return;
        }
        playMenuMusic();

        VBox root = MenuLayout.buildMenuRoot("-fx-background-color: linear-gradient(to bottom, #0A1422, #1C2B3C, #16212F);",
                MENU_PADDING, 26);

        Label title = new Label("NEW FEATHERPEDIA ENTRY");
        title.setFont(Font.font("Impact", FontWeight.BOLD, 78));
        title.setTextFill(Color.web("#FFE082"));

        String subtitleText = card.map != null ? "MAP UNLOCKED" : (card.skinKey != null ? "SKIN UNLOCKED" : "BIRD UNLOCKED");
        Label subtitle = new Label(subtitleText);
        subtitle.setFont(Font.font("Consolas", 24));
        subtitle.setTextFill(Color.web("#80DEEA"));

        VBox header = new VBox(4, title, subtitle);
        header.setAlignment(Pos.CENTER);

        String border = "#607D8B";
        VBox sidebar = new VBox(14);
        sidebar.setAlignment(Pos.TOP_CENTER);
        sidebar.setPadding(new Insets(18));
        sidebar.setPrefWidth(520);
        sidebar.setMaxWidth(520);
        sidebar.setStyle("-fx-background-color: rgba(0,0,0,0.6); -fx-background-radius: 20; -fx-border-color: "
                + border + "; -fx-border-width: 2; -fx-border-radius: 20;");

        if (card.map != null) {
            MapEntry entry = findMapEntry(card.map);
            if (entry == null) {
                entry = new MapEntry(card.map, mapDisplayName(card.map), mapDescription(card.map), mapHowToGet(card.map));
            }
            showMapSidebar(sidebar, entry);
        } else if (card.skinKey != null) {
            SkinEntry entry = findSkinEntry(card.bird, card.skinKey);
            if (entry == null) {
                String name = skinDisplayName(card.skinKey, card.bird);
                entry = new SkinEntry(card.bird, card.skinKey, name,
                        skinDescription(card.skinKey, card.bird), skinHowToGet(card.skinKey, card.bird));
            }
            showSkinSidebar(sidebar, entry);
        } else if (card.bird != null) {
            showBirdSidebar(sidebar, card.bird, true);
        }

        Label continueLabel = new Label("CLICK TO CONTINUE");
        continueLabel.setFont(Font.font("Consolas", 28));
        continueLabel.setTextFill(Color.web("#C8E6C9"));
        continueLabel.setEffect(new Glow(0.6));
        continueLabel.setAlignment(Pos.CENTER);
        continueLabel.setTextAlignment(TextAlignment.CENTER);

        root.getChildren().addAll(header, sidebar, continueLabel);

        Scene scene = new Scene(root, WIDTH, HEIGHT);
        setupKeyboardNavigation(scene);
        applyConsoleHighlight(scene);
        setScenePreservingFullscreen(stage, scene);

        Runnable advance = () -> {
            if (pendingUnlockCards.isEmpty()) {
                if (onComplete != null) onComplete.run();
            } else {
                showNextUnlockCard(stage, onComplete);
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

    private SkinEntry findSkinEntry(BirdType bird, String key) {
        if (bird == null || key == null) return null;
        for (SkinEntry entry : birdBookSkins()) {
            if (entry.bird == bird && key.equals(entry.key)) return entry;
        }
        return null;
    }

    private MapEntry findMapEntry(MapType map) {
        if (map == null) return null;
        for (MapEntry entry : birdBookMaps()) {
            if (entry.map == map) return entry;
        }
        return null;
    }

    private String skinDisplayName(String key, BirdType type) {
        if (key == null) return "Skin";
        if ("CITY_PIGEON".equals(key)) return "City Pigeon";
        if ("NOIR_PIGEON".equals(key)) return "Noir Pigeon";
        if (BEACON_PIGEON_SKIN.equals(key)) return "Beacon Pigeon";
        if ("SKY_KING_EAGLE".equals(key)) return "Sky King Eagle";
        if (NOVA_PHOENIX_SKIN.equals(key)) return "Nova Phoenix";
        if (DUNE_FALCON_SKIN.equals(key)) return "Dune Falcon";
        if (MINT_PENGUIN_SKIN.equals(key)) return "Mint Penguin";
        if (CIRCUIT_TITMOUSE_SKIN.equals(key)) return "Circuit Titmouse";
        if (PRISM_RAZORBILL_SKIN.equals(key)) return "Prism Razorbill";
        if (AURORA_PELICAN_SKIN.equals(key)) return "Aurora Pelican";
        if (SUNFLARE_HUMMINGBIRD_SKIN.equals(key)) return "Sunflare Hummingbird";
        if (GLACIER_SHOEBILL_SKIN.equals(key)) return "Glacier Shoebill";
        if (TIDE_VULTURE_SKIN.equals(key)) return "Tide Vulture";
        if (ECLIPSE_MOCKINGBIRD_SKIN.equals(key)) return "Eclipse Charles";
        if (UMBRA_BAT_SKIN.equals(key)) return "Umbra Bat";
        if (key.startsWith("CLASSIC_SKIN_") && type != null) return classicRewardFor(type);
        return key.replace('_', ' ');
    }

    private static final class UnlockCard {
        final BirdType bird;
        final String skinKey;
        final MapType map;

        private UnlockCard(BirdType bird, String skinKey, MapType map) {
            this.bird = bird;
            this.skinKey = skinKey;
            this.map = map;
        }

        static UnlockCard bird(BirdType bird) {
            return new UnlockCard(bird, null, null);
        }

        static UnlockCard skin(BirdType bird, String skinKey) {
            return new UnlockCard(bird, skinKey, null);
        }

        static UnlockCard map(MapType map) {
            return new UnlockCard(null, null, map);
        }
    }

    private static final class SkinEntry {
        final BirdType bird;
        final String key;
        final String name;
        final String description;
        final String howToGet;

        SkinEntry(BirdType bird, String key, String name, String description, String howToGet) {
            this.bird = bird;
            this.key = key;
            this.name = name;
            this.description = description;
            this.howToGet = howToGet;
        }
    }

    private static final class MapEntry {
        final MapType map;
        final String name;
        final String description;
        final String howToGet;

        MapEntry(MapType map, String name, String description, String howToGet) {
            this.map = map;
            this.name = name;
            this.description = description;
            this.howToGet = howToGet;
        }
    }

    private static final class ItemEntry {
        final String name;
        final String description;
        final String howToGet;
        final boolean unlocked;
        final PowerUpType powerUp;
        final boolean isContinue;
        final boolean isCoin;

        ItemEntry(String name, String description, String howToGet, boolean unlocked, PowerUpType powerUp, boolean isContinue, boolean isCoin) {
            this.name = name;
            this.description = description;
            this.howToGet = howToGet;
            this.unlocked = unlocked;
            this.powerUp = powerUp;
            this.isContinue = isContinue;
            this.isCoin = isCoin;
        }
    }

    private static final class BookTileStyle {
        final boolean unlocked;
        final Color accent;

        BookTileStyle(boolean unlocked, Color accent) {
            this.unlocked = unlocked;
            this.accent = accent;
        }
    }

    private void showBirdBook(Stage stage) {
        showBirdBook(stage, BirdBookCategory.BIRDS);
    }

    private void showBirdBook(Stage stage, BirdBookCategory category) {
        playMenuMusic();
        BorderPane root = new BorderPane();
        root.setPadding(new Insets(26, 40, 26, 40));
        root.setStyle("-fx-background-color: linear-gradient(to bottom, #0A1422, #1C2B3C, #16212F);");

        Button back = uiFactory.action("BACK TO HUB", 360, 90, 34, "#D32F2F", 22, () -> showMenu(stage));
        Label title = new Label("FEATHERPEDIA");
        title.setFont(Font.font("Impact", FontWeight.BOLD, 84));
        title.setTextFill(Color.web("#FFE082"));
        Label subtitle = new Label("THE BIRD BOOK");
        subtitle.setFont(Font.font("Consolas", 22));
        subtitle.setTextFill(Color.web("#80DEEA"));

        VBox titleBox = new VBox(4, title, subtitle);
        titleBox.setAlignment(Pos.CENTER_LEFT);
        HBox header = new HBox(18, back, titleBox);
        header.setAlignment(Pos.CENTER_LEFT);

        HBox tabs = new HBox(16,
                birdBookTab("ITEMS", BirdBookCategory.ITEMS, category, stage),
                birdBookTab("POWERUPS", BirdBookCategory.POWERUPS, category, stage),
                birdBookTab("BIRDS", BirdBookCategory.BIRDS, category, stage),
                birdBookTab("SKINS", BirdBookCategory.SKINS, category, stage),
                birdBookTab("MAPS", BirdBookCategory.MAPS, category, stage)
        );
        tabs.setAlignment(Pos.CENTER);
        tabs.setPadding(new Insets(8, 0, 0, 0));

        VBox top = new VBox(12, header, tabs);
        root.setTop(top);

        BorderPane content = buildBirdBookContent(category);
        root.setCenter(content);

        Scene scene = new Scene(root, WIDTH, HEIGHT);
        bindEscape(scene, back);
        setupKeyboardNavigation(scene);
        applyConsoleHighlight(scene);
        setScenePreservingFullscreen(stage, scene);
        back.requestFocus();
    }

    private Button birdBookTab(String label, BirdBookCategory tab, BirdBookCategory active, Stage stage) {
        String color = (tab == active) ? "#FBC02D" : "#455A64";
        Button btn = uiFactory.action(label, 240, 70, 26, color, 18, () -> showBirdBook(stage, tab));
        if (tab == active) {
            btn.setStyle("-fx-background-color: " + color + "; -fx-text-fill: #0A0A0A; -fx-font-weight: bold;");
        }
        return btn;
    }

    private BorderPane buildBirdBookContent(BirdBookCategory category) {
        BorderPane content = new BorderPane();
        FlowPane grid = new FlowPane(18, 18);
        grid.setAlignment(Pos.TOP_CENTER);
        grid.setPrefWrapLength(1100);
        grid.setPadding(new Insets(10, 10, 10, 10));

        ScrollPane scroll = new ScrollPane(grid);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background-color: transparent; -fx-background: transparent; -fx-control-inner-background: transparent;");
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);

        VBox sidebar = new VBox(14);
        sidebar.setAlignment(Pos.TOP_CENTER);
        sidebar.setPadding(new Insets(18));
        sidebar.setPrefWidth(520);
        sidebar.setStyle("-fx-background-color: rgba(0,0,0,0.6); -fx-background-radius: 20; -fx-border-color: #607D8B; -fx-border-width: 2; -fx-border-radius: 20;");

        buildBirdBookGrid(category, grid, sidebar);

        content.setCenter(scroll);
        content.setRight(sidebar);
        BorderPane.setMargin(sidebar, new Insets(0, 0, 0, 24));
        return content;
    }

    private void buildBirdBookGrid(BirdBookCategory category, FlowPane grid, VBox sidebar) {
        grid.getChildren().clear();
        sidebar.getChildren().clear();
        switch (category) {
            case ITEMS -> buildBirdBookItemsGrid(grid, sidebar);
            case POWERUPS -> buildBirdBookPowerUpsGrid(grid, sidebar);
            case BIRDS -> buildBirdBookBirdsGrid(grid, sidebar);
            case SKINS -> buildBirdBookSkinsGrid(grid, sidebar);
            case MAPS -> buildBirdBookMapsGrid(grid, sidebar);
        }
    }

    private void buildBirdBookItemsGrid(FlowPane grid, VBox sidebar) {
        Runnable[] initial = new Runnable[1];
        Runnable[] fallback = new Runnable[1];

        for (ItemEntry entry : birdBookItems()) {
            Color accent = entry.powerUp != null ? entry.powerUp.color : Color.web("#FFD54F");
            Node icon = entry.unlocked
                    ? (entry.isContinue ? buildContinueTileIcon() : (entry.isCoin ? buildCoinTileIcon() : buildPowerUpTileIcon(entry.powerUp)))
                    : buildLockedTileIcon(accent);
            Button tile = createBirdBookTile(grid, entry.name, icon, entry.unlocked, accent,
                    () -> showItemSidebar(sidebar, entry));
            grid.getChildren().add(tile);

            Runnable select = () -> {
                selectBirdBookTile(grid, tile);
                showItemSidebar(sidebar, entry);
            };
            if (fallback[0] == null) fallback[0] = select;
            if (initial[0] == null && entry.unlocked) initial[0] = select;
        }

        if (initial[0] != null) {
            initial[0].run();
        } else if (fallback[0] != null) {
            fallback[0].run();
        }
    }

    private void buildBirdBookBirdsGrid(FlowPane grid, VBox sidebar) {
        Runnable[] initial = new Runnable[1];
        Runnable[] fallback = new Runnable[1];

        List<BirdType> ordered = new ArrayList<>(Arrays.asList(BirdType.values()));
        if (ordered.remove(BirdType.HEISENBIRD)) {
            int opiumIndex = ordered.indexOf(BirdType.OPIUMBIRD);
            if (opiumIndex >= 0) ordered.add(opiumIndex + 1, BirdType.HEISENBIRD);
            else ordered.add(BirdType.HEISENBIRD);
        }

        for (BirdType type : ordered) {
            boolean unlocked = isBirdUnlocked(type);
            MapType origin = originMapForBird(type);
            Color accent = mapAccentColor(origin);
            Node icon = unlocked
                    ? buildBirdTileIcon(type, null, origin)
                    : buildLockedTileIcon(accent);
            Button tile = createBirdBookTile(grid, type.name.toUpperCase(), icon, unlocked, accent,
                    () -> showBirdSidebar(sidebar, type));
            grid.getChildren().add(tile);

            Runnable select = () -> {
                selectBirdBookTile(grid, tile);
                showBirdSidebar(sidebar, type);
            };
            if (fallback[0] == null) fallback[0] = select;
            if (initial[0] == null && unlocked) initial[0] = select;
        }

        if (initial[0] != null) {
            initial[0].run();
        } else if (fallback[0] != null) {
            fallback[0].run();
        }
    }

    private void buildBirdBookSkinsGrid(FlowPane grid, VBox sidebar) {
        Runnable[] initial = new Runnable[1];
        Runnable[] fallback = new Runnable[1];

        for (SkinEntry entry : birdBookSkins()) {
            boolean unlocked = isSkinUnlocked(entry.key, entry.bird);
            MapType origin = originMapForBird(entry.bird);
            Color accent = mapAccentColor(origin);
            Node icon = unlocked
                    ? buildBirdTileIcon(entry.bird, entry.key, origin)
                    : buildLockedTileIcon(accent);
            String label = entry.name + "\n" + skinRarityLabel(entry.key, entry.bird);
            Button tile = createBirdBookTile(grid, label, icon, unlocked, accent,
                    () -> showSkinSidebar(sidebar, entry));
            grid.getChildren().add(tile);

            Runnable select = () -> {
                selectBirdBookTile(grid, tile);
                showSkinSidebar(sidebar, entry);
            };
            if (fallback[0] == null) fallback[0] = select;
            if (initial[0] == null && unlocked) initial[0] = select;
        }

        if (initial[0] != null) {
            initial[0].run();
        } else if (fallback[0] != null) {
            fallback[0].run();
        }
    }

    private void buildBirdBookMapsGrid(FlowPane grid, VBox sidebar) {
        Runnable[] initial = new Runnable[1];
        Runnable[] fallback = new Runnable[1];

        for (MapEntry entry : birdBookMaps()) {
            boolean unlocked = isMapUnlocked(entry.map);
            Color accent = mapAccentColor(entry.map);
            Node icon = unlocked
                    ? buildMapTileIcon(entry.map)
                    : buildLockedTileIcon(accent);
            Button tile = createBirdBookTile(grid, entry.name, icon, unlocked, accent,
                    () -> showMapSidebar(sidebar, entry));
            grid.getChildren().add(tile);

            Runnable select = () -> {
                selectBirdBookTile(grid, tile);
                showMapSidebar(sidebar, entry);
            };
            if (fallback[0] == null) fallback[0] = select;
            if (initial[0] == null && unlocked) initial[0] = select;
        }

        if (initial[0] != null) {
            initial[0].run();
        } else if (fallback[0] != null) {
            fallback[0].run();
        }
    }

    private VBox createBookCard(double width, String borderColor) {
        VBox card = new VBox(10);
        card.setAlignment(Pos.TOP_CENTER);
        card.setPadding(new Insets(16));
        card.setPrefWidth(width);
        card.setStyle("-fx-background-color: rgba(0,0,0,0.65); -fx-border-color: " + borderColor
                + "; -fx-border-width: 2; -fx-background-radius: 18; -fx-border-radius: 18;");
        return card;
    }

    private VBox createLockedBookCard(double width, String borderColor, String name, String howToGet) {
        VBox card = createBookCard(width, borderColor);
        Label title = bookTitle(name, 28);
        Label status = bookStatus(false);
        Label how = bookBody("HOW TO GET: " + howToGet, 18);
        card.getChildren().addAll(title, status, how);
        return card;
    }

    private Label bookTitle(String text, int size) {
        Label label = new Label(text);
        label.setFont(Font.font("Impact", size));
        label.setTextFill(Color.web("#ECEFF1"));
        label.setAlignment(Pos.CENTER);
        label.setTextAlignment(TextAlignment.CENTER);
        label.setWrapText(true);
        label.setMaxWidth(440);
        return label;
    }

    private Label bookBody(String text, int size) {
        Label label = new Label(text);
        label.setFont(Font.font("Consolas", size));
        label.setTextFill(Color.web("#CFD8DC"));
        label.setAlignment(Pos.CENTER);
        label.setTextAlignment(TextAlignment.CENTER);
        label.setWrapText(true);
        label.setMaxWidth(440);
        return label;
    }

    private Label bookStatus(boolean unlocked) {
        Label label = new Label(unlocked ? "UNLOCKED" : "LOCKED");
        label.setFont(Font.font("Consolas", 16));
        label.setTextFill(unlocked ? Color.LIMEGREEN : Color.web("#FF8A65"));
        label.setStyle("-fx-background-color: rgba(0,0,0,0.45); -fx-padding: 4 12 4 12; -fx-background-radius: 12;");
        label.setAlignment(Pos.CENTER);
        return label;
    }

    private Button createBirdBookTile(FlowPane grid, String name, Node icon, boolean unlocked, Color accent, Runnable onSelect) {
        Button btn = new Button();
        btn.setPrefSize(210, 220);
        btn.setFocusTraversable(true);
        applyNoEllipsis(btn);

        Label label = new Label(name);
        label.setFont(Font.font("Consolas", 14));
        label.setTextFill(unlocked ? Color.web("#ECEFF1") : Color.web("#B0BEC5"));
        label.setWrapText(true);
        label.setTextAlignment(TextAlignment.CENTER);
        label.setAlignment(Pos.CENTER);
        label.setMaxWidth(180);

        VBox box = new VBox(8, icon, label);
        box.setAlignment(Pos.CENTER);
        btn.setGraphic(box);
        btn.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);

        btn.setUserData(new BookTileStyle(unlocked, accent));
        applyBirdBookTileStyle(btn, unlocked, accent, false);
        btn.setOnAction(e -> {
            playButtonClick();
            selectBirdBookTile(grid, btn);
            if (onSelect != null) onSelect.run();
        });
        return btn;
    }

    private void selectBirdBookTile(FlowPane grid, Button selected) {
        for (Node node : grid.getChildren()) {
            if (!(node instanceof Button btn)) continue;
            BookTileStyle style = (BookTileStyle) btn.getUserData();
            Color accent = style != null && style.accent != null ? style.accent : Color.web("#607D8B");
            boolean unlocked = style != null && style.unlocked;
            applyBirdBookTileStyle(btn, unlocked, accent, btn == selected);
        }
    }

    private void applyBirdBookTileStyle(Button btn, boolean unlocked, Color accent, boolean selected) {
        String border = toHex(accent == null ? Color.web("#607D8B") : accent);
        String bg = unlocked ? "rgba(8,12,18,0.8)" : "rgba(20,20,20,0.6)";
        int width = selected ? 3 : 2;
        btn.setStyle("-fx-background-color: " + bg
                + "; -fx-border-color: " + border
                + "; -fx-border-width: " + width
                + "; -fx-background-radius: 16; -fx-border-radius: 16; -fx-text-overrun: clip;");
        btn.setOpacity(unlocked ? 1.0 : 0.75);
    }

    private Node buildBirdTileIcon(BirdType type, String skinKey, MapType map) {
        Canvas bg = new Canvas(130, 90);
        drawMapBackdrop(bg, map);
        Canvas sprite = new Canvas(90, 90);
        drawRosterSprite(sprite, type, skinKey, false, true);
        StackPane stack = new StackPane(bg, sprite);
        if (type == BirdType.FALCON || type == BirdType.HEISENBIRD) {
            BirdType echoType = (type == BirdType.FALCON) ? BirdType.EAGLE : BirdType.OPIUMBIRD;
            double echoSize = sprite.getWidth() * 0.55;
            Canvas echo = new Canvas(echoSize, echoSize);
            drawRosterSprite(echo, echoType, null, false);
            echo.setOpacity(0.32);
            StackPane.setAlignment(echo, Pos.TOP_LEFT);
            StackPane.setMargin(echo, new Insets(6, 0, 0, 6));
            stack.getChildren().add(echo);
        }
        stack.setPrefSize(130, 110);
        stack.setMaxSize(130, 110);
        return stack;
    }

    private Node buildMapTileIcon(MapType map) {
        Canvas preview = new Canvas(130, 90);
        drawMapPreview(preview, map);
        return preview;
    }

    private Node buildPowerUpTileIcon(PowerUpType type) {
        Canvas icon = new Canvas(90, 90);
        drawPowerUpIcon(icon, type);
        return icon;
    }

    private Node buildContinueTileIcon() {
        Canvas icon = new Canvas(90, 90);
        drawContinueIcon(icon);
        return icon;
    }

    private Node buildCoinTileIcon() {
        Canvas icon = new Canvas(90, 90);
        drawCoinIcon(icon);
        return icon;
    }

    private Node buildLockedTileIcon(Color accent) {
        Canvas icon = new Canvas(90, 90);
        drawLockedIcon(icon, accent);
        return icon;
    }

    private void showItemSidebar(VBox sidebar, ItemEntry entry) {
        sidebar.getChildren().clear();
        Label name = bookTitle(entry.name, 30);
        Label status = bookStatus(entry.unlocked);
        sidebar.getChildren().addAll(name, status);
        if (!entry.unlocked) {
            sidebar.getChildren().add(bookBody("HOW TO GET: " + entry.howToGet, 18));
            return;
        }
        Canvas icon = new Canvas(200, 200);
        if (entry.isContinue) {
            drawContinueIcon(icon);
        } else if (entry.isCoin) {
            drawCoinIcon(icon);
        } else if (entry.powerUp != null) {
            drawPowerUpIcon(icon, entry.powerUp);
        }
        Label desc = bookBody(entry.description, 18);
        Label how = bookBody("HOW TO GET: " + entry.howToGet, 16);
        sidebar.getChildren().addAll(icon, desc, how);
    }

    private void showBirdSidebar(VBox sidebar, BirdType type) {
        showBirdSidebar(sidebar, type, false);
    }

    private void showBirdSidebar(VBox sidebar, BirdType type, boolean forceUnlocked) {
        sidebar.getChildren().clear();
        boolean unlocked = forceUnlocked || isBirdUnlocked(type);
        Label name = bookTitle(type.name.toUpperCase(), 32);
        Label status = bookStatus(unlocked);
        sidebar.getChildren().addAll(name, status);
        if (!unlocked) {
            sidebar.getChildren().add(bookBody("HOW TO GET: " + birdHowToGet(type), 18));
            return;
        }
        MapType origin = originMapForBird(type);
        StackPane art = buildBirdBookArt(type, null, origin);
        Label stats = bookBody(birdStatsLine(type), 18);
        Label special = bookBody("SPECIAL: " + type.ability, 18);
        Label fun = bookBody(birdFunDescription(type), 17);
        sidebar.getChildren().addAll(art, stats, special, fun);
    }

    private void showSkinSidebar(VBox sidebar, SkinEntry entry) {
        sidebar.getChildren().clear();
        boolean unlocked = isSkinUnlocked(entry.key, entry.bird);
        Label name = bookTitle(entry.name, 30);
        Label status = bookStatus(unlocked);
        Label rarity = bookBody("RARITY: " + skinRarityLabel(entry.key, entry.bird), 16);
        sidebar.getChildren().addAll(name, status, rarity);
        if (!unlocked) {
            sidebar.getChildren().add(bookBody("HOW TO GET: " + entry.howToGet, 18));
            return;
        }
        MapType origin = originMapForBird(entry.bird);
        StackPane art = buildBirdBookArt(entry.bird, entry.key, origin);
        Label base = bookBody("BASE BIRD: " + entry.bird.name, 16);
        Label desc = bookBody(entry.description, 18);
        sidebar.getChildren().addAll(art, base, desc);
    }

    private void showMapSidebar(VBox sidebar, MapEntry entry) {
        sidebar.getChildren().clear();
        boolean unlocked = isMapUnlocked(entry.map);
        Label name = bookTitle(entry.name, 30);
        Label status = bookStatus(unlocked);
        sidebar.getChildren().addAll(name, status);
        if (!unlocked) {
            sidebar.getChildren().add(bookBody("HOW TO GET: " + entry.howToGet, 18));
            return;
        }
        Canvas preview = new Canvas(360, 200);
        drawMapPreview(preview, entry.map);
        Label desc = bookBody(entry.description, 18);
        sidebar.getChildren().addAll(preview, desc);
    }

    private StackPane buildBirdBookArt(BirdType type, String skinKey, MapType map) {
        Canvas bg = new Canvas(360, 200);
        drawMapBackdrop(bg, map);
        Canvas sprite = new Canvas(160, 160);
        drawRosterSprite(sprite, type, skinKey, false, true);
        StackPane art = new StackPane(bg, sprite);
        art.setAlignment(Pos.CENTER);
        return art;
    }

    private void drawLockedIcon(Canvas canvas, Color tint) {
        GraphicsContext g = canvas.getGraphicsContext2D();
        double w = canvas.getWidth();
        double h = canvas.getHeight();
        g.clearRect(0, 0, w, h);

        Color base = tint == null ? Color.web("#90A4AE") : tint;
        g.setFill(Color.web("#0F171F", 0.8));
        g.fillRoundRect(w * 0.08, h * 0.08, w * 0.84, h * 0.84, 18, 18);

        g.setStroke(base);
        g.setLineWidth(3);
        g.strokeRoundRect(w * 0.18, h * 0.18, w * 0.64, h * 0.64, 14, 14);

        g.setStroke(base.brighter());
        g.setLineWidth(5);
        g.strokeArc(w * 0.32, h * 0.22, w * 0.36, h * 0.3, 0, 180, javafx.scene.shape.ArcType.OPEN);
        g.setFill(base.deriveColor(0, 1, 1, 0.85));
        g.fillRoundRect(w * 0.32, h * 0.42, w * 0.36, h * 0.32, 10, 10);
        g.setFill(Color.web("#263238"));
        g.fillOval(w * 0.47, h * 0.53, w * 0.06, h * 0.1);
    }

    private void drawPowerUpIcon(Canvas canvas, PowerUpType type) {
        GraphicsContext g = canvas.getGraphicsContext2D();
        double w = canvas.getWidth();
        double h = canvas.getHeight();
        g.clearRect(0, 0, w, h);
        PowerUp preview = new PowerUp(w / 2.0, h / 2.0, type);
        drawPowerUpSprite(g, preview, 0);
    }

    private void drawContinueIcon(Canvas canvas) {
        GraphicsContext g = canvas.getGraphicsContext2D();
        double w = canvas.getWidth();
        double h = canvas.getHeight();
        g.clearRect(0, 0, w, h);
        double size = Math.min(w, h) * 0.7;
        double x = (w - size) / 2.0;
        double y = (h - size) / 2.0;
        g.setFill(Color.web("#263238"));
        g.fillOval(x, y, size, size);
        g.setStroke(Color.web("#FFD54F"));
        g.setLineWidth(6);
        g.strokeOval(x, y, size, size);

        g.setStroke(Color.web("#FFE082"));
        g.setLineWidth(6);
        double pad = size * 0.18;
        g.strokeArc(x + pad, y + pad, size - pad * 2, size - pad * 2, 60, 260, javafx.scene.shape.ArcType.OPEN);

        double arrowX = x + size * 0.76;
        double arrowY = y + size * 0.3;
        g.setFill(Color.web("#FFE082"));
        g.fillPolygon(
                new double[]{arrowX, arrowX + size * 0.12, arrowX + size * 0.04},
                new double[]{arrowY, arrowY + size * 0.05, arrowY + size * 0.16},
                3
        );
    }

    private void drawCoinIcon(Canvas canvas) {
        GraphicsContext g = canvas.getGraphicsContext2D();
        double w = canvas.getWidth();
        double h = canvas.getHeight();
        g.clearRect(0, 0, w, h);

        double size = Math.min(w, h) * 0.62;
        double x = (w - size) / 2.0;
        double y = (h - size) / 2.0;

        double backSize = size * 0.9;
        double backX = x - size * 0.14;
        double backY = y + size * 0.12;
        g.setFill(Color.web("#F9A825"));
        g.fillOval(backX, backY, backSize, backSize);
        g.setStroke(Color.web("#F6C945"));
        g.setLineWidth(size * 0.06);
        g.strokeOval(backX, backY, backSize, backSize);

        g.setFill(Color.web("#FFD54F"));
        g.fillOval(x, y, size, size);
        g.setStroke(Color.web("#FFF59D"));
        g.setLineWidth(size * 0.08);
        g.strokeOval(x, y, size, size);

        g.setFill(Color.web("#F57F17"));
        double mark = size * 0.24;
        g.fillOval(x + size * 0.38, y + size * 0.38, mark, mark);
        g.setStroke(Color.web("#FFE082"));
        g.setLineWidth(size * 0.05);
        g.strokeOval(x + size * 0.38, y + size * 0.38, mark, mark);
    }

    private void drawMapBackdrop(Canvas canvas, MapType map) {
        GraphicsContext g = canvas.getGraphicsContext2D();
        double w = canvas.getWidth();
        double h = canvas.getHeight();
        g.clearRect(0, 0, w, h);

        Color top;
        Color bottom;
        switch (map) {
            case CITY -> {
                top = Color.web("#0D2A52");
                bottom = Color.web("#311B92");
            }
            case SKYCLIFFS -> {
                top = Color.web("#5D4037");
                bottom = Color.web("#B3E5FC");
            }
            case VIBRANT_JUNGLE -> {
                top = Color.web("#0B3D24");
                bottom = Color.web("#2E7D32");
            }
            case CAVE -> {
                top = Color.web("#1A237E");
                bottom = Color.web("#263238");
            }
            case BATTLEFIELD -> {
                top = Color.web("#0D47A1");
                bottom = Color.web("#1E88E5");
            }
            default -> {
                top = Color.web("#1B5E20");
                bottom = Color.web("#4CAF50");
            }
        }

        LinearGradient gradient = new LinearGradient(0, 0, 0, 1, true, CycleMethod.NO_CYCLE,
                new Stop(0, top), new Stop(1, bottom));
        g.setFill(gradient);
        g.fillRect(0, 0, w, h);

        switch (map) {
            case CITY -> {
                g.setFill(Color.web("#101820", 0.75));
                double base = h * 0.78;
                double bw = w / 7.0;
                double[] heights = new double[]{0.32, 0.54, 0.4, 0.62, 0.36, 0.5, 0.42};
                for (int i = 0; i < heights.length; i++) {
                    double bh = h * heights[i];
                    g.fillRect(i * bw, base - bh, bw * 0.9, bh);
                }
                g.setFill(Color.web("#FFC107", 0.4));
                for (int i = 0; i < heights.length; i++) {
                    double bx = i * bw + bw * 0.2;
                    double by = base - h * heights[i] + h * 0.08;
                    for (int r = 0; r < 3; r++) {
                        g.fillRect(bx, by + r * h * 0.08, bw * 0.18, h * 0.03);
                    }
                }
            }
            case SKYCLIFFS -> {
                g.setFill(Color.web("#6D4C41", 0.75));
                g.fillPolygon(new double[]{0, w * 0.22, w * 0.44}, new double[]{h, h * 0.45, h}, 3);
                g.fillPolygon(new double[]{w * 0.35, w * 0.62, w * 0.88}, new double[]{h, h * 0.35, h}, 3);
                g.setFill(Color.web("#8D6E63", 0.65));
                g.fillPolygon(new double[]{w * 0.1, w * 0.35, w * 0.6}, new double[]{h, h * 0.55, h}, 3);
            }
            case VIBRANT_JUNGLE -> {
                g.setStroke(Color.web("#1B5E20", 0.8));
                g.setLineWidth(3);
                g.strokeLine(w * 0.15, 0, w * 0.25, h);
                g.strokeLine(w * 0.4, 0, w * 0.35, h);
                g.strokeLine(w * 0.7, 0, w * 0.78, h);
                g.setFill(Color.web("#2E7D32", 0.7));
                g.fillOval(w * 0.05, h * 0.7, w * 0.25, h * 0.25);
                g.fillOval(w * 0.7, h * 0.65, w * 0.25, h * 0.3);
            }
            case CAVE -> {
                g.setFill(Color.web("#263238", 0.75));
                double spikeW = w / 6.0;
                for (int i = 0; i < 6; i++) {
                    double x = i * spikeW;
                    g.fillPolygon(new double[]{x, x + spikeW * 0.5, x + spikeW}, new double[]{0, h * 0.25, 0}, 3);
                }
                for (int i = 0; i < 5; i++) {
                    double x = i * spikeW + spikeW * 0.1;
                    g.fillPolygon(new double[]{x, x + spikeW * 0.5, x + spikeW}, new double[]{h, h * 0.75, h}, 3);
                }
            }
            case BATTLEFIELD -> {
                g.setFill(Color.web("#4E342E", 0.85));
                g.fillOval(w * 0.2, h * 0.65, w * 0.6, h * 0.3);
                g.setFill(Color.web("#2E7D32", 0.8));
                g.fillOval(w * 0.24, h * 0.62, w * 0.52, h * 0.22);
                g.setStroke(Color.web("#90CAF9", 0.5));
                g.setLineWidth(2.5);
                g.strokeLine(0, h * 0.62, w, h * 0.62);
            }
            default -> {
                g.setFill(Color.web("#1B5E20", 0.75));
                g.fillPolygon(new double[]{0, w * 0.1, w * 0.2}, new double[]{h, h * 0.55, h}, 3);
                g.fillPolygon(new double[]{w * 0.15, w * 0.3, w * 0.45}, new double[]{h, h * 0.45, h}, 3);
                g.fillPolygon(new double[]{w * 0.5, w * 0.62, w * 0.74}, new double[]{h, h * 0.5, h}, 3);
                g.fillPolygon(new double[]{w * 0.7, w * 0.82, w}, new double[]{h, h * 0.58, h}, 3);
            }
        }

        g.setFill(Color.web("#000000", 0.12));
        g.fillRect(0, 0, w, h);
    }

    private void drawMapPreview(Canvas canvas, MapType map) {
        GraphicsContext g = canvas.getGraphicsContext2D();
        double w = canvas.getWidth();
        double h = canvas.getHeight();
        g.clearRect(0, 0, w, h);
        drawMapBackdrop(canvas, map);

        g.setFill(Color.web("#ECEFF1", 0.8));
        g.fillRoundRect(w * 0.1, h * 0.7, w * 0.3, h * 0.08, 10, 10);
        g.fillRoundRect(w * 0.6, h * 0.6, w * 0.28, h * 0.08, 10, 10);
        g.fillRoundRect(w * 0.32, h * 0.42, w * 0.36, h * 0.08, 10, 10);
    }

    private Color mapAccentColor(MapType map) {
        return switch (map) {
            case CITY -> Color.web("#5E35B1");
            case SKYCLIFFS -> Color.web("#8D6E63");
            case VIBRANT_JUNGLE -> Color.web("#388E3C");
            case CAVE -> Color.web("#455A64");
            case BATTLEFIELD -> Color.web("#1E88E5");
            default -> Color.web("#2E7D32");
        };
    }

    private MapType originMapForBird(BirdType type) {
        return switch (type) {
            case PIGEON, MOCKINGBIRD, RAVEN -> MapType.CITY;
            case EAGLE, FALCON, PENGUIN, RAZORBILL -> MapType.SKYCLIFFS;
            case PHOENIX, BAT, VULTURE, OPIUMBIRD, HEISENBIRD -> MapType.CAVE;
            case HUMMINGBIRD, TITMOUSE -> MapType.VIBRANT_JUNGLE;
            case PELICAN -> MapType.BATTLEFIELD;
            default -> MapType.FOREST;
        };
    }

    private String birdStatsLine(BirdType type) {
        return "Power: " + type.power
                + " | Speed: " + String.format("%.1f", type.speed)
                + " | Jump: " + type.jumpHeight
                + " | Lift: " + String.format("%.2f", type.flyUpForce);
    }

    private String birdFunDescription(BirdType type) {
        return switch (type) {
            case PIGEON -> "Rooftop regular who knows every shortcut and every rumor. Never looks lost, even when the sky is falling.";
            case EAGLE -> "Born to patrol the highest drafts and punish anyone below. Majestic until the dive starts, then it is all violence.";
            case FALCON -> "Precision hunter with a chip on its shoulder. It loves the cleanest hit and the loudest crowd reaction.";
            case PHOENIX -> "Flies like a blaze and lands like a firework. Somehow always returns, as if it is insulting the concept of defeat.";
            case HUMMINGBIRD -> "A blur with a sweet tooth and a short temper. Will duel you for a drop of nectar and win smiling.";
            case TURKEY -> "Big steps, bigger thumps. Treats the ground like an instrument and keeps the rhythm with shockwaves.";
            case PENGUIN -> "Slides more than it flies, but it still finds a way to win. Cool, calm, and stubborn as a glacier.";
            case SHOEBILL -> "Stares too long, strikes too fast. Marsh legends say it never blinks, only decides.";
            case MOCKINGBIRD -> "Old friend of Caleb Bossk and owner of the Charles Lounge. Passed the Bossk Test to become a Bosskhead, then turned every fight into his stage.";
            case RAZORBILL -> "Cut-clean wings and sharper intent. Prefers clean lines, clean hits, and no wasted motion.";
            case GRINCHHAWK -> "Holiday menace with a grudge. Brings chaos instead of gifts and calls it tradition.";
            case VULTURE -> "Patient and dangerous, Vulture circles until the moment is right. \"You are lucky to be on my side. My crows could end you in seconds,\" he warns.";
            case OPIUMBIRD -> "Drifts in a haze and leaves trouble behind. Calm, then suddenly cruel when the cloud rolls in.";
            case HEISENBIRD -> "Blue-hatted and bald, Heisenbird cooks sky-blue crystals in a hidden roost. The coop whispers \"say my name\" when he lands, and he is the one who pecks.";
            case TITMOUSE -> "Tiny rocket with a fearless heart. Loves speed, hates standing still, and dares you to keep up.";
            case BAT -> "Night specialist who hears everything and hides in the shadows. It knows the cave better than the cave knows itself.";
            case PELICAN -> "Iron beak, iron will. Hauls momentum like cargo and hits like a loaded ship.";
            case RAVEN -> "A shadow on the skyline with a talent for misdirection. It appears, it hits, and then it is already gone.";
        };
    }

    private String typeDisplayName(PowerUpType type) {
        return switch (type) {
            case HEALTH -> "Health";
            case SPEED -> "Speed Boost";
            case RAGE -> "Rage";
            case SHRINK -> "Shrink";
            case NEON -> "Neon Boost";
            case THERMAL -> "Thermal Rise";
            case VINE_GRAPPLE -> "Vine Grapple";
            case OVERCHARGE -> "Overcharge";
            case TITAN -> "Titan Form";
        };
    }

    private String powerUpDescription(PowerUpType type) {
        return switch (type) {
            case HEALTH -> "Instant +40 HP. Turns a losing duel into a second wind.";
            case SPEED -> "Big speed surge for a short time. Great for chases, escapes, and sudden flanks.";
            case RAGE -> "Double attack power for a short burst. Every hit feels like a hammer.";
            case SHRINK -> "Shrinks and weakens all enemies. Buy space, then punish hard.";
            case NEON -> "Hyper speed rush with extra power and mobility. The loudest pickup in the arena.";
            case THERMAL -> "Stronger lift and hang time. Float above the chaos and reset the fight.";
            case VINE_GRAPPLE -> "Grants 3 vine swings and aerial control. Swing to angles nobody expects.";
            case OVERCHARGE -> "Resets special cooldown and amps attacks. Perfect for turning a brawl.";
            case TITAN -> "Grow larger with boosted power and durability. You become the hazard.";
        };
    }

    private String birdHowToGet(BirdType type) {
        return switch (type) {
            case FALCON -> "Card Packs";
            case PHOENIX -> "Card Packs";
            case BAT -> "Defeat Vulture in Episode 1 or Card Packs";
            case TITMOUSE -> "Clear Classic with Hummingbird or Card Packs";
            case HEISENBIRD -> "Card Packs";
            case RAVEN -> "Card Packs";
            default -> "Unlocked by default";
        };
    }

    private boolean isSkinUnlocked(String key, BirdType type) {
        if (key == null) return false;
        if ("CITY_PIGEON".equals(key)) return cityPigeonUnlocked;
        if ("NOIR_PIGEON".equals(key)) return noirPigeonUnlocked;
        if (BEACON_PIGEON_SKIN.equals(key)) return beaconPigeonUnlocked;
        if ("SKY_KING_EAGLE".equals(key)) return eagleSkinUnlocked;
        if (NOVA_PHOENIX_SKIN.equals(key)) return novaPhoenixUnlocked;
        if (DUNE_FALCON_SKIN.equals(key)) return duneFalconUnlocked;
        if (MINT_PENGUIN_SKIN.equals(key)) return mintPenguinUnlocked;
        if (CIRCUIT_TITMOUSE_SKIN.equals(key)) return circuitTitmouseUnlocked;
        if (PRISM_RAZORBILL_SKIN.equals(key)) return prismRazorbillUnlocked;
        if (AURORA_PELICAN_SKIN.equals(key)) return auroraPelicanUnlocked;
        if (SUNFLARE_HUMMINGBIRD_SKIN.equals(key)) return sunflareHummingbirdUnlocked;
        if (GLACIER_SHOEBILL_SKIN.equals(key)) return glacierShoebillUnlocked;
        if (TIDE_VULTURE_SKIN.equals(key)) return tideVultureUnlocked;
        if (ECLIPSE_MOCKINGBIRD_SKIN.equals(key)) return eclipseMockingbirdUnlocked;
        if (UMBRA_BAT_SKIN.equals(key)) return umbraBatUnlocked;
        if (key.startsWith("CLASSIC_SKIN_")) return isClassicRewardUnlocked(type);
        return false;
    }

    private String skinHowToGet(String key, BirdType type) {
        if (key == null) return "Card Packs";
        if ("CITY_PIGEON".equals(key)) return "Unlocked by default";
        if ("NOIR_PIGEON".equals(key)) return "Complete Pigeon Episode or Classic with Pigeon, or Card Packs";
        if (BEACON_PIGEON_SKIN.equals(key)) return "Complete Adventure Chapter 5: Signal of the Beacon";
        if ("SKY_KING_EAGLE".equals(key)) return "Complete Pelican Episode or Classic with Eagle, or Card Packs";
        if (key.startsWith("CLASSIC_SKIN_")) {
            return "Complete Classic with " + type.name + " or Card Packs";
        }
        return "Card Packs";
    }

    private String skinDescription(String key, BirdType type) {
        if ("CITY_PIGEON".equals(key)) return "Gold-plated city swagger for the rooftop boss. Every flap looks expensive and every landing sounds like a coin drop.";
        if ("NOIR_PIGEON".equals(key)) return "Trench-coat noir vibe with a hardboiled stare. Smells like rain, neon, and unfinished business.";
        if (BEACON_PIGEON_SKIN.equals(key)) return "Beacon-lit feathers with a steady glow. The signal chose a keeper and the skyline can feel it.";
        if ("SKY_KING_EAGLE".equals(key)) return "Crowned and gilded, a ruler of the highest drafts. The sky feels smaller when this one arrives.";
        if (DUNE_FALCON_SKIN.equals(key)) return "Sandstorm tones and desert grit. A heat-haze blur that hits before you hear it.";
        if (MINT_PENGUIN_SKIN.equals(key)) return "Fresh icy sheen with a minty chill. Slides look cleaner and landings feel colder.";
        if (CIRCUIT_TITMOUSE_SKIN.equals(key)) return "Neon circuits pulse across the feathers. A tiny turbo engine disguised as a bird.";
        if (PRISM_RAZORBILL_SKIN.equals(key)) return "Prismatic edge that refracts every strike. The blade line is beautiful and dangerous.";
        if (SUNFLARE_HUMMINGBIRD_SKIN.equals(key)) return "Sun-hot wings with a citrus glow. The air smells like ozone and nectar when it passes.";
        if (GLACIER_SHOEBILL_SKIN.equals(key)) return "Ice-blue armor plates and frozen eyes. Every stomp sounds like cracking lake glass.";
        if (TIDE_VULTURE_SKIN.equals(key)) return "Deep-sea hues with salt-stained edges. It circles like a stormfront rolling in.";
        if (ECLIPSE_MOCKINGBIRD_SKIN.equals(key)) return "Shadow velvet with a violet halo. The lounge feels like a night club after midnight.";
        if (NOVA_PHOENIX_SKIN.equals(key)) return "Star-forged glow with cosmic embers. A living supernova with too much style to burn out.";
        if (AURORA_PELICAN_SKIN.equals(key)) return "Polar lights woven into heavy wings. Looks calm, hits like a stormfront.";
        if (UMBRA_BAT_SKIN.equals(key)) return "Void-black membrane with starlit edges. A silent dive that leaves only afterimages.";
        if (key != null && key.startsWith("CLASSIC_SKIN_")) {
            return "Classic reward skin for " + type.name + ". Earned by clearing a full Classic run and wearing the badge with pride.";
        }
        return "Signature skin.";
    }

    private String skinRarityLabel(String key, BirdType type) {
        if (key == null) return "UNKNOWN";
        if (DUNE_FALCON_SKIN.equals(key)) return "COMMON";
        if (SUNFLARE_HUMMINGBIRD_SKIN.equals(key)) return "COMMON";
        if (MINT_PENGUIN_SKIN.equals(key)) return "UNCOMMON";
        if (GLACIER_SHOEBILL_SKIN.equals(key)) return "UNCOMMON";
        if ("CITY_PIGEON".equals(key) || CIRCUIT_TITMOUSE_SKIN.equals(key)) return "RARE";
        if (TIDE_VULTURE_SKIN.equals(key)) return "RARE";
        if ("NOIR_PIGEON".equals(key) || "SKY_KING_EAGLE".equals(key) || PRISM_RAZORBILL_SKIN.equals(key)) return "EPIC";
        if (ECLIPSE_MOCKINGBIRD_SKIN.equals(key)) return "EPIC";
        if (BEACON_PIGEON_SKIN.equals(key) || NOVA_PHOENIX_SKIN.equals(key) || AURORA_PELICAN_SKIN.equals(key)) return "LEGENDARY";
        if (UMBRA_BAT_SKIN.equals(key)) return "LEGENDARY";
        if (key.startsWith("CLASSIC_SKIN_")) return "CLASSIC";
        return "SPECIAL";
    }

    private int skinRarityRank(String key, BirdType type) {
        return switch (skinRarityLabel(key, type)) {
            case "CLASSIC" -> 0;
            case "COMMON" -> 1;
            case "UNCOMMON" -> 2;
            case "RARE" -> 3;
            case "EPIC" -> 4;
            case "LEGENDARY" -> 5;
            default -> 6;
        };
    }

    private List<ItemEntry> birdBookItems() {
        List<ItemEntry> items = new ArrayList<>();
        items.add(new ItemEntry(
                "Bird Coins",
                "Currency earned from matches and packs. Used to buy shop cards and unlock rewards.",
                "Win matches or open Card Packs",
                true,
                null,
                false,
                true
        ));
        boolean hasContinue = classicContinues > 0;
        items.add(new ItemEntry(
                "Classic Continue",
                "Spend after 3 Classic defeats to retry the encounter and keep the run alive. Consumed on use.",
                "Card Packs",
                hasContinue,
                null,
                true,
                false
        ));
        return items;
    }

    private List<ItemEntry> birdBookPowerUps() {
        List<ItemEntry> items = new ArrayList<>();
        for (PowerUpType type : PowerUpType.values()) {
            items.add(new ItemEntry(
                    typeDisplayName(type),
                    powerUpDescription(type),
                    "Appears as a match pickup",
                    true,
                    type,
                    false,
                    false
            ));
        }
        return items;
    }

    private List<SkinEntry> birdBookSkins() {
        List<SkinEntry> skins = new ArrayList<>();
        skins.add(new SkinEntry(BirdType.FALCON, DUNE_FALCON_SKIN, "Dune Falcon",
                skinDescription(DUNE_FALCON_SKIN, BirdType.FALCON), skinHowToGet(DUNE_FALCON_SKIN, BirdType.FALCON)));
        skins.add(new SkinEntry(BirdType.PENGUIN, MINT_PENGUIN_SKIN, "Mint Penguin",
                skinDescription(MINT_PENGUIN_SKIN, BirdType.PENGUIN), skinHowToGet(MINT_PENGUIN_SKIN, BirdType.PENGUIN)));
        skins.add(new SkinEntry(BirdType.PIGEON, "CITY_PIGEON", "City Pigeon",
                skinDescription("CITY_PIGEON", BirdType.PIGEON), skinHowToGet("CITY_PIGEON", BirdType.PIGEON)));
        skins.add(new SkinEntry(BirdType.TITMOUSE, CIRCUIT_TITMOUSE_SKIN, "Circuit Titmouse",
                skinDescription(CIRCUIT_TITMOUSE_SKIN, BirdType.TITMOUSE), skinHowToGet(CIRCUIT_TITMOUSE_SKIN, BirdType.TITMOUSE)));
        skins.add(new SkinEntry(BirdType.PIGEON, "NOIR_PIGEON", "Noir Pigeon",
                skinDescription("NOIR_PIGEON", BirdType.PIGEON), skinHowToGet("NOIR_PIGEON", BirdType.PIGEON)));
        skins.add(new SkinEntry(BirdType.PIGEON, BEACON_PIGEON_SKIN, "Beacon Pigeon",
                skinDescription(BEACON_PIGEON_SKIN, BirdType.PIGEON), skinHowToGet(BEACON_PIGEON_SKIN, BirdType.PIGEON)));
        skins.add(new SkinEntry(BirdType.EAGLE, "SKY_KING_EAGLE", "Sky King Eagle",
                skinDescription("SKY_KING_EAGLE", BirdType.EAGLE), skinHowToGet("SKY_KING_EAGLE", BirdType.EAGLE)));
        skins.add(new SkinEntry(BirdType.RAZORBILL, PRISM_RAZORBILL_SKIN, "Prism Razorbill",
                skinDescription(PRISM_RAZORBILL_SKIN, BirdType.RAZORBILL), skinHowToGet(PRISM_RAZORBILL_SKIN, BirdType.RAZORBILL)));
        skins.add(new SkinEntry(BirdType.PHOENIX, NOVA_PHOENIX_SKIN, "Nova Phoenix",
                skinDescription(NOVA_PHOENIX_SKIN, BirdType.PHOENIX), skinHowToGet(NOVA_PHOENIX_SKIN, BirdType.PHOENIX)));
        skins.add(new SkinEntry(BirdType.PELICAN, AURORA_PELICAN_SKIN, "Aurora Pelican",
                skinDescription(AURORA_PELICAN_SKIN, BirdType.PELICAN), skinHowToGet(AURORA_PELICAN_SKIN, BirdType.PELICAN)));
        skins.add(new SkinEntry(BirdType.HUMMINGBIRD, SUNFLARE_HUMMINGBIRD_SKIN, "Sunflare Hummingbird",
                skinDescription(SUNFLARE_HUMMINGBIRD_SKIN, BirdType.HUMMINGBIRD), skinHowToGet(SUNFLARE_HUMMINGBIRD_SKIN, BirdType.HUMMINGBIRD)));
        skins.add(new SkinEntry(BirdType.SHOEBILL, GLACIER_SHOEBILL_SKIN, "Glacier Shoebill",
                skinDescription(GLACIER_SHOEBILL_SKIN, BirdType.SHOEBILL), skinHowToGet(GLACIER_SHOEBILL_SKIN, BirdType.SHOEBILL)));
        skins.add(new SkinEntry(BirdType.VULTURE, TIDE_VULTURE_SKIN, "Tide Vulture",
                skinDescription(TIDE_VULTURE_SKIN, BirdType.VULTURE), skinHowToGet(TIDE_VULTURE_SKIN, BirdType.VULTURE)));
        skins.add(new SkinEntry(BirdType.MOCKINGBIRD, ECLIPSE_MOCKINGBIRD_SKIN, "Eclipse Charles",
                skinDescription(ECLIPSE_MOCKINGBIRD_SKIN, BirdType.MOCKINGBIRD), skinHowToGet(ECLIPSE_MOCKINGBIRD_SKIN, BirdType.MOCKINGBIRD)));
        skins.add(new SkinEntry(BirdType.BAT, UMBRA_BAT_SKIN, "Umbra Bat",
                skinDescription(UMBRA_BAT_SKIN, BirdType.BAT), skinHowToGet(UMBRA_BAT_SKIN, BirdType.BAT)));

        for (BirdType type : BirdType.values()) {
            if (type == BirdType.PIGEON || type == BirdType.EAGLE) continue;
            String key = classicSkinDataKey(type);
            String name = classicRewardFor(type);
            skins.add(new SkinEntry(type, key, name, skinDescription(key, type), skinHowToGet(key, type)));
        }
        skins.sort(Comparator
                .comparingInt((SkinEntry entry) -> skinRarityRank(entry.key, entry.bird))
                .thenComparing(entry -> entry.name));
        return skins;
    }

    private List<MapEntry> birdBookMaps() {
        return List.of(
                new MapEntry(MapType.FOREST, "Big Forest", mapDescription(MapType.FOREST), mapHowToGet(MapType.FOREST)),
                new MapEntry(MapType.CITY, "Pigeon's Rooftops", mapDescription(MapType.CITY), mapHowToGet(MapType.CITY)),
                new MapEntry(MapType.SKYCLIFFS, "Sky Cliffs", mapDescription(MapType.SKYCLIFFS), mapHowToGet(MapType.SKYCLIFFS)),
                new MapEntry(MapType.VIBRANT_JUNGLE, "Vibrant Jungle", mapDescription(MapType.VIBRANT_JUNGLE), mapHowToGet(MapType.VIBRANT_JUNGLE)),
                new MapEntry(MapType.CAVE, "Echo Cavern", mapDescription(MapType.CAVE), mapHowToGet(MapType.CAVE)),
                new MapEntry(MapType.BATTLEFIELD, "Battlefield", mapDescription(MapType.BATTLEFIELD), mapHowToGet(MapType.BATTLEFIELD))
        );
    }

    private String mapHowToGet(MapType map) {
        if (map == MapType.CAVE || map == MapType.BATTLEFIELD) {
            return "Card Packs";
        }
        return "Unlocked by default";
    }

    private String mapDescription(MapType map) {
        return switch (map) {
            case CITY -> "Neon rooftops with wind vents that launch surprise comebacks. Tight edges and sudden drafts reward daring movement.";
            case SKYCLIFFS -> "Stepping cliffs stacked into the clouds with strong updrafts. A vertical arena where momentum decides everything.";
            case VIBRANT_JUNGLE -> "Lush canopy with vines, nectar nodes, and layered platforms. Fights swing from open air to close quarters fast.";
            case CAVE -> "Tight corridors and hanging ledges that turn every chase into an ambush. Echoes hide footsteps and reward patient play.";
            case BATTLEFIELD -> "A small island, three platforms, and a deadly void. Every mistake drops you into the abyss.";
            default -> "Dense trees and long platforms for classic brawls. A steady arena that rewards smart positioning.";
        };
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

        Label legacyNote = new Label("Legacy Episodes: older story content with no rewards or unlocks.");
        legacyNote.setFont(Font.font("Consolas", 24));
        legacyNote.setTextFill(Color.web("#FFCC80"));
        legacyNote.setWrapText(true);
        legacyNote.setMaxWidth(1400);
        legacyNote.setTextAlignment(TextAlignment.CENTER);

        VBox header = new VBox(8, title, legacyNote);
        header.setAlignment(Pos.CENTER);
        BorderPane.setAlignment(header, Pos.CENTER);

        VBox pigeonCard = buildEpisodeCard(stage, EpisodeType.PIGEON, "#4FC3F7", "Legacy rooftop skirmishes that introduce the early rivalries.");
        VBox batCard = buildEpisodeCard(stage, EpisodeType.BAT, "#B388FF", "Echo caverns and aerial ambushes where timing decides everything.");
        VBox pelicanCard = buildEpisodeCard(stage, EpisodeType.PELICAN, "#FFD54F", "Iron Beak leads a siege through storm arenas and elite foes.");
        VBox cards = new VBox(18, pigeonCard, batCard, pelicanCard);
        cards.setAlignment(Pos.TOP_CENTER);

        ScrollPane scroll = new ScrollPane(cards);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background: transparent; -fx-background-color: transparent; -fx-control-inner-background: transparent;");
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);

        Button menuButton = uiFactory.action("BACK TO HUB", 520, 95, 34, "#D32F2F", 24, () -> showMenu(stage));
        menuButton.setWrapText(false);
        uiFactory.fitSingleLineOnLayout(menuButton, 34, 20);
        HBox bottom = new HBox(menuButton);
        bottom.setAlignment(Pos.CENTER);

        root.setTop(header);
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
        currentAdventureBattle = null;
        storyModeActive = false;
        storyReplayMode = false;
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
        playMenuMusic();

        ensureAdventureChapterState();
        checkAdventureAchievements();
        if (adventureChapters.length == 0) {
            showMenu(stage);
            return;
        }
        int maxVisibleChapter = 0;
        for (int i = 0; i < adventureChapters.length; i++) {
            if (i == 0 || adventureChapterCompletedByIndex[i - 1]) {
                maxVisibleChapter = i;
            } else {
                break;
            }
        }
        adventureChapterIndex = Math.max(0, Math.min(adventureChapterIndex, maxVisibleChapter));
        adventureChapterProgress = adventureChapterProgressByIndex[adventureChapterIndex];
        adventureChapterCompleted = adventureChapterCompletedByIndex[adventureChapterIndex];

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

        double cardWidth = 1400;
        double cardInnerWidth = cardWidth - 52;

        Label title = new Label("ADVENTURE MODE");
        title.setFont(Font.font("Arial Black", FontWeight.BOLD, 90));
        title.setTextFill(Color.web("#FFD54F"));

        VBox card = new VBox(16);
        card.setAlignment(Pos.CENTER_LEFT);
        card.setPadding(new Insets(26));
        card.setMinWidth(cardWidth);
        card.setPrefWidth(cardWidth);
        card.setMaxWidth(cardWidth);
        card.setStyle("-fx-background-color: rgba(0,0,0,0.6); -fx-background-radius: 24; -fx-border-color: #64B5F6; -fx-border-width: 3; -fx-border-radius: 24;");

        Label chaptersLabel = new Label("CHAPTERS");
        chaptersLabel.setFont(Font.font("Arial Black", 32));
        chaptersLabel.setTextFill(Color.web("#FFECB3"));

        FlowPane chapterRow = new FlowPane(14, 14);
        chapterRow.setAlignment(Pos.CENTER_LEFT);
        chapterRow.setPrefWrapLength(cardInnerWidth);
        chapterRow.setPrefWidth(cardInnerWidth);
        chapterRow.setMaxWidth(cardInnerWidth);
        for (int i = 0; i <= maxVisibleChapter; i++) {
            int idx = i;
            String chapterTitle = adventureChapters[i].title;
            int colon = chapterTitle.indexOf(':');
            if (colon >= 0 && colon + 1 < chapterTitle.length()) {
                chapterTitle = chapterTitle.substring(colon + 1).trim();
            }
            String buttonText = "CHAPTER " + (i + 1) + "\n" + chapterTitle;
            String color = idx == adventureChapterIndex ? "#2E7D32" : "#455A64";
            Button chapterBtn = uiFactory.action(buttonText, 360, 90, 22, color, 18, () -> {
                playButtonClick();
                adventureChapterIndex = idx;
                adventureChapterProgress = adventureChapterProgressByIndex[idx];
                adventureChapterCompleted = adventureChapterCompletedByIndex[idx];
                showAdventureHub(stage);
            });
            chapterRow.getChildren().add(chapterBtn);
        }

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
        summary.setMaxWidth(cardInnerWidth);
        summary.setPrefWidth(cardInnerWidth);
        applyNoEllipsis(summary);

        Label roster = new Label("Adventure Roster: " + adventureRosterText());
        roster.setFont(Font.font("Consolas", 24));
        roster.setTextFill(Color.web("#C5CAE9"));
        roster.setWrapText(true);
        roster.setMaxWidth(cardInnerWidth);
        roster.setPrefWidth(cardInnerWidth);
        applyNoEllipsis(roster);

        HBox actions = new HBox(18);
        actions.setAlignment(Pos.CENTER_LEFT);
        actions.setMaxWidth(cardInnerWidth);
        actions.setPrefWidth(cardInnerWidth);

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
        card.getChildren().addAll(chaptersLabel, chapterRow, chapterTitle, status, summary, roster, actions);

        Button menuBtn = uiFactory.action("BACK TO HUB", 420, 100, 34, "#D32F2F", 24, () -> showMenu(stage));

        root.getChildren().addAll(title, card, menuBtn);

        ScrollPane scroll = wrapInScroll(root);
        Scene scene = new Scene(scroll, WIDTH, HEIGHT);
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
        final boolean[] selectorLocked = new boolean[]{initial != null};
        final boolean[] selectorJustUnlocked = new boolean[]{false};

        BorderPane root = new BorderPane();
        root.setPadding(new Insets(30, 40, 30, 40));
        root.setStyle("-fx-background-color: linear-gradient(to bottom, #07111F, #16263F);");

        Label title = new Label("ADVENTURE - " + battle.title);
        title.setFont(Font.font("Arial Black", FontWeight.BOLD, 72));
        title.setTextFill(Color.web("#FFD54F"));
        BorderPane.setAlignment(title, Pos.CENTER);

        double rightCardWidth = 680;
        double rightCardInnerWidth = rightCardWidth - 44;

        Label info = new Label(battle.briefing + "\nMap: " + mapDisplayName(battle.map) + "\nOpponent: " + battle.opponentName);
        info.setFont(Font.font("Consolas", 24));
        info.setTextFill(Color.web("#B3E5FC"));
        info.setWrapText(true);
        info.setMaxWidth(rightCardInnerWidth);
        info.setPrefWidth(rightCardInnerWidth);
        applyNoEllipsis(info);

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
        rules.setWrapText(true);
        rules.setMaxWidth(rightCardInnerWidth);
        rules.setPrefWidth(rightCardInnerWidth);
        applyNoEllipsis(rules);

        Label selectedLabel = new Label();
        selectedLabel.setFont(Font.font("Arial Black", 38));
        selectedLabel.setTextFill(Color.web("#FAFAFA"));

        Button skinBtn = uiFactory.action("SKIN: BASE", 520, 90, 30, "#37474F", 22, () -> {});
        Button cpuBtn = uiFactory.action("CPU LEVEL: " + cpuLevel[0], 320, 90, 30, "#455A64", 22, () -> {});
        Runnable refreshSkin = () -> {
            BirdType pick = selected[0];
            if (pick == null) {
                skinBtn.setText("SKIN: BASE");
                skinBtn.setDisable(true);
                skinBtn.setOpacity(0.7);
                return;
            }
            List<String> options = adventureSkinOptions(pick);
            if (!options.contains(selectedSkin[0])) {
                selectedSkin[0] = options.get(0);
            }
            skinBtn.setText(adventureSkinLabel(pick, selectedSkin[0]));
            boolean hasAlt = options.size() > 1;
            skinBtn.setDisable(!hasAlt);
            skinBtn.setOpacity(hasAlt ? 1.0 : 0.7);
        };

        skinBtn.setOnAction(e -> {
            playButtonClick();
            if (selected[0] == null) return;
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
        FlowPane optionRow = new FlowPane(16, 12, skinBtn, cpuBtn);
        optionRow.setAlignment(Pos.CENTER);
        optionRow.setPrefWrapLength(rightCardInnerWidth);
        optionRow.setPrefWidth(rightCardInnerWidth);
        optionRow.setMaxWidth(rightCardInnerWidth);

        List<BirdType> gridBirds = new ArrayList<>(allowed);
        Pane selectionPane = new Pane();
        double paneW = 1100;
        double paneH = 520;
        selectionPane.setPrefSize(paneW, paneH);
        selectionPane.setStyle("-fx-background-color: rgba(0,0,0,0.35); -fx-border-color: #90A4AE; -fx-border-width: 3; -fx-background-radius: 20; -fx-border-radius: 20;");
        Rectangle selectionClip = new Rectangle(paneW, paneH);
        selectionClip.widthProperty().bind(selectionPane.widthProperty());
        selectionClip.heightProperty().bind(selectionPane.heightProperty());
        selectionClip.setArcWidth(20);
        selectionClip.setArcHeight(20);
        selectionPane.setClip(selectionClip);

        List<BirdIconSpot> spots = new ArrayList<>();
        Map<BirdType, BirdIconSpot> spotByType = new HashMap<>();

        int columns = gridBirds.size() > 12 ? 4 : 3;
        int rows = (int) Math.ceil(gridBirds.size() / (double) columns);
        double dockW = 150;
        double dockH = 300;
        double dockX = 0;
        double dockY = paneH - dockH;
        double gridX = dockX + dockW + 20;
        double gridY = 10;
        double gridRightPad = 140;
        double gridW = paneW - gridX - gridRightPad;
        double gridH = paneH - 20;
        double cellW = gridW / Math.max(1, columns);
        double cellH = gridH / Math.max(1, rows);
        double iconSize = Math.max(60, Math.min(110, Math.min(cellW, cellH) * 0.65));

        for (int i = 0; i < gridBirds.size(); i++) {
            BirdType type = gridBirds.get(i);
            int col = i % columns;
            int row = i / columns;
            double cellX = gridX + col * cellW;
            double cellY = gridY + row * cellH;
            double centerX = cellX + cellW / 2.0;
            double centerY = cellY + cellH / 2.0;

            Canvas icon = new Canvas(iconSize, iconSize);
            drawRosterSprite(icon, type, null, false);

            Label name = new Label(type.name.toUpperCase());
            name.setFont(Font.font("Consolas", 16));
            name.setTextFill(Color.web("#ECEFF1"));
            name.setWrapText(true);
            name.setAlignment(Pos.CENTER);

            VBox card = new VBox(6, icon, name);
            card.setAlignment(Pos.CENTER);
            card.setPrefWidth(cellW);
            card.setPrefHeight(cellH);
            card.setLayoutX(cellX);
            card.setLayoutY(cellY);
            card.setMouseTransparent(true);

            selectionPane.getChildren().add(card);
            BirdIconSpot spot = new BirdIconSpot(type, false, centerX, centerY);
            spots.add(spot);
            spotByType.put(type, spot);
        }

        Region selectorDock = new Region();
        selectorDock.setPrefSize(dockW, dockH);
        selectorDock.setLayoutX(dockX);
        selectorDock.setLayoutY(dockY);
        selectorDock.setStyle("-fx-background-color: rgba(10,10,10,0.6); -fx-border-color: #FFD54F; -fx-border-width: 2; -fx-background-radius: 18; -fx-border-radius: 18;");
        selectionPane.getChildren().add(selectorDock);

        Label dockLabel = new Label("PLAYER SELECTOR");
        dockLabel.setFont(Font.font("Consolas", 18));
        dockLabel.setTextFill(Color.web("#FFE082"));
        dockLabel.setLayoutX(dockX + 10);
        dockLabel.setLayoutY(dockY + 8);
        selectionPane.getChildren().add(dockLabel);

        Point2D dockPosition = new Point2D(dockX + dockW / 2.0, dockY + dockH / 2.0);

        Circle selector = new Circle(26);
        selector.setFill(Color.web("rgba(255,213,79,0.15)"));
        selector.setStroke(Color.web("#FFD54F"));
        selector.setStrokeWidth(3);
        selector.setManaged(false);
        selectionPane.getChildren().add(selector);

        Text selectorLabel = new Text("P1");
        selectorLabel.setFont(Font.font("Impact", 16));
        selectorLabel.setFill(Color.web("#FFD54F"));
        selectorLabel.setManaged(false);
        selectorLabel.setMouseTransparent(true);
        selectionPane.getChildren().add(selectorLabel);

        Button back = uiFactory.action("BACK", 320, 96, 34, "#546E7A", 22, () -> showAdventureBattleSelect(stage));
        Button start = uiFactory.action("START BATTLE", 420, 96, 34, "#00C853", 24, () -> {
            if (selected[0] == null) return;
            playButtonClick();
            adventureSelectedBird = selected[0];
            adventureSelectedSkinKey = selectedSkin[0];
            battle.cpuLevel = cpuLevel[0];
            startAdventureBattle(stage);
        });

        Runnable refreshSelection = () -> {
            BirdType pick = selected[0];
            boolean hasPick = pick != null;
            selectedLabel.setText(hasPick ? "Selected: " + pick.name : "Selected: SELECT");
            if (hasPick) {
                selectedSkin[0] = normalizeAdventureSkinChoice(pick, selectedSkin[0]);
            } else {
                selectedSkin[0] = null;
            }
            refreshSkin.run();
            start.setDisable(!hasPick);
            start.setOpacity(hasPick ? 1.0 : 0.6);
        };

        final double[] dragOffset = new double[2];
        selector.setOnMousePressed(e -> {
            if (selectorLocked[0]) {
                selectorLocked[0] = false;
                selected[0] = null;
                refreshSelection.run();
                selectorJustUnlocked[0] = true;
            }
            Point2D local = selectionPane.sceneToLocal(e.getSceneX(), e.getSceneY());
            dragOffset[0] = selector.getCenterX() - local.getX();
            dragOffset[1] = selector.getCenterY() - local.getY();
        });
        selector.setOnMouseDragged(e -> {
            if (selectorLocked[0]) return;
            Point2D local = selectionPane.sceneToLocal(e.getSceneX(), e.getSceneY());
            double nx = local.getX() + dragOffset[0];
            double ny = local.getY() + dragOffset[1];
            if (selectorJustUnlocked[0]) {
                selectorJustUnlocked[0] = false;
            }
            double boundW = selectionPane.getWidth() > 0 ? selectionPane.getWidth() : selectionPane.getPrefWidth();
            double boundH = selectionPane.getHeight() > 0 ? selectionPane.getHeight() : selectionPane.getPrefHeight();
            nx = Math.max(40, Math.min(nx, boundW - 40));
            ny = Math.max(40, Math.min(ny, boundH - 40));
            selector.setCenterX(nx);
            selector.setCenterY(ny);
            selectorLabel.setX(nx - 10);
            selectorLabel.setY(ny + 6);
        });
        selector.setOnMouseReleased(e -> {
            if (selectorLocked[0]) return;
            if (selectorJustUnlocked[0]) {
                selectorJustUnlocked[0] = false;
                selector.setCenterX(dockPosition.getX());
                selector.setCenterY(dockPosition.getY());
                selectorLabel.setX(dockPosition.getX() - 10);
                selectorLabel.setY(dockPosition.getY() + 6);
                return;
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
                selectorLabel.setX(best.cx - 10);
                selectorLabel.setY(best.cy + 6);
                selected[0] = best.type;
                selectorLocked[0] = true;
                refreshSelection.run();
            } else {
                selector.setCenterX(dockPosition.getX());
                selector.setCenterY(dockPosition.getY());
                selectorLabel.setX(dockPosition.getX() - 10);
                selectorLabel.setY(dockPosition.getY() + 6);
            }
        });

        VBox rightCard = new VBox(14, info, rules, selectedLabel, optionRow);
        rightCard.setAlignment(Pos.TOP_LEFT);
        rightCard.setPadding(new Insets(22));
        rightCard.setMinWidth(rightCardWidth);
        rightCard.setPrefWidth(rightCardWidth);
        rightCard.setMaxWidth(rightCardWidth);
        rightCard.setStyle("-fx-background-color: rgba(0,0,0,0.56); -fx-border-color: #64B5F6; -fx-border-width: 3; -fx-border-radius: 22; -fx-background-radius: 22;");

        HBox center = new HBox(22, selectionPane, rightCard);
        center.setAlignment(Pos.CENTER);

        HBox bottom = new HBox(18, back, start);
        bottom.setAlignment(Pos.CENTER);

        root.setTop(title);
        root.setCenter(center);
        root.setBottom(bottom);

        Scene scene = new Scene(root, WIDTH, HEIGHT);
        setupKeyboardNavigation(scene);
        applyConsoleHighlight(scene);
        setScenePreservingFullscreen(stage, scene);
        if (selected[0] != null) {
            BirdIconSpot spot = spotByType.get(selected[0]);
            if (spot != null) {
                selector.setCenterX(spot.cx);
                selector.setCenterY(spot.cy);
                selectorLabel.setX(spot.cx - 10);
                selectorLabel.setY(spot.cy + 6);
            }
        } else {
            selector.setCenterX(dockPosition.getX());
            selector.setCenterY(dockPosition.getY());
            selectorLabel.setX(dockPosition.getX() - 10);
            selectorLabel.setY(dockPosition.getY() + 6);
        }
        refreshSelection.run();
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
        competitionSeriesActive = false;
        Arrays.fill(competitionRoundWins, 0);
        Arrays.fill(competitionTeamWins, 0);
        competitionRoundNumber = 1;
        teamModeEnabled = false;
        competitionModeEnabled = false;
        mutatorModeEnabled = false;
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
        Bird opponent = createStoryBird(4200, battle.opponentType, 1, battle.opponentName,
                battle.opponentHealth, battle.opponentPowerMult, battle.opponentSpeedMult, true);
        String opponentSkinKey = adventureOpponentSkinKey(battle);
        applyPreviewSkinChoiceToBird(opponent, battle.opponentType, opponentSkinKey);
    }

    private void applyAdventureBattleArenaModifiers(AdventureBattle battle) {
        if (battle == null) return;
        if (adventureChapterIndex == 0) {
            switch (adventureBattleIndex) {
                case 0 -> {
                    addToKillFeed("ROOFTOP SPRINT: Neon signals boost speed.");
                    powerUps.add(new PowerUp(1200, GROUND_Y - 760, PowerUpType.NEON));
                    powerUps.add(new PowerUp(3200, GROUND_Y - 820, PowerUpType.SPEED));
                }
                case 1 -> {
                    addToKillFeed("OLD FRIEND RUN: Thermal vents surge on the cliffs.");
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
            return;
        }
        if (adventureChapterIndex != 6) return;
        switch (adventureBattleIndex) {
            case 0 -> {
                addToKillFeed("MIRROR SPRINT: Twin signals bend the rooftops.");
                powerUps.add(new PowerUp(1200, GROUND_Y - 760, PowerUpType.NEON));
                powerUps.add(new PowerUp(2200, GROUND_Y - 640, PowerUpType.TITAN));
                powerUps.add(new PowerUp(3200, GROUND_Y - 860, PowerUpType.SPEED));
                powerUps.add(new PowerUp(4700, GROUND_Y - 760, PowerUpType.SHRINK));
                windVents.add(new WindVent(1600, GROUND_Y - 340, 500));
                windVents.add(new WindVent(4200, GROUND_Y - 360, 520));
                adventureMatchTimerOverride = 80 * 60;
            }
            case 1 -> {
                addToKillFeed("SHARD REVERSAL: Crystal echoes distort the cave air.");
                powerUps.add(new PowerUp(1400, GROUND_Y - 980, PowerUpType.OVERCHARGE));
                powerUps.add(new PowerUp(3000, GROUND_Y - 1040, PowerUpType.SHRINK));
                powerUps.add(new PowerUp(4500, GROUND_Y - 860, PowerUpType.HEALTH));
                for (int i = 0; i < 3; i++) {
                    double cx = 1000 + random.nextDouble() * 4000;
                    double cy = 260 + random.nextDouble() * 520;
                    crowMinions.add(new CrowMinion(cx, cy, null));
                }
                adventureMatchTimerOverride = 85 * 60;
            }
            case 2 -> {
                addToKillFeed("BEACON PARADOX: The light fights back.");
                powerUps.add(new PowerUp(1200, GROUND_Y - 760, PowerUpType.RAGE));
                powerUps.add(new PowerUp(3000, GROUND_Y - 960, PowerUpType.OVERCHARGE));
                powerUps.add(new PowerUp(4600, GROUND_Y - 760, PowerUpType.HEALTH));
                powerUps.add(new PowerUp(3000, GROUND_Y - 600, PowerUpType.TITAN));
                windVents.add(new WindVent(2200, GROUND_Y - 320, 520));
                windVents.add(new WindVent(3600, GROUND_Y - 340, 500));
                for (int i = 0; i < 4; i++) {
                    double cx = 900 + random.nextDouble() * 4200;
                    double cy = 240 + random.nextDouble() * 520;
                    crowMinions.add(new CrowMinion(cx, cy, null));
                }
                adventureMatchTimerOverride = 90 * 60;
            }
        }
    }

    private void buildBirdBookPowerUpsGrid(FlowPane grid, VBox sidebar) {
        Runnable[] initial = new Runnable[1];
        Runnable[] fallback = new Runnable[1];

        for (ItemEntry entry : birdBookPowerUps()) {
            Color accent = entry.powerUp != null ? entry.powerUp.color : Color.web("#FFD54F");
            Node icon = entry.unlocked
                    ? buildPowerUpTileIcon(entry.powerUp)
                    : buildLockedTileIcon(accent);
            Button tile = createBirdBookTile(grid, entry.name, icon, entry.unlocked, accent,
                    () -> showItemSidebar(sidebar, entry));
            grid.getChildren().add(tile);

            Runnable select = () -> {
                selectBirdBookTile(grid, tile);
                showItemSidebar(sidebar, entry);
            };
            if (fallback[0] == null) fallback[0] = select;
            if (initial[0] == null && entry.unlocked) initial[0] = select;
        }

        if (initial[0] != null) {
            initial[0].run();
        } else if (fallback[0] != null) {
            fallback[0].run();
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
            queueUnlockCardForBird(battle.unlockReward);
            saveAchievements();
        }

        boolean chapterJustCompleted = false;
        if (!adventureReplayMode && adventureBattleIndex >= adventureChapterProgress) {
            boolean wasCompleted = adventureChapterCompleted;
            adventureChapterProgress = Math.min(adventureBattleIndex + 1, chapter.battles.length);
            if (adventureChapterProgress >= chapter.battles.length) {
                adventureChapterCompleted = true;
            }
            chapterJustCompleted = !wasCompleted && adventureChapterCompleted;
            ensureAdventureChapterState();
            if (adventureChapterIndex >= 0 && adventureChapterIndex < adventureChapterProgressByIndex.length) {
                adventureChapterProgressByIndex[adventureChapterIndex] = adventureChapterProgress;
                adventureChapterCompletedByIndex[adventureChapterIndex] = adventureChapterCompleted;
            }
            saveAchievements();
            checkAdventureAchievements();
        }
        if (chapterJustCompleted && adventureChapterIndex == beaconChapterIndex() && !beaconPigeonUnlocked) {
            beaconPigeonUnlocked = true;
            queueUnlockCardForSkin(BirdType.PIGEON, BEACON_PIGEON_SKIN);
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

        showAdventureDialogue(stage, "Victory", battle.winDialogue, () -> runAfterUnlockCards(stage, afterWin));
    }

    private void showAdventureDialogue(Stage stage, String titleText, AdventureDialogueLine[] lines, Runnable onComplete) {
        playMenuMusic();
        if (lines == null || lines.length == 0) {
            if (onComplete != null) onComplete.run();
            return;
        }

        activeAdventureDialogueTitle = titleText;
        activeAdventureDialogueChapterIndex = adventureChapterIndex;

        Canvas canvas = new Canvas(WIDTH, HEIGHT);
        GraphicsContext g = canvas.getGraphicsContext2D();
        final int[] idx = {0};

        Runnable render = () -> renderAdventureDialogueFrame(g, titleText, lines[idx[0]]);

        StackPane root = new StackPane(canvas);
        root.getProperties().put("noAutoScale", true);
        Scene scene = new Scene(root, WIDTH, HEIGHT);
        canvas.widthProperty().bind(scene.widthProperty());
        canvas.heightProperty().bind(scene.heightProperty());
        canvas.widthProperty().addListener((obs, oldVal, newVal) -> render.run());
        canvas.heightProperty().addListener((obs, oldVal, newVal) -> render.run());
        setupKeyboardNavigation(scene);
        applyConsoleHighlight(scene);
        setScenePreservingFullscreen(stage, scene);
        javafx.application.Platform.runLater(render);

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
        double width = g.getCanvas().getWidth();
        double height = g.getCanvas().getHeight();
        if (width <= 0 || height <= 0) return;
        g.clearRect(0, 0, width, height);
        Stop[] stops = new Stop[] {
                new Stop(0, Color.web("#071122")),
                new Stop(1, Color.web("#1D2C4C"))
        };
        LinearGradient bg = new LinearGradient(0, 0, 0, 1, true, CycleMethod.NO_CYCLE, stops);
        g.setFill(bg);
        g.fillRect(0, 0, width, height);

        g.setFill(Color.web("#FFD54F"));
        g.setFont(Font.font("Arial Black", 64));
        g.fillText(titleText, 80, 90);

        double groundY = height - 220;
        g.setFill(Color.rgb(14, 18, 28));
        g.fillRect(0, groundY, width, height - groundY);
        g.setStroke(Color.rgb(70, 80, 100, 0.7));
        g.setLineWidth(4);
        g.strokeLine(0, groundY, width, groundY);

        Bird left = null;
        Bird right = null;
        double birdMargin = Math.max(200, width * 0.14);
        double leftX = birdMargin;
        double rightX = width - Math.max(260, width * 0.18);
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
        double margin = Math.max(60, width * 0.05);
        double bubbleMaxWidth = Math.min(980, Math.max(240, width - margin * 2));

        List<String> wrapped = wrapTextToLines(line.text, textFont, bubbleMaxWidth - padding * 2);
        double lineH = measureTextHeight("Ag", textFont) * 1.18;
        double nameH = measureTextHeight("Ag", nameFont);
        double bubbleW = Math.min(bubbleMaxWidth, Math.max(420, maxLineWidth(wrapped, textFont) + padding * 2));
        bubbleW = Math.min(bubbleW, Math.max(240, width - margin * 2));
        double bubbleH = padding * 2 + nameH + (wrapped.isEmpty() ? 0 : 10) + lineH * wrapped.size();
        double bubbleX = line.speakerSide == DialogueSide.LEFT ? margin : width - bubbleW - margin;
        bubbleX = Math.max(margin, Math.min(bubbleX, width - bubbleW - margin));
        double bubbleY = Math.max(80, height * 0.12);

        double headX = speaker != null ? speaker.x + 40 * speaker.sizeMultiplier : width / 2.0;
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

        String continueText = "Click to continue";
        Font continueFont = Font.font("Consolas", 22);
        double continueW = measureTextWidth(continueText, continueFont);
        double continueX = Math.max(20, width - continueW - 24);
        double continueY = Math.max(32, height - 36);
        g.setFill(Color.web("#C5CAE9"));
        g.setFont(continueFont);
        g.fillText(continueText, continueX, continueY);
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
        String skinKey = dialogueSkinKeyFor(type);
        if (skinKey != null) {
            applyPreviewSkinChoiceToBird(b, type, skinKey);
        }
        return b;
    }

    private int beaconChapterIndex() {
        for (int i = 0; i < adventureChapters.length; i++) {
            String title = adventureChapters[i].title;
            if (title != null && title.contains("Signal of the Beacon")) return i;
        }
        return 4;
    }

    private boolean shouldShowBeaconPigeonSkin() {
        if (activeAdventureDialogueTitle == null) return false;
        int beaconIndex = beaconChapterIndex();
        if (activeAdventureDialogueChapterIndex > beaconIndex) return true;
        return "Chapter Complete".equals(activeAdventureDialogueTitle)
                && activeAdventureDialogueChapterIndex == beaconIndex;
    }

    private String dialogueSkinKeyFor(BirdType type) {
        if (type == null) return null;
        if (type == BirdType.HUMMINGBIRD) return classicSkinDataKey(type);
        if (type == BirdType.PIGEON && shouldShowBeaconPigeonSkin()) return BEACON_PIGEON_SKIN;
        return null;
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
                Arrays.fill(competitionRoundWins, 0);
                Arrays.fill(competitionTeamWins, 0);
                competitionRoundNumber = 1;
            }
            refreshSettingsToggleButton(mutatorToggle, "MUTATOR MODE", mutatorModeEnabled);
            refreshSettingsToggleButton(compToggle, "COMPETITION MODE", competitionModeEnabled);
            saveAchievements();
        });
        compToggle.setOnAction(e -> {
            playButtonClick();
            competitionModeEnabled = !competitionModeEnabled;
            if (competitionModeEnabled && mutatorModeEnabled) {
                mutatorModeEnabled = false;
            } else if (!competitionModeEnabled) {
                competitionSeriesActive = false;
                Arrays.fill(competitionRoundWins, 0);
                Arrays.fill(competitionTeamWins, 0);
                competitionRoundNumber = 1;
            }
            refreshSettingsToggleButton(mutatorToggle, "MUTATOR MODE", mutatorModeEnabled);
            refreshSettingsToggleButton(compToggle, "COMPETITION MODE", competitionModeEnabled);
            saveAchievements();
        });

        refreshSettingsToggleButton(mutatorToggle, "MUTATOR MODE", mutatorModeEnabled);
        refreshSettingsToggleButton(compToggle, "COMPETITION MODE", competitionModeEnabled);

        Label mutatorInfo = new Label(
                "Mutator Mode:\n" +
                "- Randomly activates 1 mutator each match.\n" +
                "- Variety includes: Rage Frenzy, Titan Rumble, Overcharge Brawl, Crow Surge, Turbo Brawl."
        );
        mutatorInfo.setFont(Font.font("Consolas", 24));
        mutatorInfo.setTextFill(Color.web("#B3E5FC"));
        mutatorInfo.setWrapText(true);
        mutatorInfo.setMaxWidth(1100);
        mutatorInfo.setTextAlignment(TextAlignment.LEFT);
        mutatorInfo.setAlignment(Pos.CENTER_LEFT);
        applyNoEllipsis(mutatorInfo);

        Label compInfo = new Label(
                "Competition Mode (Tournament Rules):\n" +
                "- No power-up spawns.\n" +
                "- Fixed 120 second round timer.\n" +
                "- No sudden-death crows or random city wind bursts.\n" +
                "- On timeout, highest-health bird wins (damage dealt as tie-breaker)."
        );
        compInfo.setFont(Font.font("Consolas", 24));
        compInfo.setTextFill(Color.web("#FFECB3"));
        compInfo.setWrapText(true);
        compInfo.setMaxWidth(1100);
        compInfo.setTextAlignment(TextAlignment.LEFT);
        compInfo.setAlignment(Pos.CENTER_LEFT);
        applyNoEllipsis(compInfo);

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
        buttons.getChildren().add(back);

        root.getChildren().addAll(title, card, buttons);
        Scene scene = new Scene(root, WIDTH, HEIGHT);
        bindEscape(scene, back);
        setupKeyboardNavigation(scene);
        applyConsoleHighlight(scene);
        setScenePreservingFullscreen(stage, scene);
        mutatorToggle.requestFocus();
    }

    private void showMainSettings(Stage stage) {
        playMenuMusic();

        VBox root = new VBox(24);
        root.setAlignment(Pos.CENTER);
        root.setPadding(new Insets(50));
        root.setStyle("-fx-background-color: linear-gradient(to bottom, #0B1624, #1A2738);");

        Label title = new Label("SETTINGS");
        title.setFont(Font.font("Arial Black", FontWeight.BOLD, 86));
        title.setTextFill(Color.GOLD);

        VBox card = new VBox(18);
        card.setAlignment(Pos.CENTER_LEFT);
        card.setPadding(new Insets(26));
        card.setMaxWidth(1320);
        card.setStyle("-fx-background-color: rgba(0,0,0,0.58); -fx-background-radius: 22; -fx-border-color: #90CAF9; -fx-border-width: 3; -fx-border-radius: 22;");

        Button musicToggle = uiFactory.action("", 620, 96, 34, "#546E7A", 24, () -> {});
        Button sfxToggle = uiFactory.action("", 620, 96, 34, "#546E7A", 24, () -> {});
        Button shakeToggle = uiFactory.action("", 620, 96, 34, "#546E7A", 24, () -> {});
        Button fullscreenToggle = uiFactory.action("", 620, 96, 34, "#546E7A", 24, () -> {});
        Button particlesToggle = uiFactory.action("", 620, 96, 34, "#546E7A", 24, () -> {});
        Button ambientToggle = uiFactory.action("", 620, 96, 34, "#546E7A", 24, () -> {});
        Button fpsCapToggle = uiFactory.action("", 620, 96, 34, "#546E7A", 24, () -> {});

        musicToggle.setOnAction(e -> {
            playButtonClick();
            musicEnabled = !musicEnabled;
            if (!musicEnabled) {
                if (musicPlayer != null) musicPlayer.stop();
                if (menuMusicPlayer != null) menuMusicPlayer.stop();
                if (victoryMusicPlayer != null) victoryMusicPlayer.stop();
            } else {
                playMenuMusic();
            }
            refreshSettingsToggleButton(musicToggle, "MUSIC", musicEnabled);
            saveAchievements();
        });
        sfxToggle.setOnAction(e -> {
            playButtonClick();
            sfxEnabled = !sfxEnabled;
            refreshSettingsToggleButton(sfxToggle, "SFX", sfxEnabled);
            saveAchievements();
        });
        shakeToggle.setOnAction(e -> {
            playButtonClick();
            screenShakeEnabled = !screenShakeEnabled;
            if (!screenShakeEnabled) shakeIntensity = 0;
            refreshSettingsToggleButton(shakeToggle, "SCREEN SHAKE", screenShakeEnabled);
            saveAchievements();
        });
        fullscreenToggle.setOnAction(e -> {
            playButtonClick();
            fullscreenEnabled = !fullscreenEnabled;
            if (stage != null) stage.setFullScreen(fullscreenEnabled);
            refreshSettingsToggleButton(fullscreenToggle, "FULLSCREEN", fullscreenEnabled);
            saveAchievements();
        });
        particlesToggle.setOnAction(e -> {
            playButtonClick();
            particleEffectsEnabled = !particleEffectsEnabled;
            if (!particleEffectsEnabled) particles.clear();
            refreshSettingsToggleButton(particlesToggle, "PARTICLES", particleEffectsEnabled);
            saveAchievements();
        });
        ambientToggle.setOnAction(e -> {
            playButtonClick();
            ambientEffectsEnabled = !ambientEffectsEnabled;
            refreshSettingsToggleButton(ambientToggle, "AMBIENT FX", ambientEffectsEnabled);
            saveAchievements();
        });
        fpsCapToggle.setOnAction(e -> {
            playButtonClick();
            cycleFpsCap(fpsCapToggle);
        });

        refreshSettingsToggleButton(musicToggle, "MUSIC", musicEnabled);
        refreshSettingsToggleButton(sfxToggle, "SFX", sfxEnabled);
        refreshSettingsToggleButton(shakeToggle, "SCREEN SHAKE", screenShakeEnabled);
        refreshSettingsToggleButton(fullscreenToggle, "FULLSCREEN", fullscreenEnabled);
        refreshSettingsToggleButton(particlesToggle, "PARTICLES", particleEffectsEnabled);
        refreshSettingsToggleButton(ambientToggle, "AMBIENT FX", ambientEffectsEnabled);
        refreshFpsCapButton(fpsCapToggle);

        VBox musicRow = buildSettingsRow(musicToggle, "Menu, match, and victory tracks.", "#B3E5FC");
        VBox sfxRow = buildSettingsRow(sfxToggle, "Hits, UI clicks, and hazard sounds.", "#B3E5FC");
        VBox shakeRow = buildSettingsRow(shakeToggle, "Camera jolts on big hits.", "#C5E1A5");
        VBox fullscreenRow = buildSettingsRow(fullscreenToggle, "Toggle windowed/fullscreen display.", "#C5E1A5");
        VBox particlesRow = buildSettingsRow(particlesToggle, "Damage sparks and ability particles.", "#FFE0B2");
        VBox ambientRow = buildSettingsRow(ambientToggle, "Animated background ambience and glow.", "#FFE0B2");
        VBox fpsRow = buildSettingsRow(fpsCapToggle, "Limits render FPS without changing game speed.", "#FFE0B2");

        VBox audioPanel = new VBox(16, musicRow, sfxRow);
        audioPanel.setAlignment(Pos.CENTER_LEFT);

        VBox displayPanel = new VBox(16, shakeRow, fullscreenRow);
        displayPanel.setAlignment(Pos.CENTER_LEFT);

        VBox graphicsPanel = new VBox(16, particlesRow, ambientRow, fpsRow);
        graphicsPanel.setAlignment(Pos.CENTER_LEFT);

        VBox contentHolder = new VBox(18);
        contentHolder.setAlignment(Pos.CENTER_LEFT);
        contentHolder.getChildren().add(audioPanel);

        Button audioTab = uiFactory.action("AUDIO", 240, 70, 26, "#455A64", 18, null);
        Button displayTab = uiFactory.action("DISPLAY", 240, 70, 26, "#455A64", 18, null);
        Button graphicsTab = uiFactory.action("GRAPHICS", 240, 70, 26, "#455A64", 18, null);

        Runnable showAudio = () -> {
            setSettingsTabActive(audioTab, true);
            setSettingsTabActive(displayTab, false);
            setSettingsTabActive(graphicsTab, false);
            contentHolder.getChildren().setAll(audioPanel);
        };
        Runnable showDisplay = () -> {
            setSettingsTabActive(audioTab, false);
            setSettingsTabActive(displayTab, true);
            setSettingsTabActive(graphicsTab, false);
            contentHolder.getChildren().setAll(displayPanel);
        };
        Runnable showGraphics = () -> {
            setSettingsTabActive(audioTab, false);
            setSettingsTabActive(displayTab, false);
            setSettingsTabActive(graphicsTab, true);
            contentHolder.getChildren().setAll(graphicsPanel);
        };

        audioTab.setOnAction(e -> {
            playButtonClick();
            showAudio.run();
        });
        displayTab.setOnAction(e -> {
            playButtonClick();
            showDisplay.run();
        });
        graphicsTab.setOnAction(e -> {
            playButtonClick();
            showGraphics.run();
        });

        HBox tabs = new HBox(16, audioTab, displayTab, graphicsTab);
        tabs.setAlignment(Pos.CENTER);
        tabs.setMaxWidth(Double.MAX_VALUE);

        showAudio.run();

        card.getChildren().addAll(tabs, contentHolder);

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
        buttons.getChildren().add(back);

        root.getChildren().addAll(title, card, buttons);
        Scene scene = new Scene(root, WIDTH, HEIGHT);
        bindEscape(scene, back);
        setupKeyboardNavigation(scene);
        applyConsoleHighlight(scene);
        setScenePreservingFullscreen(stage, scene);
        musicToggle.requestFocus();
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
            case MOCKINGBIRD -> "Velvet Bosskhead Charles";
            case RAZORBILL -> "Storm Razorbill";
            case GRINCHHAWK -> "Krampus Hawk";
            case VULTURE -> "Ashen Vulture";
            case OPIUMBIRD -> "Nightshade Opium Bird";
            case HEISENBIRD -> "Blue Sky Heisenbird";
            case TITMOUSE -> "Volt Titmouse";
            case BAT -> "Moonlit Bat";
            case PELICAN -> "Titan Pelican";
            case RAVEN -> "Nightshade Raven";
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
        if (isClassicRewardUnlocked(type)) return;
        if (type == BirdType.PIGEON) {
            noirPigeonUnlocked = true;
            classicSkinUnlocked[type.ordinal()] = true;
            queueUnlockCardForSkin(BirdType.PIGEON, "NOIR_PIGEON");
        } else if (type == BirdType.EAGLE) {
            eagleSkinUnlocked = true;
            classicSkinUnlocked[type.ordinal()] = true;
            queueUnlockCardForSkin(BirdType.EAGLE, "SKY_KING_EAGLE");
        } else {
            classicSkinUnlocked[type.ordinal()] = true;
            queueUnlockCardForSkin(type, classicSkinDataKey(type));
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
            case HEISENBIRD -> Color.web("#42A5F5");
            case TITMOUSE -> Color.web("#FFCA28");
            case BAT -> Color.web("#5E35B1");
            case PELICAN -> Color.web("#FFB74D");
            case RAVEN -> Color.web("#263238");
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
            case HEISENBIRD -> Color.web("#B3E5FC");
            case TITMOUSE -> Color.web("#FFF59D");
            case BAT -> Color.web("#D1C4E9");
            case PELICAN -> Color.web("#FFE0B2");
            case RAVEN -> Color.web("#B0BEC5");
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

    private List<MapType> classicMapPool() {
        List<MapType> maps = new ArrayList<>();
        for (MapType map : MapType.values()) {
            if (isMapUnlocked(map)) {
                maps.add(map);
            }
        }
        if (maps.isEmpty()) {
            maps.add(MapType.FOREST);
        }
        return maps;
    }

    private MapType pickClassicMap(Set<MapType> used) {
        List<MapType> maps = classicMapPool();
        maps.removeIf(used::contains);
        if (maps.isEmpty()) maps = classicMapPool();
        if (classicLastMap != null && maps.size() > 1) {
            maps.removeIf(map -> map == classicLastMap);
            if (maps.isEmpty()) maps = classicMapPool();
        }
        MapType map = maps.get(random.nextInt(maps.size()));
        used.add(map);
        classicLastMap = map;
        return map;
    }

    private MatchMutator pickClassicMutator(MatchMutator... options) {
        if (options == null || options.length == 0) return MatchMutator.NONE;
        List<MatchMutator> pool = new ArrayList<>(Arrays.asList(options));
        pool.removeIf(classicUsedMutators::contains);
        if (pool.isEmpty()) pool = new ArrayList<>(Arrays.asList(options));
        if (classicLastMutator != null && pool.size() > 1) {
            pool.removeIf(mutator -> mutator == classicLastMutator);
            if (pool.isEmpty()) pool = new ArrayList<>(Arrays.asList(options));
        }
        MatchMutator pick = pool.get(random.nextInt(pool.size()));
        classicLastMutator = pick;
        classicUsedMutators.add(pick);
        return pick;
    }

    private ClassicTwist pickClassicTwist(ClassicTwist... options) {
        if (options == null || options.length == 0) return ClassicTwist.STORM_LIFTS;
        List<ClassicTwist> pool = new ArrayList<>(Arrays.asList(options));
        pool.removeIf(classicUsedTwists::contains);
        if (pool.isEmpty()) pool = new ArrayList<>(Arrays.asList(options));
        if (classicLastTwist != null && pool.size() > 1) {
            pool.removeIf(twist -> twist == classicLastTwist);
            if (pool.isEmpty()) pool = new ArrayList<>(Arrays.asList(options));
        }
        ClassicTwist pick = pool.get(random.nextInt(pool.size()));
        classicLastTwist = pick;
        classicUsedTwists.add(pick);
        return pick;
    }

    private String bossSkinKeyFor(BirdType type) {
        if (type == null) return null;
        return switch (type) {
            case PIGEON -> BEACON_PIGEON_SKIN;
            case EAGLE -> "SKY_KING_EAGLE";
            case PHOENIX -> NOVA_PHOENIX_SKIN;
            case FALCON -> DUNE_FALCON_SKIN;
            case PENGUIN -> MINT_PENGUIN_SKIN;
            case TITMOUSE -> CIRCUIT_TITMOUSE_SKIN;
            case RAZORBILL -> PRISM_RAZORBILL_SKIN;
            case PELICAN -> AURORA_PELICAN_SKIN;
            case HUMMINGBIRD -> SUNFLARE_HUMMINGBIRD_SKIN;
            case SHOEBILL -> GLACIER_SHOEBILL_SKIN;
            case VULTURE -> TIDE_VULTURE_SKIN;
            case MOCKINGBIRD -> ECLIPSE_MOCKINGBIRD_SKIN;
            case BAT -> UMBRA_BAT_SKIN;
            default -> classicSkinDataKey(type);
        };
    }

    private ClassicFighter classicFighter(BirdType type, String title, double health, double powerMult, double speedMult) {
        return new ClassicFighter(type, title, health, powerMult, speedMult);
    }

    private ClassicFighter classicFighter(BirdType type, String title, double health, double powerMult, double speedMult, String skinKey) {
        return new ClassicFighter(type, title, health, powerMult, speedMult, skinKey);
    }

    private ClassicFighter classicFighterWithCpu(BirdType type, String title, double health, double powerMult, double speedMult, int cpuLevel) {
        return new ClassicFighter(type, title, health, powerMult, speedMult, null, cpuLevel);
    }

    private ClassicFighter classicBossFighter(BirdType type, String title, double health, double powerMult, double speedMult) {
        return classicFighter(type, title, health, powerMult, speedMult, bossSkinKeyFor(type));
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
        classicLastMap = null;
        classicLastTwist = null;
        classicLastMutator = null;
        classicUsedTwists.clear();
        classicUsedMutators.clear();

        int roundOneVariant = random.nextInt(3);
        if (roundOneVariant == 0) {
            BirdType r1 = pickClassicEnemy(playerType, usedBirds);
            run.add(new ClassicEncounter(
                    "Qualifier: First Impact",
                    "Skycaster",
                    "One rival enters. Win clean and establish tempo.",
                    pickClassicMap(usedMaps),
                    pickClassicMutator(MatchMutator.NONE, MatchMutator.RAGE_FRENZY),
                    pickClassicTwist(ClassicTwist.STORM_LIFTS, ClassicTwist.NECTAR_BLOOM, ClassicTwist.WIND_RALLY, ClassicTwist.VINE_SURGE),
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
                    pickClassicMutator(MatchMutator.NONE, MatchMutator.OVERCHARGE_BRAWL),
                    pickClassicTwist(ClassicTwist.NECTAR_BLOOM, ClassicTwist.SHADOW_CACHE, ClassicTwist.MEDIC_CACHE),
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
                    "Hazard Pulse",
                    "Arena Warden",
                    "A tuned rival with hazards online early. Survive the opening storm.",
                    pickClassicMap(usedMaps),
                    pickClassicMutator(MatchMutator.TITAN_RUMBLE, MatchMutator.CROW_SURGE),
                    pickClassicTwist(ClassicTwist.SHOCK_DROPS, ClassicTwist.TITAN_CACHE, ClassicTwist.OVERCHARGE_FURY),
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
                    pickClassicMutator(MatchMutator.RAGE_FRENZY, MatchMutator.OVERCHARGE_BRAWL, MatchMutator.NONE),
                    pickClassicTwist(ClassicTwist.TITAN_CACHE, ClassicTwist.SHOCK_DROPS, ClassicTwist.OVERCHARGE_FURY),
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
                    pickClassicMutator(MatchMutator.TURBO_BRAWL, MatchMutator.OVERCHARGE_BRAWL),
                    pickClassicTwist(ClassicTwist.RAGE_RITUAL, ClassicTwist.NECTAR_BLOOM, ClassicTwist.MEDIC_CACHE),
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
                    pickClassicMutator(MatchMutator.TITAN_RUMBLE, MatchMutator.TURBO_BRAWL),
                    pickClassicTwist(ClassicTwist.WIND_RALLY, ClassicTwist.SHADOW_CACHE, ClassicTwist.VINE_SURGE),
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
                    pickClassicMutator(MatchMutator.TURBO_BRAWL, MatchMutator.OVERCHARGE_BRAWL),
                    pickClassicTwist(ClassicTwist.NECTAR_BLOOM, ClassicTwist.STORM_LIFTS, ClassicTwist.VINE_SURGE),
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
                    pickClassicMutator(MatchMutator.CROW_SURGE, MatchMutator.RAGE_FRENZY),
                    pickClassicTwist(ClassicTwist.CROW_CARNIVAL, ClassicTwist.SHADOW_CACHE, ClassicTwist.MEDIC_CACHE),
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
                    pickClassicMutator(MatchMutator.TITAN_RUMBLE, MatchMutator.TURBO_BRAWL),
                    pickClassicTwist(ClassicTwist.RAGE_RITUAL, ClassicTwist.TITAN_CACHE, ClassicTwist.OVERCHARGE_FURY),
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
                    pickClassicTwist(ClassicTwist.CROW_CARNIVAL, ClassicTwist.SHOCK_DROPS, ClassicTwist.OVERCHARGE_FURY),
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
                    pickClassicMutator(MatchMutator.TITAN_RUMBLE, MatchMutator.CROW_SURGE),
                    pickClassicTwist(ClassicTwist.WIND_RALLY, ClassicTwist.STORM_LIFTS, ClassicTwist.VINE_SURGE),
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
                    pickClassicMutator(MatchMutator.TURBO_BRAWL, MatchMutator.OVERCHARGE_BRAWL),
                    pickClassicTwist(ClassicTwist.TITAN_CACHE, ClassicTwist.RAGE_RITUAL, ClassicTwist.MEDIC_CACHE),
                    100 * 60,
                    new ClassicFighter[0],
                    new ClassicFighter[]{
                            classicBossFighter(miniBoss, "Boss: " + miniBoss.name, 180, 1.22, 1.1),
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
                    pickClassicMutator(MatchMutator.TURBO_BRAWL, MatchMutator.CROW_SURGE, MatchMutator.TITAN_RUMBLE),
                    pickClassicTwist(ClassicTwist.TITAN_CACHE, ClassicTwist.STORM_LIFTS, ClassicTwist.CROW_CARNIVAL, ClassicTwist.OVERCHARGE_FURY),
                    110 * 60,
                    new ClassicFighter[]{
                            classicFighter(r5ally, "Ally: " + r5ally.name, 112, 1.04, 1.08)
                    },
                    new ClassicFighter[]{
                            classicBossFighter(boss, "Boss: " + boss.name, 210, 1.32, 1.08),
                            classicBossFighter(lieutenant, "Boss: " + lieutenant.name, 175, 1.24, 1.12)
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
                    pickClassicMutator(MatchMutator.TURBO_BRAWL, MatchMutator.CROW_SURGE, MatchMutator.TITAN_RUMBLE),
                    pickClassicTwist(ClassicTwist.TITAN_CACHE, ClassicTwist.WIND_RALLY, ClassicTwist.RAGE_RITUAL, ClassicTwist.MEDIC_CACHE),
                    110 * 60,
                    new ClassicFighter[0],
                    new ClassicFighter[]{
                            classicBossFighter(boss, "Boss: " + boss.name, 220, 1.34, 1.1),
                            classicFighterWithCpu(lieutenant, "Elite: " + lieutenant.name, 90, 1.18, 1.12, 2),
                            classicFighterWithCpu(elite, "Elite: " + elite.name, 80, 1.16, 1.12, 2)
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
        classicDeaths = 0;
        classicTeamMode = false;
        Arrays.fill(classicTeams, 1);
        playMenuMusic();
        checkClassicAchievements();

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

        List<BirdType> availableBirds = unlockedBirdPool();
        BirdType classicPick = isBirdUnlocked(classicSelectedBird) ? classicSelectedBird : firstUnlockedBird();
        if (classicPick == null && !availableBirds.isEmpty()) {
            classicPick = availableBirds.get(0);
        }
        final BirdType[] selected = new BirdType[]{classicPick};
        String initialSkin = normalizeAdventureSkinChoice(classicPick, classicSelectedSkinKey);
        final String[] selectedSkin = new String[]{initialSkin};
        final boolean[] selectorLocked = new boolean[]{classicPick != null};
        final boolean[] selectorJustUnlocked = new boolean[]{false};

        Pane selectionPane = new Pane();
        double paneW = 1100;
        double paneH = 520;
        selectionPane.setPrefSize(paneW, paneH);
        selectionPane.setStyle("-fx-background-color: rgba(0,0,0,0.35); -fx-border-color: #90A4AE; -fx-border-width: 3; -fx-background-radius: 20; -fx-border-radius: 20;");

        List<BirdIconSpot> spots = new ArrayList<>();
        Map<BirdType, BirdIconSpot> spotByType = new HashMap<>();

        int columns = availableBirds.size() > 12 ? 4 : 3;
        int rows = (int) Math.ceil(availableBirds.size() / (double) columns);
        double dockW = 150;
        double dockH = 300;
        double dockX = 0;
        double dockY = paneH - dockH;
        double gridX = dockX + dockW + 20;
        double gridY = 10;
        double gridW = paneW - gridX - 20;
        double gridH = paneH - 20;
        double cellW = gridW / Math.max(1, columns);
        double cellH = gridH / Math.max(1, rows);
        double iconSize = Math.max(60, Math.min(110, Math.min(cellW, cellH) * 0.65));

        for (int i = 0; i < availableBirds.size(); i++) {
            BirdType type = availableBirds.get(i);
            int col = i % columns;
            int row = i / columns;
            double cellX = gridX + col * cellW;
            double cellY = gridY + row * cellH;
            double centerX = cellX + cellW / 2.0;
            double centerY = cellY + cellH / 2.0;

            Canvas icon = new Canvas(iconSize, iconSize);
            drawRosterSprite(icon, type, null, false);

            Label name = new Label(type.name.toUpperCase());
            name.setFont(Font.font("Consolas", 16));
            name.setTextFill(Color.web("#ECEFF1"));
            name.setWrapText(true);
            name.setAlignment(Pos.CENTER);

            boolean completed = isClassicCompleted(type);
            Label badge = new Label("BADGE");
            badge.setFont(Font.font("Consolas", 12));
            badge.setTextFill(Color.web("#1B5E20"));
            badge.setStyle("-fx-background-color: #69F0AE; -fx-background-radius: 10; -fx-padding: 2 6 2 6;");
            badge.setVisible(completed);
            badge.setManaged(completed);

            HBox nameRow = new HBox(6, name, badge);
            nameRow.setAlignment(Pos.CENTER);

            VBox card = new VBox(6, icon, nameRow);
            card.setAlignment(Pos.CENTER);
            card.setPrefWidth(cellW);
            card.setPrefHeight(cellH);
            card.setLayoutX(cellX);
            card.setLayoutY(cellY);
            card.setMouseTransparent(true);

            selectionPane.getChildren().add(card);
            BirdIconSpot spot = new BirdIconSpot(type, false, centerX, centerY);
            spots.add(spot);
            spotByType.put(type, spot);
        }

        Region selectorDock = new Region();
        selectorDock.setPrefSize(dockW, dockH);
        selectorDock.setLayoutX(dockX);
        selectorDock.setLayoutY(dockY);
        selectorDock.setStyle("-fx-background-color: rgba(10,10,10,0.6); -fx-border-color: #FFD54F; -fx-border-width: 2; -fx-background-radius: 18; -fx-border-radius: 18;");
        selectionPane.getChildren().add(selectorDock);

        Label dockLabel = new Label("PLAYER SELECTOR");
        dockLabel.setFont(Font.font("Consolas", 18));
        dockLabel.setTextFill(Color.web("#FFE082"));
        dockLabel.setLayoutX(dockX + 10);
        dockLabel.setLayoutY(dockY + 8);
        selectionPane.getChildren().add(dockLabel);

        Point2D dockPosition = new Point2D(dockX + dockW / 2.0, dockY + dockH / 2.0);

        Circle selector = new Circle(26);
        selector.setFill(Color.web("rgba(255,213,79,0.15)"));
        selector.setStroke(Color.web("#FFD54F"));
        selector.setStrokeWidth(3);
        selector.setManaged(false);
        selectionPane.getChildren().add(selector);

        Text selectorLabel = new Text("P1");
        selectorLabel.setFont(Font.font("Impact", 16));
        selectorLabel.setFill(Color.web("#FFD54F"));
        selectorLabel.setManaged(false);
        selectorLabel.setMouseTransparent(true);
        selectionPane.getChildren().add(selectorLabel);

        Label selectedLabel = new Label();
        selectedLabel.setFont(Font.font("Arial Black", 44));
        selectedLabel.setTextFill(Color.WHITE);

        Button skinBtn = uiFactory.action("SKIN: BASE", 360, 70, 28, "#37474F", 20, () -> {});

        Label rewardLabel = new Label();
        rewardLabel.setFont(Font.font("Consolas", 30));
        rewardLabel.setTextFill(Color.GOLD);

        Label unlockLabel = new Label();
        unlockLabel.setFont(Font.font("Consolas", 26));

        Label badgeLabel = new Label();
        badgeLabel.setFont(Font.font("Consolas", 24));

        Label continuesLabel = new Label();
        continuesLabel.setFont(Font.font("Consolas", 24));
        continuesLabel.setTextFill(Color.web("#C5E1A5"));

        Label info = new Label("Clear full runs to unlock each bird's rewards.\nUse the Skin button to cycle unlocked skins.");
        info.setFont(Font.font("Consolas", 22));
        info.setTextFill(Color.web("#FFE082"));
        info.setWrapText(true);
        info.setMaxWidth(700);
        applyNoEllipsis(info);

        Button start = uiFactory.action("GENERATE RUN", 480, 110, 38, "#00C853", 24, () -> {
            if (selected[0] == null) return;
            classicSelectedBird = selected[0];
            classicSelectedSkinKey = normalizeAdventureSkinChoice(selected[0], selectedSkin[0]);
            showClassicRunBriefing(stage, selected[0]);
        });
        Button back = uiFactory.action("BACK TO HUB", 420, 110, 38, "#D32F2F", 24, () -> showMenu(stage));

        Runnable refreshSkin = () -> {
            BirdType pick = selected[0];
            if (pick == null) {
                skinBtn.setText("SKIN: BASE");
                skinBtn.setDisable(true);
                skinBtn.setOpacity(0.7);
                return;
            }
            List<String> options = adventureSkinOptions(pick);
            String normalized = normalizeAdventureSkinChoice(pick, selectedSkin[0]);
            if (!options.contains(normalized)) {
                normalized = options.get(0);
            }
            selectedSkin[0] = normalized;
            skinBtn.setText(adventureSkinLabel(pick, selectedSkin[0]));
            boolean hasAlt = options.size() > 1;
            skinBtn.setDisable(!hasAlt);
            skinBtn.setOpacity(hasAlt ? 1.0 : 0.7);
        };

        skinBtn.setOnAction(e -> {
            playButtonClick();
            if (selected[0] == null) return;
            List<String> options = adventureSkinOptions(selected[0]);
            if (options.size() <= 1) return;
            int idx = options.indexOf(selectedSkin[0]);
            if (idx < 0) idx = 0;
            selectedSkin[0] = options.get((idx + 1) % options.size());
            skinBtn.setText(adventureSkinLabel(selected[0], selectedSkin[0]));
        });

        Runnable refreshSelection = () -> {
            BirdType picked = selected[0];
            boolean hasPick = picked != null;
            selectedLabel.setText(hasPick ? "Selected: " + picked.name : "Selected: SELECT");
            if (hasPick) {
                String reward = classicRewardFor(picked);
                String charReward = classicCharacterReward(picked);
                boolean unlocked = isClassicRewardUnlocked(picked);
                boolean completed = isClassicCompleted(picked);
                rewardLabel.setText("Reward Skin: " + reward + (charReward.isBlank() ? "" : "\nReward Character: " + charReward));
                unlockLabel.setText(unlocked ? "Reward Status: UNLOCKED" : "Reward Status: LOCKED");
                unlockLabel.setTextFill(unlocked ? Color.LIMEGREEN : Color.ORANGE);
                badgeLabel.setText(completed ? "Classic Badge: EARNED" : "Classic Badge: NOT EARNED");
                badgeLabel.setTextFill(completed ? Color.web("#69F0AE") : Color.web("#FFAB91"));
                selectedSkin[0] = normalizeAdventureSkinChoice(picked, selectedSkin[0]);
            } else {
                rewardLabel.setText("Reward Skin: -");
                unlockLabel.setText("Reward Status: LOCKED");
                unlockLabel.setTextFill(Color.ORANGE);
                badgeLabel.setText("Classic Badge: NOT EARNED");
                badgeLabel.setTextFill(Color.web("#FFAB91"));
                selectedSkin[0] = null;
            }
            refreshSkin.run();
            continuesLabel.setText("Classic Continues: " + classicContinues);
            start.setDisable(!hasPick);
            start.setOpacity(hasPick ? 1.0 : 0.6);
        };

        VBox rightCard = new VBox(14, selectedLabel, skinBtn, rewardLabel, unlockLabel, badgeLabel, continuesLabel, info);
        rightCard.setAlignment(Pos.TOP_LEFT);
        rightCard.setPadding(new Insets(26));
        rightCard.setMaxWidth(760);
        rightCard.setStyle("-fx-background-color: rgba(0,0,0,0.56); -fx-border-color: #64B5F6; -fx-border-width: 3; -fx-border-radius: 22; -fx-background-radius: 22;");

        HBox center = new HBox(22, selectionPane, rightCard);
        center.setAlignment(Pos.CENTER);

        HBox bottom = new HBox(20, start, back);
        bottom.setAlignment(Pos.CENTER);

        root.setTop(top);
        root.setCenter(center);
        root.setBottom(bottom);

        Scene scene = new Scene(root, WIDTH, HEIGHT);
        setupKeyboardNavigation(scene);
        applyConsoleHighlight(scene);
        setScenePreservingFullscreen(stage, scene);

        final double[] dragOffset = new double[2];
        selector.setOnMousePressed(e -> {
            if (selectorLocked[0]) {
                selectorLocked[0] = false;
                selected[0] = null;
                refreshSelection.run();
                selectorJustUnlocked[0] = true;
            }
            Point2D local = selectionPane.sceneToLocal(e.getSceneX(), e.getSceneY());
            dragOffset[0] = selector.getCenterX() - local.getX();
            dragOffset[1] = selector.getCenterY() - local.getY();
        });
        selector.setOnMouseDragged(e -> {
            if (selectorLocked[0]) return;
            Point2D local = selectionPane.sceneToLocal(e.getSceneX(), e.getSceneY());
            double nx = local.getX() + dragOffset[0];
            double ny = local.getY() + dragOffset[1];
            if (selectorJustUnlocked[0]) {
                selectorJustUnlocked[0] = false;
            }
            nx = Math.max(40, Math.min(nx, selectionPane.getPrefWidth() - 40));
            ny = Math.max(40, Math.min(ny, selectionPane.getPrefHeight() - 40));
            selector.setCenterX(nx);
            selector.setCenterY(ny);
            selectorLabel.setX(nx - 10);
            selectorLabel.setY(ny + 6);
        });
        selector.setOnMouseReleased(e -> {
            if (selectorLocked[0]) return;
            if (selectorJustUnlocked[0]) {
                selectorJustUnlocked[0] = false;
                selector.setCenterX(dockPosition.getX());
                selector.setCenterY(dockPosition.getY());
                selectorLabel.setX(dockPosition.getX() - 10);
                selectorLabel.setY(dockPosition.getY() + 6);
                return;
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
                selectorLabel.setX(best.cx - 10);
                selectorLabel.setY(best.cy + 6);
                selected[0] = best.type;
                selectorLocked[0] = true;
                classicSelectedBird = selected[0];
                refreshSelection.run();
            } else {
                selector.setCenterX(dockPosition.getX());
                selector.setCenterY(dockPosition.getY());
                selectorLabel.setX(dockPosition.getX() - 10);
                selectorLabel.setY(dockPosition.getY() + 6);
            }
        });

        if (selected[0] != null) {
            BirdIconSpot spot = spotByType.get(selected[0]);
            if (spot != null) {
                selector.setCenterX(spot.cx);
                selector.setCenterY(spot.cy);
                selectorLabel.setX(spot.cx - 10);
                selectorLabel.setY(spot.cy + 6);
            }
        } else {
            selector.setCenterX(dockPosition.getX());
            selector.setCenterY(dockPosition.getY());
            selectorLabel.setX(dockPosition.getX() - 10);
            selectorLabel.setY(dockPosition.getY() + 6);
        }

        refreshSelection.run();
        back.requestFocus();
    }

    private void showClassicRunBriefing(Stage stage, BirdType birdType) {
        if (birdType == null) birdType = BirdType.PIGEON;
        if (!isBirdUnlocked(birdType)) {
            showClassicBirdSelect(stage);
            return;
        }
        classicSelectedBird = birdType;
        classicSelectedSkinKey = normalizeAdventureSkinChoice(classicSelectedBird, classicSelectedSkinKey);
        classicRunCodename = buildClassicRunCodename();
        classicRun.clear();
        classicRun.addAll(buildClassicRun(classicSelectedBird));
        classicRoundIndex = 0;
        classicDeaths = 0;
        classicEncounter = classicRun.isEmpty() ? null : classicRun.get(0);
        playMenuMusic();

        BorderPane root = new BorderPane();
        root.setPadding(new Insets(30, 36, 30, 36));
        root.setStyle("-fx-background-color: linear-gradient(to bottom, #081122, #1D3557);");

        Label title = new Label("CLASSIC BRIEFING");
        title.setFont(Font.font("Arial Black", FontWeight.BOLD, 84));
        title.setTextFill(Color.GOLD);

        Label runInfo = new Label("Run: " + classicRunCodename + "  |  Pilot: " + classicSelectedBird.name
                + "  |  Continues: " + classicContinues);
        runInfo.setFont(Font.font("Consolas", 28));
        runInfo.setTextFill(Color.WHITE);
        runInfo.setWrapText(true);
        runInfo.setMaxWidth(1400);
        applyNoEllipsis(runInfo);

        String reward = classicRewardFor(classicSelectedBird);
        String charReward = classicCharacterReward(classicSelectedBird);
        boolean unlocked = isClassicRewardUnlocked(classicSelectedBird);
        String rewardText = "Reward: " + reward + (unlocked ? " (Unlocked)" : " (Locked)")
                + (charReward.isBlank() ? "" : "\nBonus: " + charReward);
        Label rewardInfo = new Label(rewardText);
        rewardInfo.setFont(Font.font("Consolas", 26));
        rewardInfo.setTextFill(unlocked ? Color.LIMEGREEN : Color.ORANGE);
        rewardInfo.setWrapText(true);
        rewardInfo.setMaxWidth(1400);
        applyNoEllipsis(rewardInfo);

        VBox route = new VBox(10);
        route.setAlignment(Pos.TOP_LEFT);
        route.setPadding(new Insets(18));
        route.setStyle("-fx-background-color: rgba(0,0,0,0.58); -fx-border-color: #4FC3F7; -fx-border-width: 3; -fx-border-radius: 18; -fx-background-radius: 18;");
        for (int i = 0; i < classicRun.size(); i++) {
            ClassicEncounter enc = classicRun.get(i);
            int enemyCount = enc.enemies == null ? 0 : enc.enemies.length;
            String tag = enc.bossFight ? "BOSS"
                    : (enemyCount >= 3 ? "SWARM" : (enemyCount == 2 ? "DUO" : "DUEL"));
            String line = String.format(
                    "R%d - %s | %s | %s | %s | %s",
                    i + 1, enc.name, mapDisplayName(enc.map), enc.mutator.label, enc.twist.label, tag
            );
            Label l = new Label(line);
            l.setFont(Font.font("Consolas", 22));
            l.setTextFill(Color.web("#E3F2FD"));
            l.setWrapText(true);
            l.setMaxWidth(1400);
            applyNoEllipsis(l);
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
        int classicCpuLevel = getClassicCpuLevel();
        int livesLeft = Math.max(0, 3 - classicDeaths);
        playMenuMusic();

        VBox root = MenuLayout.buildMenuRoot("-fx-background-color: linear-gradient(to bottom, #081122, #13294B);",
                MENU_PADDING, 30);

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

        Label briefingLabel = new Label(classicEncounter.briefing);
        briefingLabel.setWrapText(true);
        briefingLabel.setFont(Font.font("Consolas", 32));
        briefingLabel.setTextFill(Color.WHITE);
        briefingLabel.setMaxWidth(1200);
        applyNoEllipsis(briefingLabel);

        Label detailLabel = new Label("Map: " + mapDisplayName(classicEncounter.map)
                + "  |  Mutator: " + classicEncounter.mutator.label
                + "  |  Twist: " + classicEncounter.twist.label);
        detailLabel.setWrapText(true);
        detailLabel.setFont(Font.font("Consolas", 26));
        detailLabel.setTextFill(Color.web("#E3F2FD"));
        detailLabel.setMaxWidth(1200);
        applyNoEllipsis(detailLabel);

        Label twistLabel = new Label(classicEncounter.twist.description);
        twistLabel.setWrapText(true);
        twistLabel.setFont(Font.font("Consolas", 22));
        twistLabel.setTextFill(Color.web("#B3E5FC"));
        twistLabel.setMaxWidth(1200);
        applyNoEllipsis(twistLabel);

        int allyCount = classicEncounter.allies == null ? 0 : classicEncounter.allies.length;
        int enemyCount = classicEncounter.enemies == null ? 0 : classicEncounter.enemies.length;
        Label statusLabel = new Label("Round " + (classicRoundIndex + 1) + "/" + classicRun.size()
                + "  |  Lives " + livesLeft + "/3  |  Continues " + classicContinues);
        statusLabel.setFont(Font.font("Consolas", 24));
        statusLabel.setTextFill(Color.web("#FFE082"));
        statusLabel.setWrapText(true);
        statusLabel.setMaxWidth(1200);
        applyNoEllipsis(statusLabel);

        Label rosterLabel = new Label("Allies " + allyCount + "  |  Enemies " + enemyCount
                + (classicEncounter.bossFight ? "  |  BOSS" : ""));
        rosterLabel.setFont(Font.font("Consolas", 22));
        rosterLabel.setTextFill(Color.web("#FFD54F"));
        rosterLabel.setWrapText(true);
        rosterLabel.setMaxWidth(1200);
        applyNoEllipsis(rosterLabel);

        card.getChildren().addAll(speakerLabel, briefingLabel, detailLabel, twistLabel, statusLabel, rosterLabel);

        Label cpuLabel = new Label("CPU LEVEL: " + classicCpuLevel + " (AUTO)");
        cpuLabel.setFont(Font.font("Consolas", 26));
        cpuLabel.setTextFill(Color.web("#B3E5FC"));
        HBox options = new HBox(20, cpuLabel);
        options.setAlignment(Pos.CENTER);

        HBox buttons = new HBox(30);
        buttons.setAlignment(Pos.CENTER);

        Button continueButton = uiFactory.action("START ENCOUNTER", 520, 120, 52, "#00C853", 32, () -> startClassicEncounter(stage));

        Button menuButton = uiFactory.action("BACK TO HUB", 460, 120, 36, "#D32F2F", 32, () -> showMenu(stage));
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
                        ? "Locked - character not unlocked"
                        : (completed ? "Completed" : ("Unlocked Chapters: " + unlocked + "/" + chapters.length))
        );
        epStatus.setFont(Font.font("Consolas", 30));
        epStatus.setTextFill(!episodePlayable ? Color.GRAY : (completed ? Color.LIMEGREEN : Color.ORANGE));

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
        card.getChildren().addAll(epTitle, epStatus, actions, flavor);
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
        Arrays.fill(classicCpuLevels, 0);
        classicTeams[0] = 1;

        int slot = 0;
        createStoryBird(900, classicSelectedBird, slot, "You: " + classicSelectedBird.name, 112, 1.05, 1.04, false);
        String skinKey = normalizeAdventureSkinChoice(classicSelectedBird, classicSelectedSkinKey);
        classicSelectedSkinKey = skinKey;
        applySkinChoiceToBird(players[slot], classicSelectedBird, skinKey);
        slot++;

        if (encounter.allies != null) {
            for (int i = 0; i < encounter.allies.length && slot < 4; i++) {
                ClassicFighter ally = encounter.allies[i];
                Bird allyBird = createStoryBird(
                        1300 + i * 350,
                        ally.type,
                        slot,
                        ally.title,
                        ally.health,
                        ally.powerMult,
                        ally.speedMult,
                        true
                );
                if (ally.skinKey != null) {
                    applyPreviewSkinChoiceToBird(allyBird, ally.type, ally.skinKey);
                }
                classicTeams[slot] = 1;
                classicCpuLevels[slot] = Math.max(0, ally.cpuLevel);
                slot++;
            }
        }

        if (encounter.enemies != null) {
            for (int i = 0; i < encounter.enemies.length && slot < 4; i++) {
                ClassicFighter enemy = encounter.enemies[i];
                Bird enemyBird = createStoryBird(
                        3800 + i * 520,
                        enemy.type,
                        slot,
                        enemy.title,
                        enemy.health,
                        enemy.powerMult,
                        enemy.speedMult,
                        true
                );
                if (enemy.skinKey != null) {
                    applyPreviewSkinChoiceToBird(enemyBird, enemy.type, enemy.skinKey);
                }
                classicTeams[slot] = 2;
                classicCpuLevels[slot] = Math.max(0, enemy.cpuLevel);
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
            case VINE_SURGE -> {
                nectarNodes.add(new NectarNode(1200, GROUND_Y - 900, true));
                nectarNodes.add(new NectarNode(4700, GROUND_Y - 900, true));
                powerUps.add(new PowerUp(2200, GROUND_Y - 1200, PowerUpType.VINE_GRAPPLE));
                powerUps.add(new PowerUp(3800, GROUND_Y - 1200, PowerUpType.VINE_GRAPPLE));
            }
            case MEDIC_CACHE -> {
                powerUps.add(new PowerUp(1600, GROUND_Y - 820, PowerUpType.HEALTH));
                powerUps.add(new PowerUp(3000, GROUND_Y - 960, PowerUpType.SPEED));
                powerUps.add(new PowerUp(4400, GROUND_Y - 820, PowerUpType.HEALTH));
            }
            case SHOCK_DROPS -> {
                powerUps.add(new PowerUp(2000, GROUND_Y - 820, PowerUpType.TITAN));
                powerUps.add(new PowerUp(3000, GROUND_Y - 980, PowerUpType.OVERCHARGE));
                powerUps.add(new PowerUp(4000, GROUND_Y - 820, PowerUpType.TITAN));
            }
            case OVERCHARGE_FURY -> {
                powerUps.add(new PowerUp(1600, GROUND_Y - 820, PowerUpType.RAGE));
                powerUps.add(new PowerUp(3000, GROUND_Y - 980, PowerUpType.OVERCHARGE));
                powerUps.add(new PowerUp(4400, GROUND_Y - 820, PowerUpType.SPEED));
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
            classicDeaths++;
            if (classicDeaths < 3) {
                showStoryDialogue(
                        stage,
                        "Run Failed",
                        "Skycaster",
                        "You were eliminated in " + classicEncounter.name + ".\nClassic lives: "
                                + classicDeaths + "/3.\nContinue to retry this encounter.",
                        () -> startClassicEncounter(stage)
                );
                return;
            }
            if (classicContinues > 0) {
                showClassicContinuePrompt(stage);
            } else {
                classicDeaths = 0;
                showStoryDialogue(
                        stage,
                        "Run Failed",
                        "Skycaster",
                        "You were eliminated in " + classicEncounter.name + ".\nClassic lives: 3/3.\nNo Classic Continues remaining.",
                        () -> showClassicBirdSelect(stage)
                );
            }
            return;
        }

        if (classicRoundIndex >= classicRun.size() - 1) {
            classicDeaths = 0;
            classicCompleted[classicSelectedBird.ordinal()] = true;
            if (!achievementsUnlocked[19]) {
                unlockAchievement(19, "CLASSIC CREST!");
            }
            unlockClassicReward(classicSelectedBird);
            boolean unlockedTitmouse = classicSelectedBird == BirdType.HUMMINGBIRD && !titmouseUnlocked;
            if (unlockedTitmouse) {
                titmouseUnlocked = true;
                queueUnlockCardForBird(BirdType.TITMOUSE);
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
                    () -> runAfterUnlockCards(stage, () -> showClassicBirdSelect(stage))
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

    private void showClassicContinuePrompt(Stage stage) {
        playMenuMusic();

        VBox root = MenuLayout.buildMenuRoot("-fx-background-color: linear-gradient(to bottom, #081122, #13294B);",
                MENU_PADDING, 30);

        Label title = new Label("CLASSIC CONTINUE");
        title.setFont(Font.font("Arial Black", FontWeight.BOLD, 84));
        title.setTextFill(Color.web("#FFE082"));

        VBox card = new VBox(20);
        card.setAlignment(Pos.CENTER_LEFT);
        card.setPadding(new Insets(35));
        card.setMaxWidth(1200);
        card.setStyle("-fx-background-color: rgba(0,0,0,0.6); -fx-background-radius: 24; -fx-border-color: #4FC3F7; -fx-border-width: 4; -fx-border-radius: 24;");

        Label text = new Label("Classic lives depleted (3/3).\nUse a Classic Continue to retry this encounter?\nContinues remaining: " + classicContinues);
        text.setWrapText(true);
        text.setFont(Font.font("Consolas", 30));
        text.setTextFill(Color.WHITE);

        card.getChildren().add(text);

        Button useContinue = uiFactory.action("USE CONTINUE", 460, 120, 40, "#00C853", 30, () -> {
            if (classicContinues <= 0) {
                showClassicBirdSelect(stage);
                return;
            }
            classicContinues = Math.max(0, classicContinues - 1);
            classicDeaths = 0;
            saveAchievements();
            startClassicEncounter(stage);
        });

        Button endRun = uiFactory.action("END RUN", 420, 120, 40, "#FF1744", 30, () -> {
            classicDeaths = 0;
            showClassicBirdSelect(stage);
        });

        HBox buttons = new HBox(30, useContinue, endRun);
        buttons.setAlignment(Pos.CENTER);

        root.getChildren().addAll(title, card, buttons);

        Scene scene = new Scene(root, WIDTH, HEIGHT);
        setupKeyboardNavigation(scene);
        applyConsoleHighlight(scene);
        setScenePreservingFullscreen(stage, scene);
        useContinue.requestFocus();
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
                saveAchievements();
                showStoryDialogue(stage,
                        "Episode Complete",
                        "Narrator",
                        "Episode clear. Legacy episodes do not grant rewards.",
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
                    () -> runAfterUnlockCards(stage, () -> showEpisodeChapterSelect(stage)));
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

        VBox root = MenuLayout.buildMenuRoot("-fx-background-color: linear-gradient(to bottom, #081122, #13294B);",
                MENU_PADDING, 30);

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

        Button menuButton = uiFactory.action("BACK TO HUB", 460, 120, 36, "#D32F2F", 32, () -> showMenu(stage));
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
            stageSelectHandler = null;
            stageSelectRandomHandler = null;
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

        List<MapCard> cards = new ArrayList<>(List.of(
                new MapCard("BIG FOREST", "Dense trees, layered platforms, high vertical play.", "#2E7D32", MapType.FOREST),
                new MapCard("PIGEON'S ROOFTOPS", "Neon rooftops with city wind vents.", "#5E35B1", MapType.CITY),
                new MapCard("SKY CLIFFS", "Stepping cliffs and strong updrafts.", "#8D6E63", MapType.SKYCLIFFS),
                new MapCard("VIBRANT JUNGLE", "Vines, nectar nodes, wild vertical mix.", "#388E3C", MapType.VIBRANT_JUNGLE),
                new MapCard("ECHO CAVERN", "Tight cave corridors and hang ledges.", "#455A64", MapType.CAVE),
                new MapCard("BATTLEFIELD", "Small island, three platforms, deadly void.", "#1E88E5", MapType.BATTLEFIELD)
        ));
        cards.removeIf(card -> !isMapUnlocked(card.map));

        GridPane grid = new GridPane();
        grid.setHgap(26);
        grid.setVgap(22);
        grid.setAlignment(Pos.TOP_CENTER);

        java.util.function.Consumer<MapType> handler = stageSelectHandler;
        java.util.function.Consumer<MapType> selectMap = map -> {
            stageSelectRandomHandler = null;
            if (handler != null) {
                stageSelectHandler = null;
                handler.accept(map);
            } else {
                beginFreshMatchOnMap(stage, map);
            }
        };

        for (int i = 0; i < cards.size(); i++) {
            MapCard card = cards.get(i);
            int col = i % 2;
            int row = i / 2;

            VBox cardBox = new VBox(8);
            cardBox.setPadding(new Insets(16));
            cardBox.setStyle("-fx-background-color: rgba(0,0,0,0.45); -fx-border-color: #90A4AE; -fx-border-width: 2; -fx-background-radius: 18; -fx-border-radius: 18;");

            String label = card.name;
            Button btn = uiFactory.action(label, 680, 120, 34, card.color, 22, () -> selectMap.accept(card.map));
            String descText = card.desc;
            Label desc = new Label(descText);
            desc.setFont(Font.font("Consolas", 20));
            desc.setTextFill(Color.web("#CFD8DC"));
            desc.setWrapText(true);
            desc.setMaxWidth(640);

            cardBox.getChildren().addAll(btn, desc);
            grid.add(cardBox, col, row);
        }

        Button randomBtn = uiFactory.action("RANDOM", 1400, 110, 38, "#8E24AA", 24, () -> {
            Runnable randomHandler = stageSelectRandomHandler;
            if (randomHandler != null) {
                stageSelectHandler = null;
                stageSelectRandomHandler = null;
                randomHandler.run();
                return;
            }
            List<MapType> maps = new ArrayList<>();
            for (MapType map : MapType.values()) {
                if (isMapUnlocked(map)) maps.add(map);
            }
            if (maps.isEmpty()) {
                selectMap.accept(MapType.FOREST);
                return;
            }
            selectMap.accept(maps.get(random.nextInt(maps.size())));
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
        bindEscape(scene, backArrow);
        scene.getRoot().setStyle(scene.getRoot().getStyle() + ";-fx-focus-color:transparent;-fx-faint-focus-color:transparent;");
        setupKeyboardNavigation(scene);
        applyConsoleHighlight(scene);
        setScenePreservingFullscreen(stage, scene);
        backArrow.requestFocus();
    }

    private void beginFreshMatchOnMap(Stage stage, MapType map) {
        stageSelectReturn = null;
        stageSelectHandler = null;
        selectedMap = map;
        trainingModeActive = false;
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
        startMatch(stage);
    }

    private void beginTrainingMatchOnMap(Stage stage, MapType map) {
        stageSelectReturn = null;
        stageSelectHandler = null;
        resetMatchStats();
        selectedMap = map;
        trainingModeActive = true;
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
        competitionSeriesActive = false;
        Arrays.fill(competitionRoundWins, 0);
        Arrays.fill(competitionTeamWins, 0);
        competitionRoundNumber = 1;
        teamModeEnabled = false;
        competitionModeEnabled = false;
        mutatorModeEnabled = false;
        startMatch(stage);
    }

    private void setupTrainingRoster() {
        activePlayers = 2;
        BirdType playerType = isBirdUnlocked(trainingPlayerBird) ? trainingPlayerBird : firstUnlockedBird();
        if (playerType == null) playerType = BirdType.PIGEON;
        BirdType opponentType = isBirdUnlocked(trainingOpponentBird) ? trainingOpponentBird : playerType;
        if (opponentType == null) opponentType = BirdType.PIGEON;

        Bird player = new Bird(1200, playerType, 0, this);
        player.y = GROUND_Y - 400;
        player.name = "You: " + playerType.name;
        players[0] = player;
        isAI[0] = false;

        Bird dummy = new Bird(4200, opponentType, trainingDummyIndex, this);
        dummy.y = GROUND_Y - 400;
        dummy.name = "Dummy: " + opponentType.name;
        players[trainingDummyIndex] = dummy;
        isAI[trainingDummyIndex] = false;

        applySkinChoiceToBird(player, playerType, null);
        applySkinChoiceToBird(dummy, opponentType, null);
    }

    private void captureTrainingSpawns() {
        Bird player = players[0];
        Bird dummy = players[trainingDummyIndex];
        if (player != null) {
            trainingPlayerSpawnX = player.x;
            trainingPlayerSpawnY = player.y;
        }
        if (dummy != null) {
            trainingDummySpawnX = dummy.x;
            trainingDummySpawnY = dummy.y;
        }
    }

    private void addTrainingControls(StackPane root) {
        Button resetDummy = uiFactory.action("RESET DUMMY", 260, 70, 24, "#546E7A", 18, this::resetTrainingDummyPosition);
        StackPane.setAlignment(resetDummy, Pos.BOTTOM_RIGHT);
        StackPane.setMargin(resetDummy, new Insets(0, 28, 26, 0));
        root.getChildren().add(resetDummy);
    }

    private void resetTrainingDummyPosition() {
        if (!trainingModeActive) return;
        Bird dummy = players[trainingDummyIndex];
        if (dummy == null) return;
        dummy.x = trainingDummySpawnX;
        dummy.y = trainingDummySpawnY;
        dummy.vx = 0;
        dummy.vy = 0;
        dummy.canDoubleJump = true;
        dummy.onVine = false;
        dummy.attachedVine = null;
        dummy.health = dummy.getMaxHealth();
        addToKillFeed("DUMMY RESET.");
    }

    public boolean isTrainingDummy(Bird target) {
        return trainingModeActive && target != null && target.playerIndex == trainingDummyIndex;
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
        boolean[] defaultHandled = new boolean[1];

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
            if (bt == BirdType.FALCON || bt == BirdType.HEISENBIRD) continue; // Echo birds are selected from split buttons.
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
                        selected.setText("Falcon Locked (Card Packs)");
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

            if (bt == BirdType.OPIUMBIRD) {
                boolean heisenSideVisible = true;
                String splitNeutral = heisenSideVisible
                        ? "-fx-background-color: linear-gradient(to right, #8A2BE2 0%, #8A2BE2 50%, #29B6F6 50%, #29B6F6 100%); -fx-text-fill: white; -fx-font-weight: bold; -fx-text-overrun: clip;"
                        : "-fx-background-color: #8A2BE2; -fx-text-fill: white; -fx-font-weight: bold; -fx-text-overrun: clip;";
                String splitOpium = heisenSideVisible
                        ? "-fx-background-color: linear-gradient(to right, #E1BEE7 0%, #E1BEE7 50%, #29B6F6 50%, #29B6F6 100%); -fx-text-fill: black; -fx-font-weight: bold; -fx-text-overrun: clip;"
                        : "-fx-background-color: #E1BEE7; -fx-text-fill: black; -fx-font-weight: bold; -fx-text-overrun: clip;";
                String splitOpiumSkin = heisenSideVisible
                        ? "-fx-background-color: linear-gradient(to right, #212121 0%, #212121 50%, #29B6F6 50%, #29B6F6 100%); -fx-text-fill: #FFD54F; -fx-font-weight: bold; -fx-text-overrun: clip;"
                        : "-fx-background-color: #212121; -fx-text-fill: #FFD54F; -fx-font-weight: bold; -fx-text-overrun: clip;";
                String splitHeisen = "-fx-background-color: linear-gradient(to right, #8A2BE2 0%, #8A2BE2 50%, #B3E5FC 50%, #B3E5FC 100%); -fx-text-fill: black; -fx-font-weight: bold; -fx-text-overrun: clip;";
                String splitHeisenSkin = "-fx-background-color: linear-gradient(to right, #8A2BE2 0%, #8A2BE2 50%, #E1F5FE 50%, #E1F5FE 100%); -fx-text-fill: black; -fx-font-weight: bold; -fx-text-overrun: clip;";

                String opiumClassicKey = classicSkinDataKey(BirdType.OPIUMBIRD);
                boolean hasOpiumClassic = isClassicRewardUnlocked(BirdType.OPIUMBIRD);
                String heisenClassicKey = classicSkinDataKey(BirdType.HEISENBIRD);
                boolean hasHeisenClassic = isClassicRewardUnlocked(BirdType.HEISENBIRD);

                Label opiumSideLabel = new Label("OPIUM");
                opiumSideLabel.setFont(Font.font("Arial Black", 12));
                opiumSideLabel.setTextFill(Color.WHITE);
                opiumSideLabel.setPrefWidth(heisenSideVisible ? 78 : 156);
                opiumSideLabel.setAlignment(Pos.CENTER);
                opiumSideLabel.setMouseTransparent(true);

                Label heisenSideLabel = new Label(heisenSideVisible ? "HEISEN" : "");
                heisenSideLabel.setFont(Font.font("Arial Black", 12));
                heisenSideLabel.setTextFill(Color.WHITE);
                heisenSideLabel.setPrefWidth(heisenSideVisible ? 78 : 0);
                heisenSideLabel.setAlignment(Pos.CENTER);
                heisenSideLabel.setMouseTransparent(true);

                HBox splitLabel = new HBox(opiumSideLabel, heisenSideLabel);
                splitLabel.setAlignment(Pos.CENTER);
                splitLabel.setMouseTransparent(true);

                b.setText("");
                b.setGraphic(splitLabel);
                b.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
                b.setStyle(splitNeutral);

                Runnable selectOpium = () -> {
                    selected.setText("Selected: Opium Bird");
                    box.setUserData(BirdType.OPIUMBIRD);
                    stats.setText("Power: " + BirdType.OPIUMBIRD.power + " | Speed: " + String.format("%.1f", BirdType.OPIUMBIRD.speed) + " | Jump: " + BirdType.OPIUMBIRD.jumpHeight + "\n\nSPECIAL: " + BirdType.OPIUMBIRD.ability);
                    b.setStyle(splitOpium);
                    opiumSideLabel.setTextFill(Color.BLACK);
                    heisenSideLabel.setTextFill(Color.WHITE);
                };

                Runnable selectOpiumSkin = () -> {
                    selected.setText("Selected: " + classicRewardFor(BirdType.OPIUMBIRD) + " (Skin)");
                    box.setUserData(opiumClassicKey);
                    stats.setText("Power: " + BirdType.OPIUMBIRD.power + " | Speed: " + String.format("%.1f", BirdType.OPIUMBIRD.speed) + " | Jump: " + BirdType.OPIUMBIRD.jumpHeight + "\n\nSPECIAL: " + BirdType.OPIUMBIRD.ability);
                    b.setStyle(splitOpiumSkin);
                    opiumSideLabel.setTextFill(Color.web("#FFD54F"));
                    heisenSideLabel.setTextFill(Color.WHITE);
                };

                Runnable selectHeisen = () -> {
                    if (!heisenbirdUnlocked) {
                        selected.setText("Heisenbird Locked (card packs)");
                        box.setUserData(BirdType.OPIUMBIRD);
                        stats.setText("Power: " + BirdType.OPIUMBIRD.power + " | Speed: " + String.format("%.1f", BirdType.OPIUMBIRD.speed) + " | Jump: " + BirdType.OPIUMBIRD.jumpHeight + "\n\nSPECIAL: " + BirdType.OPIUMBIRD.ability);
                        b.setStyle(splitNeutral);
                        opiumSideLabel.setTextFill(Color.WHITE);
                        heisenSideLabel.setTextFill(Color.web("#FFCDD2"));
                        return;
                    }
                    selected.setText("Selected: Heisenbird");
                    box.setUserData(BirdType.HEISENBIRD);
                    stats.setText("Power: " + BirdType.HEISENBIRD.power + " | Speed: " + String.format("%.1f", BirdType.HEISENBIRD.speed) + " | Jump: " + BirdType.HEISENBIRD.jumpHeight + "\n\nSPECIAL: " + BirdType.HEISENBIRD.ability);
                    b.setStyle(splitHeisen);
                    opiumSideLabel.setTextFill(Color.WHITE);
                    heisenSideLabel.setTextFill(Color.BLACK);
                };

                Runnable selectHeisenSkin = () -> {
                    selected.setText("Selected: " + classicRewardFor(BirdType.HEISENBIRD) + " (Skin)");
                    box.setUserData(heisenClassicKey);
                    stats.setText("Power: " + BirdType.HEISENBIRD.power + " | Speed: " + String.format("%.1f", BirdType.HEISENBIRD.speed) + " | Jump: " + BirdType.HEISENBIRD.jumpHeight + "\n\nSPECIAL: " + BirdType.HEISENBIRD.ability);
                    b.setStyle(splitHeisenSkin);
                    opiumSideLabel.setTextFill(Color.WHITE);
                    heisenSideLabel.setTextFill(Color.BLACK);
                };

                Runnable cycleOpiumSide = () -> {
                    Object currentData = box.getUserData();
                    if (opiumClassicKey.equals(currentData)) {
                        selectOpium.run();
                    } else if (currentData == BirdType.OPIUMBIRD && hasOpiumClassic) {
                        selectOpiumSkin.run();
                    } else {
                        selectOpium.run();
                    }
                };

                Runnable cycleHeisenSide = () -> {
                    if (!heisenSideVisible) {
                        cycleOpiumSide.run();
                        return;
                    }
                    if (!heisenbirdUnlocked) {
                        selectHeisen.run();
                        return;
                    }
                    if (!isBirdUnlocked(BirdType.HEISENBIRD)) {
                        selectOpium.run();
                        return;
                    }
                    Object currentData = box.getUserData();
                    if (heisenClassicKey.equals(currentData)) {
                        selectHeisen.run();
                    } else if (currentData == BirdType.HEISENBIRD && hasHeisenClassic) {
                        selectHeisenSkin.run();
                    } else {
                        selectHeisen.run();
                    }
                };

                b.setOnMousePressed(e ->
                        b.getProperties().put("splitPickHeisen", heisenSideVisible && e.getX() > b.getWidth() * 0.5));
                b.setOnKeyPressed(e -> {
                    if (e.getCode() == KeyCode.LEFT) {
                        b.getProperties().put("splitKeyboardSide", "opium");
                        cycleOpiumSide.run();
                        e.consume();
                    } else if (e.getCode() == KeyCode.RIGHT && heisenSideVisible) {
                        b.getProperties().put("splitKeyboardSide", "heisen");
                        cycleHeisenSide.run();
                        e.consume();
                    }
                });
                b.setOnAction(e -> {
                    playButtonClick();
                    Object sideData = b.getProperties().remove("splitKeyboardSide");
                    boolean keyboardHeisen = heisenSideVisible && "heisen".equals(sideData);
                    boolean mouseHeisen = Boolean.TRUE.equals(b.getProperties().remove("splitPickHeisen")) && heisenSideVisible;
                    boolean pickHeisen = mouseHeisen || keyboardHeisen;
                    if (pickHeisen) cycleHeisenSide.run();
                    else cycleOpiumSide.run();
                });

                if (def == BirdType.OPIUMBIRD) {
                    selectOpium.run();
                    defaultHandled[0] = true;
                } else if (def == BirdType.HEISENBIRD) {
                    selectHeisen.run();
                    defaultHandled[0] = true;
                }

                grid.add(b, col++, row);
                if (col > 2) { col = 0; row++; }
                continue;
            }

            b.setOnAction(e -> {
                playButtonClick();
                Object currentData = box.getUserData();

                // === PIGEON SKIN CYCLER ===
                if (bt == BirdType.PIGEON && (cityPigeonUnlocked || noirPigeonUnlocked || beaconPigeonUnlocked)) {
                    List<Object> cycle = new ArrayList<>();
                    cycle.add(BirdType.PIGEON);
                    if (cityPigeonUnlocked) cycle.add("CITY_PIGEON");
                    if (noirPigeonUnlocked) cycle.add("NOIR_PIGEON");
                    if (beaconPigeonUnlocked) cycle.add(BEACON_PIGEON_SKIN);

                    int idx = cycle.indexOf(currentData);
                    if (idx < 0) idx = 0;
                    Object next = cycle.get((idx + 1) % cycle.size());

                    if (next == BirdType.PIGEON) {
                        selected.setText("Selected: Pigeon");
                        box.setUserData(BirdType.PIGEON);
                        b.setStyle(baseStyle);
                    } else if ("CITY_PIGEON".equals(next)) {
                        selected.setText("Selected: City Pigeon (Skin)");
                        box.setUserData("CITY_PIGEON");
                        b.setStyle("-fx-background-color: #FFD700; -fx-text-fill: black; -fx-font-weight: bold;");
                    } else if ("NOIR_PIGEON".equals(next)) {
                        selected.setText("Selected: Noir Pigeon (Skin)");
                        box.setUserData("NOIR_PIGEON");
                        b.setStyle("-fx-background-color: #111111; -fx-text-fill: #F44336; -fx-font-weight: bold;");
                    } else if (BEACON_PIGEON_SKIN.equals(next)) {
                        selected.setText("Selected: Beacon Pigeon (Skin)");
                        box.setUserData(BEACON_PIGEON_SKIN);
                        b.setStyle("-fx-background-color: #FFE082; -fx-text-fill: #1E88E5; -fx-font-weight: bold;");
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
                // === NOVA PHOENIX LEGENDARY ===
                else if (bt == BirdType.PHOENIX && (novaPhoenixUnlocked || isClassicRewardUnlocked(bt))) {
                    String classicKey = classicSkinDataKey(bt);
                    boolean hasClassicSkin = isClassicRewardUnlocked(bt);
                    boolean hasLegendary = novaPhoenixUnlocked;
                    String legendaryStyle = "-fx-background-color: linear-gradient(to right, #1A237E 0%, #4A148C 50%, #00E5FF 100%); -fx-text-fill: white; -fx-font-weight: bold;";
                    String classicStyle = "-fx-background-color: #212121; -fx-text-fill: #FFD54F; -fx-font-weight: bold;";

                    if (NOVA_PHOENIX_SKIN.equals(currentData)) {
                        selected.setText("Selected: Phoenix");
                        box.setUserData(bt);
                        b.setStyle(baseStyle);
                    } else if (hasClassicSkin && classicKey.equals(currentData)) {
                        if (hasLegendary) {
                            selected.setText("Selected: Nova Phoenix (Legendary)");
                            box.setUserData(NOVA_PHOENIX_SKIN);
                            b.setStyle(legendaryStyle);
                        } else {
                            selected.setText("Selected: Phoenix");
                            box.setUserData(bt);
                            b.setStyle(baseStyle);
                        }
                    } else if (currentData == bt) {
                        if (hasClassicSkin) {
                            selected.setText("Selected: " + classicRewardFor(bt) + " (Skin)");
                            box.setUserData(classicKey);
                            b.setStyle(classicStyle);
                        } else if (hasLegendary) {
                            selected.setText("Selected: Nova Phoenix (Legendary)");
                            box.setUserData(NOVA_PHOENIX_SKIN);
                            b.setStyle(legendaryStyle);
                        } else {
                            selected.setText("Selected: Phoenix");
                            box.setUserData(bt);
                            b.setStyle(baseStyle);
                        }
                    } else {
                        selected.setText("Selected: Phoenix");
                        box.setUserData(bt);
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
        if (!defaultHandled[0]) {
            if (def == BirdType.FALCON) {
                selected.setText("Selected: Falcon");
                box.setUserData(BirdType.FALCON);
                stats.setText("Power: " + BirdType.FALCON.power + " | Speed: " + String.format("%.1f", BirdType.FALCON.speed) + " | Jump: " + BirdType.FALCON.jumpHeight + "\n\nSPECIAL: " + BirdType.FALCON.ability);
            } else if (def == BirdType.HEISENBIRD) {
                selected.setText("Selected: Heisenbird");
                box.setUserData(BirdType.HEISENBIRD);
                stats.setText("Power: " + BirdType.HEISENBIRD.power + " | Speed: " + String.format("%.1f", BirdType.HEISENBIRD.speed) + " | Jump: " + BirdType.HEISENBIRD.jumpHeight + "\n\nSPECIAL: " + BirdType.HEISENBIRD.ability);
            }
            for (var node : grid.getChildren()) {
                boolean defaultMatches = false;
                if (node instanceof Button btn) {
                    Object nodeData = btn.getUserData();
                    defaultMatches = nodeData == def
                            || (def == BirdType.FALCON && nodeData == BirdType.EAGLE)
                            || (def == BirdType.HEISENBIRD && nodeData == BirdType.OPIUMBIRD);
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
        }

        box.getChildren().addAll(selected, grid, statsPane);
        return box;
    }

    private String toHex(Color c) {
        return String.format("#%02X%02X%02X", (int) (c.getRed() * 255), (int) (c.getGreen() * 255), (int) (c.getBlue() * 255));
    }

    private void applyMutatorStartEffects(MatchMutator mutator, boolean announce, boolean classicScale) {
        if (mutator == null || mutator == MatchMutator.NONE) return;
        double turboPower = classicScale ? 1.10 : 1.12;
        double turboSpeed = classicScale ? 1.12 : 1.14;

        switch (mutator) {
            case TURBO_BRAWL -> {
                for (int i = 0; i < activePlayers; i++) {
                    Bird b = players[i];
                    if (b == null) continue;
                    b.setBaseMultipliers(
                            b.baseSizeMultiplier,
                            b.basePowerMultiplier * turboPower,
                            b.baseSpeedMultiplier * turboSpeed
                    );
                }
                if (announce) {
                    addToKillFeed("Turbo Brawl: +speed and +power for everyone.");
                }
            }
            case RAGE_FRENZY -> {
                for (int i = 0; i < activePlayers; i++) {
                    Bird b = players[i];
                    if (b == null) continue;
                    b.rageTimer = Math.max(b.rageTimer, MUTATOR_BUFF_FRAMES);
                    b.powerMultiplier = Math.max(b.powerMultiplier, b.basePowerMultiplier * 1.6);
                }
                if (announce) {
                    addToKillFeed("Rage Frenzy: everyone is enraged.");
                }
            }
            case TITAN_RUMBLE -> {
                for (int i = 0; i < activePlayers; i++) {
                    Bird b = players[i];
                    if (b == null) continue;
                    b.titanActive = true;
                    b.titanTimer = Math.max(b.titanTimer, MUTATOR_BUFF_FRAMES);
                    if (b.shrinkTimer <= 0) {
                        b.sizeMultiplier = b.baseSizeMultiplier * 1.35;
                    }
                    b.powerMultiplier = Math.max(b.powerMultiplier, b.basePowerMultiplier * 1.4);
                }
                if (announce) {
                    addToKillFeed("Titan Rumble: everyone is in Titan form.");
                }
            }
            case OVERCHARGE_BRAWL -> {
                for (int i = 0; i < activePlayers; i++) {
                    Bird b = players[i];
                    if (b == null) continue;
                    b.specialCooldown = 0;
                    b.overchargeAttackTimer = Math.max(b.overchargeAttackTimer, MUTATOR_BUFF_FRAMES);
                    b.rageTimer = Math.max(b.rageTimer, MUTATOR_BUFF_FRAMES);
                    b.powerMultiplier = Math.max(b.powerMultiplier, b.basePowerMultiplier * 1.35);
                }
                if (announce) {
                    addToKillFeed("Overcharge Brawl: rapid attacks for everyone.");
                }
            }
            default -> {
            }
        }
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
            applyMutatorStartEffects(activeMutator, false, true);
            return;
        }

        if (storyModeActive || adventureModeActive || trainingModeActive) return;

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
            return;
        }

        if (!mutatorModeEnabled) return;

        MatchMutator[] pool = {
                MatchMutator.RAGE_FRENZY,
                MatchMutator.TITAN_RUMBLE,
                MatchMutator.OVERCHARGE_BRAWL,
                MatchMutator.CROW_SURGE,
                MatchMutator.TURBO_BRAWL
        };
        activeMutator = pool[random.nextInt(pool.length)];
        addToKillFeed("MUTATOR ACTIVE: " + activeMutator.label);
        applyMutatorStartEffects(activeMutator, true, false);
    }

    private Bird findTimeoutWinner() {
        boolean teamComp = teamModeEnabled && !storyModeActive && !adventureModeActive && !classicModeActive;
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
        boolean teamComp = teamModeEnabled && !storyModeActive && !adventureModeActive && !classicModeActive;
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
        if (!(competitionModeEnabled && !storyModeActive && !adventureModeActive && !classicModeActive)) return false;

        boolean teamComp = teamModeEnabled && !storyModeActive && !adventureModeActive && !classicModeActive;
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
        if (storyModeActive || adventureModeActive || trainingModeActive) return;

        switch (activeMutator) {
            case RAGE_FRENZY -> {
                for (int i = 0; i < activePlayers; i++) {
                    Bird b = players[i];
                    if (b == null) continue;
                    b.rageTimer = Math.max(b.rageTimer, MUTATOR_BUFF_FRAMES);
                    b.powerMultiplier = Math.max(b.powerMultiplier, b.basePowerMultiplier * 1.6);
                }
            }
            case TITAN_RUMBLE -> {
                for (int i = 0; i < activePlayers; i++) {
                    Bird b = players[i];
                    if (b == null) continue;
                    b.titanActive = true;
                    b.titanTimer = Math.max(b.titanTimer, MUTATOR_BUFF_FRAMES);
                    if (b.shrinkTimer <= 0) {
                        b.sizeMultiplier = b.baseSizeMultiplier * 1.35;
                    }
                    b.powerMultiplier = Math.max(b.powerMultiplier, b.basePowerMultiplier * 1.4);
                }
            }
            case OVERCHARGE_BRAWL -> {
                for (int i = 0; i < activePlayers; i++) {
                    Bird b = players[i];
                    if (b == null) continue;
                    b.overchargeAttackTimer = Math.max(b.overchargeAttackTimer, MUTATOR_BUFF_FRAMES);
                    b.rageTimer = Math.max(b.rageTimer, MUTATOR_BUFF_FRAMES);
                    b.powerMultiplier = Math.max(b.powerMultiplier, b.basePowerMultiplier * 1.35);
                }
            }
            case CROW_SURGE -> {
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
            default -> {
            }
        }
    }

    private void triggerMatchEnd(Bird winner) {
        if (matchEnded) return;

        if (lanModeActive) {
            matchEnded = true;
            lanMatchActive = false;
            int winnerIndex = winner != null ? winner.playerIndex : -1;
            if (lanIsHost) {
                if (lanHost != null) {
                    lanHost.broadcastMatchEnd(winnerIndex);
                }
            }
            final Stage finalStage = currentStage;
            new AnimationTimer() {
                private int framesLeft = 90;
                @Override public void handle(long now) {
                    if (framesLeft > 0) {
                        framesLeft--;
                        return;
                    }
                    this.stop();
                    if (timer != null) timer.stop();
                    showLanResults(finalStage, winnerIndex);
                }
            }.start();
            return;
        }

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
                if (tournamentModeActive && currentTournamentMatch != null && !tournamentMatchResolved) {
                    TournamentEntry winnerEntry = resolveTournamentWinnerEntry(finalWinner);
                    if (winnerEntry != null) {
                        recordTournamentWinner(currentTournamentMatch, winnerEntry);
                    }
                    tournamentMatchResolved = true;
                }
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
        double spawnChance = 0.8;
        if (random.nextDouble() < spawnChance) {
            double[] spawn = pickPowerUpSpawnPoint();
            double x = spawn[0];
            double y = spawn[1];
            PowerUpType type;
            if (selectedMap == MapType.SKYCLIFFS && random.nextDouble() < 0.35) {
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
        boolean battlefield = selectedMap == MapType.BATTLEFIELD && battlefieldIslandW > 0;
        double battlefieldMinX = 0;
        double battlefieldMaxX = 0;
        if (battlefield) {
            double margin = Math.min(140, Math.max(80, battlefieldIslandW * 0.08));
            battlefieldMinX = battlefieldIslandX + margin;
            battlefieldMaxX = battlefieldIslandX + battlefieldIslandW - margin;
            if (battlefieldMaxX <= battlefieldMinX) {
                battlefieldMinX = battlefieldIslandX;
                battlefieldMaxX = battlefieldIslandX + battlefieldIslandW;
            }
        }

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
                if (battlefield) {
                    x = Math.max(battlefieldMinX, Math.min(x, battlefieldMaxX));
                }
                return new double[]{x, y};
            }
        }

        double x;
        if (battlefield) {
            x = battlefieldMinX + random.nextDouble() * Math.max(1, battlefieldMaxX - battlefieldMinX);
        } else {
            x = 200 + random.nextDouble() * (WORLD_WIDTH - 400);
        }
        // Clamp to above ground so power-ups never spawn beneath the floor.
        double y = 260 + random.nextDouble() * (GROUND_Y - 520);
        y = Math.min(y, GROUND_Y - 120);
        return new double[]{x, y};
    }

    private void handleGameplayKeyPress(Stage stage, KeyEvent e) {
        KeyCode code = e.getCode();
        if (lanModeActive && lanIsClient) {
            handleLanKeyPress(stage, e);
            return;
        }
        if (code == KeyCode.F11) {
            stage.setFullScreen(!stage.isFullScreen());
            return;
        }
        if (code == KeyCode.F3) {
            debugHudEnabled = !debugHudEnabled;
            addToKillFeed(debugHudEnabled ? "DEBUG HUD: ON" : "DEBUG HUD: OFF");
            return;
        }
        if (lanModeActive) {
            if (code == KeyCode.ESCAPE) {
                confirmLeaveLanMatch(stage);
                return;
            }
            if (lanIsHost && !isGameplayKeyForPlayer(0, code)) {
                return;
            }
        }
        boolean alreadyDown = pressedKeys.contains(code);
        if (!isBlockedInputForAI(code)) {
            pressedKeys.add(code);
        }
        if (code != null && !alreadyDown) {
            registerDashTapForKey(code);
        }
        if (!lanModeActive && e.getCode() == KeyCode.ESCAPE) {
            togglePause(stage);
        }
    }

    private void handleGameplayKeyRelease(KeyEvent e) {
        KeyCode code = e.getCode();
        if (lanModeActive && lanIsClient) {
            handleLanKeyRelease(e);
            return;
        }
        if (lanModeActive && lanIsHost && !isGameplayKeyForPlayer(0, code)) {
            return;
        }
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
        if (lanModeActive && lanIsHost) {
            if (code != leftKeyForPlayer(0) && code != rightKeyForPlayer(0)) return;
        }
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

    private KeyCode jumpKeyForPlayer(int playerIdx) {
        return switch (playerIdx) {
            case 0 -> KeyCode.W;
            case 1 -> KeyCode.UP;
            case 2 -> KeyCode.T;
            case 3 -> KeyCode.I;
            default -> KeyCode.W;
        };
    }

    private KeyCode attackKeyForPlayer(int playerIdx) {
        return switch (playerIdx) {
            case 0 -> KeyCode.SPACE;
            case 1 -> KeyCode.ENTER;
            case 2 -> KeyCode.Y;
            case 3 -> KeyCode.O;
            default -> KeyCode.SPACE;
        };
    }

    private KeyCode specialKeyForPlayer(int playerIdx) {
        return switch (playerIdx) {
            case 0 -> KeyCode.SHIFT;
            case 1 -> KeyCode.SLASH;
            case 2 -> KeyCode.U;
            case 3 -> KeyCode.P;
            default -> KeyCode.SHIFT;
        };
    }

    private KeyCode blockKeyForPlayer(int playerIdx) {
        return switch (playerIdx) {
            case 0 -> KeyCode.S;
            case 1 -> KeyCode.DOWN;
            case 2 -> KeyCode.G;
            case 3 -> KeyCode.K;
            default -> KeyCode.S;
        };
    }

    private KeyCode tauntCycleKeyForPlayer(int playerIdx) {
        return switch (playerIdx) {
            case 0 -> KeyCode.Q;
            case 1 -> KeyCode.NUMPAD1;
            case 2 -> KeyCode.NUMPAD2;
            case 3 -> KeyCode.NUMPAD3;
            default -> KeyCode.Q;
        };
    }

    private KeyCode tauntExecuteKeyForPlayer(int playerIdx) {
        return switch (playerIdx) {
            case 0 -> KeyCode.E;
            case 1 -> KeyCode.NUMPAD4;
            case 2 -> KeyCode.NUMPAD5;
            case 3 -> KeyCode.NUMPAD6;
            default -> KeyCode.E;
        };
    }

    private boolean isGameplayKeyForPlayer(int playerIdx, KeyCode code) {
        if (code == null) return false;
        return code == leftKeyForPlayer(playerIdx)
                || code == rightKeyForPlayer(playerIdx)
                || code == jumpKeyForPlayer(playerIdx)
                || code == attackKeyForPlayer(playerIdx)
                || code == specialKeyForPlayer(playerIdx)
                || code == blockKeyForPlayer(playerIdx)
                || code == tauntCycleKeyForPlayer(playerIdx)
                || code == tauntExecuteKeyForPlayer(playerIdx);
    }

    private int inputBitForKey(KeyCode code, int playerIdx) {
        if (code == leftKeyForPlayer(playerIdx)) return LanProtocol.INPUT_LEFT;
        if (code == rightKeyForPlayer(playerIdx)) return LanProtocol.INPUT_RIGHT;
        if (code == jumpKeyForPlayer(playerIdx)) return LanProtocol.INPUT_JUMP;
        if (code == attackKeyForPlayer(playerIdx)) return LanProtocol.INPUT_ATTACK;
        if (code == specialKeyForPlayer(playerIdx)) return LanProtocol.INPUT_SPECIAL;
        if (code == blockKeyForPlayer(playerIdx)) return LanProtocol.INPUT_BLOCK;
        if (code == tauntCycleKeyForPlayer(playerIdx)) return LanProtocol.INPUT_TAUNT_CYCLE;
        if (code == tauntExecuteKeyForPlayer(playerIdx)) return LanProtocol.INPUT_TAUNT_EXEC;
        return 0;
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
        g.fillText("Match " + (int) elapsedSec + "s | Map: " + mapDisplayName(selectedMap),
                panelX + 18, panelY + 58);

        g.setFill(Color.WHITE);
        g.setFont(Font.font("Consolas", 16));
        g.fillText("Player                        DPS   Specials  Elims  Falls  WRd", panelX + 18, panelY + 84);

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
        g.fillText("WRd = bird type lifetime win-rate delta from 50%", panelX + 18, panelY + panelH - 10);
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
            if (playerIdx >= 0 && playerIdx < classicCpuLevels.length && classicCpuLevels[playerIdx] > 0) {
                level = classicCpuLevels[playerIdx];
            } else {
                level = getClassicCpuLevel();
            }
        } else if (playerIdx >= 0 && playerIdx < cpuLevels.length) {
            level = cpuLevels[playerIdx];
        }
        if (level < 1) level = 1;
        if (level > 9) level = 9;
        return level;
    }

    private int getClassicCpuLevel() {
        int total = classicRun == null ? 0 : classicRun.size();
        if (total <= 1) return 5;
        int maxIndex = Math.max(1, total - 1);
        int level = 1 + (int) Math.round(8.0 * classicRoundIndex / maxIndex);
        if (level < 1) level = 1;
        if (level > 9) level = 9;
        return level;
    }

    public int getEffectiveTeam(int playerIdx) {
        if (playerIdx < 0 || playerIdx >= players.length) return -1;
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

    private double battlefieldBoundsMargin() {
        return Math.max(500, battlefieldIslandW * 0.7);
    }

    double battlefieldLeftBound() {
        if (battlefieldIslandW > 0) {
            return battlefieldIslandX - battlefieldBoundsMargin();
        }
        return 0;
    }

    double battlefieldRightBound() {
        if (battlefieldIslandW > 0) {
            return battlefieldIslandX + battlefieldIslandW + battlefieldBoundsMargin();
        }
        return WORLD_WIDTH;
    }

    private void startMatch(Stage stage) { 
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
        if (trainingModeActive) {
            setupTrainingRoster();
        } else if (classicModeActive) {
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
            int slots = lanModeActive ? LAN_MAX_PLAYERS : activePlayers;
            for (int i = 0; i < slots; i++) {
                if (lanModeActive && !lanSlotConnected[i]) {
                    players[i] = null;
                    isAI[i] = false;
                    continue;
                }
                BirdType type = lanModeActive ? lanSelectedBirds[i] : fightSelectedBirds[i];
                boolean randomPick = lanModeActive ? lanRandomBirds[i] : fightRandomSelected[i];
                if (type == null || (lanModeActive && randomPick) || (!lanModeActive && (randomPick || !isBirdUnlocked(type)))) {
                    type = pool.get(random.nextInt(pool.size()));
                    if (lanModeActive) {
                        lanSelectedBirds[i] = type;
                        lanRandomBirds[i] = false;
                    }
                }
                double startX = 300 + i * (WIDTH - 600) / (double) Math.max(1, slots - 1);
                isAI[i] = !lanModeActive && menuAI[i];
                players[i] = new Bird(startX, type, i, this);
                if (lanModeActive) {
                    String skinKey = randomPick ? null : lanSelectedSkinKeys[i];
                    applyPreviewSkinChoiceToBird(players[i], type, skinKey);
                } else {
                    String skinKey = randomPick ? null : fightSelectedSkinKeys[i];
                    skinKey = normalizeAdventureSkinChoice(type, skinKey);
                    fightSelectedSkinKeys[i] = skinKey;
                    applySkinChoiceToBird(players[i], type, skinKey);
                }

                applyAdaptiveBalance(players[i]);
            }
        }

        if (tournamentModeActive) {
            applyTournamentSlotNames();
        }

        platforms.clear();
        windVents.clear();
        nectarNodes.clear();
        swingingVines.clear();
        crowMinions.clear();
        particles.clear();
        powerUps.clear();

        if (selectedMap != MapType.CITY || lanModeActive) cityStars.clear();

        Random mapRandom = lanModeActive ? new Random(lanMatchSeed) : random;

        if (selectedMap == MapType.CITY && cityStars.isEmpty()) {
            for (int i = 0; i < 250; i++) {
                cityStars.add(new double[]{
                        mapRandom.nextDouble() * WORLD_WIDTH,
                        mapRandom.nextDouble() * (GROUND_Y - 200)
                });
            }
        }

// Common elements for both maps
        if (selectedMap == MapType.BATTLEFIELD) {
            double islandW = 1200;
            double islandH = 70;
            double islandX = (WORLD_WIDTH - islandW) / 2.0;
            double islandY = GROUND_Y - 80;
            platforms.add(new Platform(islandX, islandY, islandW, islandH));
            platforms.add(new Platform(islandX + 120, islandY - 260, 360, 45));
            platforms.add(new Platform(islandX + islandW - 480, islandY - 260, 360, 45));
            platforms.add(new Platform(islandX + (islandW - 420) / 2.0, islandY - 420, 420, 45));
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
            Random rand = mapRandom;
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
                mountainPeaks[i] = GROUND_Y - 400 - mapRandom.nextDouble() * 600;
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
            Random rand = mapRandom;
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
                platforms.add(new Platform(sx - 140, 240 + mapRandom.nextDouble() * 300, 280, 34));
            }

            // Midair crystal bridges
            for (int i = 0; i < 12; i++) {
                double px = 260 + i * 470 + mapRandom.nextDouble() * 120;
                double py = 500 + mapRandom.nextDouble() * (GROUND_Y - 900);
                platforms.add(new Platform(px, py, 220 + mapRandom.nextDouble() * 180, 30));
            }

            // Vertical shafts with updraft pockets
            double[] ventX = {950, 2400, 3900, 5400};
            for (double vx : ventX) {
                windVents.add(new WindVent(vx - 180, GROUND_Y - 820, 360));
                windVents.add(new WindVent(vx - 130, GROUND_Y - 1450, 260));
            }

            mountainPeaks = new double[7];
            for (int i = 0; i < mountainPeaks.length; i++) {
                mountainPeaks[i] = GROUND_Y - 1000 - mapRandom.nextDouble() * 800;
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
                high1.signText = possibleSigns[mapRandom.nextInt(possibleSigns.length)];
                high2.signText = possibleSigns[mapRandom.nextInt(possibleSigns.length)];

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

            // NO random floating pillars/antennas - removed completely

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
        if (trainingModeActive) {
            captureTrainingSpawns();
        }

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
        resetRenderTimer();

        timer = new AnimationTimer() {
            @Override
            public void handle(long now) {
                gameTick(1.0);
                if (lanModeActive && lanIsHost) {
                    boolean hasClients = lanHost != null && lanHost.hasClients();
                    if (hasClients && now - lastLanSnapshotNs >= LAN_SNAPSHOT_INTERVAL_NS) {
                        if (lanHost != null) {
                            lanHost.broadcastState(buildLanState());
                        }
                        lastLanSnapshotNs = now;
                    }
                }
                if (!shouldRenderFrame(now)) {
                    return;
                }
                drawGame(g);   // <- ONE LINE

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

                if (activeMutator != MatchMutator.NONE && !competitionModeEnabled && !storyModeActive && !adventureModeActive && !classicModeActive) {
                    ui.setFill(Color.web("#80DEEA"));
                    ui.setFont(Font.font("Arial Black", 24));
                    ui.fillText("MUTATOR: " + activeMutator.label.toUpperCase(), WIDTH / 2.0 - 220, healthBarY + 78);
                }
                if (classicModeActive && classicEncounter != null) {
                    ui.setFill(Color.web("#FFE082"));
                    ui.setFont(Font.font("Arial Black", 22));
                    ui.fillText(
                            "CLASSIC " + (classicRoundIndex + 1) + "/" + classicRun.size() + "  " + classicEncounter.name.toUpperCase(),
                            WIDTH / 2.0 - 360,
                            healthBarY + 104
                    );
                    ui.setFill(Color.web("#B3E5FC"));
                    ui.setFont(Font.font("Consolas", 18));
                    ui.fillText("RULES: " + classicEncounter.mutator.label + " | " + classicEncounter.twist.label,
                            WIDTH / 2.0 - 240, healthBarY + 130);
                    int livesLeft = Math.max(0, 3 - classicDeaths);
                    ui.setFill(Color.web("#C8E6C9"));
                    ui.setFont(Font.font("Consolas", 18));
                    ui.fillText("LIVES: " + livesLeft + "/3  CONTINUES: " + classicContinues,
                            WIDTH / 2.0 - 190, healthBarY + 154);
                }
                if (competitionModeEnabled && !storyModeActive && !adventureModeActive && !classicModeActive) {
                    ui.setFill(Color.web("#FFD54F"));
                    ui.setFont(Font.font("Arial Black", 22));
                    ui.fillText("COMPETITION MODE", WIDTH / 2.0 - 160, healthBarY + 78);
                    ui.setFill(Color.web("#FFF59D"));
                    ui.setFont(Font.font("Consolas", 21));
                    ui.fillText(competitionScoreLine(), WIDTH / 2.0 - 245, healthBarY + 104);
                }

                ui.setFill(Color.WHITE);
                ui.setFont(Font.font("Arial Black", 48));
                String timeText;
                if (trainingModeActive) {
                    timeText = "TRAINING";
                    ui.setFill(Color.web("#80DEEA"));
                } else {
                    timeText = matchTimer > 0 ? String.format("TIME: %d", matchTimer / 60) : "SUDDEN DEATH!";
                    if (matchTimer <= 600 && matchTimer > 0) {
                        ui.setFill(Color.RED.brighter());
                        ui.setEffect(new Glow(0.8));
                    } else if (matchTimer <= 0) {
                        ui.setFill(Color.CRIMSON.darker());
                        ui.setEffect(new Glow(1.2));
                    }
                }
                ui.fillText(timeText, WIDTH / 2.0 - 120, 80);
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
        boolean compStyle = competitionModeEnabled && !storyModeActive && !adventureModeActive && !classicModeActive;
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
        if (trainingModeActive) return 0;
        int playerCount = lanModeActive ? countLanConnected() : activePlayers;
        int coins = 30 + playerCount * 10;
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
            if (musicEnabled) victoryMusicPlayer.play();
        }
        VBox root = MenuLayout.buildMenuRoot("-fx-background-color: linear-gradient(to bottom, #0f0c29, #302b63, #24243e);",
                MENU_PADDING, 40);

        Label title = new Label(winner != null ? winner.name.toUpperCase() + " WINS!" : "TIME'S UP!");
        title.setFont(Font.font("Arial Black", 110));
        title.setTextFill(winner != null ? Color.GOLD : Color.SILVER);
        title.setEffect(new DropShadow(50, Color.BLACK));
        title.setWrapText(false);
        applyNoEllipsis(title);

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
                (teamModeEnabled && !storyModeActive && !adventureModeActive && !classicModeActive)
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
            VBox classicPanel = buildClassicSummaryPanel(winner);
            if (classicPanel != null) {
                root.getChildren().addAll(title, coinsLabel, classicPanel, podium, analyticsPanel, buttons);
            } else {
                root.getChildren().addAll(title, coinsLabel, podium, analyticsPanel, buttons);
            }
            String bgStyle = root.getStyle();
            root.setStyle("-fx-background-color: transparent;");
            StackPane container = new StackPane(root);
            container.setAlignment(Pos.CENTER);
            container.setStyle(bgStyle);
            Scene scene = new Scene(container, WIDTH, HEIGHT);
            bindScaleToFit(scene, root, 40);
            scene.widthProperty().addListener((obs, oldVal, newVal) -> {
                double maxWidth = Math.max(300, newVal.doubleValue() - 120);
                fitLabelSingleLine(title, 110, 56, maxWidth);
            });
            javafx.application.Platform.runLater(() -> {
                double maxWidth = Math.max(300, scene.getWidth() - 120);
                fitLabelSingleLine(title, 110, 56, maxWidth);
            });
            setScenePreservingFullscreen(stage, scene);
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

        // === STAT-BASED AWARDS ===
        Bird killLeader = null, mostDamage = null, mostGroundPounds = null,
                mostLean = null, mostTaunts = null, fallGuy = null, healthiest = null;

        int maxKills = -1, maxDamage = -1, maxPounds = -1, maxLean = -1, maxTaunts = -1;

        int maxFalls = -1;
        int maxHealth = -1;

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

            int healthScore = (int) Math.round(Math.max(0, b.health));
            if (healthScore > maxHealth) {
                maxHealth = healthScore;
                healthiest = b;
            }

            if (b.health <= 0) {
                if (fallGuy == null || b.y > fallGuy.y) {
                    fallGuy = b;
                }
            }
        }

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
            String skinKey = skinKeyForBird(bird);
            drawRosterSprite(icon, bird.type, skinKey, false, true);

            String name = bird.name
                    .replace("P1:", "Player 1:")
                    .replace("P2:", "Player 2:")
                    .replace("P3:", "Player 3:")
                    .replace("P4:", "Player 4:")
                    .replace("AI", "AI Player");
            Label nameLabel = new Label(name);
            nameLabel.setFont(Font.font("Arial Black", 34));
            nameLabel.setTextFill(Color.WHITE);

            // === STAT-BASED AWARD LOGIC ===
            String award;

            if (place == 1) {
                award = (bird.health < 20 && bird.health > 0) ? "CLUTCH GOD" : "CHAMPION";
            } else {
                int kills = eliminations[bird.playerIndex];
                int damage = damageDealt[bird.playerIndex];
                int pounds = groundPounds[bird.playerIndex];
                int lean = leanTime[bird.playerIndex];
                int taunts = tauntsPerformed[bird.playerIndex];
                int fallCount = falls[bird.playerIndex];
                int healthScore = (int) Math.round(Math.max(0, bird.health));

                int bestCategory = -1;
                double bestScore = -1;
                if (maxKills > 0 && kills > 0) {
                    double score = kills / (double) maxKills;
                    if (score > bestScore) {
                        bestScore = score;
                        bestCategory = 0;
                    }
                }
                if (maxDamage > 0 && damage > 0) {
                    double score = damage / (double) maxDamage;
                    if (score > bestScore) {
                        bestScore = score;
                        bestCategory = 1;
                    }
                }
                if (maxPounds > 0 && pounds > 0) {
                    double score = pounds / (double) maxPounds;
                    if (score > bestScore) {
                        bestScore = score;
                        bestCategory = 2;
                    }
                }
                if (maxLean > 0 && lean > 0) {
                    double score = lean / (double) maxLean;
                    if (score > bestScore) {
                        bestScore = score;
                        bestCategory = 3;
                    }
                }
                if (maxTaunts > 0 && taunts > 0) {
                    double score = taunts / (double) maxTaunts;
                    if (score > bestScore) {
                        bestScore = score;
                        bestCategory = 4;
                    }
                }
                if (maxFalls > 0 && fallCount > 0) {
                    double score = fallCount / (double) maxFalls;
                    if (score > bestScore) {
                        bestScore = score;
                        bestCategory = 5;
                    }
                }
                if (maxHealth > 0 && healthScore > 0) {
                    double score = healthScore / (double) maxHealth;
                    if (score > bestScore) {
                        bestScore = score;
                        bestCategory = 6;
                    }
                }

                if (bestCategory == 0) {
                    award = (bird == killLeader && maxKills > 0) ? "KILL LEADER" : "FINISHER";
                } else if (bestCategory == 1) {
                    award = (bird == mostDamage && maxDamage > 0) ? "MOST BRUTAL" : "HARD HITTER";
                } else if (bestCategory == 2) {
                    award = (bird == mostGroundPounds && maxPounds > 0) ? "GROUND POUNDER" : "PILEDRIVER";
                } else if (bestCategory == 3) {
                    award = (bird == mostLean && maxLean > 0) ? "LEAN GOD" : "LEANER";
                } else if (bestCategory == 4) {
                    award = (bird == mostTaunts && maxTaunts > 0) ? "TAUNT LORD" : "SHOWBOAT";
                } else if (bestCategory == 5) {
                    award = (bird == fallGuy && maxFalls > 0) ? "FALL GUY" : "RISK TAKER";
                } else if (bestCategory == 6) {
                    award = (bird == healthiest && maxHealth > 0) ? "SURVIVOR" : "STILL FLYING";
                } else {
                    award = bird.health > 0 ? "READY FOR MORE" : "DOWNED";
                }
            }

            Label awardLabel = new Label(award);
            awardLabel.setFont(Font.font("Arial Black", 32));
            awardLabel.setTextFill(Color.CYAN.brighter());
            awardLabel.setEffect(new Glow(0.8));

            slot.getChildren().addAll(placeLabel, icon, nameLabel, awardLabel);
            podium.getChildren().add(slot);
        }

        VBox analyticsPanel = buildPostMatchAnalyticsPanel(activeBirds);
        VBox classicPanel = buildClassicSummaryPanel(winner);
        if (classicPanel != null) {
            root.getChildren().addAll(title, coinsLabel, classicPanel, podium, analyticsPanel, buttons);
        } else {
            root.getChildren().addAll(title, coinsLabel, podium, analyticsPanel, buttons);
        }
        String bgStyle = root.getStyle();
        root.setStyle("-fx-background-color: transparent;");
        StackPane container = new StackPane(root);
        container.setAlignment(Pos.CENTER);
        container.setStyle(bgStyle);
        Scene scene = new Scene(container, WIDTH, HEIGHT);
        bindScaleToFit(scene, root, 40);
        scene.widthProperty().addListener((obs, oldVal, newVal) -> {
            double maxWidth = Math.max(300, newVal.doubleValue() - 120);
            fitLabelSingleLine(title, 110, 56, maxWidth);
        });
        javafx.application.Platform.runLater(() -> {
            double maxWidth = Math.max(300, scene.getWidth() - 120);
            fitLabelSingleLine(title, 110, 56, maxWidth);
        });
        setScenePreservingFullscreen(stage, scene);
    }

    private VBox buildClassicSummaryPanel(Bird winner) {
        if (!classicModeActive || classicEncounter == null || classicRun.isEmpty()) return null;
        boolean won = didPlayerWinClassic(winner);
        boolean finalRound = classicRoundIndex >= classicRun.size() - 1;
        int livesLeft = Math.max(0, 3 - classicDeaths);
        ClassicEncounter current = classicEncounter;
        ClassicEncounter next = (won && !finalRound && classicRoundIndex + 1 < classicRun.size())
                ? classicRun.get(classicRoundIndex + 1)
                : null;

        VBox box = new VBox(8);
        box.setAlignment(Pos.CENTER_LEFT);
        box.setPadding(new Insets(18, 26, 18, 26));
        box.setMaxWidth(1500);
        box.setStyle("-fx-background-color: rgba(0,0,0,0.55); -fx-background-radius: 22; -fx-border-color: #FFD54F; -fx-border-width: 3; -fx-border-radius: 22;");

        Label header = new Label("CLASSIC RUN: " + classicRunCodename);
        header.setFont(Font.font("Arial Black", 34));
        header.setTextFill(Color.web("#FFE082"));
        applyNoEllipsis(header);

        Label status = new Label(won ? "RESULT: VICTORY" : "RESULT: DEFEAT");
        status.setFont(Font.font("Arial Black", 28));
        status.setTextFill(won ? Color.LIMEGREEN : Color.ORANGE);
        applyNoEllipsis(status);

        Label progress = new Label("Round " + (classicRoundIndex + 1) + "/" + classicRun.size()
                + "  |  Lives " + livesLeft + "/3  |  Continues " + classicContinues);
        progress.setFont(Font.font("Consolas", 22));
        progress.setTextFill(Color.web("#B3E5FC"));
        applyNoEllipsis(progress);

        Label currentLine = new Label("Current: " + current.name + "  |  " + mapDisplayName(current.map)
                + "  |  " + current.mutator.label + "  |  " + current.twist.label);
        currentLine.setFont(Font.font("Consolas", 22));
        currentLine.setTextFill(Color.web("#E3F2FD"));
        currentLine.setWrapText(true);
        currentLine.setMaxWidth(1450);
        applyNoEllipsis(currentLine);

        box.getChildren().addAll(header, status, progress, currentLine);

        if (next != null) {
            Label nextLine = new Label("Next: " + next.name + "  |  " + mapDisplayName(next.map)
                    + "  |  " + next.mutator.label + "  |  " + next.twist.label);
            nextLine.setFont(Font.font("Consolas", 21));
            nextLine.setTextFill(Color.web("#C5E1A5"));
            nextLine.setWrapText(true);
            nextLine.setMaxWidth(1450);
            applyNoEllipsis(nextLine);
            box.getChildren().add(nextLine);
        } else if (won && finalRound) {
            Label nextLine = new Label("Next: CLAIM REWARD");
            nextLine.setFont(Font.font("Consolas", 21));
            nextLine.setTextFill(Color.web("#C8E6C9"));
            applyNoEllipsis(nextLine);
            box.getChildren().add(nextLine);
        } else if (!won) {
            String nextText = livesLeft > 0 ? "Next: RETRY ENCOUNTER" : "Next: RUN FAILED";
            Label nextLine = new Label(nextText);
            nextLine.setFont(Font.font("Consolas", 21));
            nextLine.setTextFill(Color.web("#FFCCBC"));
            applyNoEllipsis(nextLine);
            box.getChildren().add(nextLine);
        }
        return box;
    }

    private Label labelFor(Bird b, String stat) {
        return new Label(b != null ? b.name + " (" + stat + ")" : "-");
    }

    private Button button(String text, String color) {
        Button b = uiFactory.action(text, 520, 120, 40, color, 36, null);
        b.setWrapText(false);
        uiFactory.fitSingleLineOnLayout(b, 40, 16);
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

            Button menu = button("BACK TO HUB", "#D32F2F");
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
            Button menu = button("BACK TO HUB", "#D32F2F");
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
            Button menu = button("BACK TO HUB", "#D32F2F");
            menu.setOnAction(e -> {
                resetMatchStats();
                showMenu(stage);
            });
            buttons.getChildren().addAll(continueAdventure, hub, menu);
        } else if (tournamentModeActive) {
            Button bracket = button("VIEW BRACKET", "#1565C0");
            bracket.setOnAction(e -> {
                resetMatchStats();
                showTournamentBracket(stage);
            });
            Button exit = button("EXIT TO HUB", "#9C27B0");
            exit.setOnAction(e -> {
                resetMatchStats();
                resetTournamentRun();
                showMenu(stage);
            });
            buttons.getChildren().addAll(bracket, exit);
        } else {
            Button rematch = button("REMATCH", "#FF1744");
            rematch.setOnAction(e -> {
                resetMatchStats();
                competitionSeriesActive = false;
                Arrays.fill(competitionRoundWins, 0);
                Arrays.fill(competitionTeamWins, 0);
                competitionRoundNumber = 1;
                startMatch(stage);
            });
            Button menu = button("BACK TO HUB", "#D32F2F");
            menu.setOnAction(e -> {
                resetMatchStats();
                showMenu(stage);
            });
            buttons.getChildren().addAll(rematch, menu);
        }
        return buttons;
    }

    private void applyWinnerMapProgress(Bird winner) {
        if (winner == null || trainingModeActive) return;
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

        if (storyModeActive || adventureModeActive || trainingModeActive) return;

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
        Arrays.fill(loungeAchievementSnapshot, 0);
        Arrays.fill(leanAchievementSnapshot, 0);
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
        classicContinues = Math.max(0, prefs.getInt("classic_continues", 0));
        caveMapUnlocked = prefs.getBoolean("map_cave_unlocked", false);
        battlefieldMapUnlocked = prefs.getBoolean("map_battlefield_unlocked", false);
        cityPigeonUnlocked = true;
        noirPigeonUnlocked = prefs.getBoolean("skin_noirpigeon", false);
        beaconPigeonUnlocked = prefs.getBoolean("skin_beacon_pigeon", false);
        eagleSkinUnlocked = prefs.getBoolean("skin_eagle", true);
        novaPhoenixUnlocked = prefs.getBoolean("skin_nova_phoenix", false);
        duneFalconUnlocked = prefs.getBoolean("skin_dune_falcon", false);
        mintPenguinUnlocked = prefs.getBoolean("skin_mint_penguin", false);
        circuitTitmouseUnlocked = prefs.getBoolean("skin_circuit_titmouse", false);
        prismRazorbillUnlocked = prefs.getBoolean("skin_prism_razorbill", false);
        auroraPelicanUnlocked = prefs.getBoolean("skin_aurora_pelican", false);
        sunflareHummingbirdUnlocked = prefs.getBoolean("skin_sunflare_hummingbird", false);
        glacierShoebillUnlocked = prefs.getBoolean("skin_glacier_shoebill", false);
        tideVultureUnlocked = prefs.getBoolean("skin_tide_vulture", false);
        eclipseMockingbirdUnlocked = prefs.getBoolean("skin_eclipse_mockingbird", false);
        umbraBatUnlocked = prefs.getBoolean("skin_umbra_bat", false);
        batUnlocked = prefs.getBoolean("char_bat_unlocked", false);
        falconUnlocked = prefs.getBoolean("char_falcon_unlocked", false);
        heisenbirdUnlocked = prefs.getBoolean("char_heisenbird_unlocked", false);
        phoenixUnlocked = prefs.getBoolean("char_phoenix_unlocked", false);
        titmouseUnlocked = prefs.getBoolean("char_titmouse_unlocked", false);
        ravenUnlocked = prefs.getBoolean("char_raven_unlocked", false);
        musicEnabled = prefs.getBoolean("setting_music", true);
        sfxEnabled = prefs.getBoolean("setting_sfx", true);
        screenShakeEnabled = prefs.getBoolean("setting_shake", true);
        fullscreenEnabled = prefs.getBoolean("setting_fullscreen", true);
        particleEffectsEnabled = prefs.getBoolean("setting_particles", true);
        ambientEffectsEnabled = prefs.getBoolean("setting_ambient_fx", true);
        fpsCap = sanitizeFpsCap(prefs.getInt("setting_fps_cap", 60));
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
        ensureAdventureChapterState();
        for (int i = 0; i < adventureChapters.length; i++) {
            int advBattles = adventureChapters[i].battles.length;
            int progress = Math.max(0, Math.min(prefs.getInt("adv_ch" + (i + 1) + "_progress", 0), advBattles));
            boolean done = prefs.getBoolean("adv_ch" + (i + 1) + "_done", false);
            if (done && advBattles > 0) {
                progress = advBattles;
            }
            adventureChapterProgressByIndex[i] = progress;
            adventureChapterCompletedByIndex[i] = done;
        }
        adventureChapterIndex = Math.max(0, Math.min(adventureChapterIndex, adventureChapters.length - 1));
        adventureChapterProgress = adventureChapterProgressByIndex[adventureChapterIndex];
        adventureChapterCompleted = adventureChapterCompletedByIndex[adventureChapterIndex];
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
        prefs.putInt("classic_continues", classicContinues);
        prefs.putBoolean("map_cave_unlocked", caveMapUnlocked);
        prefs.putBoolean("map_battlefield_unlocked", battlefieldMapUnlocked);
        prefs.putBoolean("skin_citypigeon", cityPigeonUnlocked);
        prefs.putBoolean("skin_noirpigeon", noirPigeonUnlocked);
        prefs.putBoolean("skin_beacon_pigeon", beaconPigeonUnlocked);
        prefs.putBoolean("skin_eagle", eagleSkinUnlocked);
        prefs.putBoolean("skin_nova_phoenix", novaPhoenixUnlocked);
        prefs.putBoolean("skin_dune_falcon", duneFalconUnlocked);
        prefs.putBoolean("skin_mint_penguin", mintPenguinUnlocked);
        prefs.putBoolean("skin_circuit_titmouse", circuitTitmouseUnlocked);
        prefs.putBoolean("skin_prism_razorbill", prismRazorbillUnlocked);
        prefs.putBoolean("skin_aurora_pelican", auroraPelicanUnlocked);
        prefs.putBoolean("skin_sunflare_hummingbird", sunflareHummingbirdUnlocked);
        prefs.putBoolean("skin_glacier_shoebill", glacierShoebillUnlocked);
        prefs.putBoolean("skin_tide_vulture", tideVultureUnlocked);
        prefs.putBoolean("skin_eclipse_mockingbird", eclipseMockingbirdUnlocked);
        prefs.putBoolean("skin_umbra_bat", umbraBatUnlocked);
        prefs.putBoolean("char_bat_unlocked", batUnlocked);
        prefs.putBoolean("char_falcon_unlocked", falconUnlocked);
        prefs.putBoolean("char_heisenbird_unlocked", heisenbirdUnlocked);
        prefs.putBoolean("char_phoenix_unlocked", phoenixUnlocked);
        prefs.putBoolean("char_titmouse_unlocked", titmouseUnlocked);
        prefs.putBoolean("char_raven_unlocked", ravenUnlocked);
        prefs.putBoolean("setting_music", musicEnabled);
        prefs.putBoolean("setting_sfx", sfxEnabled);
        prefs.putBoolean("setting_shake", screenShakeEnabled);
        prefs.putBoolean("setting_fullscreen", fullscreenEnabled);
        prefs.putBoolean("setting_particles", particleEffectsEnabled);
        prefs.putBoolean("setting_ambient_fx", ambientEffectsEnabled);
        prefs.putInt("setting_fps_cap", fpsCap);
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
        ensureAdventureChapterState();
        if (adventureChapterIndex >= 0 && adventureChapterIndex < adventureChapterProgressByIndex.length) {
            adventureChapterProgressByIndex[adventureChapterIndex] = adventureChapterProgress;
            adventureChapterCompletedByIndex[adventureChapterIndex] = adventureChapterCompleted;
        }
        for (int i = 0; i < adventureChapters.length; i++) {
            prefs.putInt("adv_ch" + (i + 1) + "_progress", adventureChapterProgressByIndex[i]);
            prefs.putBoolean("adv_ch" + (i + 1) + "_done", adventureChapterCompletedByIndex[i]);
        }
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
            System.err.println("Failed to save preferences: " + e.getMessage());
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

        // Lean God - 30 seconds total in lean cloud
        if (bird.type == BirdType.OPIUMBIRD) {
            int leanDelta = Math.max(0, leanTime[idx] - leanAchievementSnapshot[idx]);
            if (leanDelta > 0) {
                achievementProgress[4] += leanDelta;
                leanAchievementSnapshot[idx] = leanTime[idx];
            }
            if (achievementProgress[4] >= 1800 && !achievementsUnlocked[4]) { // 30 seconds = 1800 frames @ 60fps
                unlockAchievement(4, "LEAN GOD!");
            }
        }

        // Lounge Lizard - heal 100 HP total
        if (bird.type == BirdType.MOCKINGBIRD) {
            int loungeDelta = Math.max(0, loungeTime[idx] - loungeAchievementSnapshot[idx]);
            if (loungeDelta > 0) {
                achievementProgress[5] += (int) Math.round(loungeDelta * (12.0 / 60.0));
                loungeAchievementSnapshot[idx] = loungeTime[idx];
            }
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

    private void checkAdventureAchievements() {
        ensureAdventureChapterState();
        if (adventureChapterCompletedByIndex.length >= 2 && adventureChapterCompletedByIndex[1] && !achievementsUnlocked[20]) {
            unlockAchievement(20, "ECHOES BELOW!");
        }
        boolean allDone = adventureChapterCompletedByIndex.length > 0;
        for (boolean done : adventureChapterCompletedByIndex) {
            if (!done) {
                allDone = false;
                break;
            }
        }
        if (allDone && !achievementsUnlocked[21]) {
            unlockAchievement(21, "STORY KEEPER!");
        }
    }

    private void checkClassicAchievements() {
        if (achievementsUnlocked[19]) return;
        for (boolean done : classicCompleted) {
            if (done) {
                unlockAchievement(19, "CLASSIC CREST!");
                break;
            }
        }
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
        String cleaned = text;
        // Normalize common punctuation and encoding noise.
        cleaned = cleaned
                .replace("\u2013", " - ")
                .replace("\u2014", " - ")
                .replace("\u2015", " - ")
                .replace("\u2212", "-")
                .replace("\u201C", "\"")
                .replace("\u201D", "\"")
                .replace("\u2018", "'")
                .replace("\u2019", "'")
                .replace("\u2022", "-")
                .replace("\u2713", "")
                .replace("\uFFFD", "");
        // Common UTF-8 -> CP1252 mojibake sequences.
        cleaned = cleaned
                .replace("\u00C3\u00A2\u00E2\u201A\u00AC\u00E2\u20AC\u0153", " - ")
                .replace("\u00C3\u00A2\u00E2\u201A\u00AC\u00E2\u20AC\u201D", " - ")
                .replace("\u00C3\u00A2\u00E2\u20AC\u009D", "\"")
                .replace("\u00C3\u00A2\u00E2\u20AC\u0153", "\"")
                .replace("\u00C3\u00A2\u00C5\u201C\u00E2\u20AC\u0153", "")
                .replace("\u00C3\u00A2\u00E2\u20AC\u0090", "-");
        return cleaned;
    }

    private void togglePause(Stage stage) {
        Scene scene = stage.getScene();
        if (isPaused) {
            // === RESUME ===
            scene.setOnKeyPressed(e -> handleGameplayKeyPress(stage, e));
            gameRoot.getChildren().removeIf(node -> node instanceof VBox && "pauseMenu".equals(node.getId()));

            lastUpdate = 0;      // <- CRITICAL: reset so no speed-up
            accumulator = 0;
            resetRenderTimer();

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
                startMatch(stage);
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




