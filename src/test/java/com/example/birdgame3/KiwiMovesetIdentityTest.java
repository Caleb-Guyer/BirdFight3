package com.example.birdgame3;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;

class KiwiMovesetIdentityTest {
    @Test
    void kiwiHasAnAuthoredCompleteGroundedForagerKit() throws Exception {
        BirdGame3 game = new BirdGame3();
        Bird kiwi = groundedBird(game, BirdGame3.BirdType.KIWI, 0, 320.0);
        Bird goose = groundedBird(game, BirdGame3.BirdType.GOOSE, 1, 320.0);
        Bird sharedBird = groundedBird(game, BirdGame3.BirdType.TITMOUSE, 2, 320.0);

        int distinctFromGoose = 0;
        int distinctFromSharedProfile = 0;
        for (Object variant : normalAttackVariantClass().getEnumConstants()) {
            Object profile = invoke(kiwi, "normalAttackProfile", variant);
            if (!profile.equals(invoke(goose, "normalAttackProfile", variant))) distinctFromGoose++;
            if (!profile.equals(invoke(sharedBird, "normalAttackProfile", variant))) distinctFromSharedProfile++;
            String moveName = (String) invoke(kiwi, "normalAttackTelemetryName", variant);
            assertTrue(moveName.startsWith("Kiwi "));
            assertFalse(moveName.contains("Normal Attack"));
            assertFalse(moveName.contains("Tilt"));
        }

        assertEquals(15, distinctFromGoose);
        assertEquals(15, distinctFromSharedProfile);
    }

    @Test
    void kiwiPummelUsesADeliberateRootProbe() throws Exception {
        BirdGame3 game = new BirdGame3();
        game.activePlayers = 2;
        Bird kiwi = groundedBird(game, BirdGame3.BirdType.KIWI, 0, 320.0);
        Bird target = groundedBird(game, BirdGame3.BirdType.PIGEON, 1, 385.0);
        game.players[0] = kiwi;
        game.players[1] = target;
        linkGrab(kiwi, target);
        game.setAiControlKey(0, game.attackKeyForPlayer(0), true);

        double healthBefore = target.health;
        assertTrue((boolean) invoke(kiwi, "handleHoldingGrabState", false, false));

        double tunedDamage = 3.0 * kiwi.type.damageDealtMult * target.type.damageTakenMult;
        assertEquals(tunedDamage, healthBefore - target.health, 0.001);
        assertSame(target, getField(kiwi, "grabbedTarget"));
        assertSame(kiwi, getField(target, "grabbedBy"));
        assertEquals(9, getInt(kiwi, "grabThrowLockTimer"));
        assertEquals(52, getInt(kiwi, "grabHoldTimer"),
                "The hold loses one simulation tick before Kiwi pays the seven-frame probe cost.");
    }

    @Test
    void kiwiThrowsUseRootsFernsAndGroundControl() throws Exception {
        ThrowOutcome forward = performThrow("FORWARD");
        ThrowOutcome back = performThrow("BACK");
        ThrowOutcome up = performThrow("UP");
        ThrowOutcome down = performThrow("DOWN");

        assertTrue(forward.vx >= 19.0);
        assertTrue(back.vx <= -19.8);
        assertTrue(up.vy < back.vy);
        assertTrue(down.damage > up.damage);
        assertEquals("Kiwi Underbrush Heave", forward.name);
        assertEquals("Kiwi Root Reversal", back.name);
        assertEquals("Kiwi Fern Lift", up.name);
        assertEquals("Kiwi Burrow Press", down.name);
    }

    @Test
    void kiwiGroundedAttackPosesStayFloorSafeAndKeepTheBillReadable() throws Exception {
        BirdGame3 game = new BirdGame3();
        Bird kiwi = groundedBird(game, BirdGame3.BirdType.KIWI, 0, 320.0);
        String[] groundedVariants = {
                "NEUTRAL", "SIDE_TILT", "UP_TILT", "DOWN_TILT",
                "SIDE_SMASH", "UP_SMASH", "DOWN_SMASH",
                "DASH_ATTACK", "LEDGE_ATTACK", "GETUP_ATTACK"
        };

        for (String variantName : groundedVariants) {
            Object variant = enumConstant("com.example.birdgame3.Bird$NormalAttackVariant", variantName);
            Object pose = invoke(kiwi, "currentKiwiNormalAttackPose", variant, 1.0);
            double translateY = (double) invoke(pose, "translateY");
            double rotation = (double) invoke(pose, "bodyRotationDegrees");
            assertTrue(translateY <= 0.0, variantName + " must not sink Kiwi below the stage");
            assertTrue(Math.abs(rotation) <= 10.0,
                    variantName + " must keep Kiwi's bill and planted stance readable");
        }
    }

    @Test
    void kiwiNormalsDoNotMutateProbeBurrowSpringStompOrUltimateState() throws Exception {
        BirdGame3 game = new BirdGame3();
        Bird kiwi = groundedBird(game, BirdGame3.BirdType.KIWI, 0, 320.0);
        Object neutralAir = enumConstant("com.example.birdgame3.Bird$NormalAttackVariant", "NEUTRAL_AIR");
        Object sideSmash = enumConstant("com.example.birdgame3.Bird$NormalAttackVariant", "SIDE_SMASH");
        Object smashProfile = invoke(kiwi, "normalAttackProfile", sideSmash);
        kiwi.kiwiProbeReuseTimer = 17;
        kiwi.kiwiBurrowReuseTimer = 23;
        kiwi.kiwiSpringReuseTimer = 29;
        kiwi.kiwiStompReuseTimer = 31;
        kiwi.kiwiUltimateWaveIndex = 2;

        invoke(kiwi, "performAttack", 0, neutralAir);

        assertEquals(17, kiwi.kiwiProbeReuseTimer);
        assertEquals(23, kiwi.kiwiBurrowReuseTimer);
        assertEquals(29, kiwi.kiwiSpringReuseTimer);
        assertEquals(31, kiwi.kiwiStompReuseTimer);
        assertEquals(2, kiwi.kiwiUltimateWaveIndex);
        assertEquals(0, kiwi.kiwiProbeTimer);
        assertEquals(0, kiwi.kiwiBurrowTimer);
        assertEquals(0, kiwi.kiwiSpringTimer);
        assertEquals(0, kiwi.kiwiStompTimer);
        assertEquals(0, kiwi.kiwiUltimateTimer);
        assertTrue((double) invoke(smashProfile, "horizontalLaunchScale") <= 1.68,
                "Burrow Breaker must keep the approved zero-percent launch ceiling.");
        assertTrue((int) invoke(smashProfile, "cooldownFrames") >= 38,
                "Kiwi's strongest grounded bill strike must remain punishable on whiff.");
    }

    @Test
    void kiwiLegsAuthorDistinctGroundedAndAerialSilhouettes() throws Exception {
        BirdGame3 game = new BirdGame3();
        Object neutral = enumConstant("com.example.birdgame3.Bird$NormalAttackVariant", "NEUTRAL");
        Object neutralAir = enumConstant("com.example.birdgame3.Bird$NormalAttackVariant", "NEUTRAL_AIR");
        Object upAir = enumConstant("com.example.birdgame3.Bird$NormalAttackVariant", "UP_AIR");
        Object downAir = enumConstant("com.example.birdgame3.Bird$NormalAttackVariant", "DOWN_AIR");
        Bird probing = groundedBird(game, BirdGame3.BirdType.KIWI, 0, 320.0);
        Bird wheeling = groundedBird(game, BirdGame3.BirdType.KIWI, 1, 320.0);
        Bird kickingUp = groundedBird(game, BirdGame3.BirdType.KIWI, 2, 320.0);
        Bird stompingDown = groundedBird(game, BirdGame3.BirdType.KIWI, 3, 320.0);

        invoke(probing, "performAttack", 0, neutral);
        double groundedExtension = (double) invoke(probing, "kiwiLegExtension",
                invoke(probing, "currentBirdAnimationState"));
        invoke(wheeling, "performAttack", 0, neutralAir);
        double wheelExtension = (double) invoke(wheeling, "kiwiLegExtension",
                invoke(wheeling, "currentBirdAnimationState"));
        invoke(kickingUp, "performAttack", 0, upAir);
        double upExtension = (double) invoke(kickingUp, "kiwiLegExtension",
                invoke(kickingUp, "currentBirdAnimationState"));
        invoke(stompingDown, "performAttack", 0, downAir);
        double downExtension = (double) invoke(stompingDown, "kiwiLegExtension",
                invoke(stompingDown, "currentBirdAnimationState"));

        assertEquals(0.0, groundedExtension, 0.001);
        assertTrue(wheelExtension >= 0.75);
        assertTrue(upExtension >= 0.89);
        assertTrue(downExtension >= 0.99);
    }

    @Test
    void kiwiAttackIdentityAndAllSpecialStateRoundTripThroughLanState() throws Exception {
        BirdGame3 game = new BirdGame3();
        Bird source = groundedBird(game, BirdGame3.BirdType.KIWI, 0, 320.0);
        source.kiwiProbeTimer = 12;
        source.kiwiProbeStrikeIndex = 2;
        source.kiwiProbeHit[1] = true;
        source.kiwiBurrowTimer = 21;
        source.kiwiBurrowGrounded = true;
        source.kiwiBurrowErupted = true;
        source.kiwiBurrowHit[2] = true;
        source.kiwiSpringTimer = 16;
        source.kiwiSpringUsed = true;
        source.kiwiSpringHit[3] = true;
        source.kiwiStompTimer = 18;
        source.kiwiStompAirborne = true;
        source.kiwiStompImpactResolved = true;
        source.kiwiStompHit[0] = true;
        source.kiwiUltimateTimer = 117;
        source.kiwiUltimateWaveIndex = 3;
        source.kiwiUltimateFinalResolved = true;
        source.kiwiUltimateHitCooldown[2] = 7;
        Object upAir = enumConstant("com.example.birdgame3.Bird$NormalAttackVariant", "UP_AIR");
        invoke(source, "performAttack", 0, upAir);

        Bird restored = groundedBird(game, BirdGame3.BirdType.KIWI, 0, 320.0);
        restored.applyLanState(source.toLanState());

        assertEquals("UP_AIR", ((Enum<?>) getField(restored, "activeAttackVariant")).name());
        assertEquals(source.attackAnimationTimer, restored.attackAnimationTimer);
        assertEquals(12, restored.kiwiProbeTimer);
        assertEquals(2, restored.kiwiProbeStrikeIndex);
        assertTrue(restored.kiwiProbeHit[1]);
        assertEquals(21, restored.kiwiBurrowTimer);
        assertTrue(restored.kiwiBurrowGrounded);
        assertTrue(restored.kiwiBurrowErupted);
        assertTrue(restored.kiwiBurrowHit[2]);
        assertEquals(16, restored.kiwiSpringTimer);
        assertTrue(restored.kiwiSpringUsed);
        assertTrue(restored.kiwiSpringHit[3]);
        assertEquals(18, restored.kiwiStompTimer);
        assertTrue(restored.kiwiStompAirborne);
        assertTrue(restored.kiwiStompImpactResolved);
        assertTrue(restored.kiwiStompHit[0]);
        assertEquals(117, restored.kiwiUltimateTimer);
        assertEquals(3, restored.kiwiUltimateWaveIndex);
        assertTrue(restored.kiwiUltimateFinalResolved);
        assertEquals(7, restored.kiwiUltimateHitCooldown[2]);
    }

    private static ThrowOutcome performThrow(String directionName) throws Exception {
        BirdGame3 game = new BirdGame3();
        game.activePlayers = 2;
        Bird kiwi = groundedBird(game, BirdGame3.BirdType.KIWI, 0, 320.0);
        Bird target = groundedBird(game, BirdGame3.BirdType.PIGEON, 1, 385.0);
        game.players[0] = kiwi;
        game.players[1] = target;
        linkGrab(kiwi, target);
        Object direction = enumConstant("com.example.birdgame3.Bird$GrabThrowDirection", directionName);
        String name = (String) invoke(kiwi, "throwTelemetryName", direction);
        double before = target.health;
        invoke(kiwi, "performThrow", direction);
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
