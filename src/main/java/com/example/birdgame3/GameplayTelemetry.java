package com.example.birdgame3;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class GameplayTelemetry {
    static final String MATCH_SURVIVAL_MOVE = "Match Survival";
    static final String RECOVERY_FAILURE_MOVE = "Recovery Failure";

    private final Map<MoveKey, MoveStats> moves = new LinkedHashMap<>();
    private final Map<MoveKey, MoveStats> currentMatchMoves = new LinkedHashMap<>();

    void recordUse(BirdGame3.BirdType birdType, String moveName, boolean cpu,
                   String mode, BirdGame3.MapType map) {
        recordUse(statsFor(moves, birdType, moveName, mode, map), cpu);
        recordUse(statsFor(currentMatchMoves, birdType, moveName, mode, map), cpu);
    }

    void recordImpact(BirdGame3.BirdType birdType, String moveName, boolean cpu,
                      String mode, BirdGame3.MapType map, int damage, boolean hit) {
        recordImpact(statsFor(moves, birdType, moveName, mode, map), damage, hit);
        recordImpact(statsFor(currentMatchMoves, birdType, moveName, mode, map), damage, hit);
    }

    void recordKo(BirdGame3.BirdType birdType, String moveName, boolean cpu,
                  String mode, BirdGame3.MapType map, boolean selfKo) {
        recordKo(statsFor(moves, birdType, moveName, mode, map), selfKo);
        recordKo(statsFor(currentMatchMoves, birdType, moveName, mode, map), selfKo);
    }

    void recordRecoveryFailure(BirdGame3.BirdType birdType, boolean cpu,
                               String mode, BirdGame3.MapType map) {
        recordRecoveryFailure(statsFor(moves, birdType, RECOVERY_FAILURE_MOVE, mode, map), cpu);
        recordRecoveryFailure(statsFor(currentMatchMoves, birdType, RECOVERY_FAILURE_MOVE, mode, map), cpu);
    }

    void recordSurvival(BirdGame3.BirdType birdType, boolean cpu, String mode,
                        BirdGame3.MapType map, int frames) {
        recordSurvival(statsFor(moves, birdType, MATCH_SURVIVAL_MOVE, mode, map), cpu, frames);
        recordSurvival(statsFor(currentMatchMoves, birdType, MATCH_SURVIVAL_MOVE, mode, map), cpu, frames);
    }

    void resetCurrentMatch() {
        currentMatchMoves.clear();
    }

    List<MoveSnapshot> topMoves(int limit) {
        return topMoves(moves, limit);
    }

    List<MoveSnapshot> topMovesForBird(BirdGame3.BirdType birdType, int limit) {
        if (birdType == null || limit <= 0) {
            return List.of();
        }
        Map<MoveKey, MoveStats> filtered = new LinkedHashMap<>();
        for (Map.Entry<MoveKey, MoveStats> entry : moves.entrySet()) {
            if (entry.getKey().birdType() == birdType) {
                filtered.put(entry.getKey(), entry.getValue());
            }
        }
        return topMoves(filtered, limit);
    }

    List<MoveSnapshot> currentMatchTopMoves(int limit) {
        return topMoves(currentMatchMoves, limit);
    }

    List<BirdSnapshot> currentMatchBirds() {
        return birdSnapshots(currentMatchMoves);
    }

    private void recordUse(MoveStats stats, boolean cpu) {
        if (cpu) {
            stats.cpuUses++;
        } else {
            stats.humanUses++;
        }
    }

    private void recordImpact(MoveStats stats, int damage, boolean hit) {
        if (hit) {
            stats.hits++;
        }
        if (damage > 0) {
            stats.damage += damage;
        }
    }

    private void recordKo(MoveStats stats, boolean selfKo) {
        if (selfKo) {
            stats.selfKos++;
        } else {
            stats.kos++;
        }
    }

    private void recordRecoveryFailure(MoveStats stats, boolean cpu) {
        stats.recoveryFailures++;
        stats.selfKos++;
        if (cpu) {
            stats.cpuUses++;
        } else {
            stats.humanUses++;
        }
    }

    private void recordSurvival(MoveStats stats, boolean cpu, int frames) {
        stats.survivalFrames += Math.max(0, frames);
        stats.survivalSamples++;
        if (cpu) {
            stats.cpuUses++;
        } else {
            stats.humanUses++;
        }
    }

    private List<MoveSnapshot> topMoves(Map<MoveKey, MoveStats> source, int limit) {
        List<MoveSnapshot> rows = new ArrayList<>();
        for (Map.Entry<MoveKey, MoveStats> entry : source.entrySet()) {
            rows.add(entry.getValue().snapshot(entry.getKey()));
        }
        rows.sort(Comparator
                .comparingInt(MoveSnapshot::damage).reversed()
                .thenComparing(Comparator.comparingInt(MoveSnapshot::kos).reversed())
                .thenComparing(Comparator.comparingInt(MoveSnapshot::hits).reversed())
                .thenComparing(Comparator.comparingInt(MoveSnapshot::uses).reversed()));
        if (rows.size() <= limit) {
            return rows;
        }
        return List.copyOf(rows.subList(0, Math.max(0, limit)));
    }

    private List<BirdSnapshot> birdSnapshots(Map<MoveKey, MoveStats> source) {
        Map<BirdGame3.BirdType, BirdStats> birds = new LinkedHashMap<>();
        for (Map.Entry<MoveKey, MoveStats> entry : source.entrySet()) {
            BirdStats stats = birds.computeIfAbsent(entry.getKey().birdType(), ignored -> new BirdStats());
            stats.add(entry.getValue());
        }

        List<BirdSnapshot> rows = new ArrayList<>();
        for (Map.Entry<BirdGame3.BirdType, BirdStats> entry : birds.entrySet()) {
            rows.add(entry.getValue().snapshot(entry.getKey()));
        }
        rows.sort(Comparator
                .comparingInt(BirdSnapshot::damage).reversed()
                .thenComparing(Comparator.comparingInt(BirdSnapshot::kos).reversed())
                .thenComparing(Comparator.comparingInt(BirdSnapshot::recoveryFailures).reversed())
                .thenComparing(Comparator.comparingInt(BirdSnapshot::selfKos).reversed())
                .thenComparing(BirdSnapshot::birdName));
        return rows;
    }

    private MoveStats statsFor(Map<MoveKey, MoveStats> source,
                               BirdGame3.BirdType birdType, String moveName,
                               String mode, BirdGame3.MapType map) {
        MoveKey key = new MoveKey(
                birdType == null ? BirdGame3.BirdType.PIGEON : birdType,
                safe(moveName, "Unknown Move"),
                safe(mode, "Unknown"),
                map == null ? BirdGame3.MapType.FOREST : map
        );
        return source.computeIfAbsent(key, ignored -> new MoveStats());
    }

    private static String safe(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private record MoveKey(BirdGame3.BirdType birdType, String moveName,
                           String mode, BirdGame3.MapType map) {
    }

    private static final class MoveStats {
        int humanUses;
        int cpuUses;
        int hits;
        int damage;
        int kos;
        int selfKos;
        int recoveryFailures;
        long survivalFrames;
        int survivalSamples;

        MoveSnapshot snapshot(MoveKey key) {
            return new MoveSnapshot(
                    key.birdType().name,
                    key.moveName(),
                    key.mode(),
                    key.map().name(),
                    humanUses,
                    cpuUses,
                    hits,
                    damage,
                    kos,
                    selfKos,
                    recoveryFailures,
                    survivalSamples <= 0 ? 0.0 : survivalFrames / (survivalSamples * 60.0)
            );
        }
    }

    private static final class BirdStats {
        int humanUses;
        int cpuUses;
        int hits;
        int damage;
        int kos;
        int selfKos;
        int recoveryFailures;
        long survivalFrames;
        int survivalSamples;

        void add(MoveStats stats) {
            humanUses += stats.humanUses;
            cpuUses += stats.cpuUses;
            hits += stats.hits;
            damage += stats.damage;
            kos += stats.kos;
            selfKos += stats.selfKos;
            recoveryFailures += stats.recoveryFailures;
            survivalFrames += stats.survivalFrames;
            survivalSamples += stats.survivalSamples;
        }

        BirdSnapshot snapshot(BirdGame3.BirdType birdType) {
            return new BirdSnapshot(
                    birdType.name,
                    humanUses,
                    cpuUses,
                    hits,
                    damage,
                    kos,
                    selfKos,
                    recoveryFailures,
                    survivalSamples <= 0 ? 0.0 : survivalFrames / (survivalSamples * 60.0)
            );
        }
    }

    record MoveSnapshot(String birdName, String moveName, String mode, String map,
                        int humanUses, int cpuUses, int hits, int damage,
                        int kos, int selfKos, int recoveryFailures,
                        double averageSurvivalSeconds) {
        int uses() {
            return humanUses + cpuUses;
        }
    }

    record BirdSnapshot(String birdName, int humanUses, int cpuUses, int hits,
                        int damage, int kos, int selfKos, int recoveryFailures,
                        double averageSurvivalSeconds) {
        int uses() {
            return humanUses + cpuUses;
        }
    }
}
