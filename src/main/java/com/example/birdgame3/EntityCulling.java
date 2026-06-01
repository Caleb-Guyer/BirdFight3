package com.example.birdgame3;

final class EntityCulling {
    private EntityCulling() {
    }

    static boolean isWorldRectOutsideCamera(double x, double y, double width, double height,
                                            double margin, double camX, double camY,
                                            double viewWidth, double viewHeight) {
        double minX = camX - margin;
        double minY = camY - margin;
        double maxX = camX + viewWidth + margin;
        double maxY = camY + viewHeight + margin;
        return x + width < minX || x > maxX || y + height < minY || y > maxY;
    }
}
