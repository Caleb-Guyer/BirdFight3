package com.example.birdgame3;

import javafx.application.Application;

public class Launcher {
    public static void main(String[] args) {
        // Install a global uncaught exception handler so crashes during startup
        // are recorded to an easy-to-find file on the Desktop.
        Thread.setDefaultUncaughtExceptionHandler((thread, throwable) -> {
            throwable.printStackTrace();
            try {
                java.io.File out = new java.io.File(System.getProperty("user.home"), "Desktop\\birdgame3-uncaught.txt");
                try (java.io.PrintStream ps = new java.io.PrintStream(new java.io.FileOutputStream(out, true))) {
                    ps.println("---- Uncaught exception on thread: " + thread.getName() + " ----");
                    throwable.printStackTrace(ps);
                    ps.println();
                }
            } catch (Exception ignore) {
            }
        });

        try {
            Application.launch(BirdGame3.class, args);
        } catch (Throwable t) {
            // Also log anything that bubbles up here (best-effort)
            t.printStackTrace();
            try {
                java.io.File out = new java.io.File(System.getProperty("user.home"), "Desktop\\birdgame3-uncaught.txt");
                try (java.io.PrintStream ps = new java.io.PrintStream(new java.io.FileOutputStream(out, true))) {
                    ps.println("---- Throwable from main launch ----");
                    t.printStackTrace(ps);
                    ps.println();
                }
            } catch (Exception ignore) {
            }
            throw t;
        }
    }
}
