package com.example.birdgame3;

import javafx.scene.paint.Color;

import java.util.Arrays;

final class RaptorSpecials {
    private RaptorSpecials() {
    }

    static final String SKY_SOVEREIGN_MOVE = "Eagle Sky Sovereign";
    static final String TERMINAL_VELOCITY_MOVE = "Falcon Terminal Velocity";

    static void use(Bird bird, boolean ultimate) {
        switch (bird.selectRaptorSpecialVariant()) {
            case NEUTRAL -> neutral(bird, ultimate);
            case SIDE -> side(bird, ultimate);
            case UP -> up(bird, ultimate);
            case DOWN -> {
                if (bird.type == BirdGame3.BirdType.EAGLE) {
                    eagleDive(bird, ultimate);
                } else {
                    falconDive(bird, ultimate);
                }
            }
        }
    }

    static void neutral(Bird bird, boolean ultimate) {
        boolean eagle = bird.type == BirdGame3.BirdType.EAGLE;
        int dir = bird.horizontalInputDirection();
        if (dir != 0) {
            bird.facingRight = dir > 0;
        }
        dir = bird.facingDirection();

        bird.raptorCryUltimate = ultimate;
        bird.raptorCryTimer = eagle
                ? (ultimate ? Bird.EAGLE_CRY_ULTIMATE_FRAMES : Bird.EAGLE_CRY_FRAMES)
                : (ultimate ? Bird.FALCON_CRY_ULTIMATE_FRAMES : Bird.FALCON_CRY_FRAMES);
        bird.raptorCryReuseTimer = cryReuseFrames(bird, ultimate);
        bird.attackAnimationTimer = Math.max(bird.attackAnimationTimer, bird.raptorCryTimer);
        bird.vx *= eagle ? 0.36 : 0.52;
        if (!bird.isOnGround()) {
            bird.vy = Math.min(bird.vy, eagle ? 1.4 : 0.9);
        }

        double centerX = bird.bodyCenterX() + dir * bird.bodyWidth() * 0.55;
        double centerY = bird.bodyCenterY() - 8.0 * bird.sizeMultiplier;
        for (Bird other : bird.game.players) {
            if (!bird.canDamageTarget(other)) continue;

            double dx = other.bodyCenterX() - centerX;
            double forward = dx * dir;
            if (forward < -other.combatHalfWidth() * 0.2) continue;

            double dy = other.bodyCenterY() - centerY;
            double reach = eagle ? (ultimate ? 170.0 : 152.0) : (ultimate ? 160.0 : 146.0);
            if (forward > reach + other.combatHalfWidth()) continue;

            double verticalAllowance = eagle
                    ? 46.0 + Math.max(0.0, forward) * 0.28
                    : 24.0 + Math.max(0.0, forward) * 0.16;
            if (Math.abs(dy) > verticalAllowance * bird.sizeMultiplier + other.combatHalfHeight()) continue;

            boolean sweetspot = !eagle && forward > 92.0 * bird.sizeMultiplier;
            int dmg = eagle
                    ? (ultimate ? 10 : 8)
                    : (sweetspot ? (ultimate ? 10 : 8) : (ultimate ? 7 : 5));
            double oldHealth = other.health;
            int dealt = (int) bird.applyDamageTo(other, dmg);
            if (dealt <= 0) continue;

            bird.game.damageDealt[bird.playerIndex] += dealt;
            bird.game.recordSpecialImpact(bird.playerIndex, dealt, true);
            if (other.health <= 0 && oldHealth > 0) {
                bird.game.eliminations[bird.playerIndex]++;
            }

            other.vx += dir * (sweetspot ? 8.4 : eagle ? 6.5 : 5.2);
            other.vy -= sweetspot ? 6.4 : eagle ? 4.8 : 4.0;
        }

        Color primary = eagle ? Color.web("#E3B74E") : Color.web("#FF9E57");
        Color secondary = eagle ? Color.web("#FFF4BC") : Color.web("#FFE0A5");
        for (int ring = 0; ring < 3; ring++) {
            double ringReach = 18 + ring * 28;
            for (int i = 0; i < 8; i++) {
                double spread = (i - 3.5) * (eagle ? 0.12 : 0.08);
                bird.game.particles.add(new Particle(
                        centerX + dir * (ringReach + i * 6),
                        centerY + spread * 26,
                        dir * (2.8 + ring * 1.2 + i * 0.2),
                        spread * (eagle ? 2.3 : 1.5),
                        (ring & 1) == 0 ? primary.deriveColor(0, 1, 1, 0.82) : secondary.deriveColor(0, 1, 1, 0.72)
                ));
            }
        }
    }

    static void side(Bird bird, boolean ultimate) {
        boolean eagle = bird.type == BirdGame3.BirdType.EAGLE;
        int dir = bird.horizontalInputDirection();
        if (dir == 0) {
            dir = bird.facingDirection();
        }
        bird.facingRight = dir > 0;
        bird.raptorRushDirection = dir;
        bird.raptorRushGrounded = bird.isOnGround();
        bird.raptorRushUltimate = ultimate;
        bird.raptorRushTimer = eagle
                ? (bird.raptorRushGrounded ? Bird.EAGLE_RUSH_GROUND_FRAMES : Bird.EAGLE_RUSH_AIR_FRAMES)
                : (bird.raptorRushGrounded ? Bird.FALCON_RUSH_GROUND_FRAMES : Bird.FALCON_RUSH_AIR_FRAMES);
        if (ultimate) {
            bird.raptorRushTimer += eagle ? 2 : 1;
        }
        Arrays.fill(bird.raptorRushHit, false);
        bird.raptorRushReuseTimer = rushReuseFrames(bird, ultimate);
        bird.attackAnimationTimer = Math.max(bird.attackAnimationTimer, bird.raptorRushTimer);
        bird.vx = dir * rushSpeed(bird);
        if (bird.raptorRushGrounded) {
            bird.vy = Math.min(bird.vy, 0.0);
        } else {
            bird.vy = Math.min(bird.vy, eagle ? 1.0 : 0.4);
        }
        bird.isBlocking = false;
        bird.parryWindowFrames = 0;
        bird.shieldStunFrames = 0;
    }

    static void up(Bird bird, boolean ultimate) {
        if (bird.raptorUpSpecialUsed) {
            return;
        }
        boolean eagle = bird.type == BirdGame3.BirdType.EAGLE;
        int dir = bird.horizontalInputDirection();
        if (dir != 0) {
            bird.facingRight = dir > 0;
        } else {
            dir = bird.facingDirection();
        }
        bird.raptorClimbDirection = dir;
        bird.raptorUpSpecialUsed = true;
        bird.raptorClimbUltimate = ultimate;
        bird.raptorClimbTimer = eagle
                ? (ultimate ? Bird.EAGLE_CLIMB_ULTIMATE_FRAMES : Bird.EAGLE_CLIMB_FRAMES)
                : (ultimate ? Bird.FALCON_CLIMB_ULTIMATE_FRAMES : Bird.FALCON_CLIMB_FRAMES);
        Arrays.fill(bird.raptorClimbHit, false);
        bird.attackAnimationTimer = Math.max(bird.attackAnimationTimer, bird.raptorClimbTimer);
        bird.canDoubleJump = false;
        bird.vx = dir * (eagle ? (ultimate ? 3.8 : 3.1) : (ultimate ? 6.3 : 5.5));
        bird.vy = eagle ? (ultimate ? -17.4 : -14.4) : (ultimate ? -16.2 : -14.2);
        bird.isBlocking = false;
        bird.parryWindowFrames = 0;
        bird.shieldStunFrames = 0;
    }

    static void eagleDive(Bird bird, boolean ultimate) {
        boolean grounded = bird.isOnGround();
        bird.diveTimer = ultimate ? Bird.EAGLE_DIVE_ULTIMATE_FRAMES : Bird.EAGLE_DIVE_FRAMES;
        bird.specialCooldown = bird.diveTimer;
        bird.specialMaxCooldown = bird.specialCooldown;
        bird.eagleDiveActive = true;
        bird.eagleAscentActive = false;
        bird.eagleAscentFrames = 0;
        Arrays.fill(bird.eagleAscentHit, false);

        bird.game.shakeIntensity = Math.max(bird.game.shakeIntensity, ultimate ? 20 : 16);
        bird.game.hitstopFrames = Math.max(bird.game.hitstopFrames, ultimate ? 11 : 9);
        bird.game.addToKillFeed("SKREEEEEEEE!!! " + bird.shortName() + (ultimate ? " ULT DIVES FROM THE HEAVENS!" : " IS DIVING FROM THE HEAVENS!"));

        int trailCount = bird.scaledParticleCount(ultimate ? 140 : 100);
        for (int i = 0; i < trailCount; i++) {
            double angle = Math.atan2(bird.vy, bird.vx) + Math.PI;
            double dist = i * 10;
            bird.game.particles.add(new Particle(
                    bird.x + 40 + Math.cos(angle) * dist,
                    bird.y + 40 + Math.sin(angle) * dist,
                    0, 0,
                    Color.CRIMSON.deriveColor(0, 1, 1, 1.0 - i / (double) trailCount)
            ));
        }

        double predictX = bird.x + bird.vx * 40;
        int warningCount = bird.scaledParticleCount(31);
        for (int i = 0; i < warningCount; i++) {
            double progress = warningCount == 1 ? 0.0 : (i / (double) (warningCount - 1));
            double laneOffset = -15.0 + progress * 30.0;
            bird.game.particles.add(new Particle(predictX + laneOffset * 60.0, BirdGame3.GROUND_Y - 20, 0, -5 - bird.game.nextParticleRandom() * 8, Color.ORANGERED.brighter()));
        }

        if (grounded) {
            bird.vy = ultimate ? -12 : -8;
            bird.vx *= ultimate ? 0.45 : 0.35;
            bird.eagleDiveCountdown = ultimate ? Bird.EAGLE_DIVE_GROUND_ULTIMATE_STARTUP_FRAMES : Bird.EAGLE_DIVE_GROUND_STARTUP_FRAMES;
        } else {
            bird.vy = Math.max(bird.vy, ultimate ? 18 : 14);
            bird.vx *= ultimate ? 0.82 : 0.7;
            bird.eagleDiveCountdown = 0;
        }
        bird.attackAnimationTimer = Math.max(bird.attackAnimationTimer, 16);
        bird.isBlocking = false;
        bird.parryWindowFrames = 0;
        bird.shieldStunFrames = 0;
    }

    static void falconDive(Bird bird, boolean ultimate) {
        boolean grounded = bird.isOnGround();
        bird.diveTimer = ultimate ? Bird.FALCON_DIVE_ULTIMATE_FRAMES : Bird.FALCON_DIVE_FRAMES;
        bird.specialCooldown = bird.diveTimer;
        bird.specialMaxCooldown = bird.specialCooldown;
        bird.eagleDiveActive = true;
        bird.eagleAscentActive = false;
        bird.eagleAscentFrames = 0;
        Arrays.fill(bird.eagleAscentHit, false);

        bird.game.shakeIntensity = Math.max(bird.game.shakeIntensity, ultimate ? 16 : 12);
        bird.game.hitstopFrames = Math.max(bird.game.hitstopFrames, ultimate ? 9 : 7);
        bird.game.addToKillFeed(bird.shortName() + (ultimate ? " ULT FALCON DIVE ENGAGED!" : " LOCKED IN A FALCON DIVE!"));

        int trailCount = bird.scaledParticleCount(ultimate ? 110 : 78);
        for (int i = 0; i < trailCount; i++) {
            double angle = Math.atan2(bird.vy, bird.vx) + Math.PI;
            double dist = i * 7.5;
            Color c = i % 2 == 0 ? Color.web("#FF7043") : Color.web("#FFE082");
            bird.game.particles.add(new Particle(
                    bird.x + 40 + Math.cos(angle) * dist,
                    bird.y + 40 + Math.sin(angle) * dist,
                    0, 0,
                    c.deriveColor(0, 1, 1, 1.0 - i / (double) trailCount)
            ));
        }

        if (grounded) {
            bird.vy = ultimate ? -11 : -8;
            bird.vx *= ultimate ? 0.55 : 0.45;
            bird.eagleDiveCountdown = ultimate ? Bird.FALCON_DIVE_GROUND_ULTIMATE_STARTUP_FRAMES : Bird.FALCON_DIVE_GROUND_STARTUP_FRAMES;
        } else {
            bird.vy = Math.max(bird.vy, ultimate ? 17 : 13);
            bird.vx += (bird.facingRight ? 1 : -1) * (ultimate ? 12 : 8);
            bird.eagleDiveCountdown = 0;
        }
        bird.attackAnimationTimer = Math.max(bird.attackAnimationTimer, 14);
        bird.isBlocking = false;
        bird.parryWindowFrames = 0;
        bird.shieldStunFrames = 0;
    }

    static void handleState(Bird bird) {
        if (!bird.isRaptor() && !bird.mockingbirdCopiedRaptorNeutral()) {
            return;
        }
        if (bird.stunTime > 0.0) {
            reset(bird);
            if (bird.mockingbirdCopiedRaptorNeutral()) {
                bird.mockingbirdCopiedNeutralSource = null;
            }
            return;
        }
        if (bird.eagleSkySovereignActive) {
            handleSkySovereign(bird);
            return;
        }
        if (bird.falconTerminalVelocityActive) {
            handleTerminalVelocity(bird);
            return;
        }
        if (bird.raptorCryTimer > 0) {
            handleCry(bird);
        }
        if (bird.raptorRushTimer > 0) {
            handleRush(bird);
        }
        if (bird.raptorClimbTimer > 0) {
            handleClimb(bird);
        }
    }

    static boolean active(Bird bird) {
        return bird.raptorCryTimer > 0
                || bird.raptorRushTimer > 0
                || bird.raptorClimbTimer > 0
                || bird.eagleDiveActive
                || bird.eagleAscentActive
                || bird.eagleSkySovereignActive
                || bird.falconTerminalVelocityActive;
    }

    static boolean ready(Bird bird, Bird.RaptorSpecialVariant variant) {
        return switch (variant) {
            case NEUTRAL -> bird.raptorCryReuseTimer <= 0;
            case SIDE -> bird.raptorRushReuseTimer <= 0;
            case UP -> !bird.raptorUpSpecialUsed;
            case DOWN -> bird.specialCooldown <= 0;
        };
    }

    static boolean onReuseLockout(Bird bird, Bird.RaptorSpecialVariant variant) {
        return switch (variant) {
            case NEUTRAL -> bird.raptorCryReuseTimer > 0;
            case SIDE -> bird.raptorRushReuseTimer > 0;
            case UP -> bird.raptorUpSpecialUsed;
            case DOWN -> bird.specialCooldown > 0;
        };
    }

    static void reset(Bird bird) {
        bird.raptorCryTimer = 0;
        bird.raptorCryUltimate = false;
        bird.raptorRushTimer = 0;
        bird.raptorRushUltimate = false;
        bird.raptorRushGrounded = false;
        bird.raptorRushDirection = 1;
        Arrays.fill(bird.raptorRushHit, false);
        bird.raptorClimbTimer = 0;
        bird.raptorClimbUltimate = false;
        bird.raptorClimbDirection = 1;
        Arrays.fill(bird.raptorClimbHit, false);
        bird.eagleDiveActive = false;
        bird.eagleAscentActive = false;
        bird.eagleAscentFrames = 0;
        Arrays.fill(bird.eagleAscentHit, false);
        bird.eagleDiveCountdown = 0;
        bird.diveTimer = 0;
        resetSkySovereign(bird);
        resetTerminalVelocity(bird);
    }

    static void startSkySovereign(Bird bird) {
        reset(bird);
        bird.eagleSkySovereignActive = true;
        bird.eagleSkySovereignDiving = false;
        bird.eagleSkySovereignTimer = Bird.EAGLE_SKY_SOVEREIGN_TARGET_FRAMES;
        bird.eagleSkySovereignHitResolved = false;
        Arrays.fill(bird.eagleSkySovereignHit, false);

        Bird target = nearestSkySovereignTarget(bird);
        bird.eagleSkySovereignTargetX = target == null ? bird.bodyCenterX() : target.bodyCenterX();
        bird.eagleSkySovereignTargetY = target == null
                ? Math.min(BirdGame3.GROUND_Y - 40.0, bird.bodyCenterY() + 180.0)
                : target.bodyCenterY();
        clampSkySovereignTarget(bird);
        bird.eagleSkySovereignDiveStartY = Math.max(45.0, bird.eagleSkySovereignTargetY - 560.0);

        bird.specialCooldown = 0;
        bird.specialMaxCooldown = 0;
        bird.attackAnimationTimer = Math.max(bird.attackAnimationTimer, 28);
        bird.isBlocking = false;
        bird.parryWindowFrames = 0;
        bird.shieldStunFrames = 0;
        bird.blockCooldown = 0;
        bird.vx = 0.0;
        bird.vy = -18.0;
        bird.x = bird.eagleSkySovereignTargetX - bird.bodyWidth() * 0.5;
        bird.y = Math.max(45.0, bird.eagleSkySovereignTargetY - 360.0);
        bird.canDoubleJump = true;
        bird.game.addToKillFeed(bird.shortName() + " claimed the sky!");

        for (int i = 0; i < bird.scaledParticleCount(58); i++) {
            double angle = -Math.PI / 2.0 + (bird.game.nextParticleRandom() - 0.5) * 1.9;
            double speed = 4.0 + bird.game.nextParticleRandom() * 10.0;
            bird.game.particles.add(new Particle(
                    bird.bodyCenterX() + (bird.game.nextParticleRandom() - 0.5) * bird.bodyWidth(),
                    bird.bodyCenterY() + (bird.game.nextParticleRandom() - 0.5) * bird.bodyHeight() * 0.4,
                    Math.cos(angle) * speed,
                    Math.sin(angle) * speed,
                    Color.GOLD.deriveColor(0, 1, 1, 0.9)
            ));
        }
    }

    static void resetSkySovereign(Bird bird) {
        bird.eagleSkySovereignActive = false;
        bird.eagleSkySovereignDiving = false;
        bird.eagleSkySovereignTimer = 0;
        bird.eagleSkySovereignTargetX = 0.0;
        bird.eagleSkySovereignTargetY = 0.0;
        bird.eagleSkySovereignDiveStartY = 0.0;
        bird.eagleSkySovereignHitResolved = false;
        Arrays.fill(bird.eagleSkySovereignHit, false);
    }

    static void startTerminalVelocity(Bird bird) {
        reset(bird);
        int dir = bird.horizontalInputDirection();
        if (dir == 0) {
            dir = bird.facingDirection();
        }
        bird.facingRight = dir > 0;
        Bird target = selectTerminalVelocityTarget(bird, dir);
        double targetX = target == null ? bird.bodyCenterX() + dir * 360.0 : target.bodyCenterX();
        double targetY = target == null ? bird.bodyCenterY() + 70.0 : target.bodyCenterY();

        bird.falconTerminalVelocityActive = true;
        bird.falconTerminalVelocityStriking = false;
        bird.falconTerminalVelocityHitAny = false;
        bird.falconTerminalVelocityTimer = Bird.FALCON_TERMINAL_VELOCITY_WARNING_FRAMES;
        bird.falconTerminalVelocityDirection = dir;
        bird.falconTerminalVelocityStartX = Math.clamp(targetX - dir * 350.0, -220.0, BirdGame3.WORLD_WIDTH + 220.0);
        bird.falconTerminalVelocityStartY = Math.clamp(targetY - 285.0, 55.0, BirdGame3.WORLD_HEIGHT - 170.0);
        bird.falconTerminalVelocityEndX = Math.clamp(targetX + dir * 350.0, -220.0, BirdGame3.WORLD_WIDTH + 220.0);
        bird.falconTerminalVelocityEndY = Math.clamp(targetY + 285.0, 95.0, BirdGame3.WORLD_HEIGHT - 70.0);
        Arrays.fill(bird.falconTerminalVelocityHit, false);

        bird.specialCooldown = 0;
        bird.specialMaxCooldown = 0;
        bird.attackAnimationTimer = Math.max(bird.attackAnimationTimer, Bird.FALCON_TERMINAL_VELOCITY_WARNING_FRAMES);
        bird.isBlocking = false;
        bird.parryWindowFrames = 0;
        bird.shieldStunFrames = 0;
        bird.blockCooldown = 0;
        bird.x = bird.falconTerminalVelocityStartX - bird.bodyWidth() * 0.5;
        bird.y = bird.falconTerminalVelocityStartY - bird.bodyHeight() * 0.5;
        bird.vx = 0.0;
        bird.vy = 0.0;
        bird.canDoubleJump = true;
        bird.game.hitstopFrames = Math.max(bird.game.hitstopFrames, 10);
        bird.game.shakeIntensity = Math.max(bird.game.shakeIntensity, 10.0);
        bird.game.addToKillFeed(bird.shortName() + " broke terminal velocity!");

        for (int i = 0; i < bird.scaledParticleCount(42); i++) {
            double angle = bird.game.nextParticleRandom() * Math.PI * 2.0;
            double speed = 3.0 + bird.game.nextParticleRandom() * 8.0;
            bird.game.particles.add(new Particle(
                    bird.bodyCenterX() + Math.cos(angle) * (10.0 + bird.game.nextParticleRandom() * 30.0),
                    bird.bodyCenterY() + Math.sin(angle) * (10.0 + bird.game.nextParticleRandom() * 30.0),
                    Math.cos(angle) * speed,
                    Math.sin(angle) * speed - 1.2,
                    Color.web("#FFCC80").deriveColor(0, 1, 1, 0.86)
            ));
        }
    }

    static void resetTerminalVelocity(Bird bird) {
        bird.falconTerminalVelocityActive = false;
        bird.falconTerminalVelocityStriking = false;
        bird.falconTerminalVelocityHitAny = false;
        bird.falconTerminalVelocityTimer = 0;
        bird.falconTerminalVelocityDirection = 1;
        bird.falconTerminalVelocityStartX = 0.0;
        bird.falconTerminalVelocityStartY = 0.0;
        bird.falconTerminalVelocityEndX = 0.0;
        bird.falconTerminalVelocityEndY = 0.0;
        Arrays.fill(bird.falconTerminalVelocityHit, false);
    }

    private static void handleSkySovereign(Bird bird) {
        if (bird.health <= 0 || bird.type != BirdGame3.BirdType.EAGLE) {
            resetSkySovereign(bird);
            return;
        }
        if (!bird.eagleSkySovereignDiving) {
            handleSkySovereignTargeting(bird);
            return;
        }
        handleSkySovereignDive(bird);
    }

    private static void handleSkySovereignTargeting(Bird bird) {
        if (bird.game.isAI[bird.playerIndex]) {
            Bird target = nearestSkySovereignTarget(bird);
            if (target != null) {
                bird.eagleSkySovereignTargetX = moveToward(
                        bird.eagleSkySovereignTargetX,
                        target.bodyCenterX(),
                        Bird.EAGLE_SKY_SOVEREIGN_TARGET_SPEED_X
                );
                bird.eagleSkySovereignTargetY = moveToward(
                        bird.eagleSkySovereignTargetY,
                        target.bodyCenterY(),
                        Bird.EAGLE_SKY_SOVEREIGN_TARGET_SPEED_Y
                );
            }
        } else {
            bird.eagleSkySovereignTargetX += bird.horizontalInputDirection() * Bird.EAGLE_SKY_SOVEREIGN_TARGET_SPEED_X;
            int verticalDir = (bird.jumpPressed() ? -1 : 0) + (bird.blockPressed() ? 1 : 0);
            bird.eagleSkySovereignTargetY += verticalDir * Bird.EAGLE_SKY_SOVEREIGN_TARGET_SPEED_Y;
        }
        clampSkySovereignTarget(bird);

        bird.x = bird.eagleSkySovereignTargetX - bird.bodyWidth() * 0.5;
        bird.y = Math.max(45.0, bird.eagleSkySovereignTargetY - 360.0);
        bird.vx = 0.0;
        bird.vy = 0.0;
        bird.canDoubleJump = true;
        bird.attackAnimationTimer = Math.max(bird.attackAnimationTimer, 8);

        if ((bird.eagleSkySovereignTimer & 7) == 0) {
            double angle = bird.game.nextParticleRandom() * Math.PI * 2.0;
            double dist = 32.0 + bird.game.nextParticleRandom() * Bird.EAGLE_SKY_SOVEREIGN_RADIUS * 0.9;
            bird.game.particles.add(new Particle(
                    bird.eagleSkySovereignTargetX + Math.cos(angle) * dist,
                    bird.eagleSkySovereignTargetY + Math.sin(angle) * dist * 0.62,
                    Math.cos(angle) * 0.4,
                    -1.4 - bird.game.nextParticleRandom() * 1.2,
                    Color.web("#FFF59D").deriveColor(0, 1, 1, 0.58)
            ));
        }

        int elapsed = Bird.EAGLE_SKY_SOVEREIGN_TARGET_FRAMES - bird.eagleSkySovereignTimer;
        boolean releaseRequested = elapsed >= Bird.EAGLE_SKY_SOVEREIGN_RELEASE_LOCK_FRAMES && !bird.specialHeld();
        if (bird.eagleSkySovereignTimer <= 0 || releaseRequested) {
            beginSkySovereignDive(bird);
        }
    }

    private static void beginSkySovereignDive(Bird bird) {
        bird.eagleSkySovereignDiving = true;
        bird.eagleSkySovereignTimer = Bird.EAGLE_SKY_SOVEREIGN_DIVE_FRAMES;
        bird.eagleSkySovereignDiveStartY = Math.max(45.0, bird.eagleSkySovereignTargetY - 620.0);
        bird.x = bird.eagleSkySovereignTargetX - bird.bodyWidth() * 0.5;
        bird.y = bird.eagleSkySovereignDiveStartY;
        bird.vx = 0.0;
        bird.vy = 0.0;
        bird.attackAnimationTimer = Math.max(bird.attackAnimationTimer, Bird.EAGLE_SKY_SOVEREIGN_DIVE_FRAMES);
        bird.game.shakeIntensity = Math.max(bird.game.shakeIntensity, 16.0);
        bird.game.hitstopFrames = Math.max(bird.game.hitstopFrames, 5);

        for (int i = 0; i < bird.scaledParticleCount(36); i++) {
            double spread = (bird.game.nextParticleRandom() - 0.5) * 95.0;
            bird.game.particles.add(new Particle(
                    bird.eagleSkySovereignTargetX + spread,
                    bird.eagleSkySovereignTargetY - 210.0 - bird.game.nextParticleRandom() * 80.0,
                    -spread * 0.018,
                    6.0 + bird.game.nextParticleRandom() * 8.0,
                    Color.GOLD.deriveColor(0, 1, 1, 0.82)
            ));
        }
    }

    private static void handleSkySovereignDive(Bird bird) {
        if (bird.eagleSkySovereignTimer <= 0) {
            resolveSkySovereignImpact(bird);
            return;
        }

        double progress = 1.0 - Math.clamp(
                bird.eagleSkySovereignTimer / (double) Bird.EAGLE_SKY_SOVEREIGN_DIVE_FRAMES,
                0.0,
                1.0
        );
        double eased = progress * progress * (3.0 - 2.0 * progress);
        double targetY = bird.eagleSkySovereignTargetY - bird.bodyHeight() * 0.55;
        bird.x = bird.eagleSkySovereignTargetX - bird.bodyWidth() * 0.5;
        bird.y = bird.eagleSkySovereignDiveStartY + (targetY - bird.eagleSkySovereignDiveStartY) * eased;
        bird.vx = 0.0;
        bird.vy = 0.0;
        bird.attackAnimationTimer = Math.max(bird.attackAnimationTimer, 3);

        if ((bird.eagleSkySovereignTimer & 1) == 0) {
            bird.game.particles.add(new Particle(
                    bird.bodyCenterX() + (bird.game.nextParticleRandom() - 0.5) * 32.0,
                    bird.bodyCenterY() - 24.0,
                    (bird.game.nextParticleRandom() - 0.5) * 3.0,
                    -3.5 - bird.game.nextParticleRandom() * 3.0,
                    Color.web("#FFFDE7").deriveColor(0, 1, 1, 0.76)
            ));
        }
    }

    private static void resolveSkySovereignImpact(Bird bird) {
        if (bird.eagleSkySovereignHitResolved) {
            resetSkySovereign(bird);
            return;
        }
        bird.eagleSkySovereignHitResolved = true;
        bird.x = bird.eagleSkySovereignTargetX - bird.bodyWidth() * 0.5;
        bird.y = bird.eagleSkySovereignTargetY - bird.bodyHeight() * 0.62;
        bird.vx = 0.0;
        bird.vy = -8.0;
        bird.canDoubleJump = true;

        boolean hitAny = false;
        for (Bird other : bird.game.players) {
            if (!bird.canDamageTarget(other)) {
                continue;
            }
            int targetIndex = other.playerIndex;
            if (targetIndex < 0 || targetIndex >= bird.eagleSkySovereignHit.length) {
                continue;
            }
            if (bird.eagleSkySovereignHit[targetIndex]) {
                continue;
            }
            double dx = other.bodyCenterX() - bird.eagleSkySovereignTargetX;
            double dy = other.bodyCenterY() - bird.eagleSkySovereignTargetY;
            double distance = Math.hypot(dx, dy);
            if (distance > Bird.EAGLE_SKY_SOVEREIGN_RADIUS + other.combatRadius()) {
                continue;
            }
            bird.eagleSkySovereignHit[targetIndex] = true;
            boolean sweetspot = distance <= Bird.EAGLE_SKY_SOVEREIGN_SWEETSPOT_RADIUS + other.combatRadius() * 0.35;
            int damage = sweetspot
                    ? Bird.EAGLE_SKY_SOVEREIGN_SWEETSPOT_DAMAGE
                    : Bird.EAGLE_SKY_SOVEREIGN_DAMAGE;
            int dealt = bird.applyTrackedSpecialDamage(other, damage);
            if (dealt <= 0) {
                continue;
            }
            hitAny = true;

            double launchDir = Math.abs(dx) < 0.001 ? bird.facingDirection() : Math.signum(dx);
            double safeDistance = Math.max(1.0, distance);
            other.vx += launchDir * (sweetspot ? 14.0 : 9.0)
                    + dx / safeDistance * (sweetspot ? 3.0 : 1.6);
            other.vy -= sweetspot ? 16.0 : 10.5;
            if (sweetspot) {
                bird.game.addToKillFeed(bird.shortName() + " seized " + other.shortName() + " in the talons!");
            }
            emitSkySovereignImpactParticles(bird, other.bodyCenterX(), other.bodyCenterY(),
                    sweetspot ? 34 : 20, sweetspot ? Color.GOLD : Color.web("#FFF59D"));
        }

        bird.game.shakeIntensity = Math.max(bird.game.shakeIntensity, hitAny ? 34.0 : 18.0);
        bird.game.hitstopFrames = Math.max(bird.game.hitstopFrames, hitAny ? 12 : 6);
        bird.game.triggerFlash(hitAny ? 0.46 : 0.24, false);
        if (!hitAny) {
            bird.applySpecialLandingLag(Bird.EAGLE_SKY_SOVEREIGN_MISS_LANDING_LAG_FRAMES);
        }
        emitSkySovereignImpactParticles(bird, bird.eagleSkySovereignTargetX,
                bird.eagleSkySovereignTargetY, hitAny ? 84 : 48, Color.GOLD);
        resetSkySovereign(bird);
    }

    private static Bird nearestSkySovereignTarget(Bird bird) {
        Bird best = null;
        double bestDistance = Double.POSITIVE_INFINITY;
        for (Bird other : bird.game.players) {
            if (!bird.canDamageTarget(other)) {
                continue;
            }
            double distance = Math.hypot(other.bodyCenterX() - bird.bodyCenterX(),
                    other.bodyCenterY() - bird.bodyCenterY());
            if (distance < bestDistance) {
                best = other;
                bestDistance = distance;
            }
        }
        return best;
    }

    private static double moveToward(double value, double target, double maxDelta) {
        double delta = target - value;
        if (Math.abs(delta) <= maxDelta) {
            return target;
        }
        return value + Math.signum(delta) * maxDelta;
    }

    private static void clampSkySovereignTarget(Bird bird) {
        bird.eagleSkySovereignTargetX = Math.clamp(bird.eagleSkySovereignTargetX, 90.0, BirdGame3.WORLD_WIDTH - 90.0);
        bird.eagleSkySovereignTargetY = Math.clamp(bird.eagleSkySovereignTargetY, 120.0, BirdGame3.WORLD_HEIGHT - 140.0);
    }

    private static void emitSkySovereignImpactParticles(Bird bird, double centerX, double centerY,
                                                        int requested, Color color) {
        for (int i = 0; i < bird.scaledParticleCount(requested); i++) {
            double angle = bird.game.nextParticleRandom() * Math.PI * 2.0;
            double speed = 4.0 + bird.game.nextParticleRandom() * 12.0;
            bird.game.particles.add(new Particle(
                    centerX + Math.cos(angle) * (8.0 + bird.game.nextParticleRandom() * 28.0),
                    centerY + Math.sin(angle) * (8.0 + bird.game.nextParticleRandom() * 28.0),
                    Math.cos(angle) * speed,
                    Math.sin(angle) * speed - 3.2,
                    color.deriveColor(0, 1, 1, 0.82)
            ));
        }
    }

    private static void handleTerminalVelocity(Bird bird) {
        if (bird.health <= 0 || bird.type != BirdGame3.BirdType.FALCON) {
            resetTerminalVelocity(bird);
            return;
        }
        if (!bird.falconTerminalVelocityStriking) {
            handleTerminalVelocityWarning(bird);
            return;
        }
        handleTerminalVelocityStrike(bird);
    }

    private static void handleTerminalVelocityWarning(Bird bird) {
        bird.x = bird.falconTerminalVelocityStartX - bird.bodyWidth() * 0.5;
        bird.y = bird.falconTerminalVelocityStartY - bird.bodyHeight() * 0.5;
        bird.vx = 0.0;
        bird.vy = 0.0;
        bird.attackAnimationTimer = Math.max(bird.attackAnimationTimer, 4);

        if ((bird.falconTerminalVelocityTimer & 3) == 0) {
            double t = bird.game.nextParticleRandom();
            double x = bird.falconTerminalVelocityStartX
                    + (bird.falconTerminalVelocityEndX - bird.falconTerminalVelocityStartX) * t;
            double y = bird.falconTerminalVelocityStartY
                    + (bird.falconTerminalVelocityEndY - bird.falconTerminalVelocityStartY) * t;
            bird.game.particles.add(new Particle(
                    x,
                    y,
                    (bird.game.nextParticleRandom() - 0.5) * 1.8,
                    -1.6 - bird.game.nextParticleRandom() * 1.8,
                    Color.web("#FFE0B2").deriveColor(0, 1, 1, 0.62)
            ));
        }

        if (bird.falconTerminalVelocityTimer <= 0) {
            bird.falconTerminalVelocityStriking = true;
            bird.falconTerminalVelocityTimer = Bird.FALCON_TERMINAL_VELOCITY_STRIKE_FRAMES;
            bird.attackAnimationTimer = Math.max(bird.attackAnimationTimer, Bird.FALCON_TERMINAL_VELOCITY_STRIKE_FRAMES);
            bird.game.shakeIntensity = Math.max(bird.game.shakeIntensity, 16.0);
            bird.game.hitstopFrames = Math.max(bird.game.hitstopFrames, 4);
        }
    }

    private static void handleTerminalVelocityStrike(Bird bird) {
        if (bird.falconTerminalVelocityTimer <= 0) {
            finishTerminalVelocity(bird);
            return;
        }

        double progress = 1.0 - Math.clamp(
                bird.falconTerminalVelocityTimer / (double) Bird.FALCON_TERMINAL_VELOCITY_STRIKE_FRAMES,
                0.0,
                1.0
        );
        bird.x = bird.falconTerminalVelocityStartX
                + (bird.falconTerminalVelocityEndX - bird.falconTerminalVelocityStartX) * progress
                - bird.bodyWidth() * 0.5;
        bird.y = bird.falconTerminalVelocityStartY
                + (bird.falconTerminalVelocityEndY - bird.falconTerminalVelocityStartY) * progress
                - bird.bodyHeight() * 0.5;
        bird.vx = bird.falconTerminalVelocityDirection * 34.0;
        bird.vy = 18.0;
        bird.attackAnimationTimer = Math.max(bird.attackAnimationTimer, 2);
        applyTerminalVelocityHits(bird);

        for (int i = 0; i < bird.scaledParticleCount(5); i++) {
            bird.game.particles.add(new Particle(
                    bird.bodyCenterX() - bird.falconTerminalVelocityDirection * (16.0 + bird.game.nextParticleRandom() * 42.0),
                    bird.bodyCenterY() + (bird.game.nextParticleRandom() - 0.5) * 18.0,
                    -bird.falconTerminalVelocityDirection * (5.0 + bird.game.nextParticleRandom() * 8.0),
                    -1.0 - bird.game.nextParticleRandom() * 2.5,
                    Color.web("#FF7043").deriveColor(0, 1, 1, 0.78)
            ));
        }
    }

    private static void finishTerminalVelocity(Bird bird) {
        bird.x = bird.falconTerminalVelocityEndX - bird.bodyWidth() * 0.5;
        bird.y = bird.falconTerminalVelocityEndY - bird.bodyHeight() * 0.5;
        bird.vx = bird.falconTerminalVelocityDirection * (bird.falconTerminalVelocityHitAny ? 12.0 : 18.0);
        bird.vy = bird.falconTerminalVelocityHitAny ? -5.0 : 4.0;
        bird.canDoubleJump = true;
        if (!bird.falconTerminalVelocityHitAny) {
            bird.applySpecialLandingLag(Bird.FALCON_TERMINAL_VELOCITY_MISS_LANDING_LAG_FRAMES);
            bird.attackCooldown = Math.max(bird.attackCooldown, 18);
        }
        resetTerminalVelocity(bird);
    }

    private static void applyTerminalVelocityHits(Bird bird) {
        for (Bird other : bird.game.players) {
            if (!bird.canDamageTarget(other)) {
                continue;
            }
            int targetIndex = other.playerIndex;
            if (targetIndex < 0 || targetIndex >= bird.falconTerminalVelocityHit.length) {
                continue;
            }
            if (bird.falconTerminalVelocityHit[targetIndex]) {
                continue;
            }
            double projection = segmentProjection(
                    other.bodyCenterX(),
                    other.bodyCenterY(),
                    bird.falconTerminalVelocityStartX,
                    bird.falconTerminalVelocityStartY,
                    bird.falconTerminalVelocityEndX,
                    bird.falconTerminalVelocityEndY
            );
            if (projection < 0.0 || projection > 1.0) {
                continue;
            }
            double distance = pointToSegmentDistance(
                    other.bodyCenterX(),
                    other.bodyCenterY(),
                    bird.falconTerminalVelocityStartX,
                    bird.falconTerminalVelocityStartY,
                    bird.falconTerminalVelocityEndX,
                    bird.falconTerminalVelocityEndY
            );
            if (distance > Bird.FALCON_TERMINAL_VELOCITY_PATH_WIDTH + other.combatRadius()) {
                continue;
            }

            boolean sweetspot = distance <= Bird.FALCON_TERMINAL_VELOCITY_SWEETSPOT_WIDTH + other.combatRadius() * 0.35
                    && Math.abs(projection - 0.5) <= 0.18;
            int damage = sweetspot
                    ? Bird.FALCON_TERMINAL_VELOCITY_SWEETSPOT_DAMAGE
                    : Bird.FALCON_TERMINAL_VELOCITY_DAMAGE;
            int dealt = bird.applyTrackedSpecialDamage(other, damage);
            if (dealt <= 0) {
                continue;
            }
            bird.falconTerminalVelocityHit[targetIndex] = true;
            bird.falconTerminalVelocityHitAny = true;

            other.vx += bird.falconTerminalVelocityDirection * (sweetspot ? 19.0 : 12.5);
            other.vy -= sweetspot ? 14.0 : 8.0;
            bird.game.shakeIntensity = Math.max(bird.game.shakeIntensity, sweetspot ? 30.0 : 18.0);
            bird.game.hitstopFrames = Math.max(bird.game.hitstopFrames, sweetspot ? 10 : 6);
            if (sweetspot) {
                bird.game.triggerFlash(0.42, false);
                bird.game.addToKillFeed(bird.shortName() + " pierced " + other.shortName() + " at terminal velocity!");
            }
            emitTerminalVelocityHitParticles(bird, other.bodyCenterX(), other.bodyCenterY(),
                    sweetspot ? 34 : 18, sweetspot ? Color.web("#FFF59D") : Color.web("#FFAB91"));
        }
    }

    private static Bird selectTerminalVelocityTarget(Bird bird, int dir) {
        Bird best = null;
        double bestScore = Double.POSITIVE_INFINITY;
        for (Bird other : bird.game.players) {
            if (!bird.canDamageTarget(other)) {
                continue;
            }
            double dx = other.bodyCenterX() - bird.bodyCenterX();
            double dy = other.bodyCenterY() - bird.bodyCenterY();
            double forward = dx * dir;
            if (forward < -120.0 || forward > 1050.0 || Math.abs(dy) > 520.0) {
                continue;
            }
            double score = Math.max(0.0, forward) + Math.abs(dy) * 1.25;
            if (score < bestScore) {
                bestScore = score;
                best = other;
            }
        }
        return best == null ? nearestTerminalVelocityTarget(bird) : best;
    }

    private static Bird nearestTerminalVelocityTarget(Bird bird) {
        Bird best = null;
        double bestDistance = Double.POSITIVE_INFINITY;
        for (Bird other : bird.game.players) {
            if (!bird.canDamageTarget(other)) {
                continue;
            }
            double distance = Math.hypot(other.bodyCenterX() - bird.bodyCenterX(),
                    other.bodyCenterY() - bird.bodyCenterY());
            if (distance < bestDistance) {
                bestDistance = distance;
                best = other;
            }
        }
        return best;
    }

    private static double segmentProjection(double px, double py, double ax, double ay, double bx, double by) {
        double abx = bx - ax;
        double aby = by - ay;
        double denom = abx * abx + aby * aby;
        if (denom <= 0.0001) {
            return 0.0;
        }
        return ((px - ax) * abx + (py - ay) * aby) / denom;
    }

    private static double pointToSegmentDistance(double px, double py, double ax, double ay, double bx, double by) {
        double t = Math.clamp(segmentProjection(px, py, ax, ay, bx, by), 0.0, 1.0);
        double closestX = ax + (bx - ax) * t;
        double closestY = ay + (by - ay) * t;
        return Math.hypot(px - closestX, py - closestY);
    }

    private static void emitTerminalVelocityHitParticles(Bird bird, double centerX, double centerY,
                                                         int requested, Color color) {
        for (int i = 0; i < bird.scaledParticleCount(requested); i++) {
            double angle = bird.game.nextParticleRandom() * Math.PI * 2.0;
            double speed = 3.5 + bird.game.nextParticleRandom() * 9.0;
            bird.game.particles.add(new Particle(
                    centerX + (bird.game.nextParticleRandom() - 0.5) * 28.0,
                    centerY + (bird.game.nextParticleRandom() - 0.5) * 28.0,
                    Math.cos(angle) * speed + bird.falconTerminalVelocityDirection * 3.0,
                    Math.sin(angle) * speed - 2.0,
                    color.deriveColor(0, 1, 1, 0.82)
            ));
        }
    }

    private static void handleCry(Bird bird) {
        boolean eagle = bird.type == BirdGame3.BirdType.EAGLE || bird.mockingbirdCopiedNeutralFrom(BirdGame3.BirdType.EAGLE);
        bird.vx *= eagle ? 0.84 : 0.9;
        if (!bird.isOnGround()) {
            bird.vy = Math.min(bird.vy, eagle ? 1.6 : 1.1);
        }

        if ((bird.raptorCryTimer & 1) != 0) {
            return;
        }

        int dir = bird.facingDirection();
        Color particleColor = eagle ? Color.web("#F0C766") : Color.web("#FFB56E");
        for (int i = 0; i < 2; i++) {
            double spread = (bird.game.nextParticleRandom() - 0.5) * (eagle ? 16.0 : 10.0);
            bird.game.particles.add(new Particle(
                    bird.bodyCenterX() + dir * (28 + bird.game.nextParticleRandom() * 20),
                    bird.bodyCenterY() - 8 + spread,
                    dir * (2.6 + bird.game.nextParticleRandom() * 2.4),
                    spread * 0.08,
                    particleColor.deriveColor(0, 1, 1, 0.76)
            ));
        }
    }

    private static void handleRush(Bird bird) {
        int dir = bird.raptorRushDirection == 0 ? bird.facingDirection() : bird.raptorRushDirection;
        boolean eagle = bird.type == BirdGame3.BirdType.EAGLE;
        bird.vx = dir * rushSpeed(bird);
        if (bird.raptorRushGrounded) {
            bird.vy = Math.min(bird.vy, 0.0);
        } else {
            bird.vy = Math.min(bird.vy, eagle ? 1.2 : 0.8);
        }

        for (Bird other : bird.game.players) {
            if (!bird.canDamageTarget(other)) continue;
            if (other.playerIndex < 0 || other.playerIndex >= bird.raptorRushHit.length) continue;
            if (bird.raptorRushHit[other.playerIndex]) continue;

            double dx = other.bodyCenterX() - bird.bodyCenterX();
            double dy = other.bodyCenterY() - bird.bodyCenterY();
            double forward = dx * dir;
            if (forward < -other.combatHalfWidth() * 0.35) continue;
            if (forward > (eagle ? 122.0 : 98.0) * bird.sizeMultiplier + other.combatHalfWidth()) continue;
            if (Math.abs(dy) > (eagle ? 78.0 : 60.0) * bird.sizeMultiplier + other.combatHalfHeight()) continue;

            boolean sweetspot = !eagle && forward > 72.0 * bird.sizeMultiplier;
            int dmg = eagle
                    ? (bird.raptorRushUltimate ? 13 : 10)
                    : (sweetspot ? (bird.raptorRushUltimate ? 11 : 9) : (bird.raptorRushUltimate ? 8 : 7));
            double oldHealth = other.health;
            int dealt = (int) bird.applyDamageTo(other, dmg);
            if (dealt <= 0) continue;

            bird.game.damageDealt[bird.playerIndex] += dealt;
            bird.game.recordSpecialImpact(bird.playerIndex, dealt, true);
            if (other.health <= 0 && oldHealth > 0) {
                bird.game.eliminations[bird.playerIndex]++;
            }

            other.vx += dir * (sweetspot ? 13.0 : eagle ? 10.8 : 8.8);
            other.vy -= sweetspot ? 12.2 : eagle ? 9.4 : 8.6;
            bird.raptorRushHit[other.playerIndex] = true;

            Color spark = sweetspot ? Color.web("#FFF0A6") : eagle ? Color.web("#E7B653") : Color.web("#FF9F68");
            for (int i = 0; i < (sweetspot ? 18 : 12); i++) {
                double angle = bird.game.nextParticleRandom() * Math.PI * 2;
                bird.game.particles.add(new Particle(
                        other.x + 40,
                        other.y + 40,
                        Math.cos(angle) * (3 + bird.game.nextParticleRandom() * 5),
                        Math.sin(angle) * (3 + bird.game.nextParticleRandom() * 5) - 2,
                        spark
                ));
            }
        }
    }

    private static void handleClimb(Bird bird) {
        boolean eagle = bird.type == BirdGame3.BirdType.EAGLE;
        int inputDir = bird.horizontalInputDirection();
        if (inputDir != 0) {
            bird.raptorClimbDirection = inputDir;
            bird.facingRight = inputDir > 0;
        }
        double steer = eagle ? 0.36 : 0.58;
        double maxHorizontal = eagle ? 5.2 : 7.8;
        bird.vx = Math.clamp(bird.vx * (eagle ? 0.9 : 0.93) + inputDir * steer, -maxHorizontal, maxHorizontal);

        int strongLiftFrames = eagle
                ? (bird.raptorClimbUltimate ? 10 : 8)
                : (bird.raptorClimbUltimate ? 8 : 6);
        double lift = bird.raptorClimbTimer > strongLiftFrames
                ? (eagle
                    ? (bird.raptorClimbUltimate ? -13.8 : -11.2)
                    : (bird.raptorClimbUltimate ? -12.8 : -11.1))
                : (eagle
                    ? (bird.raptorClimbUltimate ? -10.0 : -8.0)
                    : (bird.raptorClimbUltimate ? -8.7 : -7.5));
        bird.vy = Math.min(bird.vy, lift);

        for (Bird other : bird.game.players) {
            if (!bird.canDamageTarget(other)) continue;
            if (other.playerIndex < 0 || other.playerIndex >= bird.raptorClimbHit.length) continue;
            if (bird.raptorClimbHit[other.playerIndex]) continue;

            double dx = other.bodyCenterX() - bird.bodyCenterX();
            double dy = other.bodyCenterY() - (bird.bodyCenterY() - bird.bodyHeight() * 0.16);
            if (Math.abs(dx) > (eagle ? 88.0 : 74.0) * bird.sizeMultiplier + other.combatHalfWidth()) continue;
            if (Math.abs(dy) > (eagle ? 108.0 : 90.0) * bird.sizeMultiplier + other.combatHalfHeight()) continue;

            double forward = dx * (bird.raptorClimbDirection == 0 ? bird.facingDirection() : bird.raptorClimbDirection);
            boolean sweetspot = !eagle && forward > 44.0 * bird.sizeMultiplier;
            int dmg = eagle
                    ? (bird.raptorClimbUltimate ? 10 : 8)
                    : (sweetspot ? (bird.raptorClimbUltimate ? 9 : 7) : (bird.raptorClimbUltimate ? 8 : 6));
            double oldHealth = other.health;
            int dealt = (int) bird.applyDamageTo(other, dmg);
            if (dealt <= 0) continue;

            bird.game.damageDealt[bird.playerIndex] += dealt;
            bird.game.recordSpecialImpact(bird.playerIndex, dealt, true);
            if (other.health <= 0 && oldHealth > 0) {
                bird.game.eliminations[bird.playerIndex]++;
            }

            double launchDir = dx == 0.0 ? (bird.raptorClimbDirection == 0 ? bird.facingDirection() : bird.raptorClimbDirection) : Math.signum(dx);
            other.vx += launchDir * (sweetspot ? 8.0 : eagle ? 6.2 : 5.6);
            other.vy -= sweetspot ? 10.2 : eagle ? 8.8 : 7.8;
            bird.raptorClimbHit[other.playerIndex] = true;

            Color spark = eagle ? Color.web("#F3D37D") : sweetspot ? Color.web("#FFF0A6") : Color.web("#FFB86F");
            for (int i = 0; i < (sweetspot ? 16 : 10); i++) {
                double angle = -Math.PI / 2 + (bird.game.nextParticleRandom() - 0.5) * 1.3;
                bird.game.particles.add(new Particle(
                        other.x + 40,
                        other.y + 40,
                        Math.cos(angle) * (4 + bird.game.nextParticleRandom() * 5),
                        Math.sin(angle) * (7 + bird.game.nextParticleRandom() * 7),
                        spark
                ));
            }
        }
    }

    private static double rushSpeed(Bird bird) {
        boolean eagle = bird.type == BirdGame3.BirdType.EAGLE;
        if (eagle) {
            if (bird.raptorRushGrounded) {
                return bird.raptorRushUltimate ? 15.1 : 13.8;
            }
            return bird.raptorRushUltimate ? 13.8 : 12.4;
        }
        if (bird.raptorRushGrounded) {
            return bird.raptorRushUltimate ? 18.4 : 16.9;
        }
        return bird.raptorRushUltimate ? 16.4 : 15.0;
    }

    private static int cryReuseFrames(Bird bird, boolean ultimate) {
        boolean eagle = bird.type == BirdGame3.BirdType.EAGLE;
        return eagle ? (ultimate ? 60 : 52) : (ultimate ? 44 : 36);
    }

    private static int rushReuseFrames(Bird bird, boolean ultimate) {
        boolean eagle = bird.type == BirdGame3.BirdType.EAGLE;
        return eagle ? (ultimate ? 58 : 48) : (ultimate ? 42 : 34);
    }
}
