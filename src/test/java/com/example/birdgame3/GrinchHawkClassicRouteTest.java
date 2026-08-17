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

class GrinchHawkClassicRouteTest {
    @Test
    void grinchHawkHasEightIndependentJobsInTheLongestNight() throws Exception {
        List<ClassicEncounter> route = route(new BirdGame3());

        assertEquals(8, route.size());
        assertEquals(List.of("The First Package", "Cold Storage", "Express Delivery",
                        "The Good List", "Thieves' Honor", "The King's Ransom",
                        "Bonus: The Quiet Vault", "The Last Gift"),
                route.stream().map(encounter -> encounter.name).toList());
        assertEquals(List.of(MapType.CITY, MapType.FROSTBITE_FJORD, MapType.MIDNIGHT_WORKSHOP,
                        MapType.FOREST, MapType.SILENT_AMPHITHEATER, MapType.BEACON_CROWN,
                        MapType.MIDNIGHT_WORKSHOP, MapType.MIDNIGHT_WORKSHOP),
                route.stream().map(encounter -> encounter.map).toList());
        assertEquals(ClassicEncounterStyle.GRINCH_GUARD_GAUNTLET, route.get(3).style);
        assertEquals(ClassicEncounterStyle.QUIET_VAULT, route.get(6).style);
        assertEquals(ClassicEncounterStyle.BELLKEEPER_BOSS, route.get(7).style);
        assertTrue(route.getLast().bossFight);
        assertEquals("THE LONGEST NIGHT", invoke(new BirdGame3(), "classicRouteTitle",
                new Class<?>[]{BirdType.class}, BirdType.GRINCHHAWK));
    }

    @Test
    void everyJobStartsCleanInsteadOfCarryingRouteObjectsBetweenRounds() throws Exception {
        BirdGame3 game = prepared(3, 0x6A1101L, 0x6A1102L);
        set(game, "classicGrinchWaveIndex", 2);
        set(game, "classicVaultCompleted", true);
        set(game, "classicVaultExitOpen", true);

        game.harnessPrepareClassicEncounter(BirdType.GRINCHHAWK, 6, 5.0, 6,
                0x6A1103L, 0x6A1104L);

        assertEquals(0, get(game, "classicGrinchWaveIndex"));
        assertFalse((boolean) get(game, "classicVaultCompleted"));
        assertFalse((boolean) get(game, "classicVaultExitOpen"));
        assertEquals(3, ((List<?>) get(game, "classicVaultSeals")).size());

        game.harnessPrepareClassicEncounter(BirdType.GRINCHHAWK, 7, 5.0, 6,
                0x6A1105L, 0x6A1106L);
        assertTrue(((List<?>) get(game, "classicVaultSeals")).isEmpty());
        assertTrue(((List<?>) get(game, "classicBellkeeperProjectiles")).isEmpty());
        assertEquals(0, get(game, "classicBellkeeperPhase"));
    }

    @Test
    void goodListIsThreeSeparateUltlessGuardWaves() {
        BirdGame3 game = prepared(3, 0x6A1110L, 0x6A1111L);
        assertEquals(1, game.scores[0], "A target-band wave job should keep the standard one-stock pressure.");
        for (int wave = 0; wave < 3; wave++) {
            Bird enemy = firstEnemy(game);
            assertNotNull(enemy);
            assertFalse(enemy.hasUltimate());
            enemy.health = 0.0;
            game.scores[enemy.playerIndex] = 0;
            assertEquals(wave < 2, game.holdClassicGrinchHawkEncounterOpen());
        }
    }

    @Test
    void quietVaultUsesOrdinaryStrikesAndFailForwardsOnTimeout() throws Exception {
        BirdGame3 game = prepared(6, 0x6A1120L, 0x6A1121L);
        game.headlessHarnessMode = true;
        Bird player = game.players[0];
        @SuppressWarnings("unchecked")
        List<Object> seals = (List<Object>) get(game, "classicVaultSeals");

        for (Object seal : seals) {
            player.x = (double) get(seal, "x") - player.bodyWidth() * 0.5;
            player.y = (double) get(seal, "y") - player.bodyHeight() * 0.5;
            setAttack(game, true);
            game.applyGrinchHawkClassicRuntimeEffects();
            setAttack(game, false);
            game.applyGrinchHawkClassicRuntimeEffects();
            for (int frame = 0; frame < 12; frame++) game.applyGrinchHawkClassicRuntimeEffects();
            assertTrue((boolean) get(seal, "broken"));
        }
        assertTrue((boolean) get(game, "classicVaultExitOpen"));

        BirdGame3 timeout = prepared(6, 0x6A1122L, 0x6A1123L);
        timeout.headlessHarnessMode = true;
        timeout.finishClassicQuietVaultFromTimeout();
        assertTrue(timeout.matchEnded);
        assertSame(timeout.players[0], timeout.harnessWinner,
                "Missing the bonus must advance the route rather than strand the run.");
    }

    @Test
    void bellkeeperIsAnOriginalFlyingStaminaBossWithAntiSpamEscape() throws Exception {
        BirdGame3 game = prepared(7, 0x6A1130L, 0x6A1131L);
        Bird player = game.players[0];
        Bird boss = firstEnemy(game);

        assertNotNull(boss);
        assertTrue(game.isClassicStaminaBoss(boss));
        assertFalse(game.isAI[boss.playerIndex]);
        assertFalse(boss.hasUltimate());
        assertEquals(3, game.scores[0]);
        assertEquals(BirdGame3.BELLKEEPER_BASE_HEALTH, boss.health, 0.0001);
        assertEquals("music-grinch-bellkeeper.mp3",
                invoke(game, "gameplayMusicFile", new Class<?>[0]));

        player.x = boss.x;
        player.y = boss.y;
        for (int hit = 0; hit < 6; hit++) game.onClassicStaminaBossDamaged(boss, player, 8.0);
        assertTrue((int) get(game, "classicBellkeeperReversalTimer") > 0);
        assertTrue(game.classicStaminaBossIncomingDamageScale(boss) < 0.6);

        double startX = boss.bodyCenterX();
        double minX = startX;
        double maxX = startX;
        for (int tick = 0; tick < 240 && game.harnessTick(); tick++) {
            minX = Math.min(minX, boss.bodyCenterX());
            maxX = Math.max(maxX, boss.bodyCenterX());
        }
        assertTrue(maxX - minX > 180.0, "The Bellkeeper must patrol instead of allowing attack spam in place.");
    }

    @Test
    void midnightWorkshopUnlocksWithTheBadgeAndAlsoBelongsToTheStory() throws Exception {
        assertEquals(17, MapType.MIDNIGHT_WORKSHOP.ordinal());
        BirdGame3 progress = new BirdGame3();
        assertFalse((boolean) invoke(progress, "isMapUnlocked",
                new Class<?>[]{MapType.class}, MapType.MIDNIGHT_WORKSHOP));
        progress.setClassicCompleted(BirdType.GRINCHHAWK);
        assertTrue((boolean) invoke(progress, "isMapUnlocked",
                new Class<?>[]{MapType.class}, MapType.MIDNIGHT_WORKSHOP));

        StoryCampaign campaign = StoryCampaignContent.create();
        assertTrue(campaign.acts.stream().flatMap(act -> act.missions().stream())
                .anyMatch(mission -> mission.map() == MapType.MIDNIGHT_WORKSHOP));
    }

    @Test
    void workshopPressesHaveAWarningWindowAndOnlyStrikeDuringTheActiveCycle() {
        BirdGame3 game = prepared(2, 0x6A1140L, 0x6A1141L);
        Bird player = game.players[0];
        player.x = BirdGame3.MIDNIGHT_PRESS_X[0] - player.bodyWidth() * 0.5;
        player.y = 720.0 - player.bodyHeight() * 0.5;
        double before = player.smashDamagePercent();

        game.simTick = 339;
        game.applyGrinchHawkArenaRuntimeEffects();
        assertEquals(before, player.smashDamagePercent(), 0.0001,
                "The full warning cycle must remain non-damaging.");

        game.simTick = 340;
        game.applyGrinchHawkArenaRuntimeEffects();
        assertTrue(player.smashDamagePercent() > before);
        double afterHit = player.smashDamagePercent();
        game.simTick++;
        game.applyGrinchHawkArenaRuntimeEffects();
        assertEquals(afterHit, player.smashDamagePercent(), 0.0001,
                "A press cannot deal damage every simulation frame.");
    }

    @Test
    void literalStagePhotosCreditedMusicAndOpenSackEndingShipTogether() {
        for (BirdGame3.StageChoice choice : List.of(
                BirdGame3.StageChoice.main(MapType.MIDNIGHT_WORKSHOP),
                new BirdGame3.StageChoice(MapType.MIDNIGHT_WORKSHOP, BirdGame3.MapVariant.GIFT_VAULT),
                new BirdGame3.StageChoice(MapType.MIDNIGHT_WORKSHOP, BirdGame3.MapVariant.BELLKEEPER_VAULT))) {
            assertTrue(StagePreviewRenderer.capturedPreviewResourceExists(choice), choice.toString());
        }
        for (String file : List.of("music-grinch-workshop.mp3", "music-grinch-bellkeeper.mp3",
                "music-grinch-ending.mp3")) {
            Path path = Path.of("src/main/resources/sounds", file);
            assertTrue(Files.exists(path), file);
            assertTrue(path.toFile().length() > 1_000_000, file + " should be a complete cue.");
        }

        ClassicEndingContent.Cinematic ending = ClassicEndingContent.endingFor(BirdType.GRINCHHAWK).cinematic();
        assertTrue(ClassicEndingContent.isGrinchHawkOpenSack(ending));
        assertEquals("music-grinch-ending.mp3", ending.musicCue());
        assertEquals("THE OPEN SACK", ending.title());
        assertEquals(6, ending.beats().size());
    }

    private static BirdGame3 prepared(int round, long routeSeed, long matchSeed) {
        BirdGame3 game = new BirdGame3();
        game.harnessPrepareClassicEncounter(BirdType.GRINCHHAWK, round, 5.0, 6, routeSeed, matchSeed);
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
        return (List<ClassicEncounter>) invoke(game, "buildGrinchHawkClassicRun", new Class<?>[0]);
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
