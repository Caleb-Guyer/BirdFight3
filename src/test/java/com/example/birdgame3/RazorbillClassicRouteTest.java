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

class RazorbillClassicRouteTest {
    @Test
    void razorbillHasTheApprovedEightEncounterWorldseamRoute() throws Exception {
        List<ClassicEncounter> route = route(new BirdGame3());

        assertEquals(8, route.size());
        assertEquals(List.of("First Incision", "Death by a Thousand Cuts", "The Blunt Edge",
                        "The Hand That Holds", "A Perfect Reflection", "Bonus: Between the Lines",
                        "The Broken Guard", "The Last Division"),
                route.stream().map(encounter -> encounter.name).toList());
        assertEquals(List.of(MapType.GLASSWIND_CAUSEWAY, MapType.CITY, MapType.ASHFALL_CATHEDRAL,
                        MapType.PRISON, MapType.GLASSWIND_CAUSEWAY, MapType.WORLDSEAM,
                        MapType.WORLDSEAM, MapType.WORLDSEAM),
                route.stream().map(encounter -> encounter.map).toList());
        assertEquals(ClassicEncounterStyle.RAZORBILL_SPLINTERS, route.get(1).style);
        assertEquals(ClassicEncounterStyle.RAZORBILL_REFLECTION, route.get(4).style);
        assertEquals(ClassicEncounterStyle.BETWEEN_LINES, route.get(5).style);
        assertEquals(ClassicEncounterStyle.SEAM_WARDEN_GAUNTLET, route.get(6).style);
        assertEquals(ClassicEncounterStyle.SEAMREAVER_BOSS, route.get(7).style);
        assertTrue(route.getLast().bossFight);
        assertEquals("THE LINE BETWEEN WORLDS", invoke(new BirdGame3(), "classicRouteTitle",
                new Class<?>[]{BirdType.class}, BirdType.RAZORBILL));
    }

    @Test
    void betweenLinesIsAnObjectiveWithFourFixedSafeLanesAndAnAnchorFinish() throws Exception {
        BirdGame3 game = prepared(5, 0x5E_AA01L, 0x5E_AA02L);
        game.headlessHarnessMode = true;

        assertFalse(game.classicUsesSmashRules());
        assertArrayEquals(new int[]{1, 3, 0, 2}, BirdGame3.RAZORBILL_BONUS_SAFE_LANES);
        set(game, "classicRazorbillBonusReady", true);
        assertTrue(game.registerRazorbillBonusAnchorStrike());
        assertEquals(75, get(game, "classicBonusCoins"));
        assertSame(game.players[0], game.harnessWinner);
    }

    @Test
    void splintersAndOriginalWardensUseThreeUltlessWaves() {
        BirdGame3 splinters = prepared(1, 0x5E_AA03L, 0x5E_AA04L);
        assertThreeWaves(splinters, true);

        BirdGame3 wardens = prepared(6, 0x5E_AA05L, 0x5E_AA06L);
        assertEquals(3, wardens.scores[0]);
        assertThreeWaves(wardens, true);
    }

    @Test
    void seamWardensWinInsteadOfBeingCheesedWhenTimeExpires() {
        BirdGame3 game = prepared(6, 0x5E_AA11L, 0x5E_AA12L);
        game.headlessHarnessMode = true;
        Bird warden = firstEnemy(game);

        assertTrue(game.isClassicSeamWardenActive());
        game.finishClassicSeamWardenFromTimeout();

        assertSame(warden, game.harnessWinner);
    }

    @Test
    void seamreaverIsAnOriginalFlyingStaminaBossWithAnAntiSpamGuard() throws Exception {
        BirdGame3 game = prepared(7, 0x5E_AA07L, 0x5E_AA08L);
        Bird player = game.players[0];
        Bird boss = firstEnemy(game);

        assertNotNull(boss);
        assertTrue(game.isClassicStaminaBoss(boss));
        assertFalse(game.isAI[boss.playerIndex]);
        assertFalse(boss.hasUltimate());
        assertEquals(3, game.scores[0]);
        assertEquals(1, game.scores[boss.playerIndex]);
        assertEquals(BirdGame3.SEAMREAVER_BASE_HEALTH, boss.health, 0.0001);
        assertEquals("music-razorbill-seamreaver.mp3",
                invoke(game, "gameplayMusicFile", new Class<?>[0]));

        double openHit = player.applyUnshieldedDamageTo(boss, 8.0);
        for (int i = 0; i < 4 && (int) get(game, "classicSeamreaverGuardTimer") == 0; i++) {
            player.applyUnshieldedDamageTo(boss, 8.0);
        }
        assertTrue((int) get(game, "classicSeamreaverGuardTimer") > 0);
        double guardedHit = player.applyUnshieldedDamageTo(boss, 8.0);
        assertTrue(guardedHit < openHit * 0.25);

        double startX = boss.bodyCenterX();
        double minX = startX;
        double minY = boss.bodyCenterY();
        for (int tick = 0; tick < 180 && game.harnessTick(); tick++) {
            minX = Math.min(minX, boss.bodyCenterX());
            minY = Math.min(minY, boss.bodyCenterY());
        }
        assertTrue(minX < startX - 200.0);
        assertTrue(minY < (double) get(game, "battlefieldIslandY") - 250.0);
    }

    @Test
    void worldseamGatesPairWithoutChangingMomentum() {
        BirdGame3 game = prepared(5, 0x5E_AA09L, 0x5E_AA0AL);
        Bird player = game.players[0];
        player.x = 1_450.0 - player.bodyWidth() * 0.5;
        player.y = BirdGame3.GROUND_Y - 440.0 - player.bodyHeight() * 0.5;
        player.vx = 7.25;
        player.vy = -3.5;

        game.applyRazorbillArenaRuntimeEffects();

        assertTrue(player.bodyCenterX() > 4_550.0);
        assertEquals(7.25, player.vx, 0.0);
        assertEquals(-3.5, player.vy, 0.0);
    }

    @Test
    void routeNeverChangesRazorbillsOrdinaryFighterKit() {
        BirdGame3 game = prepared(7, 0x5E_AA0BL, 0x5E_AA0CL);
        Bird player = game.players[0];
        double size = player.baseSizeMultiplier;
        double power = player.basePowerMultiplier;
        double speed = player.baseSpeedMultiplier;

        for (int tick = 0; tick < 240; tick++) {
            game.simTick++;
            game.applyRazorbillClassicRuntimeEffects();
        }

        assertEquals(size, player.baseSizeMultiplier, 0.0);
        assertEquals(power, player.basePowerMultiplier, 0.0);
        assertEquals(speed, player.baseSpeedMultiplier, 0.0);
    }

    @Test
    void mapsEndingAndCreditedCc0MusicShipWithTheRoute() throws Exception {
        assertEquals(15, MapType.GLASSWIND_CAUSEWAY.ordinal());
        assertEquals(16, MapType.WORLDSEAM.ordinal());
        assertTrue(prepared(0, 0x5E_AA0DL, 0x5E_AA0EL).usesIslandBoundsForCurrentArena());
        assertTrue(prepared(5, 0x5E_AA0FL, 0x5E_AA10L).usesIslandBoundsForCurrentArena());

        BirdGame3 progress = new BirdGame3();
        assertFalse((boolean) invoke(progress, "isMapUnlocked",
                new Class<?>[]{MapType.class}, MapType.WORLDSEAM));
        progress.setClassicCompleted(BirdType.RAZORBILL);
        assertTrue((boolean) invoke(progress, "isMapUnlocked",
                new Class<?>[]{MapType.class}, MapType.GLASSWIND_CAUSEWAY));
        assertTrue((boolean) invoke(progress, "isMapUnlocked",
                new Class<?>[]{MapType.class}, MapType.WORLDSEAM));

        for (String file : List.of("music-razorbill-glasswind.mp3", "music-razorbill-worldseam.mp3",
                "music-razorbill-seamreaver.mp3", "music-razorbill-ending.mp3")) {
            Path path = Path.of("src/main/resources/sounds", file);
            assertTrue(Files.exists(path), file);
            assertTrue(path.toFile().length() > 900_000, file + " should be a full cue, not a stinger.");
        }
        ClassicEndingContent.Cinematic ending = ClassicEndingContent.endingFor(BirdType.RAZORBILL).cinematic();
        assertTrue(ClassicEndingContent.isRazorbillFinalCut(ending));
        assertEquals("music-razorbill-ending.mp3", ending.musicCue());
        assertEquals(6, ending.beats().size());
    }

    private static void assertThreeWaves(BirdGame3 game, boolean normalAi) {
        for (int wave = 0; wave < 3; wave++) {
            Bird enemy = firstEnemy(game);
            assertNotNull(enemy);
            assertEquals(normalAi, game.isAI[enemy.playerIndex]);
            assertFalse(enemy.hasUltimate());
            enemy.health = 0.0;
            game.scores[enemy.playerIndex] = 0;
            assertEquals(wave < 2, game.holdClassicRazorbillEncounterOpen());
        }
    }

    private static BirdGame3 prepared(int round, long routeSeed, long matchSeed) {
        BirdGame3 game = new BirdGame3();
        game.harnessPrepareClassicEncounter(BirdType.RAZORBILL, round, 5.0, 6, routeSeed, matchSeed);
        return game;
    }

    private static Bird firstEnemy(BirdGame3 game) {
        for (int slot = 1; slot < game.activePlayers; slot++) {
            Bird bird = game.players[slot];
            if (bird != null && game.getEffectiveTeam(slot) == 2 && game.scores[slot] > 0) return bird;
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private static List<ClassicEncounter> route(BirdGame3 game) throws Exception {
        return (List<ClassicEncounter>) invoke(game, "buildRazorbillClassicRun", new Class<?>[0]);
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
