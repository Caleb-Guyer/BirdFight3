package com.example.birdgame3;

import javafx.scene.paint.Color;

import java.util.Arrays;
import java.util.Iterator;

final class RoadrunnerSpecials {
    static final String REDLINE_EXECUTION_MOVE = "Redline Execution";
    static final int MOMENTUM_BUILD_GRACE_FRAMES = 48;
    static final int MOMENTUM_HIT_GRACE_FRAMES = 72;
    static final double MOMENTUM_STUN_DECAY_PER_FRAME = 0.75;
    static final double MOMENTUM_GROUND_DECAY_PER_FRAME = 0.22;
    static final double MOMENTUM_AIR_DECAY_PER_FRAME = 0.12;
    static final double MAX_MOMENTUM_DAMAGE_REDUCTION = 0.18;
    static final double MAX_MOMENTUM_DAMAGE_BONUS = 0.12;
    static final double CORE_SPECIAL_KNOCKBACK_MULTIPLIER = 1.15;

    private RoadrunnerSpecials() {
    }

    static void use(Bird bird, boolean ultimate) {
        if (ultimate) {
            startRedlineExecution(bird);
            return;
        }
        switch (bird.selectRoadrunnerSpecialVariant()) {
            case NEUTRAL -> neutral(bird, ultimate);
            case SIDE -> side(bird, ultimate);
            case UP -> up(bird, ultimate);
            case DOWN -> down(bird, ultimate);
        }
    }

    static void activateSandstorm(Bird bird) {
        bird.roadrunnerSandstormTimer = Math.max(bird.roadrunnerSandstormTimer, Bird.ROADRUNNER_SANDSTORM_FRAMES);
        bird.roadrunnerSandGustTimer = 0;
        Arrays.fill(bird.roadrunnerSandHitCooldown, 0);
        bird.roadrunnerMomentum = Bird.ROADRUNNER_MOMENTUM_MAX;
        bird.roadrunnerMomentumFxTimer = Math.max(bird.roadrunnerMomentumFxTimer, 90);
        bird.speedMultiplier = Math.max(bird.speedMultiplier, bird.baseSpeedMultiplier * Bird.ROADRUNNER_SANDSTORM_SPEED_SCALE);
        bird.speedTimer = Math.max(bird.speedTimer, Bird.ROADRUNNER_SANDSTORM_FRAMES + 45);
        bird.hoverRegenTimer = Math.max(bird.hoverRegenTimer, Bird.ROADRUNNER_SANDSTORM_FRAMES);
        bird.hoverRegenMultiplier = Math.max(bird.hoverRegenMultiplier, 1.12);
        bird.game.addToKillFeed(bird.shortName() + " ASCENDED IN A GODSTORM!");
        bird.game.shakeIntensity = Math.max(bird.game.shakeIntensity, 28);
        bird.game.hitstopFrames = Math.max(bird.game.hitstopFrames, 12);
        bird.game.triggerFlash(0.45, false);
        unleashSandGust(bird, true);
    }

    static void startRedlineExecution(Bird bird) {
        int dir = bird.horizontalInputDirection();
        if (dir == 0) {
            dir = bird.facingDirection();
        }
        bird.facingRight = dir > 0;
        clearNormalActionState(bird);
        clearRedlineState(bird);

        double startX = bird.bodyCenterX();
        double startY = bird.bodyCenterY() - 4.0 * bird.sizeMultiplier;
        double leftBound = bird.usesIslandBounds()
                ? bird.game.battlefieldLeftBound() + 70.0 * bird.sizeMultiplier
                : 70.0 * bird.sizeMultiplier;
        double rightBound = bird.usesIslandBounds()
                ? bird.game.battlefieldRightBound() - 70.0 * bird.sizeMultiplier
                : BirdGame3.WORLD_WIDTH - 70.0 * bird.sizeMultiplier;
        if (leftBound > rightBound) {
            double midpoint = (leftBound + rightBound) * 0.5;
            leftBound = midpoint;
            rightBound = midpoint;
        }
        double rawEndX = startX + dir * Bird.ROADRUNNER_REDLINE_RANGE * bird.sizeMultiplier;
        double endX = Math.clamp(rawEndX, leftBound, rightBound);

        bird.roadrunnerRedlineTimer = Bird.ROADRUNNER_REDLINE_DASH_FRAMES;
        bird.roadrunnerRedlineRecoveryTimer = 0;
        bird.roadrunnerRedlineCinematic = false;
        bird.roadrunnerRedlineDirection = dir;
        bird.roadrunnerRedlineStrikeIndex = 0;
        bird.roadrunnerRedlineFinalResolved = false;
        bird.roadrunnerRedlineStartX = startX;
        bird.roadrunnerRedlineStartY = startY;
        bird.roadrunnerRedlineEndX = endX;
        bird.roadrunnerRedlineEndY = startY;
        bird.roadrunnerRedlineAnchorX = startX;
        bird.roadrunnerRedlineAnchorY = startY;
        bird.roadrunnerRedlineLastStartX = startX;
        bird.roadrunnerRedlineLastStartY = startY;
        bird.roadrunnerRedlineLastEndX = startX;
        bird.roadrunnerRedlineLastEndY = startY;
        Arrays.fill(bird.roadrunnerRedlineCaught, false);

        bird.vx = dir * Bird.ROADRUNNER_REDLINE_DASH_SPEED;
        bird.vy *= 0.08;
        bird.attackAnimationTimer = Math.max(bird.attackAnimationTimer, Bird.ROADRUNNER_REDLINE_DASH_FRAMES + 8);
        bird.specialCooldown = 0;
        bird.specialMaxCooldown = 0;
        bird.isBlocking = false;
        bird.parryWindowFrames = 0;
        bird.shieldStunFrames = 0;
        bird.roadrunnerMomentum = Bird.ROADRUNNER_MOMENTUM_MAX;
        bird.roadrunnerMomentumFxTimer = Math.max(bird.roadrunnerMomentumFxTimer, 72);
        bird.game.addToKillFeed(bird.shortName() + " ENTERED THE REDLINE!");
        bird.game.shakeIntensity = Math.max(bird.game.shakeIntensity, 9);
        emitBurstDust(bird, startX - dir * 20.0 * bird.sizeMultiplier,
                bird.bodyBottomY() - 10.0 * bird.sizeMultiplier, dir, 38, Color.web("#FF1744"));
    }

    static double momentumRatio(Bird bird) {
        return Math.clamp(bird.roadrunnerMomentum / Bird.ROADRUNNER_MOMENTUM_MAX, 0.0, 1.0);
    }

    static double incomingDamageMultiplier(Bird bird) {
        if (bird == null || bird.type != BirdGame3.BirdType.ROADRUNNER) {
            return 1.0;
        }
        return 1.0 - momentumRatio(bird) * MAX_MOMENTUM_DAMAGE_REDUCTION;
    }

    static double outgoingDamageMultiplier(Bird bird) {
        if (bird == null || bird.type != BirdGame3.BirdType.ROADRUNNER) {
            return 1.0;
        }
        return 1.0 + momentumRatio(bird) * MAX_MOMENTUM_DAMAGE_BONUS;
    }

    static void addMomentum(Bird bird, double amount) {
        if (bird.type != BirdGame3.BirdType.ROADRUNNER || amount <= 0.0) {
            return;
        }
        double before = bird.roadrunnerMomentum;
        bird.roadrunnerMomentum = Math.clamp(bird.roadrunnerMomentum + amount, 0.0, Bird.ROADRUNNER_MOMENTUM_MAX);
        bird.roadrunnerMomentumGraceTimer = Math.max(
                bird.roadrunnerMomentumGraceTimer, MOMENTUM_BUILD_GRACE_FRAMES);
        if (bird.roadrunnerMomentum > before + 0.5) {
            bird.roadrunnerMomentumFxTimer = Math.max(bird.roadrunnerMomentumFxTimer, 18);
        }
    }

    static void onHitLanded(Bird bird) {
        if (bird == null || bird.type != BirdGame3.BirdType.ROADRUNNER || bird.health <= 0) {
            return;
        }
        bird.roadrunnerMomentumGraceTimer = Math.max(
                bird.roadrunnerMomentumGraceTimer, MOMENTUM_HIT_GRACE_FRAMES);
        bird.roadrunnerMomentumFxTimer = Math.max(bird.roadrunnerMomentumFxTimer, 24);
    }

    static void onDamageTaken(Bird bird, double scaledDamage) {
        if (bird == null || bird.type != BirdGame3.BirdType.ROADRUNNER || scaledDamage <= 0.0) {
            return;
        }
        double momentumLoss = Math.clamp(4.0 + scaledDamage * 0.45, 6.0, 18.0);
        bird.roadrunnerMomentum = Math.max(0.0, bird.roadrunnerMomentum - momentumLoss);
        bird.roadrunnerMomentumFxTimer = Math.max(bird.roadrunnerMomentumFxTimer, 12);
    }

    static void spendMomentum(Bird bird, double fraction) {
        if (fraction <= 0.0) {
            return;
        }
        bird.roadrunnerMomentum = Math.max(0.0, bird.roadrunnerMomentum * (1.0 - fraction));
    }

    static void neutral(Bird bird, boolean ultimate) {
        bird.roadrunnerBeepCharging = true;
        bird.roadrunnerBeepChargeFrames = Math.max(1, bird.roadrunnerBeepChargeFrames);
        bird.roadrunnerBeepMaxChargeHoldFrames = 0;
        bird.roadrunnerBeepUltimate = ultimate;
        int inputDir = bird.horizontalInputDirection();
        bird.roadrunnerBeepDirection = inputDir == 0 ? bird.facingDirection() : inputDir;
        bird.facingRight = bird.roadrunnerBeepDirection > 0;
        Arrays.fill(bird.roadrunnerBeepHit, false);
        bird.specialCooldown = 0;
        bird.specialMaxCooldown = 0;
        bird.attackAnimationTimer = Math.max(bird.attackAnimationTimer, 8);
        bird.vx *= bird.isOnGround() ? 0.72 : 0.84;
        if (ultimate) {
            bird.game.addToKillFeed(bird.shortName() + " wound up a golden Beep-Beep Blitz!");
        }
    }

    static void releaseBeepBlitz(Bird bird) {
        if (!bird.roadrunnerBeepCharging) {
            return;
        }
        int chargeFrames = bird.roadrunnerBeepChargeFrames;
        bird.roadrunnerBeepCharging = false;
        bird.roadrunnerBeepChargeFrames = 0;
        bird.roadrunnerBeepMaxChargeHoldFrames = 0;
        bird.roadrunnerBeepBurstTimer = bird.roadrunnerBeepUltimate
                ? Bird.ROADRUNNER_BEEP_BURST_FRAMES + 4
                : Bird.ROADRUNNER_BEEP_BURST_FRAMES;
        bird.roadrunnerBeepReuseTimer = bird.roadrunnerBeepUltimate ? 20 : Bird.ROADRUNNER_BEEP_REUSE_FRAMES;
        int releaseDir = bird.horizontalInputDirection();
        bird.roadrunnerBeepDirection = releaseDir == 0
                ? (bird.roadrunnerBeepDirection == 0 ? bird.facingDirection() : bird.roadrunnerBeepDirection)
                : releaseDir;
        bird.facingRight = bird.roadrunnerBeepDirection > 0;
        Arrays.fill(bird.roadrunnerBeepHit, false);

        double chargeRatio = Math.clamp(chargeFrames / (double) Bird.ROADRUNNER_BEEP_CHARGE_MAX_FRAMES, 0.0, 1.0);
        double carriedSpeedRatio = Math.clamp(Math.abs(bird.vx) / 26.0, 0.0, 1.0);
        double powerRatio = Math.clamp(chargeRatio * 0.82 + momentumRatio(bird) * 0.55 + carriedSpeedRatio * 0.34, 0.0, 1.35);
        double burstSpeed = 12.0 + powerRatio * 16.5;
        double chargeSpeedFloor = 16.0 + chargeRatio * 18.0;
        if (chargeRatio >= 0.98) {
            chargeSpeedFloor = (bird.roadrunnerBeepUltimate ? 56.0 : 50.0) + momentumRatio(bird) * 2.0;
        }
        burstSpeed = Math.max(burstSpeed, chargeSpeedFloor);
        bird.vx = bird.roadrunnerBeepDirection * burstSpeed;
        bird.vy = Math.min(bird.vy, bird.isOnGround() ? -1.6 - powerRatio * 2.2 : -3.0 - powerRatio * 2.0);
        bird.attackAnimationTimer = Math.max(bird.attackAnimationTimer, bird.roadrunnerBeepBurstTimer + 2);
        bird.specialCooldown = 0;
        bird.specialMaxCooldown = 0;
        spendMomentum(bird, bird.roadrunnerBeepUltimate ? 0.22 : 0.42);
        bird.game.shakeIntensity = Math.max(bird.game.shakeIntensity, 4 + powerRatio * 5.0);
        applyBeepBlitzHit(bird, powerRatio);
        emitBurstDust(bird, bird.bodyCenterX(), bird.bodyBottomY() - 12.0 * bird.sizeMultiplier,
                bird.roadrunnerBeepDirection, bird.roadrunnerBeepUltimate ? 36 : 24,
                sandColor(bird, bird.roadrunnerBeepUltimate));
    }

    static void applyBeepBlitzHit(Bird bird, double powerRatio) {
        if (bird.roadrunnerBeepBurstTimer <= 0) {
            return;
        }
        int dir = bird.roadrunnerBeepDirection == 0 ? bird.facingDirection() : bird.roadrunnerBeepDirection;
        double reach = (120.0 + powerRatio * 44.0) * bird.sizeMultiplier;
        double verticalReach = (76.0 + powerRatio * 24.0) * bird.sizeMultiplier;
        for (Bird other : bird.game.players) {
            if (!bird.canDamageTarget(other)) continue;
            if (other.playerIndex < 0 || other.playerIndex >= bird.roadrunnerBeepHit.length) continue;
            if (bird.roadrunnerBeepHit[other.playerIndex]) continue;
            double dx = other.bodyCenterX() - bird.bodyCenterX();
            double dy = other.bodyCenterY() - (bird.bodyCenterY() - 6.0 * bird.sizeMultiplier);
            if (dx * dir < -30.0 * bird.sizeMultiplier || dx * dir > reach + other.combatHalfWidth()) continue;
            if (Math.abs(dy) > verticalReach + other.combatHalfHeight()) continue;

            double oldHealth = other.health;
            int dmg = (int) Math.round((bird.roadrunnerBeepUltimate ? 8 : 6) + powerRatio * (bird.roadrunnerBeepUltimate ? 7 : 5));
            int dealt = (int) bird.applyDamageTo(other, dmg);
            bird.roadrunnerBeepHit[other.playerIndex] = true;
            if (dealt <= 0) continue;
            bird.game.damageDealt[bird.playerIndex] += dealt;
            bird.game.recordSpecialImpact(bird.playerIndex, dealt, true);
            if (other.health <= 0 && oldHealth > 0) bird.game.eliminations[bird.playerIndex]++;
            other.vx += dir * (8.0 + powerRatio * 8.5) * CORE_SPECIAL_KNOCKBACK_MULTIPLIER;
            other.vy -= (3.0 + powerRatio * 3.5) * CORE_SPECIAL_KNOCKBACK_MULTIPLIER;
            addMomentum(bird, 4.5 + dealt * 0.55);
            emitBurstDust(bird, other.bodyCenterX(), other.bodyCenterY(), dir, 16,
                    trailColor(bird, bird.roadrunnerBeepUltimate));
        }
    }

    static void side(Bird bird, boolean ultimate) {
        int dir = bird.horizontalInputDirection();
        if (dir == 0) {
            dir = bird.facingDirection();
        }
        bird.facingRight = dir > 0;
        double ratio = momentumRatio(bird);
        double existingSpeed = Math.abs(bird.vx);
        bird.roadrunnerRicochetDirection = dir;
        int travelBonusFrames = Math.min(8, (int) Math.round(existingSpeed * 0.24));
        bird.roadrunnerRicochetTimer = (ultimate ? Bird.ROADRUNNER_RICOCHET_FRAMES + 7 : Bird.ROADRUNNER_RICOCHET_FRAMES) + travelBonusFrames;
        bird.roadrunnerRicochetReuseTimer = ultimate ? 34 : Bird.ROADRUNNER_RICOCHET_REUSE_FRAMES;
        bird.roadrunnerRicochetBounces = ultimate ? 2 : 1;
        bird.roadrunnerRicochetSpeed = Math.clamp(14.0 + ratio * 7.2 + existingSpeed * 0.62 + (ultimate ? 3.0 : 0.0),
                17.5, ultimate ? 34.0 : 30.0);
        bird.roadrunnerRicochetUltimate = ultimate;
        Arrays.fill(bird.roadrunnerRicochetHitCooldown, 0);
        bird.vx = dir * bird.roadrunnerRicochetSpeed;
        bird.vy *= 0.22;
        bird.attackAnimationTimer = Math.max(bird.attackAnimationTimer, bird.roadrunnerRicochetTimer);
        bird.specialCooldown = 0;
        bird.specialMaxCooldown = 0;
        bird.isBlocking = false;
        bird.parryWindowFrames = 0;
        bird.shieldStunFrames = 0;
        spendMomentum(bird, ultimate ? 0.18 : 0.38);
        emitBurstDust(bird, bird.bodyCenterX() - dir * 24.0 * bird.sizeMultiplier,
                bird.bodyBottomY() - 10.0 * bird.sizeMultiplier, dir, ultimate ? 42 : 28,
                warmDustColor(bird, ultimate));
    }

    static void up(Bird bird, boolean ultimate) {
        if (bird.roadrunnerDustDevilUsed && !ultimate) {
            return;
        }
        double ratio = momentumRatio(bird);
        bird.roadrunnerDustDevilUsed = true;
        bird.roadrunnerDustDevilUltimate = ultimate;
        bird.roadrunnerDustDevilTimer = ultimate ? Bird.ROADRUNNER_DUST_DEVIL_FRAMES + 8 : Bird.ROADRUNNER_DUST_DEVIL_FRAMES;
        Arrays.fill(bird.roadrunnerDustDevilHit, false);
        bird.canDoubleJump = true;
        bird.vx *= 0.32;
        bird.vy = Math.min(bird.vy, -(17.0 + ratio * 7.0 + (ultimate ? 4.0 : 0.0)));
        bird.attackAnimationTimer = Math.max(bird.attackAnimationTimer, 14);
        bird.specialCooldown = 0;
        bird.specialMaxCooldown = 0;
        spendMomentum(bird, ultimate ? 0.16 : 0.34);
        emitBurstDust(bird, bird.bodyCenterX(), bird.bodyBottomY() - 4.0 * bird.sizeMultiplier,
                bird.facingDirection(), ultimate ? 54 : 36, sandColor(bird, ultimate));
    }

    static void down(Bird bird, boolean ultimate) {
        int dir = bird.horizontalInputDirection();
        if (dir == 0) {
            dir = bird.facingDirection();
        }
        bird.facingRight = dir > 0;
        double roadX = bird.bodyCenterX() + dir * 64.0 * bird.sizeMultiplier;
        double roadY = roadSurfaceY(bird, roadX);
        bird.roadrunnerPaintedRoads.clear();
        bird.roadrunnerPaintedRoads.add(new Bird.RoadrunnerPaintedRoad(roadX, roadY, dir, ultimate));
        bird.roadrunnerPaintedRoadReuseTimer = ultimate ? 38 : Bird.ROADRUNNER_PAINTED_ROAD_REUSE_FRAMES;
        bird.specialCooldown = 0;
        bird.specialMaxCooldown = 0;
        bird.attackAnimationTimer = Math.max(bird.attackAnimationTimer, 12);
        double launchSpeed = ultimate ? 11.5 : 8.4;
        bird.vx = dir * Math.max(Math.abs(bird.vx) * 0.55, launchSpeed);
        bird.vy = Math.min(bird.vy, bird.isOnGround() ? -1.2 : -2.4);
        addMomentum(bird, ultimate ? 6.5 : 3.8);
        if (ultimate) {
            bird.game.addToKillFeed(bird.shortName() + " painted a golden fake road!");
        }
        emitBurstDust(bird, roadX, roadY - 8.0, dir, ultimate ? 30 : 20,
                trailColor(bird, ultimate));
    }

    static double roadSurfaceY(Bird bird, double roadX) {
        double bestY = bird.hasSolidGroundFloorUnderBody() ? BirdGame3.GROUND_Y : Double.POSITIVE_INFINITY;
        double sourceY = bird.bodyBottomY() - 22.0 * bird.sizeMultiplier;
        for (Platform p : bird.game.platforms) {
            boolean isCaveCeiling = bird.game.selectedMap == BirdGame3.MapType.CAVE
                    && p.y <= 1 && p.h >= 60 && p.w >= BirdGame3.WORLD_WIDTH - 10;
            if (isCaveCeiling) continue;
            if (roadX < p.x - 36.0 || roadX > p.x + p.w + 36.0) continue;
            if (p.y < sourceY - 18.0) continue;
            if (p.y < bestY) {
                bestY = p.y;
            }
        }
        return Double.isFinite(bestY) ? bestY : bird.bodyBottomY() + 8.0 * bird.sizeMultiplier;
    }

    static Color trailColor(Bird bird, boolean ultimate) {
        if (ultimate) {
            return Color.GOLD;
        }
        return bird.isMirageSkin ? Color.web("#80DEEA") : Color.web("#90CAF9");
    }

    static Color sandColor(Bird bird, boolean ultimate) {
        if (ultimate) {
            return Color.GOLD;
        }
        return bird.isMirageSkin ? Color.web("#DFFBFF") : Color.web("#E6C46F");
    }

    static Color warmDustColor(Bird bird, boolean ultimate) {
        if (ultimate) {
            return Color.GOLD;
        }
        return bird.isMirageSkin ? Color.web("#B2EBF2") : Color.web("#D9A04D");
    }

    static void emitBurstDust(Bird bird, double centerX, double centerY, int dir, int baseCount, Color color) {
        int particleCount = bird.scaledParticleCount(baseCount);
        for (int i = 0; i < particleCount; i++) {
            double side = (bird.game.nextParticleRandom() - 0.5) * 2.0;
            double speed = 2.0 + bird.game.nextParticleRandom() * 7.5;
            bird.game.particles.add(new Particle(
                    centerX + side * 22.0 * bird.sizeMultiplier,
                    centerY + (bird.game.nextParticleRandom() - 0.5) * 15.0 * bird.sizeMultiplier,
                    -dir * (1.0 + bird.game.nextParticleRandom() * 2.0) + side * 0.8,
                    -1.2 - bird.game.nextParticleRandom() * speed * 0.38,
                    color.deriveColor(0, 1, 1, 0.62 + bird.game.nextParticleRandom() * 0.18)
            ));
        }
    }

    static void handleSandstorm(Bird bird) {
        if (!sandstormActive(bird)) {
            return;
        }

        bird.speedMultiplier = Math.max(bird.speedMultiplier, bird.baseSpeedMultiplier * Bird.ROADRUNNER_SANDSTORM_SPEED_SCALE);
        double centerX = bird.bodyCenterX();
        double centerY = bird.bodyCenterY();
        double intensity = Math.clamp(bird.roadrunnerSandstormTimer / (double) Bird.ROADRUNNER_SANDSTORM_FRAMES, 0.32, 1.0);
        int particleCount = Math.max(3, bird.scaledParticleCount(5));
        for (int i = 0; i < particleCount; i++) {
            double angle = bird.game.nextParticleRandom() * Math.PI * 2;
            double ring = 20.0 + bird.game.nextParticleRandom() * (105.0 + intensity * 90.0);
            double swirl = 2.6 + bird.game.nextParticleRandom() * 5.5 + intensity * 1.2;
            Color sand = bird.game.nextParticleRandom() < 0.72 ? Color.web("#E8C06A") : Color.web("#C68A3A");
            if (bird.isMirageSkin) {
                sand = bird.game.nextParticleRandom() < 0.72 ? Color.web("#DFFBFF") : Color.web("#80DEEA");
            }
            bird.game.particles.add(new Particle(
                    centerX + Math.cos(angle) * ring * 0.32,
                    centerY + Math.sin(angle) * ring * 0.22,
                    Math.cos(angle + Math.PI / 2.0) * swirl + bird.vx * 0.12,
                    Math.sin(angle + Math.PI / 2.0) * swirl - 1.2 - intensity,
                    sand.deriveColor(0, 1, 1, 0.56 + intensity * 0.22)
            ));
        }

        if (bird.roadrunnerSandGustTimer <= 0) {
            bird.roadrunnerSandGustTimer = Bird.ROADRUNNER_GUST_INTERVAL;
            unleashSandGust(bird, false);
        }
    }

    static void unleashSandGust(Bird bird, boolean openingBurst) {
        double centerX = bird.bodyCenterX();
        double centerY = bird.bodyCenterY();
        double radius = openingBurst ? 440.0 : Bird.ROADRUNNER_SANDSTORM_GUST_RADIUS;
        double forwardBias = bird.facingRight ? 1.0 : -1.0;

        for (Bird other : bird.game.players) {
            if (!bird.canDamageTarget(other)) continue;
            double dx = other.bodyCenterX() - centerX;
            double dy = other.bodyCenterY() - centerY;
            double dist = Math.hypot(dx, dy);
            double maxReach = radius + other.combatRadius();
            if (dist > maxReach) continue;

            double safeDist = Math.max(0.001, dist);
            double proximity = 1.0 - Math.clamp(dist / maxReach, 0.0, 1.0);
            double push = (openingBurst ? 14.0 : 7.0) + proximity * (openingBurst ? 14.0 : 9.0);
            other.vx += dx / safeDist * push + forwardBias * (openingBurst ? 3.2 : 1.4);
            other.vy -= (openingBurst ? 4.5 : 2.0) + proximity * (openingBurst ? 6.0 : 4.0);

            boolean canHit = openingBurst || bird.roadrunnerSandHitCooldown[other.playerIndex] <= 0;
            if (!canHit) {
                continue;
            }

            int dmg;
            if (openingBurst) {
                dmg = dist < 170.0 ? 12 : (dist < 300.0 ? 8 : 5);
            } else {
                dmg = dist < 170.0 ? 5 : 3;
            }
            double oldHealth = other.health;
            int dealt = (int) bird.applyDamageTo(other, dmg);
            if (dealt <= 0) {
                continue;
            }

            bird.roadrunnerSandHitCooldown[other.playerIndex] = Bird.ROADRUNNER_GUST_HIT_COOLDOWN;
            bird.game.damageDealt[bird.playerIndex] += dealt;
            bird.game.recordSpecialImpact(bird.playerIndex, dealt, true);
            if (other.health <= 0 && oldHealth > 0) {
                bird.game.eliminations[bird.playerIndex]++;
            }
        }

        int particleCount = bird.scaledParticleCount(openingBurst ? 180 : 72);
        for (int i = 0; i < particleCount; i++) {
            double angle = bird.game.nextParticleRandom() * Math.PI * 2;
            double ring = 36.0 + bird.game.nextParticleRandom() * radius;
            double tangential = (3.5 + bird.game.nextParticleRandom() * 8.0) * (bird.facingRight ? 1.0 : -1.0);
            Color sand = bird.game.nextParticleRandom() < 0.72 ? Color.web("#E6C46F") : Color.web("#BA7B31");
            if (bird.isMirageSkin) {
                sand = bird.game.nextParticleRandom() < 0.72 ? Color.web("#DFFBFF") : Color.web("#80DEEA");
            }
            bird.game.particles.add(new Particle(
                    centerX + Math.cos(angle) * ring * 0.24,
                    centerY + Math.sin(angle) * ring * 0.16,
                    Math.cos(angle) * (openingBurst ? 8.5 : 5.2) + tangential * 0.55,
                    Math.sin(angle) * (openingBurst ? 6.0 : 3.4) - (openingBurst ? 2.8 : 1.6),
                    sand.deriveColor(0, 1, 1, openingBurst ? 0.84 : 0.72)
            ));
        }
    }

    static void handleState(Bird bird, boolean specialHeld, boolean grabbed) {
        if (bird.type != BirdGame3.BirdType.ROADRUNNER && !bird.mockingbirdCopiedNeutralFrom(BirdGame3.BirdType.ROADRUNNER)) {
            return;
        }

        handleMomentum(bird);

        if (bird.roadrunnerRedlineTimer > 0
                || bird.roadrunnerRedlineRecoveryTimer > 0
                || bird.roadrunnerRedlineCinematic) {
            handleRedlineExecution(bird, grabbed);
            return;
        }

        if (bird.roadrunnerBeepCharging) {
            bird.roadrunnerBeepChargeFrames = Math.min(Bird.ROADRUNNER_BEEP_CHARGE_MAX_FRAMES, bird.roadrunnerBeepChargeFrames + 1);
            addMomentum(bird, bird.roadrunnerBeepUltimate ? 0.72 : 0.42);
            double chargeRatio = Math.clamp(bird.roadrunnerBeepChargeFrames / (double) Bird.ROADRUNNER_BEEP_CHARGE_MAX_FRAMES, 0.0, 1.0);
            double vibration = 0.10 + chargeRatio * 0.45;
            double vibrationRate = 0.26 + chargeRatio * 0.48;
            bird.vx = bird.vx * (bird.isOnGround() ? 0.88 : 0.92) + Math.sin(bird.roadrunnerBeepChargeFrames * vibrationRate) * vibration;
            int dustInterval = Math.max(1, 4 - (int) Math.floor(chargeRatio * 3.0));
            if (bird.roadrunnerBeepChargeFrames % dustInterval == 0) {
                emitBurstDust(bird, bird.bodyCenterX() - bird.facingDirection() * 18.0 * bird.sizeMultiplier,
                        bird.bodyBottomY() - 8.0 * bird.sizeMultiplier,
                        bird.facingDirection(),
                        bird.roadrunnerBeepUltimate ? 5 : 3,
                        sandColor(bird, bird.roadrunnerBeepUltimate));
            }
            if (bird.roadrunnerBeepChargeFrames >= Bird.ROADRUNNER_BEEP_CHARGE_MAX_FRAMES) {
                bird.roadrunnerBeepMaxChargeHoldFrames++;
            } else {
                bird.roadrunnerBeepMaxChargeHoldFrames = 0;
            }
            if (!specialHeld || bird.health <= 0 || bird.stunTime > 0.0 || grabbed) {
                releaseBeepBlitz(bird);
            } else if (bird.roadrunnerBeepMaxChargeHoldFrames >= Bird.ROADRUNNER_BEEP_MAX_HOLD_RELEASE_FRAMES) {
                releaseBeepBlitz(bird);
            }
        }

        if (bird.roadrunnerBeepBurstTimer > 0) {
            double maxFrames = bird.roadrunnerBeepUltimate ? Bird.ROADRUNNER_BEEP_BURST_FRAMES + 4.0 : Bird.ROADRUNNER_BEEP_BURST_FRAMES;
            double powerRatio = Math.clamp(bird.roadrunnerBeepBurstTimer / maxFrames, 0.0, 1.0);
            applyBeepBlitzHit(bird, 0.35 + powerRatio * 0.65);
        }

        if (bird.roadrunnerRicochetTimer > 0) {
            handleRicochet(bird);
        }

        if (bird.roadrunnerDustDevilTimer > 0) {
            handleDustDevil(bird);
        }
    }

    static void handleRedlineExecution(Bird bird, boolean grabbed) {
        if (bird.roadrunnerRedlineTimer <= 0 && bird.roadrunnerRedlineRecoveryTimer <= 0) {
            clearRedlineState(bird);
            return;
        }

        if (bird.roadrunnerRedlineRecoveryTimer > 0) {
            bird.vx *= bird.isOnGround() ? 0.62 : 0.76;
            bird.vy *= 0.94;
            if (bird.roadrunnerRedlineRecoveryTimer <= 1 && bird.roadrunnerRedlineTimer <= 0) {
                clearRedlineState(bird);
            }
            return;
        }

        if (!bird.roadrunnerRedlineCinematic) {
            handleRedlineDash(bird, grabbed);
            return;
        }

        handleRedlineCinematic(bird);
    }

    private static void handleRedlineDash(Bird bird, boolean grabbed) {
        if (grabbed || bird.health <= 0) {
            startRedlineWhiffRecovery(bird);
            return;
        }

        int dir = bird.roadrunnerRedlineDirection == 0 ? bird.facingDirection() : bird.roadrunnerRedlineDirection;
        bird.roadrunnerRedlineDirection = dir;
        bird.facingRight = dir > 0;
        bird.vx = dir * Bird.ROADRUNNER_REDLINE_DASH_SPEED;
        bird.vy *= 0.20;

        double previousX = bird.roadrunnerRedlineLastEndX;
        double previousY = bird.roadrunnerRedlineLastEndY;
        double currentX = bird.bodyCenterX();
        double currentY = bird.bodyCenterY() - 4.0 * bird.sizeMultiplier;
        bird.roadrunnerRedlineLastStartX = previousX;
        bird.roadrunnerRedlineLastStartY = previousY;
        bird.roadrunnerRedlineLastEndX = currentX;
        bird.roadrunnerRedlineLastEndY = currentY;

        if ((bird.roadrunnerRedlineTimer & 1) == 0) {
            emitBurstDust(bird, currentX - dir * 28.0 * bird.sizeMultiplier,
                    bird.bodyBottomY() - 12.0 * bird.sizeMultiplier, dir, 10, Color.web("#FF5252"));
        }

        if (collectRedlineCaughtInSegment(bird, previousX, previousY, currentX, currentY)) {
            startRedlineCinematic(bird);
            return;
        }

        boolean passedEnd = dir > 0
                ? currentX >= bird.roadrunnerRedlineEndX
                : currentX <= bird.roadrunnerRedlineEndX;
        if (bird.roadrunnerRedlineTimer <= 1 || passedEnd) {
            startRedlineWhiffRecovery(bird);
        }
    }

    private static boolean collectRedlineCaughtInSegment(Bird bird, double x0, double y0, double x1, double y1) {
        boolean caughtAny = false;
        double minX = Math.min(x0, x1);
        double maxX = Math.max(x0, x1);
        double laneY = (y0 + y1) * 0.5;
        double lanePad = Bird.ROADRUNNER_REDLINE_LANE_HALF_HEIGHT * bird.sizeMultiplier;
        for (Bird other : bird.game.players) {
            if (!bird.canDamageTarget(other)) continue;
            if (other.playerIndex < 0 || other.playerIndex >= bird.roadrunnerRedlineCaught.length) continue;
            if (bird.roadrunnerRedlineCaught[other.playerIndex]) continue;
            double otherX = other.bodyCenterX();
            double otherY = other.bodyCenterY();
            double reachPad = other.combatHalfWidth() + 42.0 * bird.sizeMultiplier;
            if (otherX < minX - reachPad || otherX > maxX + reachPad) continue;
            if (Math.abs(otherY - laneY) > lanePad + other.combatHalfHeight()) continue;
            bird.roadrunnerRedlineCaught[other.playerIndex] = true;
            caughtAny = true;
        }
        return caughtAny;
    }

    private static void startRedlineCinematic(Bird bird) {
        int caughtCount = redlineCaughtCount(bird);
        if (caughtCount <= 0) {
            startRedlineWhiffRecovery(bird);
            return;
        }

        double sumX = 0.0;
        double sumY = 0.0;
        for (Bird other : bird.game.players) {
            if (!isRedlineCaught(bird, other)) continue;
            sumX += other.bodyCenterX();
            sumY += other.bodyCenterY();
        }
        double anchorX = sumX / caughtCount;
        double anchorY = sumY / caughtCount;
        double leftBound = bird.usesIslandBounds()
                ? bird.game.battlefieldLeftBound() + 150.0 * bird.sizeMultiplier
                : 150.0 * bird.sizeMultiplier;
        double rightBound = bird.usesIslandBounds()
                ? bird.game.battlefieldRightBound() - 150.0 * bird.sizeMultiplier
                : BirdGame3.WORLD_WIDTH - 150.0 * bird.sizeMultiplier;
        if (leftBound > rightBound) {
            double midpoint = (leftBound + rightBound) * 0.5;
            leftBound = midpoint;
            rightBound = midpoint;
        }
        anchorX = Math.clamp(anchorX, leftBound, rightBound);
        anchorY = Math.clamp(anchorY, BirdGame3.CEILING_Y + 180.0 * bird.sizeMultiplier,
                BirdGame3.WORLD_HEIGHT - 260.0 * bird.sizeMultiplier);

        bird.roadrunnerRedlineTimer = Bird.ROADRUNNER_REDLINE_CINEMATIC_FRAMES;
        bird.roadrunnerRedlineCinematic = true;
        bird.roadrunnerRedlineRecoveryTimer = 0;
        bird.roadrunnerRedlineStrikeIndex = 0;
        bird.roadrunnerRedlineFinalResolved = false;
        bird.roadrunnerRedlineAnchorX = anchorX;
        bird.roadrunnerRedlineAnchorY = anchorY;
        bird.vx = 0.0;
        bird.vy = 0.0;
        bird.attackAnimationTimer = Math.max(bird.attackAnimationTimer, Bird.ROADRUNNER_REDLINE_CINEMATIC_FRAMES + 8);
        bird.game.addToKillFeed(bird.shortName() + " CAUGHT THE REDLINE!");
        bird.game.shakeIntensity = Math.max(bird.game.shakeIntensity, 13);
        bird.game.triggerFlash(0.26, false);
        emitBurstDust(bird, anchorX, anchorY, bird.roadrunnerRedlineDirection, 48, Color.web("#FFD54F"));
    }

    private static void handleRedlineCinematic(Bird bird) {
        int elapsed = Bird.ROADRUNNER_REDLINE_CINEMATIC_FRAMES - bird.roadrunnerRedlineTimer;
        int dir = bird.roadrunnerRedlineDirection == 0 ? bird.facingDirection() : bird.roadrunnerRedlineDirection;
        bird.roadrunnerRedlineDirection = dir;
        positionRedlineRunner(bird, elapsed, dir);
        if (!bird.roadrunnerRedlineFinalResolved) {
            positionRedlineTargets(bird, elapsed);
        }

        int expectedStrikes = Math.min(Bird.ROADRUNNER_REDLINE_STRIKE_COUNT,
                elapsed / Bird.ROADRUNNER_REDLINE_STRIKE_INTERVAL);
        while (bird.roadrunnerRedlineStrikeIndex < expectedStrikes) {
            applyRedlineStrike(bird, bird.roadrunnerRedlineStrikeIndex, false);
            bird.roadrunnerRedlineStrikeIndex++;
        }

        if (!bird.roadrunnerRedlineFinalResolved && elapsed >= Bird.ROADRUNNER_REDLINE_FINAL_FRAME) {
            applyRedlineStrike(bird, Bird.ROADRUNNER_REDLINE_STRIKE_COUNT, true);
            bird.roadrunnerRedlineFinalResolved = true;
            bird.game.shakeIntensity = Math.max(bird.game.shakeIntensity, 24);
            bird.game.hitstopFrames = Math.max(bird.game.hitstopFrames, 3);
            bird.game.triggerFlash(0.34, false);
        }

        if (bird.roadrunnerRedlineTimer <= 1) {
            endRedlineCinematic(bird);
        }
    }

    private static void positionRedlineRunner(Bird bird, int elapsed, int dir) {
        double s = bird.sizeMultiplier;
        double anchorX = bird.roadrunnerRedlineAnchorX;
        double anchorY = bird.roadrunnerRedlineAnchorY;
        if (bird.roadrunnerRedlineFinalResolved) {
            bird.x = anchorX + dir * 132.0 * s - bird.bodyWidth() * 0.5;
            bird.y = anchorY - 48.0 * s - bird.bodyHeight() * 0.5;
            bird.facingRight = dir < 0;
        } else {
            double phase = elapsed * 0.54;
            double side = ((elapsed / Math.max(1, Bird.ROADRUNNER_REDLINE_STRIKE_INTERVAL)) & 1) == 0 ? -dir : dir;
            double orbitX = side * (124.0 + Math.sin(phase * 0.7) * 42.0) * s;
            double orbitY = (-58.0 + Math.cos(phase) * 62.0) * s;
            bird.x = anchorX + orbitX - bird.bodyWidth() * 0.5;
            bird.y = anchorY + orbitY - bird.bodyHeight() * 0.5;
            bird.facingRight = anchorX >= bird.bodyCenterX();
        }
        bird.vx = 0.0;
        bird.vy = 0.0;
    }

    private static void positionRedlineTargets(Bird bird, int elapsed) {
        int caughtCount = Math.max(1, redlineCaughtCount(bird));
        int slot = 0;
        for (Bird other : bird.game.players) {
            if (!isRedlineCaught(bird, other) || other.health <= 0) continue;
            double centeredSlot = slot - (caughtCount - 1) * 0.5;
            double sway = Math.sin(elapsed * 0.36 + slot * 1.7) * 9.0 * bird.sizeMultiplier;
            double xOffset = centeredSlot * 34.0 * bird.sizeMultiplier + sway;
            double yOffset = Math.cos(elapsed * 0.28 + slot) * 11.0 * bird.sizeMultiplier;
            other.x = bird.roadrunnerRedlineAnchorX + xOffset - other.bodyWidth() * 0.5;
            other.y = bird.roadrunnerRedlineAnchorY + yOffset - other.bodyHeight() * 0.5;
            other.vx = 0.0;
            other.vy = 0.0;
            other.applyStun(8.0);
            other.knockdownTimer = Math.max(other.knockdownTimer, 4);
            slot++;
        }
    }

    private static void applyRedlineStrike(Bird bird, int strikeIndex, boolean finisher) {
        int dir = bird.roadrunnerRedlineDirection == 0 ? bird.facingDirection() : bird.roadrunnerRedlineDirection;
        double startAngle = -0.85 + strikeIndex * 0.42;
        double startRadius = (finisher ? 285.0 : 185.0) * bird.sizeMultiplier;
        bird.roadrunnerRedlineLastStartX = bird.roadrunnerRedlineAnchorX - dir * startRadius;
        bird.roadrunnerRedlineLastStartY = bird.roadrunnerRedlineAnchorY + Math.sin(startAngle) * 92.0 * bird.sizeMultiplier;
        bird.roadrunnerRedlineLastEndX = bird.roadrunnerRedlineAnchorX + dir * startRadius;
        bird.roadrunnerRedlineLastEndY = bird.roadrunnerRedlineAnchorY - Math.sin(startAngle) * 82.0 * bird.sizeMultiplier;

        int rawDamage = finisher ? Bird.ROADRUNNER_REDLINE_FINAL_DAMAGE : Bird.ROADRUNNER_REDLINE_STRIKE_DAMAGE;
        for (Bird other : bird.game.players) {
            if (!isRedlineCaught(bird, other) || other.health <= 0) continue;
            int dealt = bird.applyTrackedSpecialDamage(other, rawDamage);
            if (dealt <= 0) continue;
            if (finisher) {
                double away = Math.signum(other.bodyCenterX() - bird.roadrunnerRedlineAnchorX);
                if (away == 0.0) {
                    away = dir;
                }
                other.vx = away * (20.0 + dealt * 0.18);
                other.vy = -15.0 - dealt * 0.06;
                other.applyStun(14.0);
                emitBurstDust(bird, other.bodyCenterX(), other.bodyCenterY(), (int) away, 34, Color.web("#FF1744"));
            } else {
                other.vx += dir * (4.0 + strikeIndex);
                other.vy -= 2.2;
                emitBurstDust(bird, other.bodyCenterX(), other.bodyCenterY(), dir, 12, Color.web("#FFAB40"));
                bird.game.shakeIntensity = Math.max(bird.game.shakeIntensity, 5);
            }
        }
    }

    private static void startRedlineWhiffRecovery(Bird bird) {
        int dir = bird.roadrunnerRedlineDirection == 0 ? bird.facingDirection() : bird.roadrunnerRedlineDirection;
        bird.roadrunnerRedlineTimer = 0;
        bird.roadrunnerRedlineCinematic = false;
        bird.roadrunnerRedlineRecoveryTimer = Bird.ROADRUNNER_REDLINE_RECOVERY_FRAMES;
        bird.roadrunnerRedlineFinalResolved = false;
        Arrays.fill(bird.roadrunnerRedlineCaught, false);
        bird.vx = dir * 8.0;
        bird.vy *= 0.55;
        bird.attackAnimationTimer = Math.max(bird.attackAnimationTimer, Bird.ROADRUNNER_REDLINE_RECOVERY_FRAMES);
        emitBurstDust(bird, bird.bodyCenterX(), bird.bodyBottomY() - 8.0 * bird.sizeMultiplier, dir, 16, Color.web("#6D4C41"));
    }

    private static void endRedlineCinematic(Bird bird) {
        int dir = bird.roadrunnerRedlineDirection == 0 ? bird.facingDirection() : bird.roadrunnerRedlineDirection;
        bird.roadrunnerRedlineTimer = 0;
        bird.roadrunnerRedlineRecoveryTimer = Bird.ROADRUNNER_REDLINE_RECOVERY_FRAMES;
        bird.roadrunnerRedlineCinematic = false;
        bird.vx = dir * 10.0;
        bird.vy = Math.min(bird.vy, -2.0);
        bird.attackAnimationTimer = Math.max(bird.attackAnimationTimer, Bird.ROADRUNNER_REDLINE_RECOVERY_FRAMES);
    }

    private static int redlineCaughtCount(Bird bird) {
        int caught = 0;
        for (Bird other : bird.game.players) {
            if (isRedlineCaught(bird, other)) {
                caught++;
            }
        }
        return caught;
    }

    private static boolean isRedlineCaught(Bird bird, Bird other) {
        return other != null
                && other.playerIndex >= 0
                && other.playerIndex < bird.roadrunnerRedlineCaught.length
                && bird.roadrunnerRedlineCaught[other.playerIndex];
    }

    private static void clearNormalActionState(Bird bird) {
        bird.roadrunnerBeepCharging = false;
        bird.roadrunnerBeepChargeFrames = 0;
        bird.roadrunnerBeepMaxChargeHoldFrames = 0;
        bird.roadrunnerBeepBurstTimer = 0;
        bird.roadrunnerRicochetTimer = 0;
        bird.roadrunnerRicochetSpeed = 0.0;
        bird.roadrunnerDustDevilTimer = 0;
        bird.roadrunnerRoadBoostTimer = 0;
        Arrays.fill(bird.roadrunnerBeepHit, false);
        Arrays.fill(bird.roadrunnerRicochetHitCooldown, 0);
        Arrays.fill(bird.roadrunnerDustDevilHit, false);
    }

    private static void clearRedlineState(Bird bird) {
        bird.roadrunnerRedlineTimer = 0;
        bird.roadrunnerRedlineRecoveryTimer = 0;
        bird.roadrunnerRedlineCinematic = false;
        bird.roadrunnerRedlineDirection = 1;
        bird.roadrunnerRedlineStrikeIndex = 0;
        bird.roadrunnerRedlineFinalResolved = false;
        bird.roadrunnerRedlineStartX = 0.0;
        bird.roadrunnerRedlineStartY = 0.0;
        bird.roadrunnerRedlineEndX = 0.0;
        bird.roadrunnerRedlineEndY = 0.0;
        bird.roadrunnerRedlineAnchorX = 0.0;
        bird.roadrunnerRedlineAnchorY = 0.0;
        bird.roadrunnerRedlineLastStartX = 0.0;
        bird.roadrunnerRedlineLastStartY = 0.0;
        bird.roadrunnerRedlineLastEndX = 0.0;
        bird.roadrunnerRedlineLastEndY = 0.0;
        Arrays.fill(bird.roadrunnerRedlineCaught, false);
    }

    static void handleMomentum(Bird bird) {
        if (bird.type != BirdGame3.BirdType.ROADRUNNER || bird.health <= 0) {
            bird.roadrunnerMomentum = Math.max(0.0, bird.roadrunnerMomentum - 1.0);
            bird.roadrunnerMomentumGraceTimer = 0;
            return;
        }
        boolean graceActive = bird.roadrunnerMomentumGraceTimer > 0;
        if (graceActive) {
            bird.roadrunnerMomentumGraceTimer--;
        }
        boolean grounded = bird.isOnGround();
        double speed = Math.abs(bird.vx);
        boolean pressingMove = bird.leftPressed() || bird.rightPressed();
        if (bird.stunTime > 0.0 || bird.isBlocking || bird.shieldStunFrames > 0) {
            if (!graceActive) {
                bird.roadrunnerMomentum = Math.max(
                        0.0, bird.roadrunnerMomentum - MOMENTUM_STUN_DECAY_PER_FRAME);
            }
        } else if (grounded && pressingMove && speed > 3.2) {
            addMomentum(bird, (speed - 3.2) * 0.18 + 0.26);
        } else if (grounded && speed > 8.0) {
            addMomentum(bird, (speed - 8.0) * 0.06 + 0.10);
        } else if (sandstormActive(bird)) {
            addMomentum(bird, 0.10);
        } else if (!graceActive) {
            double decay = grounded
                    ? MOMENTUM_GROUND_DECAY_PER_FRAME
                    : MOMENTUM_AIR_DECAY_PER_FRAME;
            bird.roadrunnerMomentum = Math.max(0.0, bird.roadrunnerMomentum - decay);
        }
    }

    static void handleRicochet(Bird bird) {
        int dir = bird.roadrunnerRicochetDirection == 0 ? bird.facingDirection() : bird.roadrunnerRicochetDirection;
        bird.roadrunnerRicochetDirection = dir;
        bird.facingRight = dir > 0;
        bird.vx = dir * Math.max(22.0, bird.roadrunnerRicochetSpeed);
        bird.vy *= 0.78;

        boolean bounced = false;
        double leftBound = bird.usesIslandBounds() ? bird.game.battlefieldLeftBound() + 4.0 : 50.0;
        double rightBound = bird.usesIslandBounds() ? bird.game.battlefieldRightBound() - bird.bodyWidth() - 4.0 : BirdGame3.WORLD_WIDTH - 150.0 * bird.sizeMultiplier;
        if ((bird.x <= leftBound + 5.0 && dir < 0) || (bird.x >= rightBound - 5.0 && dir > 0)) {
            bounced = tryRicochetBounce(bird, -dir, bird.jumpPressed() ? -9.0 : 0.0);
        }

        if (!bounced) {
            for (Platform p : bird.game.platforms) {
                if (Math.abs(bird.bodyCenterY() - (p.y + p.h * 0.5)) > p.h * 0.5 + bird.combatHalfHeight() + 28.0) continue;
                boolean hitLeftEdge = dir > 0 && bird.bodyCenterX() < p.x && bird.bodyCenterX() + bird.combatHalfWidth() + 14.0 >= p.x;
                boolean hitRightEdge = dir < 0 && bird.bodyCenterX() > p.x + p.w && bird.bodyCenterX() - bird.combatHalfWidth() - 14.0 <= p.x + p.w;
                if (hitLeftEdge || hitRightEdge) {
                    bounced = tryRicochetBounce(bird, -dir, bird.jumpPressed() ? -8.5 : 0.0);
                    if (bounced) break;
                }
            }
        }

        for (Bird other : bird.game.players) {
            if (!bird.canDamageTarget(other)) continue;
            if (other.playerIndex < 0 || other.playerIndex >= bird.roadrunnerRicochetHitCooldown.length) continue;
            if (bird.roadrunnerRicochetHitCooldown[other.playerIndex] > 0) continue;
            double dx = other.bodyCenterX() - bird.bodyCenterX();
            double dy = other.bodyCenterY() - bird.bodyCenterY();
            if (Math.abs(dx) > 84.0 * bird.sizeMultiplier + other.combatHalfWidth()
                    || Math.abs(dy) > 82.0 * bird.sizeMultiplier + other.combatHalfHeight()) {
                continue;
            }
            double oldHealth = other.health;
            int dmg = bird.roadrunnerRicochetUltimate ? 8 : 5;
            int dealt = (int) bird.applyDamageTo(other, dmg);
            bird.roadrunnerRicochetHitCooldown[other.playerIndex] = bird.roadrunnerRicochetUltimate ? 10 : 14;
            if (dealt > 0) {
                bird.game.damageDealt[bird.playerIndex] += dealt;
                bird.game.recordSpecialImpact(bird.playerIndex, dealt, true);
                if (other.health <= 0 && oldHealth > 0) bird.game.eliminations[bird.playerIndex]++;
                other.vx += dir * (8.0 + momentumRatio(bird) * 4.0) * CORE_SPECIAL_KNOCKBACK_MULTIPLIER;
                other.vy -= (bird.roadrunnerRicochetUltimate ? 6.2 : 4.2) * CORE_SPECIAL_KNOCKBACK_MULTIPLIER;
                addMomentum(bird, 4.0 + dealt * 0.40);
                emitBurstDust(bird, other.bodyCenterX(), other.bodyCenterY(), dir, 18,
                        warmDustColor(bird, bird.roadrunnerRicochetUltimate));
            }
            if (bird.roadrunnerRicochetBounces > 0) {
                tryRicochetBounce(bird, dx >= 0 ? -1 : 1, -6.8);
            }
        }

        if ((bird.roadrunnerRicochetTimer & 1) == 0) {
            emitBurstDust(bird, bird.bodyCenterX() - dir * 26.0 * bird.sizeMultiplier,
                    bird.bodyBottomY() - 12.0 * bird.sizeMultiplier,
                    dir,
                    bird.roadrunnerRicochetUltimate ? 8 : 5,
                    warmDustColor(bird, bird.roadrunnerRicochetUltimate));
        }
    }

    static boolean tryRicochetBounce(Bird bird, int newDir, double verticalKick) {
        if (bird.roadrunnerRicochetBounces <= 0) {
            bird.roadrunnerRicochetTimer = Math.min(bird.roadrunnerRicochetTimer, 4);
            return false;
        }
        bird.roadrunnerRicochetBounces--;
        bird.roadrunnerRicochetDirection = newDir == 0 ? -bird.roadrunnerRicochetDirection : newDir;
        bird.facingRight = bird.roadrunnerRicochetDirection > 0;
        bird.roadrunnerRicochetSpeed = Math.max(20.0, bird.roadrunnerRicochetSpeed * 0.78);
        bird.vx = bird.roadrunnerRicochetDirection * bird.roadrunnerRicochetSpeed;
        if (verticalKick < 0.0) {
            bird.vy = Math.min(bird.vy, verticalKick);
        }
        bird.roadrunnerRicochetTimer = Math.max(bird.roadrunnerRicochetTimer, 8);
        bird.game.shakeIntensity = Math.max(bird.game.shakeIntensity, 5);
        emitBurstDust(bird, bird.bodyCenterX(), bird.bodyCenterY(), bird.roadrunnerRicochetDirection,
                bird.roadrunnerRicochetUltimate ? 24 : 16,
                trailColor(bird, bird.roadrunnerRicochetUltimate));
        return true;
    }

    static void handleDustDevil(Bird bird) {
        double ratio = Math.clamp(bird.roadrunnerDustDevilTimer / (double) (bird.roadrunnerDustDevilUltimate
                ? Bird.ROADRUNNER_DUST_DEVIL_FRAMES + 8
                : Bird.ROADRUNNER_DUST_DEVIL_FRAMES), 0.0, 1.0);
        if (ratio > 0.35) {
            bird.vy = Math.min(bird.vy, -(8.5 + ratio * (bird.roadrunnerDustDevilUltimate ? 8.5 : 6.0)));
        }
        double centerX = bird.bodyCenterX();
        double centerY = bird.bodyCenterY();
        for (int i = 0; i < bird.scaledParticleCount(bird.roadrunnerDustDevilUltimate ? 7 : 5); i++) {
            double spin = (bird.roadrunnerDustDevilTimer * 0.38 + i * 1.55);
            double radius = (18.0 + i * 9.0 + bird.game.nextParticleRandom() * 14.0) * bird.sizeMultiplier;
            double liftBand = Math.min(96.0, i * 15.0 + bird.game.nextParticleRandom() * 22.0) * bird.sizeMultiplier;
            Color sand = sandColor(bird, bird.roadrunnerDustDevilUltimate)
                    .deriveColor(0, 1, 1, 0.44 + bird.game.nextParticleRandom() * 0.20);
            bird.game.particles.add(new Particle(
                    centerX + Math.cos(spin) * radius * 0.7,
                    bird.bodyBottomY() - 12.0 * bird.sizeMultiplier - liftBand + Math.sin(spin * 1.2) * 4.0,
                    Math.cos(spin + Math.PI * 0.5) * (2.0 + i * 0.45),
                    -3.2 - i * 0.42 + Math.sin(spin) * 0.5,
                    sand
            ));
        }
        for (Bird other : bird.game.players) {
            if (!bird.canDamageTarget(other)) continue;
            if (other.playerIndex < 0 || other.playerIndex >= bird.roadrunnerDustDevilHit.length) continue;
            if (bird.roadrunnerDustDevilHit[other.playerIndex]) continue;
            double dx = other.bodyCenterX() - centerX;
            double dy = other.bodyCenterY() - centerY;
            double radius = (bird.roadrunnerDustDevilUltimate ? 168.0 : 138.0) * bird.sizeMultiplier;
            if (Math.hypot(dx, dy) > radius + other.combatRadius()) continue;
            double safe = Math.max(1.0, Math.hypot(dx, dy));
            double lift = bird.roadrunnerDustDevilUltimate ? 21.0 : 17.0;
            other.vx += dx / safe * (bird.roadrunnerDustDevilUltimate ? 7.0 : 5.1);
            other.vy = Math.min(other.vy, -lift);
            bird.roadrunnerDustDevilHit[other.playerIndex] = true;
            emitBurstDust(bird, other.bodyCenterX(), other.bodyBottomY() - 8.0 * other.sizeMultiplier,
                    dx >= 0.0 ? 1 : -1, bird.roadrunnerDustDevilUltimate ? 18 : 12,
                    sandColor(bird, bird.roadrunnerDustDevilUltimate));
        }
        if ((bird.roadrunnerDustDevilTimer & 1) == 0) {
            emitBurstDust(bird, centerX, bird.bodyBottomY() - 6.0 * bird.sizeMultiplier,
                    bird.facingDirection(), bird.roadrunnerDustDevilUltimate ? 8 : 5,
                    sandColor(bird, bird.roadrunnerDustDevilUltimate));
        }
    }

    static void handlePaintedRoads(Bird bird) {
        if (bird.type != BirdGame3.BirdType.ROADRUNNER || bird.roadrunnerPaintedRoads.isEmpty()) {
            return;
        }
        for (Iterator<Bird.RoadrunnerPaintedRoad> it = bird.roadrunnerPaintedRoads.iterator(); it.hasNext(); ) {
            Bird.RoadrunnerPaintedRoad road = it.next();
            road.ageFrames++;
            if (road.ownerBoostCooldown > 0) road.ownerBoostCooldown--;
            for (int i = 0; i < road.hitCooldown.length; i++) {
                if (road.hitCooldown[i] > 0) road.hitCooldown[i]--;
            }
            if (!road.collapsed && road.lifeFrames > 0) {
                road.lifeFrames--;
            }
            if (road.collapsed && road.collapseTimer > 0) {
                road.collapseTimer--;
            } else if (road.collapsed && road.fadeTimer > 0) {
                road.fadeTimer--;
            }
            if (!road.collapsed && road.lifeFrames <= 0) {
                collapsePaintedRoad(road);
            }
            if (road.usesRemaining <= 0 && !road.collapsed) {
                collapsePaintedRoad(road);
            }
            if (bird.health <= 0) {
                it.remove();
                continue;
            }
            if (road.collapsed && road.fadeTimer <= 0 && road.usesRemaining <= 0) {
                it.remove();
                continue;
            }
            if (road.collapsed) {
                continue;
            }

            double halfWidth = road.ultimate ? 104.0 : 86.0;
            boolean ownerStandingOnRoad = isStandingOnRoad(road, bird, halfWidth + 8.0);
            if (!ownerStandingOnRoad) {
                road.ownerClearedRoad = true;
            }
            if (road.ownerClearedRoad && ownerStandingOnRoad && road.ownerBoostCooldown <= 0) {
                road.ownerBoostCooldown = road.ultimate ? 24 : 34;
                road.usesRemaining--;
                bird.roadrunnerRoadBoostTimer = Math.max(bird.roadrunnerRoadBoostTimer, road.ultimate ? 42 : 30);
                addMomentum(bird, road.ultimate ? 40.0 : 30.0);
                double roadBoostSpeed = (road.ultimate ? 31.0 : 26.0) + momentumRatio(bird) * 6.0;
                bird.vx = road.direction * Math.max(Math.abs(bird.vx), roadBoostSpeed);
                bird.roadrunnerMomentumFxTimer = Math.max(bird.roadrunnerMomentumFxTimer, 36);
                emitBurstDust(bird, bird.bodyCenterX(), road.y - 8.0, road.direction,
                        road.ultimate ? 20 : 14,
                        trailColor(bird, road.ultimate));
                if (road.usesRemaining <= 0) {
                    collapsePaintedRoad(road);
                    continue;
                }
            }

            for (Bird other : bird.game.players) {
                if (!bird.canDamageTarget(other)) continue;
                if (other.playerIndex < 0 || other.playerIndex >= road.hitCooldown.length) continue;
                if (road.hitCooldown[other.playerIndex] > 0) continue;
                if (!isStandingOnRoad(road, other, halfWidth + other.combatHalfWidth())) continue;
                road.hitCooldown[other.playerIndex] = road.ultimate ? 26 : 34;
                road.usesRemaining--;
                int bounceDir = -road.direction;
                applySlip(other, bird, bounceDir, road.ultimate);
                other.vx = bounceDir * (road.ultimate ? 25.0 : 20.0);
                other.vy = Math.min(other.vy, road.ultimate ? -4.2 : -3.0);
                emitBurstDust(bird, other.bodyCenterX(), road.y - 6.0, road.direction,
                        road.ultimate ? 16 : 10,
                        trailColor(bird, road.ultimate));
                if (road.usesRemaining <= 0) {
                    collapsePaintedRoad(road);
                    break;
                }
            }
        }
    }

    static void collapsePaintedRoad(Bird.RoadrunnerPaintedRoad road) {
        if (road == null || road.collapsed) {
            return;
        }
        road.collapsed = true;
        road.collapseTimer = Bird.ROADRUNNER_PAINTED_ROAD_COLLAPSE_FRAMES;
        road.fadeTimer = Bird.ROADRUNNER_PAINTED_ROAD_FADE_FRAMES;
        road.ownerBoostCooldown = 0;
    }

    static boolean isStandingOnRoad(Bird.RoadrunnerPaintedRoad road, Bird bird, double horizontalReach) {
        double dx = bird.bodyCenterX() - road.x;
        if (Math.abs(dx) > horizontalReach) return false;
        double feetDistance = Math.abs(bird.bodyBottomY() - road.y);
        return feetDistance <= 34.0 + bird.combatHalfHeight() * 0.22
                || (bird.bodyCenterY() > road.y - 58.0 && bird.bodyCenterY() < road.y + 20.0);
    }

    static void applySlip(Bird bird, Bird owner, int direction, boolean ultimate) {
        if (owner == null || owner.playerIndex < 0 || owner.playerIndex >= bird.game.players.length) {
            return;
        }
        bird.roadrunnerSlipOwnerIndex = owner.playerIndex;
        bird.roadrunnerSlipDirection = direction == 0 ? 1 : direction;
        bird.roadrunnerSlipUltimate = bird.roadrunnerSlipUltimate || ultimate;
        bird.roadrunnerSlipTimer = Math.max(bird.roadrunnerSlipTimer,
                ultimate ? Bird.ROADRUNNER_SLIP_FRAMES + 22 : Bird.ROADRUNNER_SLIP_FRAMES);
    }

    static void handleSlipEffect(Bird bird) {
        if (bird.roadrunnerSlipTimer <= 0 || bird.health <= 0) {
            return;
        }
        double desired = bird.roadrunnerSlipDirection * (bird.roadrunnerSlipUltimate ? 48.0 : 40.0);
        if (Math.signum(bird.vx) != Math.signum(desired) && Math.abs(bird.vx) > 1.2) {
            bird.vx *= bird.roadrunnerSlipUltimate ? 0.28 : 0.22;
        }
        bird.vx += (desired - bird.vx) * (bird.roadrunnerSlipUltimate ? 0.50 : 0.42);
        if (bird.isOnGround()) {
            bird.vy = Math.min(bird.vy, -0.45);
        }
        if ((bird.roadrunnerSlipTimer & 3) == 0) {
            bird.game.particles.add(new Particle(
                    bird.bodyCenterX() - bird.roadrunnerSlipDirection * 22.0 * bird.sizeMultiplier,
                    bird.bodyBottomY() - 7.0 * bird.sizeMultiplier,
                    -bird.roadrunnerSlipDirection * (1.4 + bird.game.nextParticleRandom() * 2.0),
                    -0.8 - bird.game.nextParticleRandom() * 1.8,
                    trailColor(bird, bird.roadrunnerSlipUltimate).deriveColor(0, 1, 1, 0.62)
            ));
        }
    }

    static boolean active(Bird bird) {
        return bird.roadrunnerBeepCharging
                || bird.roadrunnerBeepBurstTimer > 0
                || bird.roadrunnerRicochetTimer > 0
                || bird.roadrunnerDustDevilTimer > 0
                || bird.roadrunnerRedlineTimer > 0
                || bird.roadrunnerRedlineRecoveryTimer > 0;
    }

    static boolean ready(Bird bird, Bird.RoadrunnerSpecialVariant variant) {
        return switch (variant) {
            case NEUTRAL -> bird.isUltimateReady() || bird.roadrunnerBeepReuseTimer <= 0;
            case SIDE -> bird.roadrunnerRicochetReuseTimer <= 0;
            case UP -> !bird.roadrunnerDustDevilUsed;
            case DOWN -> bird.roadrunnerPaintedRoadReuseTimer <= 0;
        };
    }

    static boolean canConvertShieldIntoDown(Bird bird) {
        return bird.selectRoadrunnerSpecialVariant() == Bird.RoadrunnerSpecialVariant.DOWN
                && bird.isBlocking
                && bird.shieldStunFrames <= 0;
    }

    static boolean canStart(Bird bird, boolean grabbed, boolean dodging) {
        Bird.RoadrunnerSpecialVariant variant = bird.selectRoadrunnerSpecialVariant();
        boolean shieldConversion = canConvertShieldIntoDown(bird);
        return bird.type == BirdGame3.BirdType.ROADRUNNER
                && bird.health > 0
                && bird.stunTime <= 0.0
                && !grabbed
                && (!bird.isBlocking || shieldConversion)
                && !dodging
                && !active(bird)
                && ready(bird, variant);
    }

    static boolean sandstormActive(Bird bird) {
        return bird.type == BirdGame3.BirdType.ROADRUNNER && bird.roadrunnerSandstormTimer > 0;
    }

    static void reset(Bird bird) {
        bird.roadrunnerMomentum = 0.0;
        bird.roadrunnerMomentumFxTimer = 0;
        bird.roadrunnerMomentumGraceTimer = 0;
        bird.roadrunnerBeepCharging = false;
        bird.roadrunnerBeepChargeFrames = 0;
        bird.roadrunnerBeepMaxChargeHoldFrames = 0;
        bird.roadrunnerBeepBurstTimer = 0;
        bird.roadrunnerBeepReuseTimer = 0;
        bird.roadrunnerBeepDirection = 1;
        bird.roadrunnerBeepUltimate = false;
        Arrays.fill(bird.roadrunnerBeepHit, false);
        bird.roadrunnerRicochetTimer = 0;
        bird.roadrunnerRicochetReuseTimer = 0;
        bird.roadrunnerRicochetDirection = 1;
        bird.roadrunnerRicochetBounces = 0;
        bird.roadrunnerRicochetSpeed = 0.0;
        bird.roadrunnerRicochetUltimate = false;
        Arrays.fill(bird.roadrunnerRicochetHitCooldown, 0);
        bird.roadrunnerDustDevilTimer = 0;
        bird.roadrunnerDustDevilUsed = false;
        bird.roadrunnerDustDevilUltimate = false;
        Arrays.fill(bird.roadrunnerDustDevilHit, false);
        bird.roadrunnerPaintedRoadReuseTimer = 0;
        bird.roadrunnerRoadBoostTimer = 0;
        bird.roadrunnerPaintedRoads.clear();

        bird.roadrunnerSlipTimer = 0;
        bird.roadrunnerSlipDirection = 1;
        bird.roadrunnerSlipOwnerIndex = -1;
        bird.roadrunnerSlipUltimate = false;
        clearRedlineState(bird);
    }

    static void interruptOnHit(Bird bird) {
        if (bird.type != BirdGame3.BirdType.ROADRUNNER) {
            return;
        }
        if (bird.roadrunnerRedlineCinematic && bird.roadrunnerRedlineTimer > 0) {
            return;
        }
        if (active(bird)) {
            bird.attackAnimationTimer = 0;
        }
        clearNormalActionState(bird);
        clearRedlineState(bird);
    }
}
