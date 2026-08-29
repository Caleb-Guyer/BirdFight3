package com.example.birdgame3;

import javafx.scene.input.KeyCode;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CampaignCutsceneInputTest {

    @Test
    void directionReleasedWhileCutsceneOwnsTheStageDoesNotStayLatched() {
        BirdGame3 game = new BirdGame3();
        game.activePlayers = 1;
        KeyCode left = game.leftKeyForPlayer(0);

        game.pressedKeys.add(left);
        game.setLocalActionsForKey(left, true);
        assertTrue(game.isLeftPressed(0));

        game.beginCampaignCutsceneInputCapture();
        game.noteCampaignCutsceneKeyState(left, false);
        game.finishCampaignCutsceneInputCapture(true);

        assertFalse(game.isLeftPressed(0),
                "A direction held when a cutscene starts must not stay latched after gameplay resumes.");
        assertTrue(game.pressedKeys.isEmpty());
    }

    @Test
    void directionStillHeldWhenCutsceneEndsRemainsResponsive() {
        BirdGame3 game = new BirdGame3();
        game.activePlayers = 1;
        KeyCode right = game.rightKeyForPlayer(0);

        game.pressedKeys.add(right);
        game.setLocalActionsForKey(right, true);

        game.beginCampaignCutsceneInputCapture();
        assertFalse(game.isRightPressed(0),
                "Gameplay input must stay neutral while the cutscene owns the Stage.");
        game.finishCampaignCutsceneInputCapture(true);

        assertTrue(game.isRightPressed(0),
                "A direction that is physically held through the cutscene must work immediately on resume.");
        assertTrue(game.pressedKeys.contains(right));
    }

    @Test
    void heldDirectionIsNotRestoredWhenCutsceneReturnsToAMenu() {
        BirdGame3 game = new BirdGame3();
        game.activePlayers = 1;
        KeyCode right = game.rightKeyForPlayer(0);

        game.pressedKeys.add(right);
        game.setLocalActionsForKey(right, true);

        game.beginCampaignCutsceneInputCapture();
        game.finishCampaignCutsceneInputCapture(false);

        assertFalse(game.isRightPressed(0));
        assertTrue(game.pressedKeys.isEmpty());
    }
}
