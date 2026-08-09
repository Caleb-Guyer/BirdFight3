package com.example.birdgame3;

import javafx.scene.paint.Color;

import java.util.Arrays;

final class PigeonSpecials {
    private PigeonSpecials() {
    }

    static final String ROOFTOP_CORONATION_MOVE = "Pigeon Rooftop Coronation";

    static void use(Bird bird, boolean ultimate) {
        switch (bird.selectPigeonSpecialVariant()) {
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
        bird.pigeonFeatherBurstTimer = Bird.PIGEON_NEUTRAL_BURST_FRAMES;
        bird.pigeonFeatherBurstUltimate = ultimate;
        bird.specialCooldown = Bird.PIGEON_NEUTRAL_COOLDOWN_FRAMES + (ultimate ? 6 : 0);
        bird.specialMaxCooldown = bird.specialCooldown;
        bird.attackAnimationTimer = Math.max(bird.attackAnimationTimer, bird.pigeonFeatherBurstTimer);
        bird.vx *= 0.45;
        bird.game.playPigeonFeatherBurstSfx(ultimate);

        double[] laneOffsets = {-20.0, 0.0, 20.0};
        double[] laneReach = ultimate ? new double[]{116.0, 132.0, 116.0} : new double[]{102.0, 118.0, 102.0};
        double centerX = bird.bodyCenterX() + dir * bird.bodyWidth() * 0.54;
        for (Bird other : bird.game.players) {
            if (!bird.canDamageTarget(other)) continue;
            double dx = other.bodyCenterX() - centerX;
            if (dir > 0 && dx < -other.combatHalfWidth() * 0.2) continue;
            if (dir < 0 && dx > other.combatHalfWidth() * 0.2) continue;

            boolean hit = false;
            for (int i = 0; i < laneOffsets.length; i++) {
                double laneY = bird.bodyCenterY() + laneOffsets[i] * bird.sizeMultiplier;
                double laneDx = Math.abs(dx);
                double laneDy = Math.abs(other.bodyCenterY() - laneY);
                if (laneDx > laneReach[i] * bird.sizeMultiplier + other.combatHalfWidth()) continue;
                if (laneDy > 18.0 * bird.sizeMultiplier + other.combatHalfHeight()) continue;
                hit = true;
                break;
            }
            if (!hit) continue;

            int dmg = ultimate ? 6 : 4;
            double oldHealth = other.health;
            int dealt = (int) bird.applyDamageTo(other, dmg);
            if (dealt <= 0) continue;

            bird.game.damageDealt[bird.playerIndex] += dealt;
            bird.game.recordSpecialImpact(bird.playerIndex, dealt, true);
            boolean isKill = other.health <= 0 && oldHealth > 0;
            if (isKill) {
                bird.game.eliminations[bird.playerIndex]++;
            }

            double launchX = dir * (ultimate ? 5.8 : 4.6);
            double launchY = -(ultimate ? 3.8 : 2.9);
            other.vx += launchX;
            other.vy += launchY;
            emitSpecialImpact(bird, other, launchX, launchY, dealt, isKill, "Pigeon Feather Burst");
        }

        for (int feather = 0; feather < 3; feather++) {
            double laneY = bird.bodyCenterY() + laneOffsets[feather] * bird.sizeMultiplier;
            for (int i = 0; i < 6; i++) {
                double progress = i / 5.0;
                double spread = (feather - 1) * 0.18;
                double speed = 4.2 + progress * 6.0;
                bird.game.particles.add(new Particle(
                        centerX + dir * (10 + progress * 56),
                        laneY + Math.sin(progress * Math.PI) * 6 * spread,
                        dir * speed,
                        spread * 2.4 - 0.5,
                        ultimate ? Color.GOLD.deriveColor(0, 1, 1, 0.86) : Color.WHITE.deriveColor(0, 1, 1, 0.78)
                ));
            }
        }
    }

    static void side(Bird bird, boolean ultimate) {
        int dir = bird.horizontalInputDirection();
        if (dir == 0) {
            dir = bird.facingDirection();
        }
        bird.facingRight = dir > 0;
        bird.pigeonRushGrounded = bird.isOnGround();
        bird.pigeonRushUltimate = ultimate;
        bird.pigeonRushTimer = bird.pigeonRushGrounded ? Bird.PIGEON_RUSH_GROUND_FRAMES : Bird.PIGEON_RUSH_AIR_FRAMES;
        Arrays.fill(bird.pigeonRushHit, false);
        bird.specialCooldown = 0;
        bird.specialMaxCooldown = 0;
        bird.attackAnimationTimer = Math.max(bird.attackAnimationTimer, bird.pigeonRushTimer);
        bird.vx = dir * rushSpeed(bird);
        bird.game.playPigeonRushSfx(ultimate);
        if (!bird.pigeonRushGrounded) {
            bird.vy = Math.min(bird.vy, ultimate ? 0.8 : 1.4);
        } else {
            bird.vy = Math.min(bird.vy, 0.0);
        }
        bird.isBlocking = false;
        bird.parryWindowFrames = 0;
        bird.shieldStunFrames = 0;
    }

    static void up(Bird bird, boolean ultimate) {
        if (bird.pigeonUpSpecialUsed) {
            return;
        }
        int dir = bird.horizontalInputDirection();
        if (dir != 0) {
            bird.facingRight = dir > 0;
        } else {
            dir = bird.facingDirection();
        }
        bird.pigeonUpSpecialUsed = true;
        bird.pigeonFlutterUltimate = ultimate;
        bird.pigeonFlutterTimer = ultimate ? Bird.PIGEON_FLUTTER_ULTIMATE_FRAMES : Bird.PIGEON_FLUTTER_FRAMES;
        Arrays.fill(bird.pigeonFlutterHit, false);
        bird.specialCooldown = 0;
        bird.specialMaxCooldown = 0;
        bird.attackAnimationTimer = Math.max(bird.attackAnimationTimer, bird.pigeonFlutterTimer);
        bird.canDoubleJump = false;
        bird.vx = dir * (ultimate ? 3.2 : 2.2);
        bird.vy = ultimate ? -16.4 : -14.6;
        bird.game.playPigeonFlutterSfx(ultimate);
        if (ultimate) {
            bird.game.triggerFlash(0.35, false);
        }
    }

    static void down(Bird bird, boolean ultimate) {
        bird.pigeonScavengeAirborne = !bird.isOnGround();
        bird.pigeonScavengeUltimate = ultimate;
        bird.pigeonScavengeResolved = false;
        bird.pigeonScavengeHoldFrames = 0;
        Arrays.fill(bird.pigeonScavengeHitCooldown, 0);
        bird.pigeonScavengeTimer = Bird.PIGEON_SCAVENGE_MAX_HOLD_FRAMES + Bird.PIGEON_SCAVENGE_RELEASE_FRAMES;
        bird.specialCooldown = 0;
        bird.specialMaxCooldown = 0;
        bird.attackAnimationTimer = Math.max(bird.attackAnimationTimer, bird.pigeonScavengeTimer);
        bird.vx *= bird.pigeonScavengeAirborne ? 0.62 : 0.18;
        bird.game.playPigeonScavengeSfx(bird.pigeonScavengeAirborne, ultimate);
        if (bird.pigeonScavengeAirborne) {
            bird.vy = Math.max(bird.vy, ultimate ? 7.5 : 6.5);
        } else {
            bird.vy = Math.min(bird.vy, 0.0);
        }
        bird.isBlocking = false;
        bird.parryWindowFrames = 0;
        bird.shieldStunFrames = 0;
        bird.blockCooldown = 0;
    }

    static void handleState(Bird bird) {
        boolean realPigeon = bird.type == BirdGame3.BirdType.PIGEON;
        boolean copiedPigeon = bird.mockingbirdCopiedNeutralFrom(BirdGame3.BirdType.PIGEON);
        if (!realPigeon && !copiedPigeon) {
            return;
        }
        if (bird.stunTime > 0.0) {
            reset(bird);
            if (copiedPigeon) {
                bird.mockingbirdCopiedNeutralSource = null;
            }
        } else {
            if (bird.pigeonRushTimer > 0) {
                handleRush(bird);
            }
            if (bird.pigeonFlutterTimer > 0) {
                handleFlutter(bird);
            }
            if (bird.pigeonScavengeTimer > 0) {
                handleScavenge(bird);
            }
        }
        if (realPigeon && bird.pigeonCoronationActive) {
            handleCoronation(bird);
        }
    }

    static boolean active(Bird bird) {
        return bird.pigeonRushTimer > 0 || bird.pigeonFlutterTimer > 0 || bird.pigeonScavengeTimer > 0;
    }

    static void startCoronation(Bird bird) {
        reset(bird);
        bird.pigeonCoronationActive = true;
        bird.pigeonCoronationTimer = Bird.PIGEON_CORONATION_FRAMES;
        bird.pigeonCoronationX = bird.bodyCenterX();
        bird.pigeonCoronationY = bird.bodyCenterY();
        bird.pigeonCoronationFinalResolved = false;
        bird.pigeonCoronationStayedInside = true;
        Arrays.fill(bird.pigeonCoronationTickCooldown, 0);
        Arrays.fill(bird.pigeonCoronationFinalHit, false);
        bird.specialCooldown = 0;
        bird.specialMaxCooldown = 0;
        bird.attackAnimationTimer = Math.max(bird.attackAnimationTimer, 36);
        bird.vx *= 0.24;
        bird.vy = Math.min(bird.vy, 0.0);
        bird.isBlocking = false;
        bird.parryWindowFrames = 0;
        bird.shieldStunFrames = 0;
        bird.blockCooldown = 0;
        bird.game.addToKillFeed(bird.shortName() + " claimed the rooftop!");
        bird.game.playPigeonCoronationSfx();

        for (int i = 0; i < bird.scaledParticleCount(42); i++) {
            double angle = bird.game.nextParticleRandom() * Math.PI * 2.0;
            double speed = 1.5 + bird.game.nextParticleRandom() * 5.0;
            bird.game.particles.add(new Particle(
                    bird.pigeonCoronationX + Math.cos(angle) * (18.0 + bird.game.nextParticleRandom() * 48.0),
                    bird.pigeonCoronationY + Math.sin(angle) * (18.0 + bird.game.nextParticleRandom() * 48.0),
                    Math.cos(angle) * speed,
                    Math.sin(angle) * speed - 1.2,
                    Color.web("#FFD54F").deriveColor(0, 1, 1, 0.86)
            ));
        }
    }

    static void reset(Bird bird) {
        bird.pigeonFeatherBurstTimer = 0;
        bird.pigeonFeatherBurstUltimate = false;
        bird.pigeonRushTimer = 0;
        bird.pigeonRushGrounded = false;
        bird.pigeonRushUltimate = false;
        Arrays.fill(bird.pigeonRushHit, false);
        bird.pigeonFlutterTimer = 0;
        bird.pigeonFlutterUltimate = false;
        Arrays.fill(bird.pigeonFlutterHit, false);
        bird.pigeonScavengeTimer = 0;
        bird.pigeonScavengeHoldFrames = 0;
        bird.pigeonScavengeAirborne = false;
        bird.pigeonScavengeUltimate = false;
        bird.pigeonScavengeResolved = false;
        Arrays.fill(bird.pigeonScavengeHitCooldown, 0);
    }

    static void resetCoronation(Bird bird) {
        bird.pigeonCoronationActive = false;
        bird.pigeonCoronationTimer = 0;
        bird.pigeonCoronationX = 0.0;
        bird.pigeonCoronationY = 0.0;
        bird.pigeonCoronationFinalResolved = false;
        bird.pigeonCoronationStayedInside = false;
        Arrays.fill(bird.pigeonCoronationTickCooldown, 0);
        Arrays.fill(bird.pigeonCoronationFinalHit, false);
    }

    private static void handleRush(Bird bird) {
        int dir = bird.facingDirection();
        double speed = rushSpeed(bird);
        bird.vx = dir * speed;
        if (!bird.pigeonRushGrounded) {
            bird.vy = Math.min(bird.vy, bird.pigeonRushUltimate ? 1.4 : 1.9);
        } else {
            bird.vy = Math.min(bird.vy, 0.0);
        }

        for (Bird other : bird.game.players) {
            if (!bird.canDamageTarget(other)) continue;
            if (other.playerIndex < 0 || other.playerIndex >= bird.pigeonRushHit.length) continue;
            if (bird.pigeonRushHit[other.playerIndex]) continue;

            double dx = other.bodyCenterX() - bird.bodyCenterX();
            double dy = other.bodyCenterY() - bird.bodyCenterY();
            if (dir > 0 && dx < -other.combatHalfWidth() * 0.35) continue;
            if (dir < 0 && dx > other.combatHalfWidth() * 0.35) continue;
            if (Math.abs(dx) > 108 + other.combatHalfWidth()) continue;
            if (Math.abs(dy) > 82 + other.combatHalfHeight()) continue;

            int dmg = rushDamage(bird);
            double oldHealth = other.health;
            int dealt = (int) bird.applyDamageTo(other, dmg);
            if (dealt <= 0) continue;

            bird.game.damageDealt[bird.playerIndex] += dealt;
            bird.game.recordSpecialImpact(bird.playerIndex, dealt, true);
            boolean isKill = other.health <= 0 && oldHealth > 0;
            if (isKill) {
                bird.game.eliminations[bird.playerIndex]++;
            }

            double launchX = dir * rushHorizontalLaunch(bird);
            double launchY = -rushVerticalLaunch(bird);
            other.vx += launchX;
            other.vy += launchY;
            bird.pigeonRushHit[other.playerIndex] = true;
            emitSpecialImpact(bird, other, launchX, launchY, dealt, isKill, "Pigeon Street Rush");

            for (int i = 0; i < 14; i++) {
                double angle = bird.game.nextParticleRandom() * Math.PI * 2;
                bird.game.particles.add(new Particle(
                        other.x + 40,
                        other.y + 40,
                        Math.cos(angle) * (4 + bird.game.nextParticleRandom() * 6),
                        Math.sin(angle) * (4 + bird.game.nextParticleRandom() * 6) - 2.8,
                        bird.pigeonRushUltimate ? Color.GOLD.deriveColor(0, 1, 1, 0.82) : Color.web("#CFD8DC").deriveColor(0, 1, 1, 0.78)
                ));
            }
        }

        for (int i = 0; i < 3; i++) {
            bird.game.particles.add(new Particle(
                    bird.x + bird.bodyWidth() * (dir > 0 ? 0.2 : 0.8),
                    bird.y + bird.bodyHeight() * 0.78 + (bird.game.nextParticleRandom() - 0.5) * 10,
                    -dir * (1.6 + bird.game.nextParticleRandom() * 2.4),
                    -1.4 - bird.game.nextParticleRandom() * 1.8,
                    bird.pigeonRushUltimate ? Color.GOLD.deriveColor(0, 1, 1, 0.8) : Color.LIGHTGRAY.deriveColor(0, 1, 1, 0.62)
            ));
        }
    }

    private static void handleFlutter(Bird bird) {
        int inputDir = bird.horizontalInputDirection();
        if (inputDir != 0) {
            bird.facingRight = inputDir > 0;
        }
        double steer = inputDir * (bird.pigeonFlutterUltimate ? 0.9 : 0.72);
        double maxHorizontal = bird.pigeonFlutterUltimate ? 6.6 : 5.2;
        bird.vx = Math.clamp(bird.vx * 0.84 + steer, -maxHorizontal, maxHorizontal);
        double lift = bird.pigeonFlutterTimer > (bird.pigeonFlutterUltimate ? 8 : 6)
                ? (bird.pigeonFlutterUltimate ? -12.8 : -10.9)
                : (bird.pigeonFlutterUltimate ? -9.5 : -8.0);
        bird.vy = Math.min(bird.vy, lift);

        for (Bird other : bird.game.players) {
            if (!bird.canDamageTarget(other)) continue;
            if (other.playerIndex < 0 || other.playerIndex >= bird.pigeonFlutterHit.length) continue;
            if (bird.pigeonFlutterHit[other.playerIndex]) continue;

            double dx = other.bodyCenterX() - bird.bodyCenterX();
            double dy = other.bodyCenterY() - (bird.bodyCenterY() - bird.bodyHeight() * 0.1);
            if (Math.abs(dx) > 86 + other.combatHalfWidth()) continue;
            if (Math.abs(dy) > 100 + other.combatHalfHeight()) continue;

            int dmg = bird.pigeonFlutterUltimate ? 9 : 6;
            double oldHealth = other.health;
            int dealt = (int) bird.applyDamageTo(other, dmg);
            if (dealt <= 0) continue;

            bird.game.damageDealt[bird.playerIndex] += dealt;
            bird.game.recordSpecialImpact(bird.playerIndex, dealt, true);
            boolean isKill = other.health <= 0 && oldHealth > 0;
            if (isKill) {
                bird.game.eliminations[bird.playerIndex]++;
            }

            double launchDir = dx == 0.0 ? bird.facingDirection() : Math.signum(dx);
            double launchX = launchDir * (bird.pigeonFlutterUltimate ? 7.2 : 5.6);
            double launchY = -(bird.pigeonFlutterUltimate ? 10.6 : 8.2);
            other.vx += launchX;
            other.vy += launchY;
            bird.pigeonFlutterHit[other.playerIndex] = true;
            emitSpecialImpact(bird, other, launchX, launchY, dealt, isKill, "Pigeon Fire-Escape Flutter");
        }

        for (int i = 0; i < 4; i++) {
            double angle = -Math.PI / 2 + (bird.game.nextParticleRandom() - 0.5) * 1.4;
            bird.game.particles.add(new Particle(
                    bird.bodyCenterX() + (bird.game.nextParticleRandom() - 0.5) * 26,
                    bird.bodyCenterY() + 18 + (bird.game.nextParticleRandom() - 0.5) * 18,
                    Math.cos(angle) * (2.4 + bird.game.nextParticleRandom() * 3.2),
                    Math.sin(angle) * (3.0 + bird.game.nextParticleRandom() * 5.0),
                    bird.pigeonFlutterUltimate ? Color.GOLD.deriveColor(0, 1, 1, 0.85) : Color.web("#E3F2FD").deriveColor(0, 1, 1, 0.76)
            ));
        }
    }

    private static void handleScavenge(Bird bird) {
        for (int i = 0; i < bird.pigeonScavengeHitCooldown.length; i++) {
            bird.pigeonScavengeHitCooldown[i] = Math.max(0, bird.pigeonScavengeHitCooldown[i] - 1);
        }

        boolean holding = bird.specialHeld()
                && bird.pigeonScavengeHoldFrames < Bird.PIGEON_SCAVENGE_MAX_HOLD_FRAMES
                && bird.pigeonScavengeTimer >= Bird.PIGEON_SCAVENGE_RELEASE_FRAMES;
        if (holding) {
            bird.pigeonScavengeHoldFrames++;
        } else {
            bird.pigeonScavengeTimer = Math.min(bird.pigeonScavengeTimer, Bird.PIGEON_SCAVENGE_RELEASE_FRAMES);
            bird.attackAnimationTimer = Math.min(bird.attackAnimationTimer, Bird.PIGEON_SCAVENGE_RELEASE_FRAMES);
        }

        if (bird.pigeonScavengeAirborne && bird.isOnGround()) {
            bird.pigeonScavengeAirborne = false;
            bird.pigeonScavengeResolved = true;
            Arrays.fill(bird.pigeonScavengeHitCooldown, 0);
            emitDrillLandingParticles(bird);
        }
        if (!bird.pigeonScavengeAirborne && !bird.isOnGround()) {
            reset(bird);
            return;
        }

        if (bird.pigeonScavengeAirborne) {
            int inputDir = bird.horizontalInputDirection();
            bird.vx = Math.clamp(bird.vx * 0.88 + inputDir * 0.42, -5.8, 5.8);
            if (holding) {
                double drillSpeed = bird.pigeonScavengeUltimate ? 19.5 : 17.5;
                bird.vy = Math.min(drillSpeed, Math.max(bird.vy + 1.15, bird.pigeonScavengeUltimate ? 8.0 : 7.0));
                resolveAerialDrillHits(bird);
                emitAerialDrillParticles(bird);
            } else {
                bird.vy = Math.min(bird.vy, bird.pigeonScavengeUltimate ? 13.0 : 11.5);
            }
        } else {
            bird.vx *= 0.08;
            bird.vy = Math.min(bird.vy, 0.0);
            if (holding) {
                resolveGroundCrackHits(bird);
                if ((bird.pigeonScavengeHoldFrames % 3) == 0) {
                    emitGroundCrackParticles(bird);
                }
            }
        }
    }

    static double groundCrackReachForHold(int holdFrames) {
        double progress = Math.clamp(holdFrames / (double) Bird.PIGEON_SCAVENGE_MAX_HOLD_FRAMES, 0.0, 1.0);
        return 70.0 + (Bird.PIGEON_SCAVENGE_GROUND_MAX_REACH - 70.0) * progress;
    }

    private static void resolveAerialDrillHits(Bird bird) {
        double centerX = bird.bodyCenterX();
        double centerY = bird.bodyBottomY() + 22.0 * bird.sizeMultiplier;
        for (Bird other : bird.game.players) {
            if (!bird.canDamageTarget(other) || hitOnCooldown(bird, other)) continue;
            double dx = other.bodyCenterX() - centerX;
            double dy = other.bodyCenterY() - centerY;
            if (Math.abs(dx) > 50.0 * bird.sizeMultiplier + other.combatHalfWidth()) continue;
            if (Math.abs(dy) > 62.0 * bird.sizeMultiplier + other.combatHalfHeight()) continue;

            double launchDir = Math.abs(dx) < 0.001 ? bird.facingDirection() : Math.signum(dx);
            double launchX = launchDir * (bird.pigeonScavengeUltimate ? 1.8 : 1.25);
            double launchY = bird.pigeonScavengeUltimate ? 5.4 : 4.2;
            if (applyScavengeHit(bird, other, bird.pigeonScavengeUltimate ? 3 : 2,
                    launchX, launchY, true, "Pigeon Rooftop Drill")) {
                setHitCooldown(bird, other, Bird.PIGEON_SCAVENGE_AIR_HIT_INTERVAL);
            }
        }
    }

    private static void resolveGroundCrackHits(Bird bird) {
        double centerX = bird.bodyCenterX();
        double groundY = bird.bodyBottomY();
        double reach = groundCrackReachForHold(bird.pigeonScavengeHoldFrames) * bird.sizeMultiplier;
        for (Bird other : bird.game.players) {
            if (!bird.canDamageTarget(other) || hitOnCooldown(bird, other)) continue;
            double dx = other.bodyCenterX() - centerX;
            if (Math.abs(dx) > reach + other.combatHalfWidth()) continue;
            if (Math.abs(other.bodyBottomY() - groundY) > 32.0 * bird.sizeMultiplier + other.combatHalfHeight() * 0.35) {
                continue;
            }

            double launchDir = Math.abs(dx) < 0.001 ? bird.facingDirection() : Math.signum(dx);
            double launchX = launchDir * (bird.pigeonScavengeUltimate ? 3.6 : 2.7);
            double launchY = -(bird.pigeonScavengeUltimate ? 4.8 : 3.6);
            if (applyScavengeHit(bird, other, bird.pigeonScavengeUltimate ? 4 : 2,
                    launchX, launchY, false, "Pigeon Fault Line")) {
                setHitCooldown(bird, other, Bird.PIGEON_SCAVENGE_GROUND_HIT_INTERVAL);
            }
        }
    }

    private static boolean applyScavengeHit(Bird bird, Bird other, int damage,
                                             double launchX, double launchY,
                                             boolean forceDownward, String moveName) {
        double oldHealth = other.health;
        int dealt = (int) bird.applyDamageTo(other, damage);
        if (dealt <= 0) return false;

        bird.game.damageDealt[bird.playerIndex] += dealt;
        bird.game.recordSpecialImpact(bird.playerIndex, dealt, true);
        boolean isKill = other.health <= 0 && oldHealth > 0;
        if (isKill) {
            bird.game.eliminations[bird.playerIndex]++;
        }
        other.vx += launchX;
        if (forceDownward) {
            other.vy = Math.max(other.vy, launchY);
        } else {
            other.vy += launchY;
        }
        emitSpecialImpact(bird, other, launchX, launchY, dealt, isKill, moveName);
        return true;
    }

    private static boolean hitOnCooldown(Bird bird, Bird other) {
        int index = other.playerIndex;
        return index >= 0
                && index < bird.pigeonScavengeHitCooldown.length
                && bird.pigeonScavengeHitCooldown[index] > 0;
    }

    private static void setHitCooldown(Bird bird, Bird other, int frames) {
        int index = other.playerIndex;
        if (index >= 0 && index < bird.pigeonScavengeHitCooldown.length) {
            bird.pigeonScavengeHitCooldown[index] = frames;
        }
    }

    private static void emitAerialDrillParticles(Bird bird) {
        for (int i = 0; i < 2; i++) {
            double angle = bird.pigeonScavengeHoldFrames * 0.72 + i * Math.PI;
            double radius = (12.0 + bird.game.nextParticleRandom() * 10.0) * bird.sizeMultiplier;
            bird.game.particles.add(new Particle(
                    bird.bodyCenterX() + Math.cos(angle) * radius,
                    bird.bodyBottomY() + 12.0 * bird.sizeMultiplier + Math.sin(angle) * radius * 0.35,
                    Math.cos(angle) * 1.8,
                    -1.0 - bird.game.nextParticleRandom() * 1.6,
                    (bird.pigeonScavengeUltimate ? Color.GOLD : Color.web("#D7CCC8")).deriveColor(0, 1, 1, 0.78)
            ));
        }
    }

    private static void emitGroundCrackParticles(Bird bird) {
        double reach = groundCrackReachForHold(bird.pigeonScavengeHoldFrames) * bird.sizeMultiplier;
        for (int side : new int[]{-1, 1}) {
            double distance = reach * (0.48 + bird.game.nextParticleRandom() * 0.48);
            bird.game.particles.add(new Particle(
                    bird.bodyCenterX() + side * distance,
                    bird.bodyBottomY() - 5.0 * bird.sizeMultiplier,
                    side * (0.7 + bird.game.nextParticleRandom() * 1.3),
                    -1.5 - bird.game.nextParticleRandom() * 2.0,
                    (bird.pigeonScavengeUltimate ? Color.GOLD : Color.web("#8D6E63")).deriveColor(0, 1, 1, 0.76)
            ));
        }
    }

    private static void emitDrillLandingParticles(Bird bird) {
        for (int i = 0; i < bird.scaledParticleCount(18); i++) {
            double direction = bird.game.nextParticleRandom() < 0.5 ? -1.0 : 1.0;
            double speed = 1.5 + bird.game.nextParticleRandom() * 4.0;
            bird.game.particles.add(new Particle(
                    bird.bodyCenterX() + direction * bird.game.nextParticleRandom() * 22.0,
                    bird.bodyBottomY() - 6.0,
                    direction * speed,
                    -1.2 - bird.game.nextParticleRandom() * 3.2,
                    (bird.pigeonScavengeUltimate ? Color.GOLD : Color.web("#A1887F")).deriveColor(0, 1, 1, 0.82)
            ));
        }
    }

    private static void handleCoronation(Bird bird) {
        if (bird.health <= 0) {
            resetCoronation(bird);
            return;
        }

        if (bird.pigeonCoronationTimer <= 0) {
            if (!bird.pigeonCoronationFinalResolved) {
                resolveCoronationFinal(bird);
            }
            resetCoronation(bird);
            return;
        }

        boolean inside = bird.isInsideOwnPigeonCoronationZone();
        if (inside) {
            bird.heal(Bird.PIGEON_CORONATION_HEAL_PER_FRAME);
        } else {
            bird.pigeonCoronationStayedInside = false;
        }

        if ((bird.pigeonCoronationTimer & 7) == 0) {
            double angle = bird.game.nextParticleRandom() * Math.PI * 2.0;
            double distance = 24.0 + bird.game.nextParticleRandom() * Bird.PIGEON_CORONATION_RADIUS * 0.82;
            bird.game.particles.add(new Particle(
                    bird.pigeonCoronationX + Math.cos(angle) * distance,
                    bird.pigeonCoronationY + Math.sin(angle) * distance,
                    -Math.cos(angle) * 0.7,
                    -Math.sin(angle) * 0.7 - 0.4,
                    Color.web("#FFF59D").deriveColor(0, 1, 1, 0.48)
            ));
        }

        for (Bird other : bird.game.players) {
            if (!bird.canDamageTarget(other) || !inCoronationZone(bird, other)) {
                continue;
            }
            int targetIndex = other.playerIndex;
            if (targetIndex < 0 || targetIndex >= bird.pigeonCoronationTickCooldown.length) {
                continue;
            }
            if (bird.pigeonCoronationTickCooldown[targetIndex] > 0) {
                continue;
            }
            bird.pigeonCoronationTickCooldown[targetIndex] = Bird.PIGEON_CORONATION_TICK_INTERVAL;
            int dealt = bird.applyTrackedSpecialDamage(other, Bird.PIGEON_CORONATION_TICK_DAMAGE);
            if (dealt <= 0) {
                continue;
            }

            double dx = other.bodyCenterX() - bird.pigeonCoronationX;
            double dy = other.bodyCenterY() - bird.pigeonCoronationY;
            double distance = Math.max(1.0, Math.hypot(dx, dy));
            other.vx += dx / distance * 1.25;
            other.vy -= 0.95 + Math.max(0.0, -dy / distance) * 0.55;
            emitCoronationHitParticles(bird, other, 8, Color.web("#FFD54F"));
        }
    }

    private static void resolveCoronationFinal(Bird bird) {
        bird.pigeonCoronationFinalResolved = true;
        boolean strong = bird.pigeonCoronationStayedInside;
        int damage = strong ? Bird.PIGEON_CORONATION_FINAL_DAMAGE : Bird.PIGEON_CORONATION_WEAK_FINAL_DAMAGE;
        boolean hitAny = false;

        for (Bird other : bird.game.players) {
            if (!bird.canDamageTarget(other) || !inCoronationZone(bird, other)) {
                continue;
            }
            int targetIndex = other.playerIndex;
            if (targetIndex < 0 || targetIndex >= bird.pigeonCoronationFinalHit.length) {
                continue;
            }
            if (bird.pigeonCoronationFinalHit[targetIndex]) {
                continue;
            }
            bird.pigeonCoronationFinalHit[targetIndex] = true;

            double oldHealth = other.health;
            int dealt = bird.applyTrackedSpecialDamage(other, damage);
            if (dealt <= 0) {
                continue;
            }
            hitAny = true;

            double dx = other.bodyCenterX() - bird.pigeonCoronationX;
            double dy = other.bodyCenterY() - bird.pigeonCoronationY;
            double distance = Math.max(1.0, Math.hypot(dx, dy));
            double launchDir = Math.abs(dx) < 0.001 ? bird.facingDirection() : Math.signum(dx);
            double horizontal = (strong ? 10.8 : 7.0) * Math.max(0.48, Math.abs(dx) / distance);
            double launchX = launchDir * horizontal;
            double launchY = -(strong ? 13.2 : 8.8);
            if (dy > 0.0) {
                launchY -= strong ? 1.8 : 1.0;
            }
            other.vx += launchX;
            other.vy += launchY;
            boolean isKill = other.health <= 0 && oldHealth > 0;
            emitSpecialImpact(bird, other, launchX, launchY,
                    dealt, isKill, ROOFTOP_CORONATION_MOVE);
            emitCoronationHitParticles(bird, other, strong ? 28 : 18,
                    strong ? Color.GOLD : Color.web("#FFCC80"));
        }

        if (hitAny) {
            bird.game.shakeIntensity = Math.max(bird.game.shakeIntensity, strong ? 28.0 : 18.0);
            bird.game.hitstopFrames = Math.max(bird.game.hitstopFrames, strong ? 10 : 6);
            bird.game.triggerFlash(strong ? 0.38 : 0.22, false);
            if (bird.game.isSfxEnabled()) {
                bird.game.playCherrybombSfx();
            }
        }

        for (int i = 0; i < bird.scaledParticleCount(strong ? 72 : 44); i++) {
            double angle = bird.game.nextParticleRandom() * Math.PI * 2.0;
            double speed = (strong ? 5.0 : 3.2) + bird.game.nextParticleRandom() * (strong ? 9.0 : 5.5);
            bird.game.particles.add(new Particle(
                    bird.pigeonCoronationX + Math.cos(angle) * (18.0 + bird.game.nextParticleRandom() * 30.0),
                    bird.pigeonCoronationY + Math.sin(angle) * (18.0 + bird.game.nextParticleRandom() * 30.0),
                    Math.cos(angle) * speed,
                    Math.sin(angle) * speed - 2.6,
                    strong ? Color.GOLD.deriveColor(0, 1, 1, 0.88)
                            : Color.web("#FFCC80").deriveColor(0, 1, 1, 0.74)
            ));
        }
    }

    private static boolean inCoronationZone(Bird bird, Bird other) {
        double dx = other.bodyCenterX() - bird.pigeonCoronationX;
        double dy = other.bodyCenterY() - bird.pigeonCoronationY;
        double radius = Bird.PIGEON_CORONATION_RADIUS + Math.max(other.combatHalfWidth(), other.combatHalfHeight()) * 0.55;
        return dx * dx + dy * dy <= radius * radius;
    }

    private static void emitCoronationHitParticles(Bird bird, Bird other, int requested, Color color) {
        for (int i = 0; i < bird.scaledParticleCount(requested); i++) {
            double angle = bird.game.nextParticleRandom() * Math.PI * 2.0;
            double speed = 2.0 + bird.game.nextParticleRandom() * 5.5;
            bird.game.particles.add(new Particle(
                    other.bodyCenterX() + (bird.game.nextParticleRandom() - 0.5) * other.bodyWidth() * 0.5,
                    other.bodyCenterY() + (bird.game.nextParticleRandom() - 0.5) * other.bodyHeight() * 0.5,
                    Math.cos(angle) * speed,
                    Math.sin(angle) * speed - 1.4,
                    color.deriveColor(0, 1, 1, 0.76)
            ));
        }
    }

    private static void emitSpecialImpact(Bird bird, Bird other, double launchX, double launchY,
                                          int dealt, boolean isKill, String moveName) {
        bird.game.emitCombatImpact(bird, other, other.bodyCenterX(), other.bodyCenterY(),
                launchX, launchY, dealt, isKill, moveName);
        if (dealt < 8) {
            bird.game.playPigeonLightImpactSfx(dealt);
        }
    }

    private static double rushSpeed(Bird bird) {
        if (bird.pigeonRushGrounded) {
            return bird.pigeonRushUltimate ? 22.0 : 20.0;
        }
        return bird.pigeonRushUltimate ? 19.4 : 17.8;
    }

    private static int rushDamage(Bird bird) {
        return bird.pigeonRushUltimate ? 6 : 3;
    }

    private static double rushHorizontalLaunch(Bird bird) {
        return bird.pigeonRushUltimate ? 9.0 : 7.0;
    }

    private static double rushVerticalLaunch(Bird bird) {
        return bird.pigeonRushUltimate ? 11.8 : 9.2;
    }
}
