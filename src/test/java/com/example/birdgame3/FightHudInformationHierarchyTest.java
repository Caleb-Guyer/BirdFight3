package com.example.birdgame3;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FightHudInformationHierarchyTest {
    @Test
    void minimapOnlyAppearsWhenItProvidesNavigationValue() {
        assertFalse(BirdGame3.shouldShowFightHudMinimap(false, false, false, null));
        assertFalse(BirdGame3.shouldShowFightHudMinimap(
                false, false, true, BirdGame3.ClassicEncounterStyle.STANDARD));
        assertTrue(BirdGame3.shouldShowFightHudMinimap(
                false, false, true, BirdGame3.ClassicEncounterStyle.REDLINE_RUN));
        assertTrue(BirdGame3.shouldShowFightHudMinimap(false, true, false, null));
        assertFalse(BirdGame3.shouldShowFightHudMinimap(
                true, true, true, BirdGame3.ClassicEncounterStyle.REDLINE_RUN));
    }

    @Test
    void statusCardDropsNoOpRulesAndNeverExceedsThreeLines() {
        List<String> compact = BirdGame3.compactFightHudInfoLines(List.of(
                "CLASSIC 1/8  THE FIRST CALL",
                "RULES  None | None",
                "DIFFICULTY 5.0  SCORE 0",
                "BROOD MORALE 2/3",
                "CAGES 1/3"
        ));

        assertEquals(List.of(
                "CLASSIC 1/8  THE FIRST CALL",
                "BROOD MORALE 2/3",
                "CAGES 1/3"
        ), compact);
    }

    @Test
    void meaningfulRulesRemainOnSimpleEncounters() {
        assertEquals(List.of(
                "CLASSIC 3/8  HEAVY WEATHER",
                "RULES  Heavy | Storm Front",
                "DIFFICULTY 6.0  SCORE 12,000"
        ), BirdGame3.compactFightHudInfoLines(List.of(
                "CLASSIC 3/8  HEAVY WEATHER",
                "RULES  Heavy | Storm Front",
                "DIFFICULTY 6.0  SCORE 12,000"
        )));
    }
}
