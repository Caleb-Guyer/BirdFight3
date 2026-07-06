package com.example.birdgame3;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.util.Locale;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Self-update for the packaged Windows build via GitHub Releases.
 *
 * <p>On launch (packaged builds only) the game asks the GitHub API for the
 * latest release of this repository. When the release tag is newer than the
 * running {@code jpackage.app-version}, the player is offered the update:
 * accepting downloads the {@code *-win.zip} asset, extracts it, then hands
 * off to a small PowerShell script that waits for the game to exit, copies
 * the new files over the install folder, and relaunches. Publishing an
 * update is just: build with a bumped version, create a GitHub release
 * tagged {@code v<version>}, attach the zip.
 *
 * <p>Every failure path is silent (logged only) — the update check must
 * never break starting the game.
 */
final class GameUpdater {
    private static final Logger LOGGER = Logger.getLogger(GameUpdater.class.getName());
    static final String RELEASES_API =
            "https://api.github.com/repos/Caleb-Guyer/BirdFight3/releases/latest";
    static final String RELEASES_PAGE = "https://github.com/Caleb-Guyer/BirdFight3/releases";
    private static final Pattern TAG_PATTERN = Pattern.compile("\"tag_name\"\\s*:\\s*\"([^\"]+)\"");
    private static final Pattern ASSET_PATTERN =
            Pattern.compile("\"browser_download_url\"\\s*:\\s*\"([^\"]*?-win\\.zip)\"");

    private GameUpdater() {
    }

    /** Fire-and-forget launch-time check; no-op for dev runs. */
    static void checkForUpdatesAsync() {
        String currentVersion = System.getProperty("jpackage.app-version");
        if (currentVersion == null) {
            return; // IDE/Maven run
        }
        if (!System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("win")) {
            return;
        }
        Thread worker = new Thread(() -> checkForUpdates(currentVersion), "update-check");
        worker.setDaemon(true);
        worker.start();
    }

    private static void checkForUpdates(String currentVersion) {
        try {
            Path exe = packagedLauncher();
            if (exe == null) {
                return;
            }
            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(10))
                    .followRedirects(HttpClient.Redirect.NORMAL)
                    .build();
            HttpRequest request = HttpRequest.newBuilder(URI.create(RELEASES_API))
                    .timeout(Duration.ofSeconds(15))
                    .header("Accept", "application/vnd.github+json")
                    .GET()
                    .build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                return; // no releases yet, offline, rate-limited: all fine
            }
            String tag = firstMatch(TAG_PATTERN, response.body());
            String assetUrl = firstMatch(ASSET_PATTERN, response.body());
            if (tag == null || assetUrl == null) {
                return;
            }
            String latest = tag.startsWith("v") || tag.startsWith("V") ? tag.substring(1) : tag;
            if (!isNewerVersion(latest, currentVersion)) {
                return;
            }
            Platform.runLater(() -> offerUpdate(latest, assetUrl, exe, client));
        } catch (Exception e) {
            LOGGER.log(Level.FINE, "Update check failed", e);
        }
    }

    private static Path packagedLauncher() {
        Optional<String> command = ProcessHandle.current().info().command();
        if (command.isEmpty()) {
            return null;
        }
        Path exe = Path.of(command.get());
        String name = exe.getFileName().toString().toLowerCase(Locale.ROOT);
        if (!name.endsWith(".exe") || name.startsWith("java")) {
            return null;
        }
        return exe;
    }

    private static void offerUpdate(String latest, String assetUrl, Path exe, HttpClient client) {
        Alert offer = new Alert(Alert.AlertType.CONFIRMATION,
                "Bird Fight 3 " + latest + " is available. Download and install now?\n"
                        + "The game restarts automatically once the update is ready.",
                ButtonType.OK, ButtonType.CANCEL);
        offer.setTitle("Bird Fight 3 Update");
        offer.setHeaderText("UPDATE AVAILABLE: " + latest);
        Optional<ButtonType> answer = offer.showAndWait();
        if (answer.isEmpty() || answer.get() != ButtonType.OK) {
            return; // offered again on next launch
        }

        Path appRoot = exe.getParent();
        if (!isWritableDirectory(appRoot)) {
            new Alert(Alert.AlertType.INFORMATION,
                    "The install folder is read-only, so the game can't update itself here.\n"
                            + "Download the new version manually from:\n" + RELEASES_PAGE,
                    ButtonType.OK).showAndWait();
            return;
        }

        Label status = new Label("Downloading Bird Fight 3 " + latest + "...");
        ProgressBar bar = new ProgressBar(ProgressBar.INDETERMINATE_PROGRESS);
        bar.setPrefWidth(320);
        VBox box = new VBox(12, status, bar);
        box.setAlignment(Pos.CENTER);
        box.setPadding(new Insets(20));
        Stage progressStage = new Stage(StageStyle.UTILITY);
        progressStage.setTitle("Updating Bird Fight 3");
        progressStage.setScene(new Scene(box));
        progressStage.setResizable(false);
        progressStage.setOnCloseRequest(javafx.event.Event::consume); // no cancel mid-swap
        progressStage.show();

        Thread downloader = new Thread(() -> {
            try {
                Path staged = downloadAndExtract(client, assetUrl, exe.getFileName().toString(),
                        fraction -> Platform.runLater(() -> bar.setProgress(fraction)));
                Platform.runLater(() -> {
                    progressStage.close();
                    launchSwapAndExit(staged, exe);
                });
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Update download failed", e);
                Platform.runLater(() -> {
                    progressStage.close();
                    new Alert(Alert.AlertType.WARNING,
                            "The update could not be downloaded. The game will keep running on "
                                    + "the current version.\nYou can grab it manually from:\n" + RELEASES_PAGE,
                            ButtonType.OK).showAndWait();
                });
            }
        }, "update-download");
        downloader.setDaemon(true);
        downloader.start();
    }

    private interface ProgressSink {
        void accept(double fraction);
    }

    /** Downloads the zip and extracts it; returns the folder holding the new app image contents. */
    private static Path downloadAndExtract(HttpClient client, String assetUrl, String exeName,
                                           ProgressSink progress) throws IOException, InterruptedException {
        Path tempDir = Files.createTempDirectory("birdfight3-update");
        Path zipFile = tempDir.resolve("update.zip");

        HttpRequest request = HttpRequest.newBuilder(URI.create(assetUrl))
                .timeout(Duration.ofMinutes(10))
                .GET()
                .build();
        HttpResponse<InputStream> response = client.send(request, HttpResponse.BodyHandlers.ofInputStream());
        if (response.statusCode() != 200) {
            throw new IOException("Download failed with HTTP " + response.statusCode());
        }
        long totalBytes = response.headers().firstValueAsLong("content-length").orElse(-1L);
        try (InputStream in = response.body(); OutputStream out = Files.newOutputStream(zipFile)) {
            byte[] buffer = new byte[1 << 16];
            long copied = 0;
            int read;
            while ((read = in.read(buffer)) >= 0) {
                out.write(buffer, 0, read);
                copied += read;
                if (totalBytes > 0) {
                    progress.accept((double) copied / totalBytes);
                }
            }
        }

        Path extractDir = tempDir.resolve("extracted");
        Files.createDirectories(extractDir);
        try (ZipInputStream zip = new ZipInputStream(Files.newInputStream(zipFile))) {
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                Path target = extractDir.resolve(entry.getName()).normalize();
                if (!target.startsWith(extractDir)) {
                    throw new IOException("Blocked zip entry escaping extraction dir: " + entry.getName());
                }
                if (entry.isDirectory()) {
                    Files.createDirectories(target);
                } else {
                    Files.createDirectories(target.getParent());
                    Files.copy(zip, target, StandardCopyOption.REPLACE_EXISTING);
                }
            }
        }
        Files.deleteIfExists(zipFile);

        // The zip wraps the app image in one folder ("Bird Fight 3"); find the
        // directory that actually contains the launcher exe.
        Path source = findLauncherDir(extractDir, exeName);
        if (source == null) {
            throw new IOException("Downloaded update does not contain " + exeName);
        }
        return source;
    }

    private static Path findLauncherDir(Path root, String exeName) throws IOException {
        if (Files.exists(root.resolve(exeName))) {
            return root;
        }
        try (var children = Files.list(root)) {
            for (Path child : children.filter(Files::isDirectory).toList()) {
                Path found = findLauncherDir(child, exeName);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }

    /** Hands off to a helper script that waits for this process, swaps files, and relaunches. */
    private static void launchSwapAndExit(Path stagedDir, Path exe) {
        try {
            long pid = ProcessHandle.current().pid();
            Path target = exe.getParent();
            // Values are baked into the script as single-quoted literals so nothing
            // has to survive cmd's argument re-parsing; robocopy (not Copy-Item)
            // reliably merges the new tree over the existing install, and /XF skips
            // the player's own tuning files so an update never clobbers them.
            String script = String.join("\r\n",
                    "try { Wait-Process -Id " + pid + " -Timeout 60 -ErrorAction SilentlyContinue } catch {}",
                    "Start-Sleep -Seconds 1",
                    "robocopy '" + stagedDir + "' '" + target + "' /E /IS /IT /R:3 /W:2 "
                            + "/XF bird-stats.properties /XD sprites | Out-Null",
                    "Start-Process -FilePath '" + exe + "'",
                    "");
            Path scriptFile = Files.createTempFile("birdfight3-swap", ".ps1");
            Files.writeString(scriptFile, script);
            // `cmd /c start` launches the updater in its own process tree so it
            // survives this JVM exiting — a jpackage app-image launcher runs the
            // JVM inside a Windows job object that would otherwise kill children.
            new ProcessBuilder(
                    "cmd.exe", "/c", "start", "\"BirdFight3 Update\"", "/min",
                    "powershell.exe", "-NoProfile", "-NonInteractive", "-ExecutionPolicy", "Bypass",
                    "-WindowStyle", "Hidden", "-File", scriptFile.toString())
                    .start();
            System.exit(0);
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Update handoff failed; keeping current version", e);
        }
    }

    private static boolean isWritableDirectory(Path dir) {
        try {
            Path probe = Files.createTempFile(dir, ".update-probe", ".tmp");
            Files.deleteIfExists(probe);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    static String firstMatch(Pattern pattern, String text) {
        Matcher matcher = pattern.matcher(text);
        return matcher.find() ? matcher.group(1) : null;
    }

    /** Numeric dotted-version comparison; non-numeric segments count as zero. */
    static boolean isNewerVersion(String candidate, String current) {
        int[] a = parseVersion(candidate);
        int[] b = parseVersion(current);
        for (int i = 0; i < Math.max(a.length, b.length); i++) {
            int x = i < a.length ? a[i] : 0;
            int y = i < b.length ? b[i] : 0;
            if (x != y) {
                return x > y;
            }
        }
        return false;
    }

    private static int[] parseVersion(String version) {
        String[] parts = version.trim().split("\\.");
        int[] numbers = new int[parts.length];
        for (int i = 0; i < parts.length; i++) {
            try {
                numbers[i] = Integer.parseInt(parts[i].replaceAll("\\D.*$", ""));
            } catch (NumberFormatException e) {
                numbers[i] = 0;
            }
        }
        return numbers;
    }
}
