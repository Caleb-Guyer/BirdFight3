package com.example.birdgame3;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StoryCampaignReinforcementsTest {

    @Test
    void blackoutKeyWavesNeverRespawnVultureOrItsOriginalGoon() {
        StoryCampaign.Mission mission = StoryCampaignContent.create().mission("blackout_key");
        List<StoryCampaign.Fighter> sources = mission.enemies().stream()
                .filter(fighter -> !fighter.boss())
                .toList();
        Set<BirdGame3.BirdType> originalBodies = new HashSet<>();
        Set<String> originalNames = new HashSet<>();
        sources.forEach(fighter -> {
            originalBodies.add(fighter.type());
            originalNames.add(fighter.name());
        });

        Set<BirdGame3.BirdType> waveBodies = new HashSet<>();
        Set<String> waveNames = new HashSet<>();
        for (int wave = 2; wave <= 3; wave++) {
            for (int position = 0; position < sources.size(); position++) {
                StoryCampaign.Fighter reinforcement = StoryCampaignReinforcements.create(
                        mission, sources.get(position), wave, position, sources.size());
                assertFalse(originalBodies.contains(reinforcement.type()));
                assertFalse(originalNames.contains(reinforcement.name()));
                assertEquals(BirdGame3.CAMPAIGN_NULL_ECHO_SKIN, reinforcement.skinKey());
                assertTrue(waveBodies.add(reinforcement.type()),
                        "Each Blackout Key reinforcement must have a visibly distinct body");
                assertTrue(waveNames.add(reinforcement.name()),
                        "Each Blackout Key reinforcement must be a new actor");
            }
        }
    }

    @Test
    void everyCampaignGauntletUsesFreshActorsForEveryAuthoredWave() {
        StoryCampaign campaign = StoryCampaignContent.create();
        for (StoryCampaign.Mission mission : campaign.orderedMissions) {
            int waveCount = mission.phases().stream()
                    .filter(phase -> phase.objective() == StoryCampaign.ObjectiveType.GAUNTLET)
                    .mapToInt(phase -> Math.max(1, phase.targetCount()))
                    .max()
                    .orElse(0);
            if (waveCount <= 1) continue;

            List<StoryCampaign.Fighter> sources = mission.enemies().stream()
                    .filter(fighter -> !fighter.boss())
                    .toList();
            assertFalse(sources.isEmpty(), mission.id() + " needs a reinforcement source");
            Set<BirdGame3.BirdType> authoredBodies = new HashSet<>();
            mission.playable().resolvedBirds().forEach(authoredBodies::add);
            mission.allies().forEach(fighter -> authoredBodies.add(fighter.type()));
            mission.enemies().forEach(fighter -> authoredBodies.add(fighter.type()));
            Set<String> generatedNames = new HashSet<>();
            Set<BirdGame3.BirdType> generatedBodies = new HashSet<>();

            for (int wave = 2; wave <= waveCount; wave++) {
                for (int position = 0; position < sources.size(); position++) {
                    StoryCampaign.Fighter source = sources.get(position);
                    StoryCampaign.Fighter reinforcement = StoryCampaignReinforcements.create(
                            mission, source, wave, position, sources.size());
                    assertFalse(authoredBodies.contains(reinforcement.type()),
                            mission.id() + " recycled an authored character body");
                    assertNotEquals(source.name(), reinforcement.name(),
                            mission.id() + " revived a defeated actor by name");
                    assertTrue(generatedNames.add(reinforcement.name()),
                            mission.id() + " reused a reinforcement identity");
                    assertTrue(generatedBodies.add(reinforcement.type()),
                            mission.id() + " reused a reinforcement body");
                    assertTrue(BirdGame3.isCampaignFactionSkinKey(reinforcement.skinKey()));
                }
            }
        }
    }

    @Test
    void reinforcementGenerationIsDeterministicAndRejectsBossCloning() {
        StoryCampaign.Mission mission = StoryCampaignContent.create().mission("crown_archive");
        StoryCampaign.Fighter guard = mission.enemies().stream()
                .filter(fighter -> !fighter.boss())
                .findFirst()
                .orElseThrow();

        List<StoryCampaign.Fighter> firstRun = new ArrayList<>();
        List<StoryCampaign.Fighter> secondRun = new ArrayList<>();
        for (int wave = 2; wave <= 3; wave++) {
            firstRun.add(StoryCampaignReinforcements.create(mission, guard, wave, 0, 1));
            secondRun.add(StoryCampaignReinforcements.create(mission, guard, wave, 0, 1));
        }
        assertEquals(firstRun, secondRun);

        StoryCampaign.Fighter boss = mission.enemies().stream()
                .filter(StoryCampaign.Fighter::boss)
                .findFirst()
                .orElseThrow();
        assertThrows(IllegalArgumentException.class,
                () -> StoryCampaignReinforcements.create(mission, boss, 2, 0, 1));
    }
}
