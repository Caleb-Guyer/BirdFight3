package com.example.birdgame3;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class JavaFxRendererPolicyTest {
    private final String originalGameRenderer =
            System.getProperty(JavaFxRendererPolicy.GAME_RENDERER_PROPERTY);
    private final String originalPrismOrder =
            System.getProperty(JavaFxRendererPolicy.PRISM_ORDER_PROPERTY);

    @AfterEach
    void restoreRendererProperties() {
        restoreProperty(JavaFxRendererPolicy.GAME_RENDERER_PROPERTY, originalGameRenderer);
        restoreProperty(JavaFxRendererPolicy.PRISM_ORDER_PROPERTY, originalPrismOrder);
    }

    @Test
    void automaticModeLeavesPrismFreeToChooseHardware() {
        System.clearProperty(JavaFxRendererPolicy.PRISM_ORDER_PROPERTY);
        System.setProperty(JavaFxRendererPolicy.GAME_RENDERER_PROPERTY, "auto");

        JavaFxRendererPolicy.configureBeforeJavaFxStartup(null);

        assertNull(System.getProperty(JavaFxRendererPolicy.PRISM_ORDER_PROPERTY));
    }

    @Test
    void explicitPrismOrderIsPreserved() {
        System.setProperty(JavaFxRendererPolicy.PRISM_ORDER_PROPERTY, "d3d");
        System.setProperty(JavaFxRendererPolicy.GAME_RENDERER_PROPERTY, "software");

        JavaFxRendererPolicy.configureBeforeJavaFxStartup(null);

        assertEquals("d3d", System.getProperty(JavaFxRendererPolicy.PRISM_ORDER_PROPERTY));
    }

    @Test
    void softwareModeCanBeRequestedOnAnyRuntime() {
        System.clearProperty(JavaFxRendererPolicy.PRISM_ORDER_PROPERTY);
        System.setProperty(JavaFxRendererPolicy.GAME_RENDERER_PROPERTY, "software");

        JavaFxRendererPolicy.configureBeforeJavaFxStartup(null);

        assertEquals("sw", System.getProperty(JavaFxRendererPolicy.PRISM_ORDER_PROPERTY));
    }

    @Test
    void hardwareModeDoesNotOverridePrism() {
        System.clearProperty(JavaFxRendererPolicy.PRISM_ORDER_PROPERTY);
        System.setProperty(JavaFxRendererPolicy.GAME_RENDERER_PROPERTY, "hardware");

        JavaFxRendererPolicy.configureBeforeJavaFxStartup(null);

        assertNull(System.getProperty(JavaFxRendererPolicy.PRISM_ORDER_PROPERTY));
    }

    @Test
    void unknownModeFallsBackToAutomaticHardwareSelection() {
        System.clearProperty(JavaFxRendererPolicy.PRISM_ORDER_PROPERTY);
        System.setProperty(JavaFxRendererPolicy.GAME_RENDERER_PROPERTY, "potato");

        JavaFxRendererPolicy.configureBeforeJavaFxStartup(null);

        assertEquals("auto", JavaFxRendererPolicy.normalizedRendererMode("potato"));
        assertNull(System.getProperty(JavaFxRendererPolicy.PRISM_ORDER_PROPERTY));
    }

    private static void restoreProperty(String key, String value) {
        if (value == null) {
            System.clearProperty(key);
        } else {
            System.setProperty(key, value);
        }
    }
}
