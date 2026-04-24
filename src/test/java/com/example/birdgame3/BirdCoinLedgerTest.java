package com.example.birdgame3;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BirdCoinLedgerTest {
    private static final long TEST_SALT = 123_456_789L;

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
    void loadBootstrapsLegacyBalanceAndPersistsLedgerFields() {
        prefs.putInt("balance", 75);

        BirdCoinLedger ledger = new BirdCoinLedger(TEST_SALT, "balance", "earned", "spent", "checksum");
        ledger.load(prefs);

        assertEquals(75, ledger.balance());
        assertEquals(75, prefs.getInt("earned", -1));
        assertEquals(0, prefs.getInt("spent", -1));
        assertNotEquals(Long.MIN_VALUE, prefs.getLong("checksum", Long.MIN_VALUE));
    }

    @Test
    void loadResetsLedgerWhenChecksumDoesNotMatch() {
        BirdCoinLedger original = new BirdCoinLedger(TEST_SALT, "balance", "earned", "spent", "checksum");
        original.grant(90);
        assertTrue(original.spend(30));
        original.save(prefs);

        prefs.putInt("balance", 999);

        BirdCoinLedger loaded = new BirdCoinLedger(TEST_SALT, "balance", "earned", "spent", "checksum");
        loaded.load(prefs);

        assertEquals(0, loaded.balance());
        assertEquals(0, prefs.getInt("balance", -1));
        assertEquals(0, prefs.getInt("earned", -1));
        assertEquals(0, prefs.getInt("spent", -1));
    }

    @Test
    void spendRejectsOverspendAndPreservesBalance() {
        BirdCoinLedger ledger = new BirdCoinLedger(TEST_SALT, "balance", "earned", "spent", "checksum");
        ledger.grant(40);

        assertFalse(ledger.spend(50));
        assertEquals(40, ledger.balance());
        assertTrue(ledger.spend(15));
        assertEquals(25, ledger.balance());
    }

    @Test
    void infiniteBalanceAllowsSpendingWithoutReducingStoredBalance() {
        BirdCoinLedger ledger = new BirdCoinLedger(TEST_SALT, "balance", "earned", "spent", "checksum");
        ledger.grant(40);
        ledger.setInfiniteBalance(true);

        assertTrue(ledger.hasInfiniteBalance());
        assertTrue(ledger.spend(500));
        assertEquals(40, ledger.balance());
    }
}
