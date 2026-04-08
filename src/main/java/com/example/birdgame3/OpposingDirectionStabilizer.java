package com.example.birdgame3;

import java.util.Arrays;

final class OpposingDirectionStabilizer {
    private final boolean[] stableHeld;
    private final int[] releaseSamples;
    private final int releaseDebounceSamples;

    OpposingDirectionStabilizer(int directions, int releaseDebounceSamples) {
        int safeDirections = Math.max(0, directions);
        this.stableHeld = new boolean[safeDirections];
        this.releaseSamples = new int[safeDirections];
        this.releaseDebounceSamples = Math.max(1, releaseDebounceSamples);
    }

    void reset() {
        Arrays.fill(stableHeld, false);
        Arrays.fill(releaseSamples, 0);
    }

    boolean stabilize(int index, boolean directionHeld, boolean oppositeHeld) {
        if (index < 0 || index >= stableHeld.length) {
            return directionHeld && !oppositeHeld;
        }
        if (directionHeld) {
            stableHeld[index] = true;
            releaseSamples[index] = 0;
            return true;
        }
        if (oppositeHeld) {
            stableHeld[index] = false;
            releaseSamples[index] = 0;
            return false;
        }
        if (!stableHeld[index]) {
            return false;
        }
        releaseSamples[index]++;
        if (releaseSamples[index] >= releaseDebounceSamples) {
            stableHeld[index] = false;
            releaseSamples[index] = 0;
            return false;
        }
        return true;
    }
}
