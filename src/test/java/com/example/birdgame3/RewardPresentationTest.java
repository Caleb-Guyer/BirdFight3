package com.example.birdgame3;

import javafx.scene.control.ButtonType;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import org.junit.jupiter.api.Test;

import java.util.List;

import static com.example.birdgame3.BirdGame3.BirdType;
import static com.example.birdgame3.BirdGame3.MapType;
import static org.junit.jupiter.api.Assertions.*;

class RewardPresentationTest {
    @Test
    void everyRewardLayoutKeepsArtTextAndActionsSeparate() {
        for (RewardPresentation.Kind kind : RewardPresentation.Kind.values()) {
            var layout = RewardRevealView.layout(kind);
            var boxes = List.of(layout.art(), layout.name(), layout.category(), layout.detail(),
                    RewardRevealView.FOOTER);
            for (var box : boxes) {
                assertTrue(box.x() >= 0 && box.y() >= 0);
                assertTrue(box.x() + box.width() <= RewardRevealView.WIDTH);
                assertTrue(box.y() + box.height() <= RewardRevealView.HEIGHT);
                for (var other : boxes) {
                    if (box == other) continue;
                    assertFalse(box.x() < other.x() + other.width()
                            && other.x() < box.x() + box.width()
                            && box.y() < other.y() + other.height()
                            && other.y() < box.y() + box.height(), kind + " overlaps: " + box + " / " + other);
                }
            }
            for (double[] size : new double[][]{{1280, 720}, {1920, 1080}, {2560, 1600}, {1024, 768}}) {
                double scale = UiLayoutMetrics.fitScale(size[0], size[1],
                        RewardRevealView.WIDTH, RewardRevealView.HEIGHT);
                assertTrue(scale > 0);
                assertTrue(RewardRevealView.WIDTH * scale <= size[0] + .001);
                assertTrue(RewardRevealView.HEIGHT * scale <= size[1] + .001);
            }
        }
    }

    @Test
    void allRosterNamesAndLongRewardsFitWithoutTruncatingWords() {
        var bird = RewardRevealView.layout(RewardPresentation.Kind.BIRD).name();
        for (BirdType type : BirdType.values()) assertFits(type.name, 88, bird);
        var skin = RewardRevealView.layout(RewardPresentation.Kind.SKIN).name();
        for (String name : List.of("Lore Accurate Hummingbird", "Ashen Sovereign Phoenix",
                "The Null Rock", "Sunflare Hummingbird", "Bird Coins +2,000,000")) {
            assertFits(name, 56, skin);
            assertFits(name, 33, new RewardRevealView.Box(0, 0, 418, 118));
        }
        var stage = RewardRevealView.layout(RewardPresentation.Kind.STAGE).name();
        for (MapType type : MapType.values()) assertFits(type.name().replace('_', ' '), 56, stage);
    }

    @Test
    void missingArtworkOrEmptyNamesCannotBecomeBrokenRewardScreens() {
        assertThrows(IllegalArgumentException.class, () -> new RewardPresentation(
                RewardPresentation.Kind.BIRD, "Bird", "", null, null, null));
        assertThrows(IllegalArgumentException.class, () -> new RewardPresentation(
                RewardPresentation.Kind.STAGE, "Stage", "", null, null, null));
        assertThrows(IllegalArgumentException.class, () -> new RewardPresentation(
                RewardPresentation.Kind.COINS, "  ", "", null, null, null));
        var reward = new RewardPresentation(RewardPresentation.Kind.COINS, " Coins ", null, null, null, null);
        assertEquals("Coins", reward.name());
        assertEquals("", reward.detail());
    }

    @Test
    void confirmationDefaultsToNoOrCancelButTextEntryKeepsSubmit() {
        assertSame(ButtonType.NO, ModernDialogTheme.safeDefault(true, List.of(ButtonType.YES, ButtonType.NO)));
        assertSame(ButtonType.CANCEL, ModernDialogTheme.safeDefault(true,
                List.of(ButtonType.OK, ButtonType.CANCEL)));
        assertNull(ModernDialogTheme.safeDefault(false, List.of(ButtonType.OK, ButtonType.CANCEL)));
        assertNull(ModernDialogTheme.safeDefault(true, List.of(ButtonType.OK)));
    }

    @Test
    void shortAndLongDialogActionsReserveSpaceForTheirCompleteText() {
        for (String action : List.of("Yes", "No", "Cancel", "Import and Replace", "Keep Current Save")) {
            Text text = new Text(action);
            text.setFont(Font.font("Arial Black", 20));
            double width = ModernDialogTheme.actionWidth(action);
            assertTrue(width >= 128);
            assertTrue(width >= text.getLayoutBounds().getWidth() + 72);
        }
    }

    private static void assertFits(String name, double preferred, RewardRevealView.Box box) {
        double width = box.width() - 12;
        double height = box.height() - 8;
        Font font = RewardRevealView.fittedFont(name, preferred, width, height);
        Text text = new Text(name);
        text.setFont(font);
        text.setWrappingWidth(width);
        assertTrue(text.getLayoutBounds().getHeight() <= height, name + " is vertically clipped");
        assertTrue(font.getSize() >= 18, name + " became too small to read");
        for (String word : name.split("\\s+")) {
            Text part = new Text(word);
            part.setFont(font);
            assertTrue(part.getLayoutBounds().getWidth() <= width, name + " is horizontally clipped");
        }
    }
}
