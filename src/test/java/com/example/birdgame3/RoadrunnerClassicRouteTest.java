package com.example.birdgame3;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;

import static com.example.birdgame3.BirdGame3.BirdType;
import static com.example.birdgame3.BirdGame3.ClassicEncounter;
import static com.example.birdgame3.BirdGame3.ClassicEncounterStyle;
import static com.example.birdgame3.BirdGame3.MapVariant;
import static org.junit.jupiter.api.Assertions.*;

class RoadrunnerClassicRouteTest {
    @Test
    void roadRunnerHasTheApprovedEightEncounterNoFinishLineRoute() throws Exception {
        List<ClassicEncounter> route = route(new BirdGame3());

        assertEquals(8, route.size());
        assertEquals(List.of(
                        "Off the Line", "Speed Trap", "No Straight Lines", "The Roadblock",
                        "The Other Me", "Break the Pursuit", "Bonus: Redline Run", "The Still King"),
                route.stream().map(encounter -> encounter.name).toList());
        assertEquals(List.of(
                        MapVariant.STANDARD, MapVariant.STANDARD, MapVariant.PEREGRINE_RUN,
                        MapVariant.TITAN_DOCK, MapVariant.STANDARD, MapVariant.ROOFTOP_RELAY,
                        MapVariant.REDLINE_CANYON, MapVariant.REDLINE_CANYON),
                route.stream().map(encounter -> encounter.variant).toList());
        assertEquals(ClassicEncounterStyle.MINIATURE_FLOCK, route.get(0).style);
        assertEquals(ClassicEncounterStyle.GIANT, route.get(3).style);
        assertEquals(ClassicEncounterStyle.REDLINE_MIRROR, route.get(4).style);
        assertEquals(ClassicEncounterStyle.REDLINE_PURSUIT, route.get(5).style);
        assertEquals(ClassicEncounterStyle.REDLINE_RUN, route.get(6).style);
        assertEquals(90 * 60, route.get(6).timerFrames);
        assertEquals(ClassicEncounterStyle.STILL_KING_BOSS, route.get(7).style);
        assertTrue(route.getLast().bossFight);
    }

    @Test
    void authoredOpponentsAndSuccessivePursuitWavesMatchTheProposal() throws Exception {
        List<ClassicEncounter> route = route(new BirdGame3());

        assertEquals(List.of(BirdType.HUMMINGBIRD, BirdType.TITMOUSE, BirdType.FALCON),
                List.of(route.get(0).enemies[0].type(), route.get(0).enemies[1].type(), route.get(0).enemies[2].type()));
        assertEquals(List.of(BirdType.HUMMINGBIRD, BirdType.TITMOUSE, BirdType.FALCON),
                List.of(route.get(0).waves[0][0].type(), route.get(0).waves[1][0].type(),
                        route.get(0).waves[2][0].type()));
        assertEquals(List.of(BirdType.HEISENBIRD, BirdType.OPIUMBIRD),
                List.of(route.get(1).enemies[0].type(), route.get(1).enemies[1].type()));
        assertEquals(BirdType.FALCON, route.get(2).enemies[0].type());
        assertEquals(BirdType.PELICAN, route.get(3).enemies[0].type());
        assertEquals(BirdType.ROADRUNNER, route.get(4).enemies[0].type());
        assertEquals(BirdType.PIGEON, route.get(5).allies[0].type());
        assertEquals(List.of(BirdType.RAVEN, BirdType.VULTURE, BirdType.GOOSE),
                List.of(route.get(5).waves[0][0].type(), route.get(5).waves[1][0].type(), route.get(5).waves[2][0].type()));
        assertEquals(BirdType.SHOEBILL, route.getLast().enemies[0].type());
    }

    @Test
    void sustainedMomentumBanksOneBoltAndStrengthensTheFinalRoute() throws Exception {
        BirdGame3 game = preparedGame();
        prepareEncounter(game, route(game).getFirst());
        Bird player = game.players[0];
        player.roadrunnerMomentum = Bird.ROADRUNNER_MOMENTUM_MAX;
        player.vx = 13.0;

        for (int i = 0; i < 430; i++) game.applyRoadrunnerClassicRuntimeEffects();

        boolean[] bolts = (boolean[]) getField(game, "classicRoadrunnerBolts");
        assertTrue(bolts[0]);
        assertEquals(1, countBolts(bolts));
        assertTrue((boolean) getField(game, "classicRoadrunnerBoltAwardedThisEncounter"));
        for (int i = 0; i < 430; i++) game.applyRoadrunnerClassicRuntimeEffects();
        assertEquals(1, countBolts(bolts), "A split can award only one bolt.");
    }

    @Test
    void pursuitHoldsTheMatchOpenAndSpawnsVultureThenGoose() throws Exception {
        BirdGame3 game = preparedGame();
        prepareEncounter(game, route(game).get(5));

        eliminateEnemyTeam(game);
        assertTrue(game.holdClassicRoadrunnerEncounterOpen());
        assertEquals(BirdType.VULTURE, firstEnemy(game).type);
        assertFalse(firstEnemy(game).hasUltimate());

        eliminateEnemyTeam(game);
        assertTrue(game.holdClassicRoadrunnerEncounterOpen());
        assertEquals(BirdType.GOOSE, firstEnemy(game).type);

        eliminateEnemyTeam(game);
        assertFalse(game.holdClassicRoadrunnerEncounterOpen());
    }

    @Test
    void openingSpeedstersArriveInThreeWaves() throws Exception {
        BirdGame3 game = preparedGame();
        prepareEncounter(game, route(game).getFirst());

        assertEquals(1, countLivingEnemies(game));
        game.players[0].setTrailerSmashDamagePercent(80.0);
        eliminateEnemyTeam(game);
        assertTrue(game.holdClassicRoadrunnerEncounterOpen());
        assertEquals(50.0, game.players[0].smashDamagePercent(), 0.0001);
        assertEquals(1, countLivingEnemies(game));
        assertEquals(BirdType.TITMOUSE, firstEnemy(game).type);

        eliminateEnemyTeam(game);
        assertTrue(game.holdClassicRoadrunnerEncounterOpen());
        assertEquals(1, countLivingEnemies(game));
        assertEquals(BirdType.FALCON, firstEnemy(game).type);
        assertEquals(0.68, firstEnemy(game).sizeMultiplier, 0.0001);
        assertFalse(firstEnemy(game).hasUltimate());

        eliminateEnemyTeam(game);
        assertFalse(game.holdClassicRoadrunnerEncounterOpen());
    }

    @Test
    void openingGauntletGivesRoadrunnerTwoStocks() throws Exception {
        BirdGame3 game = preparedGame();
        prepareEncounter(game, route(game).getFirst());
        invoke(game, "applyClassicEncounterStockOverrides", new Class<?>[0]);

        assertEquals(2, game.scores[0]);
        for (int slot = 1; slot < game.activePlayers; slot++) {
            assertEquals(1, game.scores[slot]);
        }
    }

    @Test
    void canyonFinishAwardsTheSeventhBoltAndARank() throws Exception {
        BirdGame3 game = preparedGame();
        game.headlessHarnessMode = true;
        prepareEncounter(game, route(game).get(6));
        Bird player = game.players[0];
        game.matchTimer = 50 * 60;
        player.health = 0.0;
        player.x = BirdGame3.REDLINE_RUN_FINISH_X - player.bodyWidth();

        game.applyRoadrunnerClassicRuntimeEffects();

        assertTrue((boolean) getField(game, "classicRoadrunnerRunCompleted"));
        assertEquals("S", getField(game, "classicRoadrunnerRunRank"));
        assertTrue(((boolean[]) getField(game, "classicRoadrunnerBolts"))[6]);
    }

    @Test
    void touchingTheVisibleFinishPoleCountsAsFinishing() {
        double bodyWidth = 84.0;

        assertFalse(BirdGame3.hasReachedRedlineRunFinish(
                BirdGame3.REDLINE_RUN_FINISH_X - bodyWidth - 2.0, bodyWidth));
        assertTrue(BirdGame3.hasReachedRedlineRunFinish(
                BirdGame3.REDLINE_RUN_FINISH_X - bodyWidth, bodyWidth));
    }

    @Test
    void redlineRunHasContinuousSupportedGroundFromSpawnThroughFinish() throws Exception {
        BirdGame3 game = preparedGame();
        prepareEncounter(game, route(game).get(6));

        for (double x = BirdGame3.REDLINE_RUN_START_X; x <= BirdGame3.REDLINE_RUN_FINISH_X; x += 50.0) {
            double sampleX = x;
            assertTrue(game.platforms.stream().anyMatch(platform -> sampleX >= platform.x
                            && sampleX <= platform.x + platform.w
                            && Math.abs(platform.y - BirdGame3.REDLINE_RUN_ROAD_Y) < 0.001),
                    "The required race line has no supporting road at x=" + sampleX);
        }
        assertTrue(game.windVents.isEmpty(), "The time trial must not launch the runner off its required line.");
        assertEquals(1_250.0, BirdGame3.nextRedlineCheckpointAfter(330.0));
        assertEquals(BirdGame3.REDLINE_RUN_FINISH_X, BirdGame3.nextRedlineCheckpointAfter(4_900.0));
    }

    @Test
    void fallingDuringRedlineRunReturnsToTheLastClearedSplitWithoutTakingAStock() throws Exception {
        BirdGame3 game = preparedGame();
        prepareEncounter(game, route(game).get(6));
        Bird player = game.players[0];
        setField(game, "classicRoadrunnerCheckpointX", 2_350.0);
        int stocksBefore = game.scores[0];
        player.y = BirdGame3.GROUND_Y + 300.0;

        game.applyRoadrunnerClassicRuntimeEffects();

        assertEquals(stocksBefore, game.scores[0]);
        assertEquals(2_260.0, player.bodyCenterX(), 0.001);
        assertEquals(BirdGame3.REDLINE_RUN_ROAD_Y, player.bodyBottomY(), 0.001);
        assertEquals(0.0, player.vy, 0.001);
    }

    @Test
    void stillKingUsesThreeStocksLargeBodyAndNoAutomaticUltimate() throws Exception {
        BirdGame3 game = preparedGame();
        ClassicEncounter bossEncounter = route(game).getLast();
        prepareEncounter(game, bossEncounter);
        invoke(game, "applyClassicEncounterStockOverrides", new Class<?>[0]);
        Bird boss = game.players[1];

        assertEquals(3, game.scores[1]);
        assertEquals(1.78, boss.sizeMultiplier, 0.0001);
        assertFalse(boss.hasUltimate());
        assertEquals("NO FINISH LINE",
                invoke(game, "classicRouteTitle", new Class<?>[]{BirdType.class}, BirdType.ROADRUNNER));
    }

    @Test
    void redlineCanyonIsStableUnlockableAndBackfilledFromTheBadge() throws Exception {
        BirdGame3 game = preparedGame();
        game.selectedMap = BirdGame3.MapType.DESERT;
        game.selectedMapVariant = MapVariant.REDLINE_CANYON;
        invoke(game, "setupMatchArenaGeometry", new Class<?>[0]);
        invoke(game, "applySelectedMapVariantArena", new Class<?>[0]);

        assertTrue(game.platforms.stream().anyMatch(platform -> platform.w == 5_640.0));
        assertFalse(game.availableStageChoices(BirdGame3.StageRandomPool.VARIANTS).stream()
                .anyMatch(choice -> choice.variant() == MapVariant.REDLINE_CANYON));
        setField(game, "redlineCanyonUnlocked", true);
        assertTrue(game.availableStageChoices(BirdGame3.StageRandomPool.VARIANTS).stream()
                .anyMatch(choice -> choice.variant() == MapVariant.REDLINE_CANYON));
    }

    private static Bird firstEnemy(BirdGame3 game) {
        for (Bird bird : game.players) {
            if (bird != null && game.getEffectiveTeam(bird.playerIndex) == 2 && game.scores[bird.playerIndex] > 0) {
                return bird;
            }
        }
        return null;
    }

    private static int countLivingEnemies(BirdGame3 game) {
        int count = 0;
        for (Bird bird : game.players) {
            if (bird != null && game.getEffectiveTeam(bird.playerIndex) == 2
                    && game.scores[bird.playerIndex] > 0) {
                count++;
            }
        }
        return count;
    }

    private static void eliminateEnemyTeam(BirdGame3 game) {
        for (Bird bird : game.players) {
            if (bird != null && game.getEffectiveTeam(bird.playerIndex) == 2) {
                game.scores[bird.playerIndex] = 0;
                bird.health = 0.0;
            }
        }
    }

    private static int countBolts(boolean[] bolts) {
        int count = 0;
        for (boolean bolt : bolts) if (bolt) count++;
        return count;
    }

    private static void prepareEncounter(BirdGame3 game, ClassicEncounter encounter) throws Exception {
        game.classicEncounter = encounter;
        game.selectedMap = encounter.map;
        game.selectedMapVariant = encounter.variant;
        setField(game, "smashCombatRulesActive", true);
        invoke(game, "setupClassicEncounterRoster", new Class<?>[]{ClassicEncounter.class}, encounter);
        invoke(game, "setupMatchArenaGeometry", new Class<?>[0]);
        invoke(game, "applySelectedMapVariantArena", new Class<?>[0]);
        for (int i = 0; i < game.activePlayers; i++) game.scores[i] = game.smashStartingStocks();
        invoke(game, "applyClassicEncounterArenaModifiers", new Class<?>[]{ClassicEncounter.class}, encounter);
        invoke(game, "positionClassicEncounterSpawns", new Class<?>[]{ClassicEncounter.class}, encounter);
    }

    private static BirdGame3 preparedGame() throws Exception {
        BirdGame3 game = new BirdGame3();
        game.classicModeActive = true;
        setField(game, "classicSelectedBird", BirdType.ROADRUNNER);
        setField(game, "classicDifficulty", 5.0);
        return game;
    }

    @SuppressWarnings("unchecked")
    private static List<ClassicEncounter> route(BirdGame3 game) throws Exception {
        return (List<ClassicEncounter>) invoke(game, "buildClassicRun",
                new Class<?>[]{BirdType.class}, BirdType.ROADRUNNER);
    }

    private static Object invoke(BirdGame3 game, String name, Class<?>[] parameterTypes, Object... args)
            throws Exception {
        Method method = BirdGame3.class.getDeclaredMethod(name, parameterTypes);
        method.setAccessible(true);
        return method.invoke(game, args);
    }

    private static Object getField(BirdGame3 game, String name) throws Exception {
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
