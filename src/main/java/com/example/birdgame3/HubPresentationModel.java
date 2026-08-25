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
        VAULT("THE VAULT", "Explore your collection and battle history."),
        SHOP("SHOP", "Spend Bird Coins on unlocks and cosmetics."),
        NETWORK("NETWORK PLAY", "Host or join local and Internet matches."),
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

    enum FightMode {
        BIRD_BATTLE("BIRD BATTLE", "Set the rules for a local battle."),
        FLOCK_STRIKE("FLOCK STRIKE", "Battle with ordered three- or five-bird flocks."),
        ROOST_BRACKET("ROOST BRACKET", "Build and play a local elimination bracket."),
        WILD_RULES("WILD RULES", "Create unusual battles with stamina and mutators.");

        private final String title;
        private final String description;

        FightMode(String title, String description) {
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

    enum WildMode {
        CUSTOM_ROOST("BUILD-A-BRAWL", "Create and save a completely custom battle."),
        STAMINA_CLASH("STAMINA CLASH", "Drain every rival's HP before they drain yours."),
        LAUNCHSTORM("LAUNCHSTORM", "A volatile ruleset with maximum launch force.");

        private final String title;
        private final String description;

        WildMode(String title, String description) {
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

    enum AdventureMode {
        CONTINUE("CONTINUE ADVENTURE", "Return to the next Still Sky mission."),
        MISSION_BOARD("MISSION BOARD", "Choose an act or replay a cleared mission."),
        CHRONICLE("STORY CHRONICLE", "Replay unlocked Still Sky cinematics."),
        LEGACY_TALES("LEGACY TALES", "Visit the original adventures and episodes."),
        DIFFICULTY("DIFFICULTY", "Change the challenge level for story battles."),
        NEW_STORY("NEW STORY", "Restart Still Sky mission progress from Act One.");

        private final String title;
        private final String description;

        AdventureMode(String title, String description) {
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
