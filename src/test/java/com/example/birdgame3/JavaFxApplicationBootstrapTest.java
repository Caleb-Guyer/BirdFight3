package com.example.birdgame3;

import javafx.application.Application;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JavaFxApplicationBootstrapTest {
    @Test
    void directlyLaunchedGameClassCannotStartJavaFxBeforeRendererPolicy() {
        assertFalse(Application.class.isAssignableFrom(BirdGame3.class));
        assertTrue(Application.class.isAssignableFrom(BirdGame3Application.class));
    }
}
