package com.example.birdgame3;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FightHudStockIconTest {
    @Test
    void iconsFollowRemainingStocksInsteadOfTheDefaultStockRule() {
        BirdGame3 game = new BirdGame3();
        Bird pigeon = new Bird(0.0, BirdGame3.BirdType.PIGEON, 0, game);
        game.players[0] = pigeon;

        game.scores[0] = 3;
        assertEquals(3, game.fightHudStockIconCount(pigeon));
        game.scores[0] = 2;
        assertEquals(2, game.fightHudStockIconCount(pigeon));
        game.scores[0] = 1;
        assertEquals(1, game.fightHudStockIconCount(pigeon));
        game.scores[0] = 0;
        assertEquals(0, game.fightHudStockIconCount(pigeon));
    }

    @Test
    void classicRouteStockOverridesAreShownExactly() {
        BirdGame3 game = new BirdGame3();
        game.harnessPrepareClassicEncounter(
                BirdGame3.BirdType.MOCKINGBIRD, 0, 5.0, 6, 0x570C_0001L, 0x570C_0002L);

        assertEquals(1, game.smashStartingStocks(),
                "Classic's global default remains one stock.");
        assertEquals(2, game.scores[0],
                "Charles's audition route grants the player a second stock.");
        assertEquals(2, game.fightHudStockIconCount(game.players[0]),
                "The HUD must show the route override, not Classic's global default.");
        assertEquals(1, game.fightHudStockIconCount(game.players[1]));
    }

    @Test
    void teammatesKeepIndependentStockIconCounts() {
        BirdGame3 game = new BirdGame3();
        Bird first = new Bird(0.0, BirdGame3.BirdType.PIGEON, 0, game);
        Bird second = new Bird(0.0, BirdGame3.BirdType.EAGLE, 1, game);
        game.players[0] = first;
        game.players[1] = second;
        game.scores[0] = 3;
        game.scores[1] = 1;

        assertEquals(3, game.fightHudStockIconCount(first));
        assertEquals(1, game.fightHudStockIconCount(second));
    }
}
