package com.example.birdgame3;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;

class TitmouseMovesetIdentityTest {
    @Test
    void titmouseHasAnAuthoredCompleteWoodlandSkirmisherKit() throws Exception {
        BirdGame3 game = new BirdGame3();
        Bird titmouse = groundedBird(game, BirdGame3.BirdType.TITMOUSE, 0, 320.0);
        Bird sharedBird = groundedBird(game, BirdGame3.BirdType.BAT, 1, 320.0);
        Bird hummingbird = groundedBird(game, BirdGame3.BirdType.HUMMINGBIRD, 2, 320.0);

        int distinctFromShared = 0;
        int distinctFromHummingbird = 0;
        for (Object variant : normalAttackVariantClass().getEnumConstants()) {
            Object profile = invoke(titmouse, "normalAttackProfile", variant);
            if (!profile.equals(invoke(sharedBird, "normalAttackProfile", variant))) distinctFromShared++;
            if (!profile.equals(invoke(hummingbird, "normalAttackProfile", variant))) distinctFromHummingbird++;
            String moveName = (String) invoke(titmouse, "normalAttackTelemetryName", variant);
            assertTrue(moveName.startsWith("Titmouse "));
            assertFalse(moveName.contains("Normal Attack"));
            assertFalse(moveName.contains("Tilt"));
        }

        assertEquals(15, distinctFromShared);
        assertEquals(15, distinctFromHummingbird);
    }

    @Test
    void titmousePummelIsTheFastLowDamageSeedTap() throws Exception {
        BirdGame3 game = new BirdGame3();
        game.activePlayers = 2;
        Bird titmouse = groundedBird(game, BirdGame3.BirdType.TITMOUSE, 0, 320.0);
        Bird target = groundedBird(game, BirdGame3.BirdType.PIGEON, 1, 385.0);
        game.players[0] = titmouse;
        game.players[1] = target;
        linkGrab(titmouse, target);
        game.setAiControlKey(0, game.attackKeyForPlayer(0), true);

        double healthBefore = target.health;
        assertTrue((boolean) invoke(titmouse, "handleHoldingGrabState", false, false));

        double tunedDamage = 2.0 * titmouse.type.damageDealtMult * target.type.damageTakenMult;
        assertEquals(tunedDamage, healthBefore - target.health, 0.001);
        assertSame(target, getField(titmouse, "grabbedTarget"));
        assertSame(titmouse, getField(target, "grabbedBy"));
        assertEquals(7, getInt(titmouse, "grabThrowLockTimer"));
        assertEquals(54, getInt(titmouse, "grabHoldTimer"));
    }

    @Test
    void titmouseThrowsControlBranchesCanopyAndSeedStashes() throws Exception {
        BirdGame3 game = new BirdGame3();
        Bird titmouse = groundedBird(game, BirdGame3.BirdType.TITMOUSE, 0, 320.0);

        Object forward = throwProfile(titmouse, "FORWARD");
        Object back = throwProfile(titmouse, "BACK");
        Object up = throwProfile(titmouse, "UP");
        Object down = throwProfile(titmouse, "DOWN");

        assertTrue((double) invoke(forward, "launchX") >= 17.5);
        assertTrue((double) invoke(back, "launchX") <= -18.4);
        assertTrue((double) invoke(up, "launchY") < (double) invoke(back, "launchY"));
        assertTrue((int) invoke(down, "damage") > (int) invoke(up, "damage"));
        assertEquals("Titmouse Cache Cast", throwName(titmouse, "FORWARD"));
        assertEquals("Titmouse Branch Reversal", throwName(titmouse, "BACK"));
        assertEquals("Titmouse Canopy Pop", throwName(titmouse, "UP"));
        assertEquals("Titmouse Seed Press", throwName(titmouse, "DOWN"));
    }

    @Test
    void titmouseGroundedAttackPosesStayFloorSafeAndReadable() throws Exception {
        BirdGame3 game = new BirdGame3();
        Bird titmouse = groundedBird(game, BirdGame3.BirdType.TITMOUSE, 0, 320.0);
        String[] groundedVariants = {
                "NEUTRAL", "SIDE_TILT", "UP_TILT", "DOWN_TILT",
                "SIDE_SMASH", "UP_SMASH", "DOWN_SMASH",
                "DASH_ATTACK", "LEDGE_ATTACK", "GETUP_ATTACK"
        };

        for (String variantName : groundedVariants) {
            Object variant = enumConstant("com.example.birdgame3.Bird$NormalAttackVariant", variantName);
            Object pose = invoke(titmouse, "currentTitmouseNormalAttackPose", variant, 1.0);
            assertTrue((double) invoke(pose, "translateY") <= 0.0,
                    variantName + " must not sink Titmouse below the stage");
            assertTrue(Math.abs((double) invoke(pose, "bodyRotationDegrees")) <= 12.0,
                    variantName + " must preserve the tiny bird's readable silhouette");
        }
    }

    @Test
    void titmouseNormalsDoNotMutateSpecialRoutesAndKeepLaunchSafe() throws Exception {
        BirdGame3 game = new BirdGame3();
        Bird titmouse = groundedBird(game, BirdGame3.BirdType.TITMOUSE, 0, 320.0);
        Object neutralAir = enumConstant("com.example.birdgame3.Bird$NormalAttackVariant", "NEUTRAL_AIR");
        Object sideSmash = enumConstant("com.example.birdgame3.Bird$NormalAttackVariant", "SIDE_SMASH");
        Object smashProfile = invoke(titmouse, "normalAttackProfile", sideSmash);
        titmouse.titmouseScoldReuseTimer = 17;
        titmouse.titmouseBarkskipReuseTimer = 23;
        titmouse.titmouseVaultReuseTimer = 29;
        titmouse.titmouseStashReuseTimer = 31;
        titmouse.titmouseMobbingNodeIndex = 2;

        invoke(titmouse, "performAttack", 0, neutralAir);

        assertEquals(17, titmouse.titmouseScoldReuseTimer);
        assertEquals(23, titmouse.titmouseBarkskipReuseTimer);
        assertEquals(29, titmouse.titmouseVaultReuseTimer);
        assertEquals(31, titmouse.titmouseStashReuseTimer);
        assertEquals(2, titmouse.titmouseMobbingNodeIndex);
        assertEquals(0, titmouse.titmouseScoldTimer);
        assertEquals(0, titmouse.titmouseBarkskipTimer);
        assertEquals(0, titmouse.titmouseVaultTimer);
        assertFalse(titmouse.titmouseStashCharging);
        assertTrue((double) invoke(smashProfile, "horizontalLaunchScale") <= 1.48);
        assertTrue((int) invoke(smashProfile, "cooldownFrames") >= 34);
    }

    @Test
    void titmouseLanStatePreservesHitsStashesAndMobbingTargets() {
        BirdGame3 sourceGame = new BirdGame3();
        sourceGame.activePlayers = 2;
        Bird source = groundedBird(sourceGame, BirdGame3.BirdType.TITMOUSE, 0, 320.0);
        Bird sourceTarget = groundedBird(sourceGame, BirdGame3.BirdType.PIGEON, 1, 760.0);
        sourceGame.players[0] = source;
        sourceGame.players[1] = sourceTarget;
        source.titmouseScoldHit[1] = true;
        source.titmouseBarkskipHit[0] = true;
        source.titmouseVaultHit[1] = true;
        Bird.TitmouseSeedStash stash = new Bird.TitmouseSeedStash(515.5, 901.25, true);
        stash.lifeFrames = 177;
        stash.ageFrames = 43;
        source.titmouseSeedStashes.add(stash);
        source.titmouseMobbingNodes.add(new Bird.TitmouseMobbingNode(515.5, 820.25, null));
        source.titmouseMobbingNodes.add(new Bird.TitmouseMobbingNode(760.0, 850.0, sourceTarget));
        source.titmouseMobbingNodeIndex = 1;

        LanBirdState snapshot = source.toLanState();

        BirdGame3 remoteGame = new BirdGame3();
        remoteGame.activePlayers = 2;
        Bird remote = groundedBird(remoteGame, BirdGame3.BirdType.TITMOUSE, 0, 320.0);
        Bird remoteTarget = groundedBird(remoteGame, BirdGame3.BirdType.PIGEON, 1, 760.0);
        remoteGame.players[0] = remote;
        remoteGame.players[1] = remoteTarget;
        remote.applyLanState(snapshot);

        assertTrue(remote.titmouseScoldHit[1]);
        assertTrue(remote.titmouseBarkskipHit[0]);
        assertTrue(remote.titmouseVaultHit[1]);
        assertEquals(1, remote.titmouseSeedStashes.size());
        assertEquals(515.5, remote.titmouseSeedStashes.getFirst().x, 0.001);
        assertEquals(177, remote.titmouseSeedStashes.getFirst().lifeFrames);
        assertEquals(43, remote.titmouseSeedStashes.getFirst().ageFrames);
        assertTrue(remote.titmouseSeedStashes.getFirst().ultimate);
        assertEquals(2, remote.titmouseMobbingNodes.size());
        assertNull(remote.titmouseMobbingNodes.getFirst().target());
        assertSame(remoteTarget, remote.titmouseMobbingNodes.get(1).target());
        assertEquals(1, remote.titmouseMobbingNodeIndex);
    }

    @Test
    void titmouseTransientSpecialStateSurvivesTheWireFormat() throws Exception {
        LanBirdState state = new LanBirdState();
        state.titmouseScoldHitSync[2] = true;
        state.titmouseBarkskipHitSync[1] = true;
        state.titmouseVaultHitSync[3] = true;
        state.titmouseSeedStashCount = 2;
        state.titmouseSeedStashX[0] = 412.25;
        state.titmouseSeedStashY[0] = 911.5;
        state.titmouseSeedStashLifeFrames[0] = 177;
        state.titmouseSeedStashAgeFrames[0] = 43;
        state.titmouseSeedStashUltimateSync[0] = true;
        state.titmouseMobbingNodeCount = 2;
        state.titmouseMobbingNodeX[1] = 712.75;
        state.titmouseMobbingNodeY[1] = 655.5;
        state.titmouseMobbingNodeTargetIndex[1] = 3;

        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        state.write(new DataOutputStream(bytes));
        LanBirdState decoded = LanBirdState.read(
                new DataInputStream(new ByteArrayInputStream(bytes.toByteArray())));

        assertTrue(decoded.titmouseScoldHitSync[2]);
        assertTrue(decoded.titmouseBarkskipHitSync[1]);
        assertTrue(decoded.titmouseVaultHitSync[3]);
        assertEquals(2, decoded.titmouseSeedStashCount);
        assertEquals(412.25, decoded.titmouseSeedStashX[0], 0.001);
        assertEquals(911.5, decoded.titmouseSeedStashY[0], 0.001);
        assertEquals(177, decoded.titmouseSeedStashLifeFrames[0]);
        assertEquals(43, decoded.titmouseSeedStashAgeFrames[0]);
        assertTrue(decoded.titmouseSeedStashUltimateSync[0]);
        assertEquals(2, decoded.titmouseMobbingNodeCount);
        assertEquals(712.75, decoded.titmouseMobbingNodeX[1], 0.001);
        assertEquals(655.5, decoded.titmouseMobbingNodeY[1], 0.001);
        assertEquals(3, decoded.titmouseMobbingNodeTargetIndex[1]);
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
