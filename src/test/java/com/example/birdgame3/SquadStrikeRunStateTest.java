package com.example.birdgame3;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;
import java.util.prefs.Preferences;

import static org.junit.jupiter.api.Assertions.*;

class SquadStrikeRunStateTest {
    private static List<SquadStrikeRunState.Fighter> team(String prefix, int size) {
        return java.util.stream.IntStream.range(0, size)
                .mapToObj(i -> new SquadStrikeRunState.Fighter(i, prefix + i, prefix + i, "skin-" + i))
                .toList();
    }

    @Test
    void roundTripsFormatRostersCarryAndProgress() {
        SquadStrikeRunState source = new SquadStrikeRunState(
                1234L, 9876L, "ELIMINATION", 3, true, false, 5, 8,
                false, "CITY", VersusRules.competitive().encode(),
                1, 2, 0, 0, 3, 1, 7, 840,
                0, 2, 73.5, false, -1, false,
                team("A", 3), team("B", 3), List.of(0, 1, 0));

        SquadStrikeRunState restored = SquadStrikeRunState.decode(source.encode());

        assertNotNull(restored);
        assertTrue(restored.usable());
        assertEquals("ELIMINATION", restored.format);
        assertEquals(3, restored.squadSize);
        assertEquals(73.5, restored.carryHealth);
        assertEquals(List.of(0, 1, 0), restored.winnerHistory);
        assertEquals("skin-2", restored.teamB.get(2).skinKey());
        assertEquals("COMPETITIVE", VersusRules.decode(restored.rules, null).name());
    }

    @Test
    void rejectsCorruptIncompleteAndUnknownFormats() {
        assertNull(SquadStrikeRunState.decode(null));
        assertNull(SquadStrikeRunState.decode("broken"));
        SquadStrikeRunState incomplete = new SquadStrikeRunState(1, 2, "RELAY", 5,
                true, false, 5, 5, true, "FOREST", "", 0, 0, 0, 0,
                0, 0, 0, 0, -1, 0, 0, false, -1, false,
                team("A", 3), team("B", 3), List.of());
        assertFalse(incomplete.usable());
        assertNull(SquadStrikeRunState.decode(incomplete.encode()));
        SquadStrikeRunState unknown = new SquadStrikeRunState(1, 2, "UNKNOWN", 3,
                true, false, 5, 5, true, "FOREST", "", 0, 0, 0, 0,
                0, 0, 0, 0, -1, 0, 0, false, -1, false,
                team("A", 3), team("B", 3), List.of());
        assertFalse(unknown.usable());
    }

    @Test
    void sanitizesUnsafeCountersAndHistory() {
        SquadStrikeRunState state = new SquadStrikeRunState(-1, 4, "BEST_OF", 3,
                true, false, -5, 99, true, "FOREST", "", 99, -2, 88, -4,
                2, 8, -3, -9, 7, -5, -10, true, 9, true,
                team("A", 3), team("B", 3), List.of(0, 7, 1, 1));

        assertEquals(0, state.startedAtMillis);
        assertEquals(1, state.teamACpuLevel);
        assertEquals(9, state.teamBCpuLevel);
        assertEquals(3, state.teamAIndex);
        assertEquals(0, state.teamBIndex);
        assertEquals(-1, state.carryTeam);
        assertEquals(List.of(0, 1), state.winnerHistory);
        assertFalse(state.usable(), "a completed checkpoint without a valid champion must be rejected");
    }

    @Test
    void preferenceStoreIsProfileScopedAndRemovable() throws Exception {
        Preferences prefs = Preferences.userRoot().node("/birdfight3-squad-strike-tests/" + UUID.randomUUID());
        try {
            SquadStrikeRunState source = new SquadStrikeRunState(1, 2, "RELAY", 5,
                    true, false, 5, 7, true, "FOREST", "R".repeat(Preferences.MAX_VALUE_LENGTH + 200),
                    1, 2, 0, 0, 3, 2, 4, 500, -1, 0, 0,
                    false, -1, false, team("A", 5), team("B", 5), List.of(0, 1, 0));
            SquadStrikeRunState.saveTo(prefs, source);
            SquadStrikeRunState restored = SquadStrikeRunState.loadFrom(prefs);
            assertNotNull(restored);
            assertEquals(4096, restored.rules.length());
            assertEquals(5, restored.teamA.size());

            SquadStrikeRunState.saveTo(prefs, null);
            assertNull(SquadStrikeRunState.loadFrom(prefs));
        } finally {
            prefs.removeNode();
        }
    }
}
