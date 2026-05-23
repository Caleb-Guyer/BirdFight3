package com.example.birdgame3;

import java.util.Arrays;

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
            case SIDE -> bird.specialTitmouseBarkskip();
            case UP -> bird.specialTitmouseTuftVault();
            case DOWN -> bird.specialTitmouseSeedStash();
        }
    }

    static boolean active(Bird bird) {
        return bird.titmouseScoldTimer > 0
                || bird.titmouseBarkskipTimer > 0
                || bird.titmouseVaultTimer > 0
                || bird.titmouseStashCharging
                || bird.titmouseMobbingTimer > 0;
    }

    static boolean ready(Bird bird, Bird.TitmouseSpecialVariant variant) {
        boolean ultimateReady = bird.isUltimateReady();
        return switch (variant) {
            case NEUTRAL -> ultimateReady || bird.titmouseScoldReuseTimer <= 0;
            case SIDE -> ultimateReady || bird.titmouseBarkskipReuseTimer <= 0;
            case UP -> ultimateReady || (!bird.titmouseVaultUsed && bird.titmouseVaultReuseTimer <= 0);
            case DOWN -> ultimateReady || bird.titmouseStashReuseTimer <= 0;
        };
    }

    static boolean canConvertShieldIntoDown(Bird bird) {
        return bird.selectTitmouseSpecialVariant() == Bird.TitmouseSpecialVariant.DOWN
                && bird.isBlocking
                && bird.shieldStunFrames <= 0;
    }

    static boolean canStart(Bird bird, boolean grabbed, boolean dodging) {
        Bird.TitmouseSpecialVariant variant = bird.selectTitmouseSpecialVariant();
        boolean shieldConversion = canConvertShieldIntoDown(bird);
        return bird.type == BirdGame3.BirdType.TITMOUSE
                && bird.health > 0
                && bird.stunTime <= 0.0
                && !grabbed
                && (!bird.isBlocking || shieldConversion)
                && !dodging
                && !active(bird)
                && ready(bird, variant);
    }

    static void reset(Bird bird, boolean clearObjects) {
        bird.titmouseScoldTimer = 0;
        bird.titmouseScoldUltimate = false;
        Arrays.fill(bird.titmouseScoldHit, false);
        bird.titmouseBarkskipTimer = 0;
        bird.titmouseBarkskipUltimate = false;
        bird.titmouseBarkskipRebounded = false;
        Arrays.fill(bird.titmouseBarkskipHit, false);
        bird.titmouseVaultTimer = 0;
        bird.titmouseVaultUltimate = false;
        bird.titmouseVaultBoosted = false;
        Arrays.fill(bird.titmouseVaultHit, false);
        bird.titmouseStashCharging = false;
        bird.titmouseStashHoldFrames = 0;
        bird.titmouseStashUltimate = false;
        bird.titmouseMobbingNodes.clear();
        bird.titmouseMobbingTimer = 0;
        bird.titmouseMobbingNodeIndex = 0;
        bird.isZipping = false;
        bird.zipTimer = 0;
        if (clearObjects) {
            bird.titmouseSeedStashes.clear();
        }
    }

    static void interruptOnHit(Bird bird) {
        if (bird.type != BirdGame3.BirdType.TITMOUSE) {
            return;
        }
        if (active(bird)) {
            bird.attackAnimationTimer = 0;
        }
        reset(bird, false);
    }
}
