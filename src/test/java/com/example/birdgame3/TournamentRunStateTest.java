package com.example.birdgame3;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;
import java.util.prefs.Preferences;

import static org.junit.jupiter.api.Assertions.*;

class TournamentRunStateTest {
    @Test
    void roundTripsNamesRulesSeedsAndBracketWinners() {
        TournamentRunState source = new TournamentRunState(
                1_725_000_000_000L, 4, 1, false, "CITY",
                VersusRules.competitive().withName("Finals | Rules").encode(),
                2, 1, 5, 742, false,
                List.of(
                        new TournamentRunState.Entry(1, true, "PIGEON", "PIGEON", "gold_skin", "Caleb | P1", 5),
                        new TournamentRunState.Entry(2, false, "EAGLE", "EAGLE", "", "CPU ~ Ace", 9),
                        new TournamentRunState.Entry(3, false, "", "FALCON", "", "", 7),
                        new TournamentRunState.Entry(4, false, "RAVEN", "RAVEN", "", "Night", 6)),
                List.of(3, 1, 4, 2), List.of(3, 2, 0));

        TournamentRunState restored = TournamentRunState.decode(source.encode());

        assertNotNull(restored);
        assertTrue(restored.usable());
        assertEquals(4, restored.entrantCount);
        assertEquals("Caleb | P1", restored.entries.getFirst().customName());
        assertEquals(List.of(3, 1, 4, 2), restored.seedOrder);
        assertEquals(List.of(3, 2, 0), restored.winners);
        assertEquals(742, restored.totalDamage);
        assertEquals("Finals | Rules", VersusRules.decode(restored.rules, null).name());
    }

    @Test
    void rejectsCorruptOrIncompletePayloads() {
        assertNull(TournamentRunState.decode(null));
        assertNull(TournamentRunState.decode("not-a-tournament"));

        TournamentRunState incomplete = new TournamentRunState(1, 8, 1, true, "FOREST", "",
                0, 0, 0, 0, false,
                List.of(new TournamentRunState.Entry(1, true, "PIGEON", "PIGEON", "", "", 5)),
                List.of(1), List.of());
        assertFalse(incomplete.usable());
        assertNull(TournamentRunState.decode(incomplete.encode()));
    }

    @Test
    void sanitizesUnsafeCountersAndDuplicateSeeds() {
        List<TournamentRunState.Entry> entries = List.of(
                new TournamentRunState.Entry(1, true, "PIGEON", "PIGEON", "", "", 99),
                new TournamentRunState.Entry(2, false, "EAGLE", "EAGLE", "", "", -2));
        TournamentRunState state = new TournamentRunState(-1, 2, 8, true, "FOREST", "",
                -4, 9, -1, -9, false, entries, List.of(1, 1, 2), List.of(2, 99));

        assertEquals(0, state.startedAtMillis);
        assertEquals(2, state.humanCount);
        assertEquals(0, state.completedMatches);
        assertEquals(0, state.simulatedMatches);
        assertEquals(List.of(1, 2), state.seedOrder);
        assertEquals(List.of(2, 0), state.winners);
        assertEquals(9, state.entries.getFirst().cpuLevel());
        assertEquals(1, state.entries.get(1).cpuLevel());
    }

    @Test
    void preferenceStoreChunksAndRemovesLargeRunSafely() throws Exception {
        Preferences prefs = Preferences.userRoot().node("/birdfight3-tournament-tests/" + UUID.randomUUID());
        try {
            String largeRules = "R".repeat(Preferences.MAX_VALUE_LENGTH + 250);
            List<TournamentRunState.Entry> entries = java.util.stream.IntStream.rangeClosed(1, 16)
                    .mapToObj(id -> new TournamentRunState.Entry(id, id == 1, "PIGEON", "PIGEON",
                            "skin_" + id, "Entrant " + id, 5)).toList();
            TournamentRunState source = new TournamentRunState(1, 16, 1, true, "FOREST", largeRules,
                    4, 2, 8, 1000, false, entries,
                    java.util.stream.IntStream.rangeClosed(1, 16).boxed().toList(), List.of());

            TournamentRunState.saveTo(prefs, source);
            TournamentRunState restored = TournamentRunState.loadFrom(prefs);
            assertNotNull(restored);
            assertEquals(16, restored.entries.size());
            assertEquals(4096, restored.rules.length());

            TournamentRunState.saveTo(prefs, null);
            assertNull(TournamentRunState.loadFrom(prefs));
        } finally {
            prefs.removeNode();
        }
    }
}
