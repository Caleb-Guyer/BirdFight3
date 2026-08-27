package com.example.birdgame3;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.prefs.Preferences;

import static org.junit.jupiter.api.Assertions.*;

class StarterContentDefaultsTest {
    private final Preferences prefs = Preferences.userRoot().node(
            "/birdfight3-tests/starter-content/" + UUID.randomUUID());

    @AfterEach
    void removeTestProfile() throws Exception {
        prefs.removeNode();
        prefs.flush();
    }

    @Test
    void freshProfileHasEightStarterBirdsFourMainMapsAndTwoVariants() throws Exception {
        BirdGame3 game = loadedGame();
        Set<BirdGame3.BirdType> unlockedBirds = EnumSet.noneOf(BirdGame3.BirdType.class);
        for (BirdGame3.BirdType bird : BirdGame3.BirdType.values()) {
            if ((Boolean) invoke(game, "isBirdUnlocked", new Class<?>[]{BirdGame3.BirdType.class}, bird)) {
                unlockedBirds.add(bird);
            }
        }
        assertEquals(EnumSet.of(
                BirdGame3.BirdType.PIGEON,
                BirdGame3.BirdType.EAGLE,
                BirdGame3.BirdType.HUMMINGBIRD,
                BirdGame3.BirdType.TURKEY,
                BirdGame3.BirdType.PENGUIN,
                BirdGame3.BirdType.SHOEBILL,
                BirdGame3.BirdType.MOCKINGBIRD,
                BirdGame3.BirdType.RAZORBILL
        ), unlockedBirds);

        Set<BirdGame3.MapType> unlockedMaps = EnumSet.noneOf(BirdGame3.MapType.class);
        for (BirdGame3.MapType map : BirdGame3.MapType.values()) {
            if ((Boolean) invoke(game, "isMapUnlocked", new Class<?>[]{BirdGame3.MapType.class}, map)) {
                unlockedMaps.add(map);
            }
        }
        assertEquals(EnumSet.of(
                BirdGame3.MapType.FOREST,
                BirdGame3.MapType.CITY,
                BirdGame3.MapType.SKYCLIFFS,
                BirdGame3.MapType.VIBRANT_JUNGLE
        ), unlockedMaps);

        Set<BirdGame3.MapVariant> starterVariants = EnumSet.noneOf(BirdGame3.MapVariant.class);
        for (BirdGame3.MapVariant variant : BirdGame3.MapVariant.values()) {
            if (variant != BirdGame3.MapVariant.STANDARD
                    && (Boolean) invoke(game, "isMapVariantUnlocked",
                    new Class<?>[]{BirdGame3.MapVariant.class}, variant)) {
                starterVariants.add(variant);
            }
        }
        assertEquals(EnumSet.of(
                BirdGame3.MapVariant.CROWN_DUEL,
                BirdGame3.MapVariant.PARLIAMENT_ROOFTOPS
        ), starterVariants);
    }

    @Test
    void premiumAndStockPhotoSkinsAreNotStarterSkins() throws Exception {
        BirdGame3 game = loadedGame();
        assertFalse(skinUnlocked(game, "PREMIUM_PIGEON", BirdGame3.BirdType.PIGEON));
        assertFalse(skinUnlocked(game, "STOCK_PHOTO_EAGLE", BirdGame3.BirdType.EAGLE));
        assertFalse(skinUnlocked(game, "STOCK_PHOTO_TURKEY", BirdGame3.BirdType.TURKEY));
        assertFalse(skinUnlocked(game, "STOCK_PHOTO_EAGLE", BirdGame3.BirdType.TURKEY),
                "A skin can never be equipped by the wrong bird");
    }

    @Test
    void everyRemovedStarterItemIsRepresentedInCardPackPreviewsAndCanPersist() throws Exception {
        BirdGame3 game = loadedGame();
        @SuppressWarnings("unchecked")
        List<ShopItem> items = (List<ShopItem>) invoke(game, "buildShopItems", new Class<?>[0]);
        Set<String> previewKeys = new HashSet<>();
        for (ShopItem item : items) {
            for (ShopPreview preview : item.previews) {
                if (preview != null && preview.skinKey() != null) previewKeys.add(preview.skinKey());
            }
        }
        for (CollectibleUnlock unlock : CollectibleUnlock.values()) {
            assertTrue(previewKeys.contains(unlock.key), unlock.label + " must be earnable from a pack");
            ShopPreview preview = unlock.preview();
            assertFalse((Boolean) invoke(game, "isShopPreviewOwned",
                    new Class<?>[]{ShopPreview.class}, preview));
            invoke(game, "unlockShopPreview", new Class<?>[]{ShopPreview.class}, preview);
            assertTrue((Boolean) invoke(game, "isShopPreviewOwned",
                    new Class<?>[]{ShopPreview.class}, preview));
        }

        invoke(game, "saveProfileProgress", new Class<?>[]{Preferences.class}, profilePrefs(game));
        BirdGame3 reloaded = loadedGame();
        for (CollectibleUnlock unlock : CollectibleUnlock.values()) {
            assertTrue((Boolean) invoke(reloaded, "isShopPreviewOwned",
                    new Class<?>[]{ShopPreview.class}, unlock.preview()));
        }
    }

    @Test
    void featherDevStillEntitlesAllRemovedStarterContent() throws Exception {
        BirdGame3 game = new BirdGame3(prefs);
        Preferences savePrefs = profilePrefs(game);
        savePrefs.putInt("starter_catalog_version", 1);
        savePrefs.putBoolean("developer_infinite_bird_coins", true);
        invoke(game, "loadProfileProgress", new Class<?>[]{Preferences.class}, savePrefs);

        for (CollectibleUnlock unlock : CollectibleUnlock.values()) {
            assertTrue((Boolean) invoke(game, "isShopPreviewOwned",
                    new Class<?>[]{ShopPreview.class}, unlock.preview()));
        }
        for (BirdGame3.MapVariant variant : BirdGame3.MapVariant.values()) {
            assertTrue((Boolean) invoke(game, "isMapVariantUnlocked",
                    new Class<?>[]{BirdGame3.MapVariant.class}, variant));
        }
    }

    private BirdGame3 loadedGame() throws Exception {
        BirdGame3 game = new BirdGame3(prefs);
        invoke(game, "loadProfileProgress", new Class<?>[]{Preferences.class}, profilePrefs(game));
        return game;
    }

    private Preferences profilePrefs(BirdGame3 game) throws Exception {
        Field field = BirdGame3.class.getDeclaredField("saveRepository");
        field.setAccessible(true);
        return ((GameSaveRepository) field.get(game)).activeProfilePrefs();
    }

    private boolean skinUnlocked(BirdGame3 game, String key, BirdGame3.BirdType type) throws Exception {
        return (Boolean) invoke(game, "isSkinUnlocked",
                new Class<?>[]{String.class, BirdGame3.BirdType.class}, key, type);
    }

    private Object invoke(BirdGame3 game, String name, Class<?>[] signature, Object... args) throws Exception {
        Method method = BirdGame3.class.getDeclaredMethod(name, signature);
        method.setAccessible(true);
        return method.invoke(game, args);
    }
}
