package com.example.birdgame3;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.prefs.Preferences;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BirdVisualAuditTest {

    private static final Set<BirdGame3.BirdType> FEATURE_AUDIT_BIRDS = Set.of(
            BirdGame3.BirdType.PIGEON,
            BirdGame3.BirdType.EAGLE,
            BirdGame3.BirdType.ROADRUNNER,
            BirdGame3.BirdType.MOCKINGBIRD,
            BirdGame3.BirdType.VULTURE
    );

    private static BirdGame3 freshGame() {
        return new BirdGame3(Preferences.userRoot().node("/birdfight3-tests/visual-audit/" + UUID.randomUUID()));
    }

    @Test
    void auditCatalogIncludesEveryBirdAndEveryFeatherpediaSkinExactlyOnce() {
        List<BirdGame3.VisualAuditSkin> entries = freshGame().visualAuditSkins();
        Set<String> identities = new HashSet<>();

        for (BirdGame3.VisualAuditSkin entry : entries) {
            String identity = entry.bird().name() + ":" + (entry.key() == null ? "BASE" : entry.key());
            assertTrue(identities.add(identity), "Duplicate visual-audit entry: " + identity);
            assertTrue(entry.name() != null && !entry.name().isBlank(), identity + " needs a review label");
        }

        for (BirdGame3.BirdType type : BirdGame3.BirdType.values()) {
            assertTrue(identities.contains(type.name() + ":BASE"), type + " base art is missing from the audit");
        }
        assertTrue(entries.size() >= BirdGame3.BirdType.values().length * 2,
                "The audit should cover base birds plus the complete Featherpedia skin catalog");
    }

    @Test
    void auditCoversTheRequestedPresentationAndCombatPoses() {
        assertEquals(List.of("IDLE", "RUN", "FLAP", "ATTACK", "HIT", "KO"),
                java.util.Arrays.stream(Bird.VisualAuditPose.values()).map(Enum::name).toList());
    }

    @Test
    void featuredBirdEyesStayInsideTheHeadAndClearOfTheBeakAcrossSkinsAndPoses() {
        BirdGame3 game = freshGame();
        List<BirdGame3.VisualAuditSkin> entries = game.visualAuditSkins().stream()
                .filter(entry -> FEATURE_AUDIT_BIRDS.contains(entry.bird()))
                .filter(entry -> !"STOCK_PHOTO_EAGLE".equals(entry.key()))
                .filter(entry -> !"NULL_ROCK_VULTURE".equals(entry.key()))
                .filter(entry -> BirdSpriteLibrary.sheetFor(entry.bird(), entry.key()) == null)
                .toList();

        for (BirdGame3.VisualAuditSkin entry : entries) {
            BirdGame3.VisualAuditSkin base = new BirdGame3.VisualAuditSkin(
                    entry.bird(), null, entry.bird().name + " base");
            for (Bird.VisualAuditPose pose : Bird.VisualAuditPose.values()) {
                Bird.VisualFeatureGeometry baseRight = game.inspectVisualAuditCombatFeatures(base, pose, true);
                Bird.VisualFeatureGeometry baseLeft = game.inspectVisualAuditCombatFeatures(base, pose, false);
                Bird.VisualFeatureGeometry skinRight = game.inspectVisualAuditCombatFeatures(entry, pose, true);
                Bird.VisualFeatureGeometry skinLeft = game.inspectVisualAuditCombatFeatures(entry, pose, false);
                String label = entry.name() + " / " + pose;

                assertFeatureGeometryIsSafe(baseRight, label + " base facing right");
                assertFeatureGeometryIsSafe(baseLeft, label + " base facing left");
                assertFeatureGeometryIsSafe(skinRight, label + " facing right");
                assertFeatureGeometryIsSafe(skinLeft, label + " facing left");
                assertMirrored(baseRight, baseLeft, label + " base");
                assertMirrored(skinRight, skinLeft, label);
                assertSameFacialGeometry(baseRight, skinRight, label + " facing right");
                assertSameFacialGeometry(baseLeft, skinLeft, label + " facing left");
            }
        }
    }

    @Test
    void pigeonKeepsBothLegsAcrossSkinsPosesAndDirections() {
        BirdGame3 game = freshGame();
        List<BirdGame3.VisualAuditSkin> entries = game.visualAuditSkins().stream()
                .filter(entry -> entry.bird() == BirdGame3.BirdType.PIGEON)
                .filter(entry -> BirdSpriteLibrary.sheetFor(entry.bird(), entry.key()) == null)
                .toList();

        for (BirdGame3.VisualAuditSkin entry : entries) {
            for (Bird.VisualAuditPose pose : Bird.VisualAuditPose.values()) {
                for (boolean facingRight : List.of(true, false)) {
                    Bird.VisualFeatureGeometry geometry =
                            game.inspectVisualAuditCombatFeatures(entry, pose, facingRight);
                    String direction = facingRight ? "right" : "left";
                    assertEquals(2, geometry.bodyPartCount(Bird.VisualBodyPart.PIGEON_LEG),
                            entry.name() + " / " + pose + " facing " + direction
                                    + " must visibly draw both legs");
                }
            }
        }
    }

    @Test
    void eagleKeepsItsTailWingAndLegsAcrossVectorSkinsPosesAndDirections() {
        BirdGame3 game = freshGame();
        List<BirdGame3.VisualAuditSkin> entries = game.visualAuditSkins().stream()
                .filter(entry -> entry.bird() == BirdGame3.BirdType.EAGLE)
                .filter(entry -> !"STOCK_PHOTO_EAGLE".equals(entry.key()))
                .filter(entry -> BirdSpriteLibrary.sheetFor(entry.bird(), entry.key()) == null)
                .toList();

        for (BirdGame3.VisualAuditSkin entry : entries) {
            for (Bird.VisualAuditPose pose : Bird.VisualAuditPose.values()) {
                for (boolean facingRight : List.of(true, false)) {
                    Bird.VisualFeatureGeometry geometry =
                            game.inspectVisualAuditCombatFeatures(entry, pose, facingRight);
                    String label = entry.name() + " / " + pose + " facing "
                            + (facingRight ? "right" : "left");
                    assertEquals(3,
                            geometry.bodyPartCount(Bird.VisualBodyPart.EAGLE_TAIL_FEATHER),
                            label + " must draw all three tail feathers");
                    assertEquals(1, geometry.bodyPartCount(Bird.VisualBodyPart.EAGLE_WING),
                            label + " must draw its folded wing");
                    assertEquals(2, geometry.bodyPartCount(Bird.VisualBodyPart.EAGLE_LEG),
                            label + " must draw both legs");
                }
            }
        }
    }

    private static void assertFeatureGeometryIsSafe(Bird.VisualFeatureGeometry geometry, String label) {
        assertTrue(geometry != null && geometry.complete(), label + " did not report semantic face geometry");
        assertTrue(geometry.head().contains(geometry.eye(), 0.01),
                label + " places the eye outside the head");
        double rootClearance = geometry.beak().rootDistanceFrom(geometry.eye());
        assertTrue(rootClearance >= geometry.eye().radius() * 0.90,
                label + " lets the beak root obstruct the eye: clearance=" + rootClearance);
    }

    private static void assertMirrored(Bird.VisualFeatureGeometry right,
                                       Bird.VisualFeatureGeometry left,
                                       String label) {
        double rightHeadCenterX = (right.head().left() + right.head().right()) * 0.5;
        double leftHeadCenterX = (left.head().left() + left.head().right()) * 0.5;
        double rightEyeOffset = right.eye().centerX() - rightHeadCenterX;
        double leftEyeOffset = left.eye().centerX() - leftHeadCenterX;
        assertEquals(rightEyeOffset, -leftEyeOffset, 0.01,
                label + " does not mirror the eye horizontally");
        assertEquals(right.eye().centerY() - right.head().top(),
                left.eye().centerY() - left.head().top(), 0.01,
                label + " changes eye height when turning around");
    }

    private static void assertSameFacialGeometry(Bird.VisualFeatureGeometry base,
                                                 Bird.VisualFeatureGeometry skin,
                                                 String label) {
        assertEquals(base.head().right() - base.head().left(),
                skin.head().right() - skin.head().left(), 0.01,
                label + " changes the base head width");
        assertEquals(base.head().bottom() - base.head().top(),
                skin.head().bottom() - skin.head().top(), 0.01,
                label + " changes the base head height");
        assertEquals(base.eye().radius(), skin.eye().radius(), 0.01,
                label + " changes the base eye size");
        assertEquals(base.beak().rootDistanceFrom(base.eye()),
                skin.beak().rootDistanceFrom(skin.eye()), 0.01,
                label + " shifts the eye into the beak");
        assertEquals(base.beak().tipDistanceFrom(base.eye()),
                skin.beak().tipDistanceFrom(skin.eye()), 0.01,
                label + " changes the base beak reach");
    }
}
