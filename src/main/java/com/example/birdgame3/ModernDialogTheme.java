package com.example.birdgame3;

import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;
import javafx.scene.control.TextInputDialog;

import java.net.URL;
import java.util.Objects;

/** Shared visual treatment for the few workflows that must use a modal JavaFX dialog. */
final class ModernDialogTheme {
    private ModernDialogTheme() {
    }

    static <T> Dialog<T> apply(Dialog<T> dialog) {
        if (dialog == null) return null;

        DialogPane pane = dialog.getDialogPane();
        pane.setGraphic(null);
        pane.setMinWidth(620);
        pane.setPrefWidth(700);
        URL stylesheetUrl = Objects.requireNonNull(
                ModernDialogTheme.class.getResource("modern-dialog.css"),
                "Missing modern-dialog.css");
        String stylesheet = stylesheetUrl.toExternalForm();
        if (!pane.getStylesheets().contains(stylesheet)) {
            pane.getStylesheets().add(stylesheet);
        }
        if (dialog instanceof TextInputDialog textInputDialog) {
            textInputDialog.getEditor().setPrefWidth(520);
            textInputDialog.getEditor().selectAll();
        }
        return dialog;
    }
}
