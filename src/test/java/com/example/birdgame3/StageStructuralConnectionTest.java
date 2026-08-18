package com.example.birdgame3;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StageStructuralConnectionTest {
    private static final double TOLERANCE = 0.001;

    @Test
    void everySelectableStagePassesItsRegisteredStructuralConnectionAudit() throws Exception {
        List<String> auditedLayouts = new ArrayList<>();
        for (BirdGame3.MapType map : BirdGame3.MapType.values()) {
            auditStage(map, BirdGame3.MapVariant.STANDARD, auditedLayouts);
        }
        for (BirdGame3.MapVariant variant : BirdGame3.MapVariant.values()) {
            if (variant != BirdGame3.MapVariant.STANDARD) {
                auditStage(variant.baseMap, variant, auditedLayouts);
            }
        }

        assertTrue(auditedLayouts.contains("CITY / STANDARD"));
        assertTrue(auditedLayouts.contains("CITY / ROOFTOP_RELAY"));
        assertTrue(auditedLayouts.contains("GLASSWIND_CAUSEWAY / STANDARD"));
    }

    @Test
    void rooftopRelayOverhangsTerminateOnPaintedFacadeInsteadOfHiddenRoofEdge() throws Exception {
        BirdGame3 game = prepare(BirdGame3.MapType.CITY, BirdGame3.MapVariant.ROOFTOP_RELAY);
        CityBuildingGeometry.Layout layout = CityBuildingGeometry.create(game.platforms, BirdGame3.GROUND_Y);
        List<CityBuildingGeometry.BalconyBraces> overhangs = layout.balconyBraces().stream()
                .filter(braces -> !braces.integratedIntoFacade())
                .toList();

        assertFalse(overhangs.isEmpty(), "Rooftop Relay must retain its facade-mounted overhangs");
        for (CityBuildingGeometry.BalconyBraces braces : overhangs) {
            for (StageArtGeometry.Segment segment : braces.segments()) {
                double endX = segment.end().x();
                CityBuildingGeometry.Bounds facade = braces.building().bounds();
                assertTrue(Math.abs(endX - facade.x()) <= TOLERANCE
                                || Math.abs(endX - facade.right()) <= TOLERANCE,
                        "overhang brace must meet the painted facade edge");
            }
        }
    }

    @Test
    void everyForegroundCityFacadeRestsOnGroundOrAParentRoof() throws Exception {
        for (BirdGame3.MapVariant variant : List.of(
                BirdGame3.MapVariant.STANDARD,
                BirdGame3.MapVariant.ROOFTOP_RELAY)) {
            BirdGame3 game = prepare(BirdGame3.MapType.CITY, variant);
            CityBuildingGeometry.Layout layout = CityBuildingGeometry.create(
                    game.platforms, BirdGame3.GROUND_Y);
            assertFalse(layout.facades().isEmpty(), variant + " must contain visible building facades");
            for (CityBuildingGeometry.Facade facade : layout.facades()) {
                assertTrue(CityBuildingGeometry.facadeHasFoundation(
                                facade, game.platforms, BirdGame3.GROUND_Y, TOLERANCE),
                        () -> variant + " has a floating facade beneath platform at ("
                                + facade.roof().x + ", " + facade.roof().y + ")");
            }
        }
    }

    @Test
    void rooftopRelaySkylineTowersContinueBehindTheCloudFoundation() {
        double foundationY = BirdGame3.GROUND_Y
                + CityBuildingGeometry.ROOFTOP_RELAY_FOUNDATION_DEPTH;
        for (CityBuildingGeometry.SkylineTower tower : CityBuildingGeometry.skylineLayer(
                340.0, 1_050.0, foundationY)) {
            assertTrue(tower.foundationY() >= BirdGame3.GROUND_Y + 500.0,
                    "distant tower must continue behind the cloud deck instead of floating on it");
            assertTrue(tower.height() > 0.0, "distant tower must have a valid body");
        }
        for (CityBuildingGeometry.SkylineTower tower : CityBuildingGeometry.skylineLayer(
                610.0, 1_260.0, foundationY)) {
            assertTrue(tower.foundationY() >= BirdGame3.GROUND_Y + 500.0,
                    "near tower must continue behind the cloud deck instead of floating on it");
            assertTrue(tower.height() > 0.0, "near tower must have a valid body");
        }
    }

    private static void auditStage(BirdGame3.MapType map,
                                   BirdGame3.MapVariant variant,
                                   List<String> auditedLayouts) throws Exception {
        if (map == BirdGame3.MapType.GLASSWIND_CAUSEWAY
                && variant == BirdGame3.MapVariant.STANDARD) {
            GlasswindCausewayGeometry.Layout layout = GlasswindCausewayGeometry.create(
                    BirdGame3.GROUND_Y - 300.0);
            StageArtGeometry.AuditResult result = StageArtGeometry.audit(
                    layout.auditedSegments(), layout.allowedOpenEndpoints(),
                    BirdGame3.WORLD_WIDTH, BirdGame3.WORLD_HEIGHT,
                    TOLERANCE, GlasswindCausewayGeometry.MIRROR_AXIS_X);
            assertTrue(result.clean(), () -> "GLASSWIND_CAUSEWAY / STANDARD: "
                    + String.join(System.lineSeparator(), result.issues()));
            auditedLayouts.add("GLASSWIND_CAUSEWAY / STANDARD");
            return;
        }
        if (map != BirdGame3.MapType.CITY
                || variant == BirdGame3.MapVariant.PARLIAMENT_ROOFTOPS) {
            return;
        }

        BirdGame3 game = prepare(map, variant);
        CityBuildingGeometry.Layout layout = CityBuildingGeometry.create(game.platforms, BirdGame3.GROUND_Y);
        for (CityBuildingGeometry.BalconyBraces braces : layout.balconyBraces()) {
            for (StageArtGeometry.Segment segment : braces.segments()) {
                String label = map + " / " + variant + " support " + segment;
                assertTrue(CityBuildingGeometry.startsOnBalcony(segment, braces.balcony(), TOLERANCE),
                        label + " does not start on its platform underside");
                assertTrue(CityBuildingGeometry.endsOnVisibleFacade(segment, braces.building(), TOLERANCE),
                        label + " does not terminate on the visible building facade");
            }
        }
        auditedLayouts.add(map + " / " + variant);
    }

    private static BirdGame3 prepare(BirdGame3.MapType map,
                                     BirdGame3.MapVariant variant) throws Exception {
        BirdGame3 game = new BirdGame3();
        game.selectedMap = map;
        game.selectedMapVariant = variant;
        invoke(game, "setupMatchArenaGeometry");
        invoke(game, "applySelectedMapVariantArena");
        return game;
    }

    private static void invoke(BirdGame3 game, String methodName) throws Exception {
        Method method = BirdGame3.class.getDeclaredMethod(methodName);
        method.setAccessible(true);
        method.invoke(game);
    }
}
