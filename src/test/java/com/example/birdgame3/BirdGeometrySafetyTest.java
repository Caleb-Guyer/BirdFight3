package com.example.birdgame3;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class BirdGeometrySafetyTest {

    @Test
    void grappleVineCanAnchorToPlatformsNarrowerThanItsNormalInsets() throws Exception {
        BirdGame3 game = new BirdGame3();
        game.platforms.add(new Platform(300.0, 760.0, 48.0, 30.0));

        Bird bird = new Bird(300.0, BirdGame3.BirdType.PIGEON, 0, game);
        bird.y = 1_000.0;

        Method findAnchor = Bird.class.getDeclaredMethod("findGrappleVineAnchor");
        findAnchor.setAccessible(true);

        Object anchor = assertDoesNotThrow(() -> findAnchor.invoke(bird),
                "Narrow platforms must not invert the grapple anchor clamp bounds");
        assertNotNull(anchor, "The reachable narrow platform should remain a valid grapple anchor");
    }
}
