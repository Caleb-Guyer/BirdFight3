package com.example.birdgame3;

import java.util.List;

record ShopPackResult(String title, List<String> lines) {

    String message() {
        return String.join("\n", lines);
    }
}
