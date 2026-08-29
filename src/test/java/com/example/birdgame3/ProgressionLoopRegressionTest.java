package com.example.birdgame3;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.prefs.Preferences;

import static org.junit.jupiter.api.Assertions.*;

class ProgressionLoopRegressionTest {
    private final Preferences root = Preferences.userRoot().node(
            "/birdfight3-tests/progression-loop/" + UUID.randomUUID());

    @AfterEach
    void removeProfile() throws Exception {
        root.removeNode();
        root.flush();
    }

    @Test
    void claimAllPaysEachRewardOnceAndSurvivesRestart() throws Exception {
        BirdGame3 game = loadedGame();
        game.setAchievementUnlocked(BirdGame3Achievement.FIRST_BLOOD, true);
        game.setAchievementUnlocked(BirdGame3Achievement.DOMINATOR, true);

        invoke(game, "claimAllAchievementRewardsInternal", new Class<?>[0]);
        assertEquals(400, ledger(game).balance());
        assertTrue(game.isAchievementRewardClaimed(BirdGame3Achievement.FIRST_BLOOD.legacyIndex));
        assertTrue(game.isAchievementRewardClaimed(BirdGame3Achievement.DOMINATOR.legacyIndex));

        invoke(game, "claimAllAchievementRewardsInternal", new Class<?>[0]);
        assertEquals(400, ledger(game).balance(), "reopening the claim flow must never pay twice");
        game.persistAchievements(profilePrefs(game));

        BirdGame3 reloaded = loadedGame();
        assertEquals(400, ledger(reloaded).balance());
        assertTrue(reloaded.isAchievementRewardClaimed(BirdGame3Achievement.FIRST_BLOOD.legacyIndex));
        assertTrue(reloaded.isAchievementRewardClaimed(BirdGame3Achievement.DOMINATOR.legacyIndex));
    }

    @Test
    void everyPackRetiresAfterItsUniqueCatalogIsOwned() throws Exception {
        BirdGame3 game = loadedGame();
        @SuppressWarnings("unchecked")
        List<ShopItem> items = (List<ShopItem>) invoke(game, "buildShopItems", new Class<?>[0]);
        for (ShopItem item : items) {
            assertTrue(item.available.getAsBoolean(), item.name + " should offer a fresh profile something new");
            assertTrue(item.remainingUniqueUnlocks(preview -> owned(game, preview)) > 0);
        }
        Set<String> unlocked = new HashSet<>();
        for (ShopItem item : items) {
            for (ShopPreview preview : item.uniqueUnlockPreviews()) {
                if (unlocked.add(preview.skinKey())) {
                    invoke(game, "unlockShopPreview", new Class<?>[]{ShopPreview.class}, preview);
                }
            }
        }

        @SuppressWarnings("unchecked")
        List<ShopItem> exhausted = (List<ShopItem>) invoke(game, "buildShopItems", new Class<?>[0]);
        for (ShopItem item : exhausted) {
            assertEquals(0, item.remainingUniqueUnlocks(preview -> owned(game, preview)), item.name);
            assertFalse(item.available.getAsBoolean(), item.name + " must not sell a coin-only duplicate pack");
        }
    }

    @Test
    void progressionSnapshotKeepsDeveloperEntitlementSeparateFromEarnedBadges() throws Exception {
        BirdGame3 game = loadedGame();
        assertTrue((Boolean) invoke(game, "tryApplySettingsCode", new Class<?>[]{String.class}, "FEATHERDEV"));
        ProgressionOverview overview = (ProgressionOverview) invoke(game, "progressionOverview", new Class<?>[0]);

        assertTrue(overview.infiniteBirdCoins());
        assertEquals(0, overview.remainingShopUnlocks());
        assertTrue(overview.goals().stream().anyMatch(goal -> !goal.completed()),
                "developer access must not manufacture earned challenge badges");
    }

    private BirdGame3 loadedGame() throws Exception {
        BirdGame3 game = new BirdGame3(root);
        invoke(game, "loadProfileProgress", new Class<?>[]{Preferences.class}, profilePrefs(game));
        return game;
    }

    private Preferences profilePrefs(BirdGame3 game) throws Exception {
        Field field = BirdGame3.class.getDeclaredField("saveRepository");
        field.setAccessible(true);
        return ((GameSaveRepository) field.get(game)).activeProfilePrefs();
    }

    private BirdCoinLedger ledger(BirdGame3 game) throws Exception {
        Field field = BirdGame3.class.getDeclaredField("birdCoinLedger");
        field.setAccessible(true);
        return (BirdCoinLedger) field.get(game);
    }

    private boolean owned(BirdGame3 game, ShopPreview preview) {
        try {
            return (Boolean) invoke(game, "isShopPreviewOwned", new Class<?>[]{ShopPreview.class}, preview);
        } catch (Exception e) {
            throw new AssertionError(e);
        }
    }

    private Object invoke(BirdGame3 game, String name, Class<?>[] signature, Object... args) throws Exception {
        Method method = BirdGame3.class.getDeclaredMethod(name, signature);
        method.setAccessible(true);
        return method.invoke(game, args);
    }
}
