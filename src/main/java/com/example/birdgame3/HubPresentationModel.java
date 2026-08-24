package com.example.birdgame3;

/** Concise contextual copy for the primary console-style mode dashboards. */
final class HubPresentationModel {
    static final String IDLE_TITLE = "BIRD FIGHT 3";
    static final String IDLE_DESCRIPTION = "Choose a mode.";
    static final int MAX_DESCRIPTION_LENGTH = 64;

    enum Destination {
        FIGHT("FIGHT", "Local battles and custom rules."),
        STORY("STORY — THE STILL SKY", "Continue the main story campaign."),
        GAMES("GAMES & MORE", "Arcade routes and focused challenges."),
        SHOP("SHOP", "Spend Bird Coins on unlocks and cosmetics."),
        NETWORK("NETWORK PLAY", "Host or join local and Internet matches."),
        VAULT("THE VAULT", "Review records and your collection."),
        SETTINGS("SETTINGS", "Tune how the game looks and controls."),
        PROFILES("PROFILES", "Switch or manage save profiles."),
        EXIT("EXIT", "Close Bird Fight 3.");

        private final String title;
        private final String description;

        Destination(String title, String description) {
            this.title = title;
            this.description = description;
        }

        String title() {
            return title;
        }

        String description() {
            return description;
        }
    }

    enum ExtraMode {
        CLASSIC("CLASSIC MODE", "Eight-round character routes and rewards."),
        ASHFALL("ASHFALL TRIAL", "A three-rite Phoenix challenge."),
        BOSS_RUSH("BOSS RUSH", "Fight the boss roster back to back."),
        LEGACY("LEGACY STORIES", "Replay the original adventures and episodes."),
        TOURNAMENT("TOURNAMENT", "Build and play a local bracket."),
        SQUAD_STRIKE("SQUAD STRIKE", "Battle with ordered three- or five-bird squads."),
        TRAINING("TRAINING", "Practice movement and combat.");

        private final String title;
        private final String description;

        ExtraMode(String title, String description) {
            this.title = title;
            this.description = description;
        }

        String title() {
            return title;
        }

        String description() {
            return description;
        }
    }

    private HubPresentationModel() {
    }
}
