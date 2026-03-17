package com.example.birdgame3;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.effect.DropShadow;
import javafx.scene.effect.Glow;
import javafx.scene.input.KeyCode;
import javafx.scene.paint.Color;
import javafx.scene.shape.ArcType;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import com.example.birdgame3.BirdGame3.MapType;

import java.util.Arrays;
import java.util.Iterator;
import java.util.Random;

/**
 * Represents a playable bird character in Bird Fight 3.
 * Extracted from BirdGame3 inner class for better code organization.
 * Handles physics, abilities, AI, rendering, and collision detection.
 */
public class Bird {
    // Reference to main game instance
    private final BirdGame3 game;

    // === CORE PROPERTIES ===
    public double x, y, vx = 0, vy = 0;
    public BirdGame3.BirdType type;
    public boolean facingRight = true;
    public int playerIndex;
    public double health = 100;
    public String name;
    public double stunTime = 0;
    public int specialCooldown = 0;
    public int specialMaxCooldown = 120;
    public int attackCooldown = 0;
    public int attackAnimationTimer = 0;
    public boolean canDoubleJump = true;
    public boolean loungeActive = false;
    public boolean isCitySkin = false;
    public boolean isNoirSkin = false;
    public boolean isFreemanSkin = false;
    public boolean isClassicSkin = false;
    public boolean isNovaSkin = false;
    public boolean isDuneSkin = false;
    public boolean isMintSkin = false;
    public boolean isCircuitSkin = false;
    public boolean isPrismSkin = false;
    public boolean isAuroraSkin = false;
    public boolean isBeaconSkin = false;
    public boolean isSunflareSkin = false;
    public boolean isGlacierSkin = false;
    public boolean isTideSkin = false;
    public boolean isEclipseSkin = false;
    public boolean isUmbraSkin = false;
    public boolean suppressSelectEffects = false;
    public double loungeX, loungeY;
    public int diveTimer = 0;

    // === TITMOUSE ZIP DASH ===
    public boolean isZipping = false;
    public double zipTargetX = 0;
    public double zipTargetY = 0;
    public int zipTimer = 0;

    public boolean isGroundPounding = false;
    public int carrionSwarmTimer = 0;
    public int crowSwarmCooldown = 0;
    public boolean isFlying = false;

    // === OPIUM BIRD ===
    public int leanTimer = 0;
    public int leanCooldown = 0;
    public boolean isHigh = false;
    public int highTimer = 0;
    public int tauntCooldown = 0;
    public int tauntTimer = 0;
    public int cooldownFlash = 0;
    public int currentTaunt = 0;
    public int eagleDiveCountdown = 0;
    public boolean eagleDiveActive = false;
    public boolean eagleAscentActive = false;
    public int eagleAscentFrames = 0;
    private final boolean[] eagleAscentHit = new boolean[4];
    public int bladeStormFrames = 0;
    private static final int RAZORBILL_DASH_FRAMES = 26;
    private static final double RAZORBILL_DASH_SPEED = 22.0;
    private double razorbillDashVX = 0.0;
    private double razorbillDashVY = 0.0;
    private final boolean[] razorbillDashHit = new boolean[4];
    public int plungeTimer = 0;
    public boolean batHanging = false;
    private Platform batHangPlatform = null;
    public int batEchoTimer = 0;
    private int batHangLockTimer = 0;

    public boolean isBlocking = false;
    public int blockCooldown = 0;

    // === VINE SWINGING ===
    public SwingingVine attachedVine = null;
    public boolean onVine = false;

    // === POWER-UP BUFFS ===
    public double speedMultiplier = 1.0;
    public double powerMultiplier = 1.0;
    public double sizeMultiplier = 1.0;
    public double baseSpeedMultiplier = 1.0;
    public double basePowerMultiplier = 1.0;
    public double baseSizeMultiplier = 1.0;
    public int speedTimer = 0;
    public int rageTimer = 0;
    public int shrinkTimer = 0;
    public int titanTimer = 0;
    public boolean titanActive = false;
    public int neonRushTimer = 0;
    public int thermalTimer = 0;
    public double thermalLift = 0.0;
    public int overchargeAttackTimer = 0;

    // === NECTAR BOOST (Jungle) ===
    public double speedBoostTimer = 0;
    public double hoverRegenTimer = 0;
    public double hoverRegenMultiplier = 1.0;

    // === MOCKINGBIRD LOUNGE ===
    public int loungeHealth = 0;
    private static final int LOUNGE_MAX_HEALTH = 100;
    private static final double LOUNGE_HEAL_PER_SECOND = 12.0;
    public int loungeDamageFlash = 0;

    // === VINE GRAPPLE ===
    private int grappleTimer = 0;
    private int grappleUses = 0;
    private boolean isGrappling = false;
    private double grappleTargetX, grappleTargetY;

    private boolean enlargedByPlunge = false;
    private static final double LIMITED_FLIGHT_MAX = 34.0; // ~0.55s at 60fps
    private double limitedFlightFuel = LIMITED_FLIGHT_MAX;
    private double penguinIceFxTimer = 0;
    private int penguinDashDamageTimer = 0;
    private final boolean[] penguinDashHit = new boolean[4];
    private int hummingFrenzyTimer = 0;
    private final int[] hummingFrenzyHitCooldown = new int[4];
    private int phoenixAfterburnTimer = 0;
    private final int[] phoenixAfterburnHitCooldown = new int[4];
    private boolean phoenixRebornUsed = false;
    private boolean phoenixRebornActive = false;
    private static final double PHOENIX_REBORN_HEALTH = 20.0;
    private static final double PHOENIX_REBORN_SIZE_SCALE = 0.75;
    private static final double PHOENIX_REBORN_POWER_SCALE = 0.8;
    private static final double PHOENIX_REBORN_SPEED_SCALE = 1.35;
    private static final double PHOENIX_REBORN_DAMAGE_SCALE = 0.85;

    private final Random random = new Random();

    /**
     * Create a new bird character.
     * @param startX Starting x position
     * @param type Bird type (species)
     * @param playerIndex Player index (0-3)
     * @param game Reference to main game instance
     */
    public Bird(double startX, BirdGame3.BirdType type, int playerIndex, BirdGame3 game) {
        this.game = game;
        this.x = startX;
        this.y = game.GROUND_Y - 200;
        this.type = type;
        this.playerIndex = playerIndex;
        this.name = (game.isAI != null && game.isAI[playerIndex] ? "AI" : "P") + (playerIndex + 1) + ": " + type.name;

        if (type == BirdGame3.BirdType.PELICAN) {
            baseSizeMultiplier = 1.2;
        }
        sizeMultiplier = baseSizeMultiplier;
    }

    public void setBaseMultipliers(double size, double power, double speed) {
        baseSizeMultiplier = size;
        basePowerMultiplier = power;
        baseSpeedMultiplier = speed;
        sizeMultiplier = size;
        powerMultiplier = power;
        speedMultiplier = speed;
    }

    public boolean isOnGround() {
        double bottom = y + 80 * sizeMultiplier;
        if (game.selectedMap != MapType.BATTLEFIELD && bottom >= game.GROUND_Y) return true;
        for (Platform p : game.platforms) {
            boolean isCaveCeiling = game.selectedMap == MapType.CAVE &&
                    p.y <= 1 && p.h >= 60 && p.w >= game.WORLD_WIDTH - 10;
            if (isCaveCeiling) continue;
            if (x + 40 * sizeMultiplier >= p.x && x + 40 * sizeMultiplier <= p.x + p.w &&
                    bottom >= p.y && bottom <= p.y + p.h &&
                    y <= p.y + 1)
                return true;
        }
        return false;
    }

    private void loungeHeal() {
        if (type == BirdGame3.BirdType.MOCKINGBIRD && loungeActive && loungeHealth > 0) {
            double birdCenterX = x + 40;
            double birdCenterY = y + 40;
            double distToLounge = Math.hypot(birdCenterX - loungeX, birdCenterY - loungeY);

            if (distToLounge < 70) {
                health += LOUNGE_HEAL_PER_SECOND / 60.0;
                if (health > getMaxHealth()) health = getMaxHealth();
            }
        }
    }

    private void handleVerticalCollision() {
        if (onVine || batHanging) return;

        boolean hit = false;
        double newY = y;

        for (Platform p : game.platforms) {
            boolean isCaveCeiling = game.selectedMap == MapType.CAVE &&
                    p.y <= 1 && p.h >= 60 && p.w >= game.WORLD_WIDTH - 10;

            if (isCaveCeiling) {
                // Solid cave ceiling: block upward movement from below but never allow standing on top.
                if (x + 40 * sizeMultiplier >= p.x && x + 40 * sizeMultiplier <= p.x + p.w &&
                        y < p.y + p.h && y > p.y - 50 && vy < 0) {
                    y = p.y + p.h + 0.5;
                    vy = 0;
                }
                continue;
            }

            // Land only when descending onto the top surface to avoid snapping onto platforms from below.
            if (x + 40 * sizeMultiplier >= p.x && x + 40 * sizeMultiplier <= p.x + p.w &&
                    y + 80 * sizeMultiplier > p.y && y < p.y + p.h &&
                    vy >= 0 && y <= p.y) {
                newY = p.y - 80 * sizeMultiplier;
                hit = true;
                break;
            }
        }

        if (!hit && game.selectedMap != MapType.BATTLEFIELD && y + 80 * sizeMultiplier > game.GROUND_Y) {
            newY = game.GROUND_Y - 80 * sizeMultiplier;
            hit = true;
        }

        if (hit) {
            y = newY;
            if (vy > 0) vy = 0;
            canDoubleJump = true;

            // === TURKEY GROUND POUND ===
            if (type == BirdGame3.BirdType.TURKEY && isGroundPounding) {
                handleTurkeyGroundPound();
            }
        }
    }

    private void handleTurkeyGroundPound() {
        isGroundPounding = false;
        game.shakeIntensity = 22;
        game.hitstopFrames = 15;
        game.addToKillFeed(name.split(":")[0].trim() + " SLAMMED THE GROUND!");

        for (Bird other : game.players) {
            if (other == null || other == this || other.health <= 0) continue;
            if (!canDamageTarget(other)) continue;
            double dx = other.x - x;
            if (Math.abs(dx) < 280 && Math.abs(other.y - y) < 180) {
                int dmg = (int) (28 * powerMultiplier);
                double oldHealth = other.health;
                double dealtDamage = applyDamageTo(other, dmg);

                game.damageDealt[playerIndex] += (int) dealtDamage;
                game.recordSpecialImpact(playerIndex, (int) dealtDamage, dealtDamage > 0);
                boolean isKill = oldHealth > 0 && other.health <= 0;
                if (isKill) {
                    game.eliminations[playerIndex]++;
                    game.groundPounds[playerIndex]++;
                    if (game.isSfxEnabled() && game.zombieFallingClip != null) game.zombieFallingClip.play();
                }

                if (dealtDamage >= 30) {
                    game.triggerFlash(Math.min(1.0, dealtDamage / 55.0), isKill);
                } else if (dealtDamage >= 15) {
                    game.triggerFlash(Math.min(0.75, dealtDamage / 40.0), false);
                }

                if (dealtDamage >= 5) {
                    spawnDamageParticles(other, dealtDamage);
                    logDamageKillFeed(dealtDamage, isKill, other);
                }

                if (dealtDamage >= 20) {
                    game.shakeIntensity = Math.min(20, dealtDamage / 2.0);
                    game.hitstopFrames = (int) Math.min(12, 4 + dealtDamage / 5);
                    game.playHitSound(dealtDamage);
                }

                other.vx += dx > 0 ? 20 : -20;
                other.vy -= 12;
            }
        }

        // Big dust cloud
        for (int i = 0; i < 80; i++) {
            double angle = i / 80.0 * Math.PI * 2;
            double speed = 4 + Math.random() * 10;
            double vx = Math.cos(angle) * speed;
            double vy = Math.sin(angle) * speed - 5;
            Color c = Math.random() < 0.7 ? Color.SADDLEBROWN : Color.SANDYBROWN;
            game.particles.add(new Particle(x + 40, y + 70, vx, vy, c));
        }

        for (int i = 0; i < 20; i++) {
            double vx = (Math.random() - 0.5) * 20;
            double vy = -8 - Math.random() * 10;
            game.particles.add(new Particle(x + 40, y + 70, vx, vy, Color.GRAY));
        }
    }

    private void spawnDamageParticles(Bird target, double damage) {
        double particleCount = Math.min(50, 3 + damage * 2);
        for (int i = 0; i < particleCount; i++) {
            double angle = (Math.random() * Math.PI * 2) - Math.PI / 4;
            double speed = 3 + Math.random() * (damage * 0.3);
            double vx = Math.cos(angle) * speed;
            double vy = Math.sin(angle) * speed - 3;
            Color c = Math.random() < 0.6 ? Color.WHITE : Color.rgb(220, 20, 20, 0.8);
            game.particles.add(new Particle(target.x + 40 + (Math.random() - 0.5) * 20,
                    target.y + 40 + (Math.random() - 0.5) * 20, vx, vy, c));
        }
    }

    private void logDamageKillFeed(double damage, boolean isKill, Bird victim) {
        String attacker = name.split(":")[0].trim();
        String victimName = victim.name.split(":")[0].trim();
        String verb = (type == BirdGame3.BirdType.RAZORBILL && bladeStormFrames > 0)
                ? "CARVED" : (damage >= 35 ? "BRUTALIZED" : damage >= 25 ? "SMASHED" : "hit");
        game.addToKillFeed(attacker + " " + verb + " " + victimName + "! -" + (int) damage + " HP");

        if (isKill) {
            game.addToKillFeed("ELIMINATED " + victimName + "!");
        }
    }

    private void attack() {
        if (health <= 0) return;
        double range = 120 * sizeMultiplier;
        int dmg = (int) (type.power * powerMultiplier);
        for (Bird other : game.players) {
            if (other == null || other == this || other.health <= 0) continue;
            if (!canDamageTarget(other)) continue;

            double dist = Math.abs(x - other.x);
            if (dist < range && Math.abs(y - other.y) < 100 * sizeMultiplier) {
                processBirdAttack(other, dmg);
            }
        }

        // === LOUNGE CAN BE HIT ===
        attackLounge();
        attackCrows(range, dmg);
    }

    private void attackCrows(double range, int dmg) {
        double reach = range + 35 * sizeMultiplier;
        double verticalReach = 120 * sizeMultiplier;
        int kills = 0;

        for (Iterator<CrowMinion> it = game.crowMinions.iterator(); it.hasNext(); ) {
            CrowMinion crow = it.next();
            if (crow.owner == this) continue;

            double dx = crow.x - (x + 40 * sizeMultiplier);
            double dy = crow.y - (y + 40 * sizeMultiplier);
            if (Math.abs(dx) > reach || Math.abs(dy) > verticalReach) continue;
            if (facingRight && dx < -35 * sizeMultiplier) continue;
            if (!facingRight && dx > 35 * sizeMultiplier) continue;

            crow.life -= Math.max(1, dmg / 10);
            if (crow.life > 0) continue;

            it.remove();
            kills++;
            Color hitColor = crow.owner == null ? Color.rgb(60, 0, 0) : Color.rgb(30, 30, 40);
            for (int i = 0; i < 14; i++) {
                double angle = Math.random() * Math.PI * 2;
                double speed = 4 + Math.random() * 10;
                game.particles.add(new Particle(
                        crow.x,
                        crow.y,
                        Math.cos(angle) * speed,
                        Math.sin(angle) * speed - 3,
                        hitColor
                ));
            }
        }

        if (kills > 0) {
            String source = name.split(":")[0].trim();
            game.addToKillFeed(source + " swatted " + kills + " crow" + (kills > 1 ? "s" : "") + "!");
            game.scores[playerIndex] += kills * 2;
        }
    }

    private void processBirdAttack(Bird other, int dmg) {
        double kb = type.power * (facingRight ? 1 : -1) * 1.8;

        if (other.isBlocking) {
            dmg = (int)(dmg * 0.45);
            game.addToKillFeed(other.name.split(":")[0].trim() + " BLOCKED the attack! -" + dmg + " HP");
            kb *= 0.35;

            if (other.facingRight == (x < other.x) && random.nextDouble() < 0.25) {
                stunTime = 35;
                game.addToKillFeed(other.name.split(":")[0].trim() + " PARRIED! Attacker stunned!");
            }
        }

        other.vx += kb;
        other.vy -= 5;
        double oldHealth = other.health;
        double dealtDamage = applyDamageTo(other, dmg);

        game.damageDealt[playerIndex] += (int) dealtDamage;
        if (other.health <= 0 && oldHealth > 0) {
            game.eliminations[playerIndex]++;
            game.checkAchievements(this);
            if (game.isSfxEnabled() && game.zombieFallingClip != null) game.zombieFallingClip.play();
            game.scores[playerIndex] += 50;
        }
        game.scores[playerIndex] += (int) dealtDamage / 2;

        if (dealtDamage >= 5) {
            spawnDamageParticles(other, dealtDamage);
            logDamageKillFeed(dealtDamage, other.health <= 0, other);
        }

        if (dealtDamage >= 20) {
            game.shakeIntensity = Math.min(20, dealtDamage / 2.0);
            game.hitstopFrames = (int) Math.min(12, 4 + dealtDamage / 5);
            game.playHitSound(dealtDamage);
        }
    }

    private void attackLounge() {
        for (Bird target : game.players) {
            if (target == null || target.type != BirdGame3.BirdType.MOCKINGBIRD || !target.loungeActive || target.loungeHealth <= 0)
                continue;
            if (!canDamageTarget(target)) continue;

            double distToLounge = Math.hypot(target.loungeX - (x + 40), target.loungeY - (y + 40));
            if (distToLounge < 130) {
                int loungeDmg = (int) (type.power * 2.0 * powerMultiplier);
                target.loungeHealth -= loungeDmg;
                target.loungeDamageFlash = 15;

                game.addToKillFeed(name.split(":")[0].trim() + " smashed the Lounge! -" + loungeDmg + " HP");

                for (int i = 0; i < 30; i++) {
                    double angle = Math.random() * Math.PI * 2;
                    game.particles.add(new Particle(
                            target.loungeX + Math.cos(angle) * 50,
                            target.loungeY + Math.sin(angle) * 40,
                            Math.cos(angle) * 10, Math.sin(angle) * 10 - 4,
                            Color.LIME));
                }

                if (target.loungeHealth <= 0) {
                    target.loungeActive = false;
                    target.loungeHealth = 0;
                    game.addToKillFeed("THE LOUNGE HAS BEEN OBLITERATED!");
                    game.shakeIntensity = 30;
                    game.hitstopFrames = 18;
                    for (int i = 0; i < 120; i++) {
                        double angle = i / 120.0 * Math.PI * 2;
                        double speed = 8 + Math.random() * 14;
                        game.particles.add(new Particle(target.loungeX, target.loungeY,
                                Math.cos(angle) * speed, Math.sin(angle) * speed - 5,
                                Math.random() < 0.5 ? Color.LIME : Color.GREENYELLOW));
                    }
                }
                break;
            }
        }
    }

    private void special() {
        if (health <= 0 || specialCooldown > 0) {
            if (specialCooldown > 0 && !game.isAI[playerIndex]) {
                this.cooldownFlash = 15;
            }
            return;
        }

        if (game.isSfxEnabled()) {
            if (type == BirdGame3.BirdType.RAZORBILL && game.vaseBreakingClip != null) {
                game.vaseBreakingClip.play();
            } else if (game.jalapenoClip != null) {
                game.jalapenoClip.play();
            }
        }
        game.specialsUsed[playerIndex]++;

        switch (type) {
            case PIGEON -> specialPigeon();
            case EAGLE -> specialEagle();
            case FALCON -> specialFalcon();
            case PHOENIX -> specialPhoenix();
            case HUMMINGBIRD -> specialHummingbird();
            case TURKEY -> specialTurkey();
            case PENGUIN -> specialPenguin();
            case SHOEBILL -> specialShoebill();
            case MOCKINGBIRD -> specialMockingbird();
            case RAZORBILL -> specialRazorbill();
            case GRINCHHAWK -> specialGrinchhawk();
            case VULTURE -> specialVulture();
            case OPIUMBIRD -> specialOpiumBird();
            case HEISENBIRD -> specialHeisenbird();
            case TITMOUSE -> specialTitmouse();
            case BAT -> specialBat();
            case PELICAN -> specialPelican();
            case RAVEN -> specialRaven();
        }
    }

    private void specialPigeon() {
        health = Math.min(getMaxHealth(), health + 20);
        canDoubleJump = true;
        specialCooldown = 540;
        specialMaxCooldown = 540;
        game.addToKillFeed(name.split(":")[0].trim() + " HEAL BURST! +20 HP + DOUBLE JUMP");
        game.shakeIntensity = 12;
        game.hitstopFrames = 8;
        for (int i = 0; i < 40; i++) {
            double angle = Math.random() * Math.PI * 2;
            double speed = 3 + Math.random() * 8;
            game.particles.add(new Particle(x + 40, y + 40,
                    Math.cos(angle) * speed, Math.sin(angle) * speed - 4,
                    Color.LIME.deriveColor(0, 1, 1, 0.8)));
        }
    }

    private void specialEagle() {
        diveTimer = 120;
        specialCooldown = 780;
        specialMaxCooldown = 780;
        eagleDiveActive = true;
        eagleAscentActive = false;
        eagleAscentFrames = 0;
        Arrays.fill(eagleAscentHit, false);

        game.shakeIntensity = 16;
        game.hitstopFrames = 9;
        game.addToKillFeed("SKREEEEEEEE!!! " + name.split(":")[0].trim() + " IS DIVING FROM THE HEAVENS!");

        for (int i = 0; i < 100; i++) {
            double angle = Math.atan2(vy, vx) + Math.PI;
            double dist = i * 10;
            game.particles.add(new Particle(
                    x + 40 + Math.cos(angle) * dist,
                    y + 40 + Math.sin(angle) * dist,
                    0, 0,
                    Color.CRIMSON.deriveColor(0, 1, 1, 1.0 - i / 100.0)
            ));
        }

        double predictX = x + vx * 40;
        for (int i = -15; i <= 15; i++) {
            game.particles.add(new Particle(predictX + i * 60, game.GROUND_Y - 20, 0, -5 - Math.random() * 8, Color.ORANGERED.brighter()));
        }

        vy = isOnGround() ? -8 : Math.max(vy, 14);
        vx *= 0.7;
        eagleDiveCountdown = 0;
    }

    private void specialFalcon() {
        diveTimer = 92;
        specialCooldown = 660;
        specialMaxCooldown = 660;
        eagleDiveActive = true;
        eagleAscentActive = false;
        eagleAscentFrames = 0;
        Arrays.fill(eagleAscentHit, false);

        game.shakeIntensity = Math.max(game.shakeIntensity, 12);
        game.hitstopFrames = Math.max(game.hitstopFrames, 7);
        game.addToKillFeed(name.split(":")[0].trim() + " LOCKED IN A FALCON DIVE!");

        for (int i = 0; i < 78; i++) {
            double angle = Math.atan2(vy, vx) + Math.PI;
            double dist = i * 7.5;
            Color c = i % 2 == 0 ? Color.web("#FF7043") : Color.web("#FFE082");
            game.particles.add(new Particle(
                    x + 40 + Math.cos(angle) * dist,
                    y + 40 + Math.sin(angle) * dist,
                    0, 0,
                    c.deriveColor(0, 1, 1, 1.0 - i / 90.0)
            ));
        }

        vy = isOnGround() ? -6 : Math.max(vy, 13);
        vx += (facingRight ? 1 : -1) * 8;
        eagleDiveCountdown = 0;
    }

    private void specialPhoenix() {
        canDoubleJump = true;
        vx += (facingRight ? 1 : -1) * 12;
        vy = Math.min(vy - 8, -10);

        powerMultiplier = Math.max(powerMultiplier, basePowerMultiplier * 1.2);
        rageTimer = Math.max(rageTimer, 220);
        speedMultiplier = Math.max(speedMultiplier, baseSpeedMultiplier * 1.2);
        speedTimer = Math.max(speedTimer, 220);

        specialCooldown = 600;
        specialMaxCooldown = 600;
        phoenixAfterburnTimer = 48;
        Arrays.fill(phoenixAfterburnHitCooldown, 0);
        game.addToKillFeed(name.split(":")[0].trim() + " UNLEASHED REBIRTH BLAZE!");
        game.shakeIntensity = Math.max(game.shakeIntensity, 16);
        game.hitstopFrames = Math.max(game.hitstopFrames, 8);

        for (Bird other : game.players) {
            if (!canDamageTarget(other)) continue;
            double dx = (other.x + 40) - (x + 40);
            double dy = (other.y + 40) - (y + 40);
            double dist = Math.hypot(dx, dy);
            if (dist > 300) continue;

            int dmg = dist < 150 ? 16 : (dist < 230 ? 12 : 9);
            double oldHealth = other.health;
            int dealt = (int) applyDamageTo(other, dmg);
            game.damageDealt[playerIndex] += dealt;
            game.recordSpecialImpact(playerIndex, dealt, dealt > 0);
            if (other.health <= 0 && oldHealth > 0) game.eliminations[playerIndex]++;

            double safeDist = Math.max(0.001, dist);
            other.vx += dx / safeDist * 12;
            other.vy -= 7;
        }

        // Flame lances shoot outward from Phoenix's body.
        for (int ray = 0; ray < 14; ray++) {
            double baseAngle = ray / 14.0 * Math.PI * 2;
            for (int seg = 0; seg < 8; seg++) {
                double angle = baseAngle + (Math.random() - 0.5) * 0.16;
                double speed = 8 + seg * 1.6 + Math.random() * 3.5;
                double spawnDist = 10 + seg * 5.0;
                Color c = Math.random() < 0.62 ? Color.ORANGERED : Color.GOLD;
                game.particles.add(new Particle(
                        x + 40 + Math.cos(angle) * spawnDist,
                        y + 40 + Math.sin(angle) * spawnDist,
                        Math.cos(angle) * speed,
                        Math.sin(angle) * speed - 5.2,
                        c.deriveColor(0, 1, 1, 0.9)
                ));
            }
        }
    }

    private void specialHummingbird() {
        hummingFrenzyTimer = 105;
        Arrays.fill(hummingFrenzyHitCooldown, 0);
        canDoubleJump = true;
        vy = Math.min(vy - type.jumpHeight * 0.45, -type.jumpHeight * 0.9);
        vx += (facingRight ? 1 : -1) * 9;
        health = Math.min(getMaxHealth(), health + 6);
        specialCooldown = 390;
        specialMaxCooldown = 390;
        game.addToKillFeed(name.split(":")[0].trim() + " UNLEASHED NECTAR FRENZY!");
        for (int i = 0; i < 55; i++) {
            Color c = Math.random() < 0.55 ? Color.CYAN.brighter() : Color.YELLOW.brighter();
            game.particles.add(new Particle(x + 40, y + 40,
                    (Math.random() - 0.5) * 22, -8 - Math.random() * 12,
                    c));
        }
    }

    private void specialTurkey() {
        vy = -type.jumpHeight * 1.45;
        isGroundPounding = true;
        specialCooldown = 450;
        specialMaxCooldown = 450;
    }

    private void specialPenguin() {
        boolean grounded = isOnGround();
        vx = (facingRight ? 1 : -1) * (grounded ? 17.0 : 14.0);
        // Mobility special: gives penguin a reliable vertical burst to reach upper lanes.
        vy = grounded ? -type.jumpHeight * 2.0 : Math.min(vy - 11.0, -16.0);
        canDoubleJump = true;
        specialCooldown = 135;
        specialMaxCooldown = 135;
        penguinIceFxTimer = 70;
        penguinDashDamageTimer = 18;
        Arrays.fill(penguinDashHit, false);
        game.addToKillFeed(name.split(":")[0].trim() + " ICE JUMP DASH!");
        for (int i = 0; i < 90; i++) {
            game.particles.add(new Particle(x + 40 + (Math.random() - 0.5) * 100,
                    y + 70, (Math.random() - 0.5) * 10, -5 - Math.random() * 9,
                    Color.CYAN.deriveColor(0, 1, 1, 0.7)));
        }
    }

    private void specialShoebill() {
        for (Bird other : game.players) {
            if (!canDamageTarget(other)) continue;
            double dist = Math.hypot(other.x - x, other.y - y);
            if (dist < 320) {
                other.stunTime = 120;
            }
        }
        specialCooldown = 780;
        specialMaxCooldown = 780;
        game.addToKillFeed(name.split(":")[0].trim() + " DEATH STARE! Nearby birds stunned!");
        game.shakeIntensity = 20;
    }

    private void specialMockingbird() {
        loungeActive = true;
        loungeX = x + 40;
        loungeY = y + 40;
        loungeHealth = 100;
        specialCooldown = 660;
        specialMaxCooldown = 660;
        game.addToKillFeed(name.split(":")[0].trim() + " opened the LOUNGE!");
    }

    private void specialRazorbill() {
        specialCooldown = 780;
        specialMaxCooldown = 780;

        game.shakeIntensity = 18;
        game.hitstopFrames = 10;
        game.addToKillFeed("RAZOR DASH! " + name.split(":")[0].trim() + " PIERCES THE SKY!");

        double dirX = vx;
        double dirY = vy;
        double mag = Math.hypot(dirX, dirY);
        if (mag < 0.35) {
            dirX = facingRight ? 1 : -1;
            dirY = 0;
            mag = 1.0;
        }
        double dashSpeed = Math.max(12.0, RAZORBILL_DASH_SPEED * speedMultiplier);
        razorbillDashVX = dirX / mag * dashSpeed;
        razorbillDashVY = dirY / mag * dashSpeed;
        vx = razorbillDashVX;
        vy = razorbillDashVY;
        bladeStormFrames = RAZORBILL_DASH_FRAMES;
        Arrays.fill(razorbillDashHit, false);

        double trailAngle = Math.atan2(razorbillDashVY, razorbillDashVX);
        for (int i = 0; i < 60; i++) {
            double angle = trailAngle + (Math.random() - 0.5) * 0.7;
            double speed = 6 + Math.random() * 10;
            double back = 20 + Math.random() * 70;
            game.particles.add(new Particle(
                    x + 40 - Math.cos(angle) * back,
                    y + 40 - Math.sin(angle) * back,
                    Math.cos(angle) * speed,
                    Math.sin(angle) * speed,
                    Color.CYAN.brighter()
            ));
        }
    }

    private void specialGrinchhawk() {
        int stolen = 0;
        for (Bird other : game.players) {
            if (!canDamageTarget(other)) continue;
            int take = (int) Math.min(8 + (health > 80 ? 4 : 0), other.health);
            double oldHealth = other.health;
            int dealt = (int) applyDamageTo(other, take);
            stolen += dealt;
            game.damageDealt[playerIndex] += dealt;
            game.recordSpecialImpact(playerIndex, dealt, dealt > 0);
            if (other.health <= 0 && oldHealth > 0) game.eliminations[playerIndex]++;
            game.addToKillFeed(name.split(":")[0].trim() + " STOLE " + dealt + " HP from " + other.name.split(":")[0].trim() + "!");
        }
        health = Math.min(getMaxHealth(), health + stolen);
        specialCooldown = 840;
        specialMaxCooldown = 840;
        game.shakeIntensity = 20;
    }

    private void specialVulture() {
        crowSwarmCooldown = 1080;
        specialCooldown = 1080;
        specialMaxCooldown = 1080;
        game.addToKillFeed(name.split(":")[0].trim() + " SUMMONS THE MURDER!");

        int crowCount = 8 + random.nextInt(5);
        for (int i = 0; i < crowCount; i++) {
            double angle = Math.random() * Math.PI * 2;
            double dist = 300 + Math.random() * 1200;
            double spawnX = x + 40 + Math.cos(angle) * dist;
            double spawnY = y + 40 + Math.sin(angle) * dist;

            CrowMinion crow = new CrowMinion(spawnX, spawnY, null);
            crow.owner = this;
            crow.life = 1;
            game.crowMinions.add(crow);
        }

        game.shakeIntensity = 18;
        game.hitstopFrames = 12;
        carrionSwarmTimer = 100;

        for (int i = 0; i < 200; i++) {
            double angle = Math.random() * Math.PI * 2;
            double speed = 8 + Math.random() * 16;
            game.particles.add(new Particle(x + 40, y + 40,
                    Math.cos(angle) * speed,
                    Math.sin(angle) * speed - 6,
                    Color.rgb(10, 0, 20)));
        }
    }

    private void specialOpiumBird() {
        leanTimer = 360;
        leanCooldown = 840;
        specialCooldown = 840;
        specialMaxCooldown = 840;
        game.shakeIntensity = 20;
        game.hitstopFrames = 15;
        for (int i = 0; i < 150; i++) {
            double angle = Math.random() * Math.PI * 2;
            game.particles.add(new Particle(x + 40, y + 40,
                    Math.cos(angle) * (2 + Math.random() * 10),
                    Math.sin(angle) * (2 + Math.random() * 10) - 4,
                    Color.PURPLE.deriveColor(0, 1, 1, 0.7)));
        }
    }

    private void specialHeisenbird() {
        leanTimer = 300;
        leanCooldown = 720;
        specialCooldown = 720;
        specialMaxCooldown = 720;
        game.shakeIntensity = 18;
        game.hitstopFrames = 12;
        for (int i = 0; i < 150; i++) {
            double angle = Math.random() * Math.PI * 2;
            game.particles.add(new Particle(x + 40, y + 40,
                    Math.cos(angle) * (2 + Math.random() * 10),
                    Math.sin(angle) * (2 + Math.random() * 10) - 4,
                    Color.web("#29B6F6").deriveColor(0, 1, 1, 0.75)));
        }
    }

    private void specialTitmouse() {
        Bird target = null;
        double bestDist = Double.MAX_VALUE;
        for (Bird b : game.players) {
            if (!canDamageTarget(b)) continue;
            double d = Math.hypot(b.x - x, b.y - y);
            if (d < bestDist) {
                bestDist = d;
                target = b;
            }
        }
        if (target == null) {
            game.addToKillFeed(name.split(":")[0].trim() + " tried to ZIP... but no target!");
            specialCooldown = 240;
            specialMaxCooldown = 240;
            return;
        }

        isZipping = true;
        zipTargetX = target.x;
        zipTargetY = target.y;
        zipTimer = 30;

        specialCooldown = 780;
        specialMaxCooldown = 780;

        game.addToKillFeed(name.split(":")[0].trim() + " ZIPPED to " + target.name.split(":")[0].trim() + "!");

        for (int i = 0; i < 50; i++) {
            double offset = i * 8;
            game.particles.add(new Particle(
                    x + 40 - vx * offset / 10,
                    y + 40 - vy * offset / 10,
                    (Math.random() - 0.5) * 8,
                    (Math.random() - 0.5) * 8 - 2,
                    Color.SKYBLUE.deriveColor(0, 1, 1, 0.8 - i / 60.0)
            ));
        }
    }

    private void specialPelican() {
        Bird target = null;
        double bestDist = Double.MAX_VALUE;
        for (Bird b : game.players) {
            if (!canDamageTarget(b)) continue;
            double d = Math.hypot(b.x - x, b.y - y);
            if (d < bestDist && d < 280) {
                bestDist = d;
                target = b;
            }
        }
        if (target != null) {
            plungeTimer = 45;
            sizeMultiplier *= 1.18;
            enlargedByPlunge = true;
            specialCooldown = 720;
            specialMaxCooldown = 720;
            game.addToKillFeed(name.split(":")[0].trim() + " PELICAN PLUNGE!!!");
            game.shakeIntensity = 32;
            game.hitstopFrames = 18;
            target.vx += (target.x > x ? 1 : -1) * 36;
            target.vy = -26;
            int dmg = (int)(24 * powerMultiplier);
            double old = target.health;
            int dealt = (int) applyDamageTo(target, dmg);
            game.damageDealt[playerIndex] += dealt;
            game.recordSpecialImpact(playerIndex, dealt, dealt > 0);
            if (target.health <= 0 && old > 0) game.eliminations[playerIndex]++;
            for (int i = 0; i < 120; i++) {
                double ang = Math.random() * Math.PI * 2;
                game.particles.add(new Particle(target.x + 40, target.y + 40,
                        Math.cos(ang) * (8 + Math.random() * 22),
                        Math.sin(ang) * (8 + Math.random() * 22) - 12,
                        Color.ORANGE.brighter()));
            }
            game.achievementProgress[18]++;
            if (game.achievementProgress[18] >= 15 && !game.achievementsUnlocked[18]) {
                game.unlockAchievement(18, "PELICAN KING!");
            }
        } else {
            specialCooldown = 210;
        }
    }

    KeyCode leftKey() {
        return switch (playerIndex) {
            case 0 -> KeyCode.A;
            case 1 -> KeyCode.LEFT;
            case 2 -> KeyCode.F;
            case 3 -> KeyCode.J;
            default -> KeyCode.A;
        };
    }

    KeyCode rightKey() {
        return switch (playerIndex) {
            case 0 -> KeyCode.D;
            case 1 -> KeyCode.RIGHT;
            case 2 -> KeyCode.H;
            case 3 -> KeyCode.L;
            default -> KeyCode.D;
        };
    }

    KeyCode jumpKey() {
        return switch (playerIndex) {
            case 0 -> KeyCode.W;
            case 1 -> KeyCode.UP;
            case 2 -> KeyCode.T;
            case 3 -> KeyCode.I;
            default -> KeyCode.W;
        };
    }

    private KeyCode attackKey() {
        return switch (playerIndex) {
            case 0 -> KeyCode.SPACE;
            case 1 -> KeyCode.ENTER;
            case 2 -> KeyCode.Y;
            case 3 -> KeyCode.O;
            default -> KeyCode.SPACE;
        };
    }

    private KeyCode specialKey() {
        return switch (playerIndex) {
            case 0 -> KeyCode.SHIFT;
            case 1 -> KeyCode.SLASH;
            case 2 -> KeyCode.U;
            case 3 -> KeyCode.P;
            default -> KeyCode.SHIFT;
        };
    }

    private KeyCode blockKey() {
        return switch (playerIndex) {
            case 0 -> KeyCode.S;
            case 1 -> KeyCode.DOWN;
            case 2 -> KeyCode.G;
            case 3 -> KeyCode.K;
            default -> KeyCode.S;
        };
    }

    private int aiJumpCooldown = 0;
    private int aiSpecialCooldown = 0;
    private int aiStrafeTimer = 0;
    private int aiStrafeDir = 1;
    private int aiIdleFrames = 0;
    private int aiCommitFrames = 0;
    private int aiRetreatCooldown = 0;
    private int aiMicroPause = 0;
    private int aiDirectionLock = 0;
    private int aiLockedDir = 0;
    private int aiStrafeHoldFrames = 0;
    private int aiPowerCommitFrames = 0;
    private double aiLastHealth = 100;

    private int dashCooldown = 0;
    private int dashTimer = 0;
    private int lastTapDir = 0;
    private long lastTapTimeNs = 0L;

    private void aiControl() {
        if (aiJumpCooldown > 0) aiJumpCooldown--;
        if (aiSpecialCooldown > 0) aiSpecialCooldown--;
        if (aiStrafeTimer > 0) aiStrafeTimer--;
        if (aiCommitFrames > 0) aiCommitFrames--;
        if (aiRetreatCooldown > 0) aiRetreatCooldown--;
        if (aiMicroPause > 0) aiMicroPause--;
        if (aiDirectionLock > 0) aiDirectionLock--;
        if (aiStrafeHoldFrames > 0) aiStrafeHoldFrames--;
        if (aiPowerCommitFrames > 0) aiPowerCommitFrames--;

        clearAIInputs();

        int cpuLevel = game.getCpuLevel(playerIndex);
        double rawSkill = Math.max(0.0, Math.min(1.0, (cpuLevel - 1) / 8.0));
        double skill = Math.pow(rawSkill, 2.1);
        double error = Math.min(1.0, 1.05 - skill);
        if (cpuLevel <= 1) {
            skill = 0.0;
            error = 1.0;
            if (random.nextDouble() < 0.22) {
                aiLastHealth = health;
                return;
            }
        }

        Bird target = pickAITarget();
        PowerUp powerUp = pickBestAIPowerUp(target);
        boolean onGround = isOnGround();
        Platform standing = findCurrentSupportPlatform();

        if (target == null && powerUp == null) {
            aiLastHealth = health;
            return;
        }

        double myCx = x + 40;
        double targetDist = target != null ? Math.hypot(target.x - x, target.y - y) : Double.MAX_VALUE;
        double idealRange = getAIIdealRange();
        boolean lowHealth = health < 38;
        boolean tookDamageRecently = health < aiLastHealth - 1.0;
        if (tookDamageRecently && target != null && targetDist < 300) {
            aiCommitFrames = Math.max(aiCommitFrames, 42 + random.nextInt(20));
        }
        if (cpuLevel > 1 && target != null && aiCommitFrames <= 0 && targetDist < 240 && random.nextDouble() < 0.012 * (0.4 + 0.6 * skill)) {
            aiCommitFrames = 36 + random.nextInt(26);
        }
        if (cpuLevel <= 1) {
            aiCommitFrames = 0;
        }

        // Emergency self-preservation before anything else.
        if (onGround && y > game.GROUND_Y + 220) {
            game.pressedKeys.add(jumpKey());
            aiJumpCooldown = 12;
        }

        if (handleAIDodgeBurstThreats(target, onGround)) {
            aiLastHealth = health;
            return;
        }

        PowerUp healthPack = findBestHealthPowerUp();
        boolean veryLowHealth = health < 20;
        boolean losingHard = target != null && target.health > health + 32;
        boolean retreatWindow = aiRetreatCooldown <= 0 && (veryLowHealth || (health < 28 && losingHard));
        boolean shouldRetreat = target != null && retreatWindow && targetDist < 220 && healthPack == null && aiCommitFrames <= 0;
        boolean immediatePowerChance = powerUp != null && isImmediatePowerUpOpportunity(powerUp);
        boolean shouldChasePower = powerUp != null &&
                ((shouldPrioritizePowerUp(powerUp, target) && aiCommitFrames <= 0) || immediatePowerChance);
        if (shouldChasePower) {
            aiPowerCommitFrames = Math.max(aiPowerCommitFrames, immediatePowerChance ? 44 : 30);
        }
        boolean powerFocus = powerUp != null && !shouldRetreat && (shouldChasePower || aiPowerCommitFrames > 0);
        if (powerFocus && powerUp != null && !isPowerUpConvenient(powerUp, target)) {
            powerFocus = false;
            aiPowerCommitFrames = 0;
        }
        if (powerFocus && target != null && targetDist < idealRange * 0.85) {
            powerFocus = false;
            aiPowerCommitFrames = 0;
        }

        if (powerFocus) {
            // Keep movement deterministic while committing to a pickup.
            aiDirectionLock = 0;
            aiMicroPause = 0;
        }

        double dyToTarget = target != null ? target.y - y : 0;
        boolean targetBelow = target != null && dyToTarget > 160;
        boolean dropPlan = false;
        double dropEdgeX = x;
        if (targetBelow && onGround && standing != null && !isBoundaryPlatform(standing)) {
            double dropOffset = 60;
            double leftEdge = standing.x - dropOffset - 40 * sizeMultiplier;
            double rightEdge = standing.x + standing.w + dropOffset - 40 * sizeMultiplier;
            double platformCenter = standing.x + standing.w / 2.0;
            double targetCx = target.x + 40;
            dropEdgeX = targetCx < platformCenter ? leftEdge : rightEdge;
            dropPlan = true;
        }
        boolean lowCpu = cpuLevel <= 2;
        double goalX;
        if (shouldRetreat && target != null) {
            goalX = x + (x - target.x) * 1.35;
            aiRetreatCooldown = 90 + random.nextInt(45);
        } else if (powerFocus && powerUp != null) {
            goalX = pickPowerUpGoalX(powerUp);
        } else if (target != null) {
            // Predict movement instead of chasing current position.
            double lead = Math.min(10, Math.max(2, targetDist / 120.0));
            if (lowCpu) lead *= 0.55;
            double predictedX = target.x + target.vx * lead;
            if (targetBelow) {
                goalX = dropPlan ? dropEdgeX : predictedX;
            } else if (targetDist > idealRange * 1.25) {
                goalX = predictedX;
            } else if (targetDist < idealRange * 0.65) {
                if (aiCommitFrames > 0) {
                    goalX = target.x + (random.nextBoolean() ? 1 : -1) * 65;
                } else {
                    goalX = target.x + (x < target.x ? -1 : 1) * 95;
                }
            } else {
                double desiredOffset = aiCommitFrames > 0 ? 95 : 125;
                double strafeTargetX = target.x + aiStrafeDir * desiredOffset;
                double strafeError = Math.abs(x - strafeTargetX);
                if (aiStrafeHoldFrames <= 0 && aiStrafeTimer <= 0 && strafeError < 55) {
                    aiStrafeDir *= -1;
                    aiStrafeHoldFrames = 26 + random.nextInt(24);
                    aiStrafeTimer = 18 + random.nextInt(18);
                } else if (aiStrafeHoldFrames <= 0 && aiStrafeTimer <= 0) {
                    aiStrafeHoldFrames = 18 + random.nextInt(18);
                    aiStrafeTimer = 14 + random.nextInt(14);
                }
                goalX = target.x + aiStrafeDir * desiredOffset;
            }
        } else {
            goalX = x;
        }

        Platform climbPlatform = null;
        boolean verticalPlan = false;
        if (!powerFocus && target != null) {
            if (dyToTarget < -160) {
                double maxRise = 520 + 180 * skill;
                climbPlatform = findClimbPlatform(target.x + 40, maxRise, 520);
                if (climbPlatform != null) {
                    goalX = climbPlatform.x + climbPlatform.w / 2.0 - 40 * sizeMultiplier;
                    verticalPlan = true;
                }
            } else if (dyToTarget > 180 && onGround) {
                if (standing != null && !isBoundaryPlatform(standing)) {
                    goalX = (target.x < x) ? (standing.x - 20) : (standing.x + standing.w + 20);
                    verticalPlan = true;
                }
            }
        }

        if (!powerFocus && target != null && error > 0.0 && !dropPlan) {
            goalX += (random.nextDouble() - 0.5) * 160 * error;
        }

        goalX = Math.max(120, Math.min(goalX, game.WORLD_WIDTH - 120));

        if (powerFocus && powerUp != null) facingRight = powerUp.x > myCx;
        else if (target != null) facingRight = target.x > myCx;
        else if (powerUp != null) facingRight = powerUp.x > myCx;

        int moveDir = 0;
        if (dropPlan) {
            goalX = dropEdgeX;
            aiDirectionLock = 0;
            aiStrafeHoldFrames = 0;
            aiStrafeTimer = 0;
            aiMicroPause = 0;
        }
        double moveDeadZone = targetBelow ? 120 : 35;
        if (!dropPlan && Math.abs(goalX - x) > moveDeadZone) {
            moveDir = goalX < x ? -1 : 1;
        }
        if (!powerFocus && target != null) {
            double loiterChance = cpuLevel <= 1 ? 0.45 : (cpuLevel == 2 ? 0.25 : 0.0);
            if (loiterChance > 0 && random.nextDouble() < loiterChance) {
                moveDir = 0;
            }
        }
        if (dropPlan) {
            moveDir = goalX < x ? -1 : 1;
            aiDirectionLock = 0;
            aiStrafeHoldFrames = 0;
        }

        // Anti-stall fallback: if spacing logic leaves us idle too long, pressure target.
        if (!powerFocus && !dropPlan && target != null && moveDir == 0 && targetDist > 130 && cpuLevel > 2) {
            aiIdleFrames++;
            if (aiIdleFrames > 24) {
                moveDir = target.x < x ? -1 : 1;
            }
        } else {
            aiIdleFrames = 0;
        }

        if (!powerFocus && !verticalPlan && !dropPlan && !targetBelow && target != null && onGround && targetDist < 270 &&
                aiDirectionLock <= 0 && random.nextDouble() < 0.02 * (0.35 + 0.65 * skill)) {
            aiDirectionLock = 18 + random.nextInt(30);
            aiLockedDir = target.x < x ? -1 : 1;
        }
        if (!powerFocus && !verticalPlan && !dropPlan && !targetBelow && aiDirectionLock > 0) {
            moveDir = aiLockedDir;
        }

        if (!powerFocus && !verticalPlan && !dropPlan && !targetBelow && onGround && aiMicroPause <= 0 && target != null &&
                targetDist > 160 && random.nextDouble() < 0.008 + 0.02 * error) {
            aiMicroPause = 6 + random.nextInt(10 + (int) (error * 10));
        }
        if (!powerFocus && targetBelow && lowCpu && aiMicroPause <= 0 && random.nextDouble() < 0.12) {
            aiMicroPause = 12 + random.nextInt(18);
        }
        if (!powerFocus && !verticalPlan && !dropPlan && !targetBelow && aiMicroPause > 0 && targetDist > 150) {
            moveDir = 0;
        }

        if (moveDir < 0) game.pressedKeys.add(leftKey());
        if (moveDir > 0) game.pressedKeys.add(rightKey());

        // Vertical positioning and recovery behavior.
        if (!powerFocus && target != null) {
            double dy = target.y - y;
            if (onGround && aiJumpCooldown <= 0) {
                boolean jumpForHeight = dy < -120 && Math.abs(target.x - x) < 420;
                boolean jumpForCombo = dy > 70 && targetDist < 220;
                boolean jumpForAboveClose = dy < -200 && Math.abs(target.x - x) < 220;
                double jumpSense = 0.35 + 0.65 * skill;
                if ((jumpForHeight || jumpForCombo || jumpForAboveClose) && random.nextDouble() < jumpSense) {
                    game.pressedKeys.add(jumpKey());
                    aiJumpCooldown = 14;
                }
                if (verticalPlan && climbPlatform != null) {
                    double climbCenter = climbPlatform.x + climbPlatform.w / 2.0;
                    if (Math.abs((x + 40) - climbCenter) < 140 && dy < -140) {
                        game.pressedKeys.add(jumpKey());
                        aiJumpCooldown = 14;
                    }
                }
            }

            if (!onGround && type.flyUpForce > 0) {
                boolean recoverAltitude = y > game.GROUND_Y - 120;
                boolean maintainVsTarget = target.y < y + 180;
                if (recoverAltitude || maintainVsTarget) {
                    game.pressedKeys.add(jumpKey());
                }
            }
        } else if (powerUp != null && onGround && aiJumpCooldown <= 0 && powerUp.y < y - 120) {
            double dx = Math.abs(powerUp.x - (x + 40));
            double dy = (y + 40) - powerUp.y;
            if (dx < 140 && dy < 320) {
                game.pressedKeys.add(jumpKey());
                aiJumpCooldown = 14;
            }
        }

        // Defensive block read.
        if (target != null && targetDist < 170 && target.attackAnimationTimer > 3 &&
                facingRight == (target.x > x) && random.nextDouble() < (lowHealth ? 0.50 : 0.34) * (0.25 + 0.75 * skill)) {
            game.pressedKeys.add(blockKey());
        }

        // Attack cadence respects role/range.
        double attackChance = (aiCommitFrames > 0 ? 0.96 : 0.84) * (0.45 + 0.55 * skill);
        if (cpuLevel <= 1) attackChance *= 0.04;
        else if (cpuLevel == 2) attackChance *= 0.35;
        if (!powerFocus && target != null && attackCooldown <= 0 &&
                targetDist < Math.max(140, idealRange * 0.95) &&
                Math.abs(target.y - y) < 115 &&
                random.nextDouble() < attackChance) {
            game.pressedKeys.add(attackKey());
        }

        // Special ability timing by bird role.
        if (!powerFocus && target != null && specialCooldown <= 0 && aiSpecialCooldown <= 0 &&
                shouldUseSpecialAI(target, targetDist, onGround, lowHealth) &&
                random.nextDouble() < (0.25 + 0.75 * skill)) {
            game.pressedKeys.add(specialKey());
            aiSpecialCooldown = 26;
        }

        if (!powerFocus && tauntCooldown <= 0 && target != null && health > 80 && target.health < 35 &&
                targetDist < 200 && random.nextDouble() < 0.006) {
            currentTaunt = random.nextInt(3) + 1;
            tauntTimer = 50;
            tauntCooldown = 300;
            game.addToKillFeed(name.split(":")[0].trim() + " IS ABSOLUTELY COOKING!");
        }

        aiLastHealth = health;
    }

    private void specialBat() {
        batEchoTimer = 150;
        specialCooldown = 660;
        specialMaxCooldown = 660;
        game.shakeIntensity = 22;
        game.hitstopFrames = 12;

        if (batHanging) {
            batHanging = false;
            batHangPlatform = null;
            vy = -16;
            vx += (facingRight ? 1 : -1) * 9;
        }

        game.addToKillFeed(name.split(":")[0].trim() + " UNLEASHED SONAR SCREECH!");

        for (Bird other : game.players) {
            if (!canDamageTarget(other)) continue;
            double dx = (other.x + 40) - (x + 40);
            double dy = (other.y + 40) - (y + 40);
            double dist = Math.hypot(dx, dy);
            if (dist > 360) continue;

            int dmg = dist < 150 ? 18 : (dist < 260 ? 12 : 8);
            double oldHealth = other.health;
            int dealt = (int) applyDamageTo(other, dmg);
            game.damageDealt[playerIndex] += dealt;
            game.recordSpecialImpact(playerIndex, dealt, dealt > 0);
            if (other.health <= 0 && oldHealth > 0) game.eliminations[playerIndex]++;

            other.stunTime = Math.max(other.stunTime, 28);
            double safeDist = Math.max(0.001, dist);
            other.vx += dx / safeDist * 16;
            other.vy -= 8;
        }

        for (int ring = 1; ring <= 4; ring++) {
            double radius = 70 + ring * 55;
            for (int i = 0; i < 42; i++) {
                double ang = i / 42.0 * Math.PI * 2;
                game.particles.add(new Particle(
                        x + 40 + Math.cos(ang) * radius,
                        y + 40 + Math.sin(ang) * radius,
                        Math.cos(ang) * 2.4,
                        Math.sin(ang) * 2.4,
                        ring % 2 == 0 ? Color.MEDIUMPURPLE.brighter() : Color.CYAN.brighter()
                ));
            }
        }
    }

    private void specialRaven() {
        Bird target = null;
        double bestDist = Double.MAX_VALUE;
        for (Bird b : game.players) {
            if (!canDamageTarget(b)) continue;
            double d = Math.hypot(b.x - x, b.y - y);
            if (d < bestDist) {
                bestDist = d;
                target = b;
            }
        }

        if (target != null && bestDist < 520) {
            double dir = (target.x + 40 >= x + 40) ? -1 : 1;
            double warpX = target.x + dir * 120;
            double warpY = target.y;
            double maxX = game.WORLD_WIDTH - 80 * sizeMultiplier;
            double maxY = game.GROUND_Y - 80 * sizeMultiplier;
            warpX = Math.max(0, Math.min(warpX, maxX));
            warpY = Math.max(0, Math.min(warpY, maxY));
            x = warpX;
            y = warpY;
            facingRight = dir < 0;

            int dmg = Math.max(8, (int) Math.round(14 * powerMultiplier));
            double oldHealth = target.health;
            int dealt = (int) applyDamageTo(target, dmg);
            game.damageDealt[playerIndex] += dealt;
            game.recordSpecialImpact(playerIndex, dealt, dealt > 0);
            if (target.health <= 0 && oldHealth > 0) game.eliminations[playerIndex]++;

            target.stunTime = Math.max(target.stunTime, 40);
            double kbDir = (target.x > x) ? 1 : -1;
            target.vx += kbDir * 10;
            target.vy -= 6;

            speedMultiplier = Math.max(speedMultiplier, baseSpeedMultiplier * 1.35);
            speedTimer = Math.max(speedTimer, 120);

            game.shakeIntensity = Math.max(game.shakeIntensity, 16);
            game.hitstopFrames = Math.max(game.hitstopFrames, 8);
            game.addToKillFeed(name.split(":")[0].trim() + " SHADOW WARPED " + target.name.split(":")[0].trim() + "! -" + dealt + " HP");

            for (int i = 0; i < 90; i++) {
                double angle = Math.random() * Math.PI * 2;
                double speed = 5 + Math.random() * 12;
                game.particles.add(new Particle(
                        x + 40 + Math.cos(angle) * 20,
                        y + 40 + Math.sin(angle) * 20,
                        Math.cos(angle) * speed,
                        Math.sin(angle) * speed - 4,
                        Color.web("#263238").deriveColor(0, 1, 1, 0.85)
                ));
            }

            specialCooldown = 660;
            specialMaxCooldown = 660;
        } else {
            vx += (facingRight ? 1 : -1) * 18;
            vy = Math.min(vy, -10);
            canDoubleJump = true;
            speedMultiplier = Math.max(speedMultiplier, baseSpeedMultiplier * 1.2);
            speedTimer = Math.max(speedTimer, 80);
            game.addToKillFeed(name.split(":")[0].trim() + " SHADOW DASH!");
            for (int i = 0; i < 50; i++) {
                double angle = Math.random() * Math.PI * 2;
                game.particles.add(new Particle(
                        x + 40,
                        y + 40,
                        Math.cos(angle) * (4 + Math.random() * 8),
                        Math.sin(angle) * (4 + Math.random() * 8) - 3,
                        Color.web("#455A64").deriveColor(0, 1, 1, 0.8)
                ));
            }
            specialCooldown = 240;
            specialMaxCooldown = 240;
        }
    }

    private void clearAIInputs() {
        game.pressedKeys.remove(leftKey());
        game.pressedKeys.remove(rightKey());
        game.pressedKeys.remove(jumpKey());
        game.pressedKeys.remove(attackKey());
        game.pressedKeys.remove(specialKey());
        game.pressedKeys.remove(blockKey());
    }

    private Bird pickAITarget() {
        Bird best = null;
        double bestScore = -Double.MAX_VALUE;
        for (Bird b : game.players) {
            if (b == null || b == this || b.health <= 0) continue;
            if (!game.canDamage(this, b)) continue;
            double dist = Math.hypot(b.x - x, b.y - y);
            double score = 3000.0 / (1 + dist);
            score += (100 - b.health) * 1.8;
            score += Math.max(0, 40 - health) * 0.9;
            if (b.specialCooldown <= 0) score += 40; // prioritize threatening targets
            if (b.playerIndex == 0) score += 15; // slight preference to human player
            if (score > bestScore) {
                bestScore = score;
                best = b;
            }
        }
        return best;
    }

    private PowerUp pickBestAIPowerUp(Bird target) {
        PowerUp bestPowerUp = null;
        double bestScore = -Double.MAX_VALUE;
        for (PowerUp p : game.powerUps) {
            boolean convenient = isPowerUpConvenient(p, target);
            if (!convenient && !isImmediatePowerUpOpportunity(p)) continue;
            double myDist = Math.hypot(p.x - (x + 40), p.y - (y + 40));
            double score = 0;
            switch (p.type) {
                case HEALTH -> score = (100 - health) * 26 + 140;
                case RAGE -> score = 96;
                case NEON -> score = 95;
                case SPEED -> score = 82;
                case THERMAL -> score = 74;
                case SHRINK -> score = 66;
                case VINE_GRAPPLE -> score = 88;
                case OVERCHARGE -> score = 102;
                case TITAN -> score = 92;
            }
            score /= (1 + myDist / 320.0);

            if (target != null) {
                double enemyDist = Math.hypot(p.x - (target.x + 40), p.y - (target.y + 40));
                if (enemyDist < myDist * 0.75) score *= 0.6;
            }

            if (score > bestScore) {
                bestScore = score;
                bestPowerUp = p;
            }
        }
        return bestPowerUp;
    }

    private boolean isImmediatePowerUpOpportunity(PowerUp p) {
        if (p == null) return false;
        double dx = Math.abs(p.x - (x + 40));
        double dy = p.y - (y + 40);
        if (dx < 110 && Math.abs(dy) < 220) return true;
        return dx < 105 && dy > 90 && dy < 360;
    }

    private double pickPowerUpGoalX(PowerUp p) {
        if (p == null) return x;
        double myCx = x + 40;
        double dx = Math.abs(p.x - myCx);
        double dy = p.y - (y + 40);

        if (dy > 90 && dx < 120) {
            Platform standing = findCurrentSupportPlatform();
            if (standing != null) {
                double centerOffset = 25;
                double leftDrop = standing.x - centerOffset - 40 * sizeMultiplier;
                double rightDrop = standing.x + standing.w + centerOffset - 40 * sizeMultiplier;
                leftDrop = Math.max(120, Math.min(leftDrop, game.WORLD_WIDTH - 120));
                rightDrop = Math.max(120, Math.min(rightDrop, game.WORLD_WIDTH - 120));
                return Math.abs(leftDrop - p.x) <= Math.abs(rightDrop - p.x) ? leftDrop : rightDrop;
            }
        }
        return p.x;
    }

    private Platform findCurrentSupportPlatform() {
        double feetX = x + 40 * sizeMultiplier;
        double feetY = y + 80 * sizeMultiplier;
        for (Platform p : game.platforms) {
            boolean isCaveCeiling = game.selectedMap == MapType.CAVE &&
                    p.y <= 1 && p.h >= 60 && p.w >= game.WORLD_WIDTH - 10;
            if (isCaveCeiling) continue;
            if (feetX >= p.x && feetX <= p.x + p.w &&
                    Math.abs(feetY - p.y) <= 10 &&
                    y <= p.y + 2) {
                return p;
            }
        }
        return null;
    }

    private PowerUp findBestHealthPowerUp() {
        PowerUp best = null;
        double bestDist = Double.MAX_VALUE;
        for (PowerUp p : game.powerUps) {
            if (p.type != PowerUpType.HEALTH) continue;
            double dist = Math.hypot(p.x - (x + 40), p.y - (y + 40));
            if (dist < bestDist) {
                bestDist = dist;
                best = p;
            }
        }
        return best;
    }

    private boolean shouldPrioritizePowerUp(PowerUp p, Bird target) {
        if (p == null) return false;
        if (!isPowerUpConvenient(p, target)) return false;
        if (isImmediatePowerUpOpportunity(p)) return true;
        if (p.type == PowerUpType.HEALTH && health < 72) return true;
        if (target == null) return true;
        if (target.health < 35 && health > 30) return false;
        double targetDist = Math.hypot(target.x - x, target.y - y);
        return targetDist > getAIIdealRange() * 2.1;
    }

    private boolean isPowerUpConvenient(PowerUp p, Bird target) {
        if (p == null) return false;
        double myCx = x + 40;
        double myCy = y + 40;
        double dx = Math.abs(p.x - myCx);
        double dy = p.y - myCy;
        double dist = Math.hypot(dx, dy);
        double maxDist = health < 25 ? 650 : 480;
        if (dist > maxDist) return false;
        double maxVertical = (type.flyUpForce > 0 || !isOnGround()) ? 520 : 320;
        if (Math.abs(dy) > maxVertical) return false;

        if (target == null) {
            return dx < 260 && Math.abs(dy) < 260;
        }

        double targetDist = Math.hypot(target.x - x, target.y - y);
        double enemyDist = Math.hypot(p.x - (target.x + 40), p.y - (target.y + 40));
        boolean onRoute = dist + enemyDist <= targetDist * 1.25;
        boolean between = dx < Math.abs(target.x - x) + 140;
        boolean nearMe = dist < 260;
        return onRoute || (nearMe && between);
    }

    private boolean isBoundaryPlatform(Platform p) {
        boolean isFloor = p.w >= game.WORLD_WIDTH - 10 && p.h >= 200;
        boolean isWall = p.h >= game.WORLD_HEIGHT - 10 && p.w <= 150;
        boolean isCaveCeiling = game.selectedMap == MapType.CAVE &&
                p.y <= 1 && p.h >= 60 && p.w >= game.WORLD_WIDTH - 10;
        return isFloor || isWall || isCaveCeiling;
    }

    private Platform findClimbPlatform(double targetX, double maxRise, double maxHorizontal) {
        Platform best = null;
        double bestScore = -Double.MAX_VALUE;
        double myCx = x + 40;
        for (Platform p : game.platforms) {
            if (isBoundaryPlatform(p)) continue;
            if (p.y >= y - 40) continue;
            double rise = y - p.y;
            if (rise <= 0 || rise > maxRise) continue;
            double centerX = p.x + p.w / 2.0;
            double dxTarget = Math.abs(centerX - targetX);
            double dxMe = Math.abs(centerX - myCx);
            if (dxMe > maxHorizontal && dxTarget > maxHorizontal) continue;
            double score = 0;
            score -= rise * 1.1;
            score -= dxTarget * 0.8;
            score -= dxMe * 0.4;
            if (score > bestScore) {
                bestScore = score;
                best = p;
            }
        }
        return best;
    }

    private double getAIIdealRange() {
        return switch (type) {
            case TURKEY, PELICAN, GRINCHHAWK -> 165;
            case RAZORBILL, SHOEBILL -> 188;
            case EAGLE, VULTURE, PENGUIN, PHOENIX -> 208;
            case FALCON -> 202;
            case HUMMINGBIRD, TITMOUSE -> 220;
            case OPIUMBIRD, HEISENBIRD, MOCKINGBIRD -> 205;
            case BAT -> 214;
            case PIGEON -> 190;
            case RAVEN -> 210;
        };
    }

    private boolean handleAIDodgeBurstThreats(Bird target, boolean onGround) {
        if (target == null) return false;

        boolean dodge = false;
        int dir = target.x > x ? -1 : 1;
        double dx = Math.abs(target.x - x);
        double dy = target.y - y;

        if (target.type == BirdGame3.BirdType.TURKEY && target.isGroundPounding && dx < 320 && dy < -60) dodge = true;
        if (target.type == BirdGame3.BirdType.EAGLE && target.diveTimer > 0 && dx < 430 && dy < 120) dodge = true;
        if (target.type == BirdGame3.BirdType.FALCON && target.diveTimer > 0 && dx < 370 && dy < 120) dodge = true;
        if (target.type == BirdGame3.BirdType.PENGUIN && Math.abs(target.vx) > 14 && dx < 380 && Math.abs(dy) < 110) dodge = true;
        if (target.type == BirdGame3.BirdType.TITMOUSE && target.isZipping && dx < 420) dodge = true;

        if (!dodge) return false;
        int cpuLevel = game.getCpuLevel(playerIndex);
        double skill = Math.max(0.0, Math.min(1.0, (cpuLevel - 1) / 8.0));
        if (random.nextDouble() > 0.25 + 0.75 * skill) return false;

        if (dir < 0) game.pressedKeys.add(leftKey());
        else game.pressedKeys.add(rightKey());
        if (onGround && aiJumpCooldown <= 0) {
            game.pressedKeys.add(jumpKey());
            aiJumpCooldown = 16;
        }
        return true;
    }

    private boolean shouldUseSpecialAI(Bird target, double dist, boolean onGround, boolean lowHealth) {
        double dy = target.y - y;
        switch (type) {
            case PIGEON:
                return lowHealth || (health < 55 && dist < 200);
            case EAGLE:
                return y < game.GROUND_Y - 800 && dy > 180 && dist < 520;
            case FALCON:
                return dist < 360 && dy > -120 && (onGround || lowHealth || target.health > health + 8);
            case PHOENIX:
                return dist < 320 && (lowHealth || Math.abs(dy) < 180 || target.health > health + 10);
            case HUMMINGBIRD:
                return (dist < 260 && Math.abs(dy) < 200) || (lowHealth && dist < 330);
            case TURKEY:
                return onGround && dist < 280 && dy > 50;
            case PENGUIN:
                return (onGround && dist > 110 && dist < 360 && Math.abs(dy) < 120)
                        || (onGround && dy < -170 && dist < 420);
            case SHOEBILL:
                return dist < 240 || (dist < 420 && random.nextDouble() < 0.2);
            case MOCKINGBIRD:
                return onGround && !loungeActive && (lowHealth || dist < 210);
            case RAZORBILL:
                return !onGround && dist < 320 && dy > -90;
            case GRINCHHAWK:
                return dist < 260 && health < 95;
            case VULTURE:
                return crowSwarmCooldown <= 0 && (dist < 380 || lowHealth);
            case OPIUMBIRD:
                return onGround && dist < 270 && random.nextDouble() < 0.85;
            case HEISENBIRD:
                return onGround && dist < 250 && random.nextDouble() < 0.9;
            case TITMOUSE:
                return dist > 140 && dist < 560;
            case BAT:
                return dist < 320 && (Math.abs(dy) < 180 || !onGround);
            case PELICAN:
                return plungeTimer <= 0 && onGround && dist < 260 && Math.abs(dy) < 130;
            case RAVEN:
                return dist < 420 && (lowHealth || Math.abs(dy) < 200);
            default:
                return false;
        }
    }

    private boolean hasLimitedFlight() {
        return switch (type) {
            case PIGEON, TURKEY, GRINCHHAWK, PELICAN -> true;
            default -> false;
        };
    }

    public void update(double gameSpeed) {
        if (health > 0 && game.isAI[playerIndex]) aiControl();

        // === UPDATE TIMERS ===
        updateTimers(gameSpeed);

        // === VINE GRAPPLE ===
        handleVineGrapple();

        // === RESET EXPIRED BUFFS ===
        resetExpiredBuffs();

        loungeHeal();
        if (type == BirdGame3.BirdType.MOCKINGBIRD && loungeActive && loungeHealth > 0) {
            game.loungeTime[playerIndex]++;
        }

        // === OPIUM / HEISENBIRD ===
        handleOpiumBirdEffects();

        boolean stunned = stunTime > 0;
        boolean airborne = !isOnGround();

        if (type == BirdGame3.BirdType.BAT && handleBatHanging(stunned)) {
            handleTaunts();
            if (tauntTimer > 0) tauntTimer--;
            return;
        }

        // === BLOCKING ===
        if (blockCooldown > 0) blockCooldown--;
        if (!stunned && game.pressedKeys.contains(blockKey()) && blockCooldown <= 0) {
            isBlocking = true;
            vx *= 0.6;
            vy *= 0.9;
        } else {
            if (isBlocking) blockCooldown = 30;
            isBlocking = false;
        }

        // === GRAVITY ===
        double gravityScale = 1.0;
        if (type == BirdGame3.BirdType.BAT && !isOnGround()) {
            gravityScale = 0.66;
        }
        vy += BirdGame3.GRAVITY * gravityScale * gameSpeed;

        // === EAGLE PASSIVE ===
        handleEaglePassive(airborne);

        // === VULTURE FLYING ===
        if (type == BirdGame3.BirdType.VULTURE && !stunned && game.pressedKeys.contains(jumpKey())) {
            isFlying = true;
            vy -= 0.65;
            if (vy < -6.0) vy = -6.0;
        } else if (type == BirdGame3.BirdType.VULTURE) {
            isFlying = false;
            if (vy < 0) vy += 0.3;
        }

        // === FLY/GLIDE ===
        if (!stunned && game.pressedKeys.contains(jumpKey()) && airborne) {
            boolean limitedFlight = hasLimitedFlight();
            boolean thermalActive = thermalTimer > 0;
            double speedRatio = baseSpeedMultiplier > 0 ? speedMultiplier / baseSpeedMultiplier : 1.0;
            double flightLiftScale = Math.max(0.8, Math.min(1.45, 1.0 + (speedRatio - 1.0) * 0.55));
            // Thermal Rise should always remain effective even if limited-flight fuel is drained.
            if (!limitedFlight || limitedFlightFuel > 0 || thermalActive) {
                vy -= (type.flyUpForce + thermalLift) * hoverRegenMultiplier * flightLiftScale;
                if (limitedFlight && !thermalActive) {
                    limitedFlightFuel = Math.max(0, limitedFlightFuel - gameSpeed);
                    if (vy < -6.4) vy = -6.4;
                } else if (limitedFlight) {
                    if (vy < -9.2) vy = -9.2;
                }
            }
            if (type == BirdGame3.BirdType.BAT) {
                // Bat gets stronger sustained lift so it can truly dogfight in the air.
                vy -= 0.55 * flightLiftScale;
                if (vy < -11.5) vy = -11.5;
            }
            if (type == BirdGame3.BirdType.PHOENIX) {
                if (phoenixRebornActive) {
                    vy -= 0.4 * flightLiftScale;
                    if (vy < -11.5) vy = -11.5;
                } else if (vy < -8.5) {
                    vy = -8.5;
                }
            }
        }

        if (hasLimitedFlight() && isOnGround()) {
            limitedFlightFuel = LIMITED_FLIGHT_MAX;
        }

        // === THERMAL SOARING ===
        if (thermalTimer > 0 && vy > 0) {
            vy *= 0.85;
        }

        // === TITMOUSE ZIP ===
        handleTitmouseZip();

        // === HORIZONTAL MOVEMENT & JUMPING ===
        handleHorizontalMovement(stunned, airborne);

        // === AIR/GROUND FRICTION ===
        if (!game.pressedKeys.contains(leftKey()) && !game.pressedKeys.contains(rightKey())) {
            vx *= airborne ? 0.96 : 0.80;
        }
        if (neonRushTimer > 0) {
            int dir = facingRight ? 1 : -1;
            vx += dir * 0.7;
            if (Math.abs(vx) > 28) vx = Math.signum(vx) * 28;
        }

        // === RAZORBILL DASH ===
        handleRazorbillBladeStorm();

        // === APPLY VELOCITY ===
        x += vx;
        y += vy;

        // === THERMALS & WIND VENTS ===
        handleThermals();
        applyPenguinDashDamage();
        handleHummingbirdFrenzy();
        handlePhoenixAfterburn();

        // Penguin ice-trail after jump dash.
        if (penguinIceFxTimer > 0) {
            for (int i = 0; i < 2; i++) {
                game.particles.add(new Particle(
                        x + 40 + (Math.random() - 0.5) * 48,
                        y + 62 + (Math.random() - 0.5) * 30,
                        -vx * 0.08 + (Math.random() - 0.5) * 2.0,
                        -1.5 - Math.random() * 3.5,
                        Color.web("#B3E5FC").deriveColor(0, 1, 1, 0.75)
                ));
            }
        }

        // === BOUNDARIES ===
        handleBoundaries();

        // === EAGLE DIVE / ASCENT DAMAGE ===
        handleEagleDiveImpact();

        // === VULTURE FEAST ===
        handleVultureFeast();

        // === POWER-UP PICKUP ===
        handlePowerUpPickup();

        // === TAUNTS ===
        handleTaunts();

        if (tauntTimer > 0) tauntTimer--;
    }

    private void updateTimers(double gameSpeed) {
        speedTimer = Math.max(0, (int)(speedTimer - gameSpeed));
        rageTimer = Math.max(0, (int)(rageTimer - gameSpeed));
        shrinkTimer = Math.max(0, (int)(shrinkTimer - gameSpeed));
        titanTimer = Math.max(0, (int)(titanTimer - gameSpeed));
        neonRushTimer = Math.max(0, (int)(neonRushTimer - gameSpeed));
        thermalTimer = Math.max(0, (int)(thermalTimer - gameSpeed));
        grappleTimer = Math.max(0, (int)(grappleTimer - gameSpeed));
        overchargeAttackTimer = Math.max(0, (int)(overchargeAttackTimer - gameSpeed));
        speedBoostTimer = Math.max(0, (int)(speedBoostTimer - gameSpeed));
        hoverRegenTimer = Math.max(0, (int)(hoverRegenTimer - gameSpeed));
        penguinIceFxTimer = Math.max(0, penguinIceFxTimer - gameSpeed);
        penguinDashDamageTimer = Math.max(0, (int)(penguinDashDamageTimer - gameSpeed));
        hummingFrenzyTimer = Math.max(0, (int)(hummingFrenzyTimer - gameSpeed));
        for (int i = 0; i < hummingFrenzyHitCooldown.length; i++) {
            hummingFrenzyHitCooldown[i] = Math.max(0, (int)(hummingFrenzyHitCooldown[i] - gameSpeed));
        }
        phoenixAfterburnTimer = Math.max(0, (int)(phoenixAfterburnTimer - gameSpeed));
        for (int i = 0; i < phoenixAfterburnHitCooldown.length; i++) {
            phoenixAfterburnHitCooldown[i] = Math.max(0, (int)(phoenixAfterburnHitCooldown[i] - gameSpeed));
        }

        stunTime = Math.max(0, stunTime - gameSpeed);
        if (specialCooldown > 0) specialCooldown = (int)Math.max(0, specialCooldown - gameSpeed);
        if (crowSwarmCooldown > 0) crowSwarmCooldown = (int)Math.max(0, crowSwarmCooldown - gameSpeed);
        if (attackCooldown > 0) attackCooldown = (int)Math.max(0, attackCooldown - gameSpeed);
        diveTimer = Math.max(0, (int)(diveTimer - gameSpeed));
        if (attackAnimationTimer > 0) attackAnimationTimer = (int)Math.max(0, attackAnimationTimer - gameSpeed);
        leanCooldown = Math.max(0, (int)(leanCooldown - gameSpeed));
        leanTimer = Math.max(0, (int)(leanTimer - gameSpeed));
        highTimer = Math.max(0, (int)(highTimer - gameSpeed));
        tauntCooldown = Math.max(0, (int)(tauntCooldown - gameSpeed));
        tauntTimer = Math.max(0, (int)(tauntTimer - gameSpeed));
        eagleDiveCountdown = Math.max(0, (int)(eagleDiveCountdown - gameSpeed));
        bladeStormFrames = Math.max(0, (int)(bladeStormFrames - gameSpeed));
        if (bladeStormFrames == 0) {
            razorbillDashVX = 0.0;
            razorbillDashVY = 0.0;
        }
        plungeTimer = Math.max(0, (int)(plungeTimer - gameSpeed));
        blockCooldown = Math.max(0, (int)(blockCooldown - gameSpeed));
        batEchoTimer = Math.max(0, (int)(batEchoTimer - gameSpeed));
        batHangLockTimer = Math.max(0, (int)(batHangLockTimer - gameSpeed));
    }

    private void handleVineGrapple() {
        if (grappleTimer > 0) {
            grappleTimer--;
            if (grappleTimer == 0) {
                grappleUses = 0;
            }
        }

        if (grappleUses > 0 && game.pressedKeys.contains(specialKey()) && !isOnGround() && !isGrappling && specialCooldown <= 0) {
            double bestDist = Double.MAX_VALUE;
            double targetX = x + 40;
            double targetY = y + 40;
            boolean found = false;

            for (Platform p : game.platforms) {
                double centerX = p.x + p.w / 2;
                double centerY = p.y;
                double d = Math.hypot(centerX - (x + 40), centerY - (y + 40));
                if (d < bestDist && d > 100 && d < 800) {
                    bestDist = d;
                    targetX = centerX;
                    targetY = centerY - 40;
                    found = true;
                }
            }

            if (game.selectedMap == MapType.VIBRANT_JUNGLE) {
                for (SwingingVine v : game.swingingVines) {
                    double d = Math.hypot(v.platformX + v.platformW / 2 - (x + 40), v.platformY - (y + 40));
                    if (d < bestDist && d > 100 && d < 800) {
                        bestDist = d;
                        targetX = v.platformX + v.platformW / 2;
                        targetY = v.platformY - 40;
                        found = true;
                    }
                }
            }

            if (found) {
                isGrappling = true;
                grappleTargetX = targetX;
                grappleTargetY = targetY;
                grappleUses--;
                specialCooldown = 30;
                game.addToKillFeed(name.split(":")[0].trim() + " GRAPPLED with vine!");
                for (int i = 0; i < 30; i++) {
                    double progress = i / 30.0;
                    game.particles.add(new Particle(
                            x + 40 + (targetX - x - 40) * progress,
                            y + 40 + (targetY - y - 40) * progress,
                            0, 0, Color.FORESTGREEN.deriveColor(0,1,1,0.7)
                    ));
                }
            }
        }

        if (isGrappling) {
            double dx = grappleTargetX - (x + 40);
            double dy = grappleTargetY - (y + 40);
            double dist = Math.hypot(dx, dy);
            if (dist > 30) {
                vx = dx / dist * 32;
                vy = dy / dist * 32;
            } else {
                isGrappling = false;
                vx *= 0.6;
                vy = 0;
                canDoubleJump = true;
            }
        }
    }

    private void resetExpiredBuffs() {
        if (speedBoostTimer <= 0) {
            speedBoostTimer = 0;
        } else {
            speedMultiplier = Math.max(speedMultiplier, baseSpeedMultiplier * 1.2);
        }
        if (hoverRegenTimer <= 0) {
            hoverRegenTimer = 0;
            hoverRegenMultiplier = 1.0;
        }
        if (speedTimer <= 0) {
            speedTimer = 0;
        }
        if (speedTimer <= 0 && speedBoostTimer <= 0) {
            speedMultiplier = baseSpeedMultiplier;
        }
        if (rageTimer <= 0) {
            powerMultiplier = basePowerMultiplier;
        }
        if (shrinkTimer <= 0 && !titanActive) {
            sizeMultiplier = baseSizeMultiplier;
        }
        if (titanActive) {
            if (titanTimer <= 0) {
                titanActive = false;
                if (shrinkTimer <= 0) sizeMultiplier = baseSizeMultiplier;
                if (rageTimer <= 0) powerMultiplier = basePowerMultiplier;
            } else {
                if (shrinkTimer <= 0) sizeMultiplier = baseSizeMultiplier * 1.35;
                if (rageTimer <= 0) powerMultiplier = basePowerMultiplier * 1.4;
            }
        }
        if (thermalTimer <= 0) {
            thermalLift = 0.0;
        }
        if (type == BirdGame3.BirdType.PELICAN && plungeTimer <= 0 && enlargedByPlunge) {
            sizeMultiplier /= 1.18;
            enlargedByPlunge = false;
        }
    }

    private void handleOpiumBirdEffects() {
        boolean opium = type == BirdGame3.BirdType.OPIUMBIRD;
        boolean heisen = type == BirdGame3.BirdType.HEISENBIRD;
        if (!opium && !heisen) return;

        if (leanTimer > 0 && opium) {
            game.leanTime[playerIndex]++;
        } else if (leanTimer > 0 && heisen) {
            game.leanTime[playerIndex]++;
        }

        if (leanTimer > 0) {
            double outerRadius = heisen ? 280 : 300;
            double innerRadius = heisen ? 220 : 250;
            double highRadius = heisen ? 110 : 120;
            int damageRoll = heisen ? 45 : 60;
            int highRoll = heisen ? 24 : 20;
            int highDuration = heisen ? 140 : 180;
            double slowX = heisen ? 0.96 : 0.94;
            double slowY = heisen ? 0.985 : 0.98;
            for (Bird other : game.players) {
                if (!canDamageTarget(other)) continue;
                double dx = other.x - x;
                double dy = other.y - y;
                double dist = Math.hypot(dx, dy);
                if (dist < outerRadius) {
                    if (dist < innerRadius) {
                        if (random.nextInt(damageRoll) == 0) {
                            applyDamageTo(other, 1);
                        }
                        other.vx *= slowX;
                        other.vy *= slowY;
                    }

                    if (dist < highRadius && random.nextInt(highRoll) == 0) {
                        other.highTimer = highDuration;
                    }
                }
            }

            if (Math.random() < (heisen ? 0.08 : 0.1)) highTimer = heisen ? 100 : 120;
        }

        if (highTimer > 0) highTimer--;
    }

    private void handleEaglePassive(boolean airborne) {
        if (type == BirdGame3.BirdType.EAGLE) {
            if (y < game.GROUND_Y - 800) {
                powerMultiplier = Math.max(powerMultiplier, 1.3);
                speedMultiplier = Math.max(speedMultiplier, 1.2);
                if (Math.random() < 0.3) {
                    game.particles.add(new Particle(x + 40 + (Math.random() - 0.5) * 60,
                            y + 80, (Math.random() - 0.5) * 6, 2 + Math.random() * 4,
                            Color.GOLD.deriveColor(0, 1, 1, 0.7)));
                }
            } else if (y < game.GROUND_Y - 400) {
                powerMultiplier = Math.max(powerMultiplier, 1.1);
            }
        } else if (type == BirdGame3.BirdType.FALCON && airborne) {
            if (y < game.GROUND_Y - 700) {
                powerMultiplier = Math.max(powerMultiplier, 1.22);
                speedMultiplier = Math.max(speedMultiplier, 1.26);
                if (Math.random() < 0.28) {
                    game.particles.add(new Particle(x + 40 + (Math.random() - 0.5) * 52,
                            y + 80, (Math.random() - 0.5) * 6, 2 + Math.random() * 4,
                            Color.web("#FFCC80").deriveColor(0, 1, 1, 0.75)));
                }
            } else if (y < game.GROUND_Y - 340) {
                powerMultiplier = Math.max(powerMultiplier, 1.08);
            }
        }
    }

    private void handleTitmouseZip() {
        if (type == BirdGame3.BirdType.TITMOUSE && isZipping) {
            zipTimer--;
            if (zipTimer > 0) {
                double progress = 1.0 - (zipTimer / 30.0);
                x = x + (zipTargetX - x) * progress * 0.4;
                y = y + (zipTargetY - y) * progress * 0.4;

                for (int i = 0; i < 5; i++) {
                    game.particles.add(new Particle(
                            x + 40 + (Math.random() - 0.5) * 30,
                            y + 40 + (Math.random() - 0.5) * 30,
                            (Math.random() - 0.5) * 20,
                            (Math.random() - 0.5) * 20,
                            Color.SKYBLUE.deriveColor(0, 1, 1, 0.6)
                    ));
                }
            } else {
                handleTitmousZipImpact();
                isZipping = false;
            }
            vx = vy = 0;
        }
    }

    private void handleTitmousZipImpact() {
        for (Bird other : game.players) {
            if (!canDamageTarget(other)) continue;
            double dist = Math.hypot(other.x - x, other.y - y);
            if (dist < 120) {
                int dmg = (int) (20 * powerMultiplier);
                double oldHealth = other.health;
                double dealt = applyDamageTo(other, dmg);
                game.damageDealt[playerIndex] += (int) dealt;
                game.recordSpecialImpact(playerIndex, (int) dealt, dealt > 0);
                if (other.health <= 0 && oldHealth > 0) game.eliminations[playerIndex]++;
                if (game.isSfxEnabled() && game.zombieFallingClip != null) game.zombieFallingClip.play();

                other.vx += (other.x > x ? 1 : -1) * 25;
                other.vy -= 18;

                game.addToKillFeed(name.split(":")[0].trim() + " ZAPPED " + other.name.split(":")[0].trim() + "! -" + dmg + " HP");

                game.hitstopFrames = 12;
                game.shakeIntensity = 28;
                game.triggerFlash(0.8, other.health <= 0);

                for (int i = 0; i < 60; i++) {
                    double angle = Math.random() * Math.PI * 2;
                    double speed = 8 + Math.random() * 16;
                    game.particles.add(new Particle(
                            other.x + 40,
                            other.y + 40,
                            Math.cos(angle) * speed,
                            Math.sin(angle) * speed - 6,
                            Color.SKYBLUE.brighter()
                    ));
                }
            }
        }
    }

    private boolean handleBatHanging(boolean stunned) {
        if (batHanging) {
            if (stunned || batHangPlatform == null || !game.platforms.contains(batHangPlatform)) {
                batHanging = false;
                batHangPlatform = null;
                return false;
            }

            double leftBound = batHangPlatform.x + 10;
            double rightBound = batHangPlatform.x + batHangPlatform.w - (80 * sizeMultiplier) - 10;
            x = Math.max(leftBound, Math.min(x, rightBound));
            y = batHangPlatform.y + batHangPlatform.h + 2;
            vy = 0;

            double hangSpeed = type.speed * speedMultiplier * 0.72;
            if (game.pressedKeys.contains(leftKey())) {
                vx = -hangSpeed;
            } else if (game.pressedKeys.contains(rightKey())) {
                vx = hangSpeed;
            } else {
                vx *= 0.55;
            }
            x += vx;
            if (Math.abs(vx) > 0.05) facingRight = vx > 0;

            if (game.pressedKeys.contains(jumpKey())) {
                if (batHangLockTimer <= 0) {
                    batHanging = false;
                    batHangPlatform = null;
                    vy = 2;
                    return false;
                }
            }

            if (game.pressedKeys.contains(attackKey()) && attackCooldown <= 0 && !isBlocking) {
                attack();
                if (game.butterClip != null) game.butterClip.play();
                attackCooldown = scaledAttackCooldown(30);
                attackAnimationTimer = overchargeAttackTimer > 0 ? 10 : 12;
            }
            if (game.pressedKeys.contains(specialKey()) && specialCooldown <= 0 && !isBlocking) {
                special();
            }
            return true;
        }

        if (!stunned && !isOnGround() && vy < -2 && game.pressedKeys.contains(jumpKey())) {
            Platform hangable = findBatHangablePlatform();
            if (hangable != null) {
                batHanging = true;
                batHangPlatform = hangable;
                batHangLockTimer = 14; // prevents immediate unlatch from the same jump press
                vx *= 0.35;
                vy = 0;
                y = hangable.y + hangable.h + 2;
                canDoubleJump = true;
                return true;
            }
        }
        return false;
    }

    private Platform findBatHangablePlatform() {
        Platform best = null;
        double bestDist = Double.MAX_VALUE;
        double centerX = x + 40 * sizeMultiplier;
        for (Platform p : game.platforms) {
            if (p.w < 120) continue;
            if (centerX < p.x + 20 || centerX > p.x + p.w - 20) continue;

            double undersideY = p.y + p.h;
            double headY = y;
            double dy = Math.abs(headY - undersideY);
            if (dy <= 28) {
                if (dy < bestDist) {
                    bestDist = dy;
                    best = p;
                }
            }
        }
        return best;
    }

    private void handleHorizontalMovement(boolean stunned, boolean airborne) {
        if (!stunned) {
            if (dashCooldown > 0) dashCooldown--;
            if (dashTimer > 0) dashTimer--;

            double targetVx = 0;
            double airFric = airborne ? 0.90 : 0.75;
            double accel = airborne ? 0.20 : 0.45;
            double moveSpeed = type.speed * speedMultiplier;
            double speedRatio = baseSpeedMultiplier > 0 ? speedMultiplier / baseSpeedMultiplier : 1.0;
            if (airborne) {
                accel *= Math.max(0.85, Math.min(1.55, 1.0 + (speedRatio - 1.0) * 0.85));
            } else {
                accel *= Math.max(0.9, Math.min(1.45, 1.0 + (speedRatio - 1.0) * 0.65));
            }
            if (type == BirdGame3.BirdType.BAT) {
                moveSpeed *= airborne ? 1.48 : 0.62;
                airFric = airborne ? 0.93 : 0.70;
                accel = airborne ? 0.28 : 0.34;
            }

            boolean leftPressed = game.pressedKeys.contains(leftKey());
            boolean rightPressed = game.pressedKeys.contains(rightKey());

            if (leftPressed) {
                targetVx = -moveSpeed;
                if (type == BirdGame3.BirdType.HUMMINGBIRD && game.pressedKeys.contains(jumpKey()) && airborne) {
                    targetVx *= 1.75;
                }
            }
            else if (rightPressed) {
                targetVx = moveSpeed;
                if (type == BirdGame3.BirdType.HUMMINGBIRD && game.pressedKeys.contains(jumpKey()) && airborne) {
                    targetVx *= 1.75;
                }
            }

            vx = vx * airFric + targetVx * accel;
            if (dashTimer > 0 && !airborne) {
                double dashSpeed = moveSpeed * 2.8;
                vx = (lastTapDir < 0 ? -dashSpeed : dashSpeed);
            }
            if (Math.abs(vx) > 0.1) facingRight = vx > 0;

            boolean canJump = isOnGround() || (type == BirdGame3.BirdType.PIGEON && canDoubleJump);
            if (game.pressedKeys.contains(jumpKey()) && canJump) {
                double mult = isOnGround() ? 1.0 : 0.75;
                vy = -type.jumpHeight * mult;
                if (!isOnGround() && type == BirdGame3.BirdType.PIGEON) canDoubleJump = false;
                if (game.isSfxEnabled() && game.swingClip != null) game.swingClip.play();
            }

            // Track rooftop jumps
            if (game.selectedMap == MapType.CITY && game.pressedKeys.contains(jumpKey()) && canJump && y < game.GROUND_Y - 500) {
                game.rooftopJumps[playerIndex]++;
                game.achievementProgress[10]++;
                if (game.achievementProgress[10] >= 20 && !game.achievementsUnlocked[10]) {
                    game.unlockAchievement(10, "ROOFTOP RUNNER!");
                }
            }

            if (game.selectedMap == MapType.SKYCLIFFS && game.pressedKeys.contains(jumpKey()) && canJump && y < game.GROUND_Y - 1000) {
                game.highCliffJumps[playerIndex]++;
                game.achievementProgress[14]++;
                if (game.achievementProgress[14] >= 15 && !game.achievementsUnlocked[14]) {
                    game.unlockAchievement(14, "CLIFF DIVER!");
                }
            }

            if (game.pressedKeys.contains(attackKey()) && attackCooldown <= 0 && !isBlocking) {
                attack();
                if (game.butterClip != null) game.butterClip.play();
                attackCooldown = scaledAttackCooldown(30);
                attackAnimationTimer = overchargeAttackTimer > 0 ? 10 : 12;
            }

            if (game.pressedKeys.contains(specialKey())) {
                if (grappleUses > 0 && !isOnGround() && !isGrappling && specialCooldown <= 0 && !isBlocking) {
                    // grapple handled in handleVineGrapple
                } else if (grappleUses == 0 && specialCooldown <= 0 && !isBlocking) {
                    special();
                } else if (!game.isAI[playerIndex] && specialCooldown > 0) {
                    cooldownFlash = 15;
                }
            }
        } else {
            vx *= 0.92;
        }
    }

    private void handleEagleDiveImpact() {
        if (type != BirdGame3.BirdType.EAGLE && type != BirdGame3.BirdType.FALCON) return;
        if (!eagleDiveActive) return;

        if (type == BirdGame3.BirdType.FALCON) {
            handleFalconDiveImpact();
            return;
        }

        if (!eagleAscentActive) {
            // Dive phase: force a committed downward slam.
            if (vy < 18) vy = 18;
            vx *= 0.96;

            if (isOnGround()) {
                processDiveImpact();
                eagleAscentActive = true;
                eagleAscentFrames = 36;
                vy = -20;
                canDoubleJump = true;
            }
            return;
        }

        // Ascent phase: the eagle rockets upward and damages enemies it passes through.
        if (eagleAscentFrames > 0) eagleAscentFrames--;
        if (vy > -16 && eagleAscentFrames > 16) vy = -16;
        applyEagleAscentHits();

        if (eagleAscentFrames <= 0 || vy >= -1.0) {
            eagleDiveActive = false;
            eagleAscentActive = false;
        }
    }

    public void registerDashTap(int dir) {
        if (dir == 0) return;
        if (dashCooldown > 0) return;
        if (!isOnGround()) return;
        long now = System.nanoTime();
        long window = 300_000_000L; // 300 ms
        if (dir == lastTapDir && (now - lastTapTimeNs) <= window) {
            dashTimer = 12;
            dashCooldown = 20;
            lastTapTimeNs = 0L;
        } else {
            lastTapDir = dir;
            lastTapTimeNs = now;
        }
    }

    private void handleFalconDiveImpact() {
        double diagSpeed = Math.max(18, Math.max(Math.abs(vx), Math.abs(vy)));
        diagSpeed = Math.min(diagSpeed, 26);
        vy = diagSpeed;
        vx = (facingRight ? 1 : -1) * diagSpeed;
        applyFalconDiveSweetspotHits();

        if (isOnGround()) {
            processDiveImpact();
            eagleDiveActive = false;
            eagleAscentActive = false;
            diveTimer = 0;
            canDoubleJump = true;
        }
    }

    private boolean canDamageTarget(Bird other) {
        return game.canDamage(this, other);
    }

    private double incomingDamageMultiplier() {
        double mult = 1.0;
        if (titanActive && titanTimer > 0) mult *= 0.75;
        if (shrinkTimer > 0) mult *= 1.22;
        return mult;
    }

    private double outgoingDamageMultiplier() {
        if (type == BirdGame3.BirdType.PHOENIX && phoenixRebornActive) return PHOENIX_REBORN_DAMAGE_SCALE;
        return 1.0;
    }

    public double getMaxHealth() {
        if (type == BirdGame3.BirdType.PHOENIX && phoenixRebornActive) return PHOENIX_REBORN_HEALTH;
        return 100.0;
    }

    private double applyDamageTo(Bird target, double rawDamage) {
        if (target == null || rawDamage <= 0 || target.health <= 0) return 0;
        double oldHealth = target.health;
        double scaledDamage = rawDamage * outgoingDamageMultiplier() * target.incomingDamageMultiplier();
        if (game.isTrainingDummy(target)) {
            target.health = target.getMaxHealth();
            return scaledDamage;
        }
        target.health = Math.max(0, target.health - scaledDamage);
        if (target.health <= 0) {
            target.tryPhoenixRebirth();
        }
        return oldHealth - target.health;
    }

    private boolean tryPhoenixRebirth() {
        if (type != BirdGame3.BirdType.PHOENIX || phoenixRebornUsed || health > 0) return false;
        phoenixRebornUsed = true;
        phoenixRebornActive = true;

        baseSizeMultiplier *= PHOENIX_REBORN_SIZE_SCALE;
        basePowerMultiplier *= PHOENIX_REBORN_POWER_SCALE;
        baseSpeedMultiplier *= PHOENIX_REBORN_SPEED_SCALE;
        sizeMultiplier = baseSizeMultiplier;
        powerMultiplier = basePowerMultiplier;
        speedMultiplier = baseSpeedMultiplier;

        health = getMaxHealth();
        stunTime = 0;
        canDoubleJump = true;
        phoenixAfterburnTimer = 0;

        String who = name.split(":")[0].trim();
        game.addToKillFeed(who + " REBORN FROM THE ASHES!");
        game.shakeIntensity = Math.max(game.shakeIntensity, 18);
        game.hitstopFrames = Math.max(game.hitstopFrames, 8);
        for (int i = 0; i < 70; i++) {
            double angle = Math.random() * Math.PI * 2;
            double speed = 5 + Math.random() * 10;
            Color c = Math.random() < 0.6 ? Color.ORANGERED : Color.GOLD;
            game.particles.add(new Particle(
                    x + 40,
                    y + 40,
                    Math.cos(angle) * speed,
                    Math.sin(angle) * speed - 4.5,
                    c.deriveColor(0, 1, 1, 0.9)
            ));
        }
        return true;
    }

    private int scaledAttackCooldown(int baseFrames) {
        if (overchargeAttackTimer <= 0) return baseFrames;
        return Math.max(10, (int) Math.round(baseFrames * 0.62));
    }

    private void handleHummingbirdFrenzy() {
        if (type != BirdGame3.BirdType.HUMMINGBIRD || hummingFrenzyTimer <= 0) return;

        double centerX = x + 40;
        double centerY = y + 40;

        for (int i = 0; i < 2; i++) {
            Color c = Math.random() < 0.5 ? Color.CYAN.brighter() : Color.YELLOW.brighter();
            game.particles.add(new Particle(
                    centerX + (Math.random() - 0.5) * 56,
                    centerY + (Math.random() - 0.5) * 44,
                    (Math.random() - 0.5) * 4.2,
                    (Math.random() - 0.5) * 4.2 - 1.8,
                    c.deriveColor(0, 1, 1, 0.75)
            ));
        }

        for (Bird other : game.players) {
            if (!canDamageTarget(other)) continue;
            if (other.playerIndex < 0 || other.playerIndex >= hummingFrenzyHitCooldown.length) continue;
            if (hummingFrenzyHitCooldown[other.playerIndex] > 0) continue;

            double dx = (other.x + 40) - centerX;
            double dy = (other.y + 40) - centerY;
            if (Math.abs(dx) > 118 || Math.abs(dy) > 105) continue;

            int dmg = 2 + random.nextInt(2);
            double oldHealth = other.health;
            int dealt = (int) applyDamageTo(other, dmg);
            if (dealt <= 0) continue;

            game.damageDealt[playerIndex] += dealt;
            game.recordSpecialImpact(playerIndex, dealt, true);
            if (other.health <= 0 && oldHealth > 0) game.eliminations[playerIndex]++;

            other.vx += (dx >= 0 ? 1 : -1) * 4.2;
            other.vy -= 3.5;
            health = Math.min(getMaxHealth(), health + dealt * 0.35);
            hummingFrenzyHitCooldown[other.playerIndex] = 8;

            for (int i = 0; i < 8; i++) {
                double angle = Math.random() * Math.PI * 2;
                game.particles.add(new Particle(
                        other.x + 40, other.y + 40,
                        Math.cos(angle) * (3 + Math.random() * 4),
                        Math.sin(angle) * (3 + Math.random() * 4) - 2,
                        Color.LIME.deriveColor(0, 1, 1, 0.82)
                ));
            }
        }
    }

    private void handlePhoenixAfterburn() {
        if (type != BirdGame3.BirdType.PHOENIX || phoenixAfterburnTimer <= 0) return;

        double centerX = x + 40;
        double centerY = y + 40;

        // Flames shoot outward from Phoenix's body while the special lingers.
        for (int i = 0; i < 5; i++) {
            double angle = Math.random() * Math.PI * 2;
            double speed = 6 + Math.random() * 10;
            double spawnDist = 8 + Math.random() * 16;
            Color c = Math.random() < 0.55 ? Color.ORANGERED : Color.GOLD;
            game.particles.add(new Particle(
                    centerX + Math.cos(angle) * spawnDist,
                    centerY + Math.sin(angle) * spawnDist,
                    Math.cos(angle) * speed,
                    Math.sin(angle) * speed - 4.5,
                    c.deriveColor(0, 1, 1, 0.88)
            ));
        }

        if (phoenixAfterburnTimer % 8 != 0) return;

        for (Bird other : game.players) {
            if (!canDamageTarget(other)) continue;
            if (other.playerIndex < 0 || other.playerIndex >= phoenixAfterburnHitCooldown.length) continue;
            if (phoenixAfterburnHitCooldown[other.playerIndex] > 0) continue;

            double dx = (other.x + 40) - centerX;
            double dy = (other.y + 40) - centerY;
            double dist = Math.hypot(dx, dy);
            if (dist > 185) continue;

            int dmg = dist < 120 ? 4 : 3;
            double oldHealth = other.health;
            int dealt = (int) applyDamageTo(other, dmg);
            if (dealt <= 0) continue;

            game.damageDealt[playerIndex] += dealt;
            game.recordSpecialImpact(playerIndex, dealt, true);
            if (other.health <= 0 && oldHealth > 0) game.eliminations[playerIndex]++;

            double safeDist = Math.max(0.001, dist);
            other.vx += dx / safeDist * 3.6;
            other.vy -= 2.6;
            phoenixAfterburnHitCooldown[other.playerIndex] = 12;
        }
    }

    private void applyPenguinDashDamage() {
        if (type != BirdGame3.BirdType.PENGUIN || penguinDashDamageTimer <= 0) return;
        for (Bird other : game.players) {
            if (!canDamageTarget(other)) continue;
            if (other.playerIndex < 0 || other.playerIndex >= penguinDashHit.length) continue;
            if (penguinDashHit[other.playerIndex]) continue;

            double dx = (other.x + 40) - (x + 40);
            double dy = (other.y + 40) - (y + 40);
            if (Math.abs(dx) > 90 || Math.abs(dy) > 95) continue;

            int dmg = 10 + random.nextInt(5);
            double oldHealth = other.health;
            int dealt = (int) applyDamageTo(other, dmg);
            game.damageDealt[playerIndex] += dealt;
            game.recordSpecialImpact(playerIndex, dealt, dealt > 0);
            if (other.health <= 0 && oldHealth > 0) game.eliminations[playerIndex]++;

            other.vx += (dx >= 0 ? 1 : -1) * 11;
            other.vy -= 9;
            penguinDashHit[other.playerIndex] = true;
            game.addToKillFeed(name.split(":")[0].trim() + " ICE-CHECKED " + other.name.split(":")[0].trim() + "! -" + dmg + " HP");

            for (int i = 0; i < 14; i++) {
                double ang = Math.random() * Math.PI * 2;
                game.particles.add(new Particle(
                        other.x + 40, other.y + 40,
                        Math.cos(ang) * (4 + Math.random() * 7),
                        Math.sin(ang) * (4 + Math.random() * 7) - 3,
                        Color.web("#B3E5FC")
                ));
            }
        }
    }

    private void applyEagleAscentHits() {
        for (Bird other : game.players) {
            if (!canDamageTarget(other)) continue;
            if (other.playerIndex < 0 || other.playerIndex >= eagleAscentHit.length) continue;
            if (eagleAscentHit[other.playerIndex]) continue;

            double dx = (other.x + 40) - (x + 40);
            double dy = (other.y + 40) - (y + 40);
            if (Math.abs(dx) < 120 && Math.abs(dy) < 115) {
                int dmg = 9 + random.nextInt(6);
                double oldHealth = other.health;
                int dealt = (int) applyDamageTo(other, dmg);
                game.damageDealt[playerIndex] += dealt;
                game.recordSpecialImpact(playerIndex, dealt, dealt > 0);
                if (other.health <= 0 && oldHealth > 0) game.eliminations[playerIndex]++;

                other.vx += (dx >= 0 ? 1 : -1) * 8;
                other.vy -= 9;
                eagleAscentHit[other.playerIndex] = true;

                game.addToKillFeed(name.split(":")[0].trim() + " ASCENT-SLASHED " +
                        other.name.split(":")[0].trim() + "! -" + dmg + " HP");

                for (int i = 0; i < 22; i++) {
                    double angle = -Math.PI / 2 + (Math.random() - 0.5) * 1.2;
                    game.particles.add(new Particle(other.x + 40, other.y + 40,
                            Math.cos(angle) * (6 + Math.random() * 8),
                            Math.sin(angle) * (10 + Math.random() * 12),
                            Color.GOLD.brighter()));
                }
            }
        }
    }

    private void applyFalconDiveSweetspotHits() {
        for (Bird other : game.players) {
            if (!canDamageTarget(other)) continue;
            if (other.playerIndex < 0 || other.playerIndex >= eagleAscentHit.length) continue;
            if (eagleAscentHit[other.playerIndex]) continue;

            double dx = (other.x + 40) - (x + 40);
            double dy = (other.y + 40) - (y + 40);
            if (facingRight && dx < -12) continue;
            if (!facingRight && dx > 12) continue;
            if (Math.abs(dx) > 95 || Math.abs(dy) > 80) continue;

            double dist = Math.hypot(dx, dy);
            boolean sweetspot = dist < 46;
            int base = sweetspot ? 30 : 16;
            int dmg = Math.max(6, (int) Math.round(base * powerMultiplier));
            double oldHealth = other.health;
            int dealt = (int) applyDamageTo(other, dmg);
            if (dealt <= 0) continue;

            game.damageDealt[playerIndex] += dealt;
            game.recordSpecialImpact(playerIndex, dealt, true);
            if (other.health <= 0 && oldHealth > 0) game.eliminations[playerIndex]++;

            double kb = sweetspot ? 16 : 10;
            other.vx += (dx >= 0 ? 1 : -1) * kb;
            other.vy -= sweetspot ? 13 : 8;
            eagleAscentHit[other.playerIndex] = true;

            String attacker = name.split(":")[0].trim();
            String victim = other.name.split(":")[0].trim();
            if (sweetspot) {
                game.addToKillFeed(attacker + " SWEETSPOTTED " + victim + "! -" + dealt + " HP");
            } else {
                game.addToKillFeed(attacker + " clipped " + victim + " with dive talons! -" + dealt + " HP");
            }

            game.shakeIntensity = Math.max(game.shakeIntensity, sweetspot ? 20 : 11);
            game.hitstopFrames = Math.max(game.hitstopFrames, sweetspot ? 10 : 6);
            game.triggerFlash(sweetspot ? 0.65 : 0.35, other.health <= 0 && oldHealth > 0);

            Color spark = sweetspot ? Color.web("#FFE082") : Color.web("#FF7043");
            for (int i = 0; i < (sweetspot ? 24 : 14); i++) {
                double angle = Math.random() * Math.PI * 2;
                game.particles.add(new Particle(
                        other.x + 40,
                        other.y + 40,
                        Math.cos(angle) * (4 + Math.random() * 8),
                        Math.sin(angle) * (4 + Math.random() * 8) - 3,
                        spark
                ));
            }
        }
    }

    private void processDiveImpact() {
        if (type == BirdGame3.BirdType.FALCON) {
            processFalconDiveImpact();
            return;
        }

        game.shakeIntensity = 24;
        game.hitstopFrames = 14;
        game.addToKillFeed("KABOOM! " + name.split(":")[0].trim() + " slams the ground!");
        Arrays.fill(eagleAscentHit, false);

        for (int i = 0; i < 140; i++) {
            double angle = i / 300.0 * Math.PI * 2;
            double speed = 7 + Math.random() * 14;
            double vx = Math.cos(angle) * speed;
            double vy = Math.sin(angle) * speed - 6;
            Color c = Math.random() < 0.5 ? Color.ORANGERED : Color.YELLOW.brighter();
            game.particles.add(new Particle(x + 40, y + 70, vx, vy, c));
        }

        for (int i = 0; i < 10; i++) {
            double offset = (Math.random() - 0.5) * 520;
            for (int j = 0; j < 9; j++) {
                game.particles.add(new Particle(x + 40 + offset + j * 10, game.GROUND_Y + j * 10,
                        (Math.random() - 0.5) * 14, -4 - Math.random() * 9, Color.SADDLEBROWN.darker()));
            }
        }

        for (Bird other : game.players) {
            if (!canDamageTarget(other)) continue;

            double dx = other.x - x;
            double dy = (other.y + 40) - (y + 70);
            double dist = Math.hypot(dx, dy);

            if (dist < 300) {
                int dmg = (int) (22 * (1.0 - dist / 420.0));
                if (dmg < 6) dmg = 6;

                double oldHealth = other.health;
                int dealt = (int) applyDamageTo(other, dmg);
                game.damageDealt[playerIndex] += dealt;
                game.recordSpecialImpact(playerIndex, dealt, dealt > 0);
                if (other.health <= 0 && oldHealth > 0) game.eliminations[playerIndex]++;

                double safeDist = Math.max(0.001, dist);
                other.vx += dx / safeDist * 18;
                other.vy -= 14;

                String intensity = dmg >= 50 ? "DEVASTATED" : dmg >= 35 ? "BLASTED" : "SMASHED";
                game.addToKillFeed(name.split(":")[0].trim() + " " + intensity + " " + other.name.split(":")[0].trim() + "! -" + dmg + " HP");

                if (dmg > 16) {
                    for (int k = 0; k < 24; k++) {
                        double angle = Math.random() * Math.PI * 2;
                        game.particles.add(new Particle(other.x + 40, other.y + 40,
                                Math.cos(angle) * (6 + Math.random() * 11),
                                Math.sin(angle) * (6 + Math.random() * 11) - 6,
                                Color.CRIMSON.brighter()));
                    }
                }
            }
        }
    }

    private void processFalconDiveImpact() {
        game.shakeIntensity = Math.max(game.shakeIntensity, 16);
        game.hitstopFrames = Math.max(game.hitstopFrames, 10);
        game.addToKillFeed(name.split(":")[0].trim() + " lands a precision strike!");
        Arrays.fill(eagleAscentHit, false);

        for (int i = 0; i < 88; i++) {
            double angle = i / 180.0 * Math.PI * 2;
            double speed = 6 + Math.random() * 10;
            Color c = Math.random() < 0.5 ? Color.web("#FF7043") : Color.web("#FFE082");
            game.particles.add(new Particle(
                    x + 40,
                    y + 68,
                    Math.cos(angle) * speed,
                    Math.sin(angle) * speed - 4.5,
                    c
            ));
        }

        for (Bird other : game.players) {
            if (!canDamageTarget(other)) continue;

            double dx = (other.x + 40) - (x + 40);
            double dy = (other.y + 40) - (y + 70);
            double dist = Math.hypot(dx, dy);
            if (dist > 230) continue;

            boolean sweetspot = dist < 95;
            int base = sweetspot ? 26 : (dist < 170 ? 14 : 8);
            int dmg = Math.max(6, (int) Math.round(base * powerMultiplier));
            double oldHealth = other.health;
            int dealt = (int) applyDamageTo(other, dmg);
            if (dealt <= 0) continue;

            game.damageDealt[playerIndex] += dealt;
            game.recordSpecialImpact(playerIndex, dealt, true);
            if (other.health <= 0 && oldHealth > 0) game.eliminations[playerIndex]++;

            double safeDist = Math.max(0.001, dist);
            other.vx += dx / safeDist * (sweetspot ? 15 : 10);
            other.vy -= sweetspot ? 12 : 8;

            String victim = other.name.split(":")[0].trim();
            if (sweetspot) {
                game.addToKillFeed(name.split(":")[0].trim() + " SWEETSPOT DOVE " + victim + "! -" + dealt + " HP");
                game.triggerFlash(0.7, other.health <= 0 && oldHealth > 0);
            } else {
                game.addToKillFeed(name.split(":")[0].trim() + " tagged " + victim + " on impact! -" + dealt + " HP");
            }
        }
    }

    private void handleRazorbillBladeStorm() {
        if (type != BirdGame3.BirdType.RAZORBILL || bladeStormFrames <= 0) return;

        double dashX = razorbillDashVX;
        double dashY = razorbillDashVY;
        double dashMag = Math.hypot(dashX, dashY);
        if (dashMag < 0.1) {
            dashX = vx;
            dashY = vy;
            dashMag = Math.hypot(dashX, dashY);
            if (dashMag < 0.1) {
                dashX = facingRight ? 1 : -1;
                dashY = 0;
                dashMag = 1.0;
            }
            double dashSpeed = Math.max(12.0, RAZORBILL_DASH_SPEED * speedMultiplier);
            razorbillDashVX = dashX / dashMag * dashSpeed;
            razorbillDashVY = dashY / dashMag * dashSpeed;
            dashX = razorbillDashVX;
            dashY = razorbillDashVY;
            dashMag = Math.hypot(dashX, dashY);
        }

        vx = dashX;
        vy = dashY;

        double dirX = dashX / dashMag;
        double dirY = dashY / dashMag;

        for (Bird other : game.players) {
            if (!canDamageTarget(other)) continue;
            if (other.playerIndex < 0 || other.playerIndex >= razorbillDashHit.length) continue;
            if (razorbillDashHit[other.playerIndex]) continue;

            double dx = (other.x + 40) - (x + 40);
            double dy = (other.y + 40) - (y + 40);
            double dist = Math.hypot(dx, dy);
            if (dist > 85) continue;

            int dmg = Math.max(6, (int) Math.round((11 + random.nextInt(5)) * powerMultiplier));
            double oldHealth = other.health;
            int dealt = (int) applyDamageTo(other, dmg);
            if (dealt <= 0) continue;

            game.damageDealt[playerIndex] += dealt;
            game.recordSpecialImpact(playerIndex, dealt, true);
            if (other.health <= 0 && oldHealth > 0) game.eliminations[playerIndex]++;

            other.vx += dirX * 12;
            other.vy += dirY * 12;
            razorbillDashHit[other.playerIndex] = true;

            game.addToKillFeed(name.split(":")[0].trim() + " PIERCED " +
                    other.name.split(":")[0].trim() + "! -" + dealt + " HP");

            for (int k = 0; k < 16; k++) {
                double angle = Math.atan2(dirY, dirX) + (Math.random() - 0.5) * 1.4;
                double speed = 6 + Math.random() * 9;
                game.particles.add(new Particle(other.x + 40, other.y + 40,
                        Math.cos(angle) * speed,
                        Math.sin(angle) * speed,
                        Color.CYAN.brighter()));
            }
            game.shakeIntensity = Math.max(game.shakeIntensity, 14);
            game.hitstopFrames = Math.max(game.hitstopFrames, 6);
        }

        if (bladeStormFrames % 3 == 0) {
            for (int i = 0; i < 6; i++) {
                double angle = Math.atan2(dirY, dirX) + Math.PI + (Math.random() - 0.5) * 0.9;
                double speed = 4 + Math.random() * 6;
                game.particles.add(new Particle(
                        x + 40 + (Math.random() - 0.5) * 16,
                        y + 40 + (Math.random() - 0.5) * 16,
                        Math.cos(angle) * speed,
                        Math.sin(angle) * speed,
                        Color.WHITE.deriveColor(0, 1, 1, 0.9)
                ));
            }
        }
    }

    private void handleThermals() {
        if (game.selectedMap == MapType.SKYCLIFFS || game.selectedMap == MapType.VIBRANT_JUNGLE || game.selectedMap == MapType.CAVE) {
            for (WindVent v : game.windVents) {
                if (v.cooldown > 0) continue;
                double centerX = v.x + v.w / 2;
                double centerY = v.y - 75;
                double dx = (x + 40) - centerX;
                double dy = (y + 40) - centerY;
                if (Math.pow(dx / (v.w / 2 + 50), 2) + Math.pow(dy / 200, 2) <= 1.0) {
                    vy = Math.min(vy, BirdGame3.WIND_FORCE);
                    if (Math.random() < 0.3) {
                        game.particles.add(new Particle(x + 40 + (Math.random() - 0.5) * 40, y + 80,
                                (Math.random() - 0.5) * 8, -4 - Math.random() * 8, Color.CYAN.deriveColor(0, 1, 1, 0.7)));
                    }
                    break;
                }
            }
        }
    }

    private void handleBoundaries() {
        double leftBound = 50;
        double rightBound = game.WORLD_WIDTH - 150 * sizeMultiplier;
        double outLeft = -300;
        double outRight = game.WORLD_WIDTH + 300;
        if (game.selectedMap == MapType.BATTLEFIELD) {
            double battlefieldLeft = game.battlefieldLeftBound();
            double battlefieldRight = game.battlefieldRightBound();
            leftBound = battlefieldLeft + 50;
            rightBound = battlefieldRight - 150 * sizeMultiplier;
            outLeft = battlefieldLeft - 300;
            outRight = battlefieldRight + 300;
        }

        if (x < leftBound) x = leftBound;
        if (x > rightBound) x = rightBound;

        boolean trainingDummy = game.isTrainingDummy(this);

        if (x < outLeft || x > outRight) {
            health = Math.max(0, health - 50);
            boolean reborn = false;
            if (trainingDummy) {
                health = getMaxHealth();
                reborn = true;
            } else if (health <= 0) {
                reborn = tryPhoenixRebirth();
                if (!reborn) {
                    game.addToKillFeed(name.split(":")[0].trim() + " FLEW INTO THE VOID!");
                }
            } else {
                game.addToKillFeed(name.split(":")[0].trim() + " went out of bounds... -50 HP");
            }
            if (!reborn && game.isSfxEnabled() && game.zombieFallingClip != null) game.zombieFallingClip.play();
            if (game.selectedMap == MapType.BATTLEFIELD) {
                double centerX = game.battlefieldSpawnCenterX();
                x = centerX - 40 * sizeMultiplier;
                y = game.battlefieldSpawnY(sizeMultiplier);
            } else {
                x = 2000 + playerIndex * 600;
                y = game.GROUND_Y - 400;
            }
            vx = 0;
            vy = 0;
            canDoubleJump = true;

        }

        if (y < game.CEILING_Y) {
            y = game.CEILING_Y;
            vy = Math.max(vy, 0);
            if (type == BirdGame3.BirdType.VULTURE) isFlying = false;
        }

        handleVerticalCollision();

        if (y > game.WORLD_HEIGHT + 300) {
            game.falls[playerIndex]++;
            if (game.selectedMap == MapType.BATTLEFIELD) {
                health = 0;
            } else {
                health = Math.max(0, health - 50);
            }
            boolean reborn = false;
            if (trainingDummy) {
                health = getMaxHealth();
                reborn = true;
            } else if (health <= 0) {
                reborn = tryPhoenixRebirth();
                if (!reborn) {
                    String msg = (game.selectedMap == MapType.BATTLEFIELD)
                            ? name.split(":")[0].trim() + " FELL INTO THE VOID!"
                            : name.split(":")[0].trim() + " FELL TO THEIR DOOM!";
                    game.addToKillFeed(msg);
                }
            } else {
                game.addToKillFeed(name.split(":")[0].trim() + " fell... but survived! -50 HP");
            }
            if (!reborn && game.isSfxEnabled() && game.zombieFallingClip != null) game.zombieFallingClip.play();
            if (game.selectedMap == MapType.BATTLEFIELD) {
                double centerX = game.battlefieldSpawnCenterX();
                x = centerX - 40 * sizeMultiplier;
                y = game.battlefieldSpawnY(sizeMultiplier);
            } else {
                x = 1000 + playerIndex * 800;
                y = game.GROUND_Y - 300;
            }
            vx = vy = 0;
            if (!game.trainingModeActive) {
                game.achievementProgress[7]++;
                if (game.achievementProgress[7] >= 3 && !game.achievementsUnlocked[7]) {
                    game.unlockAchievement(7, "FALL GUY!");
                }
            }
        }
    }

    private void handleVultureFeast() {
        if (type == BirdGame3.BirdType.VULTURE && health > 0) {
            for (Bird b : game.players) {
                if (b != null && b != this && b.health <= 0 && b.y > game.HEIGHT + 50 && b.y <= game.HEIGHT + 100) {
                    health = Math.min(getMaxHealth(), health + 4);
                    game.addToKillFeed(name.split(":")[0].trim() + " FEASTS! +4 HP");
                    for (int i = 0; i < 15; i++) {
                        double angle = Math.random() * Math.PI * 2;
                        game.particles.add(new Particle(b.x + 40, b.y + 40,
                                Math.cos(angle) * 4, Math.sin(angle) * 4 - 3, Color.DARKRED));
                    }
                }
            }
        }
    }

    private void handlePowerUpPickup() {
        for (Iterator<PowerUp> it = game.powerUps.iterator(); it.hasNext(); ) {
            PowerUp p = it.next();
            if (Math.abs(x + 40 - p.x) < 80 && Math.abs(y + 40 - p.y) < 80) {
                handlePowerUpType(p, it);
            }
        }
    }

    private void handlePowerUpType(PowerUp p, Iterator<PowerUp> it) {
        switch (p.type) {
            case HEALTH -> {
                health = Math.max(health, Math.min(getMaxHealth(), health + 40));
                game.addToKillFeed(name.split(":")[0].trim() + " grabbed HEALTH! +40 HP");
            }
            case SPEED -> {
                speedMultiplier = baseSpeedMultiplier * 1.7;
                speedTimer = 480;
                game.addToKillFeed(name.split(":")[0].trim() + " got SPEED BOOST!");
            }
            case RAGE -> {
                powerMultiplier = basePowerMultiplier * 2.0;
                rageTimer = 420;
                game.addToKillFeed(name.split(":")[0].trim() + " is ENRAGED!");
            }
            case SHRINK -> {
                for (Bird b : game.players) {
                    if (b != null && b != this && canDamageTarget(b)) {
                        b.sizeMultiplier = b.baseSizeMultiplier * 0.6;
                        b.shrinkTimer = 360;
                    }
                }
                game.addToKillFeed(name.split(":")[0].trim() + " SHRANK + WEAKENED enemies!");
            }
            case NEON -> {
                speedMultiplier = baseSpeedMultiplier * 2.4;
                speedTimer = 360;
                canDoubleJump = true;
                vy = -18;
                vx = (facingRight ? 1 : -1) * 24;
                neonRushTimer = 180;
                powerMultiplier = basePowerMultiplier * 1.3;
                rageTimer = 360;

                game.addToKillFeed(name.split(":")[0].trim() + " grabbed NEON BOOST! HYPERSPEED!");

                for (int i = 0; i < 80; i++) {
                    double angle = Math.random() * Math.PI * 2;
                    double speed = 10 + Math.random() * 20;
                    Color c = Math.random() < 0.5 ? Color.MAGENTA.brighter() : Color.CYAN.brighter();
                    game.particles.add(new Particle(x + 40, y + 40, Math.cos(angle) * speed, Math.sin(angle) * speed - 8, c));
                }

                game.shakeIntensity = 20;
                game.hitstopFrames = 12;

                game.neonPickups[playerIndex]++;
                game.scores[playerIndex] += 20;
                game.achievementProgress[11]++;
                if (game.achievementProgress[11] >= 8 && !game.achievementsUnlocked[11]) {
                    game.unlockAchievement(11, "NEON ADDICT!");
                }
            }
            case THERMAL -> {
                thermalTimer = 600;
                thermalLift = 1.2;
                game.addToKillFeed(name.split(":")[0].trim() + " rides a THERMAL! SOARING!");
                for (int i = 0; i < 100; i++) {
                    double angle = Math.random() * Math.PI * 2;
                    double speed = 8 + Math.random() * 18;
                    game.particles.add(new Particle(x + 40, y + 40,
                            Math.cos(angle) * speed,
                            Math.sin(angle) * speed - 10,
                            Color.GOLD.brighter()));
                }
                game.shakeIntensity = 15;
                game.hitstopFrames = 10;

                game.thermalPickups[playerIndex]++;
                game.achievementProgress[13]++;
            }
            case VINE_GRAPPLE -> {
                grappleTimer = 480;
                grappleUses = 3;
                game.addToKillFeed(name.split(":")[0].trim() + " grabbed VINE GRAPPLE! Swing freely!");
                for (int i = 0; i < 80; i++) {
                    double angle = Math.random() * Math.PI * 2;
                    double speed = 8 + Math.random() * 16;
                    game.particles.add(new Particle(x + 40, y + 40,
                            Math.cos(angle) * speed, Math.sin(angle) * speed - 6,
                            Color.LIMEGREEN.brighter()));
                }
                game.shakeIntensity = 18;
                game.hitstopFrames = 10;
                game.vineGrapplePickups[playerIndex]++;
                game.achievementProgress[16]++;
                if (game.achievementProgress[16] >= 8 && !game.achievementsUnlocked[16]) {
                    game.unlockAchievement(16, "VINE SWINGER!");
                }
            }
            case OVERCHARGE -> {
                specialCooldown = 0;
                powerMultiplier = Math.max(powerMultiplier, basePowerMultiplier * 1.35);
                rageTimer = Math.max(rageTimer, 260);
                overchargeAttackTimer = Math.max(overchargeAttackTimer, 300);
                game.addToKillFeed(name.split(":")[0].trim() + " got OVERCHARGE! Special reset + rapid attacks!");
                for (int i = 0; i < 65; i++) {
                    double angle = Math.random() * Math.PI * 2;
                    double speed = 8 + Math.random() * 16;
                    game.particles.add(new Particle(x + 40, y + 40,
                            Math.cos(angle) * speed, Math.sin(angle) * speed - 7,
                            Color.DEEPSKYBLUE.brighter()));
                }
            }
            case TITAN -> {
                titanActive = true;
                titanTimer = 420;
                if (shrinkTimer <= 0) {
                    sizeMultiplier = baseSizeMultiplier * 1.35;
                }
                powerMultiplier = Math.max(powerMultiplier, basePowerMultiplier * 1.4);
                game.addToKillFeed(name.split(":")[0].trim() + " entered TITAN FORM! (attack + defense)");
                for (int i = 0; i < 80; i++) {
                    double angle = Math.random() * Math.PI * 2;
                    double speed = 6 + Math.random() * 13;
                    game.particles.add(new Particle(x + 40, y + 40,
                            Math.cos(angle) * speed, Math.sin(angle) * speed - 5,
                            Color.GOLDENROD.brighter()));
                }
                game.shakeIntensity = Math.max(game.shakeIntensity, 18);
            }
        }

        game.achievementProgress[6]++;
        if (game.achievementProgress[6] >= 10 && !game.achievementsUnlocked[6]) {
            game.unlockAchievement(6, "POWER-UP HOARDER!");
        }
        game.checkAchievements(this);
        for (int i = 0; i < 30; i++) {
            double angle = Math.random() * Math.PI * 2;
            game.particles.add(new Particle(p.x, p.y, Math.cos(angle) * 8, Math.sin(angle) * 8 - 4, p.type.color.brighter()));
        }
        it.remove();
    }

    private void handleTaunts() {
        if (!game.isAI[playerIndex]) {
            if (tauntCooldown > 0) tauntCooldown--;

            KeyCode qKey = playerIndex == 0 ? KeyCode.Q :
                    playerIndex == 1 ? KeyCode.NUMPAD1 :
                            playerIndex == 2 ? KeyCode.NUMPAD2 : KeyCode.NUMPAD3;

            KeyCode eKey = playerIndex == 0 ? KeyCode.E :
                    playerIndex == 1 ? KeyCode.NUMPAD4 :
                            playerIndex == 2 ? KeyCode.NUMPAD5 : KeyCode.NUMPAD6;

            if (game.pressedKeys.contains(qKey) && tauntCooldown <= 0) {
                currentTaunt = (currentTaunt % 3) + 1;
                tauntCooldown = 30;
            }

            if (game.pressedKeys.contains(eKey) && tauntCooldown <= 0 && currentTaunt != 0) {
                tauntTimer = 60;
                tauntCooldown = 120;
                game.tauntsPerformed[playerIndex]++;
                game.checkAchievements(this);

                String tauntName = switch (currentTaunt) {
                    case 1 -> "FLIPPED OFF";
                    case 2 -> "CHALLENGED";
                    case 3 -> "MOONED";
                    default -> "TAUNTED";
                };
                game.addToKillFeed(name.split(":")[0].trim() + " " + tauntName + " EVERYONE!");

                for (int i = 0; i < 30; i++) {
                    Color c = currentTaunt == 1 ? Color.YELLOW : currentTaunt == 2 ? Color.RED : Color.PINK;
                    game.particles.add(new Particle(x + 40, y + 40, (Math.random() - 0.5) * 16, (Math.random() - 0.7) * 12, c));
                }
            }
        }
    }

    LanBirdState toLanState() {
        LanBirdState state = new LanBirdState();
        state.typeOrdinal = type.ordinal();
        state.x = x;
        state.y = y;
        state.vx = vx;
        state.vy = vy;
        state.facingRight = facingRight;
        state.health = health;
        state.stunTime = stunTime;
        state.specialCooldown = specialCooldown;
        state.specialMaxCooldown = specialMaxCooldown;
        state.attackCooldown = attackCooldown;
        state.attackAnimationTimer = attackAnimationTimer;
        state.canDoubleJump = canDoubleJump;
        state.loungeActive = loungeActive;
        state.isCitySkin = isCitySkin;
        state.isNoirSkin = isNoirSkin;
        state.isFreemanSkin = isFreemanSkin;
        state.isClassicSkin = isClassicSkin;
        state.isNovaSkin = isNovaSkin;
        state.isDuneSkin = isDuneSkin;
        state.isMintSkin = isMintSkin;
        state.isCircuitSkin = isCircuitSkin;
        state.isPrismSkin = isPrismSkin;
        state.isAuroraSkin = isAuroraSkin;
        state.isBeaconSkin = isBeaconSkin;
        state.isSunflareSkin = isSunflareSkin;
        state.isGlacierSkin = isGlacierSkin;
        state.isTideSkin = isTideSkin;
        state.isEclipseSkin = isEclipseSkin;
        state.isUmbraSkin = isUmbraSkin;
        state.suppressSelectEffects = suppressSelectEffects;
        state.loungeX = loungeX;
        state.loungeY = loungeY;
        state.loungeHealth = loungeHealth;
        state.loungeDamageFlash = loungeDamageFlash;
        state.diveTimer = diveTimer;
        state.isZipping = isZipping;
        state.zipTargetX = zipTargetX;
        state.zipTargetY = zipTargetY;
        state.zipTimer = zipTimer;
        state.isGroundPounding = isGroundPounding;
        state.carrionSwarmTimer = carrionSwarmTimer;
        state.crowSwarmCooldown = crowSwarmCooldown;
        state.isFlying = isFlying;
        state.leanTimer = leanTimer;
        state.leanCooldown = leanCooldown;
        state.isHigh = isHigh;
        state.highTimer = highTimer;
        state.tauntCooldown = tauntCooldown;
        state.tauntTimer = tauntTimer;
        state.cooldownFlash = cooldownFlash;
        state.currentTaunt = currentTaunt;
        state.eagleDiveCountdown = eagleDiveCountdown;
        state.eagleDiveActive = eagleDiveActive;
        state.eagleAscentActive = eagleAscentActive;
        state.eagleAscentFrames = eagleAscentFrames;
        state.bladeStormFrames = bladeStormFrames;
        state.plungeTimer = plungeTimer;
        state.batHanging = batHanging;
        state.batEchoTimer = batEchoTimer;
        state.isBlocking = isBlocking;
        state.blockCooldown = blockCooldown;
        state.speedMultiplier = speedMultiplier;
        state.powerMultiplier = powerMultiplier;
        state.sizeMultiplier = sizeMultiplier;
        state.baseSpeedMultiplier = baseSpeedMultiplier;
        state.basePowerMultiplier = basePowerMultiplier;
        state.baseSizeMultiplier = baseSizeMultiplier;
        state.speedTimer = speedTimer;
        state.rageTimer = rageTimer;
        state.shrinkTimer = shrinkTimer;
        state.titanTimer = titanTimer;
        state.titanActive = titanActive;
        state.neonRushTimer = neonRushTimer;
        state.thermalTimer = thermalTimer;
        state.thermalLift = thermalLift;
        state.overchargeAttackTimer = overchargeAttackTimer;
        state.speedBoostTimer = speedBoostTimer;
        state.hoverRegenTimer = hoverRegenTimer;
        state.hoverRegenMultiplier = hoverRegenMultiplier;
        state.grappleTimer = grappleTimer;
        state.grappleUses = grappleUses;
        state.isGrappling = isGrappling;
        state.grappleTargetX = grappleTargetX;
        state.grappleTargetY = grappleTargetY;
        state.enlargedByPlunge = enlargedByPlunge;
        state.limitedFlightFuel = limitedFlightFuel;
        state.penguinIceFxTimer = penguinIceFxTimer;
        state.penguinDashDamageTimer = penguinDashDamageTimer;
        state.hummingFrenzyTimer = hummingFrenzyTimer;
        state.phoenixAfterburnTimer = phoenixAfterburnTimer;
        state.phoenixRebornUsed = phoenixRebornUsed;
        state.phoenixRebornActive = phoenixRebornActive;
        return state;
    }

    void applyLanState(LanBirdState state) {
        if (state == null) return;
        BirdGame3.BirdType[] types = BirdGame3.BirdType.values();
        if (state.typeOrdinal >= 0 && state.typeOrdinal < types.length) {
            this.type = types[state.typeOrdinal];
        }
        this.name = "P" + (playerIndex + 1) + ": " + type.name;
        this.x = state.x;
        this.y = state.y;
        this.vx = state.vx;
        this.vy = state.vy;
        this.facingRight = state.facingRight;
        this.health = state.health;
        this.stunTime = state.stunTime;
        this.specialCooldown = state.specialCooldown;
        this.specialMaxCooldown = state.specialMaxCooldown;
        this.attackCooldown = state.attackCooldown;
        this.attackAnimationTimer = state.attackAnimationTimer;
        this.canDoubleJump = state.canDoubleJump;
        this.loungeActive = state.loungeActive;
        this.isCitySkin = state.isCitySkin;
        this.isNoirSkin = state.isNoirSkin;
        this.isFreemanSkin = state.isFreemanSkin;
        this.isClassicSkin = state.isClassicSkin;
        this.isNovaSkin = state.isNovaSkin;
        this.isDuneSkin = state.isDuneSkin;
        this.isMintSkin = state.isMintSkin;
        this.isCircuitSkin = state.isCircuitSkin;
        this.isPrismSkin = state.isPrismSkin;
        this.isAuroraSkin = state.isAuroraSkin;
        this.isBeaconSkin = state.isBeaconSkin;
        this.isSunflareSkin = state.isSunflareSkin;
        this.isGlacierSkin = state.isGlacierSkin;
        this.isTideSkin = state.isTideSkin;
        this.isEclipseSkin = state.isEclipseSkin;
        this.isUmbraSkin = state.isUmbraSkin;
        this.suppressSelectEffects = state.suppressSelectEffects;
        this.loungeX = state.loungeX;
        this.loungeY = state.loungeY;
        this.loungeHealth = state.loungeHealth;
        this.loungeDamageFlash = state.loungeDamageFlash;
        this.diveTimer = state.diveTimer;
        this.isZipping = state.isZipping;
        this.zipTargetX = state.zipTargetX;
        this.zipTargetY = state.zipTargetY;
        this.zipTimer = state.zipTimer;
        this.isGroundPounding = state.isGroundPounding;
        this.carrionSwarmTimer = state.carrionSwarmTimer;
        this.crowSwarmCooldown = state.crowSwarmCooldown;
        this.isFlying = state.isFlying;
        this.leanTimer = state.leanTimer;
        this.leanCooldown = state.leanCooldown;
        this.isHigh = state.isHigh;
        this.highTimer = state.highTimer;
        this.tauntCooldown = state.tauntCooldown;
        this.tauntTimer = state.tauntTimer;
        this.cooldownFlash = state.cooldownFlash;
        this.currentTaunt = state.currentTaunt;
        this.eagleDiveCountdown = state.eagleDiveCountdown;
        this.eagleDiveActive = state.eagleDiveActive;
        this.eagleAscentActive = state.eagleAscentActive;
        this.eagleAscentFrames = state.eagleAscentFrames;
        this.bladeStormFrames = state.bladeStormFrames;
        this.plungeTimer = state.plungeTimer;
        this.batHanging = state.batHanging;
        this.batEchoTimer = state.batEchoTimer;
        this.isBlocking = state.isBlocking;
        this.blockCooldown = state.blockCooldown;
        this.speedMultiplier = state.speedMultiplier;
        this.powerMultiplier = state.powerMultiplier;
        this.sizeMultiplier = state.sizeMultiplier;
        this.baseSpeedMultiplier = state.baseSpeedMultiplier;
        this.basePowerMultiplier = state.basePowerMultiplier;
        this.baseSizeMultiplier = state.baseSizeMultiplier;
        this.speedTimer = state.speedTimer;
        this.rageTimer = state.rageTimer;
        this.shrinkTimer = state.shrinkTimer;
        this.titanTimer = state.titanTimer;
        this.titanActive = state.titanActive;
        this.neonRushTimer = state.neonRushTimer;
        this.thermalTimer = state.thermalTimer;
        this.thermalLift = state.thermalLift;
        this.overchargeAttackTimer = state.overchargeAttackTimer;
        this.speedBoostTimer = state.speedBoostTimer;
        this.hoverRegenTimer = state.hoverRegenTimer;
        this.hoverRegenMultiplier = state.hoverRegenMultiplier;
        this.grappleTimer = state.grappleTimer;
        this.grappleUses = state.grappleUses;
        this.isGrappling = state.isGrappling;
        this.grappleTargetX = state.grappleTargetX;
        this.grappleTargetY = state.grappleTargetY;
        this.enlargedByPlunge = state.enlargedByPlunge;
        this.limitedFlightFuel = state.limitedFlightFuel;
        this.penguinIceFxTimer = state.penguinIceFxTimer;
        this.penguinDashDamageTimer = state.penguinDashDamageTimer;
        this.hummingFrenzyTimer = state.hummingFrenzyTimer;
        this.phoenixAfterburnTimer = state.phoenixAfterburnTimer;
        this.phoenixRebornUsed = state.phoenixRebornUsed;
        this.phoenixRebornActive = state.phoenixRebornActive;
    }

    public void draw(GraphicsContext g) {
        double drawSize = 80 * sizeMultiplier;
        boolean airborne = !isOnGround();

        drawBlockingShield(g, drawSize, airborne);
        drawTaunt(g);
        drawCooldownFlash(g);
        drawRageBuff(g, drawSize);
        drawThermalBuff(g, drawSize);
        drawPenguinIceBuff(g, drawSize);
        drawHummingbirdFrenzy(g, drawSize);
        if (!suppressSelectEffects) {
            drawPhoenixAura(g, drawSize);
        }
        drawNeonBuff(g, drawSize);
        drawBatEcho(g, drawSize);
        if (!suppressSelectEffects) {
            drawOpiumBirdEffects(g, drawSize);
        }
        if (!suppressSelectEffects) {
            drawTitmouseSpecial(g);
        }
        if (!suppressSelectEffects) {
            drawEagleSoaring(g, airborne, drawSize);
            drawEagleDive(g, drawSize);
        }
        drawRazorbillBladestorm(g, drawSize);
        drawEagleSkin(g, drawSize);
        drawGrinchhawk(g);
        drawVulture(g, drawSize);
        drawStunEffect(g);
        drawSpecialCooldown(g);
        drawLounge(g);
        drawBodyAndEyes(g, drawSize);
        drawHeisenbirdAccessories(g, drawSize);
        drawCitySkin(g);
        drawNoirSkin(g);
        drawFreemanSkin(g);
        drawBeaconSkin(g, drawSize);
        drawClassicSkinAccent(g, drawSize);
        drawSpecialSkinAccent(g, drawSize);
        drawBeak(g, drawSize);
        drawPelican(g, drawSize);
        drawVineGrapple(g, drawSize);
    }

    private void drawHummingbirdFrenzy(GraphicsContext g, double drawSize) {
        if (type != BirdGame3.BirdType.HUMMINGBIRD || hummingFrenzyTimer <= 0) return;
        double pulse = 0.5 + 0.5 * Math.sin((105 - hummingFrenzyTimer) * 0.45);
        g.setFill(Color.CYAN.deriveColor(0, 1, 1, 0.24 + 0.18 * pulse));
        g.fillOval(x - 22, y - 22, drawSize + 44, drawSize + 44);

        g.setStroke(Color.YELLOW.deriveColor(0, 1, 1, 0.78));
        g.setLineWidth(2.6);
        for (int i = 0; i < 3; i++) {
            double r = 54 + i * 17 + pulse * 9;
            g.strokeOval(x + 40 - r, y + 40 - r, r * 2, r * 2);
        }
    }

    private void drawPhoenixAura(GraphicsContext g, double drawSize) {
        if (type != BirdGame3.BirdType.PHOENIX) return;

        double s = sizeMultiplier;
        double centerX = x + drawSize / 2.0;
        double centerY = y + drawSize / 2.0;
        double pulse = 0.5 + 0.5 * Math.sin(System.currentTimeMillis() / 120.0);
        if (isNovaSkin) {
            Color core = Color.web("#1A237E");
            Color rim = Color.web("#00E5FF");
            Color ring = Color.web("#E040FB");
            g.setFill(core.deriveColor(0, 1, 1, 0.2 + pulse * 0.15));
            g.fillOval(x - 30 * s, y - 30 * s, drawSize + 60 * s, drawSize + 60 * s);

            g.setStroke(rim.deriveColor(0, 1, 1, 0.85));
            g.setLineWidth(3.0);
            double r1 = (58 + pulse * 14) * s;
            g.strokeOval(centerX - r1, centerY - r1, r1 * 2, r1 * 2);

            g.setStroke(ring.deriveColor(0, 1, 1, 0.72));
            g.setLineWidth(2.2);
            double r2 = (76 + pulse * 18) * s;
            g.strokeOval(centerX - r2, centerY - r2, r2 * 2, r2 * 2);

            if (phoenixAfterburnTimer > 0) {
                double burstIntensity = Math.min(1.0, phoenixAfterburnTimer / 48.0);
                g.setStroke(rim.brighter().deriveColor(0, 1, 1, 0.9));
                g.setLineWidth(3.2);
                double t = System.currentTimeMillis() / 180.0;
                for (int i = 0; i < 8; i++) {
                    double angle = (Math.PI * 2 * i / 8.0) + t * 0.6;
                    double len = (70 + burstIntensity * 35) * s;
                    g.strokeLine(centerX, centerY, centerX + Math.cos(angle) * len, centerY + Math.sin(angle) * len);
                }
            }

            if (Math.random() < 0.35) {
                double px = centerX + (Math.random() - 0.5) * 60 * s;
                double py = centerY + (Math.random() - 0.5) * 50 * s;
                Color c = Math.random() < 0.5 ? rim : ring;
                game.particles.add(new Particle(px, py, (Math.random() - 0.5) * 2.2, -1.5 - Math.random() * 2.4, c));
            }
            return;
        }
        g.setFill(Color.ORANGERED.deriveColor(0, 1, 1, 0.22 + pulse * 0.12));
        g.fillOval(x - 24 * s, y - 24 * s, drawSize + 48 * s, drawSize + 48 * s);

        g.setStroke(Color.GOLD.deriveColor(0, 1, 1, 0.85));
        g.setLineWidth(2.4);
        double r = (52 + pulse * 10) * s;
        g.strokeOval(centerX - r, centerY - r, r * 2, r * 2);
        if (phoenixAfterburnTimer > 0) {
            double burstIntensity = Math.min(1.0, phoenixAfterburnTimer / 48.0);
            drawPhoenixFlameBurst(g, burstIntensity, pulse, centerX, centerY, s);
            g.setStroke(Color.ORANGERED.brighter().deriveColor(0, 1, 1, 0.82));
            g.setLineWidth(3.2);
            double ring = (60 + pulse * 14) * s;
            g.strokeOval(centerX - ring, centerY - ring, ring * 2, ring * 2);
        }

        if (Math.random() < 0.28) {
            double px = centerX + (Math.random() - 0.5) * 50 * s;
            double py = centerY + 18 * s + (Math.random() - 0.5) * 35 * s;
            Color c = Math.random() < 0.6 ? Color.ORANGE : Color.GOLD;
            game.particles.add(new Particle(px, py, (Math.random() - 0.5) * 1.8, -1.2 - Math.random() * 2.2, c));
        }
    }

    private void drawPhoenixFlameBurst(GraphicsContext g, double burstIntensity, double pulse, double centerX, double centerY, double s) {
        double t = System.currentTimeMillis() / 120.0;
        int flames = 12;
        for (int i = 0; i < flames; i++) {
            double angle = (Math.PI * 2 * i / flames) + t * 0.22;
            double innerRadius = (26 + pulse * 6) * s;
            double outerRadius = (58 + burstIntensity * 36 + Math.sin(t + i * 0.8) * 8) * s;
            double sideRadius = (10 + burstIntensity * 7) * s;

            double baseX = centerX + Math.cos(angle) * innerRadius;
            double baseY = centerY + Math.sin(angle) * innerRadius;
            double tipX = centerX + Math.cos(angle) * outerRadius;
            double tipY = centerY + Math.sin(angle) * outerRadius;
            double nx = -Math.sin(angle);
            double ny = Math.cos(angle);

            g.setFill(Color.ORANGERED.deriveColor(0, 1, 1, 0.76));
            g.fillPolygon(
                    new double[]{baseX + nx * sideRadius, baseX - nx * sideRadius, tipX},
                    new double[]{baseY + ny * sideRadius, baseY - ny * sideRadius, tipY},
                    3
            );

            double coreTip = outerRadius - (9 + burstIntensity * 7) * s;
            double coreSide = sideRadius * 0.55;
            g.setFill(Color.GOLD.deriveColor(0, 1, 1, 0.84));
            g.fillPolygon(
                    new double[]{baseX + nx * coreSide, baseX - nx * coreSide, centerX + Math.cos(angle) * coreTip},
                    new double[]{baseY + ny * coreSide, baseY - ny * coreSide, centerY + Math.sin(angle) * coreTip},
                    3
            );
        }
    }

    private void drawBatEcho(GraphicsContext g, double drawSize) {
        if (type != BirdGame3.BirdType.BAT || batEchoTimer <= 0) return;
        double pulse = 0.5 + 0.5 * Math.sin((150 - batEchoTimer) * 0.33);
        g.setStroke(Color.CYAN.deriveColor(0, 1, 1, 0.6));
        g.setLineWidth(4);
        for (int i = 0; i < 3; i++) {
            double r = 70 + i * 48 + pulse * 18;
            g.strokeOval(x + 40 - r, y + 40 - r, r * 2, r * 2);
        }
        g.setFill(Color.MEDIUMPURPLE.deriveColor(0, 1, 1, 0.4));
        g.fillOval(x - 20, y - 20, drawSize + 40, drawSize + 40);
    }

    private void drawBlockingShield(GraphicsContext g, double drawSize, boolean airborne) {
        if (isBlocking) {
            double birdCenterX = x + 40 * sizeMultiplier;
            double birdCenterY = y + 40 * sizeMultiplier;
            double pulse = 0.6 + 0.4 * Math.sin(System.currentTimeMillis() / 200.0);
            g.setFill(Color.BLUE.deriveColor(0, 1, 1, pulse));
            g.fillOval(x - 20 * sizeMultiplier, y - 20 * sizeMultiplier, drawSize + 40 * sizeMultiplier, drawSize + 40 * sizeMultiplier);
            g.setStroke(Color.BLUE.brighter());
            g.setLineWidth(6 * sizeMultiplier);
            double shieldAngleStart = facingRight ? 0 : Math.PI;
            double shieldRadius = drawSize * 0.8;
            g.strokeArc(birdCenterX - shieldRadius, birdCenterY - shieldRadius, shieldRadius * 2, shieldRadius * 2,
                    Math.toDegrees(shieldAngleStart) - 90, 180, ArcType.OPEN);
            if (Math.random() < 0.5) {
                double particleAngle = shieldAngleStart + (Math.random() - 0.5) * Math.PI;
                double px = birdCenterX + Math.cos(particleAngle) * shieldRadius;
                double py = birdCenterY + Math.sin(particleAngle) * shieldRadius;
                game.particles.add(new Particle(px, py, (Math.random() - 0.5) * 4, (Math.random() - 0.5) * 4 - 2, Color.BLUE.brighter()));
            }
        }
    }

    private void drawTaunt(GraphicsContext g) {
        if (tauntTimer > 0) {
            double tauntCenterX = x + 40;
            switch (currentTaunt) {
                case 1 -> {
                    double barX = facingRight ? x + 80 : (2 * tauntCenterX - (x + 80) - 60);
                    double wingOvalX = facingRight ? x + 130 : (2 * tauntCenterX - (x + 130) - 30);
                    double tauntTextX = facingRight ? x + 100 : (2 * tauntCenterX - (x + 100));
                    double wingY = y + 20;
                    g.setFill(Color.BLACK);
                    g.fillRect(barX, wingY, 60, 15);
                    g.fillOval(wingOvalX, wingY - 20, 30, 50);
                    g.setFill(Color.WHITE);
                    g.setFont(Font.font("Arial Black", 24));
                    g.fillText("FRICK YOU!", tauntTextX, wingY - 30);
                }
                case 2 -> {
                    g.setFill(Color.RED.deriveColor(0, 1, 1, 0.6));
                    g.fillOval(x - 40, y - 60, 160, 100);
                    g.setFill(Color.WHITE);
                    g.setFont(Font.font("Arial Black", 32));
                    g.fillText("COME AT ME", x - 10, y - 10);
                }
                case 3 -> {
                    double kissFaceX = facingRight ? x + 10 : (2 * tauntCenterX - (x + 10) - 60);
                    double eyeLeftX = facingRight ? x + 25 : (2 * tauntCenterX - (x + 25) - 15);
                    double eyeRightX = facingRight ? x + 45 : (2 * tauntCenterX - (x + 45) - 15);
                    double pupilX = facingRight ? x + 32 : (2 * tauntCenterX - (x + 32) - 8);
                    double kissTextX = facingRight ? x + 15 : (2 * tauntCenterX - (x + 15));
                    g.setFill(Color.PINK.brighter());
                    g.fillOval(kissFaceX, y + 50, 60, 70);
                    g.setFill(Color.WHITE);
                    g.fillOval(eyeLeftX, y + 65, 15, 20);
                    g.fillOval(eyeRightX, y + 65, 15, 20);
                    g.setFill(Color.BLACK);
                    g.fillOval(pupilX, y + 75, 8, 8);
                    g.setFont(Font.font("Arial Black", 20));
                    g.fillText("KISS IT", kissTextX, y + 120);
                }
            }
            g.setFill(Color.WHITE.deriveColor(0, 1, 1, 0.3 + 0.4 * Math.sin(tauntTimer * 0.5)));
            g.fillOval(x - 30, y - 40, 140, 140);
        }
    }

    private void drawCooldownFlash(GraphicsContext g) {
        if (this.cooldownFlash > 0) {
            g.setFill(Color.RED.deriveColor(0, 1, 1, 0.6));
            g.setFont(Font.font("Arial Black", 32));
            g.fillText("COOLDOWN!", x - 20, y - 60);
            cooldownFlash--;
        }
    }

    private void drawRageBuff(GraphicsContext g, double drawSize) {
        if (rageTimer > 0) {
            g.setFill(Color.RED.deriveColor(0, 1, 1, 0.4));
            g.fillOval(x - 20, y - 20, drawSize + 40, drawSize + 40);
        }
    }

    private void drawThermalBuff(GraphicsContext g, double drawSize) {
        if (thermalTimer > 0) {
            double pulse = 0.5 + 0.5 * Math.sin(System.currentTimeMillis() / 120.0);
            g.setFill(Color.GOLD.deriveColor(0, 1, 1, 0.4 + 0.3 * pulse));
            g.fillOval(x - 60, y - 60, drawSize + 120, drawSize + 120);
            if (Math.random() < 0.4) {
                game.particles.add(new Particle(x + 40 + (Math.random() - 0.5) * 60,
                        y + 80,
                        (Math.random() - 0.5) * 4,
                        -6 - Math.random() * 8,
                        Color.YELLOW.deriveColor(0, 1, 1, 0.8)));
            }
        }
    }

    private void drawNeonBuff(GraphicsContext g, double drawSize) {
        if (rageTimer > 0 && speedMultiplier > 2.0) {
            double pulse = 0.5 + 0.5 * Math.sin(System.currentTimeMillis() / 100.0);
            g.setFill(Color.MAGENTA.deriveColor(0, 1, 1 + pulse, 0.6));
            g.fillOval(x - 50, y - 50, drawSize + 100, drawSize + 100);

            g.setStroke(Color.CYAN.brighter());
            g.setLineWidth(4 + pulse * 4);
            for (int i = 1; i <= 8; i++) {
                g.strokeLine(x + 40, y + 40,
                        x + 40 - vx * i * 2, y + 40 - vy * i * 2);
            }
        }
    }

    private void drawOpiumBirdEffects(GraphicsContext g, double drawSize) {
        boolean opium = type == BirdGame3.BirdType.OPIUMBIRD;
        boolean heisen = type == BirdGame3.BirdType.HEISENBIRD;
        if (!opium && !heisen) return;

        if (opium) {
            g.setFill(Color.rgb(138, 43, 226, 0.3));
            g.fillOval(x - 30, y - 40, drawSize + 60, drawSize + 80);

            g.setFill(Color.PURPLE.darker());
            double dripBaseX = facingRight ? x + 85 : x - 21;
            for (int i = 0; i < 5; i++) {
                double offset = Math.sin((System.currentTimeMillis() / 100.0) + i) * 4;
                double facingOffset = facingRight ? offset : -offset;
                g.fillOval(dripBaseX + facingOffset, y + 50 + i * 12, 16, 24);
            }

            if (highTimer > 0) {
                double intensity = highTimer / 180.0;
                g.setFill(Color.MAGENTA.deriveColor(0, 1, 1, 0.3 * intensity));
                g.fillOval(x - 100, y - 100, drawSize + 200, drawSize + 200);
                g.setFill(Color.rgb(200, 0, 255));
                g.fillOval(x + Math.sin(highTimer * 0.3) * 20, y + Math.cos(highTimer * 0.2) * 15, drawSize, drawSize);
            }

            if (leanTimer > 0) {
                double cloudAlpha = 0.3 + 0.3 * Math.sin(System.currentTimeMillis() / 200.0);
                g.setFill(Color.rgb(138, 43, 226, cloudAlpha));
                g.fillOval(x - 120, y - 100, 300, 300);
            }

            if (leanCooldown > 0) {
                g.setFill(Color.PURPLE.darker());
                g.fillRoundRect(x - 10, y + 100, 100, 20, 15, 15);
            }
        } else {
            g.setFill(Color.web("#0D47A1", 0.25));
            g.fillOval(x - 30, y - 40, drawSize + 60, drawSize + 80);

            g.setFill(Color.web("#1E88E5"));
            double crystalBaseX = facingRight ? x + 85 : x - 21;
            for (int i = 0; i < 4; i++) {
                double offset = Math.sin((System.currentTimeMillis() / 110.0) + i) * 3;
                double facingOffset = facingRight ? offset : -offset;
                double cx = crystalBaseX + facingOffset;
                double cy = y + 50 + i * 14;
                double w = 14;
                double h = 18;
                g.fillPolygon(
                        new double[]{cx, cx + w / 2.0, cx + w, cx + w / 2.0},
                        new double[]{cy + h / 2.0, cy, cy + h / 2.0, cy + h},
                        4
                );
            }
            g.setFill(Color.web("#81D4FA"));
            for (int i = 0; i < 3; i++) {
                double cx = (facingRight ? x + 66 : x + 6) + i * 8;
                double cy = y + 46 + i * 10;
                g.fillPolygon(
                        new double[]{cx, cx + 6, cx + 12, cx + 6},
                        new double[]{cy + 6, cy, cy + 6, cy + 12},
                        4
                );
            }

            if (highTimer > 0) {
                double intensity = highTimer / 140.0;
                g.setFill(Color.web("#29B6F6", 0.25 * intensity));
                g.fillOval(x - 100, y - 100, drawSize + 200, drawSize + 200);
                g.setFill(Color.web("#4FC3F7"));
                g.fillOval(x + Math.sin(highTimer * 0.3) * 18, y + Math.cos(highTimer * 0.2) * 14, drawSize, drawSize);
            }

            if (leanTimer > 0) {
                double cloudAlpha = 0.28 + 0.28 * Math.sin(System.currentTimeMillis() / 200.0);
                g.setFill(Color.web("#29B6F6", cloudAlpha));
                g.fillOval(x - 110, y - 90, 280, 280);
            }

            if (leanCooldown > 0) {
                g.setFill(Color.web("#0D47A1"));
                g.fillRoundRect(x - 10, y + 100, 100, 20, 15, 15);
            }
        }
    }

    private void drawTitmouseSpecial(GraphicsContext g) {
        if (type == BirdGame3.BirdType.TITMOUSE) {
            double s = sizeMultiplier;
            g.setFill(Color.SILVER);
            g.fillOval(x + 20 * s, y - 20 * s, 40 * s, 60 * s);

            g.setFill(Color.BLACK);
            g.fillOval(x + 25 * s, y + 15 * s, 25 * s, 25 * s);
            g.fillOval(x + 45 * s, y + 15 * s, 25 * s, 25 * s);
            g.setFill(Color.WHITE);
            g.fillOval(x + 32 * s, y + 20 * s, 10 * s, 10 * s);
            g.fillOval(x + 52 * s, y + 20 * s, 10 * s, 10 * s);

            if (isZipping) {
                g.setStroke(Color.SKYBLUE.brighter());
                g.setLineWidth(8);
                g.strokeLine(x + 40 * s, y + 40 * s, zipTargetX + 40 * s, zipTargetY + 40 * s);
                g.setLineWidth(4);
                g.setStroke(Color.WHITE);
                g.strokeLine(x + 40 * s, y + 40 * s, zipTargetX + 40 * s, zipTargetY + 40 * s);
            }
        }
    }

    private void drawEagleSoaring(GraphicsContext g, boolean airborne, double drawSize) {
        boolean eagle = type == BirdGame3.BirdType.EAGLE;
        boolean skyKing = eagle && isClassicSkin;
        boolean falcon = type == BirdGame3.BirdType.FALCON;
        boolean duneFalcon = falcon && isDuneSkin;
        if ((skyKing || duneFalcon) && (diveTimer == 0) && airborne && (vy < 2)) {
            Color aura = skyKing ? Color.GOLD : Color.web("#FFCC80");
            g.setFill(aura.deriveColor(0, 1, 1, 0.2));
            g.fillOval(x - 50, y - 50, drawSize + 100, drawSize + 100);

            if (Math.random() < 0.2) {
                game.particles.add(new Particle(x + (facingRight ? -20 : drawSize + 20), y + 40,
                        (facingRight ? 1 : -1) * (2 + Math.random() * 4),
                        (Math.random() - 0.5) * 4,
                        (skyKing ? Color.GOLD : Color.web("#FFB74D")).brighter()));
            }
        }
    }

    private void drawEagleDive(GraphicsContext g, double drawSize) {
        boolean eagle = type == BirdGame3.BirdType.EAGLE;
        boolean skyKing = eagle && isClassicSkin;
        boolean falcon = type == BirdGame3.BirdType.FALCON;
        boolean duneFalcon = falcon && isDuneSkin;
        if ((eagle || falcon) && diveTimer > 0) {
            Color aura = eagle ? Color.web("#D32F2F") : Color.SADDLEBROWN;
            double pulse = 0.55 + 0.45 * Math.sin(diveTimer * 0.35);
            double auraSize = drawSize + 170 + pulse * 30;
            double auraOffset = (auraSize - drawSize) / 2.0;
            g.setFill(aura.deriveColor(0, 1, 1, 0.35 + 0.2 * pulse));
            g.fillOval(x - auraOffset, y - auraOffset, auraSize, auraSize);

            g.setStroke(aura.brighter().deriveColor(0, 1, 1, 0.85));
            g.setLineWidth(6);
            g.strokeOval(x - auraOffset - 6, y - auraOffset - 6, auraSize + 12, auraSize + 12);

            g.setStroke(Color.WHITE.deriveColor(0, 1, 1, 0.35 + 0.25 * pulse));
            g.setLineWidth(2.5);
            g.strokeOval(x - 60, y - 60, drawSize + 120, drawSize + 120);

            if (Math.random() < 0.3) {
                double angle = Math.random() * Math.PI * 2;
                double dist = 40 + Math.random() * 45;
                double px = x + 40 + Math.cos(angle) * dist;
                double py = y + 40 + Math.sin(angle) * dist;
                double spd = 3 + Math.random() * 5;
                game.particles.add(new Particle(
                        px,
                        py,
                        Math.cos(angle) * spd - vx * 0.08,
                        Math.sin(angle) * spd - vy * 0.08,
                        aura.brighter().deriveColor(0, 1, 1, 0.9)
                ));
            }
        }
        if ((skyKing || duneFalcon) && diveTimer > 0) {
            Color core = skyKing ? Color.CRIMSON : Color.web("#FF7043");
            Color streakPrimary = skyKing ? Color.ORANGERED : Color.web("#FF8A65");
            Color streakSecondary = skyKing ? Color.YELLOW : Color.web("#FFE082");
            String diveText = skyKing ? "SKREEEEEEEE!!!" : "LOCKED IN!";

            g.setFill(core.deriveColor(0, 1, 1, 0.6 + 0.4 * Math.sin(diveTimer * 0.5)));
            g.fillOval(x - 80, y - 80, drawSize + 160, drawSize + 160);

            g.setStroke(streakPrimary);
            g.setLineWidth(8);
            for (int i = 1; i <= 12; i++) {
                g.strokeLine(x + 40, y + 40, x + 40 - vx * i * 3, y + 40 - vy * i * 3);
            }
            g.setLineWidth(3);
            g.setStroke(streakSecondary);
            for (int i = 1; i <= 8; i++) {
                g.strokeLine(x + 40, y + 40, x + 40 - vx * i * 2.5, y + 40 - vy * i * 2.5);
            }

            if (diveTimer > (skyKing ? 70 : 55)) {
                g.setFill(Color.WHITE);
                g.setFont(Font.font("Arial Black", FontWeight.BOLD, 64));
                g.setEffect(new DropShadow(20, Color.BLACK));
                g.fillText(diveText, x - 180, y - 60);
                g.setEffect(null);
            }

            g.setStroke(streakSecondary);
            g.setLineWidth(4);
            for (int i = 0; i < 6; i++) {
                double angle = Math.random() * Math.PI * 2;
                double len = 60 + Math.random() * 40;
                g.strokeLine(x + 40, y + 40, x + 40 + Math.cos(angle) * len, y + 40 + Math.sin(angle) * len);
            }
        }
    }

    private void drawRazorbillBladestorm(GraphicsContext g, double drawSize) {
        if ((type == BirdGame3.BirdType.RAZORBILL) && (bladeStormFrames > 0)) {
            double dirX = razorbillDashVX;
            double dirY = razorbillDashVY;
            double mag = Math.hypot(dirX, dirY);
            if (mag < 0.1) {
                dirX = facingRight ? 1 : -1;
                dirY = 0;
                mag = 1.0;
            }
            dirX /= mag;
            dirY /= mag;

            g.setStroke(Color.CYAN.brighter());
            g.setLineWidth(6);
            for (int i = 0; i < 7; i++) {
                double offset = i * 18;
                double jitter = (Math.random() - 0.5) * 10;
                double px = x + 40 - dirX * offset - dirY * jitter;
                double py = y + 40 - dirY * offset + dirX * jitter;
                g.strokeLine(px, py, px - dirX * 26, py - dirY * 26);
            }

            double pulse = 0.45 + 0.25 * Math.sin(bladeStormFrames * 0.6);
            g.setFill(Color.WHITE.deriveColor(0, 1, 1, pulse));
            g.fillOval(x - 35, y - 35, drawSize + 70, drawSize + 70);

            if (bladeStormFrames % 12 < 4) {
                g.setFill(Color.CYAN.brighter());
                g.setFont(Font.font("Arial Black", FontWeight.BOLD, 44));
                g.setEffect(new Glow(1.0));
                g.fillText("PIERCE!", x - 70, y - 56);
                g.setEffect(null);
            }
        }
    }

    private void drawEagleSkin(GraphicsContext g, double drawSize) {
        if (type == BirdGame3.BirdType.EAGLE && isClassicSkin) {

            if (!suppressSelectEffects) {
                g.setFill(Color.GOLD.deriveColor(0, 1, 1, 0.5));
                g.fillOval(x - 40, y - 40, drawSize + 80, drawSize + 80);
            }

            double crownScale = suppressSelectEffects ? 0.8 : 1.0;
            double crownW = 50 * crownScale;
            double crownH = 70 * crownScale;
            double crownX = x + 15 + (50 - crownW) * 0.5;
            double crownY = y - 35 + (70 - crownH) * 0.5;
            g.setFill(Color.GOLD.brighter());
            g.fillOval(crownX, crownY, crownW, crownH);
            g.setFill(Color.ORANGE.brighter());
            double gemW = 30 * crownScale;
            double gemH = 40 * crownScale;
            double gemX = x + 25 + (30 - gemW) * 0.5;
            double gemY = y - 45 + (40 - gemH) * 0.5;
            g.fillOval(gemX, gemY, gemW, gemH);

            if (!suppressSelectEffects && Math.random() < 0.4) {
                game.particles.add(new Particle(x + 40 + (Math.random() - 0.5) * 100, y + 40 + (Math.random() - 0.5) * 100,
                        (Math.random() - 0.5) * 5, (Math.random() - 0.5) * 5 - 3, Color.GOLD.brighter()));
            }
        }
    }

    private void drawGrinchhawk(GraphicsContext g) {
        if (type == BirdGame3.BirdType.GRINCHHAWK) {
            g.setFill(Color.YELLOW);
            g.fillOval(x + (facingRight ? 55 : 20) * sizeMultiplier, y + 22 * sizeMultiplier, 18 * sizeMultiplier, 18 * sizeMultiplier);
            g.setFill(Color.BLACK);
            g.fillOval(x + (facingRight ? 60 : 25) * sizeMultiplier, y + 25 * sizeMultiplier, 10 * sizeMultiplier, 10 * sizeMultiplier);
        }
    }

    private void drawVulture(GraphicsContext g, double drawSize) {
        if (type == BirdGame3.BirdType.VULTURE) {
            double s = sizeMultiplier;
            g.setFill(Color.rgb(35, 15, 45));
            g.fillOval(x, y, drawSize, drawSize);

            double wingSpread = isFlying || Math.abs(vx) > 2 ? 1.4 : 1.0;
            g.setFill(Color.rgb(20, 10, 30));
            g.fillOval(x - 30 * wingSpread * s, y + 10 * s, 50 * wingSpread * s, 90 * s);
            g.fillOval(x + drawSize - 20 * wingSpread * s, y + 10 * s, 50 * wingSpread * s, 90 * s);

            g.setFill(Color.rgb(180, 30, 30));
            g.fillOval(x + 15 * s, y + 10 * s, 50 * s, 55 * s);

            g.setFill(Color.CRIMSON.darker().darker());
            g.fillOval(x + 25 * s, y + 25 * s, 20 * s, 20 * s);
            g.fillOval(x + 45 * s, y + 25 * s, 20 * s, 20 * s);
            g.setFill(Color.RED.brighter());
            g.fillOval(x + 30 * s, y + 30 * s, 10 * s, 10 * s);
            g.fillOval(x + 50 * s, y + 30 * s, 10 * s, 10 * s);

            if (carrionSwarmTimer > 0) {
                g.setFill(Color.BLACK.deriveColor(0, 1, 1, 0.6));
                g.fillOval(x - 40 * s, y - 30 * s, drawSize + 80 * s, drawSize + 100 * s);
                carrionSwarmTimer--;
            }
        }
    }

    private void drawStunEffect(GraphicsContext g) {
        if (stunTime > 0) {
            g.setFill(Color.CYAN.deriveColor(0, 1, 1, 0.7));
            g.setFont(Font.font(28));
            g.fillText("FROZEN!", x + 5, y - 25);
        }
    }

    private void drawSpecialCooldown(GraphicsContext g) {
        if (specialCooldown > 0 && specialMaxCooldown > 0) {
            double ratio = (double) specialCooldown / specialMaxCooldown;

            double drawSize = 80 * sizeMultiplier;
            double barScale = Math.max(0.85, Math.min(sizeMultiplier, 1.25));
            double barWidth = 90 * barScale;
            double barHeight = 14 * barScale;
            double barX = x + (drawSize / 2.0) - (barWidth / 2.0);
            double barY = y + drawSize + (12 * barScale);
            double innerX = barX + (5 * barScale);
            double innerY = barY + (4 * barScale);
            double innerWidth = Math.max(0, (barWidth - (10 * barScale)) * (1 - ratio));
            double innerHeight = 6 * barScale;

            g.setFill(Color.BLACK.deriveColor(0, 1, 1, 0.8));
            g.fillRoundRect(barX, barY, barWidth, barHeight, 10 * barScale, 10 * barScale);

            Color fillColor = ratio > 0.66 ? Color.CRIMSON :
                    ratio > 0.33 ? Color.ORANGE : Color.CYAN.brighter();
            g.setFill(fillColor);
            g.fillRoundRect(innerX, innerY, innerWidth, innerHeight, 6 * barScale, 6 * barScale);

            g.setStroke(Color.WHITE);
            g.setLineWidth(2 * barScale);
            g.strokeRoundRect(barX, barY, barWidth, barHeight, 10 * barScale, 10 * barScale);

            g.setFill(Color.WHITE);
            g.setFont(Font.font("Arial Black", 16 * barScale));

            String cooldownText;
            if (type == BirdGame3.BirdType.VULTURE && crowSwarmCooldown > 0) {
                cooldownText = "CROWS";
            } else if (type == BirdGame3.BirdType.OPIUMBIRD && leanCooldown > 0) {
                cooldownText = "LEAN";
            } else if (type == BirdGame3.BirdType.HEISENBIRD && leanCooldown > 0) {
                cooldownText = "CRYSTAL";
            } else if (type == BirdGame3.BirdType.MOCKINGBIRD && specialCooldown > 0) {
                cooldownText = "LOUNGE";
            } else if (type == BirdGame3.BirdType.PIGEON && specialCooldown > 0) {
                cooldownText = "HEAL";
            } else if (specialCooldown > 0) {
                cooldownText = (int) Math.ceil(specialCooldown / 60.0) + "s";
            } else {
                cooldownText = "";
            }
            g.fillText(cooldownText, barX + (25 * barScale), barY + (12 * barScale));
        }
    }

    private void drawLounge(GraphicsContext g) {
        if (type == BirdGame3.BirdType.MOCKINGBIRD && loungeActive && loungeHealth > 0) {
            Color loungeColor = loungeDamageFlash > 0 ? Color.RED : Color.LIME;
            g.setFill(loungeColor.deriveColor(0, 1, 1, 0.7));
            g.fillRoundRect(loungeX - 60, loungeY - 40, 120, 80, 30, 30);
            g.setStroke(loungeDamageFlash > 0 ? Color.ORANGERED : Color.DARKGREEN);
            g.setLineWidth(loungeDamageFlash > 0 ? 8 : 5);
            g.strokeRoundRect(loungeX - 60, loungeY - 40, 120, 80, 30, 30);

            g.setFill(Color.BLACK);
            g.fillRect(loungeX - 65, loungeY - 75, 130, 18);
            g.setFill(Color.RED.darker());
            g.fillRect(loungeX - 60, loungeY - 70, 120, 12);
            g.setFill(Color.LIME);
            g.fillRect(loungeX - 60, loungeY - 70, 120 * (loungeHealth / (double) LOUNGE_MAX_HEALTH), 12);

            g.setFill(Color.WHITE);
            g.setFont(Font.font("Arial Black", 20));
            g.fillText(loungeHealth + " HP", loungeX - 48, loungeY - 55);

            g.setFill(Color.BLACK);
            g.setFont(Font.font("Arial Black", 24));
            g.fillText("LOUNGE", loungeX - 52, loungeY + 8);

            if (loungeDamageFlash > 0) loungeDamageFlash--;
        }
    }

    private void drawBodyAndEyes(GraphicsContext g, double drawSize) {
        if (type == BirdGame3.BirdType.BAT) {
            drawBatBody(g);
            return;
        }
        if (type == BirdGame3.BirdType.PHOENIX) {
            drawPhoenixBody(g, drawSize);
            return;
        }

        boolean noirPigeon = (type == BirdGame3.BirdType.PIGEON && isNoirSkin);
        boolean beaconPigeon = (type == BirdGame3.BirdType.PIGEON && isBeaconSkin);
        boolean classicPalette = isClassicSkin && type != BirdGame3.BirdType.PIGEON;
        boolean duneFalcon = (type == BirdGame3.BirdType.FALCON && isDuneSkin);
        boolean mintPenguin = (type == BirdGame3.BirdType.PENGUIN && isMintSkin);
        boolean circuitTitmouse = (type == BirdGame3.BirdType.TITMOUSE && isCircuitSkin);
        boolean prismRazorbill = (type == BirdGame3.BirdType.RAZORBILL && isPrismSkin);
        boolean auroraPelican = (type == BirdGame3.BirdType.PELICAN && isAuroraSkin);
        boolean sunflareHummingbird = (type == BirdGame3.BirdType.HUMMINGBIRD && isSunflareSkin);
        boolean glacierShoebill = (type == BirdGame3.BirdType.SHOEBILL && isGlacierSkin);
        boolean tideVulture = (type == BirdGame3.BirdType.VULTURE && isTideSkin);
        boolean eclipseMockingbird = (type == BirdGame3.BirdType.MOCKINGBIRD && isEclipseSkin);
        boolean freemanPigeon = (type == BirdGame3.BirdType.PIGEON && isFreemanSkin);
        boolean ravenEyes = (type == BirdGame3.BirdType.RAVEN);
        Color bodyColor;
        Color headColor;
        Color eyeOverride = null;
        if (beaconPigeon) {
            bodyColor = Color.web("#FFE082");
            headColor = Color.web("#FFF8E1");
            eyeOverride = Color.web("#1E88E5");
        } else if (noirPigeon) {
            bodyColor = Color.rgb(18, 18, 18);
            headColor = Color.rgb(42, 42, 42);
        } else if (sunflareHummingbird) {
            bodyColor = Color.web("#FFB74D");
            headColor = Color.web("#FFE082");
            eyeOverride = Color.web("#E65100");
        } else if (glacierShoebill) {
            bodyColor = Color.web("#90CAF9");
            headColor = Color.web("#BBDEFB");
            eyeOverride = Color.web("#01579B");
        } else if (tideVulture) {
            bodyColor = Color.web("#26A69A");
            headColor = Color.web("#80CBC4");
            eyeOverride = Color.web("#004D40");
        } else if (eclipseMockingbird) {
            bodyColor = Color.web("#311B92");
            headColor = Color.web("#4A148C");
            eyeOverride = Color.web("#E040FB");
        } else if (freemanPigeon) {
            bodyColor = Color.web("#7B7B7B");
            headColor = Color.web("#9E9E9E");
            eyeOverride = Color.web("#6D4C41");
        } else if (duneFalcon) {
            bodyColor = Color.web("#D7B98E");
            headColor = Color.web("#E7CFAE");
            eyeOverride = Color.web("#4E342E");
        } else if (mintPenguin) {
            bodyColor = Color.web("#7FD6D8");
            headColor = Color.web("#A6ECEB");
            eyeOverride = Color.web("#004D40");
        } else if (circuitTitmouse) {
            bodyColor = Color.web("#455A64");
            headColor = Color.web("#607D8B");
            eyeOverride = Color.web("#00E5FF");
        } else if (prismRazorbill) {
            bodyColor = Color.web("#1A237E");
            headColor = Color.web("#3949AB");
            eyeOverride = Color.web("#40C4FF");
        } else if (auroraPelican) {
            bodyColor = Color.web("#B2DFDB");
            headColor = Color.web("#E0F2F1");
            eyeOverride = Color.web("#00695C");
        } else if (classicPalette) {
            bodyColor = game.classicSkinPrimaryColor(type);
            headColor = game.classicSkinPrimaryColor(type).brighter();
        } else {
            bodyColor = type.color;
            headColor = type.color.brighter();
        }

        g.setFill(bodyColor);
        g.fillOval(x, y, drawSize, drawSize);
        g.setFill(headColor);
        g.fillOval(facingRight ? x + 50 * sizeMultiplier : x - 20 * sizeMultiplier, y + 20 * sizeMultiplier, 50 * sizeMultiplier, 40 * sizeMultiplier);
        if (type == BirdGame3.BirdType.RAZORBILL) {
            double s = sizeMultiplier;
            double headX = facingRight ? x + 50 * s : x - 20 * s;
            double crestBaseX = facingRight ? headX + 10 * s : headX + 30 * s;
            Color crest = classicPalette ? game.classicSkinAccentColor(type) : (prismRazorbill ? Color.web("#FFD740") : Color.CYAN.brighter());
            g.setFill(crest);
            g.fillPolygon(
                    new double[]{crestBaseX, crestBaseX + 6 * s, crestBaseX + 12 * s},
                    new double[]{y + 20 * s, y + 4 * s, y + 20 * s},
                    3
            );
            g.fillPolygon(
                    new double[]{crestBaseX + 10 * s, crestBaseX + 16 * s, crestBaseX + 22 * s},
                    new double[]{y + 22 * s, y + 6 * s, y + 22 * s},
                    3
            );
            g.fillPolygon(
                    new double[]{crestBaseX + 20 * s, crestBaseX + 26 * s, crestBaseX + 32 * s},
                    new double[]{y + 20 * s, y + 4 * s, y + 20 * s},
                    3
            );
        }
        // Titmouse head details are handled by drawTitmouseSpecial when effects are enabled.
        if (ravenEyes) {
            double s = sizeMultiplier;
            double glowX = x + (facingRight ? 48 : 18) * s;
            double glowY = y + 18 * s;
            g.setFill(Color.web("#B71C1C").deriveColor(0, 1, 1, 0.35));
            g.fillOval(glowX, glowY, 29 * s, 29 * s);
        }
        g.setFill(Color.WHITE);
        g.fillOval(x + (facingRight ? 50 : 20) * sizeMultiplier, y + 20 * sizeMultiplier, 25 * sizeMultiplier, 25 * sizeMultiplier);
        Color eyeColor = classicPalette ? game.classicSkinAccentColor(type) : Color.BLACK;
        if (eyeOverride != null) eyeColor = eyeOverride;
        if (noirPigeon) eyeColor = Color.RED.brighter();
        if (ravenEyes) eyeColor = Color.web("#D50000");
        g.setFill(eyeColor);
        g.fillOval(x + (facingRight ? 55 : 25) * sizeMultiplier, y + 25 * sizeMultiplier, 15 * sizeMultiplier, 15 * sizeMultiplier);
    }

    private void drawHeisenbirdAccessories(GraphicsContext g, double drawSize) {
        if (type != BirdGame3.BirdType.HEISENBIRD) return;
        double s = sizeMultiplier;
        double headX = facingRight ? x + 50 * s : x - 20 * s;
        double headY = y + 20 * s;
        double headW = 50 * s;

        // Hat
        g.setFill(Color.rgb(20, 20, 20));
        g.fillRoundRect(headX - 6 * s, headY - 12 * s, headW + 12 * s, 10 * s, 6 * s, 6 * s);
        g.setFill(Color.rgb(35, 35, 35));
        g.fillRoundRect(headX + 8 * s, headY - 34 * s, headW - 16 * s, 22 * s, 6 * s, 6 * s);
        g.setFill(Color.rgb(90, 90, 90));
        g.fillRect(headX + 10 * s, headY - 24 * s, headW - 20 * s, 5 * s);

        // Goatee
        g.setFill(Color.rgb(45, 25, 15));
        double goateeW = 14 * s;
        double goateeH = 10 * s;
        double goateeX = facingRight ? x + 62 * s : x + 4 * s;
        double goateeY = y + 54 * s;
        g.fillPolygon(
                new double[]{goateeX, goateeX + goateeW, goateeX + goateeW / 2.0},
                new double[]{goateeY, goateeY, goateeY + goateeH},
                3
        );
    }

    private void drawClassicSkinAccent(GraphicsContext g, double drawSize) {
        if (!isClassicSkin || type == BirdGame3.BirdType.PIGEON || type == BirdGame3.BirdType.EAGLE) return;
        Color accent = game.classicSkinAccentColor(type);
        g.setStroke(accent.deriveColor(0, 1, 1, 0.9));
        g.setLineWidth(3.2 * sizeMultiplier);
        g.strokeOval(x - 10 * sizeMultiplier, y - 10 * sizeMultiplier, drawSize + 20 * sizeMultiplier, drawSize + 20 * sizeMultiplier);
        g.setFill(accent.deriveColor(0, 1, 1, 0.35));
        g.fillOval(x + 8 * sizeMultiplier, y + 10 * sizeMultiplier, drawSize * 0.72, drawSize * 0.45);
    }

    private void drawSpecialSkinAccent(GraphicsContext g, double drawSize) {
        double s = sizeMultiplier;
        if (type == BirdGame3.BirdType.FALCON && isDuneSkin) {
            g.setStroke(Color.web("#8D6E63").deriveColor(0, 1, 1, 0.7));
            g.setLineWidth(2.2 * s);
            g.strokeLine(x + 18 * s, y + 55 * s, x + 62 * s, y + 45 * s);
        }
        if (type == BirdGame3.BirdType.PENGUIN && isMintSkin) {
            g.setFill(Color.web("#E0F7FA").deriveColor(0, 1, 1, 0.55));
            g.fillOval(x + 16 * s, y + 40 * s, 48 * s, 32 * s);
        }
        if (type == BirdGame3.BirdType.TITMOUSE && isCircuitSkin) {
            g.setStroke(Color.web("#00E5FF").deriveColor(0, 1, 1, 0.85));
            g.setLineWidth(2.4 * s);
            g.strokeLine(x + 18 * s, y + 46 * s, x + 62 * s, y + 30 * s);
            g.strokeLine(x + 26 * s, y + 30 * s, x + 26 * s, y + 64 * s);
            g.setFill(Color.web("#FF4081"));
            g.fillOval(x + 58 * s, y + 26 * s, 6 * s, 6 * s);
        }
        if (type == BirdGame3.BirdType.RAZORBILL && isPrismSkin) {
            double startX = facingRight ? x + 10 * s : x + 70 * s;
            double endX = facingRight ? x + 60 * s : x + 20 * s;
            g.setStroke(Color.web("#E1BEE7").deriveColor(0, 1, 1, 0.7));
            g.setLineWidth(3.0 * s);
            g.strokeLine(startX, y + 60 * s, endX, y + 35 * s);
            g.setStroke(Color.web("#80D8FF").deriveColor(0, 1, 1, 0.8));
            g.setLineWidth(2.0 * s);
            g.strokeLine(startX, y + 66 * s, endX, y + 41 * s);
        }
        if (type == BirdGame3.BirdType.PELICAN && isAuroraSkin) {
            g.setFill(Color.web("#80DEEA").deriveColor(0, 1, 1, 0.35));
            g.fillOval(x + 6 * s, y + 30 * s, 68 * s, 26 * s);
            g.setFill(Color.web("#CE93D8").deriveColor(0, 1, 1, 0.28));
            g.fillOval(x + 10 * s, y + 48 * s, 64 * s, 24 * s);
        }
        if (type == BirdGame3.BirdType.HUMMINGBIRD && isSunflareSkin) {
            g.setStroke(Color.web("#FFECB3").deriveColor(0, 1, 1, 0.8));
            g.setLineWidth(2.0 * s);
            g.strokeLine(x + 12 * s, y + 40 * s, x + 68 * s, y + 24 * s);
            g.setFill(Color.web("#FFE082").deriveColor(0, 1, 1, 0.3));
            g.fillOval(x + 18 * s, y + 52 * s, 36 * s, 20 * s);
        }
        if (type == BirdGame3.BirdType.SHOEBILL && isGlacierSkin) {
            g.setStroke(Color.web("#B3E5FC").deriveColor(0, 1, 1, 0.85));
            g.setLineWidth(2.2 * s);
            g.strokeLine(x + 18 * s, y + 30 * s, x + 58 * s, y + 18 * s);
            g.strokeLine(x + 20 * s, y + 56 * s, x + 60 * s, y + 44 * s);
        }
        if (type == BirdGame3.BirdType.VULTURE && isTideSkin) {
            g.setStroke(Color.web("#80CBC4").deriveColor(0, 1, 1, 0.7));
            g.setLineWidth(2.1 * s);
            g.strokeArc(x + 8 * s, y + 32 * s, 70 * s, 40 * s, 200, 160, ArcType.OPEN);
        }
        if (type == BirdGame3.BirdType.MOCKINGBIRD && isEclipseSkin) {
            g.setStroke(Color.web("#E040FB").deriveColor(0, 1, 1, 0.65));
            g.setLineWidth(2.4 * s);
            g.strokeOval(x - 6 * s, y + 6 * s, 92 * s, 92 * s);
            g.setFill(Color.web("#5E35B1").deriveColor(0, 1, 1, 0.25));
            g.fillOval(x + 14 * s, y + 38 * s, 52 * s, 28 * s);
        }
        if (type == BirdGame3.BirdType.BAT && isUmbraSkin) {
            g.setStroke(Color.web("#00E5FF").deriveColor(0, 1, 1, 0.45));
            g.setLineWidth(2.0 * s);
            g.strokeOval(x - 10 * s, y - 10 * s, 100 * s, 100 * s);
        }
    }

    private void drawPenguinIceBuff(GraphicsContext g, double drawSize) {
        if (type != BirdGame3.BirdType.PENGUIN || penguinIceFxTimer <= 0) return;
        double pulse = 0.5 + 0.5 * Math.sin(System.currentTimeMillis() / 90.0);
        g.setFill(Color.web("#80DEEA").deriveColor(0, 1, 1, 0.26 + 0.2 * pulse));
        g.fillOval(x - 36, y - 34, drawSize + 72, drawSize + 72);
        g.setStroke(Color.web("#E1F5FE").deriveColor(0, 1, 1, 0.7));
        g.setLineWidth(2.8);
        g.strokeOval(x - 22, y - 20, drawSize + 44, drawSize + 44);
    }

    private void drawBatBody(GraphicsContext g) {
        double s = sizeMultiplier;
        double cx = x + 40 * s;
        double cy = y + 40 * s;
        boolean airborne = !isOnGround();
        if (batHanging) {
            g.save();
            g.translate(cx, cy);
            g.scale(1, -1);
            g.translate(-cx, -cy);
        }

        boolean umbra = isUmbraSkin;
        Color wing = umbra ? Color.web("#0B0F1A") : Color.rgb(28, 16, 48);
        Color wingInner = umbra ? Color.web("#182032") : Color.rgb(50, 30, 76);
        Color body = umbra ? Color.web("#1C1033") : Color.rgb(70, 40, 102);
        Color head = umbra ? Color.web("#2D1B4D") : Color.rgb(88, 54, 124);
        double flap = airborne ? Math.sin(System.currentTimeMillis() / 90.0) * 10 * s : 0;
        double leftWingY = y + 18 * s - flap;
        double rightWingY = y + 18 * s - flap;
        double wingSpread = airborne ? 1.15 : 1.0;

        // outer wings
        g.setFill(wing);
        g.fillOval(x - 64 * wingSpread * s, leftWingY, 88 * wingSpread * s, 54 * s);
        g.fillOval(x + 56 * s, rightWingY, 88 * wingSpread * s, 54 * s);
        g.fillPolygon(
                new double[]{x + 6 * s, x - 52 * wingSpread * s, x - 8 * s},
                new double[]{y + 48 * s - flap * 0.6, y + 72 * s - flap * 0.2, y + 86 * s},
                3
        );
        g.fillPolygon(
                new double[]{x + 74 * s, x + 132 * wingSpread * s, x + 88 * s},
                new double[]{y + 48 * s - flap * 0.6, y + 72 * s - flap * 0.2, y + 86 * s},
                3
        );

        // inner wing membrane
        g.setFill(wingInner);
        g.fillOval(x - 36 * wingSpread * s, y + 26 * s - flap * 0.45, 64 * wingSpread * s, 42 * s);
        g.fillOval(x + 52 * s, y + 26 * s - flap * 0.45, 64 * wingSpread * s, 42 * s);

        // torso and head
        g.setFill(body);
        g.fillOval(x + 20 * s, y + 22 * s, 40 * s, 54 * s);
        double headX = facingRight ? x + 24 * s : x + 16 * s;
        g.setFill(head);
        g.fillOval(headX, y + 6 * s, 44 * s, 32 * s);

        // ears
        Color ear = umbra ? Color.web("#4C537A") : Color.rgb(110, 74, 150);
        g.setFill(ear);
        g.fillPolygon(new double[]{headX + 6 * s, headX + 12 * s, headX + 18 * s}, new double[]{y + 8 * s, y - 10 * s, y + 8 * s}, 3);
        g.fillPolygon(new double[]{headX + 26 * s, headX + 32 * s, headX + 38 * s}, new double[]{y + 8 * s, y - 10 * s, y + 8 * s}, 3);

        // eyes
        g.setFill(Color.WHITE);
        double eyeBias = (facingRight ? 3 : -3) * s;
        g.fillOval(headX + 8 * s + eyeBias, y + 16 * s, 11 * s, 11 * s);
        g.fillOval(headX + 24 * s + eyeBias, y + 16 * s, 11 * s, 11 * s);
        Color iris = umbra ? Color.web("#00E5FF") : Color.CRIMSON.brighter();
        g.setFill(iris);
        g.fillOval(headX + 11 * s + eyeBias, y + 19 * s, 6 * s, 6 * s);
        g.fillOval(headX + 27 * s + eyeBias, y + 19 * s, 6 * s, 6 * s);

        if (batHanging) {
            g.setStroke(Color.LIGHTGRAY);
            g.setLineWidth(2 * s);
            g.strokeLine(x + 30 * s, y + 76 * s, x + 30 * s, y + 92 * s);
            g.strokeLine(x + 50 * s, y + 76 * s, x + 50 * s, y + 92 * s);
            g.restore();
        }
    }

    private void drawPhoenixBody(GraphicsContext g, double drawSize) {
        double s = sizeMultiplier;
        boolean nova = isNovaSkin;
        boolean classicPalette = isClassicSkin && !nova;
        Color bodyMain = nova ? Color.web("#1A1033") : (classicPalette ? game.classicSkinPrimaryColor(type) : Color.rgb(230, 95, 48));
        Color bodyHead = nova ? Color.web("#3D1B6B") : (classicPalette ? game.classicSkinPrimaryColor(type).brighter() : Color.rgb(246, 132, 74));
        Color accent = nova ? Color.web("#00E5FF") : (classicPalette ? game.classicSkinAccentColor(type) : Color.GOLD);
        Color innerAccent = nova ? Color.web("#E040FB") : Color.ORANGERED.deriveColor(0, 1, 1, 0.72);
        double tailBaseX = facingRight ? x + 6 * s : x + 74 * s;
        double tailBackOffset = facingRight ? -14 * s : 14 * s;
        double tailFrontOffset = facingRight ? 6 * s : -6 * s;
        double innerBaseX = facingRight ? x + 11 * s : x + 69 * s;
        double innerBackOffset = facingRight ? -12 * s : 12 * s;
        double innerFrontOffset = facingRight ? 5 * s : -5 * s;

        // Tail accent (small flame feather)
        g.setFill(accent.deriveColor(0, 1, 1, 0.88));
        g.fillPolygon(
                new double[]{tailBaseX, tailBaseX + tailBackOffset, tailBaseX + tailFrontOffset},
                new double[]{y + 58 * s, y + 70 * s, y + 76 * s},
                3
        );
        g.setFill(innerAccent);
        g.fillPolygon(
                new double[]{innerBaseX, innerBaseX + innerBackOffset, innerBaseX + innerFrontOffset},
                new double[]{y + 60 * s, y + 76 * s, y + 80 * s},
                3
        );

        // Standard bird body/head layout.
        g.setFill(bodyMain);
        g.fillOval(x, y, drawSize, drawSize);
        g.setFill(bodyHead);
        g.fillOval(facingRight ? x + 50 * s : x - 20 * s, y + 20 * s, 50 * s, 40 * s);
        g.setFill(bodyMain.darker());
        g.fillOval(x + 14 * s, y + 28 * s, 42 * s, 26 * s);

        // Small crest feathers (subtle).
        double crestBaseX = facingRight ? x + 60 * s : x + 22 * s;
        g.setFill(accent);
        g.fillPolygon(
                new double[]{crestBaseX - 4 * s, crestBaseX, crestBaseX + 4 * s},
                new double[]{y + 19 * s, y + 3 * s, y + 19 * s},
                3
        );
        if (nova) {
            g.setStroke(accent.deriveColor(0, 1, 1, 0.8));
            g.setLineWidth(2.4 * s);
            g.strokeOval(x - 6 * s, y - 10 * s, drawSize + 12 * s, drawSize + 12 * s);

            g.setFill(innerAccent.deriveColor(0, 1, 1, 0.85));
            g.fillPolygon(
                    new double[]{crestBaseX - 10 * s, crestBaseX, crestBaseX + 10 * s},
                    new double[]{y + 21 * s, y - 6 * s, y + 21 * s},
                    3
            );
        }

        // Eyes (standard placement).
        g.setFill(Color.WHITE);
        g.fillOval(x + (facingRight ? 50 : 20) * s, y + 20 * s, 25 * s, 25 * s);
        g.setFill(Color.CRIMSON.brighter());
        g.fillOval(x + (facingRight ? 56 : 26) * s, y + 25 * s, 13 * s, 13 * s);
    }

    private void drawCitySkin(GraphicsContext g) {
        if (type == BirdGame3.BirdType.PIGEON && isCitySkin) {
            double s = sizeMultiplier;
            g.setFill(Color.DARKGRAY.darker());
            g.fillRoundRect(x + 20 * s, y - 10 * s, 40 * s, 20 * s, 10 * s, 10 * s);
            g.fillRect(x + 10 * s, y - 5 * s, 60 * s, 8 * s);

            g.setFill(Color.WHITE);
            g.fillRect(facingRight ? x + 85 * s : x - 15 * s, y + 45 * s, 20 * s, 4 * s);
            g.setFill(Color.ORANGE.brighter());
            g.fillRect(facingRight ? x + 105 * s : x - 35 * s, y + 45 * s, 8 * s, 4 * s);

            if (Math.random() < 0.7) {
                double smokeX = facingRight ? x + 110 * s : x - 20 * s;
                double smokeY = y + 40 * s + Math.random() * 12 * s;
                game.particles.add(new Particle(
                        smokeX,
                        smokeY,
                        (Math.random() - 0.5) * 3,
                        -1.5 - Math.random() * 2,
                        Color.LIGHTGRAY.deriveColor(0, 1, 1, 0.3 + Math.random() * 0.4)
                ));
            }
        }
    }

    private void drawNoirSkin(GraphicsContext g) {
        if (type == BirdGame3.BirdType.PIGEON && isNoirSkin) {
            double s = sizeMultiplier;
            g.setFill(Color.BLACK);
            g.fillRoundRect(x + 16 * s, y - 12 * s, 48 * s, 18 * s, 10 * s, 10 * s);
            g.fillRect(x + 4 * s, y - 6 * s, 72 * s, 8 * s);

            g.setFill(Color.BLACK.deriveColor(0, 1, 0.8, 1));
            g.fillRoundRect(x + 18 * s, y + 52 * s, 44 * s, 22 * s, 10 * s, 10 * s);
            g.setStroke(Color.RED.brighter());
            g.setLineWidth(3 * s);
            g.strokeLine(x + 22 * s, y + 58 * s, x + 58 * s, y + 70 * s);

            if (Math.random() < 0.45) {
                double smokeX = facingRight ? x + 98 * s : x - 10 * s;
                double smokeY = y + 42 * s + Math.random() * 10 * s;
                game.particles.add(new Particle(
                        smokeX,
                        smokeY,
                        (Math.random() - 0.5) * 2.2,
                        -1.2 - Math.random() * 1.8,
                        Color.GRAY.deriveColor(0, 1, 1, 0.35 + Math.random() * 0.25)
                ));
            }
        }
    }

    private void drawFreemanSkin(GraphicsContext g) {
        if (type == BirdGame3.BirdType.PIGEON && isFreemanSkin) {
            double s = sizeMultiplier;

            g.setFill(Color.web("#5D4037"));
            g.fillRoundRect(x + 16 * s, y - 10 * s, 48 * s, 18 * s, 10 * s, 10 * s);
            g.setFill(Color.web("#4E342E"));
            g.fillRect(x + 12 * s, y - 2 * s, 56 * s, 6 * s);

            double eyeX = x + (facingRight ? 50 : 20) * s;
            double eyeY = y + 20 * s;
            g.setFill(Color.web("#4E342E").deriveColor(0, 1, 1, 0.55));
            g.fillOval(eyeX, eyeY - 1 * s, 25 * s, 14 * s);

            g.setFill(Color.web("#ECEFF1"));
            g.fillRect(facingRight ? x + 85 * s : x - 15 * s, y + 45 * s, 20 * s, 4 * s);
            g.setFill(Color.web("#FF8F00"));
            g.fillRect(facingRight ? x + 105 * s : x - 35 * s, y + 45 * s, 8 * s, 4 * s);

            if (Math.random() < 0.6) {
                double smokeX = facingRight ? x + 110 * s : x - 20 * s;
                double smokeY = y + 40 * s + Math.random() * 12 * s;
                game.particles.add(new Particle(
                        smokeX,
                        smokeY,
                        (Math.random() - 0.5) * 2.4,
                        -1.4 - Math.random() * 2.0,
                        Color.LIGHTGRAY.deriveColor(0, 1, 1, 0.35 + Math.random() * 0.35)
                ));
            }

            g.setFill(Color.web("#8D6E63").deriveColor(0, 1, 1, 0.45));
            g.fillOval(x + 22 * s, y + 54 * s, 20 * s, 12 * s);
        }
    }

    private void drawBeaconSkin(GraphicsContext g, double drawSize) {
        if (type == BirdGame3.BirdType.PIGEON && isBeaconSkin) {
            double s = sizeMultiplier;
            double pulse = 0.5 + 0.5 * Math.sin(System.currentTimeMillis() / 160.0);
            g.setFill(Color.web("#FFF59D").deriveColor(0, 1, 1, 0.18 + 0.18 * pulse));
            g.fillOval(x - 14 * s, y - 14 * s, drawSize + 28 * s, drawSize + 28 * s);

            g.setStroke(Color.web("#FFE082").deriveColor(0, 1, 1, 0.75));
            g.setLineWidth(2.2 * s);
            g.strokeOval(x - 8 * s, y - 8 * s, drawSize + 16 * s, drawSize + 16 * s);

            double headX = facingRight ? x + 50 * s : x - 20 * s;
            double headY = y + 20 * s;
            g.setFill(Color.web("#81D4FA").deriveColor(0, 1, 1, 0.85));
            g.fillOval(headX + 18 * s, headY - 10 * s, 10 * s, 10 * s);
            g.setStroke(Color.web("#B3E5FC").deriveColor(0, 1, 1, 0.85));
            g.setLineWidth(1.6 * s);
            g.strokeLine(headX + 23 * s, headY - 12 * s, headX + 23 * s, headY - 20 * s);
        }
    }

    private void drawBeak(GraphicsContext g, double drawSize) {
        if (type == BirdGame3.BirdType.BAT) {
            double mouthX = x + 33 * sizeMultiplier;
            double mouthY = y + 28 * sizeMultiplier;
            boolean attacking = attackAnimationTimer > 0;
            double biteOpen = attacking ? (4 + Math.sin(attackAnimationTimer * 0.8) * 3) * sizeMultiplier : 0;
            g.setFill(Color.rgb(220, 120, 170));
            g.fillOval(mouthX, mouthY + biteOpen * 0.2, 16 * sizeMultiplier, (10 + biteOpen) * sizeMultiplier);
            g.setFill(Color.WHITE);
            g.fillPolygon(
                    new double[]{mouthX + 4 * sizeMultiplier, mouthX + 7 * sizeMultiplier, mouthX + 10 * sizeMultiplier},
                    new double[]{mouthY + (8 + biteOpen) * sizeMultiplier, mouthY + (13 + biteOpen) * sizeMultiplier, mouthY + (8 + biteOpen) * sizeMultiplier},
                    3
            );
            if (attacking) {
                g.setStroke(Color.MEDIUMPURPLE.brighter());
                g.setLineWidth(3);
                double dir = facingRight ? 1 : -1;
                for (int i = 0; i < 3; i++) {
                    double sx = x + 40 + dir * (25 + i * 12);
                    double sy = y + 44 - i * 6;
                    g.strokeLine(sx, sy, sx + dir * 20, sy - 10);
                }
            }
            return;
        }
        if (type == BirdGame3.BirdType.PHOENIX) {
            double s = sizeMultiplier;
            double beakY = y + 24 * s;
            boolean attacking = attackAnimationTimer > 0;
            double open = attacking ? (12 + Math.sin(attackAnimationTimer * 0.7) * 6) * s : 2.5 * s;
            g.setFill(Color.GOLD);
            if (facingRight) {
                double baseX = x + 72 * s;
                g.fillPolygon(
                        new double[]{baseX, baseX + 30 * s, baseX + 2 * s},
                        new double[]{beakY, beakY + open, beakY + 8 * s},
                        3
                );
                g.fillPolygon(
                        new double[]{baseX + 1 * s, baseX + 27 * s, baseX + 3 * s},
                        new double[]{beakY + 2 * s, beakY - open * 0.55, beakY + 9 * s},
                        3
                );
            } else {
                double baseX = x + 8 * s;
                g.fillPolygon(
                        new double[]{baseX, baseX - 30 * s, baseX - 2 * s},
                        new double[]{beakY, beakY + open, beakY + 8 * s},
                        3
                );
                g.fillPolygon(
                        new double[]{baseX - 1 * s, baseX - 27 * s, baseX - 3 * s},
                        new double[]{beakY + 2 * s, beakY - open * 0.55, beakY + 9 * s},
                        3
                );
            }
            return;
        }

        boolean isAttacking = attackAnimationTimer > 0;
        double openAmount = isAttacking ? (16 + Math.sin(attackAnimationTimer * 0.7) * 10) * sizeMultiplier : 3 * sizeMultiplier;
        double beakBaseY = y + 45 * sizeMultiplier;
        double beakLength = (type == BirdGame3.BirdType.FALCON ? 34 : 28) * sizeMultiplier;

        g.setFill(isAttacking ? Color.ORANGERED : Color.ORANGE);

        if (facingRight) {
            double tipX = x + 80 * sizeMultiplier + beakLength;

            g.fillPolygon(
                    new double[]{x + 80 * sizeMultiplier, tipX, x + 80 * sizeMultiplier},
                    new double[]{beakBaseY - 8 * sizeMultiplier, beakBaseY - openAmount, beakBaseY + 8 * sizeMultiplier},
                    3
            );
            g.fillPolygon(
                    new double[]{x + 80 * sizeMultiplier, tipX, x + 80 * sizeMultiplier},
                    new double[]{beakBaseY - 8 * sizeMultiplier, beakBaseY + openAmount * 1.6, beakBaseY + 8 * sizeMultiplier},
                    3
            );

            if (isAttacking && attackAnimationTimer > 4) {
                g.setFill(Color.DEEPPINK.darker());
                g.fillOval(tipX - 12, beakBaseY - 4, 20, 14);
            }
        } else {
            double tipX = x - beakLength;

            g.fillPolygon(
                    new double[]{x, tipX, x},
                    new double[]{beakBaseY - 8 * sizeMultiplier, beakBaseY - openAmount, beakBaseY + 8 * sizeMultiplier},
                    3
            );
            g.fillPolygon(
                    new double[]{x, tipX, x},
                    new double[]{beakBaseY - 8 * sizeMultiplier, beakBaseY + openAmount * 1.6, beakBaseY + 8 * sizeMultiplier},
                    3
            );

            if (isAttacking && attackAnimationTimer > 4) {
                g.setFill(Color.DEEPPINK.darker());
                g.fillOval(tipX - 8, beakBaseY - 4, 20, 14);
            }
        }

        int flashFrame = 12;
        if (attackAnimationTimer == flashFrame) {
            double flashOpacity = 0.7;
            double flashSize = 36;
            g.setFill(Color.WHITE.deriveColor(0, 1, 1, flashOpacity));
            g.fillOval(
                    facingRight ? x + 90 * sizeMultiplier : x - 40 * sizeMultiplier,
                    y + 30 * sizeMultiplier,
                    flashSize * sizeMultiplier,
                    flashSize * sizeMultiplier
            );
        }
    }

    private void drawPelican(GraphicsContext g, double drawSize) {
        if (type == BirdGame3.BirdType.PELICAN) {
            double headX = facingRight ? x + 50 * sizeMultiplier : x - 20 * sizeMultiplier;
            double pouchX = headX + 2 * sizeMultiplier;
            double pouchY = y + 42 * sizeMultiplier;
            double pouchW = (plungeTimer > 0 ? 62 : 46) * sizeMultiplier;
            double pouchH = (plungeTimer > 0 ? 38 : 28) * sizeMultiplier;
            g.setFill(Color.rgb(255, 180, 80));
            g.fillOval(pouchX, pouchY, pouchW, pouchH);
            g.setFill(Color.rgb(255, 200, 100));
            g.fillOval(pouchX + 5 * sizeMultiplier, pouchY + 4 * sizeMultiplier, pouchW - 12 * sizeMultiplier, pouchH - 12 * sizeMultiplier);
        }
    }

    private void drawVineGrapple(GraphicsContext g, double drawSize) {
        if (grappleUses > 0) {
            g.setFill(Color.LIMEGREEN.brighter());
            g.setFont(Font.font("Arial Black", FontWeight.BOLD, 36 * sizeMultiplier));
            g.setEffect(new Glow(0.8));
            g.setStroke(Color.BLACK);
            g.setLineWidth(4 * sizeMultiplier);
            String usesText = String.valueOf(grappleUses);
            double textWidth = g.getFont().getSize() * usesText.length() * 0.55;
            g.strokeText(usesText, x + 40 - textWidth / 2, y - 60);
            g.setFill(Color.LIMEGREEN.brighter());
            g.fillText(usesText, x + 40 - textWidth / 2, y - 60);
            g.setEffect(null);

            g.setFill(Color.FORESTGREEN.darker());
            g.fillOval(x + 25, y - 45, 30 * sizeMultiplier, 40 * sizeMultiplier);
            g.setFill(Color.LIMEGREEN);
            for (int i = 0; i < 3; i++) {
                double leafAngle = i * Math.PI * 2 / 3;
                g.fillOval(x + 40 + Math.cos(leafAngle) * 20 * sizeMultiplier,
                        y - 40 + Math.sin(leafAngle) * 20 * sizeMultiplier,
                        16 * sizeMultiplier, 24 * sizeMultiplier);
            }
        }
    }
}

