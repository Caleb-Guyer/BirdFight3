package com.example.birdgame3;

import org.junit.jupiter.api.Test;

import java.io.StringReader;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StoryDialogueScriptsTest {
    @Test
    void bundledScreenplayContainsEveryStillSkyScene() {
        Map<String, String> scripts = StoryDialogueScripts.loadBundled();
        StoryCampaign campaign = StoryCampaignContent.create();
        Set<String> expectedSections = new LinkedHashSet<>(campaign.scenes.keySet());
        expectedSections.add("dynamic_campaign_phase");
        expectedSections.add("dynamic_null_rock_duel");

        assertEquals(expectedSections.size(), scripts.size());
        assertEquals(expectedSections, scripts.keySet());
        assertTrue(scripts.containsKey("s01_dead_air"));
        assertTrue(scripts.containsKey("s80_eagle_end"));
        assertTrue(scripts.containsKey("s81_beyond_the_map"));
        int dialogueLineCount = scripts.values().stream()
                .mapToInt(script -> script.split("\\R").length)
                .sum();
        assertTrue(dialogueLineCount >= 700 && dialogueLineCount <= 900,
                "The editable screenplay should stay within the authored 700-900 line target.");
    }

    @Test
    void phaseBreakDialogueAlsoComesFromTheEditableScreenplay() {
        var lines = StoryCampaignContent.campaignPhaseDialogue(
                "Charles",
                BirdGame3.BirdType.MOCKINGBIRD,
                "Reach the east vent"
        );

        assertEquals(2, lines.size());
        assertEquals("Charles", lines.getFirst().speaker());
        assertEquals("Checkpoint's secure. Reach the east vent.", lines.getFirst().text());
        assertEquals("Crown System", lines.getLast().speaker());
    }

    @Test
    void parserRejectsDuplicateSceneSections() {
        String duplicate = """
                [s01_dead_air]
                Pigeon|First line.
                [s01_dead_air]
                Pigeon|Second line.
                """;

        assertThrows(IllegalArgumentException.class,
                () -> StoryDialogueScripts.parse(new StringReader(duplicate)));
    }

    @Test
    void selectedBirdRowsRemainPlainEditableText() throws Exception {
        Map<String, String> scripts = StoryDialogueScripts.parse(new StringReader("""
                # Comments are ignored.
                [finale]
                Pigeon|The routes are open.
                Goose@GOOSE|The sky is ours.
                """));

        assertEquals("Pigeon|The routes are open.\nGoose@GOOSE|The sky is ours.",
                scripts.get("finale"));
    }

    @Test
    void bundledDialogueNeverAddressesTheRealPlayer() {
        String screenplay = String.join("\n", StoryDialogueScripts.loadBundled().values())
                .toLowerCase();

        for (String fourthWallPhrase : Set.of(
                "player", "choose your bird", "pick your bird", "selected bird",
                "press enter", "keyboard", "controller")) {
            assertFalse(screenplay.contains(fourthWallPhrase),
                    () -> "Story dialogue broke the fourth wall with: " + fourthWallPhrase);
        }
    }
}
