package com.example.birdgame3;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;

import static com.example.birdgame3.BirdGame3.BirdType;
import static com.example.birdgame3.BirdGame3.ClassicEncounter;
import static com.example.birdgame3.BirdGame3.ClassicEncounterStyle;
import static com.example.birdgame3.BirdGame3.MapVariant;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FalconClassicRouteTest {
    @Test
    void falconHasTheFixedEightEncounterNothingEscapesRoute() throws Exception {
        List<ClassicEncounter> route = falconRoute(new BirdGame3());

        assertEquals(8, route.size());
        assertEquals(List.of(
                        "Scatter the Flock",
                        "Faster Than Royalty",
                        "Rush Hour Crossfire",
                        "The Immovable Target",
                        "Hunt After Dark",
                        "Outrun Rebirth",
                        "Bonus: Peregrine Run",
                        "The Ultimate Prey"),
                route.stream().map(encounter -> encounter.name).toList());
        assertEquals(List.of(
                        MapVariant.STANDARD,
                        MapVariant.STANDARD,
                        MapVariant.STANDARD,
                        MapVariant.STANDARD,
                        MapVariant.PARLIAMENT_ROOFTOPS,
                        MapVariant.ASHFALL_REBIRTH,
                        MapVariant.PEREGRINE_RUN,
                        MapVariant.NULL_ROC_ASCENDING),
                route.stream().map(encounter -> encounter.variant).toList());
        assertEquals(ClassicEncounterStyle.MINIATURE_FLOCK, route.get(0).style);
        assertEquals(ClassicEncounterStyle.GIANT, route.get(3).style);
        assertEquals(ClassicEncounterStyle.PHOENIX_REBIRTH, route.get(5).style);
        assertEquals(ClassicEncounterStyle.BONUS_RELAY, route.get(6).style);
        assertEquals(ClassicEncounterStyle.NULL_ROC_BOSS, route.get(7).style);
        assertTrue(route.getLast().bossFight);
    }

    @Test
    void authoredTeamsMatchTheApprovedRoute() throws Exception {
        List<ClassicEncounter> route = falconRoute(new BirdGame3());

        assertEquals(3, route.get(0).enemies.length);
        assertTrue(List.of(route.get(0).enemies).stream()
                .allMatch(fighter -> fighter.type() == BirdType.PIGEON));
        assertEquals(BirdType.EAGLE, route.get(1).enemies[0].type());
        assertEquals(BirdType.ROADRUNNER, route.get(2).allies[0].type());
        assertEquals(List.of(BirdType.HUMMINGBIRD, BirdType.BAT),
                List.of(route.get(2).enemies[0].type(), route.get(2).enemies[1].type()));
        assertEquals(BirdType.SHOEBILL, route.get(3).enemies[0].type());
        assertEquals(BirdType.EAGLE, route.get(4).allies[0].type());
        assertEquals(List.of(BirdType.RAVEN, BirdType.VULTURE),
                List.of(route.get(4).enemies[0].type(), route.get(4).enemies[1].type()));
        assertEquals(BirdType.PHOENIX, route.get(5).enemies[0].type());
        assertEquals(BirdType.VULTURE, route.get(7).enemies[0].type());
        assertTrue(route.get(7).enemies[0].title().contains("Null Roc"));
    }

    @Test
    void flockHasNoUltimatesAndShoebillUsesTheGiantRules() throws Exception {
        BirdGame3 game = preparedFalconGame();
        List<ClassicEncounter> route = falconRoute(game);

        ClassicEncounter flock = route.get(0);
        game.classicEncounter = flock;
        invoke(game, "setupClassicEncounterRoster", new Class<?>[]{ClassicEncounter.class}, flock);
        for (int i = 1; i < game.activePlayers; i++) {
            assertFalse(game.players[i].hasUltimate());
            assertTrue(game.players[i].sizeMultiplier < 0.8);
        }

        ClassicEncounter giant = route.get(3);
        game.classicEncounter = giant;
        invoke(game, "setupClassicEncounterRoster", new Class<?>[]{ClassicEncounter.class}, giant);
        assertTrue(game.players[1].sizeMultiplier >= 1.4);
    }

    @Test
    void phoenixSecondStockBecomesTheAshenPhaseOnlyOnce() throws Exception {
        BirdGame3 game = preparedFalconGame();
        ClassicEncounter rebirth = falconRoute(game).get(5);
        game.classicEncounter = rebirth;
        invoke(game, "setupClassicEncounterRoster", new Class<?>[]{ClassicEncounter.class}, rebirth);

        game.scores[0] = 1;
        game.scores[1] = 1;
        Bird phoenix = game.players[1];
        phoenix.attackCooldown = 30;
        phoenix.phoenixNeutralReuseTimer = 90;
        phoenix.phoenixFireballReuseTimer = 90;
        phoenix.phoenixLavaReuseTimer = 90;

        invoke(game, "applyFalconClassicRuntimeEffects", new Class<?>[0]);

        assertTrue(phoenix.isAshenSovereignSkin);
        assertTrue(phoenix.powerMultiplier >= phoenix.basePowerMultiplier * 1.06);
        assertTrue(phoenix.speedMultiplier >= phoenix.baseSpeedMultiplier * 1.08);
        assertTrue(phoenix.attackCooldown <= 10);
        assertTrue(phoenix.phoenixNeutralReuseTimer <= 42);
        assertTrue(phoenix.phoenixFireballReuseTimer <= 52);
        assertTrue(phoenix.phoenixLavaReuseTimer <= 34);
        assertTrue((double) invoke(game, "ashfallHazardStrengthMultiplier", new Class<?>[0]) > 1.0);

        double phasePower = phoenix.powerMultiplier;
        invoke(game, "applyFalconClassicRuntimeEffects", new Class<?>[0]);
        assertEquals(phasePower, phoenix.powerMultiplier, 0.0001);
    }

    @Test
    void authoredMultiStockEncountersStillUseSmashRules() throws Exception {
        BirdGame3 game = preparedFalconGame();
        List<ClassicEncounter> route = falconRoute(game);

        ClassicEncounter phoenix = route.get(5);
        game.classicEncounter = phoenix;
        invoke(game, "setupClassicEncounterRoster", new Class<?>[]{ClassicEncounter.class}, phoenix);
        assertTrue(game.classicUsesSmashRules());
        initializeStocks(game);
        invoke(game, "applyClassicEncounterStockOverrides", new Class<?>[0]);
        assertEquals(2, game.scores[0]);
        assertEquals(2, game.scores[1]);

        ClassicEncounter nullRoc = route.get(7);
        game.classicEncounter = nullRoc;
        invoke(game, "setupClassicEncounterRoster", new Class<?>[]{ClassicEncounter.class}, nullRoc);
        assertTrue(game.classicUsesSmashRules());
        assertFalse(game.players[1].hasUltimate());
        assertTrue(game.players[1].isNullRockForm());
        initializeStocks(game);
        invoke(game, "applyClassicEncounterStockOverrides", new Class<?>[0]);
        assertEquals(3, game.scores[0]);
        assertEquals(2, game.scores[1]);
    }

    @Test
    void nullRocPhasesAddCrosswindsAndFasterAttacksWithoutDuplication() throws Exception {
        BirdGame3 game = preparedFalconGame();
        ClassicEncounter boss = falconRoute(game).getLast();
        game.classicEncounter = boss;
        invoke(game, "setupClassicEncounterRoster", new Class<?>[]{ClassicEncounter.class}, boss);
        game.selectedMap = boss.map;
        game.selectedMapVariant = boss.variant;
        invoke(game, "setupMatchArenaGeometry", new Class<?>[0]);
        invoke(game, "applySelectedMapVariantArena", new Class<?>[0]);
        int baseVents = game.windVents.size();
        Bird nullRoc = game.players[1];

        game.scores[1] = 2;
        invoke(game, "applyFalconClassicRuntimeEffects", new Class<?>[0]);
        assertEquals(baseVents + 2, game.windVents.size());
        assertTrue(nullRoc.speedMultiplier >= nullRoc.baseSpeedMultiplier * 1.04);

        nullRoc.attackCooldown = 30;
        nullRoc.specialCooldown = 420;
        game.scores[1] = 1;
        invoke(game, "applyFalconClassicRuntimeEffects", new Class<?>[0]);
        assertTrue(nullRoc.attackCooldown <= 10);
        assertTrue(nullRoc.specialCooldown <= 210);
        assertTrue(nullRoc.powerMultiplier >= nullRoc.basePowerMultiplier * 1.08);

        invoke(game, "applyFalconClassicRuntimeEffects", new Class<?>[0]);
        assertEquals(baseVents + 2, game.windVents.size());
    }

    @Test
    void nullRocPhaseBreakIsEarnedAndClassicCannotHoverOutsideTheBlastZone() throws Exception {
        BirdGame3 game = preparedFalconGame();
        ClassicEncounter bossEncounter = falconRoute(game).getLast();
        game.classicEncounter = bossEncounter;
        invoke(game, "setupClassicEncounterRoster", new Class<?>[]{ClassicEncounter.class}, bossEncounter);
        Bird falcon = game.players[0];
        Bird nullRoc = game.players[1];
        game.scores[nullRoc.playerIndex] = 3;
        falcon.x = 1_000.0;
        nullRoc.x = 3_000.0;

        assertFalse(game.permitsNullRockVoidRecovery(nullRoc));
        nullRoc.setTrailerSmashDamagePercent(299.0);
        nullRoc.vx = 0.0;
        invoke(game, "applyFalconNullRocRuntimeEffects", new Class<?>[0]);
        assertEquals(0.0, nullRoc.vx, 0.0001);

        nullRoc.setTrailerSmashDamagePercent(300.0);
        invoke(game, "applyFalconNullRocRuntimeEffects", new Class<?>[0]);
        assertTrue(nullRoc.vx >= 52.0);
        assertTrue(nullRoc.stunTime >= 36.0);

        game.classicModeActive = false;
        assertTrue(game.permitsNullRockVoidRecovery(nullRoc),
                "Story and normal Null Rock encounters retain their authored void recovery.");
    }

    @Test
    void routeOpponentCalibrationNeverMutatesThePlayableFalcon() throws Exception {
        BirdGame3 game = preparedFalconGame();
        ClassicEncounter bossEncounter = falconRoute(game).getLast();
        game.classicEncounter = bossEncounter;
        invoke(game, "setupClassicEncounterRoster", new Class<?>[]{ClassicEncounter.class}, bossEncounter);
        Bird falcon = game.players[0];
        double health = falcon.health;
        double size = falcon.sizeMultiplier;
        double power = falcon.powerMultiplier;
        double speed = falcon.speedMultiplier;

        invoke(game, "applyClassicOpponentBalancePass",
                new Class<?>[]{ClassicEncounter.class, Bird.class}, bossEncounter, falcon);

        assertEquals(health, falcon.health, 0.0001);
        assertEquals(size, falcon.sizeMultiplier, 0.0001);
        assertEquals(power, falcon.powerMultiplier, 0.0001);
        assertEquals(speed, falcon.speedMultiplier, 0.0001);
    }

    @Test
    void peregrineBonusUsesTheThreeTargetDiveCourse() throws Exception {
        BirdGame3 game = preparedFalconGame();
        ClassicEncounter bonus = falconRoute(game).get(6);
        game.classicEncounter = bonus;
        invoke(game, "setupClassicEncounterRoster", new Class<?>[]{ClassicEncounter.class}, bonus);
        invoke(game, "positionClassicEncounterSpawns", new Class<?>[]{ClassicEncounter.class}, bonus);

        assertEquals(3, game.activePlayers - 1);
        assertTrue(game.players[1].x < game.players[2].x);
        assertTrue(game.players[2].x < game.players[3].x);
        assertTrue(game.players[2].y < game.players[1].y);
        for (int i = 1; i < 4; i++) {
            assertTrue(game.players[i].classicBonusTarget);
            assertFalse(game.players[i].hasUltimate());
        }
    }

    @Test
    void falconRouteTitleIsAuthoredInsteadOfPlaceholderText() throws Exception {
        Method title = BirdGame3.class.getDeclaredMethod("classicRouteTitle", BirdType.class);
        title.setAccessible(true);
        assertEquals("NOTHING ESCAPES", title.invoke(new BirdGame3(), BirdType.FALCON));
    }

    private static void initializeStocks(BirdGame3 game) {
        for (int i = 0; i < game.activePlayers; i++) {
            game.scores[i] = game.smashStartingStocks();
        }
    }

    private static BirdGame3 preparedFalconGame() throws Exception {
        BirdGame3 game = new BirdGame3();
        game.classicModeActive = true;
        setField(game, "classicSelectedBird", BirdType.FALCON);
        setField(game, "classicDifficulty", 5.0);
        return game;
    }

    @SuppressWarnings("unchecked")
    private static List<ClassicEncounter> falconRoute(BirdGame3 game) throws Exception {
        Method build = BirdGame3.class.getDeclaredMethod("buildClassicRun", BirdType.class);
        build.setAccessible(true);
        return (List<ClassicEncounter>) build.invoke(game, BirdType.FALCON);
    }

    private static Object invoke(BirdGame3 game, String name, Class<?>[] parameterTypes, Object... arguments)
            throws Exception {
        Method method = BirdGame3.class.getDeclaredMethod(name, parameterTypes);
        method.setAccessible(true);
        return method.invoke(game, arguments);
    }

    private static void setField(BirdGame3 game, String name, Object value) throws Exception {
        Field field = BirdGame3.class.getDeclaredField(name);
        field.setAccessible(true);
        field.set(game, value);
    }
}
