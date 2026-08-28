package com.example.birdgame3;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.EnumSet;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ContentUnlockGuideTest {
    private static final EnumSet<BirdGame3.BirdType> STARTER_BIRDS = EnumSet.of(
            BirdGame3.BirdType.PIGEON,
            BirdGame3.BirdType.EAGLE,
            BirdGame3.BirdType.HUMMINGBIRD,
            BirdGame3.BirdType.TURKEY,
            BirdGame3.BirdType.PENGUIN,
            BirdGame3.BirdType.SHOEBILL,
            BirdGame3.BirdType.MOCKINGBIRD,
            BirdGame3.BirdType.RAZORBILL
    );

    @Test
    void everyBirdMapAndVariantHasPlayerFacingUnlockDirections() {
        for (BirdGame3.BirdType bird : BirdGame3.BirdType.values()) {
            String guide = ContentUnlockGuide.bird(bird);
            assertFalse(guide.isBlank(), bird + " unlock guide");
            assertTrue(STARTER_BIRDS.contains(bird) == guide.startsWith("Available from the start"),
                    bird + " starter status must match its guide: " + guide);
        }
        for (BirdGame3.MapType map : BirdGame3.MapType.values()) {
            assertFalse(ContentUnlockGuide.map(map).isBlank(), map + " unlock guide");
        }
        for (BirdGame3.MapVariant variant : BirdGame3.MapVariant.values()) {
            assertFalse(ContentUnlockGuide.variant(variant).isBlank(), variant + " unlock guide");
        }
    }

    @Test
    void guideNamesExactProgressionSourcesInsteadOfGenericLockedCopy() {
        assertTrue(ContentUnlockGuide.bird(BirdGame3.BirdType.BAT).contains("Vine Swinger"));
        assertTrue(ContentUnlockGuide.bird(BirdGame3.BirdType.TITMOUSE).contains("Classic with Hummingbird"));
        assertTrue(ContentUnlockGuide.map(BirdGame3.MapType.PRISON).contains("Blackout Key"));
        assertTrue(ContentUnlockGuide.map(BirdGame3.MapType.WORLDSEAM).contains("Razorbill"));
        assertTrue(ContentUnlockGuide.variant(BirdGame3.MapVariant.HEARTBLOOM_SANCTUARY)
                .contains("Classic with Hummingbird"));
        assertTrue(ContentUnlockGuide.skin("STORM_PIGEON", BirdGame3.BirdType.PIGEON)
                .contains("Rooftop Legacy"));
        assertTrue(ContentUnlockGuide.skin("PREMIUM_PIGEON", BirdGame3.BirdType.PIGEON)
                .contains("Card Packs"));
    }

    @Test
    void featherpediaMapGuideIncludesEveryVariantAndCurrentLockState() throws Exception {
        BirdGame3 game = new BirdGame3();
        Method guide = BirdGame3.class.getDeclaredMethod("mapVariantGuide", BirdGame3.MapType.class);
        guide.setAccessible(true);

        String city = (String) guide.invoke(game, BirdGame3.MapType.CITY);
        assertTrue(city.contains("UNLOCKED  •  Parliament Towers"));
        assertTrue(city.contains("LOCKED  •  Rooftop Relay"));
        assertTrue(city.contains("Complete Classic with Pigeon"));
    }
}
