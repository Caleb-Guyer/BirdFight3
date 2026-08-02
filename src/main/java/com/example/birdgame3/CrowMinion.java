package com.example.birdgame3;

class CrowMinion {
    static final int VARIANT_AUTO = -1;
    static final int VARIANT_ALLIED_CROW = 0;
    static final int VARIANT_MURDER_CROW = 1;
    static final int VARIANT_GIANT_CROW = 2;
    static final int VARIANT_RAVEN = 3;
    static final int VARIANT_VOID_RAVEN = 4;
    static final int VARIANT_VULTURE_HENCHMAN = 5;

    double x, y, vx, vy;
    double prevX, prevY;
    double renderSavedX, renderSavedY;
    int life = 1;
    Bird target;
    int age = 0;
    Bird owner = null;
    boolean hasCrown = false;
    int variant = VARIANT_AUTO;
    int hitFlashTimer = 0;
    int retargetCooldown = 0;
    int contactCooldown = 0;
    double speedMultiplier = 1.0;
    int overflowProtectionFrames = 0;
    boolean anchorGuard = false;
    double anchorX = 0.0;
    double anchorY = 0.0;
    double anchorRadius = 0.0;
    double anchorOrbitOffset = 0.0;
    int anchorGuardFrames = 0;

    CrowMinion(double x, double y, Bird target) {
        this.x = x;
        this.y = y;
        this.prevX = x;
        this.prevY = y;
        this.target = target;
        this.vx = (SimRng.next() - 0.5) * 4;  // start with random drift
        this.vy = (SimRng.next() - 0.5) * 4;
        // they will pick a real target on the first update frame
    }

    CrowMinion withVariant(int variant) {
        this.variant = variant;
        this.life = Math.max(this.life, defaultLife(variant));
        if (variant == VARIANT_VOID_RAVEN) {
            this.hasCrown = true;
        }
        return this;
    }

    CrowMinion withSpeedMultiplier(double speedMultiplier) {
        this.speedMultiplier = Math.max(1.0, speedMultiplier);
        return this;
    }

    CrowMinion withOverflowProtectionFrames(int overflowProtectionFrames) {
        this.overflowProtectionFrames = Math.max(this.overflowProtectionFrames, overflowProtectionFrames);
        return this;
    }

    CrowMinion withAnchorGuard(double anchorX, double anchorY, double anchorRadius, int anchorGuardFrames) {
        this.anchorGuard = true;
        this.anchorX = anchorX;
        this.anchorY = anchorY;
        this.anchorRadius = Math.max(48.0, anchorRadius);
        this.anchorGuardFrames = Math.max(this.anchorGuardFrames, anchorGuardFrames);
        this.anchorOrbitOffset = SimRng.next() * Math.PI * 2.0;
        this.target = null;
        this.retargetCooldown = 0;
        return this;
    }

    boolean guardsAnchor() {
        return anchorGuard;
    }

    boolean guardsAnchorNear(double x, double y, double tolerance) {
        if (!anchorGuard) return false;
        return Math.hypot(anchorX - x, anchorY - y) <= tolerance;
    }

    int effectiveVariant() {
        if (variant != VARIANT_AUTO) return variant;
        return owner == null ? VARIANT_MURDER_CROW : VARIANT_ALLIED_CROW;
    }

    double drawScale() {
        return switch (effectiveVariant()) {
            case VARIANT_GIANT_CROW -> 2.15;
            case VARIANT_RAVEN -> 1.28;
            case VARIANT_VOID_RAVEN -> 1.52;
            case VARIANT_VULTURE_HENCHMAN -> 1.72;
            case VARIANT_MURDER_CROW -> 1.38;
            default -> 1.0;
        };
    }

    int contactDamage() {
        return switch (effectiveVariant()) {
            case VARIANT_GIANT_CROW -> 6;
            case VARIANT_RAVEN -> 5;
            case VARIANT_VOID_RAVEN -> 7;
            case VARIANT_VULTURE_HENCHMAN -> 6;
            case VARIANT_MURDER_CROW -> 4;
            default -> 1;
        };
    }

    double homingAccel() {
        double base = switch (effectiveVariant()) {
            case VARIANT_GIANT_CROW -> 0.16;
            case VARIANT_RAVEN -> 0.28;
            case VARIANT_VOID_RAVEN -> 0.31;
            case VARIANT_VULTURE_HENCHMAN -> 0.25;
            default -> 0.22;
        };
        return base * (0.94 + speedMultiplier * 0.12);
    }

    double maxSpeed() {
        double base = switch (effectiveVariant()) {
            case VARIANT_GIANT_CROW -> 2.65;
            case VARIANT_RAVEN -> 3.7;
            case VARIANT_VOID_RAVEN -> 4.15;
            case VARIANT_VULTURE_HENCHMAN -> 3.55;
            default -> 3.2;
        };
        return base * speedMultiplier;
    }

    String displayName() {
        return switch (effectiveVariant()) {
            case VARIANT_GIANT_CROW -> "GIANT CROW";
            case VARIANT_RAVEN -> "RAVEN";
            case VARIANT_VOID_RAVEN -> "VOID RAVEN";
            case VARIANT_VULTURE_HENCHMAN -> "VULTURE HENCHMAN";
            case VARIANT_MURDER_CROW -> "MURDER CROW";
            default -> "CROW";
        };
    }

    boolean hasHeavyLifePool() {
        return defaultLife(effectiveVariant()) > 1;
    }

    boolean isOverflowProtected() {
        return overflowProtectionFrames > 0;
    }

    void registerHit(double knockbackX, double knockbackY) {
        vx += knockbackX;
        vy += knockbackY;
        hitFlashTimer = Math.max(hitFlashTimer, 10);
    }

    private static int defaultLife(int variant) {
        return switch (variant) {
            case VARIANT_GIANT_CROW -> 3;
            case VARIANT_RAVEN -> 2;
            case VARIANT_VOID_RAVEN -> 4;
            case VARIANT_VULTURE_HENCHMAN -> 5;
            default -> 1;
        };
    }
}
