package com.example.birdgame3;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.Comparator;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

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

    @Test
    void worldseamRecoveryUsesTheLandmassNearestEachFighter() throws Exception {
        BirdGame3 game = new BirdGame3();
        game.harnessPrepareMatch(BirdGame3.BirdType.KIWI, BirdGame3.BirdType.PIGEON,
                0x5E_AA17L, BirdGame3.MapType.WORLDSEAM);
        List<Platform> landmasses = game.platforms.stream()
                .filter(platform -> platform.w >= 2_000.0 && platform.w <= 2_300.0)
                .sorted(Comparator.comparingDouble(platform -> platform.x))
                .toList();
        assertEquals(2, landmasses.size());

        Method resolver = Bird.class.getDeclaredMethod("findAIMainStagePlatform");
        resolver.setAccessible(true);
        Bird kiwi = game.players[0];
        Platform left = landmasses.get(0);
        Platform right = landmasses.get(1);

        kiwi.x = left.x + 160.0;
        assertSame(left, resolver.invoke(kiwi));

        kiwi.x = right.x + 160.0;
        assertSame(right, resolver.invoke(kiwi),
                "A grounded CPU on Worldseam's right half must not recover back through the central rift");
    }

    @Test
    void worldseamDoesNotForceKiwiToLoseEverySeededMatchup() {
        BirdGame3 game = new BirdGame3();
        int kiwiWins = 0;
        long seed = 0x5E_AA20L;
        for (BirdGame3.BirdType opponent : BirdGame3.BirdType.values()) {
            if (opponent == BirdGame3.BirdType.KIWI) continue;
            BalanceLab.MatchOutcome left = BalanceLab.playMatch(game,
                    BirdGame3.BirdType.KIWI, opponent, seed++, 4L * 60 * 60,
                    BirdGame3.MapType.WORLDSEAM);
            BalanceLab.MatchOutcome right = BalanceLab.playMatch(game,
                    opponent, BirdGame3.BirdType.KIWI, seed++, 4L * 60 * 60,
                    BirdGame3.MapType.WORLDSEAM);
            if (left.winner() == BirdGame3.BirdType.KIWI) kiwiWins++;
            if (right.winner() == BirdGame3.BirdType.KIWI) kiwiWins++;
        }
        assertTrue(kiwiWins > 0,
                "Worldseam navigation must not structurally force Kiwi to lose every matchup");
    }
}
