package com.example.birdgame3;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class BattlefieldPresentationTest {
    @Test
    void distantCitadelStaysClearlyAboveSidePlatformRow() {
        assertTrue(BirdGame3.battlefieldCitadelClearanceAboveSidePlatforms() >= 70.0,
                "Battlefield citadel must not visually merge with the side platforms");
    }

    @Test
    void distantCitadelRemainsBackgroundScale() {
        assertTrue(BirdGame3.BATTLEFIELD_CITADEL_SCALE <= 0.20,
                "Battlefield citadel must read as a distant landmark, not a gameplay platform");
    }

    @Test
    void distantHillSummitSupportsEntireCitadelFoundation() {
        assertTrue(BirdGame3.BATTLEFIELD_CITADEL_HILL_PLATEAU_HALF_W
                        >= BirdGame3.battlefieldCitadelHalfFootprint() + 20.0,
                "Battlefield citadel foundation must remain visibly supported by its distant hill");
    }
}
