package com.example.birdgame3;

import java.util.Arrays;

final class PelicanSpecials {
    static final String MAELSTROM_GULLET_MOVE = "Maelstrom Gullet";

    private PelicanSpecials() {
    }

    static void use(Bird bird, boolean ultimate) {
        if (ultimate) {
            bird.startPelicanMaelstromGullet();
            return;
        }
        switch (bird.selectPelicanSpecialVariant()) {
            case NEUTRAL -> bird.specialPelicanPouchSnare(ultimate);
            case SIDE -> bird.specialPelicanBreakwaterRun(ultimate);
            case UP -> bird.specialPelicanThermalSail(ultimate);
            case DOWN -> bird.specialPelicanBilgeCommand(ultimate);
        }
    }

    static boolean active(Bird bird) {
        return bird.pelicanNeutralTimer > 0
                || bird.pelicanSideTimer > 0
                || bird.pelicanUpTimer > 0
                || bird.pelicanKeelDiveActive
                || bird.pelicanDownCharging
                || bird.pelicanBilgeFxTimer > 0
                || bird.pelicanMaelstromTimer > 0;
    }

    static boolean ready(Bird bird, Bird.PelicanSpecialVariant variant) {
        boolean ultimateReady = bird.isUltimateReady();
        return switch (variant) {
            case NEUTRAL -> ultimateReady || bird.pelicanNeutralReuseTimer <= 0;
            case SIDE -> ultimateReady || bird.pelicanSideReuseTimer <= 0;
            case UP -> ultimateReady || !bird.pelicanUpSpecialUsed;
            case DOWN -> ultimateReady || bird.pelicanDownReuseTimer <= 0;
        };
    }

    static boolean canConvertShieldIntoDown(Bird bird) {
        return bird.selectPelicanSpecialVariant() == Bird.PelicanSpecialVariant.DOWN
                && bird.isBlocking
                && bird.shieldStunFrames <= 0;
    }

    static boolean canStart(Bird bird, boolean grabbed, boolean dodging) {
        Bird.PelicanSpecialVariant variant = bird.selectPelicanSpecialVariant();
        boolean shieldConversion = canConvertShieldIntoDown(bird);
        return bird.type == BirdGame3.BirdType.PELICAN
                && bird.health > 0
                && bird.stunTime <= 0.0
                && !grabbed
                && (!bird.isBlocking || shieldConversion)
                && !dodging
                && !active(bird)
                && ready(bird, variant);
    }

    static void reset(Bird bird, boolean clearCargo) {
        bird.plungeTimer = 0;
        bird.pelicanNeutralTimer = 0;
        bird.pelicanNeutralUltimate = false;
        Arrays.fill(bird.pelicanNeutralHit, false);
        bird.pelicanSideTimer = 0;
        bird.pelicanSideCargoSpent = 0;
        bird.pelicanSideUltimate = false;
        Arrays.fill(bird.pelicanSideHit, false);
        bird.pelicanUpTimer = 0;
        bird.pelicanUpUltimate = false;
        bird.pelicanKeelDiveActive = false;
        Arrays.fill(bird.pelicanUpHit, false);
        bird.pelicanDownCharging = false;
        bird.pelicanDownHoldFrames = 0;
        bird.pelicanDownUltimate = false;
        bird.pelicanBilgeFxTimer = 0;
        bird.pelicanBilgeCargoSpent = 0;
        bird.pelicanBilgeUltimate = false;
        bird.pelicanMaelstromTimer = 0;
        bird.pelicanMaelstromPulseCooldown = 0;
        bird.pelicanMaelstromFinalResolved = false;
        bird.pelicanMaelstromCargoSpent = 0;
        bird.pelicanMaelstromX = 0.0;
        bird.pelicanMaelstromY = 0.0;
        bird.pelicanMaelstromGeyserFxTimer = 0;
        Arrays.fill(bird.pelicanMaelstromFinalHit, false);
        if (clearCargo) {
            bird.pelicanCargoCount = 0;
            bird.pelicanFullHoldTimer = 0;
        }
    }
}
