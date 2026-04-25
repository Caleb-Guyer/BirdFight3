package com.example.birdgame3;

import javafx.animation.AnimationTimer;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

final class MatchController {
    private final BirdGame3 game;

    MatchController(BirdGame3 game) {
        this.game = game;
    }

    void prepareMatchStart(Stage stage) {
        game.clearGameplayInputs();
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
        if (game.usesSmashCombatRules()) {
            return findSmashTimeoutWinner();
        }
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

        return getBird();
    }

    private Bird findSmashTimeoutWinner() {
        if (isStandardTeamMatch()) {
            Integer winnerTeam = null;
            for (int team = 1; team <= 2; team++) {
                if (!teamHasBirds(team)) continue;
                if (winnerTeam == null || game.compareTeamPlacements(team, winnerTeam) < 0) {
                    winnerTeam = team;
                }
            }
            if (winnerTeam == null) {
                return null;
            }
            Bird winner = null;
            for (int i = 0; i < game.activePlayers; i++) {
                Bird bird = game.players[i];
                if (bird == null || game.getEffectiveTeam(i) != winnerTeam) continue;
                if (winner == null || game.compareBirdPlacements(bird, winner) < 0) {
                    winner = bird;
                }
            }
            return winner;
        }

        Bird winner = null;
        for (int i = 0; i < game.activePlayers; i++) {
            Bird bird = game.players[i];
            if (bird == null) continue;
            if (winner == null || game.compareBirdPlacements(bird, winner) < 0) {
                winner = bird;
            }
        }
        return winner;
    }

    private Bird getBird() {
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

    private boolean hasSmashTimeoutStockTie() {
        if (isStandardTeamMatch()) {
            Set<Integer> leadingTeams = leadingTeamsByStocks();
            return leadingTeams.size() > 1;
        }
        return leadingPlayerSlotsByStocks().size() > 1;
    }

    private List<Integer> leadingPlayerSlotsByStocks() {
        List<Integer> leaders = new ArrayList<>();
        int bestStocks = Integer.MIN_VALUE;
        for (int i = 0; i < game.activePlayers; i++) {
            if (game.players[i] == null) continue;
            int stocks = game.matchScoreForPlayer(i);
            if (stocks > bestStocks) {
                leaders.clear();
                leaders.add(i);
                bestStocks = stocks;
            } else if (stocks == bestStocks) {
                leaders.add(i);
            }
        }
        return leaders;
    }

    private Set<Integer> leadingTeamsByStocks() {
        Set<Integer> leaders = new HashSet<>();
        int bestStocks = Integer.MIN_VALUE;
        for (int team = 1; team <= 2; team++) {
            if (!teamHasBirds(team)) continue;
            int stocks = game.teamMatchScore(team);
            if (stocks > bestStocks) {
                leaders.clear();
                leaders.add(team);
                bestStocks = stocks;
            } else if (stocks == bestStocks) {
                leaders.add(team);
            }
        }
        return leaders;
    }

    private void startSmashSuddenDeath() {
        if (game.suddenDeath.isActive()) return;

        game.powerUps.clear();
        game.crowMinions.clear();
        game.piranhaHazards.clear();
        game.chickMinions.clear();
        game.clearDockShipBomb();
        game.suddenDeath.startSmashTiebreaker();

        List<Bird> contenders = new ArrayList<>();
        if (isStandardTeamMatch()) {
            Set<Integer> leadingTeams = leadingTeamsByStocks();
            for (int i = 0; i < game.activePlayers; i++) {
                Bird bird = game.players[i];
                if (bird == null) continue;
                boolean contender = leadingTeams.contains(game.getEffectiveTeam(i)) && game.playerHasStocksRemaining(i);
                game.scores[i] = contender ? 1 : 0;
                if (contender) {
                    contenders.add(bird);
                } else {
                    bird.retireFromStockMatch();
                }
            }
        } else {
            List<Integer> leadingSlots = leadingPlayerSlotsByStocks();
            for (int i = 0; i < game.activePlayers; i++) {
                Bird bird = game.players[i];
                if (bird == null) continue;
                boolean contender = leadingSlots.contains(i);
                game.scores[i] = contender ? 1 : 0;
                if (contender) {
                    contenders.add(bird);
                } else {
                    bird.retireFromStockMatch();
                }
            }
        }

        for (int i = 0; i < contenders.size(); i++) {
            Bird bird = contenders.get(i);
            bird.resetForSmashRespawn(
                    suddenDeathSpawnX(i, contenders.size()),
                    game.battlefieldSpawnY(bird.sizeMultiplier),
                    game.smashSuddenDeathPercent()
            );
        }

        game.addToKillFeed("TIME! Stocks tied.");
        game.addToKillFeed("SUDDEN DEATH! 1 stock at 300%. The crows are coming.");
        game.playHugewaveSfx();
        game.shakeIntensity = Math.max(game.shakeIntensity, 20);
        game.hitstopFrames = Math.max(game.hitstopFrames, 12);
    }

    private double suddenDeathSpawnX(int slot, int contenderCount) {
        double center = game.battlefieldSpawnCenterX();
        if (contenderCount <= 1) {
            return center - 40.0;
        }
        double spacing = Math.clamp(240.0 - contenderCount * 18.0, 130.0, 220.0);
        double start = center - spacing * (contenderCount - 1) / 2.0;
        return start + slot * spacing - 40.0;
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
        if (game.trainingModeActive || game.matchEnded) {
            return;
        }

        if (game.usesSmashCombatRules()) {
            if (game.matchTimer <= 0 && !game.suddenDeath.isActive()) {
                if (hasSmashTimeoutStockTie()) {
                    startSmashSuddenDeath();
                } else {
                    Bird timeoutWinner = findTimeoutWinner();
                    game.addToKillFeed("TIME! Highest stock count wins.");
                    triggerMatchEnd(timeoutWinner);
                    return;
                }
            }
        } else if (game.matchTimer <= 0 && !game.suddenDeath.isActive()) {
            game.suddenDeath.startHazardSwarm();
            game.playHugewaveSfx();
            game.addToKillFeed("SUDDEN DEATH! A MURDER OF CROWS DESCENDS!");
            game.shakeIntensity = 40;
            game.hitstopFrames = 30;
        }

        if (game.suddenDeath.isActive()) {
            game.shakeIntensity = game.suddenDeath.updateAndSpawn(
                    game.crowMinions,
                    game.piranhaHazards,
                    game.random,
                    game.shakeIntensity,
                    game.selectedMap == BirdGame3.MapType.DOCK,
                    game.dockWaterStartX(),
                    game.dockWaterSurfaceY(),
                    game.dockWaterWidth(),
                    game.dockDrownDepthY(),
                    game.matchEnded
            );
        }
    }

    void checkForMatchCompletion() {
        if (game.usesSmashCombatRules()) {
            checkForSmashStockCompletion();
            return;
        }
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

    private void checkForSmashStockCompletion() {
        if (game.matchEnded) return;

        if (isStandardTeamMatch()) {
            Set<Integer> aliveTeams = new HashSet<>();
            for (int i = 0; i < game.activePlayers; i++) {
                if (game.players[i] != null && game.playerHasStocksRemaining(i)) {
                    aliveTeams.add(game.getEffectiveTeam(i));
                }
            }
            if (aliveTeams.size() <= 1) {
                Bird winner = aliveTeams.isEmpty() ? findSmashTimeoutWinner() : bestBirdOnTeam(aliveTeams.iterator().next());
                triggerMatchEnd(winner);
            }
            return;
        }

        int alive = 0;
        Bird winner = null;
        for (int i = 0; i < game.activePlayers; i++) {
            Bird bird = game.players[i];
            if (bird == null || !game.playerHasStocksRemaining(i)) continue;
            alive++;
            if (winner == null || game.compareBirdPlacements(bird, winner) < 0) {
                winner = bird;
            }
        }
        if (alive <= 1) {
            if (winner == null) {
                winner = findSmashTimeoutWinner();
            }
            triggerMatchEnd(winner);
        }
    }

    private Bird bestBirdOnTeam(int teamId) {
        Bird winner = null;
        for (int i = 0; i < game.activePlayers; i++) {
            Bird bird = game.players[i];
            if (bird == null || game.getEffectiveTeam(i) != teamId) continue;
            if (winner == null) {
                winner = bird;
                continue;
            }
            boolean birdHasStocks = game.playerHasStocksRemaining(i);
            boolean winnerHasStocks = game.playerHasStocksRemaining(winner.playerIndex);
            if (birdHasStocks != winnerHasStocks) {
                if (birdHasStocks) {
                    winner = bird;
                }
                continue;
            }
            if (game.compareBirdPlacements(bird, winner) < 0) {
                winner = bird;
            }
        }
        return winner;
    }

    private boolean teamHasBirds(int teamId) {
        for (int i = 0; i < game.activePlayers; i++) {
            if (game.players[i] != null && game.getEffectiveTeam(i) == teamId) {
                return true;
            }
        }
        return false;
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
