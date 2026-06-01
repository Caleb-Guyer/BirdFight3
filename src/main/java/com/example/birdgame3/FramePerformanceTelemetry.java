package com.example.birdgame3;

final class FramePerformanceTelemetry {
    private long frameStartNs;
    private long updateNs;
    private long playerUpdateNs;
    private long worldUpdateNs;
    private long effectsUpdateNs;
    private long drawWorldNs;
    private long drawHudNs;
    private long frameNs;
    private int fixedUpdates;
    private int particles;
    private int crows;
    private int chicks;
    private int piranhas;
    private int powerUps;
    private int collisionChecks;
    private int cullChecks;
    private int culledRects;

    void beginFrame() {
        frameStartNs = System.nanoTime();
        updateNs = 0L;
        playerUpdateNs = 0L;
        worldUpdateNs = 0L;
        effectsUpdateNs = 0L;
        drawWorldNs = 0L;
        drawHudNs = 0L;
        fixedUpdates = 0;
        collisionChecks = 0;
        cullChecks = 0;
        culledRects = 0;
    }

    void recordFixedUpdate(long playerNs, long worldNs, long effectsNs) {
        fixedUpdates++;
        playerUpdateNs += Math.max(0L, playerNs);
        worldUpdateNs += Math.max(0L, worldNs);
        effectsUpdateNs += Math.max(0L, effectsNs);
        updateNs += Math.max(0L, playerNs) + Math.max(0L, worldNs) + Math.max(0L, effectsNs);
    }

    void recordDrawWorld(long nanos) {
        drawWorldNs = Math.max(0L, nanos);
    }

    void recordDrawHud(long nanos) {
        drawHudNs = Math.max(0L, nanos);
    }

    void recordCollisionCheck() {
        collisionChecks++;
    }

    void recordCullCheck(boolean culled) {
        cullChecks++;
        if (culled) {
            culledRects++;
        }
    }

    void recordEntityCounts(int particles, int crows, int chicks, int piranhas, int powerUps) {
        this.particles = Math.max(0, particles);
        this.crows = Math.max(0, crows);
        this.chicks = Math.max(0, chicks);
        this.piranhas = Math.max(0, piranhas);
        this.powerUps = Math.max(0, powerUps);
    }

    void finishFrame() {
        frameNs = frameStartNs <= 0L ? 0L : Math.max(0L, System.nanoTime() - frameStartNs);
    }

    double frameMs() {
        return toMs(frameNs);
    }

    double updateMs() {
        return toMs(updateNs);
    }

    double playerUpdateMs() {
        return toMs(playerUpdateNs);
    }

    double worldUpdateMs() {
        return toMs(worldUpdateNs);
    }

    double effectsUpdateMs() {
        return toMs(effectsUpdateNs);
    }

    double drawWorldMs() {
        return toMs(drawWorldNs);
    }

    double drawHudMs() {
        return toMs(drawHudNs);
    }

    int fixedUpdates() {
        return fixedUpdates;
    }

    int particles() {
        return particles;
    }

    int crows() {
        return crows;
    }

    int chicks() {
        return chicks;
    }

    int piranhas() {
        return piranhas;
    }

    int powerUps() {
        return powerUps;
    }

    int collisionChecks() {
        return collisionChecks;
    }

    int cullChecks() {
        return cullChecks;
    }

    int culledRects() {
        return culledRects;
    }

    private static double toMs(long nanos) {
        return nanos / 1_000_000.0;
    }
}
