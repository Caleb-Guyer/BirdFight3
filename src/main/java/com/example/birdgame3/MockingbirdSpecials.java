package com.example.birdgame3;

import javafx.scene.paint.Color;

final class MockingbirdSpecials {
    static final String SHADOW_COURT_MOVE = "Charles Shadow Court";
    static final String MICROPHONE_SWING_MOVE = "Charles Microphone Swing";

    private MockingbirdSpecials() {
    }

    static void use(Bird bird, boolean ultimate) {
        if (ultimate) {
            shadowCourt(bird);
            return;
        }
        switch (bird.selectMockingbirdSpecialVariant()) {
            case NEUTRAL -> neutral(bird, ultimate);
            case SIDE -> side(bird, ultimate);
            case UP -> up(bird, ultimate);
            case DOWN -> down(bird, ultimate);
        }
    }

    static void neutral(Bird bird, boolean ultimate) {
        if (bird.mockingbirdCapturedType == null) {
            performBlowbackCall(bird);
            return;
        }

        performCopiedNeutral(bird, bird.mockingbirdCapturedType, ultimate);
    }

    private static void performBlowbackCall(Bird bird) {
        bird.mockingbirdBlowbackTimer = Bird.MOCKINGBIRD_BLOWBACK_FRAMES;
        bird.attackAnimationTimer = Math.max(bird.attackAnimationTimer, 16);
        bird.specialCooldown = Bird.MOCKINGBIRD_BLOWBACK_FRAMES;
        // Charles has directional reuse timers and deliberately shows no generic cooldown bar.
        bird.specialMaxCooldown = 0;
        bird.vx *= bird.isOnGround() ? 0.58 : 0.78;
        int dir = bird.facingDirection();
        double centerX = bird.bodyCenterX();
        double centerY = bird.bodyCenterY();
        double size = bird.sizeMultiplier;

        for (Bird other : bird.game.players) {
            if (!bird.canDamageTarget(other) || other.isCombatInvulnerable()) continue;
            double dx = other.bodyCenterX() - centerX;
            double forward = dx * dir;
            if (forward < -other.combatHalfWidth()
                    || forward > 245.0 * size + other.combatHalfWidth()) continue;
            double verticalGap = Math.abs(other.bodyCenterY() - centerY);
            if (verticalGap > 100.0 * size + other.combatHalfHeight()) continue;

            double falloff = 1.0 - Math.clamp(forward / Math.max(1.0, 270.0 * size), 0.0, 0.72);
            double sizeScale = bird.outgoingSizeKnockbackMultiplier()
                    * other.incomingSizeKnockbackMultiplier();
            double launchX = (7.2 + 5.8 * falloff) * sizeScale;
            double launchY = (2.0 + 2.2 * falloff) * sizeScale;
            other.vx = dir > 0
                    ? Math.max(other.vx, launchX)
                    : Math.min(other.vx, -launchX);
            other.vy = Math.min(other.vy, -launchY);
        }

        for (int i = 0; i < bird.scaledParticleCount(22); i++) {
            double spread = (bird.game.nextParticleRandom() - 0.5) * 1.0;
            double speed = 5.0 + bird.game.nextParticleRandom() * 8.0;
            double angle = (dir > 0 ? 0.0 : Math.PI) + spread;
            double originX = centerX + dir * (24.0 + bird.game.nextParticleRandom() * 18.0) * size;
            double originY = centerY + (bird.game.nextParticleRandom() - 0.5) * 42.0 * size;
            bird.game.particles.add(new Particle(
                    originX,
                    originY,
                    Math.cos(angle) * speed,
                    Math.sin(angle) * speed * 0.55,
                    Color.web("#E1F5FE").deriveColor(0, 1, 1, 0.68)
            ));
        }
        bird.game.playSwingSfx();
        bird.game.addToKillFeed(bird.shortName() + " released a blowback call!");
    }

    static void performCopiedNeutral(Bird bird, BirdGame3.BirdType source, boolean ultimate) {
        if (source == null || source == BirdGame3.BirdType.MOCKINGBIRD) {
            bird.mockingbirdCapturedType = null;
            bird.mockingbirdCopiedNeutralSource = null;
            return;
        }

        BirdGame3.BirdType originalType = bird.type;
        bird.type = source;
        try {
            switch (source) {
                case PIGEON -> PigeonSpecials.neutral(bird, ultimate);
                case EAGLE, FALCON -> RaptorSpecials.neutral(bird, ultimate);
                case PHOENIX -> PhoenixSpecials.neutral(bird, ultimate);
                case HUMMINGBIRD -> HummingbirdSpecials.neutral(bird, ultimate);
                case TURKEY -> TurkeySpecials.neutral(bird, ultimate);
                case ROOSTER -> copiedRoosterNeutral(bird, ultimate);
                case ROADRUNNER -> RoadrunnerSpecials.neutral(bird, ultimate);
                case PENGUIN -> PenguinSpecials.neutral(bird, ultimate);
                case SHOEBILL -> ShoebillSpecials.neutral(bird, ultimate);
                case RAZORBILL -> bird.specialRazorbillNeutral(ultimate);
                case GRINCHHAWK -> bird.specialGrinchhawkHeartSnatch(ultimate);
                case VULTURE -> bird.specialVultureCarrionCall(ultimate);
                case OPIUMBIRD -> bird.specialOpiumNeutral(ultimate);
                case HEISENBIRD -> bird.specialHeisenNeutral(ultimate);
                case TITMOUSE -> bird.specialTitmouseScoldChorus(ultimate);
                case BAT -> bird.specialBatNeutral(ultimate);
                case PELICAN -> bird.specialPelicanPouchSnare(ultimate);
                case RAVEN -> bird.fireRavenBlackQuillVolley(false, ultimate);
                case GOOSE -> GooseSpecials.neutral(bird, ultimate);
                case KIWI -> KiwiSpecials.copiedNeutral(bird, ultimate);
                case MOCKINGBIRD -> {
                }
            }
            bird.mockingbirdCopiedNeutralSource = source;
        } finally {
            bird.type = originalType;
        }
    }

    private static void copiedRoosterNeutral(Bird bird, boolean ultimate) {
        int before = RoosterSpecials.ownedCount(bird);
        int openSlots = Math.max(0, Bird.ROOSTER_MAX_CHICKS - before);
        int toSpawn = ultimate ? Math.max(1, openSlots) : Math.min(2, openSlots);
        int spawned = 0;
        for (int i = 0; i < toSpawn && RoosterSpecials.ownedCount(bird) < Bird.ROOSTER_MAX_CHICKS; i++) {
            ChickMinion chick = RoosterSpecials.spawnFollower(bird, RoosterSpecials.nextVariant(bird), ultimate, before + spawned);
            if (chick == null) {
                break;
            }
            launchCopiedRoosterChick(bird, chick, ultimate);
            spawned++;
        }

        bird.roosterNeutralReuseTimer = ultimate ? 18 : Bird.ROOSTER_NEUTRAL_REUSE_FRAMES;
        bird.roosterCommandFxTimer = Math.max(bird.roosterCommandFxTimer, spawned > 0 ? 34 : 16);
        bird.roosterCommandFxKind = 2;
        bird.specialCooldown = 0;
        bird.specialMaxCooldown = 0;
        bird.attackAnimationTimer = Math.max(bird.attackAnimationTimer, 12);
        if (spawned > 0) {
            bird.game.addToKillFeed(bird.shortName() + (ultimate
                    ? " copied Rooster and sent the royal brood hunting!"
                    : " copied Rooster and sent mimic chicks hunting!"));
        }
    }

    private static void launchCopiedRoosterChick(Bird bird, ChickMinion chick, boolean ultimate) {
        int dir = bird.horizontalInputDirection();
        if (dir == 0) {
            dir = bird.facingDirection();
        }
        bird.facingRight = dir > 0;
        chick.followingOwner = false;
        chick.target = RoosterSpecials.findThrowTarget(bird, chick, dir);
        chick.retargetCooldown = 0;
        chick.commandFlashFrames = ultimate ? 42 : 30;
        chick.thrownFrames = ultimate ? 34 : 28;
        chick.boostSparkFrames = Math.max(chick.boostSparkFrames, ultimate ? 28 : 20);
        chick.attackCooldown = Math.min(chick.attackCooldown, 6);
        chick.onGround = false;
        double launchDir = dir;
        if (chick.target != null) {
            double targetDir = Math.signum(chick.target.bodyCenterX() - (chick.x + chick.width * 0.5));
            if (targetDir != 0.0) {
                launchDir = targetDir;
            }
        }
        chick.vx = launchDir * (ultimate ? 24.0 : 19.0);
        chick.vy = ultimate ? -8.5 : -6.4;
        RoosterSpecials.emitCommandBurst(bird, chick.x + chick.width * 0.5, chick.y + chick.height * 0.5,
                ultimate ? Color.GOLD : Color.web("#D7B5FF"), ultimate ? 24 : 16);
    }

    static void side(Bird bird, boolean ultimate) {
        int dir = bird.horizontalInputDirection();
        if (dir == 0) {
            dir = bird.facingDirection();
        }
        bird.facingRight = dir > 0;
        bird.mockingbirdMicCharging = true;
        bird.mockingbirdMicChargeFrames = 0;
        bird.mockingbirdMicSwingTimer = 0;
        bird.mockingbirdMicDirection = dir;
        java.util.Arrays.fill(bird.mockingbirdMicHit, false);
        bird.mockingbirdSideFxTimer = 0;
        bird.specialCooldown = 0;
        bird.specialMaxCooldown = 0;
        bird.attackAnimationTimer = Math.max(bird.attackAnimationTimer, 5);
        bird.vx *= bird.isOnGround() ? 0.58 : 0.84;
    }

    static void handleState(Bird bird, boolean specialHeld) {
        if (bird.mockingbirdMicCharging) {
            bird.attackAnimationTimer = Math.max(bird.attackAnimationTimer, 5);
            bird.vx *= bird.isOnGround() ? 0.72 : 0.92;
            if (specialHeld && bird.mockingbirdMicChargeFrames < Bird.MOCKINGBIRD_MIC_MAX_CHARGE_FRAMES) {
                bird.mockingbirdMicChargeFrames++;
                if ((bird.mockingbirdMicChargeFrames & 7) == 0) {
                    emitMicrophoneChargeSpark(bird);
                }
            }
            if (!specialHeld || bird.mockingbirdMicChargeFrames >= Bird.MOCKINGBIRD_MIC_MAX_CHARGE_FRAMES) {
                releaseMicrophoneSwing(bird);
            }
            return;
        }

        if (bird.mockingbirdMicSwingTimer <= 0) {
            return;
        }

        bird.attackAnimationTimer = Math.max(bird.attackAnimationTimer, bird.mockingbirdMicSwingTimer);
        resolveMicrophoneSwingHit(bird);
        if (bird.mockingbirdMicSwingTimer == 1) {
            bird.mockingbirdMicSwingTimer = 0;
            bird.mockingbirdMicChargeFrames = 0;
            java.util.Arrays.fill(bird.mockingbirdMicHit, false);
        }
    }

    static void reset(Bird bird, boolean clearReuse) {
        bird.mockingbirdMicCharging = false;
        bird.mockingbirdMicChargeFrames = 0;
        bird.mockingbirdMicSwingTimer = 0;
        java.util.Arrays.fill(bird.mockingbirdMicHit, false);
        if (clearReuse) {
            bird.mockingbirdSideReuseTimer = 0;
        }
    }

    private static void releaseMicrophoneSwing(Bird bird) {
        bird.mockingbirdMicCharging = false;
        bird.mockingbirdMicChargeFrames = Math.max(Bird.MOCKINGBIRD_MIC_MIN_CHARGE_FRAMES,
                bird.mockingbirdMicChargeFrames);
        bird.mockingbirdMicSwingTimer = Bird.MOCKINGBIRD_MIC_SWING_FRAMES;
        bird.mockingbirdSideFxTimer = Bird.MOCKINGBIRD_MIC_SWING_FRAMES;
        bird.mockingbirdSideReuseTimer = Bird.MOCKINGBIRD_SIDE_REUSE_FRAMES
                + (int) Math.round(microphoneChargeRatio(bird) * 12.0);
        bird.attackAnimationTimer = Math.max(bird.attackAnimationTimer, Bird.MOCKINGBIRD_MIC_SWING_FRAMES);
        bird.vx = bird.vx * 0.50 - bird.mockingbirdMicDirection * (1.4 + microphoneChargeRatio(bird) * 1.8);
        if (!bird.isOnGround()) {
            bird.vy = Math.min(bird.vy, 1.0 - microphoneChargeRatio(bird) * 1.3);
        }
        emitMicrophoneReleaseBurst(bird);
    }

    static double microphoneChargeRatio(Bird bird) {
        return Math.clamp(bird.mockingbirdMicChargeFrames
                / (double) Bird.MOCKINGBIRD_MIC_MAX_CHARGE_FRAMES, 0.0, 1.0);
    }

    static double microphoneSwingAngle(Bird bird) {
        double elapsed = Bird.MOCKINGBIRD_MIC_SWING_FRAMES - bird.mockingbirdMicSwingTimer;
        double progress = Math.clamp(elapsed / Math.max(1.0, Bird.MOCKINGBIRD_MIC_SWING_FRAMES - 1.0), 0.0, 1.0);
        double eased = progress * progress * (3.0 - 2.0 * progress);
        double localAngle = Math.toRadians(-132.0 + eased * 324.0);
        return bird.mockingbirdMicDirection >= 0 ? localAngle : Math.PI - localAngle;
    }

    private static void resolveMicrophoneSwingHit(Bird bird) {
        double charge = microphoneChargeRatio(bird);
        double angle = microphoneSwingAngle(bird);
        double reach = (78.0 + charge * 54.0) * bird.sizeMultiplier;
        double micX = bird.bodyCenterX() + Math.cos(angle) * reach;
        double micY = bird.bodyCenterY() + Math.sin(angle) * reach * 0.72;
        double headRadius = (34.0 + charge * 13.0) * bird.sizeMultiplier;

        for (Bird other : bird.game.players) {
            if (!bird.canDamageTarget(other)) continue;
            if (other.playerIndex < 0 || other.playerIndex >= bird.mockingbirdMicHit.length) continue;
            if (bird.mockingbirdMicHit[other.playerIndex]) continue;
            double dx = other.bodyCenterX() - micX;
            double dy = other.bodyCenterY() - micY;
            if (Math.hypot(dx, dy) > headRadius + other.combatRadius()) continue;

            bird.mockingbirdMicHit[other.playerIndex] = true;
            double oldHealth = other.health;
            int dealt = (int) bird.applyDamageTo(other, 7.0 + charge * 8.0);
            if (dealt <= 0) continue;
            bird.game.damageDealt[bird.playerIndex] += dealt;
            bird.game.recordSpecialImpact(bird.playerIndex, dealt, true);
            if (other.health <= 0 && oldHealth > 0) {
                bird.game.eliminations[bird.playerIndex]++;
            }

            double outward = Math.signum(other.bodyCenterX() - bird.bodyCenterX());
            if (outward == 0.0) outward = bird.mockingbirdMicDirection;
            other.vx += outward * (7.4 + charge * 6.8);
            other.vy -= 3.8 + charge * 4.9;
            other.applyStun((int) Math.round(15.0 + charge * 15.0));
            bird.game.hitstopFrames = Math.max(bird.game.hitstopFrames, charge > 0.82 ? 6 : 3);
            bird.game.shakeIntensity = Math.max(bird.game.shakeIntensity, 4.0 + charge * 7.0);
        }
    }

    private static void emitMicrophoneChargeSpark(Bird bird) {
        double charge = microphoneChargeRatio(bird);
        double angle = Math.toRadians(-100.0 + charge * 34.0);
        double reach = (48.0 + charge * 28.0) * bird.sizeMultiplier;
        bird.game.particles.add(new Particle(
                bird.bodyCenterX() + Math.cos(angle) * reach * bird.facingDirection(),
                bird.bodyCenterY() + Math.sin(angle) * reach,
                (bird.game.nextParticleRandom() - 0.5) * 2.6,
                -1.0 - bird.game.nextParticleRandom() * 2.4,
                (charge > 0.76 ? Color.GOLD : Color.web("#D7B5FF")).deriveColor(0, 1, 1, 0.78)
        ));
    }

    private static void emitMicrophoneReleaseBurst(Bird bird) {
        double charge = microphoneChargeRatio(bird);
        Color accent = charge > 0.76 ? Color.GOLD : Color.web("#D7B5FF");
        for (int i = 0; i < bird.scaledParticleCount(12 + (int) Math.round(charge * 12.0)); i++) {
            double angle = bird.game.nextParticleRandom() * Math.PI * 2.0;
            double speed = 1.8 + bird.game.nextParticleRandom() * (3.8 + charge * 3.4);
            bird.game.particles.add(new Particle(
                    bird.bodyCenterX(), bird.bodyCenterY(),
                    Math.cos(angle) * speed,
                    Math.sin(angle) * speed,
                    accent.deriveColor(0, 1, 1, 0.74)
            ));
        }
    }

    static void up(Bird bird, boolean ultimate) {
        if (bird.mockingbirdUpSpecialUsed) {
            return;
        }
        int dir = bird.horizontalInputDirection();
        if (dir != 0) {
            bird.facingRight = dir > 0;
        }
        bird.mockingbirdUpSpecialUsed = true;
        bird.mockingbirdUpFxTimer = Bird.MOCKINGBIRD_UP_FX_FRAMES + (ultimate ? 8 : 0);
        bird.mockingbirdUpReuseTimer = ultimate ? 12 : Bird.MOCKINGBIRD_UP_REUSE_FRAMES;
        bird.attackAnimationTimer = Math.max(bird.attackAnimationTimer, bird.mockingbirdUpFxTimer);
        bird.specialCooldown = 0;
        bird.specialMaxCooldown = 0;
        bird.canDoubleJump = true;
        bird.vx = bird.vx * 0.48 + dir * (ultimate ? 4.8 : 3.4);
        bird.vy = ultimate ? -18.4 : -15.9;

        double centerX = bird.bodyCenterX();
        double centerY = bird.bodyCenterY() + 22.0 * bird.sizeMultiplier;
        for (Bird other : bird.game.players) {
            if (!bird.canDamageTarget(other)) continue;
            double dx = Math.abs(other.bodyCenterX() - centerX);
            double dy = Math.abs(other.bodyCenterY() - centerY);
            if (dx > (ultimate ? 74.0 : 58.0) * bird.sizeMultiplier + other.combatHalfWidth()) continue;
            if (dy > (ultimate ? 76.0 : 62.0) * bird.sizeMultiplier + other.combatHalfHeight()) continue;
            double oldHealth = other.health;
            int dealt = (int) bird.applyDamageTo(other, ultimate ? 8 : 5);
            if (dealt <= 0) continue;
            bird.game.damageDealt[bird.playerIndex] += dealt;
            bird.game.recordSpecialImpact(bird.playerIndex, dealt, true);
            if (other.health <= 0 && oldHealth > 0) {
                bird.game.eliminations[bird.playerIndex]++;
            }
            double pushDir = Math.signum(other.bodyCenterX() - centerX);
            if (pushDir == 0.0) {
                pushDir = bird.facingDirection();
            }
            other.vx += pushDir * (ultimate ? 5.8 : 4.0);
            other.vy -= ultimate ? 10.5 : 8.0;
        }

        Color leaf = ultimate ? Color.GOLD : Color.web("#66BB6A");
        for (int i = 0; i < bird.scaledParticleCount(36); i++) {
            double angle = -Math.PI / 2.0 + (bird.game.nextParticleRandom() - 0.5) * 1.8;
            double speed = 2.4 + bird.game.nextParticleRandom() * 6.2;
            bird.game.particles.add(new Particle(
                    bird.bodyCenterX() + (bird.game.nextParticleRandom() - 0.5) * 56.0 * bird.sizeMultiplier,
                    bird.bodyBottomY() - bird.game.nextParticleRandom() * 22.0 * bird.sizeMultiplier,
                    Math.cos(angle) * speed + dir * 0.7,
                    Math.sin(angle) * speed,
                    leaf.deriveColor(0, 0.85 + bird.game.nextParticleRandom() * 0.2, 0.86 + bird.game.nextParticleRandom() * 0.24, 0.72)
            ));
        }
    }

    static void down(Bird bird, boolean ultimate) {
        if (!ultimate && bird.mockingbirdLoungeReuseTimer > 0) {
            return;
        }
        boolean relocatingLivingLounge = bird.loungeActive && bird.loungeHealth > 0;
        int preservedHealth = bird.loungeHealth;
        boolean royalLounge = ultimate || (relocatingLivingLounge && bird.loungeRoyal);
        bird.loungeActive = true;
        bird.loungeX = bird.bodyCenterX();
        bird.loungeY = bird.bodyCenterY();
        bird.loungeMaxHealth = royalLounge ? 200 : Bird.LOUNGE_MAX_HEALTH;
        bird.loungeRoyal = royalLounge;
        bird.loungeHealth = relocatingLivingLounge
                ? Math.min(preservedHealth, bird.loungeMaxHealth)
                : bird.loungeMaxHealth;
        bird.mockingbirdUncaptureTimer = 0;
        bird.mockingbirdLoungeReuseTimer = ultimate ? 30 : Bird.MOCKINGBIRD_LOUNGE_REUSE_FRAMES;
        bird.specialCooldown = 0;
        bird.specialMaxCooldown = 0;
        bird.attackAnimationTimer = Math.max(bird.attackAnimationTimer, 12);
        Color leaf = ultimate ? Color.GOLD : Color.web("#2E7D32");
        for (int i = 0; i < bird.scaledParticleCount(42); i++) {
            double angle = bird.game.nextParticleRandom() * Math.PI * 2;
            double ring = 18.0 + bird.game.nextParticleRandom() * 62.0;
            bird.game.particles.add(new Particle(
                    bird.loungeX + Math.cos(angle) * ring,
                    bird.loungeY + Math.sin(angle) * ring * 0.58,
                    Math.cos(angle) * (0.6 + bird.game.nextParticleRandom() * 3.2),
                    Math.sin(angle) * (0.6 + bird.game.nextParticleRandom() * 2.4) - 1.8,
                    leaf.deriveColor(0, 0.85, 1.05, 0.70)
            ));
        }
        bird.game.addToKillFeed(bird.shortName() + (ultimate ? " raised the ROYAL FOREST!" : " moved the forest lounge!"));
    }

    static void shadowCourt(Bird bird) {
        ensureShadowCourtLounge(bird);
        BirdGame3.BirdType copiedType = shadowCourtType(bird);
        double[] offsets = {-118.0, 118.0, 0.0};
        for (int i = 0; i < offsets.length; i++) {
            double spawnX = bird.loungeX + offsets[i] * Math.max(0.85, bird.sizeMultiplier);
            double spawnY = bird.loungeY - (i == 2 ? 18.0 : 10.0) * Math.max(0.85, bird.sizeMultiplier);
            MockingbirdShadowMinion shadow = new MockingbirdShadowMinion(spawnX, spawnY, copiedType, bird, i);
            shadow.target = findClosestShadowTarget(bird, shadow);
            bird.game.mockingbirdShadowMinions.add(shadow);
            emitShadowBirth(bird, spawnX, spawnY, copiedType, i);
        }
        bird.mockingbirdBlowbackTimer = 0;
        bird.mockingbirdSideReuseTimer = Math.max(bird.mockingbirdSideReuseTimer, 18);
        bird.mockingbirdUpReuseTimer = Math.max(bird.mockingbirdUpReuseTimer, 18);
        bird.specialCooldown = 0;
        bird.specialMaxCooldown = 0;
        bird.attackAnimationTimer = Math.max(bird.attackAnimationTimer, 28);
        bird.loungeDamageFlash = Math.max(bird.loungeDamageFlash, 18);
        bird.game.addToKillFeed(bird.shortName() + " opened the Shadow Court of " + copiedType.name + "!");
        bird.game.shakeIntensity = Math.max(bird.game.shakeIntensity, 24);
        bird.game.hitstopFrames = Math.max(bird.game.hitstopFrames, 10);
    }

    private static void ensureShadowCourtLounge(Bird bird) {
        if (bird.loungeActive && bird.loungeHealth > 0) {
            bird.loungeRoyal = true;
            bird.loungeMaxHealth = Math.max(bird.loungeMaxHealth, 200);
            bird.loungeHealth = Math.max(bird.loungeHealth, Math.min(bird.loungeMaxHealth, 120));
            return;
        }
        bird.loungeActive = true;
        bird.loungeX = bird.bodyCenterX();
        bird.loungeY = bird.bodyCenterY();
        bird.loungeMaxHealth = 200;
        bird.loungeHealth = bird.loungeMaxHealth;
        bird.loungeRoyal = true;
        bird.mockingbirdUncaptureTimer = 0;
    }

    private static BirdGame3.BirdType shadowCourtType(Bird bird) {
        if (bird.mockingbirdCapturedType != null && bird.mockingbirdCapturedType != BirdGame3.BirdType.MOCKINGBIRD) {
            return bird.mockingbirdCapturedType;
        }
        Bird closest = findClosestBirdToCopy(bird);
        return closest == null ? BirdGame3.BirdType.PIGEON : closest.type;
    }

    private static Bird findClosestBirdToCopy(Bird bird) {
        Bird closest = null;
        double bestSq = Double.MAX_VALUE;
        double cx = bird.bodyCenterX();
        double cy = bird.bodyCenterY();
        for (Bird other : bird.game.players) {
            if (other == null || other == bird || other.health <= 0 || other.type == BirdGame3.BirdType.MOCKINGBIRD) {
                continue;
            }
            if (!bird.game.canDamage(bird, other)) {
                continue;
            }
            double dx = other.bodyCenterX() - cx;
            double dy = other.bodyCenterY() - cy;
            double distSq = dx * dx + dy * dy;
            if (distSq < bestSq) {
                bestSq = distSq;
                closest = other;
            }
        }
        if (closest != null) {
            return closest;
        }
        for (Bird other : bird.game.players) {
            if (other == null || other == bird || other.health <= 0 || other.type == BirdGame3.BirdType.MOCKINGBIRD) {
                continue;
            }
            double dx = other.bodyCenterX() - cx;
            double dy = other.bodyCenterY() - cy;
            double distSq = dx * dx + dy * dy;
            if (distSq < bestSq) {
                bestSq = distSq;
                closest = other;
            }
        }
        return closest;
    }

    private static Bird findClosestShadowTarget(Bird owner, MockingbirdShadowMinion shadow) {
        Bird closest = null;
        double bestSq = Double.MAX_VALUE;
        for (Bird other : owner.game.players) {
            if (other == null || other.health <= 0 || !owner.game.canDamage(owner, other)) {
                continue;
            }
            double dx = other.bodyCenterX() - shadow.bodyCenterX();
            double dy = other.bodyCenterY() - shadow.bodyCenterY();
            double distSq = dx * dx + dy * dy;
            if (distSq < bestSq) {
                bestSq = distSq;
                closest = other;
            }
        }
        return closest;
    }

    private static void emitShadowBirth(Bird bird, double x, double y, BirdGame3.BirdType copiedType, int slot) {
        Color accent = slot == 2 ? Color.web("#EC407A") : copiedType.color;
        for (int i = 0; i < bird.scaledParticleCount(24); i++) {
            double angle = bird.game.nextParticleRandom() * Math.PI * 2.0;
            double speed = 2.6 + bird.game.nextParticleRandom() * 8.8;
            bird.game.particles.add(new Particle(
                    x + Math.cos(angle) * (12.0 + bird.game.nextParticleRandom() * 28.0),
                    y + Math.sin(angle) * (10.0 + bird.game.nextParticleRandom() * 22.0),
                    Math.cos(angle) * speed,
                    Math.sin(angle) * speed - 2.2,
                    (bird.game.nextParticleRandom() < 0.55 ? Color.BLACK : accent).deriveColor(0, 1, 1, 0.76)
            ));
        }
    }
}
