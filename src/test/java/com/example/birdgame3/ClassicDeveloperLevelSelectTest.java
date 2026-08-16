package com.example.birdgame3;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.List;

import static com.example.birdgame3.BirdGame3.BirdType;
import static com.example.birdgame3.BirdGame3.ClassicEncounter;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ClassicDeveloperLevelSelectTest {
    @Test
    void normalProfilesCannotChangeClassicRounds() throws Exception {
        BirdGame3 game = preparedRoute(BirdType.PIGEON);
        ClassicEncounter opening = game.classicEncounter;

        assertFalse(game.canUseClassicDeveloperLevelSelect());
        assertFalse(game.selectClassicDeveloperRound(4));
        assertSame(opening, game.classicEncounter);
        assertEquals(0, intField(game, "classicRoundIndex"));
    }

    @Test
    void featherDevProfilesCanMoveForwardAndBackwardThroughTheRoute() throws Exception {
        BirdGame3 game = preparedRoute(BirdType.PIGEON);
        setField(game, "developerInfiniteBirdCoins", true);
        List<ClassicEncounter> route = routeField(game);

        assertTrue(game.canUseClassicDeveloperLevelSelect());
        assertTrue(game.selectClassicDeveloperRound(4));
        assertEquals(4, intField(game, "classicRoundIndex"));
        assertSame(route.get(4), game.classicEncounter);

        assertTrue(game.selectClassicDeveloperRound(1));
        assertEquals(1, intField(game, "classicRoundIndex"));
        assertSame(route.get(1), game.classicEncounter);

        assertTrue(game.selectClassicDeveloperRound(999));
        assertEquals(route.size() - 1, intField(game, "classicRoundIndex"));
        assertSame(route.getLast(), game.classicEncounter);
    }

    @Test
    void roadRunnerLevelSelectSeedsOnlyTheRoundsBeforeTheSelection() throws Exception {
        BirdGame3 game = preparedRoute(BirdType.ROADRUNNER);
        setField(game, "developerInfiniteBirdCoins", true);

        assertTrue(game.selectClassicDeveloperRound(7));
        boolean[] bolts = (boolean[]) field(game, "classicRoadrunnerBolts");
        for (boolean bolt : bolts) assertTrue(bolt);

        assertTrue(game.selectClassicDeveloperRound(2));
        assertTrue(bolts[0]);
        assertTrue(bolts[1]);
        for (int i = 2; i < bolts.length; i++) assertFalse(bolts[i]);
    }

    @SuppressWarnings("unchecked")
    private static BirdGame3 preparedRoute(BirdType bird) throws Exception {
        BirdGame3 game = new BirdGame3();
        game.classicModeActive = true;
        setField(game, "classicSelectedBird", bird);
        List<ClassicEncounter> route = routeField(game);
        route.addAll(game.harnessClassicRoute(bird, 91L));
        game.classicEncounter = route.getFirst();
        return game;
    }

    @SuppressWarnings("unchecked")
    private static List<ClassicEncounter> routeField(BirdGame3 game) throws Exception {
        return (List<ClassicEncounter>) field(game, "classicRun");
    }

    private static int intField(BirdGame3 game, String name) throws Exception {
        return (int) field(game, name);
    }

    private static Object field(BirdGame3 game, String name) throws Exception {
        Field field = BirdGame3.class.getDeclaredField(name);
        field.setAccessible(true);
        return field.get(game);
    }

    private static void setField(BirdGame3 game, String name, Object value) throws Exception {
        Field field = BirdGame3.class.getDeclaredField(name);
        field.setAccessible(true);
        field.set(game, value);
    }
}
