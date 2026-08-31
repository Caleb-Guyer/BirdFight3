package com.example.birdgame3;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

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
        VersusRules hostRules = VersusRules.standard().withName("HOST RULES")
                .withStockCount(5).withUltimatesEnabled(false).withDamageRatePercent(140)
                .withStaminaHealth(220);
        NetworkSimulationConfig hostConfig = NetworkSimulationConfig.capture(hostRules);

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

        assertEquals(hostConfig.fingerprint(), NetworkSimulationConfig.capture(hostRules).fingerprint());
        assertEquals(7.25, BirdGame3.BirdType.PIGEON.speed);
        assertEquals(13, BirdGame3.BirdType.GOOSE.power);
        assertEquals(1.37, BirdGame3.BirdType.RAVEN.cooldownRate);
        assertEquals(0.73, BirdGame3.GRAVITY);
        assertEquals(275.0, Bird.STARTING_HEALTH);
        assertEquals(hostRules, received.versusRules());
    }

    @Test
    void captureSanitizesUnsafeHostRuntimeValues() {
        BirdGame3.BirdType.PIGEON.power = 1000;
        BirdGame3.BirdType.PIGEON.jumpHeight = 1000;
        BirdGame3.BirdType.PIGEON.speed = 1000;
        BirdGame3.BirdType.PIGEON.flyUpForce = 1000;
        BirdGame3.BirdType.PIGEON.damageDealtMult = 1000;

        NetworkSimulationConfig.capture().apply();

        assertEquals(BirdStats.MAX_POWER, BirdGame3.BirdType.PIGEON.power);
        assertEquals(BirdStats.MAX_JUMP_HEIGHT, BirdGame3.BirdType.PIGEON.jumpHeight);
        assertEquals(BirdStats.MAX_SPEED, BirdGame3.BirdType.PIGEON.speed);
        assertEquals(BirdStats.MAX_FLY_UP_FORCE, BirdGame3.BirdType.PIGEON.flyUpForce);
        assertEquals(BirdStats.MAX_MULTIPLIER, BirdGame3.BirdType.PIGEON.damageDealtMult);
    }

    @Test
    void rejectsOutOfRangeTuningReceivedFromHost() throws Exception {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        DataOutputStream out = new DataOutputStream(bytes);
        out.writeDouble(BirdGame3.DEFAULT_GRAVITY);
        out.writeDouble(Bird.DEFAULT_STARTING_HEALTH);
        BirdGame3.BirdType[] types = BirdGame3.BirdType.values();
        out.writeInt(types.length);
        for (int i = 0; i < types.length; i++) {
            BirdGame3.BirdType type = types[i];
            out.writeInt(i == 0 ? BirdStats.MAX_POWER + 1 : type.defaultPower);
            out.writeInt(type.defaultJumpHeight);
            out.writeDouble(type.defaultSpeed);
            out.writeDouble(type.defaultFlyUpForce);
            out.writeDouble(type.defaultDamageDealtMult);
            out.writeDouble(type.defaultDamageTakenMult);
            out.writeDouble(type.defaultCooldownRate);
            out.writeDouble(type.defaultUltimateRate);
        }
        out.writeUTF(VersusRules.standard().encode());

        assertThrows(IOException.class, () -> NetworkSimulationConfig.read(
                new DataInputStream(new ByteArrayInputStream(bytes.toByteArray()))));
    }
}
