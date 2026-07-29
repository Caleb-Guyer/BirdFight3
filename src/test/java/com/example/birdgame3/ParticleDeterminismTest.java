package com.example.birdgame3;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.prefs.Preferences;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ParticleDeterminismTest {

    private record Scenario(BirdGame3.BirdType left, BirdGame3.BirdType right,
                            BirdGame3.MapType map, long seed) {
    }

    private record Trace(List<Long> hashes, BirdGame3.BirdType winner,
                         double[] x, double[] y, double[] health, int[] stocks,
                         long nextSimRandomBits, int peakParticleCount) {
    }

    private static BirdGame3 freshGame() {
        return new BirdGame3(Preferences.userRoot().node(
                "/birdfight3-tests/particle-determinism/" + UUID.randomUUID()));
    }

    @Test
    void particlesOnAndOffProduceIdenticalGameplayAcrossBirdsAndStages() {
        List<Scenario> scenarios = List.of(
                new Scenario(BirdGame3.BirdType.PHOENIX, BirdGame3.BirdType.PENGUIN,
                        BirdGame3.MapType.ASHFALL_CATHEDRAL, 0x51A7E001L),
                new Scenario(BirdGame3.BirdType.VULTURE, BirdGame3.BirdType.ROADRUNNER,
                        BirdGame3.MapType.DOCK, 0x51A7E002L),
                new Scenario(BirdGame3.BirdType.HUMMINGBIRD, BirdGame3.BirdType.TURKEY,
                        BirdGame3.MapType.BATTLEFIELD, 0x51A7E003L)
        );

        for (Scenario scenario : scenarios) {
            Trace particlesOn = runTrace(scenario, true, 900);
            Trace particlesOff = runTrace(scenario, false, 900);
            assertEquivalentGameplay(particlesOn, particlesOff, scenario);
            assertTrue(particlesOn.peakParticleCount() > 0,
                    "The enabled run must exercise particles for " + scenario);
            assertEquals(0, particlesOff.peakParticleCount(),
                    "The disabled harness must retain no cosmetic particles for " + scenario);
        }
    }

    @Test
    void lanHashExchangeStaysCleanWithOppositeParticleSettings() {
        Scenario scenario = new Scenario(
                BirdGame3.BirdType.PHOENIX,
                BirdGame3.BirdType.GOOSE,
                BirdGame3.MapType.ASHFALL_CATHEDRAL,
                0x1A11FACE33L);
        Trace host = runTrace(scenario, true, 240);
        Trace client = runTrace(scenario, false, 240);
        assertEquivalentGameplay(host, client, scenario);
        assertEquals(240, host.hashes().size(), "Smoke test needs two full hash intervals.");

        LockstepSession hashExchange = new LockstepSession(new boolean[]{true, true, false, false});
        for (int tick : new int[]{120, 240}) {
            long hostHash = host.hashes().get(tick - 1);
            long clientHash = client.hashes().get(tick - 1);
            assertFalse(hashExchange.recordHash(tick, hostHash, false));
            assertFalse(hashExchange.recordHash(tick, clientHash, true),
                    "Opposite particle settings desynced at LAN hash tick " + tick);
        }
    }

    private static Trace runTrace(Scenario scenario, boolean particlesEnabled, int maxTicks) {
        BirdGame3 game = freshGame();
        game.harnessPrepareMatch(scenario.left(), scenario.right(), scenario.seed(), scenario.map());
        game.harnessSetParticleEffectsEnabled(particlesEnabled);

        List<Long> hashes = new ArrayList<>(maxTicks);
        int peakParticles = 0;
        for (int tick = 0; tick < maxTicks; tick++) {
            boolean running = game.harnessTick();
            hashes.add(game.harnessStateHash());
            peakParticles = Math.max(peakParticles, game.particles.size());
            if (!running) {
                break;
            }
        }

        double[] x = new double[2];
        double[] y = new double[2];
        double[] health = new double[2];
        for (int i = 0; i < 2; i++) {
            Bird bird = game.players[i];
            x[i] = bird.x;
            y[i] = bird.y;
            health[i] = bird.health;
        }
        BirdGame3.BirdType winner = game.harnessWinner == null ? null : game.harnessWinner.type;
        long nextSimRandomBits = Double.doubleToLongBits(SimRng.next());
        return new Trace(
                List.copyOf(hashes),
                winner,
                x,
                y,
                health,
                Arrays.copyOf(game.scores, 2),
                nextSimRandomBits,
                peakParticles);
    }

    private static void assertEquivalentGameplay(Trace expected, Trace actual, Scenario scenario) {
        assertEquals(expected.hashes(), actual.hashes(),
                "Gameplay hashes diverged with particles toggled for " + scenario);
        assertEquals(expected.winner(), actual.winner(), "Winner changed for " + scenario);
        assertArrayEquals(expected.x(), actual.x(), "X positions changed for " + scenario);
        assertArrayEquals(expected.y(), actual.y(), "Y positions changed for " + scenario);
        assertArrayEquals(expected.health(), actual.health(), "Health changed for " + scenario);
        assertArrayEquals(expected.stocks(), actual.stocks(), "Stocks changed for " + scenario);
        assertEquals(expected.nextSimRandomBits(), actual.nextSimRandomBits(),
                "The next SimRng value changed for " + scenario);
    }
}
