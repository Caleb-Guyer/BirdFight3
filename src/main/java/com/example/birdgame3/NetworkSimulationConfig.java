package com.example.birdgame3;

import com.example.birdgame3.BirdGame3.BirdType;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

/**
 * The host-authoritative tunable values that affect deterministic simulation.
 *
 * <p>{@code bird-stats.properties} deliberately survives game updates, so two
 * players can have different local values even while running the same build.
 * Lockstep cannot tolerate that: identical inputs under different movement or
 * combat constants immediately produce different matches. The host captures
 * its active tuning and sends this snapshot in {@code MSG_START}; clients apply
 * it before any birds or arena state are created.
 */
final class NetworkSimulationConfig {
    private final double gravity;
    private final double startingHealth;
    private final BirdTuning[] birds;
    private final VersusRules versusRules;

    private NetworkSimulationConfig(double gravity, double startingHealth, BirdTuning[] birds,
                                    VersusRules versusRules) {
        this.gravity = gravity;
        this.startingHealth = startingHealth;
        this.birds = birds;
        this.versusRules = versusRules == null ? VersusRules.standard() : versusRules;
    }

    static NetworkSimulationConfig capture() {
        return capture(VersusRules.standard());
    }

    static NetworkSimulationConfig capture(VersusRules versusRules) {
        BirdType[] types = BirdType.values();
        BirdTuning[] birds = new BirdTuning[types.length];
        for (int i = 0; i < types.length; i++) {
            BirdType type = types[i];
            birds[i] = new BirdTuning(
                    BirdStats.clampPower(type.power),
                    BirdStats.clampJumpHeight(type.jumpHeight),
                    BirdStats.clampSpeed(type.speed),
                    BirdStats.clampFlyUpForce(type.flyUpForce),
                    BirdStats.clampMultiplier(type.damageDealtMult),
                    BirdStats.clampMultiplier(type.damageTakenMult),
                    BirdStats.clampMultiplier(type.cooldownRate),
                    BirdStats.clampMultiplier(type.ultimateRate)
            );
        }
        return new NetworkSimulationConfig(
                BirdStats.clampGravity(BirdGame3.GRAVITY),
                BirdStats.clampStartingHealth(Bird.STARTING_HEALTH),
                birds,
                versusRules);
    }

    void write(DataOutputStream out) throws IOException {
        out.writeDouble(gravity);
        out.writeDouble(startingHealth);
        out.writeInt(birds.length);
        for (BirdTuning bird : birds) {
            out.writeInt(bird.power);
            out.writeInt(bird.jumpHeight);
            out.writeDouble(bird.speed);
            out.writeDouble(bird.flyUpForce);
            out.writeDouble(bird.damageDealtMult);
            out.writeDouble(bird.damageTakenMult);
            out.writeDouble(bird.cooldownRate);
            out.writeDouble(bird.ultimateRate);
        }
        out.writeUTF(versusRules.encode());
    }

    static NetworkSimulationConfig read(DataInputStream in) throws IOException {
        double gravity = readFiniteInRange(in, "gravity",
                BirdStats.MIN_GRAVITY, BirdStats.MAX_GRAVITY);
        double startingHealth = readFiniteInRange(in, "starting health",
                BirdStats.MIN_STARTING_HEALTH, BirdStats.MAX_STARTING_HEALTH);
        int count = in.readInt();
        BirdType[] types = BirdType.values();
        if (count != types.length) {
            throw new IOException("Network tuning roster does not match this game build.");
        }
        BirdTuning[] birds = new BirdTuning[count];
        for (int i = 0; i < count; i++) {
            int power = readIntInRange(in, "bird power",
                    BirdStats.MIN_POWER, BirdStats.MAX_POWER);
            int jumpHeight = readIntInRange(in, "bird jump height",
                    BirdStats.MIN_JUMP_HEIGHT, BirdStats.MAX_JUMP_HEIGHT);
            double speed = readFiniteInRange(in, "bird speed",
                    BirdStats.MIN_SPEED, BirdStats.MAX_SPEED);
            double flyUpForce = readFiniteInRange(in, "bird flight force",
                    BirdStats.MIN_FLY_UP_FORCE, BirdStats.MAX_FLY_UP_FORCE);
            double damageDealtMult = readFiniteInRange(in, "damage dealt multiplier",
                    BirdStats.MIN_MULTIPLIER, BirdStats.MAX_MULTIPLIER);
            double damageTakenMult = readFiniteInRange(in, "damage taken multiplier",
                    BirdStats.MIN_MULTIPLIER, BirdStats.MAX_MULTIPLIER);
            double cooldownRate = readFiniteInRange(in, "cooldown rate",
                    BirdStats.MIN_MULTIPLIER, BirdStats.MAX_MULTIPLIER);
            double ultimateRate = readFiniteInRange(in, "ultimate rate",
                    BirdStats.MIN_MULTIPLIER, BirdStats.MAX_MULTIPLIER);
            birds[i] = new BirdTuning(power, jumpHeight, speed, flyUpForce,
                    damageDealtMult, damageTakenMult, cooldownRate, ultimateRate);
        }
        VersusRules versusRules = VersusRules.decode(in.readUTF(), VersusRules.standard());
        return new NetworkSimulationConfig(gravity, startingHealth, birds, versusRules);
    }

    VersusRules versusRules() {
        return versusRules;
    }

    void apply() {
        BirdType[] types = BirdType.values();
        if (birds.length != types.length) {
            throw new IllegalStateException("Network tuning roster changed before match start.");
        }
        BirdGame3.GRAVITY = gravity;
        Bird.STARTING_HEALTH = startingHealth;
        for (int i = 0; i < types.length; i++) {
            BirdType type = types[i];
            BirdTuning bird = birds[i];
            type.power = bird.power;
            type.jumpHeight = bird.jumpHeight;
            type.speed = bird.speed;
            type.flyUpForce = bird.flyUpForce;
            type.damageDealtMult = bird.damageDealtMult;
            type.damageTakenMult = bird.damageTakenMult;
            type.cooldownRate = bird.cooldownRate;
            type.ultimateRate = bird.ultimateRate;
        }
    }

    long fingerprint() {
        long hash = 1469598103934665603L;
        hash = mix(hash, Double.doubleToLongBits(gravity));
        hash = mix(hash, Double.doubleToLongBits(startingHealth));
        hash = mix(hash, birds.length);
        for (BirdTuning bird : birds) {
            hash = mix(hash, bird.power);
            hash = mix(hash, bird.jumpHeight);
            hash = mix(hash, Double.doubleToLongBits(bird.speed));
            hash = mix(hash, Double.doubleToLongBits(bird.flyUpForce));
            hash = mix(hash, Double.doubleToLongBits(bird.damageDealtMult));
            hash = mix(hash, Double.doubleToLongBits(bird.damageTakenMult));
            hash = mix(hash, Double.doubleToLongBits(bird.cooldownRate));
            hash = mix(hash, Double.doubleToLongBits(bird.ultimateRate));
        }
        for (int i = 0; i < versusRules.encode().length(); i++) {
            hash = mix(hash, versusRules.encode().charAt(i));
        }
        return hash;
    }

    private static long mix(long hash, long value) {
        return (hash ^ value) * 1099511628211L;
    }

    private static int readIntInRange(DataInputStream in, String label,
                                      int minimum, int maximum) throws IOException {
        int value = in.readInt();
        if (value < minimum || value > maximum) {
            throw new IOException("Invalid network " + label + ".");
        }
        return value;
    }

    private static double readFiniteInRange(DataInputStream in, String label,
                                             double minimum, double maximum) throws IOException {
        double value = in.readDouble();
        if (!Double.isFinite(value) || value < minimum || value > maximum) {
            throw new IOException("Invalid network " + label + ".");
        }
        return value;
    }

    private record BirdTuning(
            int power,
            int jumpHeight,
            double speed,
            double flyUpForce,
            double damageDealtMult,
            double damageTakenMult,
            double cooldownRate,
            double ultimateRate
    ) {
    }
}
