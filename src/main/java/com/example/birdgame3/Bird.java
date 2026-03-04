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

import java.util.Iterator;
import java.util.Random;

/**
 * Represents a playable bird character in Bird Game 3.
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
    public int bladeStormFrames = 0;
    public int plungeTimer = 0;

    public boolean isBlocking = false;
    public int blockCooldown = 0;

    // === VINE SWINGING ===
    public SwingingVine attachedVine = null;
    public boolean onVine = false;

    // === POWER-UP BUFFS ===
    public double speedMultiplier = 1.0;
    public double powerMultiplier = 1.0;
    public double sizeMultiplier = 1.0;
    public int speedTimer = 0;
    public int rageTimer = 0;
    public int shrinkTimer = 0;
    public int thermalTimer = 0;
    public double thermalLift = 0.0;

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
            sizeMultiplier = 1.2;
        }
    }

    public boolean isOnGround() {
        double bottom = y + 80 * sizeMultiplier;
        if (bottom >= game.GROUND_Y) return true;
        for (Platform p : game.platforms) {
            if (x + 40 * sizeMultiplier >= p.x && x + 40 * sizeMultiplier <= p.x + p.w &&
                    bottom >= p.y && bottom <= p.y + p.h)
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
                if (health > 100) health = 100;
            }
        }
    }

    private void handleVerticalCollision() {
        if (onVine) return;

        boolean hit = false;
        double newY = y;

        for (Platform p : game.platforms) {
            if (x + 40 * sizeMultiplier >= p.x && x + 40 * sizeMultiplier <= p.x + p.w &&
                    y + 80 * sizeMultiplier > p.y && y < p.y + p.h) {
                newY = p.y - 80 * sizeMultiplier;
                hit = true;
                break;
            }
        }

        if (!hit && y + 80 * sizeMultiplier > game.GROUND_Y) {
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
            double dx = other.x - x;
            if (Math.abs(dx) < 280 && Math.abs(other.y - y) < 180) {
                int dmg = (int) (28 * powerMultiplier);
                double oldHealth = other.health;
                other.health = Math.max(0, other.health - dmg);
                double dealtDamage = oldHealth - other.health;

                game.damageDealt[playerIndex] += (int) dealtDamage;
                boolean isKill = oldHealth > 0 && other.health <= 0;
                if (isKill) {
                    game.eliminations[playerIndex]++;
                    game.groundPounds[playerIndex]++;
                    if (game.zombieFallingClip != null) game.zombieFallingClip.play();
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
        String verb = (type == BirdGame3.BirdType.RAZORBILL && diveTimer > 0)
                ? "DIVEBOMBED" : (damage >= 35 ? "BRUTALIZED" : damage >= 25 ? "SMASHED" : "hit");
        game.addToKillFeed(attacker + " " + verb + " " + victimName + "! -" + (int) damage + " HP");

        if (isKill) {
            game.addToKillFeed("ELIMINATED " + victimName + "!");
        }
    }

    private void attack() {
        if (health <= 0) return;
        double range = 120 * sizeMultiplier;
        int dmg = (int) (type.power * powerMultiplier);
        if (type == BirdGame3.BirdType.RAZORBILL && diveTimer > 0) {
            range = 200;
            dmg = (int) (type.power * 4 * powerMultiplier);
        }

        for (Bird other : game.players) {
            if (other == null || other == this || other.health <= 0) continue;

            if (game.isTrialMode) {
                boolean attackerIsFriendly = playerIndex <= 2;
                boolean targetIsFriendly = other.playerIndex <= 2;
                if (attackerIsFriendly == targetIsFriendly) {
                    continue;
                }
            }

            double dist = Math.abs(x - other.x);
            if (dist < range && Math.abs(y - other.y) < 100 * sizeMultiplier) {
                processBirdAttack(other, dmg);
            }
        }

        // === LOUNGE CAN BE HIT ===
        attackLounge();
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
        other.health -= dmg;
        if (other.health < 0) other.health = 0;
        double dealtDamage = oldHealth - other.health;

        game.damageDealt[playerIndex] += (int) dealtDamage;
        if (other.health <= 0 && oldHealth > 0) {
            game.eliminations[playerIndex]++;
            game.checkAchievements(this);
            if (game.zombieFallingClip != null) game.zombieFallingClip.play();
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

            double distToLounge = Math.hypot(target.loungeX - (x + 40), target.loungeY - (y + 40));
            if (distToLounge < 130) {
                int loungeDmg = (int) (type.power * 2.0 * powerMultiplier);
                if (type == BirdGame3.BirdType.RAZORBILL && diveTimer > 0) loungeDmg *= 2;

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

        if (game.jalapenoClip != null) game.jalapenoClip.play();

        switch (type) {
            case PIGEON -> specialPigeon();
            case EAGLE -> specialEagle();
            case HUMMINGBIRD -> specialHummingbird();
            case TURKEY -> specialTurkey();
            case PENGUIN -> specialPenguin();
            case SHOEBILL -> specialShoebill();
            case MOCKINGBIRD -> specialMockingbird();
            case RAZORBILL -> specialRazorbill();
            case GRINCHHAWK -> specialGrinchhawk();
            case VULTURE -> specialVulture();
            case OPIUMBIRD -> specialOpiumBird();
            case TITMOUSE -> specialTitmouse();
            case PELICAN -> specialPelican();
        }
    }

    private void specialPigeon() {
        health = Math.min(100, health + 20);
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
        diveTimer = 100;
        specialCooldown = 540;
        specialMaxCooldown = 540;

        game.shakeIntensity = 35;
        game.hitstopFrames = 20;
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

        eagleDiveCountdown = 60;
    }

    private void specialHummingbird() {
        vy -= type.jumpHeight * 1.2;
        vx += (facingRight ? 1 : -1) * 7;
        specialCooldown = 270;
        specialMaxCooldown = 270;
        game.addToKillFeed(name.split(":")[0].trim() + " FLUTTER BOOST!");
        for (int i = 0; i < 30; i++) {
            game.particles.add(new Particle(x + 40, y + 40,
                    (Math.random() - 0.5) * 20, -10 - Math.random() * 10,
                    Color.CYAN.brighter()));
        }
    }

    private void specialTurkey() {
        vy = -type.jumpHeight * 1.45;
        isGroundPounding = true;
        specialCooldown = 450;
        specialMaxCooldown = 450;
    }

    private void specialPenguin() {
        vx = (facingRight ? 1 : -1) * 20.0;
        vy = 0;
        specialCooldown = 510;
        specialMaxCooldown = 510;
        game.addToKillFeed(name.split(":")[0].trim() + " ICE SLIDE!");
        for (int i = 0; i < 50; i++) {
            game.particles.add(new Particle(x + 40 + (Math.random() - 0.5) * 100,
                    y + 70, (Math.random() - 0.5) * 8, -2 - Math.random() * 4,
                    Color.CYAN.deriveColor(0, 1, 1, 0.7)));
        }
    }

    private void specialShoebill() {
        for (Bird other : game.players) {
            if (other == null || other == this || other.health <= 0) continue;
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
        diveTimer = 160;
        specialCooldown = 780;
        specialMaxCooldown = 780;

        game.shakeIntensity = 28;
        game.hitstopFrames = 18;
        game.addToKillFeed("BLADE STORM! " + name.split(":")[0].trim() + " IS UNSTOPPABLE!");

        vx *= 2.0;
        sizeMultiplier = 1.15;
        powerMultiplier = 1.45;

        for (int i = 0; i < 100; i++) {
            double angle = i / 100.0 * Math.PI * 2;
            double speed = 8 + Math.random() * 10;
            game.particles.add(new Particle(
                    x + 40 + Math.cos(angle) * 50,
                    y + 40 + Math.sin(angle) * 50,
                    Math.cos(angle) * speed,
                    Math.sin(angle) * speed,
                    Color.CYAN.brighter()
            ));
        }
        bladeStormFrames = 150;
    }

    private void specialGrinchhawk() {
        int stolen = 0;
        for (Bird other : game.players) {
            if (other == null || other == this || other.health <= 0) continue;
            int take = (int) Math.min(8 + (health > 80 ? 4 : 0), other.health);
            other.health -= take;
            stolen += take;
            game.addToKillFeed(name.split(":")[0].trim() + " STOLE " + take + " HP from " + other.name.split(":")[0].trim() + "!");
        }
        health = Math.min(100, health + stolen);
        specialCooldown = 840;
        specialMaxCooldown = 840;
        game.shakeIntensity = 20;
    }

    private void specialVulture() {
        crowSwarmCooldown = 1080;
        specialCooldown = 1080;
        specialMaxCooldown = 1080;
        game.addToKillFeed(name.split(":")[0].trim() + " SUMMONS THE MURDER!");

        int crowCount = 10 + random.nextInt(6);
        for (int i = 0; i < crowCount; i++) {
            double angle = Math.random() * Math.PI * 2;
            double dist = 300 + Math.random() * 1200;
            double spawnX = x + 40 + Math.cos(angle) * dist;
            double spawnY = y + 40 + Math.sin(angle) * dist;

            CrowMinion crow = new CrowMinion(spawnX, spawnY, null);
            crow.owner = this;
            game.crowMinions.add(crow);
        }

        game.shakeIntensity = 18;
        game.hitstopFrames = 12;
        carrionSwarmTimer = 120;

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
        game.addToKillFeed(name.split(":")[0].trim() + " DROPPED THE LEAN!");
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

    private void specialTitmouse() {
        Bird target = null;
        double bestDist = Double.MAX_VALUE;
        for (Bird b : game.players) {
            if (b == null || b == this || b.health <= 0) continue;
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
            if (b == null || b == this || b.health <= 0) continue;
            if (game.isTrialMode && ((playerIndex <= 2) == (b.playerIndex <= 2))) continue;
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
            target.vx += (target.x > x ? 1 : -1) * 42;
            target.vy = -30;
            int dmg = (int)(26 * powerMultiplier);
            double old = target.health;
            target.health = Math.max(0, target.health - dmg);
            game.damageDealt[playerIndex] += (int)(old - target.health);
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
            case 1 -> KeyCode.DOWN;
            case 2 -> KeyCode.U;
            case 3 -> KeyCode.P;
            default -> KeyCode.SHIFT;
        };
    }

    private KeyCode blockKey() {
        return switch (playerIndex) {
            case 0 -> KeyCode.S;
            case 1 -> KeyCode.BACK_SLASH;
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

    private void aiControl() {
        if (aiJumpCooldown > 0) aiJumpCooldown--;
        if (aiSpecialCooldown > 0) aiSpecialCooldown--;
        if (aiStrafeTimer > 0) aiStrafeTimer--;

        clearAIInputs();

        Bird target = pickAITarget();
        PowerUp powerUp = pickBestAIPowerUp(target);
        boolean onGround = isOnGround();

        if (target == null && powerUp == null) return;

        double myCx = x + 40;
        double targetDist = target != null ? Math.hypot(target.x - x, target.y - y) : Double.MAX_VALUE;
        double idealRange = getAIIdealRange();
        boolean lowHealth = health < 38;

        // Emergency self-preservation before anything else.
        if (onGround && y > game.GROUND_Y + 220) {
            game.pressedKeys.add(jumpKey());
            aiJumpCooldown = 12;
        }

        if (handleAIDodgeBurstThreats(target, onGround)) return;

        PowerUp healthPack = findBestHealthPowerUp();
        boolean shouldRetreat = target != null && health < 30 && target.health > health + 18 && targetDist < 280 && healthPack == null;
        boolean shouldChasePower = powerUp != null && shouldPrioritizePowerUp(powerUp, target);

        double goalX;
        if (shouldRetreat && target != null) {
            goalX = x + (x - target.x) * 2.0;
        } else if (shouldChasePower) {
            goalX = powerUp.x;
        } else if (target != null) {
            // Predict movement instead of chasing current position.
            double lead = Math.min(10, Math.max(2, targetDist / 120.0));
            double predictedX = target.x + target.vx * lead;
            if (targetDist > idealRange * 1.25) {
                goalX = predictedX;
            } else if (targetDist < idealRange * 0.65) {
                goalX = x - Math.signum(predictedX - x) * 180;
            } else {
                if (aiStrafeTimer <= 0) {
                    aiStrafeTimer = 40 + random.nextInt(35);
                    aiStrafeDir = random.nextBoolean() ? 1 : -1;
                }
                goalX = target.x + aiStrafeDir * 140;
            }
        } else {
            goalX = x;
        }

        goalX = Math.max(120, Math.min(goalX, game.WORLD_WIDTH - 120));

        if (target != null) facingRight = target.x > myCx;
        else if (powerUp != null) facingRight = powerUp.x > myCx;

        int moveDir = 0;
        if (Math.abs(goalX - x) > 35) {
            moveDir = goalX < x ? -1 : 1;
        }

        // Anti-stall fallback: if spacing logic leaves us idle too long, pressure target.
        if (target != null && moveDir == 0 && targetDist > 130) {
            aiIdleFrames++;
            if (aiIdleFrames > 24) {
                moveDir = target.x < x ? -1 : 1;
            }
        } else {
            aiIdleFrames = 0;
        }

        if (moveDir < 0) game.pressedKeys.add(leftKey());
        if (moveDir > 0) game.pressedKeys.add(rightKey());

        // Vertical positioning and recovery behavior.
        if (target != null) {
            double dy = target.y - y;
            if (onGround && aiJumpCooldown <= 0) {
                boolean jumpForHeight = dy < -120 && Math.abs(target.x - x) < 420;
                boolean jumpForCombo = dy > 70 && targetDist < 220;
                if (jumpForHeight || jumpForCombo) {
                    game.pressedKeys.add(jumpKey());
                    aiJumpCooldown = 14;
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
            game.pressedKeys.add(jumpKey());
            aiJumpCooldown = 14;
        }

        // Defensive block read.
        if (target != null && targetDist < 170 && target.attackAnimationTimer > 3 &&
                facingRight == (target.x > x) && random.nextDouble() < 0.55) {
            game.pressedKeys.add(blockKey());
        }

        // Attack cadence respects role/range.
        if (target != null && attackCooldown <= 0 &&
                targetDist < Math.max(140, idealRange * 0.95) &&
                Math.abs(target.y - y) < 115 &&
                random.nextDouble() < 0.85) {
            game.pressedKeys.add(attackKey());
        }

        // Special ability timing by bird role.
        if (target != null && specialCooldown <= 0 && aiSpecialCooldown <= 0 &&
                shouldUseSpecialAI(target, targetDist, onGround, lowHealth)) {
            game.pressedKeys.add(specialKey());
            aiSpecialCooldown = 26;
        }

        if (tauntCooldown <= 0 && target != null && health > 80 && target.health < 35 &&
                targetDist < 200 && random.nextDouble() < 0.006) {
            currentTaunt = random.nextInt(3) + 1;
            tauntTimer = 50;
            tauntCooldown = 300;
            game.addToKillFeed(name.split(":")[0].trim() + " IS ABSOLUTELY COOKING!");
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
            if (game.isTrialMode) {
                if (playerIndex <= 2 && b.playerIndex != 3) continue;
                if (playerIndex == 3 && b.playerIndex > 2) continue;
            }
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
            }
            score /= (1 + myDist / 350.0);

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
        if (p.type == PowerUpType.HEALTH && health < 72) return true;
        if (target == null) return true;
        double targetDist = Math.hypot(target.x - x, target.y - y);
        return targetDist > getAIIdealRange() * 1.7;
    }

    private double getAIIdealRange() {
        return switch (type) {
            case TURKEY, PELICAN, GRINCHHAWK -> 170;
            case RAZORBILL, SHOEBILL -> 200;
            case EAGLE, VULTURE, PENGUIN -> 230;
            case HUMMINGBIRD, TITMOUSE -> 250;
            case OPIUMBIRD, MOCKINGBIRD -> 240;
            case PIGEON -> 210;
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
        if (target.type == BirdGame3.BirdType.PENGUIN && Math.abs(target.vx) > 14 && dx < 380 && Math.abs(dy) < 110) dodge = true;
        if (target.type == BirdGame3.BirdType.TITMOUSE && target.isZipping && dx < 420) dodge = true;

        if (!dodge) return false;

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
            case HUMMINGBIRD:
                return dist > 420 || dy < -220;
            case TURKEY:
                return onGround && dist < 280 && dy > 50;
            case PENGUIN:
                return onGround && dist > 110 && dist < 340 && Math.abs(dy) < 90;
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
            case TITMOUSE:
                return dist > 140 && dist < 560;
            case PELICAN:
                return plungeTimer <= 0 && onGround && dist < 260 && Math.abs(dy) < 130;
            default:
                return false;
        }
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

        // === OPIUM BIRD ===
        handleOpiumBirdEffects();

        boolean stunned = stunTime > 0;
        boolean airborne = !isOnGround();

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
        vy += BirdGame3.GRAVITY * gameSpeed;

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
            vy -= (type.flyUpForce + thermalLift) * hoverRegenMultiplier;
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

        // === EAGLE DIVE IMPACT ===
        handleEagleDiveImpact();

        // === RAZORBILL BLADE STORM ===
        handleRazorbillBladeStorm();

        // === APPLY VELOCITY ===
        x += vx;
        y += vy;

        // === THERMALS & WIND VENTS ===
        handleThermals();

        // === BOUNDARIES ===
        handleBoundaries();

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
        thermalTimer = Math.max(0, (int)(thermalTimer - gameSpeed));
        grappleTimer = Math.max(0, (int)(grappleTimer - gameSpeed));
        speedBoostTimer = Math.max(0, (int)(speedBoostTimer - gameSpeed));
        hoverRegenTimer = Math.max(0, (int)(hoverRegenTimer - gameSpeed));

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
        plungeTimer = Math.max(0, (int)(plungeTimer - gameSpeed));
        blockCooldown = Math.max(0, (int)(blockCooldown - gameSpeed));
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
            speedMultiplier = 1.0;
        }
        if (hoverRegenTimer <= 0) {
            hoverRegenTimer = 0;
            hoverRegenMultiplier = 1.0;
        }
        if (speedTimer <= 0) {
            speedMultiplier = 1.0;
        }
        if (rageTimer <= 0) {
            powerMultiplier = 1.0;
        }
        if (shrinkTimer <= 0) {
            sizeMultiplier = (type == BirdGame3.BirdType.PELICAN ? 1.2 : 1.0);
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
        if (type == BirdGame3.BirdType.OPIUMBIRD) {
            if (leanTimer > 0) {
                game.leanTime[playerIndex]++;
            }

            if (leanTimer > 0) {
                for (Bird other : game.players) {
                    if (other == null || other == this || other.health <= 0) continue;
                    double dx = other.x - x;
                    double dy = other.y - y;
                    if (Math.hypot(dx, dy) < 300) {
                        if (Math.hypot(dx, dy) < 250) {
                            if (random.nextInt(60) == 0) {
                                other.health = Math.max(0, other.health - 1);
                                game.addToKillFeed(name.split(":")[0].trim() + "'s lean is COOKING " + other.name.split(":")[0].trim() + " (-1 HP)");
                            }
                            other.vx *= 0.94;
                            other.vy *= 0.98;
                        }

                        if (Math.hypot(dx, dy) < 120 && random.nextInt(20) == 0) {
                            other.highTimer = 180;
                        }
                    }
                }

                if (Math.random() < 0.1) highTimer = 120;
            }

            if (highTimer > 0) highTimer--;
        }
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
            if (other == null || other == this || other.health <= 0) continue;
            double dist = Math.hypot(other.x - x, other.y - y);
            if (dist < 120) {
                int dmg = (int) (20 * powerMultiplier);
                double oldHealth = other.health;
                other.health = Math.max(0, other.health - dmg);
                double dealt = oldHealth - other.health;
                game.damageDealt[playerIndex] += (int) dealt;
                if (other.health <= 0 && oldHealth > 0) game.eliminations[playerIndex]++;
                if (game.zombieFallingClip != null) game.zombieFallingClip.play();

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

    private void handleHorizontalMovement(boolean stunned, boolean airborne) {
        if (!stunned) {
            double targetVx = 0;
            double airFric = airborne ? 0.90 : 0.75;
            double accel = airborne ? 0.20 : 0.45;

            if (game.pressedKeys.contains(leftKey())) {
                targetVx = -type.speed * speedMultiplier;
                if (type == BirdGame3.BirdType.HUMMINGBIRD && game.pressedKeys.contains(jumpKey()) && airborne) {
                    targetVx *= 1.75;
                }
            }
            else if (game.pressedKeys.contains(rightKey())) {
                targetVx = type.speed * speedMultiplier;
                if (type == BirdGame3.BirdType.HUMMINGBIRD && game.pressedKeys.contains(jumpKey()) && airborne) {
                    targetVx *= 1.75;
                }
            }

            vx = vx * airFric + targetVx * accel;
            if (Math.abs(vx) > 0.1) facingRight = vx > 0;

            boolean canJump = isOnGround() || (type == BirdGame3.BirdType.PIGEON && canDoubleJump);
            if (game.pressedKeys.contains(jumpKey()) && canJump) {
                double mult = isOnGround() ? 1.0 : 0.75;
                double jumpPower = type.jumpHeight * mult;
                if (playerIndex == 3 && game.isTrialMode) {
                    jumpPower *= 0.4;
                }
                vy = -type.jumpHeight * mult;
                if (!isOnGround() && type == BirdGame3.BirdType.PIGEON) canDoubleJump = false;
                if (game.swingClip != null) game.swingClip.play();
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
                if (playerIndex == 3 && game.isTrialMode) {
                    attackCooldown = 72;
                    attackAnimationTimer = 24;
                } else {
                    attackCooldown = 30;
                    attackAnimationTimer = 12;
                }
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
        if ((type == BirdGame3.BirdType.EAGLE) && (eagleDiveCountdown > 0)) {
            eagleDiveCountdown--;
            if (eagleDiveCountdown == 0) {
                processDiveImpact();
            }
        }
    }

    private void processDiveImpact() {
        game.shakeIntensity = 60;
        game.hitstopFrames = 35;
        game.addToKillFeed("KABOOOOOM!!! " + name.split(":")[0].trim() + " OBLITERATES THE GROUND!");

        for (int i = 0; i < 300; i++) {
            double angle = i / 300.0 * Math.PI * 2;
            double speed = 12 + Math.random() * 25;
            double vx = Math.cos(angle) * speed;
            double vy = Math.sin(angle) * speed - 10;
            Color c = Math.random() < 0.5 ? Color.ORANGERED : Color.YELLOW.brighter();
            game.particles.add(new Particle(x + 40, y + 70, vx, vy, c));
        }

        for (int i = 0; i < 20; i++) {
            double offset = (Math.random() - 0.5) * 800;
            for (int j = 0; j < 15; j++) {
                game.particles.add(new Particle(x + 40 + offset + j * 10, game.GROUND_Y + j * 10,
                        (Math.random() - 0.5) * 25, -6 - Math.random() * 15, Color.SADDLEBROWN.darker()));
            }
        }

        for (Bird other : game.players) {
            if (other == null || other == this || other.health <= 0) continue;
            if (game.isTrialMode) {
                boolean attackerIsFriendly = playerIndex <= 2;
                boolean targetIsFriendly = other.playerIndex <= 2;
                if (attackerIsFriendly == targetIsFriendly) continue;
            }

            double dx = other.x - x;
            double dy = (other.y + 40) - (y + 70);
            double dist = Math.hypot(dx, dy);

            if (dist < 350) {
                int dmg = (int) (58 * (1.0 - dist / 500.0));
                if (dmg < 18) dmg = 18;

                double oldHealth = other.health;
                other.health = Math.max(0, other.health - dmg);
                game.damageDealt[playerIndex] += (int) (oldHealth - other.health);
                if (other.health <= 0 && oldHealth > 0) game.eliminations[playerIndex]++;

                double safeDist = Math.max(0.001, dist);
                other.vx += dx / safeDist * 50;
                other.vy -= 35;

                String intensity = dmg >= 50 ? "DEVASTATED" : dmg >= 35 ? "BLASTED" : "SMASHED";
                game.addToKillFeed(name.split(":")[0].trim() + " " + intensity + " " + other.name.split(":")[0].trim() + "! -" + dmg + " HP");

                if (dmg > 40) {
                    for (int k = 0; k < 60; k++) {
                        double angle = Math.random() * Math.PI * 2;
                        game.particles.add(new Particle(other.x + 40, other.y + 40,
                                Math.cos(angle) * (10 + Math.random() * 20),
                                Math.sin(angle) * (10 + Math.random() * 20) - 10,
                                Color.CRIMSON.brighter()));
                    }
                }
            }
        }
    }

    private void handleRazorbillBladeStorm() {
        if (type == BirdGame3.BirdType.RAZORBILL && bladeStormFrames > 0) {
            bladeStormFrames--;
            if ((150 - bladeStormFrames) % 14 == 0) {
                for (Bird other : game.players) {
                    if (other == null || other == this || other.health <= 0) continue;
                    double dx = other.x - x;
                    double dy = other.y - y;
                    double dist = Math.hypot(dx, dy);
                    if (dist < 160) {
                        int dmg = 10 + random.nextInt(7);
                        double oldHealth = other.health;
                        other.health = Math.max(0, other.health - dmg);
                        game.damageDealt[playerIndex] += (int) (oldHealth - other.health);
                        if (other.health <= 0 && oldHealth > 0) game.eliminations[playerIndex]++;
                        double safeDist = Math.max(0.001, dist);
                        other.vx += dx / safeDist * 32;
                        other.vy -= 20;
                        String verb = dmg >= 25 ? "GASHED" : "CUT";
                        game.addToKillFeed(name.split(":")[0].trim() + " " + verb + " " +
                                other.name.split(":")[0].trim() + "! -" + dmg + " HP");

                        for (int k = 0; k < 25; k++) {
                            double angle = Math.atan2(dy, dx) + (Math.random() - 0.5) * 1.4;
                            game.particles.add(new Particle(other.x + 40, other.y + 40,
                                    Math.cos(angle) * (8 + Math.random() * 16),
                                    Math.sin(angle) * (8 + Math.random() * 16) - 8, Color.CYAN));
                        }
                        game.shakeIntensity = 18;
                        game.hitstopFrames = 6;
                    }
                }

                for (int i = 0; i < 10; i++) {
                    double angle = facingRight ? 0 : Math.PI;
                    angle += (Math.random() - 0.5);
                    game.particles.add(new Particle(x + 40 + Math.cos(angle) * 70,
                            y + 40 + Math.sin(angle) * 70,
                            Math.cos(angle) * 25, Math.sin(angle) * 25,
                            Color.WHITE.deriveColor(0, 1, 1, 0.85)));
                }
            }

            if (bladeStormFrames <= 0) {
                sizeMultiplier = 1.0;
                powerMultiplier = 1.0;
            }
        }
    }

    private void handleThermals() {
        if (game.selectedMap == MapType.SKYCLIFFS || game.selectedMap == MapType.VIBRANT_JUNGLE) {
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
        if (x < 50) x = 50;
        if (x > game.WORLD_WIDTH - 150 * sizeMultiplier) x = game.WORLD_WIDTH - 150 * sizeMultiplier;

        if (x < -300 || x > game.WORLD_WIDTH + 300) {
            health = Math.max(0, health - 50);
            if (health <= 0) {
                game.addToKillFeed(name.split(":")[0].trim() + " FLEW INTO THE VOID!");
            } else {
                game.addToKillFeed(name.split(":")[0].trim() + " went out of bounds... -50 HP");
            }
            if (game.zombieFallingClip != null) game.zombieFallingClip.play();
            x = 2000 + playerIndex * 600;
            y = game.GROUND_Y - 400;
            vx = 0;
            vy = 0;
            canDoubleJump = true;

            if (game.isTrialMode) {
                game.shakeIntensity = 20;
                game.hitstopFrames = 15;
            }
        }

        if (y < game.CEILING_Y) {
            y = game.CEILING_Y;
            vy = Math.max(vy, 0);
            if (type == BirdGame3.BirdType.VULTURE) isFlying = false;
        }

        handleVerticalCollision();

        if (y > game.WORLD_HEIGHT + 300) {
            game.falls[playerIndex]++;
            health = Math.max(0, health - 50);
            if (health <= 0) {
                game.addToKillFeed(name.split(":")[0].trim() + " FELL TO THEIR DOOM!");
            } else {
                game.addToKillFeed(name.split(":")[0].trim() + " fell... but survived! -50 HP");
            }
            if (game.zombieFallingClip != null) game.zombieFallingClip.play();
            x = 1000 + playerIndex * 800;
            y = game.GROUND_Y - 300;
            vx = vy = 0;
            game.achievementProgress[7]++;
            if (game.achievementProgress[7] >= 3 && !game.achievementsUnlocked[7]) {
                game.unlockAchievement(7, "FALL GUY!");
            }
        }
    }

    private void handleVultureFeast() {
        if (type == BirdGame3.BirdType.VULTURE && health > 0) {
            for (Bird b : game.players) {
                if (b != null && b != this && b.health <= 0 && b.y > game.HEIGHT + 50 && b.y <= game.HEIGHT + 100) {
                    health = Math.min(100, health + 5);
                    game.addToKillFeed(name.split(":")[0].trim() + " FEASTS! +5 HP");
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
                health = Math.min(100, health + 40);
                game.addToKillFeed(name.split(":")[0].trim() + " grabbed HEALTH! +40 HP");
                it.remove();
            }
            case SPEED -> {
                speedMultiplier = 1.7;
                speedTimer = 480;
                game.addToKillFeed(name.split(":")[0].trim() + " got SPEED BOOST!");
                it.remove();
            }
            case RAGE -> {
                powerMultiplier = 2.0;
                rageTimer = 420;
                game.addToKillFeed(name.split(":")[0].trim() + " is ENRAGED!");
                it.remove();
            }
            case SHRINK -> {
                for (Bird b : game.players) {
                    if (b != null && b != this) {
                        b.sizeMultiplier = 0.6;
                        b.shrinkTimer = 360;
                    }
                }
                game.addToKillFeed(name.split(":")[0].trim() + " SHRANK everyone!");
                it.remove();
            }
            case NEON -> {
                speedMultiplier = 2.4;
                speedTimer = 360;
                canDoubleJump = true;
                vy = -28;
                powerMultiplier = 1.3;
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
                it.remove();
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

                it.remove();
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
                it.remove();
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

    public void draw(GraphicsContext g) {
        double drawSize = 80 * sizeMultiplier;
        boolean airborne = !isOnGround();

        drawBlockingShield(g, drawSize, airborne);
        drawTaunt(g);
        drawCooldownFlash(g);
        drawRageBuff(g, drawSize);
        drawThermalBuff(g, drawSize);
        drawNeonBuff(g, drawSize);
        drawOpiumBirdEffects(g, drawSize);
        drawTitmouseSpecial(g);
        drawEagleSoaring(g, airborne, drawSize);
        drawEagleDive(g, drawSize);
        drawRazorbillBladestorm(g, drawSize);
        drawEagleSkin(g, drawSize);
        drawGrinchhawk(g);
        drawVulture(g, drawSize);
        drawStunEffect(g);
        drawSpecialCooldown(g);
        drawLounge(g);
        drawBodyAndEyes(g, drawSize);
        drawCitySkin(g);
        drawNoirSkin(g);
        drawBeak(g, drawSize);
        drawPelican(g, drawSize);
        drawVineGrapple(g, drawSize);
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
            switch (currentTaunt) {
                case 1 -> {
                    double wingX = facingRight ? x + 90 : x - 40;
                    double wingY = y + 20;
                    g.setFill(Color.BLACK);
                    g.fillRect(wingX - 10, wingY, 60, 15);
                    g.fillOval(wingX + 40, wingY - 20, 30, 50);
                    g.setFill(Color.WHITE);
                    g.setFont(Font.font("Arial Black", 24));
                    g.fillText("FRICK YOU!", wingX + 10, wingY - 30);
                }
                case 2 -> {
                    g.setFill(Color.RED.deriveColor(0, 1, 1, 0.6));
                    g.fillOval(x - 40, y - 60, 160, 100);
                    g.setFill(Color.WHITE);
                    g.setFont(Font.font("Arial Black", 32));
                    g.fillText("COME AT ME", x - 10, y - 10);
                }
                case 3 -> {
                    g.setFill(Color.PINK.brighter());
                    g.fillOval(x + 10, y + 50, 60, 70);
                    g.setFill(Color.WHITE);
                    g.fillOval(x + 25, y + 65, 15, 20);
                    g.fillOval(x + 45, y + 65, 15, 20);
                    g.setFill(Color.BLACK);
                    g.fillOval(x + 32, y + 75, 8, 8);
                    g.setFont(Font.font("Arial Black", 20));
                    g.fillText("KISS IT", x + 15, y + 120);
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
        if (type == BirdGame3.BirdType.OPIUMBIRD) {
            g.setFill(Color.rgb(138, 43, 226, 0.3));
            g.fillOval(x - 30, y - 40, drawSize + 60, drawSize + 80);

            g.setFill(Color.PURPLE.darker());
            for (int i = 0; i < 5; i++) {
                double offset = Math.sin((System.currentTimeMillis() / 100.0) + i) * 4;
                g.fillOval(x + 85 + offset, y + 50 + i * 12, 16, 24);
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
                g.setFill(Color.WHITE);
                g.setFont(Font.font("Arial Black", 32));
                g.fillText("LEAN", x + 10, y + 20);
            }

            if (leanCooldown > 0) {
                g.setFill(Color.PURPLE.darker());
                g.fillRoundRect(x - 10, y + 100, 100, 20, 15, 15);
                g.setFill(Color.WHITE);
                g.setFont(Font.font("Arial Black", 18));
                g.fillText("LEAN", x + 15, y + 116);
            }
        }
    }

    private void drawTitmouseSpecial(GraphicsContext g) {
        if (type == BirdGame3.BirdType.TITMOUSE) {
            g.setFill(Color.SILVER);
            g.fillOval(x + 20, y - 20, 40, 60);

            g.setFill(Color.BLACK);
            g.fillOval(x + 25, y + 15, 25, 25);
            g.fillOval(x + 45, y + 15, 25, 25);
            g.setFill(Color.WHITE);
            g.fillOval(x + 32, y + 20, 10, 10);
            g.fillOval(x + 52, y + 20, 10, 10);

            g.setFill(Color.PERU.brighter());
            g.fillOval(x + 10, y + 40, 60, 40);

            if (isZipping) {
                g.setStroke(Color.SKYBLUE.brighter());
                g.setLineWidth(8);
                g.strokeLine(x + 40, y + 40, zipTargetX + 40, zipTargetY + 40);
                g.setLineWidth(4);
                g.setStroke(Color.WHITE);
                g.strokeLine(x + 40, y + 40, zipTargetX + 40, zipTargetY + 40);
            }
        }
    }

    private void drawEagleSoaring(GraphicsContext g, boolean airborne, double drawSize) {
        if ((type == BirdGame3.BirdType.EAGLE) && (diveTimer == 0) && airborne && (vy < 2)) {
            g.setFill(Color.GOLD.deriveColor(0, 1, 1, 0.2));
            g.fillOval(x - 50, y - 50, drawSize + 100, drawSize + 100);

            if (Math.random() < 0.2) {
                game.particles.add(new Particle(x + (facingRight ? -20 : drawSize + 20), y + 40,
                        (facingRight ? 1 : -1) * (2 + Math.random() * 4),
                        (Math.random() - 0.5) * 4,
                        Color.GOLD.brighter()));
            }
        }
    }

    private void drawEagleDive(GraphicsContext g, double drawSize) {
        if (type == BirdGame3.BirdType.EAGLE && diveTimer > 0) {
            g.setFill(Color.CRIMSON.deriveColor(0, 1, 1, 0.6 + 0.4 * Math.sin(diveTimer * 0.5)));
            g.fillOval(x - 80, y - 80, drawSize + 160, drawSize + 160);

            g.setStroke(Color.ORANGERED);
            g.setLineWidth(8);
            for (int i = 1; i <= 12; i++) {
                g.strokeLine(x + 40, y + 40, x + 40 - vx * i * 3, y + 40 - vy * i * 3);
            }
            g.setLineWidth(3);
            g.setStroke(Color.YELLOW);
            for (int i = 1; i <= 8; i++) {
                g.strokeLine(x + 40, y + 40, x + 40 - vx * i * 2.5, y + 40 - vy * i * 2.5);
            }

            if (diveTimer > 70) {
                g.setFill(Color.WHITE);
                g.setFont(Font.font("Arial Black", FontWeight.BOLD, 64));
                g.setEffect(new DropShadow(20, Color.BLACK));
                g.fillText("SKREEEEEEEE!!!", x - 180, y - 60);
                g.setEffect(null);
            }

            g.setStroke(Color.YELLOW);
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
            double spin = System.currentTimeMillis() * 0.02;

            g.setStroke(Color.CYAN.brighter());
            g.setLineWidth(6);
            for (int i = 0; i < 8; i++) {
                double angle = spin + i * Math.PI / 4;
                double len = 100 + Math.sin(spin * 3) * 30;
                g.strokeLine(x + 40, y + 40, x + 40 + Math.cos(angle) * len, y + 40 + Math.sin(angle) * len);
            }

            g.setFill(Color.WHITE.deriveColor(0, 1, 1, 0.6 + 0.4 * Math.sin(spin * 10)));
            g.fillOval(x - 40, y - 40, drawSize + 80, drawSize + 80);

            if ((int) (spin * 10) % 20 < 5) {
                g.setFill(Color.CYAN.brighter());
                g.setFont(Font.font("Arial Black", FontWeight.BOLD, 48));
                g.setEffect(new Glow(1.0));
                g.fillText("SLASHING!", x - 80, y - 60);
                g.setEffect(null);
            }
        }
    }

    private void drawEagleSkin(GraphicsContext g, double drawSize) {
        if (type == BirdGame3.BirdType.EAGLE &&
                ((game.eagleSkinUnlocked && playerIndex == 0) ||
                        (game.isTrialMode && playerIndex == 3 && game.selectedMap == MapType.SKYCLIFFS))) {

            g.setFill(Color.GOLD.deriveColor(0, 1, 1, 0.5));
            g.fillOval(x - 40, y - 40, drawSize + 80, drawSize + 80);

            g.setFill(Color.GOLD.brighter());
            g.fillOval(x + 15, y - 35, 50, 70);
            g.setFill(Color.ORANGE.brighter());
            g.fillOval(x + 25, y - 45, 30, 40);

            if (Math.random() < 0.4) {
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
            g.setFill(Color.rgb(35, 15, 45));
            g.fillOval(x, y, drawSize, drawSize);

            double wingSpread = isFlying || Math.abs(vx) > 2 ? 1.4 : 1.0;
            g.setFill(Color.rgb(20, 10, 30));
            g.fillOval(x - 30 * wingSpread, y + 10, 50 * wingSpread, 90);
            g.fillOval(x + drawSize - 20 * wingSpread, y + 10, 50 * wingSpread, 90);

            g.setFill(Color.rgb(180, 30, 30));
            g.fillOval(x + 15, y + 10, 50, 55);

            g.setFill(Color.CRIMSON.darker().darker());
            g.fillOval(x + 25, y + 25, 20, 20);
            g.fillOval(x + 45, y + 25, 20, 20);
            g.setFill(Color.RED.brighter());
            g.fillOval(x + 30, y + 30, 10, 10);
            g.fillOval(x + 50, y + 30, 10, 10);

            if (carrionSwarmTimer > 0) {
                g.setFill(Color.BLACK.deriveColor(0, 1, 1, 0.6));
                g.fillOval(x - 40, y - 30, drawSize + 80, drawSize + 100);
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

            g.setFill(Color.BLACK.deriveColor(0, 1, 1, 0.8));
            g.fillRoundRect(x - 5, y + 92, 90, 14, 10, 10);

            Color fillColor = ratio > 0.66 ? Color.CRIMSON :
                    ratio > 0.33 ? Color.ORANGE : Color.CYAN.brighter();
            g.setFill(fillColor);
            g.fillRoundRect(x, y + 96, 80 * (1 - ratio), 6, 6, 6);

            g.setStroke(Color.WHITE);
            g.setLineWidth(2);
            g.strokeRoundRect(x - 5, y + 92, 90, 14, 10, 10);

            g.setFill(Color.WHITE);
            g.setFont(Font.font("Arial Black", 16));

            String cooldownText;
            if (type == BirdGame3.BirdType.VULTURE && crowSwarmCooldown > 0) {
                cooldownText = "CROWS";
            } else if (type == BirdGame3.BirdType.OPIUMBIRD && leanCooldown > 0) {
                cooldownText = "LEAN";
            } else if (type == BirdGame3.BirdType.MOCKINGBIRD && specialCooldown > 0) {
                cooldownText = "LOUNGE";
            } else if (type == BirdGame3.BirdType.PIGEON && specialCooldown > 0) {
                cooldownText = "HEAL";
            } else if (specialCooldown > 0) {
                cooldownText = (int) Math.ceil(specialCooldown / 60.0) + "s";
            } else {
                cooldownText = "";
            }
            g.fillText(cooldownText, x + 20, y + 104);
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
        boolean noirPigeon = (type == BirdGame3.BirdType.PIGEON && isNoirSkin);
        Color bodyColor = noirPigeon ? Color.rgb(18, 18, 18) : type.color;
        Color headColor = noirPigeon ? Color.rgb(42, 42, 42) : type.color.brighter();

        g.setFill(bodyColor);
        g.fillOval(x, y, drawSize, drawSize);
        g.setFill(headColor);
        g.fillOval(facingRight ? x + 50 * sizeMultiplier : x - 20 * sizeMultiplier, y + 20 * sizeMultiplier, 50 * sizeMultiplier, 40 * sizeMultiplier);
        g.setFill(Color.WHITE);
        g.fillOval(x + (facingRight ? 50 : 20) * sizeMultiplier, y + 20 * sizeMultiplier, 25 * sizeMultiplier, 25 * sizeMultiplier);
        g.setFill(noirPigeon ? Color.RED.brighter() : Color.BLACK);
        g.fillOval(x + (facingRight ? 55 : 25) * sizeMultiplier, y + 25 * sizeMultiplier, 15 * sizeMultiplier, 15 * sizeMultiplier);
    }

    private void drawCitySkin(GraphicsContext g) {
        if (type == BirdGame3.BirdType.PIGEON && isCitySkin) {
            g.setFill(Color.DARKGRAY.darker());
            g.fillRoundRect(x + 20, y - 10, 40, 20, 10, 10);
            g.fillRect(x + 10, y - 5, 60, 8);

            g.setFill(Color.WHITE);
            g.fillRect(facingRight ? x + 85 : x - 15, y + 45, 20, 4);
            g.setFill(Color.ORANGE.brighter());
            g.fillRect(facingRight ? x + 105 : x - 35, y + 45, 8, 4);

            if (Math.random() < 0.7) {
                double smokeX = facingRight ? x + 110 : x - 20;
                double smokeY = y + 40 + Math.random() * 12;
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
            g.setFill(Color.BLACK);
            g.fillRoundRect(x + 16, y - 12, 48, 18, 10, 10);
            g.fillRect(x + 4, y - 6, 72, 8);

            g.setFill(Color.BLACK.deriveColor(0, 1, 0.8, 1));
            g.fillRoundRect(x + 18, y + 52, 44, 22, 10, 10);
            g.setStroke(Color.RED.brighter());
            g.setLineWidth(3);
            g.strokeLine(x + 22, y + 58, x + 58, y + 70);

            if (Math.random() < 0.45) {
                double smokeX = facingRight ? x + 98 : x - 10;
                double smokeY = y + 42 + Math.random() * 10;
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

    private void drawBeak(GraphicsContext g, double drawSize) {
        boolean isAttacking = attackAnimationTimer > 0;
        double openAmount = isAttacking ? (16 + Math.sin(attackAnimationTimer * 0.7) * 10) * sizeMultiplier : 3 * sizeMultiplier;
        double beakBaseY = y + 45 * sizeMultiplier;
        double beakLength = 28 * sizeMultiplier;

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

        int flashFrame = (playerIndex == 3 && game.isTrialMode) ? 24 : 12;
        if (attackAnimationTimer == flashFrame) {
            double flashOpacity = (playerIndex == 3 && game.isTrialMode) ? 0.9 : 0.7;
            double flashSize = (playerIndex == 3 && game.isTrialMode) ? 60 : 36;
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

