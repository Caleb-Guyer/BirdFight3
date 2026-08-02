package com.example.birdgame3;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import static com.example.birdgame3.BirdGame3.BirdType.*;
import static com.example.birdgame3.BirdGame3.MapType.*;
import static com.example.birdgame3.StoryCampaign.ActorMotion.*;
import static com.example.birdgame3.StoryCampaign.ArenaVariant.*;
import static com.example.birdgame3.StoryCampaign.ObjectiveType.*;
import static com.example.birdgame3.StoryCampaign.ShotStyle.*;

/** Authored data for Story Mode: The Still Sky. */
final class StoryCampaignContent {
    private static final String NULL_ROCK_SKIN = "NULL_ROCK_VULTURE";
    private static final String TIDE_VULTURE_SKIN = "TIDE_VULTURE";
    private static final String CAMPAIGN_PHASE_DIALOGUE_ID = "dynamic_campaign_phase";
    private static final String NULL_ROCK_DUEL_DIALOGUE_ID = "dynamic_null_rock_duel";
    private static final Map<String, String> DIALOGUE_SCRIPTS = StoryDialogueScripts.loadBundled();

    private StoryCampaignContent() {
    }

    static StoryCampaign create() {
        List<StoryCampaign.Cutscene> scenes = new ArrayList<>();
        addActOneScenes(scenes);
        addActTwoScenes(scenes);
        addActThreeScenes(scenes);
        addActFourScenes(scenes);
        addActFiveScenes(scenes);
        addActSixScenes(scenes);
        addActSevenScenes(scenes);
        addActEightScenes(scenes);
        addActNineScenes(scenes);
        addActTenScenes(scenes);
        addActElevenScenes(scenes);
        addActTwelveScenes(scenes);
        scenes.add(scene("s81_beyond_the_map", "Beyond the Map", SKYCLIFFS,
                List.of(PIGEON, RAVEN, TITMOUSE), false, true));
        validateDialogueCoverage(scenes);

        List<StoryCampaign.Act> acts = List.of(
                act("act_01", "Act I: When the Wind Stops",
                        "Three regions lose their wind at once, and separate flocks discover the same black Crown mark.",
                        deadAir(), harborLock(), lastThermal()),
                act("act_02", "Act II: Broken Harbor",
                        "Pelican and Goose fight through a stormglass occupation while Razorbill decides what a contract is worth.",
                        breakwaterRun(), pierNine(), stormglass()),
                act("act_03", "Act III: The Long Way Up",
                        "A desert courier collides with Eagle's disciplined sky guard, and Eagle earns the trust he will later abuse.",
                        paintedRoad(), ordersFromAbove(), openSky()),
                act("act_04", "Act IV: Many Small Fires",
                        "Four local defenders stop waiting for a larger hero and build the first real coalition.",
                        needleRoute(), smallMarks(), morningLine(), greenConvergence()),
                act("act_05", "Act V: Below the Map",
                        "Bat hears a second heartbeat beneath the Crown while Shoebill learns that refusing to choose is still a choice.",
                        listenBelow(), stoneJudgment(), secondPulse()),
                act("act_06", "Act VI: A Crown of Good Intentions",
                        "Penguin and Phoenix trace the failing stabilizers to the archive where Eagle has recorded every fight.",
                        whiteoutEquation(), borrowedFire(), crownArchive()),
                act("act_07", "Act VII: The Price of Order",
                        "Eagle closes the city to save the Crown. Falcon defects, Charles loses his home, and Old Sparrow pays for the lockdown.",
                        lastCall(), cutTheLock(), sparrowsCorridor(), marshalLaw()),
                act("act_08", "Act VIII: The Carrion Bargain",
                        "The coalition reaches Vulture and finds a saboteur, a criminal, and a scapegoat in the same bird.",
                        crowCountry(), termsInDark(), carrionAudience()),
                act("act_09", "Act IX: No Clean Side",
                        "The Carrion Pact breaks apart. Its members join the coalition without pretending their old choices were harmless.",
                        blueWater(), futureThatMoves(), stolenWinter(), masterKey()),
                act("act_10", "Act X: The Last Command",
                        "The Crown attacks its own guards, the world begins to freeze, and Eagle finally stands against his system.",
                        perfectWeather(), worldGoesStill(), blackoutKey()),
                act("act_11", "Act XI: All Wings In",
                        "Mixed squads tear down the regional anchors and race to free Vulture before the Crown finishes copying him.",
                        fireAndIce(), harborEngine(), echoChain(), freeTheFlock()),
                act("act_12", "Act XII: The Hollow Crown",
                        "Every surviving faction reaches the Crown for the two battles that decide whether the sky will ever move again.",
                        lastApproach(), nullRoc(), nullRock())
        );

        StoryCampaign campaign = new StoryCampaign(
                StoryCampaign.CURRENT_VERSION,
                "the_still_sky",
                "THE STILL SKY",
                "No ruler owns the wind.",
                acts,
                scenes
        );
        campaign.validate().throwIfInvalid();
        return campaign;
    }

    private static void validateDialogueCoverage(List<StoryCampaign.Cutscene> scenes) {
        Set<String> sceneIds = new LinkedHashSet<>();
        for (StoryCampaign.Cutscene scene : scenes) {
            sceneIds.add(scene.id());
        }
        sceneIds.add(CAMPAIGN_PHASE_DIALOGUE_ID);
        sceneIds.add(NULL_ROCK_DUEL_DIALOGUE_ID);
        Set<String> missing = new LinkedHashSet<>(sceneIds);
        missing.removeAll(DIALOGUE_SCRIPTS.keySet());
        Set<String> unused = new LinkedHashSet<>(DIALOGUE_SCRIPTS.keySet());
        unused.removeAll(sceneIds);
        if (!missing.isEmpty() || !unused.isEmpty()) {
            throw new IllegalArgumentException(
                    "Still Sky dialogue sections do not match campaign scenes; missing="
                            + missing + ", unused=" + unused);
        }
    }

    private static StoryCampaign.Act act(String id, String title, String summary,
                                         StoryCampaign.Mission... missions) {
        return new StoryCampaign.Act(id, title, summary, List.of(missions));
    }

    private static StoryCampaign.Mission mission(
            String id,
            String title,
            String briefing,
            BirdGame3.MapType map,
            StoryCampaign.ArenaVariant variant,
            StoryCampaign.PlayablePolicy playable,
            List<StoryCampaign.Fighter> allies,
            List<StoryCampaign.Fighter> enemies,
            List<StoryCampaign.MissionPhase> phases,
            String pre,
            String post,
            BirdGame3.BirdType recruit,
            boolean finalBoss
    ) {
        return mission(id, title, briefing, map, BirdGame3.MapVariant.STANDARD, variant, playable,
                allies, enemies, phases, pre, post, recruit, finalBoss);
    }

    private static StoryCampaign.Mission mission(
            String id,
            String title,
            String briefing,
            BirdGame3.MapType map,
            BirdGame3.MapVariant mapVariant,
            StoryCampaign.ArenaVariant variant,
            StoryCampaign.PlayablePolicy playable,
            List<StoryCampaign.Fighter> allies,
            List<StoryCampaign.Fighter> enemies,
            List<StoryCampaign.MissionPhase> phases,
            String pre,
            String post,
            BirdGame3.BirdType recruit,
            boolean finalBoss
    ) {
        return new StoryCampaign.Mission(id, title, briefing, map, mapVariant, variant, playable,
                allies, enemies, phases, pre, post, recruit, finalBoss);
    }

    private static StoryCampaign.MissionPhase phase(StoryCampaign.ObjectiveType type, String label,
                                                    int seconds, int count, boolean checkpoint) {
        return StoryCampaign.MissionPhase.timed(type, label, seconds, count, checkpoint);
    }

    private static StoryCampaign.MissionPhase eliminate(String label) {
        return StoryCampaign.MissionPhase.elimination(label);
    }

    private static StoryCampaign.Fighter ally(BirdGame3.BirdType type, String name) {
        return StoryCampaign.Fighter.ally(type, name);
    }

    private static StoryCampaign.Fighter enemy(BirdGame3.BirdType type, String name) {
        String skinKey = campaignTroopSkinKey(name);
        return skinKey == null
                ? StoryCampaign.Fighter.enemy(type, name)
                : StoryCampaign.Fighter.enemy(type, name, skinKey);
    }

    static boolean isNamedCampaignCharacter(String name) {
        if (name == null) return false;
        return switch (name.strip().toLowerCase(Locale.ROOT)) {
            case "pigeon", "charles", "pelican", "goose", "phoenix", "penguin",
                    "roadrunner", "hummingbird", "titmouse", "turkey", "rooster",
                    "bat", "shoebill", "razorbill", "falcon", "eagle", "vulture",
                    "raven", "heisenbird", "opium bird", "grinch-hawk", "old sparrow" -> true;
            default -> false;
        };
    }

    private static String campaignTroopSkinKey(String name) {
        if (isNamedCampaignCharacter(name)) return null;
        String normalized = name == null ? "" : name.strip().toLowerCase(Locale.ROOT);
        if (normalized.contains("null")
                || normalized.contains("echo")
                || normalized.contains("automaton")
                || normalized.contains("copied")
                || normalized.contains("remnant")
                || normalized.equals("north warden")
                || normalized.equals("west warden")) {
            return BirdGame3.CAMPAIGN_NULL_ECHO_SKIN;
        }
        if (normalized.contains("crown")
                || normalized.contains("interceptor")
                || normalized.equals("thermal collector")) {
            return BirdGame3.CAMPAIGN_CROWN_TROOP_SKIN;
        }
        if (normalized.contains("stormglass")
                || normalized.contains("harbor")
                || normalized.startsWith("blue ")
                || normalized.contains("freight")
                || normalized.contains("contractor")) {
            return BirdGame3.CAMPAIGN_HARBOR_CREW_SKIN;
        }
        return BirdGame3.CAMPAIGN_CARRION_PACT_SKIN;
    }

    private static StoryCampaign.Fighter boss(BirdGame3.BirdType type, String name,
                                               double health, double power, double speed) {
        return StoryCampaign.Fighter.boss(type, name, health, power, speed, null);
    }

    private static StoryCampaign.Fighter boss(BirdGame3.BirdType type, String name,
                                               double health, double power, double speed,
                                               String skinKey) {
        return StoryCampaign.Fighter.boss(type, name, health, power, speed, skinKey);
    }

    private static List<StoryCampaign.Fighter> fighters(StoryCampaign.Fighter... fighters) {
        return List.of(fighters);
    }

    private static List<StoryCampaign.MissionPhase> phases(StoryCampaign.MissionPhase... phases) {
        return List.of(phases);
    }

    // === ACT I ===

    private static StoryCampaign.Mission deadAir() {
        return mission("dead_air", "Dead Air",
                "Wake three rooftop vents, keep Charles moving, and reach the east evacuation light.",
                CITY, STILLNESS, StoryCampaign.PlayablePolicy.forced(PIGEON),
                fighters(ally(MOCKINGBIRD, "Charles")),
                fighters(enemy(RAVEN, "Crown Scout"), enemy(FALCON, "Crown Interceptor")),
                phases(
                        phase(CAPTURE, "Restart the rooftop vents", 42, 3, true),
                        phase(REACH_EXIT, "Reach Old Sparrow's evacuation light", 38, 1, true)
                ),
                "s01_dead_air", "s02_rooftop_after", null, false);
    }

    private static StoryCampaign.Mission harborLock() {
        return mission("harbor_lock", "Harbor Lock",
                "Hold the upper-dock lever while rescue skiffs clear the flooded channel.",
                DOCK, STILLNESS, StoryCampaign.PlayablePolicy.choice(PELICAN, GOOSE),
                List.of(),
                fighters(enemy(HEISENBIRD, "Stormglass Foreman"), enemy(RAZORBILL, "Harbor Contractor")),
                phases(
                        phase(HOLD_ZONE, "Hold the dock lever", 48, 1, true),
                        phase(SURVIVE, "Cover the final rescue skiff", 28, 1, true)
                ),
                "s03_harbor_alarm", "s04_harbor_after", null, false);
    }

    private static StoryCampaign.Mission lastThermal() {
        return mission("last_thermal", "The Last Thermal",
                "Protect Penguin while the last cathedral thermal is separated from the Crown line.",
                ASHFALL_CATHEDRAL, STILLNESS, StoryCampaign.PlayablePolicy.forced(PHOENIX),
                fighters(ally(PENGUIN, "Penguin")),
                fighters(enemy(OPIUMBIRD, "Haze Surveyor"), enemy(VULTURE, "Carrion Collector")),
                phases(
                        phase(PROTECT, "Keep Penguin on the stabilizer", 52, 1, true),
                        eliminate("Drive the collectors out")
                ),
                "s05_ashfall_warning", "s06_three_signals", PENGUIN, false);
    }

    // === ACT II ===

    private static StoryCampaign.Mission breakwaterRun() {
        return mission("breakwater_run", "Breakwater Run",
                "Carry the stormglass breaker across the lower pier and charge both harbor relays.",
                DOCK, CROWN_OCCUPIED, StoryCampaign.PlayablePolicy.forced(PELICAN),
                fighters(ally(GOOSE, "Goose")),
                fighters(enemy(RAZORBILL, "Razorbill"), enemy(HEISENBIRD, "Blue Guard")),
                phases(
                        phase(CAPTURE, "Charge the two harbor relays", 46, 2, true),
                        phase(REACH_EXIT, "Deliver the breaker to the sea gate", 36, 1, true)
                ),
                "s07_broken_harbor", "s08_breakwater_after", null, false);
    }

    private static StoryCampaign.Mission pierNine() {
        return mission("pier_nine", "Pier Nine",
                "Keep the evacuation nest standing while the harbor boarding force closes in.",
                DOCK, EVACUATION, StoryCampaign.PlayablePolicy.forced(GOOSE),
                fighters(ally(PELICAN, "Pelican")),
                fighters(enemy(HEISENBIRD, "Blue Guard"), enemy(VULTURE, "Freight Warden")),
                phases(
                        phase(PROTECT, "Defend the evacuation nest", 35, 1, true),
                        phase(GAUNTLET, "Break the boarding force", 0, 1, true),
                        phase(SURVIVE, "Hold until the skiffs clear", 32, 1, true)
                ),
                "s09_pier_nine", "s10_razor_doubt", null, false);
    }

    private static StoryCampaign.Mission stormglass() {
        return mission("stormglass", "Stormglass",
                "Destroy Heisenbird's conductor team before it hard-locks the harbor weather.",
                DOCK, CROWN_OCCUPIED, StoryCampaign.PlayablePolicy.choice(PELICAN, GOOSE),
                List.of(),
                fighters(
                        boss(HEISENBIRD, "Heisenbird", 270, 1.36, 1.13),
                        enemy(RAZORBILL, "Razorbill")
                ),
                phases(
                        phase(CAPTURE, "Overload the conductor clamps", 38, 2, true),
                        phase(BOSS_PHASES, "Defeat Heisenbird", 0, 2, true)
                ),
                "s11_stormglass_before", "s12_razorbill_joins", RAZORBILL, false);
    }

    // === ACT III ===

    private static StoryCampaign.Mission paintedRoad() {
        return mission("painted_road", "Painted Road",
                "Outrun the Crown pursuit, trip both desert checkpoints, and reach the canyon exit.",
                DESERT, CROWN_OCCUPIED, StoryCampaign.PlayablePolicy.forced(ROADRUNNER),
                List.of(),
                fighters(enemy(FALCON, "Falcon"), enemy(RAVEN, "Crown Tracker")),
                phases(
                        phase(CAPTURE, "Trip the desert checkpoints", 35, 2, true),
                        phase(REACH_EXIT, "Reach the canyon exit", 30, 1, true)
                ),
                "s13_painted_road", "s14_falcon_report", null, false);
    }

    private static StoryCampaign.Mission ordersFromAbove() {
        return mission("orders_from_above", "Orders From Above",
                "Catch Roadrunner before the courier crosses Eagle's upper checkpoint.",
                SKYCLIFFS, CROWN_OCCUPIED, StoryCampaign.PlayablePolicy.forced(FALCON),
                List.of(),
                fighters(boss(ROADRUNNER, "Roadrunner", 185, 1.14, 1.28)),
                phases(
                        phase(REACH_EXIT, "Cut off the courier route", 36, 1, true),
                        eliminate("Win the pursuit duel")
                ),
                "s15_orders_above", "s16_falcon_choice", null, false);
    }

    private static StoryCampaign.Mission openSky() {
        return mission("open_sky", "Open Sky",
                "Hold the cliff village through the carrion assault until Eagle's relief wing arrives.",
                SKYCLIFFS, EVACUATION, StoryCampaign.PlayablePolicy.choice(ROADRUNNER, RAZORBILL),
                fighters(ally(FALCON, "Falcon")),
                fighters(enemy(VULTURE, "Carrion Captain"), enemy(RAVEN, "Blackwing Scout")),
                phases(
                        phase(PROTECT, "Protect the cliff village", 45, 1, true),
                        phase(GAUNTLET, "Clear the carrion force", 0, 1, true)
                ),
                "s17_open_sky_before", "s18_eagle_saves", ROADRUNNER, false);
    }

    // === ACT IV ===

    private static StoryCampaign.Mission needleRoute() {
        return mission("needle_route", "Needle Route",
                "Carry the warning through the canopy and light all four nectar relays.",
                VIBRANT_JUNGLE, STILLNESS, StoryCampaign.PlayablePolicy.forced(HUMMINGBIRD),
                List.of(),
                fighters(enemy(GRINCHHAWK, "Green Thief"), enemy(RAVEN, "Pact Runner")),
                phases(
                        phase(CAPTURE, "Light the nectar relays", 44, 4, true),
                        phase(REACH_EXIT, "Reach the forest warning bell", 30, 1, true)
                ),
                "s19_needle_route", "s20_humming_after", HUMMINGBIRD, false);
    }

    private static StoryCampaign.Mission smallMarks() {
        return mission("small_marks", "Small Marks",
                "Mark the three hidden Crown taps and defend the forest nursery while they burn out.",
                FOREST, CROWN_OCCUPIED, StoryCampaign.PlayablePolicy.forced(TITMOUSE),
                List.of(),
                fighters(enemy(GRINCHHAWK, "Present Runner"), enemy(OPIUMBIRD, "Haze Guard")),
                phases(
                        phase(CAPTURE, "Mark the hidden Crown taps", 40, 3, true),
                        phase(PROTECT, "Defend the nursery", 38, 1, true)
                ),
                "s21_small_marks", "s22_titmouse_after", TITMOUSE, false);
    }

    private static StoryCampaign.Mission morningLine() {
        return mission("morning_line", "Morning Line",
                "Keep the grain road open until every ground flock reaches the forest shelter.",
                FOREST, EVACUATION, StoryCampaign.PlayablePolicy.choice(TURKEY, ROOSTER),
                List.of(),
                fighters(enemy(GRINCHHAWK, "Grinch-Hawk"), enemy(VULTURE, "Carrion Driver")),
                phases(
                        phase(HOLD_ZONE, "Keep the grain road open", 46, 1, true),
                        phase(SURVIVE, "Cover the final ground flock", 30, 1, true)
                ),
                "s23_morning_line", "s24_rooster_turkey", null, false);
    }

    private static StoryCampaign.Mission greenConvergence() {
        return mission("green_convergence", "Green Convergence",
                "Combine four local routes, break Grinch-Hawk's convoy, and reopen the jungle canopy.",
                VIBRANT_JUNGLE, BirdGame3.MapVariant.CARRION_THRONE, CARRION,
                StoryCampaign.PlayablePolicy.choice(HUMMINGBIRD, TITMOUSE, TURKEY, ROOSTER),
                List.of(),
                fighters(
                        boss(GRINCHHAWK, "Grinch-Hawk", 255, 1.32, 1.13),
                        enemy(VULTURE, "Carrion Driver")
                ),
                phases(
                        phase(CAPTURE, "Join the four route signals", 42, 4, true),
                        phase(BOSS_PHASES, "Stop Grinch-Hawk's convoy", 0, 2, true)
                ),
                "s25_green_convergence", "s26_first_coalition", ROOSTER, false);
    }

    // === ACT V ===

    private static StoryCampaign.Mission listenBelow() {
        return mission("listen_below", "Listen Below",
                "Follow the second pulse, tune three echo points, and escape the closing cavern.",
                CAVE, STILLNESS, StoryCampaign.PlayablePolicy.forced(BAT),
                List.of(),
                fighters(enemy(OPIUMBIRD, "Haze Listener"), enemy(RAVEN, "Cave Shadow")),
                phases(
                        phase(CAPTURE, "Tune the echo points", 46, 3, true),
                        phase(REACH_EXIT, "Escape the closing cavern", 34, 1, true)
                ),
                "s27_listen_below", "s28_bat_warning", null, false);
    }

    private static StoryCampaign.Mission stoneJudgment() {
        return mission("stone_judgment", "Stone Judgment",
                "Stop Bat at the neutral marsh gate and decide whether the warning is evidence or panic.",
                CAVE, STANDARD, StoryCampaign.PlayablePolicy.forced(SHOEBILL),
                List.of(),
                fighters(boss(BAT, "Bat", 180, 1.10, 1.18)),
                phases(eliminate("Complete Shoebill's judgment duel")),
                "s29_stone_judgment", "s30_shoebill_decides", SHOEBILL, false);
    }

    private static StoryCampaign.Mission secondPulse() {
        return mission("second_pulse", "The Second Pulse",
                "Hold the lower chamber while Bat maps the object beneath the Crown.",
                CAVE, CARRION, StoryCampaign.PlayablePolicy.choice(BAT, SHOEBILL, MOCKINGBIRD),
                List.of(),
                fighters(
                        boss(OPIUMBIRD, "Opium Bird", 235, 1.24, 1.16),
                        enemy(RAVEN, "Raven")
                ),
                phases(
                        phase(PROTECT, "Protect Bat's echo map", 44, 1, true),
                        phase(BOSS_PHASES, "Break the haze lock", 0, 2, true)
                ),
                "s31_second_pulse_before", "s32_thing_below", BAT, false);
    }

    // === ACT VI ===

    private static StoryCampaign.Mission whiteoutEquation() {
        return mission("whiteout_equation", "Whiteout Equation",
                "Repair the fjord stabilizer while admitting exactly what it was built to do.",
                FROSTBITE_FJORD, STILLNESS, StoryCampaign.PlayablePolicy.forced(PENGUIN),
                fighters(ally(PHOENIX, "Phoenix")),
                fighters(enemy(FALCON, "Crown Recovery Wing"), enemy(HEISENBIRD, "Blue Technician")),
                phases(
                        phase(PROTECT, "Repair the fjord stabilizer", 48, 1, true),
                        phase(CAPTURE, "Vent the stored Crown charge", 36, 2, true)
                ),
                "s33_whiteout_equation", "s34_penguin_truth", null, false);
    }

    private static StoryCampaign.Mission borrowedFire() {
        return mission("borrowed_fire", "Borrowed Fire",
                "Return the stolen thermal charge to Ashfall without burning the cathedral apart.",
                ASHFALL_CATHEDRAL, CROWN_OCCUPIED, StoryCampaign.PlayablePolicy.forced(PHOENIX),
                fighters(ally(PENGUIN, "Penguin")),
                fighters(enemy(VULTURE, "Thermal Collector"), enemy(FALCON, "Crown Recovery Wing")),
                phases(
                        phase(CAPTURE, "Return the thermal charge", 42, 3, true),
                        phase(HOLD_ZONE, "Stabilize the cathedral heart", 34, 1, true)
                ),
                "s35_borrowed_fire", "s36_archive_route", null, false);
    }

    private static StoryCampaign.Mission crownArchive() {
        return mission("crown_archive", "The Crown Archive",
                "Enter Eagle's combat archive and extract the weapon-training ledger.",
                BATTLEFIELD, CROWN_OCCUPIED, StoryCampaign.PlayablePolicy.choice(PENGUIN, PHOENIX),
                List.of(),
                fighters(
                        boss(FALCON, "Falcon", 225, 1.25, 1.19),
                        enemy(EAGLE, "Crown Honor Guard")
                ),
                phases(
                        phase(CAPTURE, "Open the archive vaults", 40, 3, true),
                        phase(GAUNTLET, "Defeat the archive guard", 0, 1, true),
                        phase(REACH_EXIT, "Escape with the ledger", 32, 1, true)
                ),
                "s37_archive_before", "s38_archive_reveal", null, false);
    }

    // === ACT VII ===

    private static StoryCampaign.Mission lastCall() {
        return mission("last_call", "Last Call",
                "Evacuate the Charles Lounge, protect its guests, and leave before the Crown seals the block.",
                CITY, BirdGame3.MapVariant.PARLIAMENT_ROOFTOPS, EVACUATION,
                StoryCampaign.PlayablePolicy.forced(MOCKINGBIRD),
                fighters(ally(PIGEON, "Pigeon")),
                fighters(enemy(FALCON, "Crown Officer"), enemy(RAVEN, "Blackwing Informant")),
                phases(
                        phase(PROTECT, "Protect the lounge evacuation", 42, 1, true),
                        phase(REACH_EXIT, "Leave before the block seals", 32, 1, true)
                ),
                "s39_last_call", "s40_lounge_falls", null, false);
    }

    private static StoryCampaign.Mission cutTheLock() {
        return mission("cut_the_lock", "Cut the Lock",
                "Face Eagle alone on the command bridge while the other bird frees Pigeon's transport.",
                SKYCLIFFS, BirdGame3.MapVariant.CROWN_DUEL, CROWN_DUEL,
                StoryCampaign.PlayablePolicy.choice(FALCON, RAZORBILL),
                List.of(),
                fighters(boss(EAGLE, "Eagle", 480, 1.52, 1.20)),
                phases(phase(BOSS_PHASES, "Defeat Eagle on the command bridge", 0, 4, true)),
                "s41_cut_lock", "s42_falcon_defects", FALCON, false);
    }

    private static StoryCampaign.Mission sparrowsCorridor() {
        return mission("sparrows_corridor", "Sparrow's Corridor",
                "Lead the civilian column through the west corridor while Old Sparrow holds its route open.",
                CITY, EVACUATION, StoryCampaign.PlayablePolicy.choice(PIGEON, MOCKINGBIRD),
                fighters(ally(TITMOUSE, "Titmouse")),
                fighters(enemy(EAGLE, "Crown Blockade"), enemy(FALCON, "Crown Drone Wing")),
                phases(
                        phase(PROTECT, "Escort the civilian column", 52, 1, true),
                        phase(REACH_EXIT, "Clear the west corridor", 34, 1, true)
                ),
                "s43_sparrow_corridor", "s44_old_sparrow_death", null, false);
    }

    private static StoryCampaign.Mission marshalLaw() {
        return mission("marshal_law", "Marshal Law",
                "Defeat Eagle at the central command platform before the lockdown reaches every region.",
                BATTLEFIELD, CROWN_OCCUPIED, StoryCampaign.PlayablePolicy.choice(PIGEON, FALCON),
                fighters(ally(MOCKINGBIRD, "Charles")),
                fighters(boss(EAGLE, "Eagle", 390, 1.48, 1.17)),
                phases(
                        phase(CAPTURE, "Break the command seals", 36, 2, true),
                        phase(BOSS_PHASES, "Defeat Eagle", 0, 3, true)
                ),
                "s45_marshal_before", "s46_lockdown", null, false);
    }

    // === ACT VIII ===

    private static StoryCampaign.Mission crowCountry() {
        return mission("crow_country", "Crow Country",
                "Cross Vulture's outer feeding grounds and break the three swarm towers.",
                FOREST, CARRION, StoryCampaign.PlayablePolicy.choice(GOOSE, TURKEY),
                List.of(),
                fighters(enemy(VULTURE, "Carrion Captain"), enemy(RAVEN, "Swarm Guide")),
                phases(
                        phase(CAPTURE, "Break the swarm towers", 44, 3, true),
                        eliminate("Clear the released flock")
                ),
                "s47_crow_country", "s48_crow_country_after", null, false);
    }

    private static StoryCampaign.Mission termsInDark() {
        return mission("terms_in_dark", "Terms in the Dark",
                "Decode Vulture's sabotage ledger while the Pact tries to erase it.",
                CAVE, CARRION, StoryCampaign.PlayablePolicy.choice(BAT, SHOEBILL),
                List.of(),
                fighters(enemy(OPIUMBIRD, "Haze Keeper"), enemy(RAVEN, "Ledger Shadow")),
                phases(
                        phase(PROTECT, "Protect the sabotage ledger", 42, 1, true),
                        phase(CAPTURE, "Decode the Crown routes", 36, 3, true)
                ),
                "s49_terms_dark", "s50_vulture_truth", null, false);
    }

    private static StoryCampaign.Mission carrionAudience() {
        return mission("carrion_audience", "Carrion Audience",
                "Beat Vulture cleanly enough to force the whole bargain into the open.",
                CAVE, CARRION, StoryCampaign.PlayablePolicy.choice(PIGEON, BAT),
                List.of(),
                fighters(
                        boss(VULTURE, "Vulture", 430, 1.52, 1.08),
                        enemy(VULTURE, "Carrion Guard"),
                        enemy(BAT, "Cave Sentry")
                ),
                phases(
                        eliminate("Break the carrion guard"),
                        phase(BOSS_PHASES, "Defeat Vulture", 0, 3, true)
                ),
                "s51_carrion_audience", "s52_vulture_taken", null, false);
    }

    // === ACT IX ===

    private static StoryCampaign.Mission blueWater() {
        return mission("blue_water", "Blue Water",
                "Stop Heisenbird from flooding Broken Harbor with an overloaded stormglass line.",
                DOCK, ANCHOR_ASSAULT, StoryCampaign.PlayablePolicy.choice(PELICAN, ROADRUNNER),
                fighters(ally(RAZORBILL, "Razorbill")),
                fighters(boss(HEISENBIRD, "Heisenbird", 360, 1.43, 1.20)),
                phases(
                        phase(CAPTURE, "Close the stormglass valves", 40, 3, true),
                        phase(BOSS_PHASES, "Defeat Heisenbird", 0, 3, true)
                ),
                "s53_blue_water", "s54_heisen_joins", HEISENBIRD, false);
    }

    private static StoryCampaign.Mission futureThatMoves() {
        return mission("future_that_moves", "A Future That Moves",
                "Break Opium Bird's forecast lattice by choosing routes it marked impossible.",
                VIBRANT_JUNGLE, ANCHOR_ASSAULT,
                StoryCampaign.PlayablePolicy.choice(HUMMINGBIRD, TITMOUSE),
                List.of(),
                fighters(boss(OPIUMBIRD, "Opium Bird", 340, 1.38, 1.22)),
                phases(
                        phase(CAPTURE, "Take the impossible routes", 38, 4, true),
                        phase(BOSS_PHASES, "Defeat Opium Bird", 0, 3, true)
                ),
                "s55_future_moves", "s56_opium_joins", OPIUMBIRD, false);
    }

    private static StoryCampaign.Mission stolenWinter() {
        return mission("stolen_winter", "Stolen Winter",
                "Chase Grinch-Hawk through the fjord and recover the thermal key before the ice splits.",
                FROSTBITE_FJORD, ANCHOR_ASSAULT,
                StoryCampaign.PlayablePolicy.choice(GOOSE, ROOSTER),
                List.of(),
                fighters(boss(GRINCHHAWK, "Grinch-Hawk", 320, 1.39, 1.18)),
                phases(
                        phase(CAPTURE, "Recover the stolen key fragments", 40, 3, true),
                        phase(REACH_EXIT, "Catch Grinch-Hawk at the glacier shelf", 30, 1, true),
                        eliminate("Win the glacier duel")
                ),
                "s57_stolen_winter", "s58_grinch_joins", GRINCHHAWK, false);
    }

    private static StoryCampaign.Mission masterKey() {
        return mission("master_key", "The Master Key",
                "Catch Raven at the desert relay and decide whether the coalition can use a spy it cannot trust.",
                DESERT, ANCHOR_ASSAULT, StoryCampaign.PlayablePolicy.forced(MOCKINGBIRD),
                List.of(),
                fighters(boss(RAVEN, "Raven", 330, 1.41, 1.27)),
                phases(
                        phase(REACH_EXIT, "Catch Raven at the relay", 34, 1, true),
                        phase(BOSS_PHASES, "Defeat Raven", 0, 3, true)
                ),
                "s59_master_key", "s60_raven_joins", RAVEN, false);
    }

    // === ACT X ===

    private static StoryCampaign.Mission perfectWeather() {
        return mission("perfect_weather", "Perfect Weather",
                "Protect the Ashfall civilians from the Crown's null echoes after it rejects Eagle's command.",
                ASHFALL_CATHEDRAL, BirdGame3.MapVariant.ASHFALL_REBIRTH, STILLNESS,
                StoryCampaign.PlayablePolicy.forced(EAGLE),
                List.of(),
                fighters(enemy(VULTURE, "Null Echo"), enemy(RAVEN, "Null Echo")),
                phases(
                        phase(PROTECT, "Protect the Ashfall civilians", 48, 1, true),
                        eliminate("Break the null echoes")
                ),
                "s61_perfect_weather", "s62_eagle_breaks", EAGLE, false);
    }

    private static StoryCampaign.Mission worldGoesStill() {
        return mission("world_goes_still", "The World Goes Still",
                "Keep the Sky Cliffs evacuation lane moving while the Crown freezes the platforms.",
                SKYCLIFFS, BirdGame3.MapVariant.SKYBREAK_SPIRES, STILLNESS,
                StoryCampaign.PlayablePolicy.choice(PIGEON, FALCON, PHOENIX),
                fighters(ally(EAGLE, "Eagle")),
                fighters(enemy(VULTURE, "Null Echo"), enemy(FALCON, "Null Echo")),
                phases(
                        phase(PROTECT, "Protect the evacuation lane", 52, 1, true),
                        phase(HOLD_ZONE, "Hold the final updraft", 36, 1, true)
                ),
                "s63_world_still", "s64_after_stillness", null, false);
    }

    private static StoryCampaign.Mission blackoutKey() {
        return mission("blackout_key", "Blackout Key",
                "Break into Crownlock beneath the city's old central station and recover Vulture's transfer route.",
                PRISON, CROWN_OCCUPIED,
                StoryCampaign.PlayablePolicy.choice(RAVEN, GRINCHHAWK, HEISENBIRD),
                fighters(ally(OPIUMBIRD, "Opium Bird")),
                fighters(enemy(EAGLE, "Crown Automaton"), enemy(VULTURE, "Null Echo")),
                phases(
                        phase(CAPTURE, "Open the prison locks", 42, 4, true),
                        phase(GAUNTLET, "Clear the automated guard", 0, 1, true),
                        phase(REACH_EXIT, "Escape with Vulture's route", 32, 1, true)
                ),
                "s65_blackout_key", "s66_null_roc_wakes", null, false);
    }

    // === ACT XI ===

    private static StoryCampaign.Mission fireAndIce() {
        return mission("fire_and_ice", "Fire and Ice",
                "Destroy the northern anchor by balancing Ashfall heat against the frozen Crown line.",
                FROSTBITE_FJORD, ANCHOR_ASSAULT,
                StoryCampaign.PlayablePolicy.choice(PENGUIN, PHOENIX),
                List.of(),
                fighters(enemy(EAGLE, "North Warden"), enemy(HEISENBIRD, "Null Technician")),
                phases(
                        phase(CAPTURE, "Balance the thermal relays", 42, 3, true),
                        phase(PROTECT, "Protect the overload", 36, 1, true)
                ),
                "s67_fire_ice", "s68_north_anchor", null, false);
    }

    private static StoryCampaign.Mission harborEngine() {
        return mission("harbor_engine", "Harbor Engine",
                "Turn Broken Harbor's lever and guns against the western Crown anchor.",
                DOCK, BirdGame3.MapVariant.TITAN_DOCK, ANCHOR_ASSAULT,
                StoryCampaign.PlayablePolicy.choice(PELICAN, GOOSE, ROADRUNNER),
                fighters(ally(RAZORBILL, "Razorbill")),
                fighters(enemy(VULTURE, "West Warden"), enemy(HEISENBIRD, "Null Engineer")),
                phases(
                        phase(HOLD_ZONE, "Hold the harbor lever", 42, 1, true),
                        phase(CAPTURE, "Prime the harbor guns", 38, 3, true),
                        phase(SURVIVE, "Cover the anchor strike", 28, 1, true)
                ),
                "s69_harbor_engine", "s70_west_anchor", null, false);
    }

    private static StoryCampaign.Mission echoChain() {
        return mission("echo_chain", "Echo Chain",
                "Send a counter-pulse through the caves and sever the Crown's underground anchor.",
                CAVE, ANCHOR_ASSAULT,
                StoryCampaign.PlayablePolicy.choice(BAT, OPIUMBIRD, SHOEBILL),
                List.of(),
                fighters(enemy(RAVEN, "Echo Warden"), enemy(VULTURE, "Null Listener")),
                phases(
                        phase(CAPTURE, "Tune the counter-pulse", 44, 4, true),
                        phase(PROTECT, "Protect the echo chain", 36, 1, true)
                ),
                "s71_echo_chain", "s72_under_anchor", null, false);
    }

    private static StoryCampaign.Mission freeTheFlock() {
        return mission("free_the_flock", "Free the Flock",
                "Return to Crownlock, reach Vulture's extraction cell, and destroy the shadow copy guarding his mind.",
                PRISON, ANCHOR_ASSAULT,
                StoryCampaign.PlayablePolicy.choice(PIGEON, EAGLE, MOCKINGBIRD),
                fighters(ally(FALCON, "Falcon")),
                fighters(
                        boss(VULTURE, "Shadow Vulture", 420, 1.50, 1.12, BirdGame3.CAMPAIGN_NULL_ECHO_SKIN),
                        enemy(RAVEN, "Crown Automaton")
                ),
                phases(
                        phase(CAPTURE, "Open Vulture's extraction cell", 40, 3, true),
                        phase(BOSS_PHASES, "Destroy the shadow copy", 0, 3, true)
                ),
                "s73_free_flock", "s74_vulture_freed", VULTURE, false);
    }

    // === ACT XII ===

    private static StoryCampaign.Mission lastApproach() {
        return mission("last_approach", "The Last Approach",
                "Hold the four Crown approaches while every regional squad reaches the final platform.",
                BEACON_CROWN, ANCHOR_ASSAULT,
                StoryCampaign.PlayablePolicy.choice(PIGEON, EAGLE, VULTURE),
                fighters(ally(FALCON, "Falcon"), ally(RAVEN, "Raven")),
                fighters(enemy(VULTURE, "Null Herald"), enemy(RAVEN, "Crown Remnant")),
                phases(
                        phase(CAPTURE, "Secure the four approaches", 46, 4, true),
                        phase(GAUNTLET, "Clear the final approach", 0, 1, true)
                ),
                "s75_last_approach", "s76_every_wing", null, false);
    }

    private static StoryCampaign.Mission nullRoc() {
        return mission("null_roc", "Null Roc",
                "Break the copied flock-mind, strip the Crown armor, and bring down Eagle's final weapon.",
                BEACON_CROWN, BirdGame3.MapVariant.NULL_ROC_ASCENDING, NULL_ROC,
                StoryCampaign.PlayablePolicy.fullRoster(),
                fighters(ally(EAGLE, "Eagle"), ally(VULTURE, "Vulture"), ally(PHOENIX, "Phoenix")),
                fighters(boss(VULTURE, "Null Roc", 820, 1.74, 1.06, TIDE_VULTURE_SKIN)),
                phases(
                        phase(CAPTURE, "Break the Crown pylons", 44, 3, true),
                        phase(BOSS_PHASES, "Defeat Null Roc", 0, 4, true)
                ),
                "s77_null_roc_before", "s78_shell_falls", null, true);
    }

    private static StoryCampaign.Mission nullRock() {
        return mission("the_null_rock", "The Null Rock",
                "Break the living core's command network, plant Penguin's charge, then face The Null Rock directly when it tears through the cavern wall.",
                BEACON_CROWN, BirdGame3.MapVariant.VOID_CROWN, NULL_ROCK,
                StoryCampaign.PlayablePolicy.fullRoster(),
                List.of(),
                fighters(boss(VULTURE, "The Null Rock", 1200, 1.86, 1.04, NULL_ROCK_SKIN)),
                phases(
                        phase(CAPTURE, "Break the command roosts", 54, 4, true),
                        phase(HOLD_ZONE, "Join every flock signature", 48, 6, true),
                        phase(CAPTURE, "Plant Penguin's cavern charge", 38, 2, true),
                        phase(BOSS_PHASES, "Defeat The Null Rock one-on-one", 0, 5, true)
                ),
                "s79_null_rock_before", "s80_eagle_end", null, true);
    }

    private static StoryCampaign.Cutscene scene(
            String id,
            String title,
            BirdGame3.MapType location,
            List<BirdGame3.BirdType> handoff,
            boolean death,
            boolean finale
    ) {
        return new StoryCampaign.Cutscene(
                id,
                title,
                location,
                id.equals("s79_null_rock_before") || id.equals("s80_eagle_end")
                        ? "music-null-rock.mp3"
                        : musicFor(location, finale),
                parseScript(StoryDialogueScripts.require(DIALOGUE_SCRIPTS, id)),
                handoff,
                death,
                finale
        );
    }

    static List<StoryCampaign.DialogueLine> campaignPhaseDialogue(
            String heroSpeaker,
            BirdGame3.BirdType heroBird,
            String nextObjective
    ) {
        String objective = nextObjective == null ? "" : nextObjective.strip();
        List<StoryCampaign.DialogueLine> lines = new ArrayList<>();
        String script = StoryDialogueScripts.require(DIALOGUE_SCRIPTS, CAMPAIGN_PHASE_DIALOGUE_ID);
        int index = 0;
        for (String row : script.split("\\R")) {
            String trimmed = row.trim();
            if (trimmed.isEmpty() || trimmed.startsWith("#")) continue;
            int split = trimmed.indexOf('|');
            if (split <= 0 || split >= trimmed.length() - 1) {
                throw new IllegalArgumentException("Invalid campaign phase dialogue row: " + row);
            }
            String speakerToken = trimmed.substring(0, split).trim();
            String speaker = speakerToken.equals("{hero}") ? heroSpeaker : speakerToken;
            BirdGame3.BirdType bird = speakerToken.equals("{hero}") ? heroBird : birdForSpeaker(speakerToken);
            String text = trimmed.substring(split + 1).trim()
                    .replace("{objective}", objective);
            StoryCampaign.ShotStyle shot = index == 0 ? ACTION : REVEAL;
            StoryCampaign.ActorMotion motion = index == 0 ? RISE : IDLE;
            lines.add(new StoryCampaign.DialogueLine(speaker, bird, text, shot, motion, null, ""));
            index++;
        }
        if (lines.size() != 2) {
            throw new IllegalArgumentException(
                    "Dialogue section [" + CAMPAIGN_PHASE_DIALOGUE_ID + "] must contain exactly two lines");
        }
        return List.copyOf(lines);
    }

    static List<StoryCampaign.DialogueLine> nullRockDuelDialogue(
            String heroSpeaker,
            BirdGame3.BirdType heroBird
    ) {
        String speaker = heroSpeaker == null || heroSpeaker.isBlank() ? "Pigeon" : heroSpeaker.strip();
        String script = StoryDialogueScripts.require(DIALOGUE_SCRIPTS, NULL_ROCK_DUEL_DIALOGUE_ID)
                .replace("{hero}", speaker);
        List<StoryCampaign.DialogueLine> parsed = parseScript(script);
        if (heroBird == null) {
            return parsed;
        }
        return parsed.stream()
                .map(line -> line.speaker().equals(speaker)
                        ? new StoryCampaign.DialogueLine(line.speaker(), heroBird, line.text(),
                        line.shot(), line.motion(), line.whenSelected(), line.musicCue())
                        : line)
                .toList();
    }

    private static String musicFor(BirdGame3.MapType map, boolean finale) {
        if (finale || map == BEACON_CROWN) return "music-boss.mp3";
        return switch (map) {
            case CITY -> "music-city.mp3";
            case SKYCLIFFS -> "music-skycliffs.mp3";
            case VIBRANT_JUNGLE -> "music-jungle.mp3";
            case DESERT -> "music-desert.mp3";
            case CAVE -> "music-cave.mp3";
            case BATTLEFIELD -> "music-battlefield.mp3";
            case DOCK -> "music-dock.mp3";
            case FROSTBITE_FJORD -> "music-frostbite.mp3";
            case ASHFALL_CATHEDRAL -> "music-ashfall.mp3";
            case PRISON -> "music-prison.mp3";
            default -> "music-forest.mp3";
        };
    }

    /**
     * One authored line per text-block row: {@code Speaker|dialogue}. Optional
     * selection-specific rows use {@code Speaker@BIRD|dialogue}. Authors may
     * append {@code |SHOT|MOTION|MUSIC_CUE}; omitted direction is staged from
     * speaker entrances and conversational beats instead of an arbitrary action cycle.
     */
    private static List<StoryCampaign.DialogueLine> parseScript(String script) {
        List<StoryCampaign.DialogueLine> result = new ArrayList<>();
        String[] rows = script == null ? new String[0] : script.strip().split("\\R");
        Set<String> seenSpeakers = new LinkedHashSet<>();
        int visibleIndex = 0;
        for (String row : rows) {
            String trimmed = row.trim();
            if (trimmed.isEmpty() || trimmed.startsWith("#")) continue;
            String[] columns = trimmed.split("\\|", -1);
            if (columns.length < 2 || columns.length > 5
                    || columns[0].isBlank() || columns[1].isBlank()) {
                throw new IllegalArgumentException("Invalid story dialogue row: " + row);
            }
            String speakerToken = columns[0].trim();
            String text = columns[1].trim();
            BirdGame3.BirdType selected = null;
            int at = speakerToken.indexOf('@');
            if (at > 0) {
                selected = BirdGame3.BirdType.valueOf(speakerToken.substring(at + 1).trim());
                speakerToken = speakerToken.substring(0, at).trim();
            }
            BirdGame3.BirdType bird = birdForSpeaker(speakerToken);
            boolean firstAppearance = seenSpeakers.add(speakerToken);
            StoryCampaign.ShotStyle shot = columns.length >= 3 && !columns[2].isBlank()
                    ? StoryCampaign.ShotStyle.valueOf(columns[2].trim().toUpperCase(Locale.ROOT))
                    : directedShot(visibleIndex, firstAppearance, text);
            StoryCampaign.ActorMotion motion = columns.length >= 4 && !columns[3].isBlank()
                    ? StoryCampaign.ActorMotion.valueOf(columns[3].trim().toUpperCase(Locale.ROOT))
                    : directedMotion(visibleIndex, seenSpeakers.size(), firstAppearance, shot);
            String musicCue = columns.length >= 5 ? columns[4].trim() : "";
            result.add(new StoryCampaign.DialogueLine(
                    speakerToken, bird, text, shot, motion, selected, musicCue));
            visibleIndex++;
        }
        return List.copyOf(result);
    }

    private static StoryCampaign.ShotStyle directedShot(int index, boolean firstAppearance,
                                                        String text) {
        if (index == 0) return ESTABLISHING;
        String normalized = text.toLowerCase(Locale.ROOT);
        if (normalized.contains("look out")
                || normalized.contains("move!")
                || normalized.contains("get down")
                || normalized.contains("incoming")
                || normalized.contains("break the")
                || normalized.contains("cut the")) {
            return ACTION;
        }
        if (normalized.contains("the truth")
                || normalized.contains("i know")
                || normalized.contains("it was ")
                || normalized.contains("is gone")
                || normalized.contains("is awake")) {
            return REVEAL;
        }
        if (firstAppearance) return WIDE;
        if (index % 4 == 3) return CLOSE;
        if (index % 5 == 4) return PAN;
        return TWO_SHOT;
    }

    private static StoryCampaign.ActorMotion directedMotion(int index, int speakerCount,
                                                            boolean firstAppearance,
                                                            StoryCampaign.ShotStyle shot) {
        if (shot == ACTION && !firstAppearance) return ATTACK;
        if (!firstAppearance) return IDLE;
        if (index == 0 || speakerCount % 2 == 1) return ENTER_LEFT;
        return ENTER_RIGHT;
    }

    private static BirdGame3.BirdType birdForSpeaker(String speaker) {
        String key = speaker.toUpperCase(Locale.ROOT)
                .replace(" ", "")
                .replace("-", "")
                .replace("'", "");
        return switch (key) {
            case "PIGEON" -> PIGEON;
            case "EAGLE" -> EAGLE;
            case "FALCON" -> FALCON;
            case "PHOENIX" -> PHOENIX;
            case "HUMMINGBIRD" -> HUMMINGBIRD;
            case "TURKEY" -> TURKEY;
            case "ROOSTER" -> ROOSTER;
            case "ROADRUNNER" -> ROADRUNNER;
            case "PENGUIN" -> PENGUIN;
            case "SHOEBILL" -> SHOEBILL;
            case "CHARLES" -> MOCKINGBIRD;
            case "RAZORBILL" -> RAZORBILL;
            case "GRINCHHAWK" -> GRINCHHAWK;
            case "VULTURE", "NULLROC", "THENULLROCK" -> VULTURE;
            case "OPIUMBIRD" -> OPIUMBIRD;
            case "TITMOUSE" -> TITMOUSE;
            case "BAT" -> BAT;
            case "PELICAN" -> PELICAN;
            case "HEISENBIRD" -> HEISENBIRD;
            case "RAVEN" -> RAVEN;
            case "GOOSE" -> GOOSE;
            case "OLDSPARROW", "CIVILIAN", "CROWNSYSTEM" -> null;
            default -> throw new IllegalArgumentException("Unknown story speaker: " + speaker);
        };
    }

    private static void addActOneScenes(List<StoryCampaign.Cutscene> scenes) {
        scenes.add(scene("s01_dead_air", "When the Wind Stops", CITY,
                List.of(PIGEON), false, false));
        scenes.add(scene("s02_rooftop_after", "A Mark on the Metal", CITY,
                List.of(), false, false));
        scenes.add(scene("s03_harbor_alarm", "Harbor Lock", DOCK,
                List.of(PELICAN, GOOSE), false, false));
        scenes.add(scene("s04_harbor_after", "The Locked Tide", DOCK,
                List.of(), false, false));
        scenes.add(scene("s05_ashfall_warning", "The Last Thermal", ASHFALL_CATHEDRAL,
                List.of(PHOENIX), false, false));
        scenes.add(scene("s06_three_signals", "Three Signals", ASHFALL_CATHEDRAL,
                List.of(), false, false));
    }

    private static void addActTwoScenes(List<StoryCampaign.Cutscene> scenes) {
        scenes.add(scene("s07_broken_harbor", "Broken Harbor", DOCK,
                List.of(PELICAN), false, false));
        scenes.add(scene("s08_breakwater_after", "Weight in the Pouch", DOCK,
                List.of(), false, false));
        scenes.add(scene("s09_pier_nine", "Pier Nine", DOCK,
                List.of(GOOSE), false, false));
        scenes.add(scene("s10_razor_doubt", "The Cost of Precision", DOCK,
                List.of(), false, false));
        scenes.add(scene("s11_stormglass_before", "Pressure Test", DOCK,
                List.of(PELICAN, GOOSE), false, false));
        scenes.add(scene("s12_razorbill_joins", "A Contract Broken", DOCK,
                List.of(), false, false));
    }

    private static void addActThreeScenes(List<StoryCampaign.Cutscene> scenes) {
        scenes.add(scene("s13_painted_road", "Painted Road", DESERT,
                List.of(ROADRUNNER), false, false));
        scenes.add(scene("s14_falcon_report", "A Clean Report", DESERT,
                List.of(), false, false));
        scenes.add(scene("s15_orders_above", "Orders From Above", SKYCLIFFS,
                List.of(FALCON), false, false));
        scenes.add(scene("s16_falcon_choice", "The Open Gate", SKYCLIFFS,
                List.of(), false, false));
        scenes.add(scene("s17_open_sky_before", "Open Sky", SKYCLIFFS,
                List.of(ROADRUNNER, RAZORBILL), false, false));
        scenes.add(scene("s18_eagle_saves", "A Hero Arrives", SKYCLIFFS,
                List.of(), false, false));
    }

    private static void addActFourScenes(List<StoryCampaign.Cutscene> scenes) {
        scenes.add(scene("s19_needle_route", "Needle Route", VIBRANT_JUNGLE,
                List.of(HUMMINGBIRD), false, false));
        scenes.add(scene("s20_humming_after", "Slow Enough to Hear", VIBRANT_JUNGLE,
                List.of(), false, false));
        scenes.add(scene("s21_small_marks", "Small Marks", FOREST,
                List.of(TITMOUSE), false, false));
        scenes.add(scene("s22_titmouse_after", "The Nursery Holds", FOREST,
                List.of(), false, false));
        scenes.add(scene("s23_morning_line", "Morning Line", FOREST,
                List.of(TURKEY, ROOSTER), false, false));
        scenes.add(scene("s24_rooster_turkey", "Two Kinds of Leadership", FOREST,
                List.of(), false, false));
        scenes.add(scene("s25_green_convergence", "Green Convergence", VIBRANT_JUNGLE,
                List.of(HUMMINGBIRD, TITMOUSE, TURKEY, ROOSTER), false, false));
        scenes.add(scene("s26_first_coalition", "Not Waiting Anymore", VIBRANT_JUNGLE,
                List.of(), false, false));
    }

    private static void addActFiveScenes(List<StoryCampaign.Cutscene> scenes) {
        scenes.add(scene("s27_listen_below", "Listen Below", CAVE,
                List.of(BAT), false, false));
        scenes.add(scene("s28_bat_warning", "The Sound Underneath", CAVE,
                List.of(), false, false));
        scenes.add(scene("s29_stone_judgment", "Stone Judgment", CAVE,
                List.of(SHOEBILL), false, false));
        scenes.add(scene("s30_shoebill_decides", "A Neutral Door", CAVE,
                List.of(), false, false));
        scenes.add(scene("s31_second_pulse_before", "The Lower Chamber", CAVE,
                List.of(BAT, SHOEBILL, MOCKINGBIRD), false, false));
        scenes.add(scene("s32_thing_below", "Something Turns", CAVE,
                List.of(), false, false));
    }

    private static void addActSixScenes(List<StoryCampaign.Cutscene> scenes) {
        scenes.add(scene("s33_whiteout_equation", "Whiteout Equation", FROSTBITE_FJORD,
                List.of(PENGUIN), false, false));
        scenes.add(scene("s34_penguin_truth", "The Name on the Plans", FROSTBITE_FJORD,
                List.of(), false, false));
        scenes.add(scene("s35_borrowed_fire", "Borrowed Fire", ASHFALL_CATHEDRAL,
                List.of(PHOENIX), false, false));
        scenes.add(scene("s36_archive_route", "A Door Falcon Leaves Open", ASHFALL_CATHEDRAL,
                List.of(), false, false));
        scenes.add(scene("s37_archive_before", "The Crown Archive", BATTLEFIELD,
                List.of(PENGUIN, PHOENIX), false, false));
        scenes.add(scene("s38_archive_reveal", "Eagle's Signature", BATTLEFIELD,
                List.of(), false, false));
    }

    private static void addActSevenScenes(List<StoryCampaign.Cutscene> scenes) {
        scenes.add(scene("s39_last_call", "Last Call", CITY,
                List.of(MOCKINGBIRD), false, false));
        scenes.add(scene("s40_lounge_falls", "The Sign Comes Down", CITY,
                List.of(), false, false));
        scenes.add(scene("s41_cut_lock", "Cut the Lock", SKYCLIFFS,
                List.of(FALCON, RAZORBILL), false, false));
        scenes.add(scene("s42_falcon_defects", "Insignia", SKYCLIFFS,
                List.of(), false, false));
        scenes.add(scene("s43_sparrow_corridor", "Sparrow's Corridor", CITY,
                List.of(PIGEON, MOCKINGBIRD), false, false));
        scenes.add(scene("s44_old_sparrow_death", "The Gate", CITY,
                List.of(), true, false));
        scenes.add(scene("s45_marshal_before", "Marshal Law", BATTLEFIELD,
                List.of(PIGEON, FALCON), false, false));
        scenes.add(scene("s46_lockdown", "The Last Command", BATTLEFIELD,
                List.of(), false, false));
    }

    private static void addActEightScenes(List<StoryCampaign.Cutscene> scenes) {
        scenes.add(scene("s47_crow_country", "Crow Country", FOREST,
                List.of(GOOSE, TURKEY), false, false));
        scenes.add(scene("s48_crow_country_after", "The Outer Towers", FOREST,
                List.of(), false, false));
        scenes.add(scene("s49_terms_dark", "Terms in the Dark", CAVE,
                List.of(BAT, SHOEBILL), false, false));
        scenes.add(scene("s50_vulture_truth", "The Bargain", CAVE,
                List.of(), false, false));
        scenes.add(scene("s51_carrion_audience", "Carrion Audience", CAVE,
                List.of(PIGEON, BAT), false, false));
        scenes.add(scene("s52_vulture_taken", "Subject Secured", CAVE,
                List.of(), false, false));
    }

    private static void addActNineScenes(List<StoryCampaign.Cutscene> scenes) {
        scenes.add(scene("s53_blue_water", "Blue Water", DOCK,
                List.of(PELICAN, ROADRUNNER), false, false));
        scenes.add(scene("s54_heisen_joins", "An Engineer Without a Customer", DOCK,
                List.of(), false, false));
        scenes.add(scene("s55_future_moves", "A Future That Moves", VIBRANT_JUNGLE,
                List.of(HUMMINGBIRD, TITMOUSE), false, false));
        scenes.add(scene("s56_opium_joins", "The Twelfth Future", VIBRANT_JUNGLE,
                List.of(), false, false));
        scenes.add(scene("s57_stolen_winter", "Stolen Winter", FROSTBITE_FJORD,
                List.of(GOOSE, ROOSTER), false, false));
        scenes.add(scene("s58_grinch_joins", "The Stolen Key", FROSTBITE_FJORD,
                List.of(), false, false));
        scenes.add(scene("s59_master_key", "The Master Key", DESERT,
                List.of(MOCKINGBIRD), false, false));
        scenes.add(scene("s60_raven_joins", "No Clean Side", DESERT,
                List.of(), false, false));
    }

    private static void addActTenScenes(List<StoryCampaign.Cutscene> scenes) {
        scenes.add(scene("s61_perfect_weather", "Perfect Weather", ASHFALL_CATHEDRAL,
                List.of(EAGLE), false, false));
        scenes.add(scene("s62_eagle_breaks", "Command Rejected", ASHFALL_CATHEDRAL,
                List.of(), false, false));
        scenes.add(scene("s63_world_still", "The World Goes Still", SKYCLIFFS,
                List.of(PIGEON, FALCON, PHOENIX), false, false));
        scenes.add(scene("s64_after_stillness", "Movement", SKYCLIFFS,
                List.of(), false, false));
        scenes.add(scene("s65_blackout_key", "Blackout Key", PRISON,
                List.of(RAVEN, GRINCHHAWK, HEISENBIRD), false, false));
        scenes.add(scene("s66_null_roc_wakes", "Null Roc", PRISON,
                List.of(), false, false));
    }

    private static void addActElevenScenes(List<StoryCampaign.Cutscene> scenes) {
        scenes.add(scene("s67_fire_ice", "Fire and Ice", FROSTBITE_FJORD,
                List.of(PENGUIN, PHOENIX), false, false));
        scenes.add(scene("s68_north_anchor", "The Northern Line Breaks", FROSTBITE_FJORD,
                List.of(), false, false));
        scenes.add(scene("s69_harbor_engine", "Harbor Engine", DOCK,
                List.of(PELICAN, GOOSE, ROADRUNNER), false, false));
        scenes.add(scene("s70_west_anchor", "Broken Harbor Fires", DOCK,
                List.of(), false, false));
        scenes.add(scene("s71_echo_chain", "Echo Chain", CAVE,
                List.of(BAT, OPIUMBIRD, SHOEBILL), false, false));
        scenes.add(scene("s72_under_anchor", "The Chain Breaks", CAVE,
                List.of(), false, false));
        scenes.add(scene("s73_free_flock", "Free the Flock", PRISON,
                List.of(PIGEON, EAGLE, MOCKINGBIRD), false, false));
        scenes.add(scene("s74_vulture_freed", "No Forgiveness Required", PRISON,
                List.of(), false, false));
    }

    private static void addActTwelveScenes(List<StoryCampaign.Cutscene> scenes) {
        scenes.add(scene("s75_last_approach", "The Last Approach", BEACON_CROWN,
                List.of(PIGEON, EAGLE, VULTURE), false, true));
        scenes.add(scene("s76_every_wing", "Every Wing", BEACON_CROWN,
                List.of(), false, true));
        scenes.add(scene("s77_null_roc_before", "The Weapon Answers", BEACON_CROWN,
                List.of(), false, true));
        scenes.add(scene("s78_shell_falls", "The Shell Falls", BEACON_CROWN,
                List.of(), false, true));
        scenes.add(scene("s79_null_rock_before", "The Living Core", BEACON_CROWN,
                List.of(), false, true));
        scenes.add(scene("s80_eagle_end", "A Sky That Moves", BEACON_CROWN,
                List.of(), true, true));
    }
}
