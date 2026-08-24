package com.example.birdgame3;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * Saves and loads {@link MatchReplay}s as versioned, gzip-compressed binary
 * files in the {@code replays/} folder next to the game. A typical match is a
 * few dozen kilobytes. The store keeps the newest {@link #MAX_KEPT} current-
 * revision files and prunes the rest. Legacy replays remain visible and are
 * never automatically removed; corrupt or future-versioned files are skipped.
 */
final class ReplayStore {
    private static final Logger LOGGER = Logger.getLogger(ReplayStore.class.getName());
    static final String DIR_NAME = "replays";
    static final String FILE_EXTENSION = ".bf3replay";
    static final int MAX_KEPT = 30;
    private static final int MAGIC = 0x42463352; // "BF3R"
    static final int VERSION = 4;
    private static final DateTimeFormatter FILE_STAMP =
            DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss", Locale.ROOT);

    record SavedReplay(Path file, MatchReplay replay) {
    }

    private ReplayStore() {
    }

    static Path defaultDir() {
        return Path.of(System.getProperty("user.dir"), DIR_NAME);
    }

    /** Saves to the default folder and prunes old files; returns the file or null on failure. */
    static Path save(MatchReplay replay) {
        Path saved = save(defaultDir(), replay);
        if (saved != null) {
            prune(defaultDir());
        }
        return saved;
    }

    static Path save(Path dir, MatchReplay replay) {
        if (replay == null || !replay.usable() || !replay.selfContained()) {
            return null;
        }
        try {
            Files.createDirectories(dir);
            String stamp = LocalDateTime.ofInstant(
                    Instant.ofEpochMilli(replay.timestampMillis > 0 ? replay.timestampMillis : System.currentTimeMillis()),
                    ZoneId.systemDefault()).format(FILE_STAMP);
            String map = replay.mapName == null ? "match" : replay.mapName.toLowerCase(Locale.ROOT);
            Path file = dir.resolve("bf3-" + stamp + "-" + map + FILE_EXTENSION);
            for (int n = 2; Files.exists(file); n++) {
                file = dir.resolve("bf3-" + stamp + "-" + map + "-" + n + FILE_EXTENSION);
            }
            try (DataOutputStream out = new DataOutputStream(
                    new GZIPOutputStream(Files.newOutputStream(file)))) {
                write(out, replay);
            }
            return file;
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Failed to save replay", e);
            return null;
        }
    }

    static MatchReplay load(Path file) {
        try (DataInputStream in = new DataInputStream(
                new GZIPInputStream(Files.newInputStream(file)))) {
            return read(in);
        } catch (IOException | RuntimeException e) {
            LOGGER.log(Level.FINE, "Failed to load replay " + file, e);
            return null;
        }
    }

    /** All loadable replays in the folder, newest first. */
    static List<SavedReplay> listAll() {
        return listAll(defaultDir());
    }

    static List<SavedReplay> listAll(Path dir) {
        List<SavedReplay> result = new ArrayList<>();
        if (!Files.isDirectory(dir)) {
            return result;
        }
        try (var files = Files.list(dir)) {
            for (Path file : files
                    .filter(p -> p.getFileName().toString().endsWith(FILE_EXTENSION))
                    .sorted(Comparator.comparing((Path p) -> p.getFileName().toString()).reversed())
                    .toList()) {
                MatchReplay replay = load(file);
                if (replay != null) {
                    result.add(new SavedReplay(file, replay));
                }
            }
        } catch (IOException e) {
            LOGGER.log(Level.FINE, "Failed to list replays", e);
        }
        return result;
    }

    static boolean delete(Path file) {
        try {
            return Files.deleteIfExists(file);
        } catch (IOException e) {
            LOGGER.log(Level.FINE, "Failed to delete replay " + file, e);
            return false;
        }
    }

    static void prune(Path dir) {
        if (!Files.isDirectory(dir)) {
            return;
        }
        try (var files = Files.list(dir)) {
            List<Path> sorted = files
                    .filter(p -> p.getFileName().toString().endsWith(FILE_EXTENSION))
                    .sorted(Comparator.comparing((Path p) -> p.getFileName().toString()).reversed())
                    .filter(p -> {
                        MatchReplay replay = load(p);
                        return replay != null && replay.compatibleWithCurrentSimulation();
                    })
                    .toList();
            for (int i = MAX_KEPT; i < sorted.size(); i++) {
                delete(sorted.get(i));
            }
        } catch (IOException e) {
            LOGGER.log(Level.FINE, "Failed to prune replays", e);
        }
    }

    private static void write(DataOutputStream out, MatchReplay replay) throws IOException {
        out.writeInt(MAGIC);
        out.writeInt(VERSION);
        out.writeInt(replay.simulationRevision);
        out.writeLong(replay.seed);
        out.writeInt(replay.playerCount);
        out.writeUTF(nullToEmpty(replay.mapName));
        out.writeUTF(nullToEmpty(replay.mapVariantName));
        out.writeLong(replay.timestampMillis);
        out.writeUTF(nullToEmpty(replay.winnerLabel));
        out.writeBoolean(replay.teamModeEnabled);
        out.writeBoolean(replay.mutatorModeEnabled);
        out.writeUTF(nullToEmpty(replay.versusRulesEncoded));
        for (int i = 0; i < replay.playerCount; i++) {
            out.writeUTF(nullToEmpty(replay.slotBirdTypes[i]));
            out.writeBoolean(replay.slotIsAi[i]);
            out.writeInt(replay.slotTeams[i]);
            out.writeUTF(nullToEmpty(replay.slotSkinKeys[i]));
            out.writeDouble(replay.slotBaseSize[i]);
            out.writeDouble(replay.slotBasePower[i]);
            out.writeDouble(replay.slotBaseSpeed[i]);
        }
        out.writeInt(replay.dashTaps.size());
        for (MatchReplay.DashTap tap : replay.dashTaps) {
            out.writeLong(tap.tick());
            out.writeInt(tap.playerIndex());
            out.writeInt(tap.dir());
        }
        out.writeInt(replay.frames.size());
        for (int[] masks : replay.frames) {
            for (int p = 0; p < replay.playerCount; p++) {
                out.writeInt(p < masks.length ? masks[p] : 0);
            }
        }
    }

    private static MatchReplay read(DataInputStream in) throws IOException {
        if (in.readInt() != MAGIC) {
            throw new IOException("Not a BirdFight3 replay file");
        }
        int version = in.readInt();
        if (version < 1 || version > VERSION) {
            throw new IOException("Unsupported replay version " + version);
        }
        int simulationRevision = version >= 2 ? in.readInt() : 1;
        long seed = in.readLong();
        int playerCount = in.readInt();
        if (playerCount <= 0 || playerCount > 64) {
            throw new IOException("Corrupt replay: playerCount " + playerCount);
        }
        MatchReplay replay = new MatchReplay(seed, playerCount, simulationRevision);
        replay.mapName = emptyToNull(in.readUTF());
        replay.mapVariantName = version >= 3 ? emptyToNull(in.readUTF()) : null;
        replay.timestampMillis = in.readLong();
        replay.winnerLabel = in.readUTF();
        replay.teamModeEnabled = in.readBoolean();
        replay.mutatorModeEnabled = in.readBoolean();
        replay.versusRulesEncoded = version >= 4 ? emptyToNull(in.readUTF()) : null;
        replay.slotBirdTypes = new String[playerCount];
        replay.slotIsAi = new boolean[playerCount];
        replay.slotTeams = new int[playerCount];
        replay.slotSkinKeys = new String[playerCount];
        replay.slotBaseSize = new double[playerCount];
        replay.slotBasePower = new double[playerCount];
        replay.slotBaseSpeed = new double[playerCount];
        for (int i = 0; i < playerCount; i++) {
            replay.slotBirdTypes[i] = emptyToNull(in.readUTF());
            replay.slotIsAi[i] = in.readBoolean();
            replay.slotTeams[i] = in.readInt();
            replay.slotSkinKeys[i] = emptyToNull(in.readUTF());
            replay.slotBaseSize[i] = in.readDouble();
            replay.slotBasePower[i] = in.readDouble();
            replay.slotBaseSpeed[i] = in.readDouble();
        }
        int tapCount = in.readInt();
        if (tapCount < 0 || tapCount > 1_000_000) {
            throw new IOException("Corrupt replay: tapCount " + tapCount);
        }
        for (int i = 0; i < tapCount; i++) {
            replay.dashTaps.add(new MatchReplay.DashTap(in.readLong(), in.readInt(), in.readInt()));
        }
        int frameCount = in.readInt();
        if (frameCount < 0 || frameCount > MatchReplay.MAX_FRAMES) {
            throw new IOException("Corrupt replay: frameCount " + frameCount);
        }
        for (int f = 0; f < frameCount; f++) {
            int[] masks = new int[playerCount];
            for (int p = 0; p < playerCount; p++) {
                masks[p] = in.readInt();
            }
            replay.frames.add(masks);
        }
        return replay;
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    private static String emptyToNull(String value) {
        return value == null || value.isEmpty() ? null : value;
    }
}
