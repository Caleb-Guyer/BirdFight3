package com.example.birdgame3;

import javafx.scene.control.Button;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonBar.ButtonData;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;
import javafx.scene.control.OverrunStyle;
import javafx.scene.control.TextInputDialog;
import javafx.scene.text.Font;
import javafx.scene.text.Text;

import java.net.URL;
import java.util.Objects;
import java.util.List;

/** Shared visual treatment for the few workflows that must use a modal JavaFX dialog. */
final class ModernDialogTheme {
    private static final double ACTION_BUTTON_MIN_WIDTH = 128;

    private ModernDialogTheme() {
    }

    static <T> Dialog<T> apply(Dialog<T> dialog) {
        if (dialog == null) return null;

        DialogPane pane = dialog.getDialogPane();
        pane.setGraphic(null);
        pane.setMinWidth(620);
        pane.setPrefWidth(760);
        URL stylesheetUrl = Objects.requireNonNull(
                ModernDialogTheme.class.getResource("modern-dialog.css"),
                "Missing modern-dialog.css");
        String stylesheet = stylesheetUrl.toExternalForm();
        if (!pane.getStylesheets().contains(stylesheet)) {
            pane.getStylesheets().add(stylesheet);
        }
        pane.getButtonTypes().forEach(buttonType -> {
            if (pane.lookupButton(buttonType) instanceof Button button) {
                // ButtonBar's uniform sizing can leave only a few pixels for text after
                // padding at fractional Windows display scales. Reserve real content
                // space and never turn short actions such as "Yes" into "Y...".
                double width = actionWidth(button.getText());
                button.setMinWidth(width);
                button.setPrefWidth(width);
                button.setMinHeight(60);
                button.setTextOverrun(OverrunStyle.CLIP);
                button.setEllipsisString("");
            }
        });
        ButtonType safeDefault = safeDefault(dialog instanceof Alert alert
                && alert.getAlertType() == Alert.AlertType.CONFIRMATION, pane.getButtonTypes());
        if (safeDefault != null) {
            pane.getButtonTypes().forEach(type -> {
                if (pane.lookupButton(type) instanceof Button button) button.setDefaultButton(type == safeDefault);
            });
        }
        var previousShown = dialog.getOnShown();
        dialog.setOnShown(event -> {
            if (previousShown != null) previousShown.handle(event);
            focusSafeAction(dialog);
        });
        if (dialog instanceof TextInputDialog textInputDialog) {
            textInputDialog.getEditor().setPrefWidth(520);
            textInputDialog.getEditor().selectAll();
        }
        return dialog;
    }

    static double actionWidth(String label) {
        Text measurement = new Text(label == null ? "" : label);
        measurement.setFont(Font.font("Arial Black", 20));
        return Math.max(ACTION_BUTTON_MIN_WIDTH, Math.ceil(measurement.getLayoutBounds().getWidth()) + 72);
    }

    static ButtonType safeDefault(boolean confirmation, List<ButtonType> types) {
        if (!confirmation) return null; // Text inputs still submit with Enter.
        return types.stream().filter(type -> type.getButtonData() == ButtonData.NO).findFirst()
                .orElseGet(() -> types.stream().filter(type -> type.getButtonData().isCancelButton())
                        .findFirst().orElse(null));
    }

    static void focusSafeAction(Dialog<?> dialog) {
        ButtonType safe = safeDefault(dialog instanceof Alert alert
                && alert.getAlertType() == Alert.AlertType.CONFIRMATION, dialog.getDialogPane().getButtonTypes());
        if (safe != null) dialog.getDialogPane().lookupButton(safe).requestFocus();
    }
}
