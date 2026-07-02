package com.example.birdgame3;

import javafx.scene.paint.Color;

import java.util.Arrays;

final class TurkeySpecials {
    private TurkeySpecials() {
    }

    static void use(Bird bird, boolean ultimate) {
        switch (bird.selectTurkeySpecialVariant()) {
            case NEUTRAL -> neutral(bird, ultimate);
            case SIDE -> side(bird, ultimate);
            case UP -> up(bird, ultimate);
            case DOWN -> down(bird, ultimate);
        }
    }

    static void neutral(Bird bird, boolean ultimate) {
        bird.turkeyGobbleCharging = true;
        bird.turkeyGobbleTimer = 0;
        bird.turkeyGobbleHoldTimer = 1;
        bird.turkeyGobbleReuseTimer = Math.max(bird.turkeyGobbleReuseTimer,
                ultimate ? 22 : Bird.TURKEY_GOBBLE_GUARD_REUSE_FRAMES);
        bird.turkeyGobbleArmorTimer = ultimate ? Bird.TURKEY_GOBBLE_ARMOR_FRAMES + 5 : Bird.TURKEY_GOBBLE_ARMOR_FRAMES;
        bird.turkeyGobbleUltimate = ultimate;
        bird.turkeyGobbleCountered = false;
        Arrays.fill(bird.turkeyGobbleHit, false);
        bird.specialCooldown = 0;
        bird.specialMaxCooldown = 0;
        bird.attackAnimationTimer = Math.max(bird.attackAnimationTimer, 8);
        bird.vx *= bird.isOnGround() ? 0.62 : 0.78;
        if (ultimate) {
            bird.game.addToKillFeed(bird.shortName() + " ULT GOBBLE GUARD!");
        }
        for (int i = 0; i < bird.scaledParticleCount(ultimate ? 16 : 10); i++) {
            double angle = SimRng.next() * Math.PI * 2.0;
            bird.game.particles.add(new Particle(
                    bird.bodyCenterX(),
                    bird.bodyCenterY(),
                    Math.cos(angle) * (0.8 + SimRng.next() * 2.2),
                    Math.sin(angle) * (0.8 + SimRng.next() * 2.2) - 0.8,
                    (ultimate ? Color.GOLD : Color.web("#D7CCC8")).deriveColor(0, 1, 1, 0.72)
            ));
        }
    }

    static void side(Bird bird, boolean ultimate) {
        int dir = bird.horizontalInputDirection();
        if (dir == 0) {
            dir = bird.facingDirection();
        }
        bird.facingRight = dir > 0;
        bird.turkeyStampedeDirection = dir;
        bird.turkeyStampedeTimer = 2;
        bird.turkeyStampedeHoldFrames = 0;
        bird.turkeyStampedeReuseTimer = Math.max(bird.turkeyStampedeReuseTimer,
                ultimate ? 20 : Bird.TURKEY_STAMPEDE_REUSE_FRAMES);
        bird.turkeyStampedeUltimate = ultimate;
        Arrays.fill(bird.turkeyStampedeHitCooldown, 0);
        bird.specialCooldown = 0;
        bird.specialMaxCooldown = 0;
        bird.attackAnimationTimer = Math.max(bird.attackAnimationTimer, 10);
        bird.vx = dir * (ultimate ? 7.2 : 5.7);
        bird.vy *= bird.isOnGround() ? 0.70 : 0.82;
        bird.isBlocking = false;
        bird.parryWindowFrames = 0;
        bird.shieldStunFrames = 0;
        bird.recordTurkeyHeavyMoveProgress();
        if (ultimate) {
            bird.game.addToKillFeed(bird.shortName() + " ULT DRUMSTICK STAMPEDE!");
        }
    }

    static void up(Bird bird, boolean ultimate) {
        if (bird.turkeyPanicFlapUsed && !ultimate) {
            return;
        }
        bird.turkeyPanicFlapUsed = true;
        bird.turkeyPanicFlapUltimate = ultimate;
        bird.turkeyPanicFlapTimer = ultimate ? Bird.TURKEY_PANIC_FLAP_FRAMES + 7 : Bird.TURKEY_PANIC_FLAP_FRAMES;
        bird.turkeyPanicFlapReuseTimer = 0;
        Arrays.fill(bird.turkeyPanicFlapHit, false);
        bird.specialCooldown = 0;
        bird.specialMaxCooldown = 0;
        bird.attackAnimationTimer = Math.max(bird.attackAnimationTimer, bird.turkeyPanicFlapTimer);
        bird.canDoubleJump = false;
        bird.vy = Math.min(bird.vy, ultimate ? -20.5 : -17.0);
        bird.vx *= 0.22;
        if (ultimate) {
            bird.game.addToKillFeed(bird.shortName() + " ULT PANIC FLAP!");
        }
        for (int i = 0; i < bird.scaledParticleCount(ultimate ? 34 : 22); i++) {
            int side = i % 2 == 0 ? -1 : 1;
            double spread = side * (20.0 + SimRng.next() * 48.0);
            bird.game.particles.add(new Particle(
                    bird.bodyCenterX() + spread,
                    bird.bodyCenterY() + (SimRng.next() - 0.5) * 26.0 * bird.sizeMultiplier,
                    side * (2.0 + SimRng.next() * 2.6),
                    4.2 + SimRng.next() * 4.4,
                    (ultimate ? Color.GOLD : Color.web("#F5F5F5")).deriveColor(0, 1, 1, 0.70)
            ));
        }
    }

    static void down(Bird bird, boolean ultimate) {
        int dir = bird.facingDirection();
        double trapX = bird.bodyCenterX() - dir * 44.0 * bird.sizeMultiplier;
        double trapY = trapSurfaceY(bird, trapX);
        bird.turkeyFeastTraps.add(new Bird.TurkeyFeastTrap(trapX, trapY, ultimate));
        while (bird.turkeyFeastTraps.size() > (ultimate ? 5 : 3)) {
            bird.turkeyFeastTraps.removeFirst();
        }
        bird.turkeyFeastTrapReuseTimer = Math.max(bird.turkeyFeastTrapReuseTimer,
                ultimate ? 32 : Bird.TURKEY_FEAST_TRAP_REUSE_FRAMES);
        bird.specialCooldown = 0;
        bird.specialMaxCooldown = 0;
        bird.attackAnimationTimer = Math.max(bird.attackAnimationTimer, 12);
        bird.vx += dir * 2.4;
        bird.isBlocking = false;
        bird.parryWindowFrames = 0;
        bird.shieldStunFrames = 0;
        bird.blockCooldown = 0;
        if (ultimate) {
            bird.game.addToKillFeed(bird.shortName() + " SET A ROYAL FEAST TRAP!");
        }
        for (int i = 0; i < bird.scaledParticleCount(ultimate ? 34 : 22); i++) {
            bird.game.particles.add(new Particle(
                    trapX + (SimRng.next() - 0.5) * 28.0,
                    trapY - 24.0,
                    (SimRng.next() - 0.5) * 5.0,
                    -2.0 - SimRng.next() * 5.0,
                    (ultimate ? Color.GOLD : Color.web("#FFCC80")).deriveColor(0, 1, 1, 0.78)
            ));
        }
    }

    static void handleState(Bird bird) {
        if (bird.type != BirdGame3.BirdType.TURKEY && !bird.mockingbirdCopiedNeutralFrom(BirdGame3.BirdType.TURKEY)) {
            return;
        }
        if (bird.stunTime > 0.0) {
            reset(bird, false);
            if (bird.mockingbirdCopiedNeutralFrom(BirdGame3.BirdType.TURKEY)) {
                bird.mockingbirdCopiedNeutralSource = null;
            }
            return;
        }
        if (bird.turkeyGobbleCharging) {
            handleGobbleCharge(bird);
        }
        if (bird.turkeyGobbleTimer > 0) {
            handleGobbleGuard(bird);
        }
        if (bird.turkeyStampedeTimer > 0) {
            handleStampede(bird);
        }
        if (bird.turkeyPanicFlapTimer > 0) {
            handlePanicFlap(bird);
        }
    }

    static boolean active(Bird bird) {
        return bird.turkeyGobbleCharging
                || bird.turkeyGobbleTimer > 0
                || bird.turkeyStampedeTimer > 0
                || bird.turkeyPanicFlapTimer > 0;
    }

    static boolean ready(Bird bird, Bird.TurkeySpecialVariant variant) {
        boolean ultimateReady = bird.isUltimateReady();
        return switch (variant) {
            case NEUTRAL -> ultimateReady || bird.turkeyGobbleReuseTimer <= 0;
            case SIDE -> ultimateReady || bird.turkeyStampedeReuseTimer <= 0;
            case UP -> ultimateReady || !bird.turkeyPanicFlapUsed;
            case DOWN -> ultimateReady || bird.turkeyFeastTrapReuseTimer <= 0;
        };
    }

    static boolean canConvertShieldIntoDown(Bird bird) {
        return bird.selectTurkeySpecialVariant() == Bird.TurkeySpecialVariant.DOWN
                && bird.isBlocking
                && bird.shieldStunFrames <= 0;
    }

    static void interruptOnHit(Bird bird) {
        if (bird.type != BirdGame3.BirdType.TURKEY || armorActive(bird)) {
            return;
        }
        if (active(bird)) {
            bird.attackAnimationTimer = 0;
        }
        reset(bird, false);
    }

    static void reset(Bird bird, boolean clearTraps) {
        bird.turkeyGobbleTimer = 0;
        bird.turkeyGobbleCharging = false;
        bird.turkeyGobbleHoldTimer = 0;
        bird.turkeyGobbleArmorTimer = 0;
        bird.turkeyGobbleUltimate = false;
        bird.turkeyGobbleCountered = false;
        Arrays.fill(bird.turkeyGobbleHit, false);
        bird.turkeyStampedeTimer = 0;
        bird.turkeyStampedeHoldFrames = 0;
        bird.turkeyStampedeUltimate = false;
        bird.turkeyStampedeDirection = bird.facingDirection();
        Arrays.fill(bird.turkeyStampedeHitCooldown, 0);
        bird.turkeyPanicFlapTimer = 0;
        bird.turkeyPanicFlapUltimate = false;
        Arrays.fill(bird.turkeyPanicFlapHit, false);
        if (clearTraps) {
            bird.turkeyFeastTraps.clear();
        }
    }

    static boolean armorActive(Bird bird) {
        return bird.type == BirdGame3.BirdType.TURKEY
                && bird.health > 0
                && (bird.turkeyGobbleArmorTimer > 0 || bird.turkeyStampedeTimer > 0);
    }

    static double applyArmor(Bird bird, double scaledDamage) {
        boolean guarding = bird.turkeyGobbleArmorTimer > 0;
        if (guarding) {
            bird.turkeyGobbleCountered = true;
            if (bird.turkeyGobbleCharging) {
                bird.turkeyGobbleCharging = false;
                bird.turkeyGobbleHoldTimer = Math.max(bird.turkeyGobbleHoldTimer, Bird.TURKEY_GOBBLE_CHARGE_MAX_FRAMES / 2);
                bird.turkeyGobbleTimer = Math.max(bird.turkeyGobbleTimer, Bird.TURKEY_GOBBLE_GUARD_FRAMES + 4);
                bird.turkeyGobbleReuseTimer = Math.max(bird.turkeyGobbleReuseTimer, Bird.TURKEY_GOBBLE_GUARD_REUSE_FRAMES);
            } else {
                bird.turkeyGobbleTimer = Math.max(bird.turkeyGobbleTimer, 14);
            }
            Arrays.fill(bird.turkeyGobbleHit, false);
        }
        bird.vx *= guarding ? 0.22 : 0.48;
        bird.vy *= guarding ? 0.45 : 0.65;
        bird.stunTime = 0.0;
        bird.knockdownTimer = 0;
        bird.game.shakeIntensity = Math.max(bird.game.shakeIntensity, guarding ? 8 : 5);
        for (int i = 0; i < bird.scaledParticleCount(guarding ? 16 : 9); i++) {
            double angle = SimRng.next() * Math.PI * 2.0;
            bird.game.particles.add(new Particle(
                    bird.bodyCenterX(),
                    bird.bodyCenterY(),
                    Math.cos(angle) * (2.0 + SimRng.next() * 4.0),
                    Math.sin(angle) * (2.0 + SimRng.next() * 4.0) - 1.4,
                    (guarding ? Color.GOLD : Color.SADDLEBROWN).deriveColor(0, 1, 1, 0.75)
            ));
        }
        return scaledDamage * (guarding ? 0.35 : 0.58);
    }

    static double gobbleChargeRatio(Bird bird) {
        return Math.clamp(bird.turkeyGobbleHoldTimer / (double) Bird.TURKEY_GOBBLE_CHARGE_MAX_FRAMES, 0.0, 1.0);
    }

    static double trapSurfaceY(Bird bird, double trapX) {
        double bestY = bird.hasSolidGroundFloorUnderBody() ? BirdGame3.GROUND_Y : Double.POSITIVE_INFINITY;
        double sourceY = bird.bodyBottomY() - 18.0 * bird.sizeMultiplier;
        for (Platform p : bird.game.platforms) {
            boolean isCaveCeiling = bird.game.selectedMap == BirdGame3.MapType.CAVE
                    && p.y <= 1 && p.h >= 60 && p.w >= BirdGame3.WORLD_WIDTH - 10;
            if (isCaveCeiling) continue;
            if (trapX < p.x - 20.0 || trapX > p.x + p.w + 20.0) continue;
            if (p.y < sourceY - 14.0) continue;
            if (p.y < bestY) {
                bestY = p.y;
            }
        }
        return Double.isFinite(bestY) ? bestY : bird.bodyBottomY() + 8.0 * bird.sizeMultiplier;
    }

    static void applyStuffedKnockbackBonus(Bird bird, Bird target, double direction) {
        if (bird.type != BirdGame3.BirdType.TURKEY || target == null || target.turkeyStuffedTimer <= 0) {
            return;
        }
        if (target.turkeyStuffedOwnerIndex != bird.playerIndex) {
            return;
        }
        double dir = Math.signum(direction);
        if (dir == 0.0) {
            dir = Math.signum(target.bodyCenterX() - bird.bodyCenterX());
            if (dir == 0.0) {
                dir = bird.facingDirection();
            }
        }
        boolean ultimate = target.turkeyStuffedUltimate;
        target.vx += dir * (ultimate ? 6.5 : 4.5);
        target.vy -= ultimate ? 4.0 : 2.8;
        target.turkeyStuffedTimer = 0;
        target.turkeyStuffedOwnerIndex = -1;
        target.turkeyStuffedUltimate = false;
        for (int i = 0; i < bird.scaledParticleCount(ultimate ? 18 : 12); i++) {
            double angle = SimRng.next() * Math.PI * 2.0;
            bird.game.particles.add(new Particle(
                    target.bodyCenterX(),
                    target.bodyCenterY(),
                    Math.cos(angle) * (2.0 + SimRng.next() * 5.0),
                    Math.sin(angle) * (2.0 + SimRng.next() * 5.0) - 2.0,
                    (ultimate ? Color.GOLD : Color.web("#FFB74D")).deriveColor(0, 1, 1, 0.82)
            ));
        }
    }

    private static void handleGobbleCharge(Bird bird) {
        boolean stillCharging = bird.specialHeld()
                && !bird.jumpPressed()
                && !bird.blockPressed()
                && bird.horizontalInputDirection() == 0
                && bird.turkeyGobbleHoldTimer < Bird.TURKEY_GOBBLE_CHARGE_MAX_FRAMES;
        if (!stillCharging) {
            releaseGobbleGuardCharge(bird);
            return;
        }

        bird.turkeyGobbleHoldTimer = Math.min(Bird.TURKEY_GOBBLE_CHARGE_MAX_FRAMES, bird.turkeyGobbleHoldTimer + 1);
        bird.turkeyGobbleArmorTimer = Math.max(bird.turkeyGobbleArmorTimer, bird.turkeyGobbleCountered ? 4 : 2);
        bird.attackAnimationTimer = Math.max(bird.attackAnimationTimer, 5);
        bird.vx *= bird.isOnGround() ? 0.64 : 0.82;
        if (!bird.isOnGround()) {
            bird.vy = Math.min(bird.vy, 1.8);
        }

        double ratio = gobbleChargeRatio(bird);
        if ((bird.turkeyGobbleHoldTimer & 3) == 0) {
            double centerX = bird.bodyCenterX();
            double centerY = bird.bodyCenterY() - 5.0 * bird.sizeMultiplier;
            double orbit = (28.0 + ratio * 42.0) * bird.sizeMultiplier;
            for (int i = 0; i < bird.scaledParticleCount(ratio > 0.75 ? 3 : 2); i++) {
                double angle = SimRng.next() * Math.PI * 2.0;
                bird.game.particles.add(new Particle(
                        centerX + Math.cos(angle) * orbit,
                        centerY + Math.sin(angle) * orbit * 0.62,
                        -Math.cos(angle) * (0.8 + ratio * 1.8),
                        -0.6 - SimRng.next() * (1.0 + ratio * 1.6),
                        (bird.turkeyGobbleCountered ? Color.GOLD : Color.web("#EFEBE9")).deriveColor(0, 1, 1, 0.62 + ratio * 0.20)
                ));
            }
        }
    }

    private static void releaseGobbleGuardCharge(Bird bird) {
        if (!bird.turkeyGobbleCharging) {
            return;
        }
        bird.turkeyGobbleCharging = false;
        bird.turkeyGobbleHoldTimer = Math.clamp(bird.turkeyGobbleHoldTimer, 1, Bird.TURKEY_GOBBLE_CHARGE_MAX_FRAMES);
        bird.turkeyGobbleTimer = bird.turkeyGobbleUltimate ? Bird.TURKEY_GOBBLE_GUARD_FRAMES + 6 : Bird.TURKEY_GOBBLE_GUARD_FRAMES;
        bird.turkeyGobbleReuseTimer = Math.max(bird.turkeyGobbleReuseTimer,
                bird.turkeyGobbleUltimate ? 22 : Bird.TURKEY_GOBBLE_GUARD_REUSE_FRAMES);
        bird.turkeyGobbleArmorTimer = Math.max(bird.turkeyGobbleArmorTimer, bird.turkeyGobbleCountered ? 8 : 4);
        Arrays.fill(bird.turkeyGobbleHit, false);
        bird.specialCooldown = 0;
        bird.specialMaxCooldown = 0;
        bird.attackAnimationTimer = Math.max(bird.attackAnimationTimer, bird.turkeyGobbleTimer + 3);
        bird.vx *= bird.isOnGround() ? 0.46 : 0.68;

        double ratio = gobbleChargeRatio(bird);
        int burstCount = bird.scaledParticleCount(14 + (int) Math.round(ratio * 22.0));
        for (int i = 0; i < burstCount; i++) {
            double angle = SimRng.next() * Math.PI * 2.0;
            double speed = 2.0 + SimRng.next() * (3.4 + ratio * 4.0);
            bird.game.particles.add(new Particle(
                    bird.bodyCenterX(),
                    bird.bodyCenterY() - 4.0 * bird.sizeMultiplier,
                    Math.cos(angle) * speed,
                    Math.sin(angle) * speed - 1.4,
                    (bird.turkeyGobbleCountered ? Color.GOLD : Color.web("#D7CCC8")).deriveColor(0, 1, 1, 0.78)
            ));
        }
    }

    private static void handleGobbleGuard(Bird bird) {
        double chargeRatio = gobbleChargeRatio(bird);
        double chargeScale = 1.0 + chargeRatio * 1.15;

        double s = bird.sizeMultiplier;
        double radius = (bird.turkeyGobbleCountered ? 230.0 : 132.0 + chargeRatio * 92.0)
                * (bird.turkeyGobbleUltimate ? 1.15 : 1.0) * s;
        double verticalRadius = (bird.turkeyGobbleCountered ? 150.0 : 92.0 + chargeRatio * 60.0)
                * (bird.turkeyGobbleUltimate ? 1.12 : 1.0) * s;
        double centerX = bird.bodyCenterX();
        double centerY = bird.bodyCenterY() - 8.0 * s;

        if ((bird.turkeyGobbleTimer & 2) == 0) {
            int particles = bird.scaledParticleCount(bird.turkeyGobbleCountered ? 8 : 5);
            for (int i = 0; i < particles; i++) {
                double angle = SimRng.next() * Math.PI * 2.0;
                bird.game.particles.add(new Particle(
                        centerX + Math.cos(angle) * radius * 0.24,
                        centerY + Math.sin(angle) * verticalRadius * 0.18,
                        Math.cos(angle) * (2.0 + SimRng.next() * 3.0),
                        Math.sin(angle) * (1.4 + SimRng.next() * 2.4) - 0.6,
                        (bird.turkeyGobbleCountered ? Color.GOLD : Color.web("#EFEBE9")).deriveColor(0, 1, 1, 0.55)
                ));
            }
        }

        for (Bird other : bird.game.players) {
            if (!bird.canDamageTarget(other)) continue;
            if (other.playerIndex < 0 || other.playerIndex >= bird.turkeyGobbleHit.length) continue;
            if (bird.turkeyGobbleHit[other.playerIndex]) continue;

            double dx = other.bodyCenterX() - centerX;
            double dy = other.bodyCenterY() - centerY;
            double normalized = Math.hypot(dx / Math.max(1.0, radius), dy / Math.max(1.0, verticalRadius));
            if (normalized > 1.0 + other.combatRadius() / Math.max(radius, verticalRadius)) continue;

            bird.turkeyGobbleHit[other.playerIndex] = true;
            int dmg = bird.turkeyGobbleCountered
                    ? (bird.turkeyGobbleUltimate ? 24 : 18)
                    : (bird.turkeyGobbleUltimate ? 11 : 8) + (int) Math.round(chargeRatio * (bird.turkeyGobbleUltimate ? 13.0 : 10.0));
            double oldHealth = other.health;
            int dealt = (int) bird.applyDamageTo(other, dmg);
            if (dealt <= 0) continue;

            bird.game.damageDealt[bird.playerIndex] += dealt;
            bird.game.recordSpecialImpact(bird.playerIndex, dealt, true);
            if (other.health <= 0 && oldHealth > 0) {
                bird.game.eliminations[bird.playerIndex]++;
            }

            double dir = Math.signum(dx == 0.0 ? bird.facingDirection() : dx);
            other.vx += dir * (bird.turkeyGobbleCountered
                    ? (bird.turkeyGobbleUltimate ? 23.0 : 18.0)
                    : (bird.turkeyGobbleUltimate ? 13.5 : 10.8) * chargeScale);
            other.vy -= bird.turkeyGobbleCountered
                    ? (bird.turkeyGobbleUltimate ? 13.0 : 10.0)
                    : (bird.turkeyGobbleUltimate ? 7.4 : 6.2) * chargeScale;
            if (bird.turkeyGobbleCountered) {
                other.applyStun(bird.turkeyGobbleUltimate ? 16 : 10);
                bird.game.hitstopFrames = Math.max(bird.game.hitstopFrames, bird.turkeyGobbleUltimate ? 5 : 3);
                bird.game.shakeIntensity = Math.max(bird.game.shakeIntensity, bird.turkeyGobbleUltimate ? 9 : 5);
            }
            applyStuffedKnockbackBonus(bird, other, dir);
        }
    }

    private static void handleStampede(Bird bird) {
        if (!bird.specialHeld()) {
            bird.turkeyStampedeTimer = 0;
            bird.turkeyStampedeHoldFrames = 0;
            return;
        }
        for (int i = 0; i < bird.turkeyStampedeHitCooldown.length; i++) {
            if (bird.turkeyStampedeHitCooldown[i] > 0) {
                bird.turkeyStampedeHitCooldown[i]--;
            }
        }
        int inputDir = bird.horizontalInputDirection();
        if (inputDir != 0) {
            bird.turkeyStampedeDirection = inputDir;
        }
        int dir = bird.turkeyStampedeDirection == 0 ? bird.facingDirection() : bird.turkeyStampedeDirection;
        bird.facingRight = dir > 0;
        bird.turkeyStampedeTimer = Math.max(bird.turkeyStampedeTimer, 2);
        bird.turkeyStampedeHoldFrames++;
        double speed = (bird.turkeyStampedeUltimate ? 7.2 : 5.7) * (bird.isOnGround() ? 1.0 : 0.86);
        bird.vx = bird.vx * 0.42 + dir * speed;
        bird.vy *= bird.isOnGround() ? 0.74 : 0.90;

        if ((bird.turkeyStampedeHoldFrames & 3) == 0) {
            bird.game.particles.add(new Particle(
                    bird.bodyCenterX() - dir * 34.0 * bird.sizeMultiplier,
                    bird.bodyBottomY() - 5.0 * bird.sizeMultiplier,
                    -dir * (1.1 + SimRng.next() * 1.8),
                    -0.8 - SimRng.next() * 1.8,
                    (bird.turkeyStampedeUltimate ? Color.GOLD : Color.SADDLEBROWN).deriveColor(0, 1, 1, 0.65)
            ));
        }

        double centerX = bird.bodyCenterX();
        double centerY = bird.bodyCenterY();
        for (Bird other : bird.game.players) {
            if (!bird.canDamageTarget(other)) continue;
            if (other.playerIndex < 0 || other.playerIndex >= bird.turkeyStampedeHitCooldown.length) continue;
            if (bird.turkeyStampedeHitCooldown[other.playerIndex] > 0) continue;

            double forward = (other.bodyCenterX() - centerX) * dir;
            if (forward < -other.combatHalfWidth() * 0.45) continue;
            if (forward > (bird.turkeyStampedeUltimate ? 100.0 : 84.0) * bird.sizeMultiplier + other.combatHalfWidth()) continue;
            if (Math.abs(other.bodyCenterY() - centerY) > (bird.turkeyStampedeUltimate ? 78.0 : 66.0) * bird.sizeMultiplier + other.combatHalfHeight()) continue;

            bird.turkeyStampedeHitCooldown[other.playerIndex] = bird.turkeyStampedeUltimate ? 12 : 16;
            double oldHealth = other.health;
            int dealt = (int) bird.applyDamageTo(other, bird.turkeyStampedeUltimate ? 12 : 8);
            if (dealt <= 0) continue;

            bird.game.damageDealt[bird.playerIndex] += dealt;
            bird.game.recordSpecialImpact(bird.playerIndex, dealt, true);
            if (other.health <= 0 && oldHealth > 0) {
                bird.game.eliminations[bird.playerIndex]++;
            }

            other.vx += dir * (bird.turkeyStampedeUltimate ? 25.0 : 19.5);
            other.vy -= bird.turkeyStampedeUltimate ? 8.8 : 6.2;
            bird.game.hitstopFrames = Math.max(bird.game.hitstopFrames, bird.turkeyStampedeUltimate ? 3 : 1);
            bird.game.shakeIntensity = Math.max(bird.game.shakeIntensity, bird.turkeyStampedeUltimate ? 9 : 5);
            applyStuffedKnockbackBonus(bird, other, dir);
        }
    }

    private static void handlePanicFlap(Bird bird) {
        bird.vx *= 0.82;
        if (bird.vy > -7.0) {
            bird.vy -= bird.turkeyPanicFlapUltimate ? 0.95 : 0.68;
        }
        if ((bird.turkeyPanicFlapTimer & 1) == 0) {
            for (int side = -1; side <= 1; side += 2) {
                double wingX = bird.bodyCenterX() + side * 42.0 * bird.sizeMultiplier;
                double wingY = bird.bodyCenterY() + 8.0 * bird.sizeMultiplier;
                bird.game.particles.add(new Particle(
                        wingX,
                        wingY,
                        side * (1.4 + SimRng.next() * 1.6),
                        4.8 + SimRng.next() * 3.2,
                        (bird.turkeyPanicFlapUltimate ? Color.GOLD : Color.web("#F5F5F5")).deriveColor(0, 1, 1, 0.68)
                ));
            }
            if ((bird.turkeyPanicFlapTimer & 3) == 0) {
                bird.game.particles.add(new Particle(
                        bird.bodyCenterX() + (SimRng.next() - 0.5) * 36.0 * bird.sizeMultiplier,
                        bird.bodyBottomY() - 4.0 * bird.sizeMultiplier,
                        (SimRng.next() - 0.5) * 0.8,
                        7.0 + SimRng.next() * 4.5,
                        (bird.turkeyPanicFlapUltimate ? Color.web("#FFF59D") : Color.web("#D7CCC8")).deriveColor(0, 1, 1, 0.52)
                ));
            }
        }

        double centerX = bird.bodyCenterX();
        double centerY = bird.bodyCenterY();
        for (Bird other : bird.game.players) {
            if (!bird.canDamageTarget(other)) continue;
            if (other.playerIndex < 0 || other.playerIndex >= bird.turkeyPanicFlapHit.length) continue;
            if (bird.turkeyPanicFlapHit[other.playerIndex]) continue;

            double dx = other.bodyCenterX() - centerX;
            double dy = other.bodyCenterY() - centerY;
            if (Math.abs(dx) > (bird.turkeyPanicFlapUltimate ? 96.0 : 78.0) * bird.sizeMultiplier + other.combatHalfWidth()) continue;
            if (dy < 18.0 * bird.sizeMultiplier || dy > (bird.turkeyPanicFlapUltimate ? 205.0 : 165.0) * bird.sizeMultiplier + other.combatHalfHeight()) continue;

            bird.turkeyPanicFlapHit[other.playerIndex] = true;
            double oldHealth = other.health;
            int dealt = (int) bird.applyDamageTo(other, bird.turkeyPanicFlapUltimate ? 9 : 6);
            if (dealt <= 0) continue;

            bird.game.damageDealt[bird.playerIndex] += dealt;
            bird.game.recordSpecialImpact(bird.playerIndex, dealt, true);
            if (other.health <= 0 && oldHealth > 0) {
                bird.game.eliminations[bird.playerIndex]++;
            }
            double dir = Math.signum(dx == 0.0 ? bird.facingDirection() : dx);
            other.vx += dir * (bird.turkeyPanicFlapUltimate ? 5.5 : 3.8);
            other.vy = Math.max(other.vy, bird.turkeyPanicFlapUltimate ? 13.0 : 10.0);
            applyStuffedKnockbackBonus(bird, other, dir);
        }
    }
}
