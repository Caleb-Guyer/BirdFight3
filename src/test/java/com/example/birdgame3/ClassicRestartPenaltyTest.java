package com.example.birdgame3;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ClassicRestartPenaltyTest {
    @Test
    void restartingAnEncounterCountsAsALossAndLowersDifficulty() throws Exception {
        BirdGame3 game = new BirdGame3();
        set(game, "classicDifficulty", 5.0);
        set(game, "classicDeaths", 0);

        game.applyClassicRestartPenalty();

        assertEquals(4.5, (double) get(game, "classicDifficulty"), 0.0001);
        assertEquals(1, get(game, "classicDeaths"));
    }

    @Test
    void repeatedRestartsCannotPushDifficultyBelowTheClassicFloor() throws Exception {
        BirdGame3 game = new BirdGame3();
        set(game, "classicDifficulty", 1.0);

        game.applyClassicRestartPenalty();

        assertEquals(1.0, (double) get(game, "classicDifficulty"), 0.0001);
        assertEquals(1, get(game, "classicDeaths"));
    }

    private static Object get(BirdGame3 game, String name) throws Exception {
        Field field = BirdGame3.class.getDeclaredField(name);
        field.setAccessible(true);
        return field.get(game);
    }

    private static void set(BirdGame3 game, String name, Object value) throws Exception {
        Field field = BirdGame3.class.getDeclaredField(name);
        field.setAccessible(true);
        field.set(game, value);
    }
}
