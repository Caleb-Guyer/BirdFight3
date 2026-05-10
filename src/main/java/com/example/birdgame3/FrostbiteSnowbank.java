package com.example.birdgame3;

class FrostbiteSnowbank {
    final double x;
    final double y;
    final double w;
    final double h;
    final int maxHealth;
    int health;
    int damageFlash;

    FrostbiteSnowbank(double x, double y, double w, double h, int health) {
        this.x = x;
        this.y = y;
        this.w = w;
        this.h = h;
        this.maxHealth = health;
        this.health = health;
    }

    double centerX() {
        return x + w * 0.5;
    }

    double centerY() {
        return y + h * 0.5;
    }
}
