package com.example.birdgame3;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.TreeMap;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.prefs.Preferences;

import static org.junit.jupiter.api.Assertions.*;

/** Exercises Studio through the real fixed-update loop without starting JavaFX. */
class ReplayStudioIntegrationTest {
    private static final long MATCH_SEED = 0x57_0D_10_2026L;
    private static final long RNG_PROBE_SEED = 0x51_A7_E_55L;
    private final Preferences root = Preferences.userRoot().node(
            "/birdfight3-tests/replay-studio-integration/" + UUID.randomUUID());
    private int sessionNumber;

    @AfterEach
    void removeProfiles() throws Exception {
        root.removeNode();
        root.flush();
    }

    @Test
    void pauseConsumesNeitherRecordedInputsSimulationTicksNorRandomness() throws Exception {
        BirdGame3 game = replayGame(120);
        ReplayStudioState studio = studio(game);
        studio.setPaused(true);
        set(game, "hitstopFrames", 4);
        long initialHash = game.harnessStateHash();
        int initialTimer = game.matchTimer;
        SimRng.reseed(RNG_PROBE_SEED);

        for (int pulse = 0; pulse < 10; pulse++) {
            tick(game, 250_000_000L);
        }

        assertEquals(0, cursor(game));
        assertEquals(0L, get(game, "simTick"));
        assertEquals(4, get(game, "hitstopFrames"));
        assertEquals(initialTimer, game.matchTimer);
        assertEquals(initialHash, game.harnessStateHash());
        assertFalse(game.isRightPressed(0), "Paused playback must not inject its first input.");
        assertEquals(new Random(RNG_PROBE_SEED).nextLong(), SimRng.random().nextLong());
    }

    @Test
    void oneStepLoadsExactlyOneInputAndRemainsPausedRegardlessOfElapsedTime() throws Exception {
        BirdGame3 game = replayGame(120);
        ReplayStudioState studio = studio(game);
        MatchReplay replay = (MatchReplay) get(game, "activeReplay");
        replay.frames.set(0, new int[]{LanProtocol.INPUT_RIGHT, 0});
        replay.frames.set(1, new int[]{LanProtocol.INPUT_LEFT, 0});

        studio.requestStep();
        tick(game, 250_000_000L);
        assertEquals(1, cursor(game));
        assertEquals(1L, get(game, "simTick"));
        assertTrue(game.isRightPressed(0));
        assertFalse(game.isLeftPressed(0));
        assertTrue(studio.paused());

        tick(game, 250_000_000L);
        assertEquals(1, cursor(game));
        assertEquals(1L, get(game, "simTick"));

        studio.requestStep();
        tick(game, 0L);
        assertEquals(2, cursor(game));
        assertEquals(2L, get(game, "simTick"));
        assertFalse(game.isRightPressed(0));
        assertTrue(game.isLeftPressed(0));
        assertTrue(studio.paused());
    }

    @Test
    void steppingDuringHitstopConsumesOneFreezeFrameWithoutInputOrRng() throws Exception {
        BirdGame3 game = replayGame(120);
        ReplayStudioState studio = studio(game);
        set(game, "hitstopFrames", 2);
        long initialHash = game.harnessStateHash();
        SimRng.reseed(RNG_PROBE_SEED);

        studio.requestStep();
        tick(game, 250_000_000L);
        assertEquals(1, get(game, "hitstopFrames"));
        assertEquals(0, cursor(game));
        assertEquals(0L, get(game, "simTick"));
        assertEquals(initialHash, game.harnessStateHash());
        assertEquals(new Random(RNG_PROBE_SEED).nextLong(), SimRng.random().nextLong());

        tick(game, 250_000_000L);
        assertEquals(1, get(game, "hitstopFrames"), "A held pause must not drain hitstop.");
        studio.requestStep();
        tick(game, 0L);
        assertEquals(0, get(game, "hitstopFrames"));
        assertEquals(0, cursor(game));
        assertEquals(0L, get(game, "simTick"));

        studio.requestStep();
        tick(game, 0L);
        assertEquals(1, cursor(game));
        assertEquals(1L, get(game, "simTick"));
    }

    @Test
    void allSpeedsAndMidReplaySpeedChangesReachTheSameSimulationAndRngState() throws Exception {
        PlaybackTrace normal = playToEnd(1.0, false);
        for (double speed : new double[]{0.25, 0.5, 2.0}) {
            assertEquals(normal, playToEnd(speed, false), "Replay diverged at " + speed + "x.");
        }
        assertEquals(normal, playToEnd(0.25, true), "Changing speed mid-replay altered simulation.");
    }

    @Test
    void forwardSeekStopsAtTheRequestedInputBoundaryAndRemainsPaused() throws Exception {
        BirdGame3 game = replayGame(120);
        ReplayStudioState studio = studio(game);
        set(game, "hitstopFrames", 3);
        studio.seek(17);

        tick(game, 0L);
        assertEquals(17, cursor(game));
        assertEquals(17L, get(game, "simTick"));
        assertEquals(0, get(game, "hitstopFrames"));
        assertFalse(studio.seeking());
        assertTrue(studio.paused());
        tick(game, 250_000_000L);
        assertEquals(17, cursor(game));
    }

    @Test
    void exhaustedInputStreamCannotAdvanceSimulationEvenWhenResumedOrStepped() throws Exception {
        BirdGame3 game = replayGame(3);
        ReplayStudioState studio = studio(game);
        tick(game, BirdGame3.FIXED_STEP_NS * 6);
        assertEquals(3, cursor(game));
        assertEquals(3L, get(game, "simTick"));
        assertTrue(studio.paused());
        long finalHash = game.harnessStateHash();
        set(game, "hitstopFrames", 3);
        SimRng.reseed(RNG_PROBE_SEED);

        studio.setPaused(false);
        tick(game, 250_000_000L);
        studio.requestStep();
        tick(game, 0L);

        assertEquals(3, cursor(game));
        assertEquals(3L, get(game, "simTick"));
        assertEquals(3, get(game, "hitstopFrames"));
        assertEquals(finalHash, game.harnessStateHash());
        assertTrue(studio.paused());
        assertEquals(new Random(RNG_PROBE_SEED).nextLong(), SimRng.random().nextLong());
    }

    @Test
    void replayMatchEndKeepsStudioOpenWithoutFxTimersOrProgression() throws Exception {
        BirdGame3 game = replayGame(120);
        // The harness branch precedes replay handling: explicitly disable it so
        // this test would fail if the replay branch tried to start an FX timer.
        game.headlessHarnessMode = false;
        game.competitionModeEnabled = true;
        MatchReplay replay = (MatchReplay) get(game, "activeReplay");
        Bird winner = game.players[0];
        int[] winsBefore = ((int[]) get(game, "typeWins")).clone();
        List<?> historyBefore = List.copyOf((List<?>) get(game, "matchHistory"));
        BirdGame3AchievementProfile achievements = (BirdGame3AchievementProfile) get(game, "achievementProfile");
        BirdGame3AchievementProfile achievementsBefore = achievements.copy();
        BirdCoinLedger coins = (BirdCoinLedger) get(game, "birdCoinLedger");
        int coinsBefore = coins.balance();
        Map<String, String> preferencesBefore = preferencesSnapshot(root);

        assertDoesNotThrow(() -> new MatchController(game).triggerMatchEnd(winner));

        assertTrue(game.matchEnded);
        assertTrue(game.replayPlaybackActive);
        assertSame(replay, get(game, "activeReplay"));
        assertSame(winner, game.matchEndFocusBird);
        assertNull(game.harnessWinner, "Must exercise the replay branch, not the harness shortcut.");
        assertNull(get(game, "timer"));
        assertArrayEquals(winsBefore, (int[]) get(game, "typeWins"));
        assertEquals(historyBefore, get(game, "matchHistory"));
        assertEquals(coinsBefore, coins.balance());
        for (BirdGame3Achievement achievement : BirdGame3Achievement.values()) {
            assertEquals(achievementsBefore.isUnlocked(achievement), achievements.isUnlocked(achievement));
            assertEquals(achievementsBefore.progress(achievement), achievements.progress(achievement));
            assertEquals(achievementsBefore.isRewardClaimed(achievement), achievements.isRewardClaimed(achievement));
        }
        assertEquals(preferencesBefore, preferencesSnapshot(root));
    }

    @Test
    void knockoutCaptureBookmarksEachStockLossOnceAndNeverRecordsPlayback() throws Exception {
        BirdGame3 game = replayGame(120);
        game.replayPlaybackActive = false;
        game.scores[0] = 3;
        game.scores[1] = 2;
        invoke(game, "beginReplayRecordingForMatch");
        MatchReplay recording = (MatchReplay) get(game, "replayRecording");
        for (int frame = 0; frame < 12; frame++) invoke(game, "captureReplayFrame");

        game.scores[0] = 2;
        invoke(game, "captureReplayKnockouts");
        assertEquals(List.of(new MatchReplay.Knockout(12, "P1 KO · 2 stocks left")),
                recording.knockouts);

        invoke(game, "captureReplayKnockouts");
        invoke(game, "captureReplayFrame");
        invoke(game, "captureReplayKnockouts");
        assertEquals(1, recording.knockouts.size(), "Later pulses must not duplicate the same stock loss.");

        game.scores[1] = 0;
        invoke(game, "captureReplayKnockouts");
        assertEquals(new MatchReplay.Knockout(13, "P2 KO · 0 stocks left"),
                recording.knockouts.get(1));
        List<MatchReplay.Knockout> recordedKnockouts = List.copyOf(recording.knockouts);

        game.replayPlaybackActive = true;
        game.scores[0] = 1;
        invoke(game, "captureReplayKnockouts");
        invoke(game, "captureReplayKnockouts");
        assertEquals(recordedKnockouts, recording.knockouts,
                "Playback must never append markers even if a recording reference remains.");
    }

    @Test
    void clearingPlaybackCancelsExportResetsInputsAndRestoresMenuWithoutProfileChanges(
            @TempDir Path exportDirectory) throws Exception {
        BirdGame3 game = replayGame(120);
        FightSetupSelectionState selection = (FightSetupSelectionState) get(game, "fightSetupSelection");
        FrontEndMatchFlow flow = (FrontEndMatchFlow) get(game, "frontEndMatchFlow");
        VersusRules menuRules = VersusRules.competitive().withStockCount(4).withTimeLimitSeconds(210);
        flow.selectCustomRules(menuRules);
        set(game, "selectedMap", BirdGame3.MapType.DOCK);
        set(game, "selectedMapVariant", BirdGame3.MapVariant.TITAN_DOCK);
        game.activePlayers = 3;
        game.teamModeEnabled = true;
        game.mutatorModeEnabled = true;
        game.isAI[0] = true;
        game.isAI[2] = true;
        int[] teams = (int[]) get(game, "playerTeams");
        teams[0] = 2;
        teams[1] = 1;
        teams[2] = 2;
        selection.selectBirdWithSkin(0, BirdGame3.BirdType.RAVEN, "voidHeraldRaven");
        selection.selectRandom(1);
        selection.selectBird(2, BirdGame3.BirdType.GOOSE);
        boolean[] menuAi = game.isAI.clone();
        int[] menuTeams = teams.clone();
        BirdGame3.BirdType[] menuBirds = selection.selectedBirds().clone();
        boolean[] menuRandoms = selection.randomSelections().clone();
        String[] menuSkins = selection.selectedSkinKeys().clone();
        set(game, "preReplayMenuState", invoke(game, "captureReplayMenuSnapshot"));

        flow.selectRulesPreset(VersusRulesPreset.STANDARD);
        set(game, "selectedMap", BirdGame3.MapType.BATTLEFIELD);
        set(game, "selectedMapVariant", BirdGame3.MapVariant.STANDARD);
        game.activePlayers = 2;
        game.teamModeEnabled = false;
        game.mutatorModeEnabled = false;
        Arrays.fill(game.isAI, false);
        Arrays.fill(teams, 0);
        for (int slot = 0; slot < selection.selectedBirds().length; slot++) {
            selection.selectBirdWithSkin(slot, BirdGame3.BirdType.PIGEON, null);
        }
        invoke(game, "loadReplayFrame");
        assertTrue(game.isRightPressed(0));
        ((boolean[]) get(game, "replayAttackUpHeld"))[0] = true;
        ((boolean[]) get(game, "replayAttackDownHeld"))[1] = true;
        studio(game).slower();
        studio(game).markIn(30);
        studio(game).seek(50);
        set(game, "replayExportFrameDue", true);

        BirdCoinLedger coins = (BirdCoinLedger) get(game, "birdCoinLedger");
        coins.grant(150);
        game.setAchievementUnlocked(BirdGame3Achievement.FIRST_BLOOD, true);
        int coinsBefore = coins.balance();
        int[] winsBefore = ((int[]) get(game, "typeWins")).clone();
        List<?> historyBefore = List.copyOf((List<?>) get(game, "matchHistory"));
        Map<String, String> preferencesBefore = preferencesSnapshot(root);
        ExecutorService encoder = Executors.newSingleThreadExecutor();
        Path destination = exportDirectory.resolve("cancelled.avi");
        ReplayClipExport export = new ReplayClipExport(destination, encoder);
        try {
            set(game, "replayClipExport", export);
            invoke(game, "clearReplayPlaybackState");

            assertFalse(game.replayPlaybackActive);
            assertNull(get(game, "activeReplay"));
            assertNull(get(game, "replayClipExport"));
            assertNull(get(game, "replayClipImage"));
            assertFalse((boolean) get(game, "replayExportFrameDue"));
            assertThrows(IllegalStateException.class, () -> export.submit(new int[0]),
                    "Clearing playback must close the encoder, not just drop its reference.");
            assertFalse(studio(game).paused());
            assertFalse(studio(game).seeking());
            assertFalse(studio(game).consumeStepRequest());
            assertEquals(1.0, studio(game).speed());
            assertEquals(0, studio(game).totalFrames());
            assertFalse(studio(game).validClip());
            for (boolean[] inputRow : (boolean[][]) get(game, "replayActionPressed")) {
                for (boolean held : inputRow) assertFalse(held);
            }
            for (boolean held : (boolean[]) get(game, "replayAttackUpHeld")) assertFalse(held);
            for (boolean held : (boolean[]) get(game, "replayAttackDownHeld")) assertFalse(held);

            assertEquals(BirdGame3.MapType.DOCK, get(game, "selectedMap"));
            assertEquals(BirdGame3.MapVariant.TITAN_DOCK, get(game, "selectedMapVariant"));
            assertEquals(3, game.activePlayers);
            assertTrue(game.teamModeEnabled);
            assertTrue(game.mutatorModeEnabled);
            assertEquals(VersusRulesPreset.CUSTOM, flow.rulesPreset());
            assertEquals(menuRules, flow.rules());
            assertArrayEquals(menuAi, game.isAI);
            assertArrayEquals(menuTeams, teams);
            assertArrayEquals(menuBirds, selection.selectedBirds());
            assertArrayEquals(menuRandoms, selection.randomSelections());
            assertArrayEquals(menuSkins, selection.selectedSkinKeys());
            assertNull(get(game, "preReplayMenuState"), "The menu snapshot must be consumed only once.");

            assertEquals(coinsBefore, coins.balance());
            assertArrayEquals(winsBefore, (int[]) get(game, "typeWins"));
            assertEquals(historyBefore, get(game, "matchHistory"));
            assertTrue(((BirdGame3AchievementProfile) get(game, "achievementProfile"))
                    .isUnlocked(BirdGame3Achievement.FIRST_BLOOD));
            assertEquals(preferencesBefore, preferencesSnapshot(root));
        } finally {
            export.close();
            assertTrue(encoder.awaitTermination(5, TimeUnit.SECONDS), "Cancelled encoder must shut down.");
        }
        assertFalse(Files.exists(destination), "Cancelling must never publish an unfinished video.");
        try (var remaining = Files.list(exportDirectory)) {
            assertEquals(0, remaining.count(), "Cancellation must remove temporary clip files.");
        }
    }

    private BirdGame3 replayGame(int frames) throws Exception {
        BirdGame3 game = new BirdGame3(root.node("session-" + sessionNumber++));
        game.harnessPrepareMatch(BirdGame3.BirdType.PIGEON, BirdGame3.BirdType.PIGEON, MATCH_SEED);
        Arrays.fill(game.isAI, false);
        MatchReplay replay = new MatchReplay(MATCH_SEED, 2);
        for (int frame = 0; frame < frames; frame++) {
            int direction = (frame / 30) % 2 == 0 ? LanProtocol.INPUT_RIGHT : LanProtocol.INPUT_LEFT;
            int jump = frame % 60 < 8 ? LanProtocol.INPUT_JUMP : 0;
            replay.frames.add(new int[]{direction | jump, 0});
        }
        set(game, "activeReplay", replay);
        set(game, "replayFrameCursor", 0);
        set(game, "replayDashCursor", 0);
        set(game, "accumulator", 0L);
        game.replayPlaybackActive = true;
        studio(game).reset(frames);
        return game;
    }

    private PlaybackTrace playToEnd(double speed, boolean changeSpeed) throws Exception {
        BirdGame3 game = replayGame(240);
        // Exercise real deterministic AI/RNG alongside the recorded human input.
        game.isAI[1] = true;
        set(game, "hitstopFrames", 5);
        ReplayStudioState studio = studio(game);
        setSpeed(studio, speed);
        int pulses = 0;
        while (cursor(game) < 240 && pulses++ < 4_000) {
            if (changeSpeed) {
                int frame = cursor(game);
                setSpeed(studio, frame < 60 ? 0.25 : frame < 120 ? 2.0 : frame < 180 ? 0.5 : 1.0);
            }
            tick(game, BirdGame3.FIXED_STEP_NS);
        }
        assertEquals(240, cursor(game), "Replay must consume its complete input stream.");
        assertEquals(240L, get(game, "simTick"));
        return new PlaybackTrace(game.harnessStateHash(), SimRng.random().nextLong(), game.matchTimer,
                (int) get(game, "hitstopFrames"), game.matchEnded);
    }

    private static void setSpeed(ReplayStudioState studio, double speed) {
        while (studio.speed() > speed) studio.slower();
        while (studio.speed() < speed) studio.faster();
    }

    private record PlaybackTrace(long stateHash, long nextRandom, int matchTimer,
                                 int hitstopFrames, boolean matchEnded) {
    }

    private static ReplayStudioState studio(BirdGame3 game) throws Exception {
        return (ReplayStudioState) get(game, "replayStudio");
    }

    private static int cursor(BirdGame3 game) throws Exception {
        return (int) get(game, "replayFrameCursor");
    }

    private static void tick(BirdGame3 game, long elapsed) throws Exception {
        Method method = BirdGame3.class.getDeclaredMethod("gameTick", long.class);
        method.setAccessible(true);
        try {
            method.invoke(game, elapsed);
        } catch (InvocationTargetException exception) {
            if (exception.getCause() instanceof Exception cause) throw cause;
            if (exception.getCause() instanceof Error cause) throw cause;
            throw exception;
        }
    }

    private static Object invoke(BirdGame3 game, String name) throws Exception {
        Method method = BirdGame3.class.getDeclaredMethod(name);
        method.setAccessible(true);
        try {
            return method.invoke(game);
        } catch (InvocationTargetException exception) {
            if (exception.getCause() instanceof Exception cause) throw cause;
            if (exception.getCause() instanceof Error cause) throw cause;
            throw exception;
        }
    }

    private static Object get(BirdGame3 game, String name) throws Exception {
        Field field = BirdGame3.class.getDeclaredField(name);
        field.setAccessible(true);
        return field.get(game);
    }

    private static void set(BirdGame3 game, String name, Object value) throws Exception {
        Field field = BirdGame3.class.getDeclaredField(name);
        field.setAccessible(true);
        field.set(game, value);
    }

    private static Map<String, String> preferencesSnapshot(Preferences node) throws Exception {
        Map<String, String> snapshot = new TreeMap<>();
        for (String key : node.keys()) {
            snapshot.put(node.absolutePath() + "/" + key, node.get(key, ""));
        }
        for (String child : node.childrenNames()) {
            snapshot.putAll(preferencesSnapshot(node.node(child)));
        }
        return snapshot;
    }
}
