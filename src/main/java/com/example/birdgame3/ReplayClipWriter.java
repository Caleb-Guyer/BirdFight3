package com.example.birdgame3;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.MemoryCacheImageOutputStream;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.Objects;
import java.util.function.BooleanSupplier;

/**
 * Streams a silent Motion JPEG AVI clip using the JDK's JPEG encoder.
 * No simulation state, JavaFX toolkit, external encoder, or live profile is used.
 * Only one RGB frame, one encoded frame, and a small bounded index stay in memory.
 *
 * <p>Call {@link #finish()} to publish the complete clip. Closing or cancelling an
 * unfinished writer removes its temporary file. The destination is never replaced.
 * Instances are confined to one encoding worker; callers must not share them
 * between threads without external synchronization.</p>
 */
final class ReplayClipWriter implements Closeable {
    static final int MAX_SECONDS = 60;
    private static final int MAX_DIMENSION = 4096;
    // Classic AVI has 32-bit offsets; stay below 2 GiB for player compatibility.
    private static final long MAX_FILE_BYTES = Integer.MAX_VALUE;
    private static final float JPEG_QUALITY = 0.85f;

    private final Path target;
    private final Path temporary;
    private final int width;
    private final int height;
    private final int fps;
    private final BufferedImage rgbFrame;
    private final int[] rgbPixels;
    private final int[] frameOffsets;
    private final int[] frameSizes;
    private final ByteArrayOutputStream encoded = new ByteArrayOutputStream(64 * 1024);
    private RandomAccessFile file;
    private ImageWriter jpeg;
    private ImageWriteParam jpegSettings;
    private int frameCount;
    private int largestFrame;
    private long mainHeader;
    private long streamHeader;
    private long moviSizeOffset;
    private long moviStart;
    private boolean finished;

    ReplayClipWriter(Path target, int width, int height, int fps) throws IOException {
        Objects.requireNonNull(target, "target");
        if (width < 1 || height < 1 || width > MAX_DIMENSION || height > MAX_DIMENSION) {
            throw new IllegalArgumentException("Clip dimensions must be between 1 and " + MAX_DIMENSION);
        }
        if (fps < 1 || fps > 60) {
            throw new IllegalArgumentException("Clip frame rate must be between 1 and 60");
        }
        this.target = target.toAbsolutePath().normalize();
        this.width = width;
        this.height = height;
        this.fps = fps;
        if (Files.exists(this.target, LinkOption.NOFOLLOW_LINKS)) {
            throw new FileAlreadyExistsException(this.target.toString());
        }
        var encoders = ImageIO.getImageWritersByFormatName("jpeg");
        if (!encoders.hasNext()) {
            throw new IOException("The JPEG encoder is unavailable");
        }
        rgbFrame = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        rgbPixels = ((DataBufferInt) rgbFrame.getRaster().getDataBuffer()).getData();
        frameOffsets = new int[fps * MAX_SECONDS];
        frameSizes = new int[frameOffsets.length];
        Files.createDirectories(this.target.getParent());
        temporary = Files.createTempFile(this.target.getParent(), ".bf3-clip-", ".part");
        try {
            jpeg = encoders.next();
            jpegSettings = jpeg.getDefaultWriteParam();
            jpegSettings.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
            jpegSettings.setCompressionQuality(JPEG_QUALITY);
            file = new RandomAccessFile(temporary.toFile(), "rw");
            writeHeaders();
        } catch (IOException | RuntimeException failure) {
            discardAfterFailure(failure);
            throw failure;
        }
    }

    /** Copies opaque RGB channels from a row-major ARGB frame; alpha is ignored. */
    void writeFrame(int[] argb) throws IOException {
        requireOpen();
        try {
            if (argb == null || argb.length != rgbPixels.length) {
                throw new IllegalArgumentException("Frame must contain exactly " + rgbPixels.length + " pixels");
            }
            if (frameCount == frameOffsets.length) {
                throw new IOException("Clips are limited to " + MAX_SECONDS + " seconds");
            }
            System.arraycopy(argb, 0, rgbPixels, 0, rgbPixels.length);
            encoded.reset();
            try (var output = new MemoryCacheImageOutputStream(encoded)) {
                jpeg.setOutput(output);
                jpeg.write(null, new IIOImage(rgbFrame, null, null), jpegSettings);
            } finally {
                jpeg.setOutput(null);
            }
            byte[] bytes = encoded.toByteArray();
            long chunkStart = file.getFilePointer();
            long finalSize = chunkStart + 8L + bytes.length + (bytes.length & 1)
                    + 8L + 16L * (frameCount + 1);
            if (finalSize > MAX_FILE_BYTES) {
                throw new IOException("Clip exceeds the AVI file size limit");
            }
            fourCC("00dc");
            littleInt(bytes.length);
            file.write(bytes);
            if ((bytes.length & 1) != 0) {
                file.write(0);
            }
            frameOffsets[frameCount] = (int) (chunkStart - moviStart);
            frameSizes[frameCount] = bytes.length;
            frameCount++;
            largestFrame = Math.max(largestFrame, bytes.length);
        } catch (IOException | RuntimeException failure) {
            discardAfterFailure(failure);
            throw failure;
        }
    }

    int frameCount() {
        return frameCount;
    }

    boolean finished() {
        return finished;
    }

    /** Patches headers, writes the index, and publishes the file without replacing another file. */
    Path finish() throws IOException {
        return finish(this, () -> true);
    }

    /**
     * Coordinates the final rename with asynchronous cancellation. Preparation
     * stays outside the lock; the caller changes its cancellation flag under
     * {@code publicationLock}, so cancellation cannot race past publication.
     */
    Path finish(Object publicationLock, BooleanSupplier publishAllowed) throws IOException {
        if (finished) {
            return target;
        }
        requireOpen();
        try {
            if (frameCount == 0) {
                throw new IOException("Cannot export an empty clip");
            }
            long moviEnd = file.getFilePointer();
            fourCC("idx1");
            littleInt(frameCount * 16);
            for (int i = 0; i < frameCount; i++) {
                fourCC("00dc");
                littleInt(0x10); // AVIIF_KEYFRAME: every JPEG is independently decodable.
                littleInt(frameOffsets[i]);
                littleInt(frameSizes[i]);
            }
            long end = file.getFilePointer();
            patchInt(4, (int) (end - 8));
            patchInt(moviSizeOffset, (int) (moviEnd - moviStart));
            patchInt(mainHeader + 4, largestFrame * fps);
            patchInt(mainHeader + 16, frameCount);
            patchInt(mainHeader + 28, largestFrame);
            patchInt(streamHeader + 32, frameCount);
            patchInt(streamHeader + 36, largestFrame);
            file.getFD().sync();
            releaseResources();
            // ATOMIC_MOVE may replace an existing destination on some providers.
            // A regular same-directory move preserves the no-overwrite guarantee.
            synchronized (publicationLock) {
                if (!publishAllowed.getAsBoolean()) {
                    throw new IOException("Clip export was cancelled");
                }
                Files.move(temporary, target);
                finished = true;
            }
            return target;
        } catch (IOException | RuntimeException failure) {
            discardAfterFailure(failure);
            throw failure;
        }
    }

    void cancel() throws IOException {
        close();
    }

    @Override
    public void close() throws IOException {
        IOException failure = null;
        try {
            releaseResources();
        } catch (IOException e) {
            failure = e;
        }
        if (!finished) {
            try {
                Files.deleteIfExists(temporary);
            } catch (IOException e) {
                if (failure == null) {
                    failure = e;
                } else {
                    failure.addSuppressed(e);
                }
            }
        }
        if (failure != null) {
            throw failure;
        }
    }

    private void writeHeaders() throws IOException {
        fourCC("RIFF");
        littleInt(0);
        fourCC("AVI ");
        long headerList = beginList("hdrl");
        fourCC("avih");
        littleInt(56);
        mainHeader = file.getFilePointer();
        littleInt((int) Math.round(1_000_000.0 / fps));
        littleInt(0); // maximum bytes/sec, patched on finish
        littleInt(0); // padding granularity
        littleInt(0x10); // AVIF_HASINDEX
        littleInt(0); // total frames, patched on finish
        littleInt(0); // initial frames
        littleInt(1); // one video stream, no audio
        littleInt(0); // suggested buffer, patched on finish
        littleInt(width);
        littleInt(height);
        for (int i = 0; i < 4; i++) littleInt(0);

        long streamList = beginList("strl");
        fourCC("strh");
        littleInt(56);
        streamHeader = file.getFilePointer();
        fourCC("vids");
        fourCC("MJPG");
        littleInt(0); // flags
        littleInt(0); // priority and language WORDs
        littleInt(0); // initial frames
        littleInt(1); // scale
        littleInt(fps); // rate / scale gives frames/sec
        littleInt(0); // start
        littleInt(0); // length, patched on finish
        littleInt(0); // suggested buffer, patched on finish
        littleInt((int) (JPEG_QUALITY * 10_000));
        littleInt(0); // variable-size samples
        littleShort(0);
        littleShort(0);
        littleShort(width);
        littleShort(height);

        fourCC("strf");
        littleInt(40); // BITMAPINFOHEADER
        littleInt(40);
        littleInt(width);
        littleInt(height);
        littleShort(1); // planes
        littleShort(24); // bits per pixel
        fourCC("MJPG");
        littleInt(((width * 3 + 3) & ~3) * height);
        littleInt(0); // pixels/metre X
        littleInt(0); // pixels/metre Y
        littleInt(0); // colors used
        littleInt(0); // important colors
        endList(streamList);
        endList(headerList);

        moviSizeOffset = beginList("movi");
        moviStart = moviSizeOffset + 4; // offsets are relative to the movi FOURCC
    }

    private long beginList(String type) throws IOException {
        fourCC("LIST");
        long sizeOffset = file.getFilePointer();
        littleInt(0);
        fourCC(type);
        return sizeOffset;
    }

    private void endList(long sizeOffset) throws IOException {
        long end = file.getFilePointer();
        patchInt(sizeOffset, (int) (end - sizeOffset - 4));
        file.seek(end);
    }

    private void patchInt(long offset, int value) throws IOException {
        file.seek(offset);
        littleInt(value);
    }

    private void fourCC(String value) throws IOException {
        file.writeBytes(value);
    }

    private void littleInt(int value) throws IOException {
        file.writeInt(Integer.reverseBytes(value));
    }

    private void littleShort(int value) throws IOException {
        file.writeShort(Short.reverseBytes((short) value));
    }

    private void requireOpen() throws IOException {
        if (file == null) {
            throw new IOException("Clip writer is closed");
        }
    }

    private void releaseResources() throws IOException {
        try {
            if (file != null) {
                RandomAccessFile openFile = file;
                file = null;
                openFile.close();
            }
        } finally {
            if (jpeg != null) {
                jpeg.dispose();
                jpeg = null;
            }
        }
    }

    private void discardAfterFailure(Exception failure) {
        try {
            close();
        } catch (IOException cleanupFailure) {
            failure.addSuppressed(cleanupFailure);
        }
    }
}
