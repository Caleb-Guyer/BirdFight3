package com.example.birdgame3;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Small, renderer-independent geometry audit for code-drawn stage structures.
 *
 * <p>Stage art is usually assembled from many unrelated stroke calls.  That
 * makes it easy for a cable, brace, or rail to move without the line that was
 * supposed to meet it.  Maps can expose their important structural strokes as
 * segments and use this audit to catch dangling joints, out-of-bounds art, and
 * broken mirror symmetry without starting JavaFX.</p>
 */
final class StageArtGeometry {
    private StageArtGeometry() {
    }

    record Point(double x, double y) {
        Point mirrorAcross(double axisX) {
            return new Point(axisX * 2.0 - x, y);
        }
    }

    record Segment(Point start, Point end) {
        Segment mirrorAcross(double axisX) {
            return new Segment(start.mirrorAcross(axisX), end.mirrorAcross(axisX));
        }
    }

    record AuditResult(List<String> issues) {
        AuditResult {
            issues = List.copyOf(issues);
        }

        boolean clean() {
            return issues.isEmpty();
        }
    }

    static AuditResult audit(Collection<Segment> segments,
                             Collection<Point> allowedOpenEndpoints,
                             double width,
                             double height,
                             double tolerance,
                             Double mirrorAxisX) {
        List<Segment> lines = List.copyOf(segments);
        List<Point> allowed = List.copyOf(allowedOpenEndpoints);
        List<String> issues = new ArrayList<>();

        for (int index = 0; index < lines.size(); index++) {
            Segment line = lines.get(index);
            if (!finite(line.start()) || !finite(line.end())) {
                issues.add("segment " + index + " contains a non-finite coordinate");
                continue;
            }
            if (!inBounds(line.start(), width, height, tolerance)
                    || !inBounds(line.end(), width, height, tolerance)) {
                issues.add("segment " + index + " leaves the stage-art bounds");
            }
            if (near(line.start(), line.end(), tolerance)) {
                issues.add("segment " + index + " has no visible length");
            }
        }

        for (Segment line : lines) {
            inspectEndpoint(line.start(), lines, allowed, tolerance, issues);
            inspectEndpoint(line.end(), lines, allowed, tolerance, issues);
        }

        if (mirrorAxisX != null) {
            for (int index = 0; index < lines.size(); index++) {
                Segment reflected = lines.get(index).mirrorAcross(mirrorAxisX);
                boolean matched = lines.stream().anyMatch(candidate -> sameSegment(candidate, reflected, tolerance));
                if (!matched) {
                    issues.add("segment " + index + " has no mirror across x=" + mirrorAxisX);
                }
            }
        }
        return new AuditResult(issues);
    }

    private static void inspectEndpoint(Point endpoint,
                                        List<Segment> lines,
                                        List<Point> allowed,
                                        double tolerance,
                                        List<String> issues) {
        if (allowed.stream().anyMatch(point -> near(point, endpoint, tolerance))) return;
        int connections = 0;
        for (Segment line : lines) {
            if (near(line.start(), endpoint, tolerance)) connections++;
            if (near(line.end(), endpoint, tolerance)) connections++;
        }
        if (connections < 2) {
            String message = "dangling endpoint at " + format(endpoint);
            if (!issues.contains(message)) issues.add(message);
        }
    }

    static boolean near(Point first, Point second, double tolerance) {
        return Math.abs(first.x() - second.x()) <= tolerance
                && Math.abs(first.y() - second.y()) <= tolerance;
    }

    private static boolean sameSegment(Segment first, Segment second, double tolerance) {
        return near(first.start(), second.start(), tolerance) && near(first.end(), second.end(), tolerance)
                || near(first.start(), second.end(), tolerance) && near(first.end(), second.start(), tolerance);
    }

    private static boolean finite(Point point) {
        return Double.isFinite(point.x()) && Double.isFinite(point.y());
    }

    private static boolean inBounds(Point point, double width, double height, double tolerance) {
        return point.x() >= -tolerance && point.x() <= width + tolerance
                && point.y() >= -tolerance && point.y() <= height + tolerance;
    }

    private static String format(Point point) {
        return "(" + Math.round(point.x() * 10.0) / 10.0
                + ", " + Math.round(point.y() * 10.0) / 10.0 + ")";
    }
}
