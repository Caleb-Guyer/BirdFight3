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
            BirdGame3.BirdType.PHOENIX,
            BirdGame3.BirdType.HUMMINGBIRD,
            BirdGame3.BirdType.TURKEY,
            BirdGame3.BirdType.ROOSTER,
            BirdGame3.BirdType.ROADRUNNER,
            BirdGame3.BirdType.SHOEBILL,
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
                .filter(entry -> !"STOCK_PHOTO_TURKEY".equals(entry.key()))
                .filter(entry -> !"NULL_ROCK_VULTURE".equals(entry.key()))
                .filter(entry -> !"LORE_ACCURATE_HUMMINGBIRD".equals(entry.key()))
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

    @Test
    void falconKeepsItsTailWingAndLegsAcrossVectorSkinsPosesAndDirections() {
        BirdGame3 game = freshGame();
        List<BirdGame3.VisualAuditSkin> entries = game.visualAuditSkins().stream()
                .filter(entry -> entry.bird() == BirdGame3.BirdType.FALCON)
                .filter(entry -> BirdSpriteLibrary.sheetFor(entry.bird(), entry.key()) == null)
                .toList();

        for (BirdGame3.VisualAuditSkin entry : entries) {
            for (Bird.VisualAuditPose pose : Bird.VisualAuditPose.values()) {
                for (boolean facingRight : List.of(true, false)) {
                    Bird.VisualFeatureGeometry geometry =
                            game.inspectVisualAuditCombatFeatures(entry, pose, facingRight);
                    String label = entry.name() + " / " + pose + " facing "
                            + (facingRight ? "right" : "left");
                    assertEquals(2,
                            geometry.bodyPartCount(Bird.VisualBodyPart.FALCON_TAIL_FEATHER),
                            label + " must draw both tail feathers");
                    assertEquals(1, geometry.bodyPartCount(Bird.VisualBodyPart.FALCON_WING),
                            label + " must draw its swept wing");
                    assertEquals(2, geometry.bodyPartCount(Bird.VisualBodyPart.FALCON_LEG),
                            label + " must draw both legs");
                }
            }
        }
    }

    @Test
    void phoenixKeepsItsFlameTailWingCrestAndLegsAcrossVectorSkinsPosesAndDirections() {
        BirdGame3 game = freshGame();
        List<BirdGame3.VisualAuditSkin> entries = game.visualAuditSkins().stream()
                .filter(entry -> entry.bird() == BirdGame3.BirdType.PHOENIX)
                .filter(entry -> BirdSpriteLibrary.sheetFor(entry.bird(), entry.key()) == null)
                .toList();

        for (BirdGame3.VisualAuditSkin entry : entries) {
            for (Bird.VisualAuditPose pose : Bird.VisualAuditPose.values()) {
                for (boolean facingRight : List.of(true, false)) {
                    Bird.VisualFeatureGeometry geometry =
                            game.inspectVisualAuditCombatFeatures(entry, pose, facingRight);
                    String label = entry.name() + " / " + pose + " facing "
                            + (facingRight ? "right" : "left");
                    assertEquals(2,
                            geometry.bodyPartCount(Bird.VisualBodyPart.PHOENIX_TAIL_FLAME),
                            label + " must draw both layers of its flame tail");
                    assertEquals(1, geometry.bodyPartCount(Bird.VisualBodyPart.PHOENIX_WING),
                            label + " must draw its folded wing");
                    assertEquals(1, geometry.bodyPartCount(Bird.VisualBodyPart.PHOENIX_CREST),
                            label + " must draw its central flame crest");
                    assertEquals(2, geometry.bodyPartCount(Bird.VisualBodyPart.PHOENIX_LEG),
                            label + " must draw both legs");
                }
            }
        }
    }

    @Test
    void phoenixHeadAndBeakFollowItsMovementAnimationsInBothDirections() {
        BirdGame3 game = freshGame();
        BirdGame3.VisualAuditSkin phoenix = new BirdGame3.VisualAuditSkin(
                BirdGame3.BirdType.PHOENIX, null, "Phoenix base");

        for (boolean facingRight : List.of(true, false)) {
            Bird.VisualFeatureGeometry idle =
                    game.inspectVisualAuditCombatFeatures(phoenix, Bird.VisualAuditPose.IDLE, facingRight);
            Bird.VisualFeatureGeometry flap =
                    game.inspectVisualAuditCombatFeatures(phoenix, Bird.VisualAuditPose.FLAP, facingRight);
            Bird.VisualFeatureGeometry hit =
                    game.inspectVisualAuditCombatFeatures(phoenix, Bird.VisualAuditPose.HIT, facingRight);
            String direction = facingRight ? "right" : "left";

            assertTrue(Math.abs(beakVectorY(flap) - beakVectorY(idle)) >= 8.0,
                    "Phoenix's beak must look upward during its flight animation while facing " + direction);
            assertTrue(beakVectorX(hit) * beakVectorX(idle) < 0.0,
                    "Phoenix must look back during its hit animation while facing " + direction);
        }
    }

    @Test
    void hummingbirdKeepsItsWingsTailAndLegsAcrossVectorSkinsPosesAndDirections() {
        BirdGame3 game = freshGame();
        List<BirdGame3.VisualAuditSkin> entries = game.visualAuditSkins().stream()
                .filter(entry -> entry.bird() == BirdGame3.BirdType.HUMMINGBIRD)
                .filter(entry -> BirdSpriteLibrary.sheetFor(entry.bird(), entry.key()) == null)
                .toList();

        for (BirdGame3.VisualAuditSkin entry : entries) {
            for (Bird.VisualAuditPose pose : Bird.VisualAuditPose.values()) {
                for (boolean facingRight : List.of(true, false)) {
                    Bird.VisualFeatureGeometry geometry =
                            game.inspectVisualAuditCombatFeatures(entry, pose, facingRight);
                    String label = entry.name() + " / " + pose + " facing "
                            + (facingRight ? "right" : "left");
                    assertEquals(2, geometry.bodyPartCount(Bird.VisualBodyPart.HUMMINGBIRD_WING),
                            label + " must draw both wings");
                    assertEquals(2, geometry.bodyPartCount(Bird.VisualBodyPart.HUMMINGBIRD_TAIL_FEATHER),
                            label + " must draw both tail feathers");
                    assertEquals(2, geometry.bodyPartCount(Bird.VisualBodyPart.HUMMINGBIRD_LEG),
                            label + " must draw both legs");
                    if ("LORE_ACCURATE_HUMMINGBIRD".equals(entry.key())) {
                        assertFeatureGeometryIsSafe(geometry, label);
                    }
                }
            }
        }
    }

    @Test
    void hummingbirdHeadAndNeedleBillFollowMovementAnimationsInBothDirections() {
        BirdGame3 game = freshGame();
        List<BirdGame3.VisualAuditSkin> entries = game.visualAuditSkins().stream()
                .filter(entry -> entry.bird() == BirdGame3.BirdType.HUMMINGBIRD)
                .filter(entry -> BirdSpriteLibrary.sheetFor(entry.bird(), entry.key()) == null)
                .toList();

        for (BirdGame3.VisualAuditSkin entry : entries) {
            for (boolean facingRight : List.of(true, false)) {
                Bird.VisualFeatureGeometry idle = game.inspectVisualAuditCombatFeatures(
                        entry, Bird.VisualAuditPose.IDLE, facingRight);
                Bird.VisualFeatureGeometry flap = game.inspectVisualAuditCombatFeatures(
                        entry, Bird.VisualAuditPose.FLAP, facingRight);
                Bird.VisualFeatureGeometry hit = game.inspectVisualAuditCombatFeatures(
                        entry, Bird.VisualAuditPose.HIT, facingRight);
                String label = entry.name() + " facing " + (facingRight ? "right" : "left");

                assertTrue(Math.abs(beakVectorY(flap) - beakVectorY(idle)) >= 8.0,
                        label + " must point its needle bill upward in flight");
                assertTrue(beakVectorX(hit) * beakVectorX(idle) < 0.0,
                        label + " must look back during its hit animation");
            }
        }
    }

    @Test
    void turkeyKeepsItsFanNeckWingAndLegsAcrossVectorSkinsPosesAndDirections() {
        BirdGame3 game = freshGame();
        List<BirdGame3.VisualAuditSkin> entries = game.visualAuditSkins().stream()
                .filter(entry -> entry.bird() == BirdGame3.BirdType.TURKEY)
                .filter(entry -> !"STOCK_PHOTO_TURKEY".equals(entry.key()))
                .filter(entry -> BirdSpriteLibrary.sheetFor(entry.bird(), entry.key()) == null)
                .toList();

        for (BirdGame3.VisualAuditSkin entry : entries) {
            for (Bird.VisualAuditPose pose : Bird.VisualAuditPose.values()) {
                for (boolean facingRight : List.of(true, false)) {
                    Bird.VisualFeatureGeometry geometry =
                            game.inspectVisualAuditCombatFeatures(entry, pose, facingRight);
                    String label = entry.name() + " / " + pose + " facing "
                            + (facingRight ? "right" : "left");
                    assertEquals(7, geometry.bodyPartCount(Bird.VisualBodyPart.TURKEY_TAIL_FEATHER),
                            label + " must draw all seven fan feathers");
                    assertEquals(1, geometry.bodyPartCount(Bird.VisualBodyPart.TURKEY_NECK),
                            label + " must keep its neck connected to its body");
                    int expectedWings = pose == Bird.VisualAuditPose.FLAP ? 2 : 1;
                    assertEquals(expectedWings,
                            geometry.bodyPartCount(Bird.VisualBodyPart.TURKEY_WING),
                            label + " must draw every visible pose-aware wing");
                    assertEquals(2, geometry.bodyPartCount(Bird.VisualBodyPart.TURKEY_LEG),
                            label + " must visibly draw both legs");
                }
            }
        }
    }

    @Test
    void turkeyHeadAndBeakFollowMovementAnimationsInBothDirections() {
        BirdGame3 game = freshGame();
        List<BirdGame3.VisualAuditSkin> entries = game.visualAuditSkins().stream()
                .filter(entry -> entry.bird() == BirdGame3.BirdType.TURKEY)
                .filter(entry -> !"STOCK_PHOTO_TURKEY".equals(entry.key()))
                .filter(entry -> BirdSpriteLibrary.sheetFor(entry.bird(), entry.key()) == null)
                .toList();

        for (BirdGame3.VisualAuditSkin entry : entries) {
            for (boolean facingRight : List.of(true, false)) {
                Bird.VisualFeatureGeometry idle = game.inspectVisualAuditCombatFeatures(
                        entry, Bird.VisualAuditPose.IDLE, facingRight);
                Bird.VisualFeatureGeometry flap = game.inspectVisualAuditCombatFeatures(
                        entry, Bird.VisualAuditPose.FLAP, facingRight);
                Bird.VisualFeatureGeometry hit = game.inspectVisualAuditCombatFeatures(
                        entry, Bird.VisualAuditPose.HIT, facingRight);
                String label = entry.name() + " facing " + (facingRight ? "right" : "left");

                assertTrue(Math.abs(beakVectorY(flap) - beakVectorY(idle)) >= 8.0,
                        label + " must raise its head and beak during flight");
                assertTrue(beakVectorX(hit) * beakVectorX(idle) < 0.0,
                        label + " must look back during its hit animation");
            }
        }
    }

    @Test
    void turkeyNeckStaysBehindItsHeadWhenRisingAndFalling() {
        BirdGame3 game = freshGame();
        List<BirdGame3.VisualAuditSkin> entries = game.visualAuditSkins().stream()
                .filter(entry -> entry.bird() == BirdGame3.BirdType.TURKEY)
                .filter(entry -> !"STOCK_PHOTO_TURKEY".equals(entry.key()))
                .filter(entry -> BirdSpriteLibrary.sheetFor(entry.bird(), entry.key()) == null)
                .toList();

        for (BirdGame3.VisualAuditSkin entry : entries) {
            for (double verticalVelocity : List.of(-5.0, 5.0)) {
                for (boolean facingRight : List.of(true, false)) {
                    Bird.VisualFeatureGeometry geometry =
                            game.inspectVisualAuditVerticalMotionFeatures(
                                    entry, verticalVelocity, facingRight);
                    String motion = verticalVelocity < 0.0 ? "rising" : "falling";
                    assertTurkeyNeckEndsBehindHead(geometry,
                            entry.name() + " " + motion + " facing "
                                    + (facingRight ? "right" : "left"));
                }
            }
        }
    }

    @Test
    void turkeyPanicFlapOpensBothWingsAndClosesThemBeforeTheMoveEnds() {
        BirdGame3 game = freshGame();
        List<BirdGame3.VisualAuditSkin> entries = game.visualAuditSkins().stream()
                .filter(entry -> entry.bird() == BirdGame3.BirdType.TURKEY)
                .filter(entry -> !"STOCK_PHOTO_TURKEY".equals(entry.key()))
                .filter(entry -> BirdSpriteLibrary.sheetFor(entry.bird(), entry.key()) == null)
                .toList();

        for (BirdGame3.VisualAuditSkin entry : entries) {
            for (boolean facingRight : List.of(true, false)) {
                Bird.VisualFeatureGeometry folded = game.inspectVisualAuditTurkeyPanicFlapFeatures(
                        entry, Bird.TURKEY_PANIC_FLAP_FRAMES, facingRight);
                Bird.VisualFeatureGeometry spread = game.inspectVisualAuditTurkeyPanicFlapFeatures(
                        entry, Bird.TURKEY_PANIC_FLAP_FRAMES - 4, facingRight);
                Bird.VisualFeatureGeometry closing = game.inspectVisualAuditTurkeyPanicFlapFeatures(
                        entry, 1, facingRight);
                String label = entry.name() + " Panic Flap facing "
                        + (facingRight ? "right" : "left");

                assertTrue(folded.turkeyWingOpenness() <= 0.01,
                        label + " must begin with folded wings");
                assertTrue(spread.turkeyWingOpenness() >= 0.95,
                        label + " must visibly reach a fully spread pose");
                assertEquals(2, spread.bodyPartCount(Bird.VisualBodyPart.TURKEY_WING),
                        label + " must display both body wings during the up special");
                assertTrue(closing.turkeyWingOpenness() <= 0.15,
                        label + " must close its wings before Panic Flap ends");
            }
        }
    }

    @Test
    void roosterKeepsItsSickleTailWingsCombAndLegsAcrossVectorSkinsPosesAndDirections() {
        BirdGame3 game = freshGame();
        List<BirdGame3.VisualAuditSkin> entries = game.visualAuditSkins().stream()
                .filter(entry -> entry.bird() == BirdGame3.BirdType.ROOSTER)
                .filter(entry -> BirdSpriteLibrary.sheetFor(entry.bird(), entry.key()) == null)
                .toList();

        for (BirdGame3.VisualAuditSkin entry : entries) {
            for (Bird.VisualAuditPose pose : Bird.VisualAuditPose.values()) {
                for (boolean facingRight : List.of(true, false)) {
                    Bird.VisualFeatureGeometry geometry =
                            game.inspectVisualAuditCombatFeatures(entry, pose, facingRight);
                    String label = entry.name() + " / " + pose + " facing "
                            + (facingRight ? "right" : "left");
                    assertEquals(4,
                            geometry.bodyPartCount(Bird.VisualBodyPart.ROOSTER_TAIL_FEATHER),
                            label + " must draw all four sickle-tail feathers");
                    assertEquals(pose == Bird.VisualAuditPose.FLAP ? 2 : 1,
                            geometry.bodyPartCount(Bird.VisualBodyPart.ROOSTER_WING),
                            label + " must draw every pose-aware wing");
                    assertEquals(2, geometry.bodyPartCount(Bird.VisualBodyPart.ROOSTER_LEG),
                            label + " must visibly draw both legs");
                    assertEquals(3,
                            geometry.bodyPartCount(Bird.VisualBodyPart.ROOSTER_COMB_LOBE),
                            label + " must retain all three rounded comb lobes");
                }
            }
        }
    }

    @Test
    void roosterHeadAndBeakFollowMovementAnimationsInBothDirections() {
        BirdGame3 game = freshGame();
        List<BirdGame3.VisualAuditSkin> entries = game.visualAuditSkins().stream()
                .filter(entry -> entry.bird() == BirdGame3.BirdType.ROOSTER)
                .filter(entry -> BirdSpriteLibrary.sheetFor(entry.bird(), entry.key()) == null)
                .toList();

        for (BirdGame3.VisualAuditSkin entry : entries) {
            for (boolean facingRight : List.of(true, false)) {
                Bird.VisualFeatureGeometry idle = game.inspectVisualAuditCombatFeatures(
                        entry, Bird.VisualAuditPose.IDLE, facingRight);
                Bird.VisualFeatureGeometry flap = game.inspectVisualAuditCombatFeatures(
                        entry, Bird.VisualAuditPose.FLAP, facingRight);
                Bird.VisualFeatureGeometry hit = game.inspectVisualAuditCombatFeatures(
                        entry, Bird.VisualAuditPose.HIT, facingRight);
                String label = entry.name() + " facing " + (facingRight ? "right" : "left");

                assertTrue(Math.abs(beakVectorY(flap) - beakVectorY(idle)) >= 8.0,
                        label + " must raise its head and bill in flight");
                assertTrue(beakVectorX(hit) * beakVectorX(idle) < 0.0,
                        label + " must look back during its hit animation");
            }
        }
    }

    @Test
    void roosterCoopBoostOpensBothWingsThenClosesBeforeTheLiftEnds() {
        BirdGame3 game = freshGame();
        List<BirdGame3.VisualAuditSkin> entries = game.visualAuditSkins().stream()
                .filter(entry -> entry.bird() == BirdGame3.BirdType.ROOSTER)
                .filter(entry -> BirdSpriteLibrary.sheetFor(entry.bird(), entry.key()) == null)
                .toList();

        for (BirdGame3.VisualAuditSkin entry : entries) {
            for (boolean facingRight : List.of(true, false)) {
                Bird.VisualFeatureGeometry folded = game.inspectVisualAuditRoosterCoopBoostFeatures(
                        entry, 38, facingRight);
                Bird.VisualFeatureGeometry spread = game.inspectVisualAuditRoosterCoopBoostFeatures(
                        entry, 30, facingRight);
                Bird.VisualFeatureGeometry closing = game.inspectVisualAuditRoosterCoopBoostFeatures(
                        entry, 1, facingRight);
                String label = entry.name() + " Coop Boost facing "
                        + (facingRight ? "right" : "left");

                assertTrue(folded.roosterWingOpenness() <= 0.01,
                        label + " must begin with folded wings");
                assertTrue(spread.roosterWingOpenness() >= 0.95,
                        label + " must visibly reach a fully spread pose");
                assertEquals(2, spread.bodyPartCount(Bird.VisualBodyPart.ROOSTER_WING),
                        label + " must display both wings during the lift");
                assertTrue(closing.roosterWingOpenness() <= 0.15,
                        label + " must close its wings before Coop Boost ends");
            }
        }
    }

    @Test
    void roadrunnerKeepsItsTailWingsCrestAndLegsAcrossVectorSkinsPosesAndDirections() {
        BirdGame3 game = freshGame();
        List<BirdGame3.VisualAuditSkin> entries = game.visualAuditSkins().stream()
                .filter(entry -> entry.bird() == BirdGame3.BirdType.ROADRUNNER)
                .filter(entry -> BirdSpriteLibrary.sheetFor(entry.bird(), entry.key()) == null)
                .toList();

        for (BirdGame3.VisualAuditSkin entry : entries) {
            for (Bird.VisualAuditPose pose : Bird.VisualAuditPose.values()) {
                for (boolean facingRight : List.of(true, false)) {
                    Bird.VisualFeatureGeometry geometry =
                            game.inspectVisualAuditCombatFeatures(entry, pose, facingRight);
                    String label = entry.name() + " / " + pose + " facing "
                            + (facingRight ? "right" : "left");
                    assertEquals(4,
                            geometry.bodyPartCount(Bird.VisualBodyPart.ROADRUNNER_TAIL_FEATHER),
                            label + " must retain all four long tail feathers");
                    assertEquals(pose == Bird.VisualAuditPose.FLAP ? 2 : 1,
                            geometry.bodyPartCount(Bird.VisualBodyPart.ROADRUNNER_WING),
                            label + " must draw every pose-aware wing");
                    assertEquals(2,
                            geometry.bodyPartCount(Bird.VisualBodyPart.ROADRUNNER_LEG),
                            label + " must keep both long legs visible");
                    assertEquals(3,
                            geometry.bodyPartCount(Bird.VisualBodyPart.ROADRUNNER_CREST_FEATHER),
                            label + " must retain all three crest feathers");
                    assertFeatureGeometryIsSafe(geometry, label);
                }
            }
        }
    }

    @Test
    void roadrunnerHeadAndBillFollowMovementAnimationsInBothDirections() {
        BirdGame3 game = freshGame();
        List<BirdGame3.VisualAuditSkin> entries = game.visualAuditSkins().stream()
                .filter(entry -> entry.bird() == BirdGame3.BirdType.ROADRUNNER)
                .filter(entry -> BirdSpriteLibrary.sheetFor(entry.bird(), entry.key()) == null)
                .toList();

        for (BirdGame3.VisualAuditSkin entry : entries) {
            for (boolean facingRight : List.of(true, false)) {
                Bird.VisualFeatureGeometry idle = game.inspectVisualAuditCombatFeatures(
                        entry, Bird.VisualAuditPose.IDLE, facingRight);
                Bird.VisualFeatureGeometry flap = game.inspectVisualAuditCombatFeatures(
                        entry, Bird.VisualAuditPose.FLAP, facingRight);
                Bird.VisualFeatureGeometry hit = game.inspectVisualAuditCombatFeatures(
                        entry, Bird.VisualAuditPose.HIT, facingRight);
                String label = entry.name() + " facing " + (facingRight ? "right" : "left");

                assertTrue(Math.abs(beakVectorY(flap) - beakVectorY(idle)) >= 8.0,
                        label + " must raise its long bill during flight");
                assertTrue(beakVectorX(hit) * beakVectorX(idle) < 0.0,
                        label + " must look back during its hit animation");
            }
        }
    }

    @Test
    void roadrunnerDustDevilOpensBothWingsThenClosesBeforeTheLiftEnds() {
        BirdGame3 game = freshGame();
        List<BirdGame3.VisualAuditSkin> entries = game.visualAuditSkins().stream()
                .filter(entry -> entry.bird() == BirdGame3.BirdType.ROADRUNNER)
                .filter(entry -> BirdSpriteLibrary.sheetFor(entry.bird(), entry.key()) == null)
                .toList();

        for (BirdGame3.VisualAuditSkin entry : entries) {
            for (boolean facingRight : List.of(true, false)) {
                Bird.VisualFeatureGeometry folded = game.inspectVisualAuditRoadrunnerDustDevilFeatures(
                        entry, Bird.ROADRUNNER_DUST_DEVIL_FRAMES, facingRight);
                Bird.VisualFeatureGeometry spread = game.inspectVisualAuditRoadrunnerDustDevilFeatures(
                        entry, Bird.ROADRUNNER_DUST_DEVIL_FRAMES - 4, facingRight);
                Bird.VisualFeatureGeometry closing = game.inspectVisualAuditRoadrunnerDustDevilFeatures(
                        entry, 1, facingRight);
                String label = entry.name() + " Dust Devil Lift facing "
                        + (facingRight ? "right" : "left");

                assertTrue(folded.roadrunnerWingOpenness() <= 0.01,
                        label + " must begin with folded wings");
                assertTrue(spread.roadrunnerWingOpenness() >= 0.95,
                        label + " must visibly reach a fully spread pose");
                assertEquals(2, spread.bodyPartCount(Bird.VisualBodyPart.ROADRUNNER_WING),
                        label + " must display both wings during the lift");
                assertTrue(closing.roadrunnerWingOpenness() <= 0.15,
                        label + " must close its wings before Dust Devil Lift ends");
            }
        }
    }

    @Test
    void penguinKeepsItsFlippersTailAndWebbedFeetAcrossVectorSkinsPosesAndDirections() {
        BirdGame3 game = freshGame();
        List<BirdGame3.VisualAuditSkin> entries = game.visualAuditSkins().stream()
                .filter(entry -> entry.bird() == BirdGame3.BirdType.PENGUIN)
                .filter(entry -> BirdSpriteLibrary.sheetFor(entry.bird(), entry.key()) == null)
                .toList();

        for (BirdGame3.VisualAuditSkin entry : entries) {
            for (Bird.VisualAuditPose pose : Bird.VisualAuditPose.values()) {
                for (boolean facingRight : List.of(true, false)) {
                    Bird.VisualFeatureGeometry geometry =
                            game.inspectVisualAuditCombatFeatures(entry, pose, facingRight);
                    String label = entry.name() + " / " + pose + " facing "
                            + (facingRight ? "right" : "left");
                    assertEquals(2,
                            geometry.bodyPartCount(Bird.VisualBodyPart.PENGUIN_FLIPPER),
                            label + " must draw both flippers");
                    assertEquals(3,
                            geometry.bodyPartCount(Bird.VisualBodyPart.PENGUIN_TAIL_FEATHER),
                            label + " must retain all three short tail feathers");
                    assertEquals(2,
                            geometry.bodyPartCount(Bird.VisualBodyPart.PENGUIN_FOOT),
                            label + " must keep both webbed feet visible");
                    assertFeatureGeometryIsSafe(geometry, label);
                }
            }
        }
    }

    @Test
    void penguinHeadAndBeakFollowMovementAnimationsInBothDirections() {
        BirdGame3 game = freshGame();
        List<BirdGame3.VisualAuditSkin> entries = game.visualAuditSkins().stream()
                .filter(entry -> entry.bird() == BirdGame3.BirdType.PENGUIN)
                .filter(entry -> BirdSpriteLibrary.sheetFor(entry.bird(), entry.key()) == null)
                .toList();

        for (BirdGame3.VisualAuditSkin entry : entries) {
            for (boolean facingRight : List.of(true, false)) {
                Bird.VisualFeatureGeometry idle = game.inspectVisualAuditCombatFeatures(
                        entry, Bird.VisualAuditPose.IDLE, facingRight);
                Bird.VisualFeatureGeometry flap = game.inspectVisualAuditCombatFeatures(
                        entry, Bird.VisualAuditPose.FLAP, facingRight);
                Bird.VisualFeatureGeometry hit = game.inspectVisualAuditCombatFeatures(
                        entry, Bird.VisualAuditPose.HIT, facingRight);
                String label = entry.name() + " facing " + (facingRight ? "right" : "left");

                assertTrue(Math.abs(beakVectorY(flap) - beakVectorY(idle)) >= 8.0,
                        label + " must raise its head and beak during flight");
                assertTrue(beakVectorX(hit) * beakVectorX(idle) < 0.0,
                        label + " must look back during its hit animation");
            }
        }
    }

    @Test
    void penguinRocketPopOpensBothFlippersThenClosesBeforeTheLiftEnds() {
        BirdGame3 game = freshGame();
        List<BirdGame3.VisualAuditSkin> entries = game.visualAuditSkins().stream()
                .filter(entry -> entry.bird() == BirdGame3.BirdType.PENGUIN)
                .filter(entry -> BirdSpriteLibrary.sheetFor(entry.bird(), entry.key()) == null)
                .toList();

        for (BirdGame3.VisualAuditSkin entry : entries) {
            for (boolean facingRight : List.of(true, false)) {
                Bird.VisualFeatureGeometry folded = game.inspectVisualAuditPenguinRocketFeatures(
                        entry, Bird.PENGUIN_ROCKET_FRAMES, facingRight);
                Bird.VisualFeatureGeometry spread = game.inspectVisualAuditPenguinRocketFeatures(
                        entry, Bird.PENGUIN_ROCKET_FRAMES - 4, facingRight);
                Bird.VisualFeatureGeometry closing = game.inspectVisualAuditPenguinRocketFeatures(
                        entry, 1, facingRight);
                String label = entry.name() + " Rocket Pop facing "
                        + (facingRight ? "right" : "left");

                assertTrue(folded.penguinFlipperOpenness() <= 0.01,
                        label + " must begin with tucked flippers");
                assertTrue(spread.penguinFlipperOpenness() >= 0.95,
                        label + " must visibly reach a fully spread pose");
                assertEquals(2, spread.bodyPartCount(Bird.VisualBodyPart.PENGUIN_FLIPPER),
                        label + " must display both flippers during the lift");
                assertTrue(closing.penguinFlipperOpenness() <= 0.15,
                        label + " must close its flippers before Rocket Pop ends");
            }
        }
    }

    @Test
    void penguinBellyExpressTucksFlippersWithoutLosingFeetOrTail() {
        BirdGame3 game = freshGame();
        List<BirdGame3.VisualAuditSkin> entries = game.visualAuditSkins().stream()
                .filter(entry -> entry.bird() == BirdGame3.BirdType.PENGUIN)
                .filter(entry -> BirdSpriteLibrary.sheetFor(entry.bird(), entry.key()) == null)
                .toList();

        for (BirdGame3.VisualAuditSkin entry : entries) {
            for (boolean facingRight : List.of(true, false)) {
                Bird.VisualFeatureGeometry slide = game.inspectVisualAuditPenguinBellySlideFeatures(
                        entry, Bird.PENGUIN_BELLY_SLIDE_FRAMES / 2, facingRight);
                String label = entry.name() + " Belly Express facing "
                        + (facingRight ? "right" : "left");

                assertTrue(slide.penguinFlipperOpenness() <= 0.01,
                        label + " must streamline both flippers against the body");
                assertEquals(2, slide.bodyPartCount(Bird.VisualBodyPart.PENGUIN_FLIPPER),
                        label + " must keep both tucked flippers visible");
                assertEquals(2, slide.bodyPartCount(Bird.VisualBodyPart.PENGUIN_FOOT),
                        label + " must keep both tucked feet visible");
                assertEquals(3, slide.bodyPartCount(Bird.VisualBodyPart.PENGUIN_TAIL_FEATHER),
                        label + " must keep the short tail intact during the slide");
            }
        }
    }

    @Test
    void shoebillKeepsItsWingsTailCrestAndLongLegsAcrossVectorSkinsPosesAndDirections() {
        BirdGame3 game = freshGame();
        List<BirdGame3.VisualAuditSkin> entries = game.visualAuditSkins().stream()
                .filter(entry -> entry.bird() == BirdGame3.BirdType.SHOEBILL)
                .filter(entry -> BirdSpriteLibrary.sheetFor(entry.bird(), entry.key()) == null)
                .toList();

        for (BirdGame3.VisualAuditSkin entry : entries) {
            for (Bird.VisualAuditPose pose : Bird.VisualAuditPose.values()) {
                for (boolean facingRight : List.of(true, false)) {
                    Bird.VisualFeatureGeometry geometry =
                            game.inspectVisualAuditCombatFeatures(entry, pose, facingRight);
                    String label = entry.name() + " / " + pose + " facing "
                            + (facingRight ? "right" : "left");
                    assertEquals(2,
                            geometry.bodyPartCount(Bird.VisualBodyPart.SHOEBILL_WING),
                            label + " must draw both wings");
                    assertEquals(3,
                            geometry.bodyPartCount(Bird.VisualBodyPart.SHOEBILL_TAIL_FEATHER),
                            label + " must retain all three layered tail feathers");
                    assertEquals(2,
                            geometry.bodyPartCount(Bird.VisualBodyPart.SHOEBILL_LEG),
                            label + " must retain both long legs and feet");
                    assertEquals(4,
                            geometry.bodyPartCount(Bird.VisualBodyPart.SHOEBILL_CREST_FEATHER),
                            label + " must retain all four crest feathers");
                    assertFeatureGeometryIsSafe(geometry, label);
                }
            }
        }
    }

    @Test
    void shoebillGroundedFeetMeetTheGameplayFloorAcrossSkinsAndDirections() {
        BirdGame3 game = freshGame();
        List<BirdGame3.VisualAuditSkin> entries = game.visualAuditSkins().stream()
                .filter(entry -> entry.bird() == BirdGame3.BirdType.SHOEBILL)
                .filter(entry -> BirdSpriteLibrary.sheetFor(entry.bird(), entry.key()) == null)
                .toList();

        for (BirdGame3.VisualAuditSkin entry : entries) {
            for (Bird.VisualAuditPose pose : List.of(
                    Bird.VisualAuditPose.IDLE, Bird.VisualAuditPose.RUN)) {
                for (boolean facingRight : List.of(true, false)) {
                    Bird.VisualFeatureGeometry geometry =
                            game.inspectVisualAuditCombatFeatures(entry, pose, facingRight);
                    String label = entry.name() + " / " + pose + " facing "
                            + (facingRight ? "right" : "left");

                    assertTrue(Double.isFinite(geometry.shoebillFootBaseline()),
                            label + " did not report a grounded foot baseline");
                    assertEquals(80.0, geometry.shoebillFootBaseline(), 0.15,
                            label + " must place the visible toe stroke on the 80-unit collision floor");
                }
            }
        }
    }

    @Test
    void shoebillHeadEyeAndBillFollowMovementAnimationsInBothDirections() {
        BirdGame3 game = freshGame();
        List<BirdGame3.VisualAuditSkin> entries = game.visualAuditSkins().stream()
                .filter(entry -> entry.bird() == BirdGame3.BirdType.SHOEBILL)
                .filter(entry -> BirdSpriteLibrary.sheetFor(entry.bird(), entry.key()) == null)
                .toList();

        for (BirdGame3.VisualAuditSkin entry : entries) {
            for (boolean facingRight : List.of(true, false)) {
                Bird.VisualFeatureGeometry idle = game.inspectVisualAuditCombatFeatures(
                        entry, Bird.VisualAuditPose.IDLE, facingRight);
                Bird.VisualFeatureGeometry flap = game.inspectVisualAuditCombatFeatures(
                        entry, Bird.VisualAuditPose.FLAP, facingRight);
                Bird.VisualFeatureGeometry hit = game.inspectVisualAuditCombatFeatures(
                        entry, Bird.VisualAuditPose.HIT, facingRight);
                String label = entry.name() + " facing " + (facingRight ? "right" : "left");

                assertTrue(Math.abs(beakVectorY(flap) - beakVectorY(idle)) >= 20.0,
                        label + " must point its heavy bill upward during flight");
                assertTrue(beakVectorX(hit) * beakVectorX(idle) < 0.0,
                        label + " must turn its head and bill backward during hitstun");
                assertFeatureGeometryIsSafe(idle, label + " idle");
                assertFeatureGeometryIsSafe(flap, label + " flap");
                assertFeatureGeometryIsSafe(hit, label + " hit");
            }
        }
    }

    @Test
    void shoebillMarshLiftOpensBothWingsThenClosesBeforeTheLiftEnds() {
        BirdGame3 game = freshGame();
        List<BirdGame3.VisualAuditSkin> entries = game.visualAuditSkins().stream()
                .filter(entry -> entry.bird() == BirdGame3.BirdType.SHOEBILL)
                .filter(entry -> BirdSpriteLibrary.sheetFor(entry.bird(), entry.key()) == null)
                .toList();

        for (BirdGame3.VisualAuditSkin entry : entries) {
            for (boolean facingRight : List.of(true, false)) {
                Bird.VisualFeatureGeometry folded = game.inspectVisualAuditShoebillMarshLiftFeatures(
                        entry, Bird.SHOEBILL_MARSH_LIFT_FRAMES, facingRight);
                Bird.VisualFeatureGeometry spread = game.inspectVisualAuditShoebillMarshLiftFeatures(
                        entry, Bird.SHOEBILL_MARSH_LIFT_FRAMES - 4, facingRight);
                Bird.VisualFeatureGeometry closing = game.inspectVisualAuditShoebillMarshLiftFeatures(
                        entry, 1, facingRight);
                String label = entry.name() + " Marsh Lift facing "
                        + (facingRight ? "right" : "left");

                assertTrue(folded.shoebillWingOpenness() <= 0.01,
                        label + " must begin with folded wings");
                assertTrue(spread.shoebillWingOpenness() >= 0.95,
                        label + " must reach a fully spread two-wing pose");
                assertEquals(2, spread.bodyPartCount(Bird.VisualBodyPart.SHOEBILL_WING),
                        label + " must display both wings during the lift");
                assertTrue(closing.shoebillWingOpenness() <= 0.15,
                        label + " must close both wings before Marsh Lift ends");
            }
        }
    }

    @Test
    void shoebillHeavyThrustExtendsTheBillWithoutDroppingBodyParts() {
        BirdGame3 game = freshGame();
        List<BirdGame3.VisualAuditSkin> entries = game.visualAuditSkins().stream()
                .filter(entry -> entry.bird() == BirdGame3.BirdType.SHOEBILL)
                .filter(entry -> BirdSpriteLibrary.sheetFor(entry.bird(), entry.key()) == null)
                .toList();

        for (BirdGame3.VisualAuditSkin entry : entries) {
            for (boolean facingRight : List.of(true, false)) {
                Bird.VisualFeatureGeometry windup = game.inspectVisualAuditShoebillThrustFeatures(
                        entry, Bird.SHOEBILL_THRUST_FRAMES, facingRight);
                Bird.VisualFeatureGeometry strike = game.inspectVisualAuditShoebillThrustFeatures(
                        entry, Bird.SHOEBILL_THRUST_FRAMES - Bird.SHOEBILL_THRUST_STARTUP_FRAMES - 5,
                        facingRight);
                String label = entry.name() + " Heavy Thrust facing "
                        + (facingRight ? "right" : "left");
                double windupLength = Math.hypot(beakVectorX(windup), beakVectorY(windup));
                double strikeLength = Math.hypot(beakVectorX(strike), beakVectorY(strike));

                assertTrue(strikeLength >= windupLength * 1.45,
                        label + " must visibly extend its bill during the strike: windup="
                                + windupLength + ", strike=" + strikeLength);
                assertTrue(strike.shoebillWingOpenness() <= 0.01,
                        label + " must brace with folded wings");
                assertEquals(2, strike.bodyPartCount(Bird.VisualBodyPart.SHOEBILL_LEG),
                        label + " must keep both legs during the lunge");
                assertEquals(4, strike.bodyPartCount(Bird.VisualBodyPart.SHOEBILL_CREST_FEATHER),
                        label + " must keep the crest connected during the lunge");
                assertFeatureGeometryIsSafe(strike, label);
            }
        }
    }

    @Test
    void charlesKeepsHisOwnWingsLongTailAndLegsAcrossVectorSkinsPosesAndDirections() {
        BirdGame3 game = freshGame();
        List<BirdGame3.VisualAuditSkin> entries = game.visualAuditSkins().stream()
                .filter(entry -> entry.bird() == BirdGame3.BirdType.MOCKINGBIRD)
                .filter(entry -> BirdSpriteLibrary.sheetFor(entry.bird(), entry.key()) == null)
                .toList();

        for (BirdGame3.VisualAuditSkin entry : entries) {
            for (Bird.VisualAuditPose pose : Bird.VisualAuditPose.values()) {
                for (boolean facingRight : List.of(true, false)) {
                    Bird.VisualFeatureGeometry geometry =
                            game.inspectVisualAuditCombatFeatures(entry, pose, facingRight);
                    String label = entry.name() + " / " + pose + " facing "
                            + (facingRight ? "right" : "left");
                    assertEquals(2,
                            geometry.bodyPartCount(Bird.VisualBodyPart.CHARLES_WING),
                            label + " must draw Charles's two authored wings");
                    assertEquals(4,
                            geometry.bodyPartCount(Bird.VisualBodyPart.CHARLES_TAIL_FEATHER),
                            label + " must retain Charles's four-feather mockingbird tail");
                    assertEquals(2,
                            geometry.bodyPartCount(Bird.VisualBodyPart.CHARLES_LEG),
                            label + " must retain both legs and feet");
                    assertFeatureGeometryIsSafe(geometry, label);
                }
            }
        }
    }

    @Test
    void charlesGroundedFeetMeetTheGameplayFloorAcrossSkinsAndDirections() {
        BirdGame3 game = freshGame();
        List<BirdGame3.VisualAuditSkin> entries = game.visualAuditSkins().stream()
                .filter(entry -> entry.bird() == BirdGame3.BirdType.MOCKINGBIRD)
                .filter(entry -> BirdSpriteLibrary.sheetFor(entry.bird(), entry.key()) == null)
                .toList();

        for (BirdGame3.VisualAuditSkin entry : entries) {
            for (Bird.VisualAuditPose pose : List.of(
                    Bird.VisualAuditPose.IDLE, Bird.VisualAuditPose.RUN)) {
                for (boolean facingRight : List.of(true, false)) {
                    Bird.VisualFeatureGeometry geometry =
                            game.inspectVisualAuditCombatFeatures(entry, pose, facingRight);
                    String label = entry.name() + " / " + pose + " facing "
                            + (facingRight ? "right" : "left");
                    assertTrue(Double.isFinite(geometry.charlesFootBaseline()),
                            label + " did not report a grounded foot baseline");
                    assertEquals(80.0, geometry.charlesFootBaseline(), 0.15,
                            label + " must place the visible toe stroke on the 80-unit collision floor");
                }
            }
        }
    }

    @Test
    void charlesHeadEyeAndBillFollowMovementAnimationsInBothDirections() {
        BirdGame3 game = freshGame();
        List<BirdGame3.VisualAuditSkin> entries = game.visualAuditSkins().stream()
                .filter(entry -> entry.bird() == BirdGame3.BirdType.MOCKINGBIRD)
                .filter(entry -> BirdSpriteLibrary.sheetFor(entry.bird(), entry.key()) == null)
                .toList();

        for (BirdGame3.VisualAuditSkin entry : entries) {
            for (boolean facingRight : List.of(true, false)) {
                Bird.VisualFeatureGeometry idle = game.inspectVisualAuditCombatFeatures(
                        entry, Bird.VisualAuditPose.IDLE, facingRight);
                Bird.VisualFeatureGeometry flap = game.inspectVisualAuditCombatFeatures(
                        entry, Bird.VisualAuditPose.FLAP, facingRight);
                Bird.VisualFeatureGeometry hit = game.inspectVisualAuditCombatFeatures(
                        entry, Bird.VisualAuditPose.HIT, facingRight);
                String label = entry.name() + " facing " + (facingRight ? "right" : "left");

                assertTrue(Math.abs(beakVectorY(flap) - beakVectorY(idle)) >= 18.0,
                        label + " must point its bill upward during flight");
                assertTrue(beakVectorX(hit) * beakVectorX(idle) < 0.0,
                        label + " must turn its head and bill backward during hitstun");
                assertFeatureGeometryIsSafe(idle, label + " idle");
                assertFeatureGeometryIsSafe(flap, label + " flap");
                assertFeatureGeometryIsSafe(hit, label + " hit");
            }
        }
    }

    @Test
    void charlesForestLiftOpensBothWingsThenClosesBeforeTheLiftEnds() {
        BirdGame3 game = freshGame();
        List<BirdGame3.VisualAuditSkin> entries = game.visualAuditSkins().stream()
                .filter(entry -> entry.bird() == BirdGame3.BirdType.MOCKINGBIRD)
                .filter(entry -> BirdSpriteLibrary.sheetFor(entry.bird(), entry.key()) == null)
                .toList();

        for (BirdGame3.VisualAuditSkin entry : entries) {
            for (boolean facingRight : List.of(true, false)) {
                Bird.VisualFeatureGeometry folded = game.inspectVisualAuditCharlesForestLiftFeatures(
                        entry, Bird.MOCKINGBIRD_UP_FX_FRAMES, facingRight);
                Bird.VisualFeatureGeometry spread = game.inspectVisualAuditCharlesForestLiftFeatures(
                        entry, Bird.MOCKINGBIRD_UP_FX_FRAMES - 4, facingRight);
                Bird.VisualFeatureGeometry closing = game.inspectVisualAuditCharlesForestLiftFeatures(
                        entry, 1, facingRight);
                String label = entry.name() + " Forest Lift facing "
                        + (facingRight ? "right" : "left");

                assertTrue(folded.charlesWingOpenness() <= 0.01,
                        label + " must begin with folded wings");
                assertTrue(spread.charlesWingOpenness() >= 0.95,
                        label + " must reach a fully spread two-wing pose");
                assertEquals(2, spread.bodyPartCount(Bird.VisualBodyPart.CHARLES_WING),
                        label + " must display both wings during Forest Lift");
                assertTrue(closing.charlesWingOpenness() <= 0.18,
                        label + " must close both wings before Forest Lift ends");
            }
        }
    }

    @Test
    void razorbillKeepsBladeWingsTailFeetAndGroovedBillAcrossSkinsPosesAndDirections() {
        BirdGame3 game = freshGame();
        List<BirdGame3.VisualAuditSkin> entries = game.visualAuditSkins().stream()
                .filter(entry -> entry.bird() == BirdGame3.BirdType.RAZORBILL)
                .filter(entry -> BirdSpriteLibrary.sheetFor(entry.bird(), entry.key()) == null)
                .toList();

        for (BirdGame3.VisualAuditSkin entry : entries) {
            for (Bird.VisualAuditPose pose : Bird.VisualAuditPose.values()) {
                for (boolean facingRight : List.of(true, false)) {
                    Bird.VisualFeatureGeometry geometry =
                            game.inspectVisualAuditCombatFeatures(entry, pose, facingRight);
                    String label = entry.name() + " / " + pose + " facing "
                            + (facingRight ? "right" : "left");
                    assertEquals(2,
                            geometry.bodyPartCount(Bird.VisualBodyPart.RAZORBILL_WING),
                            label + " must draw both blade-like wings");
                    assertEquals(3,
                            geometry.bodyPartCount(Bird.VisualBodyPart.RAZORBILL_TAIL_FEATHER),
                            label + " must retain all three short tail feathers");
                    assertEquals(2,
                            geometry.bodyPartCount(Bird.VisualBodyPart.RAZORBILL_LEG),
                            label + " must retain both legs and webbed feet");
                    assertEquals(1,
                            geometry.bodyPartCount(Bird.VisualBodyPart.RAZORBILL_BILL_GROOVE),
                            label + " must retain the species-defining white bill groove");
                    assertFeatureGeometryIsSafe(geometry, label);
                }
            }
        }
    }

    @Test
    void razorbillGroundedFeetMeetTheGameplayFloorAcrossSkinsAndDirections() {
        BirdGame3 game = freshGame();
        List<BirdGame3.VisualAuditSkin> entries = game.visualAuditSkins().stream()
                .filter(entry -> entry.bird() == BirdGame3.BirdType.RAZORBILL)
                .filter(entry -> BirdSpriteLibrary.sheetFor(entry.bird(), entry.key()) == null)
                .toList();

        for (BirdGame3.VisualAuditSkin entry : entries) {
            for (Bird.VisualAuditPose pose : List.of(
                    Bird.VisualAuditPose.IDLE, Bird.VisualAuditPose.RUN)) {
                for (boolean facingRight : List.of(true, false)) {
                    Bird.VisualFeatureGeometry geometry =
                            game.inspectVisualAuditCombatFeatures(entry, pose, facingRight);
                    String label = entry.name() + " / " + pose + " facing "
                            + (facingRight ? "right" : "left");
                    assertTrue(Double.isFinite(geometry.razorbillFootBaseline()),
                            label + " did not report a grounded foot baseline");
                    assertEquals(80.0, geometry.razorbillFootBaseline(), 0.15,
                            label + " must place the visible foot edge on the 80-unit collision floor");
                }
            }
        }
    }

    @Test
    void razorbillHeadEyeAndBillFollowMovementAnimationsInBothDirections() {
        BirdGame3 game = freshGame();
        List<BirdGame3.VisualAuditSkin> entries = game.visualAuditSkins().stream()
                .filter(entry -> entry.bird() == BirdGame3.BirdType.RAZORBILL)
                .filter(entry -> BirdSpriteLibrary.sheetFor(entry.bird(), entry.key()) == null)
                .toList();

        for (BirdGame3.VisualAuditSkin entry : entries) {
            for (boolean facingRight : List.of(true, false)) {
                Bird.VisualFeatureGeometry idle = game.inspectVisualAuditCombatFeatures(
                        entry, Bird.VisualAuditPose.IDLE, facingRight);
                Bird.VisualFeatureGeometry flap = game.inspectVisualAuditCombatFeatures(
                        entry, Bird.VisualAuditPose.FLAP, facingRight);
                Bird.VisualFeatureGeometry hit = game.inspectVisualAuditCombatFeatures(
                        entry, Bird.VisualAuditPose.HIT, facingRight);
                String label = entry.name() + " facing " + (facingRight ? "right" : "left");

                assertTrue(Math.abs(beakVectorY(flap) - beakVectorY(idle)) >= 18.0,
                        label + " must point its deep bill upward during flight");
                assertTrue(beakVectorX(hit) * beakVectorX(idle) < 0.0,
                        label + " must turn its head and bill backward during hitstun");
                assertFeatureGeometryIsSafe(idle, label + " idle");
                assertFeatureGeometryIsSafe(flap, label + " flap");
                assertFeatureGeometryIsSafe(hit, label + " hit");
            }
        }
    }

    @Test
    void razorbillCliffShearOpensBothWingsThenTucksThemBeforeTheCutEnds() {
        BirdGame3 game = freshGame();
        List<BirdGame3.VisualAuditSkin> entries = game.visualAuditSkins().stream()
                .filter(entry -> entry.bird() == BirdGame3.BirdType.RAZORBILL)
                .filter(entry -> BirdSpriteLibrary.sheetFor(entry.bird(), entry.key()) == null)
                .toList();

        for (BirdGame3.VisualAuditSkin entry : entries) {
            for (boolean facingRight : List.of(true, false)) {
                Bird.VisualFeatureGeometry folded = game.inspectVisualAuditRazorbillCliffShearFeatures(
                        entry, Bird.RAZORBILL_SHEAR_FRAMES, facingRight);
                Bird.VisualFeatureGeometry spread = game.inspectVisualAuditRazorbillCliffShearFeatures(
                        entry, Bird.RAZORBILL_SHEAR_FRAMES - 4, facingRight);
                Bird.VisualFeatureGeometry closing = game.inspectVisualAuditRazorbillCliffShearFeatures(
                        entry, 1, facingRight);
                String label = entry.name() + " Cliff Shear facing "
                        + (facingRight ? "right" : "left");

                assertTrue(folded.razorbillWingOpenness() <= 0.01,
                        label + " must begin with tucked wings");
                assertTrue(spread.razorbillWingOpenness() >= 0.95,
                        label + " must reach a fully spread two-wing cutting pose");
                assertEquals(2, spread.bodyPartCount(Bird.VisualBodyPart.RAZORBILL_WING),
                        label + " must display both wings during Cliff Shear");
                assertTrue(closing.razorbillWingOpenness() <= 0.17,
                        label + " must tuck both wings before Cliff Shear ends");
            }
        }
    }

    private static void assertTurkeyNeckEndsBehindHead(
            Bird.VisualFeatureGeometry geometry, String label) {
        assertTrue(geometry.turkeyNeck() != null, label + " did not report its neck geometry");
        double headCenterX = (geometry.head().left() + geometry.head().right()) * 0.5;
        double headCenterY = (geometry.head().top() + geometry.head().bottom()) * 0.5;
        double beakX = beakVectorX(geometry);
        double beakY = beakVectorY(geometry);
        double neckEndX = geometry.turkeyNeck().tipX() - headCenterX;
        double neckEndY = geometry.turkeyNeck().tipY() - headCenterY;

        assertTrue(neckEndX * beakX + neckEndY * beakY < 0.0,
                label + " lets the center neck strip cross into or beyond the bill-facing half of the head");
    }

    private static double beakVectorX(Bird.VisualFeatureGeometry geometry) {
        return geometry.beak().tipX() - geometry.beak().rootX();
    }

    private static double beakVectorY(Bird.VisualFeatureGeometry geometry) {
        return geometry.beak().tipY() - geometry.beak().rootY();
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
