package com.example.birdgame3;

import java.util.Arrays;

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

    static boolean active(Bird bird) {
        return bird.opiumSideTimer > 0
                || bird.opiumUpTimer > 0;
    }

    static boolean ready(Bird bird, Bird.OpiumSpecialVariant variant) {
        boolean ultimateReady = bird.isUltimateReady();
        return switch (variant) {
            case NEUTRAL -> ultimateReady || bird.opiumNeutralReuseTimer <= 0;
            case SIDE -> ultimateReady || bird.opiumSideReuseTimer <= 0;
            case UP -> ultimateReady || !bird.opiumUpSpecialUsed;
            case DOWN -> ultimateReady || bird.opiumDownReuseTimer <= 0;
        };
    }

    static boolean canConvertShieldIntoDown(Bird bird) {
        return bird.selectOpiumSpecialVariant() == Bird.OpiumSpecialVariant.DOWN
                && bird.isBlocking
                && bird.shieldStunFrames <= 0;
    }

    static boolean canStart(Bird bird, boolean grabbed, boolean dodging) {
        Bird.OpiumSpecialVariant variant = bird.selectOpiumSpecialVariant();
        boolean shieldConversion = canConvertShieldIntoDown(bird);
        return bird.isOpiumEchoPair()
                && bird.health > 0
                && bird.stunTime <= 0.0
                && !grabbed
                && (!bird.isBlocking || shieldConversion)
                && !dodging
                && !active(bird)
                && ready(bird, variant);
    }

    static void reset(Bird bird) {
        bird.opiumSideTimer = 0;
        bird.opiumSideDirection = bird.facingDirection();
        bird.opiumSideFueled = false;
        Arrays.fill(bird.opiumSideHit, false);
        bird.opiumUpTimer = 0;
        bird.opiumUpFueled = false;
        Arrays.fill(bird.opiumUpHit, false);
        bird.heisenUltimateVolleyTimer = 0;
        bird.heisenUltimateVolleyHit = false;
        bird.resetHeisenUltimateShardState(true);
    }

    static void interruptOnHit(Bird bird) {
        if (!bird.isOpiumEchoPair()) {
            return;
        }
        if (active(bird)) {
            bird.attackAnimationTimer = 0;
        }
        reset(bird);
    }
}
