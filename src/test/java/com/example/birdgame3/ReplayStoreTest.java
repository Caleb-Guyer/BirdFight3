package com.example.birdgame3;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.zip.GZIPOutputStream;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReplayStoreTest {

    private static MatchReplay sampleReplay() {
        MatchReplay replay = new MatchReplay(-987654321L, 2);
        replay.mapName = "FOREST";
        replay.mapVariantName = BirdGame3.MapVariant.CROWN_DUEL.name();
        replay.timestampMillis = 1_752_000_000_000L;
        replay.winnerLabel = "P1: Eagle";
        replay.teamModeEnabled = false;
        replay.mutatorModeEnabled = true;
        replay.versusRulesEncoded = VersusRules.chaos().withName("REPLAY RULES").encode();
        replay.slotBirdTypes = new String[]{"EAGLE", "GOOSE"};
        replay.slotIsAi = new boolean[]{false, true};
        replay.slotTeams = new int[]{1, 2};
        replay.slotSkinKeys = new String[]{"classic", null};
        replay.slotBaseSize = new double[]{1.0, 1.05};
        replay.slotBasePower = new double[]{1.1, 0.95};
        replay.slotBaseSpeed = new double[]{1.0, 1.0};
        replay.frames.add(new int[]{0b101, 0});
        replay.frames.add(new int[]{0b001, 0b110});
        replay.frames.add(new int[]{0, 1 << 30});
        replay.dashTaps.add(new MatchReplay.DashTap(2L, 0, -1));
        replay.dashTaps.add(new MatchReplay.DashTap(3L, 1, 1));
        return replay;
    }

    @Test
    void savedReplayRoundTripsExactly(@TempDir Path dir) {
        MatchReplay original = sampleReplay();
        Path file = ReplayStore.save(dir, original);
        assertNotNull(file);
        assertTrue(Files.exists(file));

        MatchReplay loaded = ReplayStore.load(file);
        assertNotNull(loaded);
        assertEquals(original.seed, loaded.seed);
        assertEquals(original.playerCount, loaded.playerCount);
        assertEquals(MatchReplay.CURRENT_SIMULATION_REVISION, loaded.simulationRevision);
        assertTrue(loaded.compatibleWithCurrentSimulation());
        assertEquals(original.mapName, loaded.mapName);
        assertEquals(original.mapVariantName, loaded.mapVariantName);
        assertEquals(original.timestampMillis, loaded.timestampMillis);
        assertEquals(original.winnerLabel, loaded.winnerLabel);
        assertEquals(original.teamModeEnabled, loaded.teamModeEnabled);
        assertEquals(original.mutatorModeEnabled, loaded.mutatorModeEnabled);
        assertEquals(original.versusRulesEncoded, loaded.versusRulesEncoded);
        assertArrayEquals(original.slotBirdTypes, loaded.slotBirdTypes);
        assertArrayEquals(original.slotIsAi, loaded.slotIsAi);
        assertArrayEquals(original.slotTeams, loaded.slotTeams);
        assertArrayEquals(original.slotSkinKeys, loaded.slotSkinKeys);
        assertArrayEquals(original.slotBaseSize, loaded.slotBaseSize);
        assertArrayEquals(original.slotBasePower, loaded.slotBasePower);
        assertArrayEquals(original.slotBaseSpeed, loaded.slotBaseSpeed);
        assertEquals(original.frames.size(), loaded.frames.size());
        for (int i = 0; i < original.frames.size(); i++) {
            assertArrayEquals(original.frames.get(i), loaded.frames.get(i));
        }
        assertEquals(original.dashTaps, loaded.dashTaps);
        assertTrue(loaded.selfContained());
        assertTrue(loaded.usable());
    }

    @Test
    void listReturnsNewestFirstAndSkipsCorruptFiles(@TempDir Path dir) throws Exception {
        MatchReplay first = sampleReplay();
        first.timestampMillis = 1_752_000_000_000L;
        MatchReplay second = sampleReplay();
        second.timestampMillis = 1_752_086_400_000L; // one day later
        assertNotNull(ReplayStore.save(dir, first));
        assertNotNull(ReplayStore.save(dir, second));
        Files.writeString(dir.resolve("junk" + ReplayStore.FILE_EXTENSION), "not a replay");

        List<ReplayStore.SavedReplay> all = ReplayStore.listAll(dir);

        assertEquals(2, all.size(), "The corrupt file must be skipped.");
        assertEquals(second.timestampMillis, all.get(0).replay().timestampMillis,
                "Newest replay should come first.");
    }

    @Test
    void refusesToSaveNonSelfContainedReplays(@TempDir Path dir) {
        MatchReplay bare = new MatchReplay(1L, 2);
        bare.frames.add(new int[]{0, 0});
        assertNull(ReplayStore.save(dir, bare), "A replay without config must not be persisted.");
    }

    @Test
    void versionOneReplayLoadsAsVisibleButIncompatibleLegacyMetadata(@TempDir Path dir) throws Exception {
        MatchReplay original = sampleReplay();
        Path legacyFile = dir.resolve("legacy-v1" + ReplayStore.FILE_EXTENSION);
        writeVersionOneReplay(legacyFile, original);

        MatchReplay loaded = ReplayStore.load(legacyFile);

        assertNotNull(loaded);
        assertEquals(1, loaded.simulationRevision);
        assertFalse(loaded.compatibleWithCurrentSimulation());
        assertEquals(original.frames.size(), loaded.frames.size());
        assertEquals(original.dashTaps, loaded.dashTaps);
        assertEquals(1, ReplayStore.listAll(dir).size(),
                "Legacy replay must remain visible in the browser model.");
    }

    @Test
    void versionTwoReplayLoadsAsStandardMapVariant(@TempDir Path dir) throws Exception {
        MatchReplay original = sampleReplay();
        Path legacyFile = dir.resolve("legacy-v2" + ReplayStore.FILE_EXTENSION);
        writeLegacyReplay(legacyFile, original, 2);

        MatchReplay loaded = ReplayStore.load(legacyFile);

        assertNotNull(loaded);
        assertTrue(loaded.compatibleWithCurrentSimulation());
        assertNull(loaded.mapVariantName);
        assertEquals(original.frames.size(), loaded.frames.size());
    }

    @Test
    void pruningNeverDeletesLegacyReplays(@TempDir Path dir) throws Exception {
        MatchReplay legacy = sampleReplay();
        Path legacyFile = dir.resolve("000-legacy-v1" + ReplayStore.FILE_EXTENSION);
        writeVersionOneReplay(legacyFile, legacy);

        for (int i = 0; i <= ReplayStore.MAX_KEPT; i++) {
            MatchReplay current = sampleReplay();
            current.timestampMillis += i * 1_000L;
            assertNotNull(ReplayStore.save(dir, current));
        }
        ReplayStore.prune(dir);

        assertTrue(Files.exists(legacyFile), "Automatic pruning must never remove legacy replays.");
        List<ReplayStore.SavedReplay> all = ReplayStore.listAll(dir);
        assertEquals(ReplayStore.MAX_KEPT,
                all.stream().filter(entry -> entry.replay().compatibleWithCurrentSimulation()).count());
        assertEquals(1,
                all.stream().filter(entry -> !entry.replay().compatibleWithCurrentSimulation()).count());
    }

    private static void writeVersionOneReplay(Path file, MatchReplay replay) throws IOException {
        writeLegacyReplay(file, replay, 1);
    }

    private static void writeLegacyReplay(Path file, MatchReplay replay, int version) throws IOException {
        Files.createDirectories(file.getParent());
        try (DataOutputStream out = new DataOutputStream(
                new GZIPOutputStream(Files.newOutputStream(file)))) {
            out.writeInt(0x42463352); // "BF3R"
            out.writeInt(version);
            if (version >= 2) {
                out.writeInt(replay.simulationRevision);
            }
            out.writeLong(replay.seed);
            out.writeInt(replay.playerCount);
            out.writeUTF(nullToEmpty(replay.mapName));
            out.writeLong(replay.timestampMillis);
            out.writeUTF(nullToEmpty(replay.winnerLabel));
            out.writeBoolean(replay.teamModeEnabled);
            out.writeBoolean(replay.mutatorModeEnabled);
            for (int i = 0; i < replay.playerCount; i++) {
                out.writeUTF(nullToEmpty(replay.slotBirdTypes[i]));
                out.writeBoolean(replay.slotIsAi[i]);
                out.writeInt(replay.slotTeams[i]);
                out.writeUTF(nullToEmpty(replay.slotSkinKeys[i]));
                out.writeDouble(replay.slotBaseSize[i]);
                out.writeDouble(replay.slotBasePower[i]);
                out.writeDouble(replay.slotBaseSpeed[i]);
            }
            out.writeInt(replay.dashTaps.size());
            for (MatchReplay.DashTap tap : replay.dashTaps) {
                out.writeLong(tap.tick());
                out.writeInt(tap.playerIndex());
                out.writeInt(tap.dir());
            }
            out.writeInt(replay.frames.size());
            for (int[] masks : replay.frames) {
                for (int player = 0; player < replay.playerCount; player++) {
                    out.writeInt(player < masks.length ? masks[player] : 0);
                }
            }
        }
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
