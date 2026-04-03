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
    private static final int BASE_CROW_LIFE = 2;
    private static final int MAX_CROW_LIFE_BONUS = 2;
    private static final double CROW_LIFE_STEP_SECONDS = 30.0;
    private static final double BASE_CROW_SPEED = 3.8;
    private static final double CROW_SPEED_GROWTH_PER_SEC = 0.14;
    private static final double CROW_SPEED_GROWTH_CAP = 8.0;

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
        int murderCrowLife = BASE_CROW_LIFE + Math.min(
                MAX_CROW_LIFE_BONUS,
                (int) (sdSeconds / CROW_LIFE_STEP_SECONDS)
        );

        if (sdFrames % spawnInterval != 0) {
            return shakeIntensity;
        }

        for (int i = 0; i < crowsPerSide; i++) {
            double crowMaxY = BirdGame3.WORLD_HEIGHT - 800;
            if (dockMap && waterY > 260) {
                crowMaxY = Math.min(crowMaxY, waterY - 140);
            }
            double y = 200 + random.nextDouble() * Math.max(80.0, crowMaxY - 200);
            double speed = BASE_CROW_SPEED + Math.min(CROW_SPEED_GROWTH_CAP, sdSeconds * CROW_SPEED_GROWTH_PER_SEC);

            CrowMinion left = new CrowMinion(-100, y, null);
            left.life = murderCrowLife;
            left.vx = speed + random.nextDouble() * 2;
            left.vy = (random.nextDouble() - 0.5) * 4;
            crowMinions.add(left);

            CrowMinion right = new CrowMinion(BirdGame3.WORLD_WIDTH + 100, y, null);
            right.life = murderCrowLife;
            right.vx = -speed - random.nextDouble() * 2;
            right.vy = (random.nextDouble() - 0.5) * 4;
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
}
