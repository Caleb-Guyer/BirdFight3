package com.example.birdgame3;

class MockingbirdShadowMinion {
    static final int MAX_AGE = 390;
    static final double BASE_HEALTH = 10.0;

    double x, y, vx, vy;
    double prevX, prevY;
    double renderSavedX, renderSavedY;
    Bird owner;
    Bird target;
    BirdGame3.BirdType copiedType;
    double health = BASE_HEALTH;
    double maxHealth = BASE_HEALTH;
    int age = 0;
    int attackCooldown = 18;
    int jumpCooldown = 0;
    int retargetCooldown = 0;
    int hitFlashTimer = 0;
    int spawnFlashFrames = 34;
    boolean onGround = false;
    boolean facingRight = true;
    final int slot;

    MockingbirdShadowMinion(double centerX, double centerY, BirdGame3.BirdType copiedType, Bird owner, int slot) {
        this.copiedType = copiedType == null || copiedType == BirdGame3.BirdType.MOCKINGBIRD
                ? BirdGame3.BirdType.PIGEON
                : copiedType;
        this.owner = owner;
        this.slot = Math.max(0, slot);
        double w = bodyWidth();
        double h = bodyHeight();
        this.x = centerX - w * 0.5;
        this.y = centerY - h * 0.62;
        this.prevX = x;
        this.prevY = y;
        this.facingRight = slot != 0;
        this.vx = (slot - 1) * 2.2;
        this.vy = -5.2 - slot * 0.35;
        this.maxHealth = Math.max(8.0, BASE_HEALTH + this.copiedType.power * 0.24);
        this.health = maxHealth;
    }

    double sizeMultiplier() {
        double base = copiedType == BirdGame3.BirdType.PELICAN ? 1.03
                : copiedType == BirdGame3.BirdType.GOOSE ? 1.0
                : copiedType == BirdGame3.BirdType.HUMMINGBIRD || copiedType == BirdGame3.BirdType.TITMOUSE ? 0.78
                : 0.88;
        return Math.max(0.68, base);
    }

    double bodyWidth() {
        return 80.0 * sizeMultiplier();
    }

    double bodyHeight() {
        return 80.0 * sizeMultiplier();
    }

    double bodyCenterX() {
        return x + bodyWidth() * 0.5;
    }

    double bodyCenterY() {
        return y + bodyHeight() * 0.5;
    }

    double combatHalfWidth() {
        return bodyWidth() * 0.34;
    }

    double combatHalfHeight() {
        return bodyHeight() * 0.34;
    }

    double speed() {
        return Math.clamp(copiedType.speed * 1.38, 4.4, 7.4);
    }

    double contactDamage() {
        return Math.clamp(2.4 + copiedType.power * 0.18, 3.2, 5.0);
    }

    void registerHit(double damage, double knockbackX, double knockbackY) {
        health -= Math.max(1.0, damage);
        vx += knockbackX;
        vy += knockbackY;
        onGround = false;
        hitFlashTimer = Math.max(hitFlashTimer, 10);
    }

    boolean alive() {
        return health > 0.0 && age <= MAX_AGE;
    }
}
