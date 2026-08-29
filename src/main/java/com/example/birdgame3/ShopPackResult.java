package com.example.birdgame3;

import java.util.List;

/** Immutable receipt of rewards already granted by the purchase, not another grant command. */
record ShopPackResult(String title, List<Reward> rewards) {
    enum Outcome {
        NEW_UNLOCK,
        CURRENCY
    }

    record Reward(String label, ShopPreview preview, Outcome outcome) {
        Reward(String label, ShopPreview preview) {
            this(label, preview,
                    preview != null && preview.value() > 0 ? Outcome.CURRENCY : Outcome.NEW_UNLOCK);
        }

        Reward {
            if (label == null || label.isBlank()) throw new IllegalArgumentException("Reward label is required");
            if (preview == null) throw new IllegalArgumentException("Reward preview is required");
            if (outcome == null) throw new IllegalArgumentException("Reward outcome is required");
        }
    }

    ShopPackResult {
        rewards = List.copyOf(rewards);
    }

    String message() {
        return rewards.stream().map(reward -> "- " + reward.label())
                .collect(java.util.stream.Collectors.joining("\n"));
    }

    int newUnlockCount() {
        return (int) rewards.stream().filter(reward -> reward.outcome() == Outcome.NEW_UNLOCK).count();
    }

    int currencyRewardCount() {
        return (int) rewards.stream().filter(reward -> reward.outcome() == Outcome.CURRENCY).count();
    }

    String summaryLine() {
        int unlocks = newUnlockCount();
        int currency = currencyRewardCount();
        if (unlocks == 0) return currency + (currency == 1 ? " CURRENCY REWARD" : " CURRENCY REWARDS");
        if (currency == 0) return unlocks + (unlocks == 1 ? " NEW UNLOCK" : " NEW UNLOCKS");
        return unlocks + (unlocks == 1 ? " NEW UNLOCK" : " NEW UNLOCKS")
                + "  •  " + currency + (currency == 1 ? " CURRENCY REWARD" : " CURRENCY REWARDS");
    }
}
