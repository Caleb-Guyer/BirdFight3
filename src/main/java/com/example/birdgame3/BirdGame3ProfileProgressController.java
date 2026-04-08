package com.example.birdgame3;

import java.util.function.IntConsumer;

final class BirdGame3ProfileProgressController {
    private static final String NOIR_PIGEON_SKIN = "NOIR_PIGEON";
    private static final String SKY_KING_EAGLE_SKIN = "SKY_KING_EAGLE";

    private final BirdGame3 game;
    private final BirdGame3ProgressionService progressionService;

    BirdGame3ProfileProgressController(BirdGame3 game, BirdGame3ProgressionService progressionService) {
        this.game = game;
        this.progressionService = progressionService;
    }

    void reconcileLoadedProgress(boolean unitedFinaleCompleted, boolean tempestFinaleCompleted) {
        if (game.isEpisodeCompletedForBird(BirdGame3.BirdType.PIGEON)) {
            game.setEpisodeUnlockedChaptersForBird(
                    BirdGame3.BirdType.PIGEON,
                    game.episodeChapterCountForBird(BirdGame3.BirdType.PIGEON)
            );
            game.setNoirPigeonUnlocked();
        }
        if (game.isEpisodeCompletedForBird(BirdGame3.BirdType.BAT)) {
            game.setEpisodeUnlockedChaptersForBird(
                    BirdGame3.BirdType.BAT,
                    game.episodeChapterCountForBird(BirdGame3.BirdType.BAT)
            );
            game.setBatUnlocked();
        }
        if (game.isEpisodeCompletedForBird(BirdGame3.BirdType.PELICAN)) {
            game.setEpisodeUnlockedChaptersForBird(
                    BirdGame3.BirdType.PELICAN,
                    game.episodeChapterCountForBird(BirdGame3.BirdType.PELICAN)
            );
            game.setEagleSkinUnlocked();
            game.setBatUnlocked();
        }
        if (unitedFinaleCompleted) {
            game.unlockNullRockVultureReward(false);
            game.unlockBeaconCrownMapReward(false);
        }
        if (tempestFinaleCompleted) {
            game.unlockIroncladPelicanReward(false);
            game.unlockDockMapReward(false);
        }
        if (game.isClassicCompleted(BirdGame3.BirdType.HUMMINGBIRD)) {
            game.setTitmouseUnlocked();
        }
        game.setEagleSkinUnlocked();
        game.setClassicSkinUnlocked(BirdGame3.BirdType.EAGLE, true);
        game.setClassicSkinUnlocked(BirdGame3.BirdType.PIGEON, game.isNoirPigeonUnlocked());
    }

    void applyAdventureBattleUnlock(BirdGame3.BirdType unlockReward) {
        if (unlockReward == null || game.isAdventureBirdAvailableForCurrentRoute(unlockReward)) {
            return;
        }
        game.unlockAdventureBirdForCurrentRoute(unlockReward);
        game.queueBirdUnlockCard(unlockReward);
        game.requestProgressSave();
    }

    void onAdventureChapterCompleted(boolean tempestRoute, int chapterIndex, int beaconChapterIndex,
                                     int unitedFinaleChapterIndex, int tempestFinalChapterIndex,
                                     boolean beaconPigeonUnlocked, boolean nullRockVultureUnlocked,
                                     boolean beaconCrownMapUnlocked, boolean ironcladPelicanUnlocked,
                                     boolean dockMapUnlocked) {
        BirdGame3ProgressionService.AdventureChapterRewards rewards =
                progressionService.evaluateAdventureChapterRewards(
                        tempestRoute,
                        chapterIndex,
                        beaconChapterIndex,
                        unitedFinaleChapterIndex,
                        tempestFinalChapterIndex,
                        beaconPigeonUnlocked,
                        nullRockVultureUnlocked,
                        beaconCrownMapUnlocked,
                        ironcladPelicanUnlocked,
                        dockMapUnlocked
                );
        boolean changed = false;
        if (rewards.unlockBeaconPigeon()) {
            changed |= game.unlockBeaconPigeonReward();
        }
        if (rewards.unlockNullRockVulture()) {
            changed |= game.unlockNullRockVultureReward(true);
        }
        if (rewards.unlockBeaconCrownMap()) {
            changed |= game.unlockBeaconCrownMapReward(true);
        }
        if (rewards.unlockIroncladPelican()) {
            changed |= game.unlockIroncladPelicanReward(true);
        }
        if (rewards.unlockDockMap()) {
            changed |= game.unlockDockMapReward(true);
        }
        if (changed) {
            game.requestProgressSave();
        }
    }

    void onClassicRunCompleted(BirdGame3.BirdType selectedBird, Runnable afterCompletion) {
        if (selectedBird == null) {
            return;
        }
        game.setClassicCompleted(selectedBird);
        if (afterCompletion != null) {
            afterCompletion.run();
        }
        unlockClassicReward(selectedBird);
        if (selectedBird == BirdGame3.BirdType.HUMMINGBIRD && !game.isTitmouseUnlocked()) {
            game.setTitmouseUnlocked();
            game.queueBirdUnlockCard(BirdGame3.BirdType.TITMOUSE);
        }
        game.requestProgressSave();
    }

    boolean onEpisodeCompleted(BirdGame3.BirdType playerType, Runnable afterCompletion) {
        if (playerType == null || game.isEpisodeCompletedForBird(playerType)) {
            return false;
        }
        game.setEpisodeCompletedForBird(playerType);
        if (afterCompletion != null) {
            afterCompletion.run();
        }
        game.requestProgressSave();
        return true;
    }

    int onBossRushCompleted(BirdGame3.BirdType selectedBird, String rank, long elapsedMillis,
                            boolean perfectRouteCompleted, IntConsumer afterClearCountUpdated) {
        int clearCount = game.incrementBossRushClearCount();
        if (afterClearCountUpdated != null) {
            afterClearCountUpdated.accept(clearCount);
        }
        game.recordBossRushProfileCompletion(selectedBird, rank, elapsedMillis, perfectRouteCompleted);
        game.requestProgressSave();
        return clearCount;
    }

    void onTournamentChampionshipWon(Runnable afterIncrement) {
        game.incrementTournamentChampionshipsWon();
        if (afterIncrement != null) {
            afterIncrement.run();
        }
        game.requestProgressSave();
    }

    private void unlockClassicReward(BirdGame3.BirdType type) {
        if (type == null || game.isClassicRewardUnlocked(type)) {
            return;
        }
        if (type == BirdGame3.BirdType.PIGEON) {
            game.setNoirPigeonUnlocked();
            game.setClassicSkinUnlocked(type, true);
            game.queueSkinUnlockCard(BirdGame3.BirdType.PIGEON, NOIR_PIGEON_SKIN);
            return;
        }
        if (type == BirdGame3.BirdType.EAGLE) {
            game.setEagleSkinUnlocked();
            game.setClassicSkinUnlocked(type, true);
            game.queueSkinUnlockCard(BirdGame3.BirdType.EAGLE, SKY_KING_EAGLE_SKIN);
            return;
        }
        game.setClassicSkinUnlocked(type, true);
        game.queueSkinUnlockCard(type, classicSkinDataKey(type));
    }

    private String classicSkinDataKey(BirdGame3.BirdType type) {
        return "CLASSIC_SKIN_" + type.name();
    }
}
