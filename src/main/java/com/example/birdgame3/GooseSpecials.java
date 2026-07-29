package com.example.birdgame3;

import javafx.scene.paint.Color;

import java.util.Arrays;

final class GooseSpecials {
    private GooseSpecials() {
    }

    static final class GooseNest {
        double x;
        double y;
        int lifeFrames;
        int ageFrames;
        int pulseCooldown;
        boolean ultimate;

        GooseNest(double x, double y, boolean ultimate) {
            this.x = x;
            this.y = y;
            this.ultimate = ultimate;
            this.lifeFrames = ultimate ? Bird.GOOSE_NEST_ULTIMATE_LIFE_FRAMES : Bird.GOOSE_NEST_LIFE_FRAMES;
        }
    }

    static boolean active(Bird bird) {
        return bird.gooseHonkTimer > 0
                || bird.gooseBargeTimer > 0
                || bird.gooseLiftTimer > 0
                || bird.gooseNestGuardTimer > 0
                || bird.gooseNestCounterTimer > 0
                || bird.gooseUltimateTimer > 0;
    }

    static boolean ready(Bird bird, Bird.GooseSpecialVariant variant) {
        if (bird.isUltimateReady()) {
            return true;
        }
        return switch (variant) {
            case NEUTRAL -> bird.gooseHonkReuseTimer <= 0 && bird.gooseHonkTimer <= 0;
            case SIDE -> bird.gooseBargeReuseTimer <= 0 && bird.gooseBargeTimer <= 0;
            case UP -> !bird.gooseLiftUsed && bird.gooseLiftReuseTimer <= 0 && bird.gooseLiftTimer <= 0;
            case DOWN -> bird.gooseNestReuseTimer <= 0 && bird.gooseNestGuardTimer <= 0 && bird.gooseNestCounterTimer <= 0;
        };
    }

    static boolean canConvertShieldIntoDown(Bird bird) {
        return bird.type == BirdGame3.BirdType.GOOSE
                && bird.selectGooseSpecialVariant() == Bird.GooseSpecialVariant.DOWN
                && bird.isBlocking
                && bird.shieldStunFrames <= 0;
    }

    static boolean canStart(Bird bird, boolean grabbed, boolean dodging) {
        if (bird.type != BirdGame3.BirdType.GOOSE || bird.health <= 0 || bird.stunTime > 0.0) {
            return false;
        }
        Bird.GooseSpecialVariant variant = bird.selectGooseSpecialVariant();
        boolean shieldConversion = canConvertShieldIntoDown(bird);
        return !grabbed
                && (!bird.isBlocking || shieldConversion)
                && !dodging
                && !active(bird)
                && ready(bird, variant);
    }

    static void use(Bird bird, boolean ultimate) {
        if (ultimate) {
            startWholeFlock(bird);
            return;
        }
        boolean empowered = consumeTerritoryIfReady(bird);
        switch (bird.selectGooseSpecialVariant()) {
            case NEUTRAL -> neutral(bird, empowered);
            case SIDE -> side(bird, empowered);
            case UP -> up(bird, empowered);
            case DOWN -> down(bird, empowered);
        }
    }

    static void neutral(Bird bird, boolean empowered) {
        int dir = bird.horizontalInputDirection();
        if (dir == 0) {
            dir = bird.facingDirection();
        }
        bird.facingRight = dir > 0;
        bird.gooseHonkDirection = dir;
        bird.gooseHonkTimer = Bird.GOOSE_HONK_MAX_HOLD_FRAMES + Bird.GOOSE_HONK_RECOVERY_FRAMES
                + (empowered ? 10 : 0);
        bird.gooseHonkHoldFrames = 0;
        bird.gooseHonkReleased = false;
        bird.gooseHonkUltimate = false;
        bird.gooseHonkEmpowered = empowered;
        bird.gooseHonkReuseTimer = empowered ? 22 : Bird.GOOSE_HONK_REUSE_FRAMES;
        Arrays.fill(bird.gooseHonkHit, false);
        bird.attackAnimationTimer = Math.max(bird.attackAnimationTimer, bird.gooseHonkTimer);
        bird.specialCooldown = 0;
        bird.specialMaxCooldown = 0;
        bird.vx *= 0.42;
        bird.game.addToKillFeed(bird.shortName() + (empowered ? " claimed the pond with an ENFORCED HONK!" : " started a Threatening Honk!"));
        emitBurst(bird, bird.bodyCenterX(), bird.bodyCenterY() - 24.0 * bird.sizeMultiplier,
                empowered ? Color.GOLD : Color.web("#E8F5E9"), 20, 4.2);
    }

    private static void side(Bird bird, boolean empowered) {
        int dir = bird.horizontalInputDirection();
        if (dir == 0) {
            dir = bird.facingDirection();
        }
        bird.facingRight = dir > 0;
        bird.gooseBargeDirection = dir;
        bird.gooseBargeTimer = Bird.GOOSE_BARGE_FRAMES + (empowered ? 6 : 0);
        bird.gooseBargeReuseTimer = empowered ? 26 : Bird.GOOSE_BARGE_REUSE_FRAMES;
        bird.gooseBargeUltimate = false;
        bird.gooseBargeEmpowered = empowered;
        Arrays.fill(bird.gooseBargeHit, false);
        bird.attackAnimationTimer = Math.max(bird.attackAnimationTimer, bird.gooseBargeTimer + 5);
        bird.specialCooldown = 0;
        bird.specialMaxCooldown = 0;
        bird.vx = dir * (empowered ? 18.5 : 15.5);
        bird.vy = Math.min(bird.vy, bird.isOnGround() ? -1.5 : 1.0);
        bird.game.addToKillFeed(bird.shortName() + (empowered ? " launched an empowered Bite and Barge!" : " charged Bite and Barge!"));
    }

    private static void up(Bird bird, boolean empowered) {
        if (bird.gooseLiftUsed && !empowered) {
            return;
        }
        int dir = bird.horizontalInputDirection();
        if (dir != 0) {
            bird.facingRight = dir > 0;
        } else {
            dir = bird.facingDirection();
        }
        bird.gooseLiftUsed = true;
        bird.gooseLiftDirection = dir;
        bird.gooseLiftTimer = empowered ? Bird.GOOSE_LIFT_ULTIMATE_FRAMES : Bird.GOOSE_LIFT_FRAMES;
        bird.gooseLiftReuseTimer = empowered ? 18 : Bird.GOOSE_LIFT_REUSE_FRAMES;
        bird.gooseLiftUltimate = false;
        bird.gooseLiftEmpowered = empowered;
        Arrays.fill(bird.gooseLiftHit, false);
        bird.attackAnimationTimer = Math.max(bird.attackAnimationTimer, bird.gooseLiftTimer);
        bird.specialCooldown = 0;
        bird.specialMaxCooldown = 0;
        bird.canDoubleJump = true;
        bird.vx = bird.vx * 0.44 + dir * (empowered ? 5.2 : 3.8);
        bird.vy = empowered ? -19.2 : -16.4;
        emitBurst(bird, bird.bodyCenterX(), bird.bodyBottomY(), empowered ? Color.GOLD : Color.web("#E0F2F1"), 34, 6.0);
        bird.game.addToKillFeed(bird.shortName() + (empowered ? " rode an empowered V-Formation Lift!" : " lifted on V-Formation wings!"));
    }

    private static void down(Bird bird, boolean empowered) {
        bird.gooseNestReuseTimer = empowered ? 22 : Bird.GOOSE_NEST_REUSE_FRAMES;
        bird.gooseNestEmpowered = empowered;
        bird.specialCooldown = 0;
        bird.specialMaxCooldown = 0;
        bird.attackAnimationTimer = Math.max(bird.attackAnimationTimer, 18);
        bird.isBlocking = false;
        bird.parryWindowFrames = 0;

        if (bird.gooseNest == null || bird.gooseNest.lifeFrames <= 0 || distanceToNest(bird) > 150.0 * bird.sizeMultiplier) {
            double nestY = bird.bodyBottomY() + 5.0 * bird.sizeMultiplier;
            bird.gooseNest = new GooseNest(bird.bodyCenterX(), nestY, empowered);
            addTerritory(bird, empowered ? 18.0 : 10.0);
            bird.game.addToKillFeed(bird.shortName() + (empowered ? " built a golden Nest Guard!" : " planted a Nest Guard!"));
            emitBurst(bird, bird.gooseNest.x, bird.gooseNest.y - 24.0 * bird.sizeMultiplier,
                    empowered ? Color.GOLD : Color.web("#8D6E63"), 42, 5.4);
            return;
        }

        bird.gooseNestGuardTimer = empowered ? Bird.GOOSE_NEST_GUARD_ULTIMATE_FRAMES : Bird.GOOSE_NEST_GUARD_FRAMES;
        bird.gooseNestGuardUltimate = empowered;
        bird.gooseNestCounterUltimate = false;
        bird.gooseNestCounterTimer = 0;
        Arrays.fill(bird.gooseNestCounterHit, false);
        bird.vx *= 0.38;
        bird.game.addToKillFeed(bird.shortName() + (empowered ? " dared anyone to touch the golden nest!" : " guarded the nest!"));
    }

    private static void startWholeFlock(Bird bird) {
        int dir = bird.horizontalInputDirection();
        if (dir == 0) {
            dir = bird.facingDirection();
        }
        bird.facingRight = dir > 0;
        bird.gooseUltimateDirection = dir;
        bird.gooseUltimateTimer = Bird.GOOSE_ULTIMATE_FRAMES;
        bird.gooseUltimateWaveIndex = 0;
        bird.gooseUltimateFinalHitResolved = false;
        Arrays.fill(bird.gooseUltimateMarked, false);
        Arrays.fill(bird.gooseUltimateHitCooldown, 0);
        bird.gooseTerritoryMeter = Bird.GOOSE_TERRITORY_MAX;
        bird.attackAnimationTimer = Math.max(bird.attackAnimationTimer, Bird.GOOSE_ULTIMATE_FRAMES / 2);
        bird.specialCooldown = 0;
        bird.specialMaxCooldown = 0;
        bird.game.addToKillFeed(bird.shortName() + " called THE WHOLE FLOCK!");
        bird.game.shakeIntensity = Math.max(bird.game.shakeIntensity, 24);
        bird.game.hitstopFrames = Math.max(bird.game.hitstopFrames, 10);
        bird.game.triggerFlash(0.48, false);

        Bird closest = null;
        double closestDist = Double.POSITIVE_INFINITY;
        for (Bird other : bird.game.players) {
            if (!bird.canDamageTarget(other)) {
                continue;
            }
            double dx = other.bodyCenterX() - bird.bodyCenterX();
            double dy = Math.abs(other.bodyCenterY() - bird.bodyCenterY());
            double forward = dx * dir;
            double dist = Math.hypot(dx, dy);
            if (dist < closestDist) {
                closest = other;
                closestDist = dist;
            }
            if (forward >= -90.0 * bird.sizeMultiplier
                    && forward <= 760.0 * bird.sizeMultiplier + other.combatHalfWidth()
                    && dy <= 280.0 * bird.sizeMultiplier + other.combatHalfHeight()) {
                markUltimateTarget(bird, other, 7, dir);
            }
        }
        if (!anyMarked(bird) && closest != null) {
            markUltimateTarget(bird, closest, 9, dir);
        }
        emitBurst(bird, bird.bodyCenterX(), bird.bodyCenterY(), Color.GOLD, 90, 12.0);
    }

    static void handleState(Bird bird, boolean specialHeld) {
        decrementReuseTimers(bird);
        if (bird.type == BirdGame3.BirdType.GOOSE && bird.health > 0) {
            buildTerritory(bird);
        }
        handleNestLife(bird);
        if (bird.health <= 0) {
            return;
        }
        handleHonk(bird, specialHeld);
        handleBargeMovement(bird);
        handleLiftMovement(bird);
        handleNestGuardTimer(bird);
        handleUltimate(bird);
    }

    static void handlePostMoveState(Bird bird) {
        if (bird.health <= 0) {
            return;
        }
        applyBargeHits(bird);
        applyLiftHits(bird);
        applyNestAura(bird);
    }

    static void advancePresentationFrame(Bird bird) {
        if (bird.gooseNest != null) {
            bird.gooseNest.ageFrames++;
        }
        if (bird.gooseHonkTimer > 0 || bird.gooseBargeTimer > 0 || bird.gooseLiftTimer > 0
                || bird.gooseNestGuardTimer > 0 || bird.gooseNestCounterTimer > 0 || bird.gooseUltimateTimer > 0) {
            handleState(bird, true);
        }
    }

    private static void decrementReuseTimers(Bird bird) {
        bird.gooseHonkReuseTimer = Math.max(0, bird.gooseHonkReuseTimer - 1);
        bird.gooseBargeReuseTimer = Math.max(0, bird.gooseBargeReuseTimer - 1);
        bird.gooseLiftReuseTimer = Math.max(0, bird.gooseLiftReuseTimer - 1);
        bird.gooseNestReuseTimer = Math.max(0, bird.gooseNestReuseTimer - 1);
        for (int i = 0; i < bird.gooseUltimateHitCooldown.length; i++) {
            bird.gooseUltimateHitCooldown[i] = Math.max(0, bird.gooseUltimateHitCooldown[i] - 1);
        }
    }

    private static void buildTerritory(Bird bird) {
        if (bird.isBlocking) {
            addTerritory(bird, 0.22);
        }
        double centerPressure = 1.0 - Math.clamp(Math.abs(bird.bodyCenterX() - BirdGame3.WORLD_WIDTH * 0.5) / 780.0, 0.0, 1.0);
        if (bird.isOnGround()) {
            addTerritory(bird, 0.025 + centerPressure * 0.04);
        }
        if (bird.gooseNest != null && bird.gooseNest.lifeFrames > 0 && distanceToNest(bird) < 180.0 * bird.sizeMultiplier) {
            addTerritory(bird, 0.08);
        }
    }

    private static void handleNestLife(Bird bird) {
        if (bird.gooseNest == null) {
            return;
        }
        bird.gooseNest.lifeFrames--;
        bird.gooseNest.ageFrames++;
        bird.gooseNest.pulseCooldown = Math.max(0, bird.gooseNest.pulseCooldown - 1);
        if (bird.gooseNest.lifeFrames <= 0) {
            bird.gooseNest = null;
        }
    }

    private static void handleHonk(Bird bird, boolean specialHeld) {
        if (bird.gooseHonkTimer <= 0) {
            return;
        }
        if (!bird.gooseHonkReleased) {
            bird.gooseHonkHoldFrames++;
            int maxHold = honkMaxHoldFrames(bird);
            boolean minimumTellComplete = bird.gooseHonkHoldFrames >= Bird.GOOSE_HONK_MIN_HOLD_FRAMES;
            if ((minimumTellComplete && !specialHeld) || bird.gooseHonkHoldFrames >= maxHold) {
                releaseHonk(bird);
            }
        }
        bird.gooseHonkTimer--;
        if (bird.gooseHonkTimer <= 0) {
            bird.gooseHonkHoldFrames = 0;
            bird.gooseHonkReleased = false;
            bird.gooseHonkUltimate = false;
            bird.gooseHonkEmpowered = false;
            Arrays.fill(bird.gooseHonkHit, false);
        }
    }

    private static void releaseHonk(Bird bird) {
        bird.gooseHonkReleased = true;
        bird.gooseHonkTimer = Bird.GOOSE_HONK_RECOVERY_FRAMES + (bird.gooseHonkEmpowered ? 4 : 0);
        double chargeRatio = honkChargeRatio(bird);
        double chargeStrength = smoothStep(chargeRatio);
        int dir = bird.gooseHonkDirection == 0 ? bird.facingDirection() : bird.gooseHonkDirection;
        double reach = honkReach(bird, chargeStrength);
        double originX = bird.bodyCenterX() + dir * 18.0 * bird.sizeMultiplier;
        double originY = bird.bodyCenterY() - 12.0 * bird.sizeMultiplier;
        boolean hitAny = false;
        for (Bird other : bird.game.players) {
            if (!bird.canDamageTarget(other)) {
                continue;
            }
            int idx = other.playerIndex;
            if (idx >= 0 && idx < bird.gooseHonkHit.length && bird.gooseHonkHit[idx]) {
                continue;
            }
            double forward = (other.bodyCenterX() - originX) * dir;
            if (forward < -other.combatHalfWidth() || forward > reach + other.combatHalfWidth()) {
                continue;
            }
            double dy = Math.abs(other.bodyCenterY() - originY);
            double cone = (58.0 + forward * 0.22 + chargeRatio * 24.0) * bird.sizeMultiplier;
            if (dy > cone + other.combatHalfHeight()) {
                continue;
            }
            if (idx >= 0 && idx < bird.gooseHonkHit.length) {
                bird.gooseHonkHit[idx] = true;
            }
            // Edge hits keep enough shove to reset spacing, while their stun fades sharply.
            double distanceStrength = honkDistanceStrength(forward, reach);
            double launchStrength = honkLaunchStrength(forward, reach);
            int damage = (int) Math.round(7.0 + chargeStrength * 7.0
                    + (bird.gooseHonkEmpowered ? 3.0 : 0.0));
            int dealt = bird.applyTrackedSpecialDamage(other, damage);
            hitAny |= dealt > 0;
            if (dealt > 0) {
                double horizontalLaunch = (4.2 + chargeStrength * 4.8
                        + (bird.gooseHonkEmpowered ? 1.6 : 0.0)) * launchStrength;
                double verticalLaunch = (1.6 + chargeStrength * 1.8
                        + (bird.gooseHonkEmpowered ? 0.6 : 0.0)) * (0.70 + distanceStrength * 0.30);
                double stunFrames = 2.0 + (chargeStrength * 6.0
                        + (bird.gooseHonkEmpowered ? 1.0 : 0.0)) * distanceStrength;
                other.vx += dir * horizontalLaunch;
                other.vy -= verticalLaunch;
                other.applyStun(stunFrames);
                addTerritory(bird, 7.0 + dealt * 0.16);
                emitHitBurst(bird, other, bird.gooseHonkEmpowered ? Color.GOLD : Color.web("#E8F5E9"),
                        10 + (int) Math.round(distanceStrength * 8.0));
            }
        }
        bird.game.shakeIntensity = Math.max(bird.game.shakeIntensity, hitAny ? 13 : 6);
        bird.game.hitstopFrames = Math.max(bird.game.hitstopFrames, hitAny ? 5 : 2);
        bird.game.triggerFlash(hitAny ? 0.20 : 0.09, false);
    }

    static int honkMaxHoldFrames(Bird bird) {
        return Bird.GOOSE_HONK_MAX_HOLD_FRAMES + (bird.gooseHonkEmpowered ? 12 : 0);
    }

    static double honkChargeRatio(Bird bird) {
        return Math.clamp(bird.gooseHonkHoldFrames / (double) honkMaxHoldFrames(bird), 0.0, 1.0);
    }

    private static double honkReach(Bird bird, double chargeStrength) {
        double maxReach = bird.gooseHonkEmpowered ? 360.0 : 280.0;
        return maxReach * bird.sizeMultiplier * (0.62 + chargeStrength * 0.38);
    }

    private static double honkDistanceStrength(double forward, double reach) {
        double distanceRatio = Math.clamp(Math.max(0.0, forward) / Math.max(1.0, reach), 0.0, 1.0);
        return 1.0 - distanceRatio * 0.65;
    }

    private static double honkLaunchStrength(double forward, double reach) {
        double distanceRatio = Math.clamp(Math.max(0.0, forward) / Math.max(1.0, reach), 0.0, 1.0);
        return 1.0 - distanceRatio * 0.50;
    }

    private static double smoothStep(double value) {
        double clamped = Math.clamp(value, 0.0, 1.0);
        return clamped * clamped * (3.0 - 2.0 * clamped);
    }

    private static void handleBargeMovement(Bird bird) {
        if (bird.gooseBargeTimer <= 0) {
            return;
        }
        int dir = bird.gooseBargeDirection == 0 ? bird.facingDirection() : bird.gooseBargeDirection;
        double total = Bird.GOOSE_BARGE_FRAMES + (bird.gooseBargeEmpowered || bird.gooseBargeUltimate ? 6.0 : 0.0);
        double elapsed = total - bird.gooseBargeTimer;
        double speed = elapsed < 8.0 ? (bird.gooseBargeEmpowered ? 19.0 : 16.5)
                : elapsed < total - 7.0 ? (bird.gooseBargeEmpowered ? 15.5 : 13.0)
                : 7.5;
        bird.vx = dir * speed;
        if (!bird.isOnGround()) {
            bird.vy = Math.min(bird.vy + 0.08, 3.2);
        }
        if (bird.gooseBargeTimer % 4 == 0) {
            bird.game.particles.add(new Particle(
                    bird.bodyCenterX() - dir * 32.0 * bird.sizeMultiplier,
                    bird.bodyBottomY() - 8.0 * bird.sizeMultiplier,
                    -dir * (1.8 + bird.game.nextParticleRandom() * 2.4),
                    -0.8 - bird.game.nextParticleRandom() * 1.6,
                    (bird.gooseBargeEmpowered ? Color.GOLD : Color.web("#BCAAA4")).deriveColor(0, 1, 1, 0.72)
            ));
        }
        bird.gooseBargeTimer--;
        if (bird.gooseBargeTimer <= 0) {
            bird.gooseBargeUltimate = false;
            bird.gooseBargeEmpowered = false;
            Arrays.fill(bird.gooseBargeHit, false);
        }
    }

    private static void applyBargeHits(Bird bird) {
        if (bird.gooseBargeTimer <= 0) {
            return;
        }
        int dir = bird.gooseBargeDirection == 0 ? bird.facingDirection() : bird.gooseBargeDirection;
        double centerX = bird.bodyCenterX() + dir * 58.0 * bird.sizeMultiplier;
        double centerY = bird.bodyCenterY() - 2.0 * bird.sizeMultiplier;
        double reach = (bird.gooseBargeEmpowered ? 126.0 : 104.0) * bird.sizeMultiplier;
        double vertical = (bird.gooseBargeEmpowered ? 82.0 : 70.0) * bird.sizeMultiplier;
        for (Bird other : bird.game.players) {
            if (!bird.canDamageTarget(other)) {
                continue;
            }
            int idx = other.playerIndex;
            if (idx >= 0 && idx < bird.gooseBargeHit.length && bird.gooseBargeHit[idx]) {
                continue;
            }
            double forward = (other.bodyCenterX() - centerX) * dir;
            double dy = Math.abs(other.bodyCenterY() - centerY);
            if (forward < -other.combatHalfWidth() || forward > reach + other.combatHalfWidth()
                    || dy > vertical + other.combatHalfHeight()) {
                continue;
            }
            if (idx >= 0 && idx < bird.gooseBargeHit.length) {
                bird.gooseBargeHit[idx] = true;
            }
            boolean bite = forward < (58.0 + other.combatHalfWidth()) * bird.sizeMultiplier;
            int dealt = bird.applyTrackedSpecialDamage(other, bite
                    ? (bird.gooseBargeEmpowered ? 16 : 12)
                    : (bird.gooseBargeEmpowered ? 13 : 9));
            if (dealt > 0) {
                other.vx += dir * (bite ? (bird.gooseBargeEmpowered ? 14.0 : 11.2) : 8.6);
                other.vy -= bite ? (bird.gooseBargeEmpowered ? 6.8 : 5.1) : 3.0;
                other.applyStun(bite ? (bird.gooseBargeEmpowered ? 15 : 11) : 7);
                bird.game.shakeIntensity = Math.max(bird.game.shakeIntensity, bite ? 16 : 9);
                bird.game.hitstopFrames = Math.max(bird.game.hitstopFrames, bite ? 7 : 4);
                addTerritory(bird, 9.0 + dealt * 0.14);
                emitHitBurst(bird, other, bird.gooseBargeEmpowered ? Color.GOLD : Color.web("#FFB74D"), bite ? 26 : 16);
            }
        }
    }

    private static void handleLiftMovement(Bird bird) {
        if (bird.gooseLiftTimer <= 0) {
            return;
        }
        int dir = bird.gooseLiftDirection == 0 ? bird.facingDirection() : bird.gooseLiftDirection;
        if (bird.gooseLiftTimer > 12) {
            bird.vy = Math.min(bird.vy, bird.gooseLiftEmpowered ? -12.0 : -10.2);
            bird.vx += dir * (bird.gooseLiftEmpowered ? 0.20 : 0.14);
        }
        if (bird.gooseLiftTimer % 5 == 0) {
            bird.game.particles.add(new Particle(
                    bird.bodyCenterX() + (bird.game.nextParticleRandom() - 0.5) * 56.0 * bird.sizeMultiplier,
                    bird.bodyBottomY(),
                    (bird.game.nextParticleRandom() - 0.5) * 3.0,
                    -2.5 - bird.game.nextParticleRandom() * 4.5,
                    (bird.gooseLiftEmpowered ? Color.GOLD : Color.web("#E0F2F1")).deriveColor(0, 1, 1, 0.66)
            ));
        }
        bird.gooseLiftTimer--;
        if (bird.gooseLiftTimer <= 0) {
            bird.gooseLiftUltimate = false;
            bird.gooseLiftEmpowered = false;
            Arrays.fill(bird.gooseLiftHit, false);
            if (bird.isOnGround()) {
                bird.applySpecialLandingLag(10);
            }
        }
    }

    private static void applyLiftHits(Bird bird) {
        if (bird.gooseLiftTimer <= 0) {
            return;
        }
        double centerX = bird.bodyCenterX();
        double centerY = bird.bodyCenterY() + 14.0 * bird.sizeMultiplier;
        double reach = (bird.gooseLiftEmpowered ? 92.0 : 72.0) * bird.sizeMultiplier;
        double vertical = (bird.gooseLiftEmpowered ? 102.0 : 84.0) * bird.sizeMultiplier;
        for (Bird other : bird.game.players) {
            if (!bird.canDamageTarget(other)) {
                continue;
            }
            int idx = other.playerIndex;
            if (idx >= 0 && idx < bird.gooseLiftHit.length && bird.gooseLiftHit[idx]) {
                continue;
            }
            if (Math.abs(other.bodyCenterX() - centerX) > reach + other.combatHalfWidth()
                    || Math.abs(other.bodyCenterY() - centerY) > vertical + other.combatHalfHeight()) {
                continue;
            }
            if (idx >= 0 && idx < bird.gooseLiftHit.length) {
                bird.gooseLiftHit[idx] = true;
            }
            int dealt = bird.applyTrackedSpecialDamage(other, bird.gooseLiftEmpowered ? 9 : 6);
            if (dealt > 0) {
                double push = Math.signum(other.bodyCenterX() - centerX);
                if (push == 0.0) {
                    push = bird.facingDirection();
                }
                other.vx += push * (bird.gooseLiftEmpowered ? 5.4 : 3.7);
                other.vy -= bird.gooseLiftEmpowered ? 9.0 : 6.8;
                other.applyStun(bird.gooseLiftEmpowered ? 9 : 7);
                bird.game.shakeIntensity = Math.max(bird.game.shakeIntensity, bird.gooseLiftEmpowered ? 10 : 6);
                bird.game.hitstopFrames = Math.max(bird.game.hitstopFrames, bird.gooseLiftEmpowered ? 5 : 3);
                addTerritory(bird, 10.0 + dealt * 0.18);
                emitHitBurst(bird, other, bird.gooseLiftEmpowered ? Color.GOLD : Color.web("#E0F2F1"), 18);
            }
        }
    }

    private static void handleNestGuardTimer(Bird bird) {
        if (bird.gooseNestGuardTimer > 0) {
            bird.vx *= 0.78;
            addTerritory(bird, 0.10);
            bird.gooseNestGuardTimer--;
            if (bird.gooseNestGuardTimer <= 0) {
                bird.gooseNestGuardUltimate = false;
                bird.gooseNestEmpowered = false;
            }
        }
        if (bird.gooseNestCounterTimer > 0) {
            bird.gooseNestCounterTimer--;
            if (bird.gooseNestCounterTimer <= 0) {
                bird.gooseNestCounterUltimate = false;
                Arrays.fill(bird.gooseNestCounterHit, false);
            }
        }
    }

    static boolean tryNestCounter(Bird defender, Bird attacker, double scaledDamage) {
        if (defender.type != BirdGame3.BirdType.GOOSE || attacker == null || attacker.health <= 0) {
            return false;
        }
        if (defender.gooseNestGuardTimer <= 0 || defender.gooseNestCounterTimer > 0) {
            return false;
        }
        double range = (defender.gooseNestGuardUltimate ? 260.0 : 210.0) * defender.sizeMultiplier;
        boolean attackerNearDefender = defender.combatDistanceTo(attacker) <= range + attacker.combatRadius();
        boolean attackerNearNest = defender.gooseNest != null
                && Math.hypot(attacker.bodyCenterX() - defender.gooseNest.x,
                attacker.bodyCenterY() - defender.gooseNest.y) <= range + attacker.combatRadius();
        if (!attackerNearDefender && !attackerNearNest) {
            return false;
        }
        defender.gooseNestCounterTimer = defender.gooseNestGuardUltimate
                ? Bird.GOOSE_COUNTER_ULTIMATE_BURST_FRAMES
                : Bird.GOOSE_COUNTER_BURST_FRAMES;
        defender.gooseNestCounterUltimate = defender.gooseNestGuardUltimate || scaledDamage >= 18.0;
        defender.gooseNestGuardTimer = 0;
        defender.gooseNestGuardUltimate = false;
        Arrays.fill(defender.gooseNestCounterHit, false);
        defender.attackAnimationTimer = Math.max(defender.attackAnimationTimer, defender.gooseNestCounterTimer + 6);
        defender.game.shakeIntensity = Math.max(defender.game.shakeIntensity, defender.gooseNestCounterUltimate ? 20 : 14);
        defender.game.hitstopFrames = Math.max(defender.game.hitstopFrames, defender.gooseNestCounterUltimate ? 8 : 5);
        defender.game.addToKillFeed(defender.shortName() + " countered from the Nest Guard!");
        counterHit(defender, attacker);
        addTerritory(defender, 18.0);
        return true;
    }

    private static void counterHit(Bird defender, Bird attacker) {
        int dir = attacker.bodyCenterX() >= defender.bodyCenterX() ? 1 : -1;
        int dealt = recordDirectSpecialDamage(defender, attacker, defender.gooseNestCounterUltimate ? 18 : 12);
        if (dealt > 0) {
            attacker.vx += dir * (defender.gooseNestCounterUltimate ? 11.5 : 8.8);
            attacker.vy -= defender.gooseNestCounterUltimate ? 6.8 : 4.8;
            attacker.applyStun(defender.gooseNestCounterUltimate ? 16 : 11);
            emitHitBurst(defender, attacker, defender.gooseNestCounterUltimate ? Color.GOLD : Color.web("#FFF176"), 30);
        }
    }

    private static void applyNestAura(Bird bird) {
        if (bird.gooseNest == null || bird.gooseNest.lifeFrames <= 0 || bird.health <= 0) {
            return;
        }
        for (Bird other : bird.game.players) {
            if (!bird.canDamageTarget(other)) {
                continue;
            }
            double dx = Math.abs(other.bodyCenterX() - bird.gooseNest.x);
            double dy = Math.abs(other.bodyCenterY() - bird.gooseNest.y);
            if (dx > (bird.gooseNest.ultimate ? 132.0 : 112.0) * bird.sizeMultiplier + other.combatHalfWidth()
                    || dy > (bird.gooseNest.ultimate ? 74.0 : 58.0) * bird.sizeMultiplier + other.combatHalfHeight()) {
                continue;
            }
            other.vx *= bird.gooseNest.ultimate ? 0.88 : 0.92;
            if (bird.gooseNest.pulseCooldown <= 0) {
                int dealt = bird.applyTrackedSpecialDamage(other, bird.gooseNest.ultimate ? 6 : 3);
                if (dealt > 0) {
                    double dir = Math.signum(other.bodyCenterX() - bird.gooseNest.x);
                    if (dir == 0.0) {
                        dir = bird.facingDirection();
                    }
                    other.vx += dir * (bird.gooseNest.ultimate ? 4.0 : 2.6);
                    other.vy -= bird.gooseNest.ultimate ? 2.2 : 1.3;
                    other.applyStun(bird.gooseNest.ultimate ? 6 : 4);
                    addTerritory(bird, 8.0);
                    emitHitBurst(bird, other, bird.gooseNest.ultimate ? Color.GOLD : Color.web("#8BC5A1"), 10);
                }
                bird.gooseNest.pulseCooldown = bird.gooseNest.ultimate ? 24 : 32;
            }
        }
    }

    private static void handleUltimate(Bird bird) {
        if (bird.gooseUltimateTimer <= 0) {
            return;
        }
        int elapsed = Bird.GOOSE_ULTIMATE_FRAMES - bird.gooseUltimateTimer;
        int[] waveFrames = {30, 58, 86, 114};
        while (bird.gooseUltimateWaveIndex < waveFrames.length
                && elapsed >= waveFrames[bird.gooseUltimateWaveIndex]) {
            applyUltimateWave(bird, bird.gooseUltimateWaveIndex);
            bird.gooseUltimateWaveIndex++;
        }
        if (!bird.gooseUltimateFinalHitResolved && elapsed >= 145) {
            applyUltimateFinal(bird);
            bird.gooseUltimateFinalHitResolved = true;
        }
        bird.gooseUltimateTimer--;
        if (bird.gooseUltimateTimer <= 0) {
            Arrays.fill(bird.gooseUltimateMarked, false);
            Arrays.fill(bird.gooseUltimateHitCooldown, 0);
            bird.gooseUltimateWaveIndex = 0;
            bird.gooseUltimateFinalHitResolved = false;
        }
    }

    private static void applyUltimateWave(Bird bird, int waveIndex) {
        int dir = waveIndex % 2 == 0 ? bird.gooseUltimateDirection : -bird.gooseUltimateDirection;
        boolean hitAny = false;
        for (Bird other : bird.game.players) {
            if (!bird.canDamageTarget(other)) {
                continue;
            }
            int idx = other.playerIndex;
            boolean marked = idx >= 0 && idx < bird.gooseUltimateMarked.length && bird.gooseUltimateMarked[idx];
            double laneY = 150.0 + waveIndex * 105.0;
            boolean inLane = Math.abs(other.bodyCenterY() - laneY) <= 120.0 + other.combatHalfHeight();
            if (!marked && !inLane) {
                continue;
            }
            int dealt = bird.applyTrackedSpecialDamage(other, marked ? 11 : 8);
            if (dealt > 0) {
                hitAny = true;
                other.vx += dir * (marked ? 11.0 : 8.0);
                other.vy -= marked ? 5.0 : 3.2;
                other.applyStun(marked ? 11 : 7);
                emitHitBurst(bird, other, Color.GOLD, marked ? 18 : 10);
            }
        }
        bird.game.shakeIntensity = Math.max(bird.game.shakeIntensity, hitAny ? 14 : 7);
        bird.game.hitstopFrames = Math.max(bird.game.hitstopFrames, hitAny ? 4 : 2);
    }

    private static void applyUltimateFinal(Bird bird) {
        boolean hitAny = false;
        int dir = bird.gooseUltimateDirection == 0 ? bird.facingDirection() : bird.gooseUltimateDirection;
        for (Bird other : bird.game.players) {
            if (!bird.canDamageTarget(other)) {
                continue;
            }
            int idx = other.playerIndex;
            boolean marked = idx >= 0 && idx < bird.gooseUltimateMarked.length && bird.gooseUltimateMarked[idx];
            double dist = Math.hypot(other.bodyCenterX() - bird.bodyCenterX(), other.bodyCenterY() - bird.bodyCenterY());
            if (!marked && dist > 560.0 * bird.sizeMultiplier) {
                continue;
            }
            int dealt = bird.applyTrackedSpecialDamage(other, marked ? 30 : 22);
            if (dealt > 0) {
                hitAny = true;
                int launchDir = other.bodyCenterX() >= bird.bodyCenterX() ? 1 : -1;
                if (Math.abs(other.bodyCenterX() - bird.bodyCenterX()) < 30.0 * bird.sizeMultiplier) {
                    launchDir = dir;
                }
                other.vx += launchDir * (marked ? 22.0 : 17.0);
                other.vy -= marked ? 13.0 : 9.0;
                other.applyStun(marked ? 28 : 19);
                emitHitBurst(bird, other, Color.GOLD, 44);
            }
        }
        bird.game.shakeIntensity = Math.max(bird.game.shakeIntensity, hitAny ? 32 : 18);
        bird.game.hitstopFrames = Math.max(bird.game.hitstopFrames, hitAny ? 13 : 7);
        bird.game.triggerFlash(hitAny ? 0.62 : 0.30, hitAny);
    }

    private static void markUltimateTarget(Bird bird, Bird other, int damage, int dir) {
        int idx = other.playerIndex;
        if (idx >= 0 && idx < bird.gooseUltimateMarked.length) {
            bird.gooseUltimateMarked[idx] = true;
        }
        int dealt = bird.applyTrackedSpecialDamage(other, damage);
        if (dealt > 0) {
            other.vx += dir * 4.5;
            other.vy -= 2.0;
            other.applyStun(20);
            emitHitBurst(bird, other, Color.GOLD, 14);
        }
    }

    private static boolean anyMarked(Bird bird) {
        for (boolean marked : bird.gooseUltimateMarked) {
            if (marked) {
                return true;
            }
        }
        return false;
    }

    static void reset(Bird bird, boolean clearObjects) {
        bird.gooseHonkTimer = 0;
        bird.gooseHonkHoldFrames = 0;
        bird.gooseHonkDirection = bird.facingDirection();
        bird.gooseHonkUltimate = false;
        bird.gooseHonkEmpowered = false;
        bird.gooseHonkReleased = false;
        Arrays.fill(bird.gooseHonkHit, false);
        bird.gooseBargeTimer = 0;
        bird.gooseBargeDirection = bird.facingDirection();
        bird.gooseBargeUltimate = false;
        bird.gooseBargeEmpowered = false;
        Arrays.fill(bird.gooseBargeHit, false);
        bird.gooseLiftTimer = 0;
        bird.gooseLiftDirection = bird.facingDirection();
        bird.gooseLiftUltimate = false;
        bird.gooseLiftEmpowered = false;
        Arrays.fill(bird.gooseLiftHit, false);
        bird.gooseNestGuardTimer = 0;
        bird.gooseNestCounterTimer = 0;
        bird.gooseNestGuardUltimate = false;
        bird.gooseNestCounterUltimate = false;
        bird.gooseNestEmpowered = false;
        Arrays.fill(bird.gooseNestCounterHit, false);
        bird.gooseUltimateTimer = 0;
        bird.gooseUltimateDirection = bird.facingDirection();
        bird.gooseUltimateWaveIndex = 0;
        bird.gooseUltimateFinalHitResolved = false;
        Arrays.fill(bird.gooseUltimateMarked, false);
        Arrays.fill(bird.gooseUltimateHitCooldown, 0);
        if (clearObjects) {
            bird.gooseHonkReuseTimer = 0;
            bird.gooseBargeReuseTimer = 0;
            bird.gooseLiftReuseTimer = 0;
            bird.gooseLiftUsed = false;
            bird.gooseNestReuseTimer = 0;
            bird.gooseNest = null;
            bird.gooseTerritoryMeter = 0.0;
        }
    }

    private static boolean consumeTerritoryIfReady(Bird bird) {
        if (bird.gooseTerritoryMeter < Bird.GOOSE_TERRITORY_MAX) {
            return false;
        }
        bird.gooseTerritoryMeter = 0.0;
        bird.game.addToKillFeed(bird.shortName() + " spent TERRITORY for an empowered special!");
        return true;
    }

    private static void addTerritory(Bird bird, double amount) {
        if (bird.type != BirdGame3.BirdType.GOOSE || amount <= 0.0 || bird.health <= 0) {
            return;
        }
        bird.gooseTerritoryMeter = Math.clamp(bird.gooseTerritoryMeter + amount, 0.0, Bird.GOOSE_TERRITORY_MAX);
    }

    private static double distanceToNest(Bird bird) {
        if (bird.gooseNest == null) {
            return Double.POSITIVE_INFINITY;
        }
        return Math.hypot(bird.bodyCenterX() - bird.gooseNest.x, bird.bodyCenterY() - bird.gooseNest.y);
    }

    private static int recordDirectSpecialDamage(Bird bird, Bird target, int rawDamage) {
        if (target == null || target.health <= 0) {
            return 0;
        }
        double oldHealth = target.health;
        int dealt = (int) bird.applyUnshieldedDamageTo(target, rawDamage);
        if (dealt <= 0) {
            return 0;
        }
        bird.game.damageDealt[bird.playerIndex] += dealt;
        bird.game.recordSpecialImpact(bird.playerIndex, dealt, true);
        bird.confirmSpecialHit(dealt, Color.GOLD);
        if (target.health <= 0 && oldHealth > 0) {
            bird.game.eliminations[bird.playerIndex]++;
        }
        return dealt;
    }

    private static void emitHitBurst(Bird bird, Bird target, Color color, int requested) {
        int count = bird.scaledParticleCount(requested);
        for (int i = 0; i < count; i++) {
            double angle = bird.game.nextParticleRandom() * Math.PI * 2.0;
            double speed = 2.0 + bird.game.nextParticleRandom() * 5.5;
            bird.game.particles.add(new Particle(
                    target.bodyCenterX() + (bird.game.nextParticleRandom() - 0.5) * target.combatHalfWidth(),
                    target.bodyCenterY() + (bird.game.nextParticleRandom() - 0.5) * target.combatHalfHeight(),
                    Math.cos(angle) * speed,
                    Math.sin(angle) * speed - 1.4,
                    color.deriveColor(0, 1, 1, 0.74)
            ));
        }
    }

    private static void emitBurst(Bird bird, double x, double y, Color color, int requested, double maxSpeed) {
        int count = bird.scaledParticleCount(requested);
        for (int i = 0; i < count; i++) {
            double angle = bird.game.nextParticleRandom() * Math.PI * 2.0;
            double speed = 1.0 + bird.game.nextParticleRandom() * maxSpeed;
            bird.game.particles.add(new Particle(
                    x + (bird.game.nextParticleRandom() - 0.5) * 24.0 * bird.sizeMultiplier,
                    y + (bird.game.nextParticleRandom() - 0.5) * 18.0 * bird.sizeMultiplier,
                    Math.cos(angle) * speed,
                    Math.sin(angle) * speed - 1.4,
                    color.deriveColor(0, 1, 1, 0.72)
            ));
        }
    }
}
