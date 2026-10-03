package com.example.birdgame3;

import org.junit.jupiter.api.Test;
import java.util.Comparator;
import java.util.Random;
import static org.junit.jupiter.api.Assertions.*;

class FlockRunStateTest {
    static FlockRunState run(long seed) { return new FlockRunState(seed, BirdGame3.BirdType.PIGEON); }

    static void draft(FlockRunState run) {
        while (run.phase() == FlockRunState.Phase.REWARD) {
            assertTrue(run.choosePerk(run.offeredPerks().stream().min(Comparator.comparingInt(run::rank)).orElseThrow()));
        }
    }

    static FlockRunState summit(long seed, FlockRunState.Route route) {
        FlockRunState run = run(seed);
        for (int i = 0; i < 7; i++) {
            assertTrue(run.chooseRoute(route));
            assertTrue(run.finishBattle(true, 90, 60 * 45));
            draft(run);
        }
        return run;
    }

    @Test void routeCostsRecoveryAndHealthCarryAreExplicit() {
        FlockRunState run = run(4);
        assertFalse(run.chooseRoute(FlockRunState.Route.REST));
        assertFalse(run.chooseRoute(FlockRunState.Route.BOSS));
        assertTrue(run.chooseRoute(FlockRunState.Route.SAFE));
        assertFalse(run.chooseRoute(FlockRunState.Route.ELITE));
        run.finishBattle(true, 30, 90 * 60);
        assertEquals(36, run.health());
        assertEquals(1, run.picksRemaining());
        assertEquals(100, run.score());
        draft(run);
        assertTrue(run.chooseRoute(FlockRunState.Route.REST));
        assertEquals(81, run.health());
        assertEquals(2, run.encounter());
        assertEquals(FlockRunState.Phase.ROUTE, run.phase());
        assertTrue(run.chooseRoute(FlockRunState.Route.REST));
        assertEquals(112, run.health());
        assertFalse(run.routes().contains(FlockRunState.Route.REST));
        assertEquals(100, run.score());
    }

    @Test void eliteGivesTwoPicksAndMedicineHealsOnlyOnVictory() {
        FlockRunState run = run(41);
        run.chooseRoute(FlockRunState.Route.ELITE);
        run.finishBattle(true, 70, 30 * 60);
        assertEquals(2, run.picksRemaining());
        assertEquals(280, run.score());
        assertEquals(76, run.health());
        draft(run);
        run.chooseRoute(FlockRunState.Route.SAFE);
        run.finishBattle(true, 50, 60 * 60);
        assertEquals(56 + 8 * run.rank(FlockRunState.Perk.MEDIC), run.health());
    }

    @Test void draftsAndEncountersResumeExactlyWithoutUsingSimulationRandomness() {
        FlockRunState run = run(15);
        roundTrip(run);
        SimRng.reseed(777);
        for (int i = 0; i < 7; i++) {
            long previewSeed = run.encounterSeed(FlockRunState.Route.ELITE);
            run.chooseRoute(FlockRunState.Route.ELITE);
            assertEquals(previewSeed, run.encounterSeed());
            roundTrip(run);
            run.finishBattle(true, 80, 1800);
            while (run.phase() == FlockRunState.Phase.REWARD) {
                roundTrip(run);
                assertTrue(run.offeredPerks().stream().allMatch(p -> run.rank(p) < 3));
                run.choosePerk(run.offeredPerks().getFirst());
            }
        }
        assertEquals(java.util.List.of(FlockRunState.Route.BOSS), run.routes());
        run.chooseRoute(FlockRunState.Route.BOSS);
        roundTrip(run);
        run.finishBattle(true, 55, 50 * 60);
        assertEquals(FlockRunState.Medal.GOLD, run.medal());
        roundTrip(run);
        assertEquals(new Random(777).nextLong(), SimRng.random().nextLong());
        assertFalse(run.finishBattle(true, 112, 0), "A result cannot be claimed twice");
    }

    @Test void medalRewardsRiskAndDefeatCannotAwardOne() {
        FlockRunState bronze = summit(2, FlockRunState.Route.SAFE);
        bronze.chooseRoute(FlockRunState.Route.BOSS);
        bronze.finishBattle(true, 1, 120 * 60);
        assertEquals(FlockRunState.Medal.SILVER, bronze.medal()); // Seven 15-point speed bonuses.
        FlockRunState loss = run(2);
        loss.chooseRoute(FlockRunState.Route.SAFE);
        loss.finishBattle(false, 45, 120 * 60);
        assertEquals(FlockRunState.Phase.LOST, loss.phase());
        assertEquals(FlockRunState.Medal.NONE, loss.medal());
        assertEquals(45, loss.health(), "A timeout must not heal the player");
        roundTrip(loss);
        FlockRunState abandoned = run(1);
        abandoned.abandon();
        roundTrip(abandoned);
    }

    @Test void corruptCheckpointsAreDiscardedAndNumericInputsAreBounded() {
        String fresh = run(1).encode();
        assertNull(FlockRunState.decode("nonsense"));
        assertNull(FlockRunState.decode(fresh.replace("112.0", "NaN")));
        assertNull(FlockRunState.decode(fresh.replace("0,0,0,0,0,0", "4,0,0,0,0,0")));
        assertNull(FlockRunState.decode(fresh.replace("ROUTE", "WON")));
        FlockRunState run = run(1);
        run.chooseRoute(FlockRunState.Route.SAFE);
        assertNull(FlockRunState.decode(run.encode().replace("SAFE", "BOSS")));
        run.finishBattle(true, 900, -99);
        assertEquals(112, run.health());
        assertEquals(160, run.score());
        assertEquals(0, run.ticks());
    }

    @Test void chosenSkinSurvivesCheckpointsAndOldRunsStillLoad() {
        FlockRunState styled = new FlockRunState(14, BirdGame3.BirdType.EAGLE, "SKY_KING_EAGLE");
        styled.chooseRoute(FlockRunState.Route.SAFE);
        assertEquals("SKY_KING_EAGLE", FlockRunState.decode(styled.encode()).skinKey);
        String old = "1" + styled.encode().substring(1, styled.encode().lastIndexOf(';'));
        FlockRunState restored = FlockRunState.decode(old);
        assertNotNull(restored);
        assertNull(restored.skinKey);
        assertEquals(styled.encounterSeed(), restored.encounterSeed());
        assertEquals(styled.phase(), restored.phase());
    }

    private static void roundTrip(FlockRunState run) {
        FlockRunState copy = FlockRunState.decode(run.encode());
        assertNotNull(copy, run.encode());
        assertEquals(run.encode(), copy.encode());
        assertEquals(run.offeredPerks(), copy.offeredPerks());
        assertEquals(run.routes(), copy.routes());
        assertEquals(run.boss(), copy.boss());
        assertEquals(run.encounterSeed(), copy.encounterSeed());
    }
}
