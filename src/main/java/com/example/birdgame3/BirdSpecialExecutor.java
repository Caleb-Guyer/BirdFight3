package com.example.birdgame3;

final class BirdSpecialExecutor {
    private BirdSpecialExecutor() {
    }

    static void execute(Bird bird, boolean ultimateTriggered) {
        switch (bird.type) {
            case PIGEON -> PigeonSpecials.use(bird, ultimateTriggered);
            case EAGLE, FALCON -> RaptorSpecials.use(bird, ultimateTriggered);
            case PHOENIX -> PhoenixSpecials.use(bird, ultimateTriggered);
            case HUMMINGBIRD -> HummingbirdSpecials.use(bird, ultimateTriggered);
            case TURKEY -> TurkeySpecials.use(bird, ultimateTriggered);
            case ROADRUNNER -> RoadrunnerSpecials.use(bird, ultimateTriggered);
            case PENGUIN -> PenguinSpecials.use(bird, ultimateTriggered);
            case SHOEBILL -> ShoebillSpecials.use(bird, ultimateTriggered);
            case MOCKINGBIRD -> MockingbirdSpecials.use(bird, ultimateTriggered);
            case RAZORBILL -> RazorbillSpecials.use(bird, ultimateTriggered);
            case GRINCHHAWK -> GrinchhawkSpecials.use(bird, ultimateTriggered);
            case VULTURE -> VultureSpecials.use(bird, ultimateTriggered);
            case ROOSTER -> RoosterSpecials.use(bird, ultimateTriggered);
            case OPIUMBIRD -> OpiumSpecials.useOpium(bird, ultimateTriggered);
            case HEISENBIRD -> OpiumSpecials.useHeisenbird(bird, ultimateTriggered);
            case TITMOUSE -> TitmouseSpecials.use(bird, ultimateTriggered);
            case BAT -> BatSpecials.use(bird, ultimateTriggered);
            case PELICAN -> PelicanSpecials.use(bird, ultimateTriggered);
            case RAVEN -> RavenSpecials.use(bird, ultimateTriggered);
        }
    }
}

final class RaptorSpecials {
    private RaptorSpecials() {
    }

    static void use(Bird bird, boolean ultimate) {
        switch (bird.selectRaptorSpecialVariant()) {
            case NEUTRAL -> bird.specialRaptorNeutral(ultimate);
            case SIDE -> bird.specialRaptorSide(ultimate);
            case UP -> bird.specialRaptorUp(ultimate);
            case DOWN -> {
                if (bird.type == BirdGame3.BirdType.EAGLE) {
                    bird.specialEagle(ultimate);
                } else {
                    bird.specialFalcon(ultimate);
                }
            }
        }
    }
}

final class PhoenixSpecials {
    private PhoenixSpecials() {
    }

    static void use(Bird bird, boolean ultimate) {
        switch (bird.selectPhoenixSpecialVariant()) {
            case NEUTRAL -> bird.specialPhoenixNeutral(ultimate);
            case SIDE -> bird.specialPhoenixSide(ultimate);
            case UP -> bird.specialPhoenixUp(ultimate);
            case DOWN -> bird.specialPhoenixDown(ultimate);
        }
    }
}

final class HummingbirdSpecials {
    private HummingbirdSpecials() {
    }

    static void use(Bird bird, boolean ultimate) {
        switch (bird.selectHummingbirdSpecialVariant()) {
            case NEUTRAL -> bird.specialHummingbirdNeedleBarrage(ultimate);
            case SIDE -> bird.specialHummingbirdFlashSip(ultimate);
            case UP -> bird.specialHummingbirdHoverBurst(ultimate);
            case DOWN -> bird.specialHummingbirdNectarTrap(ultimate);
        }
    }
}

final class TurkeySpecials {
    private TurkeySpecials() {
    }

    static void use(Bird bird, boolean ultimate) {
        switch (bird.selectTurkeySpecialVariant()) {
            case NEUTRAL -> bird.specialTurkeyGobbleGuard(ultimate);
            case SIDE -> bird.specialTurkeyStampede(ultimate);
            case UP -> bird.specialTurkeyPanicFlap(ultimate);
            case DOWN -> bird.specialTurkeyFeastTrap(ultimate);
        }
    }
}

final class RoadrunnerSpecials {
    private RoadrunnerSpecials() {
    }

    static void use(Bird bird, boolean ultimate) {
        if (ultimate) {
            bird.activateRoadrunnerSandstorm();
        }
        switch (bird.selectRoadrunnerSpecialVariant()) {
            case NEUTRAL -> bird.specialRoadrunnerBeepBlitz(ultimate);
            case SIDE -> bird.specialRoadrunnerCanyonRicochet(ultimate);
            case UP -> bird.specialRoadrunnerDustDevilLift(ultimate);
            case DOWN -> bird.specialRoadrunnerPaintedRoad(ultimate);
        }
    }
}

final class PenguinSpecials {
    private PenguinSpecials() {
    }

    static void use(Bird bird, boolean ultimate) {
        switch (bird.selectPenguinSpecialVariant()) {
            case NEUTRAL -> bird.specialPenguinBellySlide(ultimate);
            case SIDE -> bird.specialPenguinIcebergShove(ultimate);
            case UP -> bird.specialPenguinRocketFlop(ultimate);
            case DOWN -> bird.specialPenguinSnowFort(ultimate);
        }
    }
}

final class ShoebillSpecials {
    private ShoebillSpecials() {
    }

    static void use(Bird bird, boolean ultimate) {
        switch (bird.selectShoebillSpecialVariant()) {
            case NEUTRAL -> bird.specialShoebillDeathStare(ultimate);
            case SIDE -> bird.specialShoebillHeavyThrust(ultimate);
            case UP -> bird.specialShoebillMarshLift(ultimate);
            case DOWN -> bird.specialShoebillStatueTrap(ultimate);
        }
    }
}

final class MockingbirdSpecials {
    private MockingbirdSpecials() {
    }

    static void use(Bird bird, boolean ultimate) {
        switch (bird.selectMockingbirdSpecialVariant()) {
            case NEUTRAL -> bird.specialMockingbirdNeutral(ultimate);
            case SIDE -> bird.specialMockingbirdSide(ultimate);
            case UP -> bird.specialMockingbirdUp(ultimate);
            case DOWN -> bird.specialMockingbirdLounge(ultimate);
        }
    }
}

final class RazorbillSpecials {
    private RazorbillSpecials() {
    }

    static void use(Bird bird, boolean ultimate) {
        switch (bird.selectRazorbillSpecialVariant()) {
            case NEUTRAL -> bird.specialRazorbillNeutral(ultimate);
            case SIDE -> bird.specialRazorbillSide(ultimate);
            case UP -> bird.specialRazorbillUp(ultimate);
            case DOWN -> bird.specialRazorbillCounter(ultimate);
        }
    }
}

final class GrinchhawkSpecials {
    private GrinchhawkSpecials() {
    }

    static void use(Bird bird, boolean ultimate) {
        switch (bird.selectGrinchhawkSpecialVariant()) {
            case NEUTRAL -> bird.specialGrinchhawkHeartSnatch(ultimate);
            case SIDE -> bird.specialGrinchhawkSleighCrash(ultimate);
            case UP -> bird.specialGrinchhawkChimneyFlap(ultimate);
            case DOWN -> bird.specialGrinchhawkFakePresent(ultimate);
        }
    }
}

final class VultureSpecials {
    private VultureSpecials() {
    }

    static void use(Bird bird, boolean ultimate) {
        if (bird.isNullRockForm()) {
            bird.specialNullRock(ultimate);
            return;
        }
        if (ultimate) {
            bird.specialVultureBlackSkyFeast();
            return;
        }
        switch (bird.selectVultureSpecialVariant()) {
            case NEUTRAL -> bird.specialVultureCarrionCall(false);
            case SIDE -> bird.specialVultureGravewindGlide();
            case UP -> bird.specialVultureThermalSpiral(false);
            case DOWN -> bird.specialVultureBoneOffering(false);
        }
    }
}

final class RoosterSpecials {
    private RoosterSpecials() {
    }

    static void use(Bird bird, boolean ultimate) {
        bird.ensureRoosterStartingChicks();
        switch (bird.selectRoosterSpecialVariant()) {
            case NEUTRAL -> bird.specialRoosterCallChick(ultimate);
            case SIDE -> bird.specialRoosterThrowChick(ultimate);
            case UP -> bird.specialRoosterCoopBoost(ultimate);
            case DOWN -> bird.specialRoosterRecallChicks(ultimate);
        }
    }
}

final class OpiumSpecials {
    private OpiumSpecials() {
    }

    static void useOpium(Bird bird, boolean ultimate) {
        if (ultimate) {
            bird.specialOpiumUltimate();
            return;
        }
        switch (bird.selectOpiumSpecialVariant()) {
            case NEUTRAL -> bird.specialOpiumNeutral(false);
            case SIDE -> bird.specialOpiumSide(false);
            case UP -> bird.specialOpiumUp(false);
            case DOWN -> bird.specialOpiumDown(false);
        }
    }

    static void useHeisenbird(Bird bird, boolean ultimate) {
        if (ultimate) {
            bird.specialHeisenUltimate();
            return;
        }
        switch (bird.selectOpiumSpecialVariant()) {
            case NEUTRAL -> bird.specialHeisenNeutral(false);
            case SIDE -> bird.specialOpiumSide(true);
            case UP -> bird.specialOpiumUp(true);
            case DOWN -> bird.specialOpiumDown(true);
        }
    }
}

final class TitmouseSpecials {
    private TitmouseSpecials() {
    }

    static void use(Bird bird, boolean ultimate) {
        if (ultimate) {
            bird.specialTitmouseMobbingRun();
            return;
        }
        switch (bird.selectTitmouseSpecialVariant()) {
            case NEUTRAL -> bird.specialTitmouseScoldChorus(false);
            case SIDE -> bird.specialTitmouseBarkskip(false);
            case UP -> bird.specialTitmouseTuftVault(false);
            case DOWN -> bird.specialTitmouseSeedStash(false);
        }
    }
}

final class BatSpecials {
    private BatSpecials() {
    }

    static void use(Bird bird, boolean ultimate) {
        if (ultimate) {
            bird.specialBatCathedralEcho();
            return;
        }
        switch (bird.selectBatSpecialVariant()) {
            case NEUTRAL -> bird.specialBatNeutral(false);
            case SIDE -> bird.specialBatWingcut(false);
            case UP -> bird.specialBatMoonrise(false);
            case DOWN -> bird.specialBatSilentDescent(false);
        }
    }
}

final class PelicanSpecials {
    private PelicanSpecials() {
    }

    static void use(Bird bird, boolean ultimate) {
        if (ultimate) {
            bird.beginPelicanFullHold();
        }
        switch (bird.selectPelicanSpecialVariant()) {
            case NEUTRAL -> bird.specialPelicanPouchSnare(ultimate);
            case SIDE -> bird.specialPelicanBreakwaterRun(ultimate);
            case UP -> bird.specialPelicanThermalSail(ultimate);
            case DOWN -> bird.specialPelicanBilgeCommand(ultimate);
        }
    }
}

final class RavenSpecials {
    private RavenSpecials() {
    }

    static void use(Bird bird, boolean ultimate) {
        if (ultimate) {
            bird.specialRavenUnkindness();
            return;
        }
        switch (bird.selectRavenSpecialVariant()) {
            case NEUTRAL -> bird.specialRavenBlackQuill(false);
            case SIDE -> bird.specialRavenShadowWarp(false);
            case UP -> bird.specialRavenMurderLift(false);
            case DOWN -> bird.specialRavenNevermore(false);
        }
    }
}
