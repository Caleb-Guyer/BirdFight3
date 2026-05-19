package com.example.birdgame3;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FightSetupSelectionStateTest {
    @Test
    void allReadyRequiresEachActivePlayerToChooseBirdOrRandom() {
        FightSetupSelectionState state = new FightSetupSelectionState(4);

        assertFalse(state.allReady(2));

        state.selectBird(0, BirdGame3.BirdType.PIGEON);
        assertFalse(state.allReady(2));

        state.selectRandom(1);
        assertTrue(state.allReady(2));
    }

    @Test
    void clearSelectionResetsBirdRandomAndSkin() {
        FightSetupSelectionState state = new FightSetupSelectionState(4);

        state.selectBirdWithSkin(0, BirdGame3.BirdType.VULTURE, "null-rock");
        state.clearSelection(0);

        assertFalse(state.isRandomSelected(0));
        assertNull(state.selectedBird(0));
        assertNull(state.selectedSkinKey(0));
        assertFalse(state.hasSelection(0));
    }

    @Test
    void randomSelectionClearsPreviousBirdAndSkin() {
        FightSetupSelectionState state = new FightSetupSelectionState(4);

        state.selectBirdWithSkin(1, BirdGame3.BirdType.EAGLE, "gold");
        state.selectRandom(1);

        assertTrue(state.isRandomSelected(1));
        assertNull(state.selectedBird(1));
        assertNull(state.selectedSkinKey(1));
        assertTrue(state.hasSelection(1));
    }

    @Test
    void birdSelectionKeepsSkinForLaterNormalization() {
        FightSetupSelectionState state = new FightSetupSelectionState(4);

        state.selectBirdWithSkin(2, BirdGame3.BirdType.EAGLE, "gold");
        state.selectBird(2, BirdGame3.BirdType.PIGEON);

        assertFalse(state.isRandomSelected(2));
        assertEquals(BirdGame3.BirdType.PIGEON, state.selectedBird(2));
        assertEquals("gold", state.selectedSkinKey(2));
    }

    @Test
    void invalidSlotsAreSafeNoOps() {
        FightSetupSelectionState state = new FightSetupSelectionState(2);

        state.selectBird(-1, BirdGame3.BirdType.PIGEON);
        state.selectRandom(4);
        state.setSelectedSkinKey(3, "ignored");

        assertArrayEquals(new BirdGame3.BirdType[2], state.selectedBirds());
        assertFalse(state.randomSelections()[0]);
        assertFalse(state.randomSelections()[1]);
        assertNull(state.selectedSkinKeys()[0]);
        assertNull(state.selectedSkinKeys()[1]);
    }

    @Test
    void slotCountMustBePositive() {
        assertThrows(IllegalArgumentException.class, () -> new FightSetupSelectionState(0));
    }
}
