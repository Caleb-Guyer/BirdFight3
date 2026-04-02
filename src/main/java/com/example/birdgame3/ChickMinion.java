package com.example.birdgame3;

class ChickMinion {
    double x, y, vx, vy;
    int age = 0;
    Bird owner;
    Bird target = null;
    int variant;
    boolean ultimate;
    int life = 3;
    int maxLife = 3;
    int attackCooldown = 0;
    int jumpCooldown = 0;
    boolean onGround = false;
    double width = 34;
    double height = 26;
    double speed = 5.0;
    double accel = 0.22;
    double jumpStrength = 10.0;
    int damage = 2;
    int maxAge = 2100;
    int retargetCooldown = 0;

    ChickMinion(double x, double y, int variant, boolean ultimate, Bird owner) {
        this.x = x;
        this.y = y;
        this.variant = variant;
        this.ultimate = ultimate;
        this.owner = owner;
        this.vx = (Math.random() - 0.5) * 3.0;
        this.vy = -2 - Math.random() * 2.0;
        applyVariantStats();
    }

    private void applyVariantStats() {
        switch (variant) {
            case 1 -> {
                width = 32;
                height = 24;
                speed = 5.0;
                accel = 0.26;
                jumpStrength = 24.0;
                damage = 2;
                maxLife = 3;
                maxAge = 2200;
            }
            case 2 -> {
                width = 42;
                height = 31;
                speed = 4.2;
                accel = 0.22;
                jumpStrength = 10.4;
                damage = 4;
                maxLife = 4;
                maxAge = 2400;
            }
            default -> {
                width = 34;
                height = 26;
                speed = 6.5;
                accel = 0.30;
                jumpStrength = 13.5;
                damage = 2;
                maxLife = 3;
                maxAge = 2100;
            }
        }

        if (ultimate) {
            width *= 1.12;
            height *= 1.12;
            speed *= 1.2;
            accel *= 1.15;
            jumpStrength *= 1.1;
            damage += 1;
            maxLife += 2;
            maxAge += 350;
        }
        life = maxLife;
    }
}
