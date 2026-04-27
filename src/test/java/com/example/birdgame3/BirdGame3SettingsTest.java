package com.example.birdgame3;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
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
        assertEquals(WiimoteControlMode.OFF.name(), prefs.get("setting_wiimote_mode_p1", ""));

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
        Assertions.assertNotNull(payload);
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
        game.setAchievementUnlocked(BirdGame3Achievement.ROOFTOP_RUNNER.legacyIndex);
        game.cityPigeonUnlocked = true;

        Method claim = BirdGame3.class.getDeclaredMethod("claimAchievementRewardInternal", int.class);
        claim.setAccessible(true);
        Object result = claim.invoke(game, 10);

        assertTrue(game.isAchievementRewardClaimed(BirdGame3Achievement.ROOFTOP_RUNNER.legacyIndex));
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

        assertEquals(5, game.achievementProgressValue(BirdGame3Achievement.URBAN_KING));
        assertTrue(game.isAchievementUnlocked(BirdGame3Achievement.URBAN_KING));
    }

    @Test
    void achievementProgressTextShowsCompletedAfterUnlock() throws Exception {
        BirdGame3 game = new BirdGame3();
        game.setAchievementUnlocked(BirdGame3Achievement.ECHOES_BELOW.legacyIndex);

        Method progressText = BirdGame3.class.getDeclaredMethod("achievementProgressText", int.class);
        progressText.setAccessible(true);

        assertEquals("Completed", progressText.invoke(game, 20));
    }

    @Test
    void achievementDisplayOrderMovesCompletedEntriesToBottom() throws Exception {
        BirdGame3 game = new BirdGame3();
        game.setAchievementUnlocked(BirdGame3Achievement.URBAN_KING.legacyIndex);

        Class<?> categoryClass = Class.forName("com.example.birdgame3.BirdGame3AchievementCategory");
        @SuppressWarnings({"unchecked", "rawtypes"})
        Object mapCategory = Enum.valueOf((Class<? extends Enum>) categoryClass.asSubclass(Enum.class), "MAP");

        Method orderMethod = BirdGame3.class.getDeclaredMethod("achievementDisplayOrder", categoryClass);
        orderMethod.setAccessible(true);

        @SuppressWarnings("unchecked")
        List<Integer> order = (List<Integer>) orderMethod.invoke(game, mapCategory);

        assertEquals(12, order.getLast());
    }

    @Test
    void achievementRewardDetailOmitsDuplicateCoinFallbackText() throws Exception {
        BirdGame3 game = new BirdGame3();

        Method rewardDetail = BirdGame3.class.getDeclaredMethod("achievementRewardDetail", int.class);
        rewardDetail.setAccessible(true);

        String detail = (String) rewardDetail.invoke(game, 10);
        assertEquals("Claim to unlock City Pigeon Skin.", detail);
    }

    @Test
    void ownedAchievementRewardPreviewShowsOwnedOverlayState() throws Exception {
        BirdGame3 game = new BirdGame3();
        game.cityPigeonUnlocked = true;

        Method overlayState = BirdGame3.class.getDeclaredMethod("shouldShowAchievementRewardOwnedOverlay", int.class);
        overlayState.setAccessible(true);

        assertTrue((boolean) overlayState.invoke(game, 10));
        assertFalse((boolean) overlayState.invoke(game, 0));
    }

    @Test
    void stockPhotoEagleSkinShowsLegendaryRarityInFeatherpedia() throws Exception {
        BirdGame3 game = new BirdGame3();

        Method rarityMethod = BirdGame3.class.getDeclaredMethod("skinRarityLabel", String.class);
        rarityMethod.setAccessible(true);

        assertEquals("LEGENDARY", rarityMethod.invoke(game, "STOCK_PHOTO_EAGLE"));
    }

    @Test
    void reconcileAchievementUnlocksRestoresModeAndStoryMilestones() throws Exception {
        BirdGame3 game = new BirdGame3();
        game.setAchievementProgressValue(BirdGame3Achievement.CROWN_UNBROKEN, 1);

        setPrivateField(game, "bossRushClearCount", 2);
        setPrivateField(game, "tournamentChampionshipsWon", 1);
        setPrivateField(game, "pigeonEpisodeCompleted", true);
        setPrivateField(game, "batEpisodeCompleted", true);
        setPrivateField(game, "pelicanEpisodeCompleted", true);
        setTowerDefenseBadge(game, BirdGame3.MapType.FOREST, TowerDefenseMode.Difficulty.EASY, true);
        setTowerDefenseBadge(game, BirdGame3.MapType.FOREST, TowerDefenseMode.Difficulty.MEDIUM, true);
        setTowerDefenseBadge(game, BirdGame3.MapType.FOREST, TowerDefenseMode.Difficulty.HARD, true);

        Method ensureAdventureState = BirdGame3.class.getDeclaredMethod("ensureAdventureChapterState");
        ensureAdventureState.setAccessible(true);
        ensureAdventureState.invoke(game);

        Method reconcile = BirdGame3.class.getDeclaredMethod("reconcileAchievementUnlocksFromStoredProgress");
        reconcile.setAccessible(true);
        reconcile.invoke(game);

        assertTrue(game.isAchievementUnlocked(BirdGame3Achievement.BOSS_BREAKER));
        assertTrue(game.isAchievementUnlocked(BirdGame3Achievement.CROWN_UNBROKEN));
        assertTrue(game.isAchievementUnlocked(BirdGame3Achievement.GROVE_SENTINEL));
        assertTrue(game.isAchievementUnlocked(BirdGame3Achievement.ROOFTOP_LEGACY));
        assertTrue(game.isAchievementUnlocked(BirdGame3Achievement.ECHO_SOVEREIGN));
        assertTrue(game.isAchievementUnlocked(BirdGame3Achievement.IRON_TEMPEST));
        assertTrue(game.isAchievementUnlocked(BirdGame3Achievement.BLIGHT_BUSTER));
        assertTrue(game.isAchievementUnlocked(BirdGame3Achievement.BRACKET_BOSS));
    }

    @Test
    void loadProfileProgressMigratesRetiredDailyAchievementSlotToTowerDefense() throws Exception {
        prefs.putBoolean("ach_24", true);
        prefs.putInt("prog_24", 1);
        prefs.putBoolean("ach_reward_claimed_24", true);
        prefs.putBoolean("td_badge_FOREST_EASY", true);

        BirdGame3 reloaded = new BirdGame3();
        Method loadProfileProgress = BirdGame3.class.getDeclaredMethod("loadProfileProgress", Preferences.class);
        loadProfileProgress.setAccessible(true);
        loadProfileProgress.invoke(reloaded, prefs);

        assertTrue(reloaded.isAchievementUnlocked(BirdGame3Achievement.GROVE_SENTINEL));
        assertEquals(1, reloaded.achievementProgressValue(BirdGame3Achievement.GROVE_SENTINEL));
        assertFalse(reloaded.isAchievementRewardClaimed(BirdGame3Achievement.GROVE_SENTINEL.legacyIndex));
    }

    @Test
    void persistAchievementsRoundTripsBossRushPerBirdRecords() throws Exception {
        BirdGame3 game = new BirdGame3();
        setPrivateBossRushBirdRecord(game, BirdGame3.BirdType.BAT, 505_000L, "S");
        setPrivateBossRushPerfectBadge(game, BirdGame3.BirdType.BAT, true);

        Method refreshOverall = BirdGame3.class.getDeclaredMethod("refreshBossRushOverallBestRecord");
        refreshOverall.setAccessible(true);
        refreshOverall.invoke(game);
        game.persistAchievements(prefs);

        BirdGame3 reloaded = new BirdGame3();
        Method loadProfileProgress = BirdGame3.class.getDeclaredMethod("loadProfileProgress", Preferences.class);
        loadProfileProgress.setAccessible(true);
        loadProfileProgress.invoke(reloaded, prefs);

        assertEquals("S  |  8:25.00", reloaded.bossRushBestStatusForBird(BirdGame3.BirdType.BAT));
        assertTrue(reloaded.shouldShowBossRushSelectCompletionBadge(BirdGame3.BirdType.BAT));
        assertTrue(reloaded.shouldShowBossRushSelectPerfectBadge(BirdGame3.BirdType.BAT));
        assertEquals("S  |  8:25.00  |  Bat", reloaded.bossRushOverallBestStatus());
    }

    @Test
    void persistAchievementsRoundTripsTrainingAcademyProgress() throws Exception {
        BirdGame3 game = new BirdGame3();
        setPrivateField(game, "guidedTutorialCompleted", true);
        setBirdTrialCompleted(game, BirdGame3.BirdType.PIGEON, true);
        setBirdTrialCompleted(game, BirdGame3.BirdType.EAGLE, true);
        game.persistAchievements(prefs);

        BirdGame3 reloaded = new BirdGame3();
        Method loadProfileProgress = BirdGame3.class.getDeclaredMethod("loadProfileProgress", Preferences.class);
        loadProfileProgress.setAccessible(true);
        loadProfileProgress.invoke(reloaded, prefs);

        assertTrue(reloaded.isGuidedTutorialCompleted());
        assertTrue(reloaded.isBirdTrialCompleted(BirdGame3.BirdType.PIGEON));
        assertTrue(reloaded.isBirdTrialCompleted(BirdGame3.BirdType.EAGLE));
        assertFalse(reloaded.isBirdTrialCompleted(BirdGame3.BirdType.PENGUIN));
    }

    @Test
    void developerSettingsCodeUnlocksEverythingAndRoundTripsProfileState() throws Exception {
        BirdGame3 game = new BirdGame3();

        Method applyCode = BirdGame3.class.getDeclaredMethod("tryApplySettingsCode", String.class);
        applyCode.setAccessible(true);
        Method isMapUnlocked = BirdGame3.class.getDeclaredMethod("isMapUnlocked", BirdGame3.MapType.class);
        isMapUnlocked.setAccessible(true);
        Method spendBirdCoins = BirdGame3.class.getDeclaredMethod("spendBirdCoins", int.class);
        spendBirdCoins.setAccessible(true);

        assertTrue((boolean) applyCode.invoke(game, " feather-dev "));
        assertTrue(game.roadrunnerUnlocked);
        assertTrue((boolean) isMapUnlocked.invoke(game, BirdGame3.MapType.DESERT));
        assertTrue(getPrivateBoolean(game, "developerInfiniteBirdCoins"));
        assertTrue((boolean) spendBirdCoins.invoke(game, 99_999));
        assertTrue(game.isAchievementUnlocked(BirdGame3Achievement.BOSS_BREAKER));

        game.persistAchievements(prefs);

        assertTrue(prefs.getBoolean("char_roadrunner_unlocked", false));
        assertTrue(prefs.getBoolean("map_desert_unlocked", false));
        assertTrue(prefs.getBoolean("developer_infinite_bird_coins", false));

        BirdGame3 reloaded = new BirdGame3();
        Method loadProfileProgress = BirdGame3.class.getDeclaredMethod("loadProfileProgress", Preferences.class);
        loadProfileProgress.setAccessible(true);
        loadProfileProgress.invoke(reloaded, prefs);

        assertTrue(reloaded.roadrunnerUnlocked);
        assertTrue((boolean) isMapUnlocked.invoke(reloaded, BirdGame3.MapType.DESERT));
        assertTrue(getPrivateBoolean(reloaded, "developerInfiniteBirdCoins"));
        assertTrue((boolean) spendBirdCoins.invoke(reloaded, Integer.MAX_VALUE));
    }

    @Test
    void achievementIconVariantsAreUniquePerAchievement() throws Exception {
        BirdGame3 game = new BirdGame3();

        Method iconVariant = BirdGame3.class.getDeclaredMethod("achievementIconVariantKey", int.class);
        iconVariant.setAccessible(true);

        Set<String> variants = new HashSet<>();
        for (int i = 0; i < BirdGame3.ACHIEVEMENT_COUNT; i++) {
            variants.add((String) iconVariant.invoke(game, i));
        }

        assertEquals(BirdGame3.ACHIEVEMENT_COUNT, variants.size());
        assertEquals("rooftop-arc", iconVariant.invoke(game, 10));
        assertEquals("story-book", iconVariant.invoke(game, 21));
        assertEquals("iron-wing", iconVariant.invoke(game, 27));
        assertEquals("bracket-crown", iconVariant.invoke(game, 29));
    }

    @Test
    void globalSettingsRoundTripWiimoteModes() throws Exception {
        BirdGame3 game = new BirdGame3();
        game.setWiimoteModeForPlayer(0, WiimoteControlMode.SIDEWAYS);
        game.setWiimoteModeForPlayer(1, WiimoteControlMode.NUNCHUK);
        game.persistAchievements(prefs);

        BirdGame3 reloaded = new BirdGame3();
        Method loadGlobalSettings = BirdGame3.class.getDeclaredMethod("loadGlobalSettings", Preferences.class);
        loadGlobalSettings.setAccessible(true);
        loadGlobalSettings.invoke(reloaded, prefs);

        assertEquals(WiimoteControlMode.SIDEWAYS, reloaded.wiimoteModeForPlayer(0));
        assertEquals(WiimoteControlMode.NUNCHUK, reloaded.wiimoteModeForPlayer(1));
        assertEquals(WiimoteControlMode.OFF, reloaded.wiimoteModeForPlayer(2));
    }

    @Test
    void computeUiFitScaleShrinksOversizedLayoutsUsingMeasuredBounds() throws Exception {
        BirdGame3 game = new BirdGame3();

        Method computeScale = BirdGame3.class.getDeclaredMethod(
                "computeUiFitScale",
                double.class,
                double.class,
                double.class,
                double.class
        );
        computeScale.setAccessible(true);

        double scale = (double) computeScale.invoke(game, 2200.0, 1500.0, 1280.0, 720.0);

        assertTrue(scale < 0.5);
    }

    @Test
    void computeUiFitScaleLeavesComfortablySizedLayoutsUnscaled() throws Exception {
        BirdGame3 game = new BirdGame3();

        Method computeScale = BirdGame3.class.getDeclaredMethod(
                "computeUiFitScale",
                double.class,
                double.class,
                double.class,
                double.class
        );
        computeScale.setAccessible(true);

        double scale = (double) computeScale.invoke(game, 900.0, 520.0, 1366.0, 768.0);

        assertEquals(1.0, scale, 0.0001);
    }

    private static void setPrivateField(BirdGame3 game, String fieldName, Object value) throws Exception {
        Field field = BirdGame3.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(game, value);
    }

    private static void setPrivateBossRushBirdRecord(BirdGame3 game, BirdGame3.BirdType type, long elapsedMillis, String rank) throws Exception {
        Field timeField = BirdGame3.class.getDeclaredField("bossRushBestClearMillisByBird");
        timeField.setAccessible(true);
        long[] times = (long[]) timeField.get(game);
        times[type.ordinal()] = elapsedMillis;

        Field rankField = BirdGame3.class.getDeclaredField("bossRushBestRankByBird");
        rankField.setAccessible(true);
        String[] ranks = (String[]) rankField.get(game);
        ranks[type.ordinal()] = rank;
    }

    private static void setPrivateBossRushPerfectBadge(BirdGame3 game, BirdGame3.BirdType type, boolean value) throws Exception {
        Field field = BirdGame3.class.getDeclaredField("bossRushPerfectBadgeByBird");
        field.setAccessible(true);
        boolean[] badges = (boolean[]) field.get(game);
        badges[type.ordinal()] = value;
    }

    private static void setBirdTrialCompleted(BirdGame3 game, BirdGame3.BirdType type, boolean value) throws Exception {
        Field field = BirdGame3.class.getDeclaredField("birdTrialCompleted");
        field.setAccessible(true);
        boolean[] trials = (boolean[]) field.get(game);
        trials[type.ordinal()] = value;
    }

    private static void setTowerDefenseBadge(BirdGame3 game, BirdGame3.MapType map, TowerDefenseMode.Difficulty difficulty, boolean value) throws Exception {
        Field field = BirdGame3.class.getDeclaredField("towerDefenseDifficultyBadges");
        field.setAccessible(true);
        boolean[][] badges = (boolean[][]) field.get(game);
        badges[map.ordinal()][difficulty.ordinal()] = value;
    }

    private static void invokeVolumeSetter(BirdGame3 game, String methodName, double value) throws Exception {
        Method method = BirdGame3.class.getDeclaredMethod(methodName, double.class);
        method.setAccessible(true);
        method.invoke(game, value);
    }

    private static boolean getPrivateBoolean(BirdGame3 game, String fieldName) throws Exception {
        Field field = BirdGame3.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        return field.getBoolean(game);
    }

    private static int currentBirdCoinBalance(BirdGame3 game) throws Exception {
        Field field = BirdGame3.class.getDeclaredField("birdCoinLedger");
        field.setAccessible(true);
        BirdCoinLedger ledger = (BirdCoinLedger) field.get(game);
        return ledger.balance();
    }
}
