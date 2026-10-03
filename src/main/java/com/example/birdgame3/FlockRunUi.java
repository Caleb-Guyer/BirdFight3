package com.example.birdgame3;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.TextAlignment;
import java.util.Locale;
import java.util.function.Consumer;
import java.util.function.Function;

/** Shares the Classic menu chrome, artwork, panels, and buttons. */
final class FlockRunUi {
    record Actions(Runnable newRun, Runnable resume, Runnable abandon,
                   Consumer<FlockRunState.Route> route, Consumer<FlockRunState.Perk> perk,
                   Function<FlockRunState.Route, String> encounterLabel) { }
    record Art(Node portrait, Node routeStrip, Function<FlockRunState.Route, Node> routeArt) { }
    record Page(BorderPane root, Button initialFocus, Button footerAction) { }

    static Page build(FlockRunState run, UIFactory buttons, Art art, Actions actions) {
        BorderPane root = new BorderPane();
        root.setId("flockRunPage");
        root.setPadding(new Insets(14));
        root.setStyle(MenuTheme.pageBackground());
        StackPane portrait = new StackPane(art.portrait());
        portrait.setPrefSize(260, 158);
        portrait.setMinSize(260, 158);
        portrait.setStyle(MenuTheme.insetPanelStyle("#B5121B", 18));
        Label bird = heading(run.bird.name.toUpperCase(Locale.ROOT), 28, "#FFFFFF");
        Label chapter = caption(run.finished() ? "MIGRATION RESULTS" : "THE BROKEN MIGRATION", 17, "#FFE082");
        HBox stats = new HBox(12, chip(Math.round(run.health()) + " / 112 HP", "#194E35", "#A5D6A7"),
                chip(run.score() + " POINTS", "#4E3B00", "#FFE45C"),
                chip(run.encounter() + " / 8 CLEARED", "#123044", "#90CAF9"));
        stats.setAlignment(Pos.CENTER_LEFT);
        VBox identity = new VBox(8, chapter, bird, stats);
        HBox.setHgrow(identity, Priority.ALWAYS);
        VBox route = new VBox(12, caption("YOUR ROUTE", 16, "#FFE082"), art.routeStrip());
        route.setAlignment(Pos.CENTER);
        HBox hero = new HBox(24, portrait, identity, route);
        hero.setAlignment(Pos.CENTER_LEFT);
        hero.setPadding(new Insets(12, 22, 12, 22));
        hero.setPrefWidth(1520);
        hero.setMaxWidth(1520);
        hero.setStyle(MenuTheme.panelStyle("#F8C528", 22));

        VBox content = new VBox(12);
        content.setAlignment(Pos.TOP_CENTER);
        content.setPadding(new Insets(12, 0, 6, 0));
        content.getChildren().add(hero);
        Button focus;
        Button footer;
        if (run.finished()) {
            boolean won = run.phase() == FlockRunState.Phase.WON;
            VBox results = new VBox(12, medalArt(run.medal()),
                    heading(won ? run.medal() + " MEDAL" : "RUN ENDED", 42, won ? "#FFE45C" : "#FF8A80"),
                    caption(run.score() + " POINTS  ·  " + formatTime(run.ticks()), 22, "#FFFFFF"),
                    caption(won ? "Bronze: clear  ·  Silver: 1,100  ·  Gold: 1,550"
                            : "Choose a fighter and begin a new migration.", 19, "#CFD8DC"));
            results.setAlignment(Pos.CENTER);
            results.setPadding(new Insets(20));
            results.setPrefSize(1520, 370);
            results.setMaxWidth(1520);
            results.setStyle(MenuTheme.panelStyle(won ? "#F8C528" : "#B5121B", 22));
            content.getChildren().add(results);
            footer = buttons.action("NEW RUN", 280, 60, 24, "#00C853", 18, actions.newRun());
            focus = footer;
        } else {
            String prompt = switch (run.phase()) {
                case ROUTE -> run.encounter() == 7 ? "FINAL ASCENT  ·  " + run.boss().title.toUpperCase(Locale.ROOT)
                        : "CHOOSE ENCOUNTER " + (run.encounter() + 1);
                case BATTLE -> "RESUME ENCOUNTER " + (run.encounter() + 1);
                case REWARD -> "CHOOSE A PERK  ·  " + run.picksRemaining() + " REMAINING";
                default -> "";
            };
            content.getChildren().add(heading(prompt, 27, "#FFE45C"));
            HBox choices = new HBox(20);
            choices.setAlignment(Pos.CENTER);
            if (run.phase() == FlockRunState.Phase.ROUTE) {
                for (FlockRunState.Route choice : run.routes()) choices.getChildren().add(card(buttons,
                        choice.title, choice.description + "\n" + actions.encounterLabel().apply(choice),
                        choice.color, art.routeArt().apply(choice), () -> actions.route().accept(choice)));
            } else if (run.phase() == FlockRunState.Phase.REWARD) {
                for (FlockRunState.Perk perk : run.offeredPerks()) choices.getChildren().add(card(buttons,
                        perk.title, perk.description + "\nRANK " + run.rank(perk) + " → " + (run.rank(perk) + 1) + " / 3",
                        perk.color, perkArt(perk), () -> actions.perk().accept(perk)));
            } else {
                choices.getChildren().add(card(buttons, "RESUME ENCOUNTER",
                        "Restore this encounter's opening health and perks.\n" + actions.encounterLabel().apply(run.route()),
                        "#80CBC4", art.routeArt().apply(run.route()), actions.resume()));
            }
            focus = (Button) choices.getChildren().getFirst();
            content.getChildren().add(choices);
            HBox perks = new HBox(10);
            perks.setAlignment(Pos.CENTER);
            for (FlockRunState.Perk perk : FlockRunState.Perk.values()) perks.getChildren().add(chip(
                    perk.title + " " + run.rank(perk) + "/3", "#263238", run.rank(perk) > 0 ? perk.color : "#78909C"));
            content.getChildren().addAll(perks, caption("VICTORY: +" + (6 + 8 * run.rank(FlockRunState.Perk.MEDIC))
                    + " HP  ·  ROOSTS USED: " + run.rests() + "/2  ·  DEFEAT OR TIMEOUT ENDS THE RUN", 17, "#CFD8DC"));
            footer = buttons.action("END RUN", 250, 60, 22, "#B5121B", 18, actions.abandon());
        }
        root.setCenter(content);
        return new Page(root, focus, footer);
    }

    private static Button card(UIFactory buttons, String title, String description, String accent, Node art, Runnable action) {
        Button card = buttons.action("", 470, 326, 23, "#263238", 22, action);
        VBox graphic = new VBox(10, art, heading(title.toUpperCase(Locale.ROOT), 24, "#FFFFFF"),
                caption(description, 17, "#CFD8DC"));
        graphic.setAlignment(Pos.TOP_CENTER);
        graphic.setMaxWidth(422);
        graphic.setMouseTransparent(true);
        card.setGraphic(graphic);
        card.setStyle(MenuTheme.panelStyle(accent, 22) + "-fx-padding: 16; -fx-alignment: top-center;");
        card.setAccessibleText(title + ". " + description);
        return card;
    }

    private static Label chip(String text, String base, String accent) {
        Label label = caption(text, 15, accent);
        label.setPadding(new Insets(9, 12, 9, 12));
        label.setStyle(MenuTheme.chipStyle(base, accent, 12));
        return label;
    }
    private static Label heading(String text, int size, String color) {
        Label label = caption(text, size, color);
        label.setFont(Font.font("Arial Black", size));
        return label;
    }
    private static Label caption(String text, int size, String color) {
        Label label = new Label(text);
        label.setFont(Font.font("Consolas", FontWeight.BOLD, size));
        label.setTextFill(Color.web(color));
        label.setWrapText(true);
        label.setTextAlignment(TextAlignment.CENTER);
        label.setAlignment(Pos.CENTER);
        label.setMinHeight(Region.USE_PREF_SIZE);
        return label;
    }

    private static Canvas perkArt(FlockRunState.Perk perk) {
        Canvas canvas = new Canvas(422, 130);
        GraphicsContext g = canvas.getGraphicsContext2D();
        g.setFill(Color.web(perk.color, 0.10));
        g.fillRoundRect(0, 0, 422, 130, 18, 18);
        g.translate(161, 15);
        g.setFill(Color.web(perk.color));
        g.setStroke(Color.web(perk.color));
        g.setLineWidth(7);
        switch (perk) {
            case TALONS -> { for (int i = 0; i < 3; i++) g.strokeLine(28 + i * 22, 15, 8 + i * 22, 85); }
            case GUARD -> {
                g.strokePolygon(new double[]{50, 85, 80, 50, 20, 15}, new double[]{8, 23, 60, 91, 60, 23}, 6);
                g.strokeLine(50, 28, 50, 67);
            }
            case WIND -> { for (int i = 0; i < 3; i++) { g.strokeLine(8 + i * 7, 25 + i * 24, 84, 25 + i * 24); g.strokeLine(70, 13 + i * 24, 84, 25 + i * 24); } }
            case QUICKEN -> { g.strokeOval(13, 13, 74, 74); g.strokeLine(50, 28, 50, 50); g.strokeLine(50, 50, 72, 61); }
            case SPIRIT -> g.fillPolygon(new double[]{50, 30, 13, 27, 50, 77, 87, 66, 62}, new double[]{3, 37, 57, 84, 96, 86, 63, 34, 60}, 9);
            case MEDIC -> { g.fillRoundRect(38, 13, 24, 74, 6, 6); g.fillRoundRect(13, 38, 74, 24, 6, 6); }
        }
        return canvas;
    }

    private static Canvas medalArt(FlockRunState.Medal medal) {
        Canvas canvas = new Canvas(144, 144);
        GraphicsContext g = canvas.getGraphicsContext2D();
        g.setFill(Color.web("#B5121B"));
        g.fillPolygon(new double[]{39, 60, 53, 26}, new double[]{77, 84, 141, 125}, 4);
        g.fillPolygon(new double[]{85, 106, 118, 90}, new double[]{84, 77, 125, 141}, 4);
        Color color = Color.web(switch (medal) { case GOLD -> "#FFE45C"; case SILVER -> "#CFD8DC"; case BRONZE -> "#CD8C55"; case NONE -> "#78909C"; });
        g.setFill(color.darker()); g.fillOval(19, 8, 106, 106);
        g.setFill(color); g.fillOval(27, 16, 90, 90);
        g.setFill(Color.web("#111317"));
        double[] xs = new double[10], ys = new double[10];
        for (int i = 0; i < 10; i++) {
            double angle = -Math.PI / 2 + i * Math.PI / 5;
            double radius = i % 2 == 0 ? 32 : 14;
            xs[i] = 72 + Math.cos(angle) * radius;
            ys[i] = 61 + Math.sin(angle) * radius;
        }
        g.fillPolygon(xs, ys, 10);
        return canvas;
    }
    private static String formatTime(long ticks) {
        return String.format(Locale.ROOT, "%d:%02d", ticks / 3600, ticks / 60 % 60);
    }
}
