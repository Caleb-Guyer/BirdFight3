package com.example.birdgame3;

import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ClassicModeEndToEndRegressionTest {
    private static final long ROUTE_SEED = 0x434C_4153_5349_43L;

    @Test
    void everyPlayableBirdOwnsACompleteVariedEightEncounterRouteAndEnding() {
        BirdGame3 game = new BirdGame3();
        Set<String> routeTitles = new HashSet<>();

        for (BirdGame3.BirdType bird : BirdGame3.BirdType.values()) {
            List<BirdGame3.ClassicEncounter> route = game.harnessClassicRoute(bird, ROUTE_SEED);
            assertEquals(8, route.size(), bird + " must have eight authored encounters");
            assertEquals(8, route.stream().map(encounter -> encounter.name).distinct().count(),
                    bird + " cannot repeat encounter names inside its route");
            assertTrue(route.getLast().bossFight, bird + " must end with a boss");
            assertTrue(route.getLast().style != BirdGame3.ClassicEncounterStyle.STANDARD,
                    bird + " needs an authored final-boss presentation");
            assertTrue(route.stream().map(encounter -> encounter.map).distinct().count() >= 3,
                    bird + " route should visit at least three stage settings");

            Set<String> structures = new HashSet<>();
            for (BirdGame3.ClassicEncounter encounter : route) {
                assertNotNull(encounter.map, bird + " encounter map");
                assertNotNull(encounter.variant, bird + " encounter variant");
                assertNotNull(encounter.mutator, bird + " encounter mutator");
                assertNotNull(encounter.style, bird + " encounter style");
                assertFalse(encounter.name.isBlank(), bird + " encounter name");
                assertFalse(encounter.announcer.isBlank(), bird + " encounter announcer");
                assertTrue(encounter.timerFrames >= 30 * 60,
                        bird + " / " + encounter.name + " needs a human-completable timer");
                if (encounter.variant != BirdGame3.MapVariant.STANDARD) {
                    assertEquals(encounter.map, encounter.variant.baseMap,
                            bird + " / " + encounter.name + " variant belongs to another map");
                }
                assertTrue(StagePreviewRenderer.capturedPreviewResourceExists(
                                new BirdGame3.StageChoice(encounter.map, encounter.variant)),
                        bird + " / " + encounter.name + " is missing its literal stage photo");

                if (encounter.allies.length > 0) structures.add("ALLY");
                if (encounter.enemies.length > 1) structures.add("MULTI");
                if (encounter.waves != null && encounter.waves.length > 1) structures.add("WAVES");
                if (ClassicBalanceLab.isObjectiveRound(encounter.style)) structures.add("OBJECTIVE");
                if (encounter.style == BirdGame3.ClassicEncounterStyle.GIANT) structures.add("GIANT");
                if (encounter.bossFight) structures.add("BOSS");
                if (encounter.mutator != BirdGame3.MatchMutator.NONE) structures.add("MUTATOR");
            }
            assertTrue(structures.size() >= 4,
                    bird + " route needs at least four distinct encounter structures, found " + structures);

            ClassicEndingContent.Ending ending = ClassicEndingContent.endingFor(bird);
            assertNotNull(ending, bird + " must have a gallery ending");
            assertEquals(bird, ending.bird());
            assertEquals(bird, ending.cinematic().narrator());
            assertEquals(ClassicEndingContent.Tableau.values().length, ending.cinematic().beats().size(),
                    bird + " ending must be a full moving-picture monologue");
            routeTitles.add(ending.routeTitle());
        }

        assertEquals(BirdGame3.BirdType.values().length, routeTitles.size(),
                "Every bird needs its own route identity");
    }

    @Test
    void allOneHundredSeventySixEncountersPrepareWithSafeDistinctSpawnsNavigationAndMusic() throws Exception {
        BirdGame3 game = new BirdGame3();
        Method navigationTarget = Bird.class.getDeclaredMethod("findAIMainStagePlatform");
        navigationTarget.setAccessible(true);
        Method gameplayMusic = BirdGame3.class.getDeclaredMethod("gameplayMusicFile");
        gameplayMusic.setAccessible(true);

        for (BirdGame3.BirdType bird : BirdGame3.BirdType.values()) {
            for (int round = 0; round < 8; round++) {
                BirdGame3.ClassicEncounter encounter = game.harnessPrepareClassicEncounter(
                        bird, round, BirdGame3.CLASSIC_STARTING_DIFFICULTY, 5,
                        ROUTE_SEED + bird.ordinal(), ROUTE_SEED + bird.ordinal() * 31L + round);
                String context = bird + " round " + (round + 1) + " " + encounter.name;
                assertTrue(game.activePlayers >= 1 && game.activePlayers <= game.players.length,
                        context + " active-player count");
                assertNotNull(game.players[0], context + " player spawn");
                assertTrue(Double.isFinite(game.players[0].x) && Double.isFinite(game.players[0].y),
                        context + " player spawn must be finite");
                boolean authoredAerialStart = encounter.style == BirdGame3.ClassicEncounterStyle.NECTAR_DASH;
                assertTrue(authoredAerialStart || isSupported(game, game.players[0]),
                        context + " player must begin on visible collision geometry unless the route opens in flight");

                Set<String> occupiedSpawns = new HashSet<>();
                for (int slot = 0; slot < game.activePlayers; slot++) {
                    Bird fighter = game.players[slot];
                    if (fighter == null) continue;
                    assertTrue(Double.isFinite(fighter.x) && Double.isFinite(fighter.y),
                            context + " slot " + slot + " spawn must be finite");
                    String spawn = Math.round(fighter.bodyCenterX()) + ":" + Math.round(fighter.bodyCenterY());
                    assertTrue(occupiedSpawns.add(spawn),
                            context + " stacks multiple fighters at " + spawn);
                    if (slot > 0 && game.isAI[slot] && !fighter.classicBonusTarget) {
                        assertTrue(game.hasImplicitGroundFloorForCurrentArena()
                                        || navigationTarget.invoke(fighter) != null,
                                context + " slot " + slot + " has no recoverable stage target");
                        if (game.classicTeams[slot] == 2 && (encounter.waves != null
                                || encounter.style == BirdGame3.ClassicEncounterStyle.MINIATURE_FLOCK)) {
                            assertFalse(fighter.hasUltimate(),
                                    context + " minions and wave fighters must not bring ultimates");
                        }
                    }
                }

                String music = (String) gameplayMusic.invoke(game);
                assertFalse(music.isBlank(), context + " music selection");
                try (InputStream stream = getClass().getResourceAsStream("/sounds/" + music)) {
                    assertNotNull(stream, context + " references missing music " + music);
                }
            }
        }
    }

    @Test
    void encounterResetReleasesEveryRouteOwnedObjectFromLongClassicSessions() {
        BirdGame3 game = new BirdGame3();
        int populatedEncounterKinds = 0;

        for (BirdGame3.BirdType bird : BirdGame3.BirdType.values()) {
            for (int round = 0; round < 8; round++) {
                game.harnessPrepareClassicEncounter(bird, round, 5.0, 5,
                        ROUTE_SEED + bird.ordinal(), ROUTE_SEED + round);
                if (game.classicTransientObjectCount() > 0) populatedEncounterKinds++;
                game.resetMatchStats();
                assertEquals(0, game.classicTransientObjectCount(),
                        bird + " round " + (round + 1) + " retained Classic objects after reset");
            }
        }
        assertTrue(populatedEncounterKinds >= 12,
                "The lifecycle audit must exercise the objective and boss object systems");
    }

    @Test
    void featherDevUnlocksAllPlayableContentWithoutFakingRouteBadges() throws Exception {
        BirdGame3 game = new BirdGame3();
        invoke(game, "unlockEverythingForDeveloperProfile");
        Method birdUnlocked = privateMethod("isBirdUnlocked", BirdGame3.BirdType.class);
        Method mapUnlocked = privateMethod("isMapUnlocked", BirdGame3.MapType.class);
        Method variantUnlocked = privateMethod("isMapVariantUnlocked", BirdGame3.MapVariant.class);

        for (BirdGame3.BirdType bird : BirdGame3.BirdType.values()) {
            assertTrue((boolean) birdUnlocked.invoke(game, bird), "FEATHERDEV bird " + bird);
            assertFalse(game.isClassicCompleted(bird),
                    "FEATHERDEV grants route access but badges must still be earned: " + bird);
            assertFalse(game.isClassicEndingUnlocked(bird),
                    "The ending gallery must continue to mirror earned badges: " + bird);
        }
        for (BirdGame3.MapType map : BirdGame3.MapType.values()) {
            assertTrue((boolean) mapUnlocked.invoke(game, map), "FEATHERDEV map " + map);
        }
        for (BirdGame3.MapVariant variant : BirdGame3.MapVariant.values()) {
            assertTrue((boolean) variantUnlocked.invoke(game, variant), "FEATHERDEV variant " + variant);
        }

        boolean[] routeSkins = (boolean[]) field(game, "classicSkinUnlocked");
        for (boolean unlocked : routeSkins) assertTrue(unlocked, "FEATHERDEV route reward skin access");
    }

    private static boolean isSupported(BirdGame3 game, Bird fighter) {
        double centerX = fighter.bodyCenterX();
        double bottomY = fighter.bodyBottomY();
        boolean platform = game.platforms.stream().anyMatch(surface ->
                centerX >= surface.x - 0.001
                        && centerX <= surface.x + surface.w + 0.001
                        && Math.abs(bottomY - surface.y) <= 0.001);
        return platform || game.hasImplicitGroundFloorForCurrentArena()
                && Math.abs(bottomY - BirdGame3.GROUND_Y) <= 0.001;
    }

    private static Method privateMethod(String name, Class<?> parameter) throws Exception {
        Method method = BirdGame3.class.getDeclaredMethod(name, parameter);
        method.setAccessible(true);
        return method;
    }

    private static void invoke(BirdGame3 game, String name) throws Exception {
        Method method = BirdGame3.class.getDeclaredMethod(name);
        method.setAccessible(true);
        method.invoke(game);
    }

    private static Object field(BirdGame3 game, String name) throws Exception {
        Field field = BirdGame3.class.getDeclaredField(name);
        field.setAccessible(true);
        return field.get(game);
    }
}
