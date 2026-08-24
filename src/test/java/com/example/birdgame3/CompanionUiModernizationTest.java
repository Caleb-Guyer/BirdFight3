package com.example.birdgame3;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class CompanionUiModernizationTest {
    private static final Path SOURCE = Path.of(
            "src", "main", "java", "com", "example", "birdgame3", "BirdFight3CompanionProgram.java");

    @Test
    void connectionChromeUsesAReadableModernHierarchy() throws IOException {
        String source = Files.readString(SOURCE);

        assertTrue(source.contains("MATCH COMPANION"));
        assertTrue(source.contains("VBox topChrome"));
        assertTrue(source.contains("companionButtonStyle"));
        assertTrue(source.contains("companionStatusStyle"));
        assertTrue(source.contains("hostField.setOnAction"));
        assertTrue(source.contains("HBox.setHgrow(hostField, Priority.ALWAYS)"));
    }

    @Test
    void companionCanvasStillScalesWithItsAvailableViewport() throws IOException {
        String source = Files.readString(SOURCE);

        assertTrue(source.contains("canvas.widthProperty().bind(canvasPane.widthProperty())"));
        assertTrue(source.contains("canvas.heightProperty().bind(canvasPane.heightProperty())"));
        assertTrue(source.contains("stage.setMinWidth(900)"));
        assertTrue(source.contains("stage.setMinHeight(560)"));
    }
}
