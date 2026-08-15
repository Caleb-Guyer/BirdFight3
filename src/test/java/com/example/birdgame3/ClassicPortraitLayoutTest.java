package com.example.birdgame3;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class ClassicPortraitLayoutTest {
    @Test
    void everyBirdAndSkinKeepsItsEstimatedSilhouetteInsideClassicArtwork() {
        BirdGame3 game = new BirdGame3();
        assertEveryPortraitFits(game, 420.0, 310.0);
        assertEveryPortraitFits(game, 240.0, 180.0);
    }

    private static void assertEveryPortraitFits(BirdGame3 game, double width, double height) {
        double padding = Math.min(width, height) * 0.12;
        for (BirdGame3.VisualAuditSkin skin : game.visualAuditSkins()) {
            BirdGame3.ClassicPortraitLayout layout = game.classicPortraitLayout(
                    skin.bird(), skin.key(), width, height);
            String label = skin.name() + " at " + (int) width + "x" + (int) height;
            assertTrue(Double.isFinite(layout.sizeMultiplier()) && layout.sizeMultiplier() > 0.0,
                    label + " has an invalid portrait scale");
            assertTrue(layout.visualLeft() >= padding - 0.001, label + " clips on the left");
            assertTrue(layout.visualTop() >= padding - 0.001, label + " clips on the top");
            assertTrue(layout.visualRight() <= width - padding + 0.001, label + " clips on the right");
            assertTrue(layout.visualBottom() <= height - padding + 0.001, label + " clips on the bottom");
        }
    }
}
