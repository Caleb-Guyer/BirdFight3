package com.example.birdgame3;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class StoryCutsceneDeterminismAuditTest {
    @Test
    void presentationPlayerCannotConsumeSimulationRandomnessOrWallClockSimulationState() throws IOException {
        String source = Files.readString(Path.of(
                "src", "main", "java", "com", "example", "birdgame3", "StoryCutscenePlayer.java"));

        assertFalse(source.contains("SimRng.next"));
        assertFalse(source.contains("game.random"));
        assertFalse(source.contains("random.next"));
        assertFalse(source.contains("simTick"));
        assertTrue(source.contains("AnimationTimer"));
        assertTrue(source.contains("System.nanoTime()"));
    }

    @Test
    void finalePresentationSequencesStayOutsideTheSimulationRandomStream() throws IOException {
        String escape = Files.readString(Path.of(
                "src", "main", "java", "com", "example", "birdgame3", "CaveEscapeSequence.java"));
        String credits = Files.readString(Path.of(
                "src", "main", "java", "com", "example", "birdgame3", "StillSkyCreditsPlayer.java"));

        assertFalse(escape.contains("SimRng"));
        assertFalse(escape.contains("game.random"));
        assertFalse(credits.contains("SimRng"));
        assertFalse(credits.contains("game.random"));
        assertTrue(escape.contains("STEP_NS = 16_666_667L"));
        assertTrue(escape.contains("while (!failed && !finished && accumulator >= STEP_NS"));
    }

    @Test
    void presentationRebuildsTheGameplayPoseAfterChangingFacing() throws IOException {
        String source = Files.readString(Path.of(
                "src", "main", "java", "com", "example", "birdgame3", "StoryCutscenePlayer.java"));

        int facingAssignment = source.indexOf("actor.facingRight = facingRight;");
        int poseReset = source.indexOf("actor.resetCutsceneVisualPose();");
        int draw = source.indexOf("actor.draw(g);", poseReset);

        assertTrue(facingAssignment >= 0);
        assertTrue(poseReset > facingAssignment);
        assertTrue(draw > poseReset);
    }

    @Test
    void finaleIncludesOnlyTheSelectedBirdsConditionalReaction() {
        StoryCampaign campaign = StoryCampaignContent.create();
        StoryCampaign.Cutscene finale = campaign.scene("s80_eagle_end");

        for (BirdGame3.BirdType selected : BirdGame3.BirdType.values()) {
            long conditional = finale.linesFor(selected).stream()
                    .filter(line -> line.whenSelected() != null)
                    .count();
            assertEquals(1, conditional, selected.name());
            assertTrue(finale.linesFor(selected).stream()
                    .anyMatch(line -> line.whenSelected() == selected));
        }
    }

    @Test
    void actorFacingIsFixedByShotAnchorInsteadOfEntranceOffset() {
        assertTrue(StoryCutscenePlayer.facesRightAtAnchor(600));
        assertTrue(StoryCutscenePlayer.facesRightAtAnchor(960));
        assertFalse(StoryCutscenePlayer.facesRightAtAnchor(1250));
    }

    @Test
    void actorSizeRemainsConstantAcrossCameraZooms() {
        for (double zoom : new double[]{0.86, 1.0, 1.12, 1.20, 1.46}) {
            assertEquals(1.0, zoom * StoryCutscenePlayer.screenConstantActorScale(zoom), 0.000_001);
        }
    }

    @Test
    void actorBlockingEnforcesSeparationWithoutLeavingTheStage() {
        double[] sameCenter = StoryCutscenePlayer.resolveActorCenters(
                960, 960, 390, 270, 1650);
        assertEquals(390, Math.abs(sameCenter[1] - sameCenter[0]), 0.000_001);
        assertTrue(sameCenter[0] >= 270);
        assertTrue(sameCenter[1] <= 1650);

        double[] crowdedEdge = StoryCutscenePlayer.resolveActorCenters(
                1580, 1640, 390, 270, 1650);
        assertEquals(390, Math.abs(crowdedEdge[1] - crowdedEdge[0]), 0.000_001);
        assertTrue(crowdedEdge[0] >= 270);
        assertTrue(crowdedEdge[1] <= 1650);

        double[] reversed = StoryCutscenePlayer.resolveActorCenters(
                1100, 900, 390, 270, 1650);
        assertTrue(reversed[0] > reversed[1], "Directed left/right order must be preserved");
        assertEquals(390, Math.abs(reversed[1] - reversed[0]), 0.000_001);
    }

    @Test
    void permanentDeathsUseAuthoredFallDirection() {
        StoryCampaign campaign = StoryCampaignContent.create();
        StoryCampaign.DialogueLine sparrowFall = campaign.scene("s44_old_sparrow_death").lines().stream()
                .filter(line -> line.speaker().equals("Old Sparrow"))
                .filter(line -> line.motion() == StoryCampaign.ActorMotion.FALL)
                .findFirst()
                .orElseThrow();
        StoryCampaign.DialogueLine eagleFall = campaign.scene("s80_eagle_end").lines().stream()
                .filter(line -> line.speaker().equals("Eagle"))
                .filter(line -> line.motion() == StoryCampaign.ActorMotion.FALL)
                .findFirst()
                .orElseThrow();

        assertEquals(StoryCampaign.ShotStyle.CLOSE, sparrowFall.shot());
        assertEquals(StoryCampaign.ActorMotion.FALL, sparrowFall.motion());
        assertEquals(StoryCampaign.ShotStyle.CLOSE, eagleFall.shot());
        assertEquals(StoryCampaign.ActorMotion.FALL, eagleFall.motion());
    }

    @Test
    void cutsceneCanvasKeepsOneFixedRenderTargetAcrossWindowSizes() throws IOException {
        String source = Files.readString(Path.of(
                "src", "main", "java", "com", "example", "birdgame3", "StoryCutscenePlayer.java"));

        assertTrue(source.contains("if (canvas == null)"));
        assertTrue(source.contains("new Canvas(BACKING_WIDTH, BACKING_HEIGHT)"));
        assertTrue(source.contains("canvas.setScaleX(LOGICAL_WIDTH / BACKING_WIDTH)"));
        assertTrue(source.contains("pane.getChildren().remove(canvas)"));
        assertFalse(source.contains("canvas.widthProperty().bind"));
        assertFalse(source.contains("canvas.heightProperty().bind"));
        assertTrue(source.contains("game.prepareCampaignCutsceneScene(fxScene, root, content);"));
    }

    @Test
    void midMissionCutsceneParksAndRestoresTheFullscreenGameplayScene() throws IOException {
        String source = Files.readString(Path.of(
                "src", "main", "java", "com", "example", "birdgame3", "BirdGame3.java"));

        int phaseBreak = source.indexOf("private void playCampaignMidMissionCutscene");
        int nextMethod = source.indexOf("\n    private ", phaseBreak + 1);
        String method = source.substring(phaseBreak, nextMethod);

        assertTrue(method.contains(
                "Scene resumeScene = parkGameplaySceneForTemporaryFullscreenSwap(currentStage);"));
        assertTrue(method.contains("setCampaignScene(currentStage, resumeScene);"));
        assertFalse(method.contains("setCampaignScene(currentStage, gameplayScene);"));

        int parkingHelper = source.indexOf(
                "private Scene parkGameplaySceneForTemporaryFullscreenSwap");
        int swapHelper = source.indexOf("private void swapFullscreenSceneRoot", parkingHelper);
        String helper = source.substring(parkingHelper, swapHelper);
        assertTrue(helper.contains(
                "swapFullscreenSceneRoot(parkedGameplayScene, activeGameplayScene);"));
        assertTrue(helper.contains("gameplayScene = parkedGameplayScene;"));
    }
}
