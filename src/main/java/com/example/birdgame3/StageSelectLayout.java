package com.example.birdgame3;

/** Fixed 1920x1080 stage-select geometry. Every future stage must preserve the no-scroll contract. */
final class StageSelectLayout {
    static final int GRID_COLUMNS = 10;
    static final int MAX_GRID_ROWS = 5;
    static final double ROOT_TOP_PADDING = 18.0;
    static final double ROOT_BOTTOM_PADDING = 18.0;
    static final double ROOT_HORIZONTAL_PADDING = 26.0;
    static final double TOP_BAR_HEIGHT = 84.0;
    static final double CONTENT_GAP = 22.0;
    static final double CONTENT_VERTICAL_MARGIN = 14.0;
    static final double PREVIEW_WIDTH = 596.0;
    static final double PREVIEW_HEIGHT = 760.0;
    static final double PREVIEW_CANVAS_WIDTH = 548.0;
    static final double PREVIEW_CANVAS_HEIGHT = 308.0;
    static final double TILE_WIDTH = 116.0;
    static final double TILE_HEIGHT = 132.0;
    static final double TILE_IMAGE_WIDTH = 106.0;
    static final double TILE_IMAGE_HEIGHT = 74.0;
    static final double HORIZONTAL_GAP = 8.0;
    static final double VERTICAL_GAP = 10.0;
    static final double LEGEND_HEIGHT = 30.0;
    static final double LEGEND_GAP = 8.0;
    static final double FOOTER_HEIGHT = 44.0;

    private StageSelectLayout() {
    }

    static int rowsFor(int cardCount) {
        return cardCount <= 0 ? 0 : (cardCount + GRID_COLUMNS - 1) / GRID_COLUMNS;
    }

    static int capacity() {
        return GRID_COLUMNS * MAX_GRID_ROWS;
    }

    static double gridWidth() {
        return GRID_COLUMNS * TILE_WIDTH + (GRID_COLUMNS - 1) * HORIZONTAL_GAP;
    }

    static double gridHeight(int tileCount) {
        int rows = rowsFor(tileCount);
        return rows == 0 ? 0.0 : rows * TILE_HEIGHT + (rows - 1) * VERTICAL_GAP;
    }

    static double contentWidth() {
        return PREVIEW_WIDTH + CONTENT_GAP + gridWidth();
    }

    static double requiredScreenHeight(int tileCount) {
        return ROOT_TOP_PADDING + ROOT_BOTTOM_PADDING + TOP_BAR_HEIGHT + CONTENT_VERTICAL_MARGIN
                + Math.max(PREVIEW_HEIGHT, LEGEND_HEIGHT + LEGEND_GAP + gridHeight(tileCount)) + FOOTER_HEIGHT;
    }
}
