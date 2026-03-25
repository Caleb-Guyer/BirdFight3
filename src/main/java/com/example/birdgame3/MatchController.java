package com.example.birdgame3;

import javafx.animation.AnimationTimer;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

final class MatchController {
    private final BirdGame3 game;

    MatchController(BirdGame3 game) {
        this.game = game;
    }

    void prepareMatchStart(Stage stage) {
        game.matchTimer = BirdGame3.MATCH_DURATION_FRAMES;
        game.storyMatchTimerOverride = -1;
        game.storyTeamMode = false;
        Arrays.fill(game.storyTeams, 1);
        game.adventureMatchTimerOverride = -1;
        game.adventureTeamMode = false;
        Arrays.fill(game.adventureTeams, 1);
        if (!game.classicModeActive) {
            game.classicEncounter = null;
            game.classicTeamMode = false;
            Arrays.fill(game.classicTeams, 1);
        }
        game.activeMutator = BirdGame3.MatchMutator.NONE;
        game.activePowerUpSpawnInterval = BirdGame3.POWERUP_SPAWN_INTERVAL;
        game.lastMutatorHazardTime = System.nanoTime();
        game.suddenDeath.reset();
        game.matchStartNano = System.nanoTime();
        game.balanceOutcomeRecorded = false;
        game.killFeed.clear();
        game.currentStage = stage;
        Arrays.fill(game.scores, 0);
        Arrays.fill(game.players, null);
    }

    void applyMutatorStartEffects(BirdGame3.MatchMutator mutator, boolean announce, boolean classicScale) {
        if (mutator == null || mutator == BirdGame3.MatchMutator.NONE) return;
        double turboPower = classicScale ? 1.10 : 1.12;
        double turboSpeed = classicScale ? 1.12 : 1.14;

        switch (mutator) {
            case TURBO_BRAWL -> {
                for (int i = 0; i < game.activePlayers; i++) {
                    Bird b = game.players[i];
                    if (b == null) continue;
                    b.setBaseMultipliers(
                            b.baseSizeMultiplier,
                            b.basePowerMultiplier * turboPower,
                            b.baseSpeedMultiplier * turboSpeed
                    );
                }
                if (announce) {
                    game.addToKillFeed("Turbo Brawl: +speed and +power for everyone.");
                }
            }
            case RAGE_FRENZY -> {
                for (int i = 0; i < game.activePlayers; i++) {
                    Bird b = game.players[i];
                    if (b == null) continue;
                    b.rageTimer = Math.max(b.rageTimer, BirdGame3.MUTATOR_BUFF_FRAMES);
                    b.powerMultiplier = Math.max(b.powerMultiplier, b.basePowerMultiplier * 1.6);
                }
                if (announce) {
                    game.addToKillFeed("Rage Frenzy: everyone is enraged.");
                }
            }
            case TITAN_RUMBLE -> {
                for (int i = 0; i < game.activePlayers; i++) {
                    Bird b = game.players[i];
                    if (b == null) continue;
                    b.titanActive = true;
                    b.titanTimer = Math.max(b.titanTimer, BirdGame3.MUTATOR_BUFF_FRAMES);
                    if (b.shrinkTimer <= 0) {
                        b.sizeMultiplier = b.baseSizeMultiplier * 1.35;
                    }
                    b.powerMultiplier = Math.max(b.powerMultiplier, b.basePowerMultiplier * 1.4);
                }
                if (announce) {
                    game.addToKillFeed("Titan Rumble: everyone is in Titan form.");
                }
            }
            case OVERCHARGE_BRAWL -> {
                for (int i = 0; i < game.activePlayers; i++) {
                    Bird b = game.players[i];
                    if (b == null) continue;
                    b.specialCooldown = 0;
                    b.overchargeAttackTimer = Math.max(b.overchargeAttackTimer, BirdGame3.MUTATOR_BUFF_FRAMES);
                    b.rageTimer = Math.max(b.rageTimer, BirdGame3.MUTATOR_BUFF_FRAMES);
                    b.powerMultiplier = Math.max(b.powerMultiplier, b.basePowerMultiplier * 1.35);
                }
                if (announce) {
                    game.addToKillFeed("Overcharge Brawl: rapid attacks for everyone.");
                }
            }
            default -> {
            }
        }
    }

    void configureMatchModes() {
        game.activeMutator = BirdGame3.MatchMutator.NONE;
        game.activePowerUpSpawnInterval = BirdGame3.POWERUP_SPAWN_INTERVAL;
        game.lastMutatorHazardTime = System.nanoTime();

        if (game.classicModeActive) {
            if (game.classicEncounter == null) return;
            game.activeMutator = game.classicEncounter.mutator;
            if (game.activeMutator != BirdGame3.MatchMutator.NONE) {
                game.addToKillFeed("CLASSIC MUTATOR: " + game.activeMutator.label);
            }
            applyMutatorStartEffects(game.activeMutator, false, true);
            return;
        }

        if (game.storyModeActive || game.adventureModeActive || game.trainingModeActive) return;

        if (game.competitionModeEnabled) {
            if (!game.competitionSeriesActive) {
                Arrays.fill(game.competitionRoundWins, 0);
                Arrays.fill(game.competitionTeamWins, 0);
                game.competitionRoundNumber = 1;
                game.competitionSeriesActive = true;
            }
            game.matchTimer = BirdGame3.COMPETITION_DURATION_FRAMES;
            game.activePowerUpSpawnInterval = Long.MAX_VALUE;
            game.powerUps.clear();
            game.addToKillFeed("COMPETITION MODE: Round " + game.competitionRoundNumber
                    + " (first to " + BirdGame3.COMPETITION_ROUND_TARGET + ")");
            return;
        }

        if (!game.mutatorModeEnabled) return;

        BirdGame3.MatchMutator[] pool = {
                BirdGame3.MatchMutator.RAGE_FRENZY,
                BirdGame3.MatchMutator.TITAN_RUMBLE,
                BirdGame3.MatchMutator.OVERCHARGE_BRAWL,
                BirdGame3.MatchMutator.CROW_SURGE,
                BirdGame3.MatchMutator.TURBO_BRAWL
        };
        game.activeMutator = pool[game.random.nextInt(pool.length)];
        game.addToKillFeed("MUTATOR ACTIVE: " + game.activeMutator.label);
        applyMutatorStartEffects(game.activeMutator, true, false);
    }

    Bird findTimeoutWinner() {
        boolean teamComp = isStandardTeamMatch();
        if (teamComp) {
            int[] teamHealth = new int[3];
            int[] teamDamage = new int[3];
            for (int i = 0; i < game.activePlayers; i++) {
                Bird b = game.players[i];
                if (b == null || b.health <= 0) continue;
                int team = game.getEffectiveTeam(b.playerIndex);
                if (team < 1 || team > 2) continue;
                teamHealth[team] += (int) Math.round(b.health);
                teamDamage[team] += Math.max(0, game.damageDealt[b.playerIndex]);
            }
            int winnerTeam = -1;
            if (teamHealth[1] > teamHealth[2]) winnerTeam = 1;
            else if (teamHealth[2] > teamHealth[1]) winnerTeam = 2;
            else if (teamDamage[1] > teamDamage[2]) winnerTeam = 1;
            else if (teamDamage[2] > teamDamage[1]) winnerTeam = 2;
            if (winnerTeam == -1) return null;
            for (int i = 0; i < game.activePlayers; i++) {
                Bird b = game.players[i];
                if (b != null && b.health > 0 && game.getEffectiveTeam(b.playerIndex) == winnerTeam) {
                    return b;
                }
            }
            for (int i = 0; i < game.activePlayers; i++) {
                Bird b = game.players[i];
                if (b != null && game.getEffectiveTeam(b.playerIndex) == winnerTeam) {
                    return b;
                }
            }
            return null;
        }

        Bird winner = null;
        double bestHealth = -1;
        int bestDamage = -1;
        for (int i = 0; i < game.activePlayers; i++) {
            Bird b = game.players[i];
            if (b == null || b.health <= 0) continue;
            int dmg = game.damageDealt[i];
            if (b.health > bestHealth || (Math.abs(b.health - bestHealth) < 0.001 && dmg > bestDamage)) {
                bestHealth = b.health;
                bestDamage = dmg;
                winner = b;
            }
        }
        return winner;
    }

    String competitionScoreLine() {
        if (isStandardTeamMatch()) {
            return "ROUND " + game.competitionRoundNumber + " | TEAM A "
                    + game.competitionTeamWins[1] + " - TEAM B " + game.competitionTeamWins[2];
        }
        StringBuilder sb = new StringBuilder("ROUND ").append(game.competitionRoundNumber).append(" | ");
        boolean first = true;
        for (int i = 0; i < game.activePlayers; i++) {
            if (game.players[i] == null) continue;
            if (!first) sb.append("   ");
            sb.append("P").append(i + 1).append(":").append(game.competitionRoundWins[i]);
            first = false;
        }
        return sb.toString();
    }

    boolean handleCompetitionRoundEnd(Bird winner) {
        if (!isCompetitionMatch()) return false;

        boolean teamComp = isStandardTeamMatch();
        boolean seriesWon = false;
        String roundWinnerText = "DRAW";

        if (winner != null) {
            if (teamComp) {
                int team = game.getEffectiveTeam(winner.playerIndex);
                if (team == 1 || team == 2) {
                    game.competitionTeamWins[team]++;
                    roundWinnerText = team == 1 ? "TEAM A" : "TEAM B";
                    seriesWon = game.competitionTeamWins[team] >= BirdGame3.COMPETITION_ROUND_TARGET;
                }
            } else {
                int idx = winner.playerIndex;
                if (idx >= 0 && idx < game.competitionRoundWins.length) {
                    game.competitionRoundWins[idx]++;
                    roundWinnerText = BirdGame3.shortName(winner.name);
                    seriesWon = game.competitionRoundWins[idx] >= BirdGame3.COMPETITION_ROUND_TARGET;
                }
            }
        }

        game.addToKillFeed("ROUND " + game.competitionRoundNumber + " WINNER: " + roundWinnerText);

        if (seriesWon) {
            game.competitionSeriesActive = false;
            return false;
        }

        game.competitionRoundNumber++;
        javafx.application.Platform.runLater(() -> {
            if (game.currentStage == null) return;
            game.resetMatchStats();
            game.startMatch(game.currentStage);
        });
        return true;
    }

    void applyMatchModeRuntimeEffects(long now) {
        if (game.storyModeActive || game.adventureModeActive || game.trainingModeActive) return;

        switch (game.activeMutator) {
            case RAGE_FRENZY -> {
                for (int i = 0; i < game.activePlayers; i++) {
                    Bird b = game.players[i];
                    if (b == null) continue;
                    b.rageTimer = Math.max(b.rageTimer, BirdGame3.MUTATOR_BUFF_FRAMES);
                    b.powerMultiplier = Math.max(b.powerMultiplier, b.basePowerMultiplier * 1.6);
                }
            }
            case TITAN_RUMBLE -> {
                for (int i = 0; i < game.activePlayers; i++) {
                    Bird b = game.players[i];
                    if (b == null) continue;
                    b.titanActive = true;
                    b.titanTimer = Math.max(b.titanTimer, BirdGame3.MUTATOR_BUFF_FRAMES);
                    if (b.shrinkTimer <= 0) {
                        b.sizeMultiplier = b.baseSizeMultiplier * 1.35;
                    }
                    b.powerMultiplier = Math.max(b.powerMultiplier, b.basePowerMultiplier * 1.4);
                }
            }
            case OVERCHARGE_BRAWL -> {
                for (int i = 0; i < game.activePlayers; i++) {
                    Bird b = game.players[i];
                    if (b == null) continue;
                    b.overchargeAttackTimer = Math.max(b.overchargeAttackTimer, BirdGame3.MUTATOR_BUFF_FRAMES);
                    b.rageTimer = Math.max(b.rageTimer, BirdGame3.MUTATOR_BUFF_FRAMES);
                    b.powerMultiplier = Math.max(b.powerMultiplier, b.basePowerMultiplier * 1.35);
                }
            }
            case CROW_SURGE -> {
                if (now - game.lastMutatorHazardTime > 1_000_000_000L * 6) {
                    game.lastMutatorHazardTime = now;
                    int waves = 2 + game.random.nextInt(2);
                    for (int i = 0; i < waves; i++) {
                        double y = 220 + game.random.nextDouble() * (BirdGame3.WORLD_HEIGHT - 900);
                        CrowMinion left = new CrowMinion(-120, y, null);
                        left.vx = 4.5 + game.random.nextDouble() * 2.0;
                        left.vy = (game.random.nextDouble() - 0.5) * 3.5;
                        game.crowMinions.add(left);

                        CrowMinion right = new CrowMinion(BirdGame3.WORLD_WIDTH + 120, y, null);
                        right.vx = -4.5 - game.random.nextDouble() * 2.0;
                        right.vy = (game.random.nextDouble() - 0.5) * 3.5;
                        game.crowMinions.add(right);
                    }
                    game.addToKillFeed("MUTATOR: Crow surge wave!");
                    game.shakeIntensity = Math.max(game.shakeIntensity, 12);
                }
            }
            default -> {
            }
        }
    }

    void updateTimerAndSuddenDeath() {
        if (isCompetitionMatch() && !game.trainingModeActive && !game.matchEnded && game.matchTimer <= 0) {
            Bird timeoutWinner = findTimeoutWinner();
            game.addToKillFeed("TIME! Tournament decision.");
            triggerMatchEnd(timeoutWinner);
            return;
        }

        if (!game.trainingModeActive && !game.matchEnded && game.matchTimer <= 0 && !game.suddenDeath.isActive()) {
            game.suddenDeath.start();
            game.playHugewaveSfx();
            game.addToKillFeed("SUDDEN DEATH! A MURDER OF CROWS DESCENDS!");
            game.shakeIntensity = 40;
            game.hitstopFrames = 30;
        }

        if (!isCompetitionMatch() && !game.trainingModeActive) {
            game.shakeIntensity = game.suddenDeath.updateAndSpawn(
                    game.crowMinions,
                    game.random,
                    BirdGame3.WORLD_WIDTH,
                    BirdGame3.WORLD_HEIGHT,
                    game.shakeIntensity,
                    game.matchEnded
            );
        }
    }

    void checkForMatchCompletion() {
        int alive = 0;
        Bird winner = null;
        Set<Integer> aliveTeams = new HashSet<>();
        for (Bird b : game.players) {
            if (b != null && b.health > 0) {
                alive++;
                winner = b;
                aliveTeams.add(game.getEffectiveTeam(b.playerIndex));
            }
        }
        boolean teamModeMatch =
                (game.teamModeEnabled && !game.storyModeActive && !game.adventureModeActive && !game.classicModeActive)
                        || (game.storyModeActive && game.storyTeamMode)
                        || (game.adventureModeActive && game.adventureTeamMode)
                        || (game.classicModeActive && game.classicTeamMode);
        boolean isMatchOver = teamModeMatch ? aliveTeams.size() <= 1 : alive <= 1;
        if (isMatchOver && !game.matchEnded) {
            triggerMatchEnd(winner);
        }
    }

    void triggerMatchEnd(Bird winner) {
        if (game.matchEnded) return;

        if (game.lanModeActive) {
            game.matchEnded = true;
            game.lanMatchActive = false;
            int winnerIndex = winner != null ? winner.playerIndex : -1;
            if (game.lanIsHost && game.lanHost != null) {
                game.lanHost.broadcastMatchEnd(winnerIndex);
            }
            final Stage finalStage = game.currentStage;
            new AnimationTimer() {
                private int framesLeft = 90;

                @Override
                public void handle(long now) {
                    if (framesLeft > 0) {
                        framesLeft--;
                        return;
                    }
                    stop();
                    if (game.timer != null) game.timer.stop();
                    game.showLanResults(finalStage, winnerIndex);
                }
            }.start();
            return;
        }

        if (handleCompetitionRoundEnd(winner)) {
            game.matchEnded = true;
            game.timer.stop();
            return;
        }

        game.matchEnded = true;
        final Bird finalWinner = winner;
        final Stage finalStage = game.currentStage;
        new AnimationTimer() {
            private int framesLeft = 100;

            @Override
            public void handle(long now) {
                if (framesLeft > 0) {
                    framesLeft--;
                    if (finalWinner != null && framesLeft % 3 == 0) {
                        for (int i = 0; i < 15; i++) {
                            double angle = Math.random() * Math.PI * 2;
                            double spd = 6 + Math.random() * 16;
                            game.particles.add(new Particle(
                                    finalWinner.x + 40,
                                    finalWinner.y + 40,
                                    Math.cos(angle) * spd,
                                    Math.sin(angle) * spd - 7,
                                    Color.GOLD.deriveColor(0, 1, 1, 0.95)
                            ));
                        }
                    }
                    return;
                }
                stop();
                game.timer.stop();
                game.recordBalanceOutcome(finalWinner);
                if (game.tournamentModeActive && game.currentTournamentMatch != null && !game.tournamentMatchResolved) {
                    BirdGame3.TournamentEntry winnerEntry = game.resolveTournamentWinnerEntry(finalWinner);
                    if (winnerEntry != null) {
                        game.recordTournamentWinner(game.currentTournamentMatch, winnerEntry);
                    }
                    game.tournamentMatchResolved = true;
                }
                game.showMatchSummary(finalStage, finalWinner);
                if (finalWinner != null && finalWinner.health < 20 && finalWinner.health > 0
                        && !game.achievementsUnlocked[9]) {
                    game.unlockAchievement(9, "CLUTCH GOD!");
                }
            }
        }.start();
    }

    private boolean isCompetitionMatch() {
        return game.competitionModeEnabled && !game.storyModeActive && !game.adventureModeActive && !game.classicModeActive;
    }

    private boolean isStandardTeamMatch() {
        return game.teamModeEnabled && !game.storyModeActive && !game.adventureModeActive && !game.classicModeActive;
    }
}
