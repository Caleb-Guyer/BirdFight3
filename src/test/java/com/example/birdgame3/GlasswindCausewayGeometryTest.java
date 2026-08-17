package com.example.birdgame3;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GlasswindCausewayGeometryTest {
    private static final double DECK_TOP = BirdGame3.GROUND_Y - 300.0;

    @Test
    void bridgeStructureHasNoDisconnectedOrAsymmetricStrokes() {
        GlasswindCausewayGeometry.Layout layout = GlasswindCausewayGeometry.create(DECK_TOP);

        StageArtGeometry.AuditResult result = StageArtGeometry.audit(
                layout.auditedSegments(),
                layout.allowedOpenEndpoints(),
                BirdGame3.WORLD_WIDTH,
                BirdGame3.WORLD_HEIGHT,
                0.01,
                GlasswindCausewayGeometry.MIRROR_AXIS_X);

        assertTrue(result.clean(), () -> String.join(System.lineSeparator(), result.issues()));
    }

    @Test
    void everyHangerSharesItsExactCableAndDeckRailJoints() {
        GlasswindCausewayGeometry.Layout layout = GlasswindCausewayGeometry.create(DECK_TOP);

        for (StageArtGeometry.Segment hanger : layout.hangers()) {
            assertTrue(layout.cablePoints().stream()
                            .anyMatch(point -> StageArtGeometry.near(point, hanger.start(), 0.001)),
                    () -> "hanger misses cable at " + hanger.start());
            assertEquals(DECK_TOP, hanger.end().y(), 0.001, "hanger misses deck height");
            long railConnections = layout.deckRail().stream()
                    .filter(segment -> StageArtGeometry.near(segment.start(), hanger.end(), 0.001)
                            || StageArtGeometry.near(segment.end(), hanger.end(), 0.001))
                    .count();
            assertEquals(2L, railConnections, "hanger must meet both neighboring deck-rail spans");
        }
    }

    @Test
    void cableTerminatesAtBothPylonCapsAndCenterSag() {
        GlasswindCausewayGeometry.Layout layout = GlasswindCausewayGeometry.create(DECK_TOP);

        StageArtGeometry.Point leftTower = new StageArtGeometry.Point(
                GlasswindCausewayGeometry.LEFT_TOWER_X, GlasswindCausewayGeometry.TOWER_CABLE_Y);
        StageArtGeometry.Point rightTower = new StageArtGeometry.Point(
                GlasswindCausewayGeometry.RIGHT_TOWER_X, GlasswindCausewayGeometry.TOWER_CABLE_Y);
        StageArtGeometry.Point center = new StageArtGeometry.Point(
                GlasswindCausewayGeometry.MIRROR_AXIS_X, 1_035.0);

        assertEquals(2L, spansTouching(layout, leftTower));
        assertEquals(2L, spansTouching(layout, rightTower));
        assertEquals(2L, spansTouching(layout, center));
    }

    private static long spansTouching(GlasswindCausewayGeometry.Layout layout, StageArtGeometry.Point point) {
        return layout.cableSpans().stream()
                .filter(span -> StageArtGeometry.near(span.getFirst(), point, 0.001)
                        || StageArtGeometry.near(span.getLast(), point, 0.001))
                .count();
    }
}
