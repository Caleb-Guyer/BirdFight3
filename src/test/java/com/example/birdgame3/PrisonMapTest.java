package com.example.birdgame3;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PrisonMapTest {
    @Test
    void crownlockIsASelectableFlatMainMapWithOnlySideAndRoofExits() throws Exception {
        BirdGame3 game = new BirdGame3();
        game.selectedMap = BirdGame3.MapType.PRISON;

        invoke(game, "setupMatchArenaGeometry");

        assertTrue((boolean) invoke(game, "isMapUnlocked", BirdGame3.MapType.PRISON),
                "Crownlock must be available from the regular map select without a campaign unlock.");
        assertEquals(1, game.platforms.size(), "Crownlock must have no raised or recovery platforms.");
        Platform mainFloor = game.platforms.getFirst();
        assertEquals(-420.0, mainFloor.x, 0.0001);
        assertEquals(BirdGame3.GROUND_Y - 150.0, mainFloor.y, 0.0001);
        assertEquals(BirdGame3.WORLD_WIDTH + 840.0, mainFloor.w, 0.0001);
        assertEquals(0.0, game.battlefieldLeftBound(), 0.0001);
        assertEquals(BirdGame3.WORLD_WIDTH, game.battlefieldRightBound(), 0.0001);
        assertTrue(mainFloor.x <= game.battlefieldLeftBound() - 300.0,
                "The solid floor must continue past the left blast line.");
        assertTrue(mainFloor.x + mainFloor.w >= game.battlefieldRightBound() + 300.0,
                "The solid floor must continue past the right blast line.");
        assertTrue(game.usesIslandBoundsForCurrentArena());
    }

    @Test
    void eachLeverOwnsAnIndependentCooldownAndReleasesFourPrisoners() throws Exception {
        BirdGame3 game = prisonGame();
        Bird puller = game.players[0];

        invoke(game, "releasePrisoners", 0, puller);

        int[] cooldowns = (int[]) field(game, "prisonLeverCooldowns");
        List<?> rushes = (List<?>) field(game, "prisonerRushes");
        assertEquals(720, cooldowns[0]);
        assertEquals(0, cooldowns[1], "Opening cell block A must not lock cell block B.");
        assertEquals(4, rushes.size());
    }

    @Test
    void releasedPrisonersRunAcrossTheFloorAndOnlyDamageTheOpposingTeam() throws Exception {
        BirdGame3 game = prisonGame();
        Bird puller = game.players[0];
        Bird ally = game.players[1];
        Bird enemy = game.players[2];

        puller.x = 900.0;
        ally.x = 1700.0;
        enemy.x = 2250.0;
        double allyStartingHealth = ally.health;
        double enemyStartingHealth = enemy.health;
        double standingY = BirdGame3.GROUND_Y - 150.0 - 80.0;
        puller.y = standingY;
        ally.y = standingY;
        enemy.y = standingY;

        invoke(game, "releasePrisoners", 0, puller);
        for (int tick = 0; tick < 180; tick++) {
            invoke(game, "updatePrisonStageHazards");
        }

        assertEquals(allyStartingHealth, ally.health, 0.0001,
                "A lever's prisoner wave must pass through the team that released it.");
        assertTrue(enemy.health < enemyStartingHealth,
                "The charging prisoners must damage opponents standing in their path.");
    }

    private static BirdGame3 prisonGame() {
        BirdGame3 game = new BirdGame3();
        game.selectedMap = BirdGame3.MapType.PRISON;
        game.campaignModeActive = true;
        game.campaignTeamMode = true;
        game.activePlayers = 3;
        game.players[0] = new Bird(900.0, BirdGame3.BirdType.PIGEON, 0, game);
        game.players[1] = new Bird(1700.0, BirdGame3.BirdType.MOCKINGBIRD, 1, game);
        game.players[2] = new Bird(2250.0, BirdGame3.BirdType.EAGLE, 2, game);
        game.campaignTeams[0] = 1;
        game.campaignTeams[1] = 1;
        game.campaignTeams[2] = 2;
        return game;
    }

    private static Object field(Object target, String name) throws Exception {
        Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        return field.get(target);
    }

    private static Object invoke(Object target, String name, Object... args) throws Exception {
        Method method = java.util.Arrays.stream(target.getClass().getDeclaredMethods())
                .filter(candidate -> candidate.getName().equals(name))
                .filter(candidate -> candidate.getParameterCount() == args.length)
                .findFirst()
                .orElseThrow();
        method.setAccessible(true);
        return method.invoke(target, args);
    }
}
