package com.example.birdgame3;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class VersusRulesTest {
    @Test
    void customRulesRoundTripEverySimulationOption() {
        BirdGame3.StageChoice excluded = new BirdGame3.StageChoice(
                BirdGame3.MapType.CITY, BirdGame3.MapVariant.PARLIAMENT_ROOFTOPS);
        VersusRules original = new VersusRules("Caleb Rules", 5, 330,
                false, 14, false, false, true,
                170, 80, 4, 9, BirdGame3.StageRandomPool.VARIANTS,
                true, Set.of(VersusRules.stageKey(excluded)));

        VersusRules decoded = VersusRules.decode(original.encode(), VersusRules.standard());

        assertEquals(original, decoded);
        assertTrue(decoded.excludes(excluded));
        assertEquals("5:30", decoded.timeText());
        assertEquals("VARIANTS", decoded.randomPoolText());
    }

    @Test
    void corruptOrExtremeValuesCannotEscapeSafeBounds() {
        VersusRules sanitized = new VersusRules("  A name that is intentionally far too long  ",
                -10, 99_999, true, -1, true, true, false,
                53, 999, 99, -5, BirdGame3.StageRandomPool.NONE, false,
                Set.of("bad key", "CITY:STANDARD"));

        assertEquals(1, sanitized.stockCount());
        assertEquals(600, sanitized.timeLimitSeconds());
        assertEquals(4, sanitized.powerUpIntervalSeconds());
        assertEquals(50, sanitized.launchRatePercent());
        assertEquals(200, sanitized.damageRatePercent());
        assertEquals(5, sanitized.seriesWins());
        assertEquals(1, sanitized.defaultCpuLevel());
        assertEquals(BirdGame3.StageRandomPool.ALL, sanitized.randomStagePool());
        assertEquals(Set.of("CITY:STANDARD"), sanitized.excludedStageKeys());
        assertTrue(sanitized.name().length() <= 24);
        assertEquals(VersusRules.standard(), VersusRules.decode("not rules", VersusRules.standard()));
    }

    @Test
    void libraryRestoresThreeIndependentNamedSlots() {
        VersusRulesLibrary original = new VersusRulesLibrary();
        original.setSlot(0, VersusRules.standard().withName("One").withStockCount(1));
        original.setSlot(2, VersusRules.chaos().withName("Three").withDamageRatePercent(150));
        original.selectSlot(2);

        VersusRulesLibrary restored = new VersusRulesLibrary();
        restored.restore(original.selectedSlot(), original.encodedSlots());

        assertEquals(2, restored.selectedSlot());
        assertEquals("One", restored.slot(0).name());
        assertEquals(1, restored.slot(0).stockCount());
        assertEquals("Three", restored.slot(2).name());
        assertEquals(150, restored.slot(2).damageRatePercent());
    }
}
