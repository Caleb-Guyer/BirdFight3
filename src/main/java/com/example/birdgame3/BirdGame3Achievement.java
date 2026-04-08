package com.example.birdgame3;

import java.util.Locale;

enum BirdGame3Achievement {
    FIRST_BLOOD(0, "First Blood", "Get your first elimination in a match", BirdGame3AchievementCategory.COMBAT, "blood-drop"),
    DOMINATOR(1, "Dominator", "Eliminate 3 opponents in one match (max possible in 4-player)", BirdGame3AchievementCategory.COMBAT, "triple-skull"),
    ANNIHILATOR(2, "Annihilator", "Eliminate all other opponents in one match", BirdGame3AchievementCategory.COMBAT, "annihilation-burst"),
    TURKEY_SLAM_MASTER(3, "Turkey Slam Master", "Perform 3 ground pounds in one match (Turkey only)", BirdGame3AchievementCategory.BIRD, "slam-impact"),
    LEAN_GOD(4, "Lean God", "Spend a total of 30 seconds inside your lean cloud (Opium Bird only)", BirdGame3AchievementCategory.BIRD, "lean-cloud"),
    LOUNGE_LIZARD(5, "Lounge Lizard", "Heal a total of 100 HP inside the lounge (Mockingbird only)", BirdGame3AchievementCategory.BIRD, "lounge-heart"),
    POWER_UP_HOARDER(6, "Power-Up Hoarder", "Pick up 10 power-ups in one match", BirdGame3AchievementCategory.COMBAT, "powerup-cache"),
    FALL_GUY(7, "Fall Guy", "Fall off the bottom of the map 3 times", BirdGame3AchievementCategory.COMBAT, "void-fall"),
    TAUNT_LORD(8, "Taunt Lord", "Perform 10 taunts in total", BirdGame3AchievementCategory.COMBAT, "taunt-horn"),
    CLUTCH_GOD(9, "Clutch God", "Win a match with less than 20 HP remaining", BirdGame3AchievementCategory.COMBAT, "clutch-heart"),
    ROOFTOP_RUNNER(10, "Rooftop Runner", "Perform 20 high jumps on rooftops in the City map", BirdGame3AchievementCategory.MAP, "rooftop-arc"),
    NEON_ADDICT(11, "Neon Addict", "Pick up 8 Neon Boost power-ups across matches", BirdGame3AchievementCategory.MAP, "neon-bolt"),
    URBAN_KING(12, "Urban King", "Win 5 matches on Pigeon's Rooftops", BirdGame3AchievementCategory.MAP, "urban-crown"),
    THERMAL_RIDER(13, "Thermal Rider", "Pick up 10 Thermal Rise power-ups across matches", BirdGame3AchievementCategory.MAP, "thermal-spiral"),
    CLIFF_DIVER(14, "Cliff Diver", "Perform 15 jumps from the highest cliffs (Sky Cliffs map)", BirdGame3AchievementCategory.MAP, "cliff-dive"),
    SKY_EMPEROR(15, "Sky Emperor", "Win 5 matches on Sky Cliffs", BirdGame3AchievementCategory.MAP, "sky-crown"),
    VINE_SWINGER(16, "Vine Swinger", "Pick up 8 Vine Grapple power-ups across matches", BirdGame3AchievementCategory.MAP, "vine-swing"),
    CANOPY_KING(17, "Canopy King", "Win 5 matches on Vibrant Jungle", BirdGame3AchievementCategory.MAP, "canopy-crown"),
    PELICAN_KING(18, "Pelican King", "Pelican Plunge 15 enemies across matches", BirdGame3AchievementCategory.BIRD, "pelican-plunge"),
    CLASSIC_CREST(19, "Classic Crest", "Complete Classic mode with any bird", BirdGame3AchievementCategory.MODE, "classic-crest"),
    ECHOES_BELOW(20, "Echoes Below", "Complete Adventure Chapter 2: Echoes Below", BirdGame3AchievementCategory.STORY, "cave-echo"),
    STORY_KEEPER(21, "Story Keeper", "Complete all Adventure chapters", BirdGame3AchievementCategory.STORY, "story-book"),
    BOSS_BREAKER(22, "Boss Breaker", "Clear Boss Rush once", BirdGame3AchievementCategory.MODE, "boss-breaker"),
    CROWN_UNBROKEN(23, "Crown Unbroken", "Clear the Boss Rush EX route", BirdGame3AchievementCategory.MODE, "void-crown"),
    GROVE_SENTINEL(24, "Grove Sentinel", "Earn any Tower Defense badge", BirdGame3AchievementCategory.MODE, "grove-shield"),
    ROOFTOP_LEGACY(25, "Rooftop Legacy", "Complete the Pigeon Episode", BirdGame3AchievementCategory.STORY, "storm-rooftop"),
    ECHO_SOVEREIGN(26, "Echo Sovereign", "Complete the Bat Episode", BirdGame3AchievementCategory.STORY, "echo-rings"),
    IRON_TEMPEST(27, "Iron Tempest", "Complete the Pelican Episode", BirdGame3AchievementCategory.STORY, "iron-wing"),
    BLIGHT_BUSTER(28, "Blight Buster", "Earn every Big Forest Tower Defense badge", BirdGame3AchievementCategory.MODE, "tower-triad"),
    BRACKET_BOSS(29, "Bracket Boss", "Win a Tournament as a human entrant", BirdGame3AchievementCategory.MODE, "bracket-crown");

    private static final BirdGame3Achievement[] VALUES = values();

    final int legacyIndex;
    final String displayName;
    final String description;
    final BirdGame3AchievementCategory category;
    final String iconVariantKey;

    BirdGame3Achievement(int legacyIndex, String displayName, String description,
                         BirdGame3AchievementCategory category, String iconVariantKey) {
        this.legacyIndex = legacyIndex;
        this.displayName = displayName;
        this.description = description;
        this.category = category;
        this.iconVariantKey = iconVariantKey;
    }

    static BirdGame3Achievement fromLegacyIndex(int legacyIndex) {
        if (legacyIndex < 0 || legacyIndex >= VALUES.length) {
            throw new IllegalArgumentException("Unknown achievement index: " + legacyIndex);
        }
        return VALUES[legacyIndex];
    }

    static int count() {
        return VALUES.length;
    }

    static String[] displayNames() {
        String[] names = new String[VALUES.length];
        for (BirdGame3Achievement achievement : VALUES) {
            names[achievement.legacyIndex] = achievement.displayName;
        }
        return names;
    }

    static String[] descriptions() {
        String[] descriptions = new String[VALUES.length];
        for (BirdGame3Achievement achievement : VALUES) {
            descriptions[achievement.legacyIndex] = achievement.description;
        }
        return descriptions;
    }

    String title() {
        return displayName.toUpperCase(Locale.ROOT) + "!";
    }
}
