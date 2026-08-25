package com.example.birdgame3;

import javafx.scene.input.KeyCode;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BirdGame3PauseReferenceTest {
    private static final String CONTROL_ACTION_CLASS = "com.example.birdgame3.BirdGame3$ControlAction";

    @Test
    void pauseReferenceUsesThePlayersSavedKeyboardBinding() throws Exception {
        BirdGame3 game = new BirdGame3();
        Class<?> actionType = Class.forName(CONTROL_ACTION_CLASS);
        Object attack = enumValue(actionType, "ATTACK");
        Method assign = BirdGame3.class.getDeclaredMethod(
                "assignControlBinding", int.class, actionType, KeyCode.class);
        assign.setAccessible(true);
        assign.invoke(game, 0, attack, KeyCode.Z);

        Method label = BirdGame3.class.getDeclaredMethod("pauseKeyboardBinding", int.class, actionType);
        label.setAccessible(true);
        assertEquals("Z", label.invoke(game, 0, attack));
    }

    @Test
    void directionalSpecialReferenceUsesActualPlayerKeys() throws Exception {
        BirdGame3 game = new BirdGame3();
        Class<?> actionType = Class.forName(CONTROL_ACTION_CLASS);
        Method assign = BirdGame3.class.getDeclaredMethod(
                "assignControlBinding", int.class, actionType, KeyCode.class);
        assign.setAccessible(true);
        assign.invoke(game, 0, enumValue(actionType, "SPECIAL"), KeyCode.C);
        assign.invoke(game, 0, enumValue(actionType, "JUMP"), KeyCode.V);

        Method binding = BirdGame3.class.getDeclaredMethod(
                "pauseDirectionalSpecialBinding", int.class, String.class);
        binding.setAccessible(true);
        assertEquals("V + C", binding.invoke(game, 0, "UP"));
        assertEquals("C (NO DIRECTION)", binding.invoke(game, 0, "NEUTRAL"));
    }

    @Test
    void classicRestartAndExitCopyClearlyDescribeTheirConsequences() throws Exception {
        BirdGame3 game = new BirdGame3();
        set(game, "classicModeActive", true);

        assertEquals("FORFEIT ENCOUNTER", invokeString(game, "pauseRestartLabel"));
        assertEquals("ABANDON RUN", invokeString(game, "pauseExitLabel"));
        assertTrue(invokeString(game, "pauseRestartWarning").contains("counts as a loss"));
        assertTrue(invokeString(game, "pauseExitWarning").contains("entire current Classic run"));
    }

    @Test
    void modeAwareExitLabelsLeadBackToTheOwningMode() throws Exception {
        BirdGame3 game = new BirdGame3();
        set(game, "trainingModeActive", true);
        assertEquals("EXIT TO TRAINING", invokeString(game, "pauseExitLabel"));
        assertEquals("TRAINING_SETUP", invokeObject(game, "pauseExitDestination").toString());

        set(game, "trainingModeActive", false);
        set(game, "campaignModeActive", true);
        assertEquals("EXIT TO STORY", invokeString(game, "pauseExitLabel"));
        assertEquals("STORY_HUB", invokeObject(game, "pauseExitDestination").toString());

        set(game, "campaignModeActive", false);
        set(game, "adventureModeActive", true);
        assertEquals("EXIT TO ADVENTURE", invokeString(game, "pauseExitLabel"));
        assertEquals("ADVENTURE_HUB", invokeObject(game, "pauseExitDestination").toString());
    }

    @Test
    void abandoningClassicRoutesToTheHubInsteadOfFighterSelect() throws Exception {
        BirdGame3 game = new BirdGame3();
        set(game, "classicModeActive", true);

        assertEquals("MAIN_HUB", invokeObject(game, "pauseExitDestination").toString());
    }

    @Test
    void phoenixTrialCanExitBackToItsOwningGamesMenu() throws Exception {
        BirdGame3 game = new BirdGame3();
        set(game, "classicModeActive", true);
        set(game, "ashfallTrialModeActive", true);

        assertEquals("EXIT TO GAMES & MORE", invokeString(game, "pauseExitLabel"));
        assertEquals("GAMES_MORE", invokeObject(game, "pauseExitDestination").toString());
        assertTrue(invokeString(game, "pauseExitWarning").contains("Current challenge progress will be reset"));
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static Object enumValue(Class<?> type, String name) {
        return Enum.valueOf((Class<? extends Enum>) type.asSubclass(Enum.class), name);
    }

    private static String invokeString(BirdGame3 game, String methodName) throws Exception {
        return (String) invokeObject(game, methodName);
    }

    private static Object invokeObject(BirdGame3 game, String methodName) throws Exception {
        Method method = BirdGame3.class.getDeclaredMethod(methodName);
        method.setAccessible(true);
        return method.invoke(game);
    }

    private static void set(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }
}
