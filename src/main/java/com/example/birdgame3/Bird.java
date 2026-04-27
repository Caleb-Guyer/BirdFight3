package com.example.birdgame3;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.effect.DropShadow;
import javafx.scene.effect.Glow;
import javafx.scene.image.Image;
import javafx.scene.input.KeyCode;
import javafx.scene.paint.Color;
import javafx.scene.shape.ArcType;
import javafx.scene.shape.StrokeLineCap;
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

    private enum GrabThrowDirection {
        NONE,
        FORWARD,
        BACK,
        UP,
        DOWN
    }

    private enum NormalAttackVariant {
        NEUTRAL,
        SIDE_TILT,
        UP_TILT,
        DOWN_TILT,
        SIDE_SMASH,
        UP_SMASH,
        DOWN_SMASH,
        NEUTRAL_AIR,
        FORWARD_AIR,
        BACK_AIR,
        UP_AIR,
        DOWN_AIR;

        boolean usesDownInput() {
            return this == DOWN_TILT || this == DOWN_SMASH || this == DOWN_AIR;
        }

    }

    private enum PigeonSpecialVariant {
        NEUTRAL,
        SIDE,
        UP,
        DOWN
    }

    private enum RaptorSpecialVariant {
        NEUTRAL,
        SIDE,
        UP,
        DOWN
    }

    private record NormalAttackProfile(
            double horizontalReach,
            double verticalReach,
            double centerOffsetX,
            double centerOffsetY,
            double damageMultiplier,
            double knockbackMultiplier,
            double horizontalLaunchScale,
            double verticalLaunchScale,
            double meteorVerticalLaunchScale,
            int cooldownFrames,
            int animationFrames,
            int landingLagFrames
    ) {
        double verticalLaunchScaleFor(double targetCenterY, double attackCenterY) {
            if (meteorVerticalLaunchScale >= 0.0 || targetCenterY < attackCenterY + 6.0) {
                return verticalLaunchScale;
            }
            return meteorVerticalLaunchScale;
        }
    }

    private record AttackVisualPose(
            double translateX,
            double translateY,
            double bodyRotationDegrees,
            double aimAngleRadians,
            double headReachBonus,
            double headLift,
            double beakLengthBonus,
            double beakOpenScale,
            double spriteRotationDegrees,
            double spriteScaleX,
            double spriteScaleY
    ) {
    }

    private record HeadPose(double centerX, double centerY, double aimAngleRadians) {
    }

    // Reference to main game instance
    private final BirdGame3 game;

    // === CORE PROPERTIES ===
    public double x, y, vx = 0, vy = 0;
    public BirdGame3.BirdType type;
    public boolean facingRight = true;
    public int playerIndex;
    public static final double STARTING_HEALTH = 200.0;
    public double health = STARTING_HEALTH;
    private double smashDamage = 0.0;
    public String name;
    public double stunTime = 0;
    public int specialCooldown = 0;
    public int specialMaxCooldown = 120;
    public int attackCooldown = 0;
    public int attackAnimationTimer = 0;
    private int attackChargeFrames = 0;
    private int pendingGroundAttackFrames = 0;
    private NormalAttackVariant pendingGroundAttackVariant = NormalAttackVariant.NEUTRAL;
    private NormalAttackVariant chargingAttackVariant = NormalAttackVariant.NEUTRAL;
    private NormalAttackVariant activeAttackVariant = NormalAttackVariant.NEUTRAL;
    private boolean attackHeldLastFrame = false;
    private boolean specialHeldLastFrame = false;
    private boolean aerialAttackActive = false;
    private int aerialAttackTotalFrames = 0;
    private int activeAerialLandingLagFrames = AERIAL_LANDING_LAG_FRAMES;
    private int landingLagTimer = 0;
    public boolean canDoubleJump = true;
    private int jumpSquatTimer = 0;
    private boolean shortHopQueued = false;
    private boolean jumpHeldLastFrame = false;
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
    public boolean isStormSkin = false;
    public boolean isSunflareSkin = false;
    public boolean isGlacierSkin = false;
    public boolean isTideSkin = false;
    public boolean isNullRockSkin = false;
    public boolean isEclipseSkin = false;
    public boolean isUmbraSkin = false;
    public boolean isResonanceSkin = false;
    public boolean isIroncladSkin = false;
    public boolean isSunforgeSkin = false;
    public boolean isPhotoEagleSkin = false;
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
    private int batRehangCooldownTimer = 0;
    private boolean ledgeHanging = false;
    private Platform ledgePlatform = null;
    private boolean ledgeGrabOnRightSide = false;
    private int ledgeLockTimer = 0;
    private int ledgeRegrabCooldownTimer = 0;
    private int ledgeInvulnerabilityTimer = 0;
    private int ledgeHangFrames = 0;

    public boolean isBlocking = false;
    public int blockCooldown = 0;
    private double shieldHealth = SHIELD_MAX_HEALTH;
    private int shieldStunFrames = 0;
    private int parryWindowFrames = 0;
    private double shieldHoldVisual = 0.0;
    private AttackVisualPose displayPose = null;
    private DodgeType dodgeType = DodgeType.NONE;
    private int dodgeTimer = 0;
    private int dodgeInvulnerabilityTimer = 0;
    private int dodgeCooldown = 0;
    private int dodgeDirection = 0;
    private boolean airDodgeAvailable = true;
    private boolean blockHeldLastFrame = false;
    private int techBufferTimer = 0;
    private int knockdownTimer = 0;
    private boolean leftHeldLastFrame = false;
    private boolean rightHeldLastFrame = false;
    private int grabCooldown = 0;
    private boolean grabHeldLastFrame = false;
    private Bird grabbedTarget = null;
    private Bird grabbedBy = null;
    private int grabHoldTimer = 0;
    private int grabThrowLockTimer = 0;

    // === VINE SWINGING ===
    SwingingVine attachedVine = null;
    public boolean onVine = false;
    int vineRideFrames = 0;

    // === POWER-UP BUFFS ===
    public double speedMultiplier = 1.0;
    public double powerMultiplier = 1.0;
    public double sizeMultiplier;
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
    private double ultimateMeter = 0.0;
    private int ultimateFxTimer = 0;
    private int roadrunnerSandstormTimer = 0;
    private int roadrunnerSandGustTimer = 0;
    private final int[] roadrunnerSandHitCooldown = new int[4];
    private int pigeonFeatherBurstTimer = 0;
    private boolean pigeonFeatherBurstUltimate = false;
    private int pigeonRushTimer = 0;
    private boolean pigeonRushGrounded = false;
    private boolean pigeonRushUltimate = false;
    private final boolean[] pigeonRushHit = new boolean[4];
    private int pigeonFlutterTimer = 0;
    private boolean pigeonFlutterUltimate = false;
    private final boolean[] pigeonFlutterHit = new boolean[4];
    private int pigeonScavengeTimer = 0;
    private boolean pigeonScavengeAirborne = false;
    private boolean pigeonScavengeUltimate = false;
    private boolean pigeonScavengeResolved = false;
    private boolean pigeonUpSpecialUsed = false;
    private int raptorCryTimer = 0;
    private boolean raptorCryUltimate = false;
    private int raptorRushTimer = 0;
    private boolean raptorRushUltimate = false;
    private boolean raptorRushGrounded = false;
    private int raptorRushDirection = 1;
    private final boolean[] raptorRushHit = new boolean[4];
    private int raptorClimbTimer = 0;
    private boolean raptorClimbUltimate = false;
    private int raptorClimbDirection = 1;
    private final boolean[] raptorClimbHit = new boolean[4];
    private int raptorCryReuseTimer = 0;
    private int raptorRushReuseTimer = 0;
    private boolean raptorUpSpecialUsed = false;

    // === NECTAR BOOST (Jungle) ===
    public double speedBoostTimer = 0;
    public double hoverRegenTimer = 0;
    public double hoverRegenMultiplier = 1.0;

    // === MOCKINGBIRD LOUNGE ===
    public int loungeHealth = 0;
    private static final int LOUNGE_MAX_HEALTH = 100;
    private static final double LOUNGE_HEAL_PER_SECOND = 12.0;
    private int loungeMaxHealth = LOUNGE_MAX_HEALTH;
    public int loungeDamageFlash = 0;
    private boolean loungeRoyal = false;

    // === VINE GRAPPLE ===
    private int grappleTimer = 0;
    private int grappleUses = 0;
    private boolean isGrappling = false;
    private double grappleTargetX, grappleTargetY;

    private boolean enlargedByPlunge = false;
    private static final double LIMITED_FLIGHT_MAX = 34.0; // ~0.55s at 60fps
    private double limitedFlightFuel = LIMITED_FLIGHT_MAX;
    private static final double FAST_FALL_ACCEL = 1.6;
    private static final double FAST_FALL_MAX = 22.0;
    private static final double FAST_FALL_UPDRAFT_ACCEL = 0.35;
    private static final double DOWN_WIND_DAMPING = 0.85;
    private static final double DOCK_WATER_GRAVITY_SCALE = 0.08;
    private static final double DOCK_WATER_BUOYANCY = 0.92;
    private static final double DOCK_WATER_RISE_ACCEL = 1.85;
    private static final double DOCK_WATER_DIVE_ACCEL = 0.72;
    private static final double DOCK_WATER_SWIM_DRAG_X = 0.95;
    private static final double DOCK_WATER_SWIM_DRAG_Y = 0.93;
    private static final double DOCK_WATER_MAX_RISE = -13.2;
    private static final double DOCK_WATER_MAX_SINK = 8.8;
    private static final double DOCK_WATER_SURFACE_BREACH_WINDOW = 96.0;
    private static final double DOCK_WATER_SURFACE_BREACH_BOOST = 12.4;
    private static final double ULTIMATE_MAX = 100.0;
    private static final double ULTIMATE_GAIN_DEALT = 0.35;
    private static final double ULTIMATE_GAIN_TAKEN = 0.45;
    private static final int ULTIMATE_FX_FRAMES = 24;
    private static final int ROADRUNNER_SANDSTORM_FRAMES = 540;
    private static final int ROADRUNNER_GUST_INTERVAL = 12;
    private static final int ROADRUNNER_GUST_HIT_COOLDOWN = 24;
    private static final double ROADRUNNER_SANDSTORM_FLY_LIFT = 1.1;
    private static final double ROADRUNNER_SANDSTORM_SPEED_SCALE = 1.38;
    private static final double ROADRUNNER_SANDSTORM_GUST_RADIUS = 340.0;
    private static final int PIGEON_NEUTRAL_BURST_FRAMES = 12;
    private static final int PIGEON_NEUTRAL_COOLDOWN_FRAMES = 34;
    private static final int PIGEON_RUSH_GROUND_FRAMES = 20;
    private static final int PIGEON_RUSH_AIR_FRAMES = 18;
    private static final int PIGEON_FLUTTER_FRAMES = 15;
    private static final int PIGEON_FLUTTER_ULTIMATE_FRAMES = 18;
    private static final int PIGEON_SCAVENGE_GROUND_FRAMES = 162;
    private static final int PIGEON_SCAVENGE_AIR_FRAMES = 14;
    private static final int EAGLE_CRY_FRAMES = 16;
    private static final int EAGLE_CRY_ULTIMATE_FRAMES = 20;
    private static final int FALCON_CRY_FRAMES = 13;
    private static final int FALCON_CRY_ULTIMATE_FRAMES = 16;
    private static final int EAGLE_RUSH_GROUND_FRAMES = 18;
    private static final int EAGLE_RUSH_AIR_FRAMES = 16;
    private static final int FALCON_RUSH_GROUND_FRAMES = 16;
    private static final int FALCON_RUSH_AIR_FRAMES = 14;
    private static final int EAGLE_CLIMB_FRAMES = 18;
    private static final int EAGLE_CLIMB_ULTIMATE_FRAMES = 22;
    private static final int FALCON_CLIMB_FRAMES = 15;
    private static final int FALCON_CLIMB_ULTIMATE_FRAMES = 18;
    private static final int EAGLE_DIVE_FRAMES = 120;
    private static final int EAGLE_DIVE_ULTIMATE_FRAMES = 160;
    private static final int FALCON_DIVE_FRAMES = 92;
    private static final int FALCON_DIVE_ULTIMATE_FRAMES = 130;
    private static final int EAGLE_DIVE_GROUND_STARTUP_FRAMES = 10;
    private static final int EAGLE_DIVE_GROUND_ULTIMATE_STARTUP_FRAMES = 12;
    private static final int FALCON_DIVE_GROUND_STARTUP_FRAMES = 8;
    private static final int FALCON_DIVE_GROUND_ULTIMATE_STARTUP_FRAMES = 10;
    private static final int MAX_ATTACK_CHARGE_FRAMES = 60;
    private static final int GROUND_SMASH_HOLD_THRESHOLD_FRAMES = 7;
    private static final double CHARGED_ATTACK_DAMAGE_BONUS = 0.35;
    private static final double CHARGED_ATTACK_KNOCKBACK_BONUS = 5.0;
    private static final double CHARGED_ATTACK_VERTICAL_BONUS = 1.8;
    private static final double ATTACK_HORIZONTAL_KNOCKBACK_SCALE = 1.3;
    private static final double ATTACK_VERTICAL_KNOCKBACK_SCALE = 0.52;
    private static final double SMASH_HORIZONTAL_LAUNCH_SCALE = 1.08;
    private static final double SMASH_VERTICAL_LAUNCH_SCALE = 0.84;
    private static final double SMASH_MIN_UPWARD_LAUNCH_SCALE = 2.8;
    private static final double SMASH_DI_MAX_ANGLE_RADIANS = Math.toRadians(18.0);
    private static final double LEDGE_GRAB_HORIZONTAL_REACH = 34.0;
    private static final double LEDGE_GRAB_VERTICAL_ABOVE = 18.0;
    private static final double LEDGE_GRAB_VERTICAL_BELOW = 46.0;
    private static final double LEDGE_HANG_TOP_OFFSET_RATIO = 0.36;
    private static final int LEDGE_LOCK_FRAMES = 10;
    private static final int LEDGE_REGRAB_COOLDOWN_FRAMES = 24;
    private static final int LEDGE_GRAB_INTANGIBILITY_FRAMES = 18;
    private static final int LEDGE_CLIMB_INTANGIBILITY_FRAMES = 8;
    private static final int LEDGE_ROLL_INTANGIBILITY_FRAMES = 14;
    private static final int SMASH_KO_CREDIT_FRAMES = 240;
    private static final double SHIELD_MAX_HEALTH = 60.0;
    private static final double SHIELD_DAMAGE_BASE = 1.8;
    private static final double SHIELD_DAMAGE_SCALE = 0.78;
    private static final double SHIELD_REGEN_PER_FRAME = 0.12;
    private static final int GRAB_WHIFF_COOLDOWN_FRAMES = 20;
    private static final int GRAB_RELEASE_COOLDOWN_FRAMES = 14;
    private static final int GRAB_THROW_COOLDOWN_FRAMES = 22;
    private static final int GRAB_HOLD_FRAMES = 38;
    private static final int GRAB_THROW_LOCK_FRAMES = 8;
    private static final double GRAB_FORWARD_REACH = 76.0;
    private static final double GRAB_BACK_REACH = 24.0;
    private static final double GRAB_VERTICAL_REACH = 44.0;
    private static final double GRAB_HOLD_X_PADDING = 6.0;
    private static final int THROW_FORWARD_DAMAGE = 8;
    private static final int THROW_BACK_DAMAGE = 9;
    private static final int THROW_UP_DAMAGE = 7;
    private static final int THROW_DOWN_DAMAGE = 6;
    private static final int SHIELD_PARRY_STARTUP_FRAMES = 3;
    private static final int SHIELD_PARRY_ATTACKER_STUN_FRAMES = 28;
    private static final int SHIELD_PARRY_RELEASE_FRAMES = 5;
    private static final int SHIELD_PARRY_HITSTOP_FRAMES = 5;
    private static final double SHIELD_PUSHBACK_SCALE = 0.16;
    private static final int SHIELD_STUN_BASE_FRAMES = 6;
    private static final double SHIELD_STUN_DAMAGE_SCALE = 0.30;
    private static final double SHIELD_STUN_LOW_HEALTH_BONUS = 5.0;
    private static final int SHIELD_DROP_COOLDOWN_FRAMES = 10;
    private static final int SHIELD_BREAK_STUN_FRAMES = 80;
    private static final int SHIELD_BREAK_COOLDOWN_FRAMES = 96;
    private static final double SHIELD_MIN_VISUAL_SCALE = 0.52;
    private static final double SHIELD_HOLD_VISUAL_BUILD_PER_FRAME = 0.0085;
    private static final double SHIELD_HOLD_VISUAL_RELEASE_PER_FRAME = 0.05;
    private static final double SHIELD_HOLD_VISUAL_SHRINK = 0.26;
    private static final double VISUAL_POSE_IDLE_BLEND_PER_FRAME = 0.24;
    private static final double VISUAL_POSE_AIR_BLEND_PER_FRAME = 0.34;
    private static final double VISUAL_POSE_ACTION_BLEND_PER_FRAME = 0.48;
    private static final double VISUAL_POSE_DODGE_BLEND_PER_FRAME = 0.82;
    private static final int JUMP_SQUAT_FRAMES = 3;
    private static final double SHORT_HOP_MULTIPLIER = 0.65;
    private static final int AERIAL_LANDING_LAG_FRAMES = 7;
    private static final int AERIAL_AUTO_CANCEL_STARTUP_FRAMES = 2;
    private static final int AERIAL_AUTO_CANCEL_LATE_FRAMES = 3;
    private static final int TECH_INPUT_BUFFER_FRAMES = 10;
    private static final double GROUND_TECH_MIN_IMPACT_SPEED = 7.0;
    private static final double WALL_TECH_MIN_IMPACT_SPEED = 6.5;
    private static final int TECH_INVULNERABILITY_FRAMES = 12;
    private static final int MISSED_TECH_KNOCKDOWN_FRAMES = 34;
    private static final double WALL_BOUNCE_SPEED_SCALE = 0.55;
    private static final double WALL_BOUNCE_VERTICAL_DAMPING = 0.88;
    private static final int SPOT_DODGE_FRAMES = 18;
    private static final int SPOT_DODGE_INVULNERABILITY_FRAMES = 10;
    private static final int ROLL_DODGE_FRAMES = 24;
    private static final int ROLL_DODGE_INVULNERABILITY_FRAMES = 14;
    private static final double ROLL_DODGE_SPEED = 10.5;
    private static final int AIR_DODGE_FRAMES = 22;
    private static final int AIR_DODGE_INVULNERABILITY_FRAMES = 16;
    private static final double AIR_DODGE_SPEED = 8.6;
    private static final double AIR_DODGE_STALL_VELOCITY = 1.25;
    private static final int DODGE_COOLDOWN_FRAMES = 28;
    private static final double SMASH_TOP_BLAST_Y = BirdGame3.CEILING_Y - 220.0;
    private static Image photoEagleIdleSprite;
    private static Image photoEagleAttackSprite;
    private static Image photoEagleFlapSprite;
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
    private static final double BASE_BODY_SIZE = 80.0;
    private static final double NULL_ROCK_VISIBLE_VOID_MARGIN = 80.0;
    private static final double NULL_ROCK_COMBAT_HALF_WIDTH = 58.0;
    private static final double NULL_ROCK_COMBAT_HALF_HEIGHT = 60.0;
    private static final double NULL_ROCK_TRUE_FORM_THRESHOLD = 0.50;
    private static final double NULL_ROCK_TRUE_FORM_SIZE_SCALE = 1.18;
    private static final double NULL_ROCK_TRUE_FORM_POWER_SCALE = 1.34;
    private static final double NULL_ROCK_TRUE_FORM_SPEED_SCALE = 1.18;
    private static final double[] NULL_ROCK_PHASE_THRESHOLDS = {0.84, 0.66, NULL_ROCK_TRUE_FORM_THRESHOLD, 0.24};
    private static final int NULL_ROCK_PHASE_INVULN_FRAMES = 135;
    private static final int TRUE_NULL_ROCK_ASCENSION_INVULN_FRAMES = 220;
    private static final int BAT_REHANG_COOLDOWN_FRAMES = 14;
    private int nullRockInvincibilityTimer = 0;
    private int nullRockPhaseIndex = 0;
    private int nullRockShieldFxCooldown = 0;
    private boolean trueNullRockForm = false;
    private int recentSmashAttackerIndex = -1;
    private int recentSmashAttackerFrames = 0;
    private double pendingSmashLaunchScale = 1.0;

    private final Random random = new Random();

    private enum DodgeType {
        NONE,
        SPOT,
        ROLL,
        AIR
    }

    private record ShieldHitResult(boolean blocked, boolean parried) {
        private static final ShieldHitResult NONE = new ShieldHitResult(false, false);
        private static final ShieldHitResult BLOCKED = new ShieldHitResult(true, false);
        private static final ShieldHitResult PARRIED = new ShieldHitResult(true, true);
    }

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
        this.y = BirdGame3.GROUND_Y - 200;
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

    boolean isNullRockForm() {
        return type == BirdGame3.BirdType.VULTURE && isNullRockSkin;
    }

    boolean isTrueNullRockForm() {
        return isNullRockForm() && trueNullRockForm;
    }

    private boolean hasNullRockInvulnerability() {
        return isNullRockForm() && nullRockInvincibilityTimer > 0;
    }

    private boolean hasDodgeInvulnerability() {
        return dodgeInvulnerabilityTimer > 0;
    }

    boolean isCombatInvulnerable() {
        return ledgeInvulnerabilityTimer > 0 || hasNullRockInvulnerability() || hasDodgeInvulnerability();
    }

    private boolean isStunImmune() {
        return isNullRockForm();
    }

    private boolean isShrinkImmune() {
        return isNullRockForm();
    }

    void applyStun(double frames) {
        if (frames <= 0) return;
        if (isStunImmune()) {
            spawnNullRockShieldBurst();
            return;
        }
        interruptGrabStateOnHit();
        clearAerialAttackState();
        stunTime = Math.max(stunTime, frames);
    }

    void applyShrinkEffect() {
        if (isShrinkImmune()) {
            spawnNullRockShieldBurst();
            return;
        }
        sizeMultiplier = baseSizeMultiplier * 0.6;
        shrinkTimer = Math.max(shrinkTimer, 360);
    }

    private double bodyWidth() {
        return BASE_BODY_SIZE * sizeMultiplier;
    }

    private double bodyHeight() {
        return BASE_BODY_SIZE * sizeMultiplier;
    }

    private double bodyCenterX() {
        return x + bodyWidth() / 2.0;
    }

    private double bodyCenterY() {
        return y + bodyHeight() / 2.0;
    }

    private double bodyBottomY() {
        return y + bodyHeight();
    }

    private double combatHalfWidth() {
        return isNullRockForm() ? NULL_ROCK_COMBAT_HALF_WIDTH * sizeMultiplier : bodyWidth() / 2.0;
    }

    private double combatHalfHeight() {
        return isNullRockForm() ? NULL_ROCK_COMBAT_HALF_HEIGHT * sizeMultiplier : bodyHeight() / 2.0;
    }

    private double combatRadius() {
        return Math.max(combatHalfWidth(), combatHalfHeight()) * 0.82;
    }

    private boolean overlapsPowerUp(PowerUp powerUp) {
        double pickupHalfSize = BASE_BODY_SIZE / 2.0;
        double dx = Math.abs(powerUp.x - bodyCenterX());
        double dy = Math.abs(powerUp.y - bodyCenterY());
        return dx <= combatHalfWidth() + pickupHalfSize
                && dy <= combatHalfHeight() + pickupHalfSize;
    }

    private double combatDistanceTo(Bird other) {
        return Math.hypot(other.bodyCenterX() - bodyCenterX(), other.bodyCenterY() - bodyCenterY());
    }

    private boolean canStandInVoid() {
        return isNullRockForm() && isVoidMap();
    }

    private boolean usesIslandBounds() {
        return game.selectedMap == MapType.BATTLEFIELD
                || game.selectedMap == MapType.BEACON_CROWN;
    }

    private boolean isInDockWater() {
        return (game.selectedMap == MapType.DOCK || game.selectedMap == MapType.DESERT)
                && game.isDockWaterAt(bodyCenterX(), bodyCenterY() + combatHalfHeight() * 0.25);
    }

    private boolean isFullySubmergedInDockWater() {
        return (game.selectedMap == MapType.DOCK || game.selectedMap == MapType.DESERT)
                && game.isDockWaterAt(bodyCenterX(), bodyCenterY());
    }

    private boolean isDockDrownDepthReached() {
        return game.selectedMap == MapType.DOCK
                && bodyBottomY() >= game.dockDrownDepthY();
    }

    private boolean hasSolidGroundFloorUnderBody() {
        if (game.selectedMap == MapType.DOCK || game.selectedMap == MapType.DESERT) {
            return !game.isDockWaterAt(bodyCenterX(), BirdGame3.GROUND_Y + 8);
        }
        return !usesIslandBounds();
    }

    private double voidStandFloorY() {
        double visibleVoidFloorY = BirdGame3.WORLD_HEIGHT - NULL_ROCK_VISIBLE_VOID_MARGIN;
        return Math.max(game.battlefieldVoidFloorY(), visibleVoidFloorY);
    }

    public boolean isOnGround() {
        double bottom = bodyBottomY();
        if (hasSolidGroundFloorUnderBody() && bottom >= BirdGame3.GROUND_Y) return true;
        if (canStandInVoid() && bottom >= voidStandFloorY()) return true;
        for (Platform p : game.platforms) {
            boolean isCaveCeiling = game.selectedMap == MapType.CAVE &&
                    p.y <= 1 && p.h >= 60 && p.w >= BirdGame3.WORLD_WIDTH - 10;
            if (isCaveCeiling) continue;
            if (bodyCenterX() >= p.x && bodyCenterX() <= p.x + p.w &&
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
                double healthBefore = health;
                heal(LOUNGE_HEAL_PER_SECOND / 60.0);
                game.recordLoungeHealing(this, health - healthBefore);
            }
        }
    }

    private void handleVerticalCollision(boolean wasAirborne) {
        if (onVine || batHanging || ledgeHanging) return;

        boolean hit = false;
        double newY = y;
        double impactVy = vy;

        for (Platform p : game.platforms) {
            boolean isCaveCeiling = game.selectedMap == MapType.CAVE &&
                    p.y <= 1 && p.h >= 60 && p.w >= BirdGame3.WORLD_WIDTH - 10;

            if (isCaveCeiling) {
                // Solid cave ceiling: block upward movement from below but never allow standing on top.
                if (bodyCenterX() >= p.x && bodyCenterX() <= p.x + p.w &&
                        y < p.y + p.h && y > p.y - 50 && vy < 0) {
                    y = p.y + p.h + 0.5;
                    vy = 0;
                }
                continue;
            }

            // Land only when descending onto the top surface to avoid snapping onto platforms from below.
            if (bodyCenterX() >= p.x && bodyCenterX() <= p.x + p.w &&
                    bodyBottomY() > p.y && y < p.y + p.h &&
                    vy >= 0 && y <= p.y) {
                newY = p.y - bodyHeight();
                hit = true;
                break;
            }
        }

        if (!hit && canStandInVoid() && bodyBottomY() > voidStandFloorY()) {
            newY = voidStandFloorY() - bodyHeight();
            hit = true;
        }

        if (!hit && hasSolidGroundFloorUnderBody() && y + 80 * sizeMultiplier > BirdGame3.GROUND_Y) {
            newY = BirdGame3.GROUND_Y - bodyHeight();
            hit = true;
        }

        if (hit) {
            y = newY;
            if (vy > 0) vy = 0;
            canDoubleJump = true;
            refreshAirDodge();
            if (wasAirborne) {
                if (!resolveGroundTechOrKnockdown(impactVy)) {
                    resolveAerialLandingRecovery();
                }
            }

            // === TURKEY GROUND POUND ===
            if (type == BirdGame3.BirdType.TURKEY && isGroundPounding) {
                handleTurkeyGroundPound();
            }
        }
    }

    private void snapToLedge() {
        if (ledgePlatform == null) {
            return;
        }
        double edgeX = ledgeGrabOnRightSide ? ledgePlatform.x + ledgePlatform.w : ledgePlatform.x;
        double hangInset = bodyWidth() * 0.32;
        x = ledgeGrabOnRightSide ? edgeX - hangInset : edgeX - bodyWidth() + hangInset;
        y = ledgePlatform.y - bodyHeight() * LEDGE_HANG_TOP_OFFSET_RATIO;
        vx = 0;
        vy = 0;
        facingRight = !ledgeGrabOnRightSide;
    }

    private void beginLedgeHang(Platform platform, boolean onRightSide) {
        ledgeHanging = true;
        ledgePlatform = platform;
        ledgeGrabOnRightSide = onRightSide;
        ledgeLockTimer = LEDGE_LOCK_FRAMES;
        ledgeHangFrames = 0;
        ledgeInvulnerabilityTimer = Math.max(ledgeInvulnerabilityTimer, LEDGE_GRAB_INTANGIBILITY_FRAMES);
        attackAnimationTimer = 0;
        clearAerialAttackState();
        landingLagTimer = 0;
        canDoubleJump = true;
        refreshAirDodge();
        snapToLedge();
    }

    private void clearLedgeHangState(int regrabCooldownFrames) {
        ledgeHanging = false;
        ledgePlatform = null;
        ledgeGrabOnRightSide = false;
        ledgeLockTimer = 0;
        ledgeHangFrames = 0;
        if (regrabCooldownFrames > 0) {
            ledgeRegrabCooldownTimer = Math.max(ledgeRegrabCooldownTimer, regrabCooldownFrames);
        }
    }

    private int ledgeStageDirection() {
        return ledgeGrabOnRightSide ? -1 : 1;
    }

    private boolean ledgeTowardStagePressed() {
        return ledgeStageDirection() > 0 ? rightPressed() : leftPressed();
    }

    private boolean ledgeAwayFromStagePressed() {
        return ledgeStageDirection() > 0 ? leftPressed() : rightPressed();
    }

    private void dropFromLedge() {
        clearLedgeHangState(LEDGE_REGRAB_COOLDOWN_FRAMES);
        y += Math.max(10.0, bodyHeight() * 0.18);
        vy = Math.max(vy, 3.8);
    }

    private void jumpFromLedge() {
        int dir = ledgeStageDirection();
        clearLedgeHangState(LEDGE_REGRAB_COOLDOWN_FRAMES);
        x += dir * Math.max(4.0, bodyWidth() * 0.08);
        vx = dir * Math.max(6.2, type.speed * speedMultiplier * 1.55);
        vy = -Math.max(9.5, type.jumpHeight * 0.82);
        canDoubleJump = true;
        facingRight = dir > 0;
        ledgeInvulnerabilityTimer = Math.max(ledgeInvulnerabilityTimer, 6);
    }

    private void climbFromLedge(boolean roll) {
        Platform platform = ledgePlatform;
        boolean onRightSide = ledgeGrabOnRightSide;
        int dir = ledgeStageDirection();
        clearLedgeHangState(LEDGE_REGRAB_COOLDOWN_FRAMES);
        if (platform == null) {
            return;
        }
        double desiredInset = roll ? Math.max(48.0, bodyWidth() * 0.72) : Math.max(12.0, bodyWidth() * 0.16);
        double minX = platform.x;
        double maxX = platform.x + platform.w - bodyWidth();
        double desiredX = onRightSide
                ? platform.x + platform.w - bodyWidth() - desiredInset
                : platform.x + desiredInset;
        x = Math.clamp(desiredX, minX, maxX);
        y = platform.y - bodyHeight();
        vx = dir * (roll ? 7.8 : 2.8);
        vy = 0.0;
        canDoubleJump = true;
        facingRight = dir > 0;
        ledgeInvulnerabilityTimer = Math.max(ledgeInvulnerabilityTimer,
                roll ? LEDGE_ROLL_INTANGIBILITY_FRAMES : LEDGE_CLIMB_INTANGIBILITY_FRAMES);
    }

    private boolean handleLedgeHanging(boolean stunned) {
        if (!ledgeHanging) {
            return false;
        }
        if (ledgePlatform == null || !game.platforms.contains(ledgePlatform) || isInDockWater()) {
            clearLedgeHangState(LEDGE_REGRAB_COOLDOWN_FRAMES);
            return false;
        }

        snapToLedge();
        canDoubleJump = true;
        ledgeHangFrames++;

        if (game.isAI[playerIndex]) {
            if (!stunned && ledgeLockTimer <= 0 && ledgeHangFrames >= 12) {
                climbFromLedge(false);
                return false;
            }
            return true;
        }

        if (stunned || ledgeLockTimer > 0) {
            return true;
        }
        if (blockPressed()) {
            dropFromLedge();
            return false;
        }
        if (jumpPressed()) {
            jumpFromLedge();
            game.playSwingSfx();
            return false;
        }
        if (ledgeAwayFromStagePressed()) {
            climbFromLedge(true);
            return false;
        }
        if (ledgeTowardStagePressed() || attackPressed() || specialPressed()) {
            climbFromLedge(false);
            return false;
        }
        return true;
    }

    private boolean tryGrabUniversalLedge(double prevX, boolean inDockWater) {
        // Restrict ledge grabs to the main stage platform only (prevents grabbing small/aux platforms).
        if (ledgeHanging || batHanging || onVine || isGrappling || inDockWater || health <= 0) {
            return false;
        }
        if (ledgeRegrabCooldownTimer > 0 || vy < -10.0) {
            return false;
        }

        double currentCenterX = bodyCenterX();
        double currentTop = y;
        double currentBottom = bodyBottomY();
        double prevCenterX = prevX + bodyWidth() / 2.0;

        Platform mainStage = findAIMainStagePlatform();
        if (mainStage == null) return false;

        // Only allow grabs on the edges of the main stage
        double lipY = mainStage.y;
        if (currentTop < lipY - LEDGE_GRAB_VERTICAL_ABOVE || currentTop > lipY + LEDGE_GRAB_VERTICAL_BELOW) {
            return false;
        }
        if (currentBottom < lipY + 8.0) {
            return false;
        }

        double bestDistance = Double.MAX_VALUE;
        Platform bestPlatform = null;
        boolean bestOnRightSide = false;

        double leftEdge = mainStage.x;
        if (prevCenterX <= leftEdge + 2.0
                && currentCenterX >= leftEdge - LEDGE_GRAB_HORIZONTAL_REACH
                && currentCenterX <= leftEdge + LEDGE_GRAB_HORIZONTAL_REACH * 0.35) {
            double distance = Math.abs(currentCenterX - leftEdge) + Math.abs(currentTop - lipY);
            if (distance < bestDistance) {
                bestDistance = distance;
                bestPlatform = mainStage;
            }
        }

        double rightEdge = mainStage.x + mainStage.w;
        if (prevCenterX >= rightEdge - 2.0
                && currentCenterX <= rightEdge + LEDGE_GRAB_HORIZONTAL_REACH
                && currentCenterX >= rightEdge - LEDGE_GRAB_HORIZONTAL_REACH * 0.35) {
            double distance = Math.abs(currentCenterX - rightEdge) + Math.abs(currentTop - lipY);
            if (distance < bestDistance) {
                bestPlatform = mainStage;
                bestOnRightSide = true;
            }
        }

        if (bestPlatform == null) {
            return false;
        }
        beginLedgeHang(bestPlatform, bestOnRightSide);
        return true;
    }

    private Platform resolveClosestLedgePlatformForState() {
        // Prefer the main stage platform for resolving ledge hang state.
        Platform mainStage = findAIMainStagePlatform();
        if (mainStage == null) return null;
        double expectedEdgeX = x + bodyWidth() * (ledgeGrabOnRightSide ? 0.32 : 0.68);
        double expectedLipY = y + bodyHeight() * LEDGE_HANG_TOP_OFFSET_RATIO;
        double edgeX = ledgeGrabOnRightSide ? mainStage.x + mainStage.w : mainStage.x;
        double distance = Math.abs(edgeX - expectedEdgeX) + Math.abs(mainStage.y - expectedLipY);
        return distance <= 80.0 ? mainStage : null;
    }

    private void handleTurkeyGroundPound() {
        isGroundPounding = false;
        game.groundPounds[playerIndex]++;
        game.checkAchievements(this);
        game.shakeIntensity = 22;
        game.hitstopFrames = 15;
        game.addToKillFeed(shortName() + " SLAMMED THE GROUND!");

        for (Bird other : game.players) {
            if (other == null || other == this || other.health <= 0) continue;
            if (!canDamageTarget(other)) continue;
            double dx = other.bodyCenterX() - bodyCenterX();
            if (Math.abs(dx) < 280 + other.combatHalfWidth()
                    && Math.abs(other.bodyCenterY() - bodyCenterY()) < 180 + other.combatHalfHeight()) {
                int dmg = (int) (28 * powerMultiplier);
                double oldHealth = other.health;
                double dealtDamage = applyDamageTo(other, dmg);

                game.damageDealt[playerIndex] += (int) dealtDamage;
                game.recordSpecialImpact(playerIndex, (int) dealtDamage, dealtDamage > 0);
                boolean isKill = oldHealth > 0 && other.health <= 0;
                if (isKill) {
                    game.eliminations[playerIndex]++;
                    game.playZombieFallSfx();
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
        int dustBurstCount = scaledParticleCount(80);
        for (int i = 0; i < dustBurstCount; i++) {
            double angle = i / (double) dustBurstCount * Math.PI * 2;
            double speed = 4 + Math.random() * 10;
            double vx = Math.cos(angle) * speed;
            double vy = Math.sin(angle) * speed - 5;
            Color c = Math.random() < 0.7 ? Color.SADDLEBROWN : Color.SANDYBROWN;
            game.particles.add(new Particle(x + 40, y + 70, vx, vy, c));
        }

        int debrisBurstCount = scaledParticleCount(20);
        for (int i = 0; i < debrisBurstCount; i++) {
            double vx = (Math.random() - 0.5) * 20;
            double vy = -8 - Math.random() * 10;
            game.particles.add(new Particle(x + 40, y + 70, vx, vy, Color.GRAY));
        }
    }

    private void spawnDamageParticles(Bird target, double damage) {
        int particleCount = scaledParticleCount((int) Math.round(Math.min(50, 3 + damage * 2)));
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

    private int scaledParticleCount(int requested) {
        return game.scaledParticleBurstCount(requested);
    }

    private void logDamageKillFeed(double damage, boolean isKill, Bird victim) {
        String attacker = shortName();
        String victimName = victim.shortName();
        String verb = (type == BirdGame3.BirdType.RAZORBILL && bladeStormFrames > 0)
                ? "CARVED" : (damage >= 35 ? "BRUTALIZED" : damage >= 25 ? "SMASHED" : "hit");
        game.addToKillFeed(attacker + " " + verb + " " + victimName + "! -" + (int) damage + " HP");

        if (isKill) {
            game.addToKillFeed("ELIMINATED " + victimName + "!");
        }
    }

    private NormalAttackVariant selectNormalAttackVariant(boolean grounded) {
        boolean leftHeld = leftPressed();
        boolean rightHeld = rightPressed();
        if (grounded) {
            if (blockPressed()) {
                return NormalAttackVariant.DOWN_TILT;
            }
            if (leftHeld ^ rightHeld) {
                return NormalAttackVariant.SIDE_TILT;
            }
            if (jumpPressed()) {
                return NormalAttackVariant.UP_TILT;
            }
            return NormalAttackVariant.NEUTRAL;
        }
        if (blockPressed()) {
            return NormalAttackVariant.DOWN_AIR;
        }
        if (leftHeld ^ rightHeld) {
            boolean towardFacing = rightHeld == facingRight;
            return towardFacing ? NormalAttackVariant.FORWARD_AIR : NormalAttackVariant.BACK_AIR;
        }
        if (jumpPressed()) {
            return NormalAttackVariant.UP_AIR;
        }
        return NormalAttackVariant.NEUTRAL_AIR;
    }

    private boolean isGroundedDirectionalTiltVariant(NormalAttackVariant variant) {
        return variant == NormalAttackVariant.SIDE_TILT
                || variant == NormalAttackVariant.UP_TILT
                || variant == NormalAttackVariant.DOWN_TILT;
    }

    private NormalAttackVariant smashVariantForGroundedTilt(NormalAttackVariant variant) {
        return switch (variant) {
            case SIDE_TILT -> NormalAttackVariant.SIDE_SMASH;
            case UP_TILT -> NormalAttackVariant.UP_SMASH;
            case DOWN_TILT -> NormalAttackVariant.DOWN_SMASH;
            default -> variant;
        };
    }

    private NormalAttackProfile normalAttackProfile(NormalAttackVariant variant) {
        double facingDir = facingRight ? 1.0 : -1.0;
        return switch (variant) {
            case NEUTRAL -> new NormalAttackProfile(104.0, 84.0, facingDir * 14.0, -2.0,
                    0.80, 0.76, 0.92, 0.82, 0.82, 18, 8, AERIAL_LANDING_LAG_FRAMES);
            case SIDE_TILT -> new NormalAttackProfile(132.0, 84.0, facingDir * 28.0, -1.0,
                    0.90, 0.88, 1.02, 0.68, 0.68, 20, 10, AERIAL_LANDING_LAG_FRAMES);
            case UP_TILT -> new NormalAttackProfile(84.0, 128.0, 0.0, -32.0,
                    0.84, 0.86, 0.54, 1.48, 1.48, 19, 10, AERIAL_LANDING_LAG_FRAMES);
            case DOWN_TILT -> new NormalAttackProfile(112.0, 66.0, facingDir * 16.0, 20.0,
                    0.86, 0.84, 0.78, 0.26, 0.26, 21, 10, AERIAL_LANDING_LAG_FRAMES);
            case SIDE_SMASH -> new NormalAttackProfile(154.0, 90.0, facingDir * 46.0, 0.0,
                    1.18, 1.28, 1.48, 0.88, 0.88, 34, 14, AERIAL_LANDING_LAG_FRAMES);
            case UP_SMASH -> new NormalAttackProfile(96.0, 160.0, 0.0, -48.0,
                    1.10, 1.22, 0.72, 1.96, 1.96, 33, 14, AERIAL_LANDING_LAG_FRAMES);
            case DOWN_SMASH -> new NormalAttackProfile(132.0, 82.0, facingDir * 20.0, 30.0,
                    1.14, 1.20, 1.02, 0.46, 0.46, 36, 15, AERIAL_LANDING_LAG_FRAMES);
            case NEUTRAL_AIR -> new NormalAttackProfile(118.0, 108.0, 0.0, -6.0,
                    0.92, 0.95, 0.92, 1.00, 1.00, 26, 12, 7);
            case FORWARD_AIR -> new NormalAttackProfile(138.0, 90.0, facingDir * 36.0, -6.0,
                    0.98, 1.05, 1.22, 0.86, 0.86, 27, 13, 9);
            case BACK_AIR -> new NormalAttackProfile(130.0, 86.0, -facingDir * 34.0, -2.0,
                    1.08, 1.12, 1.42, 0.76, 0.76, 28, 13, 11);
            case UP_AIR -> new NormalAttackProfile(88.0, 146.0, 0.0, -44.0,
                    0.88, 0.98, 0.55, 1.82, 1.82, 25, 12, 6);
            case DOWN_AIR -> new NormalAttackProfile(92.0, 136.0, 0.0, 46.0,
                    1.02, 1.08, 0.72, 0.34, -0.95, 28, 14, 12);
        };
    }

    private boolean overlapsAttackArea(double targetCenterX, double targetCenterY,
                                       double targetHalfWidth, double targetHalfHeight,
                                       double attackCenterX, double attackCenterY,
                                       double horizontalReach, double verticalReach) {
        double dx = Math.abs(targetCenterX - attackCenterX);
        double dy = Math.abs(targetCenterY - attackCenterY);
        return dx <= horizontalReach + targetHalfWidth
                && dy <= verticalReach + targetHalfHeight;
    }

    private double launchDirectionFromAttackCenter(double targetCenterX, double attackCenterX) {
        double dx = targetCenterX - attackCenterX;
        if (Math.abs(dx) <= 0.001) {
            return facingRight ? 1.0 : -1.0;
        }
        return Math.signum(dx);
    }

    private NormalAttackProfile attack(int chargeFrames, NormalAttackVariant variant) {
        if (health <= 0) return normalAttackProfile(variant);
        NormalAttackProfile profile = normalAttackProfile(variant);
        double chargeRatio = attackChargeRatio(chargeFrames);
        double knockbackScale = (1.0 + CHARGED_ATTACK_KNOCKBACK_BONUS * chargeRatio * chargeRatio)
                * profile.knockbackMultiplier();
        double range = profile.horizontalReach() * sizeMultiplier;
        double verticalRange = profile.verticalReach() * sizeMultiplier;
        if (chargeRatio > 0.0) {
            range *= 1.0 + chargeRatio * 0.16;
            verticalRange *= 1.0 + chargeRatio * 0.12;
        }
        double attackCenterX = bodyCenterX() + profile.centerOffsetX() * sizeMultiplier;
        double attackCenterY = bodyCenterY() + profile.centerOffsetY() * sizeMultiplier;
        if (isNullRockForm()) {
            range *= 0.86;
            verticalRange *= 0.88;
            attackCenterX += (facingRight ? 1.0 : -1.0) * combatHalfWidth() * 0.88;
            attackCenterY -= combatHalfHeight() * 0.08;
        }
        int dmg = (int) Math.round(type.power * powerMultiplier
                * profile.damageMultiplier()
                * (1.0 + CHARGED_ATTACK_DAMAGE_BONUS * chargeRatio));
        for (Bird other : game.players) {
            if (other == null || other == this || other.health <= 0) continue;
            if (!canDamageTarget(other)) continue;

            if (overlapsAttackArea(other.bodyCenterX(), other.bodyCenterY(),
                    other.combatHalfWidth(), other.combatHalfHeight(),
                    attackCenterX, attackCenterY, range, verticalRange)) {
                double horizontalDirection = launchDirectionFromAttackCenter(other.bodyCenterX(), attackCenterX);
                double verticalScale = profile.verticalLaunchScaleFor(other.bodyCenterY(), attackCenterY)
                        * (1.0 + CHARGED_ATTACK_VERTICAL_BONUS * chargeRatio);
                processBirdAttack(other, dmg, knockbackScale, verticalScale,
                        profile.horizontalLaunchScale(), horizontalDirection);
            }
        }

        // === LOUNGE CAN BE HIT ===
        attackLounge(dmg);
        attackCrows(attackCenterX, attackCenterY, range, verticalRange, dmg, knockbackScale, profile);
        attackChicks(attackCenterX, attackCenterY, range, verticalRange, dmg, knockbackScale, profile);
        return profile;
    }

    private void attackCrows(double attackCenterX, double attackCenterY,
                             double range, double verticalRange,
                             int dmg, double knockbackScale,
                             NormalAttackProfile profile) {
        double reach = range + 35 * sizeMultiplier;
        double verticalReach = verticalRange + 30 * sizeMultiplier;
        int kills = 0;

        for (Iterator<CrowMinion> it = game.crowMinions.iterator(); it.hasNext(); ) {
            CrowMinion crow = it.next();
            if (crow.owner == this) continue;

            double crowHalfSize = 18.0 * crow.drawScale();
            if (!overlapsAttackArea(crow.x, crow.y, crowHalfSize, crowHalfSize,
                    attackCenterX, attackCenterY, reach, verticalReach)) {
                continue;
            }

            int damageTaken = Math.max(1, dmg / 10);
            crow.life -= damageTaken;
            if (crow.life > 0) {
                double dx = crow.x - attackCenterX;
                double verticalScale = profile.verticalLaunchScaleFor(crow.y, attackCenterY);
                reactToCrowHit(crow, dx, crow.y, attackCenterY, damageTaken, knockbackScale, verticalScale);
                continue;
            }

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
            String source = shortName();
            game.addToKillFeed(source + " swatted " + kills + " crow" + (kills > 1 ? "s" : "") + "!");
            if (!game.usesSmashCombatRules()) {
                game.scores[playerIndex] += kills * 2;
            }
        }
    }

    private void reactToCrowHit(CrowMinion crow, double dx, double targetCenterY, double attackCenterY,
                                int damageTaken, double knockbackScale, double verticalScale) {
        if (crow == null || !crow.hasHeavyLifePool()) return;
        double direction = dx == 0 ? (facingRight ? 1.0 : -1.0) : Math.signum(dx);
        double knockback = (0.8 + damageTaken * 0.45) * Math.max(1.0, knockbackScale);
        double verticalKnockback = (0.8 + damageTaken * 0.25)
                * Math.max(0.65, 0.75 + knockbackScale * 0.25)
                * Math.max(0.45, Math.abs(verticalScale));
        double knockbackY = verticalScale < 0.0 && targetCenterY >= attackCenterY + 6.0
                ? verticalKnockback
                : -verticalKnockback;
        crow.registerHit(direction * knockback, knockbackY);

        Color spark = switch (crow.effectiveVariant()) {
            case CrowMinion.VARIANT_GIANT_CROW -> Color.web("#FF8A80");
            case CrowMinion.VARIANT_RAVEN -> Color.web("#90CAF9");
            case CrowMinion.VARIANT_VOID_RAVEN -> Color.web("#CE93D8");
            default -> Color.web("#E0E0E0");
        };
        int particleCount = 6 + damageTaken * 2;
        for (int i = 0; i < particleCount; i++) {
            double angle = Math.random() * Math.PI * 2;
            double speed = 2.5 + Math.random() * 6.0;
            game.particles.add(new Particle(
                    crow.x,
                    crow.y,
                    Math.cos(angle) * speed + direction * 0.8,
                    Math.sin(angle) * speed - 1.8,
                    spark.deriveColor(0, 1, 1, 0.82)
                ));
        }
    }

    private void attackChicks(double attackCenterX, double attackCenterY,
                              double range, double verticalRange,
                              int dmg, double knockbackScale,
                              NormalAttackProfile profile) {
        double reach = range + 30 * sizeMultiplier;
        double verticalReach = verticalRange + 24 * sizeMultiplier;
        int kills = 0;

        for (Iterator<ChickMinion> it = game.chickMinions.iterator(); it.hasNext(); ) {
            ChickMinion chick = it.next();
            if (chick.owner == this) continue;

            double cx = chick.x + chick.width * 0.5;
            double cy = chick.y + chick.height * 0.5;
            if (!overlapsAttackArea(cx, cy, chick.width * 0.5, chick.height * 0.5,
                    attackCenterX, attackCenterY, reach, verticalReach)) {
                continue;
            }

            Color hitColor = chick.ultimate ? Color.GOLD : Color.web("#FFB74D");
            int damageTaken = Math.max(1, dmg / 12);
            chick.life -= damageTaken;

            int particleCount = chick.life > 0 ? 8 + damageTaken * 2 : 14;
            for (int i = 0; i < particleCount; i++) {
                double angle = Math.random() * Math.PI * 2;
                double speed = chick.life > 0 ? 3 + Math.random() * 6 : 4 + Math.random() * 9;
                game.particles.add(new Particle(
                        cx,
                        cy,
                        Math.cos(angle) * speed,
                        Math.sin(angle) * speed - 3,
                        hitColor
                ));
            }

            double dx = cx - attackCenterX;
            double kbDir = dx == 0 ? (facingRight ? 1 : -1) : Math.signum(dx);
            chick.vx += kbDir * Math.max(4.0, dmg * 0.18) * Math.max(1.0, knockbackScale);
            double verticalScale = profile.verticalLaunchScaleFor(cy, attackCenterY);
            double verticalKnockback = (3.5 + dmg * 0.08)
                    * Math.max(0.75, Math.abs(verticalScale))
                    * Math.max(1.0, 0.75 + knockbackScale * 0.25);
            if (verticalScale < 0.0 && cy >= attackCenterY + 6.0) {
                chick.vy = Math.max(chick.vy, verticalKnockback);
            } else {
                chick.vy = Math.min(chick.vy, -verticalKnockback);
            }
            chick.onGround = false;
            chick.jumpCooldown = Math.max(chick.jumpCooldown, 10);
            chick.attackCooldown = Math.max(chick.attackCooldown, 8);

            if (chick.life > 0) continue;

            it.remove();
            kills++;
        }

        if (kills > 0) {
            String source = shortName();
            game.addToKillFeed(source + " bopped " + kills + " chick" + (kills > 1 ? "s" : "") + "!");
            if (!game.usesSmashCombatRules()) {
                game.scores[playerIndex] += kills * 2;
            }
        }
    }

    private void processBirdAttack(Bird other, int dmg, double knockbackScale,
                                   double verticalScale, double horizontalScale,
                                   double horizontalDirection) {
        double kb = type.power * horizontalDirection * (game.usesSmashCombatRules() ? 2.2 : 1.8)
                * knockbackScale * ATTACK_HORIZONTAL_KNOCKBACK_SCALE * horizontalScale;
        double verticalKb = (game.usesSmashCombatRules() ? 6.5 : 5.0) * verticalScale * ATTACK_VERTICAL_KNOCKBACK_SCALE;

        double shieldPushback = Math.copySign(
                Math.max(1.8, Math.abs(kb) * SHIELD_PUSHBACK_SCALE),
                kb == 0.0 ? horizontalDirection : kb
        );
        ShieldHitResult shieldHit = other.resolveShieldHit(this, scaledDamageAgainst(other, dmg), shieldPushback);
        if (shieldHit.blocked()) {
            return;
        }

        other.vx += kb;
        other.vy -= verticalKb;
        double oldHealth = other.health;
        double dealtDamage = applyUnshieldedDamageTo(other, dmg);

        game.damageDealt[playerIndex] += (int) dealtDamage;
        if (!game.usesSmashCombatRules() && other.health <= 0 && oldHealth > 0) {
            game.eliminations[playerIndex]++;
            game.checkAchievements(this);
            game.playZombieFallSfx();
            game.scores[playerIndex] += 50;
        }
        if (!game.usesSmashCombatRules()) {
            game.scores[playerIndex] += (int) dealtDamage / 2;
        }

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

    private double scaledDamageAgainst(Bird target, double rawDamage) {
        if (target == null || rawDamage <= 0) return 0;
        return rawDamage * outgoingDamageMultiplier() * target.incomingDamageMultiplier();
    }

    private double shieldDurabilityRatio() {
        return Math.clamp(shieldHealth / SHIELD_MAX_HEALTH, 0.0, 1.0);
    }

    private double shieldVisualScale() {
        double durabilityScale = SHIELD_MIN_VISUAL_SCALE + (1.0 - SHIELD_MIN_VISUAL_SCALE) * shieldDurabilityRatio();
        double holdScale = 1.0 - SHIELD_HOLD_VISUAL_SHRINK * Math.clamp(shieldHoldVisual, 0.0, 1.0);
        return Math.max(0.34, durabilityScale * holdScale);
    }

    private boolean shouldReserveBlockForAttack(boolean airborne) {
        if (!blockPressed() || health <= 0) {
            return false;
        }
        if (isChargingAttack()) {
            return chargingAttackVariant.usesDownInput();
        }
        if (pendingGroundAttackFrames > 0) {
            return pendingGroundAttackVariant.usesDownInput();
        }
        if (!attackPressed() || attackCooldown > 0) {
            return false;
        }
        return selectNormalAttackVariant(!airborne).usesDownInput();
    }

    private boolean canRaiseShieldFromCurrentInput() {
        return health > 0
                && !isBlocking
                && !isDodging()
                && !(type == BirdGame3.BirdType.PIGEON && pigeonSpecialActive())
                && !(isRaptor() && raptorSpecialActive())
                && jumpSquatTimer <= 0
                && landingLagTimer <= 0
                && knockdownTimer <= 0
                && blockPressed()
                && !shouldReserveBlockForAttack(false)
                && !shouldReserveBlockForSpecial()
                && stunTime <= 0.0
                && blockCooldown <= 0
                && shieldHealth > 0.0
                && !ledgeHanging
                && !batHanging
                && !onVine
                && !isGrappling
                && !isInDockWater()
                && isOnGround();
    }

    private void primeShieldForIncomingHit() {
        if (!canRaiseShieldFromCurrentInput()) {
            return;
        }
        isBlocking = true;
        if (!blockHeldLastFrame) {
            parryWindowFrames = Math.max(parryWindowFrames, SHIELD_PARRY_STARTUP_FRAMES);
        }
    }

    private void spawnShieldParticles(Color color, int count, double speedScale) {
        double centerX = bodyCenterX();
        double centerY = bodyCenterY();
        double radius = bodyWidth() * 0.58 * shieldVisualScale();
        for (int i = 0; i < count; i++) {
            double angle = Math.random() * Math.PI * 2.0;
            double speed = speedScale * (0.45 + Math.random());
            game.particles.add(new Particle(
                    centerX + Math.cos(angle) * radius,
                    centerY + Math.sin(angle) * radius,
                    Math.cos(angle) * speed,
                    Math.sin(angle) * speed - 1.0,
                    color.deriveColor(0, 1, 1, 0.45 + Math.random() * 0.35)
            ));
        }
    }

    private void breakShield(Bird attacker, double pushDirection) {
        isBlocking = false;
        parryWindowFrames = 0;
        shieldStunFrames = 0;
        shieldHealth = 0.0;
        blockCooldown = Math.max(blockCooldown, SHIELD_BREAK_COOLDOWN_FRAMES);
        stunTime = Math.max(stunTime, SHIELD_BREAK_STUN_FRAMES);
        if (Math.abs(pushDirection) > 0.001) {
            vx = pushDirection * 1.6;
        } else {
            vx *= 0.25;
        }
        vy = Math.min(vy, -4.8);
        spawnShieldParticles(Color.web("#FFF176"), 24, 6.0);
        shieldHoldVisual = 0.0;
        game.hitstopFrames = Math.max(game.hitstopFrames, 6);
        game.shakeIntensity = Math.max(game.shakeIntensity, 9.0);
        if (attacker != null && attacker != this) {
            game.addToKillFeed(shortName() + "'S SHIELD BROKE!");
        } else {
            game.addToKillFeed(shortName() + " SHIELD BROKE!");
        }
    }

    private ShieldHitResult resolveShieldHit(Bird attacker, double scaledDamage, double shieldPushback) {
        if (scaledDamage <= 0.0 || health <= 0) {
            return ShieldHitResult.NONE;
        }

        primeShieldForIncomingHit();
        if (!isBlocking || shieldHealth <= 0.0) {
            return ShieldHitResult.NONE;
        }

        if (parryWindowFrames > 0) {
            isBlocking = false;
            parryWindowFrames = 0;
            shieldStunFrames = 0;
            blockCooldown = Math.max(blockCooldown, SHIELD_PARRY_RELEASE_FRAMES);
            vx *= 0.45;
            if (attacker != null && attacker != this) {
                attacker.cancelAttackCharge();
                attacker.applyStun(SHIELD_PARRY_ATTACKER_STUN_FRAMES);
                attacker.vx *= 0.35;
            }
            spawnShieldParticles(Color.web("#D0F8FF"), 18, 4.6);
            game.hitstopFrames = Math.max(game.hitstopFrames, SHIELD_PARRY_HITSTOP_FRAMES);
            game.shakeIntensity = Math.max(game.shakeIntensity, 6.0);
            if (attacker != null && attacker != this) {
                game.addToKillFeed(shortName() + " PARRIED " + attacker.shortName() + "!");
            } else {
                game.addToKillFeed(shortName() + " PARRIED the hit!");
            }
            return ShieldHitResult.PARRIED;
        }

        parryWindowFrames = 0;
        double durabilityBeforeHit = shieldDurabilityRatio();
        double shieldDamage = Math.max(1.0, SHIELD_DAMAGE_BASE + scaledDamage * SHIELD_DAMAGE_SCALE);
        shieldHealth = Math.max(0.0, shieldHealth - shieldDamage);
        shieldStunFrames = Math.max(
                shieldStunFrames,
                (int) Math.ceil(SHIELD_STUN_BASE_FRAMES
                        + scaledDamage * SHIELD_STUN_DAMAGE_SCALE
                        + (1.0 - durabilityBeforeHit) * SHIELD_STUN_LOW_HEALTH_BONUS)
        );

        double push = shieldPushback;
        if (Math.abs(push) < 0.001 && attacker != null && attacker != this) {
            double direction = Math.signum(bodyCenterX() - attacker.bodyCenterX());
            push = (direction == 0.0 ? (attacker.facingRight ? 1.0 : -1.0) : direction) * (1.4 + scaledDamage * 0.12);
        }
        if (Math.abs(push) > 0.001) {
            double durabilityPushScale = 1.0 + (1.0 - durabilityBeforeHit) * 0.55;
            vx += push * durabilityPushScale;
        }

        shieldHoldVisual = Math.min(1.0, shieldHoldVisual + 0.08);
        spawnShieldParticles(Color.web("#64B5F6"), 10 + (int) Math.min(8.0, scaledDamage * 0.35), 3.0);
        game.hitstopFrames = Math.max(game.hitstopFrames, (int) Math.min(8, 2 + scaledDamage / 7.0));
        game.shakeIntensity = Math.clamp(2.0 + scaledDamage * 0.12, game.shakeIntensity, 8.0);

        if (shieldHealth <= 0.0) {
            breakShield(attacker, push);
        }
        return ShieldHitResult.BLOCKED;
    }

    private void attackLounge(int baseDamage) {
        for (Bird target : game.players) {
            if (target == null || target.type != BirdGame3.BirdType.MOCKINGBIRD || !target.loungeActive || target.loungeHealth <= 0)
                continue;
            if (!canDamageTarget(target)) continue;

            double distToLounge = Math.hypot(target.loungeX - (x + 40), target.loungeY - (y + 40));
            if (distToLounge < 130) {
                int loungeDmg = Math.max(1, (int) Math.round(baseDamage * 2.0));
                target.loungeHealth -= loungeDmg;
                target.loungeDamageFlash = 15;

                game.addToKillFeed(shortName() + " smashed the Lounge! -" + loungeDmg + " HP");

                int loungeHitParticles = scaledParticleCount(30);
                for (int i = 0; i < loungeHitParticles; i++) {
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
                    target.loungeMaxHealth = LOUNGE_MAX_HEALTH;
                    target.loungeRoyal = false;
                    game.addToKillFeed("THE LOUNGE HAS BEEN OBLITERATED!");
                    game.shakeIntensity = 30;
                    game.hitstopFrames = 18;
                    int loungeBreakParticles = scaledParticleCount(120);
                    for (int i = 0; i < loungeBreakParticles; i++) {
                        double angle = i / (double) loungeBreakParticles * Math.PI * 2;
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

    private boolean handleGrabInput(boolean airborne) {
        boolean held = grabPressed();
        boolean grabLocked = false;

        if (held && !grabHeldLastFrame
                && !airborne
                && stunTime <= 0.0
                && grabCooldown <= 0
                && attackCooldown <= 0
                && !isBlocking) {
            cancelAttackCharge();
            grabLocked = true;
            if (!attemptGrab()) {
                grabCooldown = Math.max(grabCooldown, GRAB_WHIFF_COOLDOWN_FRAMES);
                attackAnimationTimer = Math.max(attackAnimationTimer, 5);
                vx *= 0.55;
            }
        }

        grabHeldLastFrame = held;
        return grabLocked;
    }

    private boolean attemptGrab() {
        Bird target = findGrabTarget();
        if (target == null) {
            return false;
        }
        beginGrabOn(target);
        return true;
    }

    private Bird findGrabTarget() {
        Bird best = null;
        double bestDistance = Double.MAX_VALUE;
        for (Bird other : game.players) {
            if (!canGrabTarget(other)) {
                continue;
            }
            double distance = combatDistanceTo(other);
            if (distance < bestDistance) {
                bestDistance = distance;
                best = other;
            }
        }
        return best;
    }

    private boolean canGrabTarget(Bird other) {
        if (other == null || other == this) return false;
        if (!canDamageTarget(other)) return false;
        if (grabbedTarget != null || grabbedBy != null) return false;
        if (other.grabbedBy != null || other.grabbedTarget != null) return false;
        if (!isOnGround() || !other.isOnGround()) return false;
        if (onVine || batHanging || ledgeHanging || isGrappling || isInDockWater()) return false;
        if (other.onVine || other.batHanging || other.ledgeHanging || other.isGrappling || other.isInDockWater()) return false;

        double dx = other.bodyCenterX() - bodyCenterX();
        double dy = Math.abs(other.bodyCenterY() - bodyCenterY());
        if (dy > GRAB_VERTICAL_REACH + other.combatHalfHeight()) return false;

        double forwardReach = GRAB_FORWARD_REACH * sizeMultiplier + other.combatHalfWidth();
        double backReach = GRAB_BACK_REACH * sizeMultiplier + other.combatHalfWidth();
        if (facingRight) {
            return dx >= -backReach && dx <= forwardReach;
        }
        return dx <= backReach && dx >= -forwardReach;
    }

    private void beginGrabOn(Bird target) {
        releaseGrabState(false);
        if (target == null) {
            return;
        }
        target.releaseGrabState(false);
        grabbedTarget = target;
        target.grabbedBy = this;
        grabHoldTimer = GRAB_HOLD_FRAMES;
        grabThrowLockTimer = GRAB_THROW_LOCK_FRAMES;
        isBlocking = false;
        parryWindowFrames = 0;
        shieldStunFrames = 0;
        target.isBlocking = false;
        target.parryWindowFrames = 0;
        target.shieldStunFrames = 0;
        target.blockCooldown = Math.max(target.blockCooldown, 6);
        target.cancelAttackCharge();
        target.attackHeldLastFrame = false;
        attackAnimationTimer = Math.max(attackAnimationTimer, 8);
        vx = 0.0;
        target.vx = 0.0;
        target.vy = 0.0;
        syncGrabbedTargetPosition();
        game.addToKillFeed(shortName() + " grabbed " + target.shortName() + "!");
    }

    private boolean handleGrabbedState() {
        if (grabbedBy == null) {
            return false;
        }
        Bird holder = grabbedBy;
        if (holder.health <= 0 || holder.grabbedTarget != this) {
            grabbedBy = null;
            return false;
        }
        holder.syncGrabbedTargetPosition();
        vx = 0.0;
        vy = 0.0;
        isBlocking = false;
        parryWindowFrames = 0;
        shieldStunFrames = 0;
        cancelAttackCharge();
        attackHeldLastFrame = attackPressed();
        return true;
    }

    private boolean handleHoldingGrabState(boolean stunned, boolean inDockWater) {
        if (grabbedTarget == null) {
            return false;
        }
        Bird target = grabbedTarget;
        if (stunned || inDockWater || !isOnGround() || target.health <= 0 || target.grabbedBy != this) {
            releaseGrabState(true);
            return false;
        }

        cancelAttackCharge();
        attackHeldLastFrame = attackPressed();
        isBlocking = false;
        parryWindowFrames = 0;
        shieldStunFrames = 0;
        vx *= 0.45;
        vy = Math.max(0.0, vy);
        if (leftPressed() && !rightPressed()) {
            facingRight = false;
        } else if (rightPressed() && !leftPressed()) {
            facingRight = true;
        }

        syncGrabbedTargetPosition();
        if (grabThrowLockTimer > 0) {
            grabThrowLockTimer--;
        }
        if (grabHoldTimer > 0) {
            grabHoldTimer--;
        }

        GrabThrowDirection direction = throwDirectionFromInput();
        if (direction != GrabThrowDirection.NONE && grabThrowLockTimer <= 0) {
            performThrow(direction);
        } else if (grabHoldTimer <= 0) {
            performThrow(GrabThrowDirection.FORWARD);
        }
        return true;
    }

    private GrabThrowDirection throwDirectionFromInput() {
        if (jumpPressed()) {
            return GrabThrowDirection.UP;
        }
        if (blockPressed()) {
            return GrabThrowDirection.DOWN;
        }
        if (leftPressed() && !rightPressed()) {
            return facingRight ? GrabThrowDirection.BACK : GrabThrowDirection.FORWARD;
        }
        if (rightPressed() && !leftPressed()) {
            return facingRight ? GrabThrowDirection.FORWARD : GrabThrowDirection.BACK;
        }
        return GrabThrowDirection.NONE;
    }

    private void syncGrabbedTargetPosition() {
        if (grabbedTarget == null || grabbedTarget.grabbedBy != this) {
            return;
        }
        Bird target = grabbedTarget;
        double xDir = facingRight ? 1.0 : -1.0;
        double separation = combatHalfWidth() + target.combatHalfWidth() - GRAB_HOLD_X_PADDING;
        double targetCenterX = bodyCenterX() + xDir * separation;
        double targetGroundY = bodyBottomY();
        target.x = targetCenterX - target.bodyWidth() / 2.0;
        target.y = targetGroundY - target.bodyHeight();
        target.vx = 0.0;
        target.vy = 0.0;
        target.facingRight = !facingRight;
    }

    private void performThrow(GrabThrowDirection direction) {
        Bird target = grabbedTarget;
        if (target == null) {
            releaseGrabState(true);
            return;
        }

        double facingDir = facingRight ? 1.0 : -1.0;
        int rawDamage;
        double launchX;
        double launchY;
        int stunFrames;
        String verb;

        switch (direction) {
            case BACK -> {
                rawDamage = THROW_BACK_DAMAGE;
                launchX = -facingDir * 20.0;
                launchY = -8.2;
                stunFrames = 18;
                verb = "back-threw";
            }
            case UP -> {
                rawDamage = THROW_UP_DAMAGE;
                launchX = facingDir * 4.2;
                launchY = -17.0;
                stunFrames = 20;
                verb = "up-threw";
            }
            case DOWN -> {
                rawDamage = THROW_DOWN_DAMAGE;
                launchX = facingDir * 6.0;
                launchY = -3.4;
                stunFrames = 24;
                verb = "down-threw";
            }
            case FORWARD, NONE -> {
                rawDamage = THROW_FORWARD_DAMAGE;
                launchX = facingDir * 18.0;
                launchY = -6.8;
                stunFrames = 16;
                verb = "forward-threw";
            }
            default -> throw new IllegalStateException("Unexpected value: " + direction);
        }

        clearGrabLink(target);
        grabHoldTimer = 0;
        grabThrowLockTimer = 0;
        grabCooldown = Math.max(grabCooldown, GRAB_THROW_COOLDOWN_FRAMES);
        attackCooldown = Math.max(attackCooldown, 8);
        attackAnimationTimer = Math.max(attackAnimationTimer, 10);

        double oldHealth = target.health;
        double dealtDamage = applyUnshieldedDamageTo(target, rawDamage);
        target.vx += launchX;
        target.vy = Math.min(target.vy, launchY);
        if (target.health > 0) {
            target.applyStun(stunFrames);
            target.applyPendingSmashLaunch();
        }

        game.damageDealt[playerIndex] += (int) Math.round(dealtDamage);
        if (!game.usesSmashCombatRules() && target.health <= 0 && oldHealth > 0) {
            game.eliminations[playerIndex]++;
            game.checkAchievements(this);
            game.playZombieFallSfx();
            game.scores[playerIndex] += 50;
        }
        if (!game.usesSmashCombatRules()) {
            game.scores[playerIndex] += (int) dealtDamage / 2;
        }

        if (dealtDamage > 0) {
            spawnDamageParticles(target, dealtDamage);
        }
        if (dealtDamage >= 4.0) {
            game.addToKillFeed(shortName() + " " + verb + " " + target.shortName() + "! -" + (int) Math.round(dealtDamage) + " HP");
            game.playHitSound(dealtDamage);
        }
        game.hitstopFrames = Math.max(game.hitstopFrames, 4);
        game.shakeIntensity = Math.max(game.shakeIntensity, 5.0);
    }

    private void clearGrabLink(Bird target) {
        if (target != null && target.grabbedBy == this) {
            target.grabbedBy = null;
        }
        if (grabbedTarget == target) {
            grabbedTarget = null;
        }
    }

    private void releaseGrabState(boolean applyCooldown) {
        if (grabbedTarget != null) {
            Bird target = grabbedTarget;
            grabbedTarget = null;
            if (target.grabbedBy == this) {
                target.grabbedBy = null;
            }
            if (applyCooldown) {
                grabCooldown = Math.max(grabCooldown, GRAB_RELEASE_COOLDOWN_FRAMES);
            }
        }
        if (grabbedBy != null) {
            Bird holder = grabbedBy;
            grabbedBy = null;
            if (holder.grabbedTarget == this) {
                holder.grabbedTarget = null;
                if (applyCooldown) {
                    holder.grabCooldown = Math.max(holder.grabCooldown, GRAB_RELEASE_COOLDOWN_FRAMES);
                }
            }
        }
        grabHoldTimer = 0;
        grabThrowLockTimer = 0;
    }

    private void interruptGrabStateOnHit() {
        releaseGrabState(true);
    }

    private boolean isChargingAttack() {
        return attackChargeFrames > 0;
    }

    private boolean isGroundAttackPending() {
        return pendingGroundAttackFrames > 0;
    }

    private double attackChargeRatio(int chargeFrames) {
        if (chargeFrames <= 0) return 0.0;
        return Math.clamp(chargeFrames / (double) MAX_ATTACK_CHARGE_FRAMES, 0.0, 1.0);
    }

    private void beginAttackCharge(NormalAttackVariant variant) {
        pendingGroundAttackFrames = 0;
        pendingGroundAttackVariant = NormalAttackVariant.NEUTRAL;
        attackChargeFrames = 1;
        chargingAttackVariant = variant;
        attackAnimationTimer = 0;
    }

    private void cancelAttackCharge() {
        pendingGroundAttackFrames = 0;
        pendingGroundAttackVariant = NormalAttackVariant.NEUTRAL;
        attackChargeFrames = 0;
        chargingAttackVariant = NormalAttackVariant.NEUTRAL;
    }

    private void emitAttackChargeParticles() {
        double chargeRatio = attackChargeRatio(attackChargeFrames);
        if (chargeRatio <= 0.0) return;
        double centerX = bodyCenterX();
        double centerY = bodyCenterY();
        int count = 1 + (chargeRatio >= 0.7 ? 1 : 0);
        for (int i = 0; i < count; i++) {
            double angle = Math.random() * Math.PI * 2;
            double radius = 26 + chargeRatio * 30 + Math.random() * 10;
            Color color = chargeRatio >= 0.95 ? Color.web("#FFF59D") : Color.web("#FFB74D");
            game.particles.add(new Particle(
                    centerX + Math.cos(angle) * radius,
                    centerY + Math.sin(angle) * radius,
                    -Math.cos(angle) * (0.8 + chargeRatio * 1.6),
                    -Math.sin(angle) * (0.8 + chargeRatio * 1.6),
                    color.deriveColor(0, 1, 1, 0.72 + chargeRatio * 0.18)
            ));
        }
    }

    private void performAttack(int chargeFrames) {
        NormalAttackVariant variant = chargeFrames > 0 ? chargingAttackVariant : selectNormalAttackVariant(isOnGround());
        performAttack(chargeFrames, variant);
    }

    private void performAttack(int chargeFrames, NormalAttackVariant variant) {
        NormalAttackProfile profile = attack(chargeFrames, variant);
        double chargeRatio = attackChargeRatio(chargeFrames);
        game.playButterSfx();
        activeAttackVariant = variant;
        attackCooldown = scaledAttackCooldown(profile.cooldownFrames()) + (int) Math.round(chargeRatio * 18.0);
        attackAnimationTimer = profile.animationFrames() + (int) Math.round(chargeRatio * 10.0);
        if (!isOnGround()) {
            aerialAttackActive = true;
            aerialAttackTotalFrames = attackAnimationTimer;
            activeAerialLandingLagFrames = profile.landingLagFrames();
        } else {
            clearAerialAttackState();
        }
        cancelAttackCharge();
    }

    private boolean handleAttackInput(boolean canCharge) {
        boolean held = attackPressed();
        boolean attackLocked = isChargingAttack() || isGroundAttackPending();

        if (isChargingAttack()) {
            if (held && canCharge && attackCooldown <= 0 && !isBlocking) {
                attackChargeFrames = Math.min(MAX_ATTACK_CHARGE_FRAMES, attackChargeFrames + 1);
                if (attackChargeFrames % 5 == 0) {
                    emitAttackChargeParticles();
                }
                if (attackChargeFrames >= MAX_ATTACK_CHARGE_FRAMES) {
                    performAttack(attackChargeFrames);
                }
            } else {
                performAttack(attackChargeFrames);
            }
        } else if (isGroundAttackPending()) {
            if (!held || !isOnGround() || attackCooldown > 0 || isBlocking) {
                NormalAttackVariant variant = pendingGroundAttackVariant;
                performAttack(0, variant);
            } else {
                pendingGroundAttackFrames++;
                if (pendingGroundAttackFrames >= GROUND_SMASH_HOLD_THRESHOLD_FRAMES) {
                    beginAttackCharge(smashVariantForGroundedTilt(pendingGroundAttackVariant));
                    emitAttackChargeParticles();
                }
            }
        } else if (held && !attackHeldLastFrame && attackCooldown <= 0 && !isBlocking) {
            NormalAttackVariant variant = selectNormalAttackVariant(isOnGround());
            if (canCharge && isOnGround() && isGroundedDirectionalTiltVariant(variant)) {
                pendingGroundAttackFrames = 1;
                pendingGroundAttackVariant = variant;
            } else {
                performAttack(0, variant);
            }
            attackLocked = true;
        }

        attackHeldLastFrame = held;
        return attackLocked;
    }

    private void special() {
        if (health <= 0) {
            return;
        }
        if (type == BirdGame3.BirdType.PIGEON && !canStartPigeonSpecial()) {
            return;
        }
        if (isRaptor()) {
            RaptorSpecialVariant variant = selectRaptorSpecialVariant();
            if (!canStartRaptorSpecialVariant(variant)) {
                if (!game.isAI[playerIndex] && raptorSpecialOnReuseLockout(variant)) {
                    cooldownFlash = 15;
                }
                return;
            }
        }
        boolean ultimateReady = isUltimateReady();
        if (!isRaptor() && specialCooldown > 0 && !ultimateReady) {
            if (!game.isAI[playerIndex]) {
                this.cooldownFlash = 15;
            }
            return;
        }

        boolean ultimateTriggered = ultimateReady && consumeUltimate();
        if (ultimateTriggered) {
            game.addToKillFeed(shortName() + " UNLEASHED ULTIMATE!");
            game.shakeIntensity = Math.max(game.shakeIntensity, 18);
            game.hitstopFrames = Math.max(game.hitstopFrames, 8);
            game.triggerFlash(0.7, false);
            int ultimateBurstParticles = scaledParticleCount(90);
            for (int i = 0; i < ultimateBurstParticles; i++) {
                double angle = Math.random() * Math.PI * 2;
                double speed = 8 + Math.random() * 16;
                game.particles.add(new Particle(
                        x + 40 + Math.cos(angle) * 20,
                        y + 40 + Math.sin(angle) * 20,
                        Math.cos(angle) * speed,
                        Math.sin(angle) * speed - 4,
                        Color.GOLD.deriveColor(0, 1, 1, 0.95)
                ));
            }
        }

        if (game.isSfxEnabled()) {
                if (type == BirdGame3.BirdType.RAZORBILL) {
                    game.playVaseBreakingSfx();
                } else {
                    game.playJalapenoSfx();
                }
        }
        game.specialsUsed[playerIndex]++;

        switch (type) {
            case PIGEON -> specialPigeon(selectPigeonSpecialVariant(), ultimateTriggered);
            case EAGLE, FALCON -> specialRaptor(selectRaptorSpecialVariant(), ultimateTriggered);
            case PHOENIX -> specialPhoenix(ultimateTriggered);
            case HUMMINGBIRD -> specialHummingbird(ultimateTriggered);
            case TURKEY -> specialTurkey(ultimateTriggered);
            case ROADRUNNER -> specialRoadrunner(ultimateTriggered);
            case PENGUIN -> specialPenguin(ultimateTriggered);
            case SHOEBILL -> specialShoebill(ultimateTriggered);
            case MOCKINGBIRD -> specialMockingbird(ultimateTriggered);
            case RAZORBILL -> specialRazorbill(ultimateTriggered);
            case GRINCHHAWK -> specialGrinchhawk(ultimateTriggered);
            case VULTURE -> specialVulture(ultimateTriggered);
            case ROOSTER -> specialRooster(ultimateTriggered);
            case OPIUMBIRD -> specialOpiumBird(ultimateTriggered);
            case HEISENBIRD -> specialHeisenbird(ultimateTriggered);
            case TITMOUSE -> specialTitmouse(ultimateTriggered);
            case BAT -> specialBat(ultimateTriggered);
            case PELICAN -> specialPelican(ultimateTriggered);
            case RAVEN -> specialRaven(ultimateTriggered);
        }
    }

    private void specialPigeon(PigeonSpecialVariant variant, boolean ultimate) {
        switch (variant) {
            case NEUTRAL -> specialPigeonNeutral(ultimate);
            case SIDE -> specialPigeonSide(ultimate);
            case UP -> specialPigeonUp(ultimate);
            case DOWN -> specialPigeonDown(ultimate);
        }
    }

    private void specialRaptor(RaptorSpecialVariant variant, boolean ultimate) {
        switch (variant) {
            case NEUTRAL -> specialRaptorNeutral(ultimate);
            case SIDE -> specialRaptorSide(ultimate);
            case UP -> specialRaptorUp(ultimate);
            case DOWN -> {
                if (type == BirdGame3.BirdType.EAGLE) {
                    specialEagle(ultimate);
                } else {
                    specialFalcon(ultimate);
                }
            }
        }
    }

    private void specialPigeonNeutral(boolean ultimate) {
        int dir = horizontalInputDirection();
        if (dir != 0) {
            facingRight = dir > 0;
        }
        dir = facingDirection();
        pigeonFeatherBurstTimer = PIGEON_NEUTRAL_BURST_FRAMES;
        pigeonFeatherBurstUltimate = ultimate;
        specialCooldown = PIGEON_NEUTRAL_COOLDOWN_FRAMES + (ultimate ? 6 : 0);
        specialMaxCooldown = specialCooldown;
        attackAnimationTimer = Math.max(attackAnimationTimer, pigeonFeatherBurstTimer);
        vx *= 0.45;

        double[] laneOffsets = {-20.0, 0.0, 20.0};
        double[] laneReach = ultimate ? new double[]{116.0, 132.0, 116.0} : new double[]{102.0, 118.0, 102.0};
        double centerX = bodyCenterX() + dir * bodyWidth() * 0.54;
        for (Bird other : game.players) {
            if (!canDamageTarget(other)) continue;
            double dx = other.bodyCenterX() - centerX;
            if (dir > 0 && dx < -other.combatHalfWidth() * 0.2) continue;
            if (dir < 0 && dx > other.combatHalfWidth() * 0.2) continue;

            boolean hit = false;
            for (int i = 0; i < laneOffsets.length; i++) {
                double laneY = bodyCenterY() + laneOffsets[i] * sizeMultiplier;
                double laneDx = Math.abs(dx);
                double laneDy = Math.abs(other.bodyCenterY() - laneY);
                if (laneDx > laneReach[i] * sizeMultiplier + other.combatHalfWidth()) continue;
                if (laneDy > 18.0 * sizeMultiplier + other.combatHalfHeight()) continue;
                hit = true;
                break;
            }
            if (!hit) continue;

            int dmg = ultimate ? 6 : 4;
            double oldHealth = other.health;
            int dealt = (int) applyDamageTo(other, dmg);
            if (dealt <= 0) continue;

            game.damageDealt[playerIndex] += dealt;
            game.recordSpecialImpact(playerIndex, dealt, true);
            if (other.health <= 0 && oldHealth > 0) {
                game.eliminations[playerIndex]++;
            }

            other.vx += dir * (ultimate ? 5.8 : 4.6);
            other.vy -= ultimate ? 3.8 : 2.9;
        }

        for (int feather = 0; feather < 3; feather++) {
            double laneY = bodyCenterY() + laneOffsets[feather] * sizeMultiplier;
            for (int i = 0; i < 6; i++) {
                double progress = i / 5.0;
                double spread = (feather - 1) * 0.18;
                double speed = 4.2 + progress * 6.0;
                game.particles.add(new Particle(
                        centerX + dir * (10 + progress * 56),
                        laneY + Math.sin(progress * Math.PI) * 6 * spread,
                        dir * speed,
                        spread * 2.4 - 0.5,
                        ultimate ? Color.GOLD.deriveColor(0, 1, 1, 0.86) : Color.WHITE.deriveColor(0, 1, 1, 0.78)
                ));
            }
        }
    }

    private void specialPigeonSide(boolean ultimate) {
        int dir = horizontalInputDirection();
        if (dir == 0) {
            dir = facingDirection();
        }
        facingRight = dir > 0;
        pigeonRushGrounded = isOnGround();
        pigeonRushUltimate = ultimate;
        pigeonRushTimer = pigeonRushGrounded ? PIGEON_RUSH_GROUND_FRAMES : PIGEON_RUSH_AIR_FRAMES;
        Arrays.fill(pigeonRushHit, false);
        specialCooldown = 0;
        specialMaxCooldown = 0;
        attackAnimationTimer = Math.max(attackAnimationTimer, pigeonRushTimer);
        vx = dir * pigeonRushSpeed();
        if (!pigeonRushGrounded) {
            vy = Math.min(vy, ultimate ? 0.8 : 1.4);
        } else {
            vy = Math.min(vy, 0.0);
        }
        isBlocking = false;
        parryWindowFrames = 0;
        shieldStunFrames = 0;
    }

    private void specialPigeonUp(boolean ultimate) {
        if (pigeonUpSpecialUsed) {
            return;
        }
        int dir = horizontalInputDirection();
        if (dir != 0) {
            facingRight = dir > 0;
        } else {
            dir = facingDirection();
        }
        pigeonUpSpecialUsed = true;
        pigeonFlutterUltimate = ultimate;
        pigeonFlutterTimer = ultimate ? PIGEON_FLUTTER_ULTIMATE_FRAMES : PIGEON_FLUTTER_FRAMES;
        Arrays.fill(pigeonFlutterHit, false);
        specialCooldown = 0;
        specialMaxCooldown = 0;
        attackAnimationTimer = Math.max(attackAnimationTimer, pigeonFlutterTimer);
        canDoubleJump = false;
        vx = dir * (ultimate ? 3.2 : 2.2);
        vy = ultimate ? -16.4 : -14.6;
        if (ultimate) {
            game.triggerFlash(0.35, false);
        }
    }

    private void specialPigeonDown(boolean ultimate) {
        pigeonScavengeAirborne = !isOnGround();
        pigeonScavengeUltimate = ultimate;
        pigeonScavengeResolved = false;
        pigeonScavengeTimer = pigeonScavengeAirborne ? PIGEON_SCAVENGE_AIR_FRAMES : PIGEON_SCAVENGE_GROUND_FRAMES;
        specialCooldown = 0;
        specialMaxCooldown = 0;
        attackAnimationTimer = Math.max(attackAnimationTimer, pigeonScavengeTimer);
        vx *= pigeonScavengeAirborne ? 0.42 : 0.22;
        if (pigeonScavengeAirborne) {
            vy = Math.min(vy, ultimate ? 1.2 : 1.8);
        } else {
            vy = Math.min(vy, 0.0);
        }
        isBlocking = false;
        parryWindowFrames = 0;
        shieldStunFrames = 0;
        blockCooldown = 0;
    }

    private void handlePigeonSpecialState() {
        if (type != BirdGame3.BirdType.PIGEON) {
            return;
        }
        if (stunTime > 0.0) {
            resetPigeonSpecialState();
            return;
        }
        if (pigeonRushTimer > 0) {
            handlePigeonRush();
        }
        if (pigeonFlutterTimer > 0) {
            handlePigeonFlutter();
        }
        if (pigeonScavengeTimer > 0) {
            handlePigeonScavenge();
        }
    }

    private void handleRaptorSpecialState() {
        if (!isRaptor()) {
            return;
        }
        if (stunTime > 0.0) {
            resetRaptorSpecialState();
            return;
        }
        if (raptorCryTimer > 0) {
            handleRaptorCry();
        }
        if (raptorRushTimer > 0) {
            handleRaptorRush();
        }
        if (raptorClimbTimer > 0) {
            handleRaptorClimb();
        }
    }

    private void handleRaptorCry() {
        boolean eagle = type == BirdGame3.BirdType.EAGLE;
        vx *= eagle ? 0.84 : 0.9;
        if (!isOnGround()) {
            vy = Math.min(vy, eagle ? 1.6 : 1.1);
        }

        if ((raptorCryTimer & 1) != 0) {
            return;
        }

        int dir = facingDirection();
        Color particleColor = eagle ? Color.web("#F0C766") : Color.web("#FFB56E");
        for (int i = 0; i < 2; i++) {
            double spread = (Math.random() - 0.5) * (eagle ? 16.0 : 10.0);
            game.particles.add(new Particle(
                    bodyCenterX() + dir * (28 + Math.random() * 20),
                    bodyCenterY() - 8 + spread,
                    dir * (2.6 + Math.random() * 2.4),
                    spread * 0.08,
                    particleColor.deriveColor(0, 1, 1, 0.76)
            ));
        }
    }

    private void handleRaptorRush() {
        int dir = raptorRushDirection == 0 ? facingDirection() : raptorRushDirection;
        boolean eagle = type == BirdGame3.BirdType.EAGLE;
        vx = dir * raptorRushSpeed();
        if (raptorRushGrounded) {
            vy = Math.min(vy, 0.0);
        } else {
            vy = Math.min(vy, eagle ? 1.2 : 0.8);
        }

        for (Bird other : game.players) {
            if (!canDamageTarget(other)) continue;
            if (other.playerIndex < 0 || other.playerIndex >= raptorRushHit.length) continue;
            if (raptorRushHit[other.playerIndex]) continue;

            double dx = other.bodyCenterX() - bodyCenterX();
            double dy = other.bodyCenterY() - bodyCenterY();
            double forward = dx * dir;
            if (forward < -other.combatHalfWidth() * 0.35) continue;
            if (forward > (eagle ? 122.0 : 98.0) * sizeMultiplier + other.combatHalfWidth()) continue;
            if (Math.abs(dy) > (eagle ? 78.0 : 60.0) * sizeMultiplier + other.combatHalfHeight()) continue;

            boolean sweetspot = !eagle && forward > 72.0 * sizeMultiplier;
            int dmg = eagle
                    ? (raptorRushUltimate ? 13 : 10)
                    : (sweetspot ? (raptorRushUltimate ? 11 : 9) : (raptorRushUltimate ? 8 : 7));
            double oldHealth = other.health;
            int dealt = (int) applyDamageTo(other, dmg);
            if (dealt <= 0) continue;

            game.damageDealt[playerIndex] += dealt;
            game.recordSpecialImpact(playerIndex, dealt, true);
            if (other.health <= 0 && oldHealth > 0) {
                game.eliminations[playerIndex]++;
            }

            other.vx += dir * (sweetspot ? 13.0 : eagle ? 10.8 : 8.8);
            other.vy -= sweetspot ? 12.2 : eagle ? 9.4 : 8.6;
            raptorRushHit[other.playerIndex] = true;

            Color spark = sweetspot ? Color.web("#FFF0A6") : eagle ? Color.web("#E7B653") : Color.web("#FF9F68");
            for (int i = 0; i < (sweetspot ? 18 : 12); i++) {
                double angle = Math.random() * Math.PI * 2;
                game.particles.add(new Particle(
                        other.x + 40,
                        other.y + 40,
                        Math.cos(angle) * (3 + Math.random() * 5),
                        Math.sin(angle) * (3 + Math.random() * 5) - 2,
                        spark
                ));
            }
        }
    }

    private void handleRaptorClimb() {
        boolean eagle = type == BirdGame3.BirdType.EAGLE;
        int inputDir = horizontalInputDirection();
        if (inputDir != 0) {
            raptorClimbDirection = inputDir;
            facingRight = inputDir > 0;
        }
        double steer = eagle ? 0.36 : 0.58;
        double maxHorizontal = eagle ? 5.6 : 7.8;
        vx = Math.clamp(vx * (eagle ? 0.9 : 0.93) + inputDir * steer, -maxHorizontal, maxHorizontal);

        int strongLiftFrames = eagle
                ? (raptorClimbUltimate ? 10 : 8)
                : (raptorClimbUltimate ? 8 : 6);
        double lift = raptorClimbTimer > strongLiftFrames
                ? (eagle
                    ? (raptorClimbUltimate ? -13.8 : -12.2)
                    : (raptorClimbUltimate ? -12.8 : -11.1))
                : (eagle
                    ? (raptorClimbUltimate ? -10.0 : -8.7)
                    : (raptorClimbUltimate ? -8.7 : -7.5));
        vy = Math.min(vy, lift);

        for (Bird other : game.players) {
            if (!canDamageTarget(other)) continue;
            if (other.playerIndex < 0 || other.playerIndex >= raptorClimbHit.length) continue;
            if (raptorClimbHit[other.playerIndex]) continue;

            double dx = other.bodyCenterX() - bodyCenterX();
            double dy = other.bodyCenterY() - (bodyCenterY() - bodyHeight() * 0.16);
            if (Math.abs(dx) > (eagle ? 88.0 : 74.0) * sizeMultiplier + other.combatHalfWidth()) continue;
            if (Math.abs(dy) > (eagle ? 108.0 : 90.0) * sizeMultiplier + other.combatHalfHeight()) continue;

            double forward = dx * (raptorClimbDirection == 0 ? facingDirection() : raptorClimbDirection);
            boolean sweetspot = !eagle && forward > 44.0 * sizeMultiplier;
            int dmg = eagle
                    ? (raptorClimbUltimate ? 10 : 8)
                    : (sweetspot ? (raptorClimbUltimate ? 9 : 7) : (raptorClimbUltimate ? 8 : 6));
            double oldHealth = other.health;
            int dealt = (int) applyDamageTo(other, dmg);
            if (dealt <= 0) continue;

            game.damageDealt[playerIndex] += dealt;
            game.recordSpecialImpact(playerIndex, dealt, true);
            if (other.health <= 0 && oldHealth > 0) {
                game.eliminations[playerIndex]++;
            }

            double launchDir = dx == 0.0 ? (raptorClimbDirection == 0 ? facingDirection() : raptorClimbDirection) : Math.signum(dx);
            other.vx += launchDir * (sweetspot ? 8.0 : eagle ? 6.2 : 5.6);
            other.vy -= sweetspot ? 10.2 : eagle ? 8.8 : 7.8;
            raptorClimbHit[other.playerIndex] = true;

            Color spark = eagle ? Color.web("#F3D37D") : sweetspot ? Color.web("#FFF0A6") : Color.web("#FFB86F");
            for (int i = 0; i < (sweetspot ? 16 : 10); i++) {
                double angle = -Math.PI / 2 + (Math.random() - 0.5) * 1.3;
                game.particles.add(new Particle(
                        other.x + 40,
                        other.y + 40,
                        Math.cos(angle) * (4 + Math.random() * 5),
                        Math.sin(angle) * (7 + Math.random() * 7),
                        spark
                ));
            }
        }
    }

    private void handlePigeonRush() {
        int dir = facingDirection();
        double speed = pigeonRushSpeed();
        vx = dir * speed;
        if (!pigeonRushGrounded) {
            vy = Math.min(vy, pigeonRushUltimate ? 1.4 : 1.9);
        } else {
            vy = Math.min(vy, 0.0);
        }

        for (Bird other : game.players) {
            if (!canDamageTarget(other)) continue;
            if (other.playerIndex < 0 || other.playerIndex >= pigeonRushHit.length) continue;
            if (pigeonRushHit[other.playerIndex]) continue;

            double dx = other.bodyCenterX() - bodyCenterX();
            double dy = other.bodyCenterY() - bodyCenterY();
            if (dir > 0 && dx < -other.combatHalfWidth() * 0.35) continue;
            if (dir < 0 && dx > other.combatHalfWidth() * 0.35) continue;
            if (Math.abs(dx) > 108 + other.combatHalfWidth()) continue;
            if (Math.abs(dy) > 82 + other.combatHalfHeight()) continue;

            int dmg = pigeonRushDamage();
            double oldHealth = other.health;
            int dealt = (int) applyDamageTo(other, dmg);
            if (dealt <= 0) continue;

            game.damageDealt[playerIndex] += dealt;
            game.recordSpecialImpact(playerIndex, dealt, true);
            if (other.health <= 0 && oldHealth > 0) {
                game.eliminations[playerIndex]++;
            }

            other.vx += dir * pigeonRushHorizontalLaunch();
            other.vy -= pigeonRushVerticalLaunch();
            pigeonRushHit[other.playerIndex] = true;

            for (int i = 0; i < 14; i++) {
                double angle = Math.random() * Math.PI * 2;
                game.particles.add(new Particle(
                        other.x + 40,
                        other.y + 40,
                        Math.cos(angle) * (4 + Math.random() * 6),
                        Math.sin(angle) * (4 + Math.random() * 6) - 2.8,
                        pigeonRushUltimate ? Color.GOLD.deriveColor(0, 1, 1, 0.82) : Color.web("#CFD8DC").deriveColor(0, 1, 1, 0.78)
                ));
            }
        }

        for (int i = 0; i < 3; i++) {
            game.particles.add(new Particle(
                    x + bodyWidth() * (dir > 0 ? 0.2 : 0.8),
                    y + bodyHeight() * 0.78 + (Math.random() - 0.5) * 10,
                    -dir * (1.6 + Math.random() * 2.4),
                    -1.4 - Math.random() * 1.8,
                    pigeonRushUltimate ? Color.GOLD.deriveColor(0, 1, 1, 0.8) : Color.LIGHTGRAY.deriveColor(0, 1, 1, 0.62)
            ));
        }
    }

    private void handlePigeonFlutter() {
        int inputDir = horizontalInputDirection();
        if (inputDir != 0) {
            facingRight = inputDir > 0;
        }
        double steer = inputDir * (pigeonFlutterUltimate ? 0.9 : 0.72);
        double maxHorizontal = pigeonFlutterUltimate ? 6.6 : 5.2;
        vx = Math.clamp(vx * 0.84 + steer, -maxHorizontal, maxHorizontal);
        double lift = pigeonFlutterTimer > (pigeonFlutterUltimate ? 8 : 6)
                ? (pigeonFlutterUltimate ? -12.8 : -10.9)
                : (pigeonFlutterUltimate ? -9.5 : -8.0);
        vy = Math.min(vy, lift);

        for (Bird other : game.players) {
            if (!canDamageTarget(other)) continue;
            if (other.playerIndex < 0 || other.playerIndex >= pigeonFlutterHit.length) continue;
            if (pigeonFlutterHit[other.playerIndex]) continue;

            double dx = other.bodyCenterX() - bodyCenterX();
            double dy = other.bodyCenterY() - (bodyCenterY() - bodyHeight() * 0.1);
            if (Math.abs(dx) > 86 + other.combatHalfWidth()) continue;
            if (Math.abs(dy) > 100 + other.combatHalfHeight()) continue;

            int dmg = pigeonFlutterUltimate ? 9 : 6;
            double oldHealth = other.health;
            int dealt = (int) applyDamageTo(other, dmg);
            if (dealt <= 0) continue;

            game.damageDealt[playerIndex] += dealt;
            game.recordSpecialImpact(playerIndex, dealt, true);
            if (other.health <= 0 && oldHealth > 0) {
                game.eliminations[playerIndex]++;
            }

            double launchDir = dx == 0.0 ? facingDirection() : Math.signum(dx);
            other.vx += launchDir * (pigeonFlutterUltimate ? 7.2 : 5.6);
            other.vy -= pigeonFlutterUltimate ? 10.6 : 8.2;
            pigeonFlutterHit[other.playerIndex] = true;
        }

        for (int i = 0; i < 4; i++) {
            double angle = -Math.PI / 2 + (Math.random() - 0.5) * 1.4;
            game.particles.add(new Particle(
                    bodyCenterX() + (Math.random() - 0.5) * 26,
                    bodyCenterY() + 18 + (Math.random() - 0.5) * 18,
                    Math.cos(angle) * (2.4 + Math.random() * 3.2),
                    Math.sin(angle) * (3.0 + Math.random() * 5.0),
                    pigeonFlutterUltimate ? Color.GOLD.deriveColor(0, 1, 1, 0.85) : Color.web("#E3F2FD").deriveColor(0, 1, 1, 0.76)
            ));
        }
    }

    private void handlePigeonScavenge() {
        if (pigeonScavengeAirborne && isOnGround()) {
            pigeonScavengeAirborne = false;
        }
        if (!pigeonScavengeAirborne && !isOnGround()) {
            resetPigeonSpecialState();
            return;
        }

        if (pigeonScavengeAirborne) {
            vx *= 0.72;
            vy = Math.min(vy, pigeonScavengeUltimate ? 1.5 : 2.1);
            if (!pigeonScavengeResolved && pigeonScavengeTimer <= 4) {
                pigeonScavengeResolved = true;
                double centerX = bodyCenterX();
                double centerY = bodyBottomY() + 26;
                for (Bird other : game.players) {
                    if (!canDamageTarget(other)) continue;
                    double dx = other.bodyCenterX() - centerX;
                    double dy = other.bodyCenterY() - centerY;
                    if (Math.abs(dx) > 68 + other.combatHalfWidth()) continue;
                    if (Math.abs(dy) > 84 + other.combatHalfHeight()) continue;

                    int dmg = pigeonScavengeUltimate ? 14 : 10;
                    double oldHealth = other.health;
                    int dealt = (int) applyDamageTo(other, dmg);
                    if (dealt <= 0) continue;

                    game.damageDealt[playerIndex] += dealt;
                    game.recordSpecialImpact(playerIndex, dealt, true);
                    if (other.health <= 0 && oldHealth > 0) {
                        game.eliminations[playerIndex]++;
                    }

                    double launchDir = dx == 0.0 ? facingDirection() : Math.signum(dx);
                    other.vx += launchDir * (pigeonScavengeUltimate ? 5.2 : 4.0);
                    other.vy = Math.max(other.vy, pigeonScavengeUltimate ? 11.5 : 8.8);
                }
            }
        } else {
            vx *= 0.12;
            if ((pigeonScavengeTimer & 1) == 0) {
                for (int i = 0; i < 2; i++) {
                    double dustDir = Math.random() - 0.5;
                    game.particles.add(new Particle(
                            bodyCenterX() + dustDir * 26,
                            bodyBottomY() - 7 + Math.random() * 7,
                            dustDir * (1.4 + Math.random() * 1.6),
                            -1.0 - Math.random() * 1.5,
                            Color.web("#8D6E63").deriveColor(0, 1, 1, 0.72)
                    ));
                }
            }
            if (!pigeonScavengeResolved && pigeonScavengeTimer <= 1) {
                pigeonScavengeResolved = true;
                heal(pigeonScavengeUltimate ? 10.0 : 6.0);
                for (int i = 0; i < 16; i++) {
                    double angle = Math.random() * Math.PI * 2;
                    game.particles.add(new Particle(
                            bodyCenterX() + Math.cos(angle) * (8 + Math.random() * 14),
                            bodyBottomY() - 8 + Math.sin(angle) * (6 + Math.random() * 12),
                            Math.cos(angle) * (1.8 + Math.random() * 2.2),
                            Math.sin(angle) * (1.6 + Math.random() * 2.4) - 0.8,
                            pigeonScavengeUltimate ? Color.GOLD.deriveColor(0, 1, 1, 0.84) : Color.web("#A5D6A7").deriveColor(0, 1, 1, 0.78)
                    ));
                }
            }
        }

        for (int i = 0; i < 2; i++) {
            game.particles.add(new Particle(
                    bodyCenterX() + (Math.random() - 0.5) * 22,
                    bodyBottomY() - (pigeonScavengeAirborne ? -8 : 0),
                    (Math.random() - 0.5) * 1.6,
                    -0.8 - Math.random() * 1.2,
                    pigeonScavengeUltimate ? Color.GOLD.deriveColor(0, 1, 1, 0.78) : Color.web("#B0BEC5").deriveColor(0, 1, 1, 0.7)
            ));
        }
    }

    private void specialRaptorNeutral(boolean ultimate) {
        boolean eagle = type == BirdGame3.BirdType.EAGLE;
        int dir = horizontalInputDirection();
        if (dir != 0) {
            facingRight = dir > 0;
        }
        dir = facingDirection();

        raptorCryUltimate = ultimate;
        raptorCryTimer = eagle
                ? (ultimate ? EAGLE_CRY_ULTIMATE_FRAMES : EAGLE_CRY_FRAMES)
                : (ultimate ? FALCON_CRY_ULTIMATE_FRAMES : FALCON_CRY_FRAMES);
        raptorCryReuseTimer = raptorCryReuseFrames(ultimate);
        attackAnimationTimer = Math.max(attackAnimationTimer, raptorCryTimer);
        vx *= eagle ? 0.36 : 0.52;
        if (!isOnGround()) {
            vy = Math.min(vy, eagle ? 1.4 : 0.9);
        }

        double centerX = bodyCenterX() + dir * bodyWidth() * 0.55;
        double centerY = bodyCenterY() - 8.0 * sizeMultiplier;
        for (Bird other : game.players) {
            if (!canDamageTarget(other)) continue;

            double dx = other.bodyCenterX() - centerX;
            double forward = dx * dir;
            if (forward < -other.combatHalfWidth() * 0.2) continue;

            double dy = other.bodyCenterY() - centerY;
            double reach = eagle ? (ultimate ? 170.0 : 152.0) : (ultimate ? 160.0 : 146.0);
            if (forward > reach + other.combatHalfWidth()) continue;

            double verticalAllowance = eagle
                    ? 46.0 + Math.max(0.0, forward) * 0.28
                    : 24.0 + Math.max(0.0, forward) * 0.16;
            if (Math.abs(dy) > verticalAllowance * sizeMultiplier + other.combatHalfHeight()) continue;

            boolean sweetspot = !eagle && forward > 92.0 * sizeMultiplier;
            int dmg = eagle
                    ? (ultimate ? 10 : 8)
                    : (sweetspot ? (ultimate ? 10 : 8) : (ultimate ? 7 : 5));
            double oldHealth = other.health;
            int dealt = (int) applyDamageTo(other, dmg);
            if (dealt <= 0) continue;

            game.damageDealt[playerIndex] += dealt;
            game.recordSpecialImpact(playerIndex, dealt, true);
            if (other.health <= 0 && oldHealth > 0) {
                game.eliminations[playerIndex]++;
            }

            other.vx += dir * (sweetspot ? 8.4 : eagle ? 6.5 : 5.2);
            other.vy -= sweetspot ? 6.4 : eagle ? 4.8 : 4.0;
        }

        Color primary = eagle ? Color.web("#E3B74E") : Color.web("#FF9E57");
        Color secondary = eagle ? Color.web("#FFF4BC") : Color.web("#FFE0A5");
        for (int ring = 0; ring < 3; ring++) {
            double ringReach = 18 + ring * 28;
            for (int i = 0; i < 8; i++) {
                double spread = (i - 3.5) * (eagle ? 0.12 : 0.08);
                game.particles.add(new Particle(
                        centerX + dir * (ringReach + i * 6),
                        centerY + spread * 26,
                        dir * (2.8 + ring * 1.2 + i * 0.2),
                        spread * (eagle ? 2.3 : 1.5),
                        (ring & 1) == 0 ? primary.deriveColor(0, 1, 1, 0.82) : secondary.deriveColor(0, 1, 1, 0.72)
                ));
            }
        }
    }

    private void specialRaptorSide(boolean ultimate) {
        boolean eagle = type == BirdGame3.BirdType.EAGLE;
        int dir = horizontalInputDirection();
        if (dir == 0) {
            dir = facingDirection();
        }
        facingRight = dir > 0;
        raptorRushDirection = dir;
        raptorRushGrounded = isOnGround();
        raptorRushUltimate = ultimate;
        raptorRushTimer = eagle
                ? (raptorRushGrounded ? EAGLE_RUSH_GROUND_FRAMES : EAGLE_RUSH_AIR_FRAMES)
                : (raptorRushGrounded ? FALCON_RUSH_GROUND_FRAMES : FALCON_RUSH_AIR_FRAMES);
        if (ultimate) {
            raptorRushTimer += eagle ? 2 : 1;
        }
        Arrays.fill(raptorRushHit, false);
        raptorRushReuseTimer = raptorRushReuseFrames(ultimate);
        attackAnimationTimer = Math.max(attackAnimationTimer, raptorRushTimer);
        vx = dir * raptorRushSpeed();
        if (raptorRushGrounded) {
            vy = Math.min(vy, 0.0);
        } else {
            vy = Math.min(vy, eagle ? 1.0 : 0.4);
        }
        isBlocking = false;
        parryWindowFrames = 0;
        shieldStunFrames = 0;
    }

    private void specialRaptorUp(boolean ultimate) {
        if (raptorUpSpecialUsed) {
            return;
        }
        boolean eagle = type == BirdGame3.BirdType.EAGLE;
        int dir = horizontalInputDirection();
        if (dir != 0) {
            facingRight = dir > 0;
        } else {
            dir = facingDirection();
        }
        raptorClimbDirection = dir;
        raptorUpSpecialUsed = true;
        raptorClimbUltimate = ultimate;
        raptorClimbTimer = eagle
                ? (ultimate ? EAGLE_CLIMB_ULTIMATE_FRAMES : EAGLE_CLIMB_FRAMES)
                : (ultimate ? FALCON_CLIMB_ULTIMATE_FRAMES : FALCON_CLIMB_FRAMES);
        Arrays.fill(raptorClimbHit, false);
        attackAnimationTimer = Math.max(attackAnimationTimer, raptorClimbTimer);
        canDoubleJump = false;
        vx = dir * (eagle ? (ultimate ? 3.8 : 3.1) : (ultimate ? 6.3 : 5.5));
        vy = eagle ? (ultimate ? -17.4 : -15.6) : (ultimate ? -16.2 : -14.2);
        isBlocking = false;
        parryWindowFrames = 0;
        shieldStunFrames = 0;
    }

    private void specialEagle(boolean ultimate) {
        boolean grounded = isOnGround();
        diveTimer = ultimate ? EAGLE_DIVE_ULTIMATE_FRAMES : EAGLE_DIVE_FRAMES;
        specialCooldown = 780;
        specialMaxCooldown = 780;
        eagleDiveActive = true;
        eagleAscentActive = false;
        eagleAscentFrames = 0;
        Arrays.fill(eagleAscentHit, false);

        game.shakeIntensity = Math.max(game.shakeIntensity, ultimate ? 20 : 16);
        game.hitstopFrames = Math.max(game.hitstopFrames, ultimate ? 11 : 9);
        game.addToKillFeed("SKREEEEEEEE!!! " + shortName() + (ultimate ? " ULT DIVES FROM THE HEAVENS!" : " IS DIVING FROM THE HEAVENS!"));

        int trailCount = scaledParticleCount(ultimate ? 140 : 100);
        for (int i = 0; i < trailCount; i++) {
            double angle = Math.atan2(vy, vx) + Math.PI;
            double dist = i * 10;
            game.particles.add(new Particle(
                    x + 40 + Math.cos(angle) * dist,
                    y + 40 + Math.sin(angle) * dist,
                    0, 0,
                    Color.CRIMSON.deriveColor(0, 1, 1, 1.0 - i / (double) trailCount)
            ));
        }

        double predictX = x + vx * 40;
        int warningCount = scaledParticleCount(31);
        for (int i = 0; i < warningCount; i++) {
            double progress = warningCount == 1 ? 0.0 : (i / (double) (warningCount - 1));
            double laneOffset = -15.0 + progress * 30.0;
            game.particles.add(new Particle(predictX + laneOffset * 60.0, BirdGame3.GROUND_Y - 20, 0, -5 - Math.random() * 8, Color.ORANGERED.brighter()));
        }

        if (grounded) {
            vy = ultimate ? -12 : -8;
            vx *= ultimate ? 0.45 : 0.35;
            eagleDiveCountdown = ultimate ? EAGLE_DIVE_GROUND_ULTIMATE_STARTUP_FRAMES : EAGLE_DIVE_GROUND_STARTUP_FRAMES;
        } else {
            vy = Math.max(vy, ultimate ? 18 : 14);
            vx *= ultimate ? 0.82 : 0.7;
            eagleDiveCountdown = 0;
        }
        attackAnimationTimer = Math.max(attackAnimationTimer, 16);
        isBlocking = false;
        parryWindowFrames = 0;
        shieldStunFrames = 0;
    }

    private void specialFalcon(boolean ultimate) {
        boolean grounded = isOnGround();
        diveTimer = ultimate ? FALCON_DIVE_ULTIMATE_FRAMES : FALCON_DIVE_FRAMES;
        specialCooldown = 660;
        specialMaxCooldown = 660;
        eagleDiveActive = true;
        eagleAscentActive = false;
        eagleAscentFrames = 0;
        Arrays.fill(eagleAscentHit, false);

        game.shakeIntensity = Math.max(game.shakeIntensity, ultimate ? 16 : 12);
        game.hitstopFrames = Math.max(game.hitstopFrames, ultimate ? 9 : 7);
        game.addToKillFeed(shortName() + (ultimate ? " ULT FALCON DIVE ENGAGED!" : " LOCKED IN A FALCON DIVE!"));

        int trailCount = scaledParticleCount(ultimate ? 110 : 78);
        for (int i = 0; i < trailCount; i++) {
            double angle = Math.atan2(vy, vx) + Math.PI;
            double dist = i * 7.5;
            Color c = i % 2 == 0 ? Color.web("#FF7043") : Color.web("#FFE082");
            game.particles.add(new Particle(
                    x + 40 + Math.cos(angle) * dist,
                    y + 40 + Math.sin(angle) * dist,
                    0, 0,
                    c.deriveColor(0, 1, 1, 1.0 - i / (double) trailCount)
            ));
        }

        if (grounded) {
            vy = ultimate ? -11 : -8;
            vx *= ultimate ? 0.55 : 0.45;
            eagleDiveCountdown = ultimate ? FALCON_DIVE_GROUND_ULTIMATE_STARTUP_FRAMES : FALCON_DIVE_GROUND_STARTUP_FRAMES;
        } else {
            vy = Math.max(vy, ultimate ? 17 : 13);
            vx += (facingRight ? 1 : -1) * (ultimate ? 12 : 8);
            eagleDiveCountdown = 0;
        }
        attackAnimationTimer = Math.max(attackAnimationTimer, 14);
        isBlocking = false;
        parryWindowFrames = 0;
        shieldStunFrames = 0;
    }

    private void specialPhoenix(boolean ultimate) {
        canDoubleJump = true;
        vx += (facingRight ? 1 : -1) * (ultimate ? 16 : 12);
        vy = Math.min(vy - (ultimate ? 11 : 8), ultimate ? -12 : -10);

        double buffScale = ultimate ? 1.45 : 1.2;
        int buffTimer = ultimate ? 320 : 220;
        powerMultiplier = Math.max(powerMultiplier, basePowerMultiplier * buffScale);
        rageTimer = Math.max(rageTimer, buffTimer);
        speedMultiplier = Math.max(speedMultiplier, baseSpeedMultiplier * buffScale);
        speedTimer = Math.max(speedTimer, buffTimer);

        specialCooldown = 600;
        specialMaxCooldown = 600;
        phoenixAfterburnTimer = ultimate ? 70 : 48;
        Arrays.fill(phoenixAfterburnHitCooldown, 0);
        game.addToKillFeed(shortName() + (ultimate ? " ULT REBIRTH BLAZE!" : " UNLEASHED REBIRTH BLAZE!"));
        game.shakeIntensity = Math.max(game.shakeIntensity, ultimate ? 20 : 16);
        game.hitstopFrames = Math.max(game.hitstopFrames, ultimate ? 10 : 8);

        for (Bird other : game.players) {
            if (!canDamageTarget(other)) continue;
            double dx = other.bodyCenterX() - bodyCenterX();
            double dy = other.bodyCenterY() - bodyCenterY();
            double centerDist = Math.hypot(dx, dy);
            double dist = Math.max(0.0, centerDist - other.combatRadius());
            double radius = ultimate ? 360 : 300;
            if (dist > radius) continue;

            int dmg = dist < radius * 0.5 ? (ultimate ? 24 : 16)
                    : (dist < radius * 0.75 ? (ultimate ? 18 : 12) : (ultimate ? 14 : 9));
            double oldHealth = other.health;
            int dealt = (int) applyDamageTo(other, dmg);
            game.damageDealt[playerIndex] += dealt;
            game.recordSpecialImpact(playerIndex, dealt, dealt > 0);
            if (other.health <= 0 && oldHealth > 0) game.eliminations[playerIndex]++;

            double safeDist = Math.max(0.001, centerDist);
            other.vx += dx / safeDist * 12;
            other.vy -= 7;
        }

        // Flame lances shoot outward from Phoenix's body.
        int rayCount = Math.max(6, scaledParticleCount(ultimate ? 14 : 12));
        int segmentCount = Math.max(4, scaledParticleCount(8));
        for (int ray = 0; ray < rayCount; ray++) {
            double baseAngle = ray / (double) rayCount * Math.PI * 2;
            for (int seg = 0; seg < segmentCount; seg++) {
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

    private void specialHummingbird(boolean ultimate) {
        hummingFrenzyTimer = ultimate ? 150 : 105;
        Arrays.fill(hummingFrenzyHitCooldown, 0);
        canDoubleJump = true;
        vy = Math.min(vy - type.jumpHeight * (ultimate ? 0.6 : 0.45),
                -type.jumpHeight * (ultimate ? 1.1 : 0.9));
        vx += (facingRight ? 1 : -1) * (ultimate ? 12 : 9);
        heal(ultimate ? 12 : 6);
        if (ultimate) {
            speedMultiplier = Math.max(speedMultiplier, baseSpeedMultiplier * 1.3);
            speedTimer = Math.max(speedTimer, 160);
        }
        specialCooldown = 390;
        specialMaxCooldown = 390;
        game.addToKillFeed(shortName() + (ultimate ? " ULT NECTAR FRENZY!" : " UNLEASHED NECTAR FRENZY!"));
        int particleCount = scaledParticleCount(ultimate ? 80 : 55);
        for (int i = 0; i < particleCount; i++) {
            Color c = Math.random() < 0.55 ? Color.CYAN.brighter() : (ultimate ? Color.GOLD.brighter() : Color.YELLOW.brighter());
            game.particles.add(new Particle(x + 40, y + 40,
                    (Math.random() - 0.5) * 22, -8 - Math.random() * 12,
                    c));
        }
    }

    private void specialTurkey(boolean ultimate) {
        vy = -type.jumpHeight * (ultimate ? 1.75 : 1.45);
        isGroundPounding = true;
        if (ultimate) {
            powerMultiplier = Math.max(powerMultiplier, basePowerMultiplier * 1.2);
            rageTimer = Math.max(rageTimer, 180);
        }
        specialCooldown = 450;
        specialMaxCooldown = 450;
    }

    private void specialRoadrunner(boolean ultimate) {
        boolean grounded = isOnGround();
        double dir = facingRight ? 1.0 : -1.0;

        if (ultimate) {
            specialCooldown = 1260;
            specialMaxCooldown = 1260;
            roadrunnerSandstormTimer = Math.max(roadrunnerSandstormTimer, ROADRUNNER_SANDSTORM_FRAMES);
            roadrunnerSandGustTimer = 0;
            Arrays.fill(roadrunnerSandHitCooldown, 0);
            speedMultiplier = Math.max(speedMultiplier, baseSpeedMultiplier * ROADRUNNER_SANDSTORM_SPEED_SCALE);
            speedTimer = Math.max(speedTimer, ROADRUNNER_SANDSTORM_FRAMES + 45);
            hoverRegenTimer = Math.max(hoverRegenTimer, ROADRUNNER_SANDSTORM_FRAMES);
            hoverRegenMultiplier = Math.max(hoverRegenMultiplier, 1.12);
            vx = dir * (grounded ? 24.0 : 18.0);
            vy = grounded ? Math.min(vy, -7.5) : Math.min(vy - 3.2, -11.0);
            game.addToKillFeed(shortName() + " ASCENDED IN A GODSTORM!");
            game.shakeIntensity = Math.max(game.shakeIntensity, 28);
            game.hitstopFrames = Math.max(game.hitstopFrames, 12);
            game.triggerFlash(0.45, false);
            unleashRoadrunnerSandGust(true);
            return;
        }

        specialCooldown = 330;
        specialMaxCooldown = 330;
        speedMultiplier = Math.max(speedMultiplier, baseSpeedMultiplier * 1.35);
        speedTimer = Math.max(speedTimer, 180);
        vx = dir * (grounded ? 26.0 : 20.0);
        vy = grounded ? Math.min(vy, -2.6) : Math.min(vy - 1.5, -5.0);
        game.addToKillFeed(shortName() + " HIT DUST SPRINT!");
        game.shakeIntensity = Math.max(game.shakeIntensity, 14);
        game.hitstopFrames = Math.max(game.hitstopFrames, 8);

        for (Bird other : game.players) {
            if (!canDamageTarget(other)) continue;
            double dx = other.bodyCenterX() - bodyCenterX();
            double dy = other.bodyCenterY() - bodyCenterY();
            if (Math.abs(dy) > 95 + other.combatHalfHeight()) continue;
            if (facingRight) {
                if (dx < -60 || dx > 320 + other.combatHalfWidth()) continue;
            } else if (dx > 60 || dx < -(320 + other.combatHalfWidth())) {
                continue;
            }

            int dmg = 10 + random.nextInt(4);
            double oldHealth = other.health;
            int dealt = (int) applyDamageTo(other, dmg);
            if (dealt <= 0) continue;

            game.damageDealt[playerIndex] += dealt;
            game.recordSpecialImpact(playerIndex, dealt, true);
            if (other.health <= 0 && oldHealth > 0) game.eliminations[playerIndex]++;

            other.vx += dir * 16.0;
            other.vy -= 6.0;
        }

        int particleCount = scaledParticleCount(70);
        for (int i = 0; i < particleCount; i++) {
            double spread = (Math.random() - 0.5) * 0.7;
            double speed = 4 + Math.random() * 9;
            Color c = Math.random() < 0.6 ? Color.web("#D9A04D") : Color.web("#E2BF7B");
            game.particles.add(new Particle(
                    bodyCenterX() - dir * (10 + Math.random() * 20),
                    y + 64 + (Math.random() - 0.5) * 18,
                    dir * (5 + Math.random() * 10),
                    -2.2 + spread * speed,
                    c.deriveColor(0, 1, 1, 0.82)
            ));
        }
    }

    private void handleRoadrunnerSandstorm() {
        if (!roadrunnerSandstormActive()) {
            return;
        }

        speedMultiplier = Math.max(speedMultiplier, baseSpeedMultiplier * ROADRUNNER_SANDSTORM_SPEED_SCALE);
        double centerX = bodyCenterX();
        double centerY = bodyCenterY();
        double intensity = Math.clamp(roadrunnerSandstormTimer / (double) ROADRUNNER_SANDSTORM_FRAMES, 0.32, 1.0);
        int particleCount = Math.max(3, scaledParticleCount(5));
        for (int i = 0; i < particleCount; i++) {
            double angle = random.nextDouble() * Math.PI * 2;
            double ring = 20.0 + random.nextDouble() * (105.0 + intensity * 90.0);
            double swirl = 2.6 + random.nextDouble() * 5.5 + intensity * 1.2;
            Color sand = random.nextDouble() < 0.72 ? Color.web("#E8C06A") : Color.web("#C68A3A");
            game.particles.add(new Particle(
                    centerX + Math.cos(angle) * ring * 0.32,
                    centerY + Math.sin(angle) * ring * 0.22,
                    Math.cos(angle + Math.PI / 2.0) * swirl + vx * 0.12,
                    Math.sin(angle + Math.PI / 2.0) * swirl - 1.2 - intensity,
                    sand.deriveColor(0, 1, 1, 0.56 + intensity * 0.22)
            ));
        }

        if (roadrunnerSandGustTimer <= 0) {
            roadrunnerSandGustTimer = ROADRUNNER_GUST_INTERVAL;
            unleashRoadrunnerSandGust(false);
        }
    }

    private void unleashRoadrunnerSandGust(boolean openingBurst) {
        double centerX = bodyCenterX();
        double centerY = bodyCenterY();
        double radius = openingBurst ? 440.0 : ROADRUNNER_SANDSTORM_GUST_RADIUS;
        double forwardBias = facingRight ? 1.0 : -1.0;

        for (Bird other : game.players) {
            if (!canDamageTarget(other)) continue;
            double dx = other.bodyCenterX() - centerX;
            double dy = other.bodyCenterY() - centerY;
            double dist = Math.hypot(dx, dy);
            double maxReach = radius + other.combatRadius();
            if (dist > maxReach) continue;

            double safeDist = Math.max(0.001, dist);
            double proximity = 1.0 - Math.clamp(dist / maxReach, 0.0, 1.0);
            double push = (openingBurst ? 14.0 : 7.0) + proximity * (openingBurst ? 14.0 : 9.0);
            other.vx += dx / safeDist * push + forwardBias * (openingBurst ? 3.2 : 1.4);
            other.vy -= (openingBurst ? 4.5 : 2.0) + proximity * (openingBurst ? 6.0 : 4.0);

            boolean canHit = openingBurst || roadrunnerSandHitCooldown[other.playerIndex] <= 0;
            if (!canHit) {
                continue;
            }

            int dmg;
            if (openingBurst) {
                dmg = dist < 170.0 ? 12 : (dist < 300.0 ? 8 : 5);
            } else {
                dmg = dist < 170.0 ? 5 : 3;
            }
            double oldHealth = other.health;
            int dealt = (int) applyDamageTo(other, dmg);
            if (dealt <= 0) {
                continue;
            }

            roadrunnerSandHitCooldown[other.playerIndex] = ROADRUNNER_GUST_HIT_COOLDOWN;
            game.damageDealt[playerIndex] += dealt;
            game.recordSpecialImpact(playerIndex, dealt, true);
            if (other.health <= 0 && oldHealth > 0) {
                game.eliminations[playerIndex]++;
            }
        }

        int particleCount = scaledParticleCount(openingBurst ? 180 : 72);
        for (int i = 0; i < particleCount; i++) {
            double angle = random.nextDouble() * Math.PI * 2;
            double ring = 36.0 + random.nextDouble() * radius;
            double tangential = (3.5 + random.nextDouble() * 8.0) * (facingRight ? 1.0 : -1.0);
            Color sand = random.nextDouble() < 0.72 ? Color.web("#E6C46F") : Color.web("#BA7B31");
            game.particles.add(new Particle(
                    centerX + Math.cos(angle) * ring * 0.24,
                    centerY + Math.sin(angle) * ring * 0.16,
                    Math.cos(angle) * (openingBurst ? 8.5 : 5.2) + tangential * 0.55,
                    Math.sin(angle) * (openingBurst ? 6.0 : 3.4) - (openingBurst ? 2.8 : 1.6),
                    sand.deriveColor(0, 1, 1, openingBurst ? 0.84 : 0.72)
            ));
        }
    }

    private void specialPenguin(boolean ultimate) {
        boolean grounded = isOnGround();
        vx = (facingRight ? 1 : -1) * (grounded ? (ultimate ? 21.0 : 17.0) : (ultimate ? 17.0 : 14.0));
        // Mobility special: gives penguin a reliable vertical burst to reach upper lanes.
        vy = grounded ? -type.jumpHeight * (ultimate ? 2.4 : 2.0) : Math.min(vy - (ultimate ? 13.0 : 11.0), -(ultimate ? 20.0 : 16.0));
        canDoubleJump = true;
        specialCooldown = 135;
        specialMaxCooldown = 135;
        penguinIceFxTimer = ultimate ? 100 : 70;
        penguinDashDamageTimer = ultimate ? 26 : 18;
        Arrays.fill(penguinDashHit, false);
        game.addToKillFeed(shortName() + (ultimate ? " ULT ICE JUMP DASH!" : " ICE JUMP DASH!"));
        int particleCount = scaledParticleCount(ultimate ? 130 : 90);
        for (int i = 0; i < particleCount; i++) {
            game.particles.add(new Particle(x + 40 + (Math.random() - 0.5) * 100,
                    y + 70, (Math.random() - 0.5) * 10, -5 - Math.random() * 9,
                    Color.CYAN.deriveColor(0, 1, 1, 0.7)));
        }
    }

    private void specialShoebill(boolean ultimate) {
        for (Bird other : game.players) {
            if (!canDamageTarget(other)) continue;
            double dist = combatDistanceTo(other);
            if (dist < (ultimate ? 420 : 320) + other.combatRadius()) {
                other.applyStun(ultimate ? 180 : 120);
            }
        }
        specialCooldown = 780;
        specialMaxCooldown = 780;
        game.addToKillFeed(shortName() + (ultimate ? " ULT DEATH STARE! Wider stun!" : " DEATH STARE! Nearby birds stunned!"));
        game.shakeIntensity = Math.max(game.shakeIntensity, ultimate ? 26 : 20);
    }

    private void specialMockingbird(boolean ultimate) {
        loungeActive = true;
        loungeX = x + 40;
        loungeY = y + 40;
        loungeMaxHealth = ultimate ? 200 : LOUNGE_MAX_HEALTH;
        loungeRoyal = ultimate;
        loungeHealth = loungeMaxHealth;
        specialCooldown = 660;
        specialMaxCooldown = 660;
        game.addToKillFeed(shortName() + (ultimate ? " ROYAL LOUNGE OPENED!" : " opened the LOUNGE!"));
    }

    private void specialRazorbill(boolean ultimate) {
        specialCooldown = 780;
        specialMaxCooldown = 780;

        game.shakeIntensity = Math.max(game.shakeIntensity, ultimate ? 22 : 18);
        game.hitstopFrames = Math.max(game.hitstopFrames, ultimate ? 12 : 10);
        game.addToKillFeed("RAZOR DASH! " + shortName() + (ultimate ? " ULT PIERCES THE SKY!" : " PIERCES THE SKY!"));

        double dirX = vx;
        double dirY = vy;
        double mag = Math.hypot(dirX, dirY);
        if (mag < 0.35) {
            dirX = facingRight ? 1 : -1;
            dirY = 0;
            mag = 1.0;
        }
        double dashSpeed = Math.max(12.0, RAZORBILL_DASH_SPEED * (ultimate ? 1.35 : 1.0) * speedMultiplier);
        razorbillDashVX = dirX / mag * dashSpeed;
        razorbillDashVY = dirY / mag * dashSpeed;
        vx = razorbillDashVX;
        vy = razorbillDashVY;
        bladeStormFrames = ultimate ? (int) Math.round(RAZORBILL_DASH_FRAMES * 1.4) : RAZORBILL_DASH_FRAMES;
        Arrays.fill(razorbillDashHit, false);

        double trailAngle = Math.atan2(razorbillDashVY, razorbillDashVX);
        int trailCount = scaledParticleCount(ultimate ? 90 : 60);
        for (int i = 0; i < trailCount; i++) {
            double angle = trailAngle + (Math.random() - 0.5) * 0.7;
            double speed = 6 + Math.random() * 10;
            double back = 20 + Math.random() * 70;
            game.particles.add(new Particle(
                    x + 40 - Math.cos(angle) * back,
                    y + 40 - Math.sin(angle) * back,
                    Math.cos(angle) * speed,
                    Math.sin(angle) * speed,
                    ultimate ? Color.GOLD.brighter() : Color.CYAN.brighter()
            ));
        }
    }

    private void specialGrinchhawk(boolean ultimate) {
        int stolen = 0;
        for (Bird other : game.players) {
            if (!canDamageTarget(other)) continue;
            int base = ultimate ? 12 : 8;
            int bonus = (health > 80 ? (ultimate ? 6 : 4) : 0);
            int take = (int) Math.min(base + bonus, other.health);
            double oldHealth = other.health;
            int dealt = (int) applyDamageTo(other, take);
            stolen += dealt;
            game.damageDealt[playerIndex] += dealt;
            game.recordSpecialImpact(playerIndex, dealt, dealt > 0);
            if (other.health <= 0 && oldHealth > 0) game.eliminations[playerIndex]++;
            game.addToKillFeed(shortName() + " STOLE " + dealt + " HP from " + other.shortName() + "!");
        }
        heal(stolen);
        specialCooldown = 840;
        specialMaxCooldown = 840;
        game.shakeIntensity = Math.max(game.shakeIntensity, ultimate ? 26 : 20);
    }

    private void specialVulture(boolean ultimate) {
        if (isNullRockForm()) {
            specialNullRock(ultimate);
            return;
        }
        crowSwarmCooldown = 1080;
        specialCooldown = 1080;
        specialMaxCooldown = 1080;
        game.addToKillFeed(shortName() + (ultimate ? " ULT MURDER UNLEASHED!" : " SUMMONS THE MURDER!"));

        int crowCount = (ultimate ? 12 : 8) + random.nextInt(ultimate ? 7 : 5);
        for (int i = 0; i < crowCount; i++) {
            double angle = Math.random() * Math.PI * 2;
            double dist = 300 + Math.random() * 1200;
            double spawnX = x + 40 + Math.cos(angle) * dist;
            double spawnY = y + 40 + Math.sin(angle) * dist;

            CrowMinion crow = new CrowMinion(spawnX, spawnY, null);
            crow.owner = this;
            crow.life = 1;
            crow.hasCrown = ultimate;
            game.crowMinions.add(crow);
        }

        game.shakeIntensity = Math.max(game.shakeIntensity, ultimate ? 22 : 18);
        game.hitstopFrames = Math.max(game.hitstopFrames, ultimate ? 14 : 12);
        carrionSwarmTimer = ultimate ? 150 : 100;

        int particleCount = scaledParticleCount(ultimate ? 260 : 200);
        for (int i = 0; i < particleCount; i++) {
            double angle = Math.random() * Math.PI * 2;
            double speed = 8 + Math.random() * 16;
            game.particles.add(new Particle(x + 40, y + 40,
                    Math.cos(angle) * speed,
                    Math.sin(angle) * speed - 6,
                    ultimate ? Color.BLACK : Color.rgb(10, 0, 20)));
        }
    }

    private void specialNullRock(boolean ultimate) {
        crowSwarmCooldown = ultimate ? 960 : 1080;
        specialCooldown = crowSwarmCooldown;
        specialMaxCooldown = crowSwarmCooldown;
        game.summonNullRockSpecialFlock(this, ultimate);

        game.shakeIntensity = Math.max(game.shakeIntensity, ultimate ? 30 : 24);
        game.hitstopFrames = Math.max(game.hitstopFrames, ultimate ? 18 : 14);
        carrionSwarmTimer = ultimate ? 240 : 180;

        int particleCount = scaledParticleCount(ultimate ? 360 : 260);
        for (int i = 0; i < particleCount; i++) {
            double angle = Math.random() * Math.PI * 2;
            double speed = 9 + Math.random() * 18;
            Color shade = switch (i % 3) {
                case 1 -> Color.web("#16020C");
                case 2 -> Color.web("#25102B");
                default -> Color.BLACK;
            };
            game.particles.add(new Particle(
                    x + 40,
                    y + 40,
                    Math.cos(angle) * speed,
                    Math.sin(angle) * speed - 6,
                    shade.deriveColor(0, 1, 1, ultimate ? 0.95 : 0.82)
            ));
        }
    }

    private void specialRooster(boolean ultimate) {
        specialCooldown = 900;
        specialMaxCooldown = 900;
        game.addToKillFeed(shortName() + (ultimate ? " ULT COOP CALL!" : " calls the chicks!"));

        double centerX = x + 40 * sizeMultiplier;
        double spawnY = y + 50 * sizeMultiplier;
        for (int i = 0; i < 3; i++) {
            double offset = (i - 1) * 36 * sizeMultiplier;
            ChickMinion chick = new ChickMinion(centerX + offset, spawnY, i, ultimate, this);
            chick.x -= chick.width * 0.5;
            chick.onGround = isOnGround();
            game.chickMinions.add(chick);
        }

        int particleCount = scaledParticleCount(ultimate ? 180 : 120);
        Color burst = ultimate ? Color.GOLD : Color.ORANGE;
        for (int i = 0; i < particleCount; i++) {
            double angle = Math.random() * Math.PI * 2;
            double speed = 5 + Math.random() * 10;
            game.particles.add(new Particle(x + 40, y + 40,
                    Math.cos(angle) * speed,
                    Math.sin(angle) * speed - 4,
                    burst.deriveColor(0, 1, 1, 0.85)));
        }

        game.shakeIntensity = Math.max(game.shakeIntensity, ultimate ? 20 : 14);
        game.hitstopFrames = Math.max(game.hitstopFrames, ultimate ? 10 : 8);
    }

    private void specialOpiumBird(boolean ultimate) {
        leanTimer = ultimate ? 520 : 360;
        leanCooldown = 840;
        specialCooldown = 840;
        specialMaxCooldown = 840;
        if (ultimate) {
            powerMultiplier = Math.max(powerMultiplier, basePowerMultiplier * 1.25);
            rageTimer = Math.max(rageTimer, 240);
        }
        game.shakeIntensity = Math.max(game.shakeIntensity, ultimate ? 26 : 20);
        game.hitstopFrames = Math.max(game.hitstopFrames, ultimate ? 18 : 15);
        int particleCount = scaledParticleCount(ultimate ? 220 : 150);
        for (int i = 0; i < particleCount; i++) {
            double angle = Math.random() * Math.PI * 2;
            game.particles.add(new Particle(x + 40, y + 40,
                    Math.cos(angle) * (2 + Math.random() * 10),
                    Math.sin(angle) * (2 + Math.random() * 10) - 4,
                    (ultimate ? Color.GOLD : Color.PURPLE).deriveColor(0, 1, 1, 0.7)));
        }
    }

    private void specialHeisenbird(boolean ultimate) {
        leanTimer = ultimate ? 460 : 300;
        leanCooldown = 720;
        specialCooldown = 720;
        specialMaxCooldown = 720;
        if (ultimate) {
            powerMultiplier = Math.max(powerMultiplier, basePowerMultiplier * 1.2);
            rageTimer = Math.max(rageTimer, 220);
        }
        game.shakeIntensity = Math.max(game.shakeIntensity, ultimate ? 24 : 18);
        game.hitstopFrames = Math.max(game.hitstopFrames, ultimate ? 15 : 12);
        int particleCount = scaledParticleCount(ultimate ? 220 : 150);
        for (int i = 0; i < particleCount; i++) {
            double angle = Math.random() * Math.PI * 2;
            game.particles.add(new Particle(x + 40, y + 40,
                    Math.cos(angle) * (2 + Math.random() * 10),
                    Math.sin(angle) * (2 + Math.random() * 10) - 4,
                    (ultimate ? Color.GOLD : Color.web("#29B6F6")).deriveColor(0, 1, 1, 0.75)));
        }
    }

    private void specialTitmouse(boolean ultimate) {
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
            game.addToKillFeed(shortName() + (ultimate ? " tried ULT ZIP... but no target!" : " tried to ZIP... but no target!"));
            specialCooldown = 240;
            specialMaxCooldown = 240;
            return;
        }

        isZipping = true;
        zipTargetX = target.x;
        zipTargetY = target.y;
        zipTimer = 30;
        if (ultimate) {
            powerMultiplier = Math.max(powerMultiplier, basePowerMultiplier * 1.35);
            rageTimer = Math.max(rageTimer, 200);
        }

        specialCooldown = 780;
        specialMaxCooldown = 780;

        game.addToKillFeed(shortName() + (ultimate ? " ULT ZIPPED to " : " ZIPPED to ") + target.shortName() + "!");

        int particleCount = scaledParticleCount(ultimate ? 80 : 50);
        for (int i = 0; i < particleCount; i++) {
            double offset = i * 8;
            game.particles.add(new Particle(
                    x + 40 - vx * offset / 10,
                    y + 40 - vy * offset / 10,
                    (Math.random() - 0.5) * 8,
                    (Math.random() - 0.5) * 8 - 2,
                    (ultimate ? Color.GOLD : Color.SKYBLUE).deriveColor(0, 1, 1, 0.8 - i / 60.0)
            ));
        }
    }

    private void specialPelican(boolean ultimate) {
        Bird target = null;
        double bestDist = Double.MAX_VALUE;
        for (Bird b : game.players) {
            if (!canDamageTarget(b)) continue;
            double d = Math.hypot(b.x - x, b.y - y);
            if (d < bestDist && d < (ultimate ? 360 : 280)) {
                bestDist = d;
                target = b;
            }
        }
        if (target != null) {
            plungeTimer = ultimate ? 60 : 45;
            sizeMultiplier *= ultimate ? 1.28 : 1.18;
            enlargedByPlunge = true;
            specialCooldown = 720;
            specialMaxCooldown = 720;
            game.addToKillFeed(shortName() + (ultimate ? " ULT PELICAN PLUNGE!!!" : " PELICAN PLUNGE!!!"));
            game.shakeIntensity = Math.max(game.shakeIntensity, ultimate ? 38 : 32);
            game.hitstopFrames = Math.max(game.hitstopFrames, ultimate ? 22 : 18);
            target.vx += (target.x > x ? 1 : -1) * (ultimate ? 44 : 36);
            target.vy = ultimate ? -32 : -26;
            int dmg = (int)((ultimate ? 32 : 24) * powerMultiplier);
            double old = target.health;
            int dealt = (int) applyDamageTo(target, dmg);
            game.damageDealt[playerIndex] += dealt;
            game.recordSpecialImpact(playerIndex, dealt, dealt > 0);
            if (target.health <= 0 && old > 0) game.eliminations[playerIndex]++;
            int particleCount = scaledParticleCount(ultimate ? 170 : 120);
            for (int i = 0; i < particleCount; i++) {
                double ang = Math.random() * Math.PI * 2;
                game.particles.add(new Particle(target.x + 40, target.y + 40,
                        Math.cos(ang) * (8 + Math.random() * 22),
                        Math.sin(ang) * (8 + Math.random() * 22) - 12,
                        ultimate ? Color.GOLD.brighter() : Color.ORANGE.brighter()));
            }
            game.recordPelicanPlungeAchievement();
        } else {
            specialCooldown = 210;
        }
    }

    KeyCode leftKey() {
        return game.leftKeyForPlayer(playerIndex);
    }

    KeyCode rightKey() {
        return game.rightKeyForPlayer(playerIndex);
    }

    KeyCode jumpKey() {
        return game.jumpKeyForPlayer(playerIndex);
    }

    private KeyCode attackKey() {
        return game.attackKeyForPlayer(playerIndex);
    }

    private KeyCode specialKey() {
        return game.specialKeyForPlayer(playerIndex);
    }

    private KeyCode grabKey() {
        return game.grabKeyForPlayer(playerIndex);
    }

    private KeyCode blockKey() {
        return game.blockKeyForPlayer(playerIndex);
    }

    private boolean leftPressed() {
        return game.isLeftPressed(playerIndex);
    }

    private boolean rightPressed() {
        return game.isRightPressed(playerIndex);
    }

    private boolean jumpPressed() {
        return game.isJumpPressed(playerIndex);
    }

    private boolean attackPressed() {
        return game.isAttackPressed(playerIndex);
    }

    private boolean specialPressed() {
        return game.isSpecialPressed(playerIndex);
    }

    private boolean specialJustPressed() {
        return specialPressed() && !specialHeldLastFrame;
    }

    private boolean grabPressed() {
        return game.isGrabPressed(playerIndex);
    }

    private boolean blockPressed() {
        return game.isBlockPressed(playerIndex);
    }

    private void rememberFrameInputs(boolean jumpHeld, boolean specialHeld, boolean blockHeld, boolean grabHeld,
                                     boolean leftHeld, boolean rightHeld) {
        jumpHeldLastFrame = jumpHeld;
        specialHeldLastFrame = specialHeld;
        blockHeldLastFrame = blockHeld;
        grabHeldLastFrame = grabHeld;
        leftHeldLastFrame = leftHeld;
        rightHeldLastFrame = rightHeld;
    }

    private boolean tauntCyclePressed() {
        return game.isTauntCyclePressed(playerIndex);
    }

    private boolean tauntExecutePressed() {
        return game.isTauntExecutePressed(playerIndex);
    }

    private String shortName() {
        return shortName(name);
    }

    private static String shortName(String fullName) {
        if (fullName == null) {
            return "";
        }
        int colon = fullName.indexOf(':');
        if (colon < 0) {
            return fullName;
        }
        return fullName.substring(0, colon).trim();
    }

    boolean isDownHeld() {
        return blockPressed();
    }

    private int facingDirection() {
        return facingRight ? 1 : -1;
    }

    private int horizontalInputDirection() {
        if (leftPressed() == rightPressed()) {
            return 0;
        }
        return leftPressed() ? -1 : 1;
    }

    private boolean isRaptor() {
        return type == BirdGame3.BirdType.EAGLE || type == BirdGame3.BirdType.FALCON;
    }

    private boolean pigeonSpecialActive() {
        return pigeonRushTimer > 0 || pigeonFlutterTimer > 0 || pigeonScavengeTimer > 0;
    }

    private boolean raptorSpecialActive() {
        return raptorCryTimer > 0 || raptorRushTimer > 0 || raptorClimbTimer > 0 || eagleDiveActive || eagleAscentActive;
    }

    private double pigeonRushSpeed() {
        if (pigeonRushGrounded) {
            return pigeonRushUltimate ? 22.0 : 20.0;
        }
        return pigeonRushUltimate ? 19.4 : 17.8;
    }

    private int pigeonRushDamage() {
        return pigeonRushUltimate ? 6 : 3;
    }

    private double pigeonRushHorizontalLaunch() {
        return pigeonRushUltimate ? 9.0 : 7.0;
    }

    private double pigeonRushVerticalLaunch() {
        return pigeonRushUltimate ? 11.8 : 9.2;
    }

    private double raptorRushSpeed() {
        boolean eagle = type == BirdGame3.BirdType.EAGLE;
        if (eagle) {
            if (raptorRushGrounded) {
                return raptorRushUltimate ? 15.1 : 13.8;
            }
            return raptorRushUltimate ? 13.8 : 12.4;
        }
        if (raptorRushGrounded) {
            return raptorRushUltimate ? 18.4 : 16.9;
        }
        return raptorRushUltimate ? 16.4 : 15.0;
    }

    private int raptorCryReuseFrames(boolean ultimate) {
        boolean eagle = type == BirdGame3.BirdType.EAGLE;
        return eagle ? (ultimate ? 60 : 52) : (ultimate ? 44 : 36);
    }

    private int raptorRushReuseFrames(boolean ultimate) {
        boolean eagle = type == BirdGame3.BirdType.EAGLE;
        return eagle ? (ultimate ? 58 : 48) : (ultimate ? 42 : 34);
    }

    private boolean raptorSpecialReady(RaptorSpecialVariant variant) {
        return switch (variant) {
            case NEUTRAL -> raptorCryReuseTimer <= 0;
            case SIDE -> raptorRushReuseTimer <= 0;
            case UP -> !raptorUpSpecialUsed;
            case DOWN -> specialCooldown <= 0;
        };
    }

    private boolean raptorSpecialOnReuseLockout(RaptorSpecialVariant variant) {
        return switch (variant) {
            case NEUTRAL -> raptorCryReuseTimer > 0;
            case SIDE -> raptorRushReuseTimer > 0;
            case UP -> raptorUpSpecialUsed;
            case DOWN -> specialCooldown > 0;
        };
    }

    private boolean canConvertShieldIntoPigeonDownSpecial(PigeonSpecialVariant variant) {
        return variant == PigeonSpecialVariant.DOWN
                && isBlocking
                && shieldStunFrames <= 0;
    }

    private boolean canConvertShieldIntoRaptorDownSpecial(RaptorSpecialVariant variant) {
        return variant == RaptorSpecialVariant.DOWN
                && isBlocking
                && shieldStunFrames <= 0;
    }

    private boolean canStartPigeonSpecial() {
        PigeonSpecialVariant variant = selectPigeonSpecialVariant();
        boolean neutralReady = variant != PigeonSpecialVariant.NEUTRAL || specialCooldown <= 0;
        boolean shieldConversion = canConvertShieldIntoPigeonDownSpecial(variant);
        return type == BirdGame3.BirdType.PIGEON
                && health > 0
                && stunTime <= 0.0
                && grabbedBy == null
                && grabbedTarget == null
                && (!isBlocking || shieldConversion)
                && !isDodging()
                && !pigeonSpecialActive()
                && (variant != PigeonSpecialVariant.UP || !pigeonUpSpecialUsed)
                && neutralReady;
    }

    private boolean canStartRaptorSpecial() {
        return canStartRaptorSpecialVariant(selectRaptorSpecialVariant());
    }

    private boolean canStartRaptorSpecialVariant(RaptorSpecialVariant variant) {
        boolean shieldConversion = canConvertShieldIntoRaptorDownSpecial(variant);
        return isRaptor()
                && health > 0
                && stunTime <= 0.0
                && grabbedBy == null
                && grabbedTarget == null
                && (!isBlocking || shieldConversion)
                && !isDodging()
                && !raptorSpecialActive()
                && raptorSpecialReady(variant);
    }

    private PigeonSpecialVariant selectPigeonSpecialVariant() {
        if (jumpPressed()) {
            return PigeonSpecialVariant.UP;
        }
        if (blockPressed()) {
            return PigeonSpecialVariant.DOWN;
        }
        if (leftPressed() != rightPressed()) {
            return PigeonSpecialVariant.SIDE;
        }
        return PigeonSpecialVariant.NEUTRAL;
    }

    private RaptorSpecialVariant selectRaptorSpecialVariant() {
        if (jumpPressed()) {
            return RaptorSpecialVariant.UP;
        }
        if (blockPressed()) {
            return RaptorSpecialVariant.DOWN;
        }
        if (leftPressed() != rightPressed()) {
            return RaptorSpecialVariant.SIDE;
        }
        return RaptorSpecialVariant.NEUTRAL;
    }

    private boolean shouldReserveJumpForSpecial() {
        if (!specialJustPressed()) {
            return false;
        }
        if (type == BirdGame3.BirdType.PIGEON) {
            return canStartPigeonSpecial()
                    && selectPigeonSpecialVariant() == PigeonSpecialVariant.UP
                    && !pigeonUpSpecialUsed;
        }
        if (isRaptor()) {
            return canStartRaptorSpecial()
                    && selectRaptorSpecialVariant() == RaptorSpecialVariant.UP
                    && !raptorUpSpecialUsed;
        }
        return false;
    }

    private boolean shouldReserveBlockForSpecial() {
        if (!specialJustPressed()) {
            return false;
        }
        if (type == BirdGame3.BirdType.PIGEON) {
            return canStartPigeonSpecial()
                    && selectPigeonSpecialVariant() == PigeonSpecialVariant.DOWN;
        }
        if (isRaptor()) {
            return canStartRaptorSpecial()
                    && selectRaptorSpecialVariant() == RaptorSpecialVariant.DOWN;
        }
        return false;
    }

    private void resetPigeonSpecialState() {
        pigeonFeatherBurstTimer = 0;
        pigeonFeatherBurstUltimate = false;
        pigeonRushTimer = 0;
        pigeonRushGrounded = false;
        pigeonRushUltimate = false;
        Arrays.fill(pigeonRushHit, false);
        pigeonFlutterTimer = 0;
        pigeonFlutterUltimate = false;
        Arrays.fill(pigeonFlutterHit, false);
        pigeonScavengeTimer = 0;
        pigeonScavengeAirborne = false;
        pigeonScavengeUltimate = false;
        pigeonScavengeResolved = false;
    }

    private void resetRaptorSpecialState() {
        raptorCryTimer = 0;
        raptorCryUltimate = false;
        raptorRushTimer = 0;
        raptorRushUltimate = false;
        raptorRushGrounded = false;
        raptorRushDirection = 1;
        Arrays.fill(raptorRushHit, false);
        raptorClimbTimer = 0;
        raptorClimbUltimate = false;
        raptorClimbDirection = 1;
        Arrays.fill(raptorClimbHit, false);
        eagleDiveActive = false;
        eagleAscentActive = false;
        eagleAscentFrames = 0;
        Arrays.fill(eagleAscentHit, false);
        eagleDiveCountdown = 0;
        diveTimer = 0;
    }

    private void interruptPigeonSpecialStateOnHit() {
        if (type != BirdGame3.BirdType.PIGEON) {
            return;
        }
        if (pigeonFeatherBurstTimer > 0 || pigeonRushTimer > 0 || pigeonFlutterTimer > 0 || pigeonScavengeTimer > 0) {
            attackAnimationTimer = 0;
        }
        resetPigeonSpecialState();
    }

    private void interruptRaptorSpecialStateOnHit() {
        if (!isRaptor()) {
            return;
        }
        if (raptorSpecialActive()) {
            attackAnimationTimer = 0;
        }
        resetRaptorSpecialState();
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
    private int aiDropCommitFrames = 0;
    private int aiDropCommitDir = 0;
    private int aiVoidRecoveryLockFrames = 0;
    private int aiTargetLockFrames = 0;
    private int aiLockedTargetIndex = -1;
    private double aiDropOriginY = Double.NaN;
    private double aiLastHealth = STARTING_HEALTH;

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
        if (aiDropCommitFrames > 0) aiDropCommitFrames--;
        if (aiVoidRecoveryLockFrames > 0) aiVoidRecoveryLockFrames--;
        if (aiTargetLockFrames > 0) aiTargetLockFrames--;

        clearAIInputs();

        int cpuLevel = game.getCpuLevel(playerIndex);
        double rawSkill = Math.clamp((cpuLevel - 1) / 8.0, 0.0, 1.0);
        double skill = Math.pow(rawSkill, 2.1);
        double error = Math.min(1.0, 1.05 - skill);
        double currentDurability = aiDurabilityHealth();
        if (cpuLevel <= 1) {
            skill = 0.0;
            error = 1.0;
            if (random.nextDouble() < 0.22) {
                aiLastHealth = currentDurability;
                return;
            }
        }

        Bird target = pickAITarget();
        PowerUp powerUp = pickBestAIPowerUp(target);
        boolean onGround = isOnGround();
        Platform standing = findCurrentSupportPlatform();

        if (target == null && powerUp == null) {
            resetAIDropCommit();
            applyAIVoidRecoveryInputs(onGround, standing);
            aiLastHealth = currentDurability;
            return;
        }

        double myCx = x + 40;
        double targetDist = target != null ? Math.hypot(target.x - x, target.y - y) : Double.MAX_VALUE;
        double idealRange = getAIIdealRange();
        boolean lowHealth = currentDurability < 38;
        boolean tookDamageRecently = currentDurability < aiLastHealth - 1.0;
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
        if (onGround && y > BirdGame3.GROUND_Y + 220) {
            game.setAiControlKey(playerIndex, jumpKey(), true);
            aiJumpCooldown = 12;
        }

        if (handleAIDodgeBurstThreats(target, onGround)) {
            aiLastHealth = currentDurability;
            return;
        }

        PowerUp healthPack = findBestHealthPowerUp();
        boolean veryLowHealth = currentDurability < 20;
        boolean losingHard = target != null && target.aiDurabilityHealth() > currentDurability + 32;
        boolean retreatWindow = aiRetreatCooldown <= 0 && (veryLowHealth || (currentDurability < 28 && losingHard));
        boolean shouldRetreat = target != null && retreatWindow && targetDist < 220 && healthPack == null && aiCommitFrames <= 0;
        boolean immediatePowerChance = isImmediatePowerUpOpportunity(powerUp);
        boolean shouldChasePower = powerUp != null &&
                ((shouldPrioritizePowerUp(powerUp, target) && aiCommitFrames <= 0) || immediatePowerChance);
        if (shouldChasePower) {
            aiPowerCommitFrames = Math.max(aiPowerCommitFrames, immediatePowerChance ? 44 : 30);
        }
        boolean powerFocus = powerUp != null && !shouldRetreat && (shouldChasePower || aiPowerCommitFrames > 0);
        if (powerFocus && !isPowerUpConvenient(powerUp, target)) {
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
            double candidateDropEdgeX = targetCx < platformCenter ? leftEdge : rightEdge;
            if (isAIDropGoalSafe(candidateDropEdgeX, target.y + 40)) {
                dropEdgeX = candidateDropEdgeX;
                dropPlan = true;
                aiDropCommitDir = targetCx < platformCenter ? -1 : 1;
                aiDropCommitFrames = Math.max(aiDropCommitFrames, 24);
                aiDropOriginY = standing.y;
            }
        }
        boolean lowCpu = cpuLevel <= 2;
        double goalX;
        if (shouldRetreat) {
            goalX = x + (x - target.x) * 1.35;
            aiRetreatCooldown = 90 + random.nextInt(45);
        } else if (powerFocus) {
            goalX = pickPowerUpGoalX(powerUp);
        } else if (target != null) {
            // Predict movement instead of chasing current position.
            double lead = Math.clamp(targetDist / 120.0, 2.0, 10.0);
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
                climbPlatform = findClimbPlatform(target.x + 40, maxRise);
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

        goalX = Math.clamp(goalX, 120.0, BirdGame3.WORLD_WIDTH - 120.0);
        goalX = clampGoalXAwayFromVoid(goalX);
        boolean offstageCommit = aiGoalLeavesMainStage(goalX);

        if (powerFocus) facingRight = powerUp.x > myCx;
        else if (target != null) facingRight = target.x > myCx;
        else facingRight = powerUp.x > myCx;

        int moveDir = 0;
        if (dropPlan) {
            goalX = clampGoalXAwayFromVoid(dropEdgeX);
            offstageCommit = aiGoalLeavesMainStage(goalX);
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

        if (aiDropCommitFrames > 0) {
            boolean abandonDrop = powerFocus || target == null || !targetBelow;
            boolean clearedDrop = !Double.isNaN(aiDropOriginY) && y > aiDropOriginY + 4;
            boolean landedLower = onGround && standing != null
                    && !Double.isNaN(aiDropOriginY)
                    && standing.y > aiDropOriginY + 12;
            if (abandonDrop || clearedDrop || landedLower) {
                resetAIDropCommit();
            } else if (aiDropCommitDir != 0) {
                moveDir = aiDropCommitDir;
                aiDirectionLock = 0;
                aiStrafeHoldFrames = 0;
                aiStrafeTimer = 0;
                aiMicroPause = 0;
            }
        }

        if (isVoidMap() && moveDir != 0) {
            double projectedX = x + moveDir * Math.max(18.0, type.speed * speedMultiplier * 4.0);
            if (Math.abs(clampGoalXAwayFromVoid(projectedX) - projectedX) > 0.1) {
                moveDir = 0;
            }
        }

        boolean voidRecovery = applyAIVoidRecoveryInputs(onGround, standing);
        if (!voidRecovery) {
            if (moveDir < 0) game.setAiControlKey(playerIndex, leftKey(), true);
            if (moveDir > 0) game.setAiControlKey(playerIndex, rightKey(), true);
        } else {
            aiLastHealth = currentDurability;
            return;
        }

        // Vertical positioning and recovery behavior.
        if (!powerFocus && target != null) {
            double dy = target.y - y;
            if (onGround && aiJumpCooldown <= 0) {
                double climbCenter = climbPlatform != null ? climbPlatform.x + climbPlatform.w / 2.0 : myCx;
                boolean alignedForClimb = !verticalPlan || climbPlatform == null || Math.abs((x + 40) - climbCenter) < 165;
                boolean jumpForHeight = dy < -120 && Math.abs(target.x - x) < 420 && alignedForClimb;
                boolean jumpForCombo = dy > 70 && targetDist < 220;
                boolean jumpForAboveClose = dy < -200 && Math.abs(target.x - x) < 220 && alignedForClimb;
                boolean jumpForOffstageLaunch = shouldAIJumpBeforeOffstage(goalX);
                double jumpSense = 0.35 + 0.65 * skill;
                if (jumpForOffstageLaunch) {
                    game.setAiControlKey(playerIndex, jumpKey(), true);
                    aiJumpCooldown = 14;
                } else if ((jumpForHeight || jumpForCombo || jumpForAboveClose) && random.nextDouble() < jumpSense) {
                    game.setAiControlKey(playerIndex, jumpKey(), true);
                    aiJumpCooldown = 14;
                }
                if (verticalPlan && climbPlatform != null) {
                    if (Math.abs((x + 40) - climbCenter) < 165 && dy < -140) {
                        game.setAiControlKey(playerIndex, jumpKey(), true);
                        aiJumpCooldown = 14;
                    }
                }
            }

            if (!onGround && currentFlyUpForce() > 0) {
                Platform mainStage = findAIMainStagePlatform();
                boolean recoverAltitude = y > BirdGame3.GROUND_Y - 120;
                boolean maintainVsTarget = target.y < y + 180 && !isAIAboveCruiseCeiling(target, mainStage);
                boolean recoverVoid = isVoidMap() && (offstageCommit || isAIVoidRecoveryUrgent(false, standing));
                if (recoverAltitude || maintainVsTarget || recoverVoid) {
                    game.setAiControlKey(playerIndex, jumpKey(), true);
                }
            }
        } else if (onGround && aiJumpCooldown <= 0 && powerUp.y < y - 120) {
            double dx = Math.abs(powerUp.x - (x + 40));
            double dy = (y + 40) - powerUp.y;
            if (dx < 140 && dy < 320) {
                game.setAiControlKey(playerIndex, jumpKey(), true);
                aiJumpCooldown = 14;
            }
        }

        if (shouldAIUseUtilitySpecial(target, powerUp, onGround, climbPlatform, powerFocus)) {
            game.setAiControlKey(playerIndex, specialKey(), true);
            aiSpecialCooldown = 18;
            aiLastHealth = currentDurability;
            return;
        }

        // Defensive block read (ground only).
        if (onGround && target != null && targetDist < 170 && target.attackAnimationTimer > 3 &&
                facingRight == (target.x > x) && random.nextDouble() < (lowHealth ? 0.50 : 0.34) * (0.25 + 0.75 * skill)) {
            game.setAiControlKey(playerIndex, blockKey(), true);
        }

        // Attack cadence respects role/range.
        double attackChance = (aiCommitFrames > 0 ? 0.96 : 0.84) * (0.45 + 0.55 * skill);
        if (cpuLevel <= 1) attackChance *= 0.04;
        else if (cpuLevel == 2) attackChance *= 0.35;
        if (!powerFocus && target != null && attackCooldown <= 0 &&
                targetDist < Math.max(140, idealRange * 0.95) &&
                Math.abs(target.y - y) < 115 &&
                random.nextDouble() < attackChance) {
            game.setAiControlKey(playerIndex, attackKey(), true);
        }

        // Special ability timing by bird role.
        if (!powerFocus && target != null
                && (isRaptor() ? canStartRaptorSpecial() : specialCooldown <= 0)
                && aiSpecialCooldown <= 0 &&
                shouldUseSpecialAI(target, targetDist, onGround, lowHealth) &&
                random.nextDouble() < (0.25 + 0.75 * skill)) {
            if (type == BirdGame3.BirdType.PIGEON && onGround && lowHealth && targetDist > 110) {
                game.setAiControlKey(playerIndex, blockKey(), true);
            }
            game.setAiControlKey(playerIndex, specialKey(), true);
            aiSpecialCooldown = type == BirdGame3.BirdType.PIGEON ? 16 : 26;
        }

        if (!powerFocus && tauntCooldown <= 0 && target != null && currentDurability > 80
                && target.aiDurabilityHealth() < 35 &&
                targetDist < 200 && random.nextDouble() < 0.006) {
            currentTaunt = random.nextInt(3) + 1;
            tauntTimer = 50;
            tauntCooldown = 300;
            game.addToKillFeed(shortName() + " IS ABSOLUTELY COOKING!");
        }

        aiLastHealth = currentDurability;
    }

    private void specialBat(boolean ultimate) {
        batEchoTimer = ultimate ? 220 : 150;
        specialCooldown = 660;
        specialMaxCooldown = 660;
        game.shakeIntensity = Math.max(game.shakeIntensity, ultimate ? 28 : 22);
        game.hitstopFrames = Math.max(game.hitstopFrames, ultimate ? 15 : 12);

        if (batHanging) {
            releaseBatHang();
            vy = -16;
            vx += (facingRight ? 1 : -1) * 9;
        }

        game.addToKillFeed(shortName() + (ultimate ? " ULT SONAR SCREECH!" : " UNLEASHED SONAR SCREECH!"));

        for (Bird other : game.players) {
            if (!canDamageTarget(other)) continue;
            double dx = other.bodyCenterX() - bodyCenterX();
            double dy = other.bodyCenterY() - bodyCenterY();
            double centerDist = Math.hypot(dx, dy);
            double dist = Math.max(0.0, centerDist - other.combatRadius());
            if (dist > (ultimate ? 460 : 360)) continue;

            int dmg = dist < 150 ? (ultimate ? 26 : 18) : (dist < 260 ? (ultimate ? 18 : 12) : (ultimate ? 12 : 8));
            double oldHealth = other.health;
            int dealt = (int) applyDamageTo(other, dmg);
            game.damageDealt[playerIndex] += dealt;
            game.recordSpecialImpact(playerIndex, dealt, dealt > 0);
            if (other.health <= 0 && oldHealth > 0) game.eliminations[playerIndex]++;

            other.applyStun(ultimate ? 45 : 28);
            double safeDist = Math.max(0.001, centerDist);
            other.vx += dx / safeDist * (ultimate ? 20 : 16);
            other.vy -= ultimate ? 10 : 8;
        }

        int ringCount = ultimate ? 6 : 4;
        for (int ring = 1; ring <= ringCount; ring++) {
            double radius = 70 + ring * 55;
            for (int i = 0; i < 42; i++) {
                double ang = i / 42.0 * Math.PI * 2;
                game.particles.add(new Particle(
                        x + 40 + Math.cos(ang) * radius,
                        y + 40 + Math.sin(ang) * radius,
                        Math.cos(ang) * 2.4,
                        Math.sin(ang) * 2.4,
                        ultimate ? Color.GOLD.brighter() : (ring % 2 == 0 ? Color.MEDIUMPURPLE.brighter() : Color.CYAN.brighter())
                ));
            }
        }
    }

    private void specialRaven(boolean ultimate) {
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

        if (target != null && bestDist < (ultimate ? 720 : 520)) {
            double dir = (target.x + 40 >= x + 40) ? -1 : 1;
            double warpX = target.x + dir * (ultimate ? 150 : 120);
            double warpY = target.y;
            double maxX = BirdGame3.WORLD_WIDTH - 80 * sizeMultiplier;
            double maxY = BirdGame3.GROUND_Y - 80 * sizeMultiplier;
            warpX = Math.clamp(warpX, 0.0, maxX);
            warpY = Math.clamp(warpY, 0.0, maxY);
            x = warpX;
            y = warpY;
            facingRight = dir < 0;

            int dmg = Math.max(8, (int) Math.round((ultimate ? 20 : 14) * powerMultiplier));
            double oldHealth = target.health;
            int dealt = (int) applyDamageTo(target, dmg);
            game.damageDealt[playerIndex] += dealt;
            game.recordSpecialImpact(playerIndex, dealt, dealt > 0);
            if (target.health <= 0 && oldHealth > 0) game.eliminations[playerIndex]++;

            target.applyStun(ultimate ? 70 : 40);
            double kbDir = (target.x > x) ? 1 : -1;
            target.vx += kbDir * (ultimate ? 14 : 10);
            target.vy -= ultimate ? 9 : 6;

            speedMultiplier = Math.max(speedMultiplier, baseSpeedMultiplier * (ultimate ? 1.55 : 1.35));
            speedTimer = Math.max(speedTimer, ultimate ? 180 : 120);

            game.shakeIntensity = Math.max(game.shakeIntensity, ultimate ? 22 : 16);
            game.hitstopFrames = Math.max(game.hitstopFrames, ultimate ? 11 : 8);
            game.addToKillFeed(shortName() + (ultimate ? " ULT SHADOW WARPED " : " SHADOW WARPED ") + target.shortName() + "! -" + dealt + " HP");

            int particleCount = scaledParticleCount(ultimate ? 140 : 90);
            for (int i = 0; i < particleCount; i++) {
                double angle = Math.random() * Math.PI * 2;
                double speed = 5 + Math.random() * 12;
                game.particles.add(new Particle(
                        x + 40 + Math.cos(angle) * 20,
                        y + 40 + Math.sin(angle) * 20,
                        Math.cos(angle) * speed,
                        Math.sin(angle) * speed - 4,
                        (ultimate ? Color.GOLD : Color.web("#263238")).deriveColor(0, 1, 1, 0.85)
                ));
            }

            specialCooldown = 660;
            specialMaxCooldown = 660;
        } else {
            vx += (facingRight ? 1 : -1) * (ultimate ? 24 : 18);
            vy = Math.min(vy, ultimate ? -12 : -10);
            canDoubleJump = true;
            speedMultiplier = Math.max(speedMultiplier, baseSpeedMultiplier * (ultimate ? 1.4 : 1.2));
            speedTimer = Math.max(speedTimer, ultimate ? 120 : 80);
            game.addToKillFeed(shortName() + (ultimate ? " ULT SHADOW DASH!" : " SHADOW DASH!"));
            int particleCount = scaledParticleCount(ultimate ? 80 : 50);
            for (int i = 0; i < particleCount; i++) {
                double angle = Math.random() * Math.PI * 2;
                game.particles.add(new Particle(
                        x + 40,
                        y + 40,
                        Math.cos(angle) * (4 + Math.random() * 8),
                        Math.sin(angle) * (4 + Math.random() * 8) - 3,
                        (ultimate ? Color.GOLD : Color.web("#455A64")).deriveColor(0, 1, 1, 0.8)
                ));
            }
            specialCooldown = 240;
            specialMaxCooldown = 240;
        }
    }

    private void clearAIInputs() {
        game.setAiControlKey(playerIndex, leftKey(), false);
        game.setAiControlKey(playerIndex, rightKey(), false);
        game.setAiControlKey(playerIndex, jumpKey(), false);
        game.setAiControlKey(playerIndex, attackKey(), false);
        game.setAiControlKey(playerIndex, specialKey(), false);
        game.setAiControlKey(playerIndex, grabKey(), false);
        game.setAiControlKey(playerIndex, blockKey(), false);
    }

    private void resetAIDropCommit() {
        aiDropCommitFrames = 0;
        aiDropCommitDir = 0;
        aiDropOriginY = Double.NaN;
    }

    private Bird currentAILockedTarget() {
        if (aiLockedTargetIndex < 0 || aiLockedTargetIndex >= game.players.length) return null;
        Bird target = game.players[aiLockedTargetIndex];
        if (target == null || target == this || target.health <= 0) return null;
        if (!game.canDamage(this, target)) return null;
        return target;
    }

    private double aiTargetVoidPenalty(Bird candidate) {
        if (!isVoidMap()) return 0.0;
        Platform mainStage = findAIMainStagePlatform();
        if (mainStage == null) return 0.0;
        double stageLeft = mainStage.x;
        double stageRight = mainStage.x + mainStage.w;
        double centerX = candidate.bodyCenterX();
        double offstageDistance = centerX < stageLeft ? stageLeft - centerX
                : (centerX > stageRight ? centerX - stageRight : 0.0);
        if (offstageDistance <= 0.0) return 0.0;
        double allowance = Math.max(1.0, aiVoidHorizontalAllowance(mainStage));
        double depth = Math.max(0.0, candidate.bodyBottomY() - mainStage.y);
        double penalty = Math.max(0.0, offstageDistance - allowance * 0.22) * 0.22;
        penalty += Math.max(0.0, depth - aiVoidDepthAllowance(mainStage) * 0.45) * 0.07;
        if (aiCanUseAirRecovery()) {
            penalty += offstageDistance * 0.28;
        }
        return penalty;
    }

    private double scoreAITarget(Bird candidate, Bird lockedTarget) {
        double dist = Math.hypot(candidate.x - x, candidate.y - y);
        double score = 3000.0 / (1.0 + dist);
        score += (100.0 - candidate.health) * 1.8;
        if (candidate.specialCooldown <= 0) score += 40.0;
        if (candidate.playerIndex == 0) score += 15.0;
        if (candidate.attackAnimationTimer > 3 && dist < 260.0) score += 12.0;
        score += Math.max(0.0, 55.0 - candidate.health) * 0.42;
        score -= Math.abs(candidate.y - y) * (currentFlyUpForce() > 0.0 ? 0.025 : 0.055);
        score -= aiTargetVoidPenalty(candidate);
        if (candidate == lockedTarget) {
            score += 18.0 + Math.min(22.0, aiTargetLockFrames * 0.45);
        }
        return score;
    }

    private Bird pickAITarget() {
        Bird lockedTarget = currentAILockedTarget();
        Bird best = null;
        double bestScore = -Double.MAX_VALUE;
        double lockedScore = -Double.MAX_VALUE;
        for (Bird b : game.players) {
            if (b == null || b == this || b.health <= 0) continue;
            if (!game.canDamage(this, b)) continue;
            double score = scoreAITarget(b, lockedTarget);
            if (b == lockedTarget) {
                lockedScore = score;
            }
            if (score > bestScore) {
                bestScore = score;
                best = b;
            }
        }
        if (lockedTarget != null && lockedScore > -Double.MAX_VALUE / 2.0) {
            double keepMargin = aiTargetLockFrames > 0 ? 18.0 : 8.0;
            if (lockedScore >= bestScore - keepMargin) {
                best = lockedTarget;
            }
        }
        if (best != null) {
            if (lockedTarget == null || best.playerIndex != lockedTarget.playerIndex) {
                aiLockedTargetIndex = best.playerIndex;
                aiTargetLockFrames = 42 + random.nextInt(28);
            } else {
                aiLockedTargetIndex = best.playerIndex;
                aiTargetLockFrames = Math.max(aiTargetLockFrames, 12);
            }
        } else {
            aiLockedTargetIndex = -1;
            aiTargetLockFrames = 0;
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
            double score = getScore(target, p, myDist);

            if (score > bestScore) {
                bestScore = score;
                bestPowerUp = p;
            }
        }
        return bestPowerUp;
    }

    private double getScore(Bird target, PowerUp p, double myDist) {
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
            case BROADSIDE -> score = 90;
        }
        score /= (1 + myDist / 320.0);

        if (target != null) {
            double enemyDist = Math.hypot(p.x - (target.x + 40), p.y - (target.y + 40));
            if (enemyDist < myDist * 0.75) score *= 0.6;
        }
        return score;
    }

    private boolean isImmediatePowerUpOpportunity(PowerUp p) {
        if (p == null) return false;
        double dx = Math.abs(p.x - (x + 40));
        double dy = p.y - (y + 40);
        if (dx < 110 && Math.abs(dy) < 220) return true;
        return dx < 105 && dy > 90 && dy < 360;
    }

    private double rawPowerUpGoalX(PowerUp p) {
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
                leftDrop = Math.clamp(leftDrop, 120.0, BirdGame3.WORLD_WIDTH - 120.0);
                rightDrop = Math.clamp(rightDrop, 120.0, BirdGame3.WORLD_WIDTH - 120.0);
                return Math.abs(leftDrop - p.x) <= Math.abs(rightDrop - p.x) ? leftDrop : rightDrop;
            }
        }
        return p.x;
    }

    private double pickPowerUpGoalX(PowerUp p) {
        return clampGoalXAwayFromVoid(rawPowerUpGoalX(p));
    }

    private Platform findCurrentSupportPlatform() {
        double feetX = x + 40 * sizeMultiplier;
        double feetY = y + 80 * sizeMultiplier;
        for (Platform p : game.platforms) {
            boolean isCaveCeiling = game.selectedMap == MapType.CAVE &&
                    p.y <= 1 && p.h >= 60 && p.w >= BirdGame3.WORLD_WIDTH - 10;
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
        double maxVertical = (currentFlyUpForce() > 0 || !isOnGround()) ? 520 : 320;
        if (Math.abs(dy) > maxVertical) return false;
        if (isVoidMap()) {
            double desiredGoalX = rawPowerUpGoalX(p);
            double safeGoalX = clampGoalXAwayFromVoid(desiredGoalX);
            double edgeDrift = Math.abs(safeGoalX - desiredGoalX);
            Platform mainStage = findAIMainStagePlatform();
            if (mainStage != null) {
                double maxEdgeDrift = Math.max(24.0, aiVoidHorizontalAllowance(mainStage) * 0.55);
                if (edgeDrift > maxEdgeDrift) return false;
                double maxSafePickupY = mainStage.y + aiVoidDepthAllowance(mainStage) * 0.9;
                if (p.y > maxSafePickupY) return false;
            }
        }

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
        boolean isFloor = p.w >= BirdGame3.WORLD_WIDTH - 10 && p.h >= 200;
        boolean isWall = p.h >= BirdGame3.WORLD_HEIGHT - 10 && p.w <= 150;
        boolean isCaveCeiling = game.selectedMap == MapType.CAVE &&
                p.y <= 1 && p.h >= 60 && p.w >= BirdGame3.WORLD_WIDTH - 10;
        return isFloor || isWall || isCaveCeiling;
    }

    private boolean isVoidMap() {
        return game.selectedMap == MapType.BATTLEFIELD || game.selectedMap == MapType.BEACON_CROWN;
    }

    private Platform findAIMainStagePlatform() {
        Platform best = null;
        double bestWidth = -1.0;
        for (Platform p : game.platforms) {
            if (isBoundaryPlatform(p)) continue;
            if (p.w > bestWidth) {
                bestWidth = p.w;
                best = p;
            }
        }
        return best;
    }

    private boolean aiCanUseAirRecovery() {
        return !(currentFlyUpForce() > 0.0) && (type != BirdGame3.BirdType.PIGEON || !canDoubleJump);
    }

    private double topCameraOverflow() {
        return Math.max(0.0, -y);
    }

    private double aiCruiseCeilingY(Platform mainStage) {
        double ceiling = mainStage != null ? mainStage.y - 820.0 : BirdGame3.GROUND_Y - 1000.0;
        ceiling = Math.max(140.0, ceiling);
        return switch (type) {
            case HUMMINGBIRD -> ceiling - 140.0;
            case EAGLE, FALCON, PHOENIX, BAT -> ceiling - 60.0;
            case ROADRUNNER -> roadrunnerSandstormActive() ? ceiling - 40.0 : ceiling;
            default -> ceiling;
        };
    }

    private boolean isAIAboveCruiseCeiling(Bird target, Platform mainStage) {
        if (target == null || currentFlyUpForce() <= 0.0) return false;
        if (y >= aiCruiseCeilingY(mainStage)) return false;
        return target.y > y - 180.0;
    }

    private double aiVoidHorizontalAllowance(Platform mainStage) {
        if (mainStage == null) return 0.0;
        boolean grounded = isOnGround();
        double altitude = Math.max(0.0, mainStage.y - (y + 40 * sizeMultiplier));
        double allowance = 8.0;
        switch (type) {
            case HUMMINGBIRD -> allowance = 360.0 + altitude * 0.18 + Math.max(0.0, -vy) * 10.0;
            case EAGLE, FALCON, PHOENIX, BAT -> allowance = 240.0 + altitude * 0.14 + Math.max(0.0, -vy) * 7.0;
            case TITMOUSE, OPIUMBIRD, HEISENBIRD, RAVEN -> allowance = 210.0 + altitude * 0.12 + Math.max(0.0, -vy) * 6.0;
            case VULTURE -> {
                double launch = grounded ? 0.0 : Math.max(0.0, -vy) * 18.0;
                double altitudeBonus = Math.max(0.0, altitude - 80.0) * 0.16;
                allowance = 40.0 + launch + altitudeBonus + (isFlying ? 55.0 : 0.0);
            }
            case PIGEON -> {
                double fuelRatio = Math.clamp(limitedFlightFuel / LIMITED_FLIGHT_MAX, 0.0, 1.0);
                allowance = 70.0 + fuelRatio * 70.0 + ((grounded || canDoubleJump) ? 35.0 : 0.0) + altitude * 0.06;
            }
            case TURKEY, PELICAN, GRINCHHAWK, ROOSTER -> {
                double fuelRatio = Math.clamp(limitedFlightFuel / LIMITED_FLIGHT_MAX, 0.0, 1.0);
                allowance = 55.0 + fuelRatio * 55.0 + altitude * 0.05;
            }
            case PENGUIN -> allowance = specialCooldown <= 0 ? 105.0 + altitude * 0.08 : 10.0;
            case ROADRUNNER -> allowance = roadrunnerSandstormActive()
                    ? 170.0 + altitude * 0.12 + Math.max(0.0, -vy) * 6.0
                    : 50.0 + altitude * 0.04 + Math.max(0.0, -vy) * 2.4;
            case SHOEBILL, MOCKINGBIRD, RAZORBILL -> allowance = 95.0 + altitude * 0.07 + Math.max(0.0, -vy) * 4.0;
        }
        return Math.clamp(allowance, 0.0, Math.max(120.0, mainStage.w * 0.42));
    }

    private double aiVoidDepthAllowance(Platform mainStage) {
        if (mainStage == null) return 80.0;
        boolean grounded = isOnGround();
        double altitude = Math.max(0.0, mainStage.y - (y + 40 * sizeMultiplier));
        double allowance = 60.0 + aiVoidHorizontalAllowance(mainStage) * 0.75;
        switch (type) {
            case HUMMINGBIRD -> allowance += 120.0;
            case VULTURE -> allowance = 85.0
                    + (grounded ? 0.0 : Math.max(0.0, -vy) * 12.0)
                    + Math.max(0.0, altitude - 80.0) * 0.10
                    + (isFlying ? 55.0 : 0.0);
            case PIGEON -> {
                double fuelRatio = Math.clamp(limitedFlightFuel / LIMITED_FLIGHT_MAX, 0.0, 1.0);
                allowance = 140.0 + fuelRatio * 90.0 + ((grounded || canDoubleJump) ? 35.0 : 0.0);
            }
            case TURKEY, PELICAN, GRINCHHAWK, ROOSTER -> {
                double fuelRatio = Math.clamp(limitedFlightFuel / LIMITED_FLIGHT_MAX, 0.0, 1.0);
                allowance = 110.0 + fuelRatio * 70.0;
            }
            case PENGUIN -> allowance = specialCooldown <= 0 ? 180.0 + altitude * 0.08 : 70.0;
            case ROADRUNNER -> allowance = roadrunnerSandstormActive()
                    ? 190.0 + Math.max(0.0, altitude - 30.0) * 0.10
                    : 88.0 + Math.max(0.0, altitude - 30.0) * 0.04;
            default -> {
            }
        }
        return Math.clamp(allowance, 60.0, 520.0);
    }

    private double aiVoidReentryInset(Platform mainStage) {
        if (mainStage == null) return 40.0;
        return Math.clamp(aiVoidHorizontalAllowance(mainStage) * 0.22, 18.0, 85.0);
    }

    private double clampGoalXAwayFromVoid(double desiredX) {
        if (!isVoidMap()) return desiredX;
        Platform mainStage = findAIMainStagePlatform();
        if (mainStage == null) return desiredX;
        double allowance = aiVoidHorizontalAllowance(mainStage);
        double safeLeft = mainStage.x - 40 * sizeMultiplier - allowance;
        double safeRight = mainStage.x + mainStage.w - 40 * sizeMultiplier + allowance;
        double worldLeft = game.battlefieldLeftBound() + 20.0;
        double worldRight = game.battlefieldRightBound() - 80 * sizeMultiplier - 20.0;
        safeLeft = Math.max(worldLeft, safeLeft);
        safeRight = Math.min(worldRight, safeRight);
        return Math.clamp(desiredX, safeLeft, safeRight);
    }

    private boolean isAIDropGoalSafe(double desiredX, double targetY) {
        if (!isVoidMap()) return true;
        Platform mainStage = findAIMainStagePlatform();
        if (mainStage == null) return true;
        double safeX = clampGoalXAwayFromVoid(desiredX);
        double maxHorizontalError = Math.max(18.0, aiVoidHorizontalAllowance(mainStage) * 0.20);
        if (Math.abs(safeX - desiredX) > maxHorizontalError) return false;
        double maxDropDepth = mainStage.y + aiVoidDepthAllowance(mainStage);
        return targetY <= maxDropDepth;
    }

    private boolean isAIMainlandRecovered(boolean onGround, Platform standing, Platform mainStage) {
        if (mainStage == null) return true;
        if (!onGround) return false;
        return standing == mainStage;
    }

    private boolean shouldAIMaintainRecoveryLock(boolean onGround, Platform standing, Platform mainStage) {
        if (mainStage == null || aiVoidRecoveryLockFrames <= 0) return false;
        return !isAIMainlandRecovered(onGround, standing, mainStage);
    }

    private boolean shouldAIHoldRecoveryJump(Platform mainStage, double recoveryGoalX) {
        if (mainStage == null || aiCanUseAirRecovery()) return false;
        double centerX = x + 40 * sizeMultiplier;
        double bottomY = y + 80 * sizeMultiplier;
        double landingLeft = mainStage.x + aiVoidReentryInset(mainStage) * 0.55;
        double landingRight = mainStage.x + mainStage.w - aiVoidReentryInset(mainStage) * 0.55;
        boolean overLandingLane = centerX >= landingLeft && centerX <= landingRight;
        boolean alignedForLanding = Math.abs(recoveryGoalX - x) < 60.0;
        boolean safelyAboveIsland = bottomY < mainStage.y - 55.0;
        boolean stillBelowLip = bottomY > mainStage.y - 8.0;
        boolean driftingAway = (centerX < mainStage.x && vx < -0.8) || (centerX > mainStage.x + mainStage.w && vx > 0.8);
        if (overLandingLane && alignedForLanding && safelyAboveIsland) {
            return false;
        }
        return !overLandingLane || stillBelowLip || driftingAway;
    }

    private double aiRecoveryGoalX(Platform standing) {
        Platform mainStage = findAIMainStagePlatform();
        if (mainStage == null) return x;
        double halfWidth = 40 * sizeMultiplier;
        double mainCenterGoalX = mainStage.x + mainStage.w / 2.0 - halfWidth;
        if (standing != null && standing != mainStage && !isBoundaryPlatform(standing)) {
            double platformGoalLeft = standing.x - 28.0 - halfWidth;
            double platformGoalRight = standing.x + standing.w + 28.0 - halfWidth;
            double platformInnerLeft = standing.x + 18.0 - halfWidth;
            double platformInnerRight = standing.x + standing.w - 18.0 - halfWidth;
            if (mainCenterGoalX > platformInnerLeft && mainCenterGoalX < platformInnerRight) {
                return Math.abs(x - platformGoalLeft) <= Math.abs(x - platformGoalRight)
                        ? platformGoalLeft
                        : platformGoalRight;
            }
            return Math.clamp(mainCenterGoalX, platformGoalLeft, platformGoalRight);
        }
        double centerX = x + 40 * sizeMultiplier;
        double safeCenterX = Math.clamp(centerX,
                mainStage.x + aiVoidReentryInset(mainStage),
                mainStage.x + mainStage.w - aiVoidReentryInset(mainStage));
        return safeCenterX - 40 * sizeMultiplier;
    }

    private boolean isAIVoidRecoveryUrgent(boolean onGround, Platform standing) {
        if (!isVoidMap()) return false;
        Platform mainStage = findAIMainStagePlatform();
        if (mainStage == null) return false;
        if (isAIMainlandRecovered(onGround, standing, mainStage)) return false;
        double centerX = x + 40 * sizeMultiplier;
        double bottomY = y + 80 * sizeMultiplier;
        double hardLeft = mainStage.x - aiVoidHorizontalAllowance(mainStage);
        double hardRight = mainStage.x + mainStage.w + aiVoidHorizontalAllowance(mainStage);
        boolean tooFarOut = centerX < hardLeft || centerX > hardRight;
        boolean deepBelowStage = bottomY > mainStage.y + aiVoidDepthAllowance(mainStage);
        return tooFarOut || deepBelowStage;
    }

    private boolean isAIVoidRecoveryCaution(boolean onGround, Platform standing) {
        if (!isVoidMap()) return false;
        Platform mainStage = findAIMainStagePlatform();
        if (mainStage == null) return false;
        if (isAIMainlandRecovered(onGround, standing, mainStage)) return false;
        if (onGround && standing != null && standing != mainStage && !isBoundaryPlatform(standing)) {
            return true;
        }
        double centerX = x + 40 * sizeMultiplier;
        double bottomY = y + 80 * sizeMultiplier;
        double stageLeft = mainStage.x;
        double stageRight = mainStage.x + mainStage.w;
        double offstageDistance = centerX < stageLeft ? stageLeft - centerX
                : (centerX > stageRight ? centerX - stageRight : 0.0);
        if (offstageDistance <= 0.0) return false;
        double depth = Math.max(0.0, bottomY - mainStage.y);
        double horizontalRatio = offstageDistance / Math.max(1.0, aiVoidHorizontalAllowance(mainStage));
        double depthRatio = depth / Math.max(1.0, aiVoidDepthAllowance(mainStage));
        boolean movingAway = (centerX < stageLeft && vx < -1.4) || (centerX > stageRight && vx > 1.4);
        return switch (type) {
            case PENGUIN -> depth > 18.0 || horizontalRatio > 0.30 || movingAway || vy > 2.8;
            case PIGEON -> (!canDoubleJump && (depth > 36.0 || horizontalRatio > 0.22 || movingAway))
                    || depthRatio > 0.62
                    || (vy > 3.6 && depth > 22.0);
            default -> false;
        };
    }

    private boolean aiGoalLeavesMainStage(double goalX) {
        if (!isVoidMap()) return false;
        Platform mainStage = findAIMainStagePlatform();
        if (mainStage == null) return false;
        double goalCenterX = goalX + 40 * sizeMultiplier;
        return goalCenterX < mainStage.x || goalCenterX > mainStage.x + mainStage.w;
    }

    private boolean shouldAIJumpBeforeOffstage(double goalX) {
        if (!aiGoalLeavesMainStage(goalX)) return false;
        Platform mainStage = findAIMainStagePlatform();
        if (mainStage == null) return false;
        double centerX = x + 40 * sizeMultiplier;
        double goalCenterX = goalX + 40 * sizeMultiplier;
        double edgeX = goalCenterX < mainStage.x ? mainStage.x : mainStage.x + mainStage.w;
        double distanceToEdge = Math.abs(centerX - edgeX);
        return switch (type) {
            case VULTURE -> distanceToEdge < 140.0;
            case PIGEON, TURKEY, PELICAN, GRINCHHAWK, ROOSTER -> distanceToEdge < 95.0;
            default -> false;
        };
    }

    private boolean shouldAIUseRecoverySpecial(boolean onGround, Platform mainStage) {
        if (onGround || mainStage == null || specialCooldown > 0) return false;
        double centerX = x + 40 * sizeMultiplier;
        double bottomY = y + 80 * sizeMultiplier;
        double stageLeft = mainStage.x;
        double stageRight = mainStage.x + mainStage.w;
        boolean offstage = centerX < stageLeft || centerX > stageRight;
        double depth = bottomY - mainStage.y;
        double offstageDistance = centerX < stageLeft ? stageLeft - centerX
                : (centerX > stageRight ? centerX - stageRight : 0.0);
        boolean movingAway = (centerX < stageLeft && vx < -1.2) || (centerX > stageRight && vx > 1.2);
        return switch (type) {
            case PENGUIN -> offstage && (offstageDistance > 14.0 || depth > 10.0 || movingAway || vy > 1.2)
                    || depth > 55.0;
            case PIGEON -> !pigeonUpSpecialUsed
                    && (depth > 82.0
                    || (!canDoubleJump && (depth > 48.0 || (offstage && (offstageDistance > 10.0 || movingAway || vy > 2.2)))));
            default -> false;
        };
    }

    private boolean shouldAIUseUtilitySpecial(Bird target, PowerUp powerUp, boolean onGround,
                                              Platform climbPlatform, boolean powerFocus) {
        if (specialCooldown > 0) return false;
        double objectiveX;
        double objectiveY;
        if (powerFocus && powerUp != null) {
            objectiveX = powerUp.x;
            objectiveY = powerUp.y;
        } else if (target != null) {
            objectiveX = target.bodyCenterX();
            objectiveY = target.bodyCenterY();
        } else {
            return false;
        }
        double dx = Math.abs(objectiveX - bodyCenterX());
        double dy = objectiveY - bodyCenterY();
        if (type != BirdGame3.BirdType.PENGUIN) {
            return false;
        }
        return onGround
                && dx < 180.0
                && dy < -150.0
                && dy > -540.0
                && (climbPlatform != null || objectiveY < y - 180.0);
    }

    private boolean applyAIVoidRecoveryInputs(boolean onGround, Platform standing) {
        Platform mainStage = findAIMainStagePlatform();
        if (mainStage == null) return false;
        if (isAIMainlandRecovered(onGround, standing, mainStage)) {
            aiVoidRecoveryLockFrames = 0;
            return false;
        }
        boolean urgent = isAIVoidRecoveryUrgent(onGround, standing);
        boolean caution = isAIVoidRecoveryCaution(onGround, standing);
        boolean locked = shouldAIMaintainRecoveryLock(onGround, standing, mainStage);
        if (!urgent && !caution && !locked) return false;
        aiVoidRecoveryLockFrames = Math.max(aiVoidRecoveryLockFrames, urgent ? 40 : 26);
        resetAIDropCommit();
        aiDirectionLock = 0;
        aiStrafeHoldFrames = 0;
        aiStrafeTimer = 0;
        aiMicroPause = 0;
        double recoveryGoalX = aiRecoveryGoalX(standing);
        if (Math.abs(recoveryGoalX - x) > 18) {
            boolean moveLeft = recoveryGoalX < x;
            game.setAiControlKey(playerIndex, moveLeft ? leftKey() : rightKey(), true);
            facingRight = !moveLeft;
        }
        if (!onGround && shouldAIHoldRecoveryJump(mainStage, recoveryGoalX)) {
            game.setAiControlKey(playerIndex, jumpKey(), true);
        }
        if (shouldAIUseRecoverySpecial(onGround, mainStage)) {
            game.setAiControlKey(playerIndex, specialKey(), true);
        }
        return true;
    }

    private Platform findClimbPlatform(double targetX, double maxRise) {
        Platform best = null;
        double bestScore = -Double.MAX_VALUE;
        double myCx = x + 40;
        double practicalRise = Math.min(maxRise, aiPlatformRiseReach());
        for (Platform p : game.platforms) {
            if (isBoundaryPlatform(p)) continue;
            if (p.y >= y - 40) continue;
            double rise = y - p.y;
            if (rise <= 0 || rise > practicalRise) continue;
            double centerX = p.x + p.w / 2.0;
            double dxTarget = Math.abs(centerX - targetX);
            double dxMe = Math.abs(centerX - myCx);
            double horizontalReach = aiPlatformHorizontalReach(rise);
            if (dxMe > horizontalReach && dxTarget > horizontalReach * 1.25) continue;
            double score = 0;
            double progress = Math.abs(targetX - myCx) - dxTarget;
            score -= rise * 0.95;
            score -= dxTarget * 0.72;
            score -= dxMe * 0.4;
            score += progress * 0.45;
            if ((centerX >= Math.min(myCx, targetX) && centerX <= Math.max(myCx, targetX))) {
                score += 26.0;
            }
            if (rise < practicalRise * 0.65) {
                score += 18.0;
            }
            if (score > bestScore) {
                bestScore = score;
                best = p;
            }
        }
        return best;
    }

    private double aiPlatformRiseReach() {
        double reach = type.jumpHeight * 17.5;
        double flyLift = currentFlyUpForce();
        if (flyLift > 0.0) {
            reach += 90.0 + flyLift * 190.0;
        }
        switch (type) {
            case PIGEON -> {
                if (canDoubleJump) reach += 115.0;
                if (!pigeonUpSpecialUsed) reach += 150.0;
            }
            case PENGUIN -> {
                if (specialCooldown <= 0) reach += 210.0;
            }
            case TITMOUSE, HUMMINGBIRD, BAT -> reach += 95.0;
            case VULTURE -> {
                if (!isOnGround() || isFlying) {
                    reach += 80.0 + Math.max(0.0, -vy) * 12.0;
                }
            }
            case TURKEY, PELICAN, GRINCHHAWK, ROOSTER -> {
                if (limitedFlightFuel > 0.0) reach += 55.0;
            }
            default -> {
            }
        }
        return Math.clamp(reach, 180.0, 760.0);
    }

    private double aiPlatformHorizontalReach(double rise) {
        double reach = 155.0 + type.speed * 82.0;
        if (currentFlyUpForce() > 0.0) {
            reach += 60.0;
        }
        switch (type) {
            case PENGUIN -> {
                if (specialCooldown <= 0) reach += 85.0;
            }
            case TITMOUSE, HUMMINGBIRD, BAT -> reach += 95.0;
            case TURKEY, PELICAN, GRINCHHAWK, ROOSTER -> reach += 35.0;
            default -> {
            }
        }
        if (rise < 150.0) {
            reach += 80.0;
        } else if (rise > 320.0) {
            reach -= 35.0;
        }
        return Math.clamp(reach, 160.0, 620.0);
    }

    private double getAIIdealRange() {
        return switch (type) {
            case TURKEY, PELICAN, GRINCHHAWK -> 165;
            case ROADRUNNER -> 182;
            case RAZORBILL, SHOEBILL -> 188;
            case EAGLE, VULTURE, PENGUIN, PHOENIX -> 208;
            case FALCON -> 202;
            case HUMMINGBIRD, TITMOUSE -> 220;
            case OPIUMBIRD, HEISENBIRD, MOCKINGBIRD -> 205;
            case BAT -> 214;
            case PIGEON -> 190;
            case ROOSTER -> 195;
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
        double skill = Math.clamp((cpuLevel - 1) / 8.0, 0.0, 1.0);
        if (random.nextDouble() > 0.25 + 0.75 * skill) return false;

        if (dir < 0) game.setAiControlKey(playerIndex, leftKey(), true);
        else game.setAiControlKey(playerIndex, rightKey(), true);
        if (onGround && aiJumpCooldown <= 0) {
            game.setAiControlKey(playerIndex, jumpKey(), true);
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
                return y < BirdGame3.GROUND_Y - 800 && dy > 180 && dist < 520;
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
                        || (dy < -140 && dist < 520)
                        || (!onGround && isVoidMap() && dist < 420 && dy < 140);
            case ROADRUNNER:
                return (onGround && dist < 330 && Math.abs(dy) < 130)
                        || (lowHealth && dist < 260)
                        || (onGround && target.health > health + 12 && dist < 380);
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
            case ROOSTER: {
                int owned = 0;
                for (ChickMinion chick : game.chickMinions) {
                    if (chick.owner == this) owned++;
                }
                return owned < 3 && (dist < 360 || lowHealth);
            }
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
        return type == BirdGame3.BirdType.PIGEON
                || type == BirdGame3.BirdType.TURKEY
                || type == BirdGame3.BirdType.GRINCHHAWK
                || type == BirdGame3.BirdType.PELICAN
                || type == BirdGame3.BirdType.ROOSTER;
    }

    private boolean roadrunnerSandstormActive() {
        return type == BirdGame3.BirdType.ROADRUNNER && roadrunnerSandstormTimer > 0;
    }

    private double currentFlyUpForce() {
        return roadrunnerSandstormActive() ? ROADRUNNER_SANDSTORM_FLY_LIFT : type.flyUpForce;
    }

    private boolean photoEagleSkinActive() {
        return type == BirdGame3.BirdType.EAGLE && isPhotoEagleSkin;
    }

    private static Image loadPhotoEagleImage(String resourcePath) {
        try {
            var url = Bird.class.getResource(resourcePath);
            if (url == null) {
                return null;
            }
            Image image = new Image(url.toExternalForm(), false);
            return image.isError() ? null : image;
        } catch (Exception ignored) {
            return null;
        }
    }

    private static Image photoEagleIdleImage() {
        if (photoEagleIdleSprite == null) {
            photoEagleIdleSprite = loadPhotoEagleImage("/eagle.png");
        }
        return photoEagleIdleSprite;
    }

    private static Image photoEagleAttackImage() {
        if (photoEagleAttackSprite == null) {
            photoEagleAttackSprite = loadPhotoEagleImage("/eagle_attack.png");
        }
        return photoEagleAttackSprite;
    }

    private static Image photoEagleFlapImage() {
        if (photoEagleFlapSprite == null) {
            photoEagleFlapSprite = loadPhotoEagleImage("/eagle_flap.png");
        }
        return photoEagleFlapSprite;
    }

    private Image currentPhotoEagleSprite() {
        if (!photoEagleSkinActive()) {
            return null;
        }
        if (attackAnimationTimer > 0) {
            Image attack = photoEagleAttackImage();
            if (attack != null) {
                return attack;
            }
        }
        if (!isOnGround() || raptorSpecialActive() || diveTimer > 0 || eagleDiveActive || eagleAscentActive) {
            Image flap = photoEagleFlapImage();
            if (flap != null) {
                return flap;
            }
        }
        Image idle = photoEagleIdleImage();
        return idle != null ? idle : photoEagleAttackImage();
    }

    private boolean drawPhotoEagleSprite(GraphicsContext g, double drawSize, AttackVisualPose pose) {
        Image sprite = currentPhotoEagleSprite();
        if (sprite == null) {
            return false;
        }

        double maxWidth = drawSize * 1.9;
        double maxHeight = drawSize * 1.65;
        double aspect = sprite.getWidth() > 0 && sprite.getHeight() > 0
                ? sprite.getWidth() / sprite.getHeight()
                : 1.0;
        double renderWidth = maxWidth;
        double renderHeight = renderWidth / aspect;
        if (renderHeight > maxHeight) {
            renderHeight = maxHeight;
            renderWidth = renderHeight * aspect;
        }

        double renderCenterX = x + drawSize / 2.0;
        double renderCenterY = y + drawSize / 2.0 + 4 * sizeMultiplier;
        double rotation = pose == null ? 0.0 : pose.spriteRotationDegrees();
        double scaleX = pose == null ? 1.0 : pose.spriteScaleX();
        double scaleY = pose == null ? 1.0 : pose.spriteScaleY();

        g.save();
        g.translate(renderCenterX, renderCenterY);
        g.rotate(rotation);
        g.scale((facingRight ? 1.0 : -1.0) * scaleX, scaleY);
        g.drawImage(sprite, -renderWidth / 2.0, -renderHeight / 2.0, renderWidth, renderHeight);
        g.restore();
        return true;
    }

    public void update(double gameSpeed) {
        try {
            if (health > 0 && game.isAI[playerIndex]) aiControl();

        // === UPDATE TIMERS ===
        updateTimers(gameSpeed);
        applyPendingSmashLaunch();

        if (health <= 0) {
            updateDefeatedState(gameSpeed);
            return;
        }

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
        boolean leftHeld = leftPressed();
        boolean rightHeld = rightPressed();
        boolean jumpHeld = jumpPressed();
        boolean specialHeld = specialPressed();
        boolean grabHeld = grabPressed();
        boolean blockHeld = blockPressed();
        boolean reserveJumpForSpecial = !stunned && shouldReserveJumpForSpecial();
        boolean jumpJustPressed = jumpHeld && !jumpHeldLastFrame && !reserveJumpForSpecial;
        boolean grabJustPressed = grabHeld && !grabHeldLastFrame;
        boolean leftJustPressed = leftHeld && !leftHeldLastFrame;
        boolean rightJustPressed = rightHeld && !rightHeldLastFrame;
        boolean downHeld = !stunned && blockHeld;
        boolean inDockWater = isInDockWater();
        boolean inWindNow = isInWindVent(x, y);
        boolean inUpdraft = inWindNow || thermalTimer > 0;
        boolean reserveBlockForSpecial = !stunned && shouldReserveBlockForSpecial();
        boolean reserveBlockForAttack = !stunned && shouldReserveBlockForAttack(airborne);
        boolean defensiveBlockHeld = blockHeld && !reserveBlockForAttack && !reserveBlockForSpecial;
        boolean blockJustPressed = defensiveBlockHeld && !blockHeldLastFrame;

        if (type == BirdGame3.BirdType.PIGEON && (isOnGround() || ledgeHanging || batHanging || onVine || inDockWater)) {
            pigeonUpSpecialUsed = false;
        }
        if (isRaptor() && isOnGround()) {
            raptorUpSpecialUsed = false;
        }

        if (airborne && landingLagTimer > 0) {
            landingLagTimer = 0;
        }
        if (airborne && knockdownTimer > 0) {
            clearKnockdownState();
        }
        if (jumpSquatTimer > 0 && (stunned || airborne || inDockWater || grabbedBy != null || ledgeHanging || batHanging || onVine || isGrappling)) {
            clearJumpSquat();
        }
        if (blockJustPressed && stunTime > 0.0) {
            techBufferTimer = TECH_INPUT_BUFFER_FRAMES;
        }

        if (stunned) {
            cancelAttackCharge();
            attackHeldLastFrame = attackPressed();
            resetPigeonSpecialState();
            resetRaptorSpecialState();
        }

        if (handleGrabbedState()) {
            rememberFrameInputs(jumpHeld, specialHeld, blockHeld, grabHeld, leftHeld, rightHeld);
            if (tauntTimer > 0) tauntTimer--;
            return;
        }
        if (handleHoldingGrabState(stunned, inDockWater)) {
            rememberFrameInputs(jumpHeld, specialHeld, blockHeld, grabHeld, leftHeld, rightHeld);
            if (tauntTimer > 0) tauntTimer--;
            return;
        }

        if (type == BirdGame3.BirdType.BAT && handleBatHanging(stunned)) {
            rememberFrameInputs(jumpHeld, specialHeld, blockHeld, grabHeld, leftHeld, rightHeld);
            handleTaunts();
            if (tauntTimer > 0) tauntTimer--;
            return;
        }
        if (handleLedgeHanging(stunned)) {
            rememberFrameInputs(jumpHeld, specialHeld, blockHeld, grabHeld, leftHeld, rightHeld);
            handleTaunts();
            if (tauntTimer > 0) tauntTimer--;
            return;
        }

        handleDodgeInput(stunned, airborne, inDockWater, blockJustPressed, grabJustPressed,
                leftHeld, rightHeld, leftJustPressed, rightJustPressed);
        updateShieldState(stunned, airborne, defensiveBlockHeld, inDockWater, gameSpeed);
        if (isDodging()) {
            downHeld = false;
        }

        // === GRAVITY ===
        double gravityScale = 1.0;
        if (type == BirdGame3.BirdType.BAT && !isOnGround()) {
            gravityScale = 0.66;
        }
        if (inDockWater) {
            gravityScale *= DOCK_WATER_GRAVITY_SCALE;
        }
        vy += BirdGame3.GRAVITY * gravityScale * gameSpeed;
        if (airborne && downHeld && !inDockWater) {
            double accel = inUpdraft ? FAST_FALL_UPDRAFT_ACCEL : FAST_FALL_ACCEL;
            vy += accel * gameSpeed;
            if (!inUpdraft && vy > FAST_FALL_MAX) vy = FAST_FALL_MAX;
        }
        if (inDockWater) {
            applyDockWaterPhysics(stunned, downHeld, gameSpeed);
        }

        // === EAGLE PASSIVE ===
        handleEaglePassive(airborne);

        // === VULTURE FLYING ===
        if (type == BirdGame3.BirdType.VULTURE && !stunned && jumpPressed() && !inDockWater) {
            isFlying = true;
            vy -= 0.65;
            if (vy < -6.0) vy = -6.0;
        } else if (type == BirdGame3.BirdType.VULTURE) {
            isFlying = false;
            if (vy < 0) vy += 0.3;
        }

        // === FLY/GLIDE ===
        if (!stunned && jumpPressed() && airborne && !inDockWater
                && !(type == BirdGame3.BirdType.PIGEON && pigeonFlutterTimer > 0)
                && !(isRaptor() && raptorClimbTimer > 0)) {
            double flyLift = currentFlyUpForce();
            boolean limitedFlight = hasLimitedFlight();
            boolean thermalActive = thermalTimer > 0;
            double speedRatio = baseSpeedMultiplier > 0 ? speedMultiplier / baseSpeedMultiplier : 1.0;
            double flightLiftScale = Math.clamp(1.0 + (speedRatio - 1.0) * 0.55, 0.8, 1.45);
            double topOverflow = topCameraOverflow();
            boolean aboveCameraReach = topOverflow > 0.0;
            // Thermal Rise should always remain effective even if limited-flight fuel is drained.
            if ((!limitedFlight || limitedFlightFuel > 0 || thermalActive) && (flyLift > 0.0 || thermalLift > 0.0)) {
                if (!aboveCameraReach) {
                    vy -= (flyLift + thermalLift) * hoverRegenMultiplier * flightLiftScale;
                } else {
                    vy += (0.65 + Math.min(4.8, topOverflow * 0.045)) * gameSpeed;
                }
                double limitedFlightCap = type == BirdGame3.BirdType.ROOSTER ? -12.4 : -6.4;
                double limitedFlightThermalCap = type == BirdGame3.BirdType.ROOSTER ? -14.2 : -9.2;
                if (limitedFlight && !thermalActive) {
                    limitedFlightFuel = Math.max(0, limitedFlightFuel - gameSpeed);
                    if (vy < limitedFlightCap && !aboveCameraReach) vy = limitedFlightCap;
                } else if (limitedFlight) {
                    if (vy < limitedFlightThermalCap && !aboveCameraReach) vy = limitedFlightThermalCap;
                }
            }
            if (type == BirdGame3.BirdType.BAT && !aboveCameraReach) {
                // Bat gets stronger sustained lift so it can truly dogfight in the air.
                vy -= 0.55 * flightLiftScale;
                if (vy < -11.5) vy = -11.5;
            }
            if (type == BirdGame3.BirdType.PHOENIX) {
                if (phoenixRebornActive && !aboveCameraReach) {
                    vy -= 0.4 * flightLiftScale;
                    if (vy < -11.5) vy = -11.5;
                } else if (!aboveCameraReach && vy < -8.5) {
                    vy = -8.5;
                }
            }
        }

        if (hasLimitedFlight() && isOnGround()) {
            limitedFlightFuel = LIMITED_FLIGHT_MAX;
        }

        // === THERMAL SOARING ===
        if (thermalTimer > 0 && vy > 0 && !inDockWater) {
            vy *= 0.85;
        }

        // === TITMOUSE ZIP ===
        handleTitmouseZip();

        // === HORIZONTAL MOVEMENT & JUMPING ===
        handleHorizontalMovement(stunned, airborne, jumpHeld, jumpJustPressed, gameSpeed);

        // === AIR/GROUND FRICTION ===
        if (!leftPressed() && !rightPressed()) {
            vx *= airborne ? 0.96 : 0.80;
        }
        if (neonRushTimer > 0) {
            int dir = facingRight ? 1 : -1;
            vx += dir * 0.7;
            if (Math.abs(vx) > 28) vx = Math.signum(vx) * 28;
        }
        handlePigeonSpecialState();
        handleRaptorSpecialState();

        // === RAZORBILL DASH ===
        handleRazorbillBladeStorm();

        // === APPLY VELOCITY ===
        double prevX = x;
        double prevY = y;
        x += vx;
        y += vy;
        if (grabbedTarget != null) {
            syncGrabbedTargetPosition();
        }

        // === THERMALS & WIND VENTS ===
        handleThermals(downHeld, prevX, prevY);
        applyPenguinDashDamage();
        handleHummingbirdFrenzy();
        handlePhoenixAfterburn();
        emitRoadrunnerDust();
        handleRoadrunnerSandstorm();
        if (tryGrabUniversalLedge(prevX, inDockWater)) {
            rememberFrameInputs(jumpHeld, specialHeld, blockHeld, grabHeld, leftHeld, rightHeld);
            handleTaunts();
            if (tauntTimer > 0) tauntTimer--;
            return;
        }

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
        handleBoundaries(gameSpeed, airborne, prevX, prevY);

        // === EAGLE DIVE / ASCENT DAMAGE ===
        handleEagleDiveImpact();

        // === VULTURE FEAST ===
        handleVultureFeast();

        // === POWER-UP PICKUP ===
        handlePowerUpPickup();

        // === TAUNTS ===
        handleTaunts();

            if (tauntTimer > 0) tauntTimer--;
            rememberFrameInputs(jumpHeld, specialHeld, blockHeld, grabHeld, leftHeld, rightHeld);
        } finally {
            updateDisplayPose(gameSpeed);
        }
    }

    private void updateDefeatedState(double gameSpeed) {
        onDefeated();

        vy += BirdGame3.GRAVITY * gameSpeed;
        if (vy > FAST_FALL_MAX) vy = FAST_FALL_MAX;

        x += vx;
        y += vy;
        vx *= 0.94;

        double leftBound = 50;
        double rightBound = BirdGame3.WORLD_WIDTH - 150 * sizeMultiplier;
        if (usesIslandBounds()) {
            double battlefieldLeft = game.battlefieldLeftBound();
            double battlefieldRight = game.battlefieldRightBound();
            leftBound = battlefieldLeft + 50;
            rightBound = battlefieldRight - 150 * sizeMultiplier;
        }

        if (x < leftBound) {
            x = leftBound;
            vx = Math.max(0, vx);
        }
        if (x > rightBound) {
            x = rightBound;
            vx = Math.min(0, vx);
        }
        if (y < BirdGame3.CEILING_Y) {
            y = BirdGame3.CEILING_Y;
            vy = Math.max(vy, 0);
        }

        handleVerticalCollision(false);
        if (y > BirdGame3.WORLD_HEIGHT + 400) {
            y = BirdGame3.WORLD_HEIGHT + 400;
            vx = 0;
            vy = 0;
        }
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
        ultimateFxTimer = Math.max(0, (int)(ultimateFxTimer - gameSpeed));
        roadrunnerSandstormTimer = Math.max(0, (int)(roadrunnerSandstormTimer - gameSpeed));
        roadrunnerSandGustTimer = Math.max(0, (int)(roadrunnerSandGustTimer - gameSpeed));
        pigeonFeatherBurstTimer = Math.max(0, (int)(pigeonFeatherBurstTimer - gameSpeed));
        pigeonRushTimer = Math.max(0, (int)(pigeonRushTimer - gameSpeed));
        pigeonFlutterTimer = Math.max(0, (int)(pigeonFlutterTimer - gameSpeed));
        pigeonScavengeTimer = Math.max(0, (int)(pigeonScavengeTimer - gameSpeed));
        raptorCryTimer = Math.max(0, (int)(raptorCryTimer - gameSpeed));
        raptorRushTimer = Math.max(0, (int)(raptorRushTimer - gameSpeed));
        raptorClimbTimer = Math.max(0, (int)(raptorClimbTimer - gameSpeed));
        raptorCryReuseTimer = Math.max(0, (int)(raptorCryReuseTimer - gameSpeed));
        raptorRushReuseTimer = Math.max(0, (int)(raptorRushReuseTimer - gameSpeed));
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
        for (int i = 0; i < roadrunnerSandHitCooldown.length; i++) {
            roadrunnerSandHitCooldown[i] = Math.max(0, (int)(roadrunnerSandHitCooldown[i] - gameSpeed));
        }
        if (pigeonFeatherBurstTimer == 0) {
            pigeonFeatherBurstUltimate = false;
        }
        if (pigeonRushTimer == 0) {
            pigeonRushGrounded = false;
            pigeonRushUltimate = false;
            Arrays.fill(pigeonRushHit, false);
        }
        if (pigeonFlutterTimer == 0) {
            pigeonFlutterUltimate = false;
            Arrays.fill(pigeonFlutterHit, false);
        }
        if (pigeonScavengeTimer == 0) {
            pigeonScavengeAirborne = false;
            pigeonScavengeUltimate = false;
            pigeonScavengeResolved = false;
        }
        if (raptorCryTimer == 0) {
            raptorCryUltimate = false;
        }
        if (raptorRushTimer == 0) {
            raptorRushUltimate = false;
            raptorRushGrounded = false;
            raptorRushDirection = 1;
            Arrays.fill(raptorRushHit, false);
        }
        if (raptorClimbTimer == 0) {
            raptorClimbUltimate = false;
            raptorClimbDirection = 1;
            Arrays.fill(raptorClimbHit, false);
        }
        nullRockInvincibilityTimer = Math.max(0, (int) (nullRockInvincibilityTimer - gameSpeed));
        nullRockShieldFxCooldown = Math.max(0, (int) (nullRockShieldFxCooldown - gameSpeed));
        if (recentSmashAttackerFrames > 0) {
            recentSmashAttackerFrames = Math.max(0, recentSmashAttackerFrames - (int) Math.max(1.0, gameSpeed));
            if (recentSmashAttackerFrames == 0) {
                recentSmashAttackerIndex = -1;
            }
        }

        stunTime = Math.max(0, stunTime - gameSpeed);
        if (isStunImmune()) {
            stunTime = 0;
        }
        if (specialCooldown > 0) specialCooldown = (int)Math.max(0, specialCooldown - gameSpeed);
        if (crowSwarmCooldown > 0) crowSwarmCooldown = (int)Math.max(0, crowSwarmCooldown - gameSpeed);
        if (attackCooldown > 0) attackCooldown = (int)Math.max(0, attackCooldown - gameSpeed);
        if (grabCooldown > 0) grabCooldown = (int)Math.max(0, grabCooldown - gameSpeed);
        if (landingLagTimer > 0) landingLagTimer = (int)Math.max(0, landingLagTimer - gameSpeed);
        if (!(eagleDiveActive && eagleDiveCountdown > 0 && !eagleAscentActive)) {
            diveTimer = Math.max(0, (int)(diveTimer - gameSpeed));
        }
        if (attackAnimationTimer > 0) {
            attackAnimationTimer = (int)Math.max(0, attackAnimationTimer - gameSpeed);
            if (attackAnimationTimer == 0) {
                clearAerialAttackState();
            }
        }
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
        techBufferTimer = Math.max(0, (int)(techBufferTimer - gameSpeed));
        knockdownTimer = Math.max(0, (int)(knockdownTimer - gameSpeed));
        dodgeCooldown = Math.max(0, (int)(dodgeCooldown - gameSpeed));
        dodgeTimer = Math.max(0, (int)(dodgeTimer - gameSpeed));
        dodgeInvulnerabilityTimer = Math.max(0, (int)(dodgeInvulnerabilityTimer - gameSpeed));
        if (dodgeTimer == 0) {
            clearActiveDodge();
        }
        shieldStunFrames = Math.max(0, (int) (shieldStunFrames - gameSpeed));
        parryWindowFrames = Math.max(0, (int) (parryWindowFrames - gameSpeed));
        if (!isBlocking && shieldHealth < SHIELD_MAX_HEALTH) {
            shieldHealth = Math.min(SHIELD_MAX_HEALTH, shieldHealth + SHIELD_REGEN_PER_FRAME * gameSpeed);
        }
        batEchoTimer = Math.max(0, (int)(batEchoTimer - gameSpeed));
        batHangLockTimer = Math.max(0, (int)(batHangLockTimer - gameSpeed));
        batRehangCooldownTimer = Math.max(0, (int)(batRehangCooldownTimer - gameSpeed));
        ledgeLockTimer = Math.max(0, (int)(ledgeLockTimer - gameSpeed));
        ledgeRegrabCooldownTimer = Math.max(0, (int)(ledgeRegrabCooldownTimer - gameSpeed));
        ledgeInvulnerabilityTimer = Math.max(0, (int)(ledgeInvulnerabilityTimer - gameSpeed));
        if (isShrinkImmune()) {
            shrinkTimer = 0;
            if (sizeMultiplier < baseSizeMultiplier) {
                sizeMultiplier = baseSizeMultiplier;
            }
        }
    }

    private boolean isDodging() {
        return dodgeType != DodgeType.NONE && dodgeTimer > 0;
    }

    private void clearActiveDodge() {
        dodgeType = DodgeType.NONE;
        dodgeTimer = 0;
        dodgeInvulnerabilityTimer = 0;
        dodgeDirection = 0;
    }

    private void resetDodgeState() {
        clearActiveDodge();
        dodgeCooldown = 0;
        airDodgeAvailable = true;
    }

    private void refreshAirDodge() {
        airDodgeAvailable = true;
        if (dodgeType == DodgeType.AIR) {
            clearActiveDodge();
        }
    }

    private void clearJumpSquat() {
        jumpSquatTimer = 0;
        shortHopQueued = false;
    }

    private void clearAerialAttackState() {
        aerialAttackActive = false;
        aerialAttackTotalFrames = 0;
        activeAerialLandingLagFrames = AERIAL_LANDING_LAG_FRAMES;
        activeAttackVariant = NormalAttackVariant.NEUTRAL;
    }

    private int aerialAttackElapsedFrames() {
        return Math.max(0, aerialAttackTotalFrames - attackAnimationTimer);
    }

    private boolean aerialAttackAutoCancelsOnLanding() {
        if (!aerialAttackActive) {
            return true;
        }
        return aerialAttackElapsedFrames() <= AERIAL_AUTO_CANCEL_STARTUP_FRAMES
                || attackAnimationTimer <= AERIAL_AUTO_CANCEL_LATE_FRAMES;
    }

    private void resolveAerialLandingRecovery() {
        if (!aerialAttackActive) {
            return;
        }
        boolean autoCancel = aerialAttackAutoCancelsOnLanding();
        attackAnimationTimer = 0;
        if (!autoCancel) {
            landingLagTimer = Math.max(landingLagTimer, activeAerialLandingLagFrames);
            vx *= 0.55;
        }
        clearAerialAttackState();
    }

    private void clearTechBuffer() {
        techBufferTimer = 0;
    }

    private void clearKnockdownState() {
        knockdownTimer = 0;
    }

    private void clearLandingTechCombatState() {
        attackAnimationTimer = 0;
        clearAerialAttackState();
        landingLagTimer = 0;
        clearJumpSquat();
        isBlocking = false;
        parryWindowFrames = 0;
        shieldStunFrames = 0;
    }

    private void enterMissedTechKnockdown() {
        clearTechBuffer();
        knockdownTimer = MISSED_TECH_KNOCKDOWN_FRAMES;
        stunTime = 0.0;
        clearActiveDodge();
        vx *= 0.18;
        vy = 0.0;
    }

    private boolean resolveGroundTechOrKnockdown(double impactVy) {
        if (!game.usesSmashCombatRules() || stunTime <= 0.0 || impactVy < GROUND_TECH_MIN_IMPACT_SPEED) {
            return false;
        }

        clearLandingTechCombatState();
        clearKnockdownState();
        int techDir = rightPressed() && !leftPressed() ? 1 : leftPressed() && !rightPressed() ? -1 : 0;
        if (techBufferTimer > 0) {
            clearTechBuffer();
            stunTime = 0.0;
            if (techDir == 0) {
                startSpotDodge();
            } else {
                startRoll(techDir);
            }
        } else {
            enterMissedTechKnockdown();
        }
        return true;
    }

    private boolean isTechableWallSurface(Platform p) {
        boolean isFloor = p.w >= BirdGame3.WORLD_WIDTH - 10 && p.h >= 200;
        boolean isCaveCeiling = game.selectedMap == MapType.CAVE &&
                p.y <= 1 && p.h >= 60 && p.w >= BirdGame3.WORLD_WIDTH - 10;
        if (isFloor || isCaveCeiling) {
            return false;
        }
        if (isBoundaryPlatform(p)) {
            return p.h >= BirdGame3.WORLD_HEIGHT - 10 && p.w <= 150;
        }
        return p.h >= 24.0;
    }

    private void resolveWallImpact(double snappedX, int bounceDir) {
        x = snappedX;
        clearLandingTechCombatState();
        clearKnockdownState();
        if (techBufferTimer > 0) {
            clearTechBuffer();
            stunTime = 0.0;
            clearActiveDodge();
            dodgeInvulnerabilityTimer = Math.max(dodgeInvulnerabilityTimer, TECH_INVULNERABILITY_FRAMES);
            dodgeCooldown = Math.max(dodgeCooldown, DODGE_COOLDOWN_FRAMES / 2);
            vx = 0.0;
            vy = Math.min(vy * 0.35, 1.4);
        } else {
            clearTechBuffer();
            vx = Math.abs(vx) * WALL_BOUNCE_SPEED_SCALE * bounceDir;
            vy *= WALL_BOUNCE_VERTICAL_DAMPING;
        }
    }

    private void handleWallTechCollision(double prevX, double prevY) {
        if (!game.usesSmashCombatRules() || stunTime <= 0.0 || Math.abs(vx) < WALL_TECH_MIN_IMPACT_SPEED) {
            return;
        }

        double currentLeft = x;
        double currentRight = x + bodyWidth();
        double previousRight = prevX + bodyWidth();
        double currentTop = y;
        double currentBottom = bodyBottomY();
        double previousBottom = prevY + bodyHeight();

        for (Platform p : game.platforms) {
            if (!isTechableWallSurface(p)) {
                continue;
            }

            double verticalOverlap = Math.min(currentBottom, p.y + p.h) - Math.max(currentTop, p.y);
            if (verticalOverlap < Math.max(14.0, bodyHeight() * 0.20)) {
                continue;
            }
            if (previousBottom <= p.y + 6.0 && y <= p.y + 6.0) {
                continue;
            }

            if (previousRight <= p.x && currentRight >= p.x && vx > 0.0) {
                resolveWallImpact(p.x - bodyWidth(), -1);
                return;
            }
            if (prevX >= p.x + p.w && currentLeft <= p.x + p.w && vx < 0.0) {
                resolveWallImpact(p.x + p.w, 1);
                return;
            }
        }
    }

    private void startGroundJumpSquat() {
        clearJumpSquat();
        jumpSquatTimer = JUMP_SQUAT_FRAMES;
        if (isBlocking) {
            isBlocking = false;
            parryWindowFrames = 0;
            blockCooldown = Math.max(blockCooldown, SHIELD_DROP_COOLDOWN_FRAMES);
        }
    }

    private void recordJumpHeightAchievements() {
        if (game.selectedMap == MapType.CITY && y < BirdGame3.GROUND_Y - 500) {
            game.recordHighRooftopJumpAchievement(playerIndex);
        }
        if (game.selectedMap == MapType.SKYCLIFFS && y < BirdGame3.GROUND_Y - 1000) {
            game.recordHighCliffJumpAchievement(playerIndex);
        }
    }

    private void launchGroundJump() {
        double jumpScale = shortHopQueued ? SHORT_HOP_MULTIPLIER : 1.0;
        clearJumpSquat();
        vy = -type.jumpHeight * jumpScale;
        game.playSwingSfx();
        recordJumpHeightAchievements();
    }

    private void advanceGroundJumpSquat(boolean jumpHeld, double gameSpeed) {
        if (jumpSquatTimer <= 0) {
            return;
        }
        if (!jumpHeld) {
            shortHopQueued = true;
        }
        jumpSquatTimer = Math.max(0, (int) (jumpSquatTimer - gameSpeed));
        if (jumpSquatTimer == 0 && isOnGround()) {
            launchGroundJump();
        }
    }

    private void startSpotDodge() {
        clearActiveDodge();
        dodgeType = DodgeType.SPOT;
        dodgeTimer = SPOT_DODGE_FRAMES;
        dodgeInvulnerabilityTimer = SPOT_DODGE_INVULNERABILITY_FRAMES;
        dodgeCooldown = Math.max(dodgeCooldown, DODGE_COOLDOWN_FRAMES);
        isBlocking = false;
        parryWindowFrames = 0;
        shieldStunFrames = 0;
        vx *= 0.35;
    }

    private void startRoll(int dir) {
        if (dir == 0) {
            dir = facingRight ? 1 : -1;
        }
        clearActiveDodge();
        dodgeType = DodgeType.ROLL;
        dodgeTimer = ROLL_DODGE_FRAMES;
        dodgeInvulnerabilityTimer = ROLL_DODGE_INVULNERABILITY_FRAMES;
        dodgeCooldown = Math.max(dodgeCooldown, DODGE_COOLDOWN_FRAMES);
        dodgeDirection = dir;
        isBlocking = false;
        parryWindowFrames = 0;
        shieldStunFrames = 0;
        facingRight = dir > 0;
        vx = dir * Math.max(ROLL_DODGE_SPEED, type.speed * speedMultiplier * 1.18);
    }

    private void startAirDodge(int dir) {
        clearActiveDodge();
        dodgeType = DodgeType.AIR;
        dodgeTimer = AIR_DODGE_FRAMES;
        dodgeInvulnerabilityTimer = AIR_DODGE_INVULNERABILITY_FRAMES;
        dodgeCooldown = Math.max(dodgeCooldown, DODGE_COOLDOWN_FRAMES);
        dodgeDirection = dir;
        airDodgeAvailable = false;
        if (dir != 0) {
            vx = dir * AIR_DODGE_SPEED;
            facingRight = dir > 0;
        } else {
            vx *= 0.35;
        }
        vy = Math.min(vy * 0.35, AIR_DODGE_STALL_VELOCITY);
    }

    private void handleDodgeInput(boolean stunned, boolean airborne, boolean inDockWater,
                                  boolean blockJustPressed, boolean grabJustPressed,
                                  boolean leftPressed, boolean rightPressed,
                                  boolean leftJustPressed, boolean rightJustPressed) {
        if ((type == BirdGame3.BirdType.PIGEON && pigeonSpecialActive())
                || (isRaptor() && raptorSpecialActive())) {
            return;
        }
        if (stunned || inDockWater || health <= 0 || dodgeCooldown > 0 || isDodging()
                || landingLagTimer > 0 || knockdownTimer > 0) {
            return;
        }

        if (airborne) {
            if (blockJustPressed
                    && airDodgeAvailable
                    && attackAnimationTimer <= 0
                    && !onVine
                    && !batHanging
                    && !ledgeHanging
                    && !isGrappling) {
                int dir = rightPressed && !leftPressed ? 1 : leftPressed && !rightPressed ? -1 : 0;
                startAirDodge(dir);
            }
            return;
        }

        if (!blockPressed() || !blockHeldLastFrame || !isBlocking || shieldStunFrames > 0) {
            return;
        }

        if (grabJustPressed) {
            startSpotDodge();
            return;
        }

        if (leftJustPressed && !rightPressed) {
            startRoll(-1);
            return;
        }

        if (rightJustPressed && !leftPressed) {
            startRoll(1);
        }
    }

    private void applyActiveDodgeMovement(boolean airborne) {
        if (!isDodging()) {
            return;
        }

        switch (dodgeType) {
            case SPOT -> vx *= airborne ? 0.88 : 0.48;
            case ROLL -> {
                if (airborne) {
                    clearActiveDodge();
                } else {
                    vx = dodgeDirection * Math.max(ROLL_DODGE_SPEED, type.speed * speedMultiplier * 1.18);
                }
            }
            case AIR -> {
                if (dodgeDirection == 0) {
                    vx *= 0.88;
                } else {
                    vx = dodgeDirection * AIR_DODGE_SPEED;
                }
                if (dodgeInvulnerabilityTimer > 0) {
                    vy = Math.min(vy * 0.82, AIR_DODGE_STALL_VELOCITY);
                }
            }
        }
    }

    private void updateShieldState(boolean stunned, boolean airborne, boolean blockHeld, boolean inDockWater, double gameSpeed) {
        boolean wantsShield = blockHeld && !airborne && !inDockWater;
        boolean justPressed = blockHeld && !blockHeldLastFrame;
        boolean canShield = wantsShield
                && !stunned
                && !(type == BirdGame3.BirdType.PIGEON && pigeonSpecialActive())
                && !(isRaptor() && raptorSpecialActive())
                && blockCooldown <= 0
                && shieldHealth > 0.0
                && !isDodging()
                && landingLagTimer <= 0
                && jumpSquatTimer <= 0
                && knockdownTimer <= 0;

        if (canShield) {
            if (!isBlocking && justPressed) {
                parryWindowFrames = SHIELD_PARRY_STARTUP_FRAMES;
            }
            isBlocking = true;
            if (shieldStunFrames > 0) {
                vx *= 0.82;
            } else {
                vx = 0.0;
            }
            vy *= 0.92;
        } else {
            if (isBlocking && !wantsShield) {
                blockCooldown = Math.max(blockCooldown, SHIELD_DROP_COOLDOWN_FRAMES);
            }
            isBlocking = false;
            if (!blockHeld) {
                parryWindowFrames = 0;
            }
        }

        if (isBlocking) {
            shieldHoldVisual = Math.min(1.0, shieldHoldVisual + SHIELD_HOLD_VISUAL_BUILD_PER_FRAME * gameSpeed);
        } else {
            shieldHoldVisual = Math.max(0.0, shieldHoldVisual - SHIELD_HOLD_VISUAL_RELEASE_PER_FRAME * gameSpeed);
        }
    }

    private void handleVineGrapple() {
        if (grappleTimer <= 0) {
            grappleUses = 0;
        }

        if (grappleUses > 0 && specialJustPressed() && !isOnGround() && !onVine && !isGrappling && specialCooldown <= 0) {
            GrappleVineAnchor anchor = findGrappleVineAnchor();
            if (anchor != null) {
                SwingingVine vine = new SwingingVine(anchor.anchorX(), anchor.anchorY(), anchor.length());
                vine.temporary = true;
                vine.ownerPlayerIndex = playerIndex;
                vine.angle = Math.clamp((bodyCenterX() - anchor.anchorX()) / Math.max(120.0, anchor.length()) + vx * 0.025, -0.55, 0.55);
                vine.angularVelocity = Math.clamp(vx * 0.010 + (facingRight ? 0.022 : -0.022), -0.085, 0.085);
                vine.updatePlatformPosition();
                game.swingingVines.add(vine);

                grappleTargetX = vine.gripX();
                grappleTargetY = vine.gripY();
                isGrappling = false;
                grappleUses--;
                specialCooldown = 34;
                attachToVine(vine);
                game.addToKillFeed(shortName() + " summoned a VINE SWING!");
                for (int i = 0; i < 30; i++) {
                    double progress = i / 29.0;
                    game.particles.add(new Particle(
                            anchor.anchorX() + (grappleTargetX - anchor.anchorX()) * progress,
                            anchor.anchorY() + (grappleTargetY - anchor.anchorY()) * progress,
                            0,
                            -0.4,
                            Color.FORESTGREEN.deriveColor(0, 1, 1, 0.78)
                    ));
                }
            }
        }

        if (isGrappling) {
            isGrappling = false;
            canDoubleJump = true;
        }
    }

    void attachToVine(SwingingVine vine) {
        if (vine == null) {
            return;
        }
        attachedVine = vine;
        onVine = true;
        vineRideFrames = 0;
        isGrappling = false;
        attackAnimationTimer = 0;
        clearAerialAttackState();
        landingLagTimer = 0;
        vx = 0;
        vy = 0;
        canDoubleJump = true;
        refreshAirDodge();
        syncToAttachedVine();
    }

    void syncToAttachedVine() {
        if (!onVine || attachedVine == null) {
            return;
        }
        x = attachedVine.gripX() - 40 * sizeMultiplier;
        y = attachedVine.gripY() - 78 * sizeMultiplier;
        vx = 0;
        vy = 0;
    }

    void dropFromVine() {
        onVine = false;
        attachedVine = null;
        vineRideFrames = 0;
        isGrappling = false;
        canDoubleJump = true;
    }

    void launchFromVine(boolean autoLaunch) {
        if (!onVine || attachedVine == null) {
            return;
        }
        SwingingVine vine = attachedVine;
        double launchVx = vine.tipVelocityX() * 1.12;
        double launchVy = vine.tipVelocityY() * 1.12 - (autoLaunch ? 6.4 : 5.2);
        if (Math.abs(launchVx) < 4.5) {
            double fallbackDirection = Math.abs(vine.angle) > 0.06 ? Math.signum(vine.angle) : (facingRight ? 1.0 : -1.0);
            launchVx = fallbackDirection * 4.5;
        }

        onVine = false;
        attachedVine = null;
        vineRideFrames = 0;
        vx = Math.clamp(launchVx, -22.0, 22.0);
        vy = Math.clamp(launchVy, -18.0, 10.0);
        if (Math.abs(vx) > 0.08) {
            facingRight = vx > 0;
        }
        canDoubleJump = true;
    }

    private GrappleVineAnchor findGrappleVineAnchor() {
        double centerX = bodyCenterX();
        double centerY = bodyCenterY();
        GrappleVineAnchor best = null;
        double bestScore = Double.MAX_VALUE;

        for (Platform p : game.platforms) {
            double undersideY = p.y + p.h;
            double verticalGap = centerY - undersideY;
            if (verticalGap < 110 || verticalGap > 620) {
                continue;
            }
            double anchorX = Math.clamp(centerX, p.x + 36, p.x + p.w - 36);
            double horizontalGap = Math.abs(anchorX - centerX);
            if (horizontalGap > 250) {
                continue;
            }
            double score = verticalGap * 1.25 + horizontalGap * 1.8;
            if (score < bestScore) {
                bestScore = score;
                best = new GrappleVineAnchor(anchorX, undersideY, Math.clamp(verticalGap - 28.0, 120.0, 420.0));
            }
        }
        return best;
    }

    private record GrappleVineAnchor(double anchorX, double anchorY, double length) {
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
        if (speedTimer == 0 && speedBoostTimer <= 0) {
            speedMultiplier = baseSpeedMultiplier;
        }
        if (roadrunnerSandstormActive()) {
            speedMultiplier = Math.max(speedMultiplier, baseSpeedMultiplier * ROADRUNNER_SANDSTORM_SPEED_SCALE);
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
            game.recordLeanFrame(this);
        } else if (leanTimer > 0) {
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
            if (y < BirdGame3.GROUND_Y - 800) {
                powerMultiplier = Math.max(powerMultiplier, 1.3);
                speedMultiplier = Math.max(speedMultiplier, 1.2);
                if (Math.random() < 0.3) {
                    game.particles.add(new Particle(x + 40 + (Math.random() - 0.5) * 60,
                            y + 80, (Math.random() - 0.5) * 6, 2 + Math.random() * 4,
                            Color.GOLD.deriveColor(0, 1, 1, 0.7)));
                }
            } else if (y < BirdGame3.GROUND_Y - 400) {
                powerMultiplier = Math.max(powerMultiplier, 1.1);
            }
        } else if (type == BirdGame3.BirdType.FALCON && airborne) {
            if (y < BirdGame3.GROUND_Y - 700) {
                powerMultiplier = Math.max(powerMultiplier, 1.22);
                speedMultiplier = Math.max(speedMultiplier, 1.26);
                if (Math.random() < 0.28) {
                    game.particles.add(new Particle(x + 40 + (Math.random() - 0.5) * 52,
                            y + 80, (Math.random() - 0.5) * 6, 2 + Math.random() * 4,
                            Color.web("#FFCC80").deriveColor(0, 1, 1, 0.75)));
                }
            } else if (y < BirdGame3.GROUND_Y - 340) {
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
                handleTitmouseZipImpact();
                isZipping = false;
            }
            vx = vy = 0;
        }
    }

    private void handleTitmouseZipImpact() {
        for (Bird other : game.players) {
            if (!canDamageTarget(other)) continue;
            double dist = combatDistanceTo(other);
            if (dist < 120 + other.combatRadius()) {
                int dmg = (int) (20 * powerMultiplier);
                double oldHealth = other.health;
                double dealt = applyDamageTo(other, dmg);
                game.damageDealt[playerIndex] += (int) dealt;
                game.recordSpecialImpact(playerIndex, (int) dealt, dealt > 0);
                if (other.health <= 0 && oldHealth > 0) game.eliminations[playerIndex]++;
        game.playZombieFallSfx();

                other.vx += (other.bodyCenterX() > bodyCenterX() ? 1 : -1) * 25;
                other.vy -= 18;

                game.addToKillFeed(shortName() + " ZAPPED " + other.shortName() + "! -" + dmg + " HP");

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
                releaseBatHang();
                return false;
            }

            double leftBound = batHangPlatform.x + 10;
            double rightBound = batHangPlatform.x + batHangPlatform.w - (80 * sizeMultiplier) - 10;
            x = Math.clamp(x, leftBound, rightBound);
            y = batHangPlatform.y + batHangPlatform.h + 2;
            vy = 0;

            double hangSpeed = type.speed * speedMultiplier * 0.72;
            if (leftPressed()) {
                vx = -hangSpeed;
            } else if (rightPressed()) {
                vx = hangSpeed;
            } else {
                vx *= 0.55;
            }
            x += vx;
            if (Math.abs(vx) > 0.05) facingRight = vx > 0;

            if (jumpPressed()) {
                if (batHangLockTimer <= 0) {
                    releaseBatHang();
                    vy = 2;
                    return false;
                }
            }

            boolean attackLocked = handleAttackInput(true);
            if (attackLocked) {
                vx *= 0.42;
            }
            if (!attackLocked && specialJustPressed()
                    && (isRaptor() ? canStartRaptorSpecial() : specialCooldown <= 0)
                    && !isBlocking) {
                special();
            }
            return true;
        }

        if (!stunned && batRehangCooldownTimer <= 0 && !isOnGround() && vy < -2 && jumpPressed()) {
            Platform hangable = findBatHangablePlatform();
            if (hangable != null) {
                batHanging = true;
                batHangPlatform = hangable;
                batHangLockTimer = 14; // prevents immediate unlatch from the same jump press
                batRehangCooldownTimer = 0;
                vx *= 0.35;
                vy = 0;
                attackAnimationTimer = 0;
                clearAerialAttackState();
                landingLagTimer = 0;
                y = hangable.y + hangable.h + 2;
                canDoubleJump = true;
                refreshAirDodge();
                return true;
            }
        }
        return false;
    }

    private void releaseBatHang() {
        batHanging = false;
        batHangPlatform = null;
        batHangLockTimer = 0;
        batRehangCooldownTimer = Math.max(batRehangCooldownTimer, Bird.BAT_REHANG_COOLDOWN_FRAMES);
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

    private void handleHorizontalMovement(boolean stunned, boolean airborne,
                                          boolean jumpHeld, boolean jumpJustPressed,
                                          double gameSpeed) {
        if (!stunned) {
            if (dashCooldown > 0) dashCooldown--;
            if (dashTimer > 0) dashTimer--;
            if (isDodging()) {
                applyActiveDodgeMovement(airborne);
                return;
            }
            if (!airborne && knockdownTimer > 0) {
                vx *= 0.72;
                return;
            }
            if (!airborne && landingLagTimer > 0) {
                vx *= 0.76;
                return;
            }
            if ((type == BirdGame3.BirdType.PIGEON && pigeonSpecialActive())
                    || (isRaptor() && raptorSpecialActive())) {
                vx *= airborne ? 0.96 : 0.82;
                return;
            }

            double targetVx = 0;
            double airFric = airborne ? 0.90 : 0.75;
            double accel = airborne ? 0.20 : 0.45;
            double moveSpeed = type.speed * speedMultiplier;
            double speedRatio = baseSpeedMultiplier > 0 ? speedMultiplier / baseSpeedMultiplier : 1.0;
            if (airborne) {
                accel *= Math.clamp(1.0 + (speedRatio - 1.0) * 0.85, 0.85, 1.55);
            } else {
                accel *= Math.clamp(1.0 + (speedRatio - 1.0) * 0.65, 0.9, 1.45);
            }
            if (type == BirdGame3.BirdType.ROADRUNNER) {
                boolean sandstorm = roadrunnerSandstormActive();
                if (airborne) {
                    moveSpeed *= sandstorm ? 1.18 : 0.88;
                    airFric = sandstorm ? 0.94 : 0.91;
                    accel = sandstorm ? 0.26 : 0.17;
                } else {
                    moveSpeed *= sandstorm ? 1.46 : 1.32;
                    airFric = sandstorm ? 0.74 : 0.70;
                    accel = sandstorm ? 0.72 : 0.62;
                }
            }
            if (type == BirdGame3.BirdType.BAT) {
                moveSpeed *= airborne ? 1.48 : 0.62;
                airFric = airborne ? 0.93 : 0.70;
                accel = airborne ? 0.28 : 0.34;
            }

            boolean leftPressed = leftPressed();
            boolean rightPressed = rightPressed();
            boolean shielding = isBlocking;
            boolean shieldLocked = shielding && shieldStunFrames > 0;

            if (leftPressed) {
                targetVx = shielding ? 0.0 : -moveSpeed;
                if (!shielding && type == BirdGame3.BirdType.HUMMINGBIRD && jumpPressed() && airborne) {
                    targetVx *= 1.75;
                }
            }
            else if (rightPressed) {
                targetVx = shielding ? 0.0 : moveSpeed;
                if (!shielding && type == BirdGame3.BirdType.HUMMINGBIRD && jumpPressed() && airborne) {
                    targetVx *= 1.75;
                }
            }
            if (shieldLocked) {
                targetVx = 0.0;
            }

            vx = vx * airFric + targetVx * accel;
            if (dashTimer > 0 && !airborne && !shielding) {
                double dashSpeed = moveSpeed * 2.8;
                vx = (lastTapDir < 0 ? -dashSpeed : dashSpeed);
            }
            if (Math.abs(vx) > 0.1) facingRight = vx > 0;

            boolean attackLocked = false;
            boolean grabLocked = false;
            boolean jumpSquatting = jumpSquatTimer > 0;
            if (!shielding && !jumpSquatting) {
                grabLocked = handleGrabInput(airborne);
                if (!grabLocked) {
                    attackLocked = handleAttackInput(!airborne);
                }
            }
            if ((attackLocked || grabLocked) && !airborne) {
                vx *= 0.38;
            }

            boolean canJump = !attackLocked
                    && !grabLocked
                    && shieldStunFrames <= 0
                    && (isOnGround() || (type == BirdGame3.BirdType.PIGEON && canDoubleJump));
            if (jumpSquatting) {
                advanceGroundJumpSquat(jumpHeld, gameSpeed);
                jumpSquatting = jumpSquatTimer > 0;
            } else if (jumpJustPressed && canJump) {
                if (isOnGround()) {
                    startGroundJumpSquat();
                    advanceGroundJumpSquat(jumpHeld, gameSpeed);
                    jumpSquatting = jumpSquatTimer > 0;
                } else {
                    vy = -type.jumpHeight * 0.75;
                    if (type == BirdGame3.BirdType.PIGEON) canDoubleJump = false;
                    game.playSwingSfx();
                    recordJumpHeightAchievements();
                }
            }

            boolean canSpecialFromShield = type == BirdGame3.BirdType.PIGEON
                    ? canConvertShieldIntoPigeonDownSpecial(selectPigeonSpecialVariant())
                    : (isRaptor() && canConvertShieldIntoRaptorDownSpecial(selectRaptorSpecialVariant()));
            boolean canStartSelectedSpecial = type == BirdGame3.BirdType.PIGEON
                    ? canStartPigeonSpecial()
                    : (isRaptor() ? canStartRaptorSpecial() : specialCooldown <= 0);
            if (!attackLocked && !grabLocked && (!shielding || canSpecialFromShield) && !jumpSquatting && specialJustPressed()) {
                if (grappleUses == 0 && canStartSelectedSpecial) {
                    special();
                } else if (!game.isAI[playerIndex]
                        && ((isRaptor() && raptorSpecialOnReuseLockout(selectRaptorSpecialVariant()))
                        || (specialCooldown > 0 && type != BirdGame3.BirdType.PIGEON))) {
                    cooldownFlash = 15;
                }
            }
        } else {
            vx *= 0.92;
        }
    }

    private void applyDockWaterPhysics(boolean stunned, boolean downHeld, double gameSpeed) {
        if (!isInDockWater()) {
            return;
        }

        canDoubleJump = true;
        vx *= Math.pow(DOCK_WATER_SWIM_DRAG_X, gameSpeed);
        vy *= Math.pow(DOCK_WATER_SWIM_DRAG_Y, gameSpeed);
        double surfaceGap = bodyCenterY() - game.dockWaterSurfaceY();

        if (stunned) {
            vy -= DOCK_WATER_BUOYANCY * 0.32 * gameSpeed;
        } else {
            if (jumpPressed()) {
                if (surfaceGap <= DOCK_WATER_SURFACE_BREACH_WINDOW) {
                    vy = Math.min(vy - DOCK_WATER_RISE_ACCEL * 0.45 * gameSpeed,
                            -Math.max(DOCK_WATER_SURFACE_BREACH_BOOST, type.jumpHeight * 0.88));
                    limitedFlightFuel = LIMITED_FLIGHT_MAX;
                    canDoubleJump = true;
                } else {
                    vy -= DOCK_WATER_RISE_ACCEL * gameSpeed;
                }
            } else {
                vy -= DOCK_WATER_BUOYANCY * gameSpeed;
            }
            if (downHeld) {
                vy += DOCK_WATER_DIVE_ACCEL * gameSpeed;
            }
        }

        if ((isFullySubmergedInDockWater() || surfaceGap <= DOCK_WATER_SURFACE_BREACH_WINDOW + 18.0)
                && jumpPressed() && vy < -5.6) {
            limitedFlightFuel = LIMITED_FLIGHT_MAX;
        }

        vy = Math.clamp(vy, DOCK_WATER_MAX_RISE, DOCK_WATER_MAX_SINK);
    }

    private void respawnAfterStageLoss(boolean trainingDummy, boolean islandBounds, double leftBound, double rightBound,
                                       double fallbackX, double fallbackY) {
        if (game.usesSmashCombatRules()) {
            if (!game.playerHasStocksRemaining(playerIndex)) {
                retireFromStockMatch();
                return;
            }
            if (islandBounds) {
                double centerX = game.battlefieldSpawnCenterX();
                resetForSmashRespawn(centerX - 40 * sizeMultiplier, game.battlefieldSpawnY(sizeMultiplier), 0.0);
            } else {
                resetForSmashRespawn(fallbackX, fallbackY, 0.0);
            }
            return;
        }
        if (trainingDummy) {
            health = STARTING_HEALTH;
        }
        boolean reborn = false;
        if (!trainingDummy && health <= 0) {
            reborn = tryPhoenixRebirth();
            if (!reborn) {
                onDefeated();
            }
        }
        if (!reborn) game.playZombieFallSfx();
        if (!reborn && health <= 0) {
            x = Math.clamp(x, leftBound, rightBound);
            y = BirdGame3.WORLD_HEIGHT + 400;
        } else if (islandBounds) {
            double centerX = game.battlefieldSpawnCenterX();
            x = centerX - 40 * sizeMultiplier;
            y = game.battlefieldSpawnY(sizeMultiplier);
        } else {
            x = fallbackX;
            y = fallbackY;
        }
        vx = 0;
        vy = 0;
        canDoubleJump = true;
    }

    void resetForSmashRespawn(double spawnX, double spawnY, double damagePercent) {
        onDefeated();
        health = STARTING_HEALTH;
        smashDamage = Math.max(0.0, damagePercent);
        x = spawnX;
        y = spawnY;
        vx = 0;
        vy = 0;
        canDoubleJump = true;
        recentSmashAttackerIndex = -1;
        recentSmashAttackerFrames = 0;
        pendingSmashLaunchScale = 1.0;
    }

    void retireFromStockMatch() {
        onDefeated();
        health = 0;
        x = -2400 - playerIndex * 240.0;
        y = BirdGame3.WORLD_HEIGHT + 1200;
        vx = 0;
        vy = 0;
        canDoubleJump = false;
    }

    private void handleEagleDiveImpact() {
        if (type != BirdGame3.BirdType.EAGLE && type != BirdGame3.BirdType.FALCON) return;
        if (!eagleDiveActive) return;
        if (eagleDiveCountdown > 0 && !eagleAscentActive) return;

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
        if (isCombatInvulnerable()) return 0.0;
        if (titanActive && titanTimer > 0) mult *= 0.75;
        if (shrinkTimer > 0) mult *= 1.22;
        return mult;
    }

    private void heal(double amount) {
        if (amount <= 0) return;
        if (health <= 0) return;
        if (game.usesSmashCombatRules()) {
            smashDamage = Math.max(0.0, smashDamage - amount);
            return;
        }
        double maxHealth = getMaxHealth();
        if (health >= maxHealth) return;
        health = Math.min(maxHealth, health + amount);
    }

    public double getUltimateRatio() {
        return Math.clamp(ultimateMeter / ULTIMATE_MAX, 0.0, 1.0);
    }

    public boolean isUltimateReady() {
        return ultimateMeter >= ULTIMATE_MAX;
    }

    private void gainUltimate(double amount) {
        if (amount <= 0) return;
        ultimateMeter = Math.min(ULTIMATE_MAX, ultimateMeter + amount);
    }

    void gainUltimateFromMinionDamage(double dealtDamage) {
        if (dealtDamage <= 0) return;
        gainUltimate(dealtDamage * ULTIMATE_GAIN_DEALT);
    }

    private boolean consumeUltimate() {
        if (!isUltimateReady()) return false;
        ultimateMeter = 0.0;
        ultimateFxTimer = ULTIMATE_FX_FRAMES;
        return true;
    }

    private double outgoingDamageMultiplier() {
        double mult = 1.0;
        if (type == BirdGame3.BirdType.PHOENIX && phoenixRebornActive) mult *= PHOENIX_REBORN_DAMAGE_SCALE;
        return mult;
    }

    public double getMaxHealth() {
        if (type == BirdGame3.BirdType.PHOENIX && phoenixRebornActive) return PHOENIX_REBORN_HEALTH;
        if (isNullRockForm()) return game.nullRockTrueFormHealth();
        return 100.0;
    }

    void refillTrainingResources(boolean fillUltimate) {
        onDefeated();
        baseSizeMultiplier = type == BirdGame3.BirdType.PELICAN ? 1.2 : 1.0;
        basePowerMultiplier = 1.0;
        baseSpeedMultiplier = 1.0;
        phoenixRebornUsed = false;
        phoenixRebornActive = false;
        health = STARTING_HEALTH;
        resetSmashCombatState();
        vx = 0;
        vy = 0;
        stunTime = 0;
        attackCooldown = 0;
        attackAnimationTimer = 0;
        clearAerialAttackState();
        landingLagTimer = 0;
        techBufferTimer = 0;
        knockdownTimer = 0;
        specialCooldown = 0;
        specialMaxCooldown = 0;
        cooldownFlash = 0;
        canDoubleJump = true;
        speedTimer = 0;
        hoverRegenTimer = 0;
        hoverRegenMultiplier = 1.0;
        roadrunnerSandstormTimer = 0;
        roadrunnerSandGustTimer = 0;
        Arrays.fill(roadrunnerSandHitCooldown, 0);
        shrinkTimer = 0;
        speedMultiplier = baseSpeedMultiplier;
        powerMultiplier = basePowerMultiplier;
        sizeMultiplier = baseSizeMultiplier;
        ultimateFxTimer = 0;
        if (fillUltimate) {
            ultimateMeter = ULTIMATE_MAX;
        }
    }

    void setTrailerAttackChargeRatio(double ratio) {
        double clamped = Math.clamp(ratio, 0.0, 1.0);
        attackChargeFrames = clamped <= 0.0 ? 0 : Math.max(1, (int) Math.round(clamped * MAX_ATTACK_CHARGE_FRAMES));
    }

    void setTrailerShieldPreview(boolean blocking, double durabilityRatio, int parryFrames) {
        double clampedDurability = Math.clamp(durabilityRatio, 0.0, 1.0);
        shieldHealth = SHIELD_MAX_HEALTH * clampedDurability;
        isBlocking = blocking && shieldHealth > 0.0;
        parryWindowFrames = isBlocking ? Math.max(0, parryFrames) : 0;
        shieldStunFrames = 0;
        shieldHoldVisual = isBlocking ? Math.max(shieldHoldVisual, 0.92) : 0.0;
        if (!isBlocking) {
            blockCooldown = 0;
        }
    }

    void setTrailerSmashDamagePercent(double percent) {
        smashDamage = Math.max(0.0, percent);
    }

    void advanceTrailerPresentationFrame() {
        if (attackChargeFrames > 0) {
            emitAttackChargeParticles();
        }
        if (isBlocking) {
            shieldHoldVisual = Math.min(1.0, shieldHoldVisual + SHIELD_HOLD_VISUAL_BUILD_PER_FRAME);
        } else {
            shieldHoldVisual = Math.max(0.0, shieldHoldVisual - SHIELD_HOLD_VISUAL_RELEASE_PER_FRAME);
        }
        if (parryWindowFrames > 0) {
            parryWindowFrames--;
        }
        if (shieldStunFrames > 0) {
            shieldStunFrames--;
        }
        if (cooldownFlash > 0) {
            cooldownFlash--;
        }
        if (stunTime > 0) {
            stunTime--;
        }
        if (attackAnimationTimer > 0) {
            attackAnimationTimer--;
        }
        if (ultimateFxTimer > 0) {
            ultimateFxTimer--;
        }
        if (roadrunnerSandstormTimer > 0) {
            handleRoadrunnerSandstorm();
            roadrunnerSandstormTimer--;
        }
        if (roadrunnerSandGustTimer > 0) {
            roadrunnerSandGustTimer--;
        }
        for (int i = 0; i < roadrunnerSandHitCooldown.length; i++) {
            if (roadrunnerSandHitCooldown[i] > 0) {
                roadrunnerSandHitCooldown[i]--;
            }
        }
    }

    boolean applyTrainingRecoveryInputs() {
        return applyAIVoidRecoveryInputs(isOnGround(), findCurrentSupportPlatform());
    }

    double debugCombatLeft() {
        return bodyCenterX() - combatHalfWidth();
    }

    double debugCombatTop() {
        return bodyCenterY() - combatHalfHeight();
    }

    double debugCombatWidth() {
        return combatHalfWidth() * 2.0;
    }

    double debugCombatHeight() {
        return combatHalfHeight() * 2.0;
    }

    boolean debugAttackBoxActive() {
        return attackAnimationTimer > 0;
    }

    double debugAttackBoxLeft() {
        return debugAttackCenterX() - debugAttackHalfWidth();
    }

    double debugAttackBoxTop() {
        return debugAttackCenterY() - debugAttackHalfHeight();
    }

    double debugAttackBoxWidth() {
        return debugAttackHalfWidth() * 2.0;
    }

    double debugAttackBoxHeight() {
        return debugAttackHalfHeight() * 2.0;
    }

    private double debugAttackCenterX() {
        double centerX = bodyCenterX();
        if (isNullRockForm()) {
            centerX += (facingRight ? 1.0 : -1.0) * combatHalfWidth() * 0.88;
        }
        return centerX;
    }

    private double debugAttackCenterY() {
        double centerY = bodyCenterY();
        if (isNullRockForm()) {
            centerY -= combatHalfHeight() * 0.08;
        }
        return centerY;
    }

    private double debugAttackHalfWidth() {
        double range = 120 * sizeMultiplier;
        if (isNullRockForm()) {
            range *= 0.86;
        }
        return range;
    }

    private double debugAttackHalfHeight() {
        double verticalRange = 100 * sizeMultiplier;
        if (isNullRockForm()) {
            verticalRange *= 0.88;
        }
        return verticalRange;
    }

    private double applyScaledDamageTo(Bird target, double scaledDamage) {
        if (target == null || scaledDamage <= 0 || target.health <= 0) return 0;
        double dealtDamage = target.receiveScaledDamage(scaledDamage);
        if (dealtDamage > 0) {
            if (game.usesSmashCombatRules()) {
                target.registerSmashHit(this, dealtDamage);
            }
            gainUltimate(dealtDamage * ULTIMATE_GAIN_DEALT);
            target.gainUltimate(dealtDamage * ULTIMATE_GAIN_TAKEN);
            game.recordTrainingHit(this, target, dealtDamage);
        }
        return dealtDamage;
    }

    private double applyUnshieldedDamageTo(Bird target, double rawDamage) {
        return applyScaledDamageTo(target, scaledDamageAgainst(target, rawDamage));
    }

    private double applyDamageTo(Bird target, double rawDamage) {
        if (target == null || rawDamage <= 0 || target.health <= 0) return 0;
        double scaledDamage = scaledDamageAgainst(target, rawDamage);
        ShieldHitResult shieldHit = target.resolveShieldHit(this, scaledDamage, 0.0);
        if (shieldHit.blocked()) {
            return 0;
        }
        return applyScaledDamageTo(target, scaledDamage);
    }

    double receiveExternalDamage(double rawDamage) {
        if (rawDamage <= 0) return 0;
        if (isCombatInvulnerable()) {
            spawnNullRockShieldBurst();
            return 0;
        }
        double scaledDamage = rawDamage * incomingDamageMultiplier();
        ShieldHitResult shieldHit = resolveShieldHit(null, scaledDamage, 0.0);
        if (shieldHit.blocked()) {
            return 0;
        }
        return receiveScaledDamage(scaledDamage);
    }

    private void interruptLedgeHangOnHit() {
        if (!ledgeHanging) {
            return;
        }
        clearLedgeHangState(LEDGE_REGRAB_COOLDOWN_FRAMES);
        y += Math.max(8.0, bodyHeight() * 0.14);
    }

    private double receiveScaledDamage(double scaledDamage) {
        if (scaledDamage <= 0 || health <= 0) return 0;
        if (isCombatInvulnerable()) {
            spawnNullRockShieldBurst();
            return 0;
        }
        interruptGrabStateOnHit();
        interruptLedgeHangOnHit();
        interruptPigeonSpecialStateOnHit();
        interruptRaptorSpecialStateOnHit();
        if (game.usesSmashCombatRules()) {
            smashDamage += scaledDamage;
            return scaledDamage;
        }

        double oldHealth = health;
        if (game.isTrainingDummy(this)) {
            health = STARTING_HEALTH;
            return Math.max(0, scaledDamage);
        }

        double gatedDamage = applyNullRockPhaseGate(scaledDamage);
        if (!Double.isNaN(gatedDamage)) {
            return gatedDamage;
        }

        health = Math.max(0, health - scaledDamage);
        if (health <= 0) {
            tryPhoenixRebirth();
            if (health <= 0) {
                onDefeated();
            }
        }
        return oldHealth - health;
    }

    private double applyNullRockPhaseGate(double scaledDamage) {
        if (!isNullRockForm()) return Double.NaN;
        while (nullRockPhaseIndex < NULL_ROCK_PHASE_THRESHOLDS.length) {
            double thresholdHealth = Math.max(1.0, getMaxHealth() * NULL_ROCK_PHASE_THRESHOLDS[nullRockPhaseIndex]);
            if (health <= thresholdHealth + 0.0001) {
                nullRockPhaseIndex++;
                continue;
            }
            double nextHealth = health - scaledDamage;
            if (nextHealth > thresholdHealth + 0.0001) {
                return Double.NaN;
            }
            double oldHealth = health;
            health = thresholdHealth;
            triggerNullRockPhaseShift();
            return oldHealth - health;
        }
        return Double.NaN;
    }

    private void triggerNullRockPhaseShift() {
        nullRockPhaseIndex++;
        nullRockInvincibilityTimer = NULL_ROCK_PHASE_INVULN_FRAMES + (nullRockPhaseIndex - 1) * 18;
        nullRockShieldFxCooldown = 0;
        stunTime = 0;
        shrinkTimer = 0;
        carrionSwarmTimer = Math.max(carrionSwarmTimer, 170 + nullRockPhaseIndex * 20);
        specialCooldown = Math.min(specialCooldown, 90);
        vx *= 0.35;
        vy = Math.min(vy, -5.5);
        game.onNullRockPhaseShift(this, nullRockPhaseIndex - 1);
        if (!trueNullRockForm && nullRockPhaseIndex == 3) {
            triggerTrueNullRockAscension();
        }
    }

    private void triggerTrueNullRockAscension() {
        if (!isNullRockForm() || trueNullRockForm) return;
        trueNullRockForm = true;
        double ascendedSize = baseSizeMultiplier * NULL_ROCK_TRUE_FORM_SIZE_SCALE;
        double ascendedPower = basePowerMultiplier * NULL_ROCK_TRUE_FORM_POWER_SCALE;
        double ascendedSpeed = baseSpeedMultiplier * NULL_ROCK_TRUE_FORM_SPEED_SCALE;
        setBaseMultipliers(ascendedSize, ascendedPower, ascendedSpeed);
        nullRockInvincibilityTimer = Math.max(nullRockInvincibilityTimer, TRUE_NULL_ROCK_ASCENSION_INVULN_FRAMES);
        specialCooldown = 0;
        crowSwarmCooldown = 0;
        carrionSwarmTimer = Math.max(carrionSwarmTimer, 320);
        stunTime = 0;
        shrinkTimer = 0;
        if (name != null) {
            String replaced = name.replace("The Null Rock", "True Null Rock")
                    .replace("NULL ROCK", "TRUE NULL ROCK");
            if (replaced.equals(name) && name.contains(":")) {
                String prefix = name.substring(0, name.indexOf(':') + 1);
                replaced = prefix + " True Null Rock";
            }
            name = replaced;
        }
        game.onTrueNullRockAscension(this);
    }

    private void spawnNullRockShieldBurst() {
        if (!isNullRockForm()) return;
        if (nullRockShieldFxCooldown > 0) return;
        nullRockShieldFxCooldown = 8;
        double centerX = x + 40 * sizeMultiplier;
        double centerY = y + 40 * sizeMultiplier;
        for (int i = 0; i < 14; i++) {
            double angle = Math.random() * Math.PI * 2;
            double speed = 3 + Math.random() * 8;
            Color c = i % 2 == 0 ? Color.web("#FFCDD2") : Color.web("#80DEEA");
            game.particles.add(new Particle(
                    centerX,
                    centerY,
                    Math.cos(angle) * speed,
                    Math.sin(angle) * speed - 2.5,
                    c.deriveColor(0, 1, 1, 0.82)
            ));
        }
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

        String who = shortName();
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

    private int scaledAttackCooldown(int baseCooldownFrames) {
        if (overchargeAttackTimer <= 0) return baseCooldownFrames;
        return Math.max(10, (int) Math.round(baseCooldownFrames * 0.62));
    }

    private void handleHummingbirdFrenzy() {
        if (type != BirdGame3.BirdType.HUMMINGBIRD || hummingFrenzyTimer <= 0) return;

        double centerX = bodyCenterX();
        double centerY = bodyCenterY();

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

            double dx = other.bodyCenterX() - centerX;
            double dy = other.bodyCenterY() - centerY;
            if (Math.abs(dx) > 118 + other.combatHalfWidth() || Math.abs(dy) > 105 + other.combatHalfHeight()) continue;

            int dmg = 2 + random.nextInt(2);
            double oldHealth = other.health;
            int dealt = (int) applyDamageTo(other, dmg);
            if (dealt <= 0) continue;

            game.damageDealt[playerIndex] += dealt;
            game.recordSpecialImpact(playerIndex, dealt, true);
            if (other.health <= 0 && oldHealth > 0) game.eliminations[playerIndex]++;

            other.vx += (dx >= 0 ? 1 : -1) * 4.2;
            other.vy -= 3.5;
            heal(dealt * 0.35);
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

        double centerX = bodyCenterX();
        double centerY = bodyCenterY();

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

            double dx = other.bodyCenterX() - centerX;
            double dy = other.bodyCenterY() - centerY;
            double dist = Math.hypot(dx, dy);
            if (dist > 185 + other.combatRadius()) continue;

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

    private void emitRoadrunnerDust() {
        if (type != BirdGame3.BirdType.ROADRUNNER || !isOnGround() || Math.abs(vx) < 6.4) return;
        for (int i = 0; i < 2; i++) {
            double dir = Math.signum(vx == 0 ? (facingRight ? 1 : -1) : vx);
            Color c = Math.random() < 0.6 ? Color.web("#D9A04D") : Color.web("#E2C388");
            game.particles.add(new Particle(
                    x + 34 - dir * (12 + Math.random() * 18),
                    y + 74 + (Math.random() - 0.5) * 10,
                    -dir * (1.2 + Math.random() * 2.0),
                    -1.8 - Math.random() * 1.6,
                    c.deriveColor(0, 1, 1, 0.64)
            ));
        }
    }

    private void applyPenguinDashDamage() {
        if (type != BirdGame3.BirdType.PENGUIN || penguinDashDamageTimer <= 0) return;
        for (Bird other : game.players) {
            if (!canDamageTarget(other)) continue;
            if (other.playerIndex < 0 || other.playerIndex >= penguinDashHit.length) continue;
            if (penguinDashHit[other.playerIndex]) continue;

            double dx = other.bodyCenterX() - bodyCenterX();
            double dy = other.bodyCenterY() - bodyCenterY();
            if (Math.abs(dx) > 90 + other.combatHalfWidth() || Math.abs(dy) > 95 + other.combatHalfHeight()) continue;

            int dmg = 10 + random.nextInt(5);
            double oldHealth = other.health;
            int dealt = (int) applyDamageTo(other, dmg);
            game.damageDealt[playerIndex] += dealt;
            game.recordSpecialImpact(playerIndex, dealt, dealt > 0);
            if (other.health <= 0 && oldHealth > 0) game.eliminations[playerIndex]++;

            other.vx += (dx >= 0 ? 1 : -1) * 11;
            other.vy -= 9;
            penguinDashHit[other.playerIndex] = true;
            game.addToKillFeed(shortName() + " ICE-CHECKED " + other.shortName() + "! -" + dmg + " HP");

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

            double dx = other.bodyCenterX() - bodyCenterX();
            double dy = other.bodyCenterY() - bodyCenterY();
            if (Math.abs(dx) < 120 + other.combatHalfWidth() && Math.abs(dy) < 115 + other.combatHalfHeight()) {
                int dmg = 9 + random.nextInt(6);
                double oldHealth = other.health;
                int dealt = (int) applyDamageTo(other, dmg);
                game.damageDealt[playerIndex] += dealt;
                game.recordSpecialImpact(playerIndex, dealt, dealt > 0);
                if (other.health <= 0 && oldHealth > 0) game.eliminations[playerIndex]++;

                other.vx += (dx >= 0 ? 1 : -1) * 8;
                other.vy -= 9;
                eagleAscentHit[other.playerIndex] = true;

                game.addToKillFeed(shortName() + " ASCENT-SLASHED " +
                        other.shortName() + "! -" + dmg + " HP");

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

            double dx = other.bodyCenterX() - bodyCenterX();
            double dy = other.bodyCenterY() - bodyCenterY();
            if (facingRight && dx < -12) continue;
            if (!facingRight && dx > 12) continue;
            if (Math.abs(dx) > 95 + other.combatHalfWidth() || Math.abs(dy) > 80 + other.combatHalfHeight()) continue;

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

            String attacker = shortName();
            String victim = other.shortName();
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
        game.addToKillFeed("KABOOM! " + shortName() + " slams the ground!");
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
                game.particles.add(new Particle(x + 40 + offset + j * 10, BirdGame3.GROUND_Y + j * 10,
                        (Math.random() - 0.5) * 14, -4 - Math.random() * 9, Color.SADDLEBROWN.darker()));
            }
        }

        for (Bird other : game.players) {
            if (!canDamageTarget(other)) continue;

            double dx = other.bodyCenterX() - bodyCenterX();
            double dy = other.bodyCenterY() - (y + 70);
            double dist = Math.hypot(dx, dy);

            if (dist < 300 + other.combatRadius()) {
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
                game.addToKillFeed(shortName() + " " + intensity + " " + other.shortName() + "! -" + dmg + " HP");

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
        game.addToKillFeed(shortName() + " lands a precision strike!");
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

            double dx = other.bodyCenterX() - bodyCenterX();
            double dy = other.bodyCenterY() - (y + 70);
            double dist = Math.hypot(dx, dy);
            if (dist > 230 + other.combatRadius()) continue;

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

            String victim = other.shortName();
            if (sweetspot) {
                game.addToKillFeed(shortName() + " SWEETSPOT DOVE " + victim + "! -" + dealt + " HP");
                game.triggerFlash(0.7, other.health <= 0 && oldHealth > 0);
            } else {
                game.addToKillFeed(shortName() + " tagged " + victim + " on impact! -" + dealt + " HP");
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

            double dx = other.bodyCenterX() - bodyCenterX();
            double dy = other.bodyCenterY() - bodyCenterY();
            double dist = Math.hypot(dx, dy);
            if (dist > 85 + other.combatRadius()) continue;

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

            game.addToKillFeed(shortName() + " PIERCED " +
                    other.shortName() + "! -" + dealt + " HP");

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

    private void handleThermals(boolean downHeld, double prevX, double prevY) {
        if (game.selectedMap == MapType.SKYCLIFFS || game.selectedMap == MapType.VIBRANT_JUNGLE || game.selectedMap == MapType.CAVE) {
            for (WindVent v : game.windVents) {
                if (v.cooldown > 0) continue;
                if (isInsideWindVent(v, x, y) || isInsideWindVent(v, prevX, prevY)) {
                    if (downHeld) {
                        vy *= DOWN_WIND_DAMPING;
                    } else {
                        vy = Math.min(vy, BirdGame3.WIND_FORCE);
                    }
                    if (Math.random() < 0.3) {
                        game.particles.add(new Particle(x + 40 + (Math.random() - 0.5) * 40, y + 80,
                                (Math.random() - 0.5) * 8, -4 - Math.random() * 8, Color.CYAN.deriveColor(0, 1, 1, 0.7)));
                    }
                    break;
                }
            }
        }
    }

    private boolean isInWindVent(double px, double py) {
        if (game.selectedMap != MapType.SKYCLIFFS && game.selectedMap != MapType.VIBRANT_JUNGLE && game.selectedMap != MapType.CAVE) {
            return false;
        }
        for (WindVent v : game.windVents) {
            if (v.cooldown > 0) continue;
            if (isInsideWindVent(v, px, py)) return true;
        }
        return false;
    }

    private boolean isInsideWindVent(WindVent v, double px, double py) {
        double centerX = v.x + v.w / 2;
        double centerY = v.y - 75;
        double dx = (px + 40) - centerX;
        double dy = (py + 40) - centerY;
        double normX = dx / (v.w / 2 + 50);
        double normY = dy / 200.0;
        return normX * normX + normY * normY <= 1.0;
    }

    private boolean applyCameraTopBoundaryPressure(double gameSpeed, boolean trainingDummy) {
        if (game.usesSmashCombatRules()) {
            return y < SMASH_TOP_BLAST_Y;
        }
        double topCameraLimit = 0.0;
        if (y >= topCameraLimit) return false;
        double overflow = topCameraLimit - y;
        double chipDamage = Math.min(0.85, 0.12 + overflow * 0.0032) * gameSpeed;
        health = Math.max(0.0, health - chipDamage);
        vy = Math.max(vy, 1.35 + Math.min(6.8, overflow * 0.055));
        if (type == BirdGame3.BirdType.VULTURE && overflow > 24.0) {
            isFlying = false;
        }
        if (trainingDummy && health <= 0) {
            health = STARTING_HEALTH;
        }
        return health <= 0;
    }

    private void handleBoundaries(double gameSpeed, boolean wasAirborne, double prevX, double prevY) {
        double leftBound = 50;
        double rightBound = BirdGame3.WORLD_WIDTH - 150 * sizeMultiplier;
        double outLeft = -300;
        double outRight = BirdGame3.WORLD_WIDTH + 300;
        boolean smashRules = game.usesSmashCombatRules();
        boolean islandBounds = usesIslandBounds();
        if (islandBounds) {
            double battlefieldLeft = game.battlefieldLeftBound();
            double battlefieldRight = game.battlefieldRightBound();
            leftBound = battlefieldLeft + 50;
            rightBound = battlefieldRight - 150 * sizeMultiplier;
            outLeft = battlefieldLeft - 300;
            outRight = battlefieldRight + 300;
        }

        if (!smashRules) {
            if (x < leftBound) x = leftBound;
            if (x > rightBound) x = rightBound;
        }

        boolean trainingDummy = game.isTrainingDummy(this);

        if (applyCameraTopBoundaryPressure(gameSpeed, trainingDummy)) {
            if (smashRules) {
                handleSmashBlastZoneKo(trainingDummy, islandBounds, leftBound, rightBound,
                        2000 + playerIndex * 600, BirdGame3.GROUND_Y - 400,
                        "off the top", false);
            }
            return;
        }

        handleWallTechCollision(prevX, prevY);

        if (x < outLeft || x > outRight) {
            if (smashRules) {
                handleSmashBlastZoneKo(trainingDummy, islandBounds, leftBound, rightBound,
                        2000 + playerIndex * 600, BirdGame3.GROUND_Y - 400,
                        x < outLeft ? "off the left side" : "off the right side", false);
                return;
            }
            health = Math.max(0, health - 50);
            if (health > 0 && !trainingDummy) {
                game.addToKillFeed(shortName() + " went out of bounds... -50 HP");
            }
            if (health <= 0 && !trainingDummy) {
                game.addToKillFeed(shortName() + " FLEW INTO THE VOID!");
            }
            respawnAfterStageLoss(trainingDummy, islandBounds, leftBound, rightBound,
                    2000 + playerIndex * 600, BirdGame3.GROUND_Y - 400);
        }

        if (!smashRules && y < BirdGame3.CEILING_Y) {
            y = BirdGame3.CEILING_Y;
            vy = Math.max(vy, 0);
            if (type == BirdGame3.BirdType.VULTURE) isFlying = false;
        }

        handleVerticalCollision(wasAirborne);

        if (game.selectedMap == MapType.DOCK && isDockDrownDepthReached()) {
            if (smashRules) {
                handleSmashBlastZoneKo(trainingDummy, true, leftBound, rightBound,
                        game.battlefieldSpawnCenterX(), game.battlefieldSpawnY(sizeMultiplier),
                        "in the harbor", true);
                return;
            }
            game.falls[playerIndex]++;
            health = 0;
            if (!trainingDummy) {
                game.addToKillFeed(shortName() + " DROWNED IN THE HARBOR!");
            }
            respawnAfterStageLoss(trainingDummy, true, leftBound, rightBound,
                    game.battlefieldSpawnCenterX(), game.battlefieldSpawnY(sizeMultiplier));
            return;
        }

        if (y > BirdGame3.WORLD_HEIGHT + 300) {
            if (smashRules) {
                handleSmashBlastZoneKo(trainingDummy, islandBounds, leftBound, rightBound,
                        1000 + playerIndex * 800, BirdGame3.GROUND_Y - 300,
                        isVoidMap() ? "into the lower blast zone" : "off the bottom", true);
                return;
            }
            game.falls[playerIndex]++;
            if (isVoidMap()) {
                health = 0;
            } else {
                health = Math.max(0, health - 50);
            }
            if (health > 0 && !trainingDummy) {
                game.addToKillFeed(shortName() + " fell... but survived! -50 HP");
            }
            if (health <= 0 && !trainingDummy) {
                String msg = isVoidMap()
                        ? shortName() + " FELL INTO THE VOID!"
                        : shortName() + " FELL TO THEIR DOOM!";
                game.addToKillFeed(msg);
            }
            respawnAfterStageLoss(trainingDummy, islandBounds, leftBound, rightBound,
                    1000 + playerIndex * 800, BirdGame3.GROUND_Y - 300);
            if (!game.trainingModeActive) {
                game.recordStageFallAchievement(playerIndex);
            }
        }
    }

    private void handleVultureFeast() {
        if (type == BirdGame3.BirdType.VULTURE && health > 0) {
            for (Bird b : game.players) {
                if (b != null && b != this && b.health <= 0 && b.y > BirdGame3.HEIGHT + 50 && b.y <= BirdGame3.HEIGHT + 100) {
                    heal(4);
                    game.addToKillFeed(shortName() + " FEASTS! +4 HP");
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
            if (overlapsPowerUp(p)) {
                handlePowerUpType(p, it);
            }
        }
    }

    private void triggerBroadsidePickup() {
        double centerX = bodyCenterX();
        double centerY = bodyCenterY();
        double heaviestHit = 0;
        boolean hitAnyone = false;

        specialCooldown = Math.max(0, specialCooldown - 180);
        overchargeAttackTimer = Math.max(overchargeAttackTimer, 210);
        game.addToKillFeed(shortName() + " fired a BROADSIDE!");

        for (Bird other : game.players) {
            if (other == null || other == this || other.health <= 0) continue;
            if (!canDamageTarget(other)) continue;

            double dx = other.bodyCenterX() - centerX;
            double dy = other.bodyCenterY() - centerY;
            double maxDx = 1120 + other.combatHalfWidth();
            double maxDy = 280 + other.combatHalfHeight();
            if (Math.abs(dx) > maxDx || Math.abs(dy) > maxDy) continue;

            double laneBias = 1.0 - Math.min(1.0, Math.abs(dy) / maxDy);
            double oldHealth = other.health;
            double dealtDamage = applyDamageTo(other, 28 + laneBias * 16);
            if (dealtDamage <= 0) continue;

            hitAnyone = true;
            heaviestHit = Math.max(heaviestHit, dealtDamage);
            game.damageDealt[playerIndex] += (int) Math.round(dealtDamage);
            boolean isKill = oldHealth > 0 && other.health <= 0;
            if (isKill) {
                game.eliminations[playerIndex]++;
                game.playZombieFallSfx();
            }

            spawnDamageParticles(other, dealtDamage);
            logDamageKillFeed(dealtDamage, isKill, other);

            double dir = dx == 0 ? (other.x >= x ? 1.0 : -1.0) : Math.signum(dx);
            other.vx += dir * (20 + laneBias * 16);
            other.vy -= 11 + laneBias * 8;
            other.applyStun(12 + (int) Math.round(laneBias * 8));
        }

        int particleCount = scaledParticleCount(hitAnyone ? 86 : 58);
        for (int i = 0; i < particleCount; i++) {
            double side = i % 2 == 0 ? -1.0 : 1.0;
            double speed = 10 + Math.random() * (hitAnyone ? 18 : 13);
            double spread = (Math.random() - 0.5) * 10;
            Color color = i % 3 == 0 ? Color.web("#FFCC80") : (i % 3 == 1 ? Color.web("#8D6E63") : Color.web("#ECEFF1"));
            game.particles.add(new Particle(
                    centerX + side * (18 + Math.random() * 22),
                    centerY + spread,
                    side * speed,
                    spread * 0.35 - 3,
                    color
            ));
        }

        game.playHugewaveSfx();
        game.shakeIntensity = Math.max(game.shakeIntensity, hitAnyone ? 30 : 18);
        game.hitstopFrames = Math.max(game.hitstopFrames, hitAnyone ? 12 : 6);
        if (hitAnyone) {
            game.triggerFlash(Math.min(0.92, 0.42 + heaviestHit / 42.0), false);
            game.playHitSound(heaviestHit);
        }
    }

    private void handlePowerUpType(PowerUp p, Iterator<PowerUp> it) {
        switch (p.type) {
            case HEALTH -> {
                heal(40);
                game.addToKillFeed(shortName() + " grabbed HEALTH! +40 HP");
            }
            case SPEED -> {
                speedMultiplier = baseSpeedMultiplier * 1.7;
                speedTimer = 480;
                game.addToKillFeed(shortName() + " got SPEED BOOST!");
            }
            case RAGE -> {
                powerMultiplier = basePowerMultiplier * 2.0;
                rageTimer = 420;
                game.addToKillFeed(shortName() + " is ENRAGED!");
            }
            case SHRINK -> {
                for (Bird b : game.players) {
                    if (b != null && b != this && canDamageTarget(b)) {
                        b.applyShrinkEffect();
                    }
                }
                game.addToKillFeed(shortName() + " SHRANK + WEAKENED enemies!");
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

                game.addToKillFeed(shortName() + " grabbed NEON BOOST! HYPERSPEED!");

                for (int i = 0; i < 80; i++) {
                    double angle = Math.random() * Math.PI * 2;
                    double speed = 10 + Math.random() * 20;
                    Color c = Math.random() < 0.5 ? Color.MAGENTA.brighter() : Color.CYAN.brighter();
                    game.particles.add(new Particle(x + 40, y + 40, Math.cos(angle) * speed, Math.sin(angle) * speed - 8, c));
                }

                game.shakeIntensity = 20;
                game.hitstopFrames = 12;

                if (!game.usesSmashCombatRules()) {
                    game.scores[playerIndex] += 20;
                }
                game.recordNeonPickupAchievement(playerIndex);
            }
            case THERMAL -> {
                thermalTimer = 600;
                thermalLift = 1.2;
                game.addToKillFeed(shortName() + " rides a THERMAL! SOARING!");
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

                game.recordThermalPickupAchievement(playerIndex);
            }
            case VINE_GRAPPLE -> {
                grappleTimer = 480;
                grappleUses = 1;
                game.addToKillFeed(shortName() + " grabbed VINE GRAPPLE! One summoned swing!");
                for (int i = 0; i < 80; i++) {
                    double angle = Math.random() * Math.PI * 2;
                    double speed = 8 + Math.random() * 16;
                    game.particles.add(new Particle(x + 40, y + 40,
                            Math.cos(angle) * speed, Math.sin(angle) * speed - 6,
                            Color.LIMEGREEN.brighter()));
                }
                game.shakeIntensity = 18;
                game.hitstopFrames = 10;
                game.recordVineGrapplePickupAchievement(playerIndex);
            }
            case OVERCHARGE -> {
                specialCooldown = 0;
                powerMultiplier = Math.max(powerMultiplier, basePowerMultiplier * 1.35);
                rageTimer = Math.max(rageTimer, 260);
                overchargeAttackTimer = Math.max(overchargeAttackTimer, 300);
                game.addToKillFeed(shortName() + " got OVERCHARGE! Special reset + rapid attacks!");
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
                game.addToKillFeed(shortName() + " entered TITAN FORM! (attack + defense)");
                for (int i = 0; i < 80; i++) {
                    double angle = Math.random() * Math.PI * 2;
                    double speed = 6 + Math.random() * 13;
                    game.particles.add(new Particle(x + 40, y + 40,
                            Math.cos(angle) * speed, Math.sin(angle) * speed - 5,
                            Color.GOLDENROD.brighter()));
                }
                game.shakeIntensity = Math.max(game.shakeIntensity, 18);
            }
            case BROADSIDE -> triggerBroadsidePickup();
        }

        game.recordPowerUpPickupForAchievements(this);
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

            if (tauntCyclePressed() && tauntCooldown <= 0) {
                currentTaunt = (currentTaunt % 3) + 1;
                tauntCooldown = 30;
            }

            if (tauntExecutePressed() && tauntCooldown <= 0 && currentTaunt != 0) {
                tauntTimer = 60;
                tauntCooldown = 120;
                game.tauntsPerformed[playerIndex]++;
                game.recordTauntForAchievements(this);
                game.checkAchievements(this);

                String tauntName = switch (currentTaunt) {
                    case 1 -> "FLIPPED OFF";
                    case 2 -> "CHALLENGED";
                    case 3 -> "MOONED";
                    default -> "TAUNTED";
                };
                game.addToKillFeed(shortName() + " " + tauntName + " EVERYONE!");

                for (int i = 0; i < 30; i++) {
                    Color c = currentTaunt == 1 ? Color.YELLOW : currentTaunt == 2 ? Color.RED : Color.PINK;
                    game.particles.add(new Particle(x + 40, y + 40, (Math.random() - 0.5) * 16, (Math.random() - 0.7) * 12, c));
                }
            }
        }
    }

    private void onDefeated() {
        releaseGrabState(false);
        clearAIInputs();
        removeOwnedSummons();

        isBlocking = false;
        shieldHealth = SHIELD_MAX_HEALTH;
        shieldStunFrames = 0;
        parryWindowFrames = 0;
        shieldHoldVisual = 0.0;
        displayPose = null;
        resetDodgeState();
        clearJumpSquat();
        jumpHeldLastFrame = false;
        specialHeldLastFrame = false;
        blockHeldLastFrame = false;
        leftHeldLastFrame = false;
        rightHeldLastFrame = false;
        techBufferTimer = 0;
        knockdownTimer = 0;
        grabCooldown = 0;
        grabHeldLastFrame = false;
        stunTime = 0;
        attackAnimationTimer = 0;
        clearAerialAttackState();
        landingLagTimer = 0;
        cancelAttackCharge();
        attackHeldLastFrame = false;
        cooldownFlash = 0;
        tauntTimer = 0;
        currentTaunt = 0;
        resetPigeonSpecialState();
        pigeonUpSpecialUsed = false;
        resetRaptorSpecialState();
        raptorCryReuseTimer = 0;
        raptorRushReuseTimer = 0;
        raptorUpSpecialUsed = false;
        isGroundPounding = false;
        isZipping = false;
        zipTimer = 0;
        bladeStormFrames = 0;
        razorbillDashVX = 0.0;
        razorbillDashVY = 0.0;
        Arrays.fill(razorbillDashHit, false);
        penguinIceFxTimer = 0;
        penguinDashDamageTimer = 0;
        Arrays.fill(penguinDashHit, false);
        hummingFrenzyTimer = 0;
        Arrays.fill(hummingFrenzyHitCooldown, 0);
        phoenixAfterburnTimer = 0;
        Arrays.fill(phoenixAfterburnHitCooldown, 0);
        leanTimer = 0;
        highTimer = 0;
        isHigh = false;
        hoverRegenMultiplier = 1.0;
        batEchoTimer = 0;
        batHanging = false;
        batHangPlatform = null;
        batHangLockTimer = 0;
        batRehangCooldownTimer = 0;
        ledgeHanging = false;
        ledgePlatform = null;
        ledgeGrabOnRightSide = false;
        ledgeLockTimer = 0;
        ledgeRegrabCooldownTimer = 0;
        ledgeInvulnerabilityTimer = 0;
        ledgeHangFrames = 0;
        roadrunnerSandstormTimer = 0;
        roadrunnerSandGustTimer = 0;
        Arrays.fill(roadrunnerSandHitCooldown, 0);
        attachedVine = null;
        onVine = false;
        vineRideFrames = 0;
        isGrappling = false;
        isFlying = false;
        loungeActive = false;
        loungeHealth = 0;
        loungeRoyal = false;
        loungeDamageFlash = 0;
        nullRockInvincibilityTimer = 0;
        nullRockPhaseIndex = 0;
        nullRockShieldFxCooldown = 0;
        trueNullRockForm = false;
        resetSmashCombatState();
    }

    private void removeOwnedSummons() {
        game.crowMinions.removeIf(crow -> crow.owner == this);
        game.chickMinions.removeIf(chick -> chick.owner == this);
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
        state.smashDamage = smashDamage;
        state.stunTime = stunTime;
        state.specialCooldown = specialCooldown;
        state.specialMaxCooldown = specialMaxCooldown;
        state.attackCooldown = attackCooldown;
        state.attackAnimationTimer = attackAnimationTimer;
        state.attackChargeFrames = attackChargeFrames;
        state.pendingGroundAttackFrames = pendingGroundAttackFrames;
        state.pendingGroundAttackVariantOrdinal = pendingGroundAttackVariant.ordinal();
        state.chargingAttackVariantOrdinal = chargingAttackVariant.ordinal();
        state.activeAttackVariantOrdinal = activeAttackVariant.ordinal();
        state.aerialAttackActive = aerialAttackActive;
        state.aerialAttackTotalFrames = aerialAttackTotalFrames;
        state.activeAerialLandingLagFrames = activeAerialLandingLagFrames;
        state.landingLagTimer = landingLagTimer;
        state.canDoubleJump = canDoubleJump;
        state.jumpSquatTimer = jumpSquatTimer;
        state.shortHopQueued = shortHopQueued;
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
        state.isStormSkin = isStormSkin;
        state.isSunflareSkin = isSunflareSkin;
        state.isGlacierSkin = isGlacierSkin;
        state.isTideSkin = isTideSkin;
        state.isNullRockSkin = isNullRockSkin;
        state.isEclipseSkin = isEclipseSkin;
        state.isUmbraSkin = isUmbraSkin;
        state.isResonanceSkin = isResonanceSkin;
        state.isIroncladSkin = isIroncladSkin;
        state.isSunforgeSkin = isSunforgeSkin;
        state.isPhotoEagleSkin = isPhotoEagleSkin;
        state.suppressSelectEffects = suppressSelectEffects;
        state.loungeX = loungeX;
        state.loungeY = loungeY;
        state.loungeHealth = loungeHealth;
        state.loungeDamageFlash = loungeDamageFlash;
        state.loungeMaxHealth = loungeMaxHealth;
        state.loungeRoyal = loungeRoyal;
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
        state.batRehangCooldownTimer = batRehangCooldownTimer;
        state.ledgeHanging = ledgeHanging;
        state.ledgeGrabOnRightSide = ledgeGrabOnRightSide;
        state.ledgeLockTimer = ledgeLockTimer;
        state.ledgeRegrabCooldownTimer = ledgeRegrabCooldownTimer;
        state.ledgeInvulnerabilityTimer = ledgeInvulnerabilityTimer;
        state.isBlocking = isBlocking;
        state.blockCooldown = blockCooldown;
        state.shieldHealth = shieldHealth;
        state.shieldStunFrames = shieldStunFrames;
        state.parryWindowFrames = parryWindowFrames;
        state.shieldHoldVisual = shieldHoldVisual;
        state.dodgeTypeOrdinal = dodgeType.ordinal();
        state.dodgeTimer = dodgeTimer;
        state.dodgeInvulnerabilityTimer = dodgeInvulnerabilityTimer;
        state.dodgeCooldown = dodgeCooldown;
        state.dodgeDirection = dodgeDirection;
        state.airDodgeAvailable = airDodgeAvailable;
        state.techBufferTimer = techBufferTimer;
        state.knockdownTimer = knockdownTimer;
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
        state.roadrunnerSandstormTimer = roadrunnerSandstormTimer;
        state.roadrunnerSandGustTimer = roadrunnerSandGustTimer;
        System.arraycopy(roadrunnerSandHitCooldown, 0, state.roadrunnerSandHitCooldown, 0, roadrunnerSandHitCooldown.length);
        state.pigeonFeatherBurstTimer = pigeonFeatherBurstTimer;
        state.pigeonFeatherBurstUltimate = pigeonFeatherBurstUltimate;
        state.pigeonRushTimer = pigeonRushTimer;
        state.pigeonRushGrounded = pigeonRushGrounded;
        state.pigeonRushUltimate = pigeonRushUltimate;
        System.arraycopy(pigeonRushHit, 0, state.pigeonRushHit, 0, pigeonRushHit.length);
        state.pigeonFlutterTimer = pigeonFlutterTimer;
        state.pigeonFlutterUltimate = pigeonFlutterUltimate;
        System.arraycopy(pigeonFlutterHit, 0, state.pigeonFlutterHit, 0, pigeonFlutterHit.length);
        state.pigeonScavengeTimer = pigeonScavengeTimer;
        state.pigeonScavengeAirborne = pigeonScavengeAirborne;
        state.pigeonScavengeUltimate = pigeonScavengeUltimate;
        state.pigeonScavengeResolved = pigeonScavengeResolved;
        state.pigeonUpSpecialUsed = pigeonUpSpecialUsed;
        state.raptorCryTimer = raptorCryTimer;
        state.raptorCryUltimate = raptorCryUltimate;
        state.raptorRushTimer = raptorRushTimer;
        state.raptorRushUltimate = raptorRushUltimate;
        state.raptorRushGrounded = raptorRushGrounded;
        state.raptorRushDirection = raptorRushDirection;
        System.arraycopy(raptorRushHit, 0, state.raptorRushHit, 0, raptorRushHit.length);
        state.raptorClimbTimer = raptorClimbTimer;
        state.raptorClimbUltimate = raptorClimbUltimate;
        state.raptorClimbDirection = raptorClimbDirection;
        System.arraycopy(raptorClimbHit, 0, state.raptorClimbHit, 0, raptorClimbHit.length);
        state.raptorCryReuseTimer = raptorCryReuseTimer;
        state.raptorRushReuseTimer = raptorRushReuseTimer;
        state.raptorUpSpecialUsed = raptorUpSpecialUsed;
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
        state.ultimateMeter = ultimateMeter;
        state.ultimateFxTimer = ultimateFxTimer;
        state.nullRockInvincibilityTimer = nullRockInvincibilityTimer;
        state.nullRockPhaseIndex = nullRockPhaseIndex;
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
        this.smashDamage = state.smashDamage;
        this.stunTime = state.stunTime;
        this.specialCooldown = state.specialCooldown;
        this.specialMaxCooldown = state.specialMaxCooldown;
        this.attackCooldown = state.attackCooldown;
        this.attackAnimationTimer = state.attackAnimationTimer;
        this.attackChargeFrames = state.attackChargeFrames;
        NormalAttackVariant[] attackVariants = NormalAttackVariant.values();
        this.pendingGroundAttackFrames = Math.max(0, state.pendingGroundAttackFrames);
        if (state.pendingGroundAttackVariantOrdinal >= 0 && state.pendingGroundAttackVariantOrdinal < attackVariants.length) {
            this.pendingGroundAttackVariant = attackVariants[state.pendingGroundAttackVariantOrdinal];
        } else {
            this.pendingGroundAttackVariant = NormalAttackVariant.NEUTRAL;
        }
        if (state.chargingAttackVariantOrdinal >= 0 && state.chargingAttackVariantOrdinal < attackVariants.length) {
            this.chargingAttackVariant = attackVariants[state.chargingAttackVariantOrdinal];
        } else {
            this.chargingAttackVariant = NormalAttackVariant.NEUTRAL;
        }
        if (state.activeAttackVariantOrdinal >= 0 && state.activeAttackVariantOrdinal < attackVariants.length) {
            this.activeAttackVariant = attackVariants[state.activeAttackVariantOrdinal];
        } else {
            this.activeAttackVariant = NormalAttackVariant.NEUTRAL;
        }
        this.aerialAttackActive = state.aerialAttackActive;
        this.aerialAttackTotalFrames = state.aerialAttackTotalFrames;
        this.activeAerialLandingLagFrames = state.activeAerialLandingLagFrames > 0
                ? state.activeAerialLandingLagFrames
                : AERIAL_LANDING_LAG_FRAMES;
        this.landingLagTimer = state.landingLagTimer;
        this.canDoubleJump = state.canDoubleJump;
        this.jumpSquatTimer = state.jumpSquatTimer;
        this.shortHopQueued = state.shortHopQueued;
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
        this.isStormSkin = state.isStormSkin;
        this.isSunflareSkin = state.isSunflareSkin;
        this.isGlacierSkin = state.isGlacierSkin;
        this.isTideSkin = state.isTideSkin;
        this.isNullRockSkin = state.isNullRockSkin;
        this.isEclipseSkin = state.isEclipseSkin;
        this.isUmbraSkin = state.isUmbraSkin;
        this.isResonanceSkin = state.isResonanceSkin;
        this.isIroncladSkin = state.isIroncladSkin;
        this.isSunforgeSkin = state.isSunforgeSkin;
        this.isPhotoEagleSkin = state.isPhotoEagleSkin;
        this.suppressSelectEffects = state.suppressSelectEffects;
        this.loungeX = state.loungeX;
        this.loungeY = state.loungeY;
        this.loungeHealth = state.loungeHealth;
        this.loungeDamageFlash = state.loungeDamageFlash;
        this.loungeMaxHealth = state.loungeMaxHealth > 0 ? state.loungeMaxHealth : LOUNGE_MAX_HEALTH;
        this.loungeRoyal = state.loungeRoyal;
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
        this.batRehangCooldownTimer = state.batRehangCooldownTimer;
        this.ledgeHanging = state.ledgeHanging;
        this.ledgeGrabOnRightSide = state.ledgeGrabOnRightSide;
        this.ledgeLockTimer = state.ledgeLockTimer;
        this.ledgeRegrabCooldownTimer = state.ledgeRegrabCooldownTimer;
        this.ledgeInvulnerabilityTimer = state.ledgeInvulnerabilityTimer;
        this.ledgeHangFrames = 0;
        this.ledgePlatform = null;
        this.isBlocking = state.isBlocking;
        this.blockCooldown = state.blockCooldown;
        this.shieldHealth = state.shieldHealth;
        this.shieldStunFrames = state.shieldStunFrames;
        this.parryWindowFrames = state.parryWindowFrames;
        this.shieldHoldVisual = state.shieldHoldVisual;
        DodgeType[] dodgeTypes = DodgeType.values();
        if (state.dodgeTypeOrdinal >= 0 && state.dodgeTypeOrdinal < dodgeTypes.length) {
            this.dodgeType = dodgeTypes[state.dodgeTypeOrdinal];
        } else {
            this.dodgeType = DodgeType.NONE;
        }
        this.dodgeTimer = state.dodgeTimer;
        this.dodgeInvulnerabilityTimer = state.dodgeInvulnerabilityTimer;
        this.dodgeCooldown = state.dodgeCooldown;
        this.dodgeDirection = state.dodgeDirection;
        this.airDodgeAvailable = state.airDodgeAvailable;
        this.techBufferTimer = state.techBufferTimer;
        this.knockdownTimer = state.knockdownTimer;
        this.attackHeldLastFrame = false;
        this.jumpHeldLastFrame = false;
        this.specialHeldLastFrame = false;
        this.blockHeldLastFrame = false;
        this.grabHeldLastFrame = false;
        this.leftHeldLastFrame = false;
        this.rightHeldLastFrame = false;
        this.speedMultiplier = state.speedMultiplier;
        this.powerMultiplier = state.powerMultiplier;
        this.sizeMultiplier = state.sizeMultiplier;
        this.baseSpeedMultiplier = state.baseSpeedMultiplier;
        this.basePowerMultiplier = state.basePowerMultiplier;
        this.baseSizeMultiplier = state.baseSizeMultiplier;
        this.ledgePlatform = ledgeHanging ? resolveClosestLedgePlatformForState() : null;
        if (ledgeHanging && ledgePlatform == null) {
            clearLedgeHangState(0);
        }
        this.speedTimer = state.speedTimer;
        this.rageTimer = state.rageTimer;
        this.shrinkTimer = state.shrinkTimer;
        this.titanTimer = state.titanTimer;
        this.titanActive = state.titanActive;
        this.neonRushTimer = state.neonRushTimer;
        this.thermalTimer = state.thermalTimer;
        this.thermalLift = state.thermalLift;
        this.overchargeAttackTimer = state.overchargeAttackTimer;
        this.roadrunnerSandstormTimer = state.roadrunnerSandstormTimer;
        this.roadrunnerSandGustTimer = state.roadrunnerSandGustTimer;
        Arrays.fill(this.roadrunnerSandHitCooldown, 0);
        if (state.roadrunnerSandHitCooldown != null) {
            System.arraycopy(
                    state.roadrunnerSandHitCooldown,
                    0,
                    this.roadrunnerSandHitCooldown,
                    0,
                    Math.min(this.roadrunnerSandHitCooldown.length, state.roadrunnerSandHitCooldown.length)
            );
        } else {
            Arrays.fill(this.roadrunnerSandHitCooldown, 0);
        }
        this.pigeonFeatherBurstTimer = state.pigeonFeatherBurstTimer;
        this.pigeonFeatherBurstUltimate = state.pigeonFeatherBurstUltimate;
        this.pigeonRushTimer = state.pigeonRushTimer;
        this.pigeonRushGrounded = state.pigeonRushGrounded;
        this.pigeonRushUltimate = state.pigeonRushUltimate;
        Arrays.fill(this.pigeonRushHit, false);
        if (state.pigeonRushHit != null) {
            System.arraycopy(state.pigeonRushHit, 0, this.pigeonRushHit, 0,
                    Math.min(this.pigeonRushHit.length, state.pigeonRushHit.length));
        }
        this.pigeonFlutterTimer = state.pigeonFlutterTimer;
        this.pigeonFlutterUltimate = state.pigeonFlutterUltimate;
        Arrays.fill(this.pigeonFlutterHit, false);
        if (state.pigeonFlutterHit != null) {
            System.arraycopy(state.pigeonFlutterHit, 0, this.pigeonFlutterHit, 0,
                    Math.min(this.pigeonFlutterHit.length, state.pigeonFlutterHit.length));
        }
        this.pigeonScavengeTimer = state.pigeonScavengeTimer;
        this.pigeonScavengeAirborne = state.pigeonScavengeAirborne;
        this.pigeonScavengeUltimate = state.pigeonScavengeUltimate;
        this.pigeonScavengeResolved = state.pigeonScavengeResolved;
        this.pigeonUpSpecialUsed = state.pigeonUpSpecialUsed;
        this.raptorCryTimer = state.raptorCryTimer;
        this.raptorCryUltimate = state.raptorCryUltimate;
        this.raptorRushTimer = state.raptorRushTimer;
        this.raptorRushUltimate = state.raptorRushUltimate;
        this.raptorRushGrounded = state.raptorRushGrounded;
        this.raptorRushDirection = state.raptorRushDirection == 0 ? 1 : state.raptorRushDirection;
        Arrays.fill(this.raptorRushHit, false);
        if (state.raptorRushHit != null) {
            System.arraycopy(state.raptorRushHit, 0, this.raptorRushHit, 0,
                    Math.min(this.raptorRushHit.length, state.raptorRushHit.length));
        }
        this.raptorClimbTimer = state.raptorClimbTimer;
        this.raptorClimbUltimate = state.raptorClimbUltimate;
        this.raptorClimbDirection = state.raptorClimbDirection == 0 ? 1 : state.raptorClimbDirection;
        Arrays.fill(this.raptorClimbHit, false);
        if (state.raptorClimbHit != null) {
            System.arraycopy(state.raptorClimbHit, 0, this.raptorClimbHit, 0,
                    Math.min(this.raptorClimbHit.length, state.raptorClimbHit.length));
        }
        this.raptorCryReuseTimer = state.raptorCryReuseTimer;
        this.raptorRushReuseTimer = state.raptorRushReuseTimer;
        this.raptorUpSpecialUsed = state.raptorUpSpecialUsed;
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
        this.ultimateMeter = state.ultimateMeter;
        this.ultimateFxTimer = state.ultimateFxTimer;
        this.nullRockInvincibilityTimer = state.nullRockInvincibilityTimer;
        this.nullRockPhaseIndex = state.nullRockPhaseIndex;
        updateDisplayPose(1.0);
    }

    double smashDamagePercent() {
        return Math.max(0.0, smashDamage);
    }

    private void registerSmashHit(Bird attacker, double dealtDamage) {
        if (attacker != null && attacker != this && attacker.playerIndex >= 0) {
            recentSmashAttackerIndex = attacker.playerIndex;
            recentSmashAttackerFrames = SMASH_KO_CREDIT_FRAMES;
        }
        double percent = smashDamagePercent();
        double scaledPercent = percent <= 0.0 ? 0.0 : Math.pow(percent / 115.0, 1.18);
        double launchScale = 1.0 + Math.min(3.8, scaledPercent + dealtDamage / 55.0);
        pendingSmashLaunchScale = Math.max(pendingSmashLaunchScale, launchScale);
    }

    private void applyPendingSmashLaunch() {
        if (!game.usesSmashCombatRules() || pendingSmashLaunchScale <= 1.0001) {
            return;
        }
        double launchScale = pendingSmashLaunchScale;
        vx *= launchScale * SMASH_HORIZONTAL_LAUNCH_SCALE;
        vy *= launchScale * SMASH_VERTICAL_LAUNCH_SCALE;
        if (vy <= 0.0) {
            double minimumUpwardLaunch = SMASH_MIN_UPWARD_LAUNCH_SCALE * launchScale;
            if (vy > -minimumUpwardLaunch) {
                vy = -minimumUpwardLaunch;
            }
        }
        applySmashDirectionalInfluence();
        pendingSmashLaunchScale = 1.0;
    }

    private void applySmashDirectionalInfluence() {
        double launchSpeed = Math.hypot(vx, vy);
        if (launchSpeed <= 0.001) {
            return;
        }

        double inputX = 0.0;
        if (leftPressed()) {
            inputX -= 1.0;
        }
        if (rightPressed()) {
            inputX += 1.0;
        }

        double inputY = 0.0;
        if (jumpPressed()) {
            inputY -= 1.0;
        }
        if (blockPressed()) {
            inputY += 1.0;
        }

        if (inputX == 0.0 && inputY == 0.0) {
            return;
        }

        double inputMagnitude = Math.hypot(inputX, inputY);
        inputX /= inputMagnitude;
        inputY /= inputMagnitude;

        double dirX = vx / launchSpeed;
        double dirY = vy / launchSpeed;
        double perpendicularX = -dirY;
        double diAmount = Math.clamp(inputX * perpendicularX + inputY * dirX, -1.0, 1.0);
        if (Math.abs(diAmount) <= 0.001) {
            return;
        }

        double diAngle = diAmount * SMASH_DI_MAX_ANGLE_RADIANS;
        double cos = Math.cos(diAngle);
        double sin = Math.sin(diAngle);
        double adjustedX = dirX * cos - dirY * sin;
        double adjustedY = dirX * sin + dirY * cos;
        vx = adjustedX * launchSpeed;
        vy = adjustedY * launchSpeed;
    }

    private void handleSmashBlastZoneKo(boolean trainingDummy, boolean islandBounds, double leftBound, double rightBound,
                                        double fallbackX, double fallbackY, String zoneLabel,
                                        boolean awardStageFallAchievement) {
        game.falls[playerIndex]++;
        game.shakeIntensity = Math.max(game.shakeIntensity, 18);
        game.hitstopFrames = Math.max(game.hitstopFrames, 6);

        int stocksRemaining = game.matchScoreForPlayer(playerIndex);
        if (!trainingDummy) {
            int attackerIndex = recentSmashAttackerFrames > 0 ? recentSmashAttackerIndex : -1;
            Bird attacker = attackerIndex >= 0 && attackerIndex < game.players.length ? game.players[attackerIndex] : null;
            boolean creditedKo = attacker != null && attacker != this;
            game.scores[playerIndex] = Math.max(0, game.scores[playerIndex] - 1);
            stocksRemaining = game.matchScoreForPlayer(playerIndex);
            String stockText = stocksRemaining > 0
                    ? (stocksRemaining == 1 ? "1 stock left." : stocksRemaining + " stocks left.")
                    : "OUT OF STOCKS!";
            if (creditedKo) {
                game.eliminations[attackerIndex]++;
                game.checkAchievements(attacker);
                game.addToKillFeed(attacker.shortName() + " KO'd " + shortName() + " " + zoneLabel + "! " + stockText);
            } else {
                game.addToKillFeed(shortName() + " blasted out " + zoneLabel + "! " + stockText);
            }
            game.playZombieFallSfx();
        }

        if (awardStageFallAchievement && !game.trainingModeActive) {
            game.recordStageFallAchievement(playerIndex);
        }

        if (!trainingDummy && stocksRemaining <= 0) {
            retireFromStockMatch();
            return;
        }

        respawnAfterStageLoss(trainingDummy, islandBounds, leftBound, rightBound, fallbackX, fallbackY);
    }

    private void resetSmashCombatState() {
        smashDamage = 0.0;
        recentSmashAttackerIndex = -1;
        recentSmashAttackerFrames = 0;
        pendingSmashLaunchScale = 1.0;
        techBufferTimer = 0;
        knockdownTimer = 0;
    }

    private double aiDurabilityHealth() {
        if (!game.usesSmashCombatRules()) {
            return health;
        }
        return Math.max(0.0, STARTING_HEALTH - smashDamagePercent());
    }

    private void drawAttackChargeFx(GraphicsContext g, double drawSize) {
        if (!isChargingAttack()) {
            return;
        }
        double chargeRatio = attackChargeRatio(attackChargeFrames);
        double pulse = 0.55 + 0.45 * Math.sin(System.currentTimeMillis() / 55.0);
        double pad = (10.0 + chargeRatio * 22.0) * sizeMultiplier;
        g.setStroke(Color.web("#FFF59D").deriveColor(0, 1, 1, 0.36 + chargeRatio * 0.34));
        g.setLineWidth(1.8 + chargeRatio * 3.0);
        g.strokeOval(x - pad, y - pad, drawSize + pad * 2, drawSize + pad * 2);
        g.setStroke(Color.web("#FFB74D").deriveColor(0, 1, 1, 0.22 + chargeRatio * 0.26 * pulse));
        g.setLineWidth(3.4 + chargeRatio * 4.0);
        g.strokeArc(x - pad * 0.7, y - pad * 0.7, drawSize + pad * 1.4, drawSize + pad * 1.4,
                (System.currentTimeMillis() / 6.0) % 360.0, 110 + chargeRatio * 120, ArcType.OPEN);
    }

    private boolean pigeonSpecialPoseActive() {
        return type == BirdGame3.BirdType.PIGEON
                && (pigeonFeatherBurstTimer > 0 || pigeonRushTimer > 0 || pigeonFlutterTimer > 0 || pigeonScavengeTimer > 0);
    }

    private double pigeonSpecialPhase(int timer, int totalFrames) {
        if (timer <= 0 || totalFrames <= 0) {
            return 0.0;
        }
        return Math.clamp(1.0 - ((timer - 1.0) / (double) totalFrames), 0.0, 1.0);
    }

    private boolean raptorSpecialPoseActive() {
        return isRaptor() && (raptorCryTimer > 0 || raptorRushTimer > 0 || raptorClimbTimer > 0 || eagleDiveActive || eagleAscentActive);
    }

    private double raptorSpecialPhase(int timer, int totalFrames) {
        if (timer <= 0 || totalFrames <= 0) {
            return 0.0;
        }
        return Math.clamp(1.0 - ((timer - 1.0) / (double) totalFrames), 0.0, 1.0);
    }

    private AttackVisualPose currentPigeonSpecialPose() {
        double dir = facingRight ? 1.0 : -1.0;
        if (pigeonFlutterTimer > 0) {
            double phase = pigeonSpecialPhase(pigeonFlutterTimer,
                    pigeonFlutterUltimate ? PIGEON_FLUTTER_ULTIMATE_FRAMES : PIGEON_FLUTTER_FRAMES);
            return new AttackVisualPose(
                    dir * (1.5 + 1.5 * phase),
                    -10.0 - 12.0 * phase,
                    dir * (5.0 + 3.0 * phase),
                    normalizeAngleRadians(-Math.PI / 2.0 + dir * 0.12),
                    14.0 * phase,
                    -18.0 - 5.0 * phase,
                    12.0 * phase,
                    1.08 + 0.04 * phase,
                    -30.0 - 8.0 * phase,
                    0.98,
                    1.12 + 0.08 * phase
            );
        }
        if (pigeonRushTimer > 0) {
            double phase = pigeonSpecialPhase(pigeonRushTimer,
                    pigeonRushGrounded ? PIGEON_RUSH_GROUND_FRAMES : PIGEON_RUSH_AIR_FRAMES);
            return new AttackVisualPose(
                    dir * (10.0 + 7.0 * phase),
                    pigeonRushGrounded ? -2.0 - 2.0 * phase : -6.0 - 5.0 * phase,
                    dir * (8.0 + 4.0 * phase),
                    facingRight ? -0.14 : Math.PI + 0.14,
                    16.0 + 6.0 * phase,
                    -5.0 * phase,
                    18.0 + 8.0 * phase,
                    0.80,
                    dir * (8.0 + 4.0 * phase),
                    1.18 + 0.12 * phase,
                    0.92
            );
        }
        if (pigeonScavengeTimer > 0) {
            double phase = pigeonSpecialPhase(pigeonScavengeTimer,
                    pigeonScavengeAirborne ? PIGEON_SCAVENGE_AIR_FRAMES : PIGEON_SCAVENGE_GROUND_FRAMES);
            if (pigeonScavengeAirborne) {
                return new AttackVisualPose(
                        dir * 2.0,
                        8.0 + 10.0 * phase,
                        dir * (3.0 + 2.0 * phase),
                        normalizeAngleRadians(Math.PI / 2.0 - dir * 0.10),
                        9.0 * phase,
                        10.0 * phase,
                        8.0 * phase,
                        0.82,
                        20.0 + 10.0 * phase,
                        1.01,
                        0.88
                );
            }
            double digPulse = 0.5 + 0.5 * Math.sin((PIGEON_SCAVENGE_GROUND_FRAMES - pigeonScavengeTimer) * 0.55);
            return new AttackVisualPose(
                    dir * (1.5 + 1.5 * phase),
                    8.0 + digPulse * 5.0,
                    dir * (4.0 + 2.0 * phase),
                    normalizeAngleRadians(Math.PI / 2.0 - dir * 0.34),
                    12.0 * phase,
                    8.0 + digPulse * 4.0,
                    9.0 * phase,
                    0.72,
                    dir * (5.0 + 3.0 * phase),
                    1.08 + 0.04 * phase,
                    0.82
            );
        }

        double phase = pigeonSpecialPhase(pigeonFeatherBurstTimer, PIGEON_NEUTRAL_BURST_FRAMES);
        return new AttackVisualPose(
                dir * (5.0 + 3.0 * phase),
                -1.5 * phase,
                dir * (3.0 + 2.0 * phase),
                facingRight ? 0.0 : Math.PI,
                10.0 + 6.0 * phase,
                -2.0 * phase,
                11.0 + 5.0 * phase,
                1.10,
                dir * (3.0 + 2.0 * phase),
                1.05 + 0.04 * phase,
                0.98
        );
    }

    private AttackVisualPose currentRaptorSpecialPose() {
        double dir = facingRight ? 1.0 : -1.0;
        boolean eagle = type == BirdGame3.BirdType.EAGLE;
        if (eagleDiveActive || eagleAscentActive) {
            if (eagleAscentActive) {
                double phase = raptorSpecialPhase(eagleAscentFrames, 36);
                return new AttackVisualPose(
                        dir * (3.0 + 2.0 * phase),
                        -14.0 - 10.0 * phase,
                        dir * (6.0 + 4.0 * phase),
                        normalizeAngleRadians(-Math.PI / 2.0 + dir * 0.08),
                        14.0 + 7.0 * phase,
                        -18.0 - 6.0 * phase,
                        12.0 + 7.0 * phase,
                        1.0,
                        -18.0 - 10.0 * phase,
                        0.98,
                        1.10 + 0.05 * phase
                );
            }
            if (eagleDiveCountdown > 0) {
                boolean diveUltimate = eagle ? diveTimer > EAGLE_DIVE_FRAMES : diveTimer > FALCON_DIVE_FRAMES;
                int startupFrames = eagle
                        ? (diveUltimate ? EAGLE_DIVE_GROUND_ULTIMATE_STARTUP_FRAMES : EAGLE_DIVE_GROUND_STARTUP_FRAMES)
                        : (diveUltimate ? FALCON_DIVE_GROUND_ULTIMATE_STARTUP_FRAMES : FALCON_DIVE_GROUND_STARTUP_FRAMES);
                double phase = raptorSpecialPhase(eagleDiveCountdown, startupFrames);
                return new AttackVisualPose(
                        dir * (2.0 + 2.0 * phase),
                        -9.0 - 11.0 * phase,
                        dir * (4.0 + 3.0 * phase),
                        normalizeAngleRadians(-Math.PI / 2.0 + dir * (eagle ? 0.10 : 0.18)),
                        12.0 + 5.0 * phase,
                        -12.0 - 4.0 * phase,
                        10.0 + 5.0 * phase,
                        0.92,
                        -12.0 - 8.0 * phase,
                        0.98,
                        1.08 + 0.04 * phase
                );
            }
            double phase = raptorSpecialPhase(diveTimer, eagle ? EAGLE_DIVE_ULTIMATE_FRAMES : FALCON_DIVE_ULTIMATE_FRAMES);
            if (!eagle) {
                return new AttackVisualPose(
                        dir * (10.0 + 8.0 * phase),
                        4.0 + 12.0 * phase,
                        dir * (18.0 + 10.0 * phase),
                        normalizeAngleRadians(Math.PI / 2.0 - dir * (Math.PI / 4.0)),
                        18.0 + 10.0 * phase,
                        8.0 + 7.0 * phase,
                        15.0 + 9.0 * phase,
                        0.76,
                        22.0 + 14.0 * phase,
                        1.08 + 0.08 * phase,
                        0.86
                );
            }
            return new AttackVisualPose(
                    dir * (9.0 + 7.0 * phase),
                    6.0 + 16.0 * phase,
                    dir * (8.0 + 5.0 * phase),
                    normalizeAngleRadians(Math.PI / 2.0 - dir * 0.16),
                    16.0 + 8.0 * phase,
                    11.0 + 8.0 * phase,
                    14.0 + 9.0 * phase,
                    0.74,
                    18.0 + 12.0 * phase,
                    1.06 + 0.08 * phase,
                    0.84
            );
        }
        if (raptorClimbTimer > 0) {
            double phase = raptorSpecialPhase(raptorClimbTimer,
                    eagle
                            ? (raptorClimbUltimate ? EAGLE_CLIMB_ULTIMATE_FRAMES : EAGLE_CLIMB_FRAMES)
                            : (raptorClimbUltimate ? FALCON_CLIMB_ULTIMATE_FRAMES : FALCON_CLIMB_FRAMES));
            return new AttackVisualPose(
                    dir * (2.0 + 2.0 * phase),
                    -12.0 - 13.0 * phase,
                    dir * (4.0 + 3.0 * phase),
                    normalizeAngleRadians(-Math.PI / 2.0 + dir * (eagle ? 0.09 : 0.18)),
                    12.0 + 6.0 * phase,
                    -16.0 - 4.0 * phase,
                    10.0 + 6.0 * phase,
                    0.92,
                    -24.0 - 8.0 * phase,
                    0.98,
                    1.10 + 0.06 * phase
            );
        }
        if (raptorRushTimer > 0) {
            double phase = raptorSpecialPhase(raptorRushTimer,
                    eagle
                            ? (raptorRushGrounded ? EAGLE_RUSH_GROUND_FRAMES : EAGLE_RUSH_AIR_FRAMES)
                            : (raptorRushGrounded ? FALCON_RUSH_GROUND_FRAMES : FALCON_RUSH_AIR_FRAMES));
            return new AttackVisualPose(
                    dir * (12.0 + 8.0 * phase),
                    eagle ? -4.0 - 2.0 * phase : -7.0 - 5.0 * phase,
                    dir * (8.0 + 5.0 * phase),
                    facingRight ? -0.18 : Math.PI + 0.18,
                    18.0 + 7.0 * phase,
                    -4.0 * phase,
                    16.0 + 9.0 * phase,
                    0.78,
                    dir * (10.0 + 5.0 * phase),
                    1.15 + 0.08 * phase,
                    0.90
            );
        }

        double phase = raptorSpecialPhase(raptorCryTimer,
                eagle
                        ? (raptorCryUltimate ? EAGLE_CRY_ULTIMATE_FRAMES : EAGLE_CRY_FRAMES)
                        : (raptorCryUltimate ? FALCON_CRY_ULTIMATE_FRAMES : FALCON_CRY_FRAMES));
        return new AttackVisualPose(
                dir * (4.0 + 3.0 * phase),
                -3.0 - 2.0 * phase,
                dir * (4.0 + 2.0 * phase),
                facingRight ? 0.0 : Math.PI,
                12.0 + 6.0 * phase,
                -4.0 - 3.0 * phase,
                12.0 + 6.0 * phase,
                1.14 + 0.08 * phase,
                dir * (4.0 + 3.0 * phase),
                1.04 + 0.03 * phase,
                1.0
        );
    }

    private NormalAttackVariant currentDisplayedAttackVariant() {
        if (pigeonSpecialPoseActive() || raptorSpecialPoseActive()) {
            return null;
        }
        if (isGroundAttackPending()) {
            return pendingGroundAttackVariant;
        }
        if (isChargingAttack()) {
            return chargingAttackVariant;
        }
        if (attackAnimationTimer > 0) {
            return activeAttackVariant;
        }
        return null;
    }

    private static double normalizeAngleRadians(double angle) {
        double normalized = angle;
        while (normalized <= -Math.PI) {
            normalized += Math.PI * 2.0;
        }
        while (normalized > Math.PI) {
            normalized -= Math.PI * 2.0;
        }
        return normalized;
    }

    private static double normalizeAngleDegrees(double angle) {
        double normalized = angle;
        while (normalized <= -180.0) {
            normalized += 360.0;
        }
        while (normalized > 180.0) {
            normalized -= 360.0;
        }
        return normalized;
    }

    private static double scaledBlendFactor(double perFrameBlend, double gameSpeed) {
        return 1.0 - Math.pow(1.0 - Math.clamp(perFrameBlend, 0.0, 1.0), Math.max(0.0, gameSpeed));
    }

    private static double blendValue(double current, double target, double blend) {
        return current + (target - current) * blend;
    }

    private static double blendDegrees(double current, double target, double blend) {
        return current + normalizeAngleDegrees(target - current) * blend;
    }

    private static double blendRadians(double current, double target, double blend) {
        return normalizeAngleRadians(current + normalizeAngleRadians(target - current) * blend);
    }

    private static AttackVisualPose blendAttackVisualPose(AttackVisualPose current, AttackVisualPose target, double blend) {
        if (current == null || target == null) {
            return target;
        }
        return new AttackVisualPose(
                blendValue(current.translateX(), target.translateX(), blend),
                blendValue(current.translateY(), target.translateY(), blend),
                blendDegrees(current.bodyRotationDegrees(), target.bodyRotationDegrees(), blend),
                blendRadians(current.aimAngleRadians(), target.aimAngleRadians(), blend),
                blendValue(current.headReachBonus(), target.headReachBonus(), blend),
                blendValue(current.headLift(), target.headLift(), blend),
                blendValue(current.beakLengthBonus(), target.beakLengthBonus(), blend),
                blendValue(current.beakOpenScale(), target.beakOpenScale(), blend),
                blendValue(current.spriteRotationDegrees(), target.spriteRotationDegrees(), blend),
                blendValue(current.spriteScaleX(), target.spriteScaleX(), blend),
                blendValue(current.spriteScaleY(), target.spriteScaleY(), blend)
        );
    }

    private double currentVisualPoseBlendPerFrame() {
        if (isDodging()) {
            return VISUAL_POSE_DODGE_BLEND_PER_FRAME;
        }
        if (stunTime > 0.0 || jumpSquatTimer > 0 || landingLagTimer > 0
                || isChargingAttack() || attackAnimationTimer > 0 || aerialAttackActive
                || pigeonSpecialPoseActive() || raptorSpecialPoseActive()) {
            return VISUAL_POSE_ACTION_BLEND_PER_FRAME;
        }
        if (!isOnGround() && Math.abs(vy) > 4.0) {
            return VISUAL_POSE_AIR_BLEND_PER_FRAME;
        }
        return VISUAL_POSE_IDLE_BLEND_PER_FRAME;
    }

    private void updateDisplayPose(double gameSpeed) {
        AttackVisualPose targetPose = currentTargetAttackVisualPose();
        if (displayPose == null || gameSpeed <= 0.0) {
            displayPose = targetPose;
            return;
        }
        double blend = scaledBlendFactor(currentVisualPoseBlendPerFrame(), gameSpeed);
        displayPose = blendAttackVisualPose(displayPose, targetPose, blend);
    }

    private double currentAttackVisualPhase() {
        if (isGroundAttackPending()) {
            return Math.clamp(pendingGroundAttackFrames / (double) GROUND_SMASH_HOLD_THRESHOLD_FRAMES, 0.12, 0.88);
        }
        if (isChargingAttack()) {
            return 0.18 + 0.82 * attackChargeRatio(attackChargeFrames);
        }
        if (attackAnimationTimer > 0) {
            double timerPhase = 0.5 + 0.5 * Math.sin(attackAnimationTimer * 0.72);
            return 0.45 + 0.55 * timerPhase;
        }
        return 0.0;
    }

    private double dodgeVisualPhase(int timer, int totalFrames) {
        if (timer <= 0 || totalFrames <= 0) {
            return 0.0;
        }
        return Math.clamp(1.0 - ((timer - 1.0) / (double) totalFrames), 0.0, 1.0);
    }

    private AttackVisualPose currentDodgeVisualPose() {
        if (dodgeType != DodgeType.ROLL || dodgeTimer <= 0) {
            return null;
        }
        double dir = dodgeDirection == 0 ? (facingRight ? 1.0 : -1.0) : Math.signum(dodgeDirection);
        double phase = dodgeVisualPhase(dodgeTimer, ROLL_DODGE_FRAMES);
        double travelLift = -4.0 * Math.sin(phase * Math.PI);
        double spinDegrees = dir * 540.0 * phase;
        return new AttackVisualPose(
                dir * 3.0 * Math.sin(phase * Math.PI),
                travelLift,
                spinDegrees,
                facingRight ? 0.0 : Math.PI,
                0.0,
                0.0,
                0.0,
                1.0,
                0.0,
                0.98,
                0.95
        );
    }

    private AttackVisualPose currentTargetAttackVisualPose() {
        if (pigeonSpecialPoseActive()) {
            return currentPigeonSpecialPose();
        }
        if (raptorSpecialPoseActive()) {
            return currentRaptorSpecialPose();
        }
        AttackVisualPose dodgePose = currentDodgeVisualPose();
        if (dodgePose != null) {
            return dodgePose;
        }
        NormalAttackVariant variant = currentDisplayedAttackVariant();
        double dir = facingRight ? 1.0 : -1.0;
        if (variant == null) {
            return new AttackVisualPose(0.0, 0.0, 0.0, facingRight ? 0.0 : Math.PI,
                    0.0, 0.0, 0.0, 1.0, 0.0, 1.0, 1.0);
        }

        double phase = currentAttackVisualPhase();
        return switch (variant) {
            case NEUTRAL -> new AttackVisualPose(dir * 3.0 * phase, -2.0 * phase, dir * 4.0 * phase,
                    facingRight ? 0.0 : Math.PI,
                    4.0 * phase, -1.5 * phase, 3.5 * phase, 1.04 + 0.06 * phase,
                    dir * 3.0 * phase, 1.0 + 0.02 * phase, 1.0);
            case SIDE_TILT -> new AttackVisualPose(dir * (7.0 + 5.0 * phase), -2.0 * phase, dir * (5.0 + 4.0 * phase),
                    facingRight ? -0.04 : Math.PI + 0.04,
                    8.0 * phase, -3.0 * phase, 7.0 * phase, 1.06 + 0.08 * phase,
                    dir * 3.0 * phase, 1.03 + 0.03 * phase, 0.98);
            case UP_TILT -> new AttackVisualPose(dir * 1.0 * phase, -7.0 * phase, dir * 3.0 * phase,
                    normalizeAngleRadians(-Math.PI / 2.0 + dir * 0.18),
                    8.0 * phase, -9.0 * phase, 5.0 * phase, 1.03 + 0.08 * phase,
                    -16.0 * phase, 0.99, 1.03 + 0.03 * phase);
            case DOWN_TILT -> new AttackVisualPose(dir * 4.0 * phase, 5.0 * phase, dir * 5.0 * phase,
                    normalizeAngleRadians(Math.PI / 2.0 - dir * 0.18),
                    7.0 * phase, 7.0 * phase, 7.0 * phase, 1.05 + 0.10 * phase,
                    14.0 * phase, 1.01, 0.96);
            case SIDE_SMASH -> new AttackVisualPose(dir * (10.0 + 8.0 * phase), -4.0 * phase, dir * (8.0 + 7.0 * phase),
                    facingRight ? -0.06 : Math.PI + 0.06,
                    12.0 * phase, -6.0 * phase, 12.0 * phase, 1.12 + 0.16 * phase,
                    dir * 5.0 * phase, 1.05 + 0.04 * phase, 0.96);
            case UP_SMASH -> new AttackVisualPose(dir * 1.5 * phase, -10.0 * phase, dir * 5.0 * phase,
                    normalizeAngleRadians(-Math.PI / 2.0 + dir * 0.28),
                    11.0 * phase, -14.0 * phase, 7.0 * phase, 1.06 + 0.12 * phase,
                    -26.0 * phase, 0.98, 1.05 + 0.04 * phase);
            case DOWN_SMASH -> new AttackVisualPose(dir * 6.0 * phase, 8.0 * phase, dir * 8.0 * phase,
                    normalizeAngleRadians(Math.PI / 2.0 - dir * 0.24),
                    10.0 * phase, 10.0 * phase, 10.0 * phase, 1.1 + 0.15 * phase,
                    22.0 * phase, 1.02, 0.94);
            case NEUTRAL_AIR -> new AttackVisualPose(dir * 4.0 * phase, -5.0 * phase, dir * 6.0 * phase,
                    facingRight ? 0.0 : Math.PI,
                    5.0 * phase, -4.0 * phase, 4.0 * phase, 1.06 + 0.08 * phase,
                    dir * 4.0 * phase, 1.0 + 0.03 * phase, 0.98);
            case FORWARD_AIR -> new AttackVisualPose(dir * (12.0 + 10.0 * phase), -6.0 * phase, dir * (10.0 + 8.0 * phase),
                    facingRight ? -0.12 : Math.PI + 0.12,
                    15.0 * phase, -7.0 * phase, 13.0 * phase, 1.14 + 0.18 * phase,
                    dir * 8.0 * phase, 1.07 + 0.05 * phase, 0.95);
            case BACK_AIR -> new AttackVisualPose(-dir * (6.0 + 8.0 * phase), -4.0 * phase, -dir * (14.0 + 10.0 * phase),
                    normalizeAngleRadians((facingRight ? Math.PI : 0.0) + dir * 0.06),
                    12.0 * phase, -3.0 * phase, 11.0 * phase, 1.12 + 0.18 * phase,
                    -dir * 12.0 * phase, 1.04 + 0.04 * phase, 0.96);
            case UP_AIR -> new AttackVisualPose(dir * 1.0 * phase, -12.0 * phase, dir * 6.0 * phase,
                    normalizeAngleRadians(-Math.PI / 2.0 + dir * 0.18),
                    12.0 * phase, -16.0 * phase, 8.0 * phase, 1.08 + 0.14 * phase,
                    -34.0 * phase, 0.98, 1.07 + 0.04 * phase);
            case DOWN_AIR -> new AttackVisualPose(dir * 4.0 * phase, 12.0 * phase, dir * 12.0 * phase,
                    normalizeAngleRadians(Math.PI / 2.0 - dir * 0.12),
                    13.0 * phase, 15.0 * phase, 12.0 * phase, 1.12 + 0.18 * phase,
                    34.0 * phase, 1.03 + 0.02 * phase, 0.92);
        };
    }

    private AttackVisualPose currentAttackVisualPose() {
        if (displayPose == null) {
            displayPose = currentTargetAttackVisualPose();
        }
        return displayPose;
    }

    private static void rotateAround(GraphicsContext g, double centerX, double centerY, double degrees) {
        if (Math.abs(degrees) <= 0.001) {
            return;
        }
        g.translate(centerX, centerY);
        g.rotate(degrees);
        g.translate(-centerX, -centerY);
    }

    private void applyAttackBodyPose(GraphicsContext g, double drawSize, AttackVisualPose pose) {
        if (pose == null) {
            return;
        }
        if (Math.abs(pose.translateX()) > 0.001 || Math.abs(pose.translateY()) > 0.001) {
            g.translate(pose.translateX(), pose.translateY());
        }
        rotateAround(g, x + drawSize * 0.5, y + drawSize * 0.5, pose.bodyRotationDegrees());
    }

    private HeadPose standardHeadPose(AttackVisualPose pose) {
        double s = sizeMultiplier;
        double bodyCenterX = x + 40.0 * s;
        double bodyCenterY = y + 40.0 * s;
        double aimAngle = pose == null ? (facingRight ? 0.0 : Math.PI) : pose.aimAngleRadians();
        double headReach = 35.0 * s + (pose == null ? 0.0 : pose.headReachBonus() * s);
        double verticalReach = 8.0 * s + Math.abs(Math.sin(aimAngle)) * 14.0 * s;
        double centerX = bodyCenterX + Math.cos(aimAngle) * headReach;
        double centerY = bodyCenterY + Math.sin(aimAngle) * verticalReach + (pose == null ? 0.0 : pose.headLift() * s);
        return new HeadPose(centerX, centerY, aimAngle);
    }

    private HeadPose currentHeadPose() {
        return standardHeadPose(currentAttackVisualPose());
    }

    private void drawPigeonSpecialFx(GraphicsContext g, double drawSize) {
        if (type != BirdGame3.BirdType.PIGEON) {
            return;
        }
        double s = sizeMultiplier;
        double centerX = x + drawSize * 0.5;
        double centerY = y + drawSize * 0.5;
        double dir = facingRight ? 1.0 : -1.0;

        g.save();
        g.setLineCap(StrokeLineCap.ROUND);

        if (pigeonRushTimer > 0) {
            double phase = pigeonSpecialPhase(pigeonRushTimer,
                    pigeonRushGrounded ? PIGEON_RUSH_GROUND_FRAMES : PIGEON_RUSH_AIR_FRAMES);
            double reach = (108.0 + phase * 44.0) * s;
            g.setEffect(new Glow(0.5));
            g.setStroke(Color.web("#CFD8DC").deriveColor(0, 1, 1, 0.72));
            g.setLineWidth(7.0 * s);
            for (int i = -1; i <= 1; i++) {
                double yOffset = i * 10.0 * s;
                g.strokeLine(centerX - dir * 26.0 * s, centerY + yOffset + 6.0 * s,
                        centerX + dir * reach, centerY + yOffset - 8.0 * s);
            }
            g.setStroke(Color.web("#90CAF9").deriveColor(0, 1, 1, 0.48));
            g.setLineWidth(3.0 * s);
            g.strokeArc(centerX + dir * (reach - 14.0 * s) - 32.0 * s, centerY - 26.0 * s,
                    64.0 * s, 52.0 * s,
                    facingRight ? -42 : 222, 84, ArcType.OPEN);
            g.restore();
            return;
        }

        if (pigeonFlutterTimer > 0) {
            double phase = pigeonSpecialPhase(pigeonFlutterTimer,
                    pigeonFlutterUltimate ? PIGEON_FLUTTER_ULTIMATE_FRAMES : PIGEON_FLUTTER_FRAMES);
            double rise = (56.0 + phase * 34.0) * s;
            g.setEffect(new Glow(0.56));
            g.setStroke(Color.web("#E3F2FD").deriveColor(0, 1, 1, 0.78));
            g.setLineWidth(5.0 * s);
            g.strokeLine(centerX, centerY + 18.0 * s, centerX, centerY - rise);
            g.setStroke(Color.web("#81D4FA").deriveColor(0, 1, 1, 0.52));
            g.setLineWidth(9.0 * s);
            g.strokeArc(centerX - 34.0 * s, centerY - rise * 0.80, 68.0 * s, rise * 0.92,
                    204, 132, ArcType.OPEN);
            g.strokeArc(centerX - 50.0 * s, centerY - rise * 0.56, 100.0 * s, rise * 0.74,
                    212, 116, ArcType.OPEN);
            g.restore();
            return;
        }

        if (pigeonScavengeTimer > 0) {
            double phase = pigeonSpecialPhase(pigeonScavengeTimer,
                    pigeonScavengeAirborne ? PIGEON_SCAVENGE_AIR_FRAMES : PIGEON_SCAVENGE_GROUND_FRAMES);
            g.setEffect(new Glow(0.32));
            if (pigeonScavengeAirborne) {
                double drop = (34.0 + phase * 42.0) * s;
                g.setStroke(Color.web("#8D6E63").deriveColor(0, 1, 1, 0.72));
                g.setLineWidth(6.0 * s);
                g.strokeLine(centerX, centerY + 12.0 * s, centerX, centerY + drop);
                g.setStroke(Color.web("#D7CCC8").deriveColor(0, 1, 1, 0.46));
                g.setLineWidth(3.0 * s);
                g.strokeLine(centerX - 16.0 * s, centerY + 18.0 * s, centerX, centerY + drop);
                g.strokeLine(centerX + 16.0 * s, centerY + 18.0 * s, centerX, centerY + drop);
            } else {
                double groundY = bodyBottomY() - 4.0 * s;
                double pulse = 0.5 + 0.5 * Math.sin((PIGEON_SCAVENGE_GROUND_FRAMES - pigeonScavengeTimer) * 0.55);
                g.setStroke(Color.web("#8D6E63").deriveColor(0, 1, 1, 0.54 + 0.16 * pulse));
                g.setLineWidth(5.0 * s);
                g.strokeArc(centerX - 34.0 * s, groundY - 14.0 * s, 68.0 * s, 30.0 * s,
                        198, 144, ArcType.OPEN);
                g.setStroke(Color.web("#D7CCC8").deriveColor(0, 1, 1, 0.36 + 0.10 * pulse));
                g.setLineWidth(2.4 * s);
                g.strokeLine(centerX - 8.0 * s, groundY - 2.0 * s, centerX - 22.0 * s, groundY - 16.0 * s);
                g.strokeLine(centerX + 2.0 * s, groundY - s, centerX + 18.0 * s, groundY - 13.0 * s);
                g.strokeLine(centerX + 10.0 * s, groundY + s, centerX + 26.0 * s, groundY - 9.0 * s);
            }
            g.restore();
            return;
        }

        if (pigeonFeatherBurstTimer > 0) {
            double phase = pigeonSpecialPhase(pigeonFeatherBurstTimer, PIGEON_NEUTRAL_BURST_FRAMES);
            double baseX = centerX + dir * 20.0 * s;
            double travel = (26.0 + phase * 42.0) * s;
            double[] laneOffsets = {-18.0, 0.0, 18.0};
            g.setEffect(new Glow(0.4));
            g.setStroke((pigeonFeatherBurstUltimate ? Color.GOLD : Color.WHITE).deriveColor(0, 1, 1, 0.82));
            g.setLineWidth(3.0 * s);
            for (int i = 0; i < laneOffsets.length; i++) {
                double laneY = centerY + laneOffsets[i] * s;
                double tipX = baseX + dir * (travel + i * 10.0 * s);
                g.strokeLine(tipX - dir * 12.0 * s, laneY, tipX, laneY - 6.0 * s);
                g.strokeLine(tipX - dir * 12.0 * s, laneY, tipX, laneY + 6.0 * s);
                g.strokeLine(baseX - dir * 8.0 * s, laneY, tipX - dir * 5.0 * s, laneY);
            }
            g.restore();
            return;
        }

        g.restore();
    }

    private void drawDirectionalAttackFx(GraphicsContext g, double drawSize) {
        NormalAttackVariant variant = currentDisplayedAttackVariant();
        if (variant == null) {
            return;
        }

        double s = sizeMultiplier;
        double centerX = x + drawSize * 0.5;
        double centerY = y + drawSize * 0.5;
        boolean charging = isChargingAttack();
        double phase = charging
                ? 0.35 + 0.65 * attackChargeRatio(attackChargeFrames)
                : 0.45 + 0.55 * Math.sin(Math.max(1, attackAnimationTimer) * 0.72);
        double alpha = charging ? 0.34 + 0.26 * phase : 0.46 + 0.26 * phase;
        double glowAlpha = charging ? 0.14 + 0.12 * phase : 0.20 + 0.14 * phase;
        double dir = facingRight ? 1.0 : -1.0;

        g.save();
        g.setEffect(new Glow(charging ? 0.45 : 0.65));
        g.setLineCap(StrokeLineCap.ROUND);

        switch (variant) {
            case NEUTRAL, NEUTRAL_AIR -> {
                double ringRadius = (drawSize * 0.42) + (charging ? 8.0 : 14.0) * s + phase * 10.0 * s;
                g.setStroke(Color.WHITE.deriveColor(0, 1, 1, alpha));
                g.setLineWidth((charging ? 2.2 : 3.2) * s);
                g.strokeOval(centerX - ringRadius, centerY - ringRadius, ringRadius * 2, ringRadius * 2);
                g.setStroke(Color.web("#FFD54F").deriveColor(0, 1, 1, glowAlpha + 0.12));
                g.setLineWidth((charging ? 4.8 : 6.8) * s);
                g.strokeArc(centerX - ringRadius * 0.78, centerY - ringRadius * 0.78,
                        ringRadius * 1.56, ringRadius * 1.56,
                        40 + phase * 45, 220, ArcType.OPEN);
            }
            case SIDE_TILT, SIDE_SMASH, FORWARD_AIR -> {
                boolean smash = variant == NormalAttackVariant.SIDE_SMASH;
                double reach = (smash ? 64.0 : 50.0) * s + phase * (smash ? 18.0 : 12.0) * s;
                double attackY = centerY + (charging ? -4.0 : 0.0) * s;
                double startX = centerX + dir * (18.0 * s);
                double endX = centerX + dir * (reach + 26.0 * s);
                g.setStroke(Color.WHITE.deriveColor(0, 1, 1, alpha));
                g.setLineWidth((charging ? 4.0 : (smash ? 6.2 : 4.8)) * s);
                g.strokeLine(startX, attackY, endX, attackY - 8.0 * s);
                g.setStroke(Color.web("#FFB74D").deriveColor(0, 1, 1, glowAlpha + 0.18));
                g.setLineWidth((charging ? 10.0 : (smash ? 13.0 : 9.0)) * s);
                g.strokeLine(startX - dir * 6.0 * s, attackY + 5.0 * s, endX, attackY - 14.0 * s);
                g.setStroke(Color.web("#FFF59D").deriveColor(0, 1, 1, glowAlpha));
                g.setLineWidth((charging ? 2.2 : (smash ? 3.0 : 2.2)) * s);
                g.strokeArc(centerX + dir * 18.0 * s - 46.0 * s, centerY - 30.0 * s,
                        92.0 * s, 72.0 * s,
                        facingRight ? -48 : 228, 76, ArcType.OPEN);
            }
            case BACK_AIR -> {
                double reach = (54.0 + phase * 18.0) * s;
                double attackDir = -dir;
                double attackY = centerY - 3.0 * s;
                double startX = centerX + attackDir * (14.0 * s);
                double endX = centerX + attackDir * (reach + 22.0 * s);
                g.setStroke(Color.WHITE.deriveColor(0, 1, 1, alpha));
                g.setLineWidth(6.0 * s);
                g.strokeLine(startX, attackY, endX, attackY - 12.0 * s);
                g.setStroke(Color.web("#81D4FA").deriveColor(0, 1, 1, glowAlpha + 0.2));
                g.setLineWidth(13.0 * s);
                g.strokeLine(startX - attackDir * 4.0 * s, attackY + 6.0 * s, endX, attackY - 18.0 * s);
                g.setStroke(Color.web("#E1F5FE").deriveColor(0, 1, 1, glowAlpha));
                g.setLineWidth(3.0 * s);
                g.strokeArc(centerX + attackDir * 8.0 * s - 44.0 * s, centerY - 28.0 * s,
                        88.0 * s, 76.0 * s,
                        attackDir > 0 ? -18 : 198, 88, ArcType.OPEN);
            }
            case UP_TILT, UP_SMASH, UP_AIR -> {
                boolean smash = variant == NormalAttackVariant.UP_SMASH;
                double rise = (smash ? 64.0 : 52.0) * s + phase * (smash ? 20.0 : 12.0) * s;
                double arcW = (smash ? 56.0 : 44.0) * s + phase * (smash ? 10.0 : 8.0) * s;
                double arcH = (smash ? 78.0 : 64.0) * s + phase * (smash ? 16.0 : 10.0) * s;
                g.setStroke(Color.WHITE.deriveColor(0, 1, 1, alpha));
                g.setLineWidth((charging ? 4.4 : (smash ? 6.0 : 4.6)) * s);
                g.strokeLine(centerX, centerY - 8.0 * s, centerX, centerY - rise);
                g.setStroke(Color.web("#A5D6A7").deriveColor(0, 1, 1, glowAlpha + 0.22));
                g.setLineWidth((charging ? 9.0 : (smash ? 12.0 : 8.6)) * s);
                g.strokeArc(centerX - arcW * 0.5, centerY - rise,
                        arcW, arcH,
                        205, 130, ArcType.OPEN);
                g.setStroke(Color.web("#E8F5E9").deriveColor(0, 1, 1, glowAlpha));
                g.setLineWidth((charging ? 2.2 : (smash ? 3.0 : 2.2)) * s);
                g.strokeArc(centerX - arcW * 0.72, centerY - rise - 4.0 * s,
                        arcW * 1.44, arcH * 1.04,
                        198, 144, ArcType.OPEN);
            }
            case DOWN_TILT, DOWN_SMASH, DOWN_AIR -> {
                boolean smash = variant == NormalAttackVariant.DOWN_SMASH;
                double drop = (smash ? 62.0 : 50.0) * s + phase * (smash ? 20.0 : 12.0) * s;
                double spread = (smash ? 38.0 : 28.0) * s + phase * (smash ? 12.0 : 8.0) * s;
                g.setStroke(Color.WHITE.deriveColor(0, 1, 1, alpha));
                g.setLineWidth((charging ? 4.4 : (smash ? 6.0 : 4.6)) * s);
                g.strokeLine(centerX, centerY + 6.0 * s, centerX, centerY + drop);
                g.setStroke(Color.web("#EF9A9A").deriveColor(0, 1, 1, glowAlpha + 0.22));
                g.setLineWidth((charging ? 10.0 : (smash ? 13.0 : 8.6)) * s);
                g.strokeLine(centerX - spread, centerY + 18.0 * s, centerX, centerY + drop);
                g.strokeLine(centerX + spread, centerY + 18.0 * s, centerX, centerY + drop);
                g.setStroke(Color.web("#FFCCBC").deriveColor(0, 1, 1, glowAlpha));
                g.setLineWidth((charging ? 2.2 : (smash ? 3.0 : 2.2)) * s);
                g.strokeArc(centerX - 38.0 * s, centerY + 12.0 * s, 76.0 * s, 66.0 * s,
                        18, 144, ArcType.OPEN);
            }
        }

        g.restore();
    }

    public void draw(GraphicsContext g) {
        double drawSize = 80 * sizeMultiplier;
        boolean airborne = !isOnGround();
        AttackVisualPose attackPose = currentAttackVisualPose();

        drawBlockingShield(g, drawSize);
        drawTaunt(g);
        drawCooldownFlash(g);
        drawAttackChargeFx(g, drawSize);
        drawRageBuff(g, drawSize);
        drawThermalBuff(g, drawSize);
        drawPenguinIceBuff(g, drawSize);
        drawHummingbirdFrenzy(g, drawSize);
        if (!suppressSelectEffects) {
            drawPhoenixAura(g, drawSize);
        }
        drawNeonBuff(g, drawSize);
        drawUltimateFx(g, drawSize);
        drawRoadrunnerSandstormAura(g, drawSize);
        drawBatEcho(g, drawSize);
        if (!suppressSelectEffects) {
            drawOpiumBirdEffects(g, drawSize);
        }
        if (!suppressSelectEffects) {
            drawTitmouseSpecial(g);
        }
        if (!suppressSelectEffects) {
            drawEagleSoaring(g, airborne, drawSize);
        }
        drawRazorbillBladestorm(g, drawSize);
        drawDodgeAura(g, drawSize);
        drawNullRockShield(g, drawSize);
        drawSpecialCooldown(g);
        drawLounge(g);
        g.save();
        applyAttackBodyPose(g, drawSize, attackPose);
        drawEagleSkin(g, drawSize);
        drawGrinchhawk(g);
        drawVulture(g, drawSize);
        drawBodyAndEyes(g, drawSize, attackPose);
        drawRooster(g, drawSize);
        drawHeisenbirdAccessories(g);
        drawCitySkin(g);
        drawNoirSkin(g);
        drawFreemanSkin(g);
        drawBeaconSkin(g, drawSize);
        drawClassicSkinAccent(g, drawSize);
        drawSpecialSkinAccent(g, drawSize);
        drawBeak(g, attackPose);
        drawPelican(g);
        g.restore();
        drawPigeonSpecialFx(g, drawSize);
        drawRaptorSpecialFx(g, drawSize);
        drawDirectionalAttackFx(g, drawSize);
        drawStunEffect(g, drawSize);
        drawVineGrapple(g);
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

    private void drawBlockingShield(GraphicsContext g, double drawSize) {
        if (isBlocking) {
            double s = sizeMultiplier;
            double birdCenterX = bodyCenterX();
            double birdCenterY = bodyCenterY();
            double durability = shieldDurabilityRatio();
            double shieldScale = shieldVisualScale();
            double dir = facingRight ? 1.0 : -1.0;
            double shellHeight = (drawSize + 58 * s) * shieldScale;
            double shellWidth = shellHeight * 0.76;
            double shellX = birdCenterX - shellWidth / 2.0 + dir * (18 * s + (1.0 - durability) * 6 * s);
            double shellY = birdCenterY - shellHeight / 2.0;
            double haloWidth = shellWidth + 18 * s;
            double haloHeight = shellHeight + 14 * s;
            double rimAngle = facingRight ? -70 : 110;
            double pulse = 0.6 + 0.4 * Math.sin(System.currentTimeMillis() / 200.0);
            Color base = durability < 0.28 ? Color.web("#EF5350") : Color.web("#64B5F6");
            if (parryWindowFrames > 0) {
                base = Color.web("#D0F8FF");
            } else if (shieldStunFrames > 0) {
                base = base.brighter();
            }

            g.setFill(base.deriveColor(0, 1, 1, 0.14 + pulse * 0.12));
            g.fillOval(shellX - 10 * s, shellY - 7 * s, haloWidth, haloHeight);

            g.setFill(base.deriveColor(0, 1, 1, 0.24 + pulse * 0.24));
            g.fillOval(shellX, shellY, shellWidth, shellHeight);

            g.setFill(Color.WHITE.deriveColor(0, 1, 1, parryWindowFrames > 0 ? 0.16 : 0.09 + pulse * 0.06));
            g.fillOval(shellX - dir * shellWidth * 0.14, shellY + shellHeight * 0.08, shellWidth * 0.68, shellHeight * 0.72);

            double tipBaseX = facingRight ? shellX + shellWidth * 0.92 : shellX + shellWidth * 0.08;
            double tipX = tipBaseX + dir * 16 * s;
            g.setFill(base.deriveColor(0, 1, 1, 0.28 + pulse * 0.22));
            g.fillPolygon(
                    new double[]{tipBaseX, tipX, tipBaseX},
                    new double[]{birdCenterY - 18 * s, birdCenterY, birdCenterY + 18 * s},
                    3
            );

            g.setStroke(base.deriveColor(0, 1, 1, 0.75 + pulse * 0.20));
            g.setLineWidth((3.0 + (1.0 - durability) * 2.2) * s);
            g.strokeArc(shellX, shellY, shellWidth, shellHeight, rimAngle, 140, ArcType.OPEN);

            g.setStroke(Color.WHITE.deriveColor(0, 1, 1, parryWindowFrames > 0 ? 0.55 : 0.25));
            g.setLineWidth(1.4 * s);
            for (int i = 0; i < 3; i++) {
                double inset = (8 + i * 9) * s;
                g.strokeArc(
                        shellX + inset * 0.55,
                        shellY + inset,
                        shellWidth - inset * 1.1,
                        shellHeight - inset * 2.0,
                        rimAngle + 2,
                        136,
                        ArcType.OPEN
                );
            }

            if (durability < 0.46) {
                g.setStroke(Color.WHITE.deriveColor(0, 1, 1, 0.22 + (0.46 - durability) * 0.9));
                g.setLineWidth(1.2 * s);
                double crackX = birdCenterX + dir * shellWidth * 0.12;
                g.strokeLine(crackX, birdCenterY - 20 * s, crackX - dir * 9 * s, birdCenterY - 4 * s);
                g.strokeLine(crackX - dir * 9 * s, birdCenterY - 4 * s, crackX + dir * 4 * s, birdCenterY + 10 * s);
                g.strokeLine(crackX + dir * 4 * s, birdCenterY + 10 * s, crackX - dir * 7 * s, birdCenterY + 22 * s);
            }

            if (Math.random() < 0.18 + (shieldStunFrames > 0 ? 0.12 : 0.0)) {
                double particleAngle = (facingRight ? 0.0 : Math.PI) + (Math.random() - 0.5) * Math.PI * 0.9;
                double px = birdCenterX + dir * shellWidth * 0.12 + Math.cos(particleAngle) * shellWidth * 0.42;
                double py = birdCenterY + Math.sin(particleAngle) * shellHeight * 0.46;
                game.particles.add(new Particle(
                        px,
                        py,
                        Math.cos(particleAngle) * 1.9,
                        Math.sin(particleAngle) * 1.7 - 0.7,
                        base.deriveColor(0, 1, 1, 0.75)
                ));
            }
        }
    }

    private void drawStunStar(GraphicsContext g, double centerX, double centerY, double radius, Color fill, Color stroke) {
        double[] xs = new double[]{
                centerX,
                centerX + radius * 0.32,
                centerX + radius,
                centerX + radius * 0.32,
                centerX,
                centerX - radius * 0.32,
                centerX - radius,
                centerX - radius * 0.32
        };
        double[] ys = new double[]{
                centerY - radius,
                centerY - radius * 0.32,
                centerY,
                centerY + radius * 0.32,
                centerY + radius,
                centerY + radius * 0.32,
                centerY,
                centerY - radius * 0.32
        };
        g.setFill(fill);
        g.fillPolygon(xs, ys, xs.length);
        g.setStroke(stroke);
        g.setLineWidth(Math.max(1.0, 1.2 * sizeMultiplier));
        g.strokePolygon(xs, ys, xs.length);
    }

    private void drawStunEyeMark(GraphicsContext g, double centerX, double centerY, double radius) {
        g.setStroke(Color.WHITE.deriveColor(0, 1, 1, 0.92));
        g.setLineWidth(Math.max(1.4, 2.0 * sizeMultiplier));
        g.strokeLine(centerX - radius, centerY - radius, centerX + radius, centerY + radius);
        g.strokeLine(centerX - radius, centerY + radius, centerX + radius, centerY - radius);
        g.setStroke(Color.BLACK.deriveColor(0, 1, 1, 0.65));
        g.setLineWidth(Math.max(0.8, sizeMultiplier));
        g.strokeLine(centerX - radius * 0.9, centerY - radius * 0.9, centerX + radius * 0.9, centerY + radius * 0.9);
        g.strokeLine(centerX - radius * 0.9, centerY + radius * 0.9, centerX + radius * 0.9, centerY - radius * 0.9);
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

    private void drawUltimateFx(GraphicsContext g, double drawSize) {
        if (ultimateFxTimer <= 0) return;
        double s = sizeMultiplier;
        double t = (ULTIMATE_FX_FRAMES - ultimateFxTimer);
        double pulse = 0.6 + 0.4 * Math.sin(t * 0.6);
        double cx = x + 40;
        double cy = y + 40;
        double radius = (drawSize / 2.0) + 18 * s + pulse * 18 * s;
        Color gold = Color.GOLD.deriveColor(0, 1, 1, 0.9);
        Color purple = Color.web("#7E57C2").deriveColor(0, 1, 1, 0.7);
        Color accent = type.color.deriveColor(0, 1, 1, 0.85);

        g.setFill(gold.deriveColor(0, 1, 1, 0.12));
        g.fillOval(cx - radius, cy - radius, radius * 2, radius * 2);

        g.setStroke(gold.deriveColor(0, 1, 1, 0.85));
        g.setLineWidth(3.5);
        g.strokeOval(cx - radius, cy - radius, radius * 2, radius * 2);

        g.setStroke(purple.deriveColor(0, 1, 1, 0.45));
        g.setLineWidth(2.0);
        double outer = radius + 14 * s;
        g.strokeOval(cx - outer, cy - outer, outer * 2, outer * 2);

        switch (type) {
            case PIGEON -> {
                drawRoyalCrown(g, cx, y - 16 * s, 34 * s, 16 * s, gold, Color.web("#FFF59D"));
                g.setStroke(Color.WHITE.deriveColor(0, 1, 1, 0.7));
                g.setLineWidth(2.2);
                g.strokeOval(cx - 18 * s, y - 28 * s, 36 * s, 12 * s);
            }
            case EAGLE -> {
                g.setStroke(gold);
                g.setLineWidth(3.2);
                g.strokeArc(cx - 80 * s, cy - 50 * s, 160 * s, 90 * s, 200, 140, ArcType.OPEN);
                g.strokeArc(cx - 80 * s, cy - 50 * s, 160 * s, 90 * s, 0, 140, ArcType.OPEN);
            }
            case FALCON -> {
                g.setStroke(gold);
                g.setLineWidth(3.0);
                int dir = facingRight ? 1 : -1;
                g.strokeLine(cx, cy, cx + dir * 70 * s, cy - 18 * s);
                g.setLineWidth(2.0);
                g.strokeLine(cx - dir * 10 * s, cy + 8 * s, cx + dir * 56 * s, cy - 8 * s);
            }
            case PHOENIX -> {
                g.setStroke(Color.ORANGERED.brighter());
                g.setLineWidth(2.6);
                for (int i = 0; i < 10; i++) {
                    double ang = i / 10.0 * Math.PI * 2;
                    double len = (18 + (i % 3) * 6) * s;
                    g.strokeLine(cx, cy, cx + Math.cos(ang) * len, cy + Math.sin(ang) * len);
                }
            }
            case HUMMINGBIRD -> {
                g.setStroke(Color.GOLD.brighter());
                g.setLineWidth(2.0);
                for (int i = 0; i < 8; i++) {
                    double ang = i / 8.0 * Math.PI * 2;
                    g.strokeLine(cx, cy - 6 * s, cx + Math.cos(ang) * 22 * s, cy - 6 * s + Math.sin(ang) * 22 * s);
                }
            }
            case TURKEY -> {
                g.setStroke(gold);
                g.setLineWidth(3.0);
                g.strokeOval(cx - 90 * s, y + drawSize - 6 * s, 180 * s, 26 * s);
                g.setLineWidth(2.0);
                g.strokeOval(cx - 70 * s, y + drawSize + 4 * s, 140 * s, 18 * s);
            }
            case ROADRUNNER -> {
                Color sand = Color.web("#F0C06A").deriveColor(0, 1, 1, 0.75);
                g.setFill(sand.deriveColor(0, 1, 1, 0.18));
                g.fillOval(cx - 72 * s, cy - 32 * s, 144 * s, 64 * s);
                g.setStroke(sand);
                g.setLineWidth(3.0);
                g.strokeArc(cx - 88 * s, cy - 42 * s, 176 * s, 92 * s, 196, 148, ArcType.OPEN);
                g.strokeArc(cx - 94 * s, cy - 28 * s, 188 * s, 70 * s, 14, 158, ArcType.OPEN);
                int dir = facingRight ? 1 : -1;
                g.setStroke(Color.web("#2E5AAC").deriveColor(0, 1, 1, 0.82));
                g.setLineWidth(2.4);
                g.strokeLine(cx - dir * 4 * s, y - 6 * s, cx - dir * 20 * s, y - 28 * s);
                g.strokeLine(cx + dir * 6 * s, y - 2 * s, cx - dir * 10 * s, y - 24 * s);
            }
            case ROOSTER -> {
                g.setStroke(gold.brighter());
                g.setLineWidth(2.8);
                double combW = 44 * s;
                double combH = 16 * s;
                double topY = y - 18 * s;
                g.strokeArc(cx - combW / 2, topY, combW, combH, 0, 180, ArcType.OPEN);
                g.strokeArc(cx - combW / 2 + 10 * s, topY - 6 * s, combW * 0.7, combH * 0.7, 0, 180, ArcType.OPEN);
                g.setLineWidth(2.2);
                g.strokeLine(cx - 30 * s, cy + 24 * s, cx + 30 * s, cy + 24 * s);
            }
            case PENGUIN -> {
                double w = 26 * s;
                double h = 18 * s;
                g.setFill(Color.CYAN.deriveColor(0, 1, 1, 0.7));
                g.fillPolygon(
                        new double[]{cx - w / 2, cx, cx + w / 2, cx},
                        new double[]{y - 10 * s, y - 10 * s - h, y - 10 * s, y - 10 * s + h * 0.4},
                        4
                );
                drawRoyalCrown(g, cx, y - 20 * s, 28 * s, 14 * s, gold, Color.web("#B3E5FC"));
            }
            case SHOEBILL -> {
                g.setFill(purple.deriveColor(0, 1, 1, 0.22));
                g.fillRect(cx - 6 * s, y - 18 * s, 12 * s, drawSize + 40 * s);
                g.setStroke(gold);
                g.setLineWidth(2.2);
                g.strokeLine(cx, y - 10 * s, cx, y + drawSize + 16 * s);
            }
            case MOCKINGBIRD -> {
                g.setStroke(gold);
                g.setLineWidth(2.8);
                g.strokePolygon(
                        new double[]{cx, cx + 60 * s, cx, cx - 60 * s},
                        new double[]{cy - 60 * s, cy, cy + 60 * s, cy},
                        4
                );
                drawRoyalCrown(g, cx, y - 18 * s, 30 * s, 14 * s, gold, Color.web("#E1BEE7"));
            }
            case RAZORBILL -> {
                g.setStroke(accent);
                g.setLineWidth(2.4);
                for (int i = 0; i < 6; i++) {
                    double ang = i / 6.0 * Math.PI * 2;
                    double len = 26 * s;
                    g.strokeLine(cx, cy, cx + Math.cos(ang) * len, cy + Math.sin(ang) * len);
                }
            }
            case GRINCHHAWK -> {
                g.setStroke(Color.web("#9CCC65"));
                g.setLineWidth(2.6);
                for (int i = -2; i <= 2; i++) {
                    g.strokeLine(cx, y - 4 * s, cx + i * 10 * s, y - 28 * s);
                }
            }
            case VULTURE -> {
                g.setFill(Color.rgb(20, 0, 20, 0.25));
                g.fillOval(cx - 40 * s, cy - 40 * s, 80 * s, 80 * s);
                drawRoyalCrown(g, cx, y - 14 * s, 26 * s, 12 * s, gold, Color.web("#FFE082"));
            }
            case OPIUMBIRD -> {
                g.setFill(Color.PURPLE.deriveColor(0, 1, 1, 0.22));
                g.fillOval(cx - 70 * s, cy - 60 * s, 140 * s, 120 * s);
                g.setStroke(gold);
                g.setLineWidth(2.0);
                g.strokeOval(cx - 36 * s, cy - 30 * s, 72 * s, 60 * s);
            }
            case HEISENBIRD -> {
                g.setStroke(Color.web("#B3E5FC"));
                g.setLineWidth(2.4);
                g.strokePolygon(
                        new double[]{cx - 40 * s, cx, cx + 40 * s, cx + 20 * s, cx - 20 * s},
                        new double[]{cy, cy - 36 * s, cy, cy + 36 * s, cy + 36 * s},
                        5
                );
            }
            case TITMOUSE -> {
                g.setStroke(gold);
                g.setLineWidth(2.6);
                g.strokePolyline(
                        new double[]{cx - 30 * s, cx - 8 * s, cx + 10 * s, cx + 34 * s},
                        new double[]{cy - 14 * s, cy + 8 * s, cy - 6 * s, cy + 12 * s},
                        4
                );
            }
            case BAT -> {
                g.setStroke(Color.GOLD.deriveColor(0, 1, 1, 0.75));
                g.setLineWidth(2.4);
                g.strokeOval(cx - 70 * s, cy - 50 * s, 140 * s, 100 * s);
                g.strokeOval(cx - 90 * s, cy - 65 * s, 180 * s, 130 * s);
            }
            case PELICAN -> {
                drawRoyalCrown(g, cx, y - 20 * s, 44 * s, 20 * s, gold, Color.web("#FFF9C4"));
                g.setStroke(gold);
                g.setLineWidth(2.2);
                g.strokeLine(cx - 50 * s, cy + 10 * s, cx + 50 * s, cy + 6 * s);
            }
            case RAVEN -> {
                g.setFill(Color.web("#1C1F26", 0.28));
                g.fillOval(cx - 60 * s, cy - 60 * s, 120 * s, 120 * s);
                g.setStroke(gold);
                g.setLineWidth(2.2);
                g.strokeArc(cx - 45 * s, cy - 35 * s, 90 * s, 70 * s, 200, 140, ArcType.OPEN);
            }
        }
    }

    private void drawRoadrunnerSandstormAura(GraphicsContext g, double drawSize) {
        if (!roadrunnerSandstormActive()) return;

        double s = sizeMultiplier;
        double cx = bodyCenterX();
        double cy = bodyCenterY();
        double intensity = Math.clamp(roadrunnerSandstormTimer / (double) ROADRUNNER_SANDSTORM_FRAMES, 0.35, 1.0);
        double pulse = 0.55 + 0.45 * Math.sin(roadrunnerSandstormTimer * 0.18);
        double halo = drawSize + 120 * s + pulse * 54 * s;
        double haloOffset = halo / 2.0;
        Color gold = Color.GOLD.deriveColor(0, 1, 1, 0.24 + 0.14 * intensity);
        Color sand = Color.web("#E6C46F").deriveColor(0, 1, 1, 0.50 + 0.18 * intensity);
        Color white = Color.web("#FFF8E1").deriveColor(0, 1, 1, 0.24 + 0.12 * pulse);

        g.setFill(gold);
        g.fillOval(cx - haloOffset, cy - haloOffset, halo, halo);
        g.setFill(white);
        g.fillOval(cx - haloOffset * 0.82, cy - haloOffset * 0.82, halo * 0.82, halo * 0.82);

        g.setStroke(Color.GOLD.brighter().deriveColor(0, 1, 1, 0.82));
        g.setLineWidth(3.2);
        g.strokeOval(cx - haloOffset, cy - haloOffset, halo, halo);

        g.setStroke(sand);
        g.setLineWidth(2.6);
        g.strokeArc(cx - 118 * s, cy - 82 * s, 236 * s, 164 * s, 200, 145, ArcType.OPEN);
        g.strokeArc(cx - 128 * s, cy - 60 * s, 256 * s, 122 * s, 12, 156, ArcType.OPEN);

        g.setStroke(Color.web("#FFF59D").deriveColor(0, 1, 1, 0.88));
        g.setLineWidth(2.0);
        for (int i = 0; i < 8; i++) {
            double ang = -Math.PI / 2.0 + (i - 3.5) * 0.22;
            double inner = 34 * s;
            double outer = inner + 36 * s + (i % 2 == 0 ? 12 * s : 0);
            g.strokeLine(
                    cx + Math.cos(ang) * inner,
                    cy - 16 * s + Math.sin(ang) * inner,
                    cx + Math.cos(ang) * outer,
                    cy - 16 * s + Math.sin(ang) * outer
            );
        }
    }

    private void drawRoyalCrown(GraphicsContext g, double cx, double yTop, double width, double height, Color fill, Color stroke) {
        double half = width / 2.0;
        double x0 = cx - half;
        double x1 = cx - half * 0.5;
        double x2 = cx - half * 0.1;
        double x4 = cx + half * 0.1;
        double x5 = cx + half * 0.5;
        double x6 = cx + half;

        double y0 = yTop + height;
        double y1 = yTop + height * 0.25;
        double y2 = yTop + height;
        double y4 = yTop + height;
        double y5 = yTop + height * 0.25;
        double y6 = yTop + height;
        double yBase = yTop + height * 1.25;

        double[] xs = new double[]{x0, x1, x2, cx, x4, x5, x6, x6, x0};
        double[] ys = new double[]{y0, y1, y2, yTop, y4, y5, y6, yBase, yBase};

        g.setFill(fill);
        g.fillPolygon(xs, ys, xs.length);
        g.setStroke(stroke);
        g.setLineWidth(1.6);
        g.strokePolygon(xs, ys, xs.length);
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

    private void drawRaptorSpecialFx(GraphicsContext g, double drawSize) {
        if (!isRaptor()) {
            return;
        }
        boolean eagle = type == BirdGame3.BirdType.EAGLE;
        boolean skyKing = eagle && isClassicSkin;
        boolean falcon = type == BirdGame3.BirdType.FALCON;
        boolean duneFalcon = falcon && isDuneSkin;
        double s = sizeMultiplier;
        double centerX = x + drawSize * 0.5;
        double centerY = y + drawSize * 0.5;
        Color primary = eagle ? Color.web("#D4A042") : Color.web("#FF9A62");
        Color secondary = eagle ? Color.web("#FFF1A8") : Color.web("#FFE1A8");

        g.save();
        g.setLineCap(StrokeLineCap.ROUND);

        if (raptorRushTimer > 0) {
            double phase = raptorSpecialPhase(raptorRushTimer,
                    eagle
                            ? (raptorRushGrounded ? EAGLE_RUSH_GROUND_FRAMES : EAGLE_RUSH_AIR_FRAMES)
                            : (raptorRushGrounded ? FALCON_RUSH_GROUND_FRAMES : FALCON_RUSH_AIR_FRAMES));
            double dir = raptorRushDirection == 0 ? facingDirection() : raptorRushDirection;
            double reach = (eagle ? 116.0 : 138.0) * s + phase * (eagle ? 32.0 : 42.0) * s;
            g.setEffect(new Glow(eagle ? 0.38 : 0.52));
            g.setStroke(primary.deriveColor(0, 1, 1, 0.78));
            g.setLineWidth((eagle ? 7.0 : 6.0) * s);
            for (int i = -1; i <= 1; i++) {
                double yOffset = i * 11.0 * s;
                g.strokeLine(centerX - dir * 26.0 * s, centerY + yOffset,
                        centerX + dir * reach, centerY + yOffset - (eagle ? 10.0 : 18.0) * s);
            }
            g.setStroke(secondary.deriveColor(0, 1, 1, 0.65));
            g.setLineWidth(3.2 * s);
            g.strokeArc(centerX + dir * (reach - 34.0 * s) - 30.0 * s, centerY - 34.0 * s,
                    60.0 * s, 68.0 * s, dir > 0 ? -48 : 228, 96, ArcType.OPEN);
            g.strokeArc(centerX - dir * 8.0 * s - 42.0 * s, centerY - 28.0 * s,
                    84.0 * s, 56.0 * s, dir > 0 ? -16 : 196, 74, ArcType.OPEN);
            g.restore();
            return;
        }

        if (raptorClimbTimer > 0) {
            double phase = raptorSpecialPhase(raptorClimbTimer,
                    eagle
                            ? (raptorClimbUltimate ? EAGLE_CLIMB_ULTIMATE_FRAMES : EAGLE_CLIMB_FRAMES)
                            : (raptorClimbUltimate ? FALCON_CLIMB_ULTIMATE_FRAMES : FALCON_CLIMB_FRAMES));
            double rise = (eagle ? 78.0 : 66.0) * s + phase * (eagle ? 34.0 : 26.0) * s;
            g.setEffect(new Glow(eagle ? 0.32 : 0.46));
            g.setStroke(primary.deriveColor(0, 1, 1, 0.74));
            g.setLineWidth((eagle ? 5.5 : 4.5) * s);
            g.strokeLine(centerX, centerY + 18.0 * s, centerX, centerY - rise);
            g.setStroke(secondary.deriveColor(0, 1, 1, 0.58));
            g.setLineWidth(3.0 * s);
            g.strokeArc(centerX - 34.0 * s, centerY - rise * 0.86, 68.0 * s, rise * 0.96,
                    192, 156, ArcType.OPEN);
            g.strokeArc(centerX - 22.0 * s, centerY - rise * 0.58, 44.0 * s, rise * 0.78,
                    18, 152, ArcType.OPEN);
            g.restore();
            return;
        }

        if (raptorCryTimer > 0) {
            double phase = raptorSpecialPhase(raptorCryTimer,
                    eagle
                            ? (raptorCryUltimate ? EAGLE_CRY_ULTIMATE_FRAMES : EAGLE_CRY_FRAMES)
                            : (raptorCryUltimate ? FALCON_CRY_ULTIMATE_FRAMES : FALCON_CRY_FRAMES));
            double dir = facingRight ? 1.0 : -1.0;
            double originX = centerX + dir * 22.0 * s;
            double originY = centerY - 8.0 * s;
            g.setEffect(new Glow(eagle ? 0.28 : 0.42));
            g.setStroke(primary.deriveColor(0, 1, 1, 0.72));
            g.setLineWidth((eagle ? 4.8 : 3.8) * s);
            for (int i = 0; i < 3; i++) {
                double width = (54.0 + i * 38.0 + phase * 16.0) * s;
                double height = (28.0 + i * 18.0) * s;
                g.strokeArc(originX + dir * (20.0 + i * 26.0) * s - width * 0.5,
                        originY - height * 0.5, width, height,
                        dir > 0 ? -34 : 214, eagle ? 68 : 56, ArcType.OPEN);
            }
            g.setStroke(secondary.deriveColor(0, 1, 1, 0.54));
            g.setLineWidth(2.2 * s);
            g.strokeLine(originX, originY - 6.0 * s, originX + dir * (92.0 + phase * 24.0) * s, originY - 16.0 * s);
            g.strokeLine(originX, originY + 6.0 * s, originX + dir * (92.0 + phase * 24.0) * s, originY + 16.0 * s);
            g.restore();
            return;
        }

        if (diveTimer > 0 || eagleDiveActive || eagleAscentActive) {
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
            if (eagleAscentActive) {
                g.setStroke((eagle ? Color.GOLD : Color.web("#FFE082")).deriveColor(0, 1, 1, 0.82));
                g.setLineWidth(6.0 * s);
                for (int i = 0; i < 4; i++) {
                    double offset = (i - 1.5) * 16.0 * s;
                    g.strokeLine(centerX + offset, centerY + 18.0 * s, centerX + offset * 0.4, centerY - 76.0 * s);
                }
            }
        }
        if ((skyKing || duneFalcon) && (diveTimer > 0 || eagleDiveActive)) {
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
        g.restore();
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
            double s = sizeMultiplier;
            HeadPose headPose = currentHeadPose();
            double headX = headPose.centerX() - 25.0 * s;
            double headY = headPose.centerY() - 20.0 * s;
            g.setFill(Color.YELLOW);
            g.fillOval(headX + (facingRight ? 5 : 45) * s, headY + 2 * s, 18 * s, 18 * s);
            g.setFill(Color.BLACK);
            g.fillOval(headX + (facingRight ? 10 : 50) * s, headY + 5 * s, 10 * s, 10 * s);
        }
    }

    private void drawVulture(GraphicsContext g, double drawSize) {
        if (type == BirdGame3.BirdType.VULTURE) {
            double s = sizeMultiplier;
            if (isNullRockSkin) {
                double pulse = suppressSelectEffects ? 0.35 : (0.5 + 0.5 * Math.sin(System.currentTimeMillis() / 180.0));
                double cx = x + drawSize * 0.5;
                if (isTrueNullRockForm()) {
                    g.setFill(Color.web("#FFF8E1", 0.22 + pulse * 0.08));
                    g.fillOval(x - 88 * s, y - 92 * s, drawSize + 176 * s, drawSize + 242 * s);
                    g.setStroke(Color.web("#B39DDB").deriveColor(0, 1, 1, 0.65 + pulse * 0.18));
                    g.setLineWidth(7 * s);
                    g.strokeOval(x - 54 * s, y - 58 * s, drawSize + 108 * s, drawSize + 154 * s);
                }
                g.setFill(Color.rgb(3, 2, 7, 0.9));
                g.fillOval(x - 54 * s, y - 50 * s, drawSize + 108 * s, drawSize + 154 * s);
                g.setFill(Color.rgb(8, 6, 14, 0.85));
                g.fillOval(x - 34 * s, y - 26 * s, drawSize + 68 * s, drawSize + 84 * s);
                g.setStroke(Color.web("#5C0F16").deriveColor(0, 1, 1, 0.58 + pulse * 0.18));
                g.setLineWidth(8 * s);
                g.strokeOval(x - 18 * s, y - 10 * s, drawSize + 36 * s, drawSize + 34 * s);

                g.setFill(Color.web("#10030B"));
                double[] crestX = {
                        cx - 40 * s, cx - 24 * s, cx - 10 * s, cx + 2 * s,
                        cx + 16 * s, cx + 34 * s, cx + 16 * s, cx - 2 * s
                };
                double[] crestY = {
                        y + 28 * s, y - 34 * s, y + 12 * s, y - 44 * s,
                        y + 10 * s, y - 28 * s, y + 36 * s, y + 48 * s
                };
                g.fillPolygon(crestX, crestY, crestX.length);
                g.setFill(Color.web("#130C18"));
            } else {
                g.setFill(Color.rgb(35, 15, 45));
            }
            g.fillOval(x, y, drawSize, drawSize);

            double wingSpread = isFlying || Math.abs(vx) > 2 ? (isNullRockSkin ? 2.15 : 1.4) : (isNullRockSkin ? 1.55 : 1.0);
            g.setFill(isNullRockSkin ? Color.web("#09060D") : Color.rgb(20, 10, 30));
            g.fillOval(x - 34 * wingSpread * s, y + 2 * s, 58 * wingSpread * s, 104 * s);
            g.fillOval(x + drawSize - 24 * wingSpread * s, y + 2 * s, 58 * wingSpread * s, 104 * s);

            g.setFill(isNullRockSkin
                    ? (isTrueNullRockForm() ? Color.web("#B71CFF") : Color.web("#7A0C16"))
                    : Color.rgb(180, 30, 30));
            g.fillOval(x + 15 * s, y + 10 * s, 50 * s, 55 * s);

            g.setFill(isNullRockSkin ? Color.web("#2A050A") : Color.CRIMSON.darker().darker());
            g.fillOval(x + 25 * s, y + 25 * s, 20 * s, 20 * s);
            g.fillOval(x + 45 * s, y + 25 * s, 20 * s, 20 * s);
            g.setFill(isNullRockSkin
                    ? (isTrueNullRockForm() ? Color.web("#FFF176") : Color.web("#FF6E6E"))
                    : Color.RED.brighter());
            g.fillOval(x + 30 * s, y + 30 * s, 10 * s, 10 * s);
            g.fillOval(x + 50 * s, y + 30 * s, 10 * s, 10 * s);

            if (isNullRockSkin) {
                double cx = x + drawSize * 0.5;
                g.setFill(Color.web("#150208"));
                g.fillRoundRect(x + 16 * s, y + 54 * s, 48 * s, 18 * s, 14 * s, 14 * s);
                g.setFill(Color.web("#FFD7D7").deriveColor(0, 1, 1, 0.92));
                for (int i = 0; i < 4; i++) {
                    double toothX = x + 24 * s + i * 9 * s;
                    g.fillPolygon(
                            new double[]{toothX, toothX + 4 * s, toothX + 8 * s},
                            new double[]{y + 58 * s, y + 70 * s, y + 58 * s},
                            3
                    );
                }

                g.setStroke(Color.web("#36060E"));
                g.setLineWidth(3.2 * s);
                g.strokeLine(x + 18 * s, y + 16 * s, x + 30 * s, y + 28 * s);
                g.strokeLine(x + 62 * s, y + 16 * s, x + 50 * s, y + 28 * s);

                g.setStroke(Color.web("#FF8A80"));
                g.setLineWidth(1.8 * s);
                g.strokeLine(x + 20 * s, y + 59 * s, x + 60 * s, y + 61 * s);

                g.setStroke(Color.web("#7A101C").deriveColor(0, 1, 1, 0.74));
                g.setLineWidth(2.8 * s);
                g.strokeLine(x + 40 * s, y + 8 * s, x + 40 * s, y + 66 * s);
                g.strokeLine(x + 34 * s, y + 18 * s, x + 24 * s, y + 48 * s);
                g.strokeLine(x + 46 * s, y + 18 * s, x + 56 * s, y + 50 * s);

                Color crownFill = isTrueNullRockForm() ? Color.web("#6A1B9A") : Color.web("#54070F");
                Color crownStroke = isTrueNullRockForm() ? Color.web("#FFF59D") : Color.web("#FFB3B3");
                drawRoyalCrown(g, cx, y - 22 * s, 52 * s, 28 * s, crownFill, crownStroke);
                g.setStroke(Color.web("#3A0810").deriveColor(0, 1, 1, 0.85));
                g.setLineWidth(3.0 * s);
                g.strokeLine(cx - 22 * s, y + 14 * s, cx - 8 * s, y + 48 * s);
                g.strokeLine(cx + 22 * s, y + 14 * s, cx + 8 * s, y + 48 * s);
                g.strokeLine(cx - 8 * s, y + 52 * s, cx - 24 * s, y + 78 * s);
                g.strokeLine(cx + 8 * s, y + 52 * s, cx + 24 * s, y + 78 * s);

                if (isTrueNullRockForm()) {
                    g.setStroke(Color.web("#FFF59D").deriveColor(0, 1, 1, 0.8));
                    g.setLineWidth(2.6 * s);
                    g.strokeArc(cx - 48 * s, y - 58 * s, 96 * s, 36 * s, 200, 140, ArcType.OPEN);
                    g.strokeLine(cx, y - 30 * s, cx, y - 62 * s);
                }

                g.setStroke(Color.web("#0C050F"));
                g.setLineWidth(3.4 * s);
                for (int side = 0; side < 2; side++) {
                    double baseX = side == 0 ? x + 18 * s : x + 62 * s;
                    double dir = side == 0 ? -1.0 : 1.0;
                    g.strokeLine(baseX, y + 74 * s, baseX + dir * 10 * s, y + 88 * s);
                    g.strokeLine(baseX, y + 74 * s, baseX + dir * 4 * s, y + 92 * s);
                    g.strokeLine(baseX, y + 74 * s, baseX + dir * 15 * s, y + 84 * s);
                }
            }

            if (carrionSwarmTimer > 0) {
                g.setFill(Color.BLACK.deriveColor(0, 1, 1, 0.6));
                g.fillOval(x - 40 * s, y - 30 * s, drawSize + 80 * s, drawSize + 100 * s);
                carrionSwarmTimer--;
            }
        }
    }

    private void drawRooster(GraphicsContext g, double drawSize) {
        if (type != BirdGame3.BirdType.ROOSTER) return;
        double s = sizeMultiplier;
        HeadPose headPose = currentHeadPose();
        double headX = headPose.centerX() - 25.0 * s;
        double headY = headPose.centerY() - 20.0 * s;
        double tailBaseX = facingRight ? x + 18 * s : x + drawSize - 18 * s;
        double tailDir = facingRight ? -1 : 1;
        Color tailStroke = isSunforgeSkin ? Color.web("#FFD54F") : Color.web("#BF360C");
        if (isSunforgeSkin && !suppressSelectEffects) {
            g.setStroke(Color.web("#FFF59D").deriveColor(0, 1, 1, 0.45));
            g.setLineWidth(8 * s);
            for (int i = 0; i < 3; i++) {
                double len = 26 + i * 10;
                double rise = 18 + i * 6;
                g.strokeLine(tailBaseX, y + 52 * s, tailBaseX + tailDir * len * s, y + 52 * s - rise * s);
            }
        }
        g.setStroke(tailStroke);
        g.setLineWidth(4 * s);
        for (int i = 0; i < 3; i++) {
            double len = 26 + i * 10;
            double rise = 18 + i * 6;
            g.strokeLine(tailBaseX, y + 52 * s, tailBaseX + tailDir * len * s, y + 52 * s - rise * s);
        }

        g.setFill(isSunforgeSkin ? Color.web("#FFB300") : Color.web("#D32F2F"));
        double combX = headX + (facingRight ? -6 : 36) * s;
        double combY = headY - 26 * s;
        double combW = 28 * s;
        double combH = 18 * s;
        double[] xs = new double[]{
                combX, combX + combW * 0.25, combX + combW * 0.5, combX + combW * 0.75, combX + combW, combX + combW, combX
        };
        double[] ys = new double[]{
                combY + combH, combY, combY + combH * 0.4, combY, combY + combH, combY + combH * 1.2, combY + combH * 1.2
        };
        g.fillPolygon(xs, ys, xs.length);

        g.setFill(isSunforgeSkin ? Color.web("#FFE082") : Color.web("#B71C1C"));
        double wattleX = headX + (facingRight ? 0 : 40) * s;
        g.fillOval(wattleX, headY + 22 * s, 10 * s, 14 * s);
    }

    private void drawStunEffect(GraphicsContext g, double drawSize) {
        if (stunTime <= 0) {
            return;
        }

        double s = sizeMultiplier;
        double pulse = 0.55 + 0.45 * Math.sin(System.currentTimeMillis() / 120.0);
        double headCenterX = type == BirdGame3.BirdType.BAT
                ? x + 40 * s
                : x + (facingRight ? 72 : 28) * s;
        double headCenterY = type == BirdGame3.BirdType.BAT
                ? y + 22 * s
                : y + 34 * s;
        double orbitBaseY = headCenterY - 26 * s;

        g.setFill(Color.web("#CFD8DC").deriveColor(0, 1, 1, 0.10 + pulse * 0.08));
        g.fillOval(x - 6 * s, y - 4 * s, drawSize + 12 * s, drawSize + 10 * s);

        g.setStroke(Color.web("#B3E5FC").deriveColor(0, 1, 1, 0.42 + pulse * 0.16));
        g.setLineWidth(1.6 * s);
        g.strokeArc(headCenterX - 18 * s, headCenterY - 10 * s, 18 * s, 16 * s, 110, 150, ArcType.OPEN);
        g.strokeArc(headCenterX + 2 * s, headCenterY - 10 * s, 18 * s, 16 * s, -80, 150, ArcType.OPEN);

        for (int i = 0; i < 3; i++) {
            double angle = System.currentTimeMillis() / 240.0 + i * (Math.PI * 2.0 / 3.0);
            double orbitX = headCenterX + Math.cos(angle) * 22 * s;
            double orbitY = orbitBaseY + Math.sin(angle) * 10 * s;
            Color fill = i == 1 ? Color.web("#FFF59D") : Color.web("#FFE082");
            drawStunStar(g, orbitX, orbitY, (4.8 + (i % 2) * 1.1) * s, fill, Color.web("#5D4037", 0.55));
        }

        if (type == BirdGame3.BirdType.BAT) {
            double headX = facingRight ? x + 24 * s : x + 16 * s;
            double eyeBias = (facingRight ? 3 : -3) * s;
            drawStunEyeMark(g, headX + 13.5 * s + eyeBias, y + 21.5 * s, 4.8 * s);
            drawStunEyeMark(g, headX + 29.5 * s + eyeBias, y + 21.5 * s, 4.8 * s);
        } else {
            drawStunEyeMark(g, x + (facingRight ? 62.5 : 32.5) * s, y + 32.5 * s, 6.4 * s);
        }
    }

    private void drawDodgeAura(GraphicsContext g, double drawSize) {
        if (!hasDodgeInvulnerability()) return;
        double centerX = x + drawSize / 2.0;
        double centerY = y + drawSize / 2.0;
        double pulse = 0.55 + 0.45 * Math.sin(dodgeInvulnerabilityTimer * 0.42);
        double ring = drawSize * (0.90 + pulse * 0.11);
        Color accent = switch (dodgeType) {
            case SPOT -> Color.web("#A5D6A7", 0.75);
            case ROLL -> Color.web("#81D4FA", 0.78);
            case AIR -> Color.web("#FFF59D", 0.74);
            case NONE -> Color.web("#CFD8DC", 0.68);
        };
        g.setStroke(accent);
        g.setLineWidth(3.0);
        g.strokeOval(centerX - ring / 2.0, centerY - ring / 2.0, ring, ring);
        g.setStroke(accent.deriveColor(0, 1, 1, 0.42));
        g.setLineWidth(1.8);
        g.strokeOval(centerX - ring * 0.66, centerY - ring * 0.66, ring * 1.32, ring * 1.32);
    }

    private void drawNullRockShield(GraphicsContext g, double drawSize) {
        if (!hasNullRockInvulnerability()) return;
        double centerX = x + drawSize / 2.0;
        double centerY = y + drawSize / 2.0;
        double pulse = 0.5 + 0.5 * Math.sin(nullRockInvincibilityTimer * 0.24);
        double ring = drawSize * (0.92 + pulse * 0.14);
        g.setStroke(Color.web("#FFEBEE", 0.85));
        g.setLineWidth(4.0);
        g.strokeOval(centerX - ring / 2.0, centerY - ring / 2.0, ring, ring);
        g.setStroke(Color.web("#80DEEA", 0.68));
        g.setLineWidth(2.5);
        g.strokeOval(centerX - ring * 0.68, centerY - ring * 0.68, ring * 1.36, ring * 1.36);
        g.setFill(Color.web("#FFCDD2", 0.9));
        g.setFont(Font.font("Arial Black", Math.max(22.0, 18.0 * Math.clamp(sizeMultiplier, 1.0, 1.8))));
        g.fillText(isTrueNullRockForm() ? "DIVINE SHELL" : "VOID SHELL", x - 6 * sizeMultiplier, y - 34 * sizeMultiplier);
    }

    private void drawSpecialCooldown(GraphicsContext g) {
        if (type == BirdGame3.BirdType.PIGEON && specialCooldown > 0) {
            return;
        }
        if (specialCooldown > 0 && specialMaxCooldown > 0) {
            double ratio = (double) specialCooldown / specialMaxCooldown;

            double drawSize = 80 * sizeMultiplier;
            double barScale = Math.clamp(sizeMultiplier, 0.85, 1.25);
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
                cooldownText = isNullRockForm() ? "FLOCK" : "CROWS";
            } else if (type == BirdGame3.BirdType.ROOSTER && specialCooldown > 0) {
                cooldownText = "CHICKS";
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
            boolean royal = loungeRoyal;
            Color baseFill = royal ? Color.web("#5E35B1") : Color.LIME;
            Color baseStroke = loungeDamageFlash > 0 ? Color.ORANGERED : (royal ? Color.GOLD : Color.DARKGREEN);
            g.setFill(baseFill.deriveColor(0, 1, 1, 0.75));
            g.fillRoundRect(loungeX - 62, loungeY - 42, 124, 84, 32, 32);
            g.setStroke(baseStroke);
            g.setLineWidth(loungeDamageFlash > 0 ? 8 : (royal ? 6 : 5));
            g.strokeRoundRect(loungeX - 62, loungeY - 42, 124, 84, 32, 32);

            int maxHealth = Math.max(1, loungeMaxHealth);
            double ratio = Math.clamp(loungeHealth / (double) maxHealth, 0.0, 1.0);
            g.setFill(Color.BLACK);
            g.fillRect(loungeX - 65, loungeY - 75, 130, 18);
            g.setFill(Color.RED.darker());
            g.fillRect(loungeX - 60, loungeY - 70, 120, 12);
            g.setFill(royal ? Color.GOLD : Color.LIME);
            g.fillRect(loungeX - 60, loungeY - 70, 120 * ratio, 12);

            g.setFill(Color.WHITE);
            g.setFont(Font.font("Arial Black", 20));
            g.fillText(loungeHealth + " HP", loungeX - 48, loungeY - 55);

            g.setFill(royal ? Color.GOLD : Color.BLACK);
            g.setFont(Font.font("Arial Black", 24));
            g.fillText(royal ? "ROYAL LOUNGE" : "LOUNGE", loungeX - (royal ? 74 : 52), loungeY + 8);

            if (royal) {
                drawRoyalCrown(g, loungeX, loungeY - 58, 46, 22, Color.GOLD, Color.web("#FFF59D"));
            }

            if (loungeDamageFlash > 0) loungeDamageFlash--;
        }
    }

    private void drawBodyAndEyes(GraphicsContext g, double drawSize, AttackVisualPose pose) {
        if (type == BirdGame3.BirdType.BAT) {
            drawBatBody(g);
            return;
        }
        if (type == BirdGame3.BirdType.PHOENIX) {
            drawPhoenixBody(g, drawSize);
            return;
        }
        if (drawPhotoEagleSprite(g, drawSize, pose)) {
            return;
        }

        double s = sizeMultiplier;
        HeadPose headPose = standardHeadPose(pose);
        double headW = 50.0 * s;
        double headH = 40.0 * s;
        double headX = headPose.centerX() - headW / 2.0;
        double headY = headPose.centerY() - headH / 2.0;
        boolean noirPigeon = (type == BirdGame3.BirdType.PIGEON && isNoirSkin);
        boolean beaconPigeon = (type == BirdGame3.BirdType.PIGEON && isBeaconSkin);
        boolean stormPigeon = (type == BirdGame3.BirdType.PIGEON && isStormSkin);
        boolean classicPalette = isClassicSkin && type != BirdGame3.BirdType.PIGEON;
        boolean duneFalcon = (type == BirdGame3.BirdType.FALCON && isDuneSkin);
        boolean mintPenguin = (type == BirdGame3.BirdType.PENGUIN && isMintSkin);
        boolean circuitTitmouse = (type == BirdGame3.BirdType.TITMOUSE && isCircuitSkin);
        boolean prismRazorbill = (type == BirdGame3.BirdType.RAZORBILL && isPrismSkin);
        boolean auroraPelican = (type == BirdGame3.BirdType.PELICAN && isAuroraSkin);
        boolean ironcladPelican = (type == BirdGame3.BirdType.PELICAN && isIroncladSkin);
        boolean sunflareHummingbird = (type == BirdGame3.BirdType.HUMMINGBIRD && isSunflareSkin);
        boolean glacierShoebill = (type == BirdGame3.BirdType.SHOEBILL && isGlacierSkin);
        boolean tideVulture = (type == BirdGame3.BirdType.VULTURE && isTideSkin);
        boolean nullRockVulture = (type == BirdGame3.BirdType.VULTURE && isNullRockSkin);
        boolean eclipseMockingbird = (type == BirdGame3.BirdType.MOCKINGBIRD && isEclipseSkin);
        boolean sunforgeRooster = (type == BirdGame3.BirdType.ROOSTER && isSunforgeSkin);
        boolean freemanPigeon = (type == BirdGame3.BirdType.PIGEON && isFreemanSkin);
        boolean ravenEyes = (type == BirdGame3.BirdType.RAVEN);
        Color bodyColor;
        Color headColor;
        Color eyeOverride = null;
        if (nullRockVulture) {
            bodyColor = Color.web("#180E1A");
            headColor = Color.web("#2B1218");
            eyeOverride = Color.web("#FF6E6E");
        } else if (stormPigeon) {
            bodyColor = Color.web("#455A64");
            headColor = Color.web("#607D8B");
            eyeOverride = Color.web("#B3E5FC");
        } else if (beaconPigeon) {
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
        } else if (sunforgeRooster) {
            bodyColor = Color.web("#4E342E");
            headColor = Color.web("#BF6D1D");
            eyeOverride = Color.web("#FFF8E1");
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
        } else if (ironcladPelican) {
            bodyColor = Color.web("#8D6E63");
            headColor = Color.web("#BCAAA4");
            eyeOverride = Color.web("#FFF3E0");
        } else if (type == BirdGame3.BirdType.ROADRUNNER && !classicPalette) {
            bodyColor = Color.web("#B87333");
            headColor = Color.web("#CC8C46");
            eyeOverride = Color.web("#4E342E");
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
        g.fillOval(headX, headY, headW, headH);
        if (type == BirdGame3.BirdType.RAZORBILL) {
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
        if (type == BirdGame3.BirdType.ROADRUNNER) {
            int tailDir = facingRight ? -1 : 1;
            double crestBaseX = facingRight ? headX + 18 * s : headX + 32 * s;
            Color plume = classicPalette ? game.classicSkinAccentColor(type) : Color.web("#2E5AAC");
            g.setFill(plume);
            g.fillPolygon(
                    new double[]{crestBaseX, crestBaseX + tailDir * 14 * s, crestBaseX + tailDir * 4 * s},
                    new double[]{y + 20 * s, y - 2 * s, y + 18 * s},
                    3
            );
            g.fillPolygon(
                    new double[]{crestBaseX + 8 * s, crestBaseX + tailDir * 8 * s, crestBaseX + 12 * s},
                    new double[]{y + 22 * s, y + 2 * s, y + 19 * s},
                    3
            );
            g.setFill(Color.web("#E8D2A6").deriveColor(0, 1, 1, 0.75));
            g.fillOval(x + 20 * s, y + 36 * s, 42 * s, 24 * s);
            double tailBaseX = facingRight ? x + 12 * s : x + 68 * s;
            g.setFill(plume.deriveColor(0, 1, 0.95, 0.95));
            g.fillPolygon(
                    new double[]{tailBaseX, tailBaseX + tailDir * 24 * s, tailBaseX + tailDir * 8 * s},
                    new double[]{y + 48 * s, y + 42 * s, y + 64 * s},
                    3
            );
            g.fillPolygon(
                    new double[]{tailBaseX + 5 * s, tailBaseX + tailDir * 16 * s, tailBaseX + tailDir * 2 * s},
                    new double[]{y + 44 * s, y + 28 * s, y + 60 * s},
                    3
            );
        }
        // Titmouse head details are handled by drawTitmouseSpecial when effects are enabled.
        if (ravenEyes) {
            double glowX = headX + (facingRight ? -2 : 38) * s;
            double glowY = headY - 2 * s;
            g.setFill(Color.web("#B71C1C").deriveColor(0, 1, 1, 0.35));
            g.fillOval(glowX, glowY, 29 * s, 29 * s);
        }
        g.setFill(Color.WHITE);
        g.fillOval(headX + (facingRight ? 0 : 40) * s, headY, 25 * s, 25 * s);
        Color eyeColor = classicPalette ? game.classicSkinAccentColor(type) : Color.BLACK;
        if (eyeOverride != null) eyeColor = eyeOverride;
        if (noirPigeon) eyeColor = Color.RED.brighter();
        if (ravenEyes) eyeColor = Color.web("#D50000");
        g.setFill(eyeColor);
        g.fillOval(headX + (facingRight ? 5 : 45) * s, headY + 5 * s, 15 * s, 15 * s);
    }

    private void drawHeisenbirdAccessories(GraphicsContext g) {
        if (type != BirdGame3.BirdType.HEISENBIRD) return;
        double s = sizeMultiplier;
        HeadPose headPose = currentHeadPose();
        double headX = headPose.centerX() - 25.0 * s;
        double headY = headPose.centerY() - 20.0 * s;
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
        double goateeX = headX + (facingRight ? 12 : 24) * s;
        double goateeY = headY + 34 * s;
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
        if (type == BirdGame3.BirdType.PIGEON && isStormSkin) {
            g.setStroke(Color.web("#90CAF9").deriveColor(0, 1, 1, 0.85));
            g.setLineWidth(2.1 * s);
            g.strokeLine(x + 26 * s, y + 18 * s, x + 18 * s, y + 38 * s);
            g.strokeLine(x + 18 * s, y + 38 * s, x + 34 * s, y + 38 * s);
            g.strokeLine(x + 34 * s, y + 38 * s, x + 24 * s, y + 62 * s);
            g.setStroke(Color.web("#CFD8DC").deriveColor(0, 1, 1, 0.55));
            g.strokeArc(x - 4 * s, y + 8 * s, drawSize + 8 * s, drawSize * 0.54, 210, 120, ArcType.OPEN);
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
        if (type == BirdGame3.BirdType.PELICAN && isIroncladSkin) {
            g.setStroke(Color.web("#D7CCC8").deriveColor(0, 1, 1, 0.8));
            g.setLineWidth(2.4 * s);
            g.strokeArc(x - 4 * s, y + 28 * s, 80 * s, 34 * s, 196, 148, ArcType.OPEN);
            g.strokeLine(x + 18 * s, y + 32 * s, x + 60 * s, y + 28 * s);
            g.setFill(Color.web("#FFCC80").deriveColor(0, 1, 1, 0.5));
            g.fillOval(x + 18 * s, y + 54 * s, 8 * s, 8 * s);
            g.fillOval(x + 54 * s, y + 50 * s, 8 * s, 8 * s);
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
        if (type == BirdGame3.BirdType.VULTURE && isNullRockSkin) {
            g.setStroke(Color.web("#FF8A80").deriveColor(0, 1, 1, 0.72));
            g.setLineWidth(3.0 * s);
            g.strokeArc(x - 8 * s, y + 12 * s, drawSize + 16 * s, drawSize + 26 * s, 208, 126, ArcType.OPEN);
            g.setStroke(Color.web("#4A0610").deriveColor(0, 1, 1, 0.8));
            g.setLineWidth(2.1 * s);
            g.strokeLine(x + 20 * s, y + 22 * s, x + 34 * s, y + 60 * s);
            g.strokeLine(x + 58 * s, y + 18 * s, x + 46 * s, y + 58 * s);
            g.setFill(Color.web("#FFCDD2").deriveColor(0, 1, 1, 0.3));
            g.fillOval(x + 18 * s, y + 42 * s, 44 * s, 18 * s);
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
        if (type == BirdGame3.BirdType.BAT && isResonanceSkin) {
            g.setStroke(Color.web("#80DEEA").deriveColor(0, 1, 1, 0.7));
            g.setLineWidth(2.0 * s);
            g.strokeOval(x - 4 * s, y + 2 * s, 88 * s, 88 * s);
            g.strokeOval(x - 14 * s, y - 8 * s, 108 * s, 108 * s);
            g.setStroke(Color.web("#B39DDB").deriveColor(0, 1, 1, 0.7));
            g.strokeLine(x + 14 * s, y + 38 * s, x + 68 * s, y + 28 * s);
        }
        if (type == BirdGame3.BirdType.ROOSTER && isSunforgeSkin) {
            g.setStroke(Color.web("#FFD54F").deriveColor(0, 1, 1, 0.7));
            g.setLineWidth(2.4 * s);
            g.strokeArc(x - 10 * s, y + 4 * s, drawSize + 20 * s, drawSize + 18 * s, 210, 120, ArcType.OPEN);
            g.setFill(Color.web("#FFF8E1").deriveColor(0, 1, 1, 0.18));
            g.fillOval(x + 12 * s, y + 34 * s, 56 * s, 24 * s);
            g.setFill(Color.web("#FFE082").deriveColor(0, 1, 1, 0.5));
            g.fillOval(x + 24 * s, y + 20 * s, 8 * s, 8 * s);
            g.fillOval(x + 52 * s, y + 26 * s, 7 * s, 7 * s);
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
        boolean resonance = isResonanceSkin;
        Color wing = umbra ? Color.web("#0B0F1A") : (resonance ? Color.web("#162447") : Color.rgb(28, 16, 48));
        Color wingInner = umbra ? Color.web("#182032") : (resonance ? Color.web("#244A6A") : Color.rgb(50, 30, 76));
        Color body = umbra ? Color.web("#1C1033") : (resonance ? Color.web("#355C7D") : Color.rgb(70, 40, 102));
        Color head = umbra ? Color.web("#2D1B4D") : (resonance ? Color.web("#4E7BA7") : Color.rgb(88, 54, 124));
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
        Color ear = umbra ? Color.web("#4C537A") : (resonance ? Color.web("#8AD7FF") : Color.rgb(110, 74, 150));
        g.setFill(ear);
        g.fillPolygon(new double[]{headX + 6 * s, headX + 12 * s, headX + 18 * s}, new double[]{y + 8 * s, y - 10 * s, y + 8 * s}, 3);
        g.fillPolygon(new double[]{headX + 26 * s, headX + 32 * s, headX + 38 * s}, new double[]{y + 8 * s, y - 10 * s, y + 8 * s}, 3);

        // eyes
        g.setFill(Color.WHITE);
        double eyeBias = (facingRight ? 3 : -3) * s;
        g.fillOval(headX + 8 * s + eyeBias, y + 16 * s, 11 * s, 11 * s);
        g.fillOval(headX + 24 * s + eyeBias, y + 16 * s, 11 * s, 11 * s);
        Color iris = umbra ? Color.web("#00E5FF") : (resonance ? Color.web("#B2EBF2") : Color.CRIMSON.brighter());
        g.setFill(iris);
        g.fillOval(headX + 11 * s + eyeBias, y + 19 * s, 6 * s, 6 * s);
        g.fillOval(headX + 27 * s + eyeBias, y + 19 * s, 6 * s, 6 * s);

        if (resonance) {
            g.setStroke(Color.web("#80DEEA").deriveColor(0, 1, 1, 0.8));
            g.setLineWidth(1.8 * s);
            g.strokeArc(x - 18 * s, y + 8 * s, 116 * s, 60 * s, 12, 154, ArcType.OPEN);
            g.strokeArc(x - 6 * s, y + 18 * s, 92 * s, 42 * s, 10, 146, ArcType.OPEN);
        }

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
            HeadPose headPose = currentHeadPose();
            double headX = headPose.centerX() - 25.0 * s;
            double headY = headPose.centerY() - 20.0 * s;
            g.setFill(Color.DARKGRAY.darker());
            g.fillRoundRect(x + 20 * s, y - 10 * s, 40 * s, 20 * s, 10 * s, 10 * s);
            g.fillRect(x + 10 * s, y - 5 * s, 60 * s, 8 * s);

            g.setFill(Color.WHITE);
            g.fillRect(headX + (facingRight ? 35 : 5) * s, headY + 25 * s, 20 * s, 4 * s);
            g.setFill(Color.ORANGE.brighter());
            g.fillRect(headX + (facingRight ? 55 : -15) * s, headY + 25 * s, 8 * s, 4 * s);

            if (Math.random() < 0.7) {
                double smokeX = headX + (facingRight ? 60 : 0) * s;
                double smokeY = headY + (20 + Math.random() * 12) * s;
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
            HeadPose headPose = currentHeadPose();
            double headX = headPose.centerX() - 25.0 * s;
            double headY = headPose.centerY() - 20.0 * s;
            g.setFill(Color.BLACK);
            g.fillRoundRect(x + 16 * s, y - 12 * s, 48 * s, 18 * s, 10 * s, 10 * s);
            g.fillRect(x + 4 * s, y - 6 * s, 72 * s, 8 * s);

            g.setFill(Color.BLACK.deriveColor(0, 1, 0.8, 1));
            g.fillRoundRect(x + 18 * s, y + 52 * s, 44 * s, 22 * s, 10 * s, 10 * s);
            g.setStroke(Color.RED.brighter());
            g.setLineWidth(3 * s);
            g.strokeLine(x + 22 * s, y + 58 * s, x + 58 * s, y + 70 * s);

            if (Math.random() < 0.45) {
                double smokeX = headX + (facingRight ? 48 : 10) * s;
                double smokeY = headY + (22 + Math.random() * 10) * s;
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
            HeadPose headPose = currentHeadPose();
            double headX = headPose.centerX() - 25.0 * s;
            double headY = headPose.centerY() - 20.0 * s;

            g.setFill(Color.web("#5D4037"));
            g.fillRoundRect(x + 16 * s, y - 10 * s, 48 * s, 18 * s, 10 * s, 10 * s);
            g.setFill(Color.web("#4E342E"));
            g.fillRect(x + 12 * s, y - 2 * s, 56 * s, 6 * s);

            double eyeX = headX + (facingRight ? 0 : 40) * s;
            g.setFill(Color.web("#4E342E").deriveColor(0, 1, 1, 0.55));
            g.fillOval(eyeX, headY - 1 * s, 25 * s, 14 * s);

            g.setFill(Color.web("#ECEFF1"));
            g.fillRect(headX + (facingRight ? 35 : 5) * s, headY + 25 * s, 20 * s, 4 * s);
            g.setFill(Color.web("#FF8F00"));
            g.fillRect(headX + (facingRight ? 55 : -15) * s, headY + 25 * s, 8 * s, 4 * s);

            if (Math.random() < 0.6) {
                double smokeX = headX + (facingRight ? 60 : 0) * s;
                double smokeY = headY + (20 + Math.random() * 12) * s;
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
            HeadPose headPose = currentHeadPose();
            double headX = headPose.centerX() - 25.0 * s;
            double headY = headPose.centerY() - 20.0 * s;
            double pulse = 0.5 + 0.5 * Math.sin(System.currentTimeMillis() / 160.0);
            g.setFill(Color.web("#FFF59D").deriveColor(0, 1, 1, 0.18 + 0.18 * pulse));
            g.fillOval(x - 14 * s, y - 14 * s, drawSize + 28 * s, drawSize + 28 * s);

            g.setStroke(Color.web("#FFE082").deriveColor(0, 1, 1, 0.75));
            g.setLineWidth(2.2 * s);
            g.strokeOval(x - 8 * s, y - 8 * s, drawSize + 16 * s, drawSize + 16 * s);

            g.setFill(Color.web("#81D4FA").deriveColor(0, 1, 1, 0.85));
            g.fillOval(headX + 18 * s, headY - 10 * s, 10 * s, 10 * s);
            g.setStroke(Color.web("#B3E5FC").deriveColor(0, 1, 1, 0.85));
            g.setLineWidth(1.6 * s);
            g.strokeLine(headX + 23 * s, headY - 12 * s, headX + 23 * s, headY - 20 * s);
        }
    }

    private void drawBeak(GraphicsContext g, AttackVisualPose pose) {
        if (photoEagleSkinActive()) {
            return;
        }
        double s = sizeMultiplier;
        double openScale = pose == null ? 1.0 : pose.beakOpenScale();
        if (type == BirdGame3.BirdType.BAT) {
            double mouthX = x + 33 * s;
            double mouthY = y + 28 * s;
            boolean attacking = attackAnimationTimer > 0;
            double biteOpen = attacking ? (4 + Math.sin(attackAnimationTimer * 0.8) * 3) * s * openScale : 0;
            g.setFill(Color.rgb(220, 120, 170));
            g.fillOval(mouthX, mouthY + biteOpen * 0.2, 16 * s, (10 + biteOpen) * s);
            g.setFill(Color.WHITE);
            g.fillPolygon(
                    new double[]{mouthX + 4 * s, mouthX + 7 * s, mouthX + 10 * s},
                    new double[]{mouthY + (8 + biteOpen) * s, mouthY + (13 + biteOpen) * s, mouthY + (8 + biteOpen) * s},
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
            double beakY = y + 24 * s;
            boolean attacking = attackAnimationTimer > 0;
            double open = attacking ? (12 + Math.sin(attackAnimationTimer * 0.7) * 6) * s * openScale : 2.5 * s;
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

        HeadPose headPose = standardHeadPose(pose);
        boolean isAttacking = attackAnimationTimer > 0;
        double openAmount = (isAttacking ? (16 + Math.sin(attackAnimationTimer * 0.7) * 10) : 3) * s * openScale;
        double beakLength = ((type == BirdGame3.BirdType.FALCON ? 34
                : type == BirdGame3.BirdType.ROADRUNNER ? 42 : 28) + (pose == null ? 0.0 : pose.beakLengthBonus())) * s;
        double aimAngle = headPose.aimAngleRadians();
        double dirX = Math.cos(aimAngle);
        double dirY = Math.sin(aimAngle);
        double perpendicularAngle = aimAngle + Math.PI * 0.5;
        double normalX = Math.cos(perpendicularAngle);
        double normalY = Math.sin(perpendicularAngle);
        if (Math.abs(normalY) > Math.abs(normalX) && normalY < 0.0) {
            normalX = -normalX;
            normalY = -normalY;
        }
        double mouthCenterX = headPose.centerX() + dirX * 5.0 * s;
        double mouthCenterY = headPose.centerY() + dirY * 5.0 * s + 5.0 * s;
        double baseUpperX = mouthCenterX - normalX * 8.0 * s;
        double baseUpperY = mouthCenterY - normalY * 8.0 * s;
        double baseLowerX = mouthCenterX + normalX * 8.0 * s;
        double baseLowerY = mouthCenterY + normalY * 8.0 * s;
        double tipBaseX = mouthCenterX + dirX * beakLength;
        double tipBaseY = mouthCenterY + dirY * beakLength;
        double upperTipX = tipBaseX - normalX * openAmount;
        double upperTipY = tipBaseY - normalY * openAmount;
        double lowerTipX = tipBaseX + normalX * openAmount * 1.6;
        double lowerTipY = tipBaseY + normalY * openAmount * 1.6;
        double tongueCenterX = tipBaseX - dirX * 12.0 * s + normalX * 2.0 * s;
        double tongueCenterY = tipBaseY - dirY * 12.0 * s + normalY * 2.0 * s;

        g.setFill(isAttacking ? Color.ORANGERED : Color.ORANGE);
        g.fillPolygon(
                new double[]{baseUpperX, upperTipX, baseLowerX},
                new double[]{baseUpperY, upperTipY, baseLowerY},
                3
        );
        g.fillPolygon(
                new double[]{baseUpperX, lowerTipX, baseLowerX},
                new double[]{baseUpperY, lowerTipY, baseLowerY},
                3
        );

        if (isAttacking && attackAnimationTimer > 4) {
            g.setFill(Color.DEEPPINK.darker());
            g.fillOval(tongueCenterX - 10.0 * s, tongueCenterY - 7.0 * s, 20.0 * s, 14.0 * s);
        }

        int flashFrame = 12;
        if (attackAnimationTimer == flashFrame) {
            double flashOpacity = 0.7;
            double flashSize = 36.0 * s;
            double flashCenterX = tipBaseX + dirX * 12.0 * s;
            double flashCenterY = tipBaseY + dirY * 12.0 * s;
            g.setFill(Color.WHITE.deriveColor(0, 1, 1, flashOpacity));
            g.fillOval(flashCenterX - flashSize * 0.5, flashCenterY - flashSize * 0.5, flashSize, flashSize);
        }
    }

    private void drawPelican(GraphicsContext g) {
        if (type == BirdGame3.BirdType.PELICAN) {
            double s = sizeMultiplier;
            HeadPose headPose = currentHeadPose();
            double headX = headPose.centerX() - 25.0 * s;
            double headY = headPose.centerY() - 20.0 * s;
            double pouchX = headX + 2 * s;
            double pouchY = headY + 22 * s;
            double pouchW = (plungeTimer > 0 ? 62 : 46) * s;
            double pouchH = (plungeTimer > 0 ? 38 : 28) * s;
            g.setFill(isIroncladSkin ? Color.web("#A1887F") : Color.rgb(255, 180, 80));
            g.fillOval(pouchX, pouchY, pouchW, pouchH);
            g.setFill(isIroncladSkin ? Color.web("#D7CCC8") : Color.rgb(255, 200, 100));
            g.fillOval(pouchX + 5 * s, pouchY + 4 * s, pouchW - 12 * s, pouchH - 12 * s);
            if (isIroncladSkin) {
                g.setStroke(Color.web("#5D4037"));
                g.setLineWidth(1.8 * s);
                g.strokeOval(pouchX + 3 * s, pouchY + 3 * s, pouchW - 6 * s, pouchH - 6 * s);
            }
        }
    }

    private void drawVineGrapple(GraphicsContext g) {
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
