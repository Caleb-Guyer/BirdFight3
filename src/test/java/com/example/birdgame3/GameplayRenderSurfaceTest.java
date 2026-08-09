package com.example.birdgame3;

import javafx.scene.canvas.Canvas;
import javafx.scene.layout.StackPane;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GameplayRenderSurfaceTest {

    @Test
    void worldAndHudShareOneBoundedCanvas() {
        GameplayRenderSurface surface =
                new GameplayRenderSurface(BirdGame3.WIDTH, BirdGame3.HEIGHT);
        StackPane root = surface.attachToFreshRoot();

        assertEquals(1, root.getChildren().size());
        assertSame(surface.canvas(), root.getChildren().getFirst());
        assertSame(surface.worldGraphics(), surface.hudGraphics());
        assertSame(surface.canvas(), ((Canvas) root.getChildren().getFirst()));
        assertTrue(surface.canvas().getWidth() <= GameplayRenderSurface.MAX_BACKING_WIDTH);
        assertTrue(surface.canvas().getHeight() <= GameplayRenderSurface.MAX_BACKING_HEIGHT);
    }

    @Test
    void persistentCanvasIsReparentedInsteadOfAllocatedPerMatch() {
        GameplayRenderSurface surface =
                new GameplayRenderSurface(BirdGame3.WIDTH, BirdGame3.HEIGHT);
        StackPane firstRoot = surface.attachToFreshRoot();
        StackPane secondRoot = surface.attachToFreshRoot();

        assertNotSame(firstRoot, secondRoot);
        assertTrue(firstRoot.getChildren().isEmpty());
        assertEquals(1, secondRoot.getChildren().size());
        assertSame(surface.canvas(), secondRoot.getChildren().getFirst());
    }

    @Test
    void persistentCanvasCanBeDetachedForAResultsScene() {
        GameplayRenderSurface surface =
                new GameplayRenderSurface(BirdGame3.WIDTH, BirdGame3.HEIGHT);
        StackPane matchRoot = surface.attachToFreshRoot();

        Canvas resultsCanvas = surface.detachCanvas();

        assertSame(surface.canvas(), resultsCanvas);
        assertTrue(matchRoot.getChildren().isEmpty());
        assertTrue(resultsCanvas.getWidth() <= GameplayRenderSurface.MAX_BACKING_WIDTH);
        assertTrue(resultsCanvas.getHeight() <= GameplayRenderSurface.MAX_BACKING_HEIGHT);
    }

    @Test
    void logicalFrameTransformMapsGameCoordinatesToBackingPixels() {
        GameplayRenderSurface surface =
                new GameplayRenderSurface(BirdGame3.WIDTH, BirdGame3.HEIGHT);

        surface.worldGraphics().setTransform(1.0, 0.0, 0.0, 1.0, 25.0, 30.0);
        surface.beginLogicalFrame();

        assertEquals(surface.canvas().getWidth() / BirdGame3.WIDTH,
                surface.worldGraphics().getTransform().getMxx(), 0.000001);
        assertEquals(surface.canvas().getHeight() / BirdGame3.HEIGHT,
                surface.worldGraphics().getTransform().getMyy(), 0.000001);
        assertEquals(0.0, surface.worldGraphics().getTransform().getTx(), 0.000001);
        assertEquals(0.0, surface.worldGraphics().getTransform().getTy(), 0.000001);
    }
}
