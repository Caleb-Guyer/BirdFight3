package com.example.birdgame3;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VersusRulesUiModelTest {
    @Test
    void browserKeepsOneCompactSelectionStack() {
        assertEquals(List.of(VersusRulesPreset.STANDARD, VersusRulesPreset.COMPETITIVE,
                VersusRulesPreset.CHAOS, VersusRulesPreset.STAMINA), VersusRulesUiModel.quickPresets());
        assertEquals(VersusRulesUiModel.MAX_VISIBLE_SELECTION_ROWS,
                VersusRulesUiModel.visibleSelectionRows());
        assertTrue(VersusRulesUiModel.selectionStackHeight() <= 520.0,
                "ruleset choices must fit without scrolling or crowding the footer");
    }

    @Test
    void sidePreviewShowsOnlyFourEssentialDetails() {
        VersusRulesUiModel.Preview stock = VersusRulesUiModel.preview(
                VersusRulesPreset.STANDARD, VersusRules.standard());
        assertEquals("STANDARD SMASH", stock.title());
        assertEquals("STOCK", stock.mode());
        assertEquals(VersusRulesUiModel.PREVIEW_DETAIL_COUNT, stock.details().size());
        assertEquals(List.of("STOCKS", "TIME LIMIT", "ITEMS", "MATCH FORMAT"),
                stock.details().stream().map(VersusRulesUiModel.Detail::label).toList());

        VersusRulesUiModel.Preview stamina = VersusRulesUiModel.preview(
                VersusRulesPreset.STAMINA, VersusRules.stamina().withStaminaHealth(230));
        assertEquals("STAMINA", stamina.mode());
        assertEquals("STAMINA HP", stamina.details().getFirst().label());
        assertEquals("230 HP", stamina.details().getFirst().value());
    }

    @Test
    void customSlotSelectorWrapsAndExposesOnlyTheCurrentSlot() {
        VersusRulesLibrary library = new VersusRulesLibrary();
        library.selectSlot(VersusRulesLibrary.SLOT_COUNT - 1);
        assertEquals(0, VersusRulesUiModel.cycleCustomSlot(library.selectedSlot(), 1));
        assertEquals(VersusRulesLibrary.SLOT_COUNT - 1,
                VersusRulesUiModel.cycleCustomSlot(0, -1));
        assertTrue(VersusRulesUiModel.customSlotLabel(library).endsWith("3/3"));
    }
}
