package com.example.birdgame3;

import java.util.Arrays;

/** Three persistent user-named rules slots, independent of the quick presets. */
final class VersusRulesLibrary {
    static final int SLOT_COUNT = 3;
    private final VersusRules[] slots = defaultSlots();
    private int selectedSlot;

    int selectedSlot() {
        return selectedSlot;
    }

    void selectSlot(int slot) {
        selectedSlot = Math.clamp(slot, 0, SLOT_COUNT - 1);
    }

    VersusRules selected() {
        return slots[selectedSlot];
    }

    VersusRules slot(int slot) {
        return slots[Math.clamp(slot, 0, SLOT_COUNT - 1)];
    }

    void setSlot(int slot, VersusRules rules) {
        int safeSlot = Math.clamp(slot, 0, SLOT_COUNT - 1);
        slots[safeSlot] = rules == null ? defaultSlots()[safeSlot] : rules;
    }

    String[] encodedSlots() {
        return Arrays.stream(slots).map(VersusRules::encode).toArray(String[]::new);
    }

    void restore(int selected, String[] encoded) {
        VersusRules[] defaults = defaultSlots();
        for (int i = 0; i < SLOT_COUNT; i++) {
            String value = encoded != null && i < encoded.length ? encoded[i] : null;
            slots[i] = VersusRules.decode(value, defaults[i]);
        }
        selectSlot(selected);
    }

    private static VersusRules[] defaultSlots() {
        return new VersusRules[]{
                VersusRules.standard().withName("MY RULES"),
                VersusRules.competitive().withName("TOURNAMENT"),
                VersusRules.chaos().withName("PARTY NIGHT")
        };
    }
}
