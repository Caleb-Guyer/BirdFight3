package com.example.birdgame3;

class CrowMinion {
    double x, y, vx, vy;
    int life = 1;
    Bird target;
    int age = 0;
    Bird owner = null;
    boolean hasCrown = false;

    CrowMinion(double x, double y, Bird target) {
        this.x = x;
        this.y = y;
        this.target = target;
        this.vx = (Math.random() - 0.5) * 4;  // start with random drift
        this.vy = (Math.random() - 0.5) * 4;
        // they will pick a real target on the first update frame
    }
}
