package com.example.birdgame3;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;

class HeisenbirdMovesetIdentityTest {
    @Test
    void heisenbirdHasAnAuthoredCompleteCrystalEngineerKit() throws Exception {
        BirdGame3 game = new BirdGame3();
        Bird heisenbird = groundedBird(game, BirdGame3.BirdType.HEISENBIRD, 0, 320.0);
        Bird opiumBird = groundedBird(game, BirdGame3.BirdType.OPIUMBIRD, 1, 320.0);
        Bird sharedBird = groundedBird(game, BirdGame3.BirdType.TITMOUSE, 2, 320.0);

        int distinctFromOpiumBird = 0;
        int distinctFromSharedProfile = 0;
        for (Object variant : normalAttackVariantClass().getEnumConstants()) {
            Object profile = invoke(heisenbird, "normalAttackProfile", variant);
            if (!profile.equals(invoke(opiumBird, "normalAttackProfile", variant))) distinctFromOpiumBird++;
            if (!profile.equals(invoke(sharedBird, "normalAttackProfile", variant))) distinctFromSharedProfile++;
            String moveName = (String) invoke(heisenbird, "normalAttackTelemetryName", variant);
            assertTrue(moveName.startsWith("Heisenbird "));
            assertFalse(moveName.contains("Normal Attack"));
            assertFalse(moveName.contains("Tilt"));
        }

        assertEquals(15, distinctFromOpiumBird,
                "Heisenbird must no longer inherit Opium Bird's dream-control normals.");
        assertEquals(15, distinctFromSharedProfile);
    }

    @Test
    void heisenbirdPummelUsesAQuickMeasuredCrystalCheck() throws Exception {
        BirdGame3 game = new BirdGame3();
        game.activePlayers = 2;
        Bird heisenbird = groundedBird(game, BirdGame3.BirdType.HEISENBIRD, 0, 320.0);
        Bird target = groundedBird(game, BirdGame3.BirdType.PIGEON, 1, 385.0);
        game.players[0] = heisenbird;
        game.players[1] = target;
        linkGrab(heisenbird, target);
        game.setAiControlKey(0, game.attackKeyForPlayer(0), true);

        double healthBefore = target.health;
        assertTrue((boolean) invoke(heisenbird, "handleHoldingGrabState", false, false));

        double tunedDamage = 2.0 * heisenbird.type.damageDealtMult * target.type.damageTakenMult;
        assertEquals(tunedDamage, healthBefore - target.health, 0.001);
        assertSame(target, getField(heisenbird, "grabbedTarget"));
        assertSame(heisenbird, getField(target, "grabbedBy"));
        assertEquals(9, getInt(heisenbird, "grabThrowLockTimer"));
        assertEquals(52, getInt(heisenbird, "grabHoldTimer"),
                "The normal hold tick plus Heisenbird's seven-frame check must stay deterministic.");
    }

    @Test
    void heisenbirdThrowsCoverFourDistinctProductionLanes() throws Exception {
        ThrowOutcome forward = performThrow("FORWARD");
        ThrowOutcome back = performThrow("BACK");
        ThrowOutcome up = performThrow("UP");
        ThrowOutcome down = performThrow("DOWN");

        assertTrue(forward.vx >= 18.0);
        assertTrue(back.vx <= -19.5);
        assertTrue(up.vy < back.vy);
        assertTrue(down.damage > up.damage);
        assertEquals("Heisenbird Product Launch", forward.name);
        assertEquals("Heisenbird Batch Reversal", back.name);
        assertEquals("Heisenbird Blue-Sky Lift", up.name);
        assertEquals("Heisenbird Quality Control", down.name);
    }

    @Test
    void heisenbirdGroundedAttackPosesStayFloorSafeAndReadable() throws Exception {
        BirdGame3 game = new BirdGame3();
        Bird heisenbird = groundedBird(game, BirdGame3.BirdType.HEISENBIRD, 0, 320.0);
        String[] groundedVariants = {
                "NEUTRAL", "SIDE_TILT", "UP_TILT", "DOWN_TILT",
                "SIDE_SMASH", "UP_SMASH", "DOWN_SMASH",
                "DASH_ATTACK", "LEDGE_ATTACK", "GETUP_ATTACK"
        };

        for (String variantName : groundedVariants) {
            Object variant = enumConstant("com.example.birdgame3.Bird$NormalAttackVariant", variantName);
            Object pose = invoke(heisenbird, "currentHeisenbirdNormalAttackPose", variant, 1.0);
            double translateY = (double) invoke(pose, "translateY");
            double rotation = (double) invoke(pose, "bodyRotationDegrees");
            assertTrue(translateY <= 0.0, variantName + " must not sink Heisenbird below the stage");
            assertTrue(Math.abs(rotation) <= 15.0,
                    variantName + " must keep the crystal-engineer silhouette readable");
        }
    }

    @Test
    void heisenbirdNormalsArePreciseWithoutGeneratingSpecialResources() throws Exception {
        BirdGame3 game = new BirdGame3();
        Bird heisenbird = groundedBird(game, BirdGame3.BirdType.HEISENBIRD, 0, 320.0);
        Bird opiumBird = groundedBird(game, BirdGame3.BirdType.OPIUMBIRD, 1, 320.0);
        Object neutralAir = enumConstant("com.example.birdgame3.Bird$NormalAttackVariant", "NEUTRAL_AIR");
        Object upSmash = enumConstant("com.example.birdgame3.Bird$NormalAttackVariant", "UP_SMASH");
        Object sideSmash = enumConstant("com.example.birdgame3.Bird$NormalAttackVariant", "SIDE_SMASH");
        Object heisenNeutralAir = invoke(heisenbird, "normalAttackProfile", neutralAir);
        Object opiumNeutralAir = invoke(opiumBird, "normalAttackProfile", neutralAir);
        Object heisenUpSmash = invoke(heisenbird, "normalAttackProfile", upSmash);
        Object opiumUpSmash = invoke(opiumBird, "normalAttackProfile", upSmash);
        Object heisenSideSmash = invoke(heisenbird, "normalAttackProfile", sideSmash);
        heisenbird.opiumResourceMeter = 43.0;
        int trapCount = heisenbird.opiumTraps.size();

        invoke(heisenbird, "performAttack", 0, neutralAir);

        assertEquals(43.0, heisenbird.opiumResourceMeter, 0.001,
                "Normals must not silently refuel the crystal-special economy.");
        assertEquals(trapCount, heisenbird.opiumTraps.size());
        assertTrue((double) invoke(heisenNeutralAir, "horizontalReach")
                <= (double) invoke(opiumNeutralAir, "horizontalReach") - 20.0,
                "Heisenbird's orbit must remain the narrower precision tool.");
        assertTrue((double) invoke(heisenUpSmash, "verticalReach")
                >= (double) invoke(opiumUpSmash, "verticalReach") + 10.0,
                "The crystal column must own vertical precision instead of broad haze coverage.");
        assertTrue((double) invoke(heisenSideSmash, "horizontalLaunchScale") <= 1.48,
                "Blue Crystal Break must not restore the old zero-percent instant-KO problem.");
        assertTrue((int) invoke(heisenSideSmash, "cooldownFrames") >= 38);
    }

    @Test
    void heisenbirdWingsAuthorEveryNormalAttackSilhouette() throws Exception {
        BirdGame3 game = new BirdGame3();
        Object neutralAir = enumConstant("com.example.birdgame3.Bird$NormalAttackVariant", "NEUTRAL_AIR");
        Object upSmash = enumConstant("com.example.birdgame3.Bird$NormalAttackVariant", "UP_SMASH");
        Object downTilt = enumConstant("com.example.birdgame3.Bird$NormalAttackVariant", "DOWN_TILT");
        Bird orbiting = groundedBird(game, BirdGame3.BirdType.HEISENBIRD, 0, 320.0);
        Bird column = groundedBird(game, BirdGame3.BirdType.HEISENBIRD, 1, 320.0);
        Bird lowYield = groundedBird(game, BirdGame3.BirdType.HEISENBIRD, 2, 320.0);

        invoke(orbiting, "performAttack", 0, neutralAir);
        double orbitOpenness = (double) invoke(orbiting, "heisenWingOpenness",
                invoke(orbiting, "currentBirdAnimationState"));
        invoke(column, "performAttack", 0, upSmash);
        double columnOpenness = (double) invoke(column, "heisenWingOpenness",
                invoke(column, "currentBirdAnimationState"));
        invoke(lowYield, "performAttack", 0, downTilt);
        double lowOpenness = (double) invoke(lowYield, "heisenWingOpenness",
                invoke(lowYield, "currentBirdAnimationState"));

        assertTrue(orbitOpenness >= 0.94);
        assertTrue(columnOpenness >= 0.98);
        assertTrue(lowOpenness <= 0.18);
    }

    @Test
    void heisenbirdAttackIdentityAndCrystalStateRoundTripThroughLanState() throws Exception {
        BirdGame3 game = new BirdGame3();
        Bird source = groundedBird(game, BirdGame3.BirdType.HEISENBIRD, 0, 320.0);
        source.opiumResourceMeter = 37.5;
        source.heisenBrittleTimer = 87;
        Object upAir = enumConstant("com.example.birdgame3.Bird$NormalAttackVariant", "UP_AIR");
        invoke(source, "performAttack", 0, upAir);

        Bird restored = groundedBird(game, BirdGame3.BirdType.HEISENBIRD, 0, 320.0);
        restored.applyLanState(source.toLanState());

        assertEquals("UP_AIR", ((Enum<?>) getField(restored, "activeAttackVariant")).name());
        assertEquals(source.attackAnimationTimer, restored.attackAnimationTimer);
        assertEquals(37.5, restored.opiumResourceMeter, 0.001);
        assertEquals(87, restored.heisenBrittleTimer);
    }

    private static ThrowOutcome performThrow(String directionName) throws Exception {
        BirdGame3 game = new BirdGame3();
        game.activePlayers = 2;
        Bird heisenbird = groundedBird(game, BirdGame3.BirdType.HEISENBIRD, 0, 320.0);
        Bird target = groundedBird(game, BirdGame3.BirdType.PIGEON, 1, 385.0);
        game.players[0] = heisenbird;
        game.players[1] = target;
        linkGrab(heisenbird, target);
        Object direction = enumConstant("com.example.birdgame3.Bird$GrabThrowDirection", directionName);
        String name = (String) invoke(heisenbird, "throwTelemetryName", direction);
        double before = target.health;
        invoke(heisenbird, "performThrow", direction);
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
