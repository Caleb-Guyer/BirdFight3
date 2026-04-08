package com.example.birdgame3;

import java.util.List;

final class BirdGame3AchievementEvaluator {
    private final BirdGame3 game;
    private final BirdGame3ProgressionService progressionService;

    BirdGame3AchievementEvaluator(BirdGame3 game, BirdGame3ProgressionService progressionService) {
        this.game = game;
        this.progressionService = progressionService;
    }

    void reconcileStoredProgress(boolean[] classicCompleted, boolean[] completedAdventure, int bossRushClearCount,
                                 int towerDefenseBadgeCount, boolean pigeonEpisodeCompleted,
                                 boolean batEpisodeCompleted, boolean pelicanEpisodeCompleted,
                                 int bigForestBadgeCount, int bigForestBadgeGoal,
                                 int tournamentChampionshipsWon) {
        applyUnlocksSilently(
                progressionService.reconcileStoredAchievementUnlocks(
                        new BirdGame3ProgressionService.StoredAchievementContext(
                                game.achievementProfileState(),
                                classicCompleted,
                                completedAdventure,
                                bossRushClearCount,
                                towerDefenseBadgeCount,
                                pigeonEpisodeCompleted,
                                batEpisodeCompleted,
                                pelicanEpisodeCompleted,
                                bigForestBadgeCount,
                                bigForestBadgeGoal,
                                tournamentChampionshipsWon
                        )
                )
        );
    }

    void normalizeLoadedProfileState(int[] cityWins, int[] cliffWins, int[] jungleWins,
                                     int loadedAchievementSchemaVersion, int currentAchievementSchemaVersion,
                                     boolean[] classicCompleted, boolean[] completedAdventure,
                                     int bossRushClearCount, int towerDefenseBadgeCount,
                                     boolean pigeonEpisodeCompleted, boolean batEpisodeCompleted,
                                     boolean pelicanEpisodeCompleted, int bigForestBadgeCount,
                                     int bigForestBadgeGoal, int tournamentChampionshipsWon) {
        game.maxAchievementProgress(BirdGame3Achievement.URBAN_KING, sumProgress(cityWins));
        game.maxAchievementProgress(BirdGame3Achievement.SKY_EMPEROR, sumProgress(cliffWins));
        game.maxAchievementProgress(BirdGame3Achievement.CANOPY_KING, sumProgress(jungleWins));
        if (loadedAchievementSchemaVersion < currentAchievementSchemaVersion) {
            game.setAchievementUnlocked(BirdGame3Achievement.GROVE_SENTINEL, false);
            game.setAchievementProgressValue(BirdGame3Achievement.GROVE_SENTINEL, 0);
            game.setAchievementRewardClaimed(BirdGame3Achievement.GROVE_SENTINEL);
        }
        syncModeMilestones(
                bossRushClearCount,
                towerDefenseBadgeCount,
                pigeonEpisodeCompleted,
                batEpisodeCompleted,
                pelicanEpisodeCompleted,
                bigForestBadgeCount,
                tournamentChampionshipsWon
        );
        reconcileStoredProgress(
                classicCompleted,
                completedAdventure,
                bossRushClearCount,
                towerDefenseBadgeCount,
                pigeonEpisodeCompleted,
                batEpisodeCompleted,
                pelicanEpisodeCompleted,
                bigForestBadgeCount,
                bigForestBadgeGoal,
                tournamentChampionshipsWon
        );
        clearRewardClaimsForLockedAchievements();
    }

    void syncModeMilestones(int bossRushClearCount, int towerDefenseBadgeCount,
                            boolean pigeonEpisodeCompleted, boolean batEpisodeCompleted,
                            boolean pelicanEpisodeCompleted, int bigForestBadgeCount,
                            int tournamentChampionshipsWon) {
        BirdGame3ProgressionService.ModeMilestoneProgress milestones =
                progressionService.syncModeMilestones(
                        game.achievementProgressValue(BirdGame3Achievement.BOSS_BREAKER),
                        game.achievementProgressValue(BirdGame3Achievement.GROVE_SENTINEL),
                        game.achievementProgressValue(BirdGame3Achievement.ROOFTOP_LEGACY),
                        game.achievementProgressValue(BirdGame3Achievement.ECHO_SOVEREIGN),
                        game.achievementProgressValue(BirdGame3Achievement.IRON_TEMPEST),
                        game.achievementProgressValue(BirdGame3Achievement.BLIGHT_BUSTER),
                        game.achievementProgressValue(BirdGame3Achievement.BRACKET_BOSS),
                        bossRushClearCount,
                        towerDefenseBadgeCount,
                        pigeonEpisodeCompleted,
                        batEpisodeCompleted,
                        pelicanEpisodeCompleted,
                        bigForestBadgeCount,
                        tournamentChampionshipsWon
                );
        game.setAchievementProgressValue(BirdGame3Achievement.BOSS_BREAKER, milestones.bossRushClearProgress());
        game.setAchievementProgressValue(BirdGame3Achievement.GROVE_SENTINEL, milestones.towerDefenseBadgeProgress());
        game.setAchievementProgressValue(BirdGame3Achievement.ROOFTOP_LEGACY, milestones.pigeonEpisodeProgress());
        game.setAchievementProgressValue(BirdGame3Achievement.ECHO_SOVEREIGN, milestones.batEpisodeProgress());
        game.setAchievementProgressValue(BirdGame3Achievement.IRON_TEMPEST, milestones.pelicanEpisodeProgress());
        game.setAchievementProgressValue(BirdGame3Achievement.BLIGHT_BUSTER, milestones.bigForestBadgeProgress());
        game.setAchievementProgressValue(BirdGame3Achievement.BRACKET_BOSS, milestones.tournamentProgress());
    }

    void refreshModeAchievementUnlocks(int bossRushClearCount, int towerDefenseBadgeCount,
                                       boolean pigeonEpisodeCompleted, boolean batEpisodeCompleted,
                                       boolean pelicanEpisodeCompleted, int bigForestBadgeCount,
                                       int bigForestBadgeGoal, int tournamentChampionshipsWon) {
        syncModeMilestones(
                bossRushClearCount,
                towerDefenseBadgeCount,
                pigeonEpisodeCompleted,
                batEpisodeCompleted,
                pelicanEpisodeCompleted,
                bigForestBadgeCount,
                tournamentChampionshipsWon
        );
        BirdGame3ProgressionService.ModeMilestoneProgress milestones = new BirdGame3ProgressionService.ModeMilestoneProgress(
                game.achievementProgressValue(BirdGame3Achievement.BOSS_BREAKER),
                game.achievementProgressValue(BirdGame3Achievement.GROVE_SENTINEL),
                game.achievementProgressValue(BirdGame3Achievement.ROOFTOP_LEGACY),
                game.achievementProgressValue(BirdGame3Achievement.ECHO_SOVEREIGN),
                game.achievementProgressValue(BirdGame3Achievement.IRON_TEMPEST),
                game.achievementProgressValue(BirdGame3Achievement.BLIGHT_BUSTER),
                game.achievementProgressValue(BirdGame3Achievement.BRACKET_BOSS)
        );
        applyUnlocks(progressionService.evaluateModeAchievementUnlocks(milestones, bigForestBadgeGoal));
    }

    void onBossRushCompleted(int bossRushClearCount, boolean exCleared) {
        game.maxAchievementProgress(BirdGame3Achievement.BOSS_BREAKER, bossRushClearCount);
        unlockIfNeeded(BirdGame3Achievement.BOSS_BREAKER);
        if (exCleared) {
            game.maxAchievementProgress(BirdGame3Achievement.CROWN_UNBROKEN, 1);
            unlockIfNeeded(BirdGame3Achievement.CROWN_UNBROKEN);
        }
    }

    void onClassicRunCompleted() {
        unlockIfNeeded(BirdGame3Achievement.CLASSIC_CREST);
    }

    void onEpisodeCompleted(BirdGame3.BirdType playerType) {
        if (playerType == null) {
            return;
        }
        switch (playerType) {
            case PIGEON -> completeEpisodeAchievement(BirdGame3Achievement.ROOFTOP_LEGACY);
            case BAT -> completeEpisodeAchievement(BirdGame3Achievement.ECHO_SOVEREIGN);
            case PELICAN -> completeEpisodeAchievement(BirdGame3Achievement.IRON_TEMPEST);
            default -> {
            }
        }
    }

    void onAdventureProgressUpdated(boolean[] completedAdventure) {
        applyUnlocks(progressionService.evaluateAdventureCompletionAchievements(completedAdventure));
    }

    void onClassicProgressUpdated(boolean[] classicCompleted) {
        applyUnlocks(progressionService.evaluateClassicCompletionAchievements(classicCompleted));
    }

    void onMatchWinner(Bird winner, BirdGame3.MapType map, boolean trainingModeActive) {
        if (winner == null || trainingModeActive) {
            return;
        }
        BirdGame3ProgressionService.WinnerMapProgressResult result =
                progressionService.evaluateWinnerMapProgress(
                        map,
                        winner.health,
                        game.achievementProgressValue(BirdGame3Achievement.URBAN_KING),
                        game.achievementProgressValue(BirdGame3Achievement.SKY_EMPEROR),
                        game.achievementProgressValue(BirdGame3Achievement.CANOPY_KING)
                );
        if (result.incrementCityWins()) {
            game.cityWins[winner.playerIndex]++;
            game.incrementAchievementProgress(BirdGame3Achievement.URBAN_KING.legacyIndex);
        }
        if (result.incrementCliffWins()) {
            game.cliffWins[winner.playerIndex]++;
            game.incrementAchievementProgress(BirdGame3Achievement.SKY_EMPEROR.legacyIndex);
        }
        if (result.incrementJungleWins()) {
            game.jungleWins[winner.playerIndex]++;
            game.incrementAchievementProgress(BirdGame3Achievement.CANOPY_KING.legacyIndex);
        }
        applyUnlocks(result.unlocks());
    }

    void onCombatStatsUpdated(Bird bird) {
        if (bird == null) {
            return;
        }
        int idx = bird.playerIndex;
        if (game.eliminations[idx] >= 1) {
            unlockIfNeeded(BirdGame3Achievement.FIRST_BLOOD);
        }
        if (game.eliminations[idx] >= 3) {
            unlockIfNeeded(BirdGame3Achievement.DOMINATOR);
        }
        int annihilatorThreshold = Math.max(1, game.activePlayers - 1);
        if (game.eliminations[idx] >= annihilatorThreshold) {
            unlockIfNeeded(BirdGame3Achievement.ANNIHILATOR);
        }
        if (game.groundPounds[idx] >= 3) {
            unlockIfNeeded(BirdGame3Achievement.TURKEY_SLAM_MASTER);
        }
        if (game.achievementProgressValue(BirdGame3Achievement.TAUNT_LORD) >= 10) {
            unlockIfNeeded(BirdGame3Achievement.TAUNT_LORD);
        }
        if (bird.type == BirdGame3.BirdType.PELICAN
                && game.achievementProgressValue(BirdGame3Achievement.PELICAN_KING) >= 15) {
            unlockIfNeeded(BirdGame3Achievement.PELICAN_KING);
        }
    }

    void onLeanFrame(Bird bird) {
        if (bird == null || bird.type != BirdGame3.BirdType.OPIUMBIRD || game.isAchievementUnlocked(BirdGame3Achievement.LEAN_GOD)) {
            return;
        }
        if (game.incrementAchievementProgress(BirdGame3Achievement.LEAN_GOD.legacyIndex) >= 1800) {
            unlockIfNeeded(BirdGame3Achievement.LEAN_GOD);
        }
    }

    void onLoungeHealing(Bird bird, double healedAmount) {
        if (bird == null || bird.type != BirdGame3.BirdType.MOCKINGBIRD
                || healedAmount <= 0.0 || game.isAchievementUnlocked(BirdGame3Achievement.LOUNGE_LIZARD)) {
            return;
        }
        if (game.addAchievementProgress(
                BirdGame3Achievement.LOUNGE_LIZARD.legacyIndex,
                Math.max(1, (int) Math.round(healedAmount * 10.0))
        ) >= 1000) {
            unlockIfNeeded(BirdGame3Achievement.LOUNGE_LIZARD);
        }
    }

    void onPowerUpPickup(Bird bird) {
        if (bird == null) {
            return;
        }
        if (game.incrementPowerUpPickupMatchCount(bird.playerIndex) >= 10) {
            unlockIfNeeded(BirdGame3Achievement.POWER_UP_HOARDER);
        }
    }

    void onTaunt(Bird bird) {
        if (bird == null || game.isAchievementUnlocked(BirdGame3Achievement.TAUNT_LORD)) {
            return;
        }
        if (game.incrementAchievementProgress(BirdGame3Achievement.TAUNT_LORD.legacyIndex) >= 10) {
            unlockIfNeeded(BirdGame3Achievement.TAUNT_LORD);
        }
    }

    void onPelicanPlunge() {
        if (game.incrementAchievementProgress(BirdGame3Achievement.PELICAN_KING.legacyIndex) >= 15) {
            unlockIfNeeded(BirdGame3Achievement.PELICAN_KING);
        }
    }

    void onHighRooftopJump(int playerIndex) {
        if (playerIndex < 0 || playerIndex >= game.rooftopJumps.length) {
            return;
        }
        game.rooftopJumps[playerIndex]++;
        if (game.incrementAchievementProgress(BirdGame3Achievement.ROOFTOP_RUNNER.legacyIndex) >= 20) {
            unlockIfNeeded(BirdGame3Achievement.ROOFTOP_RUNNER);
        }
    }

    void onHighCliffJump(int playerIndex) {
        if (playerIndex < 0 || playerIndex >= game.highCliffJumps.length) {
            return;
        }
        game.highCliffJumps[playerIndex]++;
        if (game.incrementAchievementProgress(BirdGame3Achievement.CLIFF_DIVER.legacyIndex) >= 15) {
            unlockIfNeeded(BirdGame3Achievement.CLIFF_DIVER);
        }
    }

    void onStageFall(int playerIndex, boolean trainingModeActive) {
        if (trainingModeActive) {
            return;
        }
        if (game.incrementAchievementProgress(BirdGame3Achievement.FALL_GUY.legacyIndex) >= 3) {
            unlockIfNeeded(BirdGame3Achievement.FALL_GUY);
        }
    }

    void onNeonPickup(int playerIndex) {
        if (playerIndex < 0 || playerIndex >= game.neonPickups.length) {
            return;
        }
        game.neonPickups[playerIndex]++;
        if (game.incrementAchievementProgress(BirdGame3Achievement.NEON_ADDICT.legacyIndex) >= 8) {
            unlockIfNeeded(BirdGame3Achievement.NEON_ADDICT);
        }
    }

    void onThermalPickup(int playerIndex) {
        if (playerIndex < 0 || playerIndex >= game.thermalPickups.length) {
            return;
        }
        game.thermalPickups[playerIndex]++;
        if (game.incrementAchievementProgress(BirdGame3Achievement.THERMAL_RIDER.legacyIndex) >= 10) {
            unlockIfNeeded(BirdGame3Achievement.THERMAL_RIDER);
        }
    }

    void onVineGrapplePickup(int playerIndex) {
        if (playerIndex < 0 || playerIndex >= game.vineGrapplePickups.length) {
            return;
        }
        game.vineGrapplePickups[playerIndex]++;
        if (game.incrementAchievementProgress(BirdGame3Achievement.VINE_SWINGER.legacyIndex) >= 8) {
            unlockIfNeeded(BirdGame3Achievement.VINE_SWINGER);
        }
    }

    private void completeEpisodeAchievement(BirdGame3Achievement achievement) {
        game.maxAchievementProgress(achievement, 1);
        unlockIfNeeded(achievement);
    }

    private void unlockIfNeeded(BirdGame3Achievement achievement) {
        if (achievement == null || game.isAchievementUnlocked(achievement)) {
            return;
        }
        game.unlockAchievement(achievement, achievement.title());
    }

    private void applyUnlocks(List<BirdGame3ProgressionService.AchievementUnlock> unlocks) {
        for (BirdGame3ProgressionService.AchievementUnlock unlock : unlocks) {
            if (unlock == null) {
                continue;
            }
            if (!game.isAchievementUnlocked(unlock.achievement())) {
                game.unlockAchievement(unlock.achievement(), unlock.title());
            }
        }
    }

    private void applyUnlocksSilently(List<BirdGame3ProgressionService.AchievementUnlock> unlocks) {
        for (BirdGame3ProgressionService.AchievementUnlock unlock : unlocks) {
            if (unlock == null) {
                continue;
            }
            game.setAchievementUnlocked(unlock.achievement(), true);
        }
    }

    private void clearRewardClaimsForLockedAchievements() {
        for (BirdGame3Achievement achievement : BirdGame3Achievement.values()) {
            if (game.achievementProfileState().isRewardClaimed(achievement) && !game.isAchievementUnlocked(achievement)) {
                game.setAchievementRewardClaimed(achievement);
            }
        }
    }

    private int sumProgress(int[] values) {
        if (values == null) {
            return 0;
        }
        int total = 0;
        for (int value : values) {
            total += Math.max(0, value);
        }
        return total;
    }
}
