package com.example.birdgame3;

import javafx.scene.paint.Color;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;

final class PenguinSpecials {
    static final String ABSOLUTE_ZERO_FORTRESS_MOVE = "Penguin Absolute Zero Fortress";
    private static final int ABSOLUTE_ZERO_FIRST_LAUNCH_FRAME = 8;

    private PenguinSpecials() {
    }

    static void use(Bird bird, boolean ultimate) {
        if (ultimate) {
            startAbsoluteZeroFortress(bird);
            return;
        }
        switch (bird.selectPenguinSpecialVariant()) {
            case NEUTRAL -> neutral(bird, ultimate);
            case SIDE -> side(bird, ultimate);
            case UP -> up(bird, ultimate);
            case DOWN -> down(bird, ultimate);
        }
    }

    static void neutral(Bird bird, boolean ultimate) {
        int dir = bird.horizontalInputDirection();
        if (dir == 0) {
            dir = bird.facingDirection();
        }
        bird.facingRight = dir > 0;
        bird.penguinBellyDirection = dir;
        bird.penguinBellyCharging = true;
        bird.penguinBellyChargeFrames = 1;
        bird.penguinBellySlideTimer = 0;
        bird.penguinBellyReuseTimer = ultimate ? 8 : Bird.PENGUIN_BELLY_REUSE_FRAMES;
        bird.penguinBellyUltimate = ultimate;
        Arrays.fill(bird.penguinBellyHit, false);
        bird.specialCooldown = 0;
        bird.specialMaxCooldown = 0;
        bird.attackAnimationTimer = Math.max(bird.attackAnimationTimer, 10);
        bird.vx *= bird.isOnGround() ? 0.48 : 0.70;
        if (ultimate) {
            bird.game.addToKillFeed(bird.shortName() + " ULT BELLY SLIDE CHARGE!");
        }
        emitIceBurst(bird, bird.bodyCenterX(), bird.bodyBottomY() - 8.0 * bird.sizeMultiplier, dir,
                ultimate ? 26 : 16, ultimate ? Color.GOLD : Color.web("#B3E5FC"));
    }

    static void side(Bird bird, boolean ultimate) {
        int dir = bird.horizontalInputDirection();
        if (dir == 0) {
            dir = bird.facingDirection();
        }
        bird.facingRight = dir > 0;
        boolean snowball = false;
        boolean airborne = !bird.isOnGround();
        double spawnX = bird.bodyCenterX() + dir * 74.0 * bird.sizeMultiplier;
        double spawnY = airborne
                ? bird.bodyCenterY() + 2.0 * bird.sizeMultiplier
                : objectSurfaceY(bird, spawnX) - 42.0 * bird.sizeMultiplier;
        if (!airborne && bird.penguinSnowFort != null && bird.penguinSnowFort.health > 0) {
            double fortForward = (bird.penguinSnowFort.x - bird.bodyCenterX()) * dir;
            if (fortForward > 16.0 * bird.sizeMultiplier && fortForward < 190.0 * bird.sizeMultiplier
                    && Math.abs(bird.penguinSnowFort.y - objectSurfaceY(bird, bird.penguinSnowFort.x)) < 26.0 * bird.sizeMultiplier) {
                snowball = true;
                spawnX = bird.penguinSnowFort.x + dir * 24.0 * bird.sizeMultiplier;
                spawnY = bird.penguinSnowFort.y - 54.0 * bird.sizeMultiplier;
                bird.penguinSnowFort.health = 0;
                bird.penguinSnowFort.damageFlash = 8;
                bird.game.shakeIntensity = Math.max(bird.game.shakeIntensity, ultimate ? 11 : 7);
            }
        }
        double speed = snowball ? (ultimate ? 11.4 : 9.4) : (ultimate ? 7.1 : 5.8);
        Bird.PenguinIceObject object = new Bird.PenguinIceObject(spawnX, spawnY, dir * speed,
                snowball ? -2.1 : -0.6, dir, ultimate, snowball);
        bird.penguinIceObjects.add(object);
        while (bird.penguinIceObjects.size() > (ultimate ? 4 : 3)) {
            bird.penguinIceObjects.removeFirst();
        }
        bird.penguinIcebergReuseTimer = ultimate ? 18 : Bird.PENGUIN_ICEBERG_REUSE_FRAMES;
        bird.specialCooldown = 0;
        bird.specialMaxCooldown = 0;
        bird.attackAnimationTimer = Math.max(bird.attackAnimationTimer, 12);
        bird.vx -= dir * (snowball ? 3.4 : 1.8);
        if (ultimate) {
            bird.game.addToKillFeed(bird.shortName() + (snowball ? " ULT FORT SNOWBALL!" : " ULT ICEBERG SHOVE!"));
        }
        emitIceBurst(bird, spawnX, spawnY, dir, snowball ? 54 : 32,
                ultimate ? Color.GOLD : Color.web("#90CAF9"));
    }

    static void up(Bird bird, boolean ultimate) {
        if (bird.penguinUpSpecialUsed && !ultimate) {
            return;
        }
        bird.penguinUpSpecialUsed = true;
        bird.penguinFlopTimer = 0;
        bird.penguinRocketUltimate = ultimate;
        bird.penguinRocketTimer = rocketTotalFrames(bird);
        Arrays.fill(bird.penguinRocketHit, false);
        Arrays.fill(bird.penguinFlopHit, false);
        bird.canDoubleJump = true;
        bird.specialCooldown = 0;
        bird.specialMaxCooldown = 0;
        bird.attackAnimationTimer = Math.max(bird.attackAnimationTimer, bird.penguinRocketTimer);
        int launchDir = bird.horizontalInputDirection();
        if (launchDir != 0) {
            bird.facingRight = launchDir > 0;
        }
        bird.vx = bird.vx * 0.46 + launchDir * (ultimate ? 4.9 : 3.9);
        bird.vy = Math.min(bird.vy, ultimate ? -19.2 : -16.5);
        if (ultimate) {
            bird.game.addToKillFeed(bird.shortName() + " ULT ROCKET FLOP!");
        }
        emitIceBurst(bird, bird.bodyCenterX(), bird.bodyBottomY() - 4.0 * bird.sizeMultiplier,
                bird.facingDirection(), ultimate ? 54 : 36, ultimate ? Color.GOLD : Color.web("#E1F5FE"));
    }

    static void down(Bird bird, boolean ultimate) {
        if (!bird.isOnGround()) {
            dropAirIceberg(bird, ultimate);
            return;
        }
        int dir = bird.facingDirection();
        double fortX = bird.bodyCenterX() + dir * 92.0 * bird.sizeMultiplier;
        double fortY = objectSurfaceY(bird, fortX);
        bird.penguinSnowFort = new Bird.PenguinSnowFort(fortX, fortY, dir, ultimate);
        bird.penguinSnowFortReuseTimer = ultimate ? 22 : Bird.PENGUIN_SNOW_FORT_REUSE_FRAMES;
        bird.specialCooldown = 0;
        bird.specialMaxCooldown = 0;
        bird.attackAnimationTimer = Math.max(bird.attackAnimationTimer, 14);
        bird.vx *= bird.isOnGround() ? 0.54 : 0.78;
        if (ultimate) {
            bird.game.addToKillFeed(bird.shortName() + " BUILT A ROYAL SNOW FORT!");
        }
        emitIceBurst(bird, fortX, fortY - 20.0 * bird.sizeMultiplier, dir, ultimate ? 44 : 30,
                ultimate ? Color.GOLD : Color.WHITE);
    }

    private static void dropAirIceberg(Bird bird, boolean ultimate) {
        int dir = bird.facingDirection();
        double s = bird.sizeMultiplier;
        double spawnX = bird.bodyCenterX();
        double spawnY = bird.bodyBottomY() + 26.0 * s;
        double dropSpeed = Math.max(5.8 * s, Math.max(0.0, bird.vy) + (ultimate ? 4.8 : 3.6));
        Bird.PenguinIceObject object = new Bird.PenguinIceObject(
                spawnX,
                spawnY,
                0.0,
                dropSpeed,
                dir,
                ultimate,
                false,
                true);
        bird.penguinIceObjects.add(object);
        while (bird.penguinIceObjects.size() > 4) {
            bird.penguinIceObjects.removeFirst();
        }
        bird.penguinSnowFortReuseTimer = ultimate ? 20 : Bird.PENGUIN_SNOW_FORT_REUSE_FRAMES;
        bird.specialCooldown = 0;
        bird.specialMaxCooldown = 0;
        bird.attackAnimationTimer = Math.max(bird.attackAnimationTimer, 14);
        bird.vx *= 0.78;
        bird.vy = Math.min(bird.vy, 1.1);
        emitIceBurst(bird, spawnX, spawnY, dir, ultimate ? 34 : 22,
                ultimate ? Color.GOLD : Color.web("#E1F5FE"));
    }

    static void handleState(Bird bird, boolean specialHeld) {
        if (bird.type != BirdGame3.BirdType.PENGUIN && !bird.mockingbirdCopiedNeutralFrom(BirdGame3.BirdType.PENGUIN)) {
            return;
        }
        if (bird.stunTime > 0.0) {
            reset(bird, false);
            if (bird.mockingbirdCopiedNeutralFrom(BirdGame3.BirdType.PENGUIN)) {
                bird.mockingbirdCopiedNeutralSource = null;
            }
            return;
        }
        if (bird.type == BirdGame3.BirdType.PENGUIN && bird.penguinAbsoluteZeroTimer > 0) {
            handleAbsoluteZeroFortress(bird);
            return;
        }
        if (bird.penguinBellyCharging) {
            handleBellyCharge(bird, specialHeld);
        }
        if (bird.penguinBellySlideTimer > 0) {
            handleBellySlide(bird);
        }
        if (bird.penguinRocketTimer > 0) {
            handleRocket(bird, specialHeld);
        }
        if (bird.penguinFlopTimer > 0) {
            handleFlop(bird);
        }
    }

    static boolean active(Bird bird) {
        return bird.penguinBellyCharging
                || bird.penguinBellySlideTimer > 0
                || bird.penguinRocketTimer > 0
                || bird.penguinFlopTimer > 0
                || bird.penguinAbsoluteZeroTimer > 0;
    }

    static boolean ready(Bird bird, Bird.PenguinSpecialVariant variant) {
        boolean ultimateReady = bird.isUltimateReady();
        return switch (variant) {
            case NEUTRAL -> ultimateReady || bird.penguinBellyReuseTimer <= 0;
            case SIDE -> ultimateReady || bird.penguinIcebergReuseTimer <= 0;
            case UP -> ultimateReady || !bird.penguinUpSpecialUsed;
            case DOWN -> ultimateReady || bird.penguinSnowFortReuseTimer <= 0;
        };
    }

    static boolean canConvertShieldIntoDown(Bird bird) {
        return bird.selectPenguinSpecialVariant() == Bird.PenguinSpecialVariant.DOWN
                && bird.isBlocking
                && bird.shieldStunFrames <= 0;
    }

    static void interruptOnHit(Bird bird) {
        if (bird.type != BirdGame3.BirdType.PENGUIN) {
            return;
        }
        if (active(bird)) {
            bird.attackAnimationTimer = 0;
        }
        reset(bird, false);
    }

    static void reset(Bird bird, boolean clearObjects) {
        bird.penguinBellyCharging = false;
        bird.penguinBellyChargeFrames = 0;
        bird.penguinBellySlideTimer = 0;
        bird.penguinBellyUltimate = false;
        Arrays.fill(bird.penguinBellyHit, false);
        bird.penguinRocketTimer = 0;
        bird.penguinFlopTimer = 0;
        bird.penguinRocketUltimate = false;
        Arrays.fill(bird.penguinRocketHit, false);
        Arrays.fill(bird.penguinFlopHit, false);
        bird.penguinIceFxTimer = 0;
        bird.penguinDashDamageTimer = 0;
        bird.penguinAbsoluteZeroTimer = 0;
        bird.penguinAbsoluteZeroWaveIndex = 0;
        bird.penguinAbsoluteZeroThroneX = 0.0;
        bird.penguinAbsoluteZeroThroneY = 0.0;
        Arrays.fill(bird.penguinDashHit, false);
        if (clearObjects) {
            bird.penguinIceObjects.clear();
            bird.penguinSnowFort = null;
        }
    }

    static double bellyChargeRatio(Bird bird) {
        return Math.clamp(bird.penguinBellyChargeFrames / (double) Bird.PENGUIN_BELLY_CHARGE_MAX_FRAMES, 0.0, 1.0);
    }

    static int rocketTotalFrames(Bird bird) {
        return bird.penguinRocketUltimate ? Bird.PENGUIN_ROCKET_FRAMES + 6 : Bird.PENGUIN_ROCKET_FRAMES;
    }

    static int flopTotalFrames(Bird bird) {
        return bird.penguinRocketUltimate ? Bird.PENGUIN_FLOP_FRAMES + 16 : Bird.PENGUIN_FLOP_FRAMES;
    }

    static double rocketProgress(Bird bird) {
        return specialPhase(bird.penguinRocketTimer, rocketTotalFrames(bird));
    }

    static double flopProgress(Bird bird) {
        return specialPhase(bird.penguinFlopTimer, flopTotalFrames(bird));
    }

    static int absoluteZeroElapsed(Bird bird) {
        return Math.max(0, Bird.PENGUIN_ABSOLUTE_ZERO_FRAMES - bird.penguinAbsoluteZeroTimer);
    }

    static int absoluteZeroLaunchFrame(int waveIndex) {
        return ABSOLUTE_ZERO_FIRST_LAUNCH_FRAME + waveIndex * Bird.PENGUIN_ABSOLUTE_ZERO_WAVE_INTERVAL;
    }

    static int absoluteZeroImpactFrame(int waveIndex) {
        return absoluteZeroLaunchFrame(waveIndex) + Bird.PENGUIN_ABSOLUTE_ZERO_IMPACT_DELAY;
    }

    static double absoluteZeroImpactX(Bird bird, int waveIndex) {
        Bird target = absoluteZeroTarget(bird, waveIndex);
        double baseX = target == null ? bird.penguinAbsoluteZeroThroneX : target.bodyCenterX();
        if (baseX == 0.0) {
            baseX = bird.bodyCenterX();
        }
        double pattern = switch (Math.floorMod(waveIndex, 6)) {
            case 0 -> 0.0;
            case 1 -> -92.0;
            case 2 -> 92.0;
            case 3 -> -42.0;
            case 4 -> 42.0;
            default -> 0.0;
        };
        double s = bird.sizeMultiplier;
        double left = bird.game.battlefieldLeftBound() + 140.0 * s;
        double right = bird.game.battlefieldRightBound() - 140.0 * s;
        return clampBetween(baseX + pattern * s, left, right);
    }

    static double absoluteZeroImpactSurfaceY(Bird bird, double impactX) {
        return objectSurfaceY(bird, impactX);
    }

    static double fortHalfWidth(Bird bird, Bird.PenguinSnowFort fort) {
        return (fort.ultimate ? 72.0 : 58.0) * bird.sizeMultiplier;
    }

    static double fortHeight(Bird bird, Bird.PenguinSnowFort fort) {
        return (fort.ultimate ? 98.0 : 82.0) * bird.sizeMultiplier;
    }

    static int fortMaxHealth(Bird.PenguinSnowFort fort) {
        return fort.ultimate ? Bird.PENGUIN_SNOW_FORT_HEALTH + 34 : Bird.PENGUIN_SNOW_FORT_HEALTH;
    }

    static double objectSurfaceY(Bird bird, double objectX) {
        double surfaceY = objectSurfaceYOrVoid(bird, objectX, bird.bodyBottomY() - 36.0 * bird.sizeMultiplier);
        return Double.isFinite(surfaceY) ? surfaceY : bird.bodyBottomY() + 8.0 * bird.sizeMultiplier;
    }

    private static double objectSurfaceYOrVoid(Bird bird, double objectX, double sourceY) {
        double bestY = bird.hasSolidGroundFloorUnderBody() ? BirdGame3.GROUND_Y : Double.POSITIVE_INFINITY;
        for (Platform p : bird.game.platforms) {
            boolean isCaveCeiling = bird.game.selectedMap == BirdGame3.MapType.CAVE
                    && p.y <= 1 && p.h >= 60 && p.w >= BirdGame3.WORLD_WIDTH - 10;
            if (isCaveCeiling) continue;
            if (objectX < p.x - 24.0 || objectX > p.x + p.w + 24.0) continue;
            if (p.y < sourceY - 24.0) continue;
            if (p.y < bestY) {
                bestY = p.y;
            }
        }
        return bestY;
    }

    static void emitIceBurst(Bird bird, double originX, double originY, int dir, int count, Color baseColor) {
        int particleCount = bird.scaledParticleCount(count);
        for (int i = 0; i < particleCount; i++) {
            double angle = -Math.PI / 2.0 + (bird.game.nextParticleRandom() - 0.5) * Math.PI * 1.3;
            double speed = 1.4 + bird.game.nextParticleRandom() * 5.4;
            bird.game.particles.add(new Particle(
                    originX + (bird.game.nextParticleRandom() - 0.5) * 18.0 * bird.sizeMultiplier,
                    originY + (bird.game.nextParticleRandom() - 0.5) * 12.0 * bird.sizeMultiplier,
                    Math.cos(angle) * speed + dir * (0.4 + bird.game.nextParticleRandom() * 1.3),
                    Math.sin(angle) * speed - bird.game.nextParticleRandom() * 2.4,
                    baseColor.deriveColor(0, 1, 1, 0.70 + bird.game.nextParticleRandom() * 0.18)
            ));
        }
    }

    private static void startAbsoluteZeroFortress(Bird bird) {
        reset(bird, false);
        double s = bird.sizeMultiplier;
        double left = bird.game.battlefieldLeftBound() + 210.0 * s;
        double right = bird.game.battlefieldRightBound() - 210.0 * s;
        double throneX = clampBetween(bird.bodyCenterX(), left, right);
        double throneY = objectSurfaceY(bird, throneX);
        bird.penguinAbsoluteZeroTimer = Bird.PENGUIN_ABSOLUTE_ZERO_FRAMES;
        bird.penguinAbsoluteZeroWaveIndex = 0;
        bird.penguinAbsoluteZeroThroneX = throneX;
        bird.penguinAbsoluteZeroThroneY = throneY;
        bird.penguinIceFxTimer = Math.max(bird.penguinIceFxTimer, Bird.PENGUIN_ABSOLUTE_ZERO_FRAMES);
        bird.attackAnimationTimer = Math.max(bird.attackAnimationTimer, Bird.PENGUIN_ABSOLUTE_ZERO_FRAMES);
        bird.penguinBellyReuseTimer = Math.max(bird.penguinBellyReuseTimer, 18);
        bird.penguinIcebergReuseTimer = Math.max(bird.penguinIcebergReuseTimer, 18);
        bird.penguinSnowFortReuseTimer = Math.max(bird.penguinSnowFortReuseTimer, 18);
        pinToAbsoluteZeroThrone(bird);
        bird.game.shakeIntensity = Math.max(bird.game.shakeIntensity, 22);
        bird.game.triggerFlash(0.58, false);
        bird.game.addToKillFeed(bird.shortName() + " RAISED THE ABSOLUTE ZERO FORTRESS!");
        emitIceBurst(bird, throneX, throneY - 92.0 * s, bird.facingDirection(), 82, Color.web("#E1F5FE"));
    }

    private static void handleAbsoluteZeroFortress(Bird bird) {
        if (bird.penguinAbsoluteZeroTimer <= 0) {
            bird.penguinAbsoluteZeroWaveIndex = 0;
            return;
        }

        pinToAbsoluteZeroThrone(bird);
        int elapsed = absoluteZeroElapsed(bird);
        while (bird.penguinAbsoluteZeroWaveIndex < Bird.PENGUIN_ABSOLUTE_ZERO_WAVES
                && elapsed >= absoluteZeroImpactFrame(bird.penguinAbsoluteZeroWaveIndex)) {
            applyAbsoluteZeroImpact(bird, bird.penguinAbsoluteZeroWaveIndex);
            bird.penguinAbsoluteZeroWaveIndex++;
        }

        if ((bird.penguinAbsoluteZeroTimer & 3) == 0) {
            double angle = -Math.PI / 2.0 + (bird.game.nextParticleRandom() - 0.5) * 0.72;
            double radius = (46.0 + bird.game.nextParticleRandom() * 92.0) * bird.sizeMultiplier;
            bird.game.particles.add(new Particle(
                    bird.penguinAbsoluteZeroThroneX + Math.cos(angle) * radius * 0.45,
                    bird.penguinAbsoluteZeroThroneY - 54.0 * bird.sizeMultiplier + Math.sin(angle) * radius * 0.35,
                    Math.cos(angle) * (0.6 + bird.game.nextParticleRandom() * 1.6),
                    Math.sin(angle) * (2.2 + bird.game.nextParticleRandom() * 3.8) - 1.2,
                    Color.web("#B3E5FC").deriveColor(0, 1, 1, 0.66 + bird.game.nextParticleRandom() * 0.18)
            ));
        }

        bird.penguinAbsoluteZeroTimer--;
        if (bird.penguinAbsoluteZeroTimer <= 0) {
            bird.penguinAbsoluteZeroTimer = 0;
            bird.penguinAbsoluteZeroWaveIndex = 0;
            bird.vy = Math.min(bird.vy, -4.2);
            bird.penguinIceFxTimer = Math.max(bird.penguinIceFxTimer, 28);
        }
    }

    private static void pinToAbsoluteZeroThrone(Bird bird) {
        double s = bird.sizeMultiplier;
        double seatY = bird.penguinAbsoluteZeroThroneY - 132.0 * s;
        double left = bird.game.battlefieldLeftBound() + 18.0 * s;
        double right = bird.game.battlefieldRightBound() - bird.bodyWidth() - 18.0 * s;
        bird.x = clampBetween(bird.penguinAbsoluteZeroThroneX - bird.bodyWidth() * 0.5, left, right);
        bird.y = seatY - bird.bodyHeight();
        bird.vx = 0.0;
        bird.vy = 0.0;
        Bird target = nearestEnemy(bird, bird.penguinAbsoluteZeroThroneX, bird.penguinAbsoluteZeroThroneY);
        if (target != null) {
            bird.facingRight = target.bodyCenterX() >= bird.bodyCenterX();
        }
        bird.canDoubleJump = true;
        bird.attackAnimationTimer = Math.max(bird.attackAnimationTimer, 5);
    }

    private static void applyAbsoluteZeroImpact(Bird bird, int waveIndex) {
        double s = bird.sizeMultiplier;
        double impactX = absoluteZeroImpactX(bird, waveIndex);
        double surfaceY = absoluteZeroImpactSurfaceY(bird, impactX);
        double centerY = surfaceY - 72.0 * s;
        double radius = 154.0 * s;
        double verticalReach = 170.0 * s;
        boolean hitAny = false;
        for (Bird other : bird.game.players) {
            if (!bird.canDamageTarget(other)) continue;
            double dx = other.bodyCenterX() - impactX;
            double dy = other.bodyCenterY() - centerY;
            if (Math.abs(dx) > radius + other.combatHalfWidth()) continue;
            if (Math.abs(dy) > verticalReach + other.combatHalfHeight()) continue;

            double edge = 1.0 - Math.clamp(Math.abs(dx) / Math.max(1.0, radius), 0.0, 1.0);
            int damage = (int) Math.round(12.0 + edge * 5.0);
            int dealt = bird.applyTrackedSpecialDamage(other, damage);
            if (dealt <= 0) continue;
            hitAny = true;
            double launchDir = Math.signum(dx == 0.0 ? other.bodyCenterX() - bird.bodyCenterX() : dx);
            if (launchDir == 0.0) {
                launchDir = bird.facingDirection();
            }
            other.vx += launchDir * (9.0 + edge * 5.4);
            other.vy -= 9.8 + edge * 5.2;
            other.stunTime = Math.max(other.stunTime, 8.0 + edge * 7.0);
        }

        bird.game.shakeIntensity = Math.max(bird.game.shakeIntensity, hitAny ? 16 : 11);
        bird.game.hitstopFrames = Math.max(bird.game.hitstopFrames, hitAny ? 4 : 2);
        emitIceBurst(bird, impactX, surfaceY - 48.0 * s, bird.facingDirection(), hitAny ? 54 : 36,
                hitAny ? Color.web("#E1F5FE") : Color.web("#90CAF9"));
    }

    private static Bird absoluteZeroTarget(Bird bird, int waveIndex) {
        Bird best = null;
        double bestScore = Double.POSITIVE_INFINITY;
        int desiredSlot = Math.floorMod(bird.playerIndex + waveIndex + 1, Math.max(1, bird.game.players.length));
        for (Bird other : bird.game.players) {
            if (!bird.canDamageTarget(other)) continue;
            double dx = other.bodyCenterX() - bird.penguinAbsoluteZeroThroneX;
            double dy = other.bodyCenterY() - bird.penguinAbsoluteZeroThroneY;
            double slotBias = Math.abs(other.playerIndex - desiredSlot) * 34.0;
            double score = Math.abs(dx) + Math.abs(dy) * 0.34 + slotBias;
            if (score < bestScore) {
                bestScore = score;
                best = other;
            }
        }
        return best;
    }

    private static Bird nearestEnemy(Bird bird, double x, double y) {
        Bird best = null;
        double bestDist = Double.POSITIVE_INFINITY;
        for (Bird other : bird.game.players) {
            if (!bird.canDamageTarget(other)) continue;
            double dx = other.bodyCenterX() - x;
            double dy = other.bodyCenterY() - y;
            double dist = dx * dx + dy * dy;
            if (dist < bestDist) {
                bestDist = dist;
                best = other;
            }
        }
        return best;
    }

    private static double clampBetween(double value, double a, double b) {
        double min = Math.min(a, b);
        double max = Math.max(a, b);
        return Math.max(min, Math.min(max, value));
    }

    static void applyDashDamage(Bird bird) {
        if (bird.type != BirdGame3.BirdType.PENGUIN || bird.penguinDashDamageTimer <= 0) return;
        for (Bird other : bird.game.players) {
            if (!bird.canDamageTarget(other)) continue;
            if (other.playerIndex < 0 || other.playerIndex >= bird.penguinDashHit.length) continue;
            if (bird.penguinDashHit[other.playerIndex]) continue;

            double dx = other.bodyCenterX() - bird.bodyCenterX();
            double dy = other.bodyCenterY() - bird.bodyCenterY();
            if (Math.abs(dx) > 90 + other.combatHalfWidth() || Math.abs(dy) > 95 + other.combatHalfHeight()) continue;

            int dmg = 10 + bird.random.nextInt(5);
            double oldHealth = other.health;
            int dealt = (int) bird.applyDamageTo(other, dmg);
            bird.game.damageDealt[bird.playerIndex] += dealt;
            bird.game.recordSpecialImpact(bird.playerIndex, dealt, dealt > 0);
            if (other.health <= 0 && oldHealth > 0) bird.game.eliminations[bird.playerIndex]++;

            other.vx += (dx >= 0 ? 1 : -1) * 11;
            other.vy -= 9;
            bird.penguinDashHit[other.playerIndex] = true;
            bird.game.addToKillFeed(bird.shortName() + " ICE-CHECKED " + other.shortName() + "! -" + dmg + " HP");

            for (int i = 0; i < 14; i++) {
                double ang = bird.game.nextParticleRandom() * Math.PI * 2;
                bird.game.particles.add(new Particle(
                        other.x + 40, other.y + 40,
                        Math.cos(ang) * (4 + bird.game.nextParticleRandom() * 7),
                        Math.sin(ang) * (4 + bird.game.nextParticleRandom() * 7) - 3,
                        Color.web("#B3E5FC")
                ));
            }
        }
    }

    static void handleObjects(Bird bird) {
        if (bird.type != BirdGame3.BirdType.PENGUIN) {
            return;
        }
        updateSnowFort(bird);
        updateIceObjects(bird);
    }

    static void attackSnowForts(Bird attacker, double attackCenterX, double attackCenterY,
                                double range, double verticalRange, int dmg) {
        for (Bird candidate : attacker.game.players) {
            if (candidate == null || candidate == attacker || candidate.type != BirdGame3.BirdType.PENGUIN) continue;
            if (!attacker.canDamageTarget(candidate)) continue;
            damageSnowFort(candidate, attacker, dmg, attackCenterX, attackCenterY, range, verticalRange);
        }
    }

    static double adjustDamageForSnowFort(Bird bird, Bird attacker, double scaledDamage) {
        if (bird.type != BirdGame3.BirdType.PENGUIN || attacker == null || bird.penguinSnowFort == null
                || bird.penguinSnowFort.health <= 0 || scaledDamage <= 0) {
            return scaledDamage;
        }
        Bird.PenguinSnowFort fort = bird.penguinSnowFort;
        double penguinCenterX = bird.bodyCenterX();
        double attackerCenterX = attacker.bodyCenterX();
        boolean fortBetween = (penguinCenterX - fort.x) * (attackerCenterX - fort.x) <= 0.0;
        if (!fortBetween) {
            return scaledDamage;
        }
        double verticalWindow = (fort.ultimate ? 122.0 : 102.0) * bird.sizeMultiplier;
        if (Math.abs(attacker.bodyCenterY() - (fort.y - 42.0 * bird.sizeMultiplier)) > verticalWindow) {
            return scaledDamage;
        }
        double guardedDistance = Math.abs(penguinCenterX - fort.x);
        if (guardedDistance > (fort.ultimate ? 150.0 : 128.0) * bird.sizeMultiplier) {
            return scaledDamage;
        }
        double reduction = fort.ultimate ? 0.32 : 0.22;
        double absorbed = scaledDamage * reduction;
        fort.health = Math.max(0, fort.health - Math.max(1, (int) Math.ceil(absorbed * 1.35)));
        fort.damageFlash = Math.max(fort.damageFlash, 8);
        bird.penguinFortGuardFxTimer = Math.max(bird.penguinFortGuardFxTimer, 12);
        emitIceBurst(bird, fort.x, fort.y - 38.0 * bird.sizeMultiplier,
                attackerCenterX < fort.x ? -1 : 1, 7, fort.ultimate ? Color.GOLD : Color.web("#E1F5FE"));
        return Math.max(0.0, scaledDamage - absorbed);
    }

    private static void handleBellyCharge(Bird bird, boolean specialHeld) {
        int inputDir = bird.horizontalInputDirection();
        if (inputDir != 0) {
            bird.penguinBellyDirection = inputDir;
            bird.facingRight = inputDir > 0;
        }
        boolean directionalVariantRequested = bird.jumpPressed() || bird.blockPressed();
        boolean keepCharging = specialHeld
                && !directionalVariantRequested
                && bird.penguinBellyChargeFrames < Bird.PENGUIN_BELLY_CHARGE_MAX_FRAMES;
        if (!keepCharging) {
            releaseBellySlide(bird);
            return;
        }

        bird.penguinBellyChargeFrames = Math.min(Bird.PENGUIN_BELLY_CHARGE_MAX_FRAMES, bird.penguinBellyChargeFrames + 1);
        bird.attackAnimationTimer = Math.max(bird.attackAnimationTimer, 5);
        bird.vx *= bird.isOnGround() ? 0.50 : 0.78;
        if (!bird.isOnGround()) {
            bird.vy = Math.min(bird.vy, 1.4);
        }
        double ratio = bellyChargeRatio(bird);
        if ((bird.penguinBellyChargeFrames & 3) == 0) {
            double skidDir = bird.penguinBellyDirection == 0 ? bird.facingDirection() : bird.penguinBellyDirection;
            bird.game.particles.add(new Particle(
                    bird.bodyCenterX() - skidDir * (24.0 + ratio * 26.0) * bird.sizeMultiplier,
                    bird.bodyBottomY() - 5.0 * bird.sizeMultiplier,
                    -skidDir * (1.0 + ratio * 2.3 + bird.game.nextParticleRandom() * 1.4),
                    -0.8 - bird.game.nextParticleRandom() * (1.4 + ratio * 1.8),
                    (bird.penguinBellyUltimate ? Color.GOLD : Color.web("#E1F5FE")).deriveColor(0, 1, 1, 0.66 + ratio * 0.16)
            ));
        }
    }

    private static void releaseBellySlide(Bird bird) {
        if (!bird.penguinBellyCharging) {
            return;
        }
        bird.penguinBellyCharging = false;
        bird.penguinBellySlideTimer = bird.penguinBellyUltimate ? Bird.PENGUIN_BELLY_SLIDE_FRAMES + 8 : Bird.PENGUIN_BELLY_SLIDE_FRAMES;
        Arrays.fill(bird.penguinBellyHit, false);
        double ratio = bellyChargeRatio(bird);
        int dir = bird.horizontalInputDirection();
        if (dir == 0) {
            dir = bird.penguinBellyDirection == 0 ? bird.facingDirection() : bird.penguinBellyDirection;
        }
        bird.penguinBellyDirection = dir;
        bird.facingRight = dir > 0;
        double speed = (bird.penguinBellyUltimate ? 7.0 : 5.4) + ratio * (bird.penguinBellyUltimate ? 23.0 : 19.5);
        bird.vx = dir * speed;
        if (bird.isOnGround()) {
            bird.vy = Math.min(bird.vy, -(bird.penguinBellyUltimate ? 9.4 : 7.8) - ratio * (bird.penguinBellyUltimate ? 7.2 : 5.8));
        } else {
            bird.vy = Math.min(bird.vy * 0.35, -(bird.penguinBellyUltimate ? 5.0 : 3.8) - ratio * 3.0);
        }
        bird.attackAnimationTimer = Math.max(bird.attackAnimationTimer, bird.penguinBellySlideTimer);
        bird.penguinIceFxTimer = Math.max(bird.penguinIceFxTimer, bird.penguinBellySlideTimer + 10);
        emitIceBurst(bird, bird.bodyCenterX(), bird.bodyBottomY() - 8.0 * bird.sizeMultiplier, dir,
                18 + (int) Math.round(ratio * 30.0), bird.penguinBellyUltimate ? Color.GOLD : Color.web("#80DEEA"));
    }

    private static void handleBellySlide(Bird bird) {
        int dir = bird.penguinBellyDirection == 0 ? bird.facingDirection() : bird.penguinBellyDirection;
        bird.facingRight = dir > 0;
        double ratio = bellyChargeRatio(bird);
        double desired = dir * ((bird.penguinBellyUltimate ? 7.2 : 5.6) + ratio * (bird.penguinBellyUltimate ? 23.5 : 20.0));
        bird.vx += (desired - bird.vx) * (bird.isOnGround() ? 0.24 : 0.14);
        if (bird.isOnGround()) {
            bird.vy = Math.min(bird.vy, -0.25);
        }
        if ((bird.penguinBellySlideTimer & 2) == 0) {
            bird.game.particles.add(new Particle(
                    bird.bodyCenterX() - dir * 34.0 * bird.sizeMultiplier,
                    bird.bodyBottomY() - 4.0 * bird.sizeMultiplier,
                    -dir * (1.8 + bird.game.nextParticleRandom() * 2.8),
                    -1.0 - bird.game.nextParticleRandom() * 2.4,
                    (bird.penguinBellyUltimate ? Color.GOLD : Color.web("#B3E5FC")).deriveColor(0, 1, 1, 0.70)
            ));
        }
        double centerX = bird.bodyCenterX() + dir * 18.0 * bird.sizeMultiplier;
        double centerY = bird.bodyCenterY() + 12.0 * bird.sizeMultiplier;
        for (Bird other : bird.game.players) {
            if (!bird.canDamageTarget(other)) continue;
            if (other.playerIndex < 0 || other.playerIndex >= bird.penguinBellyHit.length) continue;
            if (bird.penguinBellyHit[other.playerIndex]) continue;

            double forward = (other.bodyCenterX() - centerX) * dir;
            if (forward < -other.combatHalfWidth() * 0.55) continue;
            if (forward > (bird.penguinBellyUltimate ? 112.0 : 94.0) * bird.sizeMultiplier + other.combatHalfWidth()) continue;
            if (Math.abs(other.bodyCenterY() - centerY) > (bird.penguinBellyUltimate ? 70.0 : 58.0) * bird.sizeMultiplier + other.combatHalfHeight()) continue;

            bird.penguinBellyHit[other.playerIndex] = true;
            int dmg = (bird.penguinBellyUltimate ? 9 : 6) + (int) Math.round(ratio * (bird.penguinBellyUltimate ? 6.0 : 4.5));
            double oldHealth = other.health;
            int dealt = (int) bird.applyDamageTo(other, dmg);
            if (dealt <= 0) continue;

            bird.game.damageDealt[bird.playerIndex] += dealt;
            bird.game.recordSpecialImpact(bird.playerIndex, dealt, true);
            if (other.health <= 0 && oldHealth > 0) {
                bird.game.eliminations[bird.playerIndex]++;
            }
            other.vx += dir * ((bird.penguinBellyUltimate ? 12.8 : 10.0) + ratio * 7.0);
            other.vy -= (bird.penguinBellyUltimate ? 7.0 : 5.3) + ratio * 3.8;
            bird.game.hitstopFrames = Math.max(bird.game.hitstopFrames, bird.penguinBellyUltimate ? 4 : 2);
            bird.game.shakeIntensity = Math.max(bird.game.shakeIntensity, bird.penguinBellyUltimate ? 8 : 5);
            emitIceBurst(bird, other.bodyCenterX(), other.bodyCenterY(), dir, bird.penguinBellyUltimate ? 22 : 14,
                    bird.penguinBellyUltimate ? Color.GOLD : Color.web("#90CAF9"));
        }
    }

    private static void startFlopFromRocket(Bird bird) {
        bird.penguinRocketTimer = 0;
        bird.penguinFlopTimer = flopTotalFrames(bird);
        double entryFallSpeed = bird.penguinRocketUltimate ? 1.35 : 1.05;
        bird.vy = Math.max(bird.vy * 0.28 + entryFallSpeed, entryFallSpeed);
        bird.vx *= 0.96;
        bird.attackAnimationTimer = Math.max(bird.attackAnimationTimer, bird.penguinFlopTimer);
    }

    private static void handleRocket(Bird bird, boolean specialHeld) {
        int inputDir = bird.horizontalInputDirection();
        if (inputDir != 0) {
            bird.facingRight = inputDir > 0;
        }
        double progress = rocketProgress(bird);
        double eased = ease01(progress);
        double thrustLeft = 1.0 - eased;
        double targetVy = (bird.penguinRocketUltimate ? -19.4 : -16.8)
                + eased * (bird.penguinRocketUltimate ? 8.9 : 7.5);
        double liftBlend = 0.17 + thrustLeft * 0.16;
        bird.vy += (targetVy - bird.vy) * liftBlend;
        if (bird.vy > targetVy) {
            bird.vy -= (bird.penguinRocketUltimate ? 0.50 : 0.40) * (0.7 + thrustLeft * 0.8);
        }
        bird.vy = Math.max(bird.vy, bird.penguinRocketUltimate ? -20.8 : -17.8);

        double steerSpeed = (bird.penguinRocketUltimate ? 8.4 : 6.9) * (0.55 + eased * 0.45);
        if (inputDir != 0) {
            bird.vx += (inputDir * steerSpeed - bird.vx) * (0.16 + eased * 0.09);
        } else {
            bird.vx *= 0.965;
        }

        if (!bird.isOnGround() && specialHeld && progress >= 0.82) {
            startFlopFromRocket(bird);
            return;
        }
        if ((bird.penguinRocketTimer & 1) == 0) {
            Color exhaust = bird.penguinRocketUltimate ? Color.GOLD : Color.web("#E1F5FE");
            double drift = Math.clamp(bird.vx / 10.0, -1.0, 1.0);
            for (int side = -1; side <= 1; side += 2) {
                bird.game.particles.add(new Particle(
                        bird.bodyCenterX() + side * 20.0 * bird.sizeMultiplier - drift * 14.0 * bird.sizeMultiplier,
                        bird.bodyBottomY() - 5.0 * bird.sizeMultiplier,
                        side * (0.6 + bird.game.nextParticleRandom() * 1.2) - drift * (1.7 + bird.game.nextParticleRandom() * 1.1),
                        4.5 + bird.game.nextParticleRandom() * 4.8,
                        exhaust.deriveColor(0, 1, 1, 0.60 + thrustLeft * 0.14)
                ));
            }
        }
        double centerX = bird.bodyCenterX();
        double centerY = bird.bodyCenterY() - 28.0 * bird.sizeMultiplier;
        for (Bird other : bird.game.players) {
            if (!bird.canDamageTarget(other)) continue;
            if (other.playerIndex < 0 || other.playerIndex >= bird.penguinRocketHit.length) continue;
            if (bird.penguinRocketHit[other.playerIndex]) continue;

            double dx = other.bodyCenterX() - centerX;
            double dy = other.bodyCenterY() - centerY;
            if (Math.abs(dx) > (bird.penguinRocketUltimate ? 78.0 : 62.0) * bird.sizeMultiplier + other.combatHalfWidth()) continue;
            if (dy < -other.combatHalfHeight() || dy > (bird.penguinRocketUltimate ? 112.0 : 92.0) * bird.sizeMultiplier + other.combatHalfHeight()) continue;

            bird.penguinRocketHit[other.playerIndex] = true;
            double oldHealth = other.health;
            int dealt = (int) bird.applyDamageTo(other, bird.penguinRocketUltimate ? 8 : 5);
            if (dealt <= 0) continue;

            bird.game.damageDealt[bird.playerIndex] += dealt;
            bird.game.recordSpecialImpact(bird.playerIndex, dealt, true);
            if (other.health <= 0 && oldHealth > 0) {
                bird.game.eliminations[bird.playerIndex]++;
            }
            double launchDir = Math.signum(dx == 0.0 ? bird.facingDirection() : dx);
            other.vx += launchDir * (bird.penguinRocketUltimate ? 6.5 : 4.2);
            other.vy -= bird.penguinRocketUltimate ? 11.0 : 8.5;
            emitIceBurst(bird, other.bodyCenterX(), other.bodyCenterY(), (int) launchDir,
                    bird.penguinRocketUltimate ? 18 : 12, bird.penguinRocketUltimate ? Color.GOLD : Color.web("#B3E5FC"));
        }
    }

    private static void handleFlop(Bird bird) {
        int inputDir = bird.horizontalInputDirection();
        if (inputDir != 0) {
            bird.facingRight = inputDir > 0;
        }
        double progress = flopProgress(bird);
        double eased = ease01(progress);
        double steerSpeed = (bird.penguinRocketUltimate ? 6.2 : 5.0) * (1.0 - eased * 0.22);
        if (inputDir != 0) {
            bird.vx += (inputDir * steerSpeed - bird.vx) * (0.10 + eased * 0.08);
        } else {
            bird.vx *= 0.935;
        }
        if (bird.isOnGround()) {
            triggerIcyGroundBlast(bird);
            return;
        }
        double fallCap = bird.penguinRocketUltimate ? 9.8 : 7.8;
        double fallAccel = (bird.penguinRocketUltimate ? 0.24 : 0.19) + eased * (bird.penguinRocketUltimate ? 0.34 : 0.28);
        double fallFloor = (bird.penguinRocketUltimate ? 1.25 : 0.95) + eased * (bird.penguinRocketUltimate ? 2.30 : 1.75);
        bird.vy = Math.clamp(bird.vy + fallAccel, fallFloor, fallCap);
        if ((bird.penguinFlopTimer & 2) == 0) {
            bird.game.particles.add(new Particle(
                    bird.bodyCenterX() + (bird.game.nextParticleRandom() - 0.5) * 36.0 * bird.sizeMultiplier,
                    bird.bodyCenterY() - 12.0 * bird.sizeMultiplier,
                    (bird.game.nextParticleRandom() - 0.5) * 1.7 - Math.signum(bird.vx) * 0.35,
                    -2.4 - bird.game.nextParticleRandom() * 2.8,
                    (bird.penguinRocketUltimate ? Color.GOLD : Color.web("#B3E5FC")).deriveColor(0, 1, 1, 0.58)
            ));
        }
        double centerX = bird.bodyCenterX();
        double centerY = bird.bodyBottomY() + 14.0 * bird.sizeMultiplier;
        for (Bird other : bird.game.players) {
            if (!bird.canDamageTarget(other)) continue;
            if (other.playerIndex < 0 || other.playerIndex >= bird.penguinFlopHit.length) continue;
            if (bird.penguinFlopHit[other.playerIndex]) continue;

            double dx = other.bodyCenterX() - centerX;
            double dy = other.bodyCenterY() - centerY;
            if (Math.abs(dx) > (bird.penguinRocketUltimate ? 86.0 : 70.0) * bird.sizeMultiplier + other.combatHalfWidth()) continue;
            if (dy < -30.0 * bird.sizeMultiplier - other.combatHalfHeight()
                    || dy > (bird.penguinRocketUltimate ? 74.0 : 58.0) * bird.sizeMultiplier + other.combatHalfHeight()) continue;

            bird.penguinFlopHit[other.playerIndex] = true;
            double oldHealth = other.health;
            int dealt = (int) bird.applyDamageTo(other, bird.penguinRocketUltimate ? 14 : 10);
            if (dealt <= 0) continue;

            bird.game.damageDealt[bird.playerIndex] += dealt;
            bird.game.recordSpecialImpact(bird.playerIndex, dealt, true);
            if (other.health <= 0 && oldHealth > 0) {
                bird.game.eliminations[bird.playerIndex]++;
            }
            double dir = Math.signum(dx == 0.0 ? bird.facingDirection() : dx);
            other.vx += dir * (bird.penguinRocketUltimate ? 7.5 : 5.4);
            other.vy += bird.penguinRocketUltimate ? 11.0 : 8.0;
            bird.game.hitstopFrames = Math.max(bird.game.hitstopFrames, bird.penguinRocketUltimate ? 5 : 3);
            bird.game.shakeIntensity = Math.max(bird.game.shakeIntensity, bird.penguinRocketUltimate ? 12 : 8);
            emitIceBurst(bird, other.bodyCenterX(), other.bodyBottomY(), (int) dir,
                    bird.penguinRocketUltimate ? 30 : 20, bird.penguinRocketUltimate ? Color.GOLD : Color.web("#E1F5FE"));
        }
    }

    private static void triggerIcyGroundBlast(Bird bird) {
        boolean ultimate = bird.penguinRocketUltimate;
        double centerX = bird.bodyCenterX();
        double groundY = bird.bodyBottomY();
        double radius = (ultimate ? 178.0 : 140.0) * bird.sizeMultiplier;
        double verticalReach = (ultimate ? 100.0 : 78.0) * bird.sizeMultiplier;
        for (Bird other : bird.game.players) {
            if (!bird.canDamageTarget(other)) continue;
            double dx = other.bodyCenterX() - centerX;
            double feetGap = Math.abs(other.bodyBottomY() - groundY);
            if (Math.abs(dx) > radius + other.combatHalfWidth()) continue;
            if (feetGap > verticalReach && other.bodyCenterY() < groundY - verticalReach) continue;

            double edgeRatio = 1.0 - Math.clamp(Math.abs(dx) / Math.max(1.0, radius), 0.0, 1.0);
            int dmg = (int) Math.round((ultimate ? 13.0 : 9.5) * (0.62 + edgeRatio * 0.38));
            double oldHealth = other.health;
            int dealt = (int) bird.applyDamageTo(other, dmg);
            if (dealt <= 0) continue;

            bird.game.damageDealt[bird.playerIndex] += dealt;
            bird.game.recordSpecialImpact(bird.playerIndex, dealt, true);
            if (other.health <= 0 && oldHealth > 0) {
                bird.game.eliminations[bird.playerIndex]++;
            }
            double dir = Math.signum(dx == 0.0 ? bird.facingDirection() : dx);
            other.vx += dir * (ultimate ? 13.5 : 10.4) * (0.50 + edgeRatio * 0.50);
            other.vy -= (ultimate ? 13.8 : 10.4) * (0.56 + edgeRatio * 0.44);
        }
        bird.penguinFlopTimer = 0;
        bird.penguinRocketUltimate = false;
        Arrays.fill(bird.penguinFlopHit, false);
        bird.penguinIceFxTimer = Math.max(bird.penguinIceFxTimer, 34);
        bird.game.shakeIntensity = Math.max(bird.game.shakeIntensity, ultimate ? 16 : 11);
        bird.game.hitstopFrames = Math.max(bird.game.hitstopFrames, ultimate ? 6 : 4);
        emitIceBurst(bird, centerX, groundY - 8.0 * bird.sizeMultiplier, bird.facingDirection(),
                ultimate ? 70 : 50, ultimate ? Color.GOLD : Color.web("#B3E5FC"));
    }

    private static void updateSnowFort(Bird bird) {
        if (bird.penguinSnowFort == null) {
            return;
        }
        Bird.PenguinSnowFort fort = bird.penguinSnowFort;
        fort.ageFrames++;
        for (int i = 0; i < fort.hitCooldown.length; i++) {
            fort.hitCooldown[i] = Math.max(0, fort.hitCooldown[i] - 1);
        }
        if (fort.damageFlash > 0) {
            fort.damageFlash--;
        }
        if (fort.health <= 0) {
            emitIceBurst(bird, fort.x, fort.y - 24.0 * bird.sizeMultiplier, fort.direction,
                    fort.ultimate ? 32 : 22, fort.ultimate ? Color.GOLD : Color.web("#E1F5FE"));
            bird.penguinSnowFort = null;
            return;
        }
        double halfWidth = fortHalfWidth(bird, fort);
        double height = fortHeight(bird, fort);
        for (Bird other : bird.game.players) {
            resolveSnowFortCollision(bird, other, fort, halfWidth, height);
        }
    }

    private static void resolveSnowFortCollision(Bird bird, Bird other, Bird.PenguinSnowFort fort, double halfWidth, double height) {
        if (other == null || other == bird || other.health <= 0 || fort == null || fort.health <= 0) {
            return;
        }
        double fortLeft = fort.x - halfWidth;
        double fortRight = fort.x + halfWidth;
        double fortTop = fort.y - height;
        double fortBottom = fort.y;
        double otherLeft = other.x;
        double otherRight = other.x + other.bodyWidth();
        double otherTop = other.y;
        double otherBottom = other.bodyBottomY();
        double overlapX = Math.min(otherRight, fortRight) - Math.max(otherLeft, fortLeft);
        double overlapY = Math.min(otherBottom, fortBottom) - Math.max(otherTop, fortTop);
        if (overlapX <= 0.0 || overlapY <= 0.0) {
            return;
        }

        double previousBottom = otherBottom - other.vy;
        boolean landingOnTop = previousBottom <= fortTop + 8.0
                && otherBottom >= fortTop
                && other.vy >= -1.0
                && overlapY <= Math.max(34.0 * other.sizeMultiplier, overlapX * 0.75);
        if (landingOnTop) {
            other.y = fortTop - other.bodyHeight() - 0.5;
            if (other.vy > 0.0) {
                other.vy = 0.0;
            }
            other.canDoubleJump = true;
            other.refreshAirDodge();
            return;
        }

        if (overlapY < overlapX * 0.55 && otherTop >= fortBottom - 12.0 && other.vy < 0.0) {
            other.y = fortBottom + 0.5;
            other.vy = Math.max(0.0, other.vy);
            return;
        }

        double dx = other.bodyCenterX() - fort.x;
        double pushDir = Math.signum(dx == 0.0 ? -fort.direction : dx);
        double targetCenterX = fort.x + pushDir * (halfWidth + other.combatHalfWidth() + 1.5 * Math.max(bird.sizeMultiplier, other.sizeMultiplier));
        other.x += targetCenterX - other.bodyCenterX();
        if (other.vx * pushDir < 0.0) {
            other.vx = 0.0;
        }
        other.vx += pushDir * 0.55;
    }

    private static void damageSnowFort(Bird owner, Bird attacker, double rawDamage, double attackCenterX, double attackCenterY,
                                       double horizontalReach, double verticalReach) {
        if (attacker == null || attacker == owner || owner.penguinSnowFort == null || owner.penguinSnowFort.health <= 0) {
            return;
        }
        Bird.PenguinSnowFort fort = owner.penguinSnowFort;
        if (attacker.playerIndex >= 0 && attacker.playerIndex < fort.hitCooldown.length && fort.hitCooldown[attacker.playerIndex] > 0) {
            return;
        }
        double halfWidth = fortHalfWidth(owner, fort);
        double height = fortHeight(owner, fort);
        double fortCenterY = fort.y - height * 0.5;
        if (!owner.overlapsAttackArea(fort.x, fortCenterY, halfWidth, height * 0.5,
                attackCenterX, attackCenterY, horizontalReach, verticalReach)) {
            return;
        }
        if (attacker.playerIndex >= 0 && attacker.playerIndex < fort.hitCooldown.length) {
            fort.hitCooldown[attacker.playerIndex] = 8;
        }
        int damage = Math.max(8, (int) Math.round(rawDamage * 0.95));
        fort.health = Math.max(0, fort.health - damage);
        fort.damageFlash = Math.max(fort.damageFlash, 10);
        double dir = Math.signum(fort.x - attackCenterX);
        if (dir == 0.0) {
            dir = -fort.direction;
        }
        emitIceBurst(owner, fort.x + dir * halfWidth * 0.75, fort.y - height * 0.48,
                (int) dir, fort.ultimate ? 16 : 11, fort.ultimate ? Color.GOLD : Color.WHITE);
        owner.game.shakeIntensity = Math.max(owner.game.shakeIntensity, fort.ultimate ? 5 : 3);
    }

    private static void updateIceObjects(Bird bird) {
        if (bird.penguinIceObjects.isEmpty()) {
            return;
        }
        ArrayList<Bird.PenguinIceObject> spawnedObjects = new ArrayList<>();
        Iterator<Bird.PenguinIceObject> it = bird.penguinIceObjects.iterator();
        while (it.hasNext()) {
            Bird.PenguinIceObject object = it.next();
            object.ageFrames++;
            object.lifeFrames--;
            for (int i = 0; i < object.hitCooldown.length; i++) {
                object.hitCooldown[i] = Math.max(0, object.hitCooldown[i] - 1);
            }

            object.vy += object.verticalDrop ? 0.42 : (object.snowball ? 0.30 : 0.24);
            object.x += object.vx;
            object.y += object.vy;
            double radius = (object.snowball ? 58.0 : (object.verticalDrop ? 52.0 : 42.0)) * bird.sizeMultiplier;
            double previousBottom = object.y + radius - object.vy;
            double surfaceY = objectSurfaceYOrVoid(bird, object.x, previousBottom);
            if (Double.isFinite(surfaceY) && object.y + radius >= surfaceY) {
                object.y = surfaceY - radius;
                if (object.verticalDrop) {
                    triggerDroppedIcebergImpact(bird, object, surfaceY);
                    object.shattered = true;
                } else {
                    object.vy = object.snowball ? -Math.abs(object.vx) * 0.08 : 0.0;
                    object.vx *= object.snowball ? 0.994 : 0.982;
                }
            }

            int objectDir = (int) Math.signum(object.vx == 0.0 ? object.direction : object.vx);
            if (!object.shattered && bird.game.hitFrostbiteSnowbankWithIce(object.x, object.y, radius * 0.86, objectDir, object.ultimate)) {
                if (!object.snowball && !object.verticalDrop) {
                    object.shattered = true;
                    spawnedObjects.add(new Bird.PenguinIceObject(
                            object.x + objectDir * 38.0 * bird.sizeMultiplier,
                            object.y,
                            objectDir * (object.ultimate ? 12.4 : 10.2),
                            -2.2,
                            objectDir,
                            object.ultimate,
                            true));
                } else if (object.verticalDrop) {
                    object.shattered = true;
                } else {
                    object.vx *= 0.90;
                    object.vy -= 1.4;
                }
            }

            if (!object.shattered && bird.penguinSnowFort != null && bird.penguinSnowFort.health > 0 && !object.snowball
                    && !object.verticalDrop
                    && Math.abs(object.x - bird.penguinSnowFort.x) < 82.0 * bird.sizeMultiplier
                    && Math.abs(object.y - (bird.penguinSnowFort.y - 56.0 * bird.sizeMultiplier)) < 86.0 * bird.sizeMultiplier) {
                Bird.PenguinSnowFort fort = bird.penguinSnowFort;
                object.shattered = true;
                fort.health = 0;
                spawnedObjects.add(new Bird.PenguinIceObject(fort.x + object.direction * 34.0 * bird.sizeMultiplier,
                        fort.y - 58.0 * bird.sizeMultiplier,
                        object.direction * (object.ultimate ? 11.8 : 9.4),
                        -2.0,
                        object.direction,
                        object.ultimate,
                        true));
                emitIceBurst(bird, fort.x, fort.y - 34.0 * bird.sizeMultiplier, object.direction,
                        object.ultimate ? 42 : 30, object.ultimate ? Color.GOLD : Color.web("#E1F5FE"));
            }

            double worldLeft = bird.game.battlefieldLeftBound() - 70.0;
            double worldRight = bird.game.battlefieldRightBound() + 70.0;
            double fallLimitY = bird.usesIslandBounds()
                    ? bird.game.battlefieldVoidFloorY() + 180.0
                    : BirdGame3.WORLD_HEIGHT + 220.0;
            if (object.lifeFrames <= 0 || object.x < worldLeft || object.x > worldRight
                    || object.y - radius > fallLimitY || (!object.verticalDrop && Math.abs(object.vx) < 0.45)) {
                object.shattered = true;
            }

            if (!object.shattered) {
                handleIceObjectHits(bird, object);
            }
            if (!object.shattered && (object.ageFrames & 3) == 0) {
                bird.game.particles.add(new Particle(
                        object.x - Math.signum(object.vx == 0.0 ? object.direction : object.vx) * radius * 0.7,
                        object.y + radius * 0.65,
                        -Math.signum(object.vx == 0.0 ? object.direction : object.vx) * (0.8 + bird.game.nextParticleRandom() * 1.8),
                        -0.4 - bird.game.nextParticleRandom() * 1.5,
                        (object.ultimate ? Color.GOLD : Color.web("#B3E5FC")).deriveColor(0, 1, 1, 0.58)
                ));
            }

            if (object.shattered) {
                emitIceBurst(bird, object.x, object.y, object.direction,
                        object.snowball ? 24 : 14, object.ultimate ? Color.GOLD : Color.web("#90CAF9"));
                it.remove();
            }
        }
        bird.penguinIceObjects.addAll(spawnedObjects);
        while (bird.penguinIceObjects.size() > 4) {
            bird.penguinIceObjects.removeFirst();
        }
    }

    private static void handleIceObjectHits(Bird bird, Bird.PenguinIceObject object) {
        double radius = (object.snowball ? 62.0 : (object.verticalDrop ? 58.0 : 48.0)) * bird.sizeMultiplier;
        for (Bird other : bird.game.players) {
            if (!bird.canDamageTarget(other)) continue;
            if (other.playerIndex < 0 || other.playerIndex >= object.hitCooldown.length) continue;
            if (object.hitCooldown[other.playerIndex] > 0) continue;

            double dx = other.bodyCenterX() - object.x;
            double dy = other.bodyCenterY() - object.y;
            if (Math.abs(dx) > radius + other.combatHalfWidth()) continue;
            if (Math.abs(dy) > radius + other.combatHalfHeight()) continue;

            object.hitCooldown[other.playerIndex] = object.snowball ? 12 : 28;
            int dmg = object.snowball
                    ? (object.ultimate ? 15 : 10)
                    : object.verticalDrop ? (object.ultimate ? 13 : 9) : (object.ultimate ? 10 : 7);
            double oldHealth = other.health;
            int dealt = (int) bird.applyDamageTo(other, dmg);
            if (dealt <= 0) continue;

            bird.game.damageDealt[bird.playerIndex] += dealt;
            bird.game.recordSpecialImpact(bird.playerIndex, dealt, true);
            if (other.health <= 0 && oldHealth > 0) {
                bird.game.eliminations[bird.playerIndex]++;
            }
            double launchDir = Math.signum(dx == 0.0 ? object.direction : dx);
            other.vx += launchDir * (object.snowball
                    ? (object.ultimate ? 15.5 : 12.0)
                    : object.verticalDrop ? (object.ultimate ? 8.4 : 6.2) : (object.ultimate ? 10.8 : 8.2));
            if (object.verticalDrop) {
                other.vy += object.ultimate ? 7.0 : 5.2;
            } else {
                other.vy -= object.snowball ? (object.ultimate ? 8.5 : 6.2) : (object.ultimate ? 6.0 : 4.5);
            }
            if (!object.snowball) {
                object.shattered = true;
            } else {
                object.vx *= 0.88;
                object.vy -= 0.8;
            }
            bird.game.hitstopFrames = Math.max(bird.game.hitstopFrames, object.snowball ? 4 : 2);
            bird.game.shakeIntensity = Math.max(bird.game.shakeIntensity, object.snowball ? 8 : (object.verticalDrop ? 7 : 5));
        }
    }

    private static void triggerDroppedIcebergImpact(Bird bird, Bird.PenguinIceObject object, double surfaceY) {
        double s = bird.sizeMultiplier;
        double radius = (object.ultimate ? 122.0 : 96.0) * s;
        double verticalReach = (object.ultimate ? 108.0 : 84.0) * s;
        boolean hitAny = false;
        for (Bird other : bird.game.players) {
            if (!bird.canDamageTarget(other)) continue;
            double dx = other.bodyCenterX() - object.x;
            double feetGap = Math.abs(other.bodyBottomY() - surfaceY);
            if (Math.abs(dx) > radius + other.combatHalfWidth()) continue;
            if (feetGap > verticalReach && other.bodyCenterY() < surfaceY - verticalReach) continue;

            double edge = 1.0 - Math.clamp(Math.abs(dx) / Math.max(1.0, radius), 0.0, 1.0);
            int dmg = (int) Math.round((object.ultimate ? 12.0 : 8.5) * (0.62 + edge * 0.38));
            int dealt = bird.applyTrackedSpecialDamage(other, dmg);
            if (dealt <= 0) continue;
            hitAny = true;
            double launchDir = Math.signum(dx == 0.0 ? object.direction : dx);
            other.vx += launchDir * (object.ultimate ? 10.4 : 7.8) * (0.55 + edge * 0.45);
            other.vy -= (object.ultimate ? 9.8 : 7.2) * (0.58 + edge * 0.42);
        }
        bird.game.shakeIntensity = Math.max(bird.game.shakeIntensity, hitAny ? 12 : 8);
        bird.game.hitstopFrames = Math.max(bird.game.hitstopFrames, hitAny ? 4 : 2);
        emitIceBurst(bird, object.x, surfaceY - 22.0 * s, object.direction, hitAny ? 38 : 26,
                object.ultimate ? Color.GOLD : Color.web("#E1F5FE"));
    }

    private static double specialPhase(int timer, int totalFrames) {
        if (timer <= 0 || totalFrames <= 0) {
            return 0.0;
        }
        return Math.clamp(1.0 - ((timer - 1.0) / (double) totalFrames), 0.0, 1.0);
    }

    private static double ease01(double t) {
        double clamped = Math.clamp(t, 0.0, 1.0);
        return clamped * clamped * (3.0 - 2.0 * clamped);
    }
}
