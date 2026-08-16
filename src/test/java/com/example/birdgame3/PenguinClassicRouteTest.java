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

class PenguinClassicRouteTest {
    @Test
    void penguinHasTheApprovedEightEncounterIceHoldsRoute() throws Exception {
        List<ClassicEncounter> route = route(new BirdGame3());

        assertEquals(8, route.size());
        assertEquals(List.of(
                        "Cold Water", "Heat Rises", "The Icebreaker", "Fire at the Fjord",
                        "The Cold King", "Hold the Gate", "Bonus: Ice Architect", "The Last Sun"),
                route.stream().map(encounter -> encounter.name).toList());
        assertEquals(List.of(
                        MapVariant.STANDARD, MapVariant.STANDARD, MapVariant.TITAN_DOCK,
                        MapVariant.FROZEN_CALDERA, MapVariant.STANDARD, MapVariant.STANDARD,
                        MapVariant.LAST_ICE_SHELF, MapVariant.LAST_ICE_SHELF),
                route.stream().map(encounter -> encounter.variant).toList());
        assertEquals(ClassicEncounterStyle.GIANT, route.get(2).style);
        assertEquals(ClassicEncounterStyle.ICEWORKS_MIRROR, route.get(4).style);
        assertEquals(ClassicEncounterStyle.ICEWORKS_SIEGE, route.get(5).style);
        assertEquals(ClassicEncounterStyle.ICE_ARCHITECT, route.get(6).style);
        assertEquals(ClassicEncounterStyle.LAST_SUN_BOSS, route.get(7).style);
        assertEquals(100 * 60, route.get(6).timerFrames);
        assertEquals(180 * 60, route.get(7).timerFrames);
        assertTrue(route.getLast().bossFight);
        assertEquals("THE ICE HOLDS",
                invoke(new BirdGame3(), "classicRouteTitle", new Class<?>[]{BirdType.class}, BirdType.PENGUIN));
    }

    @Test
    void routeUsesTheProposedOpponentsAlliesAndSequentialSiege() throws Exception {
        List<ClassicEncounter> route = route(new BirdGame3());

        assertEquals(List.of(BirdType.GOOSE, BirdType.RAZORBILL),
                List.of(route.get(0).enemies[0].type(), route.get(0).enemies[1].type()));
        assertEquals(List.of(BirdType.FALCON, BirdType.HUMMINGBIRD),
                List.of(route.get(1).enemies[0].type(), route.get(1).enemies[1].type()));
        assertEquals(BirdType.PELICAN, route.get(2).enemies[0].type());
        assertEquals(List.of(BirdType.PHOENIX, BirdType.ROOSTER),
                List.of(route.get(3).enemies[0].type(), route.get(3).enemies[1].type()));
        assertEquals(BirdType.PENGUIN, route.get(4).enemies[0].type());
        assertEquals(BirdType.SHOEBILL, route.get(5).allies[0].type());
        assertEquals(List.of(BirdType.RAVEN, BirdType.VULTURE, BirdType.HEISENBIRD),
                List.of(route.get(5).waves[0][0].type(), route.get(5).waves[1][0].type(),
                        route.get(5).waves[2][0].type()));
        assertEquals(BirdType.PHOENIX, route.getLast().enemies[0].type());
    }

    @Test
    void realSnowFortBuildsRepairsAndEnemyAttacksShatterIceworks() throws Exception {
        BirdGame3 game = preparedGame();
        prepareEncounter(game, route(game).getFirst());
        Bird player = game.players[0];
        Bird enemy = firstEnemy(game);
        List<BirdGame3.ClassicIceworkAnchor> anchors = iceworks(game);
        BirdGame3.ClassicIceworkAnchor anchor = anchors.getFirst();

        player.penguinSnowFort = new Bird.PenguinSnowFort(anchor.x, anchor.y, 1, false);
        game.applyPenguinClassicRuntimeEffects();

        assertTrue(anchor.built());
        assertNull(player.penguinSnowFort, "The Snow Fort should be consumed into the route structure.");
        assertTrue(game.platforms.contains(anchor.platform));
        int builtHealth = anchor.health;

        anchor.health -= 20;
        player.penguinSnowFort = new Bird.PenguinSnowFort(anchor.x, anchor.y, 1, false);
        game.applyPenguinClassicRuntimeEffects();
        assertTrue(anchor.health > builtHealth - 20, "A second fort from the owning team repairs the Icework.");

        anchor.damageCooldown = 0;
        game.damageClassicPenguinIcework(enemy,
                anchor.platform.x + anchor.platform.w * 0.5,
                anchor.platform.y + anchor.platform.h * 0.5,
                20.0, 20.0, 500);
        assertFalse(anchor.built());
        assertFalse(game.platforms.contains(anchor.platform));
    }

    @Test
    void holdTheGateSpawnsVultureThenHeisenbirdWithoutUlts() throws Exception {
        BirdGame3 game = preparedGame();
        prepareEncounter(game, route(game).get(5));

        assertEquals(BirdType.RAVEN, firstEnemy(game).type);
        assertFalse(firstEnemy(game).hasUltimate());
        eliminateEnemyTeam(game);
        assertTrue(game.holdClassicPenguinEncounterOpen());
        assertEquals(BirdType.VULTURE, firstEnemy(game).type);
        assertFalse(firstEnemy(game).hasUltimate());
        eliminateEnemyTeam(game);
        assertTrue(game.holdClassicPenguinEncounterOpen());
        assertEquals(BirdType.HEISENBIRD, firstEnemy(game).type);
        assertFalse(firstEnemy(game).hasUltimate());
        eliminateEnemyTeam(game);
        assertFalse(game.holdClassicPenguinEncounterOpen());
    }

    @Test
    void iceArchitectUsesThreeRealIceworksThenOpensAWalkThroughExit() throws Exception {
        BirdGame3 game = preparedGame();
        game.headlessHarnessMode = true;
        prepareEncounter(game, route(game).get(6));
        Bird player = game.players[0];

        for (BirdGame3.ClassicIceworkAnchor anchor : iceworks(game)) {
            player.penguinSnowFort = new Bird.PenguinSnowFort(anchor.x, anchor.y, 1, false);
            game.applyPenguinClassicRuntimeEffects();
        }
        for (int tick = 0; tick < 100; tick++) {
            game.simTick++;
            game.applyPenguinClassicRuntimeEffects();
        }

        assertArrayEquals(new boolean[]{true, true, true},
                (boolean[]) getField(game, "classicPenguinArchitectTargets"));
        assertTrue((boolean) getField(game, "classicPenguinArchitectExitOpen"));
        assertTrue(game.holdClassicPenguinEncounterOpen());

        player.penguinBellySlideTimer = 0;
        player.x = BirdGame3.LAST_ICE_EXIT_TRIGGER_X - player.bodyWidth() * 0.25;
        game.applyPenguinClassicRuntimeEffects();

        assertTrue((boolean) getField(game, "classicPenguinArchitectCompleted"));
        assertSame(player, game.harnessWinner);
    }

    @Test
    void iceArchitectFullObjectiveHasSupportedRunwayAndAcceptsNormalMovement() throws Exception {
        BirdGame3 game = preparedGame();
        game.headlessHarnessMode = true;
        prepareEncounter(game, route(game).get(6));
        Bird player = game.players[0];

        Platform exitShelf = game.platforms.stream()
                .filter(platform -> platform.y == BirdGame3.LAST_ICE_MAIN_Y + 155.0)
                .filter(platform -> platform.x <= BirdGame3.LAST_ICE_EXIT_X - 260.0)
                .filter(platform -> platform.x + platform.w >= BirdGame3.LAST_ICE_EXIT_X + 260.0)
                .findFirst()
                .orElseThrow(() -> new AssertionError("Ice Architect needs grounded runway on both sides of its gate."));
        assertTrue(game.windVents.stream().noneMatch(vent ->
                        vent.x + vent.w >= BirdGame3.LAST_ICE_EXIT_X - 420.0
                                && vent.x <= BirdGame3.LAST_ICE_EXIT_X + 180.0),
                "An updraft must not lift Penguin out of the gate approach.");

        for (BirdGame3.ClassicIceworkAnchor anchor : iceworks(game)) {
            player.penguinSnowFort = new Bird.PenguinSnowFort(anchor.x, anchor.y, 1, false);
            game.applyPenguinClassicRuntimeEffects();
        }
        for (int tick = 0; tick < 100; tick++) {
            game.simTick++;
            game.applyPenguinClassicRuntimeEffects();
        }
        assertTrue((boolean) getField(game, "classicPenguinArchitectExitOpen"));

        player.x = BirdGame3.LAST_ICE_EXIT_TRIGGER_X - player.bodyWidth() - 4.0;
        player.y = exitShelf.y - player.bodyHeight();
        player.prevX = player.x;
        player.prevY = player.y;
        player.vx = 12.0;
        player.vy = 0.0;
        player.facingRight = true;
        player.penguinBellySlideTimer = 0;

        // Exercise the real Bird update and route update ordering with ordinary
        // movement. An open exit must never depend on a hidden special timer.
        player.update(1.0);
        assertEquals(0, player.penguinBellySlideTimer);
        assertTrue(player.x + player.bodyWidth() >= BirdGame3.LAST_ICE_EXIT_TRIGGER_X);
        game.applyPenguinClassicRuntimeEffects();

        assertTrue((boolean) getField(game, "classicPenguinArchitectCompleted"));
        assertSame(player, game.harnessWinner);
    }

    @Test
    void lastSunHasThreeStocksNoUltAndMeltsOnlyAuthoredTerraces() throws Exception {
        BirdGame3 game = preparedGame();
        prepareEncounter(game, route(game).getLast());
        invoke(game, "applyClassicEncounterStockOverrides", new Class<?>[0]);
        Bird boss = firstEnemy(game);
        int startingPlatforms = game.platforms.size();

        assertEquals(3, game.scores[boss.playerIndex]);
        assertEquals(3, game.scores[0]);
        assertEquals(1.58, boss.sizeMultiplier, 0.0001);
        assertFalse(boss.hasUltimate());
        assertEquals(BirdGame3.LAST_ICE_MAIN_Y, game.players[0].bodyBottomY(), 0.001);
        assertEquals(BirdGame3.LAST_ICE_MAIN_Y, boss.bodyBottomY(), 0.001);

        game.players[0].setTrailerSmashDamagePercent(80.0);
        game.scores[boss.playerIndex] = 2;
        game.applyPenguinClassicRuntimeEffects();
        assertEquals(1, getField(game, "classicLastSunPhase"));
        assertEquals(44.0, game.players[0].smashDamagePercent(), 0.001);
        assertEquals(startingPlatforms - 2, game.platforms.size());
        game.scores[boss.playerIndex] = 1;
        game.applyPenguinClassicRuntimeEffects();
        assertEquals(2, getField(game, "classicLastSunPhase"));
        assertEquals(startingPlatforms - 4, game.platforms.size());
        assertTrue(game.platforms.stream().anyMatch(platform -> platform.w == BirdGame3.LAST_ICE_MAIN_W),
                "The main shelf must never be removed by the boss phase.");
    }

    @Test
    void combatRouteUsesTwoPlayerStocksSoIceworksHaveTimeToMatter() throws Exception {
        BirdGame3 game = preparedGame();
        for (int round = 0; round < 6; round++) {
            prepareEncounter(game, route(game).get(round));
            assertEquals(2, game.scores[0], "Penguin stock count in round " + (round + 1));
        }
        assertTrue(ClassicBalanceLab.isObjectiveRound(ClassicEncounterStyle.ICE_ARCHITECT));
    }

    @Test
    void lastIceShelfIsConnectedSupportedAndUnlockedByPenguinsExistingBadge() throws Exception {
        BirdGame3 game = preparedGame();
        game.selectedMap = BirdGame3.MapType.FROSTBITE_FJORD;
        game.selectedMapVariant = MapVariant.LAST_ICE_SHELF;
        invoke(game, "setupMatchArenaGeometry", new Class<?>[0]);
        invoke(game, "applySelectedMapVariantArena", new Class<?>[0]);

        Platform main = game.platforms.stream()
                .filter(platform -> platform.w == BirdGame3.LAST_ICE_MAIN_W).findFirst().orElseThrow();
        assertEquals("THE LAST ICE SHELF", main.signText);
        assertEquals(BirdGame3.LAST_ICE_MAIN_X, main.x, 0.001);
        assertTrue(game.usesIslandBoundsForCurrentArena());
        assertFalse(game.availableStageChoices(BirdGame3.StageRandomPool.VARIANTS).stream()
                .anyMatch(choice -> choice.variant() == MapVariant.LAST_ICE_SHELF));
        setField(game, "lastIceShelfUnlocked", true);
        assertTrue(game.availableStageChoices(BirdGame3.StageRandomPool.VARIANTS).stream()
                .anyMatch(choice -> choice.variant() == MapVariant.LAST_ICE_SHELF));

        BirdGame3ProfileProgressState legacy = new BirdGame3ProfileProgressState();
        legacy.classicCompleted[BirdType.PENGUIN.ordinal()] = true;
        invoke(game, "applyProfileProgressState",
                new Class<?>[]{BirdGame3ProfileProgressState.class}, legacy);
        assertTrue((boolean) getField(game, "lastIceShelfUnlocked"));
    }

    private static Bird firstEnemy(BirdGame3 game) {
        for (Bird bird : game.players) {
            if (bird != null && game.getEffectiveTeam(bird.playerIndex) == 2
                    && game.scores[bird.playerIndex] > 0) return bird;
        }
        return null;
    }

    private static void eliminateEnemyTeam(BirdGame3 game) {
        for (Bird bird : game.players) {
            if (bird != null && game.getEffectiveTeam(bird.playerIndex) == 2) {
                game.scores[bird.playerIndex] = 0;
                bird.health = 0.0;
            }
        }
    }

    @SuppressWarnings("unchecked")
    private static List<BirdGame3.ClassicIceworkAnchor> iceworks(BirdGame3 game) throws Exception {
        return (List<BirdGame3.ClassicIceworkAnchor>) getField(game, "classicPenguinIceworks");
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
        invoke(game, "applyClassicEncounterStockOverrides", new Class<?>[0]);
        invoke(game, "applyClassicEncounterArenaModifiers", new Class<?>[]{ClassicEncounter.class}, encounter);
        invoke(game, "positionClassicEncounterSpawns", new Class<?>[]{ClassicEncounter.class}, encounter);
    }

    private static BirdGame3 preparedGame() throws Exception {
        BirdGame3 game = new BirdGame3();
        game.classicModeActive = true;
        setField(game, "classicSelectedBird", BirdType.PENGUIN);
        setField(game, "classicDifficulty", 5.0);
        return game;
    }

    @SuppressWarnings("unchecked")
    private static List<ClassicEncounter> route(BirdGame3 game) throws Exception {
        return (List<ClassicEncounter>) invoke(game, "buildClassicRun",
                new Class<?>[]{BirdType.class}, BirdType.PENGUIN);
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
