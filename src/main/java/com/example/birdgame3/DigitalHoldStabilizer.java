package com.example.birdgame3;

import java.util.Arrays;

final class DigitalHoldStabilizer {
    private final boolean[] stableHeld;
    private final int[] releaseSamples;
    private final int releaseDebounceSamples;

    DigitalHoldStabilizer(int channels, int releaseDebounceSamples) {
        int safeChannels = Math.max(0, channels);
        this.stableHeld = new boolean[safeChannels];
        this.releaseSamples = new int[safeChannels];
        this.releaseDebounceSamples = Math.max(1, releaseDebounceSamples);
    }

    void reset() {
        Arrays.fill(stableHeld, false);
        Arrays.fill(releaseSamples, 0);
    }

    boolean stabilize(int index, boolean currentlyHeld) {
        if (index < 0 || index >= stableHeld.length) {
            return currentlyHeld;
        }
        if (currentlyHeld) {
            stableHeld[index] = true;
            releaseSamples[index] = 0;
            return true;
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
