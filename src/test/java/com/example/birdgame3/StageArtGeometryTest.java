package com.example.birdgame3;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StageArtGeometryTest {
    @Test
    void acceptsConnectedMirroredStageStructure() {
        StageArtGeometry.Point leftTop = new StageArtGeometry.Point(20.0, 20.0);
        StageArtGeometry.Point centerTop = new StageArtGeometry.Point(50.0, 35.0);
        StageArtGeometry.Point rightTop = new StageArtGeometry.Point(80.0, 20.0);
        StageArtGeometry.Point leftBottom = new StageArtGeometry.Point(20.0, 80.0);
        StageArtGeometry.Point centerBottom = new StageArtGeometry.Point(50.0, 80.0);
        StageArtGeometry.Point rightBottom = new StageArtGeometry.Point(80.0, 80.0);

        List<StageArtGeometry.Segment> structure = List.of(
                new StageArtGeometry.Segment(leftTop, centerTop),
                new StageArtGeometry.Segment(centerTop, rightTop),
                new StageArtGeometry.Segment(leftTop, leftBottom),
                new StageArtGeometry.Segment(rightTop, rightBottom),
                new StageArtGeometry.Segment(leftBottom, centerBottom),
                new StageArtGeometry.Segment(centerBottom, rightBottom));

        StageArtGeometry.AuditResult result = StageArtGeometry.audit(
                structure, List.of(), 100.0, 100.0, 0.01, 50.0);

        assertTrue(result.clean(), () -> String.join(System.lineSeparator(), result.issues()));
    }

    @Test
    void reportsDanglingOutOfBoundsAndAsymmetricArt() {
        List<StageArtGeometry.Segment> broken = List.of(
                new StageArtGeometry.Segment(
                        new StageArtGeometry.Point(10.0, 20.0),
                        new StageArtGeometry.Point(40.0, 20.0)),
                new StageArtGeometry.Segment(
                        new StageArtGeometry.Point(40.0, 20.0),
                        new StageArtGeometry.Point(105.0, 60.0)));

        StageArtGeometry.AuditResult result = StageArtGeometry.audit(
                broken, List.of(), 100.0, 100.0, 0.01, 50.0);

        assertFalse(result.clean());
        assertTrue(result.issues().stream().anyMatch(issue -> issue.contains("dangling endpoint")));
        assertTrue(result.issues().stream().anyMatch(issue -> issue.contains("leaves the stage-art bounds")));
        assertTrue(result.issues().stream().anyMatch(issue -> issue.contains("has no mirror")));
    }
}
