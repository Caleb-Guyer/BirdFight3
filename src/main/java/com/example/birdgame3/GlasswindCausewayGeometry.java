package com.example.birdgame3;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** Shared anchor geometry for Glasswind Causeway's visible bridge structure. */
final class GlasswindCausewayGeometry {
    static final double MIRROR_AXIS_X = 3_000.0;
    static final double LEFT_TOWER_X = 1_140.0;
    static final double RIGHT_TOWER_X = 4_860.0;
    static final double TOWER_CABLE_Y = 410.0;
    static final double LEFT_DECK_X = 560.0;
    static final double RIGHT_DECK_X = 5_440.0;
    static final double TRUSS_LEFT_X = 690.0;
    static final double TRUSS_RIGHT_X = 5_310.0;

    private GlasswindCausewayGeometry() {
    }

    record Layout(List<List<StageArtGeometry.Point>> cableSpans,
                  List<StageArtGeometry.Segment> hangers,
                  List<StageArtGeometry.Segment> deckRail,
                  List<StageArtGeometry.Segment> lowerTruss,
                  List<StageArtGeometry.Point> cablePoints,
                  List<StageArtGeometry.Point> allowedOpenEndpoints) {
        Layout {
            cableSpans = cableSpans.stream().map(List::copyOf).toList();
            hangers = List.copyOf(hangers);
            deckRail = List.copyOf(deckRail);
            lowerTruss = List.copyOf(lowerTruss);
            cablePoints = List.copyOf(cablePoints);
            allowedOpenEndpoints = List.copyOf(allowedOpenEndpoints);
        }

        List<StageArtGeometry.Segment> auditedSegments() {
            List<StageArtGeometry.Segment> result = new ArrayList<>();
            for (List<StageArtGeometry.Point> span : cableSpans) result.addAll(toSegments(span));
            result.addAll(hangers);
            result.addAll(deckRail);
            result.addAll(lowerTruss);
            return List.copyOf(result);
        }
    }

    static Layout create(double deckTop) {
        StageArtGeometry.Point leftTower = point(LEFT_TOWER_X, TOWER_CABLE_Y);
        StageArtGeometry.Point centerSag = point(MIRROR_AXIS_X, 1_035.0);
        StageArtGeometry.Point rightTower = point(RIGHT_TOWER_X, TOWER_CABLE_Y);
        StageArtGeometry.Point leftAnchor = point(240.0, 1_430.0);
        StageArtGeometry.Point rightAnchor = point(5_760.0, 1_430.0);

        List<StageArtGeometry.Point> leftOuter = quadratic(
                leftAnchor, point(650.0, 1_165.0), leftTower, 12);
        List<StageArtGeometry.Point> leftMain = quadratic(
                leftTower, point(1_950.0, 1_035.0), centerSag, 16);
        List<StageArtGeometry.Point> rightMain = mirrorReversed(leftMain);
        List<StageArtGeometry.Point> rightOuter = mirrorReversed(leftOuter);
        List<List<StageArtGeometry.Point>> spans = List.of(leftOuter, leftMain, rightMain, rightOuter);

        Set<StageArtGeometry.Point> cablePointSet = new LinkedHashSet<>();
        spans.forEach(cablePointSet::addAll);
        List<StageArtGeometry.Point> hangerTops = new ArrayList<>();
        addEvery(hangerTops, leftOuter, 3, 3, LEFT_DECK_X, RIGHT_DECK_X);
        addEvery(hangerTops, leftMain, 2, 2, LEFT_DECK_X, RIGHT_DECK_X);
        addEvery(hangerTops, rightMain, 2, 2, LEFT_DECK_X, RIGHT_DECK_X);
        addEvery(hangerTops, rightOuter, 3, 3, LEFT_DECK_X, RIGHT_DECK_X);
        hangerTops.sort(Comparator.comparingDouble(StageArtGeometry.Point::x));

        List<StageArtGeometry.Segment> hangers = hangerTops.stream()
                .map(top -> segment(top, point(top.x(), deckTop)))
                .toList();

        List<StageArtGeometry.Point> railPoints = new ArrayList<>();
        railPoints.add(point(LEFT_DECK_X, deckTop));
        for (StageArtGeometry.Point top : hangerTops) railPoints.add(point(top.x(), deckTop));
        railPoints.add(point(RIGHT_DECK_X, deckTop));
        railPoints.sort(Comparator.comparingDouble(StageArtGeometry.Point::x));
        List<StageArtGeometry.Segment> deckRail = toSegments(deduplicate(railPoints));

        double trussTop = deckTop + 70.0;
        double trussBottom = deckTop + 275.0;
        List<StageArtGeometry.Segment> lowerTruss = buildTruss(trussTop, trussBottom, 16);

        return new Layout(spans, hangers, deckRail, lowerTruss,
                List.copyOf(cablePointSet),
                List.of(leftAnchor, rightAnchor,
                        point(LEFT_DECK_X, deckTop), point(RIGHT_DECK_X, deckTop)));
    }

    private static List<StageArtGeometry.Segment> buildTruss(double topY, double bottomY, int cells) {
        List<StageArtGeometry.Point> top = new ArrayList<>();
        List<StageArtGeometry.Point> bottom = new ArrayList<>();
        double cellWidth = (TRUSS_RIGHT_X - TRUSS_LEFT_X) / cells;
        for (int index = 0; index <= cells; index++) {
            double x = TRUSS_LEFT_X + index * cellWidth;
            top.add(point(x, topY));
            bottom.add(point(x, bottomY));
        }
        List<StageArtGeometry.Segment> result = new ArrayList<>();
        result.addAll(toSegments(top));
        result.addAll(toSegments(bottom));
        for (int index = 0; index <= cells; index++) {
            result.add(segment(top.get(index), bottom.get(index)));
        }
        for (int index = 0; index < cells; index++) {
            result.add(segment(top.get(index), bottom.get(index + 1)));
            result.add(segment(bottom.get(index), top.get(index + 1)));
        }
        return List.copyOf(result);
    }

    private static void addEvery(List<StageArtGeometry.Point> destination,
                                 List<StageArtGeometry.Point> span,
                                 int step,
                                 int start,
                                 double minimumX,
                                 double maximumX) {
        for (int index = start; index < span.size() - 1; index += step) {
            StageArtGeometry.Point point = span.get(index);
            if (point.x() >= minimumX && point.x() <= maximumX) destination.add(point);
        }
    }

    private static List<StageArtGeometry.Point> quadratic(StageArtGeometry.Point start,
                                                           StageArtGeometry.Point control,
                                                           StageArtGeometry.Point end,
                                                           int segments) {
        List<StageArtGeometry.Point> points = new ArrayList<>();
        for (int index = 0; index <= segments; index++) {
            double t = index / (double) segments;
            double inverse = 1.0 - t;
            points.add(point(
                    inverse * inverse * start.x() + 2.0 * inverse * t * control.x() + t * t * end.x(),
                    inverse * inverse * start.y() + 2.0 * inverse * t * control.y() + t * t * end.y()));
        }
        return List.copyOf(points);
    }

    private static List<StageArtGeometry.Point> mirrorReversed(List<StageArtGeometry.Point> points) {
        List<StageArtGeometry.Point> result = new ArrayList<>();
        for (int index = points.size() - 1; index >= 0; index--) {
            result.add(points.get(index).mirrorAcross(MIRROR_AXIS_X));
        }
        return List.copyOf(result);
    }

    static List<StageArtGeometry.Segment> toSegments(List<StageArtGeometry.Point> points) {
        List<StageArtGeometry.Segment> result = new ArrayList<>();
        for (int index = 1; index < points.size(); index++) {
            result.add(segment(points.get(index - 1), points.get(index)));
        }
        return List.copyOf(result);
    }

    private static List<StageArtGeometry.Point> deduplicate(List<StageArtGeometry.Point> points) {
        List<StageArtGeometry.Point> result = new ArrayList<>();
        for (StageArtGeometry.Point point : points) {
            if (result.isEmpty() || !StageArtGeometry.near(result.getLast(), point, 0.001)) result.add(point);
        }
        return result;
    }

    private static StageArtGeometry.Point point(double x, double y) {
        return new StageArtGeometry.Point(x, y);
    }

    private static StageArtGeometry.Segment segment(StageArtGeometry.Point start, StageArtGeometry.Point end) {
        return new StageArtGeometry.Segment(start, end);
    }
}
