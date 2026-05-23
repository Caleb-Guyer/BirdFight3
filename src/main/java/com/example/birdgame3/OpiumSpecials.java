package com.example.birdgame3;

import com.example.birdgame3.BirdGame3.MapType;
import javafx.scene.paint.Color;

import java.util.Arrays;
import java.util.Iterator;

final class OpiumSpecials {
    private OpiumSpecials() {
    }

    static void useOpium(Bird bird, boolean ultimate) {
        if (ultimate) {
            ultimate(bird);
            return;
        }
        switch (bird.selectOpiumSpecialVariant()) {
            case NEUTRAL -> neutral(bird, false);
            case SIDE -> side(bird, false);
            case UP -> up(bird, false);
            case DOWN -> down(bird, false);
        }
    }

    static void useHeisenbird(Bird bird, boolean ultimate) {
        if (ultimate) {
            heisenUltimate(bird);
            return;
        }
        switch (bird.selectOpiumSpecialVariant()) {
            case NEUTRAL -> heisenNeutral(bird, false);
            case SIDE -> side(bird, true);
            case UP -> up(bird, true);
            case DOWN -> down(bird, true);
        }
    }

    static void neutral(Bird bird, boolean ultimate) {
        boolean fueled = ultimate || bird.spendOpiumResource(Bird.OPIUM_NEUTRAL_RESOURCE_COST);
        bird.opiumNeutralFueled = fueled;
        bird.leanTimer = Math.max(bird.leanTimer, ultimate
                ? Bird.OPIUM_NEUTRAL_FRAMES + 110
                : (fueled ? Bird.OPIUM_NEUTRAL_FRAMES : 34));
        bird.opiumNeutralReuseTimer = ultimate ? 28 : Bird.OPIUM_NEUTRAL_REUSE_FRAMES;
        bird.leanCooldown = 0;
        bird.specialCooldown = 0;
        bird.specialMaxCooldown = 0;
        bird.attackAnimationTimer = Math.max(bird.attackAnimationTimer, ultimate ? 18 : (fueled ? 14 : 9));
        bird.vx *= bird.isOnGround() ? (fueled ? 0.52 : 0.72) : (fueled ? 0.72 : 0.84);
        if (!bird.isOnGround()) {
            bird.vy = Math.min(bird.vy, 1.0);
        }
        emitBurst(bird, bird.bodyCenterX(), bird.bodyCenterY(), ultimate ? 54 : (fueled ? 38 : 10),
                ultimate ? Color.GOLD : (fueled ? Color.web("#AB47BC") : Color.web("#6A1B9A").deriveColor(0, 0.65, 0.72, 0.45)));
    }

    static void heisenNeutral(Bird bird, boolean ultimate) {
        boolean fueled = ultimate || bird.spendOpiumResource(Bird.HEISEN_NEUTRAL_RESOURCE_COST);
        bird.opiumNeutralFueled = fueled;
        bird.leanTimer = Math.max(bird.leanTimer, ultimate
                ? Bird.HEISEN_NEUTRAL_FRAMES + 90
                : (fueled ? Bird.HEISEN_NEUTRAL_FRAMES : 30));
        bird.opiumNeutralReuseTimer = ultimate ? 24 : Bird.HEISEN_NEUTRAL_REUSE_FRAMES;
        bird.leanCooldown = 0;
        bird.specialCooldown = 0;
        bird.specialMaxCooldown = 0;
        bird.attackAnimationTimer = Math.max(bird.attackAnimationTimer, ultimate ? 16 : (fueled ? 12 : 8));
        bird.vx *= bird.isOnGround() ? (fueled ? 0.56 : 0.74) : (fueled ? 0.76 : 0.86);
        if (!bird.isOnGround()) {
            bird.vy = Math.min(bird.vy, 1.1);
        }
        emitBurst(bird, bird.bodyCenterX(), bird.bodyCenterY(), ultimate ? 52 : (fueled ? 28 : 8),
                ultimate ? Color.GOLD : (fueled ? Color.web("#29B6F6") : Color.web("#455A64").deriveColor(0, 0.55, 0.78, 0.42)));
    }

    static void side(Bird bird, boolean heisen) {
        int dir = bird.horizontalInputDirection();
        if (dir == 0) {
            dir = bird.facingDirection();
        }
        bird.facingRight = dir > 0;
        bird.opiumSideDirection = dir;
        bird.opiumSideTimer = heisen ? Bird.HEISEN_SIDE_FRAMES : Bird.OPIUM_SIDE_FRAMES;
        bird.opiumSideReuseTimer = heisen ? Bird.HEISEN_SIDE_REUSE_FRAMES : Bird.OPIUM_SIDE_REUSE_FRAMES;
        bird.opiumSideFueled = bird.spendOpiumResource(heisen ? Bird.HEISEN_SIDE_RESOURCE_COST : Bird.OPIUM_SIDE_RESOURCE_COST);
        Arrays.fill(bird.opiumSideHit, false);
        bird.specialCooldown = 0;
        bird.specialMaxCooldown = 0;
        bird.attackAnimationTimer = Math.max(bird.attackAnimationTimer, bird.opiumSideTimer + 4);
        bird.vx = dir * (heisen ? (bird.opiumSideFueled ? 24.0 : 18.0) : (bird.opiumSideFueled ? 21.8 : 16.8));
        if (!bird.isOnGround()) {
            bird.vy = Math.min(bird.vy, heisen ? 0.6 : 0.9);
        }
        emitBurst(bird, bird.bodyCenterX(), bird.bodyCenterY(), bird.opiumSideFueled ? (heisen ? 24 : 32) : 7,
                bird.opiumSideFueled
                        ? (heisen ? Color.web("#81D4FA") : Color.web("#CE93D8"))
                        : Color.web("#78909C").deriveColor(0, 0.5, 0.85, 0.42));
    }

    static void up(Bird bird, boolean heisen) {
        if (bird.opiumUpSpecialUsed) {
            return;
        }
        bird.opiumUpSpecialUsed = true;
        bird.opiumUpTimer = heisen ? Bird.HEISEN_UP_FRAMES : Bird.OPIUM_UP_FRAMES;
        bird.opiumUpFueled = bird.spendOpiumResource(heisen ? Bird.HEISEN_UP_RESOURCE_COST : Bird.OPIUM_UP_RESOURCE_COST);
        Arrays.fill(bird.opiumUpHit, false);
        bird.specialCooldown = 0;
        bird.specialMaxCooldown = 0;
        bird.attackAnimationTimer = Math.max(bird.attackAnimationTimer, bird.opiumUpTimer + 4);
        bird.canDoubleJump = true;
        bird.vx *= heisen ? 0.24 : 0.34;
        bird.vy = Math.min(bird.vy, heisen
                ? (bird.opiumUpFueled ? -18.2 : -15.2)
                : (bird.opiumUpFueled ? -17.4 : -14.8));
        emitBurst(bird, bird.bodyCenterX(), bird.bodyBottomY() - 8.0 * bird.sizeMultiplier, bird.opiumUpFueled ? (heisen ? 30 : 38) : 8,
                bird.opiumUpFueled
                        ? (heisen ? Color.web("#81D4FA") : Color.web("#CE93D8"))
                        : Color.web("#78909C").deriveColor(0, 0.5, 0.85, 0.42));
    }

    static void down(Bird bird, boolean heisen) {
        int dir = bird.facingDirection();
        double trapX = bird.bodyCenterX() - dir * 48.0 * bird.sizeMultiplier;
        double trapY = trapSurfaceY(bird, trapX);
        bird.opiumTraps.add(new Bird.OpiumTrap(trapX, trapY, heisen, false));
        while (bird.opiumTraps.size() > (heisen ? 3 : 4)) {
            bird.opiumTraps.removeFirst();
        }
        bird.opiumDownReuseTimer = heisen ? Bird.HEISEN_DOWN_REUSE_FRAMES : Bird.OPIUM_DOWN_REUSE_FRAMES;
        bird.specialCooldown = 0;
        bird.specialMaxCooldown = 0;
        bird.attackAnimationTimer = Math.max(bird.attackAnimationTimer, 12);
        bird.vx *= 0.58;
        emitBurst(bird, trapX, trapY - 10.0, heisen ? 18 : 20,
                heisen ? Color.web("#81D4FA") : Color.web("#CE93D8"));
    }

    static void ultimate(Bird bird) {
        bird.refillOpiumResource(Bird.OPIUM_RESOURCE_MAX);
        bird.opiumNeutralFueled = true;
        bird.leanTimer = Math.max(bird.leanTimer, Bird.OPIUM_NEUTRAL_FRAMES);
        bird.opiumUltimateTimer = Bird.OPIUM_ULTIMATE_FRAMES;
        bird.opiumUltimateCollapsePending = true;
        bird.opiumUltimateCloudX = bird.bodyCenterX();
        bird.opiumUltimateCloudY = bird.bodyCenterY() + 12.0 * bird.sizeMultiplier;
        bird.specialCooldown = 0;
        bird.specialMaxCooldown = 0;
        bird.attackAnimationTimer = Math.max(bird.attackAnimationTimer, 24);
        bird.game.addToKillFeed(bird.shortName() + " DROPPED A LEAN CLOUD!");
        emitBurst(bird, bird.opiumUltimateCloudX, bird.opiumUltimateCloudY, 118, Color.GOLD);
    }

    static void heisenUltimate(Bird bird) {
        bird.refillOpiumResource(Bird.OPIUM_RESOURCE_MAX);
        bird.heisenUltimateTimer = Bird.HEISEN_ULTIMATE_FRAMES;
        bird.heisenUltimateShatterPending = true;
        bird.heisenUltimateVolleyTimer = 0;
        bird.heisenUltimateVolleyHit = false;
        resetHeisenUltimateShardState(bird, true);
        bird.specialCooldown = 0;
        bird.specialMaxCooldown = 0;
        bird.attackAnimationTimer = Math.max(bird.attackAnimationTimer, 22);
        for (Bird other : bird.game.players) {
            if (!bird.canDamageTarget(other)) continue;
            if (bird.combatDistanceTo(other) > 520.0 + other.combatRadius()) continue;
            other.applyHeisenBrittle(bird, true);
        }
        bird.game.addToKillFeed(bird.shortName() + " COOKED A CRYSTAL STORM!");
        emitBurst(bird, bird.bodyCenterX(), bird.bodyCenterY(), 92, Color.web("#B3E5FC"));
    }

    static double trapSurfaceY(Bird bird, double trapX) {
        double bestY = bird.hasSolidGroundFloorUnderBody() ? BirdGame3.GROUND_Y : Double.POSITIVE_INFINITY;
        double sourceY = bird.bodyBottomY() - 18.0 * bird.sizeMultiplier;
        for (Platform p : bird.game.platforms) {
            boolean isCaveCeiling = bird.game.selectedMap == MapType.CAVE
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

    static void emitBurst(Bird bird, double originX, double originY, int count, Color color) {
        for (int i = 0; i < bird.scaledParticleCount(count); i++) {
            double angle = Math.random() * Math.PI * 2.0;
            double speed = 1.2 + Math.random() * 5.8;
            bird.game.particles.add(new Particle(
                    originX + Math.cos(angle) * (8.0 + Math.random() * 18.0),
                    originY + Math.sin(angle) * (8.0 + Math.random() * 18.0),
                    Math.cos(angle) * speed,
                    Math.sin(angle) * speed - 1.2,
                    color.deriveColor(0, 1, 1, 0.72)
            ));
        }
    }

    static void resetHeisenUltimateShardState(Bird bird, boolean clearOrbitCooldowns) {
        Arrays.fill(bird.heisenUltimateShardLaunched, false);
        Arrays.fill(bird.heisenUltimateShardSpent, false);
        Arrays.fill(bird.heisenUltimateShardX, 0.0);
        Arrays.fill(bird.heisenUltimateShardY, 0.0);
        Arrays.fill(bird.heisenUltimateShardVX, 0.0);
        Arrays.fill(bird.heisenUltimateShardVY, 0.0);
        if (clearOrbitCooldowns) {
            Arrays.fill(bird.heisenUltimateOrbitHitCooldown, 0);
        }
    }

    static void handleBirdEffects(Bird bird, double gameSpeed) {
        boolean opium = bird.type == BirdGame3.BirdType.OPIUMBIRD
                || bird.mockingbirdCopiedNeutralFrom(BirdGame3.BirdType.OPIUMBIRD);
        boolean heisen = bird.type == BirdGame3.BirdType.HEISENBIRD
                || bird.mockingbirdCopiedNeutralFrom(BirdGame3.BirdType.HEISENBIRD);
        if (!opium && !heisen) return;

        if (bird.leanTimer > 0 && opium) {
            bird.game.leanTime[bird.playerIndex]++;
            bird.game.recordLeanFrame(bird);
        } else if (bird.leanTimer > 0) {
            bird.game.leanTime[bird.playerIndex]++;
        }

        if (bird.leanTimer > 0 && bird.opiumNeutralFueled) {
            double outerRadius = heisen ? 250.0 : 330.0;
            double innerRadius = heisen ? 190.0 : 270.0;
            int damageRoll = heisen ? 96 : 24;
            double slowX = heisen ? 0.978 : 0.93;
            double slowY = heisen ? 0.992 : 0.974;
            boolean firstFrame = heisen && bird.leanTimer >= Bird.HEISEN_NEUTRAL_FRAMES - 2;
            for (Bird other : bird.game.players) {
                if (!bird.canDamageTarget(other)) continue;
                double dx = other.bodyCenterX() - bird.bodyCenterX();
                double dy = other.bodyCenterY() - bird.bodyCenterY();
                double dist = Math.hypot(dx, dy);
                if (dist > outerRadius + other.combatRadius()) continue;
                if (dist > innerRadius + other.combatRadius()) continue;
                if (bird.random.nextInt(damageRoll) == 0) {
                    bird.applyTrackedSpecialDamage(other, 1);
                }
                other.vx *= slowX;
                other.vy *= slowY;
                if (heisen && (firstFrame || (bird.leanTimer % 24) == 0)) {
                    other.applyHeisenBrittle(bird, false);
                }
            }
        }

        if (bird.type == BirdGame3.BirdType.OPIUMBIRD && bird.opiumUltimateTimer > 0) {
            applyUltimateHaze(bird);
        }
        if (bird.type == BirdGame3.BirdType.OPIUMBIRD && bird.opiumUltimateCollapsePending
                && bird.opiumUltimateTimer <= 0) {
            collapseUltimateHaze(bird);
            bird.opiumUltimateCollapsePending = false;
        }
        if (bird.type == BirdGame3.BirdType.HEISENBIRD && bird.heisenUltimateTimer > 0) {
            applyHeisenUltimateOrbitShardHits(bird, bird.heisenUltimateTimer, -1);
        }
        if (bird.type == BirdGame3.BirdType.HEISENBIRD && bird.heisenUltimateShatterPending
                && bird.heisenUltimateTimer <= 0) {
            launchHeisenUltimateCrystals(bird);
            bird.heisenUltimateShatterPending = false;
        }
        if (bird.type == BirdGame3.BirdType.HEISENBIRD && bird.heisenUltimateVolleyTimer > 0) {
            handleHeisenUltimateCrystalVolley(bird, gameSpeed);
        }
    }

    static void handleStatusEffects(Bird bird) {
        if (bird.opiumDrowsyTimer > 0) {
            bird.vx *= bird.opiumDrowsyUltimate ? 0.972 : 0.982;
            if (bird.vy > 0.0) {
                bird.vy *= bird.opiumDrowsyUltimate ? 0.985 : 0.992;
            }
            if ((bird.opiumDrowsyTimer & 7) == 0) {
                bird.game.particles.add(new Particle(
                        bird.bodyCenterX() + (Math.random() - 0.5) * bird.bodyWidth() * 0.86,
                        bird.bodyCenterY() - 18.0 * bird.sizeMultiplier
                                + (Math.random() - 0.5) * bird.bodyHeight() * 0.32,
                        (Math.random() - 0.5) * 1.2,
                        -0.7 - Math.random() * 1.2,
                        (bird.opiumDrowsyUltimate ? Color.GOLD : Color.web("#CE93D8")).deriveColor(0, 1, 1, 0.58)
                ));
            }
        }
        if (bird.heisenBrittleTimer > 0 && (bird.heisenBrittleTimer & 5) == 0) {
            bird.game.particles.add(new Particle(
                    bird.bodyCenterX() + (Math.random() - 0.5) * bird.bodyWidth() * 0.78,
                    bird.bodyCenterY() + (Math.random() - 0.5) * bird.bodyHeight() * 0.58,
                    (Math.random() - 0.5) * 1.8,
                    -0.9 - Math.random() * 1.5,
                    (bird.heisenBrittleUltimate ? Color.GOLD : Color.web("#81D4FA")).deriveColor(0, 1, 1, 0.72)
            ));
        }
    }

    static void applyUltimateHaze(Bird bird) {
        double radius = 500.0;
        double cloudX = bird.opiumUltimateCloudX == 0.0 ? bird.bodyCenterX() : bird.opiumUltimateCloudX;
        double cloudY = bird.opiumUltimateCloudY == 0.0 ? bird.bodyCenterY() : bird.opiumUltimateCloudY;
        for (Bird other : bird.game.players) {
            if (!bird.canDamageTarget(other)) continue;
            double dx = other.bodyCenterX() - cloudX;
            double dy = other.bodyCenterY() - cloudY;
            if (Math.hypot(dx, dy) > radius + other.combatRadius()) continue;
            other.vx *= 0.925;
            other.vy *= 0.978;
            other.applyOpiumDrowsy(bird, true);
            if ((bird.opiumUltimateTimer % 16) == 0) {
                bird.applyTrackedSpecialDamage(other, 1);
            }
        }
    }

    static void collapseUltimateHaze(Bird bird) {
        double radius = 500.0;
        double cloudX = bird.opiumUltimateCloudX == 0.0 ? bird.bodyCenterX() : bird.opiumUltimateCloudX;
        double cloudY = bird.opiumUltimateCloudY == 0.0 ? bird.bodyCenterY() : bird.opiumUltimateCloudY;
        for (Bird other : bird.game.players) {
            if (!bird.canDamageTarget(other)) continue;
            double dx = other.bodyCenterX() - cloudX;
            double dy = other.bodyCenterY() - cloudY;
            double dist = Math.hypot(dx, dy);
            if (dist > radius + other.combatRadius()) continue;
            int dealt = bird.applyTrackedSpecialDamage(other, 14);
            if (dealt <= 0) continue;
            double safe = Math.max(1.0, dist);
            other.vx += dx / safe * 15.0;
            other.vy -= 10.6;
            other.applyStun(26);
        }
        emitBurst(bird, cloudX, cloudY, 132, Color.GOLD);
        bird.game.shakeIntensity = Math.max(bird.game.shakeIntensity, 16);
    }

    static void launchHeisenUltimateCrystals(Bird bird) {
        bird.heisenUltimateVolleyOriginX = bird.bodyCenterX();
        bird.heisenUltimateVolleyOriginY = bird.bodyCenterY() - 10.0 * bird.sizeMultiplier;
        Bird target = nearestOpiumDamageTarget(bird, 760.0);
        if (target != null) {
            bird.heisenUltimateVolleyTargetX = target.bodyCenterX();
            bird.heisenUltimateVolleyTargetY = target.bodyCenterY();
        } else {
            bird.heisenUltimateVolleyTargetX = bird.heisenUltimateVolleyOriginX + bird.facingDirection() * 520.0;
            bird.heisenUltimateVolleyTargetY = bird.heisenUltimateVolleyOriginY - 26.0 * bird.sizeMultiplier;
        }
        bird.heisenUltimateVolleyTimer = Bird.HEISEN_ULTIMATE_VOLLEY_FRAMES;
        bird.heisenUltimateVolleyHit = false;
        resetHeisenUltimateShardState(bird, false);
        emitBurst(bird, bird.heisenUltimateVolleyOriginX, bird.heisenUltimateVolleyOriginY, 72, Color.web("#B3E5FC"));
        bird.game.shakeIntensity = Math.max(bird.game.shakeIntensity, 10);
    }

    static void applyHeisenUltimateOrbitShardHits(Bird bird, double clock, int volleyElapsed) {
        double anchorX = bird.bodyCenterX();
        double anchorY = bird.bodyCenterY() - 10.0 * bird.sizeMultiplier;
        for (Bird other : bird.game.players) {
            if (!bird.canDamageTarget(other)) continue;
            int targetIndex = other.playerIndex;
            if (targetIndex < 0 || targetIndex >= bird.heisenUltimateOrbitHitCooldown.length) continue;
            if (bird.heisenUltimateOrbitHitCooldown[targetIndex] > 0) continue;

            for (int i = 0; i < Bird.HEISEN_ULTIMATE_SHARD_COUNT; i++) {
                if (volleyElapsed >= 0 && volleyElapsed - i * Bird.HEISEN_ULTIMATE_SHARD_LAUNCH_SPACING_FRAMES >= 0) {
                    continue;
                }
                double shardX = heisenUltimateOrbitX(bird, i, anchorX, clock);
                double shardY = heisenUltimateOrbitY(bird, i, anchorY, clock);
                double radius = (30.0 + (i % 2) * 5.0) * bird.sizeMultiplier;
                double dx = other.bodyCenterX() - shardX;
                double dy = other.bodyCenterY() - shardY;
                if (Math.hypot(dx, dy) > radius + other.combatRadius()) continue;

                int dealt = bird.applyTrackedSpecialDamage(other, 1);
                if (dealt <= 0) continue;
                other.applyHeisenBrittle(bird, true);
                double safe = Math.max(1.0, Math.hypot(dx, dy));
                other.vx += dx / safe * 2.8;
                other.vy += dy / safe * 1.6 - 0.8;
                bird.heisenUltimateOrbitHitCooldown[targetIndex] = Bird.HEISEN_ULTIMATE_ORBIT_HIT_COOLDOWN_FRAMES;
                if ((bird.heisenUltimateTimer & 1) == 0) {
                    bird.game.particles.add(new Particle(
                            shardX,
                            shardY,
                            (Math.random() - 0.5) * 2.2,
                            -0.6 - Math.random() * 1.2,
                            Color.web("#B3E5FC", 0.76)
                    ));
                }
                break;
            }
        }
    }

    static Bird nearestOpiumDamageTarget(Bird bird, double maxDistance) {
        return nearestOpiumDamageTargetFrom(bird, bird.bodyCenterX(), bird.bodyCenterY(), maxDistance);
    }

    static Bird nearestOpiumDamageTargetFrom(Bird bird, double sourceX, double sourceY, double maxDistance) {
        Bird best = null;
        double bestDist = maxDistance;
        for (Bird other : bird.game.players) {
            if (!bird.canDamageTarget(other)) continue;
            double dist = Math.hypot(other.bodyCenterX() - sourceX, other.bodyCenterY() - sourceY);
            if (dist < bestDist) {
                bestDist = dist;
                best = other;
            }
        }
        return best;
    }

    static double heisenUltimateOrbitAngle(int shardIndex, double clock) {
        return shardIndex / (double) Bird.HEISEN_ULTIMATE_SHARD_COUNT * Math.PI * 2.0 + clock * 0.040;
    }

    static double heisenUltimateOrbitX(Bird bird, int shardIndex, double anchorX, double clock) {
        double pulse = 0.5 + 0.5 * Math.sin(clock * 0.22);
        double angle = heisenUltimateOrbitAngle(shardIndex, clock);
        double radius = (104.0 + (shardIndex % 2) * 20.0 + pulse * 12.0) * bird.sizeMultiplier;
        return anchorX + Math.cos(angle) * radius;
    }

    static double heisenUltimateOrbitY(Bird bird, int shardIndex, double anchorY, double clock) {
        double pulse = 0.5 + 0.5 * Math.sin(clock * 0.22);
        double angle = heisenUltimateOrbitAngle(shardIndex, clock);
        double radius = (104.0 + (shardIndex % 2) * 20.0 + pulse * 12.0) * bird.sizeMultiplier;
        return anchorY + Math.sin(angle) * radius * 0.72;
    }

    static void handleHeisenUltimateCrystalVolley(Bird bird, double gameSpeed) {
        int elapsed = Bird.HEISEN_ULTIMATE_VOLLEY_FRAMES - bird.heisenUltimateVolleyTimer;
        applyHeisenUltimateOrbitShardHits(bird, -elapsed, elapsed);
        for (int i = 0; i < Bird.HEISEN_ULTIMATE_SHARD_COUNT; i++) {
            int shardElapsed = elapsed - i * Bird.HEISEN_ULTIMATE_SHARD_LAUNCH_SPACING_FRAMES;
            if (shardElapsed < 0 || bird.heisenUltimateShardSpent[i]) continue;
            if (shardElapsed >= Bird.HEISEN_ULTIMATE_SHARD_FLIGHT_FRAMES) {
                bird.heisenUltimateShardSpent[i] = true;
                continue;
            }
            if (!bird.heisenUltimateShardLaunched[i]) {
                launchHeisenUltimateShard(bird, i, elapsed);
            }
            updateHeisenUltimateFlyingShard(bird, i, shardElapsed, gameSpeed);
        }
    }

    static void launchHeisenUltimateShard(Bird bird, int shardIndex, int volleyElapsed) {
        double anchorX = bird.bodyCenterX();
        double anchorY = bird.bodyCenterY() - 10.0 * bird.sizeMultiplier;
        double clock = -volleyElapsed;
        double angle = heisenUltimateOrbitAngle(shardIndex, clock);
        bird.heisenUltimateShardX[shardIndex] = heisenUltimateOrbitX(bird, shardIndex, anchorX, clock);
        bird.heisenUltimateShardY[shardIndex] = heisenUltimateOrbitY(bird, shardIndex, anchorY, clock);
        Bird target = nearestOpiumDamageTargetFrom(bird, bird.heisenUltimateShardX[shardIndex],
                bird.heisenUltimateShardY[shardIndex], 940.0);
        double nx = Math.cos(angle);
        double ny = Math.sin(angle) * 0.72;
        if (target != null) {
            double dx = target.bodyCenterX() - bird.heisenUltimateShardX[shardIndex];
            double dy = target.bodyCenterY() - bird.heisenUltimateShardY[shardIndex];
            double safe = Math.max(1.0, Math.hypot(dx, dy));
            nx = dx / safe;
            ny = dy / safe;
            bird.heisenUltimateVolleyTargetX = target.bodyCenterX();
            bird.heisenUltimateVolleyTargetY = target.bodyCenterY();
        }
        bird.heisenUltimateShardVX[shardIndex] = nx * 10.8 + Math.cos(angle) * 2.0;
        bird.heisenUltimateShardVY[shardIndex] = ny * 10.8 + Math.sin(angle) * 1.2 - 0.6;
        bird.heisenUltimateShardLaunched[shardIndex] = true;
    }

    static void updateHeisenUltimateFlyingShard(Bird bird, int shardIndex, int shardElapsed, double gameSpeed) {
        double scale = heisenUltimateShardScale(shardElapsed);
        Bird target = nearestOpiumDamageTargetFrom(bird, bird.heisenUltimateShardX[shardIndex],
                bird.heisenUltimateShardY[shardIndex], 980.0);
        if (target != null) {
            double dx = target.bodyCenterX() - bird.heisenUltimateShardX[shardIndex];
            double dy = target.bodyCenterY() - bird.heisenUltimateShardY[shardIndex];
            double safe = Math.max(1.0, Math.hypot(dx, dy));
            double desiredSpeed = 12.8 + (1.0 - scale) * 4.8;
            double turn = 0.18 + (1.0 - scale) * 0.05;
            bird.heisenUltimateShardVX[shardIndex] = bird.heisenUltimateShardVX[shardIndex] * (1.0 - turn)
                    + dx / safe * desiredSpeed * turn;
            bird.heisenUltimateShardVY[shardIndex] = bird.heisenUltimateShardVY[shardIndex] * (1.0 - turn)
                    + dy / safe * desiredSpeed * turn;
            bird.heisenUltimateVolleyTargetX = target.bodyCenterX();
            bird.heisenUltimateVolleyTargetY = target.bodyCenterY();
        }

        double speed = Math.hypot(bird.heisenUltimateShardVX[shardIndex], bird.heisenUltimateShardVY[shardIndex]);
        double maxSpeed = 18.8;
        if (speed > maxSpeed) {
            bird.heisenUltimateShardVX[shardIndex] = bird.heisenUltimateShardVX[shardIndex] / speed * maxSpeed;
            bird.heisenUltimateShardVY[shardIndex] = bird.heisenUltimateShardVY[shardIndex] / speed * maxSpeed;
        }
        bird.heisenUltimateShardX[shardIndex] += bird.heisenUltimateShardVX[shardIndex] * gameSpeed;
        bird.heisenUltimateShardY[shardIndex] += bird.heisenUltimateShardVY[shardIndex] * gameSpeed;

        double hitRadius = Math.max(4.0, 38.0 * scale) * bird.sizeMultiplier;
        for (Bird other : bird.game.players) {
            if (!bird.canDamageTarget(other)) continue;
            double dx = other.bodyCenterX() - bird.heisenUltimateShardX[shardIndex];
            double dy = other.bodyCenterY() - bird.heisenUltimateShardY[shardIndex];
            if (Math.hypot(dx, dy) > hitRadius + other.combatRadius()) continue;

            boolean marked = other.hasHeisenBrittleFrom(bird);
            int dealt = bird.applyTrackedSpecialDamage(other, marked ? 7 : 5);
            if (dealt <= 0) continue;
            bird.heisenUltimateShardSpent[shardIndex] = true;
            bird.heisenUltimateVolleyHit = true;
            double safe = Math.max(1.0, Math.hypot(bird.heisenUltimateShardVX[shardIndex],
                    bird.heisenUltimateShardVY[shardIndex]));
            other.vx += bird.heisenUltimateShardVX[shardIndex] / safe * (marked ? 12.6 : 9.2);
            other.vy += bird.heisenUltimateShardVY[shardIndex] / safe * (marked ? 7.0 : 5.0)
                    - (marked ? 6.0 : 4.2);
            other.applyStun(marked ? 18 : 12);
            if (marked) {
                other.clearHeisenBrittle();
            }
            emitBurst(bird, bird.heisenUltimateShardX[shardIndex], bird.heisenUltimateShardY[shardIndex],
                    marked ? 42 : 30, Color.web("#B3E5FC"));
            bird.game.shakeIntensity = Math.max(bird.game.shakeIntensity, marked ? 9 : 6);
            break;
        }
    }

    static double heisenUltimateShardScale(int shardElapsed) {
        double progress = Math.clamp(shardElapsed / (double) Bird.HEISEN_ULTIMATE_SHARD_FLIGHT_FRAMES, 0.0, 1.0);
        return Math.max(0.0, 1.0 - Math.pow(progress, 1.18));
    }

    static void handleState(Bird bird) {
        if (!bird.isOpiumEchoPair()) {
            return;
        }
        boolean heisen = bird.type == BirdGame3.BirdType.HEISENBIRD;
        if (bird.opiumSideTimer > 0) {
            applySideHits(bird, heisen);
            if (bird.opiumSideFueled && (bird.opiumSideTimer & 1) == 0) {
                bird.game.particles.add(new Particle(
                        bird.bodyCenterX() - bird.opiumSideDirection * 34.0 * bird.sizeMultiplier,
                        bird.bodyCenterY() + (Math.random() - 0.5) * 34.0 * bird.sizeMultiplier,
                        -bird.opiumSideDirection * (1.0 + Math.random() * 2.4),
                        (Math.random() - 0.5) * 1.8,
                        (heisen ? Color.web("#81D4FA") : Color.web("#CE93D8")).deriveColor(0, 1, 1, 0.62)
                ));
            }
        }
        if (bird.opiumUpTimer > 0) {
            applyUpHits(bird, heisen);
            if (bird.opiumUpFueled && (bird.opiumUpTimer & 1) == 0) {
                bird.game.particles.add(new Particle(
                        bird.bodyCenterX() + (Math.random() - 0.5) * 32.0 * bird.sizeMultiplier,
                        bird.bodyBottomY() - 4.0 * bird.sizeMultiplier,
                        (Math.random() - 0.5) * 1.8,
                        2.0 + Math.random() * 3.0,
                        (heisen ? Color.web("#81D4FA") : Color.web("#CE93D8")).deriveColor(0, 1, 1, 0.64)
                ));
            }
        }
    }

    static void applySideHits(Bird bird, boolean heisen) {
        boolean fueled = bird.opiumSideFueled;
        double centerX = bird.bodyCenterX() + bird.opiumSideDirection * (heisen ? 74.0 : 70.0) * bird.sizeMultiplier;
        double centerY = bird.bodyCenterY() - 4.0 * bird.sizeMultiplier;
        double reach = (heisen ? (fueled ? 118.0 : 94.0) : (fueled ? 132.0 : 100.0)) * bird.sizeMultiplier;
        double verticalReach = (heisen ? (fueled ? 58.0 : 48.0) : (fueled ? 68.0 : 52.0)) * bird.sizeMultiplier;
        for (Bird other : bird.game.players) {
            if (!bird.canDamageTarget(other)) continue;
            if (other.playerIndex < 0 || other.playerIndex >= bird.opiumSideHit.length) continue;
            if (bird.opiumSideHit[other.playerIndex]) continue;
            double dx = other.bodyCenterX() - centerX;
            double forward = dx * bird.opiumSideDirection;
            if (forward < -other.combatHalfWidth() * 0.45 || forward > reach + other.combatHalfWidth()) continue;
            if (Math.abs(other.bodyCenterY() - centerY) > verticalReach + other.combatHalfHeight()) continue;

            boolean brittle = heisen && other.hasHeisenBrittleFrom(bird);
            int dealt = bird.applyTrackedSpecialDamage(other, heisen
                    ? (fueled ? 8 : 5)
                    : (fueled ? 11 : 5));
            if (dealt <= 0) continue;
            bird.opiumSideHit[other.playerIndex] = true;
            other.vx += bird.opiumSideDirection * (heisen
                    ? (fueled ? (brittle ? 13.8 : 10.8) : 7.2)
                    : (fueled ? 11.0 : 6.8));
            other.vy -= heisen
                    ? (fueled ? (brittle ? 6.3 : 4.8) : 3.4)
                    : (fueled ? 5.2 : 3.2);
            other.applyStun(heisen ? (fueled ? 16 : 10) : (fueled ? 15 : 9));
            if (!heisen) {
                other.vx *= 0.90;
            }
        }
    }

    static void applyUpHits(Bird bird, boolean heisen) {
        boolean fueled = bird.opiumUpFueled;
        double centerX = bird.bodyCenterX();
        double centerY = bird.bodyBottomY() + (heisen ? 28.0 : 34.0) * bird.sizeMultiplier;
        double radius = (heisen ? (fueled ? 96.0 : 74.0) : (fueled ? 114.0 : 84.0)) * bird.sizeMultiplier;
        double verticalRadius = (heisen ? (fueled ? 128.0 : 102.0) : (fueled ? 146.0 : 112.0)) * bird.sizeMultiplier;
        for (Bird other : bird.game.players) {
            if (!bird.canDamageTarget(other)) continue;
            if (other.playerIndex < 0 || other.playerIndex >= bird.opiumUpHit.length) continue;
            if (bird.opiumUpHit[other.playerIndex]) continue;
            double dx = other.bodyCenterX() - centerX;
            double dy = other.bodyCenterY() - centerY;
            if (Math.abs(dx) > radius + other.combatHalfWidth()) continue;
            if (dy < -26.0 * bird.sizeMultiplier || dy > verticalRadius + other.combatHalfHeight()) continue;

            int dealt = bird.applyTrackedSpecialDamage(other, heisen
                    ? (fueled ? 7 : 4)
                    : (fueled ? 9 : 4));
            if (dealt <= 0) continue;
            bird.opiumUpHit[other.playerIndex] = true;
            other.vx += Math.signum(dx == 0.0 ? bird.facingDirection() : dx) * (heisen
                    ? (fueled ? 6.0 : 3.8)
                    : (fueled ? 5.2 : 3.4));
            other.vy += heisen
                    ? (fueled ? 9.8 : 6.8)
                    : (fueled ? 8.8 : 6.2);
            other.applyStun(heisen ? (fueled ? 12 : 8) : (fueled ? 11 : 7));
            if (heisen && fueled) {
                other.applyHeisenBrittle(bird, false);
            }
        }
    }

    static void handleTraps(Bird bird) {
        if (bird.opiumTraps.isEmpty()) {
            return;
        }
        for (Iterator<Bird.OpiumTrap> it = bird.opiumTraps.iterator(); it.hasNext(); ) {
            Bird.OpiumTrap trap = it.next();
            trap.ageFrames++;
            trap.lifeFrames--;
            for (int i = 0; i < trap.hitCooldown.length; i++) {
                if (trap.hitCooldown[i] > 0) {
                    trap.hitCooldown[i]--;
                }
            }
            if (bird.health <= 0) {
                it.remove();
                continue;
            }
            if (trap.heisen) {
                handleHeisenTrap(bird, trap, it);
                continue;
            }
            handleOpiumTrap(bird, trap, it);
        }
    }

    private static void handleHeisenTrap(Bird bird, Bird.OpiumTrap trap, Iterator<Bird.OpiumTrap> it) {
        if (birdStandingInTrap(bird, bird, trap, 74.0, 42.0)) {
            bird.refillOpiumResource(Bird.HEISEN_NODE_REFILL_PER_FRAME);
            if ((trap.ageFrames & 5) == 0) {
                bird.game.particles.add(new Particle(
                        bird.bodyCenterX() + (Math.random() - 0.5) * 28.0 * bird.sizeMultiplier,
                        bird.bodyBottomY() - 20.0 * bird.sizeMultiplier,
                        (Math.random() - 0.5) * 0.8,
                        -0.7 - Math.random() * 1.2,
                        Color.web("#B3E5FC").deriveColor(0, 1, 1, 0.72)
                ));
            }
        }
        if ((trap.ageFrames & 3) == 0) {
            bird.game.particles.add(new Particle(
                    trap.x + (Math.random() - 0.5) * 42.0,
                    trap.y - 20.0 - Math.random() * 34.0,
                    (Math.random() - 0.5) * 0.8,
                    -0.8 - Math.random() * 1.6,
                    Color.web("#81D4FA").deriveColor(0, 1, 1, 0.66)
            ));
        }
        double radius = 92.0;
        for (Bird other : bird.game.players) {
            if (!bird.canDamageTarget(other)) continue;
            if (other.playerIndex < 0 || other.playerIndex >= trap.hitCooldown.length) continue;
            if (!birdStandingInTrap(bird, other, trap, radius, 54.0)) continue;
            other.vx *= 0.93;
            if ((trap.ageFrames % 28) == 0) {
                other.applyHeisenBrittle(bird, false);
            }
            if (trap.hitCooldown[other.playerIndex] <= 0) {
                trap.hitCooldown[other.playerIndex] = 34;
                bird.applyTrackedSpecialDamage(other, 1);
            }
        }
        if (trap.lifeFrames <= 0) {
            explodeHeisenTrap(bird, trap);
            it.remove();
        }
    }

    private static void handleOpiumTrap(Bird bird, Bird.OpiumTrap trap, Iterator<Bird.OpiumTrap> it) {
        if (trap.lifeFrames <= 0) {
            it.remove();
            return;
        }
        if (birdStandingInTrap(bird, bird, trap, 108.0, 42.0)) {
            bird.refillOpiumResource(Bird.OPIUM_PATCH_REFILL_PER_FRAME);
            if ((trap.ageFrames & 7) == 0) {
                bird.game.particles.add(new Particle(
                        bird.bodyCenterX() + (Math.random() - 0.5) * 36.0 * bird.sizeMultiplier,
                        bird.bodyBottomY() - 12.0 * bird.sizeMultiplier,
                        (Math.random() - 0.5) * 0.7,
                        -0.5 - Math.random(),
                        Color.web("#E1BEE7").deriveColor(0, 1, 1, 0.62)
                ));
            }
        }
        if ((trap.ageFrames & 7) == 0) {
            bird.game.particles.add(new Particle(
                    trap.x + (Math.random() - 0.5) * 88.0,
                    trap.y - 8.0 - Math.random() * 18.0,
                    (Math.random() - 0.5) * 0.9,
                    -0.5 - Math.random() * 1.2,
                    Color.web("#CE93D8").deriveColor(0, 1, 1, 0.48)
            ));
        }
        double radius = 94.0;
        for (Bird other : bird.game.players) {
            if (!bird.canDamageTarget(other)) continue;
            if (other.playerIndex < 0 || other.playerIndex >= trap.hitCooldown.length) continue;
            if (!birdStandingInTrap(bird, other, trap, radius, 58.0)) continue;

            other.vx *= 0.86;
            other.vy *= 0.94;
            if (bird.leanTimer > 0) {
                bird.leanTimer = Math.max(bird.leanTimer, 72);
            }
            if (trap.hitCooldown[other.playerIndex] <= 0) {
                trap.hitCooldown[other.playerIndex] = 20;
                bird.applyTrackedSpecialDamage(other, 2);
            }
        }
    }

    static boolean birdStandingInTrap(Bird owner, Bird bird, Bird.OpiumTrap trap, double radius, double verticalWindow) {
        if (bird == null) {
            return false;
        }
        double dx = bird.bodyCenterX() - trap.x;
        if (Math.abs(dx) > radius + bird.combatHalfWidth()) {
            return false;
        }
        double feetDistance = Math.abs(bird.bodyBottomY() - trap.y);
        return feetDistance <= verticalWindow + bird.combatHalfHeight() * 0.22
                || (bird.bodyCenterY() > trap.y - verticalWindow - 16.0 && bird.bodyCenterY() < trap.y + 24.0);
    }

    static void explodeHeisenTrap(Bird bird, Bird.OpiumTrap trap) {
        double radius = 132.0;
        for (Bird other : bird.game.players) {
            if (!bird.canDamageTarget(other)) continue;
            double dx = other.bodyCenterX() - trap.x;
            double dy = other.bodyCenterY() - (trap.y - 18.0);
            if (Math.hypot(dx, dy) > radius + other.combatRadius()) continue;
            boolean brittle = other.hasHeisenBrittleFrom(bird);
            int dealt = bird.applyTrackedSpecialDamage(other, brittle ? 12 : 7);
            if (dealt <= 0) continue;
            double dir = Math.signum(dx == 0.0 ? bird.facingDirection() : dx);
            other.vx += dir * (brittle ? 15.0 : 10.4);
            other.vy -= brittle ? 10.2 : 6.4;
            other.applyStun(brittle ? 24 : 14);
            if (brittle) {
                other.clearHeisenBrittle();
            }
        }
        emitBurst(bird, trap.x, trap.y - 18.0, 46, Color.web("#81D4FA"));
        bird.game.shakeIntensity = Math.max(bird.game.shakeIntensity, 8);
    }

    static boolean active(Bird bird) {
        return bird.opiumSideTimer > 0
                || bird.opiumUpTimer > 0;
    }

    static boolean ready(Bird bird, Bird.OpiumSpecialVariant variant) {
        boolean ultimateReady = bird.isUltimateReady();
        return switch (variant) {
            case NEUTRAL -> ultimateReady || bird.opiumNeutralReuseTimer <= 0;
            case SIDE -> ultimateReady || bird.opiumSideReuseTimer <= 0;
            case UP -> ultimateReady || !bird.opiumUpSpecialUsed;
            case DOWN -> ultimateReady || bird.opiumDownReuseTimer <= 0;
        };
    }

    static boolean canConvertShieldIntoDown(Bird bird) {
        return bird.selectOpiumSpecialVariant() == Bird.OpiumSpecialVariant.DOWN
                && bird.isBlocking
                && bird.shieldStunFrames <= 0;
    }

    static boolean canStart(Bird bird, boolean grabbed, boolean dodging) {
        Bird.OpiumSpecialVariant variant = bird.selectOpiumSpecialVariant();
        boolean shieldConversion = canConvertShieldIntoDown(bird);
        return bird.isOpiumEchoPair()
                && bird.health > 0
                && bird.stunTime <= 0.0
                && !grabbed
                && (!bird.isBlocking || shieldConversion)
                && !dodging
                && !active(bird)
                && ready(bird, variant);
    }

    static void reset(Bird bird) {
        bird.opiumSideTimer = 0;
        bird.opiumSideDirection = bird.facingDirection();
        bird.opiumSideFueled = false;
        Arrays.fill(bird.opiumSideHit, false);
        bird.opiumUpTimer = 0;
        bird.opiumUpFueled = false;
        Arrays.fill(bird.opiumUpHit, false);
        bird.heisenUltimateVolleyTimer = 0;
        bird.heisenUltimateVolleyHit = false;
        resetHeisenUltimateShardState(bird, true);
    }

    static void interruptOnHit(Bird bird) {
        if (!bird.isOpiumEchoPair()) {
            return;
        }
        if (active(bird)) {
            bird.attackAnimationTimer = 0;
        }
        reset(bird);
    }
}
