package com.example.birdgame3;

import javafx.scene.input.KeyCode;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.*;

class BirdGame3BossBalanceTest {
    @Test
    void createStoryBirdSoftensBossStatsWithoutTouchingNonBossEnemies() throws Exception {
        BirdGame3 game = new BirdGame3();

        Bird boss = createStoryBird(game, 1, "Boss: Null Roc");
        Bird elite = createStoryBird(game, 2, "Elite: Crown Herald");

        assertEquals(188.0, boss.health, 0.0001);
        assertEquals(1.455, boss.basePowerMultiplier, 0.0001);
        assertEquals(1.078, boss.baseSpeedMultiplier, 0.0001);

        assertEquals(200.0, elite.health, 0.0001);
        assertEquals(1.50, elite.basePowerMultiplier, 0.0001);
        assertEquals(1.10, elite.baseSpeedMultiplier, 0.0001);
    }

    @Test
    void getCpuLevelDropsBossAiOneStepButKeepsFloorAtOne() throws Exception {
        BirdGame3 game = new BirdGame3();
        int[] cpuLevels = getPrivateIntArray(game, "cpuLevels");

        cpuLevels[1] = 6;
        cpuLevels[2] = 6;

        Bird boss = new Bird(100, BirdGame3.BirdType.VULTURE, 1, game);
        boss.name = "Boss: Canopy Vulture";
        game.players[1] = boss;
        game.isAI[1] = true;

        Bird elite = new Bird(200, BirdGame3.BirdType.RAVEN, 2, game);
        elite.name = "Elite: Crown Herald";
        game.players[2] = elite;
        game.isAI[2] = true;

        assertEquals(5, game.getCpuLevel(1));
        assertEquals(6, game.getCpuLevel(2));

        cpuLevels[1] = 1;
        assertEquals(1, game.getCpuLevel(1));
    }

    @Test
    void unitedFinaleRosterKeepsBossEaseAfterTitanScaleOverride() throws Exception {
        BirdGame3 game = new BirdGame3();
        BirdGame3.AdventureBattle battle = new BirdGame3.AdventureBattle(
                "Battle 3: The Null Rock",
                "",
                BirdGame3.MapType.BEACON_CROWN,
                BirdGame3.BirdType.VULTURE,
                "Boss: The Null Rock",
                1000.0,
                1.78,
                1.04,
                null,
                null,
                null,
                null,
                null,
                null
        );

        Method method = BirdGame3.class.getDeclaredMethod(
                "setupUnitedFinaleAdventureRoster",
                BirdGame3.AdventureBattle.class,
                BirdGame3.BirdType.class,
                String.class
        );
        method.setAccessible(true);
        method.invoke(game, battle, BirdGame3.BirdType.PIGEON, null);

        Bird boss = game.players[game.activePlayers - 1];
        assertEquals(1000.0, boss.health, 0.0001);
        assertEquals(3.6, boss.baseSizeMultiplier, 0.0001);
        assertEquals(1.7266, boss.basePowerMultiplier, 0.0001);
        assertEquals(1.0192, boss.baseSpeedMultiplier, 0.0001);
    }

    @Test
    void unitedFinaleClimaxSpawnsTheEntireRosterAtOnce() throws Exception {
        BirdGame3 game = new BirdGame3();
        BirdGame3.AdventureBattle battle = new BirdGame3.AdventureBattle(
                "Battle 3: The Null Rock",
                "",
                BirdGame3.MapType.BEACON_CROWN,
                BirdGame3.BirdType.VULTURE,
                "Boss: The Null Rock",
                1000.0,
                1.78,
                1.04,
                null,
                null,
                null,
                null,
                null,
                null
        );

        Method method = BirdGame3.class.getDeclaredMethod(
                "setupUnitedFinaleAdventureRoster",
                BirdGame3.AdventureBattle.class,
                BirdGame3.BirdType.class,
                String.class
        );
        method.setAccessible(true);
        method.invoke(game, battle, BirdGame3.BirdType.PIGEON, null);

        assertEquals(BirdGame3.BirdType.values().length + 1, game.activePlayers);
        assertEquals(BirdGame3.BirdType.EAGLE, game.players[1].type);
        assertEquals(BirdGame3.BirdType.PHOENIX, game.players[2].type);
        assertEquals(BirdGame3.BirdType.VULTURE, game.players[game.activePlayers - 1].type);

        Field queueField = BirdGame3.class.getDeclaredField("unitedFinaleSupportQueue");
        queueField.setAccessible(true);
        @SuppressWarnings("unchecked")
        List<Object> queue = (List<Object>) queueField.get(game);
        assertTrue(queue.isEmpty());

        List<BirdGame3.BirdType> alliedTypes = new ArrayList<>();
        for (int i = 1; i < game.activePlayers - 1; i++) {
            assertNotNull(game.players[i], "Every ally slot should be populated.");
            alliedTypes.add(game.players[i].type);
        }

        assertFalse(alliedTypes.contains(BirdGame3.BirdType.PIGEON));
        assertTrue(alliedTypes.contains(BirdGame3.BirdType.HUMMINGBIRD));
        assertTrue(alliedTypes.contains(BirdGame3.BirdType.VULTURE));
        assertTrue(alliedTypes.contains(BirdGame3.BirdType.HEISENBIRD));
    }

    @Test
    void dynamicCameraSmoothsOutUnitedFinaleTagIns() throws Exception {
        BirdGame3 game = new BirdGame3();
        Bird player = new Bird(1200, BirdGame3.BirdType.PIGEON, 0, game);
        Bird ally = new Bird(1340, BirdGame3.BirdType.EAGLE, 1, game);
        Bird boss = new Bird(1460, BirdGame3.BirdType.VULTURE, 3, game);
        game.players[0] = player;
        game.players[1] = ally;
        game.players[3] = boss;

        double initialMinX = player.x;
        double initialMaxX = boss.x + 80;
        double initialMinY = Math.min(Math.min(player.y, ally.y), boss.y);
        double initialMaxY = Math.max(Math.max(player.y, ally.y), boss.y) + 80;
        double initialZoom = Math.clamp(
                Math.min(
                        BirdGame3.WIDTH / ((initialMaxX - initialMinX) + 800),
                        BirdGame3.HEIGHT / ((initialMaxY - initialMinY) + 800)
                ),
                BirdGame3.MIN_ZOOM,
                BirdGame3.MAX_ZOOM
        );
        double initialCamX = Math.clamp(
                ((initialMinX + initialMaxX) * 0.5) - BirdGame3.WIDTH / (2 * initialZoom),
                0,
                BirdGame3.WORLD_WIDTH - BirdGame3.WIDTH / initialZoom
        );
        double initialCamY = Math.clamp(
                ((initialMinY + initialMaxY) * 0.5) - BirdGame3.HEIGHT / (2 * initialZoom),
                0,
                BirdGame3.WORLD_HEIGHT - BirdGame3.HEIGHT / initialZoom
        );

        setPrivateDouble(game, "zoom", initialZoom);
        setPrivateDouble(game, "camX", initialCamX);
        setPrivateDouble(game, "camY", initialCamY);
        setPrivateDouble(game, "trackedCamMinX", initialMinX);
        setPrivateDouble(game, "trackedCamMaxX", initialMaxX);
        setPrivateDouble(game, "trackedCamMinY", initialMinY);
        setPrivateDouble(game, "trackedCamMaxY", initialMaxY);

        Bird support = new Bird(2920, BirdGame3.BirdType.HUMMINGBIRD, 2, game);
        game.players[2] = support;
        setPrivateIntField(game, "cameraTagInEaseFrames", 42);

        Method updateDynamicCamera = BirdGame3.class.getDeclaredMethod("updateDynamicCamera");
        updateDynamicCamera.setAccessible(true);
        updateDynamicCamera.invoke(game);

        double directTargetZoom = Math.clamp(
                Math.min(
                        BirdGame3.WIDTH / (((support.x + 80) - initialMinX) + 800),
                        BirdGame3.HEIGHT / ((initialMaxY - initialMinY) + 800)
                ),
                BirdGame3.MIN_ZOOM,
                BirdGame3.MAX_ZOOM
        );
        double zoomAfterUpdate = getPrivateDouble(game, "zoom");
        assertTrue(zoomAfterUpdate > directTargetZoom, "Tag-in zoom should ease out rather than jump to the full target.");

        double directCenterX = (initialMinX + (support.x + 80)) * 0.5;
        double directTargetCamX = Math.clamp(
                directCenterX - BirdGame3.WIDTH / (2 * zoomAfterUpdate),
                0,
                BirdGame3.WORLD_WIDTH - BirdGame3.WIDTH / zoomAfterUpdate
        );
        double camXAfterUpdate = getPrivateDouble(game, "camX");
        assertTrue(camXAfterUpdate > initialCamX, "Camera should start moving toward the new support.");
        assertTrue(camXAfterUpdate < directTargetCamX, "Camera should pan toward the tag-in instead of snapping to it.");
    }

    @Test
    void localNullRockSkinUsesBossTemplateStats() throws Exception {
        BirdGame3 game = new BirdGame3();
        game.nullRockVultureUnlocked = true;
        Bird bird = new Bird(100, BirdGame3.BirdType.VULTURE, 0, game);

        Method method = BirdGame3.class.getDeclaredMethod(
                "applySkinChoiceToBird",
                Bird.class,
                BirdGame3.BirdType.class,
                String.class
        );
        method.setAccessible(true);
        method.invoke(game, bird, BirdGame3.BirdType.VULTURE, "NULL_ROCK_VULTURE");

        assertEquals(1000.0, bird.health, 0.0001);
        assertEquals(3.6, bird.baseSizeMultiplier, 0.0001);
        assertEquals(1.7266, bird.basePowerMultiplier, 0.0001);
        assertEquals(1.0192, bird.baseSpeedMultiplier, 0.0001);
    }

    @Test
    void unitedFinaleDialogueUsesNullRockPortraitSkin() throws Exception {
        BirdGame3 game = new BirdGame3();

        Field chaptersField = BirdGame3.class.getDeclaredField("adventureChapters");
        chaptersField.setAccessible(true);
        Object[] chapters = (Object[]) chaptersField.get(game);
        Object unitedFinaleChapter = chapters[chapters.length - 1];
        Object[] battles = (Object[]) recordValue(unitedFinaleChapter, "battles");
        BirdGame3.AdventureBattle finalBattle = (BirdGame3.AdventureBattle) battles[battles.length - 1];

        Field preDialogueField = BirdGame3.AdventureBattle.class.getDeclaredField("preDialogue");
        preDialogueField.setAccessible(true);
        Object lines = preDialogueField.get(finalBattle);

        @SuppressWarnings("unchecked")
        Class<? extends Enum<?>> dialogueSideClass = (Class<? extends Enum<?>>) Class.forName("com.example.birdgame3.BirdGame3$DialogueSide");
        @SuppressWarnings({"rawtypes", "unchecked"})
        Object rightSide = Enum.valueOf((Class) dialogueSideClass, "RIGHT");

        Method resolve = BirdGame3.class.getDeclaredMethod("resolveAdventureDialogueSideSkinKey", lines.getClass(), dialogueSideClass);
        resolve.setAccessible(true);

        assertEquals("NULL_ROCK_VULTURE", resolve.invoke(game, lines, rightSide));
    }

    @Test
    void featherpediaShowsNullRockAsSeparateBirdWithItsOwnSummons() throws Exception {
        BirdGame3 game = new BirdGame3();
        game.nullRockVultureUnlocked = true;

        Method method = BirdGame3.class.getDeclaredMethod("birdBookBirdEntries");
        method.setAccessible(true);
        List<?> entries = (List<?>) method.invoke(game);

        Object nullRockEntry = null;
        for (Object entry : entries) {
            if ("The Null Rock".equals(recordValue(entry, "displayName"))) {
                nullRockEntry = entry;
                break;
            }
        }

        assertNotNull(nullRockEntry);
        assertEquals(BirdGame3.BirdType.VULTURE, recordValue(nullRockEntry, "type"));
        assertEquals("NULL_ROCK_VULTURE", recordValue(nullRockEntry, "skinKey"));
        assertEquals(BirdGame3.MapType.BEACON_CROWN, recordValue(nullRockEntry, "origin"));
        assertTrue((Boolean) recordValue(nullRockEntry, "unlocked"));
        assertFalse((Boolean) recordValue(nullRockEntry, "showMastery"));

        @SuppressWarnings("unchecked")
        List<Object> companions = (List<Object>) recordValue(nullRockEntry, "companions");
        List<String> companionNames = new ArrayList<>();
        for (Object companion : companions) {
            companionNames.add((String) recordValue(companion, "name"));
        }

        assertEquals(List.of("Giant Crow", "Raven", "Void Raven", "Murder Crow"), companionNames);
    }

    @Test
    void featherpediaNullRockEntryUsesFinalBossDescriptionAndRealStats() throws Exception {
        BirdGame3 game = new BirdGame3();
        game.nullRockVultureUnlocked = true;

        Method method = BirdGame3.class.getDeclaredMethod("nullRockBirdBookEntry");
        method.setAccessible(true);
        Object entry = method.invoke(game);

        String expectedStats = String.format(
                Locale.US,
                "Health: %.1f | Size: %.2fx | Power: %.2fx | Speed: %.2fx",
                game.nullRockTrueFormHealth(),
                game.nullRockTrueFormSizeMultiplier(),
                game.nullRockTrueFormPowerMultiplier(),
                game.nullRockTrueFormSpeedMultiplier()
        );

        assertEquals(expectedStats, recordValue(entry, "statsLine"));
        String description = (String) recordValue(entry, "description");
        assertTrue(description.contains("final boss of Bird Fight 3"));
        assertTrue(description.contains("Up Up Down Down Left Right"));
        assertTrue(description.contains("type NULL"));
    }

    @Test
    void lockedVultureSlotUnlocksNullRockFromDirectionalSelectorCode() throws Exception {
        BirdGame3 game = new BirdGame3();
        game.nullRockVultureUnlocked = true;
        game.activePlayers = 4;

        BirdGame3.BirdType[] selectedBirds = getPrivateBirdTypeArray(game);
        boolean[] randomSelected = getPrivateBooleanArray(game);
        String[] selectedSkins = getPrivateStringArray(game);
        boolean[] selectorLocked = {true, false, false, false};
        int[] progress = new int[4];
        selectedBirds[0] = BirdGame3.BirdType.VULTURE;
        randomSelected[0] = false;

        Method method = BirdGame3.class.getDeclaredMethod(
                "handleLockedNullRockSelectorSecret",
                int.class,
                KeyCode.class,
                boolean[].class,
                Runnable.class,
                int[].class
        );
        method.setAccessible(true);
        Runnable noop = () -> {};

        assertTrue((Boolean) method.invoke(game, 0, KeyCode.W, selectorLocked, noop, progress));
        assertEquals(1, progress[0]);
        assertTrue((Boolean) method.invoke(game, 0, KeyCode.W, selectorLocked, noop, progress));
        assertEquals(2, progress[0]);
        assertTrue((Boolean) method.invoke(game, 0, KeyCode.S, selectorLocked, noop, progress));
        assertEquals(3, progress[0]);
        assertTrue((Boolean) method.invoke(game, 0, KeyCode.S, selectorLocked, noop, progress));
        assertEquals(4, progress[0]);
        assertTrue((Boolean) method.invoke(game, 0, KeyCode.A, selectorLocked, noop, progress));
        assertEquals(5, progress[0]);
        assertTrue((Boolean) method.invoke(game, 0, KeyCode.D, selectorLocked, noop, progress));
        assertEquals("NULL_ROCK_VULTURE", selectedSkins[0]);
        assertEquals(0, progress[0]);
    }

    @Test
    void localProgressPlayerCheckOnlyCountsLocalPlayers() throws Exception {
        BirdGame3 game = new BirdGame3();
        game.players[0] = new Bird(100, BirdGame3.BirdType.PIGEON, 0, game);
        game.players[1] = new Bird(200, BirdGame3.BirdType.EAGLE, 1, game);
        game.isAI[0] = false;
        game.isAI[1] = true;

        assertTrue(BirdProgression.isLocalProgressPlayer(game.players, game.isAI, false, false, -1, 0));
        assertFalse(BirdProgression.isLocalProgressPlayer(game.players, game.isAI, false, false, -1, 1));

        game.lanModeActive = true;
        setPrivateInt(game);

        assertFalse(BirdProgression.isLocalProgressPlayer(game.players, game.isAI, false, true, 1, 0));
        assertTrue(BirdProgression.isLocalProgressPlayer(game.players, game.isAI, false, true, 1, 1));
    }

    @Test
    void createStoryBirdAppliesMuchStrongerEaseDuringBossRush() throws Exception {
        BirdGame3 game = new BirdGame3();
        game.classicModeActive = true;
        game.classicEncounter = firstBossRushEncounter(game);
        setPrivateBoolean(game);

        Bird boss = createStoryBird(game, 1, "Boss: Canopy Vulture");

        assertEquals(135.36, boss.health, 0.0001);
        assertEquals(1.2222, boss.basePowerMultiplier, 0.0001);
        assertEquals(0.94864, boss.baseSpeedMultiplier, 0.0001);
    }

    @Test
    void getCpuLevelUsesExtraReductionForBossRushBosses() throws Exception {
        BirdGame3 game = new BirdGame3();
        game.classicModeActive = true;
        game.classicEncounter = firstBossRushEncounter(game);
        setPrivateBoolean(game);

        int[] classicCpuLevels = getPrivateIntArray(game, "classicCpuLevels");
        classicCpuLevels[1] = 7;

        Bird boss = new Bird(100, BirdGame3.BirdType.VULTURE, 1, game);
        boss.name = "Boss: Canopy Vulture";
        game.players[1] = boss;
        game.isAI[1] = true;

        assertEquals(4, game.getCpuLevel(1));
    }

    private static Bird createStoryBird(BirdGame3 game, int playerIdx, String name) throws Exception {
        Method method = BirdGame3.class.getDeclaredMethod(
                "createStoryBird",
                double.class,
                BirdGame3.BirdType.class,
                int.class,
                String.class,
                double.class,
                double.class,
                double.class,
                boolean.class
        );
        method.setAccessible(true);
        return (Bird) method.invoke(game, 1000.0 + playerIdx * 100.0, BirdGame3.BirdType.VULTURE, playerIdx, name, 200.0, 1.5, 1.1, true);
    }

    private static int[] getPrivateIntArray(Object target, String fieldName) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        return (int[]) field.get(target);
    }

    private static boolean[] getPrivateBooleanArray(Object target) throws Exception {
        Field field = target.getClass().getDeclaredField("fightRandomSelected");
        field.setAccessible(true);
        return (boolean[]) field.get(target);
    }

    private static BirdGame3.BirdType[] getPrivateBirdTypeArray(Object target) throws Exception {
        Field field = target.getClass().getDeclaredField("fightSelectedBirds");
        field.setAccessible(true);
        return (BirdGame3.BirdType[]) field.get(target);
    }

    private static String[] getPrivateStringArray(Object target) throws Exception {
        Field field = target.getClass().getDeclaredField("fightSelectedSkinKeys");
        field.setAccessible(true);
        return (String[]) field.get(target);
    }

    @SuppressWarnings("unchecked")
    private static BirdGame3.ClassicEncounter firstBossRushEncounter(BirdGame3 game) throws Exception {
        Method method = BirdGame3.class.getDeclaredMethod("buildBossRushRun");
        method.setAccessible(true);
        List<BirdGame3.ClassicEncounter> encounters = (List<BirdGame3.ClassicEncounter>) method.invoke(game);
        return encounters.getFirst();
    }

    private static void setPrivateBoolean(Object target) throws Exception {
        Field field = target.getClass().getDeclaredField("bossRushModeActive");
        field.setAccessible(true);
        field.setBoolean(target, true);
    }

    private static void setPrivateInt(Object target) throws Exception {
        Field field = target.getClass().getDeclaredField("lanPlayerIndex");
        field.setAccessible(true);
        field.setInt(target, 1);
    }

    private static void setPrivateIntField(Object target, String fieldName, int value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.setInt(target, value);
    }

    private static void setPrivateDouble(Object target, String fieldName, double value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.setDouble(target, value);
    }

    private static double getPrivateDouble(Object target, String fieldName) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        return field.getDouble(target);
    }

    private static Object recordValue(Object target, String accessor) throws Exception {
        Method method = target.getClass().getDeclaredMethod(accessor);
        method.setAccessible(true);
        return method.invoke(target);
    }
}
