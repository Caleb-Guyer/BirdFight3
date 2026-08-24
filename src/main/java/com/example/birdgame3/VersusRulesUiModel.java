package com.example.birdgame3;

import java.util.List;

/** Pure presentation model for the uncluttered Smash-style ruleset browser. */
final class VersusRulesUiModel {
    static final int PREVIEW_DETAIL_COUNT = 4;
    static final int MAX_VISIBLE_SELECTION_ROWS = 6;
    static final double CHOICE_HEIGHT = 64.0;
    static final double CHOICE_GAP = 10.0;
    static final double CREATE_HEIGHT = 72.0;
    static final double GROUP_GAP = 14.0;

    record Detail(String label, String value) {
    }

    record Preview(String title, String mode, List<Detail> details) {
    }

    private VersusRulesUiModel() {
    }

    static List<VersusRulesPreset> quickPresets() {
        return List.of(VersusRulesPreset.STANDARD, VersusRulesPreset.COMPETITIVE,
                VersusRulesPreset.CHAOS, VersusRulesPreset.STAMINA);
    }

    static int visibleSelectionRows() {
        return 2 + quickPresets().size(); // Create, quick presets, one custom-slot selector.
    }

    static double selectionStackHeight() {
        int selectableRows = quickPresets().size() + 1;
        return CREATE_HEIGHT + GROUP_GAP + selectableRows * CHOICE_HEIGHT
                + Math.max(0, selectableRows - 1) * CHOICE_GAP;
    }

    static Preview preview(VersusRulesPreset preset, VersusRules rules) {
        VersusRules safeRules = rules == null ? VersusRules.standard() : rules;
        VersusRulesPreset safePreset = preset == null ? VersusRulesPreset.STANDARD : preset;
        boolean stamina = safeRules.staminaMode();
        String title = safePreset == VersusRulesPreset.CUSTOM ? safeRules.name() : safePreset.title;
        return new Preview(title, stamina ? "STAMINA" : "STOCK", List.of(
                new Detail(stamina ? "STAMINA HP" : "STOCKS",
                        stamina ? safeRules.staminaHealth() + " HP" : Integer.toString(safeRules.stockCount())),
                new Detail("TIME LIMIT", safeRules.timeText()),
                new Detail("ITEMS", safeRules.powerUpsEnabled() ? "ON" : "OFF"),
                new Detail("MATCH FORMAT", safeRules.seriesWins() <= 1
                        ? "SINGLE MATCH" : "FIRST TO " + safeRules.seriesWins())
        ));
    }

    static int cycleCustomSlot(int current, int delta) {
        return Math.floorMod(current + delta, VersusRulesLibrary.SLOT_COUNT);
    }

    static String customSlotLabel(VersusRulesLibrary library) {
        if (library == null) return "MY RULESET";
        return library.selected().name() + "  ·  "
                + (library.selectedSlot() + 1) + "/" + VersusRulesLibrary.SLOT_COUNT;
    }
}
