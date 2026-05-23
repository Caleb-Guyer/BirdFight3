package com.example.birdgame3;

import java.util.Arrays;

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

    static boolean active(Bird bird) {
        return bird.batWingcutTimer > 0
                || bird.batMoonriseTimer > 0
                || bird.batSilentStallTimer > 0
                || bird.batSilentDiveTimer > 0
                || bird.batCathedralTimer > 0;
    }

    static boolean ready(Bird bird, Bird.BatSpecialVariant variant) {
        boolean ultimateReady = bird.isUltimateReady();
        return switch (variant) {
            case NEUTRAL -> ultimateReady || bird.batNeutralReuseTimer <= 0;
            case SIDE -> ultimateReady || bird.batWingcutReuseTimer <= 0;
            case UP -> ultimateReady || !bird.batMoonriseUsed;
            case DOWN -> ultimateReady || bird.batSilentReuseTimer <= 0;
        };
    }

    static boolean canConvertShieldIntoDown(Bird bird) {
        return bird.selectBatSpecialVariant() == Bird.BatSpecialVariant.DOWN
                && bird.isBlocking
                && bird.shieldStunFrames <= 0;
    }

    static boolean canStart(Bird bird, boolean grabbed, boolean dodging) {
        Bird.BatSpecialVariant variant = bird.selectBatSpecialVariant();
        boolean shieldConversion = canConvertShieldIntoDown(bird);
        return bird.type == BirdGame3.BirdType.BAT
                && bird.health > 0
                && bird.stunTime <= 0.0
                && !grabbed
                && (!bird.isBlocking || shieldConversion)
                && !dodging
                && !active(bird)
                && ready(bird, variant);
    }

    static void reset(Bird bird, boolean clearUltimate) {
        bird.batWingcutTimer = 0;
        bird.batWingcutUltimate = false;
        bird.batWingcutAmbush = false;
        bird.batWingcutFromHang = false;
        Arrays.fill(bird.batWingcutHit, false);
        bird.batMoonriseTimer = 0;
        bird.batMoonriseUltimate = false;
        bird.batMoonriseBurstResolved = false;
        bird.batMoonriseAmbush = false;
        Arrays.fill(bird.batMoonriseHit, false);
        bird.batSilentStallTimer = 0;
        bird.batSilentDiveTimer = 0;
        bird.batSilentFromHang = false;
        bird.batSilentUltimate = false;
        bird.batSilentAmbush = false;
        Arrays.fill(bird.batSilentHit, false);
        if (clearUltimate) {
            bird.batCathedralTimer = 0;
            bird.batCathedralPulseCooldown = 0;
            bird.batCathedralWaveIndex = 0;
        }
    }

    static void interruptOnHit(Bird bird) {
        if (bird.type != BirdGame3.BirdType.BAT) {
            return;
        }
        if (active(bird)) {
            bird.attackAnimationTimer = 0;
        }
        reset(bird, false);
    }
}
