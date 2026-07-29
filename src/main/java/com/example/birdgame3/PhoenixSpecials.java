package com.example.birdgame3;

import javafx.scene.paint.Color;

import java.util.Arrays;

final class PhoenixSpecials {
    private PhoenixSpecials() {
    }

    static final String REBIRTH_NOVA_MOVE = "Phoenix Rebirth Nova";

    static void use(Bird bird, boolean ultimate) {
        if (ultimate) {
            rebirthNova(bird);
            return;
        }
        switch (bird.selectPhoenixSpecialVariant()) {
            case NEUTRAL -> neutral(bird, false);
            case SIDE -> side(bird, false);
            case UP -> up(bird, false);
            case DOWN -> down(bird, false);
        }
    }

    static void rebirthNova(Bird bird) {
        reset(bird);
        bird.phoenixRebirthNovaTimer = Bird.PHOENIX_REBIRTH_NOVA_TOTAL_FRAMES;
        bird.phoenixRebirthNovaDetonated = false;
        bird.phoenixRebirthNovaBuffTimer = 0;
        Arrays.fill(bird.phoenixRebirthNovaHit, false);
        bird.phoenixNeutralReuseTimer = 0;
        bird.phoenixFireballReuseTimer = 0;
        bird.phoenixLavaReuseTimer = 0;
        bird.phoenixSpiralUsed = false;
        bird.specialCooldown = 0;
        bird.specialMaxCooldown = 0;
        bird.attackAnimationTimer = Math.max(bird.attackAnimationTimer, Bird.PHOENIX_REBIRTH_NOVA_WINDUP_FRAMES + 18);
        bird.vx *= 0.18;
        bird.vy = Math.min(bird.vy, -6.5);
        bird.game.addToKillFeed(bird.shortName() + " ASCENDS INTO REBIRTH NOVA!");
        bird.game.shakeIntensity = Math.max(bird.game.shakeIntensity, 18);
        bird.game.hitstopFrames = Math.max(bird.game.hitstopFrames, 8);
        bird.game.triggerFlash(0.62, false);
        spawnImpactBurst(bird, bird.bodyCenterX(), bird.bodyCenterY(),
                bird.scaledParticleCount(34), Color.web("#FFF8C4"), Color.web("#FF7043"));
    }

    static void neutral(Bird bird, boolean ultimate) {
        bird.phoenixCharging = true;
        bird.phoenixChargeTimer = 0;
        bird.phoenixChargeUltimate = ultimate;
        bird.specialCooldown = 0;
        bird.specialMaxCooldown = 0;
        bird.vx *= 0.45;
        if (!bird.isOnGround()) {
            bird.vy = Math.min(bird.vy, 1.2);
        }
        bird.game.addToKillFeed(bird.shortName() + (ultimate ? " CHANNELS SOLAR HALO!" : " CHANNELS CINDER HALO!"));
    }

    static void side(Bird bird, boolean ultimate) {
        int dir = bird.horizontalInputDirection();
        if (dir != 0) {
            bird.facingRight = dir > 0;
        }
        dir = bird.facingDirection();
        boolean airborne = !bird.isOnGround();
        int startupFrames = ultimate ? Bird.PHOENIX_FIREBALL_ULTIMATE_CAST_LOCK_FRAMES : Bird.PHOENIX_FIREBALL_CAST_LOCK_FRAMES;
        int flightFrames = ultimate ? Bird.PHOENIX_FIREBALL_ULTIMATE_FRAMES : Bird.PHOENIX_FIREBALL_FRAMES;
        bird.phoenixFireballTimer = startupFrames + flightFrames;
        bird.phoenixCastLockTimer = startupFrames;
        bird.phoenixFireballX = bird.bodyCenterX() + dir * 24.0 * bird.sizeMultiplier;
        bird.phoenixFireballY = bird.bodyCenterY() + (airborne ? 8.0 : -18.0) * bird.sizeMultiplier;
        bird.phoenixFireballVX = dir * (airborne ? (ultimate ? 12.8 : 10.8) : (ultimate ? 14.4 : 12.2));
        bird.phoenixFireballVY = airborne ? (ultimate ? 7.4 : 6.4) : 0.0;
        bird.phoenixFireballUltimate = ultimate;
        bird.phoenixFireballFizzleTimer = 0;
        if (airborne) {
            bird.phoenixAirSideAimPoseTimer = Math.max(bird.phoenixAirSideAimPoseTimer,
                    Bird.PHOENIX_AIR_SIDE_AIM_POSE_FRAMES);
            bird.phoenixAirSideLandingPrimeTimer = Bird.PHOENIX_AIR_SIDE_LANDING_PRIME_FRAMES;
        } else {
            bird.phoenixAirSideLandingPrimeTimer = 0;
        }
        bird.phoenixFireballReuseTimer = Math.max(bird.phoenixFireballReuseTimer,
                ultimate ? Bird.PHOENIX_FIREBALL_ULTIMATE_REUSE_FRAMES : Bird.PHOENIX_FIREBALL_REUSE_FRAMES);
        bird.specialCooldown = 0;
        bird.specialMaxCooldown = 0;
        bird.attackAnimationTimer = Math.max(bird.attackAnimationTimer, bird.phoenixCastLockTimer);
        bird.vx = 0.0;
        if (airborne) {
            bird.vy = Math.min(bird.vy, 1.0);
        }
        bird.game.addToKillFeed(bird.shortName() + (ultimate ? " SNAPS OFF A SOLAR SHOT!" : " SNAPS OFF A FIRE SHOT!"));
        bird.game.shakeIntensity = Math.max(bird.game.shakeIntensity, ultimate ? 8 : 6);
        bird.game.hitstopFrames = Math.max(bird.game.hitstopFrames, ultimate ? 4 : 3);
        spawnImpactBurst(bird, bird.bodyCenterX() + dir * 18.0 * bird.sizeMultiplier, bird.bodyCenterY() - 6.0 * bird.sizeMultiplier,
                ultimate ? 16 : 10,
                ultimate ? Color.web("#FFD180") : Color.GOLD,
                ultimate ? Color.web("#FF7043") : Color.ORANGERED);
    }

    static void up(Bird bird, boolean ultimate) {
        bird.phoenixSpiralTimer = ultimate ? Bird.PHOENIX_SPIRAL_ULTIMATE_FRAMES : Bird.PHOENIX_SPIRAL_FRAMES;
        bird.phoenixSpiralUsed = true;
        bird.phoenixSpiralUltimate = ultimate;
        Arrays.fill(bird.phoenixSpiralHitCooldown, 0);
        bird.canDoubleJump = true;
        bird.vy = -(ultimate ? 16 : 13);
        bird.vx = (bird.facingRight ? 1 : -1) * (ultimate ? 4 : 3);
        bird.specialCooldown = 0;
        bird.specialMaxCooldown = 0;
        bird.game.addToKillFeed(bird.shortName() + (ultimate ? " IGNITES HELIX ASCENT!" : " IGNITES A FIRESPIN!"));
        bird.game.shakeIntensity = Math.max(bird.game.shakeIntensity, ultimate ? 12 : 9);
        bird.game.hitstopFrames = Math.max(bird.game.hitstopFrames, ultimate ? 6 : 4);
    }

    static void down(Bird bird, boolean ultimate) {
        boolean airborne = !bird.isOnGround();
        bird.phoenixLavaTimer = ultimate ? Bird.PHOENIX_LAVA_ULTIMATE_FRAMES : Bird.PHOENIX_LAVA_FRAMES;
        bird.phoenixLavaAirborne = airborne;
        bird.phoenixLavaHoldFrames = 0;
        bird.phoenixLavaX = bird.bodyCenterX();
        Platform support = airborne ? null : bird.findCurrentSupportPlatform();
        bird.phoenixLavaY = airborne
                ? bird.bodyBottomY() - 6.0 * bird.sizeMultiplier
                : ((support != null ? support.y : BirdGame3.GROUND_Y) - 10.0);
        bird.phoenixLavaUltimate = ultimate;
        bird.phoenixCastLockTimer = ultimate ? Bird.PHOENIX_LAVA_ULTIMATE_CAST_LOCK_FRAMES : Bird.PHOENIX_LAVA_CAST_LOCK_FRAMES;
        Arrays.fill(bird.phoenixLavaHitCooldown, false);
        bird.phoenixLavaReuseTimer = Math.max(bird.phoenixLavaReuseTimer,
                ultimate ? Bird.PHOENIX_LAVA_ULTIMATE_REUSE_FRAMES : Bird.PHOENIX_LAVA_REUSE_FRAMES);
        bird.specialCooldown = 0;
        bird.specialMaxCooldown = 0;
        bird.attackAnimationTimer = Math.max(bird.attackAnimationTimer, bird.phoenixCastLockTimer);
        if (airborne) {
            bird.vx *= 0.25;
            bird.vy = Math.max(bird.vy, 0.0);
            bird.game.addToKillFeed(bird.shortName() + (ultimate ? " POURS A SOLAR COLUMN!" : " POURS A FLAME COLUMN!"));
        } else {
            bird.vx = 0.0;
            bird.vy = 0.0;
            bird.game.addToKillFeed(bird.shortName() + (ultimate ? " CRACKS THE FLOOR OPEN!" : " CRACKS THE FLOOR!"));
        }
        bird.game.shakeIntensity = Math.max(bird.game.shakeIntensity, ultimate ? 10 : 7);
        bird.game.hitstopFrames = Math.max(bird.game.hitstopFrames, ultimate ? 5 : 3);
        spawnImpactBurst(bird, bird.phoenixLavaX, bird.phoenixLavaY - 8.0,
                ultimate ? 22 : 14,
                ultimate ? Color.web("#FFD180") : Color.GOLD,
                ultimate ? Color.web("#FF7043") : Color.ORANGERED);
    }

    static void handleState(Bird bird) {
        if (bird.type != BirdGame3.BirdType.PHOENIX && !bird.mockingbirdCopiedNeutralFrom(BirdGame3.BirdType.PHOENIX)) {
            return;
        }
        if (bird.type == BirdGame3.BirdType.PHOENIX) {
            handleRebirthNova(bird);
            handleRebirthBuff(bird);
        }
        if (bird.stunTime > 0.0) {
            reset(bird);
            if (bird.mockingbirdCopiedNeutralFrom(BirdGame3.BirdType.PHOENIX)) {
                bird.mockingbirdCopiedNeutralSource = null;
            }
            return;
        }
        if (bird.phoenixCharging) {
            handleCharge(bird);
        }
        updateAirSideLandingCue(bird);
        if (bird.phoenixFireballTimer > 0) {
            handleFireball(bird);
        }
        if (bird.phoenixFireballFizzleTimer > 0) {
            handleFireballFizzle(bird);
        }
        if (bird.phoenixSpiralTimer > 0) {
            handleSpiral(bird);
        }
        if (bird.phoenixLavaTimer > 0) {
            handleLava(bird);
        }
    }

    static boolean active(Bird bird) {
        return bird.phoenixCharging
                || bird.phoenixRebirthNovaTimer > 0
                || bird.phoenixFireballTimer > 0
                || bird.phoenixSpiralTimer > 0
                || bird.phoenixLavaTimer > 0;
    }

    static boolean ready(Bird bird, Bird.PhoenixSpecialVariant variant) {
        if (bird.isUltimateReady()) {
            return true;
        }
        return switch (variant) {
            case NEUTRAL -> bird.specialCooldown <= 0 && bird.phoenixNeutralReuseTimer <= 0;
            case SIDE -> bird.phoenixFireballReuseTimer <= 0;
            case UP -> !bird.phoenixSpiralUsed;
            case DOWN -> bird.phoenixLavaReuseTimer <= 0;
        };
    }

    static void reset(Bird bird) {
        bird.phoenixCharging = false;
        bird.phoenixChargeTimer = 0;
        bird.phoenixChargeUltimate = false;
        bird.phoenixBurstFxTimer = 0;
        bird.phoenixBurstFxUltimate = false;
        bird.phoenixBurstFxChargeRatio = 0.0;
        bird.phoenixCastLockTimer = 0;
        bird.phoenixFireballTimer = 0;
        bird.phoenixFireballUltimate = false;
        bird.phoenixFireballFizzleTimer = 0;
        bird.phoenixFireballVX = 0.0;
        bird.phoenixFireballVY = 0.0;
        bird.phoenixAirSideAimPoseTimer = 0;
        bird.phoenixAirSideLandingPrimeTimer = 0;
        bird.phoenixAirSideLandingFxTimer = 0;
        bird.phoenixSpiralTimer = 0;
        bird.phoenixSpiralUltimate = false;
        Arrays.fill(bird.phoenixSpiralHitCooldown, 0);
        bird.phoenixLavaTimer = 0;
        bird.phoenixLavaUltimate = false;
        bird.phoenixLavaAirborne = false;
        bird.phoenixLavaHoldFrames = 0;
        Arrays.fill(bird.phoenixLavaHitCooldown, false);
        bird.phoenixRebirthNovaTimer = 0;
        bird.phoenixRebirthNovaDetonated = false;
        Arrays.fill(bird.phoenixRebirthNovaHit, false);
    }

    private static void handleRebirthNova(Bird bird) {
        if (bird.phoenixRebirthNovaTimer <= 0) {
            bird.phoenixRebirthNovaDetonated = false;
            Arrays.fill(bird.phoenixRebirthNovaHit, false);
            return;
        }

        int elapsed = Bird.PHOENIX_REBIRTH_NOVA_TOTAL_FRAMES - bird.phoenixRebirthNovaTimer;
        bird.vx *= 0.68;
        if (!bird.phoenixRebirthNovaDetonated) {
            bird.vy = Math.min(bird.vy * 0.72, -1.8);
            bird.y = Math.max(BirdGame3.CEILING_Y + 68.0, bird.y - 0.62 * bird.sizeMultiplier);
        } else {
            bird.vy = Math.min(bird.vy + 0.08, 2.2);
        }

        emitRebirthNovaChargeParticles(bird, elapsed);

        if (!bird.phoenixRebirthNovaDetonated
                && elapsed >= Bird.PHOENIX_REBIRTH_NOVA_WINDUP_FRAMES) {
            detonateRebirthNova(bird);
        }

        bird.phoenixRebirthNovaTimer--;
        if (bird.phoenixRebirthNovaTimer <= 0) {
            bird.phoenixRebirthNovaTimer = 0;
            bird.phoenixRebirthNovaDetonated = false;
            Arrays.fill(bird.phoenixRebirthNovaHit, false);
        }
    }

    private static void handleRebirthBuff(Bird bird) {
        if (bird.phoenixRebirthNovaBuffTimer <= 0) {
            return;
        }
        bird.phoenixRebirthNovaBuffTimer--;
        if (bird.phoenixNeutralReuseTimer > 0) bird.phoenixNeutralReuseTimer--;
        if (bird.phoenixFireballReuseTimer > 0) bird.phoenixFireballReuseTimer--;
        if (bird.phoenixLavaReuseTimer > 0) bird.phoenixLavaReuseTimer--;
        if ((bird.phoenixRebirthNovaBuffTimer & 7) == 0) {
            double angle = -Math.PI / 2.0 + (bird.game.nextParticleRandom() - 0.5) * 1.7;
            double radius = (18.0 + bird.game.nextParticleRandom() * 38.0) * bird.sizeMultiplier;
            Color c = bird.game.nextParticleRandom() < 0.44 ? Color.web("#FFF8C4") : Color.web("#FF7043");
            bird.game.particles.add(new Particle(
                    bird.bodyCenterX() + (bird.game.nextParticleRandom() - 0.5) * radius,
                    bird.bodyCenterY() + (bird.game.nextParticleRandom() - 0.5) * radius * 0.7,
                    Math.cos(angle) * (1.2 + bird.game.nextParticleRandom() * 2.4),
                    Math.sin(angle) * (2.8 + bird.game.nextParticleRandom() * 4.0),
                    c.deriveColor(0, 1, 1, 0.72)
            ));
        }
    }

    private static void emitRebirthNovaChargeParticles(Bird bird, int elapsed) {
        if ((elapsed & 1) != 0) {
            return;
        }
        double charge = Math.clamp(elapsed / (double) Bird.PHOENIX_REBIRTH_NOVA_WINDUP_FRAMES, 0.0, 1.0);
        int count = bird.phoenixRebirthNovaDetonated ? 3 : 3 + (int) Math.round(charge * 3.0);
        for (int i = 0; i < count; i++) {
            double angle = bird.game.nextParticleRandom() * Math.PI * 2.0;
            double orbit = (42.0 + charge * 152.0 + bird.game.nextParticleRandom() * 46.0) * bird.sizeMultiplier;
            double inward = bird.phoenixRebirthNovaDetonated ? -1.0 : 1.0;
            Color c = i % 3 == 0 ? Color.web("#FFF8C4")
                    : (bird.game.nextParticleRandom() < 0.5 ? Color.GOLD : Color.web("#FF7043"));
            bird.game.particles.add(new Particle(
                    bird.bodyCenterX() + Math.cos(angle) * orbit,
                    bird.bodyCenterY() + Math.sin(angle) * orbit * 0.76,
                    -Math.cos(angle) * (2.0 + charge * 5.2) * inward,
                    -Math.sin(angle) * (1.4 + charge * 4.0) * inward - 2.0,
                    c.deriveColor(0, 1, 1, 0.86)
            ));
        }
    }

    private static void detonateRebirthNova(Bird bird) {
        bird.phoenixRebirthNovaDetonated = true;
        bird.phoenixRebirthNovaBuffTimer = Bird.PHOENIX_REBIRTH_NOVA_BUFF_FRAMES;
        bird.heal(28.0);
        bird.phoenixNeutralReuseTimer = 0;
        bird.phoenixFireballReuseTimer = 0;
        bird.phoenixLavaReuseTimer = 0;
        bird.phoenixSpiralUsed = false;
        bird.canDoubleJump = true;
        bird.vx = 0.0;
        bird.vy = Math.min(bird.vy, -8.5);
        bird.game.shakeIntensity = Math.max(bird.game.shakeIntensity, 34);
        bird.game.hitstopFrames = Math.max(bird.game.hitstopFrames, 12);
        bird.game.triggerFlash(0.86, false);
        bird.game.addToKillFeed(bird.shortName() + " DETONATED REBIRTH NOVA!");

        double radius = Bird.PHOENIX_REBIRTH_NOVA_RADIUS * bird.sizeMultiplier;
        for (Bird other : bird.game.players) {
            if (!bird.canDamageTarget(other)) continue;
            if (other.playerIndex < 0 || other.playerIndex >= bird.phoenixRebirthNovaHit.length) continue;
            if (bird.phoenixRebirthNovaHit[other.playerIndex]) continue;

            double dx = other.bodyCenterX() - bird.bodyCenterX();
            double dy = other.bodyCenterY() - bird.bodyCenterY();
            double dist = Math.hypot(dx, dy);
            if (dist > radius + other.combatRadius()) continue;

            bird.phoenixRebirthNovaHit[other.playerIndex] = true;
            double proximity = 1.0 - Math.clamp(dist / Math.max(1.0, radius), 0.0, 1.0);
            int damage = 20 + (int) Math.round(proximity * 8.0);
            double oldHealth = other.health;
            int dealt = (int) bird.applyDamageTo(other, damage);
            if (dealt <= 0) continue;

            bird.game.damageDealt[bird.playerIndex] += dealt;
            bird.game.recordSpecialImpact(bird.playerIndex, dealt, true);
            if (other.health <= 0 && oldHealth > 0) {
                bird.game.eliminations[bird.playerIndex]++;
            }

            double safeDist = Math.max(1.0, dist);
            double launch = 14.0 + proximity * 9.0;
            other.vx += dx / safeDist * launch;
            other.vy -= 12.0 + proximity * 7.0;
            other.applyStun(18 + (int) Math.round(proximity * 10.0));
            spawnImpactBurst(bird, other.bodyCenterX(), other.bodyCenterY(),
                    bird.scaledParticleCount(28), Color.web("#FFF8C4"), Color.web("#FF3D00"));
        }

        int burstCount = bird.scaledParticleCount(72);
        for (int i = 0; i < burstCount; i++) {
            double angle = bird.game.nextParticleRandom() * Math.PI * 2.0;
            double speed = 7.0 + bird.game.nextParticleRandom() * 17.0;
            Color c = i % 4 == 0 ? Color.WHITE
                    : i % 3 == 0 ? Color.web("#FFF8C4")
                    : bird.game.nextParticleRandom() < 0.55 ? Color.GOLD : Color.web("#FF3D00");
            bird.game.particles.add(new Particle(
                    bird.bodyCenterX() + Math.cos(angle) * 22.0 * bird.sizeMultiplier,
                    bird.bodyCenterY() + Math.sin(angle) * 22.0 * bird.sizeMultiplier,
                    Math.cos(angle) * speed,
                    Math.sin(angle) * speed - 4.0,
                    c.deriveColor(0, 1, 1, 0.92)
            ));
        }
    }

    private static void handleCharge(Bird bird) {
        bird.phoenixChargeTimer = Math.min(Bird.PHOENIX_CHARGE_MAX_FRAMES, bird.phoenixChargeTimer + 1);
        int inputDir = bird.horizontalInputDirection();
        if (inputDir != 0) {
            bird.facingRight = inputDir > 0;
            bird.vx = Math.clamp(bird.vx * 0.82 + inputDir * 0.34, -3.2, 3.2);
        } else {
            bird.vx *= 0.84;
        }
        if (!bird.specialHeld()) {
            releaseCharge(bird);
            return;
        }
        if (bird.phoenixChargeTimer >= Bird.PHOENIX_CHARGE_MAX_FRAMES) {
            releaseCharge(bird);
            return;
        }
        bird.vy = Math.min(bird.vy, bird.phoenixChargeUltimate ? -1.6 : -1.0);
        bird.y -= bird.phoenixChargeUltimate ? 0.36 : 0.28;
        if (bird.y < 100) bird.y = 100;

        double chargeRatio = Math.clamp(bird.phoenixChargeTimer / (double) Bird.PHOENIX_CHARGE_MAX_FRAMES, 0.0, 1.0);
        int emberCount = bird.phoenixChargeUltimate ? 4 : 3;
        for (int i = 0; i < emberCount; i++) {
            double angle = bird.game.nextParticleRandom() * Math.PI * 2;
            double orbit = (16 + bird.game.nextParticleRandom() * 24 + chargeRatio * 34) * bird.sizeMultiplier;
            double lift = 0.5 + chargeRatio * 1.4;
            Color ember = bird.phoenixChargeUltimate && bird.game.nextParticleRandom() < 0.45
                    ? Color.web("#FFF3B0")
                    : (bird.game.nextParticleRandom() < 0.6 ? Color.ORANGERED : Color.GOLD);
            bird.game.particles.add(new Particle(
                    bird.bodyCenterX() + Math.cos(angle) * orbit,
                    bird.bodyCenterY() + Math.sin(angle) * orbit * 0.72,
                    -Math.sin(angle) * (1.2 + chargeRatio * 1.8),
                    -lift - bird.game.nextParticleRandom() * 1.8,
                    ember.deriveColor(0, 1, 1, 0.82)
            ));
        }
    }

    static void releaseCharge(Bird bird) {
        bird.phoenixCharging = false;
        boolean ultimate = bird.phoenixChargeUltimate;
        bird.phoenixChargeUltimate = false;
        int chargeLevel = Math.min(bird.phoenixChargeTimer / 30, 3);
        double chargeRatio = Math.clamp(bird.phoenixChargeTimer / (double) Bird.PHOENIX_CHARGE_MAX_FRAMES, 0.0, 1.0);
        int damage = 4 + (int) Math.round(chargeRatio * 10.0);
        if (ultimate) {
            damage += 3 + (int) Math.round(chargeRatio * 4.0);
        }

        double radius = 76.0 + chargeRatio * 124.0;
        if (ultimate) radius *= 1.22;

        bird.specialCooldown = 0;
        bird.specialMaxCooldown = 0;
        bird.phoenixNeutralReuseTimer = Math.max(bird.phoenixNeutralReuseTimer,
                ultimate ? Bird.PHOENIX_NEUTRAL_ULTIMATE_REUSE_FRAMES : Bird.PHOENIX_NEUTRAL_REUSE_FRAMES);
        bird.phoenixBurstFxTimer = Bird.PHOENIX_BURST_FX_FRAMES;
        bird.phoenixBurstFxUltimate = ultimate;
        bird.phoenixBurstFxChargeRatio = chargeRatio;
        bird.phoenixAfterburnTimer = 0;
        Arrays.fill(bird.phoenixAfterburnHitCooldown, 0);
        bird.heal(2.0 + chargeRatio * 4.0 + (ultimate ? 3.0 : 0.0));

        String burstName = ultimate
                ? (chargeLevel == 3 ? "SOLAR NOVA" : "SOLAR BURST")
                : (chargeLevel == 3 ? "CINDER NOVA" : "FIRE BURST");
        bird.game.addToKillFeed(bird.shortName() + " UNLEASHES " + burstName + "!");
        bird.game.shakeIntensity = Math.max(bird.game.shakeIntensity, 8 + chargeLevel * 2 + (ultimate ? 3 : 0));
        bird.game.hitstopFrames = Math.max(bird.game.hitstopFrames, 4 + chargeLevel + (ultimate ? 2 : 0));
        bird.game.triggerFlash(0.22 + chargeRatio * 0.20 + (ultimate ? 0.12 : 0.0), false);

        for (Bird other : bird.game.players) {
            if (!bird.canDamageTarget(other)) continue;
            double dx = other.bodyCenterX() - bird.bodyCenterX();
            double dy = other.bodyCenterY() - bird.bodyCenterY();
            double dist = Math.hypot(dx, dy);
            if (dist > radius + other.combatRadius()) continue;

            double oldHealth = other.health;
            int dealt = (int) bird.applyDamageTo(other, damage);
            if (dealt <= 0) continue;

            bird.game.damageDealt[bird.playerIndex] += dealt;
            bird.game.recordSpecialImpact(bird.playerIndex, dealt, true);
            if (other.health <= 0 && oldHealth > 0) bird.game.eliminations[bird.playerIndex]++;

            double safeDist = Math.max(0.001, dist);
            double kb = 3.8 + chargeRatio * 7.4;
            if (ultimate) kb *= 1.3;
            other.vx += dx / safeDist * kb;
            other.vy -= kb * 0.8;

            for (int i = 0; i < 10 + chargeLevel * 4; i++) {
                double angle = bird.game.nextParticleRandom() * Math.PI * 2;
                Color spark = ultimate && bird.game.nextParticleRandom() < 0.35 ? Color.web("#FFF3B0") : (bird.game.nextParticleRandom() < 0.5 ? Color.ORANGERED : Color.GOLD);
                bird.game.particles.add(new Particle(
                        other.bodyCenterX(),
                        other.bodyCenterY(),
                        Math.cos(angle) * (3 + bird.game.nextParticleRandom() * 5) + dx / safeDist * 1.8,
                        Math.sin(angle) * (3 + bird.game.nextParticleRandom() * 5) - 2.6,
                        spark.deriveColor(0, 1, 1, 0.9)
                ));
            }
        }

        int particleCount = 8 + chargeLevel * 4;
        for (int i = 0; i < particleCount; i++) {
            double angle = bird.game.nextParticleRandom() * Math.PI * 2;
            double speed = 3.0 + bird.game.nextParticleRandom() * 4.5;
            Color c = ultimate && bird.game.nextParticleRandom() < 0.35
                    ? Color.web("#FFF3B0")
                    : (bird.game.nextParticleRandom() < 0.5 ? Color.ORANGERED : Color.GOLD);
            bird.game.particles.add(new Particle(
                    bird.bodyCenterX() + Math.cos(angle) * 20,
                    bird.bodyCenterY() + Math.sin(angle) * 20,
                    Math.cos(angle) * speed,
                    Math.sin(angle) * speed - 2.2,
                    c.deriveColor(0, 1, 1, 0.9)
            ));
        }

        bird.phoenixChargeTimer = 0;
    }

    private static void updateAirSideLandingCue(Bird bird) {
        if (bird.phoenixAirSideLandingPrimeTimer <= 0 || !bird.isOnGround()) {
            return;
        }
        bird.phoenixAirSideLandingPrimeTimer = 0;
        bird.phoenixAirSideAimPoseTimer = 0;
        bird.phoenixAirSideLandingFxTimer = Math.max(bird.phoenixAirSideLandingFxTimer,
                Bird.PHOENIX_AIR_SIDE_LANDING_FX_FRAMES);
        bird.game.shakeIntensity = Math.max(bird.game.shakeIntensity, 2.8);
        double footY = bird.bodyBottomY() - 4.0 * bird.sizeMultiplier;
        double centerX = bird.bodyCenterX();
        for (int i = 0; i < 12; i++) {
            double side = (i < 6 ? -1.0 : 1.0);
            double speed = 1.6 + bird.game.nextParticleRandom() * 3.2;
            Color spark = i % 3 == 0 ? Color.GOLD : Color.web("#FF7043");
            bird.game.particles.add(new Particle(
                    centerX + (bird.game.nextParticleRandom() - 0.5) * 30.0 * bird.sizeMultiplier,
                    footY + (bird.game.nextParticleRandom() - 0.5) * 8.0 * bird.sizeMultiplier,
                    side * speed + (bird.game.nextParticleRandom() - 0.5) * 0.8,
                    -1.8 - bird.game.nextParticleRandom() * 2.8,
                    spark.deriveColor(0, 1, 1, 0.82)
            ));
        }
    }

    private static void handleFireball(Bird bird) {
        if (bird.phoenixCastLockTimer > 0) {
            bird.vx = 0.0;
            if (!bird.isOnGround()) {
                bird.vy = Math.min(bird.vy, 1.1);
            }
            int dir = bird.facingDirection();
            boolean downwardShot = bird.phoenixFireballVY > 0.0;
            double startupFrames = bird.phoenixFireballUltimate
                    ? Bird.PHOENIX_FIREBALL_ULTIMATE_CAST_LOCK_FRAMES
                    : Bird.PHOENIX_FIREBALL_CAST_LOCK_FRAMES;
            double windup = 1.0 - Math.clamp(bird.phoenixCastLockTimer / startupFrames, 0.0, 1.0);
            bird.phoenixFireballX = bird.bodyCenterX() + dir * (24.0 + windup * 14.0) * bird.sizeMultiplier;
            bird.phoenixFireballY = downwardShot
                    ? bird.bodyCenterY() + (6.0 + windup * 16.0) * bird.sizeMultiplier
                    : bird.bodyCenterY() - (18.0 + windup * 18.0) * bird.sizeMultiplier;
            if ((bird.phoenixCastLockTimer & 1) == 0) {
                Color c = bird.phoenixFireballUltimate ? Color.web("#FFD180") : Color.GOLD;
                for (int i = 0; i < 3; i++) {
                    bird.game.particles.add(new Particle(
                            bird.phoenixFireballX - dir * (4.0 + bird.game.nextParticleRandom() * 10.0) * bird.sizeMultiplier,
                            bird.phoenixFireballY + (bird.game.nextParticleRandom() - 0.5) * 14.0 * bird.sizeMultiplier,
                            -dir * (0.6 + bird.game.nextParticleRandom() * 1.4),
                            downwardShot ? 1.1 + bird.game.nextParticleRandom() * 1.8 : -2.0 - bird.game.nextParticleRandom() * 2.2,
                            c.deriveColor(0, 1, 1, 0.82)
                    ));
                }
            }
            return;
        }
        if (bird.phoenixFireballVY > 0.0) {
            bird.phoenixAirSideAimPoseTimer = Math.max(bird.phoenixAirSideAimPoseTimer, 6);
        }
        bird.phoenixFireballX += bird.phoenixFireballVX;
        bird.phoenixFireballY += bird.phoenixFireballVY;

        double radius = (bird.phoenixFireballUltimate ? 34.0 : 28.0) * bird.sizeMultiplier;
        boolean hitTarget = false;

        for (Bird other : bird.game.players) {
            if (!bird.canDamageTarget(other)) continue;
            if (other.playerIndex == bird.playerIndex) continue;

            double dx = other.bodyCenterX() - bird.phoenixFireballX;
            double dy = other.bodyCenterY() - bird.phoenixFireballY;
            double dist = Math.hypot(dx, dy);
            if (dist > radius + other.combatRadius()) continue;

            int dmg = bird.phoenixFireballUltimate ? 10 : 7;
            double oldHealth = other.health;
            int dealt = (int) bird.applyDamageTo(other, dmg);
            if (dealt <= 0) continue;

            bird.game.damageDealt[bird.playerIndex] += dealt;
            bird.game.recordSpecialImpact(bird.playerIndex, dealt, true);
            if (other.health <= 0 && oldHealth > 0) bird.game.eliminations[bird.playerIndex]++;

            int dir = bird.phoenixFireballVX < 0.0 ? -1 : 1;
            other.vx += dir * (bird.phoenixFireballUltimate ? 8.8 : 6.9);
            if (bird.phoenixFireballVY > 0.0) {
                other.vy += bird.phoenixFireballUltimate ? 5.4 : 4.2;
            } else {
                other.vy -= bird.phoenixFireballUltimate ? 6.3 : 4.8;
            }
            spawnImpactBurst(bird, bird.phoenixFireballX, bird.phoenixFireballY,
                    bird.phoenixFireballUltimate ? 26 : 18,
                    bird.phoenixFireballUltimate ? Color.web("#FFD180") : Color.GOLD,
                    bird.phoenixFireballUltimate ? Color.web("#FF7043") : Color.ORANGERED);
            bird.game.shakeIntensity = Math.max(bird.game.shakeIntensity, bird.phoenixFireballUltimate ? 8 : 6);
            bird.game.hitstopFrames = Math.max(bird.game.hitstopFrames, bird.phoenixFireballUltimate ? 4 : 3);

            bird.phoenixFireballTimer = 0;
            hitTarget = true;
            break;
        }
        if (hitTarget) {
            return;
        }

        if (bird.phoenixFireballX < -100 || bird.phoenixFireballX > BirdGame3.WORLD_WIDTH + 100
                || bird.phoenixFireballY < -120 || bird.phoenixFireballY > BirdGame3.WORLD_HEIGHT + 120) {
            startFireballFizzle(bird);
            return;
        }

        if (bird.phoenixFireballTimer % 2 == 0) {
            double speed = Math.max(1.0, Math.hypot(bird.phoenixFireballVX, bird.phoenixFireballVY));
            double ux = bird.phoenixFireballVX / speed;
            double uy = bird.phoenixFireballVY / speed;
            double nx = -uy;
            double ny = ux;
            for (int i = 0; i < 5; i++) {
                double lateral = (bird.game.nextParticleRandom() - 0.5) * 22.0;
                double back = 12.0 + bird.game.nextParticleRandom() * 24.0;
                Color c = i == 0
                        ? (bird.phoenixFireballUltimate ? Color.web("#FFD180") : Color.GOLD)
                        : (bird.phoenixFireballUltimate ? Color.web("#FF7043") : Color.ORANGE);
                bird.game.particles.add(new Particle(
                        bird.phoenixFireballX - ux * back + nx * lateral,
                        bird.phoenixFireballY - uy * back + ny * lateral,
                        (bird.game.nextParticleRandom() - 0.5) * 1.4 - bird.phoenixFireballVX * 0.16,
                        (bird.game.nextParticleRandom() - 0.5) * 1.4 - bird.phoenixFireballVY * 0.08 - 1.0,
                        c.deriveColor(0, 1, 1, 0.86)
                ));
            }
        }

        if (bird.phoenixFireballTimer <= 1) {
            startFireballFizzle(bird);
        }
    }

    private static void startFireballFizzle(Bird bird) {
        if (bird.phoenixFireballFizzleTimer <= 0) {
            bird.phoenixFireballFizzleTimer = bird.phoenixFireballUltimate
                    ? Bird.PHOENIX_FIREBALL_ULTIMATE_FIZZLE_FRAMES
                    : Bird.PHOENIX_FIREBALL_FIZZLE_FRAMES;
        }
        bird.phoenixFireballTimer = 0;
        bird.phoenixCastLockTimer = 0;
        bird.phoenixFireballVX *= 0.20;
        bird.phoenixFireballVY = bird.phoenixFireballVY * 0.18 + 0.42;
    }

    private static void handleFireballFizzle(Bird bird) {
        bird.phoenixFireballX += bird.phoenixFireballVX;
        bird.phoenixFireballY += bird.phoenixFireballVY;
        bird.phoenixFireballVX *= 0.72;
        bird.phoenixFireballVY = Math.min(3.4, bird.phoenixFireballVY * 0.78 + 0.16);

        int totalFrames = bird.phoenixFireballUltimate
                ? Bird.PHOENIX_FIREBALL_ULTIMATE_FIZZLE_FRAMES
                : Bird.PHOENIX_FIREBALL_FIZZLE_FRAMES;
        double fade = Math.clamp(bird.phoenixFireballFizzleTimer / (double) totalFrames, 0.0, 1.0);
        int count = bird.phoenixFireballUltimate ? 5 : 4;
        for (int i = 0; i < count; i++) {
            double angle = bird.game.nextParticleRandom() * Math.PI * 2.0;
            double speed = 0.8 + bird.game.nextParticleRandom() * (2.2 + fade * 1.8);
            Color c = i == 0
                    ? (bird.phoenixFireballUltimate ? Color.web("#FFD180") : Color.GOLD)
                    : (bird.game.nextParticleRandom() < 0.5 ? Color.web("#FF7043") : Color.web("#4E342E"));
            bird.game.particles.add(new Particle(
                    bird.phoenixFireballX + Math.cos(angle) * (5.0 + bird.game.nextParticleRandom() * 13.0) * bird.sizeMultiplier,
                    bird.phoenixFireballY + Math.sin(angle) * (5.0 + bird.game.nextParticleRandom() * 13.0) * bird.sizeMultiplier,
                    Math.cos(angle) * speed + bird.phoenixFireballVX * 0.18,
                    Math.sin(angle) * speed - 1.0 + (1.0 - fade) * 0.8,
                    c.deriveColor(0, 1, 1, 0.34 + fade * 0.52)
            ));
        }
    }

    private static void handleSpiral(Bird bird) {
        int totalFrames = bird.phoenixSpiralUltimate ? Bird.PHOENIX_SPIRAL_ULTIMATE_FRAMES : Bird.PHOENIX_SPIRAL_FRAMES;
        double spiralProgress = bird.phoenixSpecialPhase(bird.phoenixSpiralTimer, totalFrames);
        double angle = spiralProgress * Math.PI * 4;
        double radius = (bird.phoenixSpiralUltimate ? 44.0 : 38.0) * bird.sizeMultiplier;
        int inputDir = bird.horizontalInputDirection();
        if (inputDir != 0) {
            bird.facingRight = inputDir > 0;
        }
        double steer = bird.phoenixSpiralUltimate ? 0.68 : 0.52;
        double maxHorizontal = bird.phoenixSpiralUltimate ? 8.6 : 7.2;
        bird.vx = Math.clamp(bird.vx * 0.94 + Math.cos(angle) * 0.72 + inputDir * steer, -maxHorizontal, maxHorizontal);
        bird.vy = Math.min(bird.vy - (bird.phoenixSpiralUltimate ? 0.58 : 0.46), bird.phoenixSpiralUltimate ? -11.6 : -9.8);

        for (Bird other : bird.game.players) {
            if (!bird.canDamageTarget(other)) continue;
            if (other.playerIndex == bird.playerIndex) continue;
            if (other.playerIndex < 0 || other.playerIndex >= bird.phoenixSpiralHitCooldown.length) continue;

            double dx = other.bodyCenterX() - bird.bodyCenterX();
            double dy = other.bodyCenterY() - bird.bodyCenterY();
            double horizontalReach = (bird.phoenixSpiralUltimate ? 56.0 : 48.0) * bird.sizeMultiplier + other.combatHalfWidth();
            double lowerReach = (bird.phoenixSpiralUltimate ? 58.0 : 48.0) * bird.sizeMultiplier + other.combatHalfHeight();
            double upperReach = (bird.phoenixSpiralUltimate ? 150.0 : 128.0) * bird.sizeMultiplier + other.combatHalfHeight();
            if (Math.abs(dx) > horizontalReach) continue;
            if (dy > lowerReach || dy < -upperReach) continue;

            double pull = Math.clamp(-dx * (bird.phoenixSpiralUltimate ? 0.10 : 0.085), -2.6, 2.6);
            other.vx += pull;
            other.vy = Math.min(other.vy, bird.phoenixSpiralUltimate ? -9.2 : -7.6);

            if (bird.phoenixSpiralHitCooldown[other.playerIndex] > 0) {
                continue;
            }

            int dmg = bird.phoenixSpiralUltimate ? 3 : 2;
            double oldHealth = other.health;
            int dealt = (int) bird.applyDamageTo(other, dmg);
            if (dealt <= 0) continue;

            bird.game.damageDealt[bird.playerIndex] += dealt;
            bird.game.recordSpecialImpact(bird.playerIndex, dealt, true);
            if (other.health <= 0 && oldHealth > 0) bird.game.eliminations[bird.playerIndex]++;

            bird.phoenixSpiralHitCooldown[other.playerIndex] = bird.phoenixSpiralUltimate ? 4 : 5;
            for (int i = 0; i < (bird.phoenixSpiralUltimate ? 10 : 7); i++) {
                double burstAngle = -Math.PI / 2.0 + (bird.game.nextParticleRandom() - 0.5) * 1.25;
                Color spark = bird.phoenixSpiralUltimate && bird.game.nextParticleRandom() < 0.4
                        ? Color.web("#FFD180")
                        : (bird.game.nextParticleRandom() < 0.55 ? Color.GOLD : Color.ORANGERED);
                bird.game.particles.add(new Particle(
                        other.bodyCenterX() + (bird.game.nextParticleRandom() - 0.5) * 18.0,
                        other.bodyCenterY() + (bird.game.nextParticleRandom() - 0.5) * 18.0,
                        Math.cos(burstAngle) * (2.0 + bird.game.nextParticleRandom() * 3.0),
                        Math.sin(burstAngle) * (5.0 + bird.game.nextParticleRandom() * 5.0),
                        spark.deriveColor(0, 1, 1, 0.88)
                ));
            }
        }

        if (bird.phoenixSpiralTimer % 2 == 0) {
            for (int i = 0; i < 4; i++) {
                double flameAngle = angle + Math.PI / 2 + i * Math.PI / 2.0;
                double flameRadius = radius * (0.72 + i * 0.10);
                Color flame = bird.phoenixSpiralUltimate && i % 2 == 0 ? Color.web("#FFD180") : Color.ORANGERED;
                bird.game.particles.add(new Particle(
                        bird.bodyCenterX() + Math.cos(flameAngle) * flameRadius,
                        bird.bodyCenterY() + Math.sin(flameAngle) * flameRadius,
                        Math.cos(flameAngle) * (bird.phoenixSpiralUltimate ? 4.4 : 3.4),
                        Math.sin(flameAngle) * (bird.phoenixSpiralUltimate ? 4.4 : 3.4) - 4.2,
                        flame
                ));
            }
        } else {
            double plumeX = bird.bodyCenterX() + (bird.game.nextParticleRandom() - 0.5) * 28.0 * bird.sizeMultiplier;
            bird.game.particles.add(new Particle(
                    plumeX,
                    bird.bodyBottomY() - 4.0 * bird.sizeMultiplier,
                    (bird.game.nextParticleRandom() - 0.5) * 2.2,
                    -4.5 - bird.game.nextParticleRandom() * 4.5,
                    bird.phoenixSpiralUltimate ? Color.web("#FFD180") : Color.ORANGERED
            ));
        }

        if (bird.isOnGround() && bird.vy >= 0.0) {
            bird.phoenixSpiralTimer = 0;
            bird.phoenixSpiralUltimate = false;
            bird.phoenixSpiralUsed = false;
        }
    }

    private static void handleLava(Bird bird) {
        if (bird.phoenixLavaAirborne) {
            boolean canHoldAirStream = bird.specialHeld()
                    && !bird.isOnGround()
                    && bird.phoenixLavaHoldFrames < Bird.PHOENIX_AIR_LAVA_HOLD_MAX_FRAMES;
            if (canHoldAirStream) {
                bird.phoenixLavaHoldFrames++;
                int sustainFrames = bird.phoenixLavaUltimate
                        ? Bird.PHOENIX_LAVA_ULTIMATE_FRAMES
                        : Bird.PHOENIX_LAVA_FRAMES;
                bird.phoenixLavaTimer = Math.max(bird.phoenixLavaTimer, sustainFrames);
                bird.phoenixLavaReuseTimer = Math.max(bird.phoenixLavaReuseTimer, 12);
            } else if (bird.isOnGround()) {
                bird.phoenixLavaTimer = Math.min(bird.phoenixLavaTimer, 4);
            }
            bird.phoenixLavaX = bird.bodyCenterX();
            bird.phoenixLavaY = bird.bodyBottomY() - 6.0 * bird.sizeMultiplier;
            bird.vx *= 0.84;
            bird.vy = Math.min(bird.vy, bird.phoenixLavaUltimate ? 2.1 : 2.7);
            double width = (bird.phoenixLavaUltimate ? 38.0 : 32.0) * bird.sizeMultiplier;
            double length = Bird.PHOENIX_AIR_FLAME_LENGTH * (bird.phoenixLavaUltimate ? 1.18 : 1.0) * bird.sizeMultiplier;

            for (Bird other : bird.game.players) {
                if (!bird.canDamageTarget(other)) continue;
                if (other.playerIndex == bird.playerIndex) continue;
                if (other.playerIndex < 0 || other.playerIndex >= bird.phoenixLavaHitCooldown.length) continue;
                if (bird.phoenixLavaHitCooldown[other.playerIndex]) continue;

                double dx = Math.abs(other.bodyCenterX() - bird.phoenixLavaX);
                double dy = other.bodyCenterY() - bird.phoenixLavaY;
                if (dx > width + other.combatHalfWidth()) continue;
                if (dy < -other.combatHalfHeight() || dy > length + other.combatHalfHeight()) continue;

                int dmg = bird.phoenixLavaUltimate ? 3 : 2;
                double oldHealth = other.health;
                int dealt = (int) bird.applyDamageTo(other, dmg);
                if (dealt <= 0) continue;

                bird.game.damageDealt[bird.playerIndex] += dealt;
                bird.game.recordSpecialImpact(bird.playerIndex, dealt, true);
                if (other.health <= 0 && oldHealth > 0) bird.game.eliminations[bird.playerIndex]++;

                double pushDir = other.bodyCenterX() >= bird.phoenixLavaX ? 1.0 : -1.0;
                other.vx += pushDir * (bird.phoenixLavaUltimate ? 1.8 : 1.2);
                other.vy = Math.max(other.vy, bird.phoenixLavaUltimate ? 10.5 : 8.2);
                bird.phoenixLavaHitCooldown[other.playerIndex] = true;
            }

            if (bird.phoenixLavaTimer % 6 == 0) {
                Arrays.fill(bird.phoenixLavaHitCooldown, false);
            }

            if (bird.phoenixLavaTimer % 2 == 0) {
                for (int i = 0; i < 7; i++) {
                    double flow = i / 6.0;
                    double sparkY = bird.phoenixLavaY + length * flow + (bird.game.nextParticleRandom() - 0.5) * 18.0;
                    double taper = 1.0 - flow * 0.48;
                    Color spark = i % 3 == 0
                            ? (bird.phoenixLavaUltimate ? Color.web("#FFD180") : Color.GOLD)
                            : (bird.phoenixLavaUltimate ? Color.web("#FF7043") : Color.web("#FF9800"));
                    bird.game.particles.add(new Particle(
                            bird.phoenixLavaX + (bird.game.nextParticleRandom() - 0.5) * width * taper,
                            sparkY,
                            (bird.game.nextParticleRandom() - 0.5) * (1.4 + flow * 1.8),
                            -3.0 - bird.game.nextParticleRandom() * 2.6 - flow * 1.4,
                            spark.deriveColor(0, 1, 1, 0.84)
                    ));
                }
            }
            return;
        }

        double phase = bird.phoenixSpecialPhase(bird.phoenixLavaTimer,
                bird.phoenixLavaUltimate ? Bird.PHOENIX_LAVA_ULTIMATE_FRAMES : Bird.PHOENIX_LAVA_FRAMES);
        double eruptionRadius = Bird.PHOENIX_GROUND_ERUPTION_RADIUS * (bird.phoenixLavaUltimate ? 1.18 : 1.0) * bird.sizeMultiplier;
        double eruptionHeight = Bird.PHOENIX_GROUND_ERUPTION_HEIGHT * (bird.phoenixLavaUltimate ? 1.20 : 1.0) * bird.sizeMultiplier;
        double activeHeight = eruptionHeight * (0.70 + 0.30 * Math.sin(phase * Math.PI));

        for (Bird other : bird.game.players) {
            if (!bird.canDamageTarget(other)) continue;
            if (other.playerIndex == bird.playerIndex) continue;
            if (other.playerIndex < 0 || other.playerIndex >= bird.phoenixLavaHitCooldown.length) continue;
            if (bird.phoenixLavaHitCooldown[other.playerIndex]) continue;

            double dxFromCenter = other.bodyCenterX() - bird.phoenixLavaX;
            double absDx = Math.abs(dxFromCenter);
            if (absDx > eruptionRadius + other.combatHalfWidth()) continue;
            if (other.bodyBottomY() < bird.phoenixLavaY - activeHeight) continue;
            if (other.bodyCenterY() > bird.phoenixLavaY + 26.0 * bird.sizeMultiplier + other.combatHalfHeight()) continue;

            int dmg = bird.phoenixLavaUltimate ? 9 : 7;
            double oldHealth = other.health;
            int dealt = (int) bird.applyDamageTo(other, dmg);
            if (dealt <= 0) continue;

            bird.game.damageDealt[bird.playerIndex] += dealt;
            bird.game.recordSpecialImpact(bird.playerIndex, dealt, true);
            if (other.health <= 0 && oldHealth > 0) bird.game.eliminations[bird.playerIndex]++;

            double launchDir = dxFromCenter >= 0.0 ? 1.0 : -1.0;
            other.vx += launchDir * (bird.phoenixLavaUltimate ? 3.4 : 2.6);
            other.vy -= bird.phoenixLavaUltimate ? 12.4 : 10.4;
            bird.phoenixLavaHitCooldown[other.playerIndex] = true;

            for (int i = 0; i < (bird.phoenixLavaUltimate ? 14 : 10); i++) {
                double sparkX = bird.phoenixLavaX + (bird.game.nextParticleRandom() - 0.5) * eruptionRadius * 1.25;
                Color spark = i % 2 == 0
                        ? (bird.phoenixLavaUltimate ? Color.web("#FFD180") : Color.GOLD)
                        : Color.web("#FF7043");
                bird.game.particles.add(new Particle(
                        sparkX,
                        bird.phoenixLavaY - bird.game.nextParticleRandom() * 26.0 * bird.sizeMultiplier,
                        (bird.game.nextParticleRandom() - 0.5) * (bird.phoenixLavaUltimate ? 3.0 : 2.2),
                        -5.2 - bird.game.nextParticleRandom() * 6.4,
                        spark.deriveColor(0, 1, 1, 0.86)
                ));
            }
        }

        int plumeCount = bird.phoenixLavaUltimate ? 10 : 8;
        for (int i = 0; i < plumeCount; i++) {
            double offset = (bird.game.nextParticleRandom() - 0.5) * eruptionRadius * 1.35;
            double lift = activeHeight * (0.36 + bird.game.nextParticleRandom() * 0.44);
            Color flame = i % 2 == 0
                    ? (bird.phoenixLavaUltimate ? Color.web("#FFD180") : Color.GOLD)
                    : (bird.phoenixLavaUltimate ? Color.web("#FF7043") : Color.web("#FF9800"));
            bird.game.particles.add(new Particle(
                    bird.phoenixLavaX + offset,
                    bird.phoenixLavaY - bird.game.nextParticleRandom() * 12.0 * bird.sizeMultiplier,
                    offset * 0.018 + (bird.game.nextParticleRandom() - 0.5) * 0.9,
                    -3.6 - bird.game.nextParticleRandom() * 4.8 - lift * 0.022,
                    flame.deriveColor(0, 1, 1, 0.80)
            ));
        }
    }

    private static void spawnImpactBurst(Bird bird, double burstX, double burstY, int particleCount, Color core, Color outer) {
        for (int i = 0; i < particleCount; i++) {
            double angle = bird.game.nextParticleRandom() * Math.PI * 2;
            double speed = 2.4 + bird.game.nextParticleRandom() * 5.8;
            Color c = bird.game.nextParticleRandom() < 0.4 ? core : outer;
            bird.game.particles.add(new Particle(
                    burstX,
                    burstY,
                    Math.cos(angle) * speed,
                    Math.sin(angle) * speed - 1.8,
                    c.deriveColor(0, 1, 1, 0.88)
            ));
        }
    }
}
