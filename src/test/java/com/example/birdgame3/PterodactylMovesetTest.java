package com.example.birdgame3;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.*;
import java.lang.reflect.Method;
import java.util.UUID;
import java.util.prefs.Preferences;

import static org.junit.jupiter.api.Assertions.*;

class PterodactylMovesetTest {
    private enum Input { LEFT, RIGHT, JUMP, ATTACK, SPECIAL, BLOCK, GRAB }
    private static javafx.scene.input.KeyCode key(BirdGame3 game, int slot, Input input) {
        return switch (input) {
            case LEFT -> game.leftKeyForPlayer(slot);
            case RIGHT -> game.rightKeyForPlayer(slot);
            case JUMP -> game.jumpKeyForPlayer(slot);
            case ATTACK -> game.attackKeyForPlayer(slot);
            case SPECIAL -> game.specialKeyForPlayer(slot);
            case BLOCK -> game.blockKeyForPlayer(slot);
            case GRAB -> game.grabKeyForPlayer(slot);
        };
    }
    private static boolean grabbed(Bird bird) {
        try {
            var field = Bird.class.getDeclaredField("grabbedBy"); field.setAccessible(true);
            return field.get(bird) != null;
        } catch (ReflectiveOperationException e) { throw new AssertionError(e); }
    }
    private static void defeat(Bird bird) {
        try {
            Method method = Bird.class.getDeclaredMethod("onDefeated"); method.setAccessible(true); method.invoke(bird);
        } catch (ReflectiveOperationException e) { throw new AssertionError(e); }
    }
    private final Preferences prefs = Preferences.userRoot().node("/birdfight3-tests/pterodactyl/" + UUID.randomUUID());
    @AfterEach void cleanup() throws Exception { prefs.removeNode(); prefs.flush(); }

    private BirdGame3 game() {
        BirdGame3 game = new BirdGame3(prefs);
        game.selectedMap = BirdGame3.MapType.BATTLEFIELD;
        game.platforms.add(new Platform(0, BirdGame3.GROUND_Y, BirdGame3.WORLD_WIDTH, 80));
        game.activePlayers = 2;
        game.players[0] = bird(game, BirdGame3.BirdType.PTERODACTYL, 0, 320, BirdGame3.GROUND_Y - 380);
        game.players[1] = bird(game, BirdGame3.BirdType.PIGEON, 1, 420, BirdGame3.GROUND_Y - 380);
        return game;
    }
    private static Bird bird(BirdGame3 game, BirdGame3.BirdType type, int slot, double x, double y) {
        Bird bird = new Bird(x, type, slot, game);
        bird.y = y;
        game.isAI[slot] = false;
        return bird;
    }
    private static void input(BirdGame3 game, int slot, Input action, boolean held) {
        game.setAiControlKey(slot, key(game, slot, action), held);
    }
    private static void update(BirdGame3 game) {
        for (int i = 0; i < game.activePlayers; i++) if (game.players[i] != null) game.players[i].update(1);
        game.simTick++;
    }
    private static void catchVictim(BirdGame3 game, boolean ultimate) {
        Bird pter = game.players[0], victim = game.players[1];
        pter.pterodactyl.phase = PterodactylSpecials.DIVE;
        pter.pterodactyl.direction = 1;
        pter.pterodactyl.ultimate = ultimate;
        victim.x = pter.x + 45;
        victim.y = pter.y + pter.bodyHeight() + 30;
        input(game, 0, Input.SPECIAL, true);
        update(game);
        assertEquals(PterodactylSpecials.CARRY, pter.pterodactyl.phase);
        assertSame(victim, pter.pterodactylTarget());
    }

    @Test void upAndDownSpecialInputsReserveJumpAndShieldThroughTheRealUpdatePath() {
        BirdGame3 game = game(); Bird pter = game.players[0];
        pter.y = BirdGame3.GROUND_Y - pter.bodyHeight();
        input(game, 0, Input.JUMP, true); input(game, 0, Input.SPECIAL, true);
        update(game);
        assertEquals(PterodactylSpecials.UPDRAFT, pter.pterodactyl.phase);
        game = game(); pter = game.players[0]; pter.y = BirdGame3.GROUND_Y - pter.bodyHeight();
        input(game, 0, Input.BLOCK, true); input(game, 0, Input.SPECIAL, true);
        update(game);
        assertEquals(PterodactylSpecials.WINDUP, pter.pterodactyl.phase);
        assertFalse(pter.isBlocking);
    }

    @Test void groundedSnatchClimbsBeforeDivingAndCanCatchANearbyOpponent() {
        BirdGame3 game = game(); Bird pter = game.players[0], victim = game.players[1];
        pter.y = victim.y = BirdGame3.GROUND_Y - pter.bodyHeight(); victim.x = pter.x + 90;
        input(game, 0, Input.BLOCK, true); input(game, 0, Input.SPECIAL, true);
        update(game);
        assertTrue(pter.pterodactyl.groundLaunch);
        input(game, 0, Input.BLOCK, false);
        for (int tick = 0; tick < 25 && pter.pterodactylTarget() == null; tick++) update(game);
        assertSame(victim, pter.pterodactylTarget());
        assertTrue(victim.bodyBottomY() <= BirdGame3.GROUND_Y);
    }

    @Test void cpuAvoidsSwoopingAwayFromStageSupport() {
        BirdGame3 game = game(); Bird pter = game.players[0], victim = game.players[1];
        victim.x = pter.x + 120; victim.y = pter.y + 200;
        assertTrue(PterodactylSpecials.canAISwoop(pter, victim, false));
        game.platforms.clear();
        assertFalse(PterodactylSpecials.canAISwoop(pter, victim, false));
    }

    @Test void cpuUsesTheGustLaneAndRejectsVerticalOrUnsupportedLunges() {
        BirdGame3 game = game(); Bird pter = game.players[0], victim = game.players[1];
        victim.x = pter.x + 150; victim.y = pter.y;
        assertEquals(Bird.DirectionalSpecialInput.NEUTRAL, PterodactylSpecials.aiInput(pter, victim));
        assertTrue(PterodactylSpecials.shouldAIUse(pter, victim));
        pter.pterodactyl.cooldowns[0] = 50;
        victim.x = pter.x + 200;
        assertEquals(Bird.DirectionalSpecialInput.SIDE, PterodactylSpecials.aiInput(pter, victim));
        victim.y = pter.y - 160;
        pter.pterodactyl.upUsed = true;
        assertFalse(PterodactylSpecials.shouldAIUse(pter, victim));
        victim.y = pter.y; game.platforms.clear();
        game.platforms.add(new Platform(pter.x - 80, BirdGame3.GROUND_Y, 180, 20));
        assertFalse(PterodactylSpecials.canAILunge(pter, victim));
        assertFalse(PterodactylSpecials.shouldAIUse(pter, victim));
    }

    @Test void fullUltimateMeterDoesNotDisableDirectionalSpecialsAwayFromTheCatchLane() {
        BirdGame3 game = game(); Bird pter = game.players[0], victim = game.players[1];
        pter.gainUltimateFromMinionDamage(1000);
        victim.x = pter.x + 200; victim.y = pter.y;
        assertFalse(PterodactylSpecials.canAISwoop(pter, victim, true));
        assertTrue(PterodactylSpecials.shouldAIUse(pter, victim));
        assertEquals(Bird.DirectionalSpecialInput.SIDE, PterodactylSpecials.aiInput(pter, victim));
        input(game, 0, Input.RIGHT, true); input(game, 0, Input.SPECIAL, true);
        update(game);
        assertEquals(PterodactylSpecials.LUNGE, pter.pterodactyl.phase);
        assertTrue(pter.isUltimateReady(), "Directional specials must preserve the stored ultimate");
    }

    @Test void fourInputsHaveIndependentCooldownsAndLandingRefreshesOnlyUpdraft() {
        BirdGame3 game = game(); Bird pter = game.players[0];
        PterodactylSpecials.use(pter, false);
        assertEquals(PterodactylSpecials.GUST, pter.pterodactyl.phase);
        assertEquals(70, pter.pterodactyl.cooldowns[0]);
        PterodactylSpecials.reset(pter, false);
        input(game, 0, Input.JUMP, true);
        assertTrue(BirdSpecialReadiness.canStart(pter));
        PterodactylSpecials.use(pter, false);
        assertEquals(PterodactylSpecials.UPDRAFT, pter.pterodactyl.phase);
        PterodactylSpecials.reset(pter, false);
        PterodactylSpecials.cooldowns(pter, 100);
        assertFalse(BirdSpecialReadiness.canStart(pter), "Airborne cooldown expiry must not grant a second Updraft");
        pter.y = BirdGame3.GROUND_Y - pter.bodyHeight();
        PterodactylSpecials.cooldowns(pter, 1);
        assertTrue(BirdSpecialReadiness.canStart(pter));
        input(game, 0, Input.JUMP, false);
        input(game, 0, Input.BLOCK, true);
        PterodactylSpecials.use(pter, false);
        assertEquals(PterodactylSpecials.WINDUP, pter.pterodactyl.phase);
        assertFalse(pter.isBlocking);
    }

    @Test void gustHasStartupAndLungeHitsEachOpponentOnceEvenAboveSlotThirtyOne() {
        BirdGame3 game = game(); Bird pter = game.players[0], victim = game.players[1];
        double before = victim.health;
        PterodactylSpecials.use(pter, false);
        for (int tick = 0; tick < 7; tick++) PterodactylSpecials.tick(pter, true);
        assertEquals(before, victim.health);
        PterodactylSpecials.tick(pter, true);
        assertTrue(victim.health < before);
        assertTrue(victim.vx > 0);
        PterodactylSpecials.reset(pter, false);
        Bird extra = bird(game, BirdGame3.BirdType.PIGEON, 33, 430, pter.y);
        game.players[33] = extra;
        pter.pterodactyl.phase = PterodactylSpecials.LUNGE;
        victim.stunTime = 0;
        double first = victim.health, second = extra.health;
        for (int tick = 0; tick < 18; tick++) PterodactylSpecials.tick(pter, true);
        assertTrue(victim.health < first && extra.health < second);
        assertEquals(first - victim.health, second - extra.health, 0.01);
        assertEquals(PterodactylSpecials.RECOVERY, pter.pterodactyl.phase);
    }

    @Test void swoopCapturesOneVictimAndEarlyReleaseCanThrowBackAndDown() {
        BirdGame3 game = game(); Bird pter = game.players[0], victim = game.players[1];
        game.activePlayers = 3;
        game.players[2] = bird(game, BirdGame3.BirdType.KIWI, 2, pter.x + 60, pter.y + 110);
        double before = victim.health;
        catchVictim(game, false);
        assertFalse(grabbed(game.players[2]));
        double capturedY = pter.y;
        for (int tick = 0; tick < 7; tick++) update(game);
        assertTrue(pter.y < capturedY - 20);
        input(game, 0, Input.LEFT, true);
        input(game, 0, Input.BLOCK, true);
        input(game, 0, Input.SPECIAL, false);
        update(game);
        assertNull(pter.pterodactylTarget());
        assertEquals(PterodactylSpecials.RECOVERY, pter.pterodactyl.phase);
        assertTrue(victim.health < before && victim.stunTime > 0);
        assertTrue(victim.vx < 0 && victim.vy > 0);
    }

    @Test void freshDefenderInputsEscapeWhileHoldingOneButtonDoesNotAutoEscape() {
        BirdGame3 game = game(); Bird pter = game.players[0], victim = game.players[1];
        double before = victim.health;
        catchVictim(game, false);
        for (int tick = 0; tick < 12; tick++) {
            boolean held = tick % 2 == 0;
            for (Input action : new Input[]{Input.ATTACK,
                    Input.JUMP, Input.SPECIAL, Input.BLOCK,
                    Input.GRAB, Input.LEFT, Input.RIGHT})
                input(game, 1, action, held);
            update(game);
            if (pter.pterodactylTarget() == null) break;
        }
        assertNull(pter.pterodactylTarget());
        assertEquals(before, victim.health, "Escaping must avoid the release attack");
    }

    @Test void interruptionAndDeathReleaseTheVictimWithoutDamage() {
        BirdGame3 game = game(); Bird pter = game.players[0], victim = game.players[1];
        catchVictim(game, false); double before = victim.health;
        pter.applyStun(20);
        update(game);
        assertNull(pter.pterodactylTarget());
        assertEquals(before, victim.health);
        assertEquals(PterodactylSpecials.IDLE, pter.pterodactyl.phase);
        victim.stunTime = 0;
        PterodactylSpecials.reset(pter, true);
        pter.stunTime = 0;
        // Use a fresh target; the escaped victim is protected from immediate regrab.
        game.players[1] = bird(game, BirdGame3.BirdType.PIGEON, 1, 420, pter.y + 110);
        catchVictim(game, true);
        pter.health = 0; defeat(pter);
        assertNull(pter.pterodactylTarget());
        assertFalse(grabbed(game.players[1]));
    }

    @Test void ultimateSlamsOnTheStageAndReleasesAboveTheSurface() {
        BirdGame3 game = game(); Bird pter = game.players[0], victim = game.players[1];
        catchVictim(game, true); double before = victim.health;
        for (int tick = 0; tick < 55 && pter.pterodactylTarget() != null; tick++) update(game);
        assertNull(pter.pterodactylTarget());
        assertEquals(40 * pter.type.damageDealtMult * victim.type.damageTakenMult, before - victim.health, 0.01,
                "The slam must use the real outgoing/incoming tuning multipliers");
        assertTrue(victim.bodyBottomY() <= BirdGame3.GROUND_Y + 1);
        assertTrue(victim.vy < 0 && victim.stunTime > 0);
    }

    @Test void ultimateCanFinishAgainstOrdinaryFreshInputEscapePressure() {
        BirdGame3 game = game(); Bird pter = game.players[0], victim = game.players[1];
        pter.y = BirdGame3.GROUND_Y - 190;
        catchVictim(game, true); double before = victim.health;
        Input[] mash = Input.values();
        for (int tick = 0; tick < 45 && pter.pterodactylTarget() != null; tick++) {
            for (Input action : mash) input(game, 1, action, false);
            input(game, 1, mash[tick % mash.length], true);
            update(game);
        }
        assertNull(pter.pterodactylTarget());
        assertTrue(victim.health < before, "One fresh input per tick must not prevent every supported ultimate slam");
        assertTrue(victim.bodyBottomY() <= BirdGame3.GROUND_Y + 1);
    }

    @Test void fastFreshInputMashingStillEscapesTheUltimateWithoutDamage() {
        BirdGame3 game = game(); Bird pter = game.players[0], victim = game.players[1];
        pter.y = BirdGame3.GROUND_Y - 190;
        catchVictim(game, true); double before = victim.health;
        for (int tick = 0; tick < 40 && pter.pterodactylTarget() != null; tick++) {
            for (Input action : Input.values()) input(game, 1, action, false);
            input(game, 1, tick % 2 == 0 ? Input.LEFT : Input.RIGHT, true);
            input(game, 1, tick % 2 == 0 ? Input.ATTACK : Input.BLOCK, true);
            update(game);
        }
        assertNull(pter.pterodactylTarget());
        assertEquals(before, victim.health, "Rapid fresh-input mashing must remain a counter to the ultimate");
    }

    @Test void offstageUltimateTimesOutWithoutAwardingASlamInMidair() {
        BirdGame3 game = game(); Bird pter = game.players[0], victim = game.players[1];
        catchVictim(game, true); double before = victim.health;
        game.platforms.clear();
        for (int tick = 0; tick < 55 && pter.pterodactylTarget() != null; tick++) {
            PterodactylSpecials.tick(pter, true);
            pter.y += pter.vy;
            PterodactylSpecials.postMove(pter, pter.x, pter.y - pter.vy);
        }
        assertNull(pter.pterodactylTarget());
        assertEquals(before, victim.health);
        assertEquals(PterodactylSpecials.RECOVERY, pter.pterodactyl.phase);
    }

    @Test void missesHaveRecoveryAndProtectedTargetsCannotBeCaptured() {
        BirdGame3 game = game(); Bird pter = game.players[0], victim = game.players[1];
        victim.health = 0;
        assertFalse(pter.pterodactylCanGrab(victim));
        victim.health = 100; victim.penguinAbsoluteZeroTimer = 30;
        assertFalse(pter.pterodactylCanGrab(victim));
        victim.penguinAbsoluteZeroTimer = 0;
        game.platforms.clear(); victim.x = pter.x + 900;
        pter.pterodactyl.phase = PterodactylSpecials.DIVE;
        for (int tick = 0; tick < 25; tick++) PterodactylSpecials.tick(pter, true);
        assertEquals(PterodactylSpecials.RECOVERY, pter.pterodactyl.phase);
        assertFalse(BirdSpecialReadiness.canStart(pter));
    }

    @Test void unlimitedFlightRisesAndWideWingsTurnMoreSlowlyThanBat() throws Exception {
        BirdGame3 game = game(); Bird pter = game.players[0];
        input(game, 0, Input.JUMP, true);
        for (int tick = 0; tick < 12; tick++) update(game);
        assertTrue(pter.vy < 0);
        Method limited = Bird.class.getDeclaredMethod("hasLimitedFlight"); limited.setAccessible(true);
        assertFalse((boolean) limited.invoke(pter));
        input(game, 0, Input.JUMP, false);
        Bird bat = bird(game, BirdGame3.BirdType.BAT, 1, pter.x + 200, pter.y);
        game.players[1] = bat;
        pter.vx = bat.vx = 0;
        input(game, 0, Input.RIGHT, true);
        input(game, 1, Input.RIGHT, true);
        update(game);
        assertTrue(pter.vx > 0 && pter.vx < bat.vx);
    }

    @Test void networkRoundTripAndHashCoverEveryNewStateField() throws Exception {
        BirdGame3 game = game(); Bird pter = game.players[0];
        catchVictim(game, true);
        pter.pterodactyl.upUsed = true; pter.pterodactyl.cooldowns[3] = 92;
        pter.pterodactyl.cooldowns[0] = 32; pter.pterodactyl.cooldowns[1] = 11; pter.pterodactyl.cooldowns[2] = 4;
        pter.pterodactyl.groundLaunch = true; pter.pterodactyl.direction = -1;
        pter.pterodactyl.elapsed = 5; pter.pterodactyl.carryStartY = 123.5;
        pter.pterodactyl.hitMask = 1L << 33;
        LanBirdState state = pter.toLanState();
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        state.write(new DataOutputStream(bytes));
        LanBirdState decoded = LanBirdState.read(new DataInputStream(new ByteArrayInputStream(bytes.toByteArray())));
        assertEquals(state.pterodactyl.hash(), decoded.pterodactyl.hash());
        Bird restored = bird(game, BirdGame3.BirdType.PTERODACTYL, 0, 100, 100);
        restored.applyLanState(decoded);
        assertEquals(pter.pterodactyl.hash(), restored.pterodactyl.hash());
        Method hash = BirdGame3.class.getDeclaredMethod("computeLockstepHash", long.class); hash.setAccessible(true);
        long first = (long) hash.invoke(game, game.simTick);
        pter.pterodactyl.cooldowns[0]++;
        assertNotEquals(first, (long) hash.invoke(game, game.simTick));
        PterodactylSpecials.State invalid = new PterodactylSpecials.State(); invalid.cooldowns[0] = -1;
        bytes.reset(); invalid.write(new DataOutputStream(bytes));
        assertThrows(IOException.class, () -> new PterodactylSpecials.State().read(
                new DataInputStream(new ByteArrayInputStream(bytes.toByteArray()))));
    }
}
