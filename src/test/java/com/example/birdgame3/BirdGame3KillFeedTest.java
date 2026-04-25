package com.example.birdgame3;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BirdGame3KillFeedTest {
    @Test
    void buildKillFeedRenderBlocksWrapsLongMessagesAndKeepsNewestFirst() {
        BirdGame3 game = new BirdGame3();

        game.addToKillFeed("OLDER EVENT");
        game.addToKillFeed("VOID CROWN: Umbra Bat and Night Raven reopen the route with a very long killfeed message.");

        List<BirdGame3.KillFeedRenderBlock> blocks = game.buildKillFeedRenderBlocks();

        assertEquals(2, blocks.size());
        assertTrue(blocks.get(0).lines().size() > 1);
        assertTrue(String.join(" ", blocks.get(0).lines()).startsWith("VOID CROWN:"));
        assertEquals("OLDER EVENT", String.join(" ", blocks.get(1).lines()));
    }
}
