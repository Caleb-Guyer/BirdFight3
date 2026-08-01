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
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.TextAlignment;
import javafx.stage.Stage;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/** Campaign credits with a rotating vector-art flock scrapbook. */
final class StillSkyCreditsPlayer {
    private static final double WIDTH = 1920.0;
    private static final double HEIGHT = 1080.0;
    private static final double DURATION_SECONDS = 76.0;
    private final BirdGame3 game;
    private final Map<BirdGame3.BirdType, List<BirdGame3.VisualAuditSkin>> skins =
            new EnumMap<>(BirdGame3.BirdType.class);
    private AnimationTimer timer;
    private Canvas canvas;
    private long startNanos;
    private Runnable onFinished;

    StillSkyCreditsPlayer(BirdGame3 game) {
        this.game = game;
        for (BirdGame3.VisualAuditSkin skin : game.visualAuditSkins()) {
            skins.computeIfAbsent(skin.bird(), ignored -> new ArrayList<>()).add(skin);
        }
    }

    void play(Stage stage, Runnable onFinished) {
        stop();
        this.onFinished = onFinished;
        this.startNanos = 0L;
        canvas = new Canvas(1600, 900);
        canvas.setScaleX(WIDTH / canvas.getWidth());
        canvas.setScaleY(HEIGHT / canvas.getHeight());

        StackPane content = new StackPane(canvas);
        content.setMinSize(WIDTH, HEIGHT);
        content.setPrefSize(WIDTH, HEIGHT);
        content.setMaxSize(WIDTH, HEIGHT);
        content.setStyle("-fx-background-color: #050711;");
        Button skip = button("CONTINUE TO POST-CREDITS", this::finish);
        HBox controls = new HBox(skip);
        controls.setAlignment(Pos.CENTER);
        controls.setPadding(new Insets(10));
        controls.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
        controls.setStyle("-fx-background-color: rgba(4,7,15,0.88); -fx-border-color: #80DEEA;"
                + "-fx-border-radius: 8; -fx-background-radius: 8;");
        StackPane.setAlignment(controls, Pos.TOP_RIGHT);
        StackPane.setMargin(controls, new Insets(24, 28, 0, 0));
        content.getChildren().add(controls);

        StackPane root = new StackPane(content);
        root.getProperties().put("noAutoScale", true);
        Scene scene = new Scene(root, WIDTH, HEIGHT, Color.web("#050711"));
        game.prepareCampaignCutsceneScene(scene, root, content);
        scene.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            if (event.getCode() == KeyCode.ESCAPE || event.getCode() == KeyCode.ENTER) {
                finish();
                event.consume();
            }
        });
        game.setCampaignScene(stage, scene);
        game.startCampaignSequenceMusic("music-credits.mp3", false);

        timer = new AnimationTimer() {
            @Override public void handle(long now) {
                if (startNanos == 0L) startNanos = now;
                double seconds = (now - startNanos) / 1_000_000_000.0;
                render(seconds);
                if (seconds >= DURATION_SECONDS) finish();
            }
        };
        timer.start();
    }

    private Button button(String text, Runnable action) {
        Button button = new Button(text);
        button.setFont(Font.font("Arial Black", FontWeight.BOLD, 14));
        button.setTextFill(Color.WHITE);
        button.setStyle("-fx-background-color: #10202D; -fx-border-color: #80DEEA;"
                + "-fx-border-radius: 6; -fx-background-radius: 6;");
        button.setOnAction(event -> action.run());
        return button;
    }

    private void render(double seconds) {
        GraphicsContext g = canvas.getGraphicsContext2D();
        g.save();
        g.scale(canvas.getWidth() / WIDTH, canvas.getHeight() / HEIGHT);
        g.setFill(Color.web("#050711"));
        g.fillRect(0, 0, WIDTH, HEIGHT);
        drawWind(g, seconds);
        drawCredits(g, seconds);
        drawScrapbook(g, seconds);
        if (seconds > 64.0) drawThankYou(g, seconds);
        g.restore();
    }

    private void drawWind(GraphicsContext g, double seconds) {
        g.setStroke(Color.web("#80DEEA", 0.08));
        g.setLineWidth(3);
        for (int i = 0; i < 18; i++) {
            double y = 70 + i * 61.0;
            double offset = (seconds * (45 + i * 2.0) + i * 173.0) % (WIDTH + 500) - 250;
            g.strokeArc(offset, y, 360 + (i % 4) * 90, 70, 10, 150, javafx.scene.shape.ArcType.OPEN);
        }
    }

    private void drawCredits(GraphicsContext g, double seconds) {
        String[] lines = {
                "BIRD FIGHT 3", "THE STILL SKY", "", "Created by Caleb", "",
                "STORY", "The Still Sky campaign team", "Every playtester who reported the weird stuff", "",
                "ENGINEERING", "JavaFX • deterministic 60 Hz simulation", "Code-drawn birds, arenas, cutscenes, and effects", "",
                "THE COALITION", "Pigeon • Eagle • Falcon • Phoenix • Hummingbird", "Turkey • Rooster • Roadrunner • Penguin • Shoebill",
                "Charles • Razorbill • Grinch-Hawk • Vulture • Opium Bird", "Titmouse • Bat • Pelican • Heisenbird • Raven • Goose", "",
                "MUSIC", "Public-domain and CC0 artists credited in CREDITS-AUDIO.md", "",
                "SPECIAL THANKS", "To everyone who kept flying after the first bad landing", "",
                "NO RULER OWNS THE WIND"
        };
        double y = HEIGHT + 180 - seconds * 48.0;
        g.setTextAlign(TextAlignment.CENTER);
        for (String line : lines) {
            boolean heading = line.equals(line.toUpperCase()) && !line.isBlank();
            g.setFont(Font.font(heading ? "Arial Black" : "Arial",
                    heading ? FontWeight.BOLD : FontWeight.NORMAL, heading ? 34 : 25));
            g.setFill(heading ? Color.web("#80DEEA") : Color.web("#E8EDF4"));
            g.fillText(line, WIDTH * 0.5, y);
            y += line.isBlank() ? 34 : heading ? 58 : 42;
        }
        g.setTextAlign(TextAlignment.LEFT);
    }

    private void drawScrapbook(GraphicsContext g, double seconds) {
        BirdGame3.BirdType[] roster = BirdGame3.BirdType.values();
        int panel = Math.min(roster.length - 1, (int) (seconds / 3.0));
        for (int i = 0; i < 3; i++) {
            int index = (panel + i * 7) % roster.length;
            BirdGame3.BirdType type = roster[index];
            double px = i == 0 ? 285 : i == 1 ? 960 : 1635;
            double py = 235 + (i % 2) * 590;
            double angle = (i - 1) * 5.5 + Math.sin(seconds * 0.7 + i) * 2.0;
            List<BirdGame3.VisualAuditSkin> options = skins.getOrDefault(type, List.of());
            BirdGame3.VisualAuditSkin chosen = options.isEmpty()
                    ? null : options.get((panel / Math.max(1, roster.length) + i) % options.size());
            String skinKey = chosen == null ? null : chosen.key();
            String skinName = chosen == null ? "Base" : chosen.name();

            g.save();
            g.translate(px, py);
            g.rotate(angle);
            g.setFill(Color.web(i == 0 ? "#F5E6C8" : i == 1 ? "#D8EDF0" : "#E7D7F2"));
            g.fillRoundRect(-225, -165, 450, 330, 18, 18);
            g.setStroke(Color.web("#3E3342", 0.72));
            g.setLineWidth(5);
            g.strokeRoundRect(-225, -165, 450, 330, 18, 18);
            Bird bird = game.createCampaignCutsceneBird(type, skinKey);
            bird.setBaseMultipliers(2.25, 1.0, 1.0);
            bird.x = -bird.bodyWidth() * 0.5;
            bird.y = -bird.bodyHeight() * 0.56 + Math.sin(seconds * 2.0 + i) * 8.0;
            bird.facingRight = i != 2;
            bird.vx = i == 1 ? 5.0 : 0.0;
            bird.vy = i == 2 ? -4.0 : 0.0;
            bird.draw(g);
            g.setTextAlign(TextAlignment.CENTER);
            g.setFont(Font.font("Arial Black", FontWeight.BOLD, 22));
            g.setFill(Color.web("#211A25"));
            g.fillText(type.name.toUpperCase(), 0, 125);
            g.setFont(Font.font("Consolas", FontWeight.NORMAL, 14));
            g.fillText(skinName.toUpperCase(), 0, 150);
            g.restore();
        }
        g.setTextAlign(TextAlignment.LEFT);
    }

    private void drawThankYou(GraphicsContext g, double seconds) {
        double alpha = Math.clamp((seconds - 64.0) / 4.0, 0.0, 0.94);
        g.setFill(Color.web("#02030A", alpha));
        g.fillRect(0, 0, WIDTH, HEIGHT);
        g.setTextAlign(TextAlignment.CENTER);
        g.setFill(Color.WHITE.deriveColor(0, 1, 1, alpha));
        g.setFont(Font.font("Arial Black", FontWeight.BOLD, 68));
        g.fillText("THANK YOU FOR PLAYING", WIDTH * 0.5, 485);
        g.setFill(Color.web("#80DEEA", alpha));
        g.setFont(Font.font("Georgia", FontWeight.NORMAL, 30));
        g.fillText("The sky is open because you flew through it with us.", WIDTH * 0.5, 550);
        g.setTextAlign(TextAlignment.LEFT);
    }

    private void finish() {
        stop();
        game.finishCampaignSequenceMusic();
        game.resetAfterCampaignCutscene();
        Runnable callback = onFinished;
        onFinished = null;
        if (callback != null) callback.run();
    }

    private void stop() {
        if (timer != null) {
            timer.stop();
            timer = null;
        }
        if (canvas != null) {
            Parent parent = canvas.getParent();
            if (parent instanceof Pane pane) pane.getChildren().remove(canvas);
        }
    }
}
