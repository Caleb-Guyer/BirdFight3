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
