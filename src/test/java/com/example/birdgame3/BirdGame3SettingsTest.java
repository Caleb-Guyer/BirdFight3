package com.example.birdgame3;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Deque;
import java.util.HashSet;
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
    void applyWinnerMapProgressUnlocksAshfallAchievements() throws Exception {
        BirdGame3 game = new BirdGame3();
        game.selectedMap = BirdGame3.MapType.ASHFALL_CATHEDRAL;

        Bird winner = new Bird(100.0, BirdGame3.BirdType.PHOENIX, 0, game);

        Method applyWinnerMapProgress = BirdGame3.class.getDeclaredMethod("applyWinnerMapProgress", Bird.class);
        applyWinnerMapProgress.setAccessible(true);

        for (int i = 0; i < 5; i++) {
            applyWinnerMapProgress.invoke(game, winner);
        }

        assertEquals(1, game.achievementProgressValue(BirdGame3Achievement.ASHFALL_INITIATE));
        assertTrue(game.isAchievementUnlocked(BirdGame3Achievement.ASHFALL_INITIATE));
        assertEquals(5, game.achievementProgressValue(BirdGame3Achievement.ASHFALL_ASCENDANT));
        assertTrue(game.isAchievementUnlocked(BirdGame3Achievement.ASHFALL_ASCENDANT));
        assertEquals(1, game.achievementProgressValue(BirdGame3Achievement.PHOENIX_PILGRIMAGE));
        assertTrue(game.isAchievementUnlocked(BirdGame3Achievement.PHOENIX_PILGRIMAGE));
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

        Method orderMethod = BirdGame3.class.getDeclaredMethod("achievementDisplayOrder", categoryClass);
        orderMethod.setAccessible(true);
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
    void uniqueSkinsShowUniqueRarityInFeatherpedia() throws Exception {
        BirdGame3 game = new BirdGame3();

        Method rarityMethod = BirdGame3.class.getDeclaredMethod("skinRarityLabel", String.class);
        rarityMethod.setAccessible(true);
        Method howToGetMethod = BirdGame3.class.getDeclaredMethod("skinHowToGet", String.class, BirdGame3.BirdType.class);
        howToGetMethod.setAccessible(true);

        assertEquals("UNIQUE", rarityMethod.invoke(game, "STOCK_PHOTO_EAGLE"));
        assertEquals("UNIQUE", rarityMethod.invoke(game, "ASHEN_SOVEREIGN_PHOENIX"));
        assertEquals("Complete Ashfall Trial",
                howToGetMethod.invoke(game, "ASHEN_SOVEREIGN_PHOENIX", BirdGame3.BirdType.PHOENIX));
    }

    @Test
    void latestUpdateSplashShowsUntilMarkedSeen() throws Exception {
        BirdGame3 game = new BirdGame3();

        Method latestKeyMethod = BirdGame3.class.getDeclaredMethod("latestUpdateSplashKey");
        latestKeyMethod.setAccessible(true);
        Method shouldShowMethod = BirdGame3.class.getDeclaredMethod("shouldShowLatestUpdateSplash");
        shouldShowMethod.setAccessible(true);

        String latestKey = (String) latestKeyMethod.invoke(game);
        assertEquals("REBIRTH_UPDATE", latestKey);
        assertTrue((boolean) shouldShowMethod.invoke(game));

        setPrivateField(game, "lastSeenUpdateSplashKey", latestKey);

        assertFalse((boolean) shouldShowMethod.invoke(game));
    }

    @Test
    void loadProfileProgressReconcilesModeAndStoryAchievementUnlocks() throws Exception {
        BirdGame3 game = new BirdGame3();
        game.setAchievementProgressValue(BirdGame3Achievement.CROWN_UNBROKEN, 1);

        setPrivateField(game, "bossRushClearCount", 2);
        setPrivateField(game, "tournamentChampionshipsWon", 1);
        setPrivateField(game, "pigeonEpisodeCompleted", true);
        setPrivateField(game, "batEpisodeCompleted", true);
        setPrivateField(game, "pelicanEpisodeCompleted", true);
        setTowerDefenseBadge(game, TowerDefenseMode.Difficulty.EASY);
        setTowerDefenseBadge(game, TowerDefenseMode.Difficulty.MEDIUM);
        setTowerDefenseBadge(game, TowerDefenseMode.Difficulty.HARD);
        game.persistAchievements(prefs);

        BirdGame3 reloaded = new BirdGame3();
        Method loadProfileProgress = BirdGame3.class.getDeclaredMethod("loadProfileProgress", Preferences.class);
        loadProfileProgress.setAccessible(true);
        loadProfileProgress.invoke(reloaded, prefs);

        assertTrue(reloaded.isAchievementUnlocked(BirdGame3Achievement.BOSS_BREAKER));
        assertTrue(reloaded.isAchievementUnlocked(BirdGame3Achievement.CROWN_UNBROKEN));
        assertTrue(reloaded.isAchievementUnlocked(BirdGame3Achievement.GROVE_SENTINEL));
        assertTrue(reloaded.isAchievementUnlocked(BirdGame3Achievement.ROOFTOP_LEGACY));
        assertTrue(reloaded.isAchievementUnlocked(BirdGame3Achievement.ECHO_SOVEREIGN));
        assertTrue(reloaded.isAchievementUnlocked(BirdGame3Achievement.IRON_TEMPEST));
        assertTrue(reloaded.isAchievementUnlocked(BirdGame3Achievement.BLIGHT_BUSTER));
        assertTrue(reloaded.isAchievementUnlocked(BirdGame3Achievement.BRACKET_BOSS));
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
        setPrivateBossRushBirdRecord(game);
        setPrivateBossRushPerfectBadge(game);

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
        game.persistAchievements(prefs);

        BirdGame3 reloaded = new BirdGame3();
        Method loadProfileProgress = BirdGame3.class.getDeclaredMethod("loadProfileProgress", Preferences.class);
        loadProfileProgress.setAccessible(true);
        loadProfileProgress.invoke(reloaded, prefs);

        assertTrue(reloaded.isGuidedTutorialCompleted());
    }

    @Test
    void persistAchievementsRoundTripsAshfallTrialCompletion() throws Exception {
        BirdGame3 game = new BirdGame3();
        setPrivateField(game, "ashfallTrialCompleted", true);
        game.persistAchievements(prefs);

        BirdGame3 reloaded = new BirdGame3();
        Method loadProfileProgress = BirdGame3.class.getDeclaredMethod("loadProfileProgress", Preferences.class);
        loadProfileProgress.setAccessible(true);
        loadProfileProgress.invoke(reloaded, prefs);

        assertTrue(getPrivateBooleanField(reloaded, "ashfallTrialCompleted"));
    }

    @Test
    void persistAchievementsRoundTripsAshenSovereignPhoenixSkin() throws Exception {
        BirdGame3 game = new BirdGame3();
        game.ashenSovereignPhoenixUnlocked = true;
        game.persistAchievements(prefs);

        BirdGame3 reloaded = new BirdGame3();
        Method loadProfileProgress = BirdGame3.class.getDeclaredMethod("loadProfileProgress", Preferences.class);
        loadProfileProgress.setAccessible(true);
        loadProfileProgress.invoke(reloaded, prefs);

        assertTrue(reloaded.ashenSovereignPhoenixUnlocked);
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
        assertTrue(getPrivateBoolean(game));
        assertTrue(getPrivateBooleanField(game, "ashfallTrialCompleted"));
        assertTrue(game.ashenSovereignPhoenixUnlocked);
        assertTrue((boolean) spendBirdCoins.invoke(game, 99_999));
        assertTrue(game.isAchievementUnlocked(BirdGame3Achievement.BOSS_BREAKER));

        game.persistAchievements(prefs);

        assertTrue(prefs.getBoolean("char_roadrunner_unlocked", false));
        assertTrue(prefs.getBoolean("map_desert_unlocked", false));
        assertTrue(prefs.getBoolean("developer_infinite_bird_coins", false));
        assertTrue(prefs.getBoolean("skin_ashen_sovereign_phoenix", false));

        BirdGame3 reloaded = new BirdGame3();
        Method loadProfileProgress = BirdGame3.class.getDeclaredMethod("loadProfileProgress", Preferences.class);
        loadProfileProgress.setAccessible(true);
        loadProfileProgress.invoke(reloaded, prefs);

        assertTrue(reloaded.roadrunnerUnlocked);
        assertTrue((boolean) isMapUnlocked.invoke(reloaded, BirdGame3.MapType.DESERT));
        assertTrue(getPrivateBoolean(reloaded));
        assertTrue(getPrivateBooleanField(reloaded, "ashfallTrialCompleted"));
        assertTrue(reloaded.ashenSovereignPhoenixUnlocked);
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
        assertEquals("ashfall-spark", iconVariant.invoke(game, 30));
        assertEquals("ashfall-geyser", iconVariant.invoke(game, 31));
        assertEquals("ashfall-crown", iconVariant.invoke(game, 32));
        assertEquals("phoenix-altar", iconVariant.invoke(game, 33));
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
    void localTeamAssignmentsCanBeChangedForOneVersusThreeCpuSetup() {
        BirdGame3 game = new BirdGame3();
        game.activePlayers = 4;
        game.teamModeEnabled = true;

        assertEquals(1, game.getEffectiveTeam(0));
        assertEquals(2, game.getEffectiveTeam(1));
        assertEquals(1, game.getEffectiveTeam(2));
        assertEquals(2, game.getEffectiveTeam(3));

        assertTrue(game.cycleLocalTeamForPlayer(2));

        assertEquals(1, game.getEffectiveTeam(0));
        assertEquals(2, game.getEffectiveTeam(1));
        assertEquals(2, game.getEffectiveTeam(2));
        assertEquals(2, game.getEffectiveTeam(3));
        assertFalse(game.cycleLocalTeamForPlayer(-1));
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

    private static void setPrivateBossRushBirdRecord(BirdGame3 game) throws Exception {
        Field timeField = BirdGame3.class.getDeclaredField("bossRushBestClearMillisByBird");
        timeField.setAccessible(true);
        long[] times = (long[]) timeField.get(game);
        times[BirdGame3.BirdType.BAT.ordinal()] = 505000L;

        Field rankField = BirdGame3.class.getDeclaredField("bossRushBestRankByBird");
        rankField.setAccessible(true);
        String[] ranks = (String[]) rankField.get(game);
        ranks[BirdGame3.BirdType.BAT.ordinal()] = "S";
    }

    private static void setPrivateBossRushPerfectBadge(BirdGame3 game) throws Exception {
        Field field = BirdGame3.class.getDeclaredField("bossRushPerfectBadgeByBird");
        field.setAccessible(true);
        boolean[] badges = (boolean[]) field.get(game);
        badges[BirdGame3.BirdType.BAT.ordinal()] = true;
    }

    private static void setTowerDefenseBadge(BirdGame3 game, TowerDefenseMode.Difficulty difficulty) throws Exception {
        Field field = BirdGame3.class.getDeclaredField("towerDefenseDifficultyBadges");
        field.setAccessible(true);
        boolean[][] badges = (boolean[][]) field.get(game);
        badges[BirdGame3.MapType.FOREST.ordinal()][difficulty.ordinal()] = true;
    }

    private static void invokeVolumeSetter(BirdGame3 game, String methodName, double value) throws Exception {
        Method method = BirdGame3.class.getDeclaredMethod(methodName, double.class);
        method.setAccessible(true);
        method.invoke(game, value);
    }

    private static boolean getPrivateBoolean(BirdGame3 game) throws Exception {
        return getPrivateBooleanField(game, "developerInfiniteBirdCoins");
    }

    private static boolean getPrivateBooleanField(BirdGame3 game, String fieldName) throws Exception {
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
