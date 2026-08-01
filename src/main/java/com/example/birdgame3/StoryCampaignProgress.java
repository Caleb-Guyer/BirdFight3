package com.example.birdgame3;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.prefs.Preferences;

/**
 * Save data for The Still Sky. All keys deliberately use a new namespace so
 * no legacy {@code adv_*} or {@code ep_*} progress can be reinterpreted.
 */
final class StoryCampaignProgress {
    static final String KEY_PREFIX = "story_v2_";
    private static final String KEY_VERSION = KEY_PREFIX + "version";
    private static final String KEY_CURRENT_MISSION = KEY_PREFIX + "current_mission";
    private static final String KEY_COMPLETED_MISSIONS = KEY_PREFIX + "completed_missions";
    private static final String KEY_SEEN_SCENES = KEY_PREFIX + "seen_scenes";
    private static final String KEY_RECRUITED_BIRDS = KEY_PREFIX + "recruited_birds";
    private static final String KEY_DIFFICULTY = KEY_PREFIX + "difficulty";
    private static final String KEY_COMPLETE = KEY_PREFIX + "complete";
    private static final String KEY_REWARD_CLAIMED = KEY_PREFIX + "reward_claimed";

    int version = StoryCampaign.CURRENT_VERSION;
    String currentMissionId = "dead_air";
    StoryCampaign.Difficulty difficulty = StoryCampaign.Difficulty.NORMAL;
    boolean campaignComplete;
    boolean completionRewardClaimed;
    private final LinkedHashSet<String> completedMissionIds = new LinkedHashSet<>();
    private final LinkedHashSet<String> seenSceneIds = new LinkedHashSet<>();
    private final LinkedHashSet<BirdGame3.BirdType> recruitedBirds = new LinkedHashSet<>();

    StoryCampaignProgress() {
        recruitedBirds.add(BirdGame3.BirdType.PIGEON);
        recruitedBirds.add(BirdGame3.BirdType.MOCKINGBIRD);
    }

    StoryCampaignProgress copy() {
        StoryCampaignProgress copy = new StoryCampaignProgress();
        copy.version = version;
        copy.currentMissionId = currentMissionId;
        copy.difficulty = difficulty;
        copy.campaignComplete = campaignComplete;
        copy.completionRewardClaimed = completionRewardClaimed;
        copy.completedMissionIds.clear();
        copy.completedMissionIds.addAll(completedMissionIds);
        copy.seenSceneIds.addAll(seenSceneIds);
        copy.recruitedBirds.clear();
        copy.recruitedBirds.addAll(recruitedBirds);
        return copy;
    }

    static StoryCampaignProgress load(Preferences prefs, StoryCampaign campaign) {
        StoryCampaignProgress progress = new StoryCampaignProgress();
        if (prefs == null) {
            return progress;
        }
        progress.version = Math.max(1, prefs.getInt(KEY_VERSION, StoryCampaign.CURRENT_VERSION));
        progress.currentMissionId = prefs.get(KEY_CURRENT_MISSION, campaign.firstMission().id());
        if (campaign.mission(progress.currentMissionId) == null) {
            progress.currentMissionId = campaign.firstMission().id();
        }
        progress.difficulty = StoryCampaign.Difficulty.fromName(
                prefs.get(KEY_DIFFICULTY, StoryCampaign.Difficulty.NORMAL.name()));
        progress.campaignComplete = prefs.getBoolean(KEY_COMPLETE, false);
        progress.completionRewardClaimed = prefs.getBoolean(KEY_REWARD_CLAIMED, false);
        progress.completedMissionIds.addAll(parseStableIds(
                prefs.get(KEY_COMPLETED_MISSIONS, ""), campaign.missions.keySet()));
        progress.seenSceneIds.addAll(parseStableIds(
                prefs.get(KEY_SEEN_SCENES, ""), allowedSceneIds(campaign)));
        progress.recruitedBirds.clear();
        progress.recruitedBirds.addAll(parseBirds(prefs.get(KEY_RECRUITED_BIRDS, "")));
        if (progress.recruitedBirds.isEmpty()) {
            progress.recruitedBirds.add(BirdGame3.BirdType.PIGEON);
            progress.recruitedBirds.add(BirdGame3.BirdType.MOCKINGBIRD);
        }
        progress.reconcile(campaign);
        return progress;
    }

    void saveTo(Preferences prefs) {
        if (prefs == null) {
            return;
        }
        prefs.putInt(KEY_VERSION, Math.max(1, version));
        prefs.put(KEY_CURRENT_MISSION, currentMissionId == null ? "dead_air" : currentMissionId);
        prefs.put(KEY_COMPLETED_MISSIONS, join(completedMissionIds));
        prefs.put(KEY_SEEN_SCENES, join(seenSceneIds));
        prefs.put(KEY_RECRUITED_BIRDS,
                recruitedBirds.stream().map(Enum::name).reduce((a, b) -> a + "," + b).orElse(""));
        prefs.put(KEY_DIFFICULTY, difficulty.name());
        prefs.putBoolean(KEY_COMPLETE, campaignComplete);
        prefs.putBoolean(KEY_REWARD_CLAIMED, completionRewardClaimed);
    }

    void reset(StoryCampaign campaign) {
        version = campaign.version;
        currentMissionId = campaign.firstMission().id();
        difficulty = StoryCampaign.Difficulty.NORMAL;
        campaignComplete = false;
        completionRewardClaimed = false;
        completedMissionIds.clear();
        seenSceneIds.clear();
        recruitedBirds.clear();
        recruitedBirds.add(BirdGame3.BirdType.PIGEON);
        recruitedBirds.add(BirdGame3.BirdType.MOCKINGBIRD);
    }

    boolean markMissionCompleted(StoryCampaign campaign, StoryCampaign.Mission mission) {
        if (mission == null || campaign.mission(mission.id()) == null) {
            return false;
        }
        boolean firstClear = completedMissionIds.add(mission.id());
        if (mission.recruit() != null) {
            recruitedBirds.add(mission.recruit());
        }
        StoryCampaign.Mission next = campaign.nextMission(mission.id());
        if (next == null) {
            currentMissionId = mission.id();
            campaignComplete = true;
        } else if (firstClear || campaign.mission(currentMissionId) == null) {
            currentMissionId = next.id();
        }
        return firstClear;
    }

    void markSceneSeen(String sceneId) {
        if (sceneId != null && !sceneId.isBlank()) {
            seenSceneIds.add(sceneId);
        }
    }

    boolean isMissionCompleted(String missionId) {
        return completedMissionIds.contains(missionId);
    }

    boolean hasSeenScene(String sceneId) {
        return seenSceneIds.contains(sceneId);
    }

    boolean isRecruited(BirdGame3.BirdType type) {
        return recruitedBirds.contains(type);
    }

    int completedCount() {
        return completedMissionIds.size();
    }

    Set<String> completedMissionIds() {
        return Collections.unmodifiableSet(completedMissionIds);
    }

    Set<String> seenSceneIds() {
        return Collections.unmodifiableSet(seenSceneIds);
    }

    Set<BirdGame3.BirdType> recruitedBirds() {
        return Collections.unmodifiableSet(recruitedBirds);
    }

    boolean isActAvailable(StoryCampaign campaign, int actIndex) {
        if (actIndex <= 0) {
            return true;
        }
        if (actIndex >= campaign.acts.size()) {
            return false;
        }
        StoryCampaign.Act prior = campaign.acts.get(actIndex - 1);
        return prior.missions().stream().allMatch(mission -> isMissionCompleted(mission.id()));
    }

    boolean isMissionSelectable(StoryCampaign campaign, StoryCampaign.Mission mission) {
        if (campaign == null || mission == null || campaign.mission(mission.id()) == null) {
            return false;
        }
        return isMissionCompleted(mission.id()) || mission.id().equals(currentMissionId);
    }

    private void reconcile(StoryCampaign campaign) {
        recruitedBirds.add(BirdGame3.BirdType.PIGEON);
        recruitedBirds.add(BirdGame3.BirdType.MOCKINGBIRD);
        for (StoryCampaign.Mission mission : campaign.orderedMissions) {
            if (completedMissionIds.contains(mission.id()) && mission.recruit() != null) {
                recruitedBirds.add(mission.recruit());
            }
        }
        if (completedMissionIds.size() == campaign.orderedMissions.size()) {
            campaignComplete = true;
            currentMissionId = campaign.orderedMissions.getLast().id();
        }
        if (hasStartedCampaign(campaign)) {
            // Older builds discarded special presentation IDs while loading.
            // Any real campaign progress proves the opening prologue was reached.
            seenSceneIds.add(StorybookPrologue.ID);
        }
        if (campaignComplete) {
            // Completed legacy Still Sky saves necessarily passed both ending presentations.
            seenSceneIds.add(StorybookPrologue.EPILOGUE_ID);
            seenSceneIds.add(StillSkyCreditsPlayer.ID);
        }
    }

    private boolean hasStartedCampaign(StoryCampaign campaign) {
        if (campaignComplete || !completedMissionIds.isEmpty()) {
            return true;
        }
        if (currentMissionId != null && !currentMissionId.equals(campaign.firstMission().id())) {
            return true;
        }
        return seenSceneIds.stream().anyMatch(campaign.scenes::containsKey);
    }

    private static List<String> parseStableIds(String encoded, Collection<String> allowed) {
        List<String> result = new ArrayList<>();
        if (encoded == null || encoded.isBlank()) {
            return result;
        }
        Set<String> accepted = Set.copyOf(allowed);
        for (String token : encoded.split(",")) {
            String id = token.trim();
            if (accepted.contains(id)) {
                result.add(id);
            }
        }
        return result;
    }

    private static Collection<String> allowedSceneIds(StoryCampaign campaign) {
        LinkedHashSet<String> ids = new LinkedHashSet<>(campaign.scenes.keySet());
        ids.add(StorybookPrologue.ID);
        ids.add(StorybookPrologue.EPILOGUE_ID);
        ids.add(StillSkyCreditsPlayer.ID);
        return ids;
    }

    private static List<BirdGame3.BirdType> parseBirds(String encoded) {
        List<BirdGame3.BirdType> result = new ArrayList<>();
        if (encoded == null || encoded.isBlank()) {
            return result;
        }
        for (String token : encoded.split(",")) {
            try {
                result.add(BirdGame3.BirdType.valueOf(token.trim()));
            } catch (IllegalArgumentException ignored) {
            }
        }
        return result;
    }

    private static String join(Collection<String> values) {
        return String.join(",", values);
    }
}
