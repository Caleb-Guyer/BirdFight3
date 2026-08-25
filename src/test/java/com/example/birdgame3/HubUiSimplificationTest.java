package com.example.birdgame3;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HubUiSimplificationTest {
    private static final Path GAME_SOURCE = Path.of(
            "src", "main", "java", "com", "example", "birdgame3", "BirdGame3.java");

    @Test
    void primaryHubKeepsModesLargeAndRetainsItsCompactTopTipStrip() throws IOException {
        String source = Files.readString(GAME_SOURCE).replace("\r\n", "\n");
        String hub = methodBody(source, "private void showHub(Stage stage)");

        assertEquals(5, occurrences(hub, "buildUltimateHubMainTileButton("),
                "the hub should keep the four play destinations plus the Vault large");
        assertEquals(4, occurrences(hub, "buildUltimateHubRailButton("),
                "utilities should remain on the compact side rail");
        assertTrue(hub.contains("HubPresentationModel.IDLE_TITLE"));
        assertTrue(hub.contains("HubPresentationModel.Destination.FIGHT.description()"));
        assertTrue(hub.contains("buildAdaptivePromptBar("));
        assertTrue(hub.contains("bindFixedFrameScale(scene, frame, 0.0);"));
        assertTrue(hub.contains("buildUltimateHubTipPanel(randomHubTip())"));
        assertEquals(HubPresentationModel.Destination.values().length,
                occurrences(hub, "tagUltimateHubDestination("),
                "every hub destination should drive the center preview");
        assertTrue(hub.contains("setUltimateHubDrawerExpanded("),
                "the utility rail should open as a drawer instead of permanently consuming space");
        assertTrue(hub.contains("AnchorPane.setLeftAnchor(railShell, 1496.0)"));
        assertTrue(hub.contains("railShell.setTranslateX(0.0)"),
                "the utility drawer should start in its compact state");
        assertTrue(hub.contains("if (utilityDrawerExpanded[0])"),
                "Escape should close the open drawer before leaving the hub");
        assertTrue(hub.contains("buildUltimateHubRailButton(\"SHOP\""),
                "the Bird Coin Shop should live on the utility drawer");
        assertTrue(hub.contains("Button vaultNode"),
                "the collection Vault should remain a large main-screen destination");
        assertTrue(hub.contains("hubIconVault(claimableRewards)"),
                "the restored Vault tile should use the Vault icon and reward marker");
        assertTrue(hub.contains("animateVaultExit(frame, () -> showVault(stage))"),
                "entering the Vault should use the dedicated transition");
        assertTrue(hub.contains("animateVaultExit(frame, () -> showShop(stage))"),
                "the sidebar Shop should open the actual Bird Coin Shop smoothly");
        assertTrue(source.contains("ROOST TIP"));
        assertTrue(source.contains("private String randomHubTip()"));
    }

    @Test
    void compactUtilityRailShowsIconsAndExpandedRowsPutTextAfterThem() throws IOException {
        String source = Files.readString(GAME_SOURCE).replace("\r\n", "\n");
        String railButton = methodBody(source,
                "private Button buildUltimateHubRailButton(String text, double height, Node icon, Runnable action)");
        String drawerState = methodBody(source,
                "private void setUltimateHubDrawerExpanded(StackPane drawer, Button toggle,");

        assertTrue(railButton.contains("new HBox(14, iconFrame, label, spacer)"),
                "expanded utility rows should read icon first, then label");
        assertTrue(drawerState.contains("double targetX = expanded ? -256.0 : 0.0"),
                "the drawer should expand leftward from its visible icon strip");
        assertTrue(drawerState.contains("expanded ? \"UTILITY MENU\" : \"\""),
                "collapsed drawer chrome should not expose clipped text");
    }

    @Test
    void centerMedallionProvidesAnIllustratedPreviewForEveryDestination() throws IOException {
        String source = Files.readString(GAME_SOURCE).replace("\r\n", "\n");
        String updater = methodBody(source,
                "private void updateUltimateHubCenterPreview(Node medallion,");
        String backdrop = methodBody(source,
                "private void drawUltimateHubPreviewBackdrop(Canvas canvas,");
        String layout = methodBody(source,
                "private void layoutUltimateHubPreviewPortraits(Canvas primary, Canvas secondary,");

        for (HubPresentationModel.Destination destination : HubPresentationModel.Destination.values()) {
            assertTrue(updater.contains("case " + destination.name()),
                    "missing portrait treatment for " + destination);
            assertTrue(backdrop.contains("case " + destination.name()),
                    "missing illustrated backdrop for " + destination);
            assertTrue(layout.contains("case " + destination.name()),
                    "missing a distinct portrait composition for " + destination);
        }
        String portrait = methodBody(source,
                "private void drawUltimateHubPreviewPortrait(Canvas canvas, BirdType type)");
        assertTrue(portrait.contains("drawRosterSprite(canvas, type, null, false)"),
                "the preview should feature the game's real bird artwork");
        assertTrue(updater.contains("FadeTransition"),
                "preview changes should be presented cleanly");
        assertTrue(backdrop.contains("StagePreviewRenderer.draw(canvas, stagePicture)"),
                "combat-oriented destinations should incorporate current stage captures");
        assertTrue(backdrop.contains("MapType.BATTLEFIELD"));
        assertTrue(backdrop.contains("MapType.BEACON_CROWN"));
        assertTrue(backdrop.contains("MapType.CITY"));
    }

    @Test
    void gamesDashboardUsesTheSameConciseContextContract() throws IOException {
        String source = Files.readString(GAME_SOURCE).replace("\r\n", "\n");
        String games = methodBody(source, "private void showClassicMoreMenu(Stage stage)");

        assertEquals(5, occurrences(games, "registerHubInteractiveNode("));
        assertTrue(games.contains("HubPresentationModel.ExtraMode.CLASSIC.description()"));
        assertTrue(games.contains("HubPresentationModel.ExtraMode.TRAINING.description()"));
        assertFalse(games.contains("showTournamentMode(stage)"),
                "the bracket belongs under Fight, not Games & More");
        assertFalse(games.contains("showSquadStrikeMode(stage)"),
                "ordered flock battles belong under Fight, not Games & More");
        assertFalse(games.contains("Pick a route"));
        assertFalse(games.contains("route ladder with branching encounters"));
    }

    @Test
    void fightDashboardOwnsEveryLocalBattleFormatWithoutDuplicatingThem() throws IOException {
        String source = Files.readString(GAME_SOURCE).replace("\r\n", "\n");
        String hub = methodBody(source, "private void showHub(Stage stage)");
        String fight = methodBody(source, "private void showFightMenu(Stage stage)");

        assertTrue(hub.contains("animateFightMenuExit(frame, () -> showFightMenu(stage))"),
                "Fight should enter its dashboard through the shared clean transition");
        assertEquals(HubPresentationModel.FightMode.values().length,
                occurrences(fight, "registerHubInteractiveNode("));
        assertTrue(fight.contains("showVersusRules(stage)"));
        assertTrue(fight.contains("showSquadStrikeMode(stage)"));
        assertTrue(fight.contains("showTournamentMode(stage)"));
        assertTrue(fight.contains("showWildRules(stage)"));
        assertTrue(fight.contains("fightMenuIconBirdBattle()"));
        assertTrue(fight.contains("gamesMoreIconSquadStrike()"));
        assertTrue(fight.contains("gamesMoreIconTournament()"));
        assertTrue(fight.contains("fightMenuIconWildRules()"));
        assertTrue(fight.contains("buildFightMenuHeroArt()"),
                "the lead battle destination should have dedicated posed-bird artwork");
        assertEquals(HubPresentationModel.FightMode.values().length,
                occurrences(fight, "updateFightMenuHeroArt(heroArt,"),
                "every Fight tile should own a distinct selection-driven picture");
        assertEquals(HubPresentationModel.FightMode.values().length,
                occurrences(fight, "installHubSelectionPreview("));
        assertTrue(fight.contains("playFightMenuEntrance("));
        assertTrue(fight.contains("animateFightMenuExit("));
    }

    @Test
    void wildRulesIsAnAnimatedFunctionalDashboardWithChangingArt() throws IOException {
        String source = Files.readString(GAME_SOURCE).replace("\r\n", "\n");
        String wild = methodBody(source, "private void showWildRules(Stage stage)");
        String openMode = methodBody(source,
                "private void openWildRulesMode(Stage stage, HubPresentationModel.WildMode mode)");
        String swap = methodBody(source,
                "private void swapFightPreview(StackPane shell, Object key, Pane next, String style,");

        assertEquals(HubPresentationModel.WildMode.values().length,
                occurrences(wild, "registerHubInteractiveNode("));
        assertEquals(HubPresentationModel.WildMode.values().length,
                occurrences(wild, "updateWildRulesHeroArt(heroArt,"),
                "every Wild Rules tile should swap in its own posed-bird scene");
        assertTrue(wild.contains("buildWildRulesHeroArt()"));
        assertTrue(wild.contains("fightMenuIconWildRules()"));
        assertTrue(wild.contains("fightMenuIconStaminaClash()"));
        assertTrue(wild.contains("fightMenuIconLaunchstorm()"));
        assertTrue(wild.contains("playFightMenuEntrance("));
        assertTrue(wild.contains("animateFightMenuExit("));

        assertTrue(openMode.contains("VersusRulesPreset.STAMINA"),
                "Stamina Clash must use the real stamina simulation rules");
        assertTrue(openMode.contains("withLaunchRatePercent(200)"),
                "Launchstorm must use the real maximum launch-rate setting");
        assertTrue(openMode.contains("continueFromVersusRulesSelection(stage, false)"));
        assertTrue(swap.contains("FadeTransition"));
        assertTrue(swap.contains("TranslateTransition"));
    }

    @Test
    void movedBattleModesReturnToFightAndRulesUseBirdFightLanguage() throws IOException {
        String source = Files.readString(GAME_SOURCE).replace("\r\n", "\n");
        String squad = methodBody(source, "private void showSquadStrikeMode(Stage stage)");
        String bracket = methodBody(source, "private void showTournamentMode(Stage stage)");
        String rulesets = methodBody(source, "private void showVersusRulesets(Stage stage, boolean networkLobby)");
        String editor = methodBody(source, "private void showVersusRulesEditor(Stage stage, boolean networkLobby)");

        assertTrue(squad.contains("showFightMenu(stage)"));
        assertTrue(bracket.contains("showFightMenu(stage)"));
        assertTrue(rulesets.contains("FIGHT  ›  RULESETS"));
        assertTrue(editor.contains("FIGHT  ›  RULESETS  ›  EDIT"));
        assertFalse(rulesets.contains("SMASH  ›  RULESETS"));
        assertFalse(editor.contains("SMASH  ›  RULESETS  ›  EDIT"));
    }

    private static int occurrences(String value, String token) {
        int count = 0;
        int offset = 0;
        while ((offset = value.indexOf(token, offset)) >= 0) {
            count++;
            offset += token.length();
        }
        return count;
    }

    private static String methodBody(String source, String signature) {
        int start = source.indexOf(signature);
        assertTrue(start >= 0, "missing " + signature);
        int open = source.indexOf('{', start);
        int depth = 0;
        for (int i = open; i < source.length(); i++) {
            char c = source.charAt(i);
            if (c == '{') depth++;
            if (c == '}' && --depth == 0) return source.substring(open, i + 1);
        }
        throw new AssertionError("unterminated " + signature);
    }
}
