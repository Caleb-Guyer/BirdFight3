package com.example.birdgame3;

import org.junit.jupiter.api.Test;

import java.util.UUID;
import java.util.prefs.Preferences;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MatchReplayTest {

    @Test
    void freshReplayIsNotUsableUntilFramesRecorded() {
        MatchReplay replay = new MatchReplay(42L, 2);
        assertFalse(replay.usable());
        replay.frames.add(new int[]{0, 0});
        assertTrue(replay.usable());
    }

    @Test
    void overflowedReplayIsNotUsable() {
        MatchReplay replay = new MatchReplay(42L, 2);
        replay.frames.add(new int[]{0, 0});
        replay.overflowed = true;
        assertFalse(replay.usable());
    }

    @Test
    void retainsSeedPlayerCountAndDashTapOrder() {
        MatchReplay replay = new MatchReplay(-7L, 4);
        assertEquals(-7L, replay.seed);
        assertEquals(4, replay.playerCount);

        replay.dashTaps.add(new MatchReplay.DashTap(10L, 0, -1));
        replay.dashTaps.add(new MatchReplay.DashTap(10L, 1, 1));
        replay.dashTaps.add(new MatchReplay.DashTap(25L, 0, 1));

        assertEquals(3, replay.dashTaps.size());
        assertEquals(10L, replay.dashTaps.get(0).tick());
        assertEquals(1, replay.dashTaps.get(1).playerIndex());
        assertEquals(1, replay.dashTaps.get(2).dir());
    }

    @Test
    void maxFramesIsTenMinutesAtSixtyTicks() {
        assertEquals(60 * 60 * 10, MatchReplay.MAX_FRAMES);
    }

    @Test
    void playbackGuardRejectsLegacySimulationRevision() {
        BirdGame3 game = new BirdGame3(Preferences.userRoot().node(
                "/birdfight3-tests/replay-compatibility/" + UUID.randomUUID()));
        MatchReplay legacy = new MatchReplay(42L, 2, 1);
        legacy.frames.add(new int[]{0, 0});
        MatchReplay current = new MatchReplay(42L, 2);
        current.frames.add(new int[]{0, 0});

        assertFalse(game.canPlayReplay(legacy));
        assertTrue(game.canPlayReplay(current));
    }
}
