package com.example.birdgame3;

import com.example.birdgame3.BirdGame3.MapType;
import javafx.scene.paint.Color;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;

final class TitmouseSpecials {
    private TitmouseSpecials() {
    }

    static void use(Bird bird, boolean ultimate) {
        if (ultimate) {
            ultimate(bird);
            return;
        }
        switch (bird.selectTitmouseSpecialVariant()) {
            case NEUTRAL -> neutral(bird, false);
            case SIDE -> side(bird);
            case UP -> up(bird);
            case DOWN -> down(bird);
        }
    }

    static void neutral(Bird bird, boolean ultimate) {
        bird.titmouseScoldTimer = ultimate ? Bird.TITMOUSE_SCOLD_ACTIVE_FRAMES + 4 : Bird.TITMOUSE_SCOLD_ACTIVE_FRAMES;
        bird.titmouseScoldReuseTimer = ultimate ? 10 : Bird.TITMOUSE_SCOLD_REUSE_FRAMES;
        bird.titmouseScoldUltimate = ultimate;
        Arrays.fill(bird.titmouseScoldHit, false);
        bird.attackAnimationTimer = Math.max(bird.attackAnimationTimer, bird.titmouseScoldTimer + 3);
        bird.vx *= bird.isOnGround() ? 0.55 : 0.72;
        bird.specialCooldown = 0;
        bird.specialMaxCooldown = 0;
        if (ultimate) {
            bird.game.addToKillFeed(bird.shortName() + " raised a golden Scold Chorus!");
        }
        emitBurst(bird, bird.bodyCenterX(), bird.bodyCenterY() - 8.0 * bird.sizeMultiplier,
                ultimate ? 28 : 18,
                ultimate ? Color.GOLD : Color.web("#CFD8DC"));
    }

    static void side(Bird bird) {
        int dir = bird.horizontalInputDirection();
        if (dir == 0) {
            dir = bird.facingDirection();
        }
        bird.facingRight = dir > 0;
        bird.titmouseBarkskipDirection = dir;
        bird.titmouseBarkskipUltimate = false;
        bird.titmouseBarkskipTimer = Bird.TITMOUSE_BARKSKIP_FRAMES;
        bird.titmouseBarkskipReuseTimer = Bird.TITMOUSE_BARKSKIP_REUSE_FRAMES;
        Arrays.fill(bird.titmouseBarkskipHit, false);
        Bird.TitmouseSeedStash stash = preferredRouteStash(bird, dir, 230.0 * bird.sizeMultiplier);
        bird.titmouseBarkskipRebounded = stash != null;
        if (stash != null) {
            bird.x = stash.x - bird.bodyWidth() * 0.5;
            bird.y = stash.y - bird.bodyHeight();
            bird.vy = Math.min(bird.vy, -8.0);
            emitBurst(bird, stash.x, stash.y - 14.0, 16, Color.web("#90CAF9"));
        } else {
            bird.vy *= 0.22;
        }
        bird.vx = dir * (bird.titmouseBarkskipRebounded ? 33.0 : 28.0);
        bird.attackAnimationTimer = Math.max(bird.attackAnimationTimer, bird.titmouseBarkskipTimer + 2);
        bird.specialCooldown = 0;
        bird.specialMaxCooldown = 0;
        bird.isBlocking = false;
        bird.parryWindowFrames = 0;
        bird.shieldStunFrames = 0;
    }

    static void up(Bird bird) {
        if (bird.titmouseVaultUsed) {
            return;
        }
        bird.titmouseVaultUsed = true;
        bird.titmouseVaultUltimate = false;
        bird.titmouseVaultTimer = Bird.TITMOUSE_VAULT_FRAMES;
        bird.titmouseVaultReuseTimer = Bird.TITMOUSE_VAULT_REUSE_FRAMES;
        bird.titmouseVaultBoosted = nearestStash(bird, 150.0 * bird.sizeMultiplier) != null;
        Arrays.fill(bird.titmouseVaultHit, false);
        bird.canDoubleJump = true;
        bird.vx *= 0.24;
        bird.vy = Math.min(bird.vy, -(bird.titmouseVaultBoosted ? 30.0 : 24.0));
        bird.attackAnimationTimer = Math.max(bird.attackAnimationTimer, bird.titmouseVaultTimer);
        bird.specialCooldown = 0;
        bird.specialMaxCooldown = 0;
        emitBurst(bird, bird.bodyCenterX(), bird.bodyBottomY() - 6.0 * bird.sizeMultiplier,
                bird.titmouseVaultBoosted ? 28 : 20,
                Color.web("#B0BEC5"));
    }

    static void down(Bird bird) {
        bird.titmouseStashCharging = true;
        bird.titmouseStashHoldFrames = 0;
        bird.titmouseStashUltimate = false;
        bird.titmouseStashReuseTimer = Bird.TITMOUSE_STASH_REUSE_FRAMES;
        bird.specialCooldown = 0;
        bird.specialMaxCooldown = 0;
        bird.attackAnimationTimer = Math.max(bird.attackAnimationTimer, Bird.TITMOUSE_STASH_HOLD_FRAMES + 4);
    }

    static void placeSeedStash(Bird bird, boolean ultimate) {
        while (bird.titmouseSeedStashes.size() >= Bird.TITMOUSE_MAX_STASHES) {
            bird.titmouseSeedStashes.removeFirst();
        }
        double stashX = bird.bodyCenterX();
        double stashY = stashSurfaceY(bird, stashX);
        bird.titmouseSeedStashes.add(new Bird.TitmouseSeedStash(stashX, stashY, ultimate));
        emitBurst(bird, stashX, stashY - 10.0, ultimate ? 20 : 14,
                ultimate ? Color.GOLD : Color.web("#A1887F"));
        if (ultimate) {
            bird.game.addToKillFeed(bird.shortName() + " planted a golden Seed Stash!");
        }
    }

    static void ultimate(Bird bird) {
        bird.titmouseMobbingNodes.clear();
        ArrayList<Bird.TitmouseSeedStash> remaining = new ArrayList<>(bird.titmouseSeedStashes);
        double routeX = bird.bodyCenterX();
        double routeY = bird.bodyCenterY();
        while (!remaining.isEmpty()) {
            Bird.TitmouseSeedStash next = null;
            double nextDist = Double.MAX_VALUE;
            for (Bird.TitmouseSeedStash stash : remaining) {
                double dist = Math.hypot(stash.x - routeX, stash.y - routeY);
                if (dist < nextDist) {
                    nextDist = dist;
                    next = stash;
                }
            }
            if (next == null) break;
            bird.titmouseMobbingNodes.add(new Bird.TitmouseMobbingNode(next.x, next.y - bird.bodyHeight(), null));
            routeX = next.x;
            routeY = next.y - bird.bodyHeight();
            remaining.remove(next);
        }
        Bird target = nearestMarkedTarget(bird);
        if (target == null) {
            target = nearestDamageableTarget(bird);
        }
        if (target == null && bird.titmouseMobbingNodes.isEmpty()) {
            neutral(bird, true);
            return;
        }
        if (target != null) {
            bird.titmouseMobbingNodes.add(new Bird.TitmouseMobbingNode(target.bodyCenterX(), target.bodyCenterY(), target));
        }
        bird.titmouseMobbingTimer = Bird.TITMOUSE_MOBBING_STEP_FRAMES;
        bird.titmouseMobbingNodeIndex = 0;
        bird.isZipping = true;
        bird.zipTimer = bird.titmouseMobbingTimer;
        bird.attackAnimationTimer = Math.max(bird.attackAnimationTimer,
                Bird.TITMOUSE_MOBBING_STEP_FRAMES * bird.titmouseMobbingNodes.size() + 6);
        bird.specialCooldown = 0;
        bird.specialMaxCooldown = 0;
        bird.powerMultiplier = Math.max(bird.powerMultiplier, bird.basePowerMultiplier * 1.22);
        bird.rageTimer = Math.max(bird.rageTimer, 150);
        bird.game.addToKillFeed(bird.shortName() + " launched MOBBING RUN!");
    }

    static double stashSurfaceY(Bird bird, double stashX) {
        double bestY = bird.hasSolidGroundFloorUnderBody() ? BirdGame3.GROUND_Y : Double.POSITIVE_INFINITY;
        double sourceY = bird.bodyBottomY() - 20.0 * bird.sizeMultiplier;
        for (Platform p : bird.game.platforms) {
            boolean isCaveCeiling = bird.game.selectedMap == MapType.CAVE
                    && p.y <= 1 && p.h >= 60 && p.w >= BirdGame3.WORLD_WIDTH - 10;
            if (isCaveCeiling) continue;
            if (stashX < p.x - 20.0 || stashX > p.x + p.w + 20.0) continue;
            if (p.y < sourceY - 16.0) continue;
            if (p.y < bestY) {
                bestY = p.y;
            }
        }
        return Double.isFinite(bestY) ? bestY : bird.bodyBottomY() + 8.0 * bird.sizeMultiplier;
    }

    static Bird.TitmouseSeedStash nearestStash(Bird bird, double maxDistance) {
        Bird.TitmouseSeedStash best = null;
        double bestDistance = maxDistance;
        for (Bird.TitmouseSeedStash stash : bird.titmouseSeedStashes) {
            double dist = Math.hypot(stash.x - bird.bodyCenterX(), stash.y - bird.bodyBottomY());
            if (dist <= bestDistance) {
                bestDistance = dist;
                best = stash;
            }
        }
        return best;
    }

    static Bird.TitmouseSeedStash preferredRouteStash(Bird bird, int dir, double maxDistance) {
        Bird.TitmouseSeedStash best = null;
        double bestScore = Double.POSITIVE_INFINITY;
        double centerX = bird.bodyCenterX();
        double feetY = bird.bodyBottomY();
        for (Bird.TitmouseSeedStash stash : bird.titmouseSeedStashes) {
            double dx = stash.x - centerX;
            double dy = stash.y - feetY;
            double dist = Math.hypot(dx, dy);
            if (dist > maxDistance) continue;
            double forward = dx * dir;
            double score = dist - Math.max(0.0, forward) * 0.28 + Math.max(0.0, -forward) * 0.42;
            if (score < bestScore) {
                bestScore = score;
                best = stash;
            }
        }
        return best != null ? best : nearestStash(bird, maxDistance);
    }

    static Bird nearestMarkedTarget(Bird bird) {
        Bird best = null;
        double bestDist = Double.MAX_VALUE;
        for (Bird other : bird.game.players) {
            if (!bird.canDamageTarget(other) || !other.isTitmouseMarkedBy(bird)) continue;
            double dist = bird.combatDistanceTo(other);
            if (dist < bestDist) {
                bestDist = dist;
                best = other;
            }
        }
        return best;
    }

    static Bird nearestDamageableTarget(Bird bird) {
        Bird best = null;
        double bestDist = Double.MAX_VALUE;
        for (Bird other : bird.game.players) {
            if (!bird.canDamageTarget(other)) continue;
            double dist = bird.combatDistanceTo(other);
            if (dist < bestDist) {
                bestDist = dist;
                best = other;
            }
        }
        return best;
    }

    static void detonateSeedStashes(Bird bird, boolean ultimate) {
        if (bird.titmouseSeedStashes.isEmpty()) {
            return;
        }
        boolean hitAny = false;
        for (Bird.TitmouseSeedStash stash : bird.titmouseSeedStashes) {
            double radius = (ultimate || stash.ultimate ? 112.0 : 92.0) * bird.sizeMultiplier;
            for (Bird other : bird.game.players) {
                if (!bird.canDamageTarget(other)) continue;
                double dx = other.bodyCenterX() - stash.x;
                double dy = other.bodyCenterY() - (stash.y - 18.0 * bird.sizeMultiplier);
                if (Math.hypot(dx, dy) > radius + other.combatRadius()) continue;
                boolean marked = other.isTitmouseMarkedBy(bird);
                int dmg = (ultimate || stash.ultimate ? 12 : 8) + (marked ? 4 : 0);
                double oldHealth = other.health;
                int dealt = (int) bird.applyDamageTo(other, dmg);
                if (dealt <= 0) continue;
                hitAny = true;
                bird.game.damageDealt[bird.playerIndex] += dealt;
                bird.game.recordSpecialImpact(bird.playerIndex, dealt, true);
                bird.confirmSpecialHit(dealt, ultimate || stash.ultimate ? Color.GOLD : Color.web("#BCAAA4"));
                if (other.health <= 0 && oldHealth > 0) {
                    bird.game.eliminations[bird.playerIndex]++;
                }
                double dir = Math.signum(dx == 0.0 ? bird.facingDirection() : dx);
                other.vx += dir * ((ultimate || stash.ultimate ? 14.0 : 10.0) + (marked ? 3.0 : 0.0));
                other.vy -= (ultimate || stash.ultimate ? 11.0 : 8.0) + (marked ? 2.5 : 0.0);
                if (marked) {
                    other.applyStun(ultimate || stash.ultimate ? 14 : 9);
                    emitBurst(bird, other.bodyCenterX(), other.bodyCenterY(),
                            ultimate || stash.ultimate ? 18 : 12,
                            ultimate || stash.ultimate ? Color.GOLD : Color.web("#64B5F6"));
                }
            }
            emitBurst(bird, stash.x, stash.y - 12.0, ultimate || stash.ultimate ? 34 : 24,
                    ultimate || stash.ultimate ? Color.GOLD : Color.web("#BCAAA4"));
        }
        bird.game.shakeIntensity = Math.max(bird.game.shakeIntensity, ultimate ? 10 : 6);
        bird.game.recordTrainingTitmouseStashDetonation(bird, hitAny);
        bird.titmouseSeedStashes.clear();
    }

    static void emitBurst(Bird bird, double originX, double originY, int count, Color color) {
        for (int i = 0; i < bird.scaledParticleCount(count); i++) {
            double angle = bird.game.nextParticleRandom() * Math.PI * 2.0;
            double speed = 1.8 + bird.game.nextParticleRandom() * 4.8;
            bird.game.particles.add(new Particle(
                    originX,
                    originY,
                    Math.cos(angle) * speed,
                    Math.sin(angle) * speed - 1.5,
                    color.deriveColor(0, 1, 1, 0.78)
            ));
        }
    }

    static void handleState(Bird bird) {
        if (bird.type != BirdGame3.BirdType.TITMOUSE
                && !bird.mockingbirdCopiedNeutralFrom(BirdGame3.BirdType.TITMOUSE)) {
            return;
        }
        handleScoldChorus(bird);
        handleBarkskip(bird);
        handleTuftVault(bird);
        handleSeedStashCharge(bird);
        handleMobbingRun(bird);
    }

    static void handleScoldChorus(Bird bird) {
        if (bird.titmouseScoldTimer <= 0) {
            return;
        }
        double radius = (bird.titmouseScoldUltimate ? 152.0 : 124.0) * bird.sizeMultiplier;
        double verticalRadius = radius * 0.72;
        double centerX = bird.bodyCenterX();
        double centerY = bird.bodyCenterY() - 8.0 * bird.sizeMultiplier;
        if ((bird.titmouseScoldTimer & 2) == 0) {
            emitBurst(bird, centerX, centerY, bird.titmouseScoldUltimate ? 4 : 3,
                    bird.titmouseScoldUltimate ? Color.GOLD : Color.web("#ECEFF1"));
        }
        for (Bird other : bird.game.players) {
            if (!bird.canDamageTarget(other)) continue;
            if (other.playerIndex < 0 || other.playerIndex >= bird.titmouseScoldHit.length) continue;
            if (bird.titmouseScoldHit[other.playerIndex]) continue;
            double dx = other.bodyCenterX() - centerX;
            double dy = other.bodyCenterY() - centerY;
            double normalized = Math.hypot(dx / Math.max(1.0, radius), dy / Math.max(1.0, verticalRadius));
            if (normalized > 1.0 + other.combatRadius() / Math.max(radius, verticalRadius)) continue;

            bird.titmouseScoldHit[other.playerIndex] = true;
            int dmg = bird.titmouseScoldUltimate ? 8 : 5;
            double oldHealth = other.health;
            int dealt = (int) bird.applyDamageTo(other, dmg);
            if (dealt <= 0) continue;
            bird.game.damageDealt[bird.playerIndex] += dealt;
            bird.game.recordSpecialImpact(bird.playerIndex, dealt, true);
            bird.confirmSpecialHit(dealt, bird.titmouseScoldUltimate ? Color.GOLD : Color.web("#CFD8DC"));
            if (other.health <= 0 && oldHealth > 0) {
                bird.game.eliminations[bird.playerIndex]++;
            }
            other.applyTitmouseMark(bird, bird.titmouseScoldUltimate);
            double dir = Math.signum(dx == 0.0 ? bird.facingDirection() : dx);
            other.vx += dir * (bird.titmouseScoldUltimate ? 7.5 : 5.6);
            other.vy -= bird.titmouseScoldUltimate ? 5.8 : 4.2;
        }
    }

    static void handleBarkskip(Bird bird) {
        if (bird.titmouseBarkskipTimer <= 0) {
            return;
        }
        int dir = bird.titmouseBarkskipDirection == 0 ? bird.facingDirection() : bird.titmouseBarkskipDirection;
        bird.facingRight = dir > 0;
        boolean alreadyConnected = hasRegisteredHit(bird.titmouseBarkskipHit);
        double committedSpeed = bird.titmouseBarkskipRebounded
                ? (bird.titmouseBarkskipUltimate ? 39.0 : 33.0)
                : (bird.titmouseBarkskipUltimate ? 34.0 : 28.0);
        double activeSpeed = alreadyConnected
                ? Math.min(committedSpeed, Bird.TITMOUSE_BARKSKIP_HIT_SPEED)
                : Math.max(Math.abs(bird.vx), committedSpeed);
        bird.vx = dir * activeSpeed;
        bird.vy *= 0.82;
        if ((bird.titmouseBarkskipTimer & 1) == 0) {
            emitBurst(bird, bird.bodyCenterX() - dir * 28.0 * bird.sizeMultiplier, bird.bodyCenterY(),
                    bird.titmouseBarkskipUltimate ? 4 : 3,
                    bird.titmouseBarkskipUltimate ? Color.GOLD : Color.web("#90CAF9"));
        }
        for (Bird other : bird.game.players) {
            if (!bird.canDamageTarget(other)) continue;
            if (other.playerIndex < 0 || other.playerIndex >= bird.titmouseBarkskipHit.length) continue;
            if (bird.titmouseBarkskipHit[other.playerIndex]) continue;
            double forward = (other.bodyCenterX() - bird.bodyCenterX()) * dir;
            if (forward < -other.combatHalfWidth() * 0.25) continue;
            if (forward > (bird.titmouseBarkskipRebounded ? 116.0 : 96.0) * bird.sizeMultiplier
                    + other.combatHalfWidth()) continue;
            if (Math.abs(other.bodyCenterY() - bird.bodyCenterY()) > 68.0 * bird.sizeMultiplier
                    + other.combatHalfHeight()) continue;

            boolean firstConfirmedHit = !hasRegisteredHit(bird.titmouseBarkskipHit);
            bird.titmouseBarkskipHit[other.playerIndex] = true;
            boolean marked = other.isTitmouseMarkedBy(bird);
            int dmg = bird.titmouseBarkskipUltimate
                    ? (marked ? 16 : 12)
                    : (marked ? 12 : 8);
            double oldHealth = other.health;
            int dealt = (int) bird.applyDamageTo(other, dmg);
            if (dealt <= 0) continue;
            bird.game.damageDealt[bird.playerIndex] += dealt;
            bird.game.recordSpecialImpact(bird.playerIndex, dealt, true);
            bird.confirmSpecialHit(dealt, bird.titmouseBarkskipUltimate ? Color.GOLD : Color.web("#90CAF9"));
            if (other.health <= 0 && oldHealth > 0) {
                bird.game.eliminations[bird.playerIndex]++;
            }
            other.vx += dir * (marked
                    ? (bird.titmouseBarkskipUltimate ? 20.0 : 16.0)
                    : (bird.titmouseBarkskipUltimate ? 16.0 : 12.0));
            other.vy -= marked
                    ? (bird.titmouseBarkskipUltimate ? 12.0 : 9.0)
                    : (bird.titmouseBarkskipUltimate ? 9.0 : 6.5);
            if (firstConfirmedHit) {
                bird.titmouseBarkskipTimer = Math.min(
                        bird.titmouseBarkskipTimer,
                        Bird.TITMOUSE_BARKSKIP_HIT_RECOVERY_FRAMES);
                bird.vx = dir * Bird.TITMOUSE_BARKSKIP_HIT_SPEED;
                bird.game.hitstopFrames = Math.max(bird.game.hitstopFrames, marked ? 5 : 3);
                bird.game.shakeIntensity = Math.max(bird.game.shakeIntensity, marked ? 8 : 5);
            }
            if (marked) {
                confirmMarkedRouteHit(bird, other);
            }
            if (marked && bird.titmouseBarkskipRebounded) {
                bird.canDoubleJump = true;
                bird.vy = Math.min(bird.vy, bird.titmouseBarkskipUltimate ? -13.0 : -10.0);
                bird.titmouseVaultUsed = false;
                emitBurst(bird, other.bodyCenterX(), other.bodyCenterY(),
                        bird.titmouseBarkskipUltimate ? 24 : 16,
                        bird.titmouseBarkskipUltimate ? Color.GOLD : Color.web("#4FC3F7"));
            }
        }
    }

    static void handleTuftVault(Bird bird) {
        if (bird.titmouseVaultTimer <= 0) {
            return;
        }
        if ((bird.titmouseVaultTimer & 2) == 0) {
            emitBurst(bird, bird.bodyCenterX(), bird.bodyCenterY() + 18.0 * bird.sizeMultiplier,
                    bird.titmouseVaultUltimate ? 4 : 3,
                    bird.titmouseVaultUltimate ? Color.GOLD : Color.web("#CFD8DC"));
        }
        double radius = (bird.titmouseVaultBoosted ? 88.0 : 72.0) * bird.sizeMultiplier;
        double verticalReach = (bird.titmouseVaultBoosted ? 94.0 : 78.0) * bird.sizeMultiplier;
        for (Bird other : bird.game.players) {
            if (!bird.canDamageTarget(other)) continue;
            if (other.playerIndex < 0 || other.playerIndex >= bird.titmouseVaultHit.length) continue;
            if (bird.titmouseVaultHit[other.playerIndex]) continue;
            double dx = other.bodyCenterX() - bird.bodyCenterX();
            double dy = other.bodyCenterY() - bird.bodyCenterY();
            if (Math.abs(dx) > radius + other.combatHalfWidth()) continue;
            if (Math.abs(dy) > verticalReach + other.combatHalfHeight()) continue;
            bird.titmouseVaultHit[other.playerIndex] = true;
            boolean marked = other.isTitmouseMarkedBy(bird);
            int dmg = (bird.titmouseVaultUltimate
                    ? (bird.titmouseVaultBoosted ? 15 : 12)
                    : (bird.titmouseVaultBoosted ? 11 : 8)) + (marked ? 3 : 0);
            double oldHealth = other.health;
            int dealt = (int) bird.applyDamageTo(other, dmg);
            if (dealt <= 0) continue;
            bird.game.damageDealt[bird.playerIndex] += dealt;
            bird.game.recordSpecialImpact(bird.playerIndex, dealt, true);
            bird.confirmSpecialHit(dealt, bird.titmouseVaultUltimate ? Color.GOLD : Color.web("#CFD8DC"));
            if (other.health <= 0 && oldHealth > 0) {
                bird.game.eliminations[bird.playerIndex]++;
            }
            double dir = Math.signum(dx == 0.0 ? bird.facingDirection() : dx);
            other.vx += dir * (bird.titmouseVaultBoosted
                    ? (bird.titmouseVaultUltimate ? 13.0 : 10.0)
                    : (bird.titmouseVaultUltimate ? 10.0 : 7.5)) + dir * (marked ? 2.0 : 0.0);
            other.vy -= (bird.titmouseVaultBoosted
                    ? (bird.titmouseVaultUltimate ? 15.0 : 12.0)
                    : (bird.titmouseVaultUltimate ? 12.0 : 9.0)) + (marked ? 2.0 : 0.0);
            if (marked) {
                confirmMarkedRouteHit(bird, other);
            }
        }
    }

    private static boolean hasRegisteredHit(boolean[] hitFlags) {
        for (boolean hit : hitFlags) {
            if (hit) {
                return true;
            }
        }
        return false;
    }

    private static void confirmMarkedRouteHit(Bird bird, Bird target) {
        bird.game.hitstopFrames = Math.max(bird.game.hitstopFrames, 5);
        bird.game.shakeIntensity = Math.max(bird.game.shakeIntensity, 8);
        emitBurst(bird, target.bodyCenterX(), target.bodyCenterY(), 16, Color.web("#4FC3F7"));
    }

    static void handleSeedStashCharge(Bird bird) {
        if (!bird.titmouseStashCharging) {
            return;
        }
        boolean stillHolding = bird.specialHeld() && bird.blockPressed();
        if (stillHolding && bird.titmouseStashHoldFrames < Bird.TITMOUSE_STASH_HOLD_FRAMES) {
            bird.titmouseStashHoldFrames++;
            bird.attackAnimationTimer = Math.max(bird.attackAnimationTimer, 4);
            bird.vx *= bird.isOnGround() ? 0.56 : 0.78;
            if ((bird.titmouseStashHoldFrames & 3) == 0) {
                emitBurst(bird, bird.bodyCenterX(), bird.bodyBottomY() - 10.0 * bird.sizeMultiplier,
                        3,
                        bird.titmouseStashUltimate ? Color.GOLD : Color.web("#BCAAA4"));
            }
            if (bird.titmouseStashHoldFrames < Bird.TITMOUSE_STASH_HOLD_FRAMES) {
                return;
            }
        }

        if (bird.titmouseStashHoldFrames >= Bird.TITMOUSE_STASH_HOLD_FRAMES
                && !bird.titmouseSeedStashes.isEmpty()) {
            detonateSeedStashes(bird, bird.titmouseStashUltimate);
        } else {
            placeSeedStash(bird, bird.titmouseStashUltimate);
        }
        bird.titmouseStashCharging = false;
        bird.titmouseStashHoldFrames = 0;
        bird.titmouseStashUltimate = false;
    }

    static void handleMobbingRun(Bird bird) {
        if (bird.titmouseMobbingNodes.isEmpty()) {
            if (bird.titmouseMobbingTimer <= 0) {
                bird.isZipping = false;
                bird.zipTimer = 0;
            }
            return;
        }
        if (bird.titmouseMobbingNodeIndex >= bird.titmouseMobbingNodes.size()) {
            bird.titmouseMobbingNodes.clear();
            bird.titmouseMobbingTimer = 0;
            bird.isZipping = false;
            bird.zipTimer = 0;
            return;
        }
        Bird.TitmouseMobbingNode node = bird.titmouseMobbingNodes.get(bird.titmouseMobbingNodeIndex);
        double targetX = node.target() != null ? node.target().bodyCenterX() - bird.bodyWidth() * 0.5
                : node.x() - bird.bodyWidth() * 0.5;
        double targetY = node.target() != null ? node.target().bodyCenterY() - bird.bodyHeight() * 0.5 : node.y();
        bird.zipTargetX = targetX;
        bird.zipTargetY = targetY;
        bird.zipTimer = bird.titmouseMobbingTimer;
        double dx = targetX - bird.x;
        double dy = targetY - bird.y;
        bird.x += dx * 0.58;
        bird.y += dy * 0.58;
        bird.vx = 0.0;
        bird.vy = 0.0;
        emitBurst(bird, bird.bodyCenterX(), bird.bodyCenterY(), 5, Color.GOLD);
        if (bird.titmouseMobbingTimer > 0) {
            return;
        }
        bird.x = targetX;
        bird.y = targetY;
        if (node.target() != null && bird.canDamageTarget(node.target())) {
            applyMobbingImpact(bird, node.target());
        }
        bird.titmouseMobbingNodeIndex++;
        if (bird.titmouseMobbingNodeIndex < bird.titmouseMobbingNodes.size()) {
            bird.titmouseMobbingTimer = Bird.TITMOUSE_MOBBING_STEP_FRAMES;
        } else {
            bird.titmouseMobbingNodes.clear();
            bird.titmouseMobbingTimer = 0;
            bird.isZipping = false;
            bird.zipTimer = 0;
            bird.titmouseSeedStashes.clear();
        }
    }

    static void applyMobbingImpact(Bird bird, Bird target) {
        boolean marked = target.isTitmouseMarkedBy(bird);
        int dmg = marked ? 28 : 22;
        double oldHealth = target.health;
        int dealt = (int) bird.applyDamageTo(target, dmg);
        if (dealt <= 0) {
            return;
        }
        bird.game.damageDealt[bird.playerIndex] += dealt;
        bird.game.recordSpecialImpact(bird.playerIndex, dealt, true);
        bird.confirmSpecialHit(dealt, Color.GOLD);
        if (target.health <= 0 && oldHealth > 0) {
            bird.game.eliminations[bird.playerIndex]++;
        }
        double dir = Math.signum(target.bodyCenterX() - bird.bodyCenterX());
        if (dir == 0.0) {
            dir = bird.facingDirection();
        }
        target.vx += dir * (marked ? 28.0 : 24.0);
        target.vy -= marked ? 20.0 : 17.0;
        bird.game.hitstopFrames = Math.max(bird.game.hitstopFrames, 10);
        bird.game.shakeIntensity = Math.max(bird.game.shakeIntensity, 22);
        bird.game.triggerFlash(0.6, target.health <= 0);
        emitBurst(bird, target.bodyCenterX(), target.bodyCenterY(), 48, Color.GOLD);
    }

    static void handleSeedStashes(Bird bird) {
        if (bird.titmouseSeedStashes.isEmpty()) {
            return;
        }
        for (Iterator<Bird.TitmouseSeedStash> it = bird.titmouseSeedStashes.iterator(); it.hasNext(); ) {
            Bird.TitmouseSeedStash stash = it.next();
            stash.ageFrames++;
            stash.lifeFrames--;
            if (stash.lifeFrames <= 0 || bird.health <= 0) {
                it.remove();
                continue;
            }
            if ((stash.ageFrames & 15) == 0) {
                bird.game.particles.add(new Particle(
                        stash.x + (bird.game.nextParticleRandom() - 0.5) * 28.0,
                        stash.y - 8.0,
                        (bird.game.nextParticleRandom() - 0.5) * 0.9,
                        -0.4 - bird.game.nextParticleRandom() * 1.1,
                        (stash.ultimate ? Color.GOLD : Color.web("#BCAAA4")).deriveColor(0, 1, 1, 0.56)
                ));
            }
        }
    }

    static boolean active(Bird bird) {
        return bird.titmouseScoldTimer > 0
                || bird.titmouseBarkskipTimer > 0
                || bird.titmouseVaultTimer > 0
                || bird.titmouseStashCharging
                || bird.titmouseMobbingTimer > 0;
    }

    static boolean ready(Bird bird, Bird.TitmouseSpecialVariant variant) {
        return switch (variant) {
            case NEUTRAL -> bird.isUltimateReady() || bird.titmouseScoldReuseTimer <= 0;
            case SIDE -> bird.titmouseBarkskipReuseTimer <= 0;
            case UP -> !bird.titmouseVaultUsed && bird.titmouseVaultReuseTimer <= 0;
            case DOWN -> bird.titmouseStashReuseTimer <= 0;
        };
    }

    static boolean canConvertShieldIntoDown(Bird bird) {
        return bird.selectTitmouseSpecialVariant() == Bird.TitmouseSpecialVariant.DOWN
                && bird.isBlocking
                && bird.shieldStunFrames <= 0;
    }

    static boolean canStart(Bird bird, boolean grabbed, boolean dodging) {
        Bird.TitmouseSpecialVariant variant = bird.selectTitmouseSpecialVariant();
        boolean shieldConversion = canConvertShieldIntoDown(bird);
        return bird.type == BirdGame3.BirdType.TITMOUSE
                && bird.health > 0
                && bird.stunTime <= 0.0
                && !grabbed
                && (!bird.isBlocking || shieldConversion)
                && !dodging
                && !active(bird)
                && ready(bird, variant);
    }

    static void reset(Bird bird, boolean clearObjects) {
        bird.titmouseScoldTimer = 0;
        bird.titmouseScoldUltimate = false;
        Arrays.fill(bird.titmouseScoldHit, false);
        bird.titmouseBarkskipTimer = 0;
        bird.titmouseBarkskipUltimate = false;
        bird.titmouseBarkskipRebounded = false;
        Arrays.fill(bird.titmouseBarkskipHit, false);
        bird.titmouseVaultTimer = 0;
        bird.titmouseVaultUltimate = false;
        bird.titmouseVaultBoosted = false;
        Arrays.fill(bird.titmouseVaultHit, false);
        bird.titmouseStashCharging = false;
        bird.titmouseStashHoldFrames = 0;
        bird.titmouseStashUltimate = false;
        bird.titmouseMobbingNodes.clear();
        bird.titmouseMobbingTimer = 0;
        bird.titmouseMobbingNodeIndex = 0;
        bird.isZipping = false;
        bird.zipTimer = 0;
        if (clearObjects) {
            bird.titmouseSeedStashes.clear();
        }
    }

    static void interruptOnHit(Bird bird) {
        if (bird.type != BirdGame3.BirdType.TITMOUSE) {
            return;
        }
        if (active(bird)) {
            bird.attackAnimationTimer = 0;
        }
        reset(bird, false);
    }
}
