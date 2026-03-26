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

        Bird boss = createStoryBird(game, 1, "Boss: Null Roc", 200.0, 1.50, 1.10, true);
        Bird elite = createStoryBird(game, 2, "Elite: Crown Herald", 200.0, 1.50, 1.10, true);

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
                760.0,
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

        Bird boss = game.players[3];
        assertEquals(714.4, boss.health, 0.0001);
        assertEquals(3.6, boss.baseSizeMultiplier, 0.0001);
        assertEquals(1.7266, boss.basePowerMultiplier, 0.0001);
        assertEquals(1.0192, boss.baseSpeedMultiplier, 0.0001);
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

        assertEquals(714.4, bird.health, 0.0001);
        assertEquals(3.6, bird.baseSizeMultiplier, 0.0001);
        assertEquals(1.7266, bird.basePowerMultiplier, 0.0001);
        assertEquals(1.0192, bird.baseSpeedMultiplier, 0.0001);
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
    }

    @Test
    void lockedVultureSlotUnlocksNullRockFromDirectionalSelectorCode() throws Exception {
        BirdGame3 game = new BirdGame3();
        game.nullRockVultureUnlocked = true;
        game.activePlayers = 4;

        BirdGame3.BirdType[] selectedBirds = getPrivateBirdTypeArray(game, "fightSelectedBirds");
        boolean[] randomSelected = getPrivateBooleanArray(game, "fightRandomSelected");
        String[] selectedSkins = getPrivateStringArray(game, "fightSelectedSkinKeys");
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
    void createStoryBirdAppliesMuchStrongerEaseDuringBossRush() throws Exception {
        BirdGame3 game = new BirdGame3();
        game.classicModeActive = true;
        game.classicEncounter = firstBossRushEncounter(game);
        setPrivateBoolean(game, "bossRushModeActive", true);

        Bird boss = createStoryBird(game, 1, "Boss: Canopy Vulture", 200.0, 1.50, 1.10, true);

        assertEquals(135.36, boss.health, 0.0001);
        assertEquals(1.2222, boss.basePowerMultiplier, 0.0001);
        assertEquals(0.94864, boss.baseSpeedMultiplier, 0.0001);
    }

    @Test
    void getCpuLevelUsesExtraReductionForBossRushBosses() throws Exception {
        BirdGame3 game = new BirdGame3();
        game.classicModeActive = true;
        game.classicEncounter = firstBossRushEncounter(game);
        setPrivateBoolean(game, "bossRushModeActive", true);

        int[] classicCpuLevels = getPrivateIntArray(game, "classicCpuLevels");
        classicCpuLevels[1] = 7;

        Bird boss = new Bird(100, BirdGame3.BirdType.VULTURE, 1, game);
        boss.name = "Boss: Canopy Vulture";
        game.players[1] = boss;
        game.isAI[1] = true;

        assertEquals(4, game.getCpuLevel(1));
    }

    private static Bird createStoryBird(BirdGame3 game, int playerIdx, String name,
                                        double health, double powerMult, double speedMult, boolean ai) throws Exception {
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
        return (Bird) method.invoke(game, 1000.0 + playerIdx * 100.0, BirdGame3.BirdType.VULTURE, playerIdx, name, health, powerMult, speedMult, ai);
    }

    private static int[] getPrivateIntArray(Object target, String fieldName) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        return (int[]) field.get(target);
    }

    private static boolean[] getPrivateBooleanArray(Object target, String fieldName) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        return (boolean[]) field.get(target);
    }

    private static BirdGame3.BirdType[] getPrivateBirdTypeArray(Object target, String fieldName) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        return (BirdGame3.BirdType[]) field.get(target);
    }

    private static String[] getPrivateStringArray(Object target, String fieldName) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
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

    private static void setPrivateBoolean(Object target, String fieldName, boolean value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.setBoolean(target, value);
    }

    private static Object recordValue(Object target, String accessor) throws Exception {
        Method method = target.getClass().getDeclaredMethod(accessor);
        method.setAccessible(true);
        return method.invoke(target);
    }
}
