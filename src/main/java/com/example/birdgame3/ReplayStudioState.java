package com.example.birdgame3;

import java.util.Locale;

/**
 * Presentation controls for one replay session. Frames are input-stream boundaries:
 * zero is before the first input, and {@code totalFrames} is after the last input.
 * Clip ranges are start-inclusive and end-exclusive. This class never advances the
 * simulation, reads a clock, or consumes randomness.
 */
final class ReplayStudioState {
    static final int FRAMES_PER_SECOND = 60;
    static final int MAX_CLIP_FRAMES = FRAMES_PER_SECOND * 60;
    private static final int DEFAULT_CLIP_FRAMES = FRAMES_PER_SECOND * 15;
    private static final double[] SPEEDS = {0.25, 0.5, 1.0, 2.0};
    private static final String[] SPEED_LABELS = {"0.25x", "0.5x", "1x", "2x"};

    private int totalFrames;
    private boolean paused;
    private int speedIndex = 2;
    private int stepRequests;
    private int seekTarget = -1;
    private int clipStart;
    private int clipEnd;

    void reset(int totalFrames) {
        this.totalFrames = Math.max(0, totalFrames);
        paused = false;
        speedIndex = 2;
        stepRequests = 0;
        seekTarget = -1;
        clipStart = 0;
        clipEnd = Math.min(this.totalFrames, DEFAULT_CLIP_FRAMES);
    }

    int totalFrames() {
        return totalFrames;
    }

    boolean paused() {
        return paused;
    }

    void togglePaused() {
        setPaused(!paused);
    }

    void setPaused(boolean paused) {
        this.paused = paused;
        if (!paused) {
            stepRequests = 0;
        }
    }

    double speed() {
        return SPEEDS[speedIndex];
    }

    void slower() {
        speedIndex = Math.max(0, speedIndex - 1);
    }

    void faster() {
        speedIndex = Math.min(SPEEDS.length - 1, speedIndex + 1);
    }

    String speedLabel() {
        return SPEED_LABELS[speedIndex];
    }

    void requestStep() {
        paused = true;
        cancelSeek();
        if (stepRequests < Integer.MAX_VALUE) {
            stepRequests++;
        }
    }

    boolean consumeStepRequest() {
        if (stepRequests == 0) {
            return false;
        }
        stepRequests--;
        return true;
    }

    /** Pause normal playback while the controller deterministically seeks. */
    void seek(int target) {
        seekTarget = Math.max(0, Math.min(totalFrames, target));
        stepRequests = 0;
        paused = true;
    }

    boolean seeking() {
        return seekTarget >= 0;
    }

    void cancelSeek() {
        seekTarget = -1;
    }

    int seekTarget() {
        return seekTarget;
    }

    /** Returns true exactly when a pending seek is completed. */
    boolean reachedSeek(int frame) {
        if (!seeking() || frame < seekTarget) {
            return false;
        }
        cancelSeek();
        paused = true;
        return true;
    }

    boolean markIn(int frame) {
        if (frame < 0 || frame >= totalFrames) {
            return false;
        }
        int duration = clipDuration();
        clipStart = frame;
        clipEnd = (int) Math.min(totalFrames, (long) frame + duration);
        return true;
    }

    boolean markOut(int frame) {
        if (frame <= 0 || frame > totalFrames) {
            return false;
        }
        if (frame <= clipStart) {
            clipStart = Math.max(0, frame - clipDuration());
        } else if (frame - clipStart > MAX_CLIP_FRAMES) {
            clipStart = frame - MAX_CLIP_FRAMES;
        }
        clipEnd = frame;
        return true;
    }

    private int clipDuration() {
        return Math.max(1, Math.min(MAX_CLIP_FRAMES, clipEnd - clipStart));
    }

    int clipStart() {
        return clipStart;
    }

    int clipEnd() {
        return clipEnd;
    }

    boolean validClip() {
        return validClip(clipStart, clipEnd);
    }

    private boolean validClip(int start, int end) {
        return start >= 0 && end <= totalFrames && end > start
                && end - start <= MAX_CLIP_FRAMES;
    }

    static String formatTimestamp(int frame) {
        long milliseconds = Math.max(0L, frame) * 1000L / FRAMES_PER_SECOND;
        return String.format(Locale.ROOT, "%d:%02d.%03d", milliseconds / 60_000,
                milliseconds / 1000 % 60, milliseconds % 1000);
    }
}
