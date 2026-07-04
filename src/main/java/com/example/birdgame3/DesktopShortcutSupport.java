package com.example.birdgame3;

import java.nio.file.Path;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.Preferences;

/**
 * Creates a desktop shortcut for the packaged Windows build on first launch.
 *
 * <p>Runs only when the game was started from a jpackage image (detected via
 * the {@code jpackage.app-version} system property) on Windows. The shortcut
 * targets the launcher executable that started this process and is written to
 * the shell's real Desktop folder (which handles OneDrive-redirected desktops,
 * unlike {@code user.home/Desktop}). The attempt happens once per player —
 * a preferences flag records it — so deleting the shortcut is respected and
 * the game never recreates it.
 */
final class DesktopShortcutSupport {
    private static final Logger LOGGER = Logger.getLogger(DesktopShortcutSupport.class.getName());
    static final String PREF_KEY = "desktopShortcutOffered";
    static final String SHORTCUT_NAME = "Bird Fight 3.lnk";

    private DesktopShortcutSupport() {
    }

    /** Fire-and-forget: never blocks or fails game startup. */
    static void ensureDesktopShortcutAsync() {
        Thread worker = new Thread(
                () -> ensureDesktopShortcut(Preferences.userNodeForPackage(DesktopShortcutSupport.class)),
                "desktop-shortcut");
        worker.setDaemon(true);
        worker.start();
    }

    static void ensureDesktopShortcut(Preferences prefs) {
        try {
            if (System.getProperty("jpackage.app-version") == null) {
                return; // dev run (IDE/Maven), not a packaged build
            }
            if (!System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("win")) {
                return;
            }
            if (prefs.getBoolean(PREF_KEY, false)) {
                return; // already offered once; respect a deleted shortcut
            }
            Optional<String> command = ProcessHandle.current().info().command();
            if (command.isEmpty()) {
                return;
            }
            Path exe = Path.of(command.get());
            String exeName = exe.getFileName().toString().toLowerCase(Locale.ROOT);
            if (!exeName.endsWith(".exe") || exeName.startsWith("java")) {
                return; // safety: never point a shortcut at a bare JVM
            }

            String script = String.join("; ",
                    "$desktop = [Environment]::GetFolderPath('Desktop')",
                    "$lnk = Join-Path $desktop '" + SHORTCUT_NAME + "'",
                    "if (Test-Path $lnk) { exit 0 }",
                    "$ws = New-Object -ComObject WScript.Shell",
                    "$s = $ws.CreateShortcut($lnk)",
                    "$s.TargetPath = '" + exe + "'",
                    "$s.WorkingDirectory = '" + exe.getParent() + "'",
                    "$s.Save()");
            Process process = new ProcessBuilder(
                    "powershell.exe", "-NoProfile", "-NonInteractive", "-ExecutionPolicy", "Bypass",
                    "-Command", script)
                    .redirectErrorStream(true)
                    .start();
            if (!process.waitFor(20, TimeUnit.SECONDS)) {
                process.destroyForcibly();
            }
            // Mark as offered regardless of outcome so a broken shell can't cause
            // a retry (and a fresh prompt-less attempt) on every single launch.
            prefs.putBoolean(PREF_KEY, true);
        } catch (Exception e) {
            LOGGER.log(Level.FINE, "Desktop shortcut creation skipped", e);
        }
    }
}
