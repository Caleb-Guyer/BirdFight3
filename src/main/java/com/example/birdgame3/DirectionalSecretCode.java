package com.example.birdgame3;

import java.util.Arrays;

final class DirectionalSecretCode {
    record StepResult(boolean consumed, int nextProgress, boolean complete) {
    }

    private final char[] sequence;

    DirectionalSecretCode(char... sequence) {
        if (sequence == null || sequence.length == 0) {
            throw new IllegalArgumentException("sequence must not be empty");
        }
        this.sequence = Arrays.copyOf(sequence, sequence.length);
    }

    StepResult advance(int progress, char direction, boolean eligible) {
        if (!eligible) {
            return new StepResult(false, 0, false);
        }
        int safeProgress = Math.clamp(progress, 0, sequence.length);
        if (direction == 0) {
            return new StepResult(false, safeProgress, false);
        }
        int nextProgress = nextProgress(safeProgress, direction);
        if (nextProgress >= sequence.length) {
            return new StepResult(true, 0, true);
        }
        return new StepResult(true, nextProgress, false);
    }

    private int nextProgress(int progress, char direction) {
        if (progress < sequence.length && sequence[progress] == direction) {
            return progress + 1;
        }
        return sequence[0] == direction ? 1 : 0;
    }
}
