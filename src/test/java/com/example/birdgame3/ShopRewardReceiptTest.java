package com.example.birdgame3;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.prefs.Preferences;

import static org.junit.jupiter.api.Assertions.*;

class ShopRewardReceiptTest {
    private final Preferences prefs = Preferences.userRoot().node("/birdfight3-tests/reward-receipt/" + UUID.randomUUID());
    private final BirdGame3 game = new BirdGame3(prefs);

    @AfterEach
    void removeTestProfile() throws Exception {
        prefs.removeNode();
    }

    @Test
    void emptyPackPoolStillGrantsExactlyTheOriginalFallbackCoins() throws Exception {
        ShopPackResult receipt = openPack(3, List.of());
        assertEquals(450, ledger().balance());
        assertEquals(3, receipt.rewards().size());
        for (var reward : receipt.rewards()) {
            assertEquals("Bird Coins +150", reward.label());
            assertEquals(150, reward.preview().value());
            assertEquals(ShopPackResult.Outcome.CURRENCY, reward.outcome());
            assertEquals(RewardPresentation.Kind.COINS, presentation(reward.preview()).kind());
        }
        assertEquals("3 CURRENCY REWARDS", receipt.summaryLine());
        assertEquals(450, ledger().balance(), "Rebuilding presentation data must not grant coins");
    }

    @Test
    void guaranteedUnlockOnlyGrantsOnceAndEachReceiptRetainsTheRightArt() throws Exception {
        AtomicInteger grants = new AtomicInteger();
        var preview = new ShopPreview(BirdGame3.BirdType.PIGEON, "NOIR_PIGEON", "Noir Pigeon");
        PackReward oneSkin = new PackReward("Noir Pigeon Skin", preview, 1,
                () -> grants.get() == 0, grants::incrementAndGet);
        ShopPackResult receipt = openPack(3, List.of(oneSkin));
        assertEquals(1, grants.get());
        assertEquals(300, ledger().balance());
        assertSame(preview, receipt.rewards().getFirst().preview());
        assertEquals(ShopPackResult.Outcome.NEW_UNLOCK, receipt.rewards().getFirst().outcome());
        assertEquals(1, receipt.newUnlockCount());
        assertEquals(2, receipt.currencyRewardCount());
        RewardPresentation presentation = presentation(preview);
        assertEquals(RewardPresentation.Kind.SKIN, presentation.kind());
        assertEquals(BirdGame3.BirdType.PIGEON, presentation.bird());
        assertEquals("NOIR_PIGEON", presentation.skinKey());
        assertEquals(1, grants.get());
    }

    @Test
    void everyAvailablePullPrefersANewUnlockBeforeUsingCoinFallbacks() throws Exception {
        AtomicInteger firstGrant = new AtomicInteger();
        AtomicInteger secondGrant = new AtomicInteger();
        PackReward first = new PackReward("Dune Falcon Skin",
                new ShopPreview(BirdGame3.BirdType.FALCON, "DUNE_FALCON", "Dune Falcon"),
                1, () -> firstGrant.get() == 0, firstGrant::incrementAndGet);
        PackReward second = new PackReward("Mint Penguin Skin",
                new ShopPreview(BirdGame3.BirdType.PENGUIN, "MINT_PENGUIN", "Mint Penguin"),
                1, () -> secondGrant.get() == 0, secondGrant::incrementAndGet);

        ShopPackResult receipt = openPack(2, List.of(first, second));

        assertEquals(1, firstGrant.get());
        assertEquals(1, secondGrant.get());
        assertEquals(2, receipt.newUnlockCount());
        assertEquals(0, receipt.currencyRewardCount());
        assertEquals("2 NEW UNLOCKS", receipt.summaryLine());
    }

    @Test
    void guaranteedBirdPackKeepsCharacterAndExtraCoinRewards() throws Exception {
        var preview = new ShopPreview(BirdGame3.BirdType.BAT, "CHAR_BAT", "Bat");
        ShopPackResult receipt = (ShopPackResult) invoke("openGuaranteedBirdPack",
                new Class<?>[]{List.class, List.class, List.class}, List.of(preview), List.of(), List.of());
        assertEquals(3, receipt.rewards().size());
        assertTrue((Boolean) read("batUnlocked"));
        assertEquals(400, ledger().balance());
        assertEquals(RewardPresentation.Kind.BIRD, presentation(receipt.rewards().getFirst().preview()).kind());
        assertNull(presentation(preview).skinKey(), "A character unlock key is not a skin");
        assertEquals(1, ((java.util.Queue<?>) read("pendingUnlockCards")).size());
    }

    @Test
    void stageReceiptAndCoinOnlyAscendantFallbackStayAccurate() throws Exception {
        var stage = new ShopPreview(null, "MAP_DESERT", "Sunscorch Flats");
        assertEquals(RewardPresentation.Kind.STAGE, presentation(stage).kind());
        assertEquals(BirdGame3.MapType.DESERT, presentation(stage).map());
        ShopPackResult receipt = (ShopPackResult) invoke("openGuaranteedBirdPack",
                new Class<?>[]{List.class, List.class, List.class}, List.of(), List.of(), List.of());
        assertEquals(700, ledger().balance());
        assertEquals(List.of(300, 200, 200), receipt.rewards().stream().map(r -> r.preview().value()).toList());
    }

    @Test
    void receiptIsAnImmutableSnapshotNotAGrantCallback() {
        var rewards = new ArrayList<ShopPackResult.Reward>();
        rewards.add(new ShopPackResult.Reward("Bird Coins +150", new ShopPreview(null, null, "Bird Coins +150", 150)));
        var receipt = new ShopPackResult("Test", rewards);
        rewards.clear();
        assertEquals(1, receipt.rewards().size());
        assertThrows(UnsupportedOperationException.class, () -> receipt.rewards().clear());
        assertEquals("- Bird Coins +150", receipt.message());
        assertEquals("1 CURRENCY REWARD", receipt.summaryLine());
    }

    private ShopPackResult openPack(int count, List<PackReward> rewards) throws Exception {
        return (ShopPackResult) invoke("openCardPack", new Class<?>[]{String.class, int.class, List.class}, "Test", count, rewards);
    }

    private RewardPresentation presentation(ShopPreview preview) throws Exception {
        return (RewardPresentation) invoke("rewardPresentationForPreview", new Class<?>[]{ShopPreview.class, String.class}, preview, "");
    }

    private BirdCoinLedger ledger() throws Exception { return (BirdCoinLedger) read("birdCoinLedger"); }

    private Object read(String name) throws Exception {
        Field field = BirdGame3.class.getDeclaredField(name);
        field.setAccessible(true);
        return field.get(game);
    }

    private Object invoke(String name, Class<?>[] signature, Object... args) throws Exception {
        Method method = BirdGame3.class.getDeclaredMethod(name, signature);
        method.setAccessible(true);
        return method.invoke(game, args);
    }
}
