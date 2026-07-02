package com.example.birdgame3;

class PiranhaHazard {
    double x;
    double y;
    double vx;
    double vy;
    double prevX, prevY;
    double renderSavedX, renderSavedY;
    int age = 0;
    int biteCooldown = 0;
    int breachCooldown = 0;
    int retargetCooldown = 0;
    Bird target = null;

    PiranhaHazard(double x, double y, double vx) {
        this.x = x;
        this.y = y;
        this.prevX = x;
        this.prevY = y;
        this.vx = vx;
        this.vy = 0;
    }
}
