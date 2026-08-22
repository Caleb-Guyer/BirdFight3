package com.example.birdgame3;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;

class OpiumBirdMovesetIdentityTest {
    @Test
    void opiumBirdHasAnAuthoredCompleteDreamControlKit() throws Exception {
        BirdGame3 game = new BirdGame3();
        Bird opiumBird = groundedBird(game, BirdGame3.BirdType.OPIUMBIRD, 0, 320.0);
        Bird heisenbird = groundedBird(game, BirdGame3.BirdType.HEISENBIRD, 1, 320.0);
        Bird titmouse = groundedBird(game, BirdGame3.BirdType.TITMOUSE, 2, 320.0);

        int distinctFromHeisenbird = 0;
        int distinctFromTitmouse = 0;
        for (Object variant : normalAttackVariantClass().getEnumConstants()) {
            Object profile = invoke(opiumBird, "normalAttackProfile", variant);
            if (!profile.equals(invoke(heisenbird, "normalAttackProfile", variant))) distinctFromHeisenbird++;
            if (!profile.equals(invoke(titmouse, "normalAttackProfile", variant))) distinctFromTitmouse++;
            String moveName = (String) invoke(opiumBird, "normalAttackTelemetryName", variant);
            assertTrue(moveName.startsWith("Opium Bird "));
            assertFalse(moveName.contains("Normal Attack"));
            assertFalse(moveName.contains("Tilt"));
        }

        assertEquals(15, distinctFromHeisenbird,
                "Opium Bird must no longer inherit Heisenbird's generic normal kit.");
        assertEquals(15, distinctFromTitmouse);
    }

    @Test
    void opiumBirdPummelUsesAQuickLowDamageDreamTap() throws Exception {
        BirdGame3 game = new BirdGame3();
        game.activePlayers = 2;
        Bird opiumBird = groundedBird(game, BirdGame3.BirdType.OPIUMBIRD, 0, 320.0);
        Bird target = groundedBird(game, BirdGame3.BirdType.PIGEON, 1, 385.0);
        game.players[0] = opiumBird;
        game.players[1] = target;
        linkGrab(opiumBird, target);
        game.setAiControlKey(0, game.attackKeyForPlayer(0), true);

        double healthBefore = target.health;
        assertTrue((boolean) invoke(opiumBird, "handleHoldingGrabState", false, false));

        double tunedDamage = 2.0 * opiumBird.type.damageDealtMult * target.type.damageTakenMult;
        assertEquals(tunedDamage, healthBefore - target.health, 0.001);
        assertSame(target, getField(opiumBird, "grabbedTarget"));
        assertSame(opiumBird, getField(target, "grabbedBy"));
        assertEquals(8, getInt(opiumBird, "grabThrowLockTimer"));
        assertEquals(53, getInt(opiumBird, "grabHoldTimer"),
                "The normal hold tick plus Opium Bird's six-frame tap cost must stay deterministic.");
    }

    @Test
    void opiumBirdThrowsCoverFourDistinctDreamControlLanes() throws Exception {
        ThrowOutcome forward = performThrow("FORWARD");
        ThrowOutcome back = performThrow("BACK");
        ThrowOutcome up = performThrow("UP");
        ThrowOutcome down = performThrow("DOWN");

        assertTrue(forward.vx >= 17.5);
        assertTrue(back.vx <= -19.5);
        assertTrue(up.vy < back.vy);
        assertTrue(down.damage > up.damage);
        assertEquals("Opium Bird Haze Release", forward.name);
        assertEquals("Opium Bird Reverie Reversal", back.name);
        assertEquals("Opium Bird Lucid Lift", up.name);
        assertEquals("Opium Bird Lotus Bed", down.name);
    }

    @Test
    void opiumBirdGroundedAttackPosesKeepHisFeetOnTheStage() throws Exception {
        BirdGame3 game = new BirdGame3();
        Bird opiumBird = groundedBird(game, BirdGame3.BirdType.OPIUMBIRD, 0, 320.0);
        String[] groundedVariants = {
                "NEUTRAL", "SIDE_TILT", "UP_TILT", "DOWN_TILT",
                "SIDE_SMASH", "UP_SMASH", "DOWN_SMASH",
                "DASH_ATTACK", "LEDGE_ATTACK", "GETUP_ATTACK"
        };

        for (String variantName : groundedVariants) {
            Object variant = enumConstant("com.example.birdgame3.Bird$NormalAttackVariant", variantName);
            Object pose = invoke(opiumBird, "currentOpiumBirdNormalAttackPose", variant, 1.0);
            double translateY = (double) invoke(pose, "translateY");
            double rotation = (double) invoke(pose, "bodyRotationDegrees");
            assertTrue(translateY <= 0.0, variantName + " must not sink Opium Bird below the stage");
            assertTrue(Math.abs(rotation) <= 14.0,
                    variantName + " must preserve the grounded dream-seer's readable silhouette");
        }
    }

    @Test
    void opiumBirdNormalsFavorCoverageWithoutBecomingHiddenResourceMoves() throws Exception {
        BirdGame3 game = new BirdGame3();
        Bird opiumBird = groundedBird(game, BirdGame3.BirdType.OPIUMBIRD, 0, 320.0);
        Object neutralAir = enumConstant("com.example.birdgame3.Bird$NormalAttackVariant", "NEUTRAL_AIR");
        Object sideSmash = enumConstant("com.example.birdgame3.Bird$NormalAttackVariant", "SIDE_SMASH");
        Object opiumNeutralAir = invoke(opiumBird, "normalAttackProfile", neutralAir);
        Object opiumSideSmash = invoke(opiumBird, "normalAttackProfile", sideSmash);
        double resourceBefore = opiumBird.opiumResourceMeter;

        invoke(opiumBird, "performAttack", 0, neutralAir);

        assertEquals(resourceBefore, opiumBird.opiumResourceMeter, 0.001,
                "Normal attacks must not imply resource behavior that only the special kit owns.");
        assertTrue((double) invoke(opiumNeutralAir, "horizontalReach") >= 144.0,
                "Dream Orbit must retain authored circular coverage as other birds gain complete kits.");
        assertTrue((double) invoke(opiumNeutralAir, "verticalReach") >= 130.0);
        assertTrue((double) invoke(opiumSideSmash, "horizontalLaunchScale") <= 1.46,
                "Haze Break should finish through commitment, not raw launch inflation.");
        assertTrue((int) invoke(opiumSideSmash, "cooldownFrames") >= 36);
    }

    @Test
    void opiumBirdWingsBloomForOrbitAndFoldForLowFog() throws Exception {
        BirdGame3 game = new BirdGame3();
        Object neutralAir = enumConstant("com.example.birdgame3.Bird$NormalAttackVariant", "NEUTRAL_AIR");
        Object upSmash = enumConstant("com.example.birdgame3.Bird$NormalAttackVariant", "UP_SMASH");
        Object downTilt = enumConstant("com.example.birdgame3.Bird$NormalAttackVariant", "DOWN_TILT");
        Bird orbiting = groundedBird(game, BirdGame3.BirdType.OPIUMBIRD, 0, 320.0);
        Bird blooming = groundedBird(game, BirdGame3.BirdType.OPIUMBIRD, 1, 320.0);
        Bird fogging = groundedBird(game, BirdGame3.BirdType.OPIUMBIRD, 2, 320.0);

        invoke(orbiting, "performAttack", 0, neutralAir);
        double orbitOpenness = (double) invoke(orbiting, "opiumWingOpenness",
                invoke(orbiting, "currentBirdAnimationState"));
        invoke(blooming, "performAttack", 0, upSmash);
        double bloomOpenness = (double) invoke(blooming, "opiumWingOpenness",
                invoke(blooming, "currentBirdAnimationState"));
        invoke(fogging, "performAttack", 0, downTilt);
        double fogOpenness = (double) invoke(fogging, "opiumWingOpenness",
                invoke(fogging, "currentBirdAnimationState"));

        assertTrue(orbitOpenness >= 0.98);
        assertTrue(bloomOpenness >= 0.98);
        assertTrue(fogOpenness <= 0.20);
    }

    @Test
    void opiumBirdAttackIdentityAndResourceRoundTripThroughLanState() throws Exception {
        BirdGame3 game = new BirdGame3();
        Bird source = groundedBird(game, BirdGame3.BirdType.OPIUMBIRD, 0, 320.0);
        source.opiumResourceMeter = 41.5;
        Object neutralAir = enumConstant("com.example.birdgame3.Bird$NormalAttackVariant", "NEUTRAL_AIR");
        invoke(source, "performAttack", 0, neutralAir);

        Bird restored = groundedBird(game, BirdGame3.BirdType.OPIUMBIRD, 0, 320.0);
        restored.applyLanState(source.toLanState());

        assertEquals("NEUTRAL_AIR", ((Enum<?>) getField(restored, "activeAttackVariant")).name());
        assertEquals(source.attackAnimationTimer, restored.attackAnimationTimer);
        assertEquals(41.5, restored.opiumResourceMeter, 0.001);
    }

    private static ThrowOutcome performThrow(String directionName) throws Exception {
        BirdGame3 game = new BirdGame3();
        game.activePlayers = 2;
        Bird opiumBird = groundedBird(game, BirdGame3.BirdType.OPIUMBIRD, 0, 320.0);
        Bird target = groundedBird(game, BirdGame3.BirdType.PIGEON, 1, 385.0);
        game.players[0] = opiumBird;
        game.players[1] = target;
        linkGrab(opiumBird, target);
        Object direction = enumConstant("com.example.birdgame3.Bird$GrabThrowDirection", directionName);
        String name = (String) invoke(opiumBird, "throwTelemetryName", direction);
        double before = target.health;
        invoke(opiumBird, "performThrow", direction);
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
