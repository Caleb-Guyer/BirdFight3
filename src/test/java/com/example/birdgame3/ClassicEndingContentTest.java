package com.example.birdgame3;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class ClassicEndingContentTest {
    @Test
    void allEightAuthoredRoutesHaveUniqueMovingPictureMonologues() {
        List<ClassicEndingContent.Ending> endings = ClassicEndingContent.endings();

        assertEquals(List.of(
                        BirdGame3.BirdType.PIGEON,
                        BirdGame3.BirdType.EAGLE,
                        BirdGame3.BirdType.FALCON,
                        BirdGame3.BirdType.PHOENIX,
                        BirdGame3.BirdType.HUMMINGBIRD,
                        BirdGame3.BirdType.TURKEY,
                        BirdGame3.BirdType.ROOSTER,
                        BirdGame3.BirdType.ROADRUNNER),
                endings.stream().map(ClassicEndingContent.Ending::bird).toList());
        assertEquals(8, new HashSet<>(endings.stream().map(ClassicEndingContent.Ending::title).toList()).size());
        assertEquals(8, new HashSet<>(endings.stream().map(ClassicEndingContent.Ending::crownChoice).toList()).size());
        assertEquals(8, new HashSet<>(endings.stream().map(ending -> ending.cinematic().id()).toList()).size());

        for (ClassicEndingContent.Ending ending : endings) {
            ClassicEndingContent.Cinematic cinematic = ending.cinematic();
            assertEquals(ending.bird(), cinematic.narrator());
            assertEquals(ending.defeatedBoss(), cinematic.defeatedBoss());
            assertFalse(cinematic.defeatedBossSkin().isBlank());
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
    }
}
