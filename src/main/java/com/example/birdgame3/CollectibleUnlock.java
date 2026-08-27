package com.example.birdgame3;

import com.example.birdgame3.BirdGame3.BirdType;
import com.example.birdgame3.BirdGame3.MapType;

/** Former starter content with explicit, profile-local ownership and pack rewards. */
enum CollectibleUnlock {
    GRINCH_HAWK("CHAR_GRINCHHAWK", BirdType.GRINCHHAWK, null, "Grinch-Hawk"),
    VULTURE("CHAR_VULTURE", BirdType.VULTURE, null, "Vulture"),
    OPIUM_BIRD("CHAR_OPIUMBIRD", BirdType.OPIUMBIRD, null, "Opium Bird"),
    PELICAN("CHAR_PELICAN", BirdType.PELICAN, null, "Pelican"),
    GOOSE("CHAR_GOOSE", BirdType.GOOSE, null, "Goose"),
    KIWI("CHAR_KIWI", BirdType.KIWI, null, "Kiwi Bird"),
    FROSTBITE_FJORD("MAP_FROSTBITE_FJORD", null, MapType.FROSTBITE_FJORD, "Frostbite Fjord Map"),
    ASHFALL_CATHEDRAL("MAP_ASHFALL_CATHEDRAL", null, MapType.ASHFALL_CATHEDRAL, "Ashfall Cathedral Map"),
    PREMIUM_PIGEON("PREMIUM_PIGEON", BirdType.PIGEON, null, "Premium Pigeon"),
    STOCK_PHOTO_EAGLE("STOCK_PHOTO_EAGLE", BirdType.EAGLE, null, "Stock Photo Eagle"),
    STOCK_PHOTO_TURKEY("STOCK_PHOTO_TURKEY", BirdType.TURKEY, null, "Stock Photo Turkey");

    final String key;
    final BirdType bird;
    final MapType map;
    final String label;

    CollectibleUnlock(String key, BirdType bird, MapType map, String label) {
        this.key = key;
        this.bird = bird;
        this.map = map;
        this.label = label;
    }

    boolean isBird() { return key.startsWith("CHAR_"); }
    boolean isSkin() { return bird != null && !isBird(); }
    ShopPreview preview() { return new ShopPreview(bird, key, label); }

    static CollectibleUnlock forKey(String key) {
        if (key == null) return null;
        for (CollectibleUnlock unlock : values()) {
            if (unlock.key.equals(key)) return unlock;
        }
        return null;
    }

    static CollectibleUnlock forBird(BirdType bird) {
        for (CollectibleUnlock unlock : values()) {
            if (unlock.isBird() && unlock.bird == bird) return unlock;
        }
        return null;
    }

    static CollectibleUnlock forMap(MapType map) {
        if (map == null) return null;
        for (CollectibleUnlock unlock : values()) {
            if (unlock.map == map) return unlock;
        }
        return null;
    }
}
