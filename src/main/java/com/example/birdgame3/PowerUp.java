package com.example.birdgame3;

class PowerUp {
    double x, y;
    PowerUpType type;
    double floatOffset = 0;

    PowerUp(double x, double y, PowerUpType type) {
        this.x = x;
        this.y = y;
        this.type = type;
    }
}
