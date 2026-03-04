package com.example.birdgame3;

class NectarNode {
    double x, y;
    boolean isSpeed; // true = speed boost, false = hover regen
    boolean active;

    NectarNode(double x, double y, boolean isSpeed) {
        this.x = x;
        this.y = y;
        this.isSpeed = isSpeed;
        this.active = true;
    }
}
