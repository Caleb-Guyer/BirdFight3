package com.example.birdgame3;

import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.Parent;
import javafx.scene.effect.BlendMode;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;

/**
 * The match renderer deliberately shares one bounded Canvas between the arena
 * and HUD passes.
 *
 * <p>JavaFX allocates a GPU render-target texture for every Canvas. Keeping
 * the old world/HUD pair doubled that allocation on every match restart and
 * could exhaust Prism's render-target pool before detached scenes were
 * collected. A 1920x1080 Canvas also becomes substantially larger on a
 * high-DPI display because Prism allocates it at the output scale. Render at a
 * fixed 1600x900 backing size, retain the game's 1920x1080 logical coordinate
 * system, and reuse the same Canvas for every match.
 */
final class GameplayRenderSurface {
    static final double MAX_BACKING_WIDTH = 1600.0;
    static final double MAX_BACKING_HEIGHT = 900.0;

    private final double logicalWidth;
    private final double logicalHeight;
    private final double backingScaleX;
    private final double backingScaleY;
    private final Canvas canvas;
    private final GraphicsContext graphics;

    GameplayRenderSurface(double logicalWidth, double logicalHeight) {
        this.logicalWidth = Math.max(1.0, logicalWidth);
        this.logicalHeight = Math.max(1.0, logicalHeight);
        double backingWidth = Math.min(this.logicalWidth, MAX_BACKING_WIDTH);
        double backingHeight = Math.min(this.logicalHeight, MAX_BACKING_HEIGHT);
        backingScaleX = backingWidth / this.logicalWidth;
        backingScaleY = backingHeight / this.logicalHeight;

        canvas = new Canvas(backingWidth, backingHeight);
        canvas.setScaleX(1.0 / backingScaleX);
        canvas.setScaleY(1.0 / backingScaleY);
        graphics = canvas.getGraphicsContext2D();
        beginLogicalFrame();
    }

    Canvas canvas() {
        return canvas;
    }

    GraphicsContext worldGraphics() {
        return graphics;
    }

    GraphicsContext hudGraphics() {
        return graphics;
    }

    /**
     * Reparents the persistent Canvas into a fresh match root. Detaching it
     * explicitly prevents completed matches from retaining additional Prism
     * render targets while the next scene is being installed.
     */
    StackPane attachToFreshRoot() {
        detachCanvas();
        StackPane root = new StackPane(canvas);
        root.setMinSize(logicalWidth, logicalHeight);
        root.setPrefSize(logicalWidth, logicalHeight);
        root.setMaxSize(logicalWidth, logicalHeight);
        return root;
    }

    /**
     * Detaches the persistent render target so a non-gameplay scene can reuse
     * it without allocating another full-screen Prism texture.
     */
    Canvas detachCanvas() {
        Parent parent = canvas.getParent();
        if (parent instanceof Pane pane) {
            pane.getChildren().remove(canvas);
        }
        beginLogicalFrame();
        return canvas;
    }

    /**
     * Clears the physical backing texture and resets presentation state before
     * the Canvas is repurposed by another scene.
     */
    void clearForLogicalFrame() {
        graphics.setTransform(1.0, 0.0, 0.0, 1.0, 0.0, 0.0);
        graphics.setGlobalAlpha(1.0);
        graphics.setGlobalBlendMode(BlendMode.SRC_OVER);
        graphics.setEffect(null);
        graphics.clearRect(0.0, 0.0, canvas.getWidth(), canvas.getHeight());
        beginLogicalFrame();
    }

    /**
     * Restores logical 1920x1080 drawing coordinates before each frame.
     * Individual draw methods may save/restore and add camera transforms
     * without knowing that the physical render target is smaller.
     */
    void beginLogicalFrame() {
        graphics.setTransform(backingScaleX, 0.0, 0.0, backingScaleY, 0.0, 0.0);
    }
}
