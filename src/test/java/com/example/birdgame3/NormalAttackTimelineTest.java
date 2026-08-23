package com.example.birdgame3;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;

class NormalAttackTimelineTest {
    @Test
    void pigeonNeutralRespectsStartupActiveAndRecoveryFrames() throws Exception {
        BirdGame3 game = twoBirdGame(BirdGame3.BirdType.PIGEON, BirdGame3.BirdType.EAGLE,
                320.0, 382.0);
        Bird pigeon = game.players[0];
        Bird target = game.players[1];
        double startingHealth = target.health;

        performAttack(pigeon, "NEUTRAL");

        assertTrue(pigeon.debugNormalAttackTimelineActive());
        assertEquals(0, pigeon.debugNormalAttackFrame());
        assertEquals(6, pigeon.debugNormalAttackTotalFrames());
        assertEquals("STARTUP", pigeon.debugNormalAttackPhaseLabel());
        assertEquals(startingHealth, target.health, 0.0001,
                "Starting a Pigeon attack must not resolve damage on the input tick.");

        advanceTimer(pigeon, 1);
        assertEquals(startingHealth, target.health, 0.0001);
        assertEquals("STARTUP", pigeon.debugNormalAttackPhaseLabel());
        assertFalse(pigeon.debugAttackBoxActive());

        advanceTimer(pigeon, 1);
        assertTrue(target.health < startingHealth);
        assertEquals("ACTIVE", pigeon.debugNormalAttackPhaseLabel());
        assertTrue(pigeon.debugAttackBoxActive());
        assertTrue(pigeon.debugNormalAttackConnected());

        double healthAfterFirstActiveFrame = target.health;
        advanceTimer(pigeon, 1);
        assertEquals(healthAfterFirstActiveFrame, target.health, 0.0001,
                "A lingering single-hit normal must not damage the same target twice.");

        advanceTimer(pigeon, 3);
        assertFalse(pigeon.debugNormalAttackTimelineActive());
        assertEquals(0, pigeon.attackAnimationTimer);
        assertTrue(pigeon.attackCooldown > 0,
                "The existing repeat cadence remains explicit after movement recovery ends.");
    }

    @Test
    void whiffStillCommitsPigeonToTheFullRecoveryTimeline() throws Exception {
        BirdGame3 game = twoBirdGame(BirdGame3.BirdType.PIGEON, BirdGame3.BirdType.EAGLE,
                320.0, 1_400.0);
        Bird pigeon = game.players[0];

        performAttack(pigeon, "SIDE_TILT");
        advanceTimer(pigeon, 7);

        assertEquals("RECOVERY", pigeon.debugNormalAttackPhaseLabel());
        assertEquals(2, pigeon.debugNormalAttackRemainingFrames());
        assertTrue(pigeon.attackAnimationTimer > 0);
        assertFalse(pigeon.debugNormalAttackConnected());

        advanceTimer(pigeon, 2);
        assertFalse(pigeon.debugNormalAttackTimelineActive());
        assertEquals(0, pigeon.attackAnimationTimer);
        assertTrue(pigeon.attackCooldown > 0);
    }

    @Test
    void pigeonSideSmashHasDeterministicSweetAndSourZones() throws Exception {
        AttackOutcome sour = sideSmashOutcome(390.0);
        AttackOutcome sweet = sideSmashOutcome(495.0);

        assertTrue(sweet.damage >= sour.damage,
                "The authored beak tip must not deal less damage than the inner hitbox.");
        assertTrue(Math.abs(sweet.horizontalVelocity) > Math.abs(sour.horizontalVelocity),
                "The authored beak tip should launch harder than the sour spot.");
    }

    @Test
    void hitstunInterruptsStartupBeforeItsHitboxCanAppear() throws Exception {
        BirdGame3 game = twoBirdGame(BirdGame3.BirdType.PIGEON, BirdGame3.BirdType.EAGLE,
                320.0, 382.0);
        Bird pigeon = game.players[0];
        Bird target = game.players[1];
        double startingHealth = target.health;

        performAttack(pigeon, "SIDE_SMASH");
        advanceTimer(pigeon, 3);
        pigeon.applyStun(8.0);
        advanceTimer(pigeon, 10);

        assertFalse(pigeon.debugNormalAttackTimelineActive());
        assertEquals(0, pigeon.attackAnimationTimer);
        assertEquals(startingHealth, target.health, 0.0001);
    }

    @Test
    void aerialsAutoCancelBeforeActiveFramesButLandLagDuringTheStrike() throws Exception {
        BirdGame3 earlyGame = airborneTwoBirdGame();
        Bird early = earlyGame.players[0];
        performAttack(early, "NEUTRAL_AIR");
        advanceTimer(early, 2);
        invoke(early, "resolveAerialLandingRecovery");
        assertEquals(0, intField(early, "landingLagTimer"));
        assertFalse(early.debugNormalAttackTimelineActive());

        BirdGame3 activeGame = airborneTwoBirdGame();
        Bird active = activeGame.players[0];
        performAttack(active, "NEUTRAL_AIR");
        advanceTimer(active, 3);
        assertEquals("ACTIVE", active.debugNormalAttackPhaseLabel());
        invoke(active, "resolveAerialLandingRecovery");
        assertEquals(6, intField(active, "landingLagTimer"));
        assertFalse(active.debugNormalAttackTimelineActive());
    }

    @Test
    void everyPlayableBirdUsesTheDeterministicAuthoredTimelineEngine() throws Exception {
        for (BirdGame3.BirdType type : BirdGame3.BirdType.values()) {
            BirdGame3 game = twoBirdGame(type, BirdGame3.BirdType.PIGEON, 320.0, 1_400.0);
            Bird bird = game.players[0];
            assertTrue((boolean) invoke(bird, "usesAuthoredNormalAttackTimeline"), type.name());

            performAttack(bird, "NEUTRAL");
            assertTrue(bird.debugNormalAttackTimelineActive(), type.name());
            assertEquals("STARTUP", bird.debugNormalAttackPhaseLabel(), type.name());
        }
    }

    @Test
    void everyRaptorNormalHasCompleteAuthoredFrameData() throws Exception {
        String[] variants = {
                "NEUTRAL", "SIDE_TILT", "UP_TILT", "DOWN_TILT",
                "SIDE_SMASH", "UP_SMASH", "DOWN_SMASH",
                "NEUTRAL_AIR", "FORWARD_AIR", "BACK_AIR", "UP_AIR", "DOWN_AIR",
                "DASH_ATTACK", "LEDGE_ATTACK", "GETUP_ATTACK"
        };
        int[][] eagleFrames = {
                {2, 3, 3}, {3, 3, 5}, {3, 4, 4}, {2, 3, 7},
                {7, 3, 7}, {7, 4, 6}, {7, 5, 6},
                {2, 7, 3}, {4, 4, 7}, {3, 4, 8}, {3, 5, 6}, {5, 4, 7},
                {3, 4, 8}, {3, 4, 7}, {4, 4, 8}
        };
        int[][] falconFrames = {
                {1, 3, 3}, {2, 3, 4}, {2, 4, 3}, {1, 3, 4},
                {4, 3, 6}, {4, 4, 5}, {4, 5, 5},
                {1, 6, 3}, {2, 4, 5}, {2, 4, 5}, {2, 5, 3}, {3, 4, 5},
                {1, 4, 6}, {2, 4, 4}, {3, 4, 4}
        };

        assertAuthoredMoveList(BirdGame3.BirdType.EAGLE, variants, eagleFrames);
        assertAuthoredMoveList(BirdGame3.BirdType.FALCON, variants, falconFrames);
    }

    @Test
    void falconSideSmashStrikesEarlierThanEaglesWithoutSkippingStartup() throws Exception {
        BirdGame3 falconGame = twoBirdGame(BirdGame3.BirdType.FALCON, BirdGame3.BirdType.PIGEON,
                320.0, 470.0);
        Bird falcon = falconGame.players[0];
        Bird falconTarget = falconGame.players[1];
        double falconStartingHealth = falconTarget.health;
        performAttack(falcon, "SIDE_SMASH");
        advanceTimer(falcon, 4);
        assertEquals(falconStartingHealth, falconTarget.health, 0.0001);
        advanceTimer(falcon, 1);
        assertTrue(falconTarget.health < falconStartingHealth);

        BirdGame3 eagleGame = twoBirdGame(BirdGame3.BirdType.EAGLE, BirdGame3.BirdType.PIGEON,
                320.0, 470.0);
        Bird eagle = eagleGame.players[0];
        Bird eagleTarget = eagleGame.players[1];
        double eagleStartingHealth = eagleTarget.health;
        performAttack(eagle, "SIDE_SMASH");
        advanceTimer(eagle, 5);
        assertEquals(eagleStartingHealth, eagleTarget.health, 0.0001,
                "Eagle's heavier smash must remain telegraphed after Falcon is already active.");
        advanceTimer(eagle, 3);
        assertTrue(eagleTarget.health < eagleStartingHealth);
    }

    @Test
    void aerialSpecialistsHaveDistinctCompleteFrameData() throws Exception {
        String[] variants = {
                "NEUTRAL", "SIDE_TILT", "UP_TILT", "DOWN_TILT",
                "SIDE_SMASH", "UP_SMASH", "DOWN_SMASH",
                "NEUTRAL_AIR", "FORWARD_AIR", "BACK_AIR", "UP_AIR", "DOWN_AIR",
                "DASH_ATTACK", "LEDGE_ATTACK", "GETUP_ATTACK"
        };
        int[][] phoenixFrames = {
                {1, 5, 2}, {1, 5, 4}, {1, 5, 4}, {1, 5, 4},
                {4, 5, 7}, {4, 6, 6}, {4, 7, 6},
                {1, 8, 3}, {2, 6, 5}, {2, 6, 5}, {1, 8, 3}, {3, 6, 5},
                {1, 6, 6}, {2, 5, 6}, {2, 6, 7}
        };
        int[][] hummingbirdFrames = {
                {1, 2, 3}, {1, 3, 4}, {1, 3, 4}, {1, 2, 4},
                {4, 3, 6}, {4, 4, 5}, {4, 5, 5},
                {1, 5, 3}, {2, 3, 5}, {2, 3, 5}, {1, 4, 4}, {3, 3, 5},
                {1, 3, 6}, {2, 3, 5}, {2, 4, 5}
        };

        assertAuthoredMoveList(BirdGame3.BirdType.PHOENIX, variants, phoenixFrames);
        assertAuthoredMoveList(BirdGame3.BirdType.HUMMINGBIRD, variants, hummingbirdFrames);
    }

    @Test
    void phoenixNeutralAirLingersWithoutRepeatedlyHittingOneTarget() throws Exception {
        BirdGame3 game = twoBirdGame(BirdGame3.BirdType.PHOENIX, BirdGame3.BirdType.PIGEON,
                320.0, 382.0);
        Bird phoenix = game.players[0];
        Bird target = game.players[1];
        double startingHealth = target.health;

        performAttack(phoenix, "NEUTRAL_AIR");
        advanceTimer(phoenix, 3);
        assertEquals("ACTIVE", phoenix.debugNormalAttackPhaseLabel());
        assertTrue(target.health < startingHealth);
        double afterFirstFlameContact = target.health;
        advanceTimer(phoenix, 6);
        assertEquals(afterFirstFlameContact, target.health, 0.0001,
                "Phoenix's lingering flame arc remains a single hit per target.");
        assertEquals("ACTIVE", phoenix.debugNormalAttackPhaseLabel());
    }

    @Test
    void groundedHeavyweightsHaveDistinctCompleteFrameData() throws Exception {
        String[] variants = {
                "NEUTRAL", "SIDE_TILT", "UP_TILT", "DOWN_TILT",
                "SIDE_SMASH", "UP_SMASH", "DOWN_SMASH",
                "NEUTRAL_AIR", "FORWARD_AIR", "BACK_AIR", "UP_AIR", "DOWN_AIR",
                "DASH_ATTACK", "LEDGE_ATTACK", "GETUP_ATTACK"
        };
        int[][] turkeyFrames = {
                {1, 5, 2}, {1, 6, 4}, {1, 7, 3}, {1, 6, 4},
                {3, 6, 7}, {3, 7, 6}, {3, 8, 6},
                {1, 9, 3}, {2, 6, 5}, {2, 6, 6}, {2, 7, 4}, {3, 6, 6},
                {1, 7, 6}, {2, 6, 5}, {2, 7, 6}
        };
        int[][] roosterFrames = {
                {1, 4, 1}, {1, 4, 3}, {1, 5, 2}, {1, 4, 3},
                {2, 4, 8}, {2, 5, 8}, {2, 6, 8},
                {1, 7, 2}, {1, 5, 5}, {1, 5, 5}, {1, 6, 3}, {2, 5, 6},
                {1, 5, 5}, {1, 4, 6}, {1, 5, 6}
        };

        assertAuthoredMoveList(BirdGame3.BirdType.TURKEY, variants, turkeyFrames);
        assertAuthoredMoveList(BirdGame3.BirdType.ROOSTER, variants, roosterFrames);
    }

    @Test
    void turkeyCommitsLongerThanRoosterOnComparableHeavyAttacks() throws Exception {
        BirdGame3 turkeyGame = twoBirdGame(BirdGame3.BirdType.TURKEY, BirdGame3.BirdType.PIGEON,
                320.0, 700.0);
        BirdGame3 roosterGame = twoBirdGame(BirdGame3.BirdType.ROOSTER, BirdGame3.BirdType.PIGEON,
                320.0, 700.0);

        performAttack(turkeyGame.players[0], "SIDE_SMASH");
        performAttack(roosterGame.players[0], "SIDE_SMASH");

        assertTrue(turkeyGame.players[0].debugNormalAttackTotalFrames()
                        > roosterGame.players[0].debugNormalAttackTotalFrames(),
                "Turkey's weight should cost more total commitment than Rooster's spur strike.");
        advanceTimer(roosterGame.players[0], 3);
        advanceTimer(turkeyGame.players[0], 3);
        assertEquals("ACTIVE", roosterGame.players[0].debugNormalAttackPhaseLabel());
        assertEquals("STARTUP", turkeyGame.players[0].debugNormalAttackPhaseLabel());
    }

    @Test
    void mobilityExtremesHaveDistinctCompleteFrameData() throws Exception {
        String[] variants = {
                "NEUTRAL", "SIDE_TILT", "UP_TILT", "DOWN_TILT",
                "SIDE_SMASH", "UP_SMASH", "DOWN_SMASH",
                "NEUTRAL_AIR", "FORWARD_AIR", "BACK_AIR", "UP_AIR", "DOWN_AIR",
                "DASH_ATTACK", "LEDGE_ATTACK", "GETUP_ATTACK"
        };
        int[][] roadrunnerFrames = {
                {1, 3, 2}, {1, 4, 2}, {1, 4, 3}, {1, 4, 2},
                {3, 4, 6}, {3, 5, 5}, {3, 6, 5},
                {1, 6, 2}, {1, 4, 4}, {1, 4, 4}, {1, 5, 3}, {2, 4, 5},
                {1, 5, 3}, {1, 4, 4}, {1, 5, 4}
        };
        int[][] penguinFrames = {
                {1, 6, 2}, {1, 7, 3}, {1, 7, 3}, {1, 7, 3},
                {3, 7, 7}, {3, 8, 7}, {3, 9, 7},
                {1, 9, 3}, {2, 7, 5}, {2, 7, 5}, {1, 8, 4}, {3, 7, 6},
                {1, 9, 4}, {1, 7, 5}, {2, 8, 5}
        };

        assertAuthoredMoveList(BirdGame3.BirdType.ROADRUNNER, variants, roadrunnerFrames);
        assertAuthoredMoveList(BirdGame3.BirdType.PENGUIN, variants, penguinFrames);
    }

    @Test
    void roadrunnerDashAttackStartsBeforePenguinsLongSlideEnds() throws Exception {
        BirdGame3 roadrunnerGame = twoBirdGame(BirdGame3.BirdType.ROADRUNNER, BirdGame3.BirdType.PIGEON,
                320.0, 700.0);
        BirdGame3 penguinGame = twoBirdGame(BirdGame3.BirdType.PENGUIN, BirdGame3.BirdType.PIGEON,
                320.0, 700.0);

        performAttack(roadrunnerGame.players[0], "DASH_ATTACK");
        performAttack(penguinGame.players[0], "DASH_ATTACK");
        advanceTimer(roadrunnerGame.players[0], 2);
        advanceTimer(penguinGame.players[0], 2);

        assertEquals("ACTIVE", roadrunnerGame.players[0].debugNormalAttackPhaseLabel());
        assertEquals("ACTIVE", penguinGame.players[0].debugNormalAttackPhaseLabel());
        assertTrue(penguinGame.players[0].debugNormalAttackTotalFrames()
                        > roadrunnerGame.players[0].debugNormalAttackTotalFrames(),
                "Penguin's slide should linger longer than Roadrunner's burst.");
    }

    @Test
    void technicalSpecialistsHaveDistinctCompleteFrameData() throws Exception {
        String[] variants = {
                "NEUTRAL", "SIDE_TILT", "UP_TILT", "DOWN_TILT",
                "SIDE_SMASH", "UP_SMASH", "DOWN_SMASH",
                "NEUTRAL_AIR", "FORWARD_AIR", "BACK_AIR", "UP_AIR", "DOWN_AIR",
                "DASH_ATTACK", "LEDGE_ATTACK", "GETUP_ATTACK"
        };
        int[][] shoebillFrames = {
                {1, 6, 2}, {1, 7, 4}, {1, 7, 3}, {1, 6, 4},
                {3, 6, 8}, {3, 7, 7}, {3, 8, 7},
                {1, 8, 3}, {2, 7, 5}, {2, 6, 6}, {1, 8, 4}, {3, 7, 6},
                {1, 7, 5}, {1, 7, 5}, {2, 7, 6}
        };
        int[][] charlesFrames = {
                {1, 4, 2}, {2, 5, 4}, {2, 5, 4}, {1, 5, 4},
                {4, 5, 7}, {4, 6, 6}, {4, 7, 6},
                {1, 7, 3}, {2, 5, 5}, {2, 5, 5}, {1, 6, 4}, {3, 5, 6},
                {1, 6, 5}, {2, 5, 5}, {2, 6, 5}
        };

        assertAuthoredMoveList(BirdGame3.BirdType.SHOEBILL, variants, shoebillFrames);
        assertAuthoredMoveList(BirdGame3.BirdType.MOCKINGBIRD, variants, charlesFrames);
    }

    @Test
    void shoebillSideSmashCommitsLongerThanCharlesAdaptableSwing() throws Exception {
        BirdGame3 shoebillGame = twoBirdGame(BirdGame3.BirdType.SHOEBILL, BirdGame3.BirdType.PIGEON,
                320.0, 700.0);
        BirdGame3 charlesGame = twoBirdGame(BirdGame3.BirdType.MOCKINGBIRD, BirdGame3.BirdType.PIGEON,
                320.0, 700.0);

        performAttack(shoebillGame.players[0], "SIDE_SMASH");
        performAttack(charlesGame.players[0], "SIDE_SMASH");
        assertTrue(shoebillGame.players[0].debugNormalAttackTotalFrames()
                        > charlesGame.players[0].debugNormalAttackTotalFrames(),
                "Shoebill's extended beak follow-through should outlast Charles's adaptable swing.");
    }

    @Test
    void precisionTrickstersHaveDistinctCompleteFrameData() throws Exception {
        String[] variants = {
                "NEUTRAL", "SIDE_TILT", "UP_TILT", "DOWN_TILT",
                "SIDE_SMASH", "UP_SMASH", "DOWN_SMASH",
                "NEUTRAL_AIR", "FORWARD_AIR", "BACK_AIR", "UP_AIR", "DOWN_AIR",
                "DASH_ATTACK", "LEDGE_ATTACK", "GETUP_ATTACK"
        };
        int[][] razorbillFrames = {
                {1, 3, 3}, {2, 3, 4}, {2, 4, 4}, {1, 3, 4},
                {5, 3, 7}, {5, 4, 6}, {5, 5, 6},
                {1, 6, 3}, {2, 3, 5}, {2, 3, 5}, {2, 4, 4}, {3, 3, 6},
                {1, 3, 6}, {2, 3, 5}, {2, 4, 6}
        };
        int[][] grinchHawkFrames = {
                {1, 5, 3}, {2, 5, 5}, {2, 6, 4}, {1, 5, 4},
                {5, 5, 8}, {5, 6, 7}, {5, 7, 7},
                {1, 8, 4}, {2, 5, 6}, {2, 5, 6}, {1, 7, 4}, {3, 5, 7},
                {1, 6, 6}, {2, 5, 6}, {2, 6, 6}
        };

        assertAuthoredMoveList(BirdGame3.BirdType.RAZORBILL, variants, razorbillFrames);
        assertAuthoredMoveList(BirdGame3.BirdType.GRINCHHAWK, variants, grinchHawkFrames);
    }

    @Test
    void razorbillCutsBrieflyWhileGrinchHawkMaintainsTheFeint() throws Exception {
        BirdGame3 razorbillGame = twoBirdGame(BirdGame3.BirdType.RAZORBILL, BirdGame3.BirdType.PIGEON,
                320.0, 700.0);
        BirdGame3 grinchGame = twoBirdGame(BirdGame3.BirdType.GRINCHHAWK, BirdGame3.BirdType.PIGEON,
                320.0, 700.0);

        performAttack(razorbillGame.players[0], "SIDE_TILT");
        performAttack(grinchGame.players[0], "SIDE_TILT");
        advanceTimer(razorbillGame.players[0], 6);
        advanceTimer(grinchGame.players[0], 6);

        assertEquals("RECOVERY", razorbillGame.players[0].debugNormalAttackPhaseLabel());
        assertEquals("ACTIVE", grinchGame.players[0].debugNormalAttackPhaseLabel());
        assertTrue(grinchGame.players[0].debugNormalAttackTotalFrames()
                        > razorbillGame.players[0].debugNormalAttackTotalFrames(),
                "Grinch-Hawk's broad feint should last longer than Razorbill's exact cut.");
    }

    @Test
    void lingeringZonersHaveDistinctCompleteFrameData() throws Exception {
        String[] variants = {
                "NEUTRAL", "SIDE_TILT", "UP_TILT", "DOWN_TILT",
                "SIDE_SMASH", "UP_SMASH", "DOWN_SMASH",
                "NEUTRAL_AIR", "FORWARD_AIR", "BACK_AIR", "UP_AIR", "DOWN_AIR",
                "DASH_ATTACK", "LEDGE_ATTACK", "GETUP_ATTACK"
        };
        int[][] vultureFrames = {
                {1, 6, 2}, {2, 7, 4}, {2, 7, 4}, {1, 7, 4},
                {4, 7, 8}, {4, 8, 7}, {4, 9, 7},
                {1, 10, 3}, {2, 7, 6}, {2, 7, 6}, {1, 9, 4}, {3, 7, 7},
                {1, 8, 6}, {2, 7, 6}, {2, 8, 6}
        };
        int[][] opiumFrames = {
                {1, 7, 2}, {1, 7, 4}, {1, 8, 3}, {1, 7, 4},
                {3, 7, 8}, {3, 8, 7}, {3, 9, 7},
                {1, 10, 3}, {2, 7, 6}, {2, 7, 6}, {1, 9, 4}, {3, 7, 7},
                {1, 8, 6}, {1, 7, 6}, {2, 8, 6}
        };

        assertAuthoredMoveList(BirdGame3.BirdType.VULTURE, variants, vultureFrames);
        assertAuthoredMoveList(BirdGame3.BirdType.OPIUMBIRD, variants, opiumFrames);
    }

    @Test
    void opiumBirdStartsItsHazeBeforeVulturesHeavySweep() throws Exception {
        BirdGame3 vultureGame = twoBirdGame(BirdGame3.BirdType.VULTURE, BirdGame3.BirdType.PIGEON,
                320.0, 700.0);
        BirdGame3 opiumGame = twoBirdGame(BirdGame3.BirdType.OPIUMBIRD, BirdGame3.BirdType.PIGEON,
                320.0, 700.0);

        performAttack(vultureGame.players[0], "SIDE_SMASH");
        performAttack(opiumGame.players[0], "SIDE_SMASH");
        advanceTimer(vultureGame.players[0], 4);
        advanceTimer(opiumGame.players[0], 4);

        assertEquals("STARTUP", vultureGame.players[0].debugNormalAttackPhaseLabel());
        assertEquals("ACTIVE", opiumGame.players[0].debugNormalAttackPhaseLabel());
    }

    @Test
    void calculatedRivalsHaveDistinctCompleteFrameData() throws Exception {
        String[] variants = {
                "NEUTRAL", "SIDE_TILT", "UP_TILT", "DOWN_TILT",
                "SIDE_SMASH", "UP_SMASH", "DOWN_SMASH",
                "NEUTRAL_AIR", "FORWARD_AIR", "BACK_AIR", "UP_AIR", "DOWN_AIR",
                "DASH_ATTACK", "LEDGE_ATTACK", "GETUP_ATTACK"
        };
        int[][] heisenbirdFrames = {
                {1, 5, 3}, {2, 6, 5}, {2, 6, 5}, {1, 6, 5},
                {5, 6, 8}, {5, 7, 7}, {5, 8, 7},
                {1, 8, 4}, {2, 6, 6}, {2, 6, 6}, {1, 7, 5}, {3, 6, 7},
                {1, 7, 6}, {2, 6, 6}, {2, 7, 6}
        };
        int[][] ravenFrames = {
                {1, 4, 3}, {2, 4, 5}, {2, 5, 4}, {1, 4, 5},
                {5, 4, 8}, {5, 5, 7}, {5, 6, 7},
                {1, 7, 3}, {2, 4, 6}, {2, 4, 6}, {1, 6, 4}, {3, 4, 7},
                {1, 5, 6}, {2, 4, 6}, {2, 5, 6}
        };

        assertAuthoredMoveList(BirdGame3.BirdType.HEISENBIRD, variants, heisenbirdFrames);
        assertAuthoredMoveList(BirdGame3.BirdType.RAVEN, variants, ravenFrames);
    }

    @Test
    void ravenFinishesItsOmenBeforeHeisenbirdsApparatusStops() throws Exception {
        BirdGame3 heisenGame = twoBirdGame(BirdGame3.BirdType.HEISENBIRD, BirdGame3.BirdType.PIGEON,
                320.0, 700.0);
        BirdGame3 ravenGame = twoBirdGame(BirdGame3.BirdType.RAVEN, BirdGame3.BirdType.PIGEON,
                320.0, 700.0);

        performAttack(heisenGame.players[0], "SIDE_TILT");
        performAttack(ravenGame.players[0], "SIDE_TILT");
        assertTrue(heisenGame.players[0].debugNormalAttackTotalFrames()
                        > ravenGame.players[0].debugNormalAttackTotalFrames(),
                "Heisenbird's apparatus swing should outlast Raven's exact omen cut.");
    }

    @Test
    void groundedSpecialistsHaveDistinctCompleteFrameData() throws Exception {
        String[] variants = {
                "NEUTRAL", "SIDE_TILT", "UP_TILT", "DOWN_TILT",
                "SIDE_SMASH", "UP_SMASH", "DOWN_SMASH",
                "NEUTRAL_AIR", "FORWARD_AIR", "BACK_AIR", "UP_AIR", "DOWN_AIR",
                "DASH_ATTACK", "LEDGE_ATTACK", "GETUP_ATTACK"
        };
        int[][] gooseFrames = {
                {2, 6, 4}, {3, 7, 6}, {3, 7, 5}, {2, 7, 6},
                {6, 7, 9}, {6, 8, 8}, {6, 9, 8},
                {2, 9, 5}, {3, 7, 7}, {3, 6, 8}, {2, 8, 6}, {4, 7, 8},
                {3, 8, 8}, {3, 7, 7}, {3, 8, 7}
        };
        int[][] kiwiFrames = {
                {1, 4, 4}, {2, 5, 6}, {2, 6, 5}, {1, 5, 5},
                {5, 5, 9}, {5, 6, 8}, {5, 7, 8},
                {1, 7, 4}, {2, 5, 6}, {2, 4, 7}, {1, 6, 5}, {3, 5, 8},
                {2, 6, 6}, {2, 5, 6}, {2, 6, 6}
        };

        assertAuthoredMoveList(BirdGame3.BirdType.GOOSE, variants, gooseFrames);
        assertAuthoredMoveList(BirdGame3.BirdType.KIWI, variants, kiwiFrames);
    }

    @Test
    void gooseClaimsAirspaceLongerWhileKiwiRecoversFromAQuickProbe() throws Exception {
        BirdGame3 gooseGame = twoBirdGame(BirdGame3.BirdType.GOOSE, BirdGame3.BirdType.PIGEON,
                320.0, 700.0);
        BirdGame3 kiwiGame = twoBirdGame(BirdGame3.BirdType.KIWI, BirdGame3.BirdType.PIGEON,
                320.0, 700.0);

        performAttack(gooseGame.players[0], "NEUTRAL_AIR");
        performAttack(kiwiGame.players[0], "NEUTRAL");
        assertTrue(gooseGame.players[0].debugNormalAttackActiveFrames()
                        > kiwiGame.players[0].debugNormalAttackActiveFrames());
        assertTrue(gooseGame.players[0].debugNormalAttackTotalFrames()
                        > kiwiGame.players[0].debugNormalAttackTotalFrames());
    }

    @Test
    void smallAerialistsHaveDistinctCompleteFrameData() throws Exception {
        String[] variants = {
                "NEUTRAL", "SIDE_TILT", "UP_TILT", "DOWN_TILT",
                "SIDE_SMASH", "UP_SMASH", "DOWN_SMASH",
                "NEUTRAL_AIR", "FORWARD_AIR", "BACK_AIR", "UP_AIR", "DOWN_AIR",
                "DASH_ATTACK", "LEDGE_ATTACK", "GETUP_ATTACK"
        };
        int[][] titmouseFrames = {
                {1, 3, 2}, {1, 4, 3}, {1, 5, 2}, {1, 3, 4},
                {3, 4, 8}, {3, 5, 7}, {3, 6, 7},
                {1, 7, 2}, {1, 4, 5}, {1, 4, 5}, {1, 6, 3}, {2, 4, 7},
                {1, 5, 5}, {1, 4, 5}, {1, 5, 5}
        };
        int[][] batFrames = {
                {1, 4, 2}, {2, 4, 3}, {2, 5, 3}, {1, 4, 3},
                {4, 5, 7}, {4, 6, 6}, {4, 7, 6},
                {1, 8, 2}, {2, 6, 4}, {1, 5, 5}, {1, 7, 3}, {3, 6, 6},
                {1, 6, 5}, {2, 5, 5}, {2, 6, 5}
        };

        assertAuthoredMoveList(BirdGame3.BirdType.TITMOUSE, variants, titmouseFrames);
        assertAuthoredMoveList(BirdGame3.BirdType.BAT, variants, batFrames);
    }

    @Test
    void batOwnsLongerAerialWindowsWhileTitmouseScramblesFasterOnTheGround() throws Exception {
        BirdGame3 batGame = twoBirdGame(BirdGame3.BirdType.BAT, BirdGame3.BirdType.PIGEON,
                320.0, 700.0);
        BirdGame3 titmouseGame = twoBirdGame(BirdGame3.BirdType.TITMOUSE, BirdGame3.BirdType.PIGEON,
                320.0, 700.0);

        performAttack(batGame.players[0], "FORWARD_AIR");
        performAttack(titmouseGame.players[0], "FORWARD_AIR");
        assertTrue(batGame.players[0].debugNormalAttackActiveFrames()
                        > titmouseGame.players[0].debugNormalAttackActiveFrames());

        BirdGame3 groundedBat = twoBirdGame(BirdGame3.BirdType.BAT, BirdGame3.BirdType.PIGEON,
                320.0, 700.0);
        BirdGame3 groundedTitmouse = twoBirdGame(BirdGame3.BirdType.TITMOUSE, BirdGame3.BirdType.PIGEON,
                320.0, 700.0);
        performAttack(groundedBat.players[0], "SIDE_TILT");
        performAttack(groundedTitmouse.players[0], "SIDE_TILT");
        assertTrue(groundedTitmouse.players[0].debugNormalAttackTotalFrames()
                        < groundedBat.players[0].debugNormalAttackTotalFrames());
    }

    @Test
    void pelicanHasCompleteHeavyweightFrameData() throws Exception {
        String[] variants = {
                "NEUTRAL", "SIDE_TILT", "UP_TILT", "DOWN_TILT",
                "SIDE_SMASH", "UP_SMASH", "DOWN_SMASH",
                "NEUTRAL_AIR", "FORWARD_AIR", "BACK_AIR", "UP_AIR", "DOWN_AIR",
                "DASH_ATTACK", "LEDGE_ATTACK", "GETUP_ATTACK"
        };
        int[][] pelicanFrames = {
                {2, 5, 4}, {3, 6, 6}, {3, 7, 5}, {2, 6, 6},
                {7, 6, 10}, {7, 7, 9}, {7, 8, 9},
                {2, 9, 5}, {4, 6, 8}, {3, 6, 8}, {3, 8, 6}, {5, 6, 9},
                {3, 8, 8}, {3, 6, 8}, {4, 7, 8}
        };

        assertAuthoredMoveList(BirdGame3.BirdType.PELICAN, variants, pelicanFrames);
    }

    @Test
    void pelicansLongReachCarriesHeavyweightCommitment() throws Exception {
        BirdGame3 pelicanGame = twoBirdGame(BirdGame3.BirdType.PELICAN, BirdGame3.BirdType.PIGEON,
                320.0, 700.0);
        BirdGame3 batGame = twoBirdGame(BirdGame3.BirdType.BAT, BirdGame3.BirdType.PIGEON,
                320.0, 700.0);

        performAttack(pelicanGame.players[0], "SIDE_SMASH");
        performAttack(batGame.players[0], "SIDE_SMASH");
        assertTrue(pelicanGame.players[0].debugNormalAttackStartupFrames()
                        > batGame.players[0].debugNormalAttackStartupFrames());
        assertTrue(pelicanGame.players[0].debugNormalAttackRecoveryFrames()
                        > batGame.players[0].debugNormalAttackRecoveryFrames());
    }

    @Test
    void activeTimelineAndPerTargetHitHistoryRoundTripThroughLanWireState() throws Exception {
        BirdGame3 game = twoBirdGame(BirdGame3.BirdType.PIGEON, BirdGame3.BirdType.EAGLE,
                320.0, 382.0);
        Bird source = game.players[0];
        performAttack(source, "NEUTRAL");
        advanceTimer(source, 3);

        LanBirdState snapshot = source.toLanState();
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        snapshot.write(new DataOutputStream(bytes));
        LanBirdState decoded = LanBirdState.read(new DataInputStream(
                new ByteArrayInputStream(bytes.toByteArray())));

        Bird restored = groundedBird(game, BirdGame3.BirdType.PIGEON, 0, 320.0);
        restored.applyLanState(decoded);

        assertTrue(restored.debugNormalAttackTimelineActive());
        assertEquals(3, restored.debugNormalAttackFrame());
        assertEquals("ACTIVE", restored.debugNormalAttackPhaseLabel());
        assertTrue(restored.debugNormalAttackConnected());
        assertEquals(snapshot.normalAttackLastHitFrame[1], decoded.normalAttackLastHitFrame[1]);
    }

    @Test
    void hitstopFreezesTheAuthoredFrameAndShieldAdvantageUsesRemainingRecovery() throws Exception {
        BirdGame3 game = twoBirdGame(BirdGame3.BirdType.PIGEON, BirdGame3.BirdType.EAGLE,
                320.0, 382.0);
        game.trainingModeActive = true;
        Bird pigeon = game.players[0];
        Bird defender = game.players[1];
        game.setLocalActionsForKey(game.blockKeyForPlayer(1), true);
        for (int frame = 0; frame < 5; frame++) {
            defender.update(1.0);
        }

        performAttack(pigeon, "NEUTRAL");
        advanceTimer(pigeon, 2);

        assertEquals(Bird.STARTING_HEALTH, defender.health, 0.0001);
        assertTrue(booleanField(game, "trainingShieldAdvantageAvailable"));
        assertEquals(defender.shieldStunFrames - pigeon.debugNormalAttackRemainingFrames(),
                intField(game, "trainingLastShieldAdvantageFrames"));
        assertTrue(game.hitstopFrames > 0);

        int activeFrame = pigeon.debugNormalAttackFrame();
        game.harnessTick();
        assertEquals(activeFrame, pigeon.debugNormalAttackFrame(),
                "Hitstop must consume a fixed game tick without advancing authored attack frames.");
    }

    @Test
    void lateAttackPressBuffersUntilRepeatCooldownEnds() throws Exception {
        BirdGame3 game = twoBirdGame(BirdGame3.BirdType.PIGEON, BirdGame3.BirdType.EAGLE,
                320.0, 1_400.0);
        Bird pigeon = game.players[0];
        performAttack(pigeon, "SIDE_TILT");
        advanceTimer(pigeon, 10);
        assertFalse(pigeon.debugNormalAttackTimelineActive());
        while (pigeon.debugNormalAttackCooldownFrames() > 8) {
            pigeon.update(1.0);
        }
        assertEquals(8, pigeon.debugNormalAttackCooldownFrames());

        game.setLocalActionsForKey(game.attackKeyForPlayer(0), true);
        pigeon.update(1.0);
        assertFalse(pigeon.debugNormalAttackTimelineActive(),
                "A buffered press must not bypass the current move's repeat cooldown.");
        game.setLocalActionsForKey(game.attackKeyForPlayer(0), false);

        for (int frame = 0; frame < 10 && !pigeon.debugNormalAttackTimelineActive(); frame++) {
            pigeon.update(1.0);
        }
        assertTrue(pigeon.debugNormalAttackTimelineActive(),
                "The buffered normal should begin on the first actionable fixed tick.");
        assertEquals(0, pigeon.debugNormalAttackFrame());
        assertEquals("Pigeon Rooftop Peck", pigeon.debugNormalAttackMoveName());
    }

    private static AttackOutcome sideSmashOutcome(double targetX) throws Exception {
        BirdGame3 game = twoBirdGame(BirdGame3.BirdType.PIGEON, BirdGame3.BirdType.EAGLE,
                320.0, targetX);
        Bird pigeon = game.players[0];
        Bird target = game.players[1];
        double startingHealth = target.health;
        performAttack(pigeon, "SIDE_SMASH");
        advanceTimer(pigeon, 9);
        return new AttackOutcome(startingHealth - target.health, target.vx);
    }

    private static void assertAuthoredMoveList(BirdGame3.BirdType type, String[] variants,
                                               int[][] expectedFrames) throws Exception {
        assertEquals(variants.length, expectedFrames.length);
        for (int index = 0; index < variants.length; index++) {
            BirdGame3 game = twoBirdGame(type, BirdGame3.BirdType.PIGEON, 320.0, 1_400.0);
            Bird bird = game.players[0];
            performAttack(bird, variants[index]);

            assertTrue(bird.debugNormalAttackTimelineActive(), type + " " + variants[index]);
            assertEquals(expectedFrames[index][0], bird.debugNormalAttackStartupFrames(),
                    type + " " + variants[index] + " startup");
            assertEquals(expectedFrames[index][1], bird.debugNormalAttackActiveFrames(),
                    type + " " + variants[index] + " active");
            assertEquals(expectedFrames[index][2], bird.debugNormalAttackRecoveryFrames(),
                    type + " " + variants[index] + " recovery");
            assertEquals(expectedFrames[index][0] + expectedFrames[index][1] + expectedFrames[index][2],
                    bird.debugNormalAttackTotalFrames(), type + " " + variants[index] + " total");
        }
    }

    private static BirdGame3 airborneTwoBirdGame() {
        BirdGame3 game = twoBirdGame(BirdGame3.BirdType.PIGEON, BirdGame3.BirdType.EAGLE,
                320.0, 382.0);
        game.players[0].y -= 240.0;
        game.players[1].y -= 240.0;
        return game;
    }

    private static BirdGame3 twoBirdGame(BirdGame3.BirdType attackerType, BirdGame3.BirdType targetType,
                                         double attackerX, double targetX) {
        BirdGame3 game = new BirdGame3();
        game.activePlayers = 2;
        game.players[0] = groundedBird(game, attackerType, 0, attackerX);
        game.players[1] = groundedBird(game, targetType, 1, targetX);
        return game;
    }

    private static Bird groundedBird(BirdGame3 game, BirdGame3.BirdType type, int index, double x) {
        if (game.platforms.isEmpty()) {
            game.platforms.add(new Platform(0.0, BirdGame3.GROUND_Y,
                    BirdGame3.WORLD_WIDTH, 80.0));
        }
        Bird bird = new Bird(x, type, index, game);
        bird.y = BirdGame3.GROUND_Y - bird.bodyHeight();
        return bird;
    }

    private static void performAttack(Bird bird, String variantName) throws Exception {
        Object variant = enumConstant("com.example.birdgame3.Bird$NormalAttackVariant", variantName);
        invoke(bird, "performAttack", 0, variant);
    }

    private static void advanceTimer(Bird bird, int frames) throws Exception {
        for (int frame = 0; frame < frames; frame++) {
            invoke(bird, "updateTimers", 1.0);
        }
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

    private static int intField(Object target, String name) throws Exception {
        var field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        return field.getInt(target);
    }

    private static boolean booleanField(Object target, String name) throws Exception {
        var field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        return field.getBoolean(target);
    }

    private record AttackOutcome(double damage, double horizontalVelocity) {
    }
}
