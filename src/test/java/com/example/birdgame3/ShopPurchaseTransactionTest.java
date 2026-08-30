package com.example.birdgame3;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

class ShopPurchaseTransactionTest {
    private static final long TEST_SALT = 91_337L;

    @Test
    void successfulPurchaseChargesOnceAndReturnsItsReceipt() {
        BirdCoinLedger ledger = ledgerWith(2_000);
        ShopPackResult receipt = new ShopPackResult("Rooftop Pack Opened", List.of(
                new ShopPackResult.Reward("Bird Coins +180", new ShopPreview(null, null, "Bird Coins +180", 180))));

        ShopPurchaseTransaction.Result result = ShopPurchaseTransaction.execute(ledger, 1_100, () -> receipt);

        assertEquals(ShopPurchaseTransaction.Status.SUCCESS, result.status());
        assertSame(receipt, result.receipt());
        assertEquals(900, ledger.balance());
    }

    @Test
    void insufficientFundsNeverRunTheRewardGrant() {
        BirdCoinLedger ledger = ledgerWith(1_000);
        AtomicBoolean called = new AtomicBoolean();

        ShopPurchaseTransaction.Result result = ShopPurchaseTransaction.execute(ledger, 1_100, () -> {
            called.set(true);
            return null;
        });

        assertEquals(ShopPurchaseTransaction.Status.INSUFFICIENT_FUNDS, result.status());
        assertFalse(called.get());
        assertEquals(1_000, ledger.balance());
    }

    @Test
    void failedRewardGrantRefundsTheWholeCharge() {
        BirdCoinLedger ledger = ledgerWith(3_700);
        IllegalStateException failure = new IllegalStateException("broken reward");

        ShopPurchaseTransaction.Result result = ShopPurchaseTransaction.execute(ledger, 3_400, () -> {
            throw failure;
        });

        assertEquals(ShopPurchaseTransaction.Status.FAILED, result.status());
        assertSame(failure, result.failure());
        assertEquals(3_700, ledger.balance());
    }

    @Test
    void shopDefersTheFullscreenSceneReplacementUntilAfterTheBuyEvent() throws Exception {
        String source = Files.readString(Path.of("src/main/java/com/example/birdgame3/BirdGame3.java"));
        String body = methodBody(source, "beginShopPackPurchase");
        assertTrue(body.contains("buyButton.setDisable(true)"), "BUY must be single-shot while opening");
        assertTrue(body.contains("Platform.runLater"), "The fullscreen Scene swap must wait for the BUY event to finish");
        assertTrue(body.contains("ShopPurchaseTransaction.execute"));
    }

    private static BirdCoinLedger ledgerWith(int coins) {
        BirdCoinLedger ledger = new BirdCoinLedger(TEST_SALT, "balance", "earned", "spent", "checksum");
        ledger.grant(coins);
        return ledger;
    }

    private static String methodBody(String source, String name) {
        int start = source.indexOf("private void " + name + "(");
        assertTrue(start >= 0, "Missing method " + name);
        int open = source.indexOf('{', start);
        int depth = 0;
        for (int i = open; i < source.length(); i++) {
            char c = source.charAt(i);
            if (c == '{') depth++;
            if (c == '}' && --depth == 0) return source.substring(open, i + 1);
        }
        fail("Unclosed method " + name);
        return "";
    }
}
