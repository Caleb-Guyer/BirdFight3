package com.example.birdgame3;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BirdPresentationPolicyTest {

    @Test
    void localAndNetworkCharacterSelectsShareThePolishedRosterIconBuilder() throws IOException {
        String source = Files.readString(Path.of(
                "src", "main", "java", "com", "example", "birdgame3", "BirdGame3.java"));
        String declaration = "private Node buildRosterSelectionIcon";
        int builderStart = source.indexOf(declaration);
        int builderEnd = source.indexOf("private void showFightSetup", builderStart);
        assertTrue(builderStart >= 0 && builderEnd > builderStart,
                "Could not locate the shared character-select icon builder");

        String builder = source.substring(builderStart, builderEnd);
        assertTrue(builder.contains("drawRosterSprite(icon, type, null, randomPick)"),
                "Every select screen must render the polished bird model");
        assertTrue(builder.contains("drawRosterSprite(baseIcon, echoBase, null, false)"),
                "Echo fighters must keep the inset icon of their base bird");
        assertTrue(countOccurrences(source, "buildRosterSelectionIcon(") >= 5,
                "Local, network, and alternate select screens must keep using the shared icon path");
    }

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

    private static int countOccurrences(String source, String needle) {
        int count = 0;
        int offset = 0;
        while ((offset = source.indexOf(needle, offset)) >= 0) {
            count++;
            offset += needle.length();
        }
        return count;
    }
}
