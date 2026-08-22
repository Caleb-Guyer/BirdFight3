package com.example.birdgame3;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class RavenMovesetIdentityTest {
    @Test
    void ravenHasAnAuthoredCompleteOmenAssassinKit() throws Exception {
        BirdGame3 game = new BirdGame3();
        Bird raven = groundedBird(game, BirdGame3.BirdType.RAVEN, 0, 320.0);
        Bird heisenbird = groundedBird(game, BirdGame3.BirdType.HEISENBIRD, 1, 320.0);
        Bird sharedBird = groundedBird(game, BirdGame3.BirdType.TITMOUSE, 2, 320.0);

        int distinctFromHeisenbird = 0;
        int distinctFromSharedProfile = 0;
        for (Object variant : normalAttackVariantClass().getEnumConstants()) {
            Object profile = invoke(raven, "normalAttackProfile", variant);
            if (!profile.equals(invoke(heisenbird, "normalAttackProfile", variant))) distinctFromHeisenbird++;
            if (!profile.equals(invoke(sharedBird, "normalAttackProfile", variant))) distinctFromSharedProfile++;
            String moveName = (String) invoke(raven, "normalAttackTelemetryName", variant);
            assertTrue(moveName.startsWith("Raven "));
            assertFalse(moveName.contains("Normal Attack"));
            assertFalse(moveName.contains("Tilt"));
        }

        assertEquals(15, distinctFromHeisenbird);
        assertEquals(15, distinctFromSharedProfile);
    }

    @Test
    void ravenPummelUsesAQuickOmenPrick() throws Exception {
        BirdGame3 game = new BirdGame3();
        game.activePlayers = 2;
        Bird raven = groundedBird(game, BirdGame3.BirdType.RAVEN, 0, 320.0);
        Bird target = groundedBird(game, BirdGame3.BirdType.PIGEON, 1, 385.0);
        game.players[0] = raven;
        game.players[1] = target;
        linkGrab(raven, target);
        game.setAiControlKey(0, game.attackKeyForPlayer(0), true);

        double healthBefore = target.health;
        assertTrue((boolean) invoke(raven, "handleHoldingGrabState", false, false));

        double tunedDamage = 2.0 * raven.type.damageDealtMult * target.type.damageTakenMult;
        assertEquals(tunedDamage, healthBefore - target.health, 0.001);
        assertSame(target, getField(raven, "grabbedTarget"));
        assertSame(raven, getField(target, "grabbedBy"));
        assertEquals(8, getInt(raven, "grabThrowLockTimer"));
        assertEquals(53, getInt(raven, "grabHoldTimer"));
    }

    @Test
    void ravenThrowsCoverFourDistinctFates() throws Exception {
        ThrowOutcome forward = performThrow("FORWARD");
        ThrowOutcome back = performThrow("BACK");
        ThrowOutcome up = performThrow("UP");
        ThrowOutcome down = performThrow("DOWN");

        assertTrue(forward.vx >= 18.0);
        assertTrue(back.vx <= -19.5);
        assertTrue(up.vy < back.vy);
        assertTrue(down.damage > up.damage);
        assertEquals("Raven Final Draft", forward.name);
        assertEquals("Raven Fate Reversal", back.name);
        assertEquals("Raven Black-Sun Lift", up.name);
        assertEquals("Raven Omen Pin", down.name);
    }

    @Test
    void ravenGroundedAttackPosesStayFloorSafeAndAngular() throws Exception {
        BirdGame3 game = new BirdGame3();
        Bird raven = groundedBird(game, BirdGame3.BirdType.RAVEN, 0, 320.0);
        String[] groundedVariants = {
                "NEUTRAL", "SIDE_TILT", "UP_TILT", "DOWN_TILT",
                "SIDE_SMASH", "UP_SMASH", "DOWN_SMASH",
                "DASH_ATTACK", "LEDGE_ATTACK", "GETUP_ATTACK"
        };

        for (String variantName : groundedVariants) {
            Object variant = enumConstant("com.example.birdgame3.Bird$NormalAttackVariant", variantName);
            Object pose = invoke(raven, "currentRavenNormalAttackPose", variant, 1.0);
            double translateY = (double) invoke(pose, "translateY");
            double rotation = (double) invoke(pose, "bodyRotationDegrees");
            assertTrue(translateY <= 0.0, variantName + " must not sink Raven below the stage");
            assertTrue(Math.abs(rotation) <= 16.0,
                    variantName + " must keep Raven's narrow corvid silhouette readable");
        }
    }

    @Test
    void ravenNormalsStayPreciseAndOnlyDownTiltAuthorsAPortent() throws Exception {
        BirdGame3 game = new BirdGame3();
        Bird raven = groundedBird(game, BirdGame3.BirdType.RAVEN, 0, 320.0);
        Object neutralAir = enumConstant("com.example.birdgame3.Bird$NormalAttackVariant", "NEUTRAL_AIR");
        Object sideSmash = enumConstant("com.example.birdgame3.Bird$NormalAttackVariant", "SIDE_SMASH");
        Object downTilt = enumConstant("com.example.birdgame3.Bird$NormalAttackVariant", "DOWN_TILT");
        Object smashProfile = invoke(raven, "normalAttackProfile", sideSmash);
        int groundPortents = ((List<?>) getField(raven, "ravenGroundPortents")).size();
        int quills = ((List<?>) getField(raven, "ravenQuills")).size();

        invoke(raven, "performAttack", 0, neutralAir);

        assertEquals(groundPortents, ((List<?>) getField(raven, "ravenGroundPortents")).size());
        assertEquals(quills, ((List<?>) getField(raven, "ravenQuills")).size());
        assertNull(getField(raven, "ravenDecoy"));
        assertTrue((double) invoke(smashProfile, "horizontalLaunchScale") <= 1.48,
                "Black-Sun Verdict must not restore the old zero-percent instant-KO problem.");
        assertTrue((int) invoke(smashProfile, "cooldownFrames") >= 38,
                "Raven's strongest verdict must be punishable when it misses.");

        raven.attackCooldown = 0;
        invoke(raven, "performAttack", 0, downTilt);
        assertEquals(groundPortents + 1, ((List<?>) getField(raven, "ravenGroundPortents")).size(),
                "Portent Scratch should preserve Raven's established grounded omen setup.");
    }

    @Test
    void ravenAiOnlyCashesOutShadowWarpAfterTelegraphingAPortent() throws Exception {
        BirdGame3 game = new BirdGame3();
        game.activePlayers = 2;
        Bird raven = groundedBird(game, BirdGame3.BirdType.RAVEN, 0, 320.0);
        Bird target = groundedBird(game, BirdGame3.BirdType.PIGEON, 1, 620.0);
        game.players[0] = raven;
        game.players[1] = target;
        invoke(raven, "addRavenGroundPortent", 500.0, BirdGame3.GROUND_Y, false);

        assertEquals(Bird.DirectionalSpecialInput.NEUTRAL,
                raven.chooseRavenAISpecialInput(target, 300.0, true, false, false, false),
                "CPU Raven must not turn a spare node into a frame-perfect generic approach.");

        setField(target, "ravenPortentOwnerIndex", raven.playerIndex);
        setField(target, "ravenPortentTimer", 120);
        raven.ravenSideReuseTimer = 0;
        assertEquals(Bird.DirectionalSpecialInput.SIDE,
                raven.chooseRavenAISpecialInput(target, 300.0, true, false, false, false),
                "A visibly marked target should enable Raven's authored route payoff.");

        raven.ravenSideReuseTimer = 1;
        assertEquals(Bird.DirectionalSpecialInput.NEUTRAL,
                raven.chooseRavenAISpecialInput(target, 300.0, true, false, false, false),
                "The route payoff must continue respecting its reuse window.");
    }

    @Test
    void ravenWingsAuthorEveryNormalAttackSilhouette() throws Exception {
        BirdGame3 game = new BirdGame3();
        Object neutralAir = enumConstant("com.example.birdgame3.Bird$NormalAttackVariant", "NEUTRAL_AIR");
        Object upSmash = enumConstant("com.example.birdgame3.Bird$NormalAttackVariant", "UP_SMASH");
        Object downTilt = enumConstant("com.example.birdgame3.Bird$NormalAttackVariant", "DOWN_TILT");
        Bird orbiting = groundedBird(game, BirdGame3.BirdType.RAVEN, 0, 320.0);
        Bird ascending = groundedBird(game, BirdGame3.BirdType.RAVEN, 1, 320.0);
        Bird scratching = groundedBird(game, BirdGame3.BirdType.RAVEN, 2, 320.0);

        invoke(orbiting, "performAttack", 0, neutralAir);
        double orbitOpenness = (double) invoke(orbiting, "ravenWingOpenness",
                invoke(orbiting, "currentBirdAnimationState"));
        invoke(ascending, "performAttack", 0, upSmash);
        double ascensionOpenness = (double) invoke(ascending, "ravenWingOpenness",
                invoke(ascending, "currentBirdAnimationState"));
        invoke(scratching, "performAttack", 0, downTilt);
        double scratchOpenness = (double) invoke(scratching, "ravenWingOpenness",
                invoke(scratching, "currentBirdAnimationState"));

        assertTrue(orbitOpenness >= 0.98);
        assertTrue(ascensionOpenness >= 0.98);
        assertTrue(scratchOpenness <= 0.18);
    }

    @Test
    void ravenAttackIdentityAndOmenStateRoundTripThroughLanState() throws Exception {
        BirdGame3 game = new BirdGame3();
        Bird source = groundedBird(game, BirdGame3.BirdType.RAVEN, 0, 320.0);
        source.ravenQuillCharging = true;
        source.ravenQuillChargeFrames = 18;
        Object upAir = enumConstant("com.example.birdgame3.Bird$NormalAttackVariant", "UP_AIR");
        invoke(source, "performAttack", 0, upAir);

        Bird restored = groundedBird(game, BirdGame3.BirdType.RAVEN, 0, 320.0);
        restored.applyLanState(source.toLanState());

        assertEquals("UP_AIR", ((Enum<?>) getField(restored, "activeAttackVariant")).name());
        assertEquals(source.attackAnimationTimer, restored.attackAnimationTimer);
        assertTrue(restored.ravenQuillCharging);
        assertEquals(18, restored.ravenQuillChargeFrames);
    }

    private static ThrowOutcome performThrow(String directionName) throws Exception {
        BirdGame3 game = new BirdGame3();
        game.activePlayers = 2;
        Bird raven = groundedBird(game, BirdGame3.BirdType.RAVEN, 0, 320.0);
        Bird target = groundedBird(game, BirdGame3.BirdType.PIGEON, 1, 385.0);
        game.players[0] = raven;
        game.players[1] = target;
        linkGrab(raven, target);
        Object direction = enumConstant("com.example.birdgame3.Bird$GrabThrowDirection", directionName);
        String name = (String) invoke(raven, "throwTelemetryName", direction);
        double before = target.health;
        invoke(raven, "performThrow", direction);
        return new ThrowOutcome(before - target.health, target.vx, target.vy, name);
    }

    private static Bird groundedBird(BirdGame3 game, BirdGame3.BirdType type, int index, double x) {
        if (game.platforms.isEmpty()) {
            game.platforms.add(new Platform(0.0, BirdGame3.GROUND_Y, BirdGame3.WORLD_WIDTH, 80.0));
        }
        Bird bird = new Bird(x, type, index, game);
        bird.y = BirdGame3.GROUND_Y - bird.bodyHeight();
        return bird;
    }

    private static void linkGrab(Bird holder, Bird target) throws Exception {
        setField(holder, "grabbedTarget", target);
        setField(target, "grabbedBy", holder);
        setField(holder, "grabHoldTimer", 60);
    }

    private static Class<?> normalAttackVariantClass() throws ClassNotFoundException {
        return Class.forName("com.example.birdgame3.Bird$NormalAttackVariant");
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static Object enumConstant(String className, String name) throws Exception {
        Class enumClass = Class.forName(className);
        return Enum.valueOf(enumClass, name);
    }

    private static Object invoke(Object target, String name, Object... args) throws Exception {
        Method match = null;
        for (Method method : target.getClass().getDeclaredMethods()) {
            if (method.getName().equals(name) && method.getParameterCount() == args.length) {
                match = method;
                break;
            }
        }
        assertNotNull(match, "Missing method " + name);
        match.setAccessible(true);
        return match.invoke(target, args);
    }

    private static Object getField(Object target, String name) throws Exception {
        Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        return field.get(target);
    }

    private static int getInt(Object target, String name) throws Exception {
        return (int) getField(target, name);
    }

    private static void setField(Object target, String name, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        field.set(target, value);
    }

    private record ThrowOutcome(double damage, double vx, double vy, String name) {
    }
}
