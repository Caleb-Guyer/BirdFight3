package com.example.birdgame3;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/** A seeded, eight-encounter solo run. Menu decisions never consume SimRng. */
final class FlockRunState {
    static final int ENCOUNTERS = 8;
    static final double MAX_HEALTH = 112;
    enum Phase { ROUTE, BATTLE, REWARD, WON, LOST }
    enum Route {
        SAFE("Sheltered passage", "One opponent. Win for one perk.", "#4FC3F7"),
        ELITE("Storm front", "A tougher opponent. Win for two perks and more score.", "#FF8A65"),
        REST("Recovery roost", "Restore 45 HP. Uses one encounter; no fight or perk. Two stops per run.", "#80CBC4"),
        BOSS("The final ascent", "Defeat the guardian to earn your medal.", "#FFD54F");
        final String title, description, color;
        Route(String title, String description, String color) {
            this.title = title; this.description = description; this.color = color;
        }
    }
    enum Perk {
        TALONS("Honed Talons", "+12% damage per rank.", "#FFAB91"),
        GUARD("Iron Feathers", "Reduce incoming damage: 13% / 23% / 31%.", "#90CAF9"),
        WIND("Tailwind", "+8% movement speed per rank.", "#80DEEA"),
        QUICKEN("Quick Talons", "+15% cooldown recovery per rank.", "#CE93D8"),
        SPIRIT("Inner Fire", "+20% ultimate charge per rank.", "#FFD54F"),
        MEDIC("Field Medicine", "Restore 8 extra HP per rank after every victory.", "#A5D6A7");
        final String title, description, color;
        Perk(String title, String description, String color) {
            this.title = title; this.description = description; this.color = color;
        }
    }
    enum Boss {
        PHOENIX("Ashen Phoenix"), PELICAN("Titan Pelican"), VULTURE("Carrion Regent");
        final String title;
        Boss(String title) { this.title = title; }
    }
    enum Medal { NONE, BRONZE, SILVER, GOLD }

    final long seed;
    final BirdGame3.BirdType bird;
    final String skinKey;
    private Phase phase = Phase.ROUTE;
    private int encounter;
    private double health = MAX_HEALTH;
    private int score;
    private int rests;
    private long ticks;
    private int picksRemaining;
    private int picksMade;
    private Route route;
    private final int[] ranks = new int[Perk.values().length];
    private final List<Route> path = new ArrayList<>();

    FlockRunState(long seed, BirdGame3.BirdType bird) {
        this(seed, bird, null);
    }

    FlockRunState(long seed, BirdGame3.BirdType bird, String skinKey) {
        this.seed = seed;
        this.bird = java.util.Objects.requireNonNull(bird);
        if (skinKey != null && !skinKey.matches("[A-Za-z0-9_-]{1,100}")) throw new IllegalArgumentException("Invalid skin key");
        this.skinKey = skinKey;
    }

    Phase phase() { return phase; }
    int encounter() { return encounter; }
    double health() { return health; }
    int score() { return score; }
    long ticks() { return ticks; }
    int rests() { return rests; }
    int picksRemaining() { return picksRemaining; }
    Route route() { return route; }
    int rank(Perk perk) { return ranks[perk.ordinal()]; }
    List<Route> path() { return List.copyOf(path); }
    boolean finished() { return phase == Phase.WON || phase == Phase.LOST; }
    Boss boss() { return Boss.values()[new Random(seed ^ 0x426F7373L).nextInt(Boss.values().length)]; }
    long encounterSeed() { return encounterSeed(route); }
    long encounterSeed(Route choice) { return seed ^ (0x9E3779B97F4A7C15L * (encounter + 1)) ^ (choice == null ? 0 : choice.ordinal() * 0x45D9F3BL); }

    List<Route> routes() {
        if (phase != Phase.ROUTE) return List.of();
        if (encounter == ENCOUNTERS - 1) return List.of(Route.BOSS);
        return encounter > 0 && rests < 2 ? List.of(Route.SAFE, Route.ELITE, Route.REST)
                : List.of(Route.SAFE, Route.ELITE);
    }

    boolean chooseRoute(Route choice) {
        if (!routes().contains(choice)) return false;
        route = choice;
        path.add(choice);
        if (choice == Route.REST) {
            health = Math.min(MAX_HEALTH, health + 45);
            rests++;
            encounter++;
        } else {
            phase = Phase.BATTLE;
        }
        return true;
    }

    boolean finishBattle(boolean won, double remainingHealth, long elapsedTicks) {
        if (phase != Phase.BATTLE) return false;
        elapsedTicks = Math.max(0, Math.min(240L * 60, elapsedTicks));
        ticks += elapsedTicks;
        health = Double.isFinite(remainingHealth) ? Math.max(0, Math.min(MAX_HEALTH, remainingHealth)) : 0;
        if (!won || health <= 0) {
            phase = Phase.LOST;
            return true;
        }
        score += (route == Route.BOSS ? 350 : route == Route.ELITE ? 250 : 100)
                + (int) Math.max(0, 60 - elapsedTicks / 60);
        encounter++;
        if (encounter == ENCOUNTERS) {
            score += (int) Math.round(health);
            phase = Phase.WON;
        } else {
            health = Math.min(MAX_HEALTH, health + 6 + 8 * rank(Perk.MEDIC));
            picksRemaining = route == Route.ELITE ? 2 : 1;
            picksMade = 0;
            phase = Phase.REWARD;
        }
        return true;
    }

    List<Perk> offeredPerks() {
        if (phase != Phase.REWARD) return List.of();
        List<Perk> choices = new ArrayList<>(Arrays.asList(Perk.values()));
        choices.removeIf(perk -> rank(perk) >= 3);
        Collections.shuffle(choices, new Random(seed ^ (0x5045524BL * encounter) ^ (picksMade * 7919L)));
        return List.copyOf(choices.subList(0, Math.min(3, choices.size())));
    }

    boolean choosePerk(Perk perk) {
        if (!offeredPerks().contains(perk)) return false;
        ranks[perk.ordinal()]++;
        picksMade++;
        if (--picksRemaining == 0) phase = Phase.ROUTE;
        return true;
    }

    void abandon() { if (!finished()) phase = Phase.LOST; }
    double damageMultiplier() { return 1 + 0.12 * rank(Perk.TALONS); }
    double incomingMultiplier() { return 1 / (1 + 0.15 * rank(Perk.GUARD)); }
    double speedMultiplier() { return 1 + 0.08 * rank(Perk.WIND); }
    double cooldownMultiplier() { return 1 + 0.15 * rank(Perk.QUICKEN); }
    double ultimateMultiplier() { return 1 + 0.20 * rank(Perk.SPIRIT); }
    Medal medal() {
        if (phase != Phase.WON) return Medal.NONE;
        return score >= 1550 ? Medal.GOLD : score >= 1100 ? Medal.SILVER : Medal.BRONZE;
    }

    String encode() {
        return "2;" + seed + ";" + bird.name() + ";" + phase + ";" + encounter + ";" + health + ";"
                + score + ";" + rests + ";" + ticks + ";" + picksRemaining + ";" + picksMade + ";"
                + (route == null ? "-" : route.name()) + ";"
                + Arrays.stream(ranks).mapToObj(Integer::toString).collect(java.util.stream.Collectors.joining(","))
                + ";" + path.stream().map(Enum::name).collect(java.util.stream.Collectors.joining(","))
                + ";" + (skinKey == null ? "" : skinKey);
    }

    static FlockRunState decode(String encoded) {
        if (encoded == null || encoded.isBlank() || encoded.length() > 2048) return null;
        try {
            String[] parts = encoded.split(";", -1);
            boolean legacy = parts.length == 14 && parts[0].equals("1");
            if (!legacy && (parts.length != 15 || !parts[0].equals("2"))) return null;
            FlockRunState run = new FlockRunState(Long.parseLong(parts[1]), BirdGame3.BirdType.valueOf(parts[2]),
                    legacy || parts[14].isEmpty() ? null : parts[14]);
            run.phase = Phase.valueOf(parts[3]);
            run.encounter = Integer.parseInt(parts[4]);
            run.health = Double.parseDouble(parts[5]);
            run.score = Integer.parseInt(parts[6]);
            run.rests = Integer.parseInt(parts[7]);
            run.ticks = Long.parseLong(parts[8]);
            run.picksRemaining = Integer.parseInt(parts[9]);
            run.picksMade = Integer.parseInt(parts[10]);
            run.route = parts[11].equals("-") ? null : Route.valueOf(parts[11]);
            String[] rankStrings = parts[12].split(",");
            if (rankStrings.length != run.ranks.length) return null;
            for (int i = 0; i < rankStrings.length; i++) {
                run.ranks[i] = Integer.parseInt(rankStrings[i]);
                if (run.ranks[i] < 0 || run.ranks[i] > 3) return null;
            }
            if (!parts[13].isBlank()) for (String choice : parts[13].split(",")) run.path.add(Route.valueOf(choice));
            for (int i = 0; i < run.path.size(); i++) {
                if ((i == ENCOUNTERS - 1) != (run.path.get(i) == Route.BOSS)
                        || (i == 0 && run.path.get(i) == Route.REST)) return null;
            }
            if (run.route != (run.path.isEmpty() ? null : run.path.getLast())) return null;
            if (run.encounter < 0 || run.encounter > ENCOUNTERS || !Double.isFinite(run.health)
                    || run.health < 0 || run.health > MAX_HEALTH || run.score < 0 || run.score > 5000
                    || run.rests < 0 || run.rests > 2 || run.ticks < 0 || run.ticks > ENCOUNTERS * 240L * 60
                    || run.picksRemaining < 0 || run.picksRemaining > 2 || run.picksMade < 0 || run.picksMade > 2
                    || run.path.size() > ENCOUNTERS || run.path.stream().filter(r -> r == Route.REST).count() != run.rests
                    || (run.phase == Phase.BATTLE && (run.route == null || run.route == Route.REST
                        || run.path.size() != run.encounter + 1))
                    || (run.phase != Phase.BATTLE && run.phase != Phase.LOST && run.path.size() != run.encounter)
                    || (run.phase == Phase.WON && run.encounter != ENCOUNTERS)
                    || (!run.finished() && (run.encounter >= ENCOUNTERS || run.health <= 0))
                    || (run.phase == Phase.REWARD && (run.picksRemaining == 0 || run.offeredPerks().isEmpty()))) return null;
            return run;
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }
}
