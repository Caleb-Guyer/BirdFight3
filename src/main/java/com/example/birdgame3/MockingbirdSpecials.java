package com.example.birdgame3;

import javafx.scene.paint.Color;

final class MockingbirdSpecials {
    private MockingbirdSpecials() {
    }

    static void use(Bird bird, boolean ultimate) {
        switch (bird.selectMockingbirdSpecialVariant()) {
            case NEUTRAL -> neutral(bird, ultimate);
            case SIDE -> side(bird, ultimate);
            case UP -> up(bird, ultimate);
            case DOWN -> down(bird, ultimate);
        }
    }

    static void neutral(Bird bird, boolean ultimate) {
        if (bird.mockingbirdCapturedType == null) {
            bird.mockingbirdQuestionTimer = Bird.MOCKINGBIRD_QUESTION_FRAMES;
            bird.attackAnimationTimer = Math.max(bird.attackAnimationTimer, 14);
            bird.specialCooldown = 18;
            bird.specialMaxCooldown = 18;
            bird.vx *= 0.72;
            for (int i = 0; i < bird.scaledParticleCount(12); i++) {
                double angle = -Math.PI / 2.0 + (SimRng.next() - 0.5) * 1.4;
                double speed = 1.4 + SimRng.next() * 3.2;
                bird.game.particles.add(new Particle(
                        bird.bodyCenterX() + (SimRng.next() - 0.5) * 24.0 * bird.sizeMultiplier,
                        bird.bodyCenterY() - 34.0 * bird.sizeMultiplier,
                        Math.cos(angle) * speed,
                        Math.sin(angle) * speed,
                        Color.WHITE.deriveColor(0, 1, 1, 0.72)
                ));
            }
            return;
        }

        performCopiedNeutral(bird, bird.mockingbirdCapturedType, ultimate);
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
        bird.mockingbirdSideFxTimer = Bird.MOCKINGBIRD_SIDE_FX_FRAMES + (ultimate ? 5 : 0);
        bird.mockingbirdSideReuseTimer = ultimate ? 10 : Bird.MOCKINGBIRD_SIDE_REUSE_FRAMES;
        bird.attackAnimationTimer = Math.max(bird.attackAnimationTimer, bird.mockingbirdSideFxTimer);
        bird.specialCooldown = 0;
        bird.specialMaxCooldown = 0;
        bird.vx = bird.vx * 0.45 - dir * (ultimate ? 3.2 : 2.2);
        if (!bird.isOnGround()) {
            bird.vy = Math.min(bird.vy, ultimate ? 0.4 : 0.9);
        }

        double centerX = bird.bodyCenterX() + dir * 74.0 * bird.sizeMultiplier;
        double centerY = bird.bodyCenterY() - 6.0 * bird.sizeMultiplier;
        double reach = (ultimate ? 164.0 : 142.0) * bird.sizeMultiplier;
        double verticalReach = (ultimate ? 62.0 : 52.0) * bird.sizeMultiplier;
        for (Bird other : bird.game.players) {
            if (!bird.canDamageTarget(other)) continue;
            double dx = other.bodyCenterX() - centerX;
            double forward = dx * dir;
            if (forward < -other.combatHalfWidth() * 0.45 || forward > reach + other.combatHalfWidth()) continue;
            double dy = Math.abs(other.bodyCenterY() - centerY);
            if (dy > verticalReach + other.combatHalfHeight()) continue;

            double oldHealth = other.health;
            int dealt = (int) bird.applyDamageTo(other, ultimate ? 11 : 8);
            if (dealt <= 0) continue;
            bird.game.damageDealt[bird.playerIndex] += dealt;
            bird.game.recordSpecialImpact(bird.playerIndex, dealt, true);
            if (other.health <= 0 && oldHealth > 0) {
                bird.game.eliminations[bird.playerIndex]++;
            }
            other.vx += dir * (ultimate ? 10.8 : 8.6);
            other.vy -= ultimate ? 5.6 : 4.2;
            other.applyStun(ultimate ? 28 : 18);
        }

        Color pulse = ultimate ? Color.GOLD : Color.web("#D7B5FF");
        for (int ring = 0; ring < 3; ring++) {
            for (int i = 0; i < bird.scaledParticleCount(7); i++) {
                double spread = (SimRng.next() - 0.5) * (28.0 + ring * 14.0);
                bird.game.particles.add(new Particle(
                        bird.bodyCenterX() + dir * (26.0 + ring * 30.0 + SimRng.next() * 22.0),
                        centerY + spread,
                        dir * (2.6 + ring * 0.8 + SimRng.next() * 2.4),
                        spread * 0.045,
                        pulse.deriveColor(0, 1, 1, 0.68)
                ));
            }
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
            double angle = -Math.PI / 2.0 + (SimRng.next() - 0.5) * 1.8;
            double speed = 2.4 + SimRng.next() * 6.2;
            bird.game.particles.add(new Particle(
                    bird.bodyCenterX() + (SimRng.next() - 0.5) * 56.0 * bird.sizeMultiplier,
                    bird.bodyBottomY() - SimRng.next() * 22.0 * bird.sizeMultiplier,
                    Math.cos(angle) * speed + dir * 0.7,
                    Math.sin(angle) * speed,
                    leaf.deriveColor(0, 0.85 + SimRng.next() * 0.2, 0.86 + SimRng.next() * 0.24, 0.72)
            ));
        }
    }

    static void down(Bird bird, boolean ultimate) {
        bird.loungeActive = true;
        bird.loungeX = bird.x + 40;
        bird.loungeY = bird.y + 40;
        bird.loungeMaxHealth = ultimate ? 200 : Bird.LOUNGE_MAX_HEALTH;
        bird.loungeRoyal = ultimate;
        bird.loungeHealth = bird.loungeMaxHealth;
        bird.mockingbirdUncaptureTimer = 0;
        bird.specialCooldown = 0;
        bird.specialMaxCooldown = 0;
        bird.attackAnimationTimer = Math.max(bird.attackAnimationTimer, 12);
        Color leaf = ultimate ? Color.GOLD : Color.web("#2E7D32");
        for (int i = 0; i < bird.scaledParticleCount(42); i++) {
            double angle = SimRng.next() * Math.PI * 2;
            double ring = 18.0 + SimRng.next() * 62.0;
            bird.game.particles.add(new Particle(
                    bird.loungeX + Math.cos(angle) * ring,
                    bird.loungeY + Math.sin(angle) * ring * 0.58,
                    Math.cos(angle) * (0.6 + SimRng.next() * 3.2),
                    Math.sin(angle) * (0.6 + SimRng.next() * 2.4) - 1.8,
                    leaf.deriveColor(0, 0.85, 1.05, 0.70)
            ));
        }
        bird.game.addToKillFeed(bird.shortName() + (ultimate ? " raised the ROYAL FOREST!" : " moved the forest lounge!"));
    }
}
