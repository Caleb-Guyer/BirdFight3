package com.example.birdgame3;

import java.time.LocalDate;
import java.time.temporal.WeekFields;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.prefs.Preferences;

final class ContractsBoard {
    private static final String PREF_DAILY_KEY = "contracts_daily_key";
    private static final String PREF_DAILY_IDS = "contracts_daily_ids";
    private static final String PREF_DAILY_STATE_PREFIX = "contracts_daily_state_";
    private static final String PREF_WEEKLY_KEY = "contracts_weekly_key";
    private static final String PREF_WEEKLY_ID = "contracts_weekly_id";
    private static final String PREF_WEEKLY_STATE = "contracts_weekly_state";
    private static final long DAILY_SALT = 0xA24BAED4963EE407L;
    private static final long WEEKLY_SALT = 0x9FB21C651E98DF25L;

    private static final List<ContractDefinition> DEFAULT_DAILY_POOL = List.of(
            new ContractDefinition("daily_matches", ContractFrequency.DAILY, ContractMetric.MATCHES,
                    "Open Skies", "Play 3 completed matches.", 3, 120, 10),
            new ContractDefinition("daily_wins", ContractFrequency.DAILY, ContractMetric.WINS,
                    "Winner's Circle", "Win 2 matches.", 2, 140, 14),
            new ContractDefinition("daily_damage", ContractFrequency.DAILY, ContractMetric.DAMAGE,
                    "Heavy Feathers", "Deal 450 total damage.", 450, 130, 12),
            new ContractDefinition("daily_elims", ContractFrequency.DAILY, ContractMetric.ELIMINATIONS,
                    "Sky Hunter", "Score 4 eliminations.", 4, 150, 14),
            new ContractDefinition("daily_specials", ContractFrequency.DAILY, ContractMetric.SPECIALS_USED,
                    "Special Delivery", "Use 10 specials.", 10, 110, 10),
            new ContractDefinition("daily_special_hits", ContractFrequency.DAILY, ContractMetric.SPECIAL_HITS,
                    "Pinpoint Royale", "Land 6 special hits.", 6, 130, 12)
    );

    private static final List<ContractDefinition> DEFAULT_WEEKLY_POOL = List.of(
            new ContractDefinition("weekly_matches", ContractFrequency.WEEKLY, ContractMetric.MATCHES,
                    "Wing Marathon", "Play 10 completed matches this week.", 10, 320, 22),
            new ContractDefinition("weekly_damage", ContractFrequency.WEEKLY, ContractMetric.DAMAGE,
                    "Featherstorm Week", "Deal 2200 total damage this week.", 2200, 340, 24),
            new ContractDefinition("weekly_elims", ContractFrequency.WEEKLY, ContractMetric.ELIMINATIONS,
                    "Ace Finisher", "Score 18 eliminations this week.", 18, 360, 24),
            new ContractDefinition("weekly_map_wins", ContractFrequency.WEEKLY, ContractMetric.UNIQUE_MAP_WINS,
                    "World Tour", "Win on 3 different maps this week.", 3, 380, 26)
    );

    enum ContractFrequency {
        DAILY,
        WEEKLY
    }

    enum ContractMetric {
        MATCHES,
        WINS,
        DAMAGE,
        ELIMINATIONS,
        SPECIALS_USED,
        SPECIAL_HITS,
        UNIQUE_MAP_WINS
    }

    record ContractDefinition(
            String id,
            ContractFrequency frequency,
            ContractMetric metric,
            String title,
            String description,
            int target,
            int rewardCoins,
            int rewardMasteryXp
    ) {
        ContractDefinition {
            Objects.requireNonNull(id, "id");
            Objects.requireNonNull(frequency, "frequency");
            Objects.requireNonNull(metric, "metric");
            Objects.requireNonNull(title, "title");
            Objects.requireNonNull(description, "description");
            target = Math.max(1, target);
            rewardCoins = Math.max(0, rewardCoins);
            rewardMasteryXp = Math.max(0, rewardMasteryXp);
        }
    }

    record ContractView(
            ContractFrequency frequency,
            String id,
            String title,
            String description,
            int progress,
            int target,
            boolean completed,
            int rewardCoins,
            int rewardMasteryXp
    ) {
        String cadenceLabel() {
            return frequency == ContractFrequency.DAILY ? "DAILY" : "WEEKLY";
        }

        String progressText() {
            return progress + "/" + target;
        }

        String rewardText() {
            return "+" + rewardCoins + " coins  |  +" + rewardMasteryXp + " mastery XP";
        }
    }

    record RewardEvent(ContractView contract, int rewardCoins, int rewardMasteryXp) {
    }

    record MatchStats(
            boolean localWin,
            BirdGame3.MapType map,
            int matchesPlayed,
            int wins,
            int damage,
            int eliminations,
            int specialsUsed,
            int specialHits
    ) {
        MatchStats {
            matchesPlayed = Math.max(0, matchesPlayed);
            wins = Math.max(0, wins);
            damage = Math.max(0, damage);
            eliminations = Math.max(0, eliminations);
            specialsUsed = Math.max(0, specialsUsed);
            specialHits = Math.max(0, specialHits);
        }
    }

    record UpdateResult(
            List<ContractView> updatedContracts,
            List<RewardEvent> completedContracts,
            int coinsAwarded,
            int masteryXpAwarded
    ) {
        UpdateResult {
            updatedContracts = List.copyOf(updatedContracts);
            completedContracts = List.copyOf(completedContracts);
            coinsAwarded = Math.max(0, coinsAwarded);
            masteryXpAwarded = Math.max(0, masteryXpAwarded);
        }

        static UpdateResult empty() {
            return new UpdateResult(List.of(), List.of(), 0, 0);
        }

        boolean hasChanges() {
            return !updatedContracts.isEmpty() || !completedContracts.isEmpty();
        }
    }

    private final List<ContractDefinition> dailyPool;
    private final List<ContractDefinition> weeklyPool;
    private final Map<String, ContractDefinition> dailyById;
    private final Map<String, ContractDefinition> weeklyById;
    private final List<ContractState> dailyContracts = new ArrayList<>();
    private ContractState weeklyContract;
    private String dailyRotationKey = "";
    private String weeklyRotationKey = "";

    ContractsBoard() {
        this(DEFAULT_DAILY_POOL, DEFAULT_WEEKLY_POOL);
    }

    ContractsBoard(List<ContractDefinition> dailyPool, List<ContractDefinition> weeklyPool) {
        if (dailyPool == null || dailyPool.size() < 3) {
            throw new IllegalArgumentException("Daily contract pool must contain at least 3 entries.");
        }
        if (weeklyPool == null || weeklyPool.isEmpty()) {
            throw new IllegalArgumentException("Weekly contract pool must contain at least 1 entry.");
        }
        this.dailyPool = List.copyOf(dailyPool);
        this.weeklyPool = List.copyOf(weeklyPool);
        this.dailyById = indexById(this.dailyPool);
        this.weeklyById = indexById(this.weeklyPool);
    }

    void load(Preferences prefs, LocalDate today) {
        dailyRotationKey = prefs.get(PREF_DAILY_KEY, "");
        weeklyRotationKey = prefs.get(PREF_WEEKLY_KEY, "");

        dailyContracts.clear();
        List<ContractDefinition> storedDaily = resolveDefinitions(dailyById, prefs.get(PREF_DAILY_IDS, ""));
        if (storedDaily != null) {
            for (int i = 0; i < storedDaily.size(); i++) {
                dailyContracts.add(ContractState.restore(storedDaily.get(i), prefs.get(PREF_DAILY_STATE_PREFIX + i, "")));
            }
        }

        ContractDefinition storedWeekly = resolveDefinition(weeklyById, prefs.get(PREF_WEEKLY_ID, ""));
        weeklyContract = storedWeekly == null
                ? null
                : ContractState.restore(storedWeekly, prefs.get(PREF_WEEKLY_STATE, ""));

        refresh(today);
    }

    void save(Preferences prefs) {
        prefs.put(PREF_DAILY_KEY, dailyRotationKey == null ? "" : dailyRotationKey);
        prefs.put(PREF_WEEKLY_KEY, weeklyRotationKey == null ? "" : weeklyRotationKey);

        if (dailyContracts.size() == 3) {
            prefs.put(PREF_DAILY_IDS, joinIds(dailyContracts));
            for (int i = 0; i < dailyContracts.size(); i++) {
                prefs.put(PREF_DAILY_STATE_PREFIX + i, dailyContracts.get(i).serialize());
            }
        } else {
            prefs.remove(PREF_DAILY_IDS);
            for (int i = 0; i < 3; i++) {
                prefs.remove(PREF_DAILY_STATE_PREFIX + i);
            }
        }

        if (weeklyContract != null) {
            prefs.put(PREF_WEEKLY_ID, weeklyContract.definition.id());
            prefs.put(PREF_WEEKLY_STATE, weeklyContract.serialize());
        } else {
            prefs.remove(PREF_WEEKLY_ID);
            prefs.remove(PREF_WEEKLY_STATE);
        }
    }

    boolean refresh(LocalDate today) {
        boolean changed = false;

        String currentDailyKey = dailyKeyFor(today);
        if (!currentDailyKey.equals(dailyRotationKey) || dailyContracts.size() != 3) {
            dailyRotationKey = currentDailyKey;
            dailyContracts.clear();
            for (ContractDefinition definition : pickContracts(dailyPool, 3, dailyRotationKey, DAILY_SALT)) {
                dailyContracts.add(new ContractState(definition));
            }
            changed = true;
        } else {
            for (ContractState contract : dailyContracts) {
                changed |= contract.normalize();
            }
        }

        String currentWeeklyKey = weeklyKeyFor(today);
        if (!currentWeeklyKey.equals(weeklyRotationKey) || weeklyContract == null) {
            weeklyRotationKey = currentWeeklyKey;
            weeklyContract = new ContractState(pickContracts(weeklyPool, 1, weeklyRotationKey, WEEKLY_SALT).getFirst());
            changed = true;
        } else {
            changed |= weeklyContract.normalize();
        }

        return changed;
    }

    List<ContractView> activeContracts() {
        List<ContractView> views = new ArrayList<>(dailyContracts.size() + 1);
        for (ContractState contract : dailyContracts) {
            views.add(contract.toView());
        }
        if (weeklyContract != null) {
            views.add(weeklyContract.toView());
        }
        return List.copyOf(views);
    }

    UpdateResult applyMatch(MatchStats stats) {
        if (stats == null || stats.matchesPlayed() <= 0) {
            return UpdateResult.empty();
        }

        List<ContractView> updated = new ArrayList<>();
        List<RewardEvent> completed = new ArrayList<>();
        int coinRewards = 0;
        int masteryRewards = 0;

        for (ContractState contract : dailyContracts) {
            ProgressResult result = contract.apply(stats);
            if (result.changed()) {
                updated.add(result.view());
            }
            if (result.completedNow()) {
                coinRewards += contract.definition.rewardCoins();
                masteryRewards += contract.definition.rewardMasteryXp();
                completed.add(new RewardEvent(result.view(), contract.definition.rewardCoins(), contract.definition.rewardMasteryXp()));
            }
        }

        if (weeklyContract != null) {
            ProgressResult result = weeklyContract.apply(stats);
            if (result.changed()) {
                updated.add(result.view());
            }
            if (result.completedNow()) {
                coinRewards += weeklyContract.definition.rewardCoins();
                masteryRewards += weeklyContract.definition.rewardMasteryXp();
                completed.add(new RewardEvent(result.view(), weeklyContract.definition.rewardCoins(), weeklyContract.definition.rewardMasteryXp()));
            }
        }

        if (updated.isEmpty() && completed.isEmpty()) {
            return UpdateResult.empty();
        }
        return new UpdateResult(updated, completed, coinRewards, masteryRewards);
    }

    private static Map<String, ContractDefinition> indexById(List<ContractDefinition> definitions) {
        Map<String, ContractDefinition> byId = new LinkedHashMap<>();
        for (ContractDefinition definition : definitions) {
            ContractDefinition previous = byId.put(definition.id(), definition);
            if (previous != null) {
                throw new IllegalArgumentException("Duplicate contract id: " + definition.id());
            }
        }
        return Collections.unmodifiableMap(byId);
    }

    private static List<ContractDefinition> resolveDefinitions(Map<String, ContractDefinition> byId, String encodedIds) {
        if (encodedIds == null || encodedIds.isBlank()) return null;
        String[] ids = encodedIds.split(",");
        if (ids.length != 3) return null;
        List<ContractDefinition> resolved = new ArrayList<>(3);
        for (String rawId : ids) {
            ContractDefinition definition = resolveDefinition(byId, rawId);
            if (definition == null || resolved.contains(definition)) {
                return null;
            }
            resolved.add(definition);
        }
        return resolved;
    }

    private static ContractDefinition resolveDefinition(Map<String, ContractDefinition> byId, String rawId) {
        if (rawId == null || rawId.isBlank()) return null;
        return byId.get(rawId.trim());
    }

    private static String joinIds(List<ContractState> contracts) {
        List<String> ids = new ArrayList<>(contracts.size());
        for (ContractState contract : contracts) {
            ids.add(contract.definition.id());
        }
        return String.join(",", ids);
    }

    private static List<ContractDefinition> pickContracts(List<ContractDefinition> pool, int count, String rotationKey, long salt) {
        List<ContractDefinition> shuffled = new ArrayList<>(pool);
        Collections.shuffle(shuffled, new Random(seedFor(rotationKey, salt)));
        return List.copyOf(shuffled.subList(0, count));
    }

    private static long seedFor(String key, long salt) {
        long seed = salt;
        String safeKey = key == null ? "" : key;
        for (int i = 0; i < safeKey.length(); i++) {
            seed ^= safeKey.charAt(i);
            seed = Long.rotateLeft(seed, 9) * 0x9E3779B97F4A7C15L;
        }
        return seed;
    }

    private static String dailyKeyFor(LocalDate date) {
        return date == null ? "" : date.toString();
    }

    private static String weeklyKeyFor(LocalDate date) {
        if (date == null) return "";
        WeekFields weekFields = WeekFields.ISO;
        int week = date.get(weekFields.weekOfWeekBasedYear());
        int year = date.get(weekFields.weekBasedYear());
        return year + "-W" + String.format(Locale.ROOT, "%02d", week);
    }

    private static final class ContractState {
        private final ContractDefinition definition;
        private int progress;
        private int extra;
        private boolean completed;

        private ContractState(ContractDefinition definition) {
            this.definition = definition;
        }

        private static ContractState restore(ContractDefinition definition, String encoded) {
            ContractState state = new ContractState(definition);
            if (encoded == null || encoded.isBlank()) {
                state.normalize();
                return state;
            }
            String[] parts = encoded.split("~", 3);
            state.progress = parseInt(parts, 0);
            state.extra = parseInt(parts, 1);
            state.completed = parseInt(parts, 2) > 0;
            state.normalize();
            return state;
        }

        private String serialize() {
            return progress + "~" + extra + "~" + (completed ? 1 : 0);
        }

        private boolean normalize() {
            int previousProgress = progress;
            int previousExtra = extra;
            boolean previousCompleted = completed;

            extra = Math.max(0, extra);
            if (definition.metric() == ContractMetric.UNIQUE_MAP_WINS) {
                progress = Integer.bitCount(extra);
            }
            progress = Math.clamp(progress, 0, definition.target());
            if (completed || progress >= definition.target()) {
                completed = true;
                progress = definition.target();
            }

            return progress != previousProgress || extra != previousExtra || completed != previousCompleted;
        }

        private ProgressResult apply(MatchStats stats) {
            if (completed) {
                return ProgressResult.none();
            }

            int before = progress;
            switch (definition.metric()) {
                case MATCHES -> progress = Math.min(definition.target(), progress + stats.matchesPlayed());
                case WINS -> progress = Math.min(definition.target(), progress + stats.wins());
                case DAMAGE -> progress = Math.min(definition.target(), progress + stats.damage());
                case ELIMINATIONS -> progress = Math.min(definition.target(), progress + stats.eliminations());
                case SPECIALS_USED -> progress = Math.min(definition.target(), progress + stats.specialsUsed());
                case SPECIAL_HITS -> progress = Math.min(definition.target(), progress + stats.specialHits());
                case UNIQUE_MAP_WINS -> {
                    if (stats.localWin() && stats.map() != null) {
                        extra |= (1 << stats.map().ordinal());
                        progress = Math.min(definition.target(), Integer.bitCount(extra));
                    }
                }
            }

            if (progress == before) {
                return ProgressResult.none();
            }

            boolean completedNow = progress >= definition.target();
            if (completedNow) {
                completed = true;
                progress = definition.target();
            }

            return new ProgressResult(true, completedNow, toView());
        }

        private ContractView toView() {
            return new ContractView(
                    definition.frequency(),
                    definition.id(),
                    definition.title(),
                    definition.description(),
                    progress,
                    definition.target(),
                    completed,
                    definition.rewardCoins(),
                    definition.rewardMasteryXp()
            );
        }

        private static int parseInt(String[] parts, int idx) {
            if (parts == null || idx < 0 || idx >= parts.length) return 0;
            try {
                return Integer.parseInt(parts[idx]);
            } catch (NumberFormatException ignored) {
                return 0;
            }
        }
    }

    private record ProgressResult(boolean changed, boolean completedNow, ContractView view) {
        private static ProgressResult none() {
            return new ProgressResult(false, false, null);
        }
    }
}
