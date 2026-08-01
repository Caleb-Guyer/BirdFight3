package com.example.birdgame3;

import javafx.scene.input.KeyCode;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class BirdStateTest {
    @Test
    void ultimateVisualReadyAllowsMockingbirdFallbackUlt() {
        BirdGame3 game = new BirdGame3();

        Bird pigeon = new Bird(100, BirdGame3.BirdType.PIGEON, 0, game);
        assertFalse(pigeon.isUltimateVisualReady());

        pigeon.refillTrainingResources(true);
        assertTrue(pigeon.isUltimateVisualReady());

        pigeon.health = 0;
        assertFalse(pigeon.isUltimateVisualReady());

        Bird mockingbird = new Bird(160, BirdGame3.BirdType.MOCKINGBIRD, 1, game);
        mockingbird.refillTrainingResources(true);
        assertTrue(mockingbird.isUltimateVisualReady(),
                "Mockingbird should glow on an empty neutral copy because Shadow Court falls back to the closest bird.");

        mockingbird.mockingbirdCapturedType = BirdGame3.BirdType.PIGEON;
        assertTrue(mockingbird.isUltimateVisualReady());
    }

    @Test
    void pigeonUltimateStartsCoronationInsteadOfBoostedSpecial() {
        BirdGame3 game = new BirdGame3();
        game.activePlayers = 1;

        Bird pigeon = new Bird(100, BirdGame3.BirdType.PIGEON, 0, game);
        game.players[0] = pigeon;
        pigeon.refillTrainingResources(true);

        BirdSpecialSystem.useSpecial(pigeon);

        assertTrue(pigeon.pigeonCoronationActive);
        assertEquals(Bird.PIGEON_CORONATION_FRAMES, pigeon.pigeonCoronationTimer);
        assertEquals(0, pigeon.pigeonFeatherBurstTimer);
        assertEquals(0, pigeon.pigeonRushTimer);
        assertEquals(0, pigeon.pigeonFlutterTimer);
        assertEquals(0, pigeon.pigeonScavengeTimer);
        assertFalse(pigeon.isUltimateReady());
    }

    @Test
    void pigeonCoronationTicksAndFinalLaunchesTargetsInZone() {
        BirdGame3 game = new BirdGame3();
        game.activePlayers = 2;

        Bird pigeon = new Bird(100, BirdGame3.BirdType.PIGEON, 0, game);
        Bird target = new Bird(190, BirdGame3.BirdType.EAGLE, 1, game);
        pigeon.y = BirdGame3.GROUND_Y - 80.0;
        target.y = BirdGame3.GROUND_Y - 80.0;
        game.players[0] = pigeon;
        game.players[1] = target;
        pigeon.refillTrainingResources(true);

        BirdSpecialSystem.useSpecial(pigeon);
        double startingHealth = target.health;

        pigeon.update(1.0);
        assertTrue(target.health <= startingHealth - Bird.PIGEON_CORONATION_TICK_DAMAGE);

        for (int i = 0; i < Bird.PIGEON_CORONATION_FRAMES; i++) {
            pigeon.update(1.0);
        }

        assertFalse(pigeon.pigeonCoronationActive);
        assertEquals(0, pigeon.pigeonCoronationTimer);
        assertTrue(target.health <= startingHealth
                - Bird.PIGEON_CORONATION_TICK_DAMAGE
                - Bird.PIGEON_CORONATION_FINAL_DAMAGE);
        assertTrue(target.vy < -0.1 || Math.abs(target.vx) > 0.1);
    }

    @Test
    void eagleUltimateStartsSkySovereignInsteadOfBoostedRaptorSpecial() {
        BirdGame3 game = new BirdGame3();
        game.activePlayers = 2;

        Bird eagle = new Bird(100, BirdGame3.BirdType.EAGLE, 0, game);
        Bird target = new Bird(190, BirdGame3.BirdType.PIGEON, 1, game);
        eagle.y = BirdGame3.GROUND_Y - 80.0;
        target.y = BirdGame3.GROUND_Y - 80.0;
        game.players[0] = eagle;
        game.players[1] = target;
        eagle.refillTrainingResources(true);

        BirdSpecialSystem.useSpecial(eagle);

        assertTrue(eagle.eagleSkySovereignActive);
        assertFalse(eagle.eagleSkySovereignDiving);
        assertEquals(Bird.EAGLE_SKY_SOVEREIGN_TARGET_FRAMES, eagle.eagleSkySovereignTimer);
        assertEquals(0, eagle.raptorCryTimer);
        assertEquals(0, eagle.raptorRushTimer);
        assertEquals(0, eagle.raptorClimbTimer);
        assertFalse(eagle.eagleDiveActive);
        assertFalse(eagle.isUltimateReady());
    }

    @Test
    void eagleSkySovereignImpactsTargetZoneAfterTargeting() {
        BirdGame3 game = new BirdGame3();
        game.activePlayers = 2;

        Bird eagle = new Bird(100, BirdGame3.BirdType.EAGLE, 0, game);
        Bird target = new Bird(190, BirdGame3.BirdType.PIGEON, 1, game);
        eagle.y = BirdGame3.GROUND_Y - 80.0;
        target.y = BirdGame3.GROUND_Y - 80.0;
        game.players[0] = eagle;
        game.players[1] = target;
        eagle.refillTrainingResources(true);

        BirdSpecialSystem.useSpecial(eagle);
        double startingHealth = target.health;

        for (int i = 0; i < Bird.EAGLE_SKY_SOVEREIGN_TARGET_FRAMES
                + Bird.EAGLE_SKY_SOVEREIGN_DIVE_FRAMES + 6; i++) {
            eagle.update(1.0);
        }

        assertFalse(eagle.eagleSkySovereignActive);
        assertTrue(target.health <= startingHealth - Bird.EAGLE_SKY_SOVEREIGN_DAMAGE);
        assertTrue(target.vy < -1.0 || Math.abs(target.vx) > 1.0);
        assertFalse(eagle.isUltimateReady());
    }

    @Test
    void falconUltimateStartsTerminalVelocityInsteadOfBoostedRaptorSpecial() {
        BirdGame3 game = new BirdGame3();
        game.activePlayers = 2;

        Bird falcon = new Bird(100, BirdGame3.BirdType.FALCON, 0, game);
        Bird target = new Bird(240, BirdGame3.BirdType.PIGEON, 1, game);
        falcon.y = BirdGame3.GROUND_Y - 80.0;
        target.y = BirdGame3.GROUND_Y - 80.0;
        falcon.facingRight = true;
        game.players[0] = falcon;
        game.players[1] = target;
        falcon.refillTrainingResources(true);

        BirdSpecialSystem.useSpecial(falcon);

        assertTrue(falcon.falconTerminalVelocityActive);
        assertFalse(falcon.falconTerminalVelocityStriking);
        assertEquals(Bird.FALCON_TERMINAL_VELOCITY_WARNING_FRAMES, falcon.falconTerminalVelocityTimer);
        assertEquals(0, falcon.raptorCryTimer);
        assertEquals(0, falcon.raptorRushTimer);
        assertEquals(0, falcon.raptorClimbTimer);
        assertFalse(falcon.eagleDiveActive);
        assertFalse(falcon.isUltimateReady());
    }

    @Test
    void falconTerminalVelocitySweetspotsMarkedTarget() {
        BirdGame3 game = new BirdGame3();
        game.activePlayers = 2;

        Bird falcon = new Bird(100, BirdGame3.BirdType.FALCON, 0, game);
        Bird target = new Bird(260, BirdGame3.BirdType.PIGEON, 1, game);
        falcon.y = BirdGame3.GROUND_Y - 80.0;
        target.y = BirdGame3.GROUND_Y - 80.0;
        falcon.facingRight = true;
        game.players[0] = falcon;
        game.players[1] = target;
        falcon.refillTrainingResources(true);

        BirdSpecialSystem.useSpecial(falcon);
        double startingHealth = target.health;

        for (int i = 0; i < Bird.FALCON_TERMINAL_VELOCITY_WARNING_FRAMES
                + Bird.FALCON_TERMINAL_VELOCITY_STRIKE_FRAMES + 4; i++) {
            falcon.update(1.0);
        }

        assertFalse(falcon.falconTerminalVelocityActive);
        assertTrue(target.health <= startingHealth - Bird.FALCON_TERMINAL_VELOCITY_SWEETSPOT_DAMAGE);
        assertTrue(target.vx > 10.0);
        assertTrue(target.vy < -8.0);
    }

    @Test
    void defeatedBirdCancelsLingeringFrenzyWithoutReviving() throws Exception {
        BirdGame3 game = new BirdGame3();
        game.activePlayers = 2;

        Bird hummingbird = new Bird(100, BirdGame3.BirdType.HUMMINGBIRD, 0, game);
        Bird target = new Bird(150, BirdGame3.BirdType.PIGEON, 1, game);
        game.players[0] = hummingbird;
        game.players[1] = target;

        setPrivateInt(hummingbird, "hummingFrenzyTimer", 90);
        hummingbird.health = 0;

        hummingbird.update(1.0);

        assertEquals(0.0, hummingbird.health, 0.0001);
        assertEquals(Bird.STARTING_HEALTH, target.health, 0.0001);
        assertEquals(0, getPrivateInt(hummingbird, "hummingFrenzyTimer"));
    }

    @Test
    void hummingbirdUltimateStartsNeedleheartOverdrive() throws Exception {
        BirdGame3 game = new BirdGame3();
        game.activePlayers = 2;

        Bird hummingbird = new Bird(180.0, BirdGame3.BirdType.HUMMINGBIRD, 0, game);
        Bird target = new Bird(300.0, BirdGame3.BirdType.PIGEON, 1, game);
        hummingbird.y = BirdGame3.GROUND_Y - 120.0;
        target.y = BirdGame3.GROUND_Y - 120.0;
        game.players[0] = hummingbird;
        game.players[1] = target;
        hummingbird.refillTrainingResources(true);

        BirdSpecialSystem.useSpecial(hummingbird);
        double startingHealth = target.health;

        assertEquals(Bird.HUMMING_NEEDLEHEART_TOTAL_FRAMES, getPrivateInt(hummingbird, "hummingFrenzyTimer"));
        assertEquals(0, getPrivateInt(hummingbird, "hummingNeedleHitTimer"));
        assertFalse(hummingbird.isUltimateReady());

        for (int i = 0; i < Bird.HUMMING_NEEDLEHEART_FINAL_FRAME + 12; i++) {
            hummingbird.update(1.0);
        }

        assertTrue(target.health < startingHealth);
        assertTrue(Math.abs(target.vx) > 8.0);
        assertTrue(target.vy < -8.0);
        assertTrue(getPrivateInt(target, "hummingNectarCoatedTimer") > 0);
    }

    @Test
    void hummingbirdReuseLockoutsStayInvisible() throws Exception {
        BirdGame3 game = new BirdGame3();
        game.activePlayers = 1;

        Bird hummingbird = new Bird(180.0, BirdGame3.BirdType.HUMMINGBIRD, 0, game);
        hummingbird.y = BirdGame3.GROUND_Y - 80.0;
        game.players[0] = hummingbird;

        KeyCode specialKey = game.specialKeyForPlayer(0);
        game.setLocalActionsForKey(specialKey, true);
        hummingbird.update(1.0);
        game.setLocalActionsForKey(specialKey, false);
        hummingbird.update(1.0);

        assertEquals(0, hummingbird.specialCooldown);
        assertTrue(getPrivateInt(hummingbird, "hummingNeedleReuseTimer") > 0);

        game.setLocalActionsForKey(specialKey, true);
        hummingbird.update(1.0);

        assertEquals(0, hummingbird.specialCooldown,
                "Hummingbird specials should use invisible per-move reuse gates.");
        assertEquals(0, hummingbird.cooldownFlash,
                "Hummingbird reuse lockouts should not display the cooldown warning.");
    }

    @Test
    void hummingbirdUpSpecialIsOnlyAnUpwardBurst() throws Exception {
        BirdGame3 game = new BirdGame3();
        game.activePlayers = 2;

        Bird hummingbird = new Bird(220.0, BirdGame3.BirdType.HUMMINGBIRD, 0, game);
        Bird target = new Bird(224.0, BirdGame3.BirdType.PIGEON, 1, game);
        hummingbird.y = BirdGame3.GROUND_Y - 80.0;
        target.y = BirdGame3.GROUND_Y - 80.0;
        game.players[0] = hummingbird;
        game.players[1] = target;

        double startingHealth = target.health;
        invokePrivateBooleanVoid(hummingbird, "specialHummingbirdHoverBurst", false);
        hummingbird.update(1.0);

        assertEquals(0, hummingbird.specialCooldown);
        assertTrue(hummingbird.vy < -20.0,
                "Hover Burst should be an extreme vertical launch.");
        assertEquals(startingHealth, target.health, 0.0001,
                "Hover Burst should not deal damage.");
    }

    @Test
    void hummingbirdNectarTrapCoatsTargetsAfterTheyLeaveTheFlower() throws Exception {
        BirdGame3 game = new BirdGame3();
        game.activePlayers = 2;

        Bird hummingbird = new Bird(300.0, BirdGame3.BirdType.HUMMINGBIRD, 0, game);
        Bird target = new Bird(242.0, BirdGame3.BirdType.PIGEON, 1, game);
        hummingbird.y = BirdGame3.GROUND_Y - 80.0;
        target.y = BirdGame3.GROUND_Y - 80.0;
        hummingbird.facingRight = true;
        game.players[0] = hummingbird;
        game.players[1] = target;

        invokePrivateBooleanVoid(hummingbird, "specialHummingbirdNectarTrap", false);
        for (int i = 0; i < 60; i++) {
            hummingbird.update(1.0);
        }

        assertEquals(0, hummingbird.specialCooldown);
        assertTrue(getPrivateInt(target, "hummingNectarCoatedTimer") > 0,
                "Stepping into the flower should coat the target in nectar.");
        assertTrue(getPrivateInt(target, "hummingNectarCoatedTimer") <= 100,
                "Nectar coating should use the nerfed shorter poison duration.");

        double healthAfterFlower = target.health;
        target.x += 260.0;
        target.vx = 9.0;
        for (int i = 0; i < 3; i++) {
            target.update(1.0);
        }

        assertTrue(getPrivateInt(target, "hummingNectarCoatedTimer") > 0,
                "Nectar should remain on the target briefly after leaving the flower.");
        assertTrue(target.health < healthAfterFlower,
                "The visible nectar coating should keep dealing damage after the target exits the trap.");
    }

    @Test
    void hummingbirdNectarCoatingOnlyAppliesPoisonDamage() throws Exception {
        BirdGame3 game = new BirdGame3();
        game.activePlayers = 2;

        Bird hummingbird = new Bird(300.0, BirdGame3.BirdType.HUMMINGBIRD, 0, game);
        Bird target = new Bird(360.0, BirdGame3.BirdType.PIGEON, 1, game);
        game.players[0] = hummingbird;
        game.players[1] = target;

        setPrivateInt(target, "hummingNectarCoatedTimer", 60);
        setPrivateInt(target, "hummingNectarCoatedDamageCooldown", 8);
        target.vx = 9.0;
        target.vy = 4.0;

        invokePrivateVoid(target, "handleHummingbirdNectarCoating");

        assertEquals(9.0, target.vx, 0.0001,
                "Hummingbird nectar should not slow horizontal movement.");
        assertEquals(4.0, target.vy, 0.0001,
                "Hummingbird nectar should not slow vertical movement.");
    }

    @Test
    void turkeyNeutralSpecialUsesInvisibleReuseTimer() throws Exception {
        BirdGame3 game = new BirdGame3();
        game.activePlayers = 1;

        Bird turkey = new Bird(180.0, BirdGame3.BirdType.TURKEY, 0, game);
        turkey.y = BirdGame3.GROUND_Y - 80.0;
        game.players[0] = turkey;

        KeyCode specialKey = game.specialKeyForPlayer(0);
        game.setLocalActionsForKey(specialKey, true);
        turkey.update(1.0);
        game.setLocalActionsForKey(specialKey, false);
        turkey.update(1.0);

        assertEquals(0, turkey.specialCooldown);
        assertTrue(getPrivateInt(turkey, "turkeyGobbleReuseTimer") > 0);

        game.setLocalActionsForKey(specialKey, true);
        turkey.update(1.0);

        assertEquals(0, turkey.specialCooldown,
                "Turkey's 4-special kit should use invisible per-move reuse timers.");
        assertEquals(0, turkey.cooldownFlash,
                "Turkey reuse lockouts should not show the cooldown warning.");
    }

    @Test
    void turkeyNeutralSpecialChargesBeforeAttackingOnRelease() throws Exception {
        BirdGame3 game = new BirdGame3();
        game.activePlayers = 2;

        Bird turkey = new Bird(180.0, BirdGame3.BirdType.TURKEY, 0, game);
        Bird target = new Bird(244.0, BirdGame3.BirdType.PIGEON, 1, game);
        turkey.y = BirdGame3.GROUND_Y - 80.0;
        target.y = BirdGame3.GROUND_Y - 80.0;
        game.players[0] = turkey;
        game.players[1] = target;

        KeyCode specialKey = game.specialKeyForPlayer(0);
        game.setLocalActionsForKey(specialKey, true);
        for (int i = 0; i < 48; i++) {
            turkey.update(1.0);
        }

        double targetHealthBeforeRelease = target.health;
        assertTrue(getPrivateBoolean(turkey, "turkeyGobbleCharging"),
                "Holding neutral special should charge Gobble Guard instead of attacking immediately.");
        assertEquals(0, getPrivateInt(turkey, "turkeyGobbleTimer"),
                "The Gobble Guard hitbox should not come out before release.");
        assertTrue(getPrivateInt(turkey, "turkeyGobbleHoldTimer") >= 40,
                "Gobble Guard should track charge duration.");
        assertEquals(targetHealthBeforeRelease, target.health, 0.0001,
                "Charging neutral special should not damage nearby targets.");

        game.setLocalActionsForKey(specialKey, false);
        turkey.update(1.0);

        assertFalse(getPrivateBoolean(turkey, "turkeyGobbleCharging"));
        assertTrue(getPrivateInt(turkey, "turkeyGobbleTimer") > 0,
                "Releasing neutral special should start the charged attack.");
        assertTrue(target.health < targetHealthBeforeRelease,
                "The charged neutral special should hit after release.");
        assertTrue(target.vx > 14.0,
                "The charged neutral special should launch with much stronger knockback.");
        assertTrue(getPrivateInt(turkey, "turkeyGobbleReuseTimer") <= 30,
                "Turkey neutral should use only a short immediate reuse lockout.");
    }

    @Test
    void turkeySideSpecialStaysActiveWhileHeldWithShortReuse() throws Exception {
        BirdGame3 game = new BirdGame3();
        game.activePlayers = 2;

        Bird turkey = new Bird(300.0, BirdGame3.BirdType.TURKEY, 0, game);
        Bird target = new Bird(382.0, BirdGame3.BirdType.PIGEON, 1, game);
        turkey.y = BirdGame3.GROUND_Y - 80.0;
        target.y = BirdGame3.GROUND_Y - 80.0;
        game.players[0] = turkey;
        game.players[1] = target;

        game.setLocalActionsForKey(game.rightKeyForPlayer(0), true);
        game.setLocalActionsForKey(game.specialKeyForPlayer(0), true);
        for (int i = 0; i < 36; i++) {
            turkey.update(1.0);
        }

        assertTrue(getPrivateInt(turkey, "turkeyStampedeTimer") > 0,
                "Side special should stay active while special is held.");
        assertTrue(getPrivateInt(turkey, "turkeyStampedeHoldFrames") >= 30,
                "Side special should track the held shove duration.");
        assertTrue(target.vx > 10.0,
                "Held side special should shove targets away with strong knockback.");
        assertTrue(getPrivateInt(turkey, "turkeyStampedeReuseTimer") <= 28,
                "Turkey side special should use only a short immediate reuse lockout.");

        game.setLocalActionsForKey(game.specialKeyForPlayer(0), false);
        turkey.update(1.0);

        assertEquals(0, getPrivateInt(turkey, "turkeyStampedeTimer"),
                "Releasing special should end the held side special.");
    }

    @Test
    void turkeyPanicFlapRecoversUpwardAndOnlyHitsBelow() throws Exception {
        BirdGame3 game = new BirdGame3();
        game.activePlayers = 3;

        Bird turkey = new Bird(260.0, BirdGame3.BirdType.TURKEY, 0, game);
        Bird belowTarget = new Bird(262.0, BirdGame3.BirdType.PIGEON, 1, game);
        Bird sideTarget = new Bird(430.0, BirdGame3.BirdType.PIGEON, 2, game);
        turkey.y = BirdGame3.GROUND_Y - 280.0;
        belowTarget.y = BirdGame3.GROUND_Y - 145.0;
        sideTarget.y = BirdGame3.GROUND_Y - 145.0;
        game.players[0] = turkey;
        game.players[1] = belowTarget;
        game.players[2] = sideTarget;

        double belowStart = belowTarget.health;
        double sideStart = sideTarget.health;
        invokePrivateBooleanVoid(turkey, "specialTurkeyPanicFlap", false);
        turkey.update(1.0);

        assertEquals(0, turkey.specialCooldown);
        assertEquals(0, getPrivateInt(turkey, "turkeyPanicFlapReuseTimer"),
                "Panic Flap should not use a time-based cooldown.");
        assertTrue(getPrivateBoolean(turkey, "turkeyPanicFlapUsed"),
                "Panic Flap should be locked only by the once-per-airtime flag.");
        assertTrue(turkey.vy < -10.0,
                "Panic Flap should launch Turkey upward as a recovery.");
        assertTrue(belowTarget.health < belowStart,
                "Panic Flap should lightly damage enemies directly below Turkey.");
        assertEquals(sideStart, sideTarget.health, 0.0001,
                "Panic Flap should not be a wide side hitbox.");
        assertTrue(belowTarget.vy > 0.0,
                "The wing blast should push caught enemies downward.");

        setPrivateInt(turkey, "turkeyPanicFlapTimer", 0);
        turkey.vy = 0.0;
        invokePrivateBooleanVoid(turkey, "specialTurkeyPanicFlap", false);
        assertEquals(0, getPrivateInt(turkey, "turkeyPanicFlapTimer"),
                "Panic Flap should not restart before Turkey lands.");

        turkey.y = BirdGame3.GROUND_Y - 10.0;
        turkey.update(1.0);
        assertFalse(getPrivateBoolean(turkey, "turkeyPanicFlapUsed"),
                "Landing should refresh Turkey's up special.");
    }

    @Test
    void turkeyFeastTrapSlowsWithoutDamageOrFullStun() throws Exception {
        BirdGame3 game = new BirdGame3();
        game.activePlayers = 2;

        Bird turkey = new Bird(300.0, BirdGame3.BirdType.TURKEY, 0, game);
        Bird target = new Bird(258.0, BirdGame3.BirdType.PIGEON, 1, game);
        turkey.y = BirdGame3.GROUND_Y - 80.0;
        target.y = BirdGame3.GROUND_Y - 80.0;
        turkey.facingRight = true;
        game.players[0] = turkey;
        game.players[1] = target;

        invokePrivateBooleanVoid(turkey, "specialTurkeyFeastTrap", false);
        double targetHealthBeforeTrap = target.health;
        turkey.update(1.0);

        assertEquals(0, turkey.specialCooldown);
        assertTrue(getPrivateInt(turkey, "turkeyFeastTrapReuseTimer") <= 42,
                "Turkey down special should use only a short immediate reuse lockout.");
        assertTrue(getPrivateInt(target, "turkeyStuffedTimer") > 0,
                "Stepping into Feast Trap should apply the stuffed debuff.");
        assertTrue(getPrivateInt(target, "turkeyStuffedTimer") <= 110,
                "Stuffing should use the nerfed shorter slow duration.");
        assertEquals(targetHealthBeforeTrap, target.health, 0.0001,
                "Turkey stuffing should not deal damage when applied.");

        target.vx = 12.0;
        target.vy = -6.0;
        invokePrivateVoid(target, "handleTurkeyStuffedEffect");
        assertTrue(target.vx > 8.0 && target.vx < 12.0,
                "Stuffed birds should be lightly slowed, not heavily crippled or frozen.");
        assertTrue(target.vy < 0.0 && target.vy > -6.0,
                "Stuffing should slow vertical movement without fully stopping it.");
        assertEquals(0.0, target.stunTime, 0.0001,
                "Stuffing should not stun the target.");

        target.x = 390.0;
        target.vx = 0.0;
        target.vy = 0.0;
        game.setLocalActionsForKey(game.rightKeyForPlayer(0), true);
        game.setLocalActionsForKey(game.specialKeyForPlayer(0), true);
        turkey.update(1.0);

        assertEquals(0, getPrivateInt(target, "turkeyStuffedTimer"),
                "Turkey's next hit should consume the stuffed debuff.");
        assertTrue(target.vx > 22.0,
                "Stuffed targets should take extra knockback from Turkey's next hit.");
    }

    @Test
    void turkeyUltimateSummonsHarvestTribunalAndVerdictSlash() throws Exception {
        BirdGame3 game = new BirdGame3();
        game.activePlayers = 2;

        Bird turkey = new Bird(280.0, BirdGame3.BirdType.TURKEY, 0, game);
        Bird target = new Bird(420.0, BirdGame3.BirdType.PIGEON, 1, game);
        turkey.y = BirdGame3.GROUND_Y - 80.0;
        target.y = BirdGame3.GROUND_Y - 80.0;
        game.players[0] = turkey;
        game.players[1] = target;

        turkey.refillTrainingResources(true);
        double targetHealthBefore = target.health;

        BirdSpecialSystem.useSpecial(turkey);

        assertEquals(Bird.TURKEY_HARVEST_TRIBUNAL_FRAMES,
                getPrivateInt(turkey, "turkeyHarvestTribunalTimer"),
                "Turkey ultimate should start the dedicated Harvest Tribunal state.");
        assertFalse(turkey.isUltimateReady(),
                "Starting Harvest Tribunal should consume the ultimate meter.");
        assertEquals(0, turkey.specialCooldown,
                "Harvest Tribunal should not leave a visible generic special cooldown.");
        assertEquals(0, getPrivateInt(turkey, "turkeyGobbleTimer"),
                "Turkey ultimate should not fall through into the boosted neutral special.");

        for (int i = 0; i < 54; i++) {
            turkey.update(1.0);
        }

        assertTrue(getPrivateInt(target, "turkeyStuffedTimer") > 0,
                "The tribunal pull should apply Turkey's stuffed debuff before the final hit.");
        assertEquals(targetHealthBefore, target.health, 0.0001,
                "The pull/setup phase should control space without dealing damage early.");

        for (int i = 54; i < Bird.TURKEY_HARVEST_TRIBUNAL_FINAL_FRAME + 2; i++) {
            turkey.update(1.0);
        }

        assertTrue(target.health < targetHealthBefore,
                "The verdict slash should damage a target caught at the tribunal table.");
        assertTrue(getPrivateBoolean(turkey, "turkeyHarvestTribunalFinalResolved"),
                "Harvest Tribunal should resolve exactly one final verdict.");
        assertEquals(0, getPrivateInt(target, "turkeyStuffedTimer"),
                "The verdict slash should consume the stuffed debuff.");
        assertTrue(target.vx > 12.0 || target.vy < -8.0,
                "The verdict slash should launch caught targets.");
    }

    @Test
    void opiumAndHeisenNeutralSpecialsUseInvisibleReuseTimers() throws Exception {
        BirdGame3 game = new BirdGame3();
        game.activePlayers = 2;

        Bird opium = new Bird(180.0, BirdGame3.BirdType.OPIUMBIRD, 0, game);
        Bird heisen = new Bird(320.0, BirdGame3.BirdType.HEISENBIRD, 1, game);
        opium.y = BirdGame3.GROUND_Y - 80.0;
        heisen.y = BirdGame3.GROUND_Y - 80.0;
        game.players[0] = opium;
        game.players[1] = heisen;

        invokePrivateBooleanVoid(opium, "specialOpiumNeutral", false);
        invokePrivateBooleanVoid(heisen, "specialHeisenNeutral", false);

        assertEquals(0, opium.specialCooldown);
        assertEquals(0, heisen.specialCooldown);
        assertTrue(getPrivateInt(opium, "opiumNeutralReuseTimer") > 0);
        assertTrue(getPrivateInt(heisen, "opiumNeutralReuseTimer") > 0);
        assertTrue(getPrivateDouble(opium, "opiumResourceMeter") < 100.0);
        assertTrue(getPrivateDouble(heisen, "opiumResourceMeter") < 100.0);

        game.setLocalActionsForKey(game.specialKeyForPlayer(0), true);
        opium.update(1.0);
        assertEquals(0, opium.cooldownFlash,
                "Opium Bird should use hidden reuse gates instead of visible cooldown warnings.");
    }

    @Test
    void heisenCrystalCloudMarksTargetsBrittleAndNextHitConsumesIt() throws Exception {
        BirdGame3 game = new BirdGame3();
        game.activePlayers = 2;

        Bird heisen = new Bird(260.0, BirdGame3.BirdType.HEISENBIRD, 0, game);
        Bird target = new Bird(330.0, BirdGame3.BirdType.PIGEON, 1, game);
        heisen.y = BirdGame3.GROUND_Y - 80.0;
        target.y = BirdGame3.GROUND_Y - 80.0;
        game.players[0] = heisen;
        game.players[1] = target;

        invokePrivateBooleanVoid(heisen, "specialHeisenNeutral", false);
        heisen.update(1.0);

        assertTrue(getPrivateInt(target, "heisenBrittleTimer") > 0,
                "Crystal Cloud should visibly mark nearby enemies as brittle.");

        double healthBeforeHit = target.health;
        double dealt = applyPrivateDamage(heisen, target, 6.0);

        assertTrue(dealt > 6.0,
                "Heisenbird's next hit should gain bonus damage against a brittle target.");
        assertTrue(target.health < healthBeforeHit);
        assertEquals(0, getPrivateInt(target, "heisenBrittleTimer"),
                "A normal brittle mark should be consumed by the next Heisenbird hit.");
    }

    @Test
    void opiumAndHeisenMetersGateEffectsAndRefillFromDownSpecials() throws Exception {
        BirdGame3 game = new BirdGame3();
        game.activePlayers = 2;

        Bird opium = new Bird(220.0, BirdGame3.BirdType.OPIUMBIRD, 0, game);
        Bird heisen = new Bird(380.0, BirdGame3.BirdType.HEISENBIRD, 1, game);
        opium.y = heisen.y = BirdGame3.GROUND_Y - 80.0;
        opium.facingRight = true;
        heisen.facingRight = true;
        game.players[0] = opium;
        game.players[1] = heisen;

        setPrivateDouble(opium, "opiumResourceMeter", 0.0);
        invokePrivateBooleanVoid(opium, "specialOpiumSide", false);
        assertFalse(getPrivateBoolean(opium, "opiumSideFueled"),
                "Empty Opium side special should keep the dash but not create a lean trail.");
        invokePrivateBooleanVoid(opium, "specialOpiumUp", false);
        assertFalse(getPrivateBoolean(opium, "opiumUpFueled"),
                "Empty Opium up special should not create the lean plume.");

        setPrivateDouble(heisen, "opiumResourceMeter", 0.0);
        invokePrivateBooleanVoid(heisen, "specialHeisenNeutral", false);
        heisen.update(1.0);
        assertEquals(0, getPrivateInt(opium, "heisenBrittleTimer"),
                "Empty Heisen neutral should not apply useful brittle pressure.");

        setPrivateDouble(opium, "opiumResourceMeter", 0.0);
        setPrivateInt(opium, "opiumSideTimer", 0);
        setPrivateInt(opium, "opiumUpTimer", 0);
        opium.vx = 0.0;
        opium.vy = 0.0;
        invokePrivateBooleanVoid(opium, "specialOpiumDown", false);
        for (int i = 0; i < 4; i++) {
            opium.update(1.0);
        }
        assertTrue(getPrivateDouble(opium, "opiumResourceMeter") > 0.0,
                "Standing in Opium Bird's puddle should refill his opium meter.");
        assertEquals(0.48, getPrivateDouble(opium, "opiumResourceMeter"), 0.0001,
                "Opium Bird's puddle should refill slowly enough to prevent quick farming.");

        setPrivateDouble(heisen, "opiumResourceMeter", 0.0);
        invokePrivateBooleanVoid(heisen, "specialOpiumDown", true);
        heisen.update(1.0);
        assertTrue(getPrivateDouble(heisen, "opiumResourceMeter") > 0.0,
                "Standing in Heisenbird's crystal should refill his crystal meter.");
        assertEquals(0.10, getPrivateDouble(heisen, "opiumResourceMeter"), 0.0001,
                "Heisenbird's crystal should refill slowly enough to prevent quick farming.");
        List<?> crystals = (List<?>) getPrivateObject(heisen, "opiumTraps");
        assertFalse(crystals.isEmpty());
        assertTrue(getPrivateInt(crystals.getFirst(), "lifeFrames") > 500,
                "Heisenbird's refill crystal should stay long enough to be used intentionally.");
    }

    @Test
    void lotusPatchSlowsTargetsAndRefreshesLeanCloud() throws Exception {
        BirdGame3 game = new BirdGame3();
        game.activePlayers = 2;

        Bird opium = new Bird(300.0, BirdGame3.BirdType.OPIUMBIRD, 0, game);
        Bird target = new Bird(292.0, BirdGame3.BirdType.PIGEON, 1, game);
        opium.y = BirdGame3.GROUND_Y - 80.0;
        target.y = BirdGame3.GROUND_Y - 80.0;
        opium.facingRight = true;
        target.vx = 10.0;
        game.players[0] = opium;
        game.players[1] = target;

        setPrivateInt(opium, "leanTimer", 5);
        invokePrivateBooleanVoid(opium, "specialOpiumDown", false);
        opium.update(1.0);

        assertEquals(0, opium.specialCooldown);
        assertTrue(getPrivateInt(opium, "opiumDownReuseTimer") > 0);
        assertTrue(target.vx < 10.0,
                "Lotus Patch should slow enemies standing inside it.");
        assertTrue(opium.leanTimer >= 72,
                "Enemies standing in Lotus Patch should refresh the active Lean Cloud.");
    }

    @Test
    void opiumUltimateAppliesDrowsyAndHeisenUltimateLaunchesHomingCrystalShards() throws Exception {
        BirdGame3 game = new BirdGame3();
        game.activePlayers = 4;

        Bird opium = new Bird(200.0, BirdGame3.BirdType.OPIUMBIRD, 0, game);
        Bird opiumTarget = new Bird(260.0, BirdGame3.BirdType.PIGEON, 1, game);
        Bird heisen = new Bird(560.0, BirdGame3.BirdType.HEISENBIRD, 2, game);
        Bird heisenTarget = new Bird(664.0, BirdGame3.BirdType.EAGLE, 3, game);
        opium.y = opiumTarget.y = heisen.y = heisenTarget.y = BirdGame3.GROUND_Y - 80.0;
        game.players[0] = opium;
        game.players[1] = opiumTarget;
        game.players[2] = heisen;
        game.players[3] = heisenTarget;

        setPrivateDouble(opium, "opiumResourceMeter", 0.0);
        setPrivateDouble(opium, "ultimateMeter", 100.0);
        invokePrivateVoid(opium, "special");
        opium.update(1.0);

        assertTrue(getPrivateInt(opium, "opiumUltimateTimer") > 0,
                "Opium Bird should spend ultimate meter on Purple Haze instead of a buffed normal special.");
        assertEquals(100.0, getPrivateDouble(opium, "opiumResourceMeter"), 0.0001,
                "Opium Bird's ultimate should refill his opium meter.");
        assertTrue(getPrivateInt(opiumTarget, "opiumDrowsyTimer") > 0,
                "Purple Haze should apply a readable drowsy debuff to enemies in range.");
        assertTrue(getPrivateDouble(opium, "opiumUltimateCloudX") > 0.0,
                "Purple Haze should spawn a persistent lean cloud in the arena.");

        setPrivateDouble(heisen, "opiumResourceMeter", 0.0);
        setPrivateDouble(heisen, "ultimateMeter", 100.0);
        invokePrivateVoid(heisen, "special");
        assertTrue(getPrivateInt(heisen, "heisenUltimateTimer") > 0);
        assertEquals(100.0, getPrivateDouble(heisen, "opiumResourceMeter"), 0.0001,
                "Heisenbird's ultimate should refill his crystal meter.");
        assertTrue(getPrivateInt(heisenTarget, "heisenBrittleTimer") > 0,
                "Say My Name should immediately mark nearby enemies brittle.");

        double healthBeforeOrbit = heisenTarget.health;
        for (int i = 0; i < 70; i++) {
            heisen.update(1.0);
        }
        assertTrue(heisenTarget.health < healthBeforeOrbit,
                "Orbiting crystals should damage enemies that stay in their path before they launch.");

        double healthBeforeVolley = heisenTarget.health;
        setPrivateInt(heisen, "heisenUltimateTimer", 1);
        heisen.update(1.0);
        assertTrue(getPrivateInt(heisen, "heisenUltimateVolleyTimer") > 0,
                "When Heisenbird's ultimate ends, the orbiting crystals should launch as a staggered visible volley.");
        for (int i = 0; i < 130 && heisenTarget.health >= healthBeforeVolley; i++) {
            heisen.update(1.0);
        }

        assertTrue(heisenTarget.health < healthBeforeVolley,
                "A launched crystal shard should hone toward and damage the nearest enemy.");
        boolean[] spentShards = (boolean[]) getPrivateObject(heisen, "heisenUltimateShardSpent");
        boolean anyShardSpent = false;
        for (boolean spent : spentShards) {
            anyShardSpent |= spent;
        }
        assertTrue(anyShardSpent,
                "A shard that hits or times out should leave the active volley instead of lingering forever.");
        assertEquals(0, getPrivateInt(heisenTarget, "heisenBrittleTimer"));
    }

    @Test
    void roosterSpawnsWithThreeFollowerChicks() {
        BirdGame3 game = new BirdGame3();
        game.activePlayers = 1;

        Bird rooster = new Bird(220.0, BirdGame3.BirdType.ROOSTER, 0, game);
        rooster.y = BirdGame3.GROUND_Y - 80.0;
        game.players[0] = rooster;

        rooster.update(1.0);

        List<ChickMinion> chicks = ownedChicks(game, rooster);
        boolean[] variants = new boolean[3];
        assertEquals(3, chicks.size(), "Rooster should spawn in with one chick of each type.");
        for (ChickMinion chick : chicks) {
            variants[chick.variant] = true;
            assertTrue(chick.followingOwner, "Starting chicks should follow Rooster in formation.");
            assertNull(chick.target, "Follower chicks should not be in fighting mode yet.");
        }
        assertArrayEquals(new boolean[]{true, true, true}, variants);
    }

    @Test
    void roosterNeutralAddsFollowerChicksUpToFiveWithoutVisibleCooldown() throws Exception {
        BirdGame3 game = new BirdGame3();
        game.activePlayers = 1;

        Bird rooster = new Bird(220.0, BirdGame3.BirdType.ROOSTER, 0, game);
        rooster.y = BirdGame3.GROUND_Y - 80.0;
        game.players[0] = rooster;
        rooster.update(1.0);

        invokePrivateVoid(rooster, "special");
        assertEquals(4, ownedChicks(game, rooster).size());
        assertEquals(0, rooster.specialCooldown);
        assertTrue(getPrivateInt(rooster, "roosterNeutralReuseTimer") > 0);

        setPrivateInt(rooster, "roosterNeutralReuseTimer", 0);
        invokePrivateVoid(rooster, "special");

        List<ChickMinion> chicks = ownedChicks(game, rooster);
        assertEquals(5, chicks.size(), "Neutral should fill Rooster's brood to the five-chick cap.");
        assertTrue(chicks.stream().allMatch(chick -> chick.followingOwner));
        assertEquals(0, rooster.specialCooldown,
                "Rooster's four-special kit should not show a cooldown bar.");
        assertEquals(0, rooster.cooldownFlash,
                "Rooster's invisible reuse gates should not show the cooldown warning.");
    }

    @Test
    void roosterSideThrowsNextFollowerIntoFightingMode() throws Exception {
        BirdGame3 game = new BirdGame3();
        game.activePlayers = 2;

        Bird rooster = new Bird(240.0, BirdGame3.BirdType.ROOSTER, 0, game);
        Bird target = new Bird(470.0, BirdGame3.BirdType.PIGEON, 1, game);
        rooster.y = BirdGame3.GROUND_Y - 80.0;
        target.y = BirdGame3.GROUND_Y - 80.0;
        game.players[0] = rooster;
        game.players[1] = target;
        rooster.update(1.0);

        game.setLocalActionsForKey(game.rightKeyForPlayer(0), true);
        invokePrivateVoid(rooster, "special");
        game.setLocalActionsForKey(game.rightKeyForPlayer(0), false);

        List<ChickMinion> chicks = ownedChicks(game, rooster);
        List<ChickMinion> fightingChicks = chicks.stream()
                .filter(chick -> !chick.followingOwner)
                .toList();
        assertEquals(1, fightingChicks.size(),
                "Side special should throw exactly the next follower into independent fighting mode.");
        ChickMinion thrown = fightingChicks.getFirst();
        assertSame(target, thrown.target);
        assertTrue(thrown.vx > 18.0, "Thrown chicks should launch fast enough to read as a toss.");
        assertTrue(thrown.thrownFrames > 0, "Thrown chicks should carry throw streak animation frames.");
        assertEquals(2, chicks.stream().filter(chick -> chick.followingOwner).count());
        assertEquals(0, rooster.specialCooldown);
        assertTrue(getPrivateInt(rooster, "roosterSideReuseTimer") > 0);
    }

    @Test
    void roosterUpBoostThrowsChicksUpAndOnlyUsesOncePerAirtime() throws Exception {
        BirdGame3 game = new BirdGame3();
        game.activePlayers = 1;

        Bird rooster = new Bird(260.0, BirdGame3.BirdType.ROOSTER, 0, game);
        rooster.y = BirdGame3.GROUND_Y - 220.0;
        game.players[0] = rooster;
        rooster.update(1.0);
        rooster.y = BirdGame3.GROUND_Y - 220.0;
        rooster.vy = 0.0;

        game.setLocalActionsForKey(game.jumpKeyForPlayer(0), true);
        invokePrivateVoid(rooster, "special");

        assertTrue(rooster.vy < -20.0, "Up special should be a strong chick-assisted vertical boost.");
        assertTrue(getPrivateBoolean(rooster, "roosterUpSpecialUsed"),
                "Rooster's up special should be locked by a once-per-airtime flag.");
        assertEquals(0, rooster.specialCooldown);
        assertTrue(ownedChicks(game, rooster).stream().allMatch(chick -> chick.followingOwner && chick.boostSparkFrames > 0),
                "Boosting should pull the brood into the launch animation.");

        rooster.vy = 0.0;
        invokePrivateVoid(rooster, "special");
        assertEquals(0.0, rooster.vy, 0.0001,
                "Rooster should not get another up special until he lands.");
        game.setLocalActionsForKey(game.jumpKeyForPlayer(0), false);
    }

    @Test
    void roosterDownRecallsFightingChicksBackToFormation() throws Exception {
        BirdGame3 game = new BirdGame3();
        game.activePlayers = 2;

        Bird rooster = new Bird(300.0, BirdGame3.BirdType.ROOSTER, 0, game);
        Bird target = new Bird(620.0, BirdGame3.BirdType.PIGEON, 1, game);
        rooster.y = BirdGame3.GROUND_Y - 80.0;
        target.y = BirdGame3.GROUND_Y - 80.0;
        game.players[0] = rooster;
        game.players[1] = target;
        rooster.update(1.0);

        for (int i = 0; i < game.chickMinions.size(); i++) {
            ChickMinion chick = game.chickMinions.get(i);
            chick.followingOwner = false;
            chick.target = target;
            chick.x = 740.0 + i * 45.0;
            chick.y = BirdGame3.GROUND_Y - 160.0;
        }

        game.setLocalActionsForKey(game.blockKeyForPlayer(0), true);
        invokePrivateVoid(rooster, "special");
        game.setLocalActionsForKey(game.blockKeyForPlayer(0), false);

        for (ChickMinion chick : ownedChicks(game, rooster)) {
            assertTrue(chick.followingOwner, "Down special should recall chicks from fighting mode.");
            assertNull(chick.target);
            assertTrue(Math.abs((chick.x + chick.width * 0.5) - (rooster.x + 40.0 * rooster.sizeMultiplier)) < 130.0,
                    "Recalled chicks should snap back near Rooster.");
            assertTrue(chick.commandFlashFrames > 0);
        }
        assertEquals(0, rooster.specialCooldown);
        assertTrue(getPrivateInt(rooster, "roosterDownReuseTimer") > 0);
    }

    @Test
    void roosterUltimateSummonsFlyingDawnStampede() throws Exception {
        BirdGame3 game = new BirdGame3();
        game.activePlayers = 2;

        Bird rooster = new Bird(300.0, BirdGame3.BirdType.ROOSTER, 0, game);
        Bird target = new Bird(620.0, BirdGame3.BirdType.PIGEON, 1, game);
        rooster.y = BirdGame3.GROUND_Y - 80.0;
        target.y = BirdGame3.GROUND_Y - 80.0;
        game.players[0] = rooster;
        game.players[1] = target;
        rooster.update(1.0);

        setPrivateDouble(rooster, "ultimateMeter", 100.0);
        invokePrivateVoid(rooster, "special");

        List<ChickMinion> swarm = ownedChicks(game, rooster).stream()
                .filter(chick -> chick.roosterSwarm)
                .toList();
        assertEquals(18, swarm.size(),
                "Rooster ultimate should create a large real swarm while the draw layer adds visual copies.");
        assertTrue(swarm.stream().allMatch(chick -> !chick.followingOwner),
                "Stampede chicks should immediately leave formation.");
        assertTrue(swarm.stream().allMatch(chick -> chick.target == target),
                "Stampede chicks should launch toward the nearest enemy.");
        assertTrue(swarm.stream().allMatch(chick -> chick.speed >= 14.0 && !chick.onGround),
                "Stampede chicks should be fast flying attackers.");
        assertTrue(swarm.stream().allMatch(chick -> chick.swarmHitsRemaining == 2 && chick.swarmVisualCopies >= 4),
                "Stampede chicks should have capped hits and extra render copies.");
        assertEquals(0.0, getPrivateDouble(rooster, "ultimateMeter"), 0.0001);
        assertEquals(0, rooster.specialCooldown);
        assertEquals(5, getPrivateInt(rooster, "roosterCommandFxKind"));
        assertEquals(RoosterSpecials.DAWN_STAMPEDE_MOVE, game.lastTelemetryMoveName(0, ""));
    }

    @Test
    void mockingbirdLoungeCaptureUsesBodyOverlapForLargeBirds() throws Exception {
        BirdGame3 game = new BirdGame3();
        game.activePlayers = 2;

        Bird charles = new Bird(220.0, BirdGame3.BirdType.MOCKINGBIRD, 0, game);
        Bird pelican = new Bird(500.0, BirdGame3.BirdType.PELICAN, 1, game);
        charles.y = BirdGame3.GROUND_Y - 80.0;
        charles.loungeActive = true;
        charles.loungeHealth = Bird.LOUNGE_MAX_HEALTH;
        charles.loungeX = charles.bodyCenterX();
        charles.loungeY = charles.bodyCenterY();
        pelican.x = charles.loungeX + 92.0 - pelican.bodyWidth() * 0.5;
        pelican.y = charles.loungeY - pelican.bodyHeight() * 0.5;
        game.players[0] = charles;
        game.players[1] = pelican;

        invokePrivateVoid(charles, "captureMockingbirdLoungeAbility");

        assertEquals(BirdGame3.BirdType.PELICAN, charles.mockingbirdCapturedType,
                "Lounge capture should use the target's body, not only its center point.");
    }

    @Test
    void mockingbirdLoungeCapturesEnemyChickOwnersType() throws Exception {
        BirdGame3 game = new BirdGame3();
        game.activePlayers = 2;

        Bird charles = new Bird(240.0, BirdGame3.BirdType.MOCKINGBIRD, 0, game);
        Bird rooster = new Bird(900.0, BirdGame3.BirdType.ROOSTER, 1, game);
        charles.y = BirdGame3.GROUND_Y - 80.0;
        rooster.y = BirdGame3.GROUND_Y - 80.0;
        charles.loungeActive = true;
        charles.loungeHealth = Bird.LOUNGE_MAX_HEALTH;
        charles.loungeX = charles.bodyCenterX();
        charles.loungeY = charles.bodyCenterY();
        game.players[0] = charles;
        game.players[1] = rooster;

        ChickMinion chick = new ChickMinion(charles.loungeX, charles.loungeY, 0, false, rooster);
        chick.x = charles.loungeX - chick.width * 0.5;
        chick.y = charles.loungeY - chick.height * 0.5;
        game.chickMinions.add(chick);

        invokePrivateVoid(charles, "captureMockingbirdLoungeAbility");

        assertEquals(BirdGame3.BirdType.ROOSTER, charles.mockingbirdCapturedType,
                "Capturing an enemy chick should steal the owner's Rooster neutral.");
    }

    @Test
    void copiedRoosterNeutralLaunchesHuntingChicksForMockingbird() {
        BirdGame3 game = new BirdGame3();
        game.activePlayers = 2;

        Bird charles = new Bird(260.0, BirdGame3.BirdType.MOCKINGBIRD, 0, game);
        Bird target = new Bird(560.0, BirdGame3.BirdType.PIGEON, 1, game);
        charles.y = BirdGame3.GROUND_Y - 80.0;
        target.y = BirdGame3.GROUND_Y - 80.0;
        charles.facingRight = true;
        charles.mockingbirdCapturedType = BirdGame3.BirdType.ROOSTER;
        game.players[0] = charles;
        game.players[1] = target;

        MockingbirdSpecials.performCopiedNeutral(charles, BirdGame3.BirdType.ROOSTER, false);

        List<ChickMinion> chicks = ownedChicks(game, charles);
        assertEquals(2, chicks.size(), "Copied Rooster neutral should create an immediate attacking threat.");
        for (ChickMinion chick : chicks) {
            assertFalse(chick.followingOwner, "Charles cannot use Rooster commands, so copied chicks should not idle in formation.");
            assertSame(target, chick.target);
            assertTrue(chick.vx > 15.0);
            assertTrue(chick.thrownFrames > 0);
        }
        assertEquals(BirdGame3.BirdType.MOCKINGBIRD, charles.type);
        assertEquals(BirdGame3.BirdType.ROOSTER, charles.mockingbirdCopiedNeutralSource);
    }

    @Test
    void copiedRavenNeutralKeepsFlyingAndHitsAfterCharlesReturnsToMockingbird() {
        BirdGame3 game = new BirdGame3();
        game.activePlayers = 2;

        Bird charles = new Bird(260.0, BirdGame3.BirdType.MOCKINGBIRD, 0, game);
        Bird target = new Bird(430.0, BirdGame3.BirdType.PIGEON, 1, game);
        charles.y = BirdGame3.GROUND_Y - 80.0;
        target.y = BirdGame3.GROUND_Y - 80.0;
        charles.facingRight = true;
        charles.mockingbirdCapturedType = BirdGame3.BirdType.RAVEN;
        game.players[0] = charles;
        game.players[1] = target;

        double startingHealth = target.health;
        MockingbirdSpecials.performCopiedNeutral(charles, BirdGame3.BirdType.RAVEN, false);
        assertEquals(BirdGame3.BirdType.MOCKINGBIRD, charles.type,
                "Charles should return to his own type immediately after copying the neutral.");

        for (int tick = 0; tick < 16 && target.health == startingHealth; tick++) {
            charles.update(1.0);
        }

        assertTrue(target.health < startingHealth,
                "The copied Black Quill must keep simulating after Charles returns to his own type.");
        assertTrue(target.hasRavenPortentFrom(charles),
                "A copied Black Quill hit should retain Raven's mark effect and Charles's ownership.");
    }

    @Test
    void mockingbirdUltimateSpawnsCapturedShadowCourt() throws Exception {
        BirdGame3 game = new BirdGame3();
        game.activePlayers = 2;

        Bird charles = new Bird(260.0, BirdGame3.BirdType.MOCKINGBIRD, 0, game);
        Bird target = new Bird(620.0, BirdGame3.BirdType.PIGEON, 1, game);
        charles.y = BirdGame3.GROUND_Y - 80.0;
        target.y = BirdGame3.GROUND_Y - 80.0;
        charles.loungeActive = true;
        charles.loungeHealth = Bird.LOUNGE_MAX_HEALTH;
        charles.loungeX = 430.0;
        charles.loungeY = BirdGame3.GROUND_Y - 64.0;
        charles.mockingbirdCapturedType = BirdGame3.BirdType.PELICAN;
        game.players[0] = charles;
        game.players[1] = target;

        setPrivateDouble(charles, "ultimateMeter", 100.0);
        BirdSpecialSystem.useSpecial(charles);

        assertEquals(0.0, getPrivateDouble(charles, "ultimateMeter"), 0.0001);
        assertEquals(3, game.mockingbirdShadowMinions.size());
        assertTrue(charles.loungeRoyal);
        assertEquals(200.0, charles.loungeMaxHealth, 0.0001);
        assertEquals(MockingbirdSpecials.SHADOW_COURT_MOVE, game.lastTelemetryMoveName(0, ""));

        MockingbirdShadowMinion left = game.mockingbirdShadowMinions.get(0);
        MockingbirdShadowMinion right = game.mockingbirdShadowMinions.get(1);
        MockingbirdShadowMinion inside = game.mockingbirdShadowMinions.get(2);
        assertTrue(left.bodyCenterX() < charles.loungeX - 80.0);
        assertTrue(right.bodyCenterX() > charles.loungeX + 80.0);
        assertTrue(Math.abs(inside.bodyCenterX() - charles.loungeX) < 28.0);
        assertTrue(game.mockingbirdShadowMinions.stream()
                .allMatch(shadow -> shadow.owner == charles
                        && shadow.target == target
                        && shadow.copiedType == BirdGame3.BirdType.PELICAN
                        && shadow.health <= 14.0));
    }

    @Test
    void mockingbirdUltimateCreatesLoungeAndCopiesClosestBirdWhenEmpty() throws Exception {
        BirdGame3 game = new BirdGame3();
        game.activePlayers = 3;

        Bird charles = new Bird(240.0, BirdGame3.BirdType.MOCKINGBIRD, 0, game);
        Bird eagle = new Bird(420.0, BirdGame3.BirdType.EAGLE, 1, game);
        Bird pelican = new Bird(1050.0, BirdGame3.BirdType.PELICAN, 2, game);
        charles.y = BirdGame3.GROUND_Y - 80.0;
        eagle.y = BirdGame3.GROUND_Y - 80.0;
        pelican.y = BirdGame3.GROUND_Y - 80.0;
        game.players[0] = charles;
        game.players[1] = eagle;
        game.players[2] = pelican;

        setPrivateDouble(charles, "ultimateMeter", 100.0);
        BirdSpecialSystem.useSpecial(charles);

        assertTrue(charles.loungeActive);
        assertTrue(charles.loungeRoyal);
        assertEquals(3, game.mockingbirdShadowMinions.size());
        assertTrue(game.mockingbirdShadowMinions.stream()
                .allMatch(shadow -> shadow.copiedType == BirdGame3.BirdType.EAGLE));
    }

    @Test
    void mockingbirdShadowMinionDamagesNearestEnemy() throws Exception {
        BirdGame3 game = new BirdGame3();
        game.activePlayers = 2;

        Bird charles = new Bird(260.0, BirdGame3.BirdType.MOCKINGBIRD, 0, game);
        Bird target = new Bird(350.0, BirdGame3.BirdType.PIGEON, 1, game);
        charles.y = BirdGame3.GROUND_Y - 80.0;
        target.y = BirdGame3.GROUND_Y - 80.0;
        game.players[0] = charles;
        game.players[1] = target;

        MockingbirdShadowMinion shadow = new MockingbirdShadowMinion(
                target.bodyCenterX() - 12.0,
                target.bodyCenterY(),
                BirdGame3.BirdType.EAGLE,
                charles,
                0
        );
        shadow.attackCooldown = 0;
        shadow.target = target;
        game.mockingbirdShadowMinions.add(shadow);

        double healthBefore = target.health;
        invokePrivateVoid(game, "updateMockingbirdShadowMinions");

        assertTrue(target.health < healthBefore);
        assertTrue(game.damageDealt[0] > 0);
        assertTrue(shadow.attackCooldown > 0);
    }

    @Test
    void roadrunnerNeutralChargesThenReleasesMomentumBurst() throws Exception {
        BirdGame3 game = new BirdGame3();
        game.activePlayers = 2;

        Bird runner = new Bird(220.0, BirdGame3.BirdType.ROADRUNNER, 0, game);
        Bird target = new Bird(345.0, BirdGame3.BirdType.PIGEON, 1, game);
        runner.y = BirdGame3.GROUND_Y - 80.0;
        target.y = BirdGame3.GROUND_Y - 80.0;
        game.players[0] = runner;
        game.players[1] = target;

        KeyCode specialKey = game.specialKeyForPlayer(0);
        game.setLocalActionsForKey(specialKey, true);
        for (int i = 0; i < 42; i++) {
            runner.update(1.0);
        }

        double targetHealthBeforeRelease = target.health;
        assertTrue(getPrivateBoolean(runner, "roadrunnerBeepCharging"));
        assertTrue(getPrivateInt(runner, "roadrunnerBeepChargeFrames") >= 35);
        assertEquals(targetHealthBeforeRelease, target.health, 0.0001,
                "Beep-Beep Blitz should not hit until the held neutral is released.");

        game.setLocalActionsForKey(specialKey, false);
        runner.update(1.0);

        assertFalse(getPrivateBoolean(runner, "roadrunnerBeepCharging"));
        assertTrue(getPrivateInt(runner, "roadrunnerBeepBurstTimer") > 0);
        assertTrue(target.health < targetHealthBeforeRelease,
                "Releasing neutral should fire the charged burst hit.");
        assertTrue(target.vx > 10.0,
                "Charged Beep-Beep Blitz should launch forward with real knockback.");
        assertTrue(runner.vx > 25.0,
                "Charged neutral should release Roadrunner at high speed even without horizontal input held.");
        assertEquals(0, runner.specialCooldown);
        assertTrue(getPrivateInt(runner, "roadrunnerBeepReuseTimer") > 0);
    }

    @Test
    void roadrunnerNeutralAutoReleasesAtFullChargeSpeed() throws Exception {
        BirdGame3 game = new BirdGame3();
        game.activePlayers = 1;

        Bird runner = new Bird(220.0, BirdGame3.BirdType.ROADRUNNER, 0, game);
        runner.y = BirdGame3.GROUND_Y - 80.0;
        runner.facingRight = true;
        game.players[0] = runner;

        game.setLocalActionsForKey(game.specialKeyForPlayer(0), true);
        for (int i = 0; i < 72; i++) {
            runner.update(1.0);
        }

        assertFalse(getPrivateBoolean(runner, "roadrunnerBeepCharging"),
                "Neutral should auto-release as soon as it reaches full charge.");
        assertTrue(getPrivateInt(runner, "roadrunnerBeepBurstTimer") > 0);
        assertTrue(runner.vx > 45.0,
                "A full-charge neutral should immediately propel Roadrunner at full speed.");
        game.setLocalActionsForKey(game.specialKeyForPlayer(0), false);
    }

    @Test
    void roadrunnerHighSpeedCoastsDownInsteadOfStoppingHard() {
        BirdGame3 game = new BirdGame3();
        game.activePlayers = 1;

        Bird runner = new Bird(260.0, BirdGame3.BirdType.ROADRUNNER, 0, game);
        runner.y = BirdGame3.GROUND_Y - 80.0;
        runner.vx = 30.0;
        game.players[0] = runner;

        runner.update(1.0);

        assertTrue(runner.vx > 24.0,
                "Roadrunner should bleed off high speed instead of instantly stopping when input drops.");
    }

    @Test
    void roadrunnerMomentumGracePreservesFlowBeforeSlowDecay() {
        BirdGame3 game = new BirdGame3();
        game.activePlayers = 1;

        Bird runner = new Bird(260.0, BirdGame3.BirdType.ROADRUNNER, 0, game);
        runner.y = BirdGame3.GROUND_Y - 80.0;
        runner.roadrunnerMomentum = 40.0;
        game.players[0] = runner;

        RoadrunnerSpecials.addMomentum(runner, 10.0);

        assertEquals(RoadrunnerSpecials.MOMENTUM_BUILD_GRACE_FRAMES,
                runner.roadrunnerMomentumGraceTimer);
        LanBirdState snapshot = runner.toLanState();
        Bird restored = new Bird(0.0, BirdGame3.BirdType.ROADRUNNER, 0, game);
        restored.applyLanState(snapshot);
        assertEquals(runner.roadrunnerMomentumGraceTimer, restored.roadrunnerMomentumGraceTimer,
                "LAN snapshots must preserve the deterministic grace timer.");
        for (int i = 0; i < RoadrunnerSpecials.MOMENTUM_BUILD_GRACE_FRAMES; i++) {
            RoadrunnerSpecials.handleMomentum(runner);
        }
        assertEquals(50.0, runner.roadrunnerMomentum, 0.0001,
                "Earned momentum should hold throughout the grace window.");

        RoadrunnerSpecials.handleMomentum(runner);
        assertEquals(50.0 - RoadrunnerSpecials.MOMENTUM_GROUND_DECAY_PER_FRAME,
                runner.roadrunnerMomentum, 0.0001,
                "Momentum should decay gradually once flow expires.");
    }

    @Test
    void roadrunnerCombatRetainsMomentumWithoutRemovingCounterplay() throws Exception {
        BirdGame3 game = new BirdGame3();
        game.activePlayers = 2;

        Bird runner = new Bird(220.0, BirdGame3.BirdType.ROADRUNNER, 0, game);
        Bird opponent = new Bird(330.0, BirdGame3.BirdType.PIGEON, 1, game);
        runner.y = BirdGame3.GROUND_Y - 80.0;
        opponent.y = BirdGame3.GROUND_Y - 80.0;
        runner.roadrunnerMomentum = 100.0;
        game.players[0] = runner;
        game.players[1] = opponent;

        double dealtToRunner = applyPrivateDamage(opponent, runner, 10.0);
        double expectedLoss = Math.clamp(4.0 + dealtToRunner * 0.45, 6.0, 18.0);
        assertEquals(100.0 - expectedLoss, runner.roadrunnerMomentum, 0.0001,
                "Taking damage should cost capped, damage-scaled momentum instead of a flat 38.");

        runner.roadrunnerMomentumGraceTimer = 0;
        applyPrivateDamage(runner, opponent, 8.0);
        assertEquals(RoadrunnerSpecials.MOMENTUM_HIT_GRACE_FRAMES,
                runner.roadrunnerMomentumGraceTimer,
                "Landing a hit should preserve Roadrunner's remaining flow.");
    }

    @Test
    void roadrunnerSideRicochetUsesInvisibleReuseAndHitsFast() throws Exception {
        BirdGame3 game = new BirdGame3();
        game.activePlayers = 2;

        Bird runner = new Bird(240.0, BirdGame3.BirdType.ROADRUNNER, 0, game);
        Bird target = new Bird(328.0, BirdGame3.BirdType.PIGEON, 1, game);
        runner.y = BirdGame3.GROUND_Y - 80.0;
        target.y = BirdGame3.GROUND_Y - 80.0;
        game.players[0] = runner;
        game.players[1] = target;
        setPrivateDouble(runner, "roadrunnerMomentum", 80.0);

        game.setLocalActionsForKey(game.rightKeyForPlayer(0), true);
        invokePrivateVoid(runner, "special");
        double targetHealthBefore = target.health;
        runner.update(1.0);

        assertTrue(getPrivateInt(runner, "roadrunnerRicochetTimer") > 0);
        assertTrue(Math.abs(runner.vx) > 18.0,
                "Canyon Ricochet should be a high-speed dash.");
        assertTrue(target.health < targetHealthBefore,
                "Ricochet should damage birds in its lane.");
        assertEquals(0, runner.specialCooldown);
        assertTrue(getPrivateInt(runner, "roadrunnerRicochetReuseTimer") > 0);

        game.setLocalActionsForKey(game.rightKeyForPlayer(0), false);
    }

    @Test
    void roadrunnerUpSpecialIsOncePerAirtimeDustDevilRecovery() throws Exception {
        BirdGame3 game = new BirdGame3();
        game.activePlayers = 2;

        Bird runner = new Bird(260.0, BirdGame3.BirdType.ROADRUNNER, 0, game);
        Bird target = new Bird(300.0, BirdGame3.BirdType.PIGEON, 1, game);
        runner.y = BirdGame3.GROUND_Y - 260.0;
        target.y = BirdGame3.GROUND_Y - 260.0;
        runner.vy = 0.0;
        target.vy = 0.0;
        game.players[0] = runner;
        game.players[1] = target;
        setPrivateDouble(runner, "roadrunnerMomentum", 70.0);

        game.setLocalActionsForKey(game.jumpKeyForPlayer(0), true);
        invokePrivateVoid(runner, "special");
        runner.update(1.0);

        assertTrue(runner.vy < -20.0,
                "Dust Devil Lift should provide strong vertical recovery.");
        assertTrue(target.vy < -14.0,
                "Dust Devil Lift should knock nearby birds upward.");
        assertTrue(getPrivateBoolean(runner, "roadrunnerDustDevilUsed"));
        assertEquals(0, runner.specialCooldown);

        setPrivateInt(runner, "roadrunnerDustDevilTimer", 0);
        runner.vy = 0.0;
        invokePrivateVoid(runner, "special");
        assertEquals(0.0, runner.vy, 0.0001,
                "Roadrunner should not get another up special before landing.");
        game.setLocalActionsForKey(game.jumpKeyForPlayer(0), false);
    }

    @Test
    void roadrunnerPaintedRoadSlipsEnemiesAndBoostsOwnerMomentum() throws Exception {
        BirdGame3 game = new BirdGame3();
        game.activePlayers = 2;

        Bird runner = new Bird(300.0, BirdGame3.BirdType.ROADRUNNER, 0, game);
        Bird target = new Bird(365.0, BirdGame3.BirdType.PIGEON, 1, game);
        runner.y = BirdGame3.GROUND_Y - 80.0;
        target.y = BirdGame3.GROUND_Y - 80.0;
        runner.facingRight = true;
        game.players[0] = runner;
        game.players[1] = target;

        game.setLocalActionsForKey(game.blockKeyForPlayer(0), true);
        invokePrivateVoid(runner, "special");
        game.setLocalActionsForKey(game.blockKeyForPlayer(0), false);

        assertEquals(0, runner.specialCooldown);
        assertTrue(getPrivateInt(runner, "roadrunnerPaintedRoadReuseTimer") > 0);
        assertEquals(1, ((List<?>) getPrivateObject(runner, "roadrunnerPaintedRoads")).size());
        assertTrue(runner.vx > 8.0,
                "Down special should propel Roadrunner forward when no horizontal input is held.");

        runner.update(1.0);
        assertTrue(getPrivateInt(target, "roadrunnerSlipTimer") > 0,
                "Enemies standing on Painted Road should receive the slip debuff.");
        assertEquals(0, getPrivateInt(runner, "roadrunnerRoadBoostTimer"),
                "Roadrunner should not trigger his road boost before stepping off the road once.");

        target.vx = 0.0;
        target.update(1.0);
        assertTrue(target.vx < -8.0,
                "Stepping on the road should launch enemies opposite the road direction.");

        Object road = ((List<?>) getPrivateObject(runner, "roadrunnerPaintedRoads")).getFirst();
        double roadX = getPrivateDouble(road, "x");
        double roadY = getPrivateDouble(road, "y");
        target.x = 800.0;
        runner.x = roadX - 220.0;
        runner.y = roadY - 80.0;
        runner.vx = 0.0;
        runner.vy = 0.0;
        runner.update(1.0);

        runner.x = roadX - 40.0;
        runner.y = roadY - 80.0;
        runner.vx = 0.0;
        runner.vy = 0.0;
        runner.update(1.0);

        assertTrue(getPrivateInt(runner, "roadrunnerRoadBoostTimer") > 0,
                "Roadrunner should trigger the road boost after leaving and re-entering it.");
        assertTrue(runner.vx > 20.0,
                "Roadrunner's road should launch him hard in the road direction once armed.");
    }

    @Test
    void defeatedBirdRemovesOwnedSummons() {
        BirdGame3 game = new BirdGame3();
        Bird owner = new Bird(100, BirdGame3.BirdType.VULTURE, 0, game);
        game.players[0] = owner;

        CrowMinion crow = new CrowMinion(140, 140, null);
        crow.owner = owner;
        game.crowMinions.add(crow);

        ChickMinion chick = new ChickMinion(150, 150, 0, false, owner);
        game.chickMinions.add(chick);

        owner.health = 0;
        owner.update(1.0);

        assertTrue(game.crowMinions.isEmpty());
        assertTrue(game.chickMinions.isEmpty());
    }

    @Test
    void vultureNeutralConsumesHeldCrowTicksAndRecharges() throws Exception {
        BirdGame3 game = new BirdGame3();
        game.activePlayers = 2;

        Bird vulture = new Bird(100.0, BirdGame3.BirdType.VULTURE, 0, game);
        Bird target = new Bird(260.0, BirdGame3.BirdType.PIGEON, 1, game);
        vulture.y = BirdGame3.GROUND_Y - 80.0;
        target.y = BirdGame3.GROUND_Y - 80.0;
        game.players[0] = vulture;
        game.players[1] = target;

        game.setLocalActionsForKey(game.specialKeyForPlayer(0), true);
        vulture.update(1.0);

        assertEquals(1, game.crowMinions.size());
        assertEquals(Bird.VULTURE_CROW_TICK_MAX - 1, getPrivateInt(vulture, "vultureCrowTicks"));
        assertEquals(0, vulture.specialCooldown);

        for (int i = 0; i < 40 && game.crowMinions.size() < Bird.VULTURE_CROW_TICK_MAX; i++) {
            vulture.update(1.0);
        }

        assertEquals(Bird.VULTURE_CROW_TICK_MAX, game.crowMinions.size(),
                "Holding neutral should walk through all available Vulture crow ticks.");
        assertEquals(0, getPrivateInt(vulture, "vultureCrowTicks"));

        game.setLocalActionsForKey(game.specialKeyForPlayer(0), false);
        for (int i = 0; i < 165; i++) {
            vulture.update(1.0);
        }

        assertTrue(getPrivateInt(vulture, "vultureCrowTicks") >= 1,
                "Spent Vulture crow ticks should recharge over time.");
    }

    @Test
    void vultureBoneOfferingSpawnsDelayedAnchoredCrowSwarm() throws Exception {
        BirdGame3 game = new BirdGame3();
        game.activePlayers = 2;

        Bird vulture = new Bird(100.0, BirdGame3.BirdType.VULTURE, 0, game);
        Bird target = new Bird(190.0, BirdGame3.BirdType.PIGEON, 1, game);
        vulture.y = BirdGame3.GROUND_Y - 80.0;
        target.y = BirdGame3.GROUND_Y - 80.0;
        game.players[0] = vulture;
        game.players[1] = target;

        vulture.specialVultureBoneOffering();
        Object bait = getPrivateObject(vulture, "vultureBait");

        assertTrue(getPrivateInt(bait, "lifeFrames") >= 700,
                "Vulture's bone offering should last much longer than before.");
        for (int i = 0; i < 120; i++) {
            vulture.update(1.0);
        }
        assertTrue(game.crowMinions.isEmpty(),
                "Bone offering crows should not appear immediately.");

        for (int i = 0; i < 45; i++) {
            vulture.update(1.0);
        }
        int firstWave = game.crowMinions.size();
        assertTrue(firstWave > 0);
        assertTrue(game.crowMinions.stream().allMatch(CrowMinion::guardsAnchor));

        for (int i = 0; i < 160; i++) {
            vulture.update(1.0);
        }
        assertTrue(game.crowMinions.size() > firstWave,
                "Bone offering should build crow pressure gradually.");
        assertTrue(game.crowMinions.stream().allMatch(CrowMinion::guardsAnchor),
                "Bone offering crows should stay leashed to the bone.");
    }

    @Test
    void vultureUltimateStartsBlackSkyFeastInsteadOfBoostedCall() throws Exception {
        BirdGame3 game = new BirdGame3();
        game.activePlayers = 2;

        Bird vulture = new Bird(160.0, BirdGame3.BirdType.VULTURE, 0, game);
        Bird target = new Bird(360.0, BirdGame3.BirdType.PIGEON, 1, game);
        vulture.y = BirdGame3.GROUND_Y - 80.0;
        target.y = BirdGame3.GROUND_Y - 80.0;
        game.players[0] = vulture;
        game.players[1] = target;

        setPrivateDouble(vulture, "ultimateMeter", 100.0);
        invokePrivateVoid(vulture, "special");

        assertEquals(Bird.VULTURE_BLACK_SKY_FRAMES, getPrivateInt(vulture, "vultureBlackSkyTimer"));
        assertEquals(Bird.VULTURE_BLACK_SKY_INITIAL_CROWS, getPrivateInt(vulture, "vultureBlackSkyCrowsSpawned"));
        assertEquals(Bird.VULTURE_BLACK_SKY_INITIAL_CROWS, game.crowMinions.size());
        assertTrue(game.crowMinions.stream().allMatch(crow -> crow.owner == vulture && crow.hasCrown));
        assertEquals(0, getPrivateInt(vulture, "vultureCallTimer"),
                "Vulture ultimate should not fall through into boosted Summon Crows.");
        assertFalse(vulture.isUltimateReady());
        assertEquals(0, vulture.specialCooldown);
        assertEquals(VultureSpecials.BLACK_SKY_FEAST_MOVE, game.lastTelemetryMoveName(0, ""));
    }

    @Test
    void vultureBlackSkyFeastBuildsCrowStormAndFinalHit() throws Exception {
        BirdGame3 game = new BirdGame3();
        game.activePlayers = 2;

        Bird vulture = new Bird(160.0, BirdGame3.BirdType.VULTURE, 0, game);
        Bird target = new Bird(310.0, BirdGame3.BirdType.PIGEON, 1, game);
        vulture.y = BirdGame3.GROUND_Y - 80.0;
        target.y = BirdGame3.GROUND_Y - 80.0;
        game.players[0] = vulture;
        game.players[1] = target;

        setPrivateDouble(vulture, "ultimateMeter", 100.0);
        invokePrivateVoid(vulture, "special");
        int carrionTimerAfterStart = vulture.carrionSwarmTimer;
        vulture.update(1.0);
        assertEquals(carrionTimerAfterStart - 1, vulture.carrionSwarmTimer,
                "Carrion swarm visuals should tick in sim updates, not while rendering.");

        for (int i = 0; i < 70; i++) {
            vulture.update(1.0);
        }

        assertTrue(getPrivateInt(vulture, "vultureBlackSkyCrowsSpawned") >= Bird.VULTURE_BLACK_SKY_TARGET_CROWS,
                "Black Sky Feast should build into a large real crow swarm before the finisher.");
        double healthBeforeFinal = target.health;

        for (int i = 0; i < 65; i++) {
            vulture.update(1.0);
        }

        assertTrue(getPrivateBoolean(vulture, "vultureBlackSkyFinalHit"));
        assertTrue(target.health < healthBeforeFinal,
                "The feast finisher should damage targets caught near the storm center.");
        assertTrue(getPrivateInt(vulture, "vultureBlackSkyCrowsSpawned")
                        >= Bird.VULTURE_BLACK_SKY_TARGET_CROWS + Bird.VULTURE_BLACK_SKY_FINAL_CROWS,
                "The finisher should add a final crow burst.");
    }

    @Test
    void crowContactLaunchesMoreSidewaysThanUpward() throws Exception {
        BirdGame3 game = new BirdGame3();
        game.activePlayers = 2;

        Bird target = new Bird(220.0, BirdGame3.BirdType.PIGEON, 0, game);
        Bird bystander = new Bird(980.0, BirdGame3.BirdType.EAGLE, 1, game);
        target.y = BirdGame3.GROUND_Y - 80.0;
        bystander.y = BirdGame3.GROUND_Y - 80.0;
        game.players[0] = target;
        game.players[1] = bystander;

        CrowMinion crow = new CrowMinion(target.x + 16.0, target.y + 40.0, target);
        crow.vx = 3.2;
        crow.vy = 0.0;
        game.crowMinions.add(crow);

        invokePrivateVoid(game, "updateWorldFixed");

        assertTrue(game.crowMinions.isEmpty());
        assertTrue(target.health < Bird.STARTING_HEALTH);
        assertTrue(target.vx > Math.abs(target.vy),
                "Crow contact should shove targets sideways more than it launches them upward.");
    }

    @Test
    void anchoredCrowContactLaunchesMoreSidewaysThanUpward() throws Exception {
        BirdGame3 game = new BirdGame3();
        game.activePlayers = 2;

        Bird target = new Bird(220.0, BirdGame3.BirdType.PIGEON, 0, game);
        Bird bystander = new Bird(980.0, BirdGame3.BirdType.EAGLE, 1, game);
        target.y = BirdGame3.GROUND_Y - 80.0;
        bystander.y = BirdGame3.GROUND_Y - 80.0;
        game.players[0] = target;
        game.players[1] = bystander;

        CrowMinion crow = new CrowMinion(target.x + 16.0, target.y + 40.0, target)
                .withAnchorGuard(target.x + 40.0, target.y + 40.0, 120.0, 20);
        crow.vx = 0.0;
        crow.vy = 0.0;
        game.crowMinions.add(crow);

        invokePrivateVoid(game, "updateWorldFixed");

        assertTrue(game.crowMinions.isEmpty());
        assertTrue(target.health < Bird.STARTING_HEALTH);
        assertTrue(target.vx > Math.abs(target.vy),
                "Bone-guarding crows should shove targets sideways more than they launch them upward.");
    }

    @Test
    void vultureDamageTuningAppliesToFreeAndAnchoredCrows() throws Exception {
        double originalVultureDamage = BirdGame3.BirdType.VULTURE.damageDealtMult;
        double originalPigeonDamageTaken = BirdGame3.BirdType.PIGEON.damageTakenMult;
        try {
            BirdGame3.BirdType.VULTURE.damageDealtMult = 0.5;
            BirdGame3.BirdType.PIGEON.damageTakenMult = 1.0;

            assertEquals(0.5, playOwnedVultureCrowHit(false), 0.0001,
                    "Free-flying crow contact should inherit Vulture's outgoing damage tuning.");
            assertEquals(0.5, playOwnedVultureCrowHit(true), 0.0001,
                    "Bone Offering crow contact should inherit Vulture's outgoing damage tuning.");
        } finally {
            BirdGame3.BirdType.VULTURE.damageDealtMult = originalVultureDamage;
            BirdGame3.BirdType.PIGEON.damageTakenMult = originalPigeonDamageTaken;
        }
    }

    private static double playOwnedVultureCrowHit(boolean anchored) throws Exception {
        BirdGame3 game = new BirdGame3();
        game.activePlayers = 2;

        Bird owner = new Bird(980.0, BirdGame3.BirdType.VULTURE, 0, game);
        Bird target = new Bird(220.0, BirdGame3.BirdType.PIGEON, 1, game);
        owner.y = BirdGame3.GROUND_Y - 80.0;
        target.y = BirdGame3.GROUND_Y - 80.0;
        game.players[0] = owner;
        game.players[1] = target;

        CrowMinion crow = new CrowMinion(target.x + 16.0, target.y + 40.0, target);
        crow.owner = owner;
        crow.vx = 0.0;
        crow.vy = 0.0;
        if (anchored) {
            crow.withAnchorGuard(target.x + 40.0, target.y + 40.0, 120.0, 20);
        }
        game.crowMinions.add(crow);

        double startingHealth = target.health;
        invokePrivateVoid(game, "updateWorldFixed");

        assertTrue(game.crowMinions.isEmpty());
        return startingHealth - target.health;
    }

    @Test
    void localAndAiInputsStaySeparated() {
        BirdGame3 game = new BirdGame3();
        game.activePlayers = 2;
        game.isAI[1] = true;

        game.setLocalActionsForKey(KeyCode.ENTER, true);
        assertFalse(game.isAttackPressed(1));

        game.setAiControlKey(1, game.attackKeyForPlayer(1), true);
        assertTrue(game.isAttackPressed(1));

        game.setLocalActionsForKey(KeyCode.ENTER, false);
        assertTrue(game.isAttackPressed(1));

        game.clearGameplayInputs();
        assertFalse(game.isAttackPressed(1));
    }

    @Test
    void sharedKeyboardAliasesMirrorTwoPlayerLocalInputs() {
        BirdGame3 game = new BirdGame3();
        game.activePlayers = 2;

        game.setLocalActionsForKey(KeyCode.Y, true);
        game.setLocalActionsForKey(KeyCode.O, true);

        assertTrue(game.isAttackPressed(0));
        assertTrue(game.isAttackPressed(1));

        game.setLocalActionsForKey(KeyCode.Y, false);
        game.setLocalActionsForKey(KeyCode.O, false);

        assertFalse(game.isAttackPressed(0));
        assertFalse(game.isAttackPressed(1));
    }

    @Test
    void giantCrowSurvivesHitWithKnockbackAndParticles() throws Exception {
        BirdGame3 game = new BirdGame3();
        game.activePlayers = 1;

        Bird attacker = new Bird(100.0, BirdGame3.BirdType.PIGEON, 0, game);
        attacker.facingRight = true;
        game.players[0] = attacker;

        CrowMinion giantCrow = new CrowMinion(attacker.x + 110.0, attacker.y + 40.0, null)
                .withVariant(CrowMinion.VARIANT_GIANT_CROW);
        giantCrow.vx = 0.0;
        giantCrow.vy = 0.0;
        game.crowMinions.add(giantCrow);

        invokePrivateVoid(attacker, "attack");

        assertEquals(1, game.crowMinions.size());
        CrowMinion survivor = game.crowMinions.getFirst();
        assertEquals(2, survivor.life);
        assertTrue(Math.abs(survivor.vx) > 0.1);
        assertTrue(survivor.vy < 0.0);
        assertTrue(survivor.hitFlashTimer > 0);
        assertTrue(game.particles.size() >= 6);
    }

    @Test
    void groundedSmashAttackBuildsMuchStrongerKnockbackThanSideTilt() {
        double tapKnockback = attackKnockbackAfterHoldingForFrames(1);
        double chargedKnockback = attackKnockbackAfterHoldingForFrames(36);

        assertTrue(chargedKnockback > tapKnockback * 2.2,
                "Charged smash attacks should launch much harder than a quick side tilt.");
    }

    @Test
    void groundedAttackBiasesKnockbackHorizontally() throws Exception {
        BirdGame3 game = new BirdGame3();
        game.activePlayers = 2;

        Bird attacker = new Bird(100.0, BirdGame3.BirdType.PIGEON, 0, game);
        Bird target = new Bird(190.0, BirdGame3.BirdType.EAGLE, 1, game);
        attacker.y = BirdGame3.GROUND_Y - 80.0;
        target.y = BirdGame3.GROUND_Y - 80.0;
        attacker.facingRight = true;
        game.players[0] = attacker;
        game.players[1] = target;

        invokePrivateVoid(attacker, "attack");

        assertTrue(target.vx > 0.0, "Attack should still push the target forward.");
        assertTrue(target.vy < 0.0, "Attack should still pop the target upward.");
        assertTrue(target.vx > Math.abs(target.vy) * 5.0,
                "Basic attacks should apply noticeably more horizontal knockback than vertical launch.");
    }

    @Test
    void groundedTiltInputsProduceDistinctSideAndUpTilts() throws Exception {
        BirdGame3 sideGame = new BirdGame3();
        sideGame.activePlayers = 2;

        Bird sideAttacker = new Bird(100.0, BirdGame3.BirdType.PIGEON, 0, sideGame);
        Bird sideTarget = new Bird(190.0, BirdGame3.BirdType.EAGLE, 1, sideGame);
        sideAttacker.y = BirdGame3.GROUND_Y - 80.0;
        sideTarget.y = BirdGame3.GROUND_Y - 80.0;
        sideAttacker.facingRight = true;
        sideGame.players[0] = sideAttacker;
        sideGame.players[1] = sideTarget;
        sideGame.setLocalActionsForKey(sideGame.rightKeyForPlayer(0), true);

        invokePrivateVoid(sideAttacker, "attack");

        BirdGame3 upGame = new BirdGame3();
        upGame.activePlayers = 2;

        Bird upAttacker = new Bird(100.0, BirdGame3.BirdType.PIGEON, 0, upGame);
        Bird upTarget = new Bird(190.0, BirdGame3.BirdType.EAGLE, 1, upGame);
        upAttacker.y = BirdGame3.GROUND_Y - 80.0;
        upTarget.y = BirdGame3.GROUND_Y - 80.0;
        upAttacker.facingRight = true;
        upGame.players[0] = upAttacker;
        upGame.players[1] = upTarget;
        upGame.setLocalActionsForKey(upGame.jumpKeyForPlayer(0), true);

        invokePrivateVoid(upAttacker, "attack");

        assertTrue(sideTarget.vx > 0.0);
        assertTrue(upTarget.vx > 0.0);
        assertTrue(sideTarget.vx > upTarget.vx * 1.4,
                "Side normals should launch much farther horizontally than up normals.");
        assertTrue(Math.abs(upTarget.vy) > Math.abs(sideTarget.vy) * 1.8,
                "Up normals should launch much higher than side normals.");
    }

    @Test
    void attackPlusBlockPerformsGroundDownTiltInsteadOfShielding() throws Exception {
        BirdGame3 game = new BirdGame3();
        game.activePlayers = 2;

        Bird attacker = new Bird(100.0, BirdGame3.BirdType.PIGEON, 0, game);
        Bird target = new Bird(190.0, BirdGame3.BirdType.EAGLE, 1, game);
        attacker.y = BirdGame3.GROUND_Y - 80.0;
        target.y = BirdGame3.GROUND_Y - 80.0;
        attacker.facingRight = true;
        game.players[0] = attacker;
        game.players[1] = target;

        double startingHealth = target.health;
        game.setLocalActionsForKey(game.attackKeyForPlayer(0), true);
        game.setLocalActionsForKey(game.blockKeyForPlayer(0), true);

        attacker.update(1.0);

        assertFalse(attacker.isBlocking, "Attack + block should reserve the input for a down normal, not raise shield.");
        assertEquals(0, getPrivateInt(attacker, "attackChargeFrames"), "Down tilts should not enter smash charge.");

        game.setLocalActionsForKey(game.attackKeyForPlayer(0), false);
        attacker.update(1.0);

        assertTrue(target.health < startingHealth, "Quickly releasing attack + block should perform the grounded down tilt.");
        assertTrue(target.vx > 0.0);
        assertTrue(target.vy > -3.0, "Grounded down tilt should launch flatter than the default launcher.");
    }

    @Test
    void attackPlusBlockHeldLongEnoughChargesAndReleasesGroundDownSmash() throws Exception {
        BirdGame3 game = new BirdGame3();
        game.activePlayers = 2;

        Bird attacker = new Bird(100.0, BirdGame3.BirdType.PIGEON, 0, game);
        Bird target = new Bird(190.0, BirdGame3.BirdType.EAGLE, 1, game);
        attacker.y = BirdGame3.GROUND_Y - 80.0;
        target.y = BirdGame3.GROUND_Y - 80.0;
        attacker.facingRight = true;
        game.players[0] = attacker;
        game.players[1] = target;

        double startingHealth = target.health;
        game.setLocalActionsForKey(game.attackKeyForPlayer(0), true);
        game.setLocalActionsForKey(game.blockKeyForPlayer(0), true);

        for (int i = 0; i < 8; i++) {
            attacker.update(1.0);
        }

        assertFalse(attacker.isBlocking, "Holding attack + block for a down smash should not raise shield.");
        assertTrue(getPrivateInt(attacker, "attackChargeFrames") > 0, "Holding the input should convert the grounded down attack into smash charge.");

        game.setLocalActionsForKey(game.attackKeyForPlayer(0), false);
        attacker.update(1.0);

        assertTrue(target.health < startingHealth, "Releasing after the hold should perform the down smash.");
        assertTrue(target.vx > 0.0);
        assertTrue(Math.abs(target.vx) > 9.0, "Down smash should launch harder than a down tilt.");
    }

    @Test
    void aerialBackAirLaunchesBehindTheAttacker() {
        BirdGame3 game = new BirdGame3();
        game.activePlayers = 2;

        Bird attacker = new Bird(200.0, BirdGame3.BirdType.PIGEON, 0, game);
        Bird target = new Bird(120.0, BirdGame3.BirdType.EAGLE, 1, game);
        attacker.y = BirdGame3.GROUND_Y - 280.0;
        target.y = BirdGame3.GROUND_Y - 280.0;
        attacker.facingRight = true;
        game.players[0] = attacker;
        game.players[1] = target;

        game.setLocalActionsForKey(game.leftKeyForPlayer(0), true);
        game.setLocalActionsForKey(game.attackKeyForPlayer(0), true);

        attacker.update(1.0);

        assertTrue(target.vx < 0.0, "Holding back in the air should create a back air that launches behind the bird.");
        assertTrue(target.health < Bird.STARTING_HEALTH);
    }

    @Test
    void smashAttackBiasesKnockbackHorizontallyAfterLaunchScaling() throws Exception {
        BirdGame3 game = new BirdGame3();
        game.activePlayers = 2;
        setPrivateBoolean(game);

        Bird attacker = new Bird(100.0, BirdGame3.BirdType.PIGEON, 0, game);
        Bird target = new Bird(190.0, BirdGame3.BirdType.EAGLE, 1, game);
        attacker.y = BirdGame3.GROUND_Y - 80.0;
        target.y = BirdGame3.GROUND_Y - 80.0;
        attacker.facingRight = true;
        game.players[0] = attacker;
        game.players[1] = target;

        invokePrivateVoid(attacker, "attack");
        invokePrivateVoid(target, "applyPendingSmashLaunch");

        assertTrue(target.vx > 0.0, "Smash hit should still push the target forward.");
        assertTrue(target.vy < 0.0, "Smash hit should still launch the target upward.");
        assertTrue(target.vx > Math.abs(target.vy) * 5.0,
                "Smash launch scaling should keep the knockback more horizontal than vertical.");
    }

    @Test
    void smashDirectionalInfluenceCanBendLaunchUpward() throws Exception {
        BirdGame3 baselineGame = new BirdGame3();
        baselineGame.activePlayers = 2;
        setPrivateBoolean(baselineGame);

        Bird baselineAttacker = new Bird(100.0, BirdGame3.BirdType.PIGEON, 0, baselineGame);
        Bird baselineTarget = new Bird(190.0, BirdGame3.BirdType.EAGLE, 1, baselineGame);
        baselineAttacker.y = BirdGame3.GROUND_Y - 80.0;
        baselineTarget.y = BirdGame3.GROUND_Y - 80.0;
        baselineAttacker.facingRight = true;
        baselineGame.players[0] = baselineAttacker;
        baselineGame.players[1] = baselineTarget;

        invokePrivateVoid(baselineAttacker, "attack");
        invokePrivateVoid(baselineTarget, "applyPendingSmashLaunch");

        BirdGame3 diGame = new BirdGame3();
        diGame.activePlayers = 2;
        setPrivateBoolean(diGame);

        Bird diAttacker = new Bird(100.0, BirdGame3.BirdType.PIGEON, 0, diGame);
        Bird diTarget = new Bird(190.0, BirdGame3.BirdType.EAGLE, 1, diGame);
        diAttacker.y = BirdGame3.GROUND_Y - 80.0;
        diTarget.y = BirdGame3.GROUND_Y - 80.0;
        diAttacker.facingRight = true;
        diGame.players[0] = diAttacker;
        diGame.players[1] = diTarget;

        diGame.setLocalActionsForKey(diGame.jumpKeyForPlayer(1), true);
        invokePrivateVoid(diAttacker, "attack");
        invokePrivateVoid(diTarget, "applyPendingSmashLaunch");

        assertTrue(diTarget.vx < baselineTarget.vx,
                "Holding up during launch should trade some forward speed for a steeper escape angle.");
        assertTrue(diTarget.vy < baselineTarget.vy,
                "Holding up during launch should angle the target farther upward.");
    }

    @Test
    void smashDirectionalInfluenceCanBendVerticalLaunchSideways() throws Exception {
        BirdGame3 game = new BirdGame3();
        game.activePlayers = 2;
        setPrivateBoolean(game);

        Bird target = new Bird(190.0, BirdGame3.BirdType.EAGLE, 1, game);
        game.players[1] = target;
        target.vx = 0.0;
        target.vy = -12.0;
        setPrivateDouble(target, "pendingSmashLaunchScale", 1.45);

        game.setLocalActionsForKey(game.rightKeyForPlayer(1), true);
        invokePrivateVoid(target, "applyPendingSmashLaunch");

        assertTrue(target.vx > 0.0, "Holding right during a vertical launch should bend the trajectory sideways.");
        assertTrue(target.vy < 0.0, "Directional influence should preserve upward launch on a vertical hit.");
    }

    @Test
    void knockbackTuningBoostsNonSmashNormalsAndTonesDownSmashes() throws Exception {
        BirdGame3 game = new BirdGame3();
        Bird bird = new Bird(100.0, BirdGame3.BirdType.PIGEON, 0, game);

        Class<?> variantClass = Class.forName("com.example.birdgame3.Bird$NormalAttackVariant");
        Method multiplier = Bird.class.getDeclaredMethod("attackKnockbackBalanceMultiplier", variantClass);
        multiplier.setAccessible(true);

        Enum<?> sideTilt = enumConstant(variantClass, "SIDE_TILT");
        Enum<?> neutralAir = enumConstant(variantClass, "NEUTRAL_AIR");
        Enum<?> sideSmash = enumConstant(variantClass, "SIDE_SMASH");
        Enum<?> upSmash = enumConstant(variantClass, "UP_SMASH");

        assertTrue((double) multiplier.invoke(bird, sideTilt) > 1.0);
        assertTrue((double) multiplier.invoke(bird, neutralAir) > 1.0);
        assertTrue((double) multiplier.invoke(bird, sideSmash) < 1.0);
        assertTrue((double) multiplier.invoke(bird, upSmash) < 1.0);
    }

    @Test
    void smashRespawnNestGrantsTemporaryInvulnerability() throws Exception {
        BirdGame3 game = new BirdGame3();
        game.activePlayers = 2;
        setPrivateBoolean(game);

        Bird attacker = new Bird(100.0, BirdGame3.BirdType.PIGEON, 0, game);
        Bird respawned = new Bird(190.0, BirdGame3.BirdType.EAGLE, 1, game);
        game.players[0] = attacker;
        game.players[1] = respawned;

        respawned.resetForSmashRespawn(190.0, BirdGame3.GROUND_Y - 220.0, 0.0);

        Platform nest = (Platform) getPrivateObject(respawned, "respawnNestPlatform");
        assertNotNull(nest);
        assertTrue(respawned.isCombatInvulnerable());
        assertTrue(respawned.isOnGround());
        assertFalse(game.canDamage(attacker, respawned));

        setPrivateInt(respawned, "respawnInvulnerabilityTimer", 1);
        respawned.update(1.0);

        assertFalse(respawned.isCombatInvulnerable());
        assertNull(getPrivateObject(respawned, "respawnNestPlatform"));
        assertTrue(game.canDamage(attacker, respawned));
    }

    @Test
    void smashBlastZoneKoRespawnsBirdOnNestPlatform() throws Exception {
        BirdGame3 game = new BirdGame3();
        game.activePlayers = 1;
        setPrivateBoolean(game);
        game.scores[0] = 3;

        Bird bird = new Bird(BirdGame3.WORLD_WIDTH + 420.0, BirdGame3.BirdType.EAGLE, 0, game);
        bird.y = BirdGame3.GROUND_Y - 80.0;
        game.players[0] = bird;

        bird.update(1.0);

        assertEquals(2, game.scores[0]);
        assertTrue(bird.isCombatInvulnerable());
        assertTrue(bird.isOnGround());
        assertTrue(bird.y < BirdGame3.GROUND_Y - 180.0);
        assertNotNull(getPrivateObject(bird, "respawnNestPlatform"));
    }

    @Test
    void shieldAbsorbsBasicAttackIntoDurabilityInsteadOfHealth() throws Exception {
        BirdGame3 game = new BirdGame3();
        game.activePlayers = 2;

        Bird attacker = new Bird(100.0, BirdGame3.BirdType.PIGEON, 0, game);
        Bird defender = new Bird(190.0, BirdGame3.BirdType.EAGLE, 1, game);
        attacker.y = BirdGame3.GROUND_Y - 80.0;
        defender.y = BirdGame3.GROUND_Y - 80.0;
        attacker.facingRight = true;
        game.players[0] = attacker;
        game.players[1] = defender;

        game.setLocalActionsForKey(game.blockKeyForPlayer(1), true);
        for (int i = 0; i < 5; i++) {
            defender.update(1.0);
        }
        invokePrivateVoid(attacker, "attack");

        assertEquals(Bird.STARTING_HEALTH, defender.health, 0.0001);
        assertTrue(defender.isBlocking);
        assertTrue(getPrivateDouble(defender, "shieldHealth") < 60.0);
        assertTrue(getPrivateInt(defender, "shieldStunFrames") > 0);
    }

    @Test
    void shieldHitDoesNotTriggerCooldownFlashBanner() throws Exception {
        BirdGame3 game = new BirdGame3();
        game.activePlayers = 2;

        Bird attacker = new Bird(100.0, BirdGame3.BirdType.PIGEON, 0, game);
        Bird defender = new Bird(190.0, BirdGame3.BirdType.EAGLE, 1, game);
        attacker.y = BirdGame3.GROUND_Y - 80.0;
        defender.y = BirdGame3.GROUND_Y - 80.0;
        attacker.facingRight = true;
        game.players[0] = attacker;
        game.players[1] = defender;

        game.setLocalActionsForKey(game.blockKeyForPlayer(1), true);
        for (int i = 0; i < 5; i++) {
            defender.update(1.0);
        }
        invokePrivateVoid(attacker, "attack");

        assertEquals(0, defender.cooldownFlash);
    }

    @Test
    void shieldStartupParryStunsAttackerWithoutConsumingShield() throws Exception {
        BirdGame3 game = new BirdGame3();
        game.activePlayers = 2;

        Bird attacker = new Bird(100.0, BirdGame3.BirdType.PIGEON, 0, game);
        Bird defender = new Bird(190.0, BirdGame3.BirdType.EAGLE, 1, game);
        attacker.y = BirdGame3.GROUND_Y - 80.0;
        defender.y = BirdGame3.GROUND_Y - 80.0;
        attacker.facingRight = true;
        game.players[0] = attacker;
        game.players[1] = defender;

        game.setLocalActionsForKey(game.blockKeyForPlayer(1), true);
        defender.update(1.0);
        double shieldBefore = getPrivateDouble(defender, "shieldHealth");
        invokePrivateVoid(attacker, "attack");

        assertTrue(attacker.stunTime >= 20.0);
        assertEquals(Bird.STARTING_HEALTH, defender.health, 0.0001);
        assertEquals(shieldBefore, getPrivateDouble(defender, "shieldHealth"), 0.0001);
        assertFalse(defender.isBlocking);
    }

    @Test
    void grabBeatsShieldAndCapturesTarget() throws Exception {
        BirdGame3 game = new BirdGame3();
        game.activePlayers = 2;

        Bird attacker = new Bird(100.0, BirdGame3.BirdType.PIGEON, 0, game);
        Bird defender = new Bird(170.0, BirdGame3.BirdType.EAGLE, 1, game);
        attacker.y = BirdGame3.GROUND_Y - 80.0;
        defender.y = BirdGame3.GROUND_Y - 80.0;
        attacker.facingRight = true;
        game.players[0] = attacker;
        game.players[1] = defender;

        game.setLocalActionsForKey(game.blockKeyForPlayer(1), true);
        for (int i = 0; i < 3; i++) {
            defender.update(1.0);
        }

        game.setLocalActionsForKey(game.grabKeyForPlayer(0), true);
        attacker.update(1.0);
        game.setLocalActionsForKey(game.grabKeyForPlayer(0), false);

        assertSame(defender, getPrivateObject(attacker, "grabbedTarget"));
        assertSame(attacker, getPrivateObject(defender, "grabbedBy"));
        assertFalse(defender.isBlocking);
        assertEquals(Bird.STARTING_HEALTH, defender.health, 0.0001);
    }

    @Test
    void grabbedTargetCanBeThrownUpwardAfterHoldWindow() throws Exception {
        BirdGame3 game = new BirdGame3();
        game.activePlayers = 2;

        Bird attacker = new Bird(100.0, BirdGame3.BirdType.PIGEON, 0, game);
        Bird defender = new Bird(170.0, BirdGame3.BirdType.EAGLE, 1, game);
        attacker.y = BirdGame3.GROUND_Y - 80.0;
        defender.y = BirdGame3.GROUND_Y - 80.0;
        attacker.facingRight = true;
        game.players[0] = attacker;
        game.players[1] = defender;

        game.setLocalActionsForKey(game.grabKeyForPlayer(0), true);
        attacker.update(1.0);
        game.setLocalActionsForKey(game.grabKeyForPlayer(0), false);

        double startingHealth = defender.health;
        game.setLocalActionsForKey(game.jumpKeyForPlayer(0), true);
        for (int i = 0; i < 10; i++) {
            attacker.update(1.0);
            defender.update(1.0);
        }
        game.setLocalActionsForKey(game.jumpKeyForPlayer(0), false);

        assertNull(getPrivateObject(attacker, "grabbedTarget"));
        assertNull(getPrivateObject(defender, "grabbedBy"));
        assertTrue(defender.vy < 0.0);
        assertTrue(defender.health < startingHealth);
    }

    @Test
    void defeatedHolderReleasesGrabbedTarget() throws Exception {
        BirdGame3 game = new BirdGame3();
        game.activePlayers = 2;

        Bird attacker = new Bird(100.0, BirdGame3.BirdType.PIGEON, 0, game);
        Bird defender = new Bird(170.0, BirdGame3.BirdType.EAGLE, 1, game);
        attacker.y = BirdGame3.GROUND_Y - 80.0;
        defender.y = BirdGame3.GROUND_Y - 80.0;
        attacker.facingRight = true;
        game.players[0] = attacker;
        game.players[1] = defender;

        game.setLocalActionsForKey(game.grabKeyForPlayer(0), true);
        attacker.update(1.0);
        game.setLocalActionsForKey(game.grabKeyForPlayer(0), false);

        attacker.health = 0.0;
        attacker.update(1.0);

        assertNull(getPrivateObject(attacker, "grabbedTarget"));
        assertNull(getPrivateObject(defender, "grabbedBy"));
    }

    @Test
    void holdingShieldShrinksItsVisualEvenWithoutTakingDamage() throws Exception {
        BirdGame3 game = new BirdGame3();
        game.activePlayers = 1;

        Bird defender = new Bird(190.0, BirdGame3.BirdType.EAGLE, 0, game);
        defender.y = BirdGame3.GROUND_Y - 80.0;
        game.players[0] = defender;

        game.setLocalActionsForKey(game.blockKeyForPlayer(0), true);
        for (int i = 0; i < 180; i++) {
            defender.update(1.0);
        }

        assertTrue(defender.isBlocking);
        assertEquals(60.0, getPrivateDouble(defender, "shieldHealth"), 0.0001);
        assertTrue(getPrivateDouble(defender, "shieldHoldVisual") > 0.9);
    }

    @Test
    void spotDodgeAvoidsDamageWithoutConsumingShieldDurability() throws Exception {
        BirdGame3 game = new BirdGame3();
        game.activePlayers = 1;

        Bird defender = new Bird(190.0, BirdGame3.BirdType.EAGLE, 0, game);
        defender.y = BirdGame3.GROUND_Y - 80.0;
        game.players[0] = defender;

        game.setLocalActionsForKey(game.blockKeyForPlayer(0), true);
        for (int i = 0; i < 3; i++) {
            defender.update(1.0);
        }
        double shieldBefore = getPrivateDouble(defender, "shieldHealth");

        game.setLocalActionsForKey(game.grabKeyForPlayer(0), true);
        defender.update(1.0);
        game.setLocalActionsForKey(game.grabKeyForPlayer(0), false);

        double dealtDamage = defender.receiveExternalDamage(14.0);

        assertEquals(0.0, dealtDamage, 0.0001);
        assertEquals(Bird.STARTING_HEALTH, defender.health, 0.0001);
        assertEquals(shieldBefore, getPrivateDouble(defender, "shieldHealth"), 0.0001);
        assertFalse(defender.isBlocking);
        assertEquals("SPOT", getPrivateObject(defender, "dodgeType").toString());
        assertTrue(getPrivateInt(defender, "dodgeInvulnerabilityTimer") > 0);
    }

    @Test
    void shieldRollLaunchesBirdOutOfShield() throws Exception {
        BirdGame3 game = new BirdGame3();
        game.activePlayers = 1;

        Bird defender = new Bird(190.0, BirdGame3.BirdType.EAGLE, 0, game);
        defender.y = BirdGame3.GROUND_Y - 80.0;
        game.players[0] = defender;

        game.setLocalActionsForKey(game.blockKeyForPlayer(0), true);
        for (int i = 0; i < 3; i++) {
            defender.update(1.0);
        }

        double startX = defender.x;
        game.setLocalActionsForKey(game.rightKeyForPlayer(0), true);
        defender.update(1.0);

        assertFalse(defender.isBlocking);
        assertEquals("ROLL", getPrivateObject(defender, "dodgeType").toString());
        assertTrue(defender.x > startX + 4.0);
        assertTrue(getPrivateInt(defender, "dodgeDirection") > 0);
    }

    @Test
    void shieldRollUsesVisibleRollingPose() throws Exception {
        BirdGame3 game = new BirdGame3();
        game.activePlayers = 1;

        Bird defender = new Bird(190.0, BirdGame3.BirdType.EAGLE, 0, game);
        defender.y = BirdGame3.GROUND_Y - 80.0;
        game.players[0] = defender;

        game.setLocalActionsForKey(game.blockKeyForPlayer(0), true);
        for (int i = 0; i < 3; i++) {
            defender.update(1.0);
        }

        game.setLocalActionsForKey(game.rightKeyForPlayer(0), true);
        defender.update(1.0);

        Object pose = invokePrivateObjectMethod(defender, "currentAttackVisualPose");
        assertNotNull(pose);
        assertTrue(Math.abs(invokeDoubleMethod(pose, "bodyRotationDegrees")) > 15.0,
                "Shield rolls should visibly rotate the bird instead of reading as a pure slide.");
    }

    @Test
    void attackVisualPoseBlendsTowardNewTargetInsteadOfSnapping() throws Exception {
        BirdGame3 game = new BirdGame3();
        game.activePlayers = 1;

        Bird bird = new Bird(190.0, BirdGame3.BirdType.EAGLE, 0, game);
        bird.y = BirdGame3.GROUND_Y - 80.0;
        bird.facingRight = true;
        game.players[0] = bird;

        invokePrivateObjectMethod(bird, "currentAttackVisualPose");

        game.setLocalActionsForKey(game.rightKeyForPlayer(0), true);
        game.setLocalActionsForKey(game.attackKeyForPlayer(0), true);
        bird.update(1.0);

        Object displayPose = invokePrivateObjectMethod(bird, "currentAttackVisualPose");
        Object targetPose = invokePrivateObjectMethod(bird, "currentTargetAttackVisualPose");
        double displayTranslateX = invokeDoubleMethod(displayPose, "translateX");
        double targetTranslateX = invokeDoubleMethod(targetPose, "translateX");

        assertTrue(displayTranslateX > 0.0, "The blended pose should move away from idle once an attack starts.");
        assertTrue(displayTranslateX < targetTranslateX,
                "The displayed pose should ease toward the attack target instead of snapping to it in one frame.");
    }

    @Test
    void shieldingWhileAlreadyMovingStopsBirdInsteadOfRolling() throws Exception {
        BirdGame3 game = new BirdGame3();
        game.activePlayers = 1;

        Bird defender = new Bird(190.0, BirdGame3.BirdType.EAGLE, 0, game);
        defender.y = BirdGame3.GROUND_Y - 80.0;
        game.players[0] = defender;

        game.setLocalActionsForKey(game.rightKeyForPlayer(0), true);
        for (int i = 0; i < 3; i++) {
            defender.update(1.0);
        }

        double startX = defender.x;
        game.setLocalActionsForKey(game.blockKeyForPlayer(0), true);
        defender.update(1.0);

        assertTrue(defender.isBlocking);
        assertEquals("NONE", getPrivateObject(defender, "dodgeType").toString());
        assertEquals(startX, defender.x, 0.0001);
        assertEquals(0.0, defender.vx, 0.0001);
    }

    @Test
    void airDodgeConsumesChargeUntilLanding() throws Exception {
        BirdGame3 game = new BirdGame3();
        game.activePlayers = 1;

        Bird bird = new Bird(190.0, BirdGame3.BirdType.EAGLE, 0, game);
        bird.y = BirdGame3.GROUND_Y - 240.0;
        bird.vy = 3.0;
        game.players[0] = bird;

        game.setLocalActionsForKey(game.blockKeyForPlayer(0), true);
        bird.update(1.0);
        game.setLocalActionsForKey(game.blockKeyForPlayer(0), false);

        assertEquals("AIR", getPrivateObject(bird, "dodgeType").toString());
        assertFalse(getPrivateBoolean(bird, "airDodgeAvailable"));
        assertEquals(0.0, bird.receiveExternalDamage(12.0), 0.0001);

        bird.y = BirdGame3.GROUND_Y - 80.0;
        bird.vy = 6.0;
        bird.update(1.0);

        assertEquals("NONE", getPrivateObject(bird, "dodgeType").toString());
        assertTrue(getPrivateBoolean(bird, "airDodgeAvailable"));
    }

    @Test
    void groundedJumpWaitsForJumpSquatBeforeLiftoff() throws Exception {
        BirdGame3 game = new BirdGame3();
        game.activePlayers = 1;

        Bird bird = new Bird(190.0, BirdGame3.BirdType.EAGLE, 0, game);
        bird.y = BirdGame3.GROUND_Y - 80.0;
        game.players[0] = bird;

        double startY = bird.y;
        game.setLocalActionsForKey(game.jumpKeyForPlayer(0), true);

        bird.update(1.0);
        assertEquals(startY, bird.y, 0.0001);
        assertEquals(2, getPrivateInt(bird, "jumpSquatTimer"));
        assertTrue(bird.isOnGround());

        bird.update(1.0);
        assertEquals(startY, bird.y, 0.0001);
        assertEquals(1, getPrivateInt(bird, "jumpSquatTimer"));
        assertTrue(bird.isOnGround());

        bird.update(1.0);

        assertTrue(bird.y < startY);
        assertTrue(bird.vy < 0.0);
        assertEquals(0, getPrivateInt(bird, "jumpSquatTimer"));
    }

    @Test
    void tapJumpProducesShortHopWhileHeldJumpProducesFullHop() {
        double shortHopVy = launchVelocityAfterGroundJump(1);
        double fullHopVy = launchVelocityAfterGroundJump(3);

        assertTrue(shortHopVy < fullHopVy * 0.8,
                "Short hop should launch lower than a full hop.");
    }

    @Test
    void heldGroundJumpDoesNotConsumePigeonDoubleJump() {
        BirdGame3 game = new BirdGame3();
        game.activePlayers = 1;

        Bird pigeon = new Bird(190.0, BirdGame3.BirdType.PIGEON, 0, game);
        pigeon.y = BirdGame3.GROUND_Y - 80.0;
        game.players[0] = pigeon;

        game.setLocalActionsForKey(game.jumpKeyForPlayer(0), true);
        for (int i = 0; i < 4; i++) {
            pigeon.update(1.0);
        }

        assertFalse(pigeon.isOnGround());
        assertTrue(pigeon.canDoubleJump, "Holding jump through takeoff should not auto-spend the double jump.");
    }

    @Test
    void pigeonJumpInputAloneStillStartsNormalJump() throws Exception {
        BirdGame3 game = new BirdGame3();
        game.activePlayers = 1;

        Bird pigeon = new Bird(190.0, BirdGame3.BirdType.PIGEON, 0, game);
        pigeon.y = BirdGame3.GROUND_Y - 80.0;
        game.players[0] = pigeon;

        double startY = pigeon.y;
        game.setLocalActionsForKey(game.jumpKeyForPlayer(0), true);
        for (int i = 0; i < 4; i++) {
            pigeon.update(1.0);
        }

        assertTrue(pigeon.y < startY, "Pressing up alone should still make Pigeon jump.");
        assertEquals(0, getPrivateInt(pigeon, "pigeonFlutterTimer"),
                "Jump input by itself should not start Pigeon's recovery.");
        assertTrue(pigeon.canDoubleJump, "A normal jump should preserve Pigeon's extra jump.");
    }

    @Test
    void pigeonNeutralSpecialFiresFeatherBurstWithoutHealingAndCannotBeSpammed() throws Exception {
        BirdGame3 game = new BirdGame3();
        game.activePlayers = 2;

        Bird pigeon = new Bird(100.0, BirdGame3.BirdType.PIGEON, 0, game);
        Bird target = new Bird(185.0, BirdGame3.BirdType.EAGLE, 1, game);
        pigeon.y = BirdGame3.GROUND_Y - 80.0;
        target.y = BirdGame3.GROUND_Y - 80.0;
        pigeon.facingRight = true;
        pigeon.health = 60.0;
        game.players[0] = pigeon;
        game.players[1] = target;

        double startingHealth = target.health;
        game.setLocalActionsForKey(game.specialKeyForPlayer(0), true);
        pigeon.update(1.0);
        game.setLocalActionsForKey(game.specialKeyForPlayer(0), false);

        assertEquals(startingHealth - 4.0, target.health, 0.0001,
                "Neutral special should hit for the lighter damage value.");
        assertEquals(60.0, pigeon.health, 0.0001, "Neutral special should not heal Pigeon.");
        assertTrue(getPrivateInt(pigeon, "specialCooldown") > 0,
                "Neutral special should apply an anti-spam cooldown.");

        double afterFirstBurstHealth = target.health;
        target.x = 185.0;
        target.y = BirdGame3.GROUND_Y - 80.0;
        target.vx = 0.0;
        target.vy = 0.0;
        pigeon.update(1.0);
        game.setLocalActionsForKey(game.specialKeyForPlayer(0), true);
        pigeon.update(1.0);
        game.setLocalActionsForKey(game.specialKeyForPlayer(0), false);

        assertEquals(afterFirstBurstHealth, target.health, 0.0001,
                "Neutral special should have enough lockout to prevent immediate spam.");
    }

    @Test
    void pigeonSideSpecialUsesDirectionalInputForDashStrike() {
        BirdGame3 game = new BirdGame3();
        game.activePlayers = 2;

        Bird pigeon = new Bird(100.0, BirdGame3.BirdType.PIGEON, 0, game);
        Bird target = new Bird(200.0, BirdGame3.BirdType.EAGLE, 1, game);
        pigeon.y = BirdGame3.GROUND_Y - 80.0;
        target.y = BirdGame3.GROUND_Y - 80.0;
        pigeon.facingRight = true;
        game.players[0] = pigeon;
        game.players[1] = target;

        double startingHealth = target.health;
        game.setLocalActionsForKey(game.rightKeyForPlayer(0), true);
        game.setLocalActionsForKey(game.specialKeyForPlayer(0), true);
        pigeon.update(1.0);
        game.setLocalActionsForKey(game.specialKeyForPlayer(0), false);
        game.setLocalActionsForKey(game.rightKeyForPlayer(0), false);

        assertTrue(pigeon.vx > 17.0, "Side special should commit Pigeon to a faster horizontal burst.");
        assertEquals(startingHealth - 3.0, target.health, 0.0001,
                "Side special should now deal lighter damage.");
        assertTrue(target.vy < -8.0, "Side special should launch targets much higher than before.");
    }

    @Test
    void pigeonSideSpecialTravelsMuchFartherForLessDamage() {
        BirdGame3 game = new BirdGame3();
        game.activePlayers = 2;

        Bird pigeon = new Bird(100.0, BirdGame3.BirdType.PIGEON, 0, game);
        Bird target = new Bird(430.0, BirdGame3.BirdType.EAGLE, 1, game);
        pigeon.y = BirdGame3.GROUND_Y - 80.0;
        target.y = BirdGame3.GROUND_Y - 80.0;
        pigeon.facingRight = true;
        game.players[0] = pigeon;
        game.players[1] = target;

        game.setLocalActionsForKey(game.rightKeyForPlayer(0), true);
        game.setLocalActionsForKey(game.specialKeyForPlayer(0), true);
        pigeon.update(1.0);
        game.setLocalActionsForKey(game.specialKeyForPlayer(0), false);
        game.setLocalActionsForKey(game.rightKeyForPlayer(0), false);

        for (int i = 0; i < 24; i++) {
            pigeon.update(1.0);
        }

        assertEquals(Bird.STARTING_HEALTH - 3.0, target.health, 0.0001,
                "Side special should trade damage for Fox-style travel distance.");
        assertTrue(target.vy < -8.0, "The long rush should still send targets much higher on hit.");
    }

    @Test
    void pigeonUpSpecialOverridesGroundJumpAndStartsFlutter() throws Exception {
        BirdGame3 game = new BirdGame3();
        game.activePlayers = 1;

        Bird pigeon = new Bird(190.0, BirdGame3.BirdType.PIGEON, 0, game);
        pigeon.y = BirdGame3.GROUND_Y - 80.0;
        game.players[0] = pigeon;

        game.setLocalActionsForKey(game.jumpKeyForPlayer(0), true);
        game.setLocalActionsForKey(game.specialKeyForPlayer(0), true);
        pigeon.update(1.0);
        game.setLocalActionsForKey(game.specialKeyForPlayer(0), false);
        game.setLocalActionsForKey(game.jumpKeyForPlayer(0), false);

        assertEquals(0, getPrivateInt(pigeon, "jumpSquatTimer"),
                "Up special should bypass jump squat instead of becoming a normal jump.");
        assertTrue(getPrivateInt(pigeon, "pigeonFlutterTimer") > 0);
        assertTrue(getPrivateBoolean(pigeon, "pigeonUpSpecialUsed"));
        assertFalse(pigeon.canDoubleJump, "Up special should spend Pigeon's remaining air recovery.");
        assertTrue(pigeon.vy < -8.0, "Up special should launch Pigeon upward immediately.");
    }

    @Test
    void pigeonGroundDownSpecialUsesBlockInputWithoutRaisingShieldAndDoesNotHealOnCompletion() {
        BirdGame3 game = new BirdGame3();
        game.activePlayers = 1;

        Bird pigeon = new Bird(190.0, BirdGame3.BirdType.PIGEON, 0, game);
        pigeon.y = BirdGame3.GROUND_Y - 80.0;
        pigeon.health = 48.0;
        game.players[0] = pigeon;

        game.setLocalActionsForKey(game.blockKeyForPlayer(0), true);
        game.setLocalActionsForKey(game.specialKeyForPlayer(0), true);
        pigeon.update(1.0);
        game.setLocalActionsForKey(game.blockKeyForPlayer(0), false);
        game.setLocalActionsForKey(game.specialKeyForPlayer(0), false);

        assertFalse(pigeon.isBlocking, "Down special should reserve block input instead of raising shield.");
        for (int i = 0; i < 120; i++) {
            pigeon.update(1.0);
        }

        assertEquals(48.0, pigeon.health, 0.0001,
                "Grounded scavenge should not restore health before it resolves.");
        for (int i = 0; i < 50; i++) {
            pigeon.update(1.0);
        }

        assertEquals(48.0, pigeon.health, 0.0001,
                "Completing the grounded scavenge should not heal Pigeon.");
    }

    @Test
    void pigeonShieldedSpecialConvertsIntoGroundDownSpecialWithoutHealing() throws Exception {
        BirdGame3 game = new BirdGame3();
        game.activePlayers = 1;

        Bird pigeon = new Bird(190.0, BirdGame3.BirdType.PIGEON, 0, game);
        pigeon.y = BirdGame3.GROUND_Y - 80.0;
        pigeon.health = 48.0;
        game.players[0] = pigeon;

        game.setLocalActionsForKey(game.blockKeyForPlayer(0), true);
        pigeon.update(1.0);
        assertTrue(pigeon.isBlocking, "Setup should place Pigeon into shield first.");

        game.setLocalActionsForKey(game.specialKeyForPlayer(0), true);
        pigeon.update(1.0);
        game.setLocalActionsForKey(game.specialKeyForPlayer(0), false);
        game.setLocalActionsForKey(game.blockKeyForPlayer(0), false);

        assertFalse(pigeon.isBlocking, "Pressing special out of shield should drop shield into down special.");
        assertTrue(getPrivateInt(pigeon, "pigeonScavengeTimer") > 0,
                "Special while shielding should activate grounded scavenge.");

        for (int i = 0; i < 170; i++) {
            pigeon.update(1.0);
        }

        assertEquals(48.0, pigeon.health, 0.0001,
                "Shield-canceled down special should not heal on completion.");
    }

    @Test
    void pigeonGroundDownSpecialDoesNotHealIfInterrupted() {
        BirdGame3 game = new BirdGame3();
        game.activePlayers = 1;

        Bird pigeon = new Bird(190.0, BirdGame3.BirdType.PIGEON, 0, game);
        pigeon.y = BirdGame3.GROUND_Y - 80.0;
        pigeon.health = 48.0;
        game.players[0] = pigeon;

        game.setLocalActionsForKey(game.blockKeyForPlayer(0), true);
        game.setLocalActionsForKey(game.specialKeyForPlayer(0), true);
        pigeon.update(1.0);
        game.setLocalActionsForKey(game.blockKeyForPlayer(0), false);
        game.setLocalActionsForKey(game.specialKeyForPlayer(0), false);

        for (int i = 0; i < 8; i++) {
            pigeon.update(1.0);
        }
        double damageTaken = pigeon.receiveExternalDamage(5.0);
        assertTrue(damageTaken > 0.0, "The interruption check needs Pigeon to actually take damage.");
        double healthAfterInterruption = pigeon.health;

        for (int i = 0; i < 170; i++) {
            pigeon.update(1.0);
        }

        assertEquals(healthAfterInterruption, pigeon.health, 0.0001,
                "Interrupted scavenge should not heal Pigeon afterward.");
    }

    @Test
    void pigeonAirDownSpecialStallsAndDropsAHitboxBelow() {
        BirdGame3 game = new BirdGame3();
        game.activePlayers = 2;

        Bird pigeon = new Bird(160.0, BirdGame3.BirdType.PIGEON, 0, game);
        Bird target = new Bird(170.0, BirdGame3.BirdType.EAGLE, 1, game);
        pigeon.y = BirdGame3.GROUND_Y - 280.0;
        target.y = BirdGame3.GROUND_Y - 170.0;
        game.players[0] = pigeon;
        game.players[1] = target;

        double startingHealth = target.health;
        game.setLocalActionsForKey(game.blockKeyForPlayer(0), true);
        game.setLocalActionsForKey(game.specialKeyForPlayer(0), true);
        pigeon.update(1.0);
        game.setLocalActionsForKey(game.blockKeyForPlayer(0), false);
        game.setLocalActionsForKey(game.specialKeyForPlayer(0), false);

        assertTrue(pigeon.vy < 3.0, "Air down special should stall Pigeon's fall before the drop peck.");
        for (int i = 0; i < 12; i++) {
            pigeon.update(1.0);
        }

        assertTrue(target.health < startingHealth, "Air down special should damage targets below Pigeon.");
        assertTrue(target.vy > 0.0, "Air down special should knock targets downward.");
    }

    @Test
    void phoenixFullMeterSpecialTriggersRebirthNova() throws Exception {
        BirdGame3 game = new BirdGame3();
        game.activePlayers = 2;

        Bird phoenix = new Bird(160.0, BirdGame3.BirdType.PHOENIX, 0, game);
        Bird target = new Bird(240.0, BirdGame3.BirdType.PIGEON, 1, game);
        phoenix.y = BirdGame3.GROUND_Y - 80.0;
        target.y = BirdGame3.GROUND_Y - 80.0;
        game.players[0] = phoenix;
        game.players[1] = target;

        setPrivateDouble(phoenix, "ultimateMeter", 100.0);
        double startingHealth = target.health;

        game.setLocalActionsForKey(game.specialKeyForPlayer(0), true);
        phoenix.update(1.0);

        assertFalse(getPrivateBoolean(phoenix, "phoenixCharging"));
        assertEquals(Bird.PHOENIX_REBIRTH_NOVA_TOTAL_FRAMES - 1,
                getPrivateInt(phoenix, "phoenixRebirthNovaTimer"));
        assertEquals(0.0, phoenix.getUltimateRatio(), 0.0001,
                "Starting Rebirth Nova should consume Phoenix's ultimate meter.");

        for (int i = 0; i < Bird.PHOENIX_REBIRTH_NOVA_WINDUP_FRAMES + 2; i++) {
            phoenix.update(1.0);
        }

        game.setLocalActionsForKey(game.specialKeyForPlayer(0), false);
        phoenix.update(1.0);

        assertTrue(getPrivateBoolean(phoenix, "phoenixRebirthNovaDetonated"));
        assertTrue(getPrivateInt(phoenix, "phoenixRebirthNovaBuffTimer") > 0,
                "Rebirth Nova should leave Phoenix in its reborn flame buff.");
        assertEquals(0, phoenix.specialCooldown,
                "Rebirth Nova should not leave a visible special cooldown.");
        assertEquals(0, getPrivateInt(phoenix, "phoenixAfterburnTimer"),
                "Rebirth Nova should not leave Phoenix with a lingering damaging afterburn.");
        assertTrue(target.health < startingHealth,
                "Rebirth Nova should damage nearby enemies when it detonates.");
    }

    @Test
    void phoenixNeutralChargeDamageAndKnockbackScaleWithHeldFramesOnlyOnBurst() throws Exception {
        BirdGame3 quickGame = new BirdGame3();
        quickGame.activePlayers = 2;
        Bird quickPhoenix = new Bird(160.0, BirdGame3.BirdType.PHOENIX, 0, quickGame);
        Bird quickTarget = new Bird(222.0, BirdGame3.BirdType.PIGEON, 1, quickGame);
        quickPhoenix.y = BirdGame3.GROUND_Y - 80.0;
        quickTarget.y = BirdGame3.GROUND_Y - 80.0;
        quickGame.players[0] = quickPhoenix;
        quickGame.players[1] = quickTarget;

        PhoenixSpecials.neutral(quickPhoenix, false);
        setPrivateInt(quickPhoenix, "phoenixChargeTimer", 5);
        PhoenixSpecials.releaseCharge(quickPhoenix);
        double quickDamage = Bird.STARTING_HEALTH - quickTarget.health;
        double quickKnockback = Math.hypot(quickTarget.vx, quickTarget.vy);

        BirdGame3 chargedGame = new BirdGame3();
        chargedGame.activePlayers = 2;
        Bird chargedPhoenix = new Bird(160.0, BirdGame3.BirdType.PHOENIX, 0, chargedGame);
        Bird chargedTarget = new Bird(222.0, BirdGame3.BirdType.PIGEON, 1, chargedGame);
        chargedPhoenix.y = BirdGame3.GROUND_Y - 80.0;
        chargedTarget.y = BirdGame3.GROUND_Y - 80.0;
        chargedGame.players[0] = chargedPhoenix;
        chargedGame.players[1] = chargedTarget;

        PhoenixSpecials.neutral(chargedPhoenix, false);
        setPrivateInt(chargedPhoenix, "phoenixChargeTimer", 70);
        PhoenixSpecials.releaseCharge(chargedPhoenix);
        double chargedDamage = Bird.STARTING_HEALTH - chargedTarget.health;
        double chargedKnockback = Math.hypot(chargedTarget.vx, chargedTarget.vy);

        assertTrue(chargedDamage > quickDamage,
                "A longer Phoenix neutral charge should deal more burst damage.");
        assertTrue(chargedKnockback > quickKnockback,
                "A longer Phoenix neutral charge should launch harder on the burst.");
        assertEquals(0, getPrivateInt(chargedPhoenix, "phoenixAfterburnTimer"),
                "The neutral special should not transition into a lingering afterburn hitbox.");
    }

    @Test
    void phoenixNeutralSpecialCannotBeImmediatelySpamRestarted() throws Exception {
        BirdGame3 game = new BirdGame3();
        game.activePlayers = 1;

        Bird phoenix = new Bird(160.0, BirdGame3.BirdType.PHOENIX, 0, game);
        phoenix.y = BirdGame3.GROUND_Y - 80.0;
        game.players[0] = phoenix;

        KeyCode specialKey = game.specialKeyForPlayer(0);
        game.setLocalActionsForKey(specialKey, true);
        phoenix.update(1.0);
        assertTrue(getPrivateBoolean(phoenix, "phoenixCharging"));

        game.setLocalActionsForKey(specialKey, false);
        phoenix.update(1.0);
        assertFalse(getPrivateBoolean(phoenix, "phoenixCharging"));
        assertTrue(getPrivateInt(phoenix, "phoenixNeutralReuseTimer") > 0,
                "The burst should start a short neutral-special reuse gate.");

        game.setLocalActionsForKey(specialKey, true);
        phoenix.update(1.0);

        assertFalse(getPrivateBoolean(phoenix, "phoenixCharging"),
                "Phoenix should not be able to immediately restart neutral special after bursting.");
    }

    @Test
    void phoenixUpSpecialUltimateDealsMoreDamageThanBaseVersion() throws Exception {
        BirdGame3 normalGame = new BirdGame3();
        normalGame.activePlayers = 2;
        Bird normalPhoenix = new Bird(160.0, BirdGame3.BirdType.PHOENIX, 0, normalGame);
        Bird normalTarget = new Bird(166.0, BirdGame3.BirdType.PIGEON, 1, normalGame);
        normalPhoenix.y = BirdGame3.GROUND_Y - 80.0;
        normalTarget.y = BirdGame3.GROUND_Y - 80.0;
        normalGame.players[0] = normalPhoenix;
        normalGame.players[1] = normalTarget;

        PhoenixSpecials.up(normalPhoenix, false);
        assertEquals(0, normalPhoenix.specialCooldown);
        normalPhoenix.update(1.0);
        double normalDamage = Bird.STARTING_HEALTH - normalTarget.health;

        BirdGame3 ultimateGame = new BirdGame3();
        ultimateGame.activePlayers = 2;
        Bird ultimatePhoenix = new Bird(160.0, BirdGame3.BirdType.PHOENIX, 0, ultimateGame);
        Bird ultimateTarget = new Bird(166.0, BirdGame3.BirdType.PIGEON, 1, ultimateGame);
        ultimatePhoenix.y = BirdGame3.GROUND_Y - 80.0;
        ultimateTarget.y = BirdGame3.GROUND_Y - 80.0;
        ultimateGame.players[0] = ultimatePhoenix;
        ultimateGame.players[1] = ultimateTarget;

        PhoenixSpecials.up(ultimatePhoenix, true);
        assertEquals(0, ultimatePhoenix.specialCooldown);
        ultimatePhoenix.update(1.0);
        double ultimateDamage = Bird.STARTING_HEALTH - ultimateTarget.health;

        assertTrue(ultimateDamage > normalDamage,
                "Helix Ascent should hit harder than the base Firespin.");
    }

    @Test
    void phoenixUpSpecialCarriesCaughtTargetsUpwardWithTickDamage() throws Exception {
        BirdGame3 game = new BirdGame3();
        game.activePlayers = 2;

        Bird phoenix = new Bird(160.0, BirdGame3.BirdType.PHOENIX, 0, game);
        Bird target = new Bird(164.0, BirdGame3.BirdType.PIGEON, 1, game);
        phoenix.y = BirdGame3.GROUND_Y - 80.0;
        target.y = BirdGame3.GROUND_Y - 80.0;
        game.players[0] = phoenix;
        game.players[1] = target;

        PhoenixSpecials.up(phoenix, false);
        double startingHealth = target.health;
        for (int i = 0; i < 8; i++) {
            phoenix.update(1.0);
        }

        assertTrue(target.health < startingHealth,
                "Firespin should repeatedly burn enemies caught in the rising flame column.");
        assertTrue(target.vy < 0.0,
                "Enemies caught by Firespin should be carried upward with the flames.");
    }

    @Test
    void phoenixGroundedUpSpecialContinuesAsRecoveryAfterLeavingGround() throws Exception {
        BirdGame3 game = new BirdGame3();
        game.activePlayers = 1;

        Bird phoenix = new Bird(160.0, BirdGame3.BirdType.PHOENIX, 0, game);
        phoenix.y = BirdGame3.GROUND_Y - 80.0;
        game.players[0] = phoenix;

        game.setLocalActionsForKey(game.jumpKeyForPlayer(0), true);
        game.setLocalActionsForKey(game.specialKeyForPlayer(0), true);
        phoenix.update(1.0);
        game.setLocalActionsForKey(game.jumpKeyForPlayer(0), false);
        game.setLocalActionsForKey(game.specialKeyForPlayer(0), false);

        assertTrue(getPrivateInt(phoenix, "phoenixSpiralTimer") > 0,
                "Grounded Phoenix up special should not cancel itself before the launch frame.");
        assertTrue(phoenix.vy < 0.0,
                "Grounded Phoenix up special should launch upward like the aerial version.");
    }

    @Test
    void phoenixUpSpecialStaysSpentWhenInterruptedInMidair() throws Exception {
        BirdGame3 game = new BirdGame3();
        game.activePlayers = 1;

        Bird phoenix = new Bird(190.0, BirdGame3.BirdType.PHOENIX, 0, game);
        phoenix.y = BirdGame3.GROUND_Y - 80.0;
        game.players[0] = phoenix;

        PhoenixSpecials.up(phoenix, false);
        assertTrue(getPrivateBoolean(phoenix, "phoenixSpiralUsed"));

        phoenix.y = BirdGame3.GROUND_Y - 260.0;
        phoenix.stunTime = 4.0;
        phoenix.update(1.0);

        assertTrue(getPrivateBoolean(phoenix, "phoenixSpiralUsed"),
                "Getting clipped out of Phoenix's recovery should not refresh the move for free.");

        while (phoenix.stunTime > 0.0) {
            phoenix.update(1.0);
        }

        game.setLocalActionsForKey(game.jumpKeyForPlayer(0), true);
        game.setLocalActionsForKey(game.specialKeyForPlayer(0), true);
        phoenix.update(1.0);
        game.setLocalActionsForKey(game.specialKeyForPlayer(0), false);
        game.setLocalActionsForKey(game.jumpKeyForPlayer(0), false);

        assertEquals(0, getPrivateInt(phoenix, "phoenixSpiralTimer"),
                "Phoenix should not restart its up special until it lands.");

        phoenix.y = BirdGame3.GROUND_Y - 80.0;
        phoenix.vx = 0.0;
        phoenix.vy = 0.0;
        phoenix.update(1.0);

        assertFalse(getPrivateBoolean(phoenix, "phoenixSpiralUsed"),
                "Landing should refresh Phoenix's spent up-special flag even if the move was interrupted.");
    }

    @Test
    void phoenixSideSpecialHasNoCooldownAndWaitsForHeadTiltBeforeShot() throws Exception {
        BirdGame3 game = new BirdGame3();
        game.activePlayers = 1;

        Bird phoenix = new Bird(220.0, BirdGame3.BirdType.PHOENIX, 0, game);
        phoenix.y = BirdGame3.GROUND_Y - 80.0;
        phoenix.facingRight = true;
        game.players[0] = phoenix;

        PhoenixSpecials.side(phoenix, false);

        assertEquals(0, phoenix.specialCooldown);
        assertTrue(getPrivateInt(phoenix, "phoenixCastLockTimer") > 0);

        double startX = phoenix.x;
        phoenix.vx = 4.5;
        phoenix.update(1.0);

        assertEquals(startX, phoenix.x, 0.0001,
                "Phoenix should stay planted while Snap Fire is in its cast lock.");
        assertTrue(getPrivateInt(phoenix, "phoenixCastLockTimer") > 0,
                "Snap Fire should spend its first frames tilting Phoenix's head up before the shot launches.");
        double windupX = getPrivateDouble(phoenix, "phoenixFireballX");

        while (getPrivateInt(phoenix, "phoenixCastLockTimer") > 0) {
            phoenix.update(1.0);
        }
        phoenix.update(1.0);

        assertTrue(getPrivateDouble(phoenix, "phoenixFireballX") > windupX + 12.0,
                "After the head-tilt windup, Snap Fire should launch forward as a projectile.");
    }

    @Test
    void phoenixSideSpecialCannotBeImmediatelySpamRestarted() throws Exception {
        BirdGame3 game = new BirdGame3();
        game.activePlayers = 1;

        Bird phoenix = new Bird(220.0, BirdGame3.BirdType.PHOENIX, 0, game);
        phoenix.y = BirdGame3.GROUND_Y - 80.0;
        phoenix.facingRight = true;
        game.players[0] = phoenix;

        KeyCode rightKey = game.rightKeyForPlayer(0);
        KeyCode specialKey = game.specialKeyForPlayer(0);
        game.setLocalActionsForKey(rightKey, true);
        game.setLocalActionsForKey(specialKey, true);
        phoenix.update(1.0);
        game.setLocalActionsForKey(specialKey, false);

        assertTrue(getPrivateInt(phoenix, "phoenixFireballReuseTimer") > 0);
        while (getPrivateInt(phoenix, "phoenixFireballTimer") > 0) {
            phoenix.update(1.0);
        }
        assertTrue(getPrivateInt(phoenix, "phoenixFireballReuseTimer") > 0,
                "The side-special reuse gate should outlast the projectile.");

        game.setLocalActionsForKey(specialKey, true);
        phoenix.update(1.0);

        assertEquals(0, getPrivateInt(phoenix, "phoenixFireballTimer"),
                "Phoenix should not be able to immediately restart side special after a shot.");
    }

    @Test
    void phoenixAirSideSpecialShootsDiagonallyDown() throws Exception {
        BirdGame3 game = new BirdGame3();
        game.activePlayers = 1;

        Bird phoenix = new Bird(220.0, BirdGame3.BirdType.PHOENIX, 0, game);
        phoenix.y = BirdGame3.GROUND_Y - 300.0;
        phoenix.facingRight = true;
        game.players[0] = phoenix;

        PhoenixSpecials.side(phoenix, false);

        assertTrue(getPrivateDouble(phoenix, "phoenixFireballVY") > 0.0,
                "Air Snap Fire should be aimed diagonally down.");

        while (getPrivateInt(phoenix, "phoenixCastLockTimer") > 0) {
            phoenix.update(1.0);
        }
        double launchX = getPrivateDouble(phoenix, "phoenixFireballX");
        double launchY = getPrivateDouble(phoenix, "phoenixFireballY");

        phoenix.update(1.0);

        assertTrue(getPrivateDouble(phoenix, "phoenixFireballX") > launchX,
                "Air Snap Fire should still travel forward.");
        assertTrue(getPrivateDouble(phoenix, "phoenixFireballY") > launchY,
                "Air Snap Fire should travel downward after the windup.");
    }

    @Test
    void phoenixSideSpecialTravelsFartherThenFizzlesHarmlessly() throws Exception {
        BirdGame3 game = new BirdGame3();
        game.activePlayers = 1;

        Bird phoenix = new Bird(220.0, BirdGame3.BirdType.PHOENIX, 0, game);
        phoenix.y = BirdGame3.GROUND_Y - 80.0;
        phoenix.facingRight = true;
        game.players[0] = phoenix;

        PhoenixSpecials.side(phoenix, false);
        while (getPrivateInt(phoenix, "phoenixCastLockTimer") > 0) {
            phoenix.update(1.0);
        }
        double launchX = getPrivateDouble(phoenix, "phoenixFireballX");

        int guard = 0;
        while (getPrivateInt(phoenix, "phoenixFireballTimer") > 0 && guard++ < 90) {
            phoenix.update(1.0);
        }

        assertEquals(0, getPrivateInt(phoenix, "phoenixFireballTimer"),
                "Snap Fire should leave its damaging state after max range.");
        assertTrue(getPrivateDouble(phoenix, "phoenixFireballX") > launchX + 380.0,
                "Snap Fire should travel meaningfully farther before it fizzles.");
        assertTrue(getPrivateInt(phoenix, "phoenixFireballFizzleTimer") > 0,
                "Snap Fire should enter a short visible fizzle instead of vanishing.");

        Bird target = new Bird(getPrivateDouble(phoenix, "phoenixFireballX") - 40.0,
                BirdGame3.BirdType.PIGEON, 1, game);
        target.y = getPrivateDouble(phoenix, "phoenixFireballY") - 40.0;
        game.activePlayers = 2;
        game.players[1] = target;
        double healthBefore = target.health;

        for (int i = 0; i < 6; i++) {
            phoenix.update(1.0);
        }

        assertEquals(healthBefore, target.health, 0.0001,
                "The fizzle tail should be visual only and must not keep a lingering hitbox.");
    }

    @Test
    void phoenixAirSideSpecialPrimesDiagonalPoseAndLandingFlare() throws Exception {
        BirdGame3 game = new BirdGame3();
        game.activePlayers = 1;

        Bird phoenix = new Bird(220.0, BirdGame3.BirdType.PHOENIX, 0, game);
        phoenix.y = BirdGame3.GROUND_Y - 300.0;
        phoenix.facingRight = true;
        game.players[0] = phoenix;

        PhoenixSpecials.side(phoenix, false);

        assertTrue(getPrivateInt(phoenix, "phoenixAirSideAimPoseTimer") > 0,
                "Air Snap Fire should hold a diagonal-down aim pose.");
        assertTrue(getPrivateInt(phoenix, "phoenixAirSideLandingPrimeTimer") > 0,
                "Air Snap Fire should prime a landing flare.");

        phoenix.y = BirdGame3.GROUND_Y - 80.0;
        phoenix.vy = 0.0;
        phoenix.update(1.0);

        assertEquals(0, getPrivateInt(phoenix, "phoenixAirSideLandingPrimeTimer"),
                "The landing flare should consume the primed landing cue.");
        assertTrue(getPrivateInt(phoenix, "phoenixAirSideLandingFxTimer") > 0,
                "Landing after Air Snap Fire should play a brief ember skid.");
    }

    @Test
    void phoenixGroundDownSpecialEruptsVerticallyInsteadOfSpreadingOutward() throws Exception {
        BirdGame3 game = new BirdGame3();
        game.activePlayers = 3;

        Bird phoenix = new Bird(260.0, BirdGame3.BirdType.PHOENIX, 0, game);
        Bird centerTarget = new Bird(262.0, BirdGame3.BirdType.PIGEON, 1, game);
        Bird sideTarget = new Bird(420.0, BirdGame3.BirdType.EAGLE, 2, game);
        phoenix.y = BirdGame3.GROUND_Y - 80.0;
        centerTarget.y = BirdGame3.GROUND_Y - 80.0;
        sideTarget.y = BirdGame3.GROUND_Y - 80.0;
        game.players[0] = phoenix;
        game.players[1] = centerTarget;
        game.players[2] = sideTarget;

        PhoenixSpecials.down(phoenix, false);

        assertEquals(0, phoenix.specialCooldown);
        assertFalse(getPrivateBoolean(phoenix, "phoenixLavaAirborne"));
        assertTrue(getPrivateInt(phoenix, "phoenixLavaReuseTimer") > 0,
                "Ground Faultfire should use an invisible reuse timer instead of the visible cooldown bar.");

        double centerStart = centerTarget.health;
        double sideStart = sideTarget.health;
        for (int i = 0; i < 20; i++) {
            phoenix.update(1.0);
        }

        assertTrue(centerTarget.health < centerStart,
                "Ground Faultfire should erupt under targets close to Phoenix.");
        assertEquals(sideStart, sideTarget.health, 0.0001,
                "Ground Faultfire should no longer spread outward across the floor.");
        assertTrue(centerTarget.vy < 0.0,
                "The eruption should launch caught targets upward.");
    }

    @Test
    void phoenixInvisibleReuseTimersDoNotTriggerCooldownFlash() {
        BirdGame3 game = new BirdGame3();
        game.activePlayers = 1;

        Bird phoenix = new Bird(260.0, BirdGame3.BirdType.PHOENIX, 0, game);
        phoenix.y = BirdGame3.GROUND_Y - 80.0;
        game.players[0] = phoenix;

        KeyCode blockKey = game.blockKeyForPlayer(0);
        KeyCode specialKey = game.specialKeyForPlayer(0);
        game.setLocalActionsForKey(blockKey, true);
        game.setLocalActionsForKey(specialKey, true);
        phoenix.update(1.0);

        game.setLocalActionsForKey(specialKey, false);
        phoenix.update(1.0);

        game.setLocalActionsForKey(specialKey, true);
        phoenix.update(1.0);

        assertEquals(0, phoenix.specialCooldown,
                "Phoenix down special should keep its cooldown invisible.");
        assertEquals(0, phoenix.cooldownFlash,
                "Phoenix reuse lockouts should not display the red cooldown warning.");
    }

    @Test
    void phoenixAirDownSpecialBurnsTargetsDirectlyBelow() throws Exception {
        BirdGame3 game = new BirdGame3();
        game.activePlayers = 3;

        Bird phoenix = new Bird(220.0, BirdGame3.BirdType.PHOENIX, 0, game);
        Bird belowTarget = new Bird(222.0, BirdGame3.BirdType.PIGEON, 1, game);
        Bird sideTarget = new Bird(340.0, BirdGame3.BirdType.EAGLE, 2, game);
        phoenix.y = BirdGame3.GROUND_Y - 300.0;
        belowTarget.y = BirdGame3.GROUND_Y - 110.0;
        sideTarget.y = BirdGame3.GROUND_Y - 110.0;
        game.players[0] = phoenix;
        game.players[1] = belowTarget;
        game.players[2] = sideTarget;

        PhoenixSpecials.down(phoenix, false);

        assertTrue(getPrivateBoolean(phoenix, "phoenixLavaAirborne"));

        double belowStart = belowTarget.health;
        double sideStart = sideTarget.health;
        for (int i = 0; i < 12; i++) {
            phoenix.update(1.0);
        }

        assertTrue(belowTarget.health < belowStart,
                "Air Faultfire should damage targets directly below Phoenix.");
        assertEquals(sideStart, sideTarget.health, 0.0001,
                "Air Faultfire should stay in a narrow vertical lane instead of splashing sideways.");
        assertTrue(belowTarget.vy > 0.0,
                "The vertical flame stream should force targets downward.");
    }

    @Test
    void phoenixAirDownSpecialExtendsWhileSpecialIsHeld() throws Exception {
        BirdGame3 game = new BirdGame3();
        game.activePlayers = 1;

        Bird phoenix = new Bird(220.0, BirdGame3.BirdType.PHOENIX, 0, game);
        phoenix.y = BirdGame3.GROUND_Y - 300.0;
        game.players[0] = phoenix;

        PhoenixSpecials.down(phoenix, false);
        game.setLocalActionsForKey(game.specialKeyForPlayer(0), true);

        for (int i = 0; i < Bird.PHOENIX_LAVA_FRAMES + 16; i++) {
            phoenix.update(1.0);
        }

        assertTrue(getPrivateInt(phoenix, "phoenixLavaTimer") > 0,
                "Holding special in the air should sustain Faultfire past its old fixed duration.");
        assertTrue(getPrivateInt(phoenix, "phoenixLavaHoldFrames") > 0,
                "The held air stream should track held frames for deterministic sync.");

        game.setLocalActionsForKey(game.specialKeyForPlayer(0), false);
        for (int i = 0; i < Bird.PHOENIX_LAVA_FRAMES + 4; i++) {
            phoenix.update(1.0);
        }

        assertEquals(0, getPrivateInt(phoenix, "phoenixLavaTimer"),
                "Air Faultfire should expire after the player releases special.");
    }

    @Test
    void eagleNeutralSpecialUsesHuntersCryConeAndStartsInvisibleReuseTimer() throws Exception {
        BirdGame3 game = new BirdGame3();
        game.activePlayers = 2;

        Bird eagle = new Bird(100.0, BirdGame3.BirdType.EAGLE, 0, game);
        Bird target = new Bird(195.0, BirdGame3.BirdType.PIGEON, 1, game);
        eagle.y = BirdGame3.GROUND_Y - 80.0;
        target.y = BirdGame3.GROUND_Y - 80.0;
        eagle.facingRight = true;
        game.players[0] = eagle;
        game.players[1] = target;

        double startingHealth = target.health;
        game.setLocalActionsForKey(game.specialKeyForPlayer(0), true);
        eagle.update(1.0);
        game.setLocalActionsForKey(game.specialKeyForPlayer(0), false);

        assertEquals(startingHealth - 8.0, target.health, 0.0001,
                "Hunter's Cry should deal Eagle's heavier neutral-special damage.");
        assertTrue(getPrivateInt(eagle, "raptorCryTimer") > 0);
        assertTrue(getPrivateInt(eagle, "raptorCryReuseTimer") > 0);
        assertEquals(0, getPrivateInt(eagle, "specialCooldown"),
                "Hunter's Cry should no longer use the visible special cooldown bar.");
        assertTrue(target.vx > 0.0, "Hunter's Cry should push targets forward.");
        assertTrue(target.vy < 0.0, "Hunter's Cry should pop targets slightly upward.");
    }

    @Test
    void eagleSideSpecialUsesDirectionalInputForTalonRush() throws Exception {
        BirdGame3 game = new BirdGame3();
        game.activePlayers = 2;

        Bird eagle = new Bird(100.0, BirdGame3.BirdType.EAGLE, 0, game);
        Bird target = new Bird(200.0, BirdGame3.BirdType.PIGEON, 1, game);
        eagle.y = BirdGame3.GROUND_Y - 80.0;
        target.y = BirdGame3.GROUND_Y - 80.0;
        eagle.facingRight = true;
        game.players[0] = eagle;
        game.players[1] = target;

        double startingHealth = target.health;
        game.setLocalActionsForKey(game.rightKeyForPlayer(0), true);
        game.setLocalActionsForKey(game.specialKeyForPlayer(0), true);
        eagle.update(1.0);
        game.setLocalActionsForKey(game.specialKeyForPlayer(0), false);
        game.setLocalActionsForKey(game.rightKeyForPlayer(0), false);

        assertTrue(getPrivateInt(eagle, "raptorRushTimer") > 0);
        assertTrue(getPrivateInt(eagle, "raptorRushReuseTimer") > 0);
        assertEquals(0, getPrivateInt(eagle, "specialCooldown"),
                "Talon Rush should not trigger the visible cooldown bar.");
        assertTrue(eagle.vx > 13.0, "Talon Rush should commit Eagle to a strong horizontal burst.");
        assertEquals(startingHealth - 10.0, target.health, 0.0001,
                "Talon Rush should hit for Eagle's heavier rush damage.");
        assertTrue(target.vy < -8.0, "Talon Rush should launch the target upward.");
    }

    @Test
    void eagleNeutralReuseTimerOnlyBlocksRepeatingHuntersCry() throws Exception {
        BirdGame3 game = new BirdGame3();
        game.activePlayers = 2;

        Bird eagle = new Bird(100.0, BirdGame3.BirdType.EAGLE, 0, game);
        Bird target = new Bird(220.0, BirdGame3.BirdType.PIGEON, 1, game);
        eagle.y = BirdGame3.GROUND_Y - 80.0;
        target.y = BirdGame3.GROUND_Y - 80.0;
        eagle.facingRight = true;
        game.players[0] = eagle;
        game.players[1] = target;

        game.setLocalActionsForKey(game.specialKeyForPlayer(0), true);
        eagle.update(1.0);
        game.setLocalActionsForKey(game.specialKeyForPlayer(0), false);

        while (getPrivateInt(eagle, "raptorCryTimer") > 0) {
            eagle.update(1.0);
        }

        assertTrue(getPrivateInt(eagle, "raptorCryReuseTimer") > 0);

        game.setLocalActionsForKey(game.specialKeyForPlayer(0), true);
        eagle.update(1.0);
        game.setLocalActionsForKey(game.specialKeyForPlayer(0), false);

        assertEquals(0, getPrivateInt(eagle, "raptorCryTimer"),
                "Hunter's Cry should stay locked until its hidden reuse timer expires.");

        eagle.update(1.0);

        game.setLocalActionsForKey(game.rightKeyForPlayer(0), true);
        game.setLocalActionsForKey(game.specialKeyForPlayer(0), true);
        eagle.update(1.0);
        game.setLocalActionsForKey(game.specialKeyForPlayer(0), false);
        game.setLocalActionsForKey(game.rightKeyForPlayer(0), false);

        assertTrue(getPrivateInt(eagle, "raptorRushTimer") > 0,
                "The hidden Hunter's Cry timer should not block Talon Rush.");
    }

    @Test
    void eagleUpSpecialOverridesGroundJumpAndStartsSkyrise() throws Exception {
        BirdGame3 game = new BirdGame3();
        game.activePlayers = 1;

        Bird eagle = new Bird(190.0, BirdGame3.BirdType.EAGLE, 0, game);
        eagle.y = BirdGame3.GROUND_Y - 80.0;
        game.players[0] = eagle;

        game.setLocalActionsForKey(game.jumpKeyForPlayer(0), true);
        game.setLocalActionsForKey(game.specialKeyForPlayer(0), true);
        eagle.update(1.0);
        game.setLocalActionsForKey(game.specialKeyForPlayer(0), false);
        game.setLocalActionsForKey(game.jumpKeyForPlayer(0), false);

        assertEquals(0, getPrivateInt(eagle, "jumpSquatTimer"),
                "Skyrise should bypass jump squat instead of becoming a normal jump.");
        assertTrue(getPrivateInt(eagle, "raptorClimbTimer") > 0);
        assertTrue(getPrivateBoolean(eagle, "raptorUpSpecialUsed"));
        assertEquals(0, getPrivateInt(eagle, "specialCooldown"),
                "Skyrise should not trigger the visible cooldown bar.");
        assertTrue(eagle.vy < -10.0, "Skyrise should launch Eagle sharply upward.");
    }

    @Test
    void eagleUpSpecialCannotBeUsedAgainUntilLanding() throws Exception {
        BirdGame3 game = new BirdGame3();
        game.activePlayers = 1;

        Bird eagle = new Bird(190.0, BirdGame3.BirdType.EAGLE, 0, game);
        eagle.y = BirdGame3.GROUND_Y - 80.0;
        game.players[0] = eagle;

        game.setLocalActionsForKey(game.jumpKeyForPlayer(0), true);
        game.setLocalActionsForKey(game.specialKeyForPlayer(0), true);
        eagle.update(1.0);
        game.setLocalActionsForKey(game.specialKeyForPlayer(0), false);
        game.setLocalActionsForKey(game.jumpKeyForPlayer(0), false);

        eagle.y = BirdGame3.GROUND_Y - 260.0;

        while (getPrivateInt(eagle, "raptorClimbTimer") > 0) {
            eagle.update(1.0);
        }

        assertTrue(getPrivateBoolean(eagle, "raptorUpSpecialUsed"));
        assertFalse(eagle.isOnGround(), "Skyrise should still be spent while Eagle is airborne.");

        game.setLocalActionsForKey(game.jumpKeyForPlayer(0), true);
        game.setLocalActionsForKey(game.specialKeyForPlayer(0), true);
        eagle.update(1.0);
        game.setLocalActionsForKey(game.specialKeyForPlayer(0), false);
        game.setLocalActionsForKey(game.jumpKeyForPlayer(0), false);

        assertEquals(0, getPrivateInt(eagle, "raptorClimbTimer"),
                "Skyrise should not restart again before Eagle lands.");

        eagle.y = BirdGame3.GROUND_Y - 80.0;
        eagle.vx = 0.0;
        eagle.vy = 0.0;
        eagle.update(1.0);

        assertFalse(getPrivateBoolean(eagle, "raptorUpSpecialUsed"),
                "Touching the ground should refresh Skyrise.");

        game.setLocalActionsForKey(game.jumpKeyForPlayer(0), true);
        game.setLocalActionsForKey(game.specialKeyForPlayer(0), true);
        eagle.update(1.0);
        game.setLocalActionsForKey(game.specialKeyForPlayer(0), false);
        game.setLocalActionsForKey(game.jumpKeyForPlayer(0), false);

        assertTrue(getPrivateInt(eagle, "raptorClimbTimer") > 0,
                "Skyrise should become available again after Eagle lands.");
    }

    @Test
    void eagleDownSpecialUsesBlockInputWithoutRaisingShield() {
        BirdGame3 game = new BirdGame3();
        game.activePlayers = 1;

        Bird eagle = new Bird(190.0, BirdGame3.BirdType.EAGLE, 0, game);
        eagle.y = BirdGame3.GROUND_Y - 80.0;
        game.players[0] = eagle;

        game.setLocalActionsForKey(game.blockKeyForPlayer(0), true);
        game.setLocalActionsForKey(game.specialKeyForPlayer(0), true);
        eagle.update(1.0);
        game.setLocalActionsForKey(game.blockKeyForPlayer(0), false);
        game.setLocalActionsForKey(game.specialKeyForPlayer(0), false);

        assertFalse(eagle.isBlocking, "Heavenfall should reserve block input instead of raising shield.");
        assertTrue(eagle.eagleDiveActive, "Block + special should start Eagle's dive special.");
        assertTrue(eagle.diveTimer > 0);
    }

    @Test
    void eagleGroundDownSpecialLeapsBeforeDiveBegins() {
        BirdGame3 game = new BirdGame3();
        game.activePlayers = 2;

        Bird eagle = new Bird(190.0, BirdGame3.BirdType.EAGLE, 0, game);
        Bird target = new Bird(220.0, BirdGame3.BirdType.PIGEON, 1, game);
        eagle.y = BirdGame3.GROUND_Y - 80.0;
        target.y = BirdGame3.GROUND_Y - 80.0;
        game.players[0] = eagle;
        game.players[1] = target;

        double startingHealth = target.health;

        game.setLocalActionsForKey(game.blockKeyForPlayer(0), true);
        game.setLocalActionsForKey(game.specialKeyForPlayer(0), true);
        eagle.update(1.0);
        game.setLocalActionsForKey(game.blockKeyForPlayer(0), false);
        game.setLocalActionsForKey(game.specialKeyForPlayer(0), false);

        assertTrue(eagle.eagleDiveCountdown > 0, "Grounded Heavenfall should spend a few startup frames leaping first.");
        assertFalse(eagle.isOnGround(), "Grounded Heavenfall should launch Eagle off the floor before the slam starts.");
        assertEquals(startingHealth, target.health, 0.0001,
                "Grounded Heavenfall should not hit on the startup hop.");

        while (eagle.eagleDiveCountdown > 1) {
            eagle.update(1.0);
            assertFalse(eagle.isOnGround(), "Eagle should stay airborne through the leap startup.");
        }

        eagle.update(1.0);

        assertEquals(0, eagle.eagleDiveCountdown);
        assertTrue(eagle.vy >= 18.0, "After the leap, Heavenfall should transition into its fast downward slam.");
    }

    @Test
    void falconGroundDownSpecialLeapsBeforeDiagonalDiveBegins() {
        BirdGame3 game = new BirdGame3();
        game.activePlayers = 2;

        Bird falcon = new Bird(190.0, BirdGame3.BirdType.FALCON, 0, game);
        Bird target = new Bird(220.0, BirdGame3.BirdType.PIGEON, 1, game);
        falcon.y = BirdGame3.GROUND_Y - 80.0;
        target.y = BirdGame3.GROUND_Y - 80.0;
        falcon.facingRight = true;
        game.players[0] = falcon;
        game.players[1] = target;

        double startingHealth = target.health;

        game.setLocalActionsForKey(game.blockKeyForPlayer(0), true);
        game.setLocalActionsForKey(game.specialKeyForPlayer(0), true);
        falcon.update(1.0);
        game.setLocalActionsForKey(game.blockKeyForPlayer(0), false);
        game.setLocalActionsForKey(game.specialKeyForPlayer(0), false);

        assertTrue(falcon.eagleDiveCountdown > 0, "Grounded Falcon Dive should hop before it commits to the strike.");
        assertFalse(falcon.isOnGround(), "Grounded Falcon Dive should leave the ground during startup.");
        assertEquals(startingHealth, target.health, 0.0001,
                "Grounded Falcon Dive should not hit during the startup hop.");

        while (falcon.eagleDiveCountdown > 1) {
            falcon.update(1.0);
            assertFalse(falcon.isOnGround(), "Falcon should stay airborne through the startup hop.");
        }

        falcon.update(1.0);

        assertEquals(0, falcon.eagleDiveCountdown);
        assertTrue(falcon.vx > 0.0, "Falcon Dive should break forward once the hop finishes.");
        assertTrue(falcon.vy > 0.0, "Falcon Dive should angle down once the hop finishes.");
        assertEquals(falcon.vx, falcon.vy, 0.0001,
                "Falcon Dive should launch along a true diagonal after the leap.");
    }

    @Test
    void falconDownSpecialPoseFacesDiagonalDirection() throws Exception {
        BirdGame3 game = new BirdGame3();
        game.activePlayers = 1;

        Bird falcon = new Bird(190.0, BirdGame3.BirdType.FALCON, 0, game);
        falcon.y = BirdGame3.GROUND_Y - 220.0;
        falcon.facingRight = true;
        game.players[0] = falcon;

        game.setLocalActionsForKey(game.blockKeyForPlayer(0), true);
        game.setLocalActionsForKey(game.specialKeyForPlayer(0), true);
        falcon.update(1.0);
        game.setLocalActionsForKey(game.blockKeyForPlayer(0), false);
        game.setLocalActionsForKey(game.specialKeyForPlayer(0), false);

        Object pose = invokePrivateObjectMethod(falcon, "currentRaptorSpecialPose");
        assertNotNull(pose);
        assertEquals(Math.PI / 4.0, invokeDoubleMethod(pose, "aimAngleRadians"), 0.0001,
                "Falcon's dive pose should face diagonally to match the actual attack path.");
    }

    @Test
    void eagleDownSpecialCooldownDoesNotBlockHuntersCry() throws Exception {
        BirdGame3 game = new BirdGame3();
        game.activePlayers = 2;

        Bird eagle = new Bird(100.0, BirdGame3.BirdType.EAGLE, 0, game);
        Bird target = new Bird(195.0, BirdGame3.BirdType.PIGEON, 1, game);
        eagle.y = BirdGame3.GROUND_Y - 80.0;
        target.y = BirdGame3.GROUND_Y - 80.0;
        eagle.facingRight = true;
        game.players[0] = eagle;
        game.players[1] = target;

        double startingHealth = target.health;
        game.setLocalActionsForKey(game.blockKeyForPlayer(0), true);
        game.setLocalActionsForKey(game.specialKeyForPlayer(0), true);
        eagle.update(1.0);
        game.setLocalActionsForKey(game.blockKeyForPlayer(0), false);
        game.setLocalActionsForKey(game.specialKeyForPlayer(0), false);

        assertTrue(getPrivateInt(eagle, "specialCooldown") > 0,
                "Heavenfall should still own the visible cooldown bar.");

        eagle.eagleDiveActive = false;
        eagle.eagleAscentActive = false;
        eagle.eagleDiveCountdown = 0;
        eagle.diveTimer = 0;
        eagle.vx = 0.0;
        eagle.vy = 0.0;
        eagle.y = BirdGame3.GROUND_Y - 80.0;
        eagle.update(1.0);

        game.setLocalActionsForKey(game.specialKeyForPlayer(0), true);
        eagle.update(1.0);
        game.setLocalActionsForKey(game.specialKeyForPlayer(0), false);

        assertTrue(getPrivateInt(eagle, "raptorCryTimer") > 0,
                "Hunter's Cry should still be usable while Heavenfall cools down.");
        assertEquals(startingHealth - 8.0, target.health, 0.0001,
                "Hunter's Cry should still hit normally during Heavenfall's cooldown.");
        assertTrue(getPrivateInt(eagle, "specialCooldown") > 0,
                "Using Hunter's Cry should not erase Heavenfall's visible cooldown.");
    }

    @Test
    void falconNeutralSpecialSweetspotDealsMoreDamageAtRange() {
        BirdGame3 closeGame = new BirdGame3();
        closeGame.activePlayers = 2;
        Bird closeFalcon = new Bird(100.0, BirdGame3.BirdType.FALCON, 0, closeGame);
        Bird closeTarget = new Bird(195.0, BirdGame3.BirdType.PIGEON, 1, closeGame);
        closeFalcon.y = BirdGame3.GROUND_Y - 80.0;
        closeTarget.y = BirdGame3.GROUND_Y - 80.0;
        closeFalcon.facingRight = true;
        closeGame.players[0] = closeFalcon;
        closeGame.players[1] = closeTarget;

        closeGame.setLocalActionsForKey(closeGame.specialKeyForPlayer(0), true);
        closeFalcon.update(1.0);
        closeGame.setLocalActionsForKey(closeGame.specialKeyForPlayer(0), false);

        BirdGame3 farGame = new BirdGame3();
        farGame.activePlayers = 2;
        Bird farFalcon = new Bird(100.0, BirdGame3.BirdType.FALCON, 0, farGame);
        Bird farTarget = new Bird(250.0, BirdGame3.BirdType.PIGEON, 1, farGame);
        farFalcon.y = BirdGame3.GROUND_Y - 80.0;
        farTarget.y = BirdGame3.GROUND_Y - 80.0;
        farFalcon.facingRight = true;
        farGame.players[0] = farFalcon;
        farGame.players[1] = farTarget;

        farGame.setLocalActionsForKey(farGame.specialKeyForPlayer(0), true);
        farFalcon.update(1.0);
        farGame.setLocalActionsForKey(farGame.specialKeyForPlayer(0), false);

        double closeDamage = Bird.STARTING_HEALTH - closeTarget.health;
        double farDamage = Bird.STARTING_HEALTH - farTarget.health;
        assertTrue(farDamage > closeDamage,
                "Target Snap should reward the farther tipper lane with stronger damage.");
        assertTrue(farTarget.vy < closeTarget.vy,
                "The sweetspot should also launch harder than the close hit.");
    }

    @Test
    void aerialAttackAutoCancelsOnEarlyLanding() throws Exception {
        BirdGame3 game = new BirdGame3();
        game.activePlayers = 1;

        Bird bird = new Bird(190.0, BirdGame3.BirdType.EAGLE, 0, game);
        bird.y = BirdGame3.GROUND_Y - 240.0;
        game.players[0] = bird;

        game.setLocalActionsForKey(game.attackKeyForPlayer(0), true);
        bird.update(1.0);
        game.setLocalActionsForKey(game.attackKeyForPlayer(0), false);

        bird.y = BirdGame3.GROUND_Y - 100.0;
        bird.vy = 25.0;
        bird.update(1.0);

        assertTrue(bird.isOnGround());
        assertEquals(0, getPrivateInt(bird, "landingLagTimer"));
        assertEquals(0, bird.attackAnimationTimer);

        game.setLocalActionsForKey(game.blockKeyForPlayer(0), true);
        bird.update(1.0);

        assertTrue(bird.isBlocking);
    }

    @Test
    void aerialAttackAutoCancelsOnLateLanding() throws Exception {
        BirdGame3 game = new BirdGame3();
        game.activePlayers = 1;

        Bird bird = new Bird(190.0, BirdGame3.BirdType.EAGLE, 0, game);
        bird.y = BirdGame3.GROUND_Y - 320.0;
        game.players[0] = bird;

        game.setLocalActionsForKey(game.attackKeyForPlayer(0), true);
        bird.update(1.0);
        game.setLocalActionsForKey(game.attackKeyForPlayer(0), false);

        for (int i = 0; i < 9; i++) {
            bird.y = BirdGame3.GROUND_Y - 320.0;
            bird.vy = 0.0;
            bird.update(1.0);
        }

        bird.y = BirdGame3.GROUND_Y - 100.0;
        bird.vy = 25.0;
        bird.update(1.0);

        assertTrue(bird.isOnGround());
        assertEquals(0, getPrivateInt(bird, "landingLagTimer"));
        assertEquals(0, bird.attackAnimationTimer);
    }

    @Test
    void aerialAttackLandingLagBlocksShieldUntilRecoveryEnds() throws Exception {
        BirdGame3 game = new BirdGame3();
        game.activePlayers = 1;

        Bird bird = new Bird(190.0, BirdGame3.BirdType.EAGLE, 0, game);
        bird.y = BirdGame3.GROUND_Y - 320.0;
        game.players[0] = bird;

        game.setLocalActionsForKey(game.attackKeyForPlayer(0), true);
        bird.update(1.0);
        game.setLocalActionsForKey(game.attackKeyForPlayer(0), false);

        for (int i = 0; i < 4; i++) {
            bird.y = BirdGame3.GROUND_Y - 320.0;
            bird.vy = 0.0;
            bird.update(1.0);
        }

        bird.y = BirdGame3.GROUND_Y - 100.0;
        bird.vy = 25.0;
        bird.update(1.0);

        assertTrue(bird.isOnGround());
        assertTrue(getPrivateInt(bird, "landingLagTimer") > 0);
        assertEquals(0, bird.attackAnimationTimer);

        game.setLocalActionsForKey(game.blockKeyForPlayer(0), true);
        bird.update(1.0);

        assertFalse(bird.isBlocking);

        for (int i = 0; i < 8; i++) {
            bird.update(1.0);
        }

        assertTrue(bird.isBlocking);
    }

    @Test
    void hitstunLandingWithShieldPressTriggersGroundTechRoll() throws Exception {
        BirdGame3 game = new BirdGame3();
        game.activePlayers = 1;
        setPrivateBoolean(game);

        Bird bird = new Bird(190.0, BirdGame3.BirdType.EAGLE, 0, game);
        bird.y = BirdGame3.GROUND_Y - 96.0;
        bird.vy = 18.0;
        bird.stunTime = 20.0;
        game.players[0] = bird;

        double startX = bird.x;
        game.setLocalActionsForKey(game.blockKeyForPlayer(0), true);
        game.setLocalActionsForKey(game.rightKeyForPlayer(0), true);

        bird.update(1.0);

        assertTrue(bird.isOnGround());
        assertEquals(0.0, bird.stunTime, 0.0001);
        assertEquals("ROLL", getPrivateObject(bird, "dodgeType").toString());
        assertEquals(0, getPrivateInt(bird, "knockdownTimer"));
        assertTrue(bird.vx > 0.0);

        bird.update(1.0);

        assertTrue(bird.x > startX + 4.0);
    }

    @Test
    void missedTechLandingEntersKnockdownAndBlocksShieldUntilRecoveryEnds() throws Exception {
        BirdGame3 game = new BirdGame3();
        game.activePlayers = 1;
        setPrivateBoolean(game);

        Bird bird = new Bird(190.0, BirdGame3.BirdType.EAGLE, 0, game);
        bird.y = BirdGame3.GROUND_Y - 96.0;
        bird.vy = 18.0;
        bird.stunTime = 20.0;
        game.players[0] = bird;

        bird.update(1.0);

        assertTrue(bird.isOnGround());
        assertEquals(0.0, bird.stunTime, 0.0001);
        assertTrue(getPrivateInt(bird, "knockdownTimer") > 0);
        assertEquals("NONE", getPrivateObject(bird, "dodgeType").toString());

        game.setLocalActionsForKey(game.blockKeyForPlayer(0), true);
        bird.update(1.0);

        assertFalse(bird.isBlocking);

        while (getPrivateInt(bird, "knockdownTimer") > 0) {
            bird.update(1.0);
        }
        bird.update(1.0);

        assertTrue(bird.isBlocking);
    }

    @Test
    void airborneShieldPressCanWallTechDuringHitstun() throws Exception {
        BirdGame3 game = new BirdGame3();
        game.activePlayers = 1;
        setPrivateBoolean(game);
        game.platforms.clear();
        Platform wall = new Platform(260.0, BirdGame3.GROUND_Y - 220.0, 32.0, 220.0);
        game.platforms.add(wall);

        Bird bird = new Bird(150.0, BirdGame3.BirdType.EAGLE, 0, game);
        bird.y = wall.y + 40.0;
        bird.vx = 36.0;
        bird.vy = 0.0;
        bird.stunTime = 18.0;
        game.players[0] = bird;

        game.setLocalActionsForKey(game.blockKeyForPlayer(0), true);
        bird.update(1.0);

        assertEquals(0.0, bird.stunTime, 0.0001);
        assertEquals(0, getPrivateInt(bird, "knockdownTimer"));
        assertTrue(getPrivateInt(bird, "dodgeInvulnerabilityTimer") > 0);
        assertEquals(wall.x - 80.0, bird.x, 0.0001);
        assertEquals(0.0, bird.vx, 0.0001);
    }

    @Test
    void battlefieldClampAdaptsToBirdRecoveryProfiles() throws Exception {
        BirdGame3 game = new BirdGame3();
        game.selectedMap = BirdGame3.MapType.BATTLEFIELD;
        double islandX = 2400.0;
        double islandY = BirdGame3.GROUND_Y - 80.0;
        game.platforms.add(new Platform(islandX, islandY, 1200, 70));

        Bird hummingbird = new Bird(2800, BirdGame3.BirdType.HUMMINGBIRD, 0, game);
        Bird pigeon = new Bird(2800, BirdGame3.BirdType.PIGEON, 1, game);
        Bird groundedVulture = new Bird(2800, BirdGame3.BirdType.VULTURE, 2, game);
        Bird risingVulture = new Bird(2800, BirdGame3.BirdType.VULTURE, 3, game);
        risingVulture.y = islandY - 280;
        risingVulture.vy = -5.0;

        Method clamp = Bird.class.getDeclaredMethod("clampGoalXAwayFromVoid", double.class);
        clamp.setAccessible(true);

        double offstageGoal = 1600.0;
        double hummingbirdGoal = (double) clamp.invoke(hummingbird, offstageGoal);
        double pigeonGoal = (double) clamp.invoke(pigeon, offstageGoal);
        double groundedVultureGoal = (double) clamp.invoke(groundedVulture, offstageGoal);
        double risingVultureGoal = (double) clamp.invoke(risingVulture, offstageGoal);

        assertTrue(hummingbirdGoal < pigeonGoal);
        assertTrue(pigeonGoal < groundedVultureGoal);
        assertTrue(risingVultureGoal < groundedVultureGoal);
    }

    @Test
    void penguinAiUsesIceJumpToRecoverOffstage() {
        BirdGame3 game = new BirdGame3();
        game.activePlayers = 2;
        game.selectedMap = BirdGame3.MapType.BATTLEFIELD;
        double islandX = 2400.0;
        double islandY = BirdGame3.GROUND_Y - 80.0;
        game.platforms.add(new Platform(islandX, islandY, 1200, 70));

        Bird penguin = new Bird(islandX - 160, BirdGame3.BirdType.PENGUIN, 0, game);
        penguin.y = islandY + 150;
        Bird target = new Bird(islandX + 280, BirdGame3.BirdType.EAGLE, 1, game);
        target.y = islandY - 80;

        game.players[0] = penguin;
        game.players[1] = target;
        game.isAI[0] = true;

        penguin.update(1.0);

        assertTrue(game.isRightPressed(0));
        assertTrue(game.isSpecialPressed(0));
    }

    @Test
    void penguinNeutralChargesThenReleasesBellySlideWithoutCooldownUi() throws Exception {
        BirdGame3 game = new BirdGame3();
        game.activePlayers = 2;

        Bird penguin = new Bird(120.0, BirdGame3.BirdType.PENGUIN, 0, game);
        Bird target = new Bird(210.0, BirdGame3.BirdType.EAGLE, 1, game);
        penguin.y = BirdGame3.GROUND_Y - 80.0;
        target.y = BirdGame3.GROUND_Y - 80.0;
        game.players[0] = penguin;
        game.players[1] = target;

        KeyCode specialKey = game.specialKeyForPlayer(0);
        game.setLocalActionsForKey(specialKey, true);
        for (int i = 0; i < 24; i++) {
            penguin.update(1.0);
        }

        assertTrue(getPrivateBoolean(penguin, "penguinBellyCharging"));
        assertEquals(0, penguin.specialCooldown);

        game.setLocalActionsForKey(specialKey, false);
        penguin.update(1.0);

        assertTrue(getPrivateInt(penguin, "penguinBellySlideTimer") > 0);
        assertTrue(target.health < Bird.STARTING_HEALTH || target.vx > 0.0);
        assertEquals(0, penguin.specialCooldown);
    }

    @Test
    void penguinUpSpecialUsesAirRecoveryRuleAndNoVisibleCooldown() throws Exception {
        BirdGame3 game = new BirdGame3();
        game.activePlayers = 1;

        Bird penguin = new Bird(220.0, BirdGame3.BirdType.PENGUIN, 0, game);
        penguin.y = BirdGame3.GROUND_Y - 260.0;
        game.players[0] = penguin;

        KeyCode jumpKey = game.jumpKeyForPlayer(0);
        KeyCode specialKey = game.specialKeyForPlayer(0);
        game.setLocalActionsForKey(jumpKey, true);
        game.setLocalActionsForKey(specialKey, true);
        penguin.update(1.0);

        assertTrue(getPrivateBoolean(penguin, "penguinUpSpecialUsed"));
        assertTrue(getPrivateInt(penguin, "penguinRocketTimer") > 0);
        assertTrue(penguin.vy < -12.0);
        assertEquals(0, penguin.specialCooldown);

        for (int i = 0; i < 28; i++) {
            penguin.update(1.0);
        }
        assertTrue(getPrivateInt(penguin, "penguinFlopTimer") > 0,
                "Holding up special should enter the slow falling blast only after the flap window.");
        assertTrue(penguin.vy > 0.0 && penguin.vy < 12.0);

        game.setLocalActionsForKey(specialKey, false);
        penguin.update(1.0);
        game.setLocalActionsForKey(specialKey, true);
        penguin.update(1.0);

        assertTrue(getPrivateBoolean(penguin, "penguinUpSpecialUsed"));
    }

    @Test
    void penguinUpSpecialCanBeSteeredWhileRising() {
        BirdGame3 game = new BirdGame3();
        game.activePlayers = 1;

        Bird penguin = new Bird(220.0, BirdGame3.BirdType.PENGUIN, 0, game);
        penguin.y = BirdGame3.GROUND_Y - 260.0;
        game.players[0] = penguin;

        game.setLocalActionsForKey(game.rightKeyForPlayer(0), true);
        game.setLocalActionsForKey(game.jumpKeyForPlayer(0), true);
        game.setLocalActionsForKey(game.specialKeyForPlayer(0), true);

        for (int i = 0; i < 8; i++) {
            penguin.update(1.0);
        }

        assertTrue(penguin.vx > 2.0, "Penguin should keep fluid horizontal control during the rocket rise.");
        assertTrue(penguin.vy < -4.0, "Penguin should still be rising while steering.");
        assertEquals(0, penguin.specialCooldown);
    }

    @Test
    void penguinUltimateStartsAbsoluteZeroFortress() throws Exception {
        BirdGame3 game = new BirdGame3();
        game.activePlayers = 2;

        Bird penguin = new Bird(300.0, BirdGame3.BirdType.PENGUIN, 0, game);
        Bird target = new Bird(620.0, BirdGame3.BirdType.PIGEON, 1, game);
        penguin.y = BirdGame3.GROUND_Y - 80.0;
        target.y = BirdGame3.GROUND_Y - 80.0;
        game.players[0] = penguin;
        game.players[1] = target;

        setPrivateDouble(penguin, "ultimateMeter", 100.0);
        invokePrivateVoid(penguin, "special");

        assertEquals(0.0, getPrivateDouble(penguin, "ultimateMeter"), 0.0001);
        assertTrue(getPrivateInt(penguin, "penguinAbsoluteZeroTimer") > 0);
        assertTrue(penguin.isCombatInvulnerable());
        assertEquals(0, penguin.specialCooldown);
        assertEquals(PenguinSpecials.ABSOLUTE_ZERO_FORTRESS_MOVE, game.lastTelemetryMoveName(0, ""));
    }

    @Test
    void penguinAbsoluteZeroFortressDropsSkyIcebergs() throws Exception {
        BirdGame3 game = new BirdGame3();
        game.activePlayers = 2;

        Bird penguin = new Bird(300.0, BirdGame3.BirdType.PENGUIN, 0, game);
        Bird target = new Bird(620.0, BirdGame3.BirdType.PIGEON, 1, game);
        penguin.y = BirdGame3.GROUND_Y - 80.0;
        target.y = BirdGame3.GROUND_Y - 80.0;
        game.players[0] = penguin;
        game.players[1] = target;

        setPrivateDouble(penguin, "ultimateMeter", 100.0);
        invokePrivateVoid(penguin, "special");
        double impactX = PenguinSpecials.absoluteZeroImpactX(penguin, 0);
        target.x = impactX - target.bodyWidth() * 0.5;
        target.y = BirdGame3.GROUND_Y - target.bodyHeight();
        double healthBefore = target.health;

        int firstImpactFrame = PenguinSpecials.absoluteZeroImpactFrame(0);
        for (int i = 0; i <= firstImpactFrame + 1; i++) {
            penguin.update(1.0);
        }

        assertTrue(target.health < healthBefore, "The first fortress iceberg should damage targets near impact.");
        assertTrue(getPrivateInt(penguin, "penguinAbsoluteZeroWaveIndex") >= 1);
        assertTrue(game.damageDealt[0] > 0);
    }

    @Test
    void grinchhawkUltimateStartsMidnightGiftstormInsteadOfBoostedSpecial() throws Exception {
        BirdGame3 game = new BirdGame3();
        game.activePlayers = 2;

        Bird grinch = new Bird(300.0, BirdGame3.BirdType.GRINCHHAWK, 0, game);
        Bird target = new Bird(430.0, BirdGame3.BirdType.PIGEON, 1, game);
        grinch.y = BirdGame3.GROUND_Y - 80.0;
        target.y = BirdGame3.GROUND_Y - 80.0;
        game.players[0] = grinch;
        game.players[1] = target;

        setPrivateDouble(grinch, "ultimateMeter", 100.0);
        invokePrivateVoid(grinch, "special");

        assertEquals(Bird.GRINCH_MIDNIGHT_GIFTSTORM_FRAMES, grinch.grinchGiftstormTimer);
        assertEquals(0, grinch.grinchHeartSnatchTimer,
                "Grinch-Hawk ultimate should not fall through into boosted Heart Snatch.");
        assertFalse(grinch.grinchSleighRiding);
        assertNull(grinch.grinchPresent);
        assertFalse(grinch.isUltimateReady());
        assertEquals(0, grinch.specialCooldown);
        assertEquals(GrinchhawkSpecials.MIDNIGHT_GIFTSTORM_MOVE, game.lastTelemetryMoveName(0, ""));
    }

    @Test
    void grinchhawkMidnightGiftstormDropsPresentsAndFinalSleigh() throws Exception {
        BirdGame3 game = new BirdGame3();
        game.activePlayers = 2;

        Bird grinch = new Bird(300.0, BirdGame3.BirdType.GRINCHHAWK, 0, game);
        Bird target = new Bird(380.0, BirdGame3.BirdType.PIGEON, 1, game);
        grinch.y = BirdGame3.GROUND_Y - 80.0;
        target.y = BirdGame3.GROUND_Y - 80.0;
        game.players[0] = grinch;
        game.players[1] = target;

        setPrivateDouble(grinch, "ultimateMeter", 100.0);
        double healthBefore = target.health;
        invokePrivateVoid(grinch, "special");

        for (int i = 0; i < Bird.GRINCH_MIDNIGHT_GIFTSTORM_DROP_START_FRAME + 3; i++) {
            grinch.update(1.0);
        }

        assertTrue(grinch.grinchGiftstormDropIndex >= 1,
                "Midnight Giftstorm should start raining presents on schedule.");
        assertTrue(target.health < healthBefore,
                "The first Giftstorm present should threaten the nearest target.");
        double healthAfterDrop = target.health;

        for (int i = 0; i < Bird.GRINCH_MIDNIGHT_GIFTSTORM_FINAL_FRAME + 8; i++) {
            grinch.update(1.0);
        }

        assertTrue(grinch.grinchGiftstormFinalResolved,
                "Midnight Giftstorm should resolve one final sleigh dive.");
        assertTrue(target.health < healthAfterDrop,
                "The final sleigh dive should damage caught targets.");
        assertTrue(Math.abs(target.vx) > 10.0 || target.vy < -8.0,
                "The final sleigh dive should launch caught targets.");
    }

    @Test
    void shoebillUltimateStartsFinalStillness() throws Exception {
        BirdGame3 game = new BirdGame3();
        game.activePlayers = 2;

        Bird shoebill = new Bird(300.0, BirdGame3.BirdType.SHOEBILL, 0, game);
        Bird target = new Bird(620.0, BirdGame3.BirdType.PIGEON, 1, game);
        shoebill.y = BirdGame3.GROUND_Y - 80.0;
        target.y = BirdGame3.GROUND_Y - 80.0;
        game.players[0] = shoebill;
        game.players[1] = target;

        setPrivateDouble(shoebill, "ultimateMeter", 100.0);
        invokePrivateVoid(shoebill, "special");

        assertEquals(0.0, getPrivateDouble(shoebill, "ultimateMeter"), 0.0001);
        assertTrue(getPrivateInt(shoebill, "shoebillFinalStillnessTimer") > 0);
        assertEquals(1, getPrivateInt(shoebill, "shoebillFinalStillnessTargetIndex"));
        assertTrue(shoebill.isCombatInvulnerable());
        assertEquals(0, shoebill.specialCooldown);
        assertEquals(ShoebillSpecials.FINAL_STILLNESS_MOVE, game.lastTelemetryMoveName(0, ""));
    }

    @Test
    void shoebillFinalStillnessBeamDamagesLockedTarget() throws Exception {
        BirdGame3 game = new BirdGame3();
        game.activePlayers = 2;

        Bird shoebill = new Bird(300.0, BirdGame3.BirdType.SHOEBILL, 0, game);
        Bird target = new Bird(620.0, BirdGame3.BirdType.PIGEON, 1, game);
        shoebill.y = BirdGame3.GROUND_Y - 80.0;
        target.y = BirdGame3.GROUND_Y - 80.0;
        game.players[0] = shoebill;
        game.players[1] = target;

        setPrivateDouble(shoebill, "ultimateMeter", 100.0);
        invokePrivateVoid(shoebill, "special");
        double healthBefore = target.health;

        int damageFrame = Bird.SHOEBILL_FINAL_STILLNESS_BEAM_START_FRAME + 8;
        for (int i = 0; i <= damageFrame; i++) {
            shoebill.update(1.0);
        }

        assertTrue(target.health < healthBefore, "Final Stillness should damage the locked target.");
        assertTrue(getPrivateBoolean(shoebill, "shoebillFinalStillnessBeamResolved"));
        assertTrue(game.damageDealt[0] > 0);
    }

    @Test
    void razorbillUltimateStartsGuillotineWakeInsteadOfBoostedSpecial() throws Exception {
        BirdGame3 game = new BirdGame3();
        game.activePlayers = 2;

        Bird razorbill = new Bird(300.0, BirdGame3.BirdType.RAZORBILL, 0, game);
        Bird target = new Bird(620.0, BirdGame3.BirdType.PIGEON, 1, game);
        razorbill.y = BirdGame3.GROUND_Y - 80.0;
        target.y = BirdGame3.GROUND_Y - 80.0;
        game.players[0] = razorbill;
        game.players[1] = target;

        setPrivateDouble(razorbill, "ultimateMeter", 100.0);
        invokePrivateVoid(razorbill, "special");

        assertEquals(0.0, getPrivateDouble(razorbill, "ultimateMeter"), 0.0001);
        assertEquals(Bird.RAZORBILL_GUILLOTINE_TOTAL_FRAMES, razorbill.razorbillGuillotineTimer);
        assertEquals(0, razorbill.razorbillGuillotineSlashIndex);
        assertTrue(razorbill.isCombatInvulnerable());
        assertEquals(0, razorbill.razorbillStormTimer);
        assertEquals(0, razorbill.bladeStormFrames);
        assertEquals(0, razorbill.razorbillShearTimer);
        assertEquals(0, razorbill.razorbillCounterTimer);
        assertEquals(RazorbillSpecials.GUILLOTINE_WAKE_MOVE, game.lastTelemetryMoveName(0, ""));
    }

    @Test
    void razorbillGuillotineWakeDamagesAndLeavesLingeringRazorWake() throws Exception {
        BirdGame3 game = new BirdGame3();
        game.activePlayers = 2;

        Bird razorbill = new Bird(300.0, BirdGame3.BirdType.RAZORBILL, 0, game);
        Bird target = new Bird(620.0, BirdGame3.BirdType.PIGEON, 1, game);
        razorbill.y = BirdGame3.GROUND_Y - 80.0;
        target.y = BirdGame3.GROUND_Y - 80.0;
        game.players[0] = razorbill;
        game.players[1] = target;

        setPrivateDouble(razorbill, "ultimateMeter", 100.0);
        invokePrivateVoid(razorbill, "special");
        double healthBefore = target.health;

        for (int i = 0; i <= Bird.RAZORBILL_GUILLOTINE_FINAL_FRAME + 2; i++) {
            razorbill.update(1.0);
        }

        assertTrue(target.health < healthBefore, "Guillotine Wake should damage targets that stay inside the marked cuts.");
        assertTrue(razorbill.razorbillGuillotineWakeTimer > 0, "The final slash should leave a lingering razor wake.");
        assertTrue(game.damageDealt[0] > 0);

        for (int i = 0; i < Bird.RAZORBILL_GUILLOTINE_TOTAL_FRAMES; i++) {
            razorbill.update(1.0);
        }

        assertEquals(0, razorbill.razorbillGuillotineTimer);
        assertTrue(razorbill.razorbillGuillotineWakeTimer > 0,
                "The floor wake should outlast Razorbill's untargetable slash chain.");
        assertFalse(razorbill.isCombatInvulnerable());
    }

    @Test
    void penguinSnowFortGuardsAndTurnsIcebergIntoSnowball() throws Exception {
        BirdGame3 game = new BirdGame3();
        game.activePlayers = 2;

        Bird penguin = new Bird(120.0, BirdGame3.BirdType.PENGUIN, 0, game);
        Bird attacker = new Bird(285.0, BirdGame3.BirdType.EAGLE, 1, game);
        penguin.y = BirdGame3.GROUND_Y - 80.0;
        attacker.y = BirdGame3.GROUND_Y - 80.0;
        penguin.facingRight = true;
        game.players[0] = penguin;
        game.players[1] = attacker;

        KeyCode blockKey = game.blockKeyForPlayer(0);
        KeyCode specialKey = game.specialKeyForPlayer(0);
        game.setLocalActionsForKey(blockKey, true);
        game.setLocalActionsForKey(specialKey, true);
        penguin.update(1.0);

        assertNotNull(getPrivateObject(penguin, "penguinSnowFort"));
        double dealt = applyPrivateDamage(attacker, penguin, 20.0);
        assertTrue(dealt < 20.0);
        assertTrue(penguin.health > Bird.STARTING_HEALTH - 20.0);

        game.setLocalActionsForKey(blockKey, false);
        game.setLocalActionsForKey(specialKey, false);
        penguin.update(1.0);

        KeyCode rightKey = game.rightKeyForPlayer(0);
        game.setLocalActionsForKey(rightKey, true);
        game.setLocalActionsForKey(specialKey, true);
        penguin.update(1.0);

        Object iceObjects = getPrivateObject(penguin, "penguinIceObjects");
        assertTrue(iceObjects instanceof List<?> list && !list.isEmpty());
        Object firstObject = ((List<?>) iceObjects).getFirst();
        assertTrue(getPrivateBoolean(firstObject, "snowball"));
        assertEquals(0, penguin.specialCooldown);
    }

    @Test
    void penguinAirDownSpecialDropsIcebergStraightDown() throws Exception {
        BirdGame3 game = new BirdGame3();
        game.activePlayers = 1;

        Bird penguin = new Bird(320.0, BirdGame3.BirdType.PENGUIN, 0, game);
        penguin.y = BirdGame3.GROUND_Y - 260.0;
        game.players[0] = penguin;

        double startCenterX = penguin.bodyCenterX();
        game.setLocalActionsForKey(game.blockKeyForPlayer(0), true);
        game.setLocalActionsForKey(game.specialKeyForPlayer(0), true);
        penguin.update(1.0);
        game.setLocalActionsForKey(game.blockKeyForPlayer(0), false);
        game.setLocalActionsForKey(game.specialKeyForPlayer(0), false);

        Object iceObjects = getPrivateObject(penguin, "penguinIceObjects");
        assertTrue(iceObjects instanceof List<?> list && list.size() == 1);
        Object dropped = ((List<?>) iceObjects).getFirst();
        assertTrue(getPrivateBoolean(dropped, "verticalDrop"));
        assertFalse(getPrivateBoolean(dropped, "snowball"));
        assertEquals(0.0, getPrivateDouble(dropped, "vx"), 0.0001);
        assertTrue(getPrivateDouble(dropped, "vy") > 0.0);
        assertEquals(startCenterX, getPrivateDouble(dropped, "x"), 0.0001);
        assertNull(getPrivateObject(penguin, "penguinSnowFort"));
        assertTrue(getPrivateInt(penguin, "penguinSnowFortReuseTimer") > 0);
    }

    @Test
    void penguinAirDownSpecialIcebergDamagesTargetBelow() throws Exception {
        BirdGame3 game = new BirdGame3();
        game.activePlayers = 2;

        Bird penguin = new Bird(320.0, BirdGame3.BirdType.PENGUIN, 0, game);
        Bird target = new Bird(320.0, BirdGame3.BirdType.PIGEON, 1, game);
        penguin.y = BirdGame3.GROUND_Y - 280.0;
        target.x = penguin.bodyCenterX() - target.bodyWidth() * 0.5;
        target.y = BirdGame3.GROUND_Y - target.bodyHeight();
        game.players[0] = penguin;
        game.players[1] = target;

        game.setLocalActionsForKey(game.blockKeyForPlayer(0), true);
        game.setLocalActionsForKey(game.specialKeyForPlayer(0), true);
        penguin.update(1.0);
        game.setLocalActionsForKey(game.blockKeyForPlayer(0), false);
        game.setLocalActionsForKey(game.specialKeyForPlayer(0), false);
        double healthBefore = target.health;

        for (int i = 0; i < 50 && target.health >= healthBefore; i++) {
            penguin.update(1.0);
        }

        assertTrue(target.health < healthBefore, "Air down special's falling iceberg should threaten targets below Penguin.");
    }

    @Test
    void penguinIceObjectFallsWhenUnsupportedPastPlatformEdge() {
        BirdGame3 game = new BirdGame3();
        game.activePlayers = 1;
        game.selectedMap = BirdGame3.MapType.BATTLEFIELD;
        game.platforms.clear();
        Platform island = new Platform(500.0, BirdGame3.GROUND_Y - 280.0, 260.0, 60.0);
        game.platforms.add(island);

        Bird penguin = new Bird(island.x + 80.0, BirdGame3.BirdType.PENGUIN, 0, game);
        penguin.y = island.y - 80.0;
        game.players[0] = penguin;

        Bird.PenguinIceObject object = new Bird.PenguinIceObject(
                island.x + island.w + 80.0,
                island.y - 42.0,
                1.0,
                0.0,
                1,
                false,
                false);
        penguin.penguinIceObjects.add(object);

        for (int i = 0; i < 35; i++) {
            penguin.update(1.0);
        }

        assertFalse(object.shattered);
        assertTrue(object.y > island.y + 60.0,
                "Unsupported Penguin ice objects should fall instead of riding an invisible owner-height floor.");
    }

    @Test
    void penguinSnowFortBlocksMovementAndTakesAttackDamage() throws Exception {
        BirdGame3 game = new BirdGame3();
        game.activePlayers = 2;

        Bird penguin = new Bird(120.0, BirdGame3.BirdType.PENGUIN, 0, game);
        Bird attacker = new Bird(245.0, BirdGame3.BirdType.EAGLE, 1, game);
        penguin.y = BirdGame3.GROUND_Y - 80.0;
        attacker.y = BirdGame3.GROUND_Y - 80.0;
        penguin.facingRight = true;
        attacker.facingRight = false;
        attacker.vx = -8.0;
        game.players[0] = penguin;
        game.players[1] = attacker;

        game.setLocalActionsForKey(game.blockKeyForPlayer(0), true);
        game.setLocalActionsForKey(game.specialKeyForPlayer(0), true);
        penguin.update(1.0);

        Object fort = getPrivateObject(penguin, "penguinSnowFort");
        double fortX = getPrivateDouble(fort, "x");
        attacker.x = fortX - 40.0;
        penguin.update(1.0);

        assertTrue(Math.abs((attacker.x + 40.0) - fortX) > 62.0,
                "Snow Fort should push enemy bodies out instead of letting them walk through.");

        int healthBefore = getPrivateInt(fort, "health");
        invokePrivateIntVoid(attacker);
        int healthAfter = getPrivateInt(fort, "health");

        assertTrue(healthAfter < healthBefore, "Enemy attacks should damage the Snow Fort.");
    }

    @Test
    void penguinSnowFortDoesNotExpireByTimerAndClearsOnDeath() throws Exception {
        BirdGame3 game = new BirdGame3();
        game.activePlayers = 1;

        Bird penguin = new Bird(120.0, BirdGame3.BirdType.PENGUIN, 0, game);
        penguin.y = BirdGame3.GROUND_Y - 80.0;
        penguin.facingRight = true;
        game.players[0] = penguin;

        invokePrivateBooleanVoid(penguin, "specialPenguinSnowFort", false);
        Object fort = getPrivateObject(penguin, "penguinSnowFort");
        assertNotNull(fort);
        setPrivateInt(fort, "lifeFrames", 1);

        for (int i = 0; i < 90; i++) {
            penguin.update(1.0);
        }

        assertSame(fort, getPrivateObject(penguin, "penguinSnowFort"),
                "Snow Fort should stay up after its old lifetime would have expired.");

        penguin.health = 0;
        penguin.update(1.0);

        assertNull(getPrivateObject(penguin, "penguinSnowFort"),
                "Snow Fort should disappear when Penguin dies.");
    }

    @Test
    void penguinSnowFortBlocksAlliedBirdsInTeamMode() throws Exception {
        BirdGame3 game = new BirdGame3();
        game.activePlayers = 2;
        game.teamModeEnabled = true;

        Bird penguin = new Bird(120.0, BirdGame3.BirdType.PENGUIN, 0, game);
        Bird ally = new Bird(245.0, BirdGame3.BirdType.EAGLE, 1, game);
        penguin.y = BirdGame3.GROUND_Y - 80.0;
        ally.y = BirdGame3.GROUND_Y - 80.0;
        penguin.facingRight = true;
        game.players[0] = penguin;
        game.players[1] = ally;

        invokePrivateBooleanVoid(penguin, "specialPenguinSnowFort", false);
        Object fort = getPrivateObject(penguin, "penguinSnowFort");
        double fortX = getPrivateDouble(fort, "x");
        ally.x = fortX - 40.0;

        penguin.update(1.0);

        assertTrue(Math.abs((ally.x + 40.0) - fortX) > 90.0,
                "Snow Fort should physically block allied birds too, not only enemies.");
    }

    @Test
    void pigeonAiRefreshesRecoveryBeforeItFallsTooLow() {
        BirdGame3 game = new BirdGame3();
        game.activePlayers = 2;
        game.selectedMap = BirdGame3.MapType.BATTLEFIELD;
        double islandX = 2400.0;
        double islandY = BirdGame3.GROUND_Y - 80.0;
        game.platforms.add(new Platform(islandX, islandY, 1200, 70));

        Bird pigeon = new Bird(islandX - 90, BirdGame3.BirdType.PIGEON, 0, game);
        pigeon.y = islandY + 70;
        pigeon.vx = -2.8;
        pigeon.canDoubleJump = false;
        Bird target = new Bird(islandX + 360, BirdGame3.BirdType.EAGLE, 1, game);
        target.y = islandY - 120;

        game.players[0] = pigeon;
        game.players[1] = target;
        game.isAI[0] = true;

        pigeon.update(1.0);

        assertTrue(game.isRightPressed(0));
        assertTrue(game.isSpecialPressed(0));
    }

    @Test
    void recoveryFromBattlefieldSidePlatformsStillTargetsTheMainIsland() throws Exception {
        BirdGame3 game = new BirdGame3();
        game.activePlayers = 1;
        game.selectedMap = BirdGame3.MapType.BATTLEFIELD;
        double islandX = 2400.0;
        double islandY = BirdGame3.GROUND_Y - 80.0;
        Platform mainIsland = new Platform(islandX, islandY, 1200, 70);
        Platform leftPlatform = new Platform(islandX + 120, islandY - 210, 420, 46);
        game.platforms.add(mainIsland);
        game.platforms.add(leftPlatform);

        Bird penguin = new Bird(leftPlatform.x + 120, BirdGame3.BirdType.PENGUIN, 0, game);
        penguin.x = leftPlatform.x + leftPlatform.w / 2.0 - 40.0;
        penguin.y = leftPlatform.y - 80.0;
        game.players[0] = penguin;
        game.isAI[0] = true;

        Method caution = Bird.class.getDeclaredMethod("isAIVoidRecoveryCaution", boolean.class, Platform.class);
        caution.setAccessible(true);
        Method recoveryGoal = Bird.class.getDeclaredMethod("aiRecoveryGoalX", Platform.class);
        recoveryGoal.setAccessible(true);

        boolean keepRecovering = (boolean) caution.invoke(penguin, true, leftPlatform);
        double goalX = (double) recoveryGoal.invoke(penguin, leftPlatform);

        assertTrue(keepRecovering);
        assertTrue(goalX > leftPlatform.x + leftPlatform.w - 40.0);
    }

    @Test
    void heisenbirdAiDropsTowardReachableTargetFromBattlefieldPlatform() throws Exception {
        BirdGame3 game = new BirdGame3();
        game.activePlayers = 2;
        game.selectedMap = BirdGame3.MapType.BATTLEFIELD;
        double islandX = 2400.0;
        double islandY = BirdGame3.GROUND_Y - 80.0;
        Platform mainIsland = new Platform(islandX, islandY, 1200, 70);
        Platform sidePlatform = new Platform(islandX + 160, islandY - 210, 420, 46);
        game.platforms.add(mainIsland);
        game.platforms.add(sidePlatform);

        Bird heisenbird = new Bird(sidePlatform.x + sidePlatform.w / 2.0 - 40.0, BirdGame3.BirdType.HEISENBIRD, 0, game);
        heisenbird.y = sidePlatform.y - 80.0;
        Bird pigeon = new Bird(heisenbird.x, BirdGame3.BirdType.PIGEON, 1, game);
        pigeon.y = mainIsland.y - 80.0;
        setPrivateDouble(heisenbird, "opiumResourceMeter", 100.0);
        game.players[0] = heisenbird;
        game.players[1] = pigeon;
        game.isAI[0] = true;

        heisenbird.update(1.0);

        assertTrue(getPrivateInt(heisenbird, "aiDropCommitFrames") > 0,
                "A reachable target below should keep the drop plan instead of falling into void recovery.");
        assertFalse(game.isJumpPressed(0),
                "AI should not jump when it is already above the target and needs to drop or attack.");
    }

    @Test
    void aiTargetLockKeepsCurrentTargetWhenScoresAreClose() throws Exception {
        BirdGame3 game = new BirdGame3();
        game.activePlayers = 3;

        Bird ai = new Bird(100.0, BirdGame3.BirdType.PENGUIN, 2, game);
        Bird lockedTarget = new Bird(260.0, BirdGame3.BirdType.EAGLE, 1, game);
        lockedTarget.health = 76.0;
        Bird rival = new Bird(210.0, BirdGame3.BirdType.PIGEON, 0, game);
        rival.health = 88.0;

        game.players[0] = rival;
        game.players[1] = lockedTarget;
        game.players[2] = ai;

        setPrivateInt(ai, "aiLockedTargetIndex", 1);
        setPrivateInt(ai, "aiTargetLockFrames", 24);

        Method pickTarget = Bird.class.getDeclaredMethod("pickAITarget");
        pickTarget.setAccessible(true);

        Bird chosen = (Bird) pickTarget.invoke(ai);

        assertEquals(lockedTarget, chosen);
    }

    @Test
    void aiTargetLockYieldsWhenAnotherTargetIsClearlyBetter() throws Exception {
        BirdGame3 game = new BirdGame3();
        game.activePlayers = 3;

        Bird ai = new Bird(100.0, BirdGame3.BirdType.PENGUIN, 2, game);
        Bird lockedTarget = new Bird(520.0, BirdGame3.BirdType.EAGLE, 1, game);
        lockedTarget.health = 100.0;
        Bird rival = new Bird(170.0, BirdGame3.BirdType.PIGEON, 0, game);
        rival.health = 30.0;

        game.players[0] = rival;
        game.players[1] = lockedTarget;
        game.players[2] = ai;

        setPrivateInt(ai, "aiLockedTargetIndex", 1);
        setPrivateInt(ai, "aiTargetLockFrames", 24);

        Method pickTarget = Bird.class.getDeclaredMethod("pickAITarget");
        pickTarget.setAccessible(true);

        Bird chosen = (Bird) pickTarget.invoke(ai);

        assertEquals(rival, chosen);
    }

    @Test
    void verticallyStackedAlliedCpusBreakApartAndFastFallTowardDistantTarget() throws Exception {
        BirdGame3 game = new BirdGame3();
        game.activePlayers = 3;
        game.selectedMap = BirdGame3.MapType.CAVE;
        game.teamModeEnabled = true;
        int[] teams = (int[]) getPrivateObject(game, "playerTeams");
        teams[0] = 1;
        teams[1] = 2;
        teams[2] = 2;

        Bird target = new Bird(1_200.0, BirdGame3.BirdType.PIGEON, 0, game);
        target.y = 1_650.0;
        Bird opiumBird = new Bird(1_200.0, BirdGame3.BirdType.OPIUMBIRD, 1, game);
        opiumBird.y = 320.0;
        Bird raven = new Bird(1_205.0, BirdGame3.BirdType.RAVEN, 2, game);
        raven.y = 355.0;

        game.players[0] = target;
        game.players[1] = opiumBird;
        game.players[2] = raven;
        game.isAI[1] = true;
        game.isAI[2] = true;
        int[] cpuLevels = (int[]) getPrivateObject(game, "cpuLevels");
        cpuLevels[1] = 5;
        setPrivateInt(opiumBird, "aiProgressTargetIndex", 0);
        setPrivateDouble(opiumBird, "aiBestTargetDistance", opiumBird.combatDistanceTo(target));
        setPrivateInt(opiumBird, "aiStackedFrames", 23);

        opiumBird.update(1.0);

        assertTrue(game.isRightPressed(1),
                "Odd-numbered stacked CPU slots should peel right when the target is directly below.");
        assertTrue(game.isBlockPressed(1),
                "The escape state should fast-fall instead of continuing the vertical bounce.");
        assertTrue(getPrivateInt(opiumBird, "aiNavigationEscapeFrames") > 0);
        assertFalse(game.isJumpPressed(1),
                "The anti-stuck descent must release Jump so flying birds cannot hover forever.");
    }

    @Test
    void aiNavigationEscapeDoesNotInterruptNearbyCombat() throws Exception {
        BirdGame3 game = new BirdGame3();
        game.activePlayers = 2;

        Bird target = new Bird(1_230.0, BirdGame3.BirdType.PIGEON, 0, game);
        target.y = 520.0;
        Bird ai = new Bird(1_200.0, BirdGame3.BirdType.RAVEN, 1, game);
        ai.y = 420.0;
        game.players[0] = target;
        game.players[1] = ai;
        game.isAI[1] = true;
        setPrivateInt(ai, "aiNoProgressFrames", 104);
        setPrivateInt(ai, "aiProgressTargetIndex", 0);
        setPrivateDouble(ai, "aiBestTargetDistance", ai.combatDistanceTo(target));

        ai.update(1.0);

        assertEquals(0, getPrivateInt(ai, "aiNavigationEscapeFrames"),
                "Normal close-range exchanges should not activate navigation recovery.");
    }

    @Test
    void penguinAiUsesIceJumpToClimbTowardHigherTarget() {
        BirdGame3 game = new BirdGame3();
        game.activePlayers = 2;
        game.selectedMap = BirdGame3.MapType.FOREST;

        Platform upperPlatform = new Platform(980.0, BirdGame3.GROUND_Y - 480.0, 260.0, 46.0);
        game.platforms.add(upperPlatform);

        Bird penguin = new Bird(1040.0, BirdGame3.BirdType.PENGUIN, 0, game);
        penguin.y = BirdGame3.GROUND_Y - 80.0;
        Bird target = new Bird(1080.0, BirdGame3.BirdType.EAGLE, 1, game);
        target.y = upperPlatform.y - 80.0;

        game.players[0] = penguin;
        game.players[1] = target;
        game.isAI[0] = true;

        penguin.update(1.0);

        assertTrue(game.isSpecialPressed(0));
    }

    @Test
    void findClimbPlatformAvoidsUnreachableLedgeForPenguin() throws Exception {
        BirdGame3 game = new BirdGame3();
        Bird penguin = new Bird(960.0, BirdGame3.BirdType.PENGUIN, 0, game);

        Platform intermediate = new Platform(900.0, 1910.0, 200.0, 46.0);
        Platform unreachable = new Platform(1600.0, 1710.0, 200.0, 46.0);
        game.platforms.add(intermediate);
        game.platforms.add(unreachable);

        Method findClimb = Bird.class.getDeclaredMethod("findClimbPlatform", double.class, double.class);
        findClimb.setAccessible(true);

        Platform chosen = (Platform) findClimb.invoke(penguin, 1730.0, 600.0);

        assertEquals(intermediate, chosen);
    }

    @Test
    void hummingbirdCannotKeepClimbingAboveCameraReach() {
        BirdGame3 game = new BirdGame3();
        game.activePlayers = 1;
        Bird hummingbird = new Bird(600, BirdGame3.BirdType.HUMMINGBIRD, 0, game);
        game.players[0] = hummingbird;
        hummingbird.y = -60.0;
        hummingbird.vy = -3.5;
        game.setLocalActionsForKey(game.jumpKeyForPlayer(0), true);

        hummingbird.update(1.0);

        assertTrue(hummingbird.health < Bird.STARTING_HEALTH);
        assertTrue(hummingbird.vy > 0.0);
    }

    @Test
    void batCannotImmediatelyRehangAfterDroppingFromCeiling() throws Exception {
        BirdGame3 game = new BirdGame3();
        game.activePlayers = 1;
        Platform ceiling = new Platform(1000.0, 480.0, 320, 40);
        game.platforms.add(ceiling);

        Bird bat = new Bird(1120.0, BirdGame3.BirdType.BAT, 0, game);
        game.players[0] = bat;
        bat.x = 1120.0;
        bat.y = ceiling.y + ceiling.h + 2;

        Field batHangPlatformField = Bird.class.getDeclaredField("batHangPlatform");
        batHangPlatformField.setAccessible(true);
        batHangPlatformField.set(bat, ceiling);
        bat.batHanging = true;
        setPrivateInt(bat, "batHangLockTimer", 0);

        Method handleBatHanging = Bird.class.getDeclaredMethod("handleBatHanging", boolean.class);
        handleBatHanging.setAccessible(true);

        game.setLocalActionsForKey(game.jumpKeyForPlayer(0), true);

        boolean handledDropFrame = (boolean) handleBatHanging.invoke(bat, false);
        assertFalse(handledDropFrame);
        assertFalse(bat.batHanging);
        assertEquals(14, getPrivateInt(bat, "batRehangCooldownTimer"));

        bat.y = ceiling.y + ceiling.h + 10;
        bat.vy = -3.5;

        boolean handledImmediateRetry = (boolean) handleBatHanging.invoke(bat, false);
        assertFalse(handledImmediateRetry);
        assertFalse(bat.batHanging);

        setPrivateInt(bat, "batRehangCooldownTimer", 0);
        bat.vy = -3.5;

        boolean handledRetryAfterCooldown = (boolean) handleBatHanging.invoke(bat, false);
        assertTrue(handledRetryAfterCooldown);
        assertTrue(bat.batHanging);
    }

    @Test
    void batEchoLanceBouncesIntoTargetsOffPlatforms() throws Exception {
        BirdGame3 game = new BirdGame3();
        game.activePlayers = 2;

        Bird bat = new Bird(200.0, BirdGame3.BirdType.BAT, 0, game);
        Bird target = new Bird(300.0, BirdGame3.BirdType.PIGEON, 1, game);
        bat.y = 200.0;
        target.y = 230.0;
        bat.facingRight = true;
        bat.batHanging = true;
        game.players[0] = bat;
        game.players[1] = target;
        game.platforms.add(new Platform(170.0, 360.0, 260.0, 24.0));

        invokePrivateBooleanVoid(bat, "specialBatNeutral", false);

        assertTrue(getPrivateBoolean(bat, "batEchoFxBounced"),
                "Echo Lance should record a platform ricochet.");
        assertTrue(target.health <= Bird.STARTING_HEALTH - 13.0,
                "The rebound lane should land the stronger bounced Echo Lance hit.");
    }

    @Test
    void batWingcutCanSnapIntoCeilingHang() throws Exception {
        BirdGame3 game = new BirdGame3();
        game.activePlayers = 1;
        Platform ceiling = new Platform(160.0, 300.0, 420.0, 40.0);
        game.platforms.add(ceiling);

        Bird bat = new Bird(220.0, BirdGame3.BirdType.BAT, 0, game);
        bat.y = ceiling.y + ceiling.h + 28.0;
        bat.facingRight = true;
        game.players[0] = bat;

        invokePrivateBooleanVoid(bat, "specialBatWingcut", false);
        bat.update(1.0);

        assertTrue(bat.batHanging,
                "Wingcut should convert an underside touch into Ceiling Hang.");
        assertEquals(0, getPrivateInt(bat, "batWingcutTimer"));
    }

    @Test
    void batWingcutFromHangSkimsCeilingBeforeRelatching() throws Exception {
        BirdGame3 game = new BirdGame3();
        game.activePlayers = 1;
        Platform ceiling = new Platform(160.0, 300.0, 420.0, 40.0);
        game.platforms.add(ceiling);

        Bird bat = new Bird(220.0, BirdGame3.BirdType.BAT, 0, game);
        bat.y = ceiling.y + ceiling.h + 2.0;
        bat.facingRight = true;
        bat.batHanging = true;
        game.players[0] = bat;
        Field batHangPlatformField = Bird.class.getDeclaredField("batHangPlatform");
        batHangPlatformField.setAccessible(true);
        batHangPlatformField.set(bat, ceiling);

        invokePrivateBooleanVoid(bat, "specialBatWingcut", false);
        double startX = bat.x;
        for (int i = 0; i < 6; i++) {
            bat.update(1.0);
        }

        assertTrue(bat.x > startX + 80.0,
                "Wingcut from Ceiling Hang should travel sideways along the underside.");
        assertTrue(Math.abs(bat.y - (ceiling.y + ceiling.h + 2.0)) < 10.0,
                "Ceiling Wingcut should keep Bat near the platform underside instead of dropping immediately.");

        for (int i = 0; i < 20; i++) {
            bat.update(1.0);
        }

        assertTrue(bat.batHanging,
                "Ceiling Wingcut should relatch when the dash ends under a hangable platform.");
    }

    @Test
    void batMoonriseIsOncePerAirtimeRecovery() throws Exception {
        BirdGame3 game = new BirdGame3();
        game.activePlayers = 1;

        Bird bat = new Bird(240.0, BirdGame3.BirdType.BAT, 0, game);
        bat.y = BirdGame3.GROUND_Y - 260.0;
        game.players[0] = bat;

        game.setLocalActionsForKey(game.jumpKeyForPlayer(0), true);
        invokePrivateVoid(bat, "special");

        assertTrue(getPrivateBoolean(bat, "batMoonriseUsed"));
        assertTrue(bat.vy < -18.0,
                "Moonrise should give Bat a strong vertical recovery.");

        setPrivateInt(bat, "batMoonriseTimer", 0);
        bat.vy = 0.0;
        invokePrivateVoid(bat, "special");

        assertEquals(0.0, bat.vy, 0.0001,
                "Moonrise should not restart before Bat lands.");
        game.setLocalActionsForKey(game.jumpKeyForPlayer(0), false);

        bat.y = BirdGame3.GROUND_Y - 10.0;
        bat.update(1.0);

        assertFalse(getPrivateBoolean(bat, "batMoonriseUsed"),
                "Landing should refresh Bat's up special.");
    }

    @Test
    void batCeilingReleaseEmpowersNextAerial() throws Exception {
        double baselineDamage = batForwardAirDamageAfterCeilingRelease(false);
        double ambushDamage = batForwardAirDamageAfterCeilingRelease(true);

        assertTrue(ambushDamage > baselineDamage,
                "Dropping from Ceiling Hang should empower Bat's next aerial attack.");
    }

    @Test
    void batSilentDescentFromHangMeteorsTargetsBelow() throws Exception {
        BirdGame3 game = new BirdGame3();
        game.activePlayers = 2;

        Bird bat = new Bird(260.0, BirdGame3.BirdType.BAT, 0, game);
        Bird target = new Bird(262.0, BirdGame3.BirdType.PIGEON, 1, game);
        bat.y = 240.0;
        target.y = 338.0;
        bat.batHanging = true;
        game.players[0] = bat;
        game.players[1] = target;

        invokePrivateBooleanVoid(bat, "specialBatSilentDescent", false);
        for (int i = 0; i < 8; i++) {
            bat.update(1.0);
        }

        assertTrue(target.health < Bird.STARTING_HEALTH,
                "Silent Descent should hit targets underneath Bat.");
        assertTrue(target.vy > 0.0,
                "Starting Silent Descent from Ceiling Hang should meteor targets downward.");
    }

    @Test
    void batSilentDescentFromGroundRisesBeforeDiving() throws Exception {
        BirdGame3 game = new BirdGame3();
        game.activePlayers = 1;

        Bird bat = new Bird(260.0, BirdGame3.BirdType.BAT, 0, game);
        bat.y = BirdGame3.GROUND_Y - 80.0;
        game.players[0] = bat;

        invokePrivateBooleanVoid(bat, "specialBatSilentDescent", false);
        double startY = bat.y;

        for (int i = 0; i < 4; i++) {
            bat.update(1.0);
        }

        assertTrue(bat.y < startY - 20.0,
                "Grounded Silent Descent should launch Bat upward before the stall.");
        assertTrue(getPrivateInt(bat, "batSilentStallTimer") > 0,
                "Grounded Silent Descent should still be in its startup sequence after the rise.");

        boolean sawDive = false;
        for (int i = 0; i < 16; i++) {
            bat.update(1.0);
            sawDive |= getPrivateInt(bat, "batSilentDiveTimer") > 0 || bat.vy > 0.0;
        }

        assertTrue(sawDive,
                "Grounded Silent Descent should transition into a downward dive.");
    }

    @Test
    void batCathedralEchoStartsLingeringUltimateState() throws Exception {
        BirdGame3 game = new BirdGame3();
        game.activePlayers = 2;

        Bird bat = new Bird(220.0, BirdGame3.BirdType.BAT, 0, game);
        Bird target = new Bird(312.0, BirdGame3.BirdType.PIGEON, 1, game);
        bat.y = target.y = BirdGame3.GROUND_Y - 80.0;
        game.players[0] = bat;
        game.players[1] = target;

        invokePrivateVoid(bat, "specialBatCathedralEcho");

        assertTrue(getPrivateInt(bat, "batCathedralTimer") > 0,
                "Cathedral Echo should persist after activation.");
        assertTrue(target.health < Bird.STARTING_HEALTH,
                "Cathedral Echo should open with an outward burst.");
        assertTrue(getPrivateInt(bat, "batCathedralWaveIndex") > 0,
                "Cathedral Echo should immediately begin its pulse sequence.");
    }

    private static double batForwardAirDamageAfterCeilingRelease(boolean releaseFromHang) throws Exception {
        BirdGame3 game = new BirdGame3();
        game.activePlayers = 2;

        Bird bat = new Bird(220.0, BirdGame3.BirdType.BAT, 0, game);
        Bird target = new Bird(332.0, BirdGame3.BirdType.PIGEON, 1, game);
        bat.y = target.y = BirdGame3.GROUND_Y - 260.0;
        bat.facingRight = true;
        game.players[0] = bat;
        game.players[1] = target;

        if (releaseFromHang) {
            bat.batHanging = true;
            invokePrivateVoid(bat, "releaseBatHang");
        }

        game.setLocalActionsForKey(game.rightKeyForPlayer(0), true);
        game.setLocalActionsForKey(game.attackKeyForPlayer(0), true);
        bat.update(1.0);

        return Bird.STARTING_HEALTH - target.health;
    }

    @Test
    void birdsUniversallyGrabNearbyLedges() throws Exception {
        BirdGame3 game = new BirdGame3();
        game.activePlayers = 1;
        game.selectedMap = BirdGame3.MapType.BATTLEFIELD;
        Platform mainIsland = new Platform(1000.0, BirdGame3.GROUND_Y - 220.0, 900.0, 70.0);
        game.platforms.add(mainIsland);

        Bird pigeon = new Bird(mainIsland.x - 84.0, BirdGame3.BirdType.PIGEON, 0, game);
        pigeon.y = mainIsland.y - 12.0;
        pigeon.vx = 12.0;
        pigeon.vy = 4.0;
        game.players[0] = pigeon;

        pigeon.update(1.0);

        assertTrue(getPrivateBoolean(pigeon, "ledgeHanging"));
        assertEquals(mainIsland, getPrivateObject(pigeon, "ledgePlatform"));
        assertTrue(pigeon.facingRight, "Bird should face back toward the stage while hanging.");
        assertTrue(pigeon.canDoubleJump, "Ledge grab should refresh recovery resources.");
        assertTrue(pigeon.y < mainIsland.y, "Bird should snap below the top lip instead of landing on the platform.");
    }

    @Test
    void droppingFromLedgeAppliesRegrabLockout() throws Exception {
        BirdGame3 game = new BirdGame3();
        game.activePlayers = 1;
        game.selectedMap = BirdGame3.MapType.BATTLEFIELD;
        Platform mainIsland = new Platform(1000.0, BirdGame3.GROUND_Y - 220.0, 900.0, 70.0);
        game.platforms.add(mainIsland);

        Bird pigeon = new Bird(mainIsland.x - 84.0, BirdGame3.BirdType.PIGEON, 0, game);
        game.players[0] = pigeon;

        pigeon.y = mainIsland.y - 12.0;
        pigeon.vx = 12.0;
        pigeon.vy = 4.0;
        pigeon.update(1.0);
        assertTrue(getPrivateBoolean(pigeon, "ledgeHanging"));

        setPrivateInt(pigeon, "ledgeLockTimer", 0);
        game.setLocalActionsForKey(game.blockKeyForPlayer(0), true);
        pigeon.update(1.0);

        assertFalse(getPrivateBoolean(pigeon, "ledgeHanging"));
        assertTrue(getPrivateInt(pigeon, "ledgeRegrabCooldownTimer") > 0,
                "Dropping from ledge should prevent immediate regrab stalling.");

        game.setLocalActionsForKey(game.blockKeyForPlayer(0), false);
        pigeon.x = mainIsland.x - 84.0;
        pigeon.y = mainIsland.y - 12.0;
        pigeon.vx = 12.0;
        pigeon.vy = 4.0;
        pigeon.update(1.0);

        assertFalse(getPrivateBoolean(pigeon, "ledgeHanging"),
                "Regrab cooldown should block an immediate second ledge catch.");

        setPrivateInt(pigeon, "ledgeRegrabCooldownTimer", 0);
        pigeon.x = mainIsland.x - 84.0;
        pigeon.y = mainIsland.y - 12.0;
        pigeon.vx = 12.0;
        pigeon.vy = 4.0;
        pigeon.update(1.0);

        assertTrue(getPrivateBoolean(pigeon, "ledgeHanging"));
    }

    @Test
    void vineGrapplePickupNowGrantsOneUse() throws Exception {
        BirdGame3 game = new BirdGame3();
        Bird bird = new Bird(1000.0, BirdGame3.BirdType.PIGEON, 0, game);
        game.players[0] = bird;
        game.powerUps.add(new PowerUp(bird.x + 40.0, bird.y + 40.0, PowerUpType.VINE_GRAPPLE));

        invokePrivateVoid(bird, "handlePowerUpPickup");

        assertEquals(1, getPrivateInt(bird, "grappleUses"));
    }

    @Test
    void vineGrappleSpawnsTemporaryVineFromPlatformAbove() throws Exception {
        BirdGame3 game = new BirdGame3();
        game.activePlayers = 1;
        Platform above = new Platform(900.0, 560.0, 320, 36);
        game.platforms.add(above);

        Bird bird = new Bird(1000.0, BirdGame3.BirdType.PIGEON, 0, game);
        bird.x = 1000.0;
        bird.y = 940.0;
        game.players[0] = bird;

        setPrivateInt(bird, "grappleUses", 1);
        setPrivateInt(bird, "grappleTimer", 480);
        bird.specialCooldown = 0;
        game.setLocalActionsForKey(game.specialKeyForPlayer(0), true);

        invokePrivateVoid(bird, "handleVineGrapple");

        assertTrue(bird.onVine);
        assertNotNull(bird.attachedVine);
        assertTrue(bird.attachedVine.temporary);
        assertEquals(above.y + above.h, bird.attachedVine.baseY, 0.0001);
        assertEquals(0, getPrivateInt(bird, "grappleUses"));
        assertEquals(1, game.swingingVines.size());
    }

    @Test
    void vineAutoLaunchNowDependsOnSwingSpeedAndOutwardArc() throws Exception {
        BirdGame3 game = new BirdGame3();
        SwingingVine fastOutward = new SwingingVine(1200.0, 400.0, 420.0);
        fastOutward.angle = 0.55;
        fastOutward.angularVelocity = 0.045;

        SwingingVine slowVine = new SwingingVine(1200.0, 400.0, 420.0);
        slowVine.angle = 0.55;
        slowVine.angularVelocity = 0.03;

        SwingingVine inwardVine = new SwingingVine(1200.0, 400.0, 420.0);
        inwardVine.angle = 0.55;
        inwardVine.angularVelocity = -0.045;

        Method shouldAutoLaunch = BirdGame3.class.getDeclaredMethod("shouldAutoLaunchFromVine", SwingingVine.class);
        shouldAutoLaunch.setAccessible(true);

        assertTrue((boolean) shouldAutoLaunch.invoke(game, fastOutward));
        assertFalse((boolean) shouldAutoLaunch.invoke(game, slowVine));
        assertFalse((boolean) shouldAutoLaunch.invoke(game, inwardVine));
    }

    @Test
    void releasedTemporaryVineDetachesBeforeDisappearing() throws Exception {
        BirdGame3 game = new BirdGame3();
        SwingingVine vine = new SwingingVine(1200.0, 420.0, 320.0);
        vine.temporary = true;
        game.swingingVines.add(vine);

        Method updateSwingingVines = BirdGame3.class.getDeclaredMethod("updateSwingingVines");
        updateSwingingVines.setAccessible(true);
        updateSwingingVines.invoke(game);

        assertEquals(1, game.swingingVines.size());
        assertTrue(vine.detaching);
    }

    @Test
    void dockWaterLetsBirdSwimUpward() throws Exception {
        BirdGame3 game = new BirdGame3();
        game.activePlayers = 1;
        game.selectedMap = BirdGame3.MapType.DOCK;

        Method setupDockArena = BirdGame3.class.getDeclaredMethod("setupDockArena");
        setupDockArena.setAccessible(true);
        setupDockArena.invoke(game);

        Bird bird = new Bird(3900.0, BirdGame3.BirdType.PIGEON, 0, game);
        bird.x = 3900.0;
        bird.y = game.dockWaterSurfaceY() + 120.0;
        game.players[0] = bird;
        game.setLocalActionsForKey(game.jumpKeyForPlayer(0), true);

        double startY = bird.y;
        bird.update(1.0);

        assertTrue(bird.vy < 0.0);
        assertTrue(bird.y < startY);
    }

    @Test
    void dockUsesSandFloorOutsideWaterGap() throws Exception {
        BirdGame3 game = new BirdGame3();
        game.activePlayers = 1;
        game.selectedMap = BirdGame3.MapType.DOCK;

        Method setupDockArena = BirdGame3.class.getDeclaredMethod("setupDockArena");
        setupDockArena.setAccessible(true);
        setupDockArena.invoke(game);

        Bird sandBird = new Bird(1600.0, BirdGame3.BirdType.PIGEON, 0, game);
        sandBird.x = 1600.0;
        sandBird.y = BirdGame3.GROUND_Y + 24.0;
        sandBird.vy = 6.0;

        Bird waterBird = new Bird(3900.0, BirdGame3.BirdType.PIGEON, 0, game);
        waterBird.x = 3900.0;
        waterBird.y = BirdGame3.GROUND_Y + 24.0;
        waterBird.vy = 6.0;

        assertTrue(sandBird.isOnGround());
        assertFalse(waterBird.isOnGround());
        assertTrue(waterBird.y > BirdGame3.GROUND_Y - 20.0);
    }

    @Test
    void dockSkiffPlatformsSitAboveWaterline() throws Exception {
        BirdGame3 game = new BirdGame3();
        game.selectedMap = BirdGame3.MapType.DOCK;

        Method setupDockArena = BirdGame3.class.getDeclaredMethod("setupDockArena");
        setupDockArena.setAccessible(true);
        setupDockArena.invoke(game);

        double waterline = game.dockWaterSurfaceY();
        long submergedSkiffs = game.platforms.stream()
                .filter(p -> p.x >= 2800.0 && p.x <= 3400.0 && p.w <= 260.0 && p.h <= 24.0)
                .filter(p -> p.y >= waterline)
                .count();

        assertEquals(0, submergedSkiffs);
    }

    @Test
    void dockMatchSpawnsEveryBirdOnAStableSurface() throws Exception {
        BirdGame3 game = new BirdGame3();
        game.selectedMap = BirdGame3.MapType.DOCK;
        game.activePlayers = 4;
        game.players[0] = new Bird(0.0, BirdGame3.BirdType.PELICAN, 0, game);
        game.players[1] = new Bird(0.0, BirdGame3.BirdType.GOOSE, 1, game);
        game.players[2] = new Bird(0.0, BirdGame3.BirdType.RAVEN, 2, game);
        game.players[3] = new Bird(0.0, BirdGame3.BirdType.HEISENBIRD, 3, game);

        Method setupDockArena = BirdGame3.class.getDeclaredMethod("setupDockArena");
        setupDockArena.setAccessible(true);
        setupDockArena.invoke(game);
        Method positionBattlefieldSpawns = BirdGame3.class.getDeclaredMethod("positionBattlefieldSpawns");
        positionBattlefieldSpawns.setAccessible(true);
        positionBattlefieldSpawns.invoke(game);

        for (int i = 0; i < game.activePlayers; i++) {
            Bird bird = game.players[i];
            assertNotNull(bird);
            assertTrue(bird.isOnGround(),
                    bird.type.name + " should begin the Dock countdown standing on a platform.");
            assertEquals(bird.x, bird.prevX, 0.0001);
            assertEquals(bird.y, bird.prevY, 0.0001);
        }
    }

    @Test
    void lockedMatchCountdownUsesIdlePoseEvenForAirborneSpawns() throws Exception {
        BirdGame3 game = new BirdGame3();
        Bird bird = new Bird(300.0, BirdGame3.BirdType.PELICAN, 0, game);
        bird.y = BirdGame3.GROUND_Y - 500.0;
        game.players[0] = bird;

        Method animationState = Bird.class.getDeclaredMethod("currentBirdAnimationState");
        animationState.setAccessible(true);
        setPrivateInt(game, "matchIntroOverlayFrames", 120);

        assertEquals("IDLE", animationState.invoke(bird).toString(),
                "The frozen 3-2-1 countdown should never display a falling or attack pose.");

        setPrivateInt(game, "matchIntroOverlayFrames", 40);
        assertEquals("FALL", animationState.invoke(bird).toString(),
                "Once the fight is live, an airborne bird should resume its normal fall pose.");
    }

    @Test
    void dockLeverLaunchesPirateBomb() throws Exception {
        BirdGame3 game = new BirdGame3();
        game.selectedMap = BirdGame3.MapType.DOCK;
        game.activePlayers = 2;

        Method setupDockArena = BirdGame3.class.getDeclaredMethod("setupDockArena");
        setupDockArena.setAccessible(true);
        setupDockArena.invoke(game);

        Bird puller = new Bird(0.0, BirdGame3.BirdType.PELICAN, 0, game);
        Bird target = new Bird(3600.0, BirdGame3.BirdType.EAGLE, 1, game);
        game.players[0] = puller;
        game.players[1] = target;

        puller.x = getPrivateDouble(game, "dockLeverX") - 40.0;
        puller.y = getPrivateDouble(game, "dockLeverY") - 40.0;

        Method launchDockShipBomb = BirdGame3.class.getDeclaredMethod("launchDockShipBomb", Bird.class, Bird.class);
        launchDockShipBomb.setAccessible(true);
        launchDockShipBomb.invoke(game, puller, target);

        DockShipBomb bomb = (DockShipBomb) getPrivateObject(game, "dockShipBomb");
        assertNotNull(bomb);
        assertTrue(getPrivateInt(game, "dockLeverCooldown") > 0);
        assertFalse(bomb.fired);
        assertTrue(bomb.launchDelayFrames > 0);
    }

    @Test
    void dockStageUpdateDoesNotOverflowLeverState() throws Exception {
        BirdGame3 game = new BirdGame3();
        game.selectedMap = BirdGame3.MapType.DOCK;
        game.activePlayers = 2;

        Method setupDockArena = BirdGame3.class.getDeclaredMethod("setupDockArena");
        setupDockArena.setAccessible(true);
        setupDockArena.invoke(game);

        game.players[0] = new Bird(1040.0, BirdGame3.BirdType.PIGEON, 0, game);
        game.players[1] = new Bird(3660.0, BirdGame3.BirdType.EAGLE, 1, game);

        Method updateWorldFixed = BirdGame3.class.getDeclaredMethod("updateWorldFixed");
        updateWorldFixed.setAccessible(true);

        assertDoesNotThrow(() -> updateWorldFixed.invoke(game));
    }

    @Test
    void ashfallCathedralUsesIslandBoundsAndThermals() throws Exception {
        BirdGame3 game = new BirdGame3();
        game.selectedMap = BirdGame3.MapType.ASHFALL_CATHEDRAL;

        Method setupAshfallCathedralArena = BirdGame3.class.getDeclaredMethod("setupAshfallCathedralArena");
        setupAshfallCathedralArena.setAccessible(true);
        setupAshfallCathedralArena.invoke(game);

        Bird bird = new Bird(3000.0, BirdGame3.BirdType.PHOENIX, 0, game);
        game.players[0] = bird;

        assertTrue(bird.usesIslandBounds());
        assertFalse(bird.hasSolidGroundFloorUnderBody(),
                "Ashfall Cathedral should not have the normal invisible ground floor.");
        assertTrue(game.windVents.size() >= 3);
        assertTrue(game.platforms.stream().anyMatch(p -> p.y > BirdGame3.GROUND_Y),
                "Ashfall Cathedral should include low recovery fragments over the lava sea.");
    }

    @Test
    void ashfallGeyserWarningDoesNotDamage() throws Exception {
        BirdGame3 game = new BirdGame3();
        game.selectedMap = BirdGame3.MapType.ASHFALL_CATHEDRAL;
        game.activePlayers = 1;

        Method setupAshfallCathedralArena = BirdGame3.class.getDeclaredMethod("setupAshfallCathedralArena");
        setupAshfallCathedralArena.setAccessible(true);
        setupAshfallCathedralArena.invoke(game);

        Bird bird = new Bird(1540.0, BirdGame3.BirdType.EAGLE, 0, game);
        bird.x = 1540.0;
        bird.y = BirdGame3.GROUND_Y - 138.0 - 80.0;
        game.players[0] = bird;
        double startingHealth = bird.health;
        game.simTick = 40L;

        Method updateAshfallCathedralHazards = BirdGame3.class.getDeclaredMethod("updateAshfallCathedralHazards");
        updateAshfallCathedralHazards.setAccessible(true);
        updateAshfallCathedralHazards.invoke(game);

        assertEquals(startingHealth, bird.health, 0.0001);
        assertEquals(0.0, bird.vy, 0.0001);
    }

    @Test
    void ashfallGeyserImpactLaunchesAndDamages() throws Exception {
        BirdGame3 game = new BirdGame3();
        game.selectedMap = BirdGame3.MapType.ASHFALL_CATHEDRAL;
        game.activePlayers = 1;

        Method setupAshfallCathedralArena = BirdGame3.class.getDeclaredMethod("setupAshfallCathedralArena");
        setupAshfallCathedralArena.setAccessible(true);
        setupAshfallCathedralArena.invoke(game);

        Bird bird = new Bird(1540.0, BirdGame3.BirdType.EAGLE, 0, game);
        bird.x = 1540.0;
        bird.y = BirdGame3.GROUND_Y - 138.0 - 80.0;
        game.players[0] = bird;
        double startingHealth = bird.health;
        game.simTick = 92L;

        Method updateAshfallCathedralHazards = BirdGame3.class.getDeclaredMethod("updateAshfallCathedralHazards");
        updateAshfallCathedralHazards.setAccessible(true);
        updateAshfallCathedralHazards.invoke(game);

        assertTrue(bird.health < startingHealth);
        assertTrue(bird.vy < -10.0);
        assertTrue(bird.stunTime > 0.0);
        assertTrue(game.isAchievementUnlocked(BirdGame3Achievement.GEYSER_RIDER));
        assertEquals(1, game.achievementProgressValue(BirdGame3Achievement.GEYSER_RIDER));
    }

    @Test
    void dockBombLocksOnBeforeFiring() throws Exception {
        BirdGame3 game = new BirdGame3();
        game.selectedMap = BirdGame3.MapType.DOCK;
        game.activePlayers = 2;

        Method setupDockArena = BirdGame3.class.getDeclaredMethod("setupDockArena");
        setupDockArena.setAccessible(true);
        setupDockArena.invoke(game);

        Bird puller = new Bird(0.0, BirdGame3.BirdType.PELICAN, 0, game);
        Bird target = new Bird(3600.0, BirdGame3.BirdType.EAGLE, 1, game);
        game.players[0] = puller;
        game.players[1] = target;

        Method launchDockShipBomb = BirdGame3.class.getDeclaredMethod("launchDockShipBomb", Bird.class, Bird.class);
        launchDockShipBomb.setAccessible(true);
        launchDockShipBomb.invoke(game, puller, target);

        Method updateDockShipBomb = BirdGame3.class.getDeclaredMethod("updateDockShipBomb");
        updateDockShipBomb.setAccessible(true);
        DockShipBomb bomb = (DockShipBomb) getPrivateObject(game, "dockShipBomb");
        int delay = bomb.launchDelayFrames;
        for (int i = 0; i < delay; i++) {
            updateDockShipBomb.invoke(game);
        }

        bomb = (DockShipBomb) getPrivateObject(game, "dockShipBomb");
        assertNotNull(bomb);
        assertTrue(bomb.fired);
        assertTrue(bomb.cannonFlashFrames > 0);
    }

    @Test
    void dockMapCanBeUnlockedFromShopPreview() throws Exception {
        BirdGame3 game = new BirdGame3();
        ShopPreview preview = new ShopPreview(null, "MAP_DOCK", "Broken Harbor Map");

        Method isOwned = BirdGame3.class.getDeclaredMethod("isShopPreviewOwned", ShopPreview.class);
        isOwned.setAccessible(true);
        Method unlock = BirdGame3.class.getDeclaredMethod("unlockShopPreview", ShopPreview.class);
        unlock.setAccessible(true);

        assertFalse((boolean) isOwned.invoke(game, preview));

        unlock.invoke(game, preview);

        assertTrue((boolean) isOwned.invoke(game, preview));
        assertTrue(getPrivateBoolean(game, "dockMapUnlocked"));
    }

    @Test
    void premiumPacksIncludeRoadrunnerAndDesertRewardsAndUnlockThem() throws Exception {
        BirdGame3 game = new BirdGame3();

        Method buildShopItems = BirdGame3.class.getDeclaredMethod("buildShopItems");
        buildShopItems.setAccessible(true);
        @SuppressWarnings("unchecked")
        List<ShopItem> items = (List<ShopItem>) buildShopItems.invoke(game);

        for (String packName : List.of("Rooftop Pack", "Skyline Pack", "Nebula Pack", "Ascendant Pack")) {
            ShopItem pack = items.stream()
                    .filter(item -> packName.equals(item.name))
                    .findFirst()
                    .orElseThrow();
            assertTrue(pack.previews.stream().anyMatch(preview -> "CHAR_ROADRUNNER".equals(preview.skinKey())));
            assertTrue(pack.previews.stream().anyMatch(preview -> "MAP_DESERT".equals(preview.skinKey())));
        }

        Method isOwned = BirdGame3.class.getDeclaredMethod("isShopPreviewOwned", ShopPreview.class);
        isOwned.setAccessible(true);
        Method unlock = BirdGame3.class.getDeclaredMethod("unlockShopPreview", ShopPreview.class);
        unlock.setAccessible(true);

        ShopPreview roadrunner = new ShopPreview(BirdGame3.BirdType.ROADRUNNER, "CHAR_ROADRUNNER", "Roadrunner");
        ShopPreview desert = new ShopPreview(null, "MAP_DESERT", "Sunscorch Flats Map");

        assertFalse((boolean) isOwned.invoke(game, roadrunner));
        assertFalse((boolean) isOwned.invoke(game, desert));

        unlock.invoke(game, roadrunner);
        unlock.invoke(game, desert);

        assertTrue((boolean) isOwned.invoke(game, roadrunner));
        assertTrue((boolean) isOwned.invoke(game, desert));
        assertTrue(game.roadrunnerUnlocked);
        assertTrue(getPrivateBoolean(game, "desertMapUnlocked"));
    }

    @Test
    void nullRockCannotBeStunnedOrShrunkAndAscendsAtHalfHealth() throws Exception {
        BirdGame3 game = new BirdGame3();
        game.nullRockVultureUnlocked = true;
        Bird nullRock = new Bird(600.0, BirdGame3.BirdType.VULTURE, 0, game);

        Method applySkin = BirdGame3.class.getDeclaredMethod(
                "applySkinChoiceToBird",
                Bird.class,
                BirdGame3.BirdType.class,
                String.class
        );
        applySkin.setAccessible(true);
        applySkin.invoke(game, nullRock, BirdGame3.BirdType.VULTURE, "NULL_ROCK_VULTURE");

        double baseSize = nullRock.baseSizeMultiplier;
        double basePower = nullRock.basePowerMultiplier;
        double baseSpeed = nullRock.baseSpeedMultiplier;

        nullRock.applyStun(90);
        nullRock.applyShrinkEffect();

        assertEquals(0.0, nullRock.stunTime, 0.0001);
        assertEquals(0, nullRock.shrinkTimer);
        assertEquals(baseSize, nullRock.sizeMultiplier, 0.0001);

        nullRock.health = nullRock.getMaxHealth() * 0.50 + 20.0;
        double dealt = nullRock.receiveExternalDamage(40.0);

        assertTrue(dealt > 0.0);
        assertTrue(nullRock.isTrueNullRockForm());
        assertTrue(nullRock.baseSizeMultiplier > baseSize);
        assertTrue(nullRock.basePowerMultiplier > basePower);
        assertTrue(nullRock.baseSpeedMultiplier > baseSpeed);
        assertEquals("P1: True Null Rock", game.healthBarLabel(nullRock));
    }

    @Test
    void nullRockCanStandOnBattlefieldVoidFloor() throws Exception {
        BirdGame3 game = new BirdGame3();
        game.selectedMap = BirdGame3.MapType.BATTLEFIELD;
        Bird nullRock = new Bird(1900.0, BirdGame3.BirdType.VULTURE, 0, game);
        nullRock.isNullRockSkin = true;
        double deepestVisibleTopY = BirdGame3.WORLD_HEIGHT - 160.0;
        nullRock.y = deepestVisibleTopY + 220.0;
        nullRock.vy = 6.0;

        invokePrivateVoid(nullRock, "handleVerticalCollision");

        assertEquals(deepestVisibleTopY, nullRock.y, 0.0001);
        assertTrue(nullRock.isOnGround());
    }

    @Test
    void nullRockPickupUsesExpandedBodyBounds() throws Exception {
        BirdGame3 game = new BirdGame3();
        Bird nullRock = new Bird(1000.0, BirdGame3.BirdType.VULTURE, 0, game);
        nullRock.isNullRockSkin = true;
        game.powerUps.add(new PowerUp(nullRock.x + 130.0, nullRock.y + 40.0, PowerUpType.SPEED));

        invokePrivateVoid(nullRock, "handlePowerUpPickup");

        assertTrue(game.powerUps.isEmpty());
        assertTrue(nullRock.speedTimer > 0);
    }

    @Test
    void attacksCanHitAcrossNullRockExpandedCombatBody() throws Exception {
        BirdGame3 game = new BirdGame3();
        game.activePlayers = 2;

        Bird attacker = new Bird(1000.0, BirdGame3.BirdType.PIGEON, 0, game);
        Bird nullRock = new Bird(1170.0, BirdGame3.BirdType.VULTURE, 1, game);
        nullRock.isNullRockSkin = true;

        game.players[0] = attacker;
        game.players[1] = nullRock;

        invokePrivateVoid(attacker, "attack");

        assertTrue(nullRock.health < Bird.STARTING_HEALTH);
    }

    @Test
    void nullRockRegularAttackStaysFocusedNearItsBeak() throws Exception {
        BirdGame3 game = new BirdGame3();
        game.nullRockVultureUnlocked = true;
        game.activePlayers = 3;

        Bird nullRock = new Bird(1000.0, BirdGame3.BirdType.VULTURE, 0, game);
        Bird targetNearBeak = new Bird(1365.0, BirdGame3.BirdType.EAGLE, 1, game);
        Bird targetBehind = new Bird(820.0, BirdGame3.BirdType.PIGEON, 2, game);
        nullRock.facingRight = true;

        Method applySkin = BirdGame3.class.getDeclaredMethod(
                "applySkinChoiceToBird",
                Bird.class,
                BirdGame3.BirdType.class,
                String.class
        );
        applySkin.setAccessible(true);
        applySkin.invoke(game, nullRock, BirdGame3.BirdType.VULTURE, "NULL_ROCK_VULTURE");

        game.players[0] = nullRock;
        game.players[1] = targetNearBeak;
        game.players[2] = targetBehind;

        invokePrivateVoid(nullRock, "attack");

        assertTrue(targetNearBeak.health < Bird.STARTING_HEALTH);
        assertEquals(Bird.STARTING_HEALTH, targetBehind.health, 0.0001);
    }

    @Test
    void localHealthBarUsesNullRockName() {
        BirdGame3 game = new BirdGame3();
        Bird nullRock = new Bird(600.0, BirdGame3.BirdType.VULTURE, 0, game);
        nullRock.isNullRockSkin = true;

        assertEquals("P1: The Null Rock", game.healthBarLabel(nullRock));
    }

    @Test
    void particleBurstsScaleDownDuringHeavyFightLoad() throws Exception {
        BirdGame3 game = new BirdGame3();
        game.activePlayers = 12;
        for (int i = 0; i < game.activePlayers; i++) {
            game.players[i] = new Bird(600.0 + i * 80.0, BirdGame3.BirdType.PIGEON, i, game);
        }
        for (int i = 0; i < 28; i++) {
            game.crowMinions.add(new CrowMinion(1200.0 + i * 10.0, 400.0, null));
        }
        for (int i = 0; i < 10; i++) {
            game.chickMinions.add(new ChickMinion(1000.0 + i * 20.0, 420.0, 0, false, null));
        }
        for (int i = 0; i < 1500; i++) {
            game.particles.add(new Particle(900.0, 400.0, 0.0, 0.0, javafx.scene.paint.Color.WHITE));
        }

        Method method = BirdGame3.class.getDeclaredMethod("scaledParticleBurstCount", int.class);
        method.setAccessible(true);
        int scaled = (int) method.invoke(game, 200);

        assertTrue(scaled < 200);
        assertTrue(scaled >= 24);
    }

    @Test
    void transientEffectOverflowTrimKeepsParticlesAndMinionsUnderCaps() throws Exception {
        BirdGame3 game = new BirdGame3();
        game.activePlayers = 12;
        for (int i = 0; i < game.activePlayers; i++) {
            game.players[i] = new Bird(500.0 + i * 90.0, BirdGame3.BirdType.EAGLE, i, game);
        }
        for (int i = 0; i < 3200; i++) {
            game.particles.add(new Particle(1000.0, 300.0, 0.0, 0.0, javafx.scene.paint.Color.GOLD));
        }
        for (int i = 0; i < 80; i++) {
            game.crowMinions.add(new CrowMinion(1200.0 + i * 14.0, 300.0, null));
        }
        for (int i = 0; i < 24; i++) {
            game.chickMinions.add(new ChickMinion(1300.0 + i * 18.0, 320.0, 0, false, null));
        }
        for (int i = 0; i < 18; i++) {
            game.piranhaHazards.add(new PiranhaHazard(2600.0 + i * 12.0, 2350.0, -4.5));
        }

        Method trim = BirdGame3.class.getDeclaredMethod("trimTransientEffectOverflow");
        trim.setAccessible(true);
        trim.invoke(game);

        Method particleCapMethod = BirdGame3.class.getDeclaredMethod("activeParticleSoftCap");
        particleCapMethod.setAccessible(true);
        Method crowCapMethod = BirdGame3.class.getDeclaredMethod("activeCrowMinionCap");
        crowCapMethod.setAccessible(true);
        Method chickCapMethod = BirdGame3.class.getDeclaredMethod("activeChickMinionCap");
        chickCapMethod.setAccessible(true);
        Method piranhaCapMethod = BirdGame3.class.getDeclaredMethod("activePiranhaHazardCap");
        piranhaCapMethod.setAccessible(true);

        int particleCap = (int) particleCapMethod.invoke(game);
        int crowCap = (int) crowCapMethod.invoke(game);
        int chickCap = (int) chickCapMethod.invoke(game);
        int piranhaCap = (int) piranhaCapMethod.invoke(game);

        assertTrue(game.particles.size() <= particleCap);
        assertTrue(game.crowMinions.size() <= crowCap);
        assertTrue(game.chickMinions.size() <= chickCap);
        assertTrue(game.piranhaHazards.size() <= piranhaCap);
    }

    @Test
    void trainingHitTrackingBuildsComboSessionDamageAndBlockWindow() throws Exception {
        BirdGame3 game = new BirdGame3();
        game.trainingModeActive = true;

        Bird player = new Bird(100.0, BirdGame3.BirdType.PIGEON, 0, game);
        Bird dummy = new Bird(220.0, BirdGame3.BirdType.EAGLE, 1, game);
        game.players[0] = player;
        game.players[1] = dummy;

        Method record = BirdGame3.class.getDeclaredMethod("recordTrainingHit", Bird.class, Bird.class, double.class);
        record.setAccessible(true);
        record.invoke(game, player, dummy, 18.5);

        assertEquals(1, getPrivateInt(game, "trainingComboHits"));
        assertEquals(18.5, getPrivateDouble(game, "trainingComboDamage"), 0.0001);
        assertEquals(18.5, getPrivateDouble(game, "trainingSessionDamage"), 0.0001);
        assertEquals(18.5, getPrivateDouble(game, "trainingLastHitDamage"), 0.0001);
        assertTrue(getPrivateInt(game, "trainingDummyBlockFrames") > 0);
    }

    @Test
    void trainingComboExpiresAfterWindowButKeepsSessionDamage() throws Exception {
        BirdGame3 game = new BirdGame3();
        game.trainingModeActive = true;

        Bird player = new Bird(100.0, BirdGame3.BirdType.PIGEON, 0, game);
        Bird dummy = new Bird(220.0, BirdGame3.BirdType.EAGLE, 1, game);
        game.players[0] = player;
        game.players[1] = dummy;

        Method record = BirdGame3.class.getDeclaredMethod("recordTrainingHit", Bird.class, Bird.class, double.class);
        record.setAccessible(true);
        record.invoke(game, player, dummy, 12.0);

        Method tickCombo = BirdGame3.class.getDeclaredMethod("updateTrainingComboTracker");
        tickCombo.setAccessible(true);
        for (int i = 0; i < 90; i++) {
            tickCombo.invoke(game);
        }

        assertEquals(0, getPrivateInt(game, "trainingComboHits"));
        assertEquals(0.0, getPrivateDouble(game, "trainingComboDamage"), 0.0001);
        assertEquals(12.0, getPrivateDouble(game, "trainingSessionDamage"), 0.0001);
        assertEquals(12.0, getPrivateDouble(game, "trainingLastHitDamage"), 0.0001);
    }

    @Test
    void academyProvidesDedicatedDrillForEveryBird() throws Exception {
        BirdGame3 game = new BirdGame3();
        Class<?> lessonClass = Class.forName("com.example.birdgame3.BirdGame3$GuidedTutorialLesson");
        Method drillForBird = BirdGame3.class.getDeclaredMethod(
                "trainingAcademyDrillLessonFor", BirdGame3.BirdType.class);
        Method birdForDrill = BirdGame3.class.getDeclaredMethod(
                "trainingAcademyDrillBirdFor", lessonClass);
        drillForBird.setAccessible(true);
        birdForDrill.setAccessible(true);

        for (BirdGame3.BirdType type : BirdGame3.BirdType.values()) {
            Object lesson = drillForBird.invoke(game, type);
            assertNotNull(lesson, type + " should have a dedicated Academy drill");
            assertEquals(type, birdForDrill.invoke(game, lesson),
                    type + " drill should map back to its roster bird");
        }
    }

    @Test
    void pigeonAcademyDrillTracksEveryRooftopRouteHit() throws Exception {
        BirdGame3 game = guidedAcademyGame("PIGEON_DRILL");
        Bird pigeon = new Bird(100.0, BirdGame3.BirdType.PIGEON, 0, game);
        Bird dummy = new Bird(220.0, BirdGame3.BirdType.EAGLE, 1, game);
        Method recordHit = BirdGame3.class.getDeclaredMethod(
                "recordTrainingCharacterDrillHit", Bird.class, Bird.class);
        recordHit.setAccessible(true);

        pigeon.pigeonFeatherBurstTimer = 1;
        recordHit.invoke(game, pigeon, dummy);
        pigeon.pigeonFeatherBurstTimer = 0;
        pigeon.pigeonRushTimer = 1;
        recordHit.invoke(game, pigeon, dummy);
        pigeon.pigeonRushTimer = 0;
        pigeon.pigeonFlutterTimer = 1;
        recordHit.invoke(game, pigeon, dummy);
        pigeon.pigeonFlutterTimer = 0;
        pigeon.pigeonScavengeTimer = 1;
        pigeon.pigeonScavengeAirborne = true;
        recordHit.invoke(game, pigeon, dummy);

        assertTrue(getPrivateBoolean(game, "trainingAcademyPigeonBurstHitSeen"));
        assertTrue(getPrivateBoolean(game, "trainingAcademyPigeonRushHitSeen"));
        assertTrue(getPrivateBoolean(game, "trainingAcademyPigeonFlutterHitSeen"));
        assertTrue(getPrivateBoolean(game, "trainingAcademyPigeonDropPeckHitSeen"));
        assertTrue(invokePrivateBoolean(game, "hasCompletedPigeonTrainingDrill"));
    }

    @Test
    void eagleAcademyDrillTracksEveryAirControlHit() throws Exception {
        BirdGame3 game = guidedAcademyGame("EAGLE_DRILL");
        Bird eagle = new Bird(100.0, BirdGame3.BirdType.EAGLE, 0, game);
        Bird dummy = new Bird(220.0, BirdGame3.BirdType.PIGEON, 1, game);
        Method recordHit = BirdGame3.class.getDeclaredMethod(
                "recordTrainingCharacterDrillHit", Bird.class, Bird.class);
        recordHit.setAccessible(true);

        eagle.raptorCryTimer = 1;
        recordHit.invoke(game, eagle, dummy);
        eagle.raptorCryTimer = 0;
        eagle.raptorRushTimer = 1;
        recordHit.invoke(game, eagle, dummy);
        eagle.raptorRushTimer = 0;
        eagle.raptorClimbTimer = 1;
        recordHit.invoke(game, eagle, dummy);
        eagle.raptorClimbTimer = 0;
        eagle.eagleDiveActive = true;
        recordHit.invoke(game, eagle, dummy);

        assertTrue(getPrivateBoolean(game, "trainingAcademyEagleCryHitSeen"));
        assertTrue(getPrivateBoolean(game, "trainingAcademyEagleRushHitSeen"));
        assertTrue(getPrivateBoolean(game, "trainingAcademyEagleClimbHitSeen"));
        assertTrue(getPrivateBoolean(game, "trainingAcademyEagleDiveHitSeen"));
        assertTrue(invokePrivateBoolean(game, "hasCompletedEagleTrainingDrill"));
    }

    @Test
    void gooseAcademyDrillTeachesNestTerritoryAndChargedHonk() throws Exception {
        BirdGame3 game = guidedAcademyGame("GOOSE_DRILL");
        Bird goose = new Bird(100.0, BirdGame3.BirdType.GOOSE, 0, game);
        Bird dummy = new Bird(220.0, BirdGame3.BirdType.PIGEON, 1, game);

        Method prepare = BirdGame3.class.getDeclaredMethod(
                "prepareGuidedTutorialLessonResources", Bird.class);
        prepare.setAccessible(true);
        prepare.invoke(game, goose);
        assertEquals(Bird.GOOSE_TERRITORY_MAX - 8.0, goose.gooseTerritoryMeter, 0.0001);

        goose.gooseNest = new GooseSpecials.GooseNest(goose.bodyCenterX(), goose.bodyBottomY(), false);
        goose.gooseTerritoryMeter = 18.0;
        Method update = BirdGame3.class.getDeclaredMethod("updateGooseTrainingDrill", Bird.class);
        update.setAccessible(true);
        update.invoke(game, goose);
        assertEquals(Bird.GOOSE_TERRITORY_MAX, goose.gooseTerritoryMeter, 0.0001,
                "Planting the lesson nest should finish the setup meter");

        goose.gooseHonkTimer = 1;
        goose.gooseHonkReleased = true;
        goose.gooseHonkEmpowered = true;
        Method recordHit = BirdGame3.class.getDeclaredMethod(
                "recordTrainingCharacterDrillHit", Bird.class, Bird.class);
        recordHit.setAccessible(true);
        goose.gooseHonkHoldFrames = Bird.GOOSE_HONK_MAX_HOLD_FRAMES - 1;
        recordHit.invoke(game, goose, dummy);
        assertFalse(getPrivateBoolean(game, "trainingAcademyGooseChargedHonkHitSeen"),
                "A partially charged Honk should not clear the final goal");

        goose.gooseHonkHoldFrames = Bird.GOOSE_HONK_MAX_HOLD_FRAMES;
        recordHit.invoke(game, goose, dummy);

        assertTrue(getPrivateBoolean(game, "trainingAcademyGooseNestPlacedSeen"));
        assertTrue(getPrivateBoolean(game, "trainingAcademyGooseTerritoryReadySeen"));
        assertTrue(getPrivateBoolean(game, "trainingAcademyGooseChargedHonkHitSeen"));
        assertTrue(invokePrivateBoolean(game, "hasCompletedGooseTrainingDrill"));
    }

    @Test
    void academyTrainingRosterUsesGuidedLessonBirds() throws Exception {
        BirdGame3 game = new BirdGame3();

        Class<?> academyModeClass = Class.forName("com.example.birdgame3.BirdGame3$TrainingAcademyMode");
        Class<?> lessonClass = Class.forName("com.example.birdgame3.BirdGame3$GuidedTutorialLesson");
        Object guidedMode = enumConstant(academyModeClass, "GUIDED_TUTORIAL");
        Object recoveryLesson = enumConstant(lessonClass, "RECOVERY_AND_LEDGE");

        Method setupRoster = BirdGame3.class.getDeclaredMethod("setupTrainingRoster");
        setupRoster.setAccessible(true);

        setPrivateObject(game, "trainingAcademyMode", guidedMode);
        setPrivateObject(game, "guidedTutorialLesson", recoveryLesson);
        setupRoster.invoke(game);

        assertEquals(BirdGame3.BirdType.PENGUIN, game.players[0].type);
        assertEquals(BirdGame3.BirdType.PIGEON, game.players[1].type);
    }

    @Test
    void resetTrainingPositionsRebuildsFreshRosterAtBattleSpawns() throws Exception {
        BirdGame3 game = new BirdGame3();
        game.trainingModeActive = true;

        Method setupRoster = BirdGame3.class.getDeclaredMethod("setupTrainingRoster");
        setupRoster.setAccessible(true);
        setupRoster.invoke(game);

        Method positionBattlefieldSpawns = BirdGame3.class.getDeclaredMethod("positionBattlefieldSpawns");
        positionBattlefieldSpawns.setAccessible(true);
        positionBattlefieldSpawns.invoke(game);

        Method captureSpawns = BirdGame3.class.getDeclaredMethod("captureTrainingSpawns");
        captureSpawns.setAccessible(true);
        captureSpawns.invoke(game);

        Bird originalPlayer = game.players[0];
        Bird originalDummy = game.players[1];
        double capturedPlayerX = originalPlayer.x;
        double capturedDummyX = originalDummy.x;
        originalPlayer.x = 999.0;
        originalPlayer.health = 17.0;
        originalDummy.x = 888.0;
        originalDummy.health = 6.0;

        Method resetPositions = BirdGame3.class.getDeclaredMethod("resetTrainingPositions");
        resetPositions.setAccessible(true);
        resetPositions.invoke(game);

        assertNotSame(game.players[0], originalPlayer);
        assertNotSame(game.players[1], originalDummy);
        assertEquals(capturedPlayerX, game.players[0].x, 0.0001);
        assertEquals(capturedDummyX, game.players[1].x, 0.0001);
        assertEquals(Bird.STARTING_HEALTH, game.players[0].health, 0.0001);
        assertEquals(Bird.STARTING_HEALTH, game.players[1].health, 0.0001);
    }

    @Test
    void trainingRefillRestoresHealthCooldownsMovementAndUltimate() {
        BirdGame3 game = new BirdGame3();
        Bird bird = new Bird(100.0, BirdGame3.BirdType.PIGEON, 0, game);
        bird.health = 14.0;
        bird.attackCooldown = 9;
        bird.specialCooldown = 45;
        bird.vx = 6.0;
        bird.vy = -5.0;

        bird.refillTrainingResources(true);

        assertEquals(Bird.STARTING_HEALTH, bird.health, 0.0001);
        assertEquals(0, bird.attackCooldown);
        assertEquals(0, bird.specialCooldown);
        assertEquals(0.0, bird.vx, 0.0001);
        assertEquals(0.0, bird.vy, 0.0001);
        assertTrue(bird.isUltimateReady());
    }

    @Test
    void roadrunnerUltimateCatchesIntoRedlineExecutionCutscene() throws Exception {
        BirdGame3 game = new BirdGame3();
        game.activePlayers = 2;

        Bird runner = new Bird(300.0, BirdGame3.BirdType.ROADRUNNER, 0, game);
        Bird target = new Bird(430.0, BirdGame3.BirdType.PIGEON, 1, game);
        runner.y = BirdGame3.GROUND_Y - runner.bodyHeight();
        target.y = runner.y;
        game.players[0] = runner;
        game.players[1] = target;

        setPrivateDouble(runner, "ultimateMeter", 100.0);
        double startingHealth = target.health;

        invokePrivateVoid(runner, "special");

        assertEquals(Bird.ROADRUNNER_REDLINE_DASH_FRAMES,
                getPrivateInt(runner, "roadrunnerRedlineTimer"));
        assertEquals(0, getPrivateInt(runner, "roadrunnerSandstormTimer"),
                "Roadrunner ultimate should no longer start the old sandstorm.");
        assertFalse(runner.isUltimateReady());

        for (int i = 0; i < 4; i++) {
            runner.update(1.0);
        }

        assertTrue(getPrivateBoolean(runner, "roadrunnerRedlineCinematic"),
                "A caught target should trigger the Redline cinematic.");
        assertTrue(runner.isCombatInvulnerable(),
                "Roadrunner should only gain ult invulnerability after the catch connects.");

        for (int i = 0; i < Bird.ROADRUNNER_REDLINE_FINAL_FRAME + 8; i++) {
            runner.update(1.0);
        }

        assertTrue(getPrivateBoolean(runner, "roadrunnerRedlineFinalResolved"),
                "Redline Execution should resolve a single final launch.");
        assertTrue(target.health <= startingHealth - 45.0,
                "The full caught ultimate should deal immense damage.");
        assertTrue(Math.abs(target.vx) > 20.0 && target.vy < -15.0,
                "The final hit should launch the caught target hard.");
    }

    @Test
    void roadrunnerUltimateWhiffSpendsMeterWithoutCutsceneDamage() throws Exception {
        BirdGame3 game = new BirdGame3();
        game.activePlayers = 2;

        Bird runner = new Bird(300.0, BirdGame3.BirdType.ROADRUNNER, 0, game);
        Bird target = new Bird(260.0, BirdGame3.BirdType.PIGEON, 1, game);
        runner.y = BirdGame3.GROUND_Y - runner.bodyHeight();
        target.y = runner.y - 260.0;
        game.players[0] = runner;
        game.players[1] = target;

        setPrivateDouble(runner, "ultimateMeter", 100.0);
        double startingHealth = target.health;

        invokePrivateVoid(runner, "special");
        assertFalse(runner.isUltimateReady());
        assertFalse(runner.isCombatInvulnerable(),
                "The initial lunge should still be punishable before a catch.");

        for (int i = 0; i < Bird.ROADRUNNER_REDLINE_DASH_FRAMES + 3; i++) {
            runner.update(1.0);
        }

        assertFalse(getPrivateBoolean(runner, "roadrunnerRedlineCinematic"));
        assertEquals(startingHealth, target.health, 0.0001);
        assertTrue(getPrivateInt(runner, "roadrunnerRedlineRecoveryTimer") > 0,
                "A whiff should exit into a short recovery instead of a cutscene.");
    }

    @Test
    void pelicanEmptyBilgeTapLoadsOneCargoAndHoldLoadsTwo() throws Exception {
        BirdGame3 game = new BirdGame3();
        game.activePlayers = 1;

        Bird pelican = new Bird(260.0, BirdGame3.BirdType.PELICAN, 0, game);
        pelican.y = BirdGame3.GROUND_Y - 96.0;
        game.players[0] = pelican;

        KeyCode specialKey = game.specialKeyForPlayer(0);
        KeyCode blockKey = game.blockKeyForPlayer(0);

        game.setLocalActionsForKey(blockKey, true);
        game.setLocalActionsForKey(specialKey, true);
        pelican.update(1.0);
        game.setLocalActionsForKey(specialKey, false);
        game.setLocalActionsForKey(blockKey, false);
        pelican.update(1.0);

        assertEquals(1, getPrivateInt(pelican, "pelicanCargoCount"));

        setPrivateInt(pelican, "pelicanCargoCount", 0);
        setPrivateInt(pelican, "pelicanDownReuseTimer", 0);
        pelican.update(1.0);

        game.setLocalActionsForKey(blockKey, true);
        game.setLocalActionsForKey(specialKey, true);
        pelican.update(1.0);
        for (int i = 0; i < 24; i++) {
            pelican.update(1.0);
        }

        assertEquals(2, getPrivateInt(pelican, "pelicanCargoCount"));
    }

    @Test
    void pelicanUltimateStartsMaelstromGulletInsteadOfBoostedSpecial() throws Exception {
        BirdGame3 game = new BirdGame3();
        game.activePlayers = 2;

        Bird pelican = new Bird(260.0, BirdGame3.BirdType.PELICAN, 0, game);
        Bird target = new Bird(380.0, BirdGame3.BirdType.PIGEON, 1, game);
        pelican.y = BirdGame3.GROUND_Y - pelican.bodyHeight();
        target.y = BirdGame3.GROUND_Y - target.bodyHeight();
        game.players[0] = pelican;
        game.players[1] = target;

        setPrivateInt(pelican, "pelicanCargoCount", 2);
        setPrivateDouble(pelican, "ultimateMeter", 100.0);
        invokePrivateVoid(pelican, "special");

        assertEquals(Bird.PELICAN_MAELSTROM_FRAMES, getPrivateInt(pelican, "pelicanMaelstromTimer"));
        assertEquals(2, getPrivateInt(pelican, "pelicanMaelstromCargoSpent"));
        assertEquals(0, getPrivateInt(pelican, "pelicanCargoCount"));
        assertEquals(0, getPrivateInt(pelican, "pelicanFullHoldTimer"),
                "Pelican ultimate should not open Full Hold anymore.");
        assertEquals(0, getPrivateInt(pelican, "pelicanNeutralTimer"),
                "Pelican ultimate should not fall through into boosted Pouch Snare.");
        assertEquals(0, getPrivateInt(pelican, "pelicanSideTimer"));
        assertEquals(0, getPrivateInt(pelican, "pelicanUpTimer"));
        assertFalse(getPrivateBoolean(pelican, "pelicanDownCharging"));
        assertFalse(pelican.isUltimateReady());
        assertEquals(PelicanSpecials.MAELSTROM_GULLET_MOVE, game.lastTelemetryMoveName(0, ""));
    }

    @Test
    void pelicanMaelstromGulletPullsDamagesAndLaunches() throws Exception {
        BirdGame3 game = new BirdGame3();
        game.activePlayers = 2;

        Bird pelican = new Bird(260.0, BirdGame3.BirdType.PELICAN, 0, game);
        Bird target = new Bird(320.0, BirdGame3.BirdType.PIGEON, 1, game);
        pelican.y = BirdGame3.GROUND_Y - pelican.bodyHeight();
        target.y = BirdGame3.GROUND_Y - target.bodyHeight();
        game.players[0] = pelican;
        game.players[1] = target;

        setPrivateInt(pelican, "pelicanCargoCount", 2);
        setPrivateDouble(pelican, "ultimateMeter", 100.0);
        double startingHealth = target.health;
        invokePrivateVoid(pelican, "special");

        for (int i = 0; i < Bird.PELICAN_MAELSTROM_PULL_START_FRAME + 4; i++) {
            pelican.update(1.0);
        }

        assertTrue(target.health < startingHealth,
                "The Maelstrom pull phase should tick damage targets in the pouch zone.");
        assertTrue(target.vx < 0.0, "The vortex should pull the target toward its center.");
        assertTrue(target.stunTime > 0.0, "Targets caught near the center should be briefly pouched.");
        double healthAfterPull = target.health;

        for (int i = 0; i < Bird.PELICAN_MAELSTROM_FINAL_FRAME; i++) {
            pelican.update(1.0);
        }

        assertTrue(getPrivateBoolean(pelican, "pelicanMaelstromFinalResolved"));
        assertTrue(target.health < healthAfterPull,
                "The Maelstrom final geyser should deal a separate heavy hit.");
        assertTrue(target.vy < -10.0, "The Maelstrom final should launch targets upward.");
    }

    @Test
    void pelicanBreakwaterRunSpendsCargoForAHeavyHit() throws Exception {
        BirdGame3 game = new BirdGame3();
        game.activePlayers = 2;

        Bird pelican = new Bird(260.0, BirdGame3.BirdType.PELICAN, 0, game);
        Bird target = new Bird(360.0, BirdGame3.BirdType.PIGEON, 1, game);
        pelican.y = BirdGame3.GROUND_Y - 96.0;
        target.y = BirdGame3.GROUND_Y - 80.0;
        pelican.facingRight = true;
        game.players[0] = pelican;
        game.players[1] = target;

        setPrivateInt(pelican, "pelicanCargoCount", 2);
        double startingHealth = target.health;

        invokePrivateBooleanVoid(pelican, "specialPelicanBreakwaterRun", false);
        pelican.update(1.0);

        assertEquals(0, getPrivateInt(pelican, "pelicanCargoCount"));
        assertTrue(target.health < startingHealth);
        assertTrue(target.vx > 20.0);
    }

    @Test
    void pelicanPouchSnareKnocksTargetsAwayAndLoadsCargo() throws Exception {
        BirdGame3 game = new BirdGame3();
        game.activePlayers = 2;

        Bird pelican = new Bird(260.0, BirdGame3.BirdType.PELICAN, 0, game);
        Bird target = new Bird(360.0, BirdGame3.BirdType.PIGEON, 1, game);
        pelican.y = BirdGame3.GROUND_Y - 96.0;
        target.y = BirdGame3.GROUND_Y - 80.0;
        pelican.facingRight = true;
        game.players[0] = pelican;
        game.players[1] = target;

        invokePrivateBooleanVoid(pelican, "specialPelicanPouchSnare", false);
        pelican.update(1.0);

        assertTrue(target.vx > 0.0, "Neutral special should knock targets away from Pelican.");
        assertTrue(target.vy < 0.0, "Neutral special should pop targets upward.");
        assertEquals(1, getPrivateInt(pelican, "pelicanCargoCount"));
    }

    @Test
    void pelicanThermalSailAutomaticallyTurnsIntoKeelDive() throws Exception {
        BirdGame3 game = new BirdGame3();
        game.activePlayers = 1;

        Bird pelican = new Bird(260.0, BirdGame3.BirdType.PELICAN, 0, game);
        pelican.y = BirdGame3.GROUND_Y - 360.0;
        game.players[0] = pelican;

        invokePrivateBooleanVoid(pelican, "specialPelicanThermalSail", false);
        for (int i = 0; i < 18; i++) {
            pelican.update(1.0);
        }

        assertTrue(getPrivateBoolean(pelican, "pelicanKeelDiveActive"),
                "Up special should force the dive after its ascent without extra input.");
        assertTrue(pelican.vy > 0.0, "Forced dive should drive Pelican downward.");

        for (int i = 0; i < 16; i++) {
            pelican.update(1.0);
        }

        assertEquals(0, getPrivateInt(pelican, "pelicanUpTimer"));
        assertTrue(getPrivateBoolean(pelican, "pelicanKeelDiveActive"),
                "The forced dive should keep running until Pelican lands.");
        assertTrue(pelican.vy > 0.0, "Pelican should still be slamming downward after the ascent timer expires.");
    }

    @Test
    void pelicanKeelDiveBouncesOffDockWaterInsteadOfDrowning() throws Exception {
        BirdGame3 game = new BirdGame3();
        game.activePlayers = 1;
        game.selectedMap = BirdGame3.MapType.DOCK;

        Method setupDockArena = BirdGame3.class.getDeclaredMethod("setupDockArena");
        setupDockArena.setAccessible(true);
        setupDockArena.invoke(game);

        Bird pelican = new Bird(3900.0, BirdGame3.BirdType.PELICAN, 0, game);
        pelican.x = 3900.0;
        pelican.y = game.dockWaterSurfaceY() + 18.0;
        pelican.vy = 17.0;
        pelican.pelicanUpTimer = 1;
        pelican.pelicanUpSpecialUsed = true;
        pelican.pelicanKeelDiveActive = true;
        game.players[0] = pelican;

        double startingHealth = pelican.health;
        pelican.update(1.0);

        assertEquals(startingHealth, pelican.health, 0.0001,
                "A keel dive entering water must not count as drowning.");
        assertFalse(pelican.pelicanKeelDiveActive,
                "Water contact should resolve the forced dive.");
        assertEquals(0, pelican.pelicanUpTimer);
        assertTrue(pelican.vy < 0.0,
                "Water impact should bounce Pelican back toward the surface.");
        assertTrue(pelican.y < game.dockWaterSurfaceY(),
                "The splash bounce should leave Pelican above the drowning region.");
    }

    @Test
    void pelicanKeelDiveDamageScalesWithCargo() throws Exception {
        double emptyCargoDamage = pelicanKeelDiveDamageAtCargo(0);
        double fullCargoDamage = pelicanKeelDiveDamageAtCargo(2);

        assertTrue(fullCargoDamage > emptyCargoDamage,
                "More cargo should make the forced landing hit harder.");
    }

    private static double pelicanKeelDiveDamageAtCargo(int cargo) throws Exception {
        BirdGame3 game = new BirdGame3();
        game.activePlayers = 2;

        Bird pelican = new Bird(260.0, BirdGame3.BirdType.PELICAN, 0, game);
        Bird target = new Bird(280.0, BirdGame3.BirdType.PIGEON, 1, game);
        pelican.y = BirdGame3.GROUND_Y - 96.0;
        target.y = BirdGame3.GROUND_Y - 80.0;
        game.players[0] = pelican;
        game.players[1] = target;

        setPrivateInt(pelican, "pelicanCargoCount", cargo);
        double startingHealth = target.health;
        invokePrivateVoid(pelican, "resolvePelicanKeelDiveLanding");
        return startingHealth - target.health;
    }

    @Test
    void pelicanFullHoldPreservesCargoUntilItExpires() throws Exception {
        BirdGame3 game = new BirdGame3();
        game.activePlayers = 1;

        Bird pelican = new Bird(260.0, BirdGame3.BirdType.PELICAN, 0, game);
        pelican.y = BirdGame3.GROUND_Y - 96.0;
        game.players[0] = pelican;

        invokePrivateVoid(pelican, "beginPelicanFullHold");
        assertEquals(2, getPrivateInt(pelican, "pelicanCargoCount"));

        invokePrivateBooleanVoid(pelican, "specialPelicanBreakwaterRun", false);
        assertEquals(2, getPrivateInt(pelican, "pelicanCargoCount"));

        setPrivateInt(pelican, "pelicanFullHoldTimer", 1);
        pelican.update(1.0);

        assertEquals(0, getPrivateInt(pelican, "pelicanCargoCount"));
    }

    @Test
    void ravenReuseLockoutsStayInvisible() throws Exception {
        BirdGame3 game = new BirdGame3();
        game.activePlayers = 1;

        Bird raven = new Bird(180.0, BirdGame3.BirdType.RAVEN, 0, game);
        raven.y = BirdGame3.GROUND_Y - 80.0;
        game.players[0] = raven;

        KeyCode specialKey = game.specialKeyForPlayer(0);
        game.setLocalActionsForKey(specialKey, true);
        raven.update(1.0);
        game.setLocalActionsForKey(specialKey, false);
        raven.update(1.0);

        assertEquals(0, raven.specialCooldown);
        assertTrue(getPrivateInt(raven, "ravenNeutralReuseTimer") > 0);

        game.setLocalActionsForKey(specialKey, true);
        raven.update(1.0);

        assertEquals(0, raven.specialCooldown,
                "Raven specials should use invisible per-move reuse gates.");
        assertEquals(0, raven.cooldownFlash,
                "Raven reuse lockouts should not display the cooldown warning.");
    }

    @Test
    void ravenChargedBlackQuillFansIntoThreeProjectiles() throws Exception {
        BirdGame3 game = new BirdGame3();
        game.activePlayers = 1;

        Bird raven = new Bird(180.0, BirdGame3.BirdType.RAVEN, 0, game);
        raven.y = BirdGame3.GROUND_Y - 80.0;
        game.players[0] = raven;

        KeyCode specialKey = game.specialKeyForPlayer(0);
        game.setLocalActionsForKey(specialKey, true);
        for (int i = 0; i < 20; i++) {
            raven.update(1.0);
        }
        game.setLocalActionsForKey(specialKey, false);
        raven.update(1.0);

        List<?> quills = (List<?>) getPrivateObject(raven, "ravenQuills");
        assertEquals(3, quills.size(),
                "Holding Black Quill long enough should release the charged fan.");
        assertFalse(getPrivateBoolean(raven, "ravenQuillCharging"));
    }

    @Test
    void ravenShadowWarpConsumesPortentAndEmpowersSlash() throws Exception {
        BirdGame3 game = new BirdGame3();
        game.activePlayers = 2;

        Bird raven = new Bird(180.0, BirdGame3.BirdType.RAVEN, 0, game);
        Bird target = new Bird(360.0, BirdGame3.BirdType.PIGEON, 1, game);
        raven.y = BirdGame3.GROUND_Y - 80.0;
        target.y = BirdGame3.GROUND_Y - 80.0;
        raven.facingRight = true;
        game.players[0] = raven;
        game.players[1] = target;

        invokePrivateBirdBooleanVoid(raven, target, false);
        game.setLocalActionsForKey(game.rightKeyForPlayer(0), true);
        game.setLocalActionsForKey(game.specialKeyForPlayer(0), true);
        raven.update(1.0);

        assertEquals(0, getPrivateInt(target, "ravenPortentTimer"),
                "Shadow Warp should consume the selected Portent.");
        assertTrue(getPrivateBoolean(raven, "ravenSideEmpowered"),
                "Warping through a Portent should empower the slash.");
        assertTrue(raven.x > 240.0,
                "Shadow Warp should relocate Raven near the consumed Portent.");
    }

    @Test
    void ravenMurderLiftRefreshesAfterLanding() throws Exception {
        BirdGame3 game = new BirdGame3();
        game.activePlayers = 1;

        Bird raven = new Bird(220.0, BirdGame3.BirdType.RAVEN, 0, game);
        raven.y = BirdGame3.GROUND_Y - 240.0;
        game.players[0] = raven;

        invokePrivateBooleanVoid(raven, "specialRavenMurderLift", false);

        assertTrue(getPrivateBoolean(raven, "ravenLiftUsed"));
        assertTrue(raven.vy < -20.0, "Murder Lift should launch upward decisively.");

        raven.y = BirdGame3.GROUND_Y - 80.0;
        raven.vy = 0.0;
        raven.update(1.0);

        assertFalse(getPrivateBoolean(raven, "ravenLiftUsed"),
                "Landing should refresh Raven's once-per-airtime lift.");
    }

    @Test
    void ravenNevermorePlacesAndSwapsWithDecoy() throws Exception {
        BirdGame3 game = new BirdGame3();
        game.activePlayers = 1;

        Bird raven = new Bird(220.0, BirdGame3.BirdType.RAVEN, 0, game);
        raven.y = BirdGame3.GROUND_Y - 80.0;
        game.players[0] = raven;

        double originalX = raven.x;
        invokePrivateBooleanVoid(raven, "specialRavenNevermore", false);
        assertNotNull(getPrivateObject(raven, "ravenDecoy"));

        raven.x = 480.0;
        invokePrivateBooleanVoid(raven, "specialRavenNevermore", false);

        assertEquals(originalX, raven.x, 0.0001,
                "Recasting Nevermore should return Raven to the decoy.");
    }

    @Test
    void ravenDownTiltPlantsGroundPortent() throws Exception {
        BirdGame3 game = new BirdGame3();
        game.activePlayers = 1;

        Bird raven = new Bird(180.0, BirdGame3.BirdType.RAVEN, 0, game);
        raven.y = BirdGame3.GROUND_Y - 80.0;
        game.players[0] = raven;

        game.setLocalActionsForKey(game.attackKeyForPlayer(0), true);
        game.setLocalActionsForKey(game.blockKeyForPlayer(0), true);
        raven.update(1.0);
        game.setLocalActionsForKey(game.attackKeyForPlayer(0), false);
        raven.update(1.0);

        List<?> portents = (List<?>) getPrivateObject(raven, "ravenGroundPortents");
        assertEquals(1, portents.size(),
                "Raven's grounded down tilt should seed one Portent.");
    }

    @Test
    void ravenUltimateStagesPortalsRoutesThenVoidRavens() throws Exception {
        BirdGame3 game = new BirdGame3();
        game.activePlayers = 2;

        Bird raven = new Bird(180.0, BirdGame3.BirdType.RAVEN, 0, game);
        Bird target = new Bird(380.0, BirdGame3.BirdType.PIGEON, 1, game);
        raven.y = BirdGame3.GROUND_Y - 80.0;
        target.y = BirdGame3.GROUND_Y - 80.0;
        raven.facingRight = true;
        game.players[0] = raven;
        game.players[1] = target;

        invokePrivateBirdBooleanVoid(raven, target, true);
        double startingHealth = target.health;
        invokePrivateVoid(raven, "specialRavenUnkindness");

        assertTrue(getPrivateInt(raven, "ravenUltimateWindupTimer") > 0,
                "The Unkindness should begin with a portal windup.");
        assertTrue(((List<?>) getPrivateObject(raven, "ravenUltimatePortals")).size() >= 5,
                "The opener should create several visible portals.");
        assertEquals(0, ((List<?>) getPrivateObject(raven, "ravenUltimateRoutes")).size(),
                "The main route strike should wait until after the opener.");
        assertEquals(startingHealth, target.health, 0.0001,
                "The windup should not apply the main route damage immediately.");

        for (int i = 0; i < 32; i++) {
            raven.update(1.0);
        }

        assertFalse(((List<?>) getPrivateObject(raven, "ravenUltimateRoutes")).isEmpty(), "The delayed main strike should create route slashes.");
        assertTrue(target.health < startingHealth,
                "The delayed route strike should damage targets on the route.");

        for (int i = 0; i < 16; i++) {
            raven.update(1.0);
        }

        long ownedVoidRavens = game.crowMinions.stream()
                .filter(crow -> crow.owner == raven && crow.effectiveVariant() == CrowMinion.VARIANT_VOID_RAVEN)
                .count();
        assertTrue(ownedVoidRavens >= 5,
                "The finale should summon a flock of allied void ravens.");
    }

    private static void invokePrivateVoid(Object target, String methodName) throws Exception {
        Method method;
        Object[] args;
        try {
            method = target.getClass().getDeclaredMethod(methodName);
            args = new Object[0];
        } catch (NoSuchMethodException ex) {
            if ("attack".equals(methodName)) {
                method = target.getClass().getDeclaredMethod("performAttack", int.class);
                args = new Object[]{0};
            } else if ("handleVerticalCollision".equals(methodName)) {
                method = target.getClass().getDeclaredMethod(methodName, boolean.class);
                args = new Object[]{false};
            } else {
                throw ex;
            }
        }
        method.setAccessible(true);
        method.invoke(target, args);
    }

    private static Object invokePrivateObjectMethod(Object target, String methodName) throws Exception {
        Method method = target.getClass().getDeclaredMethod(methodName);
        method.setAccessible(true);
        return method.invoke(target);
    }

    private static boolean invokePrivateBoolean(Object target, String methodName) throws Exception {
        Method method = target.getClass().getDeclaredMethod(methodName);
        method.setAccessible(true);
        return (boolean) method.invoke(target);
    }

    private static BirdGame3 guidedAcademyGame(String lessonName) throws Exception {
        BirdGame3 game = new BirdGame3();
        game.trainingModeActive = true;
        Class<?> academyModeClass = Class.forName("com.example.birdgame3.BirdGame3$TrainingAcademyMode");
        Class<?> lessonClass = Class.forName("com.example.birdgame3.BirdGame3$GuidedTutorialLesson");
        setPrivateObject(game, "trainingAcademyMode", enumConstant(academyModeClass, "GUIDED_TUTORIAL"));
        setPrivateObject(game, "guidedTutorialLesson", enumConstant(lessonClass, lessonName));
        return game;
    }

    private static void invokePrivateBooleanVoid(Object target, String methodName, boolean value) throws Exception {
        Method method = target.getClass().getDeclaredMethod(methodName, boolean.class);
        method.setAccessible(true);
        method.invoke(target, value);
    }

    private static void invokePrivateBirdBooleanVoid(Object target, Bird bird, boolean value) throws Exception {
        Method method = target.getClass().getDeclaredMethod("applyRavenPortent", Bird.class, boolean.class);
        method.setAccessible(true);
        method.invoke(target, bird, value);
    }

    private static void invokePrivateIntVoid(Object target) throws Exception {
        Method method = target.getClass().getDeclaredMethod("performAttack", int.class);
        method.setAccessible(true);
        method.invoke(target, 0);
    }

    private static double invokeDoubleMethod(Object target, String methodName) throws Exception {
        Method method = target.getClass().getDeclaredMethod(methodName);
        method.setAccessible(true);
        return ((Number) method.invoke(target)).doubleValue();
    }

    private static double applyPrivateDamage(Bird attacker, Bird target, double rawDamage) throws Exception {
        Method method = Bird.class.getDeclaredMethod("applyDamageTo", Bird.class, double.class);
        method.setAccessible(true);
        return ((Number) method.invoke(attacker, target, rawDamage)).doubleValue();
    }

    private static double attackKnockbackAfterHoldingForFrames(int holdFrames) {
        BirdGame3 game = new BirdGame3();
        game.activePlayers = 2;

        Bird attacker = new Bird(100.0, BirdGame3.BirdType.PIGEON, 0, game);
        Bird target = new Bird(190.0, BirdGame3.BirdType.EAGLE, 1, game);
        attacker.y = BirdGame3.GROUND_Y - 80.0;
        target.y = BirdGame3.GROUND_Y - 80.0;
        attacker.facingRight = true;
        game.players[0] = attacker;
        game.players[1] = target;

        KeyCode rightKey = game.rightKeyForPlayer(0);
        KeyCode attackKey = game.attackKeyForPlayer(0);
        game.setLocalActionsForKey(rightKey, true);
        game.setLocalActionsForKey(attackKey, true);
        for (int i = 0; i < holdFrames; i++) {
            attacker.update(1.0);
        }
        game.setLocalActionsForKey(attackKey, false);
        attacker.update(1.0);
        return target.vx;
    }

    private static double launchVelocityAfterGroundJump(int heldFrames) {
        BirdGame3 game = new BirdGame3();
        game.activePlayers = 1;

        Bird bird = new Bird(190.0, BirdGame3.BirdType.EAGLE, 0, game);
        bird.y = BirdGame3.GROUND_Y - 80.0;
        game.players[0] = bird;

        KeyCode jumpKey = game.jumpKeyForPlayer(0);
        game.setLocalActionsForKey(jumpKey, true);
        for (int i = 0; i < heldFrames; i++) {
            bird.update(1.0);
        }
        game.setLocalActionsForKey(jumpKey, false);
        for (int i = heldFrames; i < 3; i++) {
            bird.update(1.0);
        }
        return Math.abs(bird.vy);
    }

    private static List<ChickMinion> ownedChicks(BirdGame3 game, Bird owner) {
        return game.chickMinions.stream()
                .filter(chick -> chick.owner == owner && chick.life > 0)
                .toList();
    }

    @Test
    void jumpPressedJustBeforeLandingIsBuffered() throws Exception {
        BirdGame3 game = new BirdGame3();
        game.activePlayers = 1;

        Bird bird = new Bird(190.0, BirdGame3.BirdType.EAGLE, 0, game);
        game.players[0] = bird;

        bird.y = BirdGame3.GROUND_Y - 200.0;
        game.setLocalActionsForKey(game.jumpKeyForPlayer(0), true);
        bird.update(1.0);
        game.setLocalActionsForKey(game.jumpKeyForPlayer(0), false);

        assertTrue(getPrivateInt(bird, "jumpBufferFrames") > 0,
                "An airborne jump press should be buffered.");

        bird.y = BirdGame3.GROUND_Y - 80.0;
        bird.vy = 0.0;
        bird.update(1.0);

        assertTrue(getPrivateInt(bird, "jumpSquatTimer") > 0 || bird.vy < 0,
                "A buffered jump should fire on landing.");
        assertEquals(0, getPrivateInt(bird, "jumpBufferFrames"),
                "The jump buffer should be consumed by the buffered jump.");
    }

    @Test
    void coyoteTimeAllowsJumpJustAfterLeavingLedge() throws Exception {
        BirdGame3 game = new BirdGame3();
        game.activePlayers = 1;

        Bird bird = new Bird(190.0, BirdGame3.BirdType.EAGLE, 0, game);
        game.players[0] = bird;

        bird.y = BirdGame3.GROUND_Y - 80.0;
        bird.update(1.0);
        assertTrue(getPrivateInt(bird, "coyoteFrames") > 0,
                "Standing on the ground should refresh coyote time.");

        bird.y = BirdGame3.GROUND_Y - 200.0;
        game.setLocalActionsForKey(game.jumpKeyForPlayer(0), true);
        bird.update(1.0);
        game.setLocalActionsForKey(game.jumpKeyForPlayer(0), false);

        assertTrue(bird.vy < 0, "Jumping within the coyote window should launch a full jump.");
        assertEquals(0, getPrivateInt(bird, "coyoteFrames"),
                "Coyote time should be consumed by the jump.");
    }

    @Test
    void jumpPressedWithSpecialHeldIsNotBuffered() throws Exception {
        BirdGame3 game = new BirdGame3();
        game.activePlayers = 1;

        Bird bird = new Bird(190.0, BirdGame3.BirdType.EAGLE, 0, game);
        game.players[0] = bird;

        bird.y = BirdGame3.GROUND_Y - 200.0;
        game.setLocalActionsForKey(game.jumpKeyForPlayer(0), true);
        game.setLocalActionsForKey(game.specialKeyForPlayer(0), true);
        bird.update(1.0);
        game.setLocalActionsForKey(game.specialKeyForPlayer(0), false);
        game.setLocalActionsForKey(game.jumpKeyForPlayer(0), false);

        assertEquals(0, getPrivateInt(bird, "jumpBufferFrames"),
                "Jump pressed together with special is a special combo and must not buffer a jump.");
    }

    @Test
    void attackPressedDuringCooldownIsBufferedNearExpiry() throws Exception {
        BirdGame3 game = new BirdGame3();
        game.activePlayers = 1;

        Bird bird = new Bird(190.0, BirdGame3.BirdType.EAGLE, 0, game);
        game.players[0] = bird;
        bird.y = BirdGame3.GROUND_Y - 80.0;

        setPrivateInt(bird, "attackCooldown", 5);
        game.setLocalActionsForKey(game.attackKeyForPlayer(0), true);
        bird.update(1.0);
        game.setLocalActionsForKey(game.attackKeyForPlayer(0), false);

        assertTrue(getPrivateInt(bird, "attackBufferFrames") > 0,
                "An attack press during cooldown should be buffered.");

        for (int i = 0; i < 6 && getPrivateInt(bird, "attackCooldown") > 0; i++) {
            bird.update(1.0);
        }
        bird.update(1.0);

        assertTrue(getPrivateInt(bird, "attackCooldown") > 0,
                "The buffered attack should fire once the cooldown expires.");
        assertEquals(0, getPrivateInt(bird, "attackBufferFrames"),
                "The attack buffer should be consumed when the attack fires.");
    }

    private static void setPrivateInt(Object target, String fieldName, int value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.setInt(target, value);
    }

    private static void setPrivateBoolean(Object target) throws Exception {
        Field field = target.getClass().getDeclaredField("smashCombatRulesActive");
        field.setAccessible(true);
        field.setBoolean(target, true);
    }

    private static void setPrivateDouble(Object target, String fieldName, double value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.setDouble(target, value);
    }

    private static void setPrivateObject(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }

    private static int getPrivateInt(Object target, String fieldName) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        return field.getInt(target);
    }

    private static double getPrivateDouble(Object target, String fieldName) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        return field.getDouble(target);
    }

    private static boolean getPrivateBoolean(Object target, String fieldName) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        return field.getBoolean(target);
    }

    @Test
    void gooseHonkTapKeepsAReadableMinimumTell() {
        BirdGame3 game = new BirdGame3();
        game.activePlayers = 2;

        Bird goose = new Bird(100.0, BirdGame3.BirdType.GOOSE, 0, game);
        Bird target = new Bird(220.0, BirdGame3.BirdType.PIGEON, 1, game);
        goose.y = BirdGame3.GROUND_Y - 80.0;
        target.y = BirdGame3.GROUND_Y - 80.0;
        goose.facingRight = true;
        game.players[0] = goose;
        game.players[1] = target;

        GooseSpecials.neutral(goose, false);
        double startingHealth = target.health;
        for (int frame = 1; frame < Bird.GOOSE_HONK_MIN_HOLD_FRAMES; frame++) {
            GooseSpecials.handleState(goose, false);
            assertFalse(goose.gooseHonkReleased,
                    "A released button should not skip the honk's minimum startup tell.");
            assertEquals(startingHealth, target.health, 0.0001);
        }

        GooseSpecials.handleState(goose, false);

        assertTrue(goose.gooseHonkReleased);
        assertTrue(target.health < startingHealth);
        assertTrue(goose.gooseHonkTimer <= Bird.GOOSE_HONK_RECOVERY_FRAMES,
                "Releasing early should transition into the fixed recovery instead of retaining unused charge time.");
    }

    @Test
    void chargedGooseHonkIsStrongerThanTappedHonk() {
        GooseHonkOutcome tapped = playGooseHonk(Bird.GOOSE_HONK_MIN_HOLD_FRAMES);
        GooseHonkOutcome charged = playGooseHonk(Bird.GOOSE_HONK_MAX_HOLD_FRAMES);

        assertTrue(charged.damage > tapped.damage,
                "Holding the honk should earn meaningfully more damage.");
        assertTrue(charged.horizontalLaunch > tapped.horizontalLaunch * 1.7,
                "The strongest honk launch should require a committed charge.");
        assertTrue(charged.stunFrames > tapped.stunFrames + 3.0,
                "The charged honk should retain payoff without giving the tapped version long stun.");
    }

    @Test
    void gooseHonkLaunchAndStunFallOffAcrossTheCone() {
        BirdGame3 game = new BirdGame3();
        game.activePlayers = 3;

        Bird goose = new Bird(100.0, BirdGame3.BirdType.GOOSE, 0, game);
        Bird nearTarget = new Bird(220.0, BirdGame3.BirdType.PIGEON, 1, game);
        Bird farTarget = new Bird(430.0, BirdGame3.BirdType.PIGEON, 2, game);
        goose.y = BirdGame3.GROUND_Y - 80.0;
        nearTarget.y = BirdGame3.GROUND_Y - 80.0;
        farTarget.y = BirdGame3.GROUND_Y - 80.0;
        goose.facingRight = true;
        game.players[0] = goose;
        game.players[1] = nearTarget;
        game.players[2] = farTarget;

        GooseSpecials.neutral(goose, false);
        for (int frame = 0; frame < Bird.GOOSE_HONK_MAX_HOLD_FRAMES; frame++) {
            GooseSpecials.handleState(goose, true);
        }

        assertTrue(nearTarget.health < Bird.STARTING_HEALTH);
        assertTrue(farTarget.health < Bird.STARTING_HEALTH);
        assertTrue(nearTarget.vx > farTarget.vx * 2.0,
                "Honk should preserve its close-range reward without carrying it to the cone edge.");
        assertTrue(farTarget.vx < 3.2,
                "A normal charged honk at maximum range should reset spacing instead of acting as a safe KO launch.");
        assertTrue(nearTarget.stunTime > farTarget.stunTime,
                "Honk stun should decay with distance as well as launch.");
    }

    @Test
    void cpuReactionAndOffenseCadenceImproveGraduallyByLevel() {
        for (int level = 1; level < 9; level++) {
            assertTrue(Bird.aiReactionFramesForLevel(level) > Bird.aiReactionFramesForLevel(level + 1),
                    "Each CPU level should react sooner without becoming frame-perfect.");
            assertTrue(Bird.aiOffenseDecisionIntervalForLevel(level)
                            >= Bird.aiOffenseDecisionIntervalForLevel(level + 1),
                    "Higher CPU levels should reconsider offense at least as quickly.");
        }
        assertTrue(Bird.aiReactionFramesForLevel(9) > 0,
                "Even the strongest CPU must retain a visible reaction delay.");
        assertTrue(Bird.aiOffenseDecisionIntervalForLevel(9) > 1,
                "Even the strongest CPU must commit for more than a single frame.");
    }

    @Test
    void campaignLaunchPercentUsesAuthoredStartingHealth() throws Exception {
        BirdGame3 game = new BirdGame3();
        game.campaignModeActive = true;
        Bird bird = new Bird(100.0, BirdGame3.BirdType.PIGEON, 1, game);
        double[] startingHealth = (double[]) getPrivateObject(game, "campaignStartingHealth");
        startingHealth[1] = 120.0;

        bird.health = 120.0;
        assertEquals(0.0, game.damageScaledLaunchPercent(bird), 0.0001);
        bird.health = 90.0;
        assertEquals(25.0, game.damageScaledLaunchPercent(bird), 0.0001);
        bird.health = 30.0;
        assertEquals(75.0, game.damageScaledLaunchPercent(bird), 0.0001);
    }

    @Test
    void campaignAllyWalksTowardObjectiveAfterEnemiesAreCleared() throws Exception {
        BirdGame3 game = new BirdGame3();
        game.campaignModeActive = true;
        game.campaignTeamMode = true;
        game.activePlayers = 3;
        StoryCampaign.Mission mission = StoryCampaignContent.create().mission("dead_air");
        StoryMissionController controller = new StoryMissionController(
                mission, StoryCampaign.Difficulty.NORMAL, BirdGame3.WORLD_WIDTH);
        Field controllerField = BirdGame3.class.getDeclaredField("campaignMissionController");
        controllerField.setAccessible(true);
        controllerField.set(game, controller);

        Bird player = new Bird(850.0, BirdGame3.BirdType.PIGEON, 0, game);
        Bird charles = new Bird(1000.0, BirdGame3.BirdType.MOCKINGBIRD, 1, game);
        Bird defeatedEnemy = new Bird(4200.0, BirdGame3.BirdType.RAVEN, 2, game);
        game.players[0] = player;
        game.players[1] = charles;
        game.players[2] = defeatedEnemy;
        game.isAI[1] = true;
        game.isAI[2] = true;
        game.campaignTeams[0] = 1;
        game.campaignTeams[1] = 1;
        game.campaignTeams[2] = 2;

        assertTrue(Double.isNaN(game.campaignObjectiveAssistTargetX(charles)),
                "Allies should keep fighting while an enemy remains.");
        defeatedEnemy.health = 0.0;
        assertEquals(1440.0, game.campaignObjectiveAssistTargetX(charles), 0.0001);

        invokePrivateVoid(charles, "aiControl");

        assertTrue(game.isRightPressed(1),
                "Charles should walk toward the first rooftop vent after combat ends.");
        assertFalse(game.isLeftPressed(1));
    }

    @Test
    void campaignControllerUsesBattlefieldPlayableBounds() throws Exception {
        BirdGame3 game = new BirdGame3();
        setPrivateDouble(game, "battlefieldIslandX", 2400.0);
        setPrivateDouble(game, "battlefieldIslandW", 1200.0);
        setPrivateDouble(game, "battlefieldIslandY", BirdGame3.GROUND_Y - 80.0);
        StoryCampaign.Mission mission = StoryCampaignContent.create().mission("dead_air");

        Method setup = BirdGame3.class.getDeclaredMethod(
                "setupCampaignMissionController", StoryCampaign.Mission.class);
        setup.setAccessible(true);
        setup.invoke(game, mission);

        StoryMissionController controller = (StoryMissionController) getPrivateObject(
                game, "campaignMissionController");
        assertEquals(2688.0, controller.objectiveAssistTargetX(), 0.0001,
                "Battlefield campaign targets must be derived from the main island, not world width.");
        assertEquals(BirdGame3.GROUND_Y - 80.0, controller.objectiveFloorY(), 0.0001);
    }

    @Test
    void harborLockSurvivalKeepsRunningAfterBothEnemiesAreDefeated() throws Exception {
        BirdGame3 game = new BirdGame3();
        game.headlessHarnessMode = true;
        game.campaignModeActive = true;
        game.campaignTeamMode = true;
        game.selectedMap = BirdGame3.MapType.DOCK;
        game.matchTimer = 10_000;

        StoryCampaign.Mission mission = StoryCampaignContent.create().mission("harbor_lock");
        setPrivateObject(game, "currentCampaignMission", mission);
        setPrivateObject(game, "campaignSelectedBird", BirdGame3.BirdType.GOOSE);
        StoryCampaignProgress progress =
                (StoryCampaignProgress) getPrivateObject(game, "stillSkyProgress");
        progress.difficulty = StoryCampaign.Difficulty.EASY;
        setPrivateInt(game, "campaignRetryPhaseIndex", 1);
        Method setupRoster = BirdGame3.class.getDeclaredMethod(
                "setupCampaignMissionRoster", StoryCampaign.Mission.class);
        setupRoster.setAccessible(true);
        setupRoster.invoke(game, mission);
        invokePrivateVoid(game, "setupMatchArenaGeometry");

        StoryMissionController controller =
                (StoryMissionController) getPrivateObject(game, "campaignMissionController");
        Bird goose = game.players[0];
        Bird heisenbird = game.players[1];
        Bird razorbill = game.players[2];
        Method launchBomb = BirdGame3.class.getDeclaredMethod(
                "launchDockShipBomb", Bird.class, Bird.class);
        launchBomb.setAccessible(true);
        launchBomb.invoke(game, goose, heisenbird);
        heisenbird.health = 0.0;
        razorbill.health = 0.0;

        for (int tick = 0; tick < 1_600; tick++) {
            assertTrue(game.harnessTick(), "Harbor Lock should still be running at tick " + tick);
        }

        assertEquals(1, controller.phaseIndex());
        assertFalse(controller.complete());
        assertFalse(controller.failed());
        assertTrue(controller.objectiveProgressRatio() > 0.75);

        javafx.scene.canvas.Canvas canvas =
                new javafx.scene.canvas.Canvas(BirdGame3.WIDTH, BirdGame3.HEIGHT);
        Method drawGame = BirdGame3.class.getDeclaredMethod(
                "drawGame", javafx.scene.canvas.GraphicsContext.class);
        drawGame.setAccessible(true);
        drawGame.invoke(game, canvas.getGraphicsContext2D());

        Method buildHud = BirdGame3.class.getDeclaredMethod("buildFightHudLayout");
        buildHud.setAccessible(true);
        Object hudLayout = buildHud.invoke(game);
        @SuppressWarnings("unchecked")
        java.util.Map<String, javafx.scene.image.WritableImage> portraitCache =
                (java.util.Map<String, javafx.scene.image.WritableImage>)
                        getPrivateObject(game, "fightHudPortraitCache");
        portraitCache.put("GOOSE|", new javafx.scene.image.WritableImage(1, 1));
        portraitCache.put("HEISENBIRD|", new javafx.scene.image.WritableImage(1, 1));
        portraitCache.put("RAZORBILL|", new javafx.scene.image.WritableImage(1, 1));
        Method drawHud = java.util.Arrays.stream(BirdGame3.class.getDeclaredMethods())
                .filter(method -> method.getName().equals("drawFightHud"))
                .findFirst()
                .orElseThrow();
        drawHud.setAccessible(true);
        drawHud.invoke(game, canvas.getGraphicsContext2D(), hudLayout);
    }

    @Test
    void carrionAudienceGuardsStayDownWhenReservedBossEnters() throws Exception {
        BirdGame3 game = new BirdGame3();
        game.headlessHarnessMode = true;
        game.campaignModeActive = true;
        game.campaignTeamMode = true;
        game.selectedMap = BirdGame3.MapType.CAVE;

        StoryCampaign.Mission mission = StoryCampaignContent.create().mission("carrion_audience");
        setPrivateObject(game, "currentCampaignMission", mission);
        setPrivateObject(game, "campaignSelectedBird", BirdGame3.BirdType.PIGEON);
        Method setupRoster = BirdGame3.class.getDeclaredMethod(
                "setupCampaignMissionRoster", StoryCampaign.Mission.class);
        setupRoster.setAccessible(true);
        setupRoster.invoke(game, mission);

        boolean[] bossSlots = (boolean[]) getPrivateObject(game, "campaignBossSlots");
        boolean[] reservedBossSlots =
                (boolean[]) getPrivateObject(game, "campaignReservedBossSlots");
        assertEquals(4, game.activePlayers);
        assertTrue(bossSlots[1]);
        assertTrue(reservedBossSlots[1]);
        assertNull(game.players[1], "Vulture must wait offstage during the guard fight.");

        game.players[2].health = 0.0;
        game.players[3].health = 0.0;
        game.checkCampaignMissionCompletion();

        StoryMissionController controller =
                (StoryMissionController) getPrivateObject(game, "campaignMissionController");
        assertEquals(1, controller.phaseIndex());
        assertNotNull(game.players[1]);
        assertEquals(BirdGame3.BirdType.VULTURE, game.players[1].type);
        assertTrue(game.players[1].health > 0.0);
        assertFalse(reservedBossSlots[1]);
        assertEquals(0.0, game.players[2].health, 0.0001,
                "Carrion Guard must not revive when Vulture enters.");
        assertEquals(0.0, game.players[3].health, 0.0001,
                "Cave Sentry must not revive when Vulture enters.");
    }

    @Test
    void campaignGauntletLeavesEveryDefeatedActorDown() throws Exception {
        BirdGame3 game = new BirdGame3();
        game.headlessHarnessMode = true;
        game.campaignModeActive = true;
        game.campaignTeamMode = true;
        game.selectedMap = BirdGame3.MapType.BATTLEFIELD;

        StoryCampaign.Mission mission = StoryCampaignContent.create().mission("crown_archive");
        setPrivateObject(game, "currentCampaignMission", mission);
        setPrivateObject(game, "campaignSelectedBird", BirdGame3.BirdType.PENGUIN);
        setPrivateInt(game, "campaignRetryPhaseIndex", 1);
        Method setupRoster = BirdGame3.class.getDeclaredMethod(
                "setupCampaignMissionRoster", StoryCampaign.Mission.class);
        setupRoster.setAccessible(true);
        setupRoster.invoke(game, mission);

        Bird defeatedFalcon = game.players[1];
        Bird defeatedGuard = game.players[2];
        assertEquals(BirdGame3.BirdType.FALCON, defeatedFalcon.type);
        assertEquals(BirdGame3.BirdType.EAGLE, defeatedGuard.type);
        defeatedFalcon.health = 0.0;
        defeatedGuard.health = 0.0;

        game.checkCampaignMissionCompletion();

        assertSame(defeatedFalcon, game.players[1]);
        assertEquals(0.0, game.players[1].health, 0.0001,
                "The defeated authored boss must remain defeated.");
        assertSame(defeatedGuard, game.players[2]);
        assertEquals(0.0, game.players[2].health, 0.0001,
                "The defeated guard must remain defeated too.");
        StoryMissionController controller =
                (StoryMissionController) getPrivateObject(game, "campaignMissionController");
        assertEquals(2, controller.phaseIndex(),
                "The gauntlet should advance instead of replacing defeated enemies.");
    }

    @Test
    void blackoutKeyLeavesVultureAndEveryOtherEnemyDefeatedPermanently()
            throws Exception {
        BirdGame3 game = new BirdGame3();
        game.headlessHarnessMode = true;
        game.campaignModeActive = true;
        game.campaignTeamMode = true;
        game.selectedMap = BirdGame3.MapType.PRISON;

        StoryCampaign.Mission mission = StoryCampaignContent.create().mission("blackout_key");
        setPrivateObject(game, "currentCampaignMission", mission);
        setPrivateObject(game, "campaignSelectedBird", BirdGame3.BirdType.RAVEN);
        setPrivateInt(game, "campaignRetryPhaseIndex", 1);
        Method setupRoster = BirdGame3.class.getDeclaredMethod(
                "setupCampaignMissionRoster", StoryCampaign.Mission.class);
        setupRoster.setAccessible(true);
        setupRoster.invoke(game, mission);

        Bird defeatedAutomaton = game.players[2];
        Bird defeatedVultureEcho = game.players[3];
        assertEquals(BirdGame3.BirdType.EAGLE, defeatedAutomaton.type);
        assertEquals(BirdGame3.BirdType.VULTURE, defeatedVultureEcho.type);
        defeatedAutomaton.health = 0.0;
        defeatedVultureEcho.health = 0.0;

        game.checkCampaignMissionCompletion();

        StoryMissionController controller =
                (StoryMissionController) getPrivateObject(game, "campaignMissionController");
        assertEquals(2, controller.phaseIndex());
        assertSame(defeatedAutomaton, game.players[2]);
        assertSame(defeatedVultureEcho, game.players[3]);
        assertEquals(0.0, game.players[2].health, 0.0001);
        assertEquals(0.0, game.players[3].health, 0.0001);
    }

    @Test
    void cutTheLockBuildsAStagedCommandBridgeOneOnOne() throws Exception {
        BirdGame3 game = new BirdGame3();
        game.headlessHarnessMode = true;
        game.campaignModeActive = true;
        game.campaignTeamMode = true;
        game.selectedMap = BirdGame3.MapType.SKYCLIFFS;

        StoryCampaign.Mission mission = StoryCampaignContent.create().mission("cut_the_lock");
        setPrivateObject(game, "currentCampaignMission", mission);
        setPrivateObject(game, "campaignSelectedBird", BirdGame3.BirdType.RAZORBILL);
        Method setupRoster = BirdGame3.class.getDeclaredMethod(
                "setupCampaignMissionRoster", StoryCampaign.Mission.class);
        setupRoster.setAccessible(true);
        setupRoster.invoke(game, mission);
        invokePrivateVoid(game, "setupMatchArenaGeometry");
        Method applyArena = BirdGame3.class.getDeclaredMethod(
                "applyCampaignMissionArenaModifiers", StoryCampaign.Mission.class);
        applyArena.setAccessible(true);
        applyArena.invoke(game, mission);

        assertEquals(2, game.activePlayers);
        assertEquals(BirdGame3.BirdType.RAZORBILL, game.players[0].type);
        assertEquals(BirdGame3.BirdType.EAGLE, game.players[1].type);
        assertTrue(game.isAI[1]);
        assertEquals(4, game.platforms.size());
        assertEquals(2, game.windVents.size());
        assertTrue(game.usesIslandBoundsForCurrentArena());
        assertEquals(2_200.0, getPrivateDouble(game, "battlefieldIslandW"), 0.0001);
        assertEquals("music-boss.mp3", invokePrivateObjectMethod(game, "gameplayMusicFile"));

        Bird eagle = game.players[1];
        double startingHealth = eagle.health;
        invokePrivateVoid(game, "applyCampaignMissionRuntimeEffects");
        assertEquals(1, getPrivateInt(game, "campaignCrownDuelStage"));

        eagle.health = startingHealth * 0.74;
        invokePrivateVoid(game, "applyCampaignMissionRuntimeEffects");
        assertEquals(2, getPrivateInt(game, "campaignCrownDuelStage"));
        assertTrue(eagle.overchargeAttackTimer >= 100);
        assertEquals(2, game.windVents.size());

        eagle.health = startingHealth * 0.49;
        invokePrivateVoid(game, "applyCampaignMissionRuntimeEffects");
        assertEquals(3, getPrivateInt(game, "campaignCrownDuelStage"));
        assertEquals(3, game.windVents.size());

        eagle.health = startingHealth * 0.24;
        invokePrivateVoid(game, "applyCampaignMissionRuntimeEffects");
        assertEquals(4, getPrivateInt(game, "campaignCrownDuelStage"));
        assertTrue(eagle.rageTimer >= 300);
        assertTrue(eagle.powerMultiplier >= eagle.basePowerMultiplier * 1.15);
        assertTrue(eagle.speedMultiplier >= eagle.baseSpeedMultiplier * 1.10);
    }

    private static GooseHonkOutcome playGooseHonk(int holdFrames) {
        BirdGame3 game = new BirdGame3();
        game.activePlayers = 2;

        Bird goose = new Bird(100.0, BirdGame3.BirdType.GOOSE, 0, game);
        Bird target = new Bird(220.0, BirdGame3.BirdType.PIGEON, 1, game);
        goose.y = BirdGame3.GROUND_Y - 80.0;
        target.y = BirdGame3.GROUND_Y - 80.0;
        goose.facingRight = true;
        game.players[0] = goose;
        game.players[1] = target;

        GooseSpecials.neutral(goose, false);
        for (int frame = 0; frame < holdFrames; frame++) {
            GooseSpecials.handleState(goose, frame + 1 < holdFrames);
        }
        return new GooseHonkOutcome(
                Bird.STARTING_HEALTH - target.health,
                target.vx,
                target.stunTime
        );
    }

    private record GooseHonkOutcome(double damage, double horizontalLaunch, double stunFrames) {
    }

    private static Object getPrivateObject(Object target, String fieldName) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        return field.get(target);
    }

    private static Enum<?> enumConstant(Class<?> enumClass, String name) {
        Object[] constants = enumClass.getEnumConstants();
        if (constants == null) {
            throw new IllegalArgumentException(enumClass.getName() + " is not an enum");
        }
        for (Object constant : constants) {
            if (constant instanceof Enum<?> enumConstant && enumConstant.name().equals(name)) {
                return enumConstant;
            }
        }
        throw new IllegalArgumentException("Missing enum constant " + enumClass.getName() + "." + name);
    }
}
