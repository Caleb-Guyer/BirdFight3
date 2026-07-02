package com.example.birdgame3;

import javafx.scene.paint.Color;

import java.util.Arrays;

final class GrinchhawkSpecials {
    private GrinchhawkSpecials() {
    }

    static void use(Bird bird, boolean ultimate) {
        switch (bird.selectGrinchhawkSpecialVariant()) {
            case NEUTRAL -> neutral(bird, ultimate);
            case SIDE -> side(bird, ultimate);
            case UP -> up(bird, ultimate);
            case DOWN -> down(bird, ultimate);
        }
    }

    static void handleState(Bird bird, boolean jumpJustPressed, double gameSpeed, boolean gettingOff, boolean forcedOff) {
        boolean neutralCopy = bird.mockingbirdCopiedNeutralFrom(BirdGame3.BirdType.GRINCHHAWK);
        if (bird.type != BirdGame3.BirdType.GRINCHHAWK && !neutralCopy) {
            return;
        }

        if (bird.stunTime > 0.0) {
            reset(bird, false);
            if (neutralCopy) {
                bird.mockingbirdCopiedNeutralSource = null;
            }
        }

        if (bird.grinchHeartSnatchTimer > 0) {
            bird.grinchHeartSnatchTimer--;
        }

        if (bird.type != BirdGame3.BirdType.GRINCHHAWK) {
            return;
        }

        handleSleigh(bird, jumpJustPressed, gameSpeed, gettingOff, forcedOff);
        handleChimneyFlap(bird);
        handlePresent(bird);
    }

    static void handleSleigh(Bird bird, boolean jumpJustPressed, double gameSpeed, boolean gettingOff, boolean forcedOff) {
        if (!bird.grinchSleighActive) {
            return;
        }

        bird.grinchSleighTimer = Math.max(0, bird.grinchSleighTimer - Math.max(1, (int) Math.ceil(gameSpeed)));
        int dir = bird.grinchSleighDirection == 0 ? bird.facingDirection() : bird.grinchSleighDirection;
        double speed = (bird.grinchSleighUltimate ? Bird.GRINCH_SLEIGH_SPEED + 4.0 : Bird.GRINCH_SLEIGH_SPEED)
                * Math.max(0.6, gameSpeed);

        if (bird.grinchSleighRiding) {
            bird.grinchSleighX = bird.bodyCenterX();
            bird.grinchSleighY = bird.bodyBottomY() + 8.0 * bird.sizeMultiplier;
            bird.facingRight = dir > 0;
            if (jumpJustPressed || gettingOff || forcedOff) {
                dismountSleigh(bird, jumpJustPressed && !forcedOff);
            } else {
                bird.vx = dir * speed;
                bird.vy = Math.min(bird.vy, bird.isOnGround() ? -0.6 : 1.2);
                applySleighHits(bird, false);
            }
        } else {
            bird.grinchSleighX += dir * speed;
            bird.grinchSleighY = sleighSurfaceY(bird, bird.grinchSleighX);
            applySleighHits(bird, true);
        }

        double leftBound = bird.usesIslandBounds() ? bird.game.battlefieldLeftBound() - 80.0 : -120.0;
        double rightBound = bird.usesIslandBounds() ? bird.game.battlefieldRightBound() + 80.0 : BirdGame3.WORLD_WIDTH + 120.0;
        if (bird.grinchSleighTimer <= 0 || bird.grinchSleighX < leftBound || bird.grinchSleighX > rightBound || bird.health <= 0) {
            crashSleigh(bird);
        } else if ((bird.grinchSleighTimer & 3) == 0) {
            emitBurst(bird, bird.grinchSleighX - dir * 42.0 * bird.sizeMultiplier,
                    bird.grinchSleighY - 9.0 * bird.sizeMultiplier, -dir, bird.grinchSleighUltimate ? 4 : 3,
                    bird.grinchSleighUltimate ? Color.GOLD : Color.web("#EF9A9A"));
        }
    }

    static double sleighSurfaceY(Bird bird, double sleighX) {
        double bestY = bird.hasSolidGroundFloorUnderBody() ? BirdGame3.GROUND_Y + 8.0 * bird.sizeMultiplier : Double.POSITIVE_INFINITY;
        double sourceY = Double.isFinite(bird.grinchSleighY) && bird.grinchSleighY != 0.0
                ? bird.grinchSleighY - 42.0 * bird.sizeMultiplier
                : bird.bodyBottomY() - 18.0 * bird.sizeMultiplier;
        for (Platform p : bird.game.platforms) {
            boolean isCaveCeiling = bird.game.selectedMap == BirdGame3.MapType.CAVE
                    && p.y <= 1 && p.h >= 60 && p.w >= BirdGame3.WORLD_WIDTH - 10;
            if (isCaveCeiling || p.w <= 0 || p.h <= 0) continue;
            if (sleighX < p.x - 28.0 || sleighX > p.x + p.w + 28.0) continue;
            if (p.y < sourceY - 42.0) continue;
            if (p.y + 8.0 * bird.sizeMultiplier < bestY) {
                bestY = p.y + 8.0 * bird.sizeMultiplier;
            }
        }
        return Double.isFinite(bestY) ? bestY : bird.grinchSleighY;
    }

    static void dismountSleigh(Bird bird, boolean jumpOff) {
        if (!bird.grinchSleighRiding) {
            return;
        }
        bird.grinchSleighRiding = false;
        bird.grinchSleighX = bird.bodyCenterX();
        bird.grinchSleighY = bird.bodyBottomY() + 8.0 * bird.sizeMultiplier;
        if (jumpOff) {
            bird.vy = Math.min(bird.vy, -bird.type.jumpHeight * 0.70);
            bird.vx = -bird.grinchSleighDirection * 3.0;
            bird.canDoubleJump = true;
        } else {
            bird.vx *= 0.35;
            bird.vy = Math.min(bird.vy, -5.0);
        }
        bird.attackAnimationTimer = Math.max(bird.attackAnimationTimer, 8);
    }

    static void crashSleigh(Bird bird) {
        if (!bird.grinchSleighActive) {
            return;
        }
        emitBurst(bird, bird.grinchSleighX, bird.grinchSleighY - 16.0 * bird.sizeMultiplier,
                -bird.grinchSleighDirection, bird.grinchSleighUltimate ? 42 : 28,
                bird.grinchSleighUltimate ? Color.GOLD : Color.web("#C62828"));
        bird.game.shakeIntensity = Math.max(bird.game.shakeIntensity, bird.grinchSleighUltimate ? 14 : 9);
        bird.grinchSleighActive = false;
        bird.grinchSleighRiding = false;
        bird.grinchSleighTimer = 0;
        Arrays.fill(bird.grinchSleighHit, false);
    }

    static void applySleighHits(Bird bird, boolean crashOnHit) {
        if (!bird.grinchSleighActive) {
            return;
        }
        int dir = bird.grinchSleighDirection == 0 ? bird.facingDirection() : bird.grinchSleighDirection;
        double hitX = bird.grinchSleighRiding ? bird.bodyCenterX() + dir * 26.0 * bird.sizeMultiplier : bird.grinchSleighX;
        double hitY = bird.grinchSleighRiding ? bird.bodyCenterY() + 18.0 * bird.sizeMultiplier : bird.grinchSleighY - 34.0 * bird.sizeMultiplier;
        boolean hitAny = false;
        for (Bird other : bird.game.players) {
            if (!bird.canDamageTarget(other)) continue;
            if (other.playerIndex < 0 || other.playerIndex >= bird.grinchSleighHit.length) continue;
            if (bird.grinchSleighHit[other.playerIndex]) continue;
            double dx = other.bodyCenterX() - hitX;
            double dy = other.bodyCenterY() - hitY;
            if (Math.abs(dx) > (bird.grinchSleighUltimate ? 104.0 : 88.0) * bird.sizeMultiplier + other.combatHalfWidth()) continue;
            if (Math.abs(dy) > (bird.grinchSleighUltimate ? 70.0 : 58.0) * bird.sizeMultiplier + other.combatHalfHeight()) continue;

            bird.grinchSleighHit[other.playerIndex] = true;
            int dealt = dealDamage(
                    bird,
                    other,
                    bird.grinchSleighUltimate ? 16 : 12,
                    dir * (bird.grinchSleighUltimate ? 18.0 : 14.0),
                    bird.grinchSleighUltimate ? -6.2 : -4.6,
                    false,
                    crashOnHit ? "crashed into" : "sledded through",
                    bird.grinchSleighUltimate ? Color.GOLD : Color.web("#EF5350")
            );
            hitAny |= dealt > 0;
        }
        if (crashOnHit && hitAny) {
            crashSleigh(bird);
        }
    }

    static void handleChimneyFlap(Bird bird) {
        if (bird.grinchChimneyFlapTimer <= 0) {
            return;
        }
        bird.grinchChimneyFlapTimer--;
        if (bird.grinchChimneyFlapTimer > Bird.GRINCH_CHIMNEY_FLAP_FRAMES / 2) {
            bird.vy = Math.min(bird.vy, bird.grinchChimneyFlapUltimate ? -13.0 : -10.8);
        } else {
            bird.vx *= 0.94;
        }
        applyChimneyFlapHits(bird);
        if ((bird.grinchChimneyFlapTimer & 3) == 0) {
            emitBurst(bird, bird.bodyCenterX(), bird.bodyBottomY() - 12.0 * bird.sizeMultiplier,
                    bird.facingDirection(), bird.grinchChimneyFlapUltimate ? 5 : 3,
                    bird.grinchChimneyFlapUltimate ? Color.GOLD : Color.web("#F1F8E9"));
        }
    }

    static void applyChimneyFlapHits(Bird bird) {
        double centerX = bird.bodyCenterX();
        double centerY = bird.bodyCenterY() - 28.0 * bird.sizeMultiplier;
        double reach = (bird.grinchChimneyFlapUltimate ? 104.0 : 84.0) * bird.sizeMultiplier;
        double verticalReach = (bird.grinchChimneyFlapUltimate ? 132.0 : 108.0) * bird.sizeMultiplier;
        for (Bird other : bird.game.players) {
            if (!bird.canDamageTarget(other)) continue;
            if (other.playerIndex < 0 || other.playerIndex >= bird.grinchChimneyFlapHit.length) continue;
            if (bird.grinchChimneyFlapHit[other.playerIndex]) continue;
            double dx = other.bodyCenterX() - centerX;
            double dy = other.bodyCenterY() - centerY;
            if (Math.abs(dx) > reach + other.combatHalfWidth()) continue;
            if (Math.abs(dy) > verticalReach + other.combatHalfHeight()) continue;

            bird.grinchChimneyFlapHit[other.playerIndex] = true;
            dealDamage(
                    bird,
                    other,
                    bird.grinchChimneyFlapUltimate ? 11 : 8,
                    Math.signum(dx == 0.0 ? bird.facingDirection() : dx) * (bird.grinchChimneyFlapUltimate ? 7.5 : 5.5),
                    bird.grinchChimneyFlapUltimate ? -12.5 : -9.5,
                    false,
                    "chimney-flapped",
                    bird.grinchChimneyFlapUltimate ? Color.GOLD : Color.web("#F1F8E9")
            );
        }
    }

    static void handlePresent(Bird bird) {
        if (bird.grinchPresent == null) {
            return;
        }
        Bird.GrinchPresent present = bird.grinchPresent;
        present.ageFrames++;
        present.fuseFrames--;
        if (present.fuseFrames <= 0 || bird.health <= 0) {
            explodePresent(bird, present);
            return;
        }
        if ((present.ageFrames & 9) == 0) {
            emitBurst(bird, present.x, present.y - 15.0 * bird.sizeMultiplier,
                    0.0, present.ultimate ? 3 : 2, present.ultimate ? Color.GOLD : Color.web("#C62828"));
        }
        if (!present.armed()) {
            return;
        }
        for (Bird other : bird.game.players) {
            if (!bird.canDamageTarget(other)) continue;
            double dx = other.bodyCenterX() - present.x;
            double feetGap = Math.abs(other.bodyBottomY() - present.y);
            boolean touching = Math.abs(dx) <= (present.ultimate ? 72.0 : 56.0) * bird.sizeMultiplier + other.combatHalfWidth()
                    && (feetGap <= 34.0 * bird.sizeMultiplier || other.bodyCenterY() > present.y - 68.0 * bird.sizeMultiplier);
            if (touching) {
                explodePresent(bird, present);
                return;
            }
        }
    }

    static void explodePresent(Bird bird, Bird.GrinchPresent present) {
        if (present == null || bird.grinchPresent != present) {
            return;
        }
        bird.grinchPresent = null;
        double radius = (present.ultimate ? 128.0 : 96.0) * bird.sizeMultiplier;
        for (Bird other : bird.game.players) {
            if (!bird.canDamageTarget(other)) continue;
            double dx = other.bodyCenterX() - present.x;
            double dy = other.bodyCenterY() - (present.y - 32.0 * bird.sizeMultiplier);
            if (Math.abs(dx) > radius + other.combatHalfWidth()) continue;
            if (Math.abs(dy) > radius * 0.78 + other.combatHalfHeight()) continue;

            double pushDir = Math.signum(dx);
            if (pushDir == 0.0) {
                pushDir = bird.facingDirection();
            }
            dealDamage(
                    bird,
                    other,
                    present.ultimate ? 14 : 10,
                    pushDir * (present.ultimate ? 10.0 : 7.0),
                    present.ultimate ? -13.0 : -9.5,
                    false,
                    "gift-trapped",
                    present.ultimate ? Color.GOLD : Color.web("#C62828")
            );
        }
        emitBurst(bird, present.x, present.y - 28.0 * bird.sizeMultiplier,
                0.0, present.ultimate ? 58 : 38, present.ultimate ? Color.GOLD : Color.web("#C62828"));
        bird.game.shakeIntensity = Math.max(bird.game.shakeIntensity, present.ultimate ? 16 : 10);
    }

    static void neutral(Bird bird, boolean ultimate) {
        bird.grinchHeartSnatchTimer = ultimate ? Bird.GRINCH_HEART_SNATCH_FRAMES + 6 : Bird.GRINCH_HEART_SNATCH_FRAMES;
        bird.grinchHeartSnatchUltimate = ultimate;
        Arrays.fill(bird.grinchHeartSnatchHit, false);
        bird.specialCooldown = 0;
        bird.specialMaxCooldown = 0;
        bird.attackAnimationTimer = Math.max(bird.attackAnimationTimer, ultimate ? 18 : 14);
        bird.vx *= bird.isOnGround() ? 0.46 : 0.68;
        applyHeartSnatchHit(bird);
        emitBurst(bird, bird.bodyCenterX() + bird.facingDirection() * 48.0 * bird.sizeMultiplier,
                bird.bodyCenterY() - 4.0 * bird.sizeMultiplier, bird.facingDirection(),
                ultimate ? 26 : 16, ultimate ? Color.GOLD : Color.web("#AED581"));
    }

    static void applyHeartSnatchHit(Bird bird) {
        int dir = bird.facingDirection();
        double reach = (bird.grinchHeartSnatchUltimate ? 116.0 : 92.0) * bird.sizeMultiplier;
        double verticalReach = (bird.grinchHeartSnatchUltimate ? 84.0 : 66.0) * bird.sizeMultiplier;
        double originX = bird.bodyCenterX() + dir * 26.0 * bird.sizeMultiplier;
        double originY = bird.bodyCenterY() - 2.0 * bird.sizeMultiplier;
        for (Bird other : bird.game.players) {
            if (!bird.canDamageTarget(other)) continue;
            if (other.playerIndex < 0 || other.playerIndex >= bird.grinchHeartSnatchHit.length) continue;
            if (bird.grinchHeartSnatchHit[other.playerIndex]) continue;
            double forward = (other.bodyCenterX() - originX) * dir;
            if (forward < -other.combatHalfWidth() * 0.25 || forward > reach + other.combatHalfWidth()) continue;
            if (Math.abs(other.bodyCenterY() - originY) > verticalReach + other.combatHalfHeight()) continue;

            bird.grinchHeartSnatchHit[other.playerIndex] = true;
            int dealt = dealDamage(
                    bird,
                    other,
                    bird.grinchHeartSnatchUltimate ? 12 : 8,
                    dir * (bird.grinchHeartSnatchUltimate ? 9.5 : 6.5),
                    bird.grinchHeartSnatchUltimate ? -5.5 : -3.4,
                    true,
                    "snatched",
                    bird.grinchHeartSnatchUltimate ? Color.GOLD : Color.web("#8BC34A")
            );
            if (dealt > 0) {
                bird.heal(Math.max(2.0, dealt * (bird.grinchHeartSnatchUltimate ? 0.90 : 0.65)));
            }
        }
    }

    static void side(Bird bird, boolean ultimate) {
        int dir = bird.horizontalInputDirection();
        if (dir == 0) {
            dir = bird.facingDirection();
        }
        bird.facingRight = dir > 0;
        bird.grinchSleighActive = true;
        bird.grinchSleighRiding = true;
        bird.grinchSleighTimer = ultimate ? Bird.GRINCH_SLEIGH_LIFE_FRAMES + 60 : Bird.GRINCH_SLEIGH_LIFE_FRAMES;
        bird.grinchSleighDirection = dir;
        bird.grinchSleighUltimate = ultimate;
        bird.grinchSleighX = bird.bodyCenterX();
        bird.grinchSleighY = bird.bodyBottomY() + 8.0 * bird.sizeMultiplier;
        Arrays.fill(bird.grinchSleighHit, false);
        bird.vx = dir * (ultimate ? Bird.GRINCH_SLEIGH_SPEED + 4.0 : Bird.GRINCH_SLEIGH_SPEED);
        bird.vy = Math.min(bird.vy, bird.isOnGround() ? -1.0 : 1.0);
        bird.specialCooldown = 0;
        bird.specialMaxCooldown = 0;
        bird.attackAnimationTimer = Math.max(bird.attackAnimationTimer, 12);
        emitBurst(bird, bird.grinchSleighX - dir * 34.0 * bird.sizeMultiplier, bird.grinchSleighY,
                -dir, ultimate ? 30 : 20, ultimate ? Color.GOLD : Color.web("#C62828"));
    }

    static void up(Bird bird, boolean ultimate) {
        if (bird.grinchUpSpecialUsed && !ultimate) {
            return;
        }
        bird.grinchUpSpecialUsed = true;
        bird.grinchChimneyFlapTimer = ultimate ? Bird.GRINCH_CHIMNEY_FLAP_FRAMES + 8 : Bird.GRINCH_CHIMNEY_FLAP_FRAMES;
        bird.grinchChimneyFlapUltimate = ultimate;
        Arrays.fill(bird.grinchChimneyFlapHit, false);
        bird.canDoubleJump = true;
        bird.vx *= bird.isOnGround() ? 0.44 : 0.64;
        bird.vy = Math.min(bird.vy, -(ultimate ? 19.5 : 16.4));
        bird.specialCooldown = 0;
        bird.specialMaxCooldown = 0;
        bird.attackAnimationTimer = Math.max(bird.attackAnimationTimer, 16);
        emitBurst(bird, bird.bodyCenterX(), bird.bodyBottomY() - 8.0 * bird.sizeMultiplier,
                bird.facingDirection(), ultimate ? 40 : 28, ultimate ? Color.GOLD : Color.web("#F5F5F5"));
    }

    static void down(Bird bird, boolean ultimate) {
        int dir = bird.horizontalInputDirection();
        if (dir == 0) {
            dir = bird.facingDirection();
        }
        bird.facingRight = dir > 0;
        double presentX = bird.bodyCenterX() + dir * 56.0 * bird.sizeMultiplier;
        double presentY = presentSurfaceY(bird, presentX);
        bird.grinchPresent = new Bird.GrinchPresent(presentX, presentY, ultimate);
        bird.specialCooldown = 0;
        bird.specialMaxCooldown = 0;
        bird.attackAnimationTimer = Math.max(bird.attackAnimationTimer, 12);
        bird.vx *= bird.isOnGround() ? 0.54 : 0.78;
        emitBurst(bird, presentX, presentY - 12.0 * bird.sizeMultiplier,
                dir, ultimate ? 22 : 14, ultimate ? Color.GOLD : Color.web("#C62828"));
    }

    static double presentSurfaceY(Bird bird, double presentX) {
        double bestY = bird.hasSolidGroundFloorUnderBody() ? BirdGame3.GROUND_Y : Double.POSITIVE_INFINITY;
        double sourceY = bird.bodyBottomY() - 24.0 * bird.sizeMultiplier;
        for (Platform p : bird.game.platforms) {
            boolean isCaveCeiling = bird.game.selectedMap == BirdGame3.MapType.CAVE
                    && p.y <= 1 && p.h >= 60 && p.w >= BirdGame3.WORLD_WIDTH - 10;
            if (isCaveCeiling || p.w <= 0 || p.h <= 0) continue;
            if (presentX < p.x - 18.0 || presentX > p.x + p.w + 18.0) continue;
            if (p.y < sourceY - 18.0) continue;
            if (p.y < bestY) {
                bestY = p.y;
            }
        }
        return Double.isFinite(bestY) ? bestY : bird.bodyBottomY() + 8.0 * bird.sizeMultiplier;
    }

    static int dealDamage(Bird bird, Bird other, double rawDamage, double launchX, double launchY,
                          boolean steal, String verb, Color particleColor) {
        if (other == null) {
            return 0;
        }
        double oldHealth = other.health;
        int dealt = (int) bird.applyDamageTo(other, rawDamage);
        if (dealt <= 0) {
            bird.game.recordSpecialImpact(bird.playerIndex, 0, false);
            return 0;
        }

        bird.game.damageDealt[bird.playerIndex] += dealt;
        bird.game.recordSpecialImpact(bird.playerIndex, dealt, true);
        other.vx += launchX;
        other.vy += launchY;
        if (other.health <= 0 && oldHealth > 0) {
            bird.game.eliminations[bird.playerIndex]++;
        }
        bird.game.addToKillFeed(bird.shortName() + " " + verb + " " + other.shortName() + "! -" + dealt + " HP");
        bird.game.playHitSound(dealt);
        if (steal) {
            for (int i = 0; i < bird.scaledParticleCount(7); i++) {
                double angle = SimRng.next() * Math.PI * 2.0;
                bird.game.particles.add(new Particle(
                        other.bodyCenterX(),
                        other.bodyCenterY(),
                        Math.cos(angle) * (1.2 + SimRng.next() * 2.8),
                        Math.sin(angle) * (1.2 + SimRng.next() * 2.8) - 1.2,
                        Color.web("#AED581").deriveColor(0, 1, 1, 0.76)
                ));
            }
        }
        emitBurst(bird, other.bodyCenterX(), other.bodyCenterY(), Math.signum(launchX),
                12, particleColor);
        return dealt;
    }

    static void emitBurst(Bird bird, double cx, double cy, double dir, int count, Color color) {
        int particles = bird.scaledParticleCount(count);
        double baseAngle = dir == 0.0 ? -Math.PI / 2.0 : (dir > 0.0 ? 0.0 : Math.PI);
        for (int i = 0; i < particles; i++) {
            double angle = baseAngle + (SimRng.next() - 0.5) * 1.8;
            double speed = 2.0 + SimRng.next() * 7.0;
            bird.game.particles.add(new Particle(
                    cx + (SimRng.next() - 0.5) * 22.0 * bird.sizeMultiplier,
                    cy + (SimRng.next() - 0.5) * 18.0 * bird.sizeMultiplier,
                    Math.cos(angle) * speed,
                    Math.sin(angle) * speed - 1.8,
                    color.deriveColor(0, 1, 1, 0.72 + SimRng.next() * 0.16)
            ));
        }
    }

    static boolean active(Bird bird) {
        return bird.grinchHeartSnatchTimer > 0
                || bird.grinchSleighRiding
                || bird.grinchChimneyFlapTimer > 0;
    }

    static boolean ready(Bird bird, Bird.GrinchhawkSpecialVariant variant) {
        boolean ultimateReady = bird.isUltimateReady();
        return switch (variant) {
            case NEUTRAL -> bird.grinchHeartSnatchTimer <= 0;
            case SIDE -> ultimateReady || !bird.grinchSleighActive;
            case UP -> ultimateReady || !bird.grinchUpSpecialUsed;
            case DOWN -> ultimateReady || bird.grinchPresent == null;
        };
    }

    static boolean canConvertShieldIntoDown(Bird bird) {
        return bird.selectGrinchhawkSpecialVariant() == Bird.GrinchhawkSpecialVariant.DOWN
                && bird.isBlocking
                && bird.shieldStunFrames <= 0;
    }

    static boolean canStart(Bird bird, boolean grabbed, boolean dodging) {
        Bird.GrinchhawkSpecialVariant variant = bird.selectGrinchhawkSpecialVariant();
        boolean shieldConversion = canConvertShieldIntoDown(bird);
        return bird.type == BirdGame3.BirdType.GRINCHHAWK
                && bird.health > 0
                && bird.stunTime <= 0.0
                && !grabbed
                && (!bird.isBlocking || shieldConversion)
                && !dodging
                && ready(bird, variant);
    }

    static void reset(Bird bird, boolean clearObjects) {
        bird.grinchHeartSnatchTimer = 0;
        bird.grinchHeartSnatchUltimate = false;
        Arrays.fill(bird.grinchHeartSnatchHit, false);
        bird.grinchSleighRiding = false;
        if (clearObjects) {
            bird.grinchSleighActive = false;
            bird.grinchSleighTimer = 0;
            bird.grinchPresent = null;
        }
        bird.grinchChimneyFlapTimer = 0;
        bird.grinchChimneyFlapUltimate = false;
        Arrays.fill(bird.grinchChimneyFlapHit, false);
    }
}
