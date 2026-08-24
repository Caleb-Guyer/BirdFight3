package com.example.birdgame3;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HubPresentationModelTest {
    @Test
    void contextualHelpStaysShortEnoughForOneLine() {
        Stream.concat(
                Arrays.stream(HubPresentationModel.Destination.values())
                        .map(HubPresentationModel.Destination::description),
                Arrays.stream(HubPresentationModel.ExtraMode.values())
                        .map(HubPresentationModel.ExtraMode::description)
        ).forEach(description -> {
            assertFalse(description.isBlank());
            assertTrue(description.length() <= HubPresentationModel.MAX_DESCRIPTION_LENGTH,
                    () -> "dashboard copy is too verbose: " + description);
            assertFalse(description.contains(" and ") && description.chars().filter(c -> c == ',').count() > 1,
                    () -> "dashboard copy reads like a feature list: " + description);
        });
    }

    @Test
    void idleStateDoesNotWasteSpaceOnInstructions() {
        assertTrue(HubPresentationModel.IDLE_TITLE.length() <= 20);
        assertTrue(HubPresentationModel.IDLE_DESCRIPTION.length() <= 24);
    }
}
