package com.example.birdgame3;

import java.util.HashMap;
import java.util.Map;

/**
 * Authored per-mission handicap values for The Still Sky.
 *
 * <p>A value above {@code 1.0} gives the allied side more room; a value below
 * {@code 1.0} increases opposition pressure. The value is converted at the
 * campaign setup choke points into modest durability, damage, and objective
 * timing adjustments. Keeping the data here makes the Easy/Normal/Hard curve
 * auditable without scattering special cases through forty mission scripts.
 */
final class AdventureMissionTuning {
    private static final ThreadLocal<Map<String, Double>> HARNESS_ADVANTAGE_OVERRIDES =
            ThreadLocal.withInitial(HashMap::new);

    record Profile(double easy, double normal, double hard) {
        Profile {
            easy = bounded(easy);
            normal = bounded(normal);
            hard = bounded(hard);
        }

        double advantage(StoryCampaign.Difficulty difficulty) {
            StoryCampaign.Difficulty resolved = difficulty == null
                    ? StoryCampaign.Difficulty.NORMAL : difficulty;
            return switch (resolved) {
                case EASY -> easy;
                case NORMAL -> normal;
                case HARD -> hard;
            };
        }
    }

    private static final Map<String, Profile> PROFILES = Map.ofEntries(
            Map.entry("dead_air", profile(0.58, 0.60, 0.53)),
            Map.entry("harbor_lock", profile(2.07, 2.26, 2.16)),
            Map.entry("last_thermal", profile(1.18, 1.36, 1.26)),
            Map.entry("breakwater_run", profile(0.81, 1.04, 0.84)),
            Map.entry("pier_nine", profile(2.15, 1.85, 1.50)),
            Map.entry("stormglass", profile(3.27, 4.50, 4.96)),
            Map.entry("painted_road", profile(0.36, 0.60, 0.64)),
            Map.entry("orders_from_above", profile(0.155, 0.23, 0.10)),
            Map.entry("open_sky", profile(0.69, 0.82, 0.83)),
            Map.entry("needle_route", profile(1.26, 1.26, 0.89)),
            Map.entry("small_marks", profile(3.49, 3.50, 3.44)),
            Map.entry("morning_line", profile(1.37, 1.50, 1.72)),
            Map.entry("green_convergence", profile(1.37, 1.72, 2.04)),
            Map.entry("listen_below", profile(6.00, 6.00, 4.50)),
            Map.entry("stone_judgment", profile(0.55, 0.87, 0.58)),
            Map.entry("second_pulse", profile(1.28, 1.79, 2.97)),
            Map.entry("whiteout_equation", profile(1.14, 1.36, 1.07)),
            Map.entry("borrowed_fire", profile(0.74, 0.78, 0.91)),
            Map.entry("crown_archive", profile(1.60, 2.81, 2.77)),
            Map.entry("last_call", profile(1.54, 1.09, 1.02)),
            Map.entry("cut_the_lock", profile(1.56, 1.78, 0.78)),
            Map.entry("sparrows_corridor", profile(1.99, 1.60, 1.19)),
            Map.entry("marshal_law", profile(0.275, 0.58, 0.40)),
            Map.entry("crow_country", profile(1.97, 2.75, 3.22)),
            Map.entry("terms_in_dark", profile(1.71, 2.00, 2.55)),
            Map.entry("carrion_audience", profile(1.40, 2.11, 1.92)),
            Map.entry("blue_water", profile(0.90, 1.33, 1.38)),
            Map.entry("future_that_moves", profile(1.32, 1.47, 1.71)),
            Map.entry("stolen_winter", profile(1.12, 1.60, 1.00)),
            Map.entry("master_key", profile(0.255, 1.24, 1.47)),
            Map.entry("perfect_weather", profile(1.00, 1.47, 1.23)),
            Map.entry("world_goes_still", profile(0.80, 0.84, 0.67)),
            Map.entry("blackout_key", profile(0.72, 0.73, 0.50)),
            Map.entry("fire_and_ice", profile(1.85, 1.98, 1.25)),
            Map.entry("harbor_engine", profile(0.98, 1.16, 0.78)),
            Map.entry("echo_chain", profile(1.43, 1.71, 1.57)),
            Map.entry("free_the_flock", profile(0.66, 1.04, 0.99)),
            Map.entry("last_approach", profile(1.55, 2.25, 1.71)),
            Map.entry("null_roc", profile(2.17, 3.38, 1.47)),
            Map.entry("the_null_rock", profile(4.55, 4.01, 1.47))
    );

    private AdventureMissionTuning() {
    }

    static double advantage(StoryCampaign.Mission mission,
                            StoryCampaign.Difficulty difficulty) {
        if (mission == null) return 1.0;
        StoryCampaign.Difficulty resolved = difficulty == null
                ? StoryCampaign.Difficulty.NORMAL : difficulty;
        Double harnessOverride = HARNESS_ADVANTAGE_OVERRIDES.get()
                .get(overrideKey(mission.id(), resolved));
        if (harnessOverride != null) {
            return bounded(harnessOverride);
        }
        return PROFILES.getOrDefault(mission.id(), profile(1.0, 1.0, 1.0))
                .advantage(resolved);
    }

    static double playerHealthScale(StoryCampaign.Mission mission,
                                    StoryCampaign.Difficulty difficulty) {
        return Math.pow(advantage(mission, difficulty), 0.85);
    }

    static double alliedHealthScale(StoryCampaign.Mission mission,
                                    StoryCampaign.Difficulty difficulty) {
        return Math.pow(advantage(mission, difficulty), 0.65);
    }

    static double alliedPowerScale(StoryCampaign.Mission mission,
                                   StoryCampaign.Difficulty difficulty) {
        return Math.pow(advantage(mission, difficulty), 0.35);
    }

    static double enemyHealthScale(StoryCampaign.Mission mission,
                                   StoryCampaign.Difficulty difficulty) {
        return Math.pow(advantage(mission, difficulty), -0.35);
    }

    static double enemyPowerScale(StoryCampaign.Mission mission,
                                  StoryCampaign.Difficulty difficulty) {
        return Math.pow(advantage(mission, difficulty), -0.55);
    }

    static double objectiveWindowScale(StoryCampaign.Mission mission,
                                       StoryCampaign.Difficulty difficulty,
                                       StoryCampaign.ObjectiveType objective) {
        double advantage = advantage(mission, difficulty);
        return switch (objective) {
            case CAPTURE, HOLD_ZONE, REACH_EXIT -> Math.pow(advantage, 0.60);
            case SURVIVE, PROTECT -> Math.pow(advantage, -0.45);
            default -> 1.0;
        };
    }

    static double captureDurationScale(StoryCampaign.Mission mission,
                                       StoryCampaign.Difficulty difficulty) {
        return Math.pow(advantage(mission, difficulty), -0.55);
    }

    static Profile profile(double easy, double normal, double hard) {
        return new Profile(easy, normal, hard);
    }

    static void harnessOverrideAdvantage(String missionId,
                                         StoryCampaign.Difficulty difficulty,
                                         double advantage) {
        if (missionId == null || difficulty == null) return;
        HARNESS_ADVANTAGE_OVERRIDES.get().put(overrideKey(missionId, difficulty), bounded(advantage));
    }

    static void harnessClearAdvantageOverrides() {
        HARNESS_ADVANTAGE_OVERRIDES.remove();
    }

    private static String overrideKey(String missionId, StoryCampaign.Difficulty difficulty) {
        return missionId + ':' + difficulty.name();
    }

    private static double bounded(double value) {
        return Math.clamp(value, 0.10, 8.00);
    }
}
