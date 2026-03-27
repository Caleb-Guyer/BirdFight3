package com.example.birdgame3;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.IntConsumer;
import java.util.function.IntUnaryOperator;
import java.util.prefs.Preferences;

final class BirdProgression {
    private static final String PREF_BIRD_MASTERY_XP_PREFIX = "bird_mastery_xp_";
    private static final int[] BIRD_MASTERY_LEVEL_XP = {
            0, 120, 270, 470, 720, 1030, 1400, 1840, 2350, 2950
    };
    private static final String[] BIRD_MASTERY_TITLES = {
            "Fledgling", "Rookie", "Scout", "Striker", "Ace",
            "Elite", "Royal", "Ascendant", "Mythic", "Sovereign"
    };

    record MasteryGain(BirdGame3.BirdType type, int xpEarned, int totalXp, int levelBefore, int levelAfter) {
        boolean leveledUp() {
            return levelAfter > levelBefore;
        }

        boolean maxed() {
            return levelAfter >= BIRD_MASTERY_TITLES.length;
        }
    }

    record MasterySummary(List<MasteryGain> gains) {
        MasterySummary {
            gains = List.copyOf(gains);
        }

        boolean isEmpty() {
            return gains.isEmpty();
        }
    }

    record MatchContext(
            LocalDate contractDate,
            BirdGame3.MapType selectedMap,
            Bird winner,
            Bird[] players,
            boolean[] isAI,
            int activePlayers,
            int[] damageDealt,
            int[] eliminations,
            int[] specialsUsed,
            int[] specialHits,
            int[] tauntsPerformed,
            boolean trainingModeActive,
            boolean lanModeActive,
            int lanPlayerIndex,
            boolean dailyChallengeModeActive,
            boolean classicModeActive,
            boolean adventureModeActive,
            boolean storyModeActive,
            boolean competitionModeEnabled,
            boolean teamMode,
            IntUnaryOperator effectiveTeamResolver
    ) {
    }

    record MatchResult(ContractsBoard.UpdateResult contractUpdate, MasterySummary masterySummary) {
    }

    private record AwardOutcome(MasterySummary summary, boolean changed) {
    }

    private final ContractsBoard contractsBoard = new ContractsBoard();
    private final BirdGame3.BirdType[] birdTypes;
    private final int[] birdMasteryXp;

    BirdProgression(BirdGame3.BirdType[] birdTypes) {
        this.birdTypes = birdTypes.clone();
        this.birdMasteryXp = new int[birdTypes.length];
    }

    void load(Preferences prefs, LocalDate today) {
        for (BirdGame3.BirdType type : birdTypes) {
            int idx = type.ordinal();
            birdMasteryXp[idx] = clampXp(prefs.getInt(PREF_BIRD_MASTERY_XP_PREFIX + type.name(), 0));
        }
        contractsBoard.load(prefs, today);
    }

    void save(Preferences prefs) {
        for (BirdGame3.BirdType type : birdTypes) {
            int idx = type.ordinal();
            prefs.putInt(PREF_BIRD_MASTERY_XP_PREFIX + type.name(), clampXp(birdMasteryXp[idx]));
        }
        contractsBoard.save(prefs);
    }

    boolean refreshContracts(LocalDate today) {
        return contractsBoard.refresh(today);
    }

    List<ContractsBoard.ContractView> activeContracts() {
        return contractsBoard.activeContracts();
    }

    int level(BirdGame3.BirdType type) {
        return levelForXp(xp(type));
    }

    String title(BirdGame3.BirdType type) {
        if (type == null) {
            return "Unknown";
        }
        int level = Math.clamp(level(type), 1, maxLevel());
        return BIRD_MASTERY_TITLES[level - 1];
    }

    String statusLine(BirdGame3.BirdType type) {
        return "MASTERY: LV " + level(type) + ' ' + title(type);
    }

    String progressLine(BirdGame3.BirdType type) {
        if (type == null) {
            return "Progress: unavailable";
        }
        if (isMaxed(type)) {
            return "Progress: MAX LEVEL";
        }
        int level = level(type);
        int currentXp = xp(type);
        int levelStartXp = xpForLevel(level);
        int nextLevelXp = xpForLevel(level + 1);
        return "Progress: " + (currentXp - levelStartXp) + "/" + (nextLevelXp - levelStartXp)
                + " XP to Lv " + (level + 1);
    }

    double progressRatio(BirdGame3.BirdType type) {
        if (type == null) {
            return 0.0;
        }
        if (isMaxed(type)) {
            return 1.0;
        }
        int level = level(type);
        int currentXp = xp(type);
        int levelStartXp = xpForLevel(level);
        int nextLevelXp = xpForLevel(level + 1);
        int span = Math.max(1, nextLevelXp - levelStartXp);
        return Math.clamp((currentXp - levelStartXp) / (double) span, 0.0, 1.0);
    }

    MatchResult applyMatch(MatchContext context, IntConsumer coinGrant, Runnable saveCallback) {
        boolean changed = refreshContracts(context.contractDate());

        ContractsBoard.UpdateResult contractUpdate = ContractsBoard.UpdateResult.empty();
        MasterySummary contractBonusSummary = new MasterySummary(Collections.emptyList());
        ContractsBoard.MatchStats matchStats = buildContractMatchStats(context);
        if (matchStats.matchesPlayed() > 0) {
            contractUpdate = contractsBoard.applyMatch(matchStats);
            if (contractUpdate.hasChanges()) {
                changed = true;
                if (contractUpdate.coinsAwarded() > 0) {
                    coinGrant.accept(contractUpdate.coinsAwarded());
                }
                AwardOutcome contractBonus = awardContractMasteryBonus(localContractBirdTypes(context), contractUpdate.masteryXpAwarded());
                contractBonusSummary = contractBonus.summary();
                changed |= contractBonus.changed();
            }
        }

        AwardOutcome matchMastery = awardBirdMasteryForMatch(context);
        changed |= matchMastery.changed();
        MasterySummary combinedSummary = mergeSummaries(matchMastery.summary(), contractBonusSummary);

        if (changed) {
            saveCallback.run();
        }
        return new MatchResult(contractUpdate, combinedSummary);
    }

    static boolean isLocalProgressPlayer(
            Bird[] players,
            boolean[] isAI,
            boolean trainingModeActive,
            boolean lanModeActive,
            int lanPlayerIndex,
            int playerIdx
    ) {
        if (trainingModeActive || playerIdx < 0 || players == null || playerIdx >= players.length) {
            return false;
        }
        if (players[playerIdx] == null) {
            return false;
        }
        if (lanModeActive) {
            return playerIdx == lanPlayerIndex;
        }
        return isAI != null && playerIdx < isAI.length && !isAI[playerIdx];
    }

    private ContractsBoard.MatchStats buildContractMatchStats(MatchContext context) {
        if (context.trainingModeActive()) {
            return emptyMatchStats(context.selectedMap());
        }

        int damage = 0;
        int eliminations = 0;
        int usedSpecials = 0;
        int landedSpecials = 0;
        boolean hasLocalPlayers = false;

        for (int i = 0; i < playerCount(context); i++) {
            if (!isLocalProgressPlayer(context, i)) {
                continue;
            }
            hasLocalPlayers = true;
            damage += statAt(context.damageDealt(), i);
            eliminations += statAt(context.eliminations(), i);
            usedSpecials += statAt(context.specialsUsed(), i);
            landedSpecials += statAt(context.specialHits(), i);
        }

        if (!hasLocalPlayers) {
            return emptyMatchStats(context.selectedMap());
        }

        boolean localWin = didLocalContractPlayerWin(context);
        return new ContractsBoard.MatchStats(
                localWin,
                context.selectedMap(),
                1,
                localWin ? 1 : 0,
                damage,
                eliminations,
                usedSpecials,
                landedSpecials
        );
    }

    private boolean didLocalContractPlayerWin(MatchContext context) {
        if (context.winner() == null) {
            return false;
        }
        int winningTeam = context.effectiveTeamResolver().applyAsInt(context.winner().playerIndex);
        for (int i = 0; i < playerCount(context); i++) {
            if (isLocalProgressPlayer(context, i)
                    && context.effectiveTeamResolver().applyAsInt(i) == winningTeam) {
                return true;
            }
        }
        return false;
    }

    private Set<BirdGame3.BirdType> localContractBirdTypes(MatchContext context) {
        Set<BirdGame3.BirdType> types = new LinkedHashSet<>();
        for (int i = 0; i < playerCount(context); i++) {
            if (!isLocalProgressPlayer(context, i)) {
                continue;
            }
            Bird bird = context.players()[i];
            if (bird != null && bird.type != null) {
                types.add(bird.type);
            }
        }
        return types;
    }

    private AwardOutcome awardContractMasteryBonus(Set<BirdGame3.BirdType> localTypes, int bonusXp) {
        if (bonusXp <= 0 || localTypes == null || localTypes.isEmpty()) {
            return new AwardOutcome(new MasterySummary(Collections.emptyList()), false);
        }

        List<MasteryGain> gains = new ArrayList<>();
        boolean changed = false;
        for (BirdGame3.BirdType type : localTypes) {
            int idx = type.ordinal();
            int beforeXp = birdMasteryXp[idx];
            int beforeLevel = levelForXp(beforeXp);
            int earnedXp = Math.clamp(maxXp() - beforeXp, 0, bonusXp);
            int afterXp = clampXp(beforeXp + earnedXp);
            birdMasteryXp[idx] = afterXp;
            int afterLevel = levelForXp(afterXp);
            changed |= afterXp != beforeXp;
            gains.add(new MasteryGain(type, earnedXp, afterXp, beforeLevel, afterLevel));
        }
        return new AwardOutcome(new MasterySummary(gains), changed);
    }

    private AwardOutcome awardBirdMasteryForMatch(MatchContext context) {
        if (context.trainingModeActive()) {
            return new AwardOutcome(new MasterySummary(Collections.emptyList()), false);
        }

        Map<BirdGame3.BirdType, Integer> xpByType = new LinkedHashMap<>();
        int playerLimit = Math.min(playerCount(context), Math.max(0, context.activePlayers()));
        for (int i = 0; i < playerLimit; i++) {
            if (!isLocalProgressPlayer(context, i)) {
                continue;
            }
            Bird bird = context.players()[i];
            if (bird == null || bird.type == null) {
                continue;
            }
            xpByType.merge(bird.type, masteryXpFromPlayerStats(context, i), Integer::sum);
        }

        if (xpByType.isEmpty()) {
            return new AwardOutcome(new MasterySummary(Collections.emptyList()), false);
        }

        List<MasteryGain> gains = new ArrayList<>();
        boolean changed = false;
        for (Map.Entry<BirdGame3.BirdType, Integer> entry : xpByType.entrySet()) {
            BirdGame3.BirdType type = entry.getKey();
            int idx = type.ordinal();
            int beforeXp = birdMasteryXp[idx];
            int beforeLevel = levelForXp(beforeXp);
            int earnedXp = Math.clamp(maxXp() - beforeXp, 0, entry.getValue());
            int afterXp = clampXp(beforeXp + earnedXp);
            birdMasteryXp[idx] = afterXp;
            int afterLevel = levelForXp(afterXp);
            changed |= afterXp != beforeXp;
            gains.add(new MasteryGain(type, earnedXp, afterXp, beforeLevel, afterLevel));
        }
        return new AwardOutcome(new MasterySummary(gains), changed);
    }

    private int masteryXpFromPlayerStats(MatchContext context, int playerIdx) {
        int xp = statAt(context.damageDealt(), playerIdx) / 7;
        xp += statAt(context.eliminations(), playerIdx) * 12;
        xp += statAt(context.specialHits(), playerIdx) * 5;
        xp += Math.clamp(statAt(context.tauntsPerformed(), playerIdx), 0, 6);

        Bird winner = context.winner();
        if (winner != null) {
            if (winner.playerIndex == playerIdx) {
                xp += 22;
            } else if (context.teamMode()
                    && context.effectiveTeamResolver().applyAsInt(winner.playerIndex)
                    == context.effectiveTeamResolver().applyAsInt(playerIdx)) {
                xp += 14;
            }
        }

        if (context.dailyChallengeModeActive()) {
            xp += 20;
        } else if (context.classicModeActive()) {
            xp += 16;
        } else if (context.adventureModeActive()) {
            xp += 14;
        } else if (context.storyModeActive()) {
            xp += 12;
        }

        if (context.competitionModeEnabled()) {
            xp += 10;
        }
        if (context.lanModeActive()) {
            xp += 8;
        }
        return Math.max(8, xp);
    }

    private static MasterySummary mergeSummaries(MasterySummary primary, MasterySummary secondary) {
        if (primary == null || primary.isEmpty()) {
            return secondary == null ? new MasterySummary(Collections.emptyList()) : secondary;
        }
        if (secondary == null || secondary.isEmpty()) {
            return primary;
        }

        Map<BirdGame3.BirdType, MasteryGain> merged = new LinkedHashMap<>();
        for (MasterySummary summary : List.of(primary, secondary)) {
            for (MasteryGain gain : summary.gains()) {
                MasteryGain existing = merged.get(gain.type());
                if (existing == null) {
                    merged.put(gain.type(), gain);
                } else {
                    merged.put(gain.type(), new MasteryGain(
                            gain.type(),
                            existing.xpEarned() + gain.xpEarned(),
                            gain.totalXp(),
                            existing.levelBefore(),
                            gain.levelAfter()
                    ));
                }
            }
        }
        return new MasterySummary(new ArrayList<>(merged.values()));
    }

    private int xp(BirdGame3.BirdType type) {
        return type == null ? 0 : birdMasteryXp[type.ordinal()];
    }

    private boolean isMaxed(BirdGame3.BirdType type) {
        return level(type) >= maxLevel();
    }

    private int maxLevel() {
        return BIRD_MASTERY_TITLES.length;
    }

    private int maxXp() {
        return BIRD_MASTERY_LEVEL_XP[BIRD_MASTERY_LEVEL_XP.length - 1];
    }

    private int clampXp(int xp) {
        return Math.clamp(xp, 0, maxXp());
    }

    private int xpForLevel(int level) {
        if (level <= 1) {
            return 0;
        }
        if (level >= maxLevel()) {
            return maxXp();
        }
        return BIRD_MASTERY_LEVEL_XP[level - 1];
    }

    private int levelForXp(int xp) {
        int safeXp = clampXp(xp);
        for (int i = BIRD_MASTERY_LEVEL_XP.length - 1; i >= 0; i--) {
            if (safeXp >= BIRD_MASTERY_LEVEL_XP[i]) {
                return i + 1;
            }
        }
        return 1;
    }

    private static boolean isLocalProgressPlayer(MatchContext context, int playerIdx) {
        return isLocalProgressPlayer(
                context.players(),
                context.isAI(),
                context.trainingModeActive(),
                context.lanModeActive(),
                context.lanPlayerIndex(),
                playerIdx
        );
    }

    private static int playerCount(MatchContext context) {
        return context.players() == null ? 0 : context.players().length;
    }

    private static int statAt(int[] values, int idx) {
        if (values == null || idx < 0 || idx >= values.length) {
            return 0;
        }
        return Math.max(0, values[idx]);
    }

    private static ContractsBoard.MatchStats emptyMatchStats(BirdGame3.MapType map) {
        return new ContractsBoard.MatchStats(false, map, 0, 0, 0, 0, 0, 0);
    }
}
