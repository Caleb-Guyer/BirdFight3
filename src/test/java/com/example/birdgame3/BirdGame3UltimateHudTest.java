package com.example.birdgame3;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BirdGame3UltimateHudTest {
    @Test
    void unreadyUltimateNeverDisplaysOneHundredPercent() {
        assertEquals("99%", BirdGame3.ultimateChargeStatusText(0.999999, false));
        assertEquals("99%", BirdGame3.ultimateChargeStatusText(1.0, false));
    }

    @Test
    void fullUltimateDisplaysReadyWithTheRing() {
        assertEquals("READY", BirdGame3.ultimateChargeStatusText(1.0, true));
    }

    @Test
    void partialUltimateUsesWholePercentWithoutRoundingUp() {
        assertEquals("50%", BirdGame3.ultimateChargeStatusText(0.509, false));
        assertEquals("0%", BirdGame3.ultimateChargeStatusText(-0.2, false));
    }
}
