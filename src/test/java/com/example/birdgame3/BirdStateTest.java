package com.example.birdgame3;

import javafx.scene.input.KeyCode;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

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
    @SuppressWarnings({"rawtypes", "unchecked"})
    void knockbackTuningBoostsNonSmashNormalsAndTonesDownSmashes() throws Exception {
        BirdGame3 game = new BirdGame3();
        Bird bird = new Bird(100.0, BirdGame3.BirdType.PIGEON, 0, game);

        Class<?> variantClass = Class.forName("com.example.birdgame3.Bird$NormalAttackVariant");
        Method multiplier = Bird.class.getDeclaredMethod("attackKnockbackBalanceMultiplier", variantClass);
        multiplier.setAccessible(true);
        Class<? extends Enum> enumClass = (Class<? extends Enum>) variantClass.asSubclass(Enum.class);

        Enum<?> sideTilt = Enum.valueOf((Class) enumClass, "SIDE_TILT");
        Enum<?> neutralAir = Enum.valueOf((Class) enumClass, "NEUTRAL_AIR");
        Enum<?> sideSmash = Enum.valueOf((Class) enumClass, "SIDE_SMASH");
        Enum<?> upSmash = Enum.valueOf((Class) enumClass, "UP_SMASH");

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
    void pigeonGroundDownSpecialUsesBlockInputWithoutRaisingShieldAndHealsOnCompletion() {
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
                "Grounded scavenge should take much longer before the heal resolves.");
        for (int i = 0; i < 50; i++) {
            pigeon.update(1.0);
        }

        assertTrue(pigeon.health > 48.0, "Completing the grounded scavenge should heal Pigeon.");
    }

    @Test
    void pigeonShieldedSpecialConvertsIntoGroundDownSpecial() throws Exception {
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

        assertTrue(pigeon.health > 48.0, "Shield-canceled down special should still heal on completion.");
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
    void phoenixNeutralChargeBuildsWhileHeldAndCarriesUltimateToRelease() throws Exception {
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

        assertTrue(getPrivateBoolean(phoenix, "phoenixCharging"));
        assertTrue(getPrivateBoolean(phoenix, "phoenixChargeUltimate"),
                "Starting neutral special with full meter should bank the ultimate for the release.");

        for (int i = 0; i < 45; i++) {
            phoenix.update(1.0);
        }

        assertTrue(getPrivateInt(phoenix, "phoenixChargeTimer") >= 40,
                "Holding neutral special should actually build charge instead of getting reset every frame.");

        game.setLocalActionsForKey(game.specialKeyForPlayer(0), false);
        phoenix.update(1.0);

        assertFalse(getPrivateBoolean(phoenix, "phoenixCharging"));
        assertEquals(0, phoenix.specialCooldown,
                "Phoenix neutral special should not leave a visible cooldown after the charge detonates.");
        assertTrue(getPrivateInt(phoenix, "phoenixAfterburnTimer") > 0,
                "Releasing the charge should ignite Phoenix's lingering afterburn aura.");
        assertTrue(target.health < startingHealth,
                "A charged release should damage nearby enemies when it detonates.");
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

        invokePrivateBooleanVoid(normalPhoenix, "specialPhoenixUp", false);
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

        invokePrivateBooleanVoid(ultimatePhoenix, "specialPhoenixUp", true);
        assertEquals(0, ultimatePhoenix.specialCooldown);
        ultimatePhoenix.update(1.0);
        double ultimateDamage = Bird.STARTING_HEALTH - ultimateTarget.health;

        assertTrue(ultimateDamage > normalDamage,
                "Helix Ascent should hit harder than the base Firespin.");
    }

    @Test
    void phoenixUpSpecialStaysSpentWhenInterruptedInMidair() throws Exception {
        BirdGame3 game = new BirdGame3();
        game.activePlayers = 1;

        Bird phoenix = new Bird(190.0, BirdGame3.BirdType.PHOENIX, 0, game);
        phoenix.y = BirdGame3.GROUND_Y - 80.0;
        game.players[0] = phoenix;

        invokePrivateBooleanVoid(phoenix, "specialPhoenixUp", false);
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
    void phoenixSideSpecialHasNoCooldownAndLocksMovementDuringCast() throws Exception {
        BirdGame3 game = new BirdGame3();
        game.activePlayers = 1;

        Bird phoenix = new Bird(220.0, BirdGame3.BirdType.PHOENIX, 0, game);
        phoenix.y = BirdGame3.GROUND_Y - 80.0;
        phoenix.facingRight = true;
        game.players[0] = phoenix;

        invokePrivateBooleanVoid(phoenix, "specialPhoenixSide", false);

        assertEquals(0, phoenix.specialCooldown);
        assertTrue(getPrivateInt(phoenix, "phoenixCastLockTimer") > 0);

        double startX = phoenix.x;
        phoenix.vx = 4.5;
        phoenix.update(1.0);

        assertEquals(startX, phoenix.x, 0.0001,
                "Phoenix should stay planted while Snap Fire is in its cast lock.");
        assertTrue(getPrivateDouble(phoenix, "phoenixFireballX") > startX + 40.0,
                "Snap Fire should still launch a fast projectile even while Phoenix is locked in place.");
    }

    @Test
    void phoenixGroundDownSpecialSendsFlamesOutwardOnBothSides() throws Exception {
        BirdGame3 game = new BirdGame3();
        game.activePlayers = 3;

        Bird phoenix = new Bird(260.0, BirdGame3.BirdType.PHOENIX, 0, game);
        Bird leftTarget = new Bird(120.0, BirdGame3.BirdType.PIGEON, 1, game);
        Bird rightTarget = new Bird(400.0, BirdGame3.BirdType.EAGLE, 2, game);
        phoenix.y = BirdGame3.GROUND_Y - 80.0;
        leftTarget.y = BirdGame3.GROUND_Y - 80.0;
        rightTarget.y = BirdGame3.GROUND_Y - 80.0;
        game.players[0] = phoenix;
        game.players[1] = leftTarget;
        game.players[2] = rightTarget;

        invokePrivateBooleanVoid(phoenix, "specialPhoenixDown", false);

        assertEquals(0, phoenix.specialCooldown);
        assertFalse(getPrivateBoolean(phoenix, "phoenixLavaAirborne"));

        double leftStart = leftTarget.health;
        double rightStart = rightTarget.health;
        for (int i = 0; i < 20; i++) {
            phoenix.update(1.0);
        }

        assertTrue(leftTarget.health < leftStart,
                "Ground Faultfire should reach and hit targets to Phoenix's left.");
        assertTrue(rightTarget.health < rightStart,
                "Ground Faultfire should also reach and hit targets to Phoenix's right.");
        assertTrue(leftTarget.vx < 0.0 && rightTarget.vx > 0.0,
                "The outward flame fronts should throw each target away from Phoenix.");
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

        invokePrivateBooleanVoid(phoenix, "specialPhoenixDown", false);

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

        Class.forName("com.example.birdgame3.BirdGame3$TrainingAcademyMode");
        Class.forName("com.example.birdgame3.BirdGame3$GuidedTutorialLesson");
        Class.forName("com.example.birdgame3.BirdGame3$BirdTrialDefinition");

        Method setupRoster = BirdGame3.class.getDeclaredMethod("setupTrainingRoster");
        setupRoster.setAccessible(true);
        setupRoster.invoke(game);

        assertEquals(BirdGame3.BirdType.PENGUIN, game.players[0].type);
        assertEquals(BirdGame3.BirdType.PIGEON, game.players[1].type);

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

    private static Object invokePrivateObjectMethod(Object target, String methodName) throws Exception {
        Method method = target.getClass().getDeclaredMethod(methodName);
        method.setAccessible(true);
        return method.invoke(target);
    }

    private static void invokePrivateBooleanVoid(Object target, String methodName, boolean value) throws Exception {
        Method method = target.getClass().getDeclaredMethod(methodName, boolean.class);
        method.setAccessible(true);
        method.invoke(target, value);
    }

    private static double invokeDoubleMethod(Object target, String methodName) throws Exception {
        Method method = target.getClass().getDeclaredMethod(methodName);
        method.setAccessible(true);
        return ((Number) method.invoke(target)).doubleValue();
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
