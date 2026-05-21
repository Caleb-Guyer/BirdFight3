package com.example.birdgame3;

import javafx.scene.paint.Color;

import java.util.Arrays;

final class RaptorSpecials {
    private RaptorSpecials() {
    }

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
        bird.vy = eagle ? (ultimate ? -17.4 : -15.6) : (ultimate ? -16.2 : -14.2);
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
            bird.game.particles.add(new Particle(predictX + laneOffset * 60.0, BirdGame3.GROUND_Y - 20, 0, -5 - Math.random() * 8, Color.ORANGERED.brighter()));
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
                || bird.eagleAscentActive;
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
            double spread = (Math.random() - 0.5) * (eagle ? 16.0 : 10.0);
            bird.game.particles.add(new Particle(
                    bird.bodyCenterX() + dir * (28 + Math.random() * 20),
                    bird.bodyCenterY() - 8 + spread,
                    dir * (2.6 + Math.random() * 2.4),
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
                double angle = Math.random() * Math.PI * 2;
                bird.game.particles.add(new Particle(
                        other.x + 40,
                        other.y + 40,
                        Math.cos(angle) * (3 + Math.random() * 5),
                        Math.sin(angle) * (3 + Math.random() * 5) - 2,
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
        double maxHorizontal = eagle ? 5.6 : 7.8;
        bird.vx = Math.clamp(bird.vx * (eagle ? 0.9 : 0.93) + inputDir * steer, -maxHorizontal, maxHorizontal);

        int strongLiftFrames = eagle
                ? (bird.raptorClimbUltimate ? 10 : 8)
                : (bird.raptorClimbUltimate ? 8 : 6);
        double lift = bird.raptorClimbTimer > strongLiftFrames
                ? (eagle
                    ? (bird.raptorClimbUltimate ? -13.8 : -12.2)
                    : (bird.raptorClimbUltimate ? -12.8 : -11.1))
                : (eagle
                    ? (bird.raptorClimbUltimate ? -10.0 : -8.7)
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
                double angle = -Math.PI / 2 + (Math.random() - 0.5) * 1.3;
                bird.game.particles.add(new Particle(
                        other.x + 40,
                        other.y + 40,
                        Math.cos(angle) * (4 + Math.random() * 5),
                        Math.sin(angle) * (7 + Math.random() * 7),
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
