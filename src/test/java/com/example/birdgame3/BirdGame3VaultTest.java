package com.example.birdgame3;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BirdGame3VaultTest {
    private static final Path GAME_SOURCE = Path.of(
            "src", "main", "java", "com", "example", "birdgame3", "BirdGame3.java");

    @Test
    void developerEntitlementUnlocksFighterWithoutForgingRouteBadgeOrEnding() throws Exception {
        BirdGame3 game = new BirdGame3();
        Method unlock = BirdGame3.class.getDeclaredMethod("unlockEverythingForDeveloperProfile");
        unlock.setAccessible(true);
        unlock.invoke(game);

        Method summaries = BirdGame3.class.getDeclaredMethod("vaultFighterProgress");
        summaries.setAccessible(true);
        @SuppressWarnings("unchecked")
        List<VaultFighterProgress> progress = (List<VaultFighterProgress>) summaries.invoke(game);
        VaultFighterProgress falcon = progress.stream()
                .filter(row -> row.bird() == BirdGame3.BirdType.FALCON)
                .findFirst()
                .orElseThrow();

        assertTrue(falcon.fighterUnlocked());
        assertFalse(falcon.endingAvailable(), "Classic endings remain proof of an earned route badge");
        assertFalse(falcon.routeBadgeEarned(), "FEATHERDEV must never manufacture earned badges");
    }

    @Test
    void arenaTimeUsesSimulationTicksAndIsRecordedWithRosterStats() throws Exception {
        BirdGame3 game = new BirdGame3();
        game.harnessPrepareMatch(BirdGame3.BirdType.PIGEON, BirdGame3.BirdType.EAGLE, 44L);
        game.simTick = 3_600L;
        game.recordBalanceOutcome(game.players[0]);

        Field framesField = BirdGame3.class.getDeclaredField("typePlayFrames");
        framesField.setAccessible(true);
        long[] frames = (long[]) framesField.get(game);
        assertTrue(frames[BirdGame3.BirdType.PIGEON.ordinal()] == 3_600L);
        assertTrue(frames[BirdGame3.BirdType.EAGLE.ordinal()] == 3_600L);
    }

    @Test
    void hubRoutesCollectionsThroughOneVaultAndVaultOwnsAllLibraries() throws Exception {
        String source = Files.readString(GAME_SOURCE);
        String hub = methodBody(source, "showHub");
        String vault = methodBody(source, "showVault");

        assertTrue(hub.contains("buildUltimateHubRailButton(\"VAULT\""));
        assertFalse(hub.contains("buildUltimateHubRailButton(\"HISTORY\""));
        assertFalse(hub.contains("buildUltimateHubRailButton(\"FEATHERPEDIA\""));
        assertFalse(hub.contains("buildUltimateHubRailButton(\"ACHIEVEMENTS\""));
        for (String destination : new String[]{
                "ACHIEVEMENTS", "FEATHERPEDIA", "CLASSIC ENDINGS", "STORY MOVIES",
                "MATCH RECORDS", "REPLAYS", "SOUND & CREDITS", "BIRD COIN SHOP"}) {
            assertTrue(vault.contains(destination), "Vault lost " + destination);
        }
        assertTrue(vault.contains("vaultFighterProgress()"));
    }

    private static String methodBody(String source, String methodName) {
        int name = source.indexOf("private void " + methodName + "(");
        if (name < 0) throw new AssertionError("Missing method " + methodName);
        int open = source.indexOf('{', name);
        int depth = 0;
        for (int i = open; i < source.length(); i++) {
            char c = source.charAt(i);
            if (c == '{') depth++;
            if (c == '}' && --depth == 0) return source.substring(open, i + 1);
        }
        throw new AssertionError("Unclosed method " + methodName);
    }
}
