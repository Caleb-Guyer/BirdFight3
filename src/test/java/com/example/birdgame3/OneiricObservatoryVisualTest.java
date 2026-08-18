package com.example.birdgame3;

import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OneiricObservatoryVisualTest {
    private static final Path PREVIEW_ROOT = Path.of("src/main/resources/stage-previews");
    private static final List<String> CAPTURES = List.of(
            "main-oneiric-observatory.png",
            "variant-waking-chamber.png");

    @Test
    void capturesKeepCelestialHighlightsAndReadableArchitecture() throws IOException {
        for (String name : CAPTURES) {
            BufferedImage image = read(name);
            assertEquals(640, image.getWidth(), name);
            assertEquals(360, image.getHeight(), name);

            int brightPixels = 0;
            int cyanOrGoldPixels = 0;
            int structuredLowerPixels = 0;
            double luminanceTotal = 0.0;
            int pixelCount = image.getWidth() * image.getHeight();
            int lowerPixelCount = image.getWidth() * (image.getHeight() - 210);
            for (int y = 0; y < image.getHeight(); y++) {
                for (int x = 0; x < image.getWidth(); x++) {
                    int rgb = image.getRGB(x, y);
                    int red = (rgb >>> 16) & 0xFF;
                    int green = (rgb >>> 8) & 0xFF;
                    int blue = rgb & 0xFF;
                    double luminance = red * 0.2126 + green * 0.7152 + blue * 0.0722;
                    luminanceTotal += luminance;
                    if (luminance >= 118.0) brightPixels++;
                    boolean cyan = blue >= 105 && green >= 100 && blue >= red + 24;
                    boolean gold = red >= 145 && green >= 108 && red >= blue + 35;
                    if (cyan || gold) cyanOrGoldPixels++;
                    if (y >= 210 && luminance >= 24.0 && luminance <= 125.0) {
                        structuredLowerPixels++;
                    }
                }
            }

            assertTrue(luminanceTotal / pixelCount >= 27.0,
                    name + " must retain readable deep-space and building layers");
            assertTrue(brightPixels / (double) pixelCount >= 0.018,
                    name + " needs enough bright lens and terrace detail at card size");
            assertTrue(cyanOrGoldPixels / (double) pixelCount >= 0.014,
                    name + " lost its cyan-and-brass observatory identity");
            assertTrue(structuredLowerPixels / (double) lowerPixelCount >= 0.12,
                    name + " lower facade must read as architecture instead of empty black space");
        }
    }

    @Test
    void wakingChamberHasItsOwnVisualIdentity() throws IOException {
        BufferedImage main = read(CAPTURES.get(0));
        BufferedImage waking = read(CAPTURES.get(1));
        assertTrue(averageColorDifference(main, waking) >= 0.045,
                "Waking Chamber must look like an awakened destination, not a recolored main layout");
    }

    @Test
    void mainTerracesAreMirroredAndEverySurfaceBelongsToAReachableStructure() throws Exception {
        BirdGame3 game = prepared(BirdGame3.MapVariant.STANDARD);
        List<Platform> surfaces = playableSurfaces(game);
        for (Platform platform : surfaces) {
            double mirroredX = BirdGame3.WORLD_WIDTH - platform.x - platform.w;
            assertTrue(surfaces.stream().anyMatch(other ->
                            Math.abs(other.x - mirroredX) <= 0.001
                                    && Math.abs(other.y - platform.y) <= 0.001
                                    && Math.abs(other.w - platform.w) <= 0.001),
                    () -> "Oneiric terrace lacks a mirrored architectural partner: " + platform);
        }
        assertAllSurfacesConnectedByReachableSteps(surfaces);
    }

    @Test
    void wakingCourseKeepsEveryLensNearAReachableTerrace() throws Exception {
        BirdGame3 game = prepared(BirdGame3.MapVariant.WAKING_CHAMBER);
        List<Platform> surfaces = playableSurfaces(game);
        assertAllSurfacesConnectedByReachableSteps(surfaces);

        for (int index = 0; index < BirdGame3.OPIUM_LUCID_FRAGMENT_X.length; index++) {
            double lensX = BirdGame3.OPIUM_LUCID_FRAGMENT_X[index];
            double lensY = BirdGame3.OPIUM_LUCID_FRAGMENT_Y[index];
            double nearest = Double.POSITIVE_INFINITY;
            for (Platform platform : surfaces) {
                double surfaceX = Math.clamp(lensX, platform.x, platform.x + platform.w);
                nearest = Math.min(nearest, Math.hypot(lensX - surfaceX, lensY - platform.y));
            }
            int lensIndex = index;
            assertTrue(nearest <= 185.0,
                    () -> "Lucid lens " + lensIndex + " is visually or physically detached from the course");
        }
    }

    private static BirdGame3 prepared(BirdGame3.MapVariant variant) throws Exception {
        BirdGame3 game = new BirdGame3();
        game.selectedMap = BirdGame3.MapType.ONEIRIC_OBSERVATORY;
        game.selectedMapVariant = variant;
        invoke(game, "setupMatchArenaGeometry");
        invoke(game, "applySelectedMapVariantArena");
        return game;
    }

    private static List<Platform> playableSurfaces(BirdGame3 game) {
        return game.platforms.stream()
                .filter(platform -> platform.y < BirdGame3.GROUND_Y + 80.0)
                .toList();
    }

    private static void assertAllSurfacesConnectedByReachableSteps(List<Platform> surfaces) {
        int main = -1;
        for (int index = 0; index < surfaces.size(); index++) {
            if (surfaces.get(index).w >= 3_000.0) {
                main = index;
                break;
            }
        }
        assertTrue(main >= 0, "Oneiric layout needs a continuous observatory roof");

        boolean[] reached = new boolean[surfaces.size()];
        reached[main] = true;
        ArrayDeque<Integer> queue = new ArrayDeque<>();
        queue.add(main);
        while (!queue.isEmpty()) {
            Platform current = surfaces.get(queue.removeFirst());
            for (int candidateIndex = 0; candidateIndex < surfaces.size(); candidateIndex++) {
                if (reached[candidateIndex]) continue;
                Platform candidate = surfaces.get(candidateIndex);
                double verticalGap = Math.abs(current.y - candidate.y);
                double horizontalGap = Math.max(0.0,
                        Math.max(current.x, candidate.x)
                                - Math.min(current.x + current.w, candidate.x + candidate.w));
                if (verticalGap <= 305.0 && horizontalGap <= 230.0) {
                    reached[candidateIndex] = true;
                    queue.add(candidateIndex);
                }
            }
        }
        List<Platform> detached = new ArrayList<>();
        for (int index = 0; index < reached.length; index++) {
            if (!reached[index]) detached.add(surfaces.get(index));
        }
        assertTrue(detached.isEmpty(), () -> "Detached or unreachable Oneiric terraces: " + detached);
    }

    private static void invoke(BirdGame3 game, String methodName) throws Exception {
        Method method = BirdGame3.class.getDeclaredMethod(methodName);
        method.setAccessible(true);
        method.invoke(game);
    }

    private static BufferedImage read(String name) throws IOException {
        BufferedImage image = ImageIO.read(PREVIEW_ROOT.resolve(name).toFile());
        assertNotNull(image, name);
        return image;
    }

    private static double averageColorDifference(BufferedImage first, BufferedImage second) {
        assertEquals(first.getWidth(), second.getWidth());
        assertEquals(first.getHeight(), second.getHeight());
        double total = 0.0;
        int samples = 0;
        for (int y = 0; y < first.getHeight(); y += 2) {
            for (int x = 0; x < first.getWidth(); x += 2) {
                int a = first.getRGB(x, y);
                int b = second.getRGB(x, y);
                total += Math.abs(((a >>> 16) & 0xFF) - ((b >>> 16) & 0xFF));
                total += Math.abs(((a >>> 8) & 0xFF) - ((b >>> 8) & 0xFF));
                total += Math.abs((a & 0xFF) - (b & 0xFF));
                samples++;
            }
        }
        return total / (samples * 3.0 * 255.0);
    }
}
