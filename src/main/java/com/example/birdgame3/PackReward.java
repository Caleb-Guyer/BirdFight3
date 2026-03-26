package com.example.birdgame3;

import java.util.function.BooleanSupplier;

record PackReward(String label, int weight, BooleanSupplier available, Runnable grant) {
}
