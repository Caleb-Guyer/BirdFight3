package com.example.birdgame3;

final class FightSetupClawState {
    record PresentationChange(boolean visibleChanged, boolean closedChanged) {
        boolean needsImageRefresh() {
            return visibleChanged || closedChanged;
        }
    }

    private final double[] x;
    private final double[] y;
    private final int[] grabbedSelectorByClaw;
    private final double[] grabOffsetX;
    private final double[] grabOffsetY;
    private final boolean[] visible;
    private final boolean[] closed;
    private final double boundMargin;
    private final double homeLift;

    FightSetupClawState(int clawCount, double boundMargin, double homeLift) {
        if (clawCount < 0) {
            throw new IllegalArgumentException("clawCount cannot be negative");
        }
        this.x = new double[clawCount];
        this.y = new double[clawCount];
        this.grabbedSelectorByClaw = new int[clawCount];
        this.grabOffsetX = new double[clawCount];
        this.grabOffsetY = new double[clawCount];
        this.visible = new boolean[clawCount];
        this.closed = new boolean[clawCount];
        this.boundMargin = Math.max(0.0, boundMargin);
        this.homeLift = Math.max(0.0, homeLift);
        for (int i = 0; i < grabbedSelectorByClaw.length; i++) {
            grabbedSelectorByClaw[i] = -1;
        }
    }

    int clawCount() {
        return x.length;
    }

    boolean isValidClaw(int sourceIdx) {
        return sourceIdx >= 0 && sourceIdx < x.length;
    }

    double x(int sourceIdx) {
        return isValidClaw(sourceIdx) ? x[sourceIdx] : boundMargin;
    }

    double y(int sourceIdx) {
        return isValidClaw(sourceIdx) ? y[sourceIdx] : boundMargin;
    }

    boolean isVisible(int sourceIdx) {
        return isValidClaw(sourceIdx) && visible[sourceIdx];
    }

    boolean isClosed(int sourceIdx) {
        return isValidClaw(sourceIdx) && closed[sourceIdx];
    }

    boolean isGrabbing(int sourceIdx) {
        return grabbedSelector(sourceIdx) >= 0;
    }

    int grabbedSelector(int sourceIdx) {
        return isValidClaw(sourceIdx) ? grabbedSelectorByClaw[sourceIdx] : -1;
    }

    double grabbedSelectorX(int sourceIdx) {
        return x(sourceIdx) + (isValidClaw(sourceIdx) ? grabOffsetX[sourceIdx] : 0.0);
    }

    double grabbedSelectorY(int sourceIdx) {
        return y(sourceIdx) + (isValidClaw(sourceIdx) ? grabOffsetY[sourceIdx] : 0.0);
    }

    void moveHomeFromDock(int sourceIdx, double dockX, double dockY) {
        setPosition(sourceIdx, dockX, Math.max(boundMargin, dockY - homeLift));
    }

    void setPosition(int sourceIdx, double newX, double newY) {
        if (!isValidClaw(sourceIdx)) {
            return;
        }
        x[sourceIdx] = newX;
        y[sourceIdx] = newY;
    }

    boolean moveByDirection(int sourceIdx,
                            double horizontal,
                            double vertical,
                            double dtSeconds,
                            double speed,
                            double boundW,
                            double boundH) {
        if (!isValidClaw(sourceIdx)) {
            return false;
        }
        double length = Math.hypot(horizontal, vertical);
        if (length <= 0.0) {
            return false;
        }
        double distance = Math.max(0.0, speed) * Math.max(0.0, dtSeconds);
        double dx = horizontal / length * distance;
        double dy = vertical / length * distance;
        double maxX = Math.max(boundMargin, boundW - boundMargin);
        double maxY = Math.max(boundMargin, boundH - boundMargin);
        x[sourceIdx] = Math.clamp(x[sourceIdx] + dx, boundMargin, maxX);
        y[sourceIdx] = Math.clamp(y[sourceIdx] + dy, boundMargin, maxY);
        return true;
    }

    boolean beginGrab(int sourceIdx, int selectorIdx, double selectorCenterX, double selectorCenterY) {
        if (!isValidClaw(sourceIdx) || selectorIdx < 0 || isGrabbing(sourceIdx)) {
            return false;
        }
        grabbedSelectorByClaw[sourceIdx] = selectorIdx;
        grabOffsetX[sourceIdx] = selectorCenterX - x[sourceIdx];
        grabOffsetY[sourceIdx] = selectorCenterY - y[sourceIdx];
        return true;
    }

    void clearGrab(int sourceIdx) {
        if (!isValidClaw(sourceIdx)) {
            return;
        }
        grabbedSelectorByClaw[sourceIdx] = -1;
        grabOffsetX[sourceIdx] = 0.0;
        grabOffsetY[sourceIdx] = 0.0;
    }

    boolean selectorGrabbedByOtherClaw(int selectorIdx, int sourceIdx) {
        for (int clawIdx = 0; clawIdx < grabbedSelectorByClaw.length; clawIdx++) {
            if (clawIdx != sourceIdx && grabbedSelectorByClaw[clawIdx] == selectorIdx) {
                return true;
            }
        }
        return false;
    }

    PresentationChange setPresentation(int sourceIdx, boolean newVisible, boolean newClosed) {
        if (!isValidClaw(sourceIdx)) {
            return new PresentationChange(false, false);
        }
        boolean visibleChanged = visible[sourceIdx] != newVisible;
        boolean closedChanged = closed[sourceIdx] != newClosed;
        visible[sourceIdx] = newVisible;
        closed[sourceIdx] = newClosed;
        return new PresentationChange(visibleChanged, closedChanged);
    }
}
