package com.example.birdgame3;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;
import javafx.scene.shape.StrokeLineCap;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.TextAlignment;
import javafx.stage.Stage;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

final class BirdGame3TournamentUi {
    @FunctionalInterface
    interface EscapeBinder {
        void bind(Scene scene, Button backButton);
    }

    @FunctionalInterface
    interface StageSceneSetter {
        void accept(Stage stage, Scene scene);
    }

    @FunctionalInterface
    interface LabelFitter {
        void fit(Label label, double maxSize, double minSize, double maxWidth);
    }

    private static final Insets MENU_PADDING = new Insets(60, 80, 60, 80);
    private static final double MENU_TEXT_MAX_WIDTH = 1100.0;

    private final BirdGame3 game;
    private final UIFactory uiFactory;
    private final Runnable playMenuMusic;
    private final Runnable resetTournamentRun;
    private final Consumer<Stage> showTournamentSetup;
    private final Consumer<Stage> showMenu;
    private final Consumer<Stage> startNextTournamentMatch;
    private final BiConsumer<Stage, BirdGame3.TournamentMatch> launchTournamentMatch;
    private final BiConsumer<Stage, BirdGame3.TournamentMatch> simulateTournamentMatch;
    private final EscapeBinder escapeBinder;
    private final Consumer<Scene> keyboardNavigationSetup;
    private final Consumer<Scene> consoleHighlighter;
    private final StageSceneSetter sceneSetter;
    private final Consumer<Label> noEllipsis;
    private final LabelFitter labelFitter;

    BirdGame3TournamentUi(BirdGame3 game,
                          UIFactory uiFactory,
                          Runnable playMenuMusic,
                          Runnable resetTournamentRun,
                          Consumer<Stage> showTournamentSetup,
                          Consumer<Stage> showMenu,
                          Consumer<Stage> startNextTournamentMatch,
                          BiConsumer<Stage, BirdGame3.TournamentMatch> launchTournamentMatch,
                          BiConsumer<Stage, BirdGame3.TournamentMatch> simulateTournamentMatch,
                          EscapeBinder escapeBinder,
                          Consumer<Scene> keyboardNavigationSetup,
                          Consumer<Scene> consoleHighlighter,
                          StageSceneSetter sceneSetter,
                          Consumer<Label> noEllipsis,
                          LabelFitter labelFitter) {
        this.game = game;
        this.uiFactory = uiFactory;
        this.playMenuMusic = playMenuMusic;
        this.resetTournamentRun = resetTournamentRun;
        this.showTournamentSetup = showTournamentSetup;
        this.showMenu = showMenu;
        this.startNextTournamentMatch = startNextTournamentMatch;
        this.launchTournamentMatch = launchTournamentMatch;
        this.simulateTournamentMatch = simulateTournamentMatch;
        this.escapeBinder = escapeBinder;
        this.keyboardNavigationSetup = keyboardNavigationSetup;
        this.consoleHighlighter = consoleHighlighter;
        this.sceneSetter = sceneSetter;
        this.noEllipsis = noEllipsis;
        this.labelFitter = labelFitter;
    }

    void showBracket(Stage stage) {
        playMenuMusic.run();
        game.syncTournamentEntries();
        if (game.tournamentRounds().isEmpty()) {
            game.buildTournamentBracket();
        }
        game.resolveTournamentByes();

        BirdGame3.TournamentMatch nextMatch = game.findNextTournamentMatch();
        boolean complete = game.isTournamentComplete();

        BorderPane root = new BorderPane();
        root.setPadding(new Insets(16, 20, 16, 20));
        root.setStyle("-fx-background-color: linear-gradient(to bottom, #07111F, #142849 58%, #1B365E);");

        Label title = new Label("TOURNAMENT BRACKET");
        title.setFont(Font.font("Arial Black", FontWeight.BOLD, 58));
        title.setTextFill(Color.GOLD);

        String mapLine = game.isTournamentMapRandom()
                ? "Map: RANDOM"
                : "Map: " + game.mapDisplayName(game.tournamentFixedMap());
        Label subtitle = new Label(mapLine + "  |  Entrants: " + game.tournamentEntrantCount()
                + "  |  Random picks stay locked for the run");
        subtitle.setFont(Font.font("Consolas", 18));
        subtitle.setTextFill(Color.web("#B3E5FC"));

        Label legend = new Label(nextMatch != null ? "Gold outline marks the next match." : "Champion crowned.");
        legend.setFont(Font.font("Consolas", 14));
        legend.setTextFill(Color.web("#FFE082"));

        VBox header = new VBox(4, title, subtitle, legend);
        header.setAlignment(Pos.CENTER);
        ScrollPane scroll = buildBracketScrollPane(nextMatch);

        Button primary = uiFactory.action(complete ? "VIEW CHAMPION" : "START NEXT MATCH", 440, 86, 30, "#1565C0", 22, () -> {
            if (complete) {
                showComplete(stage);
            } else {
                startNextTournamentMatch.accept(stage);
            }
        });
        Button reset = uiFactory.action("RESET TOURNAMENT", 330, 86, 28, "#00897B", 20, () -> {
            resetTournamentRun.run();
            showTournamentSetup.accept(stage);
        });
        Button exit = uiFactory.action("EXIT TO HUB", 330, 86, 28, "#FF1744", 20, () -> {
            resetTournamentRun.run();
            showMenu.accept(stage);
        });
        HBox buttons = new HBox(18, primary, reset, exit);
        buttons.setAlignment(Pos.CENTER);
        buttons.setPadding(new Insets(10, 0, 0, 0));

        root.setTop(header);
        root.setCenter(scroll);
        root.setBottom(buttons);
        BorderPane.setAlignment(header, Pos.CENTER);

        Scene scene = new Scene(root, BirdGame3.WIDTH, BirdGame3.HEIGHT);
        escapeBinder.bind(scene, exit);
        keyboardNavigationSetup.accept(scene);
        consoleHighlighter.accept(scene);
        sceneSetter.accept(stage, scene);
        primary.requestFocus();
    }

    void showCpuChoice(Stage stage, BirdGame3.TournamentMatch match) {
        playMenuMusic.run();

        VBox root = MenuLayout.buildMenuRoot("-fx-background-color: linear-gradient(to bottom, #091426, #1A2B4B);",
                MENU_PADDING, 30);

        Label title = new Label("CPU VS CPU");
        title.setFont(Font.font("Arial Black", FontWeight.BOLD, 92));
        title.setTextFill(Color.GOLD);

        String left = game.tournamentEntryLabel(match.a) + "  |  " + game.tournamentBirdLabel(match.a, true);
        String right = game.tournamentEntryLabel(match.b) + "  |  " + game.tournamentBirdLabel(match.b, true);
        Label info = new Label(left + "\nvs\n" + right + "\nWatch the match or simulate the result.");
        MenuLayout.styleMenuMessage(info, 30, "#B3E5FC", MENU_TEXT_MAX_WIDTH, noEllipsis::accept);

        Button watch = uiFactory.action("WATCH MATCH", 520, 120, 42, "#1565C0", 26,
                () -> launchTournamentMatch.accept(stage, match));
        Button skip = uiFactory.action("SIMULATE", 420, 120, 38, "#8E24AA", 26,
                () -> simulateTournamentMatch.accept(stage, match));
        Button exit = uiFactory.action("EXIT TO HUB", 420, 120, 34, "#FF1744", 22, () -> {
            resetTournamentRun.run();
            showMenu.accept(stage);
        });

        HBox buttons = new HBox(24, watch, skip, exit);
        buttons.setAlignment(Pos.CENTER);

        root.getChildren().addAll(title, info, buttons);
        Scene scene = new Scene(root, BirdGame3.WIDTH, BirdGame3.HEIGHT);
        escapeBinder.bind(scene, exit);
        keyboardNavigationSetup.accept(scene);
        consoleHighlighter.accept(scene);
        sceneSetter.accept(stage, scene);
        watch.requestFocus();
    }

    void showSimResult(Stage stage, BirdGame3.TournamentEntry winner) {
        playMenuMusic.run();

        VBox root = MenuLayout.buildMenuRoot("-fx-background-color: linear-gradient(to bottom, #081122, #13294B);",
                MENU_PADDING, 28);

        Label title = new Label("SIMULATED RESULT");
        title.setFont(Font.font("Arial Black", FontWeight.BOLD, 88));
        title.setTextFill(Color.GOLD);

        Label info = new Label(winner != null ? game.tournamentEntryLabel(winner) + " advances." : "No winner.");
        MenuLayout.styleMenuMessage(info, 32, "#C5E1A5", MENU_TEXT_MAX_WIDTH, noEllipsis::accept);

        boolean complete = game.isTournamentComplete();
        Button next = uiFactory.action(complete ? "VIEW CHAMPION" : "VIEW BRACKET", 520, 120, 40, "#1565C0", 26, () -> {
            game.resetMatchStats();
            if (complete) {
                showComplete(stage);
            } else {
                showBracket(stage);
            }
        });
        Button exit = uiFactory.action("EXIT TO HUB", 420, 120, 34, "#FF1744", 22, () -> {
            resetTournamentRun.run();
            showMenu.accept(stage);
        });
        HBox buttons = new HBox(24, next, exit);
        buttons.setAlignment(Pos.CENTER);

        root.getChildren().addAll(title, info, buttons);
        Scene scene = new Scene(root, BirdGame3.WIDTH, BirdGame3.HEIGHT);
        escapeBinder.bind(scene, exit);
        keyboardNavigationSetup.accept(scene);
        consoleHighlighter.accept(scene);
        sceneSetter.accept(stage, scene);
        next.requestFocus();
    }

    void showComplete(Stage stage) {
        playMenuMusic.run();

        BirdGame3.TournamentEntry champion = null;
        List<List<BirdGame3.TournamentMatch>> rounds = game.tournamentRounds();
        if (!rounds.isEmpty()) {
            List<BirdGame3.TournamentMatch> last = rounds.getLast();
            if (!last.isEmpty()) {
                champion = last.getFirst().winner;
            }
        }

        VBox root = MenuLayout.buildMenuRoot("-fx-background-color: linear-gradient(to bottom, #081122, #13294B);",
                MENU_PADDING, 28);

        Label title = new Label("TOURNAMENT COMPLETE");
        title.setFont(Font.font("Arial Black", FontWeight.BOLD, 90));
        title.setTextFill(Color.GOLD);

        Label info = new Label(champion != null ? game.tournamentEntryLabel(champion) + " is champion!" : "No champion.");
        MenuLayout.styleMenuMessage(info, 34, "#FFE082", MENU_TEXT_MAX_WIDTH, noEllipsis::accept);

        Button setup = uiFactory.action("NEW TOURNAMENT", 520, 120, 38, "#1565C0", 26,
                () -> showTournamentSetup.accept(stage));
        Button exit = uiFactory.action("EXIT TO HUB", 420, 120, 34, "#FF1744", 22, () -> {
            resetTournamentRun.run();
            showMenu.accept(stage);
        });
        HBox buttons = new HBox(24, setup, exit);
        buttons.setAlignment(Pos.CENTER);

        root.getChildren().addAll(title, info, buttons);
        Scene scene = new Scene(root, BirdGame3.WIDTH, BirdGame3.HEIGHT);
        escapeBinder.bind(scene, exit);
        keyboardNavigationSetup.accept(scene);
        consoleHighlighter.accept(scene);
        sceneSetter.accept(stage, scene);
        setup.requestFocus();
    }

    private ScrollPane buildBracketScrollPane(BirdGame3.TournamentMatch nextMatch) {
        List<List<BirdGame3.TournamentMatch>> rounds = game.tournamentRounds();
        int firstRoundMatches = rounds.isEmpty() ? 0 : rounds.getFirst().size();
        double cardWidth;
        double cardHeight;
        double columnGap;
        double leftPadding = 36.0;
        double topPadding;
        double labelY;
        double baseGap;
        double labelFont;
        double connectorStroke;
        boolean compact;

        if (firstRoundMatches <= 2) {
            cardWidth = 300.0;
            cardHeight = 132.0;
            columnGap = 138.0;
            topPadding = 96.0;
            labelY = 26.0;
            baseGap = 42.0;
            labelFont = 24.0;
            connectorStroke = 4.0;
            compact = false;
        } else if (firstRoundMatches <= 4) {
            cardWidth = 286.0;
            cardHeight = 112.0;
            columnGap = 118.0;
            topPadding = 82.0;
            labelY = 18.0;
            baseGap = 18.0;
            labelFont = 22.0;
            connectorStroke = 3.5;
            compact = true;
        } else if (firstRoundMatches <= 8) {
            cardWidth = 268.0;
            cardHeight = 98.0;
            columnGap = 106.0;
            topPadding = 74.0;
            labelY = 16.0;
            baseGap = 12.0;
            labelFont = 20.0;
            connectorStroke = 3.0;
            compact = true;
        } else {
            cardWidth = 248.0;
            cardHeight = 92.0;
            columnGap = 96.0;
            topPadding = 70.0;
            labelY = 14.0;
            baseGap = 10.0;
            labelFont = 18.0;
            connectorStroke = 3.0;
            compact = true;
        }

        List<List<Double>> centersByRound = new ArrayList<>();
        for (int roundIndex = 0; roundIndex < rounds.size(); roundIndex++) {
            List<BirdGame3.TournamentMatch> round = rounds.get(roundIndex);
            List<Double> centers = new ArrayList<>(round.size());
            if (roundIndex == 0) {
                for (int matchIndex = 0; matchIndex < round.size(); matchIndex++) {
                    centers.add(topPadding + matchIndex * (cardHeight + baseGap) + cardHeight / 2.0);
                }
            } else {
                List<Double> previous = centersByRound.get(roundIndex - 1);
                for (int matchIndex = 0; matchIndex < round.size(); matchIndex++) {
                    centers.add((previous.get(matchIndex * 2) + previous.get(matchIndex * 2 + 1)) / 2.0);
                }
            }
            centersByRound.add(centers);
        }

        double width = leftPadding
                + rounds.size() * cardWidth
                + Math.max(0, rounds.size() - 1) * columnGap
                + 72.0;
        double height = centersByRound.isEmpty() || centersByRound.getFirst().isEmpty()
                ? 540.0
                : centersByRound.getFirst().getLast() + cardHeight / 2.0 + 70.0;

        javafx.scene.layout.Pane bracketPane = new javafx.scene.layout.Pane();
        bracketPane.setPrefSize(width, height);
        bracketPane.setMinSize(width, height);
        bracketPane.setMaxSize(width, height);

        Group lineLayer = new Group();
        bracketPane.getChildren().add(lineLayer);

        for (int roundIndex = 0; roundIndex < rounds.size(); roundIndex++) {
            double x = leftPadding + roundIndex * (cardWidth + columnGap);

            Label roundLabel = new Label(game.tournamentRoundLabel(roundIndex));
            roundLabel.setFont(Font.font("Arial Black", labelFont));
            roundLabel.setTextFill(Color.web("#FFE082"));
            roundLabel.setMinWidth(cardWidth);
            roundLabel.setPrefWidth(cardWidth);
            roundLabel.setMaxWidth(cardWidth);
            roundLabel.setTextAlignment(TextAlignment.CENTER);
            roundLabel.setAlignment(Pos.CENTER);
            roundLabel.setLayoutX(x);
            roundLabel.setLayoutY(labelY);
            bracketPane.getChildren().add(roundLabel);

            if (roundIndex > 0) {
                double prevX = leftPadding + (roundIndex - 1) * (cardWidth + columnGap);
                double joinX = x - columnGap * 0.55;
                List<Double> previousCenters = centersByRound.get(roundIndex - 1);
                List<Double> currentCenters = centersByRound.get(roundIndex);
                for (int matchIndex = 0; matchIndex < currentCenters.size(); matchIndex++) {
                    double topCenter = previousCenters.get(matchIndex * 2);
                    double bottomCenter = previousCenters.get(matchIndex * 2 + 1);
                    double currentCenter = currentCenters.get(matchIndex);

                    Line topLine = new Line(prevX + cardWidth, topCenter, joinX, topCenter);
                    Line bottomLine = new Line(prevX + cardWidth, bottomCenter, joinX, bottomCenter);
                    Line vertical = new Line(joinX, topCenter, joinX, bottomCenter);
                    Line finalLine = new Line(joinX, currentCenter, x, currentCenter);
                    for (Line line : List.of(topLine, bottomLine, vertical, finalLine)) {
                        line.setStroke(Color.web("rgba(255, 235, 160, 0.72)"));
                        line.setStrokeWidth(connectorStroke);
                        line.setStrokeLineCap(StrokeLineCap.ROUND);
                    }
                    lineLayer.getChildren().addAll(topLine, bottomLine, vertical, finalLine);
                }
            }
        }

        for (int roundIndex = 0; roundIndex < rounds.size(); roundIndex++) {
            List<BirdGame3.TournamentMatch> round = rounds.get(roundIndex);
            double x = leftPadding + roundIndex * (cardWidth + columnGap);
            List<Double> centers = centersByRound.get(roundIndex);
            for (int matchIndex = 0; matchIndex < round.size(); matchIndex++) {
                BirdGame3.TournamentMatch match = round.get(matchIndex);
                StackPane card = buildBracketMatchCard(match, nextMatch, roundIndex == 0, cardWidth, cardHeight, compact);
                card.setLayoutX(x);
                card.setLayoutY(centers.get(matchIndex) - cardHeight / 2.0);
                bracketPane.getChildren().add(card);
            }
        }

        ScrollPane scroll = new ScrollPane(bracketPane);
        scroll.setFitToHeight(false);
        scroll.setFitToWidth(false);
        scroll.setStyle("-fx-background: transparent; -fx-background-color: transparent; -fx-control-inner-background: transparent;");
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scroll.setPannable(true);
        return scroll;
    }

    private StackPane buildBracketMatchCard(BirdGame3.TournamentMatch match, BirdGame3.TournamentMatch nextMatch,
                                            boolean firstRound, double cardWidth, double cardHeight, boolean compact) {
        boolean current = nextMatch != null && match == nextMatch;
        boolean decided = match.winner != null;
        String border = current ? "#FFD54F" : (decided ? "#AED581" : "rgba(255,255,255,0.16)");
        String background = current
                ? "linear-gradient(to bottom right, rgba(55,66,15,0.96), rgba(21,28,6,0.98))"
                : decided
                ? "linear-gradient(to bottom right, rgba(28,46,24,0.96), rgba(10,17,9,0.98))"
                : "linear-gradient(to bottom right, rgba(19,27,41,0.96), rgba(7,10,16,0.98))";

        VBox body = new VBox(compact ? 6 : 8);
        body.setPadding(new Insets(compact ? 8 : 10));
        body.getChildren().add(buildBracketEntrantRow(match.a, firstRound ? "BYE" : "TBD", match.winner == match.a, compact));
        body.getChildren().add(buildBracketEntrantRow(match.b, firstRound ? "BYE" : "TBD", match.winner == match.b, compact));

        Label footer = new Label(decided
                ? "ADVANCES: " + game.tournamentEntryLabel(match.winner)
                : current ? "NEXT MATCH" : "WAITING");
        footer.setFont(Font.font("Consolas", compact ? 13 : 14));
        footer.setTextFill(decided ? Color.web("#C5E1A5") : (current ? Color.web("#FFE082") : Color.web("#90A4AE")));
        footer.setMaxWidth(cardWidth - 20.0);
        noEllipsis.accept(footer);
        labelFitter.fit(footer, compact ? 13 : 14, 10, cardWidth - 20.0);
        body.getChildren().add(footer);

        StackPane card = new StackPane(body);
        card.setPrefSize(cardWidth, cardHeight);
        card.setMinSize(cardWidth, cardHeight);
        card.setMaxSize(cardWidth, cardHeight);
        card.setStyle("-fx-background-color: " + background + "; -fx-background-radius: " + (compact ? "18" : "22") + "; "
                + "-fx-border-color: " + border + "; -fx-border-width: " + (current ? "3.5" : "2.0") + "; "
                + "-fx-border-radius: " + (compact ? "18" : "22") + ";");
        if (current) {
            card.setEffect(new javafx.scene.effect.DropShadow(18, Color.rgb(255, 213, 79, 0.35)));
        }
        return card;
    }

    private HBox buildBracketEntrantRow(BirdGame3.TournamentEntry entry, String placeholder, boolean winner, boolean compact) {
        HBox row = new HBox(compact ? 6 : 8);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(compact ? 5 : 6, compact ? 7 : 8, compact ? 5 : 6, compact ? 7 : 8));

        if (entry == null) {
            Label bye = new Label(placeholder);
            bye.setFont(Font.font("Consolas", compact ? 13 : 15));
            bye.setTextFill(Color.web("#B0BEC5"));
            row.getChildren().add(bye);
            row.setStyle("-fx-background-color: rgba(255,255,255,0.05); -fx-background-radius: 14;");
            return row;
        }

        BirdGame3.BirdType birdType = game.tournamentAssignedBird(entry);
        boolean randomOrigin = entry.selectedType == null && birdType != null;

        double portraitSize = compact ? 32.0 : 38.0;
        double portraitFrameSize = compact ? 40.0 : 46.0;
        Canvas portrait = new Canvas(portraitSize, portraitSize);
        game.drawTournamentPortrait(portrait, birdType);
        StackPane portraitFrame = sizedStackPane(new StackPane(portrait), portraitFrameSize,
                "-fx-background-color: rgba(0,0,0,0.48); -fx-background-radius: " + (compact ? "10" : "12") + "; "
                        + "-fx-border-color: rgba(255,255,255,0.18); -fx-border-width: 1.5; -fx-border-radius: 12;");

        Label seed = new Label("#" + game.tournamentEntrySeedNumber(entry));
        seed.setFont(Font.font("Arial Black", compact ? 11 : 12));
        seed.setTextFill(Color.web("#111111"));
        StackPane seedChip = new StackPane(seed);
        seedChip.setPadding(new Insets(2, compact ? 7 : 8, 2, compact ? 7 : 8));
        seedChip.setStyle("-fx-background-color: #FFE082; -fx-background-radius: 999;");

        Label name = new Label(game.tournamentEntryLabel(entry));
        double nameWidth = compact ? 92.0 : 104.0;
        name.setFont(Font.font("Arial Black", compact ? 13 : 14));
        name.setTextFill(winner ? Color.WHITE : Color.web("#ECEFF1"));
        name.setMaxWidth(nameWidth);
        noEllipsis.accept(name);
        labelFitter.fit(name, compact ? 13 : 14, 10, nameWidth);

        Label bird = new Label(birdType != null ? birdType.name.toUpperCase(Locale.ROOT) : placeholder);
        bird.setFont(Font.font("Arial Black", compact ? 10 : 11));
        bird.setTextFill(Color.WHITE);
        bird.setMaxWidth(compact ? 88.0 : 102.0);
        noEllipsis.accept(bird);
        labelFitter.fit(bird, compact ? 10 : 11, 8, compact ? 88.0 : 102.0);
        bird.setPadding(new Insets(2, compact ? 7 : 8, 2, compact ? 7 : 8));
        bird.setStyle("-fx-background-color: " + (randomOrigin ? "#8E24AA" : "#455A64")
                + "; -fx-background-radius: 999;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        row.getChildren().addAll(portraitFrame, seedChip, name, spacer, bird);
        row.setStyle("-fx-background-color: " + (winner ? "rgba(255,255,255,0.12)" : "rgba(255,255,255,0.06)")
                + "; -fx-background-radius: 14;");
        return row;
    }

    private static StackPane sizedStackPane(StackPane pane, double size, String style) {
        pane.setMinSize(size, size);
        pane.setPrefSize(size, size);
        pane.setMaxSize(size, size);
        pane.setStyle(style);
        return pane;
    }
}
