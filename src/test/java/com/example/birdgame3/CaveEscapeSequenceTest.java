package com.example.birdgame3;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CaveEscapeSequenceTest {
    @Test
    void pigeonDoesNotAutoRunWhenThePlayerProvidesNoInput() {
        CaveEscapeSequence escape = new CaveEscapeSequence(new BirdGame3());
        escape.resetForTest();
        double startX = escape.progressXForTest();

        for (int tick = 0; tick < 45; tick++) {
            escape.tickForTest();
        }

        assertEquals(startX, escape.progressXForTest(), 0.0001);
    }

    @Test
    void normalMovementAndHeldJumpControlTheEscapePigeon() {
        CaveEscapeSequence escape = new CaveEscapeSequence(new BirdGame3());
        escape.resetForTest();
        double startX = escape.progressXForTest();
        double startY = escape.verticalPositionForTest();

        escape.setControlsForTest(false, true, true, false, false, false);
        for (int tick = 0; tick < 12; tick++) {
            escape.tickForTest();
        }

        assertTrue(escape.progressXForTest() > startX + 30.0);
        assertTrue(escape.verticalPositionForTest() < startY - 40.0);
    }

    @Test
    void sideSpecialProvidesTheExpectedDirectionalBurst() {
        CaveEscapeSequence escape = new CaveEscapeSequence(new BirdGame3());
        escape.resetForTest();
        double startX = escape.progressXForTest();

        escape.setControlsForTest(false, true, false, false, false, true);
        escape.tickForTest();

        assertTrue(escape.progressXForTest() > startX + 15.0);
    }

    @Test
    void assignedControllerUsesTheSameMovementAndFlightPath() {
        CaveEscapeSequence escape = new CaveEscapeSequence(new BirdGame3());
        escape.resetForTest();
        double startX = escape.progressXForTest();
        double startY = escape.verticalPositionForTest();

        escape.setControllerControlsForTest(false, true, true, false, false, false);
        for (int tick = 0; tick < 12; tick++) {
            escape.tickForTest();
        }

        assertTrue(escape.progressXForTest() > startX + 30.0);
        assertTrue(escape.verticalPositionForTest() < startY - 40.0);
    }

    @Test
    void fullscreenInputFiltersAndEmotionalFinaleMusicStayConnected() throws IOException {
        String source = Files.readString(Path.of(
                "src", "main", "java", "com", "example", "birdgame3", "CaveEscapeSequence.java"));

        assertTrue(source.contains("prepareCampaignPlayableSequenceScene"));
        assertTrue(source.contains("addCampaignSceneEventFilter"));
        assertTrue(source.contains("campaignSequenceControllerState"));
        assertTrue(source.contains("startCampaignSequenceMusic(\"music-credits.mp3\", true)"));

        int finishStart = source.indexOf("private void finishEscape()");
        int abandonStart = source.indexOf("private void abandon(", finishStart);
        String finishMethod = source.substring(finishStart, abandonStart);
        assertFalse(finishMethod.contains("finishCampaignSequenceMusic"),
                "The emotional track should continue into the epilogue instead of restarting.");
    }
}
