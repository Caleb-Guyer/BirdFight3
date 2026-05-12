package com.example.birdgame3;

import javafx.animation.AnimationTimer;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.TextAlignment;
import javafx.stage.Stage;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ThreadLocalRandom;
import java.util.prefs.Preferences;

public class BirdFight3CompanionProgram extends Application {
    private static final int COMPANION_PORT = 29000;
    private static final int COMPANION_VERSION = 1;
    private static final byte MSG_COMPANION_SNAPSHOT = 13;
    private static final int PHASE_LOBBY = 0;
    private static final int PHASE_COUNTDOWN = 1;
    private static final int PHASE_MATCH = 2;
    private static final int PHASE_RESULTS = 3;
    private static final long DRAW_INTERVAL_NS = 33_333_333L;
    private static final int SPARK_COUNT = 44;
    private static final int MAX_FRAME_BYTES = 128 * 1024;

    private final Canvas canvas = new Canvas(1280, 720);
    private final Deque<EventToast> toasts = new ArrayDeque<>();
    private final double[] sparkX = new double[SPARK_COUNT];
    private final double[] sparkY = new double[SPARK_COUNT];
    private final double[] sparkSpeed = new double[SPARK_COUNT];
    private final double[] sparkSize = new double[SPARK_COUNT];
    private final Preferences preferences = Preferences.userNodeForPackage(BirdFight3CompanionProgram.class);

    private volatile Snapshot snapshot = Snapshot.empty();
    private volatile boolean readerRunning;
    private volatile boolean connected;
    private volatile String connectionStatus = "Not connected";
    private volatile int readerGeneration;
    private Socket activeSocket;
    private Thread readerThread;
    private TextField hostField;
    private Button connectButton;
    private Button disconnectButton;
    private Label statusLabel;
    private long lastDrawNs;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage stage) {
        initSparks();

        hostField = new TextField(preferences.get("host", "localhost"));
        hostField.setPromptText("Host IP");
        hostField.setPrefColumnCount(18);
        hostField.setStyle("-fx-font-family: Consolas; -fx-font-size: 16px;");

        connectButton = new Button("Connect");
        connectButton.setOnAction(e -> connectToHost());
        disconnectButton = new Button("Disconnect");
        disconnectButton.setDisable(true);
        disconnectButton.setOnAction(e -> disconnect());

        statusLabel = new Label(connectionStatus);
        statusLabel.setFont(Font.font("Consolas", 15));
        statusLabel.setTextFill(Color.web("#B2DFDB"));
        HBox.setHgrow(statusLabel, Priority.ALWAYS);

        HBox topBar = new HBox(10,
                new Label("Host"),
                hostField,
                connectButton,
                disconnectButton,
                statusLabel);
        topBar.setAlignment(Pos.CENTER_LEFT);
        topBar.setPadding(new Insets(10, 14, 10, 14));
        topBar.setStyle("-fx-background-color: #101820; -fx-text-fill: white;");
        topBar.getChildren().getFirst().setStyle("-fx-text-fill: #CFD8DC; -fx-font-family: Consolas; -fx-font-size: 15px;");

        StackPane canvasPane = new StackPane(canvas);
        canvas.widthProperty().bind(canvasPane.widthProperty());
        canvas.heightProperty().bind(canvasPane.heightProperty());

        BorderPane root = new BorderPane(canvasPane);
        root.setTop(topBar);
        root.setStyle("-fx-background-color: #071015;");

        Scene scene = new Scene(root, 1280, 720);
        stage.setTitle("Bird Fight 3 Companion");
        stage.setScene(scene);
        stage.setMinWidth(900);
        stage.setMinHeight(560);
        stage.setOnCloseRequest(e -> disconnect());
        stage.show();

        AnimationTimer timer = new AnimationTimer() {
            @Override
            public void handle(long now) {
                if (lastDrawNs != 0 && now - lastDrawNs < DRAW_INTERVAL_NS) {
                    return;
                }
                double dt = lastDrawNs == 0 ? 0.033 : Math.min(0.1, (now - lastDrawNs) / 1_000_000_000.0);
                lastDrawNs = now;
                draw(dt);
            }
        };
        timer.start();
    }

    private void connectToHost() {
        String host = hostField.getText() == null ? "" : hostField.getText().trim();
        if (host.isBlank()) {
            setConnectionStatus("Enter the LAN host IP.", false);
            return;
        }
        preferences.put("host", host);
        disconnect();
        int generation = ++readerGeneration;
        readerRunning = true;
        setConnectionStatus("Connecting to " + host + ":" + COMPANION_PORT + "...", false);
        connectButton.setDisable(true);
        disconnectButton.setDisable(false);
        readerThread = new Thread(() -> readLoop(host, generation), "BF3-Companion-Reader");
        readerThread.setDaemon(true);
        readerThread.start();
    }

    private void disconnect() {
        readerGeneration++;
        readerRunning = false;
        connected = false;
        Socket socket = activeSocket;
        activeSocket = null;
        if (socket != null) {
            try {
                socket.close();
            } catch (IOException ignored) {
            }
        }
        Platform.runLater(() -> {
            connectButton.setDisable(false);
            disconnectButton.setDisable(true);
            if (!connectionStatus.startsWith("Disconnected")) {
                setConnectionStatus("Not connected", false);
            }
        });
    }

    private void readLoop(String host, int generation) {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(host, COMPANION_PORT), 3500);
            socket.setTcpNoDelay(true);
            if (generation != readerGeneration) {
                return;
            }
            activeSocket = socket;
            connected = true;
            setConnectionStatus("Connected to " + host + ":" + COMPANION_PORT, true);

            DataInputStream in = new DataInputStream(socket.getInputStream());
            while (readerRunning && generation == readerGeneration) {
                Snapshot next = Snapshot.read(readFramed(in));
                Platform.runLater(() -> acceptSnapshot(next));
            }
        } catch (IOException e) {
            if (readerRunning && generation == readerGeneration) {
                setConnectionStatus("Disconnected: " + shortError(e), false);
            }
        } finally {
            if (generation == readerGeneration) {
                connected = false;
                readerRunning = false;
                activeSocket = null;
                Platform.runLater(() -> {
                    connectButton.setDisable(false);
                    disconnectButton.setDisable(true);
                });
            }
        }
    }

    private static byte[] readFramed(DataInputStream in) throws IOException {
        int length = in.readInt();
        if (length <= 0 || length > MAX_FRAME_BYTES) {
            throw new IOException("Invalid frame length: " + length);
        }
        byte[] payload = new byte[length];
        in.readFully(payload);
        return payload;
    }

    private void acceptSnapshot(Snapshot next) {
        Snapshot old = snapshot;
        snapshot = next;
        if (next.suddenDeathActive && !old.suddenDeathActive) {
            addToast("SUDDEN DEATH", Color.web("#FF5252"), 5200);
        }
        String newFeed = next.killFeed.isEmpty() ? "" : next.killFeed.getFirst();
        String oldFeed = old.killFeed.isEmpty() ? "" : old.killFeed.getFirst();
        if (!newFeed.isBlank() && !newFeed.equals(oldFeed)) {
            Color color = isKoLine(newFeed) ? Color.web("#FFAB40") : Color.web("#80CBC4");
            addToast(newFeed, color, isKoLine(newFeed) ? 4400 : 3000);
        }
        if (next.phase == PHASE_RESULTS && old.phase != PHASE_RESULTS) {
            addToast(next.status.toUpperCase(Locale.ROOT), Color.web("#FFD54F"), 5200);
        }
    }

    private static boolean isKoLine(String line) {
        String upper = line == null ? "" : line.toUpperCase(Locale.ROOT);
        return upper.contains("KO") || upper.contains("ELIMINATED") || upper.contains("FLEW INTO")
                || upper.contains("DROWNED") || upper.contains("BLASTED OUT");
    }

    private void addToast(String text, Color color, long durationMs) {
        if (text == null || text.isBlank()) return;
        while (toasts.size() >= 4) {
            toasts.removeFirst();
        }
        toasts.addLast(new EventToast(text, color, System.currentTimeMillis(), durationMs));
    }

    private void setConnectionStatus(String status, boolean ok) {
        connectionStatus = status == null ? "" : status;
        Platform.runLater(() -> {
            if (statusLabel != null) {
                statusLabel.setText(connectionStatus);
                statusLabel.setTextFill(ok ? Color.web("#80CBC4") : Color.web("#FFCC80"));
            }
        });
    }

    private static String shortError(IOException e) {
        String message = e.getMessage();
        return message == null || message.isBlank() ? e.getClass().getSimpleName() : message;
    }

    private void initSparks() {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        for (int i = 0; i < SPARK_COUNT; i++) {
            sparkX[i] = random.nextDouble();
            sparkY[i] = random.nextDouble();
            sparkSpeed[i] = 0.015 + random.nextDouble() * 0.045;
            sparkSize[i] = 1.0 + random.nextDouble() * 2.4;
        }
    }

    private void draw(double dt) {
        GraphicsContext g = canvas.getGraphicsContext2D();
        double w = Math.max(1, canvas.getWidth());
        double h = Math.max(1, canvas.getHeight());
        Snapshot s = snapshot;

        drawBackground(g, w, h, dt, s);
        drawHeader(g, w, s);

        boolean compact = w < 1040;
        double margin = compact ? 18 : 28;
        double top = 112;
        double feedW = compact ? 0 : Math.min(390, w * 0.30);
        double boardW = compact ? w - margin * 2 : w - margin * 3 - feedW;
        double cardGap = compact ? 10 : 14;
        double cardW = Math.max(130, (boardW - cardGap * 3) / 4.0);
        double cardH = compact ? Math.min(260, h * 0.42) : Math.min(360, h - 180);

        for (int i = 0; i < 4; i++) {
            double x = margin + i * (cardW + cardGap);
            drawPlayerPanel(g, s, i, x, top, cardW, cardH);
        }

        if (compact) {
            drawFeed(g, s, margin, top + cardH + 16, w - margin * 2, Math.max(120, h - top - cardH - 38));
        } else {
            drawFeed(g, s, margin * 2 + boardW, top, feedW, cardH);
        }

        drawCenterEvent(g, w, h, s);
        drawToasts(g, w, h);
    }

    private void drawBackground(GraphicsContext g, double w, double h, double dt, Snapshot s) {
        g.setFill(new LinearGradient(0, 0, 1, 1, true, CycleMethod.NO_CYCLE,
                new Stop(0, Color.web("#071015")),
                new Stop(0.55, Color.web(s.suddenDeathActive ? "#241016" : "#0D1D1B")),
                new Stop(1, Color.web("#11141D"))));
        g.fillRect(0, 0, w, h);

        ThreadLocalRandom random = ThreadLocalRandom.current();
        for (int i = 0; i < SPARK_COUNT; i++) {
            sparkY[i] += sparkSpeed[i] * dt;
            sparkX[i] += Math.sin((sparkY[i] + i) * 5.0) * 0.003 * dt;
            if (sparkY[i] > 1.04) {
                sparkY[i] = -0.04;
                sparkX[i] = random.nextDouble();
            }
            double alpha = s.suddenDeathActive ? 0.38 : 0.20;
            g.setFill(Color.color(1.0, s.suddenDeathActive ? 0.28 : 0.72, 0.32, alpha));
            g.fillOval(sparkX[i] * w, sparkY[i] * h, sparkSize[i], sparkSize[i]);
        }

        g.setStroke(Color.color(1, 1, 1, 0.035));
        g.setLineWidth(1);
        for (double y = 0; y < h; y += 4) {
            g.strokeLine(0, y, w, y);
        }
    }

    private void drawHeader(GraphicsContext g, double w, Snapshot s) {
        double margin = w < 980 ? 18 : 28;
        drawPanel(g, margin, 20, w - margin * 2, 70, Color.color(0.03, 0.06, 0.07, 0.76), Color.web("#29434E"));

        g.setTextAlign(TextAlignment.LEFT);
        g.setFont(Font.font("Arial Black", FontWeight.BOLD, w < 980 ? 25 : 34));
        g.setFill(Color.web("#FFE082"));
        g.fillText("BIRD FIGHT 3", margin + 20, 58);

        g.setFont(Font.font("Consolas", FontWeight.BOLD, w < 980 ? 14 : 18));
        g.setFill(Color.web("#B2DFDB"));
        String state = s.status.isBlank() ? phaseName(s.phase) : s.status;
        g.fillText(truncate(g, state + "  |  " + nullSafe(s.mapName), Math.max(180, w * 0.36)),
                margin + 20, 82);

        g.setTextAlign(TextAlignment.RIGHT);
        g.setFont(Font.font("Impact", FontWeight.BOLD, w < 980 ? 32 : 46));
        g.setFill(s.suddenDeathActive ? Color.web("#FF5252") : Color.web("#E0F7FA"));
        String clock = s.phase == PHASE_COUNTDOWN && s.countdownSeconds > 0
                ? Integer.toString(s.countdownSeconds)
                : formatTimer(s.matchTimerFrames);
        g.fillText(clock, w - margin - 22, 70);

        g.setFont(Font.font("Consolas", FontWeight.BOLD, 14));
        g.setFill(connected ? Color.web("#80CBC4") : Color.web("#FFCC80"));
        g.fillText(connected ? "LIVE" : "OFFLINE", w - margin - 22, 86);
    }

    private void drawPlayerPanel(GraphicsContext g, Snapshot s, int index, double x, double y, double w, double h) {
        Player p = index < s.players.length ? s.players[index] : Player.empty();
        Color accent = parseColor(p.colorHex, Color.web("#90A4AE"));
        Color fill = p.connected ? Color.color(0.04, 0.08, 0.09, 0.78) : Color.color(0.03, 0.04, 0.05, 0.62);
        drawPanel(g, x, y, w, h, fill, p.connected ? accent : Color.web("#37474F"));

        g.setTextAlign(TextAlignment.LEFT);
        g.setFont(Font.font("Consolas", FontWeight.BOLD, 16));
        g.setFill(Color.web("#CFD8DC"));
        g.fillText("P" + (index + 1), x + 14, y + 26);

        g.setTextAlign(TextAlignment.RIGHT);
        g.setFont(Font.font("Consolas", FontWeight.BOLD, 14));
        String state = playerStateText(s, p);
        g.setFill(playerStateColor(s, p));
        g.fillText(state, x + w - 14, y + 26);

        g.setTextAlign(TextAlignment.CENTER);
        g.setFont(Font.font("Arial Black", FontWeight.BOLD, scaledFont(w, 26, 17)));
        g.setFill(p.connected ? accent.brighter() : Color.web("#78909C"));
        g.fillText(truncate(g, p.connected ? nullSafe(p.birdName).toUpperCase(Locale.ROOT) : "OPEN", w - 24),
                x + w / 2, y + 68);

        double scoreY = y + h * 0.46;
        g.setFont(Font.font("Impact", FontWeight.BOLD, scaledFont(w, 70, 42)));
        g.setFill(Color.web("#FFFFFF"));
        g.fillText(Integer.toString(p.score), x + w / 2, scoreY);

        g.setFont(Font.font("Consolas", FontWeight.BOLD, 15));
        g.setFill(Color.web("#B0BEC5"));
        g.fillText(s.smashRules ? "STOCKS" : "SCORE", x + w / 2, scoreY + 24);

        double meterX = x + 16;
        double meterW = w - 32;
        double meterY = y + h - 72;
        if (s.phase == PHASE_LOBBY || s.phase == PHASE_COUNTDOWN) {
            g.setFont(Font.font("Consolas", FontWeight.BOLD, 16));
            g.setFill(p.ready ? Color.web("#80CBC4") : Color.web("#FFCC80"));
            g.fillText(p.connected ? (p.ready ? "READY" : "NOT READY") : "WAITING", x + w / 2, meterY + 14);
        } else if (s.smashRules) {
            drawMeter(g, meterX, meterY, meterW, 14, Math.min(1.0, Math.max(0.0, p.damage / 300.0)),
                    Color.web("#263238"), damageColor(p.damage));
            g.setFont(Font.font("Consolas", FontWeight.BOLD, 16));
            g.setFill(Color.web("#FFE0B2"));
            g.fillText(Math.round(p.damage) + "%", x + w / 2, meterY + 42);
        } else {
            drawMeter(g, meterX, meterY, meterW, 14, Math.min(1.0, Math.max(0.0, p.health / 120.0)),
                    Color.web("#263238"), Color.web("#66BB6A"));
            g.setFont(Font.font("Consolas", FontWeight.BOLD, 16));
            g.setFill(Color.web("#C8E6C9"));
            g.fillText(Math.round(p.health) + " HP", x + w / 2, meterY + 42);
        }
    }

    private static String playerStateText(Snapshot s, Player p) {
        if (!p.connected) return "OPEN";
        if (s.phase == PHASE_LOBBY || s.phase == PHASE_COUNTDOWN) return p.ready ? "READY" : "PICKING";
        return p.alive ? "IN" : "OUT";
    }

    private static Color playerStateColor(Snapshot s, Player p) {
        if (!p.connected) return Color.web("#78909C");
        if (s.phase == PHASE_LOBBY || s.phase == PHASE_COUNTDOWN) {
            return p.ready ? Color.web("#80CBC4") : Color.web("#FFCC80");
        }
        return p.alive ? Color.web("#80CBC4") : Color.web("#EF9A9A");
    }

    private void drawFeed(GraphicsContext g, Snapshot s, double x, double y, double w, double h) {
        drawPanel(g, x, y, w, h, Color.color(0.03, 0.06, 0.07, 0.74), Color.web("#455A64"));
        g.setTextAlign(TextAlignment.LEFT);
        g.setFont(Font.font("Arial Black", FontWeight.BOLD, 20));
        g.setFill(Color.web("#FFE082"));
        g.fillText("MATCH FEED", x + 16, y + 32);

        g.setFont(Font.font("Consolas", FontWeight.BOLD, 16));
        double lineY = y + 64;
        if (s.killFeed.isEmpty()) {
            g.setFill(Color.web("#90A4AE"));
            g.fillText("Waiting for events", x + 16, lineY);
            return;
        }
        int max = Math.min(s.killFeed.size(), Math.max(1, (int) ((h - 70) / 34)));
        for (int i = 0; i < max; i++) {
            String line = s.killFeed.get(i);
            g.setFill(i == 0 ? Color.web("#FFFFFF") : Color.web("#CFD8DC"));
            g.fillText(truncate(g, line, w - 32), x + 16, lineY + i * 34);
        }
    }

    private void drawCenterEvent(GraphicsContext g, double w, double h, Snapshot s) {
        if (s.phase == PHASE_COUNTDOWN && s.countdownSeconds > 0) {
            g.setTextAlign(TextAlignment.CENTER);
            g.setFont(Font.font("Impact", FontWeight.BOLD, Math.min(180, h * 0.22)));
            g.setFill(Color.color(1.0, 0.84, 0.22, 0.22));
            g.fillText(Integer.toString(s.countdownSeconds), w / 2, h * 0.58);
        } else if (s.suddenDeathActive) {
            g.setTextAlign(TextAlignment.CENTER);
            g.setFont(Font.font("Arial Black", FontWeight.BOLD, Math.min(70, w * 0.055)));
            g.setFill(Color.color(1.0, 0.22, 0.22, 0.18));
            g.fillText("SUDDEN DEATH", w / 2, h * 0.92);
        }
    }

    private void drawToasts(GraphicsContext g, double w, double h) {
        long now = System.currentTimeMillis();
        double y = 108;
        toasts.removeIf(toast -> now - toast.startedAtMs > toast.durationMs);
        for (EventToast toast : toasts) {
            double age = now - toast.startedAtMs;
            double fadeIn = Math.min(1.0, age / 180.0);
            double fadeOut = Math.min(1.0, (toast.durationMs - age) / 420.0);
            double alpha = Math.max(0.0, Math.min(fadeIn, fadeOut));
            double boxW = Math.min(w - 60, 820);
            double boxH = 56;
            double x = (w - boxW) / 2.0;
            drawPanel(g, x, y, boxW, boxH, Color.color(0.04, 0.04, 0.04, 0.72 * alpha), toast.color.deriveColor(0, 1, 1, alpha));
            g.setTextAlign(TextAlignment.CENTER);
            g.setFont(Font.font("Arial Black", FontWeight.BOLD, 24));
            g.setFill(toast.color.deriveColor(0, 1, 1.15, alpha));
            g.fillText(truncate(g, toast.text, boxW - 34), w / 2, y + 36);
            y += boxH + 8;
        }
    }

    private static void drawPanel(GraphicsContext g, double x, double y, double w, double h, Color fill, Color stroke) {
        g.setFill(fill);
        g.fillRoundRect(x, y, w, h, 8, 8);
        g.setStroke(stroke.deriveColor(0, 1, 1, 0.85));
        g.setLineWidth(2);
        g.strokeRoundRect(x + 0.5, y + 0.5, w - 1, h - 1, 8, 8);
    }

    private static void drawMeter(GraphicsContext g, double x, double y, double w, double h, double ratio, Color bg, Color fg) {
        g.setFill(bg);
        g.fillRoundRect(x, y, w, h, 6, 6);
        g.setFill(fg);
        g.fillRoundRect(x, y, Math.max(0, w * ratio), h, 6, 6);
    }

    private String truncate(GraphicsContext g, String text, double maxWidth) {
        String value = nullSafe(text);
        if (maxWidth <= 20) return "";
        if (g.getFont() == null) return value;
        if (measureText(value, g.getFont()) <= maxWidth) return value;
        String suffix = "...";
        int end = value.length();
        while (end > 0 && measureText(value.substring(0, end) + suffix, g.getFont()) > maxWidth) {
            end--;
        }
        return end <= 0 ? suffix : value.substring(0, end) + suffix;
    }

    private static double measureText(String text, Font font) {
        javafx.scene.text.Text helper = new javafx.scene.text.Text(text);
        helper.setFont(font);
        return helper.getLayoutBounds().getWidth();
    }

    private static int scaledFont(double width, int normal, int min) {
        return Math.max(min, Math.min(normal, (int) (width / 5.0)));
    }

    private static Color parseColor(String value, Color fallback) {
        try {
            return Color.web(value == null || value.isBlank() ? "#90A4AE" : value);
        } catch (IllegalArgumentException e) {
            return fallback;
        }
    }

    private static Color damageColor(double damage) {
        if (damage >= 240) return Color.web("#EF5350");
        if (damage >= 150) return Color.web("#FFAB40");
        if (damage >= 80) return Color.web("#FFD54F");
        return Color.web("#80CBC4");
    }

    private static String phaseName(int phase) {
        return switch (phase) {
            case PHASE_COUNTDOWN -> "Countdown";
            case PHASE_MATCH -> "Match live";
            case PHASE_RESULTS -> "Results";
            default -> "LAN lobby";
        };
    }

    private static String formatTimer(int frames) {
        int totalSeconds = Math.max(0, (int) Math.ceil(frames / 60.0));
        int minutes = totalSeconds / 60;
        int seconds = totalSeconds % 60;
        return String.format(Locale.ROOT, "%d:%02d", minutes, seconds);
    }

    private static String nullSafe(String value) {
        return value == null ? "" : value;
    }

    private static final class Snapshot {
        final long generatedAtMillis;
        final int phase;
        final String status;
        final String mapName;
        final int matchTimerFrames;
        final boolean matchEnded;
        final boolean suddenDeathActive;
        final boolean suddenDeathSmashStyle;
        final boolean smashRules;
        final int countdownSeconds;
        final int winnerIndex;
        final Player[] players;
        final List<String> killFeed;

        Snapshot(long generatedAtMillis, int phase, String status, String mapName, int matchTimerFrames,
                 boolean matchEnded, boolean suddenDeathActive, boolean suddenDeathSmashStyle,
                 boolean smashRules, int countdownSeconds, int winnerIndex,
                 Player[] players, List<String> killFeed) {
            this.generatedAtMillis = generatedAtMillis;
            this.phase = phase;
            this.status = status;
            this.mapName = mapName;
            this.matchTimerFrames = matchTimerFrames;
            this.matchEnded = matchEnded;
            this.suddenDeathActive = suddenDeathActive;
            this.suddenDeathSmashStyle = suddenDeathSmashStyle;
            this.smashRules = smashRules;
            this.countdownSeconds = countdownSeconds;
            this.winnerIndex = winnerIndex;
            this.players = players;
            this.killFeed = killFeed;
        }

        static Snapshot empty() {
            Player[] players = new Player[]{Player.empty(), Player.empty(), Player.empty(), Player.empty()};
            return new Snapshot(System.currentTimeMillis(), PHASE_LOBBY, "Waiting for host", "",
                    0, false, false, false, true, 0, -1, players, List.of());
        }

        static Snapshot read(byte[] payload) throws IOException {
            DataInputStream in = new DataInputStream(new ByteArrayInputStream(payload));
            byte type = in.readByte();
            if (type != MSG_COMPANION_SNAPSHOT) {
                throw new IOException("Unexpected message: " + type);
            }
            int version = in.readInt();
            if (version != COMPANION_VERSION) {
                throw new IOException("Companion protocol mismatch.");
            }

            long generatedAt = in.readLong();
            int phase = in.readInt();
            String status = in.readUTF();
            String mapName = in.readUTF();
            int timer = in.readInt();
            boolean matchEnded = in.readBoolean();
            boolean suddenDeath = in.readBoolean();
            boolean suddenDeathSmash = in.readBoolean();
            boolean smashRules = in.readBoolean();
            int countdown = in.readInt();
            int winner = in.readInt();
            Player[] players = new Player[]{Player.empty(), Player.empty(), Player.empty(), Player.empty()};
            int playerCount = in.readInt();
            for (int i = 0; i < playerCount; i++) {
                Player player = Player.read(in);
                if (i < players.length) {
                    players[i] = player;
                }
            }
            int feedCount = Math.max(0, Math.min(24, in.readInt()));
            List<String> feed = new ArrayList<>(feedCount);
            for (int i = 0; i < feedCount; i++) {
                feed.add(in.readUTF());
            }
            return new Snapshot(generatedAt, phase, status, mapName, timer, matchEnded, suddenDeath,
                    suddenDeathSmash, smashRules, countdown, winner, players, feed);
        }
    }

    private static final class Player {
        final boolean connected;
        final boolean ready;
        final String birdName;
        final String colorHex;
        final int score;
        final double health;
        final double damage;
        final boolean alive;

        Player(boolean connected, boolean ready, String birdName, String colorHex,
               int score, double health, double damage, boolean alive) {
            this.connected = connected;
            this.ready = ready;
            this.birdName = birdName;
            this.colorHex = colorHex;
            this.score = score;
            this.health = health;
            this.damage = damage;
            this.alive = alive;
        }

        static Player empty() {
            return new Player(false, false, "", "#90A4AE", 0, 0, 0, false);
        }

        static Player read(DataInputStream in) throws IOException {
            boolean connected = in.readBoolean();
            boolean ready = in.readBoolean();
            String birdName = in.readUTF();
            String colorHex = in.readUTF();
            int score = in.readInt();
            double health = in.readDouble();
            double damage = in.readDouble();
            boolean alive = in.readBoolean();
            return new Player(connected, ready, birdName, colorHex, score, health, damage, alive);
        }
    }

    private record EventToast(String text, Color color, long startedAtMs, long durationMs) {
    }
}
