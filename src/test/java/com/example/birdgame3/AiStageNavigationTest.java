package com.example.birdgame3;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;

class AiStageNavigationTest {

    @Test
    void parliamentRecoveryUsesAuthoredCentralIslandInsteadOfWiderSideDeck() throws Exception {
        BirdGame3 game = new BirdGame3();
        Method setup = BirdGame3.class.getDeclaredMethod("setupBossRushParliamentRooftops");
        setup.setAccessible(true);
        setup.invoke(game);

        Platform authored = game.authoredAiMainStagePlatform();
        assertNotNull(authored);
        assertEquals("PARLIAMENT", authored.signText);
        assertEquals(2_300.0, authored.x, 0.001);
        assertEquals(1_400.0, authored.w, 0.001);

        Bird cpu = new Bird(2_300.0, BirdGame3.BirdType.FALCON, 0, game);
        cpu.y = game.battlefieldSpawnY(1.0);
        Method resolver = Bird.class.getDeclaredMethod("findAIMainStagePlatform");
        resolver.setAccessible(true);

        assertSame(authored, resolver.invoke(cpu),
                "CPU recovery must honor the arena's authored main island even when a side deck is wider");
    }

    @Test
    void arenasWithoutAuthoredIslandKeepWidestPlatformFallback() throws Exception {
        BirdGame3 game = new BirdGame3();
        Platform narrow = new Platform(300, 900, 400, 40);
        Platform wide = new Platform(1_000, 800, 900, 40);
        game.platforms.add(narrow);
        game.platforms.add(wide);
        Bird cpu = new Bird(1_100, BirdGame3.BirdType.PIGEON, 0, game);
        cpu.y = 700;

        Method resolver = Bird.class.getDeclaredMethod("findAIMainStagePlatform");
        resolver.setAccessible(true);

        assertSame(wide, resolver.invoke(cpu));
    }
}
