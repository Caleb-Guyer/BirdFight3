package com.example.birdgame3;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FightHudNameLayoutTest {
    @Test
    void manufacturedVoiceCannotPaintIntoSingleDigitDamage() {
        BirdGame3 game = new BirdGame3();

        BirdGame3.FightHudNameFit fit = game.fitFightHudNameFont(
                "MANUFACTURED VOICE", 360.0, 88.0, "2");

        assertTrue(fit.font().getSize() < 16.0,
                "The old 16 px floor was too large for this four-fighter HUD card.");
        assertTrue(fit.renderedWidth() <= fit.maxWidth());
        assertTrue(fit.maxWidth() > 100.0,
                "The title should remain readable instead of being reduced to a token sliver.");
    }

    @Test
    void safeTitleWidthReservesMoreRoomAsDamageGainsDigits() {
        BirdGame3 game = new BirdGame3();

        BirdGame3.FightHudNameFit oneDigit = game.fitFightHudNameFont(
                "MANUFACTURED VOICE", 360.0, 88.0, "2");
        BirdGame3.FightHudNameFit threeDigits = game.fitFightHudNameFont(
                "MANUFACTURED VOICE", 360.0, 88.0, "125");

        assertTrue(threeDigits.maxWidth() < oneDigit.maxWidth());
        assertTrue(threeDigits.renderedWidth() <= threeDigits.maxWidth());
    }

    @Test
    void ordinaryNamesKeepTheFullHeadlineSize() {
        BirdGame3 game = new BirdGame3();

        BirdGame3.FightHudNameFit fit = game.fitFightHudNameFont(
                "PIGEON", 430.0, 88.0, "0");

        assertEquals(22.0, fit.font().getSize(), 0.0001);
        assertEquals(fit.naturalWidth(), fit.renderedWidth(), 0.0001);
    }

    @Test
    void damageAndMetersStayInsideTallAndCompactCards() {
        BirdGame3 game = new BirdGame3();

        for (double panelHeight : new double[]{146.0, 126.0}) {
            BirdGame3.FightHudMeterLayout layout = game.fightHudMeterLayout(panelHeight);
            double ultimateBarY = layout.ultimateBarYOffset(panelHeight);
            double portraitBottom = panelHeight >= 140.0 ? 113.0 : 95.0;

            assertTrue(layout.percentRightInset() >= 20.0,
                    "The percent sign needs visible breathing room from the card edge.");
            assertTrue(layout.healthBarYOffset() - layout.damageBaselineOffset() >= 12.0,
                    "The meter must not paint over the bottom of the damage text.");
            assertTrue(layout.healthBarBottomOffset() <= ultimateBarY - 10.0,
                    "The health and ultimate meters need a clear gap.");
            assertTrue(ultimateBarY - portraitBottom >= 4.0,
                    "The integrated ULT rail must clear the portrait.");
            assertTrue(layout.ultimateLabelBaselineOffset(panelHeight) > ultimateBarY
                            && layout.ultimateLabelBaselineOffset(panelHeight)
                            < ultimateBarY + layout.ultimateBarHeight(),
                    "The ULT label must be integrated inside its rail.");
            double bottomInset = panelHeight - (ultimateBarY + layout.ultimateBarHeight());
            assertTrue(bottomInset >= 12.0 && bottomInset <= 18.0,
                    "The ULT rail should feel centered in the lower band, not glued to the edge.");
        }
    }
}
