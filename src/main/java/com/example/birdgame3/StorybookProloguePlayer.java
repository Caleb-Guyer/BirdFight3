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
import javafx.scene.shape.ArcType;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;
import javafx.stage.Stage;

import java.util.ArrayList;
import java.util.List;

/**
 * Presentation-only illustrated-book prologue. This intentionally does not
 * share the dialogue cutscene staging: no combat actors, speech bubbles, or
 * simulation state are involved.
 */
final class StorybookProloguePlayer {
    private static final double LOGICAL_WIDTH = 1920.0;
    private static final double LOGICAL_HEIGHT = 1080.0;
    private static final double BACKING_WIDTH = 1600.0;
    private static final double BACKING_HEIGHT = 900.0;
    private static final double BOOK_X = 120.0;
    private static final double BOOK_Y = 145.0;
    private static final double BOOK_WIDTH = 1680.0;
    private static final double BOOK_HEIGHT = 815.0;
    private static final double SPINE_X = 960.0;
    private static final long TURN_DURATION_NANOS = 720_000_000L;

    private static final Font SERIES_FONT = Font.font("Consolas", FontWeight.BOLD, 20);
    private static final Font TITLE_FONT = Font.font("Georgia", FontWeight.BOLD, 43);
    private static final Font PROSE_FONT = Font.font("Georgia", FontWeight.NORMAL, 25);
    private static final Font PAGE_FONT = Font.font("Georgia", FontWeight.BOLD, 17);

    private final BirdGame3 game;
    private final StorybookPrologue prologue;
    private AnimationTimer timer;
    private Canvas canvas;
    private Button pauseButton;
    private Button modeButton;
    private int pageIndex;
    private int targetPageIndex;
    private boolean turning;
    private boolean paused;
    private boolean manual;
    private boolean finished;
    private long pageStartNanos;
    private long turnStartNanos;
    private long pausedAtNanos;
    private long accumulatedPauseNanos;
    private Runnable onFinished;

    StorybookProloguePlayer(BirdGame3 game) {
        this.game = game;
        this.prologue = StorybookPrologue.loadBundled();
    }

    void play(Stage stage, Runnable onFinished) {
        stopTimer();
        game.resetAfterCampaignCutscene();
        this.pageIndex = 0;
        this.targetPageIndex = 0;
        this.turning = false;
        this.paused = false;
        this.manual = false;
        this.finished = false;
        this.pageStartNanos = 0L;
        this.turnStartNanos = 0L;
        this.pausedAtNanos = 0L;
        this.accumulatedPauseNanos = 0L;
        this.onFinished = onFinished;

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
        content.setStyle("-fx-background-color: #090A10;");

        Button previous = controlButton("PREVIOUS PAGE", () -> requestPage(-1));
        pauseButton = controlButton("PAUSE", this::togglePause);
        modeButton = controlButton("PAGES: AUTO", () -> {
            manual = !manual;
            refreshModeButton();
        });
        Button next = controlButton("NEXT PAGE", () -> requestPage(1));
        Button skip = controlButton("SKIP PROLOGUE", this::finish);
        HBox controls = new HBox(10, previous, pauseButton, modeButton, next, skip);
        controls.setAlignment(Pos.CENTER);
        controls.setPadding(new Insets(8, 14, 8, 14));
        controls.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
        controls.setStyle("-fx-background-color: rgba(20,16,13,0.92);"
                + "-fx-border-color: #A98B58; -fx-border-width: 1.5;"
                + "-fx-background-radius: 8; -fx-border-radius: 8;");
        StackPane.setAlignment(controls, Pos.TOP_CENTER);
        StackPane.setMargin(controls, new Insets(22, 0, 0, 0));
        content.getChildren().add(controls);

        StackPane root = new StackPane(content);
        root.getProperties().put("noAutoScale", true);
        root.setStyle("-fx-background-color: #090A10;");
        Scene scene = new Scene(root, LOGICAL_WIDTH, LOGICAL_HEIGHT, Color.web("#090A10"));
        game.prepareCampaignCutsceneScene(scene, root, content);
        scene.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            if (event.getCode() == KeyCode.SPACE || event.getCode() == KeyCode.ENTER
                    || event.getCode() == KeyCode.RIGHT) {
                requestPage(1);
            } else if (event.getCode() == KeyCode.LEFT || event.getCode() == KeyCode.BACK_SPACE) {
                requestPage(-1);
            } else if (event.getCode() == KeyCode.P) {
                togglePause();
            } else if (event.getCode() == KeyCode.M) {
                manual = !manual;
                refreshModeButton();
            } else if (event.getCode() == KeyCode.ESCAPE) {
                finish();
            } else {
                return;
            }
            event.consume();
        });
        canvas.setOnMouseClicked(event -> requestPage(1));
        game.setCampaignScene(stage, scene);
        game.startCampaignProloguePresentation();

        timer = new AnimationTimer() {
            @Override
            public void handle(long now) {
                if (pageStartNanos == 0L) pageStartNanos = now;
                long presentationNow = paused && pausedAtNanos > 0L ? pausedAtNanos : now;
                if (turning && turnProgress(turnStartNanos, presentationNow) >= 1.0) {
                    completeTurn(presentationNow);
                }
                render(presentationNow);
                if (!paused && !manual && !turning
                        && elapsedSeconds(now) >= automaticDuration(prologue.pages.get(pageIndex).prose())) {
                    requestPage(1);
                }
            }
        };
        timer.start();
        canvas.requestFocus();
    }

    private Button controlButton(String text, Runnable action) {
        Button button = new Button(text);
        button.setFont(Font.font("Arial Black", FontWeight.BOLD, 13));
        button.setTextFill(Color.web("#F7E8C3"));
        button.setStyle("-fx-background-color: rgba(48,37,28,0.96);"
                + "-fx-border-color: #C5A66D; -fx-border-width: 1.3;"
                + "-fx-background-radius: 6; -fx-border-radius: 6;");
        button.setOnAction(event -> action.run());
        return button;
    }

    private void render(long now) {
        GraphicsContext g = canvas.getGraphicsContext2D();
        double width = canvas.getWidth();
        double height = canvas.getHeight();
        g.save();
        g.scale(width / LOGICAL_WIDTH, height / LOGICAL_HEIGHT);
        drawDesk(g, now);
        if (turning) {
            double progress = turnProgress(turnStartNanos, now);
            drawSpread(g, prologue.pages.get(targetPageIndex), targetPageIndex, now);
            drawTurningOldPage(g, prologue.pages.get(pageIndex), pageIndex, now, progress);
        } else {
            drawSpread(g, prologue.pages.get(pageIndex), pageIndex, now);
        }
        drawPrologueHeader(g);
        g.restore();
    }

    private void drawDesk(GraphicsContext g, long now) {
        g.setFill(new RadialGradient(0, 0, 960, 520, 1050, false, CycleMethod.NO_CYCLE,
                new Stop(0, Color.web("#29251F")), new Stop(0.56, Color.web("#15151A")),
                new Stop(1, Color.web("#08090E"))));
        g.fillRect(0, 0, LOGICAL_WIDTH, LOGICAL_HEIGHT);
        g.setStroke(Color.rgb(255, 225, 170, 0.045));
        g.setLineWidth(2);
        for (int i = 0; i < 13; i++) {
            double y = 90 + i * 82;
            g.strokeLine(0, y + Math.sin(now / 1_900_000_000.0 + i) * 2, LOGICAL_WIDTH, y);
        }
        g.setFill(Color.rgb(0, 0, 0, 0.55));
        g.fillOval(80, 885, 1760, 130);
    }

    private void drawSpread(GraphicsContext g, StorybookPrologue.Page page, int index, long now) {
        drawBookBase(g);
        drawIllustrationPage(g, page, now);
        drawTextPage(g, page, index);
        drawSpine(g);
    }

    private void drawBookBase(GraphicsContext g) {
        g.setFill(Color.web("#38261B"));
        g.fillRoundRect(BOOK_X - 25, BOOK_Y + 20, BOOK_WIDTH + 50, BOOK_HEIGHT + 28, 42, 42);
        g.setFill(new LinearGradient(0, 0, 1, 0, true, CycleMethod.NO_CYCLE,
                new Stop(0, Color.web("#DCC89B")), new Stop(0.45, Color.web("#F1E4BE")),
                new Stop(0.52, Color.web("#C9B486")), new Stop(0.58, Color.web("#EFE0B6")),
                new Stop(1, Color.web("#D3BD8D"))));
        g.fillRoundRect(BOOK_X, BOOK_Y, BOOK_WIDTH, BOOK_HEIGHT, 30, 30);
        g.setStroke(Color.web("#8B7048"));
        g.setLineWidth(3);
        g.strokeRoundRect(BOOK_X, BOOK_Y, BOOK_WIDTH, BOOK_HEIGHT, 30, 30);
        g.setStroke(Color.rgb(101, 75, 43, 0.18));
        g.setLineWidth(1);
        for (int i = 0; i < 9; i++) {
            double inset = 12 + i * 2.3;
            g.strokeRoundRect(BOOK_X + inset, BOOK_Y + inset * 0.25,
                    BOOK_WIDTH - inset * 2, BOOK_HEIGHT - inset * 0.5, 24, 24);
        }
    }

    private void drawSpine(GraphicsContext g) {
        g.setFill(new LinearGradient(0, 0, 1, 0, true, CycleMethod.NO_CYCLE,
                new Stop(0, Color.rgb(91, 67, 39, 0.05)), new Stop(0.48, Color.rgb(60, 41, 25, 0.35)),
                new Stop(0.52, Color.rgb(255, 246, 211, 0.28)), new Stop(1, Color.rgb(91, 67, 39, 0.05))));
        g.fillRect(SPINE_X - 28, BOOK_Y + 12, 56, BOOK_HEIGHT - 24);
        g.setStroke(Color.rgb(87, 62, 35, 0.35));
        g.setLineWidth(2);
        g.strokeLine(SPINE_X, BOOK_Y + 18, SPINE_X, BOOK_Y + BOOK_HEIGHT - 18);
    }

    private void drawTextPage(GraphicsContext g, StorybookPrologue.Page page, int index) {
        double x = 1035;
        g.setTextAlign(TextAlignment.LEFT);
        g.setFill(Color.web("#705936"));
        g.setFont(SERIES_FONT);
        g.fillText("A HISTORY OF THE OPEN SKY", x, 218);
        g.setStroke(Color.rgb(92, 68, 38, 0.45));
        g.setLineWidth(2);
        g.strokeLine(x, 238, 1720, 238);
        g.setFill(Color.web("#2E2418"));
        g.setFont(TITLE_FONT);
        drawWrappedText(g, page.title(), x, 302, 680, 51);

        g.setFill(Color.web("#876A3F"));
        g.setFont(Font.font("Consolas", FontWeight.BOLD, 17));
        g.fillText("NARRATOR", x, 365);
        g.setFont(PROSE_FONT);
        g.setFill(Color.web("#352A1E"));
        double y = 407;
        for (String paragraph : page.paragraphs()) {
            y = drawWrappedText(g, paragraph, x, y, 680, 34) + 20;
        }
        g.setFont(PAGE_FONT);
        g.setFill(Color.web("#75603F"));
        g.setTextAlign(TextAlignment.RIGHT);
        g.fillText((index + 1) + "  /  " + prologue.pages.size(), 1715, 902);
        g.setTextAlign(TextAlignment.LEFT);
    }

    private double drawWrappedText(GraphicsContext g, String text, double x, double y,
                                   double maxWidth, double lineHeight) {
        List<String> lines = wrap(text, g.getFont(), maxWidth);
        double cursorY = y;
        for (String line : lines) {
            g.fillText(line, x, cursorY);
            cursorY += lineHeight;
        }
        return cursorY;
    }

    static List<String> wrap(String text, Font font, double maxWidth) {
        List<String> lines = new ArrayList<>();
        StringBuilder line = new StringBuilder();
        for (String word : (text == null ? "" : text).split("\\s+")) {
            if (word.isBlank()) continue;
            String candidate = line.isEmpty() ? word : line + " " + word;
            Text measure = new Text(candidate);
            measure.setFont(font);
            if (measure.getLayoutBounds().getWidth() > maxWidth && !line.isEmpty()) {
                lines.add(line.toString());
                line.setLength(0);
                line.append(word);
            } else {
                if (!line.isEmpty()) line.append(' ');
                line.append(word);
            }
        }
        if (!line.isEmpty()) lines.add(line.toString());
        return lines;
    }

    private void drawIllustrationPage(GraphicsContext g, StorybookPrologue.Page page, long now) {
        g.save();
        g.beginPath();
        g.rect(165, 185, 725, 700);
        g.closePath();
        g.clip();
        g.setFill(new LinearGradient(0, 0, 0, 1, true, CycleMethod.NO_CYCLE,
                new Stop(0, Color.web("#263D52")), new Stop(1, Color.web("#A67A51"))));
        g.fillRoundRect(165, 185, 725, 700, 20, 20);
        switch (page.illustration()) {
            case FIRST_SKY -> drawFirstSky(g, now);
            case ELEVEN_LANDS -> drawElevenLands(g);
            case FIRST_FIGHT -> drawFirstFight(g, false);
            case OPEN_WING -> drawFirstFight(g, true);
            case AGE_OF_WINGS -> drawAgeOfWings(g);
            case BLACK_MIGRATIONS -> drawBlackMigrations(g);
            case PERFECT_WEATHER -> drawPerfectWeather(g);
            case BARGAINS -> drawBargains(g);
            case WATCHED_FIGHTS -> drawWatchedFight(g);
            case STILLNESS -> drawStillness(g, now);
        }
        g.restore();
        g.setStroke(Color.web("#7F6540"));
        g.setLineWidth(4);
        g.strokeRoundRect(165, 185, 725, 700, 20, 20);
    }

    private void drawFirstSky(GraphicsContext g, long now) {
        g.setFill(Color.web("#87C9DA"));
        g.fillRect(165, 185, 725, 700);
        g.setFill(Color.rgb(255, 244, 203, 0.75));
        g.fillOval(245, 250, 165, 165);
        for (int i = 0; i < 5; i++) {
            double y = 350 + i * 72 + Math.sin(now / 1_300_000_000.0 + i) * 7;
            g.setStroke(Color.rgb(247, 250, 227, 0.72 - i * 0.08));
            g.setLineWidth(13 - i);
            g.strokeArc(210, y, 610, 150, 8 + i * 8, 145 - i * 10, ArcType.OPEN);
        }
        g.setFill(Color.web("#426A48"));
        g.fillPolygon(new double[]{165, 320, 465, 615, 890}, new double[]{780, 580, 760, 520, 790}, 5);
        drawFlock(g, 310, 420, 1.0, Color.web("#372C27"));
        drawFlock(g, 625, 360, 0.8, Color.web("#372C27"));
    }

    private void drawElevenLands(GraphicsContext g) {
        g.setFill(Color.web("#D6BE83"));
        g.fillRect(165, 185, 725, 700);
        g.setStroke(Color.web("#6C5537"));
        g.setLineWidth(5);
        g.strokeLine(260, 310, 780, 710);
        g.strokeLine(250, 730, 760, 300);
        g.strokeLine(360, 235, 680, 825);
        Color[] colors = {Color.web("#355E3B"), Color.web("#A36A3E"), Color.web("#6D8194"),
                Color.web("#D7E8EA"), Color.web("#673E2E"), Color.web("#697CAA")};
        for (int i = 0; i < 11; i++) {
            double angle = Math.PI * 2 * i / 11.0 - Math.PI / 2;
            double radius = i == 10 ? 0 : 230;
            double x = 527 + Math.cos(angle) * radius;
            double y = 520 + Math.sin(angle) * radius;
            g.setFill(colors[i % colors.length]);
            g.fillOval(x - 34, y - 34, 68, 68);
            g.setStroke(Color.web("#F1E2B4"));
            g.setLineWidth(3);
            g.strokeOval(x - 34, y - 34, 68, 68);
        }
        g.setFill(Color.web("#F2D36C"));
        g.fillPolygon(new double[]{527, 548, 566, 527, 488, 506},
                new double[]{465, 501, 543, 528, 543, 501}, 6);
    }

    private void drawFirstFight(GraphicsContext g, boolean accord) {
        g.setFill(Color.web("#C89864"));
        g.fillRect(165, 185, 725, 700);
        g.setFill(Color.web("#68513F"));
        g.fillRect(215, 670, 625, 70);
        g.setStroke(Color.web("#E4D09B"));
        g.setLineWidth(5);
        g.strokeOval(320, 340, 420, 300);
        drawBookBird(g, 405, 530, 1, Color.web("#3F6076"), accord ? 0 : -22);
        drawBookBird(g, 655, 530, -1, Color.web("#8A3F35"), accord ? 0 : 22);
        if (accord) {
            g.setFill(Color.rgb(242, 220, 157, 0.92));
            g.fillRoundRect(342, 255, 375, 92, 16, 16);
            g.setStroke(Color.web("#6E5331"));
            g.setLineWidth(3);
            g.strokeRoundRect(342, 255, 375, 92, 16, 16);
            g.strokeLine(525, 270, 525, 330);
            g.strokeLine(390, 300, 660, 300);
        } else {
            g.setStroke(Color.rgb(66, 40, 31, 0.7));
            g.setLineWidth(7);
            g.strokeLine(500, 440, 555, 390);
            g.strokeLine(515, 390, 560, 445);
        }
    }

    private void drawAgeOfWings(GraphicsContext g) {
        g.setFill(Color.web("#A8D2D7"));
        g.fillRect(165, 185, 725, 700);
        g.setFill(Color.web("#405A68"));
        for (int i = 0; i < 7; i++) g.fillRect(195 + i * 102, 520 - (i % 3) * 75, 76, 270);
        g.setFill(Color.web("#876344"));
        g.fillRect(210, 680, 620, 48);
        g.setFill(Color.web("#4C8C4A"));
        g.fillOval(225, 360, 210, 180);
        g.fillOval(650, 315, 180, 225);
        drawFlock(g, 330, 300, 1.0, Color.web("#47352B"));
        drawFlock(g, 600, 400, 0.75, Color.web("#47352B"));
        g.setStroke(Color.web("#F0D56D"));
        g.setLineWidth(6);
        g.strokeArc(250, 240, 520, 310, 20, 135, ArcType.OPEN);
    }

    private void drawBlackMigrations(GraphicsContext g) {
        g.setFill(Color.web("#29303B"));
        g.fillRect(165, 185, 725, 700);
        g.setFill(Color.web("#171820"));
        g.fillPolygon(new double[]{165, 330, 470, 610, 890}, new double[]{760, 480, 770, 410, 770}, 5);
        g.setStroke(Color.web("#DDEAF0"));
        g.setLineWidth(5);
        for (int i = 0; i < 18; i++) {
            double x = 190 + i * 44;
            g.strokeLine(x, 230 + (i % 4) * 34, x - 70, 390 + (i % 5) * 67);
        }
        g.setStroke(Color.web("#E7B14B"));
        g.setLineWidth(11);
        g.strokeArc(230, 250, 530, 470, 205, 115, ArcType.OPEN);
        drawBookBird(g, 505, 470, 1, Color.web("#75624C"), -15);
        g.setFill(Color.rgb(225, 231, 230, 0.32));
        g.fillOval(300, 650, 390, 90);
    }

    private void drawPerfectWeather(GraphicsContext g) {
        g.setFill(Color.web("#203A50"));
        g.fillRect(165, 185, 725, 700);
        g.setStroke(Color.web("#78D7E6"));
        g.setLineWidth(6);
        for (int i = 0; i < 5; i++) {
            double x = 245 + i * 132;
            g.strokeLine(x, 690, x, 360 - (i % 2) * 75);
            g.strokeOval(x - 25, 320 - (i % 2) * 75, 50, 50);
        }
        g.setStroke(Color.rgb(130, 225, 240, 0.55));
        g.setLineWidth(8);
        g.strokeArc(220, 250, 590, 350, 14, 152, ArcType.OPEN);
        g.setFill(Color.web("#E4C968"));
        g.fillPolygon(new double[]{525, 572, 555, 525, 495, 478},
                new double[]{235, 310, 385, 345, 385, 310}, 6);
        drawBookBird(g, 355, 695, 1, Color.web("#E8ECEA"), 0);
        drawBookBird(g, 690, 695, -1, Color.web("#705346"), 0);
    }

    private void drawBargains(GraphicsContext g) {
        g.setFill(Color.web("#171521"));
        g.fillRect(165, 185, 725, 700);
        g.setFill(Color.web("#B89459"));
        g.fillOval(390, 320, 270, 270);
        g.setFill(Color.web("#171521"));
        g.fillOval(420, 350, 210, 210);
        Color[] silhouettes = {Color.web("#352D3C"), Color.web("#252832"), Color.web("#443029"), Color.web("#29333B")};
        double[][] points = {{310, 330}, {710, 340}, {315, 680}, {715, 680}};
        for (int i = 0; i < points.length; i++) {
            drawBookBird(g, points[i][0], points[i][1], i % 2 == 0 ? 1 : -1, silhouettes[i], 0);
            g.setStroke(Color.rgb(202, 163, 91, 0.55));
            g.setLineWidth(3);
            g.strokeLine(points[i][0], points[i][1], 525, 455);
        }
        g.setFill(Color.web("#C29B55"));
        g.fillRect(470, 435, 110, 90);
        g.setStroke(Color.web("#EEE0B2"));
        g.strokeLine(488, 463, 562, 463);
        g.strokeLine(488, 482, 550, 482);
    }

    private void drawWatchedFight(GraphicsContext g) {
        g.setFill(Color.web("#242B3C"));
        g.fillRect(165, 185, 725, 700);
        g.setStroke(Color.web("#765C43"));
        g.setLineWidth(16);
        g.strokeLine(240, 710, 815, 710);
        drawBookBird(g, 410, 610, 1, Color.web("#6D8395"), -12);
        drawBookBird(g, 650, 610, -1, Color.web("#76543E"), 12);
        g.setStroke(Color.rgb(104, 218, 235, 0.7));
        g.setLineWidth(4);
        for (int i = 0; i < 4; i++) g.strokeOval(272 + i * 75, 300 + i * 22, 500 - i * 150, 280 - i * 35);
        g.setFill(Color.web("#80DEEA"));
        for (int i = 0; i < 18; i++) {
            double x = 260 + (i * 91) % 540;
            double y = 260 + (i * 67) % 390;
            g.fillOval(x, y, 7, 7);
        }
        g.setFill(Color.web("#070B12"));
        g.fillRoundRect(355, 225, 340, 95, 14, 14);
        g.setStroke(Color.web("#80DEEA"));
        g.strokeLine(385, 275, 665, 275);
    }

    private void drawStillness(GraphicsContext g, long now) {
        g.setFill(Color.web("#0D1624"));
        g.fillRect(165, 185, 725, 700);
        g.setFill(Color.web("#1E2D3C"));
        for (int i = 0; i < 7; i++) g.fillRect(190 + i * 105, 480 - (i % 3) * 65, 80, 320);
        g.setStroke(Color.web("#6D8290"));
        g.setLineWidth(5);
        for (int i = 0; i < 5; i++) {
            double x = 245 + i * 128;
            g.strokeLine(x, 310, x, 680);
            g.setFill(Color.web("#708090"));
            g.fillPolygon(new double[]{x, x + 75, x}, new double[]{320, 344, 365}, 3);
        }
        double pulse = 0.35 + Math.sin(now / 600_000_000.0) * 0.12;
        g.setFill(Color.rgb(105, 220, 236, pulse));
        g.fillOval(455, 500, 160, 160);
        g.setStroke(Color.web("#83E2EF"));
        g.setLineWidth(4);
        g.strokeOval(430, 475, 210, 210);
        drawBookBird(g, 345, 740, 1, Color.web("#697983"), 0);
        g.setFill(Color.web("#F1D46D"));
        g.fillPolygon(new double[]{770, 815, 795, 770, 745, 725},
                new double[]{235, 310, 385, 345, 385, 310}, 6);
    }

    private void drawBookBird(GraphicsContext g, double x, double y, int facing, Color body, double tilt) {
        g.save();
        g.translate(x, y);
        g.rotate(tilt);
        g.scale(facing, 1);
        g.setFill(body);
        g.fillOval(-62, -52, 124, 104);
        g.setFill(body.brighter());
        g.fillOval(30, -35, 72, 70);
        g.setFill(Color.web("#F5EBD1"));
        g.fillOval(60, -25, 26, 26);
        g.setFill(Color.web("#2B251E"));
        g.fillOval(69, -17, 10, 10);
        g.setFill(Color.web("#D99A3B"));
        g.fillPolygon(new double[]{96, 135, 96}, new double[]{-10, 5, 20}, 3);
        g.setFill(body.darker());
        g.fillPolygon(new double[]{-50, -105, -64}, new double[]{-12, 5, 28}, 3);
        g.restore();
    }

    private void drawFlock(GraphicsContext g, double x, double y, double scale, Color color) {
        g.setStroke(color);
        g.setLineWidth(4 * scale);
        for (int i = 0; i < 7; i++) {
            double px = x + (i % 4) * 50 * scale;
            double py = y + (i / 4) * 45 * scale + (i % 2) * 12 * scale;
            g.strokeArc(px, py, 26 * scale, 18 * scale, 18, 145, ArcType.OPEN);
            g.strokeArc(px + 23 * scale, py, 26 * scale, 18 * scale, 18, 145, ArcType.OPEN);
        }
    }

    private void drawTurningOldPage(GraphicsContext g, StorybookPrologue.Page oldPage,
                                    int oldIndex, long now, double progress) {
        double foldX = 1778 - progress * (1778 - SPINE_X);
        g.save();
        g.setGlobalAlpha(Math.max(0.0, 1.0 - progress));
        g.beginPath();
        g.rect(BOOK_X, BOOK_Y, SPINE_X - BOOK_X, BOOK_HEIGHT);
        g.closePath();
        g.clip();
        drawSpread(g, oldPage, oldIndex, now);
        g.restore();

        if (foldX > SPINE_X + 1) {
            g.save();
            g.beginPath();
            g.rect(SPINE_X, BOOK_Y, foldX - SPINE_X, BOOK_HEIGHT);
            g.closePath();
            g.clip();
            drawSpread(g, oldPage, oldIndex, now);
            g.restore();
        }
        double curl = Math.sin(progress * Math.PI) * 72;
        g.setFill(new LinearGradient(0, 0, 1, 0, true, CycleMethod.NO_CYCLE,
                new Stop(0, Color.rgb(79, 57, 34, 0.18)), new Stop(0.45, Color.rgb(250, 235, 192, 0.92)),
                new Stop(1, Color.rgb(92, 65, 38, 0.48))));
        g.fillPolygon(new double[]{foldX - curl, foldX, foldX, foldX - curl * 0.35},
                new double[]{BOOK_Y + 18, BOOK_Y + 5, BOOK_Y + BOOK_HEIGHT - 5, BOOK_Y + BOOK_HEIGHT - 18}, 4);
        g.setStroke(Color.rgb(82, 59, 34, 0.5));
        g.setLineWidth(2);
        g.strokeLine(foldX, BOOK_Y + 10, foldX, BOOK_Y + BOOK_HEIGHT - 10);
    }

    private void drawPrologueHeader(GraphicsContext g) {
        g.setTextAlign(TextAlignment.LEFT);
        g.setFill(Color.web("#D6BE86"));
        g.setFont(Font.font("Georgia", FontWeight.BOLD, 23));
        g.fillText("PROLOGUE", 125, 112);
        g.setFill(Color.web("#8C7957"));
        g.setFont(Font.font("Georgia", FontWeight.NORMAL, 18));
        g.fillText("Narrated from the records of the Cave Archive", 270, 111);
        g.setTextAlign(TextAlignment.RIGHT);
        g.fillText("THE STILL SKY", 1795, 111);
        g.setTextAlign(TextAlignment.LEFT);
    }

    private void requestPage(int direction) {
        if (finished || turning || direction == 0) return;
        int next = pageIndex + Integer.signum(direction);
        if (next >= prologue.pages.size()) {
            finish();
            return;
        }
        if (next < 0) return;
        targetPageIndex = next;
        turning = true;
        turnStartNanos = paused && pausedAtNanos > 0L ? pausedAtNanos : System.nanoTime();
        game.playCampaignProloguePageTurnCue();
    }

    private void completeTurn(long now) {
        pageIndex = targetPageIndex;
        turning = false;
        turnStartNanos = 0L;
        pageStartNanos = now;
        accumulatedPauseNanos = 0L;
        pausedAtNanos = paused ? now : 0L;
    }

    private void togglePause() {
        paused = !paused;
        if (pauseButton != null) pauseButton.setText(paused ? "RESUME" : "PAUSE");
        long now = System.nanoTime();
        if (paused) {
            pausedAtNanos = now;
        } else if (pausedAtNanos > 0L) {
            long pausedDuration = now - pausedAtNanos;
            accumulatedPauseNanos += pausedDuration;
            if (turning) turnStartNanos += pausedDuration;
            pausedAtNanos = 0L;
        }
    }

    private void refreshModeButton() {
        if (modeButton != null) modeButton.setText(manual ? "PAGES: MANUAL" : "PAGES: AUTO");
    }

    private double elapsedSeconds(long now) {
        long effectiveNow = paused && pausedAtNanos > 0L ? pausedAtNanos : now;
        return Math.max(0.0, (effectiveNow - pageStartNanos - accumulatedPauseNanos) / 1_000_000_000.0);
    }

    static double automaticDuration(String prose) {
        int words = prose == null || prose.isBlank() ? 0 : prose.strip().split("\\s+").length;
        return Math.clamp(5.0 + words * 0.15, 10.0, 22.0);
    }

    static double turnProgress(long startNanos, long nowNanos) {
        if (startNanos <= 0L) return 0.0;
        return Math.clamp((nowNanos - startNanos) / (double) TURN_DURATION_NANOS, 0.0, 1.0);
    }

    private void finish() {
        if (finished) return;
        finished = true;
        stopTimer();
        game.finishCampaignProloguePresentation();
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
}
