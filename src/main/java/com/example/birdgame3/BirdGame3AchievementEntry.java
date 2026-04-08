package com.example.birdgame3;

final class BirdGame3AchievementEntry {
    private boolean unlocked;
    private int progress;
    private boolean rewardClaimed;

    BirdGame3AchievementEntry copy() {
        BirdGame3AchievementEntry copy = new BirdGame3AchievementEntry();
        copy.unlocked = unlocked;
        copy.progress = progress;
        copy.rewardClaimed = rewardClaimed;
        return copy;
    }

    boolean unlocked() {
        return unlocked;
    }

    void setUnlocked(boolean unlocked) {
        this.unlocked = unlocked;
    }

    int progress() {
        return progress;
    }

    void setProgress(int progress) {
        this.progress = Math.max(0, progress);
    }

    int addProgress(int delta) {
        setProgress(progress + delta);
        return progress;
    }

    void maxProgress(int candidate) {
        if (candidate > progress) {
            progress = candidate;
        }
    }

    boolean rewardClaimed() {
        return rewardClaimed;
    }

    void setRewardClaimed(boolean rewardClaimed) {
        this.rewardClaimed = rewardClaimed;
    }
}
