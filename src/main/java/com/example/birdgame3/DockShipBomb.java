package com.example.birdgame3;

class DockShipBomb {
    double x;
    double y;
    double vx;
    double vy;
    double targetX;
    double targetY;
    int ownerPlayerIndex;
    int targetPlayerIndex;
    int fuse;
    int launchDelayFrames = 0;
    int cannonFlashFrames = 0;
    int explosionFrames = 0;
    boolean fired = false;
    boolean exploded = false;

    DockShipBomb(double x, double y, double vx, double vy,
                 double targetX, double targetY,
                 int ownerPlayerIndex, int targetPlayerIndex, int fuse) {
        this.x = x;
        this.y = y;
        this.vx = vx;
        this.vy = vy;
        this.targetX = targetX;
        this.targetY = targetY;
        this.ownerPlayerIndex = ownerPlayerIndex;
        this.targetPlayerIndex = targetPlayerIndex;
        this.fuse = fuse;
    }
}
