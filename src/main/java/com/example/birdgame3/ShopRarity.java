package com.example.birdgame3;

import javafx.scene.paint.Color;

enum ShopRarity {
    COMMON("COMMON", Color.web("#B0BEC5")),
    UNCOMMON("UNCOMMON", Color.web("#81C784")),
    RARE("RARE", Color.web("#64B5F6")),
    EPIC("EPIC", Color.web("#BA68C8")),
    LEGENDARY("LEGENDARY", Color.web("#FFB300")),
    BUNDLE("BUNDLE", Color.web("#26A69A"));

    final String label;
    final Color color;

    ShopRarity(String label, Color color) {
        this.label = label;
        this.color = color;
    }
}
