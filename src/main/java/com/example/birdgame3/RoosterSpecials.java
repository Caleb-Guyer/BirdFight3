package com.example.birdgame3;

import javafx.scene.paint.Color;

import java.util.ArrayList;

final class RoosterSpecials {
    static final String DAWN_STAMPEDE_MOVE = "Rooster Dawn Stampede";

    private static final int DAWN_STAMPEDE_CHICK_COUNT = 18;
    private static final int DAWN_STAMPEDE_LARGE_FIGHT_CHICK_COUNT = 12;

    private RoosterSpecials() {
    }

    static void use(Bird bird, boolean ultimate) {
        ensureStartingChicks(bird);
        if (ultimate) {
            dawnStampede(bird);
            return;
        }
        switch (bird.selectRoosterSpecialVariant()) {
            case NEUTRAL -> neutral(bird, ultimate);
            case SIDE -> side(bird, ultimate);
            case UP -> up(bird, ultimate);
            case DOWN -> down(bird, ultimate);
        }
    }

    static boolean isActivePlayerSlot(Bird bird) {
        return bird.playerIndex >= 0
                && bird.playerIndex < bird.game.players.length
                && bird.game.players[bird.playerIndex] == bird;
    }

    static ArrayList<ChickMinion> ownedChicks(Bird bird) {
        ArrayList<ChickMinion> owned = new ArrayList<>();
        for (ChickMinion chick : bird.game.chickMinions) {
            if (chick.owner == bird && chick.life > 0) {
                owned.add(chick);
            }
        }
        return owned;
    }

    static int ownedCount(Bird bird) {
        int count = 0;
        for (ChickMinion chick : bird.game.chickMinions) {
            if (chick.owner == bird && chick.life > 0) {
                count++;
            }
        }
        return count;
    }

    static int nextVariant(Bird bird) {
        int[] counts = new int[Bird.ROOSTER_STARTING_CHICKS];
        for (ChickMinion chick : bird.game.chickMinions) {
            if (chick.owner == bird && chick.life > 0 && chick.variant >= 0 && chick.variant < counts.length) {
                counts[chick.variant]++;
            }
        }
        int bestVariant = 0;
        for (int i = 1; i < counts.length; i++) {
            if (counts[i] < counts[bestVariant]) {
                bestVariant = i;
            }
        }
        return bestVariant;
    }

    static ChickMinion spawnFollower(Bird bird, int variant, boolean ultimate, int slotHint) {
        if (ownedCount(bird) >= Bird.ROOSTER_MAX_CHICKS) {
            return null;
        }
        double s = bird.sizeMultiplier;
        int dir = bird.facingDirection();
        double centerX = bird.bodyCenterX() - dir * (46.0 + slotHint * 18.0) * s;
        double spawnY = bird.bodyBottomY() - (30.0 + (slotHint % 2) * 16.0) * s;
        ChickMinion chick = new ChickMinion(centerX, spawnY, Math.floorMod(variant, Bird.ROOSTER_STARTING_CHICKS), ultimate, bird);
        chick.x -= chick.width * 0.5;
        chick.followingOwner = true;
        chick.target = null;
        chick.commandFlashFrames = ultimate ? 42 : 30;
        chick.boostSparkFrames = Math.max(chick.boostSparkFrames, 14);
        chick.maxAge = Math.max(chick.maxAge, 18000);
        chick.onGround = bird.isOnGround();
        chick.vx = -dir * (1.2 + slotHint * 0.45);
        chick.vy = -4.5 - slotHint;
        bird.game.chickMinions.add(chick);
        emitCommandBurst(bird, chick.x + chick.width * 0.5, chick.y + chick.height * 0.5,
                ultimate ? Color.GOLD : chickColor(chick.variant), ultimate ? 22 : 14);
        return chick;
    }

    static void ensureStartingChicks(Bird bird) {
        if (bird.type != BirdGame3.BirdType.ROOSTER
                || bird.roosterInitialChicksSpawned
                || bird.health <= 0
                || !isActivePlayerSlot(bird)) {
            return;
        }
        bird.roosterInitialChicksSpawned = true;
        boolean[] hasVariant = new boolean[Bird.ROOSTER_STARTING_CHICKS];
        int owned = 0;
        for (ChickMinion chick : bird.game.chickMinions) {
            if (chick.owner != bird || chick.life <= 0) continue;
            owned++;
            if (chick.variant >= 0 && chick.variant < hasVariant.length) {
                hasVariant[chick.variant] = true;
            }
        }
        for (int variant = 0; variant < Bird.ROOSTER_STARTING_CHICKS && owned < Bird.ROOSTER_STARTING_CHICKS; variant++) {
            if (hasVariant[variant]) continue;
            if (spawnFollower(bird, variant, false, owned) != null) {
                owned++;
            }
        }
    }

    static Color chickColor(int variant) {
        return switch (variant) {
            case 1 -> Color.web("#4FC3F7");
            case 2 -> Color.web("#8D6E63");
            default -> Color.web("#FFD54F");
        };
    }

    static void emitCommandBurst(Bird bird, double centerX, double centerY, Color color, int baseCount) {
        int particleCount = bird.scaledParticleCount(baseCount);
        for (int i = 0; i < particleCount; i++) {
            double angle = SimRng.next() * Math.PI * 2.0;
            double speed = 2.0 + SimRng.next() * 5.8;
            bird.game.particles.add(new Particle(
                    centerX,
                    centerY,
                    Math.cos(angle) * speed,
                    Math.sin(angle) * speed - 2.2,
                    color.deriveColor(0, 1, 1, 0.78)
            ));
        }
    }

    static void neutral(Bird bird, boolean ultimate) {
        int before = ownedCount(bird);
        int toSpawn = ultimate ? Math.max(1, Bird.ROOSTER_MAX_CHICKS - before) : 1;
        int spawned = 0;
        for (int i = 0; i < toSpawn && ownedCount(bird) < Bird.ROOSTER_MAX_CHICKS; i++) {
            if (spawnFollower(bird, nextVariant(bird), ultimate, before + spawned) != null) {
                spawned++;
            }
        }

        bird.roosterNeutralReuseTimer = ultimate ? 22 : Bird.ROOSTER_NEUTRAL_REUSE_FRAMES;
        bird.roosterCommandFxTimer = Math.max(bird.roosterCommandFxTimer, spawned > 0 ? 34 : 16);
        bird.roosterCommandFxKind = 1;
        bird.specialCooldown = 0;
        bird.specialMaxCooldown = 0;
        bird.attackAnimationTimer = Math.max(bird.attackAnimationTimer, 12);
        if (spawned > 0) {
            bird.game.addToKillFeed(bird.shortName() + (ultimate ? " assembled the royal brood!" : " called another chick into formation!"));
        }
    }

    static void dawnStampede(Bird bird) {
        Bird target = nearestEnemy(bird, bird.bodyCenterX(), bird.bodyCenterY());
        int swarmCount = bird.game.activePlayers >= 6
                ? DAWN_STAMPEDE_LARGE_FIGHT_CHICK_COUNT
                : DAWN_STAMPEDE_CHICK_COUNT;
        bird.game.chickMinions.removeIf(chick -> chick.owner == bird && chick.roosterSwarm);

        int dir = bird.facingDirection();
        double centerX = bird.bodyCenterX();
        double centerY = bird.bodyCenterY();
        int spawned = 0;
        for (int i = 0; i < swarmCount; i++) {
            int variant = Math.floorMod(i + nextVariant(bird), Bird.ROOSTER_STARTING_CHICKS);
            double side = i % 2 == 0 ? -1.0 : 1.0;
            double lane = i / 2.0;
            double spreadX = side * (120.0 + lane * 18.0 + SimRng.next() * 90.0);
            double spreadY = -95.0 - (i % 5) * 18.0 - SimRng.next() * 70.0;
            ChickMinion chick = new ChickMinion(centerX + spreadX, centerY + spreadY, variant, true, bird);
            configureStampedeChick(chick, target, dir, i);
            bird.game.chickMinions.add(chick);
            spawned++;
            if (i % 3 == 0) {
                emitCommandBurst(bird, chick.x + chick.width * 0.5, chick.y + chick.height * 0.5,
                        Color.web("#FFF176"), 7);
            }
        }

        bird.roosterCommandFxTimer = Math.max(bird.roosterCommandFxTimer, 68);
        bird.roosterCommandFxKind = 5;
        bird.specialCooldown = 0;
        bird.specialMaxCooldown = 0;
        bird.attackAnimationTimer = Math.max(bird.attackAnimationTimer, 36);
        bird.roosterNeutralReuseTimer = Math.max(bird.roosterNeutralReuseTimer, 20);
        bird.roosterSideReuseTimer = Math.max(bird.roosterSideReuseTimer, 18);
        bird.roosterDownReuseTimer = Math.max(bird.roosterDownReuseTimer, 24);
        bird.vx -= dir * 4.0;
        bird.vy = Math.min(bird.vy, -7.5);
        bird.game.shakeIntensity = Math.max(bird.game.shakeIntensity, 20);
        bird.game.triggerFlash(0.55, false);
        bird.game.addToKillFeed(bird.shortName() + " unleashed DAWN STAMPEDE! " + spawned + " chicks are airborne!");
        emitCommandBurst(bird, centerX, centerY, Color.web("#FFD54F"), 62);
    }

    private static void configureStampedeChick(ChickMinion chick, Bird target, int dir, int index) {
        chick.x -= chick.width * 0.5;
        chick.y -= chick.height * 0.5;
        chick.roosterSwarm = true;
        chick.followingOwner = false;
        chick.target = target;
        chick.age = 0;
        chick.maxAge = 190 + (index % 5) * 10;
        chick.retargetCooldown = index % 6;
        chick.commandFlashFrames = 68;
        chick.thrownFrames = 62;
        chick.boostSparkFrames = 78;
        chick.attackCooldown = index % 8;
        chick.jumpCooldown = 0;
        chick.onGround = false;
        chick.speed = Math.max(chick.speed, 14.4 + (index % 3) * 0.8);
        chick.accel = Math.max(chick.accel, 0.48);
        chick.jumpStrength = Math.max(chick.jumpStrength, 18.0);
        chick.damage = 2;
        chick.life = Math.max(chick.life, 2);
        chick.swarmHitsRemaining = 2;
        chick.swarmVisualCopies = 4 + index % 3;
        chick.vx = dir * (7.2 + (index % 5) * 0.8) + (SimRng.next() - 0.5) * 4.0;
        chick.vy = -8.8 - SimRng.next() * 5.6 - (index % 4) * 0.7;
    }

    private static Bird nearestEnemy(Bird bird, double x, double y) {
        Bird best = null;
        double bestSq = Double.MAX_VALUE;
        for (Bird candidate : bird.game.players) {
            if (candidate == null || candidate.health <= 0 || !bird.game.canDamage(bird, candidate)) {
                continue;
            }
            double dx = candidate.bodyCenterX() - x;
            double dy = candidate.bodyCenterY() - y;
            double distSq = dx * dx + dy * dy;
            if (distSq < bestSq) {
                bestSq = distSq;
                best = candidate;
            }
        }
        return best;
    }

    static ChickMinion nextFollower(Bird bird) {
        for (ChickMinion chick : bird.game.chickMinions) {
            if (chick.owner == bird && chick.life > 0 && chick.followingOwner) {
                return chick;
            }
        }
        return null;
    }

    static Bird findThrowTarget(Bird bird, ChickMinion chick, int dir) {
        Bird best = null;
        double bestScore = Double.MAX_VALUE;
        double cx = chick.x + chick.width * 0.5;
        double cy = chick.y + chick.height * 0.5;
        for (Bird candidate : bird.game.players) {
            if (candidate == null || candidate.health <= 0 || !bird.game.canDamage(bird, candidate)) continue;
            double dx = candidate.bodyCenterX() - cx;
            double dy = candidate.bodyCenterY() - cy;
            double forwardPenalty = dx * dir < -30.0 ? 900.0 : 0.0;
            double score = Math.hypot(dx, dy) + Math.abs(dy) * 0.25 + forwardPenalty;
            if (score < bestScore) {
                bestScore = score;
                best = candidate;
            }
        }
        return best;
    }

    static void side(Bird bird, boolean ultimate) {
        ChickMinion chick = nextFollower(bird);
        int dir = bird.horizontalInputDirection();
        if (dir == 0) {
            dir = bird.facingDirection();
        }
        bird.facingRight = dir > 0;
        bird.specialCooldown = 0;
        bird.specialMaxCooldown = 0;

        if (chick == null) {
            bird.roosterSideReuseTimer = 10;
            bird.roosterCommandFxTimer = Math.max(bird.roosterCommandFxTimer, 12);
            bird.roosterCommandFxKind = 2;
            return;
        }

        chick.followingOwner = false;
        chick.target = findThrowTarget(bird, chick, dir);
        chick.retargetCooldown = 0;
        chick.commandFlashFrames = ultimate ? 42 : 30;
        chick.thrownFrames = ultimate ? 34 : 26;
        chick.attackCooldown = Math.min(chick.attackCooldown, 8);
        chick.onGround = false;
        chick.vx = dir * (ultimate ? 27.0 : 22.0);
        chick.vy = ultimate ? -9.0 : -7.0;
        chick.maxAge = Math.max(chick.maxAge, 18000);

        bird.roosterSideReuseTimer = ultimate ? 12 : Bird.ROOSTER_SIDE_REUSE_FRAMES;
        bird.roosterCommandFxTimer = Math.max(bird.roosterCommandFxTimer, 26);
        bird.roosterCommandFxKind = 2;
        bird.attackAnimationTimer = Math.max(bird.attackAnimationTimer, 14);
        bird.vx -= dir * 2.2;
        emitCommandBurst(bird, chick.x + chick.width * 0.5, chick.y + chick.height * 0.5,
                ultimate ? Color.GOLD : Color.web("#FF7043"), ultimate ? 28 : 18);
    }

    static void up(Bird bird, boolean ultimate) {
        if (bird.roosterUpSpecialUsed && !ultimate) {
            return;
        }
        ArrayList<ChickMinion> chicks = ownedChicks(bird);
        if (chicks.isEmpty()) {
            spawnFollower(bird, nextVariant(bird), ultimate, 0);
            chicks = ownedChicks(bird);
        }

        bird.roosterUpSpecialUsed = true;
        bird.specialCooldown = 0;
        bird.specialMaxCooldown = 0;
        int count = Math.max(1, chicks.size());
        bird.vy = Math.min(bird.vy, -(ultimate ? 19.5 : 15.5) - Math.min(5, count) * (ultimate ? 2.8 : 2.15));
        bird.vx *= 0.36;
        bird.canDoubleJump = true;
        bird.attackAnimationTimer = Math.max(bird.attackAnimationTimer, 18);
        bird.roosterCommandFxTimer = Math.max(bird.roosterCommandFxTimer, 38);
        bird.roosterCommandFxKind = 3;

        double centerX = bird.bodyCenterX();
        double baseY = bird.bodyBottomY() - 10.0 * bird.sizeMultiplier;
        for (int i = 0; i < chicks.size(); i++) {
            ChickMinion chick = chicks.get(i);
            double fan = i - (chicks.size() - 1) / 2.0;
            chick.followingOwner = true;
            chick.target = null;
            chick.commandFlashFrames = ultimate ? 44 : 32;
            chick.boostSparkFrames = ultimate ? 46 : 36;
            chick.thrownFrames = 0;
            chick.x = centerX - chick.width * 0.5 + fan * 23.0 * bird.sizeMultiplier;
            chick.y = baseY - chick.height - Math.abs(fan) * 7.0 * bird.sizeMultiplier;
            chick.vx = fan * (ultimate ? 3.2 : 2.5);
            chick.vy = -(ultimate ? 18.0 : 14.5) - i * 0.8;
            chick.onGround = false;
            emitCommandBurst(bird, chick.x + chick.width * 0.5, chick.y + chick.height * 0.5,
                    ultimate ? Color.GOLD : Color.web("#FFF59D"), 8);
        }
        bird.game.shakeIntensity = Math.max(bird.game.shakeIntensity, ultimate ? 9 : 6);
    }

    static void down(Bird bird, boolean ultimate) {
        ArrayList<ChickMinion> chicks = ownedChicks(bird);
        int dir = bird.facingDirection();
        double s = bird.sizeMultiplier;
        for (int i = 0; i < chicks.size(); i++) {
            ChickMinion chick = chicks.get(i);
            double side = dir > 0 ? -1.0 : 1.0;
            double row = i % 2 == 0 ? 0.0 : -18.0 * s;
            chick.followingOwner = true;
            chick.target = null;
            chick.retargetCooldown = ultimate ? 28 : 18;
            chick.commandFlashFrames = ultimate ? 44 : 32;
            chick.thrownFrames = 0;
            chick.boostSparkFrames = Math.max(chick.boostSparkFrames, ultimate ? 24 : 16);
            chick.x = bird.bodyCenterX() + side * (54.0 + i * 18.0) * s - chick.width * 0.5;
            chick.y = bird.bodyBottomY() - chick.height - 4.0 * s + row;
            chick.vx = side * (1.0 + i * 0.25);
            chick.vy = -5.0 - i * 0.35;
            chick.onGround = bird.isOnGround();
            emitCommandBurst(bird, chick.x + chick.width * 0.5, chick.y + chick.height * 0.5,
                    ultimate ? Color.GOLD : Color.web("#FFF176"), 10);
        }
        bird.roosterDownReuseTimer = ultimate ? 18 : Bird.ROOSTER_DOWN_REUSE_FRAMES;
        bird.roosterCommandFxTimer = Math.max(bird.roosterCommandFxTimer, ultimate ? 42 : 32);
        bird.roosterCommandFxKind = 4;
        bird.specialCooldown = 0;
        bird.specialMaxCooldown = 0;
        bird.attackAnimationTimer = Math.max(bird.attackAnimationTimer, 12);
    }

    static boolean ready(Bird bird, Bird.RoosterSpecialVariant variant) {
        boolean ultimateReady = bird.isUltimateReady();
        return switch (variant) {
            case NEUTRAL -> ultimateReady || (bird.roosterNeutralReuseTimer <= 0 && ownedCount(bird) < Bird.ROOSTER_MAX_CHICKS);
            case SIDE -> ultimateReady || (bird.roosterSideReuseTimer <= 0 && nextFollower(bird) != null);
            case UP -> ultimateReady || !bird.roosterUpSpecialUsed;
            case DOWN -> ultimateReady || bird.roosterDownReuseTimer <= 0;
        };
    }

    static boolean canConvertShieldIntoDown(Bird bird, Bird.RoosterSpecialVariant variant) {
        return variant == Bird.RoosterSpecialVariant.DOWN
                && bird.isBlocking
                && bird.shieldStunFrames <= 0;
    }

    static boolean canStart(Bird bird, boolean grabbed, boolean dodging) {
        ensureStartingChicks(bird);
        Bird.RoosterSpecialVariant variant = bird.selectRoosterSpecialVariant();
        boolean shieldConversion = canConvertShieldIntoDown(bird, variant);
        return bird.type == BirdGame3.BirdType.ROOSTER
                && bird.health > 0
                && bird.stunTime <= 0.0
                && !grabbed
                && (!bird.isBlocking || shieldConversion)
                && !dodging
                && ready(bird, variant);
    }
}
