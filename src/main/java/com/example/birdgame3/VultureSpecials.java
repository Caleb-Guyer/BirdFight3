package com.example.birdgame3;

import javafx.scene.paint.Color;

import java.util.Arrays;
import java.util.Iterator;

final class VultureSpecials {
    static final String BLACK_SKY_FEAST_MOVE = "Black Sky Feast";

    private VultureSpecials() {
    }

    static void use(Bird bird, boolean ultimate) {
        if (bird.isNullRockForm()) {
            nullRock(bird, ultimate);
            return;
        }
        if (ultimate) {
            blackSky(bird);
            return;
        }
        switch (bird.selectVultureSpecialVariant()) {
            case NEUTRAL -> neutral(bird, false);
            case SIDE -> side(bird);
            case UP -> up(bird);
            case DOWN -> down(bird);
        }
    }

    static void neutral(Bird bird, boolean ultimate) {
        if (!ultimate && bird.vultureCrowTicks <= 0) {
            return;
        }
        int dir = bird.horizontalInputDirection();
        if (dir != 0) {
            bird.facingRight = dir > 0;
        }
        bird.vultureCallTimer = Bird.VULTURE_CALL_FRAMES + (ultimate ? 10 : 0);
        bird.vultureCallHoldFrames = 0;
        bird.vultureCallCrowsSummoned = 0;
        bird.vultureCallUltimate = ultimate;
        bird.vultureNeutralReuseTimer = ultimate ? 24 : Bird.VULTURE_NEUTRAL_REUSE_FRAMES;
        bird.specialCooldown = 0;
        bird.specialMaxCooldown = 0;
        bird.crowSwarmCooldown = 0;
        bird.attackAnimationTimer = Math.max(bird.attackAnimationTimer, 18);
        bird.vx *= bird.isOnGround() ? 0.42 : 0.68;
        if (!bird.isOnGround()) {
            bird.vy = Math.min(bird.vy, 1.4);
        }

        spawnCallCrow(bird, ultimate);
        bird.game.addToKillFeed(bird.shortName() + " called carrion crows!");
        emitBurst(bird, bird.bodyCenterX(), bird.bodyCenterY() - 12.0 * bird.sizeMultiplier,
                bird.facingDirection(), ultimate ? 28 : 18, ultimate ? Color.GOLD : Color.web("#21162B"));
    }

    static void side(Bird bird) {
        int dir = bird.horizontalInputDirection();
        if (dir == 0) {
            dir = bird.facingDirection();
        }
        bird.facingRight = dir > 0;
        bird.vultureGlideTimer = Bird.VULTURE_GLIDE_FRAMES;
        bird.vultureGlideDirection = dir;
        bird.vultureGlideUltimate = false;
        Arrays.fill(bird.vultureGlideHit, false);
        bird.vultureSideReuseTimer = Bird.VULTURE_GLIDE_REUSE_FRAMES;
        bird.specialCooldown = 0;
        bird.specialMaxCooldown = 0;
        bird.crowSwarmCooldown = 0;
        bird.attackAnimationTimer = Math.max(bird.attackAnimationTimer, bird.vultureGlideTimer);
        bird.vx = dir * 16.4;
        bird.vy = Math.min(bird.vy, bird.isOnGround() ? -2.0 : 1.2);
        emitBurst(bird, bird.bodyCenterX() - dir * 28.0 * bird.sizeMultiplier, bird.bodyCenterY(),
                -dir, 15, Color.web("#394049"));
    }

    static void up(Bird bird) {
        if (bird.vultureUpSpecialUsed) {
            return;
        }
        int dir = bird.horizontalInputDirection();
        if (dir != 0) {
            bird.facingRight = dir > 0;
        }
        bird.vultureUpSpecialUsed = true;
        bird.vultureThermalTimer = Bird.VULTURE_THERMAL_FRAMES;
        bird.vultureThermalUltimate = false;
        Arrays.fill(bird.vultureThermalHitCooldown, 0);
        bird.specialCooldown = 0;
        bird.specialMaxCooldown = 0;
        bird.attackAnimationTimer = Math.max(bird.attackAnimationTimer, bird.vultureThermalTimer);
        bird.vy = Math.min(bird.vy, -12.4);
        bird.vx += bird.horizontalInputDirection() * 2.8;
        bird.canDoubleJump = true;
        emitBurst(bird, bird.bodyCenterX(), bird.bodyBottomY() - 10.0 * bird.sizeMultiplier,
                bird.facingDirection(), 20, Color.web("#B0BEC5"));
    }

    static void down(Bird bird) {
        int dir = bird.horizontalInputDirection();
        if (dir == 0) {
            dir = bird.facingDirection();
        }
        bird.facingRight = dir > 0;
        double baitX = Math.clamp(bird.bodyCenterX() + dir * 72.0 * bird.sizeMultiplier,
                bird.usesIslandBounds() ? bird.game.battlefieldLeftBound() + 36.0 : 36.0,
                bird.usesIslandBounds() ? bird.game.battlefieldRightBound() - 36.0 : BirdGame3.WORLD_WIDTH - 36.0);
        double baitY = baitSurfaceY(bird, baitX);
        bird.vultureBait = new Bird.VultureBait(baitX, baitY, false);
        bird.vultureDownReuseTimer = Bird.VULTURE_DOWN_REUSE_FRAMES;
        bird.specialCooldown = 0;
        bird.specialMaxCooldown = 0;
        bird.crowSwarmCooldown = 0;
        bird.attackAnimationTimer = Math.max(bird.attackAnimationTimer, 16);
        bird.isBlocking = false;
        bird.shieldHoldVisual = 0.0;
        bird.vx *= bird.isOnGround() ? 0.45 : 0.70;
        if (!bird.isOnGround()) {
            bird.vy = Math.min(bird.vy, 2.0);
        }
        bird.game.addToKillFeed(bird.shortName() + " set a bone offering.");
        emitBurst(bird, baitX, baitY - 18.0 * bird.sizeMultiplier, dir,
                16, Color.web("#D7CCC8"));
    }

    static void blackSky(Bird bird) {
        reset(bird, false);
        bird.vultureBlackSkyTimer = Bird.VULTURE_BLACK_SKY_FRAMES;
        bird.vultureBlackSkySpawnTimer = Bird.VULTURE_BLACK_SKY_WAVE_INTERVAL;
        bird.vultureBlackSkyCrowsSpawned = 0;
        bird.vultureBlackSkyWaveIndex = 0;
        bird.vultureBlackSkyFinalHit = false;
        Arrays.fill(bird.vultureBlackSkyHit, false);
        bird.carrionSwarmTimer = Math.max(bird.carrionSwarmTimer, Bird.VULTURE_BLACK_SKY_FRAMES + 48);
        bird.crowSwarmCooldown = 0;
        bird.specialCooldown = 0;
        bird.specialMaxCooldown = 0;
        bird.attackAnimationTimer = Math.max(bird.attackAnimationTimer, 62);
        bird.vx *= 0.35;
        bird.vy = Math.min(bird.vy, -5.0);
        bird.game.addToKillFeed(bird.shortName() + " opened the Black Sky Feast!");
        bird.game.shakeIntensity = Math.max(bird.game.shakeIntensity, 30);
        bird.game.hitstopFrames = Math.max(bird.game.hitstopFrames, 12);
        bird.vultureBlackSkyCrowsSpawned += spawnCrowWave(bird, Bird.VULTURE_BLACK_SKY_INITIAL_CROWS, 720.0, 1.34);
        emitBurst(bird, bird.bodyCenterX(), bird.bodyCenterY(), bird.facingDirection(), 120, Color.BLACK);
    }

    static void handleState(Bird bird, boolean specialHeld) {
        if (bird.vultureCallTimer > 0) {
            handleCall(bird, specialHeld);
        }
        if (bird.type != BirdGame3.BirdType.VULTURE) {
            return;
        }
        if (bird.isNullRockForm()) {
            handleNullRockState(bird);
            return;
        }
        handleGlide(bird);
        handleThermal(bird);
        handleBait(bird);
        handleBlackSky(bird);
    }

    static void handleCall(Bird bird, boolean specialHeld) {
        bird.vultureCallTimer--;
        bird.vultureCallHoldFrames++;
        bird.vx *= bird.isOnGround() ? 0.78 : 0.90;
        if (!bird.isOnGround()) {
            bird.vy = Math.min(bird.vy, 2.4);
        }

        int maxCrows = bird.vultureCallUltimate ? Bird.VULTURE_CALL_MAX_CROWS + 2 : Bird.VULTURE_CALL_MAX_CROWS;
        int nextCallFrame = Bird.VULTURE_CALL_SPAWN_INTERVAL * bird.vultureCallCrowsSummoned;
        if (specialHeld && bird.vultureCallCrowsSummoned < maxCrows && bird.vultureCallHoldFrames >= nextCallFrame) {
            if (!spawnCallCrow(bird, bird.vultureCallUltimate)) {
                bird.vultureCallTimer = Math.min(bird.vultureCallTimer, 3);
            }
        }
        if (!specialHeld && bird.vultureCallHoldFrames > 8) {
            bird.vultureCallTimer = Math.min(bird.vultureCallTimer, 6);
        }
        if (!bird.vultureCallUltimate && bird.vultureCrowTicks <= 0 && bird.vultureCallHoldFrames > 8) {
            bird.vultureCallTimer = Math.min(bird.vultureCallTimer, 3);
        }
        if (bird.vultureCallCrowsSummoned >= maxCrows && bird.vultureCallHoldFrames > Bird.VULTURE_CALL_SPAWN_INTERVAL) {
            bird.vultureCallTimer = Math.min(bird.vultureCallTimer, 3);
        }
        if ((bird.vultureCallHoldFrames & 3) == 0) {
            emitBurst(bird, bird.bodyCenterX(), bird.bodyCenterY() - 16.0 * bird.sizeMultiplier,
                    bird.facingDirection(), bird.vultureCallUltimate ? 5 : 3,
                    bird.vultureCallUltimate ? Color.GOLD : Color.web("#2B1B34"));
        }
        if (bird.vultureCallTimer <= 0) {
            int reuse = bird.vultureCallUltimate
                    ? 34 + bird.vultureCallCrowsSummoned * 12
                    : Bird.VULTURE_NEUTRAL_REUSE_FRAMES;
            bird.vultureNeutralReuseTimer = Math.max(bird.vultureNeutralReuseTimer, reuse);
            bird.specialCooldown = 0;
            bird.specialMaxCooldown = 0;
            bird.crowSwarmCooldown = 0;
            bird.vultureCallHoldFrames = 0;
            bird.vultureCallCrowsSummoned = 0;
            bird.vultureCallUltimate = false;
        }
    }

    static void handleGlide(Bird bird) {
        if (bird.vultureGlideTimer <= 0) {
            return;
        }
        int dir = bird.vultureGlideDirection == 0 ? bird.facingDirection() : bird.vultureGlideDirection;
        bird.facingRight = dir > 0;
        int total = Bird.VULTURE_GLIDE_FRAMES + (bird.vultureGlideUltimate ? 8 : 0);
        double progress = 1.0 - Math.clamp(bird.vultureGlideTimer / (double) Math.max(1, total), 0.0, 1.0);
        double speed = (bird.vultureGlideUltimate ? 20.0 : 16.4) * (0.72 + 0.28 * Math.cos(progress * Math.PI * 0.5));
        if (bird.vultureGlideTimer > 5) {
            bird.vx = dir * speed;
            bird.vy = Math.min(bird.vy * 0.74, bird.vultureGlideUltimate ? 0.2 : 0.8);
        } else {
            bird.vx *= 0.86;
            bird.vy *= 0.92;
        }
        applyGlideHits(bird);
        if ((bird.vultureGlideTimer & 1) == 0) {
            emitBurst(bird, bird.bodyCenterX() - dir * 48.0 * bird.sizeMultiplier,
                    bird.bodyCenterY() + 4.0 * bird.sizeMultiplier, -dir,
                    bird.vultureGlideUltimate ? 5 : 3,
                    bird.vultureGlideUltimate ? Color.GOLD : Color.web("#263238"));
        }
        bird.vultureGlideTimer--;
        if (bird.vultureGlideTimer <= 0) {
            bird.vultureGlideUltimate = false;
            Arrays.fill(bird.vultureGlideHit, false);
        }
    }

    static void applyGlideHits(Bird bird) {
        int dir = bird.vultureGlideDirection == 0 ? bird.facingDirection() : bird.vultureGlideDirection;
        double hitX = bird.bodyCenterX() + dir * 66.0 * bird.sizeMultiplier;
        double hitY = bird.bodyCenterY() + 3.0 * bird.sizeMultiplier;
        for (Bird other : bird.game.players) {
            if (!bird.canDamageTarget(other)) continue;
            if (other.playerIndex < 0 || other.playerIndex >= bird.vultureGlideHit.length) continue;
            if (bird.vultureGlideHit[other.playerIndex]) continue;
            double dx = other.bodyCenterX() - hitX;
            double forward = dx * dir;
            if (forward < -other.combatHalfWidth() * 0.65) continue;
            if (forward > (bird.vultureGlideUltimate ? 142.0 : 118.0) * bird.sizeMultiplier + other.combatHalfWidth()) continue;
            double dy = Math.abs(other.bodyCenterY() - hitY);
            if (dy > (bird.vultureGlideUltimate ? 74.0 : 60.0) * bird.sizeMultiplier + other.combatHalfHeight()) continue;

            bird.vultureGlideHit[other.playerIndex] = true;
            int assists = consumeNearbyOwnedCrows(bird, hitX, hitY, (bird.vultureGlideUltimate ? 190.0 : 150.0) * bird.sizeMultiplier,
                    bird.vultureGlideUltimate ? 2 : 1);
            int dealt = dealDamage(
                    bird,
                    other,
                    (bird.vultureGlideUltimate ? 15 : 11) + assists * 3,
                    dir * ((bird.vultureGlideUltimate ? 15.0 : 11.5) + assists * 2.2),
                    (bird.vultureGlideUltimate ? -5.8 : -4.2) - assists * 1.4,
                    assists > 0 ? "dove with a crow through" : "gravewind-glided through",
                    bird.vultureGlideUltimate ? 22 : 14,
                    assists > 0 ? Color.web("#151515") : Color.web("#455A64")
            );
            if (dealt > 0) {
                bird.game.shakeIntensity = Math.max(bird.game.shakeIntensity, assists > 0 ? 10 : 6);
            }
        }
    }

    static void handleThermal(Bird bird) {
        if (bird.vultureThermalTimer <= 0) {
            return;
        }
        int total = Bird.VULTURE_THERMAL_FRAMES + (bird.vultureThermalUltimate ? 10 : 0);
        int dir = bird.horizontalInputDirection();
        if (dir != 0) {
            bird.facingRight = dir > 0;
            double desired = dir * (bird.vultureThermalUltimate ? 8.2 : 6.4);
            bird.vx += (desired - bird.vx) * 0.16;
        } else {
            bird.vx *= 0.98;
        }
        if (bird.vultureThermalTimer > total * 0.42) {
            bird.vy = Math.min(bird.vy, bird.vultureThermalUltimate ? -10.4 : -8.8);
        } else {
            bird.vy = Math.min(bird.vy, bird.vultureThermalUltimate ? -3.8 : -2.6);
        }
        bird.canDoubleJump = true;
        steerOwnedCrowsAroundVulture(bird);
        applyThermalHits(bird);
        if ((bird.vultureThermalTimer & 2) == 0) {
            emitBurst(bird, bird.bodyCenterX(), bird.bodyBottomY() - 8.0 * bird.sizeMultiplier,
                    bird.facingDirection(), bird.vultureThermalUltimate ? 6 : 4,
                    bird.vultureThermalUltimate ? Color.GOLD : Color.web("#CFD8DC"));
        }
        bird.vultureThermalTimer--;
        if (bird.vultureThermalTimer <= 0) {
            bird.vultureThermalUltimate = false;
            Arrays.fill(bird.vultureThermalHitCooldown, 0);
        }
    }

    static void applyThermalHits(Bird bird) {
        double centerX = bird.bodyCenterX();
        double centerY = bird.bodyCenterY() - 18.0 * bird.sizeMultiplier;
        double reach = (bird.vultureThermalUltimate ? 94.0 : 76.0) * bird.sizeMultiplier;
        double height = (bird.vultureThermalUltimate ? 156.0 : 126.0) * bird.sizeMultiplier;
        for (Bird other : bird.game.players) {
            if (!bird.canDamageTarget(other)) continue;
            if (other.playerIndex < 0 || other.playerIndex >= bird.vultureThermalHitCooldown.length) continue;
            if (bird.vultureThermalHitCooldown[other.playerIndex] > 0) continue;
            double dx = other.bodyCenterX() - centerX;
            double dy = other.bodyCenterY() - centerY;
            if (Math.abs(dx) > reach + other.combatHalfWidth()) continue;
            if (Math.abs(dy) > height * 0.55 + other.combatHalfHeight()) continue;
            bird.vultureThermalHitCooldown[other.playerIndex] = bird.vultureThermalUltimate ? 7 : 9;
            double pushDir = Math.signum(dx);
            if (pushDir == 0.0) {
                pushDir = bird.facingDirection();
            }
            dealDamage(
                    bird,
                    other,
                    bird.vultureThermalUltimate ? 7 : 5,
                    pushDir * (bird.vultureThermalUltimate ? 5.2 : 3.6),
                    bird.vultureThermalUltimate ? -10.5 : -8.0,
                    "thermal-lifted",
                    bird.vultureThermalUltimate ? 14 : 8,
                    bird.vultureThermalUltimate ? Color.GOLD : Color.web("#B0BEC5")
            );
        }
    }

    static void handleBait(Bird bird) {
        if (bird.vultureBait == null) {
            return;
        }
        Bird.VultureBait bait = bird.vultureBait;
        bait.ageFrames++;
        bait.lifeFrames--;
        if (bait.damageFlash > 0) {
            bait.damageFlash--;
        }
        if (bait.damageCooldown > 0) {
            bait.damageCooldown--;
        }
        if (bait.lifeFrames <= 0 || bird.health <= 0) {
            removeBaitCrows(bird, bait);
            bird.vultureBait = null;
            return;
        }
        if (!bait.releasedCrows) {
            bait.callFrames--;
            if (tryDestroyBait(bird, bait)) {
                return;
            }
            if (bait.callFrames <= 0) {
                releaseBaitCrows(bird, bait);
            }
        } else {
            updateBaitCrowSwarm(bird, bait);
        }
        if ((bait.ageFrames & 7) == 0) {
            emitBurst(bird, bait.x, bait.y - 18.0 * bird.sizeMultiplier, 0.0,
                    bait.ultimate ? 4 : 2,
                    bait.releasedCrows ? Color.web("#1A121F") : Color.web("#D7CCC8"));
        }
    }

    static boolean tryDestroyBait(Bird bird, Bird.VultureBait bait) {
        if (!bait.armed() || bait.damageCooldown > 0) {
            return false;
        }
        for (Bird other : bird.game.players) {
            if (!bird.canDamageTarget(other)) continue;
            if (other.attackAnimationTimer <= 0 && other.vultureGlideTimer <= 0 && other.bladeStormFrames <= 0) continue;
            double dx = other.bodyCenterX() - bait.x;
            double dy = other.bodyCenterY() - (bait.y - 22.0 * bird.sizeMultiplier);
            if (Math.abs(dx) > 62.0 * bird.sizeMultiplier + other.combatHalfWidth()) continue;
            if (Math.abs(dy) > 54.0 * bird.sizeMultiplier + other.combatHalfHeight()) continue;
            bait.health--;
            bait.damageFlash = 10;
            bait.damageCooldown = 12;
            emitBurst(bird, bait.x, bait.y - 20.0 * bird.sizeMultiplier,
                    Math.signum(dx), 12, Color.web("#EF5350"));
            if (bait.health <= 0) {
                bird.game.addToKillFeed(other.shortName() + " broke " + bird.shortName() + "'s offering!");
                removeBaitCrows(bird, bait);
                bird.vultureBait = null;
                return true;
            }
            return false;
        }
        return false;
    }

    static void releaseBaitCrows(Bird bird, Bird.VultureBait bait) {
        bait.releasedCrows = true;
        bait.spawnCooldown = 0;
        updateBaitCrowSwarm(bird, bait);
        bird.game.addToKillFeed(bird.shortName() + "'s offering drew the flock!");
        emitBurst(bird, bait.x, bait.y - 24.0 * bird.sizeMultiplier,
                0.0, bait.ultimate ? 36 : 24, bait.ultimate ? Color.GOLD : Color.web("#21162B"));
    }

    static void updateBaitCrowSwarm(Bird bird, Bird.VultureBait bait) {
        int maxCrows = bait.ultimate ? Bird.VULTURE_BAIT_MAX_CROWS + 2 : 8;
        if (bait.crowsReleased >= maxCrows) {
            return;
        }
        if (bait.spawnCooldown > 0) {
            bait.spawnCooldown--;
            return;
        }
        int waveSize = bait.crowsReleased >= 8 ? 3 : bait.crowsReleased >= 4 ? 2 : 1;
        if (bait.ultimate && bait.crowsReleased >= 6) {
            waveSize++;
        }
        waveSize = Math.min(waveSize, maxCrows - bait.crowsReleased);
        for (int i = 0; i < waveSize; i++) {
            spawnBaitCrow(bird, bait, bait.crowsReleased + i);
        }
        bait.crowsReleased += waveSize;
        bait.spawnCooldown = Math.max(22,
                (bait.ultimate ? Bird.VULTURE_BAIT_CROW_SPAWN_INTERVAL - 8 : Bird.VULTURE_BAIT_CROW_SPAWN_INTERVAL)
                        - bait.crowsReleased * 2);
    }

    static void spawnBaitCrow(Bird bird, Bird.VultureBait bait, int index) {
        double anchorY = bait.y - 58.0 * bird.sizeMultiplier;
        double angle = -Math.PI / 2.0 + index * 0.76 + (SimRng.next() - 0.5) * 0.26;
        double spawnRadius = (42.0 + SimRng.next() * 44.0) * bird.sizeMultiplier;
        double spawnX = bait.x + Math.cos(angle) * spawnRadius;
        double spawnY = anchorY + Math.sin(angle) * spawnRadius * 0.62;
        double guardRadius = (bait.ultimate ? 170.0 : 135.0) * bird.sizeMultiplier;
        CrowMinion crow = spawnCrow(bird, spawnX, spawnY,
                null,
                bait.ultimate, 1.0);
        crow.withAnchorGuard(bait.x, anchorY, guardRadius, bait.lifeFrames + 30);
        double tangent = angle + Math.PI * 0.5;
        crow.vx += Math.cos(tangent) * (bait.ultimate ? 2.4 : 1.8);
        crow.vy += Math.sin(tangent) * (bait.ultimate ? 2.0 : 1.5) - 1.0;
    }

    static void removeBaitCrows(Bird bird, Bird.VultureBait bait) {
        if (bait == null) {
            return;
        }
        double anchorY = bait.y - 58.0 * bird.sizeMultiplier;
        for (Iterator<CrowMinion> it = bird.game.crowMinions.iterator(); it.hasNext(); ) {
            CrowMinion crow = it.next();
            if (crow.owner == bird && crow.guardsAnchorNear(bait.x, anchorY, 12.0 * bird.sizeMultiplier)) {
                emitBurst(bird, crow.x, crow.y, 0.0, 8, Color.web("#21162B"));
                it.remove();
            }
        }
    }

    static void handleBlackSky(Bird bird) {
        if (bird.vultureBlackSkyTimer <= 0) {
            return;
        }
        bird.vultureBlackSkyTimer--;
        bird.vultureBlackSkySpawnTimer--;
        bird.carrionSwarmTimer = Math.max(bird.carrionSwarmTimer, 2);
        bird.vx *= 0.94;
        if (!bird.isOnGround()) {
            bird.vy = Math.min(bird.vy * 0.88, 0.6);
        }
        if (bird.vultureBlackSkySpawnTimer <= 0
                && bird.vultureBlackSkyCrowsSpawned < Bird.VULTURE_BLACK_SKY_TARGET_CROWS) {
            int remaining = Bird.VULTURE_BLACK_SKY_TARGET_CROWS - bird.vultureBlackSkyCrowsSpawned;
            int waveSize = Math.min(remaining, 4 + (bird.vultureBlackSkyWaveIndex % 3 == 2 ? 1 : 0));
            double spread = 820.0 + Math.min(4, bird.vultureBlackSkyWaveIndex) * 55.0;
            double speed = 1.36 + Math.min(6, bird.vultureBlackSkyWaveIndex) * 0.025;
            bird.vultureBlackSkyCrowsSpawned += spawnCrowWave(bird, waveSize, spread, speed);
            bird.vultureBlackSkyWaveIndex++;
            bird.vultureBlackSkySpawnTimer = Bird.VULTURE_BLACK_SKY_WAVE_INTERVAL;
        }
        double centerX = bird.bodyCenterX();
        double centerY = bird.bodyCenterY() - 60.0 * bird.sizeMultiplier;
        for (Bird other : bird.game.players) {
            if (!bird.canDamageTarget(other)) continue;
            double dx = centerX - other.bodyCenterX();
            double dy = centerY - other.bodyCenterY();
            if (Math.abs(dx) > 980.0 + other.combatHalfWidth()) continue;
            if (Math.abs(dy) > 520.0 + other.combatHalfHeight()) continue;
            double pullScale = bird.vultureBlackSkyTimer <= Bird.VULTURE_BLACK_SKY_FINAL_FRAME + 18 ? 1.45 : 1.0;
            other.vx += Math.signum(dx) * 0.24 * pullScale;
            other.vy += Math.signum(dy) * 0.09 * pullScale - 0.07;
        }
        if (!bird.vultureBlackSkyFinalHit && bird.vultureBlackSkyTimer <= Bird.VULTURE_BLACK_SKY_FINAL_FRAME) {
            bird.vultureBlackSkyFinalHit = true;
            applyBlackSkyFinalHit(bird);
            bird.game.shakeIntensity = Math.max(bird.game.shakeIntensity, 34);
            bird.game.hitstopFrames = Math.max(bird.game.hitstopFrames, 14);
            bird.game.triggerFlash(0.74, false);
        }
        if ((bird.vultureBlackSkyTimer & 3) == 0) {
            emitBurst(bird, centerX + (bird.game.nextParticleRandom() - 0.5) * 220.0 * bird.sizeMultiplier,
                    centerY + (bird.game.nextParticleRandom() - 0.5) * 160.0 * bird.sizeMultiplier,
                    0.0, 5, Color.BLACK);
        }
        if (bird.vultureBlackSkyTimer <= 0) {
            Arrays.fill(bird.vultureBlackSkyHit, false);
        }
    }

    static void applyBlackSkyFinalHit(Bird bird) {
        double centerX = bird.bodyCenterX();
        double centerY = bird.bodyCenterY() - 40.0 * bird.sizeMultiplier;
        for (Bird other : bird.game.players) {
            if (!bird.canDamageTarget(other)) continue;
            if (other.playerIndex < 0 || other.playerIndex >= bird.vultureBlackSkyHit.length) continue;
            if (bird.vultureBlackSkyHit[other.playerIndex]) continue;
            double dx = other.bodyCenterX() - centerX;
            double dy = other.bodyCenterY() - centerY;
            if (Math.abs(dx) > 760.0 * bird.sizeMultiplier + other.combatHalfWidth()) continue;
            if (Math.abs(dy) > 420.0 * bird.sizeMultiplier + other.combatHalfHeight()) continue;
            bird.vultureBlackSkyHit[other.playerIndex] = true;
            double pushDir = Math.signum(dx);
            if (pushDir == 0.0) {
                pushDir = bird.facingDirection();
            }
            dealDamage(
                    bird,
                    other,
                    23,
                    pushDir * 14.5,
                    -16.0,
                    "feasted on",
                    36,
                    Color.BLACK
            );
        }
        bird.vultureBlackSkyCrowsSpawned += spawnCrowWave(bird, Bird.VULTURE_BLACK_SKY_FINAL_CROWS, 560.0, 1.48);
        emitBurst(bird, centerX, centerY, bird.facingDirection(), 130, Color.BLACK);
    }

    static boolean spawnCallCrow(Bird bird, boolean ultimate) {
        if (!ultimate) {
            if (bird.vultureCrowTicks <= 0 || ownedCrowCount(bird) >= 7) {
                return false;
            }
            consumeCrowTick(bird);
        }
        int dir = bird.facingDirection();
        double lift = -28.0 - bird.vultureCallCrowsSummoned * 8.0;
        double spawnX = bird.bodyCenterX() - dir * (18.0 + bird.vultureCallCrowsSummoned * 8.0) * bird.sizeMultiplier;
        double spawnY = bird.bodyCenterY() + lift * bird.sizeMultiplier;
        CrowMinion crow = spawnCrow(bird, spawnX, spawnY,
                nearestTarget(bird, spawnX, spawnY, 700.0),
                ultimate, ultimate ? 1.10 : 1.0);
        crow.vx += dir * (ultimate ? 4.8 : 3.4);
        crow.vy -= ultimate ? 4.0 : 3.0;
        bird.vultureCallCrowsSummoned++;
        return true;
    }

    static void consumeCrowTick(Bird bird) {
        bird.vultureCrowTicks = Math.max(0, bird.vultureCrowTicks - 1);
        if (bird.vultureCrowTicks < Bird.VULTURE_CROW_TICK_MAX && bird.vultureCrowTickRechargeTimer <= 0) {
            bird.vultureCrowTickRechargeTimer = Bird.VULTURE_CROW_TICK_RECHARGE_FRAMES;
        }
    }

    static void updateCrowTicks(Bird bird, double gameSpeed) {
        if (bird.type != BirdGame3.BirdType.VULTURE || bird.isNullRockForm()) {
            return;
        }
        if (bird.vultureCrowTicks >= Bird.VULTURE_CROW_TICK_MAX) {
            bird.vultureCrowTicks = Bird.VULTURE_CROW_TICK_MAX;
            bird.vultureCrowTickRechargeTimer = 0;
            return;
        }
        if (bird.vultureCrowTickRechargeTimer <= 0) {
            bird.vultureCrowTickRechargeTimer = Bird.VULTURE_CROW_TICK_RECHARGE_FRAMES;
            return;
        }
        bird.vultureCrowTickRechargeTimer = Math.max(0, (int) (bird.vultureCrowTickRechargeTimer - gameSpeed));
        if (bird.vultureCrowTickRechargeTimer <= 0) {
            bird.vultureCrowTicks = Math.min(Bird.VULTURE_CROW_TICK_MAX, bird.vultureCrowTicks + 1);
            if (bird.vultureCrowTicks < Bird.VULTURE_CROW_TICK_MAX) {
                bird.vultureCrowTickRechargeTimer = Bird.VULTURE_CROW_TICK_RECHARGE_FRAMES;
            }
        }
    }

    static CrowMinion spawnCrow(Bird bird, double spawnX, double spawnY, Bird target, boolean ultimate, double speedMultiplier) {
        CrowMinion crow = new CrowMinion(spawnX, spawnY, target)
                .withVariant(CrowMinion.VARIANT_ALLIED_CROW)
                .withSpeedMultiplier(speedMultiplier)
                .withOverflowProtectionFrames(ultimate ? 140 : 80);
        crow.owner = bird;
        crow.life = Math.max(crow.life, ultimate ? 2 : 1);
        crow.hasCrown = ultimate;
        bird.game.crowMinions.add(crow);
        return crow;
    }

    static int spawnCrowWave(Bird bird, int count, double horizontalSpread, double speedMultiplier) {
        int spawned = 0;
        for (int i = 0; i < count; i++) {
            double spawnX = Math.clamp(bird.bodyCenterX() + (SimRng.next() - 0.5) * horizontalSpread,
                    bird.usesIslandBounds() ? bird.game.battlefieldLeftBound() - 120.0 : -120.0,
                    bird.usesIslandBounds() ? bird.game.battlefieldRightBound() + 120.0 : BirdGame3.WORLD_WIDTH + 120.0);
            double spawnY = bird.bodyCenterY() - (360.0 + SimRng.next() * 260.0) * bird.sizeMultiplier;
            Bird target = nearestTarget(bird, spawnX, spawnY, 1200.0);
            CrowMinion crow = spawnCrow(bird, spawnX, spawnY, target, true, speedMultiplier);
            double targetX = target == null ? bird.bodyCenterX() : target.bodyCenterX();
            double targetY = target == null ? bird.bodyCenterY() : target.bodyCenterY();
            double dx = targetX - spawnX;
            double dy = targetY - spawnY;
            double len = Math.max(1.0, Math.hypot(dx, dy));
            crow.vx += dx / len * (4.4 + SimRng.next() * 2.8);
            crow.vy += dy / len * (5.0 + SimRng.next() * 3.2);
            spawned++;
        }
        return spawned;
    }

    static Bird nearestTarget(Bird bird, double sourceX, double sourceY, double maxRange) {
        Bird best = null;
        double bestSq = maxRange * maxRange;
        for (Bird other : bird.game.players) {
            if (!bird.canDamageTarget(other)) continue;
            double dx = other.bodyCenterX() - sourceX;
            double dy = other.bodyCenterY() - sourceY;
            double distSq = dx * dx + dy * dy;
            if (distSq < bestSq) {
                bestSq = distSq;
                best = other;
            }
        }
        return best;
    }

    static int ownedCrowCount(Bird bird) {
        int count = 0;
        for (CrowMinion crow : bird.game.crowMinions) {
            if (crow.owner == bird) {
                count++;
            }
        }
        return count;
    }

    static int consumeNearbyOwnedCrows(Bird bird, double centerX, double centerY, double radius, int maxCount) {
        int count = 0;
        double radiusSq = radius * radius;
        for (Iterator<CrowMinion> it = bird.game.crowMinions.iterator(); it.hasNext() && count < maxCount; ) {
            CrowMinion crow = it.next();
            if (crow.owner != bird) continue;
            double dx = crow.x - centerX;
            double dy = crow.y - centerY;
            if (dx * dx + dy * dy > radiusSq) continue;
            emitBurst(bird, crow.x, crow.y, Math.signum(dx), 18, Color.BLACK);
            it.remove();
            count++;
        }
        return count;
    }

    static void steerOwnedCrowsAroundVulture(Bird bird) {
        int index = 0;
        double centerX = bird.bodyCenterX();
        double centerY = bird.bodyCenterY() - 10.0 * bird.sizeMultiplier;
        for (CrowMinion crow : bird.game.crowMinions) {
            if (crow.owner != bird || index >= 8) continue;
            double dx = crow.x - centerX;
            double dy = crow.y - centerY;
            if (dx * dx + dy * dy > 360.0 * 360.0) continue;
            double angle = bird.vultureThermalTimer * 0.24 + index * (Math.PI * 2.0 / 3.0);
            double targetX = centerX + Math.cos(angle) * 92.0 * bird.sizeMultiplier;
            double targetY = centerY + Math.sin(angle) * 58.0 * bird.sizeMultiplier;
            crow.vx += (targetX - crow.x) * 0.035;
            crow.vy += (targetY - crow.y) * 0.035;
            crow.retargetCooldown = Math.max(crow.retargetCooldown, 5);
            index++;
        }
    }

    static double baitSurfaceY(Bird bird, double baitX) {
        double bestY = bird.hasSolidGroundFloorUnderBody() ? BirdGame3.GROUND_Y : Double.POSITIVE_INFINITY;
        double sourceY = bird.bodyBottomY() - 18.0 * bird.sizeMultiplier;
        for (Platform p : bird.game.platforms) {
            boolean isCaveCeiling = bird.game.selectedMap == BirdGame3.MapType.CAVE
                    && p.y <= 1 && p.h >= 60 && p.w >= BirdGame3.WORLD_WIDTH - 10;
            if (isCaveCeiling || p.w <= 0 || p.h <= 0) continue;
            if (baitX < p.x - 24.0 || baitX > p.x + p.w + 24.0) continue;
            if (p.y < sourceY - 34.0) continue;
            if (p.y < bestY) {
                bestY = p.y;
            }
        }
        return Double.isFinite(bestY) ? bestY : bird.bodyBottomY() + 8.0 * bird.sizeMultiplier;
    }

    static int dealDamage(Bird bird, Bird other, double rawDamage, double launchX, double launchY,
                          String verb, int stunFrames, Color particleColor) {
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
        other.applyStun(stunFrames);
        if (other.health <= 0 && oldHealth > 0) {
            bird.game.eliminations[bird.playerIndex]++;
        }
        bird.game.addToKillFeed(bird.shortName() + " " + verb + " " + other.shortName() + "! -" + dealt + " HP");
        bird.game.playHitSound(dealt);
        emitBurst(bird, other.bodyCenterX(), other.bodyCenterY(), Math.signum(launchX), 14, particleColor);
        return dealt;
    }

    static void emitBurst(Bird bird, double centerX, double centerY, double dir, int count, Color color) {
        int particles = bird.scaledParticleCount(count);
        double baseAngle = dir == 0.0 ? -Math.PI / 2.0 : (dir > 0.0 ? 0.0 : Math.PI);
        for (int i = 0; i < particles; i++) {
            double angle = dir == 0.0
                    ? bird.game.nextParticleRandom() * Math.PI * 2.0
                    : baseAngle + (bird.game.nextParticleRandom() - 0.5) * 1.45;
            double speed = 1.4 + bird.game.nextParticleRandom() * 7.2;
            Color shade = color == Color.BLACK
                    ? (bird.game.nextParticleRandom() < 0.5 ? Color.web("#050308") : Color.web("#190B1F"))
                    : color;
            bird.game.particles.add(new Particle(
                    centerX + (bird.game.nextParticleRandom() - 0.5) * 18.0 * bird.sizeMultiplier,
                    centerY + (bird.game.nextParticleRandom() - 0.5) * 16.0 * bird.sizeMultiplier,
                    Math.cos(angle) * speed,
                    Math.sin(angle) * speed - 1.3,
                    shade.deriveColor(0, 1, 1, 0.68 + bird.game.nextParticleRandom() * 0.18)
            ));
        }
    }

    static void nullRock(Bird bird, boolean ultimate) {
        switch (bird.selectVultureSpecialVariant()) {
            case NEUTRAL -> nullRockNeutral(bird, ultimate);
            case SIDE -> nullRockSide(bird, ultimate);
            case UP -> nullRockUp(bird, ultimate);
            case DOWN -> nullRockDown(bird, ultimate);
        }
    }

    static void nullRockNeutral(Bird bird, boolean ultimate) {
        beginNullRockSpecial(bird, ultimate);
        bird.game.summonNullRockSpecialFlock(bird, ultimate);
        bird.game.shakeIntensity = Math.max(bird.game.shakeIntensity, ultimate ? 28 : 20);
        bird.game.hitstopFrames = Math.max(bird.game.hitstopFrames, ultimate ? 10 : 7);
        bird.carrionSwarmTimer = ultimate ? 240 : 180;
        emitNullRockBurst(bird, ultimate ? 210 : 145, ultimate);
    }

    static void nullRockSide(Bird bird, boolean ultimate) {
        beginNullRockSpecial(bird, ultimate);
        Bird target = closestNullRockTarget(bird);
        bird.nullRockLaserTimer = Bird.NULL_ROCK_LASER_FRAMES;
        bird.nullRockLaserUltimate = ultimate;
        bird.nullRockLaserFired = false;
        bird.nullRockLaserTargetIndex = target != null ? target.playerIndex : -1;
        double fallbackDir = bird.facingDirection();
        bird.nullRockLaserTargetX = target != null
                ? target.bodyCenterX()
                : bird.bodyCenterX() + fallbackDir * 1600.0;
        bird.nullRockLaserTargetY = target != null ? target.bodyCenterY() : bird.bodyCenterY();
        if (target != null) {
            bird.facingRight = target.bodyCenterX() >= bird.bodyCenterX();
        }
        bird.attackAnimationTimer = Math.max(bird.attackAnimationTimer, Bird.NULL_ROCK_LASER_FRAMES);
        bird.vx *= 0.18;
        bird.vy = Math.min(bird.vy, 1.0);
        bird.game.addToKillFeed(bird.shortName() + " fixes its divine gaze on the nearest enemy.");
    }

    static void nullRockUp(Bird bird, boolean ultimate) {
        beginNullRockSpecial(bird, ultimate);
        bird.nullRockLiftTimer = Bird.NULL_ROCK_LIFT_FRAMES;
        bird.nullRockLiftUltimate = ultimate;
        bird.vy = Math.min(bird.vy, ultimate ? -16.5 : -13.5);
        bird.vx *= 0.55;
        spawnNullRockHenchmen(bird, 3, ultimate);
        bird.carrionSwarmTimer = Math.max(bird.carrionSwarmTimer, 80);
        bird.game.shakeIntensity = Math.max(bird.game.shakeIntensity, ultimate ? 20 : 14);
        bird.game.addToKillFeed(bird.shortName() + " is lifted by three vulture henchmen!");
        emitNullRockBurst(bird, ultimate ? 120 : 78, ultimate);
    }

    static void nullRockDown(Bird bird, boolean ultimate) {
        beginNullRockSpecial(bird, ultimate);
        Bird target = closestNullRockTarget(bird);
        double targetX = target != null ? target.bodyCenterX() : bird.bodyCenterX();
        double targetY = target != null ? target.bodyCenterY() : BirdGame3.GROUND_Y - 80.0;
        bird.nullRockSpearTimer = Bird.NULL_ROCK_SPEAR_FRAMES;
        bird.nullRockSpearCount = ultimate ? Bird.NULL_ROCK_MAX_SPEARS : 5;
        bird.nullRockSpearUltimate = ultimate;
        Arrays.fill(bird.nullRockSpearSpent, true);
        double spacing = ultimate ? 118.0 : 142.0;
        double centerIndex = (bird.nullRockSpearCount - 1) * 0.5;
        for (int i = 0; i < bird.nullRockSpearCount; i++) {
            bird.nullRockSpearX[i] = targetX + (i - centerIndex) * spacing;
            bird.nullRockSpearY[i] = Math.max(-260.0, targetY - 940.0 - (i % 2) * 110.0);
            bird.nullRockSpearDelay[i] = 22 + i * (ultimate ? 6 : 8);
            bird.nullRockSpearSpent[i] = false;
        }
        bird.attackAnimationTimer = Math.max(bird.attackAnimationTimer, 56);
        bird.vx *= 0.28;
        bird.game.shakeIntensity = Math.max(bird.game.shakeIntensity, ultimate ? 24 : 17);
        bird.game.addToKillFeed(bird.shortName() + " condemns the arena to a rain of bloodied spears.");
    }

    static int nullRockSpecialCooldown(Bird bird, boolean ultimate) {
        boolean cpu = bird.playerIndex >= 0
                && bird.playerIndex < bird.game.isAI.length
                && bird.game.isAI[bird.playerIndex];
        int frames;
        if (cpu) {
            int level = bird.game.getCpuLevel(bird.playerIndex);
            frames = 1140 - (level - 1) * 65;
        } else {
            frames = 540;
        }
        if (ultimate) frames -= 90;
        return Math.max(480, frames);
    }

    static int ownedNullRockHenchmanCount(Bird bird) {
        int count = 0;
        for (CrowMinion crow : bird.game.crowMinions) {
            if (crow.owner == bird && crow.effectiveVariant() == CrowMinion.VARIANT_VULTURE_HENCHMAN) {
                count++;
            }
        }
        return count;
    }

    private static void beginNullRockSpecial(Bird bird, boolean ultimate) {
        int cooldown = nullRockSpecialCooldown(bird, ultimate);
        bird.crowSwarmCooldown = cooldown;
        bird.specialCooldown = cooldown;
        bird.specialMaxCooldown = cooldown;
        bird.nullRockSpecialCycle = Math.floorMod(bird.nullRockSpecialCycle + 1, 4);
        bird.isBlocking = false;
        bird.shieldHoldVisual = 0.0;
    }

    private static void handleNullRockState(Bird bird) {
        handleNullRockLaser(bird);
        handleNullRockLift(bird);
        handleNullRockSpears(bird);
    }

    private static void handleNullRockLaser(Bird bird) {
        if (bird.nullRockLaserTimer <= 0) return;
        int elapsed = Bird.NULL_ROCK_LASER_FRAMES - bird.nullRockLaserTimer;
        if (elapsed < Bird.NULL_ROCK_LASER_WINDUP_FRAMES) {
            Bird target = playerAt(bird, bird.nullRockLaserTargetIndex);
            if (target != null && target.health > 0 && bird.canDamageTarget(target)) {
                bird.nullRockLaserTargetX = target.bodyCenterX();
                bird.nullRockLaserTargetY = target.bodyCenterY();
                bird.facingRight = target.bodyCenterX() >= bird.bodyCenterX();
            }
            bird.vx *= 0.72;
            bird.vy = Math.min(bird.vy, 1.5);
        } else if (!bird.nullRockLaserFired) {
            fireNullRockLaser(bird);
            bird.nullRockLaserFired = true;
        }
        bird.nullRockLaserTimer--;
    }

    private static void fireNullRockLaser(Bird bird) {
        double dir = bird.facingDirection();
        double startX = bird.bodyCenterX() + dir * 30.0 * bird.sizeMultiplier;
        double startY = bird.y + 10.0 * bird.sizeMultiplier;
        double dx = bird.nullRockLaserTargetX - startX;
        double dy = bird.nullRockLaserTargetY - startY;
        double length = Math.max(1.0, Math.hypot(dx, dy));
        double nx = dx / length;
        double ny = dy / length;
        double endX = startX + nx * 5200.0;
        double endY = startY + ny * 5200.0;
        double beamRadius = (bird.nullRockLaserUltimate ? 58.0 : 46.0) * Math.sqrt(bird.sizeMultiplier);
        double rawDamage = bird.nullRockLaserUltimate ? 20.0 : 14.0;
        for (Bird other : bird.game.players) {
            if (other == null || other == bird || other.health <= 0 || !bird.canDamageTarget(other)) continue;
            double radius = Math.max(other.combatHalfWidth(), other.combatHalfHeight()) + beamRadius;
            if (distanceToSegment(other.bodyCenterX(), other.bodyCenterY(), startX, startY, endX, endY) > radius) {
                continue;
            }
            dealDamage(bird, other, rawDamage,
                    nx * (bird.nullRockLaserUltimate ? 22.0 : 17.0),
                    ny * 10.0 - (bird.nullRockLaserUltimate ? 8.0 : 5.0),
                    "scoured", bird.nullRockLaserUltimate ? 18 : 13, Color.web("#FF1744"));
        }
        bird.game.shakeIntensity = Math.max(bird.game.shakeIntensity, bird.nullRockLaserUltimate ? 34 : 27);
        bird.game.hitstopFrames = Math.max(bird.game.hitstopFrames, bird.nullRockLaserUltimate ? 8 : 5);
    }

    private static void handleNullRockLift(Bird bird) {
        if (bird.nullRockLiftTimer <= 0) return;
        int elapsed = Bird.NULL_ROCK_LIFT_FRAMES - bird.nullRockLiftTimer;
        if (elapsed < 28) {
            bird.vy = Math.min(bird.vy, bird.nullRockLiftUltimate ? -15.0 : -12.0);
            bird.vx *= 0.94;
        }
        bird.nullRockLiftTimer--;
    }

    private static void handleNullRockSpears(Bird bird) {
        if (bird.nullRockSpearTimer <= 0) return;
        int elapsed = Bird.NULL_ROCK_SPEAR_FRAMES - bird.nullRockSpearTimer;
        double spearScale = Math.max(1.35, Math.sqrt(bird.sizeMultiplier));
        double spearLength = 112.0 * spearScale;
        double fallSpeed = bird.nullRockSpearUltimate ? 43.0 : 36.0;
        for (int i = 0; i < bird.nullRockSpearCount; i++) {
            if (bird.nullRockSpearSpent[i] || elapsed < bird.nullRockSpearDelay[i]) continue;
            double previousTip = bird.nullRockSpearY[i] + spearLength;
            bird.nullRockSpearY[i] += fallSpeed;
            double currentTip = bird.nullRockSpearY[i] + spearLength;
            for (Bird other : bird.game.players) {
                if (other == null || other == bird || other.health <= 0 || !bird.canDamageTarget(other)) continue;
                double horizontalReach = other.combatHalfWidth() + 13.0 * spearScale;
                double top = other.bodyCenterY() - other.combatHalfHeight();
                double bottom = other.bodyCenterY() + other.combatHalfHeight();
                if (Math.abs(other.bodyCenterX() - bird.nullRockSpearX[i]) > horizontalReach
                        || currentTip < top || previousTip > bottom + fallSpeed) {
                    continue;
                }
                double side = Math.signum(other.bodyCenterX() - bird.nullRockSpearX[i]);
                dealDamage(bird, other, bird.nullRockSpearUltimate ? 13.0 : 9.0,
                        side * 5.0, bird.nullRockSpearUltimate ? 14.0 : 10.0,
                        "impaled", bird.nullRockSpearUltimate ? 15 : 10, Color.web("#B71C1C"));
                bird.nullRockSpearSpent[i] = true;
                break;
            }
            if (bird.nullRockSpearY[i] > BirdGame3.GROUND_Y + 180.0) {
                bird.nullRockSpearSpent[i] = true;
            }
        }
        bird.nullRockSpearTimer--;
    }

    private static int spawnNullRockHenchmen(Bird bird, int desiredCount, boolean ultimate) {
        int existing = ownedNullRockHenchmanCount(bird);
        int spawned = 0;
        for (int slot = existing; slot < desiredCount; slot++) {
            double side = slot - 1.0;
            CrowMinion henchman = new CrowMinion(
                    bird.bodyCenterX() + side * 62.0 * Math.sqrt(bird.sizeMultiplier),
                    bird.bodyCenterY() + 38.0 * bird.sizeMultiplier,
                    null
            ).withVariant(CrowMinion.VARIANT_VULTURE_HENCHMAN)
                    .withSpeedMultiplier(ultimate ? 1.32 : 1.16)
                    .withOverflowProtectionFrames(ultimate ? 300 : 180);
            henchman.owner = bird;
            henchman.life = Math.max(henchman.life, ultimate ? 7 : 5);
            henchman.hasCrown = ultimate;
            henchman.vx = side * 3.8;
            henchman.vy = -7.5 - slot * 0.8;
            bird.game.crowMinions.add(henchman);
            spawned++;
        }
        return spawned;
    }

    private static Bird closestNullRockTarget(Bird bird) {
        Bird closest = null;
        double bestDistanceSq = Double.MAX_VALUE;
        for (Bird other : bird.game.players) {
            if (other == null || other == bird || other.health <= 0 || !bird.canDamageTarget(other)) continue;
            double dx = other.bodyCenterX() - bird.bodyCenterX();
            double dy = other.bodyCenterY() - bird.bodyCenterY();
            double distanceSq = dx * dx + dy * dy;
            if (distanceSq < bestDistanceSq) {
                bestDistanceSq = distanceSq;
                closest = other;
            }
        }
        return closest;
    }

    private static Bird playerAt(Bird bird, int index) {
        if (index < 0 || index >= bird.game.players.length) return null;
        return bird.game.players[index];
    }

    private static double distanceToSegment(double px, double py, double ax, double ay, double bx, double by) {
        double dx = bx - ax;
        double dy = by - ay;
        double lengthSq = dx * dx + dy * dy;
        if (lengthSq <= 0.0001) return Math.hypot(px - ax, py - ay);
        double t = Math.clamp(((px - ax) * dx + (py - ay) * dy) / lengthSq, 0.0, 1.0);
        return Math.hypot(px - (ax + t * dx), py - (ay + t * dy));
    }

    private static void emitNullRockBurst(Bird bird, int count, boolean ultimate) {
        int particleCount = bird.scaledParticleCount(count);
        for (int i = 0; i < particleCount; i++) {
            double angle = bird.game.nextParticleRandom() * Math.PI * 2.0;
            double speed = 7.0 + bird.game.nextParticleRandom() * 15.0;
            Color shade = switch (i % 3) {
                case 1 -> Color.web("#16020C");
                case 2 -> Color.web("#25102B");
                default -> Color.BLACK;
            };
            bird.game.particles.add(new Particle(
                    bird.bodyCenterX(), bird.bodyCenterY(),
                    Math.cos(angle) * speed, Math.sin(angle) * speed - 5.0,
                    shade.deriveColor(0, 1, 1, ultimate ? 0.95 : 0.82)
            ));
        }
    }

    static boolean active(Bird bird) {
        return bird.vultureCallTimer > 0
                || bird.vultureGlideTimer > 0
                || bird.vultureThermalTimer > 0
                || bird.vultureBlackSkyTimer > 0
                || bird.nullRockLaserTimer > 0
                || bird.nullRockLiftTimer > 0
                || bird.nullRockSpearTimer > 0;
    }

    static boolean ready(Bird bird, Bird.VultureSpecialVariant variant) {
        boolean ultimateReady = bird.isUltimateReady();
        return switch (variant) {
            case NEUTRAL -> ultimateReady || (bird.vultureNeutralReuseTimer <= 0 && bird.vultureCrowTicks > 0 && ownedCrowCount(bird) < 7);
            case SIDE -> ultimateReady || bird.vultureSideReuseTimer <= 0;
            case UP -> ultimateReady || !bird.vultureUpSpecialUsed;
            case DOWN -> ultimateReady || (bird.vultureDownReuseTimer <= 0 && bird.vultureBait == null);
        };
    }

    static boolean canConvertShieldIntoDown(Bird bird) {
        return bird.selectVultureSpecialVariant() == Bird.VultureSpecialVariant.DOWN
                && bird.isBlocking
                && bird.shieldStunFrames <= 0;
    }

    static boolean canStart(Bird bird, boolean grabbed, boolean dodging) {
        Bird.VultureSpecialVariant variant = bird.selectVultureSpecialVariant();
        boolean shieldConversion = canConvertShieldIntoDown(bird);
        boolean ultimateReady = bird.isUltimateReady();
        boolean ready = bird.isNullRockForm()
                ? ultimateReady || bird.specialCooldown <= 0
                : ready(bird, variant);
        return bird.type == BirdGame3.BirdType.VULTURE
                && bird.health > 0
                && bird.stunTime <= 0.0
                && !grabbed
                && (!bird.isBlocking || shieldConversion)
                && !dodging
                && !active(bird)
                && ready;
    }

    static void reset(Bird bird, boolean clearObjects) {
        bird.vultureCallTimer = 0;
        bird.vultureCallHoldFrames = 0;
        bird.vultureCallCrowsSummoned = 0;
        bird.vultureCallUltimate = false;
        bird.vultureGlideTimer = 0;
        bird.vultureGlideDirection = bird.facingDirection();
        bird.vultureGlideUltimate = false;
        Arrays.fill(bird.vultureGlideHit, false);
        bird.vultureThermalTimer = 0;
        bird.vultureThermalUltimate = false;
        Arrays.fill(bird.vultureThermalHitCooldown, 0);
        bird.vultureBlackSkyTimer = 0;
        bird.vultureBlackSkySpawnTimer = 0;
        bird.vultureBlackSkyCrowsSpawned = 0;
        bird.vultureBlackSkyWaveIndex = 0;
        bird.vultureBlackSkyFinalHit = false;
        Arrays.fill(bird.vultureBlackSkyHit, false);
        bird.nullRockLaserTimer = 0;
        bird.nullRockLaserTargetIndex = -1;
        bird.nullRockLaserUltimate = false;
        bird.nullRockLaserFired = false;
        bird.nullRockLiftTimer = 0;
        bird.nullRockLiftUltimate = false;
        bird.nullRockSpearTimer = 0;
        bird.nullRockSpearCount = 0;
        bird.nullRockSpearUltimate = false;
        Arrays.fill(bird.nullRockSpearSpent, true);
        if (clearObjects) {
            bird.vultureBait = null;
            bird.game.crowMinions.removeIf(crow -> crow.owner == bird
                    && crow.effectiveVariant() == CrowMinion.VARIANT_VULTURE_HENCHMAN);
        }
    }

    static void interruptOnHit(Bird bird) {
        if (bird.type != BirdGame3.BirdType.VULTURE) {
            return;
        }
        if (bird.vultureCallTimer > 0 || bird.vultureGlideTimer > 0 || bird.vultureThermalTimer > 0) {
            bird.attackAnimationTimer = 0;
        }
        reset(bird, false);
    }
}
