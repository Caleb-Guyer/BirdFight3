package com.example.birdgame3;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

final class ThrowableLogSupport {
    private ThrowableLogSupport() {
    }

    static void log(Logger logger, Level level, String message, Throwable throwable) {
        if (logger == null || level == null || throwable == null) {
            return;
        }
        logger.log(level, message, throwable);
    }

    static void writeReport(Path path, boolean append, String heading, Throwable throwable) {
        if (path == null || throwable == null) {
            return;
        }

        try {
            Path parent = path.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            OpenOption[] options = append
                    ? new OpenOption[]{StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.APPEND}
                    : new OpenOption[]{StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING};
            try (BufferedWriter writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8, options)) {
                if (heading != null && !heading.isBlank()) {
                    writer.write("---- " + heading + " @ " + Instant.now() + " ----");
                    writer.newLine();
                }
                appendThrowable(writer, throwable, "", Collections.newSetFromMap(new IdentityHashMap<>()));
                writer.newLine();
            }
        } catch (IOException ignore) {
        }
    }

    private static void appendThrowable(BufferedWriter writer,
                                        Throwable throwable,
                                        String indent,
                                        Set<Throwable> visited) throws IOException {
        if (throwable == null) {
            return;
        }
        if (!visited.add(throwable)) {
            writer.write(indent + "[circular throwable reference] " + throwable.getClass().getName());
            writer.newLine();
            return;
        }

        writer.write(indent + throwable.getClass().getName());
        String message = throwable.getMessage();
        if (message != null && !message.isBlank()) {
            writer.write(": " + message);
        }
        writer.newLine();

        for (StackTraceElement frame : throwable.getStackTrace()) {
            writer.write(indent + "\tat " + frame);
            writer.newLine();
        }

        for (Throwable suppressed : throwable.getSuppressed()) {
            writer.write(indent + "Suppressed:");
            writer.newLine();
            appendThrowable(writer, suppressed, indent + "    ", visited);
        }

        Throwable cause = throwable.getCause();
        if (cause != null) {
            writer.write(indent + "Caused by:");
            writer.newLine();
            appendThrowable(writer, cause, indent + "    ", visited);
        }
    }
}
