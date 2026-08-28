package com.example.birdgame3;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BirdGame3ProfileProgressControllerTest {
    private final BirdGame3ProgressionService progressionService = new BirdGame3ProgressionService();

    @Test
    void classicRunCompletionUnlocksRewardAndTitmouse() {
        BirdGame3 game = new BirdGame3();
        BirdGame3ProfileProgressController controller = new BirdGame3ProfileProgressController(game, progressionService);

        controller.onClassicRunCompleted(BirdGame3.BirdType.HUMMINGBIRD, () -> {});

        assertTrue(game.isClassicCompleted(BirdGame3.BirdType.HUMMINGBIRD));
        assertTrue(game.isClassicRewardUnlocked(BirdGame3.BirdType.HUMMINGBIRD));
        assertTrue(game.isTitmouseUnlocked());
    }

    @Test
    void episodeCompletionMarksProgressOnlyOnce() {
        BirdGame3 game = new BirdGame3();
        BirdGame3ProfileProgressController controller = new BirdGame3ProfileProgressController(game, progressionService);
        int[] callbacks = new int[1];

        boolean firstCompletion = controller.onEpisodeCompleted(BirdGame3.BirdType.PIGEON, () -> callbacks[0]++);
        boolean replayCompletion = controller.onEpisodeCompleted(BirdGame3.BirdType.PIGEON, () -> callbacks[0]++);

        assertTrue(firstCompletion);
        assertFalse(replayCompletion);
        assertTrue(game.isEpisodeCompletedForBird(BirdGame3.BirdType.PIGEON));
        assertEquals(1, callbacks[0]);
    }

    @Test
    void loadedProgressReconciliationRestoresEpisodeAndFinaleRewards() throws Exception {
        BirdGame3 game = new BirdGame3();
        BirdGame3ProfileProgressController controller = new BirdGame3ProfileProgressController(game, progressionService);

        game.setEpisodeCompletedForBird(BirdGame3.BirdType.PIGEON);
        game.setEpisodeUnlockedChaptersForBird(BirdGame3.BirdType.PIGEON, 1);
        game.setClassicCompleted(BirdGame3.BirdType.HUMMINGBIRD);

        controller.reconcileLoadedProgress(true, true);

        assertEquals(
                game.episodeChapterCountForBird(BirdGame3.BirdType.PIGEON),
                getPrivateInt(game, "pigeonEpisodeUnlockedChapters")
        );
        assertTrue(game.isNoirPigeonUnlocked());
        assertTrue(game.isTitmouseUnlocked());
        assertTrue(game.isClassicRewardUnlocked(BirdGame3.BirdType.EAGLE));
        assertTrue(getPrivateBoolean(game, "beaconCrownMapUnlocked"));
        assertTrue(getPrivateBoolean(game, "dockMapUnlocked"));
        assertTrue(getPrivateBoolean(game, "nullRockVultureUnlocked"));
        assertTrue(getPrivateBoolean(game, "ironcladPelicanUnlocked"));
    }

    @Test
    void replayAndHeadlessCompletionsCannotWriteProfileProgress() throws Exception {
        BirdGame3 replay = new BirdGame3();
        replay.replayPlaybackActive = true;
        BirdGame3ProfileProgressController replayController =
                new BirdGame3ProfileProgressController(replay, progressionService);
        int[] replayCallbacks = new int[1];

        replayController.onClassicRunCompleted(BirdGame3.BirdType.HUMMINGBIRD, () -> replayCallbacks[0]++);
        assertFalse(replayController.onEpisodeCompleted(BirdGame3.BirdType.PIGEON, () -> replayCallbacks[0]++));
        replayController.onTournamentChampionshipWon(() -> replayCallbacks[0]++);
        assertEquals(0, replayController.onBossRushCompleted(
                BirdGame3.BirdType.PIGEON, "S", 1_000L, true, ignored -> replayCallbacks[0]++));

        assertFalse(replay.isClassicCompleted(BirdGame3.BirdType.HUMMINGBIRD));
        assertFalse(replay.isClassicRewardUnlocked(BirdGame3.BirdType.HUMMINGBIRD));
        assertFalse(replay.isTitmouseUnlocked());
        assertFalse(replay.isEpisodeCompletedForBird(BirdGame3.BirdType.PIGEON));
        assertEquals(0, getPrivateInt(replay, "tournamentChampionshipsWon"));
        assertEquals(0, replayCallbacks[0]);

        BirdGame3 headless = new BirdGame3();
        headless.headlessHarnessMode = true;
        BirdGame3ProfileProgressController headlessController =
                new BirdGame3ProfileProgressController(headless, progressionService);
        headlessController.onClassicRunCompleted(BirdGame3.BirdType.HUMMINGBIRD, null);
        assertFalse(headless.isClassicCompleted(BirdGame3.BirdType.HUMMINGBIRD));
        assertFalse(headless.isTitmouseUnlocked());
    }

    private boolean getPrivateBoolean(Object target, String fieldName) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        return field.getBoolean(target);
    }

    private int getPrivateInt(Object target, String fieldName) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        return field.getInt(target);
    }
}
