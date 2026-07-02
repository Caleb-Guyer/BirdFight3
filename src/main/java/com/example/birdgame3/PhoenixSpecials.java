package com.example.birdgame3;

import javafx.scene.paint.Color;

import java.util.Arrays;

final class PhoenixSpecials {
    private PhoenixSpecials() {
    }

    static void use(Bird bird, boolean ultimate) {
        switch (bird.selectPhoenixSpecialVariant()) {
            case NEUTRAL -> neutral(bird, ultimate);
            case SIDE -> side(bird, ultimate);
            case UP -> up(bird, ultimate);
            case DOWN -> down(bird, ultimate);
        }
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
        int startupFrames = ultimate ? Bird.PHOENIX_FIREBALL_ULTIMATE_CAST_LOCK_FRAMES : Bird.PHOENIX_FIREBALL_CAST_LOCK_FRAMES;
        int flightFrames = ultimate ? Bird.PHOENIX_FIREBALL_ULTIMATE_FRAMES : Bird.PHOENIX_FIREBALL_FRAMES;
        bird.phoenixFireballTimer = startupFrames + flightFrames;
        bird.phoenixCastLockTimer = startupFrames;
        bird.phoenixFireballX = bird.bodyCenterX() + dir * 24.0 * bird.sizeMultiplier;
        bird.phoenixFireballY = bird.bodyCenterY() - 18.0 * bird.sizeMultiplier;
        bird.phoenixFireballVX = dir * (ultimate ? 14.4 : 12.2);
        bird.phoenixFireballVY = 0.0;
        bird.phoenixFireballUltimate = ultimate;
        bird.phoenixFireballReuseTimer = Math.max(bird.phoenixFireballReuseTimer,
                ultimate ? Bird.PHOENIX_FIREBALL_ULTIMATE_REUSE_FRAMES : Bird.PHOENIX_FIREBALL_REUSE_FRAMES);
        bird.specialCooldown = 0;
        bird.specialMaxCooldown = 0;
        bird.attackAnimationTimer = Math.max(bird.attackAnimationTimer, bird.phoenixCastLockTimer);
        bird.vx = 0.0;
        if (!bird.isOnGround()) {
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
        if (bird.phoenixFireballTimer > 0) {
            handleFireball(bird);
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
                || bird.phoenixFireballTimer > 0
                || bird.phoenixSpiralTimer > 0
                || bird.phoenixLavaTimer > 0;
    }

    static boolean ready(Bird bird, Bird.PhoenixSpecialVariant variant) {
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
        bird.phoenixFireballVX = 0.0;
        bird.phoenixFireballVY = 0.0;
        bird.phoenixSpiralTimer = 0;
        bird.phoenixSpiralUltimate = false;
        Arrays.fill(bird.phoenixSpiralHitCooldown, 0);
        bird.phoenixLavaTimer = 0;
        bird.phoenixLavaUltimate = false;
        bird.phoenixLavaAirborne = false;
        Arrays.fill(bird.phoenixLavaHitCooldown, false);
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
            double angle = SimRng.next() * Math.PI * 2;
            double orbit = (16 + SimRng.next() * 24 + chargeRatio * 34) * bird.sizeMultiplier;
            double lift = 0.5 + chargeRatio * 1.4;
            Color ember = bird.phoenixChargeUltimate && SimRng.next() < 0.45
                    ? Color.web("#FFF3B0")
                    : (SimRng.next() < 0.6 ? Color.ORANGERED : Color.GOLD);
            bird.game.particles.add(new Particle(
                    bird.bodyCenterX() + Math.cos(angle) * orbit,
                    bird.bodyCenterY() + Math.sin(angle) * orbit * 0.72,
                    -Math.sin(angle) * (1.2 + chargeRatio * 1.8),
                    -lift - SimRng.next() * 1.8,
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
                double angle = SimRng.next() * Math.PI * 2;
                Color spark = ultimate && SimRng.next() < 0.35 ? Color.web("#FFF3B0") : (SimRng.next() < 0.5 ? Color.ORANGERED : Color.GOLD);
                bird.game.particles.add(new Particle(
                        other.bodyCenterX(),
                        other.bodyCenterY(),
                        Math.cos(angle) * (3 + SimRng.next() * 5) + dx / safeDist * 1.8,
                        Math.sin(angle) * (3 + SimRng.next() * 5) - 2.6,
                        spark.deriveColor(0, 1, 1, 0.9)
                ));
            }
        }

        int particleCount = 8 + chargeLevel * 4;
        for (int i = 0; i < particleCount; i++) {
            double angle = SimRng.next() * Math.PI * 2;
            double speed = 3.0 + SimRng.next() * 4.5;
            Color c = ultimate && SimRng.next() < 0.35
                    ? Color.web("#FFF3B0")
                    : (SimRng.next() < 0.5 ? Color.ORANGERED : Color.GOLD);
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

    private static void handleFireball(Bird bird) {
        if (bird.phoenixCastLockTimer > 0) {
            bird.vx = 0.0;
            if (!bird.isOnGround()) {
                bird.vy = Math.min(bird.vy, 1.1);
            }
            int dir = bird.facingDirection();
            double startupFrames = bird.phoenixFireballUltimate
                    ? Bird.PHOENIX_FIREBALL_ULTIMATE_CAST_LOCK_FRAMES
                    : Bird.PHOENIX_FIREBALL_CAST_LOCK_FRAMES;
            double windup = 1.0 - Math.clamp(bird.phoenixCastLockTimer / startupFrames, 0.0, 1.0);
            bird.phoenixFireballX = bird.bodyCenterX() + dir * (24.0 + windup * 14.0) * bird.sizeMultiplier;
            bird.phoenixFireballY = bird.bodyCenterY() - (18.0 + windup * 18.0) * bird.sizeMultiplier;
            if ((bird.phoenixCastLockTimer & 1) == 0) {
                Color c = bird.phoenixFireballUltimate ? Color.web("#FFD180") : Color.GOLD;
                for (int i = 0; i < 3; i++) {
                    bird.game.particles.add(new Particle(
                            bird.phoenixFireballX - dir * (4.0 + SimRng.next() * 10.0) * bird.sizeMultiplier,
                            bird.phoenixFireballY + (SimRng.next() - 0.5) * 14.0 * bird.sizeMultiplier,
                            -dir * (0.6 + SimRng.next() * 1.4),
                            -2.0 - SimRng.next() * 2.2,
                            c.deriveColor(0, 1, 1, 0.82)
                    ));
                }
            }
            return;
        }
        bird.phoenixFireballX += bird.phoenixFireballVX;
        bird.phoenixFireballY += bird.phoenixFireballVY;

        double radius = (bird.phoenixFireballUltimate ? 34.0 : 28.0) * bird.sizeMultiplier;

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
            other.vy -= bird.phoenixFireballUltimate ? 6.3 : 4.8;
            spawnImpactBurst(bird, bird.phoenixFireballX, bird.phoenixFireballY,
                    bird.phoenixFireballUltimate ? 26 : 18,
                    bird.phoenixFireballUltimate ? Color.web("#FFD180") : Color.GOLD,
                    bird.phoenixFireballUltimate ? Color.web("#FF7043") : Color.ORANGERED);
            bird.game.shakeIntensity = Math.max(bird.game.shakeIntensity, bird.phoenixFireballUltimate ? 8 : 6);
            bird.game.hitstopFrames = Math.max(bird.game.hitstopFrames, bird.phoenixFireballUltimate ? 4 : 3);

            bird.phoenixFireballTimer = 0;
            break;
        }

        if (bird.phoenixFireballX < -100 || bird.phoenixFireballX > BirdGame3.WORLD_WIDTH + 100) {
            spawnImpactBurst(bird, bird.phoenixFireballX, bird.phoenixFireballY,
                    bird.phoenixFireballUltimate ? 16 : 12,
                    bird.phoenixFireballUltimate ? Color.web("#FFD180") : Color.GOLD,
                    bird.phoenixFireballUltimate ? Color.web("#FF7043") : Color.ORANGERED);
            bird.phoenixFireballTimer = 0;
        }

        if (bird.phoenixFireballTimer % 2 == 0) {
            for (int i = 0; i < 4; i++) {
                bird.game.particles.add(new Particle(
                        bird.phoenixFireballX - Math.signum(bird.phoenixFireballVX == 0.0 ? bird.facingDirection() : bird.phoenixFireballVX) * (10 + SimRng.next() * 16),
                        bird.phoenixFireballY + (SimRng.next() - 0.5) * 24,
                        (SimRng.next() - 0.5) * 1.8 - bird.phoenixFireballVX * 0.18,
                        (SimRng.next() - 0.5) * 1.8 - 1.2,
                        bird.phoenixFireballUltimate ? Color.web("#FF7043") : Color.ORANGE
                ));
            }
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
                double burstAngle = -Math.PI / 2.0 + (SimRng.next() - 0.5) * 1.25;
                Color spark = bird.phoenixSpiralUltimate && SimRng.next() < 0.4
                        ? Color.web("#FFD180")
                        : (SimRng.next() < 0.55 ? Color.GOLD : Color.ORANGERED);
                bird.game.particles.add(new Particle(
                        other.bodyCenterX() + (SimRng.next() - 0.5) * 18.0,
                        other.bodyCenterY() + (SimRng.next() - 0.5) * 18.0,
                        Math.cos(burstAngle) * (2.0 + SimRng.next() * 3.0),
                        Math.sin(burstAngle) * (5.0 + SimRng.next() * 5.0),
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
            double plumeX = bird.bodyCenterX() + (SimRng.next() - 0.5) * 28.0 * bird.sizeMultiplier;
            bird.game.particles.add(new Particle(
                    plumeX,
                    bird.bodyBottomY() - 4.0 * bird.sizeMultiplier,
                    (SimRng.next() - 0.5) * 2.2,
                    -4.5 - SimRng.next() * 4.5,
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
                    double sparkY = bird.phoenixLavaY + length * flow + (SimRng.next() - 0.5) * 18.0;
                    double taper = 1.0 - flow * 0.48;
                    Color spark = i % 3 == 0
                            ? (bird.phoenixLavaUltimate ? Color.web("#FFD180") : Color.GOLD)
                            : (bird.phoenixLavaUltimate ? Color.web("#FF7043") : Color.web("#FF9800"));
                    bird.game.particles.add(new Particle(
                            bird.phoenixLavaX + (SimRng.next() - 0.5) * width * taper,
                            sparkY,
                            (SimRng.next() - 0.5) * (1.4 + flow * 1.8),
                            -3.0 - SimRng.next() * 2.6 - flow * 1.4,
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
                double sparkX = bird.phoenixLavaX + (SimRng.next() - 0.5) * eruptionRadius * 1.25;
                Color spark = i % 2 == 0
                        ? (bird.phoenixLavaUltimate ? Color.web("#FFD180") : Color.GOLD)
                        : Color.web("#FF7043");
                bird.game.particles.add(new Particle(
                        sparkX,
                        bird.phoenixLavaY - SimRng.next() * 26.0 * bird.sizeMultiplier,
                        (SimRng.next() - 0.5) * (bird.phoenixLavaUltimate ? 3.0 : 2.2),
                        -5.2 - SimRng.next() * 6.4,
                        spark.deriveColor(0, 1, 1, 0.86)
                ));
            }
        }

        int plumeCount = bird.phoenixLavaUltimate ? 10 : 8;
        for (int i = 0; i < plumeCount; i++) {
            double offset = (SimRng.next() - 0.5) * eruptionRadius * 1.35;
            double lift = activeHeight * (0.36 + SimRng.next() * 0.44);
            Color flame = i % 2 == 0
                    ? (bird.phoenixLavaUltimate ? Color.web("#FFD180") : Color.GOLD)
                    : (bird.phoenixLavaUltimate ? Color.web("#FF7043") : Color.web("#FF9800"));
            bird.game.particles.add(new Particle(
                    bird.phoenixLavaX + offset,
                    bird.phoenixLavaY - SimRng.next() * 12.0 * bird.sizeMultiplier,
                    offset * 0.018 + (SimRng.next() - 0.5) * 0.9,
                    -3.6 - SimRng.next() * 4.8 - lift * 0.022,
                    flame.deriveColor(0, 1, 1, 0.80)
            ));
        }
    }

    private static void spawnImpactBurst(Bird bird, double burstX, double burstY, int particleCount, Color core, Color outer) {
        for (int i = 0; i < particleCount; i++) {
            double angle = SimRng.next() * Math.PI * 2;
            double speed = 2.4 + SimRng.next() * 5.8;
            Color c = SimRng.next() < 0.4 ? core : outer;
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
