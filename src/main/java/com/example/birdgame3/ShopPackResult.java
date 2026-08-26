package com.example.birdgame3;

import java.util.List;

/** Immutable receipt of rewards already granted by the purchase, not another grant command. */
record ShopPackResult(String title, List<Reward> rewards) {
    record Reward(String label, ShopPreview preview) { }

    ShopPackResult {
        rewards = List.copyOf(rewards);
    }

    String message() {
        return rewards.stream().map(reward -> "- " + reward.label())
                .collect(java.util.stream.Collectors.joining("\n"));
    }
}
