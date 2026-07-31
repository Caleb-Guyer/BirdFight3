package com.example.birdgame3;

import javafx.scene.Group;
import javafx.scene.effect.DropShadow;
import javafx.scene.shape.Rectangle;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;

class MatchSummaryPresentationTest {
    @Test
    void cinematicSummaryScalesFromItsActualDesignResolution() {
        assertEquals(1.0,
                BirdGame3.fixedFrameScale(1920.0, 1080.0, 1920.0, 1080.0),
                0.0001);
        assertEquals(2048.0 / 1920.0,
                BirdGame3.fixedFrameScale(2048.0, 1280.0, 1920.0, 1080.0),
                0.0001);
        assertEquals(1280.0 / 1920.0,
                BirdGame3.fixedFrameScale(1280.0, 720.0, 1920.0, 1080.0),
                0.0001);
    }

    @Test
    void cinematicSummaryEffectCleanupIncludesNestedNodes() {
        Rectangle nested = new Rectangle(120.0, 40.0);
        nested.setEffect(new DropShadow());
        Group inner = new Group(nested);
        inner.setEffect(new DropShadow());
        Group root = new Group(inner);
        root.setEffect(new DropShadow());
        nested.setStyle("-fx-fill: red; -fx-effect: dropshadow(gaussian, black, 14, 0.2, 0, 5);");

        BirdGame3.clearNodeEffects(root);

        assertNull(root.getEffect());
        assertNull(inner.getEffect());
        assertNull(nested.getEffect());
        assertFalse(nested.getStyle().contains("-fx-effect"));
    }
}
