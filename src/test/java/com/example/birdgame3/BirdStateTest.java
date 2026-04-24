package com.example.birdgame3;

import javafx.scene.input.KeyCode;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BirdStateTest {
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
        CrowMinion survivor = game.crowMinions.get(0);
        assertEquals(2, survivor.life);
        assertTrue(Math.abs(survivor.vx) > 0.1);
        assertTrue(survivor.vy < 0.0);
        assertTrue(survivor.hitFlashTimer > 0);
        assertTrue(game.particles.size() >= 6);
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

        Method handleVerticalCollision = Bird.class.getDeclaredMethod("handleVerticalCollision");
        handleVerticalCollision.setAccessible(true);
        handleVerticalCollision.invoke(sandBird);
        handleVerticalCollision.invoke(waterBird);

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
    void academyTrainingRosterUsesLessonAndTrialBirds() throws Exception {
        BirdGame3 game = new BirdGame3();

        @SuppressWarnings({"unchecked", "rawtypes"})
        Class<? extends Enum> academyModeClass = (Class<? extends Enum>) Class.forName("com.example.birdgame3.BirdGame3$TrainingAcademyMode");
        @SuppressWarnings({"unchecked", "rawtypes"})
        Class<? extends Enum> lessonClass = (Class<? extends Enum>) Class.forName("com.example.birdgame3.BirdGame3$GuidedTutorialLesson");
        @SuppressWarnings({"unchecked", "rawtypes"})
        Class<? extends Enum> trialClass = (Class<? extends Enum>) Class.forName("com.example.birdgame3.BirdGame3$BirdTrialDefinition");

        setPrivateObject(game, "trainingAcademyMode", Enum.valueOf((Class) academyModeClass, "GUIDED_TUTORIAL"));
        setPrivateObject(game, "guidedTutorialLesson", Enum.valueOf((Class) lessonClass, "RECOVERY"));

        Method setupRoster = BirdGame3.class.getDeclaredMethod("setupTrainingRoster");
        setupRoster.setAccessible(true);
        setupRoster.invoke(game);

        assertEquals(BirdGame3.BirdType.PENGUIN, game.players[0].type);
        assertEquals(BirdGame3.BirdType.PIGEON, game.players[1].type);

        setPrivateObject(game, "trainingAcademyMode", Enum.valueOf((Class) academyModeClass, "BIRD_TRIAL"));
        setPrivateObject(game, "activeBirdTrial", Enum.valueOf((Class) trialClass, "EAGLE"));
        setupRoster.invoke(game);

        assertEquals(BirdGame3.BirdType.EAGLE, game.players[0].type);
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

        assertFalse(game.players[0] == originalPlayer);
        assertFalse(game.players[1] == originalDummy);
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
    void roadrunnerUltimateSustainsSandstormFlightAndGusts() throws Exception {
        BirdGame3 game = new BirdGame3();
        game.activePlayers = 2;

        Bird runner = new Bird(300.0, BirdGame3.BirdType.ROADRUNNER, 0, game);
        Bird target = new Bird(420.0, BirdGame3.BirdType.PIGEON, 1, game);
        game.players[0] = runner;
        game.players[1] = target;

        setPrivateDouble(runner, "ultimateMeter", 100.0);
        double startingHealth = target.health;

        invokePrivateVoid(runner, "special");

        assertTrue(getPrivateInt(runner, "roadrunnerSandstormTimer") >= 500);
        assertTrue(target.health < startingHealth);

        double healthAfterBurst = target.health;
        runner.y = BirdGame3.GROUND_Y - 320.0;
        runner.vy = 0.0;
        game.setLocalActionsForKey(game.jumpKeyForPlayer(0), true);
        runner.update(1.0);
        game.setLocalActionsForKey(game.jumpKeyForPlayer(0), false);

        assertTrue(runner.vy < 0.0, "Sandstorm ultimate should let Roadrunner gain lift while jump is held.");

        target.vx = 0.0;
        for (int i = 0; i < 30; i++) {
            runner.update(1.0);
        }

        assertTrue(target.health < healthAfterBurst || target.vx > 0.1,
                "Lingering sandstorm gusts should keep hurting or blowing nearby enemies.");
    }

    private static void invokePrivateVoid(Object target, String methodName) throws Exception {
        Method method = target.getClass().getDeclaredMethod(methodName);
        method.setAccessible(true);
        method.invoke(target);
    }

    private static void setPrivateInt(Object target, String fieldName, int value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.setInt(target, value);
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

    private static Object getPrivateObject(Object target, String fieldName) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        return field.get(target);
    }
}
