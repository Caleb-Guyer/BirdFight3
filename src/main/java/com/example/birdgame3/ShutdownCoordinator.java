package com.example.birdgame3;

/**
 * Owns the three one-shot phases of application shutdown. JavaFX invokes
 * {@code Application.stop()} after a requested exit, so cleanup must remain
 * idempotent even when the window handler and lifecycle callback overlap.
 */
final class ShutdownCoordinator {
    private boolean shutdownStarted;
    private boolean fxCleanupClaimed;
    private boolean nativeCleanupClaimed;

    synchronized boolean beginShutdown() {
        if (shutdownStarted) return false;
        shutdownStarted = true;
        return true;
    }

    synchronized boolean claimFxCleanup() {
        if (fxCleanupClaimed) return false;
        fxCleanupClaimed = true;
        return true;
    }

    synchronized boolean claimNativeCleanup() {
        if (nativeCleanupClaimed) return false;
        nativeCleanupClaimed = true;
        return true;
    }

    synchronized boolean shutdownStarted() {
        return shutdownStarted;
    }
}
