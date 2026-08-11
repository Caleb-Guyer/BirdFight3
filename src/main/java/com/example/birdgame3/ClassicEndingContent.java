package com.example.birdgame3;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/** Authored "what if" epilogues earned by completing a bird's Classic route. */
final class ClassicEndingContent {
    enum Alignment {
        HOPEFUL,
        AMBIGUOUS,
        DOMINATING
    }

    record Ending(
            BirdGame3.BirdType bird,
            String routeTitle,
            String title,
            String crownChoice,
            Alignment alignment,
            BirdGame3.BirdType defeatedBoss,
            StoryCampaign.Cutscene cutscene
    ) {
    }

    private static final List<Ending> ENDINGS = List.of(
            ending(
                    BirdGame3.BirdType.PIGEON,
                    "ROOFTOP ASCENT",
                    "EVERY NEST, ITS OWN VOICE",
                    "LIBERATION — Dismantle the Crown into independent rooftop beacons.",
                    Alignment.HOPEFUL,
                    BirdGame3.BirdType.VULTURE,
                    BirdGame3.MapType.BEACON_CROWN,
                    "music-city.mp3",
                    lines(
                            system("NULL ROCK AUTHORITY ENDED. THE CROWN'S COMMAND CORE AWAITS ONE OWNER.",
                                    StoryCampaign.ShotStyle.REVEAL, StoryCampaign.ActorMotion.RISE),
                            line("The Null Rock", BirdGame3.BirdType.VULTURE,
                                    "One voice can still make every rooftop kneel. Take it.",
                                    StoryCampaign.ShotStyle.WIDE, StoryCampaign.ActorMotion.RECOIL),
                            line("Pigeon", BirdGame3.BirdType.PIGEON,
                                    "That's exactly why nobody gets it.",
                                    StoryCampaign.ShotStyle.ACTION, StoryCampaign.ActorMotion.ENTER_LEFT),
                            system("SELECT A MASTER.", StoryCampaign.ShotStyle.CLOSE, StoryCampaign.ActorMotion.IDLE),
                            line("Pigeon", BirdGame3.BirdType.PIGEON,
                                    "Select every nest.",
                                    StoryCampaign.ShotStyle.ACTION, StoryCampaign.ActorMotion.ATTACK),
                            system("CENTRAL COMMAND DELETED. ROOFTOP BEACONS NOW ANSWER ONLY TO THEIR OWN FLOCKS.",
                                    StoryCampaign.ShotStyle.REVEAL, StoryCampaign.ActorMotion.RECOIL),
                            line("Pigeon", BirdGame3.BirdType.PIGEON,
                                    "Good. Now the city can answer itself.",
                                    StoryCampaign.ShotStyle.ESTABLISHING, StoryCampaign.ActorMotion.TURN_AWAY,
                                    "music-victory.mp3")
                    )
            ),
            ending(
                    BirdGame3.BirdType.EAGLE,
                    "THE SKY HAS ONE KING",
                    "THE UNBROKEN CROWN",
                    "DOMINION — Bind every skyway and wing to Eagle's absolute order.",
                    Alignment.DOMINATING,
                    BirdGame3.BirdType.EAGLE,
                    BirdGame3.MapType.SKYCLIFFS,
                    "music-boss.mp3",
                    lines(
                            system("STORM TYRANT DEFEATED. THE CROWN CAN BIND EVERY CURRENT TO ONE WILL.",
                                    StoryCampaign.ShotStyle.REVEAL, StoryCampaign.ActorMotion.RISE),
                            line("Storm Tyrant", BirdGame3.BirdType.EAGLE,
                                    "Without one ruler, the open sky fractures.",
                                    StoryCampaign.ShotStyle.WIDE, StoryCampaign.ActorMotion.RECOIL),
                            line("Eagle", BirdGame3.BirdType.EAGLE,
                                    "Then it will have one.",
                                    StoryCampaign.ShotStyle.ACTION, StoryCampaign.ActorMotion.ENTER_RIGHT),
                            system("CROWN BINDING READY. ACCEPT TOTAL SKY AUTHORITY?",
                                    StoryCampaign.ShotStyle.CLOSE, StoryCampaign.ActorMotion.IDLE),
                            line("Eagle", BirdGame3.BirdType.EAGLE,
                                    "Every current. Every border. Every wing — under my protection.",
                                    StoryCampaign.ShotStyle.ACTION, StoryCampaign.ActorMotion.RISE),
                            system("AUTHORITY ACCEPTED. ALL SKYWAYS NOW ANSWER TO THE KING.",
                                    StoryCampaign.ShotStyle.REVEAL, StoryCampaign.ActorMotion.ATTACK),
                            line("Eagle", BirdGame3.BirdType.EAGLE,
                                    "No storm will challenge my sky again.",
                                    StoryCampaign.ShotStyle.CLOSE, StoryCampaign.ActorMotion.TURN_AWAY,
                                    "music-skycliffs.mp3")
                    )
            ),
            ending(
                    BirdGame3.BirdType.FALCON,
                    "NOTHING ESCAPES",
                    "SEVEN TARGETS REMAIN",
                    "VIGILANCE — Rewrite the Crown's hunt to pursue only cage-builders and tyrants.",
                    Alignment.AMBIGUOUS,
                    BirdGame3.BirdType.VULTURE,
                    BirdGame3.MapType.BEACON_CROWN,
                    "music-null-rock.mp3",
                    lines(
                            system("NULL ROC DESTROYED. EVERY LIVING TARGET REMAINS VISIBLE TO THE CROWN.",
                                    StoryCampaign.ShotStyle.REVEAL, StoryCampaign.ActorMotion.RISE),
                            line("Null Roc", BirdGame3.BirdType.VULTURE,
                                    "The hunt never ends.",
                                    StoryCampaign.ShotStyle.WIDE, StoryCampaign.ActorMotion.FALL),
                            line("Falcon", BirdGame3.BirdType.FALCON,
                                    "It ends when I say.",
                                    StoryCampaign.ShotStyle.ACTION, StoryCampaign.ActorMotion.FLY_BY),
                            system("TARGET LAW UNLOCKED. NAME THE PREY.",
                                    StoryCampaign.ShotStyle.CLOSE, StoryCampaign.ActorMotion.IDLE),
                            line("Falcon", BirdGame3.BirdType.FALCON,
                                    "Erase the weak. Mark only the birds who build cages.",
                                    StoryCampaign.ShotStyle.ACTION, StoryCampaign.ActorMotion.ATTACK),
                            system("TARGET LAW REWRITTEN. SEVEN TYRANTS REMAIN.",
                                    StoryCampaign.ShotStyle.REVEAL, StoryCampaign.ActorMotion.RECOIL),
                            line("Falcon", BirdGame3.BirdType.FALCON,
                                    "Seven is a short flight.",
                                    StoryCampaign.ShotStyle.PAN, StoryCampaign.ActorMotion.EXIT_RIGHT,
                                    "music-victory.mp3")
                    )
            ),
            ending(
                    BirdGame3.BirdType.PHOENIX,
                    "THE FLAME THAT RETURNS",
                    "THE FOUR SEASONS",
                    "RENEWAL — Divide the Crown's climate authority so no single keeper controls rebirth.",
                    Alignment.HOPEFUL,
                    BirdGame3.BirdType.GRINCHHAWK,
                    BirdGame3.MapType.FROSTBITE_FJORD,
                    "music-frostbite.mp3",
                    lines(
                            system("WINTER KING DEFEATED. THE CROWN'S CLIMATE THRONE REQUIRES A KEEPER.",
                                    StoryCampaign.ShotStyle.REVEAL, StoryCampaign.ActorMotion.RISE),
                            line("Winter King", BirdGame3.BirdType.GRINCHHAWK,
                                    "Take control, or winter returns the moment you leave.",
                                    StoryCampaign.ShotStyle.WIDE, StoryCampaign.ActorMotion.RECOIL),
                            line("Phoenix", BirdGame3.BirdType.PHOENIX,
                                    "Control is still a cage.",
                                    StoryCampaign.ShotStyle.ACTION, StoryCampaign.ActorMotion.RISE),
                            system("SEASONAL AUTHORITY CANNOT REMAIN EMPTY.",
                                    StoryCampaign.ShotStyle.CLOSE, StoryCampaign.ActorMotion.IDLE),
                            line("Phoenix", BirdGame3.BirdType.PHOENIX,
                                    "Then keep only one command: everything returns.",
                                    StoryCampaign.ShotStyle.ACTION, StoryCampaign.ActorMotion.ATTACK,
                                    "music-ashfall.mp3"),
                            system("CORE DIVIDED: FIRE. RAIN. FROST. BLOOM. NO SINGLE MASTER REMAINS.",
                                    StoryCampaign.ShotStyle.REVEAL, StoryCampaign.ActorMotion.RECOIL),
                            line("Phoenix", BirdGame3.BirdType.PHOENIX,
                                    "Let the world choose what rises from the ash.",
                                    StoryCampaign.ShotStyle.ESTABLISHING, StoryCampaign.ActorMotion.FLY_BY,
                                    "music-victory.mp3")
                    )
            ),
            ending(
                    BirdGame3.BirdType.HUMMINGBIRD,
                    "BEAT OF THE BLOOM",
                    "A GARDEN WITHOUT ORDERS",
                    "CONNECTION — Root the Crown in Heartbloom as a shared path rather than a command.",
                    Alignment.HOPEFUL,
                    BirdGame3.BirdType.RAVEN,
                    BirdGame3.MapType.VIBRANT_JUNGLE,
                    "music-jungle.mp3",
                    lines(
                            system("BLIGHTWING ROOT DESTROYED. ALL NECTAR ROUTES ACCEPT CENTRAL COMMAND.",
                                    StoryCampaign.ShotStyle.REVEAL, StoryCampaign.ActorMotion.RISE),
                            line("Blightwing", BirdGame3.BirdType.RAVEN,
                                    "One poisoned flower was enough to own the whole garden.",
                                    StoryCampaign.ShotStyle.WIDE, StoryCampaign.ActorMotion.FALL),
                            line("Hummingbird", BirdGame3.BirdType.HUMMINGBIRD,
                                    "And one healthy flower is never alone.",
                                    StoryCampaign.ShotStyle.ACTION, StoryCampaign.ActorMotion.FLY_BY),
                            system("DEFINE THE NEW POLLINATION COMMAND.",
                                    StoryCampaign.ShotStyle.CLOSE, StoryCampaign.ActorMotion.IDLE),
                            line("Hummingbird", BirdGame3.BirdType.HUMMINGBIRD,
                                    "No commands. Only directions.",
                                    StoryCampaign.ShotStyle.ACTION, StoryCampaign.ActorMotion.ATTACK),
                            system("CROWN ROOTED IN HEARTBLOOM. EVERY FLOWER NOW HOLDS ONE LIVING PATH.",
                                    StoryCampaign.ShotStyle.REVEAL, StoryCampaign.ActorMotion.RISE),
                            line("Hummingbird", BirdGame3.BirdType.HUMMINGBIRD,
                                    "Catch me if you need the next one.",
                                    StoryCampaign.ShotStyle.PAN, StoryCampaign.ActorMotion.EXIT_RIGHT,
                                    "music-victory.mp3")
                    )
            ),
            ending(
                    BirdGame3.BirdType.TURKEY,
                    "THE LAST FEAST",
                    "THE FIRST OPEN TABLE",
                    "PROVISION — Forge the Crown into table bells that open every locked food store.",
                    Alignment.HOPEFUL,
                    BirdGame3.BirdType.PELICAN,
                    BirdGame3.MapType.FOREST,
                    "music-forest.mp3",
                    lines(
                            system("THE GREAT HUNGER ENDED. THE CROWN CONTROLS EVERY LOCKED STOREHOUSE.",
                                    StoryCampaign.ShotStyle.REVEAL, StoryCampaign.ActorMotion.RISE),
                            line("The Devourer", BirdGame3.BirdType.PELICAN,
                                    "If you do not take it, somebody hungry will.",
                                    StoryCampaign.ShotStyle.WIDE, StoryCampaign.ActorMotion.RECOIL),
                            line("Turkey", BirdGame3.BirdType.TURKEY,
                                    "That's why it won't be a throne.",
                                    StoryCampaign.ShotStyle.ACTION, StoryCampaign.ActorMotion.ENTER_LEFT),
                            system("DEFINE THE NEW DISTRIBUTION LAW.",
                                    StoryCampaign.ShotStyle.CLOSE, StoryCampaign.ActorMotion.IDLE),
                            line("Turkey", BirdGame3.BirdType.TURKEY,
                                    "First plate to the smallest. Last plate to me.",
                                    StoryCampaign.ShotStyle.ACTION, StoryCampaign.ActorMotion.ATTACK),
                            system("CROWN FORGED INTO SEVEN TABLE BELLS. ALL STOREHOUSES OPEN.",
                                    StoryCampaign.ShotStyle.REVEAL, StoryCampaign.ActorMotion.RECOIL),
                            line("Turkey", BirdGame3.BirdType.TURKEY,
                                    "Sit down. Nobody eats alone.",
                                    StoryCampaign.ShotStyle.CROWD, StoryCampaign.ActorMotion.IDLE,
                                    "music-victory.mp3")
                    )
            ),
            ending(
                    BirdGame3.BirdType.ROOSTER,
                    "NO ONE LEFT BEHIND",
                    "A BELL FOR EVERY WING",
                    "PROTECTION — Split the Crown into recall bells that can guide but never compel.",
                    Alignment.HOPEFUL,
                    BirdGame3.BirdType.RAVEN,
                    BirdGame3.MapType.BEACON_CROWN,
                    "music-boss.mp3",
                    lines(
                            system("BROODBREAKER ECLIPSE ENDED. TOTAL FLOCK CONTROL REMAINS AVAILABLE.",
                                    StoryCampaign.ShotStyle.REVEAL, StoryCampaign.ActorMotion.RISE),
                            line("The Broodbreaker", BirdGame3.BirdType.RAVEN,
                                    "They scattered once. They will scatter again.",
                                    StoryCampaign.ShotStyle.WIDE, StoryCampaign.ActorMotion.RECOIL),
                            line("Rooster", BirdGame3.BirdType.ROOSTER,
                                    "Then I'll call. I won't compel.",
                                    StoryCampaign.ShotStyle.ACTION, StoryCampaign.ActorMotion.ENTER_LEFT),
                            system("THE CROWN CAN MAKE EVERY WING ANSWER.",
                                    StoryCampaign.ShotStyle.CLOSE, StoryCampaign.ActorMotion.IDLE),
                            line("Rooster", BirdGame3.BirdType.ROOSTER,
                                    "Break it into one bell for every wing.",
                                    StoryCampaign.ShotStyle.ACTION, StoryCampaign.ActorMotion.ATTACK),
                            system("CENTRAL COMMAND ENDED. RECALL BELLS ANSWER ONLY TO THEIR HOLDERS.",
                                    StoryCampaign.ShotStyle.REVEAL, StoryCampaign.ActorMotion.RECOIL),
                            line("Rooster", BirdGame3.BirdType.ROOSTER,
                                    "Count them. Nobody leaves Dawnwatch alone.",
                                    StoryCampaign.ShotStyle.CROWD, StoryCampaign.ActorMotion.RISE,
                                    "music-victory.mp3")
                    )
            )
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

    static StoryCampaign.Cutscene withRouteRecord(Ending ending, int birdCoins, int score, String mapReward) {
        if (ending == null) return null;
        List<StoryCampaign.DialogueLine> lines = new java.util.ArrayList<>(ending.cutscene().lines());
        String unlockedMap = mapReward == null || mapReward.isBlank()
                ? ""
                : " " + mapReward.strip().toUpperCase(java.util.Locale.ROOT) + " UNLOCKED.";
        lines.add(system("ROUTE BADGE RECORDED." + unlockedMap + " BIRD COINS +" + Math.max(0, birdCoins)
                        + ". FINAL SCORE " + String.format(java.util.Locale.US, "%,d", Math.max(0, score)) + ".",
                StoryCampaign.ShotStyle.REVEAL, StoryCampaign.ActorMotion.RISE));
        StoryCampaign.Cutscene base = ending.cutscene();
        return new StoryCampaign.Cutscene(
                base.id(), base.title(), base.location(), base.musicCue(), lines,
                base.handoffBirds(), base.deathScene(), base.finale());
    }

    private static Ending ending(BirdGame3.BirdType bird, String routeTitle, String title,
                                 String crownChoice, Alignment alignment,
                                 BirdGame3.BirdType defeatedBoss, BirdGame3.MapType location,
                                 String musicCue, List<StoryCampaign.DialogueLine> lines) {
        StoryCampaign.Cutscene cutscene = new StoryCampaign.Cutscene(
                "classic_ending_" + bird.name().toLowerCase(java.util.Locale.ROOT),
                title,
                location,
                musicCue,
                lines,
                List.of(),
                false,
                true
        );
        return new Ending(bird, routeTitle, title, crownChoice, alignment, defeatedBoss, cutscene);
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

    private static List<StoryCampaign.DialogueLine> lines(StoryCampaign.DialogueLine... entries) {
        return List.of(entries);
    }

    private static StoryCampaign.DialogueLine system(String text, StoryCampaign.ShotStyle shot,
                                                      StoryCampaign.ActorMotion motion) {
        return line("Crown System", null, text, shot, motion);
    }

    private static StoryCampaign.DialogueLine line(String speaker, BirdGame3.BirdType bird,
                                                   String text, StoryCampaign.ShotStyle shot,
                                                   StoryCampaign.ActorMotion motion) {
        return line(speaker, bird, text, shot, motion, "");
    }

    private static StoryCampaign.DialogueLine line(String speaker, BirdGame3.BirdType bird,
                                                   String text, StoryCampaign.ShotStyle shot,
                                                   StoryCampaign.ActorMotion motion,
                                                   String musicCue) {
        return new StoryCampaign.DialogueLine(
                speaker, bird, text, shot, motion, null, musicCue);
    }
}
