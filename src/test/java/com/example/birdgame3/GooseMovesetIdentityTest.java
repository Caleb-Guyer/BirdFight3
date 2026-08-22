package com.example.birdgame3;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;

class GooseMovesetIdentityTest {
    @Test
    void gooseHasAnAuthoredCompleteTerritorialBruiserKit() throws Exception {
        BirdGame3 game = new BirdGame3();
        Bird goose = groundedBird(game, BirdGame3.BirdType.GOOSE, 0, 320.0);
        Bird raven = groundedBird(game, BirdGame3.BirdType.RAVEN, 1, 320.0);
        Bird sharedBird = groundedBird(game, BirdGame3.BirdType.TITMOUSE, 2, 320.0);

        int distinctFromRaven = 0;
        int distinctFromSharedProfile = 0;
        for (Object variant : normalAttackVariantClass().getEnumConstants()) {
            Object profile = invoke(goose, "normalAttackProfile", variant);
            if (!profile.equals(invoke(raven, "normalAttackProfile", variant))) distinctFromRaven++;
            if (!profile.equals(invoke(sharedBird, "normalAttackProfile", variant))) distinctFromSharedProfile++;
            String moveName = (String) invoke(goose, "normalAttackTelemetryName", variant);
            assertTrue(moveName.startsWith("Goose "));
            assertFalse(moveName.contains("Normal Attack"));
            assertFalse(moveName.contains("Tilt"));
        }

        assertEquals(15, distinctFromRaven);
        assertEquals(15, distinctFromSharedProfile);
    }

    @Test
    void goosePummelUsesADeliberateBeakClamp() throws Exception {
        BirdGame3 game = new BirdGame3();
        game.activePlayers = 2;
        Bird goose = groundedBird(game, BirdGame3.BirdType.GOOSE, 0, 320.0);
        Bird target = groundedBird(game, BirdGame3.BirdType.PIGEON, 1, 385.0);
        game.players[0] = goose;
        game.players[1] = target;
        linkGrab(goose, target);
        game.setAiControlKey(0, game.attackKeyForPlayer(0), true);

        double healthBefore = target.health;
        assertTrue((boolean) invoke(goose, "handleHoldingGrabState", false, false));

        double tunedDamage = 3.0 * goose.type.damageDealtMult * target.type.damageTakenMult;
        assertEquals(tunedDamage, healthBefore - target.health, 0.001);
        assertSame(target, getField(goose, "grabbedTarget"));
        assertSame(goose, getField(target, "grabbedBy"));
        assertEquals(10, getInt(goose, "grabThrowLockTimer"));
        assertEquals(51, getInt(goose, "grabHoldTimer"),
                "The hold loses one simulation tick before Goose pays the eight-frame clamp cost.");
    }

    @Test
    void gooseThrowsControlEveryBorderOfHisTerritory() throws Exception {
        ThrowOutcome forward = performThrow("FORWARD");
        ThrowOutcome back = performThrow("BACK");
        ThrowOutcome up = performThrow("UP");
        ThrowOutcome down = performThrow("DOWN");

        assertTrue(forward.vx >= 19.0);
        assertTrue(back.vx <= -20.0);
        assertTrue(up.vy < back.vy);
        assertTrue(down.damage > up.damage);
        assertEquals("Goose Open-Border Shove", forward.name);
        assertEquals("Goose Rearguard Cast", back.name);
        assertEquals("Goose V-Formation Toss", up.name);
        assertEquals("Goose Nest Eviction", down.name);
    }

    @Test
    void gooseGroundedAttackPosesStayFloorSafeAndKeepTheLongNeckReadable() throws Exception {
        BirdGame3 game = new BirdGame3();
        Bird goose = groundedBird(game, BirdGame3.BirdType.GOOSE, 0, 320.0);
        String[] groundedVariants = {
                "NEUTRAL", "SIDE_TILT", "UP_TILT", "DOWN_TILT",
                "SIDE_SMASH", "UP_SMASH", "DOWN_SMASH",
                "DASH_ATTACK", "LEDGE_ATTACK", "GETUP_ATTACK"
        };

        for (String variantName : groundedVariants) {
            Object variant = enumConstant("com.example.birdgame3.Bird$NormalAttackVariant", variantName);
            Object pose = invoke(goose, "currentGooseNormalAttackPose", variant, 1.0);
            double translateY = (double) invoke(pose, "translateY");
            double rotation = (double) invoke(pose, "bodyRotationDegrees");
            assertTrue(translateY <= 0.0, variantName + " must not sink Goose below the stage");
            assertTrue(Math.abs(rotation) <= 12.0,
                    variantName + " must keep Goose's long neck and planted torso readable");
        }
    }

    @Test
    void gooseNormalsDoNotMutateHonkBargeLiftNestOrTerritoryState() throws Exception {
        BirdGame3 game = new BirdGame3();
        Bird goose = groundedBird(game, BirdGame3.BirdType.GOOSE, 0, 320.0);
        Object neutralAir = enumConstant("com.example.birdgame3.Bird$NormalAttackVariant", "NEUTRAL_AIR");
        Object sideSmash = enumConstant("com.example.birdgame3.Bird$NormalAttackVariant", "SIDE_SMASH");
        Object smashProfile = invoke(goose, "normalAttackProfile", sideSmash);
        goose.gooseTerritoryMeter = 43.0;

        invoke(goose, "performAttack", 0, neutralAir);

        assertEquals(43.0, goose.gooseTerritoryMeter, 0.001);
        assertEquals(0, goose.gooseHonkHoldFrames);
        assertEquals(0, goose.gooseBargeTimer);
        assertEquals(0, goose.gooseLiftTimer);
        assertEquals(0, goose.gooseNestGuardTimer);
        assertNull(goose.gooseNest);
        assertTrue((double) invoke(smashProfile, "horizontalLaunchScale") <= 1.68,
                "Border Breaker must keep the approved zero-percent launch ceiling.");
        assertTrue((int) invoke(smashProfile, "cooldownFrames") >= 39,
                "Goose's strongest border claim must remain punishable on whiff.");
    }

    @Test
    void gooseWingsAuthorDistinctGroundedAerialAndRecoverySilhouettes() throws Exception {
        BirdGame3 game = new BirdGame3();
        Object neutral = enumConstant("com.example.birdgame3.Bird$NormalAttackVariant", "NEUTRAL");
        Object neutralAir = enumConstant("com.example.birdgame3.Bird$NormalAttackVariant", "NEUTRAL_AIR");
        Object upSmash = enumConstant("com.example.birdgame3.Bird$NormalAttackVariant", "UP_SMASH");
        Bird pecking = groundedBird(game, BirdGame3.BirdType.GOOSE, 0, 320.0);
        Bird wheeling = groundedBird(game, BirdGame3.BirdType.GOOSE, 1, 320.0);
        Bird claimingAirspace = groundedBird(game, BirdGame3.BirdType.GOOSE, 2, 320.0);

        invoke(pecking, "performAttack", 0, neutral);
        double peckOpenness = (double) invoke(pecking, "gooseWingOpenness",
                invoke(pecking, "currentBirdAnimationState"));
        invoke(wheeling, "performAttack", 0, neutralAir);
        double wheelOpenness = (double) invoke(wheeling, "gooseWingOpenness",
                invoke(wheeling, "currentBirdAnimationState"));
        invoke(claimingAirspace, "performAttack", 0, upSmash);
        double claimOpenness = (double) invoke(claimingAirspace, "gooseWingOpenness",
                invoke(claimingAirspace, "currentBirdAnimationState"));

        assertTrue(peckOpenness <= 0.14);
        assertTrue(wheelOpenness >= 0.98);
        assertTrue(claimOpenness >= 0.94);
    }

    @Test
    void gooseAttackIdentityAndTerritoryStateRoundTripThroughLanState() throws Exception {
        BirdGame3 game = new BirdGame3();
        Bird source = groundedBird(game, BirdGame3.BirdType.GOOSE, 0, 320.0);
        source.gooseTerritoryMeter = 73.0;
        source.gooseHonkHoldFrames = 17;
        Object upAir = enumConstant("com.example.birdgame3.Bird$NormalAttackVariant", "UP_AIR");
        invoke(source, "performAttack", 0, upAir);

        Bird restored = groundedBird(game, BirdGame3.BirdType.GOOSE, 0, 320.0);
        restored.applyLanState(source.toLanState());

        assertEquals("UP_AIR", ((Enum<?>) getField(restored, "activeAttackVariant")).name());
        assertEquals(source.attackAnimationTimer, restored.attackAnimationTimer);
        assertEquals(73.0, restored.gooseTerritoryMeter, 0.001);
        assertEquals(17, restored.gooseHonkHoldFrames);
    }

    private static ThrowOutcome performThrow(String directionName) throws Exception {
        BirdGame3 game = new BirdGame3();
        game.activePlayers = 2;
        Bird goose = groundedBird(game, BirdGame3.BirdType.GOOSE, 0, 320.0);
        Bird target = groundedBird(game, BirdGame3.BirdType.PIGEON, 1, 385.0);
        game.players[0] = goose;
        game.players[1] = target;
        linkGrab(goose, target);
        Object direction = enumConstant("com.example.birdgame3.Bird$GrabThrowDirection", directionName);
        String name = (String) invoke(goose, "throwTelemetryName", direction);
        double before = target.health;
        invoke(goose, "performThrow", direction);
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
