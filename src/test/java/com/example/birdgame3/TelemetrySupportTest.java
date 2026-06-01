package com.example.birdgame3;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TelemetrySupportTest {
    @Test
    void entityCullingReportsOutsideCameraRects() {
        assertFalse(EntityCulling.isWorldRectOutsideCamera(
                120, 120, 40, 40,
                20, 100, 100, 400, 300));

        assertTrue(EntityCulling.isWorldRectOutsideCamera(
                10, 120, 40, 40,
                20, 100, 100, 400, 300));

        assertTrue(EntityCulling.isWorldRectOutsideCamera(
                550, 120, 40, 40,
                20, 100, 100, 400, 300));
    }

    @Test
    void gameplayTelemetryAggregatesMoveRowsByContext() {
        GameplayTelemetry telemetry = new GameplayTelemetry();

        telemetry.recordUse(BirdGame3.BirdType.MOCKINGBIRD, "Charles Down Special", false,
                "Standard", BirdGame3.MapType.BATTLEFIELD);
        telemetry.recordUse(BirdGame3.BirdType.MOCKINGBIRD, "Charles Down Special", true,
                "Standard", BirdGame3.MapType.BATTLEFIELD);
        telemetry.recordImpact(BirdGame3.BirdType.MOCKINGBIRD, "Charles Down Special", false,
                "Standard", BirdGame3.MapType.BATTLEFIELD, 18, true);
        telemetry.recordKo(BirdGame3.BirdType.MOCKINGBIRD, "Charles Down Special", false,
                "Standard", BirdGame3.MapType.BATTLEFIELD, false);

        List<GameplayTelemetry.MoveSnapshot> rows = telemetry.topMoves(4);

        assertEquals(1, rows.size());
        GameplayTelemetry.MoveSnapshot row = rows.getFirst();
        assertEquals("Charles", row.birdName());
        assertEquals("Charles Down Special", row.moveName());
        assertEquals(1, row.humanUses());
        assertEquals(1, row.cpuUses());
        assertEquals(1, row.hits());
        assertEquals(18, row.damage());
        assertEquals(1, row.kos());
        assertEquals(2, row.uses());
    }

    @Test
    void gameplayTelemetryKeepsCurrentMatchSeparateFromSessionTotals() {
        GameplayTelemetry telemetry = new GameplayTelemetry();

        telemetry.recordUse(BirdGame3.BirdType.EAGLE, "Dive", false,
                "FIGHT", BirdGame3.MapType.FOREST);
        telemetry.recordImpact(BirdGame3.BirdType.EAGLE, "Dive", false,
                "FIGHT", BirdGame3.MapType.FOREST, 24, true);

        assertEquals(1, telemetry.currentMatchTopMoves(4).size());

        telemetry.resetCurrentMatch();

        assertTrue(telemetry.currentMatchTopMoves(4).isEmpty());
        assertEquals(1, telemetry.topMoves(4).size());
        assertEquals(24, telemetry.topMoves(4).getFirst().damage());
    }

    @Test
    void gameplayTelemetryBuildsCurrentMatchBirdRows() {
        GameplayTelemetry telemetry = new GameplayTelemetry();

        telemetry.recordImpact(BirdGame3.BirdType.PHOENIX, "Snap Fire", false,
                "CLASSIC", BirdGame3.MapType.BEACON_CROWN, 31, true);
        telemetry.recordKo(BirdGame3.BirdType.PHOENIX, "Snap Fire", false,
                "CLASSIC", BirdGame3.MapType.BEACON_CROWN, false);
        telemetry.recordRecoveryFailure(BirdGame3.BirdType.PHOENIX, false,
                "CLASSIC", BirdGame3.MapType.BEACON_CROWN);
        telemetry.recordSurvival(BirdGame3.BirdType.PHOENIX, false,
                "CLASSIC", BirdGame3.MapType.BEACON_CROWN, 180);

        List<GameplayTelemetry.BirdSnapshot> rows = telemetry.currentMatchBirds();

        assertEquals(1, rows.size());
        GameplayTelemetry.BirdSnapshot row = rows.getFirst();
        assertEquals("Phoenix", row.birdName());
        assertEquals(31, row.damage());
        assertEquals(1, row.kos());
        assertEquals(1, row.selfKos());
        assertEquals(1, row.recoveryFailures());
        assertEquals(3.0, row.averageSurvivalSeconds());
    }
}
