package com.example.birdgame3;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class VaultFighterProgressTest {
    @Test
    void presentationSnapshotClampsCorruptLegacyCounters() {
        VaultFighterProgress progress = new VaultFighterProgress(
                BirdGame3.BirdType.PIGEON,
                true,
                false,
                true,
                false,
                12,
                3,
                -4,
                99,
                -10,
                -2,
                -30L
        );

        assertEquals(3, progress.skinsOwned());
        assertEquals(0, progress.appearances());
        assertEquals(0, progress.wins());
        assertEquals(0, progress.damage());
        assertEquals(0, progress.knockouts());
        assertEquals(0L, progress.arenaFrames());
        assertEquals("--", progress.winRateText());
    }

    @Test
    void formatsWinRateAndDeterministicSimulationTime() {
        VaultFighterProgress progress = new VaultFighterProgress(
                BirdGame3.BirdType.EAGLE,
                true,
                true,
                true,
                true,
                2,
                4,
                8,
                5,
                4_200,
                18,
                (2 * 3_600L + 17 * 60L + 42) * 60L
        );

        assertEquals("63%", progress.winRateText());
        assertEquals("2h 17m", progress.arenaTimeText());
    }
}
