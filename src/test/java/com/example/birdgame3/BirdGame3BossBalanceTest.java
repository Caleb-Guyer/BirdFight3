package com.example.birdgame3;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

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
                "Battle 2: All Wings Rise",
                "",
                BirdGame3.MapType.BATTLEFIELD,
                BirdGame3.BirdType.VULTURE,
                "Boss: Null Roc",
                520.0,
                1.55,
                0.96,
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
        assertEquals(488.8, boss.health, 0.0001);
        assertEquals(2.1, boss.baseSizeMultiplier, 0.0001);
        assertEquals(1.5035, boss.basePowerMultiplier, 0.0001);
        assertEquals(0.9408, boss.baseSpeedMultiplier, 0.0001);
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

    @SuppressWarnings("unchecked")
    private static BirdGame3.ClassicEncounter firstBossRushEncounter(BirdGame3 game) throws Exception {
        Method method = BirdGame3.class.getDeclaredMethod("buildBossRushRun", BirdGame3.BirdType.class);
        method.setAccessible(true);
        List<BirdGame3.ClassicEncounter> encounters = (List<BirdGame3.ClassicEncounter>) method.invoke(game, BirdGame3.BirdType.PIGEON);
        return encounters.getFirst();
    }

    private static void setPrivateBoolean(Object target, String fieldName, boolean value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.setBoolean(target, value);
    }
}
