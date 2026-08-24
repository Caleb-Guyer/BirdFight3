package com.example.birdgame3;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BirdGame3TournamentTest {
    @Test
    void buildTournamentBracketUsesStandardSeedOrdering() throws Exception {
        BirdGame3 game = new BirdGame3();
        setPrivateField(game, "tournamentEntrantCount", 8);

        invoke(game, "ensureTournamentEntries");
        invoke(game, "buildTournamentBracket");

        @SuppressWarnings("unchecked")
        List<List<BirdGame3.TournamentMatch>> rounds =
                (List<List<BirdGame3.TournamentMatch>>) getPrivateField(game, "tournamentRounds");
        List<BirdGame3.TournamentMatch> round0 = rounds.getFirst();

        assertEquals(1, round0.get(0).a.id);
        assertEquals(8, round0.get(0).b.id);
        assertEquals(4, round0.get(1).a.id);
        assertEquals(5, round0.get(1).b.id);
        assertEquals(2, round0.get(2).a.id);
        assertEquals(7, round0.get(2).b.id);
        assertEquals(3, round0.get(3).a.id);
        assertEquals(6, round0.get(3).b.id);
    }

    @Test
    void resolveTournamentEntryBirdLocksRandomPickUntilReset() throws Exception {
        BirdGame3 game = new BirdGame3();
        setPrivateField(game, "tournamentEntrantCount", 2);

        invoke(game, "ensureTournamentEntries");

        @SuppressWarnings("unchecked")
        List<BirdGame3.TournamentEntry> entries =
                (List<BirdGame3.TournamentEntry>) getPrivateField(game, "tournamentEntries");
        BirdGame3.TournamentEntry entry = entries.getFirst();

        Method resolve = BirdGame3.class.getDeclaredMethod("resolveTournamentEntryBird", BirdGame3.TournamentEntry.class);
        resolve.setAccessible(true);

        BirdGame3.BirdType first = (BirdGame3.BirdType) resolve.invoke(game, entry);
        BirdGame3.BirdType second = (BirdGame3.BirdType) resolve.invoke(game, entry);

        assertEquals(first, second);
        assertEquals(first, entry.resolvedType);

        invoke(game, "resetTournamentRun");
        assertNull(entry.resolvedType);
    }

    @Test
    void buildTournamentBracketUsesCustomSeedOrderWhenPresent() throws Exception {
        BirdGame3 game = new BirdGame3();
        setPrivateField(game, "tournamentEntrantCount", 4);

        invoke(game, "ensureTournamentEntries");

        @SuppressWarnings("unchecked")
        List<BirdGame3.TournamentEntry> entries =
                (List<BirdGame3.TournamentEntry>) getPrivateField(game, "tournamentEntries");
        @SuppressWarnings("unchecked")
        List<BirdGame3.TournamentEntry> seedOrder =
                (List<BirdGame3.TournamentEntry>) getPrivateField(game, "tournamentSeedOrder");
        seedOrder.clear();
        seedOrder.addAll(List.of(entries.get(2), entries.get(0), entries.get(3), entries.get(1)));

        invoke(game, "buildTournamentBracket");

        @SuppressWarnings("unchecked")
        List<List<BirdGame3.TournamentMatch>> rounds =
                (List<List<BirdGame3.TournamentMatch>>) getPrivateField(game, "tournamentRounds");
        List<BirdGame3.TournamentMatch> round0 = rounds.getFirst();

        assertEquals(3, round0.get(0).a.id);
        assertEquals(2, round0.get(0).b.id);
        assertEquals(1, round0.get(1).a.id);
        assertEquals(4, round0.get(1).b.id);
    }

    @Test
    void tournamentSkinChoiceStaysCompatibleWithSelectedBird() throws Exception {
        BirdGame3 game = new BirdGame3();
        setPrivateField(game, "tournamentEntrantCount", 2);

        invoke(game, "ensureTournamentEntries");

        @SuppressWarnings("unchecked")
        List<BirdGame3.TournamentEntry> entries =
                (List<BirdGame3.TournamentEntry>) getPrivateField(game, "tournamentEntries");
        BirdGame3.TournamentEntry entry = entries.getFirst();

        game.setTournamentEntrySelection(entry, BirdGame3.BirdType.EAGLE);
        game.cycleTournamentEntrySkin(entry);
        assertEquals("SKIN: STOCK PHOTO", game.tournamentEntrySkinLabel(entry, BirdGame3.BirdType.EAGLE));

        game.setTournamentEntrySelection(entry, BirdGame3.BirdType.TURKEY);

        assertEquals("SKIN: BASE", game.tournamentEntrySkinLabel(entry, BirdGame3.BirdType.TURKEY));
    }

    @Test
    void tournamentCpuLevelCyclesPerEntry() throws Exception {
        BirdGame3 game = new BirdGame3();
        setPrivateField(game, "tournamentEntrantCount", 2);

        invoke(game, "ensureTournamentEntries");

        @SuppressWarnings("unchecked")
        List<BirdGame3.TournamentEntry> entries =
                (List<BirdGame3.TournamentEntry>) getPrivateField(game, "tournamentEntries");
        BirdGame3.TournamentEntry entry = entries.getFirst();

        assertEquals(5, game.tournamentEntryCpuLevel(entry));

        game.cycleTournamentEntryCpuLevel(entry);
        assertEquals(6, game.tournamentEntryCpuLevel(entry));

        entry.cpuLevel = 9;
        game.cycleTournamentEntryCpuLevel(entry);
        assertEquals(1, game.tournamentEntryCpuLevel(entry));
    }

    @Test
    void setupFieldSizesCycleOnlyThroughFourEightAndSixteen() {
        assertEquals(4, BirdGame3.adjacentTournamentFieldSize(4, -1));
        assertEquals(8, BirdGame3.adjacentTournamentFieldSize(4, 1));
        assertEquals(4, BirdGame3.adjacentTournamentFieldSize(8, -1));
        assertEquals(16, BirdGame3.adjacentTournamentFieldSize(8, 1));
        assertEquals(8, BirdGame3.adjacentTournamentFieldSize(16, -1));
        assertEquals(16, BirdGame3.adjacentTournamentFieldSize(16, 1));
    }

    @Test
    void tournamentRunRestoresSeedsResolvedBirdsAndAdvancement() throws Exception {
        BirdGame3 source = new BirdGame3();
        setPrivateField(source, "tournamentEntrantCount", 4);
        setPrivateField(source, "tournamentHumanCount", 1);
        invoke(source, "ensureTournamentEntries");
        invoke(source, "syncTournamentEntries");

        @SuppressWarnings("unchecked")
        List<BirdGame3.TournamentEntry> entries =
                (List<BirdGame3.TournamentEntry>) getPrivateField(source, "tournamentEntries");
        entries.get(0).customName = "Local Champion";
        source.setTournamentEntrySelection(entries.get(0), BirdGame3.BirdType.PIGEON);
        source.setTournamentEntrySelection(entries.get(1), BirdGame3.BirdType.EAGLE);
        source.setTournamentEntrySelection(entries.get(2), BirdGame3.BirdType.FALCON);
        source.setTournamentEntrySelection(entries.get(3), BirdGame3.BirdType.RAVEN);
        invoke(source, "resolveTournamentEntryBirds");
        setPrivateField(source, "tournamentModeActive", true);
        setPrivateField(source, "tournamentStartedAtMillis", 123456L);
        invoke(source, "buildTournamentBracket");

        @SuppressWarnings("unchecked")
        List<List<BirdGame3.TournamentMatch>> sourceRounds =
                (List<List<BirdGame3.TournamentMatch>>) getPrivateField(source, "tournamentRounds");
        source.recordTournamentWinner(sourceRounds.getFirst().get(0), sourceRounds.getFirst().get(0).a);
        source.recordTournamentWinner(sourceRounds.getFirst().get(1), sourceRounds.getFirst().get(1).b);

        Method capture = BirdGame3.class.getDeclaredMethod("captureTournamentRunState");
        capture.setAccessible(true);
        TournamentRunState state = (TournamentRunState) capture.invoke(source);

        BirdGame3 restored = new BirdGame3();
        Method restore = BirdGame3.class.getDeclaredMethod("restoreTournamentRunState", TournamentRunState.class);
        restore.setAccessible(true);
        assertTrue((boolean) restore.invoke(restored, state));

        @SuppressWarnings("unchecked")
        List<List<BirdGame3.TournamentMatch>> restoredRounds =
                (List<List<BirdGame3.TournamentMatch>>) getPrivateField(restored, "tournamentRounds");
        assertEquals(1, restoredRounds.getFirst().get(0).winner.id);
        assertEquals(3, restoredRounds.getFirst().get(1).winner.id);
        assertEquals(1, restoredRounds.get(1).getFirst().a.id);
        assertEquals(3, restoredRounds.get(1).getFirst().b.id);
        assertEquals("Local Champion", restored.tournamentEntries().getFirst().customName);
        assertEquals(BirdGame3.BirdType.PIGEON, restored.tournamentEntries().getFirst().resolvedType);
        assertFalse(restored.isTournamentComplete());
    }

    @Test
    void tournamentMatchesUseTheirSelectedSmashRules() throws Exception {
        BirdGame3 game = new BirdGame3();
        setPrivateField(game, "tournamentModeActive", true);
        VersusRules rules = VersusRules.chaos().withStockCount(5).withSeriesWins(4);
        setPrivateField(game, "tournamentRules", rules.withSeriesWins(1));
        Field flowField = BirdGame3.class.getDeclaredField("frontEndMatchFlow");
        flowField.setAccessible(true);
        FrontEndMatchFlow flow = (FrontEndMatchFlow) flowField.get(game);
        flow.selectCustomRules(rules.withSeriesWins(1));

        assertTrue(game.appliesVersusRules());
        assertEquals(5, game.smashStartingStocks());
        assertEquals(1, game.versusSeriesWinsRequired());
        assertTrue(game.versusStageHazardsEnabled());
    }

    private static void invoke(BirdGame3 game, String methodName) throws Exception {
        Method method = BirdGame3.class.getDeclaredMethod(methodName);
        method.setAccessible(true);
        method.invoke(game);
    }

    private static void setPrivateField(BirdGame3 game, String fieldName, Object value) throws Exception {
        Field field = BirdGame3.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(game, value);
    }

    private static Object getPrivateField(BirdGame3 game, String fieldName) throws Exception {
        Field field = BirdGame3.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        return field.get(game);
    }
}
