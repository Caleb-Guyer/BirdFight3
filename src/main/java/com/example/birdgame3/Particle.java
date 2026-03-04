package com.example.birdgame3;

import javafx.scene.paint.Color;

class Particle {
    double x, y, vx, vy;
    Color color;
    int life = 60; // frames

    Particle(double x, double y, double vx, double vy, Color color) {
        this.x = x;
        this.y = y;
        this.vx = vx;
        this.vy = vy;
        this.color = color;
    }
}
