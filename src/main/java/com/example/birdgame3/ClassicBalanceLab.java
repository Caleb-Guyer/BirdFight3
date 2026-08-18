package com.example.birdgame3;

import com.example.birdgame3.BirdGame3.BirdType;
import com.example.birdgame3.BirdGame3.ClassicEncounter;
import com.example.birdgame3.BirdGame3.ClassicEncounterStyle;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.prefs.Preferences;

/**
 * Headless AI playthrough audit for the real Classic-mode encounters.
 *
 * <p>Unlike {@link BalanceLab}, this preserves each route's authored teams,
 * health, stocks, difficulty scaling, map variant, mutator, twist, timer, and
 * runtime mechanics. Results are reported per encounter so a difficult route
 * round is not mistaken for a weak character in ordinary 1v1 play.
 */
final class ClassicBalanceLab {

    record Config(BirdType[] birds, int matchesPerEncounter, long maxTicksPerMatch,
                  long baseSeed, double difficulty, int playerCpuLevel,
                  boolean includeObjectiveRounds) {
        Config {
            birds = birds == null ? new BirdType[0] : birds.clone();
            matchesPerEncounter = Math.max(1, matchesPerEncounter);
            maxTicksPerMatch = Math.max(60L, maxTicksPerMatch);
            difficulty = Math.clamp(difficulty, 1.0, 9.0);
            playerCpuLevel = Math.clamp(playerCpuLevel, 1, 9);
        }

        static Config forBird(BirdType bird) {
            return new Config(new BirdType[]{bird}, 48, 4L * 60 * 60,
                    20260815L, BirdGame3.CLASSIC_STARTING_DIFFICULTY, 5, false);
        }

        @Override
        public BirdType[] birds() {
            return birds.clone();
        }
    }

    record EncounterOutcome(BirdType bird, int roundIndex, String encounterName,
                            ClassicEncounterStyle style, boolean won, boolean lost,
                            long ticks, int damageDealt) {
        boolean decided() {
            return won || lost;
        }
    }

    record EncounterSummary(BirdType bird, int roundIndex, String encounterName,
                            ClassicEncounterStyle style, String mapName,
                            boolean objectiveRound, int matches, int wins, int losses,
                            int draws, double clearRate, double averageTicks,
                            double averageDamageDealt) {
    }

    record Report(Config config, List<EncounterOutcome> outcomes,
                  List<EncounterSummary> summaries) {
        String markdown() {
            StringBuilder sb = new StringBuilder();
            sb.append("# Classic Mode Balance Lab\n\n");
            sb.append("- Difficulty: ").append(String.format(Locale.ROOT, "%.1f", config.difficulty())).append('\n');
            sb.append("- Player AI level: ").append(config.playerCpuLevel()).append('\n');
            sb.append("- Matches per scored encounter: ").append(config.matchesPerEncounter()).append('\n');
            sb.append("- Objective/bonus rounds: ")
                    .append(config.includeObjectiveRounds() ? "included (interpret separately)" : "listed but not simulated")
                    .append('\n');
            sb.append("- Metric: attempt-based team clear rate in the real Classic encounter setup; harness cutoffs count as draws and failed clears\n\n");

            BirdType current = null;
            for (EncounterSummary summary : summaries) {
                if (summary.bird() != current) {
                    current = summary.bird();
                    sb.append("## ").append(current.name).append("\n\n");
                    sb.append("| Round | Encounter | Kind | Map | W-L-D | Clear rate | Avg time | Damage dealt | Read |\n");
                    sb.append("|---:|---|---|---|---:|---:|---:|---:|---|\n");
                }
                String result = summary.matches() == 0
                        ? "—"
                        : summary.wins() + "-" + summary.losses() + "-" + summary.draws();
                String rate = summary.matches() == 0 || Double.isNaN(summary.clearRate())
                        ? "—"
                        : String.format(Locale.ROOT, "%.1f%%", summary.clearRate() * 100.0);
                String averageTime = summary.matches() == 0
                        ? "—"
                        : String.format(Locale.ROOT, "%.1fs", summary.averageTicks() / 60.0);
                String damage = summary.matches() == 0
                        ? "—"
                        : String.format(Locale.ROOT, "%.0f", summary.averageDamageDealt());
                sb.append("| ").append(summary.roundIndex() + 1)
                        .append(" | ").append(escape(summary.encounterName()))
                        .append(" | ").append(summary.objectiveRound() ? "Objective" : "Combat")
                        .append(" | ").append(escape(summary.mapName()))
                        .append(" | ").append(result)
                        .append(" | ").append(rate)
                        .append(" | ").append(averageTime)
                        .append(" | ").append(damage)
                        .append(" | ").append(balanceRead(summary))
                        .append(" |\n");
            }

            sb.append("\n## How to use this report\n\n");
            sb.append("Combat rounds below 40% deserve review; 40–70% is the initial target band; ")
                    .append("results above 70% may be too easy. Compare this report with the ordinary roster ")
                    .append("audit: weakness in both points to the fighter, while weakness only here points to ")
                    .append("the authored Classic encounter. Objective rounds measure AI routing as much as balance.\n");
            return sb.toString();
        }

        private static String balanceRead(EncounterSummary summary) {
            if (summary.matches() == 0) return "Not scored";
            if (summary.objectiveRound()) return "AI routing check";
            if (summary.averageTicks() < 15.0 * 60.0 && summary.averageDamageDealt() < 15.0) {
                return "Likely AI routing failure";
            }
            if (summary.clearRate() < 0.40) return "Too hard / weak matchup";
            if (summary.clearRate() <= 0.70) return "Target band";
            return "Possibly too easy";
        }

        private static String escape(String value) {
            return value == null ? "" : value.replace("|", "\\|");
        }
    }

    private ClassicBalanceLab() {
    }

    static Report run(Config config, Consumer<String> progress) {
        String tuning = BirdStats.reloadFromDisk();
        if (progress != null) {
            progress.accept(tuning != null ? tuning : "BIRD STATS: COMPILED DEFAULTS");
        }
        BirdGame3 game = new BirdGame3(Preferences.userRoot().node(
                "/birdfight3-tests/classic-balance-lab/" + UUID.randomUUID()));
        List<EncounterOutcome> outcomes = new ArrayList<>();
        List<EncounterSummary> summaries = new ArrayList<>();

        for (BirdType bird : config.birds()) {
            if (bird == null) continue;
            long routeSeed = mixSeed(config.baseSeed(), bird.ordinal(), -1, 0);
            List<ClassicEncounter> route = game.harnessClassicRoute(bird, routeSeed);
            for (int roundIndex = 0; roundIndex < route.size(); roundIndex++) {
                ClassicEncounter encounter = route.get(roundIndex);
                boolean objectiveRound = isObjectiveRound(encounter.style);
                if (objectiveRound && !config.includeObjectiveRounds()) {
                    summaries.add(new EncounterSummary(bird, roundIndex, encounter.name,
                            encounter.style, encounterMapName(encounter), true,
                            0, 0, 0, 0, Double.NaN, 0.0, 0.0));
                    continue;
                }

                List<EncounterOutcome> encounterOutcomes = new ArrayList<>();
                for (int match = 0; match < config.matchesPerEncounter(); match++) {
                    long matchSeed = mixSeed(config.baseSeed(), bird.ordinal(), roundIndex, match);
                    EncounterOutcome outcome = playEncounter(game, bird, roundIndex,
                            config.difficulty(), config.playerCpuLevel(), routeSeed,
                            matchSeed, config.maxTicksPerMatch());
                    encounterOutcomes.add(outcome);
                    outcomes.add(outcome);
                }
                EncounterSummary summary = summarize(encounter, bird, roundIndex,
                        objectiveRound, encounterOutcomes);
                summaries.add(summary);
                if (progress != null) {
                    progress.accept(String.format(Locale.ROOT,
                            "%s round %d/%d %s: %.1f%% (%d-%d-%d)",
                            bird.name, roundIndex + 1, route.size(), encounter.name,
                            summary.clearRate() * 100.0,
                            summary.wins(), summary.losses(), summary.draws()));
                }
            }
        }
        return new Report(config, List.copyOf(outcomes), List.copyOf(summaries));
    }

    static EncounterOutcome playEncounter(BirdGame3 game, BirdType bird, int roundIndex,
                                           double difficulty, int playerCpuLevel,
                                           long routeSeed, long matchSeed, long maxTicks) {
        ClassicEncounter encounter = game.harnessPrepareClassicEncounter(
                bird, roundIndex, difficulty, playerCpuLevel, routeSeed, matchSeed);
        long ticks = 0;
        // The general audit cap used to end 250-second boss encounters at the
        // four-minute mark, creating artificial draws before their real timer
        // could choose a winner. Never cut an authored encounter short.
        // Hitstop consumes harness ticks without advancing the authored match
        // clock. Allow presentation pauses to finish while retaining a hard
        // ninety-second guard against a genuinely wedged encounter.
        long authoredTickLimit = Math.max(60L, (long) game.matchTimer + 90L * 60L);
        long tickLimit = Math.max(maxTicks, authoredTickLimit);
        while (ticks < tickLimit && game.harnessTick()) {
            ticks++;
        }

        boolean won = false;
        boolean lost = false;
        if (game.harnessWinner != null) {
            int playerTeam = game.getEffectiveTeam(0);
            int winnerTeam = game.getEffectiveTeam(game.harnessWinner.playerIndex);
            won = winnerTeam == playerTeam;
            lost = winnerTeam != playerTeam;
        }
        return new EncounterOutcome(bird, roundIndex, encounter.name, encounter.style,
                won, lost, ticks, Math.max(0, game.damageDealt[0]));
    }

    private static EncounterSummary summarize(ClassicEncounter encounter, BirdType bird,
                                               int roundIndex, boolean objectiveRound,
                                               List<EncounterOutcome> outcomes) {
        int wins = 0;
        int losses = 0;
        int draws = 0;
        long totalTicks = 0;
        long totalDamage = 0;
        for (EncounterOutcome outcome : outcomes) {
            if (outcome.won()) wins++;
            else if (outcome.lost()) losses++;
            else draws++;
            totalTicks += outcome.ticks();
            totalDamage += outcome.damageDealt();
        }
        int matches = outcomes.size();
        return new EncounterSummary(bird, roundIndex, encounter.name, encounter.style,
                encounterMapName(encounter), objectiveRound, matches, wins, losses, draws,
                attemptClearRate(wins, matches),
                matches == 0 ? 0.0 : totalTicks / (double) matches,
                matches == 0 ? 0.0 : totalDamage / (double) matches);
    }

    static double attemptClearRate(int wins, int matches) {
        return matches <= 0 ? 0.0 : Math.clamp(wins, 0, matches) / (double) matches;
    }

    static boolean isObjectiveRound(ClassicEncounterStyle style) {
        return style == ClassicEncounterStyle.BONUS_RELAY
                || style == ClassicEncounterStyle.NECTAR_DASH
                || style == ClassicEncounterStyle.HARVEST_DEFENSE
                || style == ClassicEncounterStyle.DAWN_MUSTER
                || style == ClassicEncounterStyle.REDLINE_RUN
                || style == ClassicEncounterStyle.ICE_ARCHITECT
                || style == ClassicEncounterStyle.RIPPLE_HUNT
                || style == ClassicEncounterStyle.PERFECT_PITCH
                || style == ClassicEncounterStyle.BETWEEN_LINES
                || style == ClassicEncounterStyle.QUIET_VAULT
                || style == ClassicEncounterStyle.FINAL_INVENTORY
                || style == ClassicEncounterStyle.OPIUM_LUCID_DASH
                || style == ClassicEncounterStyle.HEISEN_CALIBRATION
                || style == ClassicEncounterStyle.TITMOUSE_MEMORY_CACHE;
    }

    private static String encounterMapName(ClassicEncounter encounter) {
        String map = encounter.map == null ? "Unknown" : encounter.map.name().replace('_', ' ');
        if (encounter.variant == null || encounter.variant == BirdGame3.MapVariant.STANDARD) {
            return titleCase(map);
        }
        return titleCase(encounter.variant.name().replace('_', ' '));
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

    private static long mixSeed(long baseSeed, int bird, int round, int match) {
        long mixed = baseSeed;
        mixed ^= (long) (bird + 1) * 0x9E3779B97F4A7C15L;
        mixed ^= (long) (round + 2) * 0xD1B54A32D192ED03L;
        mixed ^= (long) (match + 3) * 0x94D049BB133111EBL;
        mixed ^= mixed >>> 30;
        mixed *= 0xBF58476D1CE4E5B9L;
        mixed ^= mixed >>> 27;
        mixed *= 0x94D049BB133111EBL;
        return mixed ^ (mixed >>> 31);
    }
}
