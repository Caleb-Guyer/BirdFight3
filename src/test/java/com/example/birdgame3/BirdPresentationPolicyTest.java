package com.example.birdgame3;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BirdPresentationPolicyTest {

    @Test
    void worldSpaceCooldownRendererDoesNotReconnectDirectedSpecialReadinessPanels() throws IOException {
        String source = Files.readString(Path.of(
                "src", "main", "java", "com", "example", "birdgame3", "Bird.java"));
        int rendererStart = source.indexOf("private void drawSpecialCooldown(GraphicsContext g)");
        int rendererEnd = source.indexOf("private void drawLounge(GraphicsContext g)", rendererStart);
        assertTrue(rendererStart >= 0 && rendererEnd > rendererStart,
                "Could not locate the world-space cooldown renderer");

        String renderer = source.substring(rendererStart, rendererEnd);
        assertFalse(renderer.contains("drawDirectedSpecialReadiness"),
                "Roadrunner, Titmouse, Opium, Heisen, and Null Rock must not draw the extra readiness panel");
        assertTrue(renderer.contains("type == BirdGame3.BirdType.TITMOUSE"),
                "Titmouse must remain excluded from the legacy world-space cooldown bar");
        assertTrue(renderer.contains("isOpiumEchoPair()"),
                "Opium Bird and Heisenbird must remain excluded from the legacy world-space cooldown bar");
        assertTrue(renderer.contains("type == BirdGame3.BirdType.ROADRUNNER"),
                "Roadrunner must remain excluded from the legacy world-space cooldown bar");
        assertTrue(renderer.contains("type == BirdGame3.BirdType.VULTURE"),
                "Vulture and Null Rock must remain excluded from the legacy world-space cooldown bar");
    }
}
