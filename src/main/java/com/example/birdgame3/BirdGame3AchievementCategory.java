package com.example.birdgame3;

enum BirdGame3AchievementCategory {
    COMBAT("Combat"),
    BIRD("Bird"),
    MAP("Maps"),
    MODE("Modes"),
    STORY("Story");

    final String label;

    BirdGame3AchievementCategory(String label) {
        this.label = label;
    }
}
