package com.example.birdgame3;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;

class VultureMovesetIdentityTest {
    @Test
    void vultureHasAnAuthoredCompletePatientScavengerKit() throws Exception {
        BirdGame3 game = new BirdGame3();
        Bird vulture = groundedBird(game, BirdGame3.BirdType.VULTURE, 0, 320.0);
        Bird grinchHawk = groundedBird(game, BirdGame3.BirdType.GRINCHHAWK, 1, 320.0);
        Bird titmouse = groundedBird(game, BirdGame3.BirdType.TITMOUSE, 2, 320.0);

        int distinctFromGrinchHawk = 0;
        int distinctFromTitmouse = 0;
        for (Object variant : normalAttackVariantClass().getEnumConstants()) {
            Object profile = invoke(vulture, "normalAttackProfile", variant);
            if (!profile.equals(invoke(grinchHawk, "normalAttackProfile", variant))) distinctFromGrinchHawk++;
            if (!profile.equals(invoke(titmouse, "normalAttackProfile", variant))) distinctFromTitmouse++;
            String moveName = (String) invoke(vulture, "normalAttackTelemetryName", variant);
            assertTrue(moveName.startsWith("Vulture "));
            assertFalse(moveName.contains("Normal Attack"));
            assertFalse(moveName.contains("Tilt"));
        }

        assertEquals(15, distinctFromGrinchHawk);
        assertEquals(15, distinctFromTitmouse);
    }

    @Test
    void vulturePummelUsesADeliberateCarrionClamp() throws Exception {
        BirdGame3 game = new BirdGame3();
        game.activePlayers = 2;
        Bird vulture = groundedBird(game, BirdGame3.BirdType.VULTURE, 0, 320.0);
        Bird target = groundedBird(game, BirdGame3.BirdType.PIGEON, 1, 385.0);
        game.players[0] = vulture;
        game.players[1] = target;
        linkGrab(vulture, target);
        game.setAiControlKey(0, game.attackKeyForPlayer(0), true);

        double healthBefore = target.health;
        assertTrue((boolean) invoke(vulture, "handleHoldingGrabState", false, false));

        double tunedDamage = 3.0 * vulture.type.damageDealtMult * target.type.damageTakenMult;
        assertEquals(tunedDamage, healthBefore - target.health, 0.001);
        assertSame(target, getField(vulture, "grabbedTarget"));
        assertSame(vulture, getField(target, "grabbedBy"));
        assertEquals(10, getInt(vulture, "grabThrowLockTimer"));
        assertEquals(51, getInt(vulture, "grabHoldTimer"),
                "The normal hold tick plus Vulture's eight-frame clamp cost must stay deterministic.");
    }

    @Test
    void vultureThrowsUseFourDistinctCarrionControlLanes() throws Exception {
        ThrowOutcome forward = performThrow("FORWARD");
        ThrowOutcome back = performThrow("BACK");
        ThrowOutcome up = performThrow("UP");
        ThrowOutcome down = performThrow("DOWN");

        assertTrue(forward.vx >= 19.0);
        assertTrue(back.vx <= -21.0);
        assertTrue(up.vy < back.vy);
        assertTrue(down.damage > up.damage);
        assertEquals("Vulture Castoff", forward.name);
        assertEquals("Vulture Wing Reap", back.name);
        assertEquals("Vulture Thermal Hoist", up.name);
        assertEquals("Vulture Bone Drop", down.name);
    }

    @Test
    void vultureGroundedAttackPosesKeepHisTalonsOnTheStage() throws Exception {
        BirdGame3 game = new BirdGame3();
        Bird vulture = groundedBird(game, BirdGame3.BirdType.VULTURE, 0, 320.0);
        String[] groundedVariants = {
                "NEUTRAL", "SIDE_TILT", "UP_TILT", "DOWN_TILT",
                "SIDE_SMASH", "UP_SMASH", "DOWN_SMASH",
                "DASH_ATTACK", "LEDGE_ATTACK", "GETUP_ATTACK"
        };

        for (String variantName : groundedVariants) {
            Object variant = enumConstant("com.example.birdgame3.Bird$NormalAttackVariant", variantName);
            Object pose = invoke(vulture, "currentVultureNormalAttackPose", variant, 1.0);
            double translateY = (double) invoke(pose, "translateY");
            double rotation = (double) invoke(pose, "bodyRotationDegrees");
            assertTrue(translateY <= 0.0, variantName + " must not sink Vulture below the stage");
            assertTrue(Math.abs(rotation) <= 15.0,
                    variantName + " must preserve Vulture's planted, deliberate grounded silhouette");
        }
    }

    @Test
    void vultureNormalsFavorBroadCoverageAndCommittedFinishers() throws Exception {
        BirdGame3 game = new BirdGame3();
        Bird vulture = groundedBird(game, BirdGame3.BirdType.VULTURE, 0, 320.0);
        Object neutralAir = enumConstant("com.example.birdgame3.Bird$NormalAttackVariant", "NEUTRAL_AIR");
        Object sideSmash = enumConstant("com.example.birdgame3.Bird$NormalAttackVariant", "SIDE_SMASH");
        Object downAir = enumConstant("com.example.birdgame3.Bird$NormalAttackVariant", "DOWN_AIR");
        Object vultureNeutralAir = invoke(vulture, "normalAttackProfile", neutralAir);
        Object vultureSideSmash = invoke(vulture, "normalAttackProfile", sideSmash);
        Object vultureDownAir = invoke(vulture, "normalAttackProfile", downAir);

        assertTrue((double) invoke(vultureNeutralAir, "horizontalReach") >= 160.0,
                "Circling Wings must retain its authored broad space control as other birds gain complete kits.");
        assertTrue((double) invoke(vultureNeutralAir, "verticalReach") >= 140.0);
        assertTrue((double) invoke(vultureSideSmash, "damageMultiplier") >= 1.20);
        assertTrue((double) invoke(vultureSideSmash, "horizontalLaunchScale") >= 1.55);
        assertTrue((int) invoke(vultureSideSmash, "cooldownFrames") >= 38,
                "Last Pickings is a deliberate finisher, not a safe pressure button.");
        assertTrue((double) invoke(vultureDownAir, "meteorVerticalLaunchScale") <= -1.08,
                "Final Descent must retain a committed meteor payoff.");
    }

    @Test
    void vultureWingsSpreadForCoverageAndFoldForLowBoneRake() throws Exception {
        BirdGame3 game = new BirdGame3();
        Object neutralAir = enumConstant("com.example.birdgame3.Bird$NormalAttackVariant", "NEUTRAL_AIR");
        Object upSmash = enumConstant("com.example.birdgame3.Bird$NormalAttackVariant", "UP_SMASH");
        Object downTilt = enumConstant("com.example.birdgame3.Bird$NormalAttackVariant", "DOWN_TILT");
        Bird circling = groundedBird(game, BirdGame3.BirdType.VULTURE, 0, 320.0);
        Bird rising = groundedBird(game, BirdGame3.BirdType.VULTURE, 1, 320.0);
        Bird raking = groundedBird(game, BirdGame3.BirdType.VULTURE, 2, 320.0);

        invoke(circling, "performAttack", 0, neutralAir);
        Object attackState = invoke(circling, "currentBirdAnimationState");
        double circlingOpenness = (double) invoke(circling, "vultureWingOpenness", attackState);

        invoke(rising, "performAttack", 0, upSmash);
        attackState = invoke(rising, "currentBirdAnimationState");
        double risingOpenness = (double) invoke(rising, "vultureWingOpenness", attackState);

        invoke(raking, "performAttack", 0, downTilt);
        attackState = invoke(raking, "currentBirdAnimationState");
        double rakeOpenness = (double) invoke(raking, "vultureWingOpenness", attackState);

        assertTrue(circlingOpenness >= 0.98, "Circling Wings must fill Vulture's broad aerial envelope.");
        assertTrue(risingOpenness >= 0.98, "Black Sky Rise must reach full wing extension.");
        assertTrue(rakeOpenness <= 0.35, "Bone Rake must fold into a low scavenging silhouette.");
    }

    @Test
    void vultureAttackIdentityRoundTripsThroughLanState() throws Exception {
        BirdGame3 game = new BirdGame3();
        Bird source = groundedBird(game, BirdGame3.BirdType.VULTURE, 0, 320.0);
        Object neutralAir = enumConstant("com.example.birdgame3.Bird$NormalAttackVariant", "NEUTRAL_AIR");
        invoke(source, "performAttack", 0, neutralAir);

        Bird restored = groundedBird(game, BirdGame3.BirdType.VULTURE, 0, 320.0);
        restored.applyLanState(source.toLanState());

        assertEquals("NEUTRAL_AIR", ((Enum<?>) getField(restored, "activeAttackVariant")).name());
        assertEquals(source.attackAnimationTimer, restored.attackAnimationTimer);
    }

    private static ThrowOutcome performThrow(String directionName) throws Exception {
        BirdGame3 game = new BirdGame3();
        game.activePlayers = 2;
        Bird vulture = groundedBird(game, BirdGame3.BirdType.VULTURE, 0, 320.0);
        Bird target = groundedBird(game, BirdGame3.BirdType.PIGEON, 1, 385.0);
        game.players[0] = vulture;
        game.players[1] = target;
        linkGrab(vulture, target);
        Object direction = enumConstant("com.example.birdgame3.Bird$GrabThrowDirection", directionName);
        String name = (String) invoke(vulture, "throwTelemetryName", direction);
        double before = target.health;
        invoke(vulture, "performThrow", direction);
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
