package com.example.birdgame3;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VaultUiModernizationTest {
    private static final Path GAME_SOURCE = Path.of(
            "src", "main", "java", "com", "example", "birdgame3", "BirdGame3.java");

    @Test
    void vaultUsesConciseRecordsAndActiveDevicePrompts() throws IOException {
        String source = Files.readString(GAME_SOURCE);
        String vault = methodBody(source, "showVault");
        String fighterCard = methodBody(source, "buildVaultFighterCard");

        assertTrue(vault.contains("buildAdaptivePromptBar("));
        assertFalse(vault.contains("Earned progress stays separate from developer entitlement"));
        assertFalse(vault.contains("Everything collected, earned, watched, recorded"));
        assertFalse(fighterCard.contains("TOTAL DMG"));
        assertFalse(fighterCard.contains("TIME \" + progress.arenaTimeText()"));
    }

    @Test
    void vaultLandingShowsDestinationsWhileFighterRecordsOwnTheirGrid() throws IOException {
        String source = Files.readString(GAME_SOURCE);
        String vault = methodBody(source, "showVault");
        String records = methodBody(source, "showVaultFighterRecords");

        assertTrue(vault.contains("FIGHTER RECORDS"));
        assertTrue(vault.contains("libraryGrid.add(destinations[i], i % 3, i / 3)"));
        assertFalse(vault.contains("buildVaultFighterCard(stage, progress)"));
        assertFalse(vault.contains("buildVaultSummaryChip"));
        assertTrue(records.contains("buildVaultFighterCard(stage, progress)"));
        assertTrue(records.contains("buildAdaptivePromptBar("));
        assertTrue(records.contains("bindEscape(scene, back)"));
    }

    @Test
    void achievementsAndFeatherpediaDoNotDuplicateHeaderInformation() throws IOException {
        String source = Files.readString(GAME_SOURCE);
        String achievements = methodBodyAt(source,
                "private void showAchievements(Stage stage, BirdGame3AchievementCategory category)",
                "showAchievements");
        String featherpedia = methodBodyAt(source,
                "private void showBirdBook(Stage stage, BirdBookCategory category)", "showBirdBook");

        assertTrue(achievements.contains("buildAdaptivePromptBar("));
        assertFalse(achievements.contains("Unlock challenges, claim rewards"));
        assertFalse(achievements.contains("CLAIMABLE REWARDS"));
        assertTrue(featherpedia.contains("buildAdaptivePromptBar("));
        assertFalse(featherpedia.contains("activeTabChip"));
    }

    @Test
    void shopUsesStableCardsAndChargesTheDisplayedDiscount() throws IOException {
        String shop = methodBody(Files.readString(GAME_SOURCE), "showShop");

        assertTrue(shop.contains("buildAdaptivePromptBar("));
        assertTrue(shop.contains("double cardW = item.bundle ? 500 : 420"));
        assertFalse(shop.contains("if (previewCount > 10)"));
        assertTrue(shop.contains("final int purchaseCost = effectiveCost"));
        assertFalse(shop.contains("VALUE: "));
    }

    @Test
    void vaultArchivesAndRewardCardsExposeInputAwareNavigation() throws IOException {
        String source = Files.readString(GAME_SOURCE);
        for (String method : new String[]{"showVaultSoundtrack", "showMatchHistory", "showReplayBrowser",
                "showPackResult", "showUnlockCard"}) {
            assertTrue(methodBody(source, method).contains("buildAdaptivePromptBar("),
                    method + " must show controls for only the active input device");
        }
        assertTrue(methodBody(source, "showPackResult").contains("bindEscape(scene, back)"));
    }

    private static String methodBody(String source, String methodName) {
        int name = source.indexOf("private void " + methodName + "(");
        if (name < 0) name = source.indexOf("private Button " + methodName + "(");
        assertTrue(name >= 0, "Missing method " + methodName);
        return methodBodyAt(source, source.substring(name, source.indexOf('{', name)), methodName);
    }

    private static String methodBodyAt(String source, String signature, String methodName) {
        int name = source.indexOf(signature);
        assertTrue(name >= 0, "Missing method " + methodName);
        int open = source.indexOf('{', name);
        int depth = 0;
        for (int i = open; i < source.length(); i++) {
            char c = source.charAt(i);
            if (c == '{') depth++;
            if (c == '}' && --depth == 0) return source.substring(open, i + 1);
        }
        throw new AssertionError("Unclosed body for " + methodName);
    }
}
