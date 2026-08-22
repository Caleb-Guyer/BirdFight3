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

class HummingbirdClassicRouteTest {
    @Test
    void hummingbirdHasTheFixedEightEncounterBeatOfTheBloomRoute() throws Exception {
        List<ClassicEncounter> route = route(new BirdGame3());

        assertEquals(8, route.size());
        assertEquals(List.of(
                        "First Flutter",
                        "Too Big to Miss",
                        "Night Garden",
                        "Tailwind Team",
                        "Poison in the Pollen",
                        "Needle Against Spear",
                        "Bonus: Hundred-Flower Dash",
                        "The Bloom That Wouldn't Die"),
                route.stream().map(encounter -> encounter.name).toList());
        assertEquals(List.of(
                        MapVariant.STANDARD,
                        MapVariant.STANDARD,
                        MapVariant.STANDARD,
                        MapVariant.ROOFTOP_RELAY,
                        MapVariant.CARRION_THRONE,
                        MapVariant.STANDARD,
                        MapVariant.HEARTBLOOM_SANCTUARY,
                        MapVariant.HEARTBLOOM_SANCTUARY),
                route.stream().map(encounter -> encounter.variant).toList());
        assertEquals(ClassicEncounterStyle.GIANT, route.get(1).style);
        assertEquals(ClassicEncounterStyle.GIANT, route.get(5).style);
        assertEquals(ClassicEncounterStyle.NECTAR_DASH, route.get(6).style);
        assertEquals(ClassicEncounterStyle.BLIGHTWING_BOSS, route.get(7).style);
        assertTrue(route.getLast().bossFight);
    }

    @Test
    void authoredTeamsMatchTheApprovedRoute() throws Exception {
        List<ClassicEncounter> route = route(new BirdGame3());

        assertEquals(BirdType.TITMOUSE, route.get(0).enemies[0].type());
        assertEquals(BirdType.TURKEY, route.get(1).enemies[0].type());
        assertEquals(List.of(BirdType.BAT, BirdType.OPIUMBIRD),
                List.of(route.get(2).enemies[0].type(), route.get(2).enemies[1].type()));
        assertEquals(BirdType.ROADRUNNER, route.get(3).allies[0].type());
        assertEquals(List.of(BirdType.FALCON, BirdType.EAGLE),
                List.of(route.get(3).enemies[0].type(), route.get(3).enemies[1].type()));
        assertEquals(List.of(BirdType.HEISENBIRD, BirdType.VULTURE),
                List.of(route.get(4).enemies[0].type(), route.get(4).enemies[1].type()));
        assertEquals(BirdType.SHOEBILL, route.get(5).enemies[0].type());
        assertEquals(0, route.get(6).enemies.length);
        assertEquals(BirdType.RAVEN, route.get(7).enemies[0].type());
        assertEquals(BirdGame3.BLIGHTWING_RAVEN_SKIN, route.get(7).enemies[0].skinKey());
    }

    @Test
    void nectarChainBanksOnceAndPersistsAcrossEncounterRetry() throws Exception {
        BirdGame3 game = preparedGame();
        ClassicEncounter first = route(game).getFirst();
        setField(game, "classicRoundIndex", 0);
        game.classicEncounter = first;
        game.selectedMap = first.map;
        game.selectedMapVariant = first.variant;
        invoke(game, "setupClassicEncounterRoster", new Class<?>[]{ClassicEncounter.class}, first);
        invoke(game, "setupMatchArenaGeometry", new Class<?>[0]);
        invoke(game, "applyClassicEncounterArenaModifiers", new Class<?>[]{ClassicEncounter.class}, first);

        List<BirdGame3.ClassicNectarRing> rings = nectarRings(game);
        assertEquals(3, rings.size());
        Bird player = game.players[0];
        for (BirdGame3.ClassicNectarRing ring : List.copyOf(rings)) {
            player.x = ring.x - player.bodyWidth() * 0.5;
            player.y = ring.y - player.bodyHeight() * 0.5;
            invoke(game, "applyHummingbirdClassicRuntimeEffects", new Class<?>[0]);
        }
        assertEquals(1, game.classicHummingbirdBlossomCount());

        game.resetMatchStats();
        invoke(game, "applyClassicEncounterArenaModifiers", new Class<?>[]{ClassicEncounter.class}, first);
        assertTrue(nectarRings(game).isEmpty(), "a banked blossom must not be farmable on retry");
        assertEquals(1, game.classicHummingbirdBlossomCount());
    }

    @Test
    void hundredFlowerDashHasNoTargetsAndUsesItsOwnNonSmashCompletionRules() throws Exception {
        BirdGame3 game = preparedGame();
        ClassicEncounter dash = route(game).get(6);
        setField(game, "classicRoundIndex", 6);
        game.classicEncounter = dash;
        game.selectedMap = dash.map;
        game.selectedMapVariant = dash.variant;
        invoke(game, "setupClassicEncounterRoster", new Class<?>[]{ClassicEncounter.class}, dash);
        invoke(game, "setupMatchArenaGeometry", new Class<?>[0]);
        invoke(game, "applySelectedMapVariantArena", new Class<?>[0]);
        invoke(game, "applyClassicEncounterArenaModifiers", new Class<?>[]{ClassicEncounter.class}, dash);

        assertTrue(game.isClassicNectarDashActive());
        assertFalse(game.classicUsesSmashRules());
        assertNotNull(game.players[0]);
        assertNull(game.players[1]);
        assertEquals(12, nectarRings(game).size());
    }

    @Test
    void blightwingHasTwoStocksNoUltimateAndBankedFlowersBecomeRecoveryVents() throws Exception {
        BirdGame3 game = preparedGame();
        boolean[] blossoms = (boolean[]) getField(game, "classicHummingbirdBlossoms");
        blossoms[0] = true;
        blossoms[2] = true;
        blossoms[5] = true;
        ClassicEncounter boss = route(game).getLast();
        setField(game, "classicRoundIndex", 7);
        game.classicEncounter = boss;
        game.selectedMap = boss.map;
        game.selectedMapVariant = boss.variant;
        invoke(game, "setupClassicEncounterRoster", new Class<?>[]{ClassicEncounter.class}, boss);
        invoke(game, "setupMatchArenaGeometry", new Class<?>[0]);
        invoke(game, "applySelectedMapVariantArena", new Class<?>[0]);
        initializeStocks(game);
        invoke(game, "applyClassicEncounterStockOverrides", new Class<?>[0]);

        assertTrue(game.classicUsesSmashRules());
        assertEquals(3, game.scores[0]);
        assertEquals(2, game.scores[1]);
        assertEquals(3, game.windVents.size());
        assertEquals(1.55 * 0.94, game.players[1].sizeMultiplier, 0.0001);
        assertEquals(BirdGame3.BLIGHTWING_RAVEN_SKIN, game.players[1].appliedSkinKey);
        assertFalse(game.players[1].hasUltimate());
    }

    @Test
    void routeTitleAndRewardAreAuthored() throws Exception {
        BirdGame3 game = new BirdGame3();
        assertEquals("BEAT OF THE BLOOM",
                invoke(game, "classicRouteTitle", new Class<?>[]{BirdType.class}, BirdType.HUMMINGBIRD));
        assertEquals("Prismatic Courier Hummingbird",
                invoke(game, "classicRewardFor", new Class<?>[]{BirdType.class}, BirdType.HUMMINGBIRD));
    }

    private static void initializeStocks(BirdGame3 game) {
        for (int i = 0; i < game.activePlayers; i++) game.scores[i] = game.smashStartingStocks();
    }

    private static BirdGame3 preparedGame() throws Exception {
        BirdGame3 game = new BirdGame3();
        game.classicModeActive = true;
        setField(game, "classicSelectedBird", BirdType.HUMMINGBIRD);
        setField(game, "classicDifficulty", 5.0);
        return game;
    }

    @SuppressWarnings("unchecked")
    private static List<ClassicEncounter> route(BirdGame3 game) throws Exception {
        return (List<ClassicEncounter>) invoke(game, "buildClassicRun",
                new Class<?>[]{BirdType.class}, BirdType.HUMMINGBIRD);
    }

    @SuppressWarnings("unchecked")
    private static List<BirdGame3.ClassicNectarRing> nectarRings(BirdGame3 game) throws Exception {
        return (List<BirdGame3.ClassicNectarRing>) getField(game, "classicNectarRings");
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
