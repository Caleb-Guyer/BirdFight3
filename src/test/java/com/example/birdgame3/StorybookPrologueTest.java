package com.example.birdgame3;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;

import static org.junit.jupiter.api.Assertions.*;

class StorybookPrologueTest {
    @Test
    void bundledPrologueIsACompleteTenPageHistory() {
        StorybookPrologue prologue = StorybookPrologue.loadBundled();

        assertEquals(10, prologue.pages.size());
        assertTrue(prologue.wordCount() >= 900, "The prologue should provide substantial worldbuilding");
        assertEquals(prologue.pages.size(), new HashSet<>(
                prologue.pages.stream().map(StorybookPrologue.Page::id).toList()).size());
        assertEquals(prologue.pages.size(), new HashSet<>(
                prologue.pages.stream().map(StorybookPrologue.Page::illustration).toList()).size());
        assertTrue(prologue.pages.stream().allMatch(page -> page.paragraphs().size() >= 2));
    }

    @Test
    void bundledEpilogueRecordsEveryPlayableBirdWithoutErasingTheCost() {
        StorybookPrologue epilogue = StorybookPrologue.loadEpilogue();
        String text = epilogue.pages.stream()
                .map(StorybookPrologue.Page::prose)
                .reduce("", (left, right) -> left + " " + right);

        assertEquals(StorybookPrologue.EPILOGUE_ID, epilogue.id);
        assertEquals("EPILOGUE", epilogue.header);
        assertEquals(10, epilogue.pages.size());
        for (BirdGame3.BirdType bird : StoryCampaign.STILL_SKY_ROSTER) {
            assertTrue(text.contains(bird.name), () -> "Missing epilogue legacy for " + bird.name);
        }
        assertTrue(text.contains("Old Sparrow"));
        assertTrue(text.contains("did not make him innocent"));
        assertTrue(text.contains("No ruler owned the wind"));
    }

    @Test
    void historyExplainsBirdFightAndLeadsDirectlyIntoDeadAirWithoutSpoilingTheFinalForm() {
        StorybookPrologue prologue = StorybookPrologue.loadBundled();
        String text = prologue.pages.stream()
                .map(StorybookPrologue.Page::prose)
                .reduce("", (left, right) -> left + " " + right);

        assertTrue(text.contains("first Bird Fight"));
        assertTrue(text.contains("Open-Wing Accord"));
        assertTrue(text.contains("Crown Network"));
        assertTrue(text.contains("Old Sparrow"));
        assertTrue(text.endsWith("The wind had not died. It was being held."));
        assertFalse(text.contains("The Null Rock"));
    }

    @Test
    void storybookTimingIsBoundedAndPageTurnsAreClosedForm() {
        assertEquals(10.0, StorybookProloguePlayer.automaticDuration("short"), 0.000_001);
        assertEquals(22.0, StorybookProloguePlayer.automaticDuration("word ".repeat(500)), 0.000_001);
        assertEquals(0.0, StorybookProloguePlayer.turnProgress(0, 99), 0.000_001);
        assertEquals(0.0, StorybookProloguePlayer.turnProgress(1_000, 500), 0.000_001);
        assertEquals(1.0, StorybookProloguePlayer.turnProgress(1_000, 9_000_000_000L), 0.000_001);
    }

    @Test
    void prologuePlaysOnlyBeforeTheFirstMissionUntilAStoryReset() {
        StoryCampaign campaign = StoryCampaignContent.create();
        StoryCampaignProgress progress = new StoryCampaignProgress();

        assertTrue(BirdGame3.shouldPlayCampaignPrologue(campaign.firstMission(), campaign, progress));
        assertFalse(BirdGame3.shouldPlayCampaignPrologue(campaign.orderedMissions.get(1), campaign, progress));

        progress.markSceneSeen(StorybookPrologue.ID);
        assertFalse(BirdGame3.shouldPlayCampaignPrologue(campaign.firstMission(), campaign, progress));

        progress.reset(campaign);
        assertTrue(BirdGame3.shouldPlayCampaignPrologue(campaign.firstMission(), campaign, progress));
    }

    @Test
    void storybookPresentationCannotConsumeSimulationRandomness() throws IOException {
        String source = Files.readString(Path.of(
                "src", "main", "java", "com", "example", "birdgame3", "StorybookProloguePlayer.java"));

        assertFalse(source.contains("SimRng.next"));
        assertFalse(source.contains("game.random"));
        assertFalse(source.contains("random.next"));
        assertFalse(source.contains("simTick"));
        assertTrue(source.contains("AnimationTimer"));
        assertTrue(source.contains("System.nanoTime()"));
    }
}
