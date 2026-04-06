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

    @Test
    void bossRushAshfallEncounterUsesNerfedPhoenixProfile() throws Exception {
        BirdGame3 game = new BirdGame3();
        BirdGame3.ClassicEncounter encounter = bossRushEncounterByName(game, "Ashfall Rebirth");

        assertEquals(6, encounter.cpuLevel);
        assertEquals(1, encounter.enemies.length);
        assertEquals(BirdGame3.BirdType.PHOENIX, encounter.enemies[0].type());
        assertEquals(240.0, encounter.enemies[0].health(), 0.0001);
        assertEquals(1.22, encounter.enemies[0].powerMult(), 0.0001);
        assertEquals(1.02, encounter.enemies[0].speedMult(), 0.0001);
    }

    @Test
    void bossRushTitanDockEncounterUsesNerfedPelicanProfile() throws Exception {
        BirdGame3 game = new BirdGame3();
        BirdGame3.ClassicEncounter encounter = bossRushEncounterByName(game, "Titan Dock");

        assertEquals(6, encounter.cpuLevel);
        assertEquals(1, encounter.enemies.length);
        assertEquals(BirdGame3.BirdType.PELICAN, encounter.enemies[0].type());
        assertEquals(300.0, encounter.enemies[0].health(), 0.0001);
        assertEquals(1.34, encounter.enemies[0].powerMult(), 0.0001);
        assertEquals(0.90, encounter.enemies[0].speedMult(), 0.0001);
    }

    @Test
    void bossRushParliamentEncounterUsesSofterDuoProfile() throws Exception {
        BirdGame3 game = new BirdGame3();
        BirdGame3.ClassicEncounter encounter = bossRushEncounterByName(game, "Parliament Of Smoke");

        assertEquals(7, encounter.cpuLevel);
        assertEquals(1, encounter.allies.length);
        assertEquals(124.0, encounter.allies[0].health(), 0.0001);
        assertEquals(1.16, encounter.allies[0].powerMult(), 0.0001);
        assertEquals(1.14, encounter.allies[0].speedMult(), 0.0001);

        assertEquals(2, encounter.enemies.length);
        assertEquals(BirdGame3.BirdType.MOCKINGBIRD, encounter.enemies[0].type());
        assertEquals(200.0, encounter.enemies[0].health(), 0.0001);
        assertEquals(1.12, encounter.enemies[0].powerMult(), 0.0001);
        assertEquals(1.05, encounter.enemies[0].speedMult(), 0.0001);
        assertEquals(BirdGame3.BirdType.RAVEN, encounter.enemies[1].type());
        assertEquals(180.0, encounter.enemies[1].health(), 0.0001);
        assertEquals(1.16, encounter.enemies[1].powerMult(), 0.0001);
        assertEquals(1.10, encounter.enemies[1].speedMult(), 0.0001);
    }

    @Test
    void bossRushCarrionThroneEncounterUsesSofterLateRouteProfile() throws Exception {
        BirdGame3 game = new BirdGame3();
        BirdGame3.ClassicEncounter encounter = bossRushEncounterByName(game, "Carrion Throne");

        assertEquals(7, encounter.cpuLevel);
        assertEquals(1, encounter.allies.length);
        assertEquals(126.0, encounter.allies[0].health(), 0.0001);
        assertEquals(1.18, encounter.allies[0].powerMult(), 0.0001);
        assertEquals(1.10, encounter.allies[0].speedMult(), 0.0001);

        assertEquals(2, encounter.enemies.length);
        assertEquals(BirdGame3.BirdType.VULTURE, encounter.enemies[0].type());
        assertEquals(250.0, encounter.enemies[0].health(), 0.0001);
        assertEquals(1.30, encounter.enemies[0].powerMult(), 0.0001);
        assertEquals(1.02, encounter.enemies[0].speedMult(), 0.0001);
        assertEquals(BirdGame3.BirdType.OPIUMBIRD, encounter.enemies[1].type());
        assertEquals(198.0, encounter.enemies[1].health(), 0.0001);
        assertEquals(1.18, encounter.enemies[1].powerMult(), 0.0001);
        assertEquals(1.12, encounter.enemies[1].speedMult(), 0.0001);
    }

    @Test
    void bossRushRosterModifiersAreAlsoReducedForPhoenixAndPelican() throws Exception {
        BirdGame3 game = new BirdGame3();
        Bird phoenix = new Bird(400, BirdGame3.BirdType.PHOENIX, 1, game);
        phoenix.name = "Boss: Ashen Phoenix";
        double phoenixBaseSize = phoenix.baseSizeMultiplier;
        double phoenixBasePower = phoenix.basePowerMultiplier;
        double phoenixBaseSpeed = phoenix.baseSpeedMultiplier;
        game.players[1] = phoenix;

        Bird pelican = new Bird(500, BirdGame3.BirdType.PELICAN, 2, game);
        pelican.name = "Boss: Titan Pelican";
        double pelicanBaseSize = pelican.baseSizeMultiplier;
        double pelicanBasePower = pelican.basePowerMultiplier;
        double pelicanBaseSpeed = pelican.baseSpeedMultiplier;
        game.players[2] = pelican;

        Method method = BirdGame3.class.getDeclaredMethod("applyBossRushEncounterRosterModifiers", BirdGame3.ClassicEncounter.class);
        method.setAccessible(true);
        method.invoke(game, bossRushEncounterByName(game, "Ashfall Rebirth"));

        assertEquals(phoenixBaseSize * 1.05, phoenix.baseSizeMultiplier, 0.0001);
        assertEquals(phoenixBasePower * 1.03, phoenix.basePowerMultiplier, 0.0001);
        assertEquals(phoenixBaseSpeed * 1.01, phoenix.baseSpeedMultiplier, 0.0001);

        assertEquals(pelicanBaseSize * 1.22, pelican.baseSizeMultiplier, 0.0001);
        assertEquals(pelicanBasePower * 1.04, pelican.basePowerMultiplier, 0.0001);
        assertEquals(pelicanBaseSpeed * 0.96, pelican.baseSpeedMultiplier, 0.0001);
    }

    @Test
    void bossRushRosterModifiersAreReducedForParliamentAndCarrionBosses() throws Exception {
        BirdGame3 game = new BirdGame3();
        Bird sparrow = new Bird(400, BirdGame3.BirdType.MOCKINGBIRD, 1, game);
        sparrow.name = "Boss: Old Sparrow";
        double sparrowBaseSize = sparrow.baseSizeMultiplier;
        double sparrowBasePower = sparrow.basePowerMultiplier;
        double sparrowBaseSpeed = sparrow.baseSpeedMultiplier;
        game.players[1] = sparrow;

        Bird raven = new Bird(500, BirdGame3.BirdType.RAVEN, 2, game);
        raven.name = "Boss: Void Raven";
        double ravenBaseSize = raven.baseSizeMultiplier;
        double ravenBasePower = raven.basePowerMultiplier;
        double ravenBaseSpeed = raven.baseSpeedMultiplier;
        game.players[2] = raven;

        Bird regent = new Bird(600, BirdGame3.BirdType.VULTURE, 3, game);
        regent.name = "Boss: Carrion Regent";
        double regentBaseSize = regent.baseSizeMultiplier;
        double regentBasePower = regent.basePowerMultiplier;
        double regentBaseSpeed = regent.baseSpeedMultiplier;
        game.players[3] = regent;

        Bird seer = new Bird(700, BirdGame3.BirdType.OPIUMBIRD, 4, game);
        seer.name = "Boss: Opium Seer";
        double seerBaseSize = seer.baseSizeMultiplier;
        double seerBasePower = seer.basePowerMultiplier;
        double seerBaseSpeed = seer.baseSpeedMultiplier;
        game.players[4] = seer;

        Method method = BirdGame3.class.getDeclaredMethod("applyBossRushEncounterRosterModifiers", BirdGame3.ClassicEncounter.class);
        method.setAccessible(true);
        method.invoke(game, bossRushEncounterByName(game, "Parliament Of Smoke"));

        assertEquals(sparrowBaseSize * 1.04, sparrow.baseSizeMultiplier, 0.0001);
        assertEquals(sparrowBasePower * 1.03, sparrow.basePowerMultiplier, 0.0001);
        assertEquals(sparrowBaseSpeed * 1.01, sparrow.baseSpeedMultiplier, 0.0001);
        assertEquals(ravenBaseSize * 1.06, raven.baseSizeMultiplier, 0.0001);
        assertEquals(ravenBasePower * 1.04, raven.basePowerMultiplier, 0.0001);
        assertEquals(ravenBaseSpeed * 1.05, raven.baseSpeedMultiplier, 0.0001);
        assertEquals(regentBaseSize * 1.14, regent.baseSizeMultiplier, 0.0001);
        assertEquals(regentBasePower * 1.05, regent.basePowerMultiplier, 0.0001);
        assertEquals(regentBaseSpeed * 1.01, regent.baseSpeedMultiplier, 0.0001);
        assertEquals(seerBaseSize * 1.04, seer.baseSizeMultiplier, 0.0001);
        assertEquals(seerBasePower * 1.04, seer.basePowerMultiplier, 0.0001);
        assertEquals(seerBaseSpeed * 1.04, seer.baseSpeedMultiplier, 0.0001);
    }

    @Test
    void bossRushParliamentMidpointUsesHealthPickupAndLowerCrowPressure() throws Exception {
        BirdGame3 game = new BirdGame3();
        game.classicModeActive = true;
        game.classicTeamMode = true;
        game.classicEncounter = bossRushEncounterByName(game, "Parliament Of Smoke");
        setPrivateBoolean(game);
        game.activePlayers = 3;
        game.classicTeams[0] = 1;
        game.classicTeams[1] = 2;
        game.classicTeams[2] = 2;

        Bird player = new Bird(100, BirdGame3.BirdType.PIGEON, 0, game);
        Bird sparrow = new Bird(200, BirdGame3.BirdType.MOCKINGBIRD, 1, game);
        sparrow.name = "Boss: Old Sparrow";
        sparrow.health = 80.0;
        Bird raven = new Bird(300, BirdGame3.BirdType.RAVEN, 2, game);
        raven.name = "Boss: Void Raven";
        raven.health = 70.0;
        game.players[0] = player;
        game.players[1] = sparrow;
        game.players[2] = raven;

        Method method = BirdGame3.class.getDeclaredMethod("applyBossRushRuntimeEffects");
        method.setAccessible(true);
        method.invoke(game);

        assertEquals(7, game.crowMinions.size());
        assertTrue(game.powerUps.stream().anyMatch(powerUp -> powerUp.type == PowerUpType.HEALTH));
        assertFalse(game.powerUps.stream().anyMatch(powerUp -> powerUp.type == PowerUpType.SHRINK));
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

    @SuppressWarnings("unchecked")
    private static BirdGame3.ClassicEncounter bossRushEncounterByName(BirdGame3 game, String name) throws Exception {
        Method method = BirdGame3.class.getDeclaredMethod("buildBossRushRun");
        method.setAccessible(true);
        List<BirdGame3.ClassicEncounter> encounters = (List<BirdGame3.ClassicEncounter>) method.invoke(game);
        for (BirdGame3.ClassicEncounter encounter : encounters) {
            if (name.equals(encounter.name)) {
                return encounter;
            }
        }
        fail("Missing Boss Rush encounter: " + name);
        return null;
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
