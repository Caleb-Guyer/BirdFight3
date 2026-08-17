package com.example.birdgame3;

import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Authored moving-picture epilogues earned by completing a bird's Classic route. */
final class ClassicEndingContent {
    enum Alignment {
        HOPEFUL,
        AMBIGUOUS,
        DOMINATING
    }

    enum Tableau {
        BOSS_AFTERMATH,
        CROWN_DISCOVERY,
        DECISION,
        CROWN_TRANSFORMATION,
        CHANGED_WORLD,
        FINAL_PORTRAIT
    }

    record Beat(String narration, Tableau tableau, double durationSeconds) {
        Beat {
            narration = narration == null ? "" : narration.strip();
            tableau = tableau == null ? Tableau.FINAL_PORTRAIT : tableau;
            durationSeconds = Math.clamp(durationSeconds, 2.5, 12.0);
        }
    }

    record RouteRecord(int birdCoins, int score, String mapReward) {
        RouteRecord {
            birdCoins = Math.max(0, birdCoins);
            score = Math.max(0, score);
            mapReward = mapReward == null ? "" : mapReward.strip();
        }
    }

    record Cinematic(
            String id,
            String title,
            BirdGame3.BirdType narrator,
            BirdGame3.BirdType defeatedBoss,
            String defeatedBossName,
            String defeatedBossSkin,
            BirdGame3.MapType location,
            String musicCue,
            List<Beat> beats,
            RouteRecord routeRecord
    ) {
        Cinematic {
            id = id == null ? "classic_ending" : id.strip();
            title = title == null ? "Classic Ending" : title.strip();
            defeatedBossName = defeatedBossName == null ? "The Final Boss" : defeatedBossName.strip();
            defeatedBossSkin = defeatedBossSkin == null ? "" : defeatedBossSkin.strip();
            musicCue = musicCue == null ? "" : musicCue.strip();
            beats = beats == null ? List.of() : List.copyOf(beats);
        }

        Cinematic withRouteRecord(RouteRecord record) {
            return new Cinematic(id, title, narrator, defeatedBoss, defeatedBossName,
                    defeatedBossSkin, location, musicCue, beats, record);
        }
    }

    record Ending(
            BirdGame3.BirdType bird,
            String routeTitle,
            String title,
            String crownChoice,
            Alignment alignment,
            Cinematic cinematic
    ) {
        BirdGame3.BirdType defeatedBoss() {
            return cinematic.defeatedBoss();
        }
    }

    private static final List<Ending> ENDINGS = List.of(
            ending(
                    BirdGame3.BirdType.PIGEON,
                    "ROOFTOP ASCENT",
                    "EVERY NEST, ITS OWN VOICE",
                    "LIBERATION - The Crown becomes independent rooftop beacons.",
                    Alignment.HOPEFUL,
                    BirdGame3.BirdType.VULTURE,
                    "The Null Rock",
                    "NULL_ROCK_VULTURE",
                    BirdGame3.MapType.BEACON_CROWN,
                    "music-city.mp3",
                    monologue(
                            beat("The Null Rock fell believing the whole sky needed one voice.", Tableau.BOSS_AFTERMATH),
                            beat("Then I heard the Crown waiting for me to become that voice.", Tableau.CROWN_DISCOVERY),
                            beat("I had climbed too far beside too many birds to call their freedom mine.", Tableau.DECISION),
                            beat("So I broke its command core and carried every shining piece back across the rooftops.", Tableau.CROWN_TRANSFORMATION),
                            beat("Now each nest keeps its own beacon. They can call one another, but none can give an order.", Tableau.CHANGED_WORLD),
                            beat("No king. No master. Just a city finally loud enough to answer itself.", Tableau.FINAL_PORTRAIT)
                    )),
            ending(
                    BirdGame3.BirdType.EAGLE,
                    "THE SKY HAS ONE KING",
                    "THE UNBROKEN CROWN",
                    "DOMINION - Every skyway is bound to Eagle's absolute order.",
                    Alignment.DOMINATING,
                    BirdGame3.BirdType.EAGLE,
                    "The Storm Tyrant",
                    "SKY_KING_EAGLE",
                    BirdGame3.MapType.SKYCLIFFS,
                    "music-boss.mp3",
                    monologue(
                            beat("The Storm Tyrant wore my face, but mistook violence for authority.", Tableau.BOSS_AFTERMATH),
                            beat("When the thunder cleared, the Crown offered every current, border, and wing.", Tableau.CROWN_DISCOVERY),
                            beat("A lesser bird would have shattered it and called the chaos freedom.", Tableau.DECISION),
                            beat("I took the Crown whole. The storm bent first. The mountains followed.", Tableau.CROWN_TRANSFORMATION),
                            beat("No flock is lost now. No rival crosses my sky without being seen.", Tableau.CHANGED_WORLD),
                            beat("The heavens are safe, because at last they belong to one king.", Tableau.FINAL_PORTRAIT)
                    )),
            ending(
                    BirdGame3.BirdType.FALCON,
                    "NOTHING ESCAPES",
                    "SEVEN TARGETS REMAIN",
                    "VIGILANCE - The Crown hunts only cage-builders and tyrants.",
                    Alignment.AMBIGUOUS,
                    BirdGame3.BirdType.VULTURE,
                    "Null Roc",
                    "NULL_ROCK_VULTURE",
                    BirdGame3.MapType.BEACON_CROWN,
                    "music-null-rock.mp3",
                    monologue(
                            beat("Null Roc climbed until there was nowhere left to run. I still caught it.", Tableau.BOSS_AFTERMATH),
                            beat("Inside its broken armor, the Crown showed me every living target at once.", Tableau.CROWN_DISCOVERY),
                            beat("Power like that should frighten me. Instead, I thought of every locked cage.", Tableau.DECISION),
                            beat("I erased the weak from its sight and marked only the birds who build prisons.", Tableau.CROWN_TRANSFORMATION),
                            beat("Seven lights remained. Seven rulers who believed height made them untouchable.", Tableau.CHANGED_WORLD),
                            beat("Seven is a short flight.", Tableau.FINAL_PORTRAIT)
                    )),
            ending(
                    BirdGame3.BirdType.PHOENIX,
                    "THE FLAME THAT RETURNS",
                    "THE FOUR SEASONS",
                    "RENEWAL - Climate authority is divided between four seasons.",
                    Alignment.HOPEFUL,
                    BirdGame3.BirdType.GRINCHHAWK,
                    "The Winter King",
                    "WINTER_KING_GRINCHHAWK",
                    BirdGame3.MapType.FROSTBITE_FJORD,
                    "music-frostbite.mp3",
                    monologue(
                            beat("The Winter King promised that nothing would hurt again if nothing could change.", Tableau.BOSS_AFTERMATH),
                            beat("Beneath the ice, the Crown still held enough power to choose the world's weather forever.", Tableau.CROWN_DISCOVERY),
                            beat("I know what endless fire becomes. I would not replace his winter with mine.", Tableau.DECISION),
                            beat("I divided the core into rain, frost, bloom, and flame, then let each piece turn.", Tableau.CROWN_TRANSFORMATION),
                            beat("The caldera thawed. Rivers moved. Seeds opened where an eternal season had stood.", Tableau.CHANGED_WORLD),
                            beat("Nothing rules forever. That is why everything gets another beginning.", Tableau.FINAL_PORTRAIT)
                    )),
            ending(
                    BirdGame3.BirdType.HUMMINGBIRD,
                    "BEAT OF THE BLOOM",
                    "A GARDEN WITHOUT ORDERS",
                    "CONNECTION - The Crown becomes a living path through Heartbloom.",
                    Alignment.HOPEFUL,
                    BirdGame3.BirdType.RAVEN,
                    "Blightwing Raven",
                    "BLIGHTWING_RAVEN",
                    BirdGame3.MapType.VIBRANT_JUNGLE,
                    "music-jungle.mp3",
                    monologue(
                            beat("Blightwing Raven poisoned one root and expected the whole garden to die quietly.", Tableau.BOSS_AFTERMATH),
                            beat("The Crown could reach every flower. It only needed someone to decide where every wing would go.", Tableau.CROWN_DISCOVERY),
                            beat("But a route is not an order. It is an invitation to keep moving.", Tableau.DECISION),
                            beat("I planted the command core in Heartbloom and beat my wings until it grew roots.", Tableau.CROWN_TRANSFORMATION),
                            beat("The paths now light one by one, from flower to flower, whenever a traveler needs them.", Tableau.CHANGED_WORLD),
                            beat("The garden has no ruler. If you need the next bloom, catch me.", Tableau.FINAL_PORTRAIT)
                    )),
            ending(
                    BirdGame3.BirdType.TURKEY,
                    "THE LAST FEAST",
                    "THE FIRST OPEN TABLE",
                    "PROVISION - The Crown becomes bells that open every food store.",
                    Alignment.HOPEFUL,
                    BirdGame3.BirdType.PELICAN,
                    "The Devourer",
                    "IRONCLAD_PELICAN",
                    BirdGame3.MapType.FOREST,
                    "music-forest.mp3",
                    monologue(
                            beat("The Devourer swallowed every offering because hunger had taught him that sharing was surrender.", Tableau.BOSS_AFTERMATH),
                            beat("The Crown held the keys to every sealed storehouse in the forest.", Tableau.CROWN_DISCOVERY),
                            beat("I could have guarded those doors and made every hungry bird ask my permission.", Tableau.DECISION),
                            beat("Instead, I hammered the core into seven bells and sent one to every table.", Tableau.CROWN_TRANSFORMATION),
                            beat("Whenever a bell rings, the locks open. The smallest plate is filled first.", Tableau.CHANGED_WORLD),
                            beat("There is no throne at my feast. Sit down. Nobody eats alone.", Tableau.FINAL_PORTRAIT)
                    )),
            ending(
                    BirdGame3.BirdType.ROOSTER,
                    "NO ONE LEFT BEHIND",
                    "A BELL FOR EVERY WING",
                    "PROTECTION - Recall bells guide the flock but can never compel it.",
                    Alignment.HOPEFUL,
                    BirdGame3.BirdType.RAVEN,
                    "The Broodbreaker",
                    "VOID_HERALD_RAVEN",
                    BirdGame3.MapType.BEACON_CROWN,
                    "music-boss.mp3",
                    monologue(
                            beat("The Broodbreaker scattered my flock and called their fear proof that loyalty was fragile.", Tableau.BOSS_AFTERMATH),
                            beat("The Crown could have forced every missing wing to return the instant I called.", Tableau.CROWN_DISCOVERY),
                            beat("But a flock brought home by chains is only another kind of prison.", Tableau.DECISION),
                            beat("I split the core into recall bells, one for every bird, each answering only its holder.", Tableau.CROWN_TRANSFORMATION),
                            beat("Across the dark, the bells became a path. They returned because they chose one another.", Tableau.CHANGED_WORLD),
                            beat("I counted every wing before sunrise. This time, no one was left behind.", Tableau.FINAL_PORTRAIT)
                    )),
            ending(
                    BirdGame3.BirdType.ROADRUNNER,
                    "NO FINISH LINE",
                    "THE ROAD AFTER THE MAP",
                    "PASSAGE - The Crown becomes a road that opens borders but commands no destination.",
                    Alignment.HOPEFUL,
                    BirdGame3.BirdType.SHOEBILL,
                    "The Still King",
                    "CLASSIC_SKIN_SHOEBILL",
                    BirdGame3.MapType.DESERT,
                    "music-desert.mp3",
                    monologue(
                            beat("The Still King built an ending for every road. I arrived before mine could close.", Tableau.BOSS_AFTERMATH),
                            beat("The Crown offered me every route at once, and the power to decide where every traveler stopped.", Tableau.CROWN_DISCOVERY),
                            beat("A road should help you leave. It should never choose the place you have to become.", Tableau.DECISION),
                            beat("So I melted the Crown into one bright line and ran it through every wall, cage, and border I could find.", Tableau.CROWN_TRANSFORMATION),
                            beat("It opened the prison, joined the nests, crossed the jungle, and vanished behind every bird who chose a turn.", Tableau.CHANGED_WORLD),
                            beat("At the edge of the map, I broke its compass needle. The horizon can draw itself after I get there.", Tableau.FINAL_PORTRAIT)
                    )),
            ending(
                    BirdGame3.BirdType.PENGUIN,
                    "THE ICE HOLDS",
                    "A SHELTER IN EVERY STORM",
                    "REFUGE - The Crown becomes shelter hearthstones that protect without commanding.",
                    Alignment.HOPEFUL,
                    BirdGame3.BirdType.PHOENIX,
                    "The Last Sun",
                    "ASHEN_SOVEREIGN_PHOENIX",
                    BirdGame3.MapType.FROSTBITE_FJORD,
                    "music-frostbite.mp3",
                    monologue(
                            beat("The Last Sun burned every shelter and called exposure freedom.", Tableau.BOSS_AFTERMATH),
                            beat("The Crown waited beneath the meltwater. One command could have frozen every ocean in place.", Tableau.CROWN_DISCOVERY),
                            beat("But a fort is not a kingdom. It is a promise that the storm stops here.", Tableau.DECISION),
                            beat("I broke its power into a thousand hearthstones and sent them beyond the ice.", Tableau.CROWN_TRANSFORMATION),
                            beat("Where each stone came to rest, walls rose with no locks and doors that opened from either side.", Tableau.CHANGED_WORLD),
                            beat("Let the world change. We will be ready when it does.", Tableau.FINAL_PORTRAIT)
                    )),
            ending(
                    BirdGame3.BirdType.SHOEBILL,
                    "THE LONG WATCH",
                    "THE WORLD HOLDS ITS BREATH",
                    "CLARITY - The Crown reveals every disguise once, then sleeps beneath the marsh.",
                    Alignment.AMBIGUOUS,
                    BirdGame3.BirdType.OPIUMBIRD,
                    "The Mire Oracle",
                    "CLASSIC_SKIN_OPIUMBIRD",
                    BirdGame3.MapType.FOREST,
                    "music-cave.mp3",
                    monologue(
                            beat("The Mire Oracle filled the water with answers. Every reflection lied in a different voice.", Tableau.BOSS_AFTERMATH),
                            beat("When the last false face broke, the Crown rose from the shrine and offered to make every secret visible forever.", Tableau.CROWN_DISCOVERY),
                            beat("I waited. Truth forced into the open can become another hunter.", Tableau.DECISION),
                            beat("So I used the Crown once. Across the world, disguises failed, hidden cages shone, and false rulers saw their own shadows named.", Tableau.CROWN_TRANSFORMATION),
                            beat("Then I carried it beneath the cypress roots. The marsh closed above it, quiet and awake.", Tableau.CHANGED_WORLD),
                            beat("Anything worthy of ruling the sky should survive being seen clearly.", Tableau.FINAL_PORTRAIT)
                    )),
            ending(
                    BirdGame3.BirdType.MOCKINGBIRD,
                    "NO VOICE BUT HIS OWN",
                    "THE LAST ORIGINAL",
                    "MEMORY - The Crown remembers every voice, but can command none of them.",
                    Alignment.AMBIGUOUS,
                    BirdGame3.BirdType.SHOEBILL,
                    "The Hollow Maestro",
                    "",
                    BirdGame3.MapType.RESONANCE_HALL,
                    "music-charles-ending.mp3",
                    monologue(
                            beat("The Hollow Maestro called obedience harmony. When it fell, the silence sounded more honest than its song.", Tableau.BOSS_AFTERMATH),
                            beat("The Crown opened every voice in the world to me at once. Every promise. Every warning. Every word never answered.", Tableau.CROWN_DISCOVERY),
                            beat("I spent my life borrowing voices. That did not give me the right to own them.", Tableau.DECISION),
                            beat("I returned what the Maestro had stolen, then tore command out of the Crown note by note.", Tableau.CROWN_TRANSFORMATION),
                            beat("It remembers every voice now, but it cannot force a single throat to speak. I kept the archive. Someone must hear what power tries to erase.", Tableau.CHANGED_WORLD),
                            beat("A voice matters because it can refuse. I will remember every voice. I will answer in my own.", Tableau.FINAL_PORTRAIT)
                    )),
            ending(
                    BirdGame3.BirdType.RAZORBILL,
                    "THE LINE BETWEEN WORLDS",
                    "THE FINAL CUT",
                    "RESTRAINT - The Crown's command is severed into seven boundary seals.",
                    Alignment.AMBIGUOUS,
                    BirdGame3.BirdType.RAVEN,
                    "The Seamreaver",
                    "",
                    BirdGame3.MapType.WORLDSEAM,
                    "music-razorbill-ending.mp3",
                    monologue(
                            beat("The Seamreaver wanted every boundary erased until nothing could stand apart from its command.", Tableau.BOSS_AFTERMATH),
                            beat("Beyond its broken core, the Crown showed me every border in creation as one line waiting to be cut.", Tableau.CROWN_DISCOVERY),
                            beat("Some walls are cages. Some are the distance that lets a voice remain its own.", Tableau.DECISION),
                            beat("I cut command away from the Crown, then divided its power into seven seals no single ruler can open.", Tableau.CROWN_TRANSFORMATION),
                            beat("The Worldseam closed. Gates still join distant skies, but they open only when both sides choose the crossing.", Tableau.CHANGED_WORLD),
                            beat("A blade is not freedom. It only makes the space where freedom must decide what comes next.", Tableau.FINAL_PORTRAIT)
                    )),
            ending(
                    BirdGame3.BirdType.GRINCHHAWK,
                    "THE LONGEST NIGHT",
                    "THE OPEN SACK",
                    "RELEASE - The Crown becomes a sack that opens hoarded vaults but can command no owner.",
                    Alignment.AMBIGUOUS,
                    BirdGame3.BirdType.SHOEBILL,
                    "The Bellkeeper",
                    "",
                    BirdGame3.MapType.MIDNIGHT_WORKSHOP,
                    "music-grinch-ending.mp3",
                    monologue(
                            beat("The Bellkeeper called it generosity. Every gift still had his name on it.", Tableau.BOSS_AFTERMATH),
                            beat("The Crown offered every lock, every ledger, every heart that mistook possession for love.", Tableau.CROWN_DISCOVERY),
                            beat("I could have owned it all. That sounded exactly like him.", Tableau.DECISION),
                            beat("I stitched the Crown into a bottomless sack and cut the name from every key it held.", Tableau.CROWN_TRANSFORMATION),
                            beat("Food, tools, and medicine poured from hoarded vaults. The world became louder, kinder, and much less orderly.", Tableau.CHANGED_WORLD),
                            beat("I didn't give the world anything. I just stole the word 'mine.'", Tableau.FINAL_PORTRAIT)
                    )),
            ending(
                    BirdGame3.BirdType.VULTURE,
                    "NOTHING GOES TO WASTE",
                    "THE FINAL ACCOUNT",
                    "RECKONING - The Crown becomes a ring of keys; one black ledger remains in Vulture's keeping.",
                    Alignment.AMBIGUOUS,
                    BirdGame3.BirdType.SHOEBILL,
                    "The Debt Engine",
                    "",
                    BirdGame3.MapType.CARRION_EXCHANGE,
                    "music-vulture-ending.mp3",
                    monologue(
                            beat("The Debt Engine measured every life as inventory: owned, owed, useful, or discarded.", Tableau.BOSS_AFTERMATH),
                            beat("Inside its broken furnace, the Crown offered me every debt in the sky—and every name chained to one.", Tableau.CROWN_DISCOVERY),
                            beat("Power loves a ledger. It makes cruelty look balanced when the columns line up.", Tableau.DECISION),
                            beat("I broke the Crown into keys. My crows carried them to every cage, archive, and locked granary they could find.", Tableau.CROWN_TRANSFORMATION),
                            beat("Doors opened. Debt records burned. The birds the Engine called waste chose their own names again.", Tableau.CHANGED_WORLD),
                            beat("I kept one black ledger. Not to collect what they owe—but to remember who built the cages, and where they ran.", Tableau.FINAL_PORTRAIT)
                    ))
    );

    private static final Map<BirdGame3.BirdType, Ending> BY_BIRD = indexEndings();

    private ClassicEndingContent() {
    }

    static List<Ending> endings() {
        return ENDINGS;
    }

    static Ending endingFor(BirdGame3.BirdType bird) {
        return bird == null ? null : BY_BIRD.get(bird);
    }

    static boolean hasEnding(BirdGame3.BirdType bird) {
        return endingFor(bird) != null;
    }

    static boolean isContinuousPanorama(Cinematic cinematic) {
        return cinematic != null && cinematic.narrator() == BirdGame3.BirdType.ROADRUNNER;
    }

    static boolean isSubglacialMontage(Cinematic cinematic) {
        return cinematic != null && cinematic.narrator() == BirdGame3.BirdType.PENGUIN;
    }

    static boolean isStillwaterRevelation(Cinematic cinematic) {
        return cinematic != null && cinematic.narrator() == BirdGame3.BirdType.SHOEBILL;
    }

    static boolean isCharlesLivingScore(Cinematic cinematic) {
        return cinematic != null && cinematic.narrator() == BirdGame3.BirdType.MOCKINGBIRD;
    }

    static boolean isRazorbillFinalCut(Cinematic cinematic) {
        return cinematic != null && cinematic.narrator() == BirdGame3.BirdType.RAZORBILL;
    }

    static boolean isGrinchHawkOpenSack(Cinematic cinematic) {
        return cinematic != null && cinematic.narrator() == BirdGame3.BirdType.GRINCHHAWK;
    }

    static boolean isVultureFinalAccount(Cinematic cinematic) {
        return cinematic != null && cinematic.narrator() == BirdGame3.BirdType.VULTURE;
    }

    static Cinematic withRouteRecord(Ending ending, int birdCoins, int score, String mapReward) {
        if (ending == null) return null;
        return ending.cinematic().withRouteRecord(new RouteRecord(birdCoins, score, mapReward));
    }

    static String routeRecordText(RouteRecord record) {
        if (record == null) return "";
        String map = record.mapReward().isBlank()
                ? ""
                : "  |  " + record.mapReward().toUpperCase(Locale.ROOT) + " UNLOCKED";
        return "ROUTE BADGE EARNED" + map + "  |  BIRD COINS +" + record.birdCoins()
                + "  |  SCORE " + String.format(Locale.US, "%,d", record.score());
    }

    private static Ending ending(BirdGame3.BirdType bird, String routeTitle, String title,
                                 String crownChoice, Alignment alignment,
                                 BirdGame3.BirdType defeatedBoss, String defeatedBossName,
                                 String defeatedBossSkin, BirdGame3.MapType location,
                                 String musicCue, List<Beat> beats) {
        Cinematic cinematic = new Cinematic(
                "classic_ending_" + bird.name().toLowerCase(Locale.ROOT),
                title,
                bird,
                defeatedBoss,
                defeatedBossName,
                defeatedBossSkin,
                location,
                musicCue,
                beats,
                null
        );
        return new Ending(bird, routeTitle, title, crownChoice, alignment, cinematic);
    }

    private static Map<BirdGame3.BirdType, Ending> indexEndings() {
        EnumMap<BirdGame3.BirdType, Ending> endings = new EnumMap<>(BirdGame3.BirdType.class);
        for (Ending ending : ENDINGS) {
            if (endings.put(ending.bird(), ending) != null) {
                throw new IllegalStateException("Duplicate Classic ending for " + ending.bird());
            }
        }
        return Map.copyOf(endings);
    }

    private static List<Beat> monologue(Beat... beats) {
        return List.of(beats);
    }

    private static Beat beat(String narration, Tableau tableau) {
        return new Beat(narration, tableau, tableau == Tableau.FINAL_PORTRAIT ? 7.2 : 6.2);
    }
}
