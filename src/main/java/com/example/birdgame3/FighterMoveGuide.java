package com.example.birdgame3;

import java.util.List;

/**
 * Player-facing move reference used by the in-match pause screen. Keeping the
 * copy in one data-only class lets the pause UI, tests, and future help screens
 * share the same descriptions without touching simulation state.
 */
final class FighterMoveGuide {
    record Move(String direction, String name, String description, boolean recovery) {
        Move {
            direction = clean(direction);
            name = clean(name);
            description = clean(description);
        }
    }

    record Guide(String role, String mechanic, List<Move> moves,
                 String ultimateName, String ultimateDescription) {
        Guide {
            role = clean(role);
            mechanic = clean(mechanic);
            moves = List.copyOf(moves);
            ultimateName = clean(ultimateName);
            ultimateDescription = clean(ultimateDescription);
            if (moves.size() != 4) {
                throw new IllegalArgumentException("A fighter guide must define four directional specials");
            }
        }
    }

    private FighterMoveGuide() {
    }

    static Guide forBird(BirdGame3.BirdType type) {
        if (type == null) {
            return guide("Unknown", "No fighter selected.", "Ultimate", "No Ultimate information.",
                    move("NEUTRAL", "-", "No move information."),
                    move("SIDE", "-", "No move information."),
                    recovery("UP", "-", "No recovery information."),
                    move("DOWN", "-", "No move information."));
        }
        return switch (type) {
            case PIGEON -> guide(
                    "Adaptable all-rounder",
                    "Hold Neutral to extend Long Peck. Hold Down to drill in the air or send cracks across the ground.",
                    "Skyward Seed Wave", "Ascends while flooding the stage with a gigantic rising wave of seeds.",
                    move("NEUTRAL", "Long Peck", "Hold to extend the peck's reach and active time."),
                    move("SIDE", "Street Rush", "Rushes forward through close pressure and launches on contact."),
                    recovery("UP", "Fire-Escape Flutter", "Flutters upward to recover and catches enemies above."),
                    move("DOWN", "Rooftop Breaker", "Hold in air for a drill; hold on ground to send cracks both ways."));
            case EAGLE -> guide(
                    "Powerful aerial bruiser",
                    "Egg Volley and Talon Rush both become stronger when held, but their charge commits Eagle in place.",
                    "Sovereign Storm", "Claims the sky with a devastating royal aerial assault.",
                    move("NEUTRAL", "Egg Volley", "Hold to release additional eggs across the lane."),
                    move("SIDE", "Charged Talon Rush", "Hold for a much faster, longer, and stronger piercing rush."),
                    recovery("UP", "Skyrise", "Climbs sharply and catches opponents directly above."),
                    move("DOWN", "Heavenfall", "Rises into position, then dives down with a heavy meteor hit."));
            case FALCON -> guide(
                    "Precision rushdown raptor",
                    "Falcon trades Eagle's raw authority for quicker precision chains between eggs, rush, climb, and dive.",
                    "Terminal Velocity", "Locks onto the lane and finishes with a high-speed aerial execution.",
                    move("NEUTRAL", "Egg Scatter", "Hold to scatter more eggs and cover a wider approach."),
                    move("SIDE", "Charged Razor Rush", "Hold for a faster, longer piercing dash."),
                    recovery("UP", "Jet Climb", "A quick vertical climb that can continue an aerial route."),
                    move("DOWN", "Meteor Strike", "Commits to a downward finishing dive after the rise."));
            case PHOENIX -> guide(
                    "Mobile fire zoner",
                    "Cinder Halo rewards a deliberate charge. Its other specials cover horizontal, vertical, and downward lanes.",
                    "Rebirth Supernova", "Phoenix burns out, returns, and detonates the arena in a rebirth blast.",
                    move("NEUTRAL", "Cinder Halo", "Charge to heal before releasing the surrounding fire burst."),
                    move("SIDE", "Snap Fire", "Fires across the lane; in air it angles downward and eventually fizzles."),
                    recovery("UP", "Firespin", "Spins upward through opponents while recovering."),
                    move("DOWN", "Faultfire", "Creates a downward fire lane; the airborne version can be held."));
            case HUMMINGBIRD -> guide(
                    "Lightning-fast combo fighter",
                    "Needle confirms build toward a finisher. Nectar Trap creates the point Hummingbird wants to route through.",
                    "Needleheart Overdrive", "Locks on, chains three flash pierces, then detonates a nectar final stab.",
                    move("NEUTRAL", "Needle Barrage", "Rapid needle hits build into a stronger combo finisher."),
                    move("SIDE", "Flash Sip", "Crosses a long distance and strikes through the target lane."),
                    recovery("UP", "Hover Burst", "Bursts upward with Hummingbird's strongest recovery angle."),
                    move("DOWN", "Nectar Trap", "Leaves nectar behind to control space and support future routes."));
            case TURKEY -> guide(
                    "Heavy pressure fighter",
                    "Turkey uses long holds and traps to occupy space. Panic Flap protects the area underneath him.",
                    "Harvest Tribunal", "Summons a feast altar, pulls enemies in, stuffs them, then delivers a golden verdict slash.",
                    move("NEUTRAL", "Charged Gobble Guard", "Hold before releasing a defensive close-range burst."),
                    move("SIDE", "Held Stampede", "Hold to extend the advancing pressure run."),
                    recovery("UP", "Panic Flap", "Opens the wings, gains height, and threatens the space below."),
                    move("DOWN", "Feast Trap", "Places a feast that slows and controls a section of the stage."));
            case ROOSTER -> guide(
                    "Brood commander",
                    "Rooster's chicks are real followers: call them, throw one, spend the flock for lift, or recall formation.",
                    "Dawn Stampede", "Floods the stage with a fast flying swarm of chicks.",
                    move("NEUTRAL", "Chick Call", "Adds a chick to the brood up to the current limit."),
                    move("SIDE", "Chick Toss", "Throws a follower forward as a mobile attack."),
                    recovery("UP", "Coop Boost", "Converts available chicks into additional vertical lift."),
                    move("DOWN", "Brood Recall", "Calls scattered chicks back into formation."));
            case ROADRUNNER -> guide(
                    "Momentum speedster",
                    "Running builds momentum. Beep-Blitz and Ricochet spend speed while Painted Road reloads the route.",
                    "Redline Execution", "Zips forward; a catch triggers a speed cutscene and enormous finishing launch.",
                    move("NEUTRAL", "Beep-Beep Blitz", "Converts stored momentum into a sudden close-range burst."),
                    move("SIDE", "Canyon Ricochet", "Bounces through the lane and cashes out movement speed."),
                    recovery("UP", "Dust Devil Lift", "Rides a compact dust devil back toward the stage."),
                    move("DOWN", "Painted Road", "Marks a route that supports Roadrunner's next momentum sequence."));
            case PENGUIN -> guide(
                    "Fort-building trickster",
                    "Snow Fort becomes a rolling snowball when Side is used nearby. Up rockets first, then turns into a flop.",
                    "Absolute Zero Fortress", "Creates an invulnerable ice throne and freezes control of the immediate arena.",
                    move("NEUTRAL", "Belly Slide", "Charge before sliding low across the ground."),
                    move("SIDE", "Iceberg", "Shoves a nearby fort into a rolling snowball or attacks forward."),
                    recovery("UP", "Rocket Flop", "Rockets upward, then transitions into a falling body attack."),
                    move("DOWN", "Snow Fort", "Builds solid cover; in the air, drops a straight-down iceberg."));
            case SHOEBILL -> guide(
                    "Patient heavyweight counter fighter",
                    "Death Stare punishes targets that keep their back turned. Hold Statue Counter to preserve the stance.",
                    "Final Stillness", "Drains the screen, silences the music, and fires an ancient locked beam.",
                    move("NEUTRAL", "Death Stare", "Telegraphs briefly, then catches an opponent who refuses to face Shoebill."),
                    move("SIDE", "Heavy Bill Thrust", "Controls the horizontal lane with a long, weighty bill strike."),
                    recovery("UP", "Marsh Lift", "Rises through the vertical lane with a broad hit."),
                    move("DOWN", "Statue Counter", "Hold the unmoving counter stance and punish an incoming hit."));
            case MOCKINGBIRD -> guide(
                    "Copy-and-control specialist",
                    "Forest Lounge captures an enemy neutral. Charles can spend that copy, then route through Mic Swing or Forest Lift.",
                    "Shadow Court", "Summons three fragile dark copies from the Lounge to overwhelm the stage.",
                    move("NEUTRAL", "Mimic", "Uses a captured enemy neutral; without a capture, blows foes away without damage."),
                    move("SIDE", "Charged Mic Swing", "Pulls out a microphone; hold to charge a wider, stronger swing."),
                    recovery("UP", "Forest Lift", "Uses the Lounge route to rise and strike through opponents."),
                    move("DOWN", "Forest Lounge", "Plants or relocates the Lounge and captures a nearby enemy neutral."));
            case RAZORBILL -> guide(
                    "Technical blade mobility fighter",
                    "Skimming Razor slices diagonally and cannot be reused until landing. Its hit brake sets up Cliff Shear.",
                    "Guillotine Wake", "Marks cut-lines, chains precision slashes, then leaves a damaging razor wake.",
                    move("NEUTRAL", "Razor Storm", "Hold to grow the storm before releasing its blades."),
                    move("SIDE", "Skimming Razor", "Cuts quickly on a shallow diagonal; landing is required before reuse."),
                    recovery("UP", "Cliff Shear", "Cuts upward along the recovery lane and follows a Side hit naturally."),
                    move("DOWN", "Counter Cut", "Enters a counter stance and retaliates against contact."));
            case GRINCHHAWK -> guide(
                    "Trap-based heavyweight",
                    "Heart Snatch steals health and pulls foes into Sleigh Crash range. Fake Present makes that space dangerous later.",
                    "Midnight Giftstorm", "Rains trap presents, then sends a stolen sleigh across the stage for a final gift-box slam.",
                    move("NEUTRAL", "Heart Snatch", "Flashes, pulls enemies inward, and steals health on a confirmed catch."),
                    move("SIDE", "Sleigh Crash", "Drives the sleigh through the horizontal lane."),
                    recovery("UP", "Chimney Flap", "Flaps upward with a large vertical coverage area."),
                    move("DOWN", "Fake Present", "Leaves a disguised present trap behind."));
            case VULTURE -> guide(
                    "Summoner and delayed-control fighter",
                    "Crows create pressure while Bone Offering turns one location into delayed flock control.",
                    "Black Sky Feast", "Floods the arena with crown crows before a final feast launch.",
                    move("NEUTRAL", "Summon Crows", "Calls owned crow followers that pressure nearby enemies."),
                    move("SIDE", "Carrion Glide", "Glides through the lane and converts flock pressure into a follow-up."),
                    recovery("UP", "Thermal Lift", "Rides a thermal upward while the flock guards the route."),
                    move("DOWN", "Bone Offering", "Places bait that redirects the flock into delayed stage control."));
            case OPIUMBIRD -> guide(
                    "Resource-powered setup fighter",
                    "Lotus Patch refuels the opium meter. Fueled Side and Up hits siphon some meter back to continue pressure.",
                    "Oneiric Collapse", "Overloads the arena with a dreamlike haze before the final launch.",
                    move("NEUTRAL", "Lean Cloud", "Starts a lingering cloud; spending meter strengthens the cast."),
                    move("SIDE", "Haze Drift", "A fueled hit launches harder, refunds meter, and brakes on contact."),
                    recovery("UP", "Rising Vapors", "Rises on vapor; fueled contact improves the route and refund."),
                    move("DOWN", "Lotus Patch", "Places the steady refill point for the opium resource."));
            case TITMOUSE -> guide(
                    "Mark-and-route rushdown",
                    "Scold applies MARK. Marked Vault hits harder, Barkskip brakes on contact, and Seed Stash can detonate later.",
                    "Mobbing Season", "Calls the flock into a fast coordinated marked-target assault.",
                    move("NEUTRAL", "Scold Chorus", "Marks the target so later route hits receive their payoff."),
                    move("SIDE", "Barkskip", "Skips forward and brakes on contact to keep pressure close."),
                    recovery("UP", "Tuft Vault", "Vaults upward; a marked hit receives the stronger follow-up."),
                    move("DOWN", "Seed Stash", "Plants a stash or detonates an armed stash."));
            case BAT -> guide(
                    "Ambush mobility fighter",
                    "Ceiling Hang empowers Side, Up, or Down. A confirmed Silent Descent rebounds Bat to safety.",
                    "Total Eclipse", "Turns the arena dark and attacks through a rapid echo-guided ambush.",
                    move("NEUTRAL", "Echo Lance", "Pings the lane with a precise echo projectile."),
                    move("SIDE", "Wingcut", "Cuts horizontally; launching from Ceiling Hang empowers the ambush."),
                    recovery("UP", "Moonrise", "Rises in an arc, with an empowered version available from Hang."),
                    move("DOWN", "Silent Descent", "Dives or enters Ceiling Hang; confirmed dives rebound upward."));
            case PELICAN -> guide(
                    "Cargo heavyweight",
                    "Pouch Snare loads cargo. Stocked Breakwater lands harder, while Thermal Sail can turn into a keel dive.",
                    "Maelstrom Gullet", "Opens a stage vortex, pulls enemies into the pouch zone, then erupts in a tidal launch.",
                    move("NEUTRAL", "Pouch Snare", "Catches the lane and loads cargo into Pelican's pouch."),
                    move("SIDE", "Breakwater Run", "A cargo-stocked run lands with heavyweight impact."),
                    recovery("UP", "Thermal Sail", "Sails upward and can transition into a downward keel dive."),
                    move("DOWN", "Bilge Dump", "Dumps stored force beneath Pelican to control nearby space."));
            case HEISENBIRD -> guide(
                    "Crystal resource specialist",
                    "Glass Cook creates the refill node. Crystal Cloud applies Brittle; the next real hit shatters it for bonus damage.",
                    "Blue Sky Catastrophe", "Crystallizes the battlefield before a massive blue fracture finishes the target.",
                    move("NEUTRAL", "Crystal Cloud", "Applies Brittle after Heisenbird has established crystal resources."),
                    move("SIDE", "Blue Rush", "A fueled rush adds stronger launch and can cash out Brittle."),
                    recovery("UP", "Crystal Column", "Creates a rising crystal route for recovery and vertical pressure."),
                    move("DOWN", "Glass Cook", "Places the node used to refuel the crystal meter."));
            case RAVEN -> guide(
                    "Route-setting assassin",
                    "Nevermore and Black Quill establish route points. Empowered Side or Up consumes the setup for a heavier cash-out.",
                    "The Unkindness", "Calls the full unkindness into a marked-target finishing sequence.",
                    move("NEUTRAL", "Black Quill", "Marks a target and prepares Raven's route payoff."),
                    move("SIDE", "Shadow Warp", "Warps through the lane; consuming a route adds the heavy follow-up."),
                    recovery("UP", "Murder Lift", "Lifts upward and snaps to an established route for its payoff."),
                    move("DOWN", "Nevermore", "Places a persistent route point for Raven's next special."));
            case GOOSE -> guide(
                    "Territory heavyweight",
                    "Nest Guard completes Territory and can counter nearby hits. A fully charged close Honk is the real launch threat.",
                    "The Whole Flock", "Calls a complete formation into a broad stage-clearing attack.",
                    move("NEUTRAL", "Threatening Honk", "Hold to charge; the close cone launches while the edge resets spacing."),
                    move("SIDE", "Bite and Barge", "Bites into a forceful horizontal shove."),
                    recovery("UP", "V-Formation Lift", "Calls formation support to carry Goose upward."),
                    move("DOWN", "Nest Guard", "Plants the nest, completes Territory, and counters hits near it."));
            case KIWI -> guide(
                    "Dependable grounded all-rounder",
                    "Kiwi has no resource or setup. Each direction provides one direct tool: peck, charge, recover, or stomp.",
                    "Midnight Stampede", "Chains three charges into one broad eruption across the ground.",
                    move("NEUTRAL", "Rapid Probe", "A fast sequence of direct close-range pecks."),
                    move("SIDE", "Burrow Charge", "Charges through the horizontal ground lane."),
                    recovery("UP", "Spring Kick", "Kicks upward as Kiwi's direct recovery option."),
                    move("DOWN", "Earth Stomp", "Stomps the ground and punishes opponents underneath or nearby."));
        };
    }

    static boolean hasCompleteRoster() {
        for (BirdGame3.BirdType type : BirdGame3.BirdType.values()) {
            Guide guide = forBird(type);
            if (guide.moves().size() != 4
                    || guide.moves().stream().anyMatch(move -> move.name().equals("-"))
                    || guide.ultimateName().isBlank()
                    || guide.mechanic().isBlank()) {
                return false;
            }
        }
        return true;
    }

    private static Guide guide(String role, String mechanic, String ultimateName, String ultimateDescription,
                               Move neutral, Move side, Move up, Move down) {
        return new Guide(role, mechanic, List.of(neutral, side, up, down), ultimateName, ultimateDescription);
    }

    private static Move move(String direction, String name, String description) {
        return new Move(direction, name, description, false);
    }

    private static Move recovery(String direction, String name, String description) {
        return new Move(direction, name, description, true);
    }

    private static String clean(String text) {
        return text == null ? "" : text.trim();
    }
}
