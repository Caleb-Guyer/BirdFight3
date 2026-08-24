package com.example.birdgame3;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UiLayoutMetricsTest {
    @Test
    void fixedFrameFitsCommonAspectRatiosWithoutCropping() {
        for (double[] viewport : new double[][]{
                {1920, 1080},
                {1600, 900},
                {1366, 768},
                {1280, 1024},
                {1024, 768}
        }) {
            double scale = UiLayoutMetrics.fitScale(
                    UiLayoutMetrics.safeContentWidth(viewport[0]),
                    UiLayoutMetrics.safeContentHeight(viewport[1]),
                    UiLayoutMetrics.DESIGN_WIDTH,
                    UiLayoutMetrics.DESIGN_HEIGHT);
            assertTrue(UiLayoutMetrics.DESIGN_WIDTH * scale <= viewport[0] + 0.001);
            assertTrue(UiLayoutMetrics.DESIGN_HEIGHT * scale <= viewport[1] + 0.001);
        }
    }

    @Test
    void invalidDimensionsStillProduceStablePositiveMetrics() {
        assertEquals(1.0 / 1600.0, UiLayoutMetrics.fitScale(0, 0, 1600, 950), 0.000001);
        assertEquals(1.0, UiLayoutMetrics.safeContentWidth(-10), 0.000001);
        assertEquals(1.0, UiLayoutMetrics.safeContentHeight(-10), 0.000001);
    }
}
