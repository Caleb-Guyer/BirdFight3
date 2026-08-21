package com.example.birdgame3;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
    void junglePlatformsRemainWithinVisibleBranchReachOfTheirRootedTrees() throws Exception {
        assertJunglePlatformsWithinBranchReach(BirdGame3.MapVariant.STANDARD,
                new double[]{600.0, 1_600.0, 2_600.0, 3_600.0, 4_600.0, 5_400.0},
                620.0, 24);
        assertJunglePlatformsWithinBranchReach(BirdGame3.MapVariant.CARRION_THRONE,
                new double[]{1_050.0, 3_000.0, 4_950.0},
                1_000.0, 7);
    }

    @Test
    void skyCliffsVariantSurfacesRemainWithinTheirPaintedSupportClusters() throws Exception {
        assertSkyVariantSupports(BirdGame3.MapVariant.SKYBREAK_SPIRES,
                new double[]{720.0, 3_000.0, 5_060.0}, 1_170.0, 10);
        assertSkyVariantSupports(BirdGame3.MapVariant.PEREGRINE_RUN,
                new double[]{930.0, 3_000.0, 4_990.0}, 1_070.0, 7);
        assertSkyVariantSupports(BirdGame3.MapVariant.TEMPEST_SUMMIT,
                new double[]{970.0, 3_000.0, 5_050.0}, 1_020.0, 6);

        BirdGame3 command = prepare(BirdGame3.MapType.SKYCLIFFS, BirdGame3.MapVariant.CROWN_DUEL);
        assertEquals(4, command.platforms.size(), "Command Bridge authored surface count");
        Platform bridge = command.platforms.getFirst();
        for (Platform platform : command.platforms) {
            assertTrue(platform.x >= bridge.x - 10.0
                            && platform.x + platform.w
                            <= bridge.x + bridge.w + 10.0,
                    "Command Bridge upper decks must remain inside the two suspension pylons");
        }
    }

    @Test
    void skybreakPaintedBedrockMatchesItsOnlyFullWidthCollider() throws Exception {
        BirdGame3 skybreak = prepare(BirdGame3.MapType.SKYCLIFFS,
                BirdGame3.MapVariant.SKYBREAK_SPIRES);
        long fullWidthFloors = skybreak.platforms.stream()
                .filter(platform -> platform.x <= TOLERANCE)
                .filter(platform -> platform.x + platform.w >= BirdGame3.WORLD_WIDTH - TOLERANCE)
                .filter(platform -> platform.y >= BirdGame3.GROUND_Y - TOLERANCE)
                .count();
        assertEquals(1, fullWidthFloors,
                "Skybreak should expose exactly the one full-width bedrock floor that its art depicts");
    }

    @Test
    void rooftopRelayOverhangsTerminateOnPaintedFacadeInsteadOfHiddenRoofEdge() throws Exception {
        BirdGame3 game = prepare(BirdGame3.MapType.CITY, BirdGame3.MapVariant.ROOFTOP_RELAY);
        CityBuildingGeometry.Layout layout = CityBuildingGeometry.createRooftopRelay(
                game.platforms, BirdGame3.GROUND_Y);
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
    void standardCityFacadesRestOnStreetOrAParentRoof() throws Exception {
        BirdGame3 game = prepare(BirdGame3.MapType.CITY, BirdGame3.MapVariant.STANDARD);
        CityBuildingGeometry.Layout layout = CityBuildingGeometry.create(
                game.platforms, BirdGame3.GROUND_Y);
        assertFalse(layout.facades().isEmpty(), "standard City must contain visible building facades");
        for (CityBuildingGeometry.Facade facade : layout.facades()) {
            assertTrue(CityBuildingGeometry.facadeHasFoundation(
                            facade, game.platforms, BirdGame3.GROUND_Y, TOLERANCE),
                    () -> "standard City has a floating facade beneath platform at ("
                            + facade.roof().x + ", " + facade.roof().y + ")");
        }
    }

    @Test
    void everyRooftopRelayTowerContinuesToTheBottomOfTheScene() throws Exception {
        BirdGame3 game = prepare(BirdGame3.MapType.CITY, BirdGame3.MapVariant.ROOFTOP_RELAY);
        CityBuildingGeometry.Layout layout = CityBuildingGeometry.createRooftopRelay(
                game.platforms, BirdGame3.GROUND_Y);
        List<CityBuildingGeometry.Facade> narrowTowers = layout.facades().stream()
                .filter(facade -> facade.roof().w < 650.0)
                .toList();

        assertFalse(narrowTowers.isEmpty(),
                "the audit must cover Rooftop Relay's narrow upper towers");
        for (CityBuildingGeometry.Facade facade : layout.facades()) {
            assertEquals(BirdGame3.WORLD_HEIGHT, facade.bounds().bottom(), TOLERANCE,
                    () -> "Rooftop Relay tower at (" + facade.roof().x + ", "
                            + facade.roof().y + ") stops on another building");
        }
    }

    @Test
    void rooftopRelayCloudAltitudeDoesNotCreateAnInvisibleWalkableFloor() throws Exception {
        BirdGame3 game = prepare(BirdGame3.MapType.CITY, BirdGame3.MapVariant.ROOFTOP_RELAY);
        Bird bird = new Bird(995.0, BirdGame3.BirdType.PIGEON, 0, game);
        bird.y = BirdGame3.GROUND_Y - bird.bodyHeight();

        assertFalse(game.hasImplicitGroundFloorForCurrentArena(),
                "only Rooftop Relay's authored building roofs should be solid");
        assertFalse(bird.hasSolidGroundFloorUnderBody(),
                "the cloud bank must not act as collision geometry");
        assertFalse(bird.isOnGround(),
                "the open gap between HOME and MARKET must lead into the void");

        BirdGame3 standardCity = prepare(BirdGame3.MapType.CITY, BirdGame3.MapVariant.STANDARD);
        assertTrue(standardCity.hasImplicitGroundFloorForCurrentArena(),
                "removing Rooftop Relay's fake floor must not remove the normal City street");
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
        CityBuildingGeometry.Layout layout = variant == BirdGame3.MapVariant.ROOFTOP_RELAY
                ? CityBuildingGeometry.createRooftopRelay(game.platforms, BirdGame3.GROUND_Y)
                : CityBuildingGeometry.create(game.platforms, BirdGame3.GROUND_Y);
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

    private static void assertJunglePlatformsWithinBranchReach(BirdGame3.MapVariant variant,
                                                               double[] treeCenters,
                                                               double maximumReach,
                                                               int minimumPlatforms) throws Exception {
        BirdGame3 game = prepare(BirdGame3.MapType.VIBRANT_JUNGLE, variant);
        List<Platform> aerialPlatforms = game.platforms.stream()
                .filter(platform -> platform.y < BirdGame3.GROUND_Y - 55.0)
                .filter(platform -> platform.w > 105.0 && platform.h < 135.0)
                .toList();
        assertTrue(aerialPlatforms.size() >= minimumPlatforms,
                variant + " must retain enough authored climbing ledges for this audit");
        for (Platform platform : aerialPlatforms) {
            double center = platform.x + platform.w * 0.5;
            double nearestTree = Double.POSITIVE_INFINITY;
            for (double treeCenter : treeCenters) {
                nearestTree = Math.min(nearestTree, Math.abs(center - treeCenter));
            }
            assertTrue(nearestTree <= maximumReach,
                    () -> variant + " has an aerial platform beyond branch reach at ("
                            + platform.x + ", " + platform.y + ")");
        }
    }

    private static void assertSkyVariantSupports(BirdGame3.MapVariant variant,
                                                 double[] supportCenters,
                                                 double maximumReach,
                                                 int expectedPlatforms) throws Exception {
        BirdGame3 game = prepare(BirdGame3.MapType.SKYCLIFFS, variant);
        List<Platform> playable = game.platforms.stream()
                .filter(platform -> platform.w > 120.0 && platform.h < 180.0)
                .filter(platform -> platform.x >= 0.0
                        && platform.x + platform.w <= BirdGame3.WORLD_WIDTH)
                .toList();
        assertEquals(expectedPlatforms, playable.size(), variant + " authored surface count");
        for (Platform platform : playable) {
            double center = platform.x + platform.w * 0.5;
            double nearest = Double.POSITIVE_INFINITY;
            for (double supportCenter : supportCenters) {
                nearest = Math.min(nearest, Math.abs(center - supportCenter));
            }
            assertTrue(nearest <= maximumReach,
                    () -> variant + " has a surface beyond its painted mountain support at ("
                            + platform.x + ", " + platform.y + ")");
        }
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
