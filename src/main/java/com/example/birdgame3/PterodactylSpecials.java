package com.example.birdgame3;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Arrays;

/** Tick-driven aerial grappler. All input comes through Bird's replay/lockstep gates. */
final class PterodactylSpecials {
    static final int IDLE = 0, GUST = 1, LUNGE = 2, UPDRAFT = 3,
            WINDUP = 4, DIVE = 5, CARRY = 6, SLAM = 7, RECOVERY = 8;
    static final String EXTINCTION_DIVE = "Pterodactyl Extinction Dive";

    static final class State {
        int phase, elapsed, direction = 1;
        long hitMask;
        boolean upUsed, ultimate, groundLaunch;
        double carryStartY;
        final int[] cooldowns = new int[4];

        void copyFrom(State source) {
            phase = source.phase; elapsed = source.elapsed; direction = source.direction;
            hitMask = source.hitMask; upUsed = source.upUsed; ultimate = source.ultimate; groundLaunch = source.groundLaunch;
            carryStartY = source.carryStartY;
            System.arraycopy(source.cooldowns, 0, cooldowns, 0, cooldowns.length);
        }
        void write(DataOutputStream out) throws IOException {
            out.writeInt(phase); out.writeInt(elapsed); out.writeInt(direction); out.writeLong(hitMask);
            out.writeBoolean(upUsed); out.writeBoolean(ultimate); out.writeBoolean(groundLaunch); out.writeDouble(carryStartY);
            for (int cooldown : cooldowns) out.writeInt(cooldown);
        }
        void read(DataInputStream in) throws IOException {
            phase = in.readInt(); elapsed = in.readInt(); direction = in.readInt(); hitMask = in.readLong();
            upUsed = in.readBoolean(); ultimate = in.readBoolean(); groundLaunch = in.readBoolean(); carryStartY = in.readDouble();
            for (int i = 0; i < cooldowns.length; i++) cooldowns[i] = in.readInt();
            if (phase < IDLE || phase > RECOVERY || elapsed < 0 || elapsed > 600
                    || Math.abs(direction) != 1 || !Double.isFinite(carryStartY)
                    || Arrays.stream(cooldowns).anyMatch(value -> value < 0 || value > 600)) {
                throw new IOException("Invalid Pterodactyl state");
            }
        }
        long hash() {
            long hash = phase * 31L + elapsed;
            hash = hash * 31 + direction; hash = hash * 31 + hitMask;
            hash = hash * 31 + (upUsed ? 1 : 0); hash = hash * 31 + (ultimate ? 1 : 0);
            hash = hash * 31 + (groundLaunch ? 1 : 0);
            hash = hash * 31 + Double.doubleToLongBits(carryStartY);
            for (int cooldown : cooldowns) hash = hash * 31 + cooldown;
            return hash;
        }
    }

    private PterodactylSpecials() { }

    static boolean active(Bird bird) { return bird.pterodactyl.phase != IDLE; }
    static boolean carrying(Bird bird) {
        return bird.type == BirdGame3.BirdType.PTERODACTYL
                && (bird.pterodactyl.phase == CARRY || bird.pterodactyl.phase == SLAM);
    }
    static boolean canStart(Bird bird) {
        if (!bird.pterodactylCanAct() || active(bird)) return false;
        Bird.DirectionalSpecialInput input = bird.selectDirectionalSpecialInput();
        if (input == Bird.DirectionalSpecialInput.NEUTRAL && bird.isUltimateReady()) return true;
        return bird.pterodactyl.cooldowns[input.ordinal()] <= 0
                && (input != Bird.DirectionalSpecialInput.UP || !bird.pterodactyl.upUsed);
    }
    static void cooldowns(Bird bird, int ticks) {
        if (bird.type != BirdGame3.BirdType.PTERODACTYL) return;
        for (int i = 0; i < 4; i++) bird.pterodactyl.cooldowns[i] = Math.max(0, bird.pterodactyl.cooldowns[i] - ticks);
        if (bird.isOnGround()) bird.pterodactyl.upUsed = false;
    }
    static void use(Bird bird, boolean ultimate) {
        State state = bird.pterodactyl;
        Bird.DirectionalSpecialInput input = bird.selectDirectionalSpecialInput();
        state.direction = bird.horizontalInputDirection();
        if (state.direction == 0) state.direction = bird.facingDirection();
        bird.facingRight = state.direction > 0;
        state.ultimate = ultimate; state.elapsed = 0; state.hitMask = 0;
        state.groundLaunch = bird.isOnGround();
        bird.specialCooldown = 0;
        bird.specialMaxCooldown = 0;
        bird.isBlocking = false;
        if (ultimate) {
            state.phase = WINDUP;
        } else {
            state.phase = switch (input) {
                case NEUTRAL -> GUST;
                case SIDE -> LUNGE;
                case UP -> UPDRAFT;
                case DOWN -> WINDUP;
            };
            state.cooldowns[input.ordinal()] = switch (input) {
                case NEUTRAL -> 70;
                case SIDE -> 80;
                case UP -> 48;
                case DOWN -> 125;
            };
            if (input == Bird.DirectionalSpecialInput.UP) state.upUsed = true;
        }
        bird.attackAnimationTimer = Math.max(bird.attackAnimationTimer, 12);
        if (state.phase == WINDUP && bird.isOnGround()) bird.vy = -10;
    }
    static void reset(Bird bird, boolean full) {
        if (carrying(bird)) bird.pterodactylRelease(false, false);
        State state = bird.pterodactyl;
        state.phase = IDLE; state.elapsed = 0; state.hitMask = 0; state.ultimate = false; state.groundLaunch = false;
        state.carryStartY = 0;
        if (full) { state.upUsed = false; Arrays.fill(state.cooldowns, 0); }
    }
    private static void recover(Bird bird) {
        bird.pterodactyl.phase = RECOVERY; bird.pterodactyl.elapsed = 0;
        bird.pterodactyl.ultimate = false;
    }

    static void tick(Bird bird, boolean specialHeld) {
        if ((bird.type != BirdGame3.BirdType.PTERODACTYL && !bird.mockingbirdCopiedNeutralFrom(BirdGame3.BirdType.PTERODACTYL))
                || !active(bird)) return;
        State state = bird.pterodactyl;
        if (bird.health <= 0 || bird.stunTime > 0) { reset(bird, false); return; }
        state.elapsed++;
        bird.attackAnimationTimer = Math.max(bird.attackAnimationTimer, 2);
        switch (state.phase) {
            case GUST -> {
                bird.vx *= 0.80;
                if (state.elapsed == 8) hitArea(bird, 240, 84, 6, 17, -4, 11);
                if (state.elapsed >= 23) recover(bird);
            }
            case LUNGE -> {
                bird.vx = state.elapsed <= 6 ? 0 : state.direction * 15;
                bird.vy *= 0.75;
                if (state.elapsed > 6) hitArea(bird, 120, 54, 12, 14, -7, 15);
                if (state.elapsed >= 18) recover(bird);
            }
            case UPDRAFT -> {
                bird.vy = state.elapsed <= 17 ? -15.0 : -4.0;
                bird.vx = state.direction * 3.5;
                hitArea(bird, 72, 94, 7, 5, -13, 12);
                if (state.elapsed >= 23) recover(bird);
            }
            case WINDUP -> {
                bird.vx *= 0.75;
                bird.vy = state.groundLaunch ? -12 : -4;
                if (state.elapsed >= (state.ultimate ? 8 : 11)) {
                    state.phase = DIVE; state.elapsed = 0;
                }
            }
            case DIVE -> {
                bird.vx = state.direction * (state.ultimate ? 11 : 9);
                bird.vy = state.ultimate ? 20 : 16;
                if (state.elapsed >= 25) recover(bird);
            }
            case CARRY -> {
                Bird victim = bird.pterodactylTarget();
                if (victim == null) { recover(bird); break; }
                bird.vy = bird.y > state.carryStartY - 175 ? -6.0 : 0;
                bird.vx = state.direction * 2.2;
                if (!bird.pterodactylAdvanceHold()) { recover(bird); break; }
                if (state.ultimate && state.elapsed >= 12) {
                    state.phase = SLAM; state.elapsed = 0;
                } else if (!state.ultimate && (state.elapsed >= 28 || (state.elapsed >= 6 && !specialHeld))) {
                    bird.pterodactylRelease(true, false); recover(bird);
                }
            }
            case SLAM -> {
                if (bird.pterodactylTarget() == null || !bird.pterodactylAdvanceHold()) { recover(bird); break; }
                bird.vx = state.direction * 1.8; bird.vy = 26;
                if (state.elapsed >= 32) { bird.pterodactylRelease(false, false); recover(bird); }
            }
            case RECOVERY -> {
                bird.vx *= 0.85;
                if (state.elapsed >= 14) reset(bird, false);
            }
            default -> { }
        }
    }

    static void postMove(Bird bird, double previousX, double previousY) {
        if (bird.type != BirdGame3.BirdType.PTERODACTYL) return;
        State state = bird.pterodactyl;
        if (state.phase == DIVE) {
            Bird best = null; double bestDistance = Double.POSITIVE_INFINITY;
            // Test relative swept motion as well as end overlap to avoid passing
            // through a target at dive speed. Player order breaks equal-distance ties.
            double dx = bird.x - previousX, dy = bird.y - previousY;
            for (Bird target : bird.game.players) {
                if (!bird.pterodactylCanGrab(target)) continue;
                double rx = target.bodyCenterX() - (previousX + bird.bodyWidth() / 2);
                double ry = target.bodyCenterY() - (previousY + bird.bodyHeight() + 10);
                double length = dx * dx + dy * dy;
                double t = length > 0 ? Math.clamp((rx * dx + ry * dy) / length, 0.0, 1.0) : 0;
                double distance = Math.hypot(rx - dx * t, ry - dy * t);
                if (distance <= 40 * bird.sizeMultiplier + Math.min(target.combatHalfWidth(), target.combatHalfHeight())
                        && distance < bestDistance) { best = target; bestDistance = distance; }
            }
            if (best != null) {
                state.phase = CARRY; state.elapsed = 0; state.carryStartY = bird.y;
                // An ultimate needs time to complete its lift and stage impact.
                // Fresh-input mashing still escapes, but one CPU input per tick
                // must not guarantee escape before the slam can even begin.
                bird.pterodactylGrab(best, state.ultimate ? 120 : 38);
                bird.vy = -6;
            } else if (bird.isOnGround()) recover(bird);
        } else if (state.phase == SLAM && bird.isOnGround()) {
            bird.pterodactylRelease(true, true); recover(bird);
            bird.game.shakeIntensity = Math.max(bird.game.shakeIntensity, 8);
            bird.game.playHitSound(40);
        }
    }

    private static void hitArea(Bird bird, double width, double height, int damage,
                                double launchX, double launchY, int stun) {
        State state = bird.pterodactyl;
        for (Bird target : bird.game.players) {
            if (target == null || !bird.canDamageTarget(target) || target == bird || target.health <= 0) continue;
            long mask = 1L << target.playerIndex;
            if ((state.hitMask & mask) != 0) continue;
            double dx = (target.bodyCenterX() - bird.bodyCenterX()) * state.direction;
            if (dx < -20 || dx > width * bird.sizeMultiplier + target.combatHalfWidth()
                    || Math.abs(target.bodyCenterY() - bird.bodyCenterY()) > height * bird.sizeMultiplier + target.combatHalfHeight()) continue;
            state.hitMask |= mask;
            if (bird.applyTrackedSpecialDamage(target, damage) <= 0) continue;
            target.vx += state.direction * launchX; target.vy = launchY;
            target.applyStun(stun);
        }
    }

    static Bird.DirectionalSpecialInput aiInput(Bird bird, Bird target) {
        double across = Math.abs(target.bodyCenterX() - bird.bodyCenterX());
        double below = target.bodyCenterY() - bird.bodyCenterY();
        if (bird.isUltimateReady() && canAISwoop(bird, target, true)) return Bird.DirectionalSpecialInput.NEUTRAL;
        if (!bird.isOnGround() && below < -95 && across < 135 && !bird.pterodactyl.upUsed
                && bird.pterodactyl.cooldowns[2] == 0)
            return Bird.DirectionalSpecialInput.UP;
        if (canAISwoop(bird, target, false) && bird.pterodactyl.cooldowns[3] == 0)
            return Bird.DirectionalSpecialInput.DOWN;
        if (across > 170 && across < 285 && Math.abs(below) < 65
                && bird.pterodactyl.cooldowns[1] == 0 && canAILunge(bird, target))
            return Bird.DirectionalSpecialInput.SIDE;
        if (!bird.isUltimateReady() && bird.pterodactyl.cooldowns[0] == 0 && across < 240 && Math.abs(below) < 85)
            return Bird.DirectionalSpecialInput.NEUTRAL;
        return Bird.DirectionalSpecialInput.NEUTRAL;
    }

    static boolean shouldAIUse(Bird bird, Bird target) {
        if (active(bird) || target == null) return false;
        if (bird.isUltimateReady() && canAISwoop(bird, target, true)) return true;
        Bird.DirectionalSpecialInput input = aiInput(bird, target);
        if (input != Bird.DirectionalSpecialInput.NEUTRAL) return true;
        return !bird.isUltimateReady() && bird.pterodactyl.cooldowns[0] == 0
                && Math.abs(target.bodyCenterX() - bird.bodyCenterX()) < 240
                && Math.abs(target.bodyCenterY() - bird.bodyCenterY()) < 85;
    }

    static boolean canAILunge(Bird bird, Bird target) {
        if (bird.game.hasImplicitGroundFloorForCurrentArena()) return true;
        double direction = target.bodyCenterX() >= bird.bodyCenterX() ? 1 : -1;
        double stopX = bird.bodyCenterX() + direction * 245;
        return bird.game.platforms.stream().anyMatch(platform -> platform.y >= bird.bodyBottomY() - 1
                && platform.y <= bird.bodyBottomY() + 300
                && stopX > platform.x + 35 && stopX < platform.x + platform.w - 35);
    }

    static boolean canAISwoop(Bird bird, Bird target, boolean ultimate) {
        double below = target.bodyCenterY() - bird.bodyCenterY();
        double across = Math.abs(target.bodyCenterX() - bird.bodyCenterX());
        if (bird.isOnGround()) {
            if (Math.abs(below) > 60 || across > (ultimate ? 95 : 110)) return false;
        } else if (below < 60 || below > 350 || across > 250
                || Math.abs(across - below * (ultimate ? 11.0 / 20.0 : 9.0 / 16.0)) > 95) return false;
        if (bird.game.hasImplicitGroundFloorForCurrentArena()) return true;
        double dir = target.bodyCenterX() >= bird.bodyCenterX() ? 1 : -1;
        double landingX = target.bodyCenterX() + dir * 50;
        return bird.game.platforms.stream().anyMatch(platform -> platform.y >= target.bodyBottomY() - 1
                && landingX >= platform.x + 40 && landingX <= platform.x + platform.w - 40);
    }

    static void copiedNeutral(Bird bird) {
        State state = bird.pterodactyl;
        state.phase = GUST; state.elapsed = 0; state.direction = bird.facingDirection();
        state.hitMask = 0; state.ultimate = false; state.groundLaunch = false;
        bird.specialCooldown = bird.specialMaxCooldown = 70;
        bird.attackAnimationTimer = 23;
    }
}
