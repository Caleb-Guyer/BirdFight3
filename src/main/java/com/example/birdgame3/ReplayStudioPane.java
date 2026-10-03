package com.example.birdgame3;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;

import java.util.ArrayList;
import java.util.List;
import java.util.function.IntConsumer;

/** Presentation controls only; all replay simulation stays in the game's fixed tick loop. */
final class ReplayStudioPane extends VBox {
    record Actions(Runnable pause, Runnable step, Runnable slower, Runnable faster,
                   Runnable previousKo, Runnable nextKo, Runnable markIn, Runnable markOut,
                   Runnable export, Runnable cancel, Runnable exit, IntConsumer seek) { }

    private final ReplayStudioState state;
    private final Slider timeline;
    private final Label time = label("", "#E1F5FE", 20);
    private final Label clip = label("", "#CFD8DC", 17);
    private final Label status = label("", "#FFF59D", 17);
    private final Button pause;
    private final Button export;
    private final Button cancel;
    private final List<Button> editing = new ArrayList<>();
    private boolean refreshing;
    private String inputHint = "Click the timeline to seek. J / K find KOs. I / O select a clip. ESC exits.";

    ReplayStudioPane(ReplayStudioState state, boolean hasKnockouts, Actions actions) {
        this.state = state;
        setId("replayStudio");
        setSpacing(8);
        setPadding(new Insets(15, 22, 15, 22));
        setMaxWidth(1580);
        setMaxHeight(USE_PREF_SIZE);
        setStyle("-fx-background-color: rgba(5,15,28,0.94); -fx-border-color: #4FC3F7;"
                + "-fx-border-radius: 16; -fx-background-radius: 16;");
        Label heading = label("REPLAY STUDIO", "#81D4FA", 23);
        timeline = new Slider(0, Math.max(1, state.totalFrames()), 0);
        timeline.setAccessibleText("Replay timeline");
        timeline.setBlockIncrement(300);
        timeline.setOnMouseReleased(event -> {
            if (!refreshing && !timeline.isDisabled()) actions.seek().accept((int) Math.round(timeline.getValue()));
        });
        timeline.valueChangingProperty().addListener((o, wasChanging, changing) -> {
            if (wasChanging && !changing && !refreshing && !timeline.isDisabled()) {
                actions.seek().accept((int) Math.round(timeline.getValue()));
            }
        });
        pause = button("PAUSE [SPACE]", actions.pause());
        Button step = button("FRAME [.]", actions.step());
        Button slower = button("SLOWER [−]", actions.slower());
        Button faster = button("FASTER [+]", actions.faster());
        Button previous = button("PREV KO [J]", actions.previousKo());
        Button next = button("NEXT KO [K]", actions.nextKo());
        previous.setDisable(!hasKnockouts);
        next.setDisable(!hasKnockouts);
        Button in = button("MARK IN [I]", actions.markIn());
        Button out = button("MARK OUT [O]", actions.markOut());
        export = button("EXPORT CLIP [E]", actions.export());
        cancel = button("CANCEL EXPORT", actions.cancel());
        Button exit = button("EXIT [ESC]", actions.exit());
        editing.addAll(List.of(pause, step, slower, faster, in, out));
        if (hasKnockouts) editing.addAll(List.of(previous, next));
        FlowPane controls = new FlowPane(8, 8, pause, step, slower, faster, previous, next, in, out, export, cancel, exit);
        controls.setAlignment(Pos.CENTER_LEFT);
        getChildren().addAll(heading, time, timeline, controls, clip, status);
        refresh(0, "", false);
    }

    void refresh(int frame, String message, boolean exporting) {
        refreshing = true;
        try {
            if (!timeline.isValueChanging() && !timeline.isPressed()) timeline.setValue(frame);
            timeline.setDisable(exporting);
            pause.setText(state.paused() ? "PLAY [SPACE]" : "PAUSE [SPACE]");
            time.setText(ReplayStudioState.formatTimestamp(frame) + " / "
                    + ReplayStudioState.formatTimestamp(state.totalFrames()) + "   ·   " + state.speedLabel()
                    + (state.seeking() ? "   ·   SEEKING…" : state.paused() ? "   ·   PAUSED" : ""));
            clip.setText("CLIP  " + ReplayStudioState.formatTimestamp(state.clipStart()) + " → "
                    + ReplayStudioState.formatTimestamp(state.clipEnd())
                    + "   ·   Silent AVI · 720p / 30 FPS · 1× speed · Up to 60 seconds of video");
            status.setText(message == null || message.isBlank()
                    ? inputHint
                    : message);
            editing.forEach(button -> button.setDisable(exporting));
            export.setDisable(exporting || state.seeking() || !state.validClip());
            cancel.setVisible(exporting);
            cancel.setManaged(exporting);
        } finally {
            refreshing = false;
        }
    }

    void setInputHint(String inputHint) {
        this.inputHint = inputHint;
    }

    private static Label label(String text, String color, int size) {
        Label label = new Label(text);
        label.setTextFill(Color.web(color));
        label.setStyle("-fx-font-family: 'Consolas'; -fx-font-size: " + size + "px;");
        label.setWrapText(true);
        return label;
    }

    private static Button button(String text, Runnable action) {
        Button button = new Button(text);
        button.setStyle("-fx-background-color: #193952; -fx-text-fill: #E1F5FE;"
                + "-fx-font-family: 'Consolas'; -fx-font-size: 17px; -fx-padding: 10 13;"
                + "-fx-background-radius: 7;");
        button.setOnAction(event -> action.run());
        return button;
    }
}
