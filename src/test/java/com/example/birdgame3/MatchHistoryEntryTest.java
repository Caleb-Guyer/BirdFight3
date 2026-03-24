package com.example.birdgame3;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class MatchHistoryEntryTest {
    @Test
    void serializeDeserializeRoundTripPreservesFields() {
        MatchHistoryEntry entry = new MatchHistoryEntry(
                1_717_171_717L,
                "CLASSIC",
                "ROUND 3",
                "CITY",
                "P1 - PIGEON",
                45,
                List.of(
                        new MatchHistoryEntry.Participant("P1", "PIGEON", 200, 3, 1, 350, 90, true),
                        new MatchHistoryEntry.Participant("P2", "EAGLE", 180, 2, 2, 280, 0, false)
                )
        );

        MatchHistoryEntry decoded = MatchHistoryEntry.deserialize(entry.serialize());

        assertNotNull(decoded);
        assertEquals(entry.timestampMillis(), decoded.timestampMillis());
        assertEquals(entry.mode(), decoded.mode());
        assertEquals(entry.detail(), decoded.detail());
        assertEquals(entry.map(), decoded.map());
        assertEquals(entry.winner(), decoded.winner());
        assertEquals(entry.coinsEarned(), decoded.coinsEarned());
        assertEquals(2, decoded.participants().size());
        assertEquals("PIGEON", decoded.participants().get(0).birdName());
        assertEquals(280, decoded.participants().get(1).score());
    }

    @Test
    void deserializeRejectsMalformedRows() {
        assertNull(MatchHistoryEntry.deserialize("not-a-valid-entry"));
    }
}
