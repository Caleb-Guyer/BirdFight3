package com.example.birdgame3;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;

import static com.example.birdgame3.BirdGame3.BirdType;
import static com.example.birdgame3.BirdGame3.ClassicEncounter;
import static com.example.birdgame3.BirdGame3.ClassicEncounterStyle;
import static com.example.birdgame3.BirdGame3.MapVariant;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PhoenixClassicRouteTest {
    @Test
    void phoenixHasTheFixedEightEncounterFlameThatReturnsRoute() throws Exception {
        List<ClassicEncounter> route = phoenixRoute(new BirdGame3());

        assertEquals(8, route.size());
        assertEquals(List.of(
                        "Small Sparks",
                        "Cold Front",
                        "Volatile Mixture",
                        "The Deluge",
                        "False Suns",
                        "Ashen Reflection",
                        "Bonus: Rebirth Relay",
                        "The Long Winter"),
                route.stream().map(encounter -> encounter.name).toList());
        assertEquals(List.of(
                        MapVariant.STANDARD,
                        MapVariant.STANDARD,
                        MapVariant.STANDARD,
                        MapVariant.TITAN_DOCK,
                        MapVariant.STANDARD,
                        MapVariant.ASHFALL_REBIRTH,
                        MapVariant.STANDARD,
                        MapVariant.FROZEN_CALDERA),
                route.stream().map(encounter -> encounter.variant).toList());
        assertEquals(ClassicEncounterStyle.MINIATURE_FLOCK, route.get(0).style);
        assertEquals(ClassicEncounterStyle.GIANT, route.get(3).style);
        assertEquals(ClassicEncounterStyle.PHOENIX_REBIRTH, route.get(5).style);
        assertEquals(ClassicEncounterStyle.BONUS_RELAY, route.get(6).style);
        assertEquals(ClassicEncounterStyle.LONG_WINTER_BOSS, route.get(7).style);
        assertTrue(route.getLast().bossFight);
    }

    @Test
    void authoredTeamsMatchTheApprovedRoute() throws Exception {
        List<ClassicEncounter> route = phoenixRoute(new BirdGame3());

        assertTrue(List.of(route.get(0).enemies).stream()
                .allMatch(fighter -> fighter.type() == BirdType.HUMMINGBIRD));
        assertEquals(List.of(BirdType.PENGUIN, BirdType.GRINCHHAWK),
                List.of(route.get(1).enemies[0].type(), route.get(1).enemies[1].type()));
        assertEquals(BirdType.FALCON, route.get(2).allies[0].type());
        assertEquals(List.of(BirdType.HEISENBIRD, BirdType.OPIUMBIRD),
                List.of(route.get(2).enemies[0].type(), route.get(2).enemies[1].type()));
        assertEquals(BirdType.PELICAN, route.get(3).enemies[0].type());
        assertEquals(List.of(BirdType.ROOSTER, BirdType.TURKEY),
                List.of(route.get(4).enemies[0].type(), route.get(4).enemies[1].type()));
        assertEquals(BirdType.PHOENIX, route.get(5).enemies[0].type());
        assertEquals(BirdType.GRINCHHAWK, route.get(7).enemies[0].type());
        assertTrue(route.get(7).enemies[0].title().contains("Winter King"));
    }

    @Test
    void minionsHaveNoUltimatesAndDelugeUsesGiantRules() throws Exception {
        BirdGame3 game = preparedPhoenixGame();
        List<ClassicEncounter> route = phoenixRoute(game);

        ClassicEncounter sparks = route.get(0);
        game.classicEncounter = sparks;
        invoke(game, "setupClassicEncounterRoster", new Class<?>[]{ClassicEncounter.class}, sparks);
        for (int i = 1; i < game.activePlayers; i++) {
            assertFalse(game.players[i].hasUltimate());
            assertTrue(game.players[i].sizeMultiplier < 0.8);
        }

        ClassicEncounter deluge = route.get(3);
        game.classicEncounter = deluge;
        invoke(game, "setupClassicEncounterRoster", new Class<?>[]{ClassicEncounter.class}, deluge);
        assertTrue(game.players[1].sizeMultiplier >= 1.5);
        assertTrue(game.players[1].isIroncladSkin);
    }

    @Test
    void ashenReflectionAndWinterKingUseSmashStocks() throws Exception {
        BirdGame3 game = preparedPhoenixGame();
        List<ClassicEncounter> route = phoenixRoute(game);

        ClassicEncounter reflection = route.get(5);
        game.classicEncounter = reflection;
        invoke(game, "setupClassicEncounterRoster", new Class<?>[]{ClassicEncounter.class}, reflection);
        initializeStocks(game);
        invoke(game, "applyClassicEncounterStockOverrides", new Class<?>[0]);
        assertTrue(game.classicUsesSmashRules());
        assertEquals(2, game.scores[0]);
        assertEquals(2, game.scores[1]);
        assertTrue(game.players[1].isAshenSovereignSkin);

        ClassicEncounter winter = route.get(7);
        setField(game, "classicRoundIndex", 7);
        game.classicEncounter = winter;
        invoke(game, "setupClassicEncounterRoster", new Class<?>[]{ClassicEncounter.class}, winter);
        initializeStocks(game);
        invoke(game, "applyClassicEncounterStockOverrides", new Class<?>[0]);
        assertTrue(game.classicUsesSmashRules());
        assertEquals(3, game.scores[0]);
        assertEquals(3, game.scores[1]);
        assertFalse(game.players[1].hasUltimate());
        assertEquals(BirdGame3.WINTER_KING_GRINCHHAWK_SKIN, game.players[1].appliedSkinKey);
    }

    @Test
    void rebirthRelayPositionsTargetsAndCollapsesOnlyItsCinderLedges() throws Exception {
        BirdGame3 game = preparedPhoenixGame();
        ClassicEncounter relay = phoenixRoute(game).get(6);
        game.classicEncounter = relay;
        game.selectedMap = relay.map;
        game.selectedMapVariant = relay.variant;
        invoke(game, "setupClassicEncounterRoster", new Class<?>[]{ClassicEncounter.class}, relay);
        invoke(game, "setupMatchArenaGeometry", new Class<?>[0]);
        invoke(game, "applySelectedMapVariantArena", new Class<?>[0]);
        invoke(game, "applyClassicEncounterArenaModifiers", new Class<?>[]{ClassicEncounter.class}, relay);
        invoke(game, "positionClassicEncounterSpawns", new Class<?>[]{ClassicEncounter.class}, relay);

        assertEquals(3, game.activePlayers - 1);
        assertTrue(game.players[1].x < game.players[2].x);
        assertTrue(game.players[2].x < game.players[3].x);
        assertTrue(game.players[2].y < game.players[1].y);
        for (int i = 1; i < 4; i++) {
            assertTrue(game.players[i].classicBonusTarget);
            assertFalse(game.players[i].hasUltimate());
        }
        assertTrue(hasPlatformAt(game, 1_820.0));
        assertTrue(hasPlatformAt(game, 2_840.0));
        assertTrue(hasPlatformAt(game, 3_860.0));

        game.simTick = 900;
        invoke(game, "applyPhoenixClassicRuntimeEffects", new Class<?>[0]);
        assertFalse(hasPlatformAt(game, 1_820.0));
        assertTrue(hasPlatformAt(game, 2_840.0));

        game.simTick = 2_700;
        invoke(game, "applyPhoenixClassicRuntimeEffects", new Class<?>[0]);
        assertFalse(hasPlatformAt(game, 2_840.0));
        assertFalse(hasPlatformAt(game, 3_860.0));
    }

    @Test
    void longWinterPhasesDismountTheSleighAndEndInIceArmor() throws Exception {
        BirdGame3 game = preparedPhoenixGame();
        ClassicEncounter winter = phoenixRoute(game).getLast();
        game.classicEncounter = winter;
        game.selectedMap = winter.map;
        game.selectedMapVariant = winter.variant;
        invoke(game, "setupClassicEncounterRoster", new Class<?>[]{ClassicEncounter.class}, winter);
        initializeStocks(game);
        invoke(game, "applyClassicEncounterStockOverrides", new Class<?>[0]);
        invoke(game, "setupMatchArenaGeometry", new Class<?>[0]);
        invoke(game, "applySelectedMapVariantArena", new Class<?>[0]);
        int baseVents = game.windVents.size();
        Bird winterKing = game.players[1];

        winterKing.grinchSleighActive = true;
        winterKing.grinchSleighRiding = true;
        winterKing.vy = 0.0;
        game.scores[1] = 2;
        invoke(game, "applyPhoenixClassicRuntimeEffects", new Class<?>[0]);
        assertFalse(winterKing.grinchSleighRiding);
        assertTrue(winterKing.grinchSleighActive);
        assertTrue(winterKing.grinchSleighTimer > 0);
        assertTrue(winterKing.vy >= BirdGame3.GRAVITY);
        assertEquals(baseVents + 2, game.windVents.size());

        game.scores[1] = 1;
        invoke(game, "applyPhoenixClassicRuntimeEffects", new Class<?>[0]);
        assertEquals(1.62, winterKing.sizeMultiplier, 0.0001);
        assertTrue(game.isLongWinterFinalForm(winterKing));
        assertFalse(winterKing.hasUltimate());

        invoke(game, "applyPhoenixClassicRuntimeEffects", new Class<?>[0]);
        assertEquals(baseVents + 2, game.windVents.size());
        assertEquals(1.62, winterKing.sizeMultiplier, 0.0001);
    }

    @Test
    void phoenixRouteTitleAndRewardAreAuthored() throws Exception {
        BirdGame3 game = new BirdGame3();
        assertEquals("THE FLAME THAT RETURNS",
                invoke(game, "classicRouteTitle", new Class<?>[]{BirdType.class}, BirdType.PHOENIX));
        assertEquals("Dawnfire Phoenix",
                invoke(game, "classicRewardFor", new Class<?>[]{BirdType.class}, BirdType.PHOENIX));
    }

    private static boolean hasPlatformAt(BirdGame3 game, double x) {
        return game.platforms.stream().anyMatch(platform -> Math.abs(platform.x - x) < 0.01
                && Math.abs(platform.w - 320.0) < 0.01);
    }

    private static void initializeStocks(BirdGame3 game) {
        for (int i = 0; i < game.activePlayers; i++) {
            game.scores[i] = game.smashStartingStocks();
        }
    }

    private static BirdGame3 preparedPhoenixGame() throws Exception {
        BirdGame3 game = new BirdGame3();
        game.classicModeActive = true;
        setField(game, "classicSelectedBird", BirdType.PHOENIX);
        setField(game, "classicDifficulty", 5.0);
        return game;
    }

    @SuppressWarnings("unchecked")
    private static List<ClassicEncounter> phoenixRoute(BirdGame3 game) throws Exception {
        Method build = BirdGame3.class.getDeclaredMethod("buildClassicRun", BirdType.class);
        build.setAccessible(true);
        return (List<ClassicEncounter>) build.invoke(game, BirdType.PHOENIX);
    }

    private static Object invoke(BirdGame3 game, String name, Class<?>[] parameterTypes, Object... arguments)
            throws Exception {
        Method method = BirdGame3.class.getDeclaredMethod(name, parameterTypes);
        method.setAccessible(true);
        return method.invoke(game, arguments);
    }

    private static void setField(BirdGame3 game, String name, Object value) throws Exception {
        Field field = BirdGame3.class.getDeclaredField(name);
        field.setAccessible(true);
        field.set(game, value);
    }
}
