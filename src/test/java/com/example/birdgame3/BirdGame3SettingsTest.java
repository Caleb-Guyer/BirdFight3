package com.example.birdgame3;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Deque;
import java.util.UUID;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BirdGame3SettingsTest {
    private Preferences prefs;

    @BeforeEach
    void setUp() {
        prefs = Preferences.userRoot().node("/birdfight3-tests/" + UUID.randomUUID());
    }

    @AfterEach
    void tearDown() throws BackingStoreException {
        prefs.removeNode();
        prefs.flush();
    }

    @Test
    void persistAchievementsStoresAudioChannelVolumes() throws Exception {
        BirdGame3 game = new BirdGame3();

        invokeVolumeSetter(game, "setMusicVolume", 0.42);
        invokeVolumeSetter(game, "setSfxVolume", 0.15);
        game.persistAchievements(prefs);

        assertEquals(0.42, prefs.getDouble("setting_music_volume", -1.0), 0.0001);
        assertEquals(0.15, prefs.getDouble("setting_sfx_volume", -1.0), 0.0001);
        assertTrue(prefs.getBoolean("setting_music", false));
        assertTrue(prefs.getBoolean("setting_sfx", false));

        invokeVolumeSetter(game, "setSfxVolume", 0.0);
        game.persistAchievements(prefs);

        assertEquals(0.0, prefs.getDouble("setting_sfx_volume", -1.0), 0.0001);
        assertFalse(prefs.getBoolean("setting_sfx", true));
    }

    @Test
    void enqueueAchievementToastUsesAchievementDescription() throws Exception {
        BirdGame3 game = new BirdGame3();

        Method enqueue = BirdGame3.class.getDeclaredMethod("enqueueAchievementToast", int.class, String.class);
        enqueue.setAccessible(true);
        enqueue.invoke(game, 0, "FIRST BLOOD!");

        Field queueField = BirdGame3.class.getDeclaredField("achievementToastQueue");
        queueField.setAccessible(true);
        Deque<?> queue = (Deque<?>) queueField.get(game);
        assertEquals(1, queue.size());

        Object payload = queue.peekFirst();
        Method title = payload.getClass().getDeclaredMethod("title");
        Method description = payload.getClass().getDeclaredMethod("description");
        Method rewardText = payload.getClass().getDeclaredMethod("rewardText");
        title.setAccessible(true);
        description.setAccessible(true);
        rewardText.setAccessible(true);

        assertEquals("FIRST BLOOD!", title.invoke(payload));
        assertEquals(BirdGame3.ACHIEVEMENT_DESCRIPTIONS[0], description.invoke(payload));
        assertEquals("150 Bird Coins", rewardText.invoke(payload));
    }

    @Test
    void claimAchievementRewardFallsBackToBirdCoinsWhenCosmeticIsOwned() throws Exception {
        BirdGame3 game = new BirdGame3();
        game.achievementsUnlocked[10] = true;
        game.cityPigeonUnlocked = true;

        Method claim = BirdGame3.class.getDeclaredMethod("claimAchievementRewardInternal", int.class);
        claim.setAccessible(true);
        Object result = claim.invoke(game, 10);

        assertTrue(game.achievementRewardsClaimed[10]);
        assertEquals(220, currentBirdCoinBalance(game));

        Method usesUnlockCards = result.getClass().getDeclaredMethod("usesUnlockCards");
        Method detail = result.getClass().getDeclaredMethod("detail");
        usesUnlockCards.setAccessible(true);
        detail.setAccessible(true);

        assertFalse((boolean) usesUnlockCards.invoke(result));
        assertTrue(((String) detail.invoke(result)).contains("already unlocked"));
    }

    @Test
    void applyWinnerMapProgressUnlocksUrbanKingAcrossProfileTotalWins() throws Exception {
        BirdGame3 game = new BirdGame3();
        game.selectedMap = BirdGame3.MapType.CITY;

        Bird winnerOne = new Bird(100.0, BirdGame3.BirdType.PIGEON, 0, game);
        Bird winnerTwo = new Bird(100.0, BirdGame3.BirdType.EAGLE, 1, game);

        Method applyWinnerMapProgress = BirdGame3.class.getDeclaredMethod("applyWinnerMapProgress", Bird.class);
        applyWinnerMapProgress.setAccessible(true);

        applyWinnerMapProgress.invoke(game, winnerOne);
        applyWinnerMapProgress.invoke(game, winnerTwo);
        applyWinnerMapProgress.invoke(game, winnerOne);
        applyWinnerMapProgress.invoke(game, winnerTwo);
        applyWinnerMapProgress.invoke(game, winnerOne);

        assertEquals(5, game.achievementProgress[12]);
        assertTrue(game.achievementsUnlocked[12]);
    }

    private static void invokeVolumeSetter(BirdGame3 game, String methodName, double value) throws Exception {
        Method method = BirdGame3.class.getDeclaredMethod(methodName, double.class);
        method.setAccessible(true);
        method.invoke(game, value);
    }

    private static int currentBirdCoinBalance(BirdGame3 game) throws Exception {
        Field field = BirdGame3.class.getDeclaredField("birdCoinLedger");
        field.setAccessible(true);
        BirdCoinLedger ledger = (BirdCoinLedger) field.get(game);
        return ledger.balance();
    }
}
