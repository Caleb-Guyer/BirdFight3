package com.example.birdgame3;

import javafx.scene.input.KeyCode;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MatchControllerTest {
    @Test
    void prepareMatchStartClearsTransientMatchState() {
        BirdGame3 game = new BirdGame3();
        MatchController controller = new MatchController(game);

        game.players[0] = new Bird(100, BirdGame3.BirdType.PIGEON, 0, game);
        game.players[1] = new Bird(250, BirdGame3.BirdType.EAGLE, 1, game);
        game.scores[0] = 120;
        game.scores[1] = 80;
        game.killFeed.add("OLD EVENT");
        game.matchTimer = 1;

        controller.prepareMatchStart(null);

        assertEquals(BirdGame3.MATCH_DURATION_FRAMES, game.matchTimer);
        assertTrue(game.killFeed.isEmpty());
        assertEquals(0, game.scores[0]);
        assertEquals(0, game.scores[1]);
        assertNull(game.players[0]);
        assertNull(game.players[1]);
    }

    @Test
    void prepareMatchStartClearsLatchedGameplayInputs() {
        BirdGame3 game = new BirdGame3();
        MatchController controller = new MatchController(game);
        game.activePlayers = 2;

        KeyCode right = game.rightKeyForPlayer(0);
        game.setLocalActionsForKey(right, true);
        assertTrue(game.isRightPressed(0));

        controller.prepareMatchStart(null);

        assertFalse(game.isRightPressed(0));
    }
}
