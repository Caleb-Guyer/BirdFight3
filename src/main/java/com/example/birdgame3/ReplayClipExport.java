package com.example.birdgame3;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** One frame in flight: encoding cannot grow a queue or drop simulation frames. */
final class ReplayClipExport implements AutoCloseable {
    static final int WIDTH = 1280;
    static final int HEIGHT = 720;
    static final int FPS = 30;
    static final int MAX_FRAMES = FPS * 60;
    private final ExecutorService encoder;
    private final Object publicationLock = new Object();
    private final Path target;
    private ReplayClipWriter writer;
    private CompletableFuture<Void> pending;
    private int frames;
    private boolean finishing;
    private volatile boolean closed;

    ReplayClipExport(Path target) {
        this(target, Executors.newSingleThreadExecutor(runnable -> {
            Thread thread = new Thread(runnable, "replay-clip-encoder");
            thread.setDaemon(true);
            return thread;
        }));
    }

    /** The export owns and shuts down this serial executor. */
    ReplayClipExport(Path target, ExecutorService encoder) {
        this.target = Objects.requireNonNull(target, "target");
        this.encoder = Objects.requireNonNull(encoder, "encoder");
        pending = CompletableFuture.runAsync(() -> {
            try {
                if (closed) return;
                writer = new ReplayClipWriter(target, WIDTH, HEIGHT, FPS);
            } catch (IOException exception) {
                throw new CompletionException(exception);
            }
        }, encoder);
    }

    boolean ready() { return pending.isDone(); }
    boolean finishing() { return finishing; }
    int frames() { return frames; }
    Path target() { return target; }

    boolean published() {
        synchronized (publicationLock) {
            return writer != null && writer.finished();
        }
    }

    void checkFailure() { if (ready()) pending.join(); }

    void submit(int[] pixels) {
        if (closed || finishing || !ready() || frames >= MAX_FRAMES) throw new IllegalStateException("Encoder is not ready");
        checkFailure();
        frames++;
        pending = CompletableFuture.runAsync(() -> {
            try {
                if (closed) return;
                writer.writeFrame(pixels);
            } catch (IOException exception) {
                throw new CompletionException(exception);
            }
        }, encoder);
    }

    void finish() {
        if (closed || finishing || !ready()) throw new IllegalStateException("Encoder is not ready");
        checkFailure();
        finishing = true;
        pending = CompletableFuture.runAsync(() -> {
            try {
                writer.finish(publicationLock, () -> !closed);
            } catch (IOException exception) {
                throw new CompletionException(exception);
            }
        }, encoder);
    }

    @Override
    public void close() {
        synchronized (publicationLock) {
            if (closed) return;
            closed = true;
        }
        // Queued behind the current write, so close never races with encoding.
        encoder.execute(() -> {
            if (writer != null) {
                try { writer.close(); } catch (IOException ignored) { /* writer already attempts temporary-file cleanup */ }
            }
        });
        encoder.shutdown();
    }
}
