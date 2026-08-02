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

    private NetworkSimulationConfig(double gravity, double startingHealth, BirdTuning[] birds) {
        this.gravity = gravity;
        this.startingHealth = startingHealth;
        this.birds = birds;
    }

    static NetworkSimulationConfig capture() {
        BirdType[] types = BirdType.values();
        BirdTuning[] birds = new BirdTuning[types.length];
        for (int i = 0; i < types.length; i++) {
            BirdType type = types[i];
            birds[i] = new BirdTuning(
                    type.power,
                    type.jumpHeight,
                    type.speed,
                    type.flyUpForce,
                    type.damageDealtMult,
                    type.damageTakenMult,
                    type.cooldownRate,
                    type.ultimateRate
            );
        }
        return new NetworkSimulationConfig(BirdGame3.GRAVITY, Bird.STARTING_HEALTH, birds);
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
    }

    static NetworkSimulationConfig read(DataInputStream in) throws IOException {
        double gravity = readFinite(in, "gravity");
        double startingHealth = readFinite(in, "starting health");
        int count = in.readInt();
        BirdType[] types = BirdType.values();
        if (count != types.length) {
            throw new IOException("Network tuning roster does not match this game build.");
        }
        BirdTuning[] birds = new BirdTuning[count];
        for (int i = 0; i < count; i++) {
            int power = in.readInt();
            int jumpHeight = in.readInt();
            double speed = readFinite(in, "bird speed");
            double flyUpForce = readFinite(in, "bird flight force");
            double damageDealtMult = readFinite(in, "damage dealt multiplier");
            double damageTakenMult = readFinite(in, "damage taken multiplier");
            double cooldownRate = readFinite(in, "cooldown rate");
            double ultimateRate = readFinite(in, "ultimate rate");
            birds[i] = new BirdTuning(power, jumpHeight, speed, flyUpForce,
                    damageDealtMult, damageTakenMult, cooldownRate, ultimateRate);
        }
        return new NetworkSimulationConfig(gravity, startingHealth, birds);
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
        return hash;
    }

    private static long mix(long hash, long value) {
        return (hash ^ value) * 1099511628211L;
    }

    private static double readFinite(DataInputStream in, String label) throws IOException {
        double value = in.readDouble();
        if (!Double.isFinite(value)) {
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
