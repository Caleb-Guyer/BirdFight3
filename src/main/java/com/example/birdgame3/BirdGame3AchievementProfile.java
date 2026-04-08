package com.example.birdgame3;

import java.util.EnumMap;

final class BirdGame3AchievementProfile {
    private final EnumMap<BirdGame3Achievement, BirdGame3AchievementEntry> entries =
            new EnumMap<>(BirdGame3Achievement.class);

    BirdGame3AchievementProfile() {
        for (BirdGame3Achievement achievement : BirdGame3Achievement.values()) {
            entries.put(achievement, new BirdGame3AchievementEntry());
        }
    }

    BirdGame3AchievementProfile copy() {
        BirdGame3AchievementProfile copy = new BirdGame3AchievementProfile();
        for (BirdGame3Achievement achievement : BirdGame3Achievement.values()) {
            copy.entries.put(achievement, entry(achievement).copy());
        }
        return copy;
    }

    BirdGame3AchievementEntry entry(BirdGame3Achievement achievement) {
        return entries.get(achievement);
    }

    boolean isUnlocked(BirdGame3Achievement achievement) {
        return entry(achievement).unlocked();
    }

    boolean isUnlocked(int legacyIndex) {
        return isUnlocked(BirdGame3Achievement.fromLegacyIndex(legacyIndex));
    }

    void setUnlocked(BirdGame3Achievement achievement, boolean unlocked) {
        entry(achievement).setUnlocked(unlocked);
    }

    void setUnlocked(int legacyIndex, boolean unlocked) {
        setUnlocked(BirdGame3Achievement.fromLegacyIndex(legacyIndex), unlocked);
    }

    int progress(BirdGame3Achievement achievement) {
        return entry(achievement).progress();
    }

    int progress(int legacyIndex) {
        return progress(BirdGame3Achievement.fromLegacyIndex(legacyIndex));
    }

    void setProgress(BirdGame3Achievement achievement, int progress) {
        entry(achievement).setProgress(progress);
    }

    void setProgress(int legacyIndex, int progress) {
        setProgress(BirdGame3Achievement.fromLegacyIndex(legacyIndex), progress);
    }

    int addProgress(BirdGame3Achievement achievement, int delta) {
        return entry(achievement).addProgress(delta);
    }

    int addProgress(int legacyIndex, int delta) {
        return addProgress(BirdGame3Achievement.fromLegacyIndex(legacyIndex), delta);
    }

    void maxProgress(BirdGame3Achievement achievement, int candidate) {
        entry(achievement).maxProgress(candidate);
    }

    void maxProgress(int legacyIndex, int candidate) {
        maxProgress(BirdGame3Achievement.fromLegacyIndex(legacyIndex), candidate);
    }

    boolean isRewardClaimed(BirdGame3Achievement achievement) {
        return entry(achievement).rewardClaimed();
    }

    boolean isRewardClaimed(int legacyIndex) {
        return isRewardClaimed(BirdGame3Achievement.fromLegacyIndex(legacyIndex));
    }

    void setRewardClaimed(BirdGame3Achievement achievement, boolean claimed) {
        entry(achievement).setRewardClaimed(claimed);
    }

    void setRewardClaimed(int legacyIndex, boolean claimed) {
        setRewardClaimed(BirdGame3Achievement.fromLegacyIndex(legacyIndex), claimed);
    }

    int unlockedCount() {
        int count = 0;
        for (BirdGame3Achievement achievement : BirdGame3Achievement.values()) {
            if (isUnlocked(achievement)) {
                count++;
            }
        }
        return count;
    }
}
