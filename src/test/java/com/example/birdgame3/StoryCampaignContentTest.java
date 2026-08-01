package com.example.birdgame3;

import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
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
    void vultureRescueAndTrueFinaleUseDifferentThreatsAndAuthoredObjectives() {
        StoryCampaign campaign = StoryCampaignContent.create();
        StoryCampaign.Mission rescue = campaign.mission("free_the_flock");
        StoryCampaign.Mission finale = campaign.mission("the_null_rock");

        StoryCampaign.Fighter shadowVulture = rescue.enemies().stream()
                .filter(StoryCampaign.Fighter::boss)
                .findFirst()
                .orElseThrow();
        assertEquals(BirdGame3.BirdType.VULTURE, shadowVulture.type());
        assertEquals("Shadow Vulture", shadowVulture.name());
        assertEquals(BirdGame3.CAMPAIGN_NULL_ECHO_SKIN, shadowVulture.skinKey());
        assertTrue(rescue.phases().stream()
                .anyMatch(phase -> phase.objective() == StoryCampaign.ObjectiveType.BOSS_PHASES));

        assertEquals(StoryCampaign.ArenaVariant.NULL_ROCK, finale.arenaVariant());
        assertEquals(List.of(
                        StoryCampaign.ObjectiveType.CAPTURE,
                        StoryCampaign.ObjectiveType.HOLD_ZONE,
                        StoryCampaign.ObjectiveType.CAPTURE,
                        StoryCampaign.ObjectiveType.BOSS_PHASES),
                finale.phases().stream().map(StoryCampaign.MissionPhase::objective).toList());
        assertEquals("Plant Penguin's cavern charge", finale.phases().get(2).label());
        assertEquals("Defeat The Null Rock one-on-one", finale.phases().getLast().label());
        assertEquals(9, StoryCampaignContent.nullRockDuelDialogue(
                "Pigeon", BirdGame3.BirdType.PIGEON).size());
        assertEquals("music-null-rock.mp3", campaign.scene("s79_null_rock_before").musicCue());
        assertEquals("music-null-rock.mp3", campaign.scene("s80_eagle_end").musicCue());
        StoryCampaign.DialogueLine farewell = campaign.scene("s80_eagle_end").lines().stream()
                .filter(line -> line.text().startsWith("Then move."))
                .findFirst()
                .orElseThrow();
        assertEquals("music-farewell.mp3", farewell.musicCue());
        assertNotNull(campaign.scene("s81_beyond_the_map"));
        assertNotNull(BirdGame3.class.getResource("/sounds/music-null-rock.mp3"));
        assertNotNull(BirdGame3.class.getResource("/sounds/music-farewell.mp3"));
        assertNotNull(BirdGame3.class.getResource("/sounds/music-escape.mp3"));
        assertNotNull(BirdGame3.class.getResource("/sounds/music-credits.mp3"));
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
    void carrionAudienceGuardsStayDeadBeforeReservedVultureBossEnters() {
        StoryCampaign.Mission mission = StoryCampaignContent.create().mission("carrion_audience");

        assertEquals(1, mission.enemies().stream().filter(StoryCampaign.Fighter::boss).count());
        assertEquals(2, mission.enemies().stream().filter(fighter -> !fighter.boss()).count());
        assertEquals(StoryCampaign.ObjectiveType.ELIMINATION,
                mission.phases().getFirst().objective());
        assertEquals(StoryCampaign.ObjectiveType.BOSS_PHASES,
                mission.phases().getLast().objective());

        StoryMissionController controller = new StoryMissionController(
                mission, StoryCampaign.Difficulty.NORMAL, 6000);
        StoryMissionController.TickResult result = controller.tick(List.of(
                new StoryMissionController.Participant(0, 1, 1000, 110, 110),
                new StoryMissionController.Participant(2, 2, 3000, 0, 112),
                new StoryMissionController.Participant(3, 2, 4000, 0, 112)
        ));

        assertEquals(StoryMissionController.Outcome.PHASE_ADVANCED, result.outcome());
        assertEquals(StoryCampaign.ObjectiveType.BOSS_PHASES,
                controller.currentPhase().objective());
    }

    @Test
    void crowCountryDoesNotReviveGuardsDefeatedBeforeTheSecondObjective() {
        StoryCampaign.Mission mission = StoryCampaignContent.create().mission("crow_country");

        assertEquals(StoryCampaign.ObjectiveType.CAPTURE,
                mission.phases().getFirst().objective());
        assertEquals(StoryCampaign.ObjectiveType.ELIMINATION,
                mission.phases().getLast().objective());

        StoryMissionController controller = new StoryMissionController(
                mission, StoryCampaign.Difficulty.NORMAL, 6000);
        StoryMissionController.Participant deadVulture =
                new StoryMissionController.Participant(1, 2, 2000, 0, 100);
        StoryMissionController.Participant deadRaven =
                new StoryMissionController.Participant(2, 2, 4000, 0, 100);

        StoryMissionController.TickResult result = null;
        for (int target = 0; target < 3; target++) {
            double zoneX = controller.captureZoneCenterX(target, 3);
            StoryMissionController.Participant player =
                    new StoryMissionController.Participant(0, 1, zoneX, 100, 100);
            for (int tick = 0; tick < 120; tick++) {
                result = controller.tick(List.of(player, deadVulture, deadRaven));
            }
        }

        assertNotNull(result);
        assertEquals(StoryMissionController.Outcome.PHASE_ADVANCED, result.outcome());
        assertEquals(StoryMissionController.Outcome.COMPLETE,
                controller.tick(List.of(
                        new StoryMissionController.Participant(0, 1, 3000, 100, 100),
                        deadVulture,
                        deadRaven)).outcome());
    }

    @Test
    void perfectWeatherNullEchoesStayDefeatedAcrossTheObjectiveHandoff() {
        StoryCampaign.Mission mission = StoryCampaignContent.create().mission("perfect_weather");

        assertEquals(StoryCampaign.ObjectiveType.PROTECT,
                mission.phases().getFirst().objective());
        assertEquals(StoryCampaign.ObjectiveType.ELIMINATION,
                mission.phases().getLast().objective());

        StoryMissionController controller = new StoryMissionController(
                mission, StoryCampaign.Difficulty.NORMAL, 6000);
        List<StoryMissionController.Participant> clearedRoster = List.of(
                new StoryMissionController.Participant(0, 1, 1200, 110, 110),
                new StoryMissionController.Participant(1, 2, 2800, 0, 112),
                new StoryMissionController.Participant(2, 2, 4100, 0, 112)
        );

        assertEquals(StoryMissionController.Outcome.PHASE_ADVANCED,
                controller.tick(clearedRoster).outcome());
        assertEquals(StoryMissionController.Outcome.COMPLETE,
                controller.tick(clearedRoster).outcome());
    }

    @Test
    void crownlockPrisonHostsTheBlackoutAndVultureRescueMissions() {
        StoryCampaign campaign = StoryCampaignContent.create();
        StoryCampaign.Mission blackout = campaign.mission("blackout_key");
        StoryCampaign.Mission rescue = campaign.mission("free_the_flock");

        assertEquals(BirdGame3.MapType.PRISON, blackout.map());
        assertEquals(BirdGame3.MapType.PRISON, rescue.map());
        assertEquals(BirdGame3.MapType.PRISON, campaign.scene(blackout.preSceneId()).location());
        assertEquals(BirdGame3.MapType.PRISON, campaign.scene(blackout.postSceneId()).location());
        assertEquals(BirdGame3.MapType.PRISON, campaign.scene(rescue.preSceneId()).location());
        assertEquals(BirdGame3.MapType.PRISON, campaign.scene(rescue.postSceneId()).location());
        assertEquals("music-prison.mp3", campaign.scene(blackout.preSceneId()).musicCue());
        assertEquals("music-prison.mp3", campaign.scene(rescue.postSceneId()).musicCue());
        assertNotNull(BirdGame3.class.getResource("/sounds/music-prison.mp3"));
        assertTrue(blackout.briefing().contains("city's old central station"));
    }

    @Test
    void namedCastKeepsBaseIdentityWhileEveryCampaignExtraHasAnAuthoredLook() {
        StoryCampaign campaign = StoryCampaignContent.create();

        for (StoryCampaign.Mission mission : campaign.orderedMissions) {
            for (StoryCampaign.Fighter fighter : mission.allies()) {
                assertTrue(StoryCampaignContent.isNamedCampaignCharacter(fighter.name()),
                        () -> mission.id() + " has an unnamed ally without an authored extra policy: " + fighter.name());
                assertNull(fighter.skinKey(),
                        () -> mission.id() + " should keep named ally " + fighter.name() + " on their base identity");
            }
            for (StoryCampaign.Fighter fighter : mission.enemies()) {
                if (StoryCampaignContent.isNamedCampaignCharacter(fighter.name())) {
                    assertNull(fighter.skinKey(),
                            () -> mission.id() + " should keep named character " + fighter.name()
                                    + " on their base identity");
                } else {
                    assertNotNull(fighter.skinKey(),
                            () -> mission.id() + " leaves campaign extra " + fighter.name()
                                    + " looking like the base roster character");
                    assertFalse(fighter.skinKey().isBlank(),
                            () -> mission.id() + " has a blank extra skin for " + fighter.name());
                }
            }
        }

        assertTrue(campaign.mission("dead_air").enemies().stream()
                .allMatch(fighter -> BirdGame3.CAMPAIGN_CROWN_TROOP_SKIN.equals(fighter.skinKey())));
        assertEquals(BirdGame3.CAMPAIGN_HARBOR_CREW_SKIN,
                campaign.mission("harbor_lock").enemies().get(1).skinKey());
        assertTrue(campaign.mission("last_thermal").enemies().stream()
                .allMatch(fighter -> BirdGame3.CAMPAIGN_CARRION_PACT_SKIN.equals(fighter.skinKey())));
        assertTrue(campaign.mission("perfect_weather").enemies().stream()
                .allMatch(fighter -> BirdGame3.CAMPAIGN_NULL_ECHO_SKIN.equals(fighter.skinKey())));
    }
}
