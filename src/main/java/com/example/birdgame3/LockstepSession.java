package com.example.birdgame3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Deterministic lockstep state for a LAN match.
 *
 * <p>Every participant runs the full simulation; only inputs travel the wire.
 * Each sim tick executes only once the input masks of ALL participants for
 * that tick are known. Inputs are sampled a negotiated number of ticks ahead
 * of execution, giving the network time to deliver them before anyone stalls.
 *
 * <p>Topology is a star: clients send their tick-stamped masks to the host;
 * the host assembles a per-tick bundle (one mask per slot) and broadcasts it.
 * A bundle is the authoritative input record for its tick on every machine.
 *
 * <p>Thread-safety: network reader threads call {@link #acceptMask} /
 * {@link #acceptBundle} while the JavaFX thread executes ticks, so bundle
 * state is guarded; completed bundles live in a concurrent map.
 */
final class LockstepSession {
    /** Default buffer for low-latency local-network matches. */
    static final int INPUT_DELAY_TICKS = 4;
    /** Larger buffer for direct internet matches (about 133 ms at 60 Hz). */
    static final int INTERNET_INPUT_DELAY_TICKS = 8;
    static final int MAX_SLOTS = 4;
    /** Hash-compare cadence in ticks (2 seconds at 60 Hz). */
    static final int HASH_INTERVAL_TICKS = 120;

    private final Object lock = new Object();
    private final boolean[] requiredSlot = new boolean[MAX_SLOTS];
    private final Map<Long, int[]> bundles = new ConcurrentHashMap<>();
    private final Map<Long, int[]> partialMasks = new HashMap<>();
    private final Map<Long, Integer> partialFilled = new HashMap<>();
    private final Map<Long, Long> localHashes = new ConcurrentHashMap<>();
    private final Map<Long, Long> remoteHashes = new ConcurrentHashMap<>();
    private final int inputDelayTicks;
    private long lastSampledTick;

    LockstepSession(boolean[] connectedSlots) {
        this(connectedSlots, INPUT_DELAY_TICKS);
    }

    LockstepSession(boolean[] connectedSlots, int inputDelayTicks) {
        this.inputDelayTicks = sanitizeInputDelay(inputDelayTicks);
        this.lastSampledTick = this.inputDelayTicks;
        requiredSlot[0] = true; // the host always participates
        for (int i = 1; i < MAX_SLOTS; i++) {
            requiredSlot[i] = connectedSlots != null && i < connectedSlots.length && connectedSlots[i];
        }
        // The first inputDelayTicks ticks have no sampled inputs yet; seed
        // them as all-neutral so both sides can start executing immediately.
        for (long t = 1; t <= this.inputDelayTicks; t++) {
            bundles.put(t, new int[MAX_SLOTS]);
        }
    }

    int inputDelayTicks() {
        return inputDelayTicks;
    }

    static int sanitizeInputDelay(int ticks) {
        return Math.clamp(ticks, INPUT_DELAY_TICKS, 20);
    }

    /**
     * True exactly once per target tick: the caller then samples local input
     * for that tick and ships it (client) or feeds it to assembly (host).
     */
    boolean shouldSample(long targetTick) {
        synchronized (lock) {
            if (targetTick <= lastSampledTick) {
                return false;
            }
            lastSampledTick = targetTick;
            return true;
        }
    }

    /** The authoritative input bundle for a tick, or null if not yet known. */
    int[] bundleFor(long tick) {
        return bundles.get(tick);
    }

    /**
     * Host-side: records one slot's mask for a tick. Returns the completed
     * bundle when this mask was the last one missing, else null. The caller
     * broadcasts completed bundles.
     */
    int[] acceptMask(int slot, long tick, int mask) {
        if (slot < 0 || slot >= MAX_SLOTS || tick <= 0) {
            return null;
        }
        synchronized (lock) {
            if (bundles.containsKey(tick)) {
                return null; // already complete (late duplicate)
            }
            int[] masks = partialMasks.computeIfAbsent(tick, t -> new int[MAX_SLOTS]);
            masks[slot] = mask;
            int filled = partialFilled.getOrDefault(tick, 0) | (1 << slot);
            partialFilled.put(tick, filled);
            if (!isComplete(filled)) {
                return null;
            }
            partialMasks.remove(tick);
            partialFilled.remove(tick);
            bundles.put(tick, masks);
            return masks;
        }
    }

    /** Client-side: stores a bundle received from the host. */
    void acceptBundle(long tick, int[] masks) {
        if (tick <= 0 || masks == null || masks.length < MAX_SLOTS) {
            return;
        }
        bundles.putIfAbsent(tick, masks);
    }

    /**
     * Host-side: a client left mid-match. Its slot stops being required and
     * every bundle that was only waiting on it completes with a neutral mask.
     * Returns the ticks completed by this, oldest first, for broadcasting.
     */
    List<Long> slotDisconnected(int slot) {
        List<Long> completed = new ArrayList<>();
        if (slot <= 0 || slot >= MAX_SLOTS) {
            return completed;
        }
        synchronized (lock) {
            requiredSlot[slot] = false;
            for (Map.Entry<Long, Integer> entry : new ArrayList<>(partialFilled.entrySet())) {
                if (isComplete(entry.getValue())) {
                    long tick = entry.getKey();
                    int[] masks = partialMasks.remove(tick);
                    partialFilled.remove(tick);
                    if (masks != null) {
                        bundles.put(tick, masks);
                        completed.add(tick);
                    }
                }
            }
        }
        completed.sort(Long::compare);
        return completed;
    }

    private boolean isComplete(int filled) {
        for (int i = 0; i < MAX_SLOTS; i++) {
            if (requiredSlot[i] && (filled & (1 << i)) == 0) {
                return false;
            }
        }
        return true;
    }

    /** Drops bundle/hash history well behind the executed tick. */
    void prune(long executedTick) {
        long cutoff = executedTick - 16;
        if (cutoff <= 0) {
            return;
        }
        bundles.keySet().removeIf(t -> t < cutoff);
        long hashCutoff = executedTick - HASH_INTERVAL_TICKS * 10L;
        localHashes.keySet().removeIf(t -> t < hashCutoff);
        remoteHashes.keySet().removeIf(t -> t < hashCutoff);
    }

    /**
     * Records a state hash for a tick; returns true when both sides' hashes
     * for that tick are known and disagree — a confirmed desync.
     */
    boolean recordHash(long tick, long hash, boolean fromRemote) {
        Map<Long, Long> mine = fromRemote ? remoteHashes : localHashes;
        Map<Long, Long> theirs = fromRemote ? localHashes : remoteHashes;
        mine.put(tick, hash);
        Long other = theirs.get(tick);
        return other != null && other != hash;
    }
}
