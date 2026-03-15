package com.example.birdgame3;

import java.util.function.Consumer;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.TextAlignment;

public final class MenuLayout {
    private MenuLayout() {
    }

    public static VBox buildMenuRoot(String backgroundStyle, Insets padding, double spacing) {
        VBox root = new VBox(spacing);
        root.setAlignment(Pos.CENTER);
        root.setPadding(padding);
        root.setStyle(backgroundStyle);
        return root;
    }

    public static void styleMenuMessage(Label label, int fontSize, String color, double maxWidth, Consumer<Label> noEllipsis) {
        label.setFont(Font.font("Consolas", fontSize));
        label.setTextFill(Color.web(color));
        label.setWrapText(true);
        label.setMaxWidth(maxWidth);
        label.setTextAlignment(TextAlignment.CENTER);
        label.setAlignment(Pos.CENTER);
        if (noEllipsis != null) {
            noEllipsis.accept(label);
        }
    }
}
