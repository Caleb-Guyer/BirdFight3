package com.example.birdgame3;

import java.util.Arrays;

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

    static boolean active(Bird bird) {
        return bird.ravenQuillCharging
                || bird.ravenSideTimer > 0
                || bird.ravenLiftTimer > 0
                || bird.ravenUltimateWindupTimer > 0
                || bird.ravenUltimateTimer > 0;
    }

    static boolean ready(Bird bird, Bird.RavenSpecialVariant variant) {
        boolean ultimateReady = bird.isUltimateReady();
        return switch (variant) {
            case NEUTRAL -> ultimateReady || bird.ravenNeutralReuseTimer <= 0;
            case SIDE -> ultimateReady || bird.ravenSideReuseTimer <= 0;
            case UP -> ultimateReady || !bird.ravenLiftUsed;
            case DOWN -> ultimateReady || bird.hasRavenDecoy() || bird.ravenDownReuseTimer <= 0;
        };
    }

    static boolean canConvertShieldIntoDown(Bird bird) {
        return bird.selectRavenSpecialVariant() == Bird.RavenSpecialVariant.DOWN
                && bird.isBlocking
                && bird.shieldStunFrames <= 0;
    }

    static boolean canStart(Bird bird, boolean grabbed, boolean dodging) {
        Bird.RavenSpecialVariant variant = bird.selectRavenSpecialVariant();
        boolean shieldConversion = canConvertShieldIntoDown(bird);
        return bird.type == BirdGame3.BirdType.RAVEN
                && bird.health > 0
                && bird.stunTime <= 0.0
                && !grabbed
                && (!bird.isBlocking || shieldConversion)
                && !dodging
                && (!active(bird) || variant == Bird.RavenSpecialVariant.DOWN)
                && ready(bird, variant);
    }

    static void reset(Bird bird, boolean clearObjects) {
        bird.ravenQuillCharging = false;
        bird.ravenQuillChargeFrames = 0;
        bird.ravenQuillChargeUltimate = false;
        bird.ravenSideTimer = 0;
        bird.ravenSideUltimate = false;
        bird.ravenSideEmpowered = false;
        bird.ravenSideResolved = false;
        bird.ravenLiftTimer = 0;
        bird.ravenLiftUltimate = false;
        bird.ravenLiftSnapped = false;
        bird.ravenUltimateWindupTimer = 0;
        bird.ravenUltimateFlockTimer = 0;
        bird.ravenUltimateFlockSpawned = false;
        bird.ravenUltimateFinalTargetIndex = -1;
        bird.ravenUltimateTimer = 0;
        Arrays.fill(bird.ravenSideHit, false);
        Arrays.fill(bird.ravenLiftHit, false);
        if (clearObjects) {
            bird.clearRavenSpecialObjects();
        }
    }

    static void interruptOnHit(Bird bird) {
        if (bird.type != BirdGame3.BirdType.RAVEN) {
            return;
        }
        if (active(bird)) {
            bird.attackAnimationTimer = 0;
        }
        reset(bird, false);
    }
}
