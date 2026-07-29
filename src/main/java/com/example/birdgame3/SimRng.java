package com.example.birdgame3;

import java.util.Random;

/**
 * Deterministic random source for match simulation.
 *
 * <p>All gameplay randomness (knockback variance, AI jitter, projectile behavior,
 * hazard timing) must come from this single stream so that a match can be reproduced
 * from its seed plus the frame-by-frame inputs. Cosmetic particle positions,
 * velocities, colors, counts, and emission chances must NEVER consume this stream;
 * they use BirdGame3's match-scoped particle RNG instead. The stream is reseeded at
 * the start of every match via {@link #reseed(long)}; consumption outside a match
 * (menus, victory screens) is therefore harmless.
 *
 * <p>Render-only randomness (screen-shake offsets, ambient cloud drift, cosmetic
 * sparkle in draw methods) must NOT use this class — render frame counts vary by
 * display, so drawing from this stream would desynchronize the simulation.
 *
 * <p>Only ever touched from the JavaFX application thread.
 */
final class SimRng {
    private static final Random RANDOM = new Random();

    private SimRng() {
    }

    static void reseed(long seed) {
        RANDOM.setSeed(seed);
    }

    /** The shared simulation Random instance, for code that needs the full API. */
    static Random random() {
        return RANDOM;
    }

    /** Drop-in replacement for {@code Math.random()} in simulation code. */
    static double next() {
        return RANDOM.nextDouble();
    }
}
