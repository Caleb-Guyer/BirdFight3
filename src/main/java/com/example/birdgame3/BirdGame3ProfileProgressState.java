package com.example.birdgame3;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

final class BirdGame3ProfileProgressState {
    record Schema(int achievementCount, int maxCombatants, int matchHistoryLimit,
                  int mainAdventureChapterCount, int tempestAdventureChapterCount) {
        Schema {
            achievementCount = Math.max(0, achievementCount);
            maxCombatants = Math.max(0, maxCombatants);
            matchHistoryLimit = Math.max(0, matchHistoryLimit);
            mainAdventureChapterCount = Math.max(0, mainAdventureChapterCount);
            tempestAdventureChapterCount = Math.max(0, tempestAdventureChapterCount);
        }
    }

    private static final String KEY_ACHIEVEMENT_SCHEMA_VERSION = "achievement_schema_version";
    private static final String KEY_MATCH_HISTORY_COUNT = "match_history_count";
    private static final String KEY_MATCH_HISTORY_PREFIX = "match_history_entry_";
    private static final String KEY_DAILY_CHALLENGE_BEST = "daily_challenge_best_key";
    private static final String KEY_DAILY_CHALLENGE_BEST_PROGRESS = "daily_challenge_best_progress";
    private static final String KEY_DAILY_CHALLENGE_BEST_BIRD = "daily_challenge_best_bird";
    private static final String KEY_DAILY_CHALLENGE_BONUS = "daily_challenge_bonus_key";
    private static final String KEY_DAILY_CHALLENGE_BONUS_BIRD = "daily_challenge_bonus_bird";
    private static final String KEY_BOSS_RUSH_BEST_TIME = "boss_rush_best_time";
    private static final String KEY_BOSS_RUSH_BEST_BIRD = "boss_rush_best_bird";
    private static final String KEY_BOSS_RUSH_BEST_RANK = "boss_rush_best_rank";
    private static final String KEY_BOSS_RUSH_CLEAR_COUNT = "boss_rush_clear_count";
    private static final String KEY_BOSS_RUSH_BEST_TIME_PREFIX = "boss_rush_best_time_";
    private static final String KEY_BOSS_RUSH_BEST_RANK_PREFIX = "boss_rush_best_rank_";
    private static final String KEY_BOSS_RUSH_PERFECT_PREFIX = "boss_rush_perfect_";
    private static final String KEY_GUIDED_TUTORIAL_COMPLETED = "academy_guided_tutorial_completed";
    private static final String KEY_BIRD_TRIAL_PREFIX = "academy_bird_trial_";
    private static final String KEY_DEVELOPER_INFINITE_BIRD_COINS = "developer_infinite_bird_coins";

    int achievementSchemaVersion = 0;
    BirdGame3AchievementProfile achievementProfile = new BirdGame3AchievementProfile();
    List<MatchHistoryEntry> matchHistory = new ArrayList<>();
    int classicContinues = 0;
    boolean desertMapUnlocked = false;
    boolean caveMapUnlocked = false;
    boolean battlefieldMapUnlocked = false;
    boolean beaconCrownMapUnlocked = false;
    boolean dockMapUnlocked = false;
    boolean[][] towerDefenseDifficultyBadges =
            new boolean[BirdGame3.MapType.values().length][TowerDefenseMode.Difficulty.values().length];
    boolean cityPigeonUnlocked = true;
    boolean noirPigeonUnlocked = false;
    boolean freemanPigeonUnlocked = false;
    boolean beaconPigeonUnlocked = false;
    boolean stormPigeonUnlocked = false;
    boolean eagleSkinUnlocked = true;
    boolean novaPhoenixUnlocked = false;
    boolean duneFalconUnlocked = false;
    boolean mintPenguinUnlocked = false;
    boolean circuitTitmouseUnlocked = false;
    boolean prismRazorbillUnlocked = false;
    boolean auroraPelicanUnlocked = false;
    boolean ironcladPelicanUnlocked = false;
    boolean sunflareHummingbirdUnlocked = false;
    boolean glacierShoebillUnlocked = false;
    boolean tideVultureUnlocked = false;
    boolean nullRockVultureUnlocked = false;
    boolean eclipseMockingbirdUnlocked = false;
    boolean umbraBatUnlocked = false;
    boolean resonanceBatUnlocked = false;
    boolean sunforgeRoosterUnlocked = false;
    boolean batUnlocked = false;
    boolean falconUnlocked = false;
    boolean heisenbirdUnlocked = false;
    boolean phoenixUnlocked = false;
    boolean titmouseUnlocked = false;
    boolean ravenUnlocked = false;
    boolean roadrunnerUnlocked = false;
    boolean roosterUnlocked = false;
    boolean developerInfiniteBirdCoins = false;
    boolean guidedTutorialCompleted = false;
    boolean[] birdTrialCompleted = new boolean[BirdGame3.BirdType.values().length];
    String dailyChallengeBestKey = "";
    int dailyChallengeBestProgress = 0;
    BirdGame3.BirdType dailyChallengeBestBird = null;
    String dailyChallengeBonusClaimedKey = "";
    BirdGame3.BirdType dailyChallengeBonusBird = null;
    long bossRushBestClearMillis = Long.MAX_VALUE;
    BirdGame3.BirdType bossRushBestBird = null;
    String bossRushBestRank = "";
    int bossRushClearCount = 0;
    long[] bossRushBestClearMillisByBird = filledLongArray(BirdGame3.BirdType.values().length);
    String[] bossRushBestRankByBird = new String[BirdGame3.BirdType.values().length];
    boolean[] bossRushPerfectBadgeByBird = new boolean[BirdGame3.BirdType.values().length];
    int pigeonEpisodeUnlockedChapters = 1;
    boolean pigeonEpisodeCompleted = false;
    int batEpisodeUnlockedChapters = 1;
    boolean batEpisodeCompleted = false;
    int pelicanEpisodeUnlockedChapters = 1;
    boolean pelicanEpisodeCompleted = false;
    String selectedAdventureRouteName = "MAIN";
    boolean[] mainAdventureUnlocked = new boolean[BirdGame3.BirdType.values().length];
    boolean[] tempestAdventureUnlocked = new boolean[BirdGame3.BirdType.values().length];
    int[] mainAdventureChapterProgress = new int[0];
    boolean[] mainAdventureChapterCompleted = new boolean[0];
    int[] tempestAdventureChapterProgress = new int[0];
    boolean[] tempestAdventureChapterCompleted = new boolean[0];
    boolean[] classicCompleted = new boolean[BirdGame3.BirdType.values().length];
    boolean[] classicSkinUnlocked = new boolean[BirdGame3.BirdType.values().length];
    int[] cityWins = new int[0];
    int[] cliffWins = new int[0];
    int[] jungleWins = new int[0];
    int[] rooftopJumps = new int[0];
    int[] neonPickups = new int[0];
    int[] thermalPickups = new int[0];
    int[] highCliffJumps = new int[0];
    int[] vineGrapplePickups = new int[0];
    int[] typePicks = new int[BirdGame3.BirdType.values().length];
    int[] typeWins = new int[BirdGame3.BirdType.values().length];
    int[] typeDamage = new int[BirdGame3.BirdType.values().length];
    int[] typeElims = new int[BirdGame3.BirdType.values().length];
    int tournamentChampionshipsWon = 0;

    static BirdGame3ProfileProgressState load(Preferences prefs, Schema schema) {
        BirdGame3ProfileProgressState state = new BirdGame3ProfileProgressState();
        state.initializeArrays(schema);
        if (prefs == null) {
            return state;
        }

        state.achievementSchemaVersion = Math.max(0, prefs.getInt(KEY_ACHIEVEMENT_SCHEMA_VERSION, 0));
        loadAchievements(prefs, state);
        loadMatchHistory(prefs, schema.matchHistoryLimit(), state);
        state.classicContinues = Math.max(0, prefs.getInt("classic_continues", 0));
        state.desertMapUnlocked = prefs.getBoolean("map_desert_unlocked", false);
        state.caveMapUnlocked = prefs.getBoolean("map_cave_unlocked", false);
        state.battlefieldMapUnlocked = prefs.getBoolean("map_battlefield_unlocked", false);
        state.beaconCrownMapUnlocked = prefs.getBoolean("map_beacon_crown_unlocked", false);
        state.dockMapUnlocked = prefs.getBoolean("map_dock_unlocked", false);
        loadTowerDefenseBadges(prefs, state);
        loadSkinUnlocks(prefs, state);
        loadCharacterUnlocks(prefs, state);
        state.developerInfiniteBirdCoins = prefs.getBoolean(KEY_DEVELOPER_INFINITE_BIRD_COINS, false);
        loadDailyChallenge(prefs, state);
        loadBossRush(prefs, state);
        loadEpisodeProgress(prefs, state);
        loadAdventureProgress(prefs, state);
        loadClassicProgress(prefs, state);
        loadTrainingAcademy(prefs, state);
        loadPlayerProgressStats(prefs, state);
        loadBalanceTelemetry(prefs, state);
        return state;
    }

    void saveTo(Preferences prefs, Schema schema) {
        if (prefs == null) {
            return;
        }
        prefs.remove("start_here_completed");
        removeLegacyProgressionPrefs(prefs);
        prefs.putInt(KEY_ACHIEVEMENT_SCHEMA_VERSION, Math.max(0, achievementSchemaVersion));
        saveAchievements(prefs, schema.achievementCount());
        saveMatchHistory(prefs, schema.matchHistoryLimit());
        prefs.putInt("classic_continues", Math.max(0, classicContinues));
        prefs.putBoolean("map_desert_unlocked", desertMapUnlocked);
        prefs.putBoolean("map_cave_unlocked", caveMapUnlocked);
        prefs.putBoolean("map_battlefield_unlocked", battlefieldMapUnlocked);
        prefs.putBoolean("map_beacon_crown_unlocked", beaconCrownMapUnlocked);
        prefs.putBoolean("map_dock_unlocked", dockMapUnlocked);
        saveTowerDefenseBadges(prefs);
        saveSkinUnlocks(prefs);
        saveCharacterUnlocks(prefs);
        prefs.putBoolean(KEY_DEVELOPER_INFINITE_BIRD_COINS, developerInfiniteBirdCoins);
        saveDailyChallenge(prefs);
        saveBossRush(prefs);
        saveClassicProgress(prefs);
        saveEpisodeProgress(prefs);
        saveAdventureProgress(prefs, schema);
        saveTrainingAcademy(prefs);
        savePlayerProgressStats(prefs, schema.maxCombatants());
        saveBalanceTelemetry(prefs);
    }

    private void initializeArrays(Schema schema) {
        achievementProfile = new BirdGame3AchievementProfile();
        matchHistory = new ArrayList<>();
        mainAdventureChapterProgress = new int[schema.mainAdventureChapterCount()];
        mainAdventureChapterCompleted = new boolean[schema.mainAdventureChapterCount()];
        tempestAdventureChapterProgress = new int[schema.tempestAdventureChapterCount()];
        tempestAdventureChapterCompleted = new boolean[schema.tempestAdventureChapterCount()];
        cityWins = new int[schema.maxCombatants()];
        cliffWins = new int[schema.maxCombatants()];
        jungleWins = new int[schema.maxCombatants()];
        rooftopJumps = new int[schema.maxCombatants()];
        neonPickups = new int[schema.maxCombatants()];
        thermalPickups = new int[schema.maxCombatants()];
        highCliffJumps = new int[schema.maxCombatants()];
        vineGrapplePickups = new int[schema.maxCombatants()];
        birdTrialCompleted = new boolean[BirdGame3.BirdType.values().length];
    }

    private static void loadAchievements(Preferences prefs, BirdGame3ProfileProgressState state) {
        for (BirdGame3Achievement achievement : BirdGame3Achievement.values()) {
            int index = achievement.legacyIndex;
            state.achievementProfile.setUnlocked(achievement, prefs.getBoolean("ach_" + index, false));
            state.achievementProfile.setProgress(achievement, prefs.getInt("prog_" + index, 0));
            state.achievementProfile.setRewardClaimed(achievement, prefs.getBoolean("ach_reward_claimed_" + index, false));
        }
    }

    private void saveAchievements(Preferences prefs, int achievementCount) {
        for (BirdGame3Achievement achievement : BirdGame3Achievement.values()) {
            if (achievement.legacyIndex >= achievementCount) {
                continue;
            }
            int index = achievement.legacyIndex;
            prefs.putBoolean("ach_" + index, achievementProfile.isUnlocked(achievement));
            prefs.putInt("prog_" + index, achievementProfile.progress(achievement));
            prefs.putBoolean("ach_reward_claimed_" + index, achievementProfile.isRewardClaimed(achievement));
        }
    }

    private static void loadMatchHistory(Preferences prefs, int matchHistoryLimit, BirdGame3ProfileProgressState state) {
        int count = Math.clamp(prefs.getInt(KEY_MATCH_HISTORY_COUNT, 0), 0, matchHistoryLimit);
        for (int i = 0; i < count; i++) {
            MatchHistoryEntry entry = MatchHistoryEntry.deserialize(prefs.get(KEY_MATCH_HISTORY_PREFIX + i, null));
            if (entry != null) {
                state.matchHistory.add(entry);
            }
        }
    }

    private void saveMatchHistory(Preferences prefs, int matchHistoryLimit) {
        int count = Math.min(matchHistory.size(), matchHistoryLimit);
        prefs.putInt(KEY_MATCH_HISTORY_COUNT, count);
        for (int i = 0; i < count; i++) {
            prefs.put(KEY_MATCH_HISTORY_PREFIX + i, matchHistory.get(i).serialize());
        }
        for (int i = count; i < matchHistoryLimit; i++) {
            prefs.remove(KEY_MATCH_HISTORY_PREFIX + i);
        }
    }

    private static void loadTowerDefenseBadges(Preferences prefs, BirdGame3ProfileProgressState state) {
        for (BirdGame3.MapType map : BirdGame3.MapType.values()) {
            for (TowerDefenseMode.Difficulty difficulty : TowerDefenseMode.Difficulty.values()) {
                state.towerDefenseDifficultyBadges[map.ordinal()][difficulty.ordinal()] =
                        prefs.getBoolean("td_badge_" + map.name() + "_" + difficulty.name(), false);
            }
        }
    }

    private void saveTowerDefenseBadges(Preferences prefs) {
        for (BirdGame3.MapType map : BirdGame3.MapType.values()) {
            for (TowerDefenseMode.Difficulty difficulty : TowerDefenseMode.Difficulty.values()) {
                prefs.putBoolean("td_badge_" + map.name() + "_" + difficulty.name(),
                        towerDefenseDifficultyBadges[map.ordinal()][difficulty.ordinal()]);
            }
        }
    }

    private static void loadSkinUnlocks(Preferences prefs, BirdGame3ProfileProgressState state) {
        state.cityPigeonUnlocked = prefs.getBoolean("skin_citypigeon", true);
        state.noirPigeonUnlocked = prefs.getBoolean("skin_noirpigeon", false);
        state.freemanPigeonUnlocked = prefs.getBoolean("skin_freeman_pigeon", false);
        state.beaconPigeonUnlocked = prefs.getBoolean("skin_beacon_pigeon", false);
        state.stormPigeonUnlocked = prefs.getBoolean("skin_storm_pigeon", false);
        state.eagleSkinUnlocked = prefs.getBoolean("skin_eagle", true);
        state.novaPhoenixUnlocked = prefs.getBoolean("skin_nova_phoenix", false);
        state.duneFalconUnlocked = prefs.getBoolean("skin_dune_falcon", false);
        state.mintPenguinUnlocked = prefs.getBoolean("skin_mint_penguin", false);
        state.circuitTitmouseUnlocked = prefs.getBoolean("skin_circuit_titmouse", false);
        state.prismRazorbillUnlocked = prefs.getBoolean("skin_prism_razorbill", false);
        state.auroraPelicanUnlocked = prefs.getBoolean("skin_aurora_pelican", false);
        state.ironcladPelicanUnlocked = prefs.getBoolean("skin_ironclad_pelican", false);
        state.sunflareHummingbirdUnlocked = prefs.getBoolean("skin_sunflare_hummingbird", false);
        state.glacierShoebillUnlocked = prefs.getBoolean("skin_glacier_shoebill", false);
        state.tideVultureUnlocked = prefs.getBoolean("skin_tide_vulture", false);
        state.nullRockVultureUnlocked = prefs.getBoolean("skin_null_rock_vulture", false);
        state.eclipseMockingbirdUnlocked = prefs.getBoolean("skin_eclipse_mockingbird", false);
        state.umbraBatUnlocked = prefs.getBoolean("skin_umbra_bat", false);
        state.resonanceBatUnlocked = prefs.getBoolean("skin_resonance_bat", false);
        state.sunforgeRoosterUnlocked = prefs.getBoolean("skin_sunforge_rooster", false);
    }

    private void saveSkinUnlocks(Preferences prefs) {
        prefs.putBoolean("skin_citypigeon", cityPigeonUnlocked);
        prefs.putBoolean("skin_noirpigeon", noirPigeonUnlocked);
        prefs.putBoolean("skin_freeman_pigeon", freemanPigeonUnlocked);
        prefs.putBoolean("skin_beacon_pigeon", beaconPigeonUnlocked);
        prefs.putBoolean("skin_storm_pigeon", stormPigeonUnlocked);
        prefs.putBoolean("skin_eagle", eagleSkinUnlocked);
        prefs.putBoolean("skin_nova_phoenix", novaPhoenixUnlocked);
        prefs.putBoolean("skin_dune_falcon", duneFalconUnlocked);
        prefs.putBoolean("skin_mint_penguin", mintPenguinUnlocked);
        prefs.putBoolean("skin_circuit_titmouse", circuitTitmouseUnlocked);
        prefs.putBoolean("skin_prism_razorbill", prismRazorbillUnlocked);
        prefs.putBoolean("skin_aurora_pelican", auroraPelicanUnlocked);
        prefs.putBoolean("skin_ironclad_pelican", ironcladPelicanUnlocked);
        prefs.putBoolean("skin_sunflare_hummingbird", sunflareHummingbirdUnlocked);
        prefs.putBoolean("skin_glacier_shoebill", glacierShoebillUnlocked);
        prefs.putBoolean("skin_tide_vulture", tideVultureUnlocked);
        prefs.putBoolean("skin_null_rock_vulture", nullRockVultureUnlocked);
        prefs.putBoolean("skin_eclipse_mockingbird", eclipseMockingbirdUnlocked);
        prefs.putBoolean("skin_umbra_bat", umbraBatUnlocked);
        prefs.putBoolean("skin_resonance_bat", resonanceBatUnlocked);
        prefs.putBoolean("skin_sunforge_rooster", sunforgeRoosterUnlocked);
    }

    private static void loadCharacterUnlocks(Preferences prefs, BirdGame3ProfileProgressState state) {
        state.batUnlocked = prefs.getBoolean("char_bat_unlocked", false);
        state.falconUnlocked = prefs.getBoolean("char_falcon_unlocked", false);
        state.heisenbirdUnlocked = prefs.getBoolean("char_heisenbird_unlocked", false);
        state.phoenixUnlocked = prefs.getBoolean("char_phoenix_unlocked", false);
        state.titmouseUnlocked = prefs.getBoolean("char_titmouse_unlocked", false);
        state.ravenUnlocked = prefs.getBoolean("char_raven_unlocked", false);
        state.roadrunnerUnlocked = prefs.getBoolean("char_roadrunner_unlocked", false);
        state.roosterUnlocked = prefs.getBoolean("char_rooster_unlocked", false);
    }

    private void saveCharacterUnlocks(Preferences prefs) {
        prefs.putBoolean("char_bat_unlocked", batUnlocked);
        prefs.putBoolean("char_falcon_unlocked", falconUnlocked);
        prefs.putBoolean("char_heisenbird_unlocked", heisenbirdUnlocked);
        prefs.putBoolean("char_phoenix_unlocked", phoenixUnlocked);
        prefs.putBoolean("char_titmouse_unlocked", titmouseUnlocked);
        prefs.putBoolean("char_raven_unlocked", ravenUnlocked);
        prefs.putBoolean("char_roadrunner_unlocked", roadrunnerUnlocked);
        prefs.putBoolean("char_rooster_unlocked", roosterUnlocked);
    }

    private static void loadDailyChallenge(Preferences prefs, BirdGame3ProfileProgressState state) {
        state.dailyChallengeBestKey = prefs.get(KEY_DAILY_CHALLENGE_BEST, "");
        state.dailyChallengeBestProgress = Math.max(0, prefs.getInt(KEY_DAILY_CHALLENGE_BEST_PROGRESS, 0));
        state.dailyChallengeBestBird = parseBirdTypePreference(prefs.get(KEY_DAILY_CHALLENGE_BEST_BIRD, ""));
        state.dailyChallengeBonusClaimedKey = prefs.get(KEY_DAILY_CHALLENGE_BONUS, "");
        state.dailyChallengeBonusBird = parseBirdTypePreference(prefs.get(KEY_DAILY_CHALLENGE_BONUS_BIRD, ""));
    }

    private void saveDailyChallenge(Preferences prefs) {
        prefs.put(KEY_DAILY_CHALLENGE_BEST, nullToEmpty(dailyChallengeBestKey));
        prefs.putInt(KEY_DAILY_CHALLENGE_BEST_PROGRESS, Math.max(0, dailyChallengeBestProgress));
        prefs.put(KEY_DAILY_CHALLENGE_BEST_BIRD, dailyChallengeBestBird == null ? "" : dailyChallengeBestBird.name());
        prefs.put(KEY_DAILY_CHALLENGE_BONUS, nullToEmpty(dailyChallengeBonusClaimedKey));
        prefs.put(KEY_DAILY_CHALLENGE_BONUS_BIRD, dailyChallengeBonusBird == null ? "" : dailyChallengeBonusBird.name());
    }

    private static void loadBossRush(Preferences prefs, BirdGame3ProfileProgressState state) {
        state.bossRushBestClearMillis = prefs.getLong(KEY_BOSS_RUSH_BEST_TIME, Long.MAX_VALUE);
        state.bossRushBestBird = parseBirdTypePreference(prefs.get(KEY_BOSS_RUSH_BEST_BIRD, ""));
        state.bossRushBestRank = prefs.get(KEY_BOSS_RUSH_BEST_RANK, "");
        state.bossRushClearCount = Math.max(0, prefs.getInt(KEY_BOSS_RUSH_CLEAR_COUNT, 0));
        state.tournamentChampionshipsWon = Math.max(0, prefs.getInt("tournament_championships", 0));
        Arrays.fill(state.bossRushBestClearMillisByBird, Long.MAX_VALUE);
        Arrays.fill(state.bossRushBestRankByBird, "");
        Arrays.fill(state.bossRushPerfectBadgeByBird, false);
        for (BirdGame3.BirdType type : BirdGame3.BirdType.values()) {
            int idx = type.ordinal();
            state.bossRushBestClearMillisByBird[idx] =
                    prefs.getLong(KEY_BOSS_RUSH_BEST_TIME_PREFIX + type.name(), Long.MAX_VALUE);
            state.bossRushBestRankByBird[idx] =
                    prefs.get(KEY_BOSS_RUSH_BEST_RANK_PREFIX + type.name(), "");
            state.bossRushPerfectBadgeByBird[idx] =
                    prefs.getBoolean(KEY_BOSS_RUSH_PERFECT_PREFIX + type.name(), false);
        }
    }

    private void saveBossRush(Preferences prefs) {
        prefs.putLong(KEY_BOSS_RUSH_BEST_TIME, bossRushBestClearMillis <= 0L ? Long.MAX_VALUE : bossRushBestClearMillis);
        prefs.put(KEY_BOSS_RUSH_BEST_BIRD, bossRushBestBird == null ? "" : bossRushBestBird.name());
        prefs.put(KEY_BOSS_RUSH_BEST_RANK, nullToEmpty(bossRushBestRank));
        prefs.putInt(KEY_BOSS_RUSH_CLEAR_COUNT, Math.max(0, bossRushClearCount));
        prefs.putInt("tournament_championships", Math.max(0, tournamentChampionshipsWon));
        for (BirdGame3.BirdType type : BirdGame3.BirdType.values()) {
            int idx = type.ordinal();
            long bestElapsedMillis = idx < bossRushBestClearMillisByBird.length
                    ? bossRushBestClearMillisByBird[idx]
                    : Long.MAX_VALUE;
            prefs.putLong(KEY_BOSS_RUSH_BEST_TIME_PREFIX + type.name(),
                    bestElapsedMillis <= 0L ? Long.MAX_VALUE : bestElapsedMillis);
            String rank = idx < bossRushBestRankByBird.length ? bossRushBestRankByBird[idx] : "";
            prefs.put(KEY_BOSS_RUSH_BEST_RANK_PREFIX + type.name(), rank == null ? "" : rank);
            boolean perfect = idx < bossRushPerfectBadgeByBird.length && bossRushPerfectBadgeByBird[idx];
            prefs.putBoolean(KEY_BOSS_RUSH_PERFECT_PREFIX + type.name(), perfect);
        }
    }

    private static void loadClassicProgress(Preferences prefs, BirdGame3ProfileProgressState state) {
        for (BirdGame3.BirdType type : BirdGame3.BirdType.values()) {
            int idx = type.ordinal();
            state.classicCompleted[idx] = prefs.getBoolean("classic_done_" + type.name(), false);
            state.classicSkinUnlocked[idx] = prefs.getBoolean("classic_skin_" + type.name(), false);
        }
    }

    private void saveClassicProgress(Preferences prefs) {
        for (BirdGame3.BirdType type : BirdGame3.BirdType.values()) {
            int idx = type.ordinal();
            prefs.putBoolean("classic_done_" + type.name(), classicCompleted[idx]);
            prefs.putBoolean("classic_skin_" + type.name(), classicSkinUnlocked[idx]);
        }
    }

    private static void loadEpisodeProgress(Preferences prefs, BirdGame3ProfileProgressState state) {
        state.pigeonEpisodeUnlockedChapters = Math.max(1, prefs.getInt("ep_pigeon_unlocked", 1));
        state.pigeonEpisodeCompleted = prefs.getBoolean("ep_pigeon_completed", false);
        state.batEpisodeUnlockedChapters = Math.max(1, prefs.getInt("ep_bat_unlocked", 1));
        state.batEpisodeCompleted = prefs.getBoolean("ep_bat_completed", false);
        state.pelicanEpisodeUnlockedChapters = Math.max(1, prefs.getInt("ep_pelican_unlocked", 1));
        state.pelicanEpisodeCompleted = prefs.getBoolean("ep_pelican_completed", false);
    }

    private void saveEpisodeProgress(Preferences prefs) {
        prefs.putInt("ep_pigeon_unlocked", Math.max(1, pigeonEpisodeUnlockedChapters));
        prefs.putBoolean("ep_pigeon_completed", pigeonEpisodeCompleted);
        prefs.putInt("ep_bat_unlocked", Math.max(1, batEpisodeUnlockedChapters));
        prefs.putBoolean("ep_bat_completed", batEpisodeCompleted);
        prefs.putInt("ep_pelican_unlocked", Math.max(1, pelicanEpisodeUnlockedChapters));
        prefs.putBoolean("ep_pelican_completed", pelicanEpisodeCompleted);
    }

    private static void loadAdventureProgress(Preferences prefs, BirdGame3ProfileProgressState state) {
        state.selectedAdventureRouteName = prefs.get("adv_route_selected", "MAIN");
        for (BirdGame3.BirdType type : BirdGame3.BirdType.values()) {
            int idx = type.ordinal();
            state.mainAdventureUnlocked[idx] = prefs.getBoolean("adv_bird_" + type.name(), false);
            state.tempestAdventureUnlocked[idx] = prefs.getBoolean("adv_tempest_bird_" + type.name(), false);
        }
        for (int i = 0; i < state.mainAdventureChapterProgress.length; i++) {
            state.mainAdventureChapterProgress[i] = Math.max(0, prefs.getInt("adv_ch" + (i + 1) + "_progress", 0));
            state.mainAdventureChapterCompleted[i] = prefs.getBoolean("adv_ch" + (i + 1) + "_done", false);
        }
        for (int i = 0; i < state.tempestAdventureChapterProgress.length; i++) {
            state.tempestAdventureChapterProgress[i] = Math.max(0, prefs.getInt("adv_tempest_ch" + (i + 1) + "_progress", 0));
            state.tempestAdventureChapterCompleted[i] = prefs.getBoolean("adv_tempest_ch" + (i + 1) + "_done", false);
        }
    }

    private void saveAdventureProgress(Preferences prefs, Schema schema) {
        prefs.put("adv_route_selected", selectedAdventureRouteName == null || selectedAdventureRouteName.isBlank()
                ? "MAIN"
                : selectedAdventureRouteName);
        for (int i = 0; i < schema.mainAdventureChapterCount(); i++) {
            int progress = i < mainAdventureChapterProgress.length ? mainAdventureChapterProgress[i] : 0;
            boolean done = i < mainAdventureChapterCompleted.length && mainAdventureChapterCompleted[i];
            prefs.putInt("adv_ch" + (i + 1) + "_progress", progress);
            prefs.putBoolean("adv_ch" + (i + 1) + "_done", done);
        }
        for (int i = 0; i < schema.tempestAdventureChapterCount(); i++) {
            int progress = i < tempestAdventureChapterProgress.length ? tempestAdventureChapterProgress[i] : 0;
            boolean done = i < tempestAdventureChapterCompleted.length && tempestAdventureChapterCompleted[i];
            prefs.putInt("adv_tempest_ch" + (i + 1) + "_progress", progress);
            prefs.putBoolean("adv_tempest_ch" + (i + 1) + "_done", done);
        }
        for (BirdGame3.BirdType type : BirdGame3.BirdType.values()) {
            int idx = type.ordinal();
            prefs.putBoolean("adv_bird_" + type.name(), idx < mainAdventureUnlocked.length && mainAdventureUnlocked[idx]);
            prefs.putBoolean("adv_tempest_bird_" + type.name(),
                    idx < tempestAdventureUnlocked.length && tempestAdventureUnlocked[idx]);
        }
    }

    private static void loadPlayerProgressStats(Preferences prefs, BirdGame3ProfileProgressState state) {
        for (int i = 0; i < state.cityWins.length; i++) {
            state.cityWins[i] = prefs.getInt("city_wins_" + i, 0);
            state.cliffWins[i] = prefs.getInt("cliff_wins_" + i, 0);
            state.jungleWins[i] = prefs.getInt("jungle_wins_" + i, 0);
            state.rooftopJumps[i] = prefs.getInt("rooftop_jumps_" + i, 0);
            state.neonPickups[i] = prefs.getInt("neon_pickups_" + i, 0);
            state.thermalPickups[i] = prefs.getInt("thermal_pickups_" + i, 0);
            state.highCliffJumps[i] = prefs.getInt("high_cliff_jumps_" + i, 0);
            state.vineGrapplePickups[i] = prefs.getInt("vine_pickups_" + i, 0);
        }
    }

    private static void loadTrainingAcademy(Preferences prefs, BirdGame3ProfileProgressState state) {
        state.guidedTutorialCompleted = prefs.getBoolean(
                KEY_GUIDED_TUTORIAL_COMPLETED,
                prefs.getBoolean("start_here_completed", false)
        );
        for (BirdGame3.BirdType type : BirdGame3.BirdType.values()) {
            int idx = type.ordinal();
            if (idx < state.birdTrialCompleted.length) {
                state.birdTrialCompleted[idx] = prefs.getBoolean(KEY_BIRD_TRIAL_PREFIX + type.name(), false);
            }
        }
    }

    private void saveTrainingAcademy(Preferences prefs) {
        prefs.putBoolean(KEY_GUIDED_TUTORIAL_COMPLETED, guidedTutorialCompleted);
        for (BirdGame3.BirdType type : BirdGame3.BirdType.values()) {
            int idx = type.ordinal();
            prefs.putBoolean(
                    KEY_BIRD_TRIAL_PREFIX + type.name(),
                    idx < birdTrialCompleted.length && birdTrialCompleted[idx]
            );
        }
    }

    private void savePlayerProgressStats(Preferences prefs, int maxCombatants) {
        for (int i = 0; i < maxCombatants; i++) {
            prefs.putInt("city_wins_" + i, i < cityWins.length ? cityWins[i] : 0);
            prefs.putInt("cliff_wins_" + i, i < cliffWins.length ? cliffWins[i] : 0);
            prefs.putInt("jungle_wins_" + i, i < jungleWins.length ? jungleWins[i] : 0);
            prefs.putInt("rooftop_jumps_" + i, i < rooftopJumps.length ? rooftopJumps[i] : 0);
            prefs.putInt("neon_pickups_" + i, i < neonPickups.length ? neonPickups[i] : 0);
            prefs.putInt("thermal_pickups_" + i, i < thermalPickups.length ? thermalPickups[i] : 0);
            prefs.putInt("high_cliff_jumps_" + i, i < highCliffJumps.length ? highCliffJumps[i] : 0);
            prefs.putInt("vine_pickups_" + i, i < vineGrapplePickups.length ? vineGrapplePickups[i] : 0);
        }
    }

    private static void loadBalanceTelemetry(Preferences prefs, BirdGame3ProfileProgressState state) {
        for (BirdGame3.BirdType type : BirdGame3.BirdType.values()) {
            int idx = type.ordinal();
            state.typePicks[idx] = prefs.getInt("balance_picks_" + type.name(), 0);
            state.typeWins[idx] = prefs.getInt("balance_wins_" + type.name(), 0);
            state.typeDamage[idx] = prefs.getInt("balance_damage_" + type.name(), 0);
            state.typeElims[idx] = prefs.getInt("balance_elims_" + type.name(), 0);
        }
    }

    private void saveBalanceTelemetry(Preferences prefs) {
        for (BirdGame3.BirdType type : BirdGame3.BirdType.values()) {
            int idx = type.ordinal();
            prefs.putInt("balance_picks_" + type.name(), typePicks[idx]);
            prefs.putInt("balance_wins_" + type.name(), typeWins[idx]);
            prefs.putInt("balance_damage_" + type.name(), typeDamage[idx]);
            prefs.putInt("balance_elims_" + type.name(), typeElims[idx]);
        }
    }

    private static long[] filledLongArray(int length) {
        long[] values = new long[Math.max(0, length)];
        Arrays.fill(values, Long.MAX_VALUE);
        return values;
    }

    private static BirdGame3.BirdType parseBirdTypePreference(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return BirdGame3.BirdType.valueOf(value);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    private static void removeLegacyProgressionPrefs(Preferences prefs) {
        for (BirdGame3.BirdType type : BirdGame3.BirdType.values()) {
            prefs.remove("bird_mastery_xp_" + type.name());
        }
        try {
            for (String key : prefs.keys()) {
                if (key.startsWith("contracts_")) {
                    prefs.remove(key);
                }
            }
        } catch (BackingStoreException ignored) {
        }
    }
}
