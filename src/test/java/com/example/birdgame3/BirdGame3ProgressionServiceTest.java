package com.example.birdgame3;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BirdGame3ProgressionServiceTest {
    private final BirdGame3ProgressionService service = new BirdGame3ProgressionService();

    @Test
    void winnerMapProgressReturnsClutchAndUrbanKingUnlocksOnFifthCityWin() {
        BirdGame3ProgressionService.WinnerMapProgressResult result =
                service.evaluateWinnerMapProgress(BirdGame3.MapType.CITY, 12.0, 4, 1, 2);

        assertTrue(result.incrementCityWins());
        assertFalse(result.incrementCliffWins());
        assertFalse(result.incrementJungleWins());
        assertEquals(Set.of(BirdGame3Achievement.CLUTCH_GOD, BirdGame3Achievement.URBAN_KING), unlockAchievements(result.unlocks()));
    }

    @Test
    void reconcileStoredAchievementUnlocksRestoresStoryAndModeMilestones() {
        BirdGame3AchievementProfile achievementProfile = new BirdGame3AchievementProfile();
        achievementProfile.setProgress(BirdGame3Achievement.CROWN_UNBROKEN, 1);

        boolean[] classicCompleted = new boolean[BirdGame3.BirdType.values().length];
        boolean[] completedAdventure = new boolean[]{true, true, true};

        List<BirdGame3ProgressionService.AchievementUnlock> unlocks =
                service.reconcileStoredAchievementUnlocks(
                        new BirdGame3ProgressionService.StoredAchievementContext(
                                achievementProfile,
                                classicCompleted,
                                completedAdventure,
                                2,
                                1,
                                true,
                                true,
                                true,
                                3,
                                3,
                                1
                        )
                );

        assertEquals(
                Set.of(
                        BirdGame3Achievement.ECHOES_BELOW,
                        BirdGame3Achievement.STORY_KEEPER,
                        BirdGame3Achievement.BOSS_BREAKER,
                        BirdGame3Achievement.CROWN_UNBROKEN,
                        BirdGame3Achievement.GROVE_SENTINEL,
                        BirdGame3Achievement.ROOFTOP_LEGACY,
                        BirdGame3Achievement.ECHO_SOVEREIGN,
                        BirdGame3Achievement.IRON_TEMPEST,
                        BirdGame3Achievement.BLIGHT_BUSTER,
                        BirdGame3Achievement.BRACKET_BOSS
                ),
                unlockAchievements(unlocks)
        );
    }

    @Test
    void adventureChapterRewardsSplitMainAndTempestUnlocks() {
        BirdGame3ProgressionService.AdventureChapterRewards beaconRewards =
                service.evaluateAdventureChapterRewards(false, 4, 4, 8, 3, false, false, false, false, false);
        BirdGame3ProgressionService.AdventureChapterRewards unitedRewards =
                service.evaluateAdventureChapterRewards(false, 8, 4, 8, 3, true, false, false, false, false);
        BirdGame3ProgressionService.AdventureChapterRewards tempestRewards =
                service.evaluateAdventureChapterRewards(true, 3, 4, 8, 3, true, true, true, false, false);

        assertTrue(beaconRewards.unlockBeaconPigeon());
        assertFalse(beaconRewards.unlockNullRockVulture());
        assertFalse(beaconRewards.unlockBeaconCrownMap());

        assertFalse(unitedRewards.unlockBeaconPigeon());
        assertTrue(unitedRewards.unlockNullRockVulture());
        assertTrue(unitedRewards.unlockBeaconCrownMap());

        assertTrue(tempestRewards.unlockIroncladPelican());
        assertTrue(tempestRewards.unlockDockMap());
    }

    private Set<BirdGame3Achievement> unlockAchievements(List<BirdGame3ProgressionService.AchievementUnlock> unlocks) {
        return unlocks.stream()
                .map(BirdGame3ProgressionService.AchievementUnlock::achievement)
                .collect(Collectors.toSet());
    }
}
