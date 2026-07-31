package com.example.birdgame3;

import javafx.application.Application;
import javafx.stage.Stage;

/**
 * Thin JavaFX lifecycle adapter.
 *
 * <p>The game itself deliberately does not extend {@link Application}. When
 * an {@code Application} subclass is used as the JVM main class, the Java
 * launcher may initialize JavaFX before invoking its {@code main} method.
 * That is too late for renderer properties such as {@code prism.order}.
 */
public final class BirdGame3Application extends Application {
    private final BirdGame3 game = new BirdGame3();

    @Override
    public void start(Stage stage) {
        game.start(stage);
    }

    @Override
    public void stop() throws Exception {
        game.stop();
    }
}
