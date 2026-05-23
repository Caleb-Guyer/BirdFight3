package com.example.birdgame3;

import javafx.scene.paint.Color;

import java.util.Arrays;

final class RazorbillSpecials {
    private RazorbillSpecials() {
    }

    static void use(Bird bird, boolean ultimate) {
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
        bird.bladeStormFrames = ultimate ? Bird.RAZORBILL_DASH_FRAMES + 10 : Bird.RAZORBILL_DASH_FRAMES;
        Arrays.fill(bird.razorbillDashHit, false);

        double dashSpeed = Math.max(14.0, Bird.RAZORBILL_DASH_SPEED * (ultimate ? 1.22 : 1.0) * bird.speedMultiplier);
        bird.razorbillDashVX = dir * dashSpeed;
        bird.razorbillDashVY = Math.min(bird.vy * 0.35, bird.isOnGround() ? -1.2 : 2.0);
        bird.vx = bird.razorbillDashVX;
        bird.vy = bird.razorbillDashVY;

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
        attacker.vx += dir * (ultimate ? 13.0 : 9.2);
        attacker.vy -= ultimate ? 10.2 : 7.0;
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
        other.vx += launchX;
        other.vy += launchY;

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
            double t = Math.random();
            double px = x1 + (x2 - x1) * t;
            double py = y1 + (y2 - y1) * t;
            double angle = Math.atan2(y2 - y1, x2 - x1) + Math.PI * 0.5 + (Math.random() - 0.5) * 0.75;
            double speed = 2.5 + Math.random() * 6.5;
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
            double angle = baseAngle + (Math.random() - 0.5) * 1.7;
            double speed = 4.5 + Math.random() * 9.5;
            bird.game.particles.add(new Particle(
                    cx + (Math.random() - 0.5) * 18.0,
                    cy + (Math.random() - 0.5) * 18.0,
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
                || bird.razorbillCounterWhiffTimer > 0;
    }

    static boolean ready(Bird bird, Bird.RazorbillSpecialVariant variant) {
        boolean ultimateReady = bird.isUltimateReady();
        return switch (variant) {
            case NEUTRAL -> ultimateReady || bird.razorbillStormReuseTimer <= 0;
            case SIDE -> ultimateReady || (bird.razorbillSideReuseTimer <= 0 && bird.bladeStormFrames <= 0);
            case UP -> ultimateReady || (!bird.razorbillUpSpecialUsed && bird.razorbillShearTimer <= 0);
            case DOWN -> ultimateReady || bird.razorbillCounterReuseTimer <= 0;
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
    }

    static void handleState(Bird bird) {
        if (!owner(bird)) {
            return;
        }
        handleRisingStorm(bird);
        handleBladeStorm(bird);
        handleCliffShear(bird);
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
                    double orbit = (34.0 + Math.random() * 42.0) * bird.sizeMultiplier;
                    bird.game.particles.add(new Particle(
                            bird.bodyCenterX() + Math.cos(a) * orbit,
                            bird.bodyCenterY() + Math.sin(a) * orbit * 0.72,
                            -Math.sin(a) * (3.0 + Math.random() * 3.2),
                            Math.cos(a) * (2.2 + Math.random() * 2.6) - 2.0,
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
            double angle = Math.random() * Math.PI * 2.0;
            double distance = (46.0 + Math.random() * 46.0) * bird.sizeMultiplier;
            bird.game.particles.add(new Particle(
                    bird.bodyCenterX() + Math.cos(angle) * distance,
                    bird.bodyCenterY() + Math.sin(angle) * distance * 0.72,
                    Math.cos(angle) * (2.6 + Math.random() * 3.8),
                    Math.sin(angle) * (1.9 + Math.random() * 2.8) - 1.0,
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
            double inner = (20.0 + Math.random() * 10.0) * bird.sizeMultiplier;
            double outer = radius * (0.72 + Math.random() * 0.22);
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
            bird.vy = Math.min(bird.vy, -5.8);
            bird.vx -= dirX * 2.2;

            bird.game.shakeIntensity = Math.max(bird.game.shakeIntensity, 14);
            bird.game.hitstopFrames = Math.max(bird.game.hitstopFrames, 6);
        }

        if (bird.bladeStormFrames % 3 == 0) {
            for (int i = 0; i < 6; i++) {
                double angle = Math.atan2(dirY, dirX) + Math.PI + (Math.random() - 0.5) * 0.9;
                double speed = 4 + Math.random() * 6;
                bird.game.particles.add(new Particle(
                        bird.x + 40 + (Math.random() - 0.5) * 16,
                        bird.y + 40 + (Math.random() - 0.5) * 16,
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
