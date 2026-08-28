package com.example.birdgame3;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FrontEndMatchFlowTest {
    @Test
    void versusJourneyAndBackPathAreExplicit() {
        FrontEndMatchFlow flow = new FrontEndMatchFlow();
        assertEquals(FrontEndMatchFlow.Screen.TITLE, flow.screen());

        flow.showHub();
        flow.beginVersus();
        assertEquals(FrontEndMatchFlow.Screen.RULES, flow.screen());
        flow.confirmRules();
        assertEquals(FrontEndMatchFlow.Screen.FIGHTERS, flow.screen());
        flow.confirmFighters(true);
        assertEquals(FrontEndMatchFlow.Screen.STAGES, flow.screen());
        flow.showLoading();
        flow.beginBattle();
        flow.showResults();
        assertEquals(FrontEndMatchFlow.Screen.RESULTS, flow.screen());
        assertEquals(FrontEndMatchFlow.Screen.HUB, flow.back());

        flow.beginVersus();
        flow.confirmRules();
        flow.confirmFighters(true);
        assertEquals(FrontEndMatchFlow.Screen.FIGHTERS, flow.back());
        assertEquals(FrontEndMatchFlow.Screen.RULES, flow.back());
        assertEquals(FrontEndMatchFlow.Screen.HUB, flow.back());
    }

    @Test
    void fightersCannotAdvanceUntilEverySlotIsReady() {
        FrontEndMatchFlow flow = new FrontEndMatchFlow();
        flow.showHub();
        flow.beginVersus();
        flow.confirmRules();
        assertThrows(IllegalStateException.class, () -> flow.confirmFighters(false));
        assertEquals(FrontEndMatchFlow.Screen.FIGHTERS, flow.screen());
    }

    @Test
    void savedRulesFallbackSafelyAndMapToRealSimulationModes() {
        FrontEndMatchFlow flow = new FrontEndMatchFlow();
        flow.restoreRulesPreset("competitive");
        assertEquals(VersusRulesPreset.COMPETITIVE, flow.rulesPreset());
        assertTrue(flow.rulesPreset().competitionMode);
        assertFalse(flow.rulesPreset().mutatorMode);

        flow.restoreRulesPreset("removed-in-a-future-build");
        assertEquals(VersusRulesPreset.STANDARD, flow.rulesPreset());
        assertEquals("3 stocks  •  2:30  •  items on  •  hazards on  •  ults on",
                flow.rulesPreset().summary);
    }

    @Test
    void loadingAndBattleCannotBeEnteredOutOfOrder() {
        FrontEndMatchFlow flow = new FrontEndMatchFlow();
        assertThrows(IllegalStateException.class, flow::showLoading);
        assertThrows(IllegalStateException.class, flow::beginBattle);

        flow.beginVersus();
        flow.confirmRules();
        flow.confirmFighters(true);
        flow.showLoading();
        flow.beginBattle();
        assertEquals(FrontEndMatchFlow.Screen.BATTLE, flow.screen());
    }

    @Test
    void customRulesAndLoadingBackPathKeepExactConfiguration() {
        FrontEndMatchFlow flow = new FrontEndMatchFlow();
        VersusRules custom = VersusRules.standard().withName("NO ULTS")
                .withStockCount(5).withUltimatesEnabled(false);
        flow.restoreRules("custom", custom);
        assertEquals(VersusRulesPreset.CUSTOM, flow.rulesPreset());
        assertEquals(custom, flow.rules());

        flow.beginVersus();
        flow.confirmRules();
        flow.confirmFighters(true);
        flow.showLoading();
        assertEquals(FrontEndMatchFlow.Screen.STAGES, flow.back());
        assertEquals(custom, flow.rules());
    }

    @Test
    void everyJourneyScreenPublishesACompleteDeviceAwareGuide() {
        for (FrontEndMatchFlow.Screen screen : FrontEndMatchFlow.Screen.values()) {
            FrontEndMatchFlow.ScreenGuide guide = FrontEndMatchFlow.guideFor(screen);
            assertFalse(guide.breadcrumb().isBlank(), screen + " must identify the current step");
            assertFalse(guide.primaryAction().isBlank(), screen + " must expose its primary action");
            assertFalse(guide.prompts().isEmpty(), screen + " must expose its active controls");
            for (UiInputPrompts.Device device : UiInputPrompts.Device.values()) {
                assertFalse(UiInputPrompts.render(device,
                                guide.prompts().toArray(UiInputPrompts.Prompt[]::new)).isBlank(),
                        screen + " must render controls for " + device);
            }
        }

        FrontEndMatchFlow.ScreenGuide fighters = FrontEndMatchFlow.guideFor(
                FrontEndMatchFlow.Screen.FIGHTERS);
        assertTrue(fighters.prompts().stream()
                .anyMatch(prompt -> prompt.command() == UiInputPrompts.Command.POINTER));
        assertTrue(fighters.prompts().stream()
                .anyMatch(prompt -> prompt.command() == UiInputPrompts.Command.READY));
        assertEquals("HUB", FrontEndMatchFlow.guideFor(
                FrontEndMatchFlow.Screen.RESULTS).backAction());
    }

    @Test
    void backTargetsAreInspectableWithoutMutatingTheJourney() {
        FrontEndMatchFlow flow = new FrontEndMatchFlow();
        assertEquals(FrontEndMatchFlow.Screen.TITLE, flow.backTarget());
        assertFalse(flow.canGoBack());

        flow.showHub();
        flow.beginVersus();
        flow.confirmRules();
        flow.confirmFighters(true);
        flow.showLoading();
        assertEquals(FrontEndMatchFlow.Screen.STAGES, flow.backTarget());
        assertEquals(FrontEndMatchFlow.Screen.LOADING, flow.screen());
        assertTrue(flow.canGoBack());

        flow.beginBattle();
        assertEquals(FrontEndMatchFlow.Screen.BATTLE, flow.backTarget());
        assertFalse(flow.canGoBack(), "battle owns pause/exit instead of a menu back transition");
    }
}
