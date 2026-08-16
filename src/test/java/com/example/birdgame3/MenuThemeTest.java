package com.example.birdgame3;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class MenuThemeTest {
    @Test
    void modernPanelsAndButtonsRetainLayeringBordersAndDepth() {
        String panel = MenuTheme.panelStyle("#64B5F6", 24);
        String button = MenuTheme.buttonStyle("#1565C0", 20);

        assertTrue(panel.contains("linear-gradient"));
        assertTrue(panel.contains("-fx-border-color"));
        assertTrue(panel.contains("dropshadow"));
        assertTrue(button.contains("linear-gradient"));
        assertTrue(button.contains("-fx-border-color"));
        assertTrue(button.contains("dropshadow"));
    }

    @Test
    void brightButtonsAutomaticallyUseReadableDarkText() {
        assertTrue(MenuTheme.buttonStyle("#FFE45C", 18).contains("-fx-text-fill: #111111"));
        assertTrue(MenuTheme.buttonStyle("#1565C0", 18).contains("-fx-text-fill: white"));
    }
}
