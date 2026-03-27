package com.example.birdgame3;

class CrowMinion {
    static final int VARIANT_AUTO = -1;
    static final int VARIANT_ALLIED_CROW = 0;
    static final int VARIANT_MURDER_CROW = 1;
    static final int VARIANT_GIANT_CROW = 2;
    static final int VARIANT_RAVEN = 3;
    static final int VARIANT_VOID_RAVEN = 4;

    double x, y, vx, vy;
    int life = 1;
    Bird target;
    int age = 0;
    Bird owner = null;
    boolean hasCrown = false;
    int variant = VARIANT_AUTO;
    int hitFlashTimer = 0;

    CrowMinion(double x, double y, Bird target) {
        this.x = x;
        this.y = y;
        this.target = target;
        this.vx = (Math.random() - 0.5) * 4;  // start with random drift
        this.vy = (Math.random() - 0.5) * 4;
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

    int effectiveVariant() {
        if (variant != VARIANT_AUTO) return variant;
        return owner == null ? VARIANT_MURDER_CROW : VARIANT_ALLIED_CROW;
    }

    double drawScale() {
        return switch (effectiveVariant()) {
            case VARIANT_GIANT_CROW -> 2.15;
            case VARIANT_RAVEN -> 1.28;
            case VARIANT_VOID_RAVEN -> 1.52;
            case VARIANT_MURDER_CROW -> 1.38;
            default -> 1.0;
        };
    }

    int contactDamage() {
        return switch (effectiveVariant()) {
            case VARIANT_GIANT_CROW -> 6;
            case VARIANT_RAVEN -> 5;
            case VARIANT_VOID_RAVEN -> 7;
            case VARIANT_MURDER_CROW -> 4;
            default -> 1;
        };
    }

    double homingAccel() {
        return switch (effectiveVariant()) {
            case VARIANT_GIANT_CROW -> 0.16;
            case VARIANT_RAVEN -> 0.28;
            case VARIANT_VOID_RAVEN -> 0.31;
            default -> 0.22;
        };
    }

    double maxSpeed() {
        return switch (effectiveVariant()) {
            case VARIANT_GIANT_CROW -> 2.65;
            case VARIANT_RAVEN -> 3.7;
            case VARIANT_VOID_RAVEN -> 4.15;
            default -> 3.2;
        };
    }

    String displayName() {
        return switch (effectiveVariant()) {
            case VARIANT_GIANT_CROW -> "GIANT CROW";
            case VARIANT_RAVEN -> "RAVEN";
            case VARIANT_VOID_RAVEN -> "VOID RAVEN";
            case VARIANT_MURDER_CROW -> "MURDER CROW";
            default -> "CROW";
        };
    }

    boolean hasHeavyLifePool() {
        return defaultLife(effectiveVariant()) > 1;
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
            default -> 1;
        };
    }
}
