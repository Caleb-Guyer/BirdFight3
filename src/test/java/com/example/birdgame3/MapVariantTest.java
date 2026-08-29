package com.example.birdgame3;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.lang.reflect.Field;
import java.util.List;

import static com.example.birdgame3.BirdGame3.MapType;
import static com.example.birdgame3.BirdGame3.MapVariant;
import static com.example.birdgame3.BirdGame3.StageChoice;
import static com.example.birdgame3.BirdGame3.StageRandomPool;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MapVariantTest {
    @Test
    void variantChoiceAlwaysUsesItsAuthoredBaseMap() {
        StageChoice choice = new StageChoice(MapType.FOREST, MapVariant.PARLIAMENT_ROOFTOPS);

        assertEquals(MapType.CITY, choice.map());
        assertEquals(MapVariant.PARLIAMENT_ROOFTOPS, choice.variant());
    }

    @Test
    void randomPoolsKeepMainAndVariantCatalogsSeparate() {
        BirdGame3 game = new BirdGame3();

        List<StageChoice> main = game.availableStageChoices(StageRandomPool.MAIN);
        List<StageChoice> variants = game.availableStageChoices(StageRandomPool.VARIANTS);
        List<StageChoice> all = game.availableStageChoices(StageRandomPool.ALL);

        assertTrue(main.stream().allMatch(choice -> choice.variant() == MapVariant.STANDARD));
        assertTrue(variants.stream().allMatch(choice -> choice.variant() != MapVariant.STANDARD));
        assertTrue(variants.stream().anyMatch(choice -> choice.variant() == MapVariant.CROWN_DUEL));
        assertTrue(all.containsAll(main));
        assertTrue(all.containsAll(variants));
    }

    @Test
    void mainCityUsesBroadSkyscraperRooftops() throws Exception {
        BirdGame3 game = new BirdGame3();
        game.currentMatchSeed = 42L;
        game.selectedMap = MapType.CITY;
        Method setupBase = BirdGame3.class.getDeclaredMethod("setupMatchArenaGeometry");
        setupBase.setAccessible(true);

        setupBase.invoke(game);

        List<Platform> cityPlatforms = game.platforms.stream()
                .filter(platform -> platform.x >= 0.0 && platform.x < BirdGame3.WORLD_WIDTH
                        && platform.y < BirdGame3.GROUND_Y)
                .toList();
        List<Platform> skyscraperRoofs = cityPlatforms.stream()
                .filter(platform -> platform.w >= 650.0)
                .toList();
        assertEquals(6, skyscraperRoofs.size());
        assertTrue(skyscraperRoofs.stream().allMatch(platform -> platform.w >= 700.0),
                "City combat platforms should read as broad skyscraper roofs");
        assertEquals(18, cityPlatforms.size());
        assertTrue(cityPlatforms.stream()
                        .filter(platform -> platform.w < 650.0)
                        .allMatch(platform -> skyscraperRoofs.stream().anyMatch(roof -> {
                            return platform.x >= roof.x
                                    && platform.x + platform.w <= roof.x + roof.w;
                        })),
                "secondary City platforms should sit fully within a skyscraper facade");
    }

    @Test
    void everyVariantBuildsAPlayableArena() throws Exception {
        BirdGame3 game = new BirdGame3();
        game.currentMatchSeed = 42L;
        Method setupBase = BirdGame3.class.getDeclaredMethod("setupMatchArenaGeometry");
        Method applyVariant = BirdGame3.class.getDeclaredMethod("applySelectedMapVariantArena");
        setupBase.setAccessible(true);
        applyVariant.setAccessible(true);

        for (MapVariant variant : MapVariant.values()) {
            if (variant == MapVariant.STANDARD) continue;
            game.selectedMap = variant.baseMap;
            game.selectedMapVariant = variant;
            setupBase.invoke(game);
            applyVariant.invoke(game);
            assertFalse(game.platforms.isEmpty(), variant + " must leave solid platforms in the arena");
        }
    }

    @Test
    void storyMissionAppliesItsAuthoredMapVariant() throws Exception {
        BirdGame3 game = new BirdGame3();
        game.currentMatchSeed = 42L;
        StoryCampaign.Mission mission = StoryCampaignContent.create().mission("last_call");
        game.selectedMap = mission.map();

        Method setupBase = BirdGame3.class.getDeclaredMethod("setupMatchArenaGeometry");
        Method applyMission = BirdGame3.class.getDeclaredMethod(
                "applyCampaignMissionArenaModifiers", StoryCampaign.Mission.class);
        setupBase.setAccessible(true);
        applyMission.setAccessible(true);

        setupBase.invoke(game);
        applyMission.invoke(game, mission);

        assertFalse(game.platforms.stream().anyMatch(platform -> platform.w >= BirdGame3.WORLD_WIDTH));
        assertTrue(game.platforms.stream().anyMatch(platform -> platform.w == 1_430.0),
                "Last Call should use the Parliament Towers rooftop layout");
    }

    @Test
    void commandBridgeUsesTheCompactStoryLayout() throws Exception {
        BirdGame3 game = new BirdGame3();
        game.currentMatchSeed = 7L;
        game.selectedMap = MapType.SKYCLIFFS;
        game.selectedMapVariant = MapVariant.CROWN_DUEL;
        Method setupBase = BirdGame3.class.getDeclaredMethod("setupMatchArenaGeometry");
        Method applyVariant = BirdGame3.class.getDeclaredMethod("applySelectedMapVariantArena");
        setupBase.setAccessible(true);
        applyVariant.setAccessible(true);

        setupBase.invoke(game);
        applyVariant.invoke(game);

        assertEquals(4, game.platforms.size());
        assertTrue(game.platforms.stream().anyMatch(platform -> platform.w == 2200.0));
    }

    @Test
    void rooftopRelayIsAConnectedSunriseCityCourseAndUnlockableVariant() throws Exception {
        BirdGame3 game = new BirdGame3();
        game.currentMatchSeed = 77L;
        game.selectedMap = MapType.CITY;
        game.selectedMapVariant = MapVariant.ROOFTOP_RELAY;
        Method setupBase = BirdGame3.class.getDeclaredMethod("setupMatchArenaGeometry");
        Method applyVariant = BirdGame3.class.getDeclaredMethod("applySelectedMapVariantArena");
        setupBase.setAccessible(true);
        applyVariant.setAccessible(true);

        setupBase.invoke(game);
        applyVariant.invoke(game);

        Field activeVariant = BirdGame3.class.getDeclaredField("activeArenaGeometryVariant");
        activeVariant.setAccessible(true);
        assertEquals(MapVariant.ROOFTOP_RELAY, activeVariant.get(game));
        assertTrue(game.platforms.stream().noneMatch(platform -> platform.w >= BirdGame3.WORLD_WIDTH));
        assertEquals(6, game.platforms.stream().filter(platform -> platform.w >= 650.0).count());
        assertTrue(game.platforms.stream().filter(platform -> platform.w >= 650.0)
                .allMatch(platform -> platform.signText != null));

        assertFalse(game.availableStageChoices(StageRandomPool.VARIANTS).stream()
                .anyMatch(choice -> choice.variant() == MapVariant.ROOFTOP_RELAY));
        Field unlocked = BirdGame3.class.getDeclaredField("rooftopRelayUnlocked");
        unlocked.setAccessible(true);
        unlocked.setBoolean(game, true);
        assertTrue(game.availableStageChoices(StageRandomPool.VARIANTS).stream()
                .anyMatch(choice -> choice.variant() == MapVariant.ROOFTOP_RELAY));
    }

    @Test
    void tempestSummitIsAnOpenFloatingArenaAndUnlockableVariant() throws Exception {
        BirdGame3 game = buildVariant(MapVariant.TEMPEST_SUMMIT);

        Field activeVariant = BirdGame3.class.getDeclaredField("activeArenaGeometryVariant");
        activeVariant.setAccessible(true);
        assertEquals(MapVariant.TEMPEST_SUMMIT, activeVariant.get(game));
        assertFalse(game.platforms.stream().anyMatch(platform -> platform.w >= BirdGame3.WORLD_WIDTH));
        assertFalse(game.platforms.stream().anyMatch(platform -> platform.y <= 0.0));
        assertTrue(game.platforms.stream().anyMatch(platform -> platform.w == 2_340.0));
        assertEquals(3, game.windVents.size());
        assertTrue(game.usesIslandBoundsForCurrentArena());

        assertFalse(game.availableStageChoices(StageRandomPool.VARIANTS).stream()
                .anyMatch(choice -> choice.variant() == MapVariant.TEMPEST_SUMMIT));
        Field unlocked = BirdGame3.class.getDeclaredField("tempestSummitUnlocked");
        unlocked.setAccessible(true);
        unlocked.setBoolean(game, true);
        assertTrue(game.availableStageChoices(StageRandomPool.VARIANTS).stream()
                .anyMatch(choice -> choice.variant() == MapVariant.TEMPEST_SUMMIT));
    }

    @Test
    void peregrineRunIsAnOpenThreeShelfDiveCourseAndUnlockableVariant() throws Exception {
        BirdGame3 game = buildVariant(MapVariant.PEREGRINE_RUN);

        Field activeVariant = BirdGame3.class.getDeclaredField("activeArenaGeometryVariant");
        activeVariant.setAccessible(true);
        assertEquals(MapVariant.PEREGRINE_RUN, activeVariant.get(game));
        assertFalse(game.platforms.stream().anyMatch(platform -> platform.w >= BirdGame3.WORLD_WIDTH));
        assertFalse(game.platforms.stream().anyMatch(platform -> platform.y <= 0.0));
        assertEquals(3, game.platforms.stream().filter(platform -> platform.w >= 1_200.0).count());
        assertEquals(3, game.windVents.size());
        assertTrue(game.usesIslandBoundsForCurrentArena());

        assertFalse(game.availableStageChoices(StageRandomPool.VARIANTS).stream()
                .anyMatch(choice -> choice.variant() == MapVariant.PEREGRINE_RUN));
        Field unlocked = BirdGame3.class.getDeclaredField("peregrineRunUnlocked");
        unlocked.setAccessible(true);
        unlocked.setBoolean(game, true);
        assertTrue(game.availableStageChoices(StageRandomPool.VARIANTS).stream()
                .anyMatch(choice -> choice.variant() == MapVariant.PEREGRINE_RUN));
    }

    @Test
    void frozenCalderaIsAnOpenMirroredBossArenaAndUnlockableVariant() throws Exception {
        BirdGame3 game = buildVariant(MapVariant.FROZEN_CALDERA);

        Field activeVariant = BirdGame3.class.getDeclaredField("activeArenaGeometryVariant");
        activeVariant.setAccessible(true);
        assertEquals(MapVariant.FROZEN_CALDERA, activeVariant.get(game));
        assertFalse(game.platforms.stream().anyMatch(platform -> platform.w >= BirdGame3.WORLD_WIDTH));
        assertFalse(game.platforms.stream().anyMatch(platform -> platform.y <= 0.0));
        assertTrue(game.platforms.stream().anyMatch(platform -> platform.w == 2_500.0));
        assertEquals(3, game.windVents.size());
        assertTrue(game.usesIslandBoundsForCurrentArena());
        assertFalse(game.isAshfallCathedralActive(),
                "Frozen Caldera must not run invisible Ashfall geyser collisions");

        assertFalse(game.availableStageChoices(StageRandomPool.VARIANTS).stream()
                .anyMatch(choice -> choice.variant() == MapVariant.FROZEN_CALDERA));
        Field unlocked = BirdGame3.class.getDeclaredField("frozenCalderaUnlocked");
        unlocked.setAccessible(true);
        unlocked.setBoolean(game, true);
        assertTrue(game.availableStageChoices(StageRandomPool.VARIANTS).stream()
                .anyMatch(choice -> choice.variant() == MapVariant.FROZEN_CALDERA));
    }

    @Test
    void sortingFloorTraysAreStandableSwingingPlatformsThatCarryRiders() throws Exception {
        BirdGame3 game = buildVariant(MapVariant.SORTING_FLOOR);
        List<Platform> trays = game.platforms.stream()
                .filter(platform -> platform.carrionSortingTray)
                .toList();

        assertEquals(4, trays.size());
        assertTrue(trays.stream().allMatch(platform -> platform.swinging
                        && platform.w == 640.0 && platform.h >= 60.0),
                "every hanging tray should expose a broad collision surface");

        Platform tray = trays.getFirst();
        Bird rider = new Bird(tray.x + tray.w * 0.5 - 40.0,
                BirdGame3.BirdType.PIGEON, 0, game);
        rider.y = tray.y - rider.bodyHeight();
        game.players[0] = rider;

        double previousTrayX = tray.x;
        double previousTrayY = tray.y;
        double previousRiderX = rider.x;
        double previousRiderY = rider.y;
        game.simTick++;
        game.updateSwingingPlatformsFixed();

        assertTrue(Math.abs(tray.x - previousTrayX) > 0.01
                        || Math.abs(tray.y - previousTrayY) > 0.01,
                "the tray should advance along its pendulum path each sim tick");
        assertEquals(tray.x - previousTrayX, rider.x - previousRiderX, 0.000_001);
        assertEquals(tray.y - previousTrayY, rider.y - previousRiderY, 0.000_001);
        assertSame(tray, rider.findCurrentSupportPlatform(),
                "a carried fighter must remain grounded on the moving surface");
    }

    @Test
    void heartbloomSanctuaryIsAnOpenFlowerArenaAndUnlockableVariant() throws Exception {
        BirdGame3 game = buildVariant(MapVariant.HEARTBLOOM_SANCTUARY);

        Field activeVariant = BirdGame3.class.getDeclaredField("activeArenaGeometryVariant");
        activeVariant.setAccessible(true);
        assertEquals(MapVariant.HEARTBLOOM_SANCTUARY, activeVariant.get(game));
        assertFalse(game.platforms.stream().anyMatch(platform -> platform.w >= BirdGame3.WORLD_WIDTH));
        assertFalse(game.platforms.stream().anyMatch(platform -> platform.y <= 0.0));
        assertTrue(game.platforms.stream().anyMatch(platform -> platform.w == 2_600.0));
        assertEquals(3, game.windVents.size());
        assertTrue(game.usesIslandBoundsForCurrentArena());

        assertFalse(game.availableStageChoices(StageRandomPool.VARIANTS).stream()
                .anyMatch(choice -> choice.variant() == MapVariant.HEARTBLOOM_SANCTUARY));
        Field unlocked = BirdGame3.class.getDeclaredField("heartbloomSanctuaryUnlocked");
        unlocked.setAccessible(true);
        unlocked.setBoolean(game, true);
        assertTrue(game.availableStageChoices(StageRandomPool.VARIANTS).stream()
                .anyMatch(choice -> choice.variant() == MapVariant.HEARTBLOOM_SANCTUARY));
    }

    @Test
    void ashfallRebirthLeavesTheTopBlastRouteOpen() throws Exception {
        BirdGame3 game = buildVariant(MapVariant.ASHFALL_REBIRTH);

        assertFalse(game.platforms.stream().anyMatch(platform -> platform.y <= 0.0),
                "Ashfall Rebirth must not recreate the full-width ceiling trap");
        assertFalse(game.platforms.stream().anyMatch(platform -> platform.y >= BirdGame3.GROUND_Y
                        && platform.w >= BirdGame3.WORLD_WIDTH),
                "the Rebirth Altar should remain a floating arena");
    }

    @Test
    void redesignedBossArenasDoNotReuseTheirMainStageSilhouettes() throws Exception {
        BirdGame3 titanDock = buildVariant(MapVariant.TITAN_DOCK);
        BirdGame3 parliament = buildVariant(MapVariant.PARLIAMENT_ROOFTOPS);
        BirdGame3 nullRoc = buildVariant(MapVariant.NULL_ROC_ASCENDING);
        BirdGame3 voidCrown = buildVariant(MapVariant.VOID_CROWN);
        BirdGame3 tempestSummit = buildVariant(MapVariant.TEMPEST_SUMMIT);
        BirdGame3 peregrineRun = buildVariant(MapVariant.PEREGRINE_RUN);
        BirdGame3 frozenCaldera = buildVariant(MapVariant.FROZEN_CALDERA);
        BirdGame3 heartbloom = buildVariant(MapVariant.HEARTBLOOM_SANCTUARY);

        assertFalse(titanDock.platforms.stream().anyMatch(platform -> platform.x == 720.0 && platform.w == 1820.0));
        assertFalse(parliament.platforms.stream().anyMatch(platform -> platform.w >= BirdGame3.WORLD_WIDTH));
        assertFalse(nullRoc.platforms.stream().anyMatch(platform -> platform.w == 2920.0));
        assertFalse(voidCrown.platforms.stream().anyMatch(platform -> platform.w == 2920.0));
        assertTrue(titanDock.usesIslandBoundsForCurrentArena());
        assertTrue(parliament.usesIslandBoundsForCurrentArena());
        assertTrue(tempestSummit.usesIslandBoundsForCurrentArena());
        assertTrue(peregrineRun.usesIslandBoundsForCurrentArena());
        assertTrue(frozenCaldera.usesIslandBoundsForCurrentArena());
        assertTrue(heartbloom.usesIslandBoundsForCurrentArena());
    }

    private BirdGame3 buildVariant(MapVariant variant) throws Exception {
        BirdGame3 game = new BirdGame3();
        game.currentMatchSeed = 91L;
        game.selectedMap = variant.baseMap;
        game.selectedMapVariant = variant;
        Method setupBase = BirdGame3.class.getDeclaredMethod("setupMatchArenaGeometry");
        Method applyVariant = BirdGame3.class.getDeclaredMethod("applySelectedMapVariantArena");
        setupBase.setAccessible(true);
        applyVariant.setAccessible(true);
        setupBase.invoke(game);
        applyVariant.invoke(game);
        return game;
    }
}
