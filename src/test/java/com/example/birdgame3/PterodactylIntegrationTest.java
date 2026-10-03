package com.example.birdgame3;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.lang.reflect.Method;
import java.nio.file.Path;
import java.util.UUID;
import java.util.prefs.Preferences;
import static org.junit.jupiter.api.Assertions.*;

class PterodactylIntegrationTest {
    private final Preferences prefs = Preferences.userRoot().node("/birdfight3-tests/pterodactyl-integration/" + UUID.randomUUID());
    @AfterEach void cleanup() throws Exception { prefs.removeNode(); prefs.flush(); }

    @Test void appendedRosterKeepsLegacyOrdinalsAndOffersCompleteGuidesAndSkin() throws Exception {
        assertEquals(21, BirdGame3.BirdType.KIWI.ordinal());
        assertEquals(22, BirdGame3.BirdType.PTERODACTYL.ordinal());
        FighterMoveGuide.Guide guide = FighterMoveGuide.forBird(BirdGame3.BirdType.PTERODACTYL);
        assertEquals(4, guide.moves().size()); assertEquals("Extinction Dive", guide.ultimateName());
        assertTrue(guide.mechanic().contains("escape"));
        assertSame(CollectibleUnlock.PTERODACTYL, CollectibleUnlock.forBird(BirdGame3.BirdType.PTERODACTYL));
        BirdGame3 game = new BirdGame3(prefs);
        Method unlock = BirdGame3.class.getDeclaredMethod("unlockEverythingForDeveloperProfile"); unlock.setAccessible(true);
        unlock.invoke(game);
        Method birdUnlocked = BirdGame3.class.getDeclaredMethod("isBirdUnlocked", BirdGame3.BirdType.class); birdUnlocked.setAccessible(true);
        assertTrue((boolean) birdUnlocked.invoke(game, BirdGame3.BirdType.PTERODACTYL));
        assertTrue(game.isClassicRewardUnlocked(BirdGame3.BirdType.PTERODACTYL));
        assertTrue(game.visualAuditSkins().stream().anyMatch(skin -> skin.bird() == BirdGame3.BirdType.PTERODACTYL
                && "CLASSIC_SKIN_PTERODACTYL".equals(skin.key()) && "Fossilized Pterodactyl".equals(skin.name())));
    }

    @Test void oldProfilesKeepExistingBadgesAndNewProgressRoundTripsByName() {
        prefs.putBoolean("classic_done_KIWI", true);
        prefs.putBoolean("classic_skin_BAT", true);
        var schema = new BirdGame3ProfileProgressState.Schema(BirdGame3Achievement.values().length, 4, 16, 0, 0);
        var state = BirdGame3ProfileProgressState.load(prefs, schema);
        assertTrue(state.classicCompleted[BirdGame3.BirdType.KIWI.ordinal()]);
        assertTrue(state.classicSkinUnlocked[BirdGame3.BirdType.BAT.ordinal()]);
        int index = BirdGame3.BirdType.PTERODACTYL.ordinal();
        assertFalse(state.classicCompleted[index]); assertFalse(state.classicSkinUnlocked[index]);
        state.classicCompleted[index] = true; state.classicSkinUnlocked[index] = true;
        state.trainingAcademyDrillCompleted[index] = true;
        state.collectibleUnlocks.add(CollectibleUnlock.PTERODACTYL);
        state.saveTo(prefs, schema);
        var loaded = BirdGame3ProfileProgressState.load(prefs, schema);
        assertTrue(loaded.classicCompleted[index]); assertTrue(loaded.classicSkinUnlocked[index]);
        assertTrue(loaded.trainingAcademyDrillCompleted[index]);
        assertTrue(loaded.collectibleUnlocks.contains(CollectibleUnlock.PTERODACTYL));
        assertTrue(loaded.classicCompleted[BirdGame3.BirdType.KIWI.ordinal()]);
    }

    @Test void fossilCheckpointRestoresInTheRealFlockRunArena() throws Exception {
        FlockRunState run = new FlockRunState(704, BirdGame3.BirdType.PTERODACTYL, "CLASSIC_SKIN_PTERODACTYL");
        FlockRunState restored = FlockRunState.decode(run.encode());
        assertNotNull(restored); assertEquals(run.bird, restored.bird); assertEquals(run.skinKey, restored.skinKey);
        assertTrue(restored.chooseRoute(FlockRunState.Route.SAFE));
        BirdGame3 game = new BirdGame3(prefs);
        Method unlock = BirdGame3.class.getDeclaredMethod("unlockEverythingForDeveloperProfile"); unlock.setAccessible(true);
        unlock.invoke(game);
        game.harnessPrepareFlockRunEncounter(restored, 5);
        assertEquals(BirdGame3.BirdType.PTERODACTYL, game.players[0].type);
        assertTrue(game.players[0].isClassicSkin);
        assertEquals("CLASSIC_SKIN_PTERODACTYL", game.players[0].appliedSkinKey);
    }

    @Test void currentReplayPreservesTheNewFighterAndOldRevisionRemainsStoredButIncompatible(@TempDir Path dir) {
        MatchReplay replay = new MatchReplay(712, 2);
        replay.mapName = "BATTLEFIELD"; replay.slotBirdTypes = new String[]{"PTERODACTYL", "KIWI"};
        replay.slotSkinKeys = new String[]{"CLASSIC_SKIN_PTERODACTYL", null};
        replay.slotIsAi = new boolean[]{false, false}; replay.slotTeams = new int[]{1, 2};
        replay.slotBaseSize = new double[]{1, 1}; replay.slotBasePower = new double[]{1, 1}; replay.slotBaseSpeed = new double[]{1, 1};
        replay.slotInitialStocks = new int[]{3, 3}; replay.slotInitialHealth = new double[]{200, 200};
        replay.frames.add(new int[]{17, 0});
        MatchReplay loaded = ReplayStore.load(ReplayStore.save(dir, replay));
        assertNotNull(loaded); assertTrue(loaded.compatibleWithCurrentSimulation());
        assertArrayEquals(replay.slotBirdTypes, loaded.slotBirdTypes);
        assertArrayEquals(replay.slotSkinKeys, loaded.slotSkinKeys);
        MatchReplay older = new MatchReplay(712, 2, 14); older.mapName = "BATTLEFIELD"; older.frames.add(new int[]{0, 0});
        older.slotBirdTypes = new String[]{"BAT", "KIWI"}; older.slotSkinKeys = new String[]{null, null};
        older.slotIsAi = replay.slotIsAi; older.slotTeams = replay.slotTeams;
        older.slotBaseSize = replay.slotBaseSize; older.slotBasePower = replay.slotBasePower; older.slotBaseSpeed = replay.slotBaseSpeed;
        MatchReplay oldLoaded = ReplayStore.load(ReplayStore.save(dir, older));
        assertNotNull(oldLoaded); assertFalse(oldLoaded.compatibleWithCurrentSimulation());
    }

    @Test void scriptedCarryInputsProduceIdenticalTickHashesAcrossFreshMatches() {
        assertArrayEquals(scriptedHashes(), scriptedHashes());
    }

    @Test void missingNetworkSnapshotLeavesFighterAndSpecialStateUnchanged() {
        BirdGame3 game = new BirdGame3(prefs);
        Bird bird = new Bird(200, BirdGame3.BirdType.PTERODACTYL, 0, game);
        bird.pterodactyl.phase = PterodactylSpecials.UPDRAFT;
        bird.pterodactyl.cooldowns[2] = 28;
        long specialHash = bird.pterodactyl.hash();
        assertDoesNotThrow(() -> bird.applyLanState(null));
        assertEquals(200, bird.x);
        assertEquals(BirdGame3.BirdType.PTERODACTYL, bird.type);
        assertEquals(specialHash, bird.pterodactyl.hash());
    }
    private long[] scriptedHashes() {
        BirdGame3 game = new BirdGame3(prefs);
        game.harnessPrepareMatch(BirdGame3.BirdType.PTERODACTYL, BirdGame3.BirdType.KIWI, 0x50746572L);
        game.isAI[0] = game.isAI[1] = false;
        Platform stage = game.platforms.stream().max(java.util.Comparator.comparingDouble(platform -> platform.w)).orElseThrow();
        double startX = stage.x + stage.w / 2 - 80;
        game.players[0].x = startX; game.players[0].y = stage.y - 380;
        game.players[1].x = startX + 100; game.players[1].y = stage.y - game.players[1].bodyHeight();
        long[] hashes = new long[420]; boolean captured = false;
        for (int tick = 0; tick < hashes.length; tick++) {
            game.setAiControlKey(0, game.blockKeyForPlayer(0), tick < 14);
            game.setAiControlKey(0, game.specialKeyForPlayer(0), tick < 55 || tick == 180);
            game.setAiControlKey(0, game.jumpKeyForPlayer(0), tick >= 200 && tick < 240);
            game.setAiControlKey(0, game.rightKeyForPlayer(0), tick >= 100 && tick < 190);
            game.setAiControlKey(1, game.attackKeyForPlayer(1), tick > 250 && tick % 17 < 4);
            game.harnessTick();
            captured |= game.players[0].pterodactylTarget() != null;
            hashes[tick] = game.harnessStateHash();
        }
        assertTrue(captured, "Determinism trace must include a real carry, not just idle frames");
        return hashes;
    }

    @Test void charlesCopiesWingGustAndClearsItsStateAfterRecovery() {
        BirdGame3 game = new BirdGame3(prefs); game.activePlayers = 2;
        Bird charles = game.players[0] = new Bird(200, BirdGame3.BirdType.MOCKINGBIRD, 0, game);
        Bird target = game.players[1] = new Bird(300, BirdGame3.BirdType.PTERODACTYL, 1, game);
        charles.y = target.y = BirdGame3.GROUND_Y - 300;
        PterodactylSpecials.copiedNeutral(charles);
        charles.mockingbirdCopiedNeutralSource = BirdGame3.BirdType.PTERODACTYL;
        double before = target.health;
        for (int tick = 0; tick < 40; tick++) PterodactylSpecials.tick(charles, true);
        assertTrue(target.health < before); assertFalse(PterodactylSpecials.active(charles));
        assertNull(charles.pterodactylTarget());
    }

    @Test void classicRouteProvidesAuthoredStocksAndLaunchableGiantAndBossBodies() {
        BirdGame3 game = new BirdGame3(prefs);
        for (int round : new int[]{0, 3, 4, 7}) {
            game.harnessPrepareClassicEncounter(BirdGame3.BirdType.PTERODACTYL, round, 5, 5, 1844, 9123 + round);
            assertEquals(round == 7 ? 3 : 2, game.scores[0], "Wrong player stocks in round " + (round + 1));
            Bird enemy = game.players[1];
            if (round == 3) assertTrue(enemy.sizeMultiplier > 1.3 && enemy.sizeMultiplier < 1.5,
                    "The giant must retain its identity without generic oversized resistance");
            if (round == 7) {
                assertEquals(2, game.scores[1]);
                assertTrue(enemy.sizeMultiplier < 1.0, "The finale must remain launchable");
                assertEquals(BirdGame3.BirdType.EAGLE, enemy.type);
            }
        }
    }
}
