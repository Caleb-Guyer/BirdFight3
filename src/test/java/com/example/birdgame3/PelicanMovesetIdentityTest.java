package com.example.birdgame3;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;

class PelicanMovesetIdentityTest {
    @Test
    void pelicanHasAnAuthoredCompleteHeavyweightCargoKit() throws Exception {
        BirdGame3 game = new BirdGame3();
        Bird pelican = groundedBird(game, BirdGame3.BirdType.PELICAN, 0, 320.0);
        Bird goose = groundedBird(game, BirdGame3.BirdType.GOOSE, 1, 320.0);
        Bird bat = groundedBird(game, BirdGame3.BirdType.BAT, 2, 320.0);

        int distinctFromGoose = 0;
        int distinctFromBat = 0;
        for (Object variant : normalAttackVariantClass().getEnumConstants()) {
            Object profile = invoke(pelican, "normalAttackProfile", variant);
            if (!profile.equals(invoke(goose, "normalAttackProfile", variant))) distinctFromGoose++;
            if (!profile.equals(invoke(bat, "normalAttackProfile", variant))) distinctFromBat++;
            String moveName = (String) invoke(pelican, "normalAttackTelemetryName", variant);
            assertTrue(moveName.startsWith("Pelican "));
            assertFalse(moveName.contains("Normal Attack"));
            assertFalse(moveName.endsWith(" Tilt"));
        }

        assertEquals(15, distinctFromGoose);
        assertEquals(15, distinctFromBat);
    }

    @Test
    void everyPlayableBirdNowHasAuthoredNormalMoveNames() throws Exception {
        BirdGame3 game = new BirdGame3();
        String[] fallbackLabels = {
                "Neutral Attack", "Side Tilt", "Up Tilt", "Down Tilt",
                "Side Smash", "Up Smash", "Down Smash", "Neutral Air",
                "Forward Air", "Back Air", "Up Air", "Down Air",
                "Dash Attack", "Ledge Attack", "Get-Up Attack"
        };
        Object[] variants = normalAttackVariantClass().getEnumConstants();

        for (BirdGame3.BirdType type : BirdGame3.BirdType.values()) {
            Bird bird = groundedBird(game, type, 0, 320.0);
            for (int i = 0; i < variants.length; i++) {
                String moveName = (String) invoke(bird, "normalAttackTelemetryName", variants[i]);
                assertNotEquals(type.name + " " + fallbackLabels[i], moveName,
                        type.name + " still uses the shared fallback telemetry for " + fallbackLabels[i]);
            }
        }
    }

    @Test
    void pelicanPummelIsTheSlowHeavyPouchPress() throws Exception {
        BirdGame3 game = new BirdGame3();
        game.activePlayers = 2;
        Bird pelican = groundedBird(game, BirdGame3.BirdType.PELICAN, 0, 320.0);
        Bird target = groundedBird(game, BirdGame3.BirdType.PIGEON, 1, 390.0);
        game.players[0] = pelican;
        game.players[1] = target;
        linkGrab(pelican, target);
        game.setAiControlKey(0, game.attackKeyForPlayer(0), true);

        double healthBefore = target.health;
        assertTrue((boolean) invoke(pelican, "handleHoldingGrabState", false, false));

        double tunedDamage = 4.0 * pelican.type.damageDealtMult * target.type.damageTakenMult;
        assertEquals(tunedDamage, healthBefore - target.health, 0.001);
        assertSame(target, getField(pelican, "grabbedTarget"));
        assertSame(pelican, getField(target, "grabbedBy"));
        assertEquals(13, getInt(pelican, "grabThrowLockTimer"));
        assertEquals(49, getInt(pelican, "grabHoldTimer"));
    }

    @Test
    void pelicanThrowsMoveCargoAcrossEveryHarborLane() throws Exception {
        BirdGame3 game = new BirdGame3();
        Bird pelican = groundedBird(game, BirdGame3.BirdType.PELICAN, 0, 320.0);

        Object forward = throwProfile(pelican, "FORWARD");
        Object back = throwProfile(pelican, "BACK");
        Object up = throwProfile(pelican, "UP");
        Object down = throwProfile(pelican, "DOWN");

        assertTrue((double) invoke(forward, "launchX") >= 20.8);
        assertTrue((double) invoke(back, "launchX") <= -22.2);
        assertTrue((double) invoke(up, "launchY") < (double) invoke(back, "launchY"));
        assertTrue((int) invoke(down, "damage") > (int) invoke(up, "damage"));
        assertEquals("Pelican Cargo Cast", throwName(pelican, "FORWARD"));
        assertEquals("Pelican Stern Toss", throwName(pelican, "BACK"));
        assertEquals("Pelican Geyser Hoist", throwName(pelican, "UP"));
        assertEquals("Pelican Bilge Drop", throwName(pelican, "DOWN"));
    }

    @Test
    void pelicanGroundedAttackPosesStayFloorSafeAndReadable() throws Exception {
        BirdGame3 game = new BirdGame3();
        Bird pelican = groundedBird(game, BirdGame3.BirdType.PELICAN, 0, 320.0);
        String[] groundedVariants = {
                "NEUTRAL", "SIDE_TILT", "UP_TILT", "DOWN_TILT",
                "SIDE_SMASH", "UP_SMASH", "DOWN_SMASH",
                "DASH_ATTACK", "LEDGE_ATTACK", "GETUP_ATTACK"
        };

        for (String variantName : groundedVariants) {
            Object variant = enumConstant("com.example.birdgame3.Bird$NormalAttackVariant", variantName);
            Object pose = invoke(pelican, "currentPelicanNormalAttackPose", variant, 1.0);
            assertTrue((double) invoke(pose, "translateY") <= 0.0,
                    variantName + " must not sink Pelican below the stage");
            assertTrue(Math.abs((double) invoke(pose, "bodyRotationDegrees")) <= 14.0,
                    variantName + " must preserve Pelican's readable grounded silhouette");
        }
    }

    @Test
    void pelicanNormalsDoNotConsumeCargoOrMutateItsSpecialState() throws Exception {
        BirdGame3 game = new BirdGame3();
        Bird pelican = groundedBird(game, BirdGame3.BirdType.PELICAN, 0, 320.0);
        Object neutralAir = enumConstant("com.example.birdgame3.Bird$NormalAttackVariant", "NEUTRAL_AIR");
        Object sideSmash = enumConstant("com.example.birdgame3.Bird$NormalAttackVariant", "SIDE_SMASH");
        Object smashProfile = invoke(pelican, "normalAttackProfile", sideSmash);
        pelican.pelicanCargoCount = 2;
        pelican.pelicanNeutralReuseTimer = 17;
        pelican.pelicanSideReuseTimer = 23;
        pelican.pelicanUpSpecialUsed = true;
        pelican.pelicanDownReuseTimer = 29;
        pelican.pelicanFullHoldTimer = 31;

        invoke(pelican, "performAttack", 0, neutralAir);

        assertEquals(2, pelican.pelicanCargoCount);
        assertEquals(17, pelican.pelicanNeutralReuseTimer);
        assertEquals(23, pelican.pelicanSideReuseTimer);
        assertTrue(pelican.pelicanUpSpecialUsed);
        assertEquals(29, pelican.pelicanDownReuseTimer);
        assertEquals(31, pelican.pelicanFullHoldTimer);
        assertEquals(0, pelican.pelicanNeutralTimer);
        assertEquals(0, pelican.pelicanSideTimer);
        assertEquals(0, pelican.pelicanUpTimer);
        assertFalse(pelican.pelicanDownCharging);
        assertEquals(0, pelican.pelicanBilgeFxTimer);
        assertEquals(0, pelican.pelicanMaelstromTimer);
        assertTrue((double) invoke(smashProfile, "horizontalLaunchScale") <= 1.62);
        assertTrue((int) invoke(smashProfile, "cooldownFrames") >= 40);
    }

    @Test
    void pelicanWingAnimationDistinguishesCargoRollFromAnchorDrop() throws Exception {
        BirdGame3 game = new BirdGame3();
        Bird pelican = groundedBird(game, BirdGame3.BirdType.PELICAN, 0, 320.0);
        Object attackState = enumConstant("com.example.birdgame3.Bird$BirdAnimationState", "ATTACK");
        Object neutralAir = enumConstant("com.example.birdgame3.Bird$NormalAttackVariant", "NEUTRAL_AIR");
        Object downAir = enumConstant("com.example.birdgame3.Bird$NormalAttackVariant", "DOWN_AIR");

        setField(pelican, "activeAttackVariant", neutralAir);
        setField(pelican, "attackAnimationTimer", 10);
        double rollOpenness = (double) invoke(pelican, "pelicanWingOpenness", attackState);
        setField(pelican, "activeAttackVariant", downAir);
        double dropOpenness = (double) invoke(pelican, "pelicanWingOpenness", attackState);

        assertTrue(rollOpenness >= 0.94, "Cargo Roll should spread both wings");
        assertTrue(dropOpenness <= 0.16, "Anchor Drop should tuck both wings");
        assertTrue(rollOpenness - dropOpenness >= 0.75);
    }

    @Test
    void pelicanLanStatePreservesCargoHitMasksAndMaelstromGeometry() {
        BirdGame3 sourceGame = new BirdGame3();
        sourceGame.activePlayers = 2;
        Bird source = groundedBird(sourceGame, BirdGame3.BirdType.PELICAN, 0, 320.0);
        Bird sourceTarget = groundedBird(sourceGame, BirdGame3.BirdType.PIGEON, 1, 760.0);
        sourceGame.players[0] = source;
        sourceGame.players[1] = sourceTarget;
        source.pelicanCargoCount = 2;
        source.pelicanNeutralHit[1] = true;
        source.pelicanSideHit[0] = true;
        source.pelicanUpHit[1] = true;
        source.pelicanMaelstromFinalHit[1] = true;
        source.pelicanKeelDiveActive = true;
        source.pelicanMaelstromTimer = 93;
        source.pelicanMaelstromPulseCooldown = 7;
        source.pelicanMaelstromCargoSpent = 2;
        source.pelicanMaelstromX = 612.75;
        source.pelicanMaelstromY = 455.25;

        LanBirdState snapshot = source.toLanState();

        BirdGame3 remoteGame = new BirdGame3();
        remoteGame.activePlayers = 2;
        Bird remote = groundedBird(remoteGame, BirdGame3.BirdType.PELICAN, 0, 320.0);
        Bird remoteTarget = groundedBird(remoteGame, BirdGame3.BirdType.PIGEON, 1, 760.0);
        remoteGame.players[0] = remote;
        remoteGame.players[1] = remoteTarget;
        remote.applyLanState(snapshot);

        assertEquals(2, remote.pelicanCargoCount);
        assertTrue(remote.pelicanNeutralHit[1]);
        assertTrue(remote.pelicanSideHit[0]);
        assertTrue(remote.pelicanUpHit[1]);
        assertTrue(remote.pelicanMaelstromFinalHit[1]);
        assertTrue(remote.pelicanKeelDiveActive);
        assertEquals(93, remote.pelicanMaelstromTimer);
        assertEquals(7, remote.pelicanMaelstromPulseCooldown);
        assertEquals(2, remote.pelicanMaelstromCargoSpent);
        assertEquals(612.75, remote.pelicanMaelstromX, 0.001);
        assertEquals(455.25, remote.pelicanMaelstromY, 0.001);
    }

    @Test
    void pelicanTransientSpecialStateSurvivesTheWireFormat() throws Exception {
        LanBirdState state = new LanBirdState();
        state.pelicanCargoCount = 2;
        state.pelicanNeutralHit[2] = true;
        state.pelicanSideHit[1] = true;
        state.pelicanUpHit[3] = true;
        state.pelicanMaelstromFinalHit[0] = true;
        state.pelicanKeelDiveActive = true;
        state.pelicanMaelstromTimer = 97;
        state.pelicanMaelstromPulseCooldown = 6;
        state.pelicanMaelstromCargoSpent = 2;
        state.pelicanMaelstromX = 411.25;
        state.pelicanMaelstromY = 512.5;

        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        state.write(new DataOutputStream(bytes));
        LanBirdState decoded = LanBirdState.read(
                new DataInputStream(new ByteArrayInputStream(bytes.toByteArray())));

        assertEquals(2, decoded.pelicanCargoCount);
        assertTrue(decoded.pelicanNeutralHit[2]);
        assertTrue(decoded.pelicanSideHit[1]);
        assertTrue(decoded.pelicanUpHit[3]);
        assertTrue(decoded.pelicanMaelstromFinalHit[0]);
        assertTrue(decoded.pelicanKeelDiveActive);
        assertEquals(97, decoded.pelicanMaelstromTimer);
        assertEquals(6, decoded.pelicanMaelstromPulseCooldown);
        assertEquals(2, decoded.pelicanMaelstromCargoSpent);
        assertEquals(411.25, decoded.pelicanMaelstromX, 0.001);
        assertEquals(512.5, decoded.pelicanMaelstromY, 0.001);
    }

    private static Object throwProfile(Bird bird, String direction) throws Exception {
        Object value = enumConstant("com.example.birdgame3.Bird$GrabThrowDirection", direction);
        return invoke(bird, "throwProfile", value, 1.0);
    }

    private static String throwName(Bird bird, String direction) throws Exception {
        Object value = enumConstant("com.example.birdgame3.Bird$GrabThrowDirection", direction);
        return (String) invoke(bird, "throwTelemetryName", value);
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
}
