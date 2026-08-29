package com.example.birdgame3;

import com.example.birdgame3.BirdGame3.BirdType;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.prefs.Preferences;
import java.util.stream.Collectors;

/**
 * Headless AI completion audit for every authored Still Sky Adventure mission.
 *
 * <p>The lab runs the real mission controller and real combat setup. Unlike a
 * plain deathmatch benchmark, its clear rate therefore includes navigation and
 * objective reliability for survival, protection, capture, exit, hold-zone,
 * gauntlet, and boss missions.
 */
final class AdventureBalanceLab {

    record TargetBand(double target, double minimum, double maximum) {
        TargetBand {
            if (minimum < 0.0 || maximum > 1.0 || minimum > target || target > maximum) {
                throw new IllegalArgumentException("Invalid Adventure target band");
            }
        }

        boolean contains(double clearRate) {
            return clearRate >= minimum && clearRate <= maximum;
        }
    }

    private static final Map<StoryCampaign.Difficulty, TargetBand> BASELINE_TARGETS = new EnumMap<>(
            Map.of(
                    StoryCampaign.Difficulty.EASY, new TargetBand(0.50, 0.40, 0.60),
                    StoryCampaign.Difficulty.NORMAL, new TargetBand(0.35, 0.25, 0.45),
                    StoryCampaign.Difficulty.HARD, new TargetBand(0.15, 0.05, 0.25)
            ));

    record Config(List<String> missionIds,
                  List<StoryCampaign.Difficulty> difficulties,
                  int matchesPerMission,
                  long maxTicksPerMatch,
                  long baseSeed,
                  int playerCpuLevel) {
        Config {
            missionIds = missionIds == null ? List.of() : List.copyOf(missionIds);
            difficulties = difficulties == null || difficulties.isEmpty()
                    ? List.of(StoryCampaign.Difficulty.values())
                    : List.copyOf(difficulties);
            matchesPerMission = Math.max(1, matchesPerMission);
            maxTicksPerMatch = Math.max(60L, maxTicksPerMatch);
            playerCpuLevel = Math.clamp(playerCpuLevel, 1, 9);
        }

        static Config allMissions() {
            return new Config(List.of(), List.of(StoryCampaign.Difficulty.values()),
                    24, 6L * 60L * 60L, 20260828L, 5);
        }
    }

    record MissionOutcome(String missionId,
                          StoryCampaign.Difficulty difficulty,
                          BirdType playerBird,
                          boolean won,
                          boolean lost,
                          long ticks,
                          int damageDealt,
                          int endingPhaseIndex) {
        boolean decided() {
            return won || lost;
        }
    }

    record MissionSummary(int missionNumber,
                          String missionId,
                          String missionTitle,
                          String mapName,
                          String objectives,
                          StoryCampaign.Difficulty difficulty,
                          TargetBand targetBand,
                          int matches,
                          int wins,
                          int losses,
                          int cutoffs,
                          double clearRate,
                          double averageTicks,
                          double averageDamageDealt,
                          String failurePhases) {
        boolean meetsTarget() {
            return matches > 0 && targetBand.contains(clearRate);
        }
    }

    record Report(Config config,
                  List<MissionOutcome> outcomes,
                  List<MissionSummary> summaries) {
        List<MissionSummary> targetMisses() {
            return summaries.stream().filter(summary -> !summary.meetsTarget()).toList();
        }

        long cutoffCount() {
            return outcomes.stream().filter(outcome -> !outcome.decided()).count();
        }

        List<String> orderingViolations() {
            Map<String, Map<StoryCampaign.Difficulty, MissionSummary>> byMission = new LinkedHashMap<>();
            for (MissionSummary summary : summaries) {
                byMission.computeIfAbsent(summary.missionId(), ignored -> new EnumMap<>(
                                StoryCampaign.Difficulty.class))
                        .put(summary.difficulty(), summary);
            }
            List<String> violations = new ArrayList<>();
            for (Map.Entry<String, Map<StoryCampaign.Difficulty, MissionSummary>> entry : byMission.entrySet()) {
                MissionSummary easy = entry.getValue().get(StoryCampaign.Difficulty.EASY);
                MissionSummary normal = entry.getValue().get(StoryCampaign.Difficulty.NORMAL);
                MissionSummary hard = entry.getValue().get(StoryCampaign.Difficulty.HARD);
                if (easy == null || normal == null || hard == null) continue;
                if (easy.clearRate() < normal.clearRate() || normal.clearRate() < hard.clearRate()) {
                    violations.add(entry.getKey());
                }
            }
            return List.copyOf(violations);
        }

        String markdown() {
            StringBuilder sb = new StringBuilder();
            sb.append("# The Still Sky — Adventure Balance Lab\n\n");
            sb.append("- Player AI level: ").append(config.playerCpuLevel()).append(" (fixed across difficulties)\n");
            sb.append("- Matches per mission and difficulty: ").append(config.matchesPerMission()).append('\n');
            sb.append("- Missions tested: ").append(summaries.stream().map(MissionSummary::missionId).distinct().count()).append('\n');
            sb.append("- Difficulties: ").append(config.difficulties().stream()
                    .map(difficulty -> difficulty.label)
                    .collect(Collectors.joining(", "))).append('\n');
            sb.append("- Metric: attempt-based mission clear rate using real teams, objectives, map variants, assists, and boss phases; harness cutoffs are failed clears\n\n");

            sb.append("## Difficulty targets\n\n");
            sb.append("These are the mid-campaign baselines. Earlier missions rise by up to 10 points, ")
                    .append("later missions fall by up to 10, and boss missions fall another 5 points ")
                    .append("(8 for the final boss). Each mission uses a ±10-point accepted band.\n\n");
            sb.append("| Difficulty | Enemy AI | Baseline target | Baseline band |\n");
            sb.append("|---|---:|---:|---:|\n");
            for (StoryCampaign.Difficulty difficulty : StoryCampaign.Difficulty.values()) {
                TargetBand band = targetBand(difficulty);
                sb.append("| ").append(difficulty.label)
                        .append(" | ").append(difficulty.cpuLevel)
                        .append(" | ").append(percent(band.target()))
                        .append(" | ").append(percent(band.minimum())).append("–").append(percent(band.maximum()))
                        .append(" |\n");
            }

            sb.append("\n## Mission results\n\n");
            sb.append("| # | Mission | Difficulty | Map | Objectives | W-L-C | Clear rate | Target | Avg time | Damage | Loss phases | Read |\n");
            sb.append("|---:|---|---|---|---|---:|---:|---:|---:|---:|---|---|\n");
            for (MissionSummary summary : summaries) {
                sb.append("| ").append(summary.missionNumber())
                        .append(" | ").append(escape(summary.missionTitle()))
                        .append(" | ").append(summary.difficulty().label)
                        .append(" | ").append(escape(summary.mapName()))
                        .append(" | ").append(escape(summary.objectives()))
                        .append(" | ").append(summary.wins()).append('-').append(summary.losses()).append('-').append(summary.cutoffs())
                        .append(" | ").append(percent(summary.clearRate()))
                        .append(" | ").append(percent(summary.targetBand().target()))
                        .append(" (").append(percent(summary.targetBand().minimum())).append("–")
                        .append(percent(summary.targetBand().maximum())).append(')')
                        .append(" | ").append(String.format(Locale.ROOT, "%.1fs", summary.averageTicks() / 60.0))
                        .append(" | ").append(String.format(Locale.ROOT, "%.0f", summary.averageDamageDealt()))
                        .append(" | ").append(summary.failurePhases())
                        .append(" | ").append(balanceRead(summary))
                        .append(" |\n");
            }

            sb.append("\n## Regression read\n\n");
            sb.append("- Target misses: ").append(targetMisses().size()).append(" / ").append(summaries.size()).append('\n');
            sb.append("- Harness cutoffs: ").append(cutoffCount()).append('\n');
            List<String> ordering = orderingViolations();
            sb.append("- Missions where sampled Easy ≥ Normal ≥ Hard ordering did not hold: ")
                    .append(ordering.isEmpty() ? "none" : String.join(", ", ordering)).append("\n\n");
            sb.append("Target misses are balance and human-playtest leads, not automatic stat changes. ")
                    .append("Capture, exit, hold-zone, and protection results deliberately measure AI navigation and objective reliability as well as combat.\n");
            return sb.toString();
        }

        private static String balanceRead(MissionSummary summary) {
            if (summary.cutoffs() > 0) return "Harness cutoff / possible loop";
            if (summary.meetsTarget()) return "Target band";
            if (summary.clearRate() < summary.targetBand().minimum()) {
                if (isNavigationObjective(summary.objectives())) return "Too hard / routing review";
                return "Too hard / matchup review";
            }
            return "Possibly too easy";
        }
    }

    private AdventureBalanceLab() {
    }

    static TargetBand targetBand(StoryCampaign.Difficulty difficulty) {
        StoryCampaign.Difficulty resolved = difficulty == null
                ? StoryCampaign.Difficulty.NORMAL : difficulty;
        return BASELINE_TARGETS.get(resolved);
    }

    static TargetBand targetBand(StoryCampaign.Difficulty difficulty,
                                 int missionNumber,
                                 boolean bossMission,
                                 boolean finalBoss) {
        TargetBand baseline = targetBand(difficulty);
        double progress = Math.clamp((missionNumber - 1) / 39.0, 0.0, 1.0);
        double progressionAdjustment = 0.10 - progress * 0.20;
        double bossAdjustment = finalBoss ? -0.08 : bossMission ? -0.05 : 0.0;
        double target = Math.clamp(baseline.target() + progressionAdjustment + bossAdjustment,
                0.05, 0.80);
        return new TargetBand(target, Math.max(0.0, target - 0.10), Math.min(1.0, target + 0.10));
    }

    static Report run(Config config, Consumer<String> progress) {
        return run(config, progress, true);
    }

    /** Unit-test entry point that cannot leak the user's tuning file into shared static state. */
    static Report runWithoutTuningReload(Config config, Consumer<String> progress) {
        return run(config, progress, false);
    }

    private static Report run(Config config, Consumer<String> progress, boolean reloadTuning) {
        if (reloadTuning) {
            String tuning = BirdStats.reloadFromDisk();
            if (progress != null) {
                progress.accept(tuning != null ? tuning : "BIRD STATS: COMPILED DEFAULTS");
            }
        }
        BirdGame3 game = new BirdGame3(Preferences.userRoot().node(
                "/birdfight3-tests/adventure-balance-lab/" + UUID.randomUUID()));
        List<StoryCampaign.Mission> missions = requestedMissions(game, config.missionIds());
        List<MissionOutcome> outcomes = new ArrayList<>();
        List<MissionSummary> summaries = new ArrayList<>();

        List<StoryCampaign.Mission> authoredOrder = game.harnessAdventureMissions();
        for (StoryCampaign.Mission mission : missions) {
            int missionNumber = authoredOrder.indexOf(mission) + 1;
            for (StoryCampaign.Difficulty difficulty : config.difficulties()) {
                List<MissionOutcome> sampled = new ArrayList<>();
                List<BirdType> allowedBirds = mission.playable().resolvedBirds();
                for (int match = 0; match < config.matchesPerMission(); match++) {
                    BirdType bird = allowedBirds.get(match % allowedBirds.size());
                    long seed = mixSeed(config.baseSeed(), missionNumber - 1, difficulty.ordinal(), match);
                    MissionOutcome outcome = playMission(game, mission.id(), difficulty, bird,
                            config.playerCpuLevel(), seed, config.maxTicksPerMatch());
                    sampled.add(outcome);
                    outcomes.add(outcome);
                }
                MissionSummary summary = summarize(missionNumber, mission, difficulty, sampled);
                summaries.add(summary);
                if (progress != null) {
                    progress.accept(String.format(Locale.ROOT,
                            "%02d/40 %s [%s]: %.1f%% (%d-%d-%d), target %.0f%%",
                            missionNumber, mission.title(), difficulty.label,
                            summary.clearRate() * 100.0,
                            summary.wins(), summary.losses(), summary.cutoffs(),
                            summary.targetBand().target() * 100.0));
                }
            }
        }
        return new Report(config, List.copyOf(outcomes), List.copyOf(summaries));
    }

    static MissionOutcome playMission(BirdGame3 game,
                                      String missionId,
                                      StoryCampaign.Difficulty difficulty,
                                      BirdType playerBird,
                                      int playerCpuLevel,
                                      long seed,
                                      long maxTicks) {
        StoryCampaign.Mission mission = game.harnessPrepareAdventureMission(
                missionId, difficulty, playerBird, playerCpuLevel, seed);
        long authoredTickLimit = Math.max(60L, (long) game.matchTimer + 90L * 60L);
        long tickLimit = Math.max(maxTicks, authoredTickLimit);
        long ticks = 0L;
        while (ticks < tickLimit && game.harnessTick()) {
            ticks++;
        }
        boolean won = game.harnessCampaignMissionWon();
        boolean lost = game.matchEnded && !won;
        return new MissionOutcome(mission.id(), difficulty, playerBird, won, lost, ticks,
                Math.max(0, game.damageDealt[0]), game.harnessCampaignPhaseIndex());
    }

    private static MissionSummary summarize(int missionNumber,
                                            StoryCampaign.Mission mission,
                                            StoryCampaign.Difficulty difficulty,
                                            List<MissionOutcome> outcomes) {
        int wins = 0;
        int losses = 0;
        int cutoffs = 0;
        long totalTicks = 0L;
        long totalDamage = 0L;
        for (MissionOutcome outcome : outcomes) {
            if (outcome.won()) wins++;
            else if (outcome.lost()) losses++;
            else cutoffs++;
            totalTicks += outcome.ticks();
            totalDamage += outcome.damageDealt();
        }
        int matches = outcomes.size();
        return new MissionSummary(missionNumber, mission.id(), mission.title(), mapName(mission),
                objectiveNames(mission), difficulty,
                targetBand(difficulty, missionNumber, isBossMission(mission), mission.finalBoss()), matches,
                wins, losses, cutoffs, attemptClearRate(wins, matches),
                matches == 0 ? 0.0 : totalTicks / (double) matches,
                matches == 0 ? 0.0 : totalDamage / (double) matches,
                failurePhaseRead(outcomes));
    }

    private static String failurePhaseRead(List<MissionOutcome> outcomes) {
        Map<Integer, Long> counts = outcomes.stream()
                .filter(MissionOutcome::lost)
                .collect(Collectors.groupingBy(
                        MissionOutcome::endingPhaseIndex,
                        LinkedHashMap::new,
                        Collectors.counting()));
        if (counts.isEmpty()) return "—";
        return counts.entrySet().stream()
                .map(entry -> "P" + (entry.getKey() + 1) + ":" + entry.getValue())
                .collect(Collectors.joining(", "));
    }

    static double attemptClearRate(int wins, int matches) {
        return matches <= 0 ? 0.0 : Math.clamp(wins, 0, matches) / (double) matches;
    }

    private static List<StoryCampaign.Mission> requestedMissions(BirdGame3 game, List<String> ids) {
        List<StoryCampaign.Mission> all = game.harnessAdventureMissions();
        if (ids == null || ids.isEmpty()) return all;
        Map<String, StoryCampaign.Mission> byId = all.stream().collect(Collectors.toMap(
                mission -> mission.id().toLowerCase(Locale.ROOT), mission -> mission));
        List<StoryCampaign.Mission> selected = new ArrayList<>();
        for (String id : ids) {
            StoryCampaign.Mission mission = byId.get(id.toLowerCase(Locale.ROOT));
            if (mission == null) throw new IllegalArgumentException("Unknown Still Sky mission: " + id);
            selected.add(mission);
        }
        return List.copyOf(selected);
    }

    private static String objectiveNames(StoryCampaign.Mission mission) {
        return mission.phases().stream()
                .map(StoryCampaign.MissionPhase::objective)
                .distinct()
                .map(objective -> titleCase(objective.name().replace('_', ' ')))
                .collect(Collectors.joining(" + "));
    }

    private static boolean isBossMission(StoryCampaign.Mission mission) {
        return mission.finalBoss()
                || mission.enemies().stream().anyMatch(StoryCampaign.Fighter::boss)
                || mission.phases().stream().anyMatch(phase ->
                phase.objective() == StoryCampaign.ObjectiveType.BOSS_PHASES);
    }

    private static String mapName(StoryCampaign.Mission mission) {
        String raw = mission.mapVariant() == BirdGame3.MapVariant.STANDARD
                ? mission.map().name() : mission.mapVariant().name();
        return titleCase(raw.replace('_', ' '));
    }

    private static boolean isNavigationObjective(String objectives) {
        return objectives.contains("Capture") || objectives.contains("Reach Exit")
                || objectives.contains("Hold Zone") || objectives.contains("Protect");
    }

    private static String titleCase(String value) {
        StringBuilder result = new StringBuilder(value.length());
        boolean upper = true;
        for (int i = 0; i < value.length(); i++) {
            char c = Character.toLowerCase(value.charAt(i));
            result.append(upper ? Character.toUpperCase(c) : c);
            upper = c == ' ';
        }
        return result.toString();
    }

    private static String percent(double value) {
        return String.format(Locale.ROOT, "%.0f%%", value * 100.0);
    }

    private static String escape(String value) {
        return value == null ? "" : value.replace("|", "\\|");
    }

    static long mixSeed(long baseSeed, int mission, int difficulty, int match) {
        long mixed = baseSeed;
        mixed ^= (long) (mission + 1) * 0x9E3779B97F4A7C15L;
        mixed ^= (long) (difficulty + 2) * 0xD1B54A32D192ED03L;
        mixed ^= (long) (match + 3) * 0x94D049BB133111EBL;
        mixed ^= mixed >>> 30;
        mixed *= 0xBF58476D1CE4E5B9L;
        mixed ^= mixed >>> 27;
        mixed *= 0x94D049BB133111EBL;
        return mixed ^ (mixed >>> 31);
    }
}
