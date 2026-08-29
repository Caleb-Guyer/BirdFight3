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
        assertTrue(renderer.contains("if (isRaptor())"),
                "Eagle and Falcon must remain excluded from the legacy world-space cooldown bar");
    }

    @Test
    void playerTagsExplicitlyCenterTextInsideTheirBadges() throws IOException {
        String source = Files.readString(Path.of(
                "src", "main", "java", "com", "example", "birdgame3", "BirdGame3.java"));
        int rendererStart = source.indexOf("private void drawPlayerTag(GraphicsContext g, Bird b)");
        int rendererEnd = source.indexOf("private void drawPowerUpSprite", rendererStart);
        assertTrue(rendererStart >= 0 && rendererEnd > rendererStart,
                "Could not locate the player-tag renderer");

        String renderer = source.substring(rendererStart, rendererEnd);
        assertTrue(renderer.contains("g.save();") && renderer.contains("g.restore();"),
                "Player tags must isolate text alignment inherited from earlier world rendering");
        assertTrue(renderer.contains("g.setTextAlign(TextAlignment.CENTER);"),
                "P1-P4 labels must explicitly use their badge center as the text anchor");
        assertTrue(renderer.contains("g.fillText(tag, centerX,"),
                "Player tag text must be painted from the same center used by its badge");
        assertFalse(renderer.contains("centerX - textW / 2.0"),
                "Manually offset text breaks when a previous renderer leaves RIGHT alignment active");
    }

    @Test
    void naturalStagePlatformsDoNotPaintMetadataAsDecorativeLabels() throws IOException {
        String source = Files.readString(Path.of(
                "src", "main", "java", "com", "example", "birdgame3", "BirdGame3.java"));

        assertRendererDoesNotPaintText(source, "private void drawSkyVariantRockPlatform",
                "private void drawSkyVariantGround");
        assertRendererDoesNotPaintText(source, "private void drawFrozenCalderaPlatform",
                "private double longWinterThawProgress");
        assertRendererDoesNotPaintText(source, "private void drawLastIceShelfPlatform",
                "private void drawLastIceShelfVent");
        assertRendererDoesNotPaintText(source, "private void drawStillwaterPlatform",
                "private void drawStillwaterRootPlatform");

        int redlineStart = source.indexOf("private void drawRedlineCanyonArena");
        int redlineEnd = source.indexOf("private void drawRedlineRoadFoundation", redlineStart);
        assertTrue(redlineStart >= 0 && redlineEnd > redlineStart,
                "Could not locate the Redline Canyon renderer");
        assertFalse(source.substring(redlineStart, redlineEnd).contains("THE FINAL STILLNESS"),
                "Boss-arena titles must not be painted directly onto fighting surfaces");

        int citySignStart = source.indexOf("private void drawCityNeonSigns");
        int citySignEnd = source.indexOf("private void drawCrownDuelArenaDetails", citySignStart);
        assertTrue(citySignStart >= 0 && citySignEnd > citySignStart,
                "Could not locate the intentional city-sign renderer");
        assertTrue(source.substring(citySignStart, citySignEnd).contains("g.fillText(text,"),
                "Purpose-built city signs should remain visible in-world lettering");
    }

    private static void assertRendererDoesNotPaintText(String source, String declaration,
                                                       String followingDeclaration) {
        int rendererStart = source.indexOf(declaration);
        int rendererEnd = source.indexOf(followingDeclaration, rendererStart);
        assertTrue(rendererStart >= 0 && rendererEnd > rendererStart,
                "Could not locate renderer: " + declaration);
        assertFalse(source.substring(rendererStart, rendererEnd).contains("fillText("),
                declaration + " must not paint platform metadata onto natural scenery");
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
