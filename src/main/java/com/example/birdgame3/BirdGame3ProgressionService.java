package com.example.birdgame3;

import java.util.ArrayList;
import java.util.List;

final class BirdGame3ProgressionService {
    record AchievementUnlock(BirdGame3Achievement achievement, String title) {
    }

    record WinnerMapProgressResult(boolean incrementCityWins, boolean incrementCliffWins,
                                   boolean incrementJungleWins, List<AchievementUnlock> unlocks) {
    }

    record ModeMilestoneProgress(int bossRushClearProgress, int towerDefenseBadgeProgress,
                                 int pigeonEpisodeProgress, int batEpisodeProgress,
                                 int pelicanEpisodeProgress, int bigForestBadgeProgress,
                                 int tournamentProgress) {
    }

    record StoredAchievementContext(BirdGame3AchievementProfile achievementProfile, boolean[] classicCompleted,
                                    boolean[] completedAdventure, int bossRushClearCount,
                                    int towerDefenseBadgeCount, boolean pigeonEpisodeCompleted,
                                    boolean batEpisodeCompleted, boolean pelicanEpisodeCompleted,
                                    int bigForestBadgeCount, int bigForestBadgeGoal,
                                    int tournamentChampionshipsWon) {
    }

    record AdventureChapterRewards(boolean unlockBeaconPigeon, boolean unlockNullRockVulture,
                                   boolean unlockBeaconCrownMap, boolean unlockIroncladPelican,
                                   boolean unlockDockMap) {
        boolean hasAnyUnlocks() {
            return unlockBeaconPigeon
                    || unlockNullRockVulture
                    || unlockBeaconCrownMap
                    || unlockIroncladPelican
                    || unlockDockMap;
        }
    }

    WinnerMapProgressResult evaluateWinnerMapProgress(BirdGame3.MapType map, double winnerHealth,
                                                      int urbanKingProgress, int skyEmperorProgress,
                                                      int canopyKingProgress) {
        List<AchievementUnlock> unlocks = new ArrayList<>();
        boolean incrementCityWins = false;
        boolean incrementCliffWins = false;
        boolean incrementJungleWins = false;

        if (winnerHealth > 0.0 && winnerHealth < 20.0) {
            unlocks.add(achievementUnlock(BirdGame3Achievement.CLUTCH_GOD));
        }

        if (map == BirdGame3.MapType.CITY) {
            incrementCityWins = true;
            if (urbanKingProgress + 1 >= 5) {
                unlocks.add(achievementUnlock(BirdGame3Achievement.URBAN_KING));
            }
        }

        if (map == BirdGame3.MapType.SKYCLIFFS) {
            incrementCliffWins = true;
            if (skyEmperorProgress + 1 >= 5) {
                unlocks.add(achievementUnlock(BirdGame3Achievement.SKY_EMPEROR));
            }
        }

        if (map == BirdGame3.MapType.VIBRANT_JUNGLE) {
            incrementJungleWins = true;
            if (canopyKingProgress + 1 >= 5) {
                unlocks.add(achievementUnlock(BirdGame3Achievement.CANOPY_KING));
            }
        }

        return new WinnerMapProgressResult(incrementCityWins, incrementCliffWins, incrementJungleWins, unlocks);
    }

    ModeMilestoneProgress syncModeMilestones(int bossRushAchievementProgress, int towerDefenseAchievementProgress,
                                             int pigeonEpisodeAchievementProgress, int batEpisodeAchievementProgress,
                                             int pelicanEpisodeAchievementProgress, int bigForestBadgeAchievementProgress,
                                             int tournamentAchievementProgress, int bossRushClearCount,
                                             int towerDefenseBadgeCount, boolean pigeonEpisodeCompleted,
                                             boolean batEpisodeCompleted, boolean pelicanEpisodeCompleted,
                                             int bigForestBadgeCount, int tournamentChampionshipsWon) {
        return new ModeMilestoneProgress(
                Math.max(bossRushAchievementProgress, bossRushClearCount),
                Math.max(towerDefenseAchievementProgress, towerDefenseBadgeCount > 0 ? 1 : 0),
                Math.max(pigeonEpisodeAchievementProgress, pigeonEpisodeCompleted ? 1 : 0),
                Math.max(batEpisodeAchievementProgress, batEpisodeCompleted ? 1 : 0),
                Math.max(pelicanEpisodeAchievementProgress, pelicanEpisodeCompleted ? 1 : 0),
                Math.max(bigForestBadgeAchievementProgress, bigForestBadgeCount),
                Math.max(tournamentAchievementProgress, tournamentChampionshipsWon > 0 ? 1 : 0)
        );
    }

    List<AchievementUnlock> evaluateModeAchievementUnlocks(ModeMilestoneProgress milestones, int bigForestBadgeGoal) {
        List<AchievementUnlock> unlocks = new ArrayList<>();
        if (milestones.towerDefenseBadgeProgress() >= 1) {
            unlocks.add(achievementUnlock(BirdGame3Achievement.GROVE_SENTINEL));
        }
        if (milestones.bigForestBadgeProgress() >= bigForestBadgeGoal) {
            unlocks.add(achievementUnlock(BirdGame3Achievement.BLIGHT_BUSTER));
        }
        if (milestones.tournamentProgress() >= 1) {
            unlocks.add(achievementUnlock(BirdGame3Achievement.BRACKET_BOSS));
        }
        return unlocks;
    }

    List<AchievementUnlock> reconcileStoredAchievementUnlocks(StoredAchievementContext context) {
        List<AchievementUnlock> unlocks = new ArrayList<>();
        if (progressAtLeast(context.achievementProfile(), BirdGame3Achievement.LEAN_GOD, 1800)) unlocks.add(achievementUnlock(BirdGame3Achievement.LEAN_GOD));
        if (progressAtLeast(context.achievementProfile(), BirdGame3Achievement.LOUNGE_LIZARD, 1000)) unlocks.add(achievementUnlock(BirdGame3Achievement.LOUNGE_LIZARD));
        if (progressAtLeast(context.achievementProfile(), BirdGame3Achievement.FALL_GUY, 3)) unlocks.add(achievementUnlock(BirdGame3Achievement.FALL_GUY));
        if (progressAtLeast(context.achievementProfile(), BirdGame3Achievement.TAUNT_LORD, 10)) unlocks.add(achievementUnlock(BirdGame3Achievement.TAUNT_LORD));
        if (progressAtLeast(context.achievementProfile(), BirdGame3Achievement.ROOFTOP_RUNNER, 20)) unlocks.add(achievementUnlock(BirdGame3Achievement.ROOFTOP_RUNNER));
        if (progressAtLeast(context.achievementProfile(), BirdGame3Achievement.NEON_ADDICT, 8)) unlocks.add(achievementUnlock(BirdGame3Achievement.NEON_ADDICT));
        if (progressAtLeast(context.achievementProfile(), BirdGame3Achievement.URBAN_KING, 5)) unlocks.add(achievementUnlock(BirdGame3Achievement.URBAN_KING));
        if (progressAtLeast(context.achievementProfile(), BirdGame3Achievement.THERMAL_RIDER, 10)) unlocks.add(achievementUnlock(BirdGame3Achievement.THERMAL_RIDER));
        if (progressAtLeast(context.achievementProfile(), BirdGame3Achievement.CLIFF_DIVER, 15)) unlocks.add(achievementUnlock(BirdGame3Achievement.CLIFF_DIVER));
        if (progressAtLeast(context.achievementProfile(), BirdGame3Achievement.SKY_EMPEROR, 5)) unlocks.add(achievementUnlock(BirdGame3Achievement.SKY_EMPEROR));
        if (progressAtLeast(context.achievementProfile(), BirdGame3Achievement.VINE_SWINGER, 8)) unlocks.add(achievementUnlock(BirdGame3Achievement.VINE_SWINGER));
        if (progressAtLeast(context.achievementProfile(), BirdGame3Achievement.CANOPY_KING, 5)) unlocks.add(achievementUnlock(BirdGame3Achievement.CANOPY_KING));
        if (progressAtLeast(context.achievementProfile(), BirdGame3Achievement.PELICAN_KING, 15)) unlocks.add(achievementUnlock(BirdGame3Achievement.PELICAN_KING));
        if (countCompleted(context.classicCompleted()) > 0) unlocks.add(achievementUnlock(BirdGame3Achievement.CLASSIC_CREST));
        if (isAdventureChapterTwoComplete(context.completedAdventure())) unlocks.add(achievementUnlock(BirdGame3Achievement.ECHOES_BELOW));
        if (isAdventureFullyComplete(context.completedAdventure())) unlocks.add(achievementUnlock(BirdGame3Achievement.STORY_KEEPER));
        if (progressAtLeast(context.achievementProfile(), BirdGame3Achievement.BOSS_BREAKER, 1) || context.bossRushClearCount() > 0) {
            unlocks.add(achievementUnlock(BirdGame3Achievement.BOSS_BREAKER));
        }
        if (progressAtLeast(context.achievementProfile(), BirdGame3Achievement.CROWN_UNBROKEN, 1)) unlocks.add(achievementUnlock(BirdGame3Achievement.CROWN_UNBROKEN));
        if (progressAtLeast(context.achievementProfile(), BirdGame3Achievement.GROVE_SENTINEL, 1) || context.towerDefenseBadgeCount() > 0) {
            unlocks.add(achievementUnlock(BirdGame3Achievement.GROVE_SENTINEL));
        }
        if (progressAtLeast(context.achievementProfile(), BirdGame3Achievement.ROOFTOP_LEGACY, 1) || context.pigeonEpisodeCompleted()) {
            unlocks.add(achievementUnlock(BirdGame3Achievement.ROOFTOP_LEGACY));
        }
        if (progressAtLeast(context.achievementProfile(), BirdGame3Achievement.ECHO_SOVEREIGN, 1) || context.batEpisodeCompleted()) {
            unlocks.add(achievementUnlock(BirdGame3Achievement.ECHO_SOVEREIGN));
        }
        if (progressAtLeast(context.achievementProfile(), BirdGame3Achievement.IRON_TEMPEST, 1) || context.pelicanEpisodeCompleted()) {
            unlocks.add(achievementUnlock(BirdGame3Achievement.IRON_TEMPEST));
        }
        if (progressAtLeast(context.achievementProfile(), BirdGame3Achievement.BLIGHT_BUSTER, context.bigForestBadgeGoal())
                || context.bigForestBadgeCount() >= context.bigForestBadgeGoal()) {
            unlocks.add(achievementUnlock(BirdGame3Achievement.BLIGHT_BUSTER));
        }
        if (progressAtLeast(context.achievementProfile(), BirdGame3Achievement.BRACKET_BOSS, 1) || context.tournamentChampionshipsWon() > 0) {
            unlocks.add(achievementUnlock(BirdGame3Achievement.BRACKET_BOSS));
        }
        return unlocks;
    }

    List<AchievementUnlock> evaluateAdventureCompletionAchievements(boolean[] completedAdventure) {
        List<AchievementUnlock> unlocks = new ArrayList<>();
        if (isAdventureChapterTwoComplete(completedAdventure)) {
            unlocks.add(achievementUnlock(BirdGame3Achievement.ECHOES_BELOW));
        }
        if (isAdventureFullyComplete(completedAdventure)) {
            unlocks.add(achievementUnlock(BirdGame3Achievement.STORY_KEEPER));
        }
        return unlocks;
    }

    List<AchievementUnlock> evaluateClassicCompletionAchievements(boolean[] classicCompleted) {
        if (countCompleted(classicCompleted) <= 0) {
            return List.of();
        }
        return List.of(achievementUnlock(BirdGame3Achievement.CLASSIC_CREST));
    }

    AdventureChapterRewards evaluateAdventureChapterRewards(boolean tempestRoute, int chapterIndex,
                                                            int beaconChapterIndex, int unitedFinaleChapterIndex,
                                                            int tempestFinalChapterIndex,
                                                            boolean beaconPigeonUnlocked,
                                                            boolean nullRockVultureUnlocked,
                                                            boolean beaconCrownMapUnlocked,
                                                            boolean ironcladPelicanUnlocked,
                                                            boolean dockMapUnlocked) {
        boolean unlockBeaconPigeon = !tempestRoute
                && chapterIndex == beaconChapterIndex
                && !beaconPigeonUnlocked;
        boolean unlockNullRockVulture = !tempestRoute
                && chapterIndex == unitedFinaleChapterIndex
                && !nullRockVultureUnlocked;
        boolean unlockBeaconCrownMap = !tempestRoute
                && chapterIndex == unitedFinaleChapterIndex
                && !beaconCrownMapUnlocked;
        boolean unlockIroncladPelican = tempestRoute
                && chapterIndex == tempestFinalChapterIndex
                && !ironcladPelicanUnlocked;
        boolean unlockDockMap = tempestRoute
                && chapterIndex == tempestFinalChapterIndex
                && !dockMapUnlocked;
        return new AdventureChapterRewards(
                unlockBeaconPigeon,
                unlockNullRockVulture,
                unlockBeaconCrownMap,
                unlockIroncladPelican,
                unlockDockMap
        );
    }

    private AchievementUnlock achievementUnlock(BirdGame3Achievement achievement) {
        return new AchievementUnlock(achievement, defaultAchievementTitle(achievement));
    }

    private String defaultAchievementTitle(BirdGame3Achievement achievement) {
        if (achievement == null) {
            return "NEW ACHIEVEMENT";
        }
        return achievement.title();
    }

    private boolean progressAtLeast(BirdGame3AchievementProfile profile, BirdGame3Achievement achievement, int threshold) {
        return profile != null && profile.progress(achievement) >= threshold;
    }

    private boolean isAdventureChapterTwoComplete(boolean[] completedAdventure) {
        return completedAdventure != null && completedAdventure.length >= 2 && completedAdventure[1];
    }

    private boolean isAdventureFullyComplete(boolean[] completedAdventure) {
        return completedAdventure != null
                && completedAdventure.length > 0
                && countCompleted(completedAdventure) == completedAdventure.length;
    }

    private int countCompleted(boolean[] completed) {
        if (completed == null) {
            return 0;
        }
        int total = 0;
        for (boolean value : completed) {
            if (value) {
                total++;
            }
        }
        return total;
    }
}
