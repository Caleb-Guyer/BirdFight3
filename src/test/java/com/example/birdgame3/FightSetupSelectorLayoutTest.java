package com.example.birdgame3;

import javafx.geometry.Point2D;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FightSetupSelectorLayoutTest {
    @Test
    void singleSelectorStaysCenteredOnCharacterTile() {
        assertEquals(new Point2D(0.0, 0.0), BirdGame3.fightSelectorSharedOffset(0, 1));
    }

    @Test
    void sharedCharacterTileOffsetsKeepPlayerMarkersSeparated() {
        for (int count = 2; count <= 4; count++) {
            for (int a = 0; a < count; a++) {
                Point2D first = BirdGame3.fightSelectorSharedOffset(a, count);
                for (int b = a + 1; b < count; b++) {
                    Point2D second = BirdGame3.fightSelectorSharedOffset(b, count);
                    assertTrue(first.distance(second) > 35.0,
                            "shared selector offsets should not overlap for " + count + " players");
                }
            }
        }
    }
}
