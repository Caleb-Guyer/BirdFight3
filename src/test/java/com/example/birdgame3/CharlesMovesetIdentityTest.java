package com.example.birdgame3;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;

class CharlesMovesetIdentityTest {
    @Test
    void charlesHasAnAuthoredCompleteTempoControlKit() throws Exception {
        BirdGame3 game = new BirdGame3();
        Bird charles = groundedBird(game, BirdGame3.BirdType.MOCKINGBIRD, 0, 320.0);
        Bird shoebill = groundedBird(game, BirdGame3.BirdType.SHOEBILL, 1, 320.0);
        Bird vulture = groundedBird(game, BirdGame3.BirdType.VULTURE, 2, 320.0);

        int distinctFromShoebill = 0;
        int distinctFromSharedProfile = 0;
        for (Object variant : normalAttackVariantClass().getEnumConstants()) {
            Object profile = invoke(charles, "normalAttackProfile", variant);
            if (!profile.equals(invoke(shoebill, "normalAttackProfile", variant))) distinctFromShoebill++;
            if (!profile.equals(invoke(vulture, "normalAttackProfile", variant))) distinctFromSharedProfile++;
            String moveName = (String) invoke(charles, "normalAttackTelemetryName", variant);
            assertTrue(moveName.startsWith("Charles "));
            assertFalse(moveName.contains("Normal Attack"));
            assertFalse(moveName.contains("Tilt"));
        }

        assertEquals(15, distinctFromShoebill);
        assertEquals(15, distinctFromSharedProfile);
    }

    @Test
    void charlesPummelKeepsAQuickMetronomeTempo() throws Exception {
        BirdGame3 game = new BirdGame3();
        game.activePlayers = 2;
        Bird charles = groundedBird(game, BirdGame3.BirdType.MOCKINGBIRD, 0, 320.0);
        Bird target = groundedBird(game, BirdGame3.BirdType.PIGEON, 1, 385.0);
        game.players[0] = charles;
        game.players[1] = target;
        linkGrab(charles, target);
        game.setAiControlKey(0, game.attackKeyForPlayer(0), true);

        double healthBefore = target.health;
        assertTrue((boolean) invoke(charles, "handleHoldingGrabState", false, false));

        double tunedDamage = 2.0 * charles.type.damageDealtMult * target.type.damageTakenMult;
        assertEquals(tunedDamage, healthBefore - target.health, 0.001);
        assertSame(target, getField(charles, "grabbedTarget"));
        assertSame(charles, getField(target, "grabbedBy"));
        assertEquals(8, getInt(charles, "grabThrowLockTimer"));
        assertEquals(53, getInt(charles, "grabHoldTimer"),
                "The normal hold tick plus Charles's six-frame pummel cost should be deterministic.");
    }

    @Test
    void charlesThrowsDirectTheOpponentToFourStageLanes() throws Exception {
        ThrowOutcome forward = performThrow("FORWARD");
        ThrowOutcome back = performThrow("BACK");
        ThrowOutcome up = performThrow("UP");
        ThrowOutcome down = performThrow("DOWN");

        assertTrue(forward.vx >= 18.5);
        assertTrue(back.vx <= -19.5);
        assertTrue(up.vy < back.vy);
        assertTrue(down.damage > up.damage);
        assertEquals("Charles Stage Direction", forward.name);
        assertEquals("Charles Curtain Call", back.name);
        assertEquals("Charles High Note", up.name);
        assertEquals("Charles Final Bow", down.name);
    }

    @Test
    void charlesGroundedAttackPosesKeepHisFeetAboveTheStage() throws Exception {
        BirdGame3 game = new BirdGame3();
        Bird charles = groundedBird(game, BirdGame3.BirdType.MOCKINGBIRD, 0, 320.0);
        String[] groundedVariants = {
                "NEUTRAL", "SIDE_TILT", "UP_TILT", "DOWN_TILT",
                "SIDE_SMASH", "UP_SMASH", "DOWN_SMASH",
                "DASH_ATTACK", "LEDGE_ATTACK", "GETUP_ATTACK"
        };

        for (String variantName : groundedVariants) {
            Object variant = enumConstant("com.example.birdgame3.Bird$NormalAttackVariant", variantName);
            Object pose = invoke(charles, "currentCharlesNormalAttackPose", variant, 1.0);
            double translateY = (double) invoke(pose, "translateY");
            double rotation = (double) invoke(pose, "bodyRotationDegrees");
            assertTrue(translateY <= 0.0, variantName + " must not sink Charles below the stage");
            assertTrue(Math.abs(rotation) <= 12.0,
                    variantName + " must preserve Charles's grounded stage presence");
        }
    }

    @Test
    void charlesQuickPokesCreateSetupsWithoutReplacingTheMicrophoneFinisher() throws Exception {
        BirdGame3 game = new BirdGame3();
        Bird charles = groundedBird(game, BirdGame3.BirdType.MOCKINGBIRD, 0, 320.0);
        Bird shoebill = groundedBird(game, BirdGame3.BirdType.SHOEBILL, 1, 320.0);
        Object neutral = enumConstant("com.example.birdgame3.Bird$NormalAttackVariant", "NEUTRAL");
        Object sideSmash = enumConstant("com.example.birdgame3.Bird$NormalAttackVariant", "SIDE_SMASH");
        Object charlesNeutral = invoke(charles, "normalAttackProfile", neutral);
        Object shoebillNeutral = invoke(shoebill, "normalAttackProfile", neutral);
        Object charlesSmash = invoke(charles, "normalAttackProfile", sideSmash);

        assertTrue((int) invoke(charlesNeutral, "cooldownFrames")
                < (int) invoke(shoebillNeutral, "cooldownFrames"));
        assertTrue((double) invoke(charlesNeutral, "knockbackMultiplier") < 0.70,
                "Charles's cue tap should start sequences instead of ending them.");
        assertTrue((double) invoke(charlesSmash, "horizontalLaunchScale") < 1.50,
                "His normal finisher must remain below the charged microphone's payoff.");
    }

    @Test
    void charlesWingsOpenForOvationAndTuckForQuickChange() throws Exception {
        BirdGame3 game = new BirdGame3();
        Bird charles = groundedBird(game, BirdGame3.BirdType.MOCKINGBIRD, 0, 320.0);
        Object sideSmash = enumConstant("com.example.birdgame3.Bird$NormalAttackVariant", "SIDE_SMASH");
        Object dashAttack = enumConstant("com.example.birdgame3.Bird$NormalAttackVariant", "DASH_ATTACK");

        invoke(charles, "performAttack", 0, sideSmash);
        Object attackState = invoke(charles, "currentBirdAnimationState");
        double ovationOpenness = (double) invoke(charles, "charlesWingOpenness", attackState);

        invoke(charles, "performAttack", 0, dashAttack);
        attackState = invoke(charles, "currentBirdAnimationState");
        double quickChangeOpenness = (double) invoke(charles, "charlesWingOpenness", attackState);

        assertTrue(ovationOpenness >= 0.72, "Standing Ovation must visibly spread Charles's wings");
        assertTrue(quickChangeOpenness <= 0.32, "Quick Change must tuck Charles's wings for a readable dash");
    }

    @Test
    void charlesAttackIdentityRoundTripsThroughLanState() throws Exception {
        BirdGame3 game = new BirdGame3();
        Bird source = groundedBird(game, BirdGame3.BirdType.MOCKINGBIRD, 0, 320.0);
        Object neutralAir = enumConstant("com.example.birdgame3.Bird$NormalAttackVariant", "NEUTRAL_AIR");
        invoke(source, "performAttack", 0, neutralAir);

        Bird restored = groundedBird(game, BirdGame3.BirdType.MOCKINGBIRD, 0, 320.0);
        restored.applyLanState(source.toLanState());

        assertEquals("NEUTRAL_AIR", ((Enum<?>) getField(restored, "activeAttackVariant")).name());
        assertEquals(source.attackAnimationTimer, restored.attackAnimationTimer);
    }

    private static ThrowOutcome performThrow(String directionName) throws Exception {
        BirdGame3 game = new BirdGame3();
        game.activePlayers = 2;
        Bird charles = groundedBird(game, BirdGame3.BirdType.MOCKINGBIRD, 0, 320.0);
        Bird target = groundedBird(game, BirdGame3.BirdType.PIGEON, 1, 385.0);
        game.players[0] = charles;
        game.players[1] = target;
        linkGrab(charles, target);
        Object direction = enumConstant("com.example.birdgame3.Bird$GrabThrowDirection", directionName);
        String name = (String) invoke(charles, "throwTelemetryName", direction);
        double before = target.health;
        invoke(charles, "performThrow", direction);
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
