package com.example.birdgame3;

import javafx.scene.paint.Color;

import java.util.Arrays;

final class ShoebillSpecials {
    private ShoebillSpecials() {
    }

    static void use(Bird bird, boolean ultimate) {
        switch (bird.selectShoebillSpecialVariant()) {
            case NEUTRAL -> neutral(bird, ultimate);
            case SIDE -> side(bird, ultimate);
            case UP -> up(bird, ultimate);
            case DOWN -> down(bird, ultimate);
        }
    }

    static void neutral(Bird bird, boolean ultimate) {
        int dir = bird.horizontalInputDirection();
        if (dir != 0) {
            bird.facingRight = dir > 0;
        }
        dir = bird.facingDirection();
        bird.shoebillStareFxTimer = ultimate ? Bird.SHOEBILL_STARE_FX_FRAMES + 8 : Bird.SHOEBILL_STARE_FX_FRAMES;
        bird.shoebillStareUltimate = ultimate;
        bird.shoebillStareReuseTimer = Bird.SHOEBILL_STARE_REUSE_FRAMES;
        bird.specialCooldown = 0;
        bird.specialMaxCooldown = 0;
        bird.attackAnimationTimer = Math.max(bird.attackAnimationTimer, bird.shoebillStareFxTimer + 4);
        bird.vx *= bird.isOnGround() ? 0.22 : 0.55;

        int stunnedTargets = 0;
        for (Bird other : bird.game.players) {
            if (!bird.canDamageTarget(other)) continue;
            if (!hasDirectStareLine(bird, other, ultimate)) continue;
            other.applyStun(ultimate ? 180 : 120);
            other.vx *= 0.28;
            other.vy *= 0.52;
            stunnedTargets++;
        }

        bird.game.recordSpecialImpact(bird.playerIndex, 0, stunnedTargets > 0);
        bird.game.addToKillFeed(bird.shortName() + (stunnedTargets > 0
                ? (ultimate ? " ULT DEATH STARE! Back-facing gaze dazed " : " DEATH STARE! Back-facing gaze dazed ") + stunnedTargets + "!"
                : " DEATH STARE missed the back-facing gaze!"));
        bird.game.shakeIntensity = Math.max(bird.game.shakeIntensity, stunnedTargets > 0 ? (ultimate ? 24 : 18) : 8);
        if (stunnedTargets > 0) {
            bird.game.hitstopFrames = Math.max(bird.game.hitstopFrames, ultimate ? 8 : 5);
        }

        Color stareColor = ultimate ? Color.GOLD : Color.web("#B39DDB");
        for (int i = 0; i < bird.scaledParticleCount(ultimate ? 44 : 28); i++) {
            double lane = (SimRng.next() - 0.5) * (ultimate ? 18.0 : 10.0) * bird.sizeMultiplier;
            double travel = 26.0 + SimRng.next() * (ultimate ? 165.0 : 108.0);
            bird.game.particles.add(new Particle(
                    bird.bodyCenterX() + dir * (18.0 + travel * 0.15) * bird.sizeMultiplier,
                    bird.bodyCenterY() - 18.0 * bird.sizeMultiplier + lane,
                    dir * (1.6 + SimRng.next() * 4.2),
                    (SimRng.next() - 0.5) * 1.8,
                    stareColor.deriveColor(0, 1, 1, 0.68 + SimRng.next() * 0.22)
            ));
        }
    }

    static void side(Bird bird, boolean ultimate) {
        int dir = bird.horizontalInputDirection();
        if (dir == 0) {
            dir = bird.facingDirection();
        }
        bird.facingRight = dir > 0;
        bird.shoebillThrustTimer = ultimate ? Bird.SHOEBILL_THRUST_FRAMES + 8 : Bird.SHOEBILL_THRUST_FRAMES;
        bird.shoebillThrustReuseTimer = ultimate ? 54 : Bird.SHOEBILL_THRUST_REUSE_FRAMES;
        bird.shoebillThrustDirection = dir;
        bird.shoebillThrustUltimate = ultimate;
        Arrays.fill(bird.shoebillThrustHit, false);
        bird.specialCooldown = 0;
        bird.specialMaxCooldown = 0;
        bird.attackAnimationTimer = Math.max(bird.attackAnimationTimer, bird.shoebillThrustTimer);
        bird.vx *= bird.isOnGround() ? 0.32 : 0.58;
        bird.vy = Math.min(bird.vy, bird.isOnGround() ? 0.0 : 2.4);
        bird.isBlocking = false;
        bird.parryWindowFrames = 0;
        bird.shieldStunFrames = 0;
        bird.game.addToKillFeed(bird.shortName() + (ultimate ? " wound up an ULT HEAVY BILL THRUST!" : " wound up Heavy Bill Thrust!"));
    }

    static void up(Bird bird, boolean ultimate) {
        if (bird.shoebillUpSpecialUsed && !ultimate) {
            return;
        }
        int dir = bird.horizontalInputDirection();
        if (dir != 0) {
            bird.facingRight = dir > 0;
        }
        bird.shoebillUpSpecialUsed = true;
        bird.shoebillMarshLiftUltimate = ultimate;
        bird.shoebillMarshLiftTimer = ultimate ? Bird.SHOEBILL_MARSH_LIFT_FRAMES + 8 : Bird.SHOEBILL_MARSH_LIFT_FRAMES;
        Arrays.fill(bird.shoebillMarshLiftHit, false);
        bird.canDoubleJump = true;
        bird.vx *= 0.34;
        bird.vy = Math.min(bird.vy, ultimate ? -20.8 : -17.4);
        bird.attackAnimationTimer = Math.max(bird.attackAnimationTimer, bird.shoebillMarshLiftTimer);
        bird.specialCooldown = 0;
        bird.specialMaxCooldown = 0;
        bird.isBlocking = false;
        bird.parryWindowFrames = 0;
        bird.shieldStunFrames = 0;
        emitReedBurst(bird, bird.bodyCenterX(), bird.bodyBottomY() - 8.0 * bird.sizeMultiplier,
                bird.facingDirection(), ultimate ? 42 : 28, ultimate ? Color.GOLD : Color.web("#81C784"));
    }

    static void down(Bird bird, boolean ultimate) {
        bird.shoebillStatueTimer = ultimate ? Bird.SHOEBILL_STATUE_FRAMES + 30 : Bird.SHOEBILL_STATUE_FRAMES;
        bird.shoebillStatueReuseTimer = ultimate ? 48 : Bird.SHOEBILL_STATUE_REUSE_FRAMES;
        bird.shoebillStatueUltimate = ultimate;
        bird.shoebillStatueCountered = false;
        bird.shoebillCounterBurstTimer = 0;
        bird.shoebillCounterBurstUltimate = false;
        Arrays.fill(bird.shoebillCounterHit, false);
        bird.specialCooldown = 0;
        bird.specialMaxCooldown = 0;
        bird.attackAnimationTimer = Math.max(bird.attackAnimationTimer, bird.shoebillStatueTimer);
        bird.vx *= bird.isOnGround() ? 0.12 : 0.36;
        bird.vy = Math.min(bird.vy, bird.isOnGround() ? 0.0 : 2.2);
        bird.isBlocking = false;
        bird.parryWindowFrames = 0;
        bird.shieldStunFrames = 0;
        bird.blockCooldown = 0;
    }

    static void handleState(Bird bird) {
        if (bird.type != BirdGame3.BirdType.SHOEBILL && !bird.mockingbirdCopiedNeutralFrom(BirdGame3.BirdType.SHOEBILL)) {
            return;
        }
        if (bird.stunTime > 0.0) {
            reset(bird);
            if (bird.mockingbirdCopiedNeutralFrom(BirdGame3.BirdType.SHOEBILL)) {
                bird.mockingbirdCopiedNeutralSource = null;
            }
            return;
        }
        if (bird.shoebillThrustTimer > 0) {
            handleHeavyThrust(bird);
        }
        if (bird.shoebillMarshLiftTimer > 0) {
            handleMarshLift(bird);
        }
        if (bird.shoebillStatueTimer > 0) {
            handleStatueTrap(bird);
        }
    }

    static boolean active(Bird bird) {
        return bird.shoebillStareFxTimer > 0
                || bird.shoebillThrustTimer > 0
                || bird.shoebillMarshLiftTimer > 0
                || bird.shoebillStatueTimer > 0
                || bird.shoebillCounterBurstTimer > 0;
    }

    static boolean ready(Bird bird, Bird.ShoebillSpecialVariant variant) {
        boolean ultimateReady = bird.isUltimateReady();
        return switch (variant) {
            case NEUTRAL -> true;
            case SIDE -> ultimateReady || bird.shoebillThrustReuseTimer <= 0;
            case UP -> ultimateReady || !bird.shoebillUpSpecialUsed;
            case DOWN -> ultimateReady || bird.shoebillStatueReuseTimer <= 0;
        };
    }

    static boolean canConvertShieldIntoDown(Bird bird) {
        return bird.selectShoebillSpecialVariant() == Bird.ShoebillSpecialVariant.DOWN
                && bird.isBlocking
                && bird.shieldStunFrames <= 0;
    }

    static boolean statueCounterWindowActive(Bird bird) {
        return bird.type == BirdGame3.BirdType.SHOEBILL
                && bird.health > 0
                && bird.shoebillStatueTimer > 0
                && !bird.shoebillStatueCountered
                && bird.blockPressed();
    }

    static boolean tryStatueCounter(Bird bird, Bird attacker, double scaledDamage) {
        if (!statueCounterWindowActive(bird)) {
            return false;
        }
        bird.shoebillStatueCountered = true;
        bird.shoebillStatueTimer = Math.min(bird.shoebillStatueTimer, Bird.SHOEBILL_COUNTER_BURST_FRAMES);
        bird.shoebillCounterBurstTimer = Bird.SHOEBILL_COUNTER_BURST_FRAMES;
        bird.shoebillCounterBurstUltimate = bird.shoebillStatueUltimate;
        Arrays.fill(bird.shoebillCounterHit, false);
        bird.stunTime = 0.0;
        bird.knockdownTimer = 0;
        bird.vx *= 0.08;
        bird.vy = Math.min(bird.vy, -3.6);
        bird.attackAnimationTimer = Math.max(bird.attackAnimationTimer, Bird.SHOEBILL_COUNTER_BURST_FRAMES + 4);
        bird.game.shakeIntensity = Math.max(bird.game.shakeIntensity, bird.shoebillStatueUltimate ? 18 : 13);
        bird.game.hitstopFrames = Math.max(bird.game.hitstopFrames, bird.shoebillStatueUltimate ? 8 : 5);
        bird.game.addToKillFeed(bird.shortName() + (bird.shoebillStatueUltimate ? " ULT STATUE COUNTER!" : " STATUE COUNTER!"));
        applyCounterBurstHits(bird, attacker, Math.max(0.0, scaledDamage));
        emitReedBurst(bird, bird.bodyCenterX(), bird.bodyCenterY(), bird.facingDirection(),
                bird.shoebillStatueUltimate ? 46 : 32,
                bird.shoebillStatueUltimate ? Color.GOLD : Color.web("#B0BEC5"));
        return true;
    }

    static void interruptOnHit(Bird bird) {
        if (bird.type != BirdGame3.BirdType.SHOEBILL || statueCounterWindowActive(bird)) {
            return;
        }
        if (active(bird)) {
            bird.attackAnimationTimer = 0;
        }
        reset(bird);
    }

    static void reset(Bird bird) {
        bird.shoebillStareFxTimer = 0;
        bird.shoebillStareUltimate = false;
        bird.shoebillThrustTimer = 0;
        bird.shoebillThrustUltimate = false;
        bird.shoebillThrustDirection = bird.facingDirection();
        Arrays.fill(bird.shoebillThrustHit, false);
        bird.shoebillMarshLiftTimer = 0;
        bird.shoebillMarshLiftUltimate = false;
        Arrays.fill(bird.shoebillMarshLiftHit, false);
        bird.shoebillStatueTimer = 0;
        bird.shoebillStatueUltimate = false;
        bird.shoebillStatueCountered = false;
        bird.shoebillCounterBurstTimer = 0;
        bird.shoebillCounterBurstUltimate = false;
        Arrays.fill(bird.shoebillCounterHit, false);
    }

    static void emitReedBurst(Bird bird, double originX, double originY, int dir, int count, Color baseColor) {
        int particleCount = bird.scaledParticleCount(count);
        int safeDir = dir == 0 ? bird.facingDirection() : dir;
        for (int i = 0; i < particleCount; i++) {
            double angle = -Math.PI / 2.0 + (SimRng.next() - 0.5) * Math.PI * 0.85;
            double speed = 1.2 + SimRng.next() * 5.0;
            bird.game.particles.add(new Particle(
                    originX + (SimRng.next() - 0.5) * 22.0 * bird.sizeMultiplier,
                    originY + (SimRng.next() - 0.5) * 16.0 * bird.sizeMultiplier,
                    Math.cos(angle) * speed + safeDir * (0.2 + SimRng.next()),
                    Math.sin(angle) * speed - SimRng.next() * 2.2,
                    baseColor.deriveColor(0, 1, 1, 0.62 + SimRng.next() * 0.25)
            ));
        }
    }

    private static boolean hasDirectStareLine(Bird bird, Bird other, boolean ultimate) {
        int dir = bird.facingDirection();
        double s = bird.sizeMultiplier;
        double eyeX = bird.bodyCenterX() + dir * 26.0 * s;
        double eyeY = bird.bodyCenterY() - 18.0 * s;
        double targetOffsetFromShoebill = other.bodyCenterX() - bird.bodyCenterX();
        int targetFacingDir = other.facingDirection();
        if (Math.abs(targetOffsetFromShoebill) < 1.0
                || targetFacingDir != (targetOffsetFromShoebill > 0.0 ? 1 : -1)) {
            return false;
        }
        double forward = (other.bodyCenterX() - eyeX) * dir;
        double maxReach = (ultimate ? 190.0 : 126.0) * s + other.combatHalfWidth();
        double verticalReach = (ultimate ? 42.0 : 28.0) * s + other.combatHalfHeight() * 0.55;
        if (forward < 0.0 || forward > maxReach) {
            return false;
        }
        return Math.abs(other.bodyCenterY() - eyeY) <= verticalReach;
    }

    private static void handleHeavyThrust(Bird bird) {
        int total = bird.shoebillThrustUltimate ? Bird.SHOEBILL_THRUST_FRAMES + 8 : Bird.SHOEBILL_THRUST_FRAMES;
        int elapsed = total - bird.shoebillThrustTimer;
        int dir = bird.shoebillThrustDirection == 0 ? bird.facingDirection() : bird.shoebillThrustDirection;
        bird.facingRight = dir > 0;

        if (elapsed < Bird.SHOEBILL_THRUST_STARTUP_FRAMES) {
            bird.vx *= bird.isOnGround() ? 0.56 : 0.76;
            if (!bird.isOnGround()) {
                bird.vy = Math.min(bird.vy, 2.7);
            }
            if ((elapsed & 3) == 0) {
                bird.game.particles.add(new Particle(
                        bird.bodyCenterX() + dir * 34.0 * bird.sizeMultiplier,
                        bird.bodyCenterY() - 14.0 * bird.sizeMultiplier,
                        -dir * (0.6 + SimRng.next() * 1.4),
                        -0.7 - SimRng.next() * 1.2,
                        (bird.shoebillThrustUltimate ? Color.GOLD : Color.web("#78909C")).deriveColor(0, 1, 1, 0.62)
                ));
            }
            return;
        }

        int activeEnd = Bird.SHOEBILL_THRUST_STARTUP_FRAMES + Bird.SHOEBILL_THRUST_ACTIVE_FRAMES;
        if (elapsed < activeEnd) {
            double activePhase = Math.clamp((elapsed - Bird.SHOEBILL_THRUST_STARTUP_FRAMES + 1.0)
                    / Bird.SHOEBILL_THRUST_ACTIVE_FRAMES, 0.0, 1.0);
            double thrustSpeed = (bird.shoebillThrustUltimate ? 7.0 : 5.4) + activePhase * (bird.shoebillThrustUltimate ? 2.4 : 1.6);
            bird.vx = bird.vx * 0.35 + dir * thrustSpeed;
            bird.vy *= bird.isOnGround() ? 0.70 : 0.88;
            applyThrustHits(bird, activePhase);
            if ((elapsed & 1) == 0) {
                emitReedBurst(
                        bird,
                        bird.bodyCenterX() + dir * (66.0 + activePhase * 50.0) * bird.sizeMultiplier,
                        bird.bodyCenterY() - 8.0 * bird.sizeMultiplier,
                        dir,
                        bird.shoebillThrustUltimate ? 7 : 5,
                        bird.shoebillThrustUltimate ? Color.GOLD : Color.web("#A7C7B2")
                );
            }
        } else {
            bird.vx *= bird.isOnGround() ? 0.50 : 0.78;
        }
    }

    private static void applyThrustHits(Bird bird, double activePhase) {
        int dir = bird.shoebillThrustDirection == 0 ? bird.facingDirection() : bird.shoebillThrustDirection;
        double s = bird.sizeMultiplier;
        double originX = bird.bodyCenterX() + dir * 18.0 * s;
        double originY = bird.bodyCenterY() - 9.0 * s;
        double reach = (bird.shoebillThrustUltimate ? 188.0 : 154.0) * s;
        double verticalReach = (bird.shoebillThrustUltimate ? 76.0 : 62.0) * s;

        for (Bird other : bird.game.players) {
            if (!bird.canDamageTarget(other)) continue;
            if (other.playerIndex < 0 || other.playerIndex >= bird.shoebillThrustHit.length) continue;
            if (bird.shoebillThrustHit[other.playerIndex]) continue;

            double forward = (other.bodyCenterX() - originX) * dir;
            if (forward < -other.combatHalfWidth() * 0.25 || forward > reach + other.combatHalfWidth()) continue;
            if (Math.abs(other.bodyCenterY() - originY) > verticalReach + other.combatHalfHeight()) continue;

            bird.shoebillThrustHit[other.playerIndex] = true;
            double oldHealth = other.health;
            int dmg = (bird.shoebillThrustUltimate ? 25 : 18) + (int) Math.round(activePhase * (bird.shoebillThrustUltimate ? 5.0 : 3.0));
            int dealt = (int) bird.applyDamageTo(other, dmg);
            if (dealt <= 0) continue;

            bird.game.damageDealt[bird.playerIndex] += dealt;
            bird.game.recordSpecialImpact(bird.playerIndex, dealt, true);
            if (other.health <= 0 && oldHealth > 0) {
                bird.game.eliminations[bird.playerIndex]++;
            }
            other.vx += dir * (bird.shoebillThrustUltimate ? 28.0 : 22.0);
            other.vy -= bird.shoebillThrustUltimate ? 8.6 : 6.2;
            other.applyStun(bird.shoebillThrustUltimate ? 16 : 8);
            bird.game.hitstopFrames = Math.max(bird.game.hitstopFrames, bird.shoebillThrustUltimate ? 8 : 5);
            bird.game.shakeIntensity = Math.max(bird.game.shakeIntensity, bird.shoebillThrustUltimate ? 17 : 11);
            emitReedBurst(bird, other.bodyCenterX(), other.bodyCenterY(), dir,
                    bird.shoebillThrustUltimate ? 24 : 16,
                    bird.shoebillThrustUltimate ? Color.GOLD : Color.web("#CFD8DC"));
        }
    }

    private static void handleMarshLift(Bird bird) {
        int total = bird.shoebillMarshLiftUltimate ? Bird.SHOEBILL_MARSH_LIFT_FRAMES + 8 : Bird.SHOEBILL_MARSH_LIFT_FRAMES;
        int elapsed = total - bird.shoebillMarshLiftTimer;
        double s = bird.sizeMultiplier;
        bird.vx *= 0.88;
        if (elapsed < total * 0.70 && bird.vy > (bird.shoebillMarshLiftUltimate ? -12.0 : -9.4)) {
            bird.vy -= bird.shoebillMarshLiftUltimate ? 0.84 : 0.64;
        }
        if ((elapsed & 1) == 0) {
            double spread = (SimRng.next() - 0.5) * (bird.shoebillMarshLiftUltimate ? 92.0 : 72.0) * s;
            bird.game.particles.add(new Particle(
                    bird.bodyCenterX() + spread,
                    bird.bodyBottomY() - 5.0 * s,
                    spread * 0.015,
                    -4.0 - SimRng.next() * (bird.shoebillMarshLiftUltimate ? 6.2 : 4.6),
                    (bird.shoebillMarshLiftUltimate ? Color.GOLD : Color.web("#66BB6A")).deriveColor(0, 1, 1, 0.62)
            ));
        }

        double centerX = bird.bodyCenterX();
        double centerY = bird.bodyCenterY() + 22.0 * s;
        double horizontalReach = (bird.shoebillMarshLiftUltimate ? 104.0 : 82.0) * s;
        double lowerReach = (bird.shoebillMarshLiftUltimate ? 94.0 : 76.0) * s;
        double upperReach = (bird.shoebillMarshLiftUltimate ? 210.0 : 168.0) * s;
        for (Bird other : bird.game.players) {
            if (!bird.canDamageTarget(other)) continue;
            if (other.playerIndex < 0 || other.playerIndex >= bird.shoebillMarshLiftHit.length) continue;
            if (bird.shoebillMarshLiftHit[other.playerIndex]) continue;

            double dx = Math.abs(other.bodyCenterX() - centerX);
            double dy = other.bodyCenterY() - centerY;
            if (dx > horizontalReach + other.combatHalfWidth()) continue;
            if (dy < -upperReach - other.combatHalfHeight() || dy > lowerReach + other.combatHalfHeight()) continue;

            bird.shoebillMarshLiftHit[other.playerIndex] = true;
            double oldHealth = other.health;
            int dealt = (int) bird.applyDamageTo(other, bird.shoebillMarshLiftUltimate ? 10 : 7);
            if (dealt <= 0) continue;

            bird.game.damageDealt[bird.playerIndex] += dealt;
            bird.game.recordSpecialImpact(bird.playerIndex, dealt, true);
            if (other.health <= 0 && oldHealth > 0) {
                bird.game.eliminations[bird.playerIndex]++;
            }
            double pushDir = Math.signum(other.bodyCenterX() - centerX);
            if (pushDir == 0.0) {
                pushDir = bird.facingDirection();
            }
            other.vx += pushDir * (bird.shoebillMarshLiftUltimate ? 8.0 : 5.6);
            other.vy = Math.min(other.vy, -(bird.shoebillMarshLiftUltimate ? 17.0 : 13.4));
            other.applyStun(bird.shoebillMarshLiftUltimate ? 12 : 7);
            bird.game.shakeIntensity = Math.max(bird.game.shakeIntensity, bird.shoebillMarshLiftUltimate ? 10 : 6);
            emitReedBurst(bird, other.bodyCenterX(), other.bodyBottomY(), (int) pushDir,
                    bird.shoebillMarshLiftUltimate ? 22 : 14,
                    bird.shoebillMarshLiftUltimate ? Color.GOLD : Color.web("#A5D6A7"));
        }
    }

    private static void handleStatueTrap(Bird bird) {
        if (bird.shoebillStatueTimer <= 0) {
            return;
        }
        if (!bird.blockPressed() && !bird.shoebillStatueCountered) {
            bird.shoebillStatueTimer = 0;
            bird.shoebillStatueUltimate = false;
            bird.attackAnimationTimer = Math.min(bird.attackAnimationTimer, 2);
            return;
        }
        bird.vx *= bird.isOnGround() ? 0.05 : 0.42;
        if (!bird.isOnGround()) {
            bird.vy = Math.min(bird.vy, 1.8);
        }
        bird.attackAnimationTimer = Math.max(bird.attackAnimationTimer, bird.shoebillStatueCountered ? 4 : 10);
        if ((bird.shoebillStatueTimer & 3) == 0) {
            bird.game.particles.add(new Particle(
                    bird.bodyCenterX() + (SimRng.next() - 0.5) * 34.0 * bird.sizeMultiplier,
                    bird.bodyBottomY() - 8.0 * bird.sizeMultiplier,
                    (SimRng.next() - 0.5) * 0.8,
                    -0.8 - SimRng.next() * 1.4,
                    (bird.shoebillStatueUltimate ? Color.GOLD : Color.web("#455A64")).deriveColor(0, 1, 1, 0.58)
            ));
        }
    }

    private static void applyCounterBurstHits(Bird bird, Bird primaryTarget, double absorbedDamage) {
        double radius = (bird.shoebillCounterBurstUltimate ? 190.0 : 145.0) * bird.sizeMultiplier;
        double verticalRadius = (bird.shoebillCounterBurstUltimate ? 132.0 : 104.0) * bird.sizeMultiplier;
        double centerX = bird.bodyCenterX();
        double centerY = bird.bodyCenterY() - 5.0 * bird.sizeMultiplier;
        for (Bird other : bird.game.players) {
            if (!bird.canDamageTarget(other)) continue;
            if (other.playerIndex < 0 || other.playerIndex >= bird.shoebillCounterHit.length) continue;
            if (bird.shoebillCounterHit[other.playerIndex]) continue;
            double dx = other.bodyCenterX() - centerX;
            double dy = other.bodyCenterY() - centerY;
            boolean directCounterTarget = other == primaryTarget;
            if (!directCounterTarget) {
                double normalized = Math.hypot(dx / Math.max(1.0, radius), dy / Math.max(1.0, verticalRadius));
                if (normalized > 1.0 + other.combatRadius() / Math.max(radius, verticalRadius)) continue;
            }

            bird.shoebillCounterHit[other.playerIndex] = true;
            double oldHealth = other.health;
            int dmg = (bird.shoebillCounterBurstUltimate ? 13 : 8)
                    + (int) Math.round(Math.clamp(absorbedDamage / 32.0, 0.0, 1.0) * (bird.shoebillCounterBurstUltimate ? 5.0 : 3.0));
            int dealt = (int) bird.applyDamageTo(other, dmg);
            if (dealt <= 0 && !directCounterTarget) continue;

            if (dealt > 0) {
                bird.game.damageDealt[bird.playerIndex] += dealt;
                bird.game.recordSpecialImpact(bird.playerIndex, dealt, true);
                if (other.health <= 0 && oldHealth > 0) {
                    bird.game.eliminations[bird.playerIndex]++;
                }
            } else {
                bird.game.recordSpecialImpact(bird.playerIndex, 0, true);
            }
            double pushDir = Math.signum(dx);
            if (pushDir == 0.0) {
                pushDir = bird.facingDirection();
            }
            other.vx += pushDir * (bird.shoebillCounterBurstUltimate ? 13.0 : 9.0);
            other.vy -= bird.shoebillCounterBurstUltimate ? 8.2 : 5.8;
            other.applyStun(bird.shoebillCounterBurstUltimate ? 92 : 66);
        }
    }
}
