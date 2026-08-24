package com.example.birdgame3;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;

/**
 * Sanitized, serializable rules for a normal versus match.
 *
 * <p>The record deliberately contains only options backed by real simulation
 * systems. It is also sent by the host for lockstep matches and stored in
 * replays, so presentation can never disagree with the rules being simulated.</p>
 */
record VersusRules(
        String name,
        int stockCount,
        int timeLimitSeconds,
        boolean powerUpsEnabled,
        int powerUpIntervalSeconds,
        boolean stageHazardsEnabled,
        boolean ultimatesEnabled,
        boolean friendlyFireEnabled,
        int launchRatePercent,
        int damageRatePercent,
        int seriesWins,
        int defaultCpuLevel,
        BirdGame3.StageRandomPool randomStagePool,
        boolean mutatorsEnabled,
        Set<String> excludedStageKeys
) {
    private static final int FORMAT_VERSION = 1;

    VersusRules {
        name = sanitizeName(name);
        stockCount = Math.clamp(stockCount, 1, 5);
        timeLimitSeconds = Math.clamp(timeLimitSeconds, 60, 600);
        powerUpIntervalSeconds = Math.clamp(powerUpIntervalSeconds, 4, 20);
        launchRatePercent = snapPercent(launchRatePercent);
        damageRatePercent = snapPercent(damageRatePercent);
        seriesWins = Math.clamp(seriesWins, 1, 5);
        defaultCpuLevel = Math.clamp(defaultCpuLevel, 1, 9);
        randomStagePool = sanitizeRandomPool(randomStagePool);
        excludedStageKeys = sanitizeStageKeys(excludedStageKeys);
    }

    static VersusRules standard() {
        return new VersusRules("STANDARD SMASH", 3, 150,
                true, 8, true, true, false,
                100, 100, 1, 5, BirdGame3.StageRandomPool.ALL,
                false, Set.of());
    }

    static VersusRules competitive() {
        return new VersusRules("COMPETITIVE", 3, 120,
                false, 8, false, true, false,
                100, 100, 3, 7, BirdGame3.StageRandomPool.MAIN,
                false, Set.of());
    }

    static VersusRules chaos() {
        return new VersusRules("POWER-UP CHAOS", 3, 150,
                true, 4, true, true, false,
                110, 110, 1, 6, BirdGame3.StageRandomPool.ALL,
                true, Set.of());
    }

    static VersusRules decode(String encoded, VersusRules fallback) {
        VersusRules safeFallback = fallback == null ? standard() : fallback;
        if (encoded == null || encoded.isBlank()) return safeFallback;
        try {
            String[] parts = encoded.split(";", -1);
            if (parts.length != 16 || Integer.parseInt(parts[0]) != FORMAT_VERSION) return safeFallback;
            String decodedName = new String(Base64.getUrlDecoder().decode(parts[1]), StandardCharsets.UTF_8);
            Set<String> exclusions = new LinkedHashSet<>();
            if (!parts[15].isBlank()) {
                for (String key : parts[15].split(",")) {
                    if (!key.isBlank()) exclusions.add(key);
                }
            }
            return new VersusRules(
                    decodedName,
                    Integer.parseInt(parts[2]),
                    Integer.parseInt(parts[3]),
                    Boolean.parseBoolean(parts[4]),
                    Integer.parseInt(parts[5]),
                    Boolean.parseBoolean(parts[6]),
                    Boolean.parseBoolean(parts[7]),
                    Boolean.parseBoolean(parts[8]),
                    Integer.parseInt(parts[9]),
                    Integer.parseInt(parts[10]),
                    Integer.parseInt(parts[11]),
                    Integer.parseInt(parts[12]),
                    BirdGame3.StageRandomPool.valueOf(parts[13]),
                    Boolean.parseBoolean(parts[14]),
                    exclusions
            );
        } catch (IllegalArgumentException | IndexOutOfBoundsException ignored) {
            return safeFallback;
        }
    }

    String encode() {
        String encodedName = Base64.getUrlEncoder().withoutPadding()
                .encodeToString(name.getBytes(StandardCharsets.UTF_8));
        return String.join(";",
                Integer.toString(FORMAT_VERSION),
                encodedName,
                Integer.toString(stockCount),
                Integer.toString(timeLimitSeconds),
                Boolean.toString(powerUpsEnabled),
                Integer.toString(powerUpIntervalSeconds),
                Boolean.toString(stageHazardsEnabled),
                Boolean.toString(ultimatesEnabled),
                Boolean.toString(friendlyFireEnabled),
                Integer.toString(launchRatePercent),
                Integer.toString(damageRatePercent),
                Integer.toString(seriesWins),
                Integer.toString(defaultCpuLevel),
                randomStagePool.name(),
                Boolean.toString(mutatorsEnabled),
                String.join(",", excludedStageKeys));
    }

    String summary() {
        return stockCount + (stockCount == 1 ? " stock" : " stocks")
                + "  •  " + timeText()
                + "  •  items " + onOff(powerUpsEnabled)
                + "  •  hazards " + onOff(stageHazardsEnabled)
                + "  •  ults " + onOff(ultimatesEnabled);
    }

    String detailSummary() {
        String series = seriesWins <= 1 ? "single match" : "first to " + seriesWins;
        return summary() + "  •  " + series
                + "  •  launch " + rateText(launchRatePercent)
                + "  •  damage " + rateText(damageRatePercent);
    }

    String timeText() {
        int minutes = timeLimitSeconds / 60;
        int seconds = timeLimitSeconds % 60;
        return String.format(Locale.ROOT, "%d:%02d", minutes, seconds);
    }

    String randomPoolText() {
        return switch (randomStagePool) {
            case MAIN -> "MAIN STAGES";
            case VARIANTS -> "VARIANTS";
            case ALL, NONE -> "ALL STAGES";
        };
    }

    boolean excludes(BirdGame3.StageChoice choice) {
        return choice != null && excludedStageKeys.contains(stageKey(choice));
    }

    static String stageKey(BirdGame3.StageChoice choice) {
        if (choice == null) return "";
        return choice.map().name() + ":" + choice.variant().name();
    }

    VersusRules withName(String value) {
        return copy(value, stockCount, timeLimitSeconds, powerUpsEnabled, powerUpIntervalSeconds,
                stageHazardsEnabled, ultimatesEnabled, friendlyFireEnabled, launchRatePercent,
                damageRatePercent, seriesWins, defaultCpuLevel, randomStagePool, mutatorsEnabled,
                excludedStageKeys);
    }

    VersusRules withStockCount(int value) {
        return copy(name, value, timeLimitSeconds, powerUpsEnabled, powerUpIntervalSeconds,
                stageHazardsEnabled, ultimatesEnabled, friendlyFireEnabled, launchRatePercent,
                damageRatePercent, seriesWins, defaultCpuLevel, randomStagePool, mutatorsEnabled,
                excludedStageKeys);
    }

    VersusRules withTimeLimitSeconds(int value) {
        return copy(name, stockCount, value, powerUpsEnabled, powerUpIntervalSeconds,
                stageHazardsEnabled, ultimatesEnabled, friendlyFireEnabled, launchRatePercent,
                damageRatePercent, seriesWins, defaultCpuLevel, randomStagePool, mutatorsEnabled,
                excludedStageKeys);
    }

    VersusRules withPowerUpsEnabled(boolean value) {
        return copy(name, stockCount, timeLimitSeconds, value, powerUpIntervalSeconds,
                stageHazardsEnabled, ultimatesEnabled, friendlyFireEnabled, launchRatePercent,
                damageRatePercent, seriesWins, defaultCpuLevel, randomStagePool, mutatorsEnabled,
                excludedStageKeys);
    }

    VersusRules withPowerUpIntervalSeconds(int value) {
        return copy(name, stockCount, timeLimitSeconds, powerUpsEnabled, value,
                stageHazardsEnabled, ultimatesEnabled, friendlyFireEnabled, launchRatePercent,
                damageRatePercent, seriesWins, defaultCpuLevel, randomStagePool, mutatorsEnabled,
                excludedStageKeys);
    }

    VersusRules withStageHazardsEnabled(boolean value) {
        return copy(name, stockCount, timeLimitSeconds, powerUpsEnabled, powerUpIntervalSeconds,
                value, ultimatesEnabled, friendlyFireEnabled, launchRatePercent,
                damageRatePercent, seriesWins, defaultCpuLevel, randomStagePool, mutatorsEnabled,
                excludedStageKeys);
    }

    VersusRules withUltimatesEnabled(boolean value) {
        return copy(name, stockCount, timeLimitSeconds, powerUpsEnabled, powerUpIntervalSeconds,
                stageHazardsEnabled, value, friendlyFireEnabled, launchRatePercent,
                damageRatePercent, seriesWins, defaultCpuLevel, randomStagePool, mutatorsEnabled,
                excludedStageKeys);
    }

    VersusRules withFriendlyFireEnabled(boolean value) {
        return copy(name, stockCount, timeLimitSeconds, powerUpsEnabled, powerUpIntervalSeconds,
                stageHazardsEnabled, ultimatesEnabled, value, launchRatePercent,
                damageRatePercent, seriesWins, defaultCpuLevel, randomStagePool, mutatorsEnabled,
                excludedStageKeys);
    }

    VersusRules withLaunchRatePercent(int value) {
        return copy(name, stockCount, timeLimitSeconds, powerUpsEnabled, powerUpIntervalSeconds,
                stageHazardsEnabled, ultimatesEnabled, friendlyFireEnabled, value,
                damageRatePercent, seriesWins, defaultCpuLevel, randomStagePool, mutatorsEnabled,
                excludedStageKeys);
    }

    VersusRules withDamageRatePercent(int value) {
        return copy(name, stockCount, timeLimitSeconds, powerUpsEnabled, powerUpIntervalSeconds,
                stageHazardsEnabled, ultimatesEnabled, friendlyFireEnabled, launchRatePercent,
                value, seriesWins, defaultCpuLevel, randomStagePool, mutatorsEnabled,
                excludedStageKeys);
    }

    VersusRules withSeriesWins(int value) {
        return copy(name, stockCount, timeLimitSeconds, powerUpsEnabled, powerUpIntervalSeconds,
                stageHazardsEnabled, ultimatesEnabled, friendlyFireEnabled, launchRatePercent,
                damageRatePercent, value, defaultCpuLevel, randomStagePool, mutatorsEnabled,
                excludedStageKeys);
    }

    VersusRules withDefaultCpuLevel(int value) {
        return copy(name, stockCount, timeLimitSeconds, powerUpsEnabled, powerUpIntervalSeconds,
                stageHazardsEnabled, ultimatesEnabled, friendlyFireEnabled, launchRatePercent,
                damageRatePercent, seriesWins, value, randomStagePool, mutatorsEnabled,
                excludedStageKeys);
    }

    VersusRules withRandomStagePool(BirdGame3.StageRandomPool value) {
        return copy(name, stockCount, timeLimitSeconds, powerUpsEnabled, powerUpIntervalSeconds,
                stageHazardsEnabled, ultimatesEnabled, friendlyFireEnabled, launchRatePercent,
                damageRatePercent, seriesWins, defaultCpuLevel, value, mutatorsEnabled,
                excludedStageKeys);
    }

    VersusRules withMutatorsEnabled(boolean value) {
        return copy(name, stockCount, timeLimitSeconds, powerUpsEnabled, powerUpIntervalSeconds,
                stageHazardsEnabled, ultimatesEnabled, friendlyFireEnabled, launchRatePercent,
                damageRatePercent, seriesWins, defaultCpuLevel, randomStagePool, value,
                excludedStageKeys);
    }

    VersusRules withExcludedStageKeys(Set<String> value) {
        return copy(name, stockCount, timeLimitSeconds, powerUpsEnabled, powerUpIntervalSeconds,
                stageHazardsEnabled, ultimatesEnabled, friendlyFireEnabled, launchRatePercent,
                damageRatePercent, seriesWins, defaultCpuLevel, randomStagePool, mutatorsEnabled,
                value);
    }

    private VersusRules copy(String copiedName, int copiedStocks, int copiedSeconds,
                             boolean copiedPowerUps, int copiedPowerInterval, boolean copiedHazards,
                             boolean copiedUltimates, boolean copiedFriendlyFire, int copiedLaunch,
                             int copiedDamage, int copiedSeries, int copiedCpu,
                             BirdGame3.StageRandomPool copiedPool, boolean copiedMutators,
                             Set<String> copiedExclusions) {
        return new VersusRules(copiedName, copiedStocks, copiedSeconds, copiedPowerUps,
                copiedPowerInterval, copiedHazards, copiedUltimates, copiedFriendlyFire,
                copiedLaunch, copiedDamage, copiedSeries, copiedCpu, copiedPool,
                copiedMutators, copiedExclusions);
    }

    private static String sanitizeName(String value) {
        String text = value == null ? "CUSTOM RULES" : value.trim().replaceAll("\\s+", " ");
        if (text.isBlank()) text = "CUSTOM RULES";
        return text.length() <= 24 ? text : text.substring(0, 24).trim();
    }

    private static int snapPercent(int value) {
        int clamped = Math.clamp(value, 50, 200);
        return Math.clamp((int) Math.round(clamped / 10.0) * 10, 50, 200);
    }

    private static BirdGame3.StageRandomPool sanitizeRandomPool(BirdGame3.StageRandomPool value) {
        return value == null || value == BirdGame3.StageRandomPool.NONE
                ? BirdGame3.StageRandomPool.ALL : value;
    }

    private static Set<String> sanitizeStageKeys(Set<String> values) {
        LinkedHashSet<String> result = new LinkedHashSet<>();
        if (values != null) {
            for (String value : values) {
                if (value != null && value.matches("[A-Z0-9_]+:[A-Z0-9_]+")) result.add(value);
            }
        }
        return java.util.Collections.unmodifiableSet(result);
    }

    private static String onOff(boolean value) {
        return value ? "on" : "off";
    }

    private static String rateText(int value) {
        return String.format(Locale.ROOT, "%.1fx", value / 100.0);
    }
}
