package com.example.birdgame3;

/** Pure responsive-layout math shared by fixed-frame JavaFX screens and tests. */
final class UiLayoutMetrics {
    static final double DESIGN_WIDTH = 1600.0;
    static final double DESIGN_HEIGHT = 950.0;
    static final double SAFE_EDGE = 24.0;

    private UiLayoutMetrics() {
    }

    static double fitScale(double availableWidth, double availableHeight,
                           double designWidth, double designHeight) {
        double safeDesignWidth = Math.max(1.0, designWidth);
        double safeDesignHeight = Math.max(1.0, designHeight);
        return Math.min(Math.max(1.0, availableWidth) / safeDesignWidth,
                Math.max(1.0, availableHeight) / safeDesignHeight);
    }

    static double safeContentWidth(double sceneWidth) {
        return Math.max(1.0, sceneWidth - SAFE_EDGE * 2.0);
    }

    static double safeContentHeight(double sceneHeight) {
        return Math.max(1.0, sceneHeight - SAFE_EDGE * 2.0);
    }
}
