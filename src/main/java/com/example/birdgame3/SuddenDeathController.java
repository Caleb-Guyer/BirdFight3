package com.example.birdgame3;

import java.util.List;
import java.util.Random;

/**
 * Owns sudden-death state and crow wave spawning.
 */
final class SuddenDeathController {
    private static final int FPS = 60;

    private static final int BASE_SPAWN_INTERVAL = 90;
    private static final int MIN_SPAWN_INTERVAL = 8;
    private static final double SPAWN_INTERVAL_DECAY_PER_SEC = 1.6;
    private static final int MAX_CROWS_PER_SIDE = 10;
    private static final double CROW_RAMP_DELAY_SECONDS = 8.0;
    private static final double CROW_COUNT_STEP_SECONDS = 20.0;
    private static final double NULL_ROCK_MINION_START_SECONDS = 24.0;
    private static final double VOID_RAVEN_START_SECONDS = 52.0;
    private static final double NULL_ROCK_COUNT_STEP_SECONDS = 18.0;
    private static final int MAX_NULL_ROCK_MINIONS_PER_SIDE = 3;
    private static final int BASE_CROW_LIFE = 2;
    private static final int MAX_CROW_LIFE_BONUS = 2;
    private static final double CROW_LIFE_STEP_SECONDS = 30.0;
    private static final double BASE_CROW_SPEED = 3.8;
    private static final double CROW_SPEED_GROWTH_PER_SEC = 0.14;
    private static final double CROW_SPEED_GROWTH_CAP = 8.0;
    private static final double SUDDEN_DEATH_SPEED_MULTIPLIER_GROWTH = 0.016;
    private static final double SUDDEN_DEATH_SPEED_MULTIPLIER_CAP = 0.95;
    private static final int OVERFLOW_PROTECTION_FRAMES = FPS * 12;

    private boolean active = false;
    private int frames = 0;

    boolean isActive() {
        return active;
    }

    void start() {
        active = true;
        frames = 0;
    }

    void reset() {
        active = false;
        frames = 0;
    }

    double updateAndSpawn(List<CrowMinion> crowMinions,
                          List<PiranhaHazard> piranhas,
                          Random random,
                          double shakeIntensity,
                          boolean dockMap,
                          double waterX,
                          double waterY,
                          double waterW,
                          double drownY,
                          boolean matchEnded) {
        if (!active || matchEnded) return shakeIntensity;

        frames++;
        int sdFrames = frames;
        double sdSeconds = sdFrames / (double) FPS;

        int baseInterval = BASE_SPAWN_INTERVAL + (sdSeconds < 12.0 ? 24 : 0);
        int spawnInterval = Math.max(
                MIN_SPAWN_INTERVAL,
                (int) Math.round(baseInterval - sdSeconds * SPAWN_INTERVAL_DECAY_PER_SEC)
        );
        double rampSeconds = Math.max(0.0, sdSeconds - CROW_RAMP_DELAY_SECONDS);
        int crowsPerSide = Math.min(
                MAX_CROWS_PER_SIDE,
                1 + (int) (rampSeconds / CROW_COUNT_STEP_SECONDS)
        );
        int nullRockMinionsPerSide = sdSeconds < NULL_ROCK_MINION_START_SECONDS
                ? 0
                : Math.min(
                MAX_NULL_ROCK_MINIONS_PER_SIDE,
                1 + (int) ((sdSeconds - NULL_ROCK_MINION_START_SECONDS) / NULL_ROCK_COUNT_STEP_SECONDS)
        );
        int murderCrowLife = BASE_CROW_LIFE + Math.min(
                MAX_CROW_LIFE_BONUS,
                (int) (sdSeconds / CROW_LIFE_STEP_SECONDS)
        );
        double speedMultiplier = 1.0 + Math.min(
                SUDDEN_DEATH_SPEED_MULTIPLIER_CAP,
                sdSeconds * SUDDEN_DEATH_SPEED_MULTIPLIER_GROWTH
        );

        if (sdFrames % spawnInterval != 0) {
            return shakeIntensity;
        }

        for (int i = 0; i < crowsPerSide; i++) {
            double y = suddenDeathCrowSpawnY(random, dockMap, waterY);
            double speed = (BASE_CROW_SPEED + Math.min(CROW_SPEED_GROWTH_CAP, sdSeconds * CROW_SPEED_GROWTH_PER_SEC)) * speedMultiplier;
            crowMinions.add(buildSuddenDeathCrow(-100, y, speed, random, CrowMinion.VARIANT_MURDER_CROW, murderCrowLife, speedMultiplier));
            crowMinions.add(buildSuddenDeathCrow(BirdGame3.WORLD_WIDTH + 100, y, -speed, random, CrowMinion.VARIANT_MURDER_CROW, murderCrowLife, speedMultiplier));
        }

        for (int i = 0; i < nullRockMinionsPerSide; i++) {
            int variant = pickNullRockVariant(sdSeconds, i, random);
            double y = suddenDeathCrowSpawnY(random, dockMap, waterY);
            double variantSpeed = (BASE_CROW_SPEED + 0.8 + Math.min(CROW_SPEED_GROWTH_CAP + 1.6, sdSeconds * (CROW_SPEED_GROWTH_PER_SEC + 0.03)))
                    * (speedMultiplier + 0.08);
            CrowMinion left = buildSuddenDeathCrow(-140, y, variantSpeed, random, variant, 0, speedMultiplier + 0.08);
            CrowMinion right = buildSuddenDeathCrow(BirdGame3.WORLD_WIDTH + 140, y, -variantSpeed, random, variant, 0, speedMultiplier + 0.08);
            crowMinions.add(left);
            crowMinions.add(right);
        }

        if (dockMap && piranhas != null && waterW > 0) {
            int fishPerSide = Math.min(3, 1 + (int) (rampSeconds / 24.0));
            double minY = waterY + 44;
            double maxY = Math.max(minY + 20, drownY - 80);
            for (int i = 0; i < fishPerSide; i++) {
                double y = minY + random.nextDouble() * Math.max(1.0, maxY - minY);
                double speed = 4.0 + Math.min(3.6, sdSeconds * 0.08) + random.nextDouble() * 1.4;
                piranhas.add(new PiranhaHazard(waterX + waterW - 12, y, -speed));
            }
        }

        return Math.max(shakeIntensity, 15);
    }

    private double suddenDeathCrowSpawnY(Random random, boolean dockMap, double waterY) {
        double crowMaxY = BirdGame3.WORLD_HEIGHT - 800;
        if (dockMap && waterY > 260) {
            crowMaxY = Math.min(crowMaxY, waterY - 140);
        }
        return 200 + random.nextDouble() * Math.max(80.0, crowMaxY - 200);
    }

    private CrowMinion buildSuddenDeathCrow(double x,
                                            double y,
                                            double vx,
                                            Random random,
                                            int variant,
                                            int forcedLife,
                                            double speedMultiplier) {
        CrowMinion crow = new CrowMinion(x, y, null)
                .withVariant(variant)
                .withSpeedMultiplier(speedMultiplier)
                .withOverflowProtectionFrames(OVERFLOW_PROTECTION_FRAMES);
        if (forcedLife > 0) {
            crow.life = Math.max(crow.life, forcedLife);
        }
        crow.vx = vx + Math.copySign(random.nextDouble() * 1.8, vx);
        crow.vy = (random.nextDouble() - 0.5) * 4.2;
        return crow;
    }

    private int pickNullRockVariant(double suddenDeathSeconds, int index, Random random) {
        if (suddenDeathSeconds >= VOID_RAVEN_START_SECONDS && index == 0) {
            return CrowMinion.VARIANT_VOID_RAVEN;
        }
        return (index + random.nextInt(2)) % 2 == 0
                ? CrowMinion.VARIANT_GIANT_CROW
                : CrowMinion.VARIANT_RAVEN;
    }
}
