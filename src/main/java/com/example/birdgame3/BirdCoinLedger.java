package com.example.birdgame3;

import java.util.prefs.Preferences;

final class BirdCoinLedger {
    private final long checksumSalt;
    private final String balanceKey;
    private final String earnedKey;
    private final String spentKey;
    private final String checksumKey;

    private int balance = 0;
    private int earned = 0;
    private int spent = 0;

    BirdCoinLedger(long checksumSalt, String balanceKey, String earnedKey, String spentKey, String checksumKey) {
        this.checksumSalt = checksumSalt;
        this.balanceKey = balanceKey;
        this.earnedKey = earnedKey;
        this.spentKey = spentKey;
        this.checksumKey = checksumKey;
    }

    int balance() {
        return balance;
    }

    void load(Preferences prefs) {
        balance = Math.max(0, prefs.getInt(balanceKey, 0));

        String earnedValue = prefs.get(earnedKey, null);
        String spentValue = prefs.get(spentKey, null);
        if (earnedValue == null || spentValue == null) {
            earned = Math.max(0, balance);
            spent = 0;
            synchronize();
            return;
        }

        earned = Math.max(0, prefs.getInt(earnedKey, 0));
        spent = Math.max(0, prefs.getInt(spentKey, 0));
        synchronize();

        long savedChecksum = prefs.getLong(checksumKey, Long.MIN_VALUE);
        long expectedChecksum = computeChecksum(balance, earned, spent);
        int storedBalance = Math.max(0, prefs.getInt(balanceKey, 0));
        if (savedChecksum != expectedChecksum || storedBalance != balance) {
            if (storedBalance > balance) {
                System.err.println("Bird coin ledger mismatch detected; restoring balance from "
                        + storedBalance + " to " + balance + '.');
            }
        }
    }

    void save(Preferences prefs) {
        synchronize();
        prefs.putInt(balanceKey, balance);
        prefs.putInt(earnedKey, earned);
        prefs.putInt(spentKey, spent);
        prefs.putLong(checksumKey, computeChecksum(balance, earned, spent));
    }

    void grant(int amount) {
        int safeAmount = Math.max(0, amount);
        if (safeAmount == 0) {
            synchronize();
            return;
        }
        earned = saturatingNonNegativeAdd(earned, safeAmount);
        synchronize();
    }

    boolean spend(int amount) {
        int safeAmount = Math.max(0, amount);
        int availableCoins = computeBalanceFromLedger();
        if (safeAmount > availableCoins) {
            balance = availableCoins;
            return false;
        }
        spent = saturatingNonNegativeAdd(spent, safeAmount);
        synchronize();
        return true;
    }

    private int saturatingNonNegativeAdd(int current, int delta) {
        long total = (long) current + Math.max(0, delta);
        if (total <= 0L) {
            return 0;
        }
        return total >= Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) total;
    }

    private int computeBalanceFromLedger() {
        long computedBalance = (long) earned - spent;
        if (computedBalance <= 0L) {
            return 0;
        }
        return computedBalance >= Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) computedBalance;
    }

    private long computeChecksum(int checksumBalance, int checksumEarned, int checksumSpent) {
        long checksum = checksumSalt;
        checksum = Long.rotateLeft(checksum ^ (checksumBalance * 0x9E3779B9L), 13);
        checksum = Long.rotateLeft(checksum ^ (checksumEarned * 0x85EBCA6BL), 17);
        checksum = Long.rotateLeft(checksum ^ (checksumSpent * 0xC2B2AE35L), 7);
        return checksum ^ 0xA5A5A5A5A5A5A5A5L;
    }

    private void synchronize() {
        if (spent > earned) {
            spent = earned;
        }
        balance = computeBalanceFromLedger();
    }
}
