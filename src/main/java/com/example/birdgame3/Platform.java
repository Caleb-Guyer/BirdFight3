package com.example.birdgame3;

class Platform {
    double x, y, w, h;
    String signText = null; // null = no sign

    // Optional deterministic pendulum motion. The resting coordinates remain
    // separate from x/y so collision and drawing always consume the same
    // current platform position without accumulating floating-point drift.
    boolean swinging = false;
    boolean carrionSortingTray = false;
    double restingX;
    double restingY;
    double swingCableLength;
    double swingMaxAngleRadians;
    int swingPeriodTicks;
    int swingPhaseTicks;

    Platform(double x, double y, double w, double h) {
        this.x = x;
        this.y = y;
        this.w = w;
        this.h = h;
        this.restingX = x;
        this.restingY = y;
    }

    Platform asCarrionSortingTray(double cableLength, double maxAngleDegrees,
                                   int periodTicks, int phaseTicks) {
        carrionSortingTray = true;
        swinging = true;
        swingCableLength = cableLength;
        swingMaxAngleRadians = Math.toRadians(maxAngleDegrees);
        swingPeriodTicks = Math.max(1, periodTicks);
        swingPhaseTicks = phaseTicks;
        return this;
    }

    void updateSwing(long simTick) {
        if (!swinging) return;
        double phase = Math.PI * 2.0 * (simTick + swingPhaseTicks) / swingPeriodTicks;
        double angle = swingMaxAngleRadians * Math.sin(phase);
        x = restingX + swingCableLength * Math.sin(angle);
        y = restingY + swingCableLength * (Math.cos(angle) - 1.0);
    }
}
