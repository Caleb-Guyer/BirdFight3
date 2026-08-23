package com.example.birdgame3;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;

class BatMovesetIdentityTest {
    @Test
    void batHasAnAuthoredCompleteNocturnalAmbusherKit() throws Exception {
        BirdGame3 game = new BirdGame3();
        Bird bat = groundedBird(game, BirdGame3.BirdType.BAT, 0, 320.0);
        Bird titmouse = groundedBird(game, BirdGame3.BirdType.TITMOUSE, 1, 320.0);
        Bird pelican = groundedBird(game, BirdGame3.BirdType.PELICAN, 2, 320.0);

        int distinctFromTitmouse = 0;
        int distinctFromPelican = 0;
        for (Object variant : normalAttackVariantClass().getEnumConstants()) {
            Object profile = invoke(bat, "normalAttackProfile", variant);
            if (!profile.equals(invoke(titmouse, "normalAttackProfile", variant))) distinctFromTitmouse++;
            if (!profile.equals(invoke(pelican, "normalAttackProfile", variant))) distinctFromPelican++;
            String moveName = (String) invoke(bat, "normalAttackTelemetryName", variant);
            assertTrue(moveName.startsWith("Bat "));
            assertFalse(moveName.contains("Normal Attack"));
            assertFalse(moveName.contains("Tilt"));
        }

        assertEquals(15, distinctFromTitmouse);
        assertEquals(15, distinctFromPelican);
    }

    @Test
    void batPummelIsTheFastLowDamageEchoBite() throws Exception {
        BirdGame3 game = new BirdGame3();
        game.activePlayers = 2;
        Bird bat = groundedBird(game, BirdGame3.BirdType.BAT, 0, 320.0);
        Bird target = groundedBird(game, BirdGame3.BirdType.PIGEON, 1, 385.0);
        game.players[0] = bat;
        game.players[1] = target;
        linkGrab(bat, target);
        game.setAiControlKey(0, game.attackKeyForPlayer(0), true);

        double healthBefore = target.health;
        assertTrue((boolean) invoke(bat, "handleHoldingGrabState", false, false));

        double tunedDamage = 2.0 * bat.type.damageDealtMult * target.type.damageTakenMult;
        assertEquals(tunedDamage, healthBefore - target.health, 0.001);
        assertSame(target, getField(bat, "grabbedTarget"));
        assertSame(bat, getField(target, "grabbedBy"));
        assertEquals(8, getInt(bat, "grabThrowLockTimer"));
        assertEquals(53, getInt(bat, "grabHoldTimer"));
    }

    @Test
    void batThrowsControlShadowsMoonriseAndCaveFloor() throws Exception {
        BirdGame3 game = new BirdGame3();
        Bird bat = groundedBird(game, BirdGame3.BirdType.BAT, 0, 320.0);

        Object forward = throwProfile(bat, "FORWARD");
        Object back = throwProfile(bat, "BACK");
        Object up = throwProfile(bat, "UP");
        Object down = throwProfile(bat, "DOWN");

        assertTrue((double) invoke(forward, "launchX") >= 19.0);
        assertTrue((double) invoke(back, "launchX") <= -20.8);
        assertTrue((double) invoke(up, "launchY") < (double) invoke(back, "launchY"));
        assertTrue((int) invoke(down, "damage") > (int) invoke(up, "damage"));
        assertEquals("Bat Echo Cast", throwName(bat, "FORWARD"));
        assertEquals("Bat Shadow Reversal", throwName(bat, "BACK"));
        assertEquals("Bat Moonrise Lift", throwName(bat, "UP"));
        assertEquals("Bat Cave Drop", throwName(bat, "DOWN"));
    }

    @Test
    void batGroundedAttackPosesStayFloorSafeAndReadable() throws Exception {
        BirdGame3 game = new BirdGame3();
        Bird bat = groundedBird(game, BirdGame3.BirdType.BAT, 0, 320.0);
        String[] groundedVariants = {
                "NEUTRAL", "SIDE_TILT", "UP_TILT", "DOWN_TILT",
                "SIDE_SMASH", "UP_SMASH", "DOWN_SMASH",
                "DASH_ATTACK", "LEDGE_ATTACK", "GETUP_ATTACK"
        };

        for (String variantName : groundedVariants) {
            Object variant = enumConstant("com.example.birdgame3.Bird$NormalAttackVariant", variantName);
            Object pose = invoke(bat, "currentBatNormalAttackPose", variant, 1.0);
            assertTrue((double) invoke(pose, "translateY") <= 0.0,
                    variantName + " must not sink Bat below the stage");
            assertTrue(Math.abs((double) invoke(pose, "bodyRotationDegrees")) <= 14.0,
                    variantName + " must preserve Bat's readable grounded silhouette");
        }
    }

    @Test
    void batNormalsPreserveSpecialRoutesAndConsumeOnlyTheAuthoredAmbushWindow() throws Exception {
        BirdGame3 game = new BirdGame3();
        Bird bat = groundedBird(game, BirdGame3.BirdType.BAT, 0, 320.0);
        Object neutralAir = enumConstant("com.example.birdgame3.Bird$NormalAttackVariant", "NEUTRAL_AIR");
        Object sideSmash = enumConstant("com.example.birdgame3.Bird$NormalAttackVariant", "SIDE_SMASH");
        Object smashProfile = invoke(bat, "normalAttackProfile", sideSmash);
        bat.batNeutralReuseTimer = 17;
        bat.batWingcutReuseTimer = 23;
        bat.batSilentReuseTimer = 29;
        setField(bat, "batRehangCooldownTimer", 31);
        setField(bat, "batAmbushWindowTimer", 41);

        invoke(bat, "performAttack", 0, neutralAir);

        assertEquals(17, bat.batNeutralReuseTimer);
        assertEquals(23, bat.batWingcutReuseTimer);
        assertEquals(29, bat.batSilentReuseTimer);
        assertEquals(31, getInt(bat, "batRehangCooldownTimer"));
        assertEquals(41, getInt(bat, "batAmbushWindowTimer"),
                "Startup must not spend Bat's ambush bonus before the wing arc becomes active.");
        invoke(bat, "updateTimers", 1.0);
        invoke(bat, "updateTimers", 1.0);
        assertEquals(0, getInt(bat, "batAmbushWindowTimer"));
        assertEquals(0, bat.batEchoTimer);
        assertEquals(0, bat.batWingcutTimer);
        assertEquals(0, bat.batMoonriseTimer);
        assertEquals(0, bat.batSilentDiveTimer);
        assertTrue((double) invoke(smashProfile, "horizontalLaunchScale") <= 1.48);
        assertTrue((int) invoke(smashProfile, "cooldownFrames") >= 34);
    }

    @Test
    void batWingAnimationDistinguishesOrbitFromSilentDrop() throws Exception {
        BirdGame3 game = new BirdGame3();
        Bird bat = groundedBird(game, BirdGame3.BirdType.BAT, 0, 320.0);
        bat.y -= 120.0;
        Object neutralAir = enumConstant("com.example.birdgame3.Bird$NormalAttackVariant", "NEUTRAL_AIR");
        Object downAir = enumConstant("com.example.birdgame3.Bird$NormalAttackVariant", "DOWN_AIR");

        setField(bat, "activeAttackVariant", neutralAir);
        setField(bat, "attackAnimationTimer", 10);
        double orbitOpenness = (double) invoke(bat, "batWingOpenness", true);
        setField(bat, "activeAttackVariant", downAir);
        double dropOpenness = (double) invoke(bat, "batWingOpenness", true);

        assertTrue(orbitOpenness >= 0.96, "Wing Orbit should fully spread both wings");
        assertTrue(dropOpenness <= 0.20, "Silent Drop should tuck both wings");
        assertTrue(orbitOpenness - dropOpenness >= 0.70);
    }

    @Test
    void batLanStatePreservesAmbushHitsAndEchoGeometry() throws Exception {
        BirdGame3 sourceGame = new BirdGame3();
        sourceGame.activePlayers = 2;
        Bird source = groundedBird(sourceGame, BirdGame3.BirdType.BAT, 0, 320.0);
        Bird sourceTarget = groundedBird(sourceGame, BirdGame3.BirdType.PIGEON, 1, 760.0);
        sourceGame.players[0] = source;
        sourceGame.players[1] = sourceTarget;
        source.batWingcutHit[1] = true;
        source.batMoonriseHit[0] = true;
        source.batSilentHit[1] = true;
        source.batWingcutAmbush = true;
        source.batMoonriseAmbush = true;
        source.batSilentAmbush = true;
        setField(source, "batAmbushWindowTimer", 37);
        setField(source, "batEchoFxStartX", 412.25);
        setField(source, "batEchoFxStartY", 511.5);
        setField(source, "batEchoFxMidX", 612.75);
        setField(source, "batEchoFxMidY", 455.25);
        setField(source, "batEchoFxEndX", 813.0);
        setField(source, "batEchoFxEndY", 520.75);
        setField(source, "batEchoFxBounced", true);
        setField(source, "batEchoFxUltimate", true);

        LanBirdState snapshot = source.toLanState();

        BirdGame3 remoteGame = new BirdGame3();
        remoteGame.activePlayers = 2;
        Bird remote = groundedBird(remoteGame, BirdGame3.BirdType.BAT, 0, 320.0);
        Bird remoteTarget = groundedBird(remoteGame, BirdGame3.BirdType.PIGEON, 1, 760.0);
        remoteGame.players[0] = remote;
        remoteGame.players[1] = remoteTarget;
        remote.applyLanState(snapshot);

        assertTrue(remote.batWingcutHit[1]);
        assertTrue(remote.batMoonriseHit[0]);
        assertTrue(remote.batSilentHit[1]);
        assertTrue(remote.batWingcutAmbush);
        assertTrue(remote.batMoonriseAmbush);
        assertTrue(remote.batSilentAmbush);
        assertEquals(37, getInt(remote, "batAmbushWindowTimer"));
        assertEquals(412.25, getDouble(remote, "batEchoFxStartX"), 0.001);
        assertEquals(511.5, getDouble(remote, "batEchoFxStartY"), 0.001);
        assertEquals(612.75, getDouble(remote, "batEchoFxMidX"), 0.001);
        assertEquals(455.25, getDouble(remote, "batEchoFxMidY"), 0.001);
        assertEquals(813.0, getDouble(remote, "batEchoFxEndX"), 0.001);
        assertEquals(520.75, getDouble(remote, "batEchoFxEndY"), 0.001);
        assertTrue((boolean) getField(remote, "batEchoFxBounced"));
        assertTrue((boolean) getField(remote, "batEchoFxUltimate"));
    }

    @Test
    void batTransientSpecialStateSurvivesTheWireFormat() throws Exception {
        LanBirdState state = new LanBirdState();
        state.batWingcutHit[2] = true;
        state.batMoonriseHit[1] = true;
        state.batSilentHit[3] = true;
        state.batAmbushWindowTimer = 39;
        state.batCathedralWaveIndex = 4;
        state.batEchoFxStartX = 411.25;
        state.batEchoFxMidY = 512.5;
        state.batEchoFxEndX = 813.75;
        state.batEchoFxBounced = true;
        state.batEchoFxUltimate = true;

        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        state.write(new DataOutputStream(bytes));
        LanBirdState decoded = LanBirdState.read(
                new DataInputStream(new ByteArrayInputStream(bytes.toByteArray())));

        assertTrue(decoded.batWingcutHit[2]);
        assertTrue(decoded.batMoonriseHit[1]);
        assertTrue(decoded.batSilentHit[3]);
        assertEquals(39, decoded.batAmbushWindowTimer);
        assertEquals(4, decoded.batCathedralWaveIndex);
        assertEquals(411.25, decoded.batEchoFxStartX, 0.001);
        assertEquals(512.5, decoded.batEchoFxMidY, 0.001);
        assertEquals(813.75, decoded.batEchoFxEndX, 0.001);
        assertTrue(decoded.batEchoFxBounced);
        assertTrue(decoded.batEchoFxUltimate);
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

    private static double getDouble(Object target, String name) throws Exception {
        return (double) getField(target, name);
    }

    private static void setField(Object target, String name, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        field.set(target, value);
    }
}
