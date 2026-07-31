package com.example.birdgame3;

import org.junit.jupiter.api.Test;

import java.io.StringReader;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StoryDialogueScriptsTest {
    @Test
    void bundledScreenplayContainsEveryStillSkyScene() {
        Map<String, String> scripts = StoryDialogueScripts.loadBundled();
        StoryCampaign campaign = StoryCampaignContent.create();
        Set<String> expectedSections = new LinkedHashSet<>(campaign.scenes.keySet());
        expectedSections.add("dynamic_campaign_phase");

        assertEquals(81, scripts.size());
        assertEquals(expectedSections, scripts.keySet());
        assertTrue(scripts.containsKey("s01_dead_air"));
        assertTrue(scripts.containsKey("s80_eagle_end"));
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
}
