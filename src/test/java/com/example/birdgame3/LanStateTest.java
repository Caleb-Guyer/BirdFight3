package com.example.birdgame3;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LanStateTest {
    @Test
    void writeReadRoundTripPreservesNestedState() throws IOException {
        LanState state = new LanState();
        state.matchTimer = 321;
        state.matchEnded = true;
        state.activePlayers = 3;
        state.camX = 150.5;
        state.camY = -20.25;
        state.zoom = 0.8;
        state.shakeIntensity = 2.75;
        state.hitstopFrames = 4;
        state.scores = new int[]{10, 20, 30, 40};
        state.killFeed = List.of("P1 KO", "P2 KO");

        LanBirdState bird = new LanBirdState();
        bird.typeOrdinal = 2;
        bird.x = 12.5;
        bird.y = 50.25;
        bird.health = 133.0;
        bird.facingRight = true;
        bird.specialCooldown = 18;
        bird.isCitySkin = true;
        bird.ultimateMeter = 44.0;
        state.birds[0] = bird;

        LanState.PowerUpState powerUp = new LanState.PowerUpState();
        powerUp.x = 200.0;
        powerUp.y = 300.0;
        powerUp.typeOrdinal = 1;
        state.powerUps.add(powerUp);

        LanState.NectarNodeState nectar = new LanState.NectarNodeState();
        nectar.x = 10.0;
        nectar.y = 20.0;
        nectar.isSpeed = true;
        nectar.active = false;
        state.nectarNodes.add(nectar);

        LanState.SwingingVineState vine = new LanState.SwingingVineState();
        vine.baseX = 11.0;
        vine.baseY = 22.0;
        vine.length = 33.0;
        vine.angle = 0.5;
        vine.angularVelocity = 0.75;
        state.swingingVines.add(vine);

        LanState.WindVentState vent = new LanState.WindVentState();
        vent.x = 1.0;
        vent.y = 2.0;
        vent.w = 3.0;
        vent.cooldown = 4;
        state.windVents.add(vent);

        LanState.CrowMinionState crow = new LanState.CrowMinionState();
        crow.x = 7.0;
        crow.y = 8.0;
        crow.age = 9;
        crow.ownerIndex = 1;
        crow.hasCrown = true;
        state.crowMinions.add(crow);

        LanState.ChickMinionState chick = new LanState.ChickMinionState();
        chick.x = 4.0;
        chick.y = 5.0;
        chick.vx = 6.0;
        chick.age = 7;
        chick.ownerIndex = 2;
        chick.variant = 3;
        chick.life = 8;
        chick.ultimate = true;
        state.chickMinions.add(chick);

        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        state.write(new DataOutputStream(bos));

        LanState decoded = LanState.read(new DataInputStream(new ByteArrayInputStream(bos.toByteArray())));

        assertEquals(321, decoded.matchTimer);
        assertTrue(decoded.matchEnded);
        assertEquals(3, decoded.activePlayers);
        assertEquals(0.8, decoded.zoom);
        assertEquals(20, decoded.scores[1]);
        assertEquals(List.of("P1 KO", "P2 KO"), decoded.killFeed);
        assertNotNull(decoded.birds[0]);
        assertEquals(2, decoded.birds[0].typeOrdinal);
        assertEquals(12.5, decoded.birds[0].x);
        assertTrue(decoded.birds[0].isCitySkin);
        assertEquals(44.0, decoded.birds[0].ultimateMeter);
        assertEquals(1, decoded.powerUps.size());
        assertEquals(200.0, decoded.powerUps.get(0).x);
        assertFalse(decoded.nectarNodes.get(0).active);
        assertEquals(33.0, decoded.swingingVines.get(0).length);
        assertEquals(4, decoded.windVents.get(0).cooldown);
        assertTrue(decoded.crowMinions.get(0).hasCrown);
        assertTrue(decoded.chickMinions.get(0).ultimate);
    }
}
