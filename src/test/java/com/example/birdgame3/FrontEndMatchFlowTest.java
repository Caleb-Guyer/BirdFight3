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
}
