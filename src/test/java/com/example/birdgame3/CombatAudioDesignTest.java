package com.example.birdgame3;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CombatAudioDesignTest {
    private static final List<String> COMBAT_SOUNDS = List.of(
            "sfx-swing-light.wav",
            "sfx-swing-heavy.wav",
            "sfx-impact-light.wav",
            "sfx-impact-medium.wav",
            "sfx-impact-heavy.wav",
            "sfx-launch-tail.wav",
            "sfx-shield-block.wav",
            "sfx-shield-parry.wav",
            "sfx-shield-break.wav",
            "sfx-attack-clank.wav"
    );

    @Test
    void proceduralCombatLibraryContainsPlayablePcmWavFiles() throws IOException {
        for (String sound : COMBAT_SOUNDS) {
            try (InputStream stream = getClass().getResourceAsStream("/sounds/" + sound)) {
                assertNotNull(stream, sound + " must be bundled");
                byte[] bytes = stream.readAllBytes();
                assertTrue(bytes.length > 8_000, sound + " must contain an audible waveform");
                assertEquals("RIFF", new String(bytes, 0, 4, StandardCharsets.US_ASCII));
                assertEquals("WAVE", new String(bytes, 8, 4, StandardCharsets.US_ASCII));
                assertEquals(1, littleEndian16(bytes, 20), sound + " must use PCM encoding");
                assertEquals(1, littleEndian16(bytes, 22), sound + " must be mono");
                assertEquals(44_100, littleEndian32(bytes, 24), sound + " must use the authored sample rate");
                assertEquals(16, littleEndian16(bytes, 34), sound + " must use 16-bit samples");
                assertTrue(normalizedRms(bytes) >= 0.015,
                        sound + " must contain audible sample data rather than digital silence");
            }
        }
    }

    @Test
    void attackAudioRoutesWeightLaunchDefenseAndClashesToDistinctLayers() throws IOException {
        String gameSource = Files.readString(Path.of(
                "src", "main", "java", "com", "example", "birdgame3", "BirdGame3.java"));
        String birdSource = Files.readString(Path.of(
                "src", "main", "java", "com", "example", "birdgame3", "Bird.java"));

        assertTrue(gameSource.contains("impactLightClip"));
        assertTrue(gameSource.contains("impactMediumClip"));
        assertTrue(gameSource.contains("impactHeavyClip"));
        assertTrue(gameSource.contains("launchTailClip"));
        assertTrue(gameSource.contains("swingLightClip"));
        assertTrue(gameSource.contains("swingHeavyClip"));
        assertTrue(gameSource.contains("\"Attack Clank\".equals(moveName)"));
        assertTrue(birdSource.contains("game.playShieldImpactSfx(scaledDamage, true)"));
        assertTrue(birdSource.contains("game.playShieldImpactSfx(scaledDamage, false)"));
        assertTrue(birdSource.contains("game.playShieldBreakSfx()"));
        assertFalse(birdSource.contains("playPigeonBlockedAttackSfx"),
                "Shield sounds must cover the full roster rather than one attacker");
    }

    @Test
    void combatPitchVariationUsesOnlyThePresentationRandomStream() throws IOException {
        String source = Files.readString(Path.of(
                "src", "main", "java", "com", "example", "birdgame3", "BirdGame3.java"));
        int variedStart = source.indexOf("private void playManagedSfxVaried(AudioClip clip");
        int variedEnd = source.indexOf("void playButterSfx()", variedStart);
        assertTrue(variedStart >= 0 && variedEnd > variedStart);
        String variedPlayer = source.substring(variedStart, variedEnd);

        assertTrue(variedPlayer.contains("audioRandom.nextDouble()"));
        assertFalse(variedPlayer.contains("SimRng"));
        assertFalse(variedPlayer.contains("game.random"));
        assertFalse(variedPlayer.contains("simTick"));
    }

    private static int littleEndian16(byte[] bytes, int offset) {
        return (bytes[offset] & 0xff) | ((bytes[offset + 1] & 0xff) << 8);
    }

    private static int littleEndian32(byte[] bytes, int offset) {
        return (bytes[offset] & 0xff)
                | ((bytes[offset + 1] & 0xff) << 8)
                | ((bytes[offset + 2] & 0xff) << 16)
                | ((bytes[offset + 3] & 0xff) << 24);
    }

    private static double normalizedRms(byte[] bytes) {
        double squaredTotal = 0.0;
        int sampleCount = 0;
        for (int offset = 44; offset + 1 < bytes.length; offset += 2) {
            int sample = (short) littleEndian16(bytes, offset);
            squaredTotal += (double) sample * sample;
            sampleCount++;
        }
        return Math.sqrt(squaredTotal / Math.max(1, sampleCount)) / 32_768.0;
    }
}
