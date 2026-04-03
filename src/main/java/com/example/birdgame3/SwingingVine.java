package com.example.birdgame3;

class SwingingVine {
    double baseX, baseY; // Anchor point at top
    double length; // Vine length
    double angle; // Current angle from vertical (radians)
    double platformX, platformY; // Platform at vine's end
    double platformW = 72, platformH = 26;
    double angularVelocity; // For swinging motion
    boolean temporary = false;
    boolean detaching = false;
    int ownerPlayerIndex = -1;
    double detachedVX = 0.0;
    double detachedVY = 0.0;

    SwingingVine(double baseX, double baseY, double length) {
        this.baseX = baseX;
        this.baseY = baseY;
        this.length = length;
        this.angle = 0;
        this.angularVelocity = 0;
        updatePlatformPosition();
    }

    double gripX() {
        return baseX + length * Math.sin(angle);
    }

    double gripY() {
        return baseY + length * Math.cos(angle);
    }

    double tipVelocityX() {
        return length * Math.cos(angle) * angularVelocity;
    }

    double tipVelocityY() {
        return -length * Math.sin(angle) * angularVelocity;
    }

    double tipSpeed() {
        return Math.abs(length * angularVelocity);
    }

    void startDetaching() {
        if (detaching) {
            return;
        }
        detaching = true;
        detachedVX = tipVelocityX() * 0.26;
        detachedVY = tipVelocityY() * 0.18 - 1.8;
        angularVelocity += (Math.abs(angle) < 0.08 ? 0.018 : Math.signum(angle) * 0.014);
    }

    void updateDetaching() {
        baseX += detachedVX;
        baseY += detachedVY;
        detachedVY += 0.55;
        detachedVX *= 0.988;
        angularVelocity *= 0.994;
        angle += angularVelocity;
        updatePlatformPosition();
    }

    boolean isFullyDetachedOffscreen() {
        double lowestY = Math.max(baseY, gripY());
        return lowestY > BirdGame3.WORLD_HEIGHT + 220
                || Math.max(baseX, gripX()) < -420
                || Math.min(baseX, gripX()) > BirdGame3.WORLD_WIDTH + 420;
    }

    void updatePlatformPosition() {
        platformX = gripX() - platformW / 2.0;
        platformY = gripY() - platformH / 2.0;
    }
}
