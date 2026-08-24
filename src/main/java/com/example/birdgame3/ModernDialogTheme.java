package com.example.birdgame3;

import javafx.scene.control.Button;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;
import javafx.scene.control.OverrunStyle;
import javafx.scene.control.TextInputDialog;

import java.net.URL;
import java.util.Objects;

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
        pane.setPrefWidth(700);
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
                button.setMinWidth(ACTION_BUTTON_MIN_WIDTH);
                button.setTextOverrun(OverrunStyle.CLIP);
                button.setEllipsisString("");
            }
        });
        if (dialog instanceof TextInputDialog textInputDialog) {
            textInputDialog.getEditor().setPrefWidth(520);
            textInputDialog.getEditor().selectAll();
        }
        return dialog;
    }
}
