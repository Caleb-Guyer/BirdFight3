package com.example.birdgame3;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

import static org.junit.jupiter.api.Assertions.*;

class StoryCampaignProgressTest {
    private Preferences prefs;
    private StoryCampaign campaign;

    @BeforeEach
    void setUp() {
        prefs = Preferences.userRoot().node("/birdfight3-still-sky-tests/" + UUID.randomUUID());
        campaign = StoryCampaignContent.create();
    }

    @AfterEach
    void tearDown() throws BackingStoreException {
        prefs.removeNode();
        prefs.flush();
    }

    @Test
    void legacyProfileStartsNewCampaignAtActOneWithoutChangingLegacyKeys() {
        prefs.putInt("adv_ch1_progress", 7);
        prefs.putBoolean("adv_ch1_done", true);
        prefs.putInt("ep_pigeon_unlocked", 4);
        prefs.putBoolean("ep_pigeon_completed", true);

        StoryCampaignProgress progress = StoryCampaignProgress.load(prefs, campaign);
        progress.saveTo(prefs);

        assertEquals("dead_air", progress.currentMissionId);
        assertEquals(StoryCampaign.Difficulty.NORMAL, progress.difficulty);
        assertEquals(0, progress.completedCount());
        assertEquals(7, prefs.getInt("adv_ch1_progress", -1));
        assertTrue(prefs.getBoolean("adv_ch1_done", false));
        assertEquals(4, prefs.getInt("ep_pigeon_unlocked", -1));
        assertTrue(prefs.getBoolean("ep_pigeon_completed", false));
    }

    @Test
    void firstClearRewardsAndMissionAdvanceAreIdempotent() {
        StoryCampaignProgress progress = new StoryCampaignProgress();
        StoryCampaign.Mission first = campaign.firstMission();

        assertTrue(progress.markMissionCompleted(campaign, first));
        assertFalse(progress.markMissionCompleted(campaign, first));
        assertEquals(1, progress.completedCount());
        assertEquals(campaign.nextMission(first.id()).id(), progress.currentMissionId);
    }

    @Test
    void savesStableIdsDifficultyScenesAndRecruitment() {
        StoryCampaignProgress progress = new StoryCampaignProgress();
        StoryCampaign.Mission first = campaign.firstMission();
        StoryCampaign.Mission penguinRecruit = campaign.mission("last_thermal");
        progress.markSceneSeen(first.preSceneId());
        progress.markMissionCompleted(campaign, first);
        progress.markMissionCompleted(campaign, penguinRecruit);
        progress.difficulty = StoryCampaign.Difficulty.HARD;
        progress.saveTo(prefs);

        StoryCampaignProgress loaded = StoryCampaignProgress.load(prefs, campaign);
        assertTrue(loaded.hasSeenScene(first.preSceneId()));
        assertTrue(loaded.isMissionCompleted(first.id()));
        assertTrue(loaded.isRecruited(BirdGame3.BirdType.PENGUIN));
        assertEquals(StoryCampaign.Difficulty.HARD, loaded.difficulty);
    }

    @Test
    void resetTouchesOnlyTheNewCampaignNamespace() {
        prefs.put("adv_route_selected", "TEMPEST");
        StoryCampaignProgress progress = new StoryCampaignProgress();
        progress.markMissionCompleted(campaign, campaign.firstMission());
        progress.difficulty = StoryCampaign.Difficulty.HARD;
        progress.reset(campaign);
        progress.saveTo(prefs);

        assertEquals("dead_air", progress.currentMissionId);
        assertEquals(StoryCampaign.Difficulty.NORMAL, progress.difficulty);
        assertEquals(0, progress.completedCount());
        assertEquals("TEMPEST", prefs.get("adv_route_selected", ""));
    }
}
