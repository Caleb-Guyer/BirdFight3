package com.example.birdgame3;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ProgressionOverviewTest {
    @Test
    void nextGoalChoosesTheClosestIncompleteGoalAndIgnoresCompletedRewards() {
        ProgressionOverview.Goal early = goal(BirdGame3Achievement.FIRST_BLOOD, 2, 5, false, false);
        ProgressionOverview.Goal closest = goal(BirdGame3Achievement.DOMINATOR, 9, 10, false, false);
        ProgressionOverview.Goal completed = goal(BirdGame3Achievement.ANNIHILATOR, 1, 1, true, true);
        ProgressionOverview overview = new ProgressionOverview(250, false, 1, 3, 8,
                List.of(early, closest, completed));

        assertEquals(closest, overview.nextGoal().orElseThrow());
        assertEquals("1 REWARD READY", overview.claimableText());
        assertEquals("SHOP COLLECTION  3 / 8", overview.collectionText());
        assertEquals(5, overview.remainingShopUnlocks());
    }

    @Test
    void snapshotClampsUnsafeCountsAndCannotBeMutatedThroughTheSourceList() {
        List<ProgressionOverview.Goal> goals = new ArrayList<>();
        goals.add(goal(BirdGame3Achievement.FIRST_BLOOD, -5, 0, false, false));
        ProgressionOverview overview = new ProgressionOverview(-20, false, -2, 99, 4, goals);
        goals.clear();

        assertEquals(0, overview.birdCoins());
        assertEquals(0, overview.claimableRewards());
        assertEquals(4, overview.ownedShopUnlocks());
        assertEquals(4, overview.totalShopUnlocks());
        assertEquals(1, overview.goals().size());
        assertEquals(0.0, overview.goals().getFirst().completionRatio());
        assertThrows(UnsupportedOperationException.class, () -> overview.goals().clear());
    }

    @Test
    void infiniteBalanceAndCompletedCollectionHaveExplicitPlayerFacingCopy() {
        ProgressionOverview overview = new ProgressionOverview(12, true, 0, 6, 6, List.of());
        assertEquals("INFINITE", overview.coinBalanceText());
        assertEquals("NO REWARDS WAITING", overview.claimableText());
        assertEquals(0, overview.remainingShopUnlocks());
        assertTrue(overview.nextGoal().isEmpty());
    }

    private ProgressionOverview.Goal goal(BirdGame3Achievement achievement, int current, int target,
                                          boolean completed, boolean claimable) {
        return new ProgressionOverview.Goal(
                achievement,
                achievement.displayName,
                achievement.description,
                "100 Bird Coins",
                current + " / " + target,
                current,
                target,
                completed,
                claimable
        );
    }
}
