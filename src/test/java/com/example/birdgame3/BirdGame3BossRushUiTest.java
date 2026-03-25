package com.example.birdgame3;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BirdGame3BossRushUiTest {
    @Test
    void classicSelectBadgesStayHiddenDuringBossRush() throws Exception {
        BirdGame3 game = new BirdGame3();
        setClassicCompleted(game, BirdGame3.BirdType.PIGEON, true);

        assertTrue(game.shouldShowClassicSelectBadge(BirdGame3.BirdType.PIGEON, false));
        assertFalse(game.shouldShowClassicSelectBadge(BirdGame3.BirdType.PIGEON, true));
    }

    private static void setClassicCompleted(BirdGame3 game, BirdGame3.BirdType type, boolean completed) throws Exception {
        Field field = BirdGame3.class.getDeclaredField("classicCompleted");
        field.setAccessible(true);
        boolean[] classicCompleted = (boolean[]) field.get(game);
        classicCompleted[type.ordinal()] = completed;
    }
}
