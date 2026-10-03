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
        replay.slotInitialStocks = new int[]{2, 3};
        replay.slotInitialHealth = new double[]{72.5, 100.0};
        replay.frames.add(new int[]{0b101, 0});
        replay.frames.add(new int[]{0b001, 0b110});
        replay.frames.add(new int[]{0, 1 << 30});
        replay.dashTaps.add(new MatchReplay.DashTap(2L, 0, -1));
        replay.dashTaps.add(new MatchReplay.DashTap(3L, 1, 1));
        replay.knockouts.add(new MatchReplay.Knockout(0, "Opening KO"));
        replay.knockouts.add(new MatchReplay.Knockout(2, "P1: Eagle KOs P2: Goose"));
        replay.knockouts.add(new MatchReplay.Knockout(3, "Final KO"));
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
        assertArrayEquals(original.slotInitialStocks, loaded.slotInitialStocks);
        assertArrayEquals(original.slotInitialHealth, loaded.slotInitialHealth);
        assertEquals(original.frames.size(), loaded.frames.size());
        for (int i = 0; i < original.frames.size(); i++) {
            assertArrayEquals(original.frames.get(i), loaded.frames.get(i));
        }
        assertEquals(original.dashTaps, loaded.dashTaps);
        assertEquals(original.knockouts, loaded.knockouts);
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
        assertTrue(loaded.knockouts.isEmpty());
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
    void versionFiveReplayRetainsConfigurationWithNoKnockoutBookmarks(@TempDir Path dir) throws Exception {
        MatchReplay original = sampleReplay();
        Path legacyFile = dir.resolve("legacy-v5" + ReplayStore.FILE_EXTENSION);
        writeLegacyReplay(legacyFile, original, 5);

        MatchReplay loaded = ReplayStore.load(legacyFile);

        assertNotNull(loaded);
        assertTrue(loaded.compatibleWithCurrentSimulation());
        assertEquals(original.mapVariantName, loaded.mapVariantName);
        assertEquals(original.versusRulesEncoded, loaded.versusRulesEncoded);
        assertArrayEquals(original.slotInitialStocks, loaded.slotInitialStocks);
        assertArrayEquals(original.slotInitialHealth, loaded.slotInitialHealth);
        assertEquals(original.frames.size(), loaded.frames.size());
        assertEquals(original.dashTaps, loaded.dashTaps);
        assertTrue(loaded.knockouts.isEmpty());
    }

    @Test
    void refusesOutOfRangeKnockoutBookmarksWithoutCreatingAReplay(@TempDir Path dir) throws Exception {
        for (int frame : new int[]{-1, 4}) {
            MatchReplay replay = sampleReplay();
            replay.knockouts.add(new MatchReplay.Knockout(frame, "Invalid KO"));
            assertNull(ReplayStore.save(dir, replay));
        }
        try (var files = Files.list(dir)) {
            assertEquals(0, files.count());
        }
    }

    @Test
    void corruptKnockoutBoundsCountsAndTruncatedEntriesAreRejected(@TempDir Path dir) throws Exception {
        Path file = dir.resolve("bad-knockouts" + ReplayStore.FILE_EXTENSION);
        MatchReplay replay = sampleReplay();
        for (int frame : new int[]{-1, replay.frames.size() + 1}) {
            replay.knockouts.clear();
            replay.knockouts.add(new MatchReplay.Knockout(frame, "Invalid KO"));
            writeLegacyReplay(file, replay, 6);
            assertNull(ReplayStore.load(file));
        }
        replay.knockouts.clear();
        for (int count : new int[]{-1, MatchReplay.MAX_KNOCKOUTS + 1, 1}) {
            // Count 1 with no entries also exercises an interrupted/truncated file.
            writeLegacyReplay(file, replay, 6, count);
            assertNull(ReplayStore.load(file));
        }
        replay.knockouts.add(new MatchReplay.Knockout(0, "x".repeat(257)));
        writeLegacyReplay(file, replay, 6);
        assertNull(ReplayStore.load(file));
        assertTrue(ReplayStore.listAll(dir).isEmpty());
    }

    @Test
    void favoritesPersistWithoutChangingReplayBytesAndDeleteCleansMetadata(@TempDir Path dir) throws Exception {
        Path file = ReplayStore.save(dir, sampleReplay());
        assertNotNull(file);
        byte[] original = Files.readAllBytes(file);
        assertFalse(ReplayStore.isFavorite(file));

        assertTrue(ReplayStore.setFavorite(file, true));
        assertTrue(ReplayStore.setFavorite(file, true));
        Path listedFile = ReplayStore.listAll(dir).getFirst().file();
        assertTrue(ReplayStore.isFavorite(listedFile));
        assertArrayEquals(original, Files.readAllBytes(file));

        assertTrue(ReplayStore.setFavorite(file, false));
        assertTrue(ReplayStore.setFavorite(file, false));
        assertFalse(ReplayStore.isFavorite(file));
        assertTrue(ReplayStore.setFavorite(file, true));
        assertTrue(ReplayStore.delete(file));
        assertFalse(Files.exists(file));
        assertFalse(ReplayStore.isFavorite(file));
        try (var files = Files.list(dir)) {
            assertEquals(0, files.count());
        }
    }

    @Test
    void pruningKeepsAllFavoritesPlusThirtyRecentReplays(@TempDir Path dir) {
        MatchReplay oldest = sampleReplay();
        Path favorite = ReplayStore.save(dir, oldest);
        assertNotNull(favorite);
        assertTrue(ReplayStore.setFavorite(favorite, true));
        for (int i = 1; i <= ReplayStore.MAX_KEPT + 2; i++) {
            MatchReplay current = sampleReplay();
            current.timestampMillis += i * 1_000L;
            assertNotNull(ReplayStore.save(dir, current));
        }

        ReplayStore.prune(dir);

        assertTrue(Files.exists(favorite));
        assertTrue(ReplayStore.isFavorite(favorite));
        List<ReplayStore.SavedReplay> remaining = ReplayStore.listAll(dir);
        assertEquals(ReplayStore.MAX_KEPT + 1, remaining.size());
        assertEquals(oldest.timestampMillis + 3_000L,
                remaining.stream().filter(entry -> !ReplayStore.isFavorite(entry.file()))
                        .mapToLong(entry -> entry.replay().timestampMillis).min().orElseThrow());

        assertTrue(ReplayStore.setFavorite(favorite, false));
        ReplayStore.prune(dir);
        assertFalse(Files.exists(favorite));
        assertEquals(ReplayStore.MAX_KEPT, ReplayStore.listAll(dir).size());
    }

    @Test
    void favoriteAndDeleteFailuresAreReported(@TempDir Path dir) throws Exception {
        assertFalse(ReplayStore.setFavorite(dir.resolve("missing" + ReplayStore.FILE_EXTENSION), true));
        Path file = ReplayStore.save(dir, sampleReplay());
        assertNotNull(file);
        Path marker = file.resolveSibling(file.getFileName() + ".favorite");
        Files.createDirectory(marker);
        Files.writeString(marker.resolve("obstruction"), "leave this file alone");
        assertFalse(ReplayStore.setFavorite(file, true));
        assertFalse(ReplayStore.setFavorite(file, false));
        assertTrue(ReplayStore.isFavorite(file), "An unreadable marker must conservatively protect the replay.");
        assertFalse(ReplayStore.delete(file), "Metadata cleanup failures must be observable.");
        assertTrue(Files.exists(marker.resolve("obstruction")));
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
        writeLegacyReplay(file, replay, version, replay.knockouts.size());
    }

    private static void writeLegacyReplay(Path file, MatchReplay replay, int version, int knockoutCount)
            throws IOException {
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
            if (version >= 3) {
                out.writeUTF(nullToEmpty(replay.mapVariantName));
            }
            out.writeLong(replay.timestampMillis);
            out.writeUTF(nullToEmpty(replay.winnerLabel));
            out.writeBoolean(replay.teamModeEnabled);
            out.writeBoolean(replay.mutatorModeEnabled);
            if (version >= 4) {
                out.writeUTF(nullToEmpty(replay.versusRulesEncoded));
            }
            for (int i = 0; i < replay.playerCount; i++) {
                out.writeUTF(nullToEmpty(replay.slotBirdTypes[i]));
                out.writeBoolean(replay.slotIsAi[i]);
                out.writeInt(replay.slotTeams[i]);
                out.writeUTF(nullToEmpty(replay.slotSkinKeys[i]));
                out.writeDouble(replay.slotBaseSize[i]);
                out.writeDouble(replay.slotBasePower[i]);
                out.writeDouble(replay.slotBaseSpeed[i]);
                if (version >= 5) {
                    out.writeInt(replay.slotInitialStocks[i]);
                    out.writeDouble(replay.slotInitialHealth[i]);
                }
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
            if (version >= 6) {
                out.writeInt(knockoutCount);
                for (MatchReplay.Knockout knockout : replay.knockouts) {
                    out.writeInt(knockout.frame());
                    out.writeUTF(nullToEmpty(knockout.label()));
                }
            }
        }
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
