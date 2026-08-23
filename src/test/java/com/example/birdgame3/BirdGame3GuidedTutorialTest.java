package com.example.birdgame3;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BirdGame3GuidedTutorialTest {
    private static final String LESSON_CLASS = "com.example.birdgame3.BirdGame3$GuidedTutorialLesson";
    private static final String MODE_CLASS = "com.example.birdgame3.BirdGame3$TrainingAcademyMode";

    @Test
    void curriculumHasEightStableFundamentalsInPlayerLearningOrder() throws Exception {
        Object[] lessons = (Object[]) field(BirdGame3.class, "CORE_GUIDED_TUTORIAL_LESSONS").get(null);

        assertEquals(BirdGame3ProfileProgressState.GUIDED_TUTORIAL_LESSON_COUNT, lessons.length);
        assertEquals(Arrays.asList(
                        "STAGE_CONTROL",
                        "DAMAGE_AND_CHARGE",
                        "RING_OUT",
                        "RECOVERY_AND_LEDGE",
                        "DEFENSE_AND_PUNISH",
                        "DIRECTIONAL_SPECIALS",
                        "ULTIMATE_FINISH",
                        "FINAL_PRACTICE"),
                Arrays.stream(lessons).map(Object::toString).toList());
    }

    @Test
    void curriculumNeverFallsThroughIntoCharacterDrills() throws Exception {
        Class<?> lessonType = Class.forName(LESSON_CLASS);
        Method next = lessonType.getDeclaredMethod("next");
        next.setAccessible(true);
        Object[] lessons = (Object[]) field(BirdGame3.class, "CORE_GUIDED_TUTORIAL_LESSONS").get(null);

        for (int i = 0; i < lessons.length - 1; i++) {
            assertEquals(lessons[i + 1], next.invoke(lessons[i]));
        }
        assertEquals(null, next.invoke(lessons[lessons.length - 1]));
    }

    @Test
    void ultimateLessonStartsReadyAndClearsOnlyAfterUltimateIsSpent() throws Exception {
        BirdGame3 game = guidedGame("ULTIMATE_FINISH");
        Bird eagle = new Bird(100.0, BirdGame3.BirdType.EAGLE, 0, game);
        Bird dummy = new Bird(250.0, BirdGame3.BirdType.PIGEON, 1, game);
        game.players[0] = eagle;
        game.players[1] = dummy;
        game.platforms.add(new Platform(0.0, BirdGame3.GROUND_Y, BirdGame3.WORLD_WIDTH, 80.0));

        invoke(game, "prepareGuidedTutorialLessonResources", new Class<?>[]{Bird.class}, eagle);
        assertTrue(eagle.isUltimateReady());
        assertTrue(booleanField(game, "trainingAcademyUltimateWasReady"));

        invoke(game, "updateGuidedTutorialState", new Class<?>[]{Bird.class, Bird.class}, eagle, dummy);
        assertEquals(0, intField(game, "trainingAcademyCompletionFrames"));

        assertTrue(eagle.consumeUltimate());
        invoke(game, "updateGuidedTutorialState", new Class<?>[]{Bird.class, Bird.class}, eagle, dummy);

        assertTrue(booleanField(game, "trainingAcademyUltimateActivatedSeen"));
        assertTrue(intField(game, "trainingAcademyCompletionFrames") > 0);
    }

    @Test
    void defenseLessonRequiresBlockPunishGrabAndThrow() throws Exception {
        BirdGame3 game = guidedGame("DEFENSE_AND_PUNISH");
        Bird player = new Bird(100.0, BirdGame3.BirdType.EAGLE, 0, game);
        Bird dummy = new Bird(250.0, BirdGame3.BirdType.PIGEON, 1, game);
        game.platforms.add(new Platform(0.0, BirdGame3.GROUND_Y, BirdGame3.WORLD_WIDTH, 80.0));
        set(game, "trainingAcademyShieldHitSeen", true);
        set(game, "trainingAcademyPunishReady", true);
        set(game, "trainingAcademyHitsLanded", 1);

        invoke(game, "updateGuidedTutorialState", new Class<?>[]{Bird.class, Bird.class}, player, dummy);
        assertEquals(0, intField(game, "trainingAcademyCompletionFrames"));

        set(game, "trainingAcademyGrabSeen", true);
        set(game, "trainingAcademyThrowSeen", true);
        invoke(game, "updateGuidedTutorialState", new Class<?>[]{Bird.class, Bird.class}, player, dummy);

        assertTrue(intField(game, "trainingAcademyCompletionFrames") > 0);
    }

    @Test
    void finalPracticeCompletesOnFirstRealBlastZoneKo() throws Exception {
        BirdGame3 game = guidedGame("FINAL_PRACTICE");
        Bird player = new Bird(100.0, BirdGame3.BirdType.PIGEON, 0, game);
        Bird dummy = new Bird(250.0, BirdGame3.BirdType.EAGLE, 1, game);
        game.platforms.add(new Platform(0.0, BirdGame3.GROUND_Y, BirdGame3.WORLD_WIDTH, 80.0));

        invoke(game, "updateGuidedTutorialState", new Class<?>[]{Bird.class, Bird.class}, player, dummy);
        assertEquals(0, intField(game, "trainingAcademyCompletionFrames"));

        game.falls[1] = 1;
        invoke(game, "updateGuidedTutorialState", new Class<?>[]{Bird.class, Bird.class}, player, dummy);
        assertTrue(intField(game, "trainingAcademyCompletionFrames") > 0);
    }

    @Test
    void completionSummaryRequiresEveryFundamental() throws Exception {
        BirdGame3 game = new BirdGame3();
        boolean[] progress = (boolean[]) field(BirdGame3.class, "guidedTutorialLessonCompleted").get(game);
        Arrays.fill(progress, true);
        progress[6] = false;

        assertFalse((boolean) invoke(game, "areAllGuidedTutorialLessonsCompleted", new Class<?>[0]));
        progress[6] = true;
        assertTrue((boolean) invoke(game, "areAllGuidedTutorialLessonsCompleted", new Class<?>[0]));
    }

    private static BirdGame3 guidedGame(String lessonName) throws Exception {
        BirdGame3 game = new BirdGame3();
        game.trainingModeActive = true;
        Class<?> modeType = Class.forName(MODE_CLASS);
        Class<?> lessonType = Class.forName(LESSON_CLASS);
        set(game, "trainingAcademyMode", Enum.valueOf(modeType.asSubclass(Enum.class), "GUIDED_TUTORIAL"));
        set(game, "guidedTutorialLesson", Enum.valueOf(lessonType.asSubclass(Enum.class), lessonName));
        return game;
    }

    private static Field field(Class<?> owner, String name) throws Exception {
        Field field = owner.getDeclaredField(name);
        field.setAccessible(true);
        return field;
    }

    private static void set(Object target, String name, Object value) throws Exception {
        field(target.getClass(), name).set(target, value);
    }

    private static Object invoke(Object target, String name, Class<?>[] parameterTypes, Object... args) throws Exception {
        Method method = target.getClass().getDeclaredMethod(name, parameterTypes);
        method.setAccessible(true);
        return method.invoke(target, args);
    }

    private static boolean booleanField(Object target, String name) throws Exception {
        return field(target.getClass(), name).getBoolean(target);
    }

    private static int intField(Object target, String name) throws Exception {
        return field(target.getClass(), name).getInt(target);
    }
}
