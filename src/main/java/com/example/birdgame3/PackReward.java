package com.example.birdgame3;

import java.util.function.BooleanSupplier;

class PackReward {
    final String label;
    final int weight;
    final BooleanSupplier available;
    final Runnable grant;

    PackReward(String label, int weight, BooleanSupplier available, Runnable grant) {
        this.label = label;
        this.weight = weight;
        this.available = available;
        this.grant = grant;
    }
}
