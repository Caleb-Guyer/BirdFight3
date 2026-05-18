package com.example.birdgame3;

import java.util.function.Consumer;

import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.text.Font;
import javafx.scene.text.TextAlignment;

public final class UIFactory {
    @FunctionalInterface
    public interface ButtonFitter {
        void fit(Button button, double maxSize, double minSize);
    }

    private final Runnable clickSound;
    private final Consumer<Button> noEllipsis;
    private final ButtonFitter fitter;

    public UIFactory(Runnable clickSound, Consumer<Button> noEllipsis, ButtonFitter fitter) {
        this.clickSound = clickSound;
        this.noEllipsis = noEllipsis;
        this.fitter = fitter;
    }

    public Button action(String text, double width, double height, double fontSize, String bgColor, double radius, Runnable onAction) {
        Button b = new Button(text);
        b.setPrefSize(width, height);
        b.setMinSize(width, height);
        b.setMaxSize(width, height);
        b.setFont(Font.font("Arial Black", fontSize));
        b.setWrapText(text != null && text.contains("\n"));
        b.setTextAlignment(TextAlignment.CENTER);
        b.setAlignment(Pos.CENTER);
        b.setStyle(MenuTheme.buttonStyle(bgColor, radius));
        if (noEllipsis != null) {
            noEllipsis.accept(b);
        }
        if (onAction != null) {
            b.setOnAction(e -> {
                if (clickSound != null) {
                    clickSound.run();
                }
                onAction.run();
            });
        }
        return b;
    }

    public void fitSingleLineOnLayout(Button b, double maxSize, double minSize) {
        if (fitter == null) return;
        Platform.runLater(() -> fitter.fit(b, maxSize, minSize));
    }
}
