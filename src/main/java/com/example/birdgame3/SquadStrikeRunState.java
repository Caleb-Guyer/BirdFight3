package com.example.birdgame3;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.prefs.Preferences;

/** Defensive, profile-scoped checkpoint for an in-progress Squad Strike run. */
final class SquadStrikeRunState {
    static final String PREFERENCE_KEY = "squad_strike_run_v1";
    private static final String PART_COUNT_KEY = PREFERENCE_KEY + "_parts";
    private static final String PART_KEY_PREFIX = PREFERENCE_KEY + "_part_";
    private static final int FORMAT_VERSION = 1;
    private static final int MAX_PARTS = 12;
    private static final int PART_LENGTH = Preferences.MAX_VALUE_LENGTH - 64;

    record Fighter(int slot, String selectedBird, String resolvedBird, String skinKey) {
        Fighter {
            slot = Math.clamp(slot, 0, 4);
            selectedBird = safe(selectedBird, 48);
            resolvedBird = safe(resolvedBird, 48);
            skinKey = safe(skinKey, 96);
        }
    }

    final long startedAtMillis;
    final long seed;
    final String format;
    final int squadSize;
    final boolean teamAHuman;
    final boolean teamBHuman;
    final int teamACpuLevel;
    final int teamBCpuLevel;
    final boolean randomMap;
    final String fixedMap;
    final String rules;
    final int teamAIndex;
    final int teamBIndex;
    final int teamAWins;
    final int teamBWins;
    final int boutsCompleted;
    final int boutsSimulated;
    final int totalKos;
    final long totalDamage;
    final int carryTeam;
    final int carryStocks;
    final double carryHealth;
    final boolean complete;
    final int championTeam;
    final boolean rewardGranted;
    final List<Fighter> teamA;
    final List<Fighter> teamB;
    final List<Integer> winnerHistory;

    SquadStrikeRunState(long startedAtMillis, long seed, String format, int squadSize,
                        boolean teamAHuman, boolean teamBHuman, int teamACpuLevel, int teamBCpuLevel,
                        boolean randomMap, String fixedMap, String rules,
                        int teamAIndex, int teamBIndex, int teamAWins, int teamBWins,
                        int boutsCompleted, int boutsSimulated, int totalKos, long totalDamage,
                        int carryTeam, int carryStocks, double carryHealth,
                        boolean complete, int championTeam, boolean rewardGranted,
                        List<Fighter> teamA, List<Fighter> teamB, List<Integer> winnerHistory) {
        this.startedAtMillis = Math.max(0L, startedAtMillis);
        this.seed = seed;
        this.format = safe(format, 32);
        this.squadSize = squadSize <= 3 ? 3 : 5;
        this.teamAHuman = teamAHuman;
        this.teamBHuman = teamBHuman;
        this.teamACpuLevel = Math.clamp(teamACpuLevel, 1, 9);
        this.teamBCpuLevel = Math.clamp(teamBCpuLevel, 1, 9);
        this.randomMap = randomMap;
        this.fixedMap = safe(fixedMap, 64);
        this.rules = safe(rules, 4096);
        this.teamAIndex = Math.clamp(teamAIndex, 0, this.squadSize);
        this.teamBIndex = Math.clamp(teamBIndex, 0, this.squadSize);
        this.teamAWins = Math.clamp(teamAWins, 0, this.squadSize);
        this.teamBWins = Math.clamp(teamBWins, 0, this.squadSize);
        this.boutsCompleted = Math.clamp(boutsCompleted, 0, this.squadSize * 2 - 1);
        this.boutsSimulated = Math.clamp(boutsSimulated, 0, this.boutsCompleted);
        this.totalKos = Math.max(0, totalKos);
        this.totalDamage = Math.max(0L, totalDamage);
        this.carryTeam = carryTeam == 0 || carryTeam == 1 ? carryTeam : -1;
        this.carryStocks = Math.max(0, carryStocks);
        this.carryHealth = Math.max(0.0, carryHealth);
        this.complete = complete;
        this.championTeam = championTeam == 0 || championTeam == 1 ? championTeam : -1;
        this.rewardGranted = rewardGranted;
        this.teamA = sanitizeTeam(teamA, this.squadSize);
        this.teamB = sanitizeTeam(teamB, this.squadSize);
        this.winnerHistory = sanitizeHistory(winnerHistory, this.boutsCompleted);
    }

    boolean usable() {
        return ("ELIMINATION".equals(format) || "RELAY".equals(format) || "BEST_OF".equals(format))
                && teamA.size() == squadSize && teamB.size() == squadSize
                && winnerHistory.size() == boutsCompleted
                && (!complete || championTeam == 0 || championTeam == 1);
    }

    String encode() {
        return String.join("|",
                Integer.toString(FORMAT_VERSION), Long.toString(startedAtMillis), Long.toString(seed),
                format, Integer.toString(squadSize), Boolean.toString(teamAHuman), Boolean.toString(teamBHuman),
                Integer.toString(teamACpuLevel), Integer.toString(teamBCpuLevel), Boolean.toString(randomMap),
                b64(fixedMap), b64(rules), Integer.toString(teamAIndex), Integer.toString(teamBIndex),
                Integer.toString(teamAWins), Integer.toString(teamBWins), Integer.toString(boutsCompleted),
                Integer.toString(boutsSimulated), Integer.toString(totalKos), Long.toString(totalDamage),
                Integer.toString(carryTeam), Integer.toString(carryStocks), Double.toString(carryHealth),
                Boolean.toString(complete), Integer.toString(championTeam), Boolean.toString(rewardGranted),
                b64(encodeTeam(teamA)), b64(encodeTeam(teamB)), joinInts(winnerHistory));
    }

    static SquadStrikeRunState decode(String encoded) {
        if (encoded == null || encoded.isBlank()) return null;
        try {
            String[] p = encoded.split("\\|", -1);
            if (p.length != 29 || Integer.parseInt(p[0]) != FORMAT_VERSION) return null;
            SquadStrikeRunState state = new SquadStrikeRunState(
                    Long.parseLong(p[1]), Long.parseLong(p[2]), p[3], Integer.parseInt(p[4]),
                    Boolean.parseBoolean(p[5]), Boolean.parseBoolean(p[6]), Integer.parseInt(p[7]),
                    Integer.parseInt(p[8]), Boolean.parseBoolean(p[9]), fromB64(p[10]), fromB64(p[11]),
                    Integer.parseInt(p[12]), Integer.parseInt(p[13]), Integer.parseInt(p[14]),
                    Integer.parseInt(p[15]), Integer.parseInt(p[16]), Integer.parseInt(p[17]),
                    Integer.parseInt(p[18]), Long.parseLong(p[19]), Integer.parseInt(p[20]),
                    Integer.parseInt(p[21]), Double.parseDouble(p[22]), Boolean.parseBoolean(p[23]),
                    Integer.parseInt(p[24]), Boolean.parseBoolean(p[25]), decodeTeam(fromB64(p[26])),
                    decodeTeam(fromB64(p[27])), parseInts(p[28]));
            return state.usable() ? state : null;
        } catch (IllegalArgumentException | IndexOutOfBoundsException ignored) {
            return null;
        }
    }

    static SquadStrikeRunState loadFrom(Preferences prefs) {
        if (prefs == null) return null;
        int count = prefs.getInt(PART_COUNT_KEY, 0);
        if (count > 0 && count <= MAX_PARTS) {
            StringBuilder encoded = new StringBuilder(count * PART_LENGTH);
            for (int i = 0; i < count; i++) {
                String part = prefs.get(PART_KEY_PREFIX + i, null);
                if (part == null) return null;
                encoded.append(part);
            }
            return decode(encoded.toString());
        }
        return decode(prefs.get(PREFERENCE_KEY, ""));
    }

    static void saveTo(Preferences prefs, SquadStrikeRunState state) {
        if (prefs == null) return;
        int oldParts = Math.clamp(prefs.getInt(PART_COUNT_KEY, 0), 0, MAX_PARTS);
        if (state == null || !state.usable()) {
            prefs.remove(PREFERENCE_KEY);
            prefs.remove(PART_COUNT_KEY);
            for (int i = 0; i < oldParts; i++) prefs.remove(PART_KEY_PREFIX + i);
            return;
        }
        String encoded = state.encode();
        int count = (encoded.length() + PART_LENGTH - 1) / PART_LENGTH;
        if (count <= 0 || count > MAX_PARTS) throw new IllegalArgumentException("Squad Strike save is too large");
        for (int i = 0; i < count; i++) {
            int start = i * PART_LENGTH;
            prefs.put(PART_KEY_PREFIX + i, encoded.substring(start, Math.min(encoded.length(), start + PART_LENGTH)));
        }
        for (int i = count; i < oldParts; i++) prefs.remove(PART_KEY_PREFIX + i);
        prefs.putInt(PART_COUNT_KEY, count);
        prefs.remove(PREFERENCE_KEY);
    }

    private static String encodeTeam(List<Fighter> team) {
        return team.stream().map(f -> String.join("~", Integer.toString(f.slot()), b64(f.selectedBird()),
                b64(f.resolvedBird()), b64(f.skinKey()))).reduce((a, b) -> a + "," + b).orElse("");
    }

    private static List<Fighter> decodeTeam(String text) {
        List<Fighter> result = new ArrayList<>();
        if (text == null || text.isBlank()) return result;
        for (String token : text.split(",")) {
            String[] f = token.split("~", -1);
            if (f.length != 4) throw new IllegalArgumentException("Invalid Squad Strike fighter");
            result.add(new Fighter(Integer.parseInt(f[0]), fromB64(f[1]), fromB64(f[2]), fromB64(f[3])));
        }
        return result;
    }

    private static List<Fighter> sanitizeTeam(List<Fighter> source, int size) {
        if (source == null) return List.of();
        Fighter[] slots = new Fighter[size];
        for (Fighter fighter : source) {
            if (fighter != null && fighter.slot() < size && slots[fighter.slot()] == null) slots[fighter.slot()] = fighter;
        }
        List<Fighter> result = new ArrayList<>(size);
        for (Fighter fighter : slots) if (fighter != null) result.add(fighter);
        return List.copyOf(result);
    }

    private static List<Integer> sanitizeHistory(List<Integer> source, int completed) {
        if (source == null) return List.of();
        List<Integer> result = new ArrayList<>(Math.min(source.size(), completed));
        for (Integer team : source) {
            if (result.size() >= completed) break;
            if (team != null && (team == 0 || team == 1)) result.add(team);
        }
        return List.copyOf(result);
    }

    private static String safe(String value, int maxLength) {
        if (value == null) return "";
        String trimmed = value.trim();
        return trimmed.length() <= maxLength ? trimmed : trimmed.substring(0, maxLength).trim();
    }

    private static String b64(String value) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString((value == null ? "" : value)
                .getBytes(StandardCharsets.UTF_8));
    }

    private static String fromB64(String value) {
        if (value == null || value.isBlank()) return "";
        return new String(Base64.getUrlDecoder().decode(value), StandardCharsets.UTF_8);
    }

    private static String joinInts(List<Integer> values) {
        if (values == null || values.isEmpty()) return "";
        return values.stream().map(String::valueOf).reduce((a, b) -> a + "," + b).orElse("");
    }

    private static List<Integer> parseInts(String text) {
        if (text == null || text.isBlank()) return List.of();
        List<Integer> result = new ArrayList<>();
        for (String part : text.split(",")) result.add(Integer.parseInt(part));
        return result;
    }
}
