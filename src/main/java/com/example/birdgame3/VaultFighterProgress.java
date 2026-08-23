package com.example.birdgame3;

import java.util.Locale;

/**
 * Immutable, presentation-safe snapshot for one fighter's Vault card.
 * Entitlement and accomplishment deliberately remain separate: FEATHERDEV may
 * expose content without manufacturing an earned Classic badge.
 */
record VaultFighterProgress(
        BirdGame3.BirdType bird,
        boolean fighterUnlocked,
        boolean routeBadgeEarned,
        boolean endingAvailable,
        boolean academyDrillComplete,
        int skinsOwned,
        int skinsTotal,
        int appearances,
        int wins,
        int damage,
        int knockouts,
        long arenaFrames
) {
    VaultFighterProgress {
        skinsTotal = Math.max(0, skinsTotal);
        skinsOwned = Math.clamp(skinsOwned, 0, skinsTotal);
        appearances = Math.max(0, appearances);
        wins = Math.clamp(wins, 0, appearances);
        damage = Math.max(0, damage);
        knockouts = Math.max(0, knockouts);
        arenaFrames = Math.max(0L, arenaFrames);
    }

    double winRate() {
        return appearances <= 0 ? 0.0 : wins / (double) appearances;
    }

    String winRateText() {
        return appearances <= 0
                ? "--"
                : String.format(Locale.ROOT, "%.0f%%", winRate() * 100.0);
    }

    String arenaTimeText() {
        long totalSeconds = arenaFrames / 60L;
        long hours = totalSeconds / 3_600L;
        long minutes = (totalSeconds % 3_600L) / 60L;
        if (hours > 0) {
            return hours + "h " + minutes + "m";
        }
        long seconds = totalSeconds % 60L;
        return minutes > 0 ? minutes + "m " + seconds + "s" : seconds + "s";
    }
}
