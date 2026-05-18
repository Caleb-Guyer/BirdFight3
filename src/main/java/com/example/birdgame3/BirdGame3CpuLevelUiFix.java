package com.example.birdgame3;

/**
 * CPU Level UI Fix for 4-Player Bird Selection Screen
 * 
 * This class provides a helper method to properly layout CPU level buttons
 * based on the number of active players, ensuring visibility for all players
 * including when 4 players are selected.
 */
public final class BirdGame3CpuLevelUiFix {
    
    /**
     * Calculate the X position for a CPU level button based on player index and total players
     * @param playerIndex The index of the player (0-3)
     * @param activePlayers The number of active players (1-4)
     * @param screenWidth The width of the screen (typically 1920)
     * @return The X coordinate for the button
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
    
    /**
     * Calculate the Y position for a CPU level button
     * @param screenHeight The height of the screen (typically 1080)
     * @return The Y coordinate for the button
     */
    public static double calculateCpuButtonY(double screenHeight) {
        return screenHeight * 0.75; // Position at 75% down the screen
    }
    
    /**
     * Get the preferred button width based on number of players
     * @param activePlayers The number of active players
     * @return The width for CPU level buttons
     */
    public static double getCpuButtonWidth(int activePlayers) {
        if (activePlayers >= 4) {
            return 140.0; // Narrower for 4 players
        } else if (activePlayers == 3) {
            return 160.0;
        }
        return 180.0; // Wider for 1-2 players
    }
    
    /**
     * Get the preferred button height
     * @return The height for CPU level buttons
     */
    public static double getCpuButtonHeight() {
        return 60.0;
    }
}
