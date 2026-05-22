package com.example.birdgame3;

import javafx.scene.paint.Color;

import java.util.Arrays;

final class HummingbirdSpecials {
    private HummingbirdSpecials() {
    }

    static void use(Bird bird, boolean ultimate) {
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
                    startX + bird.hummingNeedleDirection * Math.random() * 46.0,
                    startY + (Math.random() - 0.5) * 18.0,
                    bird.hummingNeedleDirection * (3.0 + Math.random() * 4.0),
                    (Math.random() - 0.5) * 2.0,
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
            double spread = (Math.random() - 0.5) * 58.0;
            bird.game.particles.add(new Particle(
                    bird.bodyCenterX() + spread,
                    bird.bodyBottomY() - 8.0 * bird.sizeMultiplier,
                    spread * 0.035,
                    5.0 + Math.random() * 7.0,
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
            double angle = Math.random() * Math.PI * 2.0;
            bird.game.particles.add(new Particle(
                    trapX,
                    startY - 12.0,
                    Math.cos(angle) * (2.0 + Math.random() * 4.0),
                    Math.sin(angle) * (2.0 + Math.random() * 3.0) - 2.0,
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
        return bird.hummingNeedleHitTimer > 0
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
                double angle = Math.random() * Math.PI * 2.0;
                bird.game.particles.add(new Particle(
                        other.bodyCenterX(),
                        other.bodyCenterY() - 6.0 * bird.sizeMultiplier,
                        Math.cos(angle) * (2.5 + Math.random() * (finisher ? 7.0 : 4.0)),
                        Math.sin(angle) * (2.5 + Math.random() * (finisher ? 7.0 : 4.0)) - 1.5,
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
                    centerX - dir * (12.0 + Math.random() * 72.0),
                    centerY + (Math.random() - 0.5) * 34.0,
                    -dir * (2.0 + Math.random() * 3.0),
                    (Math.random() - 0.5) * 2.0,
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
                double angle = Math.random() * Math.PI * 2.0;
                bird.game.particles.add(new Particle(
                        other.bodyCenterX(),
                        other.bodyCenterY(),
                        Math.cos(angle) * (3.0 + Math.random() * 6.0),
                        Math.sin(angle) * (3.0 + Math.random() * 6.0) - 2.0,
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
                double spread = (Math.random() - 0.5) * 48.0;
                bird.game.particles.add(new Particle(
                        bird.bodyCenterX() + spread,
                        bird.bodyBottomY() - 2.0 * bird.sizeMultiplier,
                        spread * 0.035,
                        7.0 + Math.random() * 7.0,
                        (bird.hummingHoverBurstUltimate ? Color.GOLD : Color.AQUA).deriveColor(0, 1, 1, 0.62)
                ));
            }
        }
    }
}
