package com.example.birdgame3;

import javafx.geometry.Point2D;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.Set;

final class TowerDefenseMode {
    static final double MAP_WIDTH = 1440.0;
    static final double MAP_HEIGHT = 900.0;
    private static final double PATH_WIDTH = 86.0;
    private static final double BUILD_PADDING = 48.0;
    private static final double TOWER_TOUCH_RADIUS = 34.0;
    private static final double TOWER_SPACING = 78.0;
    private static final double PROJECTILE_RADIUS = 9.0;
    private static final Font HUD_FONT = Font.font("Consolas", FontWeight.BOLD, 20);
    private static final Font SMALL_FONT = Font.font("Consolas", FontWeight.BOLD, 14);
    private static final Font BADGE_FONT = Font.font("Arial Black", 12);
    private static final Font TITLE_FONT = Font.font("Arial Black", 20);
    private static final List<Point2D> PATH = List.of(
            new Point2D(-40, 724),
            new Point2D(196, 724),
            new Point2D(196, 214),
            new Point2D(432, 214),
            new Point2D(432, 484),
            new Point2D(748, 484),
            new Point2D(748, 160),
            new Point2D(1116, 160),
            new Point2D(1116, 666),
            new Point2D(1480, 666)
    );
    private static final double[][] TREE_CLUMPS = {
            {110, 118, 76}, {230, 118, 54}, {376, 94, 68}, {552, 96, 78}, {924, 88, 84}, {1220, 108, 72},
            {1290, 224, 76}, {1336, 430, 82}, {1240, 792, 96}, {962, 806, 88}, {660, 782, 90}, {380, 804, 82},
            {122, 812, 86}, {74, 508, 74}, {986, 358, 66}, {582, 332, 64}
    };
    private static final double[][] CLEARINGS = {
            {306, 608, 156}, {606, 642, 144}, {958, 550, 132}, {952, 314, 118}, {620, 212, 118}, {274, 340, 124}
    };
    private static final List<RoundDefinition> MASTER_ROUNDS = buildRounds();
    private static final List<String> ROUND_HINTS = buildRoundHints();

    enum Difficulty {
        EASY("Easy", 20, 145, 700, 1.00, Color.web("#78C850")),
        MEDIUM("Medium", 30, 110, 650, 1.12, Color.web("#FFB347")),
        HARD("Hard", 40, 78, 610, 1.24, Color.web("#EF5350"));

        final String label;
        final int roundCount;
        final int startingLives;
        final int startingCash;
        final double priceMultiplier;
        final Color accent;

        Difficulty(String label, int roundCount, int startingLives, int startingCash, double priceMultiplier, Color accent) {
            this.label = label;
            this.roundCount = roundCount;
            this.startingLives = startingLives;
            this.startingCash = startingCash;
            this.priceMultiplier = priceMultiplier;
            this.accent = accent;
        }

        int scalePrice(int baseCost) {
            return Math.max(5, ((int) Math.round(baseCost * priceMultiplier / 5.0)) * 5);
        }

        String description() {
            return roundCount + " rounds • " + startingLives + " lives • " + Math.round(priceMultiplier * 100.0) + "% prices";
        }
    }

    enum TowerBirdKind {
        PIGEON(
                BirdGame3.BirdType.PIGEON,
                "Pigeon",
                "Cheap all-rounder that scales into darts, spikes, and fan-club rushes.",
                "Generalist",
                Color.web("#D0D7E2"),
                220,
                new UpgradeTier[]{
                        new UpgradeTier("Skyline Reach", 140, "Extra range for safer lane coverage."),
                        new UpgradeTier("Fire Escape Watch", 260, "More range and can see hidden blights."),
                        new UpgradeTier("Can-Lid Catapult", 560, "Throws a spiked can lid that crushes lines."),
                        new UpgradeTier("Pavement Pounder", 1160, "City-grade rolling spikes demolish layered blights.")
                },
                new UpgradeTier[]{
                        new UpgradeTier("Beak-Honed Tips", 120, "Darts pierce a little deeper."),
                        new UpgradeTier("Razor Feathers", 220, "Even more pierce through blight shells."),
                        new UpgradeTier("Triple Toss", 460, "Throws three darts at once."),
                        new UpgradeTier("Rooftop Rally", 980, "Nearby pigeons periodically overclock into a frenzy.")
                }
        ),
        EAGLE(
                BirdGame3.BirdType.EAGLE,
                "Eagle",
                "Long-range sniper that shreds armor and punishes big shells.",
                "Sniper",
                Color.web("#9B5B4C"),
                380,
                new UpgradeTier[]{
                        new UpgradeTier("Steel Beak Rounds", 220, "Shots punch through armor."),
                        new UpgradeTier("Talon Caliber", 420, "Heavy rounds crack thick shells."),
                        new UpgradeTier("Royal Precision", 820, "Every few shots land a brutal precision hit."),
                        new UpgradeTier("Colossus Breaker", 1540, "Massive shells get marked, slowed, and broken.")
                },
                new UpgradeTier[]{
                        new UpgradeTier("Hair Trigger", 160, "Shorter time between shots."),
                        new UpgradeTier("Moon Goggles", 260, "Can see hidden blights."),
                        new UpgradeTier("Perchline Repeater", 640, "Rapid sniper fire for long rounds."),
                        new UpgradeTier("Grove Airdrop", 1160, "Auto-drops supply crates while keeping the rifle blazing.")
                }
        ),
        OPIUMBIRD(
                BirdGame3.BirdType.OPIUMBIRD,
                "Opium Bird",
                "Arcane caster that mixes lightning, fire, tornadoes, and phoenix shots.",
                "Magic",
                Color.web("#8956D9"),
                320,
                new UpgradeTier[]{
                        new UpgradeTier("Hazy Hex", 160, "Magic bolts hit harder and pierce deeper."),
                        new UpgradeTier("Purple Lightning", 320, "Every few casts chains a lightning strike."),
                        new UpgradeTier("Grove Whirl", 620, "Arcane gusts shove blights back down the trail."),
                        new UpgradeTier("Tempest Spiral", 1180, "Storm funnels keep dragging whole rushes backward.")
                },
                new UpgradeTier[]{
                        new UpgradeTier("Wildfire Orb", 180, "Adds explosive fireballs to the spell cycle."),
                        new UpgradeTier("Third Eye", 280, "Can see hidden blights."),
                        new UpgradeTier("Smoke Dragon", 640, "Rapid dragonfire scorches lanes."),
                        new UpgradeTier("Street Phoenix", 1220, "A fiery phoenix circles the perch and attacks on its own.")
                }
        ),
        BAT(
                BirdGame3.BirdType.BAT,
                "Bat",
                "Stealthy control bird that throws seeking sonic blades and supply-line sabotage.",
                "Control",
                Color.web("#5C6BC0"),
                340,
                new UpgradeTier[]{
                        new UpgradeTier("Echo Discipline", 140, "More range and attack speed."),
                        new UpgradeTier("Razor Echoes", 260, "Sonic blades pierce more blights."),
                        new UpgradeTier("Twin Toss", 480, "Throws two blades at a time."),
                        new UpgradeTier("Night Swarm", 1060, "Umbra volleys fill the lane with blades.")
                },
                new UpgradeTier[]{
                        new UpgradeTier("Homing Echo", 150, "Thrown blades curve toward targets."),
                        new UpgradeTier("Wing Clip", 240, "Hits can knock small blights backward."),
                        new UpgradeTier("Stun Pop", 560, "Bomb blades burst and stun clustered shells."),
                        new UpgradeTier("Midnight Blackout", 1020, "Periodically slows the entire Blight line.")
                }
        );

        final BirdGame3.BirdType birdType;
        final String label;
        final String summary;
        final String role;
        final Color color;
        final int cost;
        final UpgradeTier[] pathA;
        final UpgradeTier[] pathB;

        TowerBirdKind(BirdGame3.BirdType birdType,
                      String label,
                      String summary,
                      String role,
                      Color color,
                      int cost,
                      UpgradeTier[] pathA,
                      UpgradeTier[] pathB) {
            this.birdType = birdType;
            this.label = label;
            this.summary = summary;
            this.role = role;
            this.color = color;
            this.cost = cost;
            this.pathA = pathA;
            this.pathB = pathB;
        }
    }

    record UpgradeTier(String name, int cost, String description) {}

    record UpgradeOffer(String title, int cost, String description, boolean allowed, boolean affordable, int currentLevel) {}

    record PlacementPreview(boolean valid, String message) {}

    enum TargetMode {
        FIRST("FIRST", "Targets the enemy furthest along the path."),
        LAST("LAST", "Targets the enemy furthest from the exit."),
        STRONG("STRONG", "Targets the toughest enemy in range."),
        CLOSE("CLOSE", "Targets the nearest enemy in range.");

        final String label;
        final String description;

        TargetMode(String label, String description) {
            this.label = label;
            this.description = description;
        }

        TargetMode next() {
            return values()[(ordinal() + 1) % values().length];
        }
    }

    record ToastMessage(String title, String body, double durationSeconds) {}

    private enum EnemyKind {
        BLIGHT("Blight", 1, 78, 1, 1, 0, 12, Color.web("#A9D665"), Color.web("#DFF3A3"), null, 0, false, false, false),
        SWIFT_BLIGHT("Swift Blight", 1, 126, 1, 1, 0, 11, Color.web("#7BCB63"), Color.web("#C8F79C"), BLIGHT, 1, false, true, false),
        SHADE_BLIGHT("Shade Blight", 1, 90, 1, 1, 0, 11, Color.web("#7A9B6A"), Color.web("#D8E9BE"), BLIGHT, 1, true, false, false),
        BRAMBLE_BLIGHT("Bramble Blight", 2, 84, 1, 2, 1, 15, Color.web("#58752F"), Color.web("#D3B96A"), BLIGHT, 1, false, false, true),
        RESIN_BLIGHT("Resin Blight", 4, 68, 1, 3, 2, 18, Color.web("#8A632C"), Color.web("#F2C26B"), BRAMBLE_BLIGHT, 2, false, false, true),
        ROOT_GOLEM("Root Golem", 10, 46, 1, 7, 4, 26, Color.web("#5D4037"), Color.web("#FFE082"), RESIN_BLIGHT, 2, false, false, true),
        IRONBARK_GOLEM("Ironbark Golem", 18, 36, 1, 12, 6, 31, Color.web("#4C302A"), Color.web("#E6B661"), ROOT_GOLEM, 2, false, false, true),
        HEARTWOOD_COLOSSUS("Heartwood Colossus", 32, 26, 1, 18, 8, 36, Color.web("#3B261F"), Color.web("#FFCC80"), IRONBARK_GOLEM, 2, false, false, true);

        final String label;
        final double health;
        final double speed;
        final int cashReward;
        final int leakDamage;
        final double armor;
        final double radius;
        final Color body;
        final Color accent;
        final EnemyKind childKind;
        final int childCount;
        final boolean hidden;
        final boolean speedBurst;
        final boolean armored;

        EnemyKind(String label, double health, double speed, int cashReward, int leakDamage, double armor, double radius,
                  Color body, Color accent, EnemyKind childKind, int childCount, boolean hidden, boolean speedBurst, boolean armored) {
            this.label = label;
            this.health = health;
            this.speed = speed;
            this.cashReward = cashReward;
            this.leakDamage = leakDamage;
            this.armor = armor;
            this.radius = radius;
            this.body = body;
            this.accent = accent;
            this.childKind = childKind;
            this.childCount = childCount;
            this.hidden = hidden;
            this.speedBurst = speedBurst;
            this.armored = armored;
        }
    }

    private record SpawnGroup(EnemyKind kind, int count, double intervalSeconds, double startDelaySeconds) {}

    private record RoundDefinition(List<SpawnGroup> groups, int clearBonus) {}

    private static final class PendingSpawn {
        final EnemyKind kind;
        final double spawnAt;

        PendingSpawn(EnemyKind kind, double spawnAt) {
            this.kind = kind;
            this.spawnAt = spawnAt;
        }
    }

    private static final class Tower {
        final TowerBirdKind kind;
        final double x;
        final double y;
        final int[] upgrades = new int[2];
        final Bird sprite;
        int investedCash;
        double cooldown;
        double utilityCooldown;
        double companionTime;
        double attackVisualTime;
        double lastAimDx;
        int attackCycle;
        TargetMode targetMode;

        Tower(TowerBirdKind kind, double x, double y, int investedCash, BirdGame3 game) {
            this.kind = kind;
            this.x = x;
            this.y = y;
            this.sprite = new Bird(0, kind.birdType, 0, game);
            this.sprite.suppressSelectEffects = true;
            this.investedCash = investedCash;
            this.cooldown = 0.0;
            this.utilityCooldown = 0.0;
            this.companionTime = 0.0;
            this.attackVisualTime = 0.0;
            this.lastAimDx = 1.0;
            this.attackCycle = 0;
            this.targetMode = TargetMode.FIRST;
        }

        int investedCash() {
            return investedCash;
        }

        String badgeText() {
            return upgrades[0] + "-" + upgrades[1];
        }
    }

    private static final class Enemy {
        final EnemyKind kind;
        final int id;
        final int cashAward;
        double x;
        double y;
        double health;
        int nextWaypointIndex = 1;
        double forcedSlowMultiplier = 1.0;
        double forcedSlowTime;
        double ambientSlowMultiplier = 1.0;
        double vulnerabilityMultiplier = 1.0;
        double forcedVulnerabilityMultiplier = 1.0;
        double forcedVulnerabilityTime;
        double speedBuffMultiplier = 1.0;
        double abilityCooldown;
        double abilityTime;
        boolean deathSpawnTriggered;

        Enemy(EnemyKind kind, int id) {
            this(kind, id, kind.cashReward);
        }

        Enemy(EnemyKind kind, int id, int cashAward) {
            this.kind = kind;
            this.id = id;
            this.cashAward = cashAward;
            this.health = kind.health;
            Point2D spawn = PATH.getFirst();
            this.x = spawn.getX();
            this.y = spawn.getY();
            this.abilityCooldown = switch (kind) {
                case SWIFT_BLIGHT -> 1.2 + (id % 4) * 0.18;
                case HEARTWOOD_COLOSSUS -> 4.6 + (id % 3) * 0.28;
                default -> 0.0;
            };
        }

        double maxHealth() {
            return kind.health;
        }
    }

    private static final class Projectile {
        double x;
        double y;
        double vx;
        double vy;
        double damage;
        double radius;
        double travelLeft;
        double splashRadius;
        int pierceLeft;
        boolean armorPierce;
        final TowerBirdKind sourceKind;
        final Color color;
        final Color accentColor;
        final Set<Integer> hitEnemyIds = new HashSet<>();
        int chainCount;
        double chainDamageMultiplier;
        double onHitSlowMultiplier = 1.0;
        double onHitSlowTime;
        double onHitKnockback;
        double onHitVulnerabilityMultiplier = 1.0;
        double onHitVulnerabilityTime;
        boolean seeking;
        boolean seesHidden;
        double turnRate;
        double cloudRadius;
        double cloudDamagePerSecond;
        double cloudSlowMultiplier = 1.0;
        double cloudVulnerabilityMultiplier = 1.0;
        double cloudDuration;

        Projectile(double x, double y, double vx, double vy, double damage, double radius, double travelLeft,
                   double splashRadius, int pierceLeft, boolean armorPierce,
                   TowerBirdKind sourceKind, Color color, Color accentColor) {
            this.x = x;
            this.y = y;
            this.vx = vx;
            this.vy = vy;
            this.damage = damage;
            this.radius = radius;
            this.travelLeft = travelLeft;
            this.splashRadius = splashRadius;
            this.pierceLeft = pierceLeft;
            this.armorPierce = armorPierce;
            this.sourceKind = sourceKind;
            this.color = color;
            this.accentColor = accentColor;
            this.chainDamageMultiplier = 0.62;
        }
    }

    private static final class Cloud {
        final double x;
        final double y;
        final double radius;
        final double damagePerSecond;
        final double slowMultiplier;
        final double vulnerabilityMultiplier;
        double timeLeft;

        Cloud(double x, double y, double radius, double damagePerSecond, double slowMultiplier,
              double vulnerabilityMultiplier, double timeLeft) {
            this.x = x;
            this.y = y;
            this.radius = radius;
            this.damagePerSecond = damagePerSecond;
            this.slowMultiplier = slowMultiplier;
            this.vulnerabilityMultiplier = vulnerabilityMultiplier;
            this.timeLeft = timeLeft;
        }
    }

    private static final class Tracer {
        final double x1;
        final double y1;
        final double x2;
        final double y2;
        final Color color;
        final double width;
        final double totalTime;
        double timeLeft;

        Tracer(double x1, double y1, double x2, double y2, Color color, double width, double timeLeft) {
            this.x1 = x1;
            this.y1 = y1;
            this.x2 = x2;
            this.y2 = y2;
            this.color = color;
            this.width = width;
            this.totalTime = timeLeft;
            this.timeLeft = timeLeft;
        }
    }

    private static final class Pulse {
        final double x;
        final double y;
        final double maxRadius;
        final Color color;
        final double totalTime;
        double timeLeft;

        Pulse(double x, double y, double maxRadius, Color color, double timeLeft) {
            this.x = x;
            this.y = y;
            this.maxRadius = maxRadius;
            this.color = color;
            this.totalTime = timeLeft;
            this.timeLeft = timeLeft;
        }
    }

    private final BirdGame3 game;
    private final Difficulty difficulty;
    private final List<RoundDefinition> rounds;
    private final Random random = new Random(0xB17DF013L);
    private final List<Tower> towers = new ArrayList<>();
    private final List<Enemy> enemies = new ArrayList<>();
    private final List<Projectile> projectiles = new ArrayList<>();
    private final List<Cloud> clouds = new ArrayList<>();
    private final List<Tracer> tracers = new ArrayList<>();
    private final List<Pulse> pulses = new ArrayList<>();
    private final ArrayDeque<PendingSpawn> pendingSpawns = new ArrayDeque<>();
    private final ArrayDeque<ToastMessage> toastQueue = new ArrayDeque<>();
    private Tower selectedTower;
    private TowerBirdKind buildSelection;
    private String banner;
    private double bannerTime;
    private ToastMessage activeToast;
    private double activeToastTime;
    private int cash;
    private int lives;
    private int nextRoundNumber;
    private double roundClock;
    private double speedMultiplier;
    private double animationClock;
    private int nextEnemyId;
    private int roundCashStart;
    private boolean roundActive;
    private boolean victory;
    private boolean gameOver;

    TowerDefenseMode(BirdGame3 game, Difficulty difficulty) {
        this.game = game;
        this.difficulty = difficulty == null ? Difficulty.MEDIUM : difficulty;
        this.rounds = MASTER_ROUNDS.subList(0, this.difficulty.roundCount);
        reset();
    }

    void reset() {
        towers.clear();
        enemies.clear();
        projectiles.clear();
        clouds.clear();
        tracers.clear();
        pulses.clear();
        pendingSpawns.clear();
        toastQueue.clear();
        selectedTower = null;
        buildSelection = null;
        banner = "";
        bannerTime = 0.0;
        activeToast = null;
        activeToastTime = 0.0;
        cash = difficulty.startingCash;
        lives = difficulty.startingLives;
        nextRoundNumber = 1;
        roundClock = 0.0;
        speedMultiplier = 1.0;
        animationClock = 0.0;
        nextEnemyId = 1;
        roundCashStart = cash;
        roundActive = false;
        victory = false;
        gameOver = false;
        queueRoundToast("BIG FOREST", difficulty.label.toUpperCase(Locale.ROOT) + "  •  " + rounds.size()
                + " rounds. Build before round 1 starts.", 5.2);
    }

    List<TowerBirdKind> shopBirds() {
        return List.of(TowerBirdKind.values());
    }

    double speedMultiplier() {
        return speedMultiplier;
    }

    Difficulty difficulty() {
        return difficulty;
    }

    void cycleSpeedMultiplier() {
        speedMultiplier = switch ((int) speedMultiplier) {
            case 1 -> 2.0;
            case 2 -> 3.0;
            default -> 1.0;
        };
        showBanner("Speed set to " + (int) speedMultiplier + "x.", 2.0);
    }

    int cash() {
        return cash;
    }

    int lives() {
        return lives;
    }

    int nextRoundNumber() {
        return Math.clamp(nextRoundNumber, 1, rounds.size());
    }

    int maxRounds() {
        return rounds.size();
    }

    int enemyCount() {
        return enemies.size() + pendingSpawns.size();
    }

    int towerCount() {
        return towers.size();
    }

    boolean roundActive() {
        return roundActive;
    }

    boolean victory() {
        return victory;
    }

    boolean gameOver() {
        return gameOver;
    }

    String banner() {
        return bannerTime > 0.0 ? banner : "";
    }

    TowerBirdKind buildSelection() {
        return buildSelection;
    }

    boolean hasSelectedTower() {
        return selectedTower != null;
    }

    boolean shouldShowRoostPanel() {
        return selectedTower == null;
    }

    boolean canAfford(TowerBirdKind kind) {
        return kind != null && cash >= shopCost(kind);
    }

    int shopCost(TowerBirdKind kind) {
        return kind == null ? 0 : difficulty.scalePrice(kind.cost);
    }

    TargetMode selectedTowerTargetMode() {
        return selectedTower == null ? TargetMode.FIRST : selectedTower.targetMode;
    }

    String selectedTowerTargetingSummary() {
        TargetMode mode = selectedTowerTargetMode();
        return mode.description;
    }

    boolean cycleSelectedTowerTargetMode() {
        if (selectedTower == null) {
            return false;
        }
        selectedTower.targetMode = selectedTower.targetMode.next();
        showBanner(selectedTower.kind.label + " targeting: " + selectedTower.targetMode.label + ".", 1.8);
        return true;
    }

    TowerBirdKind selectedTowerKind() {
        return selectedTower == null ? null : selectedTower.kind;
    }

    int selectedTowerSellValue() {
        return selectedTower == null ? 0 : sellValue(selectedTower);
    }

    String toastTitle() {
        return activeToast == null ? "" : activeToast.title();
    }

    String toastBody() {
        return activeToast == null ? "" : activeToast.body();
    }

    boolean toastVisible() {
        return activeToast != null && activeToastTime > 0.0;
    }

    void renderSelectedTowerPreview(GraphicsContext g, double width, double height) {
        g.clearRect(0, 0, width, height);
        g.setFill(Color.web("#0B1510"));
        g.fillRoundRect(0, 0, width, height, 22, 22);
        g.setStroke(Color.web("#274B39"));
        g.setLineWidth(2.0);
        g.strokeRoundRect(1, 1, Math.max(0.0, width - 2), Math.max(0.0, height - 2), 22, 22);
        if (selectedTower == null) {
            return;
        }
        Tower preview = new Tower(selectedTower.kind, width * 0.5, height * 0.60, selectedTower.investedCash, game);
        preview.upgrades[0] = selectedTower.upgrades[0];
        preview.upgrades[1] = selectedTower.upgrades[1];
        preview.attackVisualTime = selectedTower.attackVisualTime;
        preview.lastAimDx = selectedTower.lastAimDx;
        preview.targetMode = selectedTower.targetMode;
        drawTower(g, preview, false);
    }

    void selectBuild(TowerBirdKind kind) {
        if (kind == null) {
            buildSelection = null;
            return;
        }
        int cost = shopCost(kind);
        if (cash < cost) {
            showBanner("Need $" + cost + " for " + kind.label + ".", 2.0);
            return;
        }
        buildSelection = kind;
        selectedTower = null;
        showBanner(kind.label + " selected. Click grass to place it.", 2.4);
    }

    void clearBuildSelection() {
        buildSelection = null;
    }

    PlacementPreview placementPreview(double x, double y) {
        if (buildSelection == null) {
            return new PlacementPreview(false, "");
        }
        String issue = placementIssue(x, y);
        return new PlacementPreview(issue == null, issue == null ? "Place " + buildSelection.label : issue);
    }

    boolean placeSelectedTower(double x, double y) {
        if (buildSelection == null) {
            return false;
        }
        String issue = placementIssue(x, y);
        if (issue != null) {
            showBanner(issue, 2.0);
            return false;
        }
        int cost = shopCost(buildSelection);
        Tower tower = new Tower(buildSelection, x, y, cost, game);
        towers.add(tower);
        selectedTower = tower;
        cash -= cost;
        showBanner(buildSelection.label + " deployed.", 1.8);
        buildSelection = null;
        return true;
    }

    void selectTowerAt(double x, double y) {
        Tower best = null;
        double bestDistance = TOWER_TOUCH_RADIUS * TOWER_TOUCH_RADIUS;
        for (Tower tower : towers) {
            double dx = tower.x - x;
            double dy = tower.y - y;
            double distance = dx * dx + dy * dy;
            if (distance <= bestDistance) {
                bestDistance = distance;
                best = tower;
            }
        }
        selectedTower = best;
        if (best == null) {
            buildSelection = null;
        }
    }

    void deselectTower() {
        selectedTower = null;
    }

    boolean sellSelectedTower() {
        if (selectedTower == null) {
            return false;
        }
        cash += sellValue(selectedTower);
        towers.remove(selectedTower);
        showBanner(selectedTower.kind.label + " sold back to the grove.", 2.0);
        selectedTower = null;
        return true;
    }

    String selectedTowerTitle() {
        if (selectedTower == null) {
            return "NO BIRD SELECTED";
        }
        return selectedTower.kind.label.toUpperCase(Locale.ROOT) + "  " + selectedTower.badgeText();
    }

    String selectedTowerBody() {
        if (selectedTower == null) {
            return "Buy a bird and place it on open grass.";
        }
        return selectedTower.kind.role + " tower.\n"
                + selectedTower.kind.summary;
    }

    UpgradeOffer selectedUpgradeOffer(int pathIndex) {
        if (selectedTower == null || (pathIndex != 0 && pathIndex != 1)) {
            return new UpgradeOffer("No bird selected", 0, "Select a bird on the map.", false, false, 0);
        }
        UpgradeTier[] path = pathIndex == 0 ? selectedTower.kind.pathA : selectedTower.kind.pathB;
        int current = selectedTower.upgrades[pathIndex];
        if (current >= path.length) {
            return new UpgradeOffer(pathIndex == 0 ? "Top Path Maxed" : "Bottom Path Maxed", 0, "This path is already fully upgraded.", false, false, current);
        }
        UpgradeTier tier = path[current];
        int scaledCost = difficulty.scalePrice(tier.cost());
        boolean allowed = canUpgrade(selectedTower, pathIndex);
        boolean affordable = cash >= scaledCost;
        return new UpgradeOffer(tier.name(), scaledCost, tier.description(), allowed, affordable, current);
    }

    boolean upgradeSelectedTower(int pathIndex) {
        if (selectedTower == null || (pathIndex != 0 && pathIndex != 1)) {
            return false;
        }
        if (!canUpgrade(selectedTower, pathIndex)) {
            showBanner("Only one path can go beyond tier 2 on a bird.", 2.2);
            return false;
        }
        UpgradeTier[] path = pathIndex == 0 ? selectedTower.kind.pathA : selectedTower.kind.pathB;
        int current = selectedTower.upgrades[pathIndex];
        if (current >= path.length) {
            return false;
        }
        UpgradeTier tier = path[current];
        int scaledCost = difficulty.scalePrice(tier.cost());
        if (cash < scaledCost) {
            showBanner("Need $" + scaledCost + " for " + tier.name() + ".", 2.0);
            return false;
        }
        cash -= scaledCost;
        selectedTower.upgrades[pathIndex]++;
        selectedTower.investedCash += scaledCost;
        showBanner(selectedTower.kind.label + " learned " + tier.name() + ".", 2.0);
        return true;
    }

    boolean startRound() {
        if (roundActive || victory || gameOver || nextRoundNumber > rounds.size()) {
            return false;
        }
        RoundDefinition definition = rounds.get(nextRoundNumber - 1);
        pendingSpawns.clear();
        roundClock = 0.0;
        roundCashStart = cash;
        for (SpawnGroup group : definition.groups()) {
            for (int i = 0; i < group.count(); i++) {
                pendingSpawns.add(new PendingSpawn(group.kind(), group.startDelaySeconds() + group.intervalSeconds() * i));
            }
        }
        List<PendingSpawn> ordered = new ArrayList<>(pendingSpawns);
        ordered.sort(Comparator.comparingDouble(spawn -> spawn.spawnAt));
        pendingSpawns.clear();
        pendingSpawns.addAll(ordered);
        roundActive = true;
        showBanner("Round " + nextRoundNumber + " started.", 1.6);
        return true;
    }

    void setCash(int value) {
        cash = Math.max(0, value);
        showBanner("Cash set to $" + cash + ".", 2.0);
    }

    void setLives(int value) {
        lives = Math.max(0, value);
        if (lives <= 0) {
            gameOver = true;
            roundActive = false;
        }
        showBanner("Lives set to " + lives + ".", 2.0);
    }

    void jumpToRound(int roundNumber) {
        int clamped = Math.clamp(roundNumber, 1, rounds.size());
        nextRoundNumber = clamped;
        roundActive = false;
        roundClock = 0.0;
        pendingSpawns.clear();
        enemies.clear();
        projectiles.clear();
        clouds.clear();
        tracers.clear();
        pulses.clear();
        victory = false;
        if (lives <= 0) {
            lives = 1;
        }
        gameOver = false;
        showBanner("Jumped to round " + nextRoundNumber + ". Press Start Round to deploy it.", 2.6);
    }

    void clearWaveForDebug() {
        pendingSpawns.clear();
        enemies.clear();
        projectiles.clear();
        clouds.clear();
        roundActive = false;
        showBanner("Wave cleared.", 1.8);
    }

    void update(double dtSeconds) {
        if (dtSeconds <= 0.0) {
            return;
        }
        animationClock += dtSeconds;
        if (bannerTime > 0.0) {
            bannerTime = Math.max(0.0, bannerTime - dtSeconds);
        }
        if (activeToastTime > 0.0) {
            activeToastTime = Math.max(0.0, activeToastTime - dtSeconds);
            if (activeToastTime <= 0.0) {
                activeToast = null;
                showNextToastIfNeeded();
            }
        } else {
            showNextToastIfNeeded();
        }
        if (victory || gameOver) {
            return;
        }

        double scaledTime = Math.min(0.2, dtSeconds) * speedMultiplier;
        while (scaledTime > 0.0) {
            double step = Math.min(0.025, scaledTime);
            updateStep(step);
            scaledTime -= step;
        }
    }

    void render(GraphicsContext g, double mouseX, double mouseY) {
        g.clearRect(0, 0, MAP_WIDTH, MAP_HEIGHT);
        drawBackground(g);
        drawPath(g);
        drawDecor(g);
        drawClouds(g);
        drawPulses(g);
        drawSelectionRanges(g, mouseX, mouseY);
        drawProjectiles(g);
        drawTracers(g);
        drawEnemies(g);
        drawTowers(g);
        drawMapHud(g);
    }

    private void updateStep(double dt) {
        for (Tower tower : towers) {
            tower.attackVisualTime = Math.max(0.0, tower.attackVisualTime - dt);
        }
        if (roundActive) {
            roundClock += dt;
            while (!pendingSpawns.isEmpty() && pendingSpawns.peekFirst().spawnAt <= roundClock) {
                spawnEnemy(pendingSpawns.removeFirst().kind);
            }
        }

        for (Enemy enemy : enemies) {
            enemy.ambientSlowMultiplier = 1.0;
            enemy.vulnerabilityMultiplier = enemy.forcedVulnerabilityTime > 0.0 ? enemy.forcedVulnerabilityMultiplier : 1.0;
            if (enemy.forcedSlowTime > 0.0) {
                enemy.forcedSlowTime = Math.max(0.0, enemy.forcedSlowTime - dt);
                if (enemy.forcedSlowTime <= 0.0) {
                    enemy.forcedSlowMultiplier = 1.0;
                }
            }
            if (enemy.forcedVulnerabilityTime > 0.0) {
                enemy.forcedVulnerabilityTime = Math.max(0.0, enemy.forcedVulnerabilityTime - dt);
                if (enemy.forcedVulnerabilityTime <= 0.0) {
                    enemy.forcedVulnerabilityMultiplier = 1.0;
                    enemy.vulnerabilityMultiplier = 1.0;
                }
            }
        }

        updateClouds(dt);
        updateEnemyAuras(dt);
        updateTowerPassives(dt);
        updateTowerAttacks(dt);
        updateProjectiles(dt);
        removeDefeatedEnemies();
        updateEnemyMovement(dt);
        removeDefeatedEnemies();
        updateEffects(dt);
        resolveRoundEnd();
    }

    private void updateClouds(double dt) {
        for (int i = clouds.size() - 1; i >= 0; i--) {
            Cloud cloud = clouds.get(i);
            cloud.timeLeft -= dt;
            if (cloud.timeLeft <= 0.0) {
                clouds.remove(i);
                continue;
            }
            double radiusSq = cloud.radius * cloud.radius;
            for (Enemy enemy : enemies) {
                double dx = enemy.x - cloud.x;
                double dy = enemy.y - cloud.y;
                if (dx * dx + dy * dy > radiusSq) {
                    continue;
                }
                enemy.health -= cloud.damagePerSecond * dt;
                enemy.ambientSlowMultiplier = Math.min(enemy.ambientSlowMultiplier, cloud.slowMultiplier);
                enemy.vulnerabilityMultiplier = Math.max(enemy.vulnerabilityMultiplier, cloud.vulnerabilityMultiplier);
            }
        }
    }

    private void updateEnemyAuras(double dt) {
        for (Enemy enemy : enemies) {
            enemy.speedBuffMultiplier = 1.0;
            switch (enemy.kind) {
                case SWIFT_BLIGHT -> {
                    enemy.abilityTime = Math.max(0.0, enemy.abilityTime - dt);
                    enemy.abilityCooldown -= dt;
                    if (enemy.abilityCooldown <= 0.0) {
                        enemy.abilityCooldown += 2.2;
                        enemy.abilityTime = 0.52;
                        pulses.add(new Pulse(enemy.x, enemy.y, 22, Color.web("#A5D66B", 0.46), 0.18));
                    }
                }
                case IRONBARK_GOLEM -> {
                    enemy.abilityTime = Math.max(0.0, enemy.abilityTime - dt);
                    if (enemy.health <= enemy.maxHealth() * 0.45) {
                        enemy.abilityTime = 0.3;
                    }
                }
                case HEARTWOOD_COLOSSUS -> {
                    enemy.abilityTime = Math.max(0.0, enemy.abilityTime - dt);
                    enemy.abilityCooldown -= dt;
                    if (enemy.abilityCooldown <= 0.0) {
                        enemy.abilityCooldown += 5.0;
                        enemy.abilityTime = 0.8;
                        pulses.add(new Pulse(enemy.x, enemy.y, 54, Color.web("#FFCC80", 0.38), 0.30));
                    }
                }
                default -> {
                }
            }
        }
        for (Enemy source : enemies) {
            if (source.kind != EnemyKind.HEARTWOOD_COLOSSUS || source.abilityTime <= 0.0) {
                continue;
            }
            for (Enemy enemy : enemies) {
                if (enemy == source) {
                    continue;
                }
                if (currentDistanceSq(source.x, source.y, enemy.x, enemy.y) <= 170 * 170) {
                    enemy.speedBuffMultiplier = Math.max(enemy.speedBuffMultiplier, 1.18);
                }
            }
        }
    }

    private void updateTowerPassives(double dt) {
        for (Tower tower : towers) {
            tower.companionTime = Math.max(0.0, tower.companionTime - dt);
            tower.utilityCooldown = Math.max(0.0, tower.utilityCooldown - dt);
            switch (tower.kind) {
                case PIGEON -> {
                    if (tower.upgrades[1] >= 4 && tower.utilityCooldown <= 0.0) {
                        tower.utilityCooldown = 8.5;
                        tower.companionTime = 3.4;
                        pulses.add(new Pulse(tower.x, tower.y, 86, Color.web("#CFD8DC", 0.52), 0.34));
                    }
                }
                case EAGLE -> {
                    if (tower.upgrades[1] >= 4 && tower.utilityCooldown <= 0.0) {
                        tower.utilityCooldown = 10.5;
                        int payout = 30 + tower.upgrades[1] * 10;
                        cash += payout;
                        pulses.add(new Pulse(tower.x, tower.y, 52, Color.web("#FFE082", 0.62), 0.26));
                        showBanner("Supply Drop: +$" + payout, 1.4);
                    }
                }
                case OPIUMBIRD -> {
                    if (tower.upgrades[1] >= 4) {
                        tower.companionTime = Math.max(tower.companionTime, 0.45);
                    }
                    if (tower.upgrades[1] >= 4 && tower.utilityCooldown <= 0.0) {
                        tower.utilityCooldown = 3.6;
                        pulses.add(new Pulse(tower.x, tower.y, 96, Color.web("#FFB74D", 0.52), 0.34));
                    }
                }
                case BAT -> {
                    if (tower.upgrades[1] >= 4 && tower.utilityCooldown <= 0.0) {
                        tower.utilityCooldown = 11.0;
                        for (Enemy enemy : enemies) {
                            enemy.forcedSlowMultiplier = Math.min(enemy.forcedSlowMultiplier, 0.54);
                            enemy.forcedSlowTime = Math.max(enemy.forcedSlowTime, 2.8);
                        }
                        pulses.add(new Pulse(tower.x, tower.y, 280, Color.web("#B39DDB", 0.48), 0.44));
                    }
                }
            }
        }
    }

    private void updateTowerAttacks(double dt) {
        for (Tower tower : towers) {
            double supportMultiplier = supportAttackMultiplier(tower);
            tower.cooldown = Math.max(-1.0, tower.cooldown - dt * supportMultiplier);
            Enemy target = chooseTarget(tower, towerRange(tower));
            if (target != null) {
                tower.lastAimDx = target.x - tower.x;
            }
            if (tower.cooldown > 0.0) {
                continue;
            }
            if (target == null) {
                continue;
            }
            switch (tower.kind) {
                case PIGEON -> firePigeonVolley(tower, target);
                case EAGLE -> fireEagleShot(tower, target);
                case OPIUMBIRD -> fireOpiumSpell(tower, target);
                case BAT -> fireBatPulse(tower, target);
            }
            tower.cooldown += towerAttackInterval(tower);
        }
    }

    private void updateProjectiles(double dt) {
        for (int i = projectiles.size() - 1; i >= 0; i--) {
            Projectile projectile = projectiles.get(i);
            if (projectile.seeking) {
                retargetProjectile(projectile, dt);
            }
            projectile.x += projectile.vx * dt;
            projectile.y += projectile.vy * dt;
            projectile.travelLeft -= Math.hypot(projectile.vx * dt, projectile.vy * dt);

            boolean expired = projectile.travelLeft <= 0.0;
            boolean remove = projectile.x < -50
                    || projectile.x > MAP_WIDTH + 50
                    || projectile.y < -50
                    || projectile.y > MAP_HEIGHT + 50;

            for (Enemy enemy : enemies) {
                if (remove || projectile.hitEnemyIds.contains(enemy.id)) {
                    continue;
                }
                double dx = enemy.x - projectile.x;
                double dy = enemy.y - projectile.y;
                double hitRadius = enemy.kind.radius + projectile.radius;
                if (dx * dx + dy * dy > hitRadius * hitRadius) {
                    continue;
                }
                projectile.hitEnemyIds.add(enemy.id);
                remove = handleProjectileHit(projectile, enemy) || remove;
            }

            if (!remove && expired) {
                remove = true;
            }
            if (remove) {
                projectiles.remove(i);
            }
        }
    }

    private void retargetProjectile(Projectile projectile, double dt) {
        Enemy best = null;
        double bestScore = Double.NEGATIVE_INFINITY;
        for (Enemy enemy : enemies) {
            if (projectile.hitEnemyIds.contains(enemy.id)) {
                continue;
            }
            double score = enemyProgress(enemy);
            if (score > bestScore) {
                bestScore = score;
                best = enemy;
            }
        }
        if (best == null) {
            return;
        }
        double speed = Math.max(1.0, Math.hypot(projectile.vx, projectile.vy));
        double desiredX = best.x - projectile.x;
        double desiredY = best.y - projectile.y;
        double desiredMag = Math.max(0.001, Math.hypot(desiredX, desiredY));
        desiredX = desiredX / desiredMag * speed;
        desiredY = desiredY / desiredMag * speed;
        double blend = Math.clamp(projectile.turnRate * dt, 0.0, 1.0);
        projectile.vx = projectile.vx + (desiredX - projectile.vx) * blend;
        projectile.vy = projectile.vy + (desiredY - projectile.vy) * blend;
    }

    private void updateEnemyMovement(double dt) {
        for (int i = enemies.size() - 1; i >= 0; i--) {
            Enemy enemy = enemies.get(i);
            if (enemy.nextWaypointIndex >= PATH.size()) {
                enemies.remove(i);
                lives = Math.max(0, lives - enemy.kind.leakDamage);
                if (lives <= 0) {
                    gameOver = true;
                    roundActive = false;
                    showBanner("Big Forest fell to the Blight.", 99.0);
                }
                continue;
            }
            Point2D target = PATH.get(enemy.nextWaypointIndex);
            double dx = target.getX() - enemy.x;
            double dy = target.getY() - enemy.y;
            double distance = Math.hypot(dx, dy);
            if (distance < 0.001) {
                enemy.nextWaypointIndex++;
                continue;
            }
            double slow = Math.min(enemy.ambientSlowMultiplier, enemy.forcedSlowTime > 0.0 ? enemy.forcedSlowMultiplier : 1.0);
            if (enemy.kind == EnemyKind.IRONBARK_GOLEM) {
                slow = Math.max(slow, 0.78);
            }
            if (enemy.kind == EnemyKind.HEARTWOOD_COLOSSUS && enemy.health <= enemy.maxHealth() * 0.5) {
                slow = Math.max(slow, 0.72);
            }
            double speed = enemy.kind.speed * slow * enemy.speedBuffMultiplier;
            switch (enemy.kind) {
                case SWIFT_BLIGHT -> {
                    if (enemy.abilityTime > 0.0) {
                        speed *= 1.80;
                    }
                }
                case IRONBARK_GOLEM -> {
                    if (enemy.abilityTime > 0.0) {
                        speed *= 1.08;
                    }
                }
                case HEARTWOOD_COLOSSUS -> {
                    if (enemy.health <= enemy.maxHealth() * 0.5) {
                        speed *= 1.18;
                    }
                    if (enemy.abilityTime > 0.0) {
                        speed *= 1.10;
                    }
                }
                default -> {
                }
            }
            double travel = speed * dt;
            if (travel >= distance) {
                enemy.x = target.getX();
                enemy.y = target.getY();
                enemy.nextWaypointIndex++;
            } else {
                enemy.x += dx / distance * travel;
                enemy.y += dy / distance * travel;
            }
        }
    }

    private void updateEffects(double dt) {
        for (int i = tracers.size() - 1; i >= 0; i--) {
            Tracer tracer = tracers.get(i);
            tracer.timeLeft -= dt;
            if (tracer.timeLeft <= 0.0) {
                tracers.remove(i);
            }
        }
        for (int i = pulses.size() - 1; i >= 0; i--) {
            Pulse pulse = pulses.get(i);
            pulse.timeLeft -= dt;
            if (pulse.timeLeft <= 0.0) {
                pulses.remove(i);
            }
        }
    }

    private void resolveRoundEnd() {
        if (!roundActive || !pendingSpawns.isEmpty() || !enemies.isEmpty()) {
            return;
        }
        roundActive = false;
        int clearedRound = nextRoundNumber;
        cash += rounds.get(clearedRound - 1).clearBonus();
        int earned = Math.max(0, cash - roundCashStart);
        nextRoundNumber++;
        if (clearedRound >= rounds.size()) {
            victory = true;
            showBanner("The grove held. Big Forest is safe.", 99.0);
            queueRoundToast("ROUND " + clearedRound + " CLEAR",
                    "+$" + earned + " last round. Big Forest held all " + rounds.size() + " rounds on " + difficulty.label + ".", 5.4);
        } else {
            showBanner("Round " + clearedRound + " cleared. +" + rounds.get(clearedRound - 1).clearBonus() + " cash.", 2.0);
            String note = roundHint(nextRoundNumber);
            queueRoundToast("ROUND " + clearedRound + " CLEAR", "+$" + earned + " last round. " + note, 4.8);
        }
    }

    private void spawnEnemy(EnemyKind kind) {
        enemies.add(new Enemy(kind, nextEnemyId++, kind.cashReward));
    }

    private void spawnEnemyAt(EnemyKind kind, double x, double y, int nextWaypointIndex) {
        Enemy child = new Enemy(kind, nextEnemyId++, 0);
        child.x = x;
        child.y = y;
        child.nextWaypointIndex = Math.clamp(nextWaypointIndex, 1, PATH.size() - 1);
        enemies.add(child);
    }

    private void removeDefeatedEnemies() {
        for (int i = enemies.size() - 1; i >= 0; i--) {
            Enemy enemy = enemies.get(i);
            if (enemy.health > 0.0) {
                continue;
            }
            enemies.remove(i);
            cash += enemy.cashAward;
            pulses.add(new Pulse(enemy.x, enemy.y, enemy.kind.radius * 1.8, enemy.kind.accent.deriveColor(0, 1, 1, 0.7), 0.2));
            if (enemy.kind.childKind != null && enemy.kind.childCount > 0 && !enemy.deathSpawnTriggered) {
                enemy.deathSpawnTriggered = true;
                for (int split = 0; split < enemy.kind.childCount; split++) {
                    double angle = (Math.PI * 2.0 * split) / Math.max(1, enemy.kind.childCount);
                    spawnEnemyAt(
                            enemy.kind.childKind,
                            enemy.x + Math.cos(angle) * 10.0,
                            enemy.y + Math.sin(angle) * 10.0,
                            enemy.nextWaypointIndex
                    );
                }
                pulses.add(new Pulse(enemy.x, enemy.y, 34 + enemy.kind.childCount * 6.0, enemy.kind.accent.deriveColor(0, 1, 1, 0.56), 0.24));
            }
        }
    }

    private Enemy chooseTarget(Tower tower, double range) {
        Enemy best = null;
        double rangeSq = range * range;
        double bestScore = Double.NEGATIVE_INFINITY;
        for (Enemy enemy : enemies) {
            if (enemy.kind.hidden && !towerCanSeeHidden(tower)) {
                continue;
            }
            double dx = enemy.x - tower.x;
            double dy = enemy.y - tower.y;
            if (dx * dx + dy * dy > rangeSq) {
                continue;
            }
            double score = targetScore(tower, enemy, dx, dy);
            if (score > bestScore) {
                bestScore = score;
                best = enemy;
            }
        }
        return best;
    }

    private boolean towerCanSeeHidden(Tower tower) {
        return switch (tower.kind) {
            case PIGEON -> tower.upgrades[0] >= 2;
            case EAGLE -> tower.upgrades[1] >= 2;
            case OPIUMBIRD -> tower.upgrades[1] >= 2;
            case BAT -> true;
        };
    }

    private boolean towerProjectileSeesHidden(Projectile projectile) {
        return projectile.seesHidden || projectile.sourceKind == TowerBirdKind.BAT;
    }

    private double targetScore(Tower tower, Enemy enemy, double dx, double dy) {
        double progress = enemyProgress(enemy);
        double distance = Math.hypot(dx, dy);
        double shellWeight = enemy.kind.leakDamage * 120.0 + enemy.kind.health * 30.0 + enemy.kind.armor * 45.0;
        return switch (tower.targetMode) {
            case FIRST -> progress * 10000.0 - enemy.id;
            case LAST -> -progress * 10000.0 - enemy.id;
            case STRONG -> shellWeight + enemy.health * 40.0 + progress;
            case CLOSE -> -distance * 100.0 + progress;
        };
    }

    private double enemyProgress(Enemy enemy) {
        if (enemy.nextWaypointIndex <= 0) {
            return 0.0;
        }
        Point2D previous = PATH.get(Math.max(0, enemy.nextWaypointIndex - 1));
        Point2D next = PATH.get(Math.min(PATH.size() - 1, enemy.nextWaypointIndex));
        double segmentLength = Math.max(1.0, previous.distance(next));
        double traveled = previous.distance(enemy.x, enemy.y);
        return enemy.nextWaypointIndex + traveled / segmentLength;
    }

    private void firePigeonVolley(Tower tower, Enemy target) {
        double angle = Math.atan2(target.y - tower.y, target.x - tower.x);
        int shots = pigeonShots(tower);
        boolean spikeLauncher = tower.upgrades[0] >= 3;
        boolean juggernaut = tower.upgrades[0] >= 4;
        double spread = shots <= 1 ? 0.0 : 0.26;
        double projectileSpeed = spikeLauncher ? (juggernaut ? 395 : 360) : 520;
        tower.attackVisualTime = 0.22;
        tower.attackCycle++;
        for (int i = 0; i < shots; i++) {
            double shotAngle = shots == 1 ? angle : angle - spread * 0.5 + spread * (i / (double) (shots - 1));
            double vx = Math.cos(shotAngle) * projectileSpeed;
            double vy = Math.sin(shotAngle) * projectileSpeed;
            Projectile projectile = new Projectile(
                    tower.x,
                    tower.y,
                    vx,
                    vy,
                    pigeonDamage(tower),
                    spikeLauncher ? (juggernaut ? 17 : 15) : PROJECTILE_RADIUS,
                    pigeonRange(tower) + 100,
                    spikeLauncher ? pigeonSplashRadius(tower) : 0.0,
                    pigeonPierce(tower),
                    spikeLauncher || tower.upgrades[1] >= 2,
                    tower.kind,
                    spikeLauncher ? (juggernaut ? Color.web("#CFD8DC") : Color.web("#B0BEC5")) : Color.web("#FFF8E1"),
                    spikeLauncher ? (juggernaut ? Color.web("#455A64") : Color.web("#6D4C41")) : Color.web("#8D6E63")
            );
            projectile.seesHidden = towerCanSeeHidden(tower);
            projectiles.add(projectile);
        }
    }

    private void fireEagleShot(Tower tower, Enemy target) {
        double angle = Math.atan2(target.y - tower.y, target.x - tower.x);
        double speed = 840;
        tower.attackVisualTime = 0.30;
        tower.attackCycle++;
        boolean precisionShot = tower.upgrades[0] >= 3 && tower.attackCycle % 4 == 0;
        Projectile projectile = new Projectile(
                tower.x,
                tower.y,
                Math.cos(angle) * speed,
                Math.sin(angle) * speed,
                eagleDamage(tower) + (precisionShot ? eaglePrecisionBonus(tower) : 0.0),
                11 + tower.upgrades[0] * 0.8,
                eagleRange(tower) + 120,
                0.0,
                1,
                tower.upgrades[0] >= 1,
                tower.kind,
                Color.web("#FFE082"),
                Color.web("#90CAF9")
        );
        projectile.seesHidden = towerCanSeeHidden(tower);
        if (tower.upgrades[0] >= 4) {
            projectile.onHitSlowMultiplier = 0.44;
            projectile.onHitSlowTime = target.kind.leakDamage >= 7 ? 2.6 : 1.4;
            projectile.onHitVulnerabilityMultiplier = 1.35;
            projectile.onHitVulnerabilityTime = 2.2;
        }
        projectiles.add(projectile);
    }

    private void fireOpiumSpell(Tower tower, Enemy target) {
        double angle = Math.atan2(target.y - tower.y, target.x - tower.x);
        int shots = opiumShots(tower);
        double spread = shots <= 1 ? 0.0 : 0.18;
        double speed = tower.upgrades[1] >= 3 ? 520 : 440;
        tower.attackVisualTime = 0.30;
        tower.attackCycle++;
        boolean lightningShot = tower.upgrades[0] >= 2 && tower.attackCycle % 4 == 0;
        boolean fireballShot = tower.upgrades[1] >= 1 && (tower.upgrades[1] >= 3 || tower.attackCycle % 3 == 0);
        for (int i = 0; i < shots; i++) {
            double shotAngle = shots == 1 ? angle : angle - spread * 0.5 + spread * (i / (double) (shots - 1));
            Projectile projectile = new Projectile(
                    tower.x,
                    tower.y - 4,
                    Math.cos(shotAngle) * speed,
                    Math.sin(shotAngle) * speed,
                    opiumDamage(tower) + (lightningShot ? 1.0 : 0.0),
                    fireballShot ? 11 : 9,
                    opiumRange(tower) + 110,
                    fireballShot ? opiumFireballRadius(tower) : 0.0,
                    opiumPierce(tower),
                    true,
                    tower.kind,
                    fireballShot ? Color.web("#FFB74D") : Color.web("#CE93D8"),
                    lightningShot ? Color.web("#B3E5FC") : Color.web("#7B2CBF")
            );
            projectile.seesHidden = towerCanSeeHidden(tower);
            if (lightningShot) {
                projectile.chainCount = tower.upgrades[0] >= 4 ? 3 : 1;
                projectile.chainDamageMultiplier = 0.78;
            }
            if (tower.upgrades[0] >= 3) {
                projectile.onHitKnockback = tower.upgrades[0] >= 4 ? 66.0 : 34.0;
            }
            if (fireballShot) {
                projectile.cloudRadius = tower.upgrades[1] >= 3 ? 40.0 : 28.0;
                projectile.cloudDamagePerSecond = tower.upgrades[1] >= 3 ? 4.2 : 2.1;
                projectile.cloudSlowMultiplier = tower.upgrades[0] >= 3 ? 0.84 : 0.92;
                projectile.cloudVulnerabilityMultiplier = tower.upgrades[0] >= 4 ? 1.18 : 1.0;
                projectile.cloudDuration = tower.upgrades[1] >= 4 ? 1.8 : 1.0;
            }
            if (tower.upgrades[1] >= 3) {
                projectile.pierceLeft += 1;
            }
            projectiles.add(projectile);
        }
        if (tower.upgrades[1] >= 4 && tower.companionTime > 0.0) {
            firePhoenixShot(tower, target);
        }
    }

    private void fireBatPulse(Tower tower, Enemy target) {
        double angle = Math.atan2(target.y - tower.y, target.x - tower.x);
        int shots = batShots(tower);
        double spread = shots <= 1 ? 0.0 : 0.30;
        double speed = tower.upgrades[1] >= 1 ? 530 : 500;
        tower.attackVisualTime = 0.26;
        tower.attackCycle++;
        boolean flashBomb = tower.upgrades[1] >= 3 && tower.attackCycle % 4 == 0;
        for (int i = 0; i < shots; i++) {
            double shotAngle = shots == 1 ? angle : angle - spread * 0.5 + spread * (i / (double) (shots - 1));
            Projectile projectile = new Projectile(
                    tower.x,
                    tower.y - 2,
                    Math.cos(shotAngle) * speed,
                    Math.sin(shotAngle) * speed,
                    batDamage(tower),
                    flashBomb ? 12 : 10,
                    batRange(tower) + 120,
                    flashBomb ? batFlashBombRadius(tower) : 0.0,
                    batPierce(tower),
                    false,
                    tower.kind,
                    tower.upgrades[1] >= tower.upgrades[0] ? Color.web("#B39DDB") : Color.web("#7E57C2"),
                    tower.upgrades[1] >= tower.upgrades[0] ? Color.web("#80DEEA") : Color.web("#311B92")
            );
            projectile.seeking = tower.upgrades[1] >= 1;
            projectile.turnRate = tower.upgrades[1] >= 1 ? 6.8 : 0.0;
            projectile.seesHidden = true;
            if (tower.upgrades[1] >= 2) {
                projectile.onHitKnockback = flashBomb ? 38.0 : 18.0;
            }
            if (flashBomb) {
                projectile.onHitSlowMultiplier = 0.38;
                projectile.onHitSlowTime = 0.8;
            }
            projectiles.add(projectile);
        }
    }

    private void firePhoenixShot(Tower tower, Enemy target) {
        double angle = Math.atan2(target.y - tower.y, target.x - tower.x);
        double speed = 460;
        Projectile projectile = new Projectile(
                tower.x,
                tower.y - 22,
                Math.cos(angle) * speed,
                Math.sin(angle) * speed,
                3.6 + (tower.upgrades[0] >= 2 ? 0.8 : 0.0),
                13,
                opiumRange(tower) + 150,
                30,
                4,
                true,
                tower.kind,
                Color.web("#FFCC80"),
                Color.web("#FF7043")
        );
        projectile.seesHidden = true;
        projectile.cloudRadius = 24;
        projectile.cloudDamagePerSecond = 2.2;
        projectile.cloudSlowMultiplier = 0.9;
        projectile.cloudDuration = 1.0;
        projectiles.add(projectile);
    }

    private boolean handleProjectileHit(Projectile projectile, Enemy enemy) {
        switch (projectile.sourceKind) {
            case PIGEON -> {
                applyDamage(enemy, projectile.damage, projectile.armorPierce);
                if (projectile.splashRadius > 1.0) {
                    splashDamage(projectile.x, projectile.y, projectile.splashRadius, projectile.damage * 0.45, projectile.armorPierce);
                }
                projectile.pierceLeft--;
                return projectile.pierceLeft <= 0;
            }
            case EAGLE -> {
                applyDamage(enemy, projectile.damage, projectile.armorPierce);
                tracers.add(new Tracer(projectile.x, projectile.y, enemy.x, enemy.y, projectile.color, 4.0, 0.14));
                if (projectile.onHitSlowTime > 0.0) {
                    enemy.forcedSlowMultiplier = Math.min(enemy.forcedSlowMultiplier, projectile.onHitSlowMultiplier);
                    enemy.forcedSlowTime = Math.max(enemy.forcedSlowTime, projectile.onHitSlowTime);
                }
                if (projectile.onHitVulnerabilityTime > 0.0) {
                    enemy.forcedVulnerabilityMultiplier = Math.max(enemy.forcedVulnerabilityMultiplier, projectile.onHitVulnerabilityMultiplier);
                    enemy.forcedVulnerabilityTime = Math.max(enemy.forcedVulnerabilityTime, projectile.onHitVulnerabilityTime);
                }
                return true;
            }
            case BAT -> {
                applyDamage(enemy, projectile.damage, projectile.armorPierce);
                if (projectile.splashRadius > 1.0) {
                    splashDamage(projectile.x, projectile.y, projectile.splashRadius, projectile.damage * 0.42, projectile.armorPierce);
                }
                if (projectile.onHitSlowTime > 0.0) {
                    enemy.forcedSlowMultiplier = Math.min(enemy.forcedSlowMultiplier, projectile.onHitSlowMultiplier);
                    enemy.forcedSlowTime = Math.max(enemy.forcedSlowTime, projectile.onHitSlowTime);
                }
                if (projectile.onHitKnockback > 0.0 && enemy.kind.leakDamage <= 7) {
                    pushEnemyBack(enemy, projectile.onHitKnockback);
                }
                pulses.add(new Pulse(projectile.x, projectile.y, 24 + projectile.splashRadius * 0.3, projectile.accentColor.deriveColor(0, 1, 1, 0.46), 0.14));
                projectile.pierceLeft--;
                return projectile.pierceLeft <= 0;
            }
            case OPIUMBIRD -> {
                applyDamage(enemy, projectile.damage, projectile.armorPierce);
                if (projectile.splashRadius > 1.0) {
                    splashDamage(projectile.x, projectile.y, projectile.splashRadius, projectile.damage * 0.55, true);
                }
                if (projectile.cloudDuration > 0.0 && projectile.cloudRadius > 0.0) {
                    clouds.add(new Cloud(
                            projectile.x,
                            projectile.y,
                            projectile.cloudRadius,
                            projectile.cloudDamagePerSecond,
                            projectile.cloudSlowMultiplier,
                            projectile.cloudVulnerabilityMultiplier,
                            projectile.cloudDuration
                    ));
                }
                if (projectile.chainCount > 0) {
                    applyMagicChain(enemy, projectile);
                }
                if (projectile.onHitKnockback > 0.0) {
                    pushEnemyBack(enemy, projectile.onHitKnockback);
                    pulses.add(new Pulse(enemy.x, enemy.y, projectile.onHitKnockback * 0.55, Color.web("#BA68C8", 0.42), 0.18));
                }
                projectile.pierceLeft--;
                return projectile.pierceLeft <= 0;
            }
        }
        return true;
    }

    private void applyMagicChain(Enemy firstHit, Projectile projectile) {
        if (projectile.chainCount <= 0) {
            return;
        }
        List<Enemy> chained = new ArrayList<>();
        chained.add(firstHit);
        Enemy current = firstHit;
        for (int bounce = 0; bounce < projectile.chainCount; bounce++) {
            Enemy next = null;
            double bestScore = Double.NEGATIVE_INFINITY;
            for (Enemy enemy : enemies) {
                if (enemy == current || chained.contains(enemy)) {
                    continue;
                }
                if (enemy.kind.hidden && !towerProjectileSeesHidden(projectile)) {
                    continue;
                }
                if (currentDistanceSq(current.x, current.y, enemy.x, enemy.y) > 165 * 165) {
                    continue;
                }
                double score = enemyProgress(enemy);
                if (score > bestScore) {
                    bestScore = score;
                    next = enemy;
                }
            }
            if (next == null) {
                break;
            }
            chained.add(next);
            applyDamage(next, projectile.damage * projectile.chainDamageMultiplier, true);
            tracers.add(new Tracer(current.x, current.y, next.x, next.y, projectile.accentColor.deriveColor(0, 1, 1, 0.88), 2.6, 0.12));
            current = next;
        }
    }

    private void applyDamage(Enemy enemy, double rawDamage, boolean armorPierce) {
        double damage = rawDamage * enemy.vulnerabilityMultiplier;
        if (!armorPierce) {
            damage = Math.max(enemy.kind.armored ? 0.4 : 1.0, damage - enemy.kind.armor);
        }
        enemy.health -= damage;
    }

    private void splashDamage(double centerX, double centerY, double radius, double damage, boolean armorPierce) {
        double radiusSq = radius * radius;
        for (Enemy enemy : enemies) {
            double dx = enemy.x - centerX;
            double dy = enemy.y - centerY;
            if (dx * dx + dy * dy <= radiusSq) {
                applyDamage(enemy, damage, armorPierce);
            }
        }
    }

    private void pushEnemyBack(Enemy enemy, double distance) {
        if (distance <= 0.0) {
            return;
        }
        double remaining = enemy.kind.armored ? distance * 0.55 : distance;
        while (remaining > 0.1) {
            int nextIndex = Math.clamp(enemy.nextWaypointIndex, 1, PATH.size() - 1);
            Point2D previous = PATH.get(nextIndex - 1);
            Point2D next = PATH.get(nextIndex);
            double dx = next.getX() - previous.getX();
            double dy = next.getY() - previous.getY();
            double segmentLength = Math.max(1.0, Math.hypot(dx, dy));
            double progress = ((enemy.x - previous.getX()) * dx + (enemy.y - previous.getY()) * dy) / (segmentLength * segmentLength);
            progress = Math.clamp(progress, 0.0, 1.0);
            double segmentTravel = progress * segmentLength;
            if (remaining < segmentTravel || nextIndex <= 1) {
                progress = Math.max(0.0, progress - remaining / segmentLength);
                enemy.x = previous.getX() + dx * progress;
                enemy.y = previous.getY() + dy * progress;
                if (progress <= 0.0001 && nextIndex > 1) {
                    enemy.nextWaypointIndex = nextIndex - 1;
                }
                break;
            }
            remaining -= segmentTravel;
            enemy.x = previous.getX();
            enemy.y = previous.getY();
            if (nextIndex <= 1) {
                break;
            }
            enemy.nextWaypointIndex = nextIndex - 1;
        }
    }

    private double towerRange(Tower tower) {
        return switch (tower.kind) {
            case PIGEON -> pigeonRange(tower);
            case EAGLE -> eagleRange(tower);
            case OPIUMBIRD -> opiumRange(tower);
            case BAT -> batRange(tower);
        };
    }

    private double towerAttackInterval(Tower tower) {
        return switch (tower.kind) {
            case PIGEON -> pigeonAttackInterval(tower);
            case EAGLE -> eagleAttackInterval(tower);
            case OPIUMBIRD -> opiumAttackInterval(tower);
            case BAT -> batAttackInterval(tower);
        };
    }

    private double supportAttackMultiplier(Tower tower) {
        double best = 1.0;
        for (Tower support : towers) {
            if (support == tower || support.kind != TowerBirdKind.PIGEON || support.upgrades[1] < 4 || support.companionTime <= 0.0 || tower.kind != TowerBirdKind.PIGEON) {
                continue;
            }
            double radius = 160.0;
            if (currentDistanceSq(tower.x, tower.y, support.x, support.y) > radius * radius) {
                continue;
            }
            best = Math.max(best, 2.05);
        }
        return best;
    }

    private double pigeonRange(Tower tower) {
        return 168 + Math.min(2, tower.upgrades[0]) * 34 + (tower.upgrades[0] >= 3 ? 26 : 0);
    }

    private double pigeonDamage(Tower tower) {
        if (tower.upgrades[0] >= 4) return 8.0;
        if (tower.upgrades[0] >= 3) return 4.0;
        return tower.companionTime > 0.0 ? 2.0 : 1.0;
    }

    private int pigeonPierce(Tower tower) {
        if (tower.upgrades[0] >= 4) return 60;
        if (tower.upgrades[0] >= 3) return 26;
        int pierce = 1;
        if (tower.upgrades[1] >= 1) pierce += 1;
        if (tower.upgrades[1] >= 2) pierce += 1;
        return pierce + (tower.companionTime > 0.0 ? 2 : 0);
    }

    private double pigeonSplashRadius(Tower tower) {
        return tower.upgrades[0] >= 4 ? 62 : 42;
    }

    private int pigeonShots(Tower tower) {
        int shots = tower.upgrades[1] >= 3 ? 3 : 1;
        if (tower.companionTime > 0.0) {
            shots += 2;
        }
        return shots;
    }

    private double eagleRange(Tower tower) {
        return 2800;
    }

    private double eagleDamage(Tower tower) {
        return 2.4 + tower.upgrades[0] * 2.0 + (tower.upgrades[0] >= 2 ? 2.6 : 0.0);
    }

    private double eaglePrecisionBonus(Tower tower) {
        return tower.upgrades[0] >= 4 ? 22.0 : 12.0;
    }

    private double opiumRange(Tower tower) {
        return 186 + tower.upgrades[0] * 16 + (tower.upgrades[1] >= 3 ? 20 : 0);
    }

    private double opiumDamage(Tower tower) {
        return 1.2 + tower.upgrades[0] * 0.9 + (tower.upgrades[1] >= 3 ? 0.7 : 0.0);
    }

    private int opiumPierce(Tower tower) {
        return 2 + tower.upgrades[0] + (tower.upgrades[1] >= 3 ? 1 : 0);
    }

    private int opiumShots(Tower tower) {
        return tower.upgrades[1] >= 3 ? 3 : 1;
    }

    private double opiumFireballRadius(Tower tower) {
        return tower.upgrades[1] >= 3 ? 56 : 34;
    }

    private double opiumAuraRadius(Tower tower) {
        return 64 + tower.upgrades[0] * 16 + tower.upgrades[1] * 12;
    }

    private Color opiumAuraColor(Tower tower) {
        if (tower.upgrades[1] >= 4) {
            return Color.web("#FFB74D");
        }
        if (tower.upgrades[0] >= tower.upgrades[1]) {
            return Color.web("#BA68C8");
        }
        return Color.web("#CE93D8");
    }

    private double batRange(Tower tower) {
        return 192 + (tower.upgrades[0] >= 1 ? 26 : 0) + (tower.upgrades[0] >= 2 ? 14 : 0);
    }

    private double batDamage(Tower tower) {
        return 1.0 + (tower.upgrades[0] >= 2 ? 1.0 : 0.0) + (tower.upgrades[1] >= 3 ? 1.0 : 0.0);
    }

    private int batPierce(Tower tower) {
        return 2 + tower.upgrades[0] + (tower.upgrades[0] >= 4 ? 2 : 0);
    }

    private int batShots(Tower tower) {
        if (tower.upgrades[0] >= 4) return 5;
        if (tower.upgrades[0] >= 3) return 2;
        return 1;
    }

    private double batFlashBombRadius(Tower tower) {
        return tower.upgrades[1] >= 4 ? 72 : 56;
    }

    private double pigeonAttackInterval(Tower tower) {
        double interval = tower.upgrades[0] >= 3 ? 1.32 : 0.92;
        if (tower.upgrades[1] >= 4 && tower.companionTime > 0.0) {
            interval *= 0.44;
        }
        return interval;
    }

    private double eagleAttackInterval(Tower tower) {
        double interval = 1.72;
        if (tower.upgrades[1] >= 1) interval *= 0.80;
        if (tower.upgrades[1] >= 3) interval *= 0.54;
        if (tower.upgrades[1] >= 4) interval *= 0.82;
        return interval;
    }

    private double opiumAttackInterval(Tower tower) {
        double interval = tower.upgrades[1] >= 3 ? 0.32 : 0.90;
        if (tower.upgrades[0] >= 3) interval *= 0.88;
        return interval;
    }

    private double batAttackInterval(Tower tower) {
        double interval = 0.82;
        if (tower.upgrades[0] >= 1) interval *= 0.84;
        if (tower.upgrades[0] >= 3) interval *= 0.80;
        return interval;
    }

    private int sellValue(Tower tower) {
        return Math.max(1, (int) Math.round(tower.investedCash() * 0.7));
    }

    private boolean canUpgrade(Tower tower, int pathIndex) {
        UpgradeTier[] path = pathIndex == 0 ? tower.kind.pathA : tower.kind.pathB;
        int current = tower.upgrades[pathIndex];
        if (current >= path.length) {
            return false;
        }
        int other = tower.upgrades[1 - pathIndex];
        return current < 2 || other <= 2;
    }

    private String placementIssue(double x, double y) {
        if (x < BUILD_PADDING || x > MAP_WIDTH - BUILD_PADDING || y < BUILD_PADDING || y > MAP_HEIGHT - BUILD_PADDING) {
            return "Stay inside the clearing.";
        }
        if (distanceToPath(x, y) < PATH_WIDTH * 0.5 + 20) {
            return "Can't place birds on the dirt trail.";
        }
        for (Tower tower : towers) {
            if (currentDistanceSq(x, y, tower.x, tower.y) < TOWER_SPACING * TOWER_SPACING) {
                return "Birds need more room between perches.";
            }
        }
        return null;
    }

    private void showBanner(String message, double seconds) {
        banner = message;
        bannerTime = Math.max(seconds, 0.001);
    }

    private void queueRoundToast(String title, String body, double seconds) {
        if (title == null && body == null) {
            return;
        }
        toastQueue.addLast(new ToastMessage(
                title == null ? "" : title,
                body == null ? "" : body,
                Math.max(2.2, seconds)
        ));
        showNextToastIfNeeded();
    }

    private void showNextToastIfNeeded() {
        if (activeToast != null || toastQueue.isEmpty()) {
            return;
        }
        activeToast = toastQueue.removeFirst();
        activeToastTime = activeToast.durationSeconds();
    }

    private String roundHint(int roundNumber) {
        if (roundNumber <= 0 || roundNumber > ROUND_HINTS.size()) {
            return "";
        }
        return ROUND_HINTS.get(roundNumber - 1);
    }

    private double distanceToPath(double x, double y) {
        double best = Double.MAX_VALUE;
        for (int i = 1; i < PATH.size(); i++) {
            best = Math.min(best, distancePointToSegment(x, y, PATH.get(i - 1), PATH.get(i)));
        }
        return best;
    }

    private double currentDistanceSq(double x1, double y1, double x2, double y2) {
        double dx = x1 - x2;
        double dy = y1 - y2;
        return dx * dx + dy * dy;
    }

    private double distancePointToSegment(double px, double py, Point2D a, Point2D b) {
        double dx = b.getX() - a.getX();
        double dy = b.getY() - a.getY();
        if (Math.abs(dx) < 0.0001 && Math.abs(dy) < 0.0001) {
            return Math.hypot(px - a.getX(), py - a.getY());
        }
        double t = ((px - a.getX()) * dx + (py - a.getY()) * dy) / (dx * dx + dy * dy);
        t = Math.clamp(t, 0.0, 1.0);
        double sx = a.getX() + t * dx;
        double sy = a.getY() + t * dy;
        return Math.hypot(px - sx, py - sy);
    }

    private void drawBackground(GraphicsContext g) {
        g.setFill(new LinearGradient(
                0, 0, 1, 1, true, CycleMethod.NO_CYCLE,
                new Stop(0, Color.web("#10281C")),
                new Stop(0.48, Color.web("#1F513A")),
                new Stop(1, Color.web("#08130D"))
        ));
        g.fillRect(0, 0, MAP_WIDTH, MAP_HEIGHT);

        for (double[] clearing : CLEARINGS) {
            g.setFill(Color.web("#95D5B2", 0.10));
            g.fillOval(clearing[0] - clearing[2], clearing[1] - clearing[2] * 0.72, clearing[2] * 2, clearing[2] * 1.44);
        }

        for (int i = 0; i < 20; i++) {
            double phase = animationClock * 0.42 + i * 0.55;
            double x = 90 + (i * 73.0) % (MAP_WIDTH - 140);
            double y = 70 + (i * 129.0) % (MAP_HEIGHT - 120);
            double width = 150 + (i % 4) * 26;
            double height = 90 + (i % 3) * 18;
            g.setFill(Color.web("#83C5A1", 0.04 + (i % 3) * 0.018));
            g.fillOval(x - width * 0.5 + Math.sin(phase) * 12.0, y - height * 0.5, width, height);
        }

        drawAnimatedGrass(g);
        drawAmbientForestBirds(g);
    }

    private void drawAnimatedGrass(GraphicsContext g) {
        g.setLineWidth(2.2);
        for (int i = 0; i < 72; i++) {
            double baseX = 18 + i * 20.0;
            double baseY = MAP_HEIGHT - 10 - (i % 4) * 2.5;
            double sway = Math.sin(animationClock * 2.1 + i * 0.45) * (4.0 + (i % 3) * 0.8);
            g.setStroke(Color.web("#53A36C", 0.34));
            g.strokeLine(baseX, baseY, baseX + sway, baseY - 16 - (i % 3) * 5);
            g.setStroke(Color.web("#95D5B2", 0.18));
            g.strokeLine(baseX + 2, baseY, baseX + sway * 0.8 + 2, baseY - 11 - (i % 2) * 4);
        }
    }

    private void drawAmbientForestBirds(GraphicsContext g) {
        g.setStroke(Color.web("#071B12", 0.22));
        g.setLineWidth(2.2);
        g.setLineCap(javafx.scene.shape.StrokeLineCap.ROUND);
        for (int i = 0; i < 4; i++) {
            double progress = (animationClock * 0.018 + i * 0.24) % 1.25 - 0.12;
            double x = progress * MAP_WIDTH;
            double y = 130 + i * 78 + Math.sin(animationClock * 1.3 + i * 0.9) * 16.0;
            double wing = 12 + i * 1.8;
            g.strokeArc(x - wing, y - 5, wing, 10, 25, 130, javafx.scene.shape.ArcType.OPEN);
            g.strokeArc(x, y - 5, wing, 10, 25, 130, javafx.scene.shape.ArcType.OPEN);
        }
    }

    private void drawPath(GraphicsContext g) {
        g.setLineCap(javafx.scene.shape.StrokeLineCap.ROUND);
        g.setLineJoin(javafx.scene.shape.StrokeLineJoin.ROUND);
        g.setLineWidth(PATH_WIDTH + 20);
        g.setStroke(Color.web("#3A2A1A", 0.72));
        tracePath(g);
        g.setLineWidth(PATH_WIDTH);
        g.setStroke(new LinearGradient(
                0, 0, 1, 0, true, CycleMethod.NO_CYCLE,
                new Stop(0, Color.web("#8D6E63")),
                new Stop(0.5, Color.web("#A1887F")),
                new Stop(1, Color.web("#6D4C41"))
        ));
        tracePath(g);
        g.setLineWidth(PATH_WIDTH * 0.16);
        g.setStroke(Color.web("#E6C79C", 0.62));
        tracePath(g);

        Point2D start = PATH.getFirst();
        g.setFill(Color.web("#3E2723", 0.8));
        g.fillOval(start.getX() - 52, start.getY() - 52, 104, 104);
        g.setFill(Color.web("#7F5539", 0.82));
        g.fillOval(start.getX() - 28, start.getY() - 28, 56, 56);

        Point2D exit = PATH.getLast();
        g.setFill(Color.web("#4527A0", 0.20));
        g.fillOval(exit.getX() - 78, exit.getY() - 78, 156, 156);
        g.setStroke(Color.web("#D1C4E9", 0.9));
        g.setLineWidth(5.0);
        g.strokeOval(exit.getX() - 26, exit.getY() - 26, 52, 52);
        g.strokeLine(exit.getX(), exit.getY() - 34, exit.getX(), exit.getY() + 34);
        g.strokeLine(exit.getX() - 34, exit.getY(), exit.getX() + 34, exit.getY());
    }

    private void tracePath(GraphicsContext g) {
        Point2D prev = PATH.getFirst();
        for (int i = 1; i < PATH.size(); i++) {
            Point2D next = PATH.get(i);
            g.strokeLine(prev.getX(), prev.getY(), next.getX(), next.getY());
            prev = next;
        }
    }

    private void drawDecor(GraphicsContext g) {
        for (int i = 0; i < TREE_CLUMPS.length; i++) {
            double[] tree = TREE_CLUMPS[i];
            double x = tree[0];
            double y = tree[1];
            double radius = tree[2];
            double sway = Math.sin(animationClock * 0.8 + i * 0.65) * Math.min(8.0, radius * 0.08);
            g.setFill(Color.web("#0B2419", 0.45));
            g.fillOval(x - radius * 0.92, y - radius * 0.84, radius * 1.84, radius * 1.68);
            g.setFill(Color.web("#1B5E20"));
            g.fillOval(x - radius + sway * 0.18, y - radius, radius * 2, radius * 1.86);
            g.setFill(Color.web("#2E7D32", 0.82));
            g.fillOval(x - radius * 0.72 + sway * 0.28, y - radius * 0.74, radius * 1.44, radius * 1.3);
            g.setFill(Color.web("#74C69D", 0.14));
            g.fillOval(x - radius * 0.42 + sway * 0.32, y - radius * 0.48, radius * 0.84, radius * 0.58);
            g.setFill(Color.web("#4E342E", 0.8));
            g.fillOval(x - radius * 0.14, y + radius * 0.42, radius * 0.28, radius * 0.54);
        }

        for (int i = 0; i < 16; i++) {
            double x = 110 + (i * 97.0) % (MAP_WIDTH - 180);
            double y = 160 + (i * 141.0) % (MAP_HEIGHT - 220);
            g.setFill(Color.web("#DDEB9D", 0.08));
            g.fillOval(x - 5, y - 5, 10, 10);
            g.setFill(Color.web("#A7C957", 0.20));
            g.fillOval(x - 2.5, y - 2.5, 5, 5);
        }
    }

    private void drawClouds(GraphicsContext g) {
        for (Cloud cloud : clouds) {
            double alpha = Math.min(0.28, cloud.timeLeft / Math.max(1.0, cloud.timeLeft + 0.2));
            g.setFill(Color.web("#7B2CBF", alpha));
            g.fillOval(cloud.x - cloud.radius, cloud.y - cloud.radius * 0.82, cloud.radius * 2, cloud.radius * 1.64);
            g.setStroke(Color.web("#E0AAFF", alpha + 0.12));
            g.setLineWidth(2.0);
            g.strokeOval(cloud.x - cloud.radius * 0.84, cloud.y - cloud.radius * 0.68, cloud.radius * 1.68, cloud.radius * 1.36);
        }
    }

    private void drawPulses(GraphicsContext g) {
        for (Pulse pulse : pulses) {
            double t = 1.0 - pulse.timeLeft / pulse.totalTime;
            double radius = pulse.maxRadius * t;
            g.setStroke(pulse.color.deriveColor(0, 1, 1, 1.0 - t));
            g.setLineWidth(3.0);
            g.strokeOval(pulse.x - radius, pulse.y - radius, radius * 2, radius * 2);
        }
    }

    private void drawSelectionRanges(GraphicsContext g, double mouseX, double mouseY) {
        if (selectedTower != null) {
            double range = towerRange(selectedTower);
            g.setStroke(Color.web("#B3E5FC", 0.34));
            g.setLineWidth(2.0);
            g.strokeOval(selectedTower.x - range, selectedTower.y - range, range * 2, range * 2);
        }
        if (buildSelection != null) {
            PlacementPreview preview = placementPreview(mouseX, mouseY);
            Tower ghost = new Tower(buildSelection, mouseX, mouseY, shopCost(buildSelection), game);
            double range = towerRange(ghost);
            g.setStroke(preview.valid() ? Color.web("#A5D6A7", 0.52) : Color.web("#EF9A9A", 0.52));
            g.setLineWidth(2.0);
            g.strokeOval(mouseX - range, mouseY - range, range * 2, range * 2);
            g.save();
            g.setGlobalAlpha(preview.valid() ? 0.85 : 0.45);
            drawTower(g, ghost, true);
            g.restore();
        }
    }

    private void drawProjectiles(GraphicsContext g) {
        for (Projectile projectile : projectiles) {
            double angle = Math.toDegrees(Math.atan2(projectile.vy, projectile.vx));
            g.save();
            g.translate(projectile.x, projectile.y);
            g.rotate(angle);
            switch (projectile.sourceKind) {
                case PIGEON -> {
                    g.setFill(projectile.color);
                    g.fillOval(-projectile.radius, -projectile.radius * 0.72, projectile.radius * 2.0, projectile.radius * 1.44);
                    g.setFill(projectile.accentColor);
                    g.fillPolygon(
                            new double[]{projectile.radius * 0.2, projectile.radius * 1.4, projectile.radius * 0.25},
                            new double[]{0, -projectile.radius * 0.2, projectile.radius * 0.4},
                            3
                    );
                    g.setStroke(Color.web("#4E342E", 0.7));
                    g.setLineWidth(1.1);
                    g.strokeOval(-projectile.radius, -projectile.radius * 0.72, projectile.radius * 2.0, projectile.radius * 1.44);
                }
                case EAGLE -> {
                    g.setStroke(projectile.accentColor.deriveColor(0, 1, 1, 0.34));
                    g.setLineWidth(3.8);
                    g.strokeLine(-projectile.radius * 1.8, 0, projectile.radius * 1.4, 0);
                    g.setFill(projectile.color);
                    g.fillPolygon(
                            new double[]{-projectile.radius * 0.5, projectile.radius * 1.5, -projectile.radius * 0.5},
                            new double[]{-projectile.radius * 0.55, 0, projectile.radius * 0.55},
                            3
                    );
                    g.setStroke(Color.web("#FFF8E1", 0.85));
                    g.setLineWidth(1.4);
                    g.strokeLine(-projectile.radius * 1.2, 0, projectile.radius * 1.0, 0);
                }
                case OPIUMBIRD -> {
                    g.setFill(projectile.accentColor.deriveColor(0, 1, 1, 0.22));
                    g.fillOval(-projectile.radius * 1.55, -projectile.radius * 1.55, projectile.radius * 3.1, projectile.radius * 3.1);
                    g.setFill(projectile.color);
                    g.fillOval(-projectile.radius, -projectile.radius, projectile.radius * 2, projectile.radius * 2);
                    g.setStroke(Color.web("#E1BEE7", 0.82));
                    g.setLineWidth(1.4);
                    g.strokeOval(-projectile.radius, -projectile.radius, projectile.radius * 2, projectile.radius * 2);
                }
                case BAT -> {
                    g.setStroke(projectile.accentColor.deriveColor(0, 1, 1, 0.34));
                    g.setLineWidth(4.0);
                    g.strokeArc(-projectile.radius * 1.8, -projectile.radius * 1.1, projectile.radius * 3.6, projectile.radius * 2.2,
                            208, 124, javafx.scene.shape.ArcType.OPEN);
                    g.setStroke(projectile.color);
                    g.setLineWidth(2.2);
                    g.strokeArc(-projectile.radius * 1.25, -projectile.radius * 0.72, projectile.radius * 2.5, projectile.radius * 1.44,
                            208, 124, javafx.scene.shape.ArcType.OPEN);
                    g.strokeLine(-projectile.radius * 0.4, 0, projectile.radius * 1.1, 0);
                }
            }
            g.restore();
        }
    }

    private void drawTracers(GraphicsContext g) {
        for (Tracer tracer : tracers) {
            double alpha = Math.max(0.0, tracer.timeLeft / tracer.totalTime);
            g.setStroke(tracer.color.deriveColor(0, 1, 1, alpha));
            g.setLineWidth(tracer.width);
            g.strokeLine(tracer.x1, tracer.y1, tracer.x2, tracer.y2);
        }
    }

    private void drawEnemies(GraphicsContext g) {
        for (Enemy enemy : enemies) {
            drawEnemy(g, enemy);
        }
    }

    private void drawEnemy(GraphicsContext g, Enemy enemy) {
        double ratio = Math.clamp(enemy.health / enemy.maxHealth(), 0.0, 1.0);
        double wear = 1.0 - ratio;
        double alpha = enemy.kind.hidden ? 0.58 : 1.0;
        g.save();
        g.setGlobalAlpha(alpha);
        g.setFill(Color.rgb(0, 0, 0, 0.18));
        g.fillOval(enemy.x - enemy.kind.radius * 0.92, enemy.y + enemy.kind.radius * 0.38,
                enemy.kind.radius * 1.84, enemy.kind.radius * 0.62);
        switch (enemy.kind) {
            case BLIGHT, SWIFT_BLIGHT, SHADE_BLIGHT -> drawSmallBlight(g, enemy, wear);
            case BRAMBLE_BLIGHT, RESIN_BLIGHT -> drawArmoredBlight(g, enemy, wear);
            case ROOT_GOLEM, IRONBARK_GOLEM, HEARTWOOD_COLOSSUS -> drawGolemShell(g, enemy, wear);
        }
        g.restore();
    }

    private void drawSmallBlight(GraphicsContext g, Enemy enemy, double wear) {
        double r = enemy.kind.radius;
        if (enemy.kind == EnemyKind.SWIFT_BLIGHT && enemy.abilityTime > 0.0) {
            g.setStroke(Color.web("#C8F79C", 0.48));
            g.setLineWidth(3.0);
            g.strokeLine(enemy.x - r * 2.1, enemy.y - 4, enemy.x - r * 0.9, enemy.y - 4);
            g.strokeLine(enemy.x - r * 1.8, enemy.y + 4, enemy.x - r * 0.6, enemy.y + 4);
        }
        if (enemy.kind == EnemyKind.SHADE_BLIGHT) {
            g.setFill(Color.web("#1D2C1F", 0.34));
            g.fillOval(enemy.x - r * 1.6, enemy.y - r * 1.5, r * 3.2, r * 3.0);
        }
        g.setFill(enemy.kind.body.darker());
        g.fillOval(enemy.x - r * 1.18, enemy.y - r * 0.86, r * 2.36, r * 1.72);
        g.setFill(enemy.kind.body);
        g.fillOval(enemy.x - r, enemy.y - r * 0.74, r * 2.0, r * 1.48);
        g.setFill(enemy.kind.accent);
        g.fillOval(enemy.x - r * 0.76, enemy.y - r * 1.48, r * 1.52, r * 1.14);
        if (enemy.kind == EnemyKind.SWIFT_BLIGHT) {
            g.setStroke(Color.web("#F1F8E9", 0.78));
            g.setLineWidth(2.0);
            g.strokeArc(enemy.x - r * 0.78, enemy.y - r * 1.34, r * 1.56, r * 1.12, 200, 130, javafx.scene.shape.ArcType.OPEN);
        }
        drawEnemyEyes(g, enemy.x, enemy.y - r * 0.06, r * 0.22);
        drawEnemyDamageCracks(g, enemy, wear, 2);
    }

    private void drawArmoredBlight(GraphicsContext g, Enemy enemy, double wear) {
        double r = enemy.kind.radius;
        g.setFill(Color.web("#2B1A0F"));
        g.fillOval(enemy.x - r * 1.22, enemy.y - r * 0.94, r * 2.44, r * 1.88);
        g.setFill(enemy.kind.body);
        g.fillOval(enemy.x - r, enemy.y - r * 0.78, r * 2.0, r * 1.56);
        g.setFill(enemy.kind.accent.deriveColor(0, 1, 1, 0.94));
        g.fillOval(enemy.x - r * 0.66, enemy.y - r * 0.26, r * 1.32, r * 0.96);
        g.setStroke(Color.web("#F6E7B0", 0.76));
        g.setLineWidth(enemy.kind == EnemyKind.RESIN_BLIGHT ? 2.4 : 2.0);
        g.strokeArc(enemy.x - r * 0.98, enemy.y - r * 0.82, r * 1.2, r * 0.96, 205, 126, javafx.scene.shape.ArcType.OPEN);
        g.strokeArc(enemy.x - r * 0.18, enemy.y - r * 0.70, r * 0.98, r * 0.86, 210, 118, javafx.scene.shape.ArcType.OPEN);
        if (enemy.kind == EnemyKind.RESIN_BLIGHT) {
            g.setStroke(Color.web("#F2C26B", 0.82));
            g.setLineWidth(3.0);
            g.strokeLine(enemy.x - r * 0.86, enemy.y + r * 0.12, enemy.x + r * 0.92, enemy.y + r * 0.24);
        }
        drawEnemyEyes(g, enemy.x, enemy.y - r * 0.04, r * 0.2);
        drawEnemyDamageCracks(g, enemy, wear, 3);
    }

    private void drawGolemShell(GraphicsContext g, Enemy enemy, double wear) {
        double r = enemy.kind.radius;
        double width = r * 2.24;
        double height = r * 2.0;
        double x = enemy.x - width * 0.5;
        double y = enemy.y - height * 0.56;
        g.setFill(Color.web("#211310"));
        g.fillRoundRect(x - 4, y - 4, width + 8, height + 8, r * 0.7, r * 0.7);
        g.setFill(enemy.kind.body);
        g.fillRoundRect(x, y, width, height, r * 0.64, r * 0.64);
        g.setStroke(enemy.kind.accent);
        g.setLineWidth(enemy.kind == EnemyKind.HEARTWOOD_COLOSSUS ? 4.2 : 3.0);
        g.strokeLine(enemy.x - r * 0.62, enemy.y - r * 0.28, enemy.x + r * 0.62, enemy.y + r * 0.36);
        g.strokeLine(enemy.x - r * 0.56, enemy.y + r * 0.44, enemy.x + r * 0.54, enemy.y - r * 0.36);
        if (enemy.kind != EnemyKind.ROOT_GOLEM) {
            g.setStroke(Color.web("#8D6E63", 0.82));
            g.setLineWidth(3.0);
            g.strokeLine(enemy.x - r * 0.24, enemy.y - r * 0.96, enemy.x - r * 0.78, enemy.y - r * 1.42);
            g.strokeLine(enemy.x + r * 0.22, enemy.y - r * 0.94, enemy.x + r * 0.78, enemy.y - r * 1.38);
        }
        if (enemy.kind == EnemyKind.HEARTWOOD_COLOSSUS && enemy.abilityTime > 0.0) {
            g.setStroke(Color.web("#FFCC80", 0.36));
            g.setLineWidth(3.2);
            g.strokeOval(enemy.x - r * 1.5, enemy.y - r * 1.5, r * 3.0, r * 3.0);
        }
        drawEnemyEyes(g, enemy.x, enemy.y - r * 0.1, r * 0.24);
        drawEnemyDamageCracks(g, enemy, wear, enemy.kind == EnemyKind.HEARTWOOD_COLOSSUS ? 6 : 4);
    }

    private void drawEnemyEyes(GraphicsContext g, double x, double y, double radius) {
        g.setFill(Color.web("#FFF8E1"));
        g.fillOval(x - radius * 2.1, y - radius, radius * 1.7, radius * 1.7);
        g.fillOval(x + radius * 0.4, y - radius, radius * 1.7, radius * 1.7);
        g.setFill(Color.web("#1C1B14"));
        g.fillOval(x - radius * 1.48, y - radius * 0.54, radius * 0.8, radius * 0.8);
        g.fillOval(x + radius * 1.0, y - radius * 0.54, radius * 0.8, radius * 0.8);
    }

    private void drawEnemyDamageCracks(GraphicsContext g, Enemy enemy, double wear, int count) {
        if (wear <= 0.08) {
            return;
        }
        g.setStroke(enemy.kind.accent.deriveColor(0, 1, 1, 0.48 + wear * 0.4));
        g.setLineWidth(1.6 + wear * 1.8);
        for (int i = 0; i < count; i++) {
            double offset = (i - (count - 1) * 0.5) * enemy.kind.radius * 0.28;
            double crackY = enemy.y - enemy.kind.radius * 0.36 + i * enemy.kind.radius * 0.18;
            g.strokeLine(enemy.x + offset - enemy.kind.radius * 0.12, crackY,
                    enemy.x + offset + enemy.kind.radius * (0.22 + wear * 0.18), crackY + enemy.kind.radius * (0.18 + wear * 0.14));
        }
        if (wear >= 0.42) {
            g.setFill(Color.web("#0F0A08", 0.18 + wear * 0.26));
            g.fillOval(enemy.x + enemy.kind.radius * 0.32, enemy.y - enemy.kind.radius * 0.22,
                    enemy.kind.radius * 0.42, enemy.kind.radius * 0.36);
        }
    }

    private void drawTowers(GraphicsContext g) {
        for (Tower tower : towers) {
            drawTower(g, tower, false);
        }
    }

    private void drawTower(GraphicsContext g, Tower tower, boolean ghostRender) {
        double spriteScale = towerSpriteScale(tower);
        double drawSize = 80.0 * spriteScale;
        if (!ghostRender && selectedTower == tower) {
            g.setStroke(Color.web("#FFF59D", 0.9));
            g.setLineWidth(3.0);
            g.strokeOval(tower.x - 38, tower.y - 38, 76, 76);
        }

        g.setFill(Color.rgb(0, 0, 0, ghostRender ? 0.15 : 0.2));
        g.fillOval(tower.x - 28, tower.y + 26, 56, 14);

        if (tower.kind == TowerBirdKind.OPIUMBIRD && (tower.upgrades[0] + tower.upgrades[1]) >= 1) {
            double aura = opiumAuraRadius(tower);
            g.setStroke(opiumAuraColor(tower).deriveColor(0, 1, 1, selectedTower == tower ? 0.22 : 0.10));
            g.setLineWidth(1.5);
            g.strokeOval(tower.x - aura, tower.y - aura, aura * 2, aura * 2);
        }

        drawTowerPerch(g, tower, spriteScale);
        drawTowerUpgradeCosmetics(g, tower, spriteScale, drawSize);
        drawTowerBirdSprite(g, tower, spriteScale, drawSize);
        drawTowerTierPips(g, tower);
    }

    private void drawTowerPerch(GraphicsContext g, Tower tower, double spriteScale) {
        double branchWidth = 76 + (tower.upgrades[0] + tower.upgrades[1]) * 6;
        double branchHeight = 16;
        g.setFill(Color.web("#4E342E"));
        g.fillRoundRect(tower.x - branchWidth * 0.5, tower.y + 20, branchWidth, branchHeight, 14, 14);
        g.setFill(Color.web("#6D4C41"));
        g.fillRoundRect(tower.x - branchWidth * 0.46, tower.y + 22, branchWidth * 0.92, branchHeight * 0.52, 12, 12);
        g.setStroke(Color.web("#A1887F", 0.62));
        g.setLineWidth(2.0);
        g.strokeLine(tower.x - branchWidth * 0.3, tower.y + 30, tower.x + branchWidth * 0.28, tower.y + 30);
        g.setFill(Color.web("#795548", 0.75));
        g.fillOval(tower.x - 7, tower.y + 30, 14, 12);
    }

    private void drawTowerBirdSprite(GraphicsContext g, Tower tower, double spriteScale, double drawSize) {
        Bird sprite = tower.sprite;
        configureTowerSpriteAppearance(tower);
        sprite.sizeMultiplier = spriteScale;
        sprite.suppressSelectEffects = true;
        sprite.facingRight = tower.lastAimDx >= 0.0;
        sprite.attackAnimationTimer = Math.max(0, (int) Math.round(tower.attackVisualTime * 60.0));
        sprite.leanTimer = tower.kind == TowerBirdKind.OPIUMBIRD ? Math.max(0, (int) Math.round((tower.attackVisualTime + 0.12) * 60.0)) : 0;
        sprite.x = tower.x - drawSize * 0.52;
        sprite.y = tower.y - drawSize * 0.60;
        sprite.draw(g);
    }

    private void configureTowerSpriteAppearance(Tower tower) {
        Bird sprite = tower.sprite;
        sprite.type = tower.kind.birdType;
        clearTowerSkinFlags(sprite);
        switch (tower.kind) {
            case PIGEON -> {
                if (tower.upgrades[0] >= 4 && tower.upgrades[0] > tower.upgrades[1]) {
                    sprite.isCitySkin = true;
                } else if (tower.upgrades[1] >= 4) {
                    sprite.isFreemanSkin = true;
                }
            }
            case EAGLE -> {
                if (tower.upgrades[0] >= 4) {
                    sprite.isClassicSkin = true;
                }
            }
            case OPIUMBIRD -> {
                if (tower.upgrades[1] >= 4 && tower.upgrades[1] >= tower.upgrades[0]) {
                    sprite.type = BirdGame3.BirdType.HEISENBIRD;
                } else if (tower.upgrades[0] >= 4) {
                    sprite.isClassicSkin = true;
                }
            }
            case BAT -> {
                if (tower.upgrades[0] >= 4 && tower.upgrades[0] > tower.upgrades[1]) {
                    sprite.isUmbraSkin = true;
                }
                if (tower.upgrades[1] >= 4 && tower.upgrades[1] >= tower.upgrades[0]) {
                    sprite.isResonanceSkin = true;
                }
            }
        }
    }

    private void clearTowerSkinFlags(Bird sprite) {
        sprite.isCitySkin = false;
        sprite.isNoirSkin = false;
        sprite.isFreemanSkin = false;
        sprite.isClassicSkin = false;
        sprite.isNovaSkin = false;
        sprite.isDuneSkin = false;
        sprite.isMintSkin = false;
        sprite.isCircuitSkin = false;
        sprite.isPrismSkin = false;
        sprite.isAuroraSkin = false;
        sprite.isBeaconSkin = false;
        sprite.isStormSkin = false;
        sprite.isSunflareSkin = false;
        sprite.isGlacierSkin = false;
        sprite.isTideSkin = false;
        sprite.isNullRockSkin = false;
        sprite.isEclipseSkin = false;
        sprite.isUmbraSkin = false;
        sprite.isResonanceSkin = false;
        sprite.isIroncladSkin = false;
        sprite.isSunforgeSkin = false;
    }

    private double towerSpriteScale(Tower tower) {
        double tierBonus = (tower.upgrades[0] + tower.upgrades[1]) * 0.022;
        return switch (tower.kind) {
            case PIGEON -> Math.clamp(0.76 + tierBonus, 0.76, 0.98);
            case EAGLE -> Math.clamp(0.88 + tierBonus, 0.88, 1.08);
            case OPIUMBIRD -> Math.clamp(0.80 + tierBonus, 0.80, 1.00);
            case BAT -> Math.clamp(0.82 + tierBonus, 0.82, 1.02);
        };
    }

    private void drawTowerUpgradeCosmetics(GraphicsContext g, Tower tower, double spriteScale, double drawSize) {
        switch (tower.kind) {
            case PIGEON -> drawPigeonUpgradeCosmetics(g, tower, spriteScale, drawSize);
            case EAGLE -> drawEagleUpgradeCosmetics(g, tower, spriteScale, drawSize);
            case OPIUMBIRD -> drawOpiumUpgradeCosmetics(g, tower, spriteScale, drawSize);
            case BAT -> drawBatUpgradeCosmetics(g, tower, spriteScale, drawSize);
        }
    }

    private void drawPigeonUpgradeCosmetics(GraphicsContext g, Tower tower, double s, double drawSize) {
        if (tower.upgrades[0] >= 1) {
            g.setStroke(Color.web("#FFCA28", 0.76));
            g.setLineWidth(2.2);
            g.strokeLine(tower.x - 12, tower.y + 14, tower.x + 10, tower.y + 20);
        }
        if (tower.upgrades[0] >= 2) {
            double capDir = tower.lastAimDx >= 0 ? 1.0 : -1.0;
            g.setFill(Color.web("#263238", 0.92));
            g.fillRoundRect(tower.x - 16, tower.y - drawSize * 0.40, 32, 10, 10, 10);
            g.fillPolygon(
                    new double[]{tower.x - 2 * capDir, tower.x + 20 * capDir, tower.x + 6 * capDir},
                    new double[]{tower.y - drawSize * 0.34, tower.y - drawSize * 0.30, tower.y - drawSize * 0.26},
                    3
            );
        }
        if (tower.upgrades[0] >= 3) {
            g.setFill(Color.web("#B0BEC5", 0.36));
            g.fillOval(tower.x - 28, tower.y - 24, 56, 40);
            g.setStroke(Color.web("#FFD54F", 0.72));
            g.setLineWidth(2.0);
            g.strokeOval(tower.x - 24, tower.y - 18, 48, 32);
            g.setStroke(Color.web("#546E7A", 0.92));
            g.setLineWidth(2.4);
            for (int i = 0; i < 4; i++) {
                double angle = i * Math.PI * 0.5 + Math.PI * 0.25;
                g.strokeLine(tower.x + Math.cos(angle) * 10, tower.y + Math.sin(angle) * 10,
                        tower.x + Math.cos(angle) * 24, tower.y + Math.sin(angle) * 18);
            }
        }
        if (tower.upgrades[0] >= 4) {
            double faceDir = tower.lastAimDx >= 0 ? 1.0 : -1.0;
            g.setFill(Color.web("#263238", 0.94));
            g.fillRoundRect(tower.x - 18, tower.y - drawSize * 0.46, 36, 12, 10, 10);
            g.fillRect(faceDir >= 0 ? tower.x - 2 : tower.x - 20, tower.y - drawSize * 0.34, 22, 5);
            g.setStroke(Color.web("#FFCC80"));
            g.setLineWidth(1.8);
            g.strokeLine(tower.x + 16 * faceDir, tower.y + 4, tower.x + 28 * faceDir, tower.y + 8);
        }
        if (tower.upgrades[1] >= 1) {
            g.setFill(Color.web("#455A64", 0.82));
            g.fillRoundRect(tower.x - 14, tower.y - drawSize * 0.34, 28, 10, 8, 8);
            g.setStroke(Color.web("#B3E5FC"));
            g.setLineWidth(1.2);
            g.strokeRoundRect(tower.x - 14, tower.y - drawSize * 0.34, 28, 10, 8, 8);
        }
        if (tower.upgrades[1] >= 2) {
            double faceDir = tower.lastAimDx >= 0 ? 1.0 : -1.0;
            g.setStroke(Color.web("#ECEFF1"));
            g.setLineWidth(1.8);
            g.strokeLine(tower.x + 14 * faceDir, tower.y - 1, tower.x + 26 * faceDir, tower.y + 3);
        }
        if (tower.upgrades[1] >= 3) {
            g.setFill(Color.web("#37474F", 0.84));
            g.fillRoundRect(tower.x - 18, tower.y - 6, 36, 18, 10, 10);
            g.setStroke(Color.web("#CFD8DC", 0.82));
            g.setLineWidth(2.0);
            g.strokeLine(tower.x - 10, tower.y + 2, tower.x - 24, tower.y + 10);
            g.strokeLine(tower.x, tower.y + 2, tower.x - 14, tower.y + 14);
            g.strokeLine(tower.x + 10, tower.y + 2, tower.x - 4, tower.y + 14);
        }
        if (tower.upgrades[1] >= 4) {
            g.setFill(Color.web("#0D1117", 0.88));
            g.fillRoundRect(tower.x - 14, tower.y - drawSize * 0.24, 28, 14, 8, 8);
            g.setStroke(Color.web("#90CAF9", 0.90));
            g.setLineWidth(2.0);
            g.strokeLine(tower.x - 14, tower.y - drawSize * 0.17, tower.x + 14, tower.y - drawSize * 0.17);
        }
    }

    private void drawEagleUpgradeCosmetics(GraphicsContext g, Tower tower, double s, double drawSize) {
        if (tower.upgrades[0] >= 1) {
            g.setFill(Color.web("#FFD54F", 0.76));
            g.fillPolygon(
                    new double[]{tower.x - 10, tower.x, tower.x + 10},
                    new double[]{tower.y - drawSize * 0.54, tower.y - drawSize * 0.72, tower.y - drawSize * 0.54},
                    3
            );
        }
        if (tower.upgrades[0] >= 2) {
            g.setFill(Color.web("#6D4C41", 0.72));
            g.fillRoundRect(tower.x - 26, tower.y - 6, 52, 10, 8, 8);
        }
        if (tower.upgrades[0] >= 3) {
            g.setFill(Color.web("#FFD54F", 0.58));
            g.fillPolygon(
                    new double[]{tower.x - 18, tower.x, tower.x + 18},
                    new double[]{tower.y + 8, tower.y - 24, tower.y + 8},
                    3
            );
            double faceDir = tower.lastAimDx >= 0 ? 1.0 : -1.0;
            g.setStroke(Color.web("#ECEFF1", 0.88));
            g.setLineWidth(2.4);
            g.strokeLine(tower.x - 6, tower.y - 2, tower.x + 28 * faceDir, tower.y - 10);
        }
        if (tower.upgrades[0] >= 4) {
            g.setStroke(Color.web("#FFF59D", 0.90));
            g.setLineWidth(2.4);
            g.strokeOval(tower.x - 30, tower.y - 34, 60, 60);
            g.strokeLine(tower.x - 24, tower.y - 28, tower.x - 10, tower.y - 40);
            g.strokeLine(tower.x + 24, tower.y - 28, tower.x + 10, tower.y - 40);
        }
        if (tower.upgrades[1] >= 1) {
            g.setStroke(Color.web("#90CAF9", 0.68));
            g.setLineWidth(2.0);
            double radius = 30 + tower.upgrades[1] * 3.5;
            g.strokeOval(tower.x - radius, tower.y - radius, radius * 2, radius * 2);
        }
        if (tower.upgrades[1] >= 2) {
            g.setFill(Color.web("#263238", 0.86));
            g.fillRoundRect(tower.x - 16, tower.y - drawSize * 0.28, 32, 12, 10, 10);
        }
        if (tower.upgrades[1] >= 3) {
            g.setStroke(Color.web("#E3F2FD", 0.72));
            g.setLineWidth(2.0);
            g.strokeLine(tower.x - 20, tower.y - 12, tower.x - 8, tower.y + 6);
            g.strokeLine(tower.x + 18, tower.y - 16, tower.x + 8, tower.y + 4);
            g.setFill(Color.web("#5D4037", 0.86));
            g.fillRoundRect(tower.x - 26, tower.y + 12, 52, 12, 8, 8);
        }
        if (tower.upgrades[1] >= 4) {
            g.setFill(Color.web("#8D6E63", 0.90));
            g.fillRoundRect(tower.x - 16, tower.y + 20, 32, 18, 6, 6);
            g.setStroke(Color.web("#FFF8E1"));
            g.setLineWidth(1.8);
            g.strokeLine(tower.x, tower.y + 20, tower.x, tower.y + 38);
            g.strokeLine(tower.x - 16, tower.y + 29, tower.x + 16, tower.y + 29);
        }
    }

    private void drawOpiumUpgradeCosmetics(GraphicsContext g, Tower tower, double s, double drawSize) {
        if (tower.upgrades[0] >= 1) {
            g.setFill(Color.web("#BA68C8", 0.20 + tower.upgrades[0] * 0.06));
            g.fillOval(tower.x - 28 - tower.upgrades[0] * 5, tower.y - 24 - tower.upgrades[0] * 3,
                    56 + tower.upgrades[0] * 10, 34 + tower.upgrades[0] * 7);
        }
        if (tower.upgrades[0] >= 2) {
            g.setStroke(Color.web("#4A148C", 0.62));
            g.setLineWidth(2.0);
            g.strokeArc(tower.x - 26, tower.y - 20, 52, 36, 200, 140, javafx.scene.shape.ArcType.OPEN);
            g.setStroke(Color.web("#B3E5FC", 0.82));
            g.setLineWidth(2.0);
            g.strokeLine(tower.x - 10, tower.y - 28, tower.x - 2, tower.y - 12);
            g.strokeLine(tower.x + 8, tower.y - 28, tower.x + 2, tower.y - 12);
        }
        if (tower.upgrades[0] >= 3) {
            g.setStroke(Color.web("#CE93D8", 0.76));
            g.setLineWidth(2.0);
            g.strokeArc(tower.x - 34, tower.y - 24, 68, 48, 180, 220, javafx.scene.shape.ArcType.OPEN);
        }
        if (tower.upgrades[0] >= 4) {
            g.setFill(Color.web("#512DA8", 0.20));
            g.fillOval(tower.x - 42, tower.y - 44, 84, 24);
            g.setStroke(Color.web("#B39DDB", 0.88));
            g.setLineWidth(2.2);
            g.strokeLine(tower.x - 18, tower.y - 34, tower.x - 6, tower.y - 48);
            g.strokeLine(tower.x + 18, tower.y - 34, tower.x + 6, tower.y - 48);
        }
        if (tower.upgrades[1] >= 1) {
            g.setFill(Color.web("#81D4FA", 0.76));
            for (int i = 0; i < Math.min(4, tower.upgrades[1] + 1); i++) {
                g.fillPolygon(
                        new double[]{tower.x - 16 + i * 10, tower.x - 12 + i * 10, tower.x - 8 + i * 10},
                        new double[]{tower.y + 16, tower.y + 6 + (i % 2) * 4, tower.y + 16},
                        3
                );
            }
        }
        if (tower.upgrades[1] >= 3) {
            g.setStroke(Color.web("#B3E5FC", 0.74));
            g.setLineWidth(2.2);
            g.strokeOval(tower.x - 32, tower.y - 32, 64, 64);
            g.setStroke(Color.web("#FF8A65", 0.72));
            g.setLineWidth(2.4);
            g.strokeArc(tower.x - 26, tower.y - 10, 52, 36, 200, 140, javafx.scene.shape.ArcType.OPEN);
        }
        if (tower.upgrades[1] >= 4) {
            g.setStroke(Color.web("#FFCC80", 0.88));
            g.setLineWidth(2.4);
            g.strokeOval(tower.x - 42, tower.y - 42, 84, 84);
            g.strokeLine(tower.x - 18, tower.y + 10, tower.x, tower.y - 18);
            g.strokeLine(tower.x + 18, tower.y + 10, tower.x, tower.y - 18);
        }
    }

    private void drawBatUpgradeCosmetics(GraphicsContext g, Tower tower, double s, double drawSize) {
        if (tower.upgrades[0] >= 1) {
            g.setStroke(Color.web("#5E35B1", 0.70));
            g.setLineWidth(2.4);
            g.strokeArc(tower.x - 34, tower.y - 28, 68, 40, 180, 180, javafx.scene.shape.ArcType.OPEN);
            g.setStroke(Color.web("#B39DDB", 0.72));
            g.strokeLine(tower.x - 12, tower.y + 10, tower.x, tower.y - 8);
            g.strokeLine(tower.x + 12, tower.y + 10, tower.x, tower.y - 8);
        }
        if (tower.upgrades[0] >= 2) {
            g.setStroke(Color.web("#D1C4E9", 0.86));
            g.setLineWidth(1.8);
            g.strokeOval(tower.x - 18, tower.y - 18, 36, 36);
        }
        if (tower.upgrades[0] >= 3) {
            g.setFill(Color.web("#311B92", 0.20));
            g.fillOval(tower.x - 30, tower.y - 26, 60, 36);
            g.setStroke(Color.web("#CE93D8", 0.90));
            g.setLineWidth(2.0);
            g.strokeLine(tower.x - 26, tower.y - 2, tower.x + 26, tower.y - 2);
        }
        if (tower.upgrades[0] >= 4) {
            g.setStroke(Color.web("#EDE7F6", 0.88));
            g.setLineWidth(2.0);
            for (int i = 0; i < 5; i++) {
                double angle = i * (Math.PI * 2.0 / 5.0) - Math.PI * 0.5;
                g.strokeLine(tower.x + Math.cos(angle) * 16, tower.y + Math.sin(angle) * 16,
                        tower.x + Math.cos(angle) * 30, tower.y + Math.sin(angle) * 30);
            }
        }
        if (tower.upgrades[1] >= 1) {
            g.setStroke(Color.web("#80DEEA", 0.72));
            g.setLineWidth(1.8);
            g.strokeOval(tower.x - 22, tower.y - 22, 44, 44);
        }
        if (tower.upgrades[1] >= 3) {
            g.setStroke(Color.web("#B2EBF2", 0.78));
            g.setLineWidth(2.2);
            g.strokeOval(tower.x - 32, tower.y - 32, 64, 64);
            g.setFill(Color.web("#37474F", 0.84));
            g.fillRoundRect(tower.x - 12, tower.y + 10, 24, 16, 8, 8);
        }
        if (tower.upgrades[1] >= 4) {
            g.setStroke(Color.web("#80CBC4", 0.90));
            g.setLineWidth(2.0);
            g.strokeArc(tower.x - 38, tower.y - 38, 76, 76, 200, 140, javafx.scene.shape.ArcType.OPEN);
            g.strokeArc(tower.x - 30, tower.y - 30, 60, 60, 20, 140, javafx.scene.shape.ArcType.OPEN);
        }
    }

    private void drawTowerTierPips(GraphicsContext g, Tower tower) {
        int total = tower.upgrades[0] + tower.upgrades[1];
        if (total <= 0) {
            return;
        }
        double startX = tower.x - Math.min(24, total * 4.5);
        for (int i = 0; i < tower.upgrades[0]; i++) {
            g.setFill(Color.web("#FFD166"));
            g.fillOval(startX + i * 10, tower.y + 38, 7, 7);
        }
        for (int i = 0; i < tower.upgrades[1]; i++) {
            g.setFill(Color.web("#80DEEA"));
            g.fillOval(startX + (tower.upgrades[0] + i) * 10, tower.y + 38, 7, 7);
        }
    }

    private void drawMapHud(GraphicsContext g) {
        g.setFill(Color.rgb(3, 13, 8, 0.54));
        g.fillRoundRect(18, 18, 220, 54, 20, 20);
        g.setStroke(Color.web("#A5D6A7", 0.72));
        g.setLineWidth(2.0);
        g.strokeRoundRect(18, 18, 220, 54, 20, 20);
        g.setFill(Color.web("#FFF8E1"));
        g.setFont(HUD_FONT);
        g.fillText("ROUND " + Math.clamp(nextRoundNumber, 1, rounds.size()) + " / " + rounds.size(), 34, 52);

        if (bannerTime > 0.0 && banner != null && !banner.isBlank()) {
            double boxW = Math.min(620, Math.max(260, 120 + banner.length() * 8.2));
            double boxX = (MAP_WIDTH - boxW) * 0.5;
            g.setFill(Color.rgb(0, 0, 0, 0.36));
            g.fillRoundRect(boxX, 18, boxW, 42, 18, 18);
            g.setStroke(Color.web("#80CBC4", 0.62));
            g.setLineWidth(2.0);
            g.strokeRoundRect(boxX, 18, boxW, 42, 18, 18);
            g.setFill(Color.web("#E8F5E9"));
            g.setFont(SMALL_FONT);
            g.fillText(banner, boxX + 18, 44);
        }

        if (toastVisible()) {
            double fade = Math.min(1.0, activeToastTime / 0.32);
            List<String> bodyLines = wrapToastBody(activeToast.body(), 76);
            double cardW = 660;
            double cardH = 56 + bodyLines.size() * 18;
            double cardX = (MAP_WIDTH - cardW) * 0.5;
            double cardY = MAP_HEIGHT - 34 - cardH + (1.0 - fade) * 24.0;
            g.setFill(Color.rgb(5, 10, 16, 0.76 * fade));
            g.fillRoundRect(cardX, cardY, cardW, cardH, 24, 24);
            g.setStroke(Color.web("#B2DFDB", 0.74 * fade));
            g.setLineWidth(2.4);
            g.strokeRoundRect(cardX, cardY, cardW, cardH, 24, 24);
            g.setFill(Color.web("#FFF59D", 0.98 * fade));
            g.setFont(TITLE_FONT);
            g.fillText(activeToast.title(), cardX + 24, cardY + 30);
            g.setFill(Color.web("#E8F5E9", 0.98 * fade));
            g.setFont(SMALL_FONT);
            double bodyY = cardY + 52;
            for (String line : bodyLines) {
                g.fillText(line, cardX + 24, bodyY);
                bodyY += 18;
            }
        }
    }

    private List<String> wrapToastBody(String text, int maxChars) {
        if (text == null || text.isBlank()) {
            return List.of("");
        }
        List<String> lines = new ArrayList<>();
        StringBuilder line = new StringBuilder();
        for (String word : text.split("\\s+")) {
            if (line.length() == 0) {
                line.append(word);
                continue;
            }
            if (line.length() + 1 + word.length() <= maxChars) {
                line.append(' ').append(word);
            } else {
                lines.add(line.toString());
                line.setLength(0);
                line.append(word);
            }
        }
        if (line.length() > 0) {
            lines.add(line.toString());
        }
        return lines;
    }

    private static List<RoundDefinition> buildRounds() {
        return List.of(
                round(90, group(EnemyKind.BLIGHT, 20, 0.62, 0.0)),
                round(110, group(EnemyKind.BLIGHT, 28, 0.52, 0.0)),
                round(130, group(EnemyKind.BLIGHT, 18, 0.54, 0.0), group(EnemyKind.SWIFT_BLIGHT, 10, 0.40, 2.2)),
                round(150, group(EnemyKind.BLIGHT, 18, 0.48, 0.0), group(EnemyKind.SHADE_BLIGHT, 12, 0.44, 2.8)),
                round(175, group(EnemyKind.SWIFT_BLIGHT, 16, 0.34, 0.0), group(EnemyKind.SHADE_BLIGHT, 10, 0.40, 1.8), group(EnemyKind.BLIGHT, 8, 0.34, 3.2)),
                round(205, group(EnemyKind.BRAMBLE_BLIGHT, 14, 0.56, 0.0), group(EnemyKind.BLIGHT, 16, 0.42, 1.6)),
                round(235, group(EnemyKind.BRAMBLE_BLIGHT, 16, 0.50, 0.0), group(EnemyKind.SWIFT_BLIGHT, 10, 0.34, 1.8)),
                round(270, group(EnemyKind.RESIN_BLIGHT, 8, 1.00, 0.0), group(EnemyKind.BLIGHT, 14, 0.32, 1.6)),
                round(305, group(EnemyKind.RESIN_BLIGHT, 10, 0.90, 0.0), group(EnemyKind.SHADE_BLIGHT, 16, 0.28, 2.0)),
                round(350, group(EnemyKind.ROOT_GOLEM, 1, 1.0, 0.0), group(EnemyKind.SWIFT_BLIGHT, 12, 0.28, 4.6)),
                round(400, group(EnemyKind.BRAMBLE_BLIGHT, 18, 0.42, 0.0), group(EnemyKind.SHADE_BLIGHT, 12, 0.28, 1.2)),
                round(455, group(EnemyKind.RESIN_BLIGHT, 12, 0.76, 0.0), group(EnemyKind.SWIFT_BLIGHT, 16, 0.26, 1.0)),
                round(515, group(EnemyKind.ROOT_GOLEM, 2, 6.0, 0.0), group(EnemyKind.BLIGHT, 18, 0.26, 2.4)),
                round(580, group(EnemyKind.SHADE_BLIGHT, 20, 0.24, 0.0), group(EnemyKind.BRAMBLE_BLIGHT, 18, 0.34, 1.8)),
                round(650, group(EnemyKind.RESIN_BLIGHT, 14, 0.66, 0.0), group(EnemyKind.SWIFT_BLIGHT, 12, 0.22, 2.0), group(EnemyKind.SHADE_BLIGHT, 10, 0.26, 3.0)),
                round(730, group(EnemyKind.ROOT_GOLEM, 2, 5.2, 0.0), group(EnemyKind.RESIN_BLIGHT, 16, 0.62, 1.4)),
                round(810, group(EnemyKind.IRONBARK_GOLEM, 1, 1.0, 0.0), group(EnemyKind.BLIGHT, 20, 0.22, 5.0)),
                round(900, group(EnemyKind.SWIFT_BLIGHT, 24, 0.20, 0.0), group(EnemyKind.SHADE_BLIGHT, 18, 0.22, 1.2)),
                round(1000, group(EnemyKind.IRONBARK_GOLEM, 1, 1.0, 0.0), group(EnemyKind.BRAMBLE_BLIGHT, 18, 0.30, 2.0), group(EnemyKind.RESIN_BLIGHT, 12, 0.58, 3.6)),
                round(1120, group(EnemyKind.HEARTWOOD_COLOSSUS, 1, 1.0, 0.0), group(EnemyKind.ROOT_GOLEM, 1, 1.0, 8.0), group(EnemyKind.SWIFT_BLIGHT, 22, 0.18, 2.8)),
                round(1240, group(EnemyKind.BRAMBLE_BLIGHT, 20, 0.28, 0.0), group(EnemyKind.SHADE_BLIGHT, 16, 0.20, 0.8), group(EnemyKind.SWIFT_BLIGHT, 16, 0.18, 2.6)),
                round(1370, group(EnemyKind.IRONBARK_GOLEM, 2, 6.0, 0.0), group(EnemyKind.BLIGHT, 18, 0.18, 3.4)),
                round(1510, group(EnemyKind.RESIN_BLIGHT, 16, 0.56, 0.0), group(EnemyKind.SHADE_BLIGHT, 24, 0.18, 2.2)),
                round(1660, group(EnemyKind.HEARTWOOD_COLOSSUS, 1, 1.0, 0.0), group(EnemyKind.ROOT_GOLEM, 2, 5.6, 4.4), group(EnemyKind.BRAMBLE_BLIGHT, 20, 0.24, 1.0)),
                round(1820, group(EnemyKind.SWIFT_BLIGHT, 30, 0.16, 0.0), group(EnemyKind.RESIN_BLIGHT, 18, 0.52, 1.6)),
                round(1990, group(EnemyKind.IRONBARK_GOLEM, 2, 5.4, 0.0), group(EnemyKind.SHADE_BLIGHT, 20, 0.18, 2.0), group(EnemyKind.BRAMBLE_BLIGHT, 12, 0.24, 3.8)),
                round(2170, group(EnemyKind.HEARTWOOD_COLOSSUS, 1, 1.0, 0.0), group(EnemyKind.SWIFT_BLIGHT, 24, 0.16, 1.8), group(EnemyKind.SHADE_BLIGHT, 18, 0.18, 3.8)),
                round(2360, group(EnemyKind.ROOT_GOLEM, 3, 4.8, 0.0), group(EnemyKind.RESIN_BLIGHT, 20, 0.48, 1.6)),
                round(2560, group(EnemyKind.IRONBARK_GOLEM, 2, 4.8, 0.0), group(EnemyKind.BRAMBLE_BLIGHT, 24, 0.22, 2.0), group(EnemyKind.SWIFT_BLIGHT, 20, 0.16, 4.0)),
                round(2780, group(EnemyKind.HEARTWOOD_COLOSSUS, 2, 9.0, 0.0), group(EnemyKind.IRONBARK_GOLEM, 1, 1.0, 5.6), group(EnemyKind.RESIN_BLIGHT, 18, 0.46, 2.0)),
                round(3000, group(EnemyKind.SWIFT_BLIGHT, 36, 0.15, 0.0), group(EnemyKind.SHADE_BLIGHT, 24, 0.16, 1.0), group(EnemyKind.BRAMBLE_BLIGHT, 18, 0.20, 2.2)),
                round(3240, group(EnemyKind.IRONBARK_GOLEM, 3, 4.8, 0.0), group(EnemyKind.ROOT_GOLEM, 2, 5.4, 1.8)),
                round(3500, group(EnemyKind.HEARTWOOD_COLOSSUS, 2, 8.8, 0.0), group(EnemyKind.RESIN_BLIGHT, 24, 0.42, 1.4), group(EnemyKind.SHADE_BLIGHT, 20, 0.16, 5.0)),
                round(3770, group(EnemyKind.ROOT_GOLEM, 4, 4.4, 0.0), group(EnemyKind.BRAMBLE_BLIGHT, 24, 0.20, 1.6), group(EnemyKind.SWIFT_BLIGHT, 18, 0.16, 4.0)),
                round(4060, group(EnemyKind.IRONBARK_GOLEM, 3, 4.2, 0.0), group(EnemyKind.RESIN_BLIGHT, 28, 0.40, 2.2)),
                round(4370, group(EnemyKind.HEARTWOOD_COLOSSUS, 2, 8.4, 0.0), group(EnemyKind.IRONBARK_GOLEM, 3, 4.8, 2.4), group(EnemyKind.SWIFT_BLIGHT, 24, 0.15, 5.0)),
                round(4700, group(EnemyKind.ROOT_GOLEM, 4, 4.0, 0.0), group(EnemyKind.SHADE_BLIGHT, 30, 0.14, 1.6), group(EnemyKind.BRAMBLE_BLIGHT, 24, 0.18, 4.0)),
                round(5050, group(EnemyKind.HEARTWOOD_COLOSSUS, 3, 7.8, 0.0), group(EnemyKind.IRONBARK_GOLEM, 2, 4.6, 3.8), group(EnemyKind.RESIN_BLIGHT, 20, 0.38, 2.2)),
                round(5420, group(EnemyKind.IRONBARK_GOLEM, 4, 3.8, 0.0), group(EnemyKind.HEARTWOOD_COLOSSUS, 2, 8.0, 2.6), group(EnemyKind.SWIFT_BLIGHT, 30, 0.14, 5.4), group(EnemyKind.SHADE_BLIGHT, 20, 0.16, 7.0)),
                round(5800, group(EnemyKind.HEARTWOOD_COLOSSUS, 3, 7.6, 0.0), group(EnemyKind.IRONBARK_GOLEM, 4, 4.0, 1.6), group(EnemyKind.RESIN_BLIGHT, 28, 0.36, 3.2), group(EnemyKind.SWIFT_BLIGHT, 34, 0.14, 6.2))
        );
    }

    private static List<String> buildRoundHints() {
        return List.of(
                "Round 1 opens on plain Blights. Learn the path before you buy fancy upgrades.",
                "Still basic Blights. Greed is legal until the trail stops being clean.",
                "Swift Blights are arriving. Fast towers or smart targeting stop their bursts.",
                "Shade Blights are hidden. Eyesight upgrades start mattering now.",
                "Mixed rush round. If leaks feel random, your coverage is too specialized.",
                "Bramble Blights wear bark armor. Weak chip shots take longer to matter.",
                "More bark, more speed. One tower for rushes and one for layers works well.",
                "Resin Blights split into Bramble Blights. Pierce starts paying real rent here.",
                "Hidden plus resin layers is the first real filter. Plan for both problems at once.",
                "Root Golem spotted. Big shells need burst before the children flood the path.",
                "A dense mid-round mix. Strong targeting helps prevent overkill on weak layers.",
                "Blight fact: the forest still calls this an infestation, not an invasion.",
                "Double Root round. If your damage is too spread out, the lane will show it.",
                "Shade traffic is high. Hidden detection on only one tower is usually not enough.",
                "Resin and speed together. This is where cleanup towers earn their keep.",
                "Two Root Golems with backup. Splash and knockback both have value here.",
                "Ironbark Golem incoming. It resists slowdowns better and cracks slowly.",
                "Fast hidden rush. Bat and sight upgrades can save a sloppy midgame.",
                "Ironbark plus split layers. Strong single-target and lane control both matter.",
                "Easy finale. One Colossus is manageable if its escorts do not clog your shots.",
                "Medium starts here. Higher prices mean dead-end upgrades hurt more.",
                "Ironbark duo. If the sniper path is weak, this round feels expensive.",
                "Hidden resin wave. Long-range towers without sight will stare uselessly.",
                "Colossus with support. Burn the giant, but do not let the children stack.",
                "Swift flood round. Area coverage beats panic clicking.",
                "More Ironbark and more clutter. This is a cross-upgrade honesty check.",
                "Colossus returns with a fast escort. First targeting is not always the answer.",
                "Root traffic is heavy. Big pierce and knockback can keep the trail breathable.",
                "Medium finale. Two Colossi means raw boss damage is no longer optional.",
                "Hard mode begins. Spend cash like the grove will invoice your mistakes.",
                "Everything is faster now. Range and uptime matter as much as raw damage.",
                "Three Ironbarks and friends. If your strong-target line is thin, fix it now.",
                "Double Colossus backed by resin. The lane wants to drown your cleanup towers.",
                "Root rush with bark shells behind it. This is a good round for splash to shine.",
                "Ironbark wall. Jokes aside, this is just a stress test for your build.",
                "Colossus phalanx. Sell dead towers if they are only watching.",
                "Bird fact: bats really do navigate by echo. These ones just added violence.",
                "Mass layered pressure. If your defense survives this, the grove owes it rent.",
                "Penultimate stand. Hidden, fast, and heavy layers all want attention at once.",
                "Final round. Colossi, Ironbarks, resin shells, and speed rushes. Spend every dollar."
        );
    }

    private static RoundDefinition round(int clearBonus, SpawnGroup... groups) {
        return new RoundDefinition(List.of(groups), clearBonus);
    }

    private static SpawnGroup group(EnemyKind kind, int count, double intervalSeconds, double startDelaySeconds) {
        return new SpawnGroup(kind, count, intervalSeconds, startDelaySeconds);
    }
}
