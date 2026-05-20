package com.example.birdgame3;

import javafx.scene.paint.Color;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ParticleTest {
    @Test
    void nullParticleColorFallsBackToWhite() {
        Particle particle = new Particle(0.0, 0.0, 0.0, 0.0, null);

        assertEquals(Color.WHITE, particle.color);
    }
}
