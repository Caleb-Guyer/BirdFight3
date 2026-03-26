package com.example.birdgame3;

import com.example.birdgame3.BirdGame3.BirdType;

record ShopPreview(BirdType type, String skinKey, String label, int value) {
    ShopPreview(BirdType type, String skinKey, String label) {
        this(type, skinKey, label, 0);
    }

}
