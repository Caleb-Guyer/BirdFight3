package com.example.birdgame3;

import javafx.scene.paint.Color;

import java.util.Arrays;

final class HummingbirdSpecials {
    static final String NEEDLEHEART_OVERDRIVE_MOVE = "Hummingbird Needleheart Overdrive";
    private static final double NEEDLEHEART_LOCK_RANGE = 380.0;

    private HummingbirdSpecials() {
    }

    static void use(Bird bird, boolean ultimate) {
        if (ultimate) {
            needleheartOverdrive(bird);
            return;
        }
        switch (bird.selectHummingbirdSpecialVariant()) {
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
        bird.hummingNeedleDirection = bird.facingDirection();
        bird.hummingNeedleHitTimer = ultimate ? Bird.HUMMING_NEEDLE_ACTIVE_FRAMES + 2 : Bird.HUMMING_NEEDLE_ACTIVE_FRAMES;
        bird.hummingNeedleReuseTimer = ultimate ? 4 : Bird.HUMMING_NEEDLE_REUSE_FRAMES;
        bird.hummingNeedleUltimate = ultimate;
        Arrays.fill(bird.hummingNeedleHit, false);
        bird.attackAnimationTimer = Math.max(bird.attackAnimationTimer, ultimate ? 12 : 9);
        bird.vx *= bird.isOnGround() ? 0.62 : 0.78;
        if (!bird.isOnGround()) {
            bird.vy = Math.min(bird.vy, 1.2);
        }
        bird.specialCooldown = 0;
        bird.specialMaxCooldown = 0;

        double startX = bird.bodyCenterX() + bird.hummingNeedleDirection * 20.0 * bird.sizeMultiplier;
        double startY = bird.bodyCenterY() - 15.0 * bird.sizeMultiplier;
        for (int i = 0; i < bird.scaledParticleCount(ultimate ? 14 : 8); i++) {
            bird.game.particles.add(new Particle(
                    startX + bird.hummingNeedleDirection * bird.game.nextParticleRandom() * 46.0,
                    startY + (bird.game.nextParticleRandom() - 0.5) * 18.0,
                    bird.hummingNeedleDirection * (3.0 + bird.game.nextParticleRandom() * 4.0),
                    (bird.game.nextParticleRandom() - 0.5) * 2.0,
                    (ultimate ? Color.GOLD : Color.LIME).deriveColor(0, 1, 1, 0.82)
            ));
        }
    }

    static void side(Bird bird, boolean ultimate) {
        int dir = bird.horizontalInputDirection();
        if (dir == 0) {
            dir = bird.facingDirection();
        }
        bird.facingRight = dir > 0;
        bird.hummingFlashSipDirection = dir;
        bird.hummingFlashSipUltimate = ultimate;
        bird.hummingFlashSipTimer = ultimate ? Bird.HUMMING_FLASH_SIP_FRAMES + 3 : Bird.HUMMING_FLASH_SIP_FRAMES;
        bird.hummingFlashSipReuseTimer = ultimate ? 72 : Bird.HUMMING_FLASH_SIP_REUSE_FRAMES;
        Arrays.fill(bird.hummingFlashSipHit, false);
        bird.attackAnimationTimer = Math.max(bird.attackAnimationTimer, bird.hummingFlashSipTimer);
        bird.vx = dir * (ultimate ? 42.0 : 36.0);
        bird.vy *= 0.18;
        bird.specialCooldown = 0;
        bird.specialMaxCooldown = 0;
        bird.isBlocking = false;
        bird.parryWindowFrames = 0;
        bird.shieldStunFrames = 0;
        if (ultimate) {
            bird.game.addToKillFeed(bird.shortName() + " VANISHED IN GOLDEN FLASH SIP!");
        }
    }

    static void up(Bird bird, boolean ultimate) {
        if (bird.hummingHoverBurstUsed && !ultimate) {
            return;
        }
        bird.hummingHoverBurstUsed = true;
        bird.hummingHoverBurstUltimate = ultimate;
        bird.hummingHoverBurstTimer = ultimate ? 32 : 22;
        bird.hummingHoverBurstReuseTimer = ultimate ? 90 : Bird.HUMMING_HOVER_BURST_REUSE_FRAMES;
        bird.canDoubleJump = true;
        bird.vy = Math.min(bird.vy, ultimate ? -36.0 : -29.0);
        bird.vx *= 0.16;
        bird.attackAnimationTimer = Math.max(bird.attackAnimationTimer, 12);
        bird.specialCooldown = 0;
        bird.specialMaxCooldown = 0;
        if (ultimate) {
            bird.game.addToKillFeed(bird.shortName() + " ULT HOVER BURST!");
        }
        for (int i = 0; i < bird.scaledParticleCount(ultimate ? 44 : 30); i++) {
            double spread = (bird.game.nextParticleRandom() - 0.5) * 58.0;
            bird.game.particles.add(new Particle(
                    bird.bodyCenterX() + spread,
                    bird.bodyBottomY() - 8.0 * bird.sizeMultiplier,
                    spread * 0.035,
                    5.0 + bird.game.nextParticleRandom() * 7.0,
                    (ultimate ? Color.GOLD : Color.AQUA).deriveColor(0, 1, 1, 0.76)
            ));
        }
    }

    static void down(Bird bird, boolean ultimate) {
        int dir = bird.facingDirection();
        double trapX = bird.bodyCenterX() - dir * 58.0 * bird.sizeMultiplier;
        double targetY = trapSurfaceY(bird, trapX);
        double startY = Math.min(bird.bodyCenterY() - 16.0 * bird.sizeMultiplier, targetY - 70.0 * bird.sizeMultiplier);
        bird.hummingNectarTraps.add(new Bird.HummingbirdNectarTrap(trapX, startY, targetY, ultimate));
        while (bird.hummingNectarTraps.size() > (ultimate ? 5 : 4)) {
            bird.hummingNectarTraps.removeFirst();
        }
        bird.hummingNectarTrapReuseTimer = ultimate ? 132 : Bird.HUMMING_NECTAR_TRAP_REUSE_FRAMES;
        bird.specialCooldown = 0;
        bird.specialMaxCooldown = 0;
        bird.attackAnimationTimer = Math.max(bird.attackAnimationTimer, 10);
        bird.vx += dir * 3.4;
        if (ultimate) {
            bird.game.addToKillFeed(bird.shortName() + " planted a royal Nectar Trap!");
        }
        for (int i = 0; i < bird.scaledParticleCount(ultimate ? 28 : 18); i++) {
            double angle = bird.game.nextParticleRandom() * Math.PI * 2.0;
            bird.game.particles.add(new Particle(
                    trapX,
                    startY - 12.0,
                    Math.cos(angle) * (2.0 + bird.game.nextParticleRandom() * 4.0),
                    Math.sin(angle) * (2.0 + bird.game.nextParticleRandom() * 3.0) - 2.0,
                    (ultimate ? Color.GOLD : Color.HOTPINK).deriveColor(0, 1, 1, 0.78)
            ));
        }
    }

    static void handleState(Bird bird) {
        if (bird.type != BirdGame3.BirdType.HUMMINGBIRD && !bird.mockingbirdCopiedNeutralFrom(BirdGame3.BirdType.HUMMINGBIRD)) {
            return;
        }
        if (bird.stunTime > 0.0) {
            reset(bird, false);
            if (bird.mockingbirdCopiedNeutralFrom(BirdGame3.BirdType.HUMMINGBIRD)) {
                bird.mockingbirdCopiedNeutralSource = null;
            }
            return;
        }
        if (bird.hummingFrenzyTimer > 0) {
            handleNeedleheartOverdrive(bird);
            return;
        }
        if (bird.hummingNeedleHitTimer > 0) {
            handleNeedleBarrage(bird);
        }
        if (bird.hummingFlashSipTimer > 0) {
            handleFlashSip(bird);
        }
        if (bird.hummingHoverBurstTimer > 0) {
            handleHoverBurst(bird);
        }
    }

    static boolean active(Bird bird) {
        return bird.hummingFrenzyTimer > 0
                || bird.hummingNeedleHitTimer > 0
                || bird.hummingFlashSipTimer > 0
                || bird.hummingHoverBurstTimer > 0;
    }

    static boolean ready(Bird bird, Bird.HummingbirdSpecialVariant variant) {
        boolean ultimateReady = bird.isUltimateReady();
        return switch (variant) {
            case NEUTRAL -> ultimateReady || bird.hummingNeedleReuseTimer <= 0;
            case SIDE -> ultimateReady || bird.hummingFlashSipReuseTimer <= 0;
            case UP -> ultimateReady || (!bird.hummingHoverBurstUsed && bird.hummingHoverBurstReuseTimer <= 0);
            case DOWN -> ultimateReady || bird.hummingNectarTrapReuseTimer <= 0;
        };
    }

    static boolean canConvertShieldIntoDown(Bird bird) {
        return bird.selectHummingbirdSpecialVariant() == Bird.HummingbirdSpecialVariant.DOWN
                && bird.isBlocking
                && bird.shieldStunFrames <= 0;
    }

    static void interruptOnHit(Bird bird) {
        if (bird.type != BirdGame3.BirdType.HUMMINGBIRD) {
            return;
        }
        if (active(bird)) {
            bird.attackAnimationTimer = 0;
        }
        reset(bird, false);
    }

    static void reset(Bird bird, boolean clearTraps) {
        bird.hummingFrenzyTimer = 0;
        bird.hummingFrenzyTargetIndex = -1;
        bird.hummingFrenzyStrikeIndex = 0;
        bird.hummingFrenzyConnectedStrikes = 0;
        bird.hummingFrenzyFinalResolved = false;
        bird.hummingFrenzyAnchorX = 0.0;
        bird.hummingFrenzyAnchorY = 0.0;
        bird.hummingFrenzyTargetX = 0.0;
        bird.hummingFrenzyTargetY = 0.0;
        bird.hummingFrenzyLastStartX = 0.0;
        bird.hummingFrenzyLastStartY = 0.0;
        bird.hummingFrenzyLastEndX = 0.0;
        bird.hummingFrenzyLastEndY = 0.0;
        Arrays.fill(bird.hummingFrenzyHitCooldown, 0);
        bird.hummingNeedleHitTimer = 0;
        bird.hummingNeedleUltimate = false;
        Arrays.fill(bird.hummingNeedleHit, false);
        bird.hummingFlashSipTimer = 0;
        bird.hummingFlashSipUltimate = false;
        bird.hummingFlashSipDirection = bird.facingDirection();
        Arrays.fill(bird.hummingFlashSipHit, false);
        bird.hummingHoverBurstTimer = 0;
        bird.hummingHoverBurstUltimate = false;
        if (clearTraps) {
            bird.hummingNectarTraps.clear();
        }
    }

    static double trapSurfaceY(Bird bird, double trapX) {
        double bestY = bird.hasSolidGroundFloorUnderBody() ? BirdGame3.GROUND_Y : Double.POSITIVE_INFINITY;
        double sourceY = bird.bodyBottomY() - 24.0 * bird.sizeMultiplier;
        for (Platform p : bird.game.platforms) {
            boolean isCaveCeiling = bird.game.selectedMap == BirdGame3.MapType.CAVE
                    && p.y <= 1 && p.h >= 60 && p.w >= BirdGame3.WORLD_WIDTH - 10;
            if (isCaveCeiling) continue;
            if (trapX < p.x - 18.0 || trapX > p.x + p.w + 18.0) continue;
            if (p.y < sourceY - 12.0) continue;
            if (p.y < bestY) {
                bestY = p.y;
            }
        }
        if (Double.isFinite(bestY)) {
            return bestY;
        }
        return bird.bodyBottomY() + 8.0 * bird.sizeMultiplier;
    }

    private static void handleNeedleBarrage(Bird bird) {
        int dir = bird.hummingNeedleDirection == 0 ? bird.facingDirection() : bird.hummingNeedleDirection;
        double originX = bird.bodyCenterX() + dir * 16.0 * bird.sizeMultiplier;
        double originY = bird.bodyCenterY() - 16.0 * bird.sizeMultiplier;
        double reach = (bird.hummingNeedleUltimate ? 118.0 : 98.0) * bird.sizeMultiplier;
        double verticalReach = (bird.hummingNeedleUltimate ? 34.0 : 27.0) * bird.sizeMultiplier;

        for (Bird other : bird.game.players) {
            if (!bird.canDamageTarget(other)) continue;
            if (other.playerIndex < 0 || other.playerIndex >= bird.hummingNeedleHit.length) continue;
            if (bird.hummingNeedleHit[other.playerIndex]) continue;

            double forward = (other.bodyCenterX() - originX) * dir;
            if (forward < -other.combatHalfWidth() * 0.22) continue;
            if (forward > reach + other.combatHalfWidth()) continue;
            if (Math.abs(other.bodyCenterY() - originY) > verticalReach + other.combatHalfHeight()) continue;

            int nextCount = bird.hummingNeedleComboTimer > 0 ? bird.hummingNeedleComboCount + 1 : 1;
            boolean finisher = nextCount >= 3;
            int dmg = finisher ? (bird.hummingNeedleUltimate ? 10 : 8) : (bird.hummingNeedleUltimate ? 5 : 4);
            double oldHealth = other.health;
            int dealt = (int) bird.applyDamageTo(other, dmg);
            if (dealt <= 0) continue;

            bird.game.damageDealt[bird.playerIndex] += dealt;
            bird.game.recordSpecialImpact(bird.playerIndex, dealt, true);
            if (other.health <= 0 && oldHealth > 0) {
                bird.game.eliminations[bird.playerIndex]++;
            }

            bird.hummingNeedleComboCount = finisher ? 0 : nextCount;
            bird.hummingNeedleComboTimer = finisher ? 0 : Bird.HUMMING_NEEDLE_COMBO_WINDOW_FRAMES;
            bird.hummingNeedleHit[other.playerIndex] = true;
            other.vx += dir * (finisher ? (bird.hummingNeedleUltimate ? 21.0 : 17.0) : 5.2);
            other.vy -= finisher ? (bird.hummingNeedleUltimate ? 11.5 : 9.2) : 2.8;
            if (finisher) {
                other.applyStun(bird.hummingNeedleUltimate ? 12 : 8);
                bird.game.hitstopFrames = Math.max(bird.game.hitstopFrames, bird.hummingNeedleUltimate ? 5 : 3);
                bird.game.shakeIntensity = Math.max(bird.game.shakeIntensity, bird.hummingNeedleUltimate ? 7 : 4);
            }

            Color spark = finisher ? Color.web("#FFF176") : Color.web("#B2FF59");
            for (int i = 0; i < bird.scaledParticleCount(finisher ? 18 : 9); i++) {
                double angle = bird.game.nextParticleRandom() * Math.PI * 2.0;
                bird.game.particles.add(new Particle(
                        other.bodyCenterX(),
                        other.bodyCenterY() - 6.0 * bird.sizeMultiplier,
                        Math.cos(angle) * (2.5 + bird.game.nextParticleRandom() * (finisher ? 7.0 : 4.0)),
                        Math.sin(angle) * (2.5 + bird.game.nextParticleRandom() * (finisher ? 7.0 : 4.0)) - 1.5,
                        spark.deriveColor(0, 1, 1, 0.86)
                ));
            }
        }
    }

    private static void handleFlashSip(Bird bird) {
        int dir = bird.hummingFlashSipDirection == 0 ? bird.facingDirection() : bird.hummingFlashSipDirection;
        bird.facingRight = dir > 0;
        bird.vx = dir * (bird.hummingFlashSipUltimate ? 42.0 : 36.0);
        bird.vy *= 0.48;

        double centerX = bird.bodyCenterX();
        double centerY = bird.bodyCenterY();
        double reach = (bird.hummingFlashSipUltimate ? 330.0 : 270.0) * bird.sizeMultiplier;
        double height = (bird.hummingFlashSipUltimate ? 72.0 : 58.0) * bird.sizeMultiplier;

        for (int i = 0; i < bird.scaledParticleCount(3); i++) {
            bird.game.particles.add(new Particle(
                    centerX - dir * (12.0 + bird.game.nextParticleRandom() * 72.0),
                    centerY + (bird.game.nextParticleRandom() - 0.5) * 34.0,
                    -dir * (2.0 + bird.game.nextParticleRandom() * 3.0),
                    (bird.game.nextParticleRandom() - 0.5) * 2.0,
                    (bird.hummingFlashSipUltimate ? Color.GOLD : Color.CYAN).deriveColor(0, 1, 1, 0.58)
            ));
        }

        for (Bird other : bird.game.players) {
            if (!bird.canDamageTarget(other)) continue;
            if (other.playerIndex < 0 || other.playerIndex >= bird.hummingFlashSipHit.length) continue;
            if (bird.hummingFlashSipHit[other.playerIndex]) continue;

            double forward = (other.bodyCenterX() - centerX) * dir;
            if (forward < -other.combatHalfWidth() * 0.45) continue;
            if (forward > reach + other.combatHalfWidth()) continue;
            if (Math.abs(other.bodyCenterY() - centerY) > height + other.combatHalfHeight()) continue;

            double oldHealth = other.health;
            int dealt = (int) bird.applyDamageTo(other, bird.hummingFlashSipUltimate ? 12 : 9);
            if (dealt <= 0) continue;

            bird.game.damageDealt[bird.playerIndex] += dealt;
            bird.game.recordSpecialImpact(bird.playerIndex, dealt, true);
            if (other.health <= 0 && oldHealth > 0) {
                bird.game.eliminations[bird.playerIndex]++;
            }

            double exitCenterX = other.bodyCenterX() + dir * (other.combatHalfWidth() + bird.combatHalfWidth() + 18.0);
            bird.x = exitCenterX - bird.bodyWidth() / 2.0;
            other.vx += dir * (bird.hummingFlashSipUltimate ? 14.5 : 11.5);
            other.vy -= bird.hummingFlashSipUltimate ? 8.4 : 6.5;
            bird.hummingFlashSipHit[other.playerIndex] = true;
            bird.game.hitstopFrames = Math.max(bird.game.hitstopFrames, bird.hummingFlashSipUltimate ? 4 : 2);

            for (int i = 0; i < bird.scaledParticleCount(bird.hummingFlashSipUltimate ? 22 : 15); i++) {
                double angle = bird.game.nextParticleRandom() * Math.PI * 2.0;
                bird.game.particles.add(new Particle(
                        other.bodyCenterX(),
                        other.bodyCenterY(),
                        Math.cos(angle) * (3.0 + bird.game.nextParticleRandom() * 6.0),
                        Math.sin(angle) * (3.0 + bird.game.nextParticleRandom() * 6.0) - 2.0,
                        (bird.hummingFlashSipUltimate ? Color.GOLD : Color.DEEPSKYBLUE).deriveColor(0, 1, 1, 0.82)
                ));
            }
        }
    }

    private static void handleHoverBurst(Bird bird) {
        bird.vx *= 0.82;
        if (bird.vy > -6.0) {
            bird.vy -= bird.hummingHoverBurstUltimate ? 1.3 : 0.9;
        }
        if ((bird.hummingHoverBurstTimer & 1) == 0) {
            for (int i = 0; i < bird.scaledParticleCount(3); i++) {
                double spread = (bird.game.nextParticleRandom() - 0.5) * 48.0;
                bird.game.particles.add(new Particle(
                        bird.bodyCenterX() + spread,
                        bird.bodyBottomY() - 2.0 * bird.sizeMultiplier,
                        spread * 0.035,
                        7.0 + bird.game.nextParticleRandom() * 7.0,
                        (bird.hummingHoverBurstUltimate ? Color.GOLD : Color.AQUA).deriveColor(0, 1, 1, 0.62)
                ));
            }
        }
    }

    private static void needleheartOverdrive(Bird bird) {
        reset(bird, false);
        Bird target = nearestTarget(bird);
        double targetX = target == null
                ? bird.bodyCenterX() + bird.facingDirection() * 260.0 * bird.sizeMultiplier
                : target.bodyCenterX();
        double targetY = target == null
                ? bird.bodyCenterY() - 10.0 * bird.sizeMultiplier
                : target.bodyCenterY();

        bird.hummingFrenzyTimer = Bird.HUMMING_NEEDLEHEART_TOTAL_FRAMES;
        bird.hummingFrenzyTargetIndex = target == null ? -1 : target.playerIndex;
        bird.hummingFrenzyStrikeIndex = 0;
        bird.hummingFrenzyConnectedStrikes = 0;
        bird.hummingFrenzyFinalResolved = false;
        bird.hummingFrenzyAnchorX = bird.bodyCenterX();
        bird.hummingFrenzyAnchorY = bird.bodyCenterY();
        bird.hummingFrenzyTargetX = clampTargetX(targetX);
        bird.hummingFrenzyTargetY = clampTargetY(targetY);
        setNeedleheartLine(bird, 0, false);
        Arrays.fill(bird.hummingFrenzyHitCooldown, 0);
        bird.specialCooldown = 0;
        bird.specialMaxCooldown = 0;
        bird.attackAnimationTimer = Math.max(bird.attackAnimationTimer, Bird.HUMMING_NEEDLEHEART_TOTAL_FRAMES);
        bird.vx *= 0.06;
        bird.vy = Math.min(bird.vy * 0.15, -2.2);
        bird.isBlocking = false;
        bird.parryWindowFrames = 0;
        if (target != null) {
            bird.facingRight = target.bodyCenterX() >= bird.bodyCenterX();
        }

        for (int i = 0; i < bird.scaledParticleCount(58); i++) {
            double angle = bird.game.nextParticleRandom() * Math.PI * 2.0;
            double radius = 18.0 + bird.game.nextParticleRandom() * 72.0;
            double speed = 2.0 + bird.game.nextParticleRandom() * 7.0;
            bird.game.particles.add(new Particle(
                    bird.bodyCenterX() + Math.cos(angle) * radius,
                    bird.bodyCenterY() + Math.sin(angle) * radius * 0.52,
                    Math.cos(angle) * speed,
                    Math.sin(angle) * speed - 1.8,
                    (i % 3 == 0 ? Color.web("#FF5FD2") : i % 3 == 1 ? Color.web("#B2FF59") : Color.web("#E8FFFF"))
                            .deriveColor(0, 1, 1, 0.84)
            ));
        }
        bird.game.addToKillFeed(bird.shortName() + " ENTERS NEEDLEHEART OVERDRIVE!");
    }

    private static void handleNeedleheartOverdrive(Bird bird) {
        int elapsed = Bird.HUMMING_NEEDLEHEART_TOTAL_FRAMES - bird.hummingFrenzyTimer;
        Bird target = markedNeedleheartTarget(bird);
        if (target != null) {
            bird.facingRight = target.bodyCenterX() >= bird.bodyCenterX();
        }

        bird.attackAnimationTimer = Math.max(bird.attackAnimationTimer, 5);
        bird.isBlocking = false;
        bird.parryWindowFrames = 0;

        if (elapsed < Bird.HUMMING_NEEDLEHEART_WINDUP_FRAMES) {
            double pulse = Math.sin(elapsed * 0.54);
            moveToCenter(bird,
                    bird.hummingFrenzyAnchorX + pulse * 9.0 * bird.sizeMultiplier,
                    bird.hummingFrenzyAnchorY - (18.0 + Math.abs(pulse) * 10.0) * bird.sizeMultiplier);
            bird.vx = 0.0;
            bird.vy = 0.0;
            if (elapsed % 3 == 0) {
                emitNeedleheartOrbitParticles(bird, 6, Color.web("#B2FF59", 0.84));
            }
            return;
        }

        while (bird.hummingFrenzyStrikeIndex < Bird.HUMMING_NEEDLEHEART_STRIKE_COUNT
                && elapsed >= Bird.HUMMING_NEEDLEHEART_FIRST_STRIKE_FRAME
                + bird.hummingFrenzyStrikeIndex * Bird.HUMMING_NEEDLEHEART_STRIKE_SPACING) {
            performNeedleheartStrike(bird, bird.hummingFrenzyStrikeIndex);
            bird.hummingFrenzyStrikeIndex++;
        }

        if (!bird.hummingFrenzyFinalResolved && elapsed >= Bird.HUMMING_NEEDLEHEART_FINAL_FRAME) {
            performNeedleheartFinal(bird);
        }

        if (!bird.hummingFrenzyFinalResolved) {
            double routeT = Math.clamp((elapsed - Bird.HUMMING_NEEDLEHEART_WINDUP_FRAMES)
                    / (double) Math.max(1, Bird.HUMMING_NEEDLEHEART_FINAL_FRAME - Bird.HUMMING_NEEDLEHEART_WINDUP_FRAMES),
                    0.0, 1.0);
            double angle = routeT * Math.PI * 2.0 * 2.25;
            double radius = (92.0 - routeT * 22.0) * bird.sizeMultiplier;
            moveToCenter(bird,
                    bird.hummingFrenzyTargetX + Math.cos(angle) * radius,
                    bird.hummingFrenzyTargetY + Math.sin(angle) * radius * 0.58 - 18.0 * bird.sizeMultiplier);
        } else {
            double dir = bird.facingDirection();
            moveToCenter(bird,
                    bird.hummingFrenzyTargetX + dir * 78.0 * bird.sizeMultiplier,
                    bird.hummingFrenzyTargetY - 46.0 * bird.sizeMultiplier);
        }
        bird.vx = 0.0;
        bird.vy = 0.0;

        if (elapsed % 4 == 0) {
            emitNeedleheartOrbitParticles(bird, 5,
                    bird.hummingFrenzyFinalResolved ? Color.web("#FFF176", 0.82) : Color.web("#FF5FD2", 0.82));
        }
    }

    private static void performNeedleheartStrike(Bird bird, int strikeIndex) {
        setNeedleheartLine(bird, strikeIndex, false);
        double sx = bird.hummingFrenzyLastStartX;
        double sy = bird.hummingFrenzyLastStartY;
        double ex = bird.hummingFrenzyLastEndX;
        double ey = bird.hummingFrenzyLastEndY;
        boolean hitAny = false;

        for (Bird other : bird.game.players) {
            if (!bird.canDamageTarget(other)) continue;
            int index = other.playerIndex;
            if (index < 0 || index >= bird.hummingFrenzyHitCooldown.length) continue;
            if (bird.hummingFrenzyHitCooldown[index] > 0) continue;
            double distance = pointToSegmentDistance(other.bodyCenterX(), other.bodyCenterY(), sx, sy, ex, ey);
            if (distance > 34.0 * bird.sizeMultiplier + other.combatRadius()) continue;

            boolean primary = index == bird.hummingFrenzyTargetIndex;
            int damage = primary ? 2 : 1;
            int dealt = applyNeedleheartHit(bird, other, damage,
                    Math.signum(ex - sx) * (primary ? 2.8 : 1.4),
                    primary ? -2.4 : -1.2,
                    primary);
            if (dealt > 0) {
                bird.hummingFrenzyHitCooldown[index] = 7;
                hitAny = true;
                if (primary) {
                    bird.hummingFrenzyConnectedStrikes++;
                    if (bird.hummingFrenzyConnectedStrikes >= 2) {
                        other.applyHummingbirdNectarCoating(bird, true);
                    }
                }
            }
        }

        emitNeedleheartTrail(bird, sx, sy, ex, ey, 24, Color.web("#B2FF59"));
        emitNeedleheartBurst(bird, ex, ey, hitAny ? 26 : 14,
                hitAny ? Color.web("#FFF176") : Color.web("#FF5FD2"));
        moveToCenter(bird, ex, ey);
        bird.game.shakeIntensity = Math.max(bird.game.shakeIntensity, hitAny ? 7 : 4);
        bird.game.hitstopFrames = Math.max(bird.game.hitstopFrames, hitAny ? 2 : 1);
    }

    private static void performNeedleheartFinal(Bird bird) {
        bird.hummingFrenzyFinalResolved = true;
        setNeedleheartLine(bird, Bird.HUMMING_NEEDLEHEART_STRIKE_COUNT, true);
        double sx = bird.hummingFrenzyLastStartX;
        double sy = bird.hummingFrenzyLastStartY;
        double ex = bird.hummingFrenzyLastEndX;
        double ey = bird.hummingFrenzyLastEndY;
        boolean hitAny = false;

        for (Bird other : bird.game.players) {
            if (!bird.canDamageTarget(other)) continue;
            double distance = pointToSegmentDistance(other.bodyCenterX(), other.bodyCenterY(), sx, sy, ex, ey);
            if (distance > 54.0 * bird.sizeMultiplier + other.combatRadius()) continue;
            boolean primary = other.playerIndex == bird.hummingFrenzyTargetIndex;
            int damage = primary
                    ? (bird.hummingFrenzyConnectedStrikes >= 2 ? 7 : 5)
                    : 2;
            double dir = Math.signum(ex - sx);
            if (dir == 0.0) {
                dir = bird.facingDirection();
            }
            int dealt = applyNeedleheartHit(bird, other, damage,
                    dir * (primary ? 8.2 : 3.8),
                    primary ? -9.0 : -3.8,
                    primary);
            if (dealt > 0) {
                hitAny = true;
                other.applyStun(primary ? 5 : 2);
                if (primary) {
                    other.applyHummingbirdNectarCoating(bird, true);
                }
            }
        }

        emitNeedleheartTrail(bird, sx, sy, ex, ey, 46, Color.web("#FFF176"));
        emitNeedleheartBurst(bird, bird.hummingFrenzyTargetX, bird.hummingFrenzyTargetY,
                hitAny ? 58 : 36, Color.web("#E8FFFF"));
        moveToCenter(bird, ex, ey);
        bird.game.shakeIntensity = Math.max(bird.game.shakeIntensity, hitAny ? 15 : 10);
        bird.game.hitstopFrames = Math.max(bird.game.hitstopFrames, hitAny ? 4 : 2);
        bird.game.triggerFlash(hitAny ? 0.38 : 0.24, false);
        bird.game.addToKillFeed(bird.shortName() + " PIERCED THE NEEDLEHEART!");
    }

    private static int applyNeedleheartHit(Bird bird, Bird other, int damage, double launchX,
                                           double launchY, boolean primary) {
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
            bird.game.recordMoveKo(bird, other, NEEDLEHEART_OVERDRIVE_MOVE);
        }
        emitNeedleheartBurst(bird, other.bodyCenterX(), other.bodyCenterY(), primary ? 18 : 9,
                primary ? Color.web("#FFF176") : Color.web("#FF5FD2"));
        return dealt;
    }

    private static Bird nearestTarget(Bird bird) {
        Bird best = null;
        double bestDistance = Double.POSITIVE_INFINITY;
        for (Bird other : bird.game.players) {
            if (!bird.canDamageTarget(other)) continue;
            double distance = Math.hypot(other.bodyCenterX() - bird.bodyCenterX(),
                    other.bodyCenterY() - bird.bodyCenterY());
            if (distance > NEEDLEHEART_LOCK_RANGE * bird.sizeMultiplier + other.combatRadius()) {
                continue;
            }
            if (distance < bestDistance) {
                bestDistance = distance;
                best = other;
            }
        }
        return best;
    }

    private static Bird markedNeedleheartTarget(Bird bird) {
        int index = bird.hummingFrenzyTargetIndex;
        if (index >= 0 && index < bird.game.players.length) {
            Bird target = bird.game.players[index];
            if (bird.canDamageTarget(target)) {
                return target;
            }
        }
        return null;
    }

    private static void setNeedleheartLine(Bird bird, int strikeIndex, boolean finale) {
        double s = bird.sizeMultiplier;
        double tx = bird.hummingFrenzyTargetX;
        double ty = bird.hummingFrenzyTargetY;
        double side = ((strikeIndex & 1) == 0 ? -1.0 : 1.0) * bird.facingDirection();
        if (finale) {
            double dir = side >= 0.0 ? 1.0 : -1.0;
            bird.hummingFrenzyLastStartX = clampTargetX(tx - dir * 205.0 * s);
            bird.hummingFrenzyLastStartY = clampTargetY(ty - 158.0 * s);
            bird.hummingFrenzyLastEndX = clampTargetX(tx + dir * 114.0 * s);
            bird.hummingFrenzyLastEndY = clampTargetY(ty + 102.0 * s);
            return;
        }

        double reach = (154.0 + strikeIndex * 22.0) * s;
        double high = (74.0 + strikeIndex * 16.0) * s;
        double low = (42.0 + strikeIndex * 10.0) * s;
        if (strikeIndex == 0) {
            bird.hummingFrenzyLastStartX = clampTargetX(tx - side * reach);
            bird.hummingFrenzyLastStartY = clampTargetY(ty - high);
            bird.hummingFrenzyLastEndX = clampTargetX(tx + side * reach * 0.88);
            bird.hummingFrenzyLastEndY = clampTargetY(ty + low);
        } else if (strikeIndex == 1) {
            bird.hummingFrenzyLastStartX = clampTargetX(tx + side * reach * 0.72);
            bird.hummingFrenzyLastStartY = clampTargetY(ty - high * 0.30);
            bird.hummingFrenzyLastEndX = clampTargetX(tx - side * reach);
            bird.hummingFrenzyLastEndY = clampTargetY(ty + low * 0.20);
        } else {
            bird.hummingFrenzyLastStartX = clampTargetX(tx - side * reach * 0.54);
            bird.hummingFrenzyLastStartY = clampTargetY(ty + low);
            bird.hummingFrenzyLastEndX = clampTargetX(tx + side * reach * 0.74);
            bird.hummingFrenzyLastEndY = clampTargetY(ty - high * 0.82);
        }
    }

    private static void moveToCenter(Bird bird, double centerX, double centerY) {
        double x = Math.clamp(centerX - bird.bodyWidth() * 0.5, -20.0, BirdGame3.WORLD_WIDTH - bird.bodyWidth() + 20.0);
        double y = Math.clamp(centerY - bird.bodyHeight() * 0.5, -100.0, BirdGame3.GROUND_Y + 40.0);
        bird.x = x;
        bird.y = y;
    }

    private static void emitNeedleheartTrail(Bird bird, double x1, double y1, double x2, double y2,
                                             int count, Color color) {
        int particles = bird.scaledParticleCount(count);
        for (int i = 0; i < particles; i++) {
            double t = bird.game.nextParticleRandom();
            double px = x1 + (x2 - x1) * t;
            double py = y1 + (y2 - y1) * t;
            double angle = Math.atan2(y2 - y1, x2 - x1) + (bird.game.nextParticleRandom() - 0.5) * 0.54;
            double speed = 3.0 + bird.game.nextParticleRandom() * 8.0;
            bird.game.particles.add(new Particle(
                    px,
                    py,
                    Math.cos(angle) * speed,
                    Math.sin(angle) * speed - 1.5,
                    color.deriveColor(0, 1, 1, 0.84)
            ));
        }
    }

    private static void emitNeedleheartBurst(Bird bird, double cx, double cy, int count, Color color) {
        int particles = bird.scaledParticleCount(count);
        for (int i = 0; i < particles; i++) {
            double angle = bird.game.nextParticleRandom() * Math.PI * 2.0;
            double speed = 2.6 + bird.game.nextParticleRandom() * 9.4;
            bird.game.particles.add(new Particle(
                    cx + (bird.game.nextParticleRandom() - 0.5) * 18.0,
                    cy + (bird.game.nextParticleRandom() - 0.5) * 18.0,
                    Math.cos(angle) * speed,
                    Math.sin(angle) * speed - 1.8,
                    color.deriveColor(0, 1, 1, 0.86)
            ));
        }
    }

    private static void emitNeedleheartOrbitParticles(Bird bird, int count, Color color) {
        int particles = bird.scaledParticleCount(count);
        for (int i = 0; i < particles; i++) {
            double angle = bird.game.nextParticleRandom() * Math.PI * 2.0;
            double radius = 34.0 + bird.game.nextParticleRandom() * 104.0;
            bird.game.particles.add(new Particle(
                    bird.hummingFrenzyTargetX + Math.cos(angle) * radius,
                    bird.hummingFrenzyTargetY + Math.sin(angle) * radius * 0.58,
                    -Math.cos(angle) * (1.4 + bird.game.nextParticleRandom() * 3.8),
                    -Math.sin(angle) * (1.2 + bird.game.nextParticleRandom() * 3.2) - 0.8,
                    color
            ));
        }
    }

    private static double pointToSegmentDistance(double px, double py, double x1, double y1, double x2, double y2) {
        double dx = x2 - x1;
        double dy = y2 - y1;
        double lenSq = dx * dx + dy * dy;
        if (lenSq <= 0.0001) {
            return Math.hypot(px - x1, py - y1);
        }
        double t = ((px - x1) * dx + (py - y1) * dy) / lenSq;
        t = Math.clamp(t, 0.0, 1.0);
        double sx = x1 + dx * t;
        double sy = y1 + dy * t;
        return Math.hypot(px - sx, py - sy);
    }

    private static double clampTargetX(double x) {
        return Math.clamp(x, 38.0, BirdGame3.WORLD_WIDTH - 38.0);
    }

    private static double clampTargetY(double y) {
        return Math.clamp(y, 58.0, BirdGame3.GROUND_Y - 34.0);
    }
}
