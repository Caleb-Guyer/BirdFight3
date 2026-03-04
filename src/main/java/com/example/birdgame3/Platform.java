package com.example.birdgame3;

class Platform {
    double x, y, w, h;
    String signText = null; // null = no sign

    Platform(double x, double y, double w, double h) {
        this.x = x;
        this.y = y;
        this.w = w;
        this.h = h;
    }
}
