package com.example.birdgame3;

import java.util.List;

class ShopPackResult {
    final String title;
    final List<String> lines;

    ShopPackResult(String title, List<String> lines) {
        this.title = title;
        this.lines = lines;
    }

    String message() {
        return String.join("\n", lines);
    }
}
