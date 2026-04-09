package com.example.birdgame3;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

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
