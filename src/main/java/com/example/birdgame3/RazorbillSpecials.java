package com.example.birdgame3;

import javafx.scene.paint.Color;

import java.util.Arrays;

final class RazorbillSpecials {
    static final String GUILLOTINE_WAKE_MOVE = "Razorbill Guillotine Wake";
    static final double SPECIAL_KNOCKBACK_MULTIPLIER = 1.25;

    private RazorbillSpecials() {
    }

    static void use(Bird bird, boolean ultimate) {
        if (ultimate) {
            guillotineWake(bird);
            return;
        }
        switch (bird.selectRazorbillSpecialVariant()) {
            case NEUTRAL -> neutral(bird, ultimate);
            case SIDE -> side(bird, ultimate);
            case UP -> up(bird, ultimate);
            case DOWN -> counter(bird, ultimate);
        }
    }

    static void neutral(Bird bird, boolean ultimate) {
        bird.razorbillStormTimer = Bird.RAZORBILL_STORM_RELEASE_FRAMES;
        bird.razorbillStormHoldFrames = 0;
        bird.razorbillStormUltimate = ultimate;
        bird.razorbillStormReleased = false;
        bird.razorbillStormReuseTimer = ultimate ? 44 : Bird.RAZORBILL_NEUTRAL_REUSE_FRAMES;
        bird.specialCooldown = 0;
        bird.specialMaxCooldown = 0;
        bird.attackAnimationTimer = Math.max(bird.attackAnimationTimer, 14);
        bird.vx *= 0.52;
        bird.vy = Math.min(bird.vy, ultimate ? -1.4 : -0.85);
        Arrays.fill(bird.razorbillStormHitCooldown, 0);
        bird.game.addToKillFeed(bird.shortName() + (ultimate ? " WHIPS UP AN ULT RAZOR STORM!" : " whips up a razor storm!"));
    }

    static void side(Bird bird, boolean ultimate) {
        int dir = bird.horizontalInputDirection();
        if (dir == 0) {
            dir = bird.facingDirection();
        }
        bird.facingRight = dir > 0;
        bird.razorbillSideReuseTimer = ultimate ? 18 : Bird.RAZORBILL_SIDE_REUSE_FRAMES;
        bird.specialCooldown = bird.razorbillSideReuseTimer;
        bird.specialMaxCooldown = bird.razorbillSideReuseTimer;
        bird.razorbillSideUltimate = ultimate;
        bird.bladeStormFrames = (ultimate ? Bird.RAZORBILL_DASH_FRAMES + 10 : Bird.RAZORBILL_DASH_FRAMES)
                + Bird.RAZORBILL_DASH_STARTUP_FRAMES;
        Arrays.fill(bird.razorbillDashHit, false);

        double dashSpeed = Math.max(14.0, Bird.RAZORBILL_DASH_SPEED * (ultimate ? 1.22 : 1.0) * bird.speedMultiplier);
        bird.razorbillDashVX = dir * dashSpeed;
        bird.razorbillDashVY = Math.min(bird.vy * 0.35, bird.isOnGround() ? -1.2 : 2.0);
        bird.vx *= 0.34;
        bird.vy *= 0.52;
        bird.attackAnimationTimer = Math.max(bird.attackAnimationTimer, Bird.RAZORBILL_DASH_STARTUP_FRAMES + 3);

        bird.game.shakeIntensity = Math.max(bird.game.shakeIntensity, ultimate ? 12 : 7);
        emitSlashTrail(bird, bird.bodyCenterX() - dir * 40.0 * bird.sizeMultiplier, bird.bodyCenterY(),
                bird.bodyCenterX() + dir * (ultimate ? 190.0 : 150.0) * bird.sizeMultiplier,
                bird.bodyCenterY() - 8.0 * bird.sizeMultiplier,
                ultimate ? 32 : 22,
                ultimate ? Color.GOLD.brighter() : Color.web("#80DEEA"));
    }

    static void up(Bird bird, boolean ultimate) {
        int dir = bird.horizontalInputDirection();
        if (dir == 0) {
            dir = bird.facingDirection();
        }
        bird.facingRight = dir > 0;
        bird.razorbillUpSpecialUsed = true;
        bird.razorbillShearDirection = dir;
        bird.razorbillShearTimer = ultimate ? Bird.RAZORBILL_SHEAR_FRAMES + 8 : Bird.RAZORBILL_SHEAR_FRAMES;
        bird.razorbillShearUltimate = ultimate;
        Arrays.fill(bird.razorbillShearHit, false);
        bird.attackAnimationTimer = Math.max(bird.attackAnimationTimer, 12);
        bird.canDoubleJump = true;

        double centerX = bird.bodyCenterX();
        double centerY = bird.bodyCenterY();
        double endX = centerX + dir * (ultimate ? 124.0 : 92.0) * bird.sizeMultiplier;
        double endY = centerY - (ultimate ? 188.0 : 145.0) * bird.sizeMultiplier;
        bird.vx = bird.vx * 0.28 + dir * (ultimate ? 10.5 : 8.6) * bird.speedMultiplier;
        bird.vy = -Math.max(15.0, (ultimate ? 20.6 : 17.2) * bird.speedMultiplier);
        emitSlashTrail(bird, centerX - dir * 8.0 * bird.sizeMultiplier,
                centerY + 20.0 * bird.sizeMultiplier,
                endX,
                endY,
                ultimate ? 30 : 20,
                ultimate ? Color.GOLD.brighter() : Color.web("#B2EBF2"));
    }

    static void counter(Bird bird, boolean ultimate) {
        bird.razorbillCounterTimer = ultimate ? Bird.RAZORBILL_COUNTER_WINDOW_FRAMES + 8 : Bird.RAZORBILL_COUNTER_WINDOW_FRAMES;
        bird.razorbillCounterReuseTimer = ultimate ? 42 : Bird.RAZORBILL_COUNTER_REUSE_FRAMES;
        bird.razorbillCounterWhiffTimer = 0;
        bird.razorbillCounterBurstTimer = 0;
        bird.razorbillCounterUltimate = ultimate;
        bird.razorbillCountered = false;
        bird.razorbillCounterAttemptActive = true;
        bird.specialCooldown = 0;
        bird.specialMaxCooldown = 0;
        bird.attackAnimationTimer = Math.max(bird.attackAnimationTimer, bird.razorbillCounterTimer);
        bird.isBlocking = false;
        bird.parryWindowFrames = 0;
        bird.vx *= 0.35;
        bird.vy *= 0.55;
        emitSlashBurst(bird, bird.bodyCenterX(), bird.bodyCenterY(), bird.facingDirection(),
                ultimate ? Color.GOLD : Color.web("#ECEFF1"), ultimate ? 22 : 14);
    }

    static boolean counterWindowActive(Bird bird) {
        return bird.type == BirdGame3.BirdType.RAZORBILL
                && bird.health > 0
                && bird.razorbillCounterTimer > 0
                && bird.razorbillCounterAttemptActive
                && !bird.razorbillCountered;
    }

    static boolean tryCounter(Bird bird, Bird attacker, double scaledDamage) {
        if (!counterWindowActive(bird) || attacker == null || attacker == bird || attacker.health <= 0) {
            return false;
        }
        boolean ultimate = bird.razorbillCounterUltimate;
        bird.razorbillCountered = true;
        bird.razorbillCounterAttemptActive = false;
        bird.razorbillCounterTimer = 0;
        bird.razorbillCounterWhiffTimer = 0;
        bird.razorbillCounterBurstTimer = ultimate ? Bird.RAZORBILL_COUNTER_BURST_FRAMES + 5 : Bird.RAZORBILL_COUNTER_BURST_FRAMES;
        bird.attackAnimationTimer = Math.max(bird.attackAnimationTimer, bird.razorbillCounterBurstTimer);
        bird.stunTime = 0.0;
        bird.knockdownTimer = 0;
        bird.isBlocking = false;
        bird.parryWindowFrames = 0;
        bird.vx *= 0.18;
        bird.vy = Math.min(bird.vy * 0.25, -3.5);

        double dir = Math.signum(attacker.bodyCenterX() - bird.bodyCenterX());
        if (dir == 0.0) {
            dir = bird.facingDirection();
        }
        int rawDamage = (ultimate ? 14 : 10)
                + (int) Math.round(Math.clamp(scaledDamage / 24.0, 0.0, 1.0) * (ultimate ? 7.0 : 4.0));
        double oldHealth = attacker.health;
        double dealt = bird.applyUnshieldedDamageTo(attacker, rawDamage);
        if (dealt > 0) {
            bird.game.damageDealt[bird.playerIndex] += (int) dealt;
            bird.game.recordSpecialImpact(bird.playerIndex, (int) dealt, true);
            if (!bird.game.usesSmashCombatRules() && attacker.health <= 0 && oldHealth > 0) {
                bird.game.eliminations[bird.playerIndex]++;
            }
        }
        attacker.vx += dir * (ultimate ? 13.0 : 9.2) * SPECIAL_KNOCKBACK_MULTIPLIER;
        attacker.vy -= (ultimate ? 10.2 : 7.0) * SPECIAL_KNOCKBACK_MULTIPLIER;
        attacker.applyStun(ultimate ? 42 : 28);
        emitSlashBurst(bird, attacker.bodyCenterX(), attacker.bodyCenterY(), dir,
                ultimate ? Color.GOLD.brighter() : Color.web("#ECEFF1"),
                ultimate ? 42 : 28);
        bird.game.shakeIntensity = Math.max(bird.game.shakeIntensity, ultimate ? 18 : 12);
        bird.game.hitstopFrames = Math.max(bird.game.hitstopFrames, ultimate ? 8 : 5);
        bird.game.addToKillFeed(bird.shortName() + (ultimate ? " ULT COUNTER CUT!" : " counter cut!"));
        return true;
    }

    static boolean owner(Bird bird) {
        return bird.type == BirdGame3.BirdType.RAZORBILL
                || bird.mockingbirdCopiedNeutralFrom(BirdGame3.BirdType.RAZORBILL)
                || (bird.type == BirdGame3.BirdType.MOCKINGBIRD && bird.mockingbirdCapturedType == BirdGame3.BirdType.RAZORBILL);
    }

    static int applySpecialHit(Bird bird, Bird other, int damage, double launchX, double launchY, boolean ultimate) {
        if (other == null || !bird.canDamageTarget(other)) {
            return 0;
        }
        double oldHealth = other.health;
        int dealt = (int) bird.applyDamageTo(other, damage);
        if (dealt <= 0) {
            return 0;
        }

        bird.game.damageDealt[bird.playerIndex] += dealt;
        bird.game.recordSpecialImpact(bird.playerIndex, dealt, true);
        other.vx += launchX * SPECIAL_KNOCKBACK_MULTIPLIER;
        other.vy += launchY * SPECIAL_KNOCKBACK_MULTIPLIER;

        if (other.health <= 0 && oldHealth > 0) {
            bird.game.eliminations[bird.playerIndex]++;
        }
        emitSlashBurst(bird, other.bodyCenterX(), other.bodyCenterY(), Math.signum(launchX),
                ultimate ? Color.GOLD : Color.web("#80DEEA"), ultimate ? 18 : 12);
        return dealt;
    }

    static void emitSlashTrail(Bird bird, double x1, double y1, double x2, double y2, int count, Color color) {
        int particles = bird.scaledParticleCount(count);
        for (int i = 0; i < particles; i++) {
            double t = bird.game.nextParticleRandom();
            double px = x1 + (x2 - x1) * t;
            double py = y1 + (y2 - y1) * t;
            double angle = Math.atan2(y2 - y1, x2 - x1) + Math.PI * 0.5 + (bird.game.nextParticleRandom() - 0.5) * 0.75;
            double speed = 2.5 + bird.game.nextParticleRandom() * 6.5;
            bird.game.particles.add(new Particle(
                    px,
                    py,
                    Math.cos(angle) * speed,
                    Math.sin(angle) * speed - 1.6,
                    color.deriveColor(0, 1, 1, 0.78)
            ));
        }
    }

    static void emitSlashBurst(Bird bird, double cx, double cy, double dir, Color color, int count) {
        double baseAngle = dir == 0.0 ? 0.0 : (dir > 0.0 ? 0.0 : Math.PI);
        int particles = bird.scaledParticleCount(count);
        for (int i = 0; i < particles; i++) {
            double angle = baseAngle + (bird.game.nextParticleRandom() - 0.5) * 1.7;
            double speed = 4.5 + bird.game.nextParticleRandom() * 9.5;
            bird.game.particles.add(new Particle(
                    cx + (bird.game.nextParticleRandom() - 0.5) * 18.0,
                    cy + (bird.game.nextParticleRandom() - 0.5) * 18.0,
                    Math.cos(angle) * speed,
                    Math.sin(angle) * speed - 2.5,
                    color.deriveColor(0, 1, 1, 0.82)
            ));
        }
    }

    static boolean active(Bird bird) {
        return bird.razorbillStormTimer > 0
                || bird.bladeStormFrames > 0
                || bird.razorbillShearTimer > 0
                || bird.razorbillCounterTimer > 0
                || bird.razorbillCounterBurstTimer > 0
                || bird.razorbillCounterWhiffTimer > 0
                || bird.razorbillGuillotineTimer > 0;
    }

    static boolean ready(Bird bird, Bird.RazorbillSpecialVariant variant) {
        return switch (variant) {
            case NEUTRAL -> bird.isUltimateReady() || bird.razorbillStormReuseTimer <= 0;
            case SIDE -> bird.razorbillSideReuseTimer <= 0 && bird.bladeStormFrames <= 0;
            case UP -> !bird.razorbillUpSpecialUsed && bird.razorbillShearTimer <= 0;
            case DOWN -> bird.razorbillCounterReuseTimer <= 0;
        };
    }

    static boolean canConvertShieldIntoDown(Bird bird) {
        return bird.selectRazorbillSpecialVariant() == Bird.RazorbillSpecialVariant.DOWN
                && bird.isBlocking
                && bird.shieldStunFrames <= 0;
    }

    static boolean canStart(Bird bird, boolean grabbed, boolean dodging) {
        Bird.RazorbillSpecialVariant variant = bird.selectRazorbillSpecialVariant();
        boolean shieldConversion = canConvertShieldIntoDown(bird);
        return bird.type == BirdGame3.BirdType.RAZORBILL
                && bird.health > 0
                && bird.stunTime <= 0.0
                && !grabbed
                && (!bird.isBlocking || shieldConversion)
                && !dodging
                && !active(bird)
                && ready(bird, variant);
    }

    static void reset(Bird bird) {
        bird.bladeStormFrames = 0;
        bird.razorbillDashVX = 0.0;
        bird.razorbillDashVY = 0.0;
        bird.razorbillSideUltimate = false;
        Arrays.fill(bird.razorbillDashHit, false);
        bird.razorbillStormTimer = 0;
        bird.razorbillStormHoldFrames = 0;
        bird.razorbillStormUltimate = false;
        bird.razorbillStormReleased = false;
        Arrays.fill(bird.razorbillStormHitCooldown, 0);
        bird.razorbillShearTimer = 0;
        bird.razorbillShearUltimate = false;
        Arrays.fill(bird.razorbillShearHit, false);
        bird.razorbillCounterTimer = 0;
        bird.razorbillCounterWhiffTimer = 0;
        bird.razorbillCounterBurstTimer = 0;
        bird.razorbillCounterUltimate = false;
        bird.razorbillCountered = false;
        bird.razorbillCounterAttemptActive = false;
        bird.razorbillGuillotineTimer = 0;
        bird.razorbillGuillotineSlashIndex = 0;
        bird.razorbillGuillotineWakeTimer = 0;
        bird.razorbillGuillotineFinalResolved = false;
        bird.razorbillGuillotineAnchorX = 0.0;
        bird.razorbillGuillotineAnchorY = 0.0;
        bird.razorbillGuillotineTargetX = 0.0;
        bird.razorbillGuillotineTargetY = 0.0;
        bird.razorbillGuillotineLastStartX = 0.0;
        bird.razorbillGuillotineLastStartY = 0.0;
        bird.razorbillGuillotineLastEndX = 0.0;
        bird.razorbillGuillotineLastEndY = 0.0;
        bird.razorbillGuillotineWakeX1 = 0.0;
        bird.razorbillGuillotineWakeX2 = 0.0;
        bird.razorbillGuillotineWakeY = 0.0;
        Arrays.fill(bird.razorbillGuillotineHitCooldown, 0);
        Arrays.fill(bird.razorbillGuillotineWakeHitCooldown, 0);
    }

    static void handleState(Bird bird) {
        if (!owner(bird)) {
            return;
        }
        handleGuillotineWake(bird);
        if (bird.razorbillGuillotineTimer > 0) {
            return;
        }
        handleRisingStorm(bird);
        handleBladeStorm(bird);
        handleCliffShear(bird);
    }

    private static void guillotineWake(Bird bird) {
        reset(bird);
        Bird target = nearestTarget(bird);
        double targetX = target == null
                ? bird.bodyCenterX() + bird.facingDirection() * 250.0 * bird.sizeMultiplier
                : target.bodyCenterX();
        double targetY = target == null
                ? bird.bodyCenterY()
                : target.bodyCenterY();

        bird.razorbillGuillotineTimer = Bird.RAZORBILL_GUILLOTINE_TOTAL_FRAMES;
        bird.razorbillGuillotineSlashIndex = 0;
        bird.razorbillGuillotineWakeTimer = 0;
        bird.razorbillGuillotineFinalResolved = false;
        bird.razorbillGuillotineAnchorX = bird.bodyCenterX();
        bird.razorbillGuillotineAnchorY = bird.bodyCenterY();
        bird.razorbillGuillotineTargetX = clamp(targetX, 64.0, BirdGame3.WORLD_WIDTH - 64.0);
        bird.razorbillGuillotineTargetY = clamp(targetY, 96.0, BirdGame3.GROUND_Y - 42.0);
        setGuillotineChainLine(bird, 0);
        bird.specialCooldown = 0;
        bird.specialMaxCooldown = 0;
        bird.attackAnimationTimer = Math.max(bird.attackAnimationTimer, Bird.RAZORBILL_GUILLOTINE_WINDUP_FRAMES);
        bird.vx *= 0.08;
        bird.vy = Math.min(bird.vy * 0.15, -2.8);
        Arrays.fill(bird.razorbillGuillotineHitCooldown, 0);
        Arrays.fill(bird.razorbillGuillotineWakeHitCooldown, 0);

        int particles = bird.scaledParticleCount(48);
        for (int i = 0; i < particles; i++) {
            double angle = bird.game.nextParticleRandom() * Math.PI * 2.0;
            double radius = 18.0 + bird.game.nextParticleRandom() * 58.0;
            double speed = 1.8 + bird.game.nextParticleRandom() * 5.4;
            bird.game.particles.add(new Particle(
                    bird.bodyCenterX() + Math.cos(angle) * radius,
                    bird.bodyCenterY() + Math.sin(angle) * radius * 0.45,
                    Math.cos(angle + Math.PI * 0.5) * speed,
                    Math.sin(angle + Math.PI * 0.5) * speed - 1.6,
                    Color.web("#8CEBFF", 0.82)
            ));
        }
        bird.game.addToKillFeed(bird.shortName() + " OPENS THE GUILLOTINE WAKE!");
    }

    private static void handleGuillotineWake(Bird bird) {
        if (bird.razorbillGuillotineWakeTimer > 0) {
            applyGuillotineWakeDamage(bird);
        }
        if (bird.razorbillGuillotineTimer <= 0) {
            return;
        }

        int elapsed = Bird.RAZORBILL_GUILLOTINE_TOTAL_FRAMES - bird.razorbillGuillotineTimer;
        bird.attackAnimationTimer = Math.max(bird.attackAnimationTimer, 4);
        bird.isBlocking = false;
        bird.parryWindowFrames = 0;

        if (elapsed < Bird.RAZORBILL_GUILLOTINE_WINDUP_FRAMES) {
            double hover = Math.sin(elapsed * 0.34) * 5.5 * bird.sizeMultiplier;
            moveToCenter(bird, bird.razorbillGuillotineAnchorX,
                    bird.razorbillGuillotineAnchorY - 18.0 * bird.sizeMultiplier + hover);
            bird.vx = 0.0;
            bird.vy = 0.0;
            if (elapsed % 4 == 0) {
                emitGuillotineRiftParticles(bird, 7, Color.web("#31D9FF", 0.88));
            }
            return;
        }

        while (bird.razorbillGuillotineSlashIndex < Bird.RAZORBILL_GUILLOTINE_SLASH_COUNT
                && elapsed >= Bird.RAZORBILL_GUILLOTINE_WINDUP_FRAMES
                + bird.razorbillGuillotineSlashIndex * Bird.RAZORBILL_GUILLOTINE_SLASH_SPACING) {
            performGuillotineSlash(bird, bird.razorbillGuillotineSlashIndex);
            bird.razorbillGuillotineSlashIndex++;
        }

        if (!bird.razorbillGuillotineFinalResolved && elapsed >= Bird.RAZORBILL_GUILLOTINE_FINAL_FRAME) {
            performGuillotineFinal(bird);
        }

        double travelT = Math.clamp((elapsed - Bird.RAZORBILL_GUILLOTINE_WINDUP_FRAMES)
                / (double) Math.max(1, Bird.RAZORBILL_GUILLOTINE_FINAL_FRAME - Bird.RAZORBILL_GUILLOTINE_WINDUP_FRAMES),
                0.0, 1.0);
        double orbit = 96.0 * bird.sizeMultiplier;
        double angle = travelT * Math.PI * 2.0 * 1.65;
        moveToCenter(bird,
                bird.razorbillGuillotineTargetX + Math.cos(angle) * orbit,
                bird.razorbillGuillotineTargetY + Math.sin(angle) * orbit * 0.48 - 18.0 * bird.sizeMultiplier);
        bird.vx = 0.0;
        bird.vy = 0.0;

        if (elapsed % 5 == 0) {
            emitGuillotineRiftParticles(bird, 5, Color.web("#E8FBFF", 0.84));
        }
    }

    private static void performGuillotineSlash(Bird bird, int slashIndex) {
        setGuillotineChainLine(bird, slashIndex);
        double sx = bird.razorbillGuillotineLastStartX;
        double sy = bird.razorbillGuillotineLastStartY;
        double ex = bird.razorbillGuillotineLastEndX;
        double ey = bird.razorbillGuillotineLastEndY;
        double dx = ex - sx;
        double dy = ey - sy;
        double mag = Math.max(0.001, Math.hypot(dx, dy));
        double normalX = -dy / mag;
        double normalY = dx / mag;
        boolean hitAny = false;

        for (Bird other : bird.game.players) {
            if (!bird.canDamageTarget(other)) continue;
            int index = other.playerIndex;
            if (index < 0 || index >= bird.razorbillGuillotineHitCooldown.length) continue;
            if (bird.razorbillGuillotineHitCooldown[index] > 0) continue;

            double distance = pointToSegmentDistance(other.bodyCenterX(), other.bodyCenterY(), sx, sy, ex, ey);
            if (distance > 38.0 * bird.sizeMultiplier + other.combatRadius()) continue;

            double dir = Math.signum(other.bodyCenterX() - bird.razorbillGuillotineTargetX);
            if (dir == 0.0) dir = bird.facingDirection();
            int damage = Math.max(4, (int) Math.round((5.0 + slashIndex * 0.45) * bird.powerMultiplier));
            int dealt = applySpecialHit(bird, other, damage,
                    dir * 3.4 + normalX * 3.2,
                    -5.2 + normalY * 1.2,
                    true);
            if (dealt > 0) {
                bird.razorbillGuillotineHitCooldown[index] = 7;
                hitAny = true;
            }
        }

        emitSlashTrail(bird, sx, sy, ex, ey, 16, Color.web("#8CEBFF"));
        emitSlashBurst(bird, ex, ey, Math.signum(ex - sx), Color.web("#E8FBFF"), 14);
        moveToCenter(bird, ex, ey);
        bird.game.shakeIntensity = Math.max(bird.game.shakeIntensity, hitAny ? 11 : 6);
        bird.game.hitstopFrames = Math.max(bird.game.hitstopFrames, hitAny ? 3 : 1);
    }

    private static void performGuillotineFinal(Bird bird) {
        bird.razorbillGuillotineFinalResolved = true;
        double surfaceY = findWakeSurfaceY(bird, bird.razorbillGuillotineTargetX, bird.razorbillGuillotineTargetY);
        double x = bird.razorbillGuillotineTargetX;
        double startY = Math.max(72.0, bird.razorbillGuillotineTargetY - 265.0 * bird.sizeMultiplier);
        double endY = surfaceY + 28.0 * bird.sizeMultiplier;
        bird.razorbillGuillotineLastStartX = x;
        bird.razorbillGuillotineLastStartY = startY;
        bird.razorbillGuillotineLastEndX = x;
        bird.razorbillGuillotineLastEndY = endY;
        bird.razorbillGuillotineWakeX1 = clamp(x - 190.0 * bird.sizeMultiplier, 16.0, BirdGame3.WORLD_WIDTH - 16.0);
        bird.razorbillGuillotineWakeX2 = clamp(x + 190.0 * bird.sizeMultiplier, 16.0, BirdGame3.WORLD_WIDTH - 16.0);
        bird.razorbillGuillotineWakeY = surfaceY;
        bird.razorbillGuillotineWakeTimer = Bird.RAZORBILL_GUILLOTINE_WAKE_FRAMES;
        Arrays.fill(bird.razorbillGuillotineWakeHitCooldown, 0);

        boolean hitAny = false;
        for (Bird other : bird.game.players) {
            if (!bird.canDamageTarget(other)) continue;
            double distance = pointToSegmentDistance(other.bodyCenterX(), other.bodyCenterY(),
                    bird.razorbillGuillotineLastStartX,
                    bird.razorbillGuillotineLastStartY,
                    bird.razorbillGuillotineLastEndX,
                    bird.razorbillGuillotineLastEndY);
            if (distance > 58.0 * bird.sizeMultiplier + other.combatRadius()) continue;
            double dir = Math.signum(other.bodyCenterX() - x);
            if (dir == 0.0) dir = bird.facingDirection();
            int dealt = applySpecialHit(bird, other,
                    Math.max(12, (int) Math.round(14.0 * bird.powerMultiplier)),
                    dir * 6.5,
                    -18.0,
                    true);
            if (dealt > 0) {
                hitAny = true;
                other.applyStun(18);
            }
        }

        emitSlashTrail(bird, x, startY, x, endY, 36, Color.web("#E8FBFF"));
        emitSlashBurst(bird, x, surfaceY - 42.0 * bird.sizeMultiplier, bird.facingDirection(),
                Color.web("#31D9FF"), 42);
        moveToCenter(bird, x + bird.facingDirection() * 72.0 * bird.sizeMultiplier,
                Math.max(96.0, surfaceY - 108.0 * bird.sizeMultiplier));
        bird.game.shakeIntensity = Math.max(bird.game.shakeIntensity, hitAny ? 22 : 16);
        bird.game.hitstopFrames = Math.max(bird.game.hitstopFrames, hitAny ? 7 : 4);
        bird.game.triggerFlash(0.46, false);
        bird.game.addToKillFeed(bird.shortName() + " DROPS THE GUILLOTINE!");
    }

    private static void applyGuillotineWakeDamage(Bird bird) {
        double left = Math.min(bird.razorbillGuillotineWakeX1, bird.razorbillGuillotineWakeX2);
        double right = Math.max(bird.razorbillGuillotineWakeX1, bird.razorbillGuillotineWakeX2);
        double wakeY = bird.razorbillGuillotineWakeY;
        for (Bird other : bird.game.players) {
            if (!bird.canDamageTarget(other)) continue;
            int index = other.playerIndex;
            if (index < 0 || index >= bird.razorbillGuillotineWakeHitCooldown.length) continue;
            if (bird.razorbillGuillotineWakeHitCooldown[index] > 0) continue;
            double cx = other.bodyCenterX();
            if (cx < left - other.combatHalfWidth() || cx > right + other.combatHalfWidth()) continue;
            double bottomDistance = Math.abs(other.bodyBottomY() - wakeY);
            double centerDistance = Math.abs(other.bodyCenterY() - wakeY);
            if (bottomDistance > 44.0 * bird.sizeMultiplier && centerDistance > 82.0 * bird.sizeMultiplier) continue;

            double dir = Math.signum(cx - (left + right) * 0.5);
            if (dir == 0.0) dir = bird.facingDirection();
            int dealt = applySpecialHit(bird, other, Math.max(2, (int) Math.round(3.0 * bird.powerMultiplier)),
                    dir * 2.2,
                    -2.8,
                    true);
            if (dealt > 0) {
                bird.razorbillGuillotineWakeHitCooldown[index] = 24;
            }
        }
        if (bird.razorbillGuillotineWakeTimer % 9 == 0) {
            emitSlashTrail(bird,
                    left + bird.game.nextParticleRandom() * Math.max(1.0, right - left),
                    wakeY + 2.0 * bird.sizeMultiplier,
                    left + bird.game.nextParticleRandom() * Math.max(1.0, right - left),
                    wakeY - (18.0 + bird.game.nextParticleRandom() * 34.0) * bird.sizeMultiplier,
                    4,
                    Color.web("#31D9FF"));
        }
    }

    private static Bird nearestTarget(Bird bird) {
        Bird best = null;
        double bestDistance = Double.POSITIVE_INFINITY;
        for (Bird other : bird.game.players) {
            if (!bird.canDamageTarget(other)) continue;
            double distance = Math.hypot(other.bodyCenterX() - bird.bodyCenterX(),
                    other.bodyCenterY() - bird.bodyCenterY());
            if (distance < bestDistance) {
                bestDistance = distance;
                best = other;
            }
        }
        return best;
    }

    private static void setGuillotineChainLine(Bird bird, int slashIndex) {
        double s = bird.sizeMultiplier;
        double targetX = bird.razorbillGuillotineTargetX;
        double targetY = bird.razorbillGuillotineTargetY;
        double side = ((slashIndex & 1) == 0 ? 1.0 : -1.0) * bird.facingDirection();
        double reach = (230.0 + slashIndex * 18.0) * s;
        double high = (142.0 + (slashIndex % 3) * 24.0) * s;
        double low = (72.0 + slashIndex * 8.0) * s;

        if (slashIndex % 3 == 0) {
            bird.razorbillGuillotineLastStartX = clamp(targetX - side * reach, 24.0, BirdGame3.WORLD_WIDTH - 24.0);
            bird.razorbillGuillotineLastStartY = clamp(targetY - high, 36.0, BirdGame3.GROUND_Y + 80.0);
            bird.razorbillGuillotineLastEndX = clamp(targetX + side * reach, 24.0, BirdGame3.WORLD_WIDTH - 24.0);
            bird.razorbillGuillotineLastEndY = clamp(targetY + low, 36.0, BirdGame3.GROUND_Y + 80.0);
        } else if (slashIndex % 3 == 1) {
            bird.razorbillGuillotineLastStartX = clamp(targetX + side * reach, 24.0, BirdGame3.WORLD_WIDTH - 24.0);
            bird.razorbillGuillotineLastStartY = clamp(targetY - high * 0.42, 36.0, BirdGame3.GROUND_Y + 80.0);
            bird.razorbillGuillotineLastEndX = clamp(targetX - side * reach, 24.0, BirdGame3.WORLD_WIDTH - 24.0);
            bird.razorbillGuillotineLastEndY = clamp(targetY + low * 0.35, 36.0, BirdGame3.GROUND_Y + 80.0);
        } else {
            bird.razorbillGuillotineLastStartX = clamp(targetX - side * reach * 0.62, 24.0, BirdGame3.WORLD_WIDTH - 24.0);
            bird.razorbillGuillotineLastStartY = clamp(targetY + low * 0.58, 36.0, BirdGame3.GROUND_Y + 80.0);
            bird.razorbillGuillotineLastEndX = clamp(targetX + side * reach * 0.62, 24.0, BirdGame3.WORLD_WIDTH - 24.0);
            bird.razorbillGuillotineLastEndY = clamp(targetY - high * 0.72, 36.0, BirdGame3.GROUND_Y + 80.0);
        }
    }

    private static void emitGuillotineRiftParticles(Bird bird, int count, Color color) {
        int particles = bird.scaledParticleCount(count);
        for (int i = 0; i < particles; i++) {
            double angle = bird.game.nextParticleRandom() * Math.PI * 2.0;
            double radius = 30.0 + bird.game.nextParticleRandom() * 92.0;
            bird.game.particles.add(new Particle(
                    bird.razorbillGuillotineTargetX + Math.cos(angle) * radius,
                    bird.razorbillGuillotineTargetY + Math.sin(angle) * radius * 0.45,
                    -Math.cos(angle) * (1.8 + bird.game.nextParticleRandom() * 3.2),
                    -Math.sin(angle) * (1.2 + bird.game.nextParticleRandom() * 2.4) - 1.0,
                    color
            ));
        }
    }

    private static double findWakeSurfaceY(Bird bird, double x, double referenceY) {
        double best = BirdGame3.GROUND_Y;
        for (Platform platform : bird.game.platforms) {
            if (platform == null || platform.w <= 0.0 || platform.h <= 0.0) continue;
            boolean caveCeiling = platform.y <= 1.0 && platform.h >= 60.0 && platform.w >= BirdGame3.WORLD_WIDTH - 10.0;
            if (caveCeiling) continue;
            if (x < platform.x - 26.0 || x > platform.x + platform.w + 26.0) continue;
            if (platform.y < referenceY - 70.0) continue;
            if (platform.y < best) {
                best = platform.y;
            }
        }
        return best;
    }

    private static double pointToSegmentDistance(double px, double py, double x1, double y1, double x2, double y2) {
        double dx = x2 - x1;
        double dy = y2 - y1;
        double lenSq = dx * dx + dy * dy;
        if (lenSq <= 0.0001) {
            return Math.hypot(px - x1, py - y1);
        }
        double t = ((px - x1) * dx + (py - y1) * dy) / lenSq;
        if (t < 0.0) t = 0.0;
        if (t > 1.0) t = 1.0;
        double closestX = x1 + dx * t;
        double closestY = y1 + dy * t;
        return Math.hypot(px - closestX, py - closestY);
    }

    private static void moveToCenter(Bird bird, double centerX, double centerY) {
        double halfW = bird.bodyWidth() * 0.5;
        double halfH = bird.bodyHeight() * 0.5;
        double clampedX = clamp(centerX, halfW + 12.0, BirdGame3.WORLD_WIDTH - halfW - 12.0);
        double clampedY = clamp(centerY, 44.0 + halfH, BirdGame3.GROUND_Y + 70.0);
        bird.x = clampedX - halfW;
        bird.y = clampedY - halfH;
    }

    private static double clamp(double value, double min, double max) {
        if (max < min) {
            return min;
        }
        return Math.max(min, Math.min(max, value));
    }

    static void handleRisingStorm(Bird bird) {
        if (bird.razorbillStormTimer <= 0) {
            return;
        }

        int maxHoldFrames = bird.razorbillStormUltimate
                ? Bird.RAZORBILL_STORM_MAX_HOLD_FRAMES + 24
                : Bird.RAZORBILL_STORM_MAX_HOLD_FRAMES;
        boolean canHold = bird.specialHeld()
                && !bird.razorbillStormReleased
                && bird.razorbillStormHoldFrames < maxHoldFrames;
        if (canHold) {
            bird.razorbillStormTimer = Math.max(bird.razorbillStormTimer, Bird.RAZORBILL_STORM_RELEASE_FRAMES);
            bird.razorbillStormHoldFrames++;
        } else {
            releaseStorm(bird);
        }

        if (!bird.razorbillStormReleased) {
            int inputDir = bird.horizontalInputDirection();
            if (inputDir != 0) {
                bird.facingRight = inputDir > 0;
                bird.vx = Math.clamp(bird.vx * 0.80 + inputDir * (bird.razorbillStormUltimate ? 0.72 : 0.52), -4.8, 4.8);
            } else {
                bird.vx *= 0.86;
            }

            double holdRatio = Math.clamp(bird.razorbillStormHoldFrames / (double) Bird.RAZORBILL_STORM_MAX_HOLD_FRAMES, 0.0, 1.0);
            bird.vy = Math.min(bird.vy, -(bird.razorbillStormUltimate ? 1.75 : 1.25) - holdRatio * (bird.razorbillStormUltimate ? 1.1 : 0.75));
            bird.y = Math.max(96.0, bird.y - (bird.razorbillStormUltimate ? 0.22 : 0.14));
            bird.attackAnimationTimer = Math.max(bird.attackAnimationTimer, 4);

            if ((bird.razorbillStormHoldFrames + bird.razorbillStormTimer) % 3 == 0) {
                Color slash = bird.razorbillStormUltimate ? Color.GOLD.brighter() : Color.web("#CFD8DC");
                double angle = (bird.razorbillStormHoldFrames * 0.56) % (Math.PI * 2.0);
                for (int i = 0; i < (bird.razorbillStormUltimate ? 4 : 3); i++) {
                    double a = angle + i * Math.PI * 2.0 / (bird.razorbillStormUltimate ? 4 : 3);
                    double orbit = (34.0 + bird.game.nextParticleRandom() * 42.0) * bird.sizeMultiplier;
                    bird.game.particles.add(new Particle(
                            bird.bodyCenterX() + Math.cos(a) * orbit,
                            bird.bodyCenterY() + Math.sin(a) * orbit * 0.72,
                            -Math.sin(a) * (3.0 + bird.game.nextParticleRandom() * 3.2),
                            Math.cos(a) * (2.2 + bird.game.nextParticleRandom() * 2.6) - 2.0,
                            slash.deriveColor(0, 1, 1, 0.82)
                    ));
                }
            }
            return;
        }

        bird.vx *= 0.91;
        bird.attackAnimationTimer = Math.max(bird.attackAnimationTimer, 3);
        if (bird.razorbillStormTimer % 2 == 0) {
            Color slash = bird.razorbillStormUltimate ? Color.GOLD.brighter() : Color.web("#ECEFF1");
            double angle = bird.game.nextParticleRandom() * Math.PI * 2.0;
            double distance = (46.0 + bird.game.nextParticleRandom() * 46.0) * bird.sizeMultiplier;
            bird.game.particles.add(new Particle(
                    bird.bodyCenterX() + Math.cos(angle) * distance,
                    bird.bodyCenterY() + Math.sin(angle) * distance * 0.72,
                    Math.cos(angle) * (2.6 + bird.game.nextParticleRandom() * 3.8),
                    Math.sin(angle) * (1.9 + bird.game.nextParticleRandom() * 2.8) - 1.0,
                    slash.deriveColor(0, 1, 1, 0.78)
            ));
        }
    }

    static void releaseStorm(Bird bird) {
        if (bird.razorbillStormReleased || bird.razorbillStormTimer <= 0) {
            return;
        }

        bird.razorbillStormReleased = true;
        bird.razorbillStormTimer = Math.max(bird.razorbillStormTimer, Bird.RAZORBILL_STORM_RELEASE_FRAMES);
        bird.attackAnimationTimer = Math.max(bird.attackAnimationTimer, Bird.RAZORBILL_STORM_RELEASE_FRAMES + 3);
        bird.vx *= 0.72;
        bird.vy = Math.min(bird.vy, bird.razorbillStormUltimate ? -2.8 : -2.0);
        bird.game.playJalapenoSfx();

        double centerX = bird.bodyCenterX();
        double centerY = bird.bodyCenterY();
        double holdRatio = Math.clamp(bird.razorbillStormHoldFrames / (double) Bird.RAZORBILL_STORM_MAX_HOLD_FRAMES, 0.0, 1.0);
        double radius = ((bird.razorbillStormUltimate ? 118.0 : 94.0)
                + holdRatio * (bird.razorbillStormUltimate ? 34.0 : 24.0)) * bird.sizeMultiplier;
        double verticalRadius = ((bird.razorbillStormUltimate ? 100.0 : 82.0)
                + holdRatio * (bird.razorbillStormUltimate ? 26.0 : 18.0)) * bird.sizeMultiplier;
        boolean hitAny = false;
        for (Bird other : bird.game.players) {
            if (!bird.canDamageTarget(other)) continue;
            double dx = other.bodyCenterX() - centerX;
            double dy = other.bodyCenterY() - centerY;
            if (Math.abs(dx) > radius + other.combatHalfWidth()
                    || Math.abs(dy) > verticalRadius + other.combatHalfHeight()) {
                continue;
            }
            double dir = Math.signum(dx);
            if (dir == 0.0) {
                dir = bird.facingDirection();
            }
            double stormDamage = (bird.razorbillStormUltimate ? 7.0 : 4.0)
                    + holdRatio * (bird.razorbillStormUltimate ? 18.0 : 13.0);
            int dmg = Math.max(3, (int) Math.round(stormDamage * bird.powerMultiplier));
            int dealt = applySpecialHit(bird, other, dmg,
                    dir * ((bird.razorbillStormUltimate ? 8.6 : 6.6) + holdRatio * (bird.razorbillStormUltimate ? 2.6 : 1.8)),
                    -((bird.razorbillStormUltimate ? 10.5 : 8.0) + holdRatio * (bird.razorbillStormUltimate ? 3.0 : 2.0)),
                    bird.razorbillStormUltimate);
            if (dealt > 0) {
                hitAny = true;
            }
        }

        Color slash = bird.razorbillStormUltimate ? Color.GOLD.brighter() : Color.web("#ECEFF1");
        int bladeCount = bird.razorbillStormUltimate ? 10 : 8;
        double spin = bird.razorbillStormHoldFrames * 0.13;
        for (int i = 0; i < bladeCount; i++) {
            double angle = spin + i * Math.PI * 2.0 / bladeCount;
            double inner = (20.0 + bird.game.nextParticleRandom() * 10.0) * bird.sizeMultiplier;
            double outer = radius * (0.72 + bird.game.nextParticleRandom() * 0.22);
            emitSlashTrail(
                    bird,
                    centerX + Math.cos(angle) * inner,
                    centerY + Math.sin(angle) * inner * 0.74,
                    centerX + Math.cos(angle) * outer,
                    centerY + Math.sin(angle) * outer * 0.74,
                    bird.razorbillStormUltimate ? 5 : 3,
                    slash
            );
        }
        emitSlashBurst(bird, centerX, centerY, bird.facingDirection(), slash, bird.razorbillStormUltimate ? 24 : 16);
        bird.game.shakeIntensity = Math.max(bird.game.shakeIntensity, hitAny ? (bird.razorbillStormUltimate ? 16 : 11) : 6);
        bird.game.hitstopFrames = Math.max(bird.game.hitstopFrames, hitAny ? (bird.razorbillStormUltimate ? 7 : 5) : 2);
    }

    static void handleBladeStorm(Bird bird) {
        if (bird.bladeStormFrames <= 0) return;

        int activeFrames = bird.razorbillSideUltimate
                ? Bird.RAZORBILL_DASH_FRAMES + 10
                : Bird.RAZORBILL_DASH_FRAMES;
        if (bird.bladeStormFrames > activeFrames) {
            bird.vx *= 0.62;
            bird.vy *= 0.78;
            bird.attackAnimationTimer = Math.max(bird.attackAnimationTimer, 3);
            return;
        }

        double dashX = bird.razorbillDashVX;
        double dashY = bird.razorbillDashVY;
        double dashMag = Math.hypot(dashX, dashY);
        if (dashMag < 0.1) {
            dashX = bird.vx;
            dashY = bird.vy;
            dashMag = Math.hypot(dashX, dashY);
            if (dashMag < 0.1) {
                dashX = bird.facingRight ? 1 : -1;
                dashY = 0;
                dashMag = 1.0;
            }
            double dashSpeed = Math.max(12.0, Bird.RAZORBILL_DASH_SPEED * bird.speedMultiplier);
            bird.razorbillDashVX = dashX / dashMag * dashSpeed;
            bird.razorbillDashVY = dashY / dashMag * dashSpeed;
            dashX = bird.razorbillDashVX;
            dashY = bird.razorbillDashVY;
            dashMag = Math.hypot(dashX, dashY);
        }

        bird.vx = dashX;
        bird.vy = dashY;

        double dirX = dashX / dashMag;
        double dirY = dashY / dashMag;

        for (Bird other : bird.game.players) {
            if (!bird.canDamageTarget(other)) continue;
            if (other.playerIndex < 0 || other.playerIndex >= bird.razorbillDashHit.length) continue;
            if (bird.razorbillDashHit[other.playerIndex]) continue;

            double dx = other.bodyCenterX() - bird.bodyCenterX();
            double dy = other.bodyCenterY() - bird.bodyCenterY();
            double dist = Math.hypot(dx, dy);
            if (dist > 85 + other.combatRadius()) continue;

            int dmg = Math.max(5, (int) Math.round((bird.razorbillSideUltimate ? 10 : 7) * bird.powerMultiplier));
            int dealt = applySpecialHit(bird, other, dmg,
                    dirX * (bird.razorbillSideUltimate ? 10.5 : 8.0),
                    dirY * 8.0 - (bird.razorbillSideUltimate ? 4.2 : 2.8),
                    bird.razorbillSideUltimate);
            if (dealt <= 0) continue;

            bird.razorbillDashHit[other.playerIndex] = true;
            bird.bladeStormFrames = Math.min(bird.bladeStormFrames, Bird.RAZORBILL_DASH_HIT_RECOVERY_FRAMES);
            bird.razorbillDashVX = dirX * Math.min(9.0, dashMag * 0.42);
            bird.razorbillDashVY = Math.min(-5.8, dashY * 0.25);
            bird.vx = bird.razorbillDashVX;
            bird.vy = bird.razorbillDashVY;

            bird.game.shakeIntensity = Math.max(bird.game.shakeIntensity, 14);
            bird.game.hitstopFrames = Math.max(bird.game.hitstopFrames, 6);
        }

        if (bird.bladeStormFrames % 3 == 0) {
            for (int i = 0; i < 6; i++) {
                double angle = Math.atan2(dirY, dirX) + Math.PI + (bird.game.nextParticleRandom() - 0.5) * 0.9;
                double speed = 4 + bird.game.nextParticleRandom() * 6;
                bird.game.particles.add(new Particle(
                        bird.x + 40 + (bird.game.nextParticleRandom() - 0.5) * 16,
                        bird.y + 40 + (bird.game.nextParticleRandom() - 0.5) * 16,
                        Math.cos(angle) * speed,
                        Math.sin(angle) * speed,
                        Color.WHITE.deriveColor(0, 1, 1, 0.9)
                ));
            }
        }
    }

    static void handleCliffShear(Bird bird) {
        if (bird.razorbillShearTimer <= 0) {
            return;
        }
        int dir = bird.razorbillShearDirection == 0 ? bird.facingDirection() : bird.razorbillShearDirection;
        double phase = Math.clamp(bird.razorbillShearTimer / (double) (bird.razorbillShearUltimate ? Bird.RAZORBILL_SHEAR_FRAMES + 8 : Bird.RAZORBILL_SHEAR_FRAMES), 0.0, 1.0);
        bird.vx = bird.vx * 0.86 + dir * (bird.razorbillShearUltimate ? 2.0 : 1.5);
        bird.vy = Math.min(bird.vy, -3.0 - phase * (bird.razorbillShearUltimate ? 7.8 : 5.8));

        for (Bird other : bird.game.players) {
            if (!bird.canDamageTarget(other)) continue;
            if (other.playerIndex < 0 || other.playerIndex >= bird.razorbillShearHit.length) continue;
            if (bird.razorbillShearHit[other.playerIndex]) continue;
            double dx = other.bodyCenterX() - bird.bodyCenterX();
            double dy = other.bodyCenterY() - bird.bodyCenterY();
            if (dir > 0 && dx < -other.combatHalfWidth() * 0.25) continue;
            if (dir < 0 && dx > other.combatHalfWidth() * 0.25) continue;
            if (Math.abs(dx) > (bird.razorbillShearUltimate ? 118.0 : 96.0) * bird.sizeMultiplier + other.combatHalfWidth()) continue;
            if (dy > 48.0 * bird.sizeMultiplier + other.combatHalfHeight()
                    || dy < -(bird.razorbillShearUltimate ? 150.0 : 118.0) * bird.sizeMultiplier - other.combatHalfHeight()) {
                continue;
            }
            int dealt = applySpecialHit(bird, other,
                    bird.razorbillShearUltimate ? 9 : 6,
                    dir * (bird.razorbillShearUltimate ? 7.5 : 5.6),
                    bird.razorbillShearUltimate ? -12.0 : -9.0,
                    bird.razorbillShearUltimate);
            if (dealt > 0) {
                bird.razorbillShearHit[other.playerIndex] = true;
            }
        }

        if (bird.razorbillShearTimer % 4 == 0) {
            double cx = bird.bodyCenterX();
            double cy = bird.bodyCenterY();
            emitSlashTrail(bird, cx - dir * 18.0 * bird.sizeMultiplier, cy + 20.0 * bird.sizeMultiplier,
                    cx + dir * 72.0 * bird.sizeMultiplier, cy - 92.0 * bird.sizeMultiplier,
                    bird.razorbillShearUltimate ? 9 : 6,
                    bird.razorbillShearUltimate ? Color.GOLD : Color.web("#B2EBF2"));
        }
    }
}
