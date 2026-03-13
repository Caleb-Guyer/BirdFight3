package com.example.birdgame3;

import com.example.birdgame3.BirdGame3.BirdType;

class BirdIconSpot {
    final BirdType type;
    final boolean random;
    final double cx;
    final double cy;

    BirdIconSpot(BirdType type, boolean random, double cx, double cy) {
        this.type = type;
        this.random = random;
        this.cx = cx;
        this.cy = cy;
    }
}
