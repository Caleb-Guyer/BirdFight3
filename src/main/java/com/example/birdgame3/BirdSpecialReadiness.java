package com.example.birdgame3;

final class BirdSpecialReadiness {
    private BirdSpecialReadiness() {
    }

    static boolean canStart(Bird bird) {
        if (bird.health <= 0) {
            return false;
        }

        return switch (bird.type) {
            case PIGEON -> bird.canStartPigeonSpecial();
            case EAGLE, FALCON -> canStartRaptorSpecial(bird);
            case PHOENIX -> bird.canStartPhoenixSpecial();
            case HUMMINGBIRD -> bird.canStartHummingbirdSpecial();
            case TURKEY -> bird.canStartTurkeySpecial();
            case ROOSTER -> bird.canStartRoosterSpecial();
            case ROADRUNNER -> bird.canStartRoadrunnerSpecial();
            case PENGUIN -> bird.canStartPenguinSpecial();
            case SHOEBILL -> bird.canStartShoebillSpecial();
            case MOCKINGBIRD -> bird.canStartMockingbirdSpecial();
            case RAZORBILL -> bird.canStartRazorbillSpecial();
            case GRINCHHAWK -> bird.canStartGrinchhawkSpecial();
            case VULTURE -> bird.canStartVultureSpecial();
            case OPIUMBIRD, HEISENBIRD -> bird.canStartOpiumSpecial();
            case TITMOUSE -> bird.canStartTitmouseSpecial();
            case BAT -> bird.canStartBatSpecial();
            case PELICAN -> bird.canStartPelicanSpecial();
            case RAVEN -> bird.canStartRavenSpecial();
            case GOOSE -> bird.canStartGooseSpecial();
            case KIWI -> bird.canStartKiwiSpecial();
        };
    }

    static boolean hasEmptyMockingbirdNeutral(Bird bird) {
        return bird.type == BirdGame3.BirdType.MOCKINGBIRD
                && bird.selectMockingbirdSpecialVariant() == Bird.MockingbirdSpecialVariant.NEUTRAL
                && !bird.hasMockingbirdCapturedType();
    }

    static boolean usesSharedSpecialCooldown(Bird bird) {
        return switch (bird.type) {
            case EAGLE, FALCON, HUMMINGBIRD, TURKEY, ROOSTER, ROADRUNNER, PENGUIN,
                    SHOEBILL, MOCKINGBIRD, RAZORBILL, GRINCHHAWK, VULTURE,
                    OPIUMBIRD, HEISENBIRD, TITMOUSE, BAT, PELICAN, RAVEN, GOOSE, KIWI -> false;
            case PIGEON, PHOENIX -> true;
        };
    }

    private static boolean canStartRaptorSpecial(Bird bird) {
        Bird.RaptorSpecialVariant variant = bird.selectRaptorSpecialVariant();
        if (bird.canStartRaptorSpecialVariant(variant)) {
            return true;
        }
        if (!bird.game.isAI[bird.playerIndex] && bird.raptorSpecialOnReuseLockout(variant)) {
            bird.cooldownFlash = 15;
        }
        return false;
    }
}
