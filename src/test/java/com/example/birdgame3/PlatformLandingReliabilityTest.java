package com.example.birdgame3;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.UUID;
import java.util.prefs.Preferences;

import static org.junit.jupiter.api.Assertions.*;

class PlatformLandingReliabilityTest {
    private static final BirdGame3.BirdType[] FIGHTERS = {
            BirdGame3.BirdType.TITMOUSE, BirdGame3.BirdType.PELICAN, BirdGame3.BirdType.RAVEN};

    @Test
    void fastDescentCannotSkipAThinPlatform() throws Exception {
        for (BirdGame3.BirdType type : FIGHTERS) try (Fixture f = new Fixture(type)) {
            Platform platform = new Platform(2400, 1800, 1200, 12);
            f.game.platforms.add(platform);
            double previousY = platform.y - f.bird.bodyHeight() - 8;
            f.bird.y = platform.y + 16;
            f.bird.vy = f.bird.y - previousY;
            f.resolve(previousY);
            assertEquals(platform.y, f.bird.bodyBottomY(), 0.001);
            assertEquals(0, f.bird.vy, 0.001);
        }
    }

    @Test
    void descendingFromBelowCannotSnapUpOntoPlatform() throws Exception {
        for (BirdGame3.BirdType type : FIGHTERS) try (Fixture f = new Fixture(type)) {
            Platform platform = new Platform(2400, 1800, 1200, 12);
            f.game.platforms.add(platform);
            double previousY = platform.y - f.bird.bodyHeight() / 2;
            f.bird.y = previousY + 2;
            f.bird.vy = 2;
            f.resolve(previousY);
            assertEquals(previousY + 2, f.bird.y, 0.001);
            assertEquals(2, f.bird.vy, 0.001);
        }
    }

    @Test
    void firstCrossedPlatformWinsRegardlessOfListOrder() throws Exception {
        for (BirdGame3.BirdType type : FIGHTERS) try (Fixture f = new Fixture(type)) {
            Platform upper = new Platform(2400, 1800, 1200, 12);
            Platform lower = new Platform(2400, 1820, 1200, 12);
            f.game.platforms.add(lower);
            f.game.platforms.add(upper);
            double previousY = upper.y - f.bird.bodyHeight() - 4;
            f.bird.y = lower.y - f.bird.bodyHeight() + 10;
            f.bird.vy = f.bird.y - previousY;
            f.resolve(previousY);
            assertEquals(upper.y, f.bird.bodyBottomY(), 0.001);
        }
    }

    @Test
    void driftingInAfterFeetPassTheTopDoesNotSnapOntoPlatform() throws Exception {
        try (Fixture f = new Fixture(BirdGame3.BirdType.PELICAN)) {
            Platform platform = new Platform(2800, 1800, 400, 12);
            f.game.platforms.add(platform);
            double previousX = platform.x - f.bird.bodyWidth() / 2 - 12;
            double previousY = platform.y - f.bird.bodyHeight() - 2;
            f.bird.x = previousX + 24;
            f.bird.y = previousY + 8;
            f.bird.vy = 8;
            f.resolve(previousX, previousY);
            assertEquals(previousY + 8, f.bird.y, 0.001);
            assertEquals(8, f.bird.vy, 0.001);
            assertFalse(f.bird.isOnGround(), "Passing beneath a platform lip must not count as ground contact.");
        }
    }

    @Test
    void passingBelowPlatformTopDoesNotRefreshRecovery() throws Exception {
        String[] recoveryFlags = {"titmouseVaultUsed", "pelicanUpSpecialUsed", "ravenLiftUsed"};
        for (int index = 0; index < FIGHTERS.length; index++) try (Fixture f = new Fixture(FIGHTERS[index])) {
            Platform platform = new Platform(2400, 1800, 1200, 12);
            f.game.platforms.add(platform);
            f.bird.y = platform.y - f.bird.bodyHeight() + 6;
            f.bird.vy = 2;
            Field recoveryField = Bird.class.getDeclaredField(recoveryFlags[index]);
            recoveryField.setAccessible(true);
            recoveryField.setBoolean(f.bird, true);
            f.bird.update(1.0);
            assertTrue(recoveryField.getBoolean(f.bird), "An underside overlap must not refresh " + FIGHTERS[index]);
            assertTrue(f.bird.bodyBottomY() > platform.y + 6);
        }
    }

    @Test
    void driftingInBeforeFeetReachTheTopCanLand() throws Exception {
        try (Fixture f = new Fixture(BirdGame3.BirdType.PELICAN)) {
            Platform platform = new Platform(2800, 1800, 400, 12);
            f.game.platforms.add(platform);
            double previousX = platform.x - f.bird.bodyWidth() / 2 - 12;
            double previousY = platform.y - f.bird.bodyHeight() - 6;
            f.bird.x = previousX + 24;
            f.bird.y = previousY + 8;
            f.bird.vy = 8;
            f.resolve(previousX, previousY);
            assertEquals(platform.y, f.bird.bodyBottomY(), 0.001);
        }
    }

    @Test
    void walkingOffTheEdgeDoesNotKeepFighterGrounded() throws Exception {
        try (Fixture f = new Fixture(BirdGame3.BirdType.TITMOUSE)) {
            Platform platform = new Platform(2400, 1800, 400, 12);
            f.game.platforms.add(platform);
            double previousX = platform.x + platform.w - f.bird.bodyWidth() / 2 - 2;
            double previousY = platform.y - f.bird.bodyHeight();
            f.bird.x = previousX + 6;
            f.bird.y = previousY + 1;
            f.bird.vy = 1;
            f.resolve(previousX, previousY);
            assertEquals(previousY + 1, f.bird.y, 0.001);
            assertFalse(f.bird.isOnGround());
        }
    }

    @Test
    void upwardMovementStillPassesThroughPlatform() throws Exception {
        try (Fixture f = new Fixture(BirdGame3.BirdType.RAVEN)) {
            Platform platform = new Platform(2400, 1800, 1200, 12);
            f.game.platforms.add(platform);
            double previousY = platform.y - f.bird.bodyHeight() + 6;
            f.bird.y = previousY - 12;
            f.bird.vy = -12;
            f.resolve(previousY);
            assertEquals(previousY - 12, f.bird.y, 0.001);
            assertEquals(-12, f.bird.vy, 0.001);
        }
    }

    @Test
    void normalUpdateLandsAndKeepsFighterSupported() throws Exception {
        for (BirdGame3.BirdType type : FIGHTERS) try (Fixture f = new Fixture(type)) {
            Platform platform = new Platform(2400, 1800, 1200, 12);
            f.game.platforms.add(platform);
            f.bird.y = platform.y - f.bird.bodyHeight() - 4;
            f.bird.vy = 8;
            f.bird.canDoubleJump = false;
            for (int tick = 0; tick < 5; tick++) {
                f.bird.update(1.0);
                assertEquals(platform.y, f.bird.bodyBottomY(), 0.001, type.name());
                assertTrue(f.bird.canDoubleJump);
            }
        }
    }

    @Test
    void respawnNestAboveAnotherPlatformCatchesTheLandingFirst() throws Exception {
        try (Fixture f = new Fixture(BirdGame3.BirdType.RAVEN)) {
            Platform nest = new Platform(2400, 1800, 1200, 12);
            Platform lower = new Platform(2400, 1820, 1200, 12);
            f.game.platforms.add(lower);
            Field nestField = Bird.class.getDeclaredField("respawnNestPlatform");
            nestField.setAccessible(true);
            nestField.set(f.bird, nest);
            Field timerField = Bird.class.getDeclaredField("respawnInvulnerabilityTimer");
            timerField.setAccessible(true);
            timerField.setInt(f.bird, 60);
            double previousY = nest.y - f.bird.bodyHeight() - 4;
            f.bird.y = lower.y - f.bird.bodyHeight() + 10;
            f.bird.vy = f.bird.y - previousY;
            f.resolve(previousY);
            assertEquals(nest.y, f.bird.bodyBottomY(), 0.001);
        }
    }

    @Test
    void titanPickupCannotGrowFeetThroughThePlatform() throws Exception {
        for (BirdGame3.BirdType type : FIGHTERS) try (Fixture f = new Fixture(type)) {
            Platform platform = new Platform(2400, 1800, 1200, 12);
            f.game.platforms.add(platform);
            f.bird.y = platform.y - f.bird.bodyHeight();
            f.game.powerUps.add(new PowerUp(f.bird.bodyCenterX(), f.bird.bodyCenterY(), PowerUpType.TITAN));
            f.bird.update(1.0);
            assertEquals(f.bird.baseSizeMultiplier * 1.35, f.bird.sizeMultiplier, 0.001);
            for (int tick = 0; tick < 5; tick++) {
                assertEquals(platform.y, f.bird.bodyBottomY(), 0.001, type.name());
                f.bird.update(1.0);
            }
        }
    }

    @Test
    void shrinkExpiryCannotGrowFeetThroughThePlatform() throws Exception {
        for (BirdGame3.BirdType type : FIGHTERS) try (Fixture f = new Fixture(type)) {
            Platform platform = new Platform(2400, 1800, 1200, 12);
            f.game.platforms.add(platform);
            f.bird.applyShrinkEffect();
            f.bird.y = platform.y - f.bird.bodyHeight();
            f.bird.shrinkTimer = 1;
            f.bird.update(1.0);
            assertEquals(f.bird.baseSizeMultiplier, f.bird.sizeMultiplier, 0.001);
            assertEquals(platform.y, f.bird.bodyBottomY(), 0.001, type.name());
        }
    }

    @Test
    void baseSizeChangesKeepTheSameSupportPoint() throws Exception {
        try (Fixture f = new Fixture(BirdGame3.BirdType.TITMOUSE)) {
            Platform platform = new Platform(2400, 1800, 1200, 12);
            f.game.platforms.add(platform);
            f.bird.y = platform.y - f.bird.bodyHeight();
            double centerX = f.bird.bodyCenterX();
            f.bird.setBaseMultipliers(1.5, 1.0, 1.0);
            assertEquals(centerX, f.bird.bodyCenterX(), 0.001);
            assertEquals(platform.y, f.bird.bodyBottomY(), 0.001);
            f.bird.update(1.0);
            assertEquals(platform.y, f.bird.bodyBottomY(), 0.001);
        }
    }

    private static final class Fixture implements AutoCloseable {
        final Preferences prefs = Preferences.userRoot().node("/birdfight3-tests/platform-landing/" + UUID.randomUUID());
        final BirdGame3 game = new BirdGame3(prefs);
        final Bird bird;
        Fixture(BirdGame3.BirdType type) {
            game.selectedMap = BirdGame3.MapType.BATTLEFIELD;
            game.activePlayers = 1;
            game.platforms.clear();
            bird = new Bird(2800, type, 0, game);
            game.players[0] = bird;
        }
        void resolve(double previousY) throws Exception {
            resolve(bird.x, previousY);
        }
        void resolve(double previousX, double previousY) throws Exception {
            Method method = Bird.class.getDeclaredMethod("handleBoundaries", double.class, boolean.class,
                    double.class, double.class);
            method.setAccessible(true);
            method.invoke(bird, 1.0, true, previousX, previousY);
        }
        @Override public void close() throws Exception { prefs.removeNode(); }
    }
}
