package com.example.birdgame3;

final class FrameRateLimiter {
    private static final int[] FPS_CAPS = new int[]{30, 60, 90, 120, 144, 0};
    private long lastRenderNs = 0L;

    boolean shouldRender(long now, int fpsCap) {
        if (fpsCap <= 0) {
            return true;
        }
        long interval = 1_000_000_000L / fpsCap;
        if (lastRenderNs == 0L || now - lastRenderNs >= interval) {
            lastRenderNs = now;
            return true;
        }
        return false;
    }

    void reset() {
        lastRenderNs = 0L;
    }

    static int sanitizeFpsCap(int value) {
        for (int cap : FPS_CAPS) {
            if (cap == value) {
                return cap;
            }
        }
        return 60;
    }
}
