package com.example.birdgame3;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ReplayClipWriterTest {
    @Test
    void writesIndexedVideoWithDecodableOrderedJpegFrames(@TempDir Path dir) throws Exception {
        int width = 32;
        int height = 24;
        Path target = dir.resolve("highlight.avi");
        int[] pixels = new int[width * height];
        try (ReplayClipWriter writer = new ReplayClipWriter(target, width, height, 30)) {
            Arrays.fill(pixels, 0xffee2211);
            writer.writeFrame(pixels);
            Arrays.fill(pixels, 0xff2255ee);
            // An asymmetric frame verifies that the JPEG pixels are not flipped.
            Arrays.fill(pixels, 0, width * height / 2, 0xff11dd33);
            writer.writeFrame(pixels);
            assertEquals(2, writer.frameCount());
            assertFalse(Files.exists(target), "The destination must only appear after successful finish.");
            assertEquals(target.toAbsolutePath(), writer.finish());
            assertEquals(target.toAbsolutePath(), writer.finish(), "Finishing twice is harmless.");
        }

        byte[] bytes = Files.readAllBytes(target);
        ByteBuffer data = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN);
        assertEquals("RIFF", text(bytes, 0));
        assertEquals(bytes.length - 8, data.getInt(4));
        assertEquals("AVI ", text(bytes, 8));
        List<Chunk> root = chunks(bytes, data, 12, bytes.length);
        assertEquals(List.of("LIST:hdrl", "LIST:movi", "idx1"),
                root.stream().map(Chunk::name).toList());
        Chunk headers = root.get(0);
        List<Chunk> headerChunks = chunks(bytes, data, headers.payload + 4, headers.end());
        assertEquals(List.of("avih", "LIST:strl"), headerChunks.stream().map(Chunk::name).toList());
        int main = headerChunks.get(0).payload;
        assertEquals(33_333, data.getInt(main));
        assertEquals(0x10, data.getInt(main + 12));
        assertEquals(2, data.getInt(main + 16));
        assertEquals(1, data.getInt(main + 24), "Video-only clips contain exactly one stream.");
        assertEquals(width, data.getInt(main + 32));
        assertEquals(height, data.getInt(main + 36));

        Chunk streams = headerChunks.get(1);
        List<Chunk> streamChunks = chunks(bytes, data, streams.payload + 4, streams.end());
        assertEquals(List.of("strh", "strf"), streamChunks.stream().map(Chunk::name).toList());
        int stream = streamChunks.get(0).payload;
        assertEquals(56, streamChunks.get(0).size);
        assertEquals("vids", text(bytes, stream));
        assertEquals("MJPG", text(bytes, stream + 4));
        assertEquals(1, data.getInt(stream + 20));
        assertEquals(30, data.getInt(stream + 24));
        assertEquals(2, data.getInt(stream + 32));
        assertEquals(width, data.getShort(stream + 52));
        assertEquals(height, data.getShort(stream + 54));
        int format = streamChunks.get(1).payload;
        assertEquals(40, data.getInt(format));
        assertEquals(width, data.getInt(format + 4));
        assertEquals(height, data.getInt(format + 8));
        assertEquals(24, data.getShort(format + 14));
        assertEquals("MJPG", text(bytes, format + 16));

        Chunk movi = root.get(1);
        List<Chunk> frames = chunks(bytes, data, movi.payload + 4, movi.end());
        assertEquals(2, frames.size());
        Chunk index = root.get(2);
        assertEquals(32, index.size);
        int largest = 0;
        for (int i = 0; i < frames.size(); i++) {
            Chunk frame = frames.get(i);
            int entry = index.payload + i * 16;
            assertEquals("00dc", frame.name);
            assertEquals("00dc", text(bytes, entry));
            assertEquals(0x10, data.getInt(entry + 4));
            assertEquals(frame.payload - 8, movi.payload + data.getInt(entry + 8),
                    "Each index offset must point to that frame's chunk header.");
            assertEquals(frame.size, data.getInt(entry + 12));
            largest = Math.max(largest, frame.size);
            BufferedImage decoded = ImageIO.read(new ByteArrayInputStream(bytes, frame.payload, frame.size));
            assertNotNull(decoded);
            assertEquals(width, decoded.getWidth());
            assertEquals(height, decoded.getHeight());
            if (i == 0) {
                assertColorNear(0xee2211, decoded.getRGB(8, 4));
            } else {
                assertColorNear(0x11dd33, decoded.getRGB(8, 4));
                assertColorNear(0x2255ee, decoded.getRGB(8, 20));
            }
        }
        assertEquals(largest, data.getInt(main + 28));
        assertEquals(largest * 30, data.getInt(main + 4));
        assertEquals(largest, data.getInt(stream + 36));
        assertEquals(List.of(target), files(dir), "Only the published clip should remain.");
    }

    @Test
    void closingOrCancellingDiscardsTemporaryOutput(@TempDir Path dir) throws Exception {
        Path target = dir.resolve("cancel.avi");
        ReplayClipWriter writer = new ReplayClipWriter(target, 8, 8, 30);
        writer.writeFrame(new int[64]);
        assertEquals(1, files(dir).size());
        writer.cancel();
        writer.close();
        assertTrue(files(dir).isEmpty());
        assertThrows(IOException.class, writer::finish);
        assertThrows(IOException.class, () -> writer.writeFrame(new int[64]));

        try (ReplayClipWriter unfinished = new ReplayClipWriter(target, 8, 8, 30)) {
            unfinished.writeFrame(new int[64]);
        }
        assertTrue(files(dir).isEmpty());
    }

    @Test
    void invalidFrameOrEmptyFinishCleansUpImmediately(@TempDir Path dir) throws Exception {
        Path target = dir.resolve("failure.avi");
        try (ReplayClipWriter writer = new ReplayClipWriter(target, 8, 8, 30)) {
            writer.writeFrame(new int[64]);
            assertThrows(IllegalArgumentException.class, () -> writer.writeFrame(new int[63]));
            assertTrue(files(dir).isEmpty());
        }
        try (ReplayClipWriter writer = new ReplayClipWriter(target, 8, 8, 30)) {
            assertThrows(IOException.class, writer::finish);
            assertTrue(files(dir).isEmpty());
        }
    }

    @Test
    void preservesExistingDestinationEvenIfCreatedDuringEncoding(@TempDir Path dir) throws Exception {
        Path target = dir.resolve("existing.avi");
        Files.writeString(target, "keep this");
        assertThrows(FileAlreadyExistsException.class, () -> new ReplayClipWriter(target, 8, 8, 30));
        assertEquals("keep this", Files.readString(target));
        assertEquals(List.of(target), files(dir));

        Path racingTarget = dir.resolve("racing.avi");
        try (ReplayClipWriter writer = new ReplayClipWriter(racingTarget, 8, 8, 30)) {
            writer.writeFrame(new int[64]);
            Files.writeString(racingTarget, "also keep this");
            assertThrows(FileAlreadyExistsException.class, writer::finish);
            assertEquals("also keep this", Files.readString(racingTarget));
        }
        assertEquals(2, files(dir).size(), "Failed finish must remove only its own temporary file.");
    }

    @Test
    void rejectsInvalidSettingsBeforeCreatingFiles(@TempDir Path dir) throws Exception {
        Path target = dir.resolve("invalid.avi");
        assertThrows(IllegalArgumentException.class, () -> new ReplayClipWriter(target, 0, 8, 30));
        assertThrows(IllegalArgumentException.class, () -> new ReplayClipWriter(target, 8, 4097, 30));
        assertThrows(IllegalArgumentException.class, () -> new ReplayClipWriter(target, 8, 8, 0));
        assertThrows(IllegalArgumentException.class, () -> new ReplayClipWriter(target, 8, 8, 61));
        assertTrue(files(dir).isEmpty());
    }

    @Test
    void enforcesDurationLimitWithoutPublishingTruncatedClip(@TempDir Path dir) throws Exception {
        Path target = dir.resolve("long.avi");
        try (ReplayClipWriter writer = new ReplayClipWriter(target, 2, 2, 1)) {
            for (int i = 0; i < ReplayClipWriter.MAX_SECONDS; i++) {
                writer.writeFrame(new int[4]);
            }
            assertEquals(ReplayClipWriter.MAX_SECONDS, writer.frameCount());
            assertThrows(IOException.class, () -> writer.writeFrame(new int[4]));
            assertTrue(files(dir).isEmpty());
        }
    }

    private record Chunk(String name, int payload, int size) {
        int end() { return payload + size; }
    }

    private static List<Chunk> chunks(byte[] bytes, ByteBuffer data, int start, int end) {
        List<Chunk> result = new ArrayList<>();
        int position = start;
        while (position < end) {
            assertTrue(position + 8 <= end, "Every chunk needs a complete header.");
            String id = text(bytes, position);
            int size = data.getInt(position + 4);
            assertTrue(size >= 0 && (long) position + 8 + size <= end, "Chunk must fit inside its parent.");
            result.add(new Chunk(id.equals("LIST") ? id + ":" + text(bytes, position + 8) : id,
                    position + 8, size));
            position += 8 + size + (size & 1);
        }
        assertEquals(end, position, "Chunk padding must end exactly at the parent boundary.");
        return result;
    }

    private static String text(byte[] bytes, int position) {
        return new String(bytes, position, 4, StandardCharsets.US_ASCII);
    }

    private static void assertColorNear(int expected, int actual) {
        for (int shift : new int[]{0, 8, 16}) {
            assertTrue(Math.abs(((expected >> shift) & 0xff) - ((actual >> shift) & 0xff)) < 12,
                    "Decoded JPEG must preserve the input frame's colors.");
        }
    }

    private static List<Path> files(Path dir) throws IOException {
        try (var paths = Files.list(dir)) {
            return paths.sorted().toList();
        }
    }
}
