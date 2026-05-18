package com.example.birdgame3;

import javafx.animation.AnimationTimer;
import javafx.application.Platform;
import javafx.scene.control.Button;
import javafx.scene.effect.DropShadow;
import javafx.scene.paint.Color;

/**
 * CPU Level UI Fix for 4-Player Bird Selection Screen
 *
 * This class provides helper methods to properly layout CPU level buttons
 * and a runtime installer that ensures CPU buttons/labels are always visible
 * on top of translucent player overlays.
 */
public final class BirdGame3CpuLevelUiFix {

    private static volatile boolean installed = false;

    public static void installGlobal() {
        if (installed) return;
        installed = true;
        // Schedule installation on the JavaFX thread
        Platform.runLater(() -> {
            // Create a lightweight AnimationTimer that periodically brings cpuButtons to front
            AnimationTimer fixTimer = new AnimationTimer() {
                private long last = 0L;

                @Override
                public void handle(long now) {
                    // throttle to ~4 times/sec
                    if (now - last < 200_000_000L) return;
                    last = now;

                    // Try to find any active BirdGame3 instances via a weak global reference is not available,
                    // so rely on the common pattern: the UIFactory is created early by BirdGame3 and will call this installer.
                    // We will attempt to find the singleton-like active stage roots via javafx.application.Platform,
                    // but since we don't have direct access to the BirdGame3 instance here, we will instead
                    // iterate over all windows and bring Buttons that look like cpu controls to front.

                    try {
                        javafx.stage.Window.getWindows().forEach(window -> {
                            if (!(window instanceof javafx.stage.Stage)) return;
                            javafx.scene.Scene scene = ((javafx.stage.Stage) window).getScene();
                            if (scene == null) return;
                            javafx.scene.Parent root = scene.getRoot();
                            if (root == null) return;

                            // Look up nodes that have the style class or id we expect for CPU buttons.
                            // We can't rely on specific ids, so we heuristically search for Buttons whose text
                            // contains a single digit or the string "CPU" and bring them forward and restyle.
                            root.lookupAll(".button").forEach(node -> {
                                if (!(node instanceof Button)) return;
                                Button b = (Button) node;
                                String txt = b.getText();
                                if (txt == null) return;
                                String t = txt.trim().toUpperCase();
                                boolean looksLikeCpuLabel = false;
                                // Examples: "1", "2", "CPU", "P1", "P2" are common. We only target small numeric labels.
                                if (t.matches("^\\d+$") || t.equals("CPU")) {
                                    looksLikeCpuLabel = true;
                                }
                                if (!looksLikeCpuLabel) return;

                                // Bring to front and apply high-contrast styling so label stays visible
                                try {
                                    b.toFront();
                                    // Give a subtle drop shadow and ensure text is bright
                                    b.setStyle(b.getStyle() + "; -fx-text-fill: white; -fx-font-weight: bold;");
                                    b.setEffect(new DropShadow(6, Color.rgb(0,0,0,0.75)));
                                } catch (Throwable ignored) {}
                            });
                        });
                    } catch (Throwable ignored) {
                    }
                }
            };
            fixTimer.start();
        });
    }

    /**
     * Calculate the X position for a CPU level button based on player index and total players
     */
    public static double calculateCpuButtonX(int playerIndex, int activePlayers, double screenWidth) {
        if (activePlayers == 0) return screenWidth / 2;

        double centerX = screenWidth / 2;
        double totalSpacing = 280.0; // Base spacing between buttons

        // Adjust spacing based on number of players to fit all on screen
        if (activePlayers == 4) {
            totalSpacing = 220.0; // Tighter spacing for 4 players
        } else if (activePlayers == 3) {
            totalSpacing = 260.0;
        }

        // Calculate start position (centered)
        double startX = centerX - (totalSpacing * (activePlayers - 1) / 2.0);
        return startX + (playerIndex * totalSpacing);
    }

    public static double calculateCpuButtonY(double screenHeight) {
        return screenHeight * 0.75; // Position at 75% down the screen
    }

    public static double getCpuButtonWidth(int activePlayers) {
        if (activePlayers >= 4) {
            return 140.0; // Narrower for 4 players
        } else if (activePlayers == 3) {
            return 160.0;
        }
        return 180.0; // Wider for 1-2 players
    }

    public static double getCpuButtonHeight() {
        return 60.0;
    }
}
