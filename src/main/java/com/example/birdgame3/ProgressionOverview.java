package com.example.birdgame3;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Immutable, presentation-only snapshot of the active profile's progression.
 * Building a menu or results screen from this type can never grant currency or
 * unlock content.
 */
record ProgressionOverview(
        int birdCoins,
        boolean infiniteBirdCoins,
        int claimableRewards,
        int ownedShopUnlocks,
        int totalShopUnlocks,
        List<Goal> goals
) {
    record Goal(
            BirdGame3Achievement achievement,
            String title,
            String requirement,
            String reward,
            String progressText,
            int current,
            int target,
            boolean completed,
            boolean rewardClaimable
    ) {
        Goal {
            Objects.requireNonNull(achievement, "achievement");
            title = required(title, "title");
            requirement = required(requirement, "requirement");
            reward = required(reward, "reward");
            progressText = required(progressText, "progressText");
            target = Math.max(1, target);
            current = Math.clamp(current, 0, target);
            if (completed) current = target;
            rewardClaimable &= completed;
        }

        double completionRatio() {
            return current / (double) target;
        }

        int remaining() {
            return Math.max(0, target - current);
        }

        private static String required(String value, String field) {
            String cleaned = Objects.requireNonNull(value, field).strip();
            if (cleaned.isEmpty()) throw new IllegalArgumentException(field + " is required");
            return cleaned;
        }
    }

    ProgressionOverview {
        birdCoins = Math.max(0, birdCoins);
        claimableRewards = Math.max(0, claimableRewards);
        totalShopUnlocks = Math.max(0, totalShopUnlocks);
        ownedShopUnlocks = Math.clamp(ownedShopUnlocks, 0, totalShopUnlocks);
        goals = List.copyOf(Objects.requireNonNull(goals, "goals"));
    }

    Optional<Goal> nextGoal() {
        return goals.stream()
                .filter(goal -> !goal.completed())
                .max(Comparator.comparingDouble(Goal::completionRatio)
                        .thenComparing(Comparator.comparingInt(Goal::remaining).reversed())
                        .thenComparing(goal -> goal.achievement().ordinal(), Comparator.reverseOrder()));
    }

    int remainingShopUnlocks() {
        return Math.max(0, totalShopUnlocks - ownedShopUnlocks);
    }

    String coinBalanceText() {
        return infiniteBirdCoins ? "INFINITE" : Integer.toString(birdCoins);
    }

    String claimableText() {
        if (claimableRewards == 0) return "NO REWARDS WAITING";
        return claimableRewards + (claimableRewards == 1 ? " REWARD READY" : " REWARDS READY");
    }

    String collectionText() {
        return "SHOP COLLECTION  " + ownedShopUnlocks + " / " + totalShopUnlocks;
    }
}
