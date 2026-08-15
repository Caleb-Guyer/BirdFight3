package com.example.birdgame3;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class ShutdownCoordinatorTest {
    @Test
    void eachShutdownPhaseCanOnlyBeClaimedOnce() {
        ShutdownCoordinator coordinator = new ShutdownCoordinator();

        assertTrue(coordinator.beginShutdown());
        assertFalse(coordinator.beginShutdown());
        assertTrue(coordinator.claimFxCleanup());
        assertFalse(coordinator.claimFxCleanup());
        assertTrue(coordinator.claimNativeCleanup());
        assertFalse(coordinator.claimNativeCleanup());
        assertTrue(coordinator.shutdownStarted());
    }

    @Test
    void concurrentLifecycleCallbacksStillProduceOneCleanupOwner() throws Exception {
        ShutdownCoordinator coordinator = new ShutdownCoordinator();
        CountDownLatch start = new CountDownLatch(1);
        AtomicInteger fxOwners = new AtomicInteger();
        AtomicInteger nativeOwners = new AtomicInteger();
        List<Thread> callers = new ArrayList<>();

        for (int i = 0; i < 12; i++) {
            Thread caller = new Thread(() -> {
                try {
                    start.await();
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    return;
                }
                coordinator.beginShutdown();
                if (coordinator.claimFxCleanup()) fxOwners.incrementAndGet();
                if (coordinator.claimNativeCleanup()) nativeOwners.incrementAndGet();
            });
            callers.add(caller);
            caller.start();
        }
        start.countDown();
        for (Thread caller : callers) caller.join();

        assertEquals(1, fxOwners.get());
        assertEquals(1, nativeOwners.get());
    }
}
