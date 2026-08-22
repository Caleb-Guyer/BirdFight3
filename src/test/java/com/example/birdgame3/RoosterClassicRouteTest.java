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

class RoosterClassicRouteTest {
    @Test
    void broodMoraleUsesItsOwnRowBelowDifficulty() {
        assertTrue(BirdGame3.CLASSIC_INTRO_MORALE_Y
                        >= BirdGame3.CLASSIC_INTRO_DIFFICULTY_Y + BirdGame3.CLASSIC_INTRO_STATUS_HEIGHT + 8.0,
                "Brood Morale must clear the Difficulty pill with a visible gap.");
    }

    @Test
    void roosterHasTheApprovedEightEncounterNoOneLeftBehindRoute() throws Exception {
        List<ClassicEncounter> route = route(new BirdGame3());

        assertEquals(8, route.size());
        assertEquals(List.of(
                        "The First Call",
                        "Two Kinds of Leadership",
                        "Hold Formation",
                        "The Cagekeepers",
                        "One Hunter, Five Targets",
                        "The Last Night",
                        "Bonus: The Great Muster",
                        "The Broodbreaker"),
                route.stream().map(encounter -> encounter.name).toList());
        assertEquals(List.of(
                        MapVariant.STANDARD,
                        MapVariant.HARVEST_TRIBUNAL,
                        MapVariant.STANDARD,
                        MapVariant.STANDARD,
                        MapVariant.CARRION_THRONE,
                        MapVariant.PARLIAMENT_ROOFTOPS,
                        MapVariant.DAWNWATCH_BASTION,
                        MapVariant.DAWNWATCH_BASTION),
                route.stream().map(encounter -> encounter.variant).toList());
        assertEquals(ClassicEncounterStyle.BROOD_RECRUITMENT, route.get(0).style);
        assertEquals(ClassicEncounterStyle.BROOD_LEADERSHIP, route.get(1).style);
        assertEquals(ClassicEncounterStyle.BROOD_RESCUE, route.get(3).style);
        assertEquals(ClassicEncounterStyle.BROOD_HUNT, route.get(4).style);
        assertEquals(ClassicEncounterStyle.NIGHT_COMMAND, route.get(5).style);
        assertEquals(ClassicEncounterStyle.DAWN_MUSTER, route.get(6).style);
        assertEquals(ClassicEncounterStyle.BROODBREAKER_BOSS, route.get(7).style);
        assertTrue(route.getLast().bossFight);
    }

    @Test
    void authoredTeamsAndBossMatchTheProposal() throws Exception {
        List<ClassicEncounter> route = route(new BirdGame3());

        assertEquals(List.of(BirdType.PIGEON, BirdType.KIWI, BirdType.TITMOUSE),
                List.of(route.get(0).enemies[0].type(), route.get(0).enemies[1].type(), route.get(0).enemies[2].type()));
        assertEquals(BirdType.TURKEY, route.get(1).allies[0].type());
        assertEquals(List.of(BirdType.EAGLE, BirdType.FALCON),
                List.of(route.get(1).enemies[0].type(), route.get(1).enemies[1].type()));
        assertEquals(List.of(BirdType.ROADRUNNER, BirdType.HUMMINGBIRD),
                List.of(route.get(2).enemies[0].type(), route.get(2).enemies[1].type()));
        assertEquals(List.of(BirdType.HEISENBIRD, BirdType.OPIUMBIRD),
                List.of(route.get(3).enemies[0].type(), route.get(3).enemies[1].type()));
        assertEquals(BirdType.VULTURE, route.get(4).enemies[0].type());
        assertEquals(List.of(BirdType.RAVEN, BirdType.BAT),
                List.of(route.get(5).enemies[0].type(), route.get(5).enemies[1].type()));
        assertEquals(0, route.get(6).enemies.length);
        assertEquals(BirdType.RAVEN, route.getLast().enemies[0].type());
    }

    @Test
    void firstCallStartsWithOneChickAndHatchesRecruitsFromKos() throws Exception {
        BirdGame3 game = preparedGame();
        ClassicEncounter encounter = route(game).getFirst();
        prepareEncounter(game, encounter);

        assertEquals(1, ownedChicks(game));
        assertFalse(game.players[1].hasUltimate());
        assertFalse(game.players[2].hasUltimate());
        assertFalse(game.players[3].hasUltimate());

        game.scores[1] = 0;
        game.applyRoosterClassicRuntimeEffects();

        assertEquals(2, ownedChicks(game));
        assertTrue(((boolean[]) getField(game, "classicRoosterRecruitClaims"))[1]);
        assertTrue(game.chickMinions.stream()
                .filter(chick -> chick.owner == game.players[0])
                .allMatch(chick -> chick.maxAge > encounter.timerFrames));

        game.scores[2] = 0;
        game.scores[3] = 0;
        game.applyRoosterClassicRuntimeEffects();

        assertEquals(4, ownedChicks(game));
    }

    @Test
    void cagekeepersBeginWithTwoChicksAndThreeBreakableCagesAwardMorale() throws Exception {
        BirdGame3 game = preparedGame();
        prepareEncounter(game, route(game).get(3));

        assertEquals(2, ownedChicks(game));
        @SuppressWarnings("unchecked")
        List<BirdGame3.ClassicBroodCage> cages =
                (List<BirdGame3.ClassicBroodCage>) getField(game, "classicRoosterCages");
        assertEquals(3, cages.size());
        cages.forEach(cage -> cage.health = 0);

        game.applyRoosterClassicRuntimeEffects();

        assertTrue(cages.isEmpty());
        assertEquals(5, ownedChicks(game));
        assertEquals(1, getField(game, "classicRoosterMorale"));
        assertEquals(3, getField(game, "classicRoosterCagesRescued"));
    }

    @Test
    void moraleBuffsVeteransAndThirdCrestRescuesOneDefeatedChick() throws Exception {
        BirdGame3 game = preparedGame();
        prepareEncounter(game, route(game).get(2));
        setField(game, "classicRoosterMorale", 2);

        game.applyRoosterClassicRuntimeEffects();
        ChickMinion chick = game.chickMinions.stream().filter(candidate -> candidate.owner == game.players[0]).findFirst().orElseThrow();
        assertEquals(2, chick.classicMoraleApplied);
        assertTrue(chick.maxLife >= 5);
        assertEquals(0.72, chick.knockbackTakenMultiplier, 0.0001);

        setField(game, "classicRoosterMorale", 3);
        chick.life = 0;
        assertTrue(game.rescueClassicRoosterChick(chick));
        assertEquals(chick.maxLife, chick.life);
        assertTrue(chick.followingOwner);
        assertFalse(game.rescueClassicRoosterChick(chick));
    }

    @Test
    void greatMusterUsesFourOrderedCommandsAndHoldsTheBonusOpen() throws Exception {
        BirdGame3 game = preparedGame();
        prepareEncounter(game, route(game).get(6));
        Bird player = game.players[0];

        assertTrue(game.holdClassicRoosterEncounterOpen());
        assertEquals(1, ownedChicks(game));
        RoosterSpecials.spawnFollower(player, 1, false, 1);
        player.x = 1_060.0 - player.bodyWidth() * 0.5;
        player.y = BirdGame3.GROUND_Y - 540.0 - player.bodyHeight() * 0.5;
        player.roosterNeutralReuseTimer = 5;
        game.applyRoosterClassicRuntimeEffects();
        assertEquals(1, getField(game, "classicRoosterMusterStep"));

        ChickMinion thrown = game.chickMinions.stream().filter(candidate -> candidate.owner == player).findFirst().orElseThrow();
        thrown.x = 2_120.0 - thrown.width * 0.5;
        thrown.y = BirdGame3.GROUND_Y - 700.0 - thrown.height * 0.5;
        thrown.thrownFrames = 10;
        game.applyRoosterClassicRuntimeEffects();
        assertEquals(2, getField(game, "classicRoosterMusterStep"));

        player.x = 3_000.0 - player.bodyWidth() * 0.5;
        player.y = BirdGame3.GROUND_Y - 1_360.0;
        player.roosterCommandFxKind = 3;
        player.roosterCommandFxTimer = 10;
        game.applyRoosterClassicRuntimeEffects();
        assertEquals(3, getField(game, "classicRoosterMusterStep"));
    }

    @Test
    void broodbreakerHasThreeStocksRecallWindowAndThreeDawnBells() throws Exception {
        BirdGame3 game = preparedGame();
        prepareEncounter(game, route(game).getLast());
        invoke(game, "applyClassicEncounterStockOverrides", new Class<?>[0]);
        Bird player = game.players[0];
        Bird boss = game.players[1];

        assertEquals(3, game.scores[1]);
        assertFalse(boss.hasUltimate());
        assertEquals(1.68 * 0.82, boss.sizeMultiplier, 0.0001);

        game.scores[1] = 2;
        game.applyRoosterClassicRuntimeEffects();
        assertTrue((boolean) getField(game, "classicBroodbreakerCapturePhaseActive"));
        player.roosterDownReuseTimer = 5;
        game.applyRoosterClassicRuntimeEffects();
        assertTrue((boolean) getField(game, "classicBroodbreakerCaptureResolved"));

        game.scores[1] = 1;
        game.applyRoosterClassicRuntimeEffects();
        @SuppressWarnings("unchecked")
        List<BirdGame3.ClassicDawnBell> bells =
                (List<BirdGame3.ClassicDawnBell>) getField(game, "classicDawnBells");
        assertEquals(3, bells.size());
        assertTrue((boolean) getField(game, "classicBroodbreakerFinalPhaseActive"));
        assertEquals(1.28, boss.sizeMultiplier, 0.0001);

        ChickMinion chick = game.chickMinions.stream().filter(candidate -> candidate.owner == player).findFirst().orElseThrow();
        for (BirdGame3.ClassicDawnBell bell : bells) {
            chick.x = bell.x - chick.width * 0.5;
            chick.y = bell.y - chick.height * 0.5;
            chick.thrownFrames = 10;
            game.applyRoosterClassicRuntimeEffects();
        }
        assertTrue((boolean) getField(game, "classicBroodbreakerEclipseBroken"));
        assertEquals(1.16, boss.sizeMultiplier, 0.0001);
    }

    @Test
    void dawnwatchIsAStoryBackedUnlockableIslandAndRouteTitleIsAuthored() throws Exception {
        BirdGame3 game = preparedGame();
        game.selectedMap = BirdGame3.MapType.BEACON_CROWN;
        game.selectedMapVariant = MapVariant.DAWNWATCH_BASTION;
        invoke(game, "setupMatchArenaGeometry", new Class<?>[0]);
        invoke(game, "applySelectedMapVariantArena", new Class<?>[0]);

        assertTrue(game.usesIslandBoundsForCurrentArena());
        assertTrue(game.platforms.stream().anyMatch(platform -> platform.w == 3_240.0));
        assertFalse(game.availableStageChoices(BirdGame3.StageRandomPool.VARIANTS).stream()
                .anyMatch(choice -> choice.variant() == MapVariant.DAWNWATCH_BASTION));
        setField(game, "dawnwatchBastionUnlocked", true);
        assertTrue(game.availableStageChoices(BirdGame3.StageRandomPool.VARIANTS).stream()
                .anyMatch(choice -> choice.variant() == MapVariant.DAWNWATCH_BASTION));
        assertEquals(MapVariant.DAWNWATCH_BASTION,
                StoryCampaignContent.create().mission("green_convergence").mapVariant());
        assertEquals("NO ONE LEFT BEHIND",
                invoke(game, "classicRouteTitle", new Class<?>[]{BirdType.class}, BirdType.ROOSTER));
    }

    private static int ownedChicks(BirdGame3 game) {
        return (int) game.chickMinions.stream()
                .filter(chick -> chick.owner == game.players[0] && !chick.roosterSwarm && chick.life > 0)
                .count();
    }

    private static void prepareEncounter(BirdGame3 game, ClassicEncounter encounter) throws Exception {
        game.classicEncounter = encounter;
        game.selectedMap = encounter.map;
        game.selectedMapVariant = encounter.variant;
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
        setField(game, "classicSelectedBird", BirdType.ROOSTER);
        setField(game, "classicDifficulty", 5.0);
        return game;
    }

    @SuppressWarnings("unchecked")
    private static List<ClassicEncounter> route(BirdGame3 game) throws Exception {
        return (List<ClassicEncounter>) invoke(game, "buildClassicRun",
                new Class<?>[]{BirdType.class}, BirdType.ROOSTER);
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
