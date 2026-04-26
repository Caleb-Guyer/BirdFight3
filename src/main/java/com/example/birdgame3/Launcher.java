package com.example.birdgame3;

import javafx.application.Application;

import java.nio.file.Path;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Launcher {
    private static final Logger LOGGER = Logger.getLogger(Launcher.class.getName());

    public static void main(String[] args) {
        // Install a global uncaught exception handler so crashes during startup
        // are recorded to an easy-to-find file on the Desktop.
        Thread.setDefaultUncaughtExceptionHandler((thread, throwable) -> {
            ThrowableLogSupport.log(
                    LOGGER,
                    Level.SEVERE,
                    "Uncaught exception on thread " + thread.getName(),
                    throwable
            );
            ThrowableLogSupport.writeReport(
                    Path.of(System.getProperty("user.home"), "Desktop", "birdgame3-uncaught.txt"),
                    true,
                    "Uncaught exception on thread: " + thread.getName(),
                    throwable
            );
        });

        try {
            Application.launch(BirdGame3.class, args);
        } catch (Throwable t) {
            // Also log anything that bubbles up here (best-effort)
            ThrowableLogSupport.log(LOGGER, Level.SEVERE, "Throwable from main launch", t);
            ThrowableLogSupport.writeReport(
                    Path.of(System.getProperty("user.home"), "Desktop", "birdgame3-uncaught.txt"),
                    true,
                    "Throwable from main launch",
                    t
            );
            throw t;
        }
    }
}
