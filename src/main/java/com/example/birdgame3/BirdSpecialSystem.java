package com.example.birdgame3;

import javafx.scene.paint.Color;

final class BirdSpecialSystem {

    private BirdSpecialSystem() {
    }

    static void useSpecial(Bird bird) {
        boolean canStartSelectedSpecial = BirdSpecialReadiness.canStart(bird);
        boolean ultimateReady = bird.isUltimateReady()
                && (bird.type == BirdGame3.BirdType.MOCKINGBIRD
                || !BirdSpecialReadiness.hasEmptyMockingbirdNeutral(bird));
        boolean pigeonUltimateReady = ultimateReady
                && bird.type == BirdGame3.BirdType.PIGEON
                && bird.canStartPigeonUltimate();
        boolean eagleUltimateReady = ultimateReady
                && bird.type == BirdGame3.BirdType.EAGLE
                && bird.canStartEagleUltimate();
        boolean falconUltimateReady = ultimateReady
                && bird.type == BirdGame3.BirdType.FALCON
                && bird.canStartFalconUltimate();
        boolean mockingbirdUltimateReady = ultimateReady
                && bird.type == BirdGame3.BirdType.MOCKINGBIRD;
        if (!canStartSelectedSpecial
                && !pigeonUltimateReady
                && !eagleUltimateReady
                && !falconUltimateReady
                && !mockingbirdUltimateReady) {
            return;
        }

        BirdGame3 game = bird.game;
        Bird.DirectionalSpecialInput input = bird.selectDirectionalSpecialInput();

        if (ultimateReady && bird.type == BirdGame3.BirdType.PIGEON) {
            if (!pigeonUltimateReady) {
                return;
            }
            if (!bird.consumeUltimate()) {
                return;
            }
            triggerUltimateStartEffects(bird);
            playSpecialSound(bird, false);
            game.specialsUsed[bird.playerIndex]++;
            game.recordUltimateMoveUse(bird, PigeonSpecials.ROOFTOP_CORONATION_MOVE);
            game.recordTrainingSpecialUse(bird, input);
            PigeonSpecials.startCoronation(bird);
            return;
        }
        if (ultimateReady && bird.type == BirdGame3.BirdType.EAGLE) {
            if (!eagleUltimateReady) {
                return;
            }
            if (!bird.consumeUltimate()) {
                return;
            }
            triggerUltimateStartEffects(bird);
            playSpecialSound(bird, false);
            game.specialsUsed[bird.playerIndex]++;
            game.recordUltimateMoveUse(bird, RaptorSpecials.SKY_SOVEREIGN_MOVE);
            game.recordTrainingSpecialUse(bird, input);
            RaptorSpecials.startSkySovereign(bird);
            return;
        }
        if (ultimateReady && bird.type == BirdGame3.BirdType.FALCON) {
            if (!falconUltimateReady) {
                return;
            }
            if (!bird.consumeUltimate()) {
                return;
            }
            triggerUltimateStartEffects(bird);
            playSpecialSound(bird, false);
            game.specialsUsed[bird.playerIndex]++;
            game.recordUltimateMoveUse(bird, RaptorSpecials.TERMINAL_VELOCITY_MOVE);
            game.recordTrainingSpecialUse(bird, input);
            RaptorSpecials.startTerminalVelocity(bird);
            return;
        }

        if (BirdSpecialReadiness.usesSharedSpecialCooldown(bird)
                && bird.specialCooldown > 0
                && !ultimateReady) {
            return;
        }

        boolean ultimateTriggered = ultimateReady && bird.consumeUltimate();
        if (ultimateTriggered) {
            triggerUltimateStartEffects(bird);
        }

        playSpecialSound(bird, ultimateTriggered);
        game.specialsUsed[bird.playerIndex]++;
        if (ultimateTriggered && bird.type == BirdGame3.BirdType.PHOENIX) {
            game.recordUltimateMoveUse(bird, PhoenixSpecials.REBIRTH_NOVA_MOVE);
        } else if (ultimateTriggered && bird.type == BirdGame3.BirdType.HUMMINGBIRD) {
            game.recordUltimateMoveUse(bird, HummingbirdSpecials.NEEDLEHEART_OVERDRIVE_MOVE);
        } else if (ultimateTriggered && bird.type == BirdGame3.BirdType.ROOSTER) {
            game.recordUltimateMoveUse(bird, RoosterSpecials.DAWN_STAMPEDE_MOVE);
        } else if (ultimateTriggered && bird.type == BirdGame3.BirdType.PENGUIN) {
            game.recordUltimateMoveUse(bird, PenguinSpecials.ABSOLUTE_ZERO_FORTRESS_MOVE);
        } else if (ultimateTriggered && bird.type == BirdGame3.BirdType.SHOEBILL) {
            game.recordUltimateMoveUse(bird, ShoebillSpecials.FINAL_STILLNESS_MOVE);
        } else if (ultimateTriggered && bird.type == BirdGame3.BirdType.MOCKINGBIRD) {
            game.recordUltimateMoveUse(bird, MockingbirdSpecials.SHADOW_COURT_MOVE);
        } else if (ultimateTriggered && bird.type == BirdGame3.BirdType.RAZORBILL) {
            game.recordUltimateMoveUse(bird, RazorbillSpecials.GUILLOTINE_WAKE_MOVE);
        } else if (ultimateTriggered && bird.type == BirdGame3.BirdType.TURKEY) {
            game.recordUltimateMoveUse(bird, TurkeySpecials.HARVEST_TRIBUNAL_MOVE);
        } else if (ultimateTriggered && bird.type == BirdGame3.BirdType.ROADRUNNER) {
            game.recordUltimateMoveUse(bird, RoadrunnerSpecials.REDLINE_EXECUTION_MOVE);
        } else if (ultimateTriggered && bird.type == BirdGame3.BirdType.GRINCHHAWK) {
            game.recordUltimateMoveUse(bird, GrinchhawkSpecials.MIDNIGHT_GIFTSTORM_MOVE);
        } else if (ultimateTriggered && bird.type == BirdGame3.BirdType.PELICAN) {
            game.recordUltimateMoveUse(bird, PelicanSpecials.MAELSTROM_GULLET_MOVE);
        } else if (ultimateTriggered && bird.type == BirdGame3.BirdType.VULTURE) {
            game.recordUltimateMoveUse(bird, VultureSpecials.BLACK_SKY_FEAST_MOVE);
        } else if (ultimateTriggered && bird.type == BirdGame3.BirdType.KIWI) {
            game.recordUltimateMoveUse(bird, KiwiSpecials.MIDNIGHT_STAMPEDE_MOVE);
        } else {
            game.recordSpecialMoveUse(bird, input, ultimateTriggered);
        }
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
            double angle = game.nextParticleRandom() * Math.PI * 2;
            double speed = 8 + game.nextParticleRandom() * 16;
            game.particles.add(new Particle(
                    bird.x + 40 + Math.cos(angle) * 20,
                    bird.y + 40 + Math.sin(angle) * 20,
                    Math.cos(angle) * speed,
                    Math.sin(angle) * speed - 4,
                    Color.GOLD.deriveColor(0, 1, 1, 0.95)
            ));
        }
    }

    private static void playSpecialSound(Bird bird, boolean ultimateTriggered) {
        BirdGame3 game = bird.game;
        if (!game.isSfxEnabled()) {
            return;
        }
        if (ultimateTriggered && bird.type == BirdGame3.BirdType.PHOENIX) {
            game.playRebirthNovaSfx();
        } else if (ultimateTriggered && bird.type == BirdGame3.BirdType.HUMMINGBIRD) {
            game.playHummingbirdNeedleheartSfx();
        } else if (ultimateTriggered && bird.type == BirdGame3.BirdType.ROOSTER) {
            game.playRoosterStampedeSfx();
        } else if (ultimateTriggered && bird.type == BirdGame3.BirdType.PENGUIN) {
            game.playAbsoluteZeroFortressSfx();
        } else if (ultimateTriggered && bird.type == BirdGame3.BirdType.SHOEBILL) {
            game.playShoebillFinalStillnessSfx();
        } else if (ultimateTriggered && bird.type == BirdGame3.BirdType.MOCKINGBIRD) {
            game.playMockingbirdShadowCourtSfx();
        } else if (ultimateTriggered && bird.type == BirdGame3.BirdType.RAZORBILL) {
            game.playRazorbillGuillotineWakeSfx();
        } else if (ultimateTriggered && bird.type == BirdGame3.BirdType.TURKEY) {
            game.playTurkeyHarvestTribunalSfx();
        } else if (ultimateTriggered && bird.type == BirdGame3.BirdType.ROADRUNNER) {
            game.playRoadrunnerRedlineExecutionSfx();
        } else if (ultimateTriggered && bird.type == BirdGame3.BirdType.GRINCHHAWK) {
            game.playGrinchhawkMidnightGiftstormSfx();
        } else if (ultimateTriggered && bird.type == BirdGame3.BirdType.PELICAN) {
            game.playPelicanMaelstromGulletSfx();
        } else if (ultimateTriggered && bird.type == BirdGame3.BirdType.VULTURE) {
            game.playVultureBlackSkyFeastSfx();
        } else if (bird.type == BirdGame3.BirdType.RAZORBILL) {
            game.playVaseBreakingSfx();
        } else {
            game.playJalapenoSfx();
        }
    }

}
