package com.example.birdgame3;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Random;

import static com.example.birdgame3.BirdGame3.BirdType;
import static com.example.birdgame3.BirdGame3.ClassicEncounter;
import static com.example.birdgame3.BirdGame3.ClassicEncounterStyle;
import static com.example.birdgame3.BirdGame3.MapType;
import static com.example.birdgame3.BirdGame3.MapVariant;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PigeonClassicRouteTest {
    @Test
    void pigeonHasTheFixedEightEncounterRooftopAscentRoute() throws Exception {
        List<ClassicEncounter> route = pigeonRoute(new BirdGame3());

        assertEquals(8, route.size());
        assertEquals(List.of(
                        "Rooftop Rookies",
                        "Predators Overhead",
                        "Heavy Traffic",
                        "City After Dark",
                        "No Free Lunch",
                        "The Beacon's Reflection",
                        "Bonus: Rooftop Relay",
                        "No Crown, No King"),
                route.stream().map(encounter -> encounter.name).toList());
        assertEquals(List.of(
                        MapVariant.STANDARD,
                        MapVariant.STANDARD,
                        MapVariant.TITAN_DOCK,
                        MapVariant.PARLIAMENT_ROOFTOPS,
                        MapVariant.STANDARD,
                        MapVariant.CROWN_DUEL,
                        MapVariant.ROOFTOP_RELAY,
                        MapVariant.NULL_ROCK_DUEL),
                route.stream().map(encounter -> encounter.variant).toList());
        assertEquals(ClassicEncounterStyle.MINIATURE_FLOCK, route.get(0).style);
        assertEquals(ClassicEncounterStyle.GIANT, route.get(2).style);
        assertEquals(ClassicEncounterStyle.BONUS_RELAY, route.get(6).style);
        assertEquals(ClassicEncounterStyle.NULL_ROCK_BOSS, route.get(7).style);
    }

    @Test
    void authoredTeamsAndBossFormsMatchTheRoutePlan() throws Exception {
        List<ClassicEncounter> route = pigeonRoute(new BirdGame3());

        assertEquals(3, route.get(0).enemies.length);
        assertTrue(List.of(route.get(0).enemies).stream()
                .allMatch(fighter -> fighter.type() == BirdType.TITMOUSE));
        assertEquals(BirdType.HUMMINGBIRD, route.get(1).allies[0].type());
        assertEquals(List.of(BirdType.FALCON, BirdType.EAGLE),
                List.of(route.get(1).enemies[0].type(), route.get(1).enemies[1].type()));
        assertEquals(BirdType.PELICAN, route.get(2).enemies[0].type());
        assertEquals(BirdType.GOOSE, route.get(4).enemies[0].type());
        assertEquals(BirdType.PIGEON, route.get(5).enemies[0].type());
        assertEquals(BirdType.VULTURE, route.get(7).enemies[0].type());
        assertTrue(route.get(7).enemies[0].title().contains("Null Rock"));
        assertTrue(route.get(7).bossFight);
    }

    @Test
    void pigeonRouteDoesNotDependOnSimulationRandomDraws() throws Exception {
        BirdGame3 first = new BirdGame3();
        BirdGame3 second = new BirdGame3();
        first.random.setSeed(12L);
        second.random.setSeed(98_765L);

        List<ClassicEncounter> firstRoute = pigeonRoute(first);
        List<ClassicEncounter> secondRoute = pigeonRoute(second);

        assertEquals(firstRoute.stream().map(encounter -> encounter.name).toList(),
                secondRoute.stream().map(encounter -> encounter.name).toList());
        assertEquals(firstRoute.stream().map(encounter -> encounter.variant).toList(),
                secondRoute.stream().map(encounter -> encounter.variant).toList());
    }

    @Test
    void bonusRelayCreatesStationaryTargetsAndGiantFightScalesPelican() throws Exception {
        BirdGame3 game = new BirdGame3();
        setField(game, "classicSelectedBird", BirdType.PIGEON);
        game.classicModeActive = true;
        Method setupRoster = BirdGame3.class.getDeclaredMethod("setupClassicEncounterRoster", ClassicEncounter.class);
        setupRoster.setAccessible(true);
        List<ClassicEncounter> route = pigeonRoute(game);

        setupRoster.invoke(game, route.get(6));
        for (int i = 1; i < 4; i++) {
            assertTrue(game.players[i].classicBonusTarget);
            assertFalse(game.isAI[i]);
        }

        setupRoster.invoke(game, route.get(2));
        assertTrue(game.players[1].sizeMultiplier >= 1.75,
                "Ironclad Pelican should read as a giant rather than an ordinary skin");
    }

    @Test
    void nullRockKeepsTrueFormButUsesSoloRouteHealthTuning() throws Exception {
        BirdGame3 game = preparedGameAtIntensity(5.0);
        ClassicEncounter finale = pigeonRoute(game).getLast();
        game.classicEncounter = finale;
        Method setupRoster = BirdGame3.class.getDeclaredMethod("setupClassicEncounterRoster", ClassicEncounter.class);
        setupRoster.setAccessible(true);

        setupRoster.invoke(game, finale);

        Bird boss = game.players[1];
        assertTrue(boss.isNullRockForm());
        assertEquals(520.0, boss.health, 0.001);
        assertEquals(520.0, boss.getMaxHealth(), 0.001);
        assertEquals(3.0, boss.sizeMultiplier, 0.001);
        assertTrue(boss.basePowerMultiplier < game.nullRockTrueFormPowerMultiplier(),
                "solo Classic should not inherit the coalition finale's full power tuning");
    }

    @Test
    void intensityRaisesAuthoredEnemyStatsAndCpuWithoutChangingTheRoute() throws Exception {
        BirdGame3 low = preparedGameAtIntensity(1.0);
        BirdGame3 high = preparedGameAtIntensity(9.0);
        ClassicEncounter lowEncounter = pigeonRoute(low).get(3);
        ClassicEncounter highEncounter = pigeonRoute(high).get(3);
        low.classicEncounter = lowEncounter;
        high.classicEncounter = highEncounter;
        Method setupRoster = BirdGame3.class.getDeclaredMethod("setupClassicEncounterRoster", ClassicEncounter.class);
        setupRoster.setAccessible(true);

        setupRoster.invoke(low, lowEncounter);
        setupRoster.invoke(high, highEncounter);

        assertTrue(high.players[1].health > low.players[1].health);
        assertTrue(high.players[1].basePowerMultiplier > low.players[1].basePowerMultiplier);
        assertTrue(high.getCpuLevel(1) > low.getCpuLevel(1));
        assertEquals(lowEncounter.name, highEncounter.name);
        assertEquals(lowEncounter.variant, highEncounter.variant);
    }

    @Test
    void everyTemporaryRouteStillEndsInABossFight() throws Exception {
        BirdGame3 game = new BirdGame3();
        Method build = BirdGame3.class.getDeclaredMethod("buildClassicRun", BirdType.class);
        build.setAccessible(true);

        @SuppressWarnings("unchecked")
        List<ClassicEncounter> route = (List<ClassicEncounter>) build.invoke(game, BirdType.EAGLE);

        assertFalse(route.isEmpty());
        assertTrue(route.getLast().bossFight);
    }

    @Test
    void dailyChallengeKeepsItsSeededFiveRoundFormatForPigeon() throws Exception {
        BirdGame3 game = new BirdGame3();
        Method build = BirdGame3.class.getDeclaredMethod(
                "buildClassicRun", BirdType.class, Random.class, boolean.class);
        build.setAccessible(true);

        @SuppressWarnings("unchecked")
        List<ClassicEncounter> dailyRoute = (List<ClassicEncounter>) build.invoke(
                game, BirdType.PIGEON, new Random(42L), false);

        assertEquals(5, dailyRoute.size());
        assertFalse(dailyRoute.stream().anyMatch(encounter -> encounter.variant == MapVariant.ROOFTOP_RELAY));
    }

    private static BirdGame3 preparedGameAtIntensity(double intensity) throws Exception {
        BirdGame3 game = new BirdGame3();
        game.classicModeActive = true;
        setField(game, "classicSelectedBird", BirdType.PIGEON);
        setField(game, "classicIntensity", intensity);
        return game;
    }

    @SuppressWarnings("unchecked")
    private static List<ClassicEncounter> pigeonRoute(BirdGame3 game) throws Exception {
        Method build = BirdGame3.class.getDeclaredMethod("buildClassicRun", BirdType.class);
        build.setAccessible(true);
        return (List<ClassicEncounter>) build.invoke(game, BirdType.PIGEON);
    }

    private static void setField(BirdGame3 game, String name, Object value) throws Exception {
        Field field = BirdGame3.class.getDeclaredField(name);
        field.setAccessible(true);
        field.set(game, value);
    }
}
