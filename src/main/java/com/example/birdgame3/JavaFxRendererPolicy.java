package com.example.birdgame3;

import java.util.logging.Logger;

/**
 * Selects a JavaFX rendering pipeline before the toolkit starts.
 *
 * <p>Prism's default pipeline is hardware accelerated and must remain the
 * normal path for the game's full-HD Canvas. Software rendering is retained
 * as an explicit diagnostic/fallback mode, but forcing it from the Java
 * version alone makes every game screen CPU-bound.
 */
final class JavaFxRendererPolicy {
    static final String GAME_RENDERER_PROPERTY = "birdfight3.renderer";
    static final String PRISM_ORDER_PROPERTY = "prism.order";

    private JavaFxRendererPolicy() {
    }

    static void configureBeforeJavaFxStartup(Logger logger) {
        // An explicit JavaFX pipeline always wins. This keeps command-line
        // diagnostics such as -Dprism.order=d3d or -Dprism.order=sw useful.
        if (hasText(System.getProperty(PRISM_ORDER_PROPERTY))) {
            return;
        }

        String requestedMode = normalizedRendererMode(
                System.getProperty(GAME_RENDERER_PROPERTY, "auto"));
        if (!"software".equals(requestedMode)) {
            return;
        }

        System.setProperty(PRISM_ORDER_PROPERTY, "sw");
        if (logger != null) {
            logger.info("RENDERER: software Canvas pipeline (requested)");
        }
    }

    static String normalizedRendererMode(String value) {
        if (value == null) return "auto";
        return switch (value.trim().toLowerCase(java.util.Locale.ROOT)) {
            case "software" -> "software";
            case "hardware" -> "hardware";
            default -> "auto";
        };
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
