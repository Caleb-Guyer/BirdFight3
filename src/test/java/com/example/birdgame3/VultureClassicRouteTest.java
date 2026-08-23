package com.example.birdgame3;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static com.example.birdgame3.BirdGame3.BirdType;
import static com.example.birdgame3.BirdGame3.ClassicEncounter;
import static com.example.birdgame3.BirdGame3.ClassicEncounterStyle;
import static com.example.birdgame3.BirdGame3.MapType;
import static org.junit.jupiter.api.Assertions.*;

class VultureClassicRouteTest {
    @Test
    void titanDockRaidKeepsItsGiantThreatWithoutBecomingARouteWall() throws Exception {
        ClassicEncounter tide = route(new BirdGame3()).get(3);

        assertEquals(2, tide.enemies.length);
        assertEquals(124.0, tide.enemies[0].health(), 0.0001);
        assertEquals(0.68, tide.enemies[0].powerMult(), 0.0001);
        assertEquals(64.0, tide.enemies[1].health(), 0.0001);
        assertEquals(0.58, tide.enemies[1].powerMult(), 0.0001);
    }
    @Test
    void vultureHasEightSelfContainedEncountersInNothingGoesToWaste() throws Exception {
        List<ClassicEncounter> route = route(new BirdGame3());

        assertEquals(8, route.size());
        assertEquals(List.of("Ashes Still Warm", "The Smallest Share", "Salvage Rights",
                        "What the Tide Left", "False Flock", "The Last Auction",
                        "Bonus: Final Inventory", "The Final Account"),
                route.stream().map(encounter -> encounter.name).toList());
        assertEquals(List.of(MapType.ASHFALL_CATHEDRAL, MapType.CITY, MapType.CARRION_EXCHANGE,
                        MapType.DOCK, MapType.CAVE, MapType.SILENT_AMPHITHEATER,
                        MapType.CARRION_EXCHANGE, MapType.CARRION_EXCHANGE),
                route.stream().map(encounter -> encounter.map).toList());
        assertEquals(ClassicEncounterStyle.VULTURE_FALSE_FLOCK, route.get(4).style);
        assertEquals(5, route.get(4).cpuLevel,
                "False Flock should test the mirror kit without giving every impostor a reaction-speed advantage.");
        assertEquals(ClassicEncounterStyle.VULTURE_AUCTION_GAUNTLET, route.get(5).style);
        assertEquals(ClassicEncounterStyle.FINAL_INVENTORY, route.get(6).style);
        assertEquals(ClassicEncounterStyle.DEBT_ENGINE_BOSS, route.get(7).style);
        assertTrue(route.getLast().bossFight);
        assertEquals("NOTHING GOES TO WASTE", invoke(new BirdGame3(), "classicRouteTitle",
                new Class<?>[]{BirdType.class}, BirdType.VULTURE));
    }

    @Test
    void aftermathDamageAndStocksAreAuthoredButNeverCarryToTheNextBell() {
        BirdGame3 ashes = prepared(0, 0x7A1101L, 0x7A1102L);
        Bird phoenix = firstEnemy(ashes);
        assertNotNull(phoenix);
        assertEquals(40.0, phoenix.smashDamagePercent(), 0.001);
        assertEquals(2, ashes.scores[phoenix.playerIndex]);

        BirdGame3 smallShare = prepared(1, 0x7A1103L, 0x7A1104L);
        for (int slot = 1; slot < smallShare.activePlayers; slot++) {
            Bird enemy = smallShare.players[slot];
            assertNotNull(enemy);
            assertEquals(35.0, enemy.smashDamagePercent(), 0.001);
            assertFalse(enemy.hasUltimate());
        }

        BirdGame3 salvage = prepared(2, 0x7A1105L, 0x7A1106L);
        assertEquals(30.0, firstEnemy(salvage).smashDamagePercent(), 0.001,
                "Every round authors a fresh aftermath value instead of carrying the previous fight's damage.");
    }

    @Test
    void falseFlockAndAuctionAreThreeSeparateUltlessWaves() {
        assertThreeWaves(prepared(4, 0x7A1110L, 0x7A1111L));
        assertThreeWaves(prepared(5, 0x7A1112L, 0x7A1113L));
    }

    @Test
    void falseFlockRepairsVultureBetweenItsThreeMirrorDuels() {
        BirdGame3 game = prepared(4, 0x7A1114L, 0x7A1115L);
        Bird player = game.players[0];
        Bird enemy = firstEnemy(game);
        assertNotNull(enemy);
        player.setTrailerSmashDamagePercent(80.0);
        enemy.health = 0.0;
        game.scores[enemy.playerIndex] = 0;

        assertTrue(game.holdClassicVultureEncounterOpen());
        assertEquals(80.0 - BirdGame3.VULTURE_FALSE_FLOCK_CHECKPOINT_REPAIR,
                player.smashDamagePercent(), 0.001,
                "Each mirror should begin as a readable duel instead of inheriting nearly all prior damage.");
    }

    @Test
    void finalInventoryCanBeCompletedWithOrdinaryAttacksAndFailForwardsOnTimeout() throws Exception {
        BirdGame3 game = prepared(6, 0x7A1120L, 0x7A1121L);
        game.headlessHarnessMode = true;
        Bird player = game.players[0];
        @SuppressWarnings("unchecked")
        List<Object> locks = (List<Object>) get(game, "classicSalvageLocks");
        assertEquals(7, locks.size());

        for (Object lock : locks) {
            player.x = (double) get(lock, "x") - player.bodyWidth() * 0.5;
            player.y = (double) get(lock, "y") - player.bodyHeight() * 0.5;
            setAttack(game, true);
            game.applyVultureClassicRuntimeEffects();
            setAttack(game, false);
            for (int tick = 0; tick < 12; tick++) game.applyVultureClassicRuntimeEffects();
            assertTrue((boolean) get(lock, "broken"));
        }
        assertTrue((boolean) get(game, "classicInventoryExitOpen"));
        player.x = BirdGame3.FINAL_INVENTORY_EXIT_X;
        game.applyVultureClassicRuntimeEffects();
        assertTrue(game.matchEnded);
        assertSame(player, game.harnessWinner);

        BirdGame3 timeout = prepared(6, 0x7A1122L, 0x7A1123L);
        timeout.headlessHarnessMode = true;
        timeout.finishClassicFinalInventoryFromTimeout();
        assertTrue(timeout.matchEnded);
        assertSame(timeout.players[0], timeout.harnessWinner);
    }

    @Test
    void debtEngineIsAnOriginalMovingStaminaBossThatRejectsAttackSpam() throws Exception {
        BirdGame3 game = prepared(7, 0x7A1130L, 0x7A1131L);
        Bird player = game.players[0];
        Bird boss = firstEnemy(game);

        assertNotNull(boss);
        assertTrue(game.isClassicStaminaBoss(boss));
        assertFalse(game.isAI[boss.playerIndex]);
        assertFalse(boss.hasUltimate());
        assertEquals(3, game.scores[0]);
        assertEquals(BirdGame3.DEBT_ENGINE_BASE_HEALTH, boss.health, 0.001);
        assertEquals(300.0, boss.health, 0.001,
                "The Debt Engine needs enough stamina to survive Vulture's authored finishers without becoming a damage sponge.");
        assertEquals("music-vulture-debt-engine.mp3",
                invoke(game, "gameplayMusicFile", new Class<?>[0]));

        player.x = boss.x;
        player.y = boss.y;
        for (int hit = 0; hit < 7; hit++) game.onClassicStaminaBossDamaged(boss, player, 8.0);
        assertTrue((int) get(game, "classicDebtEngineReversalTimer") > 0);
        assertTrue(game.classicStaminaBossIncomingDamageScale(boss) < 0.65);

        double startX = boss.bodyCenterX();
        double minX = startX;
        double maxX = startX;
        for (int tick = 0; tick < 220 && game.harnessTick(); tick++) {
            minX = Math.min(minX, boss.bodyCenterX());
            maxX = Math.max(maxX, boss.bodyCenterX());
        }
        assertTrue(maxX - minX > 250.0,
                "The Debt Engine must use its crane track instead of sitting still under repeated attacks.");
    }

    @Test
    void debtEngineUsesRealStaminaRulesAndCannotBeCheesedByTimeOrBlastZones() throws Exception {
        BirdGame3 damageGame = prepared(7, 0x7A1132L, 0x7A1133L);
        Bird player = damageGame.players[0];
        Bird boss = firstEnemy(damageGame);
        double startingHealth = boss.health;

        double dealt = player.applyUnshieldedDamageTo(boss, 40.0);
        assertTrue(dealt > 0.0);
        assertEquals(startingHealth - dealt, boss.health, 0.0001);
        assertEquals(0.0, (double) get(boss, "smashDamage"), 0.0001,
                "The giant health bar must not secretly fill an ordinary launch meter.");
        assertEquals(1, damageGame.scores[boss.playerIndex]);

        BirdGame3 timeoutGame = prepared(7, 0x7A1134L, 0x7A1135L);
        Bird timeoutBoss = firstEnemy(timeoutGame);
        timeoutGame.headlessHarnessMode = true;
        timeoutGame.matchTimer = 0;
        assertFalse(timeoutGame.harnessTick());
        assertSame(timeoutBoss, timeoutGame.harnessWinner,
                "Waiting out an intact Debt Engine must lose the encounter.");

        BirdGame3 blastGame = prepared(7, 0x7A1136L, 0x7A1137L);
        Bird blastBoss = firstEnemy(blastGame);
        double blastHealth = blastBoss.health;
        invoke(blastBoss, "handleSmashBlastZoneKo",
                new Class<?>[]{boolean.class, boolean.class, double.class, double.class,
                        double.class, double.class, String.class, boolean.class, double.class, double.class},
                false, true, 690.0, 5_310.0, 3_000.0, BirdGame3.GROUND_Y - 300.0,
                "off the right side", false, 5_700.0, BirdGame3.GROUND_Y);
        assertEquals(blastHealth, blastBoss.health, 0.0);
        assertEquals(1, blastGame.scores[blastBoss.playerIndex]);
        assertEquals(blastGame.battlefieldSpawnCenterX() - blastBoss.bodyWidth() * 0.5,
                blastBoss.x, 0.0001);
        assertEquals(0.0, blastBoss.vx, 0.0);
        assertEquals(0.0, blastBoss.vy, 0.0);
    }

    @Test
    void debtEngineCrusherWarnsBeforeImpactAndStopsAtTheStageFloor() throws Exception {
        BirdGame3 game = prepared(7, 0x7A1138L, 0x7A1139L);
        Bird player = game.players[0];
        Bird boss = firstEnemy(game);
        double floorY = (double) get(game, "battlefieldIslandY");

        assertEquals(0.22, BirdGame3.debtEngineCrusherWarningAlpha(180.0, floorY), 0.0001);
        assertEquals(0.92, BirdGame3.debtEngineCrusherWarningAlpha(floorY, floorY), 0.0001);
        invoke(game, "spawnClassicDebtEngineAttack",
                new Class<?>[]{Bird.class, Bird.class, int.class}, boss, player, 1);

        @SuppressWarnings("unchecked")
        List<Object> shots = (List<Object>) get(game, "classicDebtProjectiles");
        assertEquals(5, shots.size());
        for (Object shot : shots) {
            double x = (double) get(shot, "x");
            assertTrue(x >= 820.0 && x <= 5_180.0, "Crusher warning escaped the playable floor.");
            set(shot, "y", floorY - 50.0);
            set(shot, "vy", 16.0);
        }
        player.x = 690.0;
        player.y = floorY - 900.0;
        game.applyVultureClassicRuntimeEffects();
        assertTrue(shots.isEmpty(), "Impacted crushers must not fall visibly through the map.");
    }

    @Test
    void exchangeHazardHasAFullWarningWindowAndUsesDeterministicSimulationTicks() throws Exception {
        BirdGame3 game = prepared(2, 0x7A1140L, 0x7A1141L);
        Bird player = game.players[0];
        player.x = BirdGame3.CARRION_MAGNET_X[0] + 360.0 - player.bodyWidth() * 0.5;
        player.y = (double) get(game, "battlefieldIslandY") - player.bodyHeight() - 80.0;
        player.vx = 0.0;

        game.simTick = 289;
        assertFalse(BirdGame3.carrionMagnetActiveAtTick(game.simTick));
        game.applyVultureArenaRuntimeEffects();
        assertEquals(0.0, player.vx, 0.0);

        game.simTick = 290;
        assertTrue(BirdGame3.carrionMagnetActiveAtTick(game.simTick));
        game.applyVultureArenaRuntimeEffects();
        assertTrue(player.vx < 0.0);
        assertFalse(BirdGame3.carrionMagnetActiveAtTick(326));
    }

    @Test
    void routeMapsSpawnSafelyUnlockWithBadgeAndBelongToTheStory() throws Exception {
        BirdGame3 exchange = prepared(2, 0x7A1150L, 0x7A1151L);
        assertTrue(exchange.usesIslandBoundsForCurrentArena());
        for (int slot = 0; slot < exchange.activePlayers; slot++) {
            Bird bird = exchange.players[slot];
            if (bird == null) continue;
            assertTrue(bird.bodyBottomY() <= (double) get(exchange, "battlefieldIslandY") + 1.0,
                    "Fighter " + slot + " spawned below Carrion Exchange.");
        }

        BirdGame3 progress = new BirdGame3();
        assertFalse((boolean) invoke(progress, "isMapUnlocked",
                new Class<?>[]{MapType.class}, MapType.CARRION_EXCHANGE));
        progress.setClassicCompleted(BirdType.VULTURE);
        assertTrue((boolean) invoke(progress, "isMapUnlocked",
                new Class<?>[]{MapType.class}, MapType.CARRION_EXCHANGE));

        StoryCampaign campaign = StoryCampaignContent.create();
        assertTrue(campaign.acts.stream().flatMap(act -> act.missions().stream())
                .anyMatch(mission -> mission.map() == MapType.CARRION_EXCHANGE));
    }

    @Test
    void newVultureArenasKeepFightersAndEveryInventoryObjectiveOnReachableSurfaces() throws Exception {
        for (int round : List.of(2, 6, 7)) {
            BirdGame3 game = prepared(round, 0x7A1160L + round, 0x7A1170L + round);
            double floorX = (double) get(game, "battlefieldIslandX");
            double floorW = (double) get(game, "battlefieldIslandW");
            double floorY = (double) get(game, "battlefieldIslandY");
            for (int slot = 0; slot < game.activePlayers; slot++) {
                Bird bird = game.players[slot];
                if (bird == null) continue;
                assertTrue(bird.bodyCenterX() >= floorX - 450.0
                                && bird.bodyCenterX() <= floorX + floorW + 450.0,
                        "Fighter " + slot + " spawned beyond the authored arena in round " + (round + 1));
                assertTrue(bird.bodyBottomY() <= floorY + 1.0,
                        "Fighter " + slot + " spawned below the stage in round " + (round + 1));
            }
        }

        BirdGame3 objective = prepared(6, 0x7A1180L, 0x7A1181L);
        double floorX = (double) get(objective, "battlefieldIslandX");
        double floorW = (double) get(objective, "battlefieldIslandW");
        double floorY = (double) get(objective, "battlefieldIslandY");
        @SuppressWarnings("unchecked")
        List<Object> locks = (List<Object>) get(objective, "classicSalvageLocks");
        double previousX = Double.NEGATIVE_INFINITY;
        for (Object lock : locks) {
            double x = (double) get(lock, "x");
            assertTrue(x > previousX, "The next-lock guide depends on a clear left-to-right order.");
            assertTrue(x >= floorX + 100.0 && x <= floorX + floorW - 100.0);
            assertEquals(floorY - 112.0, (double) get(lock, "y"), 0.0001);
            previousX = x;
        }
        assertTrue(BirdGame3.FINAL_INVENTORY_EXIT_X >= floorX
                && BirdGame3.FINAL_INVENTORY_EXIT_X <= floorX + floorW);
    }

    @Test
    void vultureRouteStateIsClearedBetweenEncountersAndConstructArtSurvivesResults() throws Exception {
        BirdGame3 game = prepared(6, 0x7A1190L, 0x7A1191L);
        @SuppressWarnings("unchecked")
        List<Object> locks = (List<Object>) get(game, "classicSalvageLocks");
        set(locks.getFirst(), "broken", true);
        set(game, "classicInventoryExitOpen", true);
        @SuppressWarnings("unchecked")
        List<Object> projectiles = (List<Object>) get(game, "classicDebtProjectiles");
        projectiles.add(null);

        game.harnessPrepareClassicEncounter(BirdType.VULTURE, 7, 5.0, 6,
                0x7A1192L, 0x7A1193L);
        assertTrue(((List<?>) get(game, "classicSalvageLocks")).isEmpty());
        assertTrue(((List<?>) get(game, "classicDebtProjectiles")).isEmpty());
        assertFalse((boolean) get(game, "classicInventoryExitOpen"));

        Bird boss = firstEnemy(game);
        assertTrue(game.usesClassicConstructVictoryPortrait(boss));
        assertFalse(game.usesClassicConstructVictoryPortrait(game.players[0]));
        assertEquals("DEBT ENGINE", invoke(game, "matchSummaryBirdLabel",
                new Class<?>[]{Bird.class}, boss));
    }

    @Test
    void actualStagePhotosCc0MusicAndFinalAccountEndingShipTogether() {
        for (BirdGame3.StageChoice choice : List.of(
                BirdGame3.StageChoice.main(MapType.CARRION_EXCHANGE),
                new BirdGame3.StageChoice(MapType.CARRION_EXCHANGE, BirdGame3.MapVariant.SORTING_FLOOR),
                new BirdGame3.StageChoice(MapType.CARRION_EXCHANGE, BirdGame3.MapVariant.RECLAMATION_CORE))) {
            assertTrue(StagePreviewRenderer.capturedPreviewResourceExists(choice), choice.toString());
        }
        for (String file : List.of("music-vulture-exchange.mp3", "music-vulture-debt-engine.mp3",
                "music-vulture-ending.mp3")) {
            Path path = Path.of("src/main/resources/sounds", file);
            assertTrue(Files.exists(path), file);
            assertTrue(path.toFile().length() > 1_000_000, file + " should be a complete cue.");
        }

        ClassicEndingContent.Cinematic ending = ClassicEndingContent.endingFor(BirdType.VULTURE).cinematic();
        assertTrue(ClassicEndingContent.isVultureFinalAccount(ending));
        assertEquals("THE FINAL ACCOUNT", ending.title());
        assertEquals("music-vulture-ending.mp3", ending.musicCue());
        assertEquals(6, ending.beats().size());
    }

    private static void assertThreeWaves(BirdGame3 game) {
        for (int wave = 0; wave < 3; wave++) {
            Bird enemy = firstEnemy(game);
            assertNotNull(enemy);
            assertFalse(enemy.hasUltimate());
            enemy.health = 0.0;
            game.scores[enemy.playerIndex] = 0;
            assertEquals(wave < 2, game.holdClassicVultureEncounterOpen());
        }
    }

    private static BirdGame3 prepared(int round, long routeSeed, long matchSeed) {
        BirdGame3 game = new BirdGame3();
        game.harnessPrepareClassicEncounter(BirdType.VULTURE, round, 5.0, 6, routeSeed, matchSeed);
        return game;
    }

    private static Bird firstEnemy(BirdGame3 game) {
        for (int slot = 1; slot < game.activePlayers; slot++) {
            Bird bird = game.players[slot];
            if (bird != null && game.getEffectiveTeam(slot) == 2 && game.scores[slot] > 0) return bird;
        }
        return null;
    }

    private static void setAttack(BirdGame3 game, boolean down) throws Exception {
        boolean[][] input = (boolean[][]) get(game, "localActionPressed");
        input[0][3] = down;
    }

    @SuppressWarnings("unchecked")
    private static List<ClassicEncounter> route(BirdGame3 game) throws Exception {
        return (List<ClassicEncounter>) invoke(game, "buildVultureClassicRun", new Class<?>[0]);
    }

    private static Object invoke(Object target, String name, Class<?>[] types, Object... args) throws Exception {
        Method method = target.getClass().getDeclaredMethod(name, types);
        method.setAccessible(true);
        return method.invoke(target, args);
    }

    private static Object get(Object target, String name) throws Exception {
        Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        return field.get(target);
    }

    private static void set(Object target, String name, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        field.set(target, value);
    }
}
