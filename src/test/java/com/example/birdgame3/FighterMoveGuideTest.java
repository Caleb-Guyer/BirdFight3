package com.example.birdgame3;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FighterMoveGuideTest {
    @Test
    void everyPlayableBirdHasACompleteFourDirectionReference() {
        assertTrue(FighterMoveGuide.hasCompleteRoster());

        for (BirdGame3.BirdType type : BirdGame3.BirdType.values()) {
            FighterMoveGuide.Guide guide = FighterMoveGuide.forBird(type);
            assertEquals(List.of("NEUTRAL", "SIDE", "UP", "DOWN"),
                    guide.moves().stream().map(FighterMoveGuide.Move::direction).toList(), type.name());
            assertTrue(guide.role().length() >= 8, type.name());
            assertTrue(guide.mechanic().length() >= 24, type.name());
            assertTrue(guide.ultimateName().length() >= 4, type.name());
            assertTrue(guide.ultimateDescription().length() >= 20, type.name());
            assertTrue(guide.moves().stream().allMatch(move -> move.description().length() >= 20), type.name());
            assertTrue(guide.moves().get(2).recovery(), type.name());
            assertFalse(guide.moves().get(0).recovery(), type.name());
            assertFalse(guide.moves().get(1).recovery(), type.name());
            assertFalse(guide.moves().get(3).recovery(), type.name());
        }
    }

    @Test
    void guidesExposeTheFighterSpecificRulesPlayersNeedDuringAMatch() {
        FighterMoveGuide.Guide pigeon = FighterMoveGuide.forBird(BirdGame3.BirdType.PIGEON);
        assertTrue(pigeon.mechanic().contains("Hold Neutral"));
        assertTrue(pigeon.moves().get(3).description().contains("cracks both ways"));

        FighterMoveGuide.Guide charles = FighterMoveGuide.forBird(BirdGame3.BirdType.MOCKINGBIRD);
        assertTrue(charles.moves().get(0).description().contains("without a capture"));
        assertTrue(charles.moves().get(1).description().contains("microphone"));

        FighterMoveGuide.Guide razorbill = FighterMoveGuide.forBird(BirdGame3.BirdType.RAZORBILL);
        assertTrue(razorbill.mechanic().contains("cannot be reused until landing"));
    }
}
