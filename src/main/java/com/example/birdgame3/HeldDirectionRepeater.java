package com.example.birdgame3;

import java.util.Arrays;

final class HeldDirectionRepeater {
    private final boolean[] held;
    private final long[] nextRepeatNs;
    private final long initialRepeatNs;
    private final long repeatNs;

    HeldDirectionRepeater(int directions, long initialRepeatNs, long repeatNs) {
        int safeDirections = Math.max(0, directions);
        this.held = new boolean[safeDirections];
        this.nextRepeatNs = new long[safeDirections];
        this.initialRepeatNs = Math.max(0L, initialRepeatNs);
        this.repeatNs = Math.max(0L, repeatNs);
    }

    void reset() {
        Arrays.fill(held, false);
        Arrays.fill(nextRepeatNs, 0L);
    }

    boolean shouldTrigger(int index, boolean currentlyHeld, long now) {
        if (index < 0 || index >= held.length) {
            return false;
        }
        if (!currentlyHeld) {
            held[index] = false;
            nextRepeatNs[index] = 0L;
            return false;
        }
        if (!held[index]) {
            held[index] = true;
            nextRepeatNs[index] = now + initialRepeatNs;
            return true;
        }
        if (now >= nextRepeatNs[index]) {
            nextRepeatNs[index] = now + repeatNs;
            return true;
        }
        return false;
    }
}
