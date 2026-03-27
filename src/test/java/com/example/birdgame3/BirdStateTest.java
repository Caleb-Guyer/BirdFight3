package com.example.birdgame3;

import javafx.scene.input.KeyCode;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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
        nullRock.applyShrinkEffect(0.6, 360);

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
    void localHealthBarUsesNullRockName() {
        BirdGame3 game = new BirdGame3();
        Bird nullRock = new Bird(600.0, BirdGame3.BirdType.VULTURE, 0, game);
        nullRock.isNullRockSkin = true;

        assertEquals("P1: The Null Rock", game.healthBarLabel(nullRock));
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

    private static int getPrivateInt(Object target, String fieldName) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        return field.getInt(target);
    }
}
