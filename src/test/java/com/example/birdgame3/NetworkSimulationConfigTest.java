package com.example.birdgame3;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class NetworkSimulationConfigTest {
    @AfterEach
    void restoreLocalTuning() {
        BirdStats.reloadFromDisk();
    }

    @Test
    void hostTuningRoundTripsAndOverridesDifferentClientValues() throws Exception {
        BirdGame3.BirdType.PIGEON.speed = 7.25;
        BirdGame3.BirdType.GOOSE.power = 13;
        BirdGame3.BirdType.RAVEN.cooldownRate = 1.37;
        BirdGame3.GRAVITY = 0.73;
        Bird.STARTING_HEALTH = 275.0;
        NetworkSimulationConfig hostConfig = NetworkSimulationConfig.capture();

        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        hostConfig.write(new DataOutputStream(bytes));
        NetworkSimulationConfig received = NetworkSimulationConfig.read(
                new DataInputStream(new ByteArrayInputStream(bytes.toByteArray())));

        BirdGame3.BirdType.PIGEON.speed = 1.0;
        BirdGame3.BirdType.GOOSE.power = 1;
        BirdGame3.BirdType.RAVEN.cooldownRate = 0.1;
        BirdGame3.GRAVITY = 4.0;
        Bird.STARTING_HEALTH = 10.0;
        assertNotEquals(hostConfig.fingerprint(), NetworkSimulationConfig.capture().fingerprint());

        received.apply();

        assertEquals(hostConfig.fingerprint(), NetworkSimulationConfig.capture().fingerprint());
        assertEquals(7.25, BirdGame3.BirdType.PIGEON.speed);
        assertEquals(13, BirdGame3.BirdType.GOOSE.power);
        assertEquals(1.37, BirdGame3.BirdType.RAVEN.cooldownRate);
        assertEquals(0.73, BirdGame3.GRAVITY);
        assertEquals(275.0, Bird.STARTING_HEALTH);
    }
}
