package com.example.birdgame3;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * Builds deterministic, disposable campaign reinforcements without cloning a
 * defeated mission character.
 *
 * <p>Gauntlets recycle combat slots because the engine has a fixed combatant
 * capacity. The bird placed into a recycled slot must still be a new actor:
 * it gets a different bird body, call sign, and the faction skin appropriate
 * to the original wave. This is setup logic and deliberately consumes no
 * {@link SimRng} values.
 */
final class StoryCampaignReinforcements {
    private static final List<BirdGame3.BirdType> BODY_POOL = List.of(
            BirdGame3.BirdType.PIGEON,
            BirdGame3.BirdType.MOCKINGBIRD,
            BirdGame3.BirdType.PELICAN,
            BirdGame3.BirdType.GOOSE,
            BirdGame3.BirdType.PHOENIX,
            BirdGame3.BirdType.PENGUIN,
            BirdGame3.BirdType.ROADRUNNER,
            BirdGame3.BirdType.HUMMINGBIRD,
            BirdGame3.BirdType.TITMOUSE,
            BirdGame3.BirdType.TURKEY,
            BirdGame3.BirdType.ROOSTER,
            BirdGame3.BirdType.BAT,
            BirdGame3.BirdType.SHOEBILL,
            BirdGame3.BirdType.RAZORBILL,
            BirdGame3.BirdType.FALCON,
            BirdGame3.BirdType.EAGLE,
            BirdGame3.BirdType.VULTURE,
            BirdGame3.BirdType.RAVEN,
            BirdGame3.BirdType.HEISENBIRD,
            BirdGame3.BirdType.OPIUMBIRD,
            BirdGame3.BirdType.GRINCHHAWK
    );

    private static final List<String> CALL_SIGNS = List.of(
            "Kite", "Rook", "Flint", "Morrow", "Slate", "Ember",
            "Rime", "Gale", "Cinder", "Lumen", "Drift", "Talon",
            "Brass", "Sable", "Mast", "Hollow"
    );

    private StoryCampaignReinforcements() {
    }

    static StoryCampaign.Fighter create(StoryCampaign.Mission mission,
                                        StoryCampaign.Fighter source,
                                        int waveNumber,
                                        int positionInWave,
                                        int fightersPerWave) {
        if (mission == null || source == null) {
            throw new IllegalArgumentException("Campaign reinforcement needs a mission and source fighter");
        }
        if (source.boss()) {
            throw new IllegalArgumentException("Bosses cannot be recycled as campaign reinforcements");
        }

        List<BirdGame3.BirdType> availableBodies = availableBodies(mission);
        int ordinal = spawnOrdinal(waveNumber, positionInWave, fightersPerWave);
        int missionOffset = Math.floorMod(mission.id().hashCode(), availableBodies.size());
        BirdGame3.BirdType body = availableBodies.get(
                Math.floorMod(missionOffset + ordinal, availableBodies.size()));
        String callSign = CALL_SIGNS.get(Math.floorMod(missionOffset + ordinal, CALL_SIGNS.size()));
        String skinKey = source.skinKey() == null || source.skinKey().isBlank()
                ? fallbackSkin(mission)
                : source.skinKey();
        String name = factionName(skinKey) + " " + callSign;

        return new StoryCampaign.Fighter(
                body,
                name,
                2,
                source.health(),
                source.power(),
                source.speed(),
                skinKey,
                false
        );
    }

    private static List<BirdGame3.BirdType> availableBodies(StoryCampaign.Mission mission) {
        Set<BirdGame3.BirdType> authoredBodies = EnumSet.noneOf(BirdGame3.BirdType.class);
        authoredBodies.addAll(mission.playable().resolvedBirds());
        mission.allies().forEach(fighter -> authoredBodies.add(fighter.type()));
        mission.enemies().forEach(fighter -> authoredBodies.add(fighter.type()));

        List<BirdGame3.BirdType> available = new ArrayList<>();
        for (BirdGame3.BirdType type : BODY_POOL) {
            if (!authoredBodies.contains(type)) {
                available.add(type);
            }
        }
        if (available.isEmpty()) {
            available.addAll(BODY_POOL);
        }
        return List.copyOf(available);
    }

    private static int spawnOrdinal(int waveNumber, int positionInWave, int fightersPerWave) {
        int reinforcementsBeforeThisWave = Math.max(0, waveNumber - 2)
                * Math.max(1, fightersPerWave);
        return reinforcementsBeforeThisWave + Math.max(0, positionInWave);
    }

    private static String fallbackSkin(StoryCampaign.Mission mission) {
        return switch (mission.arenaVariant()) {
            case CARRION -> BirdGame3.CAMPAIGN_CARRION_PACT_SKIN;
            case EVACUATION -> BirdGame3.CAMPAIGN_HARBOR_CREW_SKIN;
            case NULL_ROC, NULL_ROCK -> BirdGame3.CAMPAIGN_NULL_ECHO_SKIN;
            default -> BirdGame3.CAMPAIGN_CROWN_TROOP_SKIN;
        };
    }

    private static String factionName(String skinKey) {
        if (BirdGame3.CAMPAIGN_NULL_ECHO_SKIN.equals(skinKey)) {
            return "Null Construct";
        }
        if (BirdGame3.CAMPAIGN_HARBOR_CREW_SKIN.equals(skinKey)) {
            return "Harbor Boarder";
        }
        if (BirdGame3.CAMPAIGN_CARRION_PACT_SKIN.equals(skinKey)) {
            return "Carrion Wing";
        }
        return "Crown Guard";
    }
}
