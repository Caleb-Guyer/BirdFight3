package com.example.birdgame3;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;

import static com.example.birdgame3.BirdGame3.BirdType;
import static com.example.birdgame3.BirdGame3.ClassicEncounter;
import static com.example.birdgame3.BirdGame3.ClassicEncounterStyle;
import static com.example.birdgame3.BirdGame3.MapVariant;
import static org.junit.jupiter.api.Assertions.*;

class TurkeyClassicRouteTest {
    @Test
    void turkeyHasTheApprovedEightEncounterLastFeastRoute() throws Exception {
        List<ClassicEncounter> route = route(new BirdGame3());

        assertEquals(8, route.size());
        assertEquals(List.of(
                        "First at the Table",
                        "The Swift Take",
                        "Poisoned Course",
                        "Open Season",
                        "Carving Blades",
                        "False Dawn",
                        "Bonus: Defend the Harvest",
                        "The Great Hunger"),
                route.stream().map(encounter -> encounter.name).toList());
        assertEquals(List.of(
                        MapVariant.STANDARD,
                        MapVariant.ROOFTOP_RELAY,
                        MapVariant.STANDARD,
                        MapVariant.TEMPEST_SUMMIT,
                        MapVariant.STANDARD,
                        MapVariant.ASHFALL_REBIRTH,
                        MapVariant.HARVEST_TRIBUNAL,
                        MapVariant.HARVEST_TRIBUNAL),
                route.stream().map(encounter -> encounter.variant).toList());
        assertEquals(ClassicEncounterStyle.FEAST_GAUNTLET, route.get(0).style);
        assertEquals(ClassicEncounterStyle.FEAST_GAUNTLET, route.get(1).style);
        assertEquals(ClassicEncounterStyle.GIANT, route.get(3).style);
        assertEquals(ClassicEncounterStyle.HARVEST_DEFENSE, route.get(6).style);
        assertEquals(ClassicEncounterStyle.DEVOURER_BOSS, route.get(7).style);
        assertTrue(route.getLast().bossFight);
    }

    @Test
    void authoredWaveGroupsMatchTheProposal() throws Exception {
        List<ClassicEncounter> route = route(new BirdGame3());

        assertEquals(3, route.get(0).waves.length);
        assertEquals(BirdType.PIGEON, route.get(0).waves[0][0].type());
        assertEquals(BirdType.KIWI, route.get(0).waves[1][0].type());
        assertEquals(BirdType.GOOSE, route.get(0).waves[2][0].type());
        assertEquals(2, route.get(1).waves.length);
        assertEquals(BirdType.ROADRUNNER, route.get(1).waves[0][0].type());
        assertEquals(BirdType.HUMMINGBIRD, route.get(1).waves[1][0].type());
        assertEquals(List.of(BirdType.HEISENBIRD, BirdType.OPIUMBIRD),
                List.of(route.get(2).enemies[0].type(), route.get(2).enemies[1].type()));
        assertEquals(BirdType.EAGLE, route.get(3).enemies[0].type());
        assertEquals(BirdType.SHOEBILL, route.get(4).waves[0][0].type());
        assertEquals(BirdType.RAZORBILL, route.get(4).waves[1][0].type());
        assertEquals(BirdType.ROOSTER, route.get(5).waves[0][0].type());
        assertEquals(BirdType.PHOENIX, route.get(5).waves[1][0].type());
        assertEquals(4, route.get(6).waves.length);
        assertEquals(BirdType.PELICAN, route.getLast().enemies[0].type());
    }

    @Test
    void openingGauntletUsesThreePlayerStocksWithoutPreDamagingItsWaves() throws Exception {
        BirdGame3 game = preparedGame();
        ClassicEncounter opening = route(game).getFirst();
        prepareEncounter(game, opening);
        invoke(game, "applyClassicEncounterStockOverrides", new Class<?>[0]);

        assertEquals(3, game.scores[0]);
        assertEquals(0.0, game.players[1].smashDamagePercent(), 0.0001);
    }

    @Test
    void swiftTakeFinishesWithATwoStockHummingbirdWave() throws Exception {
        BirdGame3 game = preparedGame();
        ClassicEncounter swiftTake = route(game).get(1);
        prepareEncounter(game, swiftTake);

        game.scores[1] = 0;
        assertTrue(game.holdClassicTurkeyEncounterOpen());
        invoke(game, "completeTurkeyFeastChoice", new Class<?>[]{boolean.class}, false);

        assertEquals(BirdType.HUMMINGBIRD, game.players[1].type);
        assertEquals(2, game.scores[1]);
        assertFalse(game.players[1].hasUltimate());
    }

    @Test
    void clearingAWaveOpensFeastChoiceAndFamineSpawnsTheNextWave() throws Exception {
        BirdGame3 game = preparedGame();
        ClassicEncounter first = route(game).getFirst();
        prepareEncounter(game, first);

        assertEquals(BirdType.PIGEON, game.players[1].type);
        game.scores[1] = 0;
        assertTrue(game.holdClassicTurkeyEncounterOpen());
        assertTrue((boolean) getField(game, "classicTurkeyFeastDecisionActive"));
        assertNotNull(getField(game, "classicHarvestPlate"));

        invoke(game, "completeTurkeyFeastChoice", new Class<?>[]{boolean.class}, false);

        assertEquals(1, getField(game, "classicTurkeyWaveIndex"));
        assertEquals(BirdType.KIWI, game.players[1].type);
        assertEquals(1, game.scores[1]);
        assertTrue((int) getField(game, "classicTurkeyFamineFrames") > 0);
    }

    @Test
    void harvestDefenseUsesSmallUltlessWavesAndHoldsTheMatchOpen() throws Exception {
        BirdGame3 game = preparedGame();
        ClassicEncounter defense = route(game).get(6);
        prepareEncounter(game, defense);

        assertEquals(3, game.activePlayers);
        assertFalse(game.players[1].hasUltimate());
        assertFalse(game.players[2].hasUltimate());
        assertEquals(0.62, game.players[1].sizeMultiplier, 0.0001);
        assertTrue(game.holdClassicTurkeyEncounterOpen());

        game.scores[1] = 0;
        game.scores[2] = 0;
        game.simTick = 720;
        game.applyTurkeyClassicRuntimeEffects();

        assertEquals(1, getField(game, "classicTurkeyDefenseWaveIndex"));
        assertEquals(BirdType.PIGEON, game.players[1].type);
        assertEquals(BirdType.HUMMINGBIRD, game.players[2].type);
        assertFalse(game.players[1].hasUltimate());
        assertFalse(game.players[2].hasUltimate());
    }

    @Test
    void devourerHasThreeStocksNoUltimateAndAThreePhaseCollapse() throws Exception {
        BirdGame3 game = preparedGame();
        ClassicEncounter boss = route(game).getLast();
        prepareEncounter(game, boss);
        invoke(game, "applyClassicEncounterStockOverrides", new Class<?>[0]);

        assertEquals(3, game.scores[1]);
        assertFalse(game.players[1].hasUltimate());
        assertEquals(1.72 * 0.76, game.players[1].sizeMultiplier, 0.0001);
        int openingPlatforms = game.platforms.size();

        game.scores[1] = 2;
        game.applyTurkeyClassicRuntimeEffects();
        assertTrue((boolean) getField(game, "classicDevourerSuctionPhaseActive"));

        game.scores[1] = 1;
        game.applyTurkeyClassicRuntimeEffects();
        assertTrue((boolean) getField(game, "classicDevourerFinalPhaseActive"));
        assertEquals(openingPlatforms - 2, game.platforms.size());
        assertEquals(1.26, game.players[1].sizeMultiplier, 0.0001);
    }

    @Test
    void harvestTribunalIsAStoryBackedUnlockableIslandAndRouteTitleIsAuthored() throws Exception {
        BirdGame3 game = preparedGame();
        game.selectedMap = BirdGame3.MapType.FOREST;
        game.selectedMapVariant = MapVariant.HARVEST_TRIBUNAL;
        invoke(game, "setupMatchArenaGeometry", new Class<?>[0]);
        invoke(game, "applySelectedMapVariantArena", new Class<?>[0]);

        assertTrue(game.usesIslandBoundsForCurrentArena());
        assertTrue(game.platforms.stream().anyMatch(platform -> platform.w == 1_860.0));
        assertFalse(game.availableStageChoices(BirdGame3.StageRandomPool.VARIANTS).stream()
                .anyMatch(choice -> choice.variant() == MapVariant.HARVEST_TRIBUNAL));
        setField(game, "harvestTribunalUnlocked", true);
        assertTrue(game.availableStageChoices(BirdGame3.StageRandomPool.VARIANTS).stream()
                .anyMatch(choice -> choice.variant() == MapVariant.HARVEST_TRIBUNAL));
        assertEquals(MapVariant.HARVEST_TRIBUNAL,
                StoryCampaignContent.create().mission("morning_line").mapVariant());
        assertEquals("THE LAST FEAST",
                invoke(game, "classicRouteTitle", new Class<?>[]{BirdType.class}, BirdType.TURKEY));
        assertEquals("Warpaint Turkey",
                invoke(game, "classicRewardFor", new Class<?>[]{BirdType.class}, BirdType.TURKEY));
    }

    private static void prepareEncounter(BirdGame3 game, ClassicEncounter encounter) throws Exception {
        game.classicEncounter = encounter;
        game.selectedMap = encounter.map;
        game.selectedMapVariant = encounter.variant;
        invoke(game, "setupClassicEncounterRoster", new Class<?>[]{ClassicEncounter.class}, encounter);
        invoke(game, "setupMatchArenaGeometry", new Class<?>[0]);
        invoke(game, "applySelectedMapVariantArena", new Class<?>[0]);
        for (int i = 0; i < game.activePlayers; i++) game.scores[i] = game.smashStartingStocks();
        invoke(game, "applyClassicEncounterArenaModifiers", new Class<?>[]{ClassicEncounter.class}, encounter);
        invoke(game, "positionClassicEncounterSpawns", new Class<?>[]{ClassicEncounter.class}, encounter);
    }

    private static BirdGame3 preparedGame() throws Exception {
        BirdGame3 game = new BirdGame3();
        game.classicModeActive = true;
        setField(game, "classicSelectedBird", BirdType.TURKEY);
        setField(game, "classicDifficulty", 5.0);
        return game;
    }

    @SuppressWarnings("unchecked")
    private static List<ClassicEncounter> route(BirdGame3 game) throws Exception {
        return (List<ClassicEncounter>) invoke(game, "buildClassicRun",
                new Class<?>[]{BirdType.class}, BirdType.TURKEY);
    }

    private static Object invoke(BirdGame3 game, String name, Class<?>[] parameterTypes, Object... args)
            throws Exception {
        Method method = BirdGame3.class.getDeclaredMethod(name, parameterTypes);
        method.setAccessible(true);
        return method.invoke(game, args);
    }

    private static Object getField(BirdGame3 game, String name) throws Exception {
        Field field = BirdGame3.class.getDeclaredField(name);
        field.setAccessible(true);
        return field.get(game);
    }

    private static void setField(BirdGame3 game, String name, Object value) throws Exception {
        Field field = BirdGame3.class.getDeclaredField(name);
        field.setAccessible(true);
        field.set(game, value);
    }
}
