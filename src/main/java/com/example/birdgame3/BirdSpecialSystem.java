package com.example.birdgame3;

import javafx.scene.paint.Color;

final class BirdSpecialSystem {

    private BirdSpecialSystem() {
    }

    static void useSpecial(Bird bird) {
        boolean canStartSelectedSpecial = BirdSpecialReadiness.canStart(bird);
        boolean ultimateReady = !BirdSpecialReadiness.hasEmptyMockingbirdNeutral(bird) && bird.isUltimateReady();
        boolean pigeonUltimateReady = ultimateReady
                && bird.type == BirdGame3.BirdType.PIGEON
                && bird.canStartPigeonUltimate();
        boolean eagleUltimateReady = ultimateReady
                && bird.type == BirdGame3.BirdType.EAGLE
                && bird.canStartEagleUltimate();
        boolean falconUltimateReady = ultimateReady
                && bird.type == BirdGame3.BirdType.FALCON
                && bird.canStartFalconUltimate();
        if (!canStartSelectedSpecial && !pigeonUltimateReady && !eagleUltimateReady && !falconUltimateReady) {
            return;
        }

        BirdGame3 game = bird.game;
        Bird.DirectionalSpecialInput input = bird.selectDirectionalSpecialInput();

        if (ultimateReady && bird.type == BirdGame3.BirdType.PIGEON) {
            if (!pigeonUltimateReady) {
                if (!game.isAI[bird.playerIndex]) {
                    bird.cooldownFlash = 15;
                }
                return;
            }
            if (!bird.consumeUltimate()) {
                return;
            }
            triggerUltimateStartEffects(bird);
            playSpecialSound(bird);
            game.specialsUsed[bird.playerIndex]++;
            game.recordUltimateMoveUse(bird, PigeonSpecials.ROOFTOP_CORONATION_MOVE);
            game.recordTrainingSpecialUse(bird, input);
            PigeonSpecials.startCoronation(bird);
            return;
        }
        if (ultimateReady && bird.type == BirdGame3.BirdType.EAGLE) {
            if (!eagleUltimateReady) {
                if (!game.isAI[bird.playerIndex]) {
                    bird.cooldownFlash = 15;
                }
                return;
            }
            if (!bird.consumeUltimate()) {
                return;
            }
            triggerUltimateStartEffects(bird);
            playSpecialSound(bird);
            game.specialsUsed[bird.playerIndex]++;
            game.recordUltimateMoveUse(bird, RaptorSpecials.SKY_SOVEREIGN_MOVE);
            game.recordTrainingSpecialUse(bird, input);
            RaptorSpecials.startSkySovereign(bird);
            return;
        }
        if (ultimateReady && bird.type == BirdGame3.BirdType.FALCON) {
            if (!falconUltimateReady) {
                if (!game.isAI[bird.playerIndex]) {
                    bird.cooldownFlash = 15;
                }
                return;
            }
            if (!bird.consumeUltimate()) {
                return;
            }
            triggerUltimateStartEffects(bird);
            playSpecialSound(bird);
            game.specialsUsed[bird.playerIndex]++;
            game.recordUltimateMoveUse(bird, RaptorSpecials.TERMINAL_VELOCITY_MOVE);
            game.recordTrainingSpecialUse(bird, input);
            RaptorSpecials.startTerminalVelocity(bird);
            return;
        }

        if (BirdSpecialReadiness.usesSharedSpecialCooldown(bird)
                && bird.specialCooldown > 0
                && !ultimateReady) {
            if (!game.isAI[bird.playerIndex]) {
                bird.cooldownFlash = 15;
            }
            return;
        }

        boolean ultimateTriggered = ultimateReady && bird.consumeUltimate();
        if (ultimateTriggered) {
            triggerUltimateStartEffects(bird);
        }

        playSpecialSound(bird);
        game.specialsUsed[bird.playerIndex]++;
        game.recordSpecialMoveUse(bird, input, ultimateTriggered);
        game.recordTrainingSpecialUse(bird, input);
        BirdSpecialExecutor.execute(bird, ultimateTriggered);
    }

    private static void triggerUltimateStartEffects(Bird bird) {
        BirdGame3 game = bird.game;
        game.addToKillFeed(bird.shortName() + " UNLEASHED ULTIMATE!");
        game.shakeIntensity = Math.max(game.shakeIntensity, 18);
        game.hitstopFrames = Math.max(game.hitstopFrames, 8);
        game.triggerFlash(0.7, false);

        int ultimateBurstParticles = bird.scaledParticleCount(90);
        for (int i = 0; i < ultimateBurstParticles; i++) {
            double angle = Math.random() * Math.PI * 2;
            double speed = 8 + Math.random() * 16;
            game.particles.add(new Particle(
                    bird.x + 40 + Math.cos(angle) * 20,
                    bird.y + 40 + Math.sin(angle) * 20,
                    Math.cos(angle) * speed,
                    Math.sin(angle) * speed - 4,
                    Color.GOLD.deriveColor(0, 1, 1, 0.95)
            ));
        }
    }

    private static void playSpecialSound(Bird bird) {
        BirdGame3 game = bird.game;
        if (!game.isSfxEnabled()) {
            return;
        }
        if (bird.type == BirdGame3.BirdType.RAZORBILL) {
            game.playVaseBreakingSfx();
        } else {
            game.playJalapenoSfx();
        }
    }

}
