package com.example.birdgame3;

import javafx.scene.canvas.Canvas;
import org.junit.jupiter.api.Test;

import java.util.UUID;
import java.util.prefs.Preferences;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BirdRosterIntegrationTest {

    private static BirdGame3 freshGame() {
        return new BirdGame3(Preferences.userRoot().node(
                "/birdfight3-tests/roster-integration/" + UUID.randomUUID()));
    }

    @Test
    void everySideAttackStartsInFrontAndOverlapsItsLiveHurtbox() {
        BirdGame3 game = freshGame();
        for (BirdGame3.BirdType type : BirdGame3.BirdType.values()) {
            Bird right = integrationBird(game, type, true);
            Bird left = integrationBird(game, type, false);
            Bird.VisualCombatEnvelope rightBox = right.visualAuditSideAttackEnvelope();
            Bird.VisualCombatEnvelope leftBox = left.visualAuditSideAttackEnvelope();

            assertEnvelopeIsFiniteAndPositive(type, rightBox);
            assertEnvelopeIsFiniteAndPositive(type, leftBox);
            assertTrue(rightBox.attackCenterX() > rightBox.bodyCenterX(),
                    type + " side-attack origin must be in front while facing right");
            assertTrue(leftBox.attackCenterX() < leftBox.bodyCenterX(),
                    type + " side-attack origin must be in front while facing left");
            assertTrue(rectanglesOverlap(rightBox), type + " right side attack must connect to its hurtbox");
            assertTrue(rectanglesOverlap(leftBox), type + " left side attack must connect to its hurtbox");

            assertEquals(rightBox.bodyHalfWidth(), leftBox.bodyHalfWidth(), 0.000001, type + " hurtbox width");
            assertEquals(rightBox.bodyHalfHeight(), leftBox.bodyHalfHeight(), 0.000001, type + " hurtbox height");
            assertEquals(rightBox.attackHalfWidth(), leftBox.attackHalfWidth(), 0.000001, type + " attack width");
            assertEquals(rightBox.attackHalfHeight(), leftBox.attackHalfHeight(), 0.000001, type + " attack height");
            assertEquals(rightBox.attackCenterX() - rightBox.bodyCenterX(),
                    -(leftBox.attackCenterX() - leftBox.bodyCenterX()), 0.000001,
                    type + " side-attack origin must mirror exactly");
            assertEquals(rightBox.attackCenterY() - rightBox.bodyCenterY(),
                    leftBox.attackCenterY() - leftBox.bodyCenterY(), 0.000001,
                    type + " vertical attack origin must not change with facing");
        }
    }

    @Test
    void everyBaseBirdBillRemainsInsideItsSideAttackCoverage() {
        BirdGame3 game = freshGame();
        for (BirdGame3.BirdType type : BirdGame3.BirdType.values()) {
            for (boolean facingRight : new boolean[]{true, false}) {
                Bird bird = integrationBird(game, type, facingRight);
                bird.prepareVisualAuditPose(Bird.VisualAuditPose.IDLE);
                bird.facingRight = facingRight;
                bird.drawVisualAuditBody(new Canvas(320, 320).getGraphicsContext2D());
                Bird.VisualBeakAxis bill = bird.visualFeatureGeometry().beak();
                Bird.VisualCombatEnvelope attack = bird.visualAuditSideAttackEnvelope();
                String label = type + (facingRight ? " facing right" : " facing left");

                assertNotNull(bill, label + " must publish its authored bill anchor");
                assertTrue(Math.abs(bill.tipX() - attack.attackCenterX())
                                <= attack.attackHalfWidth() + 1.0,
                        label + " bill tip must stay inside horizontal side-attack coverage");
                assertTrue(Math.abs(bill.tipY() - attack.attackCenterY())
                                <= attack.attackHalfHeight() + 1.0,
                        label + " bill tip must stay inside vertical side-attack coverage");
            }
        }
    }

    private static Bird integrationBird(BirdGame3 game, BirdGame3.BirdType type, boolean facingRight) {
        Bird bird = new Bird(0.0, type, 0, game);
        bird.sizeMultiplier = 0.60;
        bird.prepareVisualAuditPose(Bird.VisualAuditPose.ATTACK);
        bird.facingRight = facingRight;
        return bird;
    }

    private static void assertEnvelopeIsFiniteAndPositive(
            BirdGame3.BirdType type, Bird.VisualCombatEnvelope box) {
        assertTrue(Double.isFinite(box.bodyCenterX()) && Double.isFinite(box.bodyCenterY()),
                type + " hurtbox center must be finite");
        assertTrue(Double.isFinite(box.attackCenterX()) && Double.isFinite(box.attackCenterY()),
                type + " attack center must be finite");
        assertTrue(Double.isFinite(box.bodyHalfWidth()) && box.bodyHalfWidth() > 0.0,
                type + " hurtbox width must be positive and finite");
        assertTrue(Double.isFinite(box.bodyHalfHeight()) && box.bodyHalfHeight() > 0.0,
                type + " hurtbox height must be positive and finite");
        assertTrue(Double.isFinite(box.attackHalfWidth()) && box.attackHalfWidth() > 0.0,
                type + " attack width must be positive and finite");
        assertTrue(Double.isFinite(box.attackHalfHeight()) && box.attackHalfHeight() > 0.0,
                type + " attack height must be positive and finite");
    }

    private static boolean rectanglesOverlap(Bird.VisualCombatEnvelope box) {
        return Math.abs(box.attackCenterX() - box.bodyCenterX())
                <= box.attackHalfWidth() + box.bodyHalfWidth()
                && Math.abs(box.attackCenterY() - box.bodyCenterY())
                <= box.attackHalfHeight() + box.bodyHalfHeight();
    }
}
