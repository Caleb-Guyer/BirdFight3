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
            case GOOSE -> GooseSpecials.use(bird, ultimateTriggered);
            case KIWI -> KiwiSpecials.use(bird, ultimateTriggered);
        }
    }
}
