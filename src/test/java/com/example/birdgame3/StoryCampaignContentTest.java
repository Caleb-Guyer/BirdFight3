package com.example.birdgame3;

import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class StoryCampaignContentTest {
    @Test
    void definitiveCampaignHasTheAuthoredShapeAndValidContent() {
        StoryCampaign campaign = StoryCampaignContent.create();
        StoryCampaign.ValidationReport report = campaign.validate();

        assertTrue(report.valid(), () -> String.join("\n", report.errors()));
        assertEquals(12, campaign.acts.size());
        assertEquals(40, campaign.orderedMissions.size());
        assertTrue(report.dialogueLineCount() >= 700);
        assertTrue(report.dialogueLineCount() <= 900);
        assertEquals(EnumSet.allOf(BirdGame3.MapType.class),
                campaign.orderedMissions.stream()
                        .map(StoryCampaign.Mission::map)
                        .collect(() -> EnumSet.noneOf(BirdGame3.MapType.class), Set::add, Set::addAll));
        assertEquals(EnumSet.allOf(BirdGame3.BirdType.class), report.playableBirds());
        assertEquals(2, campaign.scenes.values().stream().filter(StoryCampaign.Cutscene::deathScene).count());
        assertEquals(StoryCampaign.PlayableKind.FULL_ROSTER,
                campaign.mission("null_roc").playable().kind());
        assertEquals(StoryCampaign.PlayableKind.FULL_ROSTER,
                campaign.mission("the_null_rock").playable().kind());
    }

    @Test
    void stableIdsAndTransitionsTraverseFromFirstSceneToFinale() {
        StoryCampaign campaign = StoryCampaignContent.create();
        Set<String> missionIds = new HashSet<>();
        Set<String> sceneIds = new HashSet<>();
        StoryCampaignProgress traversal = new StoryCampaignProgress();

        for (StoryCampaign.Mission mission : campaign.orderedMissions) {
            assertTrue(missionIds.add(mission.id()), "duplicate mission " + mission.id());
            assertNotNull(campaign.scene(mission.preSceneId()));
            assertNotNull(campaign.scene(mission.postSceneId()));
            assertEquals(mission.id(), traversal.currentMissionId);
            traversal.markSceneSeen(mission.preSceneId());
            assertTrue(traversal.markMissionCompleted(campaign, mission));
            traversal.markSceneSeen(mission.postSceneId());
        }
        for (StoryCampaign.Cutscene scene : campaign.scenes.values()) {
            assertTrue(sceneIds.add(scene.id()), "duplicate scene " + scene.id());
        }

        assertEquals("dead_air", campaign.firstMission().id());
        assertEquals("the_null_rock", campaign.orderedMissions.getLast().id());
        assertNull(campaign.nextMission("the_null_rock"));
        assertTrue(traversal.campaignComplete);
        assertEquals(40, traversal.completedCount());
        assertEquals(80, traversal.seenSceneIds().size());
    }

    @Test
    void deathsAndFinalNameAppearAtTheirAuthoredStoryPoints() {
        StoryCampaign campaign = StoryCampaignContent.create();
        var deaths = campaign.scenes.values().stream()
                .filter(StoryCampaign.Cutscene::deathScene)
                .toList();

        assertEquals(2, deaths.size());
        assertTrue(deaths.stream().anyMatch(scene -> scene.lines().stream()
                .anyMatch(line -> line.speaker().equals("Old Sparrow"))));
        assertTrue(deaths.stream().anyMatch(scene -> scene.lines().stream()
                .anyMatch(line -> line.speaker().equals("Eagle"))));

        for (StoryCampaign.Mission mission : campaign.orderedMissions) {
            String combined = mission.title() + " " + mission.briefing();
            if (!mission.id().equals("the_null_rock")) {
                assertFalse(combined.contains("The Null Rock"), mission.id());
            }
        }
    }

    @Test
    void cutTheLockIsAnAuthoredEagleDuelWithoutRavenOrCaptureObjectives() {
        StoryCampaign.Mission mission = StoryCampaignContent.create().mission("cut_the_lock");

        assertEquals(StoryCampaign.ArenaVariant.CROWN_DUEL, mission.arenaVariant());
        assertEquals(StoryCampaign.PlayableKind.CHOICE, mission.playable().kind());
        assertEquals(
                Set.of(BirdGame3.BirdType.FALCON, BirdGame3.BirdType.RAZORBILL),
                Set.copyOf(mission.playable().resolvedBirds()));
        assertTrue(mission.allies().isEmpty());
        assertEquals(1, mission.enemies().size());
        assertEquals(BirdGame3.BirdType.EAGLE, mission.enemies().getFirst().type());
        assertTrue(mission.enemies().getFirst().boss());
        assertEquals(1, mission.phases().size());
        assertEquals(StoryCampaign.ObjectiveType.BOSS_PHASES,
                mission.phases().getFirst().objective());
        assertTrue(mission.enemies().stream()
                .noneMatch(fighter -> fighter.type() == BirdGame3.BirdType.RAVEN));
    }

    @Test
    void carrionAudienceHasSeparateGuardsBeforeReservedVultureBoss() {
        StoryCampaign.Mission mission = StoryCampaignContent.create().mission("carrion_audience");

        assertEquals(1, mission.enemies().stream().filter(StoryCampaign.Fighter::boss).count());
        assertEquals(2, mission.enemies().stream().filter(fighter -> !fighter.boss()).count());
        assertEquals(StoryCampaign.ObjectiveType.GAUNTLET,
                mission.phases().getFirst().objective());
        assertEquals(StoryCampaign.ObjectiveType.BOSS_PHASES,
                mission.phases().getLast().objective());
    }
}
