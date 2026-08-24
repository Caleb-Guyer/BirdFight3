package com.example.birdgame3;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class BirdGame3SquadStrikeTest {
    @Test
    void setupProvidesOrderedThreeAndFiveBirdSquadsWithoutDuplicates() throws Exception {
        BirdGame3 game = preparedGame(5, BirdGame3.SquadStrikeFormat.ELIMINATION);

        @SuppressWarnings("unchecked")
        List<BirdGame3.SquadStrikeEntry> a = (List<BirdGame3.SquadStrikeEntry>) get(game, "squadStrikeTeamA");
        @SuppressWarnings("unchecked")
        List<BirdGame3.SquadStrikeEntry> b = (List<BirdGame3.SquadStrikeEntry>) get(game, "squadStrikeTeamB");
        Set<BirdGame3.BirdType> used = new HashSet<>();
        for (int i = 0; i < 5; i++) {
            assertNotNull(a.get(i).resolvedType);
            assertNotNull(b.get(i).resolvedType);
            assertTrue(used.add(a.get(i).resolvedType));
            assertTrue(used.add(b.get(i).resolvedType));
        }
        assertEquals(5, game.squadStrikeActiveTeam(0).size());
        assertEquals(5, game.squadStrikeActiveTeam(1).size());
    }

    @Test
    void eliminationKeepsWinnerAndAdvancesOnlyLoser() throws Exception {
        BirdGame3 game = preparedGame(3, BirdGame3.SquadStrikeFormat.ELIMINATION);

        game.recordSquadStrikeWinner(0, null, true);

        assertEquals(0, get(game, "squadStrikeTeamAIndex"));
        assertEquals(1, get(game, "squadStrikeTeamBIndex"));
        assertFalse((boolean) get(game, "squadStrikeComplete"));

        set(game, "squadStrikeMatchResolved", false);
        game.recordSquadStrikeWinner(0, null, true);
        set(game, "squadStrikeMatchResolved", false);
        game.recordSquadStrikeWinner(0, null, true);

        assertTrue((boolean) get(game, "squadStrikeComplete"));
        assertEquals(0, get(game, "squadStrikeChampionTeam"));
        assertEquals(List.of(0, 0, 0), get(game, "squadStrikeWinnerHistory"));
    }

    @Test
    void relayDoesNotCarryWinnerDamageOrStocks() throws Exception {
        BirdGame3 game = preparedGame(3, BirdGame3.SquadStrikeFormat.RELAY);
        set(game, "squadStrikeCarryTeam", 1);
        set(game, "squadStrikeCarryStocks", 2);
        set(game, "squadStrikeCarryHealth", 40.0);

        game.recordSquadStrikeWinner(1, null, false);

        assertEquals(-1, get(game, "squadStrikeCarryTeam"));
        assertEquals(0, get(game, "squadStrikeCarryStocks"));
        assertEquals(0.0, get(game, "squadStrikeCarryHealth"));
        assertEquals(1, get(game, "squadStrikeTeamAIndex"));
        assertEquals(0, get(game, "squadStrikeTeamBIndex"));
    }

    @Test
    void bestOfRotatesBothTeamsAndClinchesAtMajority() throws Exception {
        BirdGame3 game = preparedGame(5, BirdGame3.SquadStrikeFormat.BEST_OF);

        game.recordSquadStrikeWinner(1, null, true);
        set(game, "squadStrikeMatchResolved", false);
        game.recordSquadStrikeWinner(0, null, true);
        set(game, "squadStrikeMatchResolved", false);
        game.recordSquadStrikeWinner(1, null, true);
        set(game, "squadStrikeMatchResolved", false);
        game.recordSquadStrikeWinner(1, null, true);

        assertEquals(4, get(game, "squadStrikeTeamAIndex"));
        assertEquals(4, get(game, "squadStrikeTeamBIndex"));
        assertEquals(1, get(game, "squadStrikeTeamAWins"));
        assertEquals(3, get(game, "squadStrikeTeamBWins"));
        assertEquals(3, game.squadStrikeTargetWins());
        assertTrue((boolean) get(game, "squadStrikeComplete"));
        assertEquals(1, get(game, "squadStrikeChampionTeam"));
    }

    @Test
    void runCheckpointRestoresFormatRostersProgressRulesAndSeed() throws Exception {
        BirdGame3 source = preparedGame(3, BirdGame3.SquadStrikeFormat.ELIMINATION);
        set(source, "squadStrikeSeed", 445566L);
        set(source, "squadStrikeMapRandom", false);
        set(source, "squadStrikeFixedMap", BirdGame3.MapType.CITY);
        set(source, "squadStrikeRules", VersusRules.chaos().withStockCount(4).withSeriesWins(1));
        source.recordSquadStrikeWinner(0, null, true);

        SquadStrikeRunState state = (SquadStrikeRunState) invoke(source, "captureSquadStrikeRunState");
        BirdGame3 restored = new BirdGame3();
        Method restore = BirdGame3.class.getDeclaredMethod("restoreSquadStrikeRunState", SquadStrikeRunState.class);
        restore.setAccessible(true);

        assertTrue((boolean) restore.invoke(restored, state));
        assertEquals(BirdGame3.SquadStrikeFormat.ELIMINATION, get(restored, "squadStrikeFormat"));
        assertEquals(1, get(restored, "squadStrikeTeamBIndex"));
        assertEquals(445566L, get(restored, "squadStrikeSeed"));
        assertEquals(BirdGame3.MapType.CITY, get(restored, "squadStrikeFixedMap"));
        assertEquals(4, ((VersusRules) get(restored, "squadStrikeRules")).stockCount());
        assertTrue(restored.squadStrikeActiveTeam(0).stream().allMatch(entry -> entry.resolvedType != null));
    }

    @Test
    void squadStrikeUsesSmashRulesAndKeepsReplaysEligible() throws Exception {
        BirdGame3 game = preparedGame(3, BirdGame3.SquadStrikeFormat.RELAY);
        VersusRules rules = VersusRules.chaos().withStockCount(5).withSeriesWins(1);
        set(game, "squadStrikeRules", rules);
        Field flowField = BirdGame3.class.getDeclaredField("frontEndMatchFlow");
        flowField.setAccessible(true);
        ((FrontEndMatchFlow) flowField.get(game)).selectCustomRules(rules);

        assertTrue(game.appliesVersusRules());
        assertEquals(5, game.smashStartingStocks());
        assertTrue((boolean) invoke(game, "replayEligibleMatch"));
    }

    private static BirdGame3 preparedGame(int size, BirdGame3.SquadStrikeFormat format) throws Exception {
        BirdGame3 game = new BirdGame3();
        set(game, "squadStrikeSize", size);
        set(game, "squadStrikeFormat", format);
        invoke(game, "ensureSquadStrikeEntries");
        set(game, "squadStrikeSeed", 123456789L);
        invoke(game, "resolveSquadStrikeBirds");
        set(game, "squadStrikeModeActive", true);
        return game;
    }

    private static Object invoke(BirdGame3 game, String method) throws Exception {
        Method target = BirdGame3.class.getDeclaredMethod(method);
        target.setAccessible(true);
        return target.invoke(game);
    }

    private static Object get(BirdGame3 game, String field) throws Exception {
        Field target = BirdGame3.class.getDeclaredField(field);
        target.setAccessible(true);
        return target.get(game);
    }

    private static void set(BirdGame3 game, String field, Object value) throws Exception {
        Field target = BirdGame3.class.getDeclaredField(field);
        target.setAccessible(true);
        target.set(game, value);
    }
}
