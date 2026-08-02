package com.example.birdgame3;

import org.junit.jupiter.api.Test;

import static com.example.birdgame3.BirdGame3.BirdType;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class RosterSelectionTest {
    @Test
    void echoFightersResolveToTheirBaseBirdPortraits() {
        BirdGame3 game = new BirdGame3();

        assertEquals(BirdType.EAGLE, game.echoBaseBird(BirdType.FALCON));
        assertEquals(BirdType.OPIUMBIRD, game.echoBaseBird(BirdType.HEISENBIRD));
        assertNull(game.echoBaseBird(BirdType.PIGEON));
        assertNull(game.echoBaseBird(null));
    }
}
