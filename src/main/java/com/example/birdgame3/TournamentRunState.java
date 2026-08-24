package com.example.birdgame3;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.prefs.Preferences;

/**
 * Compact, defensive save payload for an in-progress local tournament.
 *
 * <p>The UI model deliberately stores enum values as names. A removed bird,
 * skin, map, or rules option can therefore be rejected by the game while the
 * rest of the bracket still restores safely.</p>
 */
final class TournamentRunState {
    static final String PREFERENCE_KEY = "tournament_run_v2";
    private static final String PART_COUNT_KEY = PREFERENCE_KEY + "_parts";
    private static final String PART_KEY_PREFIX = PREFERENCE_KEY + "_part_";
    private static final int FORMAT_VERSION = 2;
    private static final int MAX_PARTS = 16;
    private static final int PART_LENGTH = Preferences.MAX_VALUE_LENGTH - 64;

    record Entry(int id, boolean human, String selectedBird, String resolvedBird,
                 String skinKey, String customName, int cpuLevel) {
        Entry {
            id = Math.clamp(id, 1, 32);
            selectedBird = safe(selectedBird, 48);
            resolvedBird = safe(resolvedBird, 48);
            skinKey = safe(skinKey, 96);
            customName = safe(customName, 32);
            cpuLevel = Math.clamp(cpuLevel, 1, 9);
        }
    }

    final long startedAtMillis;
    final int entrantCount;
    final int humanCount;
    final boolean randomMap;
    final String fixedMap;
    final String rules;
    final int completedMatches;
    final int simulatedMatches;
    final int totalKos;
    final long totalDamage;
    final boolean rewardGranted;
    final List<Entry> entries;
    final List<Integer> seedOrder;
    final List<Integer> winners;

    TournamentRunState(long startedAtMillis, int entrantCount, int humanCount,
                       boolean randomMap, String fixedMap, String rules,
                       int completedMatches, int simulatedMatches, int totalKos,
                       long totalDamage, boolean rewardGranted, List<Entry> entries,
                       List<Integer> seedOrder, List<Integer> winners) {
        this.startedAtMillis = Math.max(0L, startedAtMillis);
        this.entrantCount = Math.clamp(entrantCount, 2, 32);
        this.humanCount = Math.clamp(humanCount, 0, this.entrantCount);
        this.randomMap = randomMap;
        this.fixedMap = safe(fixedMap, 64);
        this.rules = safe(rules, 4096);
        this.completedMatches = Math.clamp(completedMatches, 0, this.entrantCount - 1);
        this.simulatedMatches = Math.clamp(simulatedMatches, 0, this.completedMatches);
        this.totalKos = Math.max(0, totalKos);
        this.totalDamage = Math.max(0L, totalDamage);
        this.rewardGranted = rewardGranted;
        this.entries = sanitizeEntries(entries, this.entrantCount);
        this.seedOrder = sanitizeIds(seedOrder, this.entries);
        this.winners = sanitizeWinnerIds(winners, this.entries);
    }

    boolean usable() {
        return entries.size() == entrantCount && seedOrder.size() == entrantCount;
    }

    String encode() {
        String entriesText = entries.stream().map(entry -> String.join("~",
                Integer.toString(entry.id()),
                Boolean.toString(entry.human()),
                b64(entry.selectedBird()),
                b64(entry.resolvedBird()),
                b64(entry.skinKey()),
                b64(entry.customName()),
                Integer.toString(entry.cpuLevel()))).reduce((a, b) -> a + "," + b).orElse("");
        return String.join("|",
                Integer.toString(FORMAT_VERSION),
                Long.toString(startedAtMillis),
                Integer.toString(entrantCount),
                Integer.toString(humanCount),
                Boolean.toString(randomMap),
                b64(fixedMap),
                b64(rules),
                Integer.toString(completedMatches),
                Integer.toString(simulatedMatches),
                Integer.toString(totalKos),
                Long.toString(totalDamage),
                Boolean.toString(rewardGranted),
                b64(entriesText),
                joinInts(seedOrder),
                joinInts(winners));
    }

    static TournamentRunState decode(String encoded) {
        if (encoded == null || encoded.isBlank()) return null;
        try {
            String[] parts = encoded.split("\\|", -1);
            if (parts.length != 15 || Integer.parseInt(parts[0]) != FORMAT_VERSION) return null;
            List<Entry> entries = new ArrayList<>();
            String entriesText = fromB64(parts[12]);
            if (!entriesText.isBlank()) {
                for (String token : entriesText.split(",")) {
                    String[] fields = token.split("~", -1);
                    if (fields.length != 7) return null;
                    entries.add(new Entry(Integer.parseInt(fields[0]), Boolean.parseBoolean(fields[1]),
                            fromB64(fields[2]), fromB64(fields[3]), fromB64(fields[4]),
                            fromB64(fields[5]), Integer.parseInt(fields[6])));
                }
            }
            TournamentRunState state = new TournamentRunState(
                    Long.parseLong(parts[1]), Integer.parseInt(parts[2]), Integer.parseInt(parts[3]),
                    Boolean.parseBoolean(parts[4]), fromB64(parts[5]), fromB64(parts[6]),
                    Integer.parseInt(parts[7]), Integer.parseInt(parts[8]), Integer.parseInt(parts[9]),
                    Long.parseLong(parts[10]), Boolean.parseBoolean(parts[11]), entries,
                    parseInts(parts[13]), parseInts(parts[14]));
            return state.usable() ? state : null;
        } catch (IllegalArgumentException | IndexOutOfBoundsException ignored) {
            return null;
        }
    }

    static TournamentRunState loadFrom(Preferences prefs) {
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

    static void saveTo(Preferences prefs, TournamentRunState state) {
        if (prefs == null) return;
        int previousParts = Math.clamp(prefs.getInt(PART_COUNT_KEY, 0), 0, MAX_PARTS);
        if (state == null || !state.usable()) {
            prefs.remove(PREFERENCE_KEY);
            prefs.remove(PART_COUNT_KEY);
            for (int i = 0; i < previousParts; i++) prefs.remove(PART_KEY_PREFIX + i);
            return;
        }
        String encoded = state.encode();
        int count = (encoded.length() + PART_LENGTH - 1) / PART_LENGTH;
        if (count <= 0 || count > MAX_PARTS) {
            throw new IllegalArgumentException("Tournament save exceeds the supported preference payload");
        }
        for (int i = 0; i < count; i++) {
            int start = i * PART_LENGTH;
            prefs.put(PART_KEY_PREFIX + i, encoded.substring(start, Math.min(encoded.length(), start + PART_LENGTH)));
        }
        for (int i = count; i < previousParts; i++) prefs.remove(PART_KEY_PREFIX + i);
        prefs.putInt(PART_COUNT_KEY, count);
        prefs.remove(PREFERENCE_KEY);
    }

    private static List<Entry> sanitizeEntries(List<Entry> source, int entrantCount) {
        if (source == null) return List.of();
        List<Entry> result = new ArrayList<>(entrantCount);
        Set<Integer> ids = new HashSet<>();
        for (Entry entry : source) {
            if (entry == null || entry.id() > entrantCount || !ids.add(entry.id())) continue;
            result.add(entry);
        }
        return List.copyOf(result);
    }

    private static List<Integer> sanitizeIds(List<Integer> source, List<Entry> entries) {
        if (source == null) return List.of();
        Set<Integer> valid = new HashSet<>();
        for (Entry entry : entries) valid.add(entry.id());
        List<Integer> result = new ArrayList<>(valid.size());
        Set<Integer> seen = new HashSet<>();
        for (Integer id : source) {
            if (id != null && valid.contains(id) && seen.add(id)) result.add(id);
        }
        return List.copyOf(result);
    }

    private static List<Integer> sanitizeWinnerIds(List<Integer> source, List<Entry> entries) {
        if (source == null) return List.of();
        Set<Integer> valid = new HashSet<>();
        for (Entry entry : entries) valid.add(entry.id());
        List<Integer> result = new ArrayList<>(source.size());
        for (Integer id : source) result.add(id != null && valid.contains(id) ? id : 0);
        return List.copyOf(result);
    }

    private static String safe(String value, int maxLength) {
        if (value == null) return "";
        String trimmed = value.trim();
        return trimmed.length() <= maxLength ? trimmed : trimmed.substring(0, maxLength).trim();
    }

    private static String b64(String value) {
        return Base64.getUrlEncoder().withoutPadding()
                .encodeToString((value == null ? "" : value).getBytes(StandardCharsets.UTF_8));
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
        List<Integer> values = new ArrayList<>();
        for (String part : text.split(",")) values.add(Integer.parseInt(part));
        return values;
    }
}
