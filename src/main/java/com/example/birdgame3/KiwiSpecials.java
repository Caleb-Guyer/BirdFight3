package com.example.birdgame3;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

import java.util.Arrays;

/** Straightforward, low-execution specials for Kiwi Bird. */
final class KiwiSpecials {
    static final String MIDNIGHT_STAMPEDE_MOVE = "Midnight Stampede";

    private KiwiSpecials() {
    }

    static boolean active(Bird bird) {
        return bird.kiwiProbeTimer > 0
                || bird.kiwiBurrowTimer > 0
                || bird.kiwiSpringTimer > 0
                || bird.kiwiStompTimer > 0
                || bird.kiwiUltimateTimer > 0;
    }

    static boolean ready(Bird bird, Bird.KiwiSpecialVariant variant) {
        if (bird.isUltimateReady()) {
            return true;
        }
        return switch (variant) {
            case NEUTRAL -> bird.kiwiProbeReuseTimer <= 0;
            case SIDE -> bird.kiwiBurrowReuseTimer <= 0;
            case UP -> !bird.kiwiSpringUsed && bird.kiwiSpringReuseTimer <= 0;
            case DOWN -> bird.kiwiStompReuseTimer <= 0;
        };
    }

    static boolean canConvertShieldIntoDown(Bird bird) {
        return bird.type == BirdGame3.BirdType.KIWI
                && bird.selectKiwiSpecialVariant() == Bird.KiwiSpecialVariant.DOWN
                && bird.isBlocking
                && bird.shieldStunFrames <= 0;
    }

    static boolean canStart(Bird bird, boolean grabbed, boolean dodging) {
        Bird.KiwiSpecialVariant variant = bird.selectKiwiSpecialVariant();
        boolean shieldConversion = canConvertShieldIntoDown(bird);
        return bird.type == BirdGame3.BirdType.KIWI
                && bird.health > 0
                && bird.stunTime <= 0.0
                && !grabbed
                && (!bird.isBlocking || shieldConversion)
                && !dodging
                && !active(bird)
                && ready(bird, variant);
    }

    static void use(Bird bird, boolean ultimate) {
        if (ultimate) {
            startUltimate(bird);
            return;
        }
        switch (bird.selectKiwiSpecialVariant()) {
            case NEUTRAL -> startProbe(bird);
            case SIDE -> startBurrow(bird);
            case UP -> startSpringKick(bird);
            case DOWN -> startEarthStomp(bird);
        }
    }

    static void copiedNeutral(Bird bird, boolean ultimate) {
        if (ultimate) {
            startUltimate(bird);
        } else {
            startProbe(bird);
        }
    }

    private static void startProbe(Bird bird) {
        int direction = directionFor(bird);
        bird.kiwiProbeDirection = direction;
        bird.facingRight = direction > 0;
        bird.kiwiProbeTimer = Bird.KIWI_PROBE_FRAMES;
        bird.kiwiProbeReuseTimer = Bird.KIWI_PROBE_REUSE_FRAMES;
        bird.kiwiProbeStrikeIndex = 0;
        Arrays.fill(bird.kiwiProbeHit, false);
        bird.attackAnimationTimer = Math.max(bird.attackAnimationTimer, Bird.KIWI_PROBE_FRAMES);
        bird.vx *= bird.isOnGround() ? 0.34 : 0.72;
        clearSharedCooldown(bird);
    }

    private static void startBurrow(Bird bird) {
        int direction = directionFor(bird);
        bird.kiwiBurrowDirection = direction;
        bird.facingRight = direction > 0;
        bird.kiwiBurrowTimer = Bird.KIWI_BURROW_FRAMES;
        bird.kiwiBurrowReuseTimer = Bird.KIWI_BURROW_REUSE_FRAMES;
        bird.kiwiBurrowGrounded = bird.isOnGround();
        bird.kiwiBurrowErupted = false;
        Arrays.fill(bird.kiwiBurrowHit, false);
        bird.attackAnimationTimer = Math.max(bird.attackAnimationTimer, Bird.KIWI_BURROW_FRAMES + 4);
        bird.vx = direction * (bird.kiwiBurrowGrounded ? 13.2 : 10.8);
        if (!bird.kiwiBurrowGrounded) {
            bird.vy *= 0.36;
        }
        clearSharedCooldown(bird);
        emitDirt(bird, bird.bodyCenterX(), bird.bodyBottomY(), 22, 5.0);
    }

    private static void startSpringKick(Bird bird) {
        int direction = directionFor(bird);
        bird.kiwiSpringUsed = true;
        bird.kiwiSpringDirection = direction;
        bird.facingRight = direction > 0;
        bird.kiwiSpringTimer = Bird.KIWI_SPRING_FRAMES;
        bird.kiwiSpringReuseTimer = Bird.KIWI_SPRING_REUSE_FRAMES;
        Arrays.fill(bird.kiwiSpringHit, false);
        bird.attackAnimationTimer = Math.max(bird.attackAnimationTimer, Bird.KIWI_SPRING_FRAMES);
        bird.canDoubleJump = true;
        bird.vx = bird.vx * 0.28 + direction * 4.6;
        bird.vy = -18.4;
        clearSharedCooldown(bird);
        emitDirt(bird, bird.bodyCenterX(), bird.bodyBottomY(), 28, 7.2);
    }

    private static void startEarthStomp(Bird bird) {
        bird.isBlocking = false;
        bird.parryWindowFrames = 0;
        bird.kiwiStompTimer = Bird.KIWI_STOMP_FRAMES;
        bird.kiwiStompReuseTimer = Bird.KIWI_STOMP_REUSE_FRAMES;
        bird.kiwiStompAirborne = !bird.isOnGround();
        bird.kiwiStompImpactResolved = false;
        bird.kiwiStompImpactFxTimer = 0;
        Arrays.fill(bird.kiwiStompHit, false);
        bird.attackAnimationTimer = Math.max(bird.attackAnimationTimer, Bird.KIWI_STOMP_FRAMES);
        if (bird.kiwiStompAirborne) {
            bird.vx *= 0.32;
            bird.vy = Math.max(9.5, bird.vy + 4.0);
        } else {
            bird.vx *= 0.18;
            emitDirt(bird, bird.bodyCenterX(), bird.bodyBottomY(), 12, 3.2);
        }
        clearSharedCooldown(bird);
    }

    private static void startUltimate(Bird bird) {
        int direction = directionFor(bird);
        bird.kiwiUltimateDirection = direction;
        bird.facingRight = direction > 0;
        bird.kiwiUltimateTimer = Bird.KIWI_ULTIMATE_FRAMES;
        bird.kiwiUltimateWaveIndex = 0;
        bird.kiwiUltimateFinalResolved = false;
        Arrays.fill(bird.kiwiUltimateHitCooldown, 0);
        bird.attackAnimationTimer = Math.max(bird.attackAnimationTimer, Bird.KIWI_ULTIMATE_FRAMES);
        clearSharedCooldown(bird);
        bird.game.addToKillFeed(bird.shortName() + " began the MIDNIGHT STAMPEDE!");
        emitDirt(bird, bird.bodyCenterX(), bird.bodyBottomY(), 70, 12.0);
    }

    static void handleState(Bird bird) {
        decrementReuseTimers(bird);
        if (bird.health <= 0) {
            return;
        }
        handleProbe(bird);
        handleBurrow(bird);
        handleSpring(bird);
        handleStomp(bird);
        handleUltimate(bird);
    }

    static void handlePostMoveState(Bird bird) {
        if (bird.health <= 0) {
            return;
        }
        if (bird.kiwiBurrowTimer > 0) {
            applyBurrowHit(bird);
        }
        if (bird.kiwiSpringTimer > 0) {
            applySpringHit(bird);
        }
        if (bird.kiwiStompTimer > 0 && bird.kiwiStompAirborne && !bird.kiwiStompImpactResolved) {
            applyDivingStompHit(bird);
            if (bird.isOnGround()) {
                resolveStompImpact(bird, true);
            }
        }
    }

    private static void decrementReuseTimers(Bird bird) {
        bird.kiwiProbeReuseTimer = Math.max(0, bird.kiwiProbeReuseTimer - 1);
        bird.kiwiBurrowReuseTimer = Math.max(0, bird.kiwiBurrowReuseTimer - 1);
        bird.kiwiSpringReuseTimer = Math.max(0, bird.kiwiSpringReuseTimer - 1);
        bird.kiwiStompReuseTimer = Math.max(0, bird.kiwiStompReuseTimer - 1);
        bird.kiwiStompImpactFxTimer = Math.max(0, bird.kiwiStompImpactFxTimer - 1);
        for (int i = 0; i < bird.kiwiUltimateHitCooldown.length; i++) {
            bird.kiwiUltimateHitCooldown[i] = Math.max(0, bird.kiwiUltimateHitCooldown[i] - 1);
        }
    }

    private static void handleProbe(Bird bird) {
        if (bird.kiwiProbeTimer <= 0) {
            return;
        }
        int elapsed = Bird.KIWI_PROBE_FRAMES - bird.kiwiProbeTimer;
        int strike = elapsed >= 16 ? 3 : elapsed >= 10 ? 2 : elapsed >= 4 ? 1 : 0;
        if (strike > bird.kiwiProbeStrikeIndex) {
            bird.kiwiProbeStrikeIndex = strike;
            Arrays.fill(bird.kiwiProbeHit, false);
            applyProbeHit(bird, strike);
        }
        bird.kiwiProbeTimer--;
    }

    private static void applyProbeHit(Bird bird, int strike) {
        double centerX = bird.bodyCenterX() + bird.kiwiProbeDirection * (58.0 + strike * 7.0) * bird.sizeMultiplier;
        double centerY = bird.bodyCenterY() - 9.0 * bird.sizeMultiplier;
        double reachX = (strike == 3 ? 76.0 : 64.0) * bird.sizeMultiplier;
        double reachY = 48.0 * bird.sizeMultiplier;
        for (Bird other : bird.game.players) {
            if (!bird.canDamageTarget(other) || alreadyHit(bird.kiwiProbeHit, other)) continue;
            if (Math.abs(other.bodyCenterX() - centerX) > reachX + other.combatHalfWidth()) continue;
            if (Math.abs(other.bodyCenterY() - centerY) > reachY + other.combatHalfHeight()) continue;
            markHit(bird.kiwiProbeHit, other);
            int dealt = bird.applyTrackedSpecialDamage(other, strike == 3 ? 7 : 3);
            if (dealt <= 0) continue;
            other.vx += bird.kiwiProbeDirection * (strike == 3 ? 10.8 : 3.6);
            other.vy -= strike == 3 ? 5.4 : 1.8;
            other.applyStun(strike == 3 ? 11 : 5);
            impact(bird, other, dealt, strike == 3, strike == 3 ? "Rapid Probe Finish" : "Rapid Probe");
        }
    }

    private static void handleBurrow(Bird bird) {
        if (bird.kiwiBurrowTimer <= 0) {
            return;
        }
        int elapsed = Bird.KIWI_BURROW_FRAMES - bird.kiwiBurrowTimer;
        if (!bird.kiwiBurrowErupted && bird.kiwiBurrowGrounded && elapsed >= 17) {
            bird.kiwiBurrowErupted = true;
            bird.vx *= 0.35;
            bird.vy = -8.6;
            Arrays.fill(bird.kiwiBurrowHit, false);
            applyBurrowEruption(bird);
            emitDirt(bird, bird.bodyCenterX(), bird.bodyBottomY(), 42, 10.0);
        } else if (!bird.kiwiBurrowErupted) {
            bird.vx = bird.kiwiBurrowDirection * (bird.kiwiBurrowGrounded ? 13.2 : 10.8);
            if (bird.kiwiBurrowGrounded) {
                bird.vy = Math.min(0.0, bird.vy);
            }
        }
        bird.kiwiBurrowTimer--;
    }

    private static void applyBurrowHit(Bird bird) {
        if (bird.kiwiBurrowErupted) {
            return;
        }
        double centerX = bird.bodyCenterX() + bird.kiwiBurrowDirection * 28.0 * bird.sizeMultiplier;
        double centerY = bird.bodyCenterY() + (bird.kiwiBurrowGrounded ? 20.0 : 0.0) * bird.sizeMultiplier;
        for (Bird other : bird.game.players) {
            if (!bird.canDamageTarget(other) || alreadyHit(bird.kiwiBurrowHit, other)) continue;
            if (Math.abs(other.bodyCenterX() - centerX) > 58.0 * bird.sizeMultiplier + other.combatHalfWidth()) continue;
            if (Math.abs(other.bodyCenterY() - centerY) > 50.0 * bird.sizeMultiplier + other.combatHalfHeight()) continue;
            markHit(bird.kiwiBurrowHit, other);
            int dealt = bird.applyTrackedSpecialDamage(other, bird.kiwiBurrowGrounded ? 9 : 8);
            if (dealt <= 0) continue;
            other.vx += bird.kiwiBurrowDirection * 9.2;
            other.vy -= 3.6;
            impact(bird, other, dealt, false, bird.kiwiBurrowGrounded ? "Burrow Charge" : "Charging Bill");
        }
    }

    private static void applyBurrowEruption(Bird bird) {
        for (Bird other : bird.game.players) {
            if (!bird.canDamageTarget(other) || alreadyHit(bird.kiwiBurrowHit, other)) continue;
            double dx = other.bodyCenterX() - bird.bodyCenterX();
            double dy = other.bodyCenterY() - bird.bodyBottomY();
            if (Math.abs(dx) > 86.0 * bird.sizeMultiplier + other.combatHalfWidth()) continue;
            if (dy > 42.0 * bird.sizeMultiplier + other.combatHalfHeight()
                    || dy < -104.0 * bird.sizeMultiplier - other.combatHalfHeight()) continue;
            markHit(bird.kiwiBurrowHit, other);
            int dealt = bird.applyTrackedSpecialDamage(other, 11);
            if (dealt <= 0) continue;
            double launch = Math.signum(dx);
            if (launch == 0.0) launch = bird.kiwiBurrowDirection;
            other.vx += launch * 7.5;
            other.vy -= 10.6;
            impact(bird, other, dealt, true, "Burrow Eruption");
        }
    }

    private static void handleSpring(Bird bird) {
        if (bird.kiwiSpringTimer <= 0) {
            return;
        }
        int elapsed = Bird.KIWI_SPRING_FRAMES - bird.kiwiSpringTimer;
        if (elapsed < 10) {
            bird.vx += bird.kiwiSpringDirection * 0.12;
            bird.vy = Math.min(bird.vy, -7.4);
        }
        bird.kiwiSpringTimer--;
    }

    private static void applySpringHit(Bird bird) {
        double centerX = bird.bodyCenterX() + bird.kiwiSpringDirection * 22.0 * bird.sizeMultiplier;
        double centerY = bird.bodyCenterY() - 30.0 * bird.sizeMultiplier;
        for (Bird other : bird.game.players) {
            if (!bird.canDamageTarget(other) || alreadyHit(bird.kiwiSpringHit, other)) continue;
            if (Math.abs(other.bodyCenterX() - centerX) > 62.0 * bird.sizeMultiplier + other.combatHalfWidth()) continue;
            if (Math.abs(other.bodyCenterY() - centerY) > 72.0 * bird.sizeMultiplier + other.combatHalfHeight()) continue;
            markHit(bird.kiwiSpringHit, other);
            int dealt = bird.applyTrackedSpecialDamage(other, 9);
            if (dealt <= 0) continue;
            other.vx += bird.kiwiSpringDirection * 5.8;
            other.vy -= 11.4;
            impact(bird, other, dealt, true, "Spring Kick");
        }
    }

    private static void handleStomp(Bird bird) {
        if (bird.kiwiStompTimer <= 0) {
            return;
        }
        int elapsed = Bird.KIWI_STOMP_FRAMES - bird.kiwiStompTimer;
        if (bird.kiwiStompAirborne && !bird.kiwiStompImpactResolved) {
            bird.vx *= 0.82;
            bird.vy = Math.max(13.8, bird.vy);
        } else if (!bird.kiwiStompAirborne && !bird.kiwiStompImpactResolved && elapsed >= 7) {
            resolveStompImpact(bird, false);
        }
        // Keep the plunge active until it actually finds ground. The bird will
        // still be defeated normally if it falls through a blast boundary.
        if (bird.kiwiStompAirborne && !bird.kiwiStompImpactResolved && bird.kiwiStompTimer <= 2) {
            bird.kiwiStompTimer = 2;
        }
        bird.kiwiStompTimer--;
    }

    private static void applyDivingStompHit(Bird bird) {
        double centerX = bird.bodyCenterX();
        double centerY = bird.bodyBottomY() + 18.0 * bird.sizeMultiplier;
        for (Bird other : bird.game.players) {
            if (!bird.canDamageTarget(other) || alreadyHit(bird.kiwiStompHit, other)) continue;
            if (Math.abs(other.bodyCenterX() - centerX) > 54.0 * bird.sizeMultiplier + other.combatHalfWidth()) continue;
            if (Math.abs(other.bodyCenterY() - centerY) > 62.0 * bird.sizeMultiplier + other.combatHalfHeight()) continue;
            markHit(bird.kiwiStompHit, other);
            int dealt = bird.applyTrackedSpecialDamage(other, 10);
            if (dealt <= 0) continue;
            other.vx += Math.signum(other.bodyCenterX() - centerX) * 5.2;
            other.vy += 10.0;
            bird.vy = -10.5;
            bird.kiwiStompImpactResolved = true;
            beginStompImpactFx(bird, true);
            impact(bird, other, dealt, true, "Earth Stomp Dive");
            emitDirt(bird, centerX, centerY, 30, 8.0);
        }
    }

    private static void resolveStompImpact(Bird bird, boolean aerial) {
        if (bird.kiwiStompImpactResolved) {
            return;
        }
        bird.kiwiStompImpactResolved = true;
        beginStompImpactFx(bird, aerial);
        bird.vx *= 0.2;
        if (aerial) {
            bird.vy = Math.min(bird.vy, -4.8);
        }
        Arrays.fill(bird.kiwiStompHit, false);
        double centerX = bird.bodyCenterX();
        double centerY = bird.bodyBottomY();
        for (Bird other : bird.game.players) {
            if (!bird.canDamageTarget(other) || alreadyHit(bird.kiwiStompHit, other)) continue;
            double dx = other.bodyCenterX() - centerX;
            double dy = other.bodyCenterY() - centerY;
            if (Math.abs(dx) > 132.0 * bird.sizeMultiplier + other.combatHalfWidth()) continue;
            if (Math.abs(dy) > 62.0 * bird.sizeMultiplier + other.combatHalfHeight()) continue;
            markHit(bird.kiwiStompHit, other);
            int dealt = bird.applyTrackedSpecialDamage(other, aerial ? 13 : 11);
            if (dealt <= 0) continue;
            double launch = Math.signum(dx);
            if (launch == 0.0) launch = bird.facingDirection();
            other.vx += launch * (aerial ? 12.4 : 9.8);
            other.vy -= aerial ? 7.8 : 5.6;
            impact(bird, other, dealt, true, "Earth Stomp");
        }
        bird.game.shakeIntensity = Math.max(bird.game.shakeIntensity, aerial ? 15 : 10);
        bird.game.playHitSound(aerial ? 14.0 : 10.0);
        emitDirt(bird, centerX, centerY, aerial ? 56 : 42, aerial ? 11.5 : 8.5);
    }

    private static void beginStompImpactFx(Bird bird, boolean aerial) {
        bird.kiwiStompImpactFxTimer = Bird.KIWI_STOMP_IMPACT_FX_FRAMES;
        bird.game.shakeIntensity = Math.max(bird.game.shakeIntensity, aerial ? 15 : 10);
    }

    private static void handleUltimate(Bird bird) {
        if (bird.kiwiUltimateTimer <= 0) {
            return;
        }
        int elapsed = Bird.KIWI_ULTIMATE_FRAMES - bird.kiwiUltimateTimer;
        int wave = elapsed >= 86 ? 3 : elapsed >= 52 ? 2 : elapsed >= 18 ? 1 : 0;
        if (wave > bird.kiwiUltimateWaveIndex) {
            if (bird.kiwiUltimateWaveIndex > 0) {
                bird.kiwiUltimateDirection = -bird.kiwiUltimateDirection;
            }
            bird.kiwiUltimateWaveIndex = wave;
            bird.facingRight = bird.kiwiUltimateDirection > 0;
            Arrays.fill(bird.kiwiUltimateHitCooldown, 0);
            emitDirt(bird, bird.bodyCenterX(), bird.bodyBottomY(), 46, 10.0);
        }
        boolean charging = (elapsed >= 18 && elapsed < 36)
                || (elapsed >= 52 && elapsed < 70)
                || (elapsed >= 86 && elapsed < 104);
        if (charging) {
            bird.vx = bird.kiwiUltimateDirection * 20.5;
            applyUltimateChargeHits(bird);
        } else {
            bird.vx *= 0.72;
        }
        if (!bird.kiwiUltimateFinalResolved && elapsed >= 126) {
            bird.kiwiUltimateFinalResolved = true;
            applyUltimateEruption(bird);
        }
        bird.kiwiUltimateTimer--;
    }

    private static void applyUltimateChargeHits(Bird bird) {
        for (Bird other : bird.game.players) {
            if (!bird.canDamageTarget(other) || onHitCooldown(bird.kiwiUltimateHitCooldown, other)) continue;
            if (Math.abs(other.bodyCenterX() - bird.bodyCenterX()) > 88.0 * bird.sizeMultiplier + other.combatHalfWidth()) continue;
            if (Math.abs(other.bodyCenterY() - bird.bodyCenterY()) > 74.0 * bird.sizeMultiplier + other.combatHalfHeight()) continue;
            setHitCooldown(bird.kiwiUltimateHitCooldown, other, 16);
            int dealt = bird.applyTrackedSpecialDamage(other, 7);
            if (dealt <= 0) continue;
            other.vx += bird.kiwiUltimateDirection * 10.5;
            other.vy -= 4.8;
            impact(bird, other, dealt, false, MIDNIGHT_STAMPEDE_MOVE);
        }
    }

    private static void applyUltimateEruption(Bird bird) {
        boolean hitAny = false;
        for (Bird other : bird.game.players) {
            if (!bird.canDamageTarget(other)) continue;
            double dx = other.bodyCenterX() - bird.bodyCenterX();
            double dy = other.bodyCenterY() - bird.bodyCenterY();
            if (Math.hypot(dx, dy) > 360.0 * bird.sizeMultiplier + other.combatRadius()) continue;
            int dealt = bird.applyTrackedSpecialDamage(other, 24);
            if (dealt <= 0) continue;
            hitAny = true;
            double launch = Math.signum(dx);
            if (launch == 0.0) launch = bird.facingDirection();
            other.vx += launch * 17.0;
            other.vy -= 13.5;
            other.applyStun(22);
            impact(bird, other, dealt, true, "Midnight Eruption");
        }
        bird.game.shakeIntensity = Math.max(bird.game.shakeIntensity, 28);
        bird.game.hitstopFrames = Math.max(bird.game.hitstopFrames, hitAny ? 8 : 4);
        bird.game.triggerFlash(0.48, false);
        emitDirt(bird, bird.bodyCenterX(), bird.bodyBottomY(), 110, 16.0);
    }

    static void drawEffects(Bird bird, GraphicsContext g) {
        if (bird.type != BirdGame3.BirdType.KIWI && !active(bird)) {
            return;
        }
        double s = bird.sizeMultiplier;
        double cx = bird.bodyCenterX();
        double feet = bird.bodyBottomY();
        if (bird.kiwiBurrowTimer > 0 && bird.kiwiBurrowGrounded && !bird.kiwiBurrowErupted) {
            double phase = (Bird.KIWI_BURROW_FRAMES - bird.kiwiBurrowTimer) * 0.55;
            g.setFill(Color.web("#3E2C20", 0.72));
            g.fillOval(cx - 46.0 * s, feet - 15.0 * s, 92.0 * s, 29.0 * s);
            g.setStroke(Color.web("#B58A5A", 0.82));
            g.setLineWidth(3.0 * s);
            for (int i = -2; i <= 2; i++) {
                double ripple = Math.sin(phase + i * 0.9) * 5.0 * s;
                g.strokeLine(cx + i * 22.0 * s, feet - 3.0 * s,
                        cx + i * 28.0 * s + ripple, feet - 13.0 * s);
            }
        }
        if (bird.kiwiStompTimer > 0 && !bird.kiwiStompImpactResolved) {
            int elapsed = Bird.KIWI_STOMP_FRAMES - bird.kiwiStompTimer;
            if (bird.kiwiStompAirborne) {
                double pulse = 0.72 + 0.28 * Math.sin(elapsed * 0.9);
                double trailHeight = (82.0 + Math.min(80.0, Math.max(0.0, bird.vy) * 4.0)) * s;
                g.setFill(Color.web("#E8C98E", 0.13 + pulse * 0.09));
                g.fillPolygon(
                        new double[]{cx - 34.0 * s, cx + 34.0 * s, cx},
                        new double[]{feet - trailHeight, feet - trailHeight, feet + 18.0 * s}, 3);
                g.setStroke(Color.web("#FFE0A3", 0.56 + pulse * 0.24));
                g.setLineWidth(3.0 * s);
                for (int i = -2; i <= 2; i++) {
                    double offset = i * 18.0 * s;
                    double startY = feet - (54.0 + Math.abs(i) * 13.0 + (elapsed * 9 + i * 17) % 38) * s;
                    g.strokeLine(cx + offset, startY, cx + offset * 0.52, startY + 34.0 * s);
                }
                g.setStroke(Color.web("#FFF2C5", 0.86));
                g.setLineWidth(4.0 * s);
                for (int i = 0; i < 2; i++) {
                    double arrowY = feet + (20.0 + i * 22.0) * s;
                    double half = (14.0 - i * 2.0) * s;
                    g.strokeLine(cx - half, arrowY - 10.0 * s, cx, arrowY);
                    g.strokeLine(cx + half, arrowY - 10.0 * s, cx, arrowY);
                }
            } else {
                double windup = Math.clamp(elapsed / 7.0, 0.0, 1.0);
                double radius = (74.0 - windup * 42.0) * s;
                g.setStroke(Color.web("#E7BE7A", 0.24 + windup * 0.54));
                g.setLineWidth((2.0 + windup * 3.0) * s);
                g.strokeOval(cx - radius, feet - radius * 0.16, radius * 2.0, radius * 0.32);
                g.setFill(Color.web("#FFF0BE", 0.28 + windup * 0.42));
                double arrowTop = feet - (82.0 - windup * 28.0) * s;
                g.fillPolygon(
                        new double[]{cx - 13.0 * s, cx + 13.0 * s, cx},
                        new double[]{arrowTop, arrowTop, arrowTop + 24.0 * s}, 3);
            }
        }
        if (bird.kiwiStompImpactFxTimer > 0) {
            double age = Bird.KIWI_STOMP_IMPACT_FX_FRAMES - bird.kiwiStompImpactFxTimer;
            double life = bird.kiwiStompImpactFxTimer / (double) Bird.KIWI_STOMP_IMPACT_FX_FRAMES;
            double radius = Math.min(168.0, 34.0 + age * 8.0) * s;
            g.setStroke(Color.web("#F0C77C", 0.16 + life * 0.72));
            g.setLineWidth((2.0 + life * 5.0) * s);
            g.strokeOval(cx - radius, feet - radius * 0.22, radius * 2.0, radius * 0.44);
            double innerRadius = radius * 0.62;
            g.setStroke(Color.web("#FFF0B5", 0.10 + life * 0.46));
            g.setLineWidth((1.0 + life * 3.0) * s);
            g.strokeOval(cx - innerRadius, feet - innerRadius * 0.18,
                    innerRadius * 2.0, innerRadius * 0.36);
            g.setStroke(Color.web("#8B5D36", 0.18 + life * 0.58));
            g.setLineWidth((1.5 + life * 2.0) * s);
            for (int i = -3; i <= 3; i++) {
                if (i == 0) continue;
                double direction = Math.signum(i);
                double crackStart = (18.0 + Math.abs(i) * 7.0) * s;
                double crackEnd = (54.0 + age * 3.2 + Math.abs(i) * 9.0) * s;
                g.strokeLine(cx + direction * crackStart, feet,
                        cx + direction * crackEnd, feet + (Math.abs(i) % 2 == 0 ? 7.0 : -5.0) * s);
            }
        }
        if (bird.kiwiUltimateTimer > 0) {
            double pulse = 0.5 + 0.5 * Math.sin(bird.kiwiUltimateTimer * 0.34);
            g.setStroke(Color.web("#E6C378", 0.38 + pulse * 0.28));
            g.setLineWidth((5.0 + pulse * 3.0) * s);
            double radius = (72.0 + pulse * 24.0) * s;
            g.strokeOval(cx - radius, feet - radius * 0.34, radius * 2.0, radius * 0.68);
        }
    }

    static boolean bodyBurrowed(Bird bird) {
        return bird.kiwiBurrowTimer > 0 && bird.kiwiBurrowGrounded && !bird.kiwiBurrowErupted;
    }

    static void reset(Bird bird) {
        bird.kiwiProbeTimer = 0;
        bird.kiwiProbeReuseTimer = 0;
        bird.kiwiProbeStrikeIndex = 0;
        Arrays.fill(bird.kiwiProbeHit, false);
        bird.kiwiBurrowTimer = 0;
        bird.kiwiBurrowReuseTimer = 0;
        bird.kiwiBurrowGrounded = false;
        bird.kiwiBurrowErupted = false;
        Arrays.fill(bird.kiwiBurrowHit, false);
        bird.kiwiSpringTimer = 0;
        bird.kiwiSpringReuseTimer = 0;
        bird.kiwiSpringUsed = false;
        Arrays.fill(bird.kiwiSpringHit, false);
        bird.kiwiStompTimer = 0;
        bird.kiwiStompReuseTimer = 0;
        bird.kiwiStompAirborne = false;
        bird.kiwiStompImpactResolved = false;
        bird.kiwiStompImpactFxTimer = 0;
        Arrays.fill(bird.kiwiStompHit, false);
        bird.kiwiUltimateTimer = 0;
        bird.kiwiUltimateWaveIndex = 0;
        bird.kiwiUltimateFinalResolved = false;
        Arrays.fill(bird.kiwiUltimateHitCooldown, 0);
    }

    private static int directionFor(Bird bird) {
        int direction = bird.horizontalInputDirection();
        return direction == 0 ? bird.facingDirection() : direction;
    }

    private static void clearSharedCooldown(Bird bird) {
        bird.specialCooldown = 0;
        bird.specialMaxCooldown = 0;
    }

    private static boolean alreadyHit(boolean[] hits, Bird target) {
        int index = target.playerIndex;
        return index >= 0 && index < hits.length && hits[index];
    }

    private static void markHit(boolean[] hits, Bird target) {
        int index = target.playerIndex;
        if (index >= 0 && index < hits.length) {
            hits[index] = true;
        }
    }

    private static boolean onHitCooldown(int[] cooldowns, Bird target) {
        int index = target.playerIndex;
        return index >= 0 && index < cooldowns.length && cooldowns[index] > 0;
    }

    private static void setHitCooldown(int[] cooldowns, Bird target, int frames) {
        int index = target.playerIndex;
        if (index >= 0 && index < cooldowns.length) {
            cooldowns[index] = frames;
        }
    }

    private static void impact(Bird bird, Bird target, int damage, boolean finisher, String moveName) {
        bird.game.playHitSound(damage);
        bird.game.emitCombatImpact(bird, target, target.bodyCenterX(), target.bodyCenterY(),
                target.vx, target.vy, damage, finisher, moveName);
    }

    private static void emitDirt(Bird bird, double x, double y, int requested, double speedScale) {
        int count = bird.scaledParticleCount(requested);
        Color[] palette = {Color.web("#5D4037"), Color.web("#8D6E63"), Color.web("#D7B27A")};
        for (int i = 0; i < count; i++) {
            double angle = Math.PI + bird.game.nextParticleRandom() * Math.PI;
            double speed = 1.2 + bird.game.nextParticleRandom() * speedScale;
            Color color = palette[(int) (bird.game.nextParticleRandom() * palette.length) % palette.length];
            bird.game.particles.add(new Particle(
                    x + (bird.game.nextParticleRandom() - 0.5) * 54.0 * bird.sizeMultiplier,
                    y - bird.game.nextParticleRandom() * 16.0 * bird.sizeMultiplier,
                    Math.cos(angle) * speed,
                    Math.sin(angle) * speed - 1.4,
                    color.deriveColor(0.0, 1.0, 1.0, 0.82)
            ));
        }
    }
}
