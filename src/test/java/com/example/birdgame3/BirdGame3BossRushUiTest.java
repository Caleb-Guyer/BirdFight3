package com.example.birdgame3;

import org.junit.jupiter.api.Test;

import javafx.scene.Node;
import javafx.scene.Parent;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BirdGame3BossRushUiTest {
    @Test
    void classicSelectBadgesStayHiddenDuringBossRush() throws Exception {
        BirdGame3 game = new BirdGame3();
        setClassicCompleted(game, BirdGame3.BirdType.PIGEON, true);

        assertTrue(game.shouldShowClassicSelectBadge(BirdGame3.BirdType.PIGEON, false));
        assertFalse(game.shouldShowClassicSelectBadge(BirdGame3.BirdType.PIGEON, true));
        assertEquals("ROUTE", game.birdSelectBadgeLabel(BirdGame3.BirdType.PIGEON, false));
        assertNull(game.birdSelectBadgeLabel(BirdGame3.BirdType.PIGEON, true));
    }

    @Test
    void bossRushBadgesTrackPerBirdClearAndPerfectRoute() throws Exception {
        BirdGame3 game = new BirdGame3();
        setBossRushRecord(game, BirdGame3.BirdType.PIGEON, 610_000L, "A");
        setBossRushPerfectBadge(game, BirdGame3.BirdType.PIGEON, true);
        setBossRushRecord(game, BirdGame3.BirdType.EAGLE, 720_000L, "B");

        assertTrue(game.shouldShowBossRushSelectCompletionBadge(BirdGame3.BirdType.PIGEON));
        assertTrue(game.shouldShowBossRushSelectPerfectBadge(BirdGame3.BirdType.PIGEON));
        assertEquals("PERFECT", game.bossRushBadgeDisplayLabel(BirdGame3.BirdType.PIGEON));
        assertEquals("CLEAR", game.bossRushBadgeDisplayLabel(BirdGame3.BirdType.EAGLE));
        assertFalse(game.shouldShowBossRushSelectCompletionBadge(BirdGame3.BirdType.BAT));
        assertFalse(game.shouldShowBossRushSelectPerfectBadge(BirdGame3.BirdType.BAT));
        assertEquals("NOT EARNED", game.bossRushBadgeDisplayLabel(BirdGame3.BirdType.BAT));
        assertEquals("PERFECT", game.birdSelectBadgeLabel(BirdGame3.BirdType.PIGEON, true));
        assertEquals("CLEAR", game.birdSelectBadgeLabel(BirdGame3.BirdType.EAGLE, true));
        assertNull(game.birdSelectBadgeLabel(BirdGame3.BirdType.BAT, true));
    }

    @Test
    void earnedBirdBadgeIsRenderedAtTopRightOfRosterIcon() throws Exception {
        BirdGame3 game = new BirdGame3();
        setClassicCompleted(game, BirdGame3.BirdType.PIGEON, true);

        Method builder = BirdGame3.class.getDeclaredMethod("buildRosterSelectionIcon",
                BirdGame3.BirdType.class, boolean.class, double.class, boolean.class);
        builder.setAccessible(true);
        Node icon = (Node) builder.invoke(game, BirdGame3.BirdType.PIGEON, false, 88.0, false);

        Node badge = findNodeById(icon, "bird-select-badge");
        assertNotNull(badge);
        assertEquals("ROUTE BADGE EARNED", badge.getAccessibleText());
    }

    @Test
    void normalFighterSelectDoesNotInheritClassicRouteBadges() throws Exception {
        BirdGame3 game = new BirdGame3();
        setClassicCompleted(game, BirdGame3.BirdType.PIGEON, true);

        Method builder = BirdGame3.class.getDeclaredMethod("buildRosterSelectionIcon",
                BirdGame3.BirdType.class, boolean.class, double.class);
        builder.setAccessible(true);
        Node icon = (Node) builder.invoke(game, BirdGame3.BirdType.PIGEON, false, 88.0);

        assertNull(findNodeById(icon, "bird-select-badge"),
                "Normal fighter select must not display Classic route badges.");
    }

    @Test
    void bossRushStatusSeparatesSelectedBirdFromOverallBest() throws Exception {
        BirdGame3 game = new BirdGame3();
        setBossRushRecord(game, BirdGame3.BirdType.PIGEON, 610_000L, "A");
        setBossRushRecord(game, BirdGame3.BirdType.BAT, 505_000L, "S");
        refreshBossRushOverallRecord(game);

        assertEquals("A  |  10:10.00", game.bossRushBestStatusForBird(BirdGame3.BirdType.PIGEON));
        assertEquals("S  |  8:25.00  |  Bat", game.bossRushOverallBestStatus());
    }

    private static void setClassicCompleted(BirdGame3 game, BirdGame3.BirdType type, boolean completed) throws Exception {
        Field field = BirdGame3.class.getDeclaredField("classicCompleted");
        field.setAccessible(true);
        boolean[] classicCompleted = (boolean[]) field.get(game);
        classicCompleted[type.ordinal()] = completed;
    }

    private static void setBossRushRecord(BirdGame3 game, BirdGame3.BirdType type, long elapsedMillis, String rank) throws Exception {
        Field timeField = BirdGame3.class.getDeclaredField("bossRushBestClearMillisByBird");
        timeField.setAccessible(true);
        long[] times = (long[]) timeField.get(game);
        times[type.ordinal()] = elapsedMillis;

        Field rankField = BirdGame3.class.getDeclaredField("bossRushBestRankByBird");
        rankField.setAccessible(true);
        String[] ranks = (String[]) rankField.get(game);
        ranks[type.ordinal()] = rank;
    }

    private static void setBossRushPerfectBadge(BirdGame3 game, BirdGame3.BirdType type, boolean earned) throws Exception {
        Field field = BirdGame3.class.getDeclaredField("bossRushPerfectBadgeByBird");
        field.setAccessible(true);
        boolean[] badges = (boolean[]) field.get(game);
        badges[type.ordinal()] = earned;
    }

    private static void refreshBossRushOverallRecord(BirdGame3 game) throws Exception {
        Method method = BirdGame3.class.getDeclaredMethod("refreshBossRushOverallBestRecord");
        method.setAccessible(true);
        method.invoke(game);
    }

    private static Node findNodeById(Node node, String id) {
        if (id.equals(node.getId())) {
            return node;
        }
        if (node instanceof Parent parent) {
            for (Node child : parent.getChildrenUnmodifiable()) {
                Node match = findNodeById(child, id);
                if (match != null) {
                    return match;
                }
            }
        }
        return null;
    }
}
