package com.example.birdgame3;

import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Label;
import javafx.scene.control.OverrunStyle;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;

/** The reward screen's visual shell; no profile, audio, simulation, or reward mutations. */
final class RewardRevealView {
    static final double WIDTH = 1600;
    static final double HEIGHT = 950;
    static final Box FOOTER = new Box(72, 840, 1456, 82);

    record Box(double x, double y, double width, double height) { }
    record Layout(Box art, Box name, Box category, Box detail, boolean centered) { }

    private RewardRevealView() { }

    static Layout layout(RewardPresentation.Kind kind) {
        return switch (kind) {
            case BIRD -> new Layout(new Box(800, 165, 720, 630),
                    new Box(88, 244, 672, 236), new Box(92, 506, 654, 74),
                    new Box(94, 614, 614, 120), false);
            case STAGE -> new Layout(new Box(88, 222, 936, 527),
                    new Box(1070, 305, 446, 190), new Box(1074, 235, 438, 46),
                    new Box(1074, 534, 438, 172), false);
            default -> new Layout(new Box(530, 166, 540, 430),
                    new Box(140, 645, 1320, 82), new Box(540, 601, 520, 34),
                    new Box(200, 740, 1200, 64), true);
        };
    }

    static AnchorPane build(RewardPresentation reward, Node art, Node footer, String queueLabel) {
        Layout layout = layout(reward.kind());
        AnchorPane frame = frame(reward.kind());
        Label headline = label(reward.kind().headline, 58, 1180, 90, Color.web("#FFE67B"), false);
        place(frame, headline, new Box(78, 30, 1180, 90));
        Label count = label(queueLabel, 19, 256, 54, Color.web("#BFCBD5"), false);
        count.setAlignment(Pos.CENTER_RIGHT);
        place(frame, count, new Box(1264, 45, 256, 54));

        StackPane artwork = new StackPane(art);
        artwork.setId("reward-art");
        artwork.setMouseTransparent(true);
        place(frame, artwork, layout.art());
        if (reward.kind() == RewardPresentation.Kind.STAGE) {
            artwork.setStyle("-fx-border-color: #FFF3C4; -fx-border-width: 4; -fx-padding: 8;"
                    + "-fx-background-color: #081A21;");
        }
        Label name = label(reward.name(), reward.kind() == RewardPresentation.Kind.BIRD ? 88 : 56,
                layout.name().width(), layout.name().height(), Color.WHITE, layout.centered());
        name.setId("reward-name");
        place(frame, name, layout.name());
        Label category = label(reward.kind().category, reward.kind() == RewardPresentation.Kind.BIRD ? 43 : 25,
                layout.category().width(), layout.category().height(), Color.web("#FFE67B"), layout.centered());
        category.setId("reward-category");
        place(frame, category, layout.category());
        Label detail = label(reward.detail(), 24, layout.detail().width(), layout.detail().height(),
                Color.web("#DCE6ED"), layout.centered());
        detail.setId("reward-detail");
        place(frame, detail, layout.detail());
        place(frame, footer, FOOTER);
        return frame;
    }

    static AnchorPane frame(RewardPresentation.Kind kind) {
        AnchorPane frame = new AnchorPane();
        frame.setId("uiFrame");
        frame.setMinSize(WIDTH, HEIGHT);
        frame.setPrefSize(WIDTH, HEIGHT);
        frame.setMaxSize(WIDTH, HEIGHT);
        frame.getChildren().add(backdrop(kind));
        return frame;
    }

    /** Fitted before layout, including wrapped height. Never substitute ellipses for a reward name. */
    static Label label(String text, double preferredSize, double width, double height,
                       Color color, boolean centered) {
        String value = text == null ? "" : text;
        Font font = fittedFont(value, preferredSize, width - 12, height - 8);
        Label label = new Label(value);
        label.setFont(font);
        label.setTextFill(color);
        label.setWrapText(true);
        label.setTextOverrun(OverrunStyle.CLIP);
        label.setEllipsisString("");
        label.setTextAlignment(centered ? TextAlignment.CENTER : TextAlignment.LEFT);
        label.setAlignment(centered ? Pos.CENTER : Pos.CENTER_LEFT);
        label.setMinSize(width, height);
        label.setPrefSize(width, height);
        label.setMaxSize(width, height);
        return label;
    }

    static Font fittedFont(String text, double preferredSize, double width, double height) {
        double size = preferredSize;
        Text probe = new Text(text);
        while (true) {
            Font font = Font.font("Arial Black", size);
            probe.setFont(font);
            probe.setWrappingWidth(Math.max(1, width));
            boolean fits = probe.getLayoutBounds().getHeight() <= height;
            // An unbroken skin name must fit too; wrapping can't conceal an oversized word.
            for (String word : text.split("\\s+")) {
                Text wordProbe = new Text(word);
                wordProbe.setFont(font);
                fits &= wordProbe.getLayoutBounds().getWidth() <= width;
            }
            if (fits || size <= 8) return font;
            size = Math.max(8, size - 1);
        }
    }

    static void place(AnchorPane frame, Node child, Box box) {
        if (child instanceof Region region) {
            region.setMinSize(box.width(), box.height());
            region.setPrefSize(box.width(), box.height());
            region.setMaxSize(box.width(), box.height());
        }
        child.relocate(box.x(), box.y());
        frame.getChildren().add(child);
    }

    private static Canvas backdrop(RewardPresentation.Kind kind) {
        Canvas canvas = new Canvas(WIDTH, HEIGHT);
        canvas.setMouseTransparent(true);
        GraphicsContext g = canvas.getGraphicsContext2D();
        Color accent = Color.web(kind.accent);
        g.setFill(Color.web("#070B12"));
        g.fillRect(0, 0, WIDTH, HEIGHT);
        g.setFill(new LinearGradient(0, 0, 1, 0, true, CycleMethod.NO_CYCLE,
                new Stop(0, accent.deriveColor(0, 1, .28, 1)), new Stop(1, accent)));
        g.fillPolygon(new double[]{0, WIDTH, WIDTH, 0}, new double[]{174, 128, 808, 824}, 4);
        // Clipped graphic texture, not a cloud of particles or a perpetual animation.
        g.save();
        g.beginPath();
        g.moveTo(0, 174); g.lineTo(WIDTH, 128); g.lineTo(WIDTH, 808); g.lineTo(0, 824);
        g.closePath(); g.clip();
        g.setStroke(Color.color(1, 1, 1, .06));
        g.setLineWidth(2);
        for (int x = -950; x < 1750; x += 26) g.strokeLine(x, 950, x + 780, 0);
        g.setFill(Color.color(0, 0, 0, .09));
        for (int x = 1060; x < 1600; x += 30) {
            for (int y = 162; y < 820; y += 30) {
                double r = 2 + (x - 1060) / 80.0;
                g.fillOval(x - r, y - r, r * 2, r * 2);
            }
        }
        double cx = kind == RewardPresentation.Kind.BIRD ? 1158 : 800;
        double cy = kind == RewardPresentation.Kind.BIRD ? 472 : 400;
        // Broad wing-shaped rays frame the art without crossing the title/footer.
        g.setFill(Color.color(1, 1, 1, .08));
        for (int i = 0; i < 9; i++) {
            double a = i * Math.PI * 2 / 9;
            g.fillPolygon(new double[]{cx, cx + Math.cos(a) * 850, cx + Math.cos(a + .075) * 850},
                    new double[]{cy, cy + Math.sin(a) * 850, cy + Math.sin(a + .075) * 850}, 3);
        }
        g.restore();
        g.setFill(Color.web("#FFDB61"));
        g.fillPolygon(new double[]{0, WIDTH, WIDTH, 0}, new double[]{168, 122, 130, 176}, 4);
        g.setFill(Color.web("#EAF2F8", .22));
        g.fillRect(72, 825, 1456, 2);
        return canvas;
    }
}
