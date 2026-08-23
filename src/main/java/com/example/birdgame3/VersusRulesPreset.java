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
            "3 stocks  •  2:30  •  items on  •  stage hazards on",
            false,
            false,
            "#EF5350"
    ),
    COMPETITIVE(
            "COMPETITIVE",
            "TOURNAMENT RULES",
            "3 stocks  •  2:00  •  items off  •  stage hazards off",
            true,
            false,
            "#42A5F5"
    ),
    CHAOS(
            "POWER-UP CHAOS",
            "ONE MUTATOR EVERY MATCH",
            "3 stocks  •  2:30  •  items on  •  hazards + mutators",
            false,
            true,
            "#FFB300"
    );

    final String title;
    final String eyebrow;
    final String summary;
    final boolean competitionMode;
    final boolean mutatorMode;
    final String accent;

    VersusRulesPreset(String title, String eyebrow, String summary,
                      boolean competitionMode, boolean mutatorMode, String accent) {
        this.title = title;
        this.eyebrow = eyebrow;
        this.summary = summary;
        this.competitionMode = competitionMode;
        this.mutatorMode = mutatorMode;
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
