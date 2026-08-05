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
            BirdGame3.BirdType.VULTURE,
            BirdGame3.BirdType.OPIUMBIRD,
            BirdGame3.BirdType.TITMOUSE,
            BirdGame3.BirdType.BAT,
            BirdGame3.BirdType.PELICAN,
            BirdGame3.BirdType.HEISENBIRD,
            BirdGame3.BirdType.RAVEN,
            BirdGame3.BirdType.GOOSE
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

    @Test
    void grinchhawkKeepsRaggedWingsTailTalonsAndCrestAcrossSkinsPosesAndDirections() {
        BirdGame3 game = freshGame();
        List<BirdGame3.VisualAuditSkin> entries = game.visualAuditSkins().stream()
                .filter(entry -> entry.bird() == BirdGame3.BirdType.GRINCHHAWK)
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
                            geometry.bodyPartCount(Bird.VisualBodyPart.GRINCHHAWK_WING),
                            label + " must draw both ragged hawk wings");
                    assertEquals(4,
                            geometry.bodyPartCount(Bird.VisualBodyPart.GRINCHHAWK_TAIL_FEATHER),
                            label + " must retain all four tail feathers");
                    assertEquals(2,
                            geometry.bodyPartCount(Bird.VisualBodyPart.GRINCHHAWK_LEG),
                            label + " must retain both taloned legs");
                    assertEquals(3,
                            geometry.bodyPartCount(Bird.VisualBodyPart.GRINCHHAWK_CREST_FEATHER),
                            label + " must retain the hostile three-feather crest");
                    assertFeatureGeometryIsSafe(geometry, label);
                }
            }
        }
    }

    @Test
    void grinchhawkGroundedTalonsMeetTheGameplayFloorAcrossSkinsAndDirections() {
        BirdGame3 game = freshGame();
        List<BirdGame3.VisualAuditSkin> entries = game.visualAuditSkins().stream()
                .filter(entry -> entry.bird() == BirdGame3.BirdType.GRINCHHAWK)
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
                    assertTrue(Double.isFinite(geometry.grinchhawkFootBaseline()),
                            label + " did not report a grounded talon baseline");
                    assertEquals(80.0, geometry.grinchhawkFootBaseline(), 0.15,
                            label + " must place the visible talon edge on the 80-unit collision floor");
                }
            }
        }
    }

    @Test
    void grinchhawkHeadEyeAndHookedBillFollowMovementAnimationsInBothDirections() {
        BirdGame3 game = freshGame();
        List<BirdGame3.VisualAuditSkin> entries = game.visualAuditSkins().stream()
                .filter(entry -> entry.bird() == BirdGame3.BirdType.GRINCHHAWK)
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
                        label + " must aim its hooked bill upward during flight");
                assertTrue(beakVectorX(hit) * beakVectorX(idle) < 0.0,
                        label + " must turn its head and bill backward during hitstun");
                assertFeatureGeometryIsSafe(idle, label + " idle");
                assertFeatureGeometryIsSafe(flap, label + " flap");
                assertFeatureGeometryIsSafe(hit, label + " hit");
            }
        }
    }

    @Test
    void grinchhawkChimneyFlapOpensBothWingsThenClosesThemBeforeLanding() {
        BirdGame3 game = freshGame();
        List<BirdGame3.VisualAuditSkin> entries = game.visualAuditSkins().stream()
                .filter(entry -> entry.bird() == BirdGame3.BirdType.GRINCHHAWK)
                .filter(entry -> BirdSpriteLibrary.sheetFor(entry.bird(), entry.key()) == null)
                .toList();

        for (BirdGame3.VisualAuditSkin entry : entries) {
            for (boolean facingRight : List.of(true, false)) {
                Bird.VisualFeatureGeometry folded = game.inspectVisualAuditGrinchhawkChimneyFlapFeatures(
                        entry, Bird.GRINCH_CHIMNEY_FLAP_FRAMES, facingRight);
                Bird.VisualFeatureGeometry spread = game.inspectVisualAuditGrinchhawkChimneyFlapFeatures(
                        entry, Bird.GRINCH_CHIMNEY_FLAP_FRAMES - 4, facingRight);
                Bird.VisualFeatureGeometry closing = game.inspectVisualAuditGrinchhawkChimneyFlapFeatures(
                        entry, 1, facingRight);
                String label = entry.name() + " Chimney Flap facing "
                        + (facingRight ? "right" : "left");

                assertTrue(folded.grinchhawkWingOpenness() <= 0.01,
                        label + " must begin with folded wings");
                assertTrue(spread.grinchhawkWingOpenness() >= 0.95,
                        label + " must reach a fully spread two-wing climbing pose");
                assertEquals(2, spread.bodyPartCount(Bird.VisualBodyPart.GRINCHHAWK_WING),
                        label + " must display both wings during Chimney Flap");
                assertTrue(closing.grinchhawkWingOpenness() <= 0.17,
                        label + " must close both wings before Chimney Flap ends");
            }
        }
    }

    @Test
    void vultureKeepsBroadWingsTailTalonsAndRuffAcrossSkinsPosesAndDirections() {
        BirdGame3 game = freshGame();
        List<BirdGame3.VisualAuditSkin> entries = game.visualAuditSkins().stream()
                .filter(entry -> entry.bird() == BirdGame3.BirdType.VULTURE)
                .filter(entry -> !"NULL_ROCK_VULTURE".equals(entry.key()))
                .filter(entry -> BirdSpriteLibrary.sheetFor(entry.bird(), entry.key()) == null)
                .toList();

        for (BirdGame3.VisualAuditSkin entry : entries) {
            for (Bird.VisualAuditPose pose : Bird.VisualAuditPose.values()) {
                for (boolean facingRight : List.of(true, false)) {
                    Bird.VisualFeatureGeometry geometry =
                            game.inspectVisualAuditCombatFeatures(entry, pose, facingRight);
                    String label = entry.name() + " / " + pose + " facing "
                            + (facingRight ? "right" : "left");
                    assertEquals(2, geometry.bodyPartCount(Bird.VisualBodyPart.VULTURE_WING),
                            label + " must draw both broad soaring wings");
                    assertEquals(4, geometry.bodyPartCount(Bird.VisualBodyPart.VULTURE_TAIL_FEATHER),
                            label + " must retain all four ragged tail feathers");
                    assertEquals(2, geometry.bodyPartCount(Bird.VisualBodyPart.VULTURE_LEG),
                            label + " must retain both taloned legs");
                    assertEquals(7, geometry.bodyPartCount(Bird.VisualBodyPart.VULTURE_RUFF_FEATHER),
                            label + " must retain the full pale neck ruff");
                    assertFeatureGeometryIsSafe(geometry, label);
                }
            }
        }
    }

    @Test
    void vultureGroundedTalonsMeetTheGameplayFloorAcrossSkinsAndDirections() {
        BirdGame3 game = freshGame();
        List<BirdGame3.VisualAuditSkin> entries = game.visualAuditSkins().stream()
                .filter(entry -> entry.bird() == BirdGame3.BirdType.VULTURE)
                .filter(entry -> !"NULL_ROCK_VULTURE".equals(entry.key()))
                .filter(entry -> BirdSpriteLibrary.sheetFor(entry.bird(), entry.key()) == null)
                .toList();

        for (BirdGame3.VisualAuditSkin entry : entries) {
            for (Bird.VisualAuditPose pose : List.of(Bird.VisualAuditPose.IDLE, Bird.VisualAuditPose.RUN)) {
                for (boolean facingRight : List.of(true, false)) {
                    Bird.VisualFeatureGeometry geometry =
                            game.inspectVisualAuditCombatFeatures(entry, pose, facingRight);
                    String label = entry.name() + " / " + pose + " facing "
                            + (facingRight ? "right" : "left");
                    assertTrue(Double.isFinite(geometry.vultureFootBaseline()),
                            label + " did not report a grounded talon baseline");
                    assertEquals(80.0, geometry.vultureFootBaseline(), 0.15,
                            label + " must place the visible talon edge on the 80-unit collision floor");
                }
            }
        }
    }

    @Test
    void vultureHeadEyeAndHookedBillFollowMovementAnimationsInBothDirections() {
        BirdGame3 game = freshGame();
        List<BirdGame3.VisualAuditSkin> entries = game.visualAuditSkins().stream()
                .filter(entry -> entry.bird() == BirdGame3.BirdType.VULTURE)
                .filter(entry -> !"NULL_ROCK_VULTURE".equals(entry.key()))
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
                        label + " must aim its hooked bill upward during flight");
                assertTrue(beakVectorX(hit) * beakVectorX(idle) < 0.0,
                        label + " must turn its head and bill backward during hitstun");
                assertFeatureGeometryIsSafe(idle, label + " idle");
                assertFeatureGeometryIsSafe(flap, label + " flap");
                assertFeatureGeometryIsSafe(hit, label + " hit");
            }
        }
    }

    @Test
    void vultureCarrionGlideOpensBothWingsThenFoldsThemBeforeTheRushEnds() {
        BirdGame3 game = freshGame();
        List<BirdGame3.VisualAuditSkin> entries = game.visualAuditSkins().stream()
                .filter(entry -> entry.bird() == BirdGame3.BirdType.VULTURE)
                .filter(entry -> !"NULL_ROCK_VULTURE".equals(entry.key()))
                .filter(entry -> BirdSpriteLibrary.sheetFor(entry.bird(), entry.key()) == null)
                .toList();

        for (BirdGame3.VisualAuditSkin entry : entries) {
            for (boolean facingRight : List.of(true, false)) {
                Bird.VisualFeatureGeometry folded = game.inspectVisualAuditVultureGlideFeatures(
                        entry, Bird.VULTURE_GLIDE_FRAMES, facingRight);
                Bird.VisualFeatureGeometry spread = game.inspectVisualAuditVultureGlideFeatures(
                        entry, Bird.VULTURE_GLIDE_FRAMES - 4, facingRight);
                Bird.VisualFeatureGeometry closing = game.inspectVisualAuditVultureGlideFeatures(
                        entry, 1, facingRight);
                String label = entry.name() + " Carrion Glide facing "
                        + (facingRight ? "right" : "left");

                assertTrue(folded.vultureWingOpenness() <= 0.01,
                        label + " must begin with folded wings");
                assertTrue(spread.vultureWingOpenness() >= 0.95,
                        label + " must reach a fully spread two-wing glide");
                assertEquals(2, spread.bodyPartCount(Bird.VisualBodyPart.VULTURE_WING),
                        label + " must display both wings during Carrion Glide");
                assertTrue(closing.vultureWingOpenness() <= 0.17,
                        label + " must fold both wings before Carrion Glide ends");
            }
        }
    }

    @Test
    void vultureTorsoAndCrowMarksMirrorCleanlyAndStayAttachedToTheBody() {
        BirdGame3 game = freshGame();
        List<BirdGame3.VisualAuditSkin> entries = game.visualAuditSkins().stream()
                .filter(entry -> entry.bird() == BirdGame3.BirdType.VULTURE)
                .filter(entry -> !"NULL_ROCK_VULTURE".equals(entry.key()))
                .filter(entry -> BirdSpriteLibrary.sheetFor(entry.bird(), entry.key()) == null)
                .toList();

        for (BirdGame3.VisualAuditSkin entry : entries) {
            for (Bird.VisualAuditPose pose : Bird.VisualAuditPose.values()) {
                Bird.VisualFeatureGeometry right =
                        game.inspectVisualAuditCombatFeatures(entry, pose, true);
                Bird.VisualFeatureGeometry left =
                        game.inspectVisualAuditCombatFeatures(entry, pose, false);
                String label = entry.name() + " / " + pose;

                assertMirrored(right, left, label);
                assertEquals(2, right.bodyPartCount(Bird.VisualBodyPart.VULTURE_CROW_MARK),
                        label + " must draw two crow-charge marks facing right");
                assertEquals(2, left.bodyPartCount(Bird.VisualBodyPart.VULTURE_CROW_MARK),
                        label + " must draw two crow-charge marks facing left");
                assertTrue(right.vultureTorso() != null && left.vultureTorso() != null,
                        label + " did not report the authored torso bounds");
                assertTrue(right.vultureTorso().contains(right.vultureCrowMarks(), 0.01),
                        label + " places a right-facing crow mark outside the torso");
                assertTrue(left.vultureTorso().contains(left.vultureCrowMarks(), 0.01),
                        label + " places a left-facing crow mark outside the torso");

                double centerX = (right.vultureTorso().left() + right.vultureTorso().right()) * 0.5;
                assertEquals(centerX * 2.0,
                        right.vultureCrowMarks().left() + left.vultureCrowMarks().right(), 0.01,
                        label + " does not mirror the rear edge of the crow markings");
                assertEquals(centerX * 2.0,
                        right.vultureCrowMarks().right() + left.vultureCrowMarks().left(), 0.01,
                        label + " does not mirror the front edge of the crow markings");
                assertEquals(right.vultureCrowMarks().top(), left.vultureCrowMarks().top(), 0.01,
                        label + " changes crow-mark height when turning around");
                assertEquals(right.vultureCrowMarks().bottom(), left.vultureCrowMarks().bottom(), 0.01,
                        label + " changes crow-mark height when turning around");
            }
        }
    }

    @Test
    void opiumBirdKeepsVaporTailPetalWingsTalonsCrestAndCharmAcrossEveryPose() {
        BirdGame3 game = freshGame();
        List<BirdGame3.VisualAuditSkin> entries = game.visualAuditSkins().stream()
                .filter(entry -> entry.bird() == BirdGame3.BirdType.OPIUMBIRD)
                .filter(entry -> BirdSpriteLibrary.sheetFor(entry.bird(), entry.key()) == null)
                .toList();

        for (BirdGame3.VisualAuditSkin entry : entries) {
            for (Bird.VisualAuditPose pose : Bird.VisualAuditPose.values()) {
                Bird.VisualFeatureGeometry right =
                        game.inspectVisualAuditCombatFeatures(entry, pose, true);
                Bird.VisualFeatureGeometry left =
                        game.inspectVisualAuditCombatFeatures(entry, pose, false);
                String label = entry.name() + " / " + pose;

                for (Bird.VisualFeatureGeometry geometry : List.of(right, left)) {
                    assertEquals(3, geometry.bodyPartCount(Bird.VisualBodyPart.OPIUM_TAIL_FEATHER),
                            label + " must retain all three vapor-tail feathers");
                    assertEquals(2, geometry.bodyPartCount(Bird.VisualBodyPart.OPIUM_WING),
                            label + " must retain both petal wings");
                    assertEquals(2, geometry.bodyPartCount(Bird.VisualBodyPart.OPIUM_LEG),
                            label + " must retain both legs");
                    assertEquals(3, geometry.bodyPartCount(Bird.VisualBodyPart.OPIUM_CREST_PETAL),
                            label + " must retain all three sleepy crown petals");
                    assertEquals(1, geometry.bodyPartCount(Bird.VisualBodyPart.OPIUM_CHARM),
                            label + " must retain the flank charm");
                    assertTrue(geometry.opiumTorso() != null,
                            label + " did not report the authored torso");
                    assertTrue(geometry.opiumTorso().contains(geometry.opiumCharm(), 0.01),
                            label + " lets the flank charm float outside the torso");
                    assertFeatureGeometryIsSafe(geometry, label);
                }

                assertMirrored(right, left, label);
                double centerX = (right.opiumTorso().left() + right.opiumTorso().right()) * 0.5;
                assertEquals(centerX * 2.0,
                        right.opiumCharm().left() + left.opiumCharm().right(), 0.01,
                        label + " does not mirror the charm's rear edge");
                assertEquals(centerX * 2.0,
                        right.opiumCharm().right() + left.opiumCharm().left(), 0.01,
                        label + " does not mirror the charm's front edge");
            }
        }
    }

    @Test
    void opiumBirdGroundedTalonsMeetTheGameplayFloorAcrossSkinsAndDirections() {
        BirdGame3 game = freshGame();
        List<BirdGame3.VisualAuditSkin> entries = game.visualAuditSkins().stream()
                .filter(entry -> entry.bird() == BirdGame3.BirdType.OPIUMBIRD)
                .filter(entry -> BirdSpriteLibrary.sheetFor(entry.bird(), entry.key()) == null)
                .toList();

        for (BirdGame3.VisualAuditSkin entry : entries) {
            for (Bird.VisualAuditPose pose : List.of(Bird.VisualAuditPose.IDLE, Bird.VisualAuditPose.RUN)) {
                for (boolean facingRight : List.of(true, false)) {
                    Bird.VisualFeatureGeometry geometry =
                            game.inspectVisualAuditCombatFeatures(entry, pose, facingRight);
                    String label = entry.name() + " / " + pose + " facing "
                            + (facingRight ? "right" : "left");
                    assertTrue(Double.isFinite(geometry.opiumFootBaseline()),
                            label + " did not report a grounded foot baseline");
                    assertEquals(80.0, geometry.opiumFootBaseline(), 0.15,
                            label + " must place the talons on the 80-unit collision floor");
                }
            }
        }
    }

    @Test
    void opiumBirdFaceAndBillFollowFlightAndHitAnimationsWithoutObstruction() {
        BirdGame3 game = freshGame();
        List<BirdGame3.VisualAuditSkin> entries = game.visualAuditSkins().stream()
                .filter(entry -> entry.bird() == BirdGame3.BirdType.OPIUMBIRD)
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

                assertTrue(Math.abs(beakVectorY(flap) - beakVectorY(idle)) >= 16.0,
                        label + " must aim its bill upward during flight");
                assertTrue(beakVectorX(hit) * beakVectorX(idle) < 0.0,
                        label + " must turn its head and bill backward during hitstun");
                assertFeatureGeometryIsSafe(idle, label + " idle");
                assertFeatureGeometryIsSafe(flap, label + " flap");
                assertFeatureGeometryIsSafe(hit, label + " hit");
            }
        }
    }

    @Test
    void opiumBirdSpecialsUseAuthoredWingGesturesAndCompleteOpenCloseCycles() {
        BirdGame3 game = freshGame();
        List<BirdGame3.VisualAuditSkin> entries = game.visualAuditSkins().stream()
                .filter(entry -> entry.bird() == BirdGame3.BirdType.OPIUMBIRD)
                .filter(entry -> BirdSpriteLibrary.sheetFor(entry.bird(), entry.key()) == null)
                .toList();

        for (BirdGame3.VisualAuditSkin entry : entries) {
            for (boolean facingRight : List.of(true, false)) {
                String label = entry.name() + " facing " + (facingRight ? "right" : "left");
                Bird.VisualFeatureGeometry idle = game.inspectVisualAuditCombatFeatures(
                        entry, Bird.VisualAuditPose.IDLE, facingRight);
                Bird.VisualFeatureGeometry neutral = game.inspectVisualAuditOpiumActionFeatures(
                        entry, Bird.VisualAuditOpiumAction.NEUTRAL, 45, facingRight);
                Bird.VisualFeatureGeometry sideStart = game.inspectVisualAuditOpiumActionFeatures(
                        entry, Bird.VisualAuditOpiumAction.SIDE, Bird.OPIUM_SIDE_FRAMES, facingRight);
                Bird.VisualFeatureGeometry sideSpread = game.inspectVisualAuditOpiumActionFeatures(
                        entry, Bird.VisualAuditOpiumAction.SIDE, Bird.OPIUM_SIDE_FRAMES - 4, facingRight);
                Bird.VisualFeatureGeometry sideClosing = game.inspectVisualAuditOpiumActionFeatures(
                        entry, Bird.VisualAuditOpiumAction.SIDE, 1, facingRight);
                Bird.VisualFeatureGeometry upSpread = game.inspectVisualAuditOpiumActionFeatures(
                        entry, Bird.VisualAuditOpiumAction.UP, Bird.OPIUM_UP_FRAMES - 4, facingRight);
                Bird.VisualFeatureGeometry down = game.inspectVisualAuditOpiumActionFeatures(
                        entry, Bird.VisualAuditOpiumAction.DOWN, 37, facingRight);
                Bird.VisualFeatureGeometry ultimate = game.inspectVisualAuditOpiumActionFeatures(
                        entry, Bird.VisualAuditOpiumAction.ULTIMATE, Bird.OPIUM_ULTIMATE_FRAMES - 20, facingRight);

                assertTrue(idle.opiumWingOpenness() <= 0.07,
                        label + " must keep relaxed idle wings folded");
                assertTrue(neutral.opiumWingOpenness() >= 0.72,
                        label + " must bloom both wings while casting Lean Cloud");
                assertTrue(sideStart.opiumWingOpenness() <= 0.13,
                        label + " must begin Haze Drift with folded wings");
                assertTrue(sideSpread.opiumWingOpenness() >= 0.72,
                        label + " must spread both wings during Haze Drift");
                assertTrue(sideClosing.opiumWingOpenness() <= 0.24,
                        label + " must fold both wings before Haze Drift ends");
                assertTrue(upSpread.opiumWingOpenness() >= 0.92,
                        label + " must fully open both wings during Rising Vapors");
                assertTrue(down.opiumWingOpenness() >= 0.57,
                        label + " must gesture with both wings while planting Lotus Patch");
                assertTrue(ultimate.opiumWingOpenness() >= 0.84,
                        label + " must hold both wings open during Purple Haze");
                for (Bird.VisualFeatureGeometry geometry :
                        List.of(neutral, sideStart, sideSpread, sideClosing, upSpread, down, ultimate)) {
                    assertEquals(2, geometry.bodyPartCount(Bird.VisualBodyPart.OPIUM_WING),
                            label + " special pose lost a wing");
                    assertFeatureGeometryIsSafe(geometry, label + " special pose");
                }
            }
        }
    }

    @Test
    void titmouseKeepsTailWingsTalonsCrestAndAttachedFlankAcrossEverySkinAndPose() {
        BirdGame3 game = freshGame();
        List<BirdGame3.VisualAuditSkin> entries = game.visualAuditSkins().stream()
                .filter(entry -> entry.bird() == BirdGame3.BirdType.TITMOUSE)
                .filter(entry -> BirdSpriteLibrary.sheetFor(entry.bird(), entry.key()) == null)
                .toList();

        for (BirdGame3.VisualAuditSkin entry : entries) {
            for (Bird.VisualAuditPose pose : Bird.VisualAuditPose.values()) {
                Bird.VisualFeatureGeometry right =
                        game.inspectVisualAuditCombatFeatures(entry, pose, true);
                Bird.VisualFeatureGeometry left =
                        game.inspectVisualAuditCombatFeatures(entry, pose, false);
                String label = entry.name() + " / " + pose;

                for (Bird.VisualFeatureGeometry geometry : List.of(right, left)) {
                    assertEquals(3, geometry.bodyPartCount(Bird.VisualBodyPart.TITMOUSE_TAIL_FEATHER),
                            label + " must retain all three long tail feathers");
                    assertEquals(2, geometry.bodyPartCount(Bird.VisualBodyPart.TITMOUSE_WING),
                            label + " must retain both articulated wings");
                    assertEquals(2, geometry.bodyPartCount(Bird.VisualBodyPart.TITMOUSE_LEG),
                            label + " must retain both legs");
                    assertEquals(3, geometry.bodyPartCount(Bird.VisualBodyPart.TITMOUSE_CREST_FEATHER),
                            label + " must retain all three crown feathers");
                    assertEquals(1, geometry.bodyPartCount(Bird.VisualBodyPart.TITMOUSE_FLANK_PATCH),
                            label + " must draw one species-defining flank patch");
                    assertTrue(geometry.titmouseTorso() != null,
                            label + " did not report the authored torso");
                    assertTrue(geometry.titmouseTorso().contains(geometry.titmouseFlankPatch(), 0.01),
                            label + " lets the flank patch float outside the torso");
                    assertFeatureGeometryIsSafe(geometry, label);
                }

                assertMirrored(right, left, label);
                double centerX = (right.titmouseTorso().left() + right.titmouseTorso().right()) * 0.5;
                assertEquals(centerX * 2.0,
                        right.titmouseFlankPatch().left() + left.titmouseFlankPatch().right(), 0.01,
                        label + " does not mirror the flank patch's rear edge");
                assertEquals(centerX * 2.0,
                        right.titmouseFlankPatch().right() + left.titmouseFlankPatch().left(), 0.01,
                        label + " does not mirror the flank patch's front edge");
            }
        }
    }

    @Test
    void titmouseGroundedTalonsMeetTheGameplayFloorAcrossSkinsAndDirections() {
        BirdGame3 game = freshGame();
        List<BirdGame3.VisualAuditSkin> entries = game.visualAuditSkins().stream()
                .filter(entry -> entry.bird() == BirdGame3.BirdType.TITMOUSE)
                .filter(entry -> BirdSpriteLibrary.sheetFor(entry.bird(), entry.key()) == null)
                .toList();

        for (BirdGame3.VisualAuditSkin entry : entries) {
            for (Bird.VisualAuditPose pose : List.of(Bird.VisualAuditPose.IDLE, Bird.VisualAuditPose.RUN)) {
                for (boolean facingRight : List.of(true, false)) {
                    Bird.VisualFeatureGeometry geometry =
                            game.inspectVisualAuditCombatFeatures(entry, pose, facingRight);
                    String label = entry.name() + " / " + pose + " facing "
                            + (facingRight ? "right" : "left");
                    assertTrue(Double.isFinite(geometry.titmouseFootBaseline()),
                            label + " did not report a grounded foot baseline");
                    assertEquals(80.0, geometry.titmouseFootBaseline(), 0.15,
                            label + " must place both feet on the 80-unit collision floor");
                }
            }
        }
    }

    @Test
    void titmouseFaceAndShortBillFollowFlightAndHitAnimationsWithoutObstruction() {
        BirdGame3 game = freshGame();
        List<BirdGame3.VisualAuditSkin> entries = game.visualAuditSkins().stream()
                .filter(entry -> entry.bird() == BirdGame3.BirdType.TITMOUSE)
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

                assertTrue(Math.abs(beakVectorY(flap) - beakVectorY(idle)) >= 13.0,
                        label + " must aim its bill upward during flight");
                assertTrue(beakVectorX(hit) * beakVectorX(idle) < 0.0,
                        label + " must turn its head and bill backward during hitstun");
                assertFeatureGeometryIsSafe(idle, label + " idle");
                assertFeatureGeometryIsSafe(flap, label + " flap");
                assertFeatureGeometryIsSafe(hit, label + " hit");
            }
        }
    }

    @Test
    void titmouseSpecialsUseAuthoredWingGesturesAndCompleteOpenCloseCycles() {
        BirdGame3 game = freshGame();
        List<BirdGame3.VisualAuditSkin> entries = game.visualAuditSkins().stream()
                .filter(entry -> entry.bird() == BirdGame3.BirdType.TITMOUSE)
                .filter(entry -> BirdSpriteLibrary.sheetFor(entry.bird(), entry.key()) == null)
                .toList();

        for (BirdGame3.VisualAuditSkin entry : entries) {
            for (boolean facingRight : List.of(true, false)) {
                String label = entry.name() + " facing " + (facingRight ? "right" : "left");
                Bird.VisualFeatureGeometry idle = game.inspectVisualAuditCombatFeatures(
                        entry, Bird.VisualAuditPose.IDLE, facingRight);
                Bird.VisualFeatureGeometry neutral = game.inspectVisualAuditTitmouseActionFeatures(
                        entry, Bird.VisualAuditTitmouseAction.NEUTRAL, 7, facingRight);
                Bird.VisualFeatureGeometry sideStart = game.inspectVisualAuditTitmouseActionFeatures(
                        entry, Bird.VisualAuditTitmouseAction.SIDE, Bird.TITMOUSE_BARKSKIP_FRAMES, facingRight);
                Bird.VisualFeatureGeometry sideSpread = game.inspectVisualAuditTitmouseActionFeatures(
                        entry, Bird.VisualAuditTitmouseAction.SIDE, Bird.TITMOUSE_BARKSKIP_FRAMES - 4, facingRight);
                Bird.VisualFeatureGeometry sideClosing = game.inspectVisualAuditTitmouseActionFeatures(
                        entry, Bird.VisualAuditTitmouseAction.SIDE, 1, facingRight);
                Bird.VisualFeatureGeometry upSpread = game.inspectVisualAuditTitmouseActionFeatures(
                        entry, Bird.VisualAuditTitmouseAction.UP, Bird.TITMOUSE_VAULT_FRAMES - 4, facingRight);
                Bird.VisualFeatureGeometry down = game.inspectVisualAuditTitmouseActionFeatures(
                        entry, Bird.VisualAuditTitmouseAction.DOWN, 14, facingRight);
                Bird.VisualFeatureGeometry ultimate = game.inspectVisualAuditTitmouseActionFeatures(
                        entry, Bird.VisualAuditTitmouseAction.ULTIMATE, 6, facingRight);

                assertTrue(idle.titmouseWingOpenness() <= 0.07,
                        label + " must keep relaxed idle wings folded");
                assertTrue(neutral.titmouseWingOpenness() >= 0.72,
                        label + " must spread both wings while calling Scold Chorus");
                assertTrue(sideStart.titmouseWingOpenness() <= 0.10,
                        label + " must begin Barkskip with folded wings");
                assertTrue(sideSpread.titmouseWingOpenness() >= 0.76,
                        label + " must spread both wings during Barkskip");
                assertTrue(sideClosing.titmouseWingOpenness() <= 0.22,
                        label + " must fold both wings before Barkskip ends");
                assertTrue(upSpread.titmouseWingOpenness() >= 0.96,
                        label + " must fully open both wings during Tuft Vault");
                assertTrue(down.titmouseWingOpenness() >= 0.54,
                        label + " must gesture with both wings while arming Seed Stash");
                assertTrue(ultimate.titmouseWingOpenness() >= 0.88,
                        label + " must hold both wings open during Mobbing Run");
                for (Bird.VisualFeatureGeometry geometry :
                        List.of(neutral, sideStart, sideSpread, sideClosing, upSpread, down, ultimate)) {
                    assertEquals(2, geometry.bodyPartCount(Bird.VisualBodyPart.TITMOUSE_WING),
                            label + " special pose lost a wing");
                    assertFeatureGeometryIsSafe(geometry, label + " special pose");
                }
            }
        }
    }

    @Test
    void batKeepsBothWingsEarsLegsTailAndFangsAcrossEverySkinPoseAndDirection() {
        BirdGame3 game = freshGame();
        List<BirdGame3.VisualAuditSkin> entries = game.visualAuditSkins().stream()
                .filter(entry -> entry.bird() == BirdGame3.BirdType.BAT)
                .filter(entry -> BirdSpriteLibrary.sheetFor(entry.bird(), entry.key()) == null)
                .toList();

        for (BirdGame3.VisualAuditSkin entry : entries) {
            for (Bird.VisualAuditPose pose : Bird.VisualAuditPose.values()) {
                Bird.VisualFeatureGeometry right = game.inspectVisualAuditCombatFeatures(entry, pose, true);
                Bird.VisualFeatureGeometry left = game.inspectVisualAuditCombatFeatures(entry, pose, false);
                String label = entry.name() + " / " + pose;
                for (Bird.VisualFeatureGeometry geometry : List.of(right, left)) {
                    assertEquals(2, geometry.bodyPartCount(Bird.VisualBodyPart.BAT_WING),
                            label + " must retain both articulated wings");
                    assertEquals(2, geometry.bodyPartCount(Bird.VisualBodyPart.BAT_EAR),
                            label + " must retain both rooted ears");
                    assertEquals(2, geometry.bodyPartCount(Bird.VisualBodyPart.BAT_LEG),
                            label + " must retain both legs");
                    assertEquals(1, geometry.bodyPartCount(Bird.VisualBodyPart.BAT_TAIL_MEMBRANE),
                            label + " must retain the tail membrane");
                    assertEquals(2, geometry.bodyPartCount(Bird.VisualBodyPart.BAT_FANG),
                            label + " must retain both fangs");
                    assertTrue(geometry.batTorso() != null, label + " did not report the authored torso");
                    assertFeatureGeometryIsSafe(geometry, label);
                }
                assertMirrored(right, left, label);
            }
        }
    }

    @Test
    void batFeetMeetTheFloorAndHangingClawsMeetTheCeilingPlane() {
        BirdGame3 game = freshGame();
        List<BirdGame3.VisualAuditSkin> entries = game.visualAuditSkins().stream()
                .filter(entry -> entry.bird() == BirdGame3.BirdType.BAT)
                .filter(entry -> BirdSpriteLibrary.sheetFor(entry.bird(), entry.key()) == null)
                .toList();

        for (BirdGame3.VisualAuditSkin entry : entries) {
            for (boolean facingRight : List.of(true, false)) {
                Bird.VisualFeatureGeometry idle = game.inspectVisualAuditCombatFeatures(
                        entry, Bird.VisualAuditPose.IDLE, facingRight);
                Bird.VisualFeatureGeometry hanging = game.inspectVisualAuditBatActionFeatures(
                        entry, Bird.VisualAuditBatAction.HANG, 1, facingRight);
                String label = entry.name() + " facing " + (facingRight ? "right" : "left");
                assertEquals(80.0, idle.batFootBaseline(), 0.15,
                        label + " must place both grounded claws on the collision floor");
                assertEquals(0.0, hanging.batCeilingClawBaseline(), 0.15,
                        label + " must place both hanging claws on the ceiling plane");
                assertTrue(hanging.batWingOpenness() <= 0.07,
                        label + " must wrap its wings while hanging");
            }
        }
    }

    @Test
    void batFaceTracksFlightAndHitDirectionsWithoutEnteringTheMuzzle() {
        BirdGame3 game = freshGame();
        List<BirdGame3.VisualAuditSkin> entries = game.visualAuditSkins().stream()
                .filter(entry -> entry.bird() == BirdGame3.BirdType.BAT)
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
                assertTrue(Math.abs(beakVectorY(flap)) > Math.abs(beakVectorX(flap)) * 2.0,
                        label + " must aim its muzzle upward in flight");
                assertTrue(beakVectorX(hit) * beakVectorX(idle) < 0.0,
                        label + " must turn its muzzle backward in hitstun");
                assertFeatureGeometryIsSafe(idle, label + " idle");
                assertFeatureGeometryIsSafe(flap, label + " flap");
                assertFeatureGeometryIsSafe(hit, label + " hit");
            }
        }
    }

    @Test
    void batSpecialsUseDistinctCompleteWingCycles() {
        BirdGame3 game = freshGame();
        List<BirdGame3.VisualAuditSkin> entries = game.visualAuditSkins().stream()
                .filter(entry -> entry.bird() == BirdGame3.BirdType.BAT)
                .filter(entry -> BirdSpriteLibrary.sheetFor(entry.bird(), entry.key()) == null)
                .toList();

        for (BirdGame3.VisualAuditSkin entry : entries) {
            for (boolean facingRight : List.of(true, false)) {
                String label = entry.name() + " facing " + (facingRight ? "right" : "left");
                Bird.VisualFeatureGeometry idle = game.inspectVisualAuditCombatFeatures(
                        entry, Bird.VisualAuditPose.IDLE, facingRight);
                Bird.VisualFeatureGeometry echo = game.inspectVisualAuditBatActionFeatures(
                        entry, Bird.VisualAuditBatAction.NEUTRAL, 10, facingRight);
                Bird.VisualFeatureGeometry sideStart = game.inspectVisualAuditBatActionFeatures(
                        entry, Bird.VisualAuditBatAction.SIDE, Bird.BAT_WINGCUT_FRAMES, facingRight);
                Bird.VisualFeatureGeometry sideOpen = game.inspectVisualAuditBatActionFeatures(
                        entry, Bird.VisualAuditBatAction.SIDE, Bird.BAT_WINGCUT_FRAMES - 4, facingRight);
                Bird.VisualFeatureGeometry sideClose = game.inspectVisualAuditBatActionFeatures(
                        entry, Bird.VisualAuditBatAction.SIDE, 1, facingRight);
                Bird.VisualFeatureGeometry moonrise = game.inspectVisualAuditBatActionFeatures(
                        entry, Bird.VisualAuditBatAction.UP, Bird.BAT_MOONRISE_FRAMES - 5, facingRight);
                Bird.VisualFeatureGeometry stall = game.inspectVisualAuditBatActionFeatures(
                        entry, Bird.VisualAuditBatAction.DOWN_STALL, 5, facingRight);
                Bird.VisualFeatureGeometry dive = game.inspectVisualAuditBatActionFeatures(
                        entry, Bird.VisualAuditBatAction.DOWN_DIVE, 12, facingRight);
                Bird.VisualFeatureGeometry ultimate = game.inspectVisualAuditBatActionFeatures(
                        entry, Bird.VisualAuditBatAction.ULTIMATE, 80, facingRight);

                assertTrue(idle.batWingOpenness() <= 0.12, label + " must fold both idle wings");
                assertTrue(echo.batWingOpenness() >= 0.62, label + " must cup both wings for Echo Pulse");
                assertTrue(sideStart.batWingOpenness() <= 0.12, label + " must begin Wingcut folded");
                assertTrue(sideOpen.batWingOpenness() >= 0.60, label + " must open both wings during Wingcut");
                assertTrue(sideClose.batWingOpenness() <= 0.12, label + " must close both wings before Wingcut ends");
                assertTrue(moonrise.batWingOpenness() >= 0.80, label + " must fully spread during Moonrise");
                assertTrue(stall.batWingOpenness() >= 0.25 && stall.batWingOpenness() <= 0.32,
                        label + " must hold a restrained inverted stall");
                assertTrue(dive.batWingOpenness() <= 0.14, label + " must tuck both wings for Silent Dive");
                assertTrue(ultimate.batWingOpenness() >= 0.94, label + " must hold both wings open for Cathedral");
                for (Bird.VisualFeatureGeometry geometry :
                        List.of(echo, sideStart, sideOpen, sideClose, moonrise, stall, dive, ultimate)) {
                    assertEquals(2, geometry.bodyPartCount(Bird.VisualBodyPart.BAT_WING),
                            label + " special pose lost a wing");
                    assertFeatureGeometryIsSafe(geometry, label + " special pose");
                }
            }
        }
    }

    @Test
    void pelicanKeepsTailWingsLegsCrestAndAttachedPouchAcrossEverySkinPoseAndDirection() {
        BirdGame3 game = freshGame();
        List<BirdGame3.VisualAuditSkin> entries = game.visualAuditSkins().stream()
                .filter(entry -> entry.bird() == BirdGame3.BirdType.PELICAN)
                .filter(entry -> BirdSpriteLibrary.sheetFor(entry.bird(), entry.key()) == null)
                .toList();

        for (BirdGame3.VisualAuditSkin entry : entries) {
            for (Bird.VisualAuditPose pose : Bird.VisualAuditPose.values()) {
                Bird.VisualFeatureGeometry right = game.inspectVisualAuditCombatFeatures(entry, pose, true);
                Bird.VisualFeatureGeometry left = game.inspectVisualAuditCombatFeatures(entry, pose, false);
                String label = entry.name() + " / " + pose;
                for (Bird.VisualFeatureGeometry geometry : List.of(right, left)) {
                    assertEquals(3, geometry.bodyPartCount(Bird.VisualBodyPart.PELICAN_TAIL_FEATHER),
                            label + " must retain all three tail vanes");
                    assertEquals(2, geometry.bodyPartCount(Bird.VisualBodyPart.PELICAN_WING),
                            label + " must retain both articulated wings");
                    assertEquals(2, geometry.bodyPartCount(Bird.VisualBodyPart.PELICAN_LEG),
                            label + " must retain both legs");
                    assertEquals(3, geometry.bodyPartCount(Bird.VisualBodyPart.PELICAN_CREST_FEATHER),
                            label + " must retain all three rooted crown feathers");
                    assertEquals(1, geometry.bodyPartCount(Bird.VisualBodyPart.PELICAN_POUCH),
                            label + " must draw exactly one attached throat pouch");
                    assertTrue(geometry.pelicanTorso() != null,
                            label + " did not report the authored heavyweight torso");
                    assertTrue(geometry.pelicanPouch() != null,
                            label + " did not report the attached pouch");
                    assertPelicanPouchFollowsBill(geometry, label);
                    assertFeatureGeometryIsSafe(geometry, label);
                }
                assertMirrored(right, left, label);
            }
        }
    }

    @Test
    void pelicanGroundedFeetMeetTheCollisionFloorAcrossEverySkinAndDirection() {
        BirdGame3 game = freshGame();
        List<BirdGame3.VisualAuditSkin> entries = game.visualAuditSkins().stream()
                .filter(entry -> entry.bird() == BirdGame3.BirdType.PELICAN)
                .filter(entry -> BirdSpriteLibrary.sheetFor(entry.bird(), entry.key()) == null)
                .toList();

        for (BirdGame3.VisualAuditSkin entry : entries) {
            for (boolean facingRight : List.of(true, false)) {
                Bird.VisualFeatureGeometry idle = game.inspectVisualAuditCombatFeatures(
                        entry, Bird.VisualAuditPose.IDLE, facingRight);
                assertEquals(80.0, idle.pelicanFootBaseline(), 0.15,
                        entry.name() + " facing " + (facingRight ? "right" : "left")
                                + " must place both webbed feet on the collision floor");
            }
        }
    }

    @Test
    void pelicanFaceAndAttachedPouchTrackFlightHitAndDiveDirections() {
        BirdGame3 game = freshGame();
        List<BirdGame3.VisualAuditSkin> entries = game.visualAuditSkins().stream()
                .filter(entry -> entry.bird() == BirdGame3.BirdType.PELICAN)
                .filter(entry -> BirdSpriteLibrary.sheetFor(entry.bird(), entry.key()) == null)
                .toList();

        for (BirdGame3.VisualAuditSkin entry : entries) {
            for (boolean facingRight : List.of(true, false)) {
                String label = entry.name() + " facing " + (facingRight ? "right" : "left");
                Bird.VisualFeatureGeometry idle = game.inspectVisualAuditCombatFeatures(
                        entry, Bird.VisualAuditPose.IDLE, facingRight);
                Bird.VisualFeatureGeometry flap = game.inspectVisualAuditCombatFeatures(
                        entry, Bird.VisualAuditPose.FLAP, facingRight);
                Bird.VisualFeatureGeometry hit = game.inspectVisualAuditCombatFeatures(
                        entry, Bird.VisualAuditPose.HIT, facingRight);
                Bird.VisualFeatureGeometry dive = game.inspectVisualAuditPelicanActionFeatures(
                        entry, Bird.VisualAuditPelicanAction.UP_DIVE, 10, facingRight);

                assertTrue(Math.abs(beakVectorY(flap)) > Math.abs(beakVectorX(flap)) * 2.0,
                        label + " must aim its bill upward during Thermal Sail");
                assertTrue(beakVectorX(hit) * beakVectorX(idle) < 0.0,
                        label + " must turn its bill away from impact in hitstun");
                assertTrue(beakVectorY(dive) > Math.abs(beakVectorX(dive)) * 2.0,
                        label + " must point its complete bill and pouch down during Keel Dive");
                for (Bird.VisualFeatureGeometry geometry : List.of(idle, flap, hit, dive)) {
                    assertPelicanPouchFollowsBill(geometry, label);
                    assertFeatureGeometryIsSafe(geometry, label);
                }
            }
        }
    }

    @Test
    void pelicanSpecialsUseDistinctCompleteWingAndPouchCycles() {
        BirdGame3 game = freshGame();
        List<BirdGame3.VisualAuditSkin> entries = game.visualAuditSkins().stream()
                .filter(entry -> entry.bird() == BirdGame3.BirdType.PELICAN)
                .filter(entry -> BirdSpriteLibrary.sheetFor(entry.bird(), entry.key()) == null)
                .toList();

        for (BirdGame3.VisualAuditSkin entry : entries) {
            for (boolean facingRight : List.of(true, false)) {
                String label = entry.name() + " facing " + (facingRight ? "right" : "left");
                Bird.VisualFeatureGeometry idle = game.inspectVisualAuditCombatFeatures(
                        entry, Bird.VisualAuditPose.IDLE, facingRight);
                Bird.VisualFeatureGeometry snare = game.inspectVisualAuditPelicanActionFeatures(
                        entry, Bird.VisualAuditPelicanAction.NEUTRAL, 8, facingRight);
                Bird.VisualFeatureGeometry sideStart = game.inspectVisualAuditPelicanActionFeatures(
                        entry, Bird.VisualAuditPelicanAction.SIDE, Bird.PELICAN_SIDE_FRAMES, facingRight);
                Bird.VisualFeatureGeometry sideOpen = game.inspectVisualAuditPelicanActionFeatures(
                        entry, Bird.VisualAuditPelicanAction.SIDE, Bird.PELICAN_SIDE_FRAMES - 8, facingRight);
                Bird.VisualFeatureGeometry sideClose = game.inspectVisualAuditPelicanActionFeatures(
                        entry, Bird.VisualAuditPelicanAction.SIDE, 1, facingRight);
                Bird.VisualFeatureGeometry sail = game.inspectVisualAuditPelicanActionFeatures(
                        entry, Bird.VisualAuditPelicanAction.UP_ASCENT, Bird.PELICAN_UP_FRAMES - 7, facingRight);
                Bird.VisualFeatureGeometry dive = game.inspectVisualAuditPelicanActionFeatures(
                        entry, Bird.VisualAuditPelicanAction.UP_DIVE, 10, facingRight);
                Bird.VisualFeatureGeometry load = game.inspectVisualAuditPelicanActionFeatures(
                        entry, Bird.VisualAuditPelicanAction.DOWN_LOAD, 16, facingRight);
                Bird.VisualFeatureGeometry bilge = game.inspectVisualAuditPelicanActionFeatures(
                        entry, Bird.VisualAuditPelicanAction.DOWN_BILGE, 9, facingRight);
                Bird.VisualFeatureGeometry ultimate = game.inspectVisualAuditPelicanActionFeatures(
                        entry, Bird.VisualAuditPelicanAction.ULTIMATE, 100, facingRight);

                assertTrue(idle.pelicanWingOpenness() <= 0.09, label + " must fold both idle wings");
                assertTrue(snare.pelicanWingOpenness() >= 0.40, label + " must brace for Pouch Snare");
                assertTrue(snare.pelicanBillGape() > idle.pelicanBillGape() + 6.0,
                        label + " must visibly open the bill for Pouch Snare");
                assertTrue(sideStart.pelicanWingOpenness() <= 0.10,
                        label + " must begin Breakwater Run with folded wings");
                assertTrue(sideOpen.pelicanWingOpenness() >= 0.56,
                        label + " must open both wings during Breakwater Run");
                assertTrue(sideClose.pelicanWingOpenness() <= 0.10,
                        label + " must close both wings before Breakwater Run ends");
                assertTrue(sail.pelicanWingOpenness() >= 0.85,
                        label + " must fully spread both wings during Thermal Sail");
                assertTrue(dive.pelicanWingOpenness() <= 0.12,
                        label + " must tuck both wings for Keel Dive");
                assertTrue(load.pelicanWingOpenness() >= 0.20 && load.pelicanWingOpenness() <= 0.28,
                        label + " must keep a restrained loading stance");
                assertTrue(bilge.pelicanWingOpenness() >= 0.55,
                        label + " must spread both wings while dumping Bilge Command");
                assertTrue(ultimate.pelicanWingOpenness() >= 0.94,
                        label + " must hold both wings open for Maelstrom Gullet");
                for (Bird.VisualFeatureGeometry geometry :
                        List.of(snare, sideStart, sideOpen, sideClose, sail, dive, load, bilge, ultimate)) {
                    assertEquals(2, geometry.bodyPartCount(Bird.VisualBodyPart.PELICAN_WING),
                            label + " special pose lost a wing");
                    assertEquals(1, geometry.bodyPartCount(Bird.VisualBodyPart.PELICAN_POUCH),
                            label + " special pose detached or duplicated the pouch");
                    assertPelicanPouchFollowsBill(geometry, label + " special pose");
                    assertFeatureGeometryIsSafe(geometry, label + " special pose");
                }
            }
        }
    }

    @Test
    void heisenbirdKeepsItsCompleteAttachedAnatomyAcrossEverySkinPoseAndDirection() {
        BirdGame3 game = freshGame();
        List<BirdGame3.VisualAuditSkin> entries = game.visualAuditSkins().stream()
                .filter(entry -> entry.bird() == BirdGame3.BirdType.HEISENBIRD)
                .filter(entry -> BirdSpriteLibrary.sheetFor(entry.bird(), entry.key()) == null)
                .toList();

        for (BirdGame3.VisualAuditSkin entry : entries) {
            for (Bird.VisualAuditPose pose : Bird.VisualAuditPose.values()) {
                Bird.VisualFeatureGeometry right = game.inspectVisualAuditCombatFeatures(entry, pose, true);
                Bird.VisualFeatureGeometry left = game.inspectVisualAuditCombatFeatures(entry, pose, false);
                String label = entry.name() + " / " + pose;
                for (Bird.VisualFeatureGeometry geometry : List.of(right, left)) {
                    assertEquals(3, geometry.bodyPartCount(Bird.VisualBodyPart.HEISEN_TAIL_FEATHER),
                            label + " must retain all three layered tail feathers");
                    assertEquals(2, geometry.bodyPartCount(Bird.VisualBodyPart.HEISEN_WING),
                            label + " must retain both articulated wings");
                    assertEquals(2, geometry.bodyPartCount(Bird.VisualBodyPart.HEISEN_LEG),
                            label + " must retain both legs");
                    assertEquals(1, geometry.bodyPartCount(Bird.VisualBodyPart.HEISEN_HAT_BRIM),
                            label + " must draw one attached hat brim");
                    assertEquals(1, geometry.bodyPartCount(Bird.VisualBodyPart.HEISEN_HAT_CROWN),
                            label + " must draw one attached hat crown");
                    assertEquals(1, geometry.bodyPartCount(Bird.VisualBodyPart.HEISEN_GOATEE),
                            label + " must draw one rooted goatee");
                    assertEquals(3, geometry.bodyPartCount(Bird.VisualBodyPart.HEISEN_FLANK_CRYSTAL),
                            label + " must root all three crystals in the flank");
                    assertTrue(geometry.heisenTorso() != null,
                            label + " did not report the authored torso");
                    assertTrue(geometry.heisenHat() != null,
                            label + " did not report the attached hat");
                    assertTrue(geometry.heisenTear() != null,
                            label + " did not report the face marking");
                    assertTrue(!geometry.heisenTear().contains(geometry.eye(), 0.0),
                            label + " lets the blue face marking cover the eye");
                    assertHeisenHatFollowsHead(geometry, label);
                    assertFeatureGeometryIsSafe(geometry, label);
                }
                assertMirrored(right, left, label);
            }
        }
    }

    @Test
    void heisenbirdGroundedFeetMeetTheCollisionFloorAcrossEverySkinAndDirection() {
        BirdGame3 game = freshGame();
        List<BirdGame3.VisualAuditSkin> entries = game.visualAuditSkins().stream()
                .filter(entry -> entry.bird() == BirdGame3.BirdType.HEISENBIRD)
                .filter(entry -> BirdSpriteLibrary.sheetFor(entry.bird(), entry.key()) == null)
                .toList();

        for (BirdGame3.VisualAuditSkin entry : entries) {
            for (boolean facingRight : List.of(true, false)) {
                Bird.VisualFeatureGeometry idle = game.inspectVisualAuditCombatFeatures(
                        entry, Bird.VisualAuditPose.IDLE, facingRight);
                assertEquals(80.0, idle.heisenFootBaseline(), 0.15,
                        entry.name() + " facing " + (facingRight ? "right" : "left")
                                + " must place both feet on the collision floor");
            }
        }
    }

    @Test
    void heisenbirdFaceHatAndMarkingTrackFlightAndHitDirections() {
        BirdGame3 game = freshGame();
        List<BirdGame3.VisualAuditSkin> entries = game.visualAuditSkins().stream()
                .filter(entry -> entry.bird() == BirdGame3.BirdType.HEISENBIRD)
                .filter(entry -> BirdSpriteLibrary.sheetFor(entry.bird(), entry.key()) == null)
                .toList();

        for (BirdGame3.VisualAuditSkin entry : entries) {
            for (boolean facingRight : List.of(true, false)) {
                String label = entry.name() + " facing " + (facingRight ? "right" : "left");
                Bird.VisualFeatureGeometry idle = game.inspectVisualAuditCombatFeatures(
                        entry, Bird.VisualAuditPose.IDLE, facingRight);
                Bird.VisualFeatureGeometry flap = game.inspectVisualAuditCombatFeatures(
                        entry, Bird.VisualAuditPose.FLAP, facingRight);
                Bird.VisualFeatureGeometry hit = game.inspectVisualAuditCombatFeatures(
                        entry, Bird.VisualAuditPose.HIT, facingRight);

                assertTrue(Math.abs(beakVectorY(flap)) > Math.abs(beakVectorX(flap)) * 2.0,
                        label + " must aim its complete face upward while climbing");
                assertTrue(beakVectorX(hit) * beakVectorX(idle) < 0.0,
                        label + " must turn its complete face away from impact in hitstun");
                for (Bird.VisualFeatureGeometry geometry : List.of(idle, flap, hit)) {
                    assertHeisenHatFollowsHead(geometry, label);
                    assertTrue(!geometry.heisenTear().contains(geometry.eye(), 0.0),
                            label + " lets the blue face marking cover the eye");
                    assertFeatureGeometryIsSafe(geometry, label);
                }
            }
        }
    }

    @Test
    void heisenbirdSpecialsUseDistinctCompleteWingAndBillCycles() {
        BirdGame3 game = freshGame();
        List<BirdGame3.VisualAuditSkin> entries = game.visualAuditSkins().stream()
                .filter(entry -> entry.bird() == BirdGame3.BirdType.HEISENBIRD)
                .filter(entry -> BirdSpriteLibrary.sheetFor(entry.bird(), entry.key()) == null)
                .toList();

        for (BirdGame3.VisualAuditSkin entry : entries) {
            for (boolean facingRight : List.of(true, false)) {
                String label = entry.name() + " facing " + (facingRight ? "right" : "left");
                Bird.VisualFeatureGeometry idle = game.inspectVisualAuditCombatFeatures(
                        entry, Bird.VisualAuditPose.IDLE, facingRight);
                Bird.VisualFeatureGeometry cloud = game.inspectVisualAuditHeisenActionFeatures(
                        entry, Bird.VisualAuditHeisenAction.NEUTRAL,
                        Bird.HEISEN_NEUTRAL_REUSE_FRAMES, facingRight);
                Bird.VisualFeatureGeometry sideStart = game.inspectVisualAuditHeisenActionFeatures(
                        entry, Bird.VisualAuditHeisenAction.SIDE, Bird.HEISEN_SIDE_FRAMES, facingRight);
                Bird.VisualFeatureGeometry sideOpen = game.inspectVisualAuditHeisenActionFeatures(
                        entry, Bird.VisualAuditHeisenAction.SIDE, Bird.HEISEN_SIDE_FRAMES - 7, facingRight);
                Bird.VisualFeatureGeometry sideClose = game.inspectVisualAuditHeisenActionFeatures(
                        entry, Bird.VisualAuditHeisenAction.SIDE, 1, facingRight);
                Bird.VisualFeatureGeometry column = game.inspectVisualAuditHeisenActionFeatures(
                        entry, Bird.VisualAuditHeisenAction.UP, Bird.HEISEN_UP_FRAMES - 6, facingRight);
                Bird.VisualFeatureGeometry node = game.inspectVisualAuditHeisenActionFeatures(
                        entry, Bird.VisualAuditHeisenAction.DOWN,
                        Bird.HEISEN_DOWN_REUSE_FRAMES, facingRight);
                Bird.VisualFeatureGeometry cook = game.inspectVisualAuditHeisenActionFeatures(
                        entry, Bird.VisualAuditHeisenAction.ULTIMATE_ORBIT, 180, facingRight);
                Bird.VisualFeatureGeometry volley = game.inspectVisualAuditHeisenActionFeatures(
                        entry, Bird.VisualAuditHeisenAction.ULTIMATE_VOLLEY, 100, facingRight);

                assertTrue(idle.heisenWingOpenness() <= 0.09, label + " must fold both idle wings");
                assertTrue(cloud.heisenWingOpenness() >= 0.56,
                        label + " must cup both wings while forming Crystal Cloud");
                assertTrue(cloud.heisenBillGape() > idle.heisenBillGape() + 3.0,
                        label + " must visibly open the bill while forming Crystal Cloud");
                assertTrue(sideStart.heisenWingOpenness() <= 0.10,
                        label + " must begin Blue Rush with folded wings");
                assertTrue(sideOpen.heisenWingOpenness() >= 0.70,
                        label + " must spread both wings during Blue Rush");
                assertTrue(sideClose.heisenWingOpenness() <= 0.10,
                        label + " must close both wings before Blue Rush ends");
                assertTrue(column.heisenWingOpenness() >= 0.90,
                        label + " must fully spread both wings during Crystal Column");
                assertTrue(node.heisenWingOpenness() >= 0.33 && node.heisenWingOpenness() <= 0.37,
                        label + " must brace both wings while planting Glass Node");
                assertTrue(cook.heisenWingOpenness() >= 0.94,
                        label + " must hold both wings open while cooking Glass Cook");
                assertTrue(volley.heisenWingOpenness() >= 0.74,
                        label + " must keep both wings spread through the shard volley");
                for (Bird.VisualFeatureGeometry geometry :
                        List.of(cloud, sideStart, sideOpen, sideClose, column, node, cook, volley)) {
                    assertEquals(2, geometry.bodyPartCount(Bird.VisualBodyPart.HEISEN_WING),
                            label + " special pose lost a wing");
                    assertEquals(3, geometry.bodyPartCount(Bird.VisualBodyPart.HEISEN_FLANK_CRYSTAL),
                            label + " special pose detached a flank crystal");
                    assertHeisenHatFollowsHead(geometry, label + " special pose");
                    assertFeatureGeometryIsSafe(geometry, label + " special pose");
                }
            }
        }
    }

    @Test
    void ravenKeepsItsCompleteAttachedAnatomyAcrossEverySkinPoseAndDirection() {
        BirdGame3 game = freshGame();
        List<BirdGame3.VisualAuditSkin> entries = game.visualAuditSkins().stream()
                .filter(entry -> entry.bird() == BirdGame3.BirdType.RAVEN)
                .filter(entry -> BirdSpriteLibrary.sheetFor(entry.bird(), entry.key()) == null)
                .toList();

        for (BirdGame3.VisualAuditSkin entry : entries) {
            for (Bird.VisualAuditPose pose : Bird.VisualAuditPose.values()) {
                Bird.VisualFeatureGeometry right = game.inspectVisualAuditCombatFeatures(entry, pose, true);
                Bird.VisualFeatureGeometry left = game.inspectVisualAuditCombatFeatures(entry, pose, false);
                String label = entry.name() + " / " + pose;
                for (Bird.VisualFeatureGeometry geometry : List.of(right, left)) {
                    assertEquals(3, geometry.bodyPartCount(Bird.VisualBodyPart.RAVEN_TAIL_FEATHER),
                            label + " must retain all three layered tail feathers");
                    assertEquals(2, geometry.bodyPartCount(Bird.VisualBodyPart.RAVEN_WING),
                            label + " must retain both articulated wings");
                    assertEquals(2, geometry.bodyPartCount(Bird.VisualBodyPart.RAVEN_LEG),
                            label + " must retain both legs");
                    assertEquals(3, geometry.bodyPartCount(Bird.VisualBodyPart.RAVEN_CREST_FEATHER),
                            label + " must root all three crest feathers in the skull");
                    assertEquals(4, geometry.bodyPartCount(Bird.VisualBodyPart.RAVEN_THROAT_FEATHER),
                            label + " must retain all four throat hackles");
                    assertTrue(geometry.ravenTorso() != null,
                            label + " did not report the authored torso");
                    if ("VOID_HERALD_RAVEN".equals(entry.key())) {
                        assertTrue(geometry.ravenMask() != null,
                                label + " must draw the promised attached cracked mask");
                        assertTrue(geometry.ravenMask().contains(geometry.eye(), 0.01),
                                label + " lets the mask or its violet eye separate from the skull");
                    } else {
                        assertTrue(geometry.ravenMask() == null,
                                label + " incorrectly gives a non-Herald skin the bone mask");
                    }
                    assertFeatureGeometryIsSafe(geometry, label);
                }
                assertMirrored(right, left, label);
            }
        }
    }

    @Test
    void ravenGroundedFeetMeetTheCollisionFloorAcrossEverySkinAndDirection() {
        BirdGame3 game = freshGame();
        List<BirdGame3.VisualAuditSkin> entries = game.visualAuditSkins().stream()
                .filter(entry -> entry.bird() == BirdGame3.BirdType.RAVEN)
                .filter(entry -> BirdSpriteLibrary.sheetFor(entry.bird(), entry.key()) == null)
                .toList();

        for (BirdGame3.VisualAuditSkin entry : entries) {
            for (boolean facingRight : List.of(true, false)) {
                Bird.VisualFeatureGeometry idle = game.inspectVisualAuditCombatFeatures(
                        entry, Bird.VisualAuditPose.IDLE, facingRight);
                assertEquals(80.0, idle.ravenFootBaseline(), 0.15,
                        entry.name() + " facing " + (facingRight ? "right" : "left")
                                + " must place both feet on the collision floor");
            }
        }
    }

    @Test
    void ravenFaceAndMaskTrackFlightAndHitDirections() {
        BirdGame3 game = freshGame();
        List<BirdGame3.VisualAuditSkin> entries = game.visualAuditSkins().stream()
                .filter(entry -> entry.bird() == BirdGame3.BirdType.RAVEN)
                .filter(entry -> BirdSpriteLibrary.sheetFor(entry.bird(), entry.key()) == null)
                .toList();

        for (BirdGame3.VisualAuditSkin entry : entries) {
            for (boolean facingRight : List.of(true, false)) {
                String label = entry.name() + " facing " + (facingRight ? "right" : "left");
                Bird.VisualFeatureGeometry idle = game.inspectVisualAuditCombatFeatures(
                        entry, Bird.VisualAuditPose.IDLE, facingRight);
                Bird.VisualFeatureGeometry flap = game.inspectVisualAuditCombatFeatures(
                        entry, Bird.VisualAuditPose.FLAP, facingRight);
                Bird.VisualFeatureGeometry hit = game.inspectVisualAuditCombatFeatures(
                        entry, Bird.VisualAuditPose.HIT, facingRight);

                assertTrue(Math.abs(beakVectorY(flap)) > Math.abs(beakVectorX(flap)) * 2.0,
                        label + " must aim its complete face upward while climbing");
                assertTrue(beakVectorX(hit) * beakVectorX(idle) < 0.0,
                        label + " must turn its complete face away from impact in hitstun");
                for (Bird.VisualFeatureGeometry geometry : List.of(idle, flap, hit)) {
                    if ("VOID_HERALD_RAVEN".equals(entry.key())) {
                        assertTrue(geometry.ravenMask() != null
                                        && geometry.ravenMask().contains(geometry.eye(), 0.01),
                                label + " lets the cracked mask float away during motion");
                    }
                    assertFeatureGeometryIsSafe(geometry, label);
                }
            }
        }
    }

    @Test
    void ravenSpecialsUseDistinctCompleteWingAndBillCycles() {
        BirdGame3 game = freshGame();
        List<BirdGame3.VisualAuditSkin> entries = game.visualAuditSkins().stream()
                .filter(entry -> entry.bird() == BirdGame3.BirdType.RAVEN)
                .filter(entry -> BirdSpriteLibrary.sheetFor(entry.bird(), entry.key()) == null)
                .toList();

        for (BirdGame3.VisualAuditSkin entry : entries) {
            for (boolean facingRight : List.of(true, false)) {
                String label = entry.name() + " facing " + (facingRight ? "right" : "left");
                Bird.VisualFeatureGeometry idle = game.inspectVisualAuditCombatFeatures(
                        entry, Bird.VisualAuditPose.IDLE, facingRight);
                Bird.VisualFeatureGeometry quillStart = game.inspectVisualAuditRavenActionFeatures(
                        entry, Bird.VisualAuditRavenAction.NEUTRAL, 1, facingRight);
                Bird.VisualFeatureGeometry quillFull = game.inspectVisualAuditRavenActionFeatures(
                        entry, Bird.VisualAuditRavenAction.NEUTRAL,
                        Bird.RAVEN_QUILL_CHARGE_FAN_FRAMES, facingRight);
                Bird.VisualFeatureGeometry warpStart = game.inspectVisualAuditRavenActionFeatures(
                        entry, Bird.VisualAuditRavenAction.SIDE, Bird.RAVEN_SIDE_FRAMES, facingRight);
                Bird.VisualFeatureGeometry warpOpen = game.inspectVisualAuditRavenActionFeatures(
                        entry, Bird.VisualAuditRavenAction.SIDE, 7, facingRight);
                Bird.VisualFeatureGeometry warpClose = game.inspectVisualAuditRavenActionFeatures(
                        entry, Bird.VisualAuditRavenAction.SIDE, 1, facingRight);
                Bird.VisualFeatureGeometry lift = game.inspectVisualAuditRavenActionFeatures(
                        entry, Bird.VisualAuditRavenAction.UP, Bird.RAVEN_LIFT_FRAMES - 7, facingRight);
                Bird.VisualFeatureGeometry place = game.inspectVisualAuditRavenActionFeatures(
                        entry, Bird.VisualAuditRavenAction.DOWN_PLACE, 12, facingRight);
                Bird.VisualFeatureGeometry swap = game.inspectVisualAuditRavenActionFeatures(
                        entry, Bird.VisualAuditRavenAction.DOWN_SWAP, 12, facingRight);
                Bird.VisualFeatureGeometry windup = game.inspectVisualAuditRavenActionFeatures(
                        entry, Bird.VisualAuditRavenAction.ULTIMATE_WINDUP,
                        Bird.RAVEN_ULTIMATE_WINDUP_FRAMES, facingRight);
                Bird.VisualFeatureGeometry route = game.inspectVisualAuditRavenActionFeatures(
                        entry, Bird.VisualAuditRavenAction.ULTIMATE_ROUTE,
                        Bird.RAVEN_ULTIMATE_ROUTE_LIFE_FRAMES - 10, facingRight);

                assertTrue(idle.ravenWingOpenness() <= 0.09,
                        label + " must fold both idle wings");
                assertTrue(quillStart.ravenWingOpenness() >= 0.30
                                && quillStart.ravenWingOpenness() <= 0.36,
                        label + " must begin Black Quill in a restrained charging stance");
                assertTrue(quillFull.ravenWingOpenness() >= 0.86,
                        label + " must fan both wings at full Black Quill charge");
                assertTrue(quillFull.ravenBillGape() > idle.ravenBillGape() + 5.0,
                        label + " must visibly open the bill at full Black Quill charge");
                assertTrue(warpStart.ravenWingOpenness() <= 0.10,
                        label + " must begin Shadow Warp with folded wings");
                assertTrue(warpOpen.ravenWingOpenness() >= 0.74,
                        label + " must burst both wings open inside Shadow Warp");
                assertTrue(warpClose.ravenWingOpenness() <= 0.10,
                        label + " must close both wings before Shadow Warp ends");
                assertTrue(lift.ravenWingOpenness() >= 0.90,
                        label + " must fully spread both wings during Murder Lift");
                assertTrue(place.ravenWingOpenness() >= 0.40 && place.ravenWingOpenness() <= 0.46,
                        label + " must brace both wings while placing Nevermore");
                assertTrue(swap.ravenWingOpenness() >= 0.68,
                        label + " must flare both wings while swapping with Nevermore");
                assertTrue(windup.ravenWingOpenness() >= 0.96,
                        label + " must hold both wings open during The Unkindness windup");
                assertTrue(route.ravenWingOpenness() >= 0.84,
                        label + " must keep both wings spread through The Unkindness route");
                for (Bird.VisualFeatureGeometry geometry : List.of(
                        quillStart, quillFull, warpStart, warpOpen, warpClose,
                        lift, place, swap, windup, route)) {
                    assertEquals(2, geometry.bodyPartCount(Bird.VisualBodyPart.RAVEN_WING),
                            label + " special pose lost a wing");
                    assertEquals(3, geometry.bodyPartCount(Bird.VisualBodyPart.RAVEN_TAIL_FEATHER),
                            label + " special pose detached a tail feather");
                    assertEquals(3, geometry.bodyPartCount(Bird.VisualBodyPart.RAVEN_CREST_FEATHER),
                            label + " special pose detached a crest feather");
                    assertFeatureGeometryIsSafe(geometry, label + " special pose");
                }
            }
        }
    }

    @Test
    void gooseKeepsItsCompleteAttachedAnatomyAcrossEverySkinPoseAndDirection() {
        BirdGame3 game = freshGame();
        List<BirdGame3.VisualAuditSkin> entries = game.visualAuditSkins().stream()
                .filter(entry -> entry.bird() == BirdGame3.BirdType.GOOSE)
                .filter(entry -> BirdSpriteLibrary.sheetFor(entry.bird(), entry.key()) == null)
                .toList();

        for (BirdGame3.VisualAuditSkin entry : entries) {
            for (Bird.VisualAuditPose pose : Bird.VisualAuditPose.values()) {
                Bird.VisualFeatureGeometry right = game.inspectVisualAuditCombatFeatures(entry, pose, true);
                Bird.VisualFeatureGeometry left = game.inspectVisualAuditCombatFeatures(entry, pose, false);
                String label = entry.name() + " / " + pose;
                for (Bird.VisualFeatureGeometry geometry : List.of(right, left)) {
                    assertEquals(3, geometry.bodyPartCount(Bird.VisualBodyPart.GOOSE_TAIL_FEATHER),
                            label + " must retain all three layered tail feathers");
                    assertEquals(2, geometry.bodyPartCount(Bird.VisualBodyPart.GOOSE_WING),
                            label + " must retain both articulated wings");
                    assertEquals(2, geometry.bodyPartCount(Bird.VisualBodyPart.GOOSE_LEG),
                            label + " must retain both webbed legs");
                    assertEquals(1, geometry.bodyPartCount(Bird.VisualBodyPart.GOOSE_NECK),
                            label + " must retain one continuous long neck");
                    assertEquals(1, geometry.bodyPartCount(Bird.VisualBodyPart.GOOSE_CHEEK_PATCH),
                            label + " must retain its attached Canada-goose cheek patch");
                    assertTrue(geometry.gooseTorso() != null,
                            label + " did not report the authored waterfowl torso");
                    assertTrue(geometry.gooseCheekPatch() != null,
                            label + " did not report the attached cheek patch");
                    assertTrue(!geometry.gooseCheekPatch().contains(geometry.eye(), 0.01),
                            label + " lets the cheek patch obstruct the eye");
                    assertGooseNeckEndsBehindHead(geometry, label);
                    assertFeatureGeometryIsSafe(geometry, label);
                }
                assertMirrored(right, left, label);
            }
        }
    }

    @Test
    void gooseGroundedWebbedFeetMeetTheCollisionFloorAcrossEverySkinAndDirection() {
        BirdGame3 game = freshGame();
        List<BirdGame3.VisualAuditSkin> entries = game.visualAuditSkins().stream()
                .filter(entry -> entry.bird() == BirdGame3.BirdType.GOOSE)
                .filter(entry -> BirdSpriteLibrary.sheetFor(entry.bird(), entry.key()) == null)
                .toList();

        for (BirdGame3.VisualAuditSkin entry : entries) {
            for (boolean facingRight : List.of(true, false)) {
                Bird.VisualFeatureGeometry idle = game.inspectVisualAuditCombatFeatures(
                        entry, Bird.VisualAuditPose.IDLE, facingRight);
                assertEquals(80.0, idle.gooseFootBaseline(), 0.15,
                        entry.name() + " facing " + (facingRight ? "right" : "left")
                                + " must place both webbed feet on the collision floor");
            }
        }
    }

    @Test
    void gooseFaceNeckAndCheekPatchTrackFlightAndHitDirections() {
        BirdGame3 game = freshGame();
        List<BirdGame3.VisualAuditSkin> entries = game.visualAuditSkins().stream()
                .filter(entry -> entry.bird() == BirdGame3.BirdType.GOOSE)
                .filter(entry -> BirdSpriteLibrary.sheetFor(entry.bird(), entry.key()) == null)
                .toList();

        for (BirdGame3.VisualAuditSkin entry : entries) {
            for (boolean facingRight : List.of(true, false)) {
                String label = entry.name() + " facing " + (facingRight ? "right" : "left");
                Bird.VisualFeatureGeometry idle = game.inspectVisualAuditCombatFeatures(
                        entry, Bird.VisualAuditPose.IDLE, facingRight);
                Bird.VisualFeatureGeometry flap = game.inspectVisualAuditCombatFeatures(
                        entry, Bird.VisualAuditPose.FLAP, facingRight);
                Bird.VisualFeatureGeometry hit = game.inspectVisualAuditCombatFeatures(
                        entry, Bird.VisualAuditPose.HIT, facingRight);

                assertTrue(Math.abs(beakVectorY(flap)) > Math.abs(beakVectorX(flap)) * 2.0,
                        label + " must aim its complete long-necked face upward while climbing");
                assertTrue(beakVectorX(hit) * beakVectorX(idle) < 0.0,
                        label + " must turn its complete face away from impact in hitstun");
                for (Bird.VisualFeatureGeometry geometry : List.of(idle, flap, hit)) {
                    assertTrue(!geometry.gooseCheekPatch().contains(geometry.eye(), 0.01),
                            label + " lets the cheek patch drift over the eye during motion");
                    assertGooseNeckEndsBehindHead(geometry, label);
                    assertFeatureGeometryIsSafe(geometry, label);
                }
            }
        }
    }

    @Test
    void gooseSpecialsUseDistinctCompleteWingAndBillCycles() {
        BirdGame3 game = freshGame();
        List<BirdGame3.VisualAuditSkin> entries = game.visualAuditSkins().stream()
                .filter(entry -> entry.bird() == BirdGame3.BirdType.GOOSE)
                .filter(entry -> BirdSpriteLibrary.sheetFor(entry.bird(), entry.key()) == null)
                .toList();

        for (BirdGame3.VisualAuditSkin entry : entries) {
            for (boolean facingRight : List.of(true, false)) {
                String label = entry.name() + " facing " + (facingRight ? "right" : "left");
                Bird.VisualFeatureGeometry idle = game.inspectVisualAuditCombatFeatures(
                        entry, Bird.VisualAuditPose.IDLE, facingRight);
                Bird.VisualFeatureGeometry honkStart = game.inspectVisualAuditGooseActionFeatures(
                        entry, Bird.VisualAuditGooseAction.HONK_CHARGE, 1, facingRight);
                Bird.VisualFeatureGeometry honkFull = game.inspectVisualAuditGooseActionFeatures(
                        entry, Bird.VisualAuditGooseAction.HONK_CHARGE,
                        Bird.GOOSE_HONK_MAX_HOLD_FRAMES, facingRight);
                Bird.VisualFeatureGeometry honkRelease = game.inspectVisualAuditGooseActionFeatures(
                        entry, Bird.VisualAuditGooseAction.HONK_RELEASE,
                        Bird.GOOSE_HONK_RECOVERY_FRAMES, facingRight);
                Bird.VisualFeatureGeometry bargeStart = game.inspectVisualAuditGooseActionFeatures(
                        entry, Bird.VisualAuditGooseAction.BARGE, Bird.GOOSE_BARGE_FRAMES, facingRight);
                Bird.VisualFeatureGeometry bargeOpen = game.inspectVisualAuditGooseActionFeatures(
                        entry, Bird.VisualAuditGooseAction.BARGE, Bird.GOOSE_BARGE_FRAMES / 2, facingRight);
                Bird.VisualFeatureGeometry bargeClose = game.inspectVisualAuditGooseActionFeatures(
                        entry, Bird.VisualAuditGooseAction.BARGE, 1, facingRight);
                Bird.VisualFeatureGeometry lift = game.inspectVisualAuditGooseActionFeatures(
                        entry, Bird.VisualAuditGooseAction.LIFT, Bird.GOOSE_LIFT_FRAMES - 7, facingRight);
                Bird.VisualFeatureGeometry place = game.inspectVisualAuditGooseActionFeatures(
                        entry, Bird.VisualAuditGooseAction.NEST_PLACE, 12, facingRight);
                Bird.VisualFeatureGeometry guard = game.inspectVisualAuditGooseActionFeatures(
                        entry, Bird.VisualAuditGooseAction.NEST_GUARD,
                        Bird.GOOSE_NEST_GUARD_FRAMES - 8, facingRight);
                Bird.VisualFeatureGeometry counter = game.inspectVisualAuditGooseActionFeatures(
                        entry, Bird.VisualAuditGooseAction.NEST_COUNTER,
                        Bird.GOOSE_COUNTER_BURST_FRAMES - 8, facingRight);
                Bird.VisualFeatureGeometry ultimate = game.inspectVisualAuditGooseActionFeatures(
                        entry, Bird.VisualAuditGooseAction.ULTIMATE,
                        Bird.GOOSE_ULTIMATE_FRAMES - 30, facingRight);

                assertTrue(idle.gooseWingOpenness() <= 0.09,
                        label + " must fold both idle wings");
                assertTrue(honkStart.gooseWingOpenness() >= 0.20
                                && honkStart.gooseWingOpenness() <= 0.23,
                        label + " must begin Threatening Honk in a restrained charging stance");
                assertTrue(honkFull.gooseWingOpenness() >= 0.43,
                        label + " must raise both wings at full Threatening Honk charge");
                assertTrue(honkFull.gooseBillGape() > idle.gooseBillGape() + 4.0,
                        label + " must visibly open the bill at full Threatening Honk charge");
                assertTrue(honkRelease.gooseWingOpenness() >= 0.58,
                        label + " must flare both wings when Threatening Honk releases");
                assertTrue(honkRelease.gooseBillGape() > honkFull.gooseBillGape() + 1.5,
                        label + " must open the bill farther for the released honk");
                assertTrue(bargeStart.gooseWingOpenness() <= 0.10,
                        label + " must begin Bite and Barge with folded wings");
                assertTrue(bargeOpen.gooseWingOpenness() >= 0.60,
                        label + " must drive both wings through Bite and Barge");
                assertTrue(bargeClose.gooseWingOpenness() <= 0.10,
                        label + " must close both wings before Bite and Barge ends");
                assertTrue(lift.gooseWingOpenness() >= 0.94,
                        label + " must fully spread both wings during V-Formation Lift");
                assertTrue(place.gooseWingOpenness() >= 0.28 && place.gooseWingOpenness() <= 0.32,
                        label + " must brace both wings while placing the nest");
                assertTrue(guard.gooseWingOpenness() >= 0.64,
                        label + " must spread both wings during Nest Guard");
                assertTrue(counter.gooseWingOpenness() >= 0.86,
                        label + " must flare both wings for the nest counter");
                assertTrue(ultimate.gooseWingOpenness() >= 0.96,
                        label + " must hold both wings open during The Whole Flock");
                for (Bird.VisualFeatureGeometry geometry : List.of(
                        honkStart, honkFull, honkRelease, bargeStart, bargeOpen, bargeClose,
                        lift, place, guard, counter, ultimate)) {
                    assertEquals(2, geometry.bodyPartCount(Bird.VisualBodyPart.GOOSE_WING),
                            label + " special pose lost a wing");
                    assertEquals(3, geometry.bodyPartCount(Bird.VisualBodyPart.GOOSE_TAIL_FEATHER),
                            label + " special pose detached a tail feather");
                    assertEquals(2, geometry.bodyPartCount(Bird.VisualBodyPart.GOOSE_LEG),
                            label + " special pose lost a leg");
                    assertTrue(!geometry.gooseCheekPatch().contains(geometry.eye(), 0.01),
                            label + " special pose lets the cheek patch obstruct the eye");
                    assertGooseNeckEndsBehindHead(geometry, label + " special pose");
                    assertFeatureGeometryIsSafe(geometry, label + " special pose");
                }
            }
        }
    }

    private static void assertHeisenHatFollowsHead(
            Bird.VisualFeatureGeometry geometry, String label) {
        Bird.VisualFeatureBounds hat = geometry.heisenHat();
        Bird.VisualFeatureBounds head = geometry.head();
        assertTrue(hat != null && head != null, label + " did not report the hat and head bounds");
        double headCenterX = (head.left() + head.right()) * 0.5;
        double headCenterY = (head.top() + head.bottom()) * 0.5;
        double hatCenterX = (hat.left() + hat.right()) * 0.5;
        double hatCenterY = (hat.top() + hat.bottom()) * 0.5;
        double headDiagonal = Math.hypot(head.right() - head.left(), head.bottom() - head.top());
        assertTrue(Math.hypot(hatCenterX - headCenterX, hatCenterY - headCenterY) < headDiagonal * 0.85,
                label + " lets the hat float away from the head");
    }

    private static void assertPelicanPouchFollowsBill(
            Bird.VisualFeatureGeometry geometry, String label) {
        Bird.VisualFeatureBounds pouch = geometry.pelicanPouch();
        assertTrue(pouch != null, label + " did not report the Pelican pouch");
        double tolerance = 0.05;
        assertTrue(geometry.beak().tipX() >= pouch.left() - tolerance
                        && geometry.beak().tipX() <= pouch.right() + tolerance
                        && geometry.beak().tipY() >= pouch.top() - tolerance
                        && geometry.beak().tipY() <= pouch.bottom() + tolerance,
                label + " lets the pouch separate from the bill tip");
        assertTrue(pouch.right() - pouch.left() > geometry.eye().radius() * 2.0
                        || pouch.bottom() - pouch.top() > geometry.eye().radius() * 2.0,
                label + " collapses the throat pouch into a decorative line");
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

    private static void assertGooseNeckEndsBehindHead(
            Bird.VisualFeatureGeometry geometry, String label) {
        assertTrue(geometry.gooseNeck() != null, label + " did not report its neck geometry");
        double headCenterX = (geometry.head().left() + geometry.head().right()) * 0.5;
        double headCenterY = (geometry.head().top() + geometry.head().bottom()) * 0.5;
        double beakX = beakVectorX(geometry);
        double beakY = beakVectorY(geometry);
        double neckEndX = geometry.gooseNeck().tipX() - headCenterX;
        double neckEndY = geometry.gooseNeck().tipY() - headCenterY;

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
