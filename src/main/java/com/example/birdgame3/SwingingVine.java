package com.example.birdgame3;

class SwingingVine {
    double baseX, baseY; // Anchor point at top
    double length; // Vine length
    double angle; // Current angle from vertical (radians)
    double platformX, platformY; // Platform at vine's end
    double platformW = 120, platformH = 40;
    double angularVelocity; // For swinging motion

    SwingingVine(double baseX, double baseY, double length) {
        this.baseX = baseX;
        this.baseY = baseY;
        this.length = length;
        this.angle = 0;
        this.angularVelocity = 0;
        updatePlatformPosition();
    }

    void updatePlatformPosition() {
        platformX = baseX + length * Math.sin(angle);
        platformY = baseY + length * Math.cos(angle);
    }
}
