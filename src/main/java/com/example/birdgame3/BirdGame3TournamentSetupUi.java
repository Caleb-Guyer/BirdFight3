package com.example.birdgame3;

import com.example.birdgame3.BirdGame3.BirdType;
import com.example.birdgame3.BirdGame3.MapType;
import com.example.birdgame3.BirdGame3.TournamentEntry;
import javafx.application.Platform;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Point2D;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.control.Button;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextInputDialog;
import javafx.scene.effect.DropShadow;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Polygon;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;
import javafx.stage.Stage;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Consumer;

final class BirdGame3TournamentSetupUi {
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

    private static final String FIGHT_SELECTOR_COLOR_PROP = "fight_selector_color";
    private static final String FIGHT_SELECTOR_SNAP_DISTANCE_PROP = "fight_selector_snap_distance";
    private static final double FIGHT_SELECTOR_BOUND_MARGIN = 40.0;
    private static final double LAYOUT_WIDTH = 1600.0;
    private static final double LAYOUT_HEIGHT = 950.0;

    private final BirdGame3 game;
    private final UIFactory uiFactory;
    private final Runnable playButtonClick;
    private final Consumer<Stage> showClassicMoreMenu;
    private final Consumer<Stage> beginTournament;
    private final EscapeBinder escapeBinder;
    private final Consumer<Scene> keyboardNavigationSetup;
    private final Consumer<Scene> consoleHighlighter;
    private final StageSceneSetter sceneSetter;
    private final Consumer<Button> buttonNoEllipsis;
    private final Consumer<Label> labelNoEllipsis;
    private final LabelFitter labelFitter;

    BirdGame3TournamentSetupUi(BirdGame3 game,
                               UIFactory uiFactory,
                               Runnable playButtonClick,
                               Consumer<Stage> showClassicMoreMenu,
                               Consumer<Stage> beginTournament,
                               EscapeBinder escapeBinder,
                               Consumer<Scene> keyboardNavigationSetup,
                               Consumer<Scene> consoleHighlighter,
                               StageSceneSetter sceneSetter,
                               Consumer<Button> buttonNoEllipsis,
                               Consumer<Label> labelNoEllipsis,
                               LabelFitter labelFitter) {
        this.game = game;
        this.uiFactory = uiFactory;
        this.playButtonClick = playButtonClick;
        this.showClassicMoreMenu = showClassicMoreMenu;
        this.beginTournament = beginTournament;
        this.escapeBinder = escapeBinder;
        this.keyboardNavigationSetup = keyboardNavigationSetup;
        this.consoleHighlighter = consoleHighlighter;
        this.sceneSetter = sceneSetter;
        this.buttonNoEllipsis = buttonNoEllipsis;
        this.labelNoEllipsis = labelNoEllipsis;
        this.labelFitter = labelFitter;
    }

    void showSetup(Stage stage) {
        sceneSetter.accept(stage, buildScene(stage));
    }

    private Scene buildScene(Stage stage) {
        StackPane root = new StackPane();
        root.getProperties().put("noAutoScale", true);
        root.setStyle("-fx-background-color: #06070A;");

        BorderPane content = new BorderPane();
        content.setPadding(new Insets(12));
        content.setMinSize(LAYOUT_WIDTH, LAYOUT_HEIGHT);
        content.setPrefSize(LAYOUT_WIDTH, LAYOUT_HEIGHT);
        content.setMaxSize(LAYOUT_WIDTH, LAYOUT_HEIGHT);
        content.setStyle("-fx-background-color: linear-gradient(to bottom, #06070A, #0D1017 34%, #171B22 100%);");
        root.getChildren().add(content);

        TournamentSetupUi ui = new TournamentSetupUi();
        content.setTop(new VBox(buildTopStrip(stage, ui)));
        configureSelectionPane(ui);

        VBox rosterCard = buildRosterCard(ui);
        StackPane footerPanel = buildFooter(stage, ui);

        VBox body = new VBox(12, rosterCard, footerPanel);
        body.setAlignment(Pos.TOP_CENTER);
        content.setCenter(body);

        if (ui.refreshAll[0] != null) {
            ui.refreshAll[0].run();
        }

        Scene scene = new Scene(root, BirdGame3.WIDTH, BirdGame3.HEIGHT);
        scene.setFill(Color.web("#06070A"));
        escapeBinder.bind(scene, ui.backBtn);
        keyboardNavigationSetup.accept(scene);
        consoleHighlighter.accept(scene);
        bindFixedFrameScale(scene, content);
        if (ui.startBtn != null) {
            ui.startBtn.requestFocus();
        }
        return scene;
    }

    private StackPane buildTopStrip(Stage stage, TournamentSetupUi ui) {
        ui.backBtn = uiFactory.action("BACK", 156, 56, 22, "#B5121B", 16, () -> showClassicMoreMenu.accept(stage));
        ui.backBtn.setStyle("-fx-background-color: linear-gradient(to bottom, #D61D28, #981019); "
                + "-fx-text-fill: white; -fx-font-family: 'Arial Black'; -fx-font-size: 17px; "
                + "-fx-font-weight: bold; -fx-background-radius: 18; -fx-border-color: black; "
                + "-fx-border-width: 3; -fx-border-radius: 18;");
        buttonNoEllipsis.accept(ui.backBtn);

        Label title = new Label("TOURNAMENT MODE");
        title.setFont(Font.font("Arial Black", FontWeight.BOLD, 34));
        title.setTextFill(Color.web("#111111"));
        StackPane titleBanner = sizedPane(new StackPane(title), 548, "-fx-background-color: linear-gradient(to bottom, #FFE45C, #F8C528); "
                + "-fx-background-radius: 12; -fx-border-color: black; -fx-border-width: 4; "
                + "-fx-border-radius: 12;");
        titleBanner.setEffect(new DropShadow(18, Color.rgb(0, 0, 0, 0.28)));

        BorderPane topChrome = new BorderPane();
        topChrome.setLeft(ui.backBtn);
        BorderPane.setAlignment(ui.backBtn, Pos.CENTER_LEFT);

        StackPane topStrip = new StackPane(topChrome, titleBanner);
        topStrip.setPadding(new Insets(8, 12, 8, 12));
        topStrip.setMinHeight(86);
        topStrip.setStyle("-fx-background-color: linear-gradient(to right, #8E0D16 0%, #C51A24 42%, #111317 42%, #111317 100%); "
                + "-fx-background-radius: 24; -fx-border-color: black; -fx-border-width: 4; -fx-border-radius: 24;");
        topStrip.setEffect(new DropShadow(24, Color.rgb(0, 0, 0, 0.30)));
        return topStrip;
    }

    private VBox buildRosterCard(TournamentSetupUi ui) {
        Label rosterTitle = new Label("SELECT YOUR TOURNAMENT BIRD");
        rosterTitle.setFont(Font.font("Arial Black", 26));
        rosterTitle.setTextFill(Color.web("#FFF176"));
        rosterTitle.setEffect(new DropShadow(10, Color.rgb(0, 0, 0, 0.30)));
        HBox rosterHeader = new HBox(rosterTitle);
        rosterHeader.setAlignment(Pos.CENTER_LEFT);

        VBox rosterCard = new VBox(8, rosterHeader, ui.selectionPane);
        rosterCard.setPadding(new Insets(10));
        rosterCard.setStyle("-fx-background-color: rgba(4,5,8,0.90); -fx-background-radius: 28; "
                + "-fx-border-color: rgba(255,255,255,0.12); -fx-border-width: 3; -fx-border-radius: 28;");
        return rosterCard;
    }

    private void configureSelectionPane(TournamentSetupUi ui) {
        List<BirdType> availableBirds = game.unlockedBirdPool();
        List<BirdType> gridBirds = new ArrayList<>(availableBirds);
        gridBirds.add(null);

        double paneW = 1520;
        double paneH = 406;
        ui.selectionPane.setPrefSize(paneW, paneH);
        ui.selectionPane.setMinSize(paneW, paneH);
        ui.selectionPane.setMaxSize(paneW, paneH);
        ui.selectionPane.setStyle("-fx-background-color: linear-gradient(to bottom, rgba(21,25,31,0.97), rgba(7,9,12,0.99)); "
                + "-fx-background-radius: 22; -fx-border-color: rgba(255,255,255,0.16); "
                + "-fx-border-width: 2; -fx-border-radius: 22;");
        Rectangle selectionClip = new Rectangle();
        selectionClip.widthProperty().bind(ui.selectionPane.widthProperty());
        selectionClip.heightProperty().bind(ui.selectionPane.heightProperty());
        ui.selectionPane.setClip(selectionClip);

        int columns = Math.clamp((int) Math.ceil(gridBirds.size() / 3.0), 8, 12);
        int rows = (int) Math.ceil(gridBirds.size() / (double) columns);
        double dockW = 230;
        double dockX = 16;
        double dockY = 18;
        double dockH = paneH - dockY * 2;
        double gridX = dockX + dockW + 16;
        double gridY = 18;
        double gridW = paneW - gridX - 18;
        double gridH = paneH - gridY * 2;
        double cellW = gridW / columns;
        double cellH = gridH / Math.max(1, rows);

        Region gridFrame = new Region();
        gridFrame.setLayoutX(gridX - 12);
        gridFrame.setLayoutY(gridY - 12);
        gridFrame.setPrefSize(gridW + 24, gridH + 24);
        gridFrame.setStyle("-fx-background-color: rgba(255,255,255,0.035); "
                + "-fx-border-color: rgba(255,255,255,0.14); -fx-border-width: 2; "
                + "-fx-background-radius: 20; -fx-border-radius: 20;");
        ui.selectionPane.getChildren().add(gridFrame);

        Region focusDock = new Region();
        focusDock.setPrefSize(dockW, dockH);
        focusDock.setLayoutX(dockX);
        focusDock.setLayoutY(dockY);
        focusDock.setStyle("-fx-background-color: linear-gradient(to bottom, rgba(18,24,32,0.96), rgba(7,9,14,0.98)); "
                + "-fx-background-radius: 20; -fx-border-color: rgba(255,224,130,0.42); "
                + "-fx-border-width: 2; -fx-border-radius: 20;");
        ui.selectionPane.getChildren().add(focusDock);

        ui.activeSeed = new Label();
        ui.activeSeed.setFont(Font.font("Consolas", 18));
        ui.activeSeed.setTextFill(Color.web("#FFE082"));
        labelNoEllipsis.accept(ui.activeSeed);

        ui.activeName = new Label();
        ui.activeName.setFont(Font.font("Arial Black", 26));
        ui.activeName.setTextFill(Color.WHITE);
        ui.activeName.setWrapText(true);
        ui.activeName.setTextAlignment(TextAlignment.CENTER);
        ui.activeName.setAlignment(Pos.CENTER);
        ui.activeName.setMaxWidth(dockW - 36);
        labelNoEllipsis.accept(ui.activeName);

        ui.activeRole = new Label();
        ui.activeRole.setFont(Font.font("Arial Black", 15));
        ui.activeRole.setTextFill(Color.WHITE);
        ui.activeRole.setPadding(new Insets(6, 14, 6, 14));

        ui.activePortrait = new Canvas(150, 150);
        StackPane activePortraitFrame = sizedPane(new StackPane(ui.activePortrait), 168, "-fx-background-color: rgba(0,0,0,0.44); -fx-background-radius: 22; "
                + "-fx-border-color: rgba(255,255,255,0.20); -fx-border-width: 2; "
                + "-fx-border-radius: 22;");

        ui.activeBird = new Label();
        ui.activeBird.setFont(Font.font("Arial Black", 18));
        ui.activeBird.setTextFill(Color.web("#FFF4C3"));
        ui.activeBird.setWrapText(true);
        ui.activeBird.setTextAlignment(TextAlignment.CENTER);
        ui.activeBird.setAlignment(Pos.CENTER);
        ui.activeBird.setMaxWidth(dockW - 40);
        labelNoEllipsis.accept(ui.activeBird);

        Label focusHint = new Label("Random locks one bird for the entire tournament.");
        focusHint.setFont(Font.font("Consolas", 15));
        focusHint.setTextFill(Color.web("#B0BEC5"));
        focusHint.setWrapText(true);
        focusHint.setTextAlignment(TextAlignment.CENTER);
        focusHint.setAlignment(Pos.CENTER);
        focusHint.setMaxWidth(dockW - 40);

        VBox focusCard = new VBox(10, ui.activeSeed, ui.activeName, ui.activeRole, activePortraitFrame, ui.activeBird, focusHint);
        focusCard.setAlignment(Pos.TOP_CENTER);
        focusCard.setPadding(new Insets(18, 12, 14, 12));
        focusCard.setLayoutX(dockX + 8);
        focusCard.setLayoutY(dockY + 8);
        focusCard.setPrefSize(dockW - 16, dockH - 16);
        focusCard.setStyle("-fx-background-color: rgba(0,0,0,0.18); -fx-background-radius: 18;");
        ui.selectionPane.getChildren().add(focusCard);

        for (int i = 0; i < gridBirds.size(); i++) {
            BirdType type = gridBirds.get(i);
            boolean isRandom = type == null;
            int col = i % columns;
            int row = i / columns;
            double cellX = gridX + col * cellW;
            double cellY = gridY + row * cellH;
            double centerX = cellX + cellW / 2.0;
            double centerY = cellY + cellH / 2.0;
            double tileW = Math.max(110.0, cellW - 8.0);
            double tileH = Math.max(110.0, cellH - 8.0);
            double iconSize = Math.clamp(Math.min(tileW - 24.0, tileH - 52.0), 68.0, 138.0);

            Canvas icon = new Canvas(iconSize, iconSize);
            game.drawTournamentPortrait(icon, type);
            Node iconNode = icon;
            if (type == BirdType.FALCON) {
                StackPane iconStack = new StackPane();
                iconStack.setPrefSize(iconSize, iconSize);
                iconStack.getChildren().add(icon);
                Canvas echo = new Canvas(iconSize * 0.55, iconSize * 0.55);
                game.drawTournamentPortrait(echo, BirdType.EAGLE);
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
                game.drawTournamentPortrait(echo, BirdType.OPIUMBIRD);
                echo.setOpacity(0.32);
                StackPane.setAlignment(echo, Pos.TOP_LEFT);
                StackPane.setMargin(echo, new Insets(6, 0, 0, 6));
                iconStack.getChildren().add(echo);
                iconNode = iconStack;
            }

            Label name = new Label(isRandom ? "RANDOM" : type.name.toUpperCase(Locale.ROOT));
            name.setFont(Font.font("Arial Black", tileH < 140 ? 13 : 15));
            name.setTextFill(Color.WHITE);
            name.setMaxWidth(tileW - 12);
            name.setTextAlignment(TextAlignment.CENTER);
            name.setAlignment(Pos.CENTER);
            name.setWrapText(true);
            if (type == BirdType.FALCON) {
                name.setText("FALCON\nECHO OF EAGLE");
                name.setFont(Font.font("Arial Black", 12));
            } else if (type == BirdType.HEISENBIRD) {
                name.setText("HEISENBIRD\nECHO OF OPIUM");
                name.setFont(Font.font("Arial Black", 12));
            }

            StackPane namePlate = new StackPane(name);
            namePlate.setPadding(new Insets(5, 8, 6, 8));
            namePlate.setStyle("-fx-background-color: rgba(0,0,0,0.78); -fx-background-radius: 12;");

            StackPane iconHolder = new StackPane(iconNode);
            iconHolder.setPadding(new Insets(8, 8, 4, 8));

            BorderPane tileBody = new BorderPane();
            tileBody.setCenter(iconHolder);
            tileBody.setBottom(namePlate);

            StackPane card = new StackPane(tileBody);
            card.setPrefSize(tileW, tileH);
            card.setLayoutX(cellX + (cellW - tileW) / 2.0);
            card.setLayoutY(cellY + (cellH - tileH) / 2.0);
            card.setStyle((isRandom
                    ? "-fx-background-color: linear-gradient(to bottom, rgba(122,91,12,0.96), rgba(49,34,4,0.98)); "
                    : "-fx-background-color: linear-gradient(to bottom, rgba(58,72,92,0.82), rgba(14,17,22,0.96)); ")
                    + "-fx-background-radius: 18; -fx-border-color: "
                    + (isRandom ? "rgba(255,236,179,0.88)" : "rgba(255,255,255,0.18)")
                    + "; -fx-border-width: 2; -fx-border-radius: 18;");
            card.setCursor(Cursor.HAND);

            card.setOnMouseClicked(e -> {
                List<TournamentEntry> entries = game.tournamentEntries();
                if (entries.isEmpty()) {
                    return;
                }
                playButtonClick.run();
                int idx = Math.clamp(ui.activeEntryIndex[0], 0, entries.size() - 1);
                game.setTournamentEntrySelection(entries.get(idx), type);
                if (ui.refreshEntries[0] != null) {
                    ui.refreshEntries[0].run();
                }
                if (ui.refreshFocus[0] != null) {
                    ui.refreshFocus[0].run();
                }
            });

            ui.selectionPane.getChildren().add(card);
            BirdIconSpot spot = new BirdIconSpot(type, isRandom, centerX, centerY);
            ui.spots.add(spot);
            if (type != null) {
                ui.spotByType.put(type, spot);
            } else {
                ui.randomSpot = spot;
            }
        }

        ui.selector = new Circle(28);
        ui.selector.setManaged(false);
        ui.selector.getProperties().put(FIGHT_SELECTOR_SNAP_DISTANCE_PROP, Math.min(cellW, cellH) * 0.45);
        ui.selectionPane.getChildren().add(ui.selector);

        ui.selectorGrabHandle = new Circle(42);
        ui.selectorGrabHandle.setManaged(false);
        ui.selectorGrabHandle.setFill(Color.rgb(255, 255, 255, 0.015));
        ui.selectorGrabHandle.setStroke(Color.TRANSPARENT);
        ui.selectionPane.getChildren().add(ui.selectorGrabHandle);

        ui.selectorLabel = new Text("#1");
        ui.selectorLabel.setFont(Font.font("Impact", 18));
        ui.selectorLabel.setManaged(false);
        ui.selectorLabel.setMouseTransparent(true);
        ui.selectionPane.getChildren().add(ui.selectorLabel);

        final double[] dragOffset = new double[2];
        EventHandler<MouseEvent> beginDrag = e -> {
            Point2D local = ui.selectionPane.sceneToLocal(e.getSceneX(), e.getSceneY());
            dragOffset[0] = ui.selectorGrabHandle.getCenterX() - local.getX();
            dragOffset[1] = ui.selectorGrabHandle.getCenterY() - local.getY();
        };
        EventHandler<MouseEvent> dragSelector = e -> {
            Point2D local = ui.selectionPane.sceneToLocal(e.getSceneX(), e.getSceneY());
            moveFightSelectorWithinPane(ui.selectorGrabHandle, ui.selectorLabel, local.getX() + dragOffset[0], local.getY() + dragOffset[1], ui.selectionPane);
            positionFightSelector(ui.selector, ui.selectorLabel, ui.selectorGrabHandle.getCenterX(), ui.selectorGrabHandle.getCenterY());
        };
        EventHandler<MouseEvent> endDrag = e -> {
            BirdIconSpot best = nearestFightSelectorSpot(ui.selector, ui.spots);
            List<TournamentEntry> entries = game.tournamentEntries();
            if (!entries.isEmpty() && best != null) {
                int idx = Math.clamp(ui.activeEntryIndex[0], 0, entries.size() - 1);
                game.setTournamentEntrySelection(entries.get(idx), best.random() ? null : best.type());
                if (ui.refreshEntries[0] != null) {
                    ui.refreshEntries[0].run();
                }
            }
            if (ui.refreshFocus[0] != null) {
                ui.refreshFocus[0].run();
            }
        };
        ui.selector.setOnMousePressed(beginDrag);
        ui.selector.setOnMouseDragged(dragSelector);
        ui.selector.setOnMouseReleased(endDrag);
        ui.selectorGrabHandle.setOnMousePressed(beginDrag);
        ui.selectorGrabHandle.setOnMouseDragged(dragSelector);
        ui.selectorGrabHandle.setOnMouseReleased(endDrag);

        ui.refreshFocus[0] = () -> {
            List<TournamentEntry> entries = game.tournamentEntries();
            if (entries.isEmpty()) {
                return;
            }
            int idx = Math.clamp(ui.activeEntryIndex[0], 0, entries.size() - 1);
            TournamentEntry entry = entries.get(idx);
            BirdType previewType = entry.selectedType;
            Color accent = game.tournamentEntryAccent(idx, entry.human);

            ui.activeSeed.setText("SEED " + game.tournamentEntrySeedNumber(entry));
            ui.activeName.setText(game.tournamentEntryLabel(entry));
            labelFitter.fit(ui.activeName, 26, 16, dockW - 36);
            ui.activeRole.setText(entry.human ? "HUMAN SLOT" : "CPU SLOT");
            ui.activeRole.setStyle("-fx-background-color: " + (entry.human ? "#1565C0" : "#6D4C41")
                    + "; -fx-background-radius: 999;");
            ui.activeBird.setText(previewType == null ? "RANDOM BIRD" : previewType.name.toUpperCase(Locale.ROOT));
            game.drawTournamentPortrait(ui.activePortrait, previewType);

            ui.selector.getProperties().put(FIGHT_SELECTOR_COLOR_PROP, accent);
            ui.selectorLabel.setFill(accent.interpolate(Color.WHITE, 0.22));
            ui.selectorLabel.setText("#" + game.tournamentEntrySeedNumber(entry));
            refreshFightSelectorVisual(ui.selector, true);

            BirdIconSpot spot = previewType == null ? ui.randomSpot : ui.spotByType.get(previewType);
            if (spot != null) {
                positionFightSelector(ui.selector, ui.selectorLabel, spot.cx(), spot.cy());
                positionFightSelector(ui.selectorGrabHandle, ui.selectorLabel, spot.cx(), spot.cy());
            }
        };
    }

    private StackPane buildFooter(Stage stage, TournamentSetupUi ui) {
        Label entrantsStripTitle = new Label("TOURNAMENT FIELD");
        entrantsStripTitle.setFont(Font.font("Arial Black", 20));
        entrantsStripTitle.setTextFill(Color.web("#FFE082"));

        ui.entrantStrip.setAlignment(Pos.TOP_LEFT);
        ui.entrantStrip.setPrefWrapLength(960);
        ui.entrantStrip.setPadding(new Insets(4));
        ui.entrantStrip.setMinWidth(0);

        ScrollPane entrantScroll = new ScrollPane(ui.entrantStrip);
        entrantScroll.setFitToWidth(true);
        entrantScroll.setFitToHeight(true);
        entrantScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        entrantScroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        entrantScroll.setStyle("-fx-background: transparent; -fx-background-color: transparent; -fx-control-inner-background: transparent;");
        entrantScroll.viewportBoundsProperty().addListener((obs, oldBounds, newBounds) ->
                ui.entrantStrip.setPrefWrapLength(Math.max(320.0, newBounds.getWidth() - 22.0)));

        Label entrantsLabel = new Label("ENTRANTS");
        entrantsLabel.setFont(Font.font("Consolas", 14));
        entrantsLabel.setTextFill(Color.web("#CFD8DC"));
        ui.entrantsValue = new Label();
        ui.entrantsValue.setFont(Font.font("Arial Black", 22));
        ui.entrantsValue.setTextFill(Color.WHITE);

        Label humansLabel = new Label("HUMANS");
        humansLabel.setFont(Font.font("Consolas", 14));
        humansLabel.setTextFill(Color.web("#CFD8DC"));
        ui.humansValue = new Label();
        ui.humansValue.setFont(Font.font("Arial Black", 22));
        ui.humansValue.setTextFill(Color.WHITE);
        ui.cpuValue = new Label();
        ui.cpuValue.setFont(Font.font("Consolas", 12));
        ui.cpuValue.setTextFill(Color.web("#FFCCBC"));

        Button entrantsMinus = uiFactory.action("-", 54, 40, 18, "#455A64", 14, () -> {
            int entrantCount = Math.max(2, game.tournamentEntrantCount() - 1);
            game.setTournamentEntrantCount(entrantCount);
            if (game.tournamentHumanCount() > entrantCount) {
                game.setTournamentHumanCount(entrantCount);
            }
            if (ui.refreshAll[0] != null) {
                ui.refreshAll[0].run();
            }
        });
        Button entrantsPlus = uiFactory.action("+", 54, 40, 18, "#455A64", 14, () -> {
            game.setTournamentEntrantCount(Math.min(32, game.tournamentEntrantCount() + 1));
            if (ui.refreshAll[0] != null) {
                ui.refreshAll[0].run();
            }
        });
        Button humansMinus = uiFactory.action("-", 54, 40, 18, "#455A64", 14, () -> {
            game.setTournamentHumanCount(Math.max(0, game.tournamentHumanCount() - 1));
            if (ui.refreshAll[0] != null) {
                ui.refreshAll[0].run();
            }
        });
        Button humansPlus = uiFactory.action("+", 54, 40, 18, "#455A64", 14, () -> {
            game.setTournamentHumanCount(Math.min(game.tournamentEntrantCount(), game.tournamentHumanCount() + 1));
            if (ui.refreshAll[0] != null) {
                ui.refreshAll[0].run();
            }
        });
        buttonNoEllipsis.accept(entrantsMinus);
        buttonNoEllipsis.accept(entrantsPlus);
        buttonNoEllipsis.accept(humansMinus);
        buttonNoEllipsis.accept(humansPlus);

        VBox entrantsBox = new VBox(2,
                entrantsLabel,
                ui.entrantsValue,
                new HBox(8, entrantsMinus, entrantsPlus));
        entrantsBox.setAlignment(Pos.CENTER);
        entrantsBox.setPadding(new Insets(8, 12, 8, 12));
        entrantsBox.setMinWidth(162);
        entrantsBox.setStyle("-fx-background-color: rgba(255,255,255,0.05); -fx-background-radius: 18; "
                + "-fx-border-color: rgba(255,255,255,0.12); -fx-border-width: 2; -fx-border-radius: 18;");

        VBox humansBox = new VBox(2,
                humansLabel,
                ui.humansValue,
                ui.cpuValue,
                new HBox(8, humansMinus, humansPlus));
        humansBox.setAlignment(Pos.CENTER);
        humansBox.setPadding(new Insets(8, 12, 8, 12));
        humansBox.setMinWidth(162);
        humansBox.setStyle("-fx-background-color: rgba(255,255,255,0.05); -fx-background-radius: 18; "
                + "-fx-border-color: rgba(255,255,255,0.12); -fx-border-width: 2; -fx-border-radius: 18;");

        ui.mapModeBtn = uiFactory.action("MAP: RANDOM", 170, 48, 18, "#6D4C41", 12, () -> {
            game.setTournamentMapRandom(!game.isTournamentMapRandom());
            if (ui.refreshMapControls[0] != null) {
                ui.refreshMapControls[0].run();
            }
        });
        ui.mapSelectBtn = uiFactory.action("MAP: " + game.mapDisplayName(game.tournamentFixedMap()), 170, 48, 18, "#00897B", 12, () -> {
            if (game.isTournamentMapRandom()) {
                return;
            }
            List<MapType> maps = game.tournamentMapPool();
            if (maps.isEmpty()) {
                return;
            }
            int idx = maps.indexOf(game.tournamentFixedMap());
            if (idx < 0) {
                idx = 0;
            }
            game.setTournamentFixedMap(maps.get((idx + 1) % maps.size()));
            if (ui.refreshMapControls[0] != null) {
                ui.refreshMapControls[0].run();
            }
        });
        buttonNoEllipsis.accept(ui.mapModeBtn);
        buttonNoEllipsis.accept(ui.mapSelectBtn);

        Button allRandomBtn = uiFactory.action("ALL RANDOM", 170, 48, 18, "#7B1FA2", 12, () -> {
            for (TournamentEntry entry : game.tournamentEntries()) {
                game.setTournamentEntrySelection(entry, null);
            }
            if (ui.refreshEntries[0] != null) {
                ui.refreshEntries[0].run();
            }
            if (ui.refreshFocus[0] != null) {
                ui.refreshFocus[0].run();
            }
        });
        buttonNoEllipsis.accept(allRandomBtn);

        Button shuffleBtn = uiFactory.action("SHUFFLE BRACKET", 170, 48, 18, "#00897B", 12, () -> {
            game.shuffleTournamentSeedOrder();
            if (ui.refreshEntries[0] != null) {
                ui.refreshEntries[0].run();
            }
            if (ui.refreshFocus[0] != null) {
                ui.refreshFocus[0].run();
            }
        });
        buttonNoEllipsis.accept(shuffleBtn);

        ui.startBtn = uiFactory.action("START TOURNAMENT", 350, 70, 22, "#00C853", 18, () -> beginTournament.accept(stage));
        buttonNoEllipsis.accept(ui.startBtn);

        ui.refreshCounts[0] = () -> {
            game.setTournamentEntrantCount(Math.clamp(game.tournamentEntrantCount(), 2, 32));
            game.setTournamentHumanCount(Math.clamp(game.tournamentHumanCount(), 0, game.tournamentEntrantCount()));
            ui.entrantsValue.setText(String.valueOf(game.tournamentEntrantCount()));
            ui.humansValue.setText(String.valueOf(game.tournamentHumanCount()));
            ui.cpuValue.setText("CPU: " + (game.tournamentEntrantCount() - game.tournamentHumanCount()));
            ui.activeEntryIndex[0] = Math.clamp(ui.activeEntryIndex[0], 0, Math.max(0, game.tournamentEntrantCount() - 1));
        };

        ui.refreshMapControls[0] = () -> {
            List<MapType> maps = game.tournamentMapPool();
            if (!maps.contains(game.tournamentFixedMap())) {
                game.setTournamentFixedMap(maps.getFirst());
            }
            ui.mapModeBtn.setText("MAP: " + (game.isTournamentMapRandom() ? "RANDOM" : "CHOOSE"));
            ui.mapSelectBtn.setText("MAP: " + game.mapDisplayName(game.tournamentFixedMap()));
            ui.mapSelectBtn.setDisable(game.isTournamentMapRandom());
            ui.mapSelectBtn.setVisible(!game.isTournamentMapRandom());
            ui.mapSelectBtn.setManaged(!game.isTournamentMapRandom());
            ui.mapSelectBtn.setOpacity(game.isTournamentMapRandom() ? 0.0 : 1.0);
        };

        ui.refreshEntries[0] = () -> {
            game.syncTournamentEntries();
            List<TournamentEntry> entries = game.tournamentEntries();
            ui.activeEntryIndex[0] = Math.clamp(ui.activeEntryIndex[0], 0, Math.max(0, entries.size() - 1));
            ui.entrantStrip.getChildren().clear();
            for (int i = 0; i < entries.size(); i++) {
                TournamentEntry entry = entries.get(i);
                Color accent = game.tournamentEntryAccent(i, entry.human);
                String accentHex = toHex(accent);
                boolean active = i == ui.activeEntryIndex[0];

                Canvas portrait = new Canvas(68, 68);
                BirdType previewType = entry.selectedType;
                game.drawTournamentPortrait(portrait, previewType);

                StackPane portraitFrame = sizedPane(new StackPane(portrait), 76, "-fx-background-color: rgba(0,0,0,0.46); -fx-background-radius: 18; "
                        + "-fx-border-color: rgba(255,255,255,0.18); -fx-border-width: 2; -fx-border-radius: 18;");

                Label seed = new Label("#" + game.tournamentEntrySeedNumber(entry));
                seed.setFont(Font.font("Arial Black", 15));
                seed.setTextFill(Color.web("#111111"));
                StackPane seedChip = new StackPane(seed);
                seedChip.setPadding(new Insets(4, 10, 4, 10));
                seedChip.setStyle("-fx-background-color: #FFE082; -fx-background-radius: 999;");

                Label name = new Label(game.tournamentEntryLabel(entry));
                name.setFont(Font.font("Arial Black", 18));
                name.setTextFill(Color.WHITE);
                name.setMaxWidth(172);
                labelNoEllipsis.accept(name);
                labelFitter.fit(name, 18, 11, 172);

                Label role = new Label(entry.human ? "HUMAN" : "CPU");
                role.setFont(Font.font("Arial Black", 12));
                role.setTextFill(Color.WHITE);
                role.setPadding(new Insets(4, 10, 4, 10));
                role.setStyle("-fx-background-color: " + (entry.human ? "#1565C0" : "#6D4C41")
                        + "; -fx-background-radius: 999;");

                Label bird = new Label(game.tournamentBirdLabel(entry, false));
                bird.setFont(Font.font("Consolas", 14));
                bird.setTextFill(Color.web("#CFD8DC"));
                bird.setMaxWidth(172);
                labelNoEllipsis.accept(bird);
                labelFitter.fit(bird, 14, 10, 172);

                VBox info = new VBox(4, seedChip, name, role, bird);
                info.setAlignment(Pos.CENTER_LEFT);
                HBox body = new HBox(10, portraitFrame, info);
                body.setAlignment(Pos.CENTER_LEFT);
                int selectedIndex = i;

                Button renameChip = new Button();
                renameChip.setGraphic(buildTournamentRenameChipIcon());
                renameChip.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
                renameChip.setMinSize(34, 28);
                renameChip.setPrefSize(34, 28);
                renameChip.setMaxSize(34, 28);
                renameChip.setFocusTraversable(false);
                renameChip.setStyle("-fx-background-color: rgba(12,16,22,0.92); -fx-background-radius: 999; "
                        + "-fx-border-color: rgba(255,224,130,0.75); -fx-border-width: 2; -fx-border-radius: 999;");
                renameChip.setOnAction(e -> {
                    playButtonClick.run();
                    ui.activeEntryIndex[0] = selectedIndex;
                    renameTournamentEntryPrompt(stage, entry, ui.refreshAll[0]);
                });
                renameChip.addEventFilter(MouseEvent.MOUSE_CLICKED, MouseEvent::consume);

                StackPane card = new StackPane(body, renameChip);
                card.setPadding(new Insets(12));
                card.setMinWidth(272);
                card.setPrefWidth(272);
                card.setMaxWidth(272);
                card.setStyle("-fx-background-color: linear-gradient(to bottom right, "
                        + accentHex + ", #0C0E12); "
                        + "-fx-background-radius: 24; -fx-border-color: "
                        + (active ? "#FFE082" : "rgba(255,255,255,0.16)")
                        + "; -fx-border-width: " + (active ? "3.5" : "2.0") + "; -fx-border-radius: 24;");
                card.setCursor(Cursor.HAND);
                StackPane.setAlignment(renameChip, Pos.TOP_RIGHT);
                StackPane.setMargin(renameChip, new Insets(2, 2, 0, 0));

                card.setOnMouseClicked(e -> {
                    playButtonClick.run();
                    ui.activeEntryIndex[0] = selectedIndex;
                    if (ui.refreshEntries[0] != null) {
                        ui.refreshEntries[0].run();
                    }
                    if (ui.refreshFocus[0] != null) {
                        ui.refreshFocus[0].run();
                    }
                });
                ui.entrantStrip.getChildren().add(card);
            }
        };

        ui.refreshAll[0] = () -> {
            if (ui.refreshCounts[0] != null) {
                ui.refreshCounts[0].run();
            }
            if (ui.refreshMapControls[0] != null) {
                ui.refreshMapControls[0].run();
            }
            if (ui.refreshEntries[0] != null) {
                ui.refreshEntries[0].run();
            }
            if (ui.refreshFocus[0] != null) {
                ui.refreshFocus[0].run();
            }
        };

        FlowPane utilityButtons = new FlowPane(10, 10,
                ui.mapModeBtn,
                ui.mapSelectBtn,
                shuffleBtn,
                allRandomBtn
        );
        utilityButtons.setAlignment(Pos.TOP_CENTER);
        utilityButtons.setPrefWrapLength(350);

        VBox actionPanel = new VBox(12, utilityButtons, ui.startBtn);
        actionPanel.setAlignment(Pos.TOP_CENTER);
        actionPanel.setMinWidth(362);
        actionPanel.setPrefWidth(362);
        actionPanel.setMaxWidth(362);

        Region headerSpacer = new Region();
        HBox.setHgrow(headerSpacer, Priority.ALWAYS);
        HBox counters = new HBox(10, entrantsBox, humansBox);
        counters.setAlignment(Pos.CENTER_RIGHT);
        HBox headerRow = new HBox(14, entrantsStripTitle, headerSpacer, counters);
        headerRow.setAlignment(Pos.CENTER_LEFT);

        HBox centerRow = new HBox(16, entrantScroll, actionPanel);
        centerRow.setAlignment(Pos.TOP_LEFT);
        HBox.setHgrow(entrantScroll, Priority.ALWAYS);

        BorderPane footerBody = new BorderPane();
        footerBody.setTop(headerRow);
        footerBody.setCenter(centerRow);
        BorderPane.setMargin(headerRow, new Insets(0, 0, 10, 0));
        entrantScroll.setMinHeight(206);
        entrantScroll.setPrefViewportHeight(206);

        StackPane footerPanel = new StackPane(footerBody);
        footerPanel.setPadding(new Insets(10));
        footerPanel.setMinHeight(292);
        footerPanel.setPrefHeight(292);
        footerPanel.setStyle("-fx-background-color: rgba(4,5,8,0.92); -fx-background-radius: 28; "
                + "-fx-border-color: rgba(255,255,255,0.12); -fx-border-width: 3; -fx-border-radius: 28;");
        return footerPanel;
    }

    private void renameTournamentEntryPrompt(Stage stage, TournamentEntry entry, Runnable onComplete) {
        if (entry == null) {
            if (onComplete != null) {
                onComplete.run();
            }
            return;
        }
        String defaultName = entry.customName == null || entry.customName.isBlank()
                ? "PLAYER " + entry.id
                : entry.customName;
        TextInputDialog dialog = new TextInputDialog(defaultName);
        dialog.setTitle("Rename Tournament Entrant");
        dialog.setHeaderText("Rename this tournament entrant.");
        dialog.setContentText("Entrant name:");
        dialog.initOwner(stage);
        dialog.showAndWait().ifPresent(name -> {
            entry.customName = normalizeTournamentEntryName(name);
            if (onComplete != null) {
                onComplete.run();
            }
        });
    }

    private Node buildTournamentRenameChipIcon() {
        Rectangle shaft = new Rectangle(10, 3, Color.web("#FFE082"));
        shaft.setArcWidth(2);
        shaft.setArcHeight(2);
        shaft.setTranslateX(-1.5);
        shaft.setTranslateY(0.5);
        shaft.setRotate(-32);

        Polygon tip = new Polygon(
                0.0, 0.0,
                4.0, 1.5,
                0.5, 4.5
        );
        tip.setFill(Color.web("#FFF8E1"));
        tip.setTranslateX(5.2);
        tip.setTranslateY(-2.4);
        tip.setRotate(-32);

        Rectangle eraser = new Rectangle(3.2, 3.0, Color.web("#F48FB1"));
        eraser.setArcWidth(1.5);
        eraser.setArcHeight(1.5);
        eraser.setTranslateX(-5.2);
        eraser.setTranslateY(3.0);
        eraser.setRotate(-32);

        Group icon = new Group(shaft, tip, eraser);
        icon.setManaged(false);
        return icon;
    }

    private String normalizeTournamentEntryName(String requestedName) {
        if (requestedName == null) {
            return "";
        }
        String trimmed = requestedName.trim();
        if (trimmed.isEmpty()) {
            return "";
        }
        String shortened = trimmed.length() > 32 ? trimmed.substring(0, 32).trim() : trimmed;
        return shortened.isEmpty() ? "" : shortened;
    }

    private void bindFixedFrameScale(Scene scene, Node content) {
        Runnable apply = () -> {
            double availW = Math.max(1.0, scene.getWidth() - 16.0);
            double availH = Math.max(1.0, scene.getHeight() - 16.0);
            double scale = Math.min(availW / LAYOUT_WIDTH, availH / LAYOUT_HEIGHT);
            content.setScaleX(scale);
            content.setScaleY(scale);
        };
        scene.widthProperty().addListener((obs, oldVal, newVal) -> apply.run());
        scene.heightProperty().addListener((obs, oldVal, newVal) -> apply.run());
        Platform.runLater(apply);
    }

    private void positionFightSelector(Circle selector, Text label, double x, double y) {
        if (selector == null || label == null) {
            return;
        }
        selector.setCenterX(x);
        selector.setCenterY(y);
        label.applyCss();
        label.setX(x - label.getLayoutBounds().getWidth() / 2.0);
        label.setY(y + 6.0);
    }

    private void moveFightSelectorWithinPane(Circle selector, Text label, double x, double y, Pane selectionPane) {
        if (selector == null || label == null || selectionPane == null) {
            return;
        }
        double boundW = Math.max(selectionPane.getWidth(), selectionPane.getPrefWidth());
        double boundH = Math.max(selectionPane.getHeight(), selectionPane.getPrefHeight());
        double nx = Math.clamp(x, FIGHT_SELECTOR_BOUND_MARGIN, boundW - FIGHT_SELECTOR_BOUND_MARGIN);
        double ny = Math.clamp(y, FIGHT_SELECTOR_BOUND_MARGIN, boundH - FIGHT_SELECTOR_BOUND_MARGIN);
        positionFightSelector(selector, label, nx, ny);
    }

    private BirdIconSpot nearestFightSelectorSpot(Circle selector, List<BirdIconSpot> spots) {
        if (selector == null || spots == null || spots.isEmpty()) {
            return null;
        }
        BirdIconSpot best = null;
        double bestDist = Double.MAX_VALUE;
        for (BirdIconSpot spot : spots) {
            if (spot == null) {
                continue;
            }
            double dx = selector.getCenterX() - spot.cx();
            double dy = selector.getCenterY() - spot.cy();
            double dist = dx * dx + dy * dy;
            if (dist < bestDist) {
                bestDist = dist;
                best = spot;
            }
        }
        Object snapDistance = selector.getProperties().get(FIGHT_SELECTOR_SNAP_DISTANCE_PROP);
        if (snapDistance instanceof Number number) {
            double maxDistance = number.doubleValue();
            if (maxDistance > 0.0 && bestDist > maxDistance * maxDistance) {
                return null;
            }
        }
        return best;
    }

    private void refreshFightSelectorVisual(Circle selector, boolean locked) {
        if (selector == null) {
            return;
        }
        Object raw = selector.getProperties().get(FIGHT_SELECTOR_COLOR_PROP);
        Color accent = raw instanceof Color color ? color : Color.web("#FFD54F");
        selector.setFill(accent.deriveColor(0.0, 1.0, locked ? 0.82 : 0.64, locked ? 0.28 : 0.16));
        selector.setStroke(accent.interpolate(Color.WHITE, locked ? 0.24 : 0.08));
        selector.setStrokeWidth(locked ? 4.5 : 3.0);
    }

    private static StackPane sizedPane(StackPane pane, double size, String style) {
        pane.setMinSize(size, size);
        pane.setPrefSize(size, size);
        pane.setMaxSize(size, size);
        pane.setStyle(style);
        return pane;
    }

    private String toHex(Color color) {
        return String.format("#%02X%02X%02X",
                (int) (color.getRed() * 255),
                (int) (color.getGreen() * 255),
                (int) (color.getBlue() * 255));
    }

    private static final class TournamentSetupUi {
        final int[] activeEntryIndex = new int[]{0};
        final Runnable[] refreshCounts = new Runnable[1];
        final Runnable[] refreshEntries = new Runnable[1];
        final Runnable[] refreshMapControls = new Runnable[1];
        final Runnable[] refreshFocus = new Runnable[1];
        final Runnable[] refreshAll = new Runnable[1];
        final Pane selectionPane = new Pane();
        final List<BirdIconSpot> spots = new ArrayList<>();
        final Map<BirdType, BirdIconSpot> spotByType = new HashMap<>();
        final FlowPane entrantStrip = new FlowPane(12, 12);
        BirdIconSpot randomSpot;
        Circle selector;
        Circle selectorGrabHandle;
        Text selectorLabel;
        Label activeSeed;
        Label activeName;
        Label activeRole;
        Canvas activePortrait;
        Label activeBird;
        Label entrantsValue;
        Label humansValue;
        Label cpuValue;
        Button mapModeBtn;
        Button mapSelectBtn;
        Button startBtn;
        Button backBtn;
    }
}
