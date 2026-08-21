package com.example.birdgame3;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class ClassicEndingContentTest {
    @Test
    void galleryCardsStayClippedBelowPersistentHeaderControls() throws Exception {
        String source = Files.readString(Path.of("src/main/java/com/example/birdgame3/BirdGame3.java"));
        int start = source.indexOf("private void showClassicEndingGallery(Stage stage)");
        int end = source.indexOf("private boolean isDailyChallengeRetired()", start);
        assertTrue(start >= 0 && end > start);
        String gallery = source.substring(start, end);

        assertTrue(gallery.contains("int galleryColumns = Math.min(5"));
        assertTrue(gallery.contains("new ScrollPane(grid)"));
        assertTrue(gallery.contains("ScrollPane.ScrollBarPolicy.NEVER"));
        assertTrue(gallery.contains("ScrollPane.ScrollBarPolicy.AS_NEEDED"));
        assertTrue(gallery.contains("installTransparentScrollViewport(galleryViewport)"));
        assertTrue(gallery.contains("index % galleryColumns"));
        assertTrue(gallery.contains("bindEscape(scene, back)"));
    }

    @Test
    void allEighteenAuthoredRoutesHaveUniqueMovingPictureMonologues() {
        List<ClassicEndingContent.Ending> endings = ClassicEndingContent.endings();

        assertEquals(List.of(
                        BirdGame3.BirdType.PIGEON,
                        BirdGame3.BirdType.EAGLE,
                        BirdGame3.BirdType.FALCON,
                        BirdGame3.BirdType.PHOENIX,
                        BirdGame3.BirdType.HUMMINGBIRD,
                        BirdGame3.BirdType.TURKEY,
                        BirdGame3.BirdType.ROOSTER,
                        BirdGame3.BirdType.ROADRUNNER,
                        BirdGame3.BirdType.PENGUIN,
                        BirdGame3.BirdType.SHOEBILL,
                        BirdGame3.BirdType.MOCKINGBIRD,
                        BirdGame3.BirdType.RAZORBILL,
                        BirdGame3.BirdType.GRINCHHAWK,
                        BirdGame3.BirdType.VULTURE,
                        BirdGame3.BirdType.OPIUMBIRD,
                        BirdGame3.BirdType.HEISENBIRD,
                        BirdGame3.BirdType.TITMOUSE,
                        BirdGame3.BirdType.BAT),
                endings.stream().map(ClassicEndingContent.Ending::bird).toList());
        assertEquals(18, new HashSet<>(endings.stream().map(ClassicEndingContent.Ending::title).toList()).size());
        assertEquals(18, new HashSet<>(endings.stream().map(ClassicEndingContent.Ending::crownChoice).toList()).size());
        assertEquals(18, new HashSet<>(endings.stream().map(ending -> ending.cinematic().id()).toList()).size());

        for (ClassicEndingContent.Ending ending : endings) {
            ClassicEndingContent.Cinematic cinematic = ending.cinematic();
            assertEquals(ending.bird(), cinematic.narrator());
            assertEquals(ending.defeatedBoss(), cinematic.defeatedBoss());
            if (ending.bird() == BirdGame3.BirdType.MOCKINGBIRD
                    || ending.bird() == BirdGame3.BirdType.RAZORBILL
                    || ending.bird() == BirdGame3.BirdType.GRINCHHAWK
                    || ending.bird() == BirdGame3.BirdType.VULTURE
                    || ending.bird() == BirdGame3.BirdType.HEISENBIRD
                    || ending.bird() == BirdGame3.BirdType.TITMOUSE) {
                assertTrue(cinematic.defeatedBossSkin().isBlank(),
                        "The Hollow Maestro is original route art, not a skin on another bird.");
            } else {
                assertFalse(cinematic.defeatedBossSkin().isBlank());
            }
            assertEquals(List.of(ClassicEndingContent.Tableau.values()),
                    cinematic.beats().stream().map(ClassicEndingContent.Beat::tableau).toList());
            assertTrue(cinematic.beats().stream().allMatch(beat -> !beat.narration().isBlank()));
            assertTrue(cinematic.beats().stream().allMatch(beat -> beat.durationSeconds() >= 5.0));
            assertTrue(cinematic.beats().getFirst().narration().contains(cinematic.defeatedBossName()),
                    ending.bird() + " must open by resolving its actual final boss.");
            assertTrue(cinematic.beats().stream().anyMatch(beat -> beat.narration().contains("Crown")),
                    ending.bird() + " must monologue about its choice for the Crown.");
        }
    }

    @Test
    void endingsIncludeGoodAmbiguousAndDarkUsesOfTheCrown() {
        Set<ClassicEndingContent.Alignment> alignments = new HashSet<>(ClassicEndingContent.endings().stream()
                .map(ClassicEndingContent.Ending::alignment)
                .toList());

        assertEquals(Set.of(
                ClassicEndingContent.Alignment.HOPEFUL,
                ClassicEndingContent.Alignment.AMBIGUOUS,
                ClassicEndingContent.Alignment.DOMINATING), alignments);
        assertEquals(ClassicEndingContent.Alignment.DOMINATING,
                ClassicEndingContent.endingFor(BirdGame3.BirdType.EAGLE).alignment());
        assertEquals(ClassicEndingContent.Alignment.AMBIGUOUS,
                ClassicEndingContent.endingFor(BirdGame3.BirdType.FALCON).alignment());
    }

    @Test
    void routeRecordIsAnEndCardInsteadOfAnotherDialogueLine() {
        ClassicEndingContent.Ending ending = ClassicEndingContent.endingFor(BirdGame3.BirdType.ROOSTER);

        ClassicEndingContent.Cinematic recorded = ClassicEndingContent.withRouteRecord(
                ending, 275, 123_456, "Dawnwatch Bastion");

        assertEquals(ending.cinematic().beats(), recorded.beats(),
                "Progress rewards must not be inserted into the bird's monologue.");
        assertEquals(275, recorded.routeRecord().birdCoins());
        assertEquals(123_456, recorded.routeRecord().score());
        String endCard = ClassicEndingContent.routeRecordText(recorded.routeRecord());
        assertTrue(endCard.contains("ROUTE BADGE EARNED"));
        assertTrue(endCard.contains("DAWNWATCH BASTION UNLOCKED"));
        assertTrue(endCard.contains("BIRD COINS +275"));
        assertTrue(endCard.contains("123,456"));
    }

    @Test
    void existingRouteBadgeImmediatelyUnlocksItsGalleryEnding() {
        BirdGame3 game = new BirdGame3();

        assertFalse(game.isClassicEndingUnlocked(BirdGame3.BirdType.PIGEON));
        game.setClassicCompleted(BirdGame3.BirdType.PIGEON);
        assertTrue(game.isClassicEndingUnlocked(BirdGame3.BirdType.PIGEON));

        game.setClassicCompleted(BirdGame3.BirdType.ROADRUNNER);
        assertTrue(game.isClassicEndingUnlocked(BirdGame3.BirdType.ROADRUNNER));
        assertTrue(ClassicEndingContent.isContinuousPanorama(
                ClassicEndingContent.endingFor(BirdGame3.BirdType.ROADRUNNER).cinematic()));
        game.setClassicCompleted(BirdGame3.BirdType.PENGUIN);
        assertTrue(game.isClassicEndingUnlocked(BirdGame3.BirdType.PENGUIN));
        assertTrue(ClassicEndingContent.isSubglacialMontage(
                ClassicEndingContent.endingFor(BirdGame3.BirdType.PENGUIN).cinematic()));
        game.setClassicCompleted(BirdGame3.BirdType.SHOEBILL);
        assertTrue(game.isClassicEndingUnlocked(BirdGame3.BirdType.SHOEBILL));
        assertTrue(ClassicEndingContent.isStillwaterRevelation(
                ClassicEndingContent.endingFor(BirdGame3.BirdType.SHOEBILL).cinematic()));
        game.setClassicCompleted(BirdGame3.BirdType.RAZORBILL);
        assertTrue(game.isClassicEndingUnlocked(BirdGame3.BirdType.RAZORBILL));
        assertTrue(ClassicEndingContent.isRazorbillFinalCut(
                ClassicEndingContent.endingFor(BirdGame3.BirdType.RAZORBILL).cinematic()));
        game.setClassicCompleted(BirdGame3.BirdType.VULTURE);
        assertTrue(game.isClassicEndingUnlocked(BirdGame3.BirdType.VULTURE));
        assertTrue(ClassicEndingContent.isVultureFinalAccount(
                ClassicEndingContent.endingFor(BirdGame3.BirdType.VULTURE).cinematic()));
        game.setClassicCompleted(BirdGame3.BirdType.HEISENBIRD);
        assertTrue(game.isClassicEndingUnlocked(BirdGame3.BirdType.HEISENBIRD));
        assertTrue(ClassicEndingContent.isHeisenBlueVault(
                ClassicEndingContent.endingFor(BirdGame3.BirdType.HEISENBIRD).cinematic()));
        game.setClassicCompleted(BirdGame3.BirdType.TITMOUSE);
        assertTrue(game.isClassicEndingUnlocked(BirdGame3.BirdType.TITMOUSE));
        assertTrue(ClassicEndingContent.isTitmouseWarningBeacon(
                ClassicEndingContent.endingFor(BirdGame3.BirdType.TITMOUSE).cinematic()));
    }
}
