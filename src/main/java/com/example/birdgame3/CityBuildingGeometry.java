package com.example.birdgame3;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Shared geometry for the playable city facades and their balcony braces.
 *
 * <p>The renderer used to aim an overhanging brace at the collision platform's
 * outer edge.  The visible facade is inset from that edge, which left the brace
 * ending in open air.  Keeping the facade and brace anchors in one model makes
 * that mismatch testable without starting JavaFX.</p>
 */
final class CityBuildingGeometry {
    private static final double MIN_PLAYABLE_ROOF_WIDTH = 150.0;
    private static final double MAIN_ROOF_MIN_WIDTH = 650.0;
    private static final double MIN_PARENT_OVERLAP = 70.0;
    static final double ROOFTOP_RELAY_FOUNDATION_DEPTH = 520.0;

    private CityBuildingGeometry() {
    }

    record Bounds(double x, double y, double width, double height) {
        double right() {
            return x + width;
        }

        double bottom() {
            return y + height;
        }

        boolean contains(StageArtGeometry.Point point, double tolerance) {
            return point.x() >= x - tolerance && point.x() <= right() + tolerance
                    && point.y() >= y - tolerance && point.y() <= bottom() + tolerance;
        }
    }

    record Facade(Platform roof, Bounds bounds) {
    }

    record BalconyBraces(Platform balcony,
                         Facade building,
                         boolean integratedIntoFacade,
                         List<StageArtGeometry.Segment> segments) {
        BalconyBraces {
            segments = List.copyOf(segments);
        }
    }

    record Layout(List<Facade> facades, List<BalconyBraces> balconyBraces) {
        Layout {
            facades = List.copyOf(facades);
            balconyBraces = List.copyOf(balconyBraces);
        }
    }

    record SkylineTower(double worldX,
                        double topY,
                        double width,
                        double foundationY,
                        boolean peaked) {
        double height() {
            return foundationY - topY;
        }
    }

    static Layout create(List<Platform> platforms, double groundY) {
        List<Platform> cityPlatforms = platforms.stream()
                .filter(platform -> platform.x >= 0.0 && platform.x < BirdGame3.WORLD_WIDTH
                        && platform.y < groundY - 2.0
                        && platform.w >= MIN_PLAYABLE_ROOF_WIDTH)
                .sorted(Comparator.comparingDouble((Platform platform) -> platform.y).reversed())
                .toList();
        List<Platform> mainRoofs = cityPlatforms.stream()
                .filter(platform -> platform.w >= MAIN_ROOF_MIN_WIDTH)
                .toList();

        List<Facade> facades = new ArrayList<>();
        List<BalconyBraces> braces = new ArrayList<>();
        for (Platform roof : mainRoofs) {
            facadeFor(roof, groundY).ifPresent(facades::add);
        }

        for (Platform feature : cityPlatforms) {
            if (feature.w >= MAIN_ROOF_MIN_WIDTH) continue;
            Platform parentRoof = mainRoofs.stream()
                    .filter(roof -> horizontalOverlap(feature, roof) >= MIN_PARENT_OVERLAP)
                    .min(Comparator.comparingDouble(roof -> Math.abs(roof.y - feature.y)))
                    .orElse(null);
            if (parentRoof == null) continue;

            if (feature.y < parentRoof.y) {
                facadeFor(feature, parentRoof.y).ifPresent(facades::add);
                continue;
            }

            Facade parentFacade = facadeFor(parentRoof, groundY).orElse(null);
            if (parentFacade != null) {
                braces.add(createBraces(feature, parentFacade));
            }
        }
        return new Layout(facades, braces);
    }

    static double facadeInset(double roofWidth) {
        return Math.clamp(roofWidth * 0.045, 8.0, 28.0);
    }

    static List<SkylineTower> skylineLayer(double spacing,
                                           double topBase,
                                           double foundationY) {
        int count = (int) Math.ceil((BirdGame3.WORLD_WIDTH + 1_400.0) / spacing);
        List<SkylineTower> towers = new ArrayList<>();
        for (int index = -2; index < count; index++) {
            double width = spacing * (0.62 + Math.floorMod(index, 3) * 0.08);
            double topY = topBase + Math.floorMod(index * 197, 560);
            if (Math.floorMod(index, 5) == 0) topY -= 270.0;
            boolean peaked = Math.floorMod(index, 3) == 0;
            towers.add(new SkylineTower(index * spacing, topY, width, foundationY, peaked));
        }
        return List.copyOf(towers);
    }

    static boolean facadeHasFoundation(Facade facade,
                                       List<Platform> platforms,
                                       double groundY,
                                       double tolerance) {
        Bounds bounds = facade.bounds();
        if (Math.abs(bounds.bottom() - groundY) <= tolerance) return true;
        return platforms.stream().anyMatch(platform ->
                Math.abs(platform.y - bounds.bottom()) <= tolerance
                        && Math.min(bounds.right(), platform.x + platform.w)
                        - Math.max(bounds.x(), platform.x) > tolerance);
    }

    static boolean startsOnBalcony(StageArtGeometry.Segment segment,
                                   Platform balcony,
                                   double tolerance) {
        StageArtGeometry.Point start = segment.start();
        return Math.abs(start.y() - (balcony.y + balcony.h)) <= tolerance
                && start.x() >= balcony.x - tolerance
                && start.x() <= balcony.x + balcony.w + tolerance;
    }

    static boolean endsOnVisibleFacade(StageArtGeometry.Segment segment,
                                       Facade facade,
                                       double tolerance) {
        return facade.bounds().contains(segment.end(), tolerance);
    }

    private static java.util.Optional<Facade> facadeFor(Platform roof, double baseY) {
        double roofBottom = roof.y + roof.h;
        if (baseY <= roofBottom + 8.0) return java.util.Optional.empty();
        double inset = facadeInset(roof.w);
        Bounds bounds = new Bounds(
                roof.x + inset,
                roofBottom,
                Math.max(44.0, roof.w - inset * 2.0),
                baseY - roofBottom);
        return java.util.Optional.of(new Facade(roof, bounds));
    }

    private static BalconyBraces createBraces(Platform balcony, Facade building) {
        Bounds facade = building.bounds();
        Platform roof = building.roof();
        boolean integrated = balcony.x >= roof.x - 1.0
                && balcony.x + balcony.w <= roof.x + roof.w + 1.0;
        double undersideY = balcony.y + balcony.h;
        List<StageArtGeometry.Segment> segments = new ArrayList<>();

        if (integrated) {
            double leftMount = balcony.x + 48.0;
            double rightMount = balcony.x + balcony.w - 48.0;
            double endY = Math.clamp(undersideY + 108.0, facade.y(), facade.bottom());
            double leftEndX = Math.clamp(leftMount + 34.0, facade.x(), facade.right());
            double rightEndX = Math.clamp(rightMount - 34.0, facade.x(), facade.right());
            segments.add(segment(leftMount, undersideY, leftEndX, endY));
            segments.add(segment(rightMount, undersideY, rightEndX, endY));
        } else {
            boolean leftSide = balcony.x < roof.x;
            // Anchor to the edge that is actually painted, not the wider
            // collision roof edge hidden behind its overhang.
            double wallX = leftSide ? facade.x() : facade.right();
            double outerX = leftSide ? balcony.x + 34.0 : balcony.x + balcony.w - 34.0;
            double endY = Math.clamp(undersideY + 112.0, facade.y(), facade.bottom());
            segments.add(segment(outerX, undersideY, wallX, endY));
        }
        return new BalconyBraces(balcony, building, integrated, segments);
    }

    private static double horizontalOverlap(Platform first, Platform second) {
        return Math.min(first.x + first.w, second.x + second.w) - Math.max(first.x, second.x);
    }

    private static StageArtGeometry.Segment segment(double startX, double startY,
                                                     double endX, double endY) {
        return new StageArtGeometry.Segment(
                new StageArtGeometry.Point(startX, startY),
                new StageArtGeometry.Point(endX, endY));
    }
}
