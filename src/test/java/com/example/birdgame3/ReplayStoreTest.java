package com.example.birdgame3;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReplayStoreTest {

    private static MatchReplay sampleReplay() {
        MatchReplay replay = new MatchReplay(-987654321L, 2);
        replay.mapName = "FOREST";
        replay.timestampMillis = 1_752_000_000_000L;
        replay.winnerLabel = "P1: Eagle";
        replay.teamModeEnabled = false;
        replay.mutatorModeEnabled = true;
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
        assertEquals(original.mapName, loaded.mapName);
        assertEquals(original.timestampMillis, loaded.timestampMillis);
        assertEquals(original.winnerLabel, loaded.winnerLabel);
        assertEquals(original.teamModeEnabled, loaded.teamModeEnabled);
        assertEquals(original.mutatorModeEnabled, loaded.mutatorModeEnabled);
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
}
