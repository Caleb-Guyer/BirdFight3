package com.example.birdgame3;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

@Timeout(10)
class ReplayClipExportTest {
    @Test
    void allowsOnlyOneFrameInFlightAndPublishesAfterFinish(@TempDir Path dir) throws Exception {
        ExecutorService worker = Executors.newSingleThreadExecutor();
        Path target = dir.resolve("async.avi");
        try (ReplayClipExport export = new ReplayClipExport(target, worker)) {
            awaitReady(export);
            export.checkFailure();
            assertEquals(target, export.target());
            assertEquals(0, export.frames());
            assertFalse(export.finishing());
            try (WorkerGate ignored = new WorkerGate(worker)) {
                export.submit(pixels());
                assertEquals(1, export.frames());
                assertFalse(export.ready());
                assertThrows(IllegalStateException.class, () -> export.submit(pixels()));
                assertThrows(IllegalStateException.class, export::finish);
                assertFalse(Files.exists(target));
            }
            awaitReady(export);
            export.checkFailure();
            export.finish();
            assertTrue(export.finishing());
            assertThrows(IllegalStateException.class, () -> export.submit(pixels()));
            assertThrows(IllegalStateException.class, export::finish);
            awaitReady(export);
            export.checkFailure();
            assertTrue(Files.exists(target));
            byte[] clip = Files.readAllBytes(target);
            assertEquals("RIFF", new String(clip, 0, 4, StandardCharsets.US_ASCII));
            assertEquals("AVI ", new String(clip, 8, 4, StandardCharsets.US_ASCII));
        } finally {
            stop(worker);
        }
        assertEquals(List.of(target), files(dir));
    }

    @Test
    void cancellingBeforeInitializationLeavesNoOutput(@TempDir Path dir) throws Exception {
        ExecutorService worker = Executors.newSingleThreadExecutor();
        Path target = dir.resolve("cancel-init.avi");
        try {
            try (WorkerGate ignored = new WorkerGate(worker)) {
                ReplayClipExport export = new ReplayClipExport(target, worker);
                assertFalse(export.ready());
                export.close();
                export.close();
                assertThrows(IllegalStateException.class, () -> export.submit(pixels()));
                assertThrows(IllegalStateException.class, export::finish);
            }
            assertTrue(worker.awaitTermination(5, TimeUnit.SECONDS));
        } finally {
            stop(worker);
        }
        assertTrue(files(dir).isEmpty());
    }

    @Test
    void cancellingWithAQueuedFrameCleansTheTemporaryFile(@TempDir Path dir) throws Exception {
        ExecutorService worker = Executors.newSingleThreadExecutor();
        Path target = dir.resolve("cancel-frame.avi");
        try (ReplayClipExport export = new ReplayClipExport(target, worker)) {
            awaitReady(export);
            export.checkFailure();
            assertEquals(1, files(dir).size(), "Initialization creates a temporary file.");
            try (WorkerGate ignored = new WorkerGate(worker)) {
                export.submit(pixels());
                export.close();
                assertFalse(Files.exists(target));
            }
            assertTrue(worker.awaitTermination(5, TimeUnit.SECONDS));
            assertTrue(files(dir).isEmpty());
        } finally {
            stop(worker);
        }
    }

    @Test
    void cancellingAPendingFinishNeverPublishesTheClip(@TempDir Path dir) throws Exception {
        ExecutorService worker = Executors.newSingleThreadExecutor();
        Path target = dir.resolve("cancel-finish.avi");
        try (ReplayClipExport export = new ReplayClipExport(target, worker)) {
            awaitReady(export);
            export.submit(pixels());
            awaitReady(export);
            export.checkFailure();
            try (WorkerGate ignored = new WorkerGate(worker)) {
                export.finish();
                assertTrue(export.finishing());
                assertFalse(export.ready());
                export.close();
            }
            assertTrue(worker.awaitTermination(5, TimeUnit.SECONDS));
            assertTrue(export.ready());
            CompletionException cancelled = assertThrows(CompletionException.class, export::checkFailure);
            assertEquals("Clip export was cancelled", cancelled.getCause().getMessage());
            assertTrue(files(dir).isEmpty(), "A cancelled queued finish must discard its complete temporary clip.");
        } finally {
            stop(worker);
        }
    }

    @Test
    void initializationFailureIsReportedWithoutChangingExistingDestination(@TempDir Path dir) throws Exception {
        ExecutorService worker = Executors.newSingleThreadExecutor();
        Path target = dir.resolve("existing.avi");
        Files.writeString(target, "preserve existing clip");
        try (ReplayClipExport export = new ReplayClipExport(target, worker)) {
            awaitReady(export);
            CompletionException failed = assertThrows(CompletionException.class, export::checkFailure);
            assertInstanceOf(FileAlreadyExistsException.class, failed.getCause());
            assertThrows(CompletionException.class, () -> export.submit(pixels()));
            assertThrows(CompletionException.class, export::finish);
            assertEquals(0, export.frames());
            assertFalse(export.finishing());
        } finally {
            stop(worker);
        }
        assertEquals("preserve existing clip", Files.readString(target));
        assertEquals(List.of(target), files(dir));
    }

    @Test
    void frameEncodingFailureIsReportedAndRemovesPartialOutput(@TempDir Path dir) throws Exception {
        ExecutorService worker = Executors.newSingleThreadExecutor();
        try (ReplayClipExport export = new ReplayClipExport(dir.resolve("invalid-frame.avi"), worker)) {
            awaitReady(export);
            export.submit(new int[4]);
            awaitReady(export);
            CompletionException failed = assertThrows(CompletionException.class, export::checkFailure);
            assertInstanceOf(IllegalArgumentException.class, failed.getCause());
            assertTrue(files(dir).isEmpty());
        } finally {
            stop(worker);
        }
    }

    private static int[] pixels() {
        int[] result = new int[ReplayClipExport.WIDTH * ReplayClipExport.HEIGHT];
        Arrays.fill(result, 0xff2675cc);
        return result;
    }

    private static void awaitReady(ReplayClipExport export) throws InterruptedException {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
        while (!export.ready() && System.nanoTime() < deadline) Thread.sleep(2);
        assertTrue(export.ready(), "The encoder should complete within five seconds.");
    }

    private static List<Path> files(Path dir) throws Exception {
        try (var paths = Files.list(dir)) {
            return paths.sorted().toList();
        }
    }

    private static void stop(ExecutorService worker) throws InterruptedException {
        worker.shutdown();
        if (!worker.awaitTermination(5, TimeUnit.SECONDS)) {
            worker.shutdownNow();
            assertTrue(worker.awaitTermination(1, TimeUnit.SECONDS));
        }
    }

    /** Holds the real serial worker so pending operations can be tested without timing races. */
    private static final class WorkerGate implements AutoCloseable {
        private final CountDownLatch release = new CountDownLatch(1);

        WorkerGate(ExecutorService worker) throws InterruptedException {
            CountDownLatch started = new CountDownLatch(1);
            worker.execute(() -> {
                started.countDown();
                try {
                    release.await(5, TimeUnit.SECONDS);
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                }
            });
            assertTrue(started.await(5, TimeUnit.SECONDS));
        }

        @Override
        public void close() {
            release.countDown();
        }
    }
}
