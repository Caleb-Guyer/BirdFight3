package com.example.birdgame3;

import com.example.birdgame3.BirdGame3.BirdType;

class ShopPreview {
    final BirdType type;
    final String skinKey;
    final String label;
    final int value;

    ShopPreview(BirdType type, String skinKey, String label) {
        this(type, skinKey, label, 0);
    }

    ShopPreview(BirdType type, String skinKey, String label, int value) {
        this.type = type;
        this.skinKey = skinKey;
        this.label = label;
        this.value = value;
    }
}
