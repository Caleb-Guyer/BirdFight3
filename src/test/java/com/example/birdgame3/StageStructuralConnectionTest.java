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
    void titanDockCatwalksTerminateOnDreadnoughtMastsOrDrydockGantries() throws Exception {
        BirdGame3 titan = prepare(BirdGame3.MapType.DOCK, BirdGame3.MapVariant.TITAN_DOCK);
        Platform mainDeck = titan.platforms.stream()
                .filter(platform -> platform.w >= 2_000.0)
                .findFirst()
                .orElseThrow();
        assertEquals(10, titan.platforms.size(), "Titan Dock authored surface count");

        for (Platform platform : titan.platforms) {
            if (platform == mainDeck || platform.w >= 800.0) continue;
            double center = platform.x + platform.w * 0.5;
            boolean shipMounted = center >= mainDeck.x - 300.0
                    && center <= mainDeck.x + mainDeck.w + 300.0;
            boolean gantryMounted = center <= 1_600.0 || center >= 4_400.0;
            assertTrue(shipMounted || gantryMounted,
                    () -> "Titan Dock catwalk has no painted mast or gantry at ("
                            + platform.x + ", " + platform.y + ")");
        }
    }

    @Test
    void crownVariantsKeepTheirAuthoredFragmentCountsAndOpenVoidGaps() throws Exception {
        assertCrownVariantGeometry(BirdGame3.MapVariant.NULL_ROCK_DUEL, 4);
        assertCrownVariantGeometry(BirdGame3.MapVariant.NULL_ROC_ASCENDING, 8);
        assertCrownVariantGeometry(BirdGame3.MapVariant.VOID_CROWN, 6);
    }

    @Test
    void desertWatchtowerRoofsHaveGroundedFoundationsAndCliffLedgesStayInTheMesa() throws Exception {
        BirdGame3 game = prepare(BirdGame3.MapType.DESERT, BirdGame3.MapVariant.STANDARD);
        List<Platform> freestandingRoofs = game.platforms.stream()
                .filter(platform -> platform.y < BirdGame3.GROUND_Y - 40.0)
                .filter(platform -> platform.x < 4_640.0)
                .filter(platform -> platform.x >= 0.0 && platform.h < 100.0)
                .toList();

        assertEquals(3, freestandingRoofs.size(), "Desert grounded waystation roof count");
        for (Platform roof : freestandingRoofs) {
            assertTrue(roof.x >= 1_500.0 && roof.x + roof.w <= 4_100.0,
                    () -> "Desert waystation roof escaped the grounded caravan route at ("
                            + roof.x + ", " + roof.y + ")");
            assertTrue(roof.w >= 150.0 && roof.w <= 200.0,
                    "the shared watchtower renderer must be able to reach both roof edges");
        }

        long cliffLedges = game.platforms.stream()
                .filter(platform -> platform.x >= 4_640.0)
                .filter(platform -> platform.y < BirdGame3.GROUND_Y - 40.0)
                .count();
        assertTrue(cliffLedges >= 7,
                "the stepped mesa must retain its integrated climb and recovery ledges");
    }

    @Test
    void redlineSwitchbacksAndBattlefieldPerchesRemainInsideTheirPaintedFoundations() throws Exception {
        BirdGame3 redline = prepare(BirdGame3.MapType.DESERT, BirdGame3.MapVariant.REDLINE_CANYON);
        Platform road = redline.platforms.stream()
                .filter(platform -> platform.w >= 3_000.0)
                .findFirst()
                .orElseThrow();
        assertTrue(road.w >= 5_500.0, "Redline must retain one finishable continuous road");
        for (Platform ledge : redline.platforms) {
            if (ledge == road) continue;
            assertTrue(ledge.x >= road.x && ledge.x + ledge.w <= road.x + road.w,
                    () -> "Redline switchback escaped the canyon viaduct foundation at ("
                            + ledge.x + ", " + ledge.y + ")");
            assertTrue(ledge.y + ledge.h < road.y,
                    "upper route ledges must remain visibly separated from the road deck");
        }

        BirdGame3 battlefield = prepare(BirdGame3.MapType.BATTLEFIELD, BirdGame3.MapVariant.STANDARD);
        Platform island = battlefield.platforms.get(0);
        assertEquals(4, battlefield.platforms.size(), "Battlefield authored surface count");
        for (int i = 1; i < battlefield.platforms.size(); i++) {
            Platform perch = battlefield.platforms.get(i);
            assertTrue(perch.x >= island.x && perch.x + perch.w <= island.x + island.w,
                    () -> "Battlefield perch cannot be joined to its citadel at ("
                            + perch.x + ", " + perch.y + ")");
        }
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

    private static void assertCrownVariantGeometry(BirdGame3.MapVariant variant,
                                                   int expectedPlatforms) throws Exception {
        BirdGame3 game = prepare(BirdGame3.MapType.BEACON_CROWN, variant);
        assertEquals(expectedPlatforms, game.platforms.size(), variant + " fragment count");
        assertFalse(game.platforms.stream().anyMatch(platform ->
                        platform.x <= 0.0 && platform.x + platform.w >= BirdGame3.WORLD_WIDTH),
                variant + " depicts open void and must not gain an invisible full-width floor");
        for (Platform platform : game.platforms) {
            assertTrue(platform.x >= 0.0 && platform.x + platform.w <= BirdGame3.WORLD_WIDTH,
                    variant + " fragment must remain within its rendered world bounds");
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
