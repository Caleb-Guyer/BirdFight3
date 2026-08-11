package com.example.birdgame3;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class ClassicEndingContentTest {
    @Test
    void allSevenAuthoredRoutesHaveUniqueAnimatedCrownEpilogues() {
        List<ClassicEndingContent.Ending> endings = ClassicEndingContent.endings();

        assertEquals(List.of(
                        BirdGame3.BirdType.PIGEON,
                        BirdGame3.BirdType.EAGLE,
                        BirdGame3.BirdType.FALCON,
                        BirdGame3.BirdType.PHOENIX,
                        BirdGame3.BirdType.HUMMINGBIRD,
                        BirdGame3.BirdType.TURKEY,
                        BirdGame3.BirdType.ROOSTER),
                endings.stream().map(ClassicEndingContent.Ending::bird).toList());
        assertEquals(7, new HashSet<>(endings.stream().map(ClassicEndingContent.Ending::title).toList()).size());
        assertEquals(7, new HashSet<>(endings.stream().map(ClassicEndingContent.Ending::crownChoice).toList()).size());
        assertEquals(7, new HashSet<>(endings.stream().map(ending -> ending.cutscene().id()).toList()).size());

        for (ClassicEndingContent.Ending ending : endings) {
            StoryCampaign.Cutscene cutscene = ending.cutscene();
            assertTrue(cutscene.finale());
            assertTrue(cutscene.lines().size() >= 7);
            assertTrue(cutscene.lines().stream().anyMatch(line -> "Crown System".equals(line.speaker())),
                    ending.bird() + " must visibly confront the Crown command core.");
            assertTrue(cutscene.lines().stream().anyMatch(line -> line.bird() == ending.bird()),
                    ending.bird() + " must act in its own ending.");
            assertTrue(cutscene.lines().stream().anyMatch(line -> line.bird() == ending.defeatedBoss()),
                    ending.bird() + " must resolve its final boss conflict.");
            assertTrue(cutscene.lines().stream().anyMatch(line -> line.motion() == StoryCampaign.ActorMotion.ATTACK),
                    ending.bird() + " must animate its choice rather than only narrating it.");
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
    void routeRecordIsPartOfTheCinematicAndIncludesTheMapReward() {
        ClassicEndingContent.Ending ending = ClassicEndingContent.endingFor(BirdGame3.BirdType.ROOSTER);

        StoryCampaign.Cutscene recorded = ClassicEndingContent.withRouteRecord(
                ending, 275, 123_456, "Dawnwatch Bastion");

        assertEquals(ending.cutscene().lines().size() + 1, recorded.lines().size());
        StoryCampaign.DialogueLine record = recorded.lines().getLast();
        assertEquals("Crown System", record.speaker());
        assertTrue(record.text().contains("ROUTE BADGE RECORDED"));
        assertTrue(record.text().contains("DAWNWATCH BASTION UNLOCKED"));
        assertTrue(record.text().contains("BIRD COINS +275"));
        assertTrue(record.text().contains("123,456"));
    }

    @Test
    void existingRouteBadgeImmediatelyUnlocksItsGalleryEnding() {
        BirdGame3 game = new BirdGame3();

        assertFalse(game.isClassicEndingUnlocked(BirdGame3.BirdType.PIGEON));
        game.setClassicCompleted(BirdGame3.BirdType.PIGEON);
        assertTrue(game.isClassicEndingUnlocked(BirdGame3.BirdType.PIGEON));

        game.setClassicCompleted(BirdGame3.BirdType.ROADRUNNER);
        assertFalse(game.isClassicEndingUnlocked(BirdGame3.BirdType.ROADRUNNER),
                "A placeholder route badge must not expose an ending that has not been authored yet.");
    }
}
