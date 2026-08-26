package com.example.birdgame3;

import com.example.birdgame3.BirdGame3.BirdType;
import com.example.birdgame3.BirdGame3.MapType;

import java.util.Objects;

/** Display-only receipt. Building or reopening a reveal must never grant a reward. */
record RewardPresentation(Kind kind, String name, String detail,
                          BirdType bird, String skinKey, MapType map) {
    enum Kind {
        BIRD("NEW FIGHTER", "TAKES FLIGHT!", "#147BE2"),
        SKIN("NEW LOOK!", "SKIN UNLOCKED", "#E6A820"),
        STAGE("NEW HORIZONS!", "STAGE UNLOCKED", "#0A9685"),
        COINS("REWARD CLAIMED!", "BIRD COINS", "#E6A820"),
        CONTINUE("REWARD CLAIMED!", "CLASSIC CONTINUE", "#7851BC");

        final String headline;
        final String category;
        final String accent;

        Kind(String headline, String category, String accent) {
            this.headline = headline;
            this.category = category;
            this.accent = accent;
        }
    }

    RewardPresentation {
        Objects.requireNonNull(kind);
        name = Objects.requireNonNull(name).strip();
        detail = detail == null ? "" : detail.strip();
        if (name.isEmpty()) throw new IllegalArgumentException("Reward name is required");
        if ((kind == Kind.BIRD || kind == Kind.SKIN) && bird == null) {
            throw new IllegalArgumentException("Bird artwork is required");
        }
        if (kind == Kind.STAGE && map == null) throw new IllegalArgumentException("Stage artwork is required");
    }
}
