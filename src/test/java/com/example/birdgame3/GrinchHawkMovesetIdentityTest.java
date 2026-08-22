package com.example.birdgame3;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;

class GrinchHawkMovesetIdentityTest {
    @Test
    void grinchHawkHasAnAuthoredCompleteAmbushThiefKit() throws Exception {
        BirdGame3 game = new BirdGame3();
        Bird grinchHawk = groundedBird(game, BirdGame3.BirdType.GRINCHHAWK, 0, 320.0);
        Bird razorbill = groundedBird(game, BirdGame3.BirdType.RAZORBILL, 1, 320.0);
        Bird opiumBird = groundedBird(game, BirdGame3.BirdType.TITMOUSE, 2, 320.0);

        int distinctFromRazorbill = 0;
        int distinctFromSharedProfile = 0;
        for (Object variant : normalAttackVariantClass().getEnumConstants()) {
            Object profile = invoke(grinchHawk, "normalAttackProfile", variant);
            if (!profile.equals(invoke(razorbill, "normalAttackProfile", variant))) distinctFromRazorbill++;
            if (!profile.equals(invoke(opiumBird, "normalAttackProfile", variant))) distinctFromSharedProfile++;
            String moveName = (String) invoke(grinchHawk, "normalAttackTelemetryName", variant);
            assertTrue(moveName.startsWith("Grinch-Hawk "));
            assertFalse(moveName.contains("Normal Attack"));
            assertFalse(moveName.contains("Tilt"));
        }

        assertEquals(15, distinctFromRazorbill);
        assertEquals(15, distinctFromSharedProfile);
    }

    @Test
    void grinchHawkPummelUsesAQuickPocketPeck() throws Exception {
        BirdGame3 game = new BirdGame3();
        game.activePlayers = 2;
        Bird grinchHawk = groundedBird(game, BirdGame3.BirdType.GRINCHHAWK, 0, 320.0);
        Bird target = groundedBird(game, BirdGame3.BirdType.PIGEON, 1, 385.0);
        game.players[0] = grinchHawk;
        game.players[1] = target;
        linkGrab(grinchHawk, target);
        game.setAiControlKey(0, game.attackKeyForPlayer(0), true);

        double healthBefore = target.health;
        assertTrue((boolean) invoke(grinchHawk, "handleHoldingGrabState", false, false));

        double tunedDamage = 3.0 * grinchHawk.type.damageDealtMult * target.type.damageTakenMult;
        assertEquals(tunedDamage, healthBefore - target.health, 0.001);
        assertSame(target, getField(grinchHawk, "grabbedTarget"));
        assertSame(grinchHawk, getField(target, "grabbedBy"));
        assertEquals(8, getInt(grinchHawk, "grabThrowLockTimer"));
        assertEquals(53, getInt(grinchHawk, "grabHoldTimer"),
                "The normal hold tick plus Grinch-Hawk's six-frame pummel cost must stay deterministic.");
    }

    @Test
    void grinchHawkThrowsUseFourDistinctStolenGoodsLanes() throws Exception {
        ThrowOutcome forward = performThrow("FORWARD");
        ThrowOutcome back = performThrow("BACK");
        ThrowOutcome up = performThrow("UP");
        ThrowOutcome down = performThrow("DOWN");

        assertTrue(forward.vx >= 18.0);
        assertTrue(back.vx <= -20.0);
        assertTrue(up.vy < back.vy);
        assertTrue(down.damage > up.damage);
        assertEquals("Grinch-Hawk Package Toss", forward.name);
        assertEquals("Grinch-Hawk Sack Snatch", back.name);
        assertEquals("Grinch-Hawk Chimney Heave", up.name);
        assertEquals("Grinch-Hawk Coal Chute", down.name);
    }

    @Test
    void grinchHawkGroundedAttackPosesKeepHisTalonsOnTheStage() throws Exception {
        BirdGame3 game = new BirdGame3();
        Bird grinchHawk = groundedBird(game, BirdGame3.BirdType.GRINCHHAWK, 0, 320.0);
        String[] groundedVariants = {
                "NEUTRAL", "SIDE_TILT", "UP_TILT", "DOWN_TILT",
                "SIDE_SMASH", "UP_SMASH", "DOWN_SMASH",
                "DASH_ATTACK", "LEDGE_ATTACK", "GETUP_ATTACK"
        };

        for (String variantName : groundedVariants) {
            Object variant = enumConstant("com.example.birdgame3.Bird$NormalAttackVariant", variantName);
            Object pose = invoke(grinchHawk, "currentGrinchHawkNormalAttackPose", variant, 1.0);
            double translateY = (double) invoke(pose, "translateY");
            double rotation = (double) invoke(pose, "bodyRotationDegrees");
            assertTrue(translateY <= 0.0, variantName + " must not sink Grinch-Hawk below the stage");
            assertTrue(Math.abs(rotation) <= 13.0,
                    variantName + " must keep Grinch-Hawk's grounded ambush visibly planted");
        }
    }

    @Test
    void grinchHawkKitRewardsFastSetupsAndABackwardFinisher() throws Exception {
        BirdGame3 game = new BirdGame3();
        Bird grinchHawk = groundedBird(game, BirdGame3.BirdType.GRINCHHAWK, 0, 320.0);
        Bird opiumBird = groundedBird(game, BirdGame3.BirdType.TITMOUSE, 1, 320.0);
        Object downTilt = enumConstant("com.example.birdgame3.Bird$NormalAttackVariant", "DOWN_TILT");
        Object backAir = enumConstant("com.example.birdgame3.Bird$NormalAttackVariant", "BACK_AIR");
        Object dashAttack = enumConstant("com.example.birdgame3.Bird$NormalAttackVariant", "DASH_ATTACK");
        Object grinchDownTilt = invoke(grinchHawk, "normalAttackProfile", downTilt);
        Object sharedDownTilt = invoke(opiumBird, "normalAttackProfile", downTilt);
        Object grinchBackAir = invoke(grinchHawk, "normalAttackProfile", backAir);
        Object grinchDash = invoke(grinchHawk, "normalAttackProfile", dashAttack);

        assertTrue((int) invoke(grinchDownTilt, "cooldownFrames")
                < (int) invoke(sharedDownTilt, "cooldownFrames"));
        assertTrue((double) invoke(grinchDownTilt, "horizontalLaunchScale") < 0.80,
                "Stocking Rake should hold targets near Grinch-Hawk's trap space.");
        assertTrue((double) invoke(grinchBackAir, "horizontalReach") >= 168.0);
        assertTrue((double) invoke(grinchBackAir, "horizontalLaunchScale") >= 1.40,
                "Sack Swing is the authored backward finisher.");
        assertTrue((int) invoke(grinchDash, "cooldownFrames") <= 24,
                "Doorstep Dash should provide a quick grounded opening without replacing Sleigh Crash.");
    }

    @Test
    void grinchHawkWingsSpreadForTrapCoverageAndTuckForDoorstepDash() throws Exception {
        BirdGame3 game = new BirdGame3();
        Object neutralAir = enumConstant("com.example.birdgame3.Bird$NormalAttackVariant", "NEUTRAL_AIR");
        Object dashAttack = enumConstant("com.example.birdgame3.Bird$NormalAttackVariant", "DASH_ATTACK");
        Bird parcel = groundedBird(game, BirdGame3.BirdType.GRINCHHAWK, 0, 320.0);
        Bird dasher = groundedBird(game, BirdGame3.BirdType.GRINCHHAWK, 1, 320.0);

        invoke(parcel, "performAttack", 0, neutralAir);
        Object attackState = invoke(parcel, "currentBirdAnimationState");
        double parcelOpenness = (double) invoke(parcel, "grinchhawkWingOpenness", attackState);

        invoke(dasher, "performAttack", 0, dashAttack);
        attackState = invoke(dasher, "currentBirdAnimationState");
        double dashOpenness = (double) invoke(dasher, "grinchhawkWingOpenness", attackState);

        assertTrue(parcelOpenness >= 0.68, "Ragged Parcel must visibly cover Grinch-Hawk's body.");
        assertTrue(dashOpenness <= 0.30, "Doorstep Dash must keep a narrow, hunched silhouette.");
    }

    @Test
    void grinchHawkAttackIdentityRoundTripsThroughLanState() throws Exception {
        BirdGame3 game = new BirdGame3();
        Bird source = groundedBird(game, BirdGame3.BirdType.GRINCHHAWK, 0, 320.0);
        Object backAir = enumConstant("com.example.birdgame3.Bird$NormalAttackVariant", "BACK_AIR");
        invoke(source, "performAttack", 0, backAir);

        Bird restored = groundedBird(game, BirdGame3.BirdType.GRINCHHAWK, 0, 320.0);
        restored.applyLanState(source.toLanState());

        assertEquals("BACK_AIR", ((Enum<?>) getField(restored, "activeAttackVariant")).name());
        assertEquals(source.attackAnimationTimer, restored.attackAnimationTimer);
    }

    private static ThrowOutcome performThrow(String directionName) throws Exception {
        BirdGame3 game = new BirdGame3();
        game.activePlayers = 2;
        Bird grinchHawk = groundedBird(game, BirdGame3.BirdType.GRINCHHAWK, 0, 320.0);
        Bird target = groundedBird(game, BirdGame3.BirdType.PIGEON, 1, 385.0);
        game.players[0] = grinchHawk;
        game.players[1] = target;
        linkGrab(grinchHawk, target);
        Object direction = enumConstant("com.example.birdgame3.Bird$GrabThrowDirection", directionName);
        String name = (String) invoke(grinchHawk, "throwTelemetryName", direction);
        double before = target.health;
        invoke(grinchHawk, "performThrow", direction);
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
