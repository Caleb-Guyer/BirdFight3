package com.example.birdgame3;

import java.util.Locale;

/**
 * The saved local-versus rulesets shown before fighter select.
 *
 * <p>These intentionally map onto the game's existing, proven simulation modes
 * instead of introducing presentation-only switches that the match cannot honor.</p>
 */
enum VersusRulesPreset {
    STANDARD(
            "STANDARD SMASH",
            "THE BIRD FIGHT 3 DEFAULT",
            VersusRules.standard(),
            "#EF5350"
    ),
    COMPETITIVE(
            "COMPETITIVE",
            "TOURNAMENT RULES",
            VersusRules.competitive(),
            "#42A5F5"
    ),
    CHAOS(
            "POWER-UP CHAOS",
            "ONE MUTATOR EVERY MATCH",
            VersusRules.chaos(),
            "#FFB300"
    ),
    CUSTOM(
            "CUSTOM RULES",
            "THREE SAVED RULESETS",
            VersusRules.standard().withName("CUSTOM RULES"),
            "#AB47BC"
    );

    final String title;
    final String eyebrow;
    final String summary;
    final VersusRules rules;
    final boolean competitionMode;
    final boolean mutatorMode;
    final String accent;

    VersusRulesPreset(String title, String eyebrow, VersusRules rules, String accent) {
        this.title = title;
        this.eyebrow = eyebrow;
        this.rules = rules;
        this.summary = rules.summary();
        this.competitionMode = rules.seriesWins() > 1;
        this.mutatorMode = rules.mutatorsEnabled();
        this.accent = accent;
    }

    static VersusRulesPreset fromPreference(String value) {
        if (value == null || value.isBlank()) {
            return STANDARD;
        }
        try {
            return valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ignored) {
            return STANDARD;
        }
    }
}
