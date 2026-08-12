package com.example.birdgame3;

final class StageSelectLayout {
    static final int MAIN_COLUMNS = 5;
    static final int VARIANT_COLUMNS = 8;
    static final double ROOT_TOP_PADDING = 22.0;
    static final double ROOT_BOTTOM_PADDING = 24.0;
    static final double ROOT_HORIZONTAL_PADDING = 32.0;
    static final double TOP_BAR_HEIGHT = 74.0;
    static final double CARD_WIDTH = 340.0;
    static final double CARD_INNER_WIDTH = 316.0;
    static final double VARIANT_CARD_WIDTH = 214.0;
    static final double VARIANT_CARD_INNER_WIDTH = 190.0;
    static final double CARD_HEIGHT = 154.0;
    static final double HORIZONTAL_GAP = 18.0;
    static final double VERTICAL_GAP = 14.0;
    static final double SECTION_LABEL_HEIGHT = 31.0;
    static final double SECTION_LABEL_GAP = 8.0;
    static final double SECTION_GAP = 14.0;
    static final double CENTER_VERTICAL_MARGIN = 26.0;
    static final double RANDOM_AREA_HEIGHT = 102.0;

    private StageSelectLayout() {
    }

    static int rowsFor(int cardCount) {
        return rowsFor(cardCount, MAIN_COLUMNS);
    }

    static int variantRowsFor(int cardCount) {
        return rowsFor(cardCount, VARIANT_COLUMNS);
    }

    private static int rowsFor(int cardCount, int columns) {
        return cardCount <= 0 ? 0 : (cardCount + columns - 1) / columns;
    }

    static double gridWidth() {
        return MAIN_COLUMNS * CARD_WIDTH + (MAIN_COLUMNS - 1) * HORIZONTAL_GAP;
    }

    static double variantGridWidth() {
        return VARIANT_COLUMNS * VARIANT_CARD_WIDTH + (VARIANT_COLUMNS - 1) * HORIZONTAL_GAP;
    }

    static double gridHeight(int cardCount) {
        int rows = rowsFor(cardCount);
        return rows == 0 ? 0.0 : rows * CARD_HEIGHT + (rows - 1) * VERTICAL_GAP;
    }

    static double variantGridHeight(int cardCount) {
        int rows = variantRowsFor(cardCount);
        return rows == 0 ? 0.0 : rows * CARD_HEIGHT + (rows - 1) * VERTICAL_GAP;
    }

    static double groupedCatalogHeight(int... sectionCardCounts) {
        double height = 0.0;
        int visibleSections = 0;
        for (int count : sectionCardCounts) {
            if (count <= 0) continue;
            if (visibleSections++ > 0) {
                height += SECTION_GAP;
            }
            height += SECTION_LABEL_HEIGHT + SECTION_LABEL_GAP + variantGridHeight(count);
        }
        return height;
    }

    static double requiredScreenHeight(double catalogHeight) {
        return ROOT_TOP_PADDING + ROOT_BOTTOM_PADDING + TOP_BAR_HEIGHT
                + CENTER_VERTICAL_MARGIN + catalogHeight + RANDOM_AREA_HEIGHT;
    }
}
